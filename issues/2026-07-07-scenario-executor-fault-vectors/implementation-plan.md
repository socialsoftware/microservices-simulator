# Implementation Plan: Scenario Executor Fault Vectors

## Spec Reference

- Spec: `issues/2026-07-07-scenario-executor-fault-vectors/spec.md`
- Decision frame: `issues/2026-07-07-scenario-executor-fault-vectors/decision-frame.md`
- Spec review: `issues/2026-07-07-scenario-executor-fault-vectors/spec-review.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor-poc.md`, `docs/verifiers-impl/roadmap.md`, `docs/verifiers-impl/README.md`, `docs/verifiers-impl/reading-guide.md`, `docs/verifiers-impl/advisor-brief.md`, `docs/verifiers-impl/thesis-claims-evidence-map.md`
- ADRs: None required by the spec.

## Summary

Implement fault-vector execution as a supported single-saga ScenarioExecutor capability by adding a simulator-owned execution-scoped in-memory fault provider, then teaching the verifier executor to validate one selected binary vector, expand it into deterministic fault slots, install the provider only for real runs, drive the saga through workflow lifecycle boundaries, and write a standalone v2 execution report. Keep CSV/manual impairment compatible outside executor-owned vector runs, but make the in-memory provider authoritative during vector runs. Preserve the current single-saga materialization boundary; do not broaden into multi-saga, TCC, distributed, search, impact scoring, or reset orchestration.

## Scope and Non-Goals

### In Scope

- Add simulator-owned in-memory fault-provider and typed injected-fault signal using plain identifier values.
- Add `--fault-vector` CLI input and `FAULT_VECTOR` Docker runner input.
- Default to `ScenarioPlan.faultSpace.defaultVector` when no vector is supplied.
- Require explicit `--scenario-id` / `SCENARIO_ID` for explicit vectors, while preserving auto-select for default-vector smoke runs.
- Validate vector syntax and fault-space-to-forward-schedule mapping before provider installation, materialization, or execution.
- Expand every vector bit into deterministic fault-slot report entries.
- Execute supported materializable single-saga saga/local plans with no-fault commit closure or realized-fault compensation closure.
- Distinguish expected injected faults, provider mismatches, unexpected runtime failures, missing expected injections, masking, compensation failures, and invalid vectors in machine-readable reports and exit codes.
- Update dummyapp-focused/unit coverage, simulator provider coverage, CLI/orchestrator coverage, one Quizzes Docker smoke path, and executor documentation.

### Out of Scope

- Multi-saga runtime execution, TCC execution, stream/gRPC/distributed parity.
- Fault injection into compensation steps.
- Delay injection or non-binary impairment types.
- Batch/vector-set execution, vector generation, deduplication, search, prioritization, GA, contextual bandits, and impact scoring.
- Generic application/database reset in executor core.
- Executor-generated CSV behavior files or CSV fault-vector input.
- Backward compatibility with the old v1 executor report schema.

## Repo Audit

### Existing Patterns to Use

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — current selection, validation, materialization, step-loop, and report-writing entry point. It currently calls `executeUntilStep(...)` for each scheduled step and does not close successful workflows with `resumeWorkflow(...)`; this must change.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOptions.java` and `ScenarioExecutorCli.java` — current CLI/options shape to extend with fault vector and v2 exit-code behavior.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestrator.java` and `verifiers/scripts/run-scenario-executor.sh` — forked runtime/Docker command construction to extend with vector input.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java` — current loose v1 report record to replace or evolve into the v2 schema required by the spec.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/FaultSpace.java`, `ScenarioPlan.java`, `ScheduledStep.java` — catalog structures that define fault-space length, `scheduledStepIds`, default vector, and expanded forward schedule.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/ExecutionPlan.java` — simulator-owned forward-step execution and existing CSV impairment query point. The in-memory provider must integrate here or immediately around this boundary so faults remain simulator-owned and occur before the step body.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/Workflow.java`, `WorkflowFunctionality.java`, `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaWorkflow.java` — lifecycle APIs for `executeUntilStep(...)`, `resumeWorkflow(...)`, and `resumeCompensation(...)`.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/impairment/ImpairmentHandler.java` — legacy CSV/manual impairment behavior to preserve outside vector scopes and suppress/override during executor-owned vector scopes.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/monitoring/dynamic/DynamicEvidenceRecorderHolder.java` — existing simulator-owned holder pattern for opt-in runtime state with no verifier dependency; useful as a style reference for a small provider holder/scope.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` and `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java` — current dummyapp-style executor fixtures; update/extend them for vector validation, lifecycle closure, compensation, mismatches, and report assertions.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestratorSpec.groovy` — command-construction coverage for forked executor invocation.
- `simulator/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/sagas/workflow/WorkflowExecutionPlanTest.groovy` and `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/ExecutionPlanDynamicEvidenceTest.java` — simulator workflow/execution-plan test patterns.
- `applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/behaviour/**` and `applications/quizzes/src/test/resources/groovy/**` — existing CSV impairment compatibility tests/resources.
- `docker-compose.yml` service `scenario-executor` and `verifiers/scripts/run-scenario-executor.sh` — supported Quizzes one-shot smoke path under `test,sagas,local`.
- `docs/verifiers-impl/README.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/roadmap.md`, `docs/verifiers-impl/reading-guide.md`, `docs/verifiers-impl/advisor-brief.md`, `docs/verifiers-impl/thesis-claims-evidence-map.md`, `docs/verifiers-impl/reference/scenario-executor-poc.md`, `docs/verifiers-impl/glossary.md`, `mkdocs.verifier.yml` — live docs that still frame the executor as a POC and must be updated after implementation.

