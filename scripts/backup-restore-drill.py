#!/usr/bin/env python3
"""Render staging -> private pg_dump -> new disposable PostgreSQL18. Opt-in only."""
import argparse
import getpass
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import socket
import stat
import subprocess
import tempfile
import time
import uuid
from urllib.parse import urlsplit, unquote

SOURCE_ID = 'dpg-dbdpdbd9fdbs7386vuug-a'
SOURCE_HOST = SOURCE_ID + '.virginia-postgres.render.com'
TARGET_DB = 'restore11_test'
PORT = 15436
MAX_SOURCE_BYTES = 128 * 1024 * 1024


class DrillError(RuntimeError):
    """Only fixed, non-sensitive messages may cross the CLI boundary."""


def read_source_config(path):
    path = Path(path).absolute()
    repo = Path(__file__).resolve().parents[2]
    if path.is_symlink() or repo in path.resolve().parents:
        raise DrillError('Source configuration must be a private regular file outside the repository')
    fd = os.open(path, os.O_RDONLY | os.O_NOFOLLOW)
    with os.fdopen(fd) as source:
        info = os.fstat(source.fileno())
        if not stat.S_ISREG(info.st_mode) or stat.S_IMODE(info.st_mode) != 0o600 or info.st_uid != os.getuid():
            raise DrillError('Source configuration requires owner-only mode600')
        config = json.load(source)
    if config.get('source_id') != SOURCE_ID or config.get('host') != SOURCE_HOST:
        raise DrillError('Source does not match the confirmed Render instance')
    for key in ('database', 'expected_database', 'username', 'password'):
        if not isinstance(config.get(key), str) or not config[key] or any(c in config[key] for c in '\r\n\x00'):
            raise DrillError('Invalid private source configuration')
    if config['database'] != config['expected_database']:
        raise DrillError('Source database does not match its approved identity')
    return config


def private_file(path, content):
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, 'w') as output:
        output.write(content)


def configure_source(path):
    path = Path(path).absolute()
    if Path(__file__).resolve().parents[2] in path.resolve().parents or path.exists() or path.is_symlink():
        raise DrillError('Choose a new private configuration outside the workspace')
    url = urlsplit(getpass.getpass('External Database URL from Render (hidden input): '))
    if url.scheme not in ('postgres', 'postgresql') or url.hostname != SOURCE_HOST or url.port not in (None, 5432):
        raise DrillError('URL must identify the confirmed Render database host')
    database = unquote(url.path.removeprefix('/'))
    config = {'source_id': SOURCE_ID, 'host': SOURCE_HOST, 'database': database,
        'expected_database': database, 'username': unquote(url.username or ''), 'password': unquote(url.password or '')}
    if any(not config[key] or any(c in config[key] for c in '\r\n\x00')
            for key in ('database', 'username', 'password')):
        raise DrillError('Incomplete private connection URL')
    private_file(path, json.dumps(config))
    print('Private source configuration created; no database connection was opened.')


def private_log(folder, label):
    path = folder / (label + '-' + uuid.uuid4().hex)
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    return os.fdopen(fd, 'wb')


def clean_environment():
    return {key: os.environ[key] for key in ('PATH', 'HOME', 'TMPDIR', 'LANG', 'LC_ALL') if key in os.environ}


def service_value(value):
    # libpq service values use literal text after '='; reject multiline injection.
    if any(c in value for c in '\n\r\x00'):
        raise DrillError('Invalid connection setting')
    return value


def make_services(folder, config, password):
    passfile = folder / 'passwords'
    escape = lambda value: value.replace('\\', '\\\\').replace(':', '\\:')
    private_file(passfile, ':'.join(map(escape, [SOURCE_HOST, '5432', config['database'], config['username'], config['password']])) + '\n'
        + ':'.join(map(escape, ['127.0.0.1', str(PORT), TARGET_DB, TARGET_DB, password])) + '\n')
    service = folder / 'services.conf'
    private_file(service, '\n'.join([
        '[source]', 'host=' + SOURCE_HOST, 'port=5432', 'dbname=' + service_value(config['database']),
        'user=' + service_value(config['username']), 'sslmode=verify-full', 'sslrootcert=system',
        'connect_timeout=10', 'application_name=backup_restore_11_read_only',
        '[target]', 'host=127.0.0.1', 'port=' + str(PORT), 'dbname=' + TARGET_DB,
        'user=' + TARGET_DB, 'sslmode=disable', 'connect_timeout=5', '']))
    env = clean_environment()
    env.update({'PGSERVICEFILE': str(service), 'PGPASSFILE': str(passfile),
        'PGOPTIONS': '-c default_transaction_read_only=on -c statement_timeout=120000 -c lock_timeout=5000',
        'PGCONNECT_TIMEOUT': '10', 'PGTZ': 'UTC', 'PGDATESTYLE': 'ISO, YMD', 'PGCLIENTENCODING': 'UTF8'})
    return env


