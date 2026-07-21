# Slice Review: 007 - Runtime Fallback and Hard Stops

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`FAIL`

Moved to done: `no`

## Sources Reviewed

- Slice: `issues/2026-07-19-compensation-aware-scenario-catalog/007-runtime-fallback-and-hard-stops.md`
- Spec: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Implementation plan: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `issues/2026-07-19-compensation-aware-scenario-catalog/decision-frame.md`
- ADRs: `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`, `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`
- Completion evidence: `007-runtime-fallback-and-hard-stops.md` — `## Completion Evidence` (`Status: implemented-awaiting-review`)
- Changed files reviewed: `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/Workflow.java`; `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowFunctionality.java`; `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowRecoveryCheckpoint.java`; `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowStepExecutionResult.java`; `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowStepRecoveryException.java`; `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/unitOfWork/SagaUnitOfWork.java`; `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/unitOfWork/SagaUnitOfWorkService.java`; `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaWorkflow.java`; `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/unitOfWork/SagaStepwiseRecoveryTest.java`; `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaExecutorControlTest.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioReportWriteException.java`; `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java`; `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`
- Prior review reports: `None`
- Commands run by reviewer:
  - `mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` from the repository root — failed immediately because this repository has no root Maven project; not treated as verification evidence
  - `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test`
  - `cd simulator && mvn -DskipTests install`
  - `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`
  - `cd verifiers && mvn -q dependency:build-classpath -Dmdep.outputFile=/tmp/verifiers-cp`
  - compiled and ran `/tmp/MultipleDeviationProbe.java` against verifier main/test classes
  - `git diff --check`
  - `git status --short` and review/done collision inspection

## Summary

The slice implements the main runtime boundary and most requested behavior: zero-bit `SimulatorException` body/commit fallback uses runtime unit-of-work checkpoints; runtime-only partial-step references and ordered explicit/implicit outcomes are reported; scheduled and fallback compensation throws hard-stop without automatic retry; successful operation markers remain retry-safe; and infrastructure/report failures use the required conformance distinction. Both required targeted suites pass.

