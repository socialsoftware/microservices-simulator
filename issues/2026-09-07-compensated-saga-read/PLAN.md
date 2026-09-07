# Plan — observation of a read of a compensated creation

**Implementation approved on 2026-09-07.** The user accepted the SPEC and assembled plan,
including controlled positive proof and separate ordinary-executor integration proof,
and requested this faithful English translation. No routine approval pause after
translation or between milestones; only a material scope expansion requires a decision.

## 1. Environment and execution mode

Documented route, milestone-reviewed execution. The initial investigation changed only
this issue's three documents. Linear local commits on `fault-analysis/scenarios` are
authorized. Preserve `note-04-09-2026.md` and concurrent work. No push, merge, PR, or
worktree. Implement in the authorized checkout and validate in an isolated snapshot;
coordinate with the campaign before source changes and heavy Docker work. Do not use
shared build targets/cache or run `mvn clean` in the main verifiers checkout. JDK21;
run Maven per module.

No visual-prototype verdict pause. Updates, score policy, or generation of the pair
using runtime result binding remain decisions for the user.

## 2. Implementation strategy

1. Add a typed adapter contract for a singular outer response and a delivery hook at
   `LocalCommandGateway.send` return. Unwrap only the known `SagaCommand` to select its
   payload contract; use the result actually returned in both serialization modes.
   Do not substitute the requested ID for the returned ID.
2. Reuse executor attribution scope, `afterCommit` writes, and existing snapshots.
   Capture a common observation order or action/occurrence positions proving causal
   order; do not compare independent collector counters.
3. Accumulate read facts in a separate diagnostic and join creation/delivery/tombstone
   to the exact checkpoint. Compose collection within the existing single observer
   scope, without a competing holder or duplicate write queries. Keep read gaps
   separate from those consumed by ImpactV2.

Initial Quizzes adapters declare `GetQuizByIdCommand → QuizDto` and
`GetTournamentByIdCommand → TournamentDto`, outer revision only. Persistent identity
comes from explicit typed mapping consistent with `PersistentStateObserver`; do not
resolve collisions by stripping “Saga” or guessing from DTO names. A returned revision
without auditable provenance remains unknown. Do not repair services, nested DTOs,
or semantic locks to obtain the desired result.

## 3. Milestones

### M0 — Prove exact delivery before building the rule

**Outcome:** FR-1, FR-2, FR-6, FR-7. A measured return identifies the delivered object's
identity/revision; a different internal read cannot replace it.

**Boundary/anchors:** observation integration in
`simulator/src/main/java/…/ms/messaging/local/LocalCommandGateway.java`, adapter/scope
contract, gateway tests, and dummyapp fixtures. Current dummyapp DTOs lack revisions:
add a dedicated positive fixture and retain a revisionless DTO as a negative. Dummyapp
remains source-only rather than becoming a runtime application.

**Proof before continuing:** direct and JSON returns; Saga wrapper; internal source v2
but returned DTO v1 (record v1); missing ID/revision; ambiguous type; service or semantic
lock handler failure after DTO construction (no delivery); failed attempt/retry followed
by success (only actual deliveries); observer/setup without application attribution;
callback failure preserving the outcome. Include unconventional names/fields to reject
heuristics.

### M1 — Auditable diagnostic and executor sidecar

**Outcome:** FR-3–FR-8. The sidecar separates positives, negatives, and gaps and changes
no score. Activation is opt-in; reads without write collection produce unavailability.

**Boundary/anchors:** `ImpactEvidence`, `ImpactWriterContext`,
`SagaUnitOfWorkService.registerCommittedWriteObservation`, `ImpactV2EvidenceCollector`,
`ScenarioExecutor`, `ScenarioExecutionReport.ActionOutcome`, and a separate diagnostic
assessor. Do not change FaultScenario definition/catalogs or the ImpactV2 assessor.

