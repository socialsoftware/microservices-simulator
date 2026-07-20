# Implementation Plan: Compensation-Aware Scenario Catalog and Replay

Create at:

```txt
issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md
```

This is a stable slice/dependency/verification plan, not a source-code edit script.

## Spec Reference

- Spec: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Decision frame: `issues/2026-07-19-compensation-aware-scenario-catalog/decision-frame.md`
- Spec review: `issues/2026-07-19-compensation-aware-scenario-catalog/spec-review.md` (`READY`)
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`
- ADRs: `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`, `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`, `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md`, `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`

## Summary

Replace the canonical v2 `ScenarioPlan` path with a v3 package in dependency-ordered stages: first retain and classify compensation evidence; then establish deterministic WorkloadPlan package semantics and WorkloadPlan-linked enrichment; add pure recovery-schedule/FaultScenario generation; integrate the bounded materializable eager baseline and exact layered accounting; add atomic on-demand multi-fault persistence; migrate the executor and simulator boundary to persisted action-aware replay; complete unassigned-runtime fallback and hard-stop reporting; and finally prove the whole feature with dummyapp, a bounded Quizzes Docker run, and post-validation live documentation. Existing schedule enumeration, input readiness, fault injection, simulator Saga unit-of-work, and Spock fixture patterns remain the implementation anchors. No new dependency is required.

## Scope and Non-Goals

### In Scope

- Clean v3 WorkloadPlan/FaultScenario package, deterministic identities, package reader/writer, manifest, and layered accounting.
- Static compensation evidence preservation and conservative checkpoint classification.
- Bounded all-zero/single-point generation, deterministic recovery-schedule cap selection, and persisted on-demand arbitrary vectors.
- Saga/local sequential replay of one persisted FaultScenario with stepwise compensation, automatic final-forward commit, truthful fallback, hard-stop behavior, and a new action-aware report version.
- WorkloadPlan-linked dynamic enrichment, dummyapp-first automated coverage, bounded Quizzes evidence, and live-documentation migration after validation.

### Out of Scope

- Any v2 read/write/upgrade/replay compatibility once v3 is canonical.
- Eager `2^n` vector enumeration or an uncomputed exact all-vector recovery total.
- New input materializers, TCC/distributed/stream/gRPC parity, true parallel execution, delay or compensation faults, automatic compensation retries, delayed commit, reset/seeding, scoring, search, or prioritization.
- Quizzes-specific generator/executor shortcuts or new third-party dependencies.

## Repo Audit

### Existing Patterns to Use

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/WorkflowFunctionalityVisitor.java` — already finds `registerCompensation` and tags recognized dispatches with `DispatchPhase`; extend the retained facts rather than adding a second parser.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/buildingblock/StepDispatchFootprint.java` and `.../scenario/adapter/ApplicationAnalysisScenarioModelAdapter.java` — current visitor-to-analysis-to-scenario boundary where phase and resolvedness must stop being discarded.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/ScenarioGenerator.java`, `ScheduleEnumerator.java`, and `ScenarioIdGenerator.java` — deterministic normalization, forward schedule generation, and SHA-256 identity patterns to preserve.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/accounting/ScenarioSpaceAccountingCalculator.java` — existing `BigInteger`/decimal-string exact-count pattern and separation of count-only from materialized output.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogJsonlWriter.java` and `ScenarioGeneratorApplication.java` — run-scoped artifact resolution, stable JSONL order, path containment, manifest writing, and configuration integration.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorReadinessEvaluator.java` and the structural checks currently private to `ScenarioExecutor.validate` — input readiness plus shape checks to separate generation admissibility from runtime materialization/startup.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/dynamic/*` — existing input/test/step attribution logic; migrate its owner from `ScenarioPlan` to WorkloadPlan without letting evidence rewrite static structure.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`, `ScenarioCatalogReader.java`, and `ScenarioExecutionReport.java` — current preparation gates, fault-boundary mapping, participant reporting, and failure classification base; v3 catalog semantics require a clean reader and a report schema version beyond current report v3.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaStep.java`, `SagaWorkflow.java`, `.../unitOfWork/SagaUnitOfWork.java`, and `SagaUnitOfWorkService.java` — runtime truth for executed steps, registered explicit compensation, implicit state rollback, stepwise abort, and retryable failed compensation.
- `applications/dummyapp/` and `verifiers/src/test/groovy/.../DummyappAccountingFixtureFoundationSpec.groovy` — canonical real-parser fixture before Quizzes-specific evidence.
- `verifiers/src/test/java/.../executor/FixtureWorkflow.java` and `verifiers/src/test/groovy/.../executor/ScenarioExecutorSpec.groovy` — deterministic runtime fixture and executor behavior style.

