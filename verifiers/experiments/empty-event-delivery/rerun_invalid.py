#!/usr/bin/env python3
"""Repeat the frozen forty v5 empty-receiver failures with the qualified v6 build."""
import argparse
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import subprocess
import sys
import time

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path.insert(0, str(HERE.parent / 'batch-execution'))
import run as batch

ORIGINAL = ROOT / 'verifiers/target/space-map'
QUALIFIED = ROOT / 'verifiers/target/empty-event-delivery/run-01'


def integrity(selection):
    assert batch.package(Path(selection['manifest']))['hashes'] == selection['packageHashes']
    for row in batch.read(QUALIFIED / 'production-hashes.json'):
        assert batch.digest(ROOT / row['path']) == row['sha256'], row['path']
    for path, sha in batch.read(QUALIFIED / 'build-provenance.json')['files'].items():
        assert batch.digest(QUALIFIED / path) == sha, path
    assert image() == selection['image']
    for path, sha in selection['evidenceHashes'].items():
        assert batch.digest(ROOT / path) == sha, path


def image():
    return subprocess.check_output(['docker', 'image', 'inspect', 'scenario-executor:latest',
                                    '--format', '{{.Id}}'], text=True).strip()


def freeze(output):
    plan = batch.read(ORIGINAL / 'prepared/experiment.json')
    records = batch.read(ORIGINAL / 'run-01/full-report-analysis.json')['records']
    previous = {r['caseId']: r for r in batch.read(ORIGINAL / 'run-01/discovery.json')}
    selected = [r for r in records if r['repetition'] == 1 and r['assessmentStatus'] == 'INVALID']
    assert len(selected) == 40
    assert {r['hardStopReason'] for r in selected} == {'SELECTED_SUBSCRIBER_NOT_FOUND'}
    ids = {r['caseId'] for r in selected}
    rows = [r for r in plan['rows'] if r['caseId'] in ids]
    assert len(rows) == 40
    evidence_hashes = {}
    for r in selected:
        for name, sha in r['reportHashes'].items():
            path = Path(previous[r['caseId']]['directory']) / name
            assert batch.digest(path) == sha
            evidence_hashes[str(path.relative_to(ROOT))] = sha
    for path in [ORIGINAL / 'prepared/experiment.json', ORIGINAL / 'run-01/full-report-analysis.json',
                 QUALIFIED / 'production-hashes.json', QUALIFIED / 'build-provenance.json',
                 HERE / 'rerun_invalid.py', HERE.parent / 'batch-execution/run.py',
                 HERE.parent / 'impact-v2-broader/run-prepared.sh']:
        evidence_hashes[str(path.relative_to(ROOT))] = batch.digest(path)
    selection = dict(manifest=plan['manifest'], packageHashes=plan['packageHashes'],
                     build=str(QUALIFIED / 'prepared-build'), image=image(),
                     revision=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
                     rows=rows, previous=selected, evidenceHashes=evidence_hashes,
                     concurrency=2, timeoutSeconds=180, repetitions=1,
                     policy='All forty original INVALID discovery IDs; unchanged retained inputs/actions/package; current qualified production build; no retries or expected scores.')
    integrity(selection)
    output.mkdir(parents=True, exist_ok=False)
    batch.save(output / 'selection.json', selection)
    return selection


def summarize(output):
    selection = batch.read(output / 'selection.json')
    integrity(selection)
    old = {r['caseId']: r for r in selection['previous']}
    rows = {r['caseId']: r for r in selection['rows']}
    results = batch.read(output / 'attempts.json')
    records, ids = [], set()
    for result in results:
        directory = Path(result['directory'])
        record = dict(caseId=result['caseId'], actionModelKey=old[result['caseId']]['actionModelKey'],
                      processStatus=result['status'], exitCode=result.get('exitCode'),
                      wallSeconds=result['wallSeconds'], reportHashes=result['reportHashes'],
                      oldTerminal=old[result['caseId']]['terminalStatus'],
                      oldHardStop=old[result['caseId']]['hardStopReason'])
        for name, sha in result['reportHashes'].items():
            assert batch.digest(directory / name) == sha
        if all((directory / name).is_file() for name in batch.REPORTS):
            e, v1, v2 = batch.reports(directory, rows[result['caseId']], Path(selection['manifest']))
            assert e['schemaVersion'].endswith('.v6')
            assert e['executionAttemptId'] not in ids
            ids.add(e['executionAttemptId'])
            status, score = batch.assessment_result(e,v2)
            assert result.get('exitCode') == 0 or score is None
            empty = [a for a in e['actualActions'] if a['status']=='NO_ELIGIBLE_SUBSCRIBER']
            for a in empty:
                assert a['bodyOutcome']=='NOT_RUN' and a['eventEvidence']['subscriberAggregateId'] is None
            record.update(terminal=e['terminalStatus'], conformance=e['scheduleConformance'],
                          assessment=v2['assessmentStatus'], score=score, hardStop=e.get('hardStopReason'),
                          sourceSetup=(e.get('sourceSetup') or {}).get('status'),
                          emptySelections=len(empty), deliveries=len(v2['eventDeliveries']),
                          semantic=batch.semantic(e,v1,v2),
                          unassignedFailures=[{k:a.get(k) for k in ['runtimeStepName','exceptionClass','exceptionMessage']}
                            for a in e['actualActions'] if a.get('faultOrigin')=='UNASSIGNED_RUNTIME'])
        records.append(record)
    groups={}
    for r in records: groups.setdefault(r['actionModelKey'],[]).append(r)
    agreement=all(all(r.get('semantic')==rs[0].get('semantic') for r in rs) for rs in groups.values())
    summary=dict(attempts=len(records),distinctSequences=len(groups),validatedAttemptIds=len(ids),
                 processStatuses=dict(Counter(r['processStatus'] for r in records)),
                 assessments=dict(Counter(r.get('assessment','NO_REPORT') for r in records)),
                 terminals=dict(Counter(r.get('terminal','NO_REPORT') for r in records)),
                 conformance=dict(Counter(r.get('conformance','NO_REPORT') for r in records)),
                 scores=dict(Counter(str(r.get('score')) for r in records)),
                 hardStops=dict(Counter(r['hardStop'] for r in records if r.get('hardStop'))),
                 emptySelections=sum(r.get('emptySelections',0) for r in records),
                 actualDeliveries=sum(r.get('deliveries',0) for r in records),
                 attemptsWithUnassignedFailure=sum(bool(r.get('unassignedFailures')) for r in records),
                 equalSequenceSemanticAgreement=agreement, integrity='PASS',records=records)
    assert len(records)==40
    batch.save(output/'comparison.json',summary)
    print({k:v for k,v in summary.items() if k!='records'},flush=True)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['run','summarize'])
    parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args(); output=args.output.resolve()
    if args.action=='summarize': return summarize(output)
    selection=freeze(output)
    started=time.monotonic()
    with ThreadPoolExecutor(max_workers=2) as pool:
        futures=[pool.submit(batch.attempt,output,selection,row,1,2,180,mode='assessment') for row in selection['rows']]
        results=[f.result() for f in futures]
    batch.save(output/'attempts.json',results)
    batch.save(output/'timing.json',dict(wallSeconds=time.monotonic()-started))
    summarize(output)


if __name__=='__main__': main()
