"""Metamorphic and negative controls for the research detector's stated boundary."""
import copy
import unittest
from analyze import analyze


def obj(key, state="ACTIVE", refs=()):
    typ, ident = key.split(":")
    return {"key": key, "type": typ, "runtimeType": "Runtime" + typ,
            "id": ident, "version": 1, "previousVersion": None, "state": state, "sagaState": "NOT_IN_SAGA",
            "refs": [{"property": "reference", "targetKey": r} for r in refs]}


def snapshot(rows):
    return {"objects": rows, "coverage": {"complete": True, "types": ["Resource", "Consumer"]}}


def frame(actor, phase, objects, events=(), outcome="SUCCESS"):
    return {"actor": actor, "phase": phase, "action": "an operation",
            "outcome": {"status": outcome}, "events": list(events),
            "snapshot": snapshot(objects)}


def example(deleted=True, consumer_active=True, pending=0):
    resource = obj("Resource:10")
    consumer = obj("Consumer:20", "ACTIVE" if consumer_active else "DELETED", ["Resource:10"])
    removed = obj("Resource:10", "DELETED" if deleted else "ACTIVE")
    read = {"eventKind": "AGGREGATE_ACCESSED", "payload": {
        "accessMode": "READ", "aggregateType": "RuntimeResource", "aggregateId": "10"}}
    frames = [frame("A", "FORWARD", [resource]),
              frame("B", "FORWARD", [resource], [read]),
              frame("A", "COMPENSATION", [removed]),
              frame("B", "FORWARD", [removed, consumer]),
              frame("B", "FINALIZE", [removed, consumer])]
    return {"caseId": "arbitrary", "initialSnapshot": snapshot([]),
            "timeline": frames, "finalSnapshot": frames[-1]["snapshot"],
            "pendingEventCount": pending, "checks": []}


class CandidateAnalysisTest(unittest.TestCase):
    def test_surviving_dependency_has_witness_but_does_not_become_harm_by_assumption(self):
        result = analyze(example())
        self.assertEqual(1, result["candidateAffectedObjectCount"])
        self.assertEqual(1, result["exposureCount"])
        self.assertTrue(result["candidateFindings"][0]["sourceCreatorReadTargetBeforeDeletion"])
        self.assertTrue(result["candidateFindings"][0]["sourceCreatorFinalizedSuccessfully"])
        self.assertIsNone(result["impactScore"])

    def test_exposure_with_surviving_target_has_no_candidate_damage(self):
        result = analyze(example(deleted=False))
        self.assertEqual(1, result["exposureCount"])
        self.assertEqual(0, result["candidateAffectedObjectCount"])

    def test_exact_version_chain_requires_observed_returned_and_stored_version(self):
        case = example()
        case["timeline"][1]["returnedReads"] = [{"actor": "B", "objectKey": "Resource:10", "version": 1}]
        case["finalSnapshot"]["objects"][1]["refs"][0]["targetVersion"] = 1
        result = analyze(case)
        self.assertTrue(result["candidateFindings"][0]["exactCreatedReadStoredVersionChain"])
        self.assertTrue(result["exposures"][0]["exactReadVersionEstablished"])
        case["finalSnapshot"]["objects"][1]["refs"][0]["targetVersion"] = 2
        self.assertFalse(analyze(case)["candidateFindings"][0]["exactCreatedReadStoredVersionChain"])

    def test_consumer_recovery_removes_the_candidate(self):
        self.assertEqual(0, analyze(example(consumer_active=False))["candidateAffectedObjectCount"])

    def test_same_actor_partial_removal_needs_no_read_or_second_actor(self):
        case = example()
        initial = [obj("Resource:10"), obj("Consumer:20", refs=["Resource:10"])]
        end = [obj("Resource:10", "DELETED"), initial[1]]
        case.update(initialSnapshot=snapshot(initial),
                    timeline=[frame("A", "FORWARD", end)], finalSnapshot=snapshot(end))
        result = analyze(case)
        self.assertEqual(1, result["candidateAffectedObjectCount"])
        self.assertEqual(0, result["exposureCount"])
        self.assertIsNone(result["candidateFindings"][0]["sourceCreatorFinalizedSuccessfully"])

    def test_existing_deleted_reference_is_not_attributed_to_this_attempt(self):
        case = example()
        case["initialSnapshot"] = copy.deepcopy(case["finalSnapshot"])
        case["timeline"] = [frame("A", "FINALIZE", case["finalSnapshot"]["objects"])]
        self.assertEqual(0, analyze(case)["candidateAffectedObjectCount"])

    def test_name_changes_do_not_change_classification(self):
        import json
        case = example()
        renamed = json.loads(json.dumps(case).replace("Resource", "Invoice").replace("Consumer", "Receipt"))
        before, after = analyze(case), analyze(renamed)
        for field in ["candidateAffectedObjectCount", "exposureCount", "guardRejectionCount", "impactScore"]:
            self.assertEqual(before[field], after[field])

    def test_pending_work_and_missing_snapshots_remain_evidence_gaps(self):
        result = analyze(example(pending=2))
        self.assertFalse(result["eventHorizonComplete"])
        self.assertIsNone(result["impactScore"])
        case = example()
        case["timeline"].append(frame("B", "FINALIZE", []))
        self.assertTrue(any("disappeared" in gap for gap in analyze(case)["evidenceGaps"]))

    def test_historical_reference_is_an_intentional_false_positive_boundary(self):
        case = example()
        case["finalSnapshot"]["objects"][1]["refs"][0]["property"] = "historicalSnapshot"
        result = analyze(case)
        self.assertEqual(1, result["candidateAffectedObjectCount"])
        self.assertFalse(result["candidateFindings"][0]["domainViolationEstablished"])

    def test_dynamic_slice_first_observation_is_not_creation_evidence(self):
        case = example()
        case["initialSnapshot"].pop("coverage")
        result = analyze(case)
        self.assertEqual(0, result["exposureCount"])
        self.assertTrue(any("creation transition" in gap for gap in result["evidenceGaps"]))

    def test_first_observed_tombstone_does_not_prove_measured_deletion(self):
        case = example()
        case["timeline"] = [frame("A", "COMPENSATION", case["finalSnapshot"]["objects"])]
        result = analyze(case)
        self.assertEqual(0, result["newlyDeletedObjectCount"])
        self.assertEqual(0, result["candidateAffectedObjectCount"])
        self.assertTrue(any("creation transition" in gap for gap in result["evidenceGaps"]))

    def test_missing_final_target_and_empty_lock_slice_are_unknown(self):
        case = example()
        case["finalSnapshot"] = snapshot([case["finalSnapshot"]["objects"][1]])
        result = analyze(case)
        self.assertTrue(any("Referenced target" in gap for gap in result["evidenceGaps"]))
        case["finalSnapshot"] = snapshot([])
        self.assertIsNone(analyze(case)["observedLocksReleased"])


if __name__ == "__main__":
    unittest.main()
