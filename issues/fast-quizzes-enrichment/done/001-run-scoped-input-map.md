## Parent PRD

`issues/fast-quizzes-enrichment/prd.md`

## Type

AFK

## What to build

Replace the per-class dynamic input-map contract with a run-scoped map containing all accepted static catalog inputs. Update simulator loading and resolution so multiple test identities can resolve against the same map without broadening the current owner/source/functionality/step matching semantics.

This is the first tracer bullet for the new run-scoped pipeline. It should prove that the input attribution contract can stop depending on one input map per test class while preserving existing matching behavior.

## Acceptance criteria

- [x] A run-scoped dynamic input map contains all accepted `ScenarioPlan.inputs()` provided to the writer in deterministic order.
- [x] The run-scoped input map has no singular root `testClassFqn` gate.
- [x] `selectedTestClassFqns` or equivalent selected-class metadata is written only as audit metadata and does not prune accepted inputs.
- [x] Simulator input-map loading accepts the new run-scoped schema without backward-compatibility handling for the old per-class schema.
- [x] Runtime resolution can match inputs owned by at least two different test classes from the same loaded map.
- [x] Runtime resolution rejects a non-owner/non-source test identity even when functionality class and step name match.
- [x] Runtime resolution preserves the existing owner/source fallback, functionality class, and step-name matching predicate except for removal of the old root per-class gate.
- [x] Tests exercise public writer/loader/resolver behavior rather than private matching helpers.

## Validation

- Updated `DynamicInputMapWriter` to accept `selectedTestClassFqns` audit metadata, write all accepted `ScenarioPlan.inputs()` without selected-class pruning, sort selected class metadata deterministically, and remove the singular root `testClassFqn` from the persisted schema.
- Updated simulator `DynamicInputMap` loading/resolution to use the run-scoped schema and remove the root class gate while preserving owner/source fallback, functionality-class equality, and step-name matching.
- Added/updated public-behavior coverage in `DynamicInputMapWriterSpec` for all-input deterministic map output, missing root `testClassFqn`, and selected-class metadata not pruning inputs.
- Added public loader/resolver coverage in `DynamicInputMapTest` proving a loaded run-scoped map resolves two different test classes and rejects a non-owner/non-source identity with matching functionality and step.
- Ran `mvn -Dtest=DynamicInputMapTest test` in `simulator/`: passed, 10 tests.
- Ran `mvn -Dtest=DynamicInputMapWriterSpec test` in `verifiers/`: passed, 3 tests.
- Ran `mvn -Dtest=DynamicEnrichmentOrchestratorSpec test` in `verifiers/`: passed, 5 tests, confirming the current public orchestrator entry point still compiles and works with the writer API change.

## Blocked by

None - can start immediately

## User stories addressed

- User story 3
- User story 4
- User story 7
- User story 8
- User story 9
- User story 11
- User story 21
- User story 23
