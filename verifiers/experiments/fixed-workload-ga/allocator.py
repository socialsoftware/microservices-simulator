"""Sequential global-budget allocation over persistent fixed-workload GA sessions.

Catalogue structure and recorded outcomes deliberately enter through separate objects.
Policies receive only structural features and progress already observed through the
evaluator boundary.
"""
import copy
from itertools import combinations
import hashlib
import math
from pathlib import Path
import random
import time

from catalogue import load as load_catalogue
from fitness import (CRITERIA_V2, PERSISTENT, WEIGHTED_V2, assess,
                     configuration)
from runtime import digest, package, read
from search import SearchSession, candidate_key, stable


SCHEMA = 'contextual-workload-allocation.v1'
POLICIES = ('round-robin', 'uniform', 'adaptive-ucb', 'contextual-linucb',
            'contextual-linucb-cooldown')
ADAPTIVE_UCB_POLICIES = ('adaptive-ucb', 'adaptive-ucb-cooldown')
PROGRESS_LINUCB_POLICIES = ('progress-linucb', 'progress-linucb-cooldown')
CONTEXTUAL_LINUCB_POLICIES = ('contextual-linucb', 'contextual-linucb-cooldown')
LINEAR_UCB_POLICIES = (*PROGRESS_LINUCB_POLICIES, *CONTEXTUAL_LINUCB_POLICIES)
COOLDOWN_POLICIES = ('adaptive-ucb-cooldown', 'progress-linucb-cooldown',
                     'contextual-linucb-cooldown')
FACTORIAL_POLICIES = ('adaptive-ucb', 'adaptive-ucb-cooldown',
                      'progress-linucb', 'progress-linucb-cooldown',
                      'contextual-linucb', 'contextual-linucb-cooldown')
SUPPORTED_POLICIES = (*POLICIES, 'adaptive-ucb-cooldown',
                      'progress-linucb', 'progress-linucb-cooldown')
MEASURED_ASSESSMENT_STATUSES = ('COMPLETE', 'PARTIAL', 'UNAVAILABLE', 'EXECUTION_INVALID')
RUNTIME_FAILURE_STATUSES = ('PROCESS_FAILURE', 'TIMEOUT', 'INVALID_REPORT',
                            'INFRASTRUCTURE_FAILURE')
BASE_FEATURES = ('bias', 'structure:saga-count', 'structure:saga-pair-count',
                 'structure:interaction-count', 'structure:event-count')
PROGRESS_FEATURES = ('progress:allocation', 'progress:known-rate',
                     'progress:unknown-rate', 'progress:positive-rate',
                     'progress:mean-score', 'progress:best-score')


def _number(value, *, positive=False, nonnegative=False, label='value'):
    valid = type(value) in (int, float) and math.isfinite(value)
    if positive:
        valid = valid and value > 0
    if nonnegative:
        valid = valid and value >= 0
    if not valid:
        raise ValueError(f'{label} must be a finite ' + ('positive' if positive else 'nonnegative') + ' number')
    return float(value)


def _squash(value):
    if value < 0:
        raise ValueError('Progress scores cannot be negative')
    return value / (1.0 + value)


def _count_scale(value):
    return value / (1.0 + value)


def _local_seed(seed, workload_id):
    value = hashlib.sha256(f'{seed}:{workload_id}'.encode()).digest()[:8]
    return int.from_bytes(value, 'big')


