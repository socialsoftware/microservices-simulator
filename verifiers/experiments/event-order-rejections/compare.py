#!/usr/bin/env python3
"""Bounded matched-pair observations, separate from the persistent-object score."""
import argparse
from pathlib import Path
import run as experiment

batch=experiment.batch


def classify(early,late):
    """Consume validated summaries; unavailable controls never become negative findings."""
    if not early.get('comparable') or not late.get('comparable'):
        return 'NOT_COMPARABLE'
    if early['targetStatus']=='FAILED' and late['targetStatus']=='FAILED':
        return 'REJECTED_IN_BOTH'
    if {early['targetStatus'],late['targetStatus']}=={'COMPLETED','FAILED'}:
        rejected=early if early['targetStatus']=='FAILED' else late
        if not rejected.get('invariantRejectionOnReceiver'):
            return 'NOT_COMPARABLE'
        if early['eventStatus']==late['eventStatus']=='COMPLETED' and early['receiverRole']==late['receiverRole'] and early['publisherRole']==late['publisherRole']:
            return 'ORDER_DEPENDENT_REJECTION'
        return 'NOT_COMPARABLE'
    return 'NO_REJECTION_CONTRAST'


def one(values,description):
    if len(values)!=1:raise ValueError('Ambiguous/missing '+description)
    return values[0]


def role(execution,aggregate_id):
    # Join each process-local ID to the same source setup producer occurrence.
    roles=[a['actionId'] for a in execution['sourceSetup']['actions']
           if a['status']=='SUCCEEDED' and a.get('aggregateId')==str(aggregate_id)]
    return one(roles,'setup aggregate role')


def inspect(out,selection,row,attempt):
    summary=dict(caseId=row['caseId'],repetition=attempt['repetition'],comparable=False,
                 processStatus=attempt['status'],reportHashes=attempt['reportHashes'])
    try:
        if attempt.get('exitCode')!=0:raise ValueError('Unsuccessful process')
        directory=Path(attempt['directory'])
        for name,h in attempt['reportHashes'].items():
            if batch.digest(directory/name)!=h:raise ValueError('Report drift')
        e,v1,v2=batch.reports(directory,row,Path(selection['manifest']))
        if e['sourceSetup']['status']!='SUCCEEDED':raise ValueError('Setup not successful')
        if set(e['assignedVector'])!={'0'}:raise ValueError('Assigned fault in pair')
        if e['terminalStatus'] not in ('SUCCESS','PARTIAL_COMPENSATED','COMPENSATED') or e.get('hardStopReason'):
            raise ValueError('Incomplete execution')
        if v2['assessmentStatus']!='COMPLETE':raise ValueError('Observation coverage incomplete')
        target=one([a for a in e['actualActions'] if a['kind']=='FORWARD' and a['sourceScheduledStepId']==row['targetStep']],'target action')
        event=one([a for a in e['actualActions'] if a['kind']=='EVENT_CONSEQUENCE' and a['sourceEventConsequenceId']=='e1'],'moved event')
        if target['status'] not in ('COMPLETED','FAILED'):raise ValueError('Target not executed')
        if target['status']=='FAILED' and target['faultOrigin']!='UNASSIGNED_RUNTIME':raise ValueError('Not an application runtime rejection')
        for a in e['actualActions']:
            if a is not target and a['status'] not in ('COMPLETED','NO_ELIGIBLE_SUBSCRIBER'):
                raise ValueError('Another failure or skipped action')
        if (event['actualPosition']<target['actualPosition'])!=(row['order']=='early'):
            raise ValueError('Wrong actual event placement')
        planned=one([a for a in e['plannedActions'] if a['sourceEventConsequenceId']=='e1'],'planned event route')
        ev=event['eventEvidence'];publisher=role(e,ev['publisherAggregateId']);receiver=None
        if event['status']=='COMPLETED':
            receiver=role(e,ev['subscriberAggregateId'])
            delivery=one([d for d in v2['eventDeliveries'] if d['eventId']==ev['eventId'] and d['eventType']==ev['eventTypeFqn']
                          and d['publisherAggregateId']==ev['publisherAggregateId']
                          and d['publisherAggregateVersion']==ev['publisherAggregateVersion']
                          and d['receiverBefore']['identity']['aggregateId']==ev['subscriberAggregateId']],'actual delivery observation')
        summary.update(comparable=True,executionAttemptId=e['executionAttemptId'],terminal=e['terminalStatus'],
            conformance=e['scheduleConformance'],targetStatus=target['status'],targetStep=target['sourceStepId'],
            targetSaga=target['sagaInstanceId'],exceptionClass=target.get('exceptionClass'),exceptionMessage=target.get('exceptionMessage'),
            eventStatus=event['status'],eventType=ev['eventTypeFqn'],handler=planned['eventHandlerClassFqn'],
            invariantRejectionOnReceiver=target['status']=='FAILED' and any(
                f.get('scheduledStepId')==target['sourceScheduledStepId']
                and f.get('sagaInstanceId')==target['sagaInstanceId']
                and f.get('aggregateId')==str(ev.get('subscriberAggregateId'))
                for f in v1['findings']),
            publisherRole=publisher,receiverRole=receiver,impactV2=v2['completeScore'],impactV1=v1['impactScore'],
            actualOrder=[a['sourceScheduledStepId'] if a['kind']=='FORWARD' else a['sourceEventConsequenceId'] for a in e['actualActions']])
    except (ValueError,KeyError,TypeError,OSError) as failure:
        summary['reason']=str(failure)
    return summary


