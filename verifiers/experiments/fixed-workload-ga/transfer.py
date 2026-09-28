"""Matched warm/cold transfer experiment over recorded fixed-workload feedback.

The training prefix is generated once by stable round-robin. Both warm models learn
from the same pre-update contexts and revealed rewards. Target arms start with fresh
local GA and progress state; warm arms receive only the learned linear model arrays.
"""
import copy
import hashlib
import math

from allocator import (ContextualLinUcbPolicy, FeatureSpace, ProgressFeatureSpace,
                       RoundRobinPolicy, _local_seed)
from fitness import CRITERIA_V2, WEIGHTED_V2, configuration
from search import SearchSession, stable


SCHEMA = 'contextual-workload-transfer.v1'
ARM_SPECS = (
    ('structural-warm', 'structural', True),
    ('structural-cold', 'structural', False),
    ('progress-warm', 'progress', True),
    ('progress-cold', 'progress', False),
)
DEFAULT_CHECKPOINTS = (16, 32, 64, 128, 256)


def _sha256(value):
    return hashlib.sha256(stable(value).encode()).hexdigest()


def _fitness():
    return configuration({'policy': WEIGHTED_V2,
                          'weights': {name: 1 for name in CRITERIA_V2}})


def _zero_progress():
    return {'allocated': 0, 'known': 0, 'unknown': 0, 'positives': 0,
            'scoreSum': 0.0, 'bestScore': None}


def _new_states(workloads, seed, budget, fitness):
    states = {}
    for row in workloads:
        states[row['id']] = {
            **row,
            'progress': _zero_progress(),
            'session': SearchSession(
                row['domain'], strategy='ga', seed=_local_seed(seed, row['id']),
                budget=max(budget, 8), population=8, mutation=0.3,
                stall_limit=100, fitness=fitness, exploration='uniform-unseen')}
    return states


def _active(states):
    return {workload_id for workload_id, state in states.items()
            if not state['session'].exhausted()
            and state['session'].stop != 'PROPOSAL_STALL'}


def _observe(progress, reward):
    progress['allocated'] += 1
    if reward is None:
        progress['unknown'] += 1
        return
    progress['known'] += 1
    progress['scoreSum'] += reward
    progress['bestScore'] = (reward if progress['bestScore'] is None
                             else max(progress['bestScore'], reward))
    if reward > 0:
        progress['positives'] += 1


def _linear_state(policy):
    """Return exactly the state permitted to cross the train/test boundary."""
    return {'inverse': copy.deepcopy(policy.inverse),
            'response': list(policy.response),
            'theta': list(policy.theta),
            'updates': policy.updates}


def _load_linear_state(policy, state):
    dimension = len(policy.feature_space.names)
    if len(state['inverse']) != dimension \
            or any(len(row) != dimension for row in state['inverse']) \
            or len(state['response']) != dimension or len(state['theta']) != dimension:
        raise ValueError('Transferred linear state does not match the frozen feature space')
    policy.inverse = copy.deepcopy(state['inverse'])
    policy.response = list(state['response'])
    policy.theta = list(state['theta'])
    policy.updates = state['updates']


def _model_evidence(policy):
    state = _linear_state(policy)
    return {'stateSha256': _sha256(state),
            'inverseSha256': _sha256(state['inverse']),
            'responseSha256': _sha256(state['response']),
            'thetaSha256': _sha256(state['theta']),
            'updates': state['updates'],
            'featureCount': len(policy.feature_space.names),
            'nonzeroTheta': sum(value != 0.0 for value in state['theta']),
            'thetaL2': math.sqrt(math.fsum(value * value for value in state['theta']))}


def _session_start_evidence(states):
    rows = [{'workload': workload_id,
             'seed': state['session'].seed,
             'attempts': len(state['session'].attempts),
             'seen': len(state['session'].seen),
             'parents': len(state['session'].parents),
             'progress': copy.deepcopy(state['progress'])}
            for workload_id, state in sorted(states.items())]
    return {'allProgressZero': all(row['progress'] == _zero_progress() for row in rows),
            'totalAttempts': sum(row['attempts'] for row in rows),
            'totalSeen': sum(row['seen'] for row in rows),
            'totalParents': sum(row['parents'] for row in rows),
            'stateSha256': _sha256(rows)}


