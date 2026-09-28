#!/usr/bin/env python3
"""Figures and readable report for the fixed complete-map replay protocol."""
import argparse
from pathlib import Path
import statistics
import json
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('directory', type=Path)
    args = ap.parse_args()
    p = args.directory
    s = json.loads((p / 'comparison.json').read_text())
    assert json.loads((p / 'status.json').read_text())['stage'] == 'COMPLETE'
    plt.rcParams.update({'font.size': 10, 'axes.spines.top': False, 'axes.spines.right': False})
    methods = [('ga', 'Genetic algorithm', '#1769aa'), ('random', 'Uniform random', '#c76927')]
    x = np.arange(s['candidateCount'] + 1)
    fig, axes = plt.subplots(1, 2, figsize=(11.8, 4.5))
    for ax, metric, label in zip(axes, ['positive', 'score'],
                                ['Positive scenarios discovered', 'Accumulated available weighted score']):
        for method, name, color in methods:
            data = np.array([r[metric] for r in s['rows'] if r['strategy']==method])
            ax.plot(x, data.mean(axis=0), color=color, label=name, linewidth=2)
            ax.fill_between(x, np.percentile(data,10,axis=0), np.percentile(data,90,axis=0), color=color, alpha=.16)
        ax.set(xlabel='Distinct scenarios evaluated', ylabel=label, xlim=(0,s['candidateCount']), ylim=(0,None))
        ax.grid(alpha=.18)
        ax.legend(frameon=False)
    fig.suptitle(f'Fixed workload · {s["candidateCount"]:,} measured scenarios · {len(s["seeds"])} seeds per method')
    fig.text(.5,.012,'Mean curves; shaded regions show the 10th–90th seed percentiles. Recorded feedback; not execution time.',ha='center',fontsize=9)
    fig.tight_layout(rect=(0,.04,1,.96))
    for ext in ['png','pdf']:
        fig.savefig(p / f'discovery-and-score.{ext}', dpi=180)
    plt.close(fig)
    fig, ax = plt.subplots(figsize=(7,4.3))
    for method,name,color in methods:
        data=np.array([r['unknown'] for r in s['rows'] if r['strategy']==method])
        ax.plot(x,data.mean(axis=0),color=color,label=name,linewidth=2)
        ax.fill_between(x,np.percentile(data,10,axis=0),np.percentile(data,90,axis=0),color=color,alpha=.16)
    ax.set(xlabel='Distinct scenarios evaluated',ylabel='Scenarios without complete score',xlim=(0,s['candidateCount']),ylim=(0,None))
    ax.grid(alpha=.18);ax.legend(frameon=False);fig.tight_layout()
    for ext in ['png','pdf']:fig.savefig(p/f'unknown-curve.{ext}',dpi=180)
    plt.close(fig)
    lines=['# Complete-map search comparison','',
      f'{s["candidateCount"]} scenarios: {s["knownPositiveCount"]} complete positive scores, {s["knownZeroCount"]} complete zero scores, {s["unknownCount"]} unavailable complete scores.',
      '', 'Both methods use the shipped search implementation, the same catalogue, unit weights for all five criteria, and seeds 1–30. Population 8 and mutation probability 0.3 are fixed. Each observation is revealed only when its scenario is selected. Uniform random samples without replacement; GA initializes and falls back to uniform unseen sampling. Unknown scores consume budget and cannot become GA parents.',
      '', '| Evaluations | GA positives | Random positives | GA score sum | Random score sum | GA unknown | Random unknown |',
      '| --- | ---: | ---: | ---: | ---: | ---: | ---: |']
    for n in sorted(s['checkpoints']['ga'], key=int):
        a,b=s['checkpoints']['ga'][n],s['checkpoints']['random'][n]
        lines.append(f'| {n} | {a["positive"]:.2f} | {b["positive"]:.2f} | {a["score"]:.2f} | {b["score"]:.2f} | {a["unknown"]:.2f} | {b["unknown"]:.2f} |')
    lines += ['', '| Fraction of known positives | GA evaluations, mean | Random evaluations, mean |', '| --- | ---: | ---: |']
    for f in ['0.5','0.8','0.9','1']:
        lines.append(f'| {float(f):.0%} | {s["meanEvaluationsToPositiveFraction"]["ga"][f]:.2f} | {s["meanEvaluationsToPositiveFraction"]["random"][f]:.2f} |')
    lines += ['', 'All 60 traces visit each catalogue member exactly once and therefore reach the same positive count and available score sum at exhaustion. Counts refer to scenarios, not distinct defects. The score sum accumulates available complete scores; unavailable scores remain explicitly reported and are not classified as negative.',
      '', 'This evaluates discovery order on one measured workload, not general superiority across applications, live search duration, or execution repeatability for every scenario. The six preselected repeat checks are reported separately. Execution deviations and incomplete assessments remain part of the reference map and must accompany its interpretation.', '']
    (p/'REPORT.md').write_text('\n'.join(lines))


if __name__=='__main__':main()
