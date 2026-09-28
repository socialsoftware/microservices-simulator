import copy
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import allocator as allocator_module
from allocator import (ContextualLinUcbPolicy, FACTORIAL_POLICIES, FeatureSpace,
                       POLICIES, ProgressFeatureSpace, RUNTIME_FAILURE_STATUSES,
                       RecordedFeedbackEvaluator, SUPPORTED_POLICIES, allocate,
                       make_policy, parse_configuration)
from catalogue import RecordedDomain
from fitness import CRITERIA_V2, PERSISTENT, assess, configuration
from search import candidate_key


FITNESS = configuration({'policy': 'weighted-criteria-v2',
                         'weights': {name: 1 for name in CRITERIA_V2}})
PARAMETERS = {
    'adaptive-ucb': {'exploration': 1.0},
    'adaptive-ucb-cooldown': {'exploration': 1.0},
    'progress-linucb': {'exploration': 1.0, 'ridge': 1.0},
    'progress-linucb-cooldown': {'exploration': 1.0, 'ridge': 1.0},
    'contextual-linucb': {'exploration': 1.0, 'ridge': 1.0},
    'contextual-linucb-cooldown': {'exploration': 1.0, 'ridge': 1.0}
}


def profile(*sagas, interactions=(), events=()):
    pairs = [f'saga-pair:{left}|{right}' for index, left in enumerate(sorted(sagas))
             for right in sorted(sagas)[index + 1:]]
    return {'counts': {'sagas': len(sagas), 'pairs': len(pairs),
                       'interactions': len(interactions), 'events': len(events)},
            'groups': {'saga': ['saga:' + saga for saga in sorted(sagas)],
                       'pair': pairs,
                       'interaction': ['interaction:' + value for value in interactions],
                       'event': ['event:' + value for value in events]}}


def complete_result(candidate, score=0, unknown=False):
    categories = []
    for name in PERSISTENT:
        value = score if name == 'FAILED_OPERATION_RESIDUAL' else 0
        categories.append({'category': name, 'positiveObjectCount': value,
                           'coverageStatus': ('PARTIAL' if unknown and name == 'FAILED_OPERATION_RESIDUAL'
                                              else 'COMPLETE'),
                           'unknownReasons': ([{'reason': 'fixture'}]
                                              if unknown and name == 'FAILED_OPERATION_RESIDUAL' else [])})
    return {'candidate': candidate, 'status': 'PARTIAL' if unknown else 'COMPLETE',
            'terminalStatus': 'PARTIAL_COMPENSATED' if unknown else 'COMPENSATED',
            'scheduleConformance': 'EXACT', 'I': None if unknown else score,
            'impactCategories': categories, 'A': 0, 'AStatus': 'COMPLETE',
            'ACoverage': 'COMPLETE_WITHIN_SCOPE', 'AGaps': [],
            'lostCopiedUpdateCount': 0, 'lostCopiedUpdateValidity': 'COMPLETE',
            'lostCopiedUpdateCoverage': 'COMPLETE_WITHIN_SCOPE',
            'lostCopiedUpdateCoverageGaps': []}


def failure_result(candidate, status):
    return {'candidate': candidate, 'status': status, 'I': None, 'A': None,
            'AStatus': 'UNAVAILABLE', 'ACoverage': 'UNAVAILABLE',
            'lostCopiedUpdateCount': None, 'lostCopiedUpdateValidity': 'UNAVAILABLE',
            'lostCopiedUpdateCoverage': 'UNAVAILABLE',
            'lostCopiedUpdateCoverageGaps': []}


def fixture_workload(workload_id, count, saga):
    workload = {'id': workload_id, 'participants': [{'id': 'p1', 'saga': saga, 'input': 'i'}],
                'interactions': [], 'schedule': [
                    {'id': 's1', 'kind': 'step', 'participant': 'p1', 'faultSlot': 0}]}
    candidates = {}
    for index in range(count):
        vector = str(index % 2)
        actions = [{'step': 's1'}, {'compensate': f'c{index}'}]
        key = candidate_key(workload_id, vector, actions)
        candidates[key] = {'id': f'{workload_id}-s{index}', 'workload': workload_id,
                           'faultVector': vector, 'actions': actions, 'key': key,
                           'aliases': [f'{workload_id}-s{index}']}
    return {'id': workload_id, 'name': workload_id, 'domain': RecordedDomain(workload, candidates),
            'profile': profile(saga)}


class FakeEvaluator:
    mode = 'SYNTHETIC_RECORDED_FEEDBACK'

    def __init__(self, scores):
        self.scores = scores
        self.seen = set()

    def evaluate(self, workload_id, candidate, attempt):
        identity = (workload_id, candidate['key'])
        if identity in self.seen:
            raise AssertionError('duplicate application attempt')
        self.seen.add(identity)
        value = self.scores.get(identity, 0)
        if value in RUNTIME_FAILURE_STATUSES:
            return failure_result(candidate, value)
        return complete_result(candidate, 0 if value is None else value, value is None)


