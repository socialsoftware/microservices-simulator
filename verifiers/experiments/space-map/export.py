#!/usr/bin/env python3
"""Export compact evidence without copying runtime snapshots or changing results."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import sys

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent/'batch-execution'))
import run as batch


def fingerprint(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(',', ':')).encode()).hexdigest()


def export(prepared, run, evidence):
    evidence.mkdir(parents=True)
    plan = batch.read(prepared/'experiment.json')
    analysis = batch.read(run/'full-report-analysis.json')
    data = batch.package(Path(plan['manifest']))['records']
    inputs = {r['id']: r for r in data['inputs']}
    setups = {r['id']: r for r in data['setups']}
    scopes = []
    for s in plan['scopes']:
        if s['status'] != 'SELECTED':
            scopes.append(s)
            continue
        f = {'setup': setups[s['workload']['setup']],
             'inputs': [inputs[p['input']] for p in s['workload']['participants']]}
        scopes.append({**{k: v for k, v in s.items() if k != 'generatedCandidates'},
                       'generatedCandidates': [{**{k: r[k] for k in ('id', 'faultVector')},
                                                'actionModelKey': fingerprint([s['workload']['id'], r['faultVector'], r['actions']])}
                                               for r in s['generatedCandidates']],
                       'actionModels': {fingerprint([s['workload']['id'], r['faultVector'], r['actions']]):
                                        {'faultVector': r['faultVector'], 'actions': r['actions']}
                                        for r in s['generatedCandidates']},
                       'sourceInputs': [{k: r[k] for k in ('id', 'saga', 'source', 'aggregateKeyEvidence') if k in r} for r in f['inputs']],
                       'setup': {**{k: v for k, v in f['setup'].items() if k != 'actions'},
                                 'actions': [{'id': a['id'], 'call': a['call']} for a in f['setup']['actions']]}})
    batch.save(evidence/'selection.json', {'seed': plan['seed'], 'frozenAt': plan['frozenAt'],
        'structuralPlanSha256': plan['structuralPlanSha256'], 'eligibleStrata': plan['eligibleStrata'],
        'scopes': scopes, 'rows': plan['rows'], 'maxVectors': plan['maxVectors'],
        'maxScenarios': plan['maxScenarios'], 'recoveryCap': plan['recoveryCap'],
        'timeoutSeconds': plan['timeoutSeconds'], 'maxConcurrency': plan['maxConcurrency']})
    batch.save(evidence/'generation-accounting.json', plan['generationAccounting'])
    batch.save(evidence/'assessment-summary.json', {k: v for k, v in analysis.items() if k != 'records'})
    for name in ('summary.json', 'full-attempts.csv', 'repeat-plan.json'):
        shutil.copy2(run/name, evidence/name)
    lines = ['# Bounded space map', '',
             '| Group | Attempted IDs | Distinct action sequences attempted / generated | COMPLETE IDs | Invalid IDs | Positive IDs / action sequences |',
             '|---|---:|---:|---:|---:|---:|']
    for group in analysis['groups']:
        lines.append(f'| {group["caseGroup"]} | {group["discoveryAttempts"]} | '
                     f'{group["selectedActionModels"]} / {group["generatedActionModels"]} | '
                     f'{group["executionStatuses"].get("COMPLETE", 0)} | '
                     f'{group["executionStatuses"].get("EXECUTION_INVALID", 0)} | '
                     f'{len(group["positiveScenarioIds"])} / {group["positiveActionModels"]} |')
    lines += ['', 'Discovery only; representative repetitions are separate. Invalid assessments have null scores.',
              'Equal workload/vector/ordered actions under different persisted IDs count as one action sequence.',
              'Generated counts cover requested vectors only; w08 requested 24 of 72 canonical vectors.',
              'Positive scenarios are not distinct defects. See assessment-summary.json for lifecycle evidence and repeatability.']
    (evidence/'map.md').write_text('\n'.join(lines)+'\n')
    proof = batch.read(run/'proof.json')
    dependencies = proof.pop('dependencyHashes')
    image_start = (prepared.parent/'image-id-start.txt').read_text().strip()
    image_end = (prepared.parent/'image-id-end.txt').read_text().strip()
    if image_start != image_end:
        raise ValueError('Image changed during campaign')
    checkout = batch.source_hashes()
    end_drift = sorted(p for p in checkout.keys() | plan['sourceHashes'].keys()
                       if checkout.get(p) != plan['sourceHashes'].get(p))
    proof.update(rawRoot=str(prepared.parent), packageHashes=plan['packageHashes'],
                 experimentSha256=batch.digest(prepared/'experiment.json'),
                 sourceFileCount=len(plan['sourceHashes']), sourceHashesFingerprint=fingerprint(plan['sourceHashes']),
                 buildFileCount=len(plan['buildHashes']), buildHashesFingerprint=fingerprint(plan['buildHashes']),
                 checkoutSourceDriftAtFreeze=plan['checkoutSourceDrift'], checkoutSourceDriftAtExport=end_drift,
                 dependencyCount=len(dependencies), dependencyHashesFingerprint=fingerprint(dependencies),
                 imageIdAtStartAndEnd=image_start, preparationSeconds=plan['preparationSeconds'],
                 pythonVersionAtExport=sys.version, containerLimits=batch.LIMITS,
                 maxExecutionConcurrency=plan['maxConcurrency'], timeoutSeconds=plan['timeoutSeconds'],
                 generationResult=batch.read(prepared.parent/'generation-result.json'),
                 postprocessingScripts={p.name: batch.digest(p) for p in [HERE/'analyze.py', HERE/'export.py']})
    batch.save(evidence/'proof.json', proof)
    hashes = {}
    for path in sorted(prepared.parent.rglob('*')):
        if path.is_file() and path.suffix in ('.json', '.jsonl', '.csv', '.log', '.sh'):
            hashes[str(path.relative_to(prepared.parent))] = batch.digest(path)
    batch.save(evidence/'artifact-hashes.json', hashes)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    for field in ('prepared', 'run', 'evidence'):
        parser.add_argument('--'+field, required=True, type=Path)
    args = parser.parse_args()
    export(args.prepared.resolve(), args.run.resolve(), args.evidence.resolve())
