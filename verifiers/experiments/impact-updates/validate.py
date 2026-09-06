#!/usr/bin/env python3
import argparse
import hashlib
import json
from pathlib import Path


CASES = (
    "question-baseline",
    "question-repaired",
    "tournament-normal-compensation",
    "tournament-noop-compensation",
)


def load(path: Path):
    with path.open(encoding="utf-8") as stream:
        return json.load(stream)


def check_map(report):
    return {item["name"]: item["passed"] for item in report["checks"]}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("directory", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    reports = {case: load(args.directory / f"{case}.json") for case in CASES}
    for case, report in reports.items():
        assert report["status"] == "PASS", (case, report.get("failure"))
        assert all(check_map(report).values()), (case, check_map(report))
        assert report["freshJvm"] is True
        assert report["build"]["sourceRevision"] not in ("", "unknown")

    repository = Path(__file__).resolve().parents[3]
    quiz_source = repository / "applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/quiz/service/QuizService.java"
    update_source = repository / "applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/coordination/sagas/UpdateTournamentFunctionalitySagas.java"
    digest = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()
    current_quiz_sha = digest(quiz_source)
    current_update_sha = digest(update_source)
    source_revisions = {report["build"]["sourceRevision"] for report in reports.values()}
    assert len(source_revisions) == 1
    fixture_times = {report["build"]["fixtureNow"] for report in reports.values()}
    assert len(fixture_times) == 1 and "unknown" not in fixture_times
    for report in reports.values():
        assert report["build"]["originalQuizServiceSha256"] == current_quiz_sha
        assert report["build"]["originalUpdateTournamentSagaSha256"] == current_update_sha

    baseline = reports["question-baseline"]
    repaired = reports["question-repaired"]
    assert baseline["event"]["publisherAggregateVersion"] != baseline["afterPublisher"]["question"]["version"]
    assert baseline["firstDelivery"]["success"] and baseline["secondDelivery"]["success"]
    assert repaired["firstDelivery"]["success"] and not repaired["secondDelivery"]["success"]
    assert repaired["secondDelivery"]["failureReason"] == "SELECTED_SUBSCRIBER_NOT_FOUND"
    assert baseline["afterSecondDelivery"]["quizQuestion"] == baseline["initial"]["quizQuestion"]
    assert repaired["afterFirstDelivery"]["quizQuestion"]["questionVersion"] == repaired["event"]["publisherAggregateVersion"]
    assert baseline["build"]["variantQuizServiceSha256"] == current_quiz_sha
    assert repaired["build"]["variantQuizServiceSha256"] != current_quiz_sha
    assert repaired["build"]["appliedPatchSha256"] == digest(repository / "verifiers/experiments/impact-updates/patches/quiz-update-register-changed.patch")
    for key in ("eventType", "publisherAggregateId", "publisherAggregateVersion", "published", "payload"):
        assert baseline["event"][key] == repaired["event"][key]
    assert baseline["event"]["eventId"] == repaired["event"]["eventId"]
    assert baseline["deliveryRoute"] == repaired["deliveryRoute"]
    assert baseline["firstDelivery"]["subscriberId"] == repaired["firstDelivery"]["subscriberId"]
    assert baseline["initial"]["question"]["aggregateId"] == repaired["initial"]["question"]["aggregateId"]
    assert baseline["initial"]["quiz"]["aggregateId"] == repaired["initial"]["quiz"]["aggregateId"]

    normal = reports["tournament-normal-compensation"]
    mutant = reports["tournament-noop-compensation"]
    projection = lambda state: {
        key: state["tournament"][key]
        for key in ("numberOfQuestions", "startTime", "endTime", "topicIds")
    }
    assert projection(normal["initial"]) == projection(normal["final"])
    assert projection(mutant["afterForwardTournamentWrite"]) == projection(mutant["final"])
    assert projection(mutant["initial"]) != projection(mutant["final"])
    assert normal["final"]["tournament"]["version"] > normal["afterForwardTournamentWrite"]["tournament"]["version"]
    assert normal["build"]["variantUpdateTournamentSagaSha256"] == current_update_sha
    assert mutant["build"]["variantUpdateTournamentSagaSha256"] != current_update_sha
    assert mutant["build"]["appliedPatchSha256"] == digest(repository / "verifiers/experiments/impact-updates/patches/update-tournament-noop-compensation.patch")
    assert normal["requestedTournamentUpdate"] == mutant["requestedTournamentUpdate"]
    assert normal["initial"]["tournament"]["aggregateId"] == mutant["initial"]["tournament"]["aggregateId"]
    assert normal["initial"]["quiz"]["aggregateId"] == mutant["initial"]["quiz"]["aggregateId"]
    assert normal["recoveryCheckpointPlan"] == mutant["recoveryCheckpointPlan"]
    assert normal["recoveryResults"] == mutant["recoveryResults"]

    hashes = {}
    for case in CASES:
        data = (args.directory / f"{case}.json").read_bytes()
        hashes[case] = hashlib.sha256(data).hexdigest()
    summary = {
        "schemaVersion": "microservices-simulator.impact-updates-validation.v1",
        "status": "PASS",
        "freshJvmCount": 4,
        "cases": list(CASES),
        "reportSha256": hashes,
        "crossCaseChecks": {
            "questionCurrentRepeatedEligibleNoProgress": True,
            "questionRepairConvergedAndMadeEventIneligible": True,
            "normalCompensationRestoredProjectionWithNewVersion": True,
            "noopCompensationRetainedFailedForwardProjection": True,
            "sourceAndVariantProvenanceVerified": True,
            "pairedFixturesRoutesAndInputsMatched": True,
        },
        "impactScore": None,
    }
    args.output.write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
