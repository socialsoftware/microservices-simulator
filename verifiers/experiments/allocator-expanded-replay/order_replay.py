"""Paired order-feature control on the corrected, frozen measurement benchmark.

Only SO, SPO and H1+O are new runs. Their parents retain the baseline alpha,
ridge, cumulative progress, seeded ties and local GA. Static S+O is normalized
as one block, keeping its norm and cold uncertainty equal to the parent S block.
"""
import argparse
from collections import Counter, defaultdict
from concurrent.futures import ProcessPoolExecutor, as_completed
import fcntl
import gzip
import itertools
import json
import math
import multiprocessing
import os
from pathlib import Path
from statistics import mean
import time

import measurement_replay as m
import order_features as order
from allocator import ContextualLinUcbPolicy, FeatureSpace, PROGRESS_FEATURES, ProgressFeatureSpace
from hybrid_linucb import HybridLinUcbPolicy
from seeded_ties import SeededTiePolicy

run, formal, batch = m.run, m.formal, m.batch
require, seal, save_fixed = m.require, m.seal, m.save_fixed
OUT = m.TARGET / 'allocator-order-replay-2026-09-28'
PARENTS = {'SO': 'S-cal', 'SPO': 'SP-cal', 'H1+O': 'H1-cal'}
WORKLOADS = EVALUATOR = None


class FixedBiasUnitStructureOrderSpace:
    """Seven bounded order counts join the non-bias structural unit block."""

    def __init__(self, profiles, include_progress=False):
        self.full = FeatureSpace(profiles)
        self.structural_count = len(self.full.names) - len(PROGRESS_FEATURES)
        self.include_progress = include_progress
        self.names = [*self.full.names[:self.structural_count], *order.ORDER_FEATURES]
        if include_progress:
            self.names.extend(self.full.names[self.structural_count:])

    def vector(self, profile, progress):
        features = profile.get('orderFeatures')
        require(isinstance(features, dict) and set(features) == set(order.ORDER_FEATURES),
                'Complete explicit order features are required')
        extra = [features[name] for name in order.ORDER_FEATURES]
        require(all(type(v) in (int, float) and math.isfinite(v) and 0 <= v < 1 for v in extra),
                'Invalid bounded order coordinate')
        full = self.full.vector(profile, progress)
        static = full[1:self.structural_count] + extra
        norm = math.sqrt(math.fsum(v * v for v in static))
        require(norm > 0, 'Static block has zero norm')
        vector = [1.0, *(v / norm for v in static)]
        return vector + full[self.structural_count:] if self.include_progress else vector


def factory(arm, states, seed):
    profiles = {wid: s['profile'] for wid, s in states.items()}
    space = FixedBiasUnitStructureOrderSpace(profiles, include_progress=arm == 'SPO')
    if arm in ('SO', 'SPO'):
        model = ContextualLinUcbPolicy(space, 1 / math.sqrt(2), 1.0)
    elif arm == 'H1+O':
        model = HybridLinUcbPolicy(space, ProgressFeatureSpace(), states,
                                  exploration=math.sqrt(2 / 3), ridge=1.0)
    else:
        raise ValueError('Unknown order arm: ' + arm)
    return SeededTiePolicy(model, seed)


def augment(workloads, profiles):
    require({w['id'] for w in workloads} == set(profiles), 'Order/workload identities differ')
    return [{**w, 'profile': order.augment_profile(w['profile'], profiles[w['id']])}
            for w in workloads]


