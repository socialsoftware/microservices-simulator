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
