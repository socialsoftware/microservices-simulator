# 003 - Multi-Participant Materialization and Startup Gates

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `001-v3-participant-report-and-single-saga-migration.md`, `002-explicit-multi-saga-selection-and-dry-run.md`  
ACs covered: `AC-7, AC-8, AC-9, AC-15, AC-25, AC-30, AC-36, AC-37, AC-40`  
Risk: `high`

## Purpose

Add the non-dry-run preparation boundary for selected multi-saga plans: all participant inputs must materialize before any startup, and all participants must start before any scheduled step executes. Failures at these gates are hard stops with v3 participant state evidence.

## Scope

- For explicit non-dry-run multi-saga plans that pass structural validation, build one participant execution state per `SagaInstance`.
- Materialize every participant input under current `ScenarioMaterializer` semantics plus existing runtime-owned argument resolution.
- Ensure each participant has exactly one `SagaUnitOfWork` and that the same unit of work is used for constructor/runtime-owned `SagaUnitOfWork` arguments and later lifecycle calls.
- Instantiate one runtime functionality/session per participant only after all participant inputs materialize.
- Report materialization failure before startup or provider installation, with one participant entry per selected-plan `SagaInstance`, participant materialization states, and participant-local blockers.
- Report startup failure before scheduled-step execution, closure, or compensation, with one participant entry per selected-plan `SagaInstance`, participant startup states, and startup blockers.
- For materialization/startup hard stops, mark assigned slots that would have required runtime execution as `NOT_REACHED` where a valid vector/fault-slot mapping exists.
- Keep single-saga v3 behavior from S1 working.

## Out of Scope

- Successful multi-saga scheduled-step execution and final closure; S4 owns that.
- Assigned-fault realization, compensate-and-continue, runtime exception continuation, and compensation failure behavior.
- Broader materializer feature work for currently unsupported recipes.
- Persistent database setup/reset or fixture synthesis.

