# Implementation Plan Review: Multi-Saga Scenario Executor Interleavings

## Recommendation

Status: `READY`

Proceed to implementation: `yes`

Fold-in status: approved revisions R1-R6 were applied to `implementation-plan.md` and the affected slice files. The detailed audit tables below preserve the original review findings; entries marked `gap`, `revise`, or `vague` refer to the pre-fold-in state and are resolved by the applied revisions.

## Sources Reviewed

- Spec: `issues/2026-07-08-multi-saga-scenario-executor/spec.md`
- Implementation plan: `issues/2026-07-08-multi-saga-scenario-executor/implementation-plan.md`
- Slice files:
  - `issues/2026-07-08-multi-saga-scenario-executor/001-v3-participant-report-and-single-saga-migration.md`
  - `issues/2026-07-08-multi-saga-scenario-executor/002-explicit-multi-saga-selection-and-dry-run.md`
  - `issues/2026-07-08-multi-saga-scenario-executor/003-multi-participant-materialization-and-startup-gates.md`
  - `issues/2026-07-08-multi-saga-scenario-executor/004-default-vector-interleaving-and-survivor-closure.md`
  - `issues/2026-07-08-multi-saga-scenario-executor/005-assigned-fault-compensate-and-continue.md`
  - `issues/2026-07-08-multi-saga-scenario-executor/006-runtime-failures-compensation-failures-and-exit-codes.md`
  - `issues/2026-07-08-multi-saga-scenario-executor/007-quizzes-smoke-and-documentation.md`
- Decision frame: `issues/2026-07-08-multi-saga-scenario-executor/decision-frame.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`, `issues/2026-07-08-multi-saga-scenario-executor/quizzes-materializability-audit.md`
- ADRs: `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md`, `docs/verifiers-impl/decisions/2026-04-28-hybrid-static-dynamic-key-binding.md`, `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`, `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`
- Repo anchors checked:
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorCli.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioCatalogReader.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/CatalogScenarioRecord.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioMaterializer.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioRuntimeContext.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorMaterializationPolicy.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestrator.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/ScenarioPlan.java`, `SagaInstance.java`, `ScheduledStep.java`, `FaultSpace.java`, `ScenarioKind.java`, `InputVariant.java`
  - `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/FaultVectorBoundaryContext.java`, `FaultVectorFault.java`, `FaultVectorInjectedFaultException.java`, `FaultVectorProviderHolder.java`, `InMemoryFaultVectorProvider.java`
  - `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowFunctionality.java`, `Workflow.java`, `ExecutionPlan.java`
  - `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaWorkflow.java`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`, `ScenarioExecutorOrchestratorSpec.groovy`, `ScenarioExecutorReadinessEvaluatorSpec.groovy`
  - `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java`
  - `verifiers/scripts/run-scenario-executor.sh`, `docker-compose.yml`, `verifiers/pom.xml`
- Commands checked:
  - `find issues/2026-07-08-multi-saga-scenario-executor -maxdepth 2 -type f | sort`
  - `rg` checks for current executor/report/status/method anchors, including `FAULT_COMPENSATED`, `stepOutcomes`, `runtimeStepName`, `exitCodeFor`, `executeUntilStep`, `resumeWorkflow`, and `resumeCompensation`
  - Python AC coverage scan over plan and slice headers; all `AC-1` through `AC-44` are mentioned in planning artifacts
  - Python catalog lookup for Quizzes candidate `0945caa9ac2fe06a268e6df6aa992fcf69e253116264684d577bdbbb955c2e25`; candidate exists at line 13 with two participants, five scheduled steps, and default vector `00000`
  - No Maven or Docker validation commands were run during this review

## Summary

The plan is broadly sound and fits the repository. The slice sequence is acyclic, dependency-ordered, and mostly vertical: v3 report migration first, then explicit multi-saga validation, preparation gates, successful replay, fault continuation, hard-stop semantics, and finally Quizzes/docs evidence. The referenced code, tests, Docker runner, and audited Quizzes catalog exist.

Do not start implementation before folding in the revisions below. They are not product decisions; they tighten the implementation contract where the current plan could let a future executor miss spec-required behavior or leave stale v2/single-saga assumptions in CLI/report validation.

