#!/usr/bin/env python3
"""Selective company extraction: private full dump -> disposable PostgreSQL 18 -> pruned dump -> empty target.

Subcommands (opt-in, each needs its own approved execution scope):
  configure-source / configure-target  private 600 connection files, hidden prompt, no connection
  backup   read-only snapshot of the confirmed Render source into a private retained dump
  prune    restore a dump locally, keep only the requested companies and verify the result
  apply    restore the verified pruned dump into an empty, identity-checked target

Only counts, identifiers and hashes are ever printed; never rows, credentials or connection details.
"""
import argparse
import getpass
import hashlib
import importlib.util
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
from urllib.parse import urlsplit, unquote

_spec = importlib.util.spec_from_file_location('backup_restore_drill', Path(__file__).with_name('backup-restore-drill.py'))
drill = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(drill)
DrillError = drill.DrillError
REPO = Path(__file__).resolve().parents[2]
WORK_PORT = 15440
WORK_DB = 'extract12_work'
LIBPQ = '/opt/homebrew/opt/libpq/bin'
TARGET_HOST = re.compile(r'(dpg-[a-z0-9]+-a)\.[a-z0-9-]+-postgres\.render\.com')
CUIT = re.compile(r'[0-9]{11}')

# Tables owned through one foreign key, parents before children.
OWNERSHIP = [
    ('usuarios_empresa', 'empresa_id', 'empresas'),
    ('catalogos_empresa', 'empresa_id', 'empresas'),
    ('responsables_empresa', 'empresa_id', 'empresas'),
    ('unidades_metricas_empresa', 'empresa_id', 'empresas'),
    ('categorias_mano_obra_empresa', 'empresa_id', 'empresas'),
    ('calendarios_laborales_empresa', 'empresa_id', 'empresas'),
    ('proyectos', 'empresa_id', 'empresas'),
    ('plantas', 'proyecto_id', 'proyectos'),
    ('contratos_obra', 'proyecto_id', 'proyectos'),
    ('rubros_obra', 'proyecto_id', 'proyectos'),
    ('proyecto_subcontratistas', 'proyecto_id', 'proyectos'),
    ('obra_tareas', 'proyecto_id', 'proyectos'),
    ('restricciones_obra', 'proyecto_id', 'proyectos'),
    ('registros_horas', 'proyecto_id', 'proyectos'),
    ('presupuesto_lineas', 'proyecto_id', 'proyectos'),
    ('ingresos', 'proyecto_id', 'proyectos'),
    ('costos_indirectos', 'proyecto_id', 'proyectos'),
    ('entregas', 'proyecto_id', 'proyectos'),
    ('certificados_obra', 'proyecto_id', 'proyectos'),
    ('reportes_emitidos', 'proyecto_id', 'proyectos'),
    ('reporte_imagenes', 'proyecto_id', 'proyectos'),
    ('tareas', 'entrega_id', 'entregas'),
    ('restriccion_comentarios', 'restriccion_id', 'restricciones_obra'),
    ('certificado_items_completados', 'certificado_id', 'certificados_obra'),
]
PARENT = {table: (column, parent) for table, column, parent in OWNERSHIP}
# Tables with two ownership references or a rule of their own.
SPECIAL = ['empresas', 'usuario_empresa_accesos', 'usuario_obra', 'invitaciones_equipo', 'invitacion_empresas']
# Cleared children first. Stripe sandbox, ephemeral reset data and ownerless global tables.
EXCLUDED = ['devoluciones_pago', 'pagos_suscripcion', 'stripe_eventos_procesados', 'suscripciones',
            'password_reset_tokens', 'password_reset_quotas', 'gastos_fijos', 'rendimientos']
KEEP_ALL = ['planes_suscripcion', 'flyway_schema_history']
CLASSIFIED = {table for table, _, _ in OWNERSHIP} | set(SPECIAL) | set(EXCLUDED) | set(KEEP_ALL)


def q(name):
    return 'public.' + drill.ident(name)


