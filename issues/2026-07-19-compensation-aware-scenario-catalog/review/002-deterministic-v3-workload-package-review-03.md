# Slice Review: 002 - Deterministic V3 Workload Package

## Review Attempt

Attempt: `03`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes`

## Sources Reviewed

- Slice: `issues/2026-07-19-compensation-aware-scenario-catalog/002-deterministic-v3-workload-package.md`
- Spec: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Implementation plan: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`
- ADRs: `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`, `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`, `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md`, `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`
- Completion evidence: `002-deterministic-v3-workload-package.md` → `## Completion Evidence`, including the attempt-01 and attempt-02 fix evidence
- Dependency evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/done/001-compensation-evidence-preservation.md`, `issues/2026-07-19-compensation-aware-scenario-catalog/review/001-compensation-evidence-preservation-review.md`
- Changed files reviewed: all current S2 production changes under `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/{scenario,dynamic,executor}/`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/ScenarioGeneratorApplication.java`, `verifiers/src/main/resources/application.yaml`, `verifiers/src/main/resources/application-test.yaml`, and all changed S2 Spock coverage under `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/{scenario,dynamic,executor}/`
- Prior review reports: `issues/2026-07-19-compensation-aware-scenario-catalog/review/002-deterministic-v3-workload-package-review-01.md`, `issues/2026-07-19-compensation-aware-scenario-catalog/review/002-deterministic-v3-workload-package-review-02.md`
- Commands run by reviewer:
  - `mvn -Dtest=ScenarioModelSpec,ScenarioGeneratorSpec,ScenarioCatalogJsonlWriterSpec,DynamicEvidenceJoinerSpec,EnrichedScenarioCatalogWriterSpec,DummyappDynamicEnrichmentIntegrationSpec,ScenarioGeneratorApplicationSpec test` from the repository root — expected command-location failure because the repository has no root POM
  - `cd verifiers && mvn -Dtest=ScenarioModelSpec,ScenarioGeneratorSpec,ScenarioCatalogJsonlWriterSpec,DynamicEvidenceJoinerSpec,EnrichedScenarioCatalogWriterSpec,DummyappDynamicEnrichmentIntegrationSpec,ScenarioGeneratorApplicationSpec test`
  - `cd verifiers && mvn -Dtest=DynamicEnrichmentOrchestratorSpec,DynamicEvidenceReaderSpec,DynamicInputMapWriterSpec,DynamicEnrichmentTestDiscoverySpec,ScenarioExecutorSpec,ScenarioSpaceAccountingCalculatorSpec,DummyappAccountingFixtureFoundationSpec,InputRecipeMapperSpec,ApplicationAnalysisScenarioModelAdapterSpec test`
  - `git diff --check`
  - production-reference inspection with `rg` for canonical v2 types, schemas, artifact names, enriched wrappers, and the executor-only legacy island
  - artifact listing, JSONL line counts, and manifest-link inspection of `/tmp/v3-workload-package13892409169161715388/`

## Summary