def ident(value):
    return '"' + value.replace('"', '""') + '"'


class Session:
    def __init__(self, service, env, folder):
        self.service = service
        self.stderr = private_log(folder, service + '-sql-errors')
        self.process = subprocess.Popen(['psql', '-X', '-q', '-A', '-t', '-v', 'ON_ERROR_STOP=1',
            'service=' + service], env=env, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=self.stderr)

    def lines(self, sql):
        marker = 'end_' + uuid.uuid4().hex
        self.process.stdin.write((sql + "\nSELECT '" + marker + "';\n").encode())
        self.process.stdin.flush()
        while True:
            line = self.process.stdout.readline()
            if not line:
                raise DrillError('Private ' + self.service + ' SQL session failed; no database content was published')
            if line.rstrip(b'\r\n') == marker.encode():
                return
            yield line

    def scalar(self, sql):
        return b''.join(self.lines(sql)).decode().strip()

    def close(self):
        if self.process.poll() is None:
            try:
                self.process.stdin.write(b'ROLLBACK;\n\\q\n')
                self.process.stdin.flush()
                self.process.wait(timeout=5)
            except (BrokenPipeError, subprocess.TimeoutExpired):
                self.process.kill()
                self.process.wait(timeout=5)
        self.process.stdin.close()
        self.process.stdout.close()
        self.stderr.close()


TABLES = "SELECT relname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND c.relkind IN ('r','p') ORDER BY relname;"
SCHEMA = """COPY (
 SELECT n.nspname,c.relname,a.attnum,a.attname,format_type(a.atttypid,a.atttypmod),a.attnotnull,a.attidentity,a.attgenerated,
 coalesce(pg_get_expr(d.adbin,d.adrelid),'')
 FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace
 LEFT JOIN pg_attrdef d ON d.adrelid=a.attrelid AND d.adnum=a.attnum
 WHERE n.nspname='public' AND c.relkind IN ('r','p') AND a.attnum>0 AND NOT a.attisdropped
 ORDER BY c.relname,a.attnum) TO STDOUT;"""
CONSTRAINTS = """COPY (SELECT c.relname,p.conname,pg_get_constraintdef(p.oid),p.convalidated
 FROM pg_constraint p JOIN pg_class c ON c.oid=p.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace
 WHERE n.nspname='public' ORDER BY c.relname,p.conname) TO STDOUT;"""
INDEXES = "COPY (SELECT tablename,indexname,indexdef FROM pg_indexes WHERE schemaname='public' ORDER BY tablename,indexname) TO STDOUT;"


def digest(session, sql):
    result = hashlib.sha256()
    for line in session.lines(sql):
        result.update(line)
    return result.hexdigest()


def manifest(session):
    session.scalar("SET TIME ZONE 'UTC'; SET DateStyle='ISO, YMD'; SET extra_float_digits=3; SET search_path=pg_catalog;")
    names = session.scalar(TABLES).splitlines()
    tables = {}
    for name in names:
        table = 'public.' + ident(name)
        count = int(session.scalar('SELECT count(*) FROM ' + table + ';'))
        # Only row hashes cross the SQL pipe; never raw rows. Sorted hashes make order irrelevant.
        fingerprint = digest(session, 'COPY (SELECT md5(row_to_json(t)::text) FROM ' + table
            + ' t ORDER BY 1) TO STDOUT;')
        tables[name] = {'count': count, 'sha256_of_sorted_row_md5': fingerprint}
    return {'columns': digest(session, SCHEMA), 'constraints': digest(session, CONSTRAINTS),
        'indexes': digest(session, INDEXES), 'tables': tables}


