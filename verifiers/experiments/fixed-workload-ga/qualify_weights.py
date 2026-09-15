#!/usr/bin/env python3
"""Revalue retained evidence, then qualify configurable feedback with 28 fresh attempts."""
import argparse
import importlib.util
from pathlib import Path
import shutil

from fitness import CRITERIA, LEGACY, WEIGHTED, assess
from qualify import freeze_runtime, OVERLAY, UPDATE, TARGET
from runtime import batch, digest, read, save, prepare, retained_attempt, validate_read
from run import control, execute, verify_source, rescore
from search import run, fault_coordinates, stable

HERE = Path(__file__).resolve().parent
POLICIES = {'legacy': {'policy': LEGACY}, **{
    name: {'policy': WEIGHTED, 'weights': dict(zip(CRITERIA, values))}
    for name, values in [('persistent', (1, 1, 1, 0)), ('combined', (1, 1, 1, 1)),
                         ('read-only', (0, 0, 0, 1))]}}


def offline(out):
    reference = TARGET / 'fixed-workload-ga/discovery-01/reference-map.json'
    data = read(reference)
    measured = {}
    rows = []
    for key, entry in sorted(data['observations'].items()):
        path = Path(entry['attemptPath'])
        if digest(path) != entry['attemptSha256']:
            raise ValueError('Reference attempt changed')
        r = retained_attempt(path)
        measured[key] = r
        rows.append({'key': key, 'I': r['I'], 'A': r.get('A'), 'source': str(path),
                     'sha256': digest(path), 'scores': {name: assess(r, policy) for name, policy in POLICIES.items()}})
    hashes = read(OVERLAY / 'artifact-hashes.json')
    for selected in read(OVERLAY / 'selection.json'):
        directory = OVERLAY / selected['label']
        reports = {}
        for name in ('execution-report.json', 'execution-report.impact.json',
                     'execution-report.impact-v2.json', 'execution-report.saga-read-exposure.json'):
            path = directory / name
            if digest(path) != hashes[str(path.relative_to(OVERLAY))]:
                raise ValueError('Retained update report changed')
            reports[name] = read(path)
        e, v1, v2, a = reports.values()
        batch.validate_reports(e, v1, v2, selected['workload'], selected['scenario'], selected['vector'])
        status, i = batch.assessment_result(e, v2)
        r = {'status': status, 'I': i, 'terminalStatus': e['terminalStatus'],
             'scheduleConformance': e['scheduleConformance'], 'impactCategories': v2['categoryResults'],
             **validate_read(a, e, directory, OVERLAY / 'package/scenario-catalog-manifest.json')}
        rows.append({'label': selected['label'], 'I': i, 'A': r['A'],
                     'source': str(directory), 'reportHashes': {n: digest(directory / n) for n in reports},
                     'scores': {name: assess(r, policy) for name, policy in POLICIES.items()}})
    save(out / 'offline.json', {'kind': 'OFFLINE_RESCORE', 'newApplicationExecutions': 0,
         'referenceSha256': digest(reference), 'policies': POLICIES, 'rows': rows})

    # Regression of the default policy against the frozen pre-change implementation.
    source = TARGET / 'fixed-workload-ga/discovery-01/source/search.py'
    spec = importlib.util.spec_from_file_location('historical_search', source)
    old = importlib.util.module_from_spec(spec); spec.loader.exec_module(old)
    workload = read(TARGET / 'fixed-workload-ga/qualification-01/benchmark-control/scope.json')['workload']
    class RecordedDomain:
        coordinates = fault_coordinates(workload)
        width = sum(len(c) - 1 for c in coordinates)
        def resolve(self, vector):
            return sorted([c for c in data['candidates'].values() if c['faultVector'] == vector],
                          key=lambda c: stable(c['actions']))
        def exhausted(self, seen):
            return set(data['candidates']) <= seen
    for seed in (11, 29, 47):
        for strategy in ('ga', 'random'):
            args = dict(strategy=strategy, seed=seed, budget=29)
            a = old.run(RecordedDomain(), lambda c, n: measured[c['key']], **args)
            b = run(RecordedDomain(), lambda c, n: measured[c['key']], **args)
            if [x['key'] for x in a['attempts']] != [x['key'] for x in b['attempts']] \
                    or a['duplicates'] != b['duplicates'] or a['bestI'] != b['bestI']:
                raise ValueError('Legacy search behavior changed')
    save(out / 'legacy-regression.json', {'status': 'PASS', 'policies': ['ga', 'random'],
         'seeds': [11, 29, 47], 'historicalSource': str(source), 'sha256': digest(source),
         'mode': 'Recorded-feedback replay; no new application executions'})
    rescore(TARGET / 'fixed-workload-ga/qualification-01/ga-29/results.json',
            POLICIES['combined'], out / 'rescore-command')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    out = args.output.resolve(); out.mkdir(parents=True, exist_ok=False)
    source = out / 'source'; source.mkdir()
    for path in HERE.glob('*.py'):
        shutil.copy2(path, source / path.name)
    save(out / 'protocol.json', {'workload': UPDATE, 'policies': POLICIES,
        'arms': ['ga-persistent', 'ga-combined', 'random-combined'], 'seed': 29,
        'budgetPerArm': 8, 'population': 4, 'mutation': 0.3, 'recoveryCap': 20,
        'maxNewApplicationExecutions': 28, 'separateControls': 1, 'fixedWitnesses': 3,
        'purpose': 'Integration qualification, not evidence of search superiority or weight tuning.',
        'selection': 'Fixed before new measurements; the three known witnesses are separate from search.',
        'sourceHashes': {p.name: digest(p) for p in source.glob('*.py')}})
    offline(out)
    config = {'manifest': str(OVERLAY / 'package/scenario-catalog-manifest.json'),
              'workload': UPDATE, 'runtime': freeze_runtime(), 'recoveryCap': 20, 'timeout': 180}
    control(config, out / 'control')
    rt, domain = prepare(config, out / 'witnesses')
    selected = [s for s in read(OVERLAY / 'selection.json') if s['position'] == 5 and not s['control']]
    cs = domain.resolve(selected[0]['vector'])
    by_id = {alias: c for c in cs for alias in c['aliases']}
    witnesses = []
    for n, row in enumerate(selected, 1):
        r = rt.evaluate(out / 'witnesses', by_id[row['scenario']], n, 180)
        witnesses.append({'label': row['label'], 'I': r['I'], 'A': r['A'],
            'directory': r['directory'], 'scores': {name: assess(r, p) for name, p in POLICIES.items()}})
    domain.verify(); rt.verify(); verify_source(config, out / 'witnesses')
    save(out / 'witnesses/results.json', witnesses)
    for name, strategy, policy in [('ga-persistent', 'ga', 'persistent'),
                                   ('ga-combined', 'ga', 'combined'),
                                   ('random-combined', 'random', 'combined')]:
        execute({**config, 'strategy': strategy, 'seed': 29, 'budget': 8,
                 'population': 4, 'fitness': POLICIES[policy]}, out / name, out / 'control/control.json')
    for name, expected in read(out / 'protocol.json')['sourceHashes'].items():
        if digest(HERE / name) != expected:
            raise ValueError('Search source changed during qualification')
    save(out / 'completion.json', {'status': 'COMPLETE'})


if __name__ == '__main__':
    main()
