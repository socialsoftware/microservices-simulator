#!/usr/bin/env python3
"""Run outer allocation plus persistent local-GA selection against recorded maps."""
import argparse
import json
from pathlib import Path

from allocator import allocate, load_recorded_inputs, parse_configuration
from runtime import save


def summary(result):
    lines = [
        '# Cross-workload allocation', '',
        f"Mode: `{result['mode']}`; policy: `{result['policy']}`; "
        f"global attempts: {result['globalAttempts']}/{result['globalBudget']}; "
        f"stop: `{result['stopReason']}`.", '',
        f"Observed positive scenarios: {result['positiveDiscoveries']}; "
        f"cumulative configured score: {result['cumulativeScore']}; "
        f"unknown scores: {result['unknowns']}.", '',
        'Selection timing covers active/exhaustion scans, the outer choice and local-GA ask. '
        'Update timing covers local-GA tell/fitness, observed progress and any adaptive-model update. '
        'Input/model initialization, recorded-feedback lookup, application latency and final serialization '
        'are outside both values.', '',
        f"Selection overhead: {result['selectionOverheadMicros']:.3f} µs; "
        f"update overhead: {result['updateOverheadMicros']:.3f} µs.", '',
        '| Workload | Attempts | Positives | Score | Unknown | State |',
        '| --- | ---: | ---: | ---: | ---: | --- |'
    ]
    for row in result['perWorkload']:
        lines.append(f"| {row['name']} | {row['allocations']} | {row['positiveDiscoveries']} | "
                     f"{row['cumulativeScore']} | {row['unknowns']} | {row['localStopReason']} |")
    lines += ['', 'Each decision resumes that workload’s existing GA population, seen set, RNG and history. '
              'Unavailable fitness consumed its decision and did not update either GA parents or an adaptive model.']
    return '\n'.join(lines) + '\n'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    config = parse_configuration(args.config)
    workloads, evaluator = load_recorded_inputs(config)
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    result = allocate(workloads, evaluator, policy=config['policy'],
                      parameters=config['policyParameters'], seed=config['seed'],
                      budget=config['budget'], fitness=config['fitness'])
    decisions = result.pop('decisions')
    result['inputs'] = [row['provenance'] for row in workloads]
    result['configurationPath'] = config['configurationPath']
    result['configurationSha256'] = config['configurationSha256']
    save(output / 'configuration.json', config)
    save(output / 'results.json', result)
    (output / 'decisions.jsonl').write_text(
        ''.join(json.dumps(row, sort_keys=True, separators=(',', ':')) + '\n'
                for row in decisions))
    (output / 'SUMMARY.md').write_text(summary(result))
    print((output / 'SUMMARY.md').read_text())


if __name__ == '__main__':
    main()
