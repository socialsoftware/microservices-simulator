## Parent PRD

`issues/structured-input-recipes/prd.md`

## Type

AFK

## What to build

Preserve embedded input recipes through dynamic enrichment sidecars without changing dynamic joiner behavior. Enriched records should carry the same scenario-plan input recipes produced by static catalog export.

## Acceptance criteria

- [x] Enriched scenario catalog records preserve `scenarioPlan.inputs.inputRecipe` from the static scenario plan.
- [x] Recipe fingerprints and readiness fields are unchanged by enrichment serialization.
- [x] Dynamic join statuses and attribution behavior remain unchanged for existing covered dummyapp cases.
- [x] Enriched catalog schema version is bumped to `microservices-simulator.scenario-catalog-enriched.v2` if the enriched output contract includes v2 scenario plans.
- [x] Enriched manifest schema version is bumped to `microservices-simulator.scenario-catalog-enriched-manifest.v2` if required by the output contract.
- [x] No new dynamic joiner matching, attribution, or ambiguity logic is introduced.

## Verification

- Preserved embedded recipes through scenario plan serialization and dynamic enrichment sidecar output without changing joiner matching or attribution logic.
- Bumped enriched record and manifest schema versions to v2.
- Verified recipe fingerprint/readiness preservation and existing dynamic join statuses in `EnrichedScenarioCatalogWriterSpec`.
- Ran `mvn -q -Dtest=EnrichedScenarioCatalogWriterSpec test` in `verifiers` successfully.

## Blocked by

- Blocked by `issues/structured-input-recipes/001-accepted-direct-recipes.md`

## User stories addressed

- User story 33
- User story 34
- User story 39
- User story 40
