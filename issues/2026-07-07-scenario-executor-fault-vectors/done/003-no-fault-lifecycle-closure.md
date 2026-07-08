# 003 - No-Fault Lifecycle Closure

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `001-simulator-in-memory-fault-provider.md`, `002-executor-vector-validation-and-report-v2.md`  
ACs covered: `AC-2, AC-7, AC-9, AC-10, AC-17, AC-18, AC-19, AC-20, AC-21`  
Risk: `medium`

## Purpose

Make default/all-zero vector execution a valid committed experiment by driving successful scheduled steps through simulator workflow closure instead of stopping after raw `executeUntilStep(...)` calls.

## Scope

- For valid all-zero/no-fault vector runs, install the in-memory provider as authoritative for the execution scope, even when it has no assigned faults.
- Drive forward scheduled steps one boundary at a time for the current supported single-saga plan.
- After the scheduled forward steps complete without injected or unexpected failures, call the simulator lifecycle closure primitive (`resumeWorkflow(...)`) so the saga commits.
- Report `terminalStatus = SUCCESS`, `lifecycleOutcome = COMMITTED`, `providerMode = IN_MEMORY_FAULT_VECTOR`, and ordered completed step outcomes.
- Clear provider state in all success/closure paths.
- Keep report-only side effects.
- Update existing executor fixture expectations that currently assert `resumeWorkflow(...)` is never called.

## Out of Scope

- Realized `1`-bit fault compensation and masking.
- Missing expected faults, provider mismatches, unexpected exceptions, and compensation failures.
- Docker smoke execution; S6 records realistic Quizzes smoke evidence.

## Repo Anchors

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — current scheduled-step loop and success status logic.
- `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java` — add/adjust resume/commit closure counters.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — update the current "never resumes workflow" behavior to the new committed lifecycle contract.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowFunctionality.java` — public `resumeWorkflow(...)` lifecycle API.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/Workflow.java` — commit behavior behind `resume(...)`.

## Implementation Shape

- Reuse validated slot assignments from S2 and provider scope from S1.
- Keep the existing materialization boundary: one supported single-saga candidate, runtime-owned arguments allowed narrowly.
- Execute each forward scheduled step in deterministic `scheduleOrder` and record outcomes as completed when the boundary succeeds.
- After all scheduled steps complete, call `resumeWorkflow(...)` through reflection or a stable executor helper, matching existing runtime style.
- Treat closure success as committed lifecycle. Later slices handle closure failure after fault/unexpected failure.
- Ensure provider scope closes even if `resumeWorkflow(...)` unexpectedly throws; S5 will refine failure classification.

## TDD / Test Shape

- First behavior to test: all-zero/default vector run calls the fixture's `resumeWorkflow(...)` once and reports `SUCCESS` + `COMMITTED`.
- Expected red failure: current `ScenarioExecutorSpec` asserts `FixtureWorkflow.resumeCalls == 0`, and current executor returns success without lifecycle closure.
- Additional coverage:
  - provider installed for default vector and cleaned after success;
  - CSV behavior is suppressed during no-fault vector runs;
  - step outcomes remain ordered by `scheduleOrder`;
  - report includes runtime metadata and unchanged catalog artifacts;
  - zero exit-code status for `SUCCESS`.

## Just-in-Time Preflight Required

Before source edits, the executor must re-check:

- Current `FixtureWorkflow` method signatures and counters.
- Current `ScenarioExecutor` reflection calls and exception unwrapping.
- Current simulator provider scope API from S1.
- Whether closure should be skipped for zero-step scenarios or reported as committed only after `resumeWorkflow(...)` is available.

## Verification

- `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` — no-fault lifecycle closure assertions pass.
- `cd simulator && mvn -Dtest=WorkflowExecutionPlanTest test` — only if simulator lifecycle code is touched during this slice.

## Evidence to Record

