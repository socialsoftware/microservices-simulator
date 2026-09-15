#!/usr/bin/env python3
"""Select every no-fault and final-update-step fault schedule, before runtime outcomes."""
import json,sys
from pathlib import Path
out=Path(sys.argv[1]);pkg=out/'package'
workloads={w['id']:w for w in map(json.loads,(pkg/'workloads.jsonl').read_text().splitlines())}
selected=[]
for f in map(json.loads,(pkg/'fault-scenarios.jsonl').read_text().splitlines()):
 w=workloads[f['workload']];steps=[s for s in w['schedule'] if s['kind']=='step']
 pos=next(i for i,s in enumerate(steps) if s['sagaStep']=='findTournamentStep#0')
 fault=next(s['faultSlot'] for s in steps if s['sagaStep']=='updateQuizStep#0')
 vector=f['faultVector'];control=set(vector)=={'0'}
 if not control and not (vector.count('1')==1 and vector[fault]=='1'):continue
 selected.append(dict(position=pos,control=control,scenario=f['id'],workload=w['id'],vector=vector,actions=f['actions'],schedule=w['schedule']))
selected.sort(key=lambda x:(not x['control'],x['position'],x['scenario']))
for i,x in enumerate(selected):x['label']=f"{i:02d}-{'control' if x['control'] else 'fault'}-position-{x['position']}"
assert sum(x['control'] for x in selected)==6
(out/'selection.json').write_text(json.dumps(selected,indent=2)+'\n')
(out/'selection.tsv').write_text(''.join(x['label']+'\t'+x['scenario']+'\n' for x in selected))
print(len(selected),'selected:',sum(x['control'] for x in selected),'controls')
