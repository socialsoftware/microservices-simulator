#!/usr/bin/env python3
"""Post-run benchmark cross-check only; never selects or executes candidates."""
import argparse
from collections import Counter
from pathlib import Path

from search import batch
from validate_assessment import validate_reports


def compare(run, historical):
    if not batch.read(run / 'completion.json')['completed']:
        raise ValueError('Reference comparison is only allowed after the online campaign')
    experiment = batch.read(run / 'experiment.json')
    scope = next(s for s in experiment['scopes'] if s['name'] == 'benchmark')
    reference = {}
    for row in scope['candidateRows']:
        directory = historical / row['faultScenarioId']
        execution, v1, v2 = [batch.read(directory / name) for name in batch.REPORTS]
        validate_reports(execution, v1, v2, row['workloadId'], row['faultScenarioId'], row['faultVector'])
        if v2['assessmentStatus'] != 'COMPLETE':
            raise ValueError('Historical reference must be the corrected COMPLETE landscape')
        reference[row['faultScenarioId']] = {'semantic': batch.semantic(execution, v1, v2),
            'reports': {str(directory / name): batch.digest(directory / name) for name in batch.REPORTS}}
    checks = []
    for strategy in scope['runs']:
        arm = batch.read(run / scope['name'] / strategy['id'] / 'results.json')
        for attempt in arm['attempts']:
            expected = reference[attempt['caseId']]['semantic']
            checks.append({'strategy': strategy['id'], 'scenarioId': attempt['caseId'],
                'executionAttemptId': attempt.get('executionAttemptId'),
                'matchesHistoricalSemanticResult': attempt.get('observed') == expected,
                'onlineStatus': attempt['status'], 'onlineScore': attempt['score'],
                'historicalScore': expected['completeScore']})
    result = {'kind': 'POST_RUN_HISTORICAL_CROSSCHECK', 'newExecutions': 0,
        'scope': 'Known corrected benchmark only. No candidate selection/order or online count comes from this cross-check.',
        'referenceCandidates': len(reference),
        'referenceScoreCounts': dict(Counter(str(v['semantic']['completeScore']) for v in reference.values())),
        'comparedOnlineAttempts': len(checks), 'mismatches': sum(not c['matchesHistoricalSemanticResult'] for c in checks),
        'checks': checks, 'referenceEvidence': reference}
    batch.save(run / 'reference-check.json', result)
    print(f'Historical cross-check: {len(checks)} existing online attempts, {result["mismatches"]} mismatches; no new executions')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', type=Path, required=True)
    parser.add_argument('--historical-attempts', type=Path, required=True)
    args = parser.parse_args()
    result = compare(args.run.resolve(), args.historical_attempts.resolve())
    raise SystemExit(0 if result['mismatches'] == 0 else 1)
