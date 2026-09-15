#!/usr/bin/env python3
"""Check planned/actual histories and independently predicted target-read exposure."""
import hashlib,json,sys
from pathlib import Path
out=Path(sys.argv[1]);read=lambda p:json.loads(p.read_text())
selection=read(out/'selection.json');rows=[];attempts=set()
for case in selection:
 folder=out/case['label'];r=read(folder/'execution-report.json');d=read(folder/'execution-report.saga-read-exposure.json');v=read(folder/'execution-report.impact-v2.json')
 assert (folder/'exit-code.txt').read_text().strip()=='0',case['label']
 assert r['faultScenarioId']==d['faultScenarioId']==case['scenario']
 assert r['workloadPlanId']==d['workloadPlanId']==case['workload']
 assert r['assignedVector']==case['vector']
 assert r['executionAttemptId']==d['executionAttemptId']==v['executionAttemptId']
 assert r['executionAttemptId'] not in attempts;attempts.add(r['executionAttemptId'])
 assert r['terminalStatus']==('SUCCESS' if case['control'] else 'PARTIAL_COMPENSATED'),(case['label'],r['terminalStatus'])
 assert r['prerequisiteSetup']['status']=='SUCCEEDED' and r['prerequisiteSetup']['emptyPendingEventBaseline']
 assert d['scheduleConformance']=='EXACT' and d['executionValidity']=='COMPLETE'
 assert v['assessmentStatus']=='COMPLETE'
 actual=r['actualActions'];assert len(actual)==len(case['actions'])
 for i,(a,p) in enumerate(zip(actual,case['actions'])):
  kind,key=('COMPENSATION','compensate') if 'compensate' in p else ('FORWARD','step')
  assert a['kind']==kind and a['sourceScheduledStepId']==p[key] and a['actualPosition']==i
  if kind=='COMPENSATION':assert a['status']=='COMPENSATED'
  elif a['runtimeStepName']=='updateQuizStep' and not case['control']:
   assert a['status']=='ASSIGNED_FAULT' and a['faultOrigin']=='ASSIGNED' and a['bodyOutcome']=='NOT_RUN'
  else:assert a['status']=='COMPLETED' and a['bodyOutcome']=='SUCCEEDED'
 lookup={x['sagaStep']:x['id'] for x in case['schedule']}
 read_id=lookup['findTournamentStep#0'];write_id=lookup['updateTournamentStep#0']
 b=next(i for i,a in enumerate(case['actions']) if a.get('step')==read_id)
 a=next(i for i,a in enumerate(case['actions']) if a.get('step')==write_id)
 recovery=next((i for i,x in enumerate(case['actions']) if x.get('compensate')==write_id),None)
 expected=not case['control'] and a<b<recovery
 calls=[c for c in d['calls'] if (c['observation'].get('reader') or {}).get('stepName')=='findTournamentStep']
 assert len(calls)==1
 call=calls[0];assert call['observation']['outcome']=='DELIVERED'
 assessment=next(a for a in d['assessments'] if a['callId']==call['id'])
 assert assessment['verdict']==('OBSERVED' if expected else 'NOT_OBSERVED'),(case['label'],assessment)
 assert d['observedExposureCount']==len(d['findings'])==int(expected)
 if expected:
  finding=d['findings'][0]
  assert finding['category']=='UPDATE' and finding['producedVersion']==call['observation']['version']
  assert {'startTime','endTime','numberOfQuestions'}<=set(finding['restoredAttributes'])
  assert finding['readerSagaId']=='p1' and finding['producerSagaId']=='p2'
 baseline=next(o['applicationData'] for o in v['baseline'] if o['identity']['aggregateType']=='SagaTournament')
 final=next(o['applicationData'] for o in v['finalState'] if o['identity']['aggregateType']=='SagaTournament')
 changed=sorted(k for k in baseline.keys()|final.keys() if baseline.get(k)!=final.get(k))
 assert v['completeScore']==(0 if case['control'] else 1)
 if not case['control']:
  assert changed==['tournamentTopics']
  assert all(t['topicCourseAggregateId']==1 for t in baseline['tournamentTopics'])
  assert all(t['topicCourseAggregateId'] is None for t in final['tournamentTopics'])
 target=next(p for p in r['participants'] if p['sagaInstanceId']=='p1')
 assert target['finalState']=='COMMITTED'
 rows.append(dict(label=case['label'],scenario=case['scenario'],workload=case['workload'],terminal=r['terminalStatus'],readPosition=case['position'],readVersion=call['observation']['version'],expectedExposure=int(expected),observedExposure=d['observedExposureCount'],targetReadVerdict=assessment['verdict'],targetReadReason=assessment['reason'],coverage=d['collectionCoverage'],gaps=d['gaps'],impactV2=v['completeScore'],finalChangedAttributes=changed))
summary=dict(executions=len(rows),controls=sum(c['control'] for c in selection),positives=sum(r['observedExposure'] for r in rows),results=rows)
(out/'validation.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps({k:v for k,v in summary.items() if k!='results'}))