def validate_cuits(cuits):
    unique = sorted(set(cuits))
    if not unique or len(unique) != len(cuits) or any(not CUIT.fullmatch(value) for value in unique):
        raise DrillError('Provide distinct 11-digit tax identifiers')
    return unique


def literals(cuits):
    return ', '.join("'" + value + "'" for value in validate_cuits(cuits))


def unclassified(tables):
    return sorted(set(tables) - CLASSIFIED)


def prune_statements(cuits):
    lits = literals(cuits)
    statements = ['DELETE FROM public.empresas WHERE identificacion_fiscal IS NULL OR identificacion_fiscal NOT IN (' + lits + ');',
        # An administrator reaches the companies they created. Creators that belong to another company are kept and
        # moved to the lowest-id kept company they created, so they still have a company in the copy.
        'UPDATE public.usuarios_empresa u SET empresa_id = (SELECT min(e.id) FROM public.empresas e WHERE e.creado_por_usuario_id = u.id)'
        ' WHERE u.empresa_id NOT IN (SELECT id FROM public.empresas)'
        ' AND EXISTS (SELECT 1 FROM public.empresas e WHERE e.creado_por_usuario_id = u.id);']
    for table, column, parent in OWNERSHIP:
        statements.append('DELETE FROM %s WHERE %s IS NULL OR %s NOT IN (SELECT id FROM %s);'
            % (q(table), drill.ident(column), drill.ident(column), q(parent)))
    statements += [
        'DELETE FROM public.usuario_empresa_accesos WHERE usuario_id NOT IN (SELECT id FROM public.usuarios_empresa)'
        ' OR empresa_id NOT IN (SELECT id FROM public.empresas);',
        'DELETE FROM public.usuario_obra WHERE usuario_id NOT IN (SELECT id FROM public.usuarios_empresa)'
        ' OR proyecto_id NOT IN (SELECT id FROM public.proyectos);',
        'DELETE FROM public.invitaciones_equipo WHERE titular_id NOT IN (SELECT id FROM public.usuarios_empresa);',
        'DELETE FROM public.invitacion_empresas WHERE invitacion_id NOT IN (SELECT id FROM public.invitaciones_equipo)'
        ' OR empresa_id NOT IN (SELECT id FROM public.empresas);',
        'DELETE FROM public.invitaciones_equipo WHERE id NOT IN (SELECT invitacion_id FROM public.invitacion_empresas);']
    statements += ['DELETE FROM %s;' % q(table) for table in EXCLUDED]
    statements += [
        'UPDATE public.usuarios_empresa SET invitante_id = NULL WHERE invitante_id IS NOT NULL'
        ' AND invitante_id NOT IN (SELECT id FROM public.usuarios_empresa);',
        'UPDATE public.empresas SET creado_por_usuario_id = NULL WHERE creado_por_usuario_id IS NOT NULL'
        ' AND creado_por_usuario_id NOT IN (SELECT id FROM public.usuarios_empresa);',
        'UPDATE public.planes_suscripcion SET stripe_price_id = NULL;']
    return statements


def kept_user(alias, lits):
    """Users that remain: members of a kept company, or creators of one."""
    return ('(%(a)s.empresa_id IN (SELECT id FROM public.empresas WHERE identificacion_fiscal IN (%(l)s))'
        ' OR %(a)s.id IN (SELECT creado_por_usuario_id FROM public.empresas WHERE identificacion_fiscal IN (%(l)s)'
        ' AND creado_por_usuario_id IS NOT NULL))') % {'a': alias, 'l': lits}


def rehomed_sql(lits):
    return ('SELECT count(*) FROM public.usuarios_empresa u WHERE u.empresa_id NOT IN (SELECT id FROM public.empresas'
        ' WHERE identificacion_fiscal IN (%s)) AND EXISTS (SELECT 1 FROM public.empresas e WHERE e.creado_por_usuario_id = u.id'
        ' AND e.identificacion_fiscal IN (%s));') % (lits, lits)


