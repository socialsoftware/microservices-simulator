"""Post-hoc control of ID priority within one exact structural equivalence class.

This is not an additional primary arm or a tuned performance estimate. The
three largest schedules were selected after observing their feature collision.
Only their relative priority changes; original workload IDs and local GA seeds
remain intact. All other tie rules and all numerical model updates are retained.
"""
import argparse
import collections
import gzip
import importlib.util
import json
from pathlib import Path
import sys

spec = importlib.util.spec_from_file_location('expanded_replay_batch', Path(__file__).with_name('batch.py'))
batch = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = batch
spec.loader.exec_module(batch)
run = batch.run


class AliasPriorityPolicy:
    def __init__(self, base, priority):
        self.base = base
        self.priority = priority

    def select(self, active, states):
        wid, choice, context = self.base.select(active, states)
        if wid in self.priority:
            replacement = next(value for value in self.priority if value in active)
            vector = self.base.feature_space.vector(states[replacement]['profile'], states[replacement]['progress'])
            if vector != context:
                raise ValueError('Priority control may only redirect exactly identical feature vectors')
            wid = replacement
        return wid, choice, context

    def update(self, wid, context, reward):
        return self.base.update(wid, context, reward)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--profile', choices=('all-five', 'DELETED_DEPENDENCY'), required=True)
    args = parser.parse_args()
    out = args.output.resolve()
    diagnostic = out / 'tie-diagnostics' / args.profile
    diagnostic.mkdir(parents=True, exist_ok=True)
    frozen = run.read(out / 'inputs.json')
    workloads, factory = run.load(diagnostic, frozen)
    groups = collections.defaultdict(list)
    for part in frozen['partitions']:
        for row in part['workloads']:
            groups[row['structuralProfileSha256']].append(row)
    aliases = max(groups.values(), key=lambda rows: sum(r['scenarios'] for r in rows))
    if len(aliases) != 3 or sum(r['scenarios'] for r in aliases) != 12462:
        raise ValueError('Diagnostic requires the reviewed three-workload equivalence class')
    priority = sorted((r['workload'] for r in aliases), reverse=True)
    proof_path = out / 'compiled-kernel-equivalence.json'
    proof = run.read(proof_path)
    binary = Path(proof['binary'])
    if (proof['inputSha256'] != run.sha(out / 'inputs.json')
            or proof['binarySha256'] != run.sha(binary)
            or any(run.sha(p) != digest for p, digest in proof['originalSourceHashes'].items())):
        raise ValueError('Numeric proof changed')
    batch.install(batch.kernel(binary))
    fit = run.configuration({'policy': 'weighted-criteria-v2', 'weights': {
        c: int(args.profile == 'all-five' or c == args.profile) for c in run.CRITERIA_V2}})
    def make_policy(arm, states, seed):
        return AliasPriorityPolicy(run.make_policy('S-cal', states, seed), priority)
    result = run.run_arm(workloads, factory, 'S-cal-alias-reversed', 1, 8000,
                         [128, 256, 512, 1024, 2048, 4096, 8000], fit, make_policy)
    with gzip.open(out / f'{args.profile}-S-cal-seed1-budget8000.json.gz', 'rt') as stream:
        baseline = json.load(stream)
    prefixes = {}
    for trace in (baseline, result):
        sequences = collections.defaultdict(list)
        for decision in trace['decisions']:
            sequences[decision['workload']].append(decision['candidate'])
        for wid, values in sequences.items():
            previous = prefixes.get(wid, [])
            common = min(len(values), len(previous))
            if previous[:common] != values[:common]:
                raise ValueError('Tie control changed the local GA candidate prefix')
            if len(values) > len(previous):
                prefixes[wid] = values
    if result['attempts'] != baseline['attempts'] or result['fitness'] != baseline['fitness']:
        raise ValueError('Diagnostic budget or objective mismatch')
    result.update(inputSha256=run.sha(out / 'inputs.json'),
                  numericKernelReceiptSha256=run.sha(proof_path),
                  diagnosticSourceSha256=run.sha(Path(__file__)),
                  originalRunnerSourceHashes=baseline['runnerSourceHashes'])
    with gzip.open(diagnostic / 'trace.json.gz', 'wt') as stream:
        json.dump(result, stream)
    receipt = {'scope': 'Post-hoc selected alias class; one matched seed; not a primary performance arm.',
        'profile': args.profile, 'seed': 1, 'budget': 8000, 'priority': priority,
        'originalPriority': list(reversed(priority)), 'localGaPrefixesMatched': True,
        'inputSha256': result['inputSha256'], 'diagnosticSourceSha256': result['diagnosticSourceSha256'],
        'traceSha256': result['decisionSha256'], 'numericKernelReceiptSha256': result['numericKernelReceiptSha256'],
        'baseline': {k: baseline[k] for k in ('positives', 'unknowns', 'cumulativeScore')},
        'reversedPriority': {k: result[k] for k in ('positives', 'unknowns', 'cumulativeScore')}}
    (diagnostic / 'receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps(receipt), flush=True)


if __name__ == '__main__':
    main()
