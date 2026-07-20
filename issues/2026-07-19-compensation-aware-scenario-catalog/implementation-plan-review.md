# Implementation Plan Review: Compensation-Aware Scenario Catalog and Replay

## Recommendation

Status: `READY`

Proceed to implementation: `yes`

## Sources Reviewed

- Spec: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Implementation plan: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`
- Fold-in slices: `002-deterministic-v3-workload-package.md`, `003-deterministic-recovery-schedules.md`, `006-exact-persisted-action-replay.md`, `007-runtime-fallback-and-hard-stops.md`, `008-end-to-end-evidence-and-live-docs.md`
- Prior review findings: R1–R4 from the preceding version of this report
- Repo anchors rechecked: current executor `ScenarioPlan`/enriched-wrapper references; `SagaStep.execute`; `Workflow.executeUntilStep` and `Workflow.resume`; `SagaWorkflow`; `SagaUnitOfWork`; `SagaUnitOfWorkService`; simulator test inventory; Compose and executor script

## Summary

R1–R4 are resolved. The fold-ins preserve the approved spec, add no dependency or product behavior, and make the affected slices executable and verifiable against the current repository. The S2/S6 legacy boundary is now explicit and temporary; S6 owns the simulator controls required for pre-body faults and non-opaque commit failure; S3 now requires non-exhaustive exact counting and lazy capped fill; and S6–S8 now name targeted and full simulator regression gates. No material issue remains from the follow-up review.

## Follow-Up Resolution Audit

| Finding | Status | Fold-In Evidence | Spec / Repo Check | Remaining Material Issue |
|---------|--------|------------------|-------------------|--------------------------|
| R1 — S2-to-S6 migration boundary | resolved | The plan Constraints and Cross-Slice Risks now permit only an inventoried executor-only compile bridge in S2. S2 scopes, inventories, tests, and audits that bridge; S6 removes the bridge plus v2 reader/vector-overlay Java, CLI, script, and Compose surfaces; S8 performs a production zero-reference gate with explicit historical/test exclusions. | This matches the v3-only final contract while acknowledging that current `CatalogScenarioRecord`, `ScenarioCatalogReader`, `ScenarioExecutor`, and enriched-wrapper paths compile against `ScenarioPlan`. It introduces no final v2 compatibility. | None. |
| R2 — simulator pre-body abort and controlled finalization | resolved | Plan Constraints/Cross-Slice Risks and S6 Scope/Implementation/TDD now require a pre-body abort transition that neither invokes nor records the target step, plus controlled body/commit finalization that does not consume recovery on failure. S7 explicitly consumes the controlled result for commit fallback and forbids ordinary `resumeWorkflow` on that executor path. | This directly addresses current repo behavior: `SagaStep.execute` records before the body, `Workflow.executeUntilStep` has no pre-body abort operation, and `Workflow.resume` currently performs opaque abort on commit failure. Existing ordinary APIs remain preserved for non-executor callers, consistent with the spec and simulator compatibility requirement. | None. |
| R3 — non-exhaustive exact recovery counting | resolved | Plan Constraints/Verification/Risks and S3 Scope/Implementation/TDD/Verification now make separation mandatory: exact counting cannot visit/retain every leaf, representative construction is finite, lexicographic fill is lazy and stops at capacity, and an instrumented high-cardinality oracle plus bounded performance smoke is required before Quizzes. | This preserves exact uncapped counts, deterministic representative priority, lexicographic fill, and cap semantics from AC-17/AC-18/AC-23 while making the bounded Quizzes path operationally credible. It does not add random sampling or weaken exactness. | None. |
| R4 — simulator regression verification | resolved | The plan and S6/S7/S8 now name `SagaExecutorControlTest`, `SagaStepwiseRecoveryTest`, and existing `WorkflowExecutionPlanTest`; S8 also runs `cd simulator && mvn test`, with honest reporting if an unrelated pre-existing blocker prevents a full pass. | The named existing regression anchor is present, and the planned new tests align with the shared `Workflow`, `SagaWorkflow`, `SagaUnitOfWork`, and `SagaUnitOfWorkService` changes. Targeted plus full-suite evidence is proportionate to the runtime risk. | None. |

## Findings Requiring User Decision

None.

## Suggested Revisions Without New Product Decisions

None.

## Affected Acceptance Criteria Audit

| AC | Follow-Up Verdict | Notes |
|----|-------------------|-------|
| AC-2 | pass | Temporary bridge is explicitly non-v3 and removed before the final canonical v3 executor state. |
| AC-17 / AC-18 | pass | Complete below-cap output and deterministic capped representatives now have a non-exhaustive implementation/verification contract. |
| AC-23 | pass | Exact per-vector counts remain required with arbitrary precision and compact state counting. |
| AC-25 | pass | S6 owns complete removal of the legacy reader/vector overlay and adds a zero-reference gate. |
| AC-28 | pass | S6 now requires a true pre-body abort transition with target-body and executed-step absence assertions. |
| AC-30 | pass | Controlled finalization explicitly separates body and commit phases and commits only after successful closure. |
| AC-31 / AC-32 | pass | S7 receives unconsumed checkpoint truth from S6 controlled finalization and can report commit-failure fallback accurately. |
| AC-35 | pass | Retry-marker/no-auto-retry semantics now have named simulator tests plus ordinary-workflow regression coverage. |
| AC-44 | pass | Dummyapp/verifier coverage is now backed by concrete simulator controls, scale instrumentation, and regression gates. |
| AC-45 | pass | Quizzes smoke is gated by the high-cardinality oracle and bounded performance check rather than cap-only assumptions. |
| AC-46 | pass | S8 verifies final v2 production-surface removal before live docs call v3 canonical. |

All other AC mappings and verdicts from the initial review are unchanged and remain passing.

## Slice Audit

| Slice | Verdict | Notes |
|-------|---------|-------|
| `002-deterministic-v3-workload-package.md` | pass | The only intermediate v2 path is inventoried, compile-only/non-v3, non-growing, and explicitly handed to S6 for removal. |
| `003-deterministic-recovery-schedules.md` | pass | Exact counting and capped materialization are unconditionally separated and have a high-space verification oracle. |
| `006-exact-persisted-action-replay.md` | pass | Owns legacy removal, pre-body abort, controlled finalization, focused simulator tests, executor tests, and production-reference inspection. |
| `007-runtime-fallback-and-hard-stops.md` | pass | Correctly builds on S6 controlled finalization and verifies checkpoint truth, retry behavior, and ordinary Saga compatibility. |
| `008-end-to-end-evidence-and-live-docs.md` | pass | Adds high-space, full simulator, zero-reference, Docker, evidence, and post-validation documentation gates. |

Unchanged slices `001`, `004`, and `005` were not re-reviewed beyond confirming that the fold-ins did not alter their dependency contracts.

## Dependency Audit

- Graph status: `acyclic`
- Ordering status: `clear`
- Notes: S2 records the temporary bridge; S6 removes it after S4 and establishes runtime controls; S7 consumes those controls; S8 joins S5 and S7 and verifies the final state. No dependency filename or ordering mismatch remains.

## Verification Audit

| Area | Verdict | Notes |
|------|---------|-------|
| Legacy migration | pass | S2 inventory plus S6/S8 production-reference gates make the temporary state and cleanup reviewable. |
| Pre-body fault / commit fallback | pass | Named simulator controls and focused state assertions cover the repo gaps identified in R2. |
| Recovery combinatorics | pass | Exact high count, retained `<= cap`, bounded visited/materialized evidence, and a pre-Quizzes smoke are required. |
| Simulator compatibility | pass | Named focused tests, existing `WorkflowExecutionPlanTest`, and the full simulator suite are included. |
| Final integration | pass | Full verifier, Docker dummyapp, bounded Quizzes generation/execution, checksums, and live-doc reconciliation remain required. |

## Plan-vs-Spec Consistency

- No new contradiction, overbuild, unapproved compatibility promise, dependency, or product decision was introduced.
- The fold-ins make existing spec behavior executable; they do not change fault timing, commit semantics, recovery ordering, cap semantics, failure policy, or compatibility scope.
- No spec revision is required.

## Proposed Fold-In Plan

- Apply automatically after user approval: `None — R1–R4 already folded in and resolved`
- Ask user first: `None`
- Requires spec revision before plan revision: `None`
- Do not fold in: `None`

## Reviewer Notes

This was a focused follow-up review of the approved R1–R4 fold-ins, not a new full-plan review. No source, spec, implementation plan, or slice file was edited by the reviewer. No Maven or Docker commands were run because this review validates planning changes rather than implemented behavior; those commands are now explicit execution/review gates in the plan.
