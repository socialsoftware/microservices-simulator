#!/usr/bin/env python3
"""Fixed matched event-order experiment using retained packages and prepared executor."""
import argparse
import importlib.util
from pathlib import Path
import subprocess
import sys
import time

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
spec=importlib.util.spec_from_file_location('prepared_batch',HERE.parent/'batch-execution/run.py')
batch=importlib.util.module_from_spec(spec);spec.loader.exec_module(batch)
QUALIFIED=ROOT/'verifiers/target/empty-event-delivery/run-01'
MANIFEST=ROOT/'verifiers/target/space-map/prepared/package/scenario-catalog-manifest.json'
CASES=[
 ('tournament-late','tournament','late','a5788c8dd3d1d6ec91ba2bb80c5fed412326312b7c31c675199b4af94efca305','s4'),
 ('tournament-early','tournament','early','f90dc4859b834458e4675cce23d11a3048085b1e700edbe6e769bdd4efacec27','s4'),
 ('removed-student-late','removed-student','late','0ec796ae9073794895f337d9982816ca2907eb16b2dedb9207694fabf1ca9341','s3'),
 ('removed-student-early','removed-student','early','ed088c072d615ee4948058b4513c4a8855bec831ab9955671dc096473e069605','s3')]


def matched(early,late,event='e1'):
    """Validate one moved event; preserve all other actions and workload declarations."""
    if {k:v for k,v in early.items() if k not in ('id','schedule')} != {k:v for k,v in late.items() if k not in ('id','schedule')}:
        raise ValueError('Different participants, inputs, setup or interactions')
    for w in (early,late):
        ids=[a['id'] for a in w['schedule']]
        if len(ids)!=len(set(ids)) or ids.count(event)!=1: raise ValueError('Ambiguous moved action')
    if [a for a in early['schedule'] if a['id']!=event] != [a for a in late['schedule'] if a['id']!=event]:
        raise ValueError('Other scheduled actions changed')
    a=next(a for a in early['schedule'] if a['id']==event)
    b=next(a for a in late['schedule'] if a['id']==event)
    if a!=b or a['kind']!='event': raise ValueError('Moved action is not the same event')
    if early['schedule']==late['schedule']: raise ValueError('Event did not move')
    return True


def verify(selection):
    if batch.package(MANIFEST)['hashes']!=selection['packageHashes']:raise ValueError('Package drift')
    for r in batch.read(QUALIFIED/'production-hashes.json'):
        if batch.digest(ROOT/r['path'])!=r['sha256']:raise ValueError('Production drift')
    for p,h in batch.read(QUALIFIED/'build-provenance.json')['files'].items():
        if batch.digest(QUALIFIED/p)!=h:raise ValueError('Build/dependency drift')
    current=subprocess.check_output(['docker','image','inspect','scenario-executor:latest','--format','{{.Id}}'],text=True).strip()
    if current!=selection['image']:raise ValueError('Image drift')
    for p,h in selection['toolHashes'].items():
        if batch.digest(ROOT/p)!=h:raise ValueError('Runner drift')


def freeze(out):
    data=batch.package(MANIFEST); ws={w['id']:w for w in data['records']['workloads']}; rows=[]
    for name,pair,order,wid,target in CASES:
        scenarios=[s for s in data['records']['faultScenarios'] if s['workload']==wid and set(s['faultVector'])=={'0'}]
        if not scenarios:raise ValueError('Missing no-fault scenario')
        chosen=min(scenarios,key=lambda s:s['id']);w=ws[wid]
        expected=[{'step' if a['kind']=='step' else 'event':a['id']} for a in w['schedule']]
        if chosen['actions']!=expected:raise ValueError('Scenario does not follow normal schedule')
        rows.append(dict(caseId=name,pair=pair,order=order,workloadId=wid,faultScenarioId=chosen['id'],faultVector=chosen['faultVector'],targetStep=target,workload=w))
    for pair in ['tournament','removed-student']:
        rr={r['order']:r for r in rows if r['pair']==pair};matched(rr['early']['workload'],rr['late']['workload'])
        for order,r in rr.items():
            ids=[a['id'] for a in r['workload']['schedule']]
            if (ids.index('e1')<ids.index(r['targetStep']))!=(order=='early'):raise ValueError('Wrong relative placement')
    selection=dict(manifest=str(MANIFEST),build=str(QUALIFIED/'prepared-build'),packageHashes=data['hashes'],rows=rows,
        revision=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
        image=batch.read(QUALIFIED/'build-provenance.json')['image'],
        toolHashes={str(p.relative_to(ROOT)):batch.digest(p) for p in [Path(__file__),HERE.parent/'batch-execution/run.py',HERE.parent/'impact-v2-broader/run-prepared.sh']},
        repetitions=2,concurrency=1,timeoutSeconds=180,
        design='Two matched pairs, no assigned faults; only e1 crosses target step; repeat in reverse launch order; retained source-derived setup with relative date recipes.')
    verify(selection);out.mkdir(parents=True,exist_ok=False);batch.save(out/'selection.json',selection)
    return selection


def main():
    p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);args=p.parse_args();out=args.output.resolve()
    selection=freeze(out);results=[];start=time.monotonic()
    for repetition in [1,2]:
        rows=selection['rows'] if repetition==1 else list(reversed(selection['rows']))
        for row in rows:
            results.append(batch.attempt(out,selection,row,repetition,1,180,mode='assessment'))
            batch.save(out/'attempts.json',results)
    verify(selection);batch.save(out/'proof.json',dict(integrity='PASS',attempts=len(results),wallSeconds=time.monotonic()-start))


if __name__=='__main__':main()
