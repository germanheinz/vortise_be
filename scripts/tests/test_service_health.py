import contextlib
import importlib.util
import io
import json
from pathlib import Path
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import unittest
from unittest.mock import patch
import urllib.error

spec = importlib.util.spec_from_file_location('service_health', Path(__file__).parents[1] / 'service-health.py')
health = importlib.util.module_from_spec(spec); spec.loader.exec_module(health)


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args): pass

    def do_GET(self):
        if self.path == '/redirect':
            self.send_response(302); self.send_header('Location', '/up'); self.end_headers(); return
        code, content = {'/up': (200, b'{"status":"UP"}'), '/down': (503, b'synthetic-private-message'),
            '/invalid': (200, b'synthetic-private-message'), '/large': (200, b'x' * 4097)}[self.path]
        self.send_response(code); self.end_headers(); self.wfile.write(content)


class ProbeTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = ThreadingHTTPServer(('127.0.0.1', 0), Handler)
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True); cls.thread.start()
        cls.url = 'http://127.0.0.1:' + str(cls.server.server_port)

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown(); cls.server.server_close(); cls.thread.join(timeout=5)

    def test_healthy_minimal_json(self):
        result = health.probe('fixture', self.url + '/up')
        self.assertTrue(result['ok']); self.assertEqual(result['http_status'], 200)

    def test_failure_does_not_publish_body(self):
        result = health.probe('fixture', self.url + '/down')
        self.assertFalse(result['ok']); self.assertEqual(result['http_status'], 503)
        self.assertNotIn('synthetic-private-message', json.dumps(result))

    def test_redirect_is_rejected_even_to_a_healthy_endpoint(self):
        result = health.probe('fixture', self.url + '/redirect')
        self.assertFalse(result['ok']); self.assertEqual(result['reason'], 'redirect_rejected')

    def test_invalid_or_oversized_response_is_not_healthy(self):
        for path in ('/invalid', '/large'):
            self.assertEqual(health.probe('fixture', self.url + path)['reason'], 'invalid_response')

    def test_timeout_is_bounded_and_error_is_generic(self):
        opener = unittest.mock.Mock(); opener.open.side_effect = urllib.error.URLError('synthetic-private-network-value')
        with patch.object(health.urllib.request, 'build_opener', return_value=opener):
            result = health.probe('fixture', self.url + '/up', timeout=0.2)
        self.assertEqual(opener.open.call_args.kwargs['timeout'], 0.2)
        self.assertEqual(result['reason'], 'transport_error')
        self.assertNotIn('synthetic-private-network-value', json.dumps(result))

    def test_remote_mode_requires_separate_opt_in_before_network(self):
        with patch('sys.argv', ['probe', '--environment', 'staging']), patch.object(health, 'probe') as request, contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit): health.main()
            request.assert_not_called()


if __name__ == '__main__': unittest.main()