### Relevant Areas

- `simulator/` — owns fault injection semantics, injected-fault signal, CSV compatibility, provider cleanup, and workflow lifecycle primitives.
- `verifiers/` — owns catalog selection, vector validation/expansion, provider installation, orchestration, v2 report writing, and executor tests.
- `applications/dummyapp/` — source-only verifier fixture background; executor tests mostly use in-test fixtures instead of running dummyapp as an application.
- `applications/quizzes/` — realistic Docker smoke target for the supported single-saga saga/local path.
- `docs/verifiers-impl/` — current-truth documentation and reference page rename.

### Existing Commands

- `cd simulator && mvn -Dtest=ExecutionPlanDynamicEvidenceTest test` — narrow simulator execution-plan regression command; add provider-specific simulator tests to the same module and run them by class name.
- `cd simulator && mvn -Dtest=WorkflowExecutionPlanTest test` — saga workflow lifecycle regression command when lifecycle/compensation tests are touched.
- `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` — focused executor behavior/regression command.
- `cd verifiers && mvn -Dtest=ScenarioExecutorOrchestratorSpec test` — focused forked command-construction regression command.
- `cd verifiers && mvn test` — broader verifier regression gate when report/model changes affect multiple executor/spec classes.
- `CATALOG_PATH=/reports/<run>/scenario-catalog.jsonl SCENARIO_ID=<plan-id> docker compose run --rm scenario-executor` — current Quizzes executor smoke pattern; extend with `FAULT_VECTOR` for explicit-vector smoke.
- `./scripts/verifier-docs build` — docs site build after docs/nav rename if the local MkDocs environment is available.

### Dependencies / Libraries

- Existing dependency/pattern selected: Java records/enums, Jackson serialization, Spring Boot CLI startup, Maven Surefire/Spock/JUnit already in `simulator/` and `verifiers/`.
- Existing dependency/pattern selected: simulator holder/scope style similar to `DynamicEvidenceRecorderHolder`, without importing verifier model classes.
- New dependency required: no.

## Constraints and Planning Decisions

- Preserve verifier pipeline boundaries: `visitor/*` -> `ApplicationAnalysisState` -> `scenario/adapter/*` -> `scenario/*` -> `dynamic/*`; executor changes should not rewrite static catalog generation.
- Fault injection must be simulator-owned. Verifier code may map catalog identities into plain simulator provider values, but simulator code must not import verifier model classes.
- Provider state must be execution-scoped and cleared in a `finally`-style path. Reject or explicitly fail concurrent active vector providers in one JVM rather than sharing mutable global state silently.
- Provider-injected faults must be abort-compatible with existing saga workflow lifecycle: the signal must extend or be translated as a simulator failure that causes `Workflow.executeUntilStep(...)` to mark the workflow aborted before compensation.
- Provider fault lookup must use an explicit current-boundary context installed by the executor before each `executeUntilStep(...)`, not only functionality/step simple names; the context carries scenario execution id, scenario plan id, saga instance id, slot index, scheduled step id, and runtime step name, and is cleared after the boundary.
- The active vector provider is authoritative during executor-owned vector runs; CSV/manual impairment must not combine with vector faults or delays in that scope.
- Existing CSV/manual impairment behavior remains valid outside vector scopes.
- The executor validates vectors and fault-space mappings before provider installation, materialization, or execution.
- Explicit vectors require explicit scenario ids. Auto-selection remains only for default-vector smoke behavior.
- Dry-run writes a v2 `DRY_RUN` report with expanded slots but does not install a provider, materialize, execute, or compensate.
- Current supported runtime shape remains materializable single-saga saga/local plans. Unsupported shapes must remain explicit report outcomes, not hidden behavior.
- `ScenarioPlan.deterministicId` stays vector-independent; each execution report gets a new opaque `scenarioExecutionId`.
- v2 executor report is a standalone artifact and must not mutate catalog JSONL, enriched JSONL, dynamic evidence, or join sidecars.
- Do not create or update root `CONTEXT.md`; glossary updates belong in `docs/verifiers-impl/glossary.md`.

