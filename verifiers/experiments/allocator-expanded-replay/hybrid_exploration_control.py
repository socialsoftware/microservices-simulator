"""Matched alpha-only control: hybrid cold bonuses sqrt(2) -> 1.

Preserves every formal result and all frozen references. This is a diagnostic
on the existing benchmark, not a fresh held-out estimate of generalization.
"""
import argparse
from collections import defaultdict
from concurrent.futures import ProcessPoolExecutor, as_completed
import copy
import gzip
import hashlib
import itertools
import json
import math
import multiprocessing
from pathlib import Path
import shutil
import time

import formal_replay as formal
from policies import policy
from seeded_ties import RULE, SeededTiePolicy
from transfer import _new_states, _observe

run, batch = formal.run, formal.batch
ARMS = {'H0-cold1': ('H0', 1.0 / math.sqrt(2.0)),
        'H1-cold1': ('H1-cal', 1.0 / math.sqrt(3.0))}
WORKLOADS = EVALUATOR = None


def factory(arm, states, seed):
    parent, alpha = ARMS[arm]
    model = policy(parent, states, seed)
    model.exploration = alpha
    return SeededTiePolicy(model, seed)


def check_alpha_only(workloads):
    """Same histories must give identical fitted models and scaled bonuses."""
    states = _new_states(workloads, 1, 64, formal.fitness('all-five'))
    checks = []
    for arm, (parent, alpha) in ARMS.items():
        old, new = policy(parent, states, 1), factory(arm, states, 1).model
        ratio = new.exploration / old.exploration
        for wid in states:
            _, before, context = old.select({wid}, states)
            _, after, other_context = new.select({wid}, states)
            if context != other_context or before['estimate'] != after['estimate']:
                raise ValueError('Control changed features or prediction')
            if not math.isclose(after['uncertainty'], 1.0, abs_tol=1e-12):
                raise ValueError('Control cold bonus is not one')
        # Explicit common histories isolate alpha from trajectory changes.
        probe_states = copy.deepcopy({wid: {'profile': s['profile'], 'progress': s['progress']}
                                      for wid, s in states.items()})
        probe_ids = sorted(states)[:3]
        for n, reward in enumerate([None, 0.0, 1.0, 3.0, 0.0, 2.0] * 4):
            wid = probe_ids[n % len(probe_ids)]
            _, a, ca = old.select({wid}, probe_states)
            _, b, cb = new.select({wid}, probe_states)
            if ca != cb or a['estimate'] != b['estimate']:
                raise ValueError('Control changed learned prediction')
            if not math.isclose(b['uncertainty'], a['uncertainty'] * ratio,
                                rel_tol=1e-12, abs_tol=1e-12):
                raise ValueError('Control changed variance rather than just alpha')
            if old.update(wid, ca, reward) != new.update(wid, cb, reward):
                raise ValueError('Update semantics changed')
            _observe(probe_states[wid]['progress'], reward)
            for key in ('shared_inverse', 'shared_response', 'shared_theta', 'updates'):
                if getattr(old, key) != getattr(new, key):
                    raise ValueError('Shared learning changed')
            for probe in probe_ids:
                if vars(old.locals[probe]) != vars(new.locals[probe]):
                    raise ValueError('Local learning changed')
        checks.append({'arm': arm, 'parent': parent, 'alpha': alpha,
            'coldWorkloadsChecked': len(states), 'commonHistoryUpdates': 24,
            'identicalFeatureVectorsAndLearning': True, 'onlyBonusMultiplierChanged': True})
    return checks


def read_trace(path):
    with gzip.open(path, 'rt') as stream:
        return json.load(stream)


def initialize(output, binary):
    global WORKLOADS, EVALUATOR
    import os
    scratch = output / 'workers' / str(os.getpid())
    scratch.mkdir(parents=True, exist_ok=True)
    WORKLOADS, EVALUATOR = run.load(scratch, run.read(output / 'inputs.json'))
    batch.install(batch.kernel(binary))


