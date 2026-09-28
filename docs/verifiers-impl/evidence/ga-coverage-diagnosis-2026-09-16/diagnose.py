"""Read-only diagnosis of retained campaign evidence; does not assign new scores."""
import collections
import json
from pathlib import Path

REPO = Path(__file__).resolve().parents[4]
ROOT = REPO / 'verifiers/target/ga-500x3-2026-09-15'

def identity(snapshot):
    return tuple(snapshot['identity'][k] for k in ('aggregateType', 'aggregateId'))

def changes(before, after):
    if before is None:
        return ['@presence']
    missing = object()
    a, b = before['applicationData'], after['applicationData']
    fields = [k for k in sorted(a.keys() | b.keys()) if a.get(k, missing) != b.get(k, missing)]
    if before['lifecycleState'] != after['lifecycleState']:
        fields.append('@lifecycle')
    return fields

rows, failures = [], []
for results in sorted(ROOT.glob('*/results.json')):
    for attempt in json.loads(results.read_text())['attempts']:
        if attempt['fitnessScore'] is not None:
            continue
        directory = Path(attempt['directory'])
        report = json.loads((directory / 'execution-report.json').read_text())
        evidence = json.loads((directory / 'execution-report.impact-v2.json').read_text())
        common = {'attempt': str(directory.relative_to(ROOT)), 'key': attempt['key'],
                  'unavailableCriteria': [k for k, v in attempt['fitnessComponents'].items() if v['count'] is None]}
        if 'COMPENSATED_READ_EXPOSURE' in common['unavailableCriteria']:
            reads = json.loads((directory / 'execution-report.saga-read-exposure.json').read_text())
            common['readGapReasons'] = sorted({g['reason'] for g in reads['gaps']})
        if report['terminalStatus'] == 'COMPENSATION_FAILED':
            failures.append(common | {'actions': [a for a in report['actualActions'] if a.get('exceptionClass')],
                                      'participants': report['participants'], 'blockers': report['blockers'],
                                      'tournamentDeletedAtHorizon': any(s['identity']['aggregateType'] == 'SagaTournament'
                                                                       and s['lifecycleState'] == 'DELETED'
                                                                       for s in evidence['finalState'])})
            continue
        failed = {a['sagaInstanceId'] for a in report['actualActions'] if a['status'] in ('ASSIGNED_FAULT', 'FAILED', 'COMMIT_FAILED')}
        baseline = {identity(s): s for s in evidence['baseline']}
        final = {identity(s): s for s in evidence['finalState']}
        for category in evidence['categoryResults']:
            for unknown in category['unknownReasons']:
                if unknown['reason'] != 'COMPETING_OR_UNKNOWN_WRITER':
                    continue
                obj = tuple(unknown['affectedObject'][k] for k in ('aggregateType', 'aggregateId'))
                writes = [w for w in evidence['committedWrites'] if identity(w['aggregate']) == obj]
                failed_writers = {w['writer']['sagaInstanceId'] for w in writes if w.get('writer') and w['writer']['sagaInstanceId'] in failed}
                assert len(failed_writers) == 1
                owner = next(iter(failed_writers))
                previous = baseline.get(obj)
                transitions = []
                failed_fields, other_fields = set(), set()
                all_owned = True
                chain = True
                for w in writes:
                    wr = w['writer'] or {}
                    fields = changes(previous, w['aggregate'])
                    valid = (wr.get('kind') == 'SAGA' and wr.get('executionAttemptId') == evidence['executionAttemptId']
                             and wr.get('workloadPlanId') == evidence['workloadPlanId'] and wr.get('phase') in ('FORWARD', 'RECOVERY'))
                    all_owned &= valid
                    pred = w['aggregate']['frameworkMetadata'].get('predecessorVersion')
                    chain &= (previous is not None and pred == previous['version']
                              and w['aggregate']['frameworkMetadata'].get('predecessorIdentity') == previous['identity'])
                    (failed_fields if wr.get('sagaInstanceId') == owner else other_fields).update(fields)
                    transitions.append({'sequence': w['sequence'], 'version': w['aggregate']['version'],
                                        'predecessorVersion': pred, 'saga': wr.get('sagaInstanceId'),
                                        'phase': wr.get('phase'), 'step': wr.get('stepName'), 'fields': fields})
                    previous = w['aggregate']
                overlap = failed_fields & other_fields
                classification = ('unattributed' if not all_owned else 'other_writes_no_data_change' if not other_fields
                                  else 'disjoint_fields' if not overlap else 'overlapping_fields')
                rows.append(common | {'object': list(obj), 'failedSaga': owner, 'classification': classification,
                                      'predecessorChain': chain, 'coverageGaps': evidence['coverageGaps'],
                                      'failedFields': sorted(failed_fields), 'otherFields': sorted(other_fields),
                                      'overlap': sorted(overlap), 'finalDifference': changes(baseline.get(obj), final[obj]),
                                      'otherWriterChangesLifecycle': '@lifecycle' in other_fields,
                                      'finalMatchesLastWrite': not changes(previous, final[obj]), 'transitions': transitions})

def counts(values):
    return dict(collections.Counter(values))

summary = {'partialAttempts': len(rows), 'distinctPartialKeys': len({r['key'] for r in rows}),
           'classification': counts(r['classification'] for r in rows),
           'objectTypes': counts(r['object'][0] for r in rows),
           'chains': counts(str(r['predecessorChain']) for r in rows),
           'gapAttempts': sum(bool(r['coverageGaps']) for r in rows),
           'finalMismatchAttempts': sum(not r['finalMatchesLastWrite'] for r in rows),
           'otherWriterChangesLifecycle': sum(r['otherWriterChangesLifecycle'] for r in rows),
           'failedFieldsStillDifferent': sum(bool(set(r['failedFields']) & set(r['finalDifference'])) for r in rows),
           'partialWithReadUnknown': sum('COMPENSATED_READ_EXPOSURE' in r['unavailableCriteria'] for r in rows),
           'readGapReasons': counts(reason for r in rows for reason in r.get('readGapReasons', [])),
           'prospectiveResidualOnlyResolutionsExcludingOtherLifecycleChanges': sum(
               not r['otherWriterChangesLifecycle'] and r['unavailableCriteria'] == ['FAILED_OPERATION_RESIDUAL'] for r in rows),
           'compensationFailures': len(failures), 'distinctFailureKeys': len({r['key'] for r in failures}),
           'failuresWithCommittedRemovalAndDeletedTournament': sum(r['tournamentDeletedAtHorizon'] and
               any(p['sagaInstanceId'] == 'p3' and p['finalState'] == 'COMMITTED' for p in r['participants']) for r in failures),
           'failureReasons': counts((a.get('runtimeStepName', '') + ': ' + str(a.get('exceptionMessage')))
                                   for r in failures for a in r['actions'] if 'COMPENSATION' in a['status'])}
output = ROOT / 'analysis/coverage-diagnosis.json'
output.write_text(json.dumps({'summary': summary, 'partial': rows, 'failures': failures}, indent=2) + '\n')
Path(__file__).with_name('summary.json').write_text(json.dumps(summary, indent=2) + '\n')
print(json.dumps(summary, indent=2))
