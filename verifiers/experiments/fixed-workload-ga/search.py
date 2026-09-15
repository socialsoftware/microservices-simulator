"""Seeded, fixed-workload policies. No application or runtime dependencies."""
import hashlib
import json
import math
import random
import time

from fitness import assess, configuration


def stable(value):
    return json.dumps(value, sort_keys=True, separators=(',', ':'))


def candidate_key(workload, vector, actions):
    return hashlib.sha256(stable([workload, vector, actions]).encode()).hexdigest()


def fault_coordinates(workload):
    coordinates = []
    slots = []
    for participant in workload['participants']:
        own = [s['faultSlot'] for s in workload['schedule']
               if s.get('participant') == participant['id'] and s.get('faultSlot') is not None]
        coordinates.append([None] + sorted(own))
        slots.extend(own)
    if sorted(slots) != list(range(len(slots))):
        raise ValueError('Fault slots must be unique and contiguous')
    return coordinates


def vector_for(genes, width):
    return ''.join('1' if i in genes else '0' for i in range(width))


def run(domain, evaluate, *, strategy, seed, budget, population=8, mutation=0.3,
        stall_limit=100, emit=lambda row: None, fitness=None):
    if strategy not in ('ga', 'random') or budget < 1 or population < 1 \
            or stall_limit < 1 or not 0 <= mutation <= 1:
        raise ValueError('Invalid search configuration')
    fitness = configuration(fitness)
    rng = random.Random(seed)
    size = min(population, budget)
    parents, seen, attempts, proposals = [], {}, [], []
    stalled = duplicates = 0
    best = best_i = None
    start = time.monotonic()
    first_positive = None
    stop = 'BUDGET'
    while len(attempts) < budget:
        if domain.exhausted(set(seen)):
            stop = 'EXHAUSTED'
            break
        if stalled >= stall_limit:
            stop = 'PROPOSAL_STALL'
            break
        lineage = {'parents': [], 'operator': 'random'}
        # A duplicate offspring triggers exploration; unavailable fitness never parents.
        if strategy == 'ga' and len(parents) >= 2 and len(attempts) >= size and stalled == 0:
            def tournament():
                pair = rng.sample(parents, 2)
                return max(pair, key=lambda p: p['fitnessScore'])
            left, right = tournament(), tournament()
            genes = [rng.choice([a, b]) for a, b in zip(left['genes'], right['genes'])]
            recovery = rng.choice([left['recovery'], right['recovery']])
            lineage = {'operator': 'crossover', 'parents': [
                {'key': p['key'], 'I': p['I'], 'fitnessScore': p['fitnessScore']} for p in (left, right)], 'mutation': None}
            if rng.random() < mutation:
                coordinate = rng.randrange(len(genes) + 1)
                lineage['mutation'] = coordinate
                if coordinate == len(genes):
                    recovery = None
                else:
                    genes[coordinate] = rng.choice(domain.coordinates[coordinate])
        else:
            genes = [rng.choice(c) for c in domain.coordinates]
            recovery = None
        vector = vector_for(genes, domain.width)
        candidates = domain.resolve(vector)
        row = {'proposal': len(proposals) + 1, 'genes': genes, 'vector': vector, **lineage}
        if not candidates:
            row['status'] = 'UNRESOLVABLE'
            stalled += 1
        else:
            by_recovery = {stable(c['actions']): c for c in candidates}
            replacement = recovery is not None and recovery not in by_recovery
            if recovery not in by_recovery:
                recovery = rng.choice(sorted(by_recovery))
            candidate = by_recovery[recovery]
            key = candidate['key']
            row.update(key=key, recovery=recovery, replacedRecovery=replacement,
                       scenarioId=candidate['id'])
            if key in seen:
                row['status'] = 'DUPLICATE'
                duplicates += 1
                stalled += 1
            else:
                result = evaluate(candidate, len(attempts) + 1)
                assessment = assess(result, fitness)
                score = assessment['fitnessScore']
                item = {**row, **result, **assessment, 'attempt': len(attempts) + 1}
                if result.get('I') is not None:
                    best_i = result['I'] if best_i is None else max(best_i, result['I'])
                seen[key] = item
                attempts.append(item)
                if score is not None:
                    best = score if best is None else max(best, score)
                    if score > 0 and first_positive is None:
                        first_positive = {'attempt': len(attempts), 'wallSeconds': time.monotonic() - start}
                    if strategy == 'ga':
                        parents.append(item)
                        rng.shuffle(parents)
                        parents.sort(key=lambda p: p['fitnessScore'], reverse=True)
                        parents = parents[:size]
                item['bestSoFar'] = best
                item['bestISoFar'] = best_i
                row['status'] = 'EVALUATED'
                stalled = 0
        proposals.append(row)
        emit({'proposal': row, 'attempt': attempts[-1] if row['status'] == 'EVALUATED' else None})
    return {'strategy': strategy, 'seed': seed, 'budget': budget,
            'fitnessPolicy': fitness['policy'], 'fitness': fitness,
            'samplingPolicy': 'uniform-per-saga-fault-then-uniform-returned-recovery',
            'population': size, 'mutationProbability': mutation, 'stallLimit': stall_limit,
            'stopReason': stop, 'attempts': attempts, 'proposals': proposals,
            'duplicates': duplicates, 'bestI': best_i, 'bestScore': best, 'firstPositive': first_positive,
            'positiveScenarios': sum(a['fitnessScore'] is not None and a['fitnessScore'] > 0 for a in attempts),
            'positiveIScenarios': sum(a.get('I') is not None and a['I'] > 0 for a in attempts),
            'nullFitnessAttempts': sum(a['fitnessScore'] is None for a in attempts),
            'vectorDomainSize': math.prod(map(len, domain.coordinates)),
            'wallSeconds': time.monotonic() - start}
