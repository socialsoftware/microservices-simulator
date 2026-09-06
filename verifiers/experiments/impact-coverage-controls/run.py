#!/usr/bin/env python3
"""Execute frozen selections using existing Docker build/run and assessment validators."""
import argparse
import collections
import concurrent.futures
import hashlib
import json
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
TARGET = ROOT / 'verifiers/target'
sys.path.insert(0, str(Path(__file__).resolve().parents[1]/'impact-v2-broader'))
from qualification import package
from validate_assessment import validate_reports


def save(path, value):
    path.write_text(json.dumps(value, indent=2)+'\n')


def run(cmd, log):
    with log.open('w') as stream:
        return subprocess.run(cmd, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT,
            env={**os.environ, 'MEDIUM_MEM_LIMIT':'4g', 'MEDIUM_MEM_RESERVATION':'1g', 'MEDIUM_CPUS':'5.0'}).returncode


def container(path):
    return '/reports/' + str(path.resolve().relative_to(TARGET.resolve()))


def compose(env, script):
    cmd=['docker','compose','-p','microservices-simulator','-f',str(ROOT/'docker-compose.yml'),
         'run','--rm','--no-deps','--pull','never','-T']
    for key,value in env.items(): cmd += ['-e',key+'='+str(value)]
    return cmd+['scenario-executor','/verifiers/experiments/impact-v2-broader/'+script]


def main(args):
    out=args.output.resolve()
    if out.exists() and any(out.iterdir()): raise ValueError('Refusing nonempty run output')
    out.mkdir(parents=True,exist_ok=True)
    save(out/'execution-environment.json', {'MEDIUM_MEM_LIMIT':'4g', 'MEDIUM_MEM_RESERVATION':'1g',
         'MEDIUM_CPUS':'5.0', 'JAVA_TOOL_OPTIONS':'-Xmx1536m -XX:MaxMetaspaceSize=512m', 'maxConcurrentAttempts':2})
    manifest=args.manifest.resolve(); data=package(manifest)
    selection_paths=[args.selection/'benchmark-selection.json',args.selection/'broader-selection.json']
    selections=[json.loads(p.read_text()) for p in selection_paths]
    if any(s['packageManifestSha256'] != data['hashes']['manifest'] for s in selections):
        raise ValueError('Selection/package mismatch')
    rows=selections[0]['rows']+selections[1]['rows']
    if args.only_pair:
        requested=set(args.only_pair)
        rows=[r for r in rows if r.get('pairId') in requested]
        for pair in requested:
            selected=[r for r in rows if r.get('pairId')==pair]
            if len(selected)!=2 or {r['role'] for r in selected}!={'control','late-fault'}:
                raise ValueError('Requested pair is missing or ambiguous: '+pair)
    if len({(r['cohort'],r['caseId']) for r in rows})!=len(rows):raise ValueError('Duplicate case')
    save(out/'plan.json',{'packageManifest':str(manifest),'packageHashes':data['hashes'],
                         'selectionHashes':{str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in selection_paths},
                         'rows':rows})
    source_hashes={}
    for source_scope,frozen_scope in [('simulator','simulator'),('verifiers','verifiers'),('applications/quizzes','quizzes')]:
        for path in (ROOT/source_scope).rglob('*'):
            rel=path.relative_to(ROOT/source_scope)
            if not path.is_file() or any(x in rel.parts for x in ['target','.git','logs','experiments','__pycache__']) or path.suffix=='.pyc':continue
            source_hashes[str(Path(frozen_scope)/rel)]=hashlib.sha256(path.read_bytes()).hexdigest()
    save(out/'source-hashes.json',source_hashes)
    build=out/'prepared-build'
    env={'BUILD_OUTPUT_DIR':container(build),'JAVA_TOOL_OPTIONS':'-Xmx1536m -XX:MaxMetaspaceSize=512m'}
    cmd=compose(env,'prepare-build.sh');save(out/'prepare-command.json',cmd)
    if run(cmd,out/'prepare-build.log')!=0:raise RuntimeError('Frozen Docker build failed')
    for rel,expected in source_hashes.items():
        if hashlib.sha256((build/'source'/rel).read_bytes()).hexdigest()!=expected:
            raise ValueError('Source changed during build: '+rel)
    print('Frozen build ready; attempts:',len(rows),flush=True)
    def attempt(row):
        case=out/row['cohort']/'attempts'/row['caseId'];case.mkdir(parents=True)
        cmd=compose({**env,'RUNNER_KIND':'generic','PACKAGE_PATH':container(manifest),
            'FAULT_SCENARIO_ID':row['faultScenarioId'],'EXECUTION_OUTPUT_PATH':container(case/'execution.json'),
            'IMPACT_OUTPUT_PATH':container(case/'impact-v1.json')},'run-prepared.sh')
        save(case/'command.json',cmd)
        code=run(cmd,case/'docker.log');(case/'process-exit-code').write_text(str(code)+'\n')
        paths=[case/n for n in ['execution.json','impact-v1.json','execution.impact-v2.json']]
        if not all(p.exists() for p in paths):
            return {**row,'processExitCode':code,'missingReports':[p.name for p in paths if not p.exists()]}
        e,v1,v2=[json.loads(p.read_text()) for p in paths]
        status=validate_reports(e,v1,v2,row['workloadId'],row['faultScenarioId'],row['faultVector'])
        if e['packageManifestPath']!=container(manifest):raise ValueError('Attempt used another manifest')
        result={**row,'processExitCode':code,'executionAttemptId':e['executionAttemptId'],
                'terminalStatus':e['terminalStatus'],'conformance':e['scheduleConformance'],
                'assessmentStatus':status,'score':v2['completeScore'],'observedCount':v2['observedAffectedObjectCount'],
                'blockers':e['blockers'],'coverageGaps':v2['coverageGaps'],
                'implicitRollbacks':[a['runtimeStepName'] for a in e['actualActions'] if any(
                    sub['kind']=='IMPLICIT_SAGA_ROLLBACK' and sub['status']=='SUCCEEDED' for sub in a['recoverySubOutcomes'])]}
        save(case/'summary.json',result)
        print(row['cohort'],row['caseId'][:55],row['faultVector'],e['terminalStatus'],status,v2['completeScore'],flush=True)
        return result
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        results=list(pool.map(attempt,rows))
    attempt_ids=[r['executionAttemptId'] for r in results if 'executionAttemptId' in r]
    if len(attempt_ids)!=len(set(attempt_ids)):raise ValueError('Duplicate execution attempt identity')
    if package(manifest)['hashes']!=data['hashes']:raise ValueError('Package mutated during execution')
    for p in selection_paths:
        expected=json.loads((out/'plan.json').read_text())['selectionHashes'][str(p)]
        if hashlib.sha256(p.read_bytes()).hexdigest()!=expected:raise ValueError('Selection mutated during execution')
    save(out/'summary.json',{'validation':'PASS' if all('missingReports' not in r for r in results) else 'INCOMPLETE_ARTIFACTS',
        'statusesByCohort':{c:dict(collections.Counter(r.get('assessmentStatus','MISSING') for r in results if r['cohort']==c))
                           for c in ['benchmark','broader']},'results':results})


if __name__=='__main__':
    p=argparse.ArgumentParser()
    p.add_argument('--manifest',type=Path,required=True)
    p.add_argument('--selection',type=Path,required=True)
    p.add_argument('--output',type=Path,required=True)
    p.add_argument('--only-pair',action='append',help='Run both roles of this explicit broader pair; omit benchmark. Use a new output directory.')
    main(p.parse_args())