## Repo Anchors

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — add participant preparation state and gate sequencing.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioMaterializer.java` — runtime-owned argument handling; may need a narrow way to supply the participant's pre-created unit of work.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioRuntimeContext.java` — `createSagaUnitOfWork` boundary.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorMaterializationPolicy.java` — runtime-owned argument whitelist.
- `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java` — fixture can be extended to record constructor/runtime unit-of-work identity if needed.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — materialization/startup gate tests.

## Implementation Shape

- Add a minimal participant runtime state holder inside the executor package for validation result, input, materialized args, functionality instance, unit of work, states, outcomes, skipped steps, and blockers.
- Sequence gates strictly:
  1. structural validation/vector mapping;
  2. materialize all participants;
  3. startup all participants;
  4. later slices execute scheduled steps.
- If any materialization fails, do not instantiate any functionality and do not install the provider.
- If any startup fails, do not execute scheduled steps, do not call `resumeWorkflow`, and do not call `resumeCompensation` for already startup-ready participants.
- Keep startup failure participant lifecycle at `NOT_STARTED` or `CLOSURE_SKIPPED` only if the implemented report vocabulary needs to distinguish a created-but-not-closed participant; do not invent lifecycle method cleanup semantics.
- Prefer adapting `ScenarioMaterializer` to accept a participant-owned runtime values context rather than creating a second `SagaUnitOfWork` internally.

## TDD / Test Shape

- First behavior to test: a two-participant non-dry-run plan with one non-materializable participant writes `MATERIALIZATION_FAILED`, no steps, no provider, and no constructor/lifecycle calls.
- Expected red failure: current executor either rejects multi-saga shape or materializes only one single-saga input.
- Additional coverage:
  - all participant inputs are attempted before startup; participant states show which materialized and which failed, and the selected-plan participant list is present even when the top-level status is `MATERIALIZATION_FAILED`;
  - startup failure after materialization reports startup states for all participants, keeps the selected-plan participant list present, and does not execute/close/compensate any participant;
  - runtime-owned `SagaUnitOfWorkService`, `CommandGateway`, and `SagaUnitOfWork` remain allowed;
  - one participant-owned unit of work is reused consistently when a constructor receives `SagaUnitOfWork` and lifecycle calls later receive that unit of work;
  - assigned slots in a valid vector are `NOT_REACHED` for materialization/startup hard stops.

## Just-in-Time Preflight Required

Before source edits, the executor must re-check:

- Current `ScenarioMaterializer.runtimeOwnedValue(...)` behavior for `SagaUnitOfWork`.
- Actual Quizzes saga constructor pattern, especially constructors that accept `SagaUnitOfWork`.
- Current single-saga runtime-owned-argument test expectations in `ScenarioExecutorSpec.groovy`.
- Whether `ScenarioExecutorReadinessEvaluator` needs a matching update if materializer runtime-owned handling changes.

## Verification

- `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` — expected to pass with materialization/startup gate cases.
- `cd verifiers && mvn -Dtest=ScenarioExecutorReadinessEvaluatorSpec test` — run if readiness evaluator or materialization policy behavior changes.

## Evidence to Record

- files changed
- commands run and outputs
- report excerpts for `MATERIALIZATION_FAILED` and `STARTUP_FAILED`, including the selected-plan participant list
- fixture evidence showing no steps/closure/compensation on gate failure
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- Do not let one materialized participant execute or close if another participant fails materialization/startup.
- Avoid broad materializer feature work; unsupported recipes should remain blockers.
- The unit-of-work reuse fix is in scope only because AC-15 requires one unit of work per participant.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Added explicit non-dry-run multi-saga preparation gates after structural/vector validation: all participants materialize before startup, and startup hard-stops before any provider installation or scheduled-step/lifecycle execution.
- Added participant runtime state tracking for materialization/startup state, participant-local blockers, materialized arguments, functionality instance, and participant-owned `SagaUnitOfWork`.
- Adapted `ScenarioMaterializer` to accept a pre-created participant-owned `SagaUnitOfWork` for runtime-owned `SagaUnitOfWork` arguments while preserving existing runtime-owned `SagaUnitOfWorkService` and `CommandGateway` behavior.
- Updated single-participant execution to reuse the same pre-created `SagaUnitOfWork` for constructor/runtime-owned arguments and lifecycle calls.
- Added fixture/test evidence for materialization failure, startup failure, no provider/steps/closure/compensation on gate failures, `NOT_REACHED` assigned slots on hard stops, and unit-of-work reuse.

### Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — added multi-participant preparation gate sequencing, participant state reports, hard-stop `NOT_REACHED` slot marking, and unit-of-work reuse for single-participant execution.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioMaterializer.java` — added optional participant-owned runtime value context for `SagaUnitOfWork` resolution.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — added/updated focused materialization/startup gate and unit-of-work reuse coverage.
- `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java` — added constructor/lifecycle counters and unit-of-work identity capture for executor assertions.
- `issues/2026-07-08-multi-saga-scenario-executor/003-multi-participant-materialization-and-startup-gates.md` — recorded completion evidence.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | PASS | 40 tests run, 0 failures, 0 errors; includes multi-saga materialization/startup gate cases. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorReadinessEvaluatorSpec test` | PASS | 2 tests run, 0 failures, 0 errors; run because materializer runtime-owned handling changed. |

### Acceptance Criteria Evidence

- AC-7: Non-dry-run multi-saga participants now each call `ScenarioMaterializer` under existing semantics; a participant with an unsupported constructor recipe reports `MATERIALIZATION_FAILED`.
- AC-8: `MATERIALIZATION_FAILED` report keeps both selected-plan participants (`left`, `right`), marks states as `MATERIALIZED` / `MATERIALIZATION_FAILED`, records participant-local blockers, uses provider mode `NONE`, has no step outcomes, and fixture counters show no constructor/lifecycle execution.
- AC-9: `STARTUP_FAILED` report keeps both selected-plan participants, marks all materialization as `MATERIALIZED`, marks startup as `STARTUP_READY` / `STARTUP_FAILED`, records the startup blocker, and fixture counters show no scheduled steps, closure, or compensation.
- AC-15: Each participant preparation state owns one `SagaUnitOfWork`; `ScenarioMaterializer` reuses that object for runtime-owned `SagaUnitOfWork` constructor arguments. Single-participant regression verifies constructor and lifecycle calls receive the same object.
- AC-25: Assigned slots in materialization/startup hard-stop reports are marked `NOT_REACHED` where fault-slot mapping exists.
- AC-30: Materialization and startup failures remain hard-stop terminal statuses with provider mode `NONE`, not domain scenario outcomes.
- AC-36: Gate-failure reports include required participant entries with materialization/startup/lifecycle states and participant-local blockers.
- AC-37: Gate-failure reports keep top-level scenario facts, assigned vector/source, provider mode, fault slots, and top-level blockers.
- AC-40: Dummy/synthetic `ScenarioExecutorSpec` coverage proves the materialization/startup gate behavior required by this slice.

### Browser / Manual Evidence

- Not required.

### TDD Notes

- Added focused Spock coverage for the first required behavior (`MATERIALIZATION_FAILED` with no provider/steps/startup) and for startup failure/unit-of-work reuse, then implemented the executor/materializer changes. The first full verification run exposed dry-run ordering and fixture reset/count expectation issues; these were corrected before the passing verification runs above.

### Deviations From Plan

- Successful non-dry-run multi-saga preparation that reaches the post-startup boundary still returns `UNSUPPORTED_SCENARIO` for scheduled-step execution, because successful interleaving/final closure is explicitly owned by S4. Gate failure behavior for this slice is implemented.

### Blockers / Follow-Ups

- None for this slice. S4 remains responsible for replacing the post-startup unsupported boundary with default-vector scheduled-step execution and closure.
