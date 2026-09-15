#!/usr/bin/env python3
"""Derive a fixed-workload experiment config while preserving a frozen runtime."""
import argparse
import json
from pathlib import Path


def read(path):
    return json.loads(path.read_text())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base', required=True, type=Path,
                        help='config containing the frozen runtime object')
    parser.add_argument('--manifest', required=True, type=Path)
    parser.add_argument('--workload', required=True)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--budget', type=int, default=4)
    parser.add_argument('--seed', type=int, default=1)
    parser.add_argument('--strategy', choices=('random', 'ga'), default='random')
    parser.add_argument('--recovery-cap', type=int, default=500)
    parser.add_argument('--timeout', type=int, default=180)
    parser.add_argument('--fitness-version', choices=('v1', 'v2'), default='v2')
    args = parser.parse_args()

    base = read(args.base.resolve())
    criteria = [
        'COMPENSATED_READ_EXPOSURE',
        'DELETED_DEPENDENCY',
        'FAILED_OPERATION_RESIDUAL',
        'UNRESOLVED_DELIVERED_EVENT',
    ]
    if args.fitness_version == 'v2':
        criteria.append('LOST_COPIED_UPDATE')
    config = {
        'manifest': str(args.manifest.resolve()),
        'workload': args.workload,
        'budget': args.budget,
        'fitness': {
            'policy': f'weighted-criteria-{args.fitness_version}',
            'weights': {criterion: 1 for criterion in criteria},
        },
        'recoveryCap': args.recovery_cap,
        'seed': args.seed,
        'strategy': args.strategy,
        'population': min(8, args.budget),
        'mutation': 0.3,
        'stallLimit': 1000,
        'timeout': args.timeout,
        'runtime': base['runtime'],
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(config, indent=2, sort_keys=True) + '\n')


if __name__ == '__main__':
    main()
