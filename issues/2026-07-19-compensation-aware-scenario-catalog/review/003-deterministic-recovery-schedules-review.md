# Slice Review: 003 - Deterministic Recovery Schedules

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes`

## Sources Reviewed

- Slice: `issues/2026-07-19-compensation-aware-scenario-catalog/003-deterministic-recovery-schedules.md`
- Spec: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Implementation plan: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`
- ADRs: `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`, `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`
- Completion evidence: `003-deterministic-recovery-schedules.md` → `## Completion Evidence`
- Dependency evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/done/002-deterministic-v3-workload-package.md`, `issues/2026-07-19-compensation-aware-scenario-catalog/review/002-deterministic-v3-workload-package-review.md`
- Changed files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/RecoveryScheduleCap.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/RecoveryScheduleGenerator.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/ScenarioIdGenerator.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/FaultScenario.java`, `FaultScenarioAction.java`, `FaultScenarioActionKind.java`, `FaultSlotGenerationDiagnostic.java`, `FaultSlotGenerationState.java`, `RecoveryScheduleGenerationMetrics.java`, `RecoveryScheduleGenerationResult.java`, `ScenarioCatalogManifest.java`, `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/RecoveryScheduleGeneratorSpec.groovy`, and `DummyappAccountingFixtureFoundationSpec.groovy`
- Repo anchors reviewed: `WorkloadPlanValidator.java`, `WorkloadPlan.java`, `ForwardFaultSlot.java`, `CompensationCheckpoint.java`, `ScheduledStep.java`, `ScheduleEnumerator.java`, and the relevant WorkloadPlan construction in `ScenarioGenerator.java`
- Prior review reports: `None`
- Commands run by reviewer:
  - `mvn -Dtest=RecoveryScheduleGeneratorSpec,DummyappAccountingFixtureFoundationSpec test` from the repository root — failed as expected because this repository has no root POM; rerun from `verifiers/`
  - `cd verifiers && mvn -Dtest=RecoveryScheduleGeneratorSpec,DummyappAccountingFixtureFoundationSpec test`
  - `cd verifiers && mvn -Dtest=ScenarioModelSpec,ScenarioGeneratorSpec,ScenarioCatalogJsonlWriterSpec,RecoveryScheduleGeneratorSpec,DummyappAccountingFixtureFoundationSpec test`
  - `git diff --check`
  - `git status --short`, changed-path/diff inspection, review-attempt scan, and done-path collision check

## Summary

The slice satisfies the pure deterministic FaultScenario recovery-generation contract. Generation validates the WorkloadPlan and vector, scans the persisted forward order once, omits only failed-owner suffix actions, realizes or masks assigned bits according to participant liveness, and releases reverse completed-checkpoint queues only after their assigned fault action. A single forward cursor prevents survivor forward re-interleaving, while queue-local positions preserve reverse checkpoint order.

