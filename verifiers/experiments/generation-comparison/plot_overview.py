#!/usr/bin/env python3
"""Global bounded Quizzes counts, separating tuple selection from order reduction."""
import argparse
import json
from pathlib import Path
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('summary', type=Path)
    ap.add_argument('output', type=Path)
    args = ap.parse_args()
    rows = [r for r in json.loads(args.summary.read_text())['rows'] if r['inputCap']==3]
    rows.sort(key=lambda r:r['sagaCount'])
    plt.rcParams.update({'font.size':10,'axes.spines.top':False,'axes.spines.right':False})
    fig, axes = plt.subplots(1,2,figsize=(12.5,5.4))
    specs = [
        ('Input combinations retained', 'allTuples', [
            ('allTuples','Brute force','#919ba4'),('broadTuples','Interaction pruning','#286ba4')]),
        ('Forward orders retained', 'allFullOrders', [
            ('allFullOrders','Brute force, all orders','#919ba4'),
            ('broadFullOrders','Interaction pruning, all orders','#286ba4'),
            ('broadCompressed','Pruning + compression','#17856c')])]
    for ax,(title,denominator,series) in zip(axes,specs):
        for i,(field,label,color) in enumerate(series):
            y=np.arange(len(rows))*1.3+i*.25
            values=[100*int(r[field])/int(r[denominator]) for r in rows]
            ax.barh(y,values,height=.2,label=label,color=color)
            for pos,value in zip(y,values):
                text='0' if value==0 else f'{value:.2f}%'
                ax.text(value+1,pos,text,va='center',fontsize=9)
        ax.set_yticks(np.arange(len(rows))*1.3+(len(series)-1)*.125,[f'{r["sagaCount"]} Sagas' for r in rows])
        ax.invert_yaxis();ax.set_xlim(0,119);ax.set_xticks([0,25,50,75,100])
        ax.set_xlabel('Percentage of the corresponding brute-force baseline')
        ax.set_title(title,fontweight='bold');ax.grid(axis='x',alpha=.15);ax.set_axisbelow(True)
        ax.legend(loc='upper center',bbox_to_anchor=(.5,-.18),frameon=False,fontsize=9)
    fig.suptitle('Quizzes: selection and compression reduce different parts of the space',fontsize=13)
    fig.text(.5,.015,'37 Saga types with accepted inputs; up to 3 inputs per Saga. Static forward-order counts, not executed fault scenarios.',ha='center',fontsize=9)
    fig.tight_layout(rect=(0,.15,1,.94))
    args.output.mkdir(parents=True,exist_ok=True)
    for ext in ['png','pdf']:fig.savefig(args.output/f'global-selection-compression.{ext}',dpi=180)
    plt.close(fig)
    lines=['# Global Quizzes selection and compression','',
        'All combinations of 2–4 distinct Saga types among 37 types with accepted inputs; at most three accepted input variants per Saga. Broad includes strict evidence plus type-only/unknown-key fallback.', '',
        '| Sagas | All input tuples | Broad input tuples | Strict input tuples | Input reduction, broad | All forward orders | Broad forward orders | Broad + compressed orders |',
        '| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |']
    for r in rows:
        fields=[r['sagaCount'],r['allTuples'],r['broadTuples'],r['strictTuples'],
            f'{100*(1-int(r["broadTuples"])/int(r["allTuples"])):.2f}%',
            r['allFullOrders'],r['broadFullOrders'],r['broadCompressed']]
        lines.append('| '+' | '.join(map(str,fields))+' |')
    lines+=['', 'The two panels have different denominators. Broad pruning removes 61–73% of input tuples, but the retained tuples dominate the number of forward orders. At sizes 3 and 4, pruning alone removes about 0.99% and 0.25% of forward orders respectively; compression then reduces the retained order space by 98.12% and 98.79%.', '',
        'These counts measure reduction, not runtime preservation. The separate 98-to-40 update/query experiment holds selection fixed and evaluates compression preservation under declared observations. It does not validate pruning recall.', '']
    (args.output/'OVERVIEW.md').write_text('\n'.join(lines))


if __name__=='__main__':main()
