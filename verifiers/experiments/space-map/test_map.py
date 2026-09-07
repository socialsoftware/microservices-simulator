import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('space_map', Path(__file__).with_name('map.py'))
mapping = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mapping)


class SamplingTests(unittest.TestCase):
    def test_vectors_respect_participant_ownership(self):
        workload = {'participants': [{'id': 'p1'}, {'id': 'p2'}], 'schedule': [
            {'kind': 'step', 'participant': 'p2', 'faultSlot': 0},
            {'kind': 'event'},
            {'kind': 'step', 'participant': 'p1', 'faultSlot': 1},
            {'kind': 'step', 'participant': 'p2', 'faultSlot': 2}]}
        self.assertEqual(mapping.vectors(workload), ['000', '001', '010', '011', '100', '110'])

    def test_vector_sample_preserves_controls_without_duplicates(self):
        values = [format(i, '06b') for i in range(64)]
        chosen = mapping.sample_vectors(values, 11, 12)
        self.assertEqual(len(chosen), 12)
        self.assertEqual(len(set(chosen)), 12)
        self.assertTrue({v for v in values if v.count('1') <= 1} <= set(chosen))
        self.assertEqual(chosen, mapping.sample_vectors(list(reversed(values)), 11, 12))
        self.assertNotEqual(chosen, mapping.sample_vectors(values, 29, 12))

    def test_small_universe_is_enumerated(self):
        rows = [{'id': 'b', 'faultVector': '1'}, {'id': 'a', 'faultVector': '0'}]
        self.assertEqual(mapping.sample_scenarios(rows, ['0', '1'], 11), list(reversed(rows)))

    def test_large_sample_covers_each_vector_and_not_only_prefix(self):
        rows = [{'id': f'{v}-{i:03}', 'faultVector': v} for v in ['00', '01', '10'] for i in range(100)]
        result = mapping.sample_scenarios(rows, ['00', '01', '10'], 11, 9)
        self.assertEqual(len(result), 9)
        self.assertEqual({r['faultVector'] for r in result}, {'00', '01', '10'})
        self.assertEqual(len({r['id'] for r in result}), 9)
        self.assertTrue(any(int(r['id'].split('-')[1]) > 50 for r in result))
        self.assertEqual(result, mapping.sample_scenarios(rows[::-1], ['10', '01', '00'], 11, 9))

    def test_missing_vector_and_duplicate_fail(self):
        row = {'id': 'a', 'faultVector': '0'}
        with self.assertRaises(ValueError): mapping.sample_scenarios([row], ['0', '1'], 11)
        with self.assertRaises(ValueError): mapping.sample_scenarios([row, row], ['0'], 11)

    def test_representatives_retain_zero_and_failure(self):
        rows = [{'caseId': str(i), 'caseGroup': 'w01', 'faultScenarioId': str(i)} for i in range(4)]
        results = [{'caseId': '0', 'status': 'COMPLETE', 'score': 0},
                   {'caseId': '1', 'status': 'COMPLETE', 'score': 0},
                   {'caseId': '2', 'status': 'COMPLETE', 'score': 2},
                   {'caseId': '3', 'status': 'TIMEOUT', 'score': None}]
        self.assertEqual({r['caseId'] for r in mapping.representatives(rows, results)}, {'0', '2', '3'})


if __name__ == '__main__': unittest.main()
