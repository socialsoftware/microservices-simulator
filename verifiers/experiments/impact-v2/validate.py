#!/usr/bin/env python3
import argparse
import csv
import datetime
import hashlib
import json
from pathlib import Path


CORE_CASES = (
    "deleted-assigned",
    "deleted-unassigned",
    "residual-assigned-normal",
    "residual-assigned-noop",
    "residual-unassigned-normal",
    "residual-unassigned-noop",
    "event-current",
    "event-repaired",
)

EXPECTED = {
    "deleted-assigned": {"score": 2, "deleted": 1, "residual": 1, "event": 0},
    "deleted-unassigned": {"score": 0, "deleted": 0, "residual": 0, "event": 0},
    "residual-assigned-normal": {"score": 1, "deleted": 0, "residual": 1, "event": 0},
    "residual-assigned-noop": {"score": 1, "deleted": 0, "residual": 1, "event": 0},
    "residual-unassigned-normal": {"score": 0, "deleted": 0, "residual": 0, "event": 0},
    "residual-unassigned-noop": {"score": 0, "deleted": 0, "residual": 0, "event": 0},
    "event-current": {"score": 1, "deleted": 0, "residual": 0, "event": 1},
    "event-repaired": {"score": 0, "deleted": 0, "residual": 0, "event": 0},
}

CATEGORIES = {
    "deleted": "DELETED_DEPENDENCY",
    "residual": "FAILED_OPERATION_RESIDUAL",
    "event": "UNRESOLVED_DELIVERED_EVENT",
}
IMPACT_SCHEMA = "microservices-simulator.scenario-impact-v2-assessment.v1"


def load(path: Path):
    with path.open(encoding="utf-8") as stream:
        return json.load(stream)


def exact_integer(value):
    return isinstance(value, int) and not isinstance(value, bool)


def categories(report):
    values = report.get("categoryResults")
    assert isinstance(values, list) and len(values) == len(CATEGORIES), values
    assert all(isinstance(value, dict) for value in values)
    assert [value.get("category") for value in values] == list(CATEGORIES.values()), values
    indexed = {value.get("category"): value for value in values}
    assert len(indexed) == len(values), values
    assert set(indexed) == set(CATEGORIES.values()), indexed.keys()
    return indexed


def verify_complete_impact(report, execution, case):
    assert report.get("schemaVersion") == IMPACT_SCHEMA, case
    assert report.get("executionAttemptId") == execution.get("executionAttemptId"), case
    assert report.get("packageManifestPath") == execution.get("packageManifestPath"), case
    assert report.get("workloadPlanId") == execution.get("workloadPlanId"), case
    assert report.get("faultScenarioId") == execution.get("faultScenarioId"), case
    assert report.get("executionTerminalStatus") == execution.get("terminalStatus"), case
    assert report.get("scheduleConformance") == execution.get("scheduleConformance"), case
    assert report.get("assessmentStatus") == "COMPLETE", case
    assert report.get("collectionStatus") == "OBSERVED", case
    assert report.get("horizon") == "FINAL_SCHEDULED_ACTION", case
    for field in ("baseline", "finalState", "committedWrites", "eventDeliveries", "coverageGaps"):
        assert isinstance(report.get(field), list), (case, field)
    assert report["coverageGaps"] == [], case
    assert "completeScore" in report and exact_integer(report["completeScore"]), case
    assert "observedAffectedObjectCount" in report
    assert exact_integer(report["observedAffectedObjectCount"]), case
    assert report["completeScore"] == EXPECTED[case]["score"], case
    assert report["observedAffectedObjectCount"] == EXPECTED[case]["score"], case

    indexed = categories(report)
    for family, category_name in CATEGORIES.items():
        result = indexed[category_name]
        assert result.get("category") == category_name, (case, family)
        assert result.get("coverageStatus") == "COMPLETE", (case, family)
        assert exact_integer(result.get("candidateCount")), (case, family)
        assert result["candidateCount"] >= 0, (case, family)
        assert isinstance(result.get("candidates"), list), (case, family)
        assert len(result["candidates"]) == result["candidateCount"], (case, family)
        assert exact_integer(result.get("positiveObjectCount")), (case, family)
        assert result["positiveObjectCount"] == EXPECTED[case][family], (case, family)
        assert result["positiveObjectCount"] <= result["candidateCount"], (case, family)
        assert isinstance(result.get("findings"), list), (case, family)
        assert all(isinstance(finding, dict) and finding.get("category") == category_name
                   for finding in result["findings"]), (case, family)
        assert isinstance(result.get("unknownReasons"), list), (case, family)
        assert result["unknownReasons"] == [], (case, family)


