#!/usr/bin/env python3
"""Run predeclared matched seed pairs, two isolated search processes at a time."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import threading
import time


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--protocol', type=Path, required=True)
    parser.add_argument('--dry-run', action='store_true')
    args = parser.parse_args()
    protocol_path = args.protocol.resolve()
    protocol = json.loads(protocol_path.read_text())
    root = protocol_path.parent
    runner = Path(__file__).with_name('run.py')
    state = {'status': 'RUNNING', 'startedAt': time.time(), 'jobs': {},
             'protocolSha256': hashlib.sha256(protocol_path.read_bytes()).hexdigest()}
    lock = threading.Lock()

    def save():
        temporary = root / 'status.tmp'
        temporary.write_text(json.dumps(state, indent=2, sort_keys=True) + '\n')
        temporary.replace(root / 'status.json')

    def command(job):
        return [sys.executable, str(runner), 'run', '--config', job['config'],
                '--output', job['output'], '--control', protocol['control']]

    def execute(job):
        name = job['name']
        with (root / (name + '.log')).open('w') as log:
            process = subprocess.Popen(command(job), stdout=log, stderr=subprocess.STDOUT)
            with lock:
                state['jobs'][name] = {'status': 'RUNNING', 'pid': process.pid,
                                       'startedAt': time.time(), 'output': job['output']}
                save()
            code = process.wait()
        with lock:
            entry = state['jobs'][name]
            entry.update(exitCode=code, finishedAt=time.time(), status='FAILED')
            if code == 0:
                result = json.loads((Path(job['output']) / 'results.json').read_text())
                count = len(result['attempts'])
                entry.update(attempts=count, stopReason=result['stopReason'],
                             positives=result['positiveScenarios'],
                             unavailable=result['nullFitnessAttempts'])
                entry['status'] = 'COMPLETE' if count == protocol['budgetPerMethod'] \
                    and result['integrity'] == 'PASS' else 'SHORT_RUN'
            save()
            return entry['status'] == 'COMPLETE'

    if args.dry_run:
        for pair in protocol['pairs']:
            for job in pair:
                print(json.dumps(command(job)))
        return
    if (root / 'status.json').exists():
        raise ValueError('Campaign already started; retain its evidence and use a new directory')
    save()
    try:
        with ThreadPoolExecutor(max_workers=2) as pool:
            for pair in protocol['pairs']:
                for path, expected in protocol['frozenFiles'].items():
                    if hashlib.sha256(Path(path).read_bytes()).hexdigest() != expected:
                        raise ValueError('Campaign input/code changed: ' + path)
                if shutil.disk_usage(root).free < protocol['minimumFreeBytes']:
                    raise ValueError('Insufficient free disk space to start the next seed pair')
                futures = [pool.submit(execute, job) for job in pair]
                completed = [future.result() for future in futures]
                if not all(completed):
                    raise ValueError('Seed pair failed or stopped short; inspect retained results')
        state['status'] = 'COMPLETE'
    except Exception as error:
        state.update(status='STOPPED', error=str(error))
        raise
    finally:
        state['finishedAt'] = time.time()
        save()


if __name__ == '__main__':
    main()
