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

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Replaced the executor-only v2 bridge with one linked v3 package reader and mandatory persisted FaultScenario-id selection. Java options, CLI/orchestrator, runner script, and Compose no longer accept an execution-time vector; the CLI rejects that former flag as an unsupported option.
- Extended pure FaultScenario validation with complete action-schedule semantics: exact residual global forward order, assigned-failure enablement, failed-owner omission, reverse compensation queues, complete recovery, unique action identities, and owned source references are checked before materialization.
- Added narrow simulator controls for assigned pre-body abort, controlled commit/finalization, and one-checkpoint Saga recovery with explicit-compensation/implicit-rollback truth while preserving ordinary workflow APIs.
- Replayed persisted forward/compensation actions through the existing in-memory assignment boundary, committed each participant at its final successful workload forward, skipped failed-owner suffixes, advanced each persisted compensation once, and handled no-checkpoint aborts without double close/recovery. After review attempt 01, forward replay uses a narrow exact-step operation that runs only the named dependency-ready step and rejects unmet or already-attempted sequencing without executing ready siblings.
- Replaced report v3 with action-aware report v4 containing package/workload/scenario/attempt identity, vector slots, planned and actual positions, body/commit outcomes, compensation evidence/sub-outcomes, lifecycle events, skipped forwards, participant final states, conformance, blockers, and terminal status. Complete replay is `EXACT`; dry-run/preparation failures omit conformance; controlled commit failure retains body success and reports an `INCOMPLETE` hard-stop foundation for S7.
- Proved package and dynamic-enrichment immutability during dry-run and measured execution with before/after checksums in the executor spec. After review attempt 01, output paths are rejected before preparation when they normalize to or reference the same file as the manifest or any linked workload, FaultScenario, accounting, or rejected-input artifact. After review attempt 02, any existing artifact recognized by the authoritative v3 dynamic sidecar, sidecar-manifest, or join-report schema is also protected regardless of its configured location or normalized/symbolic/hard-link alias.

### Files Changed

- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/ExecutionPlan.java` — added exact one-named-step execution with dependency/already-attempted guards while preserving ordinary prefix execution.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/Workflow.java` — added exact forward-step, pre-body abort, controlled finalization, and base step-recovery controls.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowFunctionality.java` — exposed the narrow executor controls to Saga functionalities.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowFinalizationResult.java` — added controlled commit result/cause contract.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowStepRecoveryResult.java` — added one-checkpoint recovery truth contract.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaWorkflow.java` — wired stepwise executor recovery for aborted Sagas.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/unitOfWork/SagaUnitOfWork.java` — exposed retry-safe explicit-compensation execution state.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/unitOfWork/SagaUnitOfWorkService.java` — added exact one-source-step explicit-then-implicit recovery.
- `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaExecutorControlTest.java` — covered exact branched-step isolation and invalid-sequence rejection in addition to pre-body target absence, abort eligibility, controlled commit, and unconsumed commit-failure recovery state.
- `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/unitOfWork/SagaStepwiseRecoveryTest.java` — covered one-checkpoint order, outcome truth, and retry markers.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/CatalogScenarioRecord.java` — removed the temporary legacy v2 plan/fault-space bridge.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioCatalogReader.java` — migrated to linked v3 package loading only.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOptions.java` — replaced catalog/vector inputs with package path and persisted FaultScenario id.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — implemented preparation-gated exact action replay through the exact-step control, assignment boundaries, stepwise recovery, automatic commit, lifecycle accounting, conformance, immutable package consumption, normalized/same-file package collision rejection, and location-independent recognition of existing v3 dynamic-enrichment outputs.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java` — introduced action-aware report v4.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorCli.java` — required `--package-path` plus `--fault-scenario-id` and rejected unsupported executor flags.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestrator.java` — migrated forked invocation to package/persisted-id inputs.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/FaultScenarioValidator.java` — added pure enabling/reverse/residual-order/completeness validation.
- `verifiers/scripts/run-scenario-executor.sh` and `docker-compose.yml` — replaced legacy catalog/scenario/vector environment inputs with `PACKAGE_PATH` and `FAULT_SCENARIO_ID`.
- `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java` — migrated the runtime fixture to a real branched SagaWorkflow with observable bodies/checkpoints.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — replaced v2 expectations with persisted replay, branched exact-step isolation, validation, report, lifecycle, gate, package/dynamic-enrichment output alias, and checksum coverage.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestratorSpec.groovy` — covered the new invocation surface.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogJsonlWriterSpec.groovy` — removed the obsolete expectation that the temporary executor rejects v3.
- `issues/2026-07-19-compensation-aware-scenario-catalog/006-exact-persisted-action-replay.md` — recorded this completion evidence.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` before simulator implementation | EXPECTED FAIL | Test compilation reported 21 missing symbols for the new pre-body, finalization, recovery-result, and compensation-state contracts. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` before executor migration | EXPECTED FAIL | The desired persisted-package options constructor was absent and the old executor contract could not run the new v3 replay fixture. |
| `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` | PASS | Fresh post-review-fix required run: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec,EnrichedScenarioCatalogWriterSpec test` | PASS | Fresh post-review-02 superset of required executor verification plus authoritative dynamic-writer schemas: `Tests run: 49, Failures: 0, Errors: 0, Skipped: 0` (`44` executor, `3` orchestrator, `2` writer); Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec,RecoveryScheduleGeneratorSpec,ScenarioCatalogJsonlWriterSpec test` | PASS | Supplemental replay/generator/package regression: `Tests run: 45, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec test` | PASS | S5 package-mutation regression after stricter action validation: `Tests run: 36, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `bash -n verifiers/scripts/run-scenario-executor.sh` plus executable-bit check | PASS | Runner syntax is valid and the script remains executable. |
| `docker compose config --quiet` | PASS | Updated `scenario-executor` environment is valid Compose configuration. |
| Production-reference inspection with `rg --pcre2` | PASS | No legacy plan/fault-space class, v2 schema, exact old `scenario-catalog.jsonl`, enriched executor catalog, `--fault-vector`, `FAULT_VECTOR`, or `CATALOG_PATH` production surface remains. |
| `git diff --check` | PASS | No whitespace errors. |

