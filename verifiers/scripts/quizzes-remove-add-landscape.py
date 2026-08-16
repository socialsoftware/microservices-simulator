#!/usr/bin/env python3
"""Plan and validate the bounded Quizzes RemoveTournament/AddParticipant landscape."""

from __future__ import annotations

import argparse
import csv
import hashlib
import itertools
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ATTEMPT_SCHEMA = "microservices-simulator.quizzes-remove-add-benchmark-attempt.v3"
EXECUTION_SCHEMA = "microservices-simulator.scenario-execution-report.v5"
IMPACT_SCHEMA = "microservices-simulator.scenario-impact-report.v1"
LANDSCAPE_SCHEMA = "microservices-simulator.quizzes-remove-add-benchmark-landscape.v2"
BENCHMARK_ID = "quizzes-remove-tournament-add-participant"
OBSERVATION_RULE = "An evaluated execution is harmful when an active Tournament still refers to a deleted Quiz."
WORKLOAD_SCHEMA = "microservices-simulator.workload-plan.v4"
SCENARIO_SCHEMA = "microservices-simulator.fault-scenario.v4"
EXPECTED_STEPS = [
    "getTournamentStep",
    "removeQuizStep",
    "removeTournamentStep",
    "getUserStep",
    "addParticipantStep",
]
EXPECTED_SAGAS = [
    "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveTournamentFunctionalitySagas",
    "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantFunctionalitySagas",
]
GENERIC_SAGA_STATE_CLASS = (
    "pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState"
)
SAGA_STATE_CONVERTER = (
    "pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter"
)
EXPECTED_RAW_NOT_IN_SAGA = f"{GENERIC_SAGA_STATE_CLASS}:NOT_IN_SAGA"


class LandscapeError(ValueError):
    pass


def require(condition: bool, message: str) -> None:
    if not condition:
        raise LandscapeError(message)


def load_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text())
    except (OSError, json.JSONDecodeError) as failure:
        raise LandscapeError(f"Cannot read JSON {path}: {failure}") from failure
    require(isinstance(value, dict), f"Expected one JSON object in {path}")
    return value


def load_jsonl(path: Path) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    try:
        for line_number, line in enumerate(path.read_text().splitlines(), start=1):
            if not line.strip():
                continue
            value = json.loads(line)
            require(isinstance(value, dict), f"Expected object at {path}:{line_number}")
            records.append(value)
    except (OSError, json.JSONDecodeError) as failure:
        raise LandscapeError(f"Cannot read JSONL {path}: {failure}") from failure
    return records


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    try:
        with path.open("rb") as source:
            for block in iter(lambda: source.read(1024 * 1024), b""):
                digest.update(block)
    except OSError as failure:
        raise LandscapeError(f"Cannot hash {path}: {failure}") from failure
    return digest.hexdigest()


def artifact_path(package_dir: Path, descriptor: dict[str, Any], expected_name: str) -> Path:
    declared = descriptor.get("path")
    require(isinstance(declared, str), f"Manifest {expected_name} path is missing")
    path = package_dir / Path(declared).name
    require(path.name == expected_name, f"Manifest points to unexpected {expected_name} path: {declared}")
    require(path.is_file(), f"Package artifact is missing: {path}")
    require(sha256(path) == descriptor.get("sha256"), f"Package artifact hash mismatch: {path}")
    return path


