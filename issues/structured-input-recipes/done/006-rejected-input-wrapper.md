## Parent PRD

`issues/structured-input-recipes/prd.md`

## Type

AFK

## What to build

Change rejected input export from a flattened record to a v2 wrapper containing the full input object and rejection metadata. Rejected inputs should preserve the same input fields as accepted inputs, including owners and `inputRecipe`, while keeping rejection reasons explicit.

## Acceptance criteria

- [x] Rejected input JSONL records use `schemaVersion=microservices-simulator.scenario-catalog-rejected-input.v2`.
- [x] Rejected input JSONL records use envelope fields `input`, `rejectionReason`, and `rejectionWarnings`.
- [x] The `input` object is the full input variant shape, including owners, source-mode metadata, summaries, provenance, warnings, and `inputRecipe` when available.
- [x] Rejection reason and rejection warnings remain explicit and separate from recipe readiness.
- [x] Rejected TCC/MIXED dummyapp or equivalent fixture inputs preserve recipe structure when trace recipes exist.
- [x] Rejected input export remains deterministic.
- [x] Scenario catalog manifest rejected-input counts and rejection-reason counters remain correct.
- [x] Backward compatibility with the old flattened rejected-input shape is not added.

## Verification

- Changed rejected input export to a v2 envelope with `schemaVersion`, full `input`, `rejectionReason`, and `rejectionWarnings`; no flattened backward-compatible record shape was added.
- Verified full input preservation, including owners/source metadata/provenance/warnings and embedded recipes, plus manifest rejected counts/reason counters, in `ScenarioCatalogJsonlWriterSpec`.
- Ran `mvn -q -Dtest=ScenarioCatalogJsonlWriterSpec test` in `verifiers` successfully.

## Blocked by

- Blocked by `issues/structured-input-recipes/001-accepted-direct-recipes.md`

## User stories addressed

- User story 4
- User story 5
- User story 8
- User story 26
- User story 30
- User story 31
- User story 32
- User story 35
