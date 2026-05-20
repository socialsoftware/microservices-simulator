## Parent PRD

`issues/structured-input-recipes/prd.md`

## Type

AFK

## What to build

Extend structured recipes for helper-reduced values and property access. Dummyapp helper chains should export `helper_result` nodes with nested result recipes and `property_access` nodes, preserving provenance while making clear that a future executor does not need to call Spock helper methods for reduced helper results.

## Acceptance criteria

- [x] Helper-return recipes export a `helper_result` node or equivalent public recipe kind.
- [x] The helper node preserves useful helper provenance, source text, and nested `resultRecipe` or equivalent child result field.
- [x] Property access exports as a node over the receiver/result recipe, not as flattened text.
- [x] Helper-return plus property-access dummyapp cases export nested machine-readable structure.
- [x] The exported helper-reduced recipe does not require the future executor to call the Spock helper method.
- [x] Existing provenance text still explains the helper chain for human debugging.
- [x] Readiness and blockers are propagated from nested helper result and property access nodes.

## Verification

- Added `helper_result` and `property_access` public nodes, preserving source/provenance text and nested `resultRecipe`/receiver structure.
- Verified helper-reduced nested structure and blocker/readiness propagation in `InputRecipeMapperSpec`.
- Verified dummyapp helper-chain/property-access recipes remain machine-readable while human provenance still explains the helper chain in `ApplicationAnalysisScenarioModelAdapterSpec` and existing Groovy trace specs.
- Ran `mvn -q -Dtest=InputRecipeMapperSpec,ApplicationAnalysisScenarioModelAdapterSpec,GroovyConstructorInputTraceVisitorSpec test` in `verifiers` successfully.

## Blocked by

- Blocked by `issues/structured-input-recipes/001-accepted-direct-recipes.md`

## User stories addressed

- User story 20
- User story 24
- User story 35
- User story 42