**Preflight:** verify compensation-action to producing-occurrence joins against the
current checkout, including runtime fallback; ambiguous references remain gaps. Confirm
that diagnostic-only failures cannot enter ImpactV2's gap list.

**Proof before continuing:** Spock in `verifiers/src/test/groovy`, dummyapp first, covering
the SPEC matrix, uncovered baseline, local rollback, failed reload, wrong-predecessor
tombstone, competing writer, wrong checkpoint, restored update, read after deletion,
reader without writes, and reader subsequently compensating. Verify deduplication, two
readers, stable ordering, attempt restart, interrupted prefix, and sidecar non-aliasing
with package/execution files. Compare ImpactV1, ImpactV2, conformance, and application
outcomes with the diagnostic enabled/disabled.

### M2 — Qualify Quizzes and document the actual boundary

**Outcome:** FR-9 and coverage documentation. The same assessor supports dummyapp and
Quizzes through adapters without domain-specific branches in the rule.

**Boundary/anchors:** Quizzes diagnostic integration/tests; existing
`verifiers/experiments/impact-three-cases/ImpactExperiment.java` harness;
`CreateTournamentStartQuizRecoveryWindowExploratoryTest`; canonical
`docs/verifiers-impl/current-state.md` and `roadmap.md`. Add the glossary term and remove
`(future)` only once implemented. Do not edit the personal/advisor meeting note.

**Proof before completion:** fresh JVM/Spring/H2 runs through Docker/snapshot repeating
the positive, compensation-before-read control, and successful-A control. Add A creates
→ B `FindQuiz` receives and finishes without writing → A compensates. This last path is
source-supported but was not reproduced during the initial investigation. Observe the
DTO at the generic return boundary and compare with the workflow-retained DTO and
persisted revisions. Validate outer Tournament lookup and the unversioned nested Quiz
reference as an uncovered case.

Demonstrate sidecar persistence through the ordinary executor using an already executable
workload (`FindQuiz` on a setup result is a control, not an A-positive). Positive proof
uses the harness to supply B with the actually produced ID; do not claim it is a normally
generated FaultScenario. Publish hashes, source/build versions, logs, and checks in a
HANDOFF. Repairing catalog runtime binding is not required by this slice.

## 4. Validation strategy and cost

Run focused simulator gateway/observer tests and new verifier Spock tests first, then
Quizzes composition. Reuse the existing investigation's Docker procedure in a coordinated
snapshot without competing with the campaign. Broaden regression only when changes
justify it.

Measure enabled/disabled differences in equivalent runs, declaring warmup and fixing
repetitions before examining durations. Report duration, response/write counts, sidecar
bytes, and retained memory. Hypothesis to validate: linear work in fact count, indexed
identity/revision joins, no extra read query; existing write collection continues to
dominate persistence cost. No mechanism cost measurement or numeric budget was approved.
Any retention limit must produce `PARTIAL/TRUNCATED`, never silently drop facts.

## 5. Risks and fallbacks

- **Misleading/incomplete DTO revision:** adapters are integration contracts, not automatic
  field-lineage proof. Compare source, DTO, and deserialized response in tests; no reliable
  contract means no finding. Avoid reflective graph expansion.
- **No universal hook:** declared bypasses preclude global conclusions.
  `COMPLETE_WITHIN_SCOPE` requires an explicit scope, never “all reads”.
- **Partial compensation/intermediate writes:** require a tombstone and direct predecessor
  linked to the producing checkpoint; retain unknowns. Add no equality heuristics.
- **Interference with existing observation:** opt-in, callback composition, and equivalent
  application/ImpactV2 results are acceptance criteria; revise integration if they fail.
- **Catalog cannot bind the produced ID to B:** retain controlled proof and separate
  integration control. A new input mechanism requires another plan.

Approved execution boundary: the creation-deletion diagnostic with typed adapters, final
return hook, and this three-milestone proof. Further routine approvals are not required.
