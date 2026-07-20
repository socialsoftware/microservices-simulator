# Slice Review: 005 - Atomic On-Demand Multi-Fault Persistence

## Review Attempt

Attempt: `04`

Latest copy updated: `yes`

## Verdict

`FAIL`

Moved to done: `no`

## Sources Reviewed

- Slice: `issues/2026-07-19-compensation-aware-scenario-catalog/005-atomic-on-demand-multi-fault-persistence.md`
- Spec: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Implementation plan: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`
- ADRs: `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`, `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`, `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md`, `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`
- Completion evidence: `005-atomic-on-demand-multi-fault-persistence.md` → `## Completion Evidence`, including the attempt-01, attempt-02, and attempt-03 fix evidence
- Dependency evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/done/004-materializable-eager-baseline-and-accounting.md` and `issues/2026-07-19-compensation-aware-scenario-catalog/review/004-materializable-eager-baseline-and-accounting-review-02.md`
- Changed files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/EagerFaultScenarioGenerator.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/accounting/ScenarioSpaceAccountingReport.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/{FaultScenarioRequestCli,OnDemandFaultScenarioRequest,OnDemandFaultScenarioResult,OnDemandFaultScenarioService,ScenarioCatalogJsonlWriter}.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/{FaultScenarioVectorSource,ScenarioCatalogManifest}.java`; `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioServiceSpec.groovy`
- Relevant existing files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogPackageReader.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/{FaultScenarioValidator,RecoveryScheduleCap,RecoveryScheduleGenerator,WorkloadPlanValidator}.java`; the S3/S4 model, accounting, package, and CLI anchors referenced by the slice
- Prior review reports: `issues/2026-07-19-compensation-aware-scenario-catalog/review/005-atomic-on-demand-multi-fault-persistence-review-01.md`, `issues/2026-07-19-compensation-aware-scenario-catalog/review/005-atomic-on-demand-multi-fault-persistence-review-02.md`, `issues/2026-07-19-compensation-aware-scenario-catalog/review/005-atomic-on-demand-multi-fault-persistence-review-03.md`
- Commands run by reviewer:
  - `mvn -Dtest=OnDemandFaultScenarioServiceSpec,ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec test` from repository root — failed because this repository has no root POM; rerun from `verifiers/`
  - `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec,ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec test`
  - `cd verifiers && mvn -Dtest=RecoveryScheduleGeneratorSpec test`
  - `git diff --check`
  - worktree, dependency, source, attempt-number, and done-path collision inspection
  - isolated Java package-integrity probe under `/tmp/s5-review-blocked-carried-vector6738727006918369261`: constructed a checksum/count/deterministic-generation-consistent package containing an `ON_DEMAND_REQUEST` vector for a freshly confirmed non-materializable WorkloadPlan, then requested the same valid vector for a separate materializable WorkloadPlan

## Summary

The attempt-03 fix now reconciles exact counts and complete FaultScenario-id sets for every carried computed vector. The earlier path-before-read, package-alias serialization, aggregate-count, direct dedup, and cleanup fixes also remain present. Fresh reviewer runs pass the 112-test S5/S4 suite and the 14-test recovery suite.

The slice still cannot pass because package validation does not require computed vectors to belong to materializable WorkloadPlans. It freshly establishes that one workload is non-materializable, then nevertheless accepts an exact, hash-consistent `ON_DEMAND_REQUEST` row and deterministic FaultScenario set for that workload. The reviewer probe requested another, materializable workload; the service returned `PERSISTED`, changed the package, and retained the non-materializable workload's on-demand vector in the newly validated revision. This violates the slice's pre-mutation input-readiness boundary and the contract that on-demand FaultScenarios exist only for materializable WorkloadPlans.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | fail | The service checks readiness for the workload currently requested, but package-wide validation accepts and republishes carried on-demand vectors owned by a WorkloadPlan whose freshly evaluated inputs are not ready. |
| Slice out-of-scope respected | pass | No batch/search policy, prioritization, execution, WorkloadPlan mutation, cap mutation, or runtime materialization/startup claim was introduced. |
| Spec non-goals respected | pass | No eager `2^n` enumeration, exact all-vector recovery total, search/scoring, compensation faults, distributed/TCC parity, or new dependency was added. |
| Dependencies done | pass | S4 is under `done/`; its latest review is attempt 02 with verdict `PASS`. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-15 | pass | The valid `0011` case persists generated scenarios with realized faults and compensation queues for both still-live participants; the S3 recovery suite remains green. | Runtime replay remains later-slice scope. |
| AC-16 | pass | Package cap use is positive/frozen; asserted zero, negative, and malformed values reject before staging. | |
| AC-19 | pass | The manifest cap is authoritative, and mismatch rejects before generation/mutation even for an already persisted vector. | |
| AC-20 | fail | A valid request persists before return, but the resulting supposedly validated package can retain on-demand FaultScenarios for a separate non-ready WorkloadPlan. | Successful revision consistency must preserve the on-demand materializability invariant package-wide. |
| AC-21 | fail | Directly requesting a non-ready workload rejects, but the same non-ready workload's exact/hash-consistent carried on-demand vector is accepted during existing-package validation and survives a successful mutation. | Input readiness is an explicit pre-mutation package boundary, not only a check on the newly named id. |
| AC-22 | pass | Fresh exact counts and complete id sets are now reconciled package-wide; direct repeats deduplicate, while same-id/different-content and deterministic mismatches remain non-mutating failures. | Attempt-03's exactness finding is fixed. |
| AC-42 | pass | The request path remains separate from `ScenarioExecutor` and normal success changes only FaultScenario JSONL, accounting, and manifest. | Full execution immutability remains S6/S7 scope. |
| AC-44 | fail | The focused suite covers normal persistence, request invalidity, path boundaries, count/id tampering, rollback, fallback, cleanup, canonical order, concurrency, and CLI behavior, but not a carried on-demand vector owned by a non-materializable workload. | The isolated probe demonstrates the missing package-wide eligibility case. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| Focused on-demand plus S4 regression suite | pass | Fresh reviewer run: `Tests run: 112, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. This includes 35 focused on-demand cases. |
| Recovery generator regression | pass | Fresh reviewer run: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| Successful staged/final v3 package validation | pass | Normal tests reload the published package, and stage/promotion injections preserve or restore the original mutable bytes. |
| Attempt-03 package-wide count/id reconciliation | pass | Source inspection confirms each computed row is regenerated under the frozen cap; eager, retained on-demand, and alternate-id tamper regressions pass. |
| Package-wide computed-vector materializability | fail | In `/tmp/s5-review-blocked-carried-vector6738727006918369261`, fresh evaluation reported `blockedMaterializable=false`. The package contained its exact `0011` `ON_DEMAND_REQUEST` row/scenarios with matching counts and hashes. Requesting `0011` for a distinct ready workload returned `PERSISTED`, changed the FaultScenario catalog, and retained the blocked workload row. |
| `git diff --check` | pass | No whitespace errors. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Changes remain confined to the request contracts/CLI/service, package hashes/source/count metadata, accounting extension, shared materializability access, and focused tests. |
| Existing patterns | pass | Uses Jackson, immutable records, S3 deterministic generation, S4 readers/validators, exact decimal counts, NIO same-directory staging, stable sorting, and Spock fixtures without a new dependency. |
| Test quality | fail | Tests now prove package-wide deterministic exactness, but do not cross materializability diagnostics with every carried computed-vector owner. |
| Regression risk | fail | A tampered or externally produced package can retain FaultScenarios that this service itself was required to reject for non-ready inputs, and a successful request republishes that invalid state. |
| Security/data safety | fail | Path handling is safe and byte rollback tests pass, but package data integrity is incomplete because successful mutation can bless an ineligible workload's on-demand experiment. |
| Change hygiene | pass | `git diff --check` passes; unrelated untracked meeting-note files were not modified. |

