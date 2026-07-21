# 003 - Deterministic Recovery Schedules

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `002-deterministic-v3-workload-package.md`  
ACs covered: `AC-4, AC-10, AC-11, AC-12, AC-13, AC-14, AC-15, AC-16, AC-17, AC-18, AC-44`  
Risk: `high`

## Purpose

Generate exact, reproducible FaultScenario semantics for one assigned vector while bounding written recovery variants with the specified deterministic coverage-first selection.

## Scope

- Define forward and compensation action kinds, occurrence/source identities, deterministic action ids, and FaultScenario identity.
- Validate binary vectors against ordered WorkloadPlan fault slots before generation.
- Generate valid schedules by walking the persisted forward sequence, realizing assigned faults only in live participants, masking later slots in failed participants, omitting their later forward actions, and enabling reverse completed-checkpoint queues.
- Preserve the complete residual WorkloadPlan forward order globally and reverse checkpoint order within each failed participant; only compensation placement may vary.
- Generate one all-zero forward-only action schedule and support single-point and arbitrary multi-fault vectors through the same pure generator.
- Compute the exact unique uncapped schedule count without retaining or visiting every valid leaf schedule, and deduplicate retained semantically equivalent schedules by FaultScenario identity.
- Apply a positive per-vector recovery cap defaulting to `20`; representative and canonical-fill materialization must retain no more than the cap.
- Implement earliest, latest, recovery-first alternating, forward-first alternating, and ordered before/after reachable successful final-forward/automatic-commit boundary representatives exactly as specified, then lexicographic action-identity fill.
- Return generation diagnostics including realized/masked/skipped slot semantics and uncapped/written counts for later package/report layers.

## Out of Scope

- Deciding which WorkloadPlans are eagerly eligible.
- Writing or mutating package files.
- Runtime execution or lifecycle report events.
- Re-interleaving survivor forward actions or scheduling commit as an action.

## Repo Anchors

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/ScheduleEnumerator.java` — deterministic forward scheduling only; recovery is a separate stage.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/ScenarioIdGenerator.java` — length-delimited SHA-256 identity approach.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/FaultSpace.java` and `ScheduledStep.java` — ordered forward slot/occurrence anchors migrated by S2.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/ScenarioGeneratorSpec.groovy` — deterministic table/fixture testing style.
- `applications/dummyapp/` — multi-step and compensation-aware real-parser shapes.

## Implementation Shape

- Keep the recovery engine pure: input is one validated WorkloadPlan/vector/cap and output is deterministic FaultScenario candidates plus exact counts/diagnostics.
- Model generation state explicitly enough to track participant liveness, completed eligible checkpoints, enabled reverse queues, residual forward cursor, realized/masked assignments, and automatic-commit reachability.
- Use one total canonical action ordering: compensation queues by participant id then source checkpoint occurrence id; residual forward actions remain in WorkloadPlan order.
- Separate exact counting from retained materialization unconditionally. Use a memoized/factorized state count or equivalent exact non-exhaustive method with arbitrary precision; do not retain all valid schedules and do not derive an all-vector aggregate.
- Build and validate the finite priority representative set first. Canonical lexicographic fill must be lazy/state-guided and stop as soon as remaining capacity is satisfied rather than enumerate/sort every valid leaf.
- Representative candidates are validated and deduplicated before consuming capacity. For caps smaller than the representative set, retain the specified priority/final-boundary prefix.
- Final-step boundary actions stay forward actions; commit remains a later execution phase associated with those actions.

## TDD / Test Shape

- First behavior to test: a single assigned pre-body fault after two eligible completed steps generates reverse compensation actions while preserving the residual other-participant forward order.
- Expected red failure: v3 has no FaultScenario/recovery generator.
- Additional coverage: first-step fault/no checkpoint; failed-owner forward omission; two participant queues; later assigned slot masking; later live-participant fault realization; all-zero uniqueness; below-cap completeness; cap 1/2/3/4 priority prefixes; duplicate representatives; multiple final boundaries; unreachable/assigned final boundary exclusion; later suffix fault with earliest handling; lexicographic fill; deterministic rerun; action/id mutation matrix; malformed/non-positive cap rejection.
- High-cardinality coverage: an instrumented synthetic state space has a known exact count far above cap `20`, returns that exact count, retains at most 20 schedules, and proves via a test-only visited-state/materialized-leaf budget (or equivalent non-exhaustive oracle) that the full leaf set was not traversed or retained.