### Acceptance Criteria Evidence

- AC-2: `ScenarioCatalogPackageReader` is now the only executor reader; the v2-selection test observes the clear unsupported-v3-package diagnostic.
- AC-9 and AC-14: the all-zero two-participant test executes only its four persisted forwards even though the runtime fixture has an additional ready branch, commits `left` at planned position 2 and `right` at position 3, emits two `AUTOMATIC_COMMIT` events, and reports `SUCCESS / EXACT`.
- AC-25: package reading plus `WorkloadPlanValidator`/expanded `FaultScenarioValidator` rejects malformed references, ownership, duplicate actions, premature compensation, wrong reverse order, incomplete recovery, and residual reorder before preparation. API/CLI/script/Compose accept a persisted id only.
- AC-26: separate materialization- and startup-failure tests retain zero actual actions and absent conformance; startup is not attempted until every participant materializes.
- AC-27 through AC-29: the persisted vector `01100` trace is exactly `FORWARD left:first`, assigned pre-body `FORWARD left:second`, `FORWARD right:first`, `COMPENSATION left:first`, `FORWARD right:second`; actual ids/positions equal planned ids/positions and the compensation advances once. A separate branched `001` case proves requesting `second` does not execute ready sibling `third`, and the later assigned `third` remains body-not-run and absent from executed steps.
- AC-28: simulator and executor tests prove the assigned target body is not called, is absent from `SagaUnitOfWork.executedSteps`, and prior checkpoints remain recoverable.
- AC-30: successful final bodies report separate successful commit phases; a final assigned fault reports body/commit `NOT_RUN`; controlled commit failure reports body `SUCCEEDED`, commit `FAILED`, and leaves checkpoints unconsumed.
- AC-36: first-step failure emits `ABORTED`, `NO_COMPENSATION_WORK`, and `COMPENSATED` with zero compensation outcomes. Commit/compensation invocation counters prove terminal participants close/recover once.
- AC-37 through AC-41: report v4 serialization covers execution-attempt/workload/FaultScenario identity, vector slots, planned/actual actions and positions, participant states, lifecycle events, static compensation evidence, runtime recovery sub-outcomes, skips/masking, blockers, conformance, and terminal status.
- AC-38: complete all-zero and assigned replay report `EXACT`; dry-run/materialization/startup reports omit conformance; controlled commit failure reports `INCOMPLETE` after measured actions.
- AC-40: the `01100` test reports slot states `NOT_ASSIGNED, REALIZED, MASKED, NOT_ASSIGNED, NOT_ASSIGNED` and retains the failed participant's skipped `third` forward.
- AC-42: dry-run and measured tests compare all five package artifact checksums before/after execution. The assigned-interleaving sample package hashes were `ffbd5a...` workload, `23daaa...` FaultScenario, `d7866f...` manifest, `55b6b0...` accounting, and `e3b0c4...` rejected-inputs, unchanged by the asserted execution path. Ten dry-run/measured collision iterations prove each package artifact is rejected as report output before startup. Six additional dry-run/measured iterations protect custom-location v3 sidecar JSONL, sidecar manifest, and join report bytes, while three normalized/symbolic/hard-link iterations prove alias-safe dynamic-artifact recognition; package and dynamic checksums remain unchanged.
- AC-44: focused simulator and verifier fixture coverage proves this slice's exact replay—including branched one-body-per-action behavior and package/dynamic-enrichment output alias safety—assigned fault, stepwise recovery, automatic commit, no-work, gate, validation, reporting, and immutability semantics before Quizzes evidence in S8.

