# Slice Review: 005 - Atomic On-Demand Multi-Fault Persistence

## Review Attempt

Attempt: `03`

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
- Completion evidence: `005-atomic-on-demand-multi-fault-persistence.md` → `## Completion Evidence`, including the attempt-01 and attempt-02 fix evidence
- Dependency evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/done/004-materializable-eager-baseline-and-accounting.md` and `issues/2026-07-19-compensation-aware-scenario-catalog/review/004-materializable-eager-baseline-and-accounting-review-02.md`
- Changed files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/EagerFaultScenarioGenerator.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/accounting/ScenarioSpaceAccountingReport.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/{FaultScenarioRequestCli,OnDemandFaultScenarioRequest,OnDemandFaultScenarioResult,OnDemandFaultScenarioService,ScenarioCatalogJsonlWriter}.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/{FaultScenarioVectorSource,ScenarioCatalogManifest}.java`; `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioServiceSpec.groovy`
- Relevant existing files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogPackageReader.java`; the S3/S4 generator, validator, model, accounting, package, and CLI anchors referenced by the slice
- Prior review reports: `issues/2026-07-19-compensation-aware-scenario-catalog/review/005-atomic-on-demand-multi-fault-persistence-review-01.md`, `issues/2026-07-19-compensation-aware-scenario-catalog/review/005-atomic-on-demand-multi-fault-persistence-review-02.md`
- Commands run by reviewer:
  - `mvn -Dtest=OnDemandFaultScenarioServiceSpec test` from repository root — failed because this repository has no root POM; rerun from `verifiers/`
  - `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec test`
  - `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec,ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec test`
  - `cd verifiers && mvn -Dtest=RecoveryScheduleGeneratorSpec test`
  - `git diff --check`
  - worktree, dependency, source-reference, attempt-number, and done-path collision inspection
  - isolated CLI package-integrity probe under `/tmp/s5-review-attempt3-cross-vector-count`: changed an existing eager vector's exact uncapped count to `999`, updated the exact aggregate, accounting checksum, and manifest aggregate consistently, then requested a different new vector

## Summary

The attempt-02 package-boundary fixes work: linked paths are rejected before linked-file parsing, and manifest aliases in one canonical package directory share the mutation guard. The normal service path is separate from execution, generates through S3, updates canonical FaultScenario/accounting records, validates staged and final packages, publishes the manifest last, restores injected promotion failures, and returns structured results. Fresh reviewer runs pass the 33-test focused suite, the 110-test S5/S4 regression suite, and the 14-test recovery suite.

The slice still cannot pass because pre-mutation and staged/final validation do not establish deterministic exactness for every computed vector carried into a new revision. Fresh generation is compared only for the vector currently requested, and only on its full-dedup path. A hash-consistent package whose existing eager vector `1000` falsely claimed `uncappedUniqueScheduleCount=999` was accepted when the reviewer requested different vector `1001`; the service returned `PERSISTED`, changed all three mutable artifacts, and retained the false `999` row in the newly validated revision. This violates the existing-package validation and consistent-revision contract.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | fail | Normal generation and publication work, but `readValidatedPackage` accepts internally sum/hash-consistent false per-vector exact metadata and carries it into a successful revision when another vector is requested. |
| Slice out-of-scope respected | pass | No batch/search policy, scenario prioritization, execution, WorkloadPlan mutation, cap mutation, or runtime materialization/startup claim was introduced. |
| Spec non-goals respected | pass | No eager `2^n` enumeration, exact all-vector recovery total, search/scoring, compensation faults, distributed/TCC parity, or new dependency was added. |
| Dependencies done | pass | S4 is under `done/`; its latest review is attempt 02 with verdict `PASS`. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-15 | pass | Valid multi-fault `0011` generation persists scenarios with realized faults and compensation queues for both still-live participants; the S3 recovery suite remains green. | Runtime replay remains later-slice scope. |
| AC-16 | pass | The package cap is positive/frozen, and asserted zero, negative, or malformed caps reject before staging. | |
| AC-19 | pass | The manifest cap remains authoritative; a mismatched asserted cap rejects before generation/mutation, including after deduplication is possible. | |
| AC-20 | fail | A normal valid request persists before returning, but the isolated valid request also published a supposedly consistent revision that retained a false exact count for another computed vector. | A successful package revision must be semantically consistent as a whole, not only for the newly requested vector. |
| AC-21 | fail | Listed request/path/readiness cases reject without mutation, but a hash-consistent package with false existing per-vector exact metadata was accepted and mutated. | Existing package validation is part of the pre-mutation boundary. |
| AC-22 | pass | Repeating the same requested vector compares its fresh exact counts and complete generated id set; byte-equivalent scenarios deduplicate, while direct requested-vector mismatches/collisions remain non-mutating integrity failures. | The blocking finding concerns a different carried-forward vector, not the direct repeat path. |
| AC-42 | pass | The request path does not invoke `ScenarioExecutor` and normal success changes only FaultScenario JSONL, accounting, and manifest. | Full execution immutability remains S6/S7 scope. |
| AC-44 | fail | The focused suite meaningfully covers success, invalid input, path boundaries, direct dedup mismatch, collision, rollback, fallback, ordering, concurrency, and CLI behavior, but has no cross-vector package-integrity case. | The reviewer probe demonstrates the missing case. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| Focused on-demand mutation/CLI spec | pass | Fresh reviewer run: `Tests run: 33, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| On-demand plus S4 regression suite | pass | Fresh reviewer run: `Tests run: 110, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| Recovery generator regression | pass | Fresh reviewer run: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| Successful staged/final v3 package validation | pass | Normal service tests reload the published package; injected staging/promotion failures retain or restore original mutable bytes. |
| Attempt-02 path-before-read and package-alias serialization regressions | pass | Both cases are present in the 33-test focused suite and pass; source inspection confirms manifest-only boundary resolution precedes `ScenarioCatalogPackageReader.read`, and the lock key is the canonical real package directory. |
| Package-wide deterministic computed-vector integrity | fail | In `/tmp/s5-review-attempt3-cross-vector-count`, eager vector `1000` was changed to exact uncapped count `999` with matching accounting sum, checksum, and manifest sum. A request for distinct vector `1001` returned exit `0` / `PERSISTED`; all mutable hashes changed, and the revised accounting still contained `1000 ... uncappedUniqueScheduleCount=999`. |
| `git diff --check` | pass | No whitespace errors. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Changes stay within request contracts/CLI/service, package hashes/source/count metadata, accounting extension, shared materializability access, and focused tests. |
| Existing patterns | pass | Uses Jackson, immutable records, S3 deterministic generation, S4 readers/validators, exact decimal counts, NIO same-directory staging, stable sorting, and Spock fixtures without a new dependency. |
| Test quality | fail | Direct requested-vector reconciliation is meaningful, but it does not prove that unrelated existing computed rows remain deterministic before they are carried into a new revision. |
| Regression risk | fail | Any hash-consistent false exact count or non-deterministic retained scenario set for vector A can be republished as part of a successful request for vector B. |
| Security/data safety | fail | Package path handling is now safe, but package data integrity remains incomplete because a successful mutation can bless and retain false exact vector metadata. |
| Change hygiene | pass | `git diff --check` passes; unrelated untracked meeting-note files were not modified. |

## Findings

| Severity | Finding | Evidence | Required Fix |
|----------|---------|----------|--------------|
| blocking | Package validation reconciles deterministic counts/id sets only for the currently requested dedup vector, so false exact metadata for any other computed vector is accepted and republished in a successful revision. | `OnDemandFaultScenarioService.java:235-245` performs fresh count/id comparison only when `additions.isEmpty()` for the requested vector. `validateConsistency` at lines `678-684` checks only non-negativity, cap bounds, persisted cardinality, and aggregate consistency; it never compares each row with `RecoveryScheduleGenerator` under the frozen cap. The isolated probe changed existing eager vector `1000` to uncapped count `999`, updated all dependent sums/hash metadata, then requested `1001`; the CLI returned `PERSISTED`, and the final accounting retained `999`. | Before any mutation, deterministically reconcile every computed-vector row that will be carried into the revision against its WorkloadPlan and frozen cap, including exact uncapped/written counts and the complete persisted FaultScenario-id set. Reject any mismatch with structured diagnostics and byte-unchanged mutable artifacts. Add a regression that tampers vector A consistently, requests distinct vector B, and proves rejection plus byte identity; cover both eager and retained on-demand rows if their validation paths differ. |

## Done Transition

- Moved to done: `no`
- Done path: `None`
- Reason if not moved: `FAIL` — package-wide deterministic computed-vector validation remains incomplete.

## Reviewer Notes

The two attempt-02 blockers are resolved. The new finding was reproduced independently against a different vector than the request, so the existing direct-repeat mismatch tests do not cover it. Unrelated worktree changes were preserved.
