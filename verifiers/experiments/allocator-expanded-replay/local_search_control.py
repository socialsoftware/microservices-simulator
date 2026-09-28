"""Matched GA versus uniform-unseen local search; primary policies remain unchanged."""
import argparse
import collections
from concurrent.futures import ProcessPoolExecutor, as_completed
import copy
import gzip
import hashlib
import importlib.util
import json
import multiprocessing
from pathlib import Path
import random
import statistics
import sys

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('control_batch', HERE / 'batch.py')
batch = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = batch
spec.loader.exec_module(batch)
run = batch.run
import allocator
import study
from fitness import assess
from search import stable

ORIGINAL_STATES = study._new_states
LOCAL_SEARCH = 'ga'
WORKLOADS = FACTORY = None
EXCLUDED = {'23f9768282509c46a6a4a4fd62d51fa6bca64591ded719e9ab070257ba0ca09d',
            '242d3ef997d7a56426092ead4f39f934c23c4e209967f36a437f0d33eb650ad5',
            'ef019eb258c89262dddfeed7279c00a43b2620e2854274aefdefb4f644becf2a'}
FITNESS = run.configuration({'policy': 'weighted-criteria-v2',
                            'weights': {c: 1 for c in run.CRITERIA_V2}})


def states(workloads, seed, budget, fitness):
    result = ORIGINAL_STATES(workloads, seed, budget, fitness)
    for state in result.values():
        state['session'].strategy = LOCAL_SEARCH
    return result


study._new_states = states


def selected(collection):
    return [w for w in WORKLOADS if collection == 'full' or w['id'] not in EXCLUDED]


def initialize(source, output, binary):
    global WORKLOADS, FACTORY
    import os
    scratch = output / 'workers' / str(os.getpid())
    scratch.mkdir(parents=True, exist_ok=True)
    WORKLOADS, FACTORY = run.load(scratch, run.read(source / 'inputs.json'))
    if binary:
        batch.install(batch.kernel(binary))


def trajectory(workloads, arm, seed, budget, strategy):
    global LOCAL_SEARCH
    LOCAL_SEARCH = strategy
    points = sorted({p for p in (1000, 2000, 3000, 4000, 8000, budget) if p <= budget})
    value = run.run_arm(workloads, FACTORY, arm, seed, budget, points, FITNESS, run.make_policy)
    if value['attempts'] != budget:
        raise ValueError('Premature exhaustion or stall')
    return value


def execute(task):
    output, collection, arm, seed, budget, strategy, seal = task
    value = trajectory(selected(collection), arm, seed, budget, strategy)
    value.update(controlSeal=seal, collection=collection, localSearch=strategy)
    path = output / f'{collection}-{strategy}-{arm}-seed{seed}.json.gz'
    temp = path.with_suffix('.partial')
    with gzip.open(temp, 'wt') as stream:
        json.dump(value, stream)
    temp.replace(path)
    return {'collection': collection, 'localSearch': strategy, 'arm': arm, 'seed': seed,
            'score': value['cumulativeScore'], 'positives': value['positives'], 'path': str(path)}


