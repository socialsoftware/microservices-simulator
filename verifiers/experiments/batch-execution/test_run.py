"""Runner contracts exercised without Docker or application compilation."""
import copy
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

import run
from summarize import summarize


def fixture():
    common = {'executionAttemptId': 'fresh-id', 'workloadPlanId': 'workload', 'faultScenarioId': 'scenario'}
    execution = {**common, 'schemaVersion': 'microservices-simulator.scenario-execution-report.v5',
        'assignedVector': '0', 'terminalStatus': 'SUCCESS', 'scheduleConformance': 'EXACT',
        'packageManifestPath': '/reports/manifest.json', 'actualActions': [],
        'sourceSetup': {'durationNanos': 1000000}, 'prerequisiteSetup': {'durationNanos': 0}}
    v1 = {**common, 'schemaVersion': 'microservices-simulator.scenario-impact-report.v1',
          'evaluationStatus': 'EVALUATED', 'impactScore': 0}
    v2 = {**common, 'schemaVersion': 'microservices-simulator.scenario-impact-v2-assessment.v1',
        'packageManifestPath': '/reports/manifest.json', 'executionTerminalStatus': 'SUCCESS',
        'scheduleConformance': 'EXACT', 'assessmentStatus': 'COMPLETE', 'completeScore': 0,
        'observedAffectedObjectCount': 0, 'horizon': 'FINAL_SCHEDULED_ACTION',
        'collectionStatus': 'OBSERVED', 'coverageGaps': [], 'finalState': [],
        'categoryResults': [{'category': c, 'candidateCount': 0, 'candidates': [],
            'positiveObjectCount': 0, 'findings': [], 'unknownReasons': [], 'coverageStatus': 'COMPLETE'}
            for c in ['DELETED_DEPENDENCY', 'FAILED_OPERATION_RESIDUAL', 'UNRESOLVED_DELIVERED_EVENT']]}
    return execution, v1, v2


class RunnerTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.target = patch.object(run, 'TARGET', self.root)
        self.target.start()
        self.addCleanup(self.target.stop)
        self.row = {'caseId': 'control', 'workloadId': 'workload', 'faultScenarioId': 'scenario',
                    'faultVector': '0', 'expected': run.semantic(*fixture())}
        self.selection = {'build': str(self.root / 'build'), 'manifest': str(self.root / 'manifest.json')}

    def test_timeout_kills_process_and_retains_log_and_command(self):
        cleaned = []
        result = run.run_process([sys.executable, '-u', '-c', 'import time; print("started"); time.sleep(30)'],
            self.root / 'timeout.log', 0.1, lambda: cleaned.append(True))
        self.assertEqual('TIMEOUT', result['status'])
        self.assertEqual([True], cleaned)
        self.assertIn('started', (self.root / 'timeout.log').read_text())
        self.assertTrue((self.root / 'timeout.command.json').exists())
        self.assertLess(result['wallSeconds'], 5)

    def execute(self, code, repetition=1):
        with patch.object(run, 'compose', return_value=[sys.executable, '-c', code]):
            return run.attempt(self.root, self.selection, self.row, repetition, 1, 5)

    def test_failed_process_and_missing_report_are_retained_without_score(self):
        failed = self.execute('raise SystemExit(7)')
        missing = self.execute('pass', 2)
        self.assertEqual('PROCESS_FAILURE', failed['status'])
        self.assertEqual(7, failed['exitCode'])
        self.assertEqual('INVALID_REPORT', missing['status'])
        for result in (failed, missing):
            self.assertIsNone(result['score'])
            self.assertEqual(result, run.read(Path(result['directory']) / 'attempt.json'))

    def write_reports_command(self, values, repetition=1):
        directory = self.root / 'c1' / f'r{repetition:02d}-control'
        return ';'.join(f'open({str(directory / name)!r}, "w").write({json.dumps(value)!r})'
                        for name, value in zip(run.REPORTS, values))

    def test_valid_zero_is_distinct_from_missing_or_invalid(self):
        result = self.execute(self.write_reports_command(fixture()))
        self.assertEqual('PASS', result['status'])
        self.assertEqual(0, result['score'])
        self.assertEqual(3, len(result['reportHashes']))
        self.assertEqual(.001, result['sourceSetupSeconds'])

    def test_current_and_historical_execution_schemas_are_explicitly_supported(self):
        current = fixture()
        current[0]['schemaVersion'] = 'microservices-simulator.scenario-execution-report.v6'
        accepted = self.execute(self.write_reports_command(current))
        self.assertEqual('PASS', accepted['status'])

        unsupported = fixture()
        unsupported[0]['schemaVersion'] = 'microservices-simulator.scenario-execution-report.v4'
        rejected = self.execute(self.write_reports_command(unsupported, 2), 2)
        self.assertEqual('INVALID_REPORT', rejected['status'])
        self.assertIsNone(rejected['score'])

    def test_mismatched_identity_is_invalid_not_zero(self):
        values = fixture()
        values[2]['executionAttemptId'] = 'another-process'
        result = self.execute(self.write_reports_command(values))
        self.assertEqual('INVALID_REPORT', result['status'])
        self.assertIsNone(result['score'])
        self.assertEqual(3, len(result['reportHashes']))

    def test_malformed_json_is_retained(self):
        file = self.root / 'c1/r01-control/execution.json'
        result = self.execute(f'open({str(file)!r}, "w").write("invalid json")')
        self.assertEqual('INVALID_REPORT', result['status'])
        self.assertIsNone(result['score'])
        self.assertEqual('invalid json', file.read_text())

    def test_launch_failure_is_retained(self):
        with patch.object(run, 'compose', return_value=['/missing/batch-command']):
            result = run.attempt(self.root, self.selection, self.row, 1, 1, 5)
        self.assertEqual('INFRASTRUCTURE_FAILURE', result['status'])
        self.assertIsNone(result['score'])
        self.assertEqual(result, run.read(Path(result['directory']) / 'attempt.json'))

    def test_semantic_difference_remains_visible(self):
        self.row['expected']['terminalStatus'] = 'COMPENSATED'
        result = self.execute(self.write_reports_command(fixture()))
        self.assertEqual('SEMANTIC_DIVERGENCE', result['status'])
        self.assertEqual('SUCCESS', result['observed']['terminalStatus'])

    def test_new_ids_do_not_change_semantics(self):
        values = fixture()
        updated = copy.deepcopy(values)
        for report in updated:
            report['executionAttemptId'] = 'new-id'
        self.assertEqual(run.semantic(*values), run.semantic(*updated))

    def test_package_drift_rejected(self):
        with patch.object(run, 'source_hashes', return_value={}), patch.object(run, 'package', return_value={'hashes': {'manifest': 'changed'}}):
            with self.assertRaisesRegex(ValueError, 'Package drift'):
                run.verify({'sourceHashes': {}, 'manifest': 'manifest.json', 'packageHashes': {'manifest': 'frozen'}})

    def test_maven_cache_must_match_prepared_module_jars(self):
        hashes = []
        for module in ('simulator', 'verifiers'):
            jar = self.root / 'source' / module / 'target' / (module + '.jar')
            jar.parent.mkdir(parents=True)
            jar.write_bytes(module.encode())
            hashes.append(run.digest(jar) + '  /cache/' + jar.name)
        run.verify_prepared_jars(self.root, hashes)
        hashes[0] = '0' * 64 + hashes[0][64:]
        with self.assertRaisesRegex(ValueError, 'Maven cache differs'):
            run.verify_prepared_jars(self.root, hashes)

    def test_existing_attempt_is_not_overwritten(self):
        result = self.execute('raise SystemExit(7)')
        with self.assertRaises(FileExistsError):
            self.execute('pass')
        self.assertEqual(result, run.read(Path(result['directory']) / 'attempt.json'))

    def test_summary_keeps_failure_denominator_and_rejects_changed_artifacts(self):
        good = self.execute(self.write_reports_command(fixture()))
        bad = self.execute('raise SystemExit(7)', 2)
        run.save(self.root / 'selection.json', {**self.selection, 'rows': [self.row]})
        run.save(self.root / 'plan.json', {'selectionSha256': run.digest(self.root / 'selection.json'),
            'plannedAttempts': 2, 'preparationSeconds': 0})
        run.save(self.root / 'results.json', {'planSha256': run.digest(self.root / 'plan.json'),
            'validation': 'FAIL', 'attempts': [good, bad], 'groups': [], 'totalWallSeconds': 1})
        summary = summarize(self.root)
        self.assertEqual(2, summary['retainedAttempts'])
        self.assertEqual({'PASS': 1, 'PROCESS_FAILURE': 1}, summary['statuses'])
        file = Path(good['directory']) / 'execution.json'
        file.write_text(file.read_text() + '\n')
        with self.assertRaisesRegex(ValueError, 'Changed report'):
            summarize(self.root)


if __name__ == '__main__':
    unittest.main()
