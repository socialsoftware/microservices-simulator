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
