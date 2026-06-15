## Parent PRD

`issues/scenario-executor-poc/prd.md`

## Type

AFK

## What to build

Extend the materialization path through public behavior tests for DTO/object constructor recipes. The executor should materialize constructor nodes with target type FQNs, simple positional arguments, setter assignments, and property assignments, and it should report structured blockers for missing target types or assignment values that cannot be materialized.

## Acceptance criteria

- [x] A constructor recipe with a target type FQN can instantiate that target type reflectively.
- [x] Constructor recipe positional arguments are materialized in argument-index order.
- [x] Setter assignments are applied in exported order after construction.
- [x] Simple property assignments are applied in exported order after construction.
- [x] Materialized DTO/object values can be used as saga constructor arguments in the fixture execution path.
- [x] A constructor recipe missing a required target type FQN is rejected with a structured `MISSING_TARGET_TYPE`-style blocker.
- [x] An assignment whose value recipe is not materializable is rejected with a structured blocker that points to the affected assignment or argument.
- [x] Tests verify observable materialized behavior through public materialization/execution output, not private reflection helpers.

## Feature criteria covered

- AC-010
- AC-014
- AC-015
- AC-022

## Domain context

Preserve the `constructor`, setter assignment, property assignment, target type FQN, and blocker semantics documented in `docs/verifiers-impl/structured-input-recipes.md`.

## Verification plan

- Run focused verifier tests for DTO constructor materialization and fixture execution using a constructed DTO/object recipe.
- Suggested command: `cd verifiers && mvn test -Dtest='*Materializ*Spec,*ScenarioExecutor*Spec' -DfailIfNoTests=false`

## Completion evidence

- Extended `ScenarioMaterializer` to instantiate constructor recipes by target type FQN, materialize positional arguments by index, and apply setter/property assignments after construction.
- Fixture execution consumes constructed DTO/property values through public `ScenarioExecutor` reports rather than private helper assertions.
- Structured blockers include `MISSING_TARGET_TYPE` and assignment materialization failure reasons.

## Verification

- Verified by `ScenarioExecutorSpec` cases for DTO constructor materialization, setter/property assignment, fixture execution with materialized DTO-derived values, and missing target type blockers.
- Verified command: `cd verifiers && mvn test -Dtest='*ScenarioExecutor*Spec' -DfailIfNoTests=false` on 2026-05-26: 10 tests run, 0 failures, 0 errors.

## Blocked by

- Blocked by `issues/scenario-executor-poc/002-first-fixture-execution.md`

## User stories addressed

- User story 16
- User story 18
- User story 19
- User story 28
- User story 49
