"""Audit every trace before aggregating matched seeds and criterion objectives."""
import argparse
import collections
import gzip
import hashlib
import itertools
import json
from pathlib import Path
import statistics

import run
from fitness import assess
from search import stable


def aggregate(directory, profiles=None):
    protocol = run.read(directory / 'batch-protocol.json')
    frozen = run.read(directory / 'inputs.json')
    selected_profiles = profiles or protocol['profiles']
    if not set(selected_profiles) <= set(protocol['profiles']):
        raise ValueError('Unknown objective selection')
    references = {r['workload']: run.read(r['reference'])['observations']
                  for part in frozen['partitions'] for r in part['workloads']}
    metadata = {r['id']: r for r in frozen['admission']}
    aliases = collections.defaultdict(list)
    for partition in frozen['partitions']:
        for row in partition['workloads']:
            aliases[row['structuralProfileSha256']].append(row['workload'])
    results, summaries, inventory = {}, {}, {}
    for profile in selected_profiles:
        fit = run.configuration({'policy': 'weighted-criteria-v2', 'weights': {
            c: int(profile == 'all-five' or profile == c) for c in run.CRITERIA_V2}})
        catalogue_scores = [assess(v, fit)['fitnessScore'] for observations in references.values()
                            for v in observations.values()]
        inventory[profile] = {'candidates': len(catalogue_scores),
            'positiveScenarios': sum(s is not None and s > 0 for s in catalogue_scores),
            'negativeScenarios': catalogue_scores.count(0), 'unavailableScore': catalogue_scores.count(None),
            'totalAvailableScore': sum(s or 0 for s in catalogue_scores)}
        results[profile] = {}
        for seed in protocol['seeds']:
            prefixes, cold_starts = {}, set()
            for arm in protocol['arms']:
                path = directory / f'{profile}-{arm}-seed{seed}-budget{protocol["budget"]}.json.gz'
                with gzip.open(path, 'rt') as stream:
                    trace = json.load(stream)
                if (trace['inputSha256'] != protocol['inputSha256'] or trace['fitness'] != fit
                        or trace['runnerSourceHashes'] != protocol['runnerSourceHashes']
                        or trace['numericKernelReceiptSha256'] != protocol['kernelReceiptSha256']
                        or trace['arm'] != arm or trace['seed'] != seed
                        or trace['attempts'] != protocol['budget'] or not trace['start']['allProgressZero']):
                    raise ValueError('Run differs from matched protocol: ' + str(path))
                if hashlib.sha256(stable(trace['decisions']).encode()).hexdigest() != trace['decisionSha256']:
                    raise ValueError('Decision trace hash mismatch')
                cold_starts.add(trace['start']['stateSha256'])
                seen = set()
                local = collections.defaultdict(list)
                workloads, families = set(), set()
                allocation = collections.Counter()
                contribution = collections.Counter()
                progress = collections.defaultdict(lambda: {'allocated': 0, 'known': 0,
                    'unknown': 0, 'positives': 0, 'scoreSum': 0.0, 'bestScore': None})
                positive = unknown = 0
                score = 0.0
                first_positive = all_known_positives = None
                trajectory = [[0, 0.0, 0, 0]]
                for decision in trace['decisions']:
                    wid, key = decision['workload'], decision['candidate']
                    identity = wid, key
                    if identity in seen:
                        raise ValueError('Repeated measured candidate')
                    seen.add(identity)
                    local[wid].append(key)
                    allocation[wid] += 1
                    actual = assess(references[wid][key], fit)
                    before = progress[wid]
                    if decision['preUpdateProgress'] != before:
                        raise ValueError('Progress includes feedback outside its prior selected prefix')
                    if actual['fitnessScore'] != decision['score']:
                        raise ValueError('Trace feedback differs from frozen measurement')
                    value = decision['score']
                    before['allocated'] += 1
                    if value is None:
                        before['unknown'] += 1
                    else:
                        before['known'] += 1
                        before['scoreSum'] += value
                        before['positives'] += value > 0
                        before['bestScore'] = value if before['bestScore'] is None else max(before['bestScore'], value)
                    if value is None:
                        unknown += 1
                    else:
                        score += value
                        if value > 0:
                            positive += 1
                            if first_positive is None:
                                first_positive = decision['decision']
                            if positive == inventory[profile]['positiveScenarios']:
                                all_known_positives = decision['decision']
                            workloads.add(wid)
                            families.add(metadata[wid]['sagaFamily'])
                        for criterion, component in actual['fitnessComponents'].items():
                            if fit['weights'][criterion] and component['count'] is not None:
                                contribution[criterion] += component['count']
                    if decision['decision'] % 128 == 0 or decision['decision'] == protocol['budget']:
                        trajectory.append([decision['decision'], score, positive, unknown])
                if (positive, unknown, score) != (trace['positives'], trace['unknowns'], trace['cumulativeScore']):
                    raise ValueError('Trace aggregate mismatch')
                if len(seen) != protocol['budget'] or sum(contribution.values()) != score:
                    raise ValueError('Budget or score contribution mismatch')
                for wid, sequence in local.items():
                    previous = prefixes.get(wid, [])
                    common = min(len(previous), len(sequence))
                    if previous[:common] != sequence[:common]:
                        raise ValueError('Local GA candidate prefixes differ')
                    if len(sequence) > len(previous):
                        prefixes[wid] = sequence
                summary = {'seed': seed, 'positiveScenarios': positive,
                    'negativeScenarios': protocol['budget'] - positive - unknown,
                    'unavailableScore': unknown, 'cumulativeScore': score,
                    'positiveWorkloads': len(workloads), 'positiveFamilies': len(families),
                    'visitedWorkloads': len(allocation), 'contributions': dict(contribution),
                    'firstPositiveAt': first_positive,
                    'allKnownPositivesFoundAt': all_known_positives,
                    'trajectory': trajectory,
                    'topWorkloads': [{'workload': wid, 'family': metadata[wid]['sagaFamily'], 'choices': n}
                                     for wid, n in allocation.most_common(3)],
                    'checkpoints': trace['checkpoints'], 'traceSha256': trace['decisionSha256']}
                results[profile].setdefault(arm, []).append(summary)
            if len(cold_starts) != 1:
                raise ValueError('Cold GA states differ within matched seed/objective')
        summaries[profile] = {}
        for arm, rows in results[profile].items():
            metrics = ('positiveScenarios', 'cumulativeScore', 'unavailableScore',
                       'positiveWorkloads', 'positiveFamilies', 'visitedWorkloads')
            summaries[profile][arm] = {metric: {
                'mean': statistics.mean(r[metric] for r in rows),
                'min': min(r[metric] for r in rows), 'max': max(r[metric] for r in rows),
                'sd': statistics.stdev(r[metric] for r in rows) if len(rows) > 1 else 0}
                for metric in metrics}
        baseline = {r['seed']: r for r in results[profile]['uniform']}
        for arm, rows in results[profile].items():
            summaries[profile][arm]['pairedScoreVsUniform'] = {
                'meanDelta': statistics.mean(r['cumulativeScore'] - baseline[r['seed']]['cumulativeScore'] for r in rows),
                'wins': sum(r['cumulativeScore'] > baseline[r['seed']]['cumulativeScore'] for r in rows),
                'ties': sum(r['cumulativeScore'] == baseline[r['seed']]['cumulativeScore'] for r in rows)}
        print('AUDITED', profile, len(protocol['seeds']) * len(protocol['arms']), 'runs', flush=True)
    return {'status': 'COMPLETE' if set(selected_profiles) == set(protocol['profiles']) else 'PROFILE_SUBSET_COMPLETE',
            'profilesAudited': selected_profiles, 'protocol': protocol, 'workloads': len(references),
            'aggregationSourceSha256': run.sha(Path(__file__)),
            'matchedColdStarts': True, 'matchedLocalGaPrefixes': True,
            'noRepeatedMeasuredCandidates': True, 'allFeedbackReassessed': True,
            'allProgressReconstructedFromPriorChoices': True,
            'structuralFeatureAliases': [{'profileSha256': digest, 'workloads': sorted(ids),
                'candidateCount': sum(metadata[wid]['scenarios'] for wid in ids)}
                for digest, ids in sorted(aliases.items()) if len(ids) > 1],
            'catalogue': inventory, 'summary': summaries, 'results': results}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--profiles', nargs='+', choices=run.PROFILES)
    args = parser.parse_args()
    result = aggregate(args.output, args.profiles)
    name = 'aggregate.json' if args.profiles is None else 'aggregate-' + '-'.join(args.profiles) + '.json'
    (args.output / name).write_text(json.dumps(result, indent=2) + '\n')
    for profile, arms in result['summary'].items():
        print(profile, 'mean positives:', {arm: round(r['positiveScenarios']['mean'], 1)
                                          for arm, r in arms.items()}, flush=True)


if __name__ == '__main__':
    main()
