from pathlib import Path
import json,sys,hashlib
import numpy as np
root=Path.cwd();sys.path.insert(0,str(root/'verifiers/experiments/allocator-transfer'))
from inputs import load_inputs,read
from allocator import FeatureSpace,ProgressFeatureSpace
p=root/'verifiers/target/allocator-transfer-2026-09-21';e=root/'docs/verifiers-impl/evidence/allocator-transfer-2026-09-21'
r=read(p/'runs/seed-01.json');oracle=read(p/'independent-seed1-oracle.json')
fields=['workload','candidate','scenarioId','localAttempt','operator','score','positive','unknown']
checks=[]
for name,actual,expected in [('training',r['training'],oracle['training']),('structural-cold',next(a for a in r['targetResults'] if a['arm']=='structural-cold'),oracle['structure-cold']),('progress-cold',next(a for a in r['targetResults'] if a['arm']=='progress-cold'),oracle['progress-cold'])]:
 assert len(actual['decisions'])==len(expected['decisions'])
 for a,b in zip(actual['decisions'],expected['decisions']):
  assert all(a[k]==b[k] for k in fields),(name,a,b)
  if name!='training':assert all(a[k]==b[k] for k in ['index','estimate','uncertainty'])
 checks.append(name+': exact candidate/reward/GA-operator parity with original allocator; cold target indices also identical')
w,f,_=load_inputs(e/'inputs.json');by={x['id']:x for x in w};spaces={'structural':FeatureSpace({x['id']:x['profile'] for x in w}),'progress':ProgressFeatureSpace()}
progress={i:{'allocated':0,'known':0,'unknown':0,'positives':0,'scoreSum':0.,'bestScore':None} for i in r['split']['trainIds']}
Xs={family:[] for family in spaces};ys=[]
for d in r['training']['decisions']:
 before=progress[d['workload']];assert before==d['preUpdateProgress']
 if d['score'] is not None:
  for family,space in spaces.items():Xs[family].append(space.vector(by[d['workload']]['profile'],before))
  ys.append(d['score'])
 before['allocated']+=1
 if d['score'] is None:before['unknown']+=1
 else:
  before['known']+=1;before['scoreSum']+=d['score'];before['positives']+=int(d['score']>0);before['bestScore']=d['score'] if before['bestScore'] is None else max(before['bestScore'],d['score'])
checks.append('All training contexts reconstructed from prior feedback only')
errors={}
for family,space in spaces.items():
 X=np.array(Xs[family]);A=np.eye(len(space.names))+X.T@X;b=X.T@np.array(ys);theta=np.linalg.solve(A,b)
 arm=next(a for a in r['targetResults'] if a['arm']==family+'-warm')
 errs=[]
 for row in arm['initialRanking']:
  x=np.array(space.vector(by[row['workload']]['profile'],{'allocated':0,'known':0,'unknown':0,'positives':0,'scoreSum':0.,'bestScore':None}))
  est=float(theta@x);unc=float(np.sqrt(max(0,x@np.linalg.solve(A,x))))
  errs.extend([abs(est-row['estimate']),abs(unc-row['uncertainty'])])
 assert max(errs)<1e-10,(family,max(errs));errors[family]=max(errs)
 checks.append(family+': transferred initial estimates and uncertainty match independent batch linear solve')
starts=[a['targetStart']['stateSha256'] for a in r['targetResults']];assert len(set(starts))==1
assert all(a['targetStart']['totalAttempts']==a['targetStart']['totalSeen']==a['targetStart']['totalParents']==0 for a in r['targetResults'])
checks.append('All four target GA/progress states begin identically empty')
for a in r['targetResults']:
 expected=r['trainingModelEvidence'][a['featureFamily']]['stateSha256']
 assert (a['modelStart']['stateSha256']==expected)==a['warm']
 assert a['trainingModelUpdates']==(len(ys) if a['warm'] else 0)
checks.append('Only warm arms inherit exact trained model state; cold starts have zero prior updates')
for file,h in read(e/'existing-engine-hashes.json').items():assert hashlib.sha256(Path(file).read_bytes()).hexdigest()==h
checks.append('Existing allocator, GA, fitness, catalogue and cooldown source unchanged')
proof={'status':'PASS','checks':checks,'maxInitialPredictionError':errors,'seed':1,'scope':'real retained inputs, no application execution','oracleSource':str(p/'make_oracle.py')}
(e/'independent-review.json').write_text(json.dumps(proof,indent=2)+'\n');print(json.dumps(proof,indent=2))