def run_fixture(policy, *, seed=7, budget=6, scores=None, counts=(2, 2, 2), parameters=None):
    workloads = [fixture_workload(name, count, 'shared' if name != 'c' else 'other')
                 for name, count in zip(('a', 'b', 'c'), counts)]
    evaluator = FakeEvaluator(scores or {})
    result = allocate(workloads, evaluator, policy=policy,
                      parameters=parameters if parameters is not None else PARAMETERS.get(policy, {}),
                      seed=seed, budget=budget, fitness=FITNESS)
    return result, evaluator


class AllocatorTest(unittest.TestCase):
    def test_round_robin_balances_until_a_workload_exhausts(self):
        result, evaluator = run_fixture('round-robin', budget=7, counts=(2, 4, 4))
        allocations = {row['workload']: row['allocations'] for row in result['perWorkload']}
        self.assertEqual(allocations, {'a': 2, 'b': 3, 'c': 2})
        self.assertEqual(len(evaluator.seen), 7)
        self.assertEqual(result['stopReason'], 'GLOBAL_BUDGET')

    def test_exhaustion_stops_before_larger_global_budget(self):
        result, evaluator = run_fixture('uniform', budget=20)
        self.assertEqual(result['globalAttempts'], 6)
        self.assertEqual(result['stopReason'], 'CATALOGUE_EXHAUSTED')
        self.assertEqual(len(evaluator.seen), 6)
        self.assertTrue(all(row['localStopReason'] == 'EXHAUSTED'
                            for row in result['perWorkload']))

    def test_unknown_consumes_budget_without_parent_or_model_update(self):
        for policy in FACTORIAL_POLICIES:
            with self.subTest(policy=policy):
                workloads = [fixture_workload('a', 2, 'shared')]
                scores = {('a', key): None for key in workloads[0]['domain'].candidates}
                evaluator = FakeEvaluator(scores)
                result = allocate(workloads, evaluator, policy=policy,
                                  parameters=PARAMETERS[policy], seed=1, budget=2,
                                  fitness=FITNESS)
                row = result['perWorkload'][0]
                self.assertEqual((result['unknowns'], result['modelUpdates']), (2, 0))
                self.assertEqual((row['populationSize'], row['knownScores']), (0, 0))
                self.assertTrue(all(not decision['modelUpdated'] for decision in result['decisions']))

    def test_runtime_failures_consume_budget_without_parent_or_model_update(self):
        for policy in FACTORIAL_POLICIES:
            with self.subTest(policy=policy):
                workloads = [fixture_workload('a', len(RUNTIME_FAILURE_STATUSES), 'shared')]
                scores = {('a', key): status for key, status in
                          zip(workloads[0]['domain'].candidates, RUNTIME_FAILURE_STATUSES)}
                result = allocate(workloads, FakeEvaluator(scores), policy=policy,
                                  parameters=PARAMETERS[policy], seed=1,
                                  budget=len(RUNTIME_FAILURE_STATUSES), fitness=FITNESS)
                row = result['perWorkload'][0]
                self.assertEqual(result['globalAttempts'], len(RUNTIME_FAILURE_STATUSES))
                self.assertEqual(result['unknowns'], len(RUNTIME_FAILURE_STATUSES))
                self.assertEqual((result['modelUpdates'], row['populationSize']), (0, 0))
                self.assertTrue(all(decision['score'] is None and not decision['modelUpdated']
                                    for decision in result['decisions']))

    def test_seed_reproduces_every_policy(self):
        for policy in SUPPORTED_POLICIES:
            with self.subTest(policy=policy):
                left, _ = run_fixture(policy, scores={})
                right, _ = run_fixture(policy, scores={})
                fields = lambda result: [(row['workload'], row['candidate'], row['score'])
                                         for row in result['decisions']]
                self.assertEqual(fields(left), fields(right))

    def test_all_policies_cover_the_same_finite_catalogue(self):
        scores = {}
        for policy in SUPPORTED_POLICIES:
            result, evaluator = run_fixture(policy, budget=20, scores=scores)
            self.assertEqual(result['globalAttempts'], 6)
            self.assertEqual(len(evaluator.seen), 6)
            self.assertEqual(result['cumulativeScore'], 0)

    def test_unseen_result_changes_cannot_change_contextual_choices(self):
        left, _ = run_fixture('contextual-linucb', budget=2, scores={})
        first = (left['decisions'][0]['workload'], left['decisions'][0]['candidate'])
        changed = {}
        for workload in [fixture_workload(name, 2, 'shared' if name != 'c' else 'other')
                         for name in ('a', 'b', 'c')]:
            for key in workload['domain'].candidates:
                if (workload['id'], key) != first:
                    changed[(workload['id'], key)] = 999
        right, _ = run_fixture('contextual-linucb', budget=2, scores=changed)
        selected = lambda result: [(row['workload'], row['candidate'])
                                   for row in result['decisions']]
        self.assertEqual(selected(left), selected(right))
        self.assertFalse(left['featureConstruction']['outcomeInputs'])

    def test_shared_context_transfers_to_related_unmeasured_workload(self):
        profiles = {'a': profile('common', 'left'), 'b': profile('common', 'right'),
                    'c': profile('other', 'third')}
        space = FeatureSpace(profiles)
        states = {name: {'profile': value, 'progress': {'allocated': 0, 'known': 0,
                  'unknown': 0, 'positives': 0, 'scoreSum': 0.0, 'bestScore': None}}
                  for name, value in profiles.items()}
        policy = ContextualLinUcbPolicy(space, exploration=0.0, ridge=1.0)
        chosen, _, context = policy.select({'a', 'b', 'c'}, states)
        self.assertEqual(chosen, 'a')
        policy.update('a', context, 10.0)
        chosen, metadata, _ = policy.select({'b', 'c'}, states)
        self.assertEqual(chosen, 'b')
        self.assertGreater(metadata['estimate'], 0)

    def test_progress_only_context_cannot_see_structural_differences(self):
        space = ProgressFeatureSpace()
        observed = {'allocated': 3, 'known': 2, 'unknown': 1, 'positives': 1,
                    'scoreSum': 4.0, 'bestScore': 3.0}
        left = space.vector(profile('left', 'shared', interactions=('write',), events=('route',)),
                            observed)
        right = space.vector(profile('unrelated'), observed)
        self.assertEqual(left, right)
        self.assertEqual(space.names, ['bias', *allocator_module.PROGRESS_FEATURES])
        self.assertFalse(any(name.startswith('structure:') or name.startswith('saga:')
                             for name in space.names))

        result, _ = run_fixture('progress-linucb', budget=1)
        self.assertEqual(result['featureConstruction']['names'], space.names)
        self.assertEqual(result['featureConstruction']['binaryGroupScaling'], 'not-used')
        self.assertEqual(result['featureConstruction']['countScaling'], 'not-used')

        full_space = FeatureSpace({'left': profile('left'), 'right': profile('right')})
        policy = make_policy('progress-linucb', PARAMETERS['progress-linucb'],
                             ('left', 'right'), full_space, seed=1)
        self.assertEqual(policy.feature_space.names, space.names)

    def test_selection_timing_includes_active_and_local_exhaustion_scans(self):
        class Clock:
            def __init__(self):
                self.now = 0

            def __call__(self):
                return self.now

            def advance(self, nanoseconds):
                self.now += nanoseconds

        clock = Clock()
        workload = fixture_workload('a', 1, 'shared')
        exhausted = workload['domain'].exhausted

        def delayed_exhaustion(seen):
            clock.advance(1000)
            return exhausted(seen)

        workload['domain'].exhausted = delayed_exhaustion
        evaluator = FakeEvaluator({})
        original_evaluate = evaluator.evaluate

        def delayed_evaluate(workload_id, candidate, attempt):
            clock.advance(1_000_000)
            return original_evaluate(workload_id, candidate, attempt)

        evaluator.evaluate = delayed_evaluate
        with patch.object(allocator_module.time, 'perf_counter_ns', clock):
            result = allocate([workload], evaluator, policy='round-robin', parameters={},
                              seed=1, budget=2, fitness=FITNESS)
        self.assertEqual(result['decisions'][0]['selectionOverheadMicros'], 2.0)
        self.assertEqual(result['selectionOverheadMicros'], 3.0)
        self.assertEqual(result['updateOverheadMicros'], 0.0)
        self.assertIn('evaluator-latency', result['overheadScope']['excludes'])


