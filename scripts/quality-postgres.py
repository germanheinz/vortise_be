#!/usr/bin/env python3
"""Explicit opt-in CODE-QUALITY-14 rehearsal. Own disposable database only."""
import json
import os
from pathlib import Path
import re
import secrets
import socket
import subprocess
import tempfile
import time
import uuid

ROOT = Path(__file__).resolve().parents[1]
PORT = 15438


def clean_environment():
    return {key: os.environ[key] for key in ('PATH', 'HOME', 'JAVA_HOME', 'TMPDIR', 'LANG', 'LC_ALL') if key in os.environ}


def require_owned(metadata, owner):
    if (metadata['Config']['Labels'].get('quality14.owner') != owner
            or metadata['HostConfig']['PortBindings'].get('5432/tcp') != [{'HostIp': '127.0.0.1', 'HostPort': str(PORT)}]
            or '/var/lib/postgresql' not in metadata['HostConfig'].get('Tmpfs', {})
            or any(mount.get('Type') in ('bind', 'volume') for mount in metadata.get('Mounts', []))):
        raise RuntimeError('Disposable quality database ownership/isolation rejected')


def recover_owned_container(name, owner, env):
    result = subprocess.run(['docker', 'inspect', name], env=env, capture_output=True, text=True)
    if result.returncode != 0:
        return None
    metadata = json.loads(result.stdout)[0]
    identity = metadata.get('Id', '')
    if (metadata.get('Config', {}).get('Labels', {}).get('quality14.owner') == owner
            and re.fullmatch(r'[a-f0-9]{12,64}', identity)):
        return identity
    return None


def main():
    with socket.socket() as probe:
        probe.bind(('127.0.0.1', PORT))
    env = clean_environment()
    owner = uuid.uuid4().hex
    name = 'quality14-' + owner
    container = None
    # Credentials and application logs are private and removed, not published as artifacts.
    with tempfile.TemporaryDirectory(prefix='quality14-') as temporary:
        folder = Path(temporary)
        try:
            password = secrets.token_urlsafe(48)
            envfile = folder / 'postgres.env'
            envfile.write_text('POSTGRES_USER=quality14_test\nPOSTGRES_DB=quality14_test\nPOSTGRES_PASSWORD=' + password + '\n')
            envfile.chmod(0o600)
            created = subprocess.run(['docker', 'run', '-d', '--name', name,
                '--label', 'quality14.owner=' + owner, '--env-file', str(envfile),
                '--tmpfs', '/var/lib/postgresql:rw', '-p', f'127.0.0.1:{PORT}:5432', 'postgres:18'],
                env=env, capture_output=True, text=True)
            if created.returncode:
                raise RuntimeError('Could not create exclusive quality database')
            container = created.stdout.strip()
            inspected = subprocess.run(['docker', 'inspect', container], env=env, capture_output=True, text=True, check=True)
            require_owned(json.loads(inspected.stdout)[0], owner)
            for _ in range(60):
                ready = subprocess.run(['docker', 'exec', container, 'pg_isready', '-U', 'quality14_test', '-d', 'quality14_test'],
                    env=env, capture_output=True)
                if ready.returncode == 0:
                    break
                time.sleep(0.5)
            else:
                raise RuntimeError('Disposable quality database readiness timed out')
            child_env = dict(env, QUALITY14_PG_PASSWORD=password)
            command = ['mvn', '-B', '-Dtest=Quality14PostgresTest', '-Dquality14.pg.enabled=true',
                '-Dpayments.pg.enabled=false', '-Dauth.pg.enabled=false', '-Dproduction09.pg.enabled=false',
                '-Dapp.password-mail.enabled=false', '-Dapp.superadmin.email=', '-Dapp.superadmin.password=',
                '-Dapp.stripe.secret-key=', '-Dapp.stripe.webhook-secret=', '-Dapp.stripe.price-basic=', '-Dapp.stripe.price-pro=', 'test']
            log = folder / 'maven.log'
            with log.open('w') as output:
                result = subprocess.run(command, cwd=ROOT, env=child_env, stdout=output, stderr=subprocess.STDOUT)
            # Only Surefire counts and outcome; never publish logs, SQL, credentials or fixtures.
            summaries = re.findall(r'Tests run: \d+, Failures: \d+, Errors: \d+, Skipped: \d+', log.read_text(errors='replace'))
            if summaries:
                print(summaries[-1], flush=True)
            if result.returncode:
                raise RuntimeError('Quality PostgreSQL verification failed; private application logs not published')
            print('CODE-QUALITY-14 PostgreSQL rehearsal: PASS', flush=True)
        finally:
            if container is None:
                container = recover_owned_container(name, owner, env)
            if container:
                inspected = subprocess.run(['docker', 'inspect', '--format', '{{index .Config.Labels "quality14.owner"}}', container],
                    env=env, capture_output=True, text=True)
                if inspected.returncode != 0 or inspected.stdout.strip() != owner:
                    raise RuntimeError('Refusing cleanup: quality container owner mismatch')
                subprocess.run(['docker', 'rm', '-f', container], env=env, check=True, stdout=subprocess.DEVNULL)
                print('Owned quality database and private artifacts cleaned', flush=True)


if __name__ == '__main__':
    main()
