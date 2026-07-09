# Slice Review: 006 - Runtime Failures, Compensation Failures, Hard Stops, and Exit Codes

## Review Attempt

Attempt: `02`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes`

## Sources Reviewed

- Slice: `issues/2026-07-08-multi-saga-scenario-executor/006-runtime-failures-compensation-failures-and-exit-codes.md` (reviewed active path before PASS move)
- Spec: `issues/2026-07-08-multi-saga-scenario-executor/spec.md`
- Implementation plan: `issues/2026-07-08-multi-saga-scenario-executor/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`
- ADRs: `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`, `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`
- Completion evidence: `issues/2026-07-08-multi-saga-scenario-executor/006-runtime-failures-compensation-failures-and-exit-codes.md#completion-evidence`
- Changed files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorCli.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestrator.java`, `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`, `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestratorSpec.groovy`, `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java`, `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/MissingExecuteWorkflow.java`
- Prior review reports: `issues/2026-07-08-multi-saga-scenario-executor/review/006-runtime-failures-compensation-failures-and-exit-codes-review-01.md`
- Commands run by reviewer: `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test`; `git diff --check -- verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/MissingExecuteWorkflow.java issues/2026-07-08-multi-saga-scenario-executor/006-runtime-failures-compensation-failures-and-exit-codes.md`; dependency/done-path inspection with `find`/`test`

## Summary

The S6 implementation satisfies the slice contract. The previous blocking findings are fixed: scheduled-step dispatch/reflection failures now hard-stop as `UNEXPECTED_EXECUTION_FAILURE`, expected-not-injected hard-stop marking preserves already `REALIZED`/`MASKED_BY_SAGA_FAILURE` slots, and the one-participant runtime-failure path reports remaining forward steps as `SKIPPED_BY_SAGA_FAILURE`. Runtime failure continuation, compensation-failure continuation, expected-not-injected validation failures, hard-stop distinctions, and v3 exit-code mapping have focused coverage. Reviewer verification passed.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | pass | `ScenarioExecutor.java` distinguishes dispatch/reflection hard stops from invoked-body scheduled-step failures (`ScenarioExecutor.java:137-179`, `ScenarioExecutor.java:275-324`), compensates failed participants, skips remaining participant work, continues survivors, aggregates `COMPENSATED`/`PARTIAL_COMPENSATED`/`COMPENSATION_FAILED`, and hard-stops expected-not-injected/provider mismatch cases. |
| Slice out-of-scope respected | pass | No retry policy, compensation-step fault injection, new failure-policy modes, Quizzes smoke, or documentation changes were introduced; S7 owns Quizzes/docs. |
| Spec non-goals respected | pass | The implementation remains deterministic sequential saga/local replay, does not add true concurrency, distributed/TCC parity, impact scoring, search, or catalog rewriting. |
| Dependencies done | pass | `issues/2026-07-08-multi-saga-scenario-executor/done/` contains slices 001-005, and `execution-log.md` records PASS reviews/commits for dependencies. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-17 | pass | Runtime-failure and compensation-failure tests assert failed/compensation-failed participants are terminal and only survivor `right` closes (`FixtureWorkflow.CLOSURES == ['right']`). | Failed participants are filtered from final closure by terminal lifecycle outcomes. |
| AC-20 | pass | Tests cover `NOT_ASSIGNED`, `REALIZED`, `MASKED_BY_SAGA_FAILURE`, `NOT_REACHED`, and `EXPECTED_FAULT_NOT_INJECTED`; `markExpectedFaultNotInjected` preserves prior realized/masked slots and only annotates still-unreached assigned slots. | Dry-run `DRY_RUN` coverage remains from earlier slices. |
| AC-25 | pass | Expected-not-injected hard-stop tests assert later assigned slots are `NOT_REACHED`; materialization/startup hard-stop behavior from prior slices remains intact. | `participantStateReport` also normalizes still-unreached assigned slots with a hard-stop reason. |
| AC-26 | pass | Tests assert participant step status `EXPECTED_FAULT_NOT_INJECTED`, top-level terminal status `EXPECTED_FAULT_NOT_INJECTED`, fault-slot state `EXPECTED_FAULT_NOT_INJECTED`, and blocker reason. | Treated as validation hard stop, not compensated domain outcome. |
| AC-28 | pass | Single-saga runtime failure now aggregates to `COMPENSATED` with a failed step and skipped later step; multi-saga runtime failure tests cover partial compensation and all-compensated aggregation while survivors continue. | Dispatch/reflection failures are separately tested as hard stops. |
| AC-29 | pass | Multi-saga compensation failure test asserts left participant `COMPENSATION_FAILED`, failed participant skipped/masked later work, survivor commits/closes, and terminal status `COMPENSATION_FAILED`. | Compensation failure no longer stops survivor execution. |
| AC-30 | pass | Single- and multi-saga dispatch/reflection tests assert `UNEXPECTED_EXECUTION_FAILURE`, no compensation, no survivor continuation; existing provider mismatch/unexpected injected fault and closure hard-stop paths remain hard stops. | Configuration/report-write remain process exceptions/non-zero unless fallback reports are implemented, which the plan allowed. |
| AC-32 | pass | `multi-saga runtime failures that all compensate aggregate to compensated` asserts top-level `COMPENSATED`. | Single-saga runtime failure also returns `COMPENSATED`. |
| AC-33 | pass | `multi-saga non-assigned runtime failure compensates failed participant and lets survivor commit` asserts top-level `PARTIAL_COMPENSATED`. | Participant outcomes are compensated + committed. |
| AC-34 | pass | Compensation-failure test and exit-code test cover top-level `COMPENSATION_FAILED`. | CLI maps it non-zero. |
| AC-36 | pass | Tests assert participant lifecycle outcomes, step statuses, skipped steps, blockers, and vocabulary for runtime failure, compensation failure, expected-not-injected, and dispatch hard stops. | Single-saga runtime failure now reports the later unassigned forward step as skipped. |
| AC-37 | pass | Tests assert top-level terminal statuses, provider mode, fault-slot states, blockers, and assigned-vector behavior for S6 cases. | v3 report shape remains canonical from S1. |
| AC-40 | pass | `ScenarioExecutorSpec` now covers runtime exception continuation, all-compensated runtime failures, compensation-failure continuation, expected-not-injected hard stop, preservation of realized/masked slot states, dispatch/reflection hard stops, and exit mapping. | Reviewer reran the suite successfully. |
| AC-44 | pass | `ScenarioExecutorCli.exitCodeFor` test asserts zero only for `SUCCESS`, `COMPENSATED`, `PARTIAL_COMPENSATED`, and `DRY_RUN`; non-zero for `COMPENSATION_FAILED`, `CONFIGURATION_FAILED`, `REPORT_WRITE_FAILED`, `EXPECTED_FAULT_NOT_INJECTED`, `FAULT_PROVIDER_MISMATCH`, and representative hard stops. | Docker runner delegates to CLI process status; S7 will smoke Docker. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test` | pass | Reviewer reran it successfully: `Tests run: 57, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. |
| `git diff --check -- ...` | pass | No whitespace errors reported for the reviewed S6 source/test/slice files. |
| Runtime failure continuation evidence | pass | Tests assert `PARTIAL_COMPENSATED`, `COMPENSATED`, participant `FAILED` step outcomes, skipped failed-participant work, survivor commits, and provider cleanup. |
| Compensation failure continuation evidence | pass | Tests assert `COMPENSATION_FAILED`, participant-local `FORWARD_FAILURE`/`COMPENSATION_FAILED` blockers, survivor commits/closes, failed participant not closed, masked/skipped failed-participant work, and provider cleanup. |
| Expected-not-injected hard-stop evidence | pass | Tests assert simple hard-stop marking plus preservation of already `REALIZED`/`MASKED_BY_SAGA_FAILURE` slots while later unreached assigned slots become `NOT_REACHED`. |
| Dispatch/reflection hard-stop evidence | pass | `MissingExecuteWorkflow` fixture tests prove missing `executeUntilStep` hard-stops as `UNEXPECTED_EXECUTION_FAILURE` without compensation or survivor execution. |
| Configuration/report-writing failure evidence | pass | No fallback report writer was added; this matches the slice's simple non-zero process behavior. Exit-code helper covers represented `CONFIGURATION_FAILED`/`REPORT_WRITE_FAILED` status strings. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Changes are localized to executor failure handling and focused fixture/spec coverage. |
| Existing patterns | pass | Uses existing report construction, participant runtime state, fault-slot records, provider scope, and Spock fixture patterns. |
| Test quality | pass | Tests exercise behavior at the report and lifecycle-observable level rather than only implementation internals, including the prior review's edge cases. |
| Regression risk | pass | Hard-stop classification now protects malformed dispatch/reflection paths from being misreported as compensated domain outcomes; survivor continuation is covered for domain failures. |
| Security/data safety | n/a | No credentials, migrations, destructive operations, or persistent data behavior were introduced. |
| Change hygiene | pass | Source changes stay within S6 files plus a focused test fixture; unrelated workspace changes were not modified by the review. |

## Findings

None.

## Done Transition

- Moved to done: `yes`
- Done path: `issues/2026-07-08-multi-saga-scenario-executor/done/006-runtime-failures-compensation-failures-and-exit-codes.md`
- Reason if not moved: `None`

## Reviewer Notes

`docs/verifiers-impl/current-state.md` and `docs/verifiers-impl/reference/scenario-executor.md` still describe pre-feature single-saga/v2 behavior, but S7 explicitly owns Quizzes smoke and documentation updates, so this is not an S6 blocker.
