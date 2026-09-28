#!/usr/bin/env python3
"""Compare a shuffled complete catalogue with retained search traces; no app runs."""
import argparse
import hashlib
import json
from pathlib import Path
import random
import statistics


def read(path):
    return json.loads(path.read_text())


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def save(path, value):
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + '\n')


def curve(scores):
    result = [0]
    for score in scores:
        result.append(result[-1] + int(score is not None and score > 0))
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--reference', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    source, out = args.reference.resolve(), args.output.resolve()
    protocol = read(source / 'protocol.json')
    seeds = protocol['seeds']
    paths = [source / name for name in ['reference.json', 'protocol.json', 'status.json']]
    paths += [source / f'{method}-{seed}.json' for seed in seeds for method in ['ga', 'random']]
    hashes = {str(path): digest(path) for path in paths}
    if read(source / 'status.json')['stage'] != 'COMPLETE':
        raise ValueError('Require a completed reference campaign')
    reference = read(source / 'reference.json')
    keys = sorted(reference['candidates'])
    if set(keys) != set(reference['fitness']) or len(keys) != protocol['candidateCount']:
        raise ValueError('Reference keys/count disagree')
    scores = {k: reference['fitness'][k]['fitnessScore'] for k in keys}
    positives = sum(s is not None and s > 0 for s in scores.values())
    unknown = sum(s is None for s in scores.values())
    out.mkdir(parents=True, exist_ok=False)
    rows = []
    for seed in seeds:
        # The full order is chosen without examining any measured feedback.
        order = keys.copy()
        random.Random(seed).shuffle(order)
        attempts = [{'key': k, 'fitnessScore': scores[k]} for k in order]
        save(out / f'uniform-{seed}.json', {'seed': seed, 'strategy': 'uniform',
             'samplingPolicy': 'uniform-permutation-without-replacement',
             'stopReason': 'EXHAUSTED', 'attempts': attempts})
        for method in ['ga', 'random', 'uniform']:
            result = (read(source / f'{method}-{seed}.json') if method != 'uniform'
                      else {'attempts': attempts, 'stopReason': 'EXHAUSTED'})
            measured = result['attempts']
            selected = [a['key'] for a in measured]
            if len(set(selected)) != len(selected) or not set(selected) <= set(keys):
                raise ValueError('Duplicate or foreign candidate in search trace')
            if any(a['fitnessScore'] != scores[a['key']] for a in measured):
                raise ValueError('Search feedback differs from measured reference')
            discovery = curve(a['fitnessScore'] for a in measured)
            rows.append({'strategy': method, 'seed': seed, 'curve': discovery,
                         'evaluatedCandidates': len(measured), 'stopReason': result['stopReason'],
                         'evaluationsToAllKnownPositives': next(
                             (i for i, n in enumerate(discovery) if positives and n == positives), None)})
    horizon = min(r['evaluatedCandidates'] for r in rows)
    checkpoints = [n for n in [25, 50, 100, 150, horizon] if n <= horizon]
    methods = ['ga', 'random', 'uniform']
    summary = {'mode': 'RECORDED_FEEDBACK_NOT_LIVE_TIMING', 'candidateCount': len(keys),
               'knownPositiveCount': positives, 'unknownCount': unknown, 'seeds': seeds,
               'commonMeasuredHorizon': horizon,
               'checkpoints': {m: {str(n): statistics.mean(r['curve'][n] for r in rows
                    if r['strategy'] == m) for n in checkpoints} for m in methods},
               'runsReachingAllKnownPositives': {m: sum(r['evaluationsToAllKnownPositives'] is not None
                    for r in rows if r['strategy'] == m) for m in methods},
               'meanEvaluationsToHalfKnownPositives': {m: statistics.mean(next(i for i, v in enumerate(r['curve'])
                    if v >= (positives + 1) // 2) for r in rows if r['strategy'] == m) for m in methods},
               'uniformExpectedPositiveCurve': [n * positives / len(keys) for n in range(len(keys) + 1)],
               'rows': rows}
    save(out / 'comparison.json', summary)
    save(out / 'protocol.json', {'source': str(source), 'sourceHashes': hashes,
         'scriptSha256': digest(Path(__file__)), 'seeds': seeds,
         'selection': 'Shuffle sorted candidate keys before feedback; no replacement',
         'scope': 'Baseline addition only. Retained GA and two-stage random traces unchanged. '
                  'Equal numeric seeds do not imply coupled random choices between algorithms.'})
    lines = ['# Uniform catalogue baseline', '',
             f'{len(keys)} candidates; {positives} complete-score positives; {unknown} unavailable scores.', '',
             'All methods use the same measured map and weights. GA and two-stage random traces are retained unchanged. '
             'Uniform random shuffles all candidate keys before reading feedback. Unknown cases remain in the permutation '
             'and consume budget. This compares discovery order, not live timing.', '',
             '| Evaluations | GA | Two-stage random | Uniform random | Uniform expectation |',
             '| --- | --- | --- | --- | --- |']
    for n in checkpoints:
        values = ' | '.join(f"{summary['checkpoints'][m][str(n)]:.2f}" for m in methods)
        lines.append(f'| {n} | {values} | {n * positives / len(keys):.2f} |')
    lines += ['', 'All-known-positive completion counts: ' + str(summary['runsReachingAllKnownPositives']), '',
              'Curves average all 30 seeds up to the common measured horizon; no extrapolation across GA stalls. '
              'The uniform expectation is n × known positives / candidate count. '
              'A complete catalogue has an enumeration/storage cost; this comparison does not measure that cost.']
    (out / 'REPORT.md').write_text('\n'.join(lines) + '\n')
    import matplotlib
    matplotlib.use('Agg')
    import matplotlib.pyplot as plt
    fig, ax = plt.subplots(figsize=(9, 5))
    for method, label, color in [('ga', 'GA', '#1769aa'), ('random', 'Aleatório em duas etapas', '#c55a11'),
                                  ('uniform', 'Aleatório uniforme, sem repetição', '#238b45')]:
        means = [statistics.mean(r['curve'][n] for r in rows if r['strategy'] == method)
                 for n in range(horizon + 1)]
        ax.plot(range(horizon + 1), means, label=label, color=color, linewidth=2)
    ax.plot(range(horizon + 1), summary['uniformExpectedPositiveCurve'][:horizon + 1],
            color='grey', linestyle=':', label='Valor esperado do uniforme')
    ax.set(xlabel='Cenários escolhidos', ylabel='Positivos com score completo (média)',
           title='Mesmo mapa de 186 cenários · 30 sementes por método', xlim=(0, horizon))
    ax.grid(alpha=.2)
    ax.legend()
    fig.tight_layout()
    for suffix in ['png', 'svg', 'pdf']:
        fig.savefig(out / f'discovery-curves.{suffix}', dpi=170)
    plt.close(fig)
    if any(digest(Path(path)) != value for path, value in hashes.items()):
        raise ValueError('Source evidence changed')
    save(out / 'validation.json', {'sourceHashesUnchanged': True, 'allTraceScoresMatchReference': True,
         'uniformPermutationsCoverAllCandidates': True, 'uniformRuns': len(seeds)})
    print((out / 'REPORT.md').read_text())


if __name__ == '__main__':
    main()