def parse_configuration(path):
    """Read the strict allocation contract, resolving paths beside the config."""
    path = Path(path).resolve()
    value = read(path)
    allowed = {'schemaVersion', 'seed', 'budget', 'policy', 'policyParameters',
               'fitness', 'workloads'}
    if not isinstance(value, dict) or set(value) - allowed:
        raise ValueError('Unknown allocator configuration fields')
    if value.get('schemaVersion') != SCHEMA:
        raise ValueError('Unsupported allocator configuration schema')
    if type(value.get('seed')) is not int:
        raise ValueError('seed must be an integer')
    if type(value.get('budget')) is not int or value['budget'] < 1:
        raise ValueError('budget must be a positive integer')
    policy = value.get('policy')
    if policy not in SUPPORTED_POLICIES:
        raise ValueError('Unknown allocation policy')
    parameters = value.get('policyParameters', {})
    if not isinstance(parameters, dict):
        raise ValueError('policyParameters must be an object')
    expected = ({'exploration'} if policy in ADAPTIVE_UCB_POLICIES else
                {'exploration', 'ridge'} if policy in LINEAR_UCB_POLICIES else set())
    if set(parameters) - expected:
        raise ValueError('Unknown parameters for allocation policy')
    if policy in ADAPTIVE_UCB_POLICIES:
        parameters = {'exploration': _number(parameters.get('exploration', 1.0),
                                              nonnegative=True, label='exploration')}
    elif policy in LINEAR_UCB_POLICIES:
        parameters = {
            'exploration': _number(parameters.get('exploration', 1.0),
                                   nonnegative=True, label='exploration'),
            'ridge': _number(parameters.get('ridge', 1.0), positive=True, label='ridge')}
    else:
        parameters = {}
    fitness = configuration(value.get('fitness'))
    if fitness['policy'] != WEIGHTED_V2:
        raise ValueError('Cross-workload allocation requires weighted-criteria-v2 fitness')
    rows = value.get('workloads')
    if not isinstance(rows, list) or not rows:
        raise ValueError('workloads must be a nonempty array')
    workloads = []
    for row in rows:
        if not isinstance(row, dict) or not {'config', 'catalogue', 'reference'} <= set(row) \
                or set(row) - {'name', 'config', 'catalogue', 'reference'}:
            raise ValueError('Each workload requires config, catalogue and reference paths')
        if any(not isinstance(row[field], str) or not row[field]
               for field in ('config', 'catalogue', 'reference')):
            raise ValueError('Workload paths must be nonempty strings')
        resolved = {field: str((path.parent / row[field]).resolve())
                    if not Path(row[field]).is_absolute() else str(Path(row[field]).resolve())
                    for field in ('config', 'catalogue', 'reference')}
        name = row.get('name')
        if name is not None and (not isinstance(name, str) or not name.strip()):
            raise ValueError('Workload names must be nonempty strings')
        workloads.append({'name': name, **resolved})
    return {'schemaVersion': SCHEMA, 'seed': value['seed'], 'budget': value['budget'],
            'policy': policy, 'policyParameters': parameters, 'fitness': fitness,
            'workloads': workloads, 'configurationPath': str(path),
            'configurationSha256': digest(path)}


def structural_profile(records, workload):
    """Build outcome-free Saga, pair, interaction and event feature tokens."""
    sagas = sorted({participant['saga'] for participant in workload['participants']})
    known_sagas = {record['fqn'] for record in records['sagas']}
    if not set(sagas) <= known_sagas:
        raise ValueError('Workload references an unknown Saga')
    interaction_by_id = {record['id']: record for record in records['interactions']}
    if len(interaction_by_id) != len(records['interactions']):
        raise ValueError('Duplicate interaction identity')
    requested = workload.get('interactions', [])
    if len(requested) != len(set(requested)) or not set(requested) <= set(interaction_by_id):
        raise ValueError('Workload interaction identity mismatch')
    groups = {
        'saga': {'saga:' + saga for saga in sagas},
        'pair': {'saga-pair:' + left + '|' + right for left, right in combinations(sagas, 2)},
        'interaction': set(),
        'event': set()
    }
    for interaction_id in requested:
        interaction = interaction_by_id[interaction_id]
        groups['interaction'].add('interaction:id:' + interaction_id)
        groups['interaction'].add('interaction:evidence:' + str(interaction.get('evidence', 'UNKNOWN')))
        accesses = interaction.get('accesses')
        if not isinstance(accesses, list) or not accesses:
            raise ValueError('Interaction requires nonempty structural accesses')
        modes = []
        for access in accesses:
            aggregate, mode, saga = access.get('aggregate'), access.get('mode'), access.get('saga')
            if not all(isinstance(value, str) and value for value in (aggregate, mode, saga)):
                raise ValueError('Invalid structural interaction access')
            groups['interaction'].add(f'interaction:access:{aggregate}:{mode}')
            modes.append(f'{aggregate}:{mode}')
        groups['interaction'].add('interaction:shape:' + '|'.join(sorted(modes)))
    events = [row for row in workload['schedule'] if row.get('kind') == 'event']
    for event in events:
        route = event.get('route')
        if not isinstance(route, str) or not route:
            raise ValueError('Event schedule entry requires a route')
        groups['event'].add('event:route:' + route)
    if events:
        groups['event'].add('event:present')
    return {'counts': {'sagas': len(sagas), 'pairs': len(groups['pair']),
                       'interactions': len(requested), 'events': len(events)},
            'groups': {name: sorted(tokens) for name, tokens in groups.items()}}


