#!/usr/bin/env python3
"""Post-run positive-discovery curves. The reference map never feeds the search policy."""
import argparse
from collections import Counter
import math
from pathlib import Path
import statistics

from runtime import Runtime, batch, digest, package, read, save, validate_read
from run import verify_source
from search import candidate_key


def discovery_metrics(result, reference, budget):
    positives = {k for k, v in reference.items() if v['I'] is not None and v['I'] > 0}
    seen, found, curve, first = set(), set(), [0], None
    offspring = fallback = 0
    attempts = result['attempts']
    for n, attempt in enumerate(attempts, 1):
        key = attempt['key']
        if key not in reference or key in seen:
            raise ValueError('Attempt outside reference or duplicate execution')
        seen.add(key)
        # A failed new measurement cannot discover a known positive through the reference map.
        if attempt['I'] is not None and attempt['I'] != reference[key]['I']:
            raise ValueError('Measured I differs from the reference map')
        if attempt['I'] is not None and attempt['I'] > 0:
            found.add(key)
            if first is None:
                first = n
        curve.append(len(found))
        offspring += attempt['operator'] == 'crossover'
        fallback += n > result['population'] and attempt['operator'] == 'random'
    if len(attempts) > budget:
        raise ValueError('Execution budget exceeded')
    targets = {}
    for percent in (50, 80, 100):
        count = math.ceil(len(positives) * percent / 100)
        reached = next((n for n, observed in enumerate(curve) if count and observed >= count), None)
        targets[str(percent)] = {'requiredPositiveCount': count, 'execution': reached,
            'status': 'NO_REFERENCE_POSITIVES' if not positives else 'REACHED' if reached is not None else 'NOT_REACHED',
            'observedThroughExecution': len(attempts)}
    # Carry the last observation for display if the policy stalled early; retain its exact stop separately.
    padded = curve + [curve[-1]] * (budget + 1 - len(curve))
    return {'strategy': result['strategy'], 'seed': result['seed'], 'executions': len(attempts),
        'stopReason': result['stopReason'], 'referencePositiveCount': len(positives),
        'positiveCount': len(found), 'firstPositiveExecution': first, 'targets': targets,
        'curve': curve, 'displayCurveThroughBudget': padded,
        'meanPositiveFractionAcrossBudget': sum(padded[1:]) / (budget * len(positives)) if positives else None,
        'newCrossoverEvaluations': offspring, 'postInitializationRandomEvaluations': fallback,
        'duplicates': result['duplicates'], 'nullFitnessAttempts': result['nullFitnessAttempts'],
        'wallSeconds': result['wallSeconds'], 'bestI': result['bestI']}


def audit_attempt(path):
    attempt = read(path)
    directory = path.parent
    for name, expected in attempt['reportHashes'].items():
        if digest(directory / name) != expected:
            raise ValueError('Report hash changed: ' + str(directory / name))
    manifest = directory / 'package/scenario-catalog-manifest.json'
    if package(manifest)['hashes'] != attempt['packageHashes']:
        raise ValueError('Replay snapshot changed')
    e = read(directory / 'execution-report.json')
    v1 = read(directory / 'execution-report.impact.json')
    v2 = read(directory / 'execution-report.impact-v2.json')
    c = attempt['candidate']
    batch.validate_reports(e, v1, v2, c['workload'], c['id'], c['faultVector'])
    a = validate_read(read(directory / 'execution-report.saga-read-exposure.json'), e, directory, manifest)
    if (attempt['status'], attempt['I']) != batch.assessment_result(e, v2) or attempt['A'] != a['A']:
        raise ValueError('Attempt summary does not match measurement')
    return attempt


