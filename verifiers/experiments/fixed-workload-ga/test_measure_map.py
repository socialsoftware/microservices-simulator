import json
from pathlib import Path
import tempfile
import threading
import unittest
from unittest.mock import patch

import measure_map as campaign
from fitness import configuration
from runtime import IntegrityError, read, save
import test_catalogue


class FakeRuntime:
    def __init__(self, stop=None, failure=False):
        self.calls = []
        self.stop = stop
        self.failure = failure
        self.config = {'fixture': True}

    def verify(self):
        pass

    def evaluate(self, out, candidate, number, timeout):
        self.calls.append((candidate['key'], number))
        directory = out / f'attempt-{number:03d}'
        directory.mkdir()
        if self.stop:
            self.stop.set()
        if self.failure:
            raise ValueError('deliberate failure')
        value = {'candidate': candidate, 'status': 'COMPLETE', 'I': None,
                 'packageHashes': {}, 'directory': str(directory)}
        save(directory / 'replay.json', {'runtime': self.config})
        save(directory / 'attempt.json', value)
        return value


class CampaignTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(); self.addCleanup(self.temp.cleanup)
        self.out = Path(self.temp.name)
        (self.out / 'receipts').mkdir()
        self.candidates = {str(n): {'key': str(n)} for n in range(10)}

    def collect(self, runtime, observations=None, number=1, stop=None, workers=2):
        return campaign.collect(self.out, self.candidates, runtime, configuration(), workers, 10,
                                observations if observations is not None else {}, number,
                                stop or threading.Event())

    def test_pause_drains_only_in_flight_then_resume_without_repeating_nulls(self):
        stop = threading.Event(); runtime = FakeRuntime(stop)
        result = self.collect(runtime, stop=stop)
        self.assertEqual('PAUSED', result['stage'])
        self.assertEqual(0, result['inFlight'])
        self.assertLessEqual(len(runtime.calls), 2)
        with patch.object(campaign, 'retained_attempt', side_effect=read):
            observations, next_number = campaign.restore(self.out, self.candidates, runtime.config, {})
        self.assertEqual(len(runtime.calls), len(observations))
        second = FakeRuntime()
        result = self.collect(second, observations, next_number)
        self.assertEqual('COMPLETE', result['stage'])
        self.assertEqual(10, len(runtime.calls) + len(second.calls))
        self.assertEqual(10, len(set(k for k, _ in runtime.calls + second.calls)))
        self.assertTrue(all(v['fitnessScore'] is None for v in read(self.out / 'reference.json')['fitness'].values()))

    def test_pause_file_prevents_dispatch(self):
        (self.out / 'PAUSE').touch()
        runtime = FakeRuntime()
        self.assertEqual('PAUSED', self.collect(runtime)['stage'])
        self.assertEqual([], runtime.calls)

    def test_crash_recovers_finished_attempt_without_receipt_preserves_incomplete(self):
        runtime = FakeRuntime()
        runtime.evaluate(self.out, self.candidates['0'], 1, 10)
        (self.out / 'attempt-002').mkdir()
        with patch.object(campaign, 'retained_attempt', side_effect=read):
            observations, number = campaign.restore(self.out, self.candidates, runtime.config, {})
        self.assertEqual(3, number)
        self.assertEqual({'0'}, set(observations))
        self.assertTrue((self.out / 'receipts/attempt-001.json').exists())
        self.collect(runtime, observations, number)
        self.assertTrue((self.out / 'attempt-002').exists())
        self.assertEqual(10, len(runtime.calls))

    def test_tampered_or_missing_completed_attempt_is_not_reexecuted(self):
        runtime = FakeRuntime(); self.collect(runtime)
        path = self.out / 'attempt-001/attempt.json'
        path.write_text('{}')
        with self.assertRaises(IntegrityError):
            campaign.restore(self.out, self.candidates, runtime.config, {})
        path.unlink()
        with self.assertRaises(IntegrityError):
            campaign.restore(self.out, self.candidates, runtime.config, {})

    def test_failure_stops_dispatch_and_marks_failed(self):
        runtime = FakeRuntime(failure=True)
        with self.assertRaisesRegex(RuntimeError, 'deliberate'):
            self.collect(runtime)
        self.assertLessEqual(len(runtime.calls), 2)
        self.assertEqual('FAILED', read(self.out / 'status.json')['stage'])
        self.assertFalse((self.out / 'reference.json').exists())

    def test_second_coordinator_cannot_take_lock(self):
        with campaign.exclusive(self.out):
            with self.assertRaisesRegex(ValueError, 'already running'):
                with campaign.exclusive(self.out):
                    self.fail('lock acquired twice')

    def test_running_orphan_blocks_resume(self):
        from types import SimpleNamespace
        responses = [SimpleNamespace(stdout='container-id\n'), SimpleNamespace(stdout=json.dumps([
            {'Name': '/previous', 'Mounts': [{'Source': str(self.out)}]}]))]
        with patch.object(campaign.subprocess, 'run', side_effect=responses):
            with self.assertRaisesRegex(ValueError, 'still running'):
                campaign.check_orphans(self.out)

    def test_parallelism_is_bounded_and_pause_waits_for_running_jobs(self):
        started = threading.Barrier(4); release = threading.Event(); stop = threading.Event()
        class BlockingRuntime(FakeRuntime):
            def evaluate(inner, *args):
                started.wait(timeout=5); release.wait(timeout=5)
                return super().evaluate(*args)
        runtime = BlockingRuntime()
        result = []
        thread = threading.Thread(target=lambda: result.append(self.collect(runtime, stop=stop, workers=3)))
        thread.start(); started.wait(timeout=5)
        stop.set()
        self.assertTrue(thread.is_alive())
        release.set(); thread.join(timeout=5)
        self.assertFalse(thread.is_alive())
        self.assertEqual(3, len(runtime.calls))
        self.assertEqual('PAUSED', result[0]['stage'])

    def test_real_package_resume_rejects_config_drift_and_preserves_selection(self):
        fixture = test_catalogue.CatalogueTests(); fixture.setUp(); self.addCleanup(fixture.doCleanups)
        fixture.build()
        control = fixture.out / 'control.json'
        save(control, {'status': 'COMPLETE', 'terminalStatus': 'PARTIAL_COMPENSATED',
                       'scheduleConformance': 'DEVIATED', 'I': 3})
        out = fixture.out / 'campaign'
        stop = threading.Event(); stop.set()
        with patch.object(campaign, 'check_control', return_value='control'), \
                patch.object(campaign.Runtime, 'verify'), patch.object(campaign, 'check_orphans'):
            result = campaign.execute(fixture.out / 'config.json', fixture.out, control, out, stop=stop)
            self.assertEqual('PAUSED', result['stage'])
            save(fixture.out / 'config.json', {**fixture.config, 'timeout': 99})
            with self.assertRaisesRegex(IntegrityError, 'changed'):
                campaign.execute(fixture.out / 'config.json', fixture.out, control, out, resume=True)
            save(fixture.out / 'config.json', fixture.config)
            # Worker count is a session setting, not a change to scenario semantics.
            result = campaign.execute(fixture.out / 'config.json', fixture.out, control, out,
                                      workers=4, resume=True, stop=stop)
            self.assertEqual(4, result['workers'])
            self.assertEqual(2, len(read(out / 'sessions.json')))


if __name__ == '__main__':
    unittest.main()
