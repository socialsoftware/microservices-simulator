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
