# 002 - Deterministic V3 Workload Package

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `001-compensation-evidence-preservation.md`  
ACs covered: `AC-1, AC-2, AC-3, AC-5, AC-6, AC-43, AC-44`  
Risk: `high`

## Purpose

Establish WorkloadPlan as the canonical deterministic structural contract, write the clean v3 package shape, and move dynamic enrichment to WorkloadPlan-linked sidecar evidence while isolating the not-yet-migrated executor as a temporary non-v3 compile bridge until S6.

## Scope

- Replace canonical generated structural records/results with WorkloadPlans containing participants, accepted inputs, one global forward schedule, conflict evidence, ordered fault slots, and ordered classified compensation checkpoints.
- Define WorkloadPlan identity over every semantic field listed by AC-3, excluding diagnostic-only warnings.
- Write `workload-catalog.jsonl`, `fault-scenario-catalog.jsonl`, `scenario-catalog-manifest.json`, and `scenario-space-accounting.json`; the FaultScenario artifact may initially be empty until S3/S4 populate it.
- Add a v3 package reader that validates linked schema/artifact metadata and clearly rejects v2 records/artifacts.
- Preserve deterministic record ordering and byte-stable output for fixed source facts, effective configuration, and supplied generation timestamp.
- Migrate dynamic input maps, joining, sidecar records/manifests, and orchestration to WorkloadPlan/input identity only; sidecars reference WorkloadPlan ids and do not embed/rewrite WorkloadPlan or create FaultScenarios.
- Keep rejected-input diagnostics as a non-canonical diagnostic artifact if still needed, clearly linked from the v3 manifest and not mistaken for either semantic catalog.
- Retain only an explicitly inventoried executor-only v2 legacy island needed for compilation until S6; it must reject/not discover v3 packages and must not be exposed as a canonical v3 reader or generated artifact.

## Out of Scope

- Non-zero vectors, recovery action schedules, eager baseline population, and on-demand mutation.
- Executor replay and final executor migration, which belong to S6.
- V2 dual writing, upgrading, fallback parsing, or enriched-catalog execution. The temporary executor-only compile bridge is not compatibility support and cannot consume v3 packages.

