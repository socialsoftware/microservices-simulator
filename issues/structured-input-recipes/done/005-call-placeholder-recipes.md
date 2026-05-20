## Parent PRD

`issues/structured-input-recipes/prd.md`

## Type

AFK

## What to build

Extend structured recipes for replayable method-call results, runtime-produced values, property access on runtime results, and injectable/source placeholders. The public schema should stay action-oriented and must not expose a facade-specific execution kind.

## Acceptance criteria

- [x] Opaque or runtime-producing calls export as generic `call_result` nodes or equivalent action-oriented public nodes.
- [x] Call-result nodes include receiver recipe or reference, method name, ordered argument recipes, source text, provenance, readiness, and blockers.
- [x] Expected return type is included when known.
- [x] Missing actionable receiver for an otherwise replayable call produces a deterministic blocker.
- [x] Runtime-produced value plus property access exports as call-result/property-access composition.
- [x] Injectable placeholders export placeholder identity, expected type when known, and placeholder purpose.
- [x] Source-provided placeholders remain explicit and do not masquerade as resolved values.
- [x] Dummyapp facade-style functionality calls export as generic call results when invocation is required.
- [x] No public `facade_call` node kind is emitted.
- [x] Existing creation-site or facade analysis facts, if kept, are diagnostic/provenance fields rather than executor action kinds.

## Verification

- Added generic `call_result` nodes for runtime/opaque calls, placeholder nodes for injectable/source placeholders, receiver/argument recipes, expected return types, and deterministic missing-receiver blockers.
- Verified runtime call plus property access composition, placeholder identity/purpose, ordered call arguments, and no public `facade_call` kind in `InputRecipeMapperSpec`.
- Verified dummyapp adapter recipes include `call_result`, `placeholder`, and no `facade_call` nodes in `ApplicationAnalysisScenarioModelAdapterSpec`.
- Ran `mvn -q -Dtest=InputRecipeMapperSpec,ApplicationAnalysisScenarioModelAdapterSpec test` in `verifiers` successfully.

## Blocked by

- Blocked by `issues/structured-input-recipes/001-accepted-direct-recipes.md`
- Blocked by `issues/structured-input-recipes/004-helper-property-recipes.md`

## User stories addressed

- User story 7
- User story 21
- User story 22
- User story 23
- User story 24
- User story 25
- User story 42