Exact uncapped counting is separated from retained materialization and uses memoized `BigInteger` state counts. Coverage representatives are built in the required priority and final-boundary order, validated and deduplicated before consuming capacity; canonical fill traverses action-identity order lazily and stops at the per-vector cap. The instrumented high-cardinality case proves the exact `C(60,30)` count without leaf enumeration and retains 20 schedules. FaultScenario/action identities recompute all replay-semantic fields rather than trusting supplied action ids. Required and supplemental reviewer test runs pass.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | pass | `RecoveryScheduleGenerator` implements validated per-vector scan, liveness/masking, reverse recovery queues, exact counting, representative selection, lazy fill, deterministic identities, diagnostics, and metrics. |
| Slice out-of-scope respected | pass | No eager eligibility decision, package mutation/writing, executor behavior, lifecycle reporting, survivor forward re-interleaving, or commit action was added. |
| Spec non-goals respected | pass | No eager `2^n` enumeration, all-vector exact aggregate, compensation faults/retries, random sampling, delayed commit, distributed/TCC behavior, search/scoring, or new dependency was introduced. |
| Dependencies done | pass | S2 is under `done/` and its latest attempt-03 review is `PASS`. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-4 | pass | `FaultScenario`, `FaultScenarioAction`, and `ScenarioIdGenerator` bind one WorkloadPlan id, one vector, and ordered action semantics including kind, owner, source slot/checkpoint, and occurrence. The identity mutation matrix includes stale supplied action-id resistance. | Covers the S3 generation/model portion; package persistence remains S4/S5. |
| AC-10 | pass | Single-point vectors use the same generator as all-zero and multi-fault vectors. First-step and later-fault fixtures prove a pre-body target does not enter the completed-checkpoint queue. | Eager consideration across materializable plans remains jointly owned by S4. |
| AC-11 | pass | Failed-owner suffix forwards are absent; assigned suffix bits are `MASKED` and zero-bit suffix slots are `SKIPPED_AFTER_PARTICIPANT_FAILURE`. |
| AC-12 | pass | Per-participant compensation queues are constructed from completed checkpoints in reverse order; synthetic and parser-real dummyapp assertions pass. |
| AC-13 | pass | Recovery state has one residual-forward cursor. The property/table test removes compensation actions from every retained schedule and matches the expected WorkloadPlan residual subsequence. |
| AC-14 | pass | Reachable zero-bit final forward occurrences produce ordered before/after representatives; assigned/faulted final occurrences are excluded, later suffix queues use earliest handling, and no commit action exists. | Runtime automatic commit remains S6. |
| AC-15 | pass | Multi-fault tests show same-participant later assignments mask while still-live participant assignments realize and release additional queues. |
| AC-16 | pass | `RecoveryScheduleCap` defaults to 20, accepts positive integers, and rejects blank, malformed, zero, and negative values. | Package-level pre-mutation configuration use remains S4/S5. |
| AC-17 | pass | Below-cap fixtures materialize every unique schedule; the six-schedule and all-zero cases have exact uncapped/written equality and unique ids. |
| AC-18 | pass | Cap 1–4 tests prove earliest/latest/recovery-first/forward-first prefix order. Further cases cover duplicate representatives, participant-id ties, successful final boundaries, later suffix faults, and lazy lexicographic action-id fill. |
| AC-44 | pass | S3's dummyapp-first portion is covered by real parser/adaptation workload bounds and reverse parser-derived checkpoint recovery, plus comprehensive pure synthetic semantics. | Full feature-wide AC-44 remains distributed across later slices. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| `cd verifiers && mvn -Dtest=RecoveryScheduleGeneratorSpec,DummyappAccountingFixtureFoundationSpec test` | pass | Fresh reviewer run: `Tests run: 27, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| Supplemental affected model/generator/package regression | pass | Fresh reviewer run of five specs: `Tests run: 145, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| Residual-forward order property/table check | pass | `RecoveryScheduleGeneratorSpec` checks all generated schedules for five vectors after deleting compensation actions. |
| High-cardinality exact/non-exhaustive case | pass | Exact `C(60,30) = 118264581564861424`; written `20`; exact counter visits `992` memoized states; lazy materialization visits fewer than `100` leaves. |
| Deterministic representative priority and fill | pass | Fixed cap 1–4 prefixes, duplicate handling, boundary order, multi-queue canonical order, and first lexicographic gap assertions all pass. |
| Real dummyapp parser-shape evidence | pass | Parser-derived compensation plan has five slots and four checkpoints; vector `00001` generates the reverse checkpoint sequence. Accepted-input workloads remain bounded at at most two slots. |
| `git diff --check` | pass | No whitespace errors. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Changes are confined to the pure recovery generator/cap, semantic records/identity, one shared schema constant, and targeted synthetic/dummyapp tests. |
| Existing patterns | pass | Uses existing immutable records, WorkloadPlan validator, SHA-256 identity helpers, deterministic list ordering, `BigInteger`, and Spock fixtures. Forward schedule generation remains owned by `ScheduleEnumerator`. |
| Test quality | pass | Tests assert semantic schedules, diagnostics, identities, exhaustive below-cap results, priority prefixes, multi-fault enablement, parser-derived checkpoints, deterministic reruns, and non-exhaustive high-cardinality behavior rather than private implementation details alone. |
| Regression risk | pass | Required and supplemental affected suites pass; one forward cursor and validated queue release positions make the key order/enabling invariants explicit. |
| Security/data safety | n/a | The slice is pure in-memory generation with no IO, package mutation, network operation, migration, or destructive behavior. |
| Change hygiene | pass | `git diff --check` passes. Unrelated untracked meeting-note files were not modified. |

## Findings

None.

## Done Transition

- Moved to done: `yes`
- Done path: `issues/2026-07-19-compensation-aware-scenario-catalog/done/003-deterministic-recovery-schedules.md`
- Reason if not moved: `n/a`

## Reviewer Notes

Eager package population/accounting and cap freezing remain correctly deferred to S4; persisted on-demand mutation remains S5; deep persisted-action validation and automatic-commit execution remain S6. This review passes only the S3-owned portions of shared acceptance criteria.