def _initial_ranking(policy, states):
    rows = []
    for workload_id in sorted(states):
        chosen, choice, context = policy.select({workload_id}, states)
        if chosen != workload_id:
            raise RuntimeError('Single-workload ranking selected another workload')
        rows.append({'workload': workload_id, 'index': choice['index'],
                     'estimate': choice['estimate'],
                     'uncertainty': choice['uncertainty'],
                     'contextSha256': _sha256(context)})
    rows.sort(key=lambda row: (-row['index'], row['workload']))
    return [{**row, 'rank': rank} for rank, row in enumerate(rows, 1)]


def _compact_workload_rows(states):
    rows = []
    for workload_id, state in sorted(states.items()):
        progress = state['progress']
        session = state['session']
        rows.append({'workload': workload_id,
                     'candidateCount': len(state['domain'].candidates),
                     'allocations': progress['allocated'],
                     'knownScores': progress['known'],
                     'unknowns': progress['unknown'],
                     'positiveDiscoveries': progress['positives'],
                     'cumulativeScore': progress['scoreSum'],
                     'bestScore': progress['bestScore'],
                     'seenCandidates': len(session.seen),
                     'populationSize': len(session.parents),
                     'duplicateProposals': session.duplicates})
    return rows


def _checkpoint(attempt, positives, score, unknowns, states):
    return {'attempt': attempt, 'positiveDiscoveries': positives,
            'cumulativeScore': score, 'unknowns': unknowns,
            'allocations': {workload_id: state['progress']['allocated']
                            for workload_id, state in sorted(states.items())}}


def _train(workloads, evaluator, seed, budget, fitness, structural, progress):
    states = _new_states(workloads, seed, budget, fitness)
    chooser = RoundRobinPolicy(states)
    decisions = []
    while len(decisions) < budget:
        active = _active(states)
        if not active:
            break
        workload_id, _, _ = chooser.select(active, states)
        state = states[workload_id]
        candidate = state['session'].ask()
        if candidate is None:
            continue

        # These vectors deliberately precede both feedback and progress mutation.
        before = copy.deepcopy(state['progress'])
        structural_context = structural.feature_space.vector(state['profile'], before)
        progress_context = progress.feature_space.vector(state['profile'], before)
        result = evaluator.evaluate(workload_id, candidate, len(decisions) + 1)
        item = state['session'].tell(candidate, result)
        reward = item['fitnessScore']
        _observe(state['progress'], reward)
        structural_updated = structural.update(workload_id, structural_context, reward)
        progress_updated = progress.update(workload_id, progress_context, reward)
        decisions.append({
            'decision': len(decisions) + 1, 'workload': workload_id,
            'candidate': candidate['key'], 'scenarioId': candidate['id'],
            'localAttempt': len(state['session'].attempts), 'operator': item['operator'],
            'score': reward, 'positive': reward is not None and reward > 0,
            'unknown': reward is None, 'preUpdateProgress': before,
            'structuralContextSha256': _sha256(structural_context),
            'progressContextSha256': _sha256(progress_context),
            'structuralModelUpdated': structural_updated,
            'progressModelUpdated': progress_updated})
    stop = 'TRAIN_BUDGET' if len(decisions) == budget else 'TRAIN_CATALOGUE_EXHAUSTED'
    known = [row for row in decisions if row['score'] is not None]
    return {'phase': 'training', 'budget': budget, 'attempts': len(decisions), 'stopReason': stop,
            'positiveDiscoveries': sum(row['score'] > 0 for row in known),
            'cumulativeScore': math.fsum(row['score'] for row in known),
            'unknowns': len(decisions) - len(known),
            'modelUpdates': {'structural': structural.updates,
                             'progress': progress.updates},
            'decisions': decisions, 'perWorkload': _compact_workload_rows(states),
            'historySha256': _sha256(decisions)}


