#!/usr/bin/env python3
"""Validate and summarize frozen control/fault pairs without treating missing data as zero."""
import argparse
import collections
import csv
import hashlib
import json
from pathlib import Path
from validate_assessment import validate_reports, STATUSES, CATEGORIES, require
from qualification import package


def load(path):
    return json.loads(path.read_text())


def summarize(selection_path, attempts, manifest):
    selection = load(selection_path)
    verified_package = package(manifest)
    require(verified_package['hashes']['manifest'] == selection['packageManifestSha256'], 'Frozen selection/package hash mismatch')
    allowed_manifests = {str(manifest.resolve())}
    report_root = Path(__file__).resolve().parents[2] / 'target'
    try:
        allowed_manifests.add('/reports/' + manifest.resolve().relative_to(report_root).as_posix())
    except ValueError:
        pass
    rows = []
    for selected in selection['rows']:
        vector = selected['faultVector']
        require(selected['role'] in ('control', 'late-fault'), 'Unknown selected role')
        expected = '0' * len(vector) if selected['role'] == 'control' else '0' * (len(vector) - 1) + '1'
        require(vector and vector == expected, 'Control/fault role does not match selected vector')
        directory = attempts / selected['caseId']
        paths = {key: directory / name for key, name in
                 [('execution', 'execution.json'), ('v1', 'impact-v1.json'), ('v2', 'execution.impact-v2.json')]}
        missing = [path.name for path in paths.values() if not path.is_file()]
        if missing:
            exit_path = directory / 'process-exit-code'
            rows.append({**selected, 'reportAvailability': 'MISSING_REPORTS', 'missingReports': missing,
                         'processExitCode': exit_path.read_text().strip() if exit_path.is_file() else None,
                         'logPath': str(directory / 'docker.log'), 'executionAttemptId': None,
                         'executionTerminalStatus': None, 'scheduleConformance': None,
                         'impactV1Score': None, 'impactV2Status': None, 'impactV2CompleteScore': None,
                         'impactV2ObservedLowerBound': None, 'categories': {},
                         'artifactSha256': {key: hashlib.sha256(path.read_bytes()).hexdigest()
                                            for key,path in paths.items() if path.is_file()}})
            continue
        e, v1, v2 = (load(path) for path in paths.values())
        validate_reports(e, v1, v2, selected['workloadId'], selected['faultScenarioId'], selected['faultVector'])
        require(v2.get('packageManifestPath') in allowed_manifests, 'Execution used another package manifest')
        rows.append({**selected, 'reportAvailability': 'PRESENT', 'executionAttemptId': e['executionAttemptId'],
                     'executionTerminalStatus': e['terminalStatus'], 'scheduleConformance': e.get('scheduleConformance'),
                     'sourceSetupStatus': (e.get('sourceSetup') or {}).get('status'),
                     'blockers': e.get('blockers', []), 'impactV1Status': v1.get('evaluationStatus'),
                     'impactV1Score': v1.get('impactScore'), 'impactV2Status': v2['assessmentStatus'],
                     'impactV2Reason': v2.get('assessmentReason'),
                     'impactV2CompleteScore': v2['completeScore'],
                     'impactV2ObservedLowerBound': v2['observedAffectedObjectCount'],
                     'categories': {c['category']: {'status': c['coverageStatus'], 'candidates': c['candidateCount'],
                                                  'positiveObjects': c['positiveObjectCount'],
                                                  'findings': c['findings'], 'unknownReasons': c['unknownReasons']}
                                    for c in v2['categoryResults']},
                     'coverageGaps': v2.get('coverageGaps', []),
                     'actionFailures': [{k: a.get(k) for k in ('kind', 'runtimeStepName', 'status', 'faultOrigin', 'exceptionClass', 'exceptionMessage')}
                                        for a in e.get('actualActions', []) if a.get('exceptionClass')],
                     'artifactSha256': {key: hashlib.sha256(path.read_bytes()).hexdigest() for key,path in paths.items()}})
    require(len({r['caseId'] for r in rows}) == len(rows), 'Duplicate selected case ID')
    reported = [r for r in rows if r['reportAvailability'] == 'PRESENT']
    require(len({r['executionAttemptId'] for r in reported}) == len(reported), 'Duplicate attempt ID')
    groups = collections.defaultdict(dict)
    for row in rows:
        require(row['role'] not in groups[row['pairId']], 'Duplicate role in pair')
        groups[row['pairId']][row['role']] = row
    pairs = []
    for pair_id, group in groups.items():
        require(set(group) == {'control', 'late-fault'}, 'Incomplete selected pair')
        control, fault = group['control'], group['late-fault']
        require(control['workloadId'] == fault['workloadId'], 'Pair workload mismatch')
        complete = control['impactV2Status'] == fault['impactV2Status'] == 'COMPLETE'
        control_ordinary = control['executionTerminalStatus'] == 'SUCCESS' and control['scheduleConformance'] == 'EXACT'
        pairs.append({'pairId': pair_id, 'controlStatus': control['impactV2Status'],
                      'controlExecution': control['executionTerminalStatus'], 'controlScore': control['impactV2CompleteScore'],
                      'faultStatus': fault['impactV2Status'], 'faultExecution': fault['executionTerminalStatus'],
                      'faultScore': fault['impactV2CompleteScore'], 'bothComplete': complete,
                      'controlSuccessfulExact': control_ordinary,
                      'scoreDifference': fault['impactV2CompleteScore'] - control['impactV2CompleteScore'] if complete else None,
                      'interpretation': 'Observed score comparison; no causal claim from subtraction alone' if complete else 'No complete-score comparison'})
    counts = collections.Counter(r['impactV2Status'] for r in rows)
    summary = {'attempted': len(rows), 'reported': len(reported), 'missingReports': len(rows) - len(reported), 'selectedPairs': len(pairs), 'selectedSagaTypes': selection['eligibleSagaCount'],
               'catalogueSagaTypes': selection['sagaCount'], 'impactV2Status': {s: counts[s] for s in STATUSES},
               'executionTerminalStatus': dict(collections.Counter(r['executionTerminalStatus'] for r in rows)),
               'scheduleConformance': dict(collections.Counter(r['scheduleConformance'] for r in rows)),
               'completeScores': dict(collections.Counter(str(r['impactV2CompleteScore']) for r in rows if r['impactV2Status']=='COMPLETE')),
               'completePairs': sum(p['bothComplete'] for p in pairs),
               'completePairsWithSuccessfulExactControl': sum(p['bothComplete'] and p['controlSuccessfulExact'] for p in pairs),
               'positiveCompleteControls': sum(r['role']=='control' and r['impactV2Status']=='COMPLETE' and r['impactV2CompleteScore']>0 for r in rows),
               'categoryPositiveAttempts': {c: sum(r['categories'][c]['positiveObjects']>0 for r in reported) for c in CATEGORIES}}
    return {'schema': 'impact-v2-broader-summary.v1', 'validation': 'PASS' if len(reported) == len(rows) else 'INCOMPLETE_ARTIFACTS',
            'selectionSha256': hashlib.sha256(selection_path.read_bytes()).hexdigest(),
            'packageManifestSha256': selection['packageManifestSha256'],
            'summary': summary, 'pairs': pairs, 'rows': rows, 'excludedSagas': selection['excludedSagas']}


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('selection', type=Path)
    parser.add_argument('attempts', type=Path)
    parser.add_argument('output', type=Path)
    parser.add_argument('--manifest', type=Path, required=True)
    args = parser.parse_args()
    result = summarize(args.selection, args.attempts, args.manifest)
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    columns = ('caseId', 'role', 'saga', 'shape', 'faultVector', 'executionTerminalStatus', 'scheduleConformance', 'impactV1Score', 'impactV2Status', 'impactV2CompleteScore', 'impactV2ObservedLowerBound')
    with args.output.with_suffix('.csv').open('w', newline='') as stream:
        writer = csv.DictWriter(stream, fieldnames=columns)
        writer.writeheader()
        writer.writerows({key: row[key] for key in columns} for row in result['rows'])
    print(json.dumps(result['summary'], indent=2))