## Slice DAG

| Slice | Name | File | Depends On | ACs Covered | Risk | Primary Verification |
|-------|------|------|------------|-------------|------|----------------------|
| S1 | Simulator in-memory fault provider | `001-simulator-in-memory-fault-provider.md` | none | AC-5, AC-6, AC-7, AC-8, AC-9 | high | `cd simulator && mvn -Dtest=<provider-test>,ExecutionPlanDynamicEvidenceTest test` |
| S2 | Executor vector validation and v2 dry-run/report contract | `002-executor-vector-validation-and-report-v2.md` | S1 | AC-1, AC-2, AC-3, AC-4, AC-17, AC-18, AC-19, AC-20, AC-21 | high | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` |
| S3 | No-fault lifecycle closure | `003-no-fault-lifecycle-closure.md` | S1, S2 | AC-2, AC-7, AC-9, AC-10, AC-17, AC-18, AC-19, AC-20, AC-21 | medium | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`; targeted simulator workflow test if touched |
| S4 | Realized fault compensation and masked slots | `004-realized-fault-compensation-and-masking.md` | S1, S2, S3 | AC-6, AC-7, AC-9, AC-11, AC-12, AC-17, AC-20, AC-21 | high | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`; provider/body-not-executed simulator test |
| S5 | Mismatches, unexpected failures, and compensation failures | `005-mismatch-unexpected-and-compensation-failures.md` | S1, S2, S3, S4 | AC-7, AC-13, AC-14, AC-15, AC-16, AC-17, AC-20, AC-21 | high | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`; simulator provider mismatch tests if simulator contract changes |
| S6 | CLI/Docker runner and Quizzes smoke | `006-cli-docker-runner-and-quizzes-smoke.md` | S1, S2, S3, S4, S5 | AC-1, AC-2, AC-11, AC-20, AC-22 | medium | `cd verifiers && mvn -Dtest=ScenarioExecutorOrchestratorSpec test`; two documented `docker compose run --rm scenario-executor` smokes |
| S7 | ScenarioExecutor docs and glossary cleanup | `007-scenario-executor-docs.md` | S1, S2, S3, S4, S5, S6 | AC-23 | low | `./scripts/verifier-docs build` or documented docs inspection if build unavailable |

## Acceptance Criteria Coverage

