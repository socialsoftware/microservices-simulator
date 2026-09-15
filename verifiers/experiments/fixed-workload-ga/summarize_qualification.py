#!/usr/bin/env python3
"""Audit the declared campaign and retain a compact evidence index."""
import argparse
from collections import Counter
from pathlib import Path

from runtime import Runtime, batch, digest, package, read, save, validate_read
from search import candidate_key
from run import verify_source


def audit(root):
    ids, names, rows, arms, by_key = set(), set(), [], [], {}
    for directory in sorted(root.glob('*/attempt-*/attempt.json')):
        a = read(directory)
        d = directory.parent
        for filename, expected in a['reportHashes'].items():
            if digest(d / filename) != expected:
                raise ValueError('Report drift: ' + str(d / filename))
        manifest = d / 'package/scenario-catalog-manifest.json'
        if package(manifest)['hashes'] != a['packageHashes']:
            raise ValueError('Replay package drift')
        execution = read(d / 'execution-report.json')
        v1 = read(d / 'execution-report.impact.json')
        v2 = read(d / 'execution-report.impact-v2.json')
        c = a['candidate']
        batch.validate_reports(execution, v1, v2, c['workload'], c['id'], c['faultVector'])
        exposure = validate_read(read(d / 'execution-report.saga-read-exposure.json'), execution, d, manifest)
        if batch.assessment_result(execution, v2) != (a['status'], a['I']) or exposure['A'] != a['A']:
            raise ValueError('Attempt summary differs from reports')
        if a['executionAttemptId'] in ids or a['containerName'] in names:
            raise ValueError('Reused attempt/container')
        ids.add(a['executionAttemptId']); names.add(a['containerName'])
        key = candidate_key(c['workload'], c['faultVector'], c['actions'])
        outcome = (a['I'], a['A'], a['status'], a['ACoverage'])
        if key in by_key and by_key[key] != outcome:
            raise ValueError('Repeated candidate measurements differ')
        by_key[key] = outcome
        rows.append({'arm': d.parent.name, 'attempt': d.name, 'scenario': c['id'],
            'key': key, 'vector': c['faultVector'], 'I': a['I'], 'A': a['A'],
            'status': a['status'], 'ACoverage': a['ACoverage'],
            'terminalStatus': a['terminalStatus'], 'conformance': a['scheduleConformance'],
            'executionAttemptId': a['executionAttemptId'],
            'packageManifestSha256': a['packageHashes']['manifest'], 'reportHashes': a['reportHashes']})
    for arm in ('ga-11', 'random-11', 'ga-29', 'random-29'):
        out = root / arm
        result = read(out / 'results.json')
        if len(result['attempts']) != 12 or result['stopReason'] != 'BUDGET':
            raise ValueError('Qualification arm did not use its declared budget')
        attempts = {a['key']: a for a in result['attempts']}
        if len(attempts) != 12:
            raise ValueError('Duplicate consumed an execution slot')
        prior = {}
        for proposal in result['proposals']:
            for parent in proposal['parents']:
                if parent['key'] not in prior or prior[parent['key']] != parent['I'] or parent['I'] is None:
                    raise ValueError('Parent did not have earlier available fitness')
            if proposal['status'] == 'EVALUATED':
                prior[proposal['key']] = attempts[proposal['key']]['I']
        requests = read(out / 'requests.json')
        for left, right in zip(requests, requests[1:]):
            if left['after'] != right['before']:
                raise ValueError('Request revision chain broken')
        if requests[-1]['after'] != package(out / 'package/scenario-catalog-manifest.json')['hashes']:
            raise ValueError('Final arm package differs')
        verify_source(read(out / 'config.json'), out)
        arms.append({k: result[k] for k in ('strategy', 'seed', 'budget', 'bestI', 'positiveScenarios',
            'firstPositive', 'nullFitnessAttempts', 'duplicates', 'generationRequests', 'generationSeconds',
            'stopReason', 'wallSeconds')} | {'crossovers': sum(p['operator'] == 'crossover' for p in result['proposals']),
            'truncatedRequests': sum(r['truncated'] for r in requests),
            'bestCurve': [a['bestSoFar'] for a in result['attempts']],
            'initialEightPositives': sum(a['I'] > 0 for a in result['attempts'][:8]),
            'evaluatedCrossoverChildren': sum(a['operator'] == 'crossover' for a in result['attempts']),
            'positiveCrossoverChildren': sum(a['operator'] == 'crossover' and a['I'] > 0 for a in result['attempts'])})
    if len(rows) != 53 or Counter(r['A'] for r in rows if r['arm'] == 'update-probes') != Counter([1, 0, 0]):
        raise ValueError('Declared control/probe count or read witnesses differ')
    Runtime(read(root / 'benchmark-control/config.json')['runtime']).verify()
    for name in ('benchmark-control', 'update-control', 'update-probes'):
        verify_source(read(root / name / 'config.json'), root / name)
    return {'validation': 'PASS', 'realApplicationExecutions': len(rows),
        'searchExecutions': sum(len(read(root / (a['strategy'] + '-' + str(a['seed'])) / 'results.json')['attempts']) for a in arms),
        'assessmentStatuses': dict(Counter(r['status'] for r in rows)),
        'readCoverage': dict(Counter(r['ACoverage'] for r in rows)),
        'scheduleConformance': dict(Counter(r['conformance'] for r in rows)),
        'distinctCandidateKeysIncludingControlsAndProbes': len(by_key),
        'stableMeasurementsForRepeatedCandidates': True, 'arms': arms, 'attempts': rows,
        'sourceHashes': {p.name: digest(p) for p in Path(__file__).parent.glob('*.py')},
        'protocolSha256': digest(root / 'protocol.json')}


if __name__ == '__main__':
    p = argparse.ArgumentParser(); p.add_argument('--run', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True); args = p.parse_args()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    result = audit(args.run.resolve()); save(args.output, result)
    print({k: result[k] for k in ('validation', 'realApplicationExecutions', 'assessmentStatuses', 'readCoverage')})
