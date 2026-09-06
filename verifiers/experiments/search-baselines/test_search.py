import copy
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

import search
from test_run import fixture

batch = search.batch


class SearchTests(unittest.TestCase):
    def test_orders_are_reproducible_without_replacement_and_ignore_outcomes(self):
        rows = [{'faultScenarioId': str(i), 'faultVector': str(i)} for i in range(10)]
        first = search.orders(rows, 4, [11, 29])
        changed = [{**r, 'score': 999 - i, 'expected': {'score': i}} for i, r in enumerate(reversed(rows))]
        self.assertEqual(first, search.orders(changed, 4, [11, 29]))
        self.assertNotEqual(first[0]['order'], first[1]['order'])
        for arm in first:
            self.assertEqual(10, len(set(arm['order'])))
            self.assertEqual(set(map(str, range(10))), set(arm['order']))
            self.assertEqual(4, len(arm['selected']))
        self.assertEqual(1, sum(a['strategy'] == 'deterministic' for a in first))

    def test_bad_budget_duplicates_and_duplicate_seeds_rejected(self):
        row = {'faultScenarioId': 'a', 'faultVector': '0'}
        for rows, budget, seeds in [([row], 2, [1]), ([row, row], 1, [1]), ([row], 1, [1, 1]), ([row], 0, [1])]:
            with self.assertRaises(ValueError):
                search.orders(rows, budget, seeds)

    def test_frozen_plan_rejects_changed_orders_budgets_and_mixed_workloads(self):
        rows = [{'faultScenarioId': str(i), 'faultVector': str(i), 'workloadId': 'w'} for i in range(4)]
        plan = {'schema': 'fixed-workload-search.v1', 'timeoutSeconds': 180, 'maxConcurrentStrategyRuns': 2,
                'acceptedConformance': ['EXACT', 'DEVIATED'], 'seeds': [11], 'plannedAttempts': 4,
                'scopes': [{'workloadId': 'w', 'candidateRows': rows, 'runs': search.orders(rows, 2, [11])}]}
        search.check_plan(plan)
        changed = copy.deepcopy(plan)
        changed['scopes'][0]['runs'][1]['selected'].reverse()
        with self.assertRaisesRegex(ValueError, 'order/budget'):
            search.check_plan(changed)
        changed = copy.deepcopy(plan)
        changed['scopes'][0]['runs'][1]['budget'] = 1
        with self.assertRaisesRegex(ValueError, 'order/budget'):
            search.check_plan(changed)
        changed = copy.deepcopy(plan)
        changed['scopes'][0]['candidateRows'][0]['workloadId'] = 'another-workload'
        with self.assertRaisesRegex(ValueError, 'Mixed workload'):
            search.check_plan(changed)

    def test_invalid_consumes_budget_and_zero_is_not_positive(self):
        attempts = [{'caseId': 'bad', 'status': 'TIMEOUT', 'score': None, 'wallSeconds': 5},
                    {'caseId': 'zero', 'status': 'COMPLETE', 'score': 0, 'wallSeconds': 2},
                    {'caseId': 'positive', 'status': 'COMPLETE', 'score': 2, 'wallSeconds': 3}]
        result = search.discovery(attempts)
        self.assertEqual(3, result['firstPositiveAttempt'])
        self.assertEqual(10, result['firstPositiveProcessWallSeconds'])
        self.assertEqual([0, 0, 1], [r['distinctPositiveScenarios'] for r in result['curve']])
        self.assertEqual(1, result['statuses']['TIMEOUT'])
        missing = search.discovery(attempts[:2])
        self.assertIsNone(missing['firstPositiveAttempt'])
        self.assertEqual('NOT_FOUND_WITHIN_BUDGET', missing['firstPositiveResult'])

    def test_partial_or_invalid_score_cannot_be_positive(self):
        for status in ['PARTIAL', 'INVALID_REPORT', 'EXECUTION_INVALID', 'UNAVAILABLE']:
            result = search.discovery([{'caseId': 'x', 'status': status, 'score': 5, 'wallSeconds': 1}])
            self.assertEqual([], result['distinctPositiveScenarioIds'])

    def evaluate(self, values):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            destination = root / 'c1/r01-case'
            code = ';'.join(f'open({str(destination / name)!r},"w").write({json.dumps(value)!r})'
                            for name, value in zip(batch.REPORTS, values))
            row = {'caseId': 'case', 'workloadId': 'workload', 'faultScenarioId': 'scenario', 'faultVector': '0'}
            selection = {'build': str(root / 'build'), 'manifest': str(root / 'manifest.json')}
            with patch.object(batch, 'TARGET', root), patch.object(batch, 'compose', return_value=[sys.executable, '-c', code]):
                result = batch.attempt(root, selection, row, 1, 1, 5, mode='assessment')
            self.assertEqual(result, batch.read(destination / 'attempt.json'))
            return result

    def test_unknown_outcome_needs_no_expectation_and_validates_positive(self):
        result = self.evaluate(fixture())
        self.assertEqual(('COMPLETE', 0), (result['status'], result['score']))
        values = fixture()
        category = values[2]['categoryResults'][0]
        identity = {'aggregateType': 'Thing', 'aggregateId': 1}
        category.update(candidateCount=1, candidates=[{'aggregate': identity}], positiveObjectCount=1,
                        findings=[{'category': category['category'], 'affectedObject': identity}])
        values[2].update(completeScore=1, observedAffectedObjectCount=1)
        result = self.evaluate(values)
        self.assertEqual(('COMPLETE', 1), (result['status'], result['score']))

    def test_incomplete_assessments_have_unavailable_search_score(self):
        values = fixture()
        values[2].update(assessmentStatus='PARTIAL', completeScore=None)
        result = self.evaluate(values)
        self.assertEqual(('PARTIAL', None), (result['status'], result['score']))
        values = fixture()
        values[0].update(terminalStatus='UNEXPECTED_EXECUTION_FAILURE', scheduleConformance='INCOMPLETE')
        values[2].update(executionTerminalStatus='UNEXPECTED_EXECUTION_FAILURE', scheduleConformance='INCOMPLETE',
                         assessmentStatus='INVALID', completeScore=None, observedAffectedObjectCount=None)
        result = self.evaluate(values)
        self.assertEqual(('EXECUTION_INVALID', None), (result['status'], result['score']))

    def test_bad_identity_still_fails_in_assessment_mode(self):
        values = fixture()
        values[2]['executionAttemptId'] = 'wrong'
        result = self.evaluate(values)
        self.assertEqual(('INVALID_REPORT', None), (result['status'], result['score']))

    def test_optional_source_setup_absence_is_valid_and_not_zero_duration(self):
        values = fixture()
        values[0].pop('sourceSetup')
        result = self.evaluate(values)
        self.assertEqual(('COMPLETE', 0), (result['status'], result['score']))
        self.assertIsNone(result['sourceSetupSeconds'])
        self.assertEqual(0, result['prerequisiteSetupSeconds'])
        self.assertNotIn('error', result)

    def test_malformed_metadata_after_score_validation_cannot_remain_complete(self):
        values = fixture()
        values[0]['sourceSetup']['durationNanos'] = 'malformed'
        result = self.evaluate(values)
        self.assertEqual(('INVALID_REPORT', None), (result['status'], result['score']))
        self.assertNotIn('observed', result)

    def test_snapshot_verification_ignores_checkout_drift_but_not_build_drift(self):
        selection = {'sourceHashes': {}, 'manifest': 'manifest', 'packageHashes': {}, 'build': 'build', 'buildHashes': {}}
        with patch.object(batch, 'source_hashes', return_value={'new': 'file'}), \
             patch.object(batch, 'package', return_value={'hashes': {}}), patch.object(batch, 'build_hashes', return_value={}):
            batch.verify(selection, check_checkout=False)
            with self.assertRaisesRegex(ValueError, 'Source drift'):
                batch.verify(selection)
        with patch.object(batch, 'package', return_value={'hashes': {}}), patch.object(batch, 'build_hashes', return_value={'changed': 'file'}):
            with self.assertRaisesRegex(ValueError, 'Prepared build drift'):
                batch.verify(selection, check_checkout=False)

    def test_arm_never_retries_or_skips_invalid_attempt(self):
        rows = [{'faultScenarioId': str(i), 'faultVector': str(i), 'caseId': str(i)} for i in range(3)]
        strategy = search.orders(rows, 2, [11])[0]
        scope = {'name': 'scope', 'candidateRows': rows}
        with tempfile.TemporaryDirectory() as temp:
            def fake(output, selection, row, rank, concurrency, timeout, mode):
                return {'caseId': row['caseId'], 'status': 'TIMEOUT', 'score': None, 'wallSeconds': 1}
            with patch.object(batch, 'attempt', side_effect=fake) as attempt:
                directory = search.run_arm(Path(temp), {'timeoutSeconds': 2}, scope, strategy)
            self.assertEqual(2, attempt.call_count)
            saved = batch.read(Path(directory) / 'results.json')
            self.assertEqual(strategy['selected'], [a['caseId'] for a in saved['attempts']])
            self.assertEqual({'TIMEOUT': 2}, saved['statuses'])
            self.assertTrue(saved['completed'])

    def test_fixed_workload_horizon_and_universe_cannot_be_changed(self):
        scope = {'workloadId': 'w', 'setupId': 's', 'schedule': [{'kind': 'event'}],
                 'candidateRows': [{'faultScenarioId': 'f', 'faultVector': '0', 'actions': []}]}
        data = {'records': {'workloads': [{'id': 'w', 'setup': 's', 'schedule': [{'kind': 'event'}]}],
                           'faultScenarios': [{'id': 'f', 'workload': 'w', 'faultVector': '0', 'actions': []}]}}
        experiment = {'manifest': 'manifest', 'scopes': [scope]}
        with patch.object(search, 'package', return_value=data):
            search.check_package(experiment)
            altered = copy.deepcopy(experiment)
            altered['scopes'][0]['schedule'] = []
            with self.assertRaisesRegex(ValueError, 'horizon'):
                search.check_package(altered)
            altered = copy.deepcopy(experiment)
            altered['scopes'][0]['candidateRows'] = []
            with self.assertRaisesRegex(ValueError, 'universe'):
                search.check_package(altered)


if __name__ == '__main__':
    unittest.main()
