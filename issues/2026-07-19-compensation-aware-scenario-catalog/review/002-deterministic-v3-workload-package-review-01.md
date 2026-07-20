# Slice Review: 002 - Deterministic V3 Workload Package

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`FAIL`

Moved to done: `no`

## Sources Reviewed

- Slice: `issues/2026-07-19-compensation-aware-scenario-catalog/002-deterministic-v3-workload-package.md`
- Spec: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Implementation plan: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`
- ADRs: `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`, `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`, `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md`, `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`
- Completion evidence: `002-deterministic-v3-workload-package.md` → `## Completion Evidence`
- Dependency evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/done/001-compensation-evidence-preservation.md`, `issues/2026-07-19-compensation-aware-scenario-catalog/review/001-compensation-evidence-preservation-review.md`
- Changed files reviewed: all S2 production changes under `verifiers/src/main/java/.../faults/{scenario,dynamic,executor}/`, `ScenarioGeneratorApplication.java`, `application.yaml`, `application-test.yaml`, and the changed S2 Spock coverage under `verifiers/src/test/groovy/.../faults/{scenario,dynamic,executor}/`
- Prior review reports: `None for slice 002`
- Commands run by reviewer:
  - `mvn -Dtest=ScenarioModelSpec,ScenarioGeneratorSpec,ScenarioCatalogJsonlWriterSpec,DynamicEvidenceJoinerSpec,EnrichedScenarioCatalogWriterSpec,DummyappDynamicEnrichmentIntegrationSpec,ScenarioGeneratorApplicationSpec test` from the repository root — failed immediately because this repository has no root POM; rerun from `verifiers/` below
  - `cd verifiers && mvn -Dtest=ScenarioModelSpec,ScenarioGeneratorSpec,ScenarioCatalogJsonlWriterSpec,DynamicEvidenceJoinerSpec,EnrichedScenarioCatalogWriterSpec,DummyappDynamicEnrichmentIntegrationSpec,ScenarioGeneratorApplicationSpec test`
  - `cd verifiers && mvn -Dtest=DynamicEnrichmentOrchestratorSpec,DynamicEvidenceReaderSpec,DynamicInputMapWriterSpec,DynamicEnrichmentTestDiscoverySpec,ScenarioExecutorSpec,ScenarioSpaceAccountingCalculatorSpec,DummyappAccountingFixtureFoundationSpec test`
  - `git diff --check`
  - production-reference inspection with `rg --pcre2` for v2 schemas, old artifact names, vector-overlay options, `LegacyScenarioPlan`, and `LegacyFaultSpace`
  - artifact listing/manifest/line-count inspection of `/tmp/v3-workload-package16670218804318020049/`
  - JShell semantic-mutation reproducer against `verifiers/target/classes` for `InputRecipe` → `WorkloadPlan` identity and `WorkloadPlanValidator`

## Summary

The v3 package shape, artifact names, reader boundary, deterministic fixed-timestamp output, WorkloadPlan-linked dynamic sidecar, and temporary executor bridge all pass their targeted suites and manual inspections. The slice still fails its central AC-3 identity-integrity contract: WorkloadPlan hashing trusts a serialized `InputRecipe.recipeFingerprint` without hashing the recipe's semantic structure, and the validator does not verify that fingerprint. A changed replay literal can therefore retain the same WorkloadPlan id and pass validation. The required field-by-field identity mutation coverage is also absent; the current model test mutates only top-level collections and does not catch this case.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | fail | Most scope is implemented, but canonical WorkloadPlan identity is not closed over accepted-input recipe semantics; see the blocking finding. |
| Slice out-of-scope respected | pass | `fault-scenario-catalog.jsonl` is intentionally empty, no recovery schedules/on-demand mutation were added, and executor changes are limited to the compile bridge. |
| Spec non-goals respected | pass | No v2 dual writing/upgrading, new dependency, search/scoring, distributed execution, or recovery enumeration was introduced. |
| Dependencies done | pass | Slice 001 is present at `done/001-compensation-evidence-preservation.md` with PASS review attempt 01. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-1 | pass | Writer/application tests and inspected `/tmp/v3-workload-package16670218804318020049/` show workload JSONL, empty FaultScenario JSONL, manifest, accounting, and no `scenario-catalog.jsonl`. | FaultScenario population remains correctly deferred. |
| AC-2 | pass | `ScenarioCatalogPackageReader` rejects v2 schemas; `ScenarioCatalogReader` rejects v3 package artifacts/manifests while remaining isolated as the approved temporary executor-only v2 bridge. | Production-reference inspection found the old surface only in the inventoried executor/script/Compose island. |
| AC-3 | fail | `ScenarioIdGenerator.java:229-230` hashes only recipe schema/fingerprint/readiness. JShell changed a recipe literal from `1` to `2` while retaining the serialized fingerprint; both WorkloadPlans had the same id and both validator results were `valid=true`. | Violates identity closure and permits semantically changed accepted input/replay data under an unchanged WorkloadPlan id. |
| AC-5 | pass | `ScenarioCatalogJsonlWriterSpec` compares all five package artifacts across fixed-timestamp reruns; the required 83-test suite passed. | Determinism for generated valid inputs is demonstrated, but it does not cure AC-3 integrity. |
| AC-6 | pass | Manifest inspection confirms v3 linked artifact schemas/paths/exact-string counts, effective generator config, generation source, materializability policy, warnings, and source-mode diagnostics. | Recovery-specific counts/cap remain owned by later slices. |
| AC-43 | pass | Sidecar record has only `schemaVersion`, `workloadPlanId`, `inputVariantIds`, and `dynamicEvidence`; tests prove exact/high-confidence WorkloadPlan/input joins and no embedded WorkloadPlan/FaultScenario schedule. | Dynamic input-map and orchestrator support also passed. |
| AC-44 | fail | Dummyapp model/package/enrichment/application suites pass, but the slice-required field-by-field identity mutation matrix is not present and misses the accepted-input recipe counterexample. | The automated semantic coverage claim is incomplete for this high-risk identity slice. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| Required S2 Maven suite | pass | `Tests run: 83, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| Dynamic input-map/orchestrator support | pass | Included in the supplemental run; all 23 relevant dynamic tests passed. |
| Executor-only bridge | pass | Included in the supplemental run; `ScenarioExecutorSpec`: 54 tests passed. |
| Accounting/dummyapp regression | pass | Included in the supplemental run; 40 tests passed across accounting and dummyapp foundation specs. |
| V3 run-directory artifact inspection | pass | One workload line, empty FaultScenario/rejected-input files, v3 manifest/accounting, and no v2 catalog. |
| Production v2-reference inspection | pass | Exact old references are confined to `CatalogScenarioRecord`, `ScenarioCatalogReader`, `ScenarioExecutor`, `ScenarioExecutorOrchestrator`, `verifiers/scripts/run-scenario-executor.sh`, and `docker-compose.yml`; no canonical/dynamic/reporting caller consumes the bridge. |
| Field-by-field identity mutation matrix | missing | `ScenarioModelSpec.groovy:21` mutates only six top-level collections. It does not mutate every nested semantic field or test stale recipe fingerprints. |
| Semantic recipe mutation reproducer | fail | Literal `1` versus `2` with the same supplied `recipeFingerprint` produced `same id=true`; `WorkloadPlanValidator` returned `valid=true` for both. |
| `git diff --check` | pass | No whitespace errors. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Changes are broad because the canonical record migration is broad; no unrelated production refactor or dependency was added. |
| Existing patterns | pass | Uses existing Jackson, SHA-256, BigInteger/string accounting, run-scoped paths, and scenario→dynamic pipeline boundaries. |
| Test quality | fail | Package/dynamic tests are meaningful, but the explicitly required field-by-field identity matrix is incomplete and misses a real integrity defect. |
| Regression risk | fail | Stable-id deduplication and future persistence can conflate accepted inputs with different replay recipes if a stale/supplied fingerprint enters a package. |
| Security/data safety | fail | This is a catalog integrity issue: the v3 reader accepts semantically modified replay input under the unchanged canonical WorkloadPlan id. |
| Change hygiene | pass | `git diff --check` passes; unrelated untracked meeting-note files were left untouched. |

