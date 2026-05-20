## Parent PRD

`issues/structured-input-recipes/prd.md`

## Type

AFK

## What to build

Run and record a Quizzes smoke validation after dummyapp coverage and documentation exist. The smoke should validate that the structured recipe export works on the realistic target without turning exact scenario counts into brittle acceptance criteria.

## Acceptance criteria

- [x] Quizzes smoke validation is run after dummyapp-focused recipe coverage exists.
- [x] The smoke produces scenario catalog records with representative `inputRecipe` payloads.
- [x] Recipe and catalog schema versions are correct in the smoke output.
- [x] Human-readable summaries, provenance, source-mode metadata, owners, and warnings remain present in representative Quizzes records.
- [x] Smoke interpretation avoids brittle exact-count assertions.
- [x] Any remaining partial, unresolved, or blocked recipe shapes are documented as diagnostic output rather than executor-ready claims.
- [x] Validation notes are recorded locally next to the PRD or in the relevant implementation documentation.

## Verification

- Ran the Quizzes smoke with scenario catalog export enabled and dynamic enrichment disabled.
- Verified accepted catalog schema `microservices-simulator.scenario-catalog.v2`, recipe schema `microservices-simulator.input-recipe.v1`, and rejected-input schema `microservices-simulator.scenario-catalog-rejected-input.v2`.
- Verified representative recipe node kinds in Quizzes output and documented remaining blockers as diagnostics in `docs/verifiers-impl/structured-input-recipes.md`.

## Blocked by

- Blocked by `issues/structured-input-recipes/008-document-recipe-contract.md`

## User stories addressed

- User story 35
- User story 36