def validate_sequences(session):
    query = """SELECT c.relname||chr(9)||a.attname||chr(9)||pg_get_serial_sequence(format('%I.%I',n.nspname,c.relname),a.attname)
      FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace
      WHERE n.nspname='public' AND c.relkind IN ('r','p') AND a.attnum>0 AND NOT a.attisdropped
      AND pg_get_serial_sequence(format('%I.%I',n.nspname,c.relname),a.attname) IS NOT NULL;"""
    rows = session.scalar(query).splitlines()
    for row in rows:
        table, column, sequence = row.split('\t')
        # Sequence identifiers are server-derived; constrain the qualified form before SQL use.
        if not re.fullmatch(r'public\.[a-z_][a-z0-9_]*', sequence):
            raise DrillError('Unexpected sequence identifier')
        last, called = session.scalar('SELECT last_value,is_called FROM ' + sequence + ';').split('|')
        increment, cycle = session.scalar("SELECT increment_by,cycle FROM pg_sequences WHERE schemaname='public' AND sequencename='"
            + sequence.split('.')[1] + "';").split('|')
        maximum = session.scalar('SELECT max(' + ident(column) + ') FROM public.' + ident(table) + ';')
        if int(increment) <= 0 or cycle != 'f':
            raise DrillError('Sequence policy is outside this rehearsal scope')
        candidate = int(last) + (int(increment) if called == 't' else 0)
        if maximum and candidate <= int(maximum):
            raise DrillError('Restored sequence could collide with existing rows')
    return len(rows)


def command(args, env, folder, name):
    with private_log(folder, name) as log:
        result = subprocess.run(args, env=env, stdout=log, stderr=subprocess.STDOUT, timeout=180)
    if result.returncode:
        raise DrillError('Private ' + name + ' failed; no SQL or connection details were published')


def restore_environment(env):
    result = dict(env)
    result['PGOPTIONS'] = '-c statement_timeout=120000 -c lock_timeout=5000'
    return result


def check_target_identity(metadata, owner):
    if (metadata['Config']['Labels'].get('backup11.owner') != owner
            or metadata['HostConfig']['PortBindings'].get('5432/tcp') != [{'HostIp': '127.0.0.1', 'HostPort': str(PORT)}]
            or '/var/lib/postgresql' not in metadata['HostConfig'].get('Tmpfs', {})):
        raise DrillError('Disposable target identity mismatch')


def cleanup_container(container_id, owner, env):
    checked = subprocess.run(['docker', 'inspect', '--format', '{{index .Config.Labels "backup11.owner"}}', container_id],
        env=env, capture_output=True, text=True)
    if checked.returncode != 0 or checked.stdout.strip() != owner:
        raise DrillError('Container ownership mismatch; cleanup refused')
    subprocess.run(['docker', 'rm', '-f', container_id], env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)


def recover_owned_container_id(name, owner, env):
    # A failed docker run can leave a stopped container. Never clean by name alone.
    checked = subprocess.run(['docker', 'inspect', '--format', '{{.Id}} {{index .Config.Labels "backup11.owner"}}', name],
        env=env, capture_output=True, text=True)
    if checked.returncode:
        return None
    fields = checked.stdout.strip().split()
    if len(fields) != 2 or not re.fullmatch(r'[a-f0-9]{64}', fields[0]) or fields[1] != owner:
        raise DrillError('Failed container creation left an unverified resource; cleanup refused')
    return fields[0]


def finish(source, target, container_id, owner, env):
    failed = False
    for session in (source, target):
        if session:
            try: session.close()
            except Exception: failed = True
    if container_id:
        try: cleanup_container(container_id, owner, env)
        except Exception: failed = True
    if failed:
        raise DrillError('Resource cleanup requires operator attention; no unrelated resource was removed')


def wait_target_ready(container_id, env):
    # initdb's temporary postmaster accepts Unix sockets before TCP is available.
    # Also verify the host port and credentials that the actual restore will use.
    for _ in range(60):
        ready = subprocess.run(['docker', 'exec', container_id, 'pg_isready', '-h', '127.0.0.1',
            '-U', TARGET_DB, '-d', TARGET_DB], env=env, capture_output=True)
        if ready.returncode == 0:
            connected = subprocess.run(['psql', '-X', '-q', '-A', '-t', '-v', 'ON_ERROR_STOP=1',
                'service=target', '-c', 'SELECT 1;'], env=env, capture_output=True)
            if connected.returncode == 0 and connected.stdout.strip() == b'1':
                return
        time.sleep(0.5)
    raise DrillError('Restore target TCP readiness/authentication timed out; no database contents were published')


