"""Resumable, matched seeded replay over the audited full and smaller cohorts."""
import argparse
from concurrent.futures import ProcessPoolExecutor, as_completed
import gzip
import hashlib
import itertools
import json
import multiprocessing
from pathlib import Path
import shutil
import sys

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import seeded_replay as seeded

run = seeded.run
batch = seeded.batch
WORKLOADS = FACTORY = None


def fitness(profile):
    return run.configuration({'policy': 'weighted-criteria-v2', 'weights': {
        c: int(profile == 'all-five' or profile == c) for c in run.CRITERIA_V2}})


def path_for(directory, profile, arm, seed, budget):
    return directory / f'{profile}-{arm}-seed{seed}-budget{budget}.json.gz'


def initialize(directory, binary):
    global WORKLOADS, FACTORY
    import os
    scratch = directory / 'workers' / str(os.getpid())
    scratch.mkdir(parents=True, exist_ok=True)
    shutil.copy2(directory / 'inputs.json', scratch / 'inputs.json')
    WORKLOADS, FACTORY = run.load(scratch, run.read(scratch / 'inputs.json'))
    batch.install(batch.kernel(binary))


def execute(task):
    directory, profile, arm, seed, budget, seal = task
    value = run.run_arm(WORKLOADS, FACTORY, arm, seed, budget,
        sorted({n for n in (1000, 2000, 4000, 8000, budget) if n <= budget}),
        fitness(profile), seeded.factory)
    if value['attempts'] != budget or not value['start']['allProgressZero']:
        raise ValueError('Incomplete budget or noncold states')
    draws = seeded.audit_ties(value)
    value.update(profile=profile, protocolSha256=seal, tieRule=seeded.RULE)
    destination = path_for(directory, profile, arm, seed, budget)
    partial = destination.with_suffix(destination.suffix + '.partial')
    with gzip.open(partial, 'wt') as stream:
        json.dump(value, stream)
    partial.replace(destination)
    return {'profile': profile, 'arm': arm, 'seed': seed, 'attempts': budget,
        'score': value['cumulativeScore'], 'positives': value['positives'],
        'unknowns': value['unknowns'], 'tieDraws': draws, 'path': str(destination)}


