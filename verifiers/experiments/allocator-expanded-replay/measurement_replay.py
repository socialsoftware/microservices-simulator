"""Prepare and replay a separately sealed synchronous-copy measurement revision.

The same eight baseline allocators use the new observations. Hybrid alpha controls
must finish first; their outcomes do not select this experiment's parameters.
"""
import argparse
from collections import Counter, defaultdict
from concurrent.futures import ProcessPoolExecutor, as_completed
import copy
import fcntl
import gzip
import hashlib
import itertools
import json
import multiprocessing
import os
from pathlib import Path
import shutil
from statistics import mean
import time

import ucb_addendum as ucb
from fitness import assess, components
from runtime import retained_attempt

formal, run = ucb.formal, ucb.run
batch = formal.batch
ROOT = run.ROOT
TARGET = ROOT / 'verifiers/target'
OUT = TARGET / 'allocator-measurement-replay-2026-09-28'
EVIDENCE = ROOT / 'docs/verifiers-impl/evidence/saga-copy-transport-2026-09-28'
ARMS = (*run.ARMS, ucb.ARM)
FIT = formal.fitness('all-five')
WORKLOADS = EVALUATOR = None


def require(condition, message):
    if not condition:
        raise ValueError(message)


def seal(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True).encode()).hexdigest()


