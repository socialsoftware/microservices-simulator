# Implementation Plan Review: Scenario Executor Fault Vectors

## Recommendation

Status: `READY_AFTER_REVISION`

Proceed to implementation: `after approved revisions`

## Sources Reviewed

- Spec: `issues/2026-07-07-scenario-executor-fault-vectors/spec.md`
- Implementation plan: `issues/2026-07-07-scenario-executor-fault-vectors/implementation-plan.md`
- Slice files:
  - `issues/2026-07-07-scenario-executor-fault-vectors/001-simulator-in-memory-fault-provider.md`
  - `issues/2026-07-07-scenario-executor-fault-vectors/002-executor-vector-validation-and-report-v2.md`
  - `issues/2026-07-07-scenario-executor-fault-vectors/003-no-fault-lifecycle-closure.md`
  - `issues/2026-07-07-scenario-executor-fault-vectors/004-realized-fault-compensation-and-masking.md`
  - `issues/2026-07-07-scenario-executor-fault-vectors/005-mismatch-unexpected-and-compensation-failures.md`
  - `issues/2026-07-07-scenario-executor-fault-vectors/006-cli-docker-runner-and-quizzes-smoke.md`
  - `issues/2026-07-07-scenario-executor-fault-vectors/007-scenario-executor-docs.md`
