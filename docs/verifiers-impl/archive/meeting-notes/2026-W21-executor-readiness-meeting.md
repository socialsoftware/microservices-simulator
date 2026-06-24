# Thesis meeting notes — 2026-W21 executor readiness

Date: 2026-05-21

Purpose: advisor-facing notes for explaining the current verifier documentation state, the structured input recipe milestone, and why the next reasonable step is a small scenario executor rather than more static catalog work.

## One-sentence update

The verifier now exports deterministic scenario plans with structured, executor-facing input recipes, so the next milestone can be a constrained app-side scenario executor that reconstructs saga inputs and drives `executeUntilStep(...)` in schedule order.

## Meeting framing

The previous milestone connected static scenario plans to runtime evidence through dynamic enrichment and direct `inputVariantId` attribution. That improved the comparable Quizzes sagas-only run from zero exact scenario matches to forty-six exact matches, while reducing ambiguity from forty-four records to three.

This new discussion moves from evidence enrichment toward execution readiness. The important change is that scenario inputs are no longer only human-readable summaries. Accepted catalog inputs can now include structured `inputRecipe` payloads that describe how to rebuild saga constructor arguments.

## What changed recently

- The scenario catalog still exports one deterministic `ScenarioPlan` per JSONL line.
- Each accepted `InputVariant` can now carry an embedded `inputRecipe`.
- The recipe schema is explicit: `microservices-simulator.input-recipe.v1`.
- Recipes contain deterministic fingerprints, readiness flags, blockers, argument indexes, expected types, and recipe trees.
- Recipe nodes cover literals, constructors, DTO assignments, collections, local transforms, helper results, property access, generic call results, placeholders, and unresolved values.
- The verifier distinguishes catalog acceptance from executor readiness: an input can be accepted for static analysis but still blocked for runtime materialization.
- A bounded Quizzes smoke validated the recipe export contract without claiming that all records are executable.
- The current-state documentation now clearly marks `ScenarioExecutor` and runtime materialization as not implemented, but ready to design as the next stage.

## How to explain it to someone who did not code it

The verifier has been building a map of possible saga executions. It reads application code and tests, extracts saga workflows, identifies inputs used by tests, and exports scenario plans.

Until now, the input side was mostly explanatory: the catalog could say where an input came from and summarize constructor arguments, but it did not give a clean machine-readable reconstruction plan.

The new recipe work changes that. The catalog can now say: to create this saga input, build this object, pass these constructor arguments, set these properties, convert this list to a set, read this property, or stop because this value is still unresolved.

That means the catalog is closer to being executable. The next step is not full fault injection yet. The next step is a minimal proof that one executor can read one scenario, materialize executor-ready inputs, instantiate the saga, and call the saga steps in the generated order.

## Even simpler explanation

Before, the verifier could say: here is a possible saga scenario and these are the steps.

Now it can also say: here is how to rebuild the input object used to start that saga.

So the next natural step is to build a small runner that reads one scenario, rebuilds the input, creates the saga, and presses the saga buttons one by one.

## Dumbest useful version

We had a map of journeys.

Now we also have recipes for some of the things needed to start those journeys.

The next step is to try cooking one recipe and walking one journey.

## Documentation map for the meeting

| Need | File |
|---|---|
| Current implementation truth | `docs/verifiers-impl/current-state.md` |
| Recipe schema and readiness semantics | `docs/verifiers-impl/structured-input-recipes.md` |
| Claim safety and thesis evidence | `docs/verifiers-impl/thesis-claims-evidence-map.md` |
| Plain pipeline narrative | `docs/verifiers-impl/verifier-pipeline-plain-explanation.md` |
| How tests become inputs | `docs/verifiers-impl/test-analysis-saga-input-flow.md` |
| Static/dynamic joining overview | `docs/verifiers-impl/dynamic-enrichment-joining-explained.md` |
| Previous W21 runtime attribution note | `docs/verifiers-impl/meeting-notes/2026-W21-thesis-meeting.md` |
| Broader staged plan | `docs/verifiers-impl/roadmap.md` |

## Current implementation state

Implemented:

- static extraction of saga structures, steps, command dispatches, and aggregate footprints;
- Groovy test tracing for saga construction inputs;
- deterministic scenario catalog generation;
- source-mode filtering for saga catalog inputs;
- embedded structured input recipes on scenario input variants;
- dynamic enrichment sidecars;
- runtime `inputVariantId` attribution for unambiguous test + functionality + step matches;
- documentation that separates current truth, claims, rationale, logs, and meeting notes.

Partially implemented:

- dynamic enrichment exactness for local/sagas Quizzes tests;
- recipe coverage for many replay-oriented value shapes;
- aggregate-key extraction and same-feature input disambiguation.

Not implemented yet:

- generic `ScenarioExecutor`;
- runtime saga/functionality materialization from catalog recipes;
- fixture/database state generation;
- behavior CSV generation from scenario records;
- runtime fault injection from generated schedules;
- domain-impact scoring;
- GA search or bandit prioritization.

