#!/usr/bin/env python3
"""Plan and aggregate the current ImpactV2 RemoveTournament/AddParticipant refresh."""

from __future__ import annotations

import argparse
import csv
import hashlib
import itertools
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

from validate_assessment import validate_reports

REMOVE = "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveTournamentFunctionalitySagas"
ADD = "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantFunctionalitySagas"
EXPECTED_STEPS = ["getTournamentStep", "removeQuizStep", "removeTournamentStep", "getUserStep", "addParticipantStep"]
STATUSES = ("COMPLETE", "PARTIAL", "INVALID", "UNAVAILABLE")


class QualificationError(ValueError):
    pass


def require(value: bool, message: str) -> None:
    if not value:
        raise QualificationError(message)


def load_json(path: Path) -> dict[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"))
    require(isinstance(value, dict), f"Expected object in {path}")
    return value


def load_jsonl(path: Path) -> list[dict[str, Any]]:
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]


def digest_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def digest(path: Path) -> str:
    return digest_bytes(path.read_bytes())


def compact(value: Any) -> str:
    return json.dumps(value, sort_keys=True, separators=(",", ":"))


def package(manifest_path: Path) -> dict[str, Any]:
    manifest_path = manifest_path.resolve()
    manifest = load_json(manifest_path)
    require(manifest.get("formatVersion") == 1, "Expected current executable manifest formatVersion 1")
    files = manifest.get("files")
    require(isinstance(files, dict), "Manifest files object is missing")
    records: dict[str, Any] = {}
    hashes: dict[str, str] = {"manifest": digest(manifest_path)}
    paths: dict[str, Path] = {}
    for role, descriptor in files.items():
        require(isinstance(descriptor, dict), f"Invalid manifest descriptor {role}")
        relative = descriptor.get("path")
        require(isinstance(relative, str) and Path(relative).name == relative, f"Unsafe package path for {role}")
        path = manifest_path.parent / relative
        require(path.is_file(), f"Missing package artifact {path}")
        actual = digest(path)
        require(actual == descriptor.get("sha256"), f"Hash mismatch for package role {role}")
        hashes[role] = actual
        paths[role] = path
        records[role] = load_json(path) if path.suffix == ".json" else load_jsonl(path)
    for required in ("sagas", "setups", "workloads", "faultScenarios", "requests", "accounting"):
        require(required in records, f"Current package lacks {required}")
    return {"manifestPath": manifest_path, "manifest": manifest, "paths": paths,
            "records": records, "hashes": hashes}


def select_workload(data: dict[str, Any]) -> dict[str, Any]:
    matches = []
    for workload in data["records"]["workloads"]:
        sagas = {participant.get("saga") for participant in workload.get("participants", [])}
        steps = [item.get("sagaStep", "").split("#", 1)[0] for item in workload.get("schedule", [])
                 if item.get("kind") == "step"]
        if sagas == {REMOVE, ADD} and steps == EXPECTED_STEPS \
                and all(item.get("kind") == "step" for item in workload.get("schedule", [])) \
                and not workload.get("interactions") \
                and isinstance(workload.get("setup"), str):
            matches.append(workload)
    require(len(matches) == 1, f"Expected one exact current benchmark workload, found {len(matches)}")
    workload = matches[0]
    setup_ids = {setup.get("id") for setup in data["records"]["setups"]}
    require(workload["setup"] in setup_ids, "Benchmark source setup is missing")
    return workload


def canonical_vectors(workload: dict[str, Any]) -> list[str]:
    steps = [item for item in workload["schedule"] if item.get("kind") == "step"]
    ownership: list[list[int]] = []
    for participant in ("p2", "p1"):
        ownership.append([item["faultSlot"] for item in steps if item["participant"] == participant])
    require(ownership == [[0, 1, 2], [3, 4]], f"Unexpected benchmark fault ownership {ownership}")
    choices = []
    for indexes in ownership:
        participant = ["00000"]
        for index in indexes:
            bits = ["0"] * 5
            bits[index] = "1"
            participant.append("".join(bits))
        choices.append(participant)
    return sorted("".join("1" if left[i] == "1" or right[i] == "1" else "0" for i in range(5))
                  for left, right in itertools.product(*choices))