def audit_group(directory, profile, seed, arms, budget, seal):
    starts, prefixes = set(), {}
    for arm in arms:
        with gzip.open(path_for(directory, profile, arm, seed, budget), 'rt') as stream:
            value = json.load(stream)
        if value['protocolSha256'] != seal or value['profile'] != profile:
            raise ValueError('Result belongs to another protocol')
        if value['attempts'] != budget or not value['start']['allProgressZero']:
            raise ValueError('Result is incomplete or noncold')
        seeded.audit_ties(value)
        starts.add(value['start']['stateSha256'])
        local = {}
        for decision in value['decisions']:
            local.setdefault(decision['workload'], []).append(decision['candidate'])
        for wid, prefix in local.items():
            old = prefixes.get(wid, [])
            if old[:min(len(old), len(prefix))] != prefix[:min(len(old), len(prefix))]:
                raise ValueError('GA prefix differs across policies')
            if len(prefix) > len(old):
                prefixes[wid] = prefix
    if len(starts) != 1:
        raise ValueError('GA initial states differ across policies')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--kernel', type=Path, required=True)
    parser.add_argument('--budget', type=int, required=True)
    parser.add_argument('--workers', type=int, default=3)
    parser.add_argument('--seeds', type=int, nargs='+', default=[1, 11, 29, 47, 73])
    parser.add_argument('--profiles', nargs='+', choices=run.PROFILES, default=list(run.PROFILES))
    parser.add_argument('--arms', nargs='+', choices=run.ARMS, default=list(run.ARMS))
    args = parser.parse_args()
    directory, binary = args.output.resolve(), args.kernel.resolve()
    proof = run.read(directory / 'compiled-kernel-equivalence.json')
    if proof['inputSha256'] != run.sha(directory / 'inputs.json') or proof['binarySha256'] != run.sha(binary):
        raise ValueError('Numeric proof differs from frozen inputs or kernel')
    if (proof['cSourceSha256'] != run.sha(HERE / 'finite_matvec.c')
            or proof['batchSourceSha256'] != run.sha(HERE / 'batch.py')
            or any(run.sha(p) != h for p, h in proof['originalSourceHashes'].items())):
        raise ValueError('Numeric proof refers to changed source')
    seeded_proof = directory / 'formal-seeded-kernel-proof.json'
    if not seeded_proof.exists():
        workloads, evaluator = run.load(directory, run.read(directory / 'inputs.json'))
        fast_kernel = batch.kernel(binary)
        comparisons = []
        for profile, arm in itertools.product(args.profiles, args.arms):
            fit = fitness(profile)
            batch.install(None)
            dense = run.run_arm(workloads, evaluator, arm, 1, 16, [16], fit, seeded.factory)
            batch.install(fast_kernel)
            fast = run.run_arm(workloads, evaluator, arm, 1, 16, [16], fit, seeded.factory)
            if dense['decisions'] != fast['decisions'] or dense['perWorkload'] != fast['perWorkload']:
                raise ValueError('Kernel changes seeded policy decisions')
            comparisons.append({'profile': profile, 'arm': arm, 'exactDecisions': 16})
        batch.install(None)
        batch.write(seeded_proof, {'inputSha256': proof['inputSha256'],
            'kernelSha256': proof['binarySha256'], 'comparisons': comparisons})
    else:
        checked = run.read(seeded_proof)
        if (checked['inputSha256'] != proof['inputSha256']
                or checked['kernelSha256'] != proof['binarySha256']
                or {(r['profile'], r['arm']) for r in checked['comparisons']}
                   != set(itertools.product(args.profiles, args.arms))):
            raise ValueError('Seeded kernel proof refers to another protocol')
    sources = [Path(__file__), HERE / 'seeded_replay.py', HERE / 'seeded_ties.py',
        HERE / 'run.py', HERE / 'batch.py', HERE / 'master_freeze.py',
        HERE.parent / 'allocator-component-pilot/policies.py',
        HERE.parent / 'allocator-order-hybrid-followup/study.py',
        *sorted((HERE.parent / 'fixed-workload-ga').glob('*.py'))]
    protocol = {'inputSha256': proof['inputSha256'], 'kernelSha256': proof['binarySha256'],
        'kernelProofSha256': run.sha(directory / 'compiled-kernel-equivalence.json'),
        'sourceHashes': {str(p.resolve()): run.sha(p) for p in sources},
        'budget': args.budget, 'seeds': args.seeds, 'profiles': args.profiles,
        'arms': args.arms, 'tieRule': seeded.RULE, 'localSearch': 'ga'}
    seal = hashlib.sha256(json.dumps(protocol, sort_keys=True).encode()).hexdigest()
    old_protocol = directory / 'formal-protocol.json'
    if old_protocol.exists() and run.read(old_protocol) != protocol:
        raise ValueError('Cannot resume a different formal protocol')
    batch.write(old_protocol, protocol)
    tasks, completed = [], []
    for profile, seed, arm in itertools.product(args.profiles, args.seeds, args.arms):
        destination = path_for(directory, profile, arm, seed, args.budget)
        if destination.exists():
            with gzip.open(destination, 'rt') as stream:
                value = json.load(stream)
            if value['protocolSha256'] != seal or value['attempts'] != args.budget:
                raise ValueError('Existing result differs from protocol')
            completed.append({'profile': profile, 'arm': arm, 'seed': seed,
                'attempts': args.budget, 'score': value['cumulativeScore'],
                'positives': value['positives'], 'unknowns': value['unknowns'],
                'tieDraws': seeded.audit_ties(value), 'path': str(destination)})
        else:
            tasks.append((directory, profile, arm, seed, args.budget, seal))
    total = len(tasks) + len(completed)
    def status(done=False):
        batch.write(directory / 'formal-status.json', {'complete': done,
            'completedRuns': len(completed), 'totalRuns': total, 'rows': completed})
    status()
    with ProcessPoolExecutor(max_workers=args.workers,
            mp_context=multiprocessing.get_context('spawn'),
            initializer=initialize, initargs=(directory, binary)) as pool:
        futures = [pool.submit(execute, task) for task in tasks]
        for future in as_completed(futures):
            row = future.result()
            completed.append(row)
            status()
            print('FORMAL', len(completed), '/', total, row['profile'], row['arm'],
                  row['seed'], row['positives'], row['score'], flush=True)
    for profile, seed in itertools.product(args.profiles, args.seeds):
        audit_group(directory, profile, seed, args.arms, args.budget, seal)
    if any(run.sha(p) != h for p, h in protocol['sourceHashes'].items()):
        raise ValueError('Source changed during replay')
    status(True)
    batch.write(directory / 'formal-results.json', {'protocol': protocol,
        'checks': {'coldStarts': True, 'matchedGaPrefixes': True,
                   'seededTiesReproduced': True, 'completeBudgets': True},
        'rows': completed})


if __name__ == '__main__':
    main()
