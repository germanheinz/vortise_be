"""Opt-in local rehearsal with synthetic data only: disposable PostgreSQL containers, no Render, credentials or network."""
import importlib.util
import json
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

spec = importlib.util.spec_from_file_location('extract12_local', Path(__file__).parents[1] / 'extract-empresas.py')
extract = importlib.util.module_from_spec(spec)
spec.loader.exec_module(extract)
MIGRATIONS = Path(__file__).parents[2] / 'src/main/resources/db/migration'
SOURCE_PORT, TARGET_PORT = 15441, 15442
CUITS = ['20000000001', '20000000002']


def seed(companies=(1, 2, 3)):
    s = ["CREATE TABLE IF NOT EXISTS flyway_schema_history(installed_rank int, version text, success boolean);",
         "INSERT INTO flyway_schema_history VALUES (1,'21',true);",
         "UPDATE planes_suscripcion SET stripe_price_id='price_synthetic';"]
    for c in companies:
        e = c; u1, u2 = c*10+1, c*10+2; p = c*100; cat = c*10+1; ent = c*10+1; rub = c*10+1; rub2 = c*10+2; rest = c*10+1; cer = c*10+1; rep = c*10+1; con = c*10+1
        s += [
         f"INSERT INTO empresas(id,nombre,identificacion_fiscal) VALUES ({e},'Empresa {c}','2000000000{c}');",
         f"INSERT INTO usuarios_empresa(id,empresa_id,email,nombre,password_hash,rol) VALUES ({u1},{e},'admin{c}@example.invalid','Admin {c}','hash','ADMINISTRADOR'),({u2},{e},'user{c}@example.invalid','User {c}','hash','COLABORADOR');",
         f"INSERT INTO catalogos_empresa(id,empresa_id) VALUES ({cat},{e});",
         f"INSERT INTO responsables_empresa(id,empresa_id) VALUES ({c},{e});",
         f"INSERT INTO unidades_metricas_empresa(id,empresa_id) VALUES ({c},{e});",
         f"INSERT INTO categorias_mano_obra_empresa(id,empresa_id) VALUES ({c},{e});",
         f"INSERT INTO calendarios_laborales_empresa(id,empresa_id) VALUES ({c},{e});",
         f"INSERT INTO proyectos(id,empresa_id,nombre) VALUES ({p},{e},'Obra {c}');",
         f"INSERT INTO plantas(id,proyecto_id,nivel) VALUES ({c},{p},'PB');",
         f"INSERT INTO contratos_obra(id,proyecto_id,nombre,subcontratista_id) VALUES ({con},{p},'Contrato {c}',{cat});",
         f"INSERT INTO rubros_obra(id,proyecto_id,subcontratista_id,contrato_id) VALUES ({rub},{p},{cat},{con}),({rub2},{p},NULL,NULL);",
         f"UPDATE rubros_obra SET parent_id={rub} WHERE id={rub2};",
         f"INSERT INTO proyecto_subcontratistas(proyecto_id,catalogo_empresa_id) VALUES ({p},{cat});",
         f"INSERT INTO obra_tareas(id,proyecto_id,rubro_id) VALUES ({c},{p},{rub});",
         f"INSERT INTO restricciones_obra(id,proyecto_id,rubro_id) VALUES ({rest},{p},{rub});",
         f"INSERT INTO restriccion_comentarios(id,restriccion_id) VALUES ({c},{rest});",
         f"INSERT INTO registros_horas(id,proyecto_id,inicio,fin,horas) VALUES ({c},{p},'2026-01-01 08:00+00','2026-01-01 16:00+00',8);",
         f"INSERT INTO presupuesto_lineas(id,proyecto_id,etapa,tipo_superficie,funcion) VALUES ({c},{p},'e','t','f');",
         f"INSERT INTO ingresos(id,proyecto_id,concepto,monto) VALUES ({c},{p},'i',1);",
         f"INSERT INTO costos_indirectos(id,proyecto_id,concepto,monto) VALUES ({c},{p},'c',1);",
         f"INSERT INTO entregas(id,proyecto_id,nombre) VALUES ({ent},{p},'Entrega');",
         f"INSERT INTO tareas(id,entrega_id,nombre) VALUES ({c},{ent},'Tarea');",
         f"INSERT INTO certificados_obra(id,proyecto_id,periodicidad,numero,etiqueta,fecha_corte,estado,creado_en,subcontratista_id) VALUES ({cer},{p},'MENSUAL',1,'e','2026-01-31','ABIERTO','2026-01-01 00:00',{cat});",
         f"INSERT INTO certificado_items_completados(id,certificado_id,rubro_id,marcado_en) VALUES ({c},{cer},{rub},'2026-01-01 00:00');",
         f"INSERT INTO reportes_emitidos(id,proyecto_id,numero,semana_desde,semana_hasta,fecha_corte,emitido_en,version_esquema,snapshot) VALUES ({rep},{p},1,'2026-01-01','2026-01-07','2026-01-07','2026-01-07 00:00',1,'{{}}');",
         f"INSERT INTO reporte_imagenes(id,proyecto_id,reporte_id,public_id,url,orden,subido_en) VALUES ({c},{p},{rep},'p{c}','u',1,'2026-01-01 00:00');",
        ]
    # relations that cross companies or are removed on purpose
    s += [
         "UPDATE usuarios_empresa SET invitante_id=31 WHERE id=11;",
         "UPDATE usuarios_empresa SET invitante_id=32 WHERE id=21;",
         "UPDATE empresas SET creado_por_usuario_id=31 WHERE id=2;",
         "UPDATE empresas SET creado_por_usuario_id=11 WHERE id=1;",
         "INSERT INTO usuario_empresa_accesos(usuario_id,empresa_id) VALUES (12,1),(12,2),(12,3),(32,1);",
         "INSERT INTO invitaciones_equipo(id,token_hash,titular_id,creado_en,vence_en) VALUES (1,'t1',11,now(),now()),(2,'t2',31,now(),now()),(3,'t3',21,now(),now());",
         "INSERT INTO invitacion_empresas(invitacion_id,empresa_id) VALUES (1,1),(1,3),(2,1),(3,3);",
         "INSERT INTO suscripciones(id,titular,stripe_customer_id,usuario_id,empresa_id,plan_id) VALUES (1,'U','cus_1',11,1,1);",
         "INSERT INTO pagos_suscripcion(id,suscripcion_id,stripe_invoice_id,monto_centavos,moneda,estado,fecha) VALUES (1,1,'in_1',5000,'USD','PAID',now());",
         "INSERT INTO devoluciones_pago(pago_id,stripe_refund_id,monto_centavos,estado,fecha) VALUES (1,'re_1',100,'OK',now());",
         "INSERT INTO stripe_eventos_procesados(event_id,tipo) VALUES ('evt_1','t');",
         "INSERT INTO password_reset_tokens(usuario_id,token_hash,creado_en,vence_en,session_version) VALUES (11,'x',now(),now(),0);",
         "INSERT INTO password_reset_quotas(clave,inicio,solicitudes) VALUES ('k',now(),1);",
         "INSERT INTO gastos_fijos(id,concepto,monto) VALUES (1,'g',1);",
         "INSERT INTO rendimientos(id,hh_porm2,precio_por_hh) VALUES (1,1,1);"]
    s.append("""DO $$ DECLARE r record; BEGIN FOR r IN SELECT c.relname AS t, a.attname AS col, pg_get_serial_sequence(format('%I.%I','public',c.relname),a.attname) AS seq FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND c.relkind='r' AND a.attnum>0 AND NOT a.attisdropped AND pg_get_serial_sequence(format('%I.%I','public',c.relname),a.attname) IS NOT NULL LOOP EXECUTE format('SELECT setval(%L, coalesce((SELECT max(%I) FROM public.%I),0)+1, false)', r.seq, r.col, r.t); END LOOP; END $$;""")
    return "\n".join(s)