def _test_arm(name, family, warm, workloads, evaluator, seed, budget, checkpoints,
              fitness, feature_spaces, learned_states, training_hash):
    states = _new_states(workloads, seed, budget, fitness)
    policy = ContextualLinUcbPolicy(feature_spaces[family], exploration=1.0, ridge=1.0)
    if warm:
        _load_linear_state(policy, learned_states[family])
    model_start = _model_evidence(policy)
    training_updates = model_start['updates']
    target_start = _session_start_evidence(states)
    ranking = _initial_ranking(policy, states)
    decisions = []
    checkpoint_rows = []
    checkpoint_set = set(checkpoints)
    positives = unknowns = 0
    cumulative_score = 0.0
    target_updates = 0
    while len(decisions) < budget:
        active = _active(states)
        if not active:
            break
        workload_id, choice, context = policy.select(active, states)
        state = states[workload_id]
        candidate = state['session'].ask()
        if candidate is None:
            continue
        result = evaluator.evaluate(workload_id, candidate, len(decisions) + 1)
        item = state['session'].tell(candidate, result)
        reward = item['fitnessScore']
        _observe(state['progress'], reward)
        updated = policy.update(workload_id, context, reward)
        target_updates += int(updated)
        if reward is None:
            unknowns += 1
        else:
            cumulative_score += reward
            positives += int(reward > 0)
        attempt = len(decisions) + 1
        decisions.append({
            'decision': attempt, 'workload': workload_id,
            'candidate': candidate['key'], 'scenarioId': candidate['id'],
            'localAttempt': len(state['session'].attempts), 'operator': item['operator'],
            'score': reward, 'positive': reward is not None and reward > 0,
            'unknown': reward is None, 'modelUpdated': updated,
            'index': choice['index'], 'estimate': choice['estimate'],
            'uncertainty': choice['uncertainty'],
            'contextSha256': _sha256(context),
            'positiveDiscoveries': positives, 'cumulativeScore': cumulative_score,
            'unknowns': unknowns})
        if attempt in checkpoint_set:
            checkpoint_rows.append(_checkpoint(attempt, positives, cumulative_score,
                                               unknowns, states))
    stop = 'TARGET_BUDGET' if len(decisions) == budget else 'TARGET_CATALOGUE_EXHAUSTED'
    final_model = _model_evidence(policy)
    return {'phase': 'target', 'arm': name, 'featureFamily': family, 'warm': warm,
            'seed': seed, 'budget': budget, 'attempts': len(decisions),
            'stopReason': stop, 'commonTrainingHistorySha256': training_hash,
            'transferredState': (['inverse', 'response', 'theta', 'updates'] if warm else []),
            'targetStart': target_start, 'modelStart': model_start,
            'trainingModelUpdates': training_updates,
            'initialRanking': ranking,
            'initialTopChoice': ranking[0]['workload'] if ranking else None,
            'checkpoints': checkpoint_rows,
            'positiveDiscoveries': positives, 'cumulativeScore': cumulative_score,
            'unknowns': unknowns, 'targetModelUpdates': target_updates,
            'totalModelUpdates': final_model['updates'], 'finalModel': final_model,
            'perWorkload': _compact_workload_rows(states), 'decisions': decisions}


