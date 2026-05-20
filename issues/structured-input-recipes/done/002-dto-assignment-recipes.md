## Parent PRD

`issues/structured-input-recipes/prd.md`

## Type

AFK

## What to build

Extend structured recipes for DTO construction and deterministic same-scope mutation. Dummyapp named-argument, setter, and simple property assignment cases should become machine-readable constructor/assignment recipes instead of only provenance text.

## Acceptance criteria

- [x] Constructor or DTO recipe nodes include `targetTypeFqn` when executor-ready.
- [x] Missing constructor target type produces an explicit deterministic blocker instead of an executor-ready node.
- [x] Typed literal values are preserved inside constructor arguments and assignment values.
- [x] Groovy named arguments such as DTO constructor properties export as assignment entries.
- [x] Setter calls export assignment entries with target property name, setter/source name, value recipe, and source order.
- [x] Simple property assignments export assignment entries with target property name, value recipe, and source order.
- [x] Assignment capture is limited to deterministic same-scope construction and mutation before use.
- [x] Ambiguous multi-writer, alias-heavy, conditional, loop-dependent, or dynamic mutation paths become explicit blockers.
- [x] Existing human-readable summaries and provenance remain available for these inputs.

## Verification

- Added constructor `targetTypeFqn`, named-argument assignments, setter assignments, property assignments, assignment source order, typed assignment values, and explicit blockers for missing target type, duplicate writers, conditional mutations, and loop-dependent mutations.
- Added dummyapp property-assignment coverage in `GroovySagaTracingSpec` and focused mutation-blocker coverage in `GroovyConstructorInputTraceVisitorSpec`.
- Verified mapper and dummyapp adapter assignment shapes with `InputRecipeMapperSpec` and `ApplicationAnalysisScenarioModelAdapterSpec`.
- Ran `mvn -q -Dtest=GroovyConstructorInputTraceVisitorSpec,ApplicationAnalysisScenarioModelAdapterSpec,InputRecipeMapperSpec test` in `verifiers` successfully: 17/10/5 tests, 0 failures.

## Blocked by

- Blocked by `issues/structured-input-recipes/001-accepted-direct-recipes.md`

## User stories addressed

- User story 7
- User story 9
- User story 10
- User story 11
- User story 12
- User story 13
- User story 14
- User story 15
- User story 42