## Why we are ready for a small executor

The catalog already has the executor-facing structure:

- `ScenarioPlan.sagaInstances()` tells which saga instances exist;
- `ScenarioPlan.inputs()` tells which input variant belongs to each saga instance;
- `InputVariant.inputRecipe()` tells how to reconstruct constructor arguments when executor-ready;
- `ScenarioPlan.expandedSchedule()` tells which step to drive and in which order;
- simulator runtime already exposes `WorkflowFunctionality.executeUntilStep(String, UnitOfWork)`.

This is enough for a narrow vertical slice. The goal is not to support every scenario. The goal is to prove the path from catalog record to actual saga stepping.

## Why we are not ready for a generic executor

The recipe reconstructs saga constructor arguments, not the complete world around the saga.

Quizzes sagas often assume database state created by test setup, such as existing users, topics, quizzes, tournaments, or course executions. If the executor starts from an empty Spring context and database, many otherwise valid scenario records will fail because the domain fixtures do not exist.

Some recipe nodes also require policy decisions before generic replay:

- injectable placeholders can be resolved from Spring beans;
- source-provided placeholders need supplied values or must be skipped;
- call results need safe receiver resolution;
- unresolved values must remain blockers;
- database fixtures need a separate generation or reuse strategy.

## Safe claim for the meeting

We are ready to implement a constrained scenario executor MVP.

We are not ready to claim arbitrary catalog replay.

The first executor should be treated as a smoke path that proves the architecture, not as the final evaluation runner.

## Proposed executor MVP

The minimal executor should:

1. Run inside the target application classpath, not inside the verifier process.
2. Read one accepted `ScenarioPlan` from `scenario-catalog.jsonl`.
3. Only select plans where every involved `inputRecipe.executorReady` is true.
4. Start with single-saga scenarios and serial schedules.
5. Resolve Spring beans such as `SagaUnitOfWorkService` and `CommandGateway` from the application context.
6. Materialize simple recipe shapes: literals, constructors, setters, property assignments, collections, `toSet`, helper-result unwrap, property access, and injectable placeholders.
7. Create the `SagaUnitOfWork` for the saga functionality.
8. Instantiate the saga reflectively with reconstructed constructor arguments.
9. Walk the schedule and call `executeUntilStep(stepName, unitOfWork)`.
10. Print a first observable trace such as `stepNumber: stepName`.

Example first output:

```text
0: getOriginalTournamentStep
1: getTopicsStep
2: updateTournamentStep
```

## Next few steps

1. Decide the executor packaging: generated app-side test, reusable app-side runner, or verifier-generated Java file.
2. Implement a small catalog reader for `ScenarioPlan` JSONL records.
3. Implement a conservative recipe materializer for executor-ready nodes only.
4. Add dummyapp-first coverage with intentionally simple recipes and known steps.
5. Add a Quizzes smoke only after dummyapp proves the shape.
6. Decide whether the executor should call `resumeWorkflow(unitOfWork)` after the final scheduled step.
7. Decide how fixture/domain state should be handled after the first smoke path.
8. After execution works, add behavior/fault injection.
9. After fault injection works, add domain-impact scoring.
10. After scoring works, add search and prioritization.

## Questions for the advisor

1. Is a constrained executor MVP the right next milestone, or should the remaining dynamic-enrichment misses be classified first?
2. Should the first executor consume JSONL directly, or generate application-side test code from JSONL?
3. For the first proof, is it acceptable to skip all non-`executorReady` recipes?
4. Should fixture/database setup be a separate thesis stage, or should the first executor reuse existing test setup where possible?
5. Should the first execution result only print step progression, or also emit dynamic evidence immediately?
6. Is the next thesis claim better framed as "scenario catalog becomes executable" or "first vertical slice from static scenario to runtime stepping"?

## Suggested verbal update

This week I moved the verifier closer to execution by making scenario inputs machine-readable. The catalog already had saga instances and schedules, but now each input can include a structured recipe for rebuilding the saga constructor arguments. The recipe explicitly says whether it is executor-ready and lists blockers when it is not. This means the next reasonable milestone is a small scenario executor, not full fault injection yet: read one executor-ready scenario, reconstruct the inputs, create the saga in the application context, and call `executeUntilStep` for each scheduled step while printing the step number and name. The important limitation is that this reconstructs inputs, not the whole database setup, so the first executor should be a constrained smoke path before claiming generic Quizzes replay.

## Claim safety checklist

- Say the catalog is executor-facing, not already executed.
- Say recipes support future materialization, not full application fixture replay.
- Say the first executor is constrained to executor-ready recipes.
- Say dynamic enrichment remains sidecar-only and does not redefine static scenarios.
- Say Quizzes exact attribution improved materially, but total coverage is still not universal.
- Say execution, fault injection, scoring, GA search, and bandit prioritization remain future stages.