class FeatureSpace:
    """One shared, deterministic feature vocabulary for all admitted workloads."""

    def __init__(self, profiles):
        tokens = sorted({token for profile in profiles.values()
                         for values in profile['groups'].values() for token in values})
        self.names = [*BASE_FEATURES, *tokens, *PROGRESS_FEATURES]
        self.index = {name: index for index, name in enumerate(self.names)}

    def vector(self, profile, progress):
        vector = [0.0] * len(self.names)
        counts = profile['counts']
        for name, value in zip(BASE_FEATURES, (1.0, _count_scale(counts['sagas']),
                                               _count_scale(counts['pairs']),
                                               _count_scale(counts['interactions']),
                                               _count_scale(counts['events']))):
            vector[self.index[name]] = value
        for tokens in profile['groups'].values():
            if tokens:
                value = 1.0 / math.sqrt(len(tokens))
                for token in tokens:
                    vector[self.index[token]] = value
        allocated = progress['allocated']
        known = progress['known']
        mean = progress['scoreSum'] / known if known else 0.0
        values = (allocated / (allocated + 8.0), known / allocated if allocated else 0.0,
                  progress['unknown'] / allocated if allocated else 0.0,
                  progress['positives'] / known if known else 0.0,
                  _squash(mean), _squash(progress['bestScore'] or 0.0))
        for name, value in zip(PROGRESS_FEATURES, values):
            vector[self.index[name]] = value
        return vector


class ProgressFeatureSpace:
    """Bias and observed progress only, with no workload structure inputs."""

    def __init__(self):
        self.names = ['bias', *PROGRESS_FEATURES]
        self.index = {name: index for index, name in enumerate(self.names)}

    def vector(self, profile, progress):
        vector = [0.0] * len(self.names)
        vector[self.index['bias']] = 1.0
        allocated = progress['allocated']
        known = progress['known']
        mean = progress['scoreSum'] / known if known else 0.0
        values = (allocated / (allocated + 8.0), known / allocated if allocated else 0.0,
                  progress['unknown'] / allocated if allocated else 0.0,
                  progress['positives'] / known if known else 0.0,
                  _squash(mean), _squash(progress['bestScore'] or 0.0))
        for name, value in zip(PROGRESS_FEATURES, values):
            vector[self.index[name]] = value
        return vector


def _dot(left, right):
    return math.fsum(a * b for a, b in zip(left, right))


def _matvec(matrix, vector):
    return [math.fsum(value * coordinate for value, coordinate in zip(row, vector))
            for row in matrix]


class RoundRobinPolicy:
    def __init__(self, workload_ids):
        self.order = sorted(workload_ids)
        self.next = 0

    def select(self, active, states):
        for _ in range(len(self.order)):
            workload_id = self.order[self.next]
            self.next = (self.next + 1) % len(self.order)
            if workload_id in active:
                return workload_id, {}, None
        raise RuntimeError('No active workload')

    def update(self, workload_id, context, reward):
        return False

    def summary(self):
        return {'modelUpdates': 0}


class UniformPolicy:
    def __init__(self, seed):
        self.rng = random.Random(seed)

    def select(self, active, states):
        return self.rng.choice(sorted(active)), {}, None

    def update(self, workload_id, context, reward):
        return False

    def summary(self):
        return {'modelUpdates': 0}