## Findings Requiring User Decision

None.

## Suggested Revisions Without New Product Decisions (Applied)

| ID | Artifact | Finding | Evidence | Suggested Revision | Fold-In Target |
|----|----------|---------|----------|--------------------|----------------|
| R1 | `001-v3-participant-report-and-single-saga-migration.md`, `006-runtime-failures-compensation-failures-and-exit-codes.md` | Single-saga scheduled-step runtime failure aggregation is ambiguous. S1 says to preserve current single-saga mechanics, and S6 frames the new runtime-failure behavior as multi-saga, but the spec makes single-saga the one-participant v3 case and treats scheduled-step domain/simulator failures as participant outcomes. | Spec Behavior and Edge Cases says scheduled-step runtime failures use compensate-and-continue; aggregation rules imply one failed-and-compensated participant with no commits => `COMPENSATED`. Current code reports non-assigned step failure as `UNEXPECTED_EXECUTION_FAILURE` after compensation in `ScenarioExecutor.java`; current tests assert that v2 behavior. | In S1, qualify the temporary preservation note so it does not survive the full feature. In S6, explicitly require a one-participant/single-saga non-assigned scheduled-step exception test that migrates from `UNEXPECTED_EXECUTION_FAILURE` to v3 participant outcome aggregation (`COMPENSATED`, or `COMPENSATION_FAILED` if compensation fails). | S1 Scope/TDD and S6 Scope/TDD |
| R2 | `001-v3-participant-report-and-single-saga-migration.md` | v3 removes top-level flat `stepOutcomes`, but current CLI stdout reads `report.stepOutcomes()`. S1 mentions CLI exit-code updates but not CLI output migration. | `ScenarioExecutorCli.java` currently prints `report.stepOutcomes().forEach(...)`; S1 says not to keep top-level `stepOutcomes`. | Add S1 scope/TDD to update CLI output to either summarize participant step outcomes from the v3 participant list or print only terminal/selection summary. Include a compile-level check through `ScenarioExecutorSpec`/focused Maven run. | S1 Scope, TDD/Test Shape, Verification |
| R3 | `002-explicit-multi-saga-selection-and-dry-run.md`, `003-multi-participant-materialization-and-startup-gates.md` | v3 participant-list requirements are explicit for successful dry-run/execution and gate failures, but selected-plan validation failures are not called out. A selected plan with an invalid vector or unsupported step still needs v3 participant evidence when the saga instances are known. | Spec Data and Lifecycle Rules: participant list is empty only when no plan was selected; for a selected plan it contains exactly one participant per `SagaInstance`. S2 TDD calls out dry-run participant reports but not invalid-vector or unsupported-step selected-plan reports. | Add S2 assertions that selected multi-saga validation failures still include the participant list and participant identities when the selected plan has saga instances, while true selection failures keep participants empty. Carry the same rule into S3 materialization/startup failures. | S2 TDD/Test Shape and S3 Scope/TDD |
| R4 | `006-runtime-failures-compensation-failures-and-exit-codes.md` | `CONFIGURATION_FAILED` and `REPORT_WRITE_FAILED` are in the spec vocabulary and plan scope, but S6 has no concrete verification shape for them. | Spec terminal statuses include `CONFIGURATION_FAILED` and `REPORT_WRITE_FAILED`; S6 Scope says report-writing/configuration failures remain hard-stop/non-zero, but S6 TDD only lists runtime/provider/compensation cases. Current `writeReport(...)` throws `IllegalStateException` and current CLI required-option failures throw before report creation. | Add S6 evidence requirements for configuration/report-write failure behavior: either assert CLI exits non-zero with useful stderr when no report can be written, or assert a fallback `REPORT_WRITE_FAILED` report only if an actual fallback path is implemented. Include `CONFIGURATION_FAILED`/`REPORT_WRITE_FAILED` in exit-code helper tests if those statuses are represented as report statuses. | S6 TDD/Test Shape and Verification |
| R5 | `002-explicit-multi-saga-selection-and-dry-run.md`, `007-quizzes-smoke-and-documentation.md` | AC-1 non-mutation evidence is strong for dry-run but weak for non-dry-run execution. | Acceptance Criteria Coverage says temp-dir tests compare catalog contents before/after dry-run/execution. S2 TDD compares dry-run catalog bytes; S7 currently says to record the Quizzes input catalog path unchanged, not a byte/hash comparison. | Add a before/after byte or hash comparison for at least one non-dry-run multi-saga execution path, preferably the S7 Docker smoke catalog. | S7 Scope/Evidence to Record; optionally S4 TDD |
| R6 | `002-explicit-multi-saga-selection-and-dry-run.md` | Explicit fault vector without scenario id is required evidence for AC-19 but is not explicitly listed in S2 TDD. | Implementation plan AC-19 Required Evidence says validation table tests include explicit vector without id. S2 TDD lists malformed vectors but not the no-id case. Existing v2 tests cover it today, but S2 should preserve it after the v3 migration. | Add explicit-vector-without-`--scenario-id` to S2 TDD/validation evidence, expecting no materialization/runtime side effects and v3 invalid-vector report shape. | S2 TDD/Test Shape |

