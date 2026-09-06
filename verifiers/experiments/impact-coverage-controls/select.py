#!/usr/bin/env python3
"""Freeze source-backed replacements for the previously named control/fault pairs."""
import argparse
import collections
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'impact-v2-broader'))
from qualification import package, select_workload, canonical_vectors, action_model


def select(manifest, old_manifest, old_selection, overrides):
    current, previous = package(manifest), package(old_manifest)
    inputs = {r['id']: r for r in current['records']['inputs']}
    old_workloads = {r['id']: r for r in previous['records']['workloads']}
    old_inputs = {r['id']: r for r in previous['records']['inputs']}
    setups = {r['id']: r for r in current['records']['setups']}
    scenarios = collections.defaultdict(list)
    for s in current['records']['faultScenarios']:
        scenarios[s['workload']].append(s)
    rows, changes = [], []
    for old in old_selection['rows']:
        if old['role'] != 'control':
            continue
        pair = old['pairId']
        prior_input = old_inputs[old_workloads[old['workloadId']]['participants'][0]['input']]
        override = overrides.get(pair)
        candidates = []
        for w in current['records']['workloads']:
            if len(w['participants']) != 1 or w['participants'][0]['saga'] != old['saga']:
                continue
            if not setups.get(w.get('setup'), {}).get('materializable'):
                continue
            i = inputs[w['participants'][0]['input']]
            if override:
                if not all(i.get('source', {}).get(k) == v for k, v in override['source'].items()):
                    continue
            elif i['id'] != prior_input['id']:
                continue
            event_count = sum(a['kind'] == 'event' for a in w['schedule'])
            if (old['shape'] == 'events') != bool(event_count):
                continue
            candidates.append(w)
        if not candidates:
            raise ValueError(f'No source-backed replacement for {pair}; do not silently exclude it')
        # Preserve the old forward/action route where possible; ties are fixed before execution.
        old_shape = old_workloads[old['workloadId']]['schedule']
        def action_shape(w):
            return [(a['kind'], a.get('sagaStep'), a.get('route')) for a in w['schedule']]
        prior_shape = action_shape({'schedule': old_shape})
        w = min(candidates, key=lambda w: (action_shape(w) != prior_shape,
                    -sum(a['kind'] == 'event' for a in w['schedule']) if old['shape'] == 'events' else 0,
                    len(setups[w['setup']]['actions']), w['id']))
        i = inputs[w['participants'][0]['input']]
        width = sum(a['kind'] == 'step' for a in w['schedule'])
        changes.append({'pairId': pair, 'oldWorkloadId': old['workloadId'], 'newWorkloadId': w['id'],
                        'oldInputId': prior_input['id'], 'newInputId': i['id'],
                        'oldSource': prior_input['source'], 'newSource': i['source'],
                        'reason': override['reason'] if override else 'Preserve prior input; regenerate current recovery/observation contract',
                        'eligibleCandidateCount': len(candidates)})
        for role, vector in [('control', '0' * width), ('late-fault', '0' * (width - 1) + '1')]:
            choices = [s for s in scenarios[w['id']] if s['faultVector'] == vector]
            if not choices:
                raise ValueError(f'Missing vector {vector} for {pair}')
            s = min(choices, key=lambda s: s['id'])
            rows.append({'caseId': pair+'-'+role, 'cohort': 'broader', 'pairId': pair,
                         'role': role, 'saga': old['saga'], 'shape': old['shape'],
                         'workloadId': w['id'], 'faultScenarioId': s['id'], 'faultVector': vector,
                         'eventCount': sum(a['kind']=='event' for a in w['schedule']),
                         'setupActionCount': len(setups[w['setup']]['actions']),
                         'recoverySchedulesAvailable': len(choices)})
    if set(overrides) - {r['pairId'] for r in rows}:
        raise ValueError('An override did not correspond to a selected pair')
    broader = {'schema': 'impact-v2-source-requalified-selection.v1',
               'selectionRule': 'Preserve named prior pairs; declared source replacements, then matching action shape, event count, setup cost and workload ID. Frozen before this campaign outcomes.',
               'packageManifestSha256': current['hashes']['manifest'],
               'sagaCount': len(current['records']['sagas']),
               'eligibleSagaCount': len({r['saga'] for r in rows}),
               'excludedSagas': [{'saga': s, 'reason': 'Outside the retained earlier comparison cohort'}
                   for s in sorted({s['fqn'] for s in current['records']['sagas']} - {r['saga'] for r in rows})],
               'rows': rows, 'changes': changes}
    benchmark = select_workload(current)
    vectors = canonical_vectors(benchmark)
    chosen = sorted((s for s in scenarios[benchmark['id']]), key=lambda s:(s['faultVector'], s['id']))
    if {s['faultVector'] for s in chosen} != set(vectors):
        raise ValueError('Request only missing canonical benchmark vectors before freezing selection')
    # Repeated requests can produce distinct IDs for identical actions: reject them, never silently deduplicate.
    signatures = {(s['faultVector'], json.dumps(s['actions'], sort_keys=True)) for s in chosen}
    if len(signatures) != len(chosen):
        raise ValueError('Duplicate exact benchmark action sequences; regenerate without overlapping requests')
    bench = {'packageManifestSha256': current['hashes']['manifest'], 'workloadId': benchmark['id'], 'rows': [
        {'cohort': 'benchmark', 'caseId': s['id'], 'faultScenarioId': s['id'],
         'workloadId': benchmark['id'], 'faultVector': s['faultVector'],
         'normalizedActions': action_model(current, benchmark, s)} for s in chosen]}
    return broader, bench


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--manifest', type=Path, required=True)
    p.add_argument('--old-manifest', type=Path, required=True)
    p.add_argument('--old-selection', type=Path, required=True)
    p.add_argument('--overrides', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    args = p.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    broader, benchmark = select(args.manifest, args.old_manifest,
        json.loads(args.old_selection.read_text()), json.loads(args.overrides.read_text()))
    for name, value in [('broader-selection.json', broader), ('benchmark-selection.json', benchmark)]:
        output = args.output/name
        if output.exists(): raise ValueError(f'Refusing to replace selection: {output}')
        output.write_text(json.dumps(value, indent=2)+'\n')
    print(f"Frozen {len(broader['rows'])} broader and {len(benchmark['rows'])} benchmark attempts")