class RecordedEvaluatorTest(unittest.TestCase):
    def reference(self):
        workload = fixture_workload('a', 2, 'shared')
        observations = {key: complete_result(candidate, index)
                        for index, (key, candidate) in enumerate(workload['domain'].candidates.items())}
        return workload, {'candidates': copy.deepcopy(workload['domain'].candidates),
                          'observations': observations,
                          'fitness': {key: assess(value, FITNESS)
                                      for key, value in observations.items()}}

    def test_validates_complete_candidate_join_and_stored_policy(self):
        workload, reference = self.reference()
        evaluator = RecordedFeedbackEvaluator({'a': reference}, {'a': workload['domain']},
                                               {'a': FITNESS})
        candidate = next(iter(workload['domain'].candidates.values()))
        self.assertEqual(evaluator.evaluate('a', candidate, 1)['candidate'], candidate)
        with self.assertRaises(ValueError):
            evaluator.evaluate('a', candidate, 2)

    def test_rejects_incompatible_reference_schema_and_identity(self):
        workload, reference = self.reference()
        broken = copy.deepcopy(reference)
        key = next(iter(broken['fitness']))
        broken['fitness'][key]['fitnessScore'] = 999
        with self.assertRaises(ValueError):
            RecordedFeedbackEvaluator({'a': broken}, {'a': workload['domain']}, {'a': FITNESS})
        missing = copy.deepcopy(reference)
        missing['observations'].pop(key)
        with self.assertRaises(ValueError):
            RecordedFeedbackEvaluator({'a': missing}, {'a': workload['domain']}, {'a': FITNESS})

    def test_accepts_declared_runtime_failures_with_null_stored_fitness(self):
        workload = fixture_workload('a', len(RUNTIME_FAILURE_STATUSES), 'shared')
        observations = {key: failure_result(candidate, status)
                        for (key, candidate), status in
                        zip(workload['domain'].candidates.items(), RUNTIME_FAILURE_STATUSES)}
        reference = {'candidates': copy.deepcopy(workload['domain'].candidates),
                     'observations': observations,
                     'fitness': {key: assess(value, FITNESS)
                                 for key, value in observations.items()}}
        evaluator = RecordedFeedbackEvaluator({'a': reference}, {'a': workload['domain']},
                                               {'a': FITNESS})
        self.assertEqual(len(evaluator.observations['a']), len(RUNTIME_FAILURE_STATUSES))
        broken = copy.deepcopy(reference)
        key = next(iter(broken['observations']))
        broken['observations'][key]['status'] = 'ARBITRARY_FAILURE'
        broken['fitness'][key] = assess(broken['observations'][key], FITNESS)
        with self.assertRaises(ValueError):
            RecordedFeedbackEvaluator({'a': broken}, {'a': workload['domain']}, {'a': FITNESS})


