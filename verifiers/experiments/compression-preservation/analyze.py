#!/usr/bin/env python3
"""Compare explicit finding signatures within identical named fault assignments.

This is observational coverage for the implemented detectors, not equivalence of
all execution histories or proof that all business harms are preserved.
"""
import json
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'fixed-workload-ga'))
from runtime import read, save, package
from fitness import components


def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(',', ':'))


def projection(snapshot):
    # Keep application values, declared dependencies and lifecycle. Only framework
    # row/version/lock metadata is excluded from this supplementary state comparison.
    return {k: snapshot[k] for k in ('identity', 'lifecycleState', 'applicationData', 'dependencies')}


def signatures(directory, workload):
    execution = read(directory / 'execution-report.json')
    impact = read(directory / 'execution-report.impact-v2.json')
    reads = read(directory / 'execution-report.saga-read-exposure.json')
    lost = read(directory / 'execution-report-lost-copied-updates.json')
    roles = {p['id']: p['saga'] for p in workload['participants']}
    steps = {s['id']: [roles[s['participant']], s['sagaStep']] for s in workload['schedule']}
    actions = {a['actionId']: [roles.get(a['sagaInstanceId'], a['sagaInstanceId']),
               a['kind'], a['runtimeStepName']] for a in execution['actualActions']}
    before = {canonical(s['identity']): s for s in impact['baseline']}
    after = {canonical(s['identity']): s for s in impact['finalState']}
    facts = []
    for category in impact['categoryResults']:
        for f in category['findings']:
            identity = canonical(f['affectedObject'])
            facts.append({
                'criterion': category['category'], 'reason': f['reason'],
                'affectedObject': f['affectedObject'], 'relatedObject': f['relatedObject'],
                'actions': sorted([actions[a] for a in f['actionIds']]),
                'before': projection(before[identity]) if identity in before else None,
                'after': projection(after[identity]) if identity in after else None})
    for f in reads['findings']:
        facts.append({'criterion': 'COMPENSATED_READ_EXPOSURE', 'category': f['category'],
            'identity': f['identity'], 'producer': roles[f['producerSagaId']],
            'reader': roles[f['readerSagaId']], 'sourceStep': steps[f['sourceScheduledStepId']],
            'restoredAttributes': sorted(f['restoredAttributes']),
            'notRestoredAttributes': sorted(f['notRestoredAttributes'])})
    # This fixed update/query pair has no copied-update writer. Do not silently
    # discard an unexpected positive whose signature has not been specified.
    if lost['findings']:
        raise ValueError('Unexpected copied-update finding: retain evidence and define its signature before comparing')
    initial = sorted([projection(s) for s in impact['baseline']], key=canonical)
    final = sorted([projection(s) for s in impact['finalState']], key=canonical)
    return sorted({canonical(f) for f in facts}), canonical(initial), canonical(final)