def run_transfer(workloads, evaluator_factory, train_ids, test_ids, seed,
                 train_budget=132, test_budget=256,
                 checkpoints=DEFAULT_CHECKPOINTS):
    """Run one matched seed of the four-arm recorded-feedback transfer protocol.

    ``evaluator_factory`` must return a fresh evaluator exposing
    ``evaluate(workload_id, candidate, attempt)``. It is invoked once for the shared
    training prefix and once per target arm, so reveal state never crosses arms.
    """
    if type(seed) is not int or type(train_budget) is not int or train_budget < 1 \
            or type(test_budget) is not int or test_budget < 1:
        raise ValueError('Seed and positive integer train/test budgets are required')
    if not callable(evaluator_factory):
        raise ValueError('evaluator_factory must be callable')
    if not isinstance(checkpoints, (list, tuple)) \
            or any(type(value) is not int or value < 1 or value > test_budget
                   for value in checkpoints) \
            or list(checkpoints) != sorted(set(checkpoints)):
        raise ValueError('Checkpoints must be unique increasing attempts within the test budget')
    if not isinstance(workloads, (list, tuple)) or not workloads:
        raise ValueError('workloads must be a nonempty sequence')
    by_id = {row.get('id'): row for row in workloads if isinstance(row, dict)}
    if len(by_id) != len(workloads) or None in by_id:
        raise ValueError('Workloads require unique identities')
    train_ids, test_ids = list(train_ids), list(test_ids)
    if not train_ids or not test_ids or len(set(train_ids)) != len(train_ids) \
            or len(set(test_ids)) != len(test_ids):
        raise ValueError('Train and test identities must be nonempty and unique')
    if set(train_ids) & set(test_ids):
        raise ValueError('Train and test identities must be disjoint')
    if not (set(train_ids) | set(test_ids)) <= set(by_id):
        raise ValueError('Train or test identity is not an admitted workload')

    train = [by_id[workload_id] for workload_id in sorted(train_ids)]
    test = [by_id[workload_id] for workload_id in sorted(test_ids)]
    admitted = {row['id']: row['profile'] for row in (*train, *test)}
    feature_spaces = {'structural': FeatureSpace(admitted),
                      'progress': ProgressFeatureSpace()}
    if len(feature_spaces['progress'].names) != 7:
        raise RuntimeError('Progress-only feature contract changed')
    fitness = _fitness()
    warm_models = {
        family: ContextualLinUcbPolicy(space, exploration=1.0, ridge=1.0)
        for family, space in feature_spaces.items()}
    training = _train(train, evaluator_factory(), seed, train_budget, fitness,
                      warm_models['structural'], warm_models['progress'])
    learned_states = {family: _linear_state(model)
                      for family, model in warm_models.items()}

    arms = []
    for name, family, warm in ARM_SPECS:
        arms.append(_test_arm(name, family, warm, test, evaluator_factory(), seed,
                              test_budget, tuple(checkpoints), fitness, feature_spaces,
                              learned_states, training['historySha256']))

    candidate_inventory = [{'workload': row['id'],
                            'candidateCount': len(row['domain'].candidates),
                            'candidateKeysSha256': _sha256(sorted(row['domain'].candidates)),
                            'profileSha256': _sha256(row['profile'])}
                           for row in (*train, *test)]
    return {
        'schemaVersion': SCHEMA, 'seed': seed,
        'protocol': {'trainBudget': train_budget, 'testBudget': test_budget,
                     'checkpoints': list(checkpoints), 'population': 8,
                     'mutation': 0.3, 'exploration': 'uniform-unseen',
                     'linearExploration': 1.0, 'ridge': 1.0,
                     'cooldown': False, 'fitness': fitness,
                     'trainingSelection': 'stable-round-robin-skipping-exhausted',
                     'trainingCost': 'one-common-prior-prefix-reported-separately',
                     'coldCostInterpretation': 'not-end-to-end-cold-policy-cost-equivalence',
                     'targetState': 'fresh-ga-and-progress-per-arm',
                     'warmTransfer': ['inverse', 'response', 'theta', 'updates']},
        'split': {'trainIds': sorted(train_ids), 'testIds': sorted(test_ids),
                  'disjoint': True,
                  'trainCandidateCount': sum(len(row['domain'].candidates) for row in train),
                  'testCandidateCount': sum(len(row['domain'].candidates) for row in test)},
        'inputEvidence': {'candidateInventorySha256': _sha256(candidate_inventory),
                          'candidateInventory': candidate_inventory,
                          'outcomesUsedForFeatures': False},
        'featureSpaces': {
            family: {'names': space.names, 'namesSha256': _sha256(space.names),
                     'dimension': len(space.names),
                     'profileIds': sorted(admitted) if family == 'structural' else []}
            for family, space in feature_spaces.items()},
        'training': training,
        'trainingModelEvidence': {family: _model_evidence(model)
                                  for family, model in warm_models.items()},
        'targetResults': arms}