The review still fails because a valid multi-participant run can encounter more than one zero-bit runtime fallback as survivors continue. `ScenarioExecutor` overwrites the report's singular deviation point on every such failure. The report therefore identifies the last deviation, not the action where actual execution first departed from the persisted schedule. This violates the decision frame and AC-32's requirement to identify the deviation point and leaves the multiple-fallback reporting case untested.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | fail | Fallback and hard-stop mechanics are present, but the completed trace does not preserve the first deviation point when more than one survivor later has an unassigned runtime failure. |
| Slice out-of-scope respected | pass | No retry loop, retry/backoff accounting, compensation fault slot, survivor continuation after compensation failure, or schedule mutation was introduced. |
| Spec non-goals respected | pass | No TCC/distributed/stream/gRPC parity, true concurrency, scoring/search, delayed commit, state-reset feature, or new dependency was added. |
| Dependencies done | pass | Required dependency `006-exact-persisted-action-replay.md` exists under `issues/2026-07-19-compensation-aware-scenario-catalog/done/`. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-31 | pass | `executeStepForExecutorControlled` separates invoked body failure from invocation-shape failure; controlled finalization preserves commit recovery state; executor tests cover immediate body/commit fallback, owner suffix skipping, and survivor continuation. | Runtime checkpoints include a partially failed executed step when unit-of-work state requires recovery. |
| AC-32 | fail | `ScenarioExecutor.java` assigns `deviationActionId`/`deviationPlannedPosition` unconditionally at both forward- and commit-failure branches. `/tmp/MultipleDeviationProbe.java` produced two valid zero-bit fallback outcomes but reported the second action at position `1` as the deviation point instead of the first action at position `0`. | Planned actions remain immutable and actual fallback actions are recorded, but the report-level deviation point is not truthful for multiple fallbacks. |
| AC-33 | pass | Non-`SimulatorException` body/commit failures, provider installation failure, selection, materialization/startup, and report-write paths remain infrastructure hard stops rather than domain outcomes. | Focused executor cases pass. |
| AC-34 | pass | Scheduled compensation, fallback explicit compensation, and fallback implicit rollback failures produce `COMPENSATION_FAILED / INCOMPLETE`, record the failed sub-outcome, stop survivors, and invoke the failed operation once. | Tests assert no survivor continuation. |
| AC-35 | pass | Simulator tests prove failed explicit work remains unmarked and later succeeds; explicit success plus implicit failure retries only the implicit rollback. Executor coverage proves no automatic retry. | The later retry is explicit and outside the attempt. |
| AC-36 | pass | Empty runtime recovery emits `ABORTED -> NO_COMPENSATION_WORK -> COMPENSATED` with no compensation action outcome; terminal guards remain in place. | Focused executor case passes. |
| AC-37 | pass | Report v4 carries workload/scenario/attempt identity, planned/actual actions, lifecycle, participant state, blockers, fault slots, terminal status, and new deviation/hard-stop metadata. | AC-32 blocks the correctness of one metadata value, not field presence. |
| AC-38 | pass | Required suites cover `EXACT`, completed `DEVIATED`, measured `INCOMPLETE`, and absent pre-measurement conformance; the multi-deviation probe still reports `DEVIATED`. | The incorrect point is assessed under AC-32. |
| AC-39 | pass | Runtime outcomes carry nullable planned positions/checkpoint ids, runtime occurrence ids, fault origin, exception details, and ordered explicit/implicit recovery sub-outcomes. | Scheduled and runtime-only recovery forms are both serialized. |
| AC-40 | pass | Tests cover unassigned, realized, masked, not-reached, participant-local deviation skips, and hard-stop survivor skips. | Runtime failure masking remains participant-local. |
| AC-41 | pass | Abort, automatic commit, compensated, no-work, and compensation-failed lifecycle events are emitted; body and commit outcomes remain distinct. | Focused body/commit cases pass. |
| AC-42 | pass | Fallback, scheduled compensation-failure, and report-write tests compare package checksums; execution writes only the separate report path or exposes the in-memory report on write failure. | Existing S6 package/dynamic-artifact alias guards remain intact. |
| AC-44 | fail | Dummyapp-style fixture coverage is broad, but it has no valid multiple-unassigned-fallback case and therefore did not catch the overwritten deviation point reproduced by the reviewer probe. | Add a regression that makes at least two participants fail at zero bits while fallback continuation remains valid. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` | pass | Reviewer rerun: `Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `cd simulator && mvn -DskipTests install` | pass | Refreshed the standalone verifier module's local simulator dependency; Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | pass | Reviewer rerun: `Tests run: 55, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| Multiple-fallback deviation-point probe | fail | Planned actions were `[51a79e..., 95ced9...]`; both actual actions failed with valid zero-bit fallback, but report metadata was `deviation=95ced9...@1` while the first departure was `51a79e...@0`. |
| `git diff --check` | pass | No whitespace errors. |
| Browser/manual evidence | n/a | No browser behavior is in scope; serialized/runtime behavior was inspected through tests and the reviewer probe. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Changes stay within the executor/report boundary, narrow Saga runtime controls, focused fixture behavior, and tests. |
| Existing patterns | pass | The implementation reuses the v3 package reader, in-memory fault boundary, Saga unit-of-work truth, controlled finalization, and action-aware report model. |
| Test quality | fail | Existing tests assert useful observable bodies, recovery order, retry markers, lifecycle, skips, serialization, and checksums, but omit the valid multiple-fallback continuation case that exposes the blocking metadata bug. |
| Regression risk | fail | A downstream scorer/audit consumer can be given the wrong divergence location whenever more than one participant experiences zero-bit fallback in one completed attempt. |
| Security/data safety | pass | Package artifacts remain protected from report output aliasing and are checksum-verified on the new execution paths. |
| Change hygiene | pass | No unrelated product/docs refactor or new dependency was introduced; unrelated untracked meeting notes were left untouched. |

## Findings

| Severity | Finding | Evidence | Required Fix |
|----------|---------|----------|--------------|
| blocking | Completed traces overwrite the first schedule-deviation point when more than one participant has a zero-bit runtime fallback. The report's singular deviation metadata then points to the last fallback instead of the action where actual execution first departed from the persisted FaultScenario. | `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java:295-296` and `:341-342` unconditionally replace `deviationActionId` and `deviationPlannedPosition`. `/tmp/MultipleDeviationProbe.java` ran an all-zero two-participant scenario where both first bodies threw `SimulatorException`; actual outcomes retained both failures, but metadata reported action 2 at planned position 1 rather than action 1 at position 0. The decision frame says the report identifies “the deviation point,” and AC-32 requires that point. | Preserve the first non-null deviation action/position for the whole attempt (or introduce an explicitly specified ordered collection while retaining the first divergence as the conformance point). Add a `ScenarioExecutorSpec` regression with at least two valid zero-bit fallbacks and assert first-deviation metadata, both per-action `UNASSIGNED_RUNTIME` origins, completed `DEVIATED` conformance, and continued execution semantics. |

## Done Transition

- Moved to done: `no`
- Done path: `None`
- Reason if not moved: `FAIL` — blocking AC-32/AC-44 deviation-point reporting defect remains.

## Reviewer Notes

The initial root-level Maven invocation was a reviewer command error caused by this repository's deliberate lack of a root aggregator. The required module-scoped command was rerun correctly and passed; only that successful run is used as verification evidence.