### Relevant Areas

- `verifiers/src/main/java/.../scenario/model/` — v2 records are currently the shared contract and will be replaced/migrated together, not dual-written.
- `verifiers/src/main/java/.../dynamic/model/EnrichedScenarioRecord.java` — currently embeds a `ScenarioPlan`; v3 enrichment must become WorkloadPlan-linked sidecar evidence.
- `verifiers/src/main/resources/application.yaml`, `application-test.yaml`, `docker-compose.yml`, and `verifiers/scripts/run-scenario-executor.sh` — current v2 artifact names, schedule cap, direct vector overlay, and executor invocation surface.
- `docs/verifiers-impl/{current-state.md,evidence.md,roadmap.md,advisor-brief.md,thesis-claims-evidence-map.md,reference/scenario-executor.md,glossary.md}` — live claims that must change only after implementation evidence passes.

### Existing Commands

- `cd verifiers && mvn -Dtest=<SpockSpecNames> test` — targeted verifier/fixture validation.
- `cd verifiers && mvn test` — full verifier regression gate.
- `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` — targeted executor-control, stepwise-recovery, and existing ordinary Saga workflow regression validation after S6/S7 introduce the named tests.
- `cd simulator && mvn test` — full simulator regression gate for shared Saga runtime changes.
- `docker compose run --rm fault-analysis-scenario-gen-test` — dummyapp verifier tests in the user-visible container path.
- `docker compose run --rm fault-analysis-scenario-gen` — Quizzes generation/runtime-evidence path; use bounded v3 overrides for this feature.
- `docker compose run --rm scenario-executor` — persisted-scenario Quizzes execution path after its v3 invocation contract is migrated.

### Dependencies / Libraries

- Existing dependency/pattern selected: Jackson, Java `MessageDigest`, `BigInteger`, NIO staged writes/atomic replacement where supported, JavaParser, Spring configuration, Spock/JUnit — all already present.
- New dependency required: `no`.

## Constraints and Planning Decisions

- Preserve `visitor/* -> ApplicationAnalysisState -> scenario/adapter/* -> scenario/* -> dynamic/*`; shared pure structural validation belongs at the scenario contract boundary, while materialization/startup remain executor gates.
- Forward schedule generation remains the existing configured schedule strategy, including segment compression. Recovery generation consumes one WorkloadPlan forward order and only inserts compensation actions.
- S2 may retain one explicitly inventoried executor-only v2 legacy island solely to keep the module compiling until S6. It cannot consume v3 packages, is not a canonical compatibility path, and must be removed with the v2 executor options/script/Compose surface in S6; S8 verifies no production references remain, excluding documented historical tests/archives.
- Exact uncapped per-vector counting and capped materialization are separate mandatory operations: counting must not retain/enumerate every leaf schedule, while representative/canonical fill stops as soon as retained capacity is satisfied.
- Diagnostic warnings must be serialized for auditability but excluded from semantic IDs. Ordered participants, inputs, forward schedule, conflict evidence, fault slots, checkpoints/evidence, vector, and ordered action identities must be closed under identity.
- The recovery cap is a separate positive setting from `maxSchedulesPerInputTuple`, defaults to `20`, and is frozen in each package manifest.
- A staged package mutation must serialize and validate all replacement bytes before publishing; on-demand failure must restore/retain all three prior package artifacts byte-for-byte, and the manifest is published last as the revision link.
- Execution accepts a persisted FaultScenario id only. Remove the execution-time vector overlay rather than preserving it under another name.
- S6 owns a narrow executor-oriented simulator control boundary: a pre-body assigned-fault transition marks the workflow aborted without executing/recording the target step, and controlled finalization exposes body-versus-commit success/failure without invoking opaque whole-Saga abort. Existing normal `execute`/`resumeWorkflow`/`resumeCompensation` behavior remains intact for non-executor callers.
- The simulator may expose narrowly structured step-recovery outcomes needed for reporting, but existing normal Saga behavior and later explicit retryability must remain intact.
- Current report v3 remains historical; compensation-aware execution uses a new schema version rather than silently changing it.
- Post-validation documentation edits belong only to S8; earlier slices must not claim pending behavior as current truth.
- Existing unrelated worktree changes in docs/issues are user-owned and must not be reverted or reformatted.