# A kept company nobody could open: no member of its own and no kept creator.
UNREACHABLE_SQL = ('SELECT count(*) FROM public.empresas e WHERE NOT EXISTS (SELECT 1 FROM public.usuarios_empresa u WHERE u.empresa_id = e.id)'
    ' AND (e.creado_por_usuario_id IS NULL OR NOT EXISTS (SELECT 1 FROM public.usuarios_empresa c WHERE c.id = e.creado_por_usuario_id));')


def expected_sql(table, cuits):
    """Independent formulation of what must remain: joins up to the company, not the anti-joins used to delete."""
    lits = literals(cuits)
    if table == 'empresas':
        return 'SELECT count(*) FROM public.empresas WHERE identificacion_fiscal IN (' + lits + ');'
    if table == 'usuarios_empresa':
        return 'SELECT count(*) FROM public.usuarios_empresa u WHERE ' + kept_user('u', lits) + ';'
    if table in PARENT:
        joins, current, index = [], table, 0
        while current != 'empresas':
            column, parent = PARENT[current]
            index += 1
            joins.append('JOIN %s t%d ON t%d.%s = t%d.id' % (q(parent), index, index - 1, drill.ident(column), index))
            current = parent
        return 'SELECT count(*) FROM %s t0 %s WHERE t%d.identificacion_fiscal IN (%s);' % (q(table), ' '.join(joins), index, lits)
    if table == 'usuario_empresa_accesos':
        return ('SELECT count(*) FROM public.usuario_empresa_accesos a JOIN public.usuarios_empresa u ON a.usuario_id = u.id'
            ' JOIN public.empresas ae ON a.empresa_id = ae.id WHERE %s AND ae.identificacion_fiscal IN (%s);' % (kept_user('u', lits), lits))
    if table == 'usuario_obra':
        return ('SELECT count(*) FROM public.usuario_obra o JOIN public.usuarios_empresa u ON o.usuario_id = u.id'
            ' JOIN public.proyectos p ON o.proyecto_id = p.id JOIN public.empresas pe ON p.empresa_id = pe.id'
            ' WHERE %s AND pe.identificacion_fiscal IN (%s);' % (kept_user('u', lits), lits))
    if table == 'invitaciones_equipo':
        return ('SELECT count(DISTINCT i.id) FROM public.invitaciones_equipo i JOIN public.usuarios_empresa u ON i.titular_id = u.id'
            ' JOIN public.invitacion_empresas ie ON ie.invitacion_id = i.id JOIN public.empresas e ON ie.empresa_id = e.id'
            ' WHERE %s AND e.identificacion_fiscal IN (%s);' % (kept_user('u', lits), lits))
    if table == 'invitacion_empresas':
        return ('SELECT count(*) FROM public.invitacion_empresas ie JOIN public.invitaciones_equipo i ON ie.invitacion_id = i.id'
            ' JOIN public.usuarios_empresa u ON i.titular_id = u.id JOIN public.empresas e ON ie.empresa_id = e.id'
            ' WHERE %s AND e.identificacion_fiscal IN (%s);' % (kept_user('u', lits), lits))
    if table in EXCLUDED:
        return 'SELECT 0;'
    return 'SELECT count(*) FROM %s;' % q(table)


FOREIGN_KEYS = """SELECT conrelid::regclass::text||'|'||confrelid::regclass::text||'|'||
 (SELECT string_agg(quote_ident(a.attname),',' ORDER BY k.ord) FROM unnest(conkey) WITH ORDINALITY k(attnum,ord)
   JOIN pg_attribute a ON a.attrelid=conrelid AND a.attnum=k.attnum)||'|'||
 (SELECT string_agg(quote_ident(a.attname),',' ORDER BY k.ord) FROM unnest(confkey) WITH ORDINALITY k(attnum,ord)
   JOIN pg_attribute a ON a.attrelid=confrelid AND a.attnum=k.attnum)
 FROM pg_constraint WHERE contype='f' AND connamespace='public'::regnamespace ORDER BY 1;"""


