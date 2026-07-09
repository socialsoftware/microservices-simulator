# 006 - Runtime Failures, Compensation Failures, Hard Stops, and Exit Codes

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `001-v3-participant-report-and-single-saga-migration.md`, `002-explicit-multi-saga-selection-and-dry-run.md`, `003-multi-participant-materialization-and-startup-gates.md`, `004-default-vector-interleaving-and-survivor-closure.md`, `005-assigned-fault-compensate-and-continue.md`  
ACs covered: `AC-17, AC-20, AC-25, AC-26, AC-28, AC-29, AC-30, AC-32, AC-33, AC-34, AC-36, AC-37, AC-40, AC-44`  
Risk: `high`

## Purpose

Complete failure semantics for v3 single- and multi-saga execution: non-assigned scheduled-step exceptions follow participant outcome aggregation, compensation failures do not stop surviving participants, validation/provider hard stops remain hard stops, and CLI/Docker exit-code behavior matches the spec.

## Scope

- Treat non-assigned scheduled-step runtime exceptions as participant scenario outcomes for both multi-saga attempts and one-participant single-saga attempts:
  - record participant step outcome `FAILED`;
  - compensate that participant immediately when possible;
  - mark participant terminal;
  - skip that participant's remaining scheduled forward steps;
  - continue surviving participants when any exist;
  - aggregate a one-participant failed-and-compensated attempt to `COMPENSATED` rather than the old v2-style hard-stop `UNEXPECTED_EXECUTION_FAILURE`.
- If participant compensation fails:
  - mark that participant lifecycle `COMPENSATION_FAILED`;
  - keep that participant terminal and skip its remaining forward steps;
  - continue surviving participants;
  - aggregate top-level terminal status to `COMPENSATION_FAILED`.
- Handle assigned fault reached without expected injection as validation failure:
  - participant step outcome `EXPECTED_FAULT_NOT_INJECTED`;
  - fault-slot state `EXPECTED_FAULT_NOT_INJECTED`;
  - top-level terminal status `EXPECTED_FAULT_NOT_INJECTED`;
  - scenario-level validation blocker;
  - whole-attempt hard stop with later assigned slots `NOT_REACHED` unless already terminal/masked.
- Preserve provider mismatch and unexpected injected fault as hard-stop statuses, not domain outcomes.
- Mark assigned slots skipped by whole-attempt hard stops as `NOT_REACHED`.
- Keep closure failures, configuration failures, and report-writing failures as hard-stop/non-zero outcomes.
- For configuration failures before a normal report can be built, ensure CLI/Docker exits non-zero with useful stderr; use `CONFIGURATION_FAILED` as a report status only when a report object can actually be written.
- For report-writing failures, ensure CLI/Docker exits non-zero with useful stderr; write a `REPORT_WRITE_FAILED` report only if an actual secondary/fallback write path is implemented.
- Update CLI/orchestrator-facing exit-code tests for v3 statuses, including `CONFIGURATION_FAILED` and `REPORT_WRITE_FAILED` if represented by report statuses.

## Out of Scope

- New failure-policy modes such as stop-first or deferred compensation.
- Retry behavior after participant failure.
- Compensation-step fault injection.
- Quizzes smoke and docs updates; S7 owns them.