def canonical_vectors(slots: list[dict[str, Any]]) -> tuple[list[str], list[dict[str, Any]]]:
    require(len(slots) == 5, "Benchmark WorkloadPlan must have five fault slots")
    participants: list[str] = []
    indexes_by_participant: dict[str, list[int]] = {}
    for index, slot in enumerate(slots):
        require(slot.get("slotIndex") == index, "Fault slots must be in contiguous persisted order")
        participant = slot.get("sagaInstanceId")
        require(isinstance(participant, str), f"Fault slot {index} has no participant identity")
        if participant not in indexes_by_participant:
            participants.append(participant)
            indexes_by_participant[participant] = []
        indexes_by_participant[participant].append(index)
    require([len(indexes_by_participant[p]) for p in participants] == [3, 2],
            "Benchmark fault-slot ownership must be 3 RemoveTournament slots then 2 AddParticipant slots")
    require(indexes_by_participant[participants[0]] == [0, 1, 2]
            and indexes_by_participant[participants[1]] == [3, 4],
            "Benchmark participant slots must remain contiguous in persisted order")

    choices: list[list[str]] = []
    for participant in participants:
        indexes = indexes_by_participant[participant]
        participant_choices = ["0" * len(slots)]
        for index in indexes:
            bits = ["0"] * len(slots)
            bits[index] = "1"
            participant_choices.append("".join(bits))
        choices.append(participant_choices)
    canonical = sorted("".join("1" if left[i] == "1" or right[i] == "1" else "0"
                               for i in range(len(slots)))
                       for left, right in itertools.product(*choices))

    excluded: list[dict[str, Any]] = []
    canonical_set = set(canonical)
    for bits in itertools.product("01", repeat=len(slots)):
        vector = "".join(bits)
        if vector in canonical_set:
            continue
        masking: list[dict[str, Any]] = []
        for participant in participants:
            assigned = [index for index in indexes_by_participant[participant] if bits[index] == "1"]
            if len(assigned) > 1:
                first = assigned[0]
                masking.append({
                    "sagaInstanceId": participant,
                    "firstFaultSlotIndex": first,
                    "firstFaultStep": slots[first]["runtimeStepName"],
                    "maskedFaultSlotIndexes": assigned[1:],
                    "maskedFaultSteps": [slots[index]["runtimeStepName"] for index in assigned[1:]],
                })
        require(masking, f"Excluded vector {vector} has no same-participant masking explanation")
        excluded.append({"assignedVector": vector, "masking": masking})
    require(len(canonical) == 12, f"Expected 12 canonical vectors, found {len(canonical)}")
    require(len(excluded) == 20, f"Expected 20 excluded vectors, found {len(excluded)}")
    return canonical, excluded


def package_model(manifest_path: Path, workload_plan_id: str) -> dict[str, Any]:
    manifest_path = manifest_path.resolve()
    manifest = load_json(manifest_path)
    package_dir = manifest_path.parent
    workload_path = artifact_path(package_dir, manifest.get("workloadCatalog", {}), "workload-catalog.jsonl")
    scenario_path = artifact_path(package_dir, manifest.get("faultScenarioCatalog", {}), "fault-scenario-catalog.jsonl")
    accounting_path = artifact_path(package_dir, manifest.get("scenarioSpaceAccounting", {}),
                                    "scenario-space-accounting.json")

    workloads = load_jsonl(workload_path)
    require(str(len(workloads)) == manifest["workloadCatalog"].get("recordCount"),
            "Workload catalog record count does not match the manifest")
    selected = [record for record in workloads if record.get("deterministicId") == workload_plan_id]
    require(len(selected) == 1, f"Expected one WorkloadPlan {workload_plan_id}, found {len(selected)}")
    workload = selected[0]
    require(workload.get("schemaVersion") == WORKLOAD_SCHEMA, "Benchmark WorkloadPlan must use package v4")
    require([step.get("runtimeStepName") for step in workload.get("forwardSchedule", [])] == EXPECTED_STEPS,
            "Benchmark WorkloadPlan forward order does not match RemoveTournament/AddParticipant")
    require([slot.get("runtimeStepName") for slot in workload.get("faultSlots", [])] == EXPECTED_STEPS,
            "Benchmark WorkloadPlan fault-slot order does not match RemoveTournament/AddParticipant")
    require(not workload.get("eventConsequences"), "Benchmark WorkloadPlan must have no event route")
    fqn_by_id = {participant.get("deterministicId"): participant.get("sagaFqn")
                 for participant in workload.get("participants", [])}
    slot_sagas: list[str] = []
    for slot in workload["faultSlots"]:
        saga = fqn_by_id.get(slot.get("sagaInstanceId"))
        if saga not in slot_sagas:
            slot_sagas.append(saga)
    require(slot_sagas == EXPECTED_SAGAS, "Benchmark participant order or Saga types do not match")
    require(workload.get("prerequisiteBaseline", {}).get("providerId") == "quizzes-stale-read-baseline",
            "Benchmark WorkloadPlan has the wrong prerequisite provider")

    canonical, excluded = canonical_vectors(workload["faultSlots"])
    all_scenarios = load_jsonl(scenario_path)
    require(str(len(all_scenarios)) == manifest["faultScenarioCatalog"].get("recordCount"),
            "FaultScenario catalog record count does not match the manifest")
    scenarios = [record for record in all_scenarios if record.get("workloadPlanId") == workload_plan_id]
    scenario_ids = [record.get("deterministicId") for record in scenarios]
    require(len(scenario_ids) == len(set(scenario_ids)), "Package has duplicate FaultScenario identities")
    require(all(record.get("schemaVersion") == SCENARIO_SCHEMA for record in scenarios),
            "Benchmark FaultScenarios must use package v4")
    require(all(record.get("assignedVector") in canonical for record in scenarios),
            "Benchmark package contains a persisted non-canonical vector")

    accounting = load_json(accounting_path)
    rows = [row for row in accounting.get("faultScenarioCatalogSpace", {})
            .get("perComputedVectorRecoverySpace", [])
            if row.get("workloadPlanId") == workload_plan_id]
    rows_by_vector = {row.get("assignedVector"): row for row in rows}
    require(len(rows_by_vector) == len(rows), "Accounting has duplicate benchmark vector rows")
    require(set(rows_by_vector) == set(canonical),
            f"Accounting vectors do not equal the 12 canonical vectors: {sorted(rows_by_vector)}")
    scenarios_by_vector: dict[str, list[dict[str, Any]]] = {vector: [] for vector in canonical}
    for scenario in scenarios:
        scenarios_by_vector[scenario["assignedVector"]].append(scenario)
    for vector in canonical:
        scenarios_by_vector[vector].sort(key=lambda scenario: (
            tuple(action.get("deterministicId") for action in scenario.get("actions", [])),
            scenario["deterministicId"],
        ))
        persisted = int(rows_by_vector[vector]["writtenScheduleCount"])
        uncapped = int(rows_by_vector[vector]["uncappedUniqueScheduleCount"])
        require(persisted == len(scenarios_by_vector[vector]),
                f"Persisted scenario count for {vector} disagrees with accounting")
        require(persisted <= uncapped, f"Persisted count exceeds uncapped count for {vector}")
        require(persisted <= int(manifest.get("recoveryScheduleCap")),
                f"Persisted count exceeds package recovery cap for {vector}")
    require(len(scenarios) == sum(int(row["writtenScheduleCount"]) for row in rows),
            "Benchmark FaultScenario total disagrees with accounting")

    return {
        "manifestPath": manifest_path,
        "manifest": manifest,
        "manifestSha256": sha256(manifest_path),
        "workload": workload,
        "canonicalVectors": canonical,
        "excludedVectors": excluded,
        "scenariosByVector": scenarios_by_vector,
        "accountingByVector": rows_by_vector,
    }