## Slice DAG

| Slice | Name | File | Depends On | ACs Covered | Risk | Primary Verification |
|-------|------|------|------------|-------------|------|----------------------|
| S1 | Compensation evidence preservation | `001-compensation-evidence-preservation.md` | none | AC-7, AC-8, AC-44 | medium | targeted visitor/adapter/dummyapp Spock specs |
| S2 | Deterministic v3 workload package | `002-deterministic-v3-workload-package.md` | S1 | AC-1, AC-2, AC-3, AC-5, AC-6, AC-43, AC-44 | high | model/generator/writer/dynamic application specs |
| S3 | Deterministic recovery schedules | `003-deterministic-recovery-schedules.md` | S2 | AC-4, AC-10–AC-18, AC-44 | high | pure recovery generator and identity specs |
| S4 | Materializable eager baseline and accounting | `004-materializable-eager-baseline-and-accounting.md` | S2, S3 | AC-1, AC-5, AC-6, AC-9, AC-10, AC-16, AC-19, AC-23, AC-24, AC-44 | high | accounting/export/application dummyapp specs |
| S5 | Atomic on-demand multi-fault persistence | `005-atomic-on-demand-multi-fault-persistence.md` | S4 | AC-15, AC-16, AC-19–AC-22, AC-42, AC-44 | high | mutation/CLI specs with byte snapshots and injected write failure |
| S6 | Exact persisted action replay | `006-exact-persisted-action-replay.md` | S4 | AC-2, AC-9, AC-14, AC-25–AC-30, AC-36–AC-42, AC-44 | high | simulator stepwise tests plus executor exact-replay specs |
| S7 | Runtime fallback and hard stops | `007-runtime-fallback-and-hard-stops.md` | S6 | AC-31–AC-41, AC-42, AC-44 | high | executor fallback/compensation/infrastructure specs |
| S8 | End-to-end evidence and live docs | `008-end-to-end-evidence-and-live-docs.md` | S5, S7 | AC-1–AC-46 (integration gate), especially AC-43–AC-46 | high | full tests, Docker dummyapp, bounded Quizzes generation/execution, artifact inspection |

Risk: `low`, `medium`, or `high`.

## Acceptance Criteria Coverage

