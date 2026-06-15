## Parent PRD

`issues/scenario-executor-poc/prd.md`

## Type

AFK

## What to build

Add the first executable tracer bullet using a dummy or synthetic fixture runtime. Starting from a selected structurally eligible single-saga plan, materialize the minimal supported constructor arguments, reflectively instantiate a fixture `WorkflowFunctionality`, call `executeUntilStep` for each scheduled step in catalog order, and write an execution report plus console trace for a successful run.

This slice should prove the full catalog-to-step path without depending on Quizzes runtime startup.

## Acceptance criteria

- [x] A fixture single-saga plan with ready literal arguments can be selected and materialized.
- [x] Injectable placeholders for `SagaUnitOfWorkService` and `CommandGateway` can be resolved from a runtime context abstraction.
- [x] A constructor argument with expected type `SagaUnitOfWork` can be supplied as an executor-owned runtime argument.
- [x] A fixture `WorkflowFunctionality` can be instantiated reflectively without compile-time fixture-specific imports in the executor path.
- [x] Scheduled steps are executed by calling `executeUntilStep` in ascending `scheduleOrder`.
- [x] `resumeWorkflow` is not called.
- [x] The execution report includes schema version, selected scenario plan id, selection mode/reason, saga/input identifiers, step outcomes, skipped-candidate counts, and terminal status.
- [x] The console trace includes the selected scenario plan id and each attempted scheduled step.
- [x] Repeating the same fixture run produces stable selected ids, step order, skipped counts, and report semantics.

## Feature criteria covered

- AC-006
- AC-010
- AC-011
- AC-012
- AC-013
- AC-016
- AC-017
- AC-019
- AC-020
- AC-023
- AC-026

## Domain context

Use `inputRecipe` readiness and runtime-owned argument terminology from `docs/verifiers-impl/structured-input-recipes.md`. Runtime-owned overrides are limited to the PRD-approved types and must not make arbitrary unresolved values executable.

## Verification plan

- Run focused verifier tests that exercise the public fixture execution path and assert the execution report/console trace rather than private helper calls.
- Suggested command: `cd verifiers && mvn test -Dtest='*ScenarioExecutor*Spec,*Execution*Spec' -DfailIfNoTests=false`

## Completion evidence

- Implemented executable `ScenarioExecutor` path using `ScenarioRuntimeContext`, `ScenarioMaterializer`, reflective saga/functionality construction, scheduled `executeUntilStep` invocation, report writing, and console-trace support in `ScenarioExecutorCli`.
- Added test fixture workflow classes under verifier tests without compile-time fixture-specific imports in the executor path.
- Verified literal argument materialization, runtime-owned `SagaUnitOfWorkService`, `CommandGateway`, and `SagaUnitOfWork` handling, ascending `scheduleOrder` execution, no `resumeWorkflow` calls, deterministic reports, and output file writing.

## Verification

- Verified by `cd verifiers && mvn test -Dtest='*ScenarioExecutor*Spec' -DfailIfNoTests=false` on 2026-05-26: 10 tests run, 0 failures, 0 errors.

## Blocked by

- Blocked by `issues/scenario-executor-poc/001-catalog-reader-dry-run.md`

## User stories addressed

- User story 1
- User story 8
- User story 9
- User story 15
- User story 16
- User story 17
- User story 24
- User story 25
- User story 26
- User story 27
- User story 33
- User story 35
- User story 36
- User story 37
- User story 38
- User story 39
- User story 40
- User story 41
- User story 42
- User story 43
- User story 45
- User story 47
- User story 48
