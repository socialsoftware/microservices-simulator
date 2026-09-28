#!/usr/bin/env python3
"""Prepare, run, summarize or replay a fixed-workload search."""
import argparse
import json
from pathlib import Path
import shutil
import sys
import time

from search import run, candidate_key
import catalogue
from fitness import assess, configuration
from runtime import Domain, IntegrityError, Runtime, batch, digest, package, prepare, read, save, retained_attempt


def verify_source(config, out):
    if package(Path(config['manifest']))['hashes'] != read(out / 'source-package-hashes.json'):
        raise IntegrityError('Original input package changed')


def control(config, out):
    runtime, domain = prepare(config, out)
    candidates = domain.resolve('0' * domain.width)
    if len(candidates) != 1:
        raise ValueError('No-fault control must resolve to one candidate')
    result = runtime.evaluate(out, candidates[0], 1, config.get('timeout', 180))
    domain.verify()
    runtime.verify()
    verify_source(config, out)
    save(out / 'control.json', result)
    if result['status'] != 'COMPLETE':
        raise ValueError('No-fault control unavailable; search not qualified')
    return result


def check_control(config, control_path):
    parent = control_path.parent
    reference = read(parent / 'config.json')
    for field in ('manifest', 'workload', 'runtime', 'recoveryCap'):
        if config[field] != reference[field]:
            raise ValueError('Control scope differs: ' + field)
    result = read(control_path)
    if result['status'] != 'COMPLETE' or result['candidate']['faultVector'].strip('0'):
        raise ValueError('A qualified no-fault control is required')
    for name, expected in result['reportHashes'].items():
        if digest(Path(result['directory']) / name) != expected:
            raise IntegrityError('Control evidence changed')
    verify_source(config, parent)
    return digest(control_path)


def require_scored_control(baseline, fitness):
    """Qualify execution and enabled evidence without selecting on its score."""
    if baseline.get('status') != 'COMPLETE' \
            or baseline.get('terminalStatus') not in ('SUCCESS', 'COMPENSATED', 'PARTIAL_COMPENSATED') \
            or baseline.get('scheduleConformance') not in ('EXACT', 'DEVIATED') \
            or assess(baseline, fitness)['fitnessScore'] is None:
        raise ValueError('Require valid measured no-fault control with complete enabled score')


def execute(config, out, control_path, catalogue_path=None):
    started = time.monotonic()
    config = {**config, 'fitness': configuration(config.get('fitness'))}
    control_hash = check_control(config, control_path)
    finite, seal = None, None
    if catalogue_path is not None:
        finite, seal = catalogue.load(catalogue_path, config, package(Path(config['manifest']))['hashes'])
        require_scored_control(read(control_path), config['fitness'])
    runtime, domain = prepare(config, out)
    catalogue_package_hashes = None
    if finite is not None:
        shutil.copytree(catalogue_path / 'package', out / 'package', dirs_exist_ok=True)
        catalogue_package_hashes = package(domain.manifest)['hashes']
        if catalogue_package_hashes != seal['packageHashes']:
            raise IntegrityError('Catalogue changed while copying')
        save(out / 'catalogue-reference.json', {'path': str(catalogue_path),
             'sha256': digest(catalogue_path / 'catalogue.json'), **seal})
    save(out / 'control-reference.json', {'path': str(control_path), 'sha256': control_hash})
    def emit(row):
        with (out / 'progress.jsonl').open('a') as stream:
            stream.write(json.dumps(row, sort_keys=True) + '\n')
    try:
        result = run(finite if finite is not None else domain,
                     lambda c, n: runtime.evaluate(out, c, n, config.get('timeout', 180)),
                     strategy=config['strategy'], seed=config['seed'], budget=config['budget'],
                     population=config.get('population', 8), mutation=config.get('mutation', 0.3),
                     stall_limit=config.get('stallLimit', 100), emit=emit, fitness=config['fitness'],
                     exploration='uniform-unseen' if finite is not None else 'per-saga')
        domain.verify()
        runtime.verify()
        verify_source(config, out)
        if finite is not None:
            catalogue.load(catalogue_path, config, read(out / 'source-package-hashes.json'))
            if package(domain.manifest)['hashes'] != catalogue_package_hashes:
                raise IntegrityError('Search modified the complete catalogue')
        result['integrity'] = 'PASS'
    except IntegrityError as error:
        save(out / 'failure.json', {'stopReason': 'INTEGRITY_FAILURE', 'error': str(error)})
        raise
    result['packageFinalHashes'] = package(domain.manifest)['hashes']
    result['generationRequests'] = len(domain.requests)
    result['generationSeconds'] = sum(r['process']['wallSeconds'] for r in domain.requests)
    result['candidateDomain'] = 'complete-catalogue' if finite is not None else 'on-demand'
    if finite is not None:
        inventory = read(catalogue_path / 'domain-count.json')
        result['catalogue'] = {'sha256': digest(catalogue_path / 'catalogue.json'),
             'candidateCount': len(finite.candidates), 'complete': True,
             'generationSeconds': inventory['generatorWallSeconds'],
             'enumerationWallSeconds': inventory['enumerationWallSeconds'],
             'note': 'Shared preparation cost; not included in this search wall time.'}
    result['applicationSeconds'] = sum(a.get('wallSeconds', 0) for a in result['attempts'])
    result['commandWallSeconds'] = time.monotonic() - started
    save(out / 'results.json', result)
    summarize(out)
    return result


