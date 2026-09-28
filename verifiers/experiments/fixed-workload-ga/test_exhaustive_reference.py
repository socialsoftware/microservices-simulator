import unittest
from pathlib import Path
from unittest.mock import patch

from exhaustive_reference import RecordedDomain, enumerated_domain
from search import candidate_key, run


class ExhaustiveReferenceTests(unittest.TestCase):
    def setUp(self):
        self.workload = {'id': 'w', 'participants': [{'id': 'p1'}],
                         'schedule': [{'participant': 'p1', 'faultSlot': 0}]}
        self.scenarios = [{'id': 's' + vector, 'workload': 'w', 'faultVector': vector,
                           'actions': [{'step': vector}]} for vector in ['0', '1']]
        self.inventory = {'countComplete': True, 'anyTruncated': False,
                          'capLimitedUniqueCandidateCount': 2}
        self.requests = [{'vector': s['faultVector'], 'truncated': False, 'process': {'status': 'EXITED'},
                          'response': {'status': 'PERSISTED', 'workloadPlanId': 'w',
                                       'faultScenarioIds': [s['id']], 'uncappedScheduleCount': '1',
                                       'writtenScheduleCount': 1}} for s in self.scenarios]

    def load(self):
        data = {'hashes': {}, 'records': {'workloads': [self.workload], 'faultScenarios': self.scenarios}}
        with patch('catalogue.package', return_value=data), patch(
                'catalogue.read', side_effect=lambda p: self.inventory if p.name == 'domain-count.json' else self.requests):
            return enumerated_domain(Path('/unused'), 'w')[0]

    def test_complete_structure_exposes_feedback_only_when_selected(self):
        domain = self.load()
        calls = []
        def evaluate(candidate, number):
            calls.append(candidate['key'])
            return {'I': int(candidate['faultVector']), 'status': 'COMPLETE'}
        result = run(domain, evaluate, strategy='ga', seed=11, budget=2)
        self.assertEqual(calls, [a['key'] for a in result['attempts']])
        self.assertEqual(set(calls), set(domain.candidates))
        self.assertTrue(domain.exhausted(set(calls)))
        self.assertFalse(domain.exhausted(set(calls[:1])))
        self.assertTrue(all('I' not in c and 'fitnessScore' not in c for c in domain.candidates.values()))

    def test_partial_or_truncated_enumerations_are_rejected(self):
        for field, value in [('countComplete', False), ('anyTruncated', True)]:
            with self.subTest(field=field):
                original = self.inventory[field]; self.inventory[field] = value
                with self.assertRaises(ValueError): self.load()
                self.inventory[field] = original

    def test_missing_vector_and_unrelated_candidate_are_rejected(self):
        saved = self.requests.pop()
        with self.assertRaisesRegex(ValueError, 'Missing canonical'): self.load()
        self.requests.append(saved)
        self.scenarios[0]['workload'] = 'other'
        with self.assertRaisesRegex(ValueError, 'identity mismatch'): self.load()

    def test_structure_is_grouped_by_vector_with_stable_recovery_order(self):
        cs = {}
        for action in ['z', 'a']:
            c = {'workload': 'w', 'faultVector': '1', 'actions': [action]}
            c['key'] = candidate_key('w', '1', c['actions']); cs[c['key']] = c
        domain = RecordedDomain(self.workload, cs)
        self.assertEqual([['a'], ['z']], [c['actions'] for c in domain.resolve('1')])
        self.assertEqual([], domain.resolve('0'))

    def test_uniform_fallback_exhausts_catalogue_despite_duplicate_children(self):
        candidates = {}
        for i in range(20):
            c = {'id': str(i), 'workload': 'w', 'faultVector': '1', 'actions': [{'recovery': i}]}
            c['key'] = candidate_key('w', '1', c['actions'])
            candidates[c['key']] = c
        domain = RecordedDomain(self.workload, candidates)
        def assess(c, n):
            return {'I': None if int(c['id']) % 3 == 0 else 1}
        r = run(domain, assess, strategy='ga', seed=11, budget=25, population=2,
                mutation=0, stall_limit=2, exploration='uniform-unseen')
        self.assertEqual('EXHAUSTED', r['stopReason'])
        self.assertEqual(set(candidates), {a['key'] for a in r['attempts']})
        self.assertEqual(20, len(r['attempts']))
        self.assertGreater(r['duplicates'], 0)
        self.assertEqual(7, r['nullFitnessAttempts'])
        null_keys = {a['key'] for a in r['attempts'] if a['fitnessScore'] is None}
        self.assertFalse(any(p['key'] in null_keys for a in r['attempts'] for p in a['parents']))
        reordered = RecordedDomain(self.workload, dict(reversed(list(candidates.items()))))
        again = run(reordered, assess, strategy='ga', seed=11, budget=25, population=2,
                    mutation=0, stall_limit=2, exploration='uniform-unseen')
        self.assertEqual([a['key'] for a in r['attempts']], [a['key'] for a in again['attempts']])

    def test_uniform_mode_requires_catalogue_and_fallback_opportunity(self):
        with self.assertRaises(ValueError):
            run(object(), lambda c,n: {}, strategy='ga', seed=1, budget=1,
                exploration='uniform-unseen')
        with self.assertRaises(ValueError):
            run(self.load(), lambda c,n: {}, strategy='ga', seed=1, budget=1,
                exploration='uniform-unseen', stall_limit=1)


if __name__ == '__main__':
    unittest.main()
