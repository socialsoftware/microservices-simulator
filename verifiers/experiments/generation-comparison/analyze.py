#!/usr/bin/env python3
"""Summarize exact static counts; no inference of runtime impact or speedup."""
import collections
import json
import math
from pathlib import Path
import sys

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt


def main():
    out = Path(sys.argv[1])
    if json.loads((out/'status.json').read_text())['stage'] != 'COMPLETE':
        raise RuntimeError('Refusing to summarize an incomplete comparison')
    summaries = []
    examples = []
    caps = sorted(int(p.stem.split('-')[-1]) for p in out.glob('inputs-cap-*.json'))
    if not caps:
        raise RuntimeError('No input populations found')
    plot_cap = max(caps)
    for cap in caps:
        inputs = json.loads((out/f'inputs-cap-{cap}.json').read_text())['idsBySaga']
        n = len(inputs)
        acc = collections.defaultdict(lambda: collections.Counter())
        row_counts = collections.Counter()
        seen = set()
        with (out/f'rows-cap-{cap}.jsonl').open() as f:
            for line in f:
                r = json.loads(line)
                key = tuple(r['sagas'])
                if key in seen:
                    raise AssertionError('Duplicate Saga set')
                seen.add(key)
                k = len(key)
                row_counts[k] += 1
                a,s,b = (int(r[f'{p}Tuples']) for p in ('all','strict','broad'))
                full,sc,bc = (int(r[p]) for p in ('fullOrders','strictCompressed','broadCompressed'))
                assert 0 <= s <= b <= a
                assert 1 <= sc <= bc <= full
                c = acc[k]
                c.update(allSets=1, strictSets=int(s>0), broadSets=int(b>0),
                         allTuples=a, strictTuples=s, broadTuples=b,
                         allFullOrders=a*full, strictFullOrders=s*full, broadFullOrders=b*full,
                         strictCompressed=s*sc, broadCompressed=b*bc)
                if cap==plot_cap and s>0:
                    examples.append(r)
        for k in (2,3,4):
            assert row_counts[k] == math.comb(n,k), (cap,k,row_counts[k],math.comb(n,k))
            summaries.append({'inputCap':cap,'sagaCount':k,'sagasWithInputs':n,**acc[k]})
    checks = [json.loads(l) for l in (out/'verification.jsonl').read_text().splitlines()]
    assert checks and all(c['passed'] for c in checks)
    accounting = json.loads((out/'accounting-cross-check.json').read_text())
    assert accounting['passed']
    # JSON strings preserve very large counts for consumers that use IEEE-754 numbers.
    serial = [{k: (str(v) if k not in ('inputCap','sagaCount','sagasWithInputs') else v)
               for k,v in row.items()} for row in summaries]
    (out/'summary.json').write_text(json.dumps({'rows':serial,'verificationCases':len(checks),'verificationInputCap':accounting.get('inputCap',3),
        'accountingComparisons':accounting['comparisons'],
        'scope':'Uncapped input-bound forward schedule shapes, not FaultScenarios'},indent=2)+'\n')
    text = ['# Quizzes: static generation comparison','',
        'Exact counts over all combinations of 2–4 distinct Saga types with accepted inputs, under each declared input cap. '
        'Forward schedules only: no event expansion, prerequisites, faults or recovery schedules. '
        'Accepted input recipes may still be unsupported by the executor. These are not counts of runnable FaultScenarios.','',
        '## Input selection','',
        '| Input cap | Sagas per set | All sets | Strict sets | Broad sets | All tuples | Strict tuples | Broad tuples |',
        '| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |']
    for r in summaries:
        text.append('| '+' | '.join(str(r[k]) for k in ('inputCap','sagaCount','allSets','strictSets','broadSets','allTuples','strictTuples','broadTuples'))+' |')
    text += ['', 'Broad means strict evidence plus type-only fallback, not type-only matches alone.', '',
             '## Compression on identical selected inputs','',
             '| Input cap | Sagas per set | Conflict lens | Full orders | Compressed orders | Reduction |',
             '| --- | --- | --- | ---: | ---: | ---: |']
    for r in summaries:
        for lens in ('strict','broad'):
            f,c=r[lens+'FullOrders'],r[lens+'Compressed']
            reduction=f'{100*(1-c/f):.2f}%' if f else 'not applicable'
            text.append(f"| {r['inputCap']} | {r['sagaCount']} | {lens} | {f} | {c} | {reduction} |")
    text += ['', 'Totals weight every selected input tuple equally. The same sequence of Saga steps with different inputs counts separately. '
             'A reduction in the static space does not establish preserved runtime outcomes or a measured execution speedup.', '',
             '## Enumeration checks','',
             f'{len(checks)} deterministically selected cases passed complete enumeration, exact count agreement, '
             'uniqueness, in-Saga step order and equality of whole conflict-anchor order sets. '
             f"The independent totals also matched {accounting['comparisons']} ordinary accounting rows/configurations after applying its schedule cap.", '',
             '| Sagas | Lens | Full orders | Compressed orders |', '| --- | --- | ---: | ---: |']
    for c in checks:
        names=[x.rsplit('.',1)[-1].replace('FunctionalitySagas','') for x in c['sagas']]
        text.append(f"| {' + '.join(names)} | {c['lens']} | {c['fullOrders']} | {c['compressedOrders']} |")
    text += ['', '## Scope and interpretation','',
             '- Source hashes, exact input IDs and all Saga-set rows accompany the results.',
             '- No schedule-count cap is applied to the totals shown here; no sampling of Saga sets at sizes 2–4.',
             f'- Input caps {caps} are explicit bounded input populations, not all original tests or possible application inputs.',
             '- Preservation is checked against the extracted conflict model, not a universal application-correctness oracle.',
             '- The enumerated subset is selected by Saga count, full-order size and conflict lens, with SHA-256 ordering; compression ratio is not a selection criterion.',
             '- Runtime, memory, GA quality and impact are not measured by this experiment.','']
    (out/'RESULTS.md').write_text('\n'.join(text))
    plt.rcParams.update({'font.size':10,'axes.spines.top':False,'axes.spines.right':False,'pdf.fonttype':42})
    rows=[r for r in summaries if r['inputCap']==plot_cap]
    fig,axes=plt.subplots(1,2,figsize=(10,4.8),layout='constrained')
    for ax,lens,title in zip(axes,('strict','broad'),('Strict evidence','With type-only fallback')):
        x=list(range(len(rows)))
        for offset,field,label,color in [(-.18,lens+'FullOrders','All step interleavings','#425b76'),(.18,lens+'Compressed','Segment compression','#008576')]:
            values=[r[field] for r in rows]
            ax.bar([v+offset for v in x],values,width=.34,label=label,color=color)
            for xx,v in zip(x,values):
                if v: ax.text(xx+offset,v,f'{v:.2g}',ha='center',va='bottom',fontsize=8)
        if lens == 'strict':
            ax.set_ylim(0, max(r[lens+'FullOrders'] for r in rows)*1.25)
            ax.set_ylabel('Input-bound forward orders')
        else:
            ax.set_yscale('log')
            ax.set_ylim(1, max(r[lens+'FullOrders'] for r in rows)*15)
            ax.set_ylabel('Input-bound forward orders (log scale)')
        for xx,r in zip(x,rows):
            if not r[lens+'FullOrders']:
                ax.text(xx,.08,'No selected\ninput tuples',ha='center',va='bottom',fontsize=8,transform=ax.get_xaxis_transform())
        ax.set_xticks(x,[r['sagaCount'] for r in rows]);ax.set_xlabel('Sagas per combination');ax.set_title(title)
        ax.grid(axis='y',alpha=.15);ax.legend(fontsize=8)
    fig.suptitle(f'Quizzes · uncapped orders · up to {plot_cap} inputs per Saga', fontsize=12)
    for ext in ('png','pdf'):fig.savefig(out/f'compression.{ext}',dpi=180,bbox_inches='tight')
    plt.close(fig)
    fig,ax=plt.subplots(figsize=(7,4),layout='constrained')
    for offset,field,label,color in [(-.25,'allTuples','Brute force','#425b76'),(0,'broadTuples','With type-only fallback','#dc9438'),(.25,'strictTuples','Strict evidence','#008576')]:
        vals=[r[field] for r in rows]
        ax.bar([i+offset for i in range(3)],vals,width=.23,label=label,color=color)
        for i,v in enumerate(vals):
            if v:ax.text(i+offset,v,f'{v:,}',ha='center',va='bottom',fontsize=8)
            else:ax.text(i+offset,.02,'0',ha='center',va='bottom',fontsize=8,transform=ax.get_xaxis_transform())
    ax.set_yscale('log');ax.set_xticks(range(3),[2,3,4]);ax.set_xlabel('Sagas per combination')
    ax.set_ylabel('Selected input tuples (log scale)');ax.set_title(f'Quizzes: input selection · up to {plot_cap} inputs per Saga');ax.legend(fontsize=8)
    ax.grid(axis='y',alpha=.15)
    for ext in ('png','pdf'):fig.savefig(out/f'selection.{ext}',dpi=180,bbox_inches='tight')
    plt.close(fig)
    print(json.dumps({'summary':str(out/'summary.json'),'checks':len(checks)}))

if __name__=='__main__':main()
