#!/usr/bin/env python3
"""Bounded native functional comparison; frozen runtime, matched source setup, no retries."""
import concurrent.futures, hashlib, json, pathlib, shutil, subprocess, sys, time
ROOT = pathlib.Path(__file__).resolve().parents[3]
OUT = pathlib.Path(sys.argv[1]).resolve()
JAVA = '/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/bin/java'
def read(p): return json.loads(p.read_text())
def digest(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def save(p, v): p.write_text(json.dumps(v, indent=2))
def rows(p): return [json.loads(s) for s in p.read_text().splitlines() if s.strip()]
config_path = ROOT/'verifiers/target/full-map-5184-2026-09-17/config.json'
cfg = read(config_path)['runtime']
for name,h in cfg['hashes'].items():
    assert digest(pathlib.Path(name)) == h, name
overlay_root=ROOT/'verifiers/target/event-read-attribution-2026-09-19'
for name,h in read(overlay_root/'final-build-receipt.json')['classes'].items():
    assert digest(pathlib.Path(name)) == h, name
cp = str(overlay_root/'runtime-overlay-final')+':'+cfg['classpath'].replace('/reports',str(ROOT/'verifiers/target'))
try:
    docker=subprocess.run(['docker','info','--format','{{.ServerVersion}}'],capture_output=True,text=True,timeout=10)
    docker_status={'exit':docker.returncode,'output':docker.stdout[:300], 'error':docker.stderr[:300]}
except (subprocess.TimeoutExpired,FileNotFoundError):docker_status={'status':'UNAVAILABLE_WITHIN_10S'}
save(OUT/'runtime-receipt.json',{'config':str(config_path),'sha256':digest(config_path),'overlayReceiptHash':digest(overlay_root/'final-build-receipt.json'),'docker':docker_status,'execution':'native-java21 functional comparison, not Docker parity/performance'})

def assess(attempt):
    ex=read(attempt/'execution-report.json');im=read(attempt/'execution-report.impact-v2.json');rd=read(attempt/'execution-report.saga-read-exposure.json');lost=read(attempt/'execution-report-lost-copied-updates.json')
    for report in (im,rd,lost):
        for key in ('executionAttemptId','workloadPlanId','faultScenarioId'):assert report[key]==ex[key],key
    cs={c['category']:c['positiveObjectCount'] if c['coverageStatus']=='COMPLETE' and not c['unknownReasons'] else None for c in im['categoryResults']}
    cs['COMPENSATED_READ_EXPOSURE']=rd['observedExposureCount'] if rd['executionValidity']=='COMPLETE' and rd['collectionCoverage']=='COMPLETE_WITHIN_SCOPE' and not rd['gaps'] else None
    cs['LOST_COPIED_UPDATE']=lost['count'] if lost['validity']=='COMPLETE' and lost['coverage']=='COMPLETE_WITHIN_SCOPE' and not lost['coverageGaps'] else None
    return {'terminalStatus':ex['terminalStatus'],'conformance':ex['scheduleConformance'],'counts':cs,'score':sum(cs.values()) if all(x is not None for x in cs.values()) else None}

def run(pkg, scenario, index):
    attempt=pkg.parent/'run'/f'attempt-{index:03d}'
    assert not attempt.exists(), 'No implicit retry'
    attempt.mkdir(parents=True)
    if shutil.disk_usage(OUT).free < 2*1024**3: raise RuntimeError('Disk safety margin')
    options=cfg['javaOptions']+['-XX:ActiveProcessorCount=2','-javaagent:'+cfg['lostCopiedUpdateAgent']['path'],'-Dsimulator.copied-update.contracts='+str(pkg/'copy-contracts.json'),'-Dsimulator.copied-update.source-root='+cfg['lostCopiedUpdateSourceRoot']]
    command=[JAVA,*options,'-cp',cp,cfg['executorMain'],*cfg['executorArgs'],'--microservices.simulator.lost-copied-update.enabled=true','--package-path',str(pkg/'scenario-catalog-manifest.json'),'--fault-scenario-id',scenario['id'],'--output-path',str(attempt/'execution-report.json'),'--impact-output-path',str(attempt/'execution-report.impact.json')]
    save(attempt/'command.json',command);start=time.time()
    with (attempt/'execution.log').open('w') as log:
        try:code=subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,timeout=180).returncode
        except subprocess.TimeoutExpired:code='TIMEOUT'
    receipt={'scenario':scenario,'exitCode':code,'seconds':time.time()-start,'reportHashes':{p.name:digest(p) for p in attempt.glob('execution-report*.json')}}
    save(attempt/'receipt.json',receipt)
    result={'scenario':scenario,'attempt':str(attempt),'exitCode':code}
    if code==0:result.update(assess(attempt))
    print(pkg.parent.name,index,result.get('score'),code,flush=True)
    return result

summary=[]
for label in ['GetCourseExecutionById-FindTournament','GetCourseExecutionById-UpdateTournament']:
    pkg=OUT/label/'BRUTE_FORCE'
    if not pkg.exists(): continue
    ws=rows(pkg/'workloads.jsonl');ss=rows(pkg/'setups.jsonl')
    assert all(s['actions']==ss[0]['actions'] for s in ss), 'Unequal preparation actions'
    pruned=rows(pkg.parent/'INTERACTION_PRUNED/workloads.jsonl')
    assert {w['id'] for w in pruned}=={w['id'] for w in ws if len(w['participants'])==1}
    assert all(a['kind']=='step' for w in ws for a in w['schedule'])
    # Bindings may differ only by removal of the other participant's argument bindings.
    bysetup={s['id']:s for s in ss}; pair=next(w for w in ws if len(w['participants'])==2)
    pair_bindings=bysetup[pair['setup']]['bindings']
    for w in ws:
        assert all(b in pair_bindings for b in bysetup[w['setup']]['bindings']), 'Argument binding mismatch'
    hashes={p.name:digest(p) for p in pkg.iterdir() if p.is_file()}
    scenarios=sorted(rows(pkg/'fault-scenarios.jsonl'),key=lambda s:('1' in s['faultVector'],s['id']))
    save(pkg.parent/'runtime-inventory.json',{'packageHashes':hashes,'scenarios':scenarios,'setupActionsIdentical':True})
    controls=[(i,s) for i,s in enumerate(scenarios) if '1' not in s['faultVector']]
    faulted=[(i,s) for i,s in enumerate(scenarios) if '1' in s['faultVector']]
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        results=list(pool.map(lambda x:run(pkg,x[1],x[0]),controls))
        healthy=all(r.get('terminalStatus')=='SUCCESS' and r.get('conformance')=='EXACT' and r.get('score')==0 for r in results)
        if healthy:results.extend(pool.map(lambda x:run(pkg,x[1],x[0]),faulted))
    assert hashes=={p.name:digest(p) for p in pkg.iterdir() if p.is_file()}
    summary.append({'label':label,'healthyControls':healthy,'results':results})
    save(OUT/'runtime-summary.json',summary)
save(OUT/'runtime-complete.json',{'groups':len(summary),'attempts':sum(len(x['results']) for x in summary)})