- files changed
- commands run and outputs
- v2 success report excerpt
- provider cleanup evidence
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- Do not broaden executor materialization or scenario-shape support.
- `resumeWorkflow(...)` may execute no additional steps if all scheduled steps already ran; it is still needed for commit closure.
- Be explicit in tests that the old no-resume behavior is intentionally replaced by supported lifecycle semantics.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Updated real ScenarioExecutor runs to install the simulator in-memory fault-vector provider for the execution scope, enter a provider boundary for each scheduled forward step, and close the provider with try-with-resources.
- After all scheduled forward steps complete, the executor now invokes `resumeWorkflow(...)` and reports committed success as `terminalStatus = SUCCESS`, `lifecycleOutcome = COMMITTED`, and `providerMode = IN_MEMORY_FAULT_VECTOR`.
- Updated the fixture executor spec to replace the old no-resume expectation with committed lifecycle assertions, provider cleanup evidence, ordered completed step outcomes, default-vector metadata, and persisted v2 report checks.

### Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — install/clear provider scope, enter per-step fault-vector boundary context, invoke `resumeWorkflow(...)`, and emit committed success report fields.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — assert no-fault/default execution calls lifecycle closure once, reports committed success, keeps ordered completed step outcomes, and clears provider state.
- `issues/2026-07-07-scenario-executor-fault-vectors/003-no-fault-lifecycle-closure.md` — recorded this completion evidence.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd simulator && mvn -DskipTests install` | PASS | Refreshed the local `MicroservicesSimulator` snapshot containing S1 provider classes so the verifier module can compile against them; build success. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | PASS | `Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`; includes no-fault lifecycle closure fixture assertions. |
| v2 success report excerpt from `ScenarioExecutorSpec` persisted output assertion | PASS | Fixture report asserted `terminalStatus=SUCCESS`, `lifecycleOutcome=COMMITTED`, `providerMode=IN_MEMORY_FAULT_VECTOR`, `vectorSource=DEFAULT_VECTOR`, `assignedVector=00`, and JSON `lifecycleOutcome` persisted as `COMMITTED`. |
| Provider cleanup evidence | PASS | Fixture test calls `FaultVectorProviderHolder.clear()` before execution and asserts `!FaultVectorProviderHolder.active` after committed success. |

### Acceptance Criteria Evidence

- AC-2: Default vector execution uses and reports `vectorSource = DEFAULT_VECTOR` with assigned all-zero vector `00`; provider mode proves executor-owned vector run rather than legacy CSV mode.
- AC-7: Provider scope is installed with try-with-resources and the test asserts the holder is inactive after success.
- AC-9: In-memory provider is authoritative during real executor runs via `providerMode = IN_MEMORY_FAULT_VECTOR`; the installed active provider suppresses CSV behavior at the simulator boundary from S1.
- AC-10: No-fault/default execution now calls `resumeWorkflow(...)` once and reports `SUCCESS` + `COMMITTED`.
- AC-17: Step outcomes remain ordered by schedule order and report `COMPLETED` for `first`, then `second`.
- AC-18: Runtime metadata/report fields from S2 remain populated; this slice asserts scenario/vector/provider lifecycle fields on the v2 report.
- AC-19: The executor still writes only the requested execution report; catalog mutation behavior remains covered by existing `ScenarioExecutorSpec` dry-run/report assertions.
- AC-20: `SUCCESS` remains a zero-exit terminal status through existing `ScenarioExecutorCli.exitCodeFor` coverage in `ScenarioExecutorSpec`.
- AC-21: Dummyapp-style fixture coverage now includes no-fault lifecycle closure and provider cleanup.

### Browser / Manual Evidence

- Not required.

### TDD Notes

- Updated the focused fixture spec before implementation to assert the new committed lifecycle contract and provider cleanup. The first targeted run did not execute the Spock feature because test filtering by a spaced feature name matched zero tests after compiling; the required full `ScenarioExecutorSpec` verification was run after implementation and passed.

### Deviations From Plan

- Did not touch simulator lifecycle code; therefore `cd simulator && mvn -Dtest=WorkflowExecutionPlanTest test` was not required. A `simulator` install without tests was run only to refresh the local dependency snapshot used by `verifiers`.

### Blockers / Follow-Ups

- None.