class AdaptiveUcbPolicy:
    """Independent per-workload mean score with allocation-count exploration."""
    def __init__(self, exploration):
        self.exploration = exploration
        self.updates = 0

    def select(self, active, states):
        total = sum(state['progress']['allocated'] for state in states.values())
        scored = []
        for workload_id in sorted(active):
            progress = states[workload_id]['progress']
            estimate = progress['scoreSum'] / progress['known'] if progress['known'] else 0.0
            bonus = self.exploration * math.sqrt(math.log(total + 2.0) /
                                                  (progress['allocated'] + 1.0))
            scored.append((estimate + bonus, workload_id, estimate, bonus))
        value, workload_id, estimate, bonus = max(scored, key=lambda row: row[0])
        return workload_id, {'index': value, 'estimate': estimate, 'uncertainty': bonus}, None

    def update(self, workload_id, context, reward):
        if reward is None:
            return False
        self.updates += 1
        return True

    def summary(self):
        return {'modelUpdates': self.updates, 'exploration': self.exploration}


class ContextualLinUcbPolicy:
    """Shared linear UCB with one parameter vector across every workload."""
    def __init__(self, feature_space, exploration, ridge):
        self.feature_space = feature_space
        self.exploration = exploration
        self.ridge = ridge
        dimension = len(feature_space.names)
        self.inverse = [[(1.0 / ridge if row == column else 0.0)
                         for column in range(dimension)] for row in range(dimension)]
        self.response = [0.0] * dimension
        self.theta = [0.0] * dimension
        self.updates = 0

    def select(self, active, states):
        scored = []
        for workload_id in sorted(active):
            state = states[workload_id]
            vector = self.feature_space.vector(state['profile'], state['progress'])
            transformed = _matvec(self.inverse, vector)
            uncertainty = self.exploration * math.sqrt(max(0.0, _dot(vector, transformed)))
            estimate = _dot(self.theta, vector)
            scored.append((estimate + uncertainty, workload_id, estimate, uncertainty, vector))
        value, workload_id, estimate, uncertainty, vector = max(scored, key=lambda row: row[0])
        return workload_id, {'index': value, 'estimate': estimate,
                             'uncertainty': uncertainty}, vector

    def update(self, workload_id, vector, reward):
        if reward is None:
            return False
        transformed = _matvec(self.inverse, vector)
        denominator = 1.0 + _dot(vector, transformed)
        dimension = len(vector)
        self.inverse = [[self.inverse[row][column]
                         - transformed[row] * transformed[column] / denominator
                         for column in range(dimension)] for row in range(dimension)]
        self.response = [value + reward * coordinate
                         for value, coordinate in zip(self.response, vector)]
        self.theta = _matvec(self.inverse, self.response)
        self.updates += 1
        return True

    def summary(self):
        return {'modelUpdates': self.updates, 'exploration': self.exploration,
                'ridge': self.ridge, 'featureCount': len(self.feature_space.names),
                'sharedParameters': True}


def make_policy(name, parameters, workload_ids, feature_space, seed):
    if name == 'round-robin':
        return RoundRobinPolicy(workload_ids)
    if name == 'uniform':
        return UniformPolicy(seed)
    if name in ADAPTIVE_UCB_POLICIES:
        policy = AdaptiveUcbPolicy(parameters['exploration'])
    elif name in PROGRESS_LINUCB_POLICIES:
        policy = ContextualLinUcbPolicy(
            ProgressFeatureSpace(), parameters['exploration'], parameters['ridge'])
    elif name in CONTEXTUAL_LINUCB_POLICIES:
        policy = ContextualLinUcbPolicy(
            feature_space, parameters['exploration'], parameters['ridge'])
    else:
        raise ValueError('Unknown allocation policy')
    if name in COOLDOWN_POLICIES:
        from allocation_cooldown import MissingFeedbackCooldownPolicy
        policy = MissingFeedbackCooldownPolicy(policy)
    return policy