## Acceptance Criteria Coverage Audit

| AC | Planned Coverage | Verdict | Notes |
|----|------------------|---------|-------|
| AC-1 | S2, S7 | gap | Dry-run non-mutation is planned; add non-dry-run/Docker byte or hash evidence per R5. |
| AC-2 | S2, S7 | pass | Explicit deterministic id selection is covered by unit and Quizzes smoke. |
| AC-3 | S2 | pass | Auto-selection remains single-saga and multi-saga is skipped/counted. |
| AC-4 | S2 | pass | Missing requested explicit scenario id fails before runtime. |
| AC-5 | S2, S3 | pass | Unsupported shapes and gate-ineligible paths are covered before runtime. |
| AC-6 | S2 | pass | Missing and duplicate input variant cases are planned. |
| AC-7 | S3 | pass | Non-dry-run materializability gate is explicit. |
| AC-8 | S3 | pass | Materialization failure reports no runtime/provider/step side effects. |
| AC-9 | S3 | pass | Startup failure semantics match spec: no steps, closure, or compensation. |
| AC-10 | S2 | pass | Dry-run boundary excludes materialization/runtime-owned resolution/runtime execution. |
| AC-11 | S2, S4 | pass | Existing final-`::` and trailing `#<digits>` rule is anchored in current code. |
| AC-12 | S2 | pass | Unsupported step ids fail before runtime with blockers. |
| AC-13 | S4 | pass | Schedule-order execution is the primary S4 behavior. |
| AC-14 | S4 | pass | Dispatch by scheduled step owner is explicitly tested. |
| AC-15 | S3, S4 | pass | One functionality/session and one unit of work per participant is planned with fixture evidence. |
| AC-16 | S4 | pass | Survivor closure ordering is concrete and testable. |
| AC-17 | S4, S5, S6 | pass | Failed terminal participants are excluded from final closure. |
| AC-18 | S2, S4 | pass | Default vector and `DEFAULT_VECTOR` source are tested in dry-run and execution. |
| AC-19 | S2 | gap | Binary/length checks are covered; add explicit vector without scenario id per R6. |
| AC-20 | S2, S5, S6 | pass | Scenario-level fault-slot mapping and realization states are covered across slices. |
| AC-21 | S5 | pass | Provider identity matching is planned; simulator provider anchor supports scenario/plan/saga/step/runtime identity. |
| AC-22 | S5 | pass | Multiple assigned faults across surviving participants are explicitly covered. |
| AC-23 | S5, S6 | pass | Failed participant later assigned slots become `MASKED_BY_SAGA_FAILURE`. |
| AC-24 | S5 | pass | Surviving participant assigned slots remain active. |
| AC-25 | S3, S6 | pass | Whole-attempt hard-stop slots become `NOT_REACHED`. |
| AC-26 | S6 | pass | Expected-fault-not-injected is a validation hard stop, not a domain outcome. |
| AC-27 | S5 | pass | Assigned-fault compensate-and-continue is the S5 core. |
| AC-28 | S6 | gap | Multi-saga runtime exceptions are covered; add explicit one-participant/single-saga migration per R1. |
| AC-29 | S6 | pass | Compensation failure continuation and top-level `COMPENSATION_FAILED` are covered. |
| AC-30 | S2, S3, S6 | gap | Main hard stops are covered; config/report-write failure verification needs R4. |
| AC-31 | S4 | pass | `SUCCESS` only after all participants commit is planned. |
| AC-32 | S5, S6 | pass | All-compensated aggregation is planned; strengthen with R1 for single-saga runtime failure. |
| AC-33 | S5, S6 | pass | Mixed compensated/committed aggregation is planned. |
| AC-34 | S6 | pass | Any compensation failure aggregates to `COMPENSATION_FAILED`. |
| AC-35 | S1, S7 | pass | v3 schema canonicalization is owned by S1 and documented in S7. |
| AC-36 | S1-S6 | gap | Participant vocabulary is broad; add selected-plan validation-failure participant shape per R3. |
| AC-37 | S1-S6 | gap | Top-level vocabulary is broad; add config/report-write verification per R4. |
| AC-38 | S1 | pass | v2 top-level fields and `FAULT_COMPENSATED` alias removal is explicit. |
| AC-39 | S1, S7 | pass | Single-saga v3 one-participant mechanics are covered; runtime exception edge strengthened by R1. |
| AC-40 | S1-S6 | pass | Dummy/synthetic behavior coverage is extensive after the suggested additions. |
| AC-41 | S7 | pass | Quizzes candidate exists in the audited catalog and S7 has regeneration fallback. |
| AC-42 | S7 | pass | Required smoke evidence fields are listed. |
| AC-43 | S7 | pass | Reference/current-state docs update scope is clear. |
| AC-44 | S1, S6, S7 | gap | Exit-code helper and zero Docker smoke are covered; add config/report-write/non-zero vocabulary evidence per R4. |