def action_model(data: dict[str, Any], workload: dict[str, Any], scenario: dict[str, Any]) -> list[list[Any]]:
    roles = {participant["id"]: "REMOVE" if participant["saga"] == REMOVE else "ADD"
             for participant in workload["participants"]}
    steps = {item["id"]: item for item in workload["schedule"] if item.get("kind") == "step"}
    saga_steps: dict[tuple[str, str], dict[str, Any]] = {}
    for saga in data["records"]["sagas"]:
        for step in saga.get("steps", []):
            saga_steps[(saga["fqn"], step["id"])] = step
    result = []
    for action in scenario.get("actions", []):
        kind = "FORWARD" if "step" in action else "COMPENSATION"
        scheduled = steps[action.get("step") or action.get("compensate")]
        role = roles[scheduled["participant"]]
        runtime_step = scheduled["sagaStep"].split("#", 1)[0]
        evidence = None
        if kind == "COMPENSATION":
            saga_fqn = REMOVE if role == "REMOVE" else ADD
            stored = saga_steps[(saga_fqn, scheduled["sagaStep"])].get("compensation", {}).get("kind")
            evidence = {"implicitSagaRollback": "IMPLICIT_SAGA_ROLLBACK",
                        "explicitCompensation": "EXPLICIT_COMPENSATION"}.get(stored, stored)
        result.append([kind, role, runtime_step, evidence])
    return result


def structural_key(vector: str, actions: list[list[Any]]) -> str:
    return digest_bytes(compact({"vector": vector, "actions": actions}).encode())


def build_plan(manifest: Path, historical_path: Path) -> dict[str, Any]:
    data = package(manifest)
    workload = select_workload(data)
    vectors = canonical_vectors(workload)
    scenarios = [value for value in data["records"]["faultScenarios"] if value.get("workload") == workload["id"]]
    by_vector: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for scenario in scenarios:
        require(scenario.get("faultVector") in vectors, "Benchmark has noncanonical persisted vector")
        actions = action_model(data, workload, scenario)
        scenario = {**scenario, "normalizedActions": actions,
                    "structuralKeySha256": structural_key(scenario["faultVector"], actions)}
        by_vector[scenario["faultVector"]].append(scenario)
    require(set(by_vector) == set(vectors), f"Missing current canonical vectors: {sorted(set(vectors)-set(by_vector))}")
    for rows in by_vector.values():
        rows.sort(key=lambda row: (row["structuralKeySha256"], row["id"]))

    requests = {(item["faultVector"]): item for item in data["records"]["requests"]
                if item.get("workload") == workload["id"]}
    requested_vectors = {"00101", "00110", "01001", "01010", "10001", "10010"}
    require(requested_vectors <= set(requests), f"Missing on-demand requests: {sorted(requested_vectors-set(requests))}")
    for vector, request in requests.items():
        ids = request.get("faultScenarioIds", [])
        require(set(ids) == {row["id"] for row in by_vector[vector]} and len(ids) == len(by_vector[vector]),
                f"Request/scenario mismatch for {vector}")

    historical = load_json(historical_path)
    historical_rows = historical.get("rows", [])
    require(len(historical_rows) == 34, "Historical comparison must retain 34 rows")
    labels = Counter(row.get("label") for row in historical_rows)
    require(labels == {"HARMFUL_FOR_RULE": 19, "NO_BROKEN_REFERENCE": 15},
            f"Historical label split changed: {labels}")
    exact = defaultdict(list)
    for row in historical_rows:
        exact[(row["vector"], row["structuralKeySha256"])].append(row)
    projected = {(row["vector"], row["projectedStructuralKeySha256"]): row
                 for row in historical["projection"]["groups"]}
    require(sum(row["historicalRowCount"] for row in projected.values()) == 34,
            "Historical projection does not account for 34 rows")

    plan_rows = []
    for vector in vectors:
        for ordinal, scenario in enumerate(by_vector[vector], 1):
            key = (vector, scenario["structuralKeySha256"])
            exact_rows = exact.get(key, [])
            projection = projected.get(key)
            require(projection is not None, f"Current structural key has no historical conservative-action projection: {key}")
            plan_rows.append({
                "vector": vector, "currentOrdinal": ordinal, "faultScenarioId": scenario["id"],
                "structuralKeySha256": scenario["structuralKeySha256"],
                "normalizedActions": scenario["normalizedActions"],
                "exactHistoricalMatchCount": len(exact_rows),
                "projectedHistoricalRowCount": projection["historicalRowCount"],
                "projectedHistoricalLabel": projection["label"],
            })
    require(sum(row["projectedHistoricalRowCount"] for row in plan_rows) == 34,
            "Current projection does not account for every historical row")
    return {
        "schemaVersion": "microservices-simulator.impact-v2-broader-plan.v1",
        "package": {"manifestPath": str(data["manifestPath"]), "artifactSha256": data["hashes"]},
        "workloadPlanId": workload["id"], "sourceSetupId": workload["setup"],
        "faultSlots": [{"slot": item["faultSlot"], "participant": item["participant"],
                        "runtimeStep": item["sagaStep"].split("#", 1)[0]}
                       for item in workload["schedule"] if item.get("kind") == "step"],
        "canonicalVectors": [{"vector": vector, "currentScheduleCount": len(by_vector[vector]),
                              "historicalScheduleCount": sum(row["historicalRowCount"] for key, row in projected.items()
                                                               if key[0] == vector)} for vector in vectors],
        "summary": {"currentScheduleCount": len(plan_rows), "historicalScheduleCount": 34,
                    "exactMatchedCurrentKeys": sum(bool(row["exactHistoricalMatchCount"]) for row in plan_rows),
                    "currentKeysWithoutExactMatch": sum(not row["exactHistoricalMatchCount"] for row in plan_rows),
                    "historicalRowsWithoutExactMatch": 34 - sum(row["exactHistoricalMatchCount"] for row in plan_rows),
                    "projectedHistoricalRows": sum(row["projectedHistoricalRowCount"] for row in plan_rows),
                    "historicalLabels": dict(labels)},
        "rows": plan_rows,
    }


