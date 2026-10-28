#!/usr/bin/env python3
"""One-shot availability probe. No scheduling, notifications or credentials."""
import argparse
import json
import time
import urllib.error
import urllib.request

TARGETS = {
    'local': [('backend_readiness', 'http://127.0.0.1:18081/actuator/health/readiness'),
              ('frontend_runtime', 'http://127.0.0.1:3001/api/health')],
    'staging': [('backend_readiness', 'https://vortise-be.onrender.com/actuator/health/readiness'),
                ('frontend_runtime', 'https://vortise-fe.vercel.app/api/health')],
}


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, message, headers, url):
        return None


def probe(name, url, timeout=5):
    start = time.monotonic()
    status = None
    healthy = False
    reason = 'transport_error'
    try:
        request = urllib.request.Request(url, headers={'Accept': 'application/json', 'User-Agent': 'gestion-healthcheck/1'})
        with urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect()).open(request, timeout=timeout) as response:
            status = response.status
            body = response.read(4097)
            if len(body) > 4096:
                reason = 'invalid_response'
            else:
                try:
                    value = json.loads(body)
                    healthy = status == 200 and isinstance(value, dict) and value.get('status') == 'UP'
                    reason = 'healthy' if healthy else 'unavailable'
                except (ValueError, UnicodeError):
                    reason = 'invalid_response'
    except urllib.error.HTTPError as error:
        status = error.code
        reason = 'redirect_rejected' if 300 <= status < 400 else 'unavailable'
        error.close()
    except (urllib.error.URLError, TimeoutError, OSError, ValueError):
        pass
    return {'target': name, 'ok': healthy, 'http_status': status, 'reason': reason,
            'duration_ms': round((time.monotonic() - start) * 1000)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--environment', choices=TARGETS, default='local')
    parser.add_argument('--allow-staging', action='store_true', help='Only after explicit remote probe approval')
    parser.add_argument('--timeout', type=float, default=5)
    args = parser.parse_args()
    if args.environment == 'staging' and not args.allow_staging:
        parser.error('Staging probing requires explicit opt-in and execution approval')
    if not 0.1 <= args.timeout <= 30:
        parser.error('Timeout must be between0.1 and30seconds')
    results = [probe(name, url, args.timeout) for name, url in TARGETS[args.environment]]
    print(json.dumps({'environment': args.environment, 'checks': results}))
    return 0 if all(result['ok'] for result in results) else 1


if __name__ == '__main__':
    raise SystemExit(main())
