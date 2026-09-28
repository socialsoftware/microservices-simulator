"""Freeze the audited 28 September inventory for outcome-blind allocator replay."""
import argparse
import hashlib
import json
from pathlib import Path

import run


INVENTORY = run.MASTER / 'master-inventory-2026-09-28.json'
HISTORICAL = run.ROOT / 'verifiers/target/allocator-expanded-replay-qualified-2026-09-26/inputs.json'
GIANTS = 3


def freeze(output, without_giants=False):
    inventory = run.read(INVENTORY)
    selected = inventory['qualifiedWorkloads']
    if len(selected) != 316 or len({r['id'] for r in selected}) != len(selected):
        raise ValueError('Audited inventory identities changed')
    giants = {r['id'] for r in sorted(selected, key=lambda r: (-r['scenarios'], r['id']))[:GIANTS]}
    if sum(r['scenarios'] for r in selected if r['id'] in giants) != 12462:
        raise ValueError('Giant selection changed')
    if without_giants:
        selected = [r for r in selected if r['id'] not in giants]
    selected_ids = {r['id'] for r in selected}
    original = run.read(HISTORICAL)
    old_ids = {r['workload'] for p in original['partitions'] for r in p['workloads']}
    if old_ids != {r['id'] for r in inventory['qualifiedWorkloads'] if r['source'] == 'historical-qualified'}:
        raise ValueError('Historical frozen set differs from audited inventory')
    partitions = []
    for p in original['partitions']:
        rows = [r for r in p['workloads'] if r['workload'] in selected_ids]
        if rows:
            partitions.append({**p, 'workloads': rows})
    for item in selected:
        if item['id'] in old_ids:
            continue
        reference = run.ROOT / item['reference']
        if run.sha(reference) != item['referenceSha256']:
            raise ValueError('Audited reference changed: ' + item['id'])
        catalogue = reference.parent.parent / 'catalogue'
        metadata = catalogue / 'package/workloads.jsonl'
        interactions = catalogue / 'package/interactions.jsonl'
        workload = next((w for w in run.rows(metadata) if w['id'] == item['id']), None)
        if workload is None:
            raise ValueError('Workload metadata missing: ' + item['id'])
        interaction_rows = {r['id']: r for r in run.rows(interactions)}
        profile = run.structural_profile({
            'sagas': [{'fqn': p['saga']} for p in workload['participants']],
            'interactions': [interaction_rows[i] for i in workload['interactions']]}, workload)
        profile_hash = hashlib.sha256(json.dumps(profile, sort_keys=True).encode()).hexdigest()
        fitness = run.configuration(run.read(catalogue / 'config.json')['fitness'])
        files = [reference, metadata, interactions, catalogue / 'domain-count.json',
                 catalogue / 'requests.json', catalogue / 'config.json']
        partitions.append({'kind': 'qualified-recorded-domain',
            'files': {str(p.resolve()): run.sha(p) for p in files},
            'workloads': [{'workload': item['id'], 'workloadFile': str(metadata.resolve()),
                'interactionFile': str(interactions.resolve()),
                'reference': str(reference.resolve()), 'enumeration': str(catalogue.resolve()),
                'coverage': 'AUDITED_COMPLETE_DOMAIN', 'scenarios': item['scenarios'],
                'label': item['family'], 'fitness': fitness,
                'structuralProfileSha256': profile_hash}]})
    frozen = {**original, 'purpose': 'Audited 316-workload inventory; prospective seeded replay',
        'admissionRule': 'Audited complete maps, excluding preidentified preparation/control failures; no outcome selection',
        'sourceHashes': {**original['sourceHashes'], str(HISTORICAL): run.sha(HISTORICAL),
                         str(INVENTORY): run.sha(INVENTORY)},
        'admission': [{'id': r['id'], 'admission': 'RECORDED_BENCHMARK_ADMITTED'} for r in selected],
        'partitions': partitions, 'excludedGiantIds': sorted(giants) if without_giants else [],
        'cohort': 'without-three-giants' if without_giants else 'complete'}
    if sum(r['scenarios'] for r in selected) != (6396 if without_giants else 18858):
        raise ValueError('Scenario total differs from audited inventory')
    output.mkdir(parents=True, exist_ok=True)
    (output / 'inputs.json').write_text(json.dumps(frozen, indent=2) + '\n')
    workloads, _ = run.load(output, frozen)
    if len(workloads) != len(selected) or sum(len(w['domain'].candidates) for w in workloads) != sum(r['scenarios'] for r in selected):
        raise ValueError('Loaded cohort differs from audited inventory')
    return len(workloads)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--without-three-giants', action='store_true')
    args = parser.parse_args()
    print(freeze(args.output, args.without_three_giants), flush=True)