def save_fixed(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists():
        require(run.read(path) == value, 'Refusing changed frozen artifact: ' + str(path))
    else:
        batch.write(path, value)


def verify_hashes(hashes):
    for path, expected in hashes.items():
        require(run.sha(path) == expected, 'Frozen artifact changed: ' + str(path))


def check_disk():
    require(shutil.disk_usage(ROOT).free >= 2 * 1024**3, 'Less than 2 GiB free; no new run started')


def factory(arm, states, seed):
    return ucb.factory(arm, states, seed) if arm == ucb.ARM else formal.seeded.factory(arm, states, seed)


def score_counts(observations, profile):
    scores = [assess(o, formal.fitness(profile))['fitnessScore'] for o in observations]
    return {'positive': sum(s is not None and s > 0 for s in scores),
            'negative': sum(s == 0 for s in scores), 'unavailable': scores.count(None),
            'score': sum(s for s in scores if s is not None)}


def prepare():
    check_disk()
    OUT.mkdir(parents=True, exist_ok=True)
    selection = run.read(EVIDENCE / 'selection.json')
    audit = run.read(EVIDENCE / 'audit.json')
    measured = run.read(EVIDENCE / 'results.json')
    require(audit['selected'] == audit['completed'] == measured['completed'] == 270,
            'Synchronous measurement cohort is incomplete')
    require(run.sha(EVIDENCE / 'selection.json') == audit['selectionSha256'], 'Selection changed')
    require(audit['otherCriteriaChangedOnCompletedExecutions'] == 0, 'Other criterion changes need review')
    selected = {(r['workload'], r['candidate']): r for r in selection['selected']}
    result_rows = {(r['workload'], r['candidate']): r for r in measured['rows']}
    audit_rows = {(r['workload'], r['candidate']): r for r in audit['rows']}
    require(len(selected) == 270 and set(selected) == set(result_rows) == set(audit_rows),
            'Selection/result identities differ')
    grouped = defaultdict(list)
    for identity, row in selected.items():
        grouped[row['workload']].append((identity, row))
    full_source = TARGET / 'allocator-formal-full-2026-09-28'
    full_inputs = run.read(full_source / 'inputs.json')
    metadata = {w['workload']: w for p in full_inputs['partitions'] for w in p['workloads']}
    replacements, changes, hashes = {}, [], {}
    for name in ('selection.json', 'results.json', 'audit.json', 'regression-controls.json'):
        p = EVIDENCE / name
        hashes[str(p)] = run.sha(p)
    for wid, rows in sorted(grouped.items()):
        meta = metadata[wid]
        old_path = Path(meta['reference']).resolve()
        old = run.read(old_path)
        new = copy.deepcopy(old)
        hashes[str(old_path)] = run.sha(old_path)
        changed = set()
        for identity, row in rows:
            rr, ar = result_rows[identity], audit_rows[identity]
            key = row['candidate']
            require((ROOT / row['reference']).resolve() == old_path, 'Selection names a different map')
            require(hashes[str(old_path)] == row['referenceSha256'] == rr['oldReferenceSha256'],
                    'Historical reference changed')
            attempt = ROOT / rr['newAttempt']
            require(run.sha(attempt) == rr['newAttemptSha256'] == ar['newAttemptSha256'], 'Attempt changed')
            observation = retained_attempt(attempt)
            require(observation['candidate'] == old['candidates'][key] == row['candidateData'],
                    'Remeasurement changed candidate identity')
            require(observation['packageHashes'] == old['observations'][key]['packageHashes'],
                    'Remeasurement changed package identity')
            score = assess(observation, FIT)
            require(score == rr['fitness'] and score['fitnessScore'] == ar['jointScore'], 'Verdict disagrees with audit')
            require(assess(old['observations'][key], FIT)['fitnessScore'] is None,
                    'Selected candidate was already jointly scored')
            if observation['status'] == 'COMPLETE':
                require(components(observation) == components(old['observations'][key]), 'Other criteria changed')
            new['observations'][key] = observation
            # This cache is validated against the original map's declared policy.
            # Actual replay reward is always the independently frozen all-five FIT.
            new['fitness'][key] = assess(observation, run.configuration(meta['fitness']))
            hashes[str(attempt)] = rr['newAttemptSha256']
            changed.add(key)
            changes.append({'workload': wid, 'candidate': key, 'newAttempt': str(attempt),
                            'status': observation['status'], 'jointScore': score['fitnessScore']})
        require(new['candidates'] == old['candidates'], 'Catalogue changed')
        for key in set(old['candidates']) - changed:
            require(new['observations'][key] == old['observations'][key]
                    and new['fitness'][key] == old['fitness'][key], 'Unselected result changed')
        new_path = OUT / 'references' / (wid + '.json')
        save_fixed(new_path, new)
        replacements[wid] = {'oldReference': str(old_path), 'newReference': str(new_path),
                             'newReferenceSha256': run.sha(new_path), 'changedObservations': len(changed)}
    cohorts = {}
    for cohort in ('small', 'full'):
        source = TARGET / f'allocator-formal-{cohort}-2026-09-28'
        directory = OUT / cohort
        directory.mkdir(exist_ok=True)
        previous = run.read(source / 'formal-protocol.json')
        require(run.sha(source / 'inputs.json') == previous['inputSha256'], 'Baseline inputs changed')
        verify_hashes(previous['sourceHashes'])
        old_inputs = run.read(source / 'inputs.json')
        new_inputs = copy.deepcopy(old_inputs)
        new_inputs['purpose'] = 'Same catalogue; fixed synchronous-copy observations replaced, including failed attempt'
        new_inputs['sourceHashes'].update(hashes)
        new_inputs['sourceHashes'][str(source / 'inputs.json')] = previous['inputSha256']
        old_obs, new_obs = [], []
        seen = set()
        for part in new_inputs['partitions']:
            for row in part['workloads']:
                old_reference = run.read(row['reference'])
                old_obs.extend(old_reference['observations'].values())
                change = replacements.get(row['workload'])
                if change:
                    require(Path(row['reference']).resolve() == Path(change['oldReference']), 'Cohort references differ')
                    row['reference'] = change['newReference']
                    part['files'][row['reference']] = change['newReferenceSha256']
                    seen.add(row['workload'])
                new_obs.extend(run.read(row['reference'])['observations'].values())
        require(seen == set(replacements), 'Remeasured workloads missing from cohort')
        new_inputs['measurementRevision'] = {'selectionSha256': audit['selectionSha256'],
            'replacements': replacements, 'attempted': 270, 'recoveredJointScores': 269,
            'historicalEvidenceReplaced': False, 'mixedMeasurementVersions': True}
        save_fixed(directory / 'inputs.json', new_inputs)
        workloads, evaluator = run.load(directory, new_inputs)
        require(evaluator().revealed == set(), 'Labels revealed during initialization')
        profiles = {p: {'before': score_counts(old_obs, p), 'after': score_counts(new_obs, p)}
                    for p in run.PROFILES}
        cohort_proof = {'workloads': len(workloads), 'scenarios': len(new_obs),
            'identicalCandidatesAndStructuralProfiles': True, 'profiles': profiles,
            'budget': previous['budget'], 'seeds': previous['seeds'], 'arms': list(ARMS)}
        require(len(old_obs) == len(new_obs) == (6396 if cohort == 'small' else 18858), 'Cohort size changed')
        require(profiles['all-five']['before']['unavailable'] - profiles['all-five']['after']['unavailable'] == 269,
                'Unexpected joint coverage delta')
        save_fixed(directory / 'preparation.json', cohort_proof)
        kernel_proof = run.read(source / 'compiled-kernel-equivalence.json')
        binary = Path(kernel_proof['binary'])
        require(run.sha(binary) == previous['kernelSha256'], 'Numeric kernel changed')
        protocol = {'scope': 'Measurement revision on previously examined benchmark; not held-out confirmation',
            'inputSha256': run.sha(directory / 'inputs.json'), 'budget': previous['budget'],
            'seeds': previous['seeds'], 'arms': list(ARMS), 'profiles': ['all-five'],
            'sourceHashes': {**previous['sourceHashes'], str(Path(__file__).resolve()): run.sha(__file__),
                             str(Path(ucb.__file__).resolve()): run.sha(ucb.__file__)},
            'kernel': str(binary), 'kernelSha256': previous['kernelSha256'], 'fitness': FIT,
            'localSearch': 'ga', 'tieRule': formal.seeded.RULE,
            'baselineDirectory': str(source), 'baselineProtocolSha256': run.sha(source / 'formal-protocol.json'),
            'ucbBaselineDirectory': str(TARGET / f'allocator-ucb-{cohort}-2026-09-28'),
            'baselineUcbResultsSha256': run.sha(TARGET / f'allocator-ucb-{cohort}-2026-09-28/results.json'),
            'parametersSelectedFromAlphaResults': False, 'historicalRunsReplaced': False}
        save_fixed(directory / 'protocol.json', protocol)
        proof_path = directory / 'proof.json'
        if not proof_path.exists():
            exact = []
            fast = batch.kernel(binary)
            for arm in ARMS:
                batch.install(None)
                dense = run.run_arm(workloads, evaluator, arm, 1, 16, [16], FIT, factory)
                batch.install(fast)
                compiled = run.run_arm(workloads, evaluator, arm, 1, 16, [16], FIT, factory)
                require(dense['decisions'] == compiled['decisions'] and dense['perWorkload'] == compiled['perWorkload'],
                        'Numeric kernel changed corrected-map trajectory')
                formal.seeded.audit_ties(compiled)
                exact.append({'arm': arm, 'decisions': 16})
            save_fixed(proof_path, {'protocolSha256': seal(protocol), 'exactComparisons': exact,
                                   'ucbAdapter': ucb.verify_adapter()})
            batch.install(None)
        require(run.read(proof_path)['protocolSha256'] == seal(protocol), 'Preparation proof changed')
        cohorts[cohort] = {**cohort_proof, 'directory': str(directory),
                           'protocolSha256': run.sha(directory / 'protocol.json'),
                           'proofSha256': run.sha(proof_path)}
        print('PREPARED', cohort, len(workloads), len(new_obs), profiles['all-five'], flush=True)
    preparation = {'ready': True, 'totalRuns': 80, 'profiles': ['all-five'],
        'cohorts': cohorts, 'replacements': replacements, 'changes': changes,
        'failedRemeasurementsRetained': sum(r['status'] != 'COMPLETE' for r in changes),
        'note': 'Joint replay only; original eight policy settings, no new progress/order or automatic alpha adoption'}
    save_fixed(OUT / 'preparation.json', preparation)


def gate():
    receipts = {}
    for cohort in ('small', 'full'):
        directory = TARGET / f'allocator-hybrid-exploration-{cohort}-2026-09-28'
        status = run.read(directory / 'status.json')
        require(status['complete'] and status['completedRuns'] == status['totalRuns'] == 60,
                'Hybrid control is still incomplete: ' + cohort)
        result = run.read(directory / 'results.json')
        checks = result['checks']
        require(len(result['rows']) == 60 and checks['baselineComparisons'] == 420,
                'Hybrid result count/audit incomplete')
        for key in ('coldStartsMatched', 'matchedGaCandidateAndScorePrefixes', 'seededTiesReproduced',
                    'unknownsNeverZeroOrUpdates', 'completeBudgets'):
            require(checks[key] is True, 'Hybrid audit failed: ' + key)
        verify_hashes(result['protocol']['sourceHashes'])
        receipts[cohort] = run.sha(directory / 'results.json')
    return receipts


def initialize(directory, binary):
    global WORKLOADS, EVALUATOR
    scratch = directory / 'workers' / str(os.getpid())
    scratch.mkdir(parents=True, exist_ok=True)
    WORKLOADS, EVALUATOR = run.load(scratch, run.read(directory / 'inputs.json'))
    batch.install(batch.kernel(binary))


def row_for(value, path):
    return {'profile': 'all-five', 'arm': value['arm'], 'seed': value['seed'], 'attempts': value['attempts'],
            'score': value['cumulativeScore'], 'positives': value['positives'], 'unknowns': value['unknowns'],
            'path': str(path), 'traceSha256': run.sha(path)}


def execute(task):
    directory, arm, seed, budget, protocol_seal = task
    check_disk()
    started = time.monotonic()
    value = run.run_arm(WORKLOADS, EVALUATOR, arm, seed, budget,
        [n for n in (1000, 2000, 4000, 8000) if n <= budget], FIT, factory)
    require(value['attempts'] == budget and value['start']['allProgressZero'], 'Incomplete/noncold run')
    formal.seeded.audit_ties(value)
    value.update(profile='all-five', protocolSha256=protocol_seal, wallSeconds=time.monotonic() - started)
    path = formal.path_for(directory, 'all-five', arm, seed, budget)
    partial = path.with_suffix(path.suffix + '.partial')
    with gzip.open(partial, 'wt') as stream:
        json.dump(value, stream)
    partial.replace(path)
    return row_for(value, path)


def audit_replays(directory, protocol, rows):
    references = {w['workload']: run.read(w['reference']) for part in run.read(directory / 'inputs.json')['partitions']
                  for w in part['workloads']}
    groups = defaultdict(list)
    for row in rows:
        require(run.sha(row['path']) == row['traceSha256'], 'Trace changed')
        groups[row['seed']].append(row)
    compared = 0
    for seed, group in groups.items():
        require({r['arm'] for r in group} == set(ARMS) and len(group) == len(ARMS), 'Incomplete arm group')
        starts, prefixes = set(), {}
        for row in group:
            value = ucb.read_trace(row['path'])
            require(value['protocolSha256'] == seal(protocol) and value['fitness'] == FIT, 'Trace protocol mismatch')
            require(value['attempts'] == len(value['decisions']) == protocol['budget'], 'Incomplete trace')
            require(value['seed'] == seed and value['arm'] == row['arm'] and value['start']['allProgressZero'], 'Noncold/mislabelled trace')
            formal.seeded.audit_ties(value)
            starts.add(value['start']['stateSha256'])
            local = defaultdict(list)
            total = positives = unknowns = 0
            for d in value['decisions']:
                expected = assess(references[d['workload']]['observations'][d['candidate']], FIT)['fitnessScore']
                require(d['score'] == expected, 'Feedback differs from corrected map')
                require(d['modelUpdated'] == (expected is not None and row['arm'] != 'uniform'), 'Model update contract differs')
                local[d['workload']].append((d['candidate'], d['score']))
                total += expected or 0
                positives += expected is not None and expected > 0
                unknowns += expected is None
            require((total, positives, unknowns) == (value['cumulativeScore'], value['positives'], value['unknowns']), 'Trace totals differ')
            for wid, prefix in local.items():
                old = prefixes.get(wid, [])
                n = min(len(old), len(prefix))
                require(old[:n] == prefix[:n], 'GA candidate/score prefix differs within corrected cohort')
                if len(prefix) > len(old): prefixes[wid] = prefix
            baseline_dir = Path(protocol['ucbBaselineDirectory'] if row['arm'] == ucb.ARM else protocol['baselineDirectory'])
            old = ucb.read_trace(formal.path_for(baseline_dir, 'all-five', row['arm'], seed, protocol['budget']))
            require(old['start'] == value['start'] and old['fitness'] == value['fitness'], 'Baseline cold state/fitness changed')
            compared += 1
        require(len(starts) == 1, 'Initial GA states differ across arms')
    require(set(groups) == set(protocol['seeds']) and len(rows) == 40, 'Matrix incomplete')
    return {'coldStartsMatched': True, 'seededTiesReproduced': True, 'completeBudgets': True,
            'unknownsNeverZeroOrUpdates': True, 'feedbackMatchesCorrectedMaps': True,
            'matchedGaCandidateAndScorePrefixesWithinRevision': True, 'baselineColdStatesCompared': compared,
            'crossRevisionGaPrefixesRequired': False}


def run_cohort(cohort, workers):
    directory = OUT / cohort
    protocol = run.read(directory / 'protocol.json')
    verify_hashes(protocol['sourceHashes'])
    require(run.sha(directory / 'inputs.json') == protocol['inputSha256'], 'Inputs changed')
    require(run.sha(protocol['kernel']) == protocol['kernelSha256'], 'Kernel changed')
    require(run.read(directory / 'proof.json')['protocolSha256'] == seal(protocol), 'Proof changed')
    rows, tasks = [], []
    for seed, arm in itertools.product(protocol['seeds'], protocol['arms']):
        path = formal.path_for(directory, 'all-five', arm, seed, protocol['budget'])
        if path.exists():
            v = ucb.read_trace(path)
            require(v['protocolSha256'] == seal(protocol), 'Cannot resume changed protocol')
            rows.append(row_for(v, path))
        else:
            tasks.append((directory, arm, seed, protocol['budget'], seal(protocol)))
    def status(complete=False):
        batch.write(directory / 'status.json', {'complete': complete, 'completedRuns': len(rows), 'totalRuns': 40, 'rows': rows})
    status()
    with ProcessPoolExecutor(max_workers=workers, mp_context=multiprocessing.get_context('spawn'),
                             initializer=initialize, initargs=(directory, Path(protocol['kernel']))) as pool:
        for future in as_completed([pool.submit(execute, task) for task in tasks]):
            row = future.result(); rows.append(row); status()
            print('MEASUREMENT', cohort, len(rows), '/40', row['arm'], row['seed'], row['score'], flush=True)
    checks = audit_replays(directory, protocol, rows)
    verify_hashes(protocol['sourceHashes'])
    batch.write(directory / 'results.json', {'protocol': protocol, 'rows': rows, 'checks': checks})
    status(True)


def summarize():
    metadata = {w['id']: w for w in run.read(run.MASTER / 'master-inventory-2026-09-28.json')['qualifiedWorkloads']}
    output = {}
    for cohort in ('small', 'full'):
        directory = OUT / cohort
        result = run.read(directory / 'results.json')
        require(run.read(directory / 'status.json')['complete'], 'Replay incomplete')
        protocol = result['protocol']; paired = []
        for row in result['rows']:
            now = ucb.read_trace(row['path'])
            old_dir = Path(protocol['ucbBaselineDirectory'] if row['arm'] == ucb.ARM else protocol['baselineDirectory'])
            old = ucb.read_trace(formal.path_for(old_dir, 'all-five', row['arm'], row['seed'], protocol['budget']))
            def metrics(v):
                positive = [w for w in v['perWorkload'] if w['positiveDiscoveries']]
                return {'score': v['cumulativeScore'], 'positives': v['positives'], 'unknowns': v['unknowns'],
                        'positiveFamilies': len({metadata[w['workload']]['family'] for w in positive})}
            paired.append({'arm': row['arm'], 'seed': row['seed'], 'before': metrics(old), 'after': metrics(now)})
        summaries = []
        for arm in ARMS:
            rs = [r for r in paired if r['arm'] == arm]
            summaries.append({'arm': arm,
                **{k: {side: mean(r[side][k] for r in rs) for side in ('before', 'after')}
                   for k in ('score', 'positives', 'unknowns', 'positiveFamilies')},
                'scoreWinsTiesLosses': [sum(r['after']['score'] > r['before']['score'] for r in rs),
                    sum(r['after']['score'] == r['before']['score'] for r in rs),
                    sum(r['after']['score'] < r['before']['score'] for r in rs)]})
        output[cohort] = {'checks': result['checks'], 'summaries': summaries, 'pairedRows': paired,
                          'resultsSha256': run.sha(directory / 'results.json')}
    batch.write(OUT / 'comparison.json', {'scope': 'Joint score, same eight baseline policies and five seeds', 'cohorts': output})


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('stage', choices=['prepare', 'run', 'gate', 'summarize'])
    parser.add_argument('--workers', type=int, choices=range(1, 4), default=3)
    args = parser.parse_args()
    if args.stage == 'prepare':
        prepare()
    elif args.stage == 'gate':
        print(json.dumps(gate()))
    elif args.stage == 'summarize':
        summarize()
    else:
        OUT.mkdir(parents=True, exist_ok=True)
        with (OUT / 'run.lock').open('a') as lock:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            gate_receipts = gate()
            require(run.read(OUT / 'preparation.json')['ready'], 'Preparation incomplete')
            check_disk()
            batch.write(OUT / 'launch.json', {'pid': os.getpid(), 'workers': args.workers,
                'startedAtUnix': time.time(), 'hybridResultHashes': gate_receipts,
                'preparationSha256': run.sha(OUT / 'preparation.json')})
            try:
                for cohort in ('small', 'full'): run_cohort(cohort, args.workers)
                summarize()
                batch.write(OUT / 'completion.json', {'complete': True, 'totalRuns': 80,
                    'comparisonSha256': run.sha(OUT / 'comparison.json')})
            except Exception as error:
                batch.write(OUT / 'error.json', {'type': type(error).__name__, 'message': str(error), 'pid': os.getpid()})
                raise


if __name__ == '__main__':
    main()
