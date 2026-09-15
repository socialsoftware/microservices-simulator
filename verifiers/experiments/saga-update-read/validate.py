#!/usr/bin/env python3
"""Verify the fixed experimental matrix; this is not the production anomaly assessor."""
import hashlib, json, sys
from datetime import datetime
from pathlib import Path
CASES=('success-between','fault-between','fault-before','fault-after')
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def need(value,message):
    if not value:raise AssertionError(message)
def one(values,label):
    need(len(values)==1,f'{label}: expected one, got {len(values)}');return values[0]
def instant(value):return datetime.fromisoformat(value.replace('Z','+00:00')).replace(tzinfo=None)
def aggregate(batch,identity):
    return one([a for a in batch['aggregates'] if a['identity']==identity],'persistent aggregate')
def verify(r):
    case=r['caseId'];fault=case!='success-between'
    need(r['status']=='PASS' and all(c['passed'] for c in r['checks']),'harness checks failed')
    need(r['scoreEvaluated'] is False,'unexpected score claim')
    need(not r['observerFailures'] and not r['readObserverFailures'],'observer callback failure')
    need(not r['persistentInitial']['gaps'] and not r['persistentFinal']['gaps'],'snapshot coverage gap')
    need(r['readerOutcome']=='COMMITTED' and r['producerOutcome']==('COMPENSATED' if fault else 'COMMITTED'),'terminal outcome')
    # UnitOfWork.id is unset in Saga/local; controlled actor attribution uses A/B, not this field.
    need(r['receivedDto']==r['retainedDtoAtHorizon'],'retained DTO changed after return')
    names=[a['name'] for a in r['actions']]
    expected=['A:getOriginalTournamentStep','A:getTopicsStep','A:updateTournamentStep',
              'A:findQuestionsByTopicIds','A:updateQuizStep']
    b=['B:findTournamentStep','B:commit']
    if case=='fault-before':expected=b+expected
    elif case.endswith('between'):expected=expected[:3]+b+expected[3:]
    expected+=['recover-updateTournamentStep','recover-getOriginalTournamentStep'] if fault else ['A:commit']
    if case=='fault-after':expected+=b
    need(names==expected,'actual action schedule differs')
    for a in r['actions']:
        expected_status='FAULT' if fault and a['name']=='A:updateQuizStep' else 'COMMITTED' if a['name'].endswith(':commit') else 'SUCCESS'
        need(a['status']==expected_status,'unexpected action outcome')
    rows=r['observations'];need([x['order'] for x in rows]==list(range(len(rows))),'observation order')
    need(not [x for x in rows if x['kind'] in ('GAP','EVENT')],'unexpected gap or event delivery')
    reads=[x for x in rows if x['kind']=='READ' and (x['data']['reader'] or {}).get('sagaInstanceId')=='B']
    read=one(reads,'B gateway observation');rd=read['data'];identity=rd['identity'];dto=r['receivedDto']
    need(rd['outcome']=='DELIVERED' and rd['reader']['phase']=='FORWARD','no successful forward read')
    need(rd['contract']['id']=='quizzes.saga-local.tournament-by-id.outer','wrong adapter')
    need(rd['serialized']==r['serialized'],'transport mismatch')
    need(rd['version']==dto['version'] and identity['aggregateId']==dto['aggregateId'],'DTO/gateway revision mismatch')
    initial=r['initial']['tournament'];forward=r['afterUpdate']['tournament'];final=r['final']['tournament']
    atread=r['stateAtRead']['tournament']
    need(rd['version']==atread['version'],'returned version not latest at read')
    need(instant(dto['startTime'])==instant(atread['startTime']) and instant(dto['endTime'])==instant(atread['endTime']),'DTO dates differ from persisted revision')
    need(dto['numberOfQuestions']==atread['numberOfQuestions'],'DTO question count')
    need(sorted(t['aggregateId'] for t in dto['topics'])==atread['topicIds'],'DTO topics')
    need(initial['startTime']=='2030-01-01T15:00' and forward['startTime']=='2030-01-01T16:00','fixture dates')
    need(initial['numberOfQuestions']==2 and forward['numberOfQuestions']==3,'fixture counts')
    writes=[x for x in rows if x['kind']=='WRITE']
    need(all(x['data']['writer']['sagaInstanceId']=='A' for x in writes),'unexpected writer or reader write')
    producer=one([x for x in writes if x['data']['aggregate']['identity']==identity
                  and x['data']['writer']['phase']=='FORWARD'],'producer write')
    pa=producer['data']['aggregate'];pw=producer['data']['writer']
    need(pw['stepName']=='updateTournamentStep' and pa['version']==forward['version'],'forward attribution')
    need(pw['sagaInstanceId']!=rd['reader']['sagaInstanceId'] and pw['functionalityName']!=rd['reader']['functionalityName'],'producer and reader attribution overlap')
    need(pa['frameworkMetadata']['predecessorVersion']==initial['version'],'forward predecessor')
    selected=('startTime','endTime','numberOfQuestions','topicIds')
    need(all(initial[k]!=forward[k] for k in selected),'fixture did not change selected fields')
    recovered=[x for x in writes if x['data']['writer']['phase']=='RECOVERY']
    if fault:
        recovery=one(recovered,'compensation write');ra=recovery['data']['aggregate'];rw=recovery['data']['writer']
        need(ra['identity']==identity and rw['stepName']==pw['stepName'],'recovery attributed to another effect')
        need(ra['frameworkMetadata']['predecessorVersion']==pa['version'] and ra['version']==final['version'],'recovery predecessor')
        need(all(final[k]==initial[k] for k in selected),'selected data was not restored')
        need(r['initial']['quiz']==r['afterQuizStep']['quiz']==r['final']['quiz'],'faulted Quiz body persisted changes')
        cps=r['recoveryCheckpoints']
        need(cps==[dict(sourceStepName='updateTournamentStep',explicitCompensationPending=True,implicitRollbackPending=False),
                   dict(sourceStepName='getOriginalTournamentStep',explicitCompensationPending=False,implicitRollbackPending=True)],'checkpoint plan')
        for a,explicit in zip([a for a in r['actions'] if a['name'].startswith('recover-')],[True,False]):
            rc=a['details']['recovery']
            need(rc['explicitCompensationExecuted']==explicit and rc['implicitRollbackExecuted']==(not explicit),'recovery mode')
        need(r['actions'][names.index('recover-updateTournamentStep')]['details']['state']['tournament']['sagaState']=='IN_UPDATE_TOURNAMENT','lock was already released at explicit compensation')
    else:
        need(not recovered,'successful Saga recovered')
        need(all(final[k]==forward[k] for k in selected),'successful update not retained')
        need(instant(r['final']['quiz']['availableDate'])==instant(final['startTime']),'successful Quiz did not update')
        need(r['final']['quiz']['questionCount']==3,'successful Quiz question count')
    need(final['sagaState']=='NOT_IN_SAGA' and r['final']['quiz']['sagaState']=='NOT_IN_SAGA','unreleased final locks')
    if case.endswith('between'):need(atread['sagaState']=='IN_UPDATE_TOURNAMENT','target read was not during semantic lock')
    witnessed=fault and rd['version']==pa['version'] and producer['order']<read['order']<recovery['order']
    need(witnessed==(case=='fault-between'),'unexpected exposure pattern')
    before=aggregate(r['persistentInitial'],identity)['applicationData']
    after=aggregate(r['persistentFinal'],identity)['applicationData']
    residual=sorted(k for k in before.keys()|after.keys() if before.get(k)!=after.get(k))
    topic_ids_before=[t['topicCourseAggregateId'] for t in before['tournamentTopics']]
    topic_ids_after=[t['topicCourseAggregateId'] for t in after['tournamentTopics']]
    if fault:need(all(v is not None for v in topic_ids_before) and all(v is None for v in topic_ids_after),'topic recovery difference changed')
    return dict(caseId=case,serialized=r['serialized'],experimentalWitnessCount=int(witnessed),
        aggregateId=identity['aggregateId'],baselineVersion=initial['version'],forwardVersion=pa['version'],
        returnedVersion=rd['version'],finalVersion=final['version'],returnedStart=dto['startTime'],finalStart=final['startTime'],
        readerOutcome=r['readerOutcome'],producerOutcome=r['producerOutcome'],
        restoredSelectedFields=list(selected) if fault else [],remainingApplicationDifferences=residual,
        initialTopicCourseIds=topic_ids_before,finalTopicCourseIds=topic_ids_after,scoreEvaluated=False)

def main():
    root=Path(sys.argv[1]);hashes=json.loads((root/'artifact-hashes.json').read_text())
    for path,h in hashes.items():need(digest(root/path)==h,'artifact drift: '+path)
    need(json.loads((root/'exit.json').read_text())['returnCode']==0,'runner failed')
    summaries=[]
    for mode in (False,True):
        for case in CASES:
            r=json.loads((root/f'{case}-{str(mode).lower()}.json').read_text());summaries.append(verify(r))
    for case in CASES:
        pair=[r for r in summaries if r['caseId']==case]
        need({k:v for k,v in pair[0].items() if k!='serialized'}=={k:v for k,v in pair[1].items() if k!='serialized'},'transport control differs: '+case)
    summary=dict(status='PASS',experimentalHistories=4,executions=8,rows=summaries,
        validatorSha256=digest(Path(__file__)),productionScoringModified=False,
        interpretation='One read of an update later selectively restored, in each transport mode; experimental witness only.')
    (root/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    print(json.dumps(summary,indent=2))
if __name__=='__main__':main()
