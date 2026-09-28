"""Paired seed comparisons and compact plots for the frozen allocation study."""
import gzip
import hashlib
import json
import random
import statistics as st
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / 'docs/verifiers-impl/evidence/allocator-order-hybrid-2026-09-21'
RUNS = ROOT / 'verifiers/target/allocator-order-hybrid-2026-09-21'


def quantile(rows, q):
    rows = sorted(rows)
    idx = (len(rows)-1)*q
    lo = int(idx)
    return rows[lo] + (rows[min(lo+1,len(rows)-1)]-rows[lo])*(idx-lo)


def paired(xs, ys):
    ds = [x-y for x,y in zip(xs,ys)]
    rng = random.Random(20260921)
    samples = [st.mean(rng.choices(ds,k=len(ds))) for _ in range(10000)]
    return {'meanDifference':st.mean(ds),'pairedBootstrap95':[quantile(samples,.025),quantile(samples,.975)],
            'wins':sum(d>0 for d in ds),'ties':sum(d==0 for d in ds),'losses':sum(d<0 for d in ds)}


def summarize():
    protocol = json.loads((OUT / 'protocol.json').read_text())
    results, loaded = {}, {}
    for collection in protocol['collections']:
        arms = {}
        loaded[collection] = {}
        for arm in protocol['arms']:
            records = []
            for seed in protocol['seeds']:
                p = RUNS / collection / arm / f'seed-{seed:02}.json.gz'
                if not p.exists():
                    break
                receipt = json.loads(p.with_suffix('.receipt.json').read_text())
                assert hashlib.sha256(p.read_bytes()).hexdigest() == receipt['sha256']
                row = json.load(gzip.open(p,'rt'))
                assert row['seed']==seed and row['arm']==arm and row['attempts']==protocol['budget']
                assert row['start']['allProgressZero']
                assert len({(d['workload'],d['candidate']) for d in row['decisions']})==row['attempts']
                assert sum(d['score'] is None for d in row['decisions'])==row['unknowns']
                assert sum(d['score'] is not None and d['score']>0 for d in row['decisions'])==row['positives']
                assert sum(d['score'] or 0 for d in row['decisions'])==row['cumulativeScore']
                records.append(row)
            if len(records)!=len(protocol['seeds']):
                continue
            loaded[collection][arm] = records
            arms[arm] = {metric:{'mean':st.mean(r[metric] for r in records),
                         'p10':quantile([r[metric] for r in records],.1),
                         'p90':quantile([r[metric] for r in records],.9)}
                         for metric in ('positives','cumulativeScore','unknowns','selectionAndUpdateSeconds')}
            arms[arm]['checkpoints'] = [{
                'attempt':n, **{metric:st.mean(next(c for c in r['checkpoints'] if c['attempt']==n)[metric] for r in records)
                               for metric in ('positives','cumulativeScore','unknowns')}} for n in protocol['checkpoints']]
        contrasts = {}
        for left,right in [('order','structural'),('hybrid','structural'),('structural','progress'),('independent','uniform')]:
            if left in loaded[collection] and right in loaded[collection]:
                contrasts[left+' minus '+right] = {m:paired([r[m] for r in loaded[collection][left]],
                    [r[m] for r in loaded[collection][right]]) for m in ('positives','cumulativeScore')}
        results[collection] = {'arms':arms,'contrasts':contrasts}
    (OUT/'summary.json').write_text(json.dumps(results,indent=2)+'\n')
    for col,result in results.items():
        print(col)
        for arm,stats in result['arms'].items():
            print(arm, round(stats['positives']['mean'],2), round(stats['cumulativeScore']['mean'],2))
    return protocol, results, loaded


def plot(protocol, results, loaded):
    import matplotlib
    matplotlib.use('Agg')
    import matplotlib.pyplot as plt
    fig,axes=plt.subplots(2,2,figsize=(11,7),sharex=True)
    labels={'round-robin':'Balanced','uniform':'Random workload','independent':'Independent UCB',
            'progress':'Shared progress','structural':'Shared structure','order':'Shared structure + order','hybrid':'Hybrid structure'}
    for row,(collection,data) in enumerate(loaded.items()):
        for col,metric in enumerate(('positives','cumulativeScore')):
            ax=axes[row,col]
            for arm,records in data.items():
                ys=[0]+[st.mean(r['decisions'][i][metric] for r in records) for i in range(protocol['budget'])]
                ax.plot(range(protocol['budget']+1),ys,label=labels[arm],linewidth=1.6)
            ax.set_title(('15 workloads with three Sagas' if row==0 else '66 workloads with one or two Sagas')+'\n'+('Positive scenarios' if col==0 else 'Accumulated impact'))
            ax.grid(alpha=.2)
            if row==1: ax.set_xlabel('Scenario selections')
    handles,labels_=axes[0,0].get_legend_handles_labels()
    fig.legend(handles,labels_,loc='lower center',ncol=4,fontsize=9)
    fig.tight_layout(rect=(0,.09,1,1))
    fig.savefig(OUT/'allocation-comparison.png',dpi=150)
    fig.savefig(OUT/'allocation-comparison.svg')
    plt.close(fig)


if __name__=='__main__':
    args=summarize()
    try: plot(*args)
    except ImportError: pass
