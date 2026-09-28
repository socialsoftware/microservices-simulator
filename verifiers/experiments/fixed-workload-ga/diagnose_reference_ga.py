#!/usr/bin/env python3
"""Replay frozen GA source for proposal diagnostics; no search or app changes."""
import argparse
import ast
from collections import Counter, defaultdict
import hashlib
import json
from pathlib import Path
import sys


def read(path):
    return json.loads(path.read_text())


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def save(path, data):
    path.write_text(json.dumps(data, indent=2, sort_keys=True) + '\n')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--reference', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    source, out = args.reference.resolve(), args.output.resolve()
    protocol = read(source / 'protocol.json')
    hashes = {str(source / name): digest(source / name)
              for name in ['reference.json', 'protocol.json', 'package/workloads.jsonl']}
    for name, expected in protocol['sourceHashes'].items():
        path = source / 'source' / name
        if digest(path) != expected:
            raise ValueError('Frozen source differs: ' + name)
        hashes[str(path)] = expected
    sys.path.insert(0, str(source / 'source'))
    from search import run, stable, fault_coordinates
    # Load only the frozen structural class; avoid importing Docker runtime tooling.
    structural_source = source / 'source/catalogue.py'
    if not structural_source.exists():
        structural_source = source / 'source/exhaustive_reference.py'
    tree = ast.parse(structural_source.read_text())
    node = next(n for n in tree.body if isinstance(n, ast.ClassDef) and n.name == 'RecordedDomain')
    namespace = {'stable': stable, 'fault_coordinates': fault_coordinates}
    exec(compile(ast.Module(body=[node], type_ignores=[]), 'frozen-RecordedDomain', 'exec'), namespace)
    RecordedDomain = namespace['RecordedDomain']
    reference = read(source / 'reference.json')
    workload = next(json.loads(line) for line in (source / 'package/workloads.jsonl').read_text().splitlines()
                    if json.loads(line)['id'] == protocol['config']['workload'])
    domain = RecordedDomain(workload, reference['candidates'])
    out.mkdir(parents=True, exist_ok=False)
    pooled = defaultdict(Counter)
    runs = []
    examples = []
    for seed in protocol['seeds']:
        retained_path = source / f'ga-{seed}.json'
        hashes[str(retained_path)] = digest(retained_path)
        retained = read(retained_path)
        snapshots = []

        def observe(event):
            if event['proposal']['status'] != 'EVALUATED':
                return
            # Read-only instrumentation of the frozen search frame. No RNG calls or writes.
            parents = sys._getframe(1).f_locals['parents']
            snapshots.append({'attempt': event['attempt']['attempt'],
                              'parents': [{'key': p['key'], 'genes': p['genes'], 'score': p['fitnessScore']}
                                          for p in parents]})

        result = run(domain, lambda c, n: reference['observations'][c['key']],
                     strategy='ga', seed=seed, budget=protocol['budget'],
                     population=protocol['population'], mutation=protocol['mutation'],
                     stall_limit=protocol['stallLimit'], fitness=protocol['config']['fitness'], emit=observe)
        for key in ['duplicates', 'stopReason', 'positiveScenarios']:
            if result[key] != retained[key]:
                raise ValueError(f'Replay differs: seed {seed}, {key}')
        if len(result['proposals']) != retained['proposalCount']:
            raise ValueError('Proposal count differs')
        fields = ['key', 'fitnessScore', 'operator', 'parents', 'genes', 'replacedRecovery']
        if [[a[k] for k in fields] for a in result['attempts']] != [
                [a[k] for k in fields] for a in retained['attempts']]:
            raise ValueError('Replay changed retained choices or feedback')
        evaluated = 0
        counts = Counter()
        for proposal in result['proposals']:
            end = next((n for n in [8, 50, 100, 150, 186] if evaluated < n), 186)
            bucket = pooled[str(end)]
            operator = proposal['operator']
            bucket['proposals'] += 1
            bucket[operator + 'Proposals'] += 1
            counts[operator + 'Proposals'] += 1
            if operator == 'crossover':
                ps = proposal['parents']
                bucket['sameParent'] += int(ps[0]['key'] == ps[1]['key'])
                bucket['bothParentsScore2'] += int(all(p['fitnessScore'] == 2 for p in ps))
                bucket['mutationAttempted'] += int(proposal['mutation'] is not None)
                bucket['replacedRecovery'] += int(proposal['replacedRecovery'])
                bucket['childIsParent'] += int(proposal['key'] in {p['key'] for p in ps})
                bucket['childGenesEqual'] += int(any(proposal['genes'] == next(a['genes'] for a in result['attempts']
                    if a['key'] == p['key']) for p in ps))
            bucket[operator + proposal['status']] += 1
            counts[operator + proposal['status']] += 1
            if proposal['status'] == 'EVALUATED':
                a = result['attempts'][evaluated]
                score = a['fitnessScore']
                bucket[operator + 'Positive'] += int(score is not None and score > 0)
                bucket[operator + 'Score2'] += int(score == 2)
                evaluated += 1
            if len(examples) < 3 and seed == 1 and operator == 'crossover' and proposal['status'] == 'DUPLICATE':
                examples.append(proposal)
        save(out / f'ga-{seed}-diagnostics.json', {'seed': seed, 'counts': dict(counts),
             'snapshots': snapshots, 'proposals': result['proposals']})
        runs.append({'seed': seed, 'firstAllScore2Population': next((s['attempt'] for s in snapshots
                     if len(s['parents']) == protocol['population'] and all(p['score'] == 2 for p in s['parents'])), None),
                     'populationDistinctVectorsAt100': len({stable(p['genes']) for p in snapshots[99]['parents']}),
                     'populationUpdateFaultsAt100': sorted({p['genes'][2] for p in snapshots[99]['parents']}, key=str)})
    for path, expected in hashes.items():
        if digest(Path(path)) != expected:
            raise ValueError('Input changed: ' + path)
    summary = {'seeds': protocol['seeds'], 'replaysMatchRetained': True,
               'proposalBucketsByCompletedEvaluations': {k: dict(v) for k, v in pooled.items()},
               'runs': runs, 'examples': examples,
               'interpretation': 'Proposal buckets end at 8, 50, 100, 150, 186 distinct evaluations. '
                   'Duplicates and tail stalls belong to the next not-yet-completed evaluation. '
                   'Population observed read-only through emit; all retained choices verified identical.'}
    save(out / 'summary.json', summary)
    save(out / 'validation.json', {'sourceHashesUnchanged': True, 'replaysMatchRetained': True,
         'seedCount': len(runs), 'scriptSha256': digest(Path(__file__)), 'inputHashes': hashes})
    print(json.dumps({k:v for k,v in summary.items() if k not in ['runs','examples']}, indent=2))
    print('All-score-2 population first reached:', [r['firstAllScore2Population'] for r in runs])


if __name__ == '__main__':
    main()
