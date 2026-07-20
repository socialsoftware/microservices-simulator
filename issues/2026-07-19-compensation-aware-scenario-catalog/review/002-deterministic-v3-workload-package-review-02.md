# Slice Review: 002 - Deterministic V3 Workload Package

## Review Attempt

Attempt: `02`

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
- Completion evidence: `002-deterministic-v3-workload-package.md` → `## Completion Evidence`, including `### Review Attempt 01 Fix Evidence`
- Dependency evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/done/001-compensation-evidence-preservation.md`, `issues/2026-07-19-compensation-aware-scenario-catalog/review/001-compensation-evidence-preservation-review.md`
- Changed files reviewed: all current S2 production changes under `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/{scenario,dynamic,executor}/`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/ScenarioGeneratorApplication.java`, `verifiers/src/main/resources/application.yaml`, `verifiers/src/main/resources/application-test.yaml`, and all changed S2 Spock coverage under `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/{scenario,dynamic,executor}/`
- Prior review reports: `issues/2026-07-19-compensation-aware-scenario-catalog/review/002-deterministic-v3-workload-package-review-01.md`
- Commands run by reviewer:
  - `mvn -Dtest=ScenarioModelSpec,ScenarioGeneratorSpec,ScenarioCatalogJsonlWriterSpec,DynamicEvidenceJoinerSpec,EnrichedScenarioCatalogWriterSpec,DummyappDynamicEnrichmentIntegrationSpec,ScenarioGeneratorApplicationSpec test` from the repository root — command-location failure because the repository has no root POM; rerun correctly from `verifiers/`
  - `cd verifiers && mvn -Dtest=ScenarioModelSpec,ScenarioGeneratorSpec,ScenarioCatalogJsonlWriterSpec,DynamicEvidenceJoinerSpec,EnrichedScenarioCatalogWriterSpec,DummyappDynamicEnrichmentIntegrationSpec,ScenarioGeneratorApplicationSpec test`
  - `cd verifiers && mvn -Dtest=DynamicEnrichmentOrchestratorSpec,DynamicEvidenceReaderSpec,DynamicInputMapWriterSpec,DynamicEnrichmentTestDiscoverySpec,ScenarioExecutorSpec,ScenarioSpaceAccountingCalculatorSpec,DummyappAccountingFixtureFoundationSpec,InputRecipeMapperSpec,ApplicationAnalysisScenarioModelAdapterSpec test`
  - inspected the latest `/tmp/v3-workload-package12536005887520492703/` artifact names, line counts, manifest, accounting, and WorkloadPlan JSON
  - production-reference inspection with `rg` for canonical v2 types/schemas/artifact names and the executor-only legacy island
  - JShell direct `InputRecipe` JSON round-trip with `BigDecimal("12345678901234567890.12345678901234567890")`
  - JShell end-to-end `ScenarioGenerator` → `ScenarioCatalogJsonlWriter` → `ScenarioCatalogPackageReader` reproducer, package at `/tmp/v3-big-decimal-review12952934103985694262/`
  - `git diff --check`

## Summary

Attempt 01's stale-fingerprint defect is fixed: WorkloadPlan identity now hashes recomputed recursive recipe semantics, stale serialized fingerprints are rejected, and the expanded mutation suite passes. The broader v3 artifact shape, dynamic sidecar migration, deterministic fixed-timestamp output, and executor-only legacy island also pass.

