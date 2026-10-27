#!/usr/bin/env python3
"""Sandbox-only lifecycle scenarios. All mutations are scoped to one lab clock."""
import argparse
import hashlib
import hmac
import importlib.util
import json
from pathlib import Path
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

spec = importlib.util.spec_from_file_location("lab", Path(__file__).with_name("payments-local.py"))
lab = importlib.util.module_from_spec(spec)
spec.loader.exec_module(lab)
BASE = "http://127.0.0.1:18080"
HOOK = "we_1UV7qwCzX9cIGlYvOx43aEqn"


class ApiError(RuntimeError):
    pass


class Cycle:
    def __init__(self, subscription):
        self.values = lab.read_env(lab.ROOT / ".env")
        lab.identity()
        lab.validate_backend(self.values)
        self.subscription = subscription
        self.clock = self.values.get("STRIPE_TEST_CLOCK")
        self.sub = self.stripe("GET", "/subscriptions/" + subscription)
        clock = self.stripe("GET", "/test_helpers/test_clocks/" + str(self.clock))
        if not self.valid_scope(self.sub, clock, self.clock):
            raise ApiError("La suscripción no pertenece al reloj de este laboratorio sandbox.")
        self.customer = self.sub["customer"]
        self.local_id = int(self.sub.get("metadata", {}).get("suscripcionId", 0))
        customer = self.stripe("GET", "/customers/" + self.customer)
        if customer.get("test_clock") != self.clock:
            raise ApiError("Cliente fuera del laboratorio.")
        siblings = self.stripe("GET", "/subscriptions", {"test_clock": self.clock, "status": "all", "limit": 100})
        ongoing = [s for s in siblings["data"] if s["status"] not in ("canceled", "incomplete_expired")]
        if siblings.get("has_more") or len(ongoing) > 1 or (ongoing and ongoing[0]["id"] != subscription):
            raise ApiError("El reloj debe tener una sola suscripción no finalizada, la del escenario, para no afectar a otros usuarios.")
        auth = self.local("POST", "/api/auth/login", {
            "email": self.values.get("APP_SUPERADMIN_EMAIL"),
            "password": self.values.get("APP_SUPERADMIN_PASSWORD")})
        self.token = auth["token"]
        profile = self.local("GET", "/api/me")
        config = self.local("GET", "/api/admin/pagos/configuracion")
        if profile.get("superAdmin") is not True or config.get("mode") != "test":
            raise ApiError("API local sin autoridad o modo de prueba.")
        # Fail before any mutation if the old backend version is still running.
        rows = self.local("GET", "/api/admin/pagos?size=100")["content"]
        if rows and "suscripcionId" not in rows[0]:
            raise ApiError("Reinicia el backend del laboratorio para cargar el contrato nuevo.")
        snapshot = self.snapshot()
        if not snapshot["suscripcion"]:
            raise ApiError("La suscripción Stripe no tiene vínculo con el usuario local esperado.")

    @staticmethod
    def valid_scope(sub, clock, expected):
        return bool(expected and sub.get("livemode") is False and clock.get("livemode") is False
                    and sub.get("test_clock") == expected and clock.get("id") == expected
                    and clock.get("name") == "gestion-payments-local-06")

    def request(self, method, url, headers, values=None, as_json=False):
        body = None
        if values is not None:
            encoded = json.dumps(values) if as_json else urllib.parse.urlencode(values)
            if method == "GET":
                url += "?" + encoded
            else:
                body = encoded.encode()
        request = urllib.request.Request(url, data=body, headers=headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                return json.load(response)
        except urllib.error.HTTPError as error:
            # Do not print bodies: they can contain card details or sensitive identifiers.
            raise ApiError("La operación devolvió HTTP " + str(error.code) + "; no se muestra la respuesta sensible.") from None

    def stripe(self, method, path, values=None):
        headers = {"Authorization": "Bearer " + self.values["STRIPE_SECRET_KEY"],
                   "Stripe-Version": "2026-08-26.dahlia", "Content-Type": "application/x-www-form-urlencoded"}
        if method == "POST":
            headers["Idempotency-Key"] = "payments-cycle-07-" + str(uuid.uuid4())
        return self.request(method, "https://api.stripe.com/v1" + path, headers, values)

    def local(self, method, path, values=None):
        headers = {"Content-Type": "application/json"}
        if hasattr(self, "token"):
            headers["Authorization"] = "Bearer " + self.token
        return self.request(method, BASE + path, headers, values, as_json=True)

    def wait_clock(self):
        for _ in range(40):
            clock = self.stripe("GET", "/test_helpers/test_clocks/" + self.clock)
            if clock["status"] == "ready":
                return clock
            if clock["status"] != "advancing":
                raise ApiError("El reloj no está disponible para simular.")
            time.sleep(2)
        raise ApiError("El reloj sigue avanzando; consulta status antes de repetir una mutación.")

    def advance(self):
        clock = self.wait_clock()
        sub = self.stripe("GET", "/subscriptions/" + self.subscription)
        if sub["status"] not in ("active", "trialing"):
            raise ApiError("Solo se avanza una suscripción activa o en prueba.")
        target = max(item["current_period_end"] for item in sub["items"]["data"]) + 7200
        if target <= clock["frozen_time"]:
            raise ApiError("Fin de período anterior al reloj; revisar antes de continuar.")
        self.stripe("POST", "/test_helpers/test_clocks/" + self.clock + "/advance", {"frozen_time": target})
        self.wait_clock()

    def payment_method(self, failing):
        method = "pm_card_chargeCustomerFail" if failing else "pm_card_visa"
        attached = self.stripe("POST", "/payment_methods/" + method + "/attach", {"customer": self.customer})
        self.stripe("POST", "/subscriptions/" + self.subscription, {"default_payment_method": attached["id"]})
        return attached["id"]

    def snapshot(self):
        sub = self.stripe("GET", "/subscriptions/" + self.subscription)
        invoices = self.stripe("GET", "/invoices", {"subscription": self.subscription, "limit": 100})
        if invoices.get("has_more"):
            raise ApiError("Demasiadas facturas para este laboratorio; no se verificó paginación completa.")
        rows = []
        page = 0
        while True:
            result = self.local("GET", "/api/admin/pagos?page=" + str(page) + "&size=100")
            rows.extend(x for x in result["content"] if x.get("suscripcionId") == self.local_id)
            if page + 1 >= result["totalPages"]:
                break
            page += 1
        subscriptions = self.local("GET", "/api/admin/suscripciones?size=100")["content"]
        local_sub = next((x for x in subscriptions if x["id"] == self.local_id), None)
        return {"stripeEstado": sub["status"], "cancelacionProgramada": sub["cancel_at_period_end"],
                "suscripcion": {k: local_sub.get(k) for k in ["estado", "cancelaAlFinal", "finPeriodoActual"]} if local_sub else None,
                "facturasStripe": [{"estado": i["status"], "importePagado": i["amount_paid"], "intentos": i["attempt_count"]} for i in invoices["data"]],
                "pagosLocales": [{k: x.get(k) for k in ["id", "estado", "montoCentavos", "montoDevueltoCentavos", "montoNetoCentavos", "intentos", "proximoIntento", "estadoFactura"]} for x in rows]}

    def replay(self, invoice):
        events = self.stripe("GET", "/events", {"type": "invoice.paid", "limit": 100})
        event = next((e for e in events["data"] if e["data"]["object"]["id"] == invoice["id"]), None)
        if not event:
            raise ApiError("No se encontró evento pagado de esta factura para reentregar.")
        before = self.snapshot()
        body = json.dumps(event).encode()
        timestamp = str(int(time.time()))
        signature = hmac.new(self.values["STRIPE_WEBHOOK_SECRET"].encode(), timestamp.encode() + b"." + body, hashlib.sha256).hexdigest()
        for _ in range(2):
            request = urllib.request.Request(BASE + "/api/stripe/webhook", data=body, method="POST",
                headers={"Content-Type": "application/json", "Stripe-Signature": "t=" + timestamp + ",v1=" + signature})
            with urllib.request.urlopen(request, timeout=30) as response:
                if response.status != 200:
                    raise ApiError("Reentrega rechazada por el backend local.")
        after = self.snapshot()
        if before["pagosLocales"] != after["pagosLocales"]:
            raise ApiError("La reentrega modificó los pagos locales; revisar idempotencia.")

    def run(self, action):
        if action == "status":
            return self.snapshot()
        self.wait_clock()
        before = self.snapshot()
        hook = self.stripe("GET", "/webhook_endpoints/" + HOOK)
        if hook.get("livemode") is not False or hook.get("url") != "https://vortise-be.onrender.com/api/stripe/webhook":
            raise ApiError("Webhook remoto distinto al autorizado.")
        restore = hook["status"] == "enabled"
        try:
            if restore:
                self.stripe("POST", "/webhook_endpoints/" + HOOK, {"disabled": "true"})
            if action == "renew":
                self.advance()
            elif action == "reject":
                if self.stripe("GET", "/subscriptions/" + self.subscription)["status"] not in ("active", "trialing"):
                    raise ApiError("El rechazo de renovación requiere una suscripción activa.")
                self.payment_method(True)
                self.advance()
            elif action == "recover":
                sub = self.stripe("GET", "/subscriptions/" + self.subscription)
                invoice = self.stripe("GET", "/invoices/" + sub["latest_invoice"])
                if invoice["status"] != "open" or invoice["amount_due"] <= 0:
                    raise ApiError("No hay factura abierta para recuperar.")
                method = self.payment_method(False)
                self.stripe("POST", "/invoices/" + invoice["id"] + "/pay", {"payment_method": method})
            elif action == "cancel":
                self.stripe("POST", "/subscriptions/" + self.subscription, {"cancel_at_period_end": "true"})
            elif action == "finish-cancel":
                sub = self.stripe("GET", "/subscriptions/" + self.subscription)
                if not sub["cancel_at_period_end"]:
                    raise ApiError("Primero programa la cancelación.")
                self.advance()
            elif action == "replay":
                sub = self.stripe("GET", "/subscriptions/" + self.subscription)
                invoice = self.stripe("GET", "/invoices/" + sub["latest_invoice"])
                if invoice["status"] != "paid":
                    raise ApiError("Reentrega requiere una factura pagada.")
                self.replay(invoice)
            # Stripe may emit the last events after clock-ready; give the local
            # listener a bounded delivery window, then report actual state.
            for _ in range(20):
                time.sleep(2)
                after = self.snapshot()
                if self.matches(action, before, after):
                    return {"escenario": action, "verificado": True, **after}
            raise ApiError("Stripe recibió la operación, pero no se confirmó el estado local esperado. Consulta status; no repitas la mutación automáticamente.")
        finally:
            if restore:
                try:
                    self.stripe("POST", "/webhook_endpoints/" + HOOK, {"disabled": "false"})
                except Exception:
                    raise ApiError("No se pudo restaurar el webhook de Render. Reactívalo en Stripe antes de continuar.") from None

    @staticmethod
    def matches(action, before, after):
        local = after.get("suscripcion") or {}
        if local.get("estado") != after["stripeEstado"]:
            return False
        old = {p["id"]: p for p in before["pagosLocales"]}
        new = {p["id"]: p for p in after["pagosLocales"]}
        if action == "renew":
            return (after["stripeEstado"] == "active" and len(new) == len(old) + 1
                    and any(p["estado"] == "PAGADO" and p.get("estadoFactura") == "paid" for key, p in new.items() if key not in old))
        if action == "reject":
            return (after["stripeEstado"] == "past_due" and len(new) == len(old) + 1
                    and any(p["estado"] == "FALLIDO" and (p.get("intentos") or 0) >= 1 for key, p in new.items() if key not in old))
        if action == "recover":
            failed = [key for key, p in old.items() if p["estado"] == "FALLIDO"]
            return (after["stripeEstado"] == "active" and new.keys() == old.keys() and bool(failed)
                    and all(new[key]["estado"] == "PAGADO" and new[key]["proximoIntento"] is None for key in failed))
        if action == "cancel":
            return after["cancelacionProgramada"] and local.get("cancelaAlFinal") and new.keys() == old.keys()
        if action == "finish-cancel":
            return after["stripeEstado"] == "canceled" and new.keys() == old.keys()
        return before["pagosLocales"] == after["pagosLocales"]


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["status", "renew", "reject", "recover", "cancel", "finish-cancel", "replay"])
    parser.add_argument("--subscription", required=True)
    args = parser.parse_args()
    try:
        print(json.dumps(Cycle(args.subscription).run(args.action), indent=2))
    except (ApiError, urllib.error.URLError, KeyError, ValueError) as error:
        raise SystemExit(str(error) if isinstance(error, ApiError) else "No se completó el escenario. Revisa configuración/conectividad local; no se muestran detalles sensibles.")
