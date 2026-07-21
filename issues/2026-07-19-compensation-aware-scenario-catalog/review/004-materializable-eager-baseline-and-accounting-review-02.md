# Slice Review: 004 - Materializable Eager Baseline and Accounting

## Review Attempt

Attempt: `02`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes`

## Sources Reviewed

- Slice: `issues/2026-07-19-compensation-aware-scenario-catalog/004-materializable-eager-baseline-and-accounting.md`
- Spec: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Implementation plan: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`
- ADRs: `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`
- Completion evidence: `004-materializable-eager-baseline-and-accounting.md` → `## Completion Evidence`, including `### Review Attempt 01 Fix Evidence`
- Dependency evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/done/002-deterministic-v3-workload-package.md`, `issues/2026-07-19-compensation-aware-scenario-catalog/done/003-deterministic-recovery-schedules.md`, and their latest PASS reviews
- Changed files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/ScenarioGeneratorApplication.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/{EagerFaultScenarioGenerator,FaultScenarioValidator,RecoveryScheduleCap}.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/{ComputedVectorRecovery,EagerFaultScenarioGenerationResult,FaultScenarioVectorSource,ScenarioCatalogManifest,WorkloadMaterializability}.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/accounting/ScenarioSpaceAccountingReport.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/{ScenarioCatalogJsonlWriter,ScenarioCatalogPackageReader}.java`; `verifiers/src/main/resources/{application.yaml,application-test.yaml}`; and the four changed Spock specs listed in the slice evidence
- Prior review reports: `issues/2026-07-19-compensation-aware-scenario-catalog/review/004-materializable-eager-baseline-and-accounting-review-01.md`
- Commands run by reviewer:
  - `mvn -Dtest=ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec test` from the repository root — failed because this repository has no root POM; rerun from `verifiers/`
  - `cd verifiers && mvn -Dtest=ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec test`
  - `cd verifiers && mvn -Dtest=RecoveryScheduleGeneratorSpec test`
  - `git diff --check`
  - dependency, review-attempt, worktree, changed-path, and done-path collision inspection
  - `find`, `wc`, and `jq` inspection of `/tmp/s4-package-evidence/dummyapp-20260720-040045-198/`

## Summary

The whole S4 implementation now satisfies the bounded eager-baseline and layered-accounting contract. Eligibility combines current input readiness with the shared structural validator while retaining every WorkloadPlan and recording ineligible diagnostics. Each eligible plan uses the S3 generator for exactly one all-zero vector and every one-hot vector under the independent frozen recovery cap. The application writes both catalogs, the manifest, and exact computed-vector accounting with deterministic ordering; count-only mode makes no eager-record claim.