def orphan_sql(child, parent, child_columns, parent_columns):
    children, parents = child_columns.split(','), parent_columns.split(',')
    present = ' AND '.join('c.%s IS NOT NULL' % column for column in children)
    match = ' AND '.join('p.%s = c.%s' % (pc, cc) for pc, cc in zip(parents, children))
    return 'SELECT count(*) FROM %s c WHERE %s AND NOT EXISTS (SELECT 1 FROM %s p WHERE %s);' % (child, present, parent, match)


def count_orphans(session):
    rows = session.scalar(FOREIGN_KEYS).splitlines()
    for row in rows:
        child, parent, child_columns, parent_columns = row.split('|')
        for value in (child, parent):
            if not re.fullmatch(r'(public\.)?"?[a-z_][a-z0-9_]*"?', value):
                raise DrillError('Unexpected relation identifier')
        for value in (child_columns, parent_columns):
            if not re.fullmatch(r'[a-z_"][a-z0-9_",]*', value):
                raise DrillError('Unexpected column identifier')
        if int(session.scalar(orphan_sql(child, parent, child_columns, parent_columns))):
            raise DrillError('Referential integrity failed after pruning: a kept row references a removed row; nothing was published')
    return len(rows)


def tool_environment(env):
    result = dict(env)
    result['PATH'] = os.environ.get('PATH', '') + os.pathsep + LIBPQ
    for key in ('DOCKER_HOST', 'DOCKER_CONFIG'):
        if key in os.environ:
            result[key] = os.environ[key]
    return result


def private_output(path):
    path = Path(path).absolute()
    if REPO in path.resolve().parents or path.exists() or path.is_symlink() or not path.parent.is_dir():
        raise DrillError('Choose a new private file outside the workspace in an existing folder')
    drill.private_file(path, '')
    return path


def sha256_file(path):
    digest = hashlib.sha256()
    with open(path, 'rb') as source:
        for block in iter(lambda: source.read(1024 * 1024), b''):
            digest.update(block)
    return digest.hexdigest()


def read_private(path):
    path = Path(path).absolute()
    if path.is_symlink() or REPO in path.resolve().parents or not path.is_file():
        raise DrillError('Private file must be a regular file outside the repository')
    info = path.stat()
    if info.st_uid != os.getuid() or (info.st_mode & 0o077):
        raise DrillError('Private file requires owner-only permissions')
    return path


# --- configuration ---------------------------------------------------------------------------------------------

def configure_target(path):
    path = Path(path).absolute()
    if REPO in path.resolve().parents or path.exists() or path.is_symlink():
        raise DrillError('Choose a new private configuration outside the workspace')
    url = urlsplit(getpass.getpass('External Database URL of the NEW empty production database (hidden input): '))
    match = TARGET_HOST.fullmatch(url.hostname or '')
    if url.scheme not in ('postgres', 'postgresql') or not match or url.port not in (None, 5432):
        raise DrillError('URL must identify a Render PostgreSQL host')
    if match.group(1) == drill.SOURCE_ID:
        raise DrillError('Target must not be the source database')
    database = unquote(url.path.removeprefix('/'))
    config = {'target_id': match.group(1), 'host': url.hostname, 'database': database, 'expected_database': database,
        'username': unquote(url.username or ''), 'password': unquote(url.password or '')}
    if any(not config[key] or any(c in config[key] for c in '\r\n\x00') for key in ('database', 'username', 'password')):
        raise DrillError('Incomplete private connection URL')
    drill.private_file(path, json.dumps(config))
    print('Private target configuration created; no database connection was opened.')


