"""Prepare bounded scheduling breadth; preparation does not admit measured cases."""
import argparse
import copy
from functools import lru_cache
import hashlib
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'fixed-workload-ga'))
from runtime import Runtime, digest, package, read, save
from fitness import configuration, CRITERIA_V2


@lru_cache(None)
def records(path):
    return [json.loads(line) for line in Path(path).read_text().splitlines() if line.strip()]


def lines(path, values):
    path.write_text(''.join(json.dumps(value, sort_keys=True) + '\n' for value in values))


def prepare(proposal_path, inventory_path, runtime_path, output):
    proposal, frozen, runtime = read(proposal_path), read(inventory_path), read(runtime_path)
    Runtime(runtime).verify()
    output.mkdir(parents=True, exist_ok=False)
    known = {r['workload']: (part, r) for part in frozen['partitions'] for r in part['workloads']}
    candidates = proposal['candidates']
    if len(candidates) != len({r['workload'] for r in candidates}):
        raise ValueError('Repeated proposed workload')
    if any(r['workload'] in known for r in candidates):
        raise ValueError('Expansion includes an existing measured workload')
    fitness = configuration({'policy': 'weighted-criteria-v2', 'weights': {c: 1 for c in CRITERIA_V2}})
    rows, checked = [], set()
    for candidate in candidates:
        source_file = Path(candidate['source'])
        if source_file not in checked:
            if digest(source_file) != candidate['sourceSha256']:
                raise ValueError('Generated workload source changed')
            package(source_file.parent / 'scenario-catalog-manifest.json')
            checked.add(source_file)
        source = source_file.parent
        workload = copy.deepcopy(next(w for w in records(str(source_file))
                                      if w['id'] == candidate['workload']))
        part, analogue = known[candidate['matchesMeasuredWorkload']]
        setup = copy.deepcopy(next(s for s in records(str(source / 'setups.jsonl'))
                                   if s['id'] == workload['setup']))
        setup_mode = 'RETAINED_EXPLICIT_FIXTURE'
        qualified_source = None
        if part.get('kind') == 'qualified-recorded-domain':
            qualified_source = Path(analogue['workloadFile']).parent
            old = next(w for w in records(str(qualified_source / 'workloads.jsonl'))
                       if w['id'] == analogue['workload'])
            if sorted(p['input'] for p in old['participants']) != sorted(
                    p['input'] for p in workload['participants']):
                raise ValueError('Qualified fixture has different participants')
            setup = copy.deepcopy(next(s for s in records(str(qualified_source / 'setups.jsonl'))
                                       if s['id'] == old['setup']))
            setup_mode = 'QUALIFIED_MATCHING_INPUT_FIXTURE'
        selected_inputs = {p['input'] for p in workload['participants']}
        setup['bindings'] = [b for b in setup['bindings'] if b['input'] in selected_inputs]
        setup['id'] = 'expansion-fixture-' + hashlib.sha256(
            json.dumps({k: v for k, v in setup.items() if k != 'id'}, sort_keys=True).encode()).hexdigest()[:24]
        workload['setup'] = setup['id']
        directory = output / workload['id'][:12]
        pack = directory / 'package'
        pack.mkdir(parents=True)
        lines(pack / 'sagas.jsonl', records(str(source / 'sagas.jsonl')))
        inputs = [i for i in records(str(source / 'inputs.jsonl')) if i['id'] in selected_inputs]
        if {i['id'] for i in inputs} != selected_inputs:
            raise ValueError('Missing selected input')
        if qualified_source:
            qualified_inputs = {i['id']: i for i in records(str(qualified_source / 'inputs.jsonl'))}
            if any(i != qualified_inputs[i['id']] for i in inputs):
                raise ValueError('Qualified fixture input record differs')
        lines(pack / 'inputs.jsonl', inputs)
        lines(pack / 'setups.jsonl', [setup])
        lines(pack / 'workloads.jsonl', [workload])
        interactions = [i for i in records(str(source / 'interactions.jsonl'))
                        if i['id'] in workload['interactions']]
        if {i['id'] for i in interactions} != set(workload['interactions']):
            raise ValueError('Missing selected interaction')
        lines(pack / 'interactions.jsonl', interactions)
        for name in ('fault-scenarios.jsonl', 'requests.jsonl'):
            lines(pack / name, [])
        # Historical analysis accounting is provenance, not the new admission count.
        for name in ('copy-contracts.json', 'accounting.json'):
            save(pack / name, read(source / name))
        roles = {'sagas': 'sagas.jsonl', 'inputs': 'inputs.jsonl', 'setups': 'setups.jsonl',
            'workloads': 'workloads.jsonl', 'interactions': 'interactions.jsonl',
            'faultScenarios': 'fault-scenarios.jsonl', 'requests': 'requests.jsonl',
            'accounting': 'accounting.json', 'copy-contracts': 'copy-contracts.json'}
        manifest = pack / 'scenario-catalog-manifest.json'
        save(manifest, {'formatVersion': 1, 'files': {role: {'path': name,
                      'sha256': digest(pack / name)} for role, name in roles.items()}})
        package(manifest)
        config = {'manifest': str(manifest), 'workload': workload['id'], 'runtime': runtime,
                  'recoveryCap': 100000, 'timeout': 180, 'fitness': fitness}
        save(directory / 'config.json', config)
        rows.append({**candidate, 'state': 'PREPARED_NOT_EXECUTED',
            'setupMode': setup_mode, 'qualifiedFixtureSource': str(qualified_source) if qualified_source else None,
            'directory': str(directory), 'config': str(directory / 'config.json'),
            'configSha256': digest(directory / 'config.json'), 'packageHashes': package(manifest)['hashes'],
            'staticSourceHashes': {name: digest(source / name) for name in
                ('sagas.jsonl', 'inputs.jsonl', 'setups.jsonl', 'interactions.jsonl', 'copy-contracts.json')}})
    Runtime(runtime).verify()
    plan = {'status': 'PREPARED_NOT_EXECUTED', 'selectionRule': proposal['selectionRule'],
        'selectionSeed': proposal['selectionSeed'], 'workloads': len(rows),
        'families': len({r['family'] for r in rows}),
        'currentSmallerMeasuredScenarios': proposal['currentMeasuredSmallerScenarios'],
        'analogueEstimatedAddedScenarios': proposal['analogueEstimatedAddedScenarios'],
        'sources': {str(p.resolve()): digest(p) for p in
                    (Path(__file__), proposal_path, inventory_path, runtime_path)},
        'admission': 'Each schedule requires its own setup preflight, valid scored no-fault control, uncapped complete catalogue and preserved execution/measurement outcomes. Positive controls are allowed; unknowns are not negatives.',
        'clusterLeaseRequiredBeforeExecution': True, 'rows': rows}
    save(output / 'plan.json', plan)
    print(json.dumps({k: v for k, v in plan.items() if k not in ('rows', 'sources')}, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    for name in ('proposal', 'inventory', 'runtime', 'output'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    prepare(args.proposal.resolve(), args.inventory.resolve(), args.runtime.resolve(), args.output.resolve())