- Decision frame: `issues/2026-07-07-scenario-executor-fault-vectors/decision-frame.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor-poc.md`, `docs/verifiers-impl/roadmap.md`, `docs/verifiers-impl/README.md`
- ADRs: None referenced or required by the spec.
- Repo anchors checked:
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOptions.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorCli.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestrator.java`
  - `verifiers/scripts/run-scenario-executor.sh`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/FaultSpace.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/ScenarioPlan.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/ScheduledStep.java`
  - `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/ExecutionPlan.java`
  - `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/Workflow.java`
  - `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowFunctionality.java`
  - `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaWorkflow.java`
  - `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/impairment/ImpairmentHandler.java`
  - `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/monitoring/dynamic/DynamicEvidenceRecorderHolder.java`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`
  - `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestratorSpec.groovy`
  - `simulator/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/sagas/workflow/WorkflowExecutionPlanTest.groovy`
  - `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/ExecutionPlanDynamicEvidenceTest.java`
  - `docker-compose.yml`, `mkdocs.verifier.yml`
- Commands checked:
  - `find issues/2026-07-07-scenario-executor-fault-vectors -maxdepth 2 -type f | sort`
  - path-existence shell check for repo anchors listed above
  - `rg -n "scenario-executor-poc|ScenarioExecutor POC" docker-compose.yml mkdocs.verifier.yml docs/verifiers-impl -g '!site/**'`
  - targeted `rg -n` checks for workflow abort/compensation and plan AC mappings

## Summary

The plan is mostly ready: scope matches the spec, the slice DAG is acyclic, every acceptance criterion has a planned implementation path, and the referenced code anchors exist. The decomposition is intentionally staged and justifies the simulator-provider foundation slice before executor behavior.

Revise before implementation. The plan needs four safe clarifications: make typed injected faults explicitly participate in existing saga abort/compensation detection, make current fault-slot identity binding and prefix timing concrete enough to prevent wrong-slot or early injection, map AC-7 cleanup evidence to S5 failure paths, and broaden/fix docs cleanup for live POC references.

## Findings Requiring User Decision

None.

## Suggested Revisions Without New Product Decisions

| ID | Artifact | Finding | Evidence | Suggested Revision | Fold-In Target |
|----|----------|---------|----------|--------------------|----------------|
| R1 | `001-simulator-in-memory-fault-provider.md`, `004-realized-fault-compensation-and-masking.md` | The typed injected-fault signal is not explicitly tied to the existing workflow abort semantics required before compensation. | `Workflow.executeUntilStep(...)` marks `aborted = true` only for `CompletionException` and `SimulatorException`; `SagaWorkflow.resumeCompensation(...)` throws when `!aborted`. S1 says to add a typed signal but does not require it to extend `SimulatorException` or otherwise set abort state. | State that the provider-injected signal must either extend `SimulatorException` or be caught by workflow code in a way that marks the workflow aborted before `resumeCompensation(...)`. Add simulator or executor evidence that a provider fault makes compensation legal and that ordinary runtime failures remain distinguishable. | S1 Implementation Shape/TDD/Verification; S4 Implementation Shape/TDD |
| R2 | `001-simulator-in-memory-fault-provider.md`, `004-realized-fault-compensation-and-masking.md` | Current fault-slot identity binding is too implicit, and fault timing needs a concrete prefix-execution test. | S1 requires signal identity fields such as scenario execution id, scheduled step id, and slot index. Current `ExecutionPlan` only has functionality and runtime `stepName`; catalog `ScheduledStep` ids live in verifier models. S4 says to provide boundary identity before `executeUntilStep(...)` only "if needed". Current `ExecutionPlan.executeSteps(...)` checks fault values for all steps in a batch before executing bodies, so a naive provider integration could fault a later target before earlier prefix steps run. | Make the simulator-owned boundary context/scope explicit: the executor binds the current scenario execution id, plan id, saga instance id, slot index, scheduled step id, and runtime step name before each forward boundary, then clears it. Add tests that a vector such as `010` executes the first step, injects before the second step body, and reports the exact slot identity; add a wrong-slot/duplicate-runtime-name guard if feasible. | S1 Scope/Implementation Shape/TDD/Risks; S4 Implementation Shape/TDD |
| R3 | `implementation-plan.md`, `005-mismatch-unexpected-and-compensation-failures.md` | AC-7 cleanup coverage is inconsistent: S5 owns failure cleanup behavior, but the plan's DAG and AC table omit AC-7 from S5. | Plan Slice DAG lists S5 ACs as `AC-13, AC-14, AC-15, AC-16, AC-17, AC-20, AC-21`. AC-7 evidence says cleanup must cover success/fault/failure. S5 Scope/TDD already says provider state is cleared after every failure path. | Add `AC-7` to S5's AC list in the Slice DAG and slice header, and add S5 to the AC-7 coverage row as failure-path cleanup evidence. | `implementation-plan.md` Slice DAG and AC coverage; `005-mismatch-unexpected-and-compensation-failures.md` header |
| R4 | `007-scenario-executor-docs.md`, `implementation-plan.md` | Docs cleanup misses live POC references and uses an `rg` check that will be noisy if archive references are intentionally preserved. | Live docs currently reference POC framing in `docs/verifiers-impl/advisor-brief.md` and `docs/verifiers-impl/thesis-claims-evidence-map.md`, but S7 scope/anchors omit them. The verification command `rg "scenario-executor-poc|ScenarioExecutor POC" docs/verifiers-impl mkdocs.verifier.yml` also searches `docs/verifiers-impl/archive/**`, while S7 says archive updates are out of scope unless needed. | Add `advisor-brief.md` and `thesis-claims-evidence-map.md` to S7 scope/anchors if they remain live truth/prep docs. Change the docs verification command to exclude archive paths, for example `rg "scenario-executor-poc|ScenarioExecutor POC" docs/verifiers-impl mkdocs.verifier.yml -g '!archive/**'`, plus optional manual inspection of intentional archive mentions. | S7 Scope/Repo Anchors/Verification; implementation-plan docs verification row |

## Acceptance Criteria Coverage Audit

| AC | Planned Coverage | Verdict | Notes |
|----|------------------|---------|-------|
| AC-1 | S2, S6 | pass | CLI/options and Docker runner coverage are planned, including explicit vector requiring explicit scenario id. |
| AC-2 | S2, S3, S6 | pass | Default-vector validation/reporting and CSV suppression are covered. |
| AC-3 | S2 | pass | Malformed vectors, invalid defaults, and malformed fault-space mappings are planned before execution. |
| AC-4 | S2 | pass | Dry-run mapping and no provider/materialization/execution behavior are planned. |
| AC-5 | S1 | pass | Simulator-owned plain-value provider and no verifier model dependency are covered. |
| AC-6 | S1, S4 | revise | Identity fields are planned, but R1/R2 should make abort integration and current-slot binding explicit. |
| AC-7 | S1, S3, S4 | revise | Failure-path cleanup belongs in S5 too; see R3. |
| AC-8 | S1 | pass | Legacy CSV/manual behavior outside vector scopes is covered. |
| AC-9 | S1, S3, S4 | pass | Active provider authority and CSV suppression are covered. |
| AC-10 | S3 | pass | No-fault lifecycle closure through `resumeWorkflow(...)` is clear. |
| AC-11 | S4, S6 | revise | Behavior is planned; R1/R2 should ensure compensation is legal and prefix timing is tested. |
| AC-12 | S4 | pass | Masked later assigned slots are planned. |
| AC-13 | S5 | pass | Missing expected injected fault gets exact status coverage. |
| AC-14 | S5 | pass | Unexpected/wrong-slot provider signal statuses are planned; R2 strengthens identity plumbing. |
| AC-15 | S5 | pass | Ordinary `0`-bit failures and best-effort closure are covered. |
| AC-16 | S5 | pass | Compensation failure status, details, and non-zero exit mapping are covered. |
| AC-17 | S2, S3, S4, S5 | pass | Deterministic slot and step ordering are covered across dry-run/success/fault/failure. |
| AC-18 | S2, S3 | pass | Runtime metadata boundary and no DB snapshot are covered. |
| AC-19 | S2, S3 | pass | Report-only side effect and no catalog/sidecar mutation are covered. |
| AC-20 | S2, S3, S4, S5, S6 | pass | Exit-code classes and Docker smoke exit evidence are planned. |
| AC-21 | S2, S3, S4, S5 | pass | Dummyapp-style fixture coverage is broad; S5 cleanup mapping should be reflected via R3. |
| AC-22 | S6 | pass | Default success and explicit compensated Quizzes Docker smokes are planned. |
| AC-23 | S7 | revise | S7 should include all live POC references and fix the archive-aware verification command; see R4. |

## Slice Audit

| Slice | Verdict | Notes |
|-------|---------|-------|
| `001-simulator-in-memory-fault-provider.md` | revise | Strong foundation slice, but it should explicitly require abort-compatible typed injected faults and a current-boundary fault-slot context/scope. |
| `002-executor-vector-validation-and-report-v2.md` | pass | Clear vertical slice for vector validation, dry-run, report schema, and side-effect boundary. Dependency on S1 is conservative but acceptable. |
| `003-no-fault-lifecycle-closure.md` | pass | Correctly replaces old no-resume behavior with committed lifecycle closure and provider cleanup. |
| `004-realized-fault-compensation-and-masking.md` | revise | Behavior is well scoped; revise to make compensation legality and boundary identity/timing tests concrete. |
| `005-mismatch-unexpected-and-compensation-failures.md` | revise | Negative-path semantics are clear; add AC-7 to the slice because it owns provider cleanup after failure paths. |
| `006-cli-docker-runner-and-quizzes-smoke.md` | pass | Runner and Docker smoke scope is appropriate and does not add CSV/vector generation. |
| `007-scenario-executor-docs.md` | revise | Correct goal, but include remaining live docs with POC framing and make verification archive-aware. |

## Dependency Audit

- Graph status: `acyclic`
- Ordering status: `clear`
- Notes: S1 -> S2 -> S3 -> S4 -> S5 -> S6 -> S7 is conservative and executable. S2 could theoretically start before S1, but depending on the simulator provider values first is acceptable and lowers schema/API churn.

## Verification Audit

| Slice / AC | Required Evidence | Verdict | Notes |
|------------|-------------------|---------|-------|
| S1 / AC-5..AC-9 | `cd simulator && mvn -Dtest=<new-provider-test>,ExecutionPlanDynamicEvidenceTest test`; CSV compatibility/override test | revise | Add explicit evidence that injected faults integrate with workflow abort state and that second-slot injection does not preempt earlier prefix steps. |
| S2 / AC-1..AC-4, AC-17..AC-21 | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | pass | Validation and dry-run evidence is concrete enough. |
| S3 / AC-10 | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`; `WorkflowExecutionPlanTest` if simulator lifecycle changes | pass | Good targeted validation. |
| S4 / AC-11..AC-12 | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`; provider timing test if simulator adjusted | revise | Add first-step-prefix-executed and compensation-legal evidence. |
| S5 / AC-13..AC-16, AC-20, AC-21 | `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | revise | Add AC-7 failure cleanup to required evidence. |
| S6 / AC-22 | `cd verifiers && mvn -Dtest=ScenarioExecutorOrchestratorSpec test`; two `docker compose run --rm scenario-executor` smokes | pass | Correctly requires actual Docker evidence before claiming AC-22. |
| S7 / AC-23 | docs `rg` plus `./scripts/verifier-docs build` if available | revise | Make the `rg` command exclude archives or explicitly separate live vs historical references. |

## Repo Anchor Audit

| Anchor | Exists? | Fit | Notes |
|--------|---------|-----|-------|
| `verifiers/.../executor/ScenarioExecutor.java` | yes | good | Current selection/materialization/step loop/report-writing entry point. |
| `verifiers/.../executor/ScenarioExecutorOptions.java` | yes | good | Current options record is small and suitable for adding `faultVector`. |
| `verifiers/.../executor/ScenarioExecutorCli.java` | yes | good | Current CLI parses flags and starts Spring; exit-code/report handling must be added. |
| `verifiers/.../executor/ScenarioExecutorOrchestrator.java` | yes | good | Current forked command builder is the correct S6 anchor. |
| `verifiers/scripts/run-scenario-executor.sh` | yes | good | Current Docker entrypoint has `SCENARIO_ID` handling and no `FAULT_VECTOR`. |
| `verifiers/.../executor/ScenarioExecutionReport.java` | yes | good | Current v1 loose string report is the correct replacement/evolution point. |
| `FaultSpace.java`, `ScenarioPlan.java`, `ScheduledStep.java` | yes | good | They expose fault-space/default-vector and schedule ids as planned; current constructors allow malformed spaces, matching S2 validation need. |
| `simulator/.../coordination/ExecutionPlan.java` | yes | good | Correct provider integration point; watch current batch fault-check timing in `executeSteps(...)`. |
| `Workflow.java`, `WorkflowFunctionality.java`, `SagaWorkflow.java` | yes | good | Correct lifecycle anchors; abort/compensation coupling creates R1. |
| `ImpairmentHandler.java` | yes | good | Correct CSV compatibility boundary. |
| `DynamicEvidenceRecorderHolder.java` | yes | good | Useful holder style reference, though new provider needs stronger scoped/concurrency semantics than the current recorder holder. |
| `ScenarioExecutorSpec.groovy`, `FixtureWorkflow.java` | yes | good | Correct focused executor fixtures; old `resumeCalls == 0` expectation is intentionally replaced in S3. |
| `ScenarioExecutorOrchestratorSpec.groovy` | yes | good | Correct command-construction coverage target. |
| `ExecutionPlanDynamicEvidenceTest.java`, `WorkflowExecutionPlanTest.groovy` | yes | good | Good simulator test anchors for provider/timing/lifecycle regressions. |
| Quizzes behaviour tests/resources | yes | good | CSV compatibility fixtures exist under the paths cited by the plan. |
| `docker-compose.yml` scenario-executor service | yes | good | Currently exposes `SCENARIO_ID`; S6 should add `FAULT_VECTOR`. |
| docs/nav files | yes | revise | Additional live docs with POC framing should be added to S7; see R4. |

## Plan-vs-Spec Consistency

- No product-scope contradiction found. The plan preserves the spec's single-saga saga/local boundary, simulator-owned fault injection, default/explicit vector behavior, v2 report side-effect boundary, CSV compatibility outside vector scopes, and future-work exclusions.
- The plan needs implementation-contract clarifications for abort-compatible injected faults and exact current-slot binding; these are required to satisfy the existing spec, not new behavior.

## Proposed Fold-In Plan

- Apply automatically after user approval: R1, R2, R3, R4.
- Ask user first: None.
- Requires spec revision before plan revision: None.
- Do not fold in: None.

## Reviewer Notes

No tests were run for this review; this was a planning/repo-anchor audit only. The implementation plan should remain implementation-guiding, not code-prescriptive, but R1/R2 are concrete enough to prevent a likely compensation/timing failure in the first simulator slice.