# Slice Review: 006 - Runtime Failures, Compensation Failures, Hard Stops, and Exit Codes

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`FAIL`

Moved to done: `no`

## Sources Reviewed

- Slice: `issues/2026-07-08-multi-saga-scenario-executor/006-runtime-failures-compensation-failures-and-exit-codes.md`
- Spec: `issues/2026-07-08-multi-saga-scenario-executor/spec.md`
- Implementation plan: `issues/2026-07-08-multi-saga-scenario-executor/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`
- ADRs: `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`, `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`
- Completion evidence: `issues/2026-07-08-multi-saga-scenario-executor/006-runtime-failures-compensation-failures-and-exit-codes.md#completion-evidence`
- Changed files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorCli.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestrator.java`, `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`, `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestratorSpec.groovy`, `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java`
- Prior review reports: `issues/2026-07-08-multi-saga-scenario-executor/review/001-v3-participant-report-and-single-saga-migration-review.md`, `issues/2026-07-08-multi-saga-scenario-executor/review/002-explicit-multi-saga-selection-and-dry-run-review.md`, `issues/2026-07-08-multi-saga-scenario-executor/review/003-multi-participant-materialization-and-startup-gates-review.md`, `issues/2026-07-08-multi-saga-scenario-executor/review/004-default-vector-interleaving-and-survivor-closure-review.md`, `issues/2026-07-08-multi-saga-scenario-executor/review/005-assigned-fault-compensate-and-continue-review.md`
- Commands run by reviewer: `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test`; `git diff --check -- verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy issues/2026-07-08-multi-saga-scenario-executor/006-runtime-failures-compensation-failures-and-exit-codes.md`; `rg "UNEXPECTED_EXECUTION_FAILURE|WORKFLOW_CLOSURE_FAILED|closure" verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor`

## Summary

The focused tests pass, and the main happy-path S6 behaviors for runtime failure continuation, compensation-failure continuation, expected-not-injected hard stop, and exit-code mapping are partly implemented. The slice cannot pass because the executor still conflates non-domain reflection/dispatch failures with scheduled-step domain outcomes, and expected-not-injected slot masking can overwrite already-realized/masked fault-slot states. There is also a single-saga report gap for skipped remaining forward steps after a runtime failure.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | fail | Runtime failures and compensation failures are implemented for the covered fixture cases, but reflection/dispatch failures inside scheduled-step dispatch are still treated as compensated domain outcomes at `ScenarioExecutor.java:137-160` and `ScenarioExecutor.java:256-287`, contrary to the slice's hard-stop distinction. |
| Slice out-of-scope respected | pass | No new failure-policy modes, retries, compensation-step faults, Quizzes smoke, or docs changes were introduced in the reviewed diff. |
| Spec non-goals respected | pass | The implementation remains verifier-owned, deterministic, sequential, and does not add distributed/TCC/search/scoring behavior. |
| Dependencies done | pass | Slices 001-005 are present under `issues/2026-07-08-multi-saga-scenario-executor/done/`, and `execution-log.md` records PASS reviews and commits for them. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-17 | pass | S6 multi-saga tests assert failed/compensation-failed participants are not closed again and only survivor `right` closes. | Covered for runtime-failure and compensation-failure paths. |
| AC-20 | fail | Runtime-failure masking and expected-not-injected states are tested, but `markExpectedFaultNotInjected` overwrites any assigned slot with greater slot index without preserving existing `REALIZED` or `MASKED_BY_SAGA_FAILURE` states (`ScenarioExecutor.java:429-435`). | Violates the slice/spec rule that hard-stop `NOT_REACHED` marking must not overwrite already terminal/masked slots. |
| AC-25 | pass | Existing S6 test asserts expected-not-injected marks a later assigned slot `NOT_REACHED`; materialization/startup hard-stop coverage from earlier slices remains intact. | The overwrite bug above still blocks AC-20/hard-stop hygiene. |
| AC-26 | pass | S6 test asserts `EXPECTED_FAULT_NOT_INJECTED` participant step status, terminal status, fault-slot state, and blocker reason. | Covered for the simple case. |
| AC-28 | fail | Fixture tests cover ordinary thrown scheduled-step exceptions, but reflection/dispatch failures in the same catch are also reported as `FAILED`/`COMPENSATED` instead of hard-stop `UNEXPECTED_EXECUTION_FAILURE` (`ScenarioExecutor.java:137-160`, `ScenarioExecutor.java:256-287`). | The spec distinguishes ordinary scheduled-step domain/simulator exceptions from infrastructure/reflection failures. |
| AC-29 | pass | S6 compensation-failure test asserts failed participant `COMPENSATION_FAILED`, survivor commits, failed participant later slot masks, and top-level `COMPENSATION_FAILED`. | Covered. |
| AC-30 | fail | Closure hard-stop code exists, and provider hard-stop tests exist, but scheduled-step reflection/dispatch failures are still domain-compensated rather than hard stops. `rg` found no current `UNEXPECTED_EXECUTION_FAILURE` coverage in `ScenarioExecutorSpec.groovy`. | Blocking hard-stop distinction issue. |
| AC-32 | pass | Test `multi-saga runtime failures that all compensate aggregate to compensated` asserts top-level `COMPENSATED`. | Covered. |
| AC-33 | pass | Test `multi-saga non-assigned runtime failure compensates failed participant and lets survivor commit` asserts top-level `PARTIAL_COMPENSATED`. | Covered. |
| AC-34 | pass | Test `multi-saga compensation failure does not stop surviving participant and aggregates to compensation failed` asserts top-level `COMPENSATION_FAILED`. | Covered. |
| AC-36 | fail | Multi-saga runtime-failure skipped steps are reported, but the single-saga runtime-failure test uses a later scheduled step (`ScenarioExecutorSpec.groovy:260-280`) and the report helper only derives skipped steps from assigned masked fault slots (`ScenarioExecutor.java:786-792`), so an unassigned remaining forward step is not reported as skipped. | The v3 participant model should make skipped forward work auditable for the one-participant case too. |
| AC-37 | pass | Top-level terminal/fault-slot status vocabulary is covered for the simple S6 cases. | Blocked indirectly by AC-20 overwrite bug. |
| AC-40 | fail | Targeted Spock coverage exists and passes, but it misses the reflection hard-stop case and the already-realized/masked preservation case for expected-not-injected hard stops. | Coverage gaps correspond to blocking implementation gaps. |
| AC-44 | pass | `ScenarioExecutorCli.exitCodeFor` returns zero only for `SUCCESS`, `COMPENSATED`, `PARTIAL_COMPENSATED`, `DRY_RUN`; tests include representative hard-stop/config/report statuses. | Report-writing/configuration failures are handled as process exceptions rather than fallback reports, which the plan allowed. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test` | pass | Reviewer reran it successfully: `Tests run: 54, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. |
| `git diff --check -- ...` | pass | No whitespace errors reported for the reviewed S6 source/test/slice files. |
| Runtime failure continuation evidence | pass | Tests assert `PARTIAL_COMPENSATED`, `COMPENSATED`, skipped/masked failed participant work, survivor commits, and provider cleanup. |
| Compensation failure continuation evidence | pass | Tests assert `COMPENSATION_FAILED`, participant blockers, survivor commits, failed participant not closed, and provider cleanup. |
| Expected-not-injected hard-stop evidence | partial | Simple hard-stop case is tested; preservation of already-realized/masked slots is not covered and implementation can overwrite them. |
| Configuration/report-writing failure evidence | partial | Exit-code helper covers status strings; no fallback report was implemented, so process exceptions remain the behavior. Acceptable under the plan, but not sufficient to offset the hard-stop implementation bug above. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Changes are mostly localized to `ScenarioExecutor.java` and `ScenarioExecutorSpec.groovy`. |
| Existing patterns | fail | The scheduled-step catch blocks now treat all `ReflectiveOperationException` values as participant domain failures. Existing closure handling already treats reflection/runtime closure failures as `UNEXPECTED_EXECUTION_FAILURE`, and S6 requires the same hard-stop distinction for non-body reflection failures. |
| Test quality | fail | Tests cover ordinary fixture body failures, but not reflection/dispatch failure hard stops or preservation of already-realized/masked slots under expected-not-injected. |
| Regression risk | fail | A malformed or incompatible runtime functionality can be reported as `COMPENSATED` or even aggregate incorrectly instead of a hard-stop setup/execution failure. |
| Security/data safety | n/a | No security/data migration behavior changed. |
| Change hygiene | pass | No broad refactor or unrelated source changes were reviewed in the S6 diff. |

## Findings

| Severity | Finding | Evidence | Required Fix |
|----------|---------|----------|--------------|
| blocking | Scheduled-step dispatch still treats reflection/infrastructure failures as compensated participant domain failures. The slice explicitly says `UNEXPECTED_EXECUTION_FAILURE` is for infrastructure/closure/reflection failures outside ordinary scheduled-step domain outcomes. | In the single-saga path, the catch at `ScenarioExecutor.java:137-160` catches `ReflectiveOperationException`; any non-`FaultVectorInjectedFaultException`, including `NoSuchMethodException`, `IllegalAccessException`, or `ClassNotFoundException`, records step `FAILED`, calls `compensate(...)`, and returns terminal `COMPENSATED`. The multi-saga path does the same at `ScenarioExecutor.java:256-287` and then continues. | Distinguish invocation-target/body exceptions from reflection/dispatch failures. Only exceptions thrown by the invoked scheduled-step body should become participant `FAILED` outcomes. Reflection/dispatch failures outside the body must hard-stop with `UNEXPECTED_EXECUTION_FAILURE` and blocker evidence. Add targeted tests for single- and/or multi-saga missing/inaccessible `executeUntilStep` or equivalent dispatch failure. |
| blocking | Expected-not-injected hard-stop marking can overwrite already-realized or already-masked assigned fault slots. | `markExpectedFaultNotInjected` rewrites every assigned slot with `slotIndex() > unrealizedSlotIndex` to `NOT_REACHED` (`ScenarioExecutor.java:429-435`) without checking the current `realizationState`. This violates the slice rule to mark later hard-stop slots `NOT_REACHED` unless already terminal/masked. | Preserve existing terminal states such as `REALIZED` and `MASKED_BY_SAGA_FAILURE`; only unexecuted assigned slots should become `NOT_REACHED`. Add a test where a previous participant slot is already `REALIZED` or `MASKED_BY_SAGA_FAILURE` before a later expected-not-injected hard stop, and assert it is not overwritten. |
| blocking | One-participant single-saga runtime failure reports do not record remaining forward steps as skipped. | The S6 single-saga test creates a later scheduled step after the failing step (`ScenarioExecutorSpec.groovy:260-280`), but does not assert `skippedSteps`. The single-saga report path returns after failure with only executed `outcomes` (`ScenarioExecutor.java:154-160`), and `participantEntry` derives skipped steps only from assigned masked fault slots (`ScenarioExecutor.java:786-792`), so unassigned later forward steps are omitted. | Report `SKIPPED_BY_SAGA_FAILURE` entries for remaining scheduled forward steps in the one-participant runtime-failure path, consistent with the multi-saga participant path, and add an assertion to the single-saga runtime-failure test. |

## Done Transition

- Moved to done: `no`
- Done path: `None`
- Reason if not moved: `FAIL` due blocking hard-stop/status and report-completeness findings.

## Reviewer Notes

Do not broaden this into a refactor. The smallest fix is to split scheduled-step body exceptions from reflection/dispatch exceptions in the existing catch blocks, preserve realized/masked slot states when applying expected-not-injected hard-stop marking, and add the missing focused Spock assertions.
