# 004 - Materializable Eager Baseline and Accounting

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `002-deterministic-v3-workload-package.md`, `003-deterministic-recovery-schedules.md`  
ACs covered: `AC-1, AC-5, AC-6, AC-9, AC-10, AC-16, AC-19, AC-23, AC-24, AC-44`  
Risk: `high`

## Purpose

Turn v3 workload generation into the bounded executable baseline: exactly one all-zero plus every single-point vector for each input-ready, structurally admissible WorkloadPlan, with a frozen recovery cap and defensible layered accounting.

## Scope

- Combine `ScenarioExecutorReadinessEvaluator` input semantics with the shared pure structural validator to decide eager WorkloadPlan eligibility.
- Keep runtime materialization and startup explicitly unproven and deferred to execution.
- For every eligible WorkloadPlan, generate exactly one all-zero FaultScenario and all bounded unique schedules for every single-point vector.
- Exclude non-materializable WorkloadPlans from eager FaultScenario records while retaining them in the WorkloadPlan catalog and diagnostics/accounting.
- Add a separate positive `recoveryScheduleCap` configuration, default `20`, validated before any output path is mutated; do not reuse the forward `maxSchedulesPerInputTuple` meaning.
- Freeze the effective recovery cap and materializability policy in the manifest.
- Populate layered accounting with exact WorkloadPlan counts; exact per-plan `n` and `2^n`; exact eager vector count; and exact uncapped/written counts for all-zero and every single-point vector.
- Clearly label only computed sums as exact and make no exact aggregate claim over ungenerated vectors.
- Write both catalogs, accounting, and linking manifest with stable ordering and complete artifact/count metadata through the application path.

## Out of Scope

- On-demand arbitrary vectors and package append/mutation.
- Runtime execution.
- Eager multi-fault enumeration or generic materialization expansion.