| AC | Summary | Covered By | Required Evidence |
|----|---------|------------|-------------------|
| AC-1 | Separate v3 catalogs, manifest, accounting; no v2 artifact | S2, S4, S8 | application/export tests and run-directory artifact listing |
| AC-2 | Clear v2 rejection; no compatibility | S2, S6 | package-reader and executor schema-rejection assertions |
| AC-3 | Complete deterministic WorkloadPlan semantics/identity | S2 | identity mutation matrix and byte-stability test |
| AC-4 | Complete deterministic FaultScenario/action identity | S3 | action-field/vector/workload mutation identity tests |
| AC-5 | Byte-stable reruns and ids | S2, S3, S4, S8 | repeated generation byte comparison |
| AC-6 | Manifest links settings, modes, diagnostics, counts | S2, S4 | manifest JSON assertions |
| AC-7 | Forward/compensation phases stay distinct | S1 | visitor-to-adapter dummyapp assertion |
| AC-8 | Evidence precedence and effect-free proof | S1 | explicit/implicit/unknown/omitted fixture matrix |
| AC-9 | One materializable all-zero scenario, no compensations, auto commit | S4, S6 | eager count and exact execution assertions |
| AC-10 | Every materializable single-point vector considered; pre-body fault | S3, S4, S6 | generator vector matrix and executor body-not-called assertion |
| AC-11 | Failed owner’s later forwards omitted but diagnosable | S3, S6 | generated actions plus skipped workload-slot report |
| AC-12 | Reverse completed-checkpoint compensation order | S3, S6 | schedule and actual-order assertions |
| AC-13 | Residual global forward order is preserved | S3 | property/table-driven order validation |
| AC-14 | Final successful forward action commits automatically | S3, S6 | before/between/after generated and actual lifecycle evidence |
| AC-15 | Multi-fault masking and still-live realization | S3, S5, S6 | multi-fault generation and replay assertions |
| AC-16 | Positive recovery cap default 20; malformed values rejected pre-mutation | S3, S4, S5 | configuration tests and package checksums |
| AC-17 | All unique schedules written below cap | S3 | uncapped-vs-written equality cases |
| AC-18 | Coverage-first representatives and lexicographic fill | S3 | constructor-priority, dedup, small-cap, suffix, and fill fixtures |
| AC-19 | Cap is per-vector, frozen, and shared by eager/on-demand | S4, S5 | manifest and mismatch-byte-stability tests |
| AC-20 | Valid arbitrary vector persists before execution | S5 | request result and persisted-record assertions |
| AC-21 | Invalid requests fail without mutation | S5 | full invalid-input matrix with before/after bytes |
| AC-22 | Idempotent dedup and conflicting-id integrity error | S5 | repeated request and injected collision tests |
| AC-23 | Exact `n`, `2^n`, eager/per-vector counts without overflow | S4 | `BigInteger`/serialized decimal accounting tests |
| AC-24 | No false exact all-vector recovery aggregate | S4 | accounting schema/label assertions |
| AC-25 | Persisted id only and complete pre-execution validation | S6 | reader/validator/CLI tests; direct vector option rejected/removed |
| AC-26 | All participants prepared before measured action | S6 | materialization/startup failure traces with zero actual actions |
| AC-27 | Exact actual order absent deviation | S6 | planned-vs-actual equality assertion |
| AC-28 | Assigned pre-body fault aborts and follows persisted continuation | S6 | fixture body/abort/action-order assertions |
| AC-29 | One stepwise compensation advance; no compensation fault slot | S6 | simulator and executor invocation counts |
| AC-30 | Final body and commit phases; no body/commit on assigned final fault | S6 | final-action phase/lifecycle matrix |
| AC-31 | Zero-bit body/commit failure uses immediate fallback and continues survivors | S7 | fallback execution fixtures including partial failing step |
| AC-32 | Fallback is non-mutating and reports checkpoint/sub-outcome truth | S7 | artifact checksums and ordered recovery sub-outcomes |
| AC-33 | Infrastructure failures hard-stop as non-domain outcomes | S7 | reflection/provider/config/invocation/report failure matrix |
| AC-34 | Any compensation throw hard-stops with `COMPENSATION_FAILED`/`INCOMPLETE` | S7 | scheduled and fallback failure tests |
| AC-35 | Failed compensation remains later-explicitly retryable; no auto retry | S7 | simulator state and single-invocation assertions |
| AC-36 | Terminal participant idempotence and no-work compensation event | S6, S7 | lifecycle transition/no-double-close tests |
| AC-37 | New action-aware report identity/state contract | S6, S7 | serialized schema and required-field assertions |
| AC-38 | `EXACT`/`DEVIATED`/`INCOMPLETE`/absent conformance | S6, S7 | four-way conformance matrix |
| AC-39 | Action outcomes and fallback sub-outcomes are complete | S6, S7 | serialized planned/actual action assertions |
| AC-40 | Fault-slot states and participant skipped forwards | S6, S7 | realized/masked/not-reached/unassigned matrix |
| AC-41 | Abort/commit/compensated/no-work lifecycle events and body/commit distinction | S6, S7 | lifecycle event ordering assertions |
| AC-42 | Execution does not mutate package/enrichment/accounting | S5, S6, S7 | before/after byte checksums for every execution class |
| AC-43 | Dynamic enrichment is WorkloadPlan-linked sidecar only | S2, S8 | joiner/writer integration and no FaultScenario rewrite assertions |
| AC-44 | Dummyapp-first automated semantic coverage | S1–S8 | targeted specs plus full verifier and Docker test pass |
| AC-45 | Bounded Quizzes generation and executable interleaving evidence | S8 | saved manifest/accounting/report/log paths and inspected ids/actions |
| AC-46 | Live docs migrated after validation | S8 | documentation diff cross-checked against produced evidence |

