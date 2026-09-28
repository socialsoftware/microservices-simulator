#!/usr/bin/env python3
"""Loopback-only experiment workbench; Python 3.10+, no third-party dependencies."""
import argparse
import copy
from contextlib import contextmanager
import csv
import fcntl
import io
import json
import math
import mimetypes
from pathlib import Path
import secrets
import subprocess
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

from data import Catalog, CRITERIA, POLICIES, SCHEMAS, identifier, journal, optional, read
from worker import save

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[1]


def active(directory):
    with (directory / 'worker.lock').open('a') as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            fcntl.flock(lock, fcntl.LOCK_UN)
            return False
        except BlockingIOError:
            return True


class Jobs:
    def __init__(self, repo, catalog):
        self.repo = Path(repo)
        self.catalog = catalog
        self.root = self.repo / 'verifiers/target/webui/jobs'
        self.root.mkdir(parents=True, exist_ok=True)
        self.lock = threading.Lock()
        self.children = []

    @contextmanager
    def dispatch_lock(self):
        with self.lock, (self.root / 'dispatch.lock').open('a') as lock:
            fcntl.flock(lock, fcntl.LOCK_EX)
            try:
                yield
            finally:
                fcntl.flock(lock, fcntl.LOCK_UN)

    def list(self):
        result = []
        for path in sorted(self.root.glob('*/job.json'), reverse=True):
            job = optional(path)
            if not job:
                continue
            running = active(path.parent)
            if job['state'] in ('QUEUED', 'RUNNING') and not running and time.time() - job['createdAt'] > 10:
                job = dict(job, state='INTERRUPTED')
            status = optional(path.parent / 'output/status.json')
            job['progress'] = status
            job['running'] = running
            job['resultId'] = identifier(path.parent / 'output/results.json') if (path.parent / 'output/results.json').exists() else None
            job['resultUpdated'] = (path.parent / 'output/results.json').stat().st_mtime_ns if job['resultId'] else None
            job['canPause'] = job['mode'] == 'live' and running and (path.parent / 'output/protocol.json').exists()
            job['canResume'] = job['mode'] == 'live' and not running and job['state'] == 'PAUSED'
            if job['mode'] == 'live':
                try:
                    rows = journal(path.parent / 'output/decisions.jsonl')
                    job['latestDecision'] = rows[-1] if rows else None
                except (OSError, ValueError):
                    job['latestDecision'] = None
            result.append(job)
        # Reap supervisors completed while this server stayed alive.
        self.children = [p for p in self.children if p.poll() is None]
        return result

    def prepare(self, body):
        if not isinstance(body, dict):
            raise ValueError('Expected an experiment configuration')
        preset = self.catalog.configs.get(body.get('configId'))
        if not preset:
            raise ValueError('Choose an indexed allocation configuration')
        source = Path(preset['path'])
        config = read(source)
        mode = SCHEMAS.get(config.get('schemaVersion'))
        if not mode:
            raise ValueError('Unsupported configuration schema')
        for key, low, high in [('budget', 1, 1000000), ('seed', 0, 2147483647)]:
            value = body.get(key, config.get(key))
            if type(value) is not int or not low <= value <= high:
                raise ValueError(f'{key} must be an integer between {low} and {high}')
            config[key] = value
        policy = body.get('policy', config.get('policy'))
        if policy not in POLICIES:
            raise ValueError('Unsupported allocation policy')
        config['policy'] = policy
        parameters = config.get('policyParameters', {})
        allowed = ('exploration', 'ridge') if 'linucb' in policy else ('exploration',) if 'ucb' in policy else ()
        config['policyParameters'] = {k: v for k, v in parameters.items() if k in allowed}
        weights = body.get('weights', config.get('fitness', {}).get('weights', {}))
        if not isinstance(weights, dict) or set(weights) != set(CRITERIA) or any(
                type(x) not in (int, float) or not math.isfinite(x) or x < 0 for x in weights.values()) or not any(weights.values()):
            raise ValueError('Provide five finite nonnegative weights, with at least one positive')
        config['fitness'] = {'policy': 'weighted-criteria-v2', 'weights': weights}
        rows = config.get('workloads', [])
        selected = body.get('workloads', list(range(len(rows))))
        if not isinstance(selected, list) or not selected or len(set(selected)) != len(selected) or any(
                type(i) is not int or i < 0 or i >= len(rows) for i in selected):
            raise ValueError('Select at least one valid workload')
        config['workloads'] = [copy.deepcopy(rows[i]) for i in selected]
        required = ['config', 'catalogue', 'control' if mode == 'live' else 'reference']
        for row in config['workloads']:
            for field in required:
                value = row.get(field)
                if not isinstance(value, str) or not value:
                    raise ValueError('Missing workload input: ' + field)
                path = (source.parent / value).resolve()
                if not path.exists():
                    raise ValueError('Prepared input is missing: ' + str(path))
                row[field] = str(path)
        # Preserve only the runner's public input fields, not resolved metadata.
        config = {k: v for k, v in config.items() if k in ('schemaVersion', 'seed', 'budget', 'policy',
                                                        'policyParameters', 'fitness', 'workloads')}
        return config, mode, source

    def spawn(self, directory):
        with (directory / 'supervisor.log').open('ab') as log:
            child = subprocess.Popen([sys.executable, str(HERE / 'worker.py'), str(directory)],
                                     cwd=self.repo, stdin=subprocess.DEVNULL, stdout=log, stderr=log,
                                     start_new_session=True, close_fds=True)
        self.children.append(child)

    def launch(self, body):
        with self.dispatch_lock():
            # Keep local resource use predictable; no implicit parallel Docker runs.
            if any(j['running'] or j['state'] == 'QUEUED' for j in self.list()):
                raise ValueError('An experiment is already running. Wait or pause it before starting another.')
            config, mode, source = self.prepare(body)
            name = body.get('name', '')
            if not isinstance(name, str):
                raise ValueError('Experiment name must be text')
            job_id = time.strftime('%Y%m%d-%H%M%S') + '-' + secrets.token_hex(4)
            directory = self.root / job_id
            directory.mkdir()
            save(directory / 'configuration.json', config)
            runner = self.repo / 'verifiers/experiments/fixed-workload-ga' / ('allocate.py' if mode == 'recorded' else 'live_allocate.py')
            command = [sys.executable, str(runner)] + ([] if mode == 'recorded' else ['run'])
            command += ['--config', str(directory / 'configuration.json'), '--output', str(directory / 'output')]
            job = {'id': job_id, 'name': name.strip()[:120] or f"{config['policy']} · seed {config['seed']}",
                   'state': 'QUEUED', 'mode': mode, 'createdAt': time.time(), 'repo': str(self.repo),
                   'source': str(source), 'command': command, 'budget': config['budget'], 'seed': config['seed']}
            save(directory / 'job.json', job)
            self.spawn(directory)
            return job

    def action(self, job_id, action):
        with self.dispatch_lock():
            job = next((j for j in self.list() if j['id'] == job_id), None)
            if not job:
                raise ValueError('Unknown job')
            directory = self.root / job['id']
            if action == 'pause' and job['canPause']:
                (directory / 'output/PAUSE').touch()
                return {'message': 'Pause requested; the current attempt will finish first.'}
            if action == 'resume' and job['canResume']:
                if any(j['running'] or j['state'] == 'QUEUED' for j in self.list()):
                    raise ValueError('Another experiment is running')
                original = read(directory / 'job.json')
                original['command'][2] = 'resume'
                original.update(state='QUEUED', createdAt=time.time())
                save(directory / 'job.json', original)
                self.spawn(directory)
                return {'message': 'Resume requested; the runner will verify the retained state.'}
            raise ValueError('This action is not available for the current job state')

    def log(self, job_id):
        if not any(j['id'] == job_id for j in self.list()):
            raise ValueError('Unknown job')
        path = self.root / job_id / 'runner.log'
        if not path.exists():
            return ''
        with path.open('rb') as stream:
            stream.seek(max(0, path.stat().st_size - 32000))
            return stream.read().decode('utf-8', errors='replace')


