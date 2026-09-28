"""Practical allocation alternatives; full eight-arm evidence stays in tables."""
import gzip
import json
import statistics
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from independent_review import OUT, read, trace_path


def main():
    p = read(OUT / 'protocol.json')
    chosen = ['uniform', 'independent', 'progress', 'structural', 'hybrid-order']
    labels = {'uniform': 'Random workload', 'independent': 'Independent UCB',
              'progress': 'Shared progress', 'structural': 'Shared structure', 'hybrid-order': 'Hybrid + order'}
    colors = {'uniform': '#777777', 'independent': '#D68120', 'progress': '#008C95', 'structural': '#386CB0', 'hybrid-order': '#754CA3'}
    fig, axes = plt.subplots(2, 2, figsize=(10.4, 6.8), sharex=True)
    for j, profile in enumerate(p['profiles']):
        for i, collection in enumerate(p['collections']):
            ax = axes[i, j]
            for arm in chosen:
                rs = [json.load(gzip.open(trace_path(profile, collection, arm, s, p), 'rt')) for s in p['seeds']]
                ys = [0] + [statistics.mean(r['decisions'][t]['cumulativeScore'] for r in rs) for t in range(p['budget'])]
                ax.plot(range(p['budget'] + 1), ys, label=labels[arm], color=colors[arm], lw=1.7)
            ax.set_title(('15 workloads / 1,026 scenarios' if i == 0 else '66 workloads / 485 scenarios') + '\n' +
                         ('All five criteria' if profile == 'all-five' else 'Deleted dependencies and compensated reads'), fontsize=10)
            ax.grid(alpha=.18)
            ax.set_ylabel('Accumulated impact')
            if i == 1: ax.set_xlabel('Scenario selections')
            ax.set_xlim(0, 256)
            ax.set_ylim(bottom=0)
    handles, names = axes[0, 0].get_legend_handles_labels()
    fig.legend(handles, names, loc='lower center', ncol=3, frameon=False)
    fig.tight_layout(rect=(0, .105, 1, 1))
    fig.savefig(OUT / 'allocation-preferences.png', dpi=160)
    fig.savefig(OUT / 'allocation-preferences.svg')
    plt.close(fig)

if __name__ == '__main__': main()
