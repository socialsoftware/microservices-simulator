# 004 - Default-Vector Interleaving and Survivor Closure

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `001-v3-participant-report-and-single-saga-migration.md`, `002-explicit-multi-saga-selection-and-dry-run.md`, `003-multi-participant-materialization-and-startup-gates.md`  
ACs covered: `AC-13, AC-14, AC-15, AC-16, AC-17, AC-18, AC-31, AC-36, AC-37, AC-40`  
Risk: `high`

## Purpose

Execute a supported materializable multi-saga plan with the default/no-fault vector by replaying `expandedSchedule` deterministically and closing surviving participants at the end. This is the first end-to-end multi-saga runtime path.

## Scope

- Execute scheduled forward steps in ascending `scheduleOrder` for a selected, materialized, startup-ready multi-saga plan.
- Dispatch each scheduled step to the participant whose `sagaInstanceId` matches the step owner.
- Install the in-memory fault-vector provider for real execution, even when the assigned vector is all zeros, so CSV/manual impairment remains suppressed inside executor-owned vector runs.
- Record participant-local step outcomes under the owning participant.
- Track each participant's last executed scheduled step order.
- Close surviving participants after scheduled forward execution completes using `resumeWorkflow`.
- Enforce survivor closure order: ascending last executed scheduled step order, then zero-step participants in deterministic saga-instance id order.
- Report `SUCCESS` only when every participant commits without scheduled-step failures.
- Keep failed/terminal participant closure skipping behavior available for later slices; this slice only needs to avoid double-closing if an earlier single-saga failure path already creates a terminal participant.

## Out of Scope

- Assigned fault realization and compensation continuation.
- Non-assigned scheduled-step runtime exception continuation.
- Compensation failure continuation.
- Quizzes Docker smoke.
- Fixture/database reset or test setup replay.

## Repo Anchors

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — scheduled-step loop and lifecycle closure.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/FaultVectorProviderHolder.java` — provider scope cleanup.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowFunctionality.java` — `executeUntilStep` and `resumeWorkflow` lifecycle methods.
- `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java` — extend fixture to record participant identity, step execution order, and closure order.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — add multi-saga success and closure-order tests.

## Implementation Shape

- Use the normalized runtime-step list from validation; do not re-sort or reinterpret the catalog schedule during execution except by ascending `scheduleOrder`.
- Keep provider installation outside the per-step loop and inside a `try`/scope that always clears provider state.
- Enter a fault-vector boundary per scheduled step with scenario execution id, scenario plan id, participant saga instance id, scheduled step id, slot index, functionality identity, runtime step name, and assigned bit.
- Record `COMPLETED` participant step outcome after a zero-bit step completes.
- After all scheduled steps, close only participants not already terminal.
- If `resumeWorkflow` closure fails for a survivor in this slice, treat it as a hard-stop `UNEXPECTED_EXECUTION_FAILURE` with participant-local closure blocker; richer runtime-failure continuation is in S6 only for scheduled forward steps.

## TDD / Test Shape

- First behavior to test: a two-participant all-zero/default-vector plan executes steps in exact catalog schedule order and reports `SUCCESS` with both participants `COMMITTED`.
- Expected red failure: current executor rejects multi-saga plans or executes only a single participant.
- Additional coverage:
  - dispatch is by scheduled step `sagaInstanceId`, not by schedule position or saga class;
  - participant step outcomes are participant-local and preserve schedule order values;
  - closure order follows last executed scheduled step order;
  - participant with no scheduled steps closes after scheduled participants in deterministic saga-instance id order;
  - provider is cleared after successful multi-saga execution;
  - top-level `assignedVector` and `vectorSource=DEFAULT_VECTOR` match plan default.

## Just-in-Time Preflight Required

Before source edits, the executor must re-check:

- Current reflection calls for `executeUntilStep` and `resumeWorkflow` in `ScenarioExecutor.java`.
- Current `FixtureWorkflow` static state and whether tests need per-participant fixture classes or constructor arguments to distinguish participants.
- Existing provider setup/cleanup assertions in `ScenarioExecutorSpec.groovy`.

## Verification

- `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` — expected to pass with multi-saga default-vector success and closure-order coverage.

## Evidence to Record

