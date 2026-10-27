#!/usr/bin/env python3
"""Explicit local-only payments lab. Never prints credentials or resets data."""
import json
import os
import shutil
from pathlib import Path
import subprocess
import sys
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
ACCOUNT = "acct_1UV7ZmCzX9cIGlYv"
EVENTS = "checkout.session.completed,customer.subscription.created,customer.subscription.updated,customer.subscription.deleted,invoice.paid,invoice.payment_failed,invoice.updated,invoice.voided,invoice.marked_uncollectible,invoice.payment_action_required,refund.created,refund.updated,refund.failed"

def find_stripe_cli(home=None):
    executable = shutil.which("stripe")
    if executable:
        return Path(executable)
    # npm global installations belong to an NVM Node version. A terminal using
    # another Node version will not include that version's bin directory.
    home = Path.home() if home is None else Path(home)
    candidates = list((home / ".nvm/versions/node").glob("*/bin/stripe"))
    candidates.sort(key=lambda p: tuple(int(x) for x in p.parents[1].name.lstrip("v").split(".") if x.isdigit()), reverse=True)
    for candidate in candidates:
        if candidate.is_file() and os.access(candidate, os.X_OK):
            return candidate
    sys.exit("No se encontró Stripe CLI. Instálalo con npm install -g @stripe/cli y vuelve a ejecutar el comando.")

def stripe_environment(executable):
    env = dict(os.environ)
    # Use the matching Node runtime for npm's CLI launcher, without changing
    # the user's shell or the runtime used for the application.
    env["PATH"] = str(executable.parent) + os.pathsep + env.get("PATH", "")
    return env

def read_env(path):
    values = {}
    for line in path.read_text().splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip()
    return values

def identity():
    executable = find_stripe_cli()
    result = subprocess.run([str(executable), "whoami", "--format", "json"],
        capture_output=True, text=True, env=stripe_environment(executable))
    try:
        data = json.loads(result.stdout)
    except ValueError:
        sys.exit("Stripe CLI: inicia sesión en Vortise_sandbox.")
    if result.returncode or data.get("account_id") != ACCOUNT or data.get("mode") != "test":
        sys.exit("Stripe CLI no está autenticado en el sandbox esperado. No se inició nada.")

def validate_backend(values):
    key = values.get("STRIPE_SECRET_KEY", "")
    if not key.startswith(("sk_test_", "rk_test_")):
        sys.exit("Guarda una clave de Vortise_sandbox en STRIPE_SECRET_KEY de .env. No se inició el backend.")
    for name, amount in [("BASIC", 5000), ("PRO", 9900)]:
        price = values.get("STRIPE_PRICE_" + name, "")
        request = urllib.request.Request("https://api.stripe.com/v1/prices/" + price,
            headers={"Authorization": "Bearer " + key, "Stripe-Version": "2026-08-26.dahlia"})
        try:
            with urllib.request.urlopen(request, timeout=20) as response:
                data = json.load(response)
        except Exception:
            sys.exit("No se pudo validar el precio " + name + ". Revisa clave, permisos y sandbox; no se inició nada.")
        if data.get("livemode") is not False or data.get("unit_amount") != amount or data.get("currency") != "usd" or data.get("recurring", {}).get("interval") != "month":
            sys.exit("Precio " + name + " incompatible con el laboratorio. No se inició nada.")
    if not values.get("STRIPE_WEBHOOK_SECRET", "").startswith("whsec_") or not values.get("APP_JWT_SECRET"):
        sys.exit("Falta secreto local de webhook o JWT en .env.")

if __name__ == "__main__":
    os.chdir(ROOT)
    action = sys.argv[1] if len(sys.argv) == 2 else "help"
    if action not in ("check", "backend", "listen"):
        sys.exit("Uso: python3 scripts/payments-local.py check|backend|listen")
    identity()
    if action == "listen":
        executable = find_stripe_cli()
        os.execve(str(executable), [str(executable), "listen", "--events", EVENTS,
            "--forward-to", "http://127.0.0.1:18080/api/stripe/webhook"], stripe_environment(executable))
    values = read_env(ROOT / ".env")
    validate_backend(values)
    if action == "check":
        print("Sandbox y precios BASIC/PRO validados. Sin escrituras ni arranque de aplicación.")
    else:
        database = read_env(ROOT / ".env.payments-local-06")
        env = dict(os.environ)
        # Prevent inherited production datasource/profile settings from overriding the lab.
        for name in list(env):
            if name.startswith(("SPRING_", "APP_", "STRIPE_", "PAYMENTS_")) or name in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "MAVEN_OPTS", "PORT"):
                env.pop(name)
        env.update(values)
        env["PAYMENTS_DB_PASSWORD"] = database["POSTGRES_PASSWORD"]
        env["SPRING_PROFILES_ACTIVE"] = "payments-local"
        env["SPRING_DATASOURCE_URL"] = "jdbc:postgresql://127.0.0.1:15433/gestion_payments_test"
        env["SPRING_DATASOURCE_USERNAME"] = "gestion_payments_test"
        env["SPRING_DATASOURCE_PASSWORD"] = database["POSTGRES_PASSWORD"]
        env["APP_FRONTEND_URL"] = "http://localhost:3000"
        os.execvpe("mvn", ["mvn", "spring-boot:run", "-Dspring-boot.run.profiles=payments-local"], env)