## Repo Anchors

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorReadinessEvaluator.java` — current input-level readiness semantics.
- Structural validation introduced in S2 — generation-time admissibility shared with later execution.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/accounting/ScenarioSpaceAccountingCalculator.java` and `ScenarioSpaceAccountingReport.java` — exact-number/report patterns.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogJsonlWriter.java` and `ScenarioGeneratorApplication.java` — integrated package output.
- `verifiers/src/main/resources/application.yaml`, `application-test.yaml` — defaults/config validation.
- `verifiers/src/test/groovy/.../scenario/DummyappAccountingFixtureFoundationSpec.groovy` and `.../ScenarioGeneratorApplicationSpec.groovy` — real-pipeline integration.

## Implementation Shape

- Produce one immutable generation result that contains WorkloadPlans, eager FaultScenarios, rejections/diagnostics, and exact accounting inputs before writing artifacts.
- Ensure all-zero uniqueness is independent of recovery cap and has no compensation actions.
- Invoke S3’s same per-vector generator for every single-point vector; do not fork eager recovery semantics.
- Use `BigInteger` internally and schema-safe decimal serialization for `2^n` and sums.
- Keep count-only forward-workload behavior honest: it may account for workload shape without claiming eager FaultScenario records for unmaterialized WorkloadPlans.
- Validate complete package referential integrity before writer publication.

## TDD / Test Shape

- First behavior to test: a materializable two-slot WorkloadPlan emits one all-zero vector plus both single-point vectors, while an input-blocked or structurally malformed WorkloadPlan emits no eager FaultScenario.
- Expected red failure: S2 writes no eager FaultScenarios and current accounting has no vector/recovery layers.
- Additional coverage: all-zero no compensation; independent per-vector cap; cap default/positive validation; malformed cap before file creation; exact `2^n` beyond primitive ranges; exact per-vector uncapped/written rows; aggregate labels; no all-vector recovery exact claim; stable package bytes/order; manifest artifact/config/source-mode/count fields; dummyapp real-parser baseline.

## Just-in-Time Preflight Required

Before source edits, the executor must re-check:

- S2 structural validator and S3 generation result APIs.
- Current Spring property binding behavior for malformed integer values versus explicit semantic validation.
- Existing count-only expectations and tests that assumed one v2 catalog.
- Dummyapp input-readiness distribution after S1/S2 migration.

## Verification

- `cd verifiers && mvn -Dtest=ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec test` — eager eligibility, cap, package, and accounting pass.
- Generate the same dummyapp fixture twice with a fixed timestamp/config and compare both catalog bytes and ids.

## Evidence to Record

- files changed
- commands run and outputs
- eligible/ineligible WorkloadPlan and eager vector counts
- exact accounting sample including `n`, `2^n`, uncapped, and written counts
- package artifact listing
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- Input-level readiness alone is not structural admissibility; require both without trying runtime construction.
- Do not drop ineligible WorkloadPlans from the workload catalog or mathematical vector-space accounting.
- Validate the recovery cap before creating/truncating any artifact.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Added one immutable eager-generation result that retains every generated WorkloadPlan, records input-readiness plus structural-admissibility diagnostics per plan, contains all eager FaultScenarios, freezes the recovery cap, and carries exact per-vector recovery-count inputs.
- Added deterministic eager generation for exactly one all-zero vector and every single-point vector on each materializable WorkloadPlan. It delegates every vector to `RecoveryScheduleGenerator`, emits no eager records for blocked/structurally inadmissible plans, and keeps runtime materialization/startup explicitly unproven.
- Added a separate `recovery-schedule-cap` property with default `20`. Explicit parsing rejects blank, malformed, zero, and negative values during application construction, before the run output directory or artifacts are created; it does not reuse `max-schedules-per-input-tuple`.
- Populated the FaultScenario JSONL artifact, manifest, and accounting through the application path. Accounting now includes per-workload exact `n`/`2^n`, materializable/ineligible counts and diagnostics, exact eager-vector counts, exact per-computed-vector uncapped/written rows, exact sums scoped only to computed vectors, and `allVectorRecoveryTotalStatus=NOT_COMPUTED`.
- Added pre-publication package validation for WorkloadPlan/FaultScenario identities, references, action sources/ownership, vector coverage, materializability diagnostics, and count-to-record consistency. The package reader now deserializes and validates typed FaultScenario records.

### Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/EagerFaultScenarioGenerator.java` — materializability evaluation and bounded all-zero/single-point generation.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/FaultScenarioValidator.java` — package-boundary FaultScenario identity/reference/action validation.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/EagerFaultScenarioGenerationResult.java` — immutable integrated generation result.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/{WorkloadMaterializability,ComputedVectorRecovery,FaultScenarioVectorSource}.java` — diagnostics and exact vector-generation inputs.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/accounting/ScenarioSpaceAccountingReport.java` — layered workload/vector/recovery accounting and exact-sum scope labels.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/{ScenarioCatalogJsonlWriter,ScenarioCatalogPackageReader}.java` — populated stable FaultScenario export, pre-publication validation, manifest/accounting integration, and typed reader validation.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/ScenarioCatalogManifest.java` — frozen cap, vector source, and per-workload materializability diagnostics.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/RecoveryScheduleCap.java` — clear malformed-value failure without leaking numeric parser text as the root diagnostic.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/ScenarioGeneratorApplication.java` — early cap validation and integrated eager generation before package writing.
- `verifiers/src/main/resources/{application.yaml,application-test.yaml}` — independent recovery-cap default `20`.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/accounting/ScenarioSpaceAccountingCalculatorSpec.groovy` — eligibility, malformed/blocked exclusion, independent cap, exact `2^70`, and aggregate-scope coverage.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogJsonlWriterSpec.groovy` — populated package, stable bytes, manifest/accounting, typed reader, and pre-mutation integrity coverage.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/ScenarioGeneratorApplicationSpec.groovy` — early cap rejection, complete dummyapp package, and honest count-only behavior.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/DummyappAccountingFixtureFoundationSpec.groovy` — real-parser eligibility/count baseline and repeated fixed-timestamp catalog-byte comparison.
- `issues/2026-07-19-compensation-aware-scenario-catalog/004-materializable-eager-baseline-and-accounting.md` — completion evidence only.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec test` before implementation | EXPECTED FAIL | Groovy compilation failed because `EagerFaultScenarioGenerator` did not exist, confirming the intended red baseline. |
| Same required command after implementation | PASS | Fresh final run: `Tests run: 75, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=RecoveryScheduleGeneratorSpec test` | PASS | `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`; existing pure recovery semantics remain green after cap integration. |
| Fixed-config/fixed-timestamp dummyapp generation twice in `DummyappAccountingFixtureFoundationSpec` | PASS | Workload ids and FaultScenario ids matched; workload-catalog SHA-256 was `d9c4ef1da77f182c68f63064c82839ab525090d9a839c17bb6f363937b721416` for both runs and fault-catalog SHA-256 was `e3038533bd3c356fe6a4836cec655861e10fb92c8b13fb51bea308fe4b8e7129` for both runs. |
| Manual dummyapp application generation with `BRUTE_FORCE`, `WRITE_WORKLOADS`, singles, max saga-set size `1`, and recovery cap `3` | PASS | `/tmp/s4-package-evidence/dummyapp-20260720-040045-198/` contains analysis HTML, both v3 catalogs, manifest, accounting, and rejected-input diagnostics; no v2 `scenario-catalog.jsonl`. Manifest/accounting report 8 workloads, 6 materializable, 2 ineligible, 14 eager vectors, and 14 FaultScenarios. |
| `git diff --check` | PASS | No whitespace errors. |

### Acceptance Criteria Evidence

- AC-1: Application and writer coverage prove separate populated WorkloadPlan/FaultScenario catalogs, linking manifest, and layered accounting; inspected output has no v2 catalog.
- AC-5: Fixed timestamp/config dummyapp runs produced identical WorkloadPlan/FaultScenario ids and byte-identical catalog hashes.
- AC-6: The manifest records linked artifact schemas/paths/counts, cap `3`, vector source `EAGER_ALL_ZERO_AND_SINGLE_POINT`, materializability policy with runtime materialization unproven, source-mode diagnostics, and exact computed counts.
- AC-9: The dummyapp application baseline has 6 materializable workloads and exactly 6 all-zero scenarios within the 14-record eager catalog; all inspected all-zero schedules contain forward actions only and no compensation actions.
- AC-10: The same baseline has 8 ordered fault slots across eligible plans and exactly 8 single-point vectors in addition to the 6 all-zero vectors. Focused tests prove blocked and structurally malformed plans emit none.
- AC-16: Defaults/resources use `20`; positive custom cap `3` is accepted and frozen; `0`, `-1`, and `not-an-integer` fail before the configured output root exists.
- AC-19: Eager generation passes the same cap independently to each vector. A focused fixture records all-zero `1/1` and vector `0100` uncapped/written `3/2` at cap `2`.
- AC-23: Accounting serializes exact decimal per-workload `n` and `2^n`; the 70-slot test records `2^70 = 1180591620717411303424`, 71 eager vectors, and exact per-vector/summed uncapped and written counts.
- AC-24: Only computed-vector sums are labeled exact with scope `EXACT_SUM_OVER_COMPUTED_VECTORS_ONLY`; `allVectorRecoveryTotalStatus` is `NOT_COMPUTED`, and no exact all-vector recovery field is emitted.
- AC-44: Dummyapp-first parser/application coverage proves the 8-workload application baseline, 6/2 eligibility split, 14 eager vectors/FaultScenarios, deterministic bytes/ids, cap validation, package metadata, and accounting semantics.

Exact inspected accounting sample:

```text
workload 05c09ae31f55...: n=2, 2^n=4, materializable=true, eagerVectors=3
vector 00: source=EAGER_ALL_ZERO, uncapped=1, written=1
vector 10: source=EAGER_SINGLE_POINT, uncapped=1, written=1
vector 01: source=EAGER_SINGLE_POINT, uncapped=1, written=1
computed-vector sums: uncapped=14, written=14, scope=EXACT_SUM_OVER_COMPUTED_VECTORS_ONLY
all-vector recovery total: NOT_COMPUTED
```

Package artifact listing:

```text
analysis-report.html
workload-catalog.jsonl
fault-scenario-catalog.jsonl
scenario-catalog-manifest.json
scenario-space-accounting.json
workload-catalog-rejected-inputs.jsonl
```

### Browser / Manual Evidence

- Browser evidence was not required. Manual artifact inspection is recorded above.

### TDD Notes

- Added the eager eligibility/vector/accounting/export tests first and observed the expected compile failure for the missing eager generator. Implemented the immutable result and eligibility/generation path, then drove focused accounting/writer tests, real dummyapp parser tests, and the application suite to green.

### Deviations From Plan

- None.

### Blockers / Follow-Ups

- None. Ready for `sp-review-slice`; S5 and S6 may now depend on this active slice after review passes.

### Review Attempt 01 Fix Evidence

Status: `implemented-awaiting-review`

The single blocking attempt-01 finding is fixed without expanding slice scope:

- `ScenarioCatalogJsonlWriter` now derives the exact expected eager vector/source map for every materializable WorkloadPlan: one all-zero vector labeled `EAGER_ALL_ZERO` and every ordered one-hot vector labeled `EAGER_SINGLE_POINT`; ineligible plans contribute no expected vectors.
- Pre-publication validation now requires computed vector/source rows to equal that exact map rather than checking cardinality only. A missing one-hot vector, substituted multi-fault vector, wrong source classification, extra vector, or vector on an ineligible plan is rejected before parent directories or artifacts are created.
- The all-zero computed row must also report exact uncapped/written counts `1/1` before publication.
- `ScenarioCatalogJsonlWriterSpec` now constructs the reviewer case for a two-slot plan by replacing required vector `01` with S3-generated valid multi-fault vector `11`; it proves rejection and verifies that no artifact path is created. A separate regression proves tampered all-zero accounting `2/1` is rejected before artifact creation.

#### Attempt 01 Fix Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogJsonlWriter.java`
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogJsonlWriterSpec.groovy`
- `issues/2026-07-19-compensation-aware-scenario-catalog/004-materializable-eager-baseline-and-accounting.md`

#### Attempt 01 TDD and Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=ScenarioCatalogJsonlWriterSpec test` before the fix | EXPECTED FAIL | `Tests run: 8, Failures: 1`; the writer published the `{00,10,11}` package and the new regression observed no exception. |
| Same focused command after exact vector/source and all-zero-count validation | PASS | `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`; both the multi-fault substitution and all-zero count tampering are rejected pre-mutation. |
| `cd verifiers && mvn -Dtest=ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec test` | PASS | Fresh required run: `Tests run: 77, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `git diff --check` | PASS | No whitespace errors. |

AC-10 and the S4 portion of AC-44 now include package-boundary proof that the persisted eager set is exactly all-zero plus every single-point vector with required source classifications. The completion-evidence claim of pre-publication vector-coverage validation is now accurate. No review file, spec/plan artifact, acceptance checkbox, dependent slice, on-demand behavior, or runtime execution behavior was edited.