The slice still fails because the v3 reader cannot read every valid package produced by the v3 writer. Static decimal literals are represented as `BigDecimal`, but the package reader's default Jackson configuration deserializes an untyped high-precision recipe `value` as `Double`, losing precision before semantic fingerprint and WorkloadPlan-id validation. An end-to-end reviewer reproducer wrote a valid package and the reader rejected it with `INPUT_RECIPE_FINGERPRINT_MISMATCH` and `WORKLOAD_ID_MISMATCH`.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | fail | The canonical v3 writer/reader package boundary is not round-trip safe for supported high-precision decimal recipe literals. |
| Slice out-of-scope respected | pass | FaultScenario records/recovery schedules, on-demand mutation, and v3 executor replay remain deferred; the old executor remains an isolated compile bridge. |
| Spec non-goals respected | pass | No v2 dual writing/upgrading, recovery enumeration, search/scoring, distributed parity, or new dependency was introduced. |
| Dependencies done | pass | Slice 001 is in `done/` with a PASS review. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-1 | pass | Reviewer artifact inspection shows `workload-catalog.jsonl`, empty `fault-scenario-catalog.jsonl`, linked manifest, accounting, and diagnostic; no v2 `scenario-catalog.jsonl`. | FaultScenario population remains correctly deferred. |
| AC-2 | pass | Package-reader tests clearly reject v2; the executor bridge rejects v3 artifact names/schemas and does not discover a v3 manifest as a legacy package. | The new finding concerns valid v3 numeric round trips, not v2 fallback. |
| AC-3 | fail | `InputRecipeMapper` produces `BigDecimal` decimal literals, while `ScenarioCatalogPackageReader` uses a default `ObjectMapper`; the persisted high-precision decimal became `Double 1.2345678901234567E19`, changing the semantic fingerprint and loaded WorkloadPlan id. | Canonical accepted-input semantics are not preserved across the required writer/reader package boundary. |
| AC-5 | pass | The fixed-timestamp test compares all five package artifacts byte-for-byte, generated ids are stable, and the required suite passes. | Stable bytes do not make the lossy reader acceptable. |
| AC-6 | pass | Manifest inspection confirms both linked catalogs, effective config, generation/materializability metadata, exact-string counts, source-mode diagnostics, warnings, accounting, and rejected-input linkage. | Later slices own recovery-cap/per-vector additions. |
| AC-43 | pass | Join/input-map/orchestrator/writer code and tests use WorkloadPlan/input ids; sidecar records do not embed WorkloadPlan or FaultScenario structure. | Supplemental dynamic tests passed. |
| AC-44 | fail | Dummyapp and targeted suites pass, but numeric round-trip coverage only exercises integral wrapper changes and misses a supported decimal recipe that breaks the v3 package reader. | Add end-to-end high-precision decimal package coverage. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| Required S2 Maven suite | pass | Fresh reviewer run: `Tests run: 160, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| Dynamic/orchestrator, executor bridge, accounting, adapter, and recipe regressions | pass | Fresh reviewer run: `Tests run: 136, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| V3 run-directory artifact inspection | pass | `/tmp/v3-workload-package12536005887520492703/` contains the five expected artifacts; one workload, zero FaultScenarios, decimal-string package counts, and no v2 catalog. |
| Production v2-reference inspection | pass | Canonical `ScenarioPlan`/`ScenarioGenerationResult`/`FaultSpace` references are removed; old schema/artifact/vector behavior remains in the inventoried executor/script/Compose bridge. |
| Attempt-01 stale integral fingerprint regression | pass | Model and package tests prove changed integer recipe semantics alter WorkloadPlan identity and stale fingerprints are rejected. |
| High-precision decimal writer/reader round trip | fail | Direct round trip changed `BigDecimal 12345678901234567890.12345678901234567890` to `Double 1.2345678901234567E19`. End-to-end package `/tmp/v3-big-decimal-review12952934103985694262/` was rejected with both `INPUT_RECIPE_FINGERPRINT_MISMATCH` and `WORKLOAD_ID_MISMATCH`. |
| `git diff --check` | pass | No whitespace errors. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | The migration is broad by contract but remains within the scenario package, enrichment, application wiring, and temporary executor bridge. |
| Existing patterns | pass | Uses existing Jackson, SHA-256, BigInteger/string accounting, deterministic collection normalization, run-scoped paths, and Spock fixtures. |
| Test quality | fail | The added stale-fingerprint tests are meaningful, but numeric JSON round-trip coverage stops at `Long`→`Integer` and misses supported arbitrary-precision decimal literals. |
| Regression risk | fail | A valid accepted input containing a sufficiently precise decimal makes the reader reject the writer's own package, blocking later FaultScenario generation/replay from that package. |
| Security/data safety | fail | This is persisted catalog data-integrity loss: decimal recipe content is rounded before canonical validation. |
| Change hygiene | pass | `git diff --check` passes; unrelated untracked meeting notes remain untouched. |

## Findings

| Severity | Finding | Evidence | Required Fix |
|----------|---------|----------|--------------|
| blocking | The v3 package reader performs lossy JSON deserialization for supported high-precision decimal recipe literals and therefore rejects valid packages written by the v3 writer. | `InputRecipeMapper.java:510-534` maps decimal literals to `BigDecimal`. `ScenarioCatalogPackageReader.java:26` constructs a default Jackson mapper without exact untyped numeric deserialization. `InputRecipeFingerprinter.java:123-132` fingerprints the deserialized numeric value. Reviewer direct evidence: `BigDecimal("12345678901234567890.12345678901234567890")` serialized exactly but loaded as `Double 1.2345678901234567E19`; fingerprints differed. End-to-end writer/reader evidence at `/tmp/v3-big-decimal-review12952934103985694262/` failed with `INPUT_RECIPE_FINGERPRINT_MISMATCH` and `WORKLOAD_ID_MISMATCH`. | Preserve exact JSON numeric semantics before recipe fingerprint/WorkloadPlan validation, either through exact-number Jackson configuration or an equally canonical typed representation. Add a `ScenarioGenerator` → writer → reader regression using a high-precision decimal recipe literal and assert the loaded recipe fingerprint and WorkloadPlan id remain unchanged. Rerun the required S2 suite. |

## Done Transition

- Moved to done: `no`
- Done path: `None`
- Reason if not moved: `FAIL — the canonical v3 reader rejects valid writer output for supported high-precision decimal input recipes.`

## Reviewer Notes

The attempt-01 identity-closure finding is resolved for the tested recursive recipe structure and stale serialized fingerprints. The remaining blocker is a package-boundary numeric representation defect, not a request for later recovery/executor functionality.