## Repo Anchors

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — failure handling, compensation, hard-stop report construction, final status aggregation.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorCli.java` — exit-code helper.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestrator.java` — forked command behavior; mostly unchanged but covered for status/option regressions.
- `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java` — fixture hooks for thrown body exception, suppressed expected fault, unexpected/wrong injected fault, and compensation failure.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — failure/status/exit vocabulary coverage.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestratorSpec.groovy` — command and option regression coverage.

## Implementation Shape

- Maintain two failure classes in the executor logic:
  - participant scheduled-step outcomes that can continue (`INJECTED_FAULT`, non-assigned `FAILED`, compensation failure after forward failure);
  - whole-attempt hard stops (`SELECTION_FAILED`, `UNSUPPORTED_SCENARIO`, `INVALID_FAULT_VECTOR`, `MATERIALIZATION_FAILED`, `STARTUP_FAILED`, `EXPECTED_FAULT_NOT_INJECTED`, `UNEXPECTED_INJECTED_FAULT`, `FAULT_PROVIDER_MISMATCH`, `UNEXPECTED_EXECUTION_FAILURE`, `CONFIGURATION_FAILED`, `REPORT_WRITE_FAILED`).
- Aggregate final domain outcome after all possible survivor continuation/closure:
  - any compensation failure => `COMPENSATION_FAILED`;
  - any compensated and any committed => `PARTIAL_COMPENSATED`;
  - compensated participants and no committed participants => `COMPENSATED`, including the one-participant single-saga scheduled-step failure case;
  - all committed and no scheduled-step failures => `SUCCESS`.
- Use `UNEXPECTED_EXECUTION_FAILURE` for infrastructure/closure/reflection failures outside scheduled-step participant outcomes, not for ordinary non-assigned scheduled-step domain/simulator exceptions.
- Keep report-writing failure handling simple: ensure CLI exits non-zero and emits useful stderr if the report cannot be written; write a `REPORT_WRITE_FAILED` report only if a secondary/fallback write path is actually possible.

## TDD / Test Shape

- First behavior to test: a non-assigned runtime exception in participant A compensates A, participant B continues and commits, top-level status is `PARTIAL_COMPENSATED`.
- Expected red failure: current executor reports `UNEXPECTED_EXECUTION_FAILURE` and stops the attempt.
- Additional coverage:
  - all participants that fail with non-assigned runtime exceptions and compensate aggregate to `COMPENSATED`;
  - one-participant single-saga non-assigned runtime exception compensates and aggregates to `COMPENSATED`, replacing the current `UNEXPECTED_EXECUTION_FAILURE` expectation;
  - compensation failure in participant A continues participant B and aggregates to `COMPENSATION_FAILED`;
  - expected fault not injected hard-stops and marks later assigned slots `NOT_REACHED`;
  - provider mismatch and unexpected injected fault hard-stop with non-zero status;
  - failed-and-compensated or compensation-failed participants are not closed again;
  - exact v3 status/state strings appear in report JSON;
  - configuration failure and report-writing failure behavior is covered either by non-zero CLI/stderr assertions or by `CONFIGURATION_FAILED`/`REPORT_WRITE_FAILED` report-status assertions if fallback reports are implemented;
  - `ScenarioExecutorCli.exitCodeFor` returns zero only for `SUCCESS`, `COMPENSATED`, `PARTIAL_COMPENSATED`, and `DRY_RUN`, and non-zero for `COMPENSATION_FAILED`, `CONFIGURATION_FAILED`, `REPORT_WRITE_FAILED`, and representative hard-stop statuses.

## Just-in-Time Preflight Required

Before source edits, the executor must re-check:

- Current catch blocks around `executeUntilStep`, expected fault not injected detection, provider mismatch detection, and `compensate(...)` in `ScenarioExecutor.java`.
- Current fixture flags in `FixtureWorkflow.java` and whether multi-participant tests need participant-specific flags rather than global flags.
- Current `ScenarioExecutorCli.exitCodeFor(...)` status list.
- Any docs/tests still expecting `UNEXPECTED_EXECUTION_FAILURE` for scheduled-step domain failures, including existing single-saga tests.

## Verification

- `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test` — expected to pass with failure, configuration/report-write behavior, and exit-code coverage.

## Evidence to Record

- files changed
- commands run and outputs
- report excerpts for scheduled-step failure continuation, one-participant single-saga runtime failure aggregation, compensation failure continuation, expected-not-injected hard stop, and exit-code mapping
- CLI/stderr or fallback-report evidence for configuration and report-writing failures
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- The spec deliberately uses `UNEXPECTED_EXECUTION_FAILURE` as a hard-stop status while non-assigned scheduled-step runtime exceptions are domain scenario outcomes. Keep that distinction explicit in code and tests.
- A compensation failure changes top-level status to `COMPENSATION_FAILED` but does not stop survivor execution.
- Do not let expected-not-injected become a compensated domain outcome; it is a validation failure.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Addressed review attempt 01 blocking findings: scheduled-step dispatch/reflection failures now hard-stop as `UNEXPECTED_EXECUTION_FAILURE`, expected-not-injected hard-stop marking preserves already `REALIZED`/`MASKED_BY_SAGA_FAILURE` slots, and one-participant runtime failures now report remaining forward steps as `SKIPPED_BY_SAGA_FAILURE`.
- Updated scheduled-step runtime exception handling so non-assigned step failures are participant domain outcomes: the failed participant records `FAILED`, compensates immediately, masks/skips that participant's later forward work, and survivor participants continue.
- Updated one-participant single-saga runtime failure aggregation from `UNEXPECTED_EXECUTION_FAILURE` to `COMPENSATED` when compensation succeeds.
- Updated multi-saga compensation failure handling so failed participants become `COMPENSATION_FAILED` while survivors still execute/close; final aggregation returns top-level `COMPENSATION_FAILED`.
- Added multi-saga `EXPECTED_FAULT_NOT_INJECTED` hard-stop handling with participant step outcome, scenario blocker, terminal status, and later assigned slots marked `NOT_REACHED`.
- Expanded executor coverage for runtime-failure continuation, all-compensated aggregation, compensation-failure continuation, expected-not-injected hard stop, and v3 exit-code mapping including `CONFIGURATION_FAILED` and `REPORT_WRITE_FAILED` status strings.

### Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — implemented S6 runtime failure, compensation failure continuation, expected-not-injected hard-stop, and fault-slot masking/not-reached semantics.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — updated single-saga runtime-failure expectation and added focused S6 multi-saga/status/exit-code/review-fix coverage.
- `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java` — added a targeted per-step suppressed-fault fixture hook for expected-not-injected preservation coverage.
- `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/MissingExecuteWorkflow.java` — added a focused fixture without `executeUntilStep` to test dispatch/reflection hard stops.
- `issues/2026-07-08-multi-saga-scenario-executor/006-runtime-failures-compensation-failures-and-exit-codes.md` — updated completion evidence only.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test` | PASS | Maven reported `Tests run: 57, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`. |
| `git diff --check -- verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/MissingExecuteWorkflow.java issues/2026-07-08-multi-saga-scenario-executor/006-runtime-failures-compensation-failures-and-exit-codes.md` | PASS | No whitespace errors reported. |