def extract(frozen, workloads):
    """Use co-generated metadata, with receipt-pinned recovery for a trimmed package."""
    cache, sources, profiles, deferred, hashes = {}, {}, {}, [], {}
    by_id = {w['id']: w for w in workloads}
    for part in frozen['partitions']:
        for row in part['workloads']:
            paths = tuple(str(run.local(p)) for p in
                          (part.get('interactionFiles') or [row['interactionFile']]))
            if any(not Path(p).with_name('scenario-catalog-manifest.json').exists() for p in paths):
                deferred.append((row, paths))
                continue
            if paths not in cache:
                cache[paths] = order.load_compatible_structural_metadata(paths)
            metadata = cache[paths]
            wid = row['workload']
            profiles[wid] = order.order_profile(by_id[wid]['domain'].workload, metadata)
            sources[wid] = metadata['source']
    for row, paths in deferred:
        require(len(paths) == 1, 'Cannot recover mixed missing metadata')
        receipt = Path(paths[0]).parent.parent / 'source-package-hashes.json'
        recorded = run.read(receipt)
        require(recorded['interactions'] == run.sha(paths[0]), 'Recovery interaction seal differs')
        require(recorded['workloads'] == run.sha(row['workloadFile']), 'Recovery workload seal differs')
        compatible = [meta for meta in cache.values()
                      if meta['source']['interactionsSha256'] == recorded['interactions']
                      and meta['source']['sagasSha256'] == recorded['sagas']]
        require(bool(compatible), 'Missing exact receipt-pinned Saga/access metadata')
        metadata = compatible[0]
        wid = row['workload']
        profiles[wid] = order.order_profile(by_id[wid]['domain'].workload, metadata)
        sources[wid] = {**metadata['source'], 'recoveredViaExactRoleHashes': str(receipt),
                        'receiptSha256': run.sha(receipt)}
        hashes[str(receipt)] = run.sha(receipt)
    for source in sources.values():
        for package in source['packages']:
            for role in ('manifest', 'sagas', 'interactions'):
                hashes[package[role]] = package[role + 'Sha256']
    require(len(profiles) == len(workloads), 'Incomplete order extraction')
    return {'profiles': profiles, 'sources': sources, 'sourceHashes': hashes,
            'coverage': dict(Counter(p['coverage']['status'] for p in profiles.values())),
            'staticKnowledge': dict(Counter(p['coverage']['staticKnowledge'] for p in profiles.values())),
            'recoveredPackages': len(deferred), 'outcomeInputs': False}


def geometry_checks(workloads):
    from transfer import _zero_progress
    from policies import FixedBiasUnitStructureFeatureSpace
    profiles = {w['id']: w['profile'] for w in workloads}
    states = {wid: {'profile': p, 'progress': _zero_progress()} for wid, p in profiles.items()}
    cold = {}
    for arm, parent in PARENTS.items():
        new, old = factory(arm, states, 1), m.factory(parent, states, 1)
        gaps = []
        for wid in states:
            nc, oc = new.select({wid}, states), old.select({wid}, states)
            require(math.isclose(nc[1]['uncertainty'], oc[1]['uncertainty'], abs_tol=1e-12),
                    'Cold exploration magnitude changed')
            require(nc[1]['estimate'] == oc[1]['estimate'] == 0, 'Noncold model')
            if arm == 'H1+O':
                require(nc[2]['local'] == oc[2]['local'], 'Hybrid local progress changed')
            gaps.append(abs(nc[1]['uncertainty'] - oc[1]['uncertainty']))
        cold[arm] = {'workloads': len(states), 'maximumBonusDifference': max(gaps)}
    old_space = FixedBiasUnitStructureFeatureSpace(profiles, True)
    new_space = FixedBiasUnitStructureOrderSpace(profiles, True)
    progress = {'allocated': 9, 'known': 7, 'unknown': 2, 'positives': 3, 'scoreSum': 11, 'bestScore': 6}
    for p in profiles.values():
        ov, nv = old_space.vector(p, progress), new_space.vector(p, progress)
        require(ov[-len(PROGRESS_FEATURES):] == nv[-len(PROGRESS_FEATURES):], 'Progress block changed')
        require(math.isclose(math.fsum(v*v for v in nv[1:-len(PROGRESS_FEATURES)]), 1, abs_tol=1e-12),
                'Static S+O norm differs')
        zero = {**p, 'orderFeatures': dict.fromkeys(order.ORDER_FEATURES, 0.0)}
        zv = new_space.vector(zero, progress)
        require(zv[:new_space.structural_count] == ov[:new_space.structural_count],
                'Zero-order control changed existing structural coordinates')
    by_s, by_so = defaultdict(list), defaultdict(list)
    s_space = FixedBiasUnitStructureFeatureSpace(profiles)
    so_space = FixedBiasUnitStructureOrderSpace(profiles)
    for wid, p in profiles.items():
        by_s[seal(s_space.vector(p, _zero_progress()))].append(wid)
        by_so[seal(so_space.vector(p, _zero_progress()))].append(wid)
    return {'coldBonuses': cold, 'progressUnchanged': True, 'zeroOrderReducesToParent': True,
            'distinctStructuralVectors': len(by_s), 'distinctStructureOrderVectors': len(by_so),
            'structuralCollisions': [ids for ids in by_s.values() if len(ids) > 1],
            'remainingCollisions': [ids for ids in by_so.values() if len(ids) > 1]}


