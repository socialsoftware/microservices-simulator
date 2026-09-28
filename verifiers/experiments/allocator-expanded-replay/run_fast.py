"""Optional finite-data replay kernel; verify exact decisions before installing.

Only zero products are omitted. Retained terms keep their original order and use
math.fsum. The original policy parameters, feature vectors and updates are intact.
"""
import argparse
import json
import math
import random
from pathlib import Path

import run
import allocator
import hybrid_linucb


ORIGINAL = (allocator._matvec, hybrid_linucb._matvec)


def sparse_matvec(matrix, vector):
    nonzero = [(i, value) for i, value in enumerate(vector) if value != 0]
    return [math.fsum(row[i] * value for i, value in nonzero) for row in matrix]


def install(sparse):
    allocator._matvec = sparse_matvec if sparse else ORIGINAL[0]
    hybrid_linucb._matvec = sparse_matvec if sparse else ORIGINAL[1]


def prove(directory):
    rng = random.Random(260926)
    cases = 0
    for size in (1, 3, 10, 30, 100):
        for density in (0, .02, .4, 1):
            for _ in range(10):
                matrix = [[rng.uniform(-1000, 1000) for _ in range(size)] for _ in range(size)]
                vector = [rng.uniform(-100, 100) if rng.random() < density else 0.0
                          for _ in range(size)]
                expected = ORIGINAL[0](matrix, vector)
                actual = sparse_matvec(matrix, vector)
                if [x.hex() for x in expected] != [x.hex() for x in actual]:
                    raise ValueError('Finite numeric kernel differs bit for bit')
                cases += 1
    frozen = run.read(directory / 'inputs.json')
    workloads, factory = run.load(directory, frozen)
    fit = run.configuration({'policy': 'weighted-criteria-v2',
                             'weights': {c: 1 for c in run.CRITERIA_V2}})
    evidence = []
    for arm in run.ARMS:
        install(False)
        dense = run.run_arm(workloads, factory, arm, 1, 128, [128], fit, run.make_policy)
        install(True)
        sparse = run.run_arm(workloads, factory, arm, 1, 128, [128], fit, run.make_policy)
        if dense['decisions'] != sparse['decisions'] or dense['perWorkload'] != sparse['perWorkload']:
            raise ValueError('Replay decisions differ: ' + arm)
        evidence.append({'arm': arm, 'decisions': 128, 'traceSha256': dense['decisionSha256'],
                         'denseSeconds': dense['selectionAndUpdateSeconds'],
                         'sparseSeconds': sparse['selectionAndUpdateSeconds']})
        print('IDENTICAL', arm, 128, flush=True)
    install(False)
    receipt = {'scope': 'Bounded finite vectors/matrices in this frozen replay',
               'inputSha256': run.sha(directory / 'inputs.json'),
               'kernelSha256': run.sha(Path(__file__)), 'finiteNumericCases': cases,
               'originalSourceHashes': {str(p): run.sha(p) for p in [Path(allocator.__file__),
                                                                    Path(hybrid_linucb.__file__)]},
               'exactReplayComparisons': evidence}
    (directory / 'kernel-equivalence.json').write_text(json.dumps(receipt, indent=2) + '\n')


def main():
    parser = argparse.ArgumentParser(add_help=False)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--prove-kernel', action='store_true')
    args, remaining = parser.parse_known_args()
    if args.prove_kernel:
        prove(args.output)
        return
    receipt = run.read(args.output / 'kernel-equivalence.json')
    if receipt['inputSha256'] != run.sha(args.output / 'inputs.json') \
            or receipt['kernelSha256'] != run.sha(Path(__file__)) \
            or any(run.sha(p) != h for p, h in receipt['originalSourceHashes'].items()):
        raise ValueError('Kernel equivalence receipt differs from this run')
    (args.output / 'execution-kernel.json').write_text(json.dumps({
        'mode': 'OMIT_FINITE_ZERO_PRODUCTS_PRESERVE_FSUM_ORDER',
        'runArguments': remaining,
        'receiptSha256': run.sha(args.output / 'kernel-equivalence.json')}, indent=2) + '\n')
    install(True)
    run.sys.argv = [str(Path(run.__file__)), '--output', str(args.output), *remaining]
    run.main()


if __name__ == '__main__':
    main()
