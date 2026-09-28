"""Paper figures from final saved counts and search traces; no new experiments."""
from pathlib import Path
import json
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import numpy as np

ROOT = Path(__file__).resolve().parents[4]
OUT = ROOT / 'output/pdf/paper-2026-09-20'
OUT.mkdir(parents=True, exist_ok=True)
read = lambda p: json.loads(p.read_text())
plt.rcParams.update({'font.size': 10, 'pdf.fonttype': 42,
                     'axes.spines.top': False, 'axes.spines.right': False})
data = read(ROOT / 'docs/verifiers-impl/evidence/three-variants-2026-09-20/quizzes-summary.json')
rows = [data['totals'][str(k)] for k in [2, 3, 4]]
fig, axes = plt.subplots(1, 2, figsize=(10.8, 3.7), layout='constrained')
panels = [
    ('Input combinations', 'inputCombinations', [('Without reduction', 'inputCombinations', '#bcc3ca'),
      ('After pruning', 'selectedInputCombinations', '#4576a9')]),
    ('Normal action orders', 'allOrders', [('Without reduction', 'allOrders', '#bcc3ca'),
      ('After pruning', 'prunedOrders', '#4576a9'),
      ('After pruning + compression', 'compressedOrders', '#20856a')])]
for ax, (title, denominator, series) in zip(axes, panels):
    centres = np.arange(3) * 1.25
    for i, (label, numerator, colour) in enumerate(series):
        values = [100 * r[numerator] / r[denominator] for r in rows]
        print(title, label, values)
        bars = ax.barh(centres + (i - (len(series)-1)/2)*.23, values,
                       height=.207, color=colour, label=label)
        ax.bar_label(bars, labels=[f'{v:.2f}%' for v in values], padding=3, fontsize=9)
    ax.set_yticks(centres, ['2 Sagas', '3 Sagas', '4 Sagas'])
    ax.invert_yaxis()
    ax.set_xlim(0, 117)
    ax.set_xticks([0, 25, 50, 75, 100])
    ax.set_xlabel('% of the count without reduction')
    ax.set_title(title)
    ax.grid(axis='x', alpha=.15)
    ax.set_axisbelow(True)
    ax.legend(loc='upper center', bbox_to_anchor=(.5, -.24), frameon=False, fontsize=9)
fig.savefig(OUT / 'quizzes-reduction-final.pdf')
plt.close(fig)

base = ROOT / 'verifiers/target/cluster-proteina01-2026-09-20/weight-comparison'
fig, axes = plt.subplots(1, 2, figsize=(10.8, 3.5), layout='constrained')
for ax, (profile, title) in zip(axes, [('all', 'All five criteria'),
                                     ('deleted-dependency', 'Deleted dependencies only')]):
    d = base / ('009-' + profile)
    summary = read(d / 'summary.json')
    for method, label, colour in [('ga', 'GA', '#2166ac'), ('random', 'Uniform random', '#d6604d')]:
        values = np.array([read(d / f'{method}-{i}.json')['positive'] for i in range(1,31)])
        x = np.arange(values.shape[1])
        ax.plot(x, values.mean(axis=0), label=label, color=colour, linewidth=1.8)
        ax.fill_between(x, np.percentile(values,10,axis=0), np.percentile(values,90,axis=0), color=colour, alpha=.16)
    ax.set_title(f"{title}\n{summary['positive']:,} positive / {summary['count']:,} scenarios")
    ax.set_xlabel('Distinct scenarios selected')
    ax.set_ylabel('Positive scenarios found')
    ax.set_xlim(0,3360)
    ax.set_ylim(bottom=0)
    ax.grid(alpha=.17)
    ax.legend(frameon=False)
fig.savefig(OUT / 'search-preferences-final.pdf')
plt.close(fig)
