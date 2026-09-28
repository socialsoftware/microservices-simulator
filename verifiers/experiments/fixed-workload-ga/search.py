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


class SearchSession:
    """Incremental ask/tell search state for one fixed workload.

    The session owns its population, seen set, RNG and proposal history. Pausing it
    between ``ask`` and ``tell`` is deliberately forbidden, while pausing between
    completed attempts is safe. ``run`` below is only the original synchronous
    loop expressed through this API.
    """

    def __init__(self, domain, *, strategy, seed, budget, population=8, mutation=0.3,
                 stall_limit=100, emit=lambda row: None, fitness=None, exploration='per-saga'):
        if strategy not in ('ga', 'random') or budget < 1 or population < 1 \
                or stall_limit < 1 or not 0 <= mutation <= 1:
            raise ValueError('Invalid search configuration')
        if exploration not in ('per-saga', 'uniform-unseen'):
            raise ValueError('Unknown exploration policy')
        if exploration == 'uniform-unseen' and (not callable(getattr(domain, 'sample_unseen', None))
                                               or stall_limit < 2):
            raise ValueError('Uniform unseen exploration requires a complete catalogue and stall limit >= 2')
        self.domain = domain
        self.strategy = strategy
        self.seed = seed
        self.budget = budget
        self.fitness = configuration(fitness)
        self.rng = random.Random(seed)
        self.size = min(population, budget)
        self.mutation = mutation
        self.stall_limit = stall_limit
        self.emit = emit
        self.exploration = exploration
        self.parents = []
        self.seen = {}
        self.attempts = []
        self.proposals = []
        self.stalled = 0
        self.duplicates = 0
        self.best = None
        self.best_i = None
        self.started = time.monotonic()
        self.first_positive = None
        self.stop = 'BUDGET'
        self.pending = None

    def _finished(self):
        if len(self.attempts) >= self.budget:
            self.stop = 'BUDGET'
            return True
        if self.domain.exhausted(set(self.seen)):
            self.stop = 'EXHAUSTED'
            return True
        if self.stalled >= self.stall_limit:
            self.stop = 'PROPOSAL_STALL'
            return True
        return False

    def exhausted(self):
        """Return true only for proven finite-catalogue exhaustion."""
        return self.pending is None and self.domain.exhausted(set(self.seen))

    def ask(self):
        """Return one new candidate, skipping duplicate/unresolvable proposals."""
        if self.pending is not None:
            raise RuntimeError('tell must complete the pending candidate before the next ask')
        while not self._finished():
            lineage = {'parents': [], 'operator': 'random'}
            # A duplicate offspring triggers exploration; unavailable fitness never parents.
            if (self.strategy == 'ga' and len(self.parents) >= 2
                    and len(self.attempts) >= self.size and self.stalled == 0):
                def tournament():
                    pair = self.rng.sample(self.parents, 2)
                    return max(pair, key=lambda p: p['fitnessScore'])
                left, right = tournament(), tournament()
                genes = [self.rng.choice([a, b]) for a, b in zip(left['genes'], right['genes'])]
                recovery = self.rng.choice([left['recovery'], right['recovery']])
                lineage = {'operator': 'crossover', 'parents': [
                    {'key': p['key'], 'I': p['I'], 'fitnessScore': p['fitnessScore']}
                    for p in (left, right)], 'mutation': None}
                if self.rng.random() < self.mutation:
                    coordinate = self.rng.randrange(len(genes) + 1)
                    lineage['mutation'] = coordinate
                    if coordinate == len(genes):
                        recovery = None
                    else:
                        genes[coordinate] = self.rng.choice(self.domain.coordinates[coordinate])
            else:
                if self.exploration == 'uniform-unseen':
                    selected = self.domain.sample_unseen(self.rng, set(self.seen))
                    genes = [next((slot for slot in coordinate if slot is not None
                                   and selected['faultVector'][slot] == '1'), None)
                             for coordinate in self.domain.coordinates]
                    recovery = stable(selected['actions'])
                else:
                    genes = [self.rng.choice(c) for c in self.domain.coordinates]
                    recovery = None
            vector = vector_for(genes, self.domain.width)
            candidates = self.domain.resolve(vector)
            row = {'proposal': len(self.proposals) + 1, 'genes': genes, 'vector': vector, **lineage}
            if not candidates:
                row['status'] = 'UNRESOLVABLE'
                self.stalled += 1
            else:
                by_recovery = {stable(c['actions']): c for c in candidates}
                replacement = recovery is not None and recovery not in by_recovery
                if recovery not in by_recovery:
                    recovery = self.rng.choice(sorted(by_recovery))
                candidate = by_recovery[recovery]
                key = candidate['key']
                row.update(key=key, recovery=recovery, replacedRecovery=replacement,
                           scenarioId=candidate['id'])
                if key in self.seen:
                    row['status'] = 'DUPLICATE'
                    self.duplicates += 1
                    self.stalled += 1
                else:
                    self.pending = (candidate, row)
                    return candidate
            self.proposals.append(row)
            self.emit({'proposal': row, 'attempt': None})
        return None

    def tell(self, candidate, result):
        """Record feedback for the exact candidate returned by the last ``ask``."""
        if self.pending is None or candidate is not self.pending[0]:
            raise ValueError('Feedback does not match the pending candidate')
        pending, row = self.pending
        assessment = assess(result, self.fitness)
        score = assessment['fitnessScore']
        item = {**row, **result, **assessment, 'attempt': len(self.attempts) + 1}
        if result.get('I') is not None:
            self.best_i = result['I'] if self.best_i is None else max(self.best_i, result['I'])
        self.seen[pending['key']] = item
        self.attempts.append(item)
        if score is not None:
            self.best = score if self.best is None else max(self.best, score)
            if score > 0 and self.first_positive is None:
                self.first_positive = {
                    'attempt': len(self.attempts), 'wallSeconds': time.monotonic() - self.started}
            if self.strategy == 'ga':
                self.parents.append(item)
                self.rng.shuffle(self.parents)
                self.parents.sort(key=lambda p: p['fitnessScore'], reverse=True)
                self.parents = self.parents[:self.size]
        item['bestSoFar'] = self.best
        item['bestISoFar'] = self.best_i
        row['status'] = 'EVALUATED'
        self.stalled = 0
        self.proposals.append(row)
        self.pending = None
        self.emit({'proposal': row, 'attempt': item})
        return item

    def result(self):
        if self.pending is not None:
            raise RuntimeError('Cannot summarize a session with pending feedback')
        return {'strategy': self.strategy, 'seed': self.seed, 'budget': self.budget,
                'fitnessPolicy': self.fitness['policy'], 'fitness': self.fitness,
                'samplingPolicy': ('uniform-unseen-catalogue-initialization-and-fallback'
                                   if self.exploration == 'uniform-unseen'
                                   else 'uniform-per-saga-fault-then-uniform-returned-recovery'),
                'population': self.size, 'mutationProbability': self.mutation,
                'stallLimit': self.stall_limit, 'stopReason': self.stop,
                'attempts': self.attempts, 'proposals': self.proposals,
                'duplicates': self.duplicates, 'bestI': self.best_i, 'bestScore': self.best,
                'firstPositive': self.first_positive,
                'positiveScenarios': sum(a['fitnessScore'] is not None and a['fitnessScore'] > 0
                                         for a in self.attempts),
                'positiveIScenarios': sum(a.get('I') is not None and a['I'] > 0
                                          for a in self.attempts),
                'nullFitnessAttempts': sum(a['fitnessScore'] is None for a in self.attempts),
                'vectorDomainSize': math.prod(map(len, self.domain.coordinates)),
                'wallSeconds': time.monotonic() - self.started}


def run(domain, evaluate, *, strategy, seed, budget, population=8, mutation=0.3,
        stall_limit=100, emit=lambda row: None, fitness=None, exploration='per-saga'):
    """Compatibility wrapper preserving the original synchronous seeded trajectory."""
    session = SearchSession(domain, strategy=strategy, seed=seed, budget=budget,
                            population=population, mutation=mutation,
                            stall_limit=stall_limit, emit=emit, fitness=fitness,
                            exploration=exploration)
    while True:
        candidate = session.ask()
        if candidate is None:
            break
        session.tell(candidate, evaluate(candidate, len(session.attempts) + 1))
    return session.result()
