#!/usr/bin/env python3
"""Match discarded pair findings to isolated executions with the same inputs and faults."""
import hashlib, importlib.util, itertools, json, pathlib, sys
ROOT=pathlib.Path(__file__).resolve().parents[3]
spec=importlib.util.spec_from_file_location('compression_observations',ROOT/'verifiers/experiments/compression-preservation/analyze.py')
obs=importlib.util.module_from_spec(spec);spec.loader.exec_module(obs)
def read(p):return json.loads(p.read_text())
def rows(p):return [json.loads(x) for x in p.read_text().splitlines() if x.strip()]
def canonical(x):return json.dumps(x,sort_keys=True,separators=(',',':'))
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()

def summarize(root):
 result=[]
 for group in read(root/'runtime-summary.json'):
  pkg=root/group['label']/'BRUTE_FORCE';ws={w['id']:w for w in rows(pkg/'workloads.jsonl')}
  inventory=read(pkg.parent/'runtime-inventory.json')
  assert inventory['packageHashes']=={p.name:digest(p) for p in pkg.iterdir() if p.is_file()}
  measured=[]
  for r in group['results']:
   a=pathlib.Path(r['attempt']);receipt=read(a/'receipt.json')
   assert receipt['scenario']==r['scenario']
   assert receipt['reportHashes']=={p.name:digest(p) for p in a.glob('execution-report*.json')}
   w=ws[r['scenario']['workload']];roles={p['id']:p for p in w['participants']}
   faults=sorted([roles[s['participant']]['saga'],s['sagaStep']] for s in w['schedule'] if r['scenario']['faultVector'][s['faultSlot']]=='1')
   facts,before,after=obs.signatures(a,w)
   measured.append({**r,'participants':w['participants'],'faults':faults,'facts':facts,'before':before,'after':after})
  baselines={r['before'] for r in measured}
  assert len(baselines)==1,'Initial domain state differs'
  single=[r for r in measured if len(r['participants'])==1];pairs=[r for r in measured if len(r['participants'])==2]
  comparisons=[]
  for pair in pairs:
   alternatives=[]
   for participant in pair['participants']:
    f=[x for x in pair['faults'] if x[0]==participant['saga']]
    alternatives.append([r for r in single if r['participants'][0]['saga']==participant['saga'] and r['participants'][0]['input']==participant['input'] and r['faults']==f])
   assert all(alternatives),'Missing matched isolated fault'
   matches=[]
   for combo in itertools.product(*alternatives):
    union=set().union(*(set(r['facts']) for r in combo))
    baseline={canonical(s['identity']):s for s in json.loads(pair['before'])};composed=dict(baseline);changed={};compatible=True
    for isolated in combo:
     final={canonical(s['identity']):s for s in json.loads(isolated['after'])}
     for identity in baseline.keys()|final.keys():
      if baseline.get(identity)!=final.get(identity):
       if identity in changed and changed[identity]!=final.get(identity):compatible=False
       changed[identity]=final.get(identity)
    for k,v in changed.items():
     if v is None:composed.pop(k,None)
     else:composed[k]=v
    final_equal=compatible and canonical(sorted(composed.values(),key=canonical))==pair['after']
    if union==set(pair['facts']) and final_equal:matches.append([r['scenario']['id'] for r in combo])
   comparisons.append({'pairScenario':pair['scenario']['id'],'matchedIsolated':matches,'findingCount':len(pair['facts']),'score':pair.get('score'),'faults':pair['faults']})
  complete=all(r.get('score') is not None and r.get('conformance')=='EXACT' for r in measured)
  result.append({'label':group['label'],'healthyControls':group['healthyControls'],'completeExactMeasurement':complete,'measured':len(measured),'pairScenarios':len(pairs),'singleScenarios':len(single),'pairPositive':sum((r.get('score') or 0)>0 for r in pairs),'singlePositive':sum((r.get('score') or 0)>0 for r in single),'pairUnknown':sum(r.get('score') is None for r in pairs),'uniquePairFindings':len(set().union(*(set(r['facts']) for r in pairs))),'uniqueSingleFindings':len(set().union(*(set(r['facts']) for r in single))),'allInitialStatesMatch':True,'allMatched':all(x['matchedIsolated'] for x in comparisons),'comparisons':comparisons,'findingSignatures':[json.loads(x) for x in sorted(set().union(*(set(r['facts']) for r in measured)))]})
 return result
if __name__=='__main__':
 root=pathlib.Path(sys.argv[1]).resolve();result=summarize(root);(root/'preservation-summary.json').write_text(json.dumps(result,indent=2))
 for g in result:print({k:v for k,v in g.items() if k not in ['comparisons','findingSignatures']})
