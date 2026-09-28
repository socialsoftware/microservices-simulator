"""Render final count reductions, including selected events and recovery orders."""
import json
from pathlib import Path

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import numpy as np

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
OUT = ROOT / 'output/pdf/paper-2026-09-20'
OUT.mkdir(parents=True, exist_ok=True)
data = json.loads((HERE / 'summaries.json').read_text())
plt.rcParams.update({'font.size': 10, 'pdf.fonttype': 42,
                     'axes.spines.top': False, 'axes.spines.right': False})
fig, axes = plt.subplots(1, 2, figsize=(10.8, 2.9), layout='constrained')
for ax, key, title in zip(axes, ['workloads', 'faultScenarios'],
                         ['Workloads', 'Fault scenarios']):
    base = [row[key] for row in data['all-final']['totals']]
    for index, (profile, label, colour) in enumerate([
        ('pruned-final', 'Pruning', '#4576a9'),
        ('compressed-final', 'Pruning + compression', '#20856a')]):
        values = [100 * (a - row[key]) / a for a, row in
                  zip(base, data[profile]['totals'])]
        bars = ax.barh(np.arange(3) + (index - .5) * .3, values,
                       height=.26, color=colour, label=label)
        ax.bar_label(bars, labels=[f'{v:.2f}%' if v >= .01 else '<0.01%'
                                  for v in values], padding=4, fontsize=9)
    ax.set_yticks(np.arange(3), ['2 Sagas', '3 Sagas', '4 Sagas'])
    ax.invert_yaxis()
    ax.set_xlim(0, 100)
    ax.set_xticks([0, 25, 50, 75, 100])
    ax.set_xlabel('Reduction (%)')
    ax.set_title(title)
    ax.grid(axis='x', alpha=.15)
    ax.set_axisbelow(True)
fig.legend(*axes[0].get_legend_handles_labels(), loc='outside lower center',
           ncol=2, frameon=False)
fig.savefig(OUT / 'quizzes-fault-reduction.pdf')
fig.savefig(OUT / 'quizzes-fault-reduction.png', dpi=150)
plt.close(fig)