Every in-scope AC is mapped; none is blocked.

## Verification Matrix

| Requirement / Area | Verification Type | Command / Method | Required Evidence |
|--------------------|-------------------|------------------|-------------------|
| Static phase/evidence retention | unit + dummyapp parser integration | `cd verifiers && mvn -Dtest=WorkflowFunctionalityVisitorSpec,ApplicationAnalysisScenarioModelAdapterSpec,DummyappAccountingFixtureFoundationSpec test` | passing tier/omission assertions |
| V3 workload/package and enrichment | unit + integration | `cd verifiers && mvn -Dtest=ScenarioModelSpec,ScenarioGeneratorSpec,ScenarioCatalogJsonlWriterSpec,DynamicEvidenceJoinerSpec,EnrichedScenarioCatalogWriterSpec,DummyappDynamicEnrichmentIntegrationSpec,ScenarioGeneratorApplicationSpec test` | separate artifacts, stable ids/bytes, v2 rejection, WorkloadPlan sidecar linkage |
| Recovery generation/cap | unit/property/performance fixtures | targeted recovery/FaultScenario generator specs introduced in S3, including an instrumented high-cardinality synthetic case | complete semantic matrix plus exact large count, retained count `<= cap`, bounded visited/materialized state evidence, and no full leaf retention |
| Eager accounting/export | unit + dummyapp integration | `cd verifiers && mvn -Dtest=ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec test` | materializable counts and exact decimal accounting |
| On-demand mutation | unit + CLI/service integration | targeted new catalog-mutation specs selected during S5 preflight | invalid/failure byte identity, successful consistent revision, dedup/collision behavior |
| Simulator executor control/recovery | unit + regression | `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` | target absent after pre-body abort, compensation eligibility, controlled commit failure without recovery consumption, one-checkpoint outcomes, retry marker truth, and ordinary Saga compatibility |
| Exact executor replay/report | unit/integration | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test` plus repository-wide production-reference inspection | exact planned/actual order, preparation gate, action report, artifact immutability, and no remaining v2 reader/vector-overlay production surface after S6 |
| Fallback/hard stops | unit/integration | `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` and `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | deviation/incomplete traces, hard-stop classification, no retry, and ordinary Saga regression safety |
| Full verifier regression | build/test | `cd verifiers && mvn test` | successful Maven summary |
| Full simulator regression | build/test | `cd simulator && mvn test` | successful Maven summary, or a documented unrelated pre-existing blocker with targeted gates still passing |
| Dummyapp container path | Docker integration | `docker compose run --rm fault-analysis-scenario-gen-test` | successful container exit/log |
| Bounded Quizzes generation | Docker smoke | run `fault-analysis-scenario-gen` with explicit bounded multi-Saga, `SEGMENT_COMPRESSED`, dynamic-enrichment choice, and recovery-cap `20` overrides established by S4 | host-mounted v3 package, counts, cap, and at least one compensation interleaving |
| Bounded Quizzes execution | Docker smoke | run updated `scenario-executor` against the package and one persisted FaultScenario id, with no vector overlay | action-aware report with planned/actual actions and package checksums unchanged |
| Live docs | inspection | compare listed live docs to final artifacts and commands | no v2-current claims; evidence paths and non-goals remain honest |