def _require_weighted_observation(result, candidate, assessment):
    if not isinstance(result, dict) or result.get('candidate') != candidate:
        raise ValueError('Recorded observation candidate identity mismatch')
    status = result.get('status')
    if status not in (*MEASURED_ASSESSMENT_STATUSES, *RUNTIME_FAILURE_STATUSES):
        raise ValueError('Unsupported recorded assessment status')
    if status in RUNTIME_FAILURE_STATUSES:
        required = {'I', 'A', 'AStatus', 'ACoverage', 'lostCopiedUpdateCount',
                    'lostCopiedUpdateValidity', 'lostCopiedUpdateCoverage',
                    'lostCopiedUpdateCoverageGaps'}
        if not required <= set(result) or any(result.get(name) is not None
                                              for name in ('I', 'A', 'lostCopiedUpdateCount')) \
                or result.get('AStatus') != 'UNAVAILABLE' \
                or result.get('ACoverage') != 'UNAVAILABLE' \
                or result.get('lostCopiedUpdateValidity') != 'UNAVAILABLE' \
                or result.get('lostCopiedUpdateCoverage') != 'UNAVAILABLE' \
                or result.get('lostCopiedUpdateCoverageGaps') != [] \
                or assessment['fitnessScore'] is not None:
            raise ValueError('Recorded runtime failure has incompatible measured feedback')
        return
    required = {'terminalStatus', 'scheduleConformance', 'I', 'impactCategories',
                'A', 'AStatus', 'ACoverage', 'AGaps', 'lostCopiedUpdateCount',
                'lostCopiedUpdateValidity', 'lostCopiedUpdateCoverage',
                'lostCopiedUpdateCoverageGaps'}
    if not required <= set(result):
        raise ValueError('Recorded observation lacks weighted-criteria-v2 evidence')
    categories = result['impactCategories']
    if not isinstance(categories, list) or any(not isinstance(row, dict) for row in categories) \
            or sorted(row.get('category') for row in categories) != sorted(PERSISTENT):
        raise ValueError('Recorded observation has incompatible ImpactV2 categories')


def _same_assessment(stored, computed):
    """Compare assessment meaning while tolerating historical reason ordering."""
    fields = {'fitnessScore', 'fitnessComponents', 'fitnessUnavailableReasons'}
    return isinstance(stored, dict) and set(stored) == fields \
        and stored['fitnessScore'] == computed['fitnessScore'] \
        and stored['fitnessComponents'] == computed['fitnessComponents'] \
        and sorted(stored['fitnessUnavailableReasons']) == sorted(computed['fitnessUnavailableReasons'])


class RecordedFeedbackEvaluator:
    """Complete-map evaluator that reveals one selected observation per call."""
    mode = 'RECORDED_FEEDBACK_SEQUENTIAL'

    def __init__(self, references, domains, original_fitness):
        self.observations = {}
        self.revealed = set()
        for workload_id, reference in references.items():
            domain = domains[workload_id]
            if not isinstance(reference, dict) or set(reference) != {'candidates', 'observations', 'fitness'}:
                raise ValueError('Unsupported recorded reference schema')
            keys = set(domain.candidates)
            if keys != set(reference['candidates']) or keys != set(reference['observations']) \
                    or keys != set(reference['fitness']):
                raise ValueError('Recorded reference is not complete for the catalogue')
            policy = original_fitness[workload_id]
            for key in sorted(keys):
                candidate = domain.candidates[key]
                if candidate['key'] != key or candidate_key(workload_id, candidate['faultVector'],
                                                             candidate['actions']) != key:
                    raise ValueError('Catalogue candidate identity mismatch')
                if reference['candidates'][key] != candidate:
                    raise ValueError('Reference candidate differs from catalogue')
                result = reference['observations'][key]
                computed = assess(result, policy)
                _require_weighted_observation(result, candidate, computed)
                if not _same_assessment(reference['fitness'][key], computed):
                    raise ValueError('Stored fitness does not match the original map policy')
            self.observations[workload_id] = reference['observations']

    def evaluate(self, workload_id, candidate, attempt):
        identity = (workload_id, candidate['key'])
        if identity in self.revealed:
            raise ValueError('A candidate cannot be evaluated twice')
        if candidate != self.observations.get(workload_id, {}).get(candidate['key'], {}).get('candidate'):
            raise ValueError('Evaluator candidate identity mismatch')
        self.revealed.add(identity)
        return copy.deepcopy(self.observations[workload_id][candidate['key']])