### Acceptance Criteria Evidence

- AC-17: Runtime-failed/compensated and compensation-failed participants are terminal; tests assert only survivor `right` closes (`FixtureWorkflow.CLOSURES == ['right']`) and failed participants are not closed again.
- AC-20: Fault-slot state coverage now includes runtime-failure masking (`MASKED_BY_SAGA_FAILURE`), expected-not-injected/not-reached states, and preservation of existing `REALIZED`/`MASKED_BY_SAGA_FAILURE` slot states during a later expected-not-injected hard stop.
- AC-25: Expected-not-injected hard-stop test asserts later assigned slot state `NOT_REACHED`.
- AC-26: Expected-not-injected test asserts participant step status `EXPECTED_FAULT_NOT_INJECTED`, top-level terminal status `EXPECTED_FAULT_NOT_INJECTED`, and blocker reason `EXPECTED_FAULT_NOT_INJECTED`.
- AC-28: Multi-saga non-assigned runtime failure test asserts `FAILED` for left, left `COMPENSATED`, left later step skipped/masked, right survivor commits, and top-level `PARTIAL_COMPENSATED`; single-saga runtime failure test asserts top-level `COMPENSATED` and skipped remaining forward step reporting.
- AC-29: Multi-saga compensation failure test asserts left `COMPENSATION_FAILED`, survivor right commits, later left slot masked, and top-level `COMPENSATION_FAILED`.
- AC-30: Provider mismatch/unexpected injected fault remain existing hard-stop tests; closure failures still use `UNEXPECTED_EXECUTION_FAILURE`; scheduled-step dispatch/reflection failures now hard-stop as `UNEXPECTED_EXECUTION_FAILURE` without compensation or survivor continuation; report-write failures still throw when no fallback path exists, giving non-zero CLI behavior rather than a synthetic report status.
- AC-32: All runtime-failing participants that compensate test asserts top-level `COMPENSATED`.
- AC-33: Mixed runtime-failed compensated participant plus committed survivor test asserts top-level `PARTIAL_COMPENSATED`.
- AC-34: Compensation failure continuation test asserts top-level `COMPENSATION_FAILED`.
- AC-36/AC-37: Tests assert participant lifecycle/step/skipped-step vocabulary and top-level terminal/fault-slot vocabulary for the new S6 cases, including one-participant skipped-step reporting after runtime failure.
- AC-40: Dummy/synthetic Spock coverage covers runtime exception continuation, all-compensated runtime failures, compensation failure continuation, expected-not-injected hard stop, expected-not-injected preservation of already terminal/masked slots, dispatch/reflection hard stops, and exit mapping.
- AC-44: Exit-code helper test asserts zero only for `SUCCESS`, `COMPENSATED`, `PARTIAL_COMPENSATED`, and `DRY_RUN`; non-zero for `COMPENSATION_FAILED`, `CONFIGURATION_FAILED`, `REPORT_WRITE_FAILED`, `EXPECTED_FAULT_NOT_INJECTED`, `FAULT_PROVIDER_MISMATCH`, and representative hard-stop statuses.

