import importlib.util
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("payments_local", Path(__file__).parents[1] / "payments-local.py")
lab = importlib.util.module_from_spec(spec)
spec.loader.exec_module(lab)


class StripeExecutableTest(unittest.TestCase):
    def test_prefers_shell_executable(self):
        with patch.object(lab.shutil, "which", return_value="/custom/bin/stripe"):
            self.assertEqual(Path("/custom/bin/stripe"), lab.find_stripe_cli())

    def test_other_nvm_version_is_found_when_missing_from_path(self):
        with tempfile.TemporaryDirectory() as home:
            executable = Path(home) / ".nvm/versions/node/v20.20.2/bin/stripe"
            executable.parent.mkdir(parents=True)
            executable.write_text("#!/bin/sh\nexit 0\n")
            executable.chmod(0o700)
            with patch.object(lab.shutil, "which", return_value=None):
                self.assertEqual(executable, lab.find_stripe_cli(home))
            with patch.dict(os.environ, {"PATH": "/node18/bin:/usr/bin"}):
                self.assertTrue(lab.stripe_environment(executable)["PATH"].startswith(str(executable.parent) + os.pathsep))
                self.assertEqual("/node18/bin:/usr/bin", os.environ["PATH"])

    def test_missing_installation_has_actionable_error(self):
        with tempfile.TemporaryDirectory() as home, patch.object(lab.shutil, "which", return_value=None):
            with self.assertRaisesRegex(SystemExit, "No se encontró Stripe CLI"):
                lab.find_stripe_cli(home)


if __name__ == "__main__":
    unittest.main()
