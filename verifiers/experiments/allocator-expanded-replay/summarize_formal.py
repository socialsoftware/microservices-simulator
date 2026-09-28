"""Summarize audited formal traces by objective, policy, seed, and cohort."""
import argparse
from collections import defaultdict
import gzip
import json
from pathlib import Path
from statistics import mean, stdev
import sys


ROOT = Path(__file__).resolve().parents[3]
INVENTORY = ROOT / 'docs/verifiers-impl/evidence/workload-master-inventory-2026-09-25/master-inventory-2026-09-28.json'
sys.path.insert(0, str(ROOT / 'verifiers/experiments/fixed-workload-ga'))
from fitness import CRITERIA_V2, assess, configuration


def aggregate(values):
    return {'mean': mean(values), 'sd': stdev(values) if len(values) > 1 else 0,
            'min': min(values), 'max': max(values)}


def summarize(directories):
    inventory = json.loads(INVENTORY.read_text())
    metadata = {r['id']: r for r in inventory['qualifiedWorkloads']}
    giants = {r['id'] for r in sorted(metadata.values(),
        key=lambda r: (-r['scenarios'], r['id']))[:3]}
    result = {'inventorySha256': None, 'cohorts': {}}
    import hashlib
    result['inventorySha256'] = hashlib.sha256(INVENTORY.read_bytes()).hexdigest()
    for directory in directories:
        source = json.loads((directory / 'formal-results.json').read_text())
        protocol = source['protocol']
        if len(source['rows']) != len(protocol['profiles']) * len(protocol['arms']) * len(protocol['seeds']):
            raise ValueError('Formal matrix incomplete')
        input_spec = json.loads((directory / 'inputs.json').read_text())
        cohort = input_spec.get('cohort', directory.name)
        admitted = {r['id'] for r in input_spec['admission']
                    if r['admission'] == 'RECORDED_BENCHMARK_ADMITTED'}
        populations = {}
        for profile in protocol['profiles']:
            fit = configuration({'policy': 'weighted-criteria-v2', 'weights': {
                c: int(profile == 'all-five' or profile == c) for c in CRITERIA_V2}})
            population = {'scenarios': 0, 'positive': 0, 'negative': 0,
                          'unavailable': 0, 'score': 0, 'positiveWorkloads': set(),
                          'positiveFamilies': set()}
            for wid in admitted:
                reference = json.loads((ROOT / metadata[wid]['reference']).read_text())
                for observation in reference['observations'].values():
                    score = assess(observation, fit)['fitnessScore']
                    population['scenarios'] += 1
                    if score is None:
                        population['unavailable'] += 1
                    elif score > 0:
                        population['positive'] += 1
                        population['score'] += score
                        population['positiveWorkloads'].add(wid)
                        population['positiveFamilies'].add(metadata[wid]['family'])
                    else:
                        population['negative'] += 1
            population['positiveWorkloads'] = len(population['positiveWorkloads'])
            population['positiveFamilies'] = len(population['positiveFamilies'])
            populations[profile] = population
        group = defaultdict(list)
        seed_rows = {}
        for row in source['rows']:
            trace_path = Path(row['path']).resolve()
            with gzip.open(trace_path, 'rt') as stream:
                trace = json.load(stream)
            if (trace['profile'], trace['arm'], trace['seed']) != (row['profile'], row['arm'], row['seed']):
                raise ValueError('Trace identity mismatch')
            by_workload = trace['perWorkload']
            selected = [w for w in by_workload if w['allocations']]
            positive = [w for w in by_workload if w['positiveDiscoveries']]
            first = next((d['decision'] for d in trace['decisions'] if d['score'] is not None and d['score'] > 0), None)
            derived = {**row, 'path': str(trace_path.relative_to(ROOT)),
                'traceSha256': hashlib.sha256(trace_path.read_bytes()).hexdigest(),
                'negativeChoices': trace['attempts'] - trace['positives'] - trace['unknowns'],
                'selectedWorkloads': len(selected), 'selectedFamilies': len({metadata[w['workload']]['family'] for w in selected}),
                'positiveWorkloads': len(positive), 'positiveFamilies': len({metadata[w['workload']]['family'] for w in positive}),
                'firstPositiveDecision': first,
                'giantChoices': sum(w['allocations'] for w in by_workload if w['workload'] in giants),
                'giantScore': sum(w['cumulativeScore'] for w in by_workload if w['workload'] in giants),
                'checkpoints': trace['checkpoints']}
            group[(row['profile'], row['arm'])].append(derived)
            seed_rows[(row['profile'], row['arm'], row['seed'])] = derived
        summaries = []
        for profile in protocol['profiles']:
            for arm in protocol['arms']:
                rows = sorted(group[(profile, arm)], key=lambda r: r['seed'])
                summaries.append({'profile': profile, 'arm': arm, 'seeds': [r['seed'] for r in rows],
                    'score': aggregate([r['score'] for r in rows]),
                    'positives': aggregate([r['positives'] for r in rows]),
                    'unknowns': aggregate([r['unknowns'] for r in rows]),
                    'negativeChoices': aggregate([r['negativeChoices'] for r in rows]),
                    'positiveFamilies': aggregate([r['positiveFamilies'] for r in rows]),
                    'positiveWorkloads': aggregate([r['positiveWorkloads'] for r in rows]),
                    'giantChoices': aggregate([r['giantChoices'] for r in rows]),
                    'giantScore': aggregate([r['giantScore'] for r in rows]),
                    'firstPositiveDecisions': [r['firstPositiveDecision'] for r in rows],
                    'checkpoints': {str(n): {'score': aggregate([next(c['cumulativeScore'] for c in r['checkpoints'] if c['attempt'] == n) for r in rows]),
                        'positives': aggregate([next(c['positives'] for c in r['checkpoints'] if c['attempt'] == n) for r in rows])}
                        for n in (1000, 2000, 4000, 8000) if n <= protocol['budget']}})
        deltas = []
        for profile in protocol['profiles']:
            for arm in protocol['arms']:
                if arm == 'uniform':
                    continue
                deltas.append({'profile': profile, 'arm': arm, 'vs': 'uniform',
                    'score': aggregate([seed_rows[(profile, arm, seed)]['score'] -
                                        seed_rows[(profile, 'uniform', seed)]['score']
                                        for seed in protocol['seeds']]),
                    'positives': aggregate([seed_rows[(profile, arm, seed)]['positives'] -
                                            seed_rows[(profile, 'uniform', seed)]['positives']
                                            for seed in protocol['seeds']])})
        result['cohorts'][cohort] = {'protocol': protocol, 'checks': source['checks'],
            'population': populations,
            'summaries': summaries, 'pairedDifferences': deltas,
            'seedRows': sorted(seed_rows.values(), key=lambda r: (r['profile'], r['arm'], r['seed']))}
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--inputs', type=Path, nargs='+', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    value = summarize(args.inputs)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(value, indent=2) + '\n')