## Just-in-Time Preflight Required

Before source edits, the executor must re-check:

- S2’s final WorkloadPlan, checkpoint, fault-slot, structural-validator, and identity contracts.
- Existing schedule occurrence-id guarantees and whether repeated step identities need an explicit occurrence field.
- Real dummyapp materializable slot lengths to keep semantic exhaustive fixtures bounded.
- The high-cardinality synthetic oracle and a bounded performance threshold/environment that can run before the Quizzes smoke without making wall-clock timing the sole correctness assertion.

## Verification

- Run the targeted recovery/FaultScenario generator specs introduced during preflight — all vector, ordering, representative, cap, dedup, exact-count, and identity cases pass.
- Include a deterministic property/table check that deleting compensation actions from every generated schedule yields the valid residual subsequence of the original WorkloadPlan forward order.
- Run the instrumented high-cardinality case — exact large uncapped count matches the oracle, retained schedules stay within cap, and visited/materialized evidence stays below full leaf enumeration.

## Evidence to Record

- files changed
- commands run and outputs
- representative schedules and priority order for a fixed fixture
- uncapped/written count examples
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- Highest-risk slice: multi-fault queues become enabled over time; precomputing one static compensation list can produce invalid schedules.
- Lexicographic fill and exact count can be expensive. Exact counting must use compact state reuse, and fill must stop at capacity; a design that first enumerates every leaf fails this slice even if small fixtures pass.
- Never create a new survivor-to-survivor forward interleaving.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Added the v3 `FaultScenario` and forward/compensation action contract with deterministic semantic action ids and FaultScenario ids derived from WorkloadPlan id, assigned vector, and ordered action semantics.
- Added a pure recovery generator that validates WorkloadPlans/vectors/caps, scans the persisted forward order once to realize or mask assigned faults, omits failed-owner suffixes, releases reverse completed-checkpoint queues at realized faults, and reports per-slot generation diagnostics.
- Separated exact uncapped counting from retained materialization. Exact counts use memoized arbitrary-precision state counting; coverage representatives are constructed and deduplicated before cap consumption; canonical fill traverses action-identity order lazily and stops at capacity.
- Implemented earliest, latest, recovery-first alternating, forward-first alternating, successful final-forward before/after boundary representatives, canonical multi-queue tie-breaking, all-zero/single-point/multi-fault generation, and positive cap parsing with default `20`.
- Added synthetic semantic/property/high-cardinality coverage and real dummyapp parser-shape coverage. No package writing, eager eligibility, executor behavior, or lifecycle reporting was added.

### Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/RecoveryScheduleGenerator.java` — pure scan/count/representative/lazy-fill recovery engine.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/RecoveryScheduleCap.java` — positive cap value/parser with default `20`.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/ScenarioIdGenerator.java` — semantic action and FaultScenario SHA-256 identities.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/FaultScenario.java` — v3 concrete fault-scenario record.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/FaultScenarioAction.java` — action owner/source/occurrence contract.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/FaultScenarioActionKind.java` — `FORWARD` and `COMPENSATION` kinds.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/FaultSlotGenerationDiagnostic.java` — assigned-bit/slot generation diagnostic.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/FaultSlotGenerationState.java` — not-assigned, realized, masked, and failed-owner skipped states.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/RecoveryScheduleGenerationMetrics.java` — counting/materialization instrumentation.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/RecoveryScheduleGenerationResult.java` — scenarios, exact counts, cap, diagnostics, and metrics result.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/ScenarioCatalogManifest.java` — shares the canonical FaultScenario v3 schema constant.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/RecoveryScheduleGeneratorSpec.groovy` — vector, ordering, cap, representative, boundary, dedup, identity, validation, property, determinism, and high-cardinality tests.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/DummyappAccountingFixtureFoundationSpec.groovy` — real-parser workload-bound and compensation-checkpoint recovery assertions.
- `issues/2026-07-19-compensation-aware-scenario-catalog/003-deterministic-recovery-schedules.md` — completion evidence only.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=RecoveryScheduleGeneratorSpec test` before implementation | EXPECTED FAIL | Groovy compilation could not resolve `FaultScenario`; the v3 recovery contract did not exist. |
| `cd verifiers && mvn -Dtest=RecoveryScheduleGeneratorSpec,DummyappAccountingFixtureFoundationSpec test` | PASS | Fresh final required run: `Tests run: 27, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=ScenarioModelSpec,ScenarioGeneratorSpec,ScenarioCatalogJsonlWriterSpec,RecoveryScheduleGeneratorSpec,DummyappAccountingFixtureFoundationSpec test` | PASS | Supplemental model/generator/package regression: `Tests run: 145, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| Instrumented high-cardinality synthetic case in `RecoveryScheduleGeneratorSpec` | PASS | Exact count `C(60,30) = 118264581564861424`; written count `20`; memoized counting visited exactly `992` states and lazy materialization visited fewer than `100` leaves. |
| Real dummyapp parser-shape assertions | PASS | Accepted-input WorkloadPlans had at most `2` slots; the parser-real compensation fixture had `5` slots and `4` retained checkpoints, with reverse checkpoint actions generated for vector `00001`. |
| `git diff --check` | PASS | No whitespace errors. |

