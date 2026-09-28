#!/usr/bin/env python3
"""Measure a complete workload map with bounded concurrency and safe pause/resume.

This collects evidence only. Search replay is a separate experiment.
"""
import argparse
from concurrent.futures import FIRST_COMPLETED, ThreadPoolExecutor, wait
from contextlib import contextmanager
import fcntl
import json
from pathlib import Path
import shutil
import signal
import subprocess
import threading
import time

from catalogue import enumerated_domain
from fitness import assess, configuration
from run import check_control, require_scored_control
from runtime import Runtime, IntegrityError, batch, digest, package, read, retained_attempt, save


@contextmanager
def exclusive(out):
    with (out / '.lock').open('a') as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            raise ValueError('Campaign already running') from None
        try:
            yield
        finally:
            fcntl.flock(lock, fcntl.LOCK_UN)


def check_orphans(out):
    """A killed coordinator may leave Docker attempts alive; never overlap them."""
    ids = subprocess.run(['docker', 'ps', '-q'], check=True, capture_output=True,
                         text=True, timeout=20).stdout.split()
    if not ids:
        return
    containers = json.loads(subprocess.run(['docker', 'inspect', *ids], check=True,
                            capture_output=True, text=True, timeout=20).stdout)
    names = [c['Name'] for c in containers if any(
        m.get('Source') == str(out) for m in c.get('Mounts', []))]
    if names:
        raise ValueError('Previous attempts still running; wait before resuming: ' + ', '.join(names))


def restore(out, candidates, runtime_config, package_hashes):
    """Recover completed attempts, including a finish just before coordinator death.

    Incomplete directories are preserved and retried with a new attempt number.
    Completed null assessments are retained, never retried until they become positive.
    """
    observations = {}
    next_number = 1
    for receipt in (out / 'receipts').glob('attempt-*.json'):
        if not (out / receipt.stem / 'attempt.json').exists():
            raise IntegrityError('Completed attempt missing: ' + receipt.stem)
    for directory in sorted(out.glob('attempt-*')):
        if not directory.is_dir():
            continue
        next_number = max(next_number, int(directory.name.split('-')[1]) + 1)
        path = directory / 'attempt.json'
        receipt = out / 'receipts' / (directory.name + '.json')
        if receipt.exists() and (not path.exists() or digest(path) != read(receipt)['sha256']):
            raise IntegrityError('Completed attempt changed: ' + directory.name)
        if not path.exists():
            continue
        attempt = retained_attempt(path)
        candidate = attempt['candidate']; key = candidate['key']
        if key not in candidates or candidate != candidates[key]:
            raise IntegrityError('Attempt outside frozen selection')
        replay = read(directory / 'replay.json')
        if replay['runtime'] != runtime_config or attempt['packageHashes'] != package_hashes:
            raise IntegrityError('Attempt runtime/package differs')
        if key in observations:
            raise IntegrityError('Duplicate completed candidate')
        observations[key] = attempt
        save(receipt, {'key': key, 'sha256': digest(path)})
    return observations, next_number


def collect(out, candidates, runtime, policy, workers, timeout, observations, next_number, stop):
    """Only workers jobs are in flight. Stop requests drain these, not a full queue."""
    pending = iter(k for k in sorted(candidates) if k not in observations)
    started = time.monotonic()
    initial = len(observations)
    jobs = {}
    errors = []
    def stopping():
        return stop.is_set() or (out / 'PAUSE').exists() or bool(errors)
    def status(stage):
        save(out / 'status.json', {'stage': stage, 'measured': len(observations),
             'total': len(candidates), 'inFlight': len(jobs), 'workers': workers,
             'sessionMeasured': len(observations) - initial,
             'sessionWallSeconds': time.monotonic() - started, 'updatedAt': time.time(),
             'errors': errors})
    status('MEASURING_REFERENCE')
    with ThreadPoolExecutor(max_workers=workers) as pool:
        while True:
            while len(jobs) < workers and not stopping():
                key = next(pending, None)
                if key is None:
                    break
                number = next_number; next_number += 1
                jobs[pool.submit(runtime.evaluate, out, candidates[key], number, timeout)] = (key, number)
            status('DRAINING' if stopping() else 'MEASURING_REFERENCE')
            if not jobs:
                break
            done, _ = wait(jobs, timeout=0.5, return_when=FIRST_COMPLETED)
            for future in done:
                key, number = jobs.pop(future)
                try:
                    value = future.result()
                    path = out / f'attempt-{number:03d}/attempt.json'
                    save(out / 'receipts' / f'attempt-{number:03d}.json',
                         {'key': key, 'sha256': digest(path)})
                    observations[key] = value
                except Exception as error:
                    errors.append(str(error))
    if errors:
        status('FAILED')
        raise RuntimeError('; '.join(errors))
    runtime.verify()
    if len(observations) == len(candidates):
        save(out / 'reference.json', {'candidates': candidates, 'observations': observations,
             'fitness': {k: assess(v, policy) for k, v in observations.items()}})
        status('COMPLETE')
    else:
        status('PAUSED')
    return read(out / 'status.json')