## Repo Anchors

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/ScenarioPlan.java`, `ScenarioGenerationResult.java`, and `ScenarioCatalogManifest.java` — v2 contract being cleanly replaced.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/ScenarioGenerator.java` and `ScenarioIdGenerator.java` — deterministic structural generation/identity.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogJsonlWriter.java` — JSONL/manifest/path pattern.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/ScenarioGeneratorApplication.java` — run-scoped artifact wiring.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/dynamic/` — current ScenarioPlan-owned enrichment path.
- `verifiers/src/main/resources/application.yaml` and `application-test.yaml` — canonical v3 filenames/defaults.

## Implementation Shape

- Keep WorkloadPlan forward schedule generation delegated to existing `ScheduleEnumerator`; do not mix recovery enumeration into this slice.
- Introduce a pure scenario-layer structural validator reusable later by eager eligibility, on-demand generation, and executor validation. It covers references, ownership, occurrence identity, runtime step-name mapping, fault-space shape, and supported Saga/local shape without claiming materialization/startup success.
- Ensure identity canonicalization preserves semantically ordered lists and only sorts collections declared unordered by the contract.
- Represent exact large accounting values as decimal strings/`BigInteger`-backed values from the first v3 schema, even where this slice has only workload/vector-space facts.
- Make the manifest the package link and schema/configuration source; readers resolve the two catalogs from it rather than guessing v2 filenames.
- Dynamic evidence remains a separate artifact and cannot be preferred by the executor as an alternative static catalog.
- Inventory the temporary legacy island at preflight and constrain it to the current executor contract files: `CatalogScenarioRecord`, `ScenarioCatalogReader`, `ScenarioExecutor`, `ScenarioExecutorOptions`, `ScenarioExecutionReport`, `ScenarioExecutorCli`, `ScenarioExecutorOrchestrator`, plus `verifiers/scripts/run-scenario-executor.sh` and the Compose executor service where they still expose v2 names/options. No new v2 callers may be added, and S6 owns removal.

## TDD / Test Shape

- First behavior to test: changing one ordered compensation checkpoint evidence class changes WorkloadPlan id, while changing only a warning does not.
- Expected red failure: current `ScenarioPlan` has no checkpoints/evidence and its writer emits only `scenario-catalog.jsonl`.
- Additional coverage: field-by-field identity mutation matrix; byte-stable rerun; v3 artifact listing and manifest links; no v2 file; malformed/dangling package diagnostics; explicit v2 rejection; dynamic exact/high-confidence joins keyed to WorkloadPlan and input ids; sidecars contain no rewritten WorkloadPlan/FaultScenario structure; temporary executor bridge compiles but rejects/is never passed a v3 package.

## Just-in-Time Preflight Required

Before source edits, the executor must re-check:

- All production/test references to `ScenarioPlan`, v2 schema strings, old artifact names, and enriched wrappers; record which references are migrated in S2 and which exact executor-only references form the temporary bridge removed by S6.
- Whether HTML reporting consumes scenario-model classes directly.
- Current dynamic input-map and join-index assumptions about embedded schedule/input records.
- Existing generated-at handling so byte-stability assertions compare runs with the same explicit timestamp.

## Verification

- `cd verifiers && mvn -Dtest=ScenarioModelSpec,ScenarioGeneratorSpec,ScenarioCatalogJsonlWriterSpec,DynamicEvidenceJoinerSpec,EnrichedScenarioCatalogWriterSpec,DummyappDynamicEnrichmentIntegrationSpec,ScenarioGeneratorApplicationSpec test` — v3 model/package/enrichment behavior passes and the full main module compiles with only the inventoried executor bridge remaining.
- Inspect a test run directory — both v3 JSONL files, manifest, and accounting exist; `scenario-catalog.jsonl` does not.
- Inspect production references — every remaining v2/`ScenarioPlan` reference is in the recorded executor-only bridge and no bridge path accepts the v3 manifest/catalogs.

## Evidence to Record

- files changed
- commands run and outputs
- v3 artifact names/schema versions and v2 rejection diagnostic
- deterministic id/byte comparison
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- This is a broad breaking migration. The only allowed intermediate v2 production references are the explicitly inventoried executor-only compile bridge; they cannot consume v3, cannot grow, and must be removed in S6.
- Do not hash diagnostics, generated timestamps, artifact paths, or enrichment evidence into WorkloadPlan identity.
- Preserve global forward schedule order; sorting it for hashing would erase semantics.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Replaced the canonical v2 `ScenarioPlan`/`FaultSpace` result model with deterministic v3 `WorkloadPlan` records containing ordered participants, accepted inputs, one global forward schedule, conflict evidence, one forward fault slot per occurrence, and ordered compensation checkpoints with retained phase-specific footprints and primary evidence classes.
- Added semantic WorkloadPlan hashing and a pure structural validator covering ids/references, ownership, occurrence/runtime-step mapping, fault-space shape, checkpoint shape/order, and the supported `SAGA_LOCAL` contract. Diagnostic warnings are serialized but excluded from identity.
- Added the linked v3 package writer/reader for `workload-catalog.jsonl`, empty-until-S3/S4 `fault-scenario-catalog.jsonl`, `scenario-catalog-manifest.json`, exact-string `scenario-space-accounting.json`, and the rejected-input diagnostic. The reader validates schemas, links, exact counts, workload structure, and dangling FaultScenario references, and clearly rejects v2.
- Migrated dynamic input maps, joining, orchestration, sidecar records, manifests, and defaults to WorkloadPlan/input identity. Sidecar records contain only `workloadPlanId`, `inputVariantIds`, and dynamic evidence; they do not embed WorkloadPlans or create/rewrite FaultScenarios.
- Removed canonical v2 model classes. Kept the temporary v2 executor compile bridge only in the inventoried executor files/scripts/Compose surface, using executor-local `LegacyScenarioPlan`/`LegacyFaultSpace`; it explicitly refuses v3 artifacts/packages.

### Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/{WorkloadPlan,WorkloadGenerationResult,WorkloadExecutionShape,ForwardFaultSlot,CompensationCheckpoint}.java` — added the canonical v3 workload model.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/{ScenarioPlan,ScenarioGenerationResult,FaultSpace}.java` — removed canonical v2 records.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/{ScheduledStep,ScenarioCatalogManifest,RejectedInputVariant}.java` — added runtime occurrence mapping and v3 manifest/diagnostic schemas.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/{ScenarioGenerator,ScenarioIdGenerator,ScenarioGeneratorConfig,WorkloadPlanValidator}.java` — generated complete WorkloadPlans, closed semantic identity, renamed write mode, and added pure structural validation.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/{ScenarioCatalogJsonlWriter,ScenarioCatalogPackageReader}.java` — wrote/read and validated the linked v3 package.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/accounting/ScenarioSpaceAccountingReport.java` — added v3 per-workload exact fault-slot/`2^n` layers and explicit not-computed FaultScenario recovery status.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/ScenarioGeneratorApplication.java` — wired both v3 catalogs, manifest/accounting, and WorkloadPlan-linked enrichment into run-scoped output.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/dynamic/**` — migrated enrichment config, input map, joiner, unmatched diagnostics, orchestration, writer, result model, and sidecar record from embedded ScenarioPlan ownership to WorkloadPlan/input links; replaced `EnrichedScenarioRecord` with `WorkloadDynamicEvidenceRecord`.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/{CatalogScenarioRecord,ScenarioCatalogReader,ScenarioExecutor}.java` — isolated the compile-only legacy v2 model/reader and made it reject v3.
- `verifiers/src/main/resources/{application.yaml,application-test.yaml}` — made the canonical v3 artifact/sidecar filenames and `WRITE_WORKLOADS` defaults explicit.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/{ScenarioGeneratorApplicationSpec,dynamic/**,executor/ScenarioExecutorSpec,scenario/**}.groovy` — migrated coverage and added identity mutation, byte-stability, package-link/schema/count, malformed/dangling, v2/legacy rejection, sidecar non-embedding, and dummyapp integration assertions.
- `issues/2026-07-19-compensation-aware-scenario-catalog/002-deterministic-v3-workload-package.md` — recorded this completion evidence.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=ScenarioModelSpec,ScenarioGeneratorSpec,ScenarioCatalogJsonlWriterSpec,DynamicEvidenceJoinerSpec,EnrichedScenarioCatalogWriterSpec,DummyappDynamicEnrichmentIntegrationSpec,ScenarioGeneratorApplicationSpec test` | PASS | Fresh required run: `Tests run: 83, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=DynamicEnrichmentOrchestratorSpec,DynamicEvidenceReaderSpec,DynamicInputMapWriterSpec,DynamicEnrichmentTestDiscoverySpec test` | PASS | `Tests run: 23, Failures: 0, Errors: 0, Skipped: 0`; WorkloadPlan-linked input-map/orchestration support passed. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | PASS | `Tests run: 54, Failures: 0, Errors: 0, Skipped: 0`; executor-only v2 bridge still compiles and preserves its pre-S6 behavior. |
| `cd verifiers && mvn -Dtest=ScenarioSpaceAccountingCalculatorSpec,DummyappAccountingFixtureFoundationSpec test` | PASS | `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`; existing accounting and real dummyapp generation remained green. |
| `cd verifiers && mvn -Dtest=ScenarioCatalogJsonlWriterSpec test` | PASS | Fresh post-reader validation run: `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`. |
| Inspect `/tmp/v3-workload-package3468472947773604714/` | PASS | Contains `workload-catalog.jsonl` (1 record), empty `fault-scenario-catalog.jsonl`, linked manifest, accounting, and rejected-input diagnostic; `scenario-catalog.jsonl` is absent. Manifest schemas are WorkloadPlan v3, FaultScenario v3, manifest v3, and accounting v3; counts are decimal strings. |
| Production-reference inspection with `rg --pcre2` | PASS | No exact `ScenarioPlan` production references. All remaining v2 schema/artifact/vector-overlay references are confined to the inventoried executor bridge: `CatalogScenarioRecord`, `ScenarioCatalogReader`, `ScenarioExecutor`, `ScenarioExecutorOptions`, `ScenarioExecutionReport`, `ScenarioExecutorCli`, `ScenarioExecutorOrchestrator`, `scripts/run-scenario-executor.sh`, and Compose. No canonical/dynamic/reporting path consumes the bridge. |
| `git diff --check` | PASS | No whitespace errors reported. |

### Acceptance Criteria Evidence

- AC-1: Writer/application tests and inspected output prove both v3 JSONL artifacts, linking manifest, and accounting are written while `scenario-catalog.jsonl` is absent.
- AC-2: `ScenarioCatalogPackageReader` rejects v2 with `v3 WorkloadPlan/FaultScenario packages are required and v2 catalogs are not supported`; the temporary legacy executor reports that it does not support v3 and S6 owns persisted replay.
- AC-3: Generator/model tests cover every structural collection, ordered fault-slot/checkpoint occurrences, checkpoint evidence mutation, warning-only controls, phase-specific checkpoint footprints, and structural validation.
- AC-5: Repeated generation preserves WorkloadPlan ids; the fixed-timestamp writer test compares all five package artifact texts byte-for-byte across reruns.
- AC-6: Manifest assertions cover linked artifact kinds, record schemas/paths/exact-string counts, effective generator config, generation source, materializability policy, source-mode diagnostics, and warnings.
- AC-43: Joiner/orchestrator tests prove exact and high-confidence joins use WorkloadPlan/input ids; serialized sidecars contain no `workloadPlan`, `faultScenario`, or forward/action schedule structure.
- AC-44: Required model/generator/package/dynamic/application coverage passes, including real dummyapp parser/enrichment and accounting fixtures.

### Browser / Manual Evidence

- Not required.

### TDD Notes

- Replaced the model spec first and observed the expected red failure from `cd verifiers && mvn -Dtest=ScenarioModelSpec test`: Groovy compilation could not resolve `WorkloadPlan`, `ForwardFaultSlot`, `CompensationCheckpoint`, or `WorkloadExecutionShape` (14 errors). Implemented the v3 model/identity/validator and then drove the model, generator, package, dynamic, application, accounting, and bridge suites to green.

### Deviations From Plan

- None. The FaultScenario artifact is intentionally empty in S2 as approved; S3/S4 own its records. The executor-local v2 bridge is compile-only, cannot discover/consume v3 packages, and remains scheduled for removal in S6.

### Blockers / Follow-Ups

- None for this slice. Ready for `sp-review-slice`; S3 is the next dependent implementation slice after review.

### Review Attempt 01 Fix Evidence

Status: `implemented-awaiting-review`

The blocking attempt-01 finding is fixed without changing slice scope:

- `InputRecipe.semanticFingerprint()` now recomputes the fingerprint from canonical recipe schema, readiness, blockers, ordered arguments, and the complete recursive recipe-node structure. `ScenarioIdGenerator` hashes this recomputed semantic fingerprint rather than trusting the serialized `recipeFingerprint` field.
- `WorkloadPlanValidator` compares the serialized and recomputed fingerprints and emits `INPUT_RECIPE_FINGERPRINT_MISMATCH`; because the package reader applies this validator, a stale fingerprint is rejected before a WorkloadPlan is accepted.
- Numeric recipe values now fingerprint by canonical decimal value rather than JVM numeric wrapper class, preserving fingerprint integrity across JSON round trips such as `Long` serialization followed by `Integer` deserialization.
- `ScenarioModelSpec` now includes a field-by-field nested identity mutation matrix covering participants, accepted-input metadata and owners, recipe/argument/literal semantics, scheduled occurrences, conflicts/aggregate keys, fault slots, checkpoints/evidence, and phase-specific footprints, plus warning-only controls at every warning-bearing WorkloadPlan layer.
- A direct stale-fingerprint regression changes a recipe literal while retaining the old serialized fingerprint and proves both that the WorkloadPlan id changes and that validation fails. `ScenarioCatalogJsonlWriterSpec` tampers the persisted recipe literal and proves the v3 package reader rejects it with the fingerprint-mismatch diagnostic.

#### Fix Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/InputRecipe.java`
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/InputRecipeFingerprinter.java`
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/ScenarioIdGenerator.java`
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/WorkloadPlanValidator.java`
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/ScenarioModelSpec.groovy`
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogJsonlWriterSpec.groovy`
- `issues/2026-07-19-compensation-aware-scenario-catalog/002-deterministic-v3-workload-package.md`

#### Fix TDD and Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=ScenarioModelSpec,ScenarioCatalogJsonlWriterSpec test` before the fix | EXPECTED FAIL | `Tests run: 15, Failures: 2`: a changed literal with the old fingerprint retained the same WorkloadPlan id, and the package reader accepted the tampered record. |
| Same focused command after semantic hashing/validation | PASS | `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`; this run also exposed and then drove the JSON numeric round-trip canonicalization fix. |
| `cd verifiers && mvn -Dtest=ScenarioModelSpec test` after completing the field matrix | PASS | `Tests run: 82, Failures: 0, Errors: 0, Skipped: 0`; the final required run below includes the subsequently added aggregate-key field rows and reports 85 model iterations. |
| `cd verifiers && mvn -Dtest=ScenarioModelSpec,ScenarioGeneratorSpec,ScenarioCatalogJsonlWriterSpec,DynamicEvidenceJoinerSpec,EnrichedScenarioCatalogWriterSpec,DummyappDynamicEnrichmentIntegrationSpec,ScenarioGeneratorApplicationSpec test` | PASS | Fresh slice-required run: `Tests run: 160, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. `ScenarioModelSpec` contributed 85 passing iterations and package-reader coverage contributed 5 passing tests. |
| `cd verifiers && mvn -Dtest=InputRecipeMapperSpec,ApplicationAnalysisScenarioModelAdapterSpec test` | PASS | `Tests run: 19, Failures: 0, Errors: 0, Skipped: 0`; recipe mapping/fingerprints and dummyapp adapter behavior remain green. |
| `git diff --check` | PASS | No whitespace errors. |

AC-3 and the S2 portion of AC-44 are now supported by the nested mutation matrix and persisted stale-fingerprint rejection. No review file, spec/plan artifact, acceptance checkbox, or dependent slice was edited.

### Review Attempt 02 Fix Evidence

Status: `implemented-awaiting-review`

The single blocking attempt-02 high-precision numeric round-trip finding is fixed:

- `ScenarioCatalogPackageReader` now enables Jackson's exact untyped numeric deserialization with `USE_BIG_DECIMAL_FOR_FLOATS` and `USE_BIG_INTEGER_FOR_INTS` before converting package trees into WorkloadPlan records. Persisted decimal recipe literals therefore reach fingerprint and WorkloadPlan-id validation without a lossy `Double` conversion.
- `ScenarioCatalogJsonlWriterSpec` now runs `ScenarioGenerator` → package writer → package reader with `BigDecimal("12345678901234567890.12345678901234567890")` and asserts the loaded value remains an equal `BigDecimal`, the recipe fingerprint is unchanged, and the WorkloadPlan id is unchanged.

#### Attempt 02 Fix Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogPackageReader.java`
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogJsonlWriterSpec.groovy`
- `issues/2026-07-19-compensation-aware-scenario-catalog/002-deterministic-v3-workload-package.md`

#### Attempt 02 TDD and Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=ScenarioCatalogJsonlWriterSpec test` before exact-number reader configuration | EXPECTED FAIL | `Tests run: 6, Failures: 0, Errors: 1`; the generated high-precision package was rejected with `INPUT_RECIPE_FINGERPRINT_MISMATCH` and `WORKLOAD_ID_MISMATCH`, reproducing review attempt 02. |
| Same focused command after exact-number reader configuration | PASS | `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`; the high-precision value, recipe fingerprint, and WorkloadPlan id survive the writer/reader boundary exactly. |
| `cd verifiers && mvn -Dtest=ScenarioModelSpec,ScenarioGeneratorSpec,ScenarioCatalogJsonlWriterSpec,DynamicEvidenceJoinerSpec,EnrichedScenarioCatalogWriterSpec,DummyappDynamicEnrichmentIntegrationSpec,ScenarioGeneratorApplicationSpec test` | PASS | Fresh slice-required run: `Tests run: 161, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `git diff --check` | PASS | No whitespace errors. |

AC-3 and the S2 portion of AC-44 now include exact high-precision decimal package round-trip evidence. This fix did not change recipe semantics, package schemas, writer bytes, recovery/executor scope, dependencies, review artifacts, spec/plan artifacts, or acceptance checkboxes.