The attempt-01 publication-boundary defect is fixed. The writer now derives the exact expected vector/source map for every eligible workload, requires computed rows to equal it, enforces all-zero `1/1` accounting, and checks computed written counts against validated FaultScenario records before creating output directories. The regression that replaces required vector `01` with valid multi-fault vector `11` passes by observing pre-mutation rejection. Fresh reviewer runs pass all 77 required tests and all 14 recovery-generator regression tests.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | pass | `EagerFaultScenarioGenerator` performs combined eligibility and delegates all-zero/one-hot vectors to `RecoveryScheduleGenerator`; writer/application integration publishes catalogs, manifest, diagnostics, and exact scoped accounting. Exact eager vector/source coverage is now validated before publication. |
| Slice out-of-scope respected | pass | No on-demand package mutation, runtime execution, eager multi-fault enumeration, or generic materialization expansion was introduced. Multi-fault generation appears only in the writer regression proving that such a substitution is rejected from the eager package. |
| Spec non-goals respected | pass | No eager `2^n` vector enumeration, exact all-vector recovery total, v2 compatibility path, search/scoring, distributed/TCC parity, or new dependency was added. |
| Dependencies done | pass | S2 and S3 are under `done/` and their latest reviews are `PASS`. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-1 | pass | Application tests and inspected output contain separate populated WorkloadPlan/FaultScenario JSONL catalogs, manifest, accounting, and rejected-input diagnostics; no v2 `scenario-catalog.jsonl` exists. | |
| AC-5 | pass | Fixed-configuration/fixed-timestamp writer and real dummyapp tests compare ids and catalog bytes across reruns; the required suite passes. | |
| AC-6 | pass | The manifest links both catalog schemas/paths/counts, accounting and diagnostics, effective workload configuration, cap, materializability policy/diagnostics, eager vector source, source-mode diagnostics, and computed uncapped/written sums. | |
| AC-9 | pass | Generation and writer tests prove exactly one all-zero record per eligible workload, with all forward actions and no compensation. The inspected package has six all-zero scenarios for six eligible plans and zero all-zero compensation actions. | Automatic runtime commit remains correctly deferred to S6. |
| AC-10 | pass | The generator emits every one-hot vector. The publication boundary now requires the exact all-zero/one-hot vector/source map and rejects replacing required `01` with valid multi-fault `11` before creating artifacts. | Pre-body fault/checkpoint semantics remain supplied by the already-passed S3 generator. |
| AC-16 | pass | Recovery cap defaults/resources use `20`; positive custom values are accepted; blank/malformed/non-positive parsing is rejected, and application tests prove `0`, `-1`, and malformed text fail before the output root exists. | |
| AC-19 | pass | The cap is passed independently to every eager vector, focused coverage proves one vector is capped without affecting others, and the effective cap is frozen in the manifest. | On-demand cap matching remains S5-owned. |
| AC-23 | pass | Accounting emits exact decimal `n`, `2^n` (including `2^70`), exact eager-vector counts, per-computed-vector uncapped/written rows, and exact sums over those rows. Writer validation ties written rows to FaultScenario record counts. | |
| AC-24 | pass | Exact aggregate labels are limited to `EXACT_SUM_OVER_COMPUTED_VECTORS_ONLY`; `allVectorRecoveryTotalStatus` is `NOT_COMPUTED`, with no exact all-vector recovery field. | |
| AC-44 | pass | S4's dummyapp-first coverage proves the real parser eligibility split, eager counts, deterministic ids/catalog bytes, cap validation, package metadata, exact accounting, and the exact eager-vector publication invariant. | Full feature-wide AC-44 remains distributed across later slices. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| Required S4 Maven suite | pass | Fresh reviewer run: `Tests run: 77, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| Existing recovery semantics | pass | Fresh reviewer run: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| Attempt-01 exact-vector regression | pass | Included in `ScenarioCatalogJsonlWriterSpec`: valid multi-fault `11` cannot replace required one-hot `01`, and no output path is created. The all-zero `1/1` tamper regression also passes. |
| Fixed-config/fixed-timestamp stability | pass | Automated writer and dummyapp tests pass; completion evidence records identical workload and FaultScenario catalog SHA-256 values across paired runs. |
| Manual package inspection | pass | `/tmp/s4-package-evidence/dummyapp-20260720-040045-198/` has 8 workloads, 6 materializable and 2 ineligible plans, 14 eager vectors/FaultScenarios, cap `3`, complete linked artifact metadata, exact computed-only accounting, six all-zero records, and zero all-zero compensation actions. |
| Count-only honesty | pass | Application coverage writes empty workload/FaultScenario catalogs in `COUNT_ONLY`, retains non-zero mathematical workload accounting, and reports zero eager vectors/FaultScenarios. |
| `git diff --check` | pass | No whitespace errors. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Changes remain limited to eager generation, eligibility diagnostics, cap configuration, package validation/export/reading, accounting, and targeted tests. |
| Existing patterns | pass | Uses the shared readiness evaluator and WorkloadPlan validator, S3 recovery generator, immutable records, Jackson, `BigInteger`, stable sorting, run-relative output handling, and Spock fixtures. |
| Test quality | pass | Tests cover normal semantics, blocked and structurally malformed workloads, independent cap behavior, large exact arithmetic, deterministic bytes, application packaging, count-only behavior, and adversarial pre-publication vector/count tampering. |
| Regression risk | pass | Required and recovery suites pass; the writer validates WorkloadPlan/FaultScenario identities/references, exact eager vector/source coverage, and count-to-record consistency before output creation. |
| Security/data safety | pass | Invalid recovery-cap configuration and invalid generation results fail before output path mutation; existing application path-containment checks remain intact. |
| Change hygiene | pass | `git diff --check` passes; unrelated untracked meeting-note files were not modified. |

## Findings

None.

## Done Transition

- Moved to done: `yes`
- Done path: `issues/2026-07-19-compensation-aware-scenario-catalog/done/004-materializable-eager-baseline-and-accounting.md`
- Reason if not moved: `n/a`

## Reviewer Notes

Runtime materialization/startup and automatic commit are correctly not claimed by S4. Atomic on-demand mutation remains S5, and full persisted-action execution validation remains S6.