The slice now satisfies the deterministic v3 workload-package contract. The canonical model records complete ordered WorkloadPlan structure; identity hashing is closed over accepted-input recipe semantics and all other semantic fields while excluding warnings; exact numeric reader configuration preserves high-precision decimal recipes across the writer/reader boundary. The writer emits the linked v3 workload/FaultScenario/manifest/accounting package with no v2 catalog, and the reader validates package links, WorkloadPlan structure and identity, record counts, schemas, stale recipe fingerprints, and dangling FaultScenario workload links. Dynamic enrichment is WorkloadPlan/input-linked sidecar evidence only. Remaining v2 behavior is confined to the approved temporary executor-only bridge and explicitly refuses v3 packages.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | pass | Current model/generator/id/validator/writer/reader/application code provides the v3 WorkloadPlan package and WorkloadPlan-linked enrichment described by S2; the required and supplemental suites pass. |
| Slice out-of-scope respected | pass | FaultScenario records remain empty for S3/S4, no recovery schedule/vector materialization or on-demand package mutation was added, and v3 persisted replay remains deferred to S6. |
| Spec non-goals respected | pass | No v2 dual writing/upgrading/fallback, recovery enumeration, execution-time v3 vector overlay, search/scoring, distributed parity, or new dependency was introduced. |
| Dependencies done | pass | S1 is present under `done/` with PASS review attempt 01. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-1 | pass | Fresh artifact inspection shows `workload-catalog.jsonl`, empty `fault-scenario-catalog.jsonl`, `scenario-catalog-manifest.json`, `scenario-space-accounting.json`, and the rejected-input diagnostic; `scenario-catalog.jsonl` is absent. | Empty FaultScenario output is explicitly allowed in S2. |
| AC-2 | pass | `ScenarioCatalogPackageReader` rejects v2 schemas clearly; the temporary `ScenarioCatalogReader` rejects v3 canonical artifact names, v3 manifest discovery, and v3 record schemas. Production inspection confines v2 behavior to the inventoried executor island. | Final bridge removal remains owned by S6. |
| AC-3 | pass | `ScenarioIdGenerator` hashes ordered participants, full accepted-input semantics via recomputed recursive recipe fingerprints, forward occurrences, unordered-normalized conflict evidence, ordered fault slots, and ordered checkpoints/evidence/phase footprints. `ScenarioModelSpec` has 85 passing identity/validator iterations, including stale-fingerprint and warning-only controls. | Attempt 01's identity-closure defect is resolved. |
| AC-5 | pass | The fixed-timestamp writer test compares all five package artifacts across reruns; generated WorkloadPlan ids match, and the required 161-test suite passes. | The high-precision package round trip also preserves the WorkloadPlan id. |
| AC-6 | pass | The inspected manifest links both catalogs, accounting, and diagnostics with v3 schemas, paths, decimal-string record counts, effective configuration, generation source, materializability policy, source-mode diagnostics, and warnings. | Recovery-specific fields are correctly deferred to later slices. |
| AC-43 | pass | Joiner/input-map/orchestrator/writer code and tests use WorkloadPlan and input ids. `WorkloadDynamicEvidenceRecord` contains links plus dynamic evidence and does not embed or rewrite WorkloadPlan/FaultScenario structure. | Exact and high-confidence join coverage passed. |
| AC-44 | pass | Required model/generator/package/dynamic/application coverage passes, supplemented by adapter/recipe, dynamic orchestration, executor bridge, accounting, and real dummyapp parser fixtures. | This verdict covers the S2 portion of AC-44 only. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| Required S2 Maven suite | pass | Fresh reviewer run: `Tests run: 161, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| Dynamic orchestration/input-map/reader support | pass | Included in the supplemental 136-test run; all selected dynamic specs passed. |
| Executor-only bridge | pass | `ScenarioExecutorSpec` passed in the supplemental run, and production-reference inspection found canonical v2 behavior only in the approved bridge. |
| Accounting, adapter, recipe, and dummyapp regressions | pass | Included in the supplemental run: `Tests run: 136, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| V3 artifact/manifest inspection | pass | `/tmp/v3-workload-package13892409169161715388/` contains one WorkloadPlan, zero FaultScenarios, linked v3 manifest/accounting/diagnostic metadata, and no v2 catalog. |
| Exact high-precision numeric round trip | pass | `ScenarioCatalogJsonlWriterSpec` passes with `BigDecimal("12345678901234567890.12345678901234567890")`, preserving value type/value, recipe fingerprint, and WorkloadPlan id. |
| Stale recipe-fingerprint rejection | pass | Model and package-reader tests prove changed recipe semantics alter WorkloadPlan identity and a retained stale serialized fingerprint is rejected. |
| `git diff --check` | pass | No whitespace errors. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | The migration is necessarily broad but remains within the canonical scenario package, dynamic linkage, application wiring, tests, and the explicitly approved compile bridge. |
| Existing patterns | pass | Uses existing records, Jackson, SHA-256, `BigInteger`/decimal-string accounting, run-relative path containment, deterministic normalization, and Spock fixtures. |
| Test quality | pass | Tests exercise semantic identity mutations, warning exclusions, package bytes/links, malformed and v2 boundaries, exact numeric persistence, dynamic sidecar non-embedding, and real dummyapp integration. |
| Regression risk | pass | Required and supplemental affected suites pass; reader validation catches semantic tampering; the legacy executor still compiles and cannot consume v3. |
| Security/data safety | pass | Output paths remain constrained by application run-directory validation, persisted numeric semantics are exact, and no destructive migration or external data surface was added. |
| Change hygiene | pass | `git diff --check` passes; unrelated untracked meeting-note files were not modified. |

## Findings

None.

## Done Transition

- Moved to done: `yes`
- Done path: `issues/2026-07-19-compensation-aware-scenario-catalog/done/002-deterministic-v3-workload-package.md`
- Reason if not moved: `n/a`

## Reviewer Notes

FaultScenario semantic records and deep action validation remain intentionally absent because S3/S4 own their generation; S2 validates their v3 schema, ids, and WorkloadPlan links only. The executor-local v2 bridge remains a deliberate temporary compile island and must be removed by S6 as planned.
