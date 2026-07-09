# Implementation Plan: Multi-Saga Scenario Executor Interleavings

## Spec Reference

- Spec: `issues/2026-07-08-multi-saga-scenario-executor/spec.md`
- Decision frame: `issues/2026-07-08-multi-saga-scenario-executor/decision-frame.md`
- Spec review: `issues/2026-07-08-multi-saga-scenario-executor/spec-review.md`
- Planning audit: `issues/2026-07-08-multi-saga-scenario-executor/quizzes-materializability-audit.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`
- ADRs: `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md`, `docs/verifiers-impl/decisions/2026-04-28-hybrid-static-dynamic-key-binding.md`, `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`, `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`

## Summary

Implement multi-saga execution by first making the v3 participants report model canonical for the existing single-saga executor, then adding explicit multi-saga selection/dry-run validation, participant preparation gates, deterministic `expandedSchedule` replay, compensate-and-continue fault handling, hard-stop/exit-code behavior, and final Quizzes/documentation evidence. The executor remains verifier-owned, saga/local-only, deterministic, sequential, one scenario attempt per run, and uses the existing simulator in-memory fault-vector provider with scenario/saga/scheduled-step identity. Static catalogs and enrichment sidecars remain input contracts and are not rewritten.

## Scope and Non-Goals

### In Scope

- Migrate ScenarioExecutor reports from v2 flat single-saga fields to v3 top-level scenario facts plus required participant entries.
- Keep single-saga execution mechanics working as the one-participant v3 case.
- Support explicit `--scenario-id` selection of one materializable multi-saga `ScenarioPlan`.
- Keep multi-saga auto-selection out of scope; auto-selection remains limited to supported single-saga default-vector candidates.
- Validate participant/input/step/fault-vector mapping for explicit multi-saga plans, including dry-run without materialization or runtime execution.
- Materialize and start one participant per `SagaInstance`, with one functionality/session and one `SagaUnitOfWork` per participant.
- Execute multi-saga scheduled forward steps in ascending `expandedSchedule.scheduleOrder`, dispatching each step by `sagaInstanceId`.
- Route scenario-wide binary fault vectors through the existing simulator `InMemoryFaultVectorProvider`.
- Apply compensate-and-continue for assigned faults and scheduled-step runtime failures.
- Write v3 report states/statuses using the spec vocabulary.
- Update CLI/Docker exit-code mapping and participant-oriented CLI output for v3 statuses/reports.
- Add dummy/synthetic Spock coverage and a Quizzes Docker/forked smoke using a currently materializable multi-saga candidate.
- Update ScenarioExecutor/current-state documentation after implementation.

### Out of Scope

- True parallel/threaded execution.
- Generic distributed, stream, gRPC, or TCC runtime parity.
- Generic fixture/database seeding, reset, teardown, or Spock setup replay.
- Event-origin payload reconstruction or broader materializer support beyond currently materializable candidates.
- Compensation-step faults, delay injection, non-binary impairments, retries, deferred compensation, configurable failure-policy matrices.
- Batch execution, impact scoring, local/genetic search, scenario prioritization, or bandit allocation.
- Quizzes-specific executor shortcuts.
- Rewriting static catalogs, enriched catalogs, dynamic evidence, or join-report sidecars from executor output.

## Repo Audit

