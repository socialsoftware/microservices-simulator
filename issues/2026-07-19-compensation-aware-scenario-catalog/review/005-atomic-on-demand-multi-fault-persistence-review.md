# Slice Review: 005 - Atomic On-Demand Multi-Fault Persistence

## Review Attempt

Attempt: `05`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes`

## Sources Reviewed

- Slice: `issues/2026-07-19-compensation-aware-scenario-catalog/005-atomic-on-demand-multi-fault-persistence.md`
- Spec: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Implementation plan: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`
- ADRs: `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`, `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`, `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md`, `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`
- Completion evidence: `005-atomic-on-demand-multi-fault-persistence.md` → `## Completion Evidence`, including all four review-fix evidence sections
- Dependency evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/done/004-materializable-eager-baseline-and-accounting.md` and `issues/2026-07-19-compensation-aware-scenario-catalog/review/004-materializable-eager-baseline-and-accounting-review-02.md`
- Changed files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/EagerFaultScenarioGenerator.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/accounting/ScenarioSpaceAccountingReport.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/{FaultScenarioRequestCli,OnDemandFaultScenarioRequest,OnDemandFaultScenarioResult,OnDemandFaultScenarioService,ScenarioCatalogJsonlWriter}.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/{FaultScenarioVectorSource,ScenarioCatalogManifest}.java`; `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioServiceSpec.groovy`
- Relevant existing files reviewed: `ScenarioCatalogPackageReader.java`, `FaultScenarioValidator.java`, `WorkloadPlanValidator.java`, `RecoveryScheduleGenerator.java`, the S3/S4 result/model records, and `ScenarioExecutorCli.java`
- Prior review reports: `issues/2026-07-19-compensation-aware-scenario-catalog/review/005-atomic-on-demand-multi-fault-persistence-review-{01,02,03,04}.md`
- Commands run by reviewer:
  - `mvn -Dtest=OnDemandFaultScenarioServiceSpec,ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec test` from the repository root — expected invocation-location failure because the repository has no root POM; rerun from `verifiers/`
  - `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec,ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec test`
  - `cd verifiers && mvn -Dtest=RecoveryScheduleGeneratorSpec test`
  - `git diff --check`
  - worktree, dependency, changed-file/reference, executor-reference, review-attempt, and done-path collision inspection

## Summary

The complete S5 implementation now satisfies the on-demand mutation contract. A request validates the manifest and linked artifact boundary before dereferencing package files, verifies checksums, schemas, references, materializability, exact eager/on-demand vector accounting, deterministic generated counts/id sets, and the frozen cap, then delegates the requested vector to the S3 recovery generator. New records and accounting rows are canonically merged, all replacement bytes are staged and validated, data/accounting are promoted before the manifest, promotion failures restore the original mutable artifacts, and successful cleanup is best-effort. The request path remains independent of execution.