class Container:
    def __init__(self, env, port, password):
        self.env, self.port, self.password, self.owner, self.id = env, port, password, uuid.uuid4().hex, None

    def __enter__(self):
        with socket.socket() as probe:
            probe.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            probe.bind(('127.0.0.1', self.port))
        started = subprocess.run(['docker', 'run', '-d', '--name', 'extract12-fixture-' + self.owner, '--label', 'extract12.owner=' + self.owner,
            '-e', 'POSTGRES_USER=fixture', '-e', 'POSTGRES_DB=fixture', '-e', 'POSTGRES_PASSWORD=' + self.password,
            '--tmpfs', '/var/lib/postgresql:rw', '-p', '127.0.0.1:%d:5432' % self.port, 'postgres:18'], env=self.env, capture_output=True, text=True)
        if started.returncode:
            raise AssertionError('Could not create disposable fixture')
        self.id = started.stdout.strip()
        for _ in range(60):
            if subprocess.run(['docker', 'exec', self.id, 'pg_isready', '-h', '127.0.0.1', '-U', 'fixture', '-d', 'fixture'], env=self.env, capture_output=True).returncode == 0:
                time.sleep(1)
                return self
            time.sleep(0.5)
        raise AssertionError('Fixture readiness timed out')

    def __exit__(self, *failure):
        if self.id:
            label = subprocess.run(['docker', 'inspect', '--format', '{{index .Config.Labels "extract12.owner"}}', self.id], env=self.env, capture_output=True, text=True)
            if label.stdout.strip() == self.owner:
                subprocess.run(['docker', 'rm', '-f', self.id], env=self.env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    def sql(self, text, *, output=None):
        return subprocess.run(['docker', 'exec', '-i', self.id, 'psql', '-X', '-q', '-A', '-t', '-v', 'ON_ERROR_STOP=1', '-U', 'fixture', '-d', 'fixture'],
            input=text, env=self.env, capture_output=True, text=True, check=True).stdout.strip()

    def dump(self, path):
        with open(path, 'wb') as target:
            subprocess.run(['docker', 'exec', self.id, 'pg_dump', '-U', 'fixture', '-d', 'fixture', '--format=custom'], env=self.env, stdout=target, check=True)
        path.chmod(0o600)


@unittest.skipUnless(os.environ.get('EXTRACT12_LOCAL_CHECK') == '1', 'Explicit disposable PostgreSQL fixture approval required')
class ExtractPostgresTest(unittest.TestCase):
    def setUp(self):
        self.env = extract.tool_environment(extract.drill.clean_environment())
        self.folder = tempfile.TemporaryDirectory(prefix='extract12-fixture-', dir='/private/tmp')
        self.addCleanup(self.folder.cleanup)
        self.path = Path(self.folder.name)

    def source(self, source, extra=''):
        for script in sorted(MIGRATIONS.glob('V*.sql'), key=lambda item: int(item.name[1:].split('__')[0])):
            source.sql(script.read_text())
        source.sql(seed())
        if extra:
            source.sql(extra)
        dump = self.path / ('full-%s.dump' % uuid.uuid4().hex)
        source.dump(dump)
        return dump

    def test_keeps_only_requested_companies_and_everything_related_then_applies_to_an_empty_target(self):
        with Container(self.env, SOURCE_PORT, secrets.token_urlsafe(24)) as source:
            dump = self.source(source)
        pruned, report = self.path / 'pruned.dump', self.path / 'report.json'
        summary = extract.prune(dump, CUITS, pruned, report)
        self.assertEqual(summary['status'], 'pruned_verified')
        counts = summary['per_table']
        self.assertEqual((counts['empresas'], counts['usuarios_empresa'], counts['proyectos']), (2, 5, 2))
        self.assertEqual(summary['users_rehomed'], 1)
        self.assertEqual((counts['restricciones_obra'], counts['restriccion_comentarios'], counts['registros_horas']), (2, 2, 2))
        self.assertEqual((counts['rubros_obra'], counts['certificados_obra'], counts['reporte_imagenes']), (4, 2, 2))
        self.assertEqual((counts['usuario_empresa_accesos'], counts['invitaciones_equipo'], counts['invitacion_empresas']), (2, 2, 2))
        for removed in ('suscripciones', 'pagos_suscripcion', 'devoluciones_pago', 'stripe_eventos_procesados', 'password_reset_tokens',
                'password_reset_quotas', 'gastos_fijos', 'rendimientos'):
            self.assertEqual(counts.get(removed, 0), 0, removed)
        self.assertEqual(oct(pruned.stat().st_mode & 0o777), '0o600')
        self.assertEqual(oct(report.stat().st_mode & 0o777), '0o600')
        self.assertNotIn('example.invalid', report.read_text())

        password = secrets.token_urlsafe(24)
        with Container(self.env, TARGET_PORT, password) as target:
            def local_target(folder, config):
                passfile = folder / 'target-passwords'
                extract.drill.private_file(passfile, '127.0.0.1:%d:fixture:fixture:%s\n' % (TARGET_PORT, password))
                service = folder / 'target-services.conf'
                extract.drill.private_file(service, '[target]\nhost=127.0.0.1\nport=%d\ndbname=fixture\nuser=fixture\nsslmode=disable\n' % TARGET_PORT)
                env = dict(self.env, PGSERVICEFILE=str(service), PGPASSFILE=str(passfile), PGTZ='UTC', PGDATESTYLE='ISO, YMD', PGCLIENTENCODING='UTF8')
                return env
            config = {'host': 'local', 'database': 'fixture', 'expected_database': 'fixture', 'username': 'fixture', 'password': 'unused'}
            # Deliberate local-only seam; the real CLI still pins a distinct Render host with verified TLS.
            with patch.object(extract, 'target_services', side_effect=local_target):
                result = extract.apply(config, pruned, report, extract.sha256_file(pruned))
                self.assertEqual(result['status'], 'target_verified')
                self.assertEqual(target.sql('SELECT string_agg(identificacion_fiscal, \',\' ORDER BY identificacion_fiscal) FROM empresas;'), ','.join(CUITS))
                self.assertEqual(target.sql('SELECT count(*) FROM usuarios_empresa WHERE empresa_id NOT IN (1,2);'), '0')
                # The creator of company 2 belonged to a company that is not migrated: kept and moved into company 2.
                self.assertEqual(target.sql('SELECT empresa_id FROM usuarios_empresa WHERE id = 31;'), '2')
                self.assertEqual(target.sql('SELECT creado_por_usuario_id FROM empresas WHERE id = 2;'), '31')
                self.assertEqual(target.sql('SELECT count(*) FROM planes_suscripcion WHERE stripe_price_id IS NOT NULL;'), '0')
                # An inviter that is kept stays; one that is not migrated is cleared. A creator of a kept company is always kept.
                self.assertEqual(target.sql('SELECT invitante_id FROM usuarios_empresa WHERE id = 11;'), '31')
                self.assertEqual(target.sql('SELECT invitante_id IS NULL FROM usuarios_empresa WHERE id = 21;'), 't')
                self.assertEqual(target.sql('SELECT creado_por_usuario_id FROM empresas WHERE id = 1;'), '11')
                self.assertEqual(target.sql('SELECT count(*) FROM usuarios_empresa WHERE id IN (32, 33);'), '0')
                with self.assertRaises(extract.DrillError):
                    extract.apply(config, pruned, report, extract.sha256_file(pruned))

    def test_aborts_and_publishes_nothing_when_a_kept_row_references_another_company(self):
        crossing = 'UPDATE rubros_obra SET subcontratista_id = 31 WHERE id = 11;'
        with Container(self.env, SOURCE_PORT, secrets.token_urlsafe(24)) as source:
            dump = self.source(source, crossing)
        pruned, report = self.path / 'pruned.dump', self.path / 'report.json'
        with self.assertRaises(extract.DrillError) as failure:
            extract.prune(dump, CUITS, pruned, report)
        self.assertIn('Referential integrity', str(failure.exception))
        self.assertFalse(pruned.exists() or report.exists())

    def test_aborts_when_a_kept_company_would_have_no_member_and_no_creator(self):
        orphaned = 'UPDATE usuarios_empresa SET empresa_id = 3 WHERE empresa_id = 1; UPDATE empresas SET creado_por_usuario_id = NULL WHERE id = 1;'
        with Container(self.env, SOURCE_PORT, secrets.token_urlsafe(24)) as source:
            dump = self.source(source, orphaned)
        pruned, report = self.path / 'pruned.dump', self.path / 'report.json'
        with self.assertRaises(extract.DrillError) as failure:
            extract.prune(dump, CUITS, pruned, report)
        self.assertIn('nobody could open it', str(failure.exception))
        self.assertFalse(pruned.exists() or report.exists())

    def test_aborts_before_pruning_when_the_schema_has_an_unclassified_table(self):
        with Container(self.env, SOURCE_PORT, secrets.token_urlsafe(24)) as source:
            dump = self.source(source, 'CREATE TABLE nueva_tabla(id int);')
        with self.assertRaises(extract.DrillError) as failure:
            extract.prune(dump, CUITS, self.path / 'pruned.dump', self.path / 'report.json')
        self.assertIn('nueva_tabla', str(failure.exception))

    def test_aborts_when_a_requested_company_is_missing(self):
        with Container(self.env, SOURCE_PORT, secrets.token_urlsafe(24)) as source:
            dump = self.source(source)
        with self.assertRaises(extract.DrillError):
            extract.prune(dump, ['20000000001', '20999999999'], self.path / 'pruned.dump', self.path / 'report.json')


if __name__ == '__main__':
    unittest.main()
