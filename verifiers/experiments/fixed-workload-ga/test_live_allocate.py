import json
from pathlib import Path
import tempfile
import threading
from types import SimpleNamespace
import unittest
from unittest.mock import patch

import live_allocate as live
from runtime import IntegrityError, read
from test_allocator import (FITNESS, PARAMETERS, complete_result, failure_result,
                            fixture_workload)


class FakeRuntime:
    def __init__(self, *, stop=None, unknown=False, status=None, fail_before=False,
                 fail_after=False, fail_verify_at=None):
        self.stop = stop
        self.unknown = unknown
        self.status = status
        self.fail_before = fail_before
        self.fail_after = fail_after
        self.fail_verify_at = fail_verify_at
        self.calls = []
        self.verify_calls = 0

    def verify(self):
        self.verify_calls += 1
        if self.verify_calls == self.fail_verify_at:
            raise IntegrityError('runtime drift')

    def evaluate(self, root, candidate, number, timeout):
        self.calls.append((candidate['key'], number))
        if self.fail_before:
            self.fail_before = False
            raise RuntimeError('coordinator lost result boundary')
        directory = root / f'attempt-{number:03d}'
        directory.mkdir()
        result = (failure_result(candidate, self.status) if self.status else
                  complete_result(candidate, score=number, unknown=self.unknown))
        live.durable_save(directory / 'attempt.json', result)
        if self.stop:
            self.stop.set()
        if self.fail_after:
            self.fail_after = False
            raise RuntimeError('crash after executor completion')
        return result


class LiveAllocationTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.config_path = self.root / 'live.json'

    def write_config(self, *, budget=4, policy='contextual-linucb-cooldown'):
        parameters = PARAMETERS.get(policy, {})
        self.config_path.write_text(json.dumps({
            'schemaVersion': live.SCHEMA, 'seed': 11, 'budget': budget,
            'policy': policy, 'policyParameters': parameters, 'fitness': FITNESS,
            'workloads': [{'name': 'fixture', 'config': 'map.json',
                           'catalogue': 'catalogue', 'control': 'control.json'}]}))

    def workload(self, count=6):
        workload = fixture_workload('w', count, 'shared')
        workload.update(mapConfig={'runtime': {'fixture': True}, 'timeout': 10},
                        cataloguePath=self.root / 'catalogue',
                        outputRootName='workload-001')
        workload['provenance'] = {
            'workload': 'w', 'name': 'fixture', 'config': str(self.root / 'map.json'),
            'configSha256': 'config', 'catalogue': str(self.root / 'catalogue'),
            'catalogueSha256': 'catalogue', 'cataloguePackageHashes': {},
            'candidateCount': count, 'control': str(self.root / 'control.json'),
            'controlSha256': 'control', 'runtime': {'fixture': True},
            'outputRoot': 'workload-001'}
        return workload

    @staticmethod
    def initialize(out, workloads):
        (out / 'dispatches').mkdir()
        (out / 'receipts').mkdir()
        (out / 'source').mkdir()
        for workload in workloads:
            (out / 'workloads' / workload['outputRootName']).mkdir(parents=True)

    def run_patches(self, workload, runtime, orphan_error=None):
        return (patch.object(live, 'load_inputs', return_value=([workload], {'w': runtime})),
                patch.object(live, 'initialize_output', side_effect=self.initialize),
                patch.object(live, 'verify_output'),
                patch.object(live, 'validate_attempt', side_effect=lambda path, *_: read(path)),
                patch.object(live, 'check_orphans', side_effect=orphan_error))

    def execute(self, output, workload, runtime, *, resume=False, stop=None, orphan_error=None):
        patches = self.run_patches(workload, runtime, orphan_error)
        with patches[0], patches[1], patches[2], patches[3], patches[4]:
            return live.execute(self.config_path, output, resume=resume, stop=stop)

    def resolve(self, output, workload, runtime, decision):
        patches = self.run_patches(workload, runtime)
        with patches[0], patches[2], patches[3], patches[4]:
            return live.resolve_spent(output, decision)

    def test_pause_and_resume_match_an_uninterrupted_seeded_trajectory(self):
        self.write_config(budget=4)
        stop = threading.Event()
        workload = self.workload()
        first = FakeRuntime(stop=stop)
        output = self.root / 'paused'
        paused = self.execute(output, workload, first, stop=stop)
        self.assertEqual(('PAUSED', 1), (paused['stopReason'], paused['globalAttempts']))

        second = FakeRuntime()
        resumed = self.execute(output, workload, second, resume=True)
        resumed_rows = [json.loads(line) for line in (output / 'decisions.jsonl').read_text().splitlines()]
        self.assertEqual(4, resumed['globalAttempts'])
        self.assertEqual(1, len(first.calls))
        self.assertEqual(3, len(second.calls))
        self.assertEqual(4, len({row['candidate'] for row in resumed_rows}))

        uninterrupted_workload = self.workload()
        uninterrupted = self.root / 'uninterrupted'
        self.execute(uninterrupted, uninterrupted_workload, FakeRuntime())
        uninterrupted_rows = [json.loads(line) for line in
                              (uninterrupted / 'decisions.jsonl').read_text().splitlines()]
        selected = lambda rows: [(row['workload'], row['candidate'], row['score']) for row in rows]
        self.assertEqual(selected(uninterrupted_rows), selected(resumed_rows))

    def test_unknown_feedback_consumes_budget_without_model_or_parent_update(self):
        self.write_config(budget=3, policy='adaptive-ucb')
        result = self.execute(self.root / 'unknown', self.workload(), FakeRuntime(unknown=True))
        self.assertEqual((3, 0), (result['unknowns'], result['modelUpdates']))
        self.assertEqual((0, 0),
                         (result['perWorkload'][0]['populationSize'],
                          result['perWorkload'][0]['knownScores']))
        self.assertEqual(3, result['verifiedAttemptResults'])
        self.assertTrue(result['controlGate']['outsideGlobalSearchBudget'])

    def test_completed_attempt_is_salvaged_after_interrupted_return(self):
        self.write_config(budget=2, policy='round-robin')
        workload = self.workload()
        output = self.root / 'salvage'
        crashed = FakeRuntime(fail_after=True)
        with self.assertRaisesRegex(RuntimeError, 'after executor completion'):
            self.execute(output, workload, crashed)
        self.assertEqual('REVIEW_REQUIRED', read(output / 'status.json')['stage'])

        resumed_runtime = FakeRuntime()
        result = self.execute(output, workload, resumed_runtime, resume=True)
        self.assertEqual(2, result['globalAttempts'])
        self.assertEqual(1, len(crashed.calls))
        self.assertEqual(1, len(resumed_runtime.calls))
        self.assertEqual(2, len(list((output / 'receipts').glob('decision-*.json'))))

    def test_ambiguous_dispatch_blocks_resume_until_explicitly_marked_spent(self):
        self.write_config(budget=2, policy='round-robin')
        workload = self.workload()
        output = self.root / 'ambiguous'
        failed = FakeRuntime(fail_before=True)
        with self.assertRaisesRegex(RuntimeError, 'lost result boundary'):
            self.execute(output, workload, failed)

        replacement = FakeRuntime()
        with self.assertRaises(live.AmbiguousDispatchError):
            self.execute(output, workload, replacement, resume=True)
        self.assertEqual([], replacement.calls)
        self.resolve(output, workload, replacement, 1)
        result = self.execute(output, workload, replacement, resume=True)
        self.assertEqual((2, 1, 1), (result['globalAttempts'], result['unknowns'],
                                     result['ambiguousDispatchesMarkedSpent']))
        self.assertEqual(1, len(replacement.calls))
        first = json.loads((output / 'receipts/decision-000001.json').read_text())
        self.assertEqual('AMBIGUOUS_SPENT', first['outcome'])

    def test_changed_completed_attempt_is_rejected_without_execution(self):
        self.write_config(budget=1, policy='round-robin')
        workload = self.workload()
        output = self.root / 'tampered'
        self.execute(output, workload, FakeRuntime())
        path = output / 'workloads/workload-001/attempt-001/attempt.json'
        path.write_text('{}')
        runtime = FakeRuntime()
        with self.assertRaisesRegex(IntegrityError, 'changed'):
            self.execute(output, workload, runtime, resume=True)
        self.assertEqual([], runtime.calls)

    def test_resume_refuses_docker_unavailable_before_salvage_or_dispatch(self):
        self.write_config(budget=2, policy='round-robin')
        stop = threading.Event()
        output = self.root / 'docker-unavailable'
        self.execute(output, self.workload(), FakeRuntime(stop=stop), stop=stop)
        replacement = FakeRuntime()
        with self.assertRaisesRegex(ValueError, 'daemon unavailable'):
            self.execute(output, self.workload(), replacement, resume=True,
                         orphan_error=ValueError('daemon unavailable'))
        self.assertEqual([], replacement.calls)

    def test_mounted_executor_blocks_resume_check(self):
        output = self.root / 'mounted'; output.mkdir()
        workload = self.workload()
        mounted = str((output / 'workloads/workload-001').resolve())
        responses = [SimpleNamespace(stdout='container-id\n'),
                     SimpleNamespace(stdout=json.dumps([
                         {'Name': '/old-executor', 'Mounts': [{'Source': mounted}]}]))]
        with patch.object(live.subprocess, 'run', side_effect=responses):
            with self.assertRaisesRegex(ValueError, 'still running'):
                live.check_orphans(output, [workload])

    def test_docker_query_failure_cannot_be_treated_as_no_orphan(self):
        output = self.root / 'docker-query'; output.mkdir()
        with patch.object(live.subprocess, 'run', side_effect=OSError('daemon unavailable')):
            with self.assertRaisesRegex(ValueError, 'Cannot prove'):
                live.check_orphans(output, [self.workload()])

    def test_failed_process_with_leftover_executor_stops_before_next_dispatch(self):
        self.write_config(budget=3, policy='round-robin')
        for status in ('TIMEOUT', 'PROCESS_FAILURE', 'INFRASTRUCTURE_FAILURE'):
            with self.subTest(status=status):
                runtime = FakeRuntime(status=status)
                output = self.root / ('orphan-' + status)
                with self.assertRaisesRegex(ValueError, 'still mounted'):
                    self.execute(output, self.workload(), runtime,
                                 orphan_error=ValueError('still mounted'))
                self.assertEqual(1, len(runtime.calls))
                self.assertEqual(1, len(list((output / 'dispatches').glob('decision-*.json'))))
                self.assertEqual(0, len(list((output / 'receipts').glob('decision-*.json'))))

    def test_runtime_is_reverified_before_each_new_dispatch(self):
        self.write_config(budget=3, policy='round-robin')
        runtime = FakeRuntime(fail_verify_at=2)
        output = self.root / 'runtime-drift'
        with self.assertRaisesRegex(IntegrityError, 'runtime drift'):
            self.execute(output, self.workload(), runtime)
        self.assertEqual(1, len(runtime.calls))
        self.assertEqual(1, len(list((output / 'dispatches').glob('decision-*.json'))))

    def test_transitive_report_validators_are_frozen_sources(self):
        paths = {path.name for path in live._source_paths()}
        self.assertTrue({'qualification.py', 'validate_assessment.py'} <= paths)

    def test_second_coordinator_cannot_take_lock(self):
        output = self.root / 'locked'; output.mkdir()
        with live.exclusive(output):
            with self.assertRaisesRegex(ValueError, 'already running'):
                with live.exclusive(output):
                    self.fail('lock acquired twice')

    def test_configuration_accepts_all_existing_policy_labels(self):
        for policy in live.SUPPORTED_POLICIES:
            with self.subTest(policy=policy):
                self.write_config(budget=1, policy=policy)
                parsed = live.parse_configuration(self.config_path)
                self.assertEqual(policy, parsed['policy'])
                self.assertEqual(PARAMETERS.get(policy, {}), parsed['policyParameters'])

    def test_live_input_gate_rejects_incomplete_control_before_catalogue_use(self):
        self.write_config(budget=1, policy='round-robin')
        (self.root / 'catalogue').mkdir()
        (self.root / 'catalogue/catalogue.json').write_text('{}')
        map_config = {'manifest': str(self.root / 'manifest.json'), 'workload': 'w',
                      'runtime': {'fixture': True}, 'recoveryCap': 4,
                      'fitness': FITNESS}
        (self.root / 'map.json').write_text(json.dumps(map_config))
        candidate = next(iter(self.workload(1)['domain'].candidates.values()))
        control = complete_result(candidate)
        control.update(terminalStatus='SUCCESS', scheduleConformance='DEVIATED')
        control['lostCopiedUpdateCoverage'] = 'INCOMPLETE'
        control['lostCopiedUpdateCoverageGaps'] = [{'reason': 'fixture'}]
        (self.root / 'control.json').write_text(json.dumps(control))
        config = live.parse_configuration(self.config_path)
        with patch.object(live, 'verified_control', return_value=('control', control)), \
                patch.object(live, 'load_catalogue') as catalogue:
            with self.assertRaisesRegex(ValueError, 'complete enabled score'):
                live.load_inputs(config)
        catalogue.assert_not_called()

    def test_live_input_gate_accepts_positive_control_without_exposing_reward(self):
        self.write_config(budget=1, policy='round-robin')
        (self.root / 'catalogue').mkdir()
        (self.root / 'catalogue/catalogue.json').write_text('{}')
        map_config = {'manifest': str(self.root / 'manifest.json'), 'workload': 'w',
                      'runtime': {'fixture': True}, 'recoveryCap': 4,
                      'fitness': FITNESS}
        (self.root / 'map.json').write_text(json.dumps(map_config))
        domain = self.workload(1)['domain']
        baseline = complete_result(next(iter(domain.candidates.values())), score=1)
        baseline['terminalStatus'] = 'PARTIAL_COMPENSATED'
        baseline['scheduleConformance'] = 'DEVIATED'
        with patch.object(live, 'verified_control', return_value=('control-hash', baseline)), \
                patch.object(live, 'load_catalogue', return_value=(domain, {
                    'workload': 'w', 'packageHashes': {}})), \
                patch.object(live, 'package', return_value={'hashes': {}, 'records': {}}), \
                patch.object(live, 'structural_profile', return_value={'fixture': True}), \
                patch.object(live.Runtime, 'verify'):
            workloads, _ = live.load_inputs(live.parse_configuration(self.config_path))
        self.assertEqual(1, len(workloads))
        self.assertEqual('control-hash', workloads[0]['provenance']['controlSha256'])
        self.assertNotIn('fitnessScore', workloads[0]['provenance'])

    def test_control_summary_must_match_recomputed_retained_attempt(self):
        directory = self.root / 'control-attempt'; directory.mkdir()
        candidate = next(iter(self.workload(1)['domain'].candidates.values()))
        raw = complete_result(candidate)
        raw.update(terminalStatus='SUCCESS', directory=str(directory),
                   packageHashes={'manifest': 'package'}, reportHashes={})
        live.durable_save(directory / 'attempt.json', raw)
        live.durable_save(directory / 'replay.json', {
            'runtime': {'fixture': True}, 'candidate': candidate,
            'packageHashes': raw['packageHashes']})
        control_path = self.root / 'control-summary.json'
        changed = dict(raw); changed['ACoverage'] = 'PARTIAL'
        live.durable_save(control_path, changed)
        with patch.object(live, 'check_control', return_value='control'), \
                patch.object(live, 'retained_attempt', return_value=raw):
            with self.assertRaisesRegex(IntegrityError, 'summary differs'):
                live.verified_control({'runtime': {'fixture': True}}, control_path)

        live.durable_save(control_path, raw)
        with patch.object(live, 'check_control', return_value='control'), \
                patch.object(live, 'retained_attempt', return_value=raw):
            self.assertEqual(('control', raw),
                             live.verified_control({'runtime': {'fixture': True}}, control_path))


if __name__ == '__main__':
    unittest.main()