## Slice Audit

| Slice | Verdict | Notes |
|-------|---------|-------|
| `001-v3-participant-report-and-single-saga-migration.md` | revise | Good first slice. Add CLI output migration for participant step outcomes and qualify the single-saga runtime-failure preservation note so it cannot contradict final v3 aggregation. |
| `002-explicit-multi-saga-selection-and-dry-run.md` | revise | Strong validation/dry-run slice. Add explicit-vector-without-id and selected-plan validation-failure participant-shape assertions. |
| `003-multi-participant-materialization-and-startup-gates.md` | pass | Gate sequencing, side-effect boundaries, and unit-of-work reuse are clear and fit current code. |
| `004-default-vector-interleaving-and-survivor-closure.md` | pass | Vertical end-to-end happy path with deterministic schedule and closure evidence. |
| `005-assigned-fault-compensate-and-continue.md` | pass | Fault continuation, masking, survivor continuation, and aggregation are well scoped. |
| `006-runtime-failures-compensation-failures-and-exit-codes.md` | revise | Add explicit single-saga runtime-failure migration plus config/report-write verification. |
| `007-quizzes-smoke-and-documentation.md` | revise | Good final validation slice. Add concrete before/after catalog hash/byte evidence for AC-1. |

## Dependency Audit

- Graph status: `acyclic`
- Ordering status: `clear`
- Notes: The slice files and implementation-plan DAG agree. S1 is the required breaking report migration foundation; S2 validates selected multi-saga plans without runtime; S3-S6 layer runtime behavior in dependency order; S7 is correctly last. No filename mismatches found.

## Verification Audit

| Slice / AC | Required Evidence | Verdict | Notes |
|------------|-------------------|---------|-------|
| S1 / AC-35..39,44 | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`; JSON assertions for v3 one-participant reports and no v2 fields | vague | Add CLI participant-output migration evidence and R1 wording fix. |
| S2 / AC-1..6,10..12,18..20,30,40 | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`; dry-run mapping, validation blockers, catalog immutability | vague | Add selected-plan validation-failure participants and explicit-vector-without-id test. |
| S3 / AC-7..9,15,25,30,36,37,40 | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`; readiness evaluator test if policy changes | pass | Verification is specific and repo-feasible. |
| S4 / AC-13..18,31,36,37,40 | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`; fixture order/closure/provider cleanup | pass | Good targeted evidence. |
| S5 / AC-20..24,27,32,33,36,37,40 | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`; assigned fault continuation/masking | pass | Good targeted evidence. |
| S6 / AC-17,20,25,26,28..30,32..34,36,37,40,44 | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test` | vague | Add single-saga runtime-failure migration and config/report-write failure behavior. |
| S7 / AC-1,2,35,39,41..44 | Docker `scenario-executor` smoke; docs build/inspection | vague | Add catalog byte/hash non-mutation evidence, not just path unchanged. |