def read_target_config(path):
    path = read_private(path)
    config = json.loads(path.read_text())
    match = TARGET_HOST.fullmatch(config.get('host', ''))
    if not match or match.group(1) != config.get('target_id') or config['target_id'] == drill.SOURCE_ID:
        raise DrillError('Target configuration does not identify a distinct Render database')
    for key in ('database', 'expected_database', 'username', 'password'):
        if not isinstance(config.get(key), str) or not config[key] or any(c in config[key] for c in '\r\n\x00'):
            raise DrillError('Invalid private target configuration')
    if config['database'] != config['expected_database']:
        raise DrillError('Target database does not match its approved identity')
    return config


def target_services(folder, config):
    passfile = folder / 'target-passwords'
    escape = lambda value: value.replace('\\', '\\\\').replace(':', '\\:')
    drill.private_file(passfile, ':'.join(map(escape, [config['host'], '5432', config['database'], config['username'], config['password']])) + '\n')
    service = folder / 'target-services.conf'
    drill.private_file(service, '\n'.join(['[target]', 'host=' + drill.service_value(config['host']), 'port=5432',
        'dbname=' + drill.service_value(config['database']), 'user=' + drill.service_value(config['username']),
        'sslmode=verify-full', 'sslrootcert=system', 'connect_timeout=10', 'application_name=extract_12_apply', '']))
    env = tool_environment(drill.clean_environment())
    env.update({'PGSERVICEFILE': str(service), 'PGPASSFILE': str(passfile), 'PGOPTIONS': '-c statement_timeout=300000 -c lock_timeout=5000',
        'PGTZ': 'UTC', 'PGDATESTYLE': 'ISO, YMD', 'PGCLIENTENCODING': 'UTF8'})
    return env


def work_services(folder, password):
    passfile = folder / 'work-passwords'
    drill.private_file(passfile, '127.0.0.1:%d:%s:%s:%s\n' % (WORK_PORT, WORK_DB, WORK_DB, password))
    service = folder / 'work-services.conf'
    drill.private_file(service, '\n'.join(['[work]', 'host=127.0.0.1', 'port=%d' % WORK_PORT, 'dbname=' + WORK_DB,
        'user=' + WORK_DB, 'sslmode=disable', 'connect_timeout=5', '']))
    env = tool_environment(drill.clean_environment())
    env.update({'PGSERVICEFILE': str(service), 'PGPASSFILE': str(passfile), 'PGOPTIONS': '-c statement_timeout=300000 -c lock_timeout=5000',
        'PGTZ': 'UTC', 'PGDATESTYLE': 'ISO, YMD', 'PGCLIENTENCODING': 'UTF8'})
    return env


# --- backup ----------------------------------------------------------------------------------------------------

def discard_on_failure(paths, action):
    try:
        return action()
    except BaseException:
        for path in paths:
            path.unlink(missing_ok=True)
        raise


def backup(config, output):
    output = private_output(output)
    return discard_on_failure([output], lambda: _backup(config, output))


def _backup(config, output):
    with tempfile.TemporaryDirectory(prefix='extract12-', dir='/private/tmp') as temporary:
        folder = Path(temporary)
        env = tool_environment(drill.make_services(folder, config, secrets.token_urlsafe(24)))
        source = None
        try:
            source = drill.Session('source', env, folder)
            source.scalar('BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY;')
            if source.scalar('SELECT current_database();') != config['expected_database']:
                raise DrillError('Connected source identity mismatch')
            if source.scalar('SHOW transaction_read_only;') != 'on':
                raise DrillError('Source session is not read-only')
            if source.scalar('SHOW server_version_num;')[:2] != '18':
                raise DrillError('Source major must be PostgreSQL18')
            if int(source.scalar('SELECT pg_database_size(current_database());')) > drill.MAX_SOURCE_BYTES:
                raise DrillError('Source exceeds the128MiB size budget; ask the owner before raising it')
            snapshot = source.scalar('SELECT pg_export_snapshot();')
            if not re.fullmatch(r'[0-9A-Fa-f]+-[0-9A-Fa-f]+-[0-9]+', snapshot):
                raise DrillError('Invalid exported snapshot identity')
            drill.command(['pg_dump', '--dbname=service=source', '--format=custom', '--file=' + str(output), '--snapshot=' + snapshot,
                '--lock-wait-timeout=5s', '--no-publications', '--no-subscriptions'], env, folder, 'export')
            output.chmod(0o600)
            drill.command(['pg_restore', '--list', str(output)], env, folder, 'archive-list')
            tables = len(source.scalar(drill.TABLES).splitlines())
        finally:
            if source:
                source.close()
    return {'status': 'backup_verified', 'source_id': drill.SOURCE_ID, 'tables': tables,
        'archive_bytes': output.stat().st_size, 'archive_sha256': sha256_file(output), 'source_writes': False}


