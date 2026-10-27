import importlib.util
from pathlib import Path
import unittest
from unittest.mock import Mock, call

spec = importlib.util.spec_from_file_location("payments_cycle", Path(__file__).parents[1] / "payments-cycle.py")
cycle = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cycle)


class CycleSafetyTest(unittest.TestCase):
    def test_scope_rejects_live_or_unrelated_clock(self):
        sub = {"livemode": False, "test_clock": "clock_local"}
        clock = {"livemode": False, "id": "clock_local", "name": "gestion-payments-local-06"}
        self.assertTrue(cycle.Cycle.valid_scope(sub, clock, "clock_local"))
        self.assertFalse(cycle.Cycle.valid_scope({**sub, "livemode": True}, clock, "clock_local"))
        self.assertFalse(cycle.Cycle.valid_scope(sub, clock, "clock_other"))

    def test_webhook_is_restored_when_scenario_fails(self):
        runner = cycle.Cycle.__new__(cycle.Cycle)
        runner.wait_clock = Mock()
        runner.snapshot = Mock(return_value={"pagosLocales": []})
        hook = {"status": "enabled", "livemode": False, "url": "https://vortise-be.onrender.com/api/stripe/webhook"}
        runner.stripe = Mock(return_value=hook)
        runner.advance = Mock(side_effect=cycle.ApiError("test failure"))
        with self.assertRaises(cycle.ApiError):
            runner.run("renew")
        self.assertEqual([
            call("GET", "/webhook_endpoints/" + cycle.HOOK),
            call("POST", "/webhook_endpoints/" + cycle.HOOK, {"disabled": "true"}),
            call("POST", "/webhook_endpoints/" + cycle.HOOK, {"disabled": "false"})], runner.stripe.call_args_list)

    def test_previously_disabled_webhook_is_not_reenabled(self):
        runner = cycle.Cycle.__new__(cycle.Cycle)
        runner.wait_clock = Mock()
        runner.snapshot = Mock(return_value={"pagosLocales": []})
        runner.stripe = Mock(return_value={"status": "disabled", "livemode": False, "url": "https://vortise-be.onrender.com/api/stripe/webhook"})
        runner.advance = Mock(side_effect=cycle.ApiError("test failure"))
        with self.assertRaises(cycle.ApiError):
            runner.run("renew")
        self.assertEqual(1, runner.stripe.call_count)

    def test_recovery_must_update_the_same_invoice_row(self):
        before = {"pagosLocales": [{"id": 10, "estado": "FALLIDO"}]}
        after = {"stripeEstado": "active", "suscripcion": {"estado": "active"},
                 "pagosLocales": [{"id": 10, "estado": "PAGADO", "proximoIntento": None}]}
        self.assertTrue(cycle.Cycle.matches("recover", before, after))
        duplicated = {**after, "pagosLocales": after["pagosLocales"] + [{"id": 11, "estado": "PAGADO"}]}
        self.assertFalse(cycle.Cycle.matches("recover", before, duplicated))

    def test_stripe_success_without_local_update_is_not_a_pass(self):
        before = {"pagosLocales": []}
        after = {"stripeEstado": "active", "suscripcion": {"estado": "past_due"}, "pagosLocales": []}
        self.assertFalse(cycle.Cycle.matches("renew", before, after))