def verify_unavailable_impact(report, execution, case):
    assert report.get("schemaVersion") == IMPACT_SCHEMA, case
    assert report.get("executionAttemptId") == execution.get("executionAttemptId"), case
    assert report.get("packageManifestPath") == execution.get("packageManifestPath"), case
    assert report.get("workloadPlanId") == execution.get("workloadPlanId"), case
    assert report.get("faultScenarioId") == execution.get("faultScenarioId"), case
    assert report.get("executionTerminalStatus") == execution.get("terminalStatus"), case
    assert report.get("scheduleConformance") == execution.get("scheduleConformance"), case
    assert report.get("assessmentStatus") == "UNAVAILABLE", case
    assert report.get("assessmentReason") == "COLLECTION_DISABLED", case
    assert report.get("collectionStatus") == "UNAVAILABLE", case
    assert report.get("collectionReason") == "COLLECTION_DISABLED", case
    assert report.get("horizon") == "FINAL_SCHEDULED_ACTION", case
    assert "completeScore" in report and report["completeScore"] is None, case
    assert "observedAffectedObjectCount" in report
    assert report["observedAffectedObjectCount"] is None, case
    for field in ("baseline", "finalState", "committedWrites", "eventDeliveries", "coverageGaps"):
        assert report.get(field) == [], (case, field)
    for result in categories(report).values():
        assert result.get("coverageStatus") == "UNAVAILABLE", (case, result)
        assert result.get("candidateCount") == 0 and not isinstance(result.get("candidateCount"), bool)
        assert result.get("positiveObjectCount") == 0 and not isinstance(result.get("positiveObjectCount"), bool)
        assert result.get("candidates") == [], (case, result)
        assert result.get("findings") == [], (case, result)
        assert result.get("unknownReasons") == [], (case, result)


def comparable_execution(report):
    return {
        "terminalStatus": report.get("terminalStatus"),
        "workloadPlanId": report.get("workloadPlanId"),
        "faultScenarioId": report.get("faultScenarioId"),
        "assignedVector": report.get("assignedVector"),
        "scheduleConformance": report.get("scheduleConformance"),
        "prerequisiteStatus": (report.get("prerequisiteSetup") or {}).get("status"),
        "faultSlots": [(item.get("slotIndex"), item.get("assignedBit"), item.get("state"))
                       for item in report.get("faultSlots", [])],
        "actualActions": [(item.get("kind"), item.get("runtimeStepName"), item.get("status"),
                           item.get("bodyOutcome"), item.get("commitOutcome"), item.get("faultOrigin"))
                          for item in report.get("actualActions", [])],
        "participants": [(item.get("sagaFqn"), item.get("materializationState"), item.get("finalState"))
                         for item in report.get("participants", [])],
    }


def sidecar_path(directory: Path, case: str):
    return directory / f"{case}.execution.impact-v2.json"


def witness_path(directory: Path, case: str):
    return directory / f"{case}.state-witness.json"


def verify_manifest(package_directory: Path):
    manifest = load(package_directory / "scenario-catalog-manifest.json")
    assert manifest.get("formatVersion") == 1
    files = manifest.get("files")
    assert isinstance(files, dict) and files
    for role, metadata in files.items():
        path = package_directory / metadata["path"]
        assert path.is_file(), (role, path)
        assert hashlib.sha256(path.read_bytes()).hexdigest() == metadata["sha256"], role
    return manifest