# --- disposable work database ------------------------------------------------------------------------------------

class WorkDatabase:
    def __init__(self, env, folder):
        self.env, self.folder, self.owner, self.container = env, folder, uuid.uuid4().hex, None

    def start(self, password):
        with socket.socket() as probe:
            probe.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            probe.bind(('127.0.0.1', WORK_PORT))
        envfile = self.folder / 'docker.env'
        drill.private_file(envfile, 'POSTGRES_USER=%s\nPOSTGRES_DB=%s\nPOSTGRES_PASSWORD=%s\n' % (WORK_DB, WORK_DB, password))
        created = subprocess.run(['docker', 'run', '-d', '--name', 'extract12-' + self.owner, '--label', 'extract12.owner=' + self.owner,
            '--env-file', str(envfile), '--tmpfs', '/var/lib/postgresql:rw', '-p', '127.0.0.1:%d:5432' % WORK_PORT, 'postgres:18'],
            env=self.env, capture_output=True, text=True)
        if created.returncode:
            self.container = self.recover('extract12-' + self.owner)
            raise DrillError('Could not create exclusive work database')
        self.container = created.stdout.strip()
        inspected = json.loads(subprocess.run(['docker', 'inspect', self.container], env=self.env, capture_output=True, text=True, check=True).stdout)[0]
        if (inspected['Config']['Labels'].get('extract12.owner') != self.owner
                or inspected['HostConfig']['PortBindings'].get('5432/tcp') != [{'HostIp': '127.0.0.1', 'HostPort': str(WORK_PORT)}]
                or '/var/lib/postgresql' not in inspected['HostConfig'].get('Tmpfs', {})):
            raise DrillError('Work database identity mismatch')
        for _ in range(60):
            ready = subprocess.run(['docker', 'exec', self.container, 'pg_isready', '-h', '127.0.0.1', '-U', WORK_DB, '-d', WORK_DB],
                env=self.env, capture_output=True)
            if ready.returncode == 0:
                probe = subprocess.run(['psql', '-X', '-q', '-A', '-t', 'service=work', '-c', 'SELECT 1;'], env=self.env, capture_output=True)
                if probe.returncode == 0 and probe.stdout.strip() == b'1':
                    return
            time.sleep(0.5)
        raise DrillError('Work database readiness timed out')

    def recover(self, name):
        checked = subprocess.run(['docker', 'inspect', '--format', '{{.Id}} {{index .Config.Labels "extract12.owner"}}', name],
            env=self.env, capture_output=True, text=True)
        if checked.returncode:
            return None
        fields = checked.stdout.strip().split()
        if len(fields) != 2 or not re.fullmatch(r'[a-f0-9]{64}', fields[0]) or fields[1] != self.owner:
            raise DrillError('Failed creation left an unverified resource; cleanup refused')
        return fields[0]

    def remove(self):
        if not self.container:
            return
        checked = subprocess.run(['docker', 'inspect', '--format', '{{index .Config.Labels "extract12.owner"}}', self.container],
            env=self.env, capture_output=True, text=True)
        if checked.returncode != 0 or checked.stdout.strip() != self.owner:
            raise DrillError('Container ownership mismatch; cleanup refused')
        subprocess.run(['docker', 'rm', '-f', self.container], env=self.env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)


# --- prune -----------------------------------------------------------------------------------------------------

