"""Current replay entry point with common seeded ties and preserved historical runners."""
import argparse
import gzip
import hashlib
import importlib.util
import json
from pathlib import Path
import sys

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('seeded_batch', HERE / 'batch.py')
batch = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = batch
spec.loader.exec_module(batch)
run = batch.run
import study
from seeded_ties import ABS_TOL, REL_TOL, RULE, SeededTiePolicy, make_policy


def factory(arm, states, seed):
    return make_policy(run.make_policy, arm, states, seed)


def audit_ties(value):
    verifier = SeededTiePolicy(None, value['seed'])
    seen = set()
    for decision in value['decisions']:
        identity = decision['workload'], decision['candidate']
        if identity in seen:
            raise ValueError('Repeated candidate')
        seen.add(identity)
        if value['arm'] == 'uniform':
            continue
        tie = decision['choice']['tie']
        candidates = tie['candidates']
        if candidates != sorted(set(candidates)) or not candidates:
            raise ValueError('Invalid tie candidate set')
        if len(candidates) > 1:
            verifier.draws += 1
            chosen = verifier.rng.choice(candidates)
            if tie['draw'] != verifier.draws:
                raise ValueError('Tie draw count mismatch')
        else:
            chosen = candidates[0]
        if chosen != decision['workload']:
            raise ValueError('Tie choice differs from seeded uniform draw')
    return verifier.draws


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--kernel', type=Path, required=True)
    parser.add_argument('--budget', type=int, default=256)
    parser.add_argument('--seeds', type=int, nargs='+', default=[1, 11, 29, 47, 73])
    parser.add_argument('--arms', nargs='+', choices=run.ARMS, default=list(run.ARMS))
    parser.add_argument('--prove', action='store_true')
    args = parser.parse_args()
    output, source = args.output.resolve(), args.source.resolve()
    output.mkdir(parents=True, exist_ok=False)
    proof = run.read(source / 'compiled-kernel-equivalence.json')
    if run.sha(args.kernel) != proof['binarySha256'] or run.sha(source / 'inputs.json') != proof['inputSha256']:
        raise ValueError('Kernel/inventory differs from the numerical proof')
    workloads, evaluator = run.load(output, run.read(source / 'inputs.json'))
    fitness = run.configuration({'policy': 'weighted-criteria-v2',
                                 'weights': {c: 1 for c in run.CRITERIA_V2}})
    sources = [Path(__file__), HERE / 'seeded_ties.py', HERE / 'run.py', HERE / 'batch.py',
        HERE.parent / 'allocator-component-pilot/policies.py',
        HERE.parent / 'allocator-order-hybrid-followup/study.py',
        *sorted((HERE.parent / 'fixed-workload-ga').glob('*.py'))]
    protocol = {'inputSha256': proof['inputSha256'], 'source': str(source),
        'sourceHashes': {str(p): run.sha(p) for p in sources}, 'seeds': args.seeds,
        'arms': args.arms, 'budget': args.budget, 'fitness': fitness, 'localSearch': 'ga',
        'tieRule': RULE, 'tieRelativeTolerance': REL_TOL, 'tieAbsoluteTolerance': ABS_TOL,
        'kernelSha256': proof['binarySha256'], 'historicalRunsReplaced': False}
    seal = hashlib.sha256(json.dumps(protocol, sort_keys=True).encode()).hexdigest()
    batch.write(output / 'protocol.json', protocol)
    binary = batch.kernel(args.kernel)
    if args.prove:
        comparisons = []
        for arm in args.arms:
            batch.install(None)
            dense = run.run_arm(workloads, evaluator, arm, 1, 32, [32], fitness, factory)
            batch.install(binary)
            fast = run.run_arm(workloads, evaluator, arm, 1, 32, [32], fitness, factory)
            if dense['decisions'] != fast['decisions'] or dense['perWorkload'] != fast['perWorkload']:
                raise ValueError('Compiled kernel changes seeded policy decisions')
            comparisons.append({'arm': arm, 'exactDecisions': 32})
        batch.write(output / 'seeded-kernel-proof.json', comparisons)
    batch.install(binary)
    results, starts = [], {}
    for seed in args.seeds:
        local_prefixes = {}
        for arm in args.arms:
            value = run.run_arm(workloads, evaluator, arm, seed, args.budget,
                sorted({n for n in (1000, 2000, 4000, 8000, args.budget) if n <= args.budget}),
                fitness, factory)
            if value['attempts'] != args.budget or not value['start']['allProgressZero']:
                raise ValueError('Incomplete budget or noncold states')
            old_start = starts.setdefault(seed, value['start']['stateSha256'])
            if old_start != value['start']['stateSha256']:
                raise ValueError('GA initial states differ across variants')
            paths = {}
            for d in value['decisions']:
                paths.setdefault(d['workload'], []).append(d['candidate'])
            for wid, prefix in paths.items():
                old = local_prefixes.get(wid, [])
                n = min(len(old), len(prefix))
                if old[:n] != prefix[:n]:
                    raise ValueError('Local GA prefix differs across variants')
                if len(prefix) > len(old):
                    local_prefixes[wid] = prefix
            draws = audit_ties(value)
            value.update(protocolSha256=seal, tieRule=RULE)
            path = output / f'{arm}-seed{seed}-budget{args.budget}.json.gz'
            with gzip.open(path, 'wt') as stream:
                json.dump(value, stream)
            results.append({'arm': arm, 'seed': seed, 'score': value['cumulativeScore'],
                'positives': value['positives'], 'tieDraws': draws, 'path': str(path)})
            batch.write(output / 'status.json', {'complete': False, 'completed': len(results),
                'total': len(args.arms) * len(args.seeds)})
            print('SEEDED', arm, seed, value['cumulativeScore'], 'ties', draws, flush=True)
    if any(run.sha(p) != h for p, h in protocol['sourceHashes'].items()):
        raise ValueError('Source changed during replay')
    batch.write(output / 'results.json', {'rows': results, 'protocol': protocol,
        'checks': {'seededTieDrawsReproduced': True, 'coldStatesMatched': True,
                   'localGaPrefixesMatched': True, 'noRepeatedCandidates': True}})
    batch.write(output / 'status.json', {'complete': True, 'completed': len(results),
        'total': len(args.arms) * len(args.seeds)})


if __name__ == '__main__':
    main()