def jsonl_ids(path: Path):
    result = set()
    with path.open(encoding="utf-8") as stream:
        for line in stream:
            if line.strip():
                result.add(json.loads(line)["id"])
    return result


def sha256(path: Path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def tree_hashes(root: Path):
    result = {}
    for path in sorted(root.rglob("*")):
        if not path.is_file():
            continue
        relative = path.relative_to(root)
        if "target" in relative.parts or ".git" in relative.parts or "logs" in relative.parts:
            continue
        if path.name in (".git", "classpath.txt"):
            continue
        result[relative.as_posix()] = sha256(path)
    return result


def verify_source_provenance(directory: Path):
    manifest_path = directory / "source-content-manifest.tsv"
    scopes = {}
    with manifest_path.open(encoding="utf-8", newline="") as stream:
        rows = csv.DictReader(stream, delimiter="\t")
        assert rows.fieldnames == ["scope", "path", "sha256"]
        for row in rows:
            scope = scopes.setdefault(row["scope"], {})
            assert row["path"] not in scope, (row["scope"], row["path"])
            assert len(row["sha256"]) == 64
            scope[row["path"]] = row["sha256"]

    repository = Path(__file__).resolve().parents[3]
    if (repository / "simulator").is_dir():
        simulator_root = repository / "simulator"
        verifier_root = repository / "verifiers"
        app_root = repository / "applications" / "quizzes"
    else:
        simulator_root = Path("/workspace/simulator")
        verifier_root = Path("/verifiers")
        app_root = Path("/applications/quizzes")
    assert scopes["source-simulator"] == tree_hashes(simulator_root)
    assert scopes["source-verifiers"] == tree_hashes(verifier_root)
    assert scopes["source-app"] == tree_hashes(app_root)
    assert scopes["build-simulator"] == scopes["source-simulator"]
    assert scopes["build-verifiers"] == scopes["source-verifiers"]

    provider = "src/test/java/pt/ulisboa/tecnico/socialsoftware/quizzes/executor/QuizzesImpactV2PrerequisiteProvider.java"
    quiz_source = "src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/quiz/service/QuizService.java"
    update_source = "src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/coordination/sagas/UpdateTournamentFunctionalitySagas.java"
    provider_hash = sha256(verifier_root / "experiments" / "impact-v2" / "fixtures" /
                           "QuizzesImpactV2PrerequisiteProvider.java")
    expected_base = dict(scopes["source-app"])
    expected_base[provider] = provider_hash
    assert scopes["app-base"] == expected_base
    for variant, changed in (("app-question-repaired", quiz_source),
                             ("app-compensation-noop", update_source)):
        unchanged = dict(scopes[variant])
        changed_hash = unchanged.pop(changed)
        base_without_changed = dict(expected_base)
        original_hash = base_without_changed.pop(changed)
        assert unchanged == base_without_changed, variant
        assert changed_hash != original_hash, variant

    trusted = verifier_root / "experiments" / "impact-updates" / "patches"
    current = verifier_root / "experiments" / "impact-v2" / "patches"
    for name in ("quiz-update-register-changed.patch", "update-tournament-noop-compensation.patch"):
        assert (trusted / name).read_bytes() == (current / name).read_bytes(), name
    return sha256(manifest_path), scopes


def verify_artifact_hashes(directory: Path, output: Path, source_manifest_sha256: str, scopes):
    metadata = {}
    checksums = {}
    for line in (directory / "artifact-hashes.txt").read_text(encoding="utf-8").splitlines():
        if not line:
            continue
        if "=" in line and len(line.split(maxsplit=1)) == 1:
            key, value = line.split("=", 1)
            assert key not in metadata, key
            metadata[key] = value
            continue
        digest, path = line.split(maxsplit=1)
        resolved = Path(path)
        if not resolved.is_absolute():
            resolved = directory / resolved
        assert resolved.is_file(), resolved
        assert digest == sha256(resolved), resolved
        assert path not in checksums, path
        checksums[path] = digest

    required_metadata = {
        "sourceRevision", "fixtureNow", "originalQuizServiceSha256",
        "repairedQuizServiceSha256", "quizRepairPatchSha256",
        "originalUpdateTournamentSagaSha256", "mutantUpdateTournamentSagaSha256",
        "compensationMutantPatchSha256", "providerSourceSha256",
        "fixtureGeneratorSha256", "runnerSha256", "sourceContentManifestSha256",
    }
    assert set(metadata) == required_metadata, metadata.keys()
    assert len(metadata["sourceRevision"]) in (40, 64)
    assert all(character in "0123456789abcdef" for character in metadata["sourceRevision"])
    datetime.datetime.fromisoformat(metadata["fixtureNow"])
    assert metadata["sourceContentManifestSha256"] == source_manifest_sha256
    quiz_source = "src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/quiz/service/QuizService.java"
    update_source = "src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/coordination/sagas/UpdateTournamentFunctionalitySagas.java"
    assert metadata["originalQuizServiceSha256"] == scopes["source-app"][quiz_source]
    assert metadata["repairedQuizServiceSha256"] == scopes["app-question-repaired"][quiz_source]
    assert metadata["originalUpdateTournamentSagaSha256"] == scopes["source-app"][update_source]
    assert metadata["mutantUpdateTournamentSagaSha256"] == scopes["app-compensation-noop"][update_source]

    repository = Path(__file__).resolve().parents[3]
    verifier_root = repository / "verifiers" if (repository / "verifiers").is_dir() else Path("/verifiers")
    experiment = verifier_root / "experiments" / "impact-v2"
    assert metadata["providerSourceSha256"] == sha256(experiment / "fixtures" / "QuizzesImpactV2PrerequisiteProvider.java")
    assert metadata["fixtureGeneratorSha256"] == sha256(experiment / "generate-fixture.groovy")
    assert metadata["runnerSha256"] == sha256(experiment / "run.sh")
    assert metadata["quizRepairPatchSha256"] == sha256(experiment / "patches" / "quiz-update-register-changed.patch")
    assert metadata["compensationMutantPatchSha256"] == sha256(experiment / "patches" / "update-tournament-noop-compensation.patch")

    expected_hashed = set(directory.glob("*.json")) | set(directory.glob("*.tsv"))
    expected_hashed |= set((directory / "package").glob("*"))
    expected_hashed = {path.relative_to(directory).as_posix()
                       for path in expected_hashed if path.is_file() and path.resolve() != output.resolve()}
    assert set(checksums) == expected_hashed, (set(checksums) ^ expected_hashed)
    return metadata


def verify_event_identity(execution, impact, case):
    event_actions = [action for action in execution.get("actualActions", [])
                     if action.get("kind") == "EVENT_CONSEQUENCE"]
    assert len(event_actions) == 1, (case, event_actions)
    action = event_actions[0]
    evidence = action.get("eventEvidence")
    assert isinstance(evidence, dict), (case, action)
    deliveries = impact["eventDeliveries"]
    assert len(deliveries) == 1, (case, deliveries)
    delivery = deliveries[0]
    assert evidence.get("eventId") == delivery.get("eventId"), case
    assert evidence.get("eventTypeFqn") == delivery.get("eventType"), case
    assert evidence.get("publisherAggregateId") == delivery.get("publisherAggregateId"), case
    assert evidence.get("publisherAggregateVersion") == delivery.get("publisherAggregateVersion"), case
    receiver = delivery.get("receiverBefore") or {}
    receiver_identity = receiver.get("identity") or {}
    assert evidence.get("subscriberAggregateId") == receiver_identity.get("aggregateId"), case
    writer = delivery.get("writer") or {}
    assert writer.get("eventId") == evidence.get("eventId"), case
    assert writer.get("executionAttemptId") == execution.get("executionAttemptId"), case
    assert writer.get("workloadPlanId") == execution.get("workloadPlanId"), case
    assert writer.get("actionId") == action.get("actionId"), case


def tournament_application_data(report, field):
    snapshots = [value for value in report[field]
                 if (value.get("identity") or {}).get("aggregateType") == "SagaTournament"]
    assert len(snapshots) == 1, (field, snapshots)
    data = snapshots[0].get("applicationData")
    assert isinstance(data, dict), (field, snapshots[0])
    return data


def verify_update_residual_differences(impacts):
    normal_baseline = tournament_application_data(impacts["residual-assigned-normal"], "baseline")
    normal_final = tournament_application_data(impacts["residual-assigned-normal"], "finalState")
    noop_baseline = tournament_application_data(impacts["residual-assigned-noop"], "baseline")
    noop_final = tournament_application_data(impacts["residual-assigned-noop"], "finalState")

    def changed(left, right):
        return {key for key in set(left) | set(right) if left.get(key) != right.get(key)}

    assert changed(normal_baseline, normal_final) == {"lastModifiedTime", "tournamentTopics"}
    assert changed(noop_baseline, noop_final) == {
        "endTime", "lastModifiedTime", "numberOfQuestions", "startTime", "tournamentTopics"}
    assert normal_baseline["numberOfQuestions"] == normal_final["numberOfQuestions"] == 2
    assert normal_baseline["startTime"] == normal_final["startTime"]
    assert normal_baseline["endTime"] == normal_final["endTime"]
    baseline_topics = normal_baseline["tournamentTopics"]
    final_topics = normal_final["tournamentTopics"]
    assert [value["topicAggregateId"] for value in baseline_topics] == [
        value["topicAggregateId"] for value in final_topics]
    assert all(value["topicCourseAggregateId"] is not None for value in baseline_topics)
    assert all(value["topicCourseAggregateId"] is None for value in final_topics)


def assert_no_unknown(value, path="report"):
    if isinstance(value, dict):
        for key, child in value.items():
            assert_no_unknown(child, f"{path}.{key}")
    elif isinstance(value, list):
        for index, child in enumerate(value):
            assert_no_unknown(child, f"{path}[{index}]")
    elif isinstance(value, str):
        assert value != "UNKNOWN", path


def read_cost_metrics(directory: Path, impacts):
    with (directory / "cases.tsv").open(encoding="utf-8", newline="") as stream:
        rows = list(csv.DictReader(stream, delimiter="\t"))
    expected_cases = set(CORE_CASES) | {
        "deleted-assigned-observer-off",
        "residual-assigned-normal-observer-off",
        "event-current-observer-off",
    }
    assert {row["caseId"] for row in rows} == expected_cases
    expected_rows = {
        "deleted-assigned": ("deletedDependencyAssigned", "current", "on"),
        "deleted-unassigned": ("deletedDependencyUnassigned", "current", "on"),
        "residual-assigned-normal": ("failedUpdateAssigned", "current", "on"),
        "residual-assigned-noop": ("failedUpdateAssigned", "controlled-noop-compensation", "on"),
        "residual-unassigned-normal": ("failedUpdateUnassigned", "current", "on"),
        "residual-unassigned-noop": ("failedUpdateUnassigned", "controlled-noop-compensation", "on"),
        "event-current": ("unresolvedQuestionEvent", "current", "on"),
        "event-repaired": ("unresolvedQuestionEvent", "controlled-registerChanged-repair", "on"),
        "deleted-assigned-observer-off": ("deletedDependencyAssigned", "current", "off"),
        "residual-assigned-normal-observer-off": ("failedUpdateAssigned", "current", "off"),
        "event-current-observer-off": ("unresolvedQuestionEvent", "current", "off"),
    }
    metrics = {}
    for row in rows:
        case = row["caseId"]
        assert (row["selectionKey"], row["buildVariant"], row["observerMode"]) == expected_rows[case]
        numeric = {key: int(row[key]) for key in (
            "durationNanos", "executionBytes", "impactV1Bytes", "witnessBytes", "impactV2Bytes",
            "baselineCount", "finalStateCount", "committedWriteCount", "eventDeliveryCount",
            "coverageGapCount")}
        assert numeric["durationNanos"] > 0, case
        for key in ("executionBytes", "impactV1Bytes", "witnessBytes", "impactV2Bytes"):
            assert numeric[key] > 0, (case, key)
        assert numeric["executionBytes"] == (directory / f"{case}.execution.json").stat().st_size
        assert numeric["impactV1Bytes"] == (directory / f"{case}.impact-v1.json").stat().st_size
        assert numeric["witnessBytes"] == witness_path(directory, case).stat().st_size
        assert numeric["impactV2Bytes"] == sidecar_path(directory, case).stat().st_size
        impact = impacts.get(case) or load(sidecar_path(directory, case))
        for field, report_key in (("baselineCount", "baseline"), ("finalStateCount", "finalState"),
                                  ("committedWriteCount", "committedWrites"),
                                  ("eventDeliveryCount", "eventDeliveries"),
                                  ("coverageGapCount", "coverageGaps")):
            value = impact.get(report_key)
            assert numeric[field] == (len(value) if isinstance(value, list) else 0), (case, field)
        metrics[case] = numeric
    return metrics


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("directory", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    selection = load(args.directory / "package" / "selection.json")
    assert selection["fixtureKind"] == "qualification-only-provider-backed-not-source-extracted"
    verify_manifest(args.directory / "package")
    workload_ids = jsonl_ids(args.directory / "package" / "workloads.jsonl")
    fault_ids = jsonl_ids(args.directory / "package" / "fault-scenarios.jsonl")
    for selected in selection["cases"].values():
        assert selected["workloadPlanId"] in workload_ids
        assert selected["faultScenarioId"] in fault_ids
    source_manifest_sha256, source_scopes = verify_source_provenance(args.directory)
    provenance = verify_artifact_hashes(
        args.directory, args.output, source_manifest_sha256, source_scopes)

    reports = {}
    impacts = {}
    for case in CORE_CASES:
        execution_path = args.directory / f"{case}.execution.json"
        impact_path = sidecar_path(args.directory, case)
        execution = load(execution_path)
        impact = load(impact_path)
        reports[case] = execution
        impacts[case] = impact
        witness = load(witness_path(args.directory, case))
        assert witness["schemaVersion"] == "microservices-simulator.impact-v2-qualification-witness.v1"
        assert witness["fixtureKind"] == "qualification-only-read-only-post-execution-witness"
        assert execution["terminalStatus"] in ("SUCCESS", "COMPENSATED", "PARTIAL_COMPENSATED"), case
        assert execution["scheduleConformance"] in ("EXACT", "DEVIATED"), case
        assert (execution.get("prerequisiteSetup") or {}).get("status") == "SUCCEEDED", case
        reported_manifest = Path(execution.get("packageManifestPath", ""))
        assert reported_manifest.is_absolute(), case
        assert reported_manifest.name == "scenario-catalog-manifest.json", case
        verify_complete_impact(impact, execution, case)
        assert_no_unknown(impact)

    assert reports["deleted-assigned"]["assignedVector"] == "001"
    assert reports["deleted-unassigned"]["assignedVector"] == "000"
    assert reports["residual-assigned-normal"]["assignedVector"] == "00001"
    assert reports["residual-assigned-noop"]["faultScenarioId"] == reports["residual-assigned-normal"]["faultScenarioId"]
    assert reports["residual-unassigned-normal"]["assignedVector"] == "00000"
    assert reports["residual-unassigned-noop"]["faultScenarioId"] == reports["residual-unassigned-normal"]["faultScenarioId"]
    assert reports["event-current"]["faultScenarioId"] == reports["event-repaired"]["faultScenarioId"]
    verify_update_residual_differences(impacts)
    def prerequisite_facts(case):
        setup = reports[case].get("prerequisiteSetup") or {}
        return setup.get("bindings"), setup.get("evidence")

    for pair in (
        ("deleted-assigned", "deleted-unassigned"),
        ("residual-assigned-normal", "residual-assigned-noop"),
        ("residual-unassigned-normal", "residual-unassigned-noop"),
        ("residual-assigned-normal", "residual-unassigned-normal"),
        ("event-current", "event-repaired"),
    ):
        assert prerequisite_facts(pair[0]) == prerequisite_facts(pair[1]), pair
    common_fixture_evidence = []
    for case in CORE_CASES:
        evidence = dict((reports[case].get("prerequisiteSetup") or {}).get("evidence") or {})
        evidence.pop("requiredBindingCount", None)
        common_fixture_evidence.append(evidence)
    assert all(value == common_fixture_evidence[0] for value in common_fixture_evidence[1:])

    baseline_delivery = impacts["event-current"].get("eventDeliveries", [])
    repaired_delivery = impacts["event-repaired"].get("eventDeliveries", [])
    verify_event_identity(reports["event-current"], impacts["event-current"], "event-current")
    verify_event_identity(reports["event-repaired"], impacts["event-repaired"], "event-repaired")
    assert len(baseline_delivery) == 1 and len(repaired_delivery) == 1
    assert baseline_delivery[0]["receiverBefore"]["applicationData"] == baseline_delivery[0]["receiverAfter"]["applicationData"]
    assert baseline_delivery[0].get("eligibleAtHorizon") is True
    assert repaired_delivery[0]["receiverBefore"]["applicationData"] != repaired_delivery[0]["receiverAfter"]["applicationData"]
    assert repaired_delivery[0].get("eligibleAtHorizon") is False

    equivalence = {}
    for enabled_case in ("deleted-assigned", "residual-assigned-normal", "event-current"):
        disabled_case = f"{enabled_case}-observer-off"
        disabled_path = args.directory / f"{disabled_case}.execution.json"
        assert disabled_path.is_file(), disabled_case
        disabled = load(disabled_path)
        assert comparable_execution(reports[enabled_case]) == comparable_execution(disabled), enabled_case
        assert prerequisite_facts(enabled_case) == (
            (disabled.get("prerequisiteSetup") or {}).get("bindings"),
            (disabled.get("prerequisiteSetup") or {}).get("evidence")), enabled_case
        assert load(witness_path(args.directory, enabled_case)) == load(witness_path(args.directory, disabled_case)), enabled_case
        disabled_impact = load(sidecar_path(args.directory, disabled_case))
        verify_unavailable_impact(disabled_impact, disabled, disabled_case)
        assert_no_unknown(disabled_impact)
        equivalence[enabled_case] = True

    assert set(equivalence) == {"deleted-assigned", "residual-assigned-normal", "event-current"}
    cost_metrics = read_cost_metrics(args.directory, impacts)
    observer_cost_pairs = {}
    for enabled_case in equivalence:
        disabled_case = f"{enabled_case}-observer-off"
        observer_cost_pairs[enabled_case] = {
            "enabledDurationNanos": cost_metrics[enabled_case]["durationNanos"],
            "disabledDurationNanos": cost_metrics[disabled_case]["durationNanos"],
            "descriptiveDurationRatio": (
                cost_metrics[enabled_case]["durationNanos"] /
                cost_metrics[disabled_case]["durationNanos"]),
            "enabledImpactV2Bytes": cost_metrics[enabled_case]["impactV2Bytes"],
            "disabledImpactV2Bytes": cost_metrics[disabled_case]["impactV2Bytes"],
        }

    hashes = {}
    for path in sorted(args.directory.glob("*.json")):
        if path != args.output:
            hashes[path.name] = hashlib.sha256(path.read_bytes()).hexdigest()
    summary = {
        "schemaVersion": "microservices-simulator.impact-v2-qualification-validation.v1",
        "status": "PASS",
        "fixtureKind": selection["fixtureKind"],
        "coreCases": list(CORE_CASES),
        "observerOffEquivalence": equivalence,
        "expectedCounts": EXPECTED,
        "sourceContentManifestSha256": source_manifest_sha256,
        "sourceRevision": provenance["sourceRevision"],
        "fixtureNow": provenance["fixtureNow"],
        "costMetrics": cost_metrics,
        "observerCostPairs": observer_cost_pairs,
        "costInterpretation": "Single serial observations only; descriptive, not a statistical benchmark.",
        "reportSha256": hashes,
    }
    args.output.write_text(json.dumps(summary, indent=2, sort_keys=True) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