def load_recorded_inputs(config):
    """Load and validate structure first, then construct the hidden evaluator."""
    workloads, domains, references, original_fitness = [], {}, {}, {}
    identities, names = set(), set()
    for row in config['workloads']:
        map_config_path = Path(row['config'])
        catalogue_path = Path(row['catalogue'])
        reference_path = Path(row['reference'])
        map_config = read(map_config_path)
        workload_id = map_config.get('workload')
        if not isinstance(workload_id, str) or not workload_id or workload_id in identities:
            raise ValueError('Workload identities must be unique nonempty strings')
        map_fitness = configuration(map_config.get('fitness'))
        if map_fitness['policy'] != WEIGHTED_V2:
            raise ValueError('Recorded map must use weighted-criteria-v2')
        domain, seal = load_catalogue(catalogue_path, map_config,
                                      read(catalogue_path / 'source-package-hashes.json'))
        data = package(catalogue_path / 'package/scenario-catalog-manifest.json')
        if domain.workload['id'] != workload_id or seal['workload'] != workload_id:
            raise ValueError('Catalogue/workload identity mismatch')
        name = row['name'] or workload_id
        if name in names:
            raise ValueError('Workload names must be unique')
        profile = structural_profile(data['records'], domain.workload)
        workloads.append({'id': workload_id, 'name': name, 'domain': domain,
                          'profile': profile, 'provenance': {
                              'config': str(map_config_path), 'configSha256': digest(map_config_path),
                              'catalogue': str(catalogue_path),
                              'catalogueSha256': digest(catalogue_path / 'catalogue.json'),
                              'reference': str(reference_path),
                              'referenceSha256': digest(reference_path),
                              'candidateCount': len(domain.candidates)}})
        identities.add(workload_id)
        names.add(name)
        domains[workload_id] = domain
        original_fitness[workload_id] = map_fitness
    # Outcomes are opened only by this evaluator and never enter feature construction.
    for row, workload in zip(config['workloads'], workloads):
        references[workload['id']] = read(Path(row['reference']))
    return workloads, RecordedFeedbackEvaluator(references, domains, original_fitness)


