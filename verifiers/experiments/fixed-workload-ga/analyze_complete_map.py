#!/usr/bin/env python3
"""Replay the shipped GA and uniform random over a completed measurement map."""
import argparse
from collections import Counter
import math
from pathlib import Path
import statistics
import time

from catalogue import RecordedDomain, enumerated_domain
from fitness import assess, configuration
from runtime import read, save, digest, package
from search import run


def curves(attempts):
    positive, score, unknown = [0], [0.0], [0]
    for a in attempts:
        s = a['fitnessScore']
        positive.append(positive[-1] + int(s is not None and s > 0))
        score.append(score[-1] + (s if s is not None else 0))
        unknown.append(unknown[-1] + int(s is None))
    return {'positive': positive, 'score': score, 'unknown': unknown}



def initialize_worker(campaign, output):
    global reference, config, domain, keys, out, total_positive, total_score
    campaign, out = Path(campaign), Path(output)
    reference = read(campaign / 'map/reference.json')
    config = read(campaign / 'config.json')
    config['fitness'] = configuration(config['fitness'])
    workload = next(w for w in package(campaign / 'map/package/scenario-catalog-manifest.json')['records']['workloads'] if w['id'] == config['workload'])
    domain = RecordedDomain(workload, reference['candidates'])
    keys = set(domain.candidates)
    global scores
    scores = {k:f['fitnessScore'] for k,f in reference['fitness'].items()}
    total_positive = sum(v is not None and v > 0 for v in scores.values())
    total_score = sum(v for v in scores.values() if v is not None)