def execute(task):
    output, profile, arm, seed, budget, seal = task
    started = time.monotonic()
    value = run.run_arm(WORKLOADS, EVALUATOR, arm, seed, budget,
        [n for n in (1000, 2000, 4000, 8000) if n <= budget], formal.fitness(profile), factory)
    if value['attempts'] != budget or not value['start']['allProgressZero']:
        raise ValueError('Incomplete or noncold control')
    formal.seeded.audit_ties(value)
    value.update(profile=profile, protocolSha256=seal, tieRule=RULE,
                 parentArm=ARMS[arm][0], exploration=ARMS[arm][1],
                 wallSeconds=time.monotonic() - started)
    path = formal.path_for(output, profile, arm, seed, budget)
    partial = path.with_suffix(path.suffix + '.partial')
    with gzip.open(partial, 'wt') as stream:
        json.dump(value, stream)
    partial.replace(path)
    return row_for(value, path)


def row_for(value, path):
    return {'profile': value['profile'], 'arm': value['arm'], 'seed': value['seed'],
        'attempts': value['attempts'], 'score': value['cumulativeScore'],
        'positives': value['positives'], 'unknowns': value['unknowns'],
        'wallSeconds': value['wallSeconds'], 'path': str(path), 'traceSha256': run.sha(path)}


