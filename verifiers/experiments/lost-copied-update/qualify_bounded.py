#!/usr/bin/env python3
"""Replay an explicit bounded case list with an integrity-checked runtime descriptor."""
import argparse
import json
from pathlib import Path
import shutil
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path.insert(0, str(HERE.parent / 'fixed-workload-ga'))
from runtime import Runtime, package, read, save


def rooted(value):
    path = Path(value)
    return path if path.is_absolute() else ROOT / path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--case', action='append', dest='selected', help='case label; repeat to select')
    parser.add_argument('--timeout', type=int, default=180)
    args = parser.parse_args()
    config = read(args.config)
    runtime_path = rooted(config['runtime'])
    source_package = rooted(config['package'])
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    shutil.copytree(source_package, output / 'package')
    source_hashes = package(source_package / 'scenario-catalog-manifest.json')['hashes']
    if package(output / 'package/scenario-catalog-manifest.json')['hashes'] != source_hashes:
        raise ValueError('Copied package differs from source')
    runtime = Runtime(read(runtime_path))
    runtime.verify()
    cases = config['cases']
    if args.selected:
        wanted = set(args.selected)
        cases = [case for case in cases if case['label'] in wanted]
        if {case['label'] for case in cases} != wanted:
            raise ValueError('Unknown or duplicate --case selection')
    results = []
    for number, case in enumerate(cases, 1):
        result = runtime.evaluate(output, case['candidate'], number, args.timeout)
        results.append({'label': case['label'], 'attempt': result})
    runtime.verify()
    if package(output / 'package/scenario-catalog-manifest.json')['hashes'] != source_hashes:
        raise ValueError('Qualification modified package')
    save(output / 'qualification.json', {
        'kind': 'BOUNDED_GENERATED_QUALIFICATION',
        'runtime': str(runtime_path),
        'package': str(source_package),
        'cases': results,
    })


if __name__ == '__main__':
    main()