The attempt-04 blocker is fixed: package-wide consistency now retains each freshly evaluated WorkloadPlan materializability result and rejects every computed-vector row whose owner is not materializable. The focused two-workload regression constructs an otherwise exact/hash-consistent carried on-demand vector for a blocked workload and proves pre-mutation rejection with all three mutable artifacts byte-identical. Earlier aggregate/count/id-set, path-before-read, package-alias serialization, and cleanup fixes remain present. Fresh reviewer runs pass the 113-test S5/S4 suite and the 14-test recovery-generator suite.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | pass | The service/CLI accept one manifest, WorkloadPlan id, vector, and optional asserted cap; validation, deterministic generation, dedup/collision handling, canonical accounting/catalog revision, staged validation, manifest-last publication, rollback, and structured results are implemented. |
| Slice out-of-scope respected | pass | No batch/search policy, prioritization, executor invocation, WorkloadPlan mutation, cap mutation, or runtime materialization/startup claim was introduced. |
| Spec non-goals respected | pass | No eager `2^n` expansion, all-vector exact recovery total, scoring/search, compensation faults, distributed/TCC parity, reset support, or new dependency was added. |
| Dependencies done | pass | S4 is in `done/` and its latest review, attempt 02, is `PASS`. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-15 | pass | The valid `0011` case persists bounded scenarios in which assigned faults realize for both still-live participants and both compensation queues are represented; the 14-test S3 recovery suite remains green. | Runtime replay remains S6 scope. |
| AC-16 | pass | The package's positive frozen cap is required; asserted `0`, `-1`, and malformed values reject before staging, while S4 retains the default `20` behavior. | |
| AC-19 | pass | Generation uses the manifest cap, and a different asserted cap rejects before mutation even after the vector already exists. | |
| AC-20 | pass | A valid ready, structurally admissible four-slot workload persists bounded multi-fault FaultScenarios plus accounting/manifest metadata; staged and final package validation complete before `PERSISTED` is returned. | Runtime materialization/startup are not claimed. |
| AC-21 | pass | Missing plan, non-binary/wrong-length vector, malformed slots, blocked workloads, outside/symlink paths, checksum/count/package inconsistencies, and a carried blocked-workload vector return structured rejection and retain exact mutable bytes. | The attempt-04 package-wide eligibility hole is closed. |
| AC-22 | pass | Repeated requests reconcile fresh exact counts and the complete generated id set, return `DEDUPLICATED`, and preserve bytes; duplicate existing ids, same-id/different-content collisions, false carried counts, and alternate carried id sets reject without mutation. | |
| AC-42 | pass | The on-demand path has no `ScenarioExecutor` dependency or invocation; successful mutation changes only FaultScenario JSONL, accounting, and manifest, while workload and rejected-input artifacts remain unchanged. | Full execution immutability remains S6/S7 scope. |
| AC-44 | pass | The 36-case focused suite covers valid multi-fault persistence, invalid inputs/packages, package-wide eligibility and deterministic reconciliation, dedup/collision, all six stage/promotion boundaries, fallback, cleanup, ordering, package-local concurrency/aliases, CLI output, and reader inspection. | Feature-wide AC-44 remains distributed across later slices. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| Focused on-demand plus S4 regression suite | pass | Fresh reviewer run: `Tests run: 113, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. This includes 36 focused on-demand cases. |
| Recovery generator regression | pass | Fresh reviewer run: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| Successful package inspection through v3 reader | pass | The successful and fallback service cases reload the published package; service code also validates staged and final revisions before success. |
| Invalid/collision/failure byte snapshots | pass | Focused tests compare all three mutable artifacts for the invalid matrix, package tampering, collision/deterministic mismatch, and every declared staging/promotion boundary. |
| Package-wide materializability regression | pass | The new two-workload case supplies deterministic/count/hash-consistent blocked-workload scenarios and accounting, requests a separate ready workload, and observes `REJECTED/INVALID_PACKAGE` with exact byte identity. |
| Package-local guard and path-before-read behavior | pass | Same-package manifest aliases serialize under the canonical package-directory guard; outside and symlink target tokens do not appear in diagnostics. |
| Canonical ordering and repeat stability | pass | Opposite request orders produce identical FaultScenario/accounting bytes; repeat requests preserve all mutable bytes. |
| `git diff --check` | pass | No whitespace errors. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Changes are confined to the on-demand boundary, required package checksum/source/count metadata, accounting extension, shared materializability access, and focused tests. |
| Existing patterns | pass | Uses Jackson records, S3 generation, S4 readers/validators, `BigInteger` exact counts, stable sorting, NIO same-directory staging/moves, and the existing simple CLI parsing style without a new dependency. |
| Test quality | pass | Tests are behavioral and adversarial: they inspect published package semantics and bytes rather than asserting only internal calls, and include package-wide cross-vector/cross-workload cases. |
| Regression risk | pass | The focused S5/S4 and S3 suites pass; staged and final validation use the same package-wide invariants, and all declared failure boundaries retain/restore prior bytes. |
| Security/data safety | pass | Containment/symlink checks occur before linked-target reads, hashes and exact semantic metadata are reconciled, package-local writes are serialized in-process, and manifest-last rollback behavior is covered. |
| Change hygiene | pass | `git diff --check` passes; unrelated meeting-note files and later active slices were not modified. |

## Findings

None.

## Done Transition

- Moved to done: `yes`
- Done path: `issues/2026-07-19-compensation-aware-scenario-catalog/done/005-atomic-on-demand-multi-fault-persistence.md`
- Reason if not moved: `n/a`

## Reviewer Notes

The mutation guard intentionally provides the slice-required single-process serialization; cross-process locking and crash-consistent filesystem transactions are not claimed. Runtime replay remains owned by S6/S7.