def audit(source, rows, protocol, seal):
    comparisons = 0
    for row in rows:
        trace = read_trace(row['path'])
        if (trace['protocolSha256'] != seal or trace['attempts'] != protocol['budget']
                or run.sha(row['path']) != row['traceSha256']):
            raise ValueError('Control trace identity changed')
        formal.seeded.audit_ties(trace)
        local = defaultdict(list)
        for d in trace['decisions']:
            local[d['workload']].append((d['candidate'], d['score']))
            if d['modelUpdated'] != (d['score'] is not None):
                raise ValueError('Unknown feedback was used as a reward')
        for parent in protocol['baselineArms']:
            old = read_trace(formal.path_for(source, row['profile'], parent, row['seed'], protocol['budget']))
            if (old['start'] != trace['start'] or old['fitness'] != trace['fitness'] or
                    old['protocolSha256'] != protocol['baselineProtocolSeal']):
                raise ValueError('Control cold states or objective differ from baseline')
            previous = defaultdict(list)
            for d in old['decisions']:
                previous[d['workload']].append((d['candidate'], d['score']))
            for wid, prefix in local.items():
                n = min(len(prefix), len(previous[wid]))
                if prefix[:n] != previous[wid][:n]:
                    raise ValueError('Control local GA/feedback differs from frozen baseline')
            comparisons += 1
    return {'baselineComparisons': comparisons, 'coldStartsMatched': True,
        'matchedGaCandidateAndScorePrefixes': True, 'seededTiesReproduced': True,
        'unknownsNeverZeroOrUpdates': True, 'completeBudgets': True}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--workers', type=int, default=2)
    args = parser.parse_args()
    source, output = args.source.resolve(), args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    baseline = run.read(source / 'formal-protocol.json')
    previous = run.read(source / 'formal-results.json')
    if previous['protocol'] != baseline or len(previous['rows']) != 210:
        raise ValueError('Baseline matrix incomplete')
    for p, digest in baseline['sourceHashes'].items():
        if run.sha(p) != digest:
            raise ValueError('Frozen runner changed: ' + p)
    if run.sha(source / 'inputs.json') != baseline['inputSha256']:
        raise ValueError('Frozen inputs changed')
    kernel_proof = run.read(source / 'compiled-kernel-equivalence.json')
    binary = Path(kernel_proof['binary'])
    if run.sha(binary) != baseline['kernelSha256']:
        raise ValueError('Numeric kernel changed')
    shutil.copy2(source / 'inputs.json', output / 'inputs.json')
    protocol = {'scope': 'Alpha-only diagnostic on previously examined frozen benchmark',
        'baselineProtocolSeal': hashlib.sha256(json.dumps(baseline, sort_keys=True).encode()).hexdigest(),
        'baselineProtocolSha256': run.sha(source / 'formal-protocol.json'),
        'sourceHashes': {**baseline['sourceHashes'], str(Path(__file__).resolve()): run.sha(__file__)},
        'inputSha256': baseline['inputSha256'], 'kernelSha256': baseline['kernelSha256'],
        'baselineArms': baseline['arms'], 'arms': list(ARMS),
        'parameters': {a: {'parent': p, 'exploration': alpha, 'initialBonus': 1.0}
                       for a, (p, alpha) in ARMS.items()},
        'budget': baseline['budget'], 'profiles': baseline['profiles'], 'seeds': baseline['seeds'],
        'tieRule': RULE, 'localSearch': 'ga', 'historicalRunsReplaced': False,
        'newMeasurementReferencesUsed': False}
    seal = hashlib.sha256(json.dumps(protocol, sort_keys=True).encode()).hexdigest()
    if (output / 'protocol.json').exists() and run.read(output / 'protocol.json') != protocol:
        raise ValueError('Cannot resume changed control protocol')
    batch.write(output / 'protocol.json', protocol)
    proof_path = output / 'proof.json'
    if proof_path.exists():
        if run.read(proof_path)['protocolSha256'] != seal:
            raise ValueError('Control proof belongs to another protocol')
    else:
        workloads, evaluator = run.load(output, run.read(output / 'inputs.json'))
        checks = check_alpha_only(workloads)
        fast = batch.kernel(binary)
        exact = []
        for profile, arm in itertools.product(baseline['profiles'], ARMS):
            batch.install(None)
            dense = run.run_arm(workloads, evaluator, arm, 1, 16, [16], formal.fitness(profile), factory)
            batch.install(fast)
            compiled = run.run_arm(workloads, evaluator, arm, 1, 16, [16], formal.fitness(profile), factory)
            if dense['decisions'] != compiled['decisions'] or dense['perWorkload'] != compiled['perWorkload']:
                raise ValueError('Compiled kernel changed control trajectory')
            exact.append({'profile': profile, 'arm': arm, 'exactDecisions': 16})
            print('PROOF', profile, arm, flush=True)
        batch.write(proof_path, {'protocolSha256': seal, 'alphaOnlyChecks': checks,
                                'numericKernelExactComparisons': exact})
        batch.install(None)
        del workloads, evaluator
    tasks, rows = [], []
    for profile, seed, arm in itertools.product(baseline['profiles'], baseline['seeds'], ARMS):
        path = formal.path_for(output, profile, arm, seed, baseline['budget'])
        if path.exists():
            value = read_trace(path)
            if value['protocolSha256'] != seal or value['attempts'] != baseline['budget']:
                raise ValueError('Existing control trace differs')
            rows.append(row_for(value, path))
        else:
            tasks.append((output, profile, arm, seed, baseline['budget'], seal))
    def status(complete=False):
        batch.write(output / 'status.json', {'complete': complete, 'completedRuns': len(rows),
                                           'totalRuns': 60, 'rows': rows})
    status()
    with ProcessPoolExecutor(max_workers=args.workers, mp_context=multiprocessing.get_context('spawn'),
                             initializer=initialize, initargs=(output, binary)) as pool:
        for future in as_completed([pool.submit(execute, task) for task in tasks]):
            row = future.result()
            rows.append(row)
            status()
            print('CONTROL', len(rows), '/60', row['profile'], row['arm'], row['seed'],
                  row['score'], round(row['wallSeconds'], 1), flush=True)
    checks = audit(source, rows, protocol, seal)
    if any(run.sha(p) != h for p, h in protocol['sourceHashes'].items()):
        raise ValueError('Source changed during control')
    batch.write(output / 'results.json', {'protocol': protocol, 'checks': checks, 'rows': rows})
    status(True)


if __name__ == '__main__':
    main()
