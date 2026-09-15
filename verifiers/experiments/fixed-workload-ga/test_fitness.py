import math
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from fitness import (CRITERIA, CRITERIA_V2, PERSISTENT, READ, LOST_COPIED_UPDATE,
                     LEGACY, WEIGHTED, WEIGHTED_V2, assess, configuration)
from search import run
from test_search import FakeDomain
import run as cli
from runtime import save, read


def weights(*values):
    return {'policy': WEIGHTED, 'weights': dict(zip(CRITERIA, values))}


def weights_v2(*values):
    return {'policy': WEIGHTED_V2, 'weights': dict(zip(CRITERIA_V2, values))}


def evidence(counts=(1, 1, 0), a=1, i=1):
    return {'I': i, 'A': a, 'status': 'COMPLETE', 'terminalStatus': 'COMPENSATED',
            'scheduleConformance': 'EXACT', 'AStatus': 'COMPLETE',
            'ACoverage': 'COMPLETE_WITHIN_SCOPE', 'AGaps': [],
            'impactCategories': [{'category': name, 'positiveObjectCount': n,
                                  'coverageStatus': 'COMPLETE', 'unknownReasons': []}
                                 for name, n in zip(PERSISTENT, counts)]}


def lost_evidence(value, *, validity='COMPLETE', coverage='COMPLETE_WITHIN_SCOPE', gaps=()):
    return {'lostCopiedUpdateCount': value, 'lostCopiedUpdateValidity': validity,
            'lostCopiedUpdateCoverage': coverage, 'lostCopiedUpdateCoverageGaps': list(gaps)}


