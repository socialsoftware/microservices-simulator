## Parent PRD

`issues/structured-input-recipes/prd.md`

## Type

AFK

## What to build

Add the first vertical slice of embedded structured input recipes for accepted scenario inputs. A dummyapp direct-constructor input should export an `inputRecipe` payload in `scenario-catalog.jsonl` with schema versioning, typed literal nodes, readiness, deterministic fingerprinting, and preserved existing input diagnostics.

This slice establishes the public model/export contract that later slices extend.

## Acceptance criteria

- [x] Accepted scenario catalog JSONL uses `schemaVersion=microservices-simulator.scenario-catalog.v2`.
- [x] Accepted input variants can include `inputRecipe` with `schemaVersion=microservices-simulator.input-recipe.v1`.
- [x] A representative dummyapp direct-constructor input exports typed literal recipe nodes and argument entries ordered by constructor index.
- [x] `inputRecipe.executorReady`, argument readiness, node readiness, and blocking-reason fields are present with deterministic values.
- [x] `inputRecipe.recipeFingerprint` is deterministic across repeated runs for the same recipe.
- [x] `inputVariantId` includes the recipe fingerprint in its hash inputs.
- [x] Existing `logicalKeyBindings` remain part of the input variant hash.
- [x] Owners are preserved on the input but are not included solely as recipe identity/hash material.
- [x] Existing summaries, provenance text, source-mode metadata, owners, and warnings remain available in accepted inputs.
- [x] No recipe sidecar file is introduced for this behavior.

## Verification

- Added embedded `inputRecipe` to `InputVariant`, public recipe DTOs, deterministic fingerprinting, and v2 accepted catalog schema.
- Verified direct-constructor typed literal recipes, constructor-index ordering, readiness/blocker fields, deterministic fingerprints, recipe-aware input IDs, preserved logical bindings/owners/provenance/source metadata, and no sidecar output with `InputRecipeMapperSpec`, `ApplicationAnalysisScenarioModelAdapterSpec`, `ScenarioCatalogJsonlWriterSpec`, and `ScenarioGeneratorSpec`.
- Ran `mvn -q -DskipTests compile` in `verifiers` successfully.
- Ran `mvn -q -Dtest=InputRecipeMapperSpec,ApplicationAnalysisScenarioModelAdapterSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorSpec,EnrichedScenarioCatalogWriterSpec test` in `verifiers` successfully.

## Blocked by

None - can start immediately

## User stories addressed

- User story 1
- User story 2
- User story 3
- User story 6
- User story 8
- User story 26
- User story 27
- User story 28
- User story 29
- User story 30
- User story 31
- User story 32
- User story 35
- User story 39
- User story 40
