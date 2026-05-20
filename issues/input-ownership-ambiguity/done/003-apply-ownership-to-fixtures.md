## Parent PRD

`issues/input-ownership-ambiguity/prd.md`

## Type

AFK

## What to build

Extend the ownership model from helper-created inputs to fixture-derived inputs. The analyzer should preserve conservative ownership for inputs created through `setup()`, field initializers, inherited helper/setup/field paths, and `setupSpec()` analysis metadata. Runtime attribution should improve for the in-scope per-feature fixture contexts, while `setupSpec()` remains analysis metadata only unless better runtime behavior falls out naturally.

This slice uses dummyapp-oriented behavior to prove the model handles the fixture shapes already present in the repository.

## Acceptance criteria

- [x] `setup()`-created inputs expose ownership metadata for the feature executions statically justified by the spec hierarchy.
- [x] Field-initialized inputs expose ownership metadata where the field is statically tied to feature execution through the existing trace model.
- [x] Inherited helper, setup, and field fixture paths expose ownership metadata without broad class-wide guessing.
- [x] `setupSpec()`-created inputs preserve ownership or lifecycle metadata in the analyzed model/exported artifacts, but tests do not require improved runtime attribution for `setupSpec()` execution events.
- [x] Runtime attribution uses fixture ownership for in-scope contexts and remains conservative when ownership is not justified.
- [x] Dummyapp regression coverage demonstrates helper/setup/field/inherited ownership behavior through public analysis/export/runtime surfaces.
- [x] Same-feature sibling ambiguity in dummyapp remains valid when runtime identity cannot distinguish sibling inputs.
- [x] The implementation does not introduce semantic deduplication of value-equivalent fixture inputs.

## Validation

Implemented fixture ownership derivation in `ApplicationAnalysisScenarioModelAdapter`: direct/helper traces own themselves; `setup()`, field initializer, inherited fixture, and `setupSpec()` traces receive feature owners from analyzed feature traces for the same effective spec class. This keeps `setupSpec()` as exported analysis metadata and does not add runtime-specific setupSpec attribution. Deduplication still uses deterministic ids and existing normalization, not semantic value equivalence.

Validated with:

- `mvn -Dtest=GroovyConstructorInputTraceVisitorDummyappSpec test` in `verifiers/`: BUILD SUCCESS, 22 tests, 0 failures.
- `mvn -Dtest=DynamicEvidenceJoinerSpec,DynamicInputMapWriterSpec test` in `verifiers/`: BUILD SUCCESS, 20 tests, 0 failures.
- `mvn -Dtest=DynamicInputMapTest test` in `simulator/`: BUILD SUCCESS, 7 tests, 0 failures.

## Blocked by

- Blocked by `issues/input-ownership-ambiguity/001-resolve-owned-helper-inputs.md`

## User stories addressed

- User story 2
- User story 3
- User story 4
- User story 5
- User story 6
- User story 16
- User story 22
