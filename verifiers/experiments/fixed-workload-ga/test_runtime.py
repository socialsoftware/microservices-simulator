"""Evaluator failures and replay snapshots; no Docker or retained local build required."""
import copy
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import runtime
import run as cli


class ArtifactCopyTest(unittest.TestCase):
    def test_snapshot_is_independent_of_source_and_destination_writes(self):
        with tempfile.TemporaryDirectory() as tmp:
            source, snapshot = Path(tmp) / 'source', Path(tmp) / 'snapshot'
            source.write_text('original')
            runtime.copy_artifact(source, snapshot)
            self.assertNotEqual(source.stat().st_ino, snapshot.stat().st_ino)
            source.write_text('changed source')
            self.assertEqual(snapshot.read_text(), 'original')
            snapshot.write_text('changed snapshot')
            self.assertEqual(source.read_text(), 'changed source')

    def test_clone_unavailable_falls_back_to_independent_copy(self):
        with tempfile.TemporaryDirectory() as tmp:
            source, snapshot = Path(tmp) / 'source', Path(tmp) / 'snapshot'
            source.write_text('original')
            with patch.object(runtime.sys, 'platform', 'darwin'), \
                    patch.object(runtime.subprocess, 'run', side_effect=OSError('no clone')):
                runtime.copy_artifact(source, snapshot)
            source.write_text('changed')
            self.assertEqual(snapshot.read_text(), 'original')


class EvaluatorTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.out = Path(self.temp.name) / 'arm'
        self.out.mkdir()
        package = self.out / 'package'; package.mkdir()
        files = {}
        for role in ('sagas', 'setups', 'workloads', 'faultScenarios', 'requests', 'accounting'):
            p = package / (role + ('.json' if role == 'accounting' else '.jsonl'))
            p.write_text('{}' if role == 'accounting' else '')
            files[role] = {'path': p.name, 'sha256': runtime.digest(p)}
        runtime.save(package / 'scenario-catalog-manifest.json', {'formatVersion': 1, 'files': files})
        self.candidate = {'id': 's', 'workload': 'w', 'faultVector': '0', 'actions': [{'step': 's1'}]}
        self.config = {'executorArgs': [], 'executorMain': 'fake', 'javaOptions': [],
                       'image': 'sha256:fake', 'classpath': ''}
        self.rt = runtime.Runtime(self.config)

    def reports(self, status='COMPLETE'):
        e = {'schemaVersion': 'microservices-simulator.scenario-execution-report.v6',
            'executionAttemptId': 'e', 'workloadPlanId': 'w', 'faultScenarioId': 's',
            'assignedVector': '0', 'terminalStatus': 'SUCCESS', 'scheduleConformance': 'EXACT',
            'packageManifestPath': '/out/package/scenario-catalog-manifest.json'}
        v1 = {**e, 'schemaVersion': 'microservices-simulator.scenario-impact-report.v1'}
        v2 = {**e, 'schemaVersion': 'microservices-simulator.scenario-impact-v2-assessment.v1',
            'executionTerminalStatus': 'SUCCESS', 'assessmentStatus': status,
            'completeScore': 0 if status == 'COMPLETE' else None, 'observedAffectedObjectCount': 0,
            'horizon': 'FINAL_SCHEDULED_ACTION', 'collectionStatus': 'OBSERVED', 'coverageGaps': [],
            'categoryResults': [{'category': c, 'candidateCount': 0, 'candidates': [],
                'positiveObjectCount': 0, 'findings': [], 'unknownReasons': [], 'coverageStatus': 'COMPLETE'}
                for c in ('DELETED_DEPENDENCY', 'FAILED_OPERATION_RESIDUAL', 'UNRESOLVED_DELIVERED_EVENT')]}
        return e, v1, v2

    def launch(self, reports):
        def fake(out, label, main, args, timeout):
            d = out / 'attempt-001'
            for name, report in zip(('execution-report.json', 'execution-report.impact.json',
                                    'execution-report.impact-v2.json'), reports):
                runtime.save(d / name, report)
            return {'status': 'EXITED', 'wallSeconds': 0, 'exitCode': 0}
        return fake

    def lost_report(self, count=1):
        return {'schemaVersion': 'lost-copied-updates.v1', 'executionAttemptId': 'e',
            'workloadPlanId': 'w', 'faultScenarioId': 's', 'scheduleConformance': 'EXACT',
            'executionTerminalStatus': 'SUCCESS', 'validity': 'COMPLETE',
            'coverage': 'COMPLETE_WITHIN_SCOPE', 'count': count,
            'findings': [{'findingId': f'f-{number}', 'fields': []} for number in range(count)],
            'coverageGaps': []}

    def test_missing_a_does_not_remove_complete_i(self):
        self.rt.launch = self.launch(self.reports())
        r = self.rt.evaluate(self.out, self.candidate, 1, 2)
        self.assertEqual(r['I'], 0)
        self.assertEqual(r['status'], 'COMPLETE')
        self.assertIsNone(r['A'])
        self.assertEqual(r['AStatus'], 'INVALID_OR_MISSING_REPORT')

    def test_partial_i_is_null(self):
        self.rt.launch = self.launch(self.reports('PARTIAL'))
        r = self.rt.evaluate(self.out, self.candidate, 1, 2)
        self.assertIsNone(r['I'])
        self.assertEqual(r['status'], 'PARTIAL')

    def test_wrong_report_join_is_not_zero(self):
        reports = self.reports(); reports[1]['executionAttemptId'] = 'another attempt'
        self.rt.launch = self.launch(reports)
        r = self.rt.evaluate(self.out, self.candidate, 1, 2)
        self.assertIsNone(r['I'])
        self.assertEqual(r['status'], 'INVALID_REPORT')

    def test_process_failure_does_not_fabricate_reports(self):
        self.rt.launch = lambda *args: {'status': 'PROCESS_FAILURE', 'wallSeconds': 0, 'exitCode': 1}
        r = self.rt.evaluate(self.out, self.candidate, 1, 2)
        self.assertEqual(r['status'], 'PROCESS_FAILURE')
        self.assertIsNone(r['I'])

    def test_retained_unavailable_i_still_loads_read_evidence(self):
        reports = self.reports('UNAVAILABLE')
        reports[2]['observedAffectedObjectCount'] = None
        reports[2]['collectionStatus'] = 'UNAVAILABLE'
        for category in reports[2]['categoryResults']:
            category['coverageStatus'] = 'UNAVAILABLE'
        base_launch = self.launch(reports)
        def launch(*args):
            process = base_launch(*args)
            runtime.save(self.out / 'attempt-001/execution-report.saga-read-exposure.json', {})
            return process
        self.rt.launch = launch
        with patch.object(runtime, 'validate_read', return_value={
                'A': 1, 'AStatus': 'COMPLETE', 'ACoverage': 'COMPLETE_WITHIN_SCOPE', 'AGaps': []}):
            # The retained loader must reach read validation, not discard it with unavailable I.
            self.rt.evaluate(self.out, self.candidate, 1, 2)
            r = runtime.retained_attempt(self.out / 'attempt-001/attempt.json')
        self.assertIsNone(r['I'])
        self.assertEqual(r['A'], 1)

    def test_later_added_read_report_is_not_original_evidence(self):
        self.rt.launch = self.launch(self.reports())
        self.rt.evaluate(self.out, self.candidate, 1, 2)
        runtime.save(self.out / 'attempt-001/execution-report.saga-read-exposure.json', {})
        with patch.object(runtime, 'validate_read', side_effect=AssertionError('Unattested report')):
            r = runtime.retained_attempt(self.out / 'attempt-001/attempt.json')
        self.assertIsNone(r['A'])

    def test_retained_report_mutation_is_rejected(self):
        self.rt.launch = self.launch(self.reports())
        self.rt.evaluate(self.out, self.candidate, 1, 2)
        runtime.save(self.out / 'attempt-001/execution-report.json', {})
        with self.assertRaises(runtime.IntegrityError):
            runtime.retained_attempt(self.out / 'attempt-001/attempt.json')

    def test_lost_copied_update_sidecar_is_ingested_and_attested(self):
        base_launch = self.launch(self.reports())
        def launch(*args):
            process = base_launch(*args)
            runtime.save(self.out / 'attempt-001/execution-report-lost-copied-updates.json',
                         self.lost_report(2))
            return process
        self.rt.launch = launch
        result = self.rt.evaluate(self.out, self.candidate, 1, 2)
        self.assertEqual(result['lostCopiedUpdateCount'], 2)
        sidecar = self.out / 'attempt-001/execution-report-lost-copied-updates.json'
        self.assertEqual(result['reportHashes'][sidecar.name], runtime.digest(sidecar))
        retained = runtime.retained_attempt(self.out / 'attempt-001/attempt.json')
        self.assertEqual(retained['lostCopiedUpdateCount'], 2)
        runtime.save(sidecar, self.lost_report(1))
        with self.assertRaises(runtime.IntegrityError):
            runtime.retained_attempt(self.out / 'attempt-001/attempt.json')

    def test_replay_uses_attempt_snapshot_after_arm_evolves(self):
        self.rt.launch = self.launch(self.reports())
        original = self.rt.evaluate(self.out, self.candidate, 1, 2)
        (self.out / 'package/scenario-catalog-manifest.json').write_text('changed by later request')
        with patch.object(runtime.Runtime, 'verify'), patch.object(runtime.Runtime, 'evaluate', return_value=original) as evaluate:
            destination = self.out.parent / 'replay'
            cli.replay(self.out / 'attempt-001/attempt.json', destination)
            self.assertEqual(runtime.package(destination / 'package/scenario-catalog-manifest.json')['hashes'],
                             original['packageHashes'])
            self.assertEqual(evaluate.call_args.args[1], self.candidate)

    def test_agent_is_attested_and_only_added_to_executor_worker(self):
        agent = self.out / 'copy-agent.jar'
        agent.write_bytes(b'agent')
        source_root = self.out / 'quizzes'
        (source_root / 'src/main/java').mkdir(parents=True)
        descriptor = {**self.config, 'hashes': {str(agent): runtime.digest(agent)},
            'lostCopiedUpdateAgent': {'path': str(agent), 'sha256': runtime.digest(agent)},
            'lostCopiedUpdateSourceRoot': str(source_root)}
        rt = runtime.Runtime(descriptor)
        def container(path):
            return '/reports/quizzes' if Path(path) == source_root else '/reports/copy-agent.jar'
        with patch.object(runtime.batch, 'container', side_effect=container):
            rt.verify()
            executor = rt.command(self.out, 'executor', 'fake', [])
            generator = rt.command(self.out, 'generator', 'generator-main', [])
        self.assertIn('-javaagent:/reports/copy-agent.jar', executor)
        self.assertIn('-Dsimulator.copied-update.contracts=/out/package/copy-contracts.json', executor)
        self.assertIn('-Dsimulator.copied-update.source-root=/reports/quizzes', executor)
        self.assertFalse(any(option.startswith('-javaagent:') for option in generator))
        descriptor['lostCopiedUpdateAgent']['sha256'] = 'wrong'
        with patch.object(runtime.batch, 'container', side_effect=container), \
                self.assertRaises(runtime.IntegrityError):
            rt.verify()

    def test_source_root_must_be_an_existing_target_directory(self):
        marker = self.out / 'runtime-marker'
        marker.write_text('runtime')
        descriptor = {**self.config, 'hashes': {str(marker): runtime.digest(marker)},
                      'lostCopiedUpdateSourceRoot': str(self.out / 'missing')}
        with patch.object(runtime.batch, 'container', return_value='/reports/missing'), \
                self.assertRaisesRegex(runtime.IntegrityError, 'not a directory'):
            runtime.Runtime(descriptor).verify()

    def test_agent_enables_observer_for_control_and_attempt(self):
        self.config['lostCopiedUpdateAgent'] = {
            'path': str(runtime.batch.TARGET / 'agent.jar'), 'sha256': 'hash'}
        captured = {}
        def launch(out, label, main, args, timeout):
            captured['args'] = args
            return {'status': 'PROCESS_FAILURE', 'wallSeconds': 0, 'exitCode': 1}
        self.rt.launch = launch
        self.rt.evaluate(self.out, self.candidate, 1, 2)
        self.assertIn('--microservices.simulator.lost-copied-update.enabled=true', captured['args'])


if __name__ == '__main__':
    unittest.main()
