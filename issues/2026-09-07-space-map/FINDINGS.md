# Findings outside the experiment implementation boundary

These findings do not authorize production changes. The campaign keeps its frozen
package, schedule, sample, ImpactV2 definition and all failed attempts.

## Eager/request identity equivalence

For workload `8d4c46bcb8700b2fd70e4911d4a5730f6d374a185c2b307ef629d3b5f55dccf0`,
the eager all-zero scenario
`b2cd97c18a849a63fb0e56e2725ef3ebf365e6f55ec5d0697f82ae9fbddecd16` and the
requested scenario
`5a5304c78a84b6463025243b66d98726de1df62c510860d29d18a8a2d7bb6a3e` have the same
vector `000000000` and exactly the same nine compact step actions. The request
returned PERSISTED with one added scenario. Both were accepted and executed by the
current reader. This is not a hash collision: different IDs represent equal compact
executable content. Across the eight workloads, 175 persisted IDs represent 125
distinct `(workload, vector, ordered actions)` tuples.

Relevant code path in the measured source:

- `ExecutableArtifactWriter` aliases participants/occurrences to `pN`/`sN`, while
  retaining the original FaultScenario ID.
- `ScenarioCatalogPackageReader.currentWorkload` reconstructs compact slot and
  checkpoint identities such as `slot-N` and `checkpoint-sN`.
- `ScenarioIdGenerator.faultScenarioId` hashes participant, source slot/checkpoint,
  event and occurrence identities as part of action content.
- `OnDemandFaultScenarioService` deduplicates additions by generated ID.

This explains a plausible identity boundary to characterize in a dedicated test.
A future change must decide how semantic idempotency coexists with preservation of
already persisted IDs and package compatibility. This experiment only adds an
analysis view of equivalent action sequences, and compares their runtime summaries;
it does not rewrite IDs or silently discard attempted cases.

Follow-up status (2026-09-07): the separately approved on-demand correction now reuses
an exact persisted compact `(workload, vector, ordered actions)` match and returns its
retained ID. It preserves every historical record; if a retained package already has
multiple matching IDs, new requests choose the lowest ID deterministically. The frozen
175-ID campaign and its 125 executable-content tuples remain unchanged evidence.

## Source setup readiness is weaker than a useful no-fault control

The w01 no-fault control successfully executes all setup actions and resolves shared
Execution/User bindings. The setup has already enrolled the student, so the normal
AddStudent participant encounters an unassigned "already enrolled" application
failure. A subsequent selected event has no eligible subscriber and stops execution
with `SELECTED_SUBSCRIBER_NOT_FOUND` and INVALID assessment.

Future qualification should distinguish a usable participant initial state from
constructor/setup readiness, and should account for every selected event delivery's
receiver. Removing a setup action or fabricating a receiver would change the modeled
workload and is outside this campaign. Existing no-event or differently routed
workloads would be separate workload/horizon controls, never replacements for the
failed selected cases.

## A positive score needs its lifecycle interpretation

In w07's vector `000000001`, the assigned fault at `solveQuizStep` follows successful
`startQuizAnswerStep`. Recovery reports the explicit answer compensation and the
implicit earlier checkpoints completed. ImpactV2 reports one
FAILED_OPERATION_RESIDUAL: SagaQuizAnswer was absent from baseline and remains
persisted with lifecycle DELETED. The metric counts that persistent difference.

This is an observed current-metric signal, not proof that a logically deleted object
is a functional application defect or that interaction between the two Sagas caused
the finding. The experiment does not alter residual equivalence, the anomaly policy,
or score. A later research decision may assess the value of this signal for the
intended defect definition; the raw snapshots and findings support that review.
