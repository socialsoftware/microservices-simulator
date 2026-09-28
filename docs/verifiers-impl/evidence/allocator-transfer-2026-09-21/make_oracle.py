from pathlib import Path
import sys,json,time
root=Path.cwd();sys.path.insert(0,str(root/'verifiers/experiments/allocator-transfer'))
from inputs import load_inputs,read
import allocator
out=root/'verifiers/target/allocator-transfer-2026-09-21'
protocol=read(root/'docs/verifiers-impl/evidence/allocator-transfer-2026-09-21/protocol.json')
w,f,_=load_inputs(root/'docs/verifiers-impl/evidence/allocator-transfer-2026-09-21/inputs.json')
allfeatures=allocator.FeatureSpace({x['id']:x['profile'] for x in w})
train=[x for x in w if x['id'] in protocol['train']];test=[x for x in w if x['id'] in protocol['test']]
results={}
for label,items,policy,budget in [('training',train,'round-robin',132),('structure-cold',test,'contextual-linucb',256),('progress-cold',test,'progress-linucb',256)]:
 old=allocator.FeatureSpace;allocator.FeatureSpace=lambda profiles:allfeatures
 try:
  t=time.monotonic();r=allocator.allocate(items,f(),policy=policy,parameters=protocol['parameters'],seed=1,budget=budget,fitness=protocol['weights']);print(label,round(time.monotonic()-t,2),'seconds',flush=True)
 finally:allocator.FeatureSpace=old
 results[label]=r
(out/'independent-seed1-oracle.json').write_text(json.dumps(results,separators=(',',':'))+'\n')
