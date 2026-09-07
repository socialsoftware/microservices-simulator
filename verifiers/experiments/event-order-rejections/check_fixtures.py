#!/usr/bin/env python3
"""Quizzes-specific evidence checks; not part of the generic pair verdict."""
import argparse
from datetime import datetime
import json
from pathlib import Path
import re

TIME_FIELDS={'startTime','endTime','endDate','lastModifiedTime','creationDate','availableDate','conclusionDate','resultsDate'}


def check(root):
    records=[]
    for attempt in json.loads((root/'attempts.json').read_text()):
        directory=Path(attempt['directory']);v=json.loads((directory/'execution.impact-v2.json').read_text())
        dates=[]
        def clean(value,path=''):
            if isinstance(value,dict):
                result={}
                for key,item in value.items():
                    if key in TIME_FIELDS:
                        dates.append(dict(path=path+'/'+key,value=item));result[key]='RUNTIME_TIME'
                    else:result[key]=clean(item,path+'/'+key)
                return result
            if isinstance(value,list):return [clean(x,path+'/'+str(i)) for i,x in enumerate(value)]
            return value
        baseline=[dict(identity=s['identity'],state=s['lifecycleState'],data=clean(s['applicationData'],s['identity']['aggregateType'])) for s in v['baseline']]
        row=dict(caseId=attempt['caseId'],repetition=attempt['repetition'],baseline=baseline,observedTimes=dates)
        if attempt['caseId'].startswith('tournament'):
            tournament=next(s['applicationData'] for s in v['baseline'] if s['identity']['aggregateType']=='SagaTournament')
            additions=re.findall(r'^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}).*Adding participant UpdatedName to tournament',
                                 (directory/'docker.log').read_text(),re.M)
            assert len(additions)==1
            margin=(datetime.fromisoformat(tournament['startTime'])-datetime.fromisoformat(additions[0])).total_seconds()
            assert margin>240
            assert datetime.fromisoformat(tournament['endTime'])>datetime.fromisoformat(tournament['startTime'])
            final=next(s['applicationData'] for s in v['finalState'] if s['identity']['aggregateType']=='SagaTournament')
            row.update(secondsBeforeTournamentStart=margin,finalCreator=final['tournamentCreator']['creatorName'],
                       finalParticipants=[r['participantName'] for r in final['tournamentParticipants']])
        else:
            final=next(s['applicationData'] for s in v['finalState'] if s['identity']['aggregateType']=='SagaExecution')
            row['finalEnrolmentCount']=len(final['students']);assert row['finalEnrolmentCount']==0
        records.append(row)
    for prefix in ['tournament','removed-student']:
        chosen=[r for r in records if r['caseId'].startswith(prefix)]
        assert len(chosen)==4
        assert all(r['baseline']==chosen[0]['baseline'] for r in chosen)
    for row in records:row.pop('baseline')
    result=dict(passed=True,excludedTimeFields=sorted(TIME_FIELDS),
                baselineComparison='Same typed identities, lifecycle and application projections except listed time fields; setup producer roles independently joined by comparator.',records=records)
    (root/'fixture-checks.json').write_text(json.dumps(result,indent=2)+'\n')
    print('PASS: comparable baseline projections; every Tournament enrolment attempt more than four minutes before start')


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--output',type=Path,required=True)
    check(parser.parse_args().output.resolve())