def plan(args: argparse.Namespace) -> None:
    model = package_model(args.manifest, args.workload_plan_id)
    lines: list[str] = []
    for vector in model["canonicalVectors"]:
        for ordinal, scenario in enumerate(model["scenariosByVector"][vector], start=1):
            lines.append(f"{vector}\t{ordinal}\t{scenario['deterministicId']}")
    output = "\n".join(lines) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(output)
    else:
        sys.stdout.write(output)


def action_order(scenario: dict[str, Any]) -> list[str]:
    return [action.get("deterministicId") for action in scenario.get("actions", [])]


def read_attempt(attempt_dir: Path, scenario: dict[str, Any], vector: str, ordinal: int,
                 model: dict[str, Any]) -> dict[str, Any]:
    scenario_id = scenario["deterministicId"]
    base = attempt_dir / scenario_id
    result_path = base / "result.json"
    execution_path = base / "execution.json"
    impact_path = base / "impact.json"
    for path in (result_path, execution_path, impact_path):
        require(path.is_file(), f"Missing landscape artifact for {scenario_id}: {path}")
    result = load_json(result_path)
    execution = load_json(execution_path)
    impact = load_json(impact_path)

    require(result.get("schemaVersion") == ATTEMPT_SCHEMA, f"Wrong attempt schema for {scenario_id}")
    require(execution.get("schemaVersion") == EXECUTION_SCHEMA, f"Wrong execution schema for {scenario_id}")
    require(impact.get("schemaVersion") == IMPACT_SCHEMA, f"Wrong ImpactV1 schema for {scenario_id}")
    require(result.get("benchmarkId") == BENCHMARK_ID, f"Wrong benchmark id for {scenario_id}")
    require(result.get("observationRule") == OBSERVATION_RULE, f"Wrong observation rule for {scenario_id}")
    require(result.get("packageManifestSha256") == model["manifestSha256"],
            f"Attempt {scenario_id} used a different package manifest")
    require(result.get("workloadPlanId") == model["workload"]["deterministicId"],
            f"Attempt {scenario_id} used a different WorkloadPlan")
    require(result.get("faultScenarioId") == scenario_id, f"Attempt result scenario mismatch for {scenario_id}")
    require(result.get("assignedVector") == vector, f"Attempt vector mismatch for {scenario_id}")
    require(result.get("recoveryScheduleIdentity") == scenario_id,
            f"Attempt recovery identity mismatch for {scenario_id}")
    require([action.get("actionId") for action in result.get("persistedActionOrder", [])]
            == action_order(scenario), f"Attempt persisted action order mismatch for {scenario_id}")
    require(result.get("repetition") == 1, f"Landscape attempt repetition must be 1 for {scenario_id}")
    runtime = result.get("runtimeContext") or {}
    require(runtime.get("resetBoundary") == "FRESH_PROCESS_AND_H2",
            f"Attempt did not record fresh process/H2 reset for {scenario_id}")
    require(runtime.get("database") == "H2_IN_MEMORY", f"Attempt did not use H2 for {scenario_id}")
    require(runtime.get("springProfiles") == "test,sagas,local", f"Attempt profiles differ for {scenario_id}")

    attempt_id = result.get("executionAttemptId")
    require(isinstance(attempt_id, str) and attempt_id, f"Attempt id missing for {scenario_id}")
    for artifact, name in ((execution, "execution"), (impact, "impact")):
        require(artifact.get("executionAttemptId") == attempt_id,
                f"{name} attempt identity mismatch for {scenario_id}")
        require(artifact.get("workloadPlanId") == result.get("workloadPlanId"),
                f"{name} WorkloadPlan mismatch for {scenario_id}")
        require(artifact.get("faultScenarioId") == scenario_id,
                f"{name} FaultScenario mismatch for {scenario_id}")
    require(execution.get("assignedVector") == vector, f"Execution vector mismatch for {scenario_id}")
    require([action.get("actionId") for action in execution.get("plannedActions", [])]
            == action_order(scenario), f"Execution planned order mismatch for {scenario_id}")
    require(result.get("executionTerminalStatus") == execution.get("terminalStatus"),
            f"Execution terminal status mismatch for {scenario_id}")
    require(result.get("scheduleConformance") == execution.get("scheduleConformance"),
            f"Schedule conformance mismatch for {scenario_id}")
    result_impact = result.get("impactV1") or {}
    require(result_impact.get("evaluationStatus") == impact.get("evaluationStatus"),
            f"Impact status mismatch for {scenario_id}")
    require(result_impact.get("findingCount") == impact.get("invariantViolationCount"),
            f"Impact finding count mismatch for {scenario_id}")
    require(result_impact.get("score") == impact.get("impactScore"),
            f"Impact score mismatch for {scenario_id}")

    validity = result.get("validity")
    classification = result.get("classification")
    broken = result.get("brokenReference")
    if validity == "VALID":
        require(execution.get("terminalStatus") in ("SUCCESS", "COMPENSATED", "PARTIAL_COMPENSATED"),
                f"Valid attempt has a non-evaluable terminal status for {scenario_id}")
        require(impact.get("evaluationStatus") == "EVALUATED",
                f"Valid attempt has non-evaluated ImpactV1 for {scenario_id}")
        require(result.get("observationCompleted") is True, f"Valid attempt lacks observation for {scenario_id}")
        tournament = result.get("tournament")
        quiz = result.get("referencedQuiz")
        require(isinstance(tournament, dict) and isinstance(quiz, dict),
                f"Valid attempt lacks observed aggregates for {scenario_id}")
        prerequisite_evidence = (execution.get("prerequisiteSetup") or {}).get("evidence") or {}
        require(str(tournament.get("aggregateId")) == prerequisite_evidence.get("tournamentAggregateId"),
                f"Observed Tournament identity disagrees with setup evidence for {scenario_id}")
        require(str(quiz.get("aggregateId")) == prerequisite_evidence.get("referencedQuizAggregateId"),
                f"Observed Quiz identity disagrees with setup evidence for {scenario_id}")
        persisted_saga_state = tournament.get("persistedSagaState")
        require(isinstance(persisted_saga_state, dict),
                f"Valid attempt lacks raw persisted Tournament Saga-state evidence for {scenario_id}")
        require(persisted_saga_state.get("aggregateId") == tournament.get("aggregateId")
                and isinstance(persisted_saga_state.get("aggregateVersion"), int)
                and persisted_saga_state.get("rawValue") == EXPECTED_RAW_NOT_IN_SAGA
                and persisted_saga_state.get("valueStatus") == "VALUE"
                and persisted_saga_state.get("decodeStatus") == "DECODED"
                and persisted_saga_state.get("decodedStateClass") == GENERIC_SAGA_STATE_CLASS
                and persisted_saga_state.get("decodedStateName") == "NOT_IN_SAGA"
                and persisted_saga_state.get("decodeFailure") is None
                and persisted_saga_state.get("decoder") == SAGA_STATE_CONVERTER
                and persisted_saga_state.get("source") == "RAW_DATABASE_COLUMN"
                and persisted_saga_state.get("storageTable") == "saga_tournament"
                and persisted_saga_state.get("storageColumn") == "saga_state"
                and persisted_saga_state.get("rowSelection") == "LATEST_AGGREGATE_VERSION",
                f"Tournament is not proven outside an active Saga by converter-decoded latest-row evidence for {scenario_id}")
        computed_broken = (tournament.get("state") == "ACTIVE"
                           and tournament.get("referencedQuizAggregateId") == quiz.get("aggregateId")
                           and quiz.get("state") == "DELETED")
        require(classification in ("HARMFUL_FOR_RULE", "NO_BROKEN_REFERENCE"),
                f"Invalid classification for evaluated attempt {scenario_id}")
        require(broken is computed_broken and broken is (classification == "HARMFUL_FOR_RULE"),
                f"Broken-reference observation or classification mismatch for {scenario_id}")
    else:
        require(validity == "INVALID" and classification == "NOT_EVALUATED",
                f"Invalid attempt must be NOT_EVALUATED for {scenario_id}")
        require(result.get("observationCompleted") is False and broken is None,
                f"Invalid attempt must not claim broken-reference absence for {scenario_id}")

    return {
        "assignedVector": vector,
        "recoveryOrderIndex": ordinal,
        "recoveryScheduleIdentity": scenario_id,
        "persistedActionOrder": result.get("persistedActionOrder", []),
        "executionAttemptId": attempt_id,
        "validity": validity,
        "validityReason": result.get("validityReason"),
        "executionTerminalStatus": result.get("executionTerminalStatus"),
        "scheduleConformance": result.get("scheduleConformance"),
        "impactV1": result_impact,
        "classification": classification,
        "observationCompleted": result.get("observationCompleted"),
        "brokenReference": broken,
        "brokenReferenceReason": result.get("brokenReferenceReason"),
        "tournamentOutsideActiveSaga": validity == "VALID",
        "tournament": result.get("tournament"),
        "referencedQuiz": result.get("referencedQuiz"),
        "runtimeContext": runtime,
        "artifacts": {
            "executionReport": {"path": str(execution_path), "sha256": sha256(execution_path)},
            "impactV1Report": {"path": str(impact_path), "sha256": sha256(impact_path)},
            "benchmarkAttempt": {"path": str(result_path), "sha256": sha256(result_path)},
        },
    }