def audit(output, source, protocol, seal):
    scratch = output / 'audit'
    scratch.mkdir(exist_ok=True)
    workloads, _ = run.load(scratch, run.read(source / 'inputs.json'))
    domains = {w['id']: w['domain'] for w in workloads}
    frozen = run.read(source / 'inputs.json')
    metadata = {r['id']: r for r in frozen['admission']}
    references = {r['workload']: run.read(r['reference'])['observations']
                  for part in frozen['partitions'] for r in part['workloads']}
    summaries = collections.defaultdict(lambda: collections.defaultdict(dict))
    expected_source = run.read(source / 'batch-protocol.json')
    for collection, budget in protocol['collections'].items():
        for strategy in ('ga', 'random'):
            for seed in protocol['seeds']:
                prefixes, starts = {}, set()
                for arm in run.ARMS:
                    path = (source / f'all-five-{arm}-seed{seed}-budget8000.json.gz'
                            if collection == 'full' and strategy == 'ga'
                            else output / f'{collection}-{strategy}-{arm}-seed{seed}.json.gz')
                    with gzip.open(path, 'rt') as stream:
                        value = json.load(stream)
                    if collection == 'full' and strategy == 'ga':
                        if (value['inputSha256'] != protocol['inputSha256']
                                or value['runnerSourceHashes'] != expected_source['runnerSourceHashes']):
                            raise ValueError('Reused GA protocol mismatch')
                    elif value['controlSeal'] != seal:
                        raise ValueError('Control protocol mismatch')
                    if (value['seed'] != seed or value['arm'] != arm or value['fitness'] != FITNESS
                            or value['attempts'] != budget or not value['start']['allProgressZero']
                            or hashlib.sha256(stable(value['decisions']).encode()).hexdigest()
                                != value['decisionSha256']):
                        raise ValueError('Trace mismatch')
                    starts.add(value['start']['stateSha256'])
                    seen = set()
                    local = collections.defaultdict(list)
                    progress = collections.defaultdict(lambda: {'allocated': 0, 'known': 0,
                        'unknown': 0, 'positives': 0, 'scoreSum': 0.0, 'bestScore': None})
                    score = positive = unknown = 0
                    positive_families = set()
                    checkpoints = []
                    for d in value['decisions']:
                        wid, key = d['workload'], d['candidate']
                        if ((wid, key) in seen or collection != 'full' and wid in EXCLUDED
                                or d['preUpdateProgress'] != progress[wid]):
                            raise ValueError('Repeated/out-of-scope candidate or noncausal progress')
                        seen.add((wid, key))
                        local[wid].append(key)
                        if assess(references[wid][key], FITNESS)['fitnessScore'] != d['score']:
                            raise ValueError('Feedback mismatch')
                        study._observe(progress[wid], d['score'])
                        if d['score'] is None:
                            unknown += 1
                        else:
                            score += d['score']
                            positive += d['score'] > 0
                            if d['score'] > 0:
                                positive_families.add(metadata[wid]['sagaFamily'])
                        if d['decision'] in protocol['checkpoints'][collection]:
                            checkpoints.append({'choices': d['decision'], 'score': score,
                                'positives': positive, 'unknowns': unknown,
                                'positiveFamilies': len(positive_families)})
                    if (score, positive, unknown) != (value['cumulativeScore'], value['positives'], value['unknowns']):
                        raise ValueError('Aggregate mismatch')
                    for wid, keys in local.items():
                        old = prefixes.get(wid, [])
                        common = min(len(old), len(keys))
                        if old[:common] != keys[:common]:
                            raise ValueError('Local prefixes differ between outer policies')
                        if len(keys) > len(old):
                            prefixes[wid] = keys
                        if strategy == 'random':
                            rng, drawn = random.Random(allocator._local_seed(seed, wid)), set()
                            for key in keys:
                                if domains[wid].sample_unseen(rng, drawn)['key'] != key:
                                    raise ValueError('Local random sequence differs from uniform unseen draws')
                                drawn.add(key)
                    summaries[collection][strategy].setdefault(arm, []).append({
                        'seed': seed, 'checkpoints': checkpoints, 'traceSha256': value['decisionSha256']})
                if len(starts) != 1:
                    raise ValueError('Cold states differ within collection/method/seed')
    aggregate = {}
    for collection, methods in summaries.items():
        aggregate[collection] = {}
        for strategy, arms in methods.items():
            aggregate[collection][strategy] = {}
            for arm, rows in arms.items():
                aggregate[collection][strategy][arm] = [{
                    'choices': n, **{k: statistics.mean(next(c[k] for c in r['checkpoints']
                        if c['choices'] == n) for r in rows)
                        for k in ('score', 'positives', 'unknowns', 'positiveFamilies')}}
                    for n in protocol['checkpoints'][collection]]
    result = {'status': 'COMPLETE', 'protocol': protocol, 'mean': aggregate, 'seeds': summaries,
        'checks': {'feedbackReassessed': True, 'causalProgress': True,
                   'coldStatesMatched': True, 'localPrefixesMatchedWithinMethod': True,
                   'randomPrefixesMatchUniformUnseenDraws': True, 'noRepeatedMeasuredCandidates': True},
        'scope': 'Post-hoc local-search control. Fixed original ID tie rules retained. The smaller collection excludes exactly three maps; it is not yet a balanced final sample.'}
    batch.write(output / 'aggregate.json', result)
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--kernel', type=Path, required=True)
    parser.add_argument('--workers', type=int, default=6)
    args = parser.parse_args()
    source, output, binary = args.source.resolve(), args.output.resolve(), args.kernel.resolve()
    output.mkdir(parents=True, exist_ok=True)
    receipt = run.read(source / 'compiled-kernel-equivalence.json')
    if run.sha(binary) != receipt['binarySha256'] or run.sha(source / 'inputs.json') != receipt['inputSha256']:
        raise ValueError('Numeric proof or inventory mismatch')
    protocol = {'inputSha256': receipt['inputSha256'], 'seeds': [1, 11, 29, 47, 73],
        'arms': run.ARMS, 'fitness': FITNESS,
        'collections': {'full': 8000, 'without-three-largest': 3000},
        'checkpoints': {'full': [1000, 2000, 4000, 8000], 'without-three-largest': [1000, 2000, 3000]},
        'excludedWorkloads': sorted(EXCLUDED), 'originalTieRulesRetained': True,
        'sourceHashes': {str(p): run.sha(p) for p in [Path(__file__), Path(batch.__file__),
                         Path(run.__file__), *sorted((run.HERE.parent / 'fixed-workload-ga').glob('*.py')),
                         run.HERE.parent / 'allocator-component-pilot/policies.py',
                         run.HERE.parent / 'allocator-order-hybrid-followup/study.py']},
        'kernelProofSha256': run.sha(source / 'compiled-kernel-equivalence.json')}
    seal = hashlib.sha256(stable(protocol).encode()).hexdigest()
    batch.write(output / 'protocol.json', protocol)
    initialize(source, output, None)
    batch.install(None)
    proof = []
    for collection in protocol['collections']:
        for arm in run.ARMS:
            batch.install(None)
            dense = trajectory(selected(collection), arm, 1, 64, 'random')
            batch.install(batch.kernel(binary))
            fast = trajectory(selected(collection), arm, 1, 64, 'random')
            if dense['decisions'] != fast['decisions']:
                raise ValueError('Numeric kernel changes local random control')
            proof.append({'collection': collection, 'arm': arm, 'exactDecisions': 64})
    batch.write(output / 'random-kernel-proof.json', {'comparisons': proof, 'controlSeal': seal})
    tasks, completed = [], []
    for collection, budget in protocol['collections'].items():
        for strategy in ('ga', 'random'):
            if collection == 'full' and strategy == 'ga':
                continue
            for seed in protocol['seeds']:
                for arm in run.ARMS:
                    path = output / f'{collection}-{strategy}-{arm}-seed{seed}.json.gz'
                    if path.exists():
                        with gzip.open(path, 'rt') as stream:
                            if json.load(stream)['controlSeal'] != seal:
                                raise ValueError('Resume protocol mismatch')
                        completed.append(str(path))
                    else:
                        tasks.append((output, collection, arm, seed, budget, strategy, seal))
    with ProcessPoolExecutor(max_workers=args.workers, mp_context=multiprocessing.get_context('spawn'),
                             initializer=initialize, initargs=(source, output, binary)) as pool:
        futures = [pool.submit(execute, task) for task in tasks]
        for future in as_completed(futures):
            value = future.result()
            completed.append(value['path'])
            batch.write(output / 'status.json', {'newRunsCompleted': len(completed),
                                                'totalNewRuns': 105, 'complete': False})
            print('CONTROL', len(completed), '/105', value['collection'], value['localSearch'],
                  value['arm'], value['seed'], value['score'], flush=True)
    if any(run.sha(p) != h for p, h in protocol['sourceHashes'].items()):
        raise ValueError('Source changed during control')
    audit(output, source, protocol, seal)
    batch.write(output / 'status.json', {'newRunsCompleted': 105, 'reusedRuns': 35,
                                       'auditedRuns': 140, 'complete': True})


if __name__ == '__main__':
    main()
