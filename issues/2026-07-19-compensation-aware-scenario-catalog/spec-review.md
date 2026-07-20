# Spec Review: Compensation-Aware Scenario Catalog and Replay

## Recommendation

Status: `READY`

Proceed to implementation planning: `yes`

## Sources Reviewed

- Spec: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md` (final revised version; complete)
- Decision frame: `issues/2026-07-19-compensation-aware-scenario-catalog/decision-frame.md` (complete, including Q11–Q18)
- Prior review: `issues/2026-07-19-compensation-aware-scenario-catalog/spec-review.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`
- ADRs: `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`, `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`, `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md`, `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`
- Repo/docs checked: the simulator compensation/unit-of-work and verifier executor/static-analysis files cited in the two prior review passes

## Summary

No material findings remain. The final spec consistently folds D1–D8, R1–R7, and F1–F5. It is aligned with the decision frame, canonical glossary, v3 ADR and supersession boundaries, current implementation truth, executor reference, and relevant simulator behavior. Scope, identity, generation, recovery ordering, cap selection, data lifecycle, failure semantics, reporting, accounting, existing-system fit, and acceptance criteria are sufficiently explicit and testable for implementation planning.

## Findings Requiring User Decision

None.

## Suggested Revisions Without New Product Decisions

None.

## Final Follow-Up Resolution Audit

| Finding | Status | Evidence |
|---------|--------|----------|
| F1 — Identity scope | resolved | The In Scope WorkloadPlan identity now includes global forward schedule, ordered fault slots, ordered compensation checkpoints, and primary evidence classes, consistent with Solution, Data rules, Q12, glossary, and AC-3. |
| F2 — Boundary representatives | resolved | Final-step representatives target only reachable zero-bit successful automatic-commit boundaries; before/after construction, canonical queue draining, suffix handling, ordering, deduplication, and fill are total and deterministic in behavior, Q16, and AC-18. |
| F3 — Fallback report model | resolved | The spec distinguishes persisted completed-step checkpoints from runtime-only partially failed source-step references and defines one recovery action per source step/checkpoint with ordered explicit-compensation and implicit-rollback sub-outcomes in lifecycle behavior and AC-31/AC-32/AC-39. |
| F4 — Fallback compensation failure | resolved | Any scheduled or fallback recovery action/sub-outcome failure produces `COMPENSATION_FAILED`, hard-stops remaining work, retains an `INCOMPLETE` partial trace, and receives no automatic retry in behavior and AC-34. |
| F5 — Conformance vocabulary | resolved | Reporting consistently defines `EXACT`, `DEVIATED`, `INCOMPLETE`, and absence/not-applicable before measured execution in behavior, field inventory, glossary, Q17, and AC-38. |
| Earlier D1–D8 / R1–R7 | resolved | The preceding decisions and safe revisions remain present and consistent; no regression was found during the final reread. |

## Coverage Audit

| Area | Status | Notes |
|------|--------|-------|
| Decision-frame alignment | pass | Selected two-level catalog, global residual forward order, evidence/identity rules, bounded generation, frozen cap, lifecycle, failure, and conformance decisions match Q1–Q18. |
| Scope and non-goals | pass | V3-only migration and the Saga/local bounded baseline are explicit; retries, compensation faults, full-vector eager expansion, concurrency/runtime parity, scoring, search, and reset remain excluded. |
| Actors and permissions | pass | Generator, executor, enrichment, operator, and future-consumer ownership and mutation limits are clear. |
| Data lifecycle | pass | Sources of truth, immutable WorkloadPlans, consistent package revisions, dedup/integrity behavior, frozen cap, execution immutability, attempt identity, and caller-owned retention are explicit. |
| Domain language | pass | WorkloadPlan, FaultScenario, CompensationCheckpoint, evidence class, recovery schedule, automatic commit, fallback, and conformance match the glossary and distinguish design truth from implemented v2 truth. |
| User-visible edge cases | pass | Invalid requests, empty compensation work, commit failure, partial-step fallback recovery, compensation failure, preparation failure, and report conformance are covered. |
| Security/privacy/legal/ops risk | pass | No new user identity, authorization, or sharing surface is introduced; reset and retention responsibility are explicit. |
| Acceptance criteria testability | pass | Deterministic identity/order, total representative construction, accounting, validation, runtime lifecycle, failure, and report expectations have stable test or smoke oracles. |
| Existing-system fit | pass | The plan respects the existing pipeline, input-readiness boundary, pre-body fault injection, stepwise compensation, UoW retry markers, sidecar enrichment, and participant report migration boundary. |
| Open questions classification | pass | No blocking product/domain/risk question remains; deferred work is correctly classified. |

## Acceptance Criteria Audit

| AC | Verdict | Notes |
|----|---------|-------|
| AC-1 | pass | Separate v3 catalogs, manifest, and accounting with no v2 output. |
| AC-2 | pass | Explicit v2 rejection and no compatibility requirement. |
| AC-3 | pass | Complete WorkloadPlan fields and identity closure. |
| AC-4 | pass | Complete FaultScenario reference, vector, action schedule, and identity closure. |
| AC-5 | pass | Byte-stable ordering and deterministic IDs. |
| AC-6 | pass | Manifest artifacts, settings, diagnostics, and counts. |
| AC-7 | pass | Forward/compensation phase preservation. |
| AC-8 | pass | Evidence precedence, registration retention, and effect-free omission proof. |
| AC-9 | pass | Exactly one all-zero eager case and automatic commit. |
| AC-10 | pass | Every single-point vector and pre-body fault semantics. |
| AC-11 | pass | Failed-participant forward omission plus diagnostics. |
| AC-12 | pass | Reverse eligible checkpoint order. |
| AC-13 | pass | Complete global residual forward-order preservation. |
| AC-14 | pass | Successful final-step automatic commit boundary. |
| AC-15 | pass | Multi-fault realization and masking. |
| AC-16 | pass | Positive/default cap validation before mutation. |
| AC-17 | pass | Complete below-cap materialization. |
| AC-18 | pass | Total deterministic representative constructors, ordering, deduplication, and fill. |
| AC-19 | pass | Per-vector frozen package cap and mismatch rejection. |
| AC-20 | pass | On-demand arbitrary-vector generation, persistence, and execution gates. |
| AC-21 | pass | Structured rejection and byte-unchanged package on invalid requests. |
| AC-22 | pass | Byte-equivalent dedup and conflicting-ID integrity failure. |
| AC-23 | pass | Exact per-plan/per-vector accounting and non-overflow representation. |
| AC-24 | pass | No unsupported exact all-vector recovery claim. |
| AC-25 | pass | Persisted identity selection and complete pre-execution catalog validation. |
| AC-26 | pass | All-or-nothing preparation before measured actions. |
| AC-27 | pass | Exact selected action order absent deviation. |
| AC-28 | pass | Assigned fault injection, abort, and persisted continuation. |
| AC-29 | pass | One stepwise advancement per planned compensation action and no compensation fault slots. |
| AC-30 | pass | Final body/commit phases and final assigned-fault behavior. |
| AC-31 | pass | Body/commit zero-bit fallback and runtime-only partial-step recovery reference. |
| AC-32 | pass | Non-mutating fallback with one source-step recovery action and ordered sub-outcomes. |
| AC-33 | pass | Infrastructure failure classification and hard stop. |
| AC-34 | pass | Scheduled/fallback recovery failure hard stop, `COMPENSATION_FAILED`, and `INCOMPLETE`. |
| AC-35 | pass | Later explicit retryability without automatic retry. |
| AC-36 | pass | Terminal idempotence and `NO_COMPENSATION_WORK`. |
| AC-37 | pass | New action-aware report version and required identity/state fields. |
| AC-38 | pass | Complete conformance vocabulary and applicability. |
| AC-39 | pass | Planned/runtime-only action references, positions, evidence, exceptions, and ordered recovery sub-outcomes. |
| AC-40 | pass | Fault-slot and skipped-forward diagnostics. |
| AC-41 | pass | Abort/commit/compensated/no-work lifecycle events and body/commit outcomes. |
| AC-42 | pass | Execution artifact immutability. |
| AC-43 | pass | Workload-linked sidecar-only dynamic enrichment. |
| AC-44 | pass | Dummyapp-first deterministic and runtime semantic coverage. |
| AC-45 | pass | Bounded Quizzes generation/execution evidence without an unsupported scale threshold. |
| AC-46 | pass | Post-validation live-documentation migration. |

## Repo / Docs Contradictions

None found.

## Proposed Fold-In Plan

- Apply automatically after user approval: `None`
- Ask user first: `None`
- Do not fold in: `None`

## Reviewer Notes

Implementation planning is safe to begin. This review approves clarity, consistency, risk treatment, testability, and existing-system fit; it does not approve product desirability or claim that the pending v3 behavior is already implemented.

This final follow-up changed only `spec-review.md`; it did not modify the spec, decision frame, glossary, ADRs, current-state/reference docs, or source code.