def prune(dump, cuits, output, report_path):
    cuits = validate_cuits(cuits)
    dump = read_private(dump)
    output, report_path = private_output(output), private_output(report_path)
    return discard_on_failure([output, report_path], lambda: _prune(dump, cuits, output, report_path))


def _prune(dump, cuits, output, report_path):
    source_hash = sha256_file(dump)
    with tempfile.TemporaryDirectory(prefix='extract12-', dir='/private/tmp') as temporary:
        folder = Path(temporary)
        password = secrets.token_urlsafe(40)
        env = work_services(folder, password)
        work, session = WorkDatabase(env, folder), None
        try:
            drill.command(['pg_restore', '--list', str(dump)], env, folder, 'archive-list')
            work.start(password)
            drill.command(['pg_restore', '--dbname=service=work', '--exit-on-error', '--single-transaction', '--no-owner', '--no-acl', str(dump)],
                env, folder, 'restore-full')
            session = drill.Session('work', env, folder)
            tables = session.scalar(drill.TABLES).splitlines()
            missing = unclassified(tables)
            if missing:
                raise DrillError('Unclassified tables, extend the extraction map first: ' + ', '.join(missing))
            if not {'empresas', 'usuarios_empresa', 'proyectos'} <= set(tables):
                raise DrillError('Dump is not the expected application schema')
            if 'flyway_schema_history' in tables and int(session.scalar('SELECT count(*) FROM public.flyway_schema_history WHERE NOT success;')):
                raise DrillError('Dump contains unsuccessful migration history')
            found = session.scalar('SELECT count(*) FROM public.empresas WHERE identificacion_fiscal IN (%s);' % literals(cuits))
            if int(found) != len(cuits):
                raise DrillError('The dump does not contain exactly the requested companies')
            before = {table: int(session.scalar('SELECT count(*) FROM %s;' % q(table))) for table in tables}
            rehomed = int(session.scalar(rehomed_sql(literals(cuits))))
            expected = {table: int(session.scalar(expected_sql(table, cuits))) for table in tables}
            session.scalar('BEGIN; SET LOCAL session_replication_role = replica;')
            for statement in prune_statements(cuits):
                session.scalar(statement)
            after = {table: int(session.scalar('SELECT count(*) FROM %s;' % q(table))) for table in tables}
            for table in tables:
                if after[table] != expected[table]:
                    raise DrillError('Row count differs from the independent expectation for table ' + table + '; nothing was published')
            if session.scalar("SELECT count(*) FROM public.planes_suscripcion WHERE stripe_price_id IS NOT NULL;") != '0':
                raise DrillError('Sandbox price identifiers remain')
            orphans = count_orphans(session)
            if int(session.scalar(UNREACHABLE_SQL)):
                raise DrillError('A kept company would have no member and no kept creator, so nobody could open it; decide which user to carry first. Nothing was published')
            session.scalar('COMMIT;')
            session.close(); session = None
            session = drill.Session('work', env, folder)
            session.scalar('BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY;')
            sequences = drill.validate_sequences(session)
            fingerprint = drill.manifest(session)
            session.close(); session = None
            drill.command(['pg_dump', '--dbname=service=work', '--format=custom', '--file=' + str(output), '--no-owner', '--no-acl'], env, folder, 'export-pruned')
            output.chmod(0o600)
            drill.command(['pg_restore', '--list', str(output)], env, folder, 'archive-list-pruned')
            summary = {'status': 'pruned_verified', 'companies': len(cuits), 'tables': len(tables),
                'rows_before': sum(before.values()), 'rows_after': sum(after.values()), 'users_rehomed': rehomed, 'foreign_keys_checked': orphans,
                'sequences_checked': sequences, 'pruned_bytes': output.stat().st_size, 'pruned_sha256': sha256_file(output),
                'source_dump_sha256': source_hash, 'source_writes': False}
            detail = dict(summary, per_table={table: {'before': before[table], 'after': after[table]} for table in tables}, manifest=fingerprint)
            report_path.write_text(json.dumps(detail, sort_keys=True))
            report_path.chmod(0o600)
            return dict(summary, per_table={table: after[table] for table in tables if before[table] != after[table] or after[table]})
        finally:
            failure = False
            if session:
                try: session.close()
                except Exception: failure = True
            try: work.remove()
            except Exception: failure = True
            if failure:
                raise DrillError('Resource cleanup requires operator attention; no unrelated resource was removed')


