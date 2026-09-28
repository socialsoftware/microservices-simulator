import unittest

import allocator
from catalogue import RecordedDomain
from fitness import CRITERIA_V2, PERSISTENT, configuration
from search import candidate_key


FITNESS = configuration({'policy': 'weighted-criteria-v2',
                         'weights': {name: 1 for name in CRITERIA_V2}})
GUARDED_POLICIES = ('adaptive-ucb-cooldown', 'progress-linucb-cooldown',
                    'contextual-linucb-cooldown')
PARAMETERS = {
    'adaptive-ucb-cooldown': {'exploration': 1.0},
    'progress-linucb-cooldown': {'exploration': 1.0, 'ridge': 1.0},
    'contextual-linucb-cooldown': {'exploration': 1.0, 'ridge': 1.0}
}


def profile():
    return {'counts': {'sagas': 1, 'pairs': 0, 'interactions': 0, 'events': 0},
            'groups': {'saga': ['saga:shared'], 'pair': [],
                       'interaction': [], 'event': []}}


def progress():
    return {'allocated': 0, 'known': 0, 'unknown': 0, 'positives': 0,
            'scoreSum': 0.0, 'bestScore': None}


def observe(state, reward):
    state['progress']['allocated'] += 1
    if reward is None:
        state['progress']['unknown'] += 1
    else:
        state['progress']['known'] += 1
        state['progress']['scoreSum'] += reward
        state['progress']['bestScore'] = reward if state['progress']['bestScore'] is None \
            else max(state['progress']['bestScore'], reward)
        state['progress']['positives'] += int(reward > 0)


def workload(workload_id, count=40):
    structure = {'id': workload_id,
                 'participants': [{'id': 'p1', 'saga': 'shared', 'input': 'i'}],
                 'interactions': [],
                 'schedule': [{'id': 's1', 'kind': 'step',
                               'participant': 'p1', 'faultSlot': 0}]}
    candidates = {}
    for index in range(count):
        vector = str(index % 2)
        actions = [{'step': 's1'}, {'compensate': f'c{index}'}]
        key = candidate_key(workload_id, vector, actions)
        candidates[key] = {'id': f'{workload_id}-{index}', 'workload': workload_id,
                           'faultVector': vector, 'actions': actions,
                           'key': key, 'aliases': [f'{workload_id}-{index}']}
    return {'id': workload_id, 'name': workload_id,
            'domain': RecordedDomain(structure, candidates), 'profile': profile()}


def available(candidate, score):
    categories = [{'category': name,
                   'positiveObjectCount': score if name == 'FAILED_OPERATION_RESIDUAL' else 0,
                   'coverageStatus': 'COMPLETE', 'unknownReasons': []}
                  for name in PERSISTENT]
    return {'candidate': candidate, 'status': 'COMPLETE',
            'terminalStatus': 'COMPENSATED', 'scheduleConformance': 'EXACT',
            'I': score, 'impactCategories': categories, 'A': 0,
            'AStatus': 'COMPLETE', 'ACoverage': 'COMPLETE_WITHIN_SCOPE', 'AGaps': [],
            'lostCopiedUpdateCount': 0, 'lostCopiedUpdateValidity': 'COMPLETE',
            'lostCopiedUpdateCoverage': 'COMPLETE_WITHIN_SCOPE',
            'lostCopiedUpdateCoverageGaps': []}


def unavailable(candidate):
    return {'candidate': candidate, 'status': 'PROCESS_FAILURE', 'I': None}


class Feedback:
    mode = 'COOLDOWN_TEST'

    def __init__(self, scores):
        self.scores = scores
        self.seen = set()

    def evaluate(self, workload_id, candidate, attempt):
        identity = (workload_id, candidate['key'])
        if identity in self.seen:
            raise AssertionError('candidate repeated')
        self.seen.add(identity)
        score = self.scores[workload_id]
        return unavailable(candidate) if score is None else available(candidate, score)


def run_guarded(policy, workloads, scores, budget):
    return allocator.allocate(workloads, Feedback(scores), policy=policy,
                              parameters=PARAMETERS[policy], seed=1,
                              budget=budget, fitness=FITNESS)


class CooldownPolicyTest(unittest.TestCase):
    def test_stress_case_gives_second_decision_to_available_peer(self):
        for policy in GUARDED_POLICIES:
            with self.subTest(policy=policy):
                result = run_guarded(policy, [workload('a'), workload('b')],
                                     {'a': None, 'b': 1}, budget=20)
                self.assertEqual([row['workload'] for row in result['decisions'][:2]],
                                 ['a', 'b'])
                self.assertLess(next(row['allocations'] for row in result['perWorkload']
                                     if row['workload'] == 'a'), 20)
                self.assertGreater(result['modelUpdates'], 0)

    def test_all_unknown_alternates_without_deadlock(self):
        for policy in GUARDED_POLICIES:
            with self.subTest(policy=policy):
                result = run_guarded(policy, [workload('a'), workload('b')],
                                     {'a': None, 'b': None}, budget=20)
                self.assertEqual([row['workload'] for row in result['decisions']],
                                 ['a', 'b'] * 10)
                self.assertEqual((result['globalAttempts'], result['unknowns'],
                                  result['modelUpdates']), (20, 20, 0))
                self.assertTrue(all(row['populationSize'] == 0
                                    for row in result['perWorkload']))
                self.assertEqual(result['stopReason'], 'GLOBAL_BUDGET')

    def test_only_active_workload_remains_eligible_and_consumes_budget(self):
        for policy in GUARDED_POLICIES:
            with self.subTest(policy=policy):
                result = run_guarded(policy, [workload('a')], {'a': None}, budget=20)
                self.assertEqual(result['globalAttempts'], 20)
                self.assertTrue(all(row['workload'] == 'a' for row in result['decisions']))
                self.assertEqual(result['unknowns'], 20)

    def test_available_zero_updates_model_and_does_not_start_cooldown(self):
        states = {name: {'profile': profile(), 'progress': progress()}
                  for name in ('a', 'b')}
        for name in GUARDED_POLICIES:
            with self.subTest(policy=name):
                local_states = {key: {'profile': value['profile'],
                                      'progress': progress()}
                                for key, value in states.items()}
                space = (allocator.ProgressFeatureSpace()
                         if name == 'progress-linucb-cooldown'
                         else allocator.FeatureSpace({'a': profile(), 'b': profile()}))
                policy = allocator.make_policy(name, PARAMETERS[name], local_states, space, seed=1)
                selected, _, context = policy.select({'a', 'b'}, local_states)
                self.assertEqual(selected, 'a')
                observe(local_states['a'], 0)
                self.assertTrue(policy.update('a', context, 0))
                self.assertIsNone(policy.cooldown)
                self.assertEqual(policy.updates, 1)

    def test_switching_preserves_each_local_attempt_sequence(self):
        for policy in GUARDED_POLICIES:
            with self.subTest(policy=policy):
                result = run_guarded(policy, [workload('a'), workload('b')],
                                     {'a': None, 'b': None}, budget=6)
                self.assertEqual([(row['workload'], row['localAttempt'])
                                  for row in result['decisions']],
                                 [('a', 1), ('b', 1), ('a', 2),
                                  ('b', 2), ('a', 3), ('b', 3)])
                keys = [row['candidate'] for row in result['decisions']]
                self.assertEqual(len(keys), len(set(keys)))
                self.assertEqual([row['seenCandidates']
                                  for row in result['perWorkload']], [3, 3])


if __name__ == '__main__':
    unittest.main()
