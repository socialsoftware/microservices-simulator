import copy
import random
from pathlib import Path
import shutil
import tempfile
import unittest
from unittest.mock import patch

import catalogue
import run as runner
from runtime import Domain, Runtime, IntegrityError, package, read, save
import test_search


class CatalogueTests(unittest.TestCase):
    def setUp(self):
        self.fixture = test_search.PackageTest()
        self.fixture.setUp()
        self.addCleanup(self.fixture.doCleanups)
        self.out = self.fixture.out
        self.original = self.out / 'original'
        shutil.copytree(self.out / 'package', self.original)
        self.source_hashes = package(self.original / 'scenario-catalog-manifest.json')['hashes']
        self.config = {'manifest': str(self.original / 'scenario-catalog-manifest.json'),
            'workload': 'fixture', 'runtime': {'fixture': True}, 'recoveryCap': 10,
            'strategy': 'ga', 'seed': 11, 'budget': 3, 'population': 2}
        save(self.out / 'config.json', self.config)
        save(self.out / 'source-package-hashes.json', self.source_hashes)
        self.fixture.verify = lambda: None
        old = self.fixture.request
        def complete(*args):
            process, response = old(*args)
            response['uncappedScheduleCount'] = '1'
            return process, response
        self.fixture.request = complete
        self.domain = Domain(self.out, self.fixture, 'fixture', 10)

    def build(self):
        return catalogue.build(self.domain, self.fixture)

    def test_complete_catalogue_reuses_structure_without_scores(self):
        self.build()
        domain, seal = catalogue.load(self.out, self.config, self.source_hashes)
        self.assertEqual(2, seal['candidateCount'])
        self.assertEqual({'0','1'}, {c['faultVector'] for c in domain.candidates.values()})
        # Historical aliases/outside-request entries cannot expand the catalogue.
        self.assertEqual(6, len(self.fixture.records['faultScenarios']))
        self.assertFalse(any('fitnessScore' in c for c in domain.candidates.values()))
        altered = copy.deepcopy(self.config); altered['runtime'] = {'other': True}
        with self.assertRaisesRegex(ValueError, 'scope differs'): catalogue.load(self.out, altered, self.source_hashes)
        with self.assertRaisesRegex(IntegrityError, 'source package'): catalogue.load(self.out, self.config, {})
        (self.out / 'requests.json').write_text('[]')
        with self.assertRaisesRegex(IntegrityError, 'evidence changed'): catalogue.load(self.out, self.config, self.source_hashes)

    def test_truncation_and_generator_failure_do_not_publish_complete_catalogue(self):
        old = self.fixture.request
        def truncated(*args):
            process, response = old(*args); response['uncappedScheduleCount'] = '2'
            return process,response
        self.fixture.request = truncated
        with self.assertRaisesRegex(ValueError, 'truncated'): self.build()
        self.assertFalse((self.out / 'catalogue.json').exists())
        self.assertTrue((self.out / 'catalogue-failure.json').exists())
        self.domain.cache.clear(); self.domain.requests.clear()
        self.fixture.request = lambda *args: ({'status':'TIMEOUT','wallSeconds':1}, None)
        with self.assertRaisesRegex(ValueError, 'generation failed'): self.build()
        self.assertFalse((self.out / 'catalogue.json').exists())

    def test_live_command_uses_selected_candidate_and_preserves_unknown_feedback(self):
        self.build()
        baseline = self.out / 'control.json'
        save(baseline, {'terminalStatus':'SUCCESS', 'scheduleConformance':'EXACT', 'status':'COMPLETE', 'I':0})
        calls = []
        def evaluate(runtime, out, candidate, number, timeout):
            # The selected scenario is present in the actual runtime package.
            ids = {c['id'] for c in package(out / 'package/scenario-catalog-manifest.json')['records']['faultScenarios']}
            self.assertIn(candidate['id'], ids)
            calls.append(candidate['key'])
            return {'status':'COMPLETE' if candidate['faultVector']=='0' else 'PROCESS_FAILURE',
                    'I':0 if candidate['faultVector']=='0' else None, 'A':0, 'wallSeconds':1}
        with patch.object(runner, 'check_control', return_value='fixture'), \
                patch.object(Runtime, 'verify'), patch.object(Runtime, 'evaluate', evaluate):
            for strategy in ['ga','random']:
                calls.clear()
                result = runner.execute({**self.config,'strategy':strategy}, self.out / strategy, baseline, self.out)
                self.assertEqual('EXHAUSTED', result['stopReason'])
                self.assertEqual(2,len(set(calls)))
                self.assertEqual(1,result['nullFitnessAttempts'])
                self.assertEqual('complete-catalogue', result['candidateDomain'])
                self.assertEqual(0,result['generationRequests'])
                self.assertEqual(2,result['catalogue']['candidateCount'])
                self.assertEqual('PASS',result['integrity'])
                self.assertEqual(2,result['applicationSeconds'])

    def test_unqualified_control_prevents_live_attempts(self):
        self.build()
        baseline = self.out / 'control.json'
        save(baseline, {'status':'COMPLETE', 'terminalStatus':'INVALID',
                        'scheduleConformance':'DEVIATED','I':0})
        with patch.object(runner, 'check_control',return_value='fixture'), patch.object(Runtime,'evaluate') as evaluate:
            with self.assertRaisesRegex(ValueError,'valid measured'):
                runner.execute(self.config,self.out/'rejected',baseline,self.out)
            evaluate.assert_not_called()
            self.assertFalse((self.out/'rejected').exists())

    def test_positive_control_is_admitted_without_becoming_search_feedback(self):
        self.build()
        baseline = self.out / 'positive-control.json'
        save(baseline, {'status': 'COMPLETE', 'terminalStatus': 'PARTIAL_COMPENSATED',
                        'scheduleConformance': 'DEVIATED', 'I': 7})
        observed = []
        def evaluate(runtime, out, candidate, number, timeout):
            observed.append(candidate['key'])
            return {'status': 'COMPLETE', 'I': 0, 'A': 0, 'wallSeconds': 1}
        with patch.object(runner, 'check_control', return_value='fixture'), \
                patch.object(Runtime, 'verify'), patch.object(Runtime, 'evaluate', evaluate):
            result = runner.execute({**self.config, 'budget': 1},
                                    self.out / 'positive-search', baseline, self.out)
        self.assertEqual(1, len(observed))
        self.assertEqual(1, len(result['attempts']))
        self.assertEqual(0, result['attempts'][0]['fitnessScore'])
        self.assertEqual(0, result['positiveScenarios'])

    def test_incomplete_control_is_rejected_before_search(self):
        self.build()
        baseline = self.out / 'incomplete-control.json'
        save(baseline, {'status': 'COMPLETE', 'terminalStatus': 'SUCCESS',
                        'scheduleConformance': 'EXACT', 'I': None})
        with patch.object(runner, 'check_control', return_value='fixture'), \
                patch.object(Runtime, 'evaluate') as evaluate:
            with self.assertRaisesRegex(ValueError, 'complete enabled score'):
                runner.execute(self.config, self.out / 'incomplete-search', baseline, self.out)
            evaluate.assert_not_called()

    def test_uniform_random_matches_unseen_draws_and_ignores_feedback(self):
        self.build()
        domain, _ = catalogue.load(self.out, self.config, self.source_hashes)
        from search import run
        for seed in range(10):
            rng = random.Random(seed)
            remaining = sorted(domain.candidates)
            expected = []
            while remaining:
                key = rng.choice(remaining); expected.append(key); remaining.remove(key)
            for feedback in [0, 9, None]:
                result = run(domain, lambda c,n: {'I':feedback}, strategy='random',
                             seed=seed, budget=3, exploration='uniform-unseen')
                self.assertEqual(expected, [a['key'] for a in result['attempts']])


if __name__ == '__main__': unittest.main()
