#!/usr/bin/env python3
"""Measure an enumerated domain once, then replay search with feedback on selection."""
import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
import shutil
import statistics
import time

from fitness import assess, configuration
from run import check_control
from runtime import Runtime, digest, package, read, retained_attempt, save
from search import candidate_key, run, stable


# Compatibility exports for retained experiment callers.
from catalogue import RecordedDomain, enumerated_domain


def summarize(out, observations, policy, seeds):
    scores = {key: assess(value, policy)['fitnessScore'] for key, value in observations.items()}
    positives = sum(value is not None and value > 0 for value in scores.values())
    unknown = sum(value is None for value in scores.values())
    rows = []
    for seed in seeds:
        for strategy in ['ga', 'random']:
            result = read(out / f'{strategy}-{seed}.json')
            curve = [0]
            for a in result['attempts']:
                curve.append(curve[-1] + int(a['fitnessScore'] is not None and a['fitnessScore'] > 0))
            rows.append({'seed': seed, 'strategy': strategy, 'curve': curve,
                         'evaluatedCandidates': len(result['attempts']), 'stopReason': result['stopReason'],
                         'positiveCount': curve[-1], 'duplicateProposals': result['duplicates'],
                         'evaluationsToAllKnownPositives': next((i for i, count in enumerate(curve)
                             if positives and count == positives), None)})
    horizon = min(r['evaluatedCandidates'] for r in rows)
    summary = {'mode': 'RECORDED_FEEDBACK_SEARCH_NOT_LIVE_TIMING', 'candidateCount': len(scores),
               'knownPositiveCount': positives, 'unknownCount': unknown, 'completeZeroCount': len(scores)-positives-unknown,
               'commonMeasuredHorizon': horizon, 'seeds': seeds,
               'meanPositivesAtCommonHorizon': {s: statistics.mean(r['curve'][horizon] for r in rows if r['strategy']==s)
                                              for s in ['ga', 'random']},
               'runsReachingAllKnownPositives': {s: sum(r['evaluationsToAllKnownPositives'] is not None
                                                      for r in rows if r['strategy']==s) for s in ['ga', 'random']},
               'rows': rows}
    save(out / 'comparison.json', summary)
    lines = ['# Exhaustive reference and recorded-feedback comparison', '',
             f"Reference: {len(scores)} measured candidates; {positives} known positives, "
             f"{len(scores)-positives-unknown} complete zeros and {unknown} unavailable scores.", '',
             'Thirty seeds per method use the unchanged search loop and receive feedback only after selecting a candidate. '
             'Application executions are reused; replay wall time is not end-to-end search performance. '
             'Unknown cases remain in the domain and consume budget. The denominator is known positives, not an assumed '
             'classification of the unavailable cases. Proposal stalls do not prove exhaustion.', '',
             '| Method | Mean positives at common horizon | Runs reaching all known positives |',
             '| --- | --- | --- |']
    for strategy in ['ga', 'random']:
        lines.append(f"| {strategy} | {summary['meanPositivesAtCommonHorizon'][strategy]:.2f} at {horizon} evaluations "
                     f"| {summary['runsReachingAllKnownPositives'][strategy]}/30 |")
    lines += ['', 'The figure shows seeds 1, 11 and 29, selected before observing search outcomes. '
              'All thirty paired results and stop reasons are retained in comparison.json.']
    (out / 'REPORT.md').write_text('\n'.join(lines) + '\n')
    import matplotlib
    matplotlib.use('Agg')
    import matplotlib.pyplot as plt
    fig, axes = plt.subplots(1, 3, figsize=(12, 3.8), sharex=True, sharey=True)
    for ax, seed in zip(axes, [1, 11, 29]):
        for strategy, color, label in [('ga', '#1769aa', 'GA'), ('random', '#c55a11', 'Aleatório')]:
            row = next(r for r in rows if r['strategy'] == strategy and r['seed'] == seed)
            ax.step(range(len(row['curve'])), row['curve'], where='post', color=color, label=label)
        ax.axhline(positives, color='grey', linestyle=':', linewidth=1)
        ax.set_title(f'Semente {seed}'); ax.set_xlabel('Cenários escolhidos'); ax.set_xlim(0, len(scores)); ax.grid(alpha=.2)
    axes[0].set_ylabel('Positivos confirmados encontrados'); axes[0].legend()
    fig.suptitle(f'Mapa medido: {len(scores)} cenários; {unknown} sem score — pesquisa sobre resultados guardados')
    fig.tight_layout()
    for suffix in ['png', 'svg', 'pdf']: fig.savefig(out / f'discovery-curves.{suffix}', dpi=170)
    plt.close(fig)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', type=Path, required=True)
    parser.add_argument('--enumeration', type=Path, required=True)
    parser.add_argument('--control', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--reuse', type=Path, action='append', default=[])
    args = parser.parse_args()
    config = read(args.config)
    policy = configuration(config['fitness'])
    check_control(config, args.control)
    control = read(args.control)
    if (control.get('terminalStatus') != 'SUCCESS' or control.get('scheduleConformance') != 'EXACT'
            or assess(control, policy)['fitnessScore'] is None):
        raise ValueError('Require a successful exact control with complete enabled criteria')
    domain, hashes = enumerated_domain(args.enumeration, config['workload'])
    out = args.output.resolve(); out.mkdir(parents=True, exist_ok=False)
    shutil.copytree(args.enumeration / 'package', out / 'package')
    source = out / 'source'; source.mkdir()
    for p in Path(__file__).parent.glob('*.py'): shutil.copy2(p, source / p.name)
    runtime = Runtime(config['runtime']); runtime.verify()
    observations = {}
    for directory in args.reuse:
        for path in sorted(directory.glob('attempt-*/attempt.json')):
            a = retained_attempt(path)
            if read(path.parent / 'replay.json')['runtime'] != config['runtime']:
                raise ValueError('Reused runtime mismatch')
            c = a['candidate']; key = candidate_key(c['workload'], c['faultVector'], c['actions'])
            if key not in domain.candidates: raise ValueError('Reused case outside domain')
            if key in observations and assess(a, policy) != assess(observations[key], policy):
                raise ValueError('Reused feedback is not repeatable')
            observations[key] = a
    seeds = list(range(1, 31))
    protocol = {'mode': 'MEASURED_REFERENCE_WITH_RECORDED_FEEDBACK_SEARCH', 'config': config,
                'candidateCount': len(domain.candidates), 'seeds': seeds, 'strategies': ['ga', 'random'],
                'budget': len(domain.candidates), 'population': 8, 'mutation': 0.3, 'stallLimit': 1000,
                'workers': 2, 'repeatKeys': sorted(domain.candidates)[::max(1, len(domain.candidates)//6)][:6],
                'figureSeeds': [1, 11, 29],
                'reusedKeys': sorted(observations), 'enumeration': str(args.enumeration.resolve()),
                'packageHashes': hashes, 'controlSha256': digest(args.control),
                'sourceHashes': {p.name: digest(p) for p in source.glob('*.py')},
                'interpretation': 'Only evaluate(candidate) supplies recorded feedback to unchanged search; '
                    'wall time of replay is not live-search performance. Unknown scores stay unknown. '
                    'Proposal stalls remain possible; no unseen-candidate fallback or tuning.'}
    save(out / 'protocol.json', protocol)
    started = time.time()
    def progress(stage):
        save(out / 'status.json', {'stage': stage, 'measured': len(observations),
             'total': len(domain.candidates), 'startedAt': started, 'updatedAt': time.time()})
    progress('MEASURING_REFERENCE')
    missing = [key for key in sorted(domain.candidates) if key not in observations]
    try:
        with ThreadPoolExecutor(max_workers=2) as pool:
            jobs = {pool.submit(runtime.evaluate, out, domain.candidates[key], n, config['timeout']): key
                    for n, key in enumerate(missing, 1)}
            for future in as_completed(jobs):
                key = jobs[future]; observations[key] = future.result()
                save(out / 'reference-progress.json', observations)
                progress('MEASURING_REFERENCE')
        save(out / 'reference.json', {'candidates': domain.candidates, 'observations': observations,
                                     'fitness': {k: assess(v, policy) for k, v in observations.items()}})
        progress('CHECKING_REPEATS')
        repeats = []
        for n, key in enumerate(protocol['repeatKeys'], len(missing) + 1):
            repeated = runtime.evaluate(out, domain.candidates[key], n, config['timeout'])
            equal = (assess(repeated, policy) == assess(observations[key], policy)
                     and all(repeated.get(k) == observations[key].get(k)
                             for k in ('status', 'terminalStatus', 'scheduleConformance')))
            repeats.append({'key': key, 'sameFeedback': equal, 'attempt': repeated})
            save(out / 'repeats.json', repeats)
            if not equal: raise ValueError('Repeated feedback differs; do not replay search as deterministic')
        progress('REPLAYING_SEARCH')
        for seed in seeds:
            for strategy in ['ga', 'random']:
                result = run(domain, lambda c, n: observations[c['key']], strategy=strategy, seed=seed,
                             budget=len(domain.candidates), population=8, mutation=0.3, stall_limit=1000,
                             fitness=policy)
                result['proposalCount'] = len(result.pop('proposals'))
                # Full reports are retained once in the reference, not copied into every seed.
                fields = {'key', 'attempt', 'fitnessScore', 'fitnessComponents', 'fitnessUnavailableReasons',
                          'I', 'A', 'lostCopiedUpdateCount', 'status', 'terminalStatus', 'scheduleConformance',
                          'bestSoFar', 'operator', 'parents', 'genes', 'replacedRecovery'}
                result['attempts'] = [{k: v for k, v in a.items() if k in fields} for a in result['attempts']]
                save(out / f'{strategy}-{seed}.json', result)
        runtime.verify()
        if package(args.enumeration / 'package/scenario-catalog-manifest.json')['hashes'] != hashes:
            raise ValueError('Enumerated package changed')
        for name, expected in protocol['sourceHashes'].items():
            if digest(Path(__file__).parent / name) != expected: raise ValueError('Search source changed')
        summarize(out, observations, policy, seeds)
        progress('COMPLETE')
    except BaseException as error:
        progress('FAILED')
        save(out / 'failure.json', {'error': str(error)})
        raise


if __name__ == '__main__':
    main()