# --- apply -----------------------------------------------------------------------------------------------------

def apply(config, pruned, report_path, confirm):
    pruned, report_path = read_private(pruned), read_private(report_path)
    actual = sha256_file(pruned)
    report = json.loads(report_path.read_text())
    if not re.fullmatch(r'[0-9a-f]{64}', confirm or '') or confirm != actual or report.get('pruned_sha256') != actual or report.get('status') != 'pruned_verified':
        raise DrillError('The reviewed report does not match this pruned dump; nothing was written')
    with tempfile.TemporaryDirectory(prefix='extract12-', dir='/private/tmp') as temporary:
        folder = Path(temporary)
        env = target_services(folder, config)
        session = None
        try:
            drill.command(['pg_restore', '--list', str(pruned)], env, folder, 'archive-list-pruned')
            session = drill.Session('target', env, folder)
            if session.scalar('SELECT current_database();') != config['expected_database']:
                raise DrillError('Connected target identity mismatch')
            if session.scalar('SHOW server_version_num;')[:2] != '18':
                raise DrillError('Target major must be PostgreSQL18')
            if session.scalar(drill.TABLES):
                raise DrillError('Target must be empty; nothing was written')
            session.close(); session = None
            drill.command(['pg_restore', '--dbname=service=target', '--exit-on-error', '--single-transaction', '--no-owner', '--no-acl', str(pruned)],
                env, folder, 'restore-pruned')
            session = drill.Session('target', env, folder)
            session.scalar('BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY;')
            restored = drill.manifest(session)
            if restored != report['manifest']:
                raise DrillError('Target schema or table fingerprints differ from the verified pruned copy')
            orphans = count_orphans(session)
            sequences = drill.validate_sequences(session)
            return {'status': 'target_verified', 'tables': len(restored['tables']), 'rows': report['rows_after'],
                'foreign_keys_checked': orphans, 'sequences_checked': sequences, 'pruned_sha256': actual}
        finally:
            if session:
                session.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest='command', required=True)
    for name in ('configure-source', 'configure-target'):
        commands.add_parser(name).add_argument('--config', required=True)
    item = commands.add_parser('backup')
    item.add_argument('--source-config', required=True); item.add_argument('--output', required=True)
    item = commands.add_parser('prune')
    item.add_argument('--dump', required=True); item.add_argument('--output', required=True); item.add_argument('--report', required=True)
    item.add_argument('--cuit', action='append', required=True, help='11-digit tax identifier; repeat for each company')
    item = commands.add_parser('apply')
    item.add_argument('--target-config', required=True); item.add_argument('--pruned-dump', required=True)
    item.add_argument('--report', required=True); item.add_argument('--confirm-sha256', required=True)
    args = parser.parse_args()
    if args.command == 'configure-source':
        return drill.configure_source(args.config)
    if args.command == 'configure-target':
        return configure_target(args.config)
    if args.command == 'backup':
        result = backup(drill.read_source_config(args.source_config), args.output)
    elif args.command == 'prune':
        result = prune(args.dump, args.cuit, args.output, args.report)
    else:
        result = apply(read_target_config(args.target_config), args.pruned_dump, args.report, args.confirm_sha256)
    print(json.dumps(result, sort_keys=True))


if __name__ == '__main__':
    try:
        main()
    except DrillError as failure:
        raise SystemExit(str(failure))
    except Exception:
        # Never print underlying SQL, environment, DSN, rows or secret-bearing errors.
        raise SystemExit('Extraction failed; no database contents were published. Review guards and access privately.')
