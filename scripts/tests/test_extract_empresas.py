"""Context-free tests for the selective extraction: no database, Render, Docker, credentials or network."""
import importlib.util
import json
import os
from pathlib import Path
import re
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('extract12', Path(__file__).parents[1] / 'extract-empresas.py')
extract = importlib.util.module_from_spec(spec)
spec.loader.exec_module(extract)
MIGRATIONS = Path(__file__).parents[2] / 'src/main/resources/db/migration'
CUITS = ['20000000001', '20000000002']


def migration_tables():
    names = set()
    for script in MIGRATIONS.glob('V*.sql'):
        names |= set(re.findall(r'^\s*CREATE TABLE(?: IF NOT EXISTS)?\s+([a-z_]+)', script.read_text(), re.I | re.M))
    return names


class ClassificationTest(unittest.TestCase):
    def test_every_table_created_by_the_migrations_is_classified(self):
        # A new table must be classified deliberately; otherwise the extraction aborts instead of guessing.
        self.assertEqual(extract.unclassified(migration_tables()), [])
        self.assertEqual(migration_tables(), extract.CLASSIFIED - {'flyway_schema_history'})

    def test_unknown_tables_are_reported_and_nothing_else(self):
        self.assertEqual(extract.unclassified(['empresas', 'nueva_tabla', 'otra']), ['nueva_tabla', 'otra'])

    def test_each_table_has_exactly_one_treatment(self):
        groups = [{t for t, _, _ in extract.OWNERSHIP}, set(extract.SPECIAL), set(extract.EXCLUDED), set(extract.KEEP_ALL)]
        for index, group in enumerate(groups):
            for other in groups[index + 1:]:
                self.assertEqual(group & other, set())

    def test_every_ownership_parent_is_processed_before_its_children(self):
        seen = {'empresas'}
        for table, _, parent in extract.OWNERSHIP:
            self.assertIn(parent, seen, table)
            seen.add(table)


class StatementTest(unittest.TestCase):
    def test_tax_identifiers_are_validated_before_use_in_sql(self):
        for bad in ([], ['123'], ["20000000001' OR '1'='1"], ['20000000001', '20000000001'], ['abcdefghijk']):
            with self.assertRaises(extract.DrillError):
                extract.prune_statements(bad)
        self.assertEqual(extract.validate_cuits(['20000000002', '20000000001']), CUITS)

    def test_pruning_deletes_parents_before_children_and_clears_stripe_children_first(self):
        statements = extract.prune_statements(CUITS)
        position = {}
        for index, text in enumerate(statements):
            match = re.match(r'DELETE FROM public\.("?[a-z_]+"?)', text)
            if match and match.group(1).strip('"') not in position:
                position[match.group(1).strip('"')] = index
        for table, _, parent in extract.OWNERSHIP:
            self.assertLess(position[parent], position[table], table)
        self.assertLess(position['devoluciones_pago'], position['pagos_suscripcion'])
        self.assertLess(position['pagos_suscripcion'], position['suscripciones'])
        self.assertTrue(statements[0].startswith('DELETE FROM public.empresas'))

    def test_pruning_only_embeds_requested_identifiers_and_neutralizes_references_to_removed_rows(self):
        text = '\n'.join(extract.prune_statements(CUITS))
        self.assertEqual(sorted(set(re.findall(r"'([^']*)'", text))), CUITS)
        self.assertIn('SET invitante_id = NULL', text)
        self.assertIn('SET creado_por_usuario_id = NULL', text)
        self.assertIn('SET stripe_price_id = NULL', text)
        self.assertNotIn('TRUNCATE', text)
        self.assertNotIn('CASCADE', text)

    def test_creators_of_kept_companies_are_moved_into_a_kept_company_before_users_are_pruned(self):
        statements = extract.prune_statements(CUITS)
        move = next(index for index, text in enumerate(statements) if text.startswith('UPDATE public.usuarios_empresa u SET empresa_id'))
        delete_users = next(index for index, text in enumerate(statements) if text.startswith('DELETE FROM public."usuarios_empresa"') or text.startswith('DELETE FROM public.usuarios_empresa'))
        delete_companies = next(index for index, text in enumerate(statements) if text.startswith('DELETE FROM public.empresas'))
        self.assertLess(delete_companies, move)
        self.assertLess(move, delete_users)
        self.assertIn('min(e.id)', statements[move])
        self.assertIn('creado_por_usuario_id = u.id', statements[move])

    def test_expected_users_include_members_and_creators_of_kept_companies(self):
        for table in ('usuarios_empresa', 'usuario_empresa_accesos', 'usuario_obra', 'invitaciones_equipo', 'invitacion_empresas'):
            self.assertIn('creado_por_usuario_id', extract.expected_sql(table, CUITS), table)
        self.assertEqual(extract.expected_sql('usuarios_empresa', CUITS).count('identificacion_fiscal IN'), 2)
        # user: member or creator (2 mentions); project's company (1)
        self.assertEqual(extract.expected_sql('usuario_obra', CUITS).count('identificacion_fiscal IN'), 3)

    def test_expected_counts_are_computed_by_joins_up_to_the_company(self):
        deep = extract.expected_sql('restriccion_comentarios', CUITS)
        chain = re.findall(r'(?:FROM|JOIN) public\."([a-z_]+)"', deep)
        self.assertEqual(chain, ['restriccion_comentarios', 'restricciones_obra', 'proyectos', 'empresas'])
        self.assertIn('identificacion_fiscal IN', deep)
        self.assertNotIn('NOT IN', deep)
        self.assertEqual(extract.expected_sql('devoluciones_pago', CUITS), 'SELECT 0;')
        self.assertIn('planes_suscripcion', extract.expected_sql('planes_suscripcion', CUITS))
        invitations = extract.expected_sql('invitaciones_equipo', CUITS)
        self.assertIn('invitacion_empresas', invitations)
        access = extract.expected_sql('usuario_empresa_accesos', CUITS)
        self.assertEqual(access.count('identificacion_fiscal IN'), 3)  # user: member or creator; access: company

    def test_orphan_check_supports_composite_keys_and_skips_null_references(self):
        sql = extract.orphan_sql('public.restricciones_obra', 'public.rubros_obra', 'proyecto_id,rubro_id', 'proyecto_id,id')
        self.assertIn('c.proyecto_id IS NOT NULL AND c.rubro_id IS NOT NULL', sql)
        self.assertIn('p.proyecto_id = c.proyecto_id AND p.id = c.rubro_id', sql)


