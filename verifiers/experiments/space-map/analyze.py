#!/usr/bin/env python3
"""Read-only full-report analysis, including reports emitted with nonzero CLI exit."""
import argparse
from collections import Counter, defaultdict
import csv
import hashlib
import json
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'batch-execution'))
import run as batch


def action_model_key(workload_id, scenario):
    value = [workload_id, scenario['faultVector'], scenario['actions']]
    return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(',', ':')).encode()).hexdigest()


def shared_bindings(execution, expected_inputs):
    groups = defaultdict(list)
    bindings = (execution.get('sourceSetup') or {}).get('participantBindings', [])
    all_inputs = set(expected_inputs)
    for binding in bindings:
        if binding['inputVariantId'] not in all_inputs:
            raise ValueError('Unexpected input binding')
        if binding['status'] == 'RESOLVED':
            groups[(binding['sourceActionId'], binding['propertyName'])].append(binding)
    shared = []
    edges = {i: set() for i in all_inputs}
    for (action, prop), values in sorted(groups.items()):
        inputs = {v['inputVariantId'] for v in values}
        if len(inputs) > 1:
            if len({v['resolvedValue'] for v in values}) != 1:
                raise ValueError('Shared setup result resolved inconsistently')
            shared.append({'action': action, 'property': prop, 'inputs': sorted(inputs)})
            for inp in inputs:
                edges[inp].update(inputs)
    seen, pending = set(), list(sorted(all_inputs)[:1])
    while pending:
        current = pending.pop()
        if current not in seen:
            seen.add(current)
            pending.extend(edges[current] - seen)
    return shared, len(all_inputs) > 1 and seen == all_inputs


