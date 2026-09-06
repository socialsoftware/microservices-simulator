#!/usr/bin/env python3
"""Recheck retained artifacts and summarize only instrumented timing boundaries."""
import argparse
from collections import Counter
import math
from pathlib import Path
import re
import statistics

from run import digest, read, reports, save, semantic


def distribution(values):
    if not values:
        return {'count': 0}
    values = sorted(values)
    return {'count': len(values), 'min': values[0], 'median': statistics.median(values),
            'mean': statistics.mean(values), 'p95NearestRank': values[math.ceil(.95 * len(values)) - 1],
            'max': values[-1]}


def summarize(directory):
    results = read(directory / 'results.json')
    plan = read(directory / 'plan.json')
    selection = read(directory / 'selection.json')
    if digest(directory / 'plan.json') != results['planSha256'] or digest(directory / 'selection.json') != plan['selectionSha256']:
        raise ValueError('Changed plan/selection')
    rows = {r['caseId']: r for r in selection['rows']}
    timing_rows = []
    for attempt in results['attempts']:
        path = Path(attempt['directory'])
        if read(path / 'attempt.json') != attempt:
            raise ValueError('Attempt record mismatch: ' + str(path))
        for name, expected in attempt['reportHashes'].items():
            if digest(path / name) != expected:
                raise ValueError('Changed report: ' + str(path / name))
        if attempt['status'] in ('PASS', 'SEMANTIC_DIVERGENCE'):
            execution, v1, v2 = reports(path, rows[attempt['caseId']], Path(selection['manifest']))
            if semantic(execution, v1, v2) != attempt['observed']:
                raise ValueError('Semantic record mismatch')
            if attempt['status'] == 'PASS' and attempt['observed'] != rows[attempt['caseId']]['expected']:
                raise ValueError('False semantic PASS')
        matches = re.findall(r'Started ScenarioExecutorCli in ([0-9.]+) seconds', (path / 'docker.log').read_text())
        timing_rows.append({'caseId': attempt['caseId'], 'repetition': attempt['repetition'],
            'concurrency': attempt['concurrency'], 'status': attempt['status'],
            'wallSeconds': attempt.get('wallSeconds'),
            'springStartupSeconds': float(matches[0]) if len(matches) == 1 else None,
            'sourceSetupSeconds': attempt.get('sourceSetupSeconds'),
            'prerequisiteSetupSeconds': attempt.get('prerequisiteSetupSeconds')})
    ids = [r['executionAttemptId'] for r in results['attempts'] if 'executionAttemptId' in r]
    if len(ids) != len(set(ids)):
        raise ValueError('Duplicate attempt identity')
    groups = []
    for group in results['groups']:
        selected = [r for r in timing_rows if r['concurrency'] == group['concurrency']]
        groups.append({**group, 'timings': {key: distribution([r[key] for r in selected if r[key] is not None])
                       for key in ['wallSeconds', 'springStartupSeconds', 'sourceSetupSeconds', 'prerequisiteSetupSeconds']}})
    summary = {'validation': results['validation'], 'artifactRecheck': 'PASS',
        'plannedAttempts': plan['plannedAttempts'], 'retainedAttempts': len(results['attempts']),
        'statuses': dict(Counter(r['status'] for r in results['attempts'])),
        'uniqueExecutionAttemptIds': len(set(ids)), 'preparationSeconds': plan['preparationSeconds'],
        'totalWallSeconds': results['totalWallSeconds'], 'groups': groups, 'timingRows': timing_rows,
        'timingBoundary': 'Wall: Docker launch through process exit. Spring startup: existing log timer. '
            'Setup: existing report nanosecond timer. Remaining wall time mixes Docker/JVM overhead, '
            'package loading, target actions, assessment, report I/O and shutdown; it is not pure execution time. '
            'Compilation/generation reused and not measured here.'}
    save(directory / 'measurements.json', summary)
    return summary


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', type=Path, required=True)
    summary = summarize(parser.parse_args().run.resolve())
    print(summary['validation'], summary['retainedAttempts'], 'retained attempts')
    for group in summary['groups']:
        print(f'c{group["concurrency"]}: {group["wallSeconds"]:.2f}s, {group["validCompletePerHour"]:.1f} valid/hour')
