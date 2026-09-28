"""Independent direct counting over retained schedule/access JSON, no extractor calls."""
import hashlib
import itertools
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
OUT=ROOT/'docs/verifiers-impl/evidence/allocator-order-hybrid-2026-09-21'

def main():
    evidence=json.loads((OUT/'order-inputs.json').read_text())
    manifest=json.loads((ROOT/'docs/verifiers-impl/evidence/allocator-transfer-2026-09-21/inputs.json').read_text())
    source=evidence['metadata']['packages'][0]['sagas']
    sagas={s['fqn']:s for s in (json.loads(l) for l in Path(source).read_text().splitlines())}
    reviewed=events=0
    for record in manifest['workloads']:
        path=Path(record['workloadFile'])
        workload=json.loads(path.read_text()) if path.name=='workload.json' else next(json.loads(l) for l in path.read_text().splitlines() if json.loads(l)['id']==record['workload'])
        pmap={p['id']:p['saga'] for p in workload['participants']}
        accesses=[];deliveries=[]
        for pos,item in enumerate(workload['schedule']):
            if item['kind']=='step':
                definition=next(s for s in sagas[pmap[item['participant']]]['steps'] if s['id']==item['sagaStep'])
                footprint={(a['aggregate']['name'],a['aggregate']['mode']) for a in definition['commandAccesses']}
                accesses += [(pos,item['participant'],agg,mode) for agg,mode in footprint]
            else:
                trigger=next(s for s in workload['schedule'] if s['id']==item['triggeringStep'])
                step=next(s for s in sagas[pmap[trigger['participant']]]['steps'] if s['id']==trigger['sagaStep'])
                route=next(r for r in step['eventRoutes'] if r['id']==item['route'])
                footprint={(a['aggregate']['name'],a['aggregate']['mode']) for s in sagas[route['downstreamSaga']]['steps'] for a in s['commandAccesses']}
                deliveries += [(pos,agg,mode) for agg,mode in footprint]
                events+=1
        expected=evidence['workloads'][record['workload']]['counts']['rawPatterns']
        counts={key:0 for key in expected}
        for a,b in itertools.product(accesses,repeat=2):
            if a[0]<b[0] and a[1]!=b[1] and a[2]==b[2]:
                key=f'order:type-potential:{a[3]}-before-{b[3]}'
                if key in counts:counts[key]+=1
        for a,r,b in itertools.product(accesses,repeat=3):
            if (a[0]<r[0]<b[0] and a[1]!=r[1] and b[1]!=r[1] and
                a[2]==r[2]==b[2] and (a[3],r[3],b[3])==('write','read','write')):
                counts['order:type-potential:read-between-foreign-writes']+=1
        for (pos,agg,mode),ordinary in itertools.product(deliveries,accesses):
            if agg==ordinary[2] and pos!=ordinary[0]:
                first,second=(mode,ordinary[3]) if pos<ordinary[0] else (ordinary[3],mode)
                key=f'order:event-type-potential:{first}-before-{second}'
                if key in counts:counts[key]+=1
        assert counts==expected,(record['workload'],counts,expected)
        reviewed+=1
    result={'independentDirectCountsMatch':reviewed,'eventDeliveriesReviewed':events,
            'orderInputSha256':hashlib.sha256((OUT/'order-inputs.json').read_bytes()).hexdigest(),
            'semantics':'Recorded type-level potential access patterns, not same-object or outcome proof'}
    (OUT/'order-independent-review.json').write_text(json.dumps(result,indent=2)+'\n')
    print(result)
if __name__=='__main__':main()