def analyze(plan_path, output):
    plan = batch.read(plan_path)
    batch.verify(plan, check_checkout=False)
    all_results = batch.read(output/'discovery.json') + batch.read(output/'repeats.json')
    rows = {r['caseId']: r for r in plan['rows']}
    inputs_by_workload = {s['workload']['id']: {p['input'] for p in s['workload']['participants']}
                          for s in plan['scopes'] if s['status'] == 'SELECTED'}
    behavior_by_id = {r['id']: action_model_key(s['workload']['id'], r)
                      for s in plan['scopes'] for r in s.get('generatedCandidates', [])}
    records, ids = [], []
    for result in all_results:
        row = rows[result['caseId']]
        directory = Path(result['directory'])
        record = {**row, 'actionModelKey': behavior_by_id[row['faultScenarioId']],
                  'repetition': result['repetition'], 'processStatus': result['status'],
                  'processExitCode': result.get('exitCode'), 'wallSeconds': result['wallSeconds'],
                  'executionStatus': 'NO_VALIDATED_REPORT', 'assessmentStatus': None, 'score': None,
                  'terminalStatus': None, 'scheduleConformance': None, 'executionAttemptId': None,
                  'sourceSetupStatus': None, 'hardStopReason': None, 'failedActions': [],
                  'sourceSetupSeconds': None,
                  'sharedBindings': [], 'sharedBindingGraphConnected': False,
                  'positiveObjectStates': [],
                  'conditions': None, 'semantic': None, 'reportHashes': result['reportHashes']}
        if all((directory/name).is_file() for name in batch.REPORTS):
            for name in batch.REPORTS:
                if batch.digest(directory/name) != result['reportHashes'][name]:
                    raise ValueError('Report hash mismatch: '+str(directory/name))
            e, v1, v2 = batch.reports(directory, row, Path(plan['manifest']))
            status, score = batch.assessment_result(e, v2)
            # A nonzero process exit never earns a score, even if its report says COMPLETE.
            if result.get('exitCode') != 0 and score is not None:
                raise ValueError('Nonzero exit with complete score: '+row['caseId'])
            record.update(executionStatus=status, assessmentStatus=v2['assessmentStatus'], score=score,
                          terminalStatus=e['terminalStatus'], scheduleConformance=e['scheduleConformance'],
                          executionAttemptId=e['executionAttemptId'],
                          sourceSetupStatus=(e.get('sourceSetup') or {}).get('status'),
                          sourceSetupSeconds=batch.setup_seconds(e, 'sourceSetup'),
                          hardStopReason=e.get('hardStopReason'), conditions={c['category']: c['positiveObjectCount'] for c in v2['categoryResults']},
                          semantic=batch.semantic(e, v1, v2))
            record['failedActions'] = [{k: a.get(k) for k in ('kind', 'sourceStepId', 'runtimeStepName', 'status',
                                      'bodyOutcome', 'faultOrigin', 'exceptionClass', 'exceptionMessage')}
                                     for a in e['actualActions'] if a['status'] not in ('COMPLETED', 'NOT_REACHED', 'SKIPPED')]
            record['sharedBindings'], record['sharedBindingGraphConnected'] = shared_bindings(e, inputs_by_workload[row['workloadId']])
            if status == 'COMPLETE' and score > 0:
                before = {json.dumps(s['identity'], sort_keys=True): s['lifecycleState'] for s in v2['baseline']}
                after = {json.dumps(s['identity'], sort_keys=True): s['lifecycleState'] for s in v2['finalState']}
                findings = defaultdict(set)
                for category in v2['categoryResults']:
                    for finding in category['findings']:
                        findings[json.dumps(finding['affectedObject'], sort_keys=True)].add(category['category'])
                record['positiveObjectStates'] = [{'identity': json.loads(key), 'categories': sorted(categories),
                                                  'baselineLifecycle': before.get(key, 'ABSENT'),
                                                  'finalLifecycle': after.get(key, 'ABSENT')}
                                                 for key, categories in sorted(findings.items())]
            ids.append(e['executionAttemptId'])
        records.append(record)
    if len(ids) != len(set(ids)):
        raise ValueError('Execution attempt reused')
    groups = []
    for scope in plan['scopes']:
        discovery = [r for r in records if r['caseGroup'] == scope['caseGroup'] and r['repetition'] == 1]
        valid = [r for r in discovery if r['executionStatus'] == 'COMPLETE']
        repeated = []
        for r in records:
            if r['caseGroup'] == scope['caseGroup'] and r['repetition'] == 2:
                original = next(o for o in discovery if o['caseId'] == r['caseId'])
                repeated.append({'caseId': r['caseId'], 'originalStatus': original['executionStatus'],
                                 'repeatStatus': r['executionStatus'],
                                 'comparable': original['semantic'] is not None and r['semantic'] is not None,
                                 'sameSemantic': original['semantic'] is not None and original['semantic'] == r['semantic'],
                                 'sameHardStopReason': original['hardStopReason'] == r['hardStopReason']})
        vector_results = defaultdict(list)
        all_vector_results = defaultdict(list)
        by_model = defaultdict(list)
        for r in discovery:
            all_vector_results[r['faultVector']].append(r)
            by_model[r['actionModelKey']].append(r)
        for r in valid:
            vector_results[r['faultVector']].append(r)
        groups.append({'caseGroup': scope['caseGroup'], 'sagas': scope['sagas'],
                       'discoveryAttempts': len(discovery),
                       'generatedCandidateIds': len(scope.get('generatedCandidates', [])),
                       'generatedActionModels': len({behavior_by_id[r['id']] for r in scope.get('generatedCandidates', [])}),
                       'selectedActionModels': len(by_model),
                       'completeActionModels': len({r['actionModelKey'] for r in valid}),
                       'positiveActionModels': len({r['actionModelKey'] for r in valid if r['score'] > 0}),
                       'positiveObjectStatePatterns': dict(Counter(s['identity']['aggregateType'] + ':' + s['baselineLifecycle'] + '->' + s['finalLifecycle']
                                                                  for r in valid for s in r['positiveObjectStates'])),
                       'equivalentIdChecks': [{'caseIds': [r['caseId'] for r in rs],
                                               'sameSemantic': all(r['semantic'] is not None and r['semantic'] == rs[0]['semantic'] for r in rs),
                                               'sameHardStopReason': len({r['hardStopReason'] for r in rs}) == 1}
                                              for rs in by_model.values() if len(rs) > 1],
                       'executionStatuses': dict(Counter(r['executionStatus'] for r in discovery)),
                       'assessmentStatuses': dict(Counter(str(r['assessmentStatus']) for r in discovery)),
                       'terminalStatuses': dict(Counter(str(r['terminalStatus']) for r in discovery)),
                       'conformance': dict(Counter(str(r['scheduleConformance']) for r in discovery)),
                       'setupStatuses': dict(Counter(str(r['sourceSetupStatus']) for r in discovery)),
                       'connectedRuntimeBindingAttempts': sum(r['sharedBindingGraphConnected'] for r in discovery),
                       'unassignedRuntimeFailureAttempts': sum(any(a.get('faultOrigin') == 'UNASSIGNED_RUNTIME' for a in r['failedActions']) for r in discovery),
                       'scoreDistribution': dict(Counter(str(r['score']) for r in valid)),
                       'conditionSignaturesCompleteOnly': dict(Counter(json.dumps(r['conditions'], sort_keys=True) for r in valid)),
                       'positiveScenarioIds': [r['faultScenarioId'] for r in valid if r['score'] > 0],
                       'noFaultControl': [{k: r[k] for k in ('caseId', 'executionStatus', 'score', 'terminalStatus', 'hardStopReason', 'failedActions')}
                                          for r in discovery if set(r['faultVector']) == {'0'}],
                       'hardStops': dict(Counter(str(r['hardStopReason']) for r in discovery if r['hardStopReason'])),
                       'recoveryOutcomeDifferences': {v: [{'caseId': r['caseId'], 'score': r['score'], 'conditions': r['conditions']} for r in rs]
                                                     for v, rs in vector_results.items() if len({json.dumps([r['score'], r['conditions']], sort_keys=True) for r in rs}) > 1},
                       'validityDifferencesAcrossRecoveries': {v: [{'caseId': r['caseId'], 'executionStatus': r['executionStatus'], 'score': r['score']} for r in rs]
                                                               for v, rs in all_vector_results.items() if len({r['executionStatus'] for r in rs}) > 1},
                       'repeatChecks': repeated})
    result = {'validatedReportAttemptIds': len(ids), 'totalAttempts': len(records), 'groups': groups,
              'discoveryExecutionStatuses': dict(Counter(r['executionStatus'] for r in records if r['repetition'] == 1)),
              'discoveryAssessmentStatuses': dict(Counter(str(r['assessmentStatus']) for r in records if r['repetition'] == 1)),
              'records': records}
    batch.save(output/'full-report-analysis.json', result)
    flat = [{k: json.dumps(v, sort_keys=True) if isinstance(v, (dict, list)) else v for k, v in r.items() if k != 'semantic'} for r in records]
    with (output/'full-attempts.csv').open('w', newline='') as stream:
        writer = csv.DictWriter(stream, fieldnames=list(flat[0]))
        writer.writeheader(); writer.writerows(flat)
    print(json.dumps({k:v for k,v in result.items() if k != 'records'}, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--plan', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    analyze(args.plan, args.output)
