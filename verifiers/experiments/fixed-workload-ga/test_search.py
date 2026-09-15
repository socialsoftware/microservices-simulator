import copy
import json
from pathlib import Path
import tempfile
import unittest

from search import candidate_key, fault_coordinates, run, stable
from runtime import (Domain, IntegrityError, digest, package, save, validate_read,
                     validate_lost_copied_updates)


class FakeDomain:
    coordinates = [[None, 0, 1], [None, 2, 3]]
    width = 4

    def resolve(self, vector):
        return [dict(id=vector + '-' + str(i), workload='fixture', faultVector=vector,
                     actions=[{'recovery': i}], key=candidate_key('fixture', vector, [{'recovery': i}]))
                for i in range(1 + vector.count('1') * 3)]

    def exhausted(self, seen):
        return False


def evaluate(c, n):
    return {'I': int(c['faultVector'], 2), 'A': 0, 'status': 'COMPLETE'}


class SearchTest(unittest.TestCase):
    def search(self, strategy='ga', fn=evaluate, **kwargs):
        return run(FakeDomain(), fn, strategy=strategy, seed=29, budget=24, population=4, **kwargs)

    def test_seed_reproduces_choices(self):
        a, b = self.search(), self.search()
        self.assertEqual(a['proposals'], b['proposals'])
        self.assertEqual([x['key'] for x in a['attempts']], [x['key'] for x in b['attempts']])

    def test_fitness_changes_ga_but_not_random(self):
        reverse = lambda c, n: {'I': 15 - int(c['faultVector'], 2)}
        self.assertNotEqual(self.search()['proposals'], self.search(fn=reverse)['proposals'])
        self.assertEqual(self.search('random')['proposals'], self.search('random', reverse)['proposals'])

    def test_parent_scores_and_legal_genes(self):
        result = self.search(mutation=1)
        prior = {}
        attempts = {a['key']: a for a in result['attempts']}
        crossed = replacements = 0
        for p in result['proposals']:
            for gene, options in zip(p['genes'], FakeDomain.coordinates):
                self.assertIn(gene, options)
            for parent in p['parents']:
                self.assertEqual(parent['I'], prior[parent['key']])
            crossed += p['operator'] == 'crossover'
            replacements += p.get('replacedRecovery', False)
            if p['status'] == 'EVALUATED':
                prior[p['key']] = attempts[p['key']]['I']
        self.assertGreater(crossed, 0)
        self.assertTrue(any(p.get('mutation') is not None for p in result['proposals']))
        self.assertEqual(len(attempts), 24)

    def test_null_fitness_consumes_budget_never_parents(self):
        result = self.search(fn=lambda c, n: {'I': None, 'status': 'TIMEOUT'})
        self.assertEqual(result['nullFitnessAttempts'], 24)
        self.assertEqual(result['bestI'], None)
        self.assertEqual(result['stopReason'], 'BUDGET')
        self.assertTrue(all(not p['parents'] for p in result['proposals']))

    def test_equal_scores_do_not_stall_population(self):
        result = self.search(fn=lambda c, n: {'I': 0})
        self.assertEqual(len(result['attempts']), 24)
        self.assertTrue(any(p['parents'] for p in result['proposals']))
        self.assertEqual(result['positiveScenarios'], 0)

    def test_stall_not_exhaustion_and_no_retries(self):
        domain = FakeDomain()
        candidate = domain.resolve('0000')[0]
        domain.resolve = lambda v: [candidate]
        r = run(domain, lambda c, n: {'I': None}, strategy='ga', seed=1, budget=8, stall_limit=7)
        self.assertEqual(len(r['attempts']), 1)
        self.assertEqual(r['duplicates'], 7)
        self.assertEqual(r['stopReason'], 'PROPOSAL_STALL')
        domain.resolve = lambda v: []
        r = run(domain, evaluate, strategy='random', seed=1, budget=8, stall_limit=3)
        self.assertEqual(len(r['attempts']), 0)
        self.assertEqual(len(r['proposals']), 3)

    def test_small_budget_and_bad_config(self):
        r = run(FakeDomain(), evaluate, strategy='ga', seed=1, budget=1)
        self.assertEqual(r['population'], 1)
        with self.assertRaises(ValueError):
            run(FakeDomain(), evaluate, strategy='ga', seed=1, budget=0)

    def test_key_ignores_alias_not_action_order(self):
        self.assertNotEqual(candidate_key('w', '10', [{'step': 's1'}, {'step': 's2'}]),
                            candidate_key('w', '10', [{'step': 's2'}, {'step': 's1'}]))


class PackageTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.out = Path(self.temp.name)
        (self.out / 'package').mkdir()
        # Minimal exported shape with a dummyapp participant; generator is an injected boundary.
        self.workload = {'id': 'fixture', 'participants': [{'id': 'p1',
            'saga': 'com.example.dummyapp.item.coordination.sagas.CreateItemFunctionalitySagas', 'input': 'i'}],
            'setup': 'setup', 'interactions': [], 'schedule': [
                {'id': 's1', 'participant': 'p1', 'kind': 'step', 'faultSlot': 0}]}
        self.records = {'workloads': [self.workload], 'setups': [{'id': 'setup'}], 'sagas': [],
                        'requests': [], 'faultScenarios': [], 'accounting': {'configuration': {'cap': 2}}}
        self.write_package()
        self.calls = 0

    def write_package(self):
        descriptors = {}
        for role, value in self.records.items():
            p = self.out / 'package' / (role + ('.json' if isinstance(value, dict) else '.jsonl'))
            p.write_text(json.dumps(value) if isinstance(value, dict) else '\n'.join(map(json.dumps, value)))
            descriptors[role] = {'path': p.name, 'sha256': digest(p)}
        save(self.out / 'package/scenario-catalog-manifest.json', {'formatVersion': 1, 'files': descriptors})

    def request(self, out, vector, workload, cap, number, timeout):
        self.calls += 1
        c = {'id': 'returned-' + vector, 'workload': workload, 'faultVector': vector, 'actions': [{'step': 's1'}]}
        alias = {**c, 'id': 'old-' + vector}
        outside_cap = {**c, 'id': 'outside-' + vector, 'actions': [{'step': 's2'}]}
        self.records['faultScenarios'] += [c, alias, outside_cap]
        self.write_package()
        return {'status': 'EXITED', 'wallSeconds': 0}, {'status': 'DEDUPLICATED',
            'workloadPlanId': workload, 'assignedVector': vector, 'recoveryScheduleCap': cap,
            'writtenScheduleCount': 1, 'uncappedScheduleCount': '2', 'faultScenarioIds': [c['id']]}

    def test_request_cache_aliases_cap_and_exhaustion(self):
        d = Domain(self.out, self, 'fixture', 1)
        a = d.resolve('0')
        self.assertEqual(len(a), 1)
        self.assertEqual(len(a[0]['aliases']), 2)
        self.assertTrue(d.requests[0]['truncated'])
        self.assertEqual(a, d.resolve('0'))
        self.assertEqual(self.calls, 1)
        self.assertFalse(d.exhausted({a[0]['key']}))
        b = d.resolve('1')
        self.assertTrue(d.exhausted({a[0]['key'], b[0]['key']}))
        self.assertNotEqual(a[0]['key'], b[0]['key'])

    def test_missing_setup_workload_and_integrity(self):
        with self.assertRaises(ValueError):
            Domain(self.out, self, 'missing', 1)
        d = Domain(self.out, self, 'fixture', 1)
        self.records['setups'] = []
        self.write_package()
        with self.assertRaises(IntegrityError):
            d.verify()
        with self.assertRaises(ValueError):
            Domain(self.out, self, 'fixture', 1)

    def test_masked_slots_are_not_separate_genes(self):
        self.workload['schedule'].append({'id': 's2', 'participant': 'p1', 'faultSlot': 1})
        self.assertEqual(fault_coordinates(self.workload), [[None, 0, 1]])
        self.write_package()
        d = Domain(self.out, self, 'fixture', 2)
        with self.assertRaises(ValueError):
            d.resolve('11')


