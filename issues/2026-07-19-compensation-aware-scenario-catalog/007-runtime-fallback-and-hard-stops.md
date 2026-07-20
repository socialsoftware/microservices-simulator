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
