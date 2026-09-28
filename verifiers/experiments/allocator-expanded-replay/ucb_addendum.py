"""Independent-UCB addendum to frozen formal runs; never replaces their artifacts."""
import argparse
from collections import defaultdict
from concurrent.futures import ProcessPoolExecutor, as_completed
import gzip
import hashlib
import itertools
import json
import math
import multiprocessing
from pathlib import Path
import random
import shutil
from statistics import mean, stdev

import formal_replay as formal
from allocator import AdaptiveUcbPolicy
from seeded_ties import ABS_TOL, REL_TOL, RULE, SeededTiePolicy

run = formal.run
ARM = 'UCB-independent'
WORKLOADS = EVALUATOR = None


class SeededUcb(SeededTiePolicy):
    """Batch the existing UCB formula, avoiding a repeated global count per arm."""
    def __init__(self, seed):
        super().__init__(AdaptiveUcbPolicy(1.0), seed)

    def select(self, active, states):
        total = sum(s['progress']['allocated'] for s in states.values())
        rows = []
        for wid in sorted(active):
            p = states[wid]['progress']
            estimate = p['scoreSum'] / p['known'] if p['known'] else 0.0
            bonus = self.model.exploration * math.sqrt(math.log(total + 2.0) /
                                                       (p['allocated'] + 1.0))
            rows.append((wid, {'index': estimate + bonus, 'estimate': estimate,
                              'uncertainty': bonus}, None))
        maximum = max(row[1]['index'] for row in rows)
        tied = [r for r in rows if math.isclose(r[1]['index'], maximum,
                                               rel_tol=REL_TOL, abs_tol=ABS_TOL)]
        if len(tied) > 1:
            self.draws += 1
            wid, choice, context = self.rng.choice(tied)
        else:
            wid, choice, context = tied[0]
        return wid, {**choice, 'tie': {'rule': RULE, 'candidates': [r[0] for r in tied],
            'maximumIndex': maximum, 'selectedGap': maximum - choice['index'],
            'draw': self.draws if len(tied) > 1 else None}}, context


def factory(arm, states, seed):
    if arm != ARM:
        raise ValueError('UCB addendum accepts only independent UCB')
    return SeededUcb(seed)


def verify_adapter():
    """Exact equivalence with the original selector plus the common tie wrapper."""
    from transfer import _observe
    comparisons = 0
    for seed in (1, 11, 29, 47, 73):
        rng = random.Random(seed)
        states = {str(i): {'progress': {'allocated': 0, 'known': 0, 'unknown': 0,
            'positives': 0, 'scoreSum': 0.0, 'bestScore': None}} for i in range(12)}
        expected = SeededTiePolicy(AdaptiveUcbPolicy(1.0), seed)
        actual = SeededUcb(seed)
        for n in range(300):
            # Exhausted/inactive arms still contribute to the global allocation count.
            active = set(states) if n < 150 else set(list(states)[3:])
            a, b = expected.select(active, states), actual.select(active, states)
            if a != b:
                raise ValueError('Batched UCB differs from canonical UCB with seeded ties')
            reward = rng.choice((None, 0.0, 0.0, 1.0, 2.0, 7.0))
            _observe(states[a[0]]['progress'], reward)
            if expected.update(a[0], a[2], reward) != actual.update(b[0], b[2], reward):
                raise ValueError('UCB update contract differs')
            comparisons += 1
        if expected.summary() != actual.summary():
            raise ValueError('UCB model summaries differ')
    return {'exactCanonicalDecisions': comparisons, 'seeds': [1, 11, 29, 47, 73],
            'feedbackIncludes': ['unavailable', 'negative', 'positive'],
            'inactiveArmsIncludedInGlobalClock': True}


def write(path, value):
    partial = path.with_suffix(path.suffix + '.partial')
    partial.write_text(json.dumps(value, indent=2) + '\n')
    partial.replace(path)


def initialize(output):
    global WORKLOADS, EVALUATOR
    import os
    scratch = output / 'workers' / str(os.getpid())
    scratch.mkdir(parents=True, exist_ok=True)
    WORKLOADS, EVALUATOR = run.load(scratch, run.read(output / 'inputs.json'))


def execute(task):
    output, profile, seed, budget, seal = task
    value = run.run_arm(WORKLOADS, EVALUATOR, ARM, seed, budget,
        [n for n in (1000, 2000, 4000, 8000) if n <= budget], formal.fitness(profile), factory)
    if value['attempts'] != budget or not value['start']['allProgressZero']:
        raise ValueError('Incomplete or noncold run')
    formal.seeded.audit_ties(value)
    value.update(profile=profile, protocolSha256=seal, tieRule=RULE)
    path = formal.path_for(output, profile, ARM, seed, budget)
    partial = path.with_suffix(path.suffix + '.partial')
    with gzip.open(partial, 'wt') as stream:
        json.dump(value, stream)
    partial.replace(path)
    return {'profile': profile, 'arm': ARM, 'seed': seed, 'attempts': budget,
            'score': value['cumulativeScore'], 'positives': value['positives'],
            'unknowns': value['unknowns'], 'path': str(path), 'traceSha256': run.sha(path)}


def read_trace(path):
    with gzip.open(path, 'rt') as stream:
        return json.load(stream)