- files changed
- commands run and outputs
- multi-saga success report excerpt showing participants, step outcomes, fault slots, and terminal `SUCCESS`
- fixture execution/closure order evidence
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- The simulator workflow may execute dependency-prefix steps when `executeUntilStep` is called; tests should use fixture workflows that make the intended one-boundary behavior visible and do not overclaim true concurrency.
- Do not close participants that became terminal in earlier/later failure paths.
- Keep schedule replay sequential and deterministic; do not add threads or async orchestration.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Added non-dry-run multi-saga default-vector execution after existing participant materialization/startup gates.
- Multi-saga execution now installs the in-memory fault-vector provider for the scheduled runtime section, replays validated runtime steps in ascending `scheduleOrder`, dispatches each step by `sagaInstanceId`, records participant-local `COMPLETED` outcomes, and closes surviving participants with `resumeWorkflow`.
- Survivor closure order is deterministic: participants with executed steps close by ascending last executed schedule order; zero-step participants close afterward by saga-instance id.
- Extended the executor fixture to record participant-qualified step execution and closure order, and added focused Spock coverage for a three-participant default-vector multi-saga execution including a zero-step participant.

### Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — replaces the prior multi-saga post-startup unsupported result with scheduled replay, provider scope, participant-local outcomes, and survivor closure.
- `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java` — records participant-qualified step execution and closure order while preserving existing counters.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — adds multi-saga default-vector success, owner dispatch, participant-local outcomes, closure order, and provider cleanup coverage.
- `issues/2026-07-08-multi-saga-scenario-executor/004-default-vector-interleaving-and-survivor-closure.md` — records this completion evidence.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | PASS | `Tests run: 41, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| TDD red run: `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` after adding the multi-saga success test and before implementation | FAIL | New test failed because report was `UNSUPPORTED_SCENARIO` with reason `multi-saga scheduled-step execution is not supported yet`, matching the expected red state. |

### Acceptance Criteria Evidence

- AC-13: The new multi-saga runtime path iterates `candidate.steps()` from validation, which is normalized by ascending `scheduleOrder`; the test uses catalog order `[right-step order 2, left-step order 1]` and observes execution `['left:leftRun', 'right:rightRun']`.
- AC-14: Runtime dispatch uses `step.scheduled().sagaInstanceId()` to select the participant state; the test proves left/right steps execute against their owning participant arguments.
- AC-15: Existing S3 participant preparation remains in use and this slice reuses each participant's own functionality and unit of work for scheduled execution and closure.
- AC-16: Closure order test evidence: `FixtureWorkflow.CLOSURES == ['left', 'right', 'idle']`, where `left` last executed order 1, `right` order 2, and zero-step `idle` closes afterward.
- AC-17: The closure loop filters terminal lifecycle outcomes before calling `resumeWorkflow`, preserving closure skipping for participants already marked terminal by existing/later failure paths.
- AC-18: The success test asserts `assignedVector == '00'` and `vectorSource == 'DEFAULT_VECTOR'`.
- AC-31: The multi-saga success path returns top-level `SUCCESS` only after every non-terminal participant closes and each participant lifecycle is `COMMITTED`.
- AC-36: Participant-local report evidence: left participant has `leftRun` at schedule order 1, right participant has `rightRun` at schedule order 2, idle participant has no step outcomes, and all participants report `MATERIALIZED`, `STARTUP_READY`, `COMMITTED`.
- AC-37: Top-level report evidence: scenario kind `MULTI_SAGA`, provider mode `IN_MEMORY_FAULT_VECTOR`, default assigned vector `00`, and fault slots remain scenario-level with `NOT_ASSIGNED` states.
- AC-40: Dummy/synthetic `ScenarioExecutorSpec` now covers successful multi-saga interleaving execution and final closure ordering.

### Browser / Manual Evidence

- Not required.

### TDD Notes

- TDD was used for the primary behavior. The new multi-saga success/closure-order test failed first with `UNSUPPORTED_SCENARIO`; implementation then changed the executor and the same focused test suite passed.

### Deviations From Plan

- The new focused test uses three participants instead of two so the same test also covers zero-step survivor closure ordering. This stays within the slice contract.
- Scheduled-step runtime exception handling in the new multi-saga path is intentionally only a hard-stop `UNEXPECTED_EXECUTION_FAILURE` with participant-local blocker, matching this slice's out-of-scope boundary for compensate-and-continue.

### Blockers / Follow-Ups

- None.
