"""Matched multi-seed criterion replay, with an audited finite numeric kernel."""
import argparse
from concurrent.futures import ProcessPoolExecutor, as_completed
import gzip
import importlib.util
import itertools
import json
import multiprocessing
from pathlib import Path
import random
import shutil
import sys
import time

# Several experiment folders contain run.py. Spawned workers inherit the
# search paths altered by study imports, so resolve this runner by exact file.
_runner_spec = importlib.util.spec_from_file_location(
    'expanded_replay_run', Path(__file__).with_name('run.py'))
run = importlib.util.module_from_spec(_runner_spec)
sys.modules[_runner_spec.name] = run
_runner_spec.loader.exec_module(run)
import allocator
import hybrid_linucb

ORIGINAL = (allocator._matvec, hybrid_linucb._matvec, allocator._dot,
            hybrid_linucb._dot, hybrid_linucb._transpose_matvec)
WORKLOADS = FACTORY = None


def kernel(path):
    spec = importlib.util.spec_from_file_location('finite_matvec', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def install(module):
    if module is None:
        (allocator._matvec, hybrid_linucb._matvec, allocator._dot,
         hybrid_linucb._dot, hybrid_linucb._transpose_matvec) = ORIGINAL
    else:
        allocator._matvec = hybrid_linucb._matvec = module.matvec
        allocator._dot = hybrid_linucb._dot = module.dot
        hybrid_linucb._transpose_matvec = module.transpose_matvec


def write(path, value):
    temporary = path.with_suffix(path.suffix + '.tmp')
    temporary.write_text(json.dumps(value, indent=2) + '\n')
    temporary.replace(path)


def proof(directory, binary):
    fast = kernel(binary)
    rng = random.Random(260926)
    cases = 0
    for size, density in itertools.product((1, 3, 10, 30, 155), (0, .02, .4, 1)):
        for _ in range(10):
            matrix = [[rng.uniform(-1000, 1000) for _ in range(size)] for _ in range(size)]
            vector = [rng.uniform(-100, 100) if rng.random() < density else 0.0 for _ in range(size)]
            if [v.hex() for v in ORIGINAL[0](matrix, vector)] != [v.hex() for v in fast.matvec(matrix, vector)]:
                raise ValueError('Finite matrix products differ bit for bit')
            if ORIGINAL[2](matrix[0], vector).hex() != fast.dot(matrix[0], vector).hex():
                raise ValueError('Finite dot product differs bit for bit')
            if [v.hex() for v in ORIGINAL[4](matrix, vector)] != [v.hex() for v in fast.transpose_matvec(matrix, vector)]:
                raise ValueError('Finite transpose product differs bit for bit')
            cases += 3
    workloads, factory = run.load(directory, run.read(directory / 'inputs.json'))
    evidence = []
    for profile, arm in itertools.product(run.PROFILES, run.ARMS):
        fit = run.configuration({'policy': 'weighted-criteria-v2', 'weights': {
            c: int(profile == 'all-five' or profile == c) for c in run.CRITERIA_V2}})
        budget = 128 if profile == 'all-five' else 32
        install(None)
        dense = run.run_arm(workloads, factory, arm, 1, budget, [budget], fit, run.make_policy)
        install(fast)
        compiled = run.run_arm(workloads, factory, arm, 1, budget, [budget], fit, run.make_policy)
        if dense['decisions'] != compiled['decisions'] or dense['perWorkload'] != compiled['perWorkload']:
            raise ValueError('Kernel changed decisions or local GA state')
        evidence.append({'profile': profile, 'arm': arm, 'decisions': budget,
                         'traceSha256': dense['decisionSha256']})
        print('EXACT KERNEL', profile, arm, budget, flush=True)
    install(None)
    receipt = {'inputSha256': run.sha(directory / 'inputs.json'),
               'binary': str(binary.resolve()), 'binarySha256': run.sha(binary),
               'cSourceSha256': run.sha(Path(__file__).with_name('finite_matvec.c')),
               'batchSourceSha256': run.sha(Path(__file__)),
               'originalSourceHashes': {str(p): run.sha(p) for p in
                   (Path(allocator.__file__), Path(hybrid_linucb.__file__))},
               'numericCases': cases, 'exactReplayComparisons': evidence,
               'scope': 'Finite measured replay. Ordered products and original CPython math.fsum.'}
    write(directory / 'compiled-kernel-equivalence.json', receipt)


def initialize(directory, binary):
    global WORKLOADS, FACTORY
    import os
    scratch = directory / 'workers' / str(os.getpid())
    scratch.mkdir(parents=True, exist_ok=True)
    shutil.copy2(directory / 'inputs.json', scratch / 'inputs.json')
    WORKLOADS, FACTORY = run.load(scratch, run.read(scratch / 'inputs.json'))
    install(kernel(binary))


def execute(task):
    directory, profile, arm, seed, budget, hashes, receipt_hash = task
    path = directory / f'{profile}-{arm}-seed{seed}-budget{budget}.json.gz'
    started = time.monotonic()
    fit = run.configuration({'policy': 'weighted-criteria-v2', 'weights': {
        c: int(profile == 'all-five' or profile == c) for c in run.CRITERIA_V2}})
    result = run.run_arm(WORKLOADS, FACTORY, arm, seed, budget,
                         sorted({n for n in (128, 256, 512, 1024, 2048, 4096, 8000, budget)
                                 if n <= budget}), fit, run.make_policy)
    result.update(inputSha256=run.sha(directory / 'inputs.json'),
                  runnerSourceHashes=hashes, wallSeconds=time.monotonic() - started,
                  numericKernelReceiptSha256=receipt_hash)
    if result['attempts'] != budget:
        raise ValueError('Search stopped before the matched budget')
    temporary = path.with_suffix(path.suffix + '.partial')
    with gzip.open(temporary, 'wt') as stream:
        json.dump(result, stream)
    temporary.replace(path)
    return {'profile': profile, 'arm': arm, 'seed': seed, 'attempts': result['attempts'],
            'positives': result['positives'], 'score': result['cumulativeScore'],
            'unknowns': result['unknowns'], 'wallSeconds': result['wallSeconds'], 'path': str(path)}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--kernel', type=Path, required=True)
    parser.add_argument('--prove', action='store_true')
    parser.add_argument('--seeds', type=int, nargs='+', default=[1, 11, 29, 47, 73])
    parser.add_argument('--budget', type=int, default=8000)
    parser.add_argument('--workers', type=int, default=3)
    args = parser.parse_args()
    directory, binary = args.output.resolve(), args.kernel.resolve()
    if args.prove:
        proof(directory, binary)
        return
    receipt_path = directory / 'compiled-kernel-equivalence.json'
    receipt = run.read(receipt_path)
    sources = [Path(run.__file__), Path(__file__), Path(__file__).with_name('finite_matvec.c'),
               run.HERE.parent / 'allocator-component-pilot/policies.py',
               run.HERE.parent / 'allocator-order-hybrid-followup/study.py',
               *sorted((run.HERE.parent / 'fixed-workload-ga').glob('*.py'))]
    hashes = {str(p): run.sha(p) for p in sources}
    if (receipt['inputSha256'] != run.sha(directory / 'inputs.json')
            or receipt['binarySha256'] != run.sha(binary)
            or receipt['cSourceSha256'] != run.sha(Path(__file__).with_name('finite_matvec.c'))
            or receipt['batchSourceSha256'] != run.sha(Path(__file__))
            or any(run.sha(p) != h for p, h in receipt['originalSourceHashes'].items())):
        raise ValueError('Kernel proof no longer matches this experiment')
    tasks = []
    completed = []
    for profile, seed, arm in itertools.product(run.PROFILES, args.seeds, run.ARMS):
        path = directory / f'{profile}-{arm}-seed{seed}-budget{args.budget}.json.gz'
        if path.exists():
            with gzip.open(path, 'rt') as f:
                previous = json.load(f)
            if (previous['inputSha256'] != receipt['inputSha256'] or previous['runnerSourceHashes'] != hashes
                    or previous['numericKernelReceiptSha256'] != run.sha(receipt_path)):
                raise ValueError('Existing result has a different protocol')
            completed.append({'profile': profile, 'arm': arm, 'seed': seed,
                              'positives': previous['positives'], 'score': previous['cumulativeScore'],
                              'unknowns': previous['unknowns'], 'path': str(path)})
        else:
            tasks.append((directory, profile, arm, seed, args.budget, hashes, run.sha(receipt_path)))
    total = len(tasks) + len(completed)
    protocol = {'seeds': args.seeds, 'budget': args.budget, 'profiles': run.PROFILES,
                'arms': run.ARMS, 'totalRuns': total, 'inputSha256': receipt['inputSha256'],
                'runnerSourceHashes': hashes, 'kernelReceiptSha256': run.sha(receipt_path)}
    write(directory / 'batch-protocol.json', protocol)
    def status():
        write(directory / 'batch-status.json', {'complete': len(completed) == total,
              'completedRuns': len(completed), 'totalRuns': total, 'results': completed})
    status()
    with ProcessPoolExecutor(max_workers=args.workers, mp_context=multiprocessing.get_context('spawn'),
                             initializer=initialize, initargs=(directory, binary)) as pool:
        futures = [pool.submit(execute, task) for task in tasks]
        for future in as_completed(futures):
            value = future.result()
            completed.append(value)
            status()
            print('REPLAY', len(completed), '/', total, value['profile'], value['arm'],
                  value['seed'], value['positives'], value['score'], value['unknowns'],
                  round(value['wallSeconds'], 1), flush=True)
    if any(run.sha(p) != h for p, h in hashes.items()):
        raise ValueError('Runner changed during experiment')


if __name__ == '__main__':
    main()