def audit_against_frozen(source, output, rows, protocol, seal):
    audited = 0
    for row in rows:
        trace = read_trace(row['path'])
        if trace['protocolSha256'] != seal or trace['attempts'] != protocol['budget']:
            raise ValueError('UCB trace protocol mismatch')
        formal.seeded.audit_ties(trace)
        local = defaultdict(list)
        for d in trace['decisions']:
            local[d['workload']].append((d['candidate'], d['score']))
            if d['modelUpdated'] != (d['score'] is not None):
                raise ValueError('UCB unknown feedback update mismatch')
        for arm in protocol['baselineArms']:
            path = formal.path_for(source, row['profile'], arm, row['seed'], protocol['budget'])
            old = read_trace(path)
            if (old['start'] != trace['start'] or old['fitness'] != trace['fitness'] or
                    old['protocolSha256'] != protocol['baselineProtocolSeal']):
                raise ValueError('UCB initial state, fitness or baseline protocol differs')
            prefixes = defaultdict(list)
            for d in old['decisions']:
                prefixes[d['workload']].append((d['candidate'], d['score']))
            for wid, prefix in local.items():
                n = min(len(prefix), len(prefixes[wid]))
                if prefix[:n] != prefixes[wid][:n]:
                    raise ValueError('Local GA candidate/score prefix differs from baseline')
            audited += 1
    return {'coldStartsMatched': True, 'fitnessMatched': True, 'completeBudgets': True,
            'seededTieDrawsReproduced': True, 'noRepeatedCandidates': True,
            'unknownsNeverZeroOrModelUpdates': True, 'matchedGaCandidateAndScorePrefixes': True,
            'baselineRunsCompared': audited}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--workers', type=int, default=3)
    args = parser.parse_args()
    source, output = args.source.resolve(), args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    baseline = run.read(source / 'formal-protocol.json')
    old_results = run.read(source / 'formal-results.json')
    if old_results['protocol'] != baseline or len(old_results['rows']) != 210:
        raise ValueError('Baseline is not the completed frozen formal matrix')
    for path, digest in baseline['sourceHashes'].items():
        if run.sha(path) != digest:
            raise ValueError('Baseline source changed: ' + path)
    if run.sha(source / 'inputs.json') != baseline['inputSha256']:
        raise ValueError('Baseline inputs changed')
    shutil.copy2(source / 'inputs.json', output / 'inputs.json')
    protocol = {'baselineDirectory': str(source), 'baselineProtocolSha256': run.sha(source / 'formal-protocol.json'),
        'baselineProtocolSeal': hashlib.sha256(json.dumps(baseline, sort_keys=True).encode()).hexdigest(),
        'inputSha256': baseline['inputSha256'], 'sourceHashes': {**baseline['sourceHashes'],
            str(Path(__file__).resolve()): run.sha(__file__)}, 'baselineArms': baseline['arms'],
        'arms': [ARM], 'profiles': baseline['profiles'], 'seeds': baseline['seeds'],
        'budget': baseline['budget'], 'localSearch': 'ga', 'tieRule': RULE,
        'exploration': 1.0, 'forcedInitialSweep': False, 'unknownsChangeMean': False,
        'unknownsCountInExploration': True, 'rewardNormalization': None,
        'historicalRunsReplaced': False, 'adapterProof': verify_adapter()}
    seal = hashlib.sha256(json.dumps(protocol, sort_keys=True).encode()).hexdigest()
    if (output / 'protocol.json').exists() and run.read(output / 'protocol.json') != protocol:
        raise ValueError('Cannot resume a changed addendum')
    write(output / 'protocol.json', protocol)
    tasks, rows = [], []
    for profile, seed in itertools.product(protocol['profiles'], protocol['seeds']):
        path = formal.path_for(output, profile, ARM, seed, protocol['budget'])
        if path.exists():
            v = read_trace(path)
            if v['protocolSha256'] != seal or v['attempts'] != protocol['budget']:
                raise ValueError('Cannot reuse incompatible trace')
            rows.append({'profile': profile, 'arm': ARM, 'seed': seed, 'attempts': v['attempts'],
                'score': v['cumulativeScore'], 'positives': v['positives'], 'unknowns': v['unknowns'],
                'path': str(path), 'traceSha256': run.sha(path)})
        else:
            tasks.append((output, profile, seed, protocol['budget'], seal))
    def status(complete=False):
        write(output / 'status.json', {'complete': complete, 'completedRuns': len(rows),
                                     'totalRuns': 30, 'rows': rows})
    status()
    with ProcessPoolExecutor(max_workers=args.workers, mp_context=multiprocessing.get_context('spawn'),
                             initializer=initialize, initargs=(output,)) as pool:
        for future in as_completed([pool.submit(execute, t) for t in tasks]):
            row = future.result()
            rows.append(row)
            status()
            print(len(rows), '/30', row['profile'], row['seed'], row['score'], flush=True)
    checks = audit_against_frozen(source, output, rows, protocol, seal)
    if any(run.sha(p) != h for p, h in protocol['sourceHashes'].items()):
        raise ValueError('Source changed during replay')
    write(output / 'results.json', {'protocol': protocol, 'checks': checks, 'rows': rows})
    status(True)


if __name__ == '__main__':
    main()