## Slice Files

Detailed slice cards live next to this plan:

- `001-compensation-evidence-preservation.md` — retain static phase/resolvedness facts and classify checkpoints conservatively.
- `002-deterministic-v3-workload-package.md` — replace the canonical structural contract and migrate enrichment to WorkloadPlan linkage.
- `003-deterministic-recovery-schedules.md` — generate/cap deterministic compensation-aware FaultScenario schedules.
- `004-materializable-eager-baseline-and-accounting.md` — integrate materializable all-zero/single-point generation, package export, and exact layered accounting.
- `005-atomic-on-demand-multi-fault-persistence.md` — validate and transactionally persist arbitrary requested vectors.
- `006-exact-persisted-action-replay.md` — validate and execute one persisted action schedule with automatic commit and an action-aware report.
- `007-runtime-fallback-and-hard-stops.md` — implement zero-bit fallback and truthful compensation/infrastructure failure traces.
- `008-end-to-end-evidence-and-live-docs.md` — run full/dummyapp/Quizzes gates and migrate current documentation.

## Cross-Slice Risks

- **Recovery combinatorics:** exact uncapped per-vector counts plus coverage-first capped materialization can become expensive. S3 must use non-exhaustive exact state counting, never retain every valid leaf schedule, stop canonical fill at capacity, prove this with an instrumented high-cardinality synthetic case, and pass a bounded performance smoke before Quizzes.
- **Static certainty:** the current visitor logs unresolved dispatches instead of retaining analyzable proof state. S1 must avoid turning missing evidence into read-only certainty.
- **Identity closure:** adding a semantic field without hashing it would corrupt deduplication and package immutability. S2/S3 need mutation matrices that change one semantic field at a time and warning-only controls that do not change IDs.
- **Multi-file mutation:** fixed package artifact names cannot rely on one cross-file filesystem atomic operation. S5 must stage complete bytes, validate the staged package, publish the manifest last, and test rollback/byte restoration on each injected write boundary.
- **Runtime truth:** current `SagaStep.execute` records a step before its body, there is no pre-body abort transition, `resumeCompensation` is opaque, and `Workflow.resume` consumes whole-Saga recovery on commit failure. S6 must add narrow executor controls for pre-body abort and non-opaque finalization plus checkpoint results, while preserving and regression-testing ordinary Saga APIs.
- **Failure classification:** invocation-target domain/simulator failures must not be conflated with reflection/provider/configuration failures. S7 requires explicit tests for both body and commit phases.
- **Breaking migration breadth:** model, writer, dynamic enrichment, application orchestration, executor CLI/script/compose, tests, and docs all name v2 `ScenarioPlan` artifacts. S2 leaves only the documented executor-only compile bridge; S6 removes it, and S8 performs a production zero-reference gate. No user-visible dual contract is planned.
- **Quizzes persistent state:** reset remains caller-owned. S8 must use a bounded known environment and report startup/domain deviations honestly rather than treating environmental contamination as catalog correctness.

## Planning Blockers / Deferred Decisions

- Planning blockers: `None`.
- Deferred by spec: exact compressed all-vector recovery totals, broader materialization, search/scoring/prioritization, compensation retry experiments, delay/non-binary impairments, delayed commit, TCC/distributed parity, and generic state reset.
- Just-in-time implementation choice: exact class/flag names for the on-demand request entry point and persisted-id executor option should follow the then-current simple CLI/orchestrator style; their behavior is fixed by S5/S6 even though the plan does not freeze spelling.

## Handoff

Ready for implementation: `yes`

Highest-risk slice: S3 / `003-deterministic-recovery-schedules.md` because it must combine exact valid-schedule counts, multi-fault queue enablement/masking, deterministic representative construction, and bounded materialization without changing residual forward order.

Unmapped/blocked ACs: `None`.

Recommended next step: run `sp-review-implementation-plan`, then execute slices in dependency order with the execution skill; every slice must perform just-in-time preflight before source edits.