def prepare():
    m.check_disk()
    OUT.mkdir(exist_ok=True)
    cohorts = {}
    for cohort in ('small', 'full'):
        source, directory = m.OUT / cohort, OUT / cohort
        directory.mkdir(exist_ok=True)
        baseline = run.read(source / 'results.json')
        require(run.read(source / 'status.json')['complete'] and len(baseline['rows']) == 40,
                'Corrected baseline incomplete')
        previous = baseline['protocol']
        m.verify_hashes(previous['sourceHashes'])
        require(run.sha(source / 'inputs.json') == previous['inputSha256'], 'Baseline inputs changed')
        frozen = run.read(source / 'inputs.json')
        save_fixed(directory / 'inputs.json', frozen)
        workloads, evaluator = run.load(directory, frozen)
        require(not evaluator().revealed, 'Initialization revealed outcomes')
        extraction = extract(frozen, workloads)
        save_fixed(directory / 'order-profiles.json', extraction)
        workloads = augment(workloads, extraction['profiles'])
        geometry = geometry_checks(workloads)
        binary = Path(previous['kernel'])
        require(run.sha(binary) == previous['kernelSha256'], 'Kernel differs')
        hashes = {**previous['sourceHashes'], **extraction['sourceHashes'],
                  str(Path(__file__).resolve()): run.sha(__file__),
                  str(Path(order.__file__).resolve()): run.sha(order.__file__),
                  str(source / 'results.json'): run.sha(source / 'results.json'),
                  str(run.MASTER / 'master-inventory-2026-09-28.json'):
                      run.sha(run.MASTER / 'master-inventory-2026-09-28.json')}
        for row in baseline['rows']:
            if row['arm'] in PARENTS.values(): hashes[row['path']] = row['traceSha256']
        protocol = {'scope': 'Exploratory paired order-feature control; no held-out claim',
            'inputSha256': run.sha(directory / 'inputs.json'), 'sourceHashes': hashes,
            'orderProfilesSha256': run.sha(directory / 'order-profiles.json'),
            'kernel': str(binary), 'kernelSha256': previous['kernelSha256'],
            'baselineDirectory': str(source), 'budget': previous['budget'], 'seeds': previous['seeds'],
            'arms': list(PARENTS), 'parents': PARENTS, 'profiles': ['all-five'], 'fitness': m.FIT,
            'localSearch': 'ga', 'tieRule': formal.seeded.RULE,
            'staticScaling': 'Bias=1; concatenate original non-bias S with seven bounded O counts, then unit L2',
            'alpha': {'SO': 1/math.sqrt(2), 'SPO': 1/math.sqrt(2), 'H1+O': math.sqrt(2/3)},
            'ridge': 1, 'progressChanged': False, 'parametersSelectedFromAlphaResults': False}
        save_fixed(directory / 'protocol.json', protocol)
        if not (directory / 'proof.json').exists():
            comparisons = []
            for arm in PARENTS:
                batch.install(None)
                dense = run.run_arm(workloads, evaluator, arm, 1, 16, [16], m.FIT, factory)
                batch.install(batch.kernel(binary))
                fast = run.run_arm(workloads, evaluator, arm, 1, 16, [16], m.FIT, factory)
                require(dense['decisions'] == fast['decisions'] and dense['perWorkload'] == fast['perWorkload'],
                        'Compiled kernel changes order trajectory')
                formal.seeded.audit_ties(fast)
                comparisons.append({'arm': arm, 'exactDecisions': 16})
            save_fixed(directory / 'proof.json', {'protocolSha256': seal(protocol), 'geometry': geometry,
                                                'denseCompiled': comparisons, 'passed': True})
        batch.install(None)
        require(run.read(directory / 'proof.json')['protocolSha256'] == seal(protocol), 'Proof mismatch')
        cohorts[cohort] = {'workloads': len(workloads), 'budget': previous['budget'], 'runs': 15,
            'extractionCoverage': extraction['coverage'], 'staticKnowledge': extraction['staticKnowledge'],
            'distinctS': geometry['distinctStructuralVectors'], 'distinctSO': geometry['distinctStructureOrderVectors'],
            'protocolSha256': run.sha(directory / 'protocol.json'), 'proofSha256': run.sha(directory / 'proof.json')}
        print('PREPARED', cohort, cohorts[cohort], flush=True)
    save_fixed(OUT / 'preparation.json', {'ready': True, 'totalRuns': 30, 'cohorts': cohorts})


def initialize(directory, binary):
    global WORKLOADS, EVALUATOR
    scratch = directory / 'workers' / str(os.getpid())
    scratch.mkdir(parents=True, exist_ok=True)
    workloads, EVALUATOR = run.load(scratch, run.read(directory / 'inputs.json'))
    WORKLOADS = augment(workloads, run.read(directory / 'order-profiles.json')['profiles'])
    batch.install(batch.kernel(binary))


