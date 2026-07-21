# 007 - Runtime Fallback and Hard Stops

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `006-exact-persisted-action-replay.md`  
ACs covered: `AC-31, AC-32, AC-33, AC-34, AC-35, AC-36, AC-37, AC-38, AC-39, AC-40, AC-41, AC-42, AC-44`  
Risk: `high`

## Purpose

Complete truthful runtime semantics when actual domain execution deviates from the persisted schedule or the experiment becomes invalid: immediate checkpoint-level fallback for zero-bit domain/simulator failure, and hard stops for infrastructure or any compensation failure.

## Scope

- Classify invoked forward-body and close/commit domain/simulator exceptions at zero bits as `UNASSIGNED_RUNTIME`; keep reflection, provider, configuration, invocation-shape, selection, package, preparation, and report failures as infrastructure.
- At a zero-bit domain/simulator failure, abort the participant, skip its remaining forwards, run exactly one immediate fallback recovery episode from simulator unit-of-work truth, and continue still-valid planned actions of other participants.
- Support runtime-only recovery references for a partially failed step that the unit of work marked executed, with nullable planned checkpoint id.
- Report one runtime recovery action per source step/checkpoint and ordered explicit-compensation then implicit Saga-state rollback sub-outcomes when both apply; report explicit work for a partially failed step only if registration actually completed.
- Mark completed fallback traces `DEVIATED`, record deviation point/policy/fault origin, and never rewrite the persisted FaultScenario.
- If any scheduled or fallback recovery action/sub-outcome throws, set terminal status `COMPENSATION_FAILED`, conformance `INCOMPLETE`, retain the exact/deviated prefix and hard-stop action/reason, execute nothing further, and perform no automatic retry.
- Preserve simulator retryability by marking explicit compensation/implicit rollback complete only after each corresponding operation succeeds.
- Mark infrastructure hard stops after measured execution `INCOMPLETE`; leave conformance absent when no measured action began.
- Complete lifecycle/fault-slot/skipped-action reporting for deviation and partial traces.

## Out of Scope

- Retry loops, retry counts/backoff, compensation fault injection, scorer eligibility implementation, or recovery schedule mutation.
- Continuing survivors after a compensation failure.
- Treating infrastructure failure as a domain outcome.

## Repo Anchors

