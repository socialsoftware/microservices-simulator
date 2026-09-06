#!/usr/bin/env python3
"""Small read-only regression checks for broader-qualification helpers."""

from __future__ import annotations

import copy
import hashlib
import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
EXPERIMENT = ROOT / "verifiers" / "experiments" / "impact-v2-broader"
sys.path.insert(0, str(EXPERIMENT))

from validate_assessment import CATEGORIES, validate_reports  # noqa: E402
from qualification import package as load_package, select_workload  # noqa: E402


def load_selector():
    path = EXPERIMENT / "select-broader.py"
    spec = importlib.util.spec_from_file_location("impact_v2_broader_selector", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Cannot load selector from {path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def load_summarizer():
    path = EXPERIMENT / "summarize-broader.py"
    spec = importlib.util.spec_from_file_location("impact_v2_broader_summarizer", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Cannot load summarizer from {path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def current_manifest():
    manifests = sorted(
        (ROOT / "verifiers" / "target" / "impact-v2-broader" / "generated").glob(
            "*/scenario-catalog-manifest.json"
        )
    )
    if not manifests:
        raise AssertionError("The frozen broader qualification package is unavailable")
    return manifests[-1]


def selected_row(vector="0", role="control"):
    return {
        "caseId": "missing-case",
        "pairId": "missing-pair",
        "workloadId": "workload-1",
        "faultScenarioId": "scenario-1",
        "faultVector": vector,
        "role": role,
        "saga": "fixture.Saga",
        "shape": "basic",
    }


class BroaderQualificationChecks(unittest.TestCase):
    def test_validator_accepts_complete_report_and_rejects_identity_or_score_mutations(self):
        execution, v1, v2 = complete_reports()
        self.assertEqual(
            validate_reports(execution, v1, v2, "workload-1", "scenario-1", "0"),
            "COMPLETE",
        )

        mutations = (
            (lambda value: value.update(workloadPlanId="other-workload"), execution),
            (lambda value: value.update(assignedVector="1"), execution),
            (lambda value: value.update(executionAttemptId="other-attempt"), v1),
            (lambda value: value.update(completeScore=0), v2),
            (lambda value: value.update(completeScore=None), v2),
            (lambda value: value.update(observedAffectedObjectCount=None), v2),
        )
        for mutate, original in mutations:
            candidate = copy.deepcopy(original)
            mutate(candidate)
            candidate_execution = candidate if original is execution else execution
            candidate_v1 = candidate if original is v1 else v1
            candidate_v2 = candidate if original is v2 else v2
            with self.subTest(mutation=mutate):
                with self.assertRaises(ValueError):
                    validate_reports(
                        candidate_execution,
                        candidate_v1,
                        candidate_v2,
                        "workload-1",
                        "scenario-1",
                        "0",
                    )

    def test_selector_is_deterministic_and_selects_one_event_shape(self):
        selector = load_selector()
        with tempfile.TemporaryDirectory(prefix="impact-v2-selector-") as directory:
            package = Path(directory)
            (package / "scenario-catalog-manifest.json").write_text("{}\n", encoding="utf-8")

            saga = "com.example.DemoFunctionalitySagas"
            (package / "sagas.jsonl").write_text(
                json.dumps({"fqn": saga}) + "\n", encoding="utf-8"
            )
            (package / "setups.jsonl").write_text(
                "\n".join(
                    (
                        json.dumps({"id": "setup-basic", "materializable": True, "actions": [{}]}),
                        json.dumps({"id": "setup-events", "materializable": True, "actions": [{}, {}]}),
                    )
                )
                + "\n",
                encoding="utf-8",
            )
            workloads = [
                {
                    "id": "workload-events",
                    "participants": [{"id": "p1", "saga": saga}],
                    "setup": "setup-events",
                    "schedule": [
                        {"id": "s1", "kind": "step", "faultSlot": 0},
                        {"id": "e1", "kind": "event"},
                    ],
                },
                {
                    "id": "workload-basic",
                    "participants": [{"id": "p1", "saga": saga}],
                    "setup": "setup-basic",
                    "schedule": [{"id": "s1", "kind": "step", "faultSlot": 0}],
                },
            ]
            (package / "workloads.jsonl").write_text(
                "".join(json.dumps(row) + "\n" for row in workloads), encoding="utf-8"
            )
            scenarios = [
                {"id": "z-events-fault", "workload": "workload-events", "faultVector": "1"},
                {"id": "a-basic-fault", "workload": "workload-basic", "faultVector": "1"},
                {"id": "z-basic-control", "workload": "workload-basic", "faultVector": "0"},
                {"id": "a-events-control", "workload": "workload-events", "faultVector": "0"},
            ]
            (package / "fault-scenarios.jsonl").write_text(
                "".join(json.dumps(row) + "\n" for row in scenarios), encoding="utf-8"
            )

            first = selector.select(package)
            second = selector.select(package)
            self.assertEqual(first, second)
            self.assertEqual(first["sagaCount"], 1)
            self.assertEqual(first["eligibleSagaCount"], 1)
            self.assertEqual(len(first["rows"]), 4)
            self.assertEqual(
                {(row["shape"], row["role"]) for row in first["rows"]},
                {("basic", "control"), ("basic", "late-fault"),
                 ("events", "control"), ("events", "late-fault")},
            )
            self.assertEqual(
                {row["faultScenarioId"] for row in first["rows"]},
                {"z-basic-control", "a-basic-fault", "a-events-control", "z-events-fault"},
            )

    def test_summary_rejects_wrong_package_hash_and_role_vector_binding(self):
        summarizer = load_summarizer()
        manifest = current_manifest()
        package_hash = hashlib.sha256(manifest.read_bytes()).hexdigest()
        with tempfile.TemporaryDirectory(prefix="impact-v2-summary-") as directory:
            root = Path(directory)
            attempts = root / "attempts"
            attempts.mkdir()

            def write_selection(name, package_manifest_hash, row):
                selection = {
                    "packageManifestSha256": package_manifest_hash,
                    "sagaCount": 1,
                    "eligibleSagaCount": 1,
                    "excludedSagas": [],
                    "rows": [row],
                }
                path = root / name
                path.write_text(json.dumps(selection) + "\n", encoding="utf-8")
                return path

            wrong_hash = write_selection("wrong-hash.json", "0" * 64, selected_row())
            with self.assertRaises(ValueError):
                summarizer.summarize(wrong_hash, attempts, manifest)

            wrong_vector = write_selection(
                "wrong-vector.json", package_hash, selected_row(vector="1", role="control")
            )
            with self.assertRaises(ValueError):
                summarizer.summarize(wrong_vector, attempts, manifest)

    def test_historical_benchmark_rejects_event_in_exact_workload(self):
        manifest = current_manifest()
        data = load_package(manifest)
        exact = select_workload(data)
        candidate = copy.deepcopy(data)
        candidate["records"]["workloads"] = [copy.deepcopy(exact)]
        candidate["records"]["workloads"][0]["schedule"].append(
            {"id": "synthetic-event", "kind": "event"}
        )

        with self.assertRaises(ValueError):
            select_workload(candidate)

    def test_summary_preserves_missing_reports_as_null_incomplete_artifact(self):
        summarizer = load_summarizer()
        manifest = current_manifest()
        package_hash = hashlib.sha256(manifest.read_bytes()).hexdigest()
        with tempfile.TemporaryDirectory(prefix="impact-v2-missing-") as directory:
            root = Path(directory)
            attempts = root / "attempts"
            attempts.mkdir()
            selection = {
                "packageManifestSha256": package_hash,
                "sagaCount": 1,
                "eligibleSagaCount": 1,
                "excludedSagas": [],
                "rows": [
                    selected_row(),
                    {
                        **selected_row(vector="1", role="late-fault"),
                        "caseId": "missing-case-fault",
                    },
                ],
            }
            selection_path = root / "selection.json"
            selection_path.write_text(json.dumps(selection) + "\n", encoding="utf-8")

            result = summarizer.summarize(selection_path, attempts, manifest)

            self.assertEqual(result["validation"], "INCOMPLETE_ARTIFACTS")
            self.assertEqual(result["summary"]["attempted"], 2)
            self.assertEqual(result["summary"]["reported"], 0)
            self.assertEqual(result["summary"]["missingReports"], 2)
            row = result["rows"][0]
            self.assertEqual(row["reportAvailability"], "MISSING_REPORTS")
            self.assertIsNone(row["executionAttemptId"])
            self.assertIsNone(row["executionTerminalStatus"])
            self.assertIsNone(row["impactV2Status"])
            self.assertIsNone(row["impactV2CompleteScore"])
            self.assertIsNone(row["impactV2ObservedLowerBound"])
            self.assertEqual(
                row["missingReports"],
                ["execution.json", "impact-v1.json", "execution.impact-v2.json"],
            )


def complete_reports():
    identity = {"aggregateType": "FixtureAggregate", "aggregateId": 7}
    categories = []
    for category in CATEGORIES:
        finding = []
        candidates = []
        positive = 0
        if category == CATEGORIES[0]:
            candidates = [{"aggregate": identity, "eventId": None}]
            finding = [{"category": category, "affectedObject": identity}]
            positive = 1
        categories.append(
            {
                "category": category,
                "coverageStatus": "COMPLETE",
                "candidateCount": len(candidates),
                "candidates": candidates,
                "positiveObjectCount": positive,
                "findings": finding,
                "unknownReasons": [],
            }
        )
    execution = {
        "executionAttemptId": "attempt-1",
        "packageManifestPath": "/reports/package/scenario-catalog-manifest.json",
        "workloadPlanId": "workload-1",
        "faultScenarioId": "scenario-1",
        "assignedVector": "0",
        "terminalStatus": "SUCCESS",
        "scheduleConformance": "EXACT",
    }
    v1 = {
        "executionAttemptId": "attempt-1",
        "workloadPlanId": "workload-1",
        "faultScenarioId": "scenario-1",
    }
    v2 = {
        "schemaVersion": "microservices-simulator.scenario-impact-v2-assessment.v1",
        "packageManifestPath": execution["packageManifestPath"],
        "executionAttemptId": "attempt-1",
        "workloadPlanId": "workload-1",
        "faultScenarioId": "scenario-1",
        "executionTerminalStatus": "SUCCESS",
        "scheduleConformance": "EXACT",
        "assessmentStatus": "COMPLETE",
        "horizon": "FINAL_SCHEDULED_ACTION",
        "collectionStatus": "OBSERVED",
        "coverageGaps": [],
        "completeScore": 1,
        "observedAffectedObjectCount": 1,
        "categoryResults": categories,
    }
    return execution, v1, v2


if __name__ == "__main__":
    unittest.main()