def summarize(out):
    selection=batch.read(out/'selection.json');experiment.verify(selection)
    attempts=batch.read(out/'attempts.json');rows={r['caseId']:r for r in selection['rows']}
    summaries=[inspect(out,selection,rows[a['caseId']],a) for a in attempts]
    pairs=[]
    for name in sorted({r['pair'] for r in rows.values()}):
        rr={r['order']:r for r in rows.values() if r['pair']==name}
        experiment.matched(rr['early']['workload'],rr['late']['workload'])
        for repetition in [1,2]:
            early=one([r for r in summaries if r['caseId']==rr['early']['caseId'] and r['repetition']==repetition],'early result')
            late=one([r for r in summaries if r['caseId']==rr['late']['caseId'] and r['repetition']==repetition],'late result')
            if early['comparable'] and late['comparable']:
                for key in ['targetStep','targetSaga','eventType','handler','publisherRole']:
                    if early[key]!=late[key]:raise ValueError('Pair identity mismatch: '+key)
            pairs.append(dict(pair=name,repetition=repetition,verdict=classify(early,late),early=early,late=late))
    ids=[r['executionAttemptId'] for r in summaries if r.get('executionAttemptId')]
    if len(ids)!=len(set(ids)):raise ValueError('Reused execution attempt')
    def repeated_semantics(result):
        fields=['comparable','targetStatus','eventStatus','exceptionClass','exceptionMessage',
                'targetStep','targetSaga','publisherRole','receiverRole','impactV2','actualOrder']
        return {side:{k:result[side].get(k) for k in fields} for side in ['early','late']}
    stability={name:len({str(repeated_semantics(r)) for r in pairs if r['pair']==name})==1
               for name in {r['pair'] for r in pairs}}
    for pair in pairs:
        pair['rejectedOrder']=next((side for side in ['early','late'] if pair[side].get('targetStatus')=='FAILED'),None) if pair['verdict']=='ORDER_DEPENDENT_REJECTION' else None
    result=dict(schema='event-order-rejection-comparison.v1',attempts=len(attempts),pairs=pairs,
                repeatAgreement=stability,
                scope='Matched no-fault source-setup pairs; not a universal legality, business-harm, final-state or concurrency-anomaly oracle.')
    batch.save(out/'comparison.json',result)
    print([(r['pair'],r['repetition'],r['verdict'],r['early'].get('reason'),r['late'].get('reason')) for r in pairs])


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);args=p.parse_args();summarize(args.output.resolve())
