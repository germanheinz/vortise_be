import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import Mock, patch

spec = importlib.util.spec_from_file_location('backup_restore_drill', Path(__file__).parents[1] / 'backup-restore-drill.py')
drill = importlib.util.module_from_spec(spec)
spec.loader.exec_module(drill)


def config():
    return {'source_id': drill.SOURCE_ID, 'host': drill.SOURCE_HOST, 'database': 'synthetic_test',
        'expected_database': 'synthetic_test', 'username': 'synthetic_user', 'password': 'synthetic-secret:not-real'}


class SourceGuardTest(unittest.TestCase):
    def write(self, folder, values, mode=0o600):
        path = Path(folder) / 'source.json'
        path.write_text(json.dumps(values)); path.chmod(mode)
        return path

    def test_requires_exact_source_id_host_and_database(self):
        with tempfile.TemporaryDirectory() as folder:
            path = self.write(folder, config())
            self.assertEqual(drill.read_source_config(path)['source_id'], drill.SOURCE_ID)
            for key, value in [('source_id', 'other'), ('host', 'remote.example.invalid'),
                    ('expected_database', 'other'), ('password', 'newline\ninjection')]:
                path = self.write(folder, {**config(), key: value})
                with self.assertRaises(drill.DrillError): drill.read_source_config(path)

    def test_rejects_public_permissions_and_symlinks(self):
        with tempfile.TemporaryDirectory() as folder:
            path = self.write(folder, config(), 0o644)
            with self.assertRaises(drill.DrillError): drill.read_source_config(path)
            path.chmod(0o600)
            link = Path(folder) / 'link'; link.symlink_to(path)
            with self.assertRaises(drill.DrillError): drill.read_source_config(link)

    def test_does_not_inherit_database_environment_or_put_password_in_service(self):
        with tempfile.TemporaryDirectory() as folder, patch.dict(os.environ, {'PGHOST': 'other', 'PGPASSWORD': 'unapproved'}):
            folder = Path(folder)
            env = drill.make_services(folder, config(), 'synthetic-target-password')
            self.assertNotIn('PGHOST', env); self.assertNotIn('PGPASSWORD', env)
            self.assertIn('default_transaction_read_only=on', env['PGOPTIONS'])
            service = Path(env['PGSERVICEFILE']).read_text()
            self.assertIn('sslmode=verify-full', service); self.assertIn('sslrootcert=system', service)
            self.assertNotIn(config()['password'], service)
            self.assertIn('synthetic-secret\\:not-real', Path(env['PGPASSFILE']).read_text())
            for path in folder.iterdir(): self.assertEqual(path.stat().st_mode & 0o777, 0o600)

    def test_cli_requires_explicit_execution_before_reading_config(self):
        with patch('sys.argv', ['drill', '--source-config', '/not/read']), patch.object(drill, 'read_source_config') as read:
            with self.assertRaises(SystemExit): drill.main()
            read.assert_not_called()

    def test_interactive_setup_hides_url_and_creates_new_private_file(self):
        with tempfile.TemporaryDirectory() as folder, patch.object(drill.getpass, 'getpass',
                return_value='postgresql://synthetic_user:synthetic-pass@' + drill.SOURCE_HOST + '/synthetic_test'), patch('builtins.print'):
            path = Path(folder) / 'new-source.json'
            drill.configure_source(path)
            self.assertEqual(path.stat().st_mode & 0o777, 0o600)
            self.assertEqual(drill.read_source_config(path)['database'], 'synthetic_test')
            with self.assertRaises(drill.DrillError): drill.configure_source(path)


class TargetGuardTest(unittest.TestCase):
    def metadata(self):
        return {'Config': {'Labels': {'backup11.owner': 'own'}}, 'HostConfig': {
            'PortBindings': {'5432/tcp': [{'HostIp': '127.0.0.1', 'HostPort': '15436'}]},
            'Tmpfs': {'/var/lib/postgresql': 'rw'}}}

    def test_rejects_foreign_container_wrong_port_or_non_tmpfs(self):
        drill.check_target_identity(self.metadata(), 'own')
        cases = [self.metadata() for _ in range(3)]
        cases[0]['Config']['Labels']['backup11.owner'] = 'foreign'
        cases[1]['HostConfig']['PortBindings']['5432/tcp'][0]['HostPort'] = '15433'
        cases[2]['HostConfig']['Tmpfs'] = {}
        for case in cases:
            with self.assertRaises(drill.DrillError): drill.check_target_identity(case, 'own')

    def test_cleanup_never_removes_container_with_wrong_owner(self):
        result = subprocess.CompletedProcess([], 0, 'foreign\n', '')
        with patch.object(drill.subprocess, 'run', return_value=result) as run:
            with self.assertRaises(drill.DrillError): drill.cleanup_container('id', 'own', {})
            self.assertEqual(run.call_count, 1)

    def test_cleanup_only_removes_verified_container_id(self):
        result = subprocess.CompletedProcess([], 0, 'own\n', '')
        with patch.object(drill.subprocess, 'run', return_value=result) as run:
            drill.cleanup_container('exact-owned-id', 'own', {})
            self.assertEqual(run.call_args.args[0], ['docker', 'rm', '-f', 'exact-owned-id'])

    def test_failed_creation_recovers_id_only_after_owner_verification(self):
        owned = 'a' * 64
        with patch.object(drill.subprocess, 'run', return_value=subprocess.CompletedProcess([], 0, owned + ' own\n', '')):
            self.assertEqual(drill.recover_owned_container_id('own-name', 'own', {}), owned)
        with patch.object(drill.subprocess, 'run', return_value=subprocess.CompletedProcess([], 0, owned + ' foreign\n', '')):
            with self.assertRaises(drill.DrillError): drill.recover_owned_container_id('own-name', 'own', {})

    def test_session_cleanup_failure_does_not_skip_container_cleanup(self):
        source = Mock(); source.close.side_effect = OSError('synthetic')
        target = Mock()
        with patch.object(drill, 'cleanup_container') as cleanup:
            with self.assertRaises(drill.DrillError): drill.finish(source, target, 'own-id', 'own', {})
            target.close.assert_called_once(); cleanup.assert_called_once_with('own-id', 'own', {})

    def test_target_waits_for_tcp_and_host_authentication_not_bootstrap_socket(self):
        ready = subprocess.CompletedProcess([], 0, b'', b'')
        refused = subprocess.CompletedProcess([], 2, b'', b'private connection error')
        accepted = subprocess.CompletedProcess([], 0, b'1\n', b'')
        with patch.object(drill.subprocess, 'run', side_effect=[ready, refused, ready, accepted]) as run, patch.object(drill.time, 'sleep') as sleep:
            drill.wait_target_ready('owned-id', {})
            self.assertEqual(run.call_count, 4)
            for call in (run.call_args_list[0], run.call_args_list[2]):
                command = call.args[0]
                self.assertIn('-h', command)
                self.assertEqual(command[command.index('-h') + 1], '127.0.0.1')
            self.assertEqual(run.call_args_list[3].args[0][-1], 'SELECT 1;')
            sleep.assert_called_once_with(0.5)

    def test_write_access_is_enabled_only_for_restore_environment(self):
        source = {'PGOPTIONS': '-c default_transaction_read_only=on', 'PGSERVICEFILE': 'private'}
        target = drill.restore_environment(source)
        self.assertIn('read_only=on', source['PGOPTIONS'])
        self.assertNotIn('read_only=on', target['PGOPTIONS'])
        self.assertEqual(target['PGSERVICEFILE'], source['PGSERVICEFILE'])


if __name__ == '__main__': unittest.main()
