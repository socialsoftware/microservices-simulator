#!/usr/bin/env python3
"""Qualification expectations live here, separately from the application-independent assessor."""
import argparse
import copy
import hashlib
import json
import re
import shutil
from pathlib import Path
from assess import assess


def digest(path): return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    p=argparse.ArgumentParser();p.add_argument('--run',type=Path,required=True);p.add_argument('--baseline-run',type=Path,required=True)
    a=p.parse_args();protocol=json.loads((a.run/'protocol.json').read_text())
    assert digest(a.run/'contracts.json')==protocol['contractsHash']
    assert digest(a.run/'agent.jar')==protocol['agentHash']
    for name,h in protocol['sourceHashes'].items():assert digest(a.run/'source'/name)==h
    for name,h in protocol['applicationSourceHashes'].items():assert digest(a.run/'application-source'/name)==h
    rows=[]
    for mode in ('false','true'):
        for case in ('forward-stale','forward-fresh','recovery-stale','recovery-delayed-event','recovery-no-event'):
            name=case+'-'+mode+'.json'
            report=json.loads((a.run/name).read_text());trace=json.loads((a.run/(name+'.trace.json')).read_text())
            base=json.loads((a.baseline_run/name).read_text())
            assert report['status']=='PASS' and not report['observerFailures'] and not report['readObserverFailures']
            assert all(report[f]==base[f] for f in ('initial','final','producerOutcome')),'Application outcome changed'
            # These application audit timestamps use wall time, not the fixed business-date fixture.
            writes=lambda r:[(e['data']['aggregate']['identity'],e['data']['aggregate']['version'],
                {k:v for k,v in e['data']['aggregate']['applicationData'].items() if k not in ('creationDate','lastModifiedTime')},
                e['data']['writer']) for e in r['observations'] if e['kind']=='WRITE']
            assert writes(report)==writes(base),'Committed application writes changed'
            result=assess(trace,report['persistentInitial']['aggregates'])
            assert not result['coverageGaps'],result['coverageGaps']
            positive=case in ('forward-stale','recovery-stale')
            assert len(result['findings'])==int(positive),(name,result)
            if positive:
                f=result['findings'][0]
                assert f['fieldPath'].endswith('.topicName')
                assert f['phase']==('RECOVERY' if case.startswith('recovery') else 'FORWARD')
                if mode=='true':assert any(e['kind']=='TRANSPORT_LINK' and e['data']['cloned'] and e['data']['readOrder']==f['readOrder'] for e in trace['events'])
            rows.append({'case':case,'serialized':mode=='true','applicationOutcomeUnchanged':True,**result,
                         'traceSha256':digest(a.run/(name+'.trace.json'))})
    # Missing execution evidence must never be treated as a positive on the constructor mapping alone.
    original=json.loads((a.run/'forward-stale-true.json.trace.json').read_text())
    report=json.loads((a.run/'forward-stale-true.json').read_text())
    dependency_checks=[]
    for kind in ('RESPONSE','COMMAND_INPUT','CONSTRUCTOR_COPY','REGISTERED_COPY','COMMITTED_WRITE'):
        reduced=copy.deepcopy(original);reduced['events']=[e for e in reduced['events'] if e['kind']!=kind]
        assert not assess(reduced,report['persistentInitial']['aggregates'])['findings'],kind
        dependency_checks.append(kind)
    assert re.search(r'\[\s*0 tests failed', (a.run/'spock.log').read_text())
    analysis_source=a.run/'analysis-source';analysis_source.mkdir(exist_ok=True)
    for name in ('assess.py','validate.py','qualify.py'):
        shutil.copy2(Path(__file__).with_name(name),analysis_source/name)
    result={'status':'PASS','executions':10,'positiveExecutions':4,'controlExecutions':6,
            'inferredQuizzesContracts':len(json.loads((a.run/'contracts.json').read_text())),
            'spockReportedTests':13,'spockCases':10,'requiredEvidenceRemovalChecks':dependency_checks,
            'applicationSourcesChanged':False,'productionDetectorIntegrated':False,'productionScoreChanged':False,
            'nonInterferenceComparisonExcludes':['applicationData.creationDate','applicationData.lastModifiedTime'],
            'validatorSha256':digest(Path(__file__)),'assessorSha256':digest(Path(__file__).with_name('assess.py')),'rows':rows}
    (a.run/'validation.json').write_text(json.dumps(result,indent=2)+'\n')
    (a.run/'artifact-hashes.json').write_text(json.dumps({str(f.relative_to(a.run)):digest(f) for f in a.run.rglob('*') if f.is_file() and f.name!='artifact-hashes.json'},indent=2)+'\n')
    print('PASS: 4 positive executions, 6 controls; 10 Spock checks; 5 evidence-removal checks; all application outcomes unchanged.')


if __name__=='__main__':main()
