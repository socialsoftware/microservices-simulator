# Spec Review: Multi-Saga Scenario Executor Interleavings

## Recommendation

Status: `READY`

Proceed to implementation planning: `yes`

## Sources Reviewed

- Spec: `issues/2026-07-08-multi-saga-scenario-executor/spec.md`
- Decision frame: `issues/2026-07-08-multi-saga-scenario-executor/decision-frame.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`, `issues/2026-07-08-multi-saga-scenario-executor/quizzes-materializability-audit.md`
- ADRs: `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md`, `docs/verifiers-impl/decisions/2026-04-28-hybrid-static-dynamic-key-binding.md`, `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`, `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`
- Repo/docs checked: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`, `ScenarioExecutionReport.java`, `ScenarioExecutorCli.java`, `ScenarioCatalogReader.java`, `ScenarioExecutorReadinessEvaluator.java`, `ScenarioRuntimeContext.java`, `ScenarioExecutorOrchestrator.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/ScenarioPlan.java`, `SagaInstance.java`, `ScheduledStep.java`, `FaultSpace.java`, `ScenarioKind.java`; `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/*`; `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowFunctionality.java`, `Workflow.java`, `ExecutionPlan.java`; `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaWorkflow.java`, `SagaUnitOfWorkService.java`; `verifiers/scripts/run-scenario-executor.sh`

## Summary

The spec is well scoped and aligned with the selected thesis direction: deterministic sequential replay of catalog `expandedSchedule`, one participant per `SagaInstance`, existing materialization policy, no true concurrency, no fixture synthesis, no scoring/search, and a real Quizzes smoke backed by the materializability audit.

The blocking v3 terminal-status/single-saga/CLI exit-code contract decision has been resolved and folded into the spec. The spec is now ready for implementation planning.

Resolved decision: v3 is canonical for both single- and multi-saga reports; `FAULT_COMPENSATED` is not preserved as a v3 terminal alias; single-saga execution keeps existing execution mechanics through the participant model; CLI/Docker exit zero for `SUCCESS`, `COMPENSATED`, `PARTIAL_COMPENSATED`, and `DRY_RUN`; `COMPENSATION_FAILED` exits non-zero.

## Findings Requiring User Decision

None remaining.

Resolved D1: v3 is canonical for both single- and multi-saga reports; no v3 `FAULT_COMPENSATED` alias; AC-39 preserves single-saga execution mechanics rather than v2 report/status strings; CLI/Docker exit zero for `SUCCESS`, `COMPENSATED`, `PARTIAL_COMPENSATED`, and `DRY_RUN`; `COMPENSATION_FAILED` and hard-stop/setup/validation/provider/configuration/report-writing failures exit non-zero.

## Suggested Revisions Without New Product Decisions (Applied)

| ID | Area | Finding | Evidence | Suggested Revision | Fold-In Target |
|----|------|---------|----------|--------------------|----------------|
| R1 | v3 fault-slot contract | Fault-slot state names are not fully enumerated for v3. | Spec names `MASKED_BY_SAGA_FAILURE` and `NOT_REACHED`, and says reached-but-not-injected assigned slots are validation failures. Current v2 docs/code use `NOT_ASSIGNED`, `REALIZED`, `MASKED`, and `UNREALIZED`. AC-23, AC-25, and AC-26 depend on exact machine-readable state. | Add a v3 fault-slot state table. At minimum define exact labels for unassigned slots, realized assigned slots, slots masked by failed participant, assigned slots not reached due to whole-attempt hard stop, and assigned slots reached without expected injection. Also state the terminal status/blocker used for the reached-without-injection validation failure. | `spec.md` Behavior and Edge Cases, Data and Lifecycle Rules, AC-20, AC-23, AC-25, AC-26, AC-36, AC-37 |
| R2 | Startup failure lifecycle | Startup failure reports do not say how already materialized or already instantiated participants are represented or cleaned up. | Spec says all participant inputs materialize before runtime execution, and startup failure happens before scheduled-step execution (`spec.md` Behavior, AC-8, AC-9). Current executor creates one functionality and one `SagaUnitOfWork` for single-saga startup, and saga lifecycle closure/compensation uses runtime methods that should not be confused with scheduled-step outcomes. | Define startup as a pre-forward hard-stop phase: no scheduled steps, no final `resumeWorkflow`, and no scenario compensation. For participants successfully materialized/instantiated before another participant fails startup, specify their report startup/lifecycle state and whether cleanup is best-effort non-scenario cleanup or intentionally not attempted. | `spec.md` Behavior and Edge Cases, Data and Lifecycle Rules, AC-9, AC-30, AC-36 |
| R3 | Dry-run/materialization boundary | Dry-run validation is structurally scoped, but the spec also says supported multi-saga scenarios require every participant input to be materializable. | Spec dry-run text validates participant/input/step/fault-vector mapping and performs no runtime execution (`spec.md` Behavior, AC-10). AC-7 requires materializable participant inputs for supported execution. Current v2 dry-run validates mapping/vector and does not materialize. | State explicitly that multi-saga dry-run does not invoke materialization or runtime-owned argument resolution; it validates structural participant/input/step/fault mapping only. Materialization blockers are reported by non-dry-run attempts. | `spec.md` Behavior and Edge Cases, AC-7, AC-8, AC-10 |
| R4 | Decision artifact authority | Resolved: the failure-policy and v3-report ADRs now match the spec's selected behavior. | The ADR status headers were changed to `Status: accepted`; the spec Domain References now describes them as accepted. | No remaining action. | Done |
| R5 | v3 schema testability | The v3 report is described by categories but lacks a concrete schema/version vocabulary anchor. | AC-35..AC-38 define participant/top-level categories and removal of v2 top-level fields, but do not name the v3 schema version or exact status vocabularies. Current `ScenarioExecutionReport.SCHEMA_VERSION` is `microservices-simulator.scenario-execution-report.v2`. | Add the v3 schema version string and exact enum/status tables for `terminalStatus`, participant lifecycle, materialization/startup states, step outcomes, `vectorSource`, `providerMode`, and blocker placement. Keep this a contract table, not an implementation design. | `spec.md` Data and Lifecycle Rules, AC-35, AC-36, AC-37, AC-38, AC-40 |

## Coverage Audit

| Area | Status | Notes |
|------|--------|-------|
| Decision-frame alignment | pass | The selected deterministic interleaving runner, compensate-and-continue policy, explicit multi-saga selection, and v3 participants model match the decision frame. The single-saga/v3/exit-code decision is folded into the spec. |
| Scope and non-goals | pass | The spec sharply excludes true parallelism, distributed parity, TCC, fixture synthesis/reset, retry, deferred compensation, scoring, search, prioritization, and Quizzes-specific shortcuts. |
| Actors and permissions | pass | Executor caller, ScenarioExecutor, and future scorer responsibilities are clear. |
| Data lifecycle | pass | Catalog/enrichment immutability, standalone report behavior, startup-failure participant lifecycle, exact v3 vocabulary, and accepted ADR authority are now specified. |
| Domain language | pass | Terms align with `docs/verifiers-impl/glossary.md`; no root `CONTEXT.md` issue. |
| User-visible edge cases | pass | Runtime edge cases now include exact statuses, fault-slot states, startup report semantics, dry-run boundaries, and exit-code behavior. |
| Security/privacy/legal/ops risk | pass | No new security/privacy/legal risk. CLI/Docker exit-code behavior is now specified. |
| Acceptance criteria testability | pass | ACs now include exact v3 status/state/schema, dry-run/startup boundaries, and CLI/Docker exit-code behavior. |
| Existing-system fit | pass | Repo check confirms current static `ScenarioPlan` supports multi-saga records, `expandedSchedule`, `FaultSpace`, and saga-instance ids; current executor is intentionally single-saga/v2; in-memory provider identity already includes scenario execution id, plan id, saga instance id, scheduled step id, slot index, and runtime step name. |
| Open questions classification | pass | No blocking open questions remain after D1 was resolved and folded in. |

## Acceptance Criteria Audit

| AC | Verdict | Notes |
|----|---------|-------|
| AC-1 | pass | Static/enriched catalog loading without artifact mutation matches current reader direction and ADRs. |
| AC-2 | pass | Explicit `--scenario-id` selecting by deterministic plan id is clear. |
| AC-3 | pass | Multi-saga auto-selection is explicitly deferred; single-saga auto-selection remains. |
| AC-4 | pass | Missing explicit scenario id failure before materialization/runtime is clear. |
| AC-5 | pass | Unsupported shapes pre-runtime with blockers is clear. |
| AC-6 | pass | One matching input variant per `SagaInstance` is clear enough; implementation should enforce exactly one match, not first match. |
| AC-7 | pass | Non-dry-run materializability is explicit; dry-run is excluded. |
| AC-8 | pass | Materialization failure before runtime step execution is clear. |
| AC-9 | pass | Startup failure pre-step behavior, participant states, and no lifecycle cleanup/compensation are explicit. |
| AC-10 | pass | Dry-run explicitly performs no materialization, runtime-owned argument resolution, or runtime execution. |
| AC-11 | pass | Runtime step-name normalization matches current code behavior. |
| AC-12 | pass | Unsupported step ids fail before runtime; exact blocker/status can be covered by R5. |
| AC-13 | pass | Ascending `scheduleOrder` replay is clear. |
| AC-14 | pass | Dispatch by scheduled step `sagaInstanceId` is clear. |
| AC-15 | pass | One functionality/session and one unit of work per participant is clear. |
| AC-16 | pass | Successful survivor closure ordering is clear. |
| AC-17 | pass | Failed-and-compensated participants are terminal and not closed again. |
| AC-18 | pass | Default vector and `vectorSource=DEFAULT_VECTOR` are clear. |
| AC-19 | pass | Explicit vector validation is clear. |
| AC-20 | pass | Scenario-level slot mapping and exact v3 fault-slot states are specified. |
| AC-21 | pass | Provider identity requirements are clear and fit current simulator provider fields. |
| AC-22 | pass | Multiple assigned faults across surviving participants are clear. |
| AC-23 | pass | `MASKED_BY_SAGA_FAILURE` is the exact v3 state. |
| AC-24 | pass | Surviving participant assigned slots stay active after another participant fails. |
| AC-25 | pass | `NOT_REACHED` is specified for assigned slots skipped by whole-attempt hard stops. |
| AC-26 | pass | Terminal status, fault-slot state, and blocker placement are specified for expected faults not injected. |
| AC-27 | pass | Assigned-fault compensate-and-continue behavior is clear. |
| AC-28 | pass | Non-assigned scheduled-step runtime exceptions follow the same compensate-and-continue policy; the one-participant/single-saga v3 behavior is clarified. |
| AC-29 | pass | Compensation failure continuation, top-level status, and non-zero process exit are clear. |
| AC-30 | pass | Hard-stop infrastructure/setup statuses are explicitly separated from domain scenario outcomes. |
| AC-31 | pass | `SUCCESS` aggregation is clear. |
| AC-32 | pass | `COMPENSATED` aggregation is clear and replaces single-saga v2 `FAULT_COMPENSATED` in v3 reports. |
| AC-33 | pass | `PARTIAL_COMPENSATED` aggregation is clear. |
| AC-34 | pass | `COMPENSATION_FAILED` aggregation is clear and exits non-zero. |
| AC-35 | pass | Canonical v3 schema version and no v2 status compatibility are specified. |
| AC-36 | pass | Participant contents now reference exact v3 vocabulary. |
| AC-37 | pass | Top-level facts now reference exact v3 vocabulary. |
| AC-38 | pass | No v2 top-level single-saga identity compatibility fields is explicit. |
| AC-39 | pass | Single-saga keeps execution mechanics through the participant model, not v2 report/status strings. |
| AC-40 | pass | Coverage now includes exact v3 status/state vocabulary and CLI exit-code mapping. |
| AC-41 | pass | Quizzes smoke target is credible because the audit found materializable multi-saga plans. |
| AC-42 | pass | Smoke evidence fields are explicit. |
| AC-43 | pass | Docs update scope is clear; ADR status/authority is aligned. |
| AC-44 | pass | CLI/Docker exit-code contract is explicit, including non-zero `COMPENSATION_FAILED`. |

## Repo / Docs Contradictions

- The current repo/docs state that ScenarioExecutor is single-saga/v2 and multi-saga execution is not implemented. That is expected pre-implementation, not a contradiction.
- The two new decision docs for failure policy and v3 participants are now marked `Status: accepted`, matching the spec's selected behavior.
- No contradiction found with the core feasibility claims: `ScenarioPlan` has `MULTI_SAGA`, `SagaInstance`, `expandedSchedule`, and `FaultSpace`; the simulator in-memory fault-vector provider already matches on scenario execution id, scenario plan id, saga instance id, scheduled step id, slot index, and runtime step name; the Quizzes audit found materializable multi-saga candidates.

## Proposed Fold-In Plan

- Applied after user approval: D1 with `COMPENSATION_FAILED` non-zero, plus R1, R2, R3, R4, R5.
- Ask user first: None remaining.
- Do not fold in: None.

## Reviewer Notes

No source/product code was changed. Folded approved planning-doc revisions into `issues/2026-07-08-multi-saga-scenario-executor/spec.md` and accepted the two referenced decision docs.
