"""Audit completed matched runs and summarise revealed outcomes only."""
import argparse
import collections
import gzip
import json
from pathlib import Path

from run import ARMS, CRITERIA_V2, configuration, read, sha
from fitness import assess


def summarize(directory, budget, seed):
    frozen = read(directory / 'inputs.json')
    metadata = {r['id']: r for r in frozen['admission']}
    references = {r['workload']: read(r['reference'])['observations']
                  for part in frozen['partitions'] for r in part['workloads']}
    fit = configuration({'policy': 'weighted-criteria-v2', 'weights': {c: 1 for c in CRITERIA_V2}})
    paths = [directory / f'all-five-{arm}-seed{seed}-budget{budget}.json.gz' for arm in ARMS]
    results, starts, prefixes = {}, set(), {}
    sources = None
    for path in paths:
        if not path.exists():
            continue
        with gzip.open(path, 'rt') as stream:
            run = json.load(stream)
        if (run['inputSha256'] != sha(directory / 'inputs.json') or run['fitness'] != fit
                or run['seed'] != seed or run['budget'] != budget):
            raise ValueError('Run configuration differs from the matched comparison')
        if sources is not None and sources != run['runnerSourceHashes']:
            raise ValueError('Model/GA source hashes differ across compared arms')
        sources = run['runnerSourceHashes']
        starts.add(run['start']['stateSha256'])
        if not run['start']['allProgressZero'] or run['attempts'] != budget:
            raise ValueError('Run is not a complete cold comparison')
        allocation = collections.Counter()
        versions = collections.Counter()
        criteria = {c: collections.Counter() for c in CRITERIA_V2}
        sequences = collections.defaultdict(list)
        total = 0.0
        positives = unknown = 0
        seen = set()
        failure = collections.Counter()
        for decision in run['decisions']:
            wid, key = decision['workload'], decision['candidate']
            if (wid, key) in seen:
                raise ValueError('Repeated candidate in run')
            seen.add((wid, key))
            sequences[wid].append(key)
            observation = references[wid][key]
            assessment = assess(observation, fit)
            if assessment['fitnessScore'] != decision['score']:
                raise ValueError('Revealed score differs from frozen observation')
            allocation[wid] += 1
            versions[metadata[wid]['runtime']['signature']] += 1
            score = decision['score']
            total += score or 0.0
            positives += score is not None and score > 0
            unknown += score is None
            if score is None:
                failure[observation['status']] += 1
            for criterion, component in assessment['fitnessComponents'].items():
                count = component['count']
                criteria[criterion]['unavailable' if count is None else 'positive' if count > 0 else 'zero'] += 1
        if (positives, unknown, total) != (run['positives'], run['unknowns'], run['cumulativeScore']):
            raise ValueError('Run aggregates differ from decision trace')
        for wid, keys in sequences.items():
            for previous in prefixes.get(wid, []):
                common = min(len(previous), len(keys))
                if previous[:common] != keys[:common]:
                    raise ValueError('Local GA prefixes differ across matched allocation arms')
            prefixes.setdefault(wid, []).append(keys)
        results[run['arm']] = {
            'attempts': run['attempts'], 'positiveScenarios': positives,
            'negativeScenarios': run['attempts'] - positives - unknown,
            'unavailableJointScore': unknown, 'cumulativeScore': total,
            'visitedWorkloads': len(allocation), 'criteria': {c: dict(v) for c, v in criteria.items()},
            'unknownAttemptStatuses': dict(failure), 'allocationsByRecordedRuntime': dict(versions),
            'topWorkloads': [{'workload': wid, 'family': metadata[wid]['sagaFamily'], 'allocations': n}
                             for wid, n in allocation.most_common(5)],
            'checkpoints': run['checkpoints'], 'traceSha256': run['decisionSha256'],
            'wallSeconds': run['wallSeconds']}
    if len(starts) > 1:
        raise ValueError('Cold local starting states differ across arms')
    return {'status': 'COMPLETE' if len(results) == len(ARMS) else 'IN_PROGRESS',
            'purpose': 'One-seed recorded comparison on historical measurement versions',
            'budget': budget, 'seed': seed, 'workloads': len(references),
            'recordedCandidates': sum(len(v) for v in references.values()),
            'matchedColdStarts': bool(results), 'matchedLocalGaPrefixes': bool(results),
            'results': results}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--directory', type=Path, required=True)
    parser.add_argument('--budget', type=int, default=8000)
    parser.add_argument('--seed', type=int, default=1)
    args = parser.parse_args()
    result = summarize(args.directory, args.budget, args.seed)
    (args.directory / f'summary-seed{args.seed}-budget{args.budget}.json').write_text(
        json.dumps(result, indent=2) + '\n')
    print(result['status'], len(result['results']), 'arms')
    for arm, row in result['results'].items():
        print(arm, row['positiveScenarios'], row['negativeScenarios'],
              row['unavailableJointScore'], row['cumulativeScore'])


if __name__ == '__main__':
    main()