def execute(task):
    directory, arm, seed, budget, protocol_seal = task
    m.check_disk()
    started = time.monotonic()
    value = run.run_arm(WORKLOADS, EVALUATOR, arm, seed, budget,
        [n for n in (1000, 2000, 4000, 8000) if n <= budget], m.FIT, factory)
    require(value['attempts'] == budget and value['start']['allProgressZero'], 'Incomplete/noncold run')
    formal.seeded.audit_ties(value)
    value.update(profile='all-five', protocolSha256=protocol_seal, wallSeconds=time.monotonic()-started)
    path = formal.path_for(directory, 'all-five', arm, seed, budget)
    partial = path.with_suffix(path.suffix + '.partial')
    with gzip.open(partial, 'wt') as stream: json.dump(value, stream)
    partial.replace(path)
    return m.row_for(value, path)


def audit(directory, protocol, rows):
    require(len(rows) == 15 and {(r['arm'], r['seed']) for r in rows} ==
            set(itertools.product(PARENTS, protocol['seeds'])), 'Incomplete comparison matrix')
    references = {w['workload']: run.read(w['reference']) for p in run.read(directory/'inputs.json')['partitions']
                  for w in p['workloads']}
    comparisons = 0
    for seed in protocol['seeds']:
        starts, prefixes = set(), {}
        values = []
        for row in rows:
            if row['seed'] != seed: continue
            require(run.sha(row['path']) == row['traceSha256'], 'Trace hash differs')
            value = m.ucb.read_trace(row['path'])
            require(value['protocolSha256'] == seal(protocol), 'Trace protocol mismatch')
            require(value['seed'] == seed and value['arm'] == row['arm'], 'Trace identity mismatch')
            values.append(value)
            old = m.ucb.read_trace(formal.path_for(Path(protocol['baselineDirectory']), 'all-five',
                PARENTS[row['arm']], seed, protocol['budget']))
            require(old['start'] == value['start'], 'Parent cold GA state differs')
            values.append(old)
            comparisons += 1
        for value in values:
            require(value['start']['allProgressZero'] and value['fitness'] == m.FIT, 'Noncold/changed fitness')
            require(value['attempts'] == len(value['decisions']) == protocol['budget'], 'Incomplete budget')
            formal.seeded.audit_ties(value)
            starts.add(value['start']['stateSha256'])
            local = defaultdict(list)
            total = positives = unknowns = 0
            for d in value['decisions']:
                expected = m.assess(references[d['workload']]['observations'][d['candidate']], m.FIT)['fitnessScore']
                require(expected == d['score'] and d['modelUpdated'] == (expected is not None), 'Feedback/update mismatch')
                local[d['workload']].append((d['candidate'], expected))
                total += expected or 0
                positives += expected is not None and expected > 0
                unknowns += expected is None
            require((total, positives, unknowns) == (value['cumulativeScore'], value['positives'], value['unknowns']),
                    'Recomputed totals differ')
            for wid, prefix in local.items():
                previous = prefixes.get(wid, [])
                n = min(len(previous), len(prefix))
                require(previous[:n] == prefix[:n], 'Local GA candidate/feedback prefix changed')
                if len(prefix) > len(previous): prefixes[wid] = prefix
        require(len(starts) == 1, 'Initial states differ across arms')
    return {'passed': True, 'baselineComparisons': comparisons, 'coldStartsMatched': True,
        'matchedGaCandidateAndScorePrefixes': True, 'completeBudgets': True,
        'seededTiesReproduced': True, 'feedbackMatchesCorrectedMaps': True, 'unknownsNeverZeroOrUpdates': True}