class FitnessTest(unittest.TestCase):
    def test_overlap_and_fractional_preferences_preserve_original_i(self):
        original = evidence()
        result = assess(original, weights(1, 1, 1, 0.25))
        self.assertEqual(result['fitnessScore'], 2.25)
        self.assertEqual(original['I'], 1)
        self.assertEqual(assess(original, configuration())['fitnessScore'], 1)

    def test_configuration_rejects_typos_missing_negative_nonfinite_and_zero(self):
        cases = [weights(0, 0, 0, 0), weights(-1, 1, 1, 1), weights(True, 1, 1, 1),
                 weights(math.nan, 1, 1, 1), weights(math.inf, 1, 1, 1),
                 weights(10**1000, 1, 1, 1), weights(1, 1, 1), {},
                 {'policy': LEGACY, 'weights': {}}, {'policy': 'typo'},
                 {'policy': WEIGHTED, 'weights': {**weights(1, 1, 1, 1)['weights'], 'typo': 1}}]
        for value in cases:
            with self.subTest(value=value), self.assertRaises(ValueError):
                configuration(value)
        c = weights(1, 0, 0, 0.5)
        frozen = configuration(c)
        c['weights'][READ] = 10
        self.assertEqual(frozen['weights'][READ], 0.5)

    def test_v1_configuration_and_components_remain_exactly_four_criteria(self):
        original = evidence()
        original.update(lost_evidence(9))
        configured = configuration(weights(1, 1, 1, 1))
        self.assertEqual(configured, weights(1, 1, 1, 1))
        assessed = assess(original, configured)
        self.assertEqual(list(assessed['fitnessComponents']), list(CRITERIA))
        self.assertEqual(assessed['fitnessScore'], 3)
        with self.assertRaises(ValueError):
            configuration({'policy': WEIGHTED, 'weights': dict.fromkeys(CRITERIA_V2, 1)})

    def test_v2_requires_fifth_weight_and_prefers_lost_copied_updates(self):
        low = {**evidence(counts=(0, 0, 0), a=0, i=0), **lost_evidence(1)}
        high = {**evidence(counts=(0, 0, 0), a=0, i=0), **lost_evidence(3)}
        policy = weights_v2(0, 0, 0, 0, 2)
        self.assertEqual(assess(low, policy)['fitnessScore'], 2)
        self.assertEqual(assess(high, policy)['fitnessScore'], 6)
        self.assertEqual(assess(high, policy)['fitnessComponents'][LOST_COPIED_UPDATE]['observedCount'], 3)
        with self.assertRaises(ValueError):
            configuration({'policy': WEIGHTED_V2, 'weights': dict.fromkeys(CRITERIA, 1)})

    def test_v2_incomplete_enabled_is_null_but_raw_count_stays_visible(self):
        result = {**evidence(), **lost_evidence(2, validity='COMPLETE',
                  coverage='INCOMPLETE', gaps=('MISSING_ORIGIN',))}
        assessed = assess(result, weights_v2(0, 0, 0, 0, 1))
        self.assertIsNone(assessed['fitnessScore'])
        self.assertEqual(assessed['fitnessComponents'][LOST_COPIED_UPDATE]['observedCount'], 2)
        self.assertEqual(assessed['fitnessUnavailableReasons'], ['LOST_COPIED_UPDATE_INCOMPLETE'])
        self.assertEqual(assess(result, weights_v2(1, 0, 0, 0, 0))['fitnessScore'], 1)

    def test_old_report_cannot_be_rescored_with_enabled_fifth_criterion(self):
        self.assertIsNone(assess(evidence(), weights_v2(0, 0, 0, 0, 1))['fitnessScore'])
        self.assertEqual(assess(evidence(), weights_v2(1, 0, 0, 0, 0))['fitnessScore'], 1)

    def test_partial_read_lower_bound_is_not_a_complete_score(self):
        r = {**evidence(), 'ACoverage': 'PARTIAL', 'AGaps': ['gap']}
        self.assertIsNone(assess(r, weights(1, 1, 1, 1))['fitnessScore'])
        self.assertEqual(assess(r, weights(1, 1, 1, 0))['fitnessScore'], 2)
        self.assertEqual(assess(r, weights(0, 0, 0, 1))['fitnessComponents'][READ]['observedCount'], 1)
        for field, value in [('A', None), ('AStatus', 'INCOMPLETE'), ('AGaps', ['gap'])]:
            with self.subTest(field=field):
                self.assertIsNone(assess({**evidence(), field: value}, weights(0, 0, 0, 1))['fitnessScore'])

    def test_only_enabled_categories_need_complete_coverage(self):
        r = evidence(i=None)
        r['status'] = 'PARTIAL'
        r['impactCategories'][1].update(coverageStatus='PARTIAL', unknownReasons=['competing writer'])
        self.assertEqual(assess(r, weights(1, 0, 0, 1))['fitnessScore'], 2)
        self.assertIsNone(assess(r, weights(1, 1, 0, 1))['fitnessScore'])
        self.assertIsNone(assess(r, configuration())['fitnessScore'])
        r['impactCategories'] = []
        self.assertEqual(assess(r, weights(0, 0, 0, 1))['fitnessScore'], 1)
        self.assertIsNone(assess(r, weights(1, 0, 0, 1))['fitnessScore'])

    def test_invalid_attempt_cannot_score_from_positive_sidecars(self):
        for field, value in [('status', 'INVALID_REPORT'), ('terminalStatus', 'UNEXPECTED_EXECUTION_FAILURE'),
                             ('scheduleConformance', 'INCOMPLETE')]:
            with self.subTest(field=field):
                r = assess({**evidence(), field: value}, weights(0, 0, 0, 1))
                self.assertIsNone(r['fitnessScore'])

    def test_unavailable_disabled_state_checks_do_not_block_complete_reads(self):
        r = {**evidence(i=None), 'status': 'UNAVAILABLE'}
        for category in r['impactCategories']:
            category.update(coverageStatus='UNAVAILABLE', positiveObjectCount=0)
        self.assertEqual(assess(r, weights(0, 0, 0, 1))['fitnessScore'], 1)
        self.assertIsNone(assess(r, weights(1, 0, 0, 1))['fitnessScore'])
        self.assertIsNone(assess(r, configuration())['fitnessScore'])

    def test_overflow_is_unavailable_not_infinity(self):
        r = assess(evidence(counts=(2, 2, 0)), configuration(weights(1e308, 1e308, 0, 0)))
        self.assertIsNone(r['fitnessScore'])
        self.assertEqual(r['fitnessUnavailableReasons'], ['WEIGHTED_SCORE_OVERFLOW'])

    def test_weights_change_ga_choices_not_random_choices(self):
        def measure(c, n):
            a = int(c['faultVector'], 2)
            return evidence(counts=(15 - a, 0, 0), a=a, i=15-a)
        def search(strategy, policy):
            return run(FakeDomain(), measure, strategy=strategy, seed=29, budget=24,
                       population=4, fitness=policy)
        objects, reads = weights(1, 0, 0, 0), weights(0, 0, 0, 0.5)
        a, b = search('ga', objects), search('ga', reads)
        self.assertNotEqual([x['key'] for x in a['attempts']], [x['key'] for x in b['attempts']])
        self.assertEqual([x['key'] for x in search('random', objects)['attempts']],
                         [x['key'] for x in search('random', reads)['attempts']])
        prior = {}
        attempts = {x['key']: x for x in b['attempts']}
        for p in b['proposals']:
            for parent in p['parents']:
                self.assertEqual(parent['fitnessScore'], prior[parent['key']])
            if p['status'] == 'EVALUATED':
                prior[p['key']] = attempts[p['key']]['fitnessScore']
        self.assertEqual(b['bestScore'], max(x['A'] * 0.5 for x in b['attempts']))
        self.assertEqual(b['bestI'], max(x['I'] for x in b['attempts']))

    def test_fifth_weight_changes_ga_choices_not_random_sampling(self):
        def measure(c, n):
            value = int(c['faultVector'], 2)
            return {**evidence(counts=(15 - value, 0, 0), a=0, i=15 - value),
                    **lost_evidence(value)}
        persistent = weights_v2(1, 0, 0, 0, 0)
        lost = weights_v2(0, 0, 0, 0, 1)
        def attempts(strategy, policy):
            result = run(FakeDomain(), measure, strategy=strategy, seed=29, budget=24,
                         population=4, fitness=policy)
            return [row['key'] for row in result['attempts']]
        self.assertNotEqual(attempts('ga', persistent), attempts('ga', lost))
        self.assertEqual(attempts('random', persistent), attempts('random', lost))

    def test_unavailable_weighted_scores_consume_budget_never_parent(self):
        result = run(FakeDomain(), lambda c, n: {**evidence(), 'ACoverage': 'PARTIAL'},
                     strategy='ga', seed=29, budget=12, population=4, fitness=weights(0, 0, 0, 1))
        self.assertEqual(result['nullFitnessAttempts'], 12)
        self.assertTrue(all(not p['parents'] for p in result['proposals']))
        self.assertIsNone(result['bestScore'])
        self.assertEqual(result['bestI'], 1)

    def test_rescore_does_not_run_application_or_rewrite_measurements(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            saved = {'attempts': [{'directory': str(root), 'attempt': 1, 'key': 'k', 'scenarioId': 's'}]}
            save(root / 'results.json', saved)
            save(root / 'attempt.json', {})
            measured = {**evidence(), 'candidate': {'id': 's'}}
            with patch.object(cli, 'retained_attempt', return_value=measured), \
                    patch.object(cli.Runtime, 'evaluate', side_effect=AssertionError('Must not execute')):
                cli.rescore(root / 'results.json', weights(1, 1, 1, 1), root / 'new')
            self.assertEqual(read(root / 'results.json'), saved)
            result = read(root / 'new/rescore.json')
            self.assertEqual(result['newApplicationExecutions'], 0)
            self.assertEqual(result['attempts'][0]['fitnessScore'], 3)

    def test_rescore_v2_retains_sidecar_derived_count_and_attempt_hash(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            saved = {'attempts': [{'directory': str(root), 'attempt': 1, 'key': 'k', 'scenarioId': 's'}]}
            save(root / 'results.json', saved)
            save(root / 'attempt.json', {'attested': True})
            measured = {**evidence(), **lost_evidence(2), 'candidate': {'id': 's'},
                        'reportHashes': {'execution-report-lost-copied-updates.json': 'sidecar-hash'}}
            with patch.object(cli, 'retained_attempt', return_value=measured):
                cli.rescore(root / 'results.json', weights_v2(0, 0, 0, 0, 1), root / 'new')
            row = read(root / 'new/rescore.json')['attempts'][0]
            self.assertEqual(row['lostCopiedUpdateCount'], 2)
            self.assertEqual(row['fitnessScore'], 2)
            self.assertEqual(row['sourceSha256'], cli.digest(root / 'attempt.json'))
            self.assertEqual(row['lostCopiedUpdateReportSha256'], 'sidecar-hash')


if __name__ == '__main__':
    unittest.main()