def replay(task):
    seed, method = task
    started = time.monotonic()
    revealed = set()
    def evaluate(candidate, number):
        key = candidate['key']
        assert key not in revealed and number == len(revealed) + 1
        revealed.add(key)
        return reference['observations'][key]
    result = run(domain, evaluate, strategy=method, seed=seed, budget=len(keys),
        population=config['population'], mutation=config['mutation'],
        stall_limit=config['stallLimit'], fitness=config['fitness'], exploration='uniform-unseen')
    assert revealed == keys, (method, seed, result['stopReason'], len(revealed))
    attempts = result['attempts']
    assert len(attempts) == len(keys) and len({a['key'] for a in attempts}) == len(keys)
    assert all(a['fitnessScore'] == scores[a['key']] for a in attempts)
    c = curves(attempts)
    assert c['positive'][-1] == total_positive and c['score'][-1] == total_score
    trace = [{k: a[k] for k in ('key', 'fitnessScore', 'genes', 'parents', 'operator', 'replacedRecovery')}
             for a in attempts]
    save(out / f'{method}-{seed}.json', {'strategy': method, 'seed': seed,
         'attempts': trace, 'duplicates': result['duplicates'], 'proposalCount': len(result['proposals']),
         'stopReason': result['stopReason'], 'allCandidatesVisited': True})
    row = {'strategy': method, 'seed': seed, **c,
        'toPositiveFraction': {str(f): next(i for i, n in enumerate(c['positive']) if n >= math.ceil(total_positive*f))
                               for f in (.5, .8, .9, 1)},
        'toScoreFraction': {str(f): next(i for i, n in enumerate(c['score']) if n >= total_score*f)
                            for f in (.5, .8, .9, 1)}}
    print(method, seed, 'seconds', round(time.monotonic()-started, 2), flush=True)
    return row

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--campaign', type=Path, required=True)
    ap.add_argument('--output', type=Path, required=True)
    args = ap.parse_args()
    source, out = args.campaign.resolve(), args.output.resolve()
    out.mkdir(parents=True, exist_ok=False)
    reference_path = source / 'map/reference.json'
    reference = read(reference_path)
    config = read(source / 'config.json')
    config['fitness'] = configuration(config['fitness'])
    assert read(source / 'map/status.json')['stage'] == 'COMPLETE'
    domain, hashes = enumerated_domain(source / 'catalogue', config['workload'])
    assert domain.candidates == reference['candidates']
    keys = set(domain.candidates)
    assert keys == set(reference['observations']) == set(reference['fitness'])
    for key, obs in reference['observations'].items():
        assert obs['candidate'] == domain.candidates[key]
        assert assess(obs, config['fitness']) == reference['fitness'][key]
    paths = [reference_path, source / 'map/protocol.json', source / 'config.json',
             source / 'catalogue/domain-count.json', source / 'catalogue/requests.json',
             source / 'catalogue/package/scenario-catalog-manifest.json']
    paths += [Path(__file__).parent / n for n in ('search.py', 'fitness.py', 'catalogue.py', 'analyze_complete_map.py')]
    frozen = {str(p): digest(p) for p in paths}
    seeds = list(range(1, 31))
    save(out / 'protocol.json', {'mode': 'RECORDED_FEEDBACK_NOT_LIVE_TIMING',
        'sourceHashes': frozen, 'seeds': seeds, 'seedSelection': 'Fixed integers 1..30 before replay, no seed selection by outcomes',
        'population': config['population'], 'mutation': config['mutation'],
        'stallLimit': config['stallLimit'], 'fitness': config['fitness'], 'budget': len(keys),
        'strategies': ['ga', 'random'], 'exploration': 'uniform-unseen',
        'feedback': 'Only returned when selected; null scores remain unavailable and consume one evaluation',
        'scope': 'One measured observation per candidate; repeat qualification is separate'})
    scores = {k: f['fitnessScore'] for k, f in reference['fitness'].items()}
    total_positive = sum(v is not None and v > 0 for v in scores.values())
    total_score = sum(v for v in scores.values() if v is not None)
    rows = []
    tasks = [(seed, method) for seed in seeds for method in ('ga', 'random')]
    from concurrent.futures import ProcessPoolExecutor, as_completed
    import multiprocessing
    with ProcessPoolExecutor(max_workers=4, mp_context=multiprocessing.get_context('spawn'),
            initializer=initialize_worker, initargs=(str(source), str(out))) as pool:
        pending = [pool.submit(replay, task) for task in tasks]
        for future in as_completed(pending):
            rows.append(future.result())
            save(out / 'status.json', {'stage':'REPLAYING', 'finishedRuns':len(rows), 'totalRuns':len(tasks)})
    rows.sort(key=lambda r: (r['seed'], r['strategy']))
    checkpoints = [100, 250, 500, 1000, 2000, 3000, len(keys)]
    summary = {'candidateCount': len(keys), 'knownPositiveCount': total_positive,
        'knownZeroCount': sum(v == 0 for v in scores.values()), 'unknownCount': sum(v is None for v in scores.values()),
        'availableScoreSum': total_score, 'scoreDistribution': dict(Counter(str(v) for v in scores.values())),
        'seeds': seeds, 'criteria': {name: {
            'positiveCandidates': sum((f['fitnessComponents'][name]['count'] or 0) > 0 for f in reference['fitness'].values()),
            'unknownCandidates': sum(f['fitnessComponents'][name]['count'] is None for f in reference['fitness'].values())}
            for name in next(iter(reference['fitness'].values()))['fitnessComponents']},
        'checkpoints': {method: {str(n): {metric: statistics.mean(r[metric][n] for r in rows if r['strategy']==method)
            for metric in ('positive', 'score', 'unknown')} for n in checkpoints} for method in ('ga', 'random')},
        'meanEvaluationsToPositiveFraction': {method: {str(f): statistics.mean(r['toPositiveFraction'][str(f)] for r in rows if r['strategy']==method)
            for f in (.5, .8, .9, 1)} for method in ('ga', 'random')},
        'meanEvaluationsToScoreFraction': {method: {str(f): statistics.mean(r['toScoreFraction'][str(f)] for r in rows if r['strategy']==method)
            for f in (.5, .8, .9, 1)} for method in ('ga', 'random')}, 'rows': rows}
    save(out / 'comparison.json', summary)
    assert all(digest(Path(p)) == h for p, h in frozen.items())
    save(out / 'validation.json', {'referenceScoresRecomputed': True, 'catalogueIdentityVerified': True,
        'all60RunsVisitedAll5184ExactlyOnce': len(keys)==5184 and len(rows)==60,
        'feedbackOnlyAfterSelection': True, 'sameEndpointScores': True, 'sourceHashesUnchanged': True})
    save(out / 'status.json', {'stage': 'COMPLETE', 'runs': len(rows), 'at': time.time()})


if __name__ == '__main__':
    main()