## Findings

| Severity | Finding | Evidence | Required Fix |
|----------|---------|----------|--------------|
| blocking | WorkloadPlan identity is not closed over accepted-input recipe semantics, and package validation trusts stale recipe fingerprints. | `ScenarioIdGenerator.java:229-230` hashes `inputRecipe.schemaVersion`, the supplied `recipeFingerprint`, and `executorReady`, but not canonical recipe arguments/nodes. `InputRecipe.java:18-20` accepts any nonblank supplied fingerprint. `WorkloadPlanValidator.java:88-100` checks only input-id presence/duplication and never verifies recipe fingerprint integrity. Reviewer JShell evidence: changing a literal from `1` to `2` with the same fingerprint yielded the same WorkloadPlan id and both plans validated successfully. `ScenarioModelSpec.groovy:21-39` has only a top-level collection matrix. | Close the identity transitively by either hashing canonical `InputRecipe` semantic content directly or recomputing/validating its fingerprint before accepting a WorkloadPlan/package. Ensure the v3 reader rejects a stale fingerprint. Add the slice-required field-by-field mutation matrix, including accepted-input recipe literal/argument changes and a warning-only control; add a reader/validator regression proving stale fingerprints cannot pass. |

## Done Transition

- Moved to done: `no`
- Done path: `None`
- Reason if not moved: `FAIL — AC-3 identity closure and the required identity verification matrix remain incomplete.`

## Reviewer Notes

The completion evidence's Maven counts, artifact names, v2 rejection, dynamic linkage, and legacy-island inventory were independently reproduced. The failure is narrow but blocking because WorkloadPlan identity is the canonical key for all later FaultScenario generation, sidecars, deduplication, persistence, and replay.