| AC | Summary | Covered By | Required Evidence |
|----|---------|------------|-------------------|
| AC-1 | CLI accepts `--fault-vector`; Docker accepts `FAULT_VECTOR`; explicit vectors require explicit scenario id. | S2, S6 | Executor CLI/options tests, orchestrator script/command tests, Docker env propagation inspection/smoke. |
| AC-2 | Default vector is validated/used/reported and suppresses CSV during executor-owned runs. | S2, S3, S6 | Default-vector report assertions; provider-mode/CSV suppression tests; no-fault Docker smoke report. |
| AC-3 | Invalid vectors and malformed fault-space mappings fail before execution. | S2 | Dummyapp-focused executor tests for length, non-binary, explicit-without-id, invalid default, length mismatch, duplicate ids, unresolved ids, duplicate mappings; provider-install/materialization fixtures proving no execution. |
| AC-4 | Dry-run validates and expands slots without provider install or saga execution/compensation. | S2 | Dry-run report test with full slot mapping and fixture counters proving no provider/materialization/execution/compensation. |
| AC-5 | Simulator exposes provider API using plain identifiers and no verifier model classes. | S1 | Simulator unit tests and source inspection; no imports from `verifiers` in simulator provider package. |
| AC-6 | Injected faults are machine-distinguishable and carry execution/plan/slot/step identity. | S1, S4 | Typed injected-fault exception tests and report assertions for scenario execution id, plan id, slot index, scheduled step id, runtime step. |
| AC-7 | Provider state is scoped, cleaned, and cannot silently leak/concurrently share. | S1, S3, S4, S5 | Provider holder/scope tests; executor finally-cleanup tests on success, realized fault, provider mismatch, unexpected failure, and compensation failure; concurrent active provider rejection test. |
| AC-8 | Legacy CSV/manual impairment works outside executor vector runs. | S1 | Existing CSV tests still pass or targeted `ImpairmentHandler`/ExecutionPlan regression proving CSV behavior when no provider is active. |
| AC-9 | In-memory vector provider overrides/suppresses CSV during vector runs. | S1, S3, S4 | Test with active vector provider and CSV directory/behavior showing no CSV fault/delay affects assigned vector semantics. |
| AC-10 | No-fault/default success closes lifecycle and reports committed `SUCCESS`. | S3 | Fixture test verifying `resumeWorkflow(...)`/commit closure and report `SUCCESS` + `COMMITTED`. |
| AC-11 | Reached `1` bit injects before body, stops forward execution, compensates, reports `FAULT_COMPENSATED`. | S4, S6 | Fixture/provider tests proving target body not executed, later forward steps skipped, `resumeCompensation(...)` called, report `FAULT_COMPENSATED`; Quizzes explicit-vector smoke. |
| AC-12 | Later assigned `1` bits masked by earlier abort are reported with reason. | S4 | Multi-step single-saga fixture test asserting slot states/order and mask reason. |
| AC-13 | Reached `1` bit with no injected fault reports `EXPECTED_FAULT_NOT_INJECTED`. | S5 | Fixture/provider test simulating provider/no-signal mismatch and report status/assertions. |
| AC-14 | Injected fault at `0` bit or wrong slot reports provider mismatch statuses. | S5 | Tests for `UNEXPECTED_INJECTED_FAULT` and `FAULT_PROVIDER_MISMATCH`, not ordinary runtime failure. |
| AC-15 | Unexpected `0`-bit forward exception reports `UNEXPECTED_EXECUTION_FAILURE` and best-effort closure. | S5 | Fixture test with body exception on `0` slot and compensation/closure evidence where exposed. |
| AC-16 | Compensation failure reports details and exits non-zero. | S5 | Fixture test where compensation closure throws; report `COMPENSATION_FAILED`, exception detail, non-zero exit mapping. |
| AC-17 | v2 report includes deterministic slot mapping and ordered step outcomes. | S2, S3, S4, S5 | JSON/report assertions for slot index ordering and `scheduleOrder` step ordering across dry-run/success/fault/failure. |
| AC-18 | v2 report includes reproducible runtime metadata without DB snapshots. | S2, S3 | Report assertions for application/runtime/catalog/scenario/vector/executor inputs; source inspection that no DB snapshot is attempted. |
| AC-19 | Fault-vector execution writes only executor report and does not mutate catalogs/sidecars. | S2, S3 | Temp-dir tests comparing catalog/enriched/dynamic sidecar content/timestamps or read-only fixture paths after execution. |
| AC-20 | Exit codes zero for valid experiments and non-zero for invalid/broken outcomes. | S2, S3, S4, S5, S6 | CLI/status mapping tests; Docker smoke exit codes for `SUCCESS` and `FAULT_COMPENSATED`; unit tests for non-zero statuses. |
| AC-21 | Dummyapp-focused coverage covers designed states. | S2, S3, S4, S5 | Expanded `ScenarioExecutorSpec`/fixtures covering validation, defaults, malformed mappings, dry-run, cleanup, compensation, masking, missing/mismatch faults, unexpected/compensation failures, CSV override, unsupported shape. |
| AC-22 | Quizzes Docker smoke covers default success and explicit single-bit compensated fault. | S6 | Recorded commands, exit codes, and v2 report excerpts/paths under `verifiers/target/`. |
| AC-23 | Docs stop framing ScenarioExecutor as POC; reference doc renamed and terminology updated. | S7 | File rename/update diff, docs nav/search references updated, docs build or inspection evidence. |

## Verification Matrix

