"""Validate generated packages and summarize the static pruning audit."""
import hashlib
import json
from pathlib import Path
import sys

root = Path(sys.argv[1]).resolve()
def read(path):
    return json.loads(path.read_text())
def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

inventory = read(root / 'inventory.json')
rows = inventory['pairs']
assert len({tuple(r['sagas']) for r in rows}) == len(rows)
assert all(0 <= r['selectedTuples'] <= r['allTuples'] for r in rows)
rejected = [r for r in rows if r['selectedTuples'] == 0]
probes = read(root / 'probes.json')
enumeration = read(root / 'fault-enumeration.json')
counts = {str(Path(e['package']).parent.name): e for e in enumeration}
results = []
for probe in probes:
    if 'BRUTE_FORCE' not in probe:
        results.append({'sagas': probe['sagas'], 'status': probe['status']})
        continue
    full = {w['id'] for w in probe['BRUTE_FORCE']['workloads']}
    pruned = {w['id'] for w in probe['INTERACTION_PRUNED']['workloads']}
    assert pruned <= full
    assert not any('cap' in warning.lower() for strategy in ('BRUTE_FORCE', 'INTERACTION_PRUNED')
                   for warning in probe[strategy]['warnings'])
    results.append({'sagas': probe['sagas'], 'fullWorkloads': len(full),
                    'prunedWorkloads': len(pruned),
                    'staticMaterializable': sum(w['materializability']['materializable'] for w in probe['BRUTE_FORCE']['workloads']),
                    'runtimeQualified': False})
verified = {}
for path in sorted(root.glob('*/*/scenario-catalog-manifest.json')):
    manifest = read(path)
    for entry in manifest['files'].values():
        assert digest(path.parent / entry['path']) == entry['sha256'], path
    verified[str(path.relative_to(root))] = digest(path)
for entry in enumeration:
    package = Path(entry['package'])
    scenarios = [json.loads(l) for l in (package / 'fault-scenarios.jsonl').read_text().splitlines() if l.strip()]
    assert len(scenarios) == len({s['id'] for s in scenarios}) == entry['distinctScenarios']
    assert all(r['status'] in ('PERSISTED', 'DEDUPLICATED') and
               int(r['uncappedScheduleCount']) == r['writtenScheduleCount'] for r in entry['requests'])
summary = {'scope': 'Static selection and complete enumeration; no runtime observations',
    'eligibleInputs': inventory['normalization']['inputVariantsAccepted'],
    'pairs': len(rows), 'retainedPairs': len(rows)-len(rejected), 'discardedPairs': len(rejected),
    'discardedInputTuples': sum(r['allTuples']-r['selectedTuples'] for r in rows),
    'discardedWithPotentialEventBridge': sum(bool(r['potentialEventBridges']) for r in rejected),
    'discardedWithPotentialRecoveryInteraction': sum(r['potentialRecoveryInteraction'] for r in rejected),
    'probes': results, 'enumeration': [{k:v for k,v in e.items() if k != 'requests'} for e in enumeration],
    'verifiedPackageManifests': verified,
    'artifactHashes': {p.name:digest(p) for p in sorted(root.glob('*.json')) if p.name != 'summary.json'}}
(root/'summary.json').write_text(json.dumps(summary, indent=2)+'\n')
print(json.dumps({k:v for k,v in summary.items() if k not in ('artifactHashes','verifiedPackageManifests')},indent=2))
