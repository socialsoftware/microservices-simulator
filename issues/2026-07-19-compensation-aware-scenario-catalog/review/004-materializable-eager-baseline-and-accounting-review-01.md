# Slice Review: 004 - Materializable Eager Baseline and Accounting

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`FAIL`

Moved to done: `no`

## Sources Reviewed

- Slice: `issues/2026-07-19-compensation-aware-scenario-catalog/004-materializable-eager-baseline-and-accounting.md`
- Spec: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Implementation plan: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`
- ADRs: `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`
- Completion evidence: `004-materializable-eager-baseline-and-accounting.md` → `## Completion Evidence`
- Dependency evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/done/002-deterministic-v3-workload-package.md`, `issues/2026-07-19-compensation-aware-scenario-catalog/done/003-deterministic-recovery-schedules.md`, and their latest PASS reviews
- Changed files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/ScenarioGeneratorApplication.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/{EagerFaultScenarioGenerator,FaultScenarioValidator,RecoveryScheduleCap}.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/{ComputedVectorRecovery,EagerFaultScenarioGenerationResult,FaultScenarioVectorSource,ScenarioCatalogManifest,WorkloadMaterializability}.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/accounting/ScenarioSpaceAccountingReport.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/{ScenarioCatalogJsonlWriter,ScenarioCatalogPackageReader}.java`; `verifiers/src/main/resources/{application.yaml,application-test.yaml}`; and all four changed Spock specs listed in the slice evidence
- Prior review reports: `None`
- Commands run by reviewer:
  - `cd verifiers && mvn -Dtest=ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec test`
  - `cd verifiers && mvn -Dtest=RecoveryScheduleGeneratorSpec test`
  - `git diff --check`
  - worktree/diff, dependency, review-attempt, and done-path inspection
  - `jq`, line-count, artifact-listing, and SHA-256 inspection of `/tmp/s4-package-evidence/dummyapp-20260720-040045-198/`
  - JShell package-boundary probe that replaced required eager vector `01` with valid multi-fault vector `11` and invoked `ScenarioCatalogJsonlWriter.write(...)`

## Summary

The normal application path correctly evaluates input readiness plus structural admissibility, retains ineligible WorkloadPlans, generates the all-zero and single-point baseline through the S3 generator, freezes the independent cap, and writes deterministic catalogs with exact scoped accounting. The required 75-test suite and the 14-test recovery regression suite pass.

