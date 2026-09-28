"""Run the frozen transfer protocol locally; no application execution."""
import argparse
import json
import time
from pathlib import Path
from inputs import load_inputs, read, sha
from transfer import run_transfer
from fitness import assess


def save(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, separators=(',', ':')) + '\n')


def run(protocol_path, manifest, output, seeds):
    protocol, output = read(protocol_path), Path(output)
    if sha(manifest) != protocol['inputManifestSha256']:
        raise ValueError('Manifest differs from frozen protocol')
    if any(s not in protocol['seeds'] for s in seeds):
        raise ValueError('Seed outside frozen protocol')
    workloads, evaluator_factory, proof = load_inputs(manifest)
    source_root = Path(__file__).resolve().parent
    sources = [source_root / n for n in ('inputs.py', 'run_study.py')]
    sources += [source_root.parent / 'fixed-workload-ga' / n for n in (
        'transfer.py', 'allocator.py', 'search.py', 'catalogue.py', 'fitness.py')]
    identity = {'protocolSha256': sha(protocol_path), 'manifestSha256': sha(manifest),
                'sources': {str(p.resolve()): sha(p) for p in sources}}
    output.mkdir(parents=True, exist_ok=True)
    if (output / 'run-identity.json').exists():
        if read(output / 'run-identity.json') != identity:
            raise ValueError('Cannot mix changed inputs or code in one study')
    else:
        save(output / 'run-identity.json', identity)
    save(output / 'input-checks.json', proof)
    for seed in seeds:
        dest = output / f'seed-{seed:02d}.json'
        receipt = output / f'seed-{seed:02d}-receipt.json'
        if dest.exists():
            if not receipt.exists() or read(receipt)['sha256'] != sha(dest):
                raise ValueError('Existing seed result lacks valid receipt')
            print(f'seed {seed}: retained verified result', flush=True)
            continue
        calls = []
        class CheckedEvaluator:
            mode = 'RECORDED_FEEDBACK_SEQUENTIAL'
            def __init__(self):
                self.evaluator = evaluator_factory()
                self.calls = []
                calls.append(self.calls)
            def evaluate(self, workload, candidate, attempt):
                if attempt != len(self.calls) + 1:
                    raise ValueError('Nonsequential evaluator calls')
                value = self.evaluator.evaluate(workload, candidate, attempt)
                score = assess(value, protocol['weights'])['fitnessScore']
                self.calls.append((workload, candidate['key'], score))
                return value
        started = time.monotonic()
        result = run_transfer(workloads, CheckedEvaluator,
            protocol['train'], protocol['test'], seed,
            train_budget=protocol['trainingBudget'], test_budget=protocol['testBudget'],
            checkpoints=protocol['testCheckpoints'])
        assert len(calls) == 5
        assert result['protocol']['fitness'] == protocol['weights']
        phases = [result['training'], *result['targetResults']]
        for index, (phase, observed) in enumerate(zip(phases, calls)):
            rows = phase['decisions']
            assert observed == [(r['workload'], r['candidate'], r['score']) for r in rows]
            assert len({(w, c) for w, c, _ in observed}) == len(observed)
            allowed = set(protocol['train'] if index == 0 else protocol['test'])
            assert all(w in allowed for w, _, _ in observed)
            assert len(observed) == (protocol['trainingBudget'] if index == 0 else protocol['testBudget'])
            if index:
                assert phase['targetStart']['allProgressZero']
                assert phase['commonTrainingHistorySha256'] == result['training']['historySha256']
                assert phase['positiveDiscoveries'] == sum(s is not None and s > 0 for _, _, s in observed)
                assert phase['cumulativeScore'] == sum(s for _, _, s in observed if s is not None)
                assert phase['unknowns'] == sum(s is None for _, _, s in observed)
        result['verification'] = {'evaluatorTraceMatchesAllFivePhases': True,
            'onlySelectedFeedbackRevealed': True, 'noRepeatedCandidatesWithinPhase': True,
            'trainingAndTestDisjoint': True, 'targetTotalsRecomputed': True,
            'applicationExecutions': 0, 'wallSeconds': time.monotonic() - started}
        if any(sha(p) != h for p, h in identity['sources'].items()):
            raise ValueError('Engine changed during search')
        save(dest, result)
        save(receipt, {'seed': seed, 'sha256': sha(dest), 'identitySha256': sha(output / 'run-identity.json')})
        save(output / 'status.json', {'lastCompletedSeed': seed,
            'completedSeeds': sorted(read(p)['seed'] for p in output.glob('seed-*-receipt.json')),
            'applicationExecutions': 0})
        print(f'seed {seed}: four arms verified in {result["verification"]["wallSeconds"]:.2f}s', flush=True)
    # References/metadata can be edited independently of the runner process.
    for name, expected in read(manifest)['files'].items():
        if sha(name) != expected:
            raise ValueError('Input changed during study: ' + name)


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--protocol', required=True)
    ap.add_argument('--inputs', required=True)
    ap.add_argument('--output', required=True)
    ap.add_argument('--seeds', type=int, nargs='+')
    args = ap.parse_args()
    run(Path(args.protocol), Path(args.inputs), Path(args.output),
        args.seeds or read(args.protocol)['seeds'])