## Repo Anchor Audit

| Anchor | Exists? | Fit | Notes |
|--------|---------|-----|-------|
| `ScenarioExecutor.java` | yes | good | Current v2/single-saga executor has the exact selection, validation, materialization, provider, step, compensation, and report construction seams referenced by the plan. |
| `ScenarioExecutionReport.java` | yes | good | Current record is v2 and flat; S1 correctly owns the breaking v3 participant migration. |
| `ScenarioExecutorCli.java` | yes | weak | Exit-code helper is anchored, but CLI stdout currently reads flat `stepOutcomes`; needs R2. |
| `ScenarioCatalogReader.java`, `CatalogScenarioRecord.java` | yes | good | Reader already unwraps enriched records and preserves static `ScenarioPlan` as execution contract. |
| `ScenarioMaterializer.java` | yes | good | Runtime-owned argument handling exists; current `SagaUnitOfWork` creation mismatch is real and S3 correctly owns it. |
| `ScenarioRuntimeContext.java` | yes | good | `createSagaUnitOfWork` is the right boundary for participant-owned UOW creation. |
| `ScenarioExecutorMaterializationPolicy.java` | yes | good | Whitelist matches spec: `SagaUnitOfWorkService`, `CommandGateway`, `SagaUnitOfWork`. |
| `ScenarioExecutorOrchestrator.java` and `verifiers/scripts/run-scenario-executor.sh` | yes | good | Forked/Docker command shape matches plan; mostly needs exit/status regression coverage. |
| Scenario model classes | yes | good | `ScenarioPlan`, `SagaInstance`, `ScheduledStep`, `FaultSpace`, `ScenarioKind`, and `InputVariant` support the planned participant/schedule/fault contracts. |
| Simulator fault provider classes | yes | good | Provider identity already covers scenario execution id, plan id, saga instance id, scheduled step id, slot index, and runtime step name. |
| Simulator workflow classes | yes | good | `executeUntilStep`, `resumeWorkflow`, and `resumeCompensation` exist and match executor lifecycle assumptions. |
| Executor Spock specs and `FixtureWorkflow` | yes | good | Existing tests cover current v2 behavior and provide a practical fixture to extend. |
| `verifiers/pom.xml` | yes | good | Focused `mvn -Dtest=... test` commands are plausible for Spock/Surefire setup. |
| `docker-compose.yml` service `scenario-executor` | yes | good | Service mounts `/reports` from `verifiers/target` and runs the expected script. |
| Audited Quizzes catalog path | yes | good | Candidate plan id exists with the expected two participants, five scheduled steps, and default vector `00000`. |
| Docs/current-state/reference/evidence files | yes | good | Current docs correctly describe pre-feature single-saga/v2 status and are appropriate S7 update targets. |

## Plan-vs-Spec Consistency

- R1: The plan must make clear that the final v3 behavior changes one-participant/single-saga non-assigned scheduled-step runtime failures from the current hard-stop-style report to participant outcome aggregation.
- R3: The plan should explicitly preserve the spec's participant-list invariant for selected-plan validation failures.
- R4: The plan names `CONFIGURATION_FAILED` and `REPORT_WRITE_FAILED`; verification needs to cover or explicitly document the no-report fallback behavior for write failures.
- No product-scope contradiction found after those revisions. The plan otherwise matches the spec non-goals: no true concurrency, no distributed/TCC parity, no fixture synthesis/reset, no scoring/search, and no Quizzes-specific executor shortcuts.

## Proposed Fold-In Plan

- Applied after user approval: R1, R2, R3, R4, R5, R6.
- Ask user first: None.
- Requires spec revision before plan revision: None.
- Do not fold in: None.

## Reviewer Notes

No product/source code was changed. No Maven or Docker tests were run because this was a planning-artifact review. Current code remains the expected pre-feature v2/single-saga implementation; repo contradictions against the desired multi-saga behavior are expected implementation targets, not plan blockers.