### Report / CLI Evidence Excerpts

- Scheduled-step failure continuation: `multi-saga non-assigned runtime failure compensates failed participant and lets survivor commit` asserts `terminalStatus == 'PARTIAL_COMPENSATED'`, left lifecycle `COMPENSATED`, left step `FAILED`, left skipped step `left-later`, right lifecycle `COMMITTED`, and fault-slot states `['NOT_ASSIGNED', 'NOT_ASSIGNED', 'MASKED_BY_SAGA_FAILURE', 'NOT_ASSIGNED']`.
- One-participant runtime failure aggregation: `single-saga runtime step failure at zero bit compensates and aggregates to compensated` asserts `terminalStatus == 'COMPENSATED'`, participant lifecycle `COMPENSATED`, step status `FAILED`, skipped remaining step `failure-plan-step-2`, and no scenario blockers.
- Compensation failure continuation: `multi-saga compensation failure does not stop surviving participant and aggregates to compensation failed` asserts `terminalStatus == 'COMPENSATION_FAILED'`, left lifecycle `COMPENSATION_FAILED`, right lifecycle `COMMITTED`, and survivor steps `['rightFirst', 'rightSecond']`.
- Expected-not-injected hard stop: `multi-saga expected fault not injected hard stops and marks later assigned slots not reached` asserts `terminalStatus == 'EXPECTED_FAULT_NOT_INJECTED'`, slot states `['EXPECTED_FAULT_NOT_INJECTED', 'NOT_ASSIGNED', 'NOT_REACHED']`, and blocker reason `EXPECTED_FAULT_NOT_INJECTED`.
- Expected-not-injected state preservation: `expected fault not injected preserves already masked slots while marking unreached assigned slots` asserts slot states `['REALIZED', 'EXPECTED_FAULT_NOT_INJECTED', 'MASKED_BY_SAGA_FAILURE', 'NOT_REACHED']`.
- Dispatch/reflection hard stops: single- and multi-saga dispatch-failure tests assert `terminalStatus == 'UNEXPECTED_EXECUTION_FAILURE'`, no step outcomes, no compensation calls, no survivor continuation in multi-saga, and provider cleanup.
- Exit-code mapping: `ScenarioExecutorCli.exitCodeFor` table asserts `SUCCESS`, `COMPENSATED`, `PARTIAL_COMPENSATED`, and `DRY_RUN` map to `0`; `COMPENSATION_FAILED`, `CONFIGURATION_FAILED`, `REPORT_WRITE_FAILED`, `EXPECTED_FAULT_NOT_INJECTED`, `FAULT_PROVIDER_MISMATCH`, `INVALID_FAULT_VECTOR`, and `UNSUPPORTED_SCENARIO` map to `1`.
- Configuration/report-writing behavior: no fallback report writer was implemented in this slice; current CLI/configuration/report-write exceptions remain process failures with non-zero exit behavior, and represented status strings are covered by the exit-code helper test.

### Browser / Manual Evidence

- Not required.

### TDD Notes

- Focused tests were added/updated for the first requested behavior, related edge cases, and review attempt 01 findings in `ScenarioExecutorSpec.groovy`. The required verification command was run after implementation and passed.

### Deviations From Plan

- No `REPORT_WRITE_FAILED` fallback report was added; the plan explicitly allowed keeping report-writing failure handling simple with non-zero CLI/stderr behavior when no secondary write path exists.

### Blockers / Follow-Ups

- None.