def run_cohort(cohort, workers):
    directory = OUT / cohort
    protocol = run.read(directory / 'protocol.json')
    m.verify_hashes(protocol['sourceHashes'])
    for name, key in [('inputs.json','inputSha256'), ('order-profiles.json','orderProfilesSha256')]:
        require(run.sha(directory/name) == protocol[key], 'Frozen input changed')
    require(run.sha(protocol['kernel']) == protocol['kernelSha256'], 'Kernel changed')
    proof = run.read(directory/'proof.json')
    require(proof['passed'] and proof['protocolSha256'] == seal(protocol), 'Proof differs')
    rows, tasks = [], []
    for seed, arm in itertools.product(protocol['seeds'], PARENTS):
        path = formal.path_for(directory,'all-five',arm,seed,protocol['budget'])
        if path.exists():
            value = m.ucb.read_trace(path)
            require(value['protocolSha256'] == seal(protocol), 'Refusing resume under changed protocol')
            rows.append(m.row_for(value,path))
        else: tasks.append((directory,arm,seed,protocol['budget'],seal(protocol)))
    def status(complete=False):
        batch.write(directory/'status.json',{'complete':complete,'completedRuns':len(rows),'totalRuns':15,'rows':rows})
    status()
    with ProcessPoolExecutor(max_workers=workers, mp_context=multiprocessing.get_context('spawn'),
        initializer=initialize, initargs=(directory,Path(protocol['kernel']))) as pool:
        for future in as_completed([pool.submit(execute,t) for t in tasks]):
            row = future.result(); rows.append(row); status()
            print('ORDER',cohort,len(rows),'/15',row['arm'],row['seed'],row['score'],flush=True)
    checks = audit(directory,protocol,rows)
    m.verify_hashes(protocol['sourceHashes'])
    batch.write(directory/'results.json',{'protocol':protocol,'rows':rows,'checks':checks})
    status(True)


def summarize():
    metadata = {w['id']:w for w in run.read(run.MASTER/'master-inventory-2026-09-28.json')['qualifiedWorkloads']}
    full_ids = {w['workload'] for p in run.read(OUT/'full/inputs.json')['partitions'] for w in p['workloads']}
    small_ids = {w['workload'] for p in run.read(OUT/'small/inputs.json')['partitions'] for w in p['workloads']}
    giants = full_ids-small_ids
    def metrics(v):
        positive = [w for w in v['perWorkload'] if w['positiveDiscoveries']]
        return {'score':v['cumulativeScore'],'positives':v['positives'],'unknowns':v['unknowns'],
            'positiveFamilies':len({metadata[w['workload']]['family'] for w in positive}),
            'giantChoices':sum(d['workload'] in giants for d in v['decisions'])}
    output = {}
    for cohort in ('small','full'):
        directory = OUT/cohort
        results = run.read(directory/'results.json'); protocol = results['protocol']
        require(results['checks']['passed'] and run.read(directory/'status.json')['complete'], 'Unaudited results')
        paired = []
        for row in results['rows']:
            now = m.ucb.read_trace(row['path'])
            old = m.ucb.read_trace(formal.path_for(Path(protocol['baselineDirectory']),'all-five',
                PARENTS[row['arm']],row['seed'],protocol['budget']))
            paired.append({'arm':row['arm'],'parent':PARENTS[row['arm']],'seed':row['seed'],
                           'before':metrics(old),'after':metrics(now)})
        summaries = []
        for arm in PARENTS:
            rs = [r for r in paired if r['arm'] == arm]
            summaries.append({'arm':arm,'parent':PARENTS[arm],
                **{k:{side:mean(r[side][k] for r in rs) for side in ('before','after')}
                   for k in rs[0]['before']},
                'scoreWinsTiesLosses':[sum(r['after']['score']>r['before']['score'] for r in rs),
                    sum(r['after']['score']==r['before']['score'] for r in rs),
                    sum(r['after']['score']<r['before']['score'] for r in rs)]})
        output[cohort] = {'summaries':summaries,'pairedRows':paired,'checks':results['checks'],
                          'resultsSha256':run.sha(directory/'results.json')}
    batch.write(OUT/'comparison.json',{'scope':'All-five objective, five paired seeds, order only', 'cohorts':output})


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('stage',choices=['prepare','run','summarize'])
    parser.add_argument('--workers',type=int,choices=range(1,4),default=3)
    args = parser.parse_args()
    if args.stage == 'prepare': prepare()
    elif args.stage == 'summarize': summarize()
    else:
        with (OUT/'run.lock').open('a') as lock:
            fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
            require(run.read(OUT/'preparation.json')['ready'], 'Preparation incomplete')
            m.check_disk()
            batch.write(OUT/'launch.json',{'pid':os.getpid(),'workers':args.workers,'startedAtUnix':time.time(),
                'preparationSha256':run.sha(OUT/'preparation.json')})
            try:
                for cohort in ('small','full'): run_cohort(cohort,args.workers)
                summarize()
                batch.write(OUT/'completion.json',{'complete':True,'totalRuns':30,
                    'comparisonSha256':run.sha(OUT/'comparison.json')})
            except Exception as error:
                batch.write(OUT/'error.json',{'type':type(error).__name__,'message':str(error),'pid':os.getpid()})
                raise


if __name__ == '__main__': main()
