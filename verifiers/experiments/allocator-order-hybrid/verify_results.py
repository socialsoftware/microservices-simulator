"""Verify every compact receipt, matched local GA prefixes and preserved sources."""
import collections
import gzip
import hashlib
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
OUT=ROOT/'docs/verifiers-impl/evidence/allocator-order-hybrid-2026-09-21'
RUNS=ROOT/'verifiers/target/allocator-order-hybrid-2026-09-21'

def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    protocol=json.loads((OUT/'protocol.json').read_text())
    searches=selections=prefixes_checked=0
    for collection in protocol['collections']:
        for seed in protocol['seeds']:
            starts=[];bywid=collections.defaultdict(list)
            for arm in protocol['arms']:
                folder=RUNS/collection/arm
                identity=json.loads((folder/'identity.json').read_text())
                assert identity['protocol']==sha(OUT/'protocol.json')
                for file,h in identity['sources'].items():
                    path=ROOT/file
                    if sha(path)!=h:
                        assert path.name=='run_study.py' and arm not in ('order','hybrid')
                        assert sha(OUT/'sources/baseline-runner.py')==h
                p=folder/f'seed-{seed:02}.json.gz'
                receipt=json.loads(p.with_suffix('.receipt.json').read_text())
                assert sha(p)==receipt['sha256']
                r=json.load(gzip.open(p,'rt'))
                assert r['attempts']==protocol['budget']==len(r['decisions'])
                starts.append(r['start']['stateSha256'])
                local=collections.defaultdict(list)
                known=positive=unknown=0;score=0.0
                for i,d in enumerate(r['decisions'],1):
                    assert d['decision']==i
                    local[d['workload']].append((d['candidate'],d['score'],d['operator']))
                    assert d['preUpdateProgress']['allocated']==len(local[d['workload']])-1
                    if d['score'] is None: unknown+=1
                    else: known+=1;positive+=d['score']>0;score+=d['score']
                    assert (d['positives'],d['cumulativeScore'],d['unknowns'])==(positive,score,unknown)
                assert (r['positives'],r['cumulativeScore'],r['unknowns'])==(positive,score,unknown)
                for wid,trace in local.items():
                    assert len({t[0] for t in trace})==len(trace)
                    bywid[wid].append(trace)
                searches+=1;selections+=r['attempts']
            assert len(set(starts))==1
            for histories in bywid.values():
                full=max(histories,key=len)
                for history in histories:
                    assert history==full[:len(history)]
                    prefixes_checked+=1
    result={'searchesVerified':searches,'recordedSelections':selections,
            'localGAPrefixesMatched':prefixes_checked,'freshStatesMatchAllArms':True,
            'sourceHashesVerified':True,'baselineRunnerSnapshotException':'Only later metadata pinning differs; exact earlier runner retained',
            'originalEvidenceUnchanged':True}
    # Recheck frozen source evidence bytes, not merely their manifest.
    manifest=json.loads((ROOT/protocol['inputManifest']).read_text())
    for path,h in manifest['files'].items(): assert sha(Path(path))==h
    for p in json.loads((OUT/'order-inputs.json').read_text())['metadata']['packages']:
        for field in ('manifest','sagas','interactions'):assert sha(Path(p[field]))==p[field+'Sha256']
    (OUT/'verification.json').write_text(json.dumps(result,indent=2)+'\n')
    print(result)
if __name__=='__main__': main()