def summarize(out):
    r = read(out / 'results.json')
    lines = ['# Fixed-workload search result', '',
        f"Strategy: {r['strategy']}; seed: {r['seed']}; attempts: {len(r['attempts'])}/{r['budget']}.",
        f"Domain: {r.get('candidateDomain', 'on-demand')}; sampling: {r.get('samplingPolicy', 'legacy')}.",
        f"Fitness: `{json.dumps(r.get('fitness', {'policy': r['fitnessPolicy']}), sort_keys=True)}`.",
        f"Stop: {r['stopReason']}. Best score: {r.get('bestScore', r['bestI'])}. Best I: {r['bestI']}. Positive-score scenarios: {r['positiveScenarios']}.",
        f"Null fitness: {r['nullFitnessAttempts']}; duplicate proposals: {r['duplicates']}.", '',
        '| Attempt | Vector | I | A | A coverage | Lost copied updates | Lost copied coverage | Score | Best score |',
        '| --- | --- | --- | --- | --- | --- | --- | --- | --- |']
    for a in r['attempts']:
        lines.append(f"| {a['attempt']} | {a['vector']} | {a['I']} | {a.get('A')} | {a.get('ACoverage')} | {a.get('lostCopiedUpdateCount')} | {a.get('lostCopiedUpdateCoverage')} | {a.get('fitnessScore', a['I'])} | {a['bestSoFar']} |")
    (out / 'SUMMARY.md').write_text('\n'.join(lines) + '\n')


def replay(attempt_path, out):
    attempt = read(attempt_path)
    original = read(attempt_path.parent / 'replay.json')
    snapshot = attempt_path.parent / 'package'
    if package(snapshot / 'scenario-catalog-manifest.json')['hashes'] != attempt['packageHashes']:
        raise IntegrityError('Replay snapshot changed')
    runtime = Runtime(original['runtime'])
    runtime.verify()
    out.mkdir(parents=True, exist_ok=False)
    shutil.copytree(snapshot, out / 'package')
    result = runtime.evaluate(out, attempt['candidate'], 1, 180)
    runtime.verify()
    return result


def rescore(results_path, fitness, out):
    fitness = configuration(fitness)
    original = read(results_path)
    rows = []
    for attempt in original['attempts']:
        path = Path(attempt['directory']) / 'attempt.json'
        measured = retained_attempt(path)
        if measured['candidate']['id'] != attempt['scenarioId']:
            raise IntegrityError('Result/attempt scenario mismatch')
        rows.append({'attempt': attempt['attempt'], 'key': attempt['key'],
                     'scenarioId': attempt['scenarioId'], 'I': measured['I'], 'A': measured.get('A'),
                     'lostCopiedUpdateCount': measured.get('lostCopiedUpdateCount'),
                     'lostCopiedUpdateReportSha256': measured.get('reportHashes', {}).get(
                         'execution-report-lost-copied-updates.json'),
                     'sourceAttempt': str(path), 'sourceSha256': digest(path), **assess(measured, fitness)})
    out.mkdir(parents=True, exist_ok=False)
    save(out / 'rescore.json', {'kind': 'OFFLINE_RESCORE', 'newApplicationExecutions': 0,
        'sourceResults': str(results_path), 'sourceSha256': digest(results_path),
        'fitness': fitness, 'attempts': rows,
        'note': 'Revalues the measured sequence; does not simulate how this policy would search.'})


def main():
    p = argparse.ArgumentParser(description=__doc__)
    sub = p.add_subparsers(dest='command', required=True)
    for name in ('prepare', 'catalogue', 'control', 'run'):
        s = sub.add_parser(name)
        s.add_argument('--config', type=Path, required=True)
        s.add_argument('--output', type=Path, required=True)
        if name == 'run':
            s.add_argument('--control', type=Path, required=True)
            s.add_argument('--catalogue', type=Path,
                           help='Complete catalogue prepared by the catalogue command; enables uniform exploration')
    s = sub.add_parser('summary'); s.add_argument('--output', type=Path, required=True)
    s = sub.add_parser('replay'); s.add_argument('--attempt', type=Path, required=True)
    s.add_argument('--output', type=Path, required=True)
    s = sub.add_parser('rescore'); s.add_argument('--results', type=Path, required=True)
    s.add_argument('--fitness', type=Path, required=True)
    s.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    out = a.output.resolve()
    if a.command == 'prepare':
        prepare(read(a.config), out)
        print((out / 'scope.json').read_text())
    elif a.command == 'catalogue':
        config = read(a.config)
        runtime, domain = prepare(config, out)
        catalogue.build(domain, runtime)
        verify_source(config, out)
        print((out / 'domain-count.json').read_text())
    elif a.command == 'control':
        control(read(a.config), out)
    elif a.command == 'run':
        execute(read(a.config), out, a.control.resolve(), a.catalogue.resolve() if a.catalogue else None)
    elif a.command == 'replay':
        replay(a.attempt.resolve(), out)
    elif a.command == 'rescore':
        rescore(a.results.resolve(), read(a.fitness), out)
    else:
        summarize(out)


if __name__ == '__main__':
    main()
