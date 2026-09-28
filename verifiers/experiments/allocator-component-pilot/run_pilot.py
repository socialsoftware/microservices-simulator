"""Small design pilot on previously examined Quizzes complete maps.

The output diagnoses model behavior and feasibility; it is not confirmation evidence.
"""
import argparse
import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path[:0] = [str(HERE), str(HERE.parent / 'allocator-order-hybrid-followup'),
                str(HERE.parent / 'allocator-transfer'),
                str(HERE.parent / 'fixed-workload-ga')]

from fitness import CRITERIA_V2, configuration
from inputs import load_inputs
from policies import policy
from study import run_arm


ARMS = ('P-shared', 'S-shared', 'SP-shared', 'P-local', 'H0', 'H1',
        'S-unit', 'SP-unit', 'H1-unit', 'S-cal', 'SP-cal', 'H1-cal')
INPUTS = ROOT / 'docs/verifiers-impl/evidence/allocator-transfer-2026-09-21/inputs.json'
SPLIT = ROOT / 'docs/verifiers-impl/evidence/transfer-inventory-2026-09-21/proposed-split.json'


def profile(name):
    if name == 'all-five':
        enabled = set(CRITERIA_V2)
    elif name == 'dependency-and-read':
        enabled = {'DELETED_DEPENDENCY', 'COMPENSATED_READ_EXPOSURE'}
    else:
        raise ValueError(name)
    return configuration({'policy': 'weighted-criteria-v2',
                          'weights': {criterion: int(criterion in enabled)
                                      for criterion in CRITERIA_V2}})


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--seed', type=int, default=1)
    parser.add_argument('--budget', type=int, default=32)
    parser.add_argument('--arms', nargs='+', choices=ARMS, default=list(ARMS))
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.budget < 1 or args.budget > 64:
        raise ValueError('Design pilot budget must be 1..64')
    workloads, factory, proof = load_inputs(INPUTS)
    by_id = {row['id']: row for row in workloads}
    split = json.loads(SPLIT.read_text())
    # The previously examined 66- and 15-workload panels remain separate.
    panels = {'one-two-saga': split['train'], 'three-saga': split['test']}
    results = {'purpose': 'design pilot only; no confirmatory comparison',
               'seed': args.seed, 'budget': args.budget, 'inputs': str(INPUTS),
               'profiles': {}, 'inputWorkloads': len(proof)}
    for profile_name in ('all-five', 'dependency-and-read'):
        fit = profile(profile_name)
        results['profiles'][profile_name] = {}
        for panel_name, identities in panels.items():
            rows = [by_id[wid] for wid in identities]
            result_rows = {}
            for arm in args.arms:
                run = run_arm(rows, factory, arm, args.seed, args.budget,
                              [args.budget], fit, policy)
                decisions = run['decisions']
                counts = {}
                for decision in decisions:
                    wid = decision['workload']
                    counts[wid] = counts.get(wid, 0) + 1
                result_rows[arm] = {
                    'attempts': run['attempts'],
                    'positives': run['positives'],
                    'score': run['cumulativeScore'],
                    'unknowns': run['unknowns'],
                    'visited': len(counts),
                    'maximumAllocationsToOneWorkload': max(counts.values(), default=0),
                    'perWorkloadAllocations': dict(sorted(counts.items())),
                    'selectionAndUpdateSeconds': run['selectionAndUpdateSeconds'],
                    'firstEightChoices': [row['workload'] for row in decisions[:8]],
                    'decisions': [{key: row[key] for key in (
                        'decision', 'workload', 'candidate', 'score', 'choice',
                        'preUpdateProgress', 'modelUpdated')} for row in decisions],
                    'decisionSha256': run['decisionSha256'],
                }
                print(profile_name, panel_name, arm, run['attempts'], flush=True)
            results['profiles'][profile_name][panel_name] = result_rows
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(results, indent=2) + '\n')


if __name__ == '__main__':
    main()