def plan_command(args: argparse.Namespace) -> None:
    plan = build_plan(args.manifest, args.historical)
    args.output_json.parent.mkdir(parents=True, exist_ok=True)
    args.output_json.write_text(json.dumps(plan, indent=2) + "\n", encoding="utf-8")
    args.output_tsv.write_text("".join(f"{row['vector']}\t{row['currentOrdinal']}\t{row['faultScenarioId']}\n"
                                       for row in plan["rows"]), encoding="utf-8")


def aggregate_command(args: argparse.Namespace) -> None:
    plan = load_json(args.plan)
    rows = []
    for selection in plan["rows"]:
        base = args.attempt_dir / selection["faultScenarioId"]
        paths = {"execution": base / "execution.json", "impactV1": base / "impact-v1.json",
                 "impactV2": base / "execution.impact-v2.json"}
        supporting_paths = {"processExitCode": base / "process-exit-code", "dockerLog": base / "docker.log"}
        rejected_observation = base / "result.json"
        if rejected_observation.is_file():
            supporting_paths["rejectedApplicationObservation"] = rejected_observation
        missing = [path.name for path in paths.values() if not path.is_file()]
        artifact_hashes = {name: digest(path) for name, path in {**paths, **supporting_paths}.items()
                           if path.is_file()}
        process_exit = ((base / "process-exit-code").read_text(encoding="utf-8").strip()
                        if (base / "process-exit-code").is_file() else None)
        if missing:
            rows.append({**selection, "reportAvailability": "MISSING_REPORTS",
                         "missingReports": missing, "processExitCode": process_exit,
                         "logPath": str(base / "docker.log"), "executionAttemptId": None,
                         "executionTerminalStatus": None, "scheduleConformance": None,
                         "impactV1Status": None, "impactV1Score": None, "impactV2Status": None,
                         "impactV2CompleteScore": None, "impactV2ObservedLowerBound": None,
                         "impactV2CategoryPositiveCounts": {},
                         "applicationObservationStatus": "UNAVAILABLE_CURRENT_PROVIDER_SETUP",
                         "applicationObservationReason": None,
                         "impactV2FinalSnapshotRuleCrossCheck": "UNKNOWN",
                         "artifactSha256": artifact_hashes})
            continue
        reports = {name: load_json(path) for name, path in paths.items()}
        execution, v1, v2 = (reports[name] for name in paths)
        scenario_id = selection["faultScenarioId"]
        attempt_id = execution.get("executionAttemptId")
        status = validate_reports(execution, v1, v2, plan["workloadPlanId"], scenario_id,
                                  selection["vector"])
        require(execution.get("faultScenarioId") == scenario_id
                and v2.get("faultScenarioId") == scenario_id, f"Scenario join mismatch for {scenario_id}")
        require(v1.get("executionAttemptId") == attempt_id and v2.get("executionAttemptId") == attempt_id,
                f"Attempt join mismatch for {scenario_id}")
        app_status = "UNAVAILABLE_CURRENT_PROVIDER_SETUP"
        app_reason = None
        if rejected_observation.is_file():
            rejected = load_json(rejected_observation)
            app_reason = rejected.get("validityReason")
            require(rejected.get("executionAttemptId") is None and rejected.get("classification") == "NOT_EVALUATED",
                    "Rejected application observation must not claim a measured verdict")
        final_by_identity = {(item.get("identity", {}).get("aggregateType"),
                              item.get("identity", {}).get("aggregateId")): item
                             for item in v2.get("finalState", [])}
        broken = False
        seen_tournament = False
        for identity, tournament in final_by_identity.items():
            if identity[0] != "SagaTournament":
                continue
            seen_tournament = True
            if tournament.get("lifecycleState") != "ACTIVE":
                continue
            for dependency in tournament.get("dependencies", []):
                target = dependency.get("target", {})
                if target.get("aggregateType") == "SagaQuiz":
                    quiz = final_by_identity.get(("SagaQuiz", target.get("aggregateId")))
                    if quiz is not None and quiz.get("lifecycleState") == "DELETED":
                        broken = True
        final_cross_check = ("BROKEN_REFERENCE_PRESENT" if broken else
                             "NO_BROKEN_REFERENCE" if seen_tournament else "UNKNOWN")
        rows.append({**selection, "reportAvailability": "PRESENT", "missingReports": [],
                     "processExitCode": process_exit, "logPath": str(base / "docker.log"),
                     "executionAttemptId": attempt_id,
                     "executionTerminalStatus": execution.get("terminalStatus"),
                     "scheduleConformance": execution.get("scheduleConformance"),
                     "impactV1Status": v1.get("evaluationStatus"), "impactV1Score": v1.get("impactScore"),
                     "impactV2Status": status, "impactV2CompleteScore": v2.get("completeScore"),
                     "impactV2ObservedLowerBound": v2.get("observedAffectedObjectCount"),
                     "impactV2CategoryPositiveCounts": {
                         category["category"]: category["positiveObjectCount"]
                         for category in v2.get("categoryResults", [])},
                     "applicationObservationStatus": app_status,
                     "applicationObservationReason": app_reason,
                     "impactV2FinalSnapshotRuleCrossCheck": final_cross_check,
                     "artifactSha256": artifact_hashes})
    reported = [row for row in rows if row["reportAvailability"] == "PRESENT"]
    require(len({row["executionAttemptId"] for row in reported}) == len(reported),
            "Duplicate execution attempts")
    status_counts = Counter(row["impactV2Status"] for row in reported)
    by_vector = {}
    for vector in sorted({row["vector"] for row in rows}):
        selected = [row for row in rows if row["vector"] == vector]
        by_vector[vector] = {
            "currentRows": len(selected),
            "reportedRows": sum(row["reportAvailability"] == "PRESENT" for row in selected),
            "missingReports": sum(row["reportAvailability"] == "MISSING_REPORTS" for row in selected),
            "impactV2Status": dict(Counter(row["impactV2Status"] for row in selected
                                             if row["impactV2Status"] is not None)),
            "completeScores": dict(Counter(str(row["impactV2CompleteScore"]) for row in selected
                                             if row["impactV2Status"] == "COMPLETE")),
            "observedLowerBounds": dict(Counter(str(row["impactV2ObservedLowerBound"]) for row in selected
                                                   if row["impactV2ObservedLowerBound"] is not None)),
            "categoryPositiveCountPatterns": dict(Counter(compact(row["impactV2CategoryPositiveCounts"])
                                                           for row in selected
                                                           if row["reportAvailability"] == "PRESENT")),
        }
    weighted_status = Counter()
    weighted_scores = Counter()
    weighted_lower_bounds = Counter()
    weighted_cross_check = Counter()
    weighted_label_cross_check = Counter()
    for row in rows:
        weight = row["projectedHistoricalRowCount"]
        if row["impactV2Status"] is not None:
            weighted_status[row["impactV2Status"]] += weight
        if row["impactV2ObservedLowerBound"] is not None:
            weighted_lower_bounds[str(row["impactV2ObservedLowerBound"])] += weight
        weighted_cross_check[row["impactV2FinalSnapshotRuleCrossCheck"]] += weight
        weighted_label_cross_check[f"{row['projectedHistoricalLabel']} -> {row['impactV2FinalSnapshotRuleCrossCheck']}"] += weight
        if row["impactV2Status"] == "COMPLETE":
            weighted_scores[str(row["impactV2CompleteScore"])] += weight
    weighted_missing = sum(row["projectedHistoricalRowCount"] for row in rows
                           if row["reportAvailability"] == "MISSING_REPORTS")
    result = {"schemaVersion": "microservices-simulator.impact-v2-broader-results.v1",
              "validation": "PASS" if len(reported) == len(rows) else "INCOMPLETE_ARTIFACTS",
              "planSha256": digest(args.plan), "summary": {"attempted": len(rows),
              "reported": len(reported), "missingReports": len(rows) - len(reported),
              "executed": len(reported),
              "impactV2Status": {status: status_counts.get(status, 0) for status in STATUSES},
              "completeScores": dict(Counter(str(row["impactV2CompleteScore"]) for row in rows
                                               if row["impactV2Status"] == "COMPLETE")),
              "applicationObservations": dict(Counter(row["applicationObservationStatus"] for row in rows)),
              "impactV2FinalSnapshotRuleCrossCheck": dict(Counter(
                  row["impactV2FinalSnapshotRuleCrossCheck"] for row in rows)),
              "byVector": by_vector,
              "historical34Projection": {
                  "note": "Weighted projection onto current structural keys; these are not 34 new executions",
                  "rowsRepresented": sum(row["projectedHistoricalRowCount"] for row in rows),
                  "missingReports": weighted_missing,
                  "impactV2Status": dict(weighted_status),
                  "completeScores": dict(weighted_scores),
                  "observedLowerBounds": dict(weighted_lower_bounds),
                  "finalSnapshotRuleCrossCheck": dict(weighted_cross_check),
                  "historicalLabelToCurrentCrossCheck": dict(weighted_label_cross_check)},
              "historicalComparisonBoundary": "labels compared after execution; projected labels never entered scoring"},
              "rows": rows}
    args.output_json.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    columns = ["vector", "currentOrdinal", "faultScenarioId", "reportAvailability",
               "missingReports", "processExitCode", "logPath", "executionAttemptId",
               "executionTerminalStatus", "scheduleConformance", "impactV1Status", "impactV1Score",
               "impactV2Status", "impactV2CompleteScore", "impactV2ObservedLowerBound",
               "applicationObservationStatus", "impactV2FinalSnapshotRuleCrossCheck",
               "exactHistoricalMatchCount", "projectedHistoricalRowCount",
               "projectedHistoricalLabel", "structuralKeySha256"]
    with args.output_csv.open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=columns)
        writer.writeheader()
        writer.writerows({column: row.get(column) for column in columns} for row in rows)


def parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser(description=__doc__)
    commands = root.add_subparsers(dest="command", required=True)
    plan = commands.add_parser("plan")
    plan.add_argument("--manifest", type=Path, required=True)
    plan.add_argument("--historical", type=Path, required=True)
    plan.add_argument("--output-json", type=Path, required=True)
    plan.add_argument("--output-tsv", type=Path, required=True)
    plan.set_defaults(handler=plan_command)
    aggregate = commands.add_parser("aggregate")
    aggregate.add_argument("--plan", type=Path, required=True)
    aggregate.add_argument("--attempt-dir", type=Path, required=True)
    aggregate.add_argument("--output-json", type=Path, required=True)
    aggregate.add_argument("--output-csv", type=Path, required=True)
    aggregate.set_defaults(handler=aggregate_command)
    return root


def main() -> int:
    try:
        args = parser().parse_args()
        args.handler(args)
        return 0
    except (ValueError, OSError, json.JSONDecodeError, KeyError) as failure:
        print(f"ImpactV2 broader qualification failed: {failure}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