class Server(ThreadingHTTPServer):
    daemon_threads = True

    def __init__(self, address, catalog, jobs):
        self.catalog, self.jobs = catalog, jobs
        self.token = secrets.token_urlsafe(32)
        super().__init__(address, Handler)


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        if args and str(args[1]) not in ('200', '202'):
            super().log_message(fmt, *args)

    def send(self, value, status=200, content_type='application/json; charset=utf-8', filename=None):
        if content_type.startswith('application/json'):
            value = json.dumps(value, allow_nan=False).encode()
        elif isinstance(value, str):
            value = value.encode()
        self.send_response(status)
        self.send_header('Content-Type', content_type)
        self.send_header('Content-Length', str(len(value)))
        self.send_header('Cache-Control', 'no-store')
        self.send_header('X-Content-Type-Options', 'nosniff')
        self.send_header('Content-Security-Policy', "default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' blob:; connect-src 'self'; frame-ancestors 'none'")
        if filename:
            self.send_header('Content-Disposition', f'attachment; filename="{filename}"')
        self.end_headers()
        self.wfile.write(value)

    def allowed_host(self):
        return self.headers.get('Host') in {f'127.0.0.1:{self.server.server_port}', f'localhost:{self.server.server_port}'}

    def do_GET(self):
        if not self.allowed_host():
            return self.send({'error': 'Only loopback access is supported'}, 403)
        try:
            url = urlparse(self.path)
            query = parse_qs(url.query)
            if url.path == '/api/catalog':
                return self.send({**self.server.catalog.snapshot(), 'token': self.server.token,
                                  'policies': POLICIES, 'criteria': CRITERIA})
            if url.path == '/api/run':
                return self.send(self.server.catalog.detail(query.get('id', [''])[0]))
            if url.path == '/api/export':
                detail = self.server.catalog.detail(query.get('id', [''])[0])
                if query.get('format', ['csv'])[0] == 'json':
                    return self.send(detail, filename='experiment.json')
                output = io.StringIO()
                writer = csv.writer(output)
                writer.writerow(['attempt', 'scenario', 'workload', 'score', 'terminal_status', 'conformance'])
                for row in detail['attemptsDetail']:
                    values = [row[k] for k in ('attempt', 'scenario', 'workload', 'score', 'status', 'conformance')]
                    writer.writerow([("'" + v) if isinstance(v, str) and v.startswith(('=', '+', '-', '@')) else v for v in values])
                return self.send(output.getvalue(), content_type='text/csv; charset=utf-8', filename='attempts.csv')
            if url.path == '/api/jobs':
                return self.send(self.server.jobs.list())
            if url.path == '/api/log':
                return self.send({'text': self.server.jobs.log(query.get('id', [''])[0])})
            static = {'/': 'index.html', '/app.js': 'app.js', '/style.css': 'style.css'}
            if url.path in static:
                path = HERE / 'static' / static[url.path]
                return self.send(path.read_bytes(), content_type=mimetypes.guess_type(path)[0] + '; charset=utf-8')
            return self.send({'error': 'Not found'}, 404)
        except (ValueError, OSError, KeyError, TypeError) as error:
            self.send({'error': str(error)}, 400)

    def do_POST(self):
        origin = self.headers.get('Origin')
        if not self.allowed_host() or self.headers.get('X-Workbench-Token') != self.server.token or (
                origin and origin != 'http://' + self.headers.get('Host', '')):
            return self.send({'error': 'Refresh the local workbench before making changes'}, 403)
        try:
            length = int(self.headers.get('Content-Length', '0'))
            if not 0 < length <= 65536 or self.headers.get('Content-Type') != 'application/json':
                raise ValueError('Expected a JSON request up to 64 KiB')
            body = json.loads(self.rfile.read(length))
            if not isinstance(body, dict):
                raise ValueError('Expected a JSON object')
            if self.path == '/api/refresh':
                self.server.catalog.start_scan()
                return self.send({'scanning': True}, 202)
            if self.path == '/api/validate':
                config, mode, source = self.server.jobs.prepare(body)
                return self.send({'configuration': config, 'mode': mode, 'source': str(source),
                                  'message': 'Input paths and settings checked. The runner verifies hashes, controls and runtime before search.'})
            if self.path == '/api/launch':
                return self.send(self.server.jobs.launch(body), 202)
            if self.path == '/api/action':
                return self.send(self.server.jobs.action(body.get('id'), body.get('action')), 202)
            self.send({'error': 'Not found'}, 404)
        except (ValueError, OSError, KeyError, TypeError) as error:
            self.send({'error': str(error)}, 400)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--port', type=int, default=8765)
    parser.add_argument('--root', type=Path, action='append', help='Additional read-only artifact root')
    args = parser.parse_args()
    catalog = Catalog(REPO, [REPO / 'verifiers/target', *(args.root or [])])
    jobs = Jobs(REPO, catalog)
    server = Server(('127.0.0.1', args.port), catalog, jobs)
    catalog.start_scan()
    print(f'Experiment workbench: http://127.0.0.1:{server.server_port}', flush=True)
    print('Closing this server does not stop detached experiments.', flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == '__main__':
    main()
