"""Aggregate paired transfer seeds and draw the two discovery curves."""
import argparse,json,hashlib
from pathlib import Path
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt


def read(p):return json.loads(p.read_text())
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def stats(xs):
    a=np.array(xs,dtype=float)
    return {'mean':float(a.mean()),'p10':float(np.quantile(a,.1)),
            'p90':float(np.quantile(a,.9)),'min':float(a.min()),'max':float(a.max())}
def paired(xs):
    a=np.array(xs,dtype=float);rng=np.random.default_rng(20260921)
    boot=a[rng.integers(0,len(a),size=(10000,len(a)))].mean(axis=1)
    return {**stats(xs),'mean95PercentBootstrapInterval':[float(x) for x in np.quantile(boot,[.025,.975])],
            'wins':int((a>0).sum()),'ties':int((a==0).sum()),'losses':int((a<0).sum())}


def summarize(run_dir,output,protocol_path):
    output.mkdir(parents=True,exist_ok=True);p=read(protocol_path)
    results=[];hashes={}
    for seed in p['seeds']:
        f=run_dir/f'seed-{seed:02d}.json';receipt=read(run_dir/f'seed-{seed:02d}-receipt.json')
        assert receipt['sha256']==sha(f)
        r=read(f);assert r['seed']==seed
        assert r['verification']['applicationExecutions']==0
        results.append(r);hashes[str(f)]=sha(f)
    names=[a['arm'] for a in results[0]['targetResults']]
    by={name:[next(a for a in r['targetResults'] if a['arm']==name) for r in results] for name in names}
    aggregates={}
    for name,arms in by.items():
        assert all(a['attempts']==p['testBudget'] and a['unknowns']==0 for a in arms)
        aggregates[name]={'positives':stats([a['positiveDiscoveries'] for a in arms]),
                         'score':stats([a['cumulativeScore'] for a in arms]),'checkpoints':{}}
        for k in p['testCheckpoints']:
            points=[next(c for c in a['checkpoints'] if c['attempt']==k) for a in arms]
            aggregates[name]['checkpoints'][str(k)]={metric:stats([c[metric] for c in points]) for metric in ['positiveDiscoveries','cumulativeScore']}
    differences={}
    for family in ['structural','progress']:
        differences[family]={metric:paired([a[field]-b[field] for a,b in zip(by[family+'-warm'],by[family+'-cold'])]) for metric,field in [('positives','positiveDiscoveries'),('score','cumulativeScore')]}
    did={metric:paired([(s1[field]-s0[field])-(p1[field]-p0[field]) for s1,s0,p1,p0 in zip(by['structural-warm'],by['structural-cold'],by['progress-warm'],by['progress-cold'])]) for metric,field in [('positives','positiveDiscoveries'),('score','cumulativeScore')]}
    # Aggregate initial ordering and allocation after grouping by the frozen Saga sets.
    inv=read(Path(protocol_path).parent.parent/'transfer-inventory-2026-09-21/inventory.json')
    family_by_id={r['workload']:' + '.join(r['sagas']) for r in inv['rows']}
    initial={};allocations={}
    for name,arms in by.items():
        counts={}
        for arm in arms:
            family=family_by_id[arm['initialTopChoice']];counts[family]=counts.get(family,0)+1
        initial[name]=counts
        fams=sorted({family_by_id[r['workload']] for r in arms[0]['perWorkload']})
        allocations[name]={family:stats([sum(row['allocations'] for row in a['perWorkload'] if family_by_id[row['workload']]==family) for a in arms]) for family in fams}
    summary={'seeds':p['seeds'],'trainingBudget':p['trainingBudget'],'targetBudget':p['testBudget'],
             'targetRuns':len(names)*len(results),'trainingPrefixes':len(results),
             'recordedSelections':sum(r['training']['attempts']+sum(a['attempts'] for a in r['targetResults']) for r in results),
             'applicationExecutions':0,'arms':aggregates,'pairedWarmMinusCold':differences,
             'differenceInTransferBenefitsStructuralMinusProgress':did,'initialTopFamily':initial,
             'targetAllocationsByFamily':allocations,
             'trainingPositives':stats([sum(d['positive'] for d in r['training']['decisions']) for r in results]),
             'bootstrap':'10,000 paired seed resamples, RNG seed 20260921; intervals describe seed variability conditional on this fixed split',
             'sourceHashes':hashes,'protocolSha256':sha(protocol_path)}
    (output/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    plt.rcParams.update({'font.size':10,'axes.spines.top':False,'axes.spines.right':False})
    fig,axes=plt.subplots(1,2,figsize=(10.4,4.1),layout='constrained')
    styles={'structural-warm':('#1764ab','-','Structure, with prior learning'),
            'structural-cold':('#1764ab','--','Structure, without prior learning'),
            'progress-warm':('#cd6815','-','Progress, with prior learning'),
            'progress-cold':('#cd6815','--','Progress, without prior learning')}
    for ax,metric,ylabel in zip(axes,['positiveDiscoveries','cumulativeScore'],['Positive scenarios found','Accumulated score']):
        for name,arms in by.items():
            curves=np.array([[0]+[d[metric] for d in a['decisions']] for a in arms]);color,ls,label=styles[name];x=np.arange(curves.shape[1])
            ax.plot(x,curves.mean(axis=0),color=color,ls=ls,lw=1.8,label=label)
            ax.fill_between(x,np.quantile(curves,.1,axis=0),np.quantile(curves,.9,axis=0),color=color,alpha=.055)
        ax.set(xlabel='Scenarios selected in test workloads',ylabel=ylabel,xlim=(0,p['testBudget']),ylim=(0,None));ax.grid(alpha=.18)
    axes[0].legend(fontsize=8,loc='upper left')
    fig.suptitle('Transfer to new combinations of known Sagas',fontsize=13)
    fig.savefig(output/'transfer-curves.png',dpi=180)
    fig.savefig(output/'transfer-curves.svg')
    plt.close(fig)
    print(json.dumps({'arms':{k:{m:round(v[m]['mean'],2) for m in ['positives','score']} for k,v in aggregates.items()},'pairedWarmMinusCold':differences,'did':did},indent=2))


if __name__=='__main__':
    ap=argparse.ArgumentParser();ap.add_argument('--runs',type=Path,required=True);ap.add_argument('--output',type=Path,required=True);ap.add_argument('--protocol',type=Path,required=True);a=ap.parse_args();summarize(a.runs,a.output,a.protocol)
