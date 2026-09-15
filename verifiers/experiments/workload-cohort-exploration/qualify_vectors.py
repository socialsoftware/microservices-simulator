#!/usr/bin/env python3
"""Execute a small declared set of generated vectors for one fixed workload."""
import argparse
from pathlib import Path
import sys

FIXED_GA = Path(__file__).resolve().parents[1] / 'fixed-workload-ga'
sys.path.insert(0, str(FIXED_GA))

from runtime import prepare, read, save  # noqa: E402


def parse_case(value):
    parts = value.split('=', 1)
    if len(parts) != 2 or not all(parts):
        raise argparse.ArgumentTypeError('case must be LABEL=VECTOR[:INDEX]')
    label, selection = parts
    vector, separator, raw_index = selection.partition(':')
    try:
        index = int(raw_index) if separator else 0
    except ValueError as error:
        raise argparse.ArgumentTypeError('recovery index must be an integer') from error
    return label, vector, index


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--case', required=True, action='append', type=parse_case)
    args = parser.parse_args()

    runtime, domain = prepare(read(args.config.resolve()), args.output.resolve())
    results = []
    for number, (label, vector, index) in enumerate(args.case, 1):
        candidates = domain.resolve(vector)
        if not candidates:
            results.append({'label': label, 'vector': vector, 'recoveryIndex': index,
                            'status': 'NO_GENERATED_CANDIDATE'})
            continue
        resolved_index = index if index >= 0 else len(candidates) + index
        if resolved_index < 0 or resolved_index >= len(candidates):
            raise ValueError(f'{label}: recovery index {index} outside {len(candidates)} candidates')
        candidate = candidates[resolved_index]
        attempt = runtime.evaluate(args.output.resolve(), candidate, number, domain.timeout)
        results.append({'label': label, 'vector': vector, 'recoveryIndex': index,
                        'generatedCandidates': len(candidates), 'attempt': attempt})
    domain.verify()
    runtime.verify()
    save(args.output.resolve() / 'qualification.json', {
        'kind': 'DECLARED_VECTOR_QUALIFICATION',
        'workload': domain.workload,
        'recoveryCap': domain.cap,
        'cases': results,
    })


if __name__ == '__main__':
    main()
