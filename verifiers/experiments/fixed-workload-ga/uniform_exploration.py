#!/usr/bin/env python3
"""Single-change experiment: GA with uniform unseen initialization and fallback."""
import argparse
from pathlib import Path
import statistics

from exhaustive_reference import RecordedDomain
from search import run
from uniform_reference import curve, digest, read, save


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--reference', type=Path, required=True)
    parser.add_argument('--uniform', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    source, uniform, out = args.reference.resolve(), args.uniform.resolve(), args.output.resolve()
    protocol = read(source / 'protocol.json')
    reference = read(source / 'reference.json')
    if read(source / 'status.json')['stage'] != 'COMPLETE':
        raise ValueError('Require a complete reference campaign')
    seeds = protocol['seeds']
    paths = [source / 'reference.json', source / 'protocol.json', source / 'package/workloads.jsonl']
    paths += [source / f'ga-{s}.json' for s in seeds] + [uniform / f'uniform-{s}.json' for s in seeds]
    paths += [Path(__file__).parent / n for n in ['search.py', 'fitness.py', 'catalogue.py', 'exhaustive_reference.py',
                                               'uniform_reference.py', 'uniform_exploration.py']]
    hashes = {str(p): digest(p) for p in paths}
    workload = next(w for w in map(__import__('json').loads, (source / 'package/workloads.jsonl').read_text().splitlines())
                    if w['id'] == protocol['config']['workload'])
    domain = RecordedDomain(workload, reference['candidates'])
    expected = reference['fitness']
    out.mkdir(parents=True, exist_ok=False)
    rows = []
    for seed in seeds:
        arguments = dict(strategy='ga', seed=seed, budget=protocol['budget'], population=protocol['population'],
                         mutation=protocol['mutation'], stall_limit=protocol['stallLimit'],
                         fitness=protocol['config']['fitness'])
        feedback = lambda c, n: reference['observations'][c['key']]
        original = read(source / f'ga-{seed}.json')
        replay = run(domain, feedback, **arguments)
        fields = ['key', 'fitnessScore', 'operator', 'parents', 'genes', 'replacedRecovery']
        if [[a[k] for k in fields] for a in replay['attempts']] != [[a[k] for k in fields] for a in original['attempts']]:
            raise ValueError('Default search behavior changed')
        if (replay['duplicates'], len(replay['proposals']), replay['stopReason']) != (
                original['duplicates'], original['proposalCount'], original['stopReason']):
            raise ValueError('Default proposal behavior changed')
        variant = run(domain, feedback, **arguments, exploration='uniform-unseen')
        selected = [a['key'] for a in variant['attempts']]
        if len(selected) != len(set(selected)) or set(selected) != set(domain.candidates):
            raise ValueError('Variant did not cover exactly the complete domain')
        compact = {k:v for k,v in variant.items() if k not in ['attempts', 'proposals']}
        compact['attempts'] = [{k:a[k] for k in fields} for a in variant['attempts']]
        compact['proposals'] = variant['proposals']
        save(out / f'ga-uniform-{seed}.json', compact)
        for label, result in [('ga-original', original), ('ga-uniform', variant),
                              ('uniform', read(uniform / f'uniform-{seed}.json'))]:
            attempts = result['attempts']
            if any(a['fitnessScore'] != expected[a['key']]['fitnessScore'] for a in attempts):
                raise ValueError('Feedback mismatch')
            rows.append({'strategy': label, 'seed': seed, 'curve': curve(a['fitnessScore'] for a in attempts),
                         'score2Curve': curve(1 if a['fitnessScore'] == 2 else 0 for a in attempts),
                         'availableScoreSumCurve': [sum(a['fitnessScore'] for a in attempts[:n]
                             if a['fitnessScore'] is not None) for n in range(len(attempts) + 1)],
                         'evaluatedCandidates': len(attempts), 'stopReason': result['stopReason']})
    positives = sum(f['fitnessScore'] is not None and f['fitnessScore'] > 0 for f in expected.values())
    methods = ['ga-original', 'ga-uniform', 'uniform']
    horizon = min(r['evaluatedCandidates'] for r in rows)
    checkpoints = [n for n in [25, 50, 100, 150, horizon] if n <= horizon]
    summary = {'candidateCount': len(domain.candidates), 'knownPositiveCount': positives,
        'seeds': seeds, 'commonMeasuredHorizon': horizon,
        'meanPositives': {m:{str(n):statistics.mean(r['curve'][n] for r in rows if r['strategy']==m)
                           for n in checkpoints} for m in methods},
        'meanEvaluationsToHalfKnownPositives': {m:statistics.mean(next(i for i,v in enumerate(r['curve'])
            if v >= (positives+1)//2) for r in rows if r['strategy']==m) for m in methods},
        'runsReachingAllKnownPositives': {m:sum(r['curve'][-1] == positives for r in rows if r['strategy']==m)
                                         for m in methods}, 'rows': rows}
    save(out / 'comparison.json', summary)
    save(out / 'protocol.json', {'inputHashes': hashes, 'seeds': seeds,
         'change': 'Uniform unseen catalogue sampling for initialization and duplicate fallback only',
         'unchanged': 'Population 8, mutation 0.3, selection, crossover, recovery repair, weights, feedback',
         'scope': 'Finite measured catalogue; development experiment, not held-out validation or live timing'})
    lines = ['# GA with uniform unseen exploration', '',
             'Single-change recorded-feedback experiment on the previously inspected development map.', '',
             '| Evaluations | Original GA | GA with uniform unseen exploration | Uniform random |',
             '| --- | --- | --- | --- |']
    for n in checkpoints:
        lines.append(f"| {n} | " + ' | '.join(f"{summary['meanPositives'][m][str(n)]:.2f}" for m in methods) + ' |')
    lines += ['', f"All known positives reached: {summary['runsReachingAllKnownPositives']}", '',
              'All 30 original traces reproduce exactly under the default policy. All variant traces cover '
              'the full catalogue with no repeated evaluations. Unknown feedback remains unavailable. '
              'No application runs, weights or genetic operators were changed. Generalization requires other workloads.']
    (out / 'REPORT.md').write_text('\n'.join(lines)+'\n')
    import matplotlib
    matplotlib.use('Agg')
    import matplotlib.pyplot as plt
    fig, ax = plt.subplots(figsize=(9,5))
    for m,label,color in [('ga-original','GA original','#1769aa'),
                          ('ga-uniform','GA com exploração uniforme','#8c2981'),
                          ('uniform','Aleatório uniforme','#238b45')]:
        means=[statistics.mean(r['curve'][n] for r in rows if r['strategy']==m) for n in range(horizon+1)]
        ax.plot(range(horizon+1),means,label=label,color=color,linewidth=2)
    ax.set(xlabel='Cenários escolhidos',ylabel='Positivos com score completo (média)',xlim=(0,horizon),
           title='Uma alteração à exploração do GA · 30 sementes · mesmo mapa de 186 cenários')
    ax.legend();ax.grid(alpha=.2);fig.tight_layout()
    for ext in ['png','svg','pdf']:fig.savefig(out/f'discovery-curves.{ext}',dpi=170)
    plt.close(fig)
    if any(digest(Path(p)) != h for p,h in hashes.items()):raise ValueError('Inputs changed')
    save(out / 'validation.json', {'original30TracesUnchanged':True, 'variant30FullDistinctCatalogues':True,
         'allScoresMatchReference':True,'inputHashesUnchanged':True})
    print((out / 'REPORT.md').read_text())


if __name__ == '__main__':
    main()
