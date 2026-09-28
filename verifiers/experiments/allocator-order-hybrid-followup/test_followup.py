import copy
import sys
import unittest
from pathlib import Path
from unittest.mock import patch


HERE = Path(__file__).resolve().parent
sys.path[:0] = [str(HERE), str(HERE.parent / 'fixed-workload-ga'),
                str(HERE.parent / 'allocator-transfer')]

import study
from allocator import PROGRESS_FEATURES, ProgressFeatureSpace
from fitness import assess, configuration
from integration import StructuralOrderFeatureSpace, _previous_study, policy
from order_features import ORDER_FEATURES
from test_allocator import complete_result
from test_transfer import Fixture
from transfer import _fitness, _new_states


DEPENDENCY_AND_READ = configuration({
    'policy': 'weighted-criteria-v2',
    'weights': {
        'DELETED_DEPENDENCY': 1,
        'FAILED_OPERATION_RESIDUAL': 0,
        'UNRESOLVED_DELIVERED_EVENT': 0,
        'COMPENSATED_READ_EXPOSURE': 1,
        'LOST_COPIED_UPDATE': 0,
    },
})


def factory(fixture):
    def create():
        evaluator = fixture.factory()
        evaluator.revealed = evaluator.seen
        return evaluator
    return create


def add_order_profiles(workloads):
    for index, row in enumerate(workloads):
        profile = copy.deepcopy(row['profile'])
        profile['orderFeatures'] = {name: 0.0 for name in ORDER_FEATURES}
        profile['orderFeatures'][ORDER_FEATURES[index % len(ORDER_FEATURES)]] = 0.5
        row['orderProfile'] = profile


class FollowupTest(unittest.TestCase):
    def test_combined_hybrid_partition_has_order_only_in_shared_context(self):
        fixture = Fixture()
        add_order_profiles(fixture.workloads)
        states = _new_states(fixture.workloads, 3, 8, _fitness())
        combined = policy('hybrid-order', states, 3)
        self.assertEqual(combined.local_feature_space.names, ProgressFeatureSpace().names)
        self.assertTrue(set(ORDER_FEATURES) <= set(combined.shared_feature_space.names))
        self.assertFalse(set(PROGRESS_FEATURES) & set(combined.shared_feature_space.names))
        context = combined.context('train-a', states['train-a'])
        self.assertEqual(len(context['shared']), len(combined.shared_feature_space.names))
        self.assertEqual(len(context['local']), len(ProgressFeatureSpace().names))
        self.assertEqual(context['local'], ProgressFeatureSpace().vector(
            states['train-a']['profile'], states['train-a']['progress']))

    def test_structural_order_space_preserves_original_structure_coordinates(self):
        fixture = Fixture()
        add_order_profiles(fixture.workloads)
        profiles = {row['id']: row['orderProfile'] for row in fixture.workloads}
        combined = StructuralOrderFeatureSpace(profiles)
        self.assertEqual(combined.names[-len(ORDER_FEATURES):], list(ORDER_FEATURES))
        self.assertEqual(len(combined.names), len(set(combined.names)))

    def test_explicit_all_five_fitness_preserves_preceding_trajectory(self):
        expected_fixture = Fixture()
        actual_fixture = Fixture()
        expected = _previous_study().run_arm(
            expected_fixture.workloads, factory(expected_fixture), 'structural',
            7, 24, [8, 16, 24])
        actual = study.run_arm(
            actual_fixture.workloads, factory(actual_fixture), 'structural',
            7, 24, [8, 16, 24], _fitness(), policy)
        self.assertEqual(actual['decisions'], expected['decisions'])
        self.assertEqual(actual['perWorkload'], expected['perWorkload'])

    def test_dependency_and_read_profile_scores_only_its_enabled_components(self):
        result = complete_result({'key': 'candidate'}, score=7)
        result['impactCategories'][0]['positiveObjectCount'] = 2
        result['impactCategories'][1].update(
            coverageStatus='PARTIAL', unknownReasons=[{'reason': 'disabled'}])
        result['A'] = 3
        result['lostCopiedUpdateCoverage'] = 'INCOMPLETE'
        assessed = assess(result, DEPENDENCY_AND_READ)
        self.assertEqual(assessed['fitnessScore'], 5)
        self.assertEqual(assessed['fitnessUnavailableReasons'], [])
        result['ACoverage'] = 'PARTIAL'
        result['AGaps'] = ['enabled read evidence incomplete']
        assessed = assess(result, DEPENDENCY_AND_READ)
        self.assertIsNone(assessed['fitnessScore'])
        self.assertIn('COMPENSATED_READ_EXPOSURE_INCOMPLETE',
                      assessed['fitnessUnavailableReasons'])

    def test_selected_profile_is_passed_to_every_search_session(self):
        fixture = Fixture()
        original = study._new_states
        captured = []

        def inspect(workloads, seed, budget, fitness):
            states = original(workloads, seed, budget, fitness)
            captured.extend(state['session'].fitness for state in states.values())
            return states

        with patch.object(study, '_new_states', side_effect=inspect):
            result = study.run_arm(fixture.workloads, factory(fixture), 'round-robin',
                                   5, 8, [8], DEPENDENCY_AND_READ, policy)
        self.assertEqual(result['fitness'], DEPENDENCY_AND_READ)
        self.assertTrue(captured)
        self.assertTrue(all(value == DEPENDENCY_AND_READ for value in captured))


if __name__ == '__main__':
    unittest.main()