- Exact action runner/report state and executor-oriented pre-body abort/controlled-finalization contract from S6.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaStep.java` — executed-before-body and compensation-registration-after-success behavior.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/unitOfWork/SagaUnitOfWork.java` — runtime-only current/executed steps and explicit-action executed marker.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/unitOfWork/SagaUnitOfWorkService.java` — implicit rollback and aborted-step marker ordering.
- `verifiers/src/test/java/.../executor/FixtureWorkflow.java` — add deterministic body, commit, explicit compensation, implicit rollback, and infrastructure failure probes without application-specific shortcuts.
- `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaExecutorControlTest.java`, `.../unitOfWork/SagaStepwiseRecoveryTest.java`, and existing `.../sagas/workflow/WorkflowExecutionPlanTest.groovy` — controlled commit/fallback, retry, and ordinary Saga regression tests.
- Existing `ScenarioExecutorSpec.groovy` failure-policy matrix.

## Implementation Shape

- Classify based on failure boundary and cause type, not message text. An exception thrown by the invoked domain/simulator operation is distinct from failure to find/invoke that operation.
- Consume S6’s controlled finalization result for commit-domain fallback; never call ordinary `resumeWorkflow` on the executor path where its opaque automatic abort would consume recovery before reporting.
- Ask the simulator for checkpoint-granular recovery progress; do not infer explicit registration or implicit rollback from static checkpoints when runtime state disagrees.
- Preserve the persisted planned list and record actual fallback actions separately.
- Maintain independent progress markers for explicit compensation and implicit rollback so a throw does not falsely mark work complete and a later explicit invocation can retry it.
- Hard-stop immediately on the first failing recovery sub-outcome; do not run the remaining sub-outcomes/actions/survivor forwards.
- Report-writing failure remains non-domain even though a report may not be persistable; tests may inspect the in-memory classification where feasible.

## TDD / Test Shape

- First behavior to test: a zero-bit body failure after the runtime records the failing step produces a runtime-only fallback recovery action, marks `DEVIATED`, and then executes a surviving participant’s next valid planned action.
- Expected red failure: S6 only supports exact assigned-fault replay and current executor’s opaque compensation cannot report checkpoint/sub-outcome truth.
- Additional coverage: zero-bit commit domain failure through controlled finalization with checkpoints still available and no pre-report compensation consumption; commit reflection/invocation-shape hard stop; partially failed step before/after explicit registration; explicit+implicit ordered sub-outcomes; no-work fallback; scheduled compensation throw; fallback explicit throw; fallback implicit throw; no survivor continuation after throw; later explicit retry succeeds; exactly one invocation/no automatic retry; provider/config/selection/package/preparation/report failures; `EXACT`/`DEVIATED`/`INCOMPLETE`/absent matrix; all artifact checksums unchanged; ordinary `resumeWorkflow` regression remains unchanged.

## Just-in-Time Preflight Required

Before source edits, the executor must re-check:

- S6’s final action state machine and report model.
- Actual simulator exception types (`SimulatorException`, completion wrappers, command gateway failures) and reflection wrapper behavior.
- Stepwise recovery marker ordering after S6 to ensure failed operations remain retryable.
- Existing tests whose superseded policy currently continues survivors after compensation failure.

## Verification

- `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` — controlled commit failure, unconsumed checkpoint truth, marker/retry behavior, and ordinary Saga regression pass.
- `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` — fallback, classification, conformance, hard-stop, lifecycle, slot, and immutability matrix passes.

## Evidence to Record

- files changed
- commands run and outputs
- exact/deviated/incomplete sample traces
- scheduled and fallback compensation-failure evidence
- retry-marker/no-auto-retry evidence
- artifact checksums
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- Current `abortUntilStep` performs explicit and implicit work opaquely. Do not report guessed sub-outcomes; adjust the narrow simulator boundary first.
- A domain exception during commit occurs after a successful body but before `COMMITTED`; preserve both phase outcomes and use S6 controlled finalization so opaque ordinary-workflow abort has not already consumed recovery.
- Do not continue any measured action after compensation failure, even though the superseded v2/current policy did.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Added a controlled forward-body result boundary and simulator-owned reverse runtime-recovery checkpoint discovery so the executor distinguishes invoked `SimulatorException` domain failures from invocation-shape/infrastructure failures and recovers from unit-of-work truth.
- Implemented immediate zero-bit body/commit fallback with runtime-only recovery references, nullable planned checkpoint ids, ordered explicit/implicit sub-outcomes, no-work lifecycle handling, survivor continuation, deviation metadata, runtime masking/skips, and no package mutation.
- Made scheduled and fallback recovery failures first-failure hard stops with `COMPENSATION_FAILED / INCOMPLETE`, failed sub-outcome details, hard-stop metadata, survivor hard-stop skips, no automatic retry, and retry-safe explicit/implicit simulator markers.
- Completed infrastructure classification/reporting for body/commit shape failures, selection, preparation, provider installation, and report writing; report-write failure now exposes the in-memory incomplete trace through `ScenarioReportWriteException` when persistence is impossible.
- After review attempt 01, singular deviation metadata is write-once: later valid zero-bit fallbacks retain their per-action outcomes but cannot replace the first action/position where execution departed from the persisted schedule.

### Files Changed

- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/Workflow.java` — added controlled executor body execution and runtime-recovery checkpoint inspection boundaries.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowFunctionality.java` — exposed the new narrow controls.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowRecoveryCheckpoint.java` — added simulator-owned pending recovery truth.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowStepExecutionResult.java` — added invoked-body success/failure result.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowStepRecoveryException.java` — retained completed recovery-prefix and failed sub-outcome details.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/unitOfWork/SagaUnitOfWork.java` — exposed pending explicit/implicit recovery markers without changing ordinary Saga APIs.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/unitOfWork/SagaUnitOfWorkService.java` — added reverse pending-checkpoint discovery and success-only explicit/implicit completion markers.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaWorkflow.java` — delegated checkpoint inspection through the Saga-only executor boundary.
- `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaExecutorControlTest.java` — covered body-versus-shape boundary behavior while retaining controlled finalization tests.
- `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/unitOfWork/SagaStepwiseRecoveryTest.java` — covered reverse runtime truth, failed explicit retry, and explicit-success/implicit-failure retry markers.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — implemented fallback, classification, hard stops, conformance, lifecycle, slot/skip, immutability, and report-write failure behavior.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java` — added deviation/hard-stop metadata, nullable runtime positions/occurrences, and failed recovery sub-outcome details.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioReportWriteException.java` — exposes the in-memory infrastructure-failure report when writing fails.
- `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java` — added deterministic body, explicit registration/compensation, implicit rollback, and infrastructure probes.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — added the fallback, compensation-failure, classification, conformance, lifecycle, retry, skip/slot, serialization, and checksum matrix.
- `issues/2026-07-19-compensation-aware-scenario-catalog/007-runtime-fallback-and-hard-stops.md` — recorded completion evidence.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` before implementation | EXPECTED FAIL | Test compilation reported 15 missing symbols for controlled body results, recovery checkpoints, recovery failure details, and the new controls. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` before implementation | EXPECTED FAIL | Existing S6 behavior produced 9 assertion failures and 1 fixture error across the new fallback/classification/hard-stop cases. |
| `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` | PASS | Final required run: `Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. The OTLP exporter logged a non-test connection warning after assertions; the Maven run passed. |
| `cd simulator && mvn -DskipTests install` | PASS | Refreshed the local simulator artifact consumed by the standalone verifier module; Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | PASS | Initial implementation run: `Tests run: 55, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` after adding the review regression, before the fix | EXPECTED FAIL | `Tests run: 56, Failures: 1`; the two-fallback body/commit case reported the second action at position 1 instead of the first action at position 0. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` after review fix | PASS | Fresh required run: `Tests run: 56, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorOrchestratorSpec test` | PASS | Supplemental invocation regression: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| Serialized trace and checksum inspection under `/tmp/v3-executor-package15321752176929031200` | PASS | `DEVIATED` trace is `PARTIAL_COMPENSATED`; actual order is forward, forward, failed forward, runtime implicit rollback, runtime explicit compensation, survivor forward. Five package SHA-256 values remained `b14c9a...`, `7a91de...`, `636dc9...`, `55b6b0...`, and `e3b0c4...`. |
| `git diff --check` | PASS | No whitespace errors. |

### Acceptance Criteria Evidence

- AC-31 and AC-32: zero-bit `SimulatorException` body and controlled-commit failures abort once, recover immediately from reverse unit-of-work checkpoints, and continue a valid survivor. `/tmp/v3-executor-package15321752176929031200/reports/deviated-runtime-fallback.json` records `DEVIATED`, the deviation point/policy, a nullable-checkpoint runtime-only partial-step rollback, a planned-checkpoint explicit compensation, masked/skipped owner suffix, and unchanged package hashes. The post-review regression runs a body fallback at planned position 0 followed by a commit fallback at position 1 and proves report-level deviation metadata remains fixed at the first action while both action outcomes retain `UNASSIGNED_RUNTIME`.
- AC-33: non-`SimulatorException` body/commit failures hard-stop as infrastructure without fallback; selection, materialization, startup/reflection, provider-installation, CLI configuration, package-schema, and report-write paths remain non-domain and have absent conformance before measured actions or `INCOMPLETE` afterward.
- AC-34: scheduled explicit, fallback explicit, and fallback implicit failures stop at the first failing sub-outcome, emit `COMPENSATION_FAILED` lifecycle/final state plus hard-stop metadata, mark survivor forwards `NOT_EXECUTED_HARD_STOP`, and execute no later action. `/tmp/v3-executor-package5726674303669120343/reports/scheduled-compensation-failed.json` is the serialized scheduled-failure sample.
- AC-35: simulator tests prove failed explicit compensation is not marked executed and succeeds on a later explicit invocation; explicit success followed by implicit failure retains only the explicit marker and retries only rollback. Executor counters prove one attempt and no automatic retry.
- AC-36: zero-work runtime fallback emits `ABORTED -> NO_COMPENSATION_WORK -> COMPENSATED` with no recovery action; existing exact terminal guards remain covered.
- AC-37 through AC-41: report v4 now carries runtime occurrence/checkpoint references, ordered successful/failed recovery sub-outcomes, fault origin, deviation/hard-stop fields, `EXACT / DEVIATED / INCOMPLETE / absent` conformance, lifecycle events, masked/not-reached slots, participant-local deviation skips, and hard-stop skips. Exact sample: `/tmp/v3-executor-package8097094936401475169/reports/execution-report.json`.
- AC-42: fallback and compensation-failure tests compare all five package artifact checksums before/after; report-write failure also leaves package bytes unchanged and exposes only an in-memory `REPORT_WRITE_FAILED / INCOMPLETE` report.
- AC-44: the simulator controls and dummyapp-style `FixtureWorkflow` provide deterministic automated coverage without application-specific executor shortcuts, including two valid zero-bit fallbacks in one completed multi-participant attempt.

### Review Attempt 01 Fix Evidence

- Fixed only the blocking first-deviation reporting finding in `ScenarioExecutor`: both body- and commit-fallback branches now assign `deviationActionId` and `deviationPlannedPosition` only when no earlier deviation has been recorded.
- Added one `ScenarioExecutorSpec` regression with a left participant body fallback followed by a right participant commit fallback. It asserts `DEVIATED`, first action/position metadata, both planned failure outcomes with `UNASSIGNED_RUNTIME`, completed recovery, continued second-participant execution, and both participants ending `COMPENSATED`.
- TDD red evidence reproduced the review defect exactly: the new case reported action `95ced9...` at position 1 instead of first action `51a79e...` at position 0. The guarded assignments made all 56 executor cases pass.

### Browser / Manual Evidence

- Browser evidence was not required. Serialized exact, deviated, and incomplete reports plus package SHA-256 values were inspected directly at the paths above.

### TDD Notes

- Added the simulator boundary/recovery tests first and observed the expected 15-symbol compilation failure, then implemented the narrow controls and drove all 17 required simulator tests green.
- Added the verifier fallback, failure, retry, classification, reporting, and immutability matrix against S6 first; observed the expected 9 failures plus 1 fixture error, then implemented the executor/report behavior. The report-write test separately failed on the missing `ScenarioReportWriteException` before that infrastructure contract was added.
- For review attempt 01, added the two-fallback regression first, observed the expected last-deviation overwrite, then made only the two existing deviation assignments conditional on the report-level point still being absent.

### Deviations From Plan

- None. No retry loop, compensation fault slot, survivor continuation after compensation failure, package mutation, new dependency, or live-documentation update was introduced.

### Blockers / Follow-Ups

- None. Ready for `sp-review-slice`; the active slice remains at `issues/2026-07-19-compensation-aware-scenario-catalog/007-runtime-fallback-and-hard-stops.md`.