## Findings

| Severity | Finding | Evidence | Required Fix |
|----------|---------|----------|--------------|
| blocking | Package-wide computed-vector validation does not require the owning WorkloadPlan to be materializable, so an exact/hash-consistent on-demand vector for a non-ready workload is accepted and republished. | `OnDemandFaultScenarioService.java:639-650` freshly computes materializability, but the row loop at lines `663-714` validates only workload existence, vector shape/source, counts, generated ids, and cap. It never rejects a row whose owner has `materializable=false`; the eager check at lines `719-735` only excludes that workload from the eager map, and workload accounting at lines `760-780` merely accepts its on-demand count. The isolated two-workload probe reported `blockedMaterializable=false`, then `requestStatus=PERSISTED`, `blockedRowRetained=true`, and `faultCatalogChanged=true`. | During pre-mutation, staged, and final validation, require every computed-vector row and its FaultScenarios to belong to a freshly confirmed materializable WorkloadPlan, including `ON_DEMAND_REQUEST` rows. Reject violations as `INVALID_PACKAGE` before generation/staging. Add a regression with one ready and one blocked workload: inject a deterministic/count/hash-consistent on-demand row for the blocked workload, request a distinct vector for the ready workload, and prove structured rejection plus exact byte identity for all three mutable artifacts. |

## Done Transition

- Moved to done: `no`
- Done path: `None`
- Reason if not moved: `FAIL` — package-wide materializability validation remains incomplete for carried on-demand vectors.

## Reviewer Notes

The attempt-03 exact-count/id-set defect is resolved. The new finding is not a malformed generator-output case: the carried blocked-workload row was fully deterministic under S3 and internally count/hash consistent, which isolates the missing eligibility invariant. The probe touched `/tmp` only; unrelated worktree changes were preserved.