def allocate(workloads, evaluator, *, policy, parameters, seed, budget, fitness,
             emit=lambda decision: None, stop_requested=lambda: False):
    """Allocate one application-equivalent attempt per global decision."""
    if type(seed) is not int or type(budget) is not int or budget < 1:
        raise ValueError('Invalid global allocation seed or budget')
    fitness = configuration(fitness)
    if fitness['policy'] != WEIGHTED_V2:
        raise ValueError('Cross-workload allocation requires weighted-criteria-v2')
    if not workloads or len({row['id'] for row in workloads}) != len(workloads):
        raise ValueError('Require distinct admitted workloads')
    profiles = {row['id']: row['profile'] for row in workloads}
    features = ProgressFeatureSpace() if policy in PROGRESS_LINUCB_POLICIES \
        else FeatureSpace(profiles)
    states = {}
    for row in workloads:
        states[row['id']] = {**row, 'progress': {'allocated': 0, 'known': 0, 'unknown': 0,
                                                 'positives': 0, 'scoreSum': 0.0,
                                                 'bestScore': None},
                             'session': SearchSession(
                                 row['domain'], strategy='ga', seed=_local_seed(seed, row['id']),
                                 budget=max(budget, 8), population=8, mutation=0.3, stall_limit=100,
                                 fitness=fitness, exploration='uniform-unseen')}
    allocator = make_policy(policy, parameters, states, features, seed)
    decisions = []
    selection_ns = update_ns = 0
    cumulative_score = 0.0
    positives = unknowns = model_updates = 0
    stop = 'GLOBAL_BUDGET'
    while len(decisions) < budget:
        if stop_requested():
            stop = 'PAUSED'
            break
        selected_at = time.perf_counter_ns()
        active = {workload_id for workload_id, state in states.items()
                  if not state['session'].exhausted()
                  and state['session'].stop != 'PROPOSAL_STALL'}
        if not active:
            selection_ns += time.perf_counter_ns() - selected_at
            stop = 'CATALOGUE_EXHAUSTED'
            break
        workload_id, choice, context = allocator.select(active, states)
        state = states[workload_id]
        candidate = state['session'].ask()
        selection_elapsed = time.perf_counter_ns() - selected_at
        selection_ns += selection_elapsed
        if candidate is None:
            continue
        result = evaluator.evaluate(workload_id, candidate, len(decisions) + 1)
        updated_at = time.perf_counter_ns()
        item = state['session'].tell(candidate, result)
        reward = item['fitnessScore']
        progress = state['progress']
        progress['allocated'] += 1
        if reward is None:
            progress['unknown'] += 1
            unknowns += 1
        else:
            progress['known'] += 1
            progress['scoreSum'] += reward
            progress['bestScore'] = reward if progress['bestScore'] is None \
                else max(progress['bestScore'], reward)
            cumulative_score += reward
            if reward > 0:
                progress['positives'] += 1
                positives += 1
        updated = allocator.update(workload_id, context, reward)
        model_updates += int(updated)
        update_elapsed = time.perf_counter_ns() - updated_at
        update_ns += update_elapsed
        decision = {'decision': len(decisions) + 1, 'workload': workload_id,
                    'workloadName': state['name'], 'candidate': candidate['key'],
                    'scenarioId': candidate['id'], 'localAttempt': len(state['session'].attempts),
                    'operator': item['operator'], 'score': reward,
                    'positive': reward is not None and reward > 0,
                    'unknown': reward is None, 'modelUpdated': updated,
                    'cumulativeScore': cumulative_score,
                    'positiveDiscoveries': positives, 'unknowns': unknowns,
                    'selectionOverheadMicros': selection_elapsed / 1000.0,
                    'updateOverheadMicros': update_elapsed / 1000.0,
                    **choice}
        decisions.append(decision)
        emit(decision)
    per_workload = []
    for workload_id in sorted(states):
        state = states[workload_id]
        session = state['session']
        progress = state['progress']
        local_stop = ('EXHAUSTED' if session.exhausted() else
                      'PROPOSAL_STALL' if session.stop == 'PROPOSAL_STALL'
                      else 'PAUSED_GLOBAL_BUDGET')
        per_workload.append({'workload': workload_id, 'name': state['name'],
                             'candidateCount': len(state['domain'].candidates),
                             'allocations': progress['allocated'], 'knownScores': progress['known'],
                             'unknowns': progress['unknown'],
                             'positiveDiscoveries': progress['positives'],
                             'cumulativeScore': progress['scoreSum'],
                             'bestScore': progress['bestScore'], 'localStopReason': local_stop,
                             'populationSize': len(session.parents),
                             'seenCandidates': len(session.seen),
                             'proposalCount': len(session.proposals),
                             'duplicateProposals': session.duplicates})
    return {'schemaVersion': SCHEMA, 'mode': getattr(evaluator, 'mode', 'EVALUATOR'),
            'policy': policy, 'policyParameters': parameters, 'seed': seed,
            'globalBudget': budget, 'globalAttempts': len(decisions), 'stopReason': stop,
            'fitness': fitness, 'localSearch': {'strategy': 'ga', 'population': 8,
                'mutation': 0.3, 'exploration': 'uniform-unseen',
                'statePersistence': 'population-seen-rng-history'},
            'positiveDiscoveries': positives, 'cumulativeScore': cumulative_score,
            'unknowns': unknowns, 'modelUpdates': model_updates,
            'selectionOverheadMicros': selection_ns / 1000.0,
            'updateOverheadMicros': update_ns / 1000.0,
            'overheadScope': {
                'selectionIncludes': ['active-workload-and-exhaustion-scan',
                                      'outer-policy-choice', 'local-ga-ask-and-proposal-fallback',
                                      'final-exhaustion-scan-when-reached'],
                'updateIncludes': ['local-ga-tell-and-fitness-assessment',
                                   'observed-progress-update', 'adaptive-model-update'],
                'excludes': ['input-validation-and-model-initialization', 'evaluator-latency',
                             'application-latency', 'result-serialization']},
            'featureConstruction': {'outcomeInputs': False, 'names': features.names,
                                    'binaryGroupScaling': ('not-used' if policy in PROGRESS_LINUCB_POLICIES
                                                           else '1/sqrt(group-token-count)'),
                                    'countScaling': ('not-used' if policy in PROGRESS_LINUCB_POLICIES
                                                     else 'count/(1+count)'),
                                    'scoreProgressScaling': 'score/(1+score)'},
            'policyState': allocator.summary(), 'perWorkload': per_workload,
            'decisions': decisions}
