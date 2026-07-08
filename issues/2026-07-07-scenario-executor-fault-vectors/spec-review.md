# Spec Review: Scenario Executor Fault Vectors

## Recommendation

Status: `READY_AFTER_REVISION`

Proceed to implementation planning: `after approved revisions`

## Sources Reviewed

- Spec: `issues/2026-07-07-scenario-executor-fault-vectors/spec.md`
- Decision frame: `issues/2026-07-07-scenario-executor-fault-vectors/decision-frame.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/README.md`, `mkdocs.verifier.yml`
- ADRs: None found/referenced for this feature
- Repo/docs checked: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/FaultSpace.java`, `ScenarioPlan.java`, `ScheduledStep.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/*`, `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/ExecutionPlan.java`, `Workflow.java`, `WorkflowFunctionality.java`, `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaWorkflow.java`, `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/impairment/ImpairmentHandler.java`, `verifiers/scripts/run-scenario-executor.sh`, `docker-compose.yml`

## Summary

The spec is materially aligned with the decision frame and is scoped well: single-saga saga/local execution now, simulator-owned in-memory fault injection, no CSV executor contract, no search/scoring/batching. It is close enough to plan after small revisions.

Proceed to implementation planning after confirming the folded-in revisions. The context/glossary source-of-truth mismatch, malformed `faultSpace`/default-vector validation, and v2 report status/lifecycle vocabulary have been addressed in the spec/glossary.

## Findings Requiring User Decision

None.

## Suggested Revisions Without New Product Decisions

| ID | Area | Finding | Evidence | Suggested Revision | Fold-In Target |
|----|------|---------|----------|--------------------|----------------|
| R1 | Domain language / docs source of truth | Resolved: the spec now routes terminology through `CONTEXT-MAP.md` to `docs/verifiers-impl/glossary.md`, and the mapped glossary contains the executor/fault-vector terms. | Spec Domain References: `spec.md`; context map: `CONTEXT-MAP.md`; glossary: `docs/verifiers-impl/glossary.md`. | No remaining action. | Done |
| R2 | Vector and fault-space validation | Validation covers wrong-length/non-binary explicit vectors and non-unique mappings, but it does not explicitly cover malformed plan fault spaces or invalid `faultSpace.defaultVector`. Current `FaultSpace` permits `length` to differ from `scheduledStepIds.size()` and preserves a caller-supplied `defaultVector` without length/binary validation. | Spec validation: `spec.md` lines 86 and 89, AC-2/AC-3 lines 148-149. Code: `FaultSpace.java` lines 6-16, `ScenarioPlan.java` line 25. | State that the same strict validation applies to explicit and default vectors before provider installation/materialization/execution. Add structural checks: `faultSpace.length == scheduledStepIds.size()`, selected vector length equals `faultSpace.length`, selected vector is binary, every `scheduledStepId` resolves exactly one forward `expandedSchedule` entry, and fault-slot ids are unique. Add dummyapp coverage for invalid default vector and malformed fault-space mapping. | `spec.md` Behavior and Edge Cases, Data and Lifecycle Rules, AC-3, AC-4, AC-21 |
| R3 | Report/status contract testability | The v2 report is intended to be machine-readable, but terminal/lifecycle status names are not fully enumerated and some ACs allow “or equivalent” statuses. That weakens the contract for future search/scoring consumers and makes tests less stable. | Report contract: `spec.md` lines 101-102, 107-118. AC-13/AC-14: `spec.md` lines 159-160. Existing v1 report is a loose string schema: `ScenarioExecutionReport.java` lines 6-26. | Add a small v2 report enum table for `terminalStatus`, provider-consistency statuses, `lifecycleOutcome`, `vectorSource`, and fault-slot realization states. Prefer the exact labels already named in the spec, and remove “or equivalent” from AC-13/AC-14 unless aliases are explicitly listed. | `spec.md` Behavior and Edge Cases, Data and Lifecycle Rules, AC-13, AC-14, AC-17, AC-20 |

## Coverage Audit

| Area | Status | Notes |
|------|--------|-------|
| Decision-frame alignment | pass | Selected in-memory simulator provider, single-saga executable scope, CSV rejection as canonical contract, and future search/scoring deferral match the decision frame. |
| Scope and non-goals | pass | In/out-of-scope boundaries are explicit and prevent multi-saga/TCC/batch/search overbuild. |
| Actors and permissions | pass | Relevant actors are listed; simulator ownership and verifier mapping boundary are clear. |
| Data lifecycle | gap | Report sidecar behavior and provider cleanup are clear, but malformed `faultSpace`/default-vector lifecycle needs R2. |
| Domain language | pass | Canonical glossary target now matches `CONTEXT-MAP.md`; see R1. |
| User-visible edge cases | pass | Explicit/default vectors, dry-run, masking, provider mismatch, unexpected failures, compensation failures, and exit-code classes are covered. R3 tightens names, not behavior. |
| Security/privacy/legal/ops risk | pass | No new sensitive-data behavior. Operational risks around provider cleanup, concurrent providers, and persistent DB reset ownership are addressed. |
| Acceptance criteria testability | gap | Most ACs are testable. AC-13/AC-14 should use exact status names; AC-3/AC-21 should include malformed/default-vector validation. |
| Existing-system fit | pass | Repo check confirms existing `ScenarioPlan`/`FaultSpace`, single-saga executor, simulator `ExecutionPlan`/CSV impairment path, workflow lifecycle primitives, and Quizzes Docker runner assumptions. |
| Open questions classification | pass | No product/domain decisions remain blocking after the suggested revisions. Deferred multi-saga/search/impact-analysis questions are correctly future work. |

## Acceptance Criteria Audit

| AC | Verdict | Notes |
|----|---------|-------|
| AC-1 | pass | CLI/Docker explicit-vector surface and explicit scenario-id requirement are clear. |
| AC-2 | revise | Behavior is clear, but default vector must be explicitly validated under the same strict rules as explicit vectors; see R2. |
| AC-3 | revise | Add malformed `faultSpace` checks and default-vector validation; see R2. |
| AC-4 | pass | Dry-run behavior is clear. R2 only adds what it must validate. |
| AC-5 | pass | Simulator-owned API and no verifier model dependency are clear. |
| AC-6 | pass | Injected-fault identity requirements are sufficient for current single-saga scope and future multi-saga resolution through scheduled-step identity. |
| AC-7 | pass | Cleanup and concurrent-provider rejection/unsupported behavior are clear. |
| AC-8 | pass | Legacy CSV/manual behavior compatibility outside executor-owned vector runs is clear. |
| AC-9 | pass | CSV suppression/override during executor-owned vector runs is clear. |
| AC-10 | pass | Success closure and committed lifecycle outcome are clear; R3 asks only to enumerate lifecycle names. |
| AC-11 | pass | Reached `1` bit behavior, pre-body injection, compensation, and `FAULT_COMPENSATED` terminal result are clear. |
| AC-12 | pass | Masked assigned `1` bits and mask reason are clear. |
| AC-13 | revise | Replace “or equivalent” with exact status/enum values; see R3. |
| AC-14 | revise | Replace slash/“such as” ambiguity with exact provider-consistency status values; see R3. |
| AC-15 | pass | Unexpected `0`-bit failure behavior and best-effort closure are clear. |
| AC-16 | pass | Compensation failure status, details, and non-zero exit are clear. |
| AC-17 | pass | Mapping and step ordering are clear. R3 should add enum vocabulary to the same report contract. |
| AC-18 | pass | Reproduction metadata boundary is clear and avoids full DB snapshots. |
| AC-19 | pass | Executor report-only side effect is explicit. |
| AC-20 | pass | Zero/non-zero classes are clear. R3 should keep terminal-status names exact. |
| AC-21 | revise | Add invalid default vector and malformed fault-space mapping coverage when R2 is folded in. |
| AC-22 | pass | Quizzes smoke scope is small and aligned with Docker/test profile assumptions. |
| AC-23 | revise | Include the canonical glossary/context-map correction and broader POC-reference cleanup, not only the reference-doc rename; see R1. |

## Repo / Docs Contradictions

- Current docs still intentionally describe the executor as a POC: `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/README.md`, `mkdocs.verifier.yml`, and `docs/verifiers-impl/reference/scenario-executor-poc.md`. This is not a contradiction before implementation, but AC-23 explicitly includes these references in the docs cleanup path.
- No product/code contradiction found with the selected direction. The repo currently has `FaultSpace` on `ScenarioPlan`, a v1 executor report, single-saga executor behavior, simulator CSV impairment in `ExecutionPlan`/`ImpairmentHandler`, and saga lifecycle primitives that the spec correctly targets for replacement/extension.

## Proposed Fold-In Plan

- Apply automatically after user approval: R1, R2, R3.
- Ask user first: None.
- Do not fold in: None.

## Reviewer Notes

The spec should remain a behavior/specification artifact, not an implementation plan. The suggested revisions are contract clarifications only; they should not add multi-saga execution, batch vector execution, search, scoring, or generic reset behavior.
