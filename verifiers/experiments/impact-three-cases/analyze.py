#!/usr/bin/env python3
"""Bounded research analysis of observed effects; never a domain-correctness oracle.

Consumes plain snapshot/trace facts. No Quizzes names, reference-liveness policy,
expected-case labels, or known harmful-case IDs participate in detection.
"""
import argparse
import json
from pathlib import Path


def objects(snapshot):
    rows = snapshot.get("objects", [])
    result = {row["key"]: row for row in rows}
    if len(result) != len(rows):
        raise ValueError("Duplicate object identity in snapshot")
    return result


def status(frame):
    value = frame.get("outcome", {})
    return value.get("status", "UNKNOWN") if isinstance(value, dict) else value


def covers(snapshot, obj):
    coverage = snapshot.get("coverage", {})
    return coverage.get("complete") is True and obj["type"] in coverage.get("types", [])


def analyze(case):
    initial = objects(case["initialSnapshot"])
    previous = initial
    previous_snapshot = case["initialSnapshot"]
    ever_seen = set(initial)
    created_by = {}
    creation_versions = {}
    completed = set()
    deletions = {}
    reads = []
    exposures = []
    guard_rejections = 0
    incomplete_observations = []
    if not case["initialSnapshot"].get("coverage", {}).get("complete"):
        incomplete_observations.append("Initial snapshot does not establish complete absence within a declared type slice.")

    for position, frame in enumerate(case["timeline"]):
        actor = frame["actor"]
        after = objects(frame["snapshot"])
        if frame["snapshot"].get("coverage") != case["initialSnapshot"].get("coverage"):
            incomplete_observations.append(f"Snapshot coverage changed at action interval {position}.")
        # Resolve raw event types only through explicit snapshot type facts.
        lookup = {}
        for obj in list(previous.values()) + list(after.values()):
            for type_name in {obj["type"], obj.get("runtimeType", obj["type"])}:
                lookup[(type_name, str(obj["id"]))] = obj["key"]
        for event in frame.get("events", []):
            kind = event.get("eventKind")
            if kind == "INVARIANT_VIOLATION":
                guard_rejections += 1
            payload = event.get("payload", {})
            if kind != "AGGREGATE_ACCESSED" or payload.get("accessMode") != "READ":
                continue
            key = lookup.get((payload.get("aggregateType"), str(payload.get("aggregateId"))))
            if key is None:
                continue  # Outside the explicitly observed object slice.
            read = {"actor": actor, "object": key, "position": position,
                    "action": frame["action"]}
            returned = [value for value in frame.get("returnedReads", [])
                        if value.get("actor") == actor and value.get("objectKey") == key]
            versions = {value.get("version") for value in returned if value.get("version") is not None}
            read["returnedVersion"] = next(iter(versions)) if len(versions) == 1 else None
            read["versionEvidenceSource"] = "SUPPLEMENTAL_DTO_OBSERVER" if len(versions) == 1 else None
            reads.append(read)
            producer = created_by.get(key)
            if (producer is not None and producer != actor and producer not in completed
                    and previous.get(key, {}).get("state") != "DELETED"):
                exposures.append({**read, "producer": producer,
                                  "exactReadVersionEstablished": read["returnedVersion"] is not None,
                                  "matchesObservedCreationVersion": read["returnedVersion"] is not None
                                  and read["returnedVersion"] == creation_versions.get(key)})
        for key, obj in after.items():
            if key not in previous:
                if key not in ever_seen and covers(previous_snapshot, obj) and obj["state"] == "ACTIVE":
                    created_by[key] = actor
                    # A first observed version need not be the creation version.
                    if "previousVersion" in obj and obj["previousVersion"] is None:
                        creation_versions[key] = obj.get("version")
                else:
                    incomplete_observations.append(f"First appearance or reappearance lacks a proven creation transition: {key}")
            if (obj["state"] == "DELETED" and key in previous
                    and previous[key]["state"] != "DELETED"):
                deletions[key] = {"actor": actor, "phase": frame["phase"],
                                  "action": frame["action"], "position": position,
                                  "versionAfter": obj.get("version")}
        for key in previous.keys() - after.keys():
            incomplete_observations.append(f"Object disappeared from snapshot without a tombstone: {key}")
        if frame["phase"].upper() == "FINALIZE" and status(frame) == "SUCCESS":
            completed.add(actor)
        ever_seen.update(after)
        previous = after
        previous_snapshot = frame["snapshot"]

    final = objects(case["finalSnapshot"])
    if case["finalSnapshot"].get("coverage") != case["initialSnapshot"].get("coverage"):
        incomplete_observations.append("Final snapshot coverage differs from the initial slice.")
    for key in previous.keys() - final.keys():
        incomplete_observations.append(f"Object missing from final snapshot: {key}")
    findings = []
    for key, obj in sorted(final.items()):
        if obj["state"] != "ACTIVE":
            continue
        for ref in sorted(obj.get("refs", []), key=lambda r: (r["targetKey"], r["property"])):
            target = ref["targetKey"]
            if target not in final:
                incomplete_observations.append(f"Referenced target not observed in final slice: {target}")
                continue
            if (target not in deletions or final.get(target, {}).get("state") != "DELETED"
                    or initial.get(target, {}).get("state") == "DELETED"):
                continue
            deletion = deletions[target]
            source_creator = created_by.get(key)
            observed_reads = [r for r in reads if r["actor"] == source_creator
                              and r["object"] == target and r["position"] < deletion["position"]]
            finding = {
                "kind": "ACTIVE_REFERENCE_TO_OBJECT_REMOVED_DURING_ATTEMPT",
                "source": key, "target": target, "property": ref["property"],
                "sourcePresentInitially": key in initial,
                "sourceCreator": source_creator,
                "sourceCreatorFinalizedSuccessfully": None if source_creator is None else source_creator in completed,
                "targetDeletion": deletion,
                "sourceCreatorReadTargetBeforeDeletion": bool(observed_reads),
                "readEvidence": observed_reads,
                "storedReferenceVersion": ref.get("targetVersion"),
                "exactCreatedReadStoredVersionChain": ref.get("targetVersion") is not None
                and created_by.get(target) == deletion["actor"]
                and any(r["returnedVersion"] is not None
                        and r["returnedVersion"] == ref["targetVersion"] == creation_versions.get(target)
                        for r in observed_reads),
                "domainViolationEstablished": False,
            }
            if finding not in findings:
                findings.append(finding)

    checks_failed = [check["name"] for check in case.get("checks", []) if not check.get("passed")]
    pending = case.get("pendingEventCount")
    event_horizon_complete = pending == 0
    locks_released = (None if not final or any(obj.get("sagaState") is None for obj in final.values())
                      else all(obj["sagaState"] == "NOT_IN_SAGA" for obj in final.values()))
    evidence_gaps = [
        "Reference existence does not establish that the application requires a live target.",
        "Raw aggregate-access events do not contain the version actually read.",
        "Snapshot deltas attribute persisted effects to controlled action intervals, not every internal write.",
        "Only explicitly exported object types and relationships are observed.",
    ]
    if not event_horizon_complete:
        evidence_gaps.append("Pending events remain or their count is unknown; no settled global outcome is claimed.")
    if not locks_released:
        evidence_gaps.append("At least one observed semantic lock remains held or has unknown state.")
    evidence_gaps.extend(incomplete_observations)
    if checks_failed:
        evidence_gaps.append("Harness qualification checks failed: " + ", ".join(checks_failed))

    return {
        "caseId": case["caseId"],
        "candidateAffectedObjectCount": len({f["source"] for f in findings}),
        "candidateFindings": findings,
        "exposureCount": len({(e["producer"], e["actor"], e["object"]) for e in exposures}),
        "exposures": exposures,
        "guardRejectionCount": guard_rejections,
        "newlyDeletedObjectCount": len([key for key in deletions
                                       if final.get(key, {}).get("state") == "DELETED"]),
        "observedLocksReleased": locks_released,
        "pendingEventCount": pending,
        "eventHorizonComplete": event_horizon_complete,
        "impactScore": None,
        "impactStatus": "NOT_ESTABLISHED_BY_THIS_EXPERIMENT",
        "evidenceGaps": evidence_gaps,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("cases", nargs="+", type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    rows = [analyze(json.loads(path.read_text())) for path in args.cases]
    rows.sort(key=lambda row: row["caseId"])
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps({"experiment": "impact-three-cases", "results": rows},
                                     indent=2, sort_keys=True) + "\n")
    for row in rows:
        print(f"{row['caseId']}: candidates={row['candidateAffectedObjectCount']} "
              f"exposures={row['exposureCount']} guards={row['guardRejectionCount']} "
              f"pending={row['pendingEventCount']} impact=not-established")


if __name__ == "__main__":
    main()
