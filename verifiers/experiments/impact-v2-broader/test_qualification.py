#!/usr/bin/env python3
"""Focused regression tests for historical-family workload selection."""

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("qualification.py")
SPEC = importlib.util.spec_from_file_location("qualification", MODULE_PATH)
qualification = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(qualification)


class WorkloadSelectionTest(unittest.TestCase):
    def workload(self):
        participants = [
            {"id": "p1", "saga": qualification.ADD},
            {"id": "p2", "saga": qualification.REMOVE},
        ]
        schedule = [
            {"kind": "step", "sagaStep": name, "participant": "p2" if index < 3 else "p1"}
            for index, name in enumerate(qualification.EXPECTED_STEPS)
        ]
        return {"id": "workload", "participants": participants, "schedule": schedule,
                "interactions": [], "setup": "setup"}

    def test_event_bearing_schedule_cannot_match_filtered_step_sequence(self):
        workload = self.workload()
        workload["schedule"].insert(3, {"kind": "event", "event": "delivered"})
        data = {"records": {"workloads": [workload], "setups": [{"id": "setup"}]}}

        with self.assertRaisesRegex(qualification.QualificationError,
                                   "Expected one exact current benchmark workload, found 0"):
            qualification.select_workload(data)

    def test_missing_execution_reports_remain_null_and_preserve_attempt_evidence(self):
        with tempfile.TemporaryDirectory(prefix="impact-v2-historical-missing-") as directory:
            root = Path(directory)
            attempt = root / "attempts" / "scenario"
            attempt.mkdir(parents=True)
            (attempt / "process-exit-code").write_text("17\n", encoding="utf-8")
            (attempt / "docker.log").write_text("failed before reports\n", encoding="utf-8")
            plan = {"workloadPlanId": "workload", "rows": [{
                "vector": "00000", "currentOrdinal": 1, "faultScenarioId": "scenario",
                "structuralKeySha256": "key", "exactHistoricalMatchCount": 0,
                "projectedHistoricalRowCount": 2,
                "projectedHistoricalLabel": "NO_BROKEN_REFERENCE",
            }]}
            plan_path = root / "plan.json"
            plan_path.write_text(json.dumps(plan), encoding="utf-8")
            output_json, output_csv = root / "results.json", root / "results.csv"
            args = type("Args", (), {"plan": plan_path, "attempt_dir": root / "attempts",
                                      "output_json": output_json, "output_csv": output_csv})

            qualification.aggregate_command(args)

            result = json.loads(output_json.read_text(encoding="utf-8"))
            row = result["rows"][0]
            self.assertEqual(result["validation"], "INCOMPLETE_ARTIFACTS")
            self.assertEqual(result["summary"]["attempted"], 1)
            self.assertEqual(result["summary"]["reported"], 0)
            self.assertEqual(result["summary"]["historical34Projection"]["missingReports"], 2)
            self.assertEqual(row["reportAvailability"], "MISSING_REPORTS")
            self.assertEqual(row["processExitCode"], "17")
            self.assertEqual(row["missingReports"],
                             ["execution.json", "impact-v1.json", "execution.impact-v2.json"])
            self.assertIsNone(row["impactV2Status"])
            self.assertIsNone(row["impactV2CompleteScore"])
            self.assertIsNone(row["impactV2ObservedLowerBound"])
            self.assertEqual(row["impactV2FinalSnapshotRuleCrossCheck"], "UNKNOWN")
            vector = result["summary"]["byVector"]["00000"]
            self.assertEqual(vector["observedLowerBounds"], {})
            self.assertEqual(vector["categoryPositiveCountPatterns"], {})


if __name__ == "__main__":
    unittest.main()
