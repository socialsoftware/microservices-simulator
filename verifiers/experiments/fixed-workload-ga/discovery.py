#!/usr/bin/env python3
"""Complete the reference map, then run the unchanged policies with a longer budget."""
import argparse
from concurrent.futures import ThreadPoolExecutor
from itertools import product
from pathlib import Path
import shutil

from runtime import Runtime, read, save, prepare, package, digest
from run import execute, verify_source, check_control
from search import vector_for

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
REFERENCE = ROOT / 'verifiers/target/fixed-workload-ga/qualification-01'
SEEDS = (11, 29, 47)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    out = args.output.resolve(); out.mkdir(parents=True, exist_ok=False)
    config = read(REFERENCE / 'benchmark-control/config.json')
    check_control(config, REFERENCE / 'benchmark-control/control.json')
    Runtime(config['runtime']).verify()
    source = out / 'source'; source.mkdir()
    for p in HERE.glob('*.py'):
        shutil.copy2(p, source / p.name)
    save(out / 'protocol.json', {'referenceRun': str(REFERENCE), 'config': config,
        'seeds': SEEDS, 'strategies': ['ga', 'random'], 'budgetPerArm': 29,
        'population': 8, 'mutation': 0.3, 'stallLimit': 100,
        'concurrentFreshContainers': 2, 'maxNewApplicationExecutions': 182,
        'control': 'Reuse hash-verified identical-runtime no-fault control; no new control cost.',
        'referenceMap': 'Reuse 21 measured keys and execute the eight unseen keys once.',
        'referenceScoreAccess': 'Map used only in post-run analysis; each arm measures its own feedback.',
        'metrics': ['cumulative distinct positives per execution', 'first positive',
                    'executions to >=50%, >=80%, 100% positives', 'best I',
                    'new crossover children', 'duplicates', 'wall time', 'stop reason'],
        'selection': 'Fixed before missing-case measurements; no parameter tuning or arm replacement.',
        'sourceHashes': {p.name: digest(p) for p in source.glob('*.py')}})
    rt, domain = prepare(config, out / 'map-completion')
    candidates = {}
    for genes in product(*domain.coordinates):
        for c in domain.resolve(vector_for(genes, domain.width)):
            candidates[c['key']] = c
    if len(candidates) != 29 or len(domain.cache) != 12:
        raise ValueError('Declared 29-candidate/12-vector domain changed')
    seen = {}
    for arm in ('ga-11', 'random-11', 'ga-29', 'random-29'):
        for attempt in read(REFERENCE / arm / 'results.json')['attempts']:
            path = Path(attempt['directory']) / 'attempt.json'
            record = read(path)
            for name, expected in record['reportHashes'].items():
                if digest(path.parent / name) != expected:
                    raise ValueError('Reference report drift')
            prior = seen.get(attempt['key'])
            if prior and prior['I'] != attempt['I']:
                raise ValueError('Reference repeated-key disagreement')
            seen[attempt['key']] = {'I': attempt['I'], 'A': attempt['A'],
                'status': attempt['status'], 'attemptPath': str(path), 'attemptSha256': digest(path)}
    missing = sorted(set(candidates) - set(seen))
    if len(seen) != 21 or len(missing) != 8:
        raise ValueError('Expected exactly eight unmeasured candidates')
    save(out / 'reference-selection.json', {'candidates': candidates, 'reused': seen, 'missingKeys': missing})
    for n, key in enumerate(missing, 1):
        a = rt.evaluate(out / 'map-completion', candidates[key], n, config['timeout'])
        if a['status'] != 'COMPLETE':
            raise ValueError('Reference map incomplete; retain failure before comparing discovery fractions')
        path = Path(a['directory']) / 'attempt.json'
        seen[key] = {'I': a['I'], 'A': a['A'], 'status': a['status'],
                     'attemptPath': str(path), 'attemptSha256': digest(path)}
    domain.verify(); rt.verify(); verify_source(config, out / 'map-completion')
    save(out / 'reference-map.json', {'scope': domain.workload['id'], 'candidateCount': len(candidates),
         'positiveCount': sum(a['I'] > 0 for a in seen.values()), 'observations': seen,
         'candidates': candidates, 'newApplicationExecutions': 8, 'reusedCandidateKeys': 21})
    print('Reference map complete. Starting independently measured search arms.', flush=True)
    # Only the configuration reaches execute; no reference-map values are passed to search.
    for seed in SEEDS:
        policies = ('ga', 'random') if seed != 29 else ('random', 'ga')
        with ThreadPoolExecutor(max_workers=2) as pool:
            futures = [pool.submit(execute, {**config, 'strategy': strategy, 'seed': seed,
                'budget': 29, 'population': 8, 'mutation': 0.3, 'stallLimit': 100},
                out / f'{strategy}-{seed}', REFERENCE / 'benchmark-control/control.json')
                for strategy in policies]
            for future in futures:
                future.result()
    rt.verify()
    for name, expected in read(out / 'protocol.json')['sourceHashes'].items():
        if digest(source / name) != expected or digest(HERE / name) != expected:
            raise ValueError('Experiment source changed during measurement')
    save(out / 'completion.json', {'status': 'COMPLETE', 'seeds': SEEDS})
    print('Discovery campaign complete: ' + str(out), flush=True)


if __name__ == '__main__':
    main()