The slice cannot pass because its pre-publication package validation does not actually enforce the exact eager vector set it claims to validate. It checks only the number of computed vectors per materializable workload. A package that omits a required single-point vector and substitutes a valid multi-fault vector passes validation and is published. That violates the S4 all-zero/single-point package invariant and makes the completion-evidence claim of vector-coverage validation inaccurate.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | fail | `ScenarioCatalogJsonlWriter.validateGenerationResult` checks `vectorCount == faultSlots + 1` but not the exact expected vectors or source classification; the reviewer probe published `{00,10,11}` for a two-slot eager package instead of required `{00,10,01}`. |
| Slice out-of-scope respected | pass | No on-demand mutation, runtime execution, eager multi-fault enumeration, or generic materialization expansion was introduced. The probe uses S3 multi-fault generation only to test rejection at the S4 eager-package boundary. |
| Spec non-goals respected | pass | No eager `2^n` enumeration, exact all-vector recovery total, distributed/TCC parity, search/scoring, or new dependency was added. |
| Dependencies done | pass | S2 and S3 are under `done/` with latest PASS reviews. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-1 | pass | Application and writer tests plus manual artifact inspection show separate populated WorkloadPlan/FaultScenario catalogs, manifest, and accounting, with no v2 catalog. | |
| AC-5 | pass | Fixed-timestamp writer and dummyapp rerun tests compare ids and bytes; required suite passes. | |
| AC-6 | pass | Manifest contains both artifact links/schemas/counts, effective workload config, cap, materializability policy/diagnostics, vector source, source-mode diagnostics, and computed sums. | |
| AC-9 | pass | Normal eager generation produces exactly one forward-only all-zero scenario for each eligible workload; application evidence has six all-zero scenarios for six eligible plans. Runtime commit remains S6-owned. | Pre-publication exact-set validation still needs the finding below fixed. |
| AC-10 | fail | `EagerFaultScenarioGenerator` itself emits every single point, but the package publisher accepts and writes a result missing `01` and containing `11` for a two-slot workload because it validates only vector cardinality. | The persisted S4 package invariant is not enforced. |
| AC-16 | pass | Default `20`, custom positive `3`, and zero/negative/malformed early failures are covered; malformed values fail before the configured output root exists. | |
| AC-19 | pass | S4 coverage proves the frozen cap and independent per-vector application. On-demand cap matching remains S5-owned. | |
| AC-23 | pass | Accounting contains exact decimal `n`, `2^n` including `2^70`, eager-vector counts, and exact per-computed-vector uncapped/written rows and sums. | |
| AC-24 | pass | Aggregate labels are scoped to computed vectors and `allVectorRecoveryTotalStatus` is `NOT_COMPUTED`; no exact all-vector total is emitted. | |
| AC-44 | fail | Dummyapp-first normal-path coverage is substantial, but the package-integrity test checks only a missing workload reference and does not prove the exact eager vector set survives the publication boundary. | S4-owned portion only. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| Required S4 Maven suite | pass | Fresh reviewer run: `Tests run: 75, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| Existing recovery semantics | pass | Fresh reviewer run: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| Fixed-config/fixed-timestamp catalog stability | pass | Automated tests pass; inspected package has stable deterministic ids/order and no duplicate FaultScenario ids. |
| Manual package listing/count/accounting inspection | pass | `/tmp/s4-package-evidence/dummyapp-20260720-040045-198/` contains both catalogs, manifest, accounting, diagnostics, and HTML; counts are 8 workloads, 6 eligible, 14 eager vectors, and 14 FaultScenarios. |
| Pre-publication exact eager vector coverage | fail | JShell constructed a two-slot package with computed/persisted vectors `{00,10,11}`. `ScenarioCatalogJsonlWriter.write(...)` succeeded and `/tmp/s4-review-invalid-eager/fault-scenario-catalog.jsonl` contains vector `11`. |
| `git diff --check` | pass | No whitespace errors. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Changes stay within eager generation, package/accounting integration, configuration, and targeted tests. |
| Existing patterns | pass | Uses the shared readiness evaluator/structural validator, S3 recovery generator, immutable records, Jackson, `BigInteger`, stable sorting, and Spock fixtures. |
| Test quality | fail | Normal-path tests are meaningful, but the only invalid eager-package test covers a missing workload reference. It does not challenge the exact vector/source set despite the new attempted vector-coverage validator. |
| Regression risk | fail | The writer is the package publication boundary and currently permits a semantically non-eager vector set while reporting `EAGER_ALL_ZERO_AND_SINGLE_POINT` in the manifest. |
| Security/data safety | n/a | No security-sensitive or destructive data operation is introduced in this slice. |
| Change hygiene | pass | `git diff --check` passes; unrelated untracked meeting-note files were not modified. |

## Findings

| Severity | Finding | Evidence | Required Fix |
|----------|---------|----------|--------------|
| blocking | Pre-publication validation treats eager vector cardinality as vector coverage, so a required single-point vector can be replaced by an arbitrary valid vector and still be published under the `EAGER_ALL_ZERO_AND_SINGLE_POINT` manifest policy. | `ScenarioCatalogJsonlWriter.java:214-255` records unique keys and checks only `vectorCount == faultSlots + 1`. Reviewer probe: valid generator set `[00, 10, 01]`; replacing `01` with S3-generated `11` still made `write(...)` succeed, and `/tmp/s4-review-invalid-eager/fault-scenario-catalog.jsonl` contains `11`. | Derive the exact expected eager vector/source map per eligible WorkloadPlan (one all-zero as `EAGER_ALL_ZERO`, every one-hot vector as `EAGER_SINGLE_POINT`, none for ineligible plans), require the computed rows to match it exactly, and enforce the all-zero row's exact `1/1` count. Add a writer-boundary regression that substitutes a valid multi-fault vector for a required single point and proves rejection occurs before any artifact/directory is created. |

## Done Transition

- Moved to done: `no`
- Done path: `None`
- Reason if not moved: `FAIL` — exact eager vector coverage is not enforced at the package publication boundary.

## Reviewer Notes

The normal `EagerFaultScenarioGenerator` output is correct; the defect is the integrated result/writer invariant explicitly introduced and claimed by this slice. Fixing it does not require implementing S5 on-demand persistence or S6 execution validation.
