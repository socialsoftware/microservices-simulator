#!/usr/bin/env python3
"""Check the ten copied-update histories against their uninstrumented application controls."""
import argparse
import json
from pathlib import Path

CASES = ['forward-stale', 'forward-fresh', 'recovery-stale', 'recovery-delayed-event', 'recovery-no-event']


def snapshot(value):
    result = {k: value[k] for k in ['identity', 'version', 'lifecycleState', 'runtimeType', 'applicationData', 'dependencies']}
    result['applicationData'] = {k: v for k, v in result['applicationData'].items()
                                 if k not in ['creationDate', 'lastModifiedTime']}
    return result


def comparable(report):
    return {**{k: report[k] for k in ['initial', 'final', 'producerOutcome']},
        'baseline': [snapshot(s) for s in report['persistentInitial']['aggregates']],
        'horizon': [snapshot(s) for s in report['persistentFinal']['aggregates']],
        'writes': [{'aggregate': snapshot(e['data']['aggregate']), 'writer': e['data']['writer']}
                   for e in report['observations'] if e['kind'] == 'WRITE']}


def main():
    p = argparse.ArgumentParser(); p.add_argument('--run', type=Path, required=True)
    p.add_argument('--baseline', type=Path, required=True); a = p.parse_args()
    rows = []
    for mode in ['false', 'true']:
        for case in CASES:
            name = case + '-' + mode + '.json'
            report = json.loads((a.run / name).read_text())
            baseline = json.loads((a.baseline / name).read_text())
            assessment = report['copiedUpdateAssessment']
            assert report['status'] == baseline['status'] == 'PASS', name
            assert comparable(report) == comparable(baseline), 'Business observation mismatch: ' + name
            expected = int(case in ['forward-stale', 'recovery-stale'])
            assert len(assessment['findings']) == expected, name
            assert not assessment['coverageGaps'], (name, assessment['coverageGaps'])
            if expected:
                assert len(assessment['findings'][0]['fields']) == 1
                field = assessment['findings'][0]['fields'][0]
                assert field['oldValue'] == 'TOPIC 1' and field['overwrittenValue'] == 'RENAMED TOPIC'
            rows.append({'case': case, 'serialized': mode == 'true', 'findings': expected,
                         'coverageGaps': 0, 'businessObservationsPreserved': True,
                         'traceBytes': len(json.dumps(report['copiedUpdateTrace']).encode())})
    summary = {'executions': len(rows), 'positiveExecutions': sum(r['findings'] > 0 for r in rows),
        'controls': sum(r['findings'] == 0 for r in rows), 'rows': rows,
        'comparisonScope': 'Initial/final business observations and all committed identities, revisions, writers, lifecycle, dependencies and business data; excludes framework metadata and top-level creationDate/lastModifiedTime wall-clock audit fields.'}
    (a.run / 'validation.json').write_text(json.dumps(summary, indent=2) + '\n')
    print(json.dumps(summary, indent=2))


if __name__ == '__main__': main()