### Existing Patterns to Use

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — current selection, validation, materialization, step execution, provider installation, compensation, and report-writing flow; split or reorganize only as needed to support participants without changing executor ownership.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java` — current v2 report record to replace with the v3 participants model and exact status/state vocabulary.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioCatalogReader.java` and `CatalogScenarioRecord.java` — static/enriched JSONL loading already treats enriched records as wrappers and uses embedded `ScenarioPlan` as execution contract.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioMaterializer.java` and `ScenarioExecutorMaterializationPolicy.java` — existing materialization semantics and runtime-owned argument whitelist; evolve only enough to support one pre-created unit of work per participant.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioRuntimeContext.java` — existing runtime-owned bean and `SagaUnitOfWork` creation boundary.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorCli.java` — current Spring/forked CLI startup and exit-code helper; update to v3 status semantics and participant-oriented output.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestrator.java` and `verifiers/scripts/run-scenario-executor.sh` — existing forked command/Docker runner shape for catalog path, scenario id, fault vector, output path, app base, Spring class, profiles, and Maven profile.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/ScenarioPlan.java`, `SagaInstance.java`, `ScheduledStep.java`, `FaultSpace.java`, `ScenarioKind.java`, `InputVariant.java` — catalog contract for participants, input references, deterministic schedules, and scenario-level fault slots.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/*` — existing in-memory fault-vector provider, boundary context, injected-fault exception, and scoped provider holder; reuse without adding verifier dependencies to simulator.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowFunctionality.java`, `Workflow.java`, `ExecutionPlan.java`, `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaWorkflow.java` — lifecycle methods used by executor: `executeUntilStep`, `resumeWorkflow`, and `resumeCompensation`.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` and `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java` — current synthetic executor coverage and fixtures to extend for participant reports, multi-saga order, fault realization, continuation, masking, and closure ordering.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestratorSpec.groovy` — existing command-construction coverage for forked executor invocation.
- `verifiers/target/multi-saga-executor-planning-audit/quizzes-20260709-004627-310/scenario-catalog.jsonl` — existing planning-audit catalog with materializable multi-saga Quizzes candidates, including plan `0945caa9ac2fe06a268e6df6aa992fcf69e253116264684d577bdbbb955c2e25`.
- `docs/verifiers-impl/reference/scenario-executor.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/evidence.md`, `docs/verifiers-impl/glossary.md` — docs/current-truth and evidence surfaces to update after implementation.

### Relevant Areas

- `verifiers/faults/executor` — main implementation area for v3 reports, selection, validation, materialization/startup gates, participant runtime state, scheduled replay, fault-slot realization, and exit/report behavior.
- `verifiers/faults/scenario/model` — read-only catalog model contract; changes should not be required unless implementation discovers a deserialization/reporting defect.
- `simulator/faults` and `simulator/coordination` — runtime fault provider and workflow execution boundaries; expected to be reused as-is, with targeted tests only if a provider identity gap is found.
- `verifiers/src/test/groovy/.../executor` and `verifiers/src/test/java/.../executor` — dummy/synthetic coverage for all status/state vocabulary and edge cases before relying on Quizzes.
- `applications/quizzes` plus Docker `scenario-executor` service — realistic smoke target for the materializable multi-saga candidate.
- `docs/verifiers-impl` — current-state/reference/evidence updates after behavior is implemented and validated.

### Existing Commands

- `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` — primary focused executor behavior/regression suite.
- `cd verifiers && mvn -Dtest=ScenarioExecutorOrchestratorSpec test` — forked command and runner option regression suite.
- `cd verifiers && mvn -Dtest=ScenarioExecutorReadinessEvaluatorSpec test` — materializability policy regression when materializer/readiness behavior is touched.
- `cd verifiers && mvn test` — broader verifier regression gate after report model changes stabilize.
- `CATALOG_PATH=/reports/multi-saga-executor-planning-audit/quizzes-20260709-004627-310/scenario-catalog.jsonl SCENARIO_ID=0945caa9ac2fe06a268e6df6aa992fcf69e253116264684d577bdbbb955c2e25 OUTPUT_PATH=/reports/scenario-executor/multi-saga-default-report.json docker compose run --rm scenario-executor` — planned Quizzes default-vector smoke shape.
- `CATALOG_PATH=... SCENARIO_ID=... FAULT_VECTOR=<binary> OUTPUT_PATH=... docker compose run --rm scenario-executor` — planned Quizzes explicit-vector smoke shape when a vector is selected for validation.
- `./scripts/verifier-docs build` — docs-site build if the local docs script/environment is available; otherwise use targeted docs inspection with `rg` and record that build was unavailable.

### Dependencies / Libraries

- Existing dependency/pattern selected: Java records/collections, Jackson report serialization, Spring Boot CLI startup, Spock/Surefire tests, current simulator fault-provider holder/scope.
- Existing dependency/pattern selected: verifier catalog JSONL and enriched-wrapper reader; no new catalog format or adapter artifact.
- New dependency required: no.

## Constraints and Planning Decisions

- Preserve the verifier pipeline boundary: executor consumes `ScenarioPlan` records and writes standalone execution reports; it does not alter `visitor/*`, `ApplicationAnalysisState`, `scenario/adapter/*`, catalog generation, dynamic enrichment, or join sidecars.
- Treat `ScenarioPlan.expandedSchedule` sorted by `scheduleOrder` as the runtime interleaving contract.
- Use explicit multi-saga selection only. Auto-selection must keep skipping multi-saga records until a later prioritization/selection feature.
- Keep supported runtime scope to saga/local materializable plans under current materializer semantics plus existing runtime-owned argument resolution.
- Enforce exactly one matching `InputVariant` per `SagaInstance` by deterministic input variant id; duplicates and missing inputs are unsupported.
- Validate structural participant mapping, runtime step-name mapping, and fault-vector mapping before materialization or runtime execution.
- v3 participant lists are empty only when no scenario plan was selected. Once a plan is selected, validation, materialization, startup, runtime, and hard-stop reports include one participant entry per known `SagaInstance` where the selected plan exposes saga instances.
- Dry-run validates and writes a v3 report only; it must not materialize, resolve runtime-owned arguments, instantiate functionality, install a provider, execute steps, close, or compensate.
- For non-dry-run multi-saga attempts, materialize all participants before startup, and complete startup for all participants before any scheduled forward step executes.
- Create one `SagaUnitOfWork` per participant and use that same unit of work for constructor/runtime-owned argument resolution and lifecycle method calls. The current single-saga path creates unit-of-work values in more than one place; the implementation should remove that mismatch for participant execution.
- Install the in-memory fault-vector provider only around scheduled runtime execution and clear it on every success/failure path.
- Fault slots remain scenario-level and ordered by `FaultSpace.scheduledStepIds`, not by participant-local order.
- Assigned fault bits in a failed participant after its terminal failure become `MASKED_BY_SAGA_FAILURE`; assigned bits in surviving participants remain active until reached or until a whole-attempt hard stop.
- Scheduled-step domain/simulator exceptions are participant outcomes under compensate-and-continue. The same final policy applies to one-participant single-saga v3 attempts: a non-assigned scheduled-step exception that compensates aggregates to `COMPENSATED`, not the old v2 hard-stop-style `UNEXPECTED_EXECUTION_FAILURE`.
- Infrastructure, selection, vector, provider, materialization, startup, configuration, report-writing, and unexpected non-scheduled execution failures remain hard stops.
- Failed-and-compensated participants are terminal and excluded from final `resumeWorkflow` closure.
- Surviving participant closure order is deterministic: ascending last executed scheduled step order; participants with no executed scheduled steps close afterward in saga-instance-id order.
- v3 is a clean breaking report schema. Do not preserve v2 top-level `sagaInstanceId`, `sagaFqn`, `inputVariantId`, flat `stepOutcomes`, top-level `lifecycleOutcome`, or `FAULT_COMPENSATED` alias.
- Do not create or update root `CONTEXT.md`; use `docs/verifiers-impl/glossary.md` only if terminology changes are needed.

## Slice DAG

| Slice | Name | File | Depends On | ACs Covered | Risk | Primary Verification |
|-------|------|------|------------|-------------|------|----------------------|
| S1 | v3 participant report and single-saga migration | `001-v3-participant-report-and-single-saga-migration.md` | none | AC-35, AC-36, AC-37, AC-38, AC-39, AC-44 | high | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` |
| S2 | explicit multi-saga selection and dry-run validation | `002-explicit-multi-saga-selection-and-dry-run.md` | S1 | AC-1, AC-2, AC-3, AC-4, AC-5, AC-6, AC-10, AC-11, AC-12, AC-18, AC-19, AC-20, AC-30, AC-40 | high | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` |
| S3 | multi-participant materialization and startup gates | `003-multi-participant-materialization-and-startup-gates.md` | S1, S2 | AC-7, AC-8, AC-9, AC-15, AC-25, AC-30, AC-36, AC-37, AC-40 | high | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` |
| S4 | default-vector interleaving and survivor closure | `004-default-vector-interleaving-and-survivor-closure.md` | S1, S2, S3 | AC-13, AC-14, AC-15, AC-16, AC-17, AC-18, AC-31, AC-36, AC-37, AC-40 | high | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` |
| S5 | assigned-fault compensate-and-continue | `005-assigned-fault-compensate-and-continue.md` | S1, S2, S3, S4 | AC-20, AC-21, AC-22, AC-23, AC-24, AC-27, AC-32, AC-33, AC-36, AC-37, AC-40 | high | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` |
| S6 | runtime failures, compensation failures, hard stops, and exit codes | `006-runtime-failures-compensation-failures-and-exit-codes.md` | S1, S2, S3, S4, S5 | AC-17, AC-20, AC-25, AC-26, AC-28, AC-29, AC-30, AC-32, AC-33, AC-34, AC-36, AC-37, AC-40, AC-44 | high | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test` |
| S7 | Quizzes smoke and documentation | `007-quizzes-smoke-and-documentation.md` | S1, S2, S3, S4, S5, S6 | AC-1, AC-2, AC-35, AC-39, AC-41, AC-42, AC-43, AC-44 | medium | Quizzes `docker compose run --rm scenario-executor`; docs build/inspection |

Risk: `low`, `medium`, or `high`.

## Acceptance Criteria Coverage

| AC | Summary | Covered By | Required Evidence |
|----|---------|------------|-------------------|
| AC-1 | Load static/enriched multi-saga catalogs without mutating artifacts. | S2, S7 | Temp-dir tests compare catalog contents before/after dry-run; Quizzes non-dry-run smoke records before/after byte or checksum evidence proving the input catalog is unchanged. |
| AC-2 | Explicit `--scenario-id` selects multi-saga plan by deterministic id. | S2, S7 | Spock selection test for `ScenarioKind.MULTI_SAGA`; Quizzes smoke uses the audited deterministic id. |
| AC-3 | Multi-saga is not auto-selected; auto-selection stays single-saga. | S2 | Auto-select test with multi-saga and single-saga records proves multi-saga records are skipped and counted. |
| AC-4 | Missing explicit scenario id fails before materialization/runtime. | S2 | Explicit missing id test asserts `SELECTION_FAILED`, participant list empty or selected-plan absent, and fixture counters unchanged. |
| AC-5 | Unsupported multi-saga shapes fail before runtime with blockers. | S2, S3 | Tests for unsupported shape/missing schedule owner/duplicate inputs/startup-ineligible shape with structured blockers and no execution. |
| AC-6 | Every `SagaInstance` has exactly one matching input variant. | S2 | Tests for missing and duplicate input variant ids, with participant-local or scenario blockers. |
| AC-7 | Non-dry-run requires every participant input materializable. | S3 | Multi-participant materialization success/failure tests using current materializer semantics and runtime-owned arguments. |
| AC-8 | Materialization failure writes v3 blockers and no step execution. | S3 | Test with one blocked participant asserts `MATERIALIZATION_FAILED`, participant states, `NOT_REACHED` assigned slots where applicable, no provider/steps. |
| AC-9 | Startup failure writes v3 startup report, no steps/closure/compensation. | S3 | Test with constructor/class/startup failure asserts `STARTUP_FAILED`, participant startup states, and no lifecycle method calls. |
| AC-10 | Dry-run validates mappings and writes v3 without materialization/runtime. | S2 | Explicit multi-saga dry-run test asserts participant entries, step/fault mapping, provider `NONE`, materialization/startup `NOT_ATTEMPTED`. |
| AC-11 | Runtime step names use final `::` and trailing `#<digits>` removal. | S2, S4 | Step-name normalization tests for multi-saga scheduled steps and execution dispatch. |
| AC-12 | Non-mappable step ids produce structured blockers before runtime. | S2 | Invalid step-id test asserts `UNSUPPORTED_SCENARIO`/`UNSUPPORTED_STEP_ID` and no materialization. |
| AC-13 | Runtime follows `expandedSchedule` ascending `scheduleOrder`. | S4 | Multi-participant fixture records global execution order matching catalog order. |
| AC-14 | Each step dispatches to matching `sagaInstanceId` participant. | S4 | Fixture records participant identity per step; cross-saga schedule test proves dispatch is by scheduled owner. |
| AC-15 | One functionality/session and one unit of work per participant. | S3, S4 | Fixture/runtime test records distinct participant instances and stable per-participant unit-of-work reuse. |
| AC-16 | Surviving participants close in deterministic final order. | S4 | Closure-order test for last-executed ordering and zero-step participant ordering. |
| AC-17 | Failed-and-compensated participants are not closed again. | S4, S5, S6 | Fault/runtime-failure tests assert compensation called once and `resumeWorkflow` not called for terminal failed participants. |
| AC-18 | Default-vector multi-saga uses plan default and reports `DEFAULT_VECTOR`. | S2, S4 | Dry-run and execution tests assert `assignedVector`, `vectorSource`, and fault slots from plan default. |
| AC-19 | Explicit vectors are binary and length-aligned. | S2 | Validation table tests for non-binary, wrong length, empty/nonempty mismatch, and explicit vector without id. |
| AC-20 | Scenario-level fault-slot mapping and v3 realization states. | S2, S5, S6 | Report assertions for `NOT_ASSIGNED`, `DRY_RUN`, `REALIZED`, `MASKED_BY_SAGA_FAILURE`, `NOT_REACHED`, `EXPECTED_FAULT_NOT_INJECTED`. |
| AC-21 | Assigned fault realizes only on matching scenario/saga/step/runtime/slot identity. | S5 | Provider identity tests across two participants and wrong identity/mismatch cases. |
| AC-22 | Multiple assigned faults across surviving participants can realize. | S5 | Multi-participant vector test where two participants each realize assigned faults in one attempt. |
| AC-23 | Later assigned slots in failed participant are `MASKED_BY_SAGA_FAILURE`. | S5, S6 | Fault and runtime-failure tests assert same-participant later assigned slots are masked. |
| AC-24 | Surviving participant assigned slots remain active after another fails. | S5 | Interleaving test with A failing first and B realizing a later assigned fault. |
| AC-25 | Whole-attempt hard-stop skipped assigned slots are `NOT_REACHED`. | S3, S6 | Materialization/startup/expected-not-injected/provider hard-stop tests assert later assigned slots become `NOT_REACHED`. |
| AC-26 | Reached assigned slot without injection is validation failure. | S6 | Suppressed-provider fixture test asserts `EXPECTED_FAULT_NOT_INJECTED` terminal status, slot state, and validation blocker. |
| AC-27 | Assigned fault compensates participant, skips its remaining steps, continues survivors. | S5 | Assigned-fault test asserts failed participant compensation, skipped steps, survivor steps continue. |
| AC-28 | Non-assigned scheduled-step exception follows compensate-and-continue. | S6 | Runtime exception tests assert participant compensated, remaining participant continues when present, one-participant single-saga runtime failure aggregates to `COMPENSATED`, and top-level aggregate status is a scenario outcome. |
| AC-29 | Compensation failure marks participant and top-level `COMPENSATION_FAILED`, survivors continue. | S6 | Compensation-failure test asserts failed participant lifecycle, survivor continuation/closure, non-zero exit mapping. |
| AC-30 | Executor/infrastructure failures remain hard stops. | S2, S3, S6 | Validation/materialization/startup/provider/closure/configuration/report-writing tests distinguish hard-stop statuses or non-zero CLI/stderr outcomes from domain outcomes. |
| AC-31 | `SUCCESS` only when all participants commit without scheduled-step failures. | S4 | Default-vector success test asserts all participants `COMMITTED` and top-level `SUCCESS`. |
| AC-32 | `COMPENSATED` when all terminal started/executed participants compensate and none commits. | S5, S6 | Single- and multi-participant all-failed/all-compensated tests assert `COMPENSATED`. |
| AC-33 | `PARTIAL_COMPENSATED` when at least one compensates and one commits. | S5, S6 | Mixed failed/surviving participant test asserts compensated + committed participants and top-level status. |
| AC-34 | `COMPENSATION_FAILED` when any participant compensation fails. | S6 | Compensation-failure continuation test and CLI exit-code test. |
| AC-35 | v3 report canonical for single- and multi-saga attempts. | S1, S7 | JSON assertions for schema version `microservices-simulator.scenario-execution-report.v3`; docs updated. |
| AC-36 | v3 participant list and participant vocabulary. | S1, S2, S3, S4, S5, S6 | Report-shape tests assert required participants and exact materialization/startup/lifecycle/step/blocker fields. |
| AC-37 | v3 top-level scenario facts and vocabulary. | S1, S2, S3, S4, S5, S6 | Report-shape tests assert top-level scenario execution id, plan id, kind, status, vector, provider, metadata, fault slots, skipped counts, blockers. |
| AC-38 | No v2 top-level single-saga identity fields or `FAULT_COMPENSATED`. | S1 | JSON tree tests assert fields/status alias are absent. |
| AC-39 | Single-saga execution writes v3 one-participant reports and keeps mechanics. | S1, S7 | Existing single-saga success/fault tests migrated to v3; optional existing Quizzes single-saga smoke still works if run. |
| AC-40 | Dummy/synthetic coverage proves required behaviors. | S1, S2, S3, S4, S5, S6 | `ScenarioExecutorSpec` expanded to cover dry-run, success, assigned faults, runtime exception, multiple faults, compensation failure, closure order, vocabulary, exit mapping, shape. |
| AC-41 | Quizzes validates bounded multi-saga `WRITE_PLANS` catalog through Docker/forked path. | S7 | Recorded Docker command, exit code, catalog path, selected plan id, and report artifact under `verifiers/target/`. |
| AC-42 | Quizzes smoke records required scenario/report facts. | S7 | Smoke evidence file or docs evidence records participants, schedule, vector, terminal status, lifecycle outcomes, report path. |
| AC-43 | Docs describe multi-saga deterministic replay and limitations. | S7 | Updates to `docs/verifiers-impl/reference/scenario-executor.md` and `docs/verifiers-impl/current-state.md`; docs build/inspection evidence. |
| AC-44 | CLI/Docker exit zero/non-zero contract for v3 statuses. | S1, S6, S7 | CLI helper tests for all represented statuses, including `CONFIGURATION_FAILED`/`REPORT_WRITE_FAILED` if represented as report statuses; Docker smoke exit code for zero status; hard-stop/compensation-failed/config-report failure checks for non-zero. |

## Verification Matrix

| Requirement / Area | Verification Type | Command / Method | Required Evidence |
|--------------------|-------------------|------------------|-------------------|
| v3 report schema and single-saga migration | unit / JSON inspection | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | Passing tests for v3 schema, one participant, removed v2 top-level fields, participant-local step outcomes, CLI code no longer depending on flat `stepOutcomes`, status vocabulary, existing single-saga mechanics. |
| Explicit multi-saga selection and dry-run mapping | unit | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | Passing tests for explicit id, explicit vector without id, no multi-saga auto-select, participant/input mapping, selected-plan validation-failure participant entries, runtime step names, vector slots, no materialization/runtime. |
| Materialization/startup hard-stop gates | unit | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`; add `ScenarioExecutorReadinessEvaluatorSpec` if readiness behavior changes | Passing tests showing all participant inputs materialize before startup, startup before steps, selected-plan participant entries remain present on gate failures, and failure reports have no runtime side effects. |
| Successful deterministic interleaving and closure | unit/integration fixture | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | Passing tests showing schedule order, owner dispatch, one session/unit of work per participant, closure order, `SUCCESS`. |
| Assigned-fault compensate-and-continue | unit/integration fixture | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | Passing tests showing realized slots, per-participant compensation, skipped failed participant steps, surviving participant continuation, multiple realized faults. |
| Runtime failure, compensation failure, provider validation, hard stops | unit | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test` | Passing tests for non-assigned step exceptions including one-participant single-saga aggregation, expected-not-injected, provider mismatch, compensation failure continuation, `NOT_REACHED`, configuration/report-write behavior, and exit code mapping. |
| Existing simulator provider compatibility | source inspection / targeted simulator test only if touched | `cd simulator && mvn -Dtest=<touched-test> test` if simulator code changes | If simulator code is unchanged, record inspection that existing provider identity covers scenario/saga/slot/runtime fields; if changed, record passing simulator tests. |
| Quizzes realistic smoke | Docker smoke | `docker compose run --rm scenario-executor` with audited catalog/scenario id and output path | Exit code, report path, terminal status, participants, schedule, assigned vector, lifecycle outcomes, and before/after input catalog checksum or byte comparison recorded. |
| Documentation and evidence | docs build / inspection | `./scripts/verifier-docs build` or targeted `rg` inspection if unavailable | Reference/current-state docs no longer claim multi-saga unsupported after implementation; limitations remain explicit. |

## Slice Files

Detailed slice cards live next to this plan:

- `001-v3-participant-report-and-single-saga-migration.md` — migrate reports, participant-oriented CLI output, and status/exit contract to v3 while preserving one-participant single-saga execution.
- `002-explicit-multi-saga-selection-and-dry-run.md` — add explicit multi-saga selection, no-auto-selection behavior, structural validation, vector mapping, selected-plan validation-failure participant reports, and dry-run reports.
- `003-multi-participant-materialization-and-startup-gates.md` — prepare all participants before runtime and report materialization/startup hard stops.
- `004-default-vector-interleaving-and-survivor-closure.md` — execute fault-free/default-vector multi-saga schedules and close surviving participants deterministically.
- `005-assigned-fault-compensate-and-continue.md` — realize scenario-wide assigned faults, compensate failed participants, mask failed-participant slots, and continue survivors.
- `006-runtime-failures-compensation-failures-and-exit-codes.md` — handle non-assigned runtime failures including one-participant single-saga aggregation, compensation failures, validation/provider/config/report hard stops, and v3 CLI exit codes.
- `007-quizzes-smoke-and-documentation.md` — run the Quizzes multi-saga smoke, record evidence including catalog non-mutation checks, and update docs/current-state language.

## Cross-Slice Risks

- Report migration risk: v3 intentionally breaks v2 report consumers and tests. Keep the migration in S1 so later slices do not maintain dual report shapes.
- Current unit-of-work risk: the existing single-saga executor can create a runtime-owned `SagaUnitOfWork` during materialization and a second one for lifecycle calls. Multi-saga participant execution should create one unit of work per participant and reuse it consistently.
- Monolithic executor risk: `ScenarioExecutor.java` currently centralizes validation, execution, and report construction. Introduce only minimal package-private helpers/state records needed to keep participant behavior understandable; do not refactor unrelated pipeline code.
- Fault-state aggregation risk: distinguish participant domain outcomes from whole-attempt hard stops. Scheduled-step faults/exceptions can aggregate to `COMPENSATED`/`PARTIAL_COMPENSATED`; provider mismatches, expected-not-injected, materialization/startup/config/report failures are hard stops.
- Provider cleanup risk: provider holder state is process-global. Tests must cover cleanup on success, assigned fault, expected-not-injected, provider mismatch, runtime failure, compensation failure, and startup/materialization paths where the provider should not install.
- Closure-order risk: survivor closure ordering depends on tracking last executed scheduled step per participant and deterministic id ordering for zero-step participants. Tests must not rely on map insertion order accidentally.
- Quizzes smoke risk: the audited materializable plan is adequate for proving replay, but may depend on persistent test data/profile setup. If it no longer materializes, regenerate a bounded multi-saga `WRITE_PLANS` catalog with the audit command and record the replacement candidate.
- Scope creep risk: do not add generic fixture synthesis, distributed parity, retry/deferred compensation, scoring/search, or Quizzes-specific shortcuts while implementing this feature.

## Planning Blockers / Deferred Decisions

- Planning blockers: None.
- Deferred by spec: multi-saga auto-selection/prioritization, true concurrency, distributed/stream/gRPC/TCC parity, generic fixture/reset orchestration, broader materialization/event payload reconstruction, compensation-step faults, retries, deferred-compensation experimental policy, batch execution, impact scoring, search, and catalog/dynamic sidecar rewriting.

## Handoff

Ready for implementation: `yes`

Recommended next step: execute slices in dependency order with a future execution skill that performs just-in-time preflight before each slice.