Assigned-interleaving report sample: `/tmp/v3-executor-package14602888250996112561/reports/assigned-interleaving.json` (ephemeral test artifact). It records report schema v4, WorkloadPlan `134fbe5c...`, FaultScenario `7e3913dc...`, attempt `2b59c392...`, vector `01100`, terminal `PARTIAL_COMPENSATED`, conformance `EXACT`, lifecycle `ABORTED -> COMPENSATED -> AUTOMATIC_COMMIT`, and planned/actual action statuses `COMPLETED, ASSIGNED_FAULT, COMPLETED, COMPENSATED, COMPLETED`.

### Review Attempt 01 Fix Evidence

- Finding 1, exact forward isolation: added `ExecutionPlan.executeStepForExecutor`, exposed it only through the named executor controls, and switched `ScenarioExecutor` from prefix-oriented `executeUntilStep`. The operation executes a single named target only when all runtime dependencies are complete and rejects unmet dependencies or an already-attempted target. Ordinary `executeUntilStep`, `resumeWorkflow`, and whole-workflow behavior are unchanged.
- Finding 1 regression: the branched simulator fixture executes `first`, then `second`, while ready sibling `third` remains absent from calls and `SagaUnitOfWork.executedSteps`; an assigned later `third` remains untouched. The verifier branched `001` scenario has exact planned/actual ids and positions, body trace `[solo:first, solo:second]`, third `NOT_RUN`, and `EXACT` conformance.
- Finding 2, output/package aliasing: executor startup now compares the absolute normalized output against the manifest and all four linked artifacts, then uses `Files.isSameFile` for existing hard/symbolic aliases. Rejection occurs immediately after package reading and before selection/materialization/startup/actions.
- Finding 2 regressions: every protected artifact is tested as output in both dry-run and measured mode; normalized and symbolic aliases are tested separately. All five package hashes remain unchanged and `FixtureWorkflow.constructorCalls/BODIES` remain zero.
- TDD red evidence: focused simulator test compilation failed with three missing `executeStepForExecutor` symbols. The pre-fix verifier run had 14 failures across 33 iterations, visibly producing leaked bodies such as `[left:first, right:first, left:third, left:second, right:third, right:second]` and accepting direct package-artifact outputs.
- Reviewer probe reproduction: the adapted branched probe returned `NO_VIOLATION_OBSERVED`. The original output-alias probe now exits with `IllegalArgumentException`; manifest SHA-256 remained `0a88c2944bcf191c0b551a32cc1a63809e778b175c04ce7aa355208f86639e2e` before and after.

### Review Attempt 02 Fix Evidence

- Fixed only the dynamic-enrichment output-collision finding. Because the base v3 package does not link enrichment paths, the pre-preparation gate now inspects an existing candidate output and rejects the three authoritative schemas: `microservices-simulator.workload-dynamic-evidence.v3`, `microservices-simulator.workload-dynamic-evidence-manifest.v3`, and `microservices-simulator.dynamic-evidence-join-report.v3`.
- Schema recognition is location-independent, so custom configured locations require no executor CLI/options expansion. The normalized output path is inspected through the filesystem; symbolic and hard-link candidates resolve to the same recognized content and are rejected before selection/materialization/startup/actions.
- Added six direct regressions covering sidecar JSONL, sidecar manifest, and join report in both dry-run and measured mode, plus normalized, symbolic-link, and hard-link regressions. Every case asserts byte-identical dynamic checksums and zero `FixtureWorkflow` constructor/body activity; direct cases also assert all package checksums remain unchanged.
- TDD red evidence: before the guard, all nine new dynamic collision iterations failed because no exception was thrown, and measured cases visibly started/committed the fixture body.
- Reviewer probe reproduction: `/tmp/DynamicManifestAliasProbe.java` now exits with `IllegalArgumentException`; the artifact still has schema `microservices-simulator.workload-dynamic-evidence-manifest.v3` after rejection instead of report v4.

### Browser / Manual Evidence

- Browser evidence was not required. The serialized action-aware report and package checksums were inspected directly as recorded above.

### TDD Notes

- Added the two named simulator tests first and observed the expected missing-contract compilation failure. Implemented the narrow controls and drove pre-body/commit/recovery plus ordinary Saga regression tests green.
- Replaced the executor spec with the persisted all-zero behavior first; the old options/reader/report contract failed before the v3 migration. Then implemented selection, validation, preparation, replay, lifecycle/reporting, and expanded the suite through assigned interleaving, no-work, startup/materialization, commit failure, CLI rejection, serialization, and immutability cases.
- For review attempt 01, added branched exact-step and output-alias tests before fixes. Observed the expected missing simulator method plus leaked ready-sibling bodies and overwritten package outputs, then added only the exact executor-step boundary and pre-preparation output collision guard needed by the two blocking findings.
- For review attempt 02, added dynamic sidecar/manifest/join collision and alias tests first, observed nine expected failures, then added only recognized-v3-artifact inspection to the existing pre-preparation output guard.

### Deviations From Plan

- None. Zero-bit fallback and detailed compensation/infrastructure hard-stop behavior remain for S7; S6 includes only the controlled commit/recovery hard-stop foundation needed to report truthful partial attempts.

### Blockers / Follow-Ups

- None. Ready for `sp-review-slice`; S7 is the next dependent implementation slice after review passes.