def count_rows(rows: list[dict[str, Any]]) -> dict[str, int]:
    return {
        "executed": len(rows),
        "valid": sum(row["validity"] == "VALID" for row in rows),
        "notEvaluated": sum(row["classification"] == "NOT_EVALUATED" for row in rows),
        "impactV1Zero": sum(row["impactV1"].get("evaluationStatus") == "EVALUATED"
                            and row["impactV1"].get("score") == 0 for row in rows),
        "impactV1NonZero": sum(row["impactV1"].get("evaluationStatus") == "EVALUATED"
                               and row["impactV1"].get("score") != 0 for row in rows),
        "impactV1NotEvaluated": sum(row["impactV1"].get("evaluationStatus") != "EVALUATED" for row in rows),
        "brokenReferencePresent": sum(row["brokenReference"] is True for row in rows),
        "brokenReferenceAbsent": sum(row["brokenReference"] is False for row in rows),
        "brokenReferenceNotEvaluated": sum(row["brokenReference"] is None for row in rows),
        "tournamentOutsideActiveSaga": sum(row["tournamentOutsideActiveSaga"] is True for row in rows),
        "tournamentSagaStateNotProven": sum(row["tournamentOutsideActiveSaga"] is not True for row in rows),
    }


def aggregate(args: argparse.Namespace) -> None:
    model = package_model(args.manifest, args.workload_plan_id)
    rows: list[dict[str, Any]] = []
    vector_summaries: list[dict[str, Any]] = []
    for vector in model["canonicalVectors"]:
        scenarios = model["scenariosByVector"][vector]
        vector_rows = [read_attempt(args.attempt_dir, scenario, vector, ordinal, model)
                       for ordinal, scenario in enumerate(scenarios, start=1)]
        rows.extend(vector_rows)
        accounting = model["accountingByVector"][vector]
        vector_summaries.append({
            "assignedVector": vector,
            "vectorSource": accounting["vectorSource"],
            "uncappedRecoveryScheduleCount": int(accounting["uncappedUniqueScheduleCount"]),
            "persistedRecoveryScheduleCount": int(accounting["writtenScheduleCount"]),
            **count_rows(vector_rows),
        })

    attempt_ids = [row["executionAttemptId"] for row in rows]
    scenario_ids = [row["recoveryScheduleIdentity"] for row in rows]
    require(len(attempt_ids) == len(set(attempt_ids)), "Landscape has duplicate execution attempt rows")
    require(len(scenario_ids) == len(set(scenario_ids)), "Landscape has duplicate FaultScenario rows")
    extra_result_paths = set(args.attempt_dir.glob("*/result.json")) - {
        Path(row["artifacts"]["benchmarkAttempt"]["path"]) for row in rows
    }
    require(not extra_result_paths, f"Attempt directory contains results outside the landscape: {sorted(extra_result_paths)}")
    expected = sum(summary["persistedRecoveryScheduleCount"] for summary in vector_summaries)
    require(len(rows) == expected, "Executed rows do not equal persisted canonical recovery schedules")

    revisions = Counter(row["runtimeContext"].get("sourceRevision") for row in rows)
    tree_states = Counter(row["runtimeContext"].get("sourceTreeState") for row in rows)
    require(len(revisions) == 1 and None not in revisions, "Landscape attempts do not pin one source revision")
    require(len(tree_states) == 1 and None not in tree_states, "Landscape attempts do not pin one source tree state")

    uncapped_total = sum(summary["uncappedRecoveryScheduleCount"] for summary in vector_summaries)
    persisted_total = sum(summary["persistedRecoveryScheduleCount"] for summary in vector_summaries)
    result = {
        "schemaVersion": LANDSCAPE_SCHEMA,
        "benchmarkId": BENCHMARK_ID,
        "observationRule": OBSERVATION_RULE,
        "scopeStatement": "NO_BROKEN_REFERENCE means only that the benchmark predicate is false; it is not a global safety claim.",
        "package": {
            "manifestPath": str(model["manifestPath"]),
            "manifestSha256": model["manifestSha256"],
            "semanticSchemaVersion": SCENARIO_SCHEMA,
            "recoveryScheduleCap": int(model["manifest"]["recoveryScheduleCap"]),
            "workloadPlanId": model["workload"]["deterministicId"],
            "sourceRevision": next(iter(revisions)),
            "sourceTreeState": next(iter(tree_states)),
        },
        "faultSlots": [{
            "slotIndex": slot["slotIndex"],
            "runtimeStepName": slot["runtimeStepName"],
            "sagaInstanceId": slot["sagaInstanceId"],
        } for slot in model["workload"]["faultSlots"]],
        "canonicalVectors": vector_summaries,
        "excludedMaskedVariants": model["excludedVectors"],
        "summary": {
            "canonicalVectorCount": len(model["canonicalVectors"]),
            "excludedMaskedVariantCount": len(model["excludedVectors"]),
            "uncappedRecoveryScheduleCount": uncapped_total,
            "persistedRecoveryScheduleCount": persisted_total,
            **count_rows(rows),
        },
        "rows": rows,
    }
    args.output_json.parent.mkdir(parents=True, exist_ok=True)
    args.output_json.write_text(json.dumps(result, indent=2, sort_keys=False) + "\n")

    columns = [
        "assignedVector", "recoveryOrderIndex", "recoveryScheduleIdentity", "executionAttemptId",
        "validity", "executionTerminalStatus", "scheduleConformance", "impactV1EvaluationStatus",
        "impactV1FindingCount", "impactV1Score", "classification", "brokenReference",
        "tournamentOutsideActiveSaga", "tournamentState", "tournamentRawSagaState",
        "tournamentSagaStateValueStatus", "tournamentSagaStateDecodeStatus",
        "tournamentDecodedSagaStateClass", "tournamentDecodedSagaStateName",
        "tournamentSagaStateDecoder", "tournamentSagaStateSource", "tournamentSagaStateVersion",
        "referencedQuizState",
        "runtimeContextId", "processId",
        "resetBoundary", "sourceRevision", "executionReportSha256", "impactV1ReportSha256",
        "benchmarkAttemptSha256",
    ]
    args.output_csv.parent.mkdir(parents=True, exist_ok=True)
    with args.output_csv.open("w", newline="") as destination:
        writer = csv.DictWriter(destination, fieldnames=columns)
        writer.writeheader()
        for row in rows:
            writer.writerow({
                "assignedVector": row["assignedVector"],
                "recoveryOrderIndex": row["recoveryOrderIndex"],
                "recoveryScheduleIdentity": row["recoveryScheduleIdentity"],
                "executionAttemptId": row["executionAttemptId"],
                "validity": row["validity"],
                "executionTerminalStatus": row["executionTerminalStatus"],
                "scheduleConformance": row["scheduleConformance"],
                "impactV1EvaluationStatus": row["impactV1"].get("evaluationStatus"),
                "impactV1FindingCount": row["impactV1"].get("findingCount"),
                "impactV1Score": row["impactV1"].get("score"),
                "classification": row["classification"],
                "brokenReference": row["brokenReference"],
                "tournamentOutsideActiveSaga": row["tournamentOutsideActiveSaga"],
                "tournamentState": (row["tournament"] or {}).get("state"),
                "tournamentRawSagaState": ((row["tournament"] or {}).get("persistedSagaState") or {}).get("rawValue"),
                "tournamentSagaStateValueStatus": ((row["tournament"] or {}).get("persistedSagaState") or {}).get("valueStatus"),
                "tournamentSagaStateDecodeStatus": ((row["tournament"] or {}).get("persistedSagaState") or {}).get("decodeStatus"),
                "tournamentDecodedSagaStateClass": ((row["tournament"] or {}).get("persistedSagaState") or {}).get("decodedStateClass"),
                "tournamentDecodedSagaStateName": ((row["tournament"] or {}).get("persistedSagaState") or {}).get("decodedStateName"),
                "tournamentSagaStateDecoder": ((row["tournament"] or {}).get("persistedSagaState") or {}).get("decoder"),
                "tournamentSagaStateSource": ((row["tournament"] or {}).get("persistedSagaState") or {}).get("source"),
                "tournamentSagaStateVersion": ((row["tournament"] or {}).get("persistedSagaState") or {}).get("aggregateVersion"),
                "referencedQuizState": (row["referencedQuiz"] or {}).get("state"),
                "runtimeContextId": row["runtimeContext"].get("runtimeContextId"),
                "processId": row["runtimeContext"].get("processId"),
                "resetBoundary": row["runtimeContext"].get("resetBoundary"),
                "sourceRevision": row["runtimeContext"].get("sourceRevision"),
                "executionReportSha256": row["artifacts"]["executionReport"]["sha256"],
                "impactV1ReportSha256": row["artifacts"]["impactV1Report"]["sha256"],
                "benchmarkAttemptSha256": row["artifacts"]["benchmarkAttempt"]["sha256"],
            })


def parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser(description=__doc__)
    commands = root.add_subparsers(dest="command", required=True)
    plan_parser = commands.add_parser("plan", help="derive canonical persisted scenarios from a package")
    plan_parser.add_argument("--manifest", type=Path, required=True)
    plan_parser.add_argument("--workload-plan-id", required=True)
    plan_parser.add_argument("--output", type=Path)
    plan_parser.set_defaults(handler=plan)

    aggregate_parser = commands.add_parser("aggregate", help="strictly join package and attempt evidence")
    aggregate_parser.add_argument("--manifest", type=Path, required=True)
    aggregate_parser.add_argument("--workload-plan-id", required=True)
    aggregate_parser.add_argument("--attempt-dir", type=Path, required=True)
    aggregate_parser.add_argument("--output-json", type=Path, required=True)
    aggregate_parser.add_argument("--output-csv", type=Path, required=True)
    aggregate_parser.set_defaults(handler=aggregate)
    return root


def main() -> int:
    args = parser().parse_args()
    try:
        args.handler(args)
        return 0
    except LandscapeError as failure:
        print(f"Landscape validation failed: {failure}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
