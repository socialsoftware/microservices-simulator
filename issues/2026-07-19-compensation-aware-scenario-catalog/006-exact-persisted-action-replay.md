# 006 - Exact Persisted Action Replay

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `004-materializable-eager-baseline-and-accounting.md`  
ACs covered: `AC-2, AC-9, AC-14, AC-25, AC-26, AC-27, AC-28, AC-29, AC-30, AC-36, AC-37, AC-38, AC-39, AC-40, AC-41, AC-42, AC-44`  
Risk: `high`

## Purpose

Migrate the executor from `{ScenarioPlan, vector overlay}` to one persisted FaultScenario action contract and prove exact assigned-fault replay, stepwise compensation, automatic commit, lifecycle reporting, and artifact immutability.

## Scope

- Remove the temporary executor-only v2 bridge inventoried by S2, load one v3 package, and select one persisted FaultScenario id; reject v2 packages and remove/reject direct execution-time vector overlays from Java options/CLI, script, and Compose.
- Resolve the referenced WorkloadPlan and validate all references, ownership, unique action identities, enabling conditions, reverse compensation order, participant-local legality, and residual global forward order before materialization.
- Preserve the all-or-nothing materialization/startup gate for every participant before measured actions.
- Execute forward actions through the existing in-memory fault assignment boundary using persisted WorkloadPlan slots and FaultScenario bits.
- Add a narrow executor-oriented simulator pre-body abort transition that marks the workflow aborted/compensatable without invoking the target body or adding that target to executed steps; use it when an assigned bit realizes, then skip the owner’s remaining forwards and continue exactly in persisted action order.
- Advance each planned compensation action once through a narrow simulator stepwise recovery operation; compensation actions have no fault slots.
- Add controlled executor finalization for the participant’s final successful forward action: run body then close/commit as separate reported phases, expose commit failure/cause without automatically invoking opaque whole-Saga abort, and leave recovery state available for S7. Run it immediately at that action and report `COMMITTED` only after commit succeeds.
- Transition abort-with-no-checkpoints directly to `COMPENSATED` and emit `NO_COMPENSATION_WORK`; prevent double close/compensation of terminal participants.
- Introduce the new action-aware report schema with package/scenario/attempt identity, vector/slots, planned and actual actions, participants, lifecycle events, positions, outcomes, evidence, blockers, final state, terminal status, and `EXACT`/absent conformance for this slice.
- Keep all package, accounting, and enrichment bytes unchanged during dry-run/preparation/execution.

## Out of Scope

- Zero-bit domain/simulator fallback and compensation-failure detail beyond the hard-stop foundation completed in S7.
- On-demand mutation.
- V2 report compatibility.

## Repo Anchors

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioCatalogReader.java`, `ScenarioExecutor.java`, `ScenarioExecutorOptions.java`, `ScenarioExecutorCli.java`, and `ScenarioExecutionReport.java` — clean migration surface.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioMaterializer.java` and `ScenarioExecutorReadinessEvaluator.java` — preparation behavior.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowFunctionality.java`, `Workflow.java`, and `.../transaction/sagas/workflow/SagaWorkflow.java` — current execution/abort/finalization boundary requiring narrow executor controls while ordinary APIs remain stable.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaStep.java` — currently records a step before its body, which the assigned-fault control must bypass.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/unitOfWork/SagaUnitOfWorkService.java` — reverse step abort and implicit rollback.
- `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaExecutorControlTest.java` and `.../unitOfWork/SagaStepwiseRecoveryTest.java` — named focused tests to introduce; `simulator/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/sagas/workflow/WorkflowExecutionPlanTest.groovy` remains the ordinary Saga regression anchor.
- `verifiers/src/test/java/.../executor/FixtureWorkflow.java` and `.../ScenarioExecutorSpec.groovy` — runtime fixture/report assertions.
- `docker-compose.yml` and `verifiers/scripts/run-scenario-executor.sh` — persisted-id invocation contract.

## Implementation Shape

- Replace the executor’s catalog/vector inputs atomically at the contract level; remove the S2 legacy bridge and do not keep the v2 reader as fallback.
- Keep pure package/action validation separate from runtime reflection/materialization so failures before measured execution have absent conformance and no actual actions.
- Add only the minimal executor-oriented simulator controls/results needed to (a) enter abort before a target body without recording it, (b) finalize/commit without opaque automatic compensation on failure, and (c) advance one source recovery step with ordered explicit-compensation/implicit-rollback truth. Preserve existing `execute`, `resumeWorkflow`, and `resumeCompensation` behavior for non-executor callers.
- Track planned index and actual index independently. Exact conformance requires completion of the entire persisted action list in order.
- Bind the fault boundary to persisted slot identity and assigned bit, not CLI input.
- Generate execution-attempt identity independently from stable FaultScenario identity.

## TDD / Test Shape

- First behavior to test: an all-zero persisted FaultScenario executes its complete action list, commits each participant at its final forward action, and reports `EXACT` with no compensation actions.
- Expected red failure: current executor reads v2 plans, accepts a vector overlay, closes survivors only after the whole schedule, and has no action report.
- Additional coverage: v2 rejection; missing/dangling/misowned references; duplicate actions; compensation before enablement; wrong reverse order; residual forward reorder; materialization/startup gate with zero measured actions; assigned pre-body body-not-called and target absent from executed steps; workflow is compensation-eligible after pre-body abort; exact interleaving; one compensation advance per action; successful controlled automatic commit; commit-domain failure exposes body-success/commit-failure and leaves checkpoints unconsumed; final assigned fault no body/commit; no-work event; terminal idempotence; fault-slot state matrix; skipped forwards; new report serialization; dry-run/execution package checksums; no production v2/vector-overlay references after migration.

## Just-in-Time Preflight Required

Before source edits, the executor must re-check:

- S2/S3/S4 final package, action, validator, and manifest contracts.
- Current simulator behavior when `executeUntilStep` throws before/inside the body, `SagaStep` records execution, and `resumeWorkflow` commits/automatically aborts on failure.
- Current fault provider identity fields and cleanup tests.
- The exact S2 legacy-island inventory plus all CLI/script/Compose callers that still supply `FAULT_VECTOR` or a v2 catalog path; define documented historical/test exclusions for the final production zero-reference check.

## Verification

- `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` — pre-body target absence/abort eligibility, controlled finalization, one-checkpoint recovery, and ordinary Saga compatibility pass.
- `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test` — exact persisted replay, validation, report, and immutability pass.
- Repository-wide production-reference inspection — no S2 legacy executor reader, enriched-catalog execution, `scenario-catalog.jsonl`, `--fault-vector`, or `FAULT_VECTOR` surface remains outside explicitly documented historical/test fixtures.

## Evidence to Record

- files changed
- commands run and outputs
- planned versus actual action trace
- report schema/id/conformance/lifecycle sample
- package checksums before/after
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- Current simulator marks a step executed before its body and only marks abort after an execution exception. The new pre-body transition must do neither target-step execution nor target-step registration while still making prior checkpoints compensatable.
- Current `Workflow.resume` performs opaque abort on commit failure. Executor-controlled finalization must prevent that recovery consumption while preserving ordinary `resumeWorkflow` behavior.
- Automatic commit changes current multi-Saga closure timing. Ensure a participant closes exactly once at its own final successful forward action.
- Reflection/invocation failures must not be mislabeled as assigned domain faults.