class PrivateFileTest(unittest.TestCase):
    def test_outputs_must_be_new_private_files_outside_the_repository(self):
        with tempfile.TemporaryDirectory(dir='/private/tmp') as folder:
            target = Path(folder) / 'out.dump'
            created = extract.private_output(target)
            self.assertEqual(oct(created.stat().st_mode & 0o777), '0o600')
            with self.assertRaises(extract.DrillError):
                extract.private_output(target)
            with self.assertRaises(extract.DrillError):
                extract.private_output(Path(folder) / 'missing' / 'out.dump')
        with self.assertRaises(extract.DrillError):
            extract.private_output(extract.REPO / 'gestion_be' / 'must-not-exist.dump')

    def test_private_inputs_require_owner_only_permissions(self):
        with tempfile.TemporaryDirectory(dir='/private/tmp') as folder:
            path = Path(folder) / 'input'
            path.write_text('x'); path.chmod(0o644)
            with self.assertRaises(extract.DrillError):
                extract.read_private(path)
            path.chmod(0o600)
            self.assertEqual(extract.read_private(path), path)

    def test_failed_work_removes_partial_private_outputs(self):
        with tempfile.TemporaryDirectory(dir='/private/tmp') as folder:
            first = extract.private_output(Path(folder) / 'a'); second = extract.private_output(Path(folder) / 'b')
            with self.assertRaises(RuntimeError):
                extract.discard_on_failure([first, second], lambda: (_ for _ in ()).throw(RuntimeError('synthetic')))
            self.assertFalse(first.exists() or second.exists())


class TargetTest(unittest.TestCase):
    def config(self, **changes):
        base = {'target_id': 'dpg-abc123-a', 'host': 'dpg-abc123-a.frankfurt-postgres.render.com', 'database': 'db',
            'expected_database': 'db', 'username': 'user', 'password': 'synthetic'}
        return dict(base, **changes)

    def read(self, config):
        with tempfile.TemporaryDirectory(dir='/private/tmp') as folder:
            path = Path(folder) / 'target.json'
            path.write_text(json.dumps(config)); path.chmod(0o600)
            return extract.read_target_config(path)

    def test_accepts_only_a_distinct_render_database_with_matching_identity(self):
        self.assertEqual(self.read(self.config())['target_id'], 'dpg-abc123-a')
        for bad in (self.config(host='db.example.com'), self.config(target_id='dpg-other-a'),
                self.config(target_id=extract.drill.SOURCE_ID, host=extract.drill.SOURCE_HOST),
                self.config(expected_database='other'), self.config(password='')):
            with self.assertRaises(extract.DrillError):
                self.read(bad)

    def test_configuration_prompt_rejects_the_source_and_foreign_hosts(self):
        for url in ('postgresql://u:p@db.example.com/db', 'postgresql://u:p@%s/db' % extract.drill.SOURCE_HOST):
            with tempfile.TemporaryDirectory(dir='/private/tmp') as folder, patch.object(extract.getpass, 'getpass', return_value=url):
                with self.assertRaises(extract.DrillError):
                    extract.configure_target(Path(folder) / 'target.json')


class ApplyGuardTest(unittest.TestCase):
    def test_apply_refuses_an_unreviewed_dump_before_any_connection(self):
        with tempfile.TemporaryDirectory(dir='/private/tmp') as folder:
            dump, report = Path(folder) / 'pruned.dump', Path(folder) / 'report.json'
            dump.write_bytes(b'synthetic'); dump.chmod(0o600)
            digest = extract.sha256_file(dump)
            report.write_text(json.dumps({'status': 'pruned_verified', 'pruned_sha256': digest})); report.chmod(0o600)
            config = {'host': 'dpg-abc123-a.frankfurt-postgres.render.com', 'database': 'db', 'expected_database': 'db', 'username': 'u', 'password': 'p'}
            forbidden = AssertionError('connected before the confirmation was validated')
            with patch.object(extract.drill, 'Session', side_effect=forbidden), patch.object(extract.drill, 'command', side_effect=forbidden):
                for confirm in ('', 'x' * 64, '0' * 64):
                    with self.assertRaises(extract.DrillError):
                        extract.apply(config, dump, report, confirm)
                report.write_text(json.dumps({'status': 'other', 'pruned_sha256': digest}))
                with self.assertRaises(extract.DrillError):
                    extract.apply(config, dump, report, digest)


if __name__ == '__main__':
    unittest.main()
