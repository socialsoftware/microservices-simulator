import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('space_analysis', Path(__file__).with_name('analyze.py'))
analysis = importlib.util.module_from_spec(spec)
spec.loader.exec_module(analysis)


def binding(inp, action, value='2'):
    return {'inputVariantId': inp, 'sourceActionId': action, 'propertyName': 'aggregateId',
            'status': 'RESOLVED', 'resolvedValue': value}


def report(*bindings):
    return {'sourceSetup': {'participantBindings': list(bindings)}}


class RuntimeSharingTests(unittest.TestCase):
    def test_distinct_ids_with_same_vector_and_actions_share_model(self):
        first = {'id': 'eager', 'faultVector': '00', 'actions': [{'step': 's1'}, {'step': 's2'}]}
        second = {**first, 'id': 'requested'}
        self.assertEqual(analysis.action_model_key('workload', first), analysis.action_model_key('workload', second))

    def test_model_keeps_workload_vector_and_action_order(self):
        first = {'faultVector': '00', 'actions': [{'step': 's1'}, {'step': 's2'}]}
        key = analysis.action_model_key('workload', first)
        self.assertNotEqual(key, analysis.action_model_key('other', first))
        self.assertNotEqual(key, analysis.action_model_key('workload', {**first, 'faultVector': '01'}))
        self.assertNotEqual(key, analysis.action_model_key('workload', {**first, 'actions': first['actions'][::-1]}))

    def test_three_participants_connected_by_two_objects(self):
        shared, connected = analysis.shared_bindings(report(
            binding('p1', 'execution'), binding('p2', 'execution'),
            binding('p2', 'user', '3'), binding('p3', 'user', '3')), {'p1', 'p2', 'p3'})
        self.assertTrue(connected)
        self.assertEqual(len(shared), 2)

    def test_missing_participant_does_not_prove_connected_triple(self):
        _, connected = analysis.shared_bindings(report(binding('p1', 'execution'), binding('p2', 'execution')),
                                                {'p1', 'p2', 'p3'})
        self.assertFalse(connected)

    def test_equal_integer_from_distinct_results_is_not_sharing(self):
        shared, connected = analysis.shared_bindings(report(binding('p1', 'execution'), binding('p2', 'user')),
                                                     {'p1', 'p2'})
        self.assertFalse(connected)
        self.assertEqual(shared, [])

    def test_conflicting_resolution_is_rejected(self):
        with self.assertRaises(ValueError):
            analysis.shared_bindings(report(binding('p1', 'execution'), binding('p2', 'execution', '3')), {'p1', 'p2'})


if __name__ == '__main__': unittest.main()
