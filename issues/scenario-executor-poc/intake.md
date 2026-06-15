# Intake

## Goal

Design the first scenario executor POC: a verifier-owned executor that can read an enriched scenario catalog by default, fall back to the static catalog when needed, select or accept a specific scenario id, materialize one supported single-saga scenario, instantiate the target saga reflectively on the target application classpath, execute scheduled steps in catalog order, and emit a small execution report plus console trace.

The first success target is to demonstrate at least one real Quizzes catalog scenario crossing the full path from verifier-generated JSONL to runtime saga stepping.

## Context

The latest executor-readiness meeting note says the verifier now exports deterministic scenario plans with structured input recipes, so the next milestone should be a constrained scenario executor rather than more static catalog work.

Current repo facts:

- Generic `ScenarioExecutor` and runtime materialization are not implemented.
- `ScenarioPlan` records contain saga instances, inputs, input recipes, and expanded schedules.
- Enriched records preserve the embedded static `scenarioPlan`.
- Simulator runtime already exposes `WorkflowFunctionality.executeUntilStep(String, UnitOfWork)`.
- Current Quizzes recipe-aware catalog has zero plans executable under a strict `inputRecipe.executorReady=true` rule.
- With executor-owned runtime argument overrides, the current Quizzes smoke catalog has 15 plausible candidate plans, including 4 for `GetCourseExecutionsFunctionalitySagas`.

## Decisions

- Executor code and orchestration belong to `verifiers`; Quizzes should not be modified for the POC.
- The executor runs target app execution in a subprocess/JVM whose classpath includes the target app plus verifier executor classes.
- The executor reads `scenario-catalog-enriched.jsonl` by default when present and falls back to `scenario-catalog.jsonl`.
- The user can pass `--scenario-id`; if provided, that scenario must exist and be materializable or the run fails clearly.
- Without `--scenario-id`, the executor deterministically selects the first materializable single-saga candidate, using enriched metadata only for candidate prioritization when available.
- The first executor supports only single-saga serial schedules.
- The executor calls `executeUntilStep(stepName, unitOfWork)` once per scheduled step in `scheduleOrder`.
- The executor does not call `resumeWorkflow` in the MVP.
- The executor may supply runtime-owned constructor arguments:
  - `SagaUnitOfWorkService` from Spring
  - `CommandGateway` from Spring
  - `SagaUnitOfWork` created by the executor
- Other unresolved or source-owned values remain blockers.
- Auto-select mode skips unsupported scenarios with structured reasons; explicit scenario mode fails on unsupported selected scenarios.
- The first output is a dedicated execution report and console trace.
- Execution stops on first step failure and reports the failed step and exception.
- Candidate selection and reporting must be deterministic.

## Recommended Defaults Accepted

- Use a verifier-owned forked-JVM/subprocess architecture.
- Make target runtime settings explicit in CLI/config inputs:
  - application base directory
  - Spring app class
  - Maven profile
  - Spring profiles
  - catalog path or run directory
  - optional scenario id
  - output path
- Prefer Quizzes `GetCourseExecutionsFunctionalitySagas` as the first realistic smoke if auto-selection finds it materializable.
- Use dummyapp-first automated tests for reader, selector, materializer, step-name extraction, and reflection behavior.
- Use Quizzes as a focused realistic smoke target with no Quizzes source/test changes.

## Out Of Scope

- Modifying Quizzes source or tests for the POC.
- Fixture/database generation.
- Replaying original Spock setup methods.
- Calling Quizzes helper methods such as `createCourseExecution(...)`.
- Multi-saga scenarios or cross-saga interleavings.
- Fault injection.
- Behavior CSV generation.
- Impact scoring.
- GA search.
- Bandit prioritization.
- Feeding executor-produced runtime evidence back into dynamic enrichment.

## Risks And Tradeoffs

- Running from `verifiers` avoids Quizzes changes but adds classpath, Maven, and Spring-context orchestration complexity.
- Current recipe readiness marks runtime-created `SagaUnitOfWork` as unresolved, so the executor needs a narrowly-scoped runtime-owned argument override.
- Some materializable scenarios may still fail because recipes rebuild constructor arguments, not full database fixture state.
- A one-scenario professor demo proves the execution path, not broad Quizzes replay coverage.
- Using enriched metadata for selection is useful, but the executable contract still comes from the embedded `scenarioPlan`.
