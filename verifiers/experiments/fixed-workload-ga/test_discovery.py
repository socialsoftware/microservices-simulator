import unittest
from discovery_analysis import discovery_metrics


class DiscoveryTest(unittest.TestCase):
    def result(self, scores, stop='BUDGET'):
        return dict(strategy='ga', seed=11, population=2, attempts=[
            dict(key=str(n), I=score, operator='random' if n < 2 else 'crossover')
            for n, score in enumerate(scores)], stopReason=stop, duplicates=5,
            nullFitnessAttempts=sum(s is None for s in scores), wallSeconds=9, bestI=2)

    def test_targets_use_actual_execution_positions_and_round_up(self):
        reference = {str(i): {'I': score} for i, score in enumerate([0, 2, 2, 0, 2])}
        r = discovery_metrics(self.result([0, 2, 2, 0, 2]), reference, 5)
        self.assertEqual(r['curve'], [0, 0, 1, 2, 2, 3])
        self.assertEqual(r['firstPositiveExecution'], 2)
        self.assertEqual(r['targets']['50']['execution'], 3)
        self.assertEqual(r['targets']['80']['execution'], 5)
        self.assertEqual(r['targets']['100']['execution'], 5)

    def test_unavailable_feedback_does_not_reveal_known_positive(self):
        r = discovery_metrics(self.result([None, 0]), {'0': {'I': 2}, '1': {'I': 0}}, 2)
        self.assertEqual(r['positiveCount'], 0)
        self.assertIsNone(r['firstPositiveExecution'])
        self.assertEqual(r['targets']['100']['status'], 'NOT_REACHED')

    def test_stall_is_not_full_coverage_or_extra_executions(self):
        reference = {'0': {'I': 2}, '1': {'I': 2}, '2': {'I': 2}}
        r = discovery_metrics(self.result([2], 'PROPOSAL_STALL'), reference, 3)
        self.assertEqual(r['executions'], 1)
        self.assertEqual(r['curve'], [0, 1])
        self.assertEqual(r['displayCurveThroughBudget'], [0, 1, 1, 1])
        self.assertEqual(r['targets']['100']['status'], 'NOT_REACHED')

    def test_duplicate_or_changed_measurement_is_rejected(self):
        result = self.result([2, 2]); result['attempts'][1]['key'] = '0'
        with self.assertRaises(ValueError):
            discovery_metrics(result, {'0': {'I': 2}}, 2)
        with self.assertRaises(ValueError):
            discovery_metrics(self.result([0]), {'0': {'I': 2}}, 1)

    def test_zero_positive_reference_is_explicit(self):
        r = discovery_metrics(self.result([0]), {'0': {'I': 0}}, 1)
        self.assertIsNone(r['meanPositiveFractionAcrossBudget'])
        self.assertEqual(r['targets']['100']['status'], 'NO_REFERENCE_POSITIVES')


if __name__ == '__main__':
    unittest.main()
