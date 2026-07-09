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