### Acceptance Criteria Evidence

- AC-4: FaultScenario references one WorkloadPlan and stores one vector plus ordered actions; action and scenario mutation tests cover kind, owner, source slot/checkpoint, occurrence, workload id, vector, and action order, including stale supplied action-id resistance.
- AC-10: Single-point vectors use the same generator as all-zero/multi-fault vectors; first-step and checkpoint-bearing target faults prove pre-body failure does not complete or add the target checkpoint.
- AC-11: Failed-owner suffix forwards are absent from actions and are diagnosed separately as masked assigned slots or skipped zero-bit slots.
- AC-12: Every generated failed-participant queue preserves reverse eligible-checkpoint order; the dummyapp parser fixture also proves reverse checkpoint ids.
- AC-13: A deterministic table/property assertion deletes compensation actions from every generated schedule and compares the result to the valid residual WorkloadPlan forward subsequence.
- AC-14: Reachable successful final forward actions produce ordered before/after boundary representatives, assigned/faulted boundaries are excluded, and no commit action is introduced.
- AC-15: Multi-fault coverage proves later same-participant assignments mask while still-live participant assignments realize and enable a new queue.
- AC-16: Missing cap input defaults to `20`; positive values parse; zero, negative, blank, and malformed values are rejected.
- AC-17: The six-schedule below-cap fixture writes all six unique FaultScenario ids; all-zero writes its one unique schedule.
- AC-18: Cap `1/2/3/4` tests prove the earliest/latest/recovery-first/forward-first priority prefix. Additional tests cover duplicate representatives, participant-id queue ties, multiple successful boundaries, suffix-fault earliest handling, and lazy lexicographic fill.
- AC-44: Required synthetic coverage is dummyapp-first complemented by a real-parser compensation fixture and bounded accepted-input slot-length assertion.

Fixed-fixture representative priority evidence for vector `00100` (common forward prefix `F:a1,F:a2,F:a3`):

1. earliest: `C:a2,C:a1,F:b1,F:b2`
2. latest: `F:b1,F:b2,C:a2,C:a1`
3. alternating recovery first: `C:a2,F:b1,C:a1,F:b2`
4. alternating forward first: `F:b1,C:a2,F:b2,C:a1`

Count examples: uncapped/written `6/6` at cap `20`, priority prefixes `6/1` through `6/4`, and high-cardinality `118264581564861424/20` at the default cap.

### Browser / Manual Evidence

- Not required.

### TDD Notes

- Added the first recovery behavior spec before production code and observed the expected compile failure because `FaultScenario` did not exist. Implemented the action/model/generator path, then expanded the suite through single/multi-fault, representative, identity, validation, property, dummyapp, and non-exhaustive exact-count cases.

### Deviations From Plan

- None.

### Blockers / Follow-Ups

- None. S4 owns eager materializable baseline/package/accounting integration; this slice intentionally stops at the pure per-vector generator and semantic models.
