## Parent PRD

`issues/structured-input-recipes/prd.md`

## Type

AFK

## What to build

Document the structured recipe export contract for verifier maintainers and future executor work. The documentation should explain recipe semantics, readiness, accepted versus rejected inputs, ownership/provenance separation, and the fact that this work does not implement an executor or materializer.

## Acceptance criteria

- [x] Verifier docs explain that structured `inputRecipe` is the machine-readable contract and existing summaries/provenance remain diagnostics.
- [x] Docs explain `executorReady`, argument/node readiness, and blocking reasons.
- [x] Docs explain `RESOLVED`, `REPLAYABLE`, `PARTIAL`, and `UNRESOLVED` recipe handling through one schema.
- [x] Docs explain accepted input export and rejected input wrapper export.
- [x] Docs explain that catalog acceptance and recipe materializability are separate concepts.
- [x] Docs explain ownership/provenance separation in the recipe-export context.
- [x] Docs include a concrete dummyapp JSON example or fixture-backed equivalent covering representative recipe shapes.
- [x] Docs explicitly state that scenario executor, runtime materializer, and semantic input deduplication are out of scope for this work.

## Verification

- Added `docs/verifiers-impl/structured-input-recipes.md` and linked it from `docs/verifiers-impl/README.md`.
- Updated `docs/verifiers-impl/current-state.md` with the implemented recipe-export scope.
- Verified with focused recipe/scenario tests and the full verifier suite.

## Blocked by

- Blocked by `issues/structured-input-recipes/002-dto-assignment-recipes.md`
- Blocked by `issues/structured-input-recipes/003-collection-transform-recipes.md`
- Blocked by `issues/structured-input-recipes/004-helper-property-recipes.md`
- Blocked by `issues/structured-input-recipes/005-call-placeholder-recipes.md`
- Blocked by `issues/structured-input-recipes/006-rejected-input-wrapper.md`
- Blocked by `issues/structured-input-recipes/007-enriched-recipe-preservation.md`

## User stories addressed

- User story 37
- User story 38
- User story 39
- User story 40
- User story 41
