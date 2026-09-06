#!/usr/bin/env python3
"""Freeze a deterministic breadth sample before observing execution outcomes."""
import argparse
import collections
import hashlib
import json
from pathlib import Path


def select(package):
    def read(name):
        return [json.loads(line) for line in (package / name).read_text().splitlines()]
    workloads = read('workloads.jsonl')
    setups = {row['id']: row for row in read('setups.jsonl')}
    all_sagas = {row['fqn'] for row in read('sagas.jsonl')}
    scenarios = collections.defaultdict(list)
    for row in read('fault-scenarios.jsonl'):
        scenarios[row['workload']].append(row)
    groups = collections.defaultdict(list)
    for workload in workloads:
        if len(workload['participants']) != 1:
            continue
        if setups.get(workload.get('setup'), {}).get('materializable') is not True:
            continue
        if not any(set(row['faultVector']) == {'0'} for row in scenarios[workload['id']]):
            continue
        groups[workload['participants'][0]['saga']].append(workload)
    events = lambda w: sum(action['kind'] == 'event' for action in w['schedule'])
    cost = lambda w: len(setups[w['setup']]['actions'])
    rows = []
    for saga, choices in sorted(groups.items()):
        selected = [('basic', min(choices, key=lambda w: (events(w), cost(w), w['id'])))]
        event_choices = [w for w in choices if events(w)]
        if event_choices:
            selected.append(('events', min(event_choices, key=lambda w: (-events(w), cost(w), w['id']))))
        seen = set()
        for shape, workload in selected:
            if workload['id'] in seen:
                continue
            seen.add(workload['id'])
            fault_rows = scenarios[workload['id']]
            width = len(fault_rows[0]['faultVector'])
            pair = saga.split('.')[-1].removesuffix('FunctionalitySagas') + '-' + shape
            for role, vector in [('control', '0' * width), ('late-fault', '0' * (width - 1) + '1')]:
                candidates = [f for f in fault_rows if f['faultVector'] == vector]
                if not candidates:
                    raise ValueError(f'Missing required vector {vector}: {workload["id"]}')
                scenario = min(candidates, key=lambda f: f['id'])
                rows.append(dict(caseId=pair+'-'+role, cohort='broader', pairId=pair,
                                 role=role, saga=saga, shape=shape, workloadId=workload['id'],
                                 faultScenarioId=scenario['id'], faultVector=vector,
                                 eventCount=events(workload), setupActionCount=cost(workload),
                                 recoverySchedulesAvailable=len(candidates)))
    manifest = package / 'scenario-catalog-manifest.json'
    return dict(schema='impact-v2-broader-selection.v1',
                selectionRule='One materializable single-participant workload per Saga, minimizing event count, setup length, then workload ID; additionally one event-bearing workload per eligible Saga maximizing event count, then minimizing setup length and ID. Pair zero vector with last-slot fault, choosing smallest scenario ID if multiple recovery schedules. No runtime outcomes used.',
                packageManifestSha256=hashlib.sha256(manifest.read_bytes()).hexdigest(),
                sagaCount=len(all_sagas), eligibleSagaCount=len(groups),
                excludedSagas=[dict(saga=s, reason='No single-participant workload with materializable setup and persisted zero vector') for s in sorted(all_sagas - groups.keys())],
                rows=rows)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('package', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    result = select(args.package)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({key: result[key] for key in ('sagaCount', 'eligibleSagaCount', 'packageManifestSha256')}))
    print(f'{len(result["rows"])} attempts, {len(result["rows"]) // 2} control/fault pairs')
