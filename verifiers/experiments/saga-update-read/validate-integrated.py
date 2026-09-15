#!/usr/bin/env python3
"""Check production diagnostic output against independently validated application witnesses."""
import importlib.util,json,hashlib,sys
from pathlib import Path
HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('application_checks',HERE/'validate.py')
checks=importlib.util.module_from_spec(spec);spec.loader.exec_module(checks)
def read(p):return json.loads(p.read_text())
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def need(value,message):
    if not value:raise AssertionError(message)
def main():
    root=Path(sys.argv[1]);manifest=read(root/'artifact-hashes.json')
    for path,h in manifest.items():need(sha(root/path)==h,'artifact mismatch: '+path)
    need(read(root/'exit.json')['returnCode']==0,'runner did not complete')
    rows=[]
    for mode in (False,True):
        for case in checks.CASES:
            name=f'{case}-{str(mode).lower()}'
            app=read(root/(name+'.json'));expected=checks.verify(app)
            d=read(root/(name+'.saga-read-exposure.json'))
            need(d==app['productionDiagnostic'],'sidecar differs from embedded report')
            need(d['schemaVersion']=='microservices-simulator.saga-read-exposure.v2','schema')
            need(d['executionValidity']=='COMPLETE','incomplete execution')
            need(d['collectionCoverage']=='COMPLETE_WITHIN_SCOPE','unexpected observation gap: '+str(d['gaps']))
            need(d['observedExposureCount']==expected['experimentalWitnessCount'],'detector/application disagreement')
            calls={c['id']:c for c in d['calls']}
            b=[a for a in d['assessments'] if (calls[a['callId']]['observation'].get('reader') or {}).get('sagaInstanceId')=='B']
            need(len(b)==1,'B assessment ambiguity')
            need(b[0]['verdict']==('OBSERVED' if case=='fault-between' else 'NOT_OBSERVED'),'B verdict')
            if case=='fault-between':
                f=checks.one(d['findings'],'update finding')
                need(f['category']=='UPDATE','finding category')
                need(f['producedVersion']==expected['forwardVersion'] and f['recoveryVersion']==expected['finalVersion'],'version chain')
                need(f['producerSagaId']=='A' and f['readerSagaId']=='B','actor attribution')
                need(f['restoredAttributes']==['endTime','numberOfQuestions','startTime'],'restored attributes')
                need(f['notRestoredAttributes']==['lastModifiedTime','tournamentTopics'],'partial restoration')
                need(not any(f.get(k) is not None for k in ('createdVersion','deletedVersion','creationWriteId','deletionWriteId')),'misleading creation fields for update')
            else:need(not d['findings'],'negative control has finding')
            # Sidecar carries fingerprints, not raw business values or a score.
            encoded=json.dumps(d)
            need('2030-01-01' not in encoded and 'applicationData' not in encoded and 'retainedDto' not in encoded,'raw business payload leak')
            rows.append(dict(**expected,productionCount=d['observedExposureCount'],verdict=b[0]['verdict'],reason=b[0]['reason']))
    regressions=[]
    for case,count in [('reader-only',1),('early-compensation',0)]:
        d=read(root/f'creation-{case}/execution-report.saga-read-exposure.json')
        need(d['schemaVersion'].endswith('.v2') and d['observedExposureCount']==count,'creation regression '+case)
        need(all(f['category']=='CREATION' for f in d['findings']),'creation classification')
        regressions.append(dict(caseId=case,observedExposureCount=count))
    ordinary=root/'ordinary'
    execution=read(ordinary/'execution-report.json');d=read(ordinary/'execution-report.saga-read-exposure.json')
    need(execution['terminalStatus']=='SUCCESS','ordinary executor failed')
    need(d['schemaVersion'].endswith('.v2') and d['observedExposureCount']==0 and d['executionValidity']=='COMPLETE','ordinary diagnostic persistence')
    summary=dict(status='PASS',updateHistories=4,updateExecutions=8,rows=rows,creationRegressions=regressions,
        ordinary=dict(terminalStatus=execution['terminalStatus'],schemaVersion=d['schemaVersion'],observedExposureCount=d['observedExposureCount']),
        productionScoringModified=False,validatorSha256=sha(Path(__file__)),applicationValidatorSha256=sha(HERE/'validate.py'))
    (root/'integrated-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    print('PASS: 8 production update diagnostic runs, 2 creation regressions, 1 ordinary executor sidecar control')
if __name__=='__main__':main()