class ConfigurationTest(unittest.TestCase):
    def test_strict_config_resolves_map_paths_and_policy_defaults(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            value = {'schemaVersion': 'contextual-workload-allocation.v1', 'seed': 11,
                     'budget': 20, 'policy': 'contextual-linucb', 'fitness': FITNESS,
                     'workloads': [{'name': 'one', 'config': 'map/config.json',
                                    'catalogue': 'map/catalogue',
                                    'reference': 'map/reference.json'}]}
            path = root / 'allocation.json'
            path.write_text(json.dumps(value))
            parsed = parse_configuration(path)
            self.assertEqual(parsed['policyParameters'], {'exploration': 1.0, 'ridge': 1.0})
            self.assertEqual(parsed['workloads'][0]['config'],
                             str((root / 'map/config.json').resolve()))
            value['unknown'] = True
            path.write_text(json.dumps(value))
            with self.assertRaises(ValueError):
                parse_configuration(path)

    def test_factorial_policies_use_model_specific_defaults(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / 'allocation.json'
            for policy in FACTORIAL_POLICIES:
                with self.subTest(policy=policy):
                    path.write_text(json.dumps({
                        'schemaVersion': 'contextual-workload-allocation.v1', 'seed': 11,
                        'budget': 20, 'policy': policy, 'fitness': FITNESS,
                        'workloads': [{'config': 'map/config.json',
                                       'catalogue': 'map/catalogue',
                                       'reference': 'map/reference.json'}]}))
                    parsed = parse_configuration(path)
                    self.assertEqual(parsed['policy'], policy)
                    self.assertEqual(parsed['policyParameters'], PARAMETERS[policy])

    def test_legacy_policy_list_remains_stable(self):
        self.assertEqual(POLICIES, ('round-robin', 'uniform', 'adaptive-ucb',
                                   'contextual-linucb', 'contextual-linucb-cooldown'))

    def test_rejects_legacy_fitness_and_wrong_policy_parameters(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / 'allocation.json'
            base = {'schemaVersion': 'contextual-workload-allocation.v1', 'seed': 1,
                    'budget': 1, 'policy': 'round-robin',
                    'workloads': [{'config': 'c', 'catalogue': 'd', 'reference': 'r'}]}
            path.write_text(json.dumps({**base,
                'fitness': {'policy': 'complete-impact-v2-object-count'}}))
            with self.assertRaises(ValueError):
                parse_configuration(path)
            path.write_text(json.dumps({**base, 'fitness': FITNESS,
                                        'policyParameters': {'ridge': 1}}))
            with self.assertRaises(ValueError):
                parse_configuration(path)


if __name__ == '__main__':
    unittest.main()
