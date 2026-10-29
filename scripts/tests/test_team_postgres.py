import copy
import json
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch, Mock

spec = importlib.util.spec_from_file_location('team_postgres', Path(__file__).parents[1] / 'team-postgres.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class TeamIsolationTest(unittest.TestCase):
    def metadata(self):
        return {'Config': {'Labels': {'team15.owner': 'synthetic-owner'}},
                'HostConfig': {'PortBindings': {'5432/tcp': [{'HostIp': '127.0.0.1', 'HostPort': '15439'}]},
                               'Tmpfs': {'/var/lib/postgresql': 'rw'}}, 'Mounts': []}

    def test_exclusive_contract_accepts_only_disposable_owner_and_loopback(self):
        module.require_owned(self.metadata(), 'synthetic-owner')
        for key in ('owner', 'port', 'address', 'tmpfs', 'volume'):
            candidate = copy.deepcopy(self.metadata())
            if key == 'owner': candidate['Config']['Labels']['team15.owner'] = 'other'
            if key == 'port': candidate['HostConfig']['PortBindings']['5432/tcp'][0]['HostPort'] = '15433'
            if key == 'address': candidate['HostConfig']['PortBindings']['5432/tcp'][0]['HostIp'] = '0.0.0.0'
            if key == 'tmpfs': candidate['HostConfig']['Tmpfs'] = {}
            if key == 'volume': candidate['Mounts'] = [{'Type': 'volume'}]
            with self.assertRaises(RuntimeError): module.require_owned(candidate, 'synthetic-owner')

    def test_parent_application_credentials_are_not_forwarded(self):
        with patch.dict('os.environ', {'STRIPE_SECRET_KEY': 'synthetic', 'APP_DATABASE_JDBC_URL': 'forbidden',
                                      'TEAM15_PG_PASSWORD': 'stale', 'APP_SUPERADMIN_PASSWORD': 'synthetic'}):
            env = module.clean_environment()
        for key in ('STRIPE_SECRET_KEY', 'APP_DATABASE_JDBC_URL', 'TEAM15_PG_PASSWORD', 'APP_SUPERADMIN_PASSWORD'):
            self.assertNotIn(key, env)


class FailedCreationCleanupTest(unittest.TestCase):
    def test_recovery_refuses_a_foreign_owner_and_accepts_only_a_valid_owned_id(self):
        owner = 'synthetic-owner'; identity = 'a' * 64
        for candidate_owner, expected in ((owner, identity), ('foreign', None)):
            metadata = {'Id': identity, 'Config': {'Labels': {'team15.owner': candidate_owner}}}
            result = Mock(returncode=0, stdout=json.dumps([metadata]))
            with patch.object(module.subprocess, 'run', return_value=result):
                self.assertEqual(expected, module.recover_owned_container('team15-test', owner, {}))
        with patch.object(module.subprocess, 'run', return_value=Mock(returncode=1)):
            self.assertIsNone(module.recover_owned_container('team15-test', owner, {}))

    def test_failed_docker_run_still_cleans_only_the_container_with_matching_label(self):
        identity = 'a' * 64
        def command(args, **kwargs):
            if args[:2] == ['docker', 'run']:
                return Mock(returncode=1, stdout='', stderr='synthetic failure')
            if args[:2] == ['docker', 'inspect'] and '--format' not in args:
                return Mock(returncode=0, stdout=json.dumps([{'Id': identity, 'Config': {'Labels': {'team15.owner': 'owner'}}}]))
            if args[:2] == ['docker', 'inspect']:
                return Mock(returncode=0, stdout='owner')
            if args[:3] == ['docker', 'rm', '-f']:
                self.assertEqual(identity, args[3]); return Mock(returncode=0)
            raise AssertionError('Unexpected subprocess')
        with patch.object(module.socket, 'socket'), patch.object(module.uuid, 'uuid4', return_value=Mock(hex='owner')), \
                patch.object(module.subprocess, 'run', side_effect=command) as run:
            with self.assertRaisesRegex(RuntimeError, 'Could not create'):
                module.main()
            self.assertTrue(any(call.args[0][:3] == ['docker', 'rm', '-f'] for call in run.call_args_list))