| Requirement / Area | Verification Type | Command / Method | Required Evidence |
|--------------------|-------------------|------------------|-------------------|
| Simulator provider API, injected exception, cleanup, concurrency rejection | unit | `cd simulator && mvn -Dtest=<new-provider-test> test` | Passing provider tests; source shows no verifier imports in simulator provider package. |
| ExecutionPlan provider integration and CSV compatibility/override | unit/integration | `cd simulator && mvn -Dtest=<new-provider-test>,ExecutionPlanDynamicEvidenceTest test` plus targeted existing CSV behavior test if touched | Passing tests proving legacy CSV outside scope and vector authority inside scope. |
| Executor vector validation, dry-run, v2 report mapping | unit | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | Passing assertions for invalid/default/explicit vectors and no provider/execution on dry-run/invalid. |
| No-fault lifecycle closure | unit/integration | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`; `cd simulator && mvn -Dtest=WorkflowExecutionPlanTest test` if simulator lifecycle code changes | Report `SUCCESS` + `COMMITTED`; fixture records `resumeWorkflow`/commit closure. |
| Fault compensation and masking | unit/integration | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | Report `FAULT_COMPENSATED`; prefix steps before the fault execute; target step body not executed; compensation closure is legal/called; masked slot states ordered. |
| Broken/mismatch failure statuses and exit-code mapping | unit | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` and CLI-focused test if separated | Exact terminal statuses, provider cleanup on failure paths, and non-zero/zero exit decisions asserted. |
| Docker runner env and Quizzes realistic smoke | smoke | Default: `CATALOG_PATH=... docker compose run --rm scenario-executor`; explicit: `CATALOG_PATH=... SCENARIO_ID=... FAULT_VECTOR=... docker compose run --rm scenario-executor` | Exit code 0 for `SUCCESS` and `FAULT_COMPENSATED`; v2 report excerpts stored under `verifiers/target/`. |
| Docs/nav rename | docs build/manual | `./scripts/verifier-docs build` or `rg "scenario-executor-poc|ScenarioExecutor POC" docs/verifiers-impl mkdocs.verifier.yml -g '!archive/**'` | No live POC framing for the implemented executor; unsupported/future areas remain explicit. |

## Slice Files

Detailed slice cards live next to this plan:

- `001-simulator-in-memory-fault-provider.md` — simulator provider/signal/scope and CSV compatibility boundary.
- `002-executor-vector-validation-and-report-v2.md` — vector input, validation, dry-run expansion, v2 report foundation, report side-effect boundary.
- `003-no-fault-lifecycle-closure.md` — default/no-fault execution uses lifecycle closure and reports committed success.
- `004-realized-fault-compensation-and-masking.md` — reached assigned faults inject at forward-step start, compensate, and mask later assigned slots.
- `005-mismatch-unexpected-and-compensation-failures.md` — missing expected faults, unexpected/wrong-slot injected faults, unexpected runtime exceptions, and compensation failures.
- `006-cli-docker-runner-and-quizzes-smoke.md` — runner/Docker fault-vector surface and realistic Quizzes smokes.
- `007-scenario-executor-docs.md` — docs reference rename and current-state/glossary/nav cleanup.

## Cross-Slice Risks

- Workflow lifecycle risk: current executor tests explicitly assert `resumeWorkflow` is not called; S3 must intentionally update this expectation because the supported feature requires commit closure.
- Fault timing risk: `ExecutionPlan` must inject before the target step body without preempting earlier already-scheduled boundaries. Executor tests should drive one forward boundary at a time and simulator tests should prove a second-slot fault lets the first prefix step execute while the faulted body does not execute.
- Abort/compensation risk: the injected-fault signal must mark the workflow aborted through existing lifecycle semantics before `resumeCompensation(...)`; otherwise expected faults will fail as illegal compensation attempts.
- Identity plumbing risk: simulator must not import verifier models, but report quality requires catalog slot identity. Use plain-value provider assignments and explicit current-boundary context set by the executor before each forward boundary, then clear that context after the boundary.
- Global state risk: both legacy CSV impairment and the new provider use process-level state. Provider scopes and cleanup tests must cover success, invalid, faulted, unexpected failure, and compensation failure paths.
- Report compatibility risk: v2 breaks the old report schema by spec. Keep old compatibility only if it falls out naturally; do not add dual-schema complexity unless implementation discovers an unavoidable consumer.
- Quizzes smoke risk: an explicit single-bit vector requires a known materializable single-saga scenario and vector index. The implementer may need to generate or reuse a current catalog and record the chosen scenario/vector in evidence.

## Planning Blockers / Deferred Decisions

- Planning blockers: None.
- Deferred by spec: multi-saga execution model, impact-analysis telemetry shape, search/orchestration policy, persistent-environment reset ownership, non-binary/delay impairments, and compensation-step fault injection.

## Handoff

Ready for implementation: `yes`

Recommended next step: execute slices in dependency order with a future execution skill that performs just-in-time preflight before each slice.