class ReadReportTest(unittest.TestCase):
    def test_join_coverage_counts_and_hashes(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp)
            execution = {'executionAttemptId': 'e', 'workloadPlanId': 'w', 'faultScenarioId': 's',
                         'scheduleConformance': 'EXACT', 'terminalStatus': 'SUCCESS'}
            save(d / 'execution-report.json', execution)
            save(d / 'manifest.json', {})
            report = {**execution, 'schemaVersion': 'microservices-simulator.saga-read-exposure.v2',
                'executionTerminalStatus': 'SUCCESS', 'executionValidity': 'COMPLETE',
                'collectionCoverage': 'COMPLETE_WITHIN_SCOPE', 'observedExposureCount': 0,
                'findings': [], 'gaps': [], 'scope': 'declared', 'contracts': [], 'excludedPaths': [],
                'artifacts': [{'role': role, 'status': 'AVAILABLE', 'sha256': digest(d / name)}
                    for role, name in [('EXECUTION_REPORT', 'execution-report.json'), ('PACKAGE_MANIFEST', 'manifest.json')]]}
            self.assertEqual(validate_read(report, execution, d, d / 'manifest.json')['A'], 0)
            for field, value in [('executionAttemptId', 'wrong'), ('observedExposureCount', 1),
                                 ('gaps', [{'reason': 'missing attribution'}])]:
                with self.subTest(field=field), self.assertRaises(ValueError):
                    validate_read({**report, field: value}, execution, d, d / 'manifest.json')
            partial = {**report, 'collectionCoverage': 'PARTIAL', 'gaps': [{'reason': 'gap'}]}
            self.assertEqual(validate_read(partial, execution, d, d / 'manifest.json')['ACoverage'], 'PARTIAL')


class LostCopiedUpdateReportTest(unittest.TestCase):
    def report(self):
        return {'schemaVersion': 'lost-copied-updates.v1', 'executionAttemptId': 'e',
            'workloadPlanId': 'w', 'faultScenarioId': 's', 'scheduleConformance': 'EXACT',
            'executionTerminalStatus': 'SUCCESS', 'validity': 'COMPLETE',
            'coverage': 'COMPLETE_WITHIN_SCOPE', 'count': 1,
            'findings': [{'findingId': 'finding-1', 'fields': [{'path': 'title'}]}], 'coverageGaps': []}

    def test_join_and_grouped_count(self):
        execution = {'executionAttemptId': 'e', 'workloadPlanId': 'w', 'faultScenarioId': 's',
                     'scheduleConformance': 'EXACT', 'terminalStatus': 'SUCCESS'}
        result = validate_lost_copied_updates(self.report(), execution)
        self.assertEqual(result['lostCopiedUpdateCount'], 1)
        self.assertEqual(len(result['lostCopiedUpdateFindings'][0]['fields']), 1)
        for field, value in [('executionAttemptId', 'wrong'), ('count', 2),
                             ('findings', [{'findingId': 'same'}, {'findingId': 'same'}])]:
            with self.subTest(field=field), self.assertRaises(ValueError):
                validate_lost_copied_updates({**self.report(), field: value}, execution)

    def test_incomplete_report_preserves_observed_count(self):
        execution = {'executionAttemptId': 'e', 'workloadPlanId': 'w', 'faultScenarioId': 's',
                     'scheduleConformance': 'EXACT', 'terminalStatus': 'SUCCESS'}
        report = {**self.report(), 'coverage': 'INCOMPLETE',
                  'coverageGaps': [{'reason': 'MISSING_ORIGIN'}]}
        self.assertEqual(validate_lost_copied_updates(report, execution)['lostCopiedUpdateCount'], 1)


if __name__ == '__main__':
    unittest.main()