def audit(root):
    protocol = read(root / 'protocol.json')
    if read(root / 'completion.json')['status'] != 'COMPLETE':
        raise ValueError('Campaign not complete')
    reference = read(root / 'reference-map.json')
    observations = reference['observations']
    if len(observations) != 29 or set(observations) != set(reference['candidates']):
        raise ValueError('Reference map incomplete')
    for key, row in observations.items():
        path = Path(row['attemptPath'])
        if digest(path) != row['attemptSha256']:
            raise ValueError('Reference attempt changed')
        attempt = audit_attempt(path)
        c = attempt['candidate']
        if key != candidate_key(c['workload'], c['faultVector'], c['actions']) or attempt['I'] != row['I']:
            raise ValueError('Reference candidate/score mismatch')
    rows, attempts, ids, containers = [], [], set(), set()
    for seed in protocol['seeds']:
        for strategy in protocol['strategies']:
            arm = root / f'{strategy}-{seed}'
            result = read(arm / 'results.json')
            for r in result['attempts']:
                a = audit_attempt(Path(r['directory']) / 'attempt.json')
                if a['executionAttemptId'] in ids or a['containerName'] in containers:
                    raise ValueError('Application attempt/container reused across arms')
                ids.add(a['executionAttemptId']); containers.add(a['containerName'])
                if (r['I'], r['A'], r['status']) != (a['I'], a['A'], a['status']):
                    raise ValueError('Search feedback differs from measured reports')
                attempts.append({'arm': arm.name, 'key': r['key'], 'I': a['I'], 'A': a['A'],
                    'status': a['status'], 'ACoverage': a['ACoverage'],
                    'executionAttemptId': a['executionAttemptId'], 'reportHashes': a['reportHashes']})
            # Verify selected parents used only earlier observations from this arm.
            completed = {}
            indexed = {a['key']: a for a in result['attempts']}
            for p in result['proposals']:
                for parent in p['parents']:
                    if parent['key'] not in completed or completed[parent['key']] != parent['I'] or parent['I'] is None:
                        raise ValueError('Invalid parent feedback chronology')
                if p['status'] == 'EVALUATED':
                    completed[p['key']] = indexed[p['key']]['I']
            requests = read(arm / 'requests.json')
            for left, right in zip(requests, requests[1:]):
                if left['after'] != right['before']:
                    raise ValueError('Package revision chain changed')
            if requests[-1]['after'] != package(arm / 'package/scenario-catalog-manifest.json')['hashes']:
                raise ValueError('Final package changed')
            verify_source(read(arm / 'config.json'), arm)
            rows.append(discovery_metrics(result, observations, protocol['budgetPerArm']))
    Runtime(protocol['config']['runtime']).verify()
    for name, expected in protocol['sourceHashes'].items():
        if digest(root / 'source' / name) != expected or digest(Path(__file__).parent / name) != expected:
            raise ValueError('Measured sources changed')
    if set(a['executionAttemptId'] for a in attempts) & {
            read(Path(row['attemptPath']))['executionAttemptId'] for row in observations.values()}:
        raise ValueError('Reference execution reused as search feedback')
    return {'validation': 'PASS', 'referenceCandidateCount': len(observations),
        'referencePositiveCount': reference['positiveCount'], 'newReferenceExecutions': 8,
        'newSearchExecutions': len(attempts), 'maxSearchBudgetPerArm': protocol['budgetPerArm'],
        'assessmentStatuses': dict(Counter(a['status'] for a in attempts)),
        'readCoverage': dict(Counter(a['ACoverage'] for a in attempts)),
        'referenceScoresReusedBySearch': False, 'arms': rows, 'attempts': attempts,
        'protocolSha256': digest(root / 'protocol.json'),
        'referenceMapSha256': digest(root / 'reference-map.json'),
        'analysisSourceSha256': digest(Path(__file__))}


def plot(summary, output):
    import matplotlib
    matplotlib.use('Agg')
    import matplotlib.pyplot as plt
    seeds = sorted({a['seed'] for a in summary['arms']})
    fig, axes = plt.subplots(1, len(seeds), figsize=(11, 3.6), sharey=True, sharex=True)
    for axis, seed in zip(axes, seeds):
        for strategy, color in [('ga', '#1769aa'), ('random', '#c55a11')]:
            arm = next(a for a in summary['arms'] if a['seed'] == seed and a['strategy'] == strategy)
            curve = arm['curve']
            axis.step(range(len(curve)), curve, where='post', color=color,
                      label='GA' if strategy == 'ga' else 'Aleatório', linewidth=1.8)
            if arm['executions'] < summary['maxSearchBudgetPerArm']:
                axis.plot([arm['executions']], [curve[-1]], 'x', color=color)
        axis.axvline(8, color='#999999', linestyle=':', linewidth=1)
        axis.set_title(f'Seed {seed}')
        axis.set_xlabel('Execuções da aplicação')
        axis.set_xlim(0, summary['maxSearchBudgetPerArm'])
        axis.set_xticks([0, 5, 10, 15, 20, 25, 29])
        axis.set_yticks([0, 3, 6, 9, 12, 15])
        axis.set_ylim(0, summary['referencePositiveCount'] + .5)
        axis.grid(alpha=.2)
    axes[0].set_ylabel('Cenários positivos distintos encontrados')
    axes[-1].legend(loc='lower right')
    fig.suptitle('Descoberta de positivos com I atual — mesmo workload e orçamento')
    fig.tight_layout()
    fig.savefig(output / 'discovery-curves.png', dpi=180)
    fig.savefig(output / 'discovery-curves.svg')
    fig.savefig(output / 'discovery-curves.pdf')
    plt.close(fig)


if __name__ == '__main__':
    p = argparse.ArgumentParser(); p.add_argument('--run', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True); p.add_argument('--plot', action='store_true')
    a = p.parse_args(); a.output.mkdir(parents=True, exist_ok=True)
    result = audit(a.run.resolve()); save(a.output / 'summary.json', result)
    if a.plot:
        plot(result, a.output)
    print({k: result[k] for k in ('validation', 'referencePositiveCount', 'newSearchExecutions')})
