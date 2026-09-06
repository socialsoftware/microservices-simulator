#!/usr/bin/env python3
"""Check exact event reuse, terminal behavior, final lifecycle, and existing score."""
import argparse
import hashlib
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'impact-v2-broader'))
from validate_assessment import validate_reports

LABELS = {'QuizAnswerEventHandling': 'answer', 'QuizEventHandling': 'quiz',
          'TournamentEventHandling': 'tournament'}


def validate(run):
    plan = json.loads((run / 'plan.json').read_text())
    results = []
    identities = set()
    for row in plan['rows']:
        case = run / row['cohort'] / 'attempts' / row['caseId']
        execution = json.loads((case / 'execution.json').read_text())
        impact = json.loads((case / 'execution.impact-v2.json').read_text())
        invariant = json.loads((case / 'impact-v1.json').read_text())
        validate_reports(execution, invariant, impact, row['workloadId'], row['faultScenarioId'], row['faultVector'])
        assert execution['executionAttemptId'] == impact['executionAttemptId']
        assert execution['executionAttemptId'] not in identities
        identities.add(execution['executionAttemptId'])
        assert execution['workloadPlanId'] == row['workloadId']
        assert execution['faultScenarioId'] == row['faultScenarioId']
        actions = [a for a in execution['actualActions'] if a['kind'] == 'EVENT_CONSEQUENCE']
        delivered = [a for a in actions if a['status'] == 'COMPLETED']
        assert len(actions) == len(row['deliveryOrder'])
        actual_order = [LABELS[a['eventEvidence']['eventHandlingClassFqn'].split('.')[-1]] for a in delivered]
        assert actual_order == row['deliveryOrder'][:len(delivered)]
        event_ids = {a['eventEvidence']['eventId'] for a in delivered}
        assert len(event_ids) <= 1
        assert len({a['actionId'] for a in delivered}) == len(delivered)
        assert len(impact['eventDeliveries']) == len(delivered)
        final = {str(s['identity']['aggregateType']) + ':' + str(s['identity']['aggregateId']): s['lifecycleState']
                 for s in impact['finalState']}
        invalid = row['caseId'].endswith('missing-tournament')
        if invalid:
            assert execution['terminalStatus'] == 'UNEXPECTED_EXECUTION_FAILURE'
            assert impact['assessmentStatus'] == 'INVALID' and impact['completeScore'] is None
            assert len(delivered) == 2 and actions[-1]['status'] == 'SELECTED_SUBSCRIBER_NOT_FOUND'
        elif row['faultVector'] == '001':
            assert execution['terminalStatus'] == 'COMPENSATED'
            assert all(a['status'] == 'MASKED_BY_TRIGGER_FAULT' for a in actions)
            assert final['SagaExecution:2'] == 'ACTIVE'
            assert impact['assessmentStatus'] == 'COMPLETE' and impact['completeScore'] == 1
        else:
            assert execution['terminalStatus'] == 'SUCCESS'
            assert execution['scheduleConformance'] == 'EXACT'
            assert impact['assessmentStatus'] == 'COMPLETE'
            assert len(delivered) == len(row['deliveryOrder'])
            assert final['SagaExecution:2'] == 'DELETED'
            assert final['SagaQuiz:6'] == ('INACTIVE' if 'quiz' in row['deliveryOrder'] else 'ACTIVE')
            assert final['SagaQuizAnswer:7'] == ('DELETED' if 'answer' in row['deliveryOrder'] else 'ACTIVE')
            assert impact['completeScore'] == 2 - len(row['deliveryOrder'])
        results.append({'case': row['caseId'], 'order': row['deliveryOrder'],
                        'vector': row['faultVector'], 'terminalStatus': execution['terminalStatus'],
                        'assessmentStatus': impact['assessmentStatus'], 'score': impact['completeScore'],
                        'eventIds': sorted(event_ids), 'delivered': actual_order,
                        'eventStatuses': [a['status'] for a in actions], 'finalLifecycle': final,
                        'executionAttemptId': execution['executionAttemptId'],
                        'reports': {name: {'path': str((case/name).resolve()),
                                          'sha256': hashlib.sha256((case/name).read_bytes()).hexdigest()}
                                    for name in ['execution.json', 'execution.impact-v2.json']}})
    output = {'validation': 'PASS', 'scope': 'One source fixture; five successful horizons, two masked combined deliveries, one retained missing-third-receiver attempt.',
              'results': results}
    (run / 'comparison.json').write_text(json.dumps(output, indent=2) + '\n')
    print(json.dumps([{k:r[k] for k in ['case','terminalStatus','assessmentStatus','score']} for r in results], indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--run', type=Path, required=True)
    validate(parser.parse_args().run)