def execute(config_path, enumeration, control, out, workers=2, resume=False, keys_path=None,
            stop=None):
    if type(workers) is not int or workers < 1:
        raise ValueError('workers must be positive')
    config = read(config_path)
    policy = configuration(config.get('fitness'))
    control_hash = check_control(config, control)
    require_scored_control(read(control), policy)
    domain, hashes = enumerated_domain(enumeration, config['workload'])
    keys = read(keys_path) if keys_path else sorted(domain.candidates)
    if not isinstance(keys, list) or not keys or len(set(keys)) != len(keys) \
            or not set(keys) <= domain.candidates.keys():
        raise ValueError('Selection must contain distinct catalogue keys')
    candidates = {k: domain.candidates[k] for k in sorted(keys)}
    runtime = Runtime(config['runtime']); runtime.verify()
    sources = [Path(__file__).parent / name for name in
               ('measure_map.py', 'runtime.py', 'catalogue.py', 'fitness.py', 'search.py', 'run.py')]
    sources.append(Path(batch.__file__))
    identity = {'schemaVersion': 'resumable-reference.v1', 'config': config,
                'enumeration': str(enumeration.resolve()), 'packageHashes': hashes,
                'enumerationHashes': {n: digest(enumeration / n) for n in ('domain-count.json', 'requests.json')},
                'controlSha256': control_hash, 'keys': sorted(keys),
                'fullCatalogueCount': len(domain.candidates),
                'sourceHashes': {str(p.resolve()): digest(p) for p in sources}}
    out = out.resolve()
    out.mkdir(parents=True, exist_ok=resume)
    with exclusive(out):
        if resume:
            if read(out / 'protocol.json') != identity:
                raise IntegrityError('Campaign inputs or runner changed; cannot resume')
            check_orphans(out)
        else:
            shutil.copytree(enumeration / 'package', out / 'package')
            (out / 'receipts').mkdir()
            (out / 'source').mkdir()
            for p in sources:
                shutil.copy2(p, out / 'source' / ('batch-run.py' if p == sources[-1] else p.name))
            save(out / 'protocol.json', identity)
        if package(out / 'package/scenario-catalog-manifest.json')['hashes'] != hashes:
            raise IntegrityError('Campaign package changed')
        observations, number = restore(out, candidates, config['runtime'], hashes)
        # An explicit resume acknowledges the previous pause. A new request during
        # execution is left in place until the next explicit resume.
        if resume:
            (out / 'PAUSE').unlink(missing_ok=True)
        sessions = read(out / 'sessions.json') if (out / 'sessions.json').exists() else []
        sessions.append({'workers': workers, 'startedAt': time.time(), 'resumed': resume,
                         'alreadyMeasured': len(observations)})
        save(out / 'sessions.json', sessions)
        try:
            result = collect(out, candidates, runtime, policy, workers, config.get('timeout', 180),
                             observations, number, stop or threading.Event())
            if package(out / 'package/scenario-catalog-manifest.json')['hashes'] != hashes \
                    or any(digest(Path(p)) != h for p, h in identity['sourceHashes'].items()):
                raise IntegrityError('Campaign package/runner changed during execution')
            return result
        except Exception as error:
            save(out / 'status.json', {'stage': 'FAILED', 'measured': len(observations),
                 'total': len(candidates), 'inFlight': 0, 'error': str(error), 'updatedAt': time.time()})
            raise
        finally:
            sessions[-1].update(finishedAt=time.time(), status=read(out / 'status.json'))
            save(out / 'sessions.json', sessions)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='command', required=True)
    p = sub.add_parser('run')
    for name in ('config', 'enumeration', 'control', 'output'):
        p.add_argument('--' + name, required=True, type=Path)
    p.add_argument('--workers', type=int, default=2)
    p.add_argument('--resume', action='store_true')
    p.add_argument('--keys', type=Path, help='Optional fixed subset for a pilot; frozen across resume')
    for name in ('pause', 'status'):
        p = sub.add_parser(name); p.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    if args.command == 'pause':
        if not (args.output / 'protocol.json').exists():
            parser.error('Not a campaign directory')
        (args.output / 'PAUSE').touch()
        print('Pause requested. Wait for status PAUSED or COMPLETE and inFlight=0 before sleeping.')
    elif args.command == 'status':
        print(json.dumps(read(args.output / 'status.json'), indent=2))
    else:
        stop = threading.Event()
        for sig in (signal.SIGINT, signal.SIGTERM):
            signal.signal(sig, lambda *_: stop.set())
        print(json.dumps(execute(args.config, args.enumeration, args.control, args.output,
              args.workers, args.resume, args.keys, stop), indent=2))


if __name__ == '__main__':
    main()