def summarize(root):
    root = Path(root)
    run = root / 'run'
    inventory = read(run / 'inventory.json')
    workloads = {w['id']: w for w in package(root / 'full/scenario-catalog-manifest.json')['records']['workloads']}
    rows, baselines = [], set()
    by_fault = {}
    for row in inventory:
        directory = run / f'attempt-{row["number"]:03d}'
        if not (directory / 'attempt.json').exists():
            continue
        result = read(directory / 'attempt.json')
        values = components(result, True)
        complete = (result.get('terminalStatus') in ('SUCCESS', 'COMPENSATED', 'PARTIAL_COMPENSATED')
                    and result.get('scheduleConformance') == 'EXACT'
                    and all(v['count'] is not None for v in values.values()))
        facts, baseline, final = signatures(directory, workloads[row['candidate']['workload']])
        baselines.add(baseline)
        fault = canonical(row['faults'])
        group = by_fault.setdefault(fault, {'fullFacts': set(), 'keptFacts': set(),
            'fullOutcomes': set(), 'keptOutcomes': set(), 'fullFinalStates': set(),
            'keptFinalStates': set(), 'measured': 0, 'unknown': 0})
        group['measured'] += 1
        group['unknown'] += not complete
        if complete:
            group['fullFacts'].update(facts)
            group['fullOutcomes'].add(canonical(facts))
            group['fullFinalStates'].add(final)
            if row['retained']:
                group['keptFacts'].update(facts)
                group['keptOutcomes'].add(canonical(facts))
                group['keptFinalStates'].add(final)
        rows.append({'number': row['number'], 'retained': row['retained'], 'faults': row['faults'],
            'workload': row['candidate']['workload'], 'scenario': row['candidate']['id'],
            'terminal': result.get('terminalStatus'), 'conformance': result.get('scheduleConformance'),
            'complete': complete, 'criteria': values, 'findings': [json.loads(f) for f in facts],
            'wallSeconds': result.get('wallSeconds')})
    comparisons = []
    for fault, group in sorted(by_fault.items()):
        expected = sum(canonical(r['faults']) == fault for r in inventory)
        comparisons.append({'faults': json.loads(fault), 'measured': group['measured'], 'expected': expected,
            'unknown': group['unknown'], 'comparisonComplete': group['measured'] == expected and group['unknown'] == 0,
            'fullDistinctFindings': len(group['fullFacts']), 'keptDistinctFindings': len(group['keptFacts']),
            'findingsAbsentFromMeasuredKept': [json.loads(f) for f in sorted(group['fullFacts']-group['keptFacts'])],
            'fullFindingCombinations': len(group['fullOutcomes']), 'keptFindingCombinations': len(group['keptOutcomes']),
            'findingCombinationsAbsentFromMeasuredKept': [json.loads(f) for f in sorted(group['fullOutcomes']-group['keptOutcomes'])],
            'fullObservedFinalStates': len(group['fullFinalStates']), 'keptObservedFinalStates': len(group['keptFinalStates']),
            'finalStatesAbsentFromMeasuredKept': len(group['fullFinalStates']-group['keptFinalStates'])})
    summary = {'scope': 'UpdateTournament + FindTournament, fixed original test inputs; full canonical fault domain; no selected events',
        'planned': len(inventory), 'plannedRetained': sum(r['retained'] for r in inventory),
        'measured': len(rows), 'unknown': sum(not r['complete'] for r in rows),
        'sameObservedBaseline': len(baselines) == 1, 'comparisonComplete': len(rows) == len(inventory)
            and all(r['complete'] for r in rows) and len(baselines) == 1,
        'comparisons': comparisons, 'attempts': rows}
    save(run / 'comparison.json', summary)
    lines = ['# Segment compression: update and query', '',
        f'Measured {len(rows)}/{len(inventory)} scenarios; {sum(not r["complete"] for r in rows)} incomplete observations.',
        f'The compressed subset contains {summary["plannedRetained"]} scenarios across three of six forward orders.',
        f'Identical observed application baseline: {summary["sameObservedBaseline"]}.', '',
        'Only complete per-fault comparisons support preservation claims. Missing signatures while execution is ongoing are provisional.', '',
        '| Assigned faults | Measured/expected | Unknown | Distinct findings: full / kept | Absent from measured kept |',
        '| --- | ---: | ---: | ---: | ---: |']
    for c in comparisons:
        label = ', '.join(s.rsplit('.', 1)[-1].replace('FunctionalitySagas', '') + ':' + step for s, step in c['faults']) or 'No fault'
        lines.append(f'| {label} | {c["measured"]}/{c["expected"]} | {c["unknown"]} | {c["fullDistinctFindings"]} / {c["keptDistinctFindings"]} | {len(c["findingsAbsentFromMeasuredKept"])} |')
    lines += ['', 'Finding signatures retain criterion, affected/related object identities and source operations. Persistent findings additionally retain before/after application projections; read findings retain producer, reader, source step and restored/not-restored attributes. Run IDs and evidence version counters are excluded. Aggregate IDs are retained, with baseline equality checked.', '',
        'This measures implemented detector observations for one fixed input pair. It is not a universal harm oracle. Final-state projection differences are supplementary observations, not automatically harmful; nested application version fields remain present.', '']
    (run / 'RESULTS.md').write_text('\n'.join(lines))
    return summary


if __name__ == '__main__':
    summarize(Path(sys.argv[1]).resolve())