def drill(config):
    with socket.socket() as probe:
        probe.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        probe.bind(('127.0.0.1', PORT))
    owner = uuid.uuid4().hex
    container_id = None
    source = target = None
    with tempfile.TemporaryDirectory(prefix='backup11-', dir='/private/tmp') as temporary:
        folder = Path(temporary)
        password = secrets.token_urlsafe(40)
        env = make_services(folder, config, password)
        try:
            source = Session('source', env, folder)
            source.scalar('BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY;')
            if source.scalar('SELECT current_database();') != config['expected_database']:
                raise DrillError('Connected source identity mismatch')
            if source.scalar('SHOW transaction_read_only;') != 'on':
                raise DrillError('Source session is not read-only')
            if source.scalar('SHOW server_version_num;')[:2] != '18':
                raise DrillError('Source major must be PostgreSQL18')
            if int(source.scalar('SELECT pg_database_size(current_database());')) > MAX_SOURCE_BYTES:
                raise DrillError('Source exceeds the128MiB rehearsal size budget')
            snapshot = source.scalar('SELECT pg_export_snapshot();')
            if not re.fullmatch(r'[0-9A-Fa-f]+-[0-9A-Fa-f]+-[0-9]+', snapshot):
                raise DrillError('Invalid exported snapshot identity')
            start = time.monotonic()
            archive = folder / 'staging.dump'
            private_file(archive, '')
            command(['pg_dump', '--dbname=service=source', '--format=custom', '--file=' + str(archive),
                '--snapshot=' + snapshot, '--lock-wait-timeout=5s', '--no-publications', '--no-subscriptions'], env, folder, 'export')
            archive.chmod(0o600)
            export_seconds = time.monotonic() - start
            reference = manifest(source)
            required = {'empresas', 'usuarios_empresa', 'proyectos', 'flyway_schema_history'}
            if not required.issubset(reference['tables']) or not reference['tables']['usuarios_empresa']['count']:
                raise DrillError('Source is not the expected populated application schema')
            if int(source.scalar('SELECT count(*) FROM public.flyway_schema_history WHERE NOT success;')):
                raise DrillError('Source contains unsuccessful migration history')
            source.close(); source = None
            archive_hash = hashlib.sha256(archive.read_bytes()).hexdigest()
            command(['pg_restore', '--list', str(archive)], env, folder, 'archive-list')

            envfile = folder / 'docker.env'
            private_file(envfile, 'POSTGRES_USER=' + TARGET_DB + '\nPOSTGRES_DB=' + TARGET_DB + '\nPOSTGRES_PASSWORD=' + password + '\n')
            created = subprocess.run(['docker', 'run', '-d', '--name', 'backup11-' + owner,
                '--label', 'backup11.owner=' + owner, '--env-file', str(envfile), '--tmpfs', '/var/lib/postgresql:rw',
                '-p', '127.0.0.1:' + str(PORT) + ':5432', 'postgres:18'], env=env, capture_output=True, text=True)
            if created.returncode:
                container_id = recover_owned_container_id('backup11-' + owner, owner, env)
                raise DrillError('Could not create exclusive restore target')
            container_id = created.stdout.strip()
            inspected = subprocess.run(['docker', 'inspect', container_id], env=env, capture_output=True, text=True, check=True)
            check_target_identity(json.loads(inspected.stdout)[0], owner)
            wait_target_ready(container_id, env)
            target = Session('target', env, folder)
            if target.scalar('SELECT current_database();') != TARGET_DB or target.scalar(TABLES):
                raise DrillError('Restore target must be the empty owned database')
            target.close(); target = None
            start = time.monotonic()
            command(['pg_restore', '--dbname=service=target', '--exit-on-error', '--single-transaction', '--no-owner', '--no-acl', str(archive)],
                restore_environment(env), folder, 'restore')
            restore_seconds = time.monotonic() - start
            target = Session('target', env, folder)
            target.scalar('BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY;')
            restored = manifest(target)
            if reference != restored:
                raise DrillError('Restored schema or table fingerprints differ from the exported snapshot')
            sequences = validate_sequences(target)
            if hashlib.sha256(archive.read_bytes()).hexdigest() != archive_hash:
                raise DrillError('Private archive integrity changed during rehearsal')
            return {'status': 'verified', 'source_id': SOURCE_ID, 'tables_verified': len(reference['tables']),
                'sequences_checked': sequences, 'export_seconds': round(export_seconds, 3),
                'restore_seconds': round(restore_seconds, 3), 'archive_sha256': archive_hash,
                'archive_retained': False, 'source_writes': False}
        finally:
            finish(source, target, container_id, owner, env)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source-config', required=True, help='Private600 JSON outside repo; never paste values in chat')
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument('--execute', action='store_true', help='Requires separate approved execution scope')
    mode.add_argument('--configure-source', action='store_true', help='Create private config with hidden local prompt; no connections')
    args = parser.parse_args()
    if args.configure_source:
        return configure_source(args.source_config)
    print(json.dumps(drill(read_source_config(args.source_config)), sort_keys=True))


if __name__ == '__main__':
    try:
        main()
    except DrillError as failure:
        raise SystemExit(str(failure))
    except Exception:
        # Never print underlying SQL, environment, DSN, rows or secret-bearing errors.
        raise SystemExit('Backup rehearsal failed; no database contents were published. Review guards/access privately.')
