"""Retrospective allocation descriptions, never inputs to a search policy."""
import collections
import gzip
import json
import statistics
from pathlib import Path
from independent_review import OUT, read, trace_path


def main():
    p = read(OUT / 'protocol.json')
    inventory = read(OUT / 'pre-search-component-inventory.json')
    description = read(OUT / 'collection-description.json')
    result = {'use': 'Retrospective description; positive-workload labels are never passed to policies.', 'profiles': {}}
    for profile in p['profiles']:
        result['profiles'][profile] = {}
        for collection in p['collections']:
            data = inventory['profiles'][profile]['collections'][collection]
            relevant = {w['workload'] for w in data['workloads'] if w['positive'] > 0}
            sizes = {w['workload']: w['positive'] + w['zero'] + w['unknown'] for w in data['workloads']}
            family = {wid: row['sagas'] for row in description[collection]['families'] for wid in row['ids']}
            arms = {}
            for arm in p['arms']:
                rows = [json.load(gzip.open(trace_path(profile, collection, arm, seed, p), 'rt')) for seed in p['seeds']]
                checkpoints = []
                for budget in p['checkpoints']:
                    observations = []
                    for row in rows:
                        decisions = row['decisions'][:budget]
                        counts = collections.Counter(d['workload'] for d in decisions)
                        observations.append({
                            'workloadsVisited': len(counts),
                            'positiveWorkloadsVisited': len(set(counts) & relevant),
                            'choicesInPositiveWorkloads': sum(v for w, v in counts.items() if w in relevant),
                            'workloadsExhausted': sum(v == sizes[w] for w, v in counts.items()),
                            'discoveredAnyPositive': any(d['score'] is not None and d['score'] > 0 for d in decisions),
                            'positives': decisions[-1]['positives'],
                            'cumulativeScore': decisions[-1]['cumulativeScore'],
                        })
                    checkpoints.append({'budget': budget, **{key: statistics.mean(r[key] for r in observations) for key in observations[0]}})
                family_counts = {f: [] for f in set(family.values())}
                for row in rows:
                    counts = collections.Counter(family[d['workload']] for d in row['decisions'])
                    for f, values in family_counts.items(): values.append(counts[f])
                arms[arm] = {'checkpoints': checkpoints,
                             'meanSelectionsBySagaSetAt256': {f: statistics.mean(v) for f, v in sorted(family_counts.items())}}
            result['profiles'][profile][collection] = {'positiveWorkloads': len(relevant), 'arms': arms}
    (OUT / 'allocation-diagnostics.json').write_text(json.dumps(result, indent=2) + '\n')
    print('Saved diagnostics for', sum(len(v) for v in result['profiles'].values()), 'profile/collection pairs')

if __name__ == '__main__': main()
