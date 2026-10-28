"""Opt-in local fixture rehearsal: no Render configuration, credentials or network calls."""
import importlib.util
import os
from pathlib import Path
import secrets
import socket
import subprocess
import tempfile
import time
import unittest
import uuid
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('backup11_local_fixture', Path(__file__).parents[1] / 'backup-restore-drill.py')
drill = importlib.util.module_from_spec(spec); spec.loader.exec_module(drill)


@unittest.skipUnless(os.environ.get('BACKUP11_LOCAL_CHECK') == '1', 'Explicit disposable PostgreSQL fixture approval required')
class BackupRestorePostgresTest(unittest.TestCase):
    def test_exports_shared_snapshot_restores_data_constraints_and_sequences(self):
        with socket.socket() as probe:
            probe.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            probe.bind(('127.0.0.1', 15437))
        env = drill.clean_environment()
        owner = uuid.uuid4().hex
        container = None
        with tempfile.TemporaryDirectory(prefix='backup11-fixture-', dir='/private/tmp') as folder:
            folder = Path(folder)
            password = secrets.token_urlsafe(40)
            settings = folder / 'fixture.env'
            drill.private_file(settings, 'POSTGRES_USER=backup11_source\nPOSTGRES_DB=backup11_source\nPOSTGRES_PASSWORD=' + password + '\n')
            try:
                result = subprocess.run(['docker', 'run', '-d', '--name', 'backup11-fixture-' + owner,
                    '--label', 'backup11.owner=' + owner, '--env-file', str(settings), '--tmpfs', '/var/lib/postgresql:rw',
                    '-p', '127.0.0.1:15437:5432', 'postgres:18'], env=env, capture_output=True, text=True)
                if result.returncode:
                    container = drill.recover_owned_container_id('backup11-fixture-' + owner, owner, env)
                    self.fail('Could not create disposable fixture')
                container = result.stdout.strip()
                for _ in range(60):
                    ready = subprocess.run(['docker', 'exec', container, 'pg_isready', '-U', 'backup11_source'], env=env, capture_output=True)
                    if ready.returncode == 0: break
                    time.sleep(0.5)
                else: self.fail('Disposable fixture readiness timed out')
                sql = """
                    CREATE TABLE empresas(id bigserial PRIMARY KEY,nombre text NOT NULL);
                    CREATE TABLE usuarios_empresa(id bigserial PRIMARY KEY,empresa_id bigint REFERENCES empresas(id),email text UNIQUE NOT NULL);
                    CREATE TABLE proyectos(id bigserial PRIMARY KEY,empresa_id bigint REFERENCES empresas(id),fecha timestamptz,importe numeric(12,2),detalle jsonb,archivo bytea);
                    CREATE TABLE flyway_schema_history(version text,success boolean);
                    INSERT INTO empresas(nombre) VALUES ('Empresa sintética');
                    INSERT INTO usuarios_empresa(empresa_id,email) VALUES (1,'fixture@example.invalid');
                    INSERT INTO proyectos(empresa_id,fecha,importe,detalle,archivo)
                      VALUES (1,'2026-10-28 12:00:00+00',123.45,'{"prueba":"á"}',decode('010203','hex'));
                    INSERT INTO flyway_schema_history VALUES ('8',true);
                """
                seeded = subprocess.run(['docker', 'exec', '-i', container, 'psql', '-X', '-q', '-v', 'ON_ERROR_STOP=1',
                    '-U', 'backup11_source', '-d', 'backup11_source'], input=sql, env=env, capture_output=True, text=True)
                self.assertEqual(seeded.returncode, 0, 'Could not seed disposable fixture')
                original = drill.make_services

                def local_services(temporary, config, target_password):
                    settings = original(temporary, config, target_password)
                    service = Path(settings['PGSERVICEFILE'])
                    content = service.read_text().replace('host=' + drill.SOURCE_HOST, 'host=127.0.0.1')
                    content = content.replace('port=5432', 'port=15437').replace('sslmode=verify-full', 'sslmode=disable').replace('sslrootcert=system\n', '')
                    service.write_text(content)
                    passfile = Path(settings['PGPASSFILE'])
                    passfile.write_text(passfile.read_text().replace(drill.SOURCE_HOST + ':5432:', '127.0.0.1:15437:'))
                    return settings

                config = {'database': 'backup11_source', 'expected_database': 'backup11_source',
                    'username': 'backup11_source', 'password': password}
                # Deliberate local-only test seam; production CLI still enforces exact Render host and TLS.
                with patch.object(drill, 'make_services', side_effect=local_services):
                    report = drill.drill(config)
                self.assertEqual(report['status'], 'verified')
                self.assertEqual(report['tables_verified'], 4)
                self.assertEqual(report['sequences_checked'], 3)
                self.assertFalse(report['archive_retained'])
            finally:
                if container: drill.cleanup_container(container, owner, env)


if __name__ == '__main__': unittest.main()
