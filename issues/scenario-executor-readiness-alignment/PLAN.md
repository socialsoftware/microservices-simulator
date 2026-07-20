# Scenario Executor Readiness Alignment Plan

## Goal

Make `scenario-space-accounting.json` answer the question users actually need:

```text
How many accepted input variants / scenario plans can the current ScenarioExecutor materialize and run?
```

Today the accounting field named `executorReadiness` is misleading. It counts only input variants whose root `inputRecipe.executorReady=true`. The ScenarioExecutor POC can still run some inputs when blocked recipe subtrees belong to runtime-owned arguments that the executor supplies itself, such as:

- `pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService`
- `pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWork`
- `pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway`

Concrete observed mismatch:

```text
scenario-space-accounting.json:
  executorReadiness.executorReadyInputVariantCount = 0

ScenarioExecutor POC:
  successfully executed generated Quizzes scenario
  2f0c64a371fcd65b5a38f294ccbda93a42df060c3d1e5b7dcedf43568abcf661
  GetCourseExecutionsFunctionalitySagas#getCourseExecutionsStep
```

The selected input's root recipe was not `executorReady` because it contained an unresolved `functionalityName` inside the `SagaUnitOfWork` creation trace. The executor still ran it because `SagaUnitOfWork` is runtime-owned and created directly by the executor.

## Required Outcome

After this change, accounting must distinguish these concepts:

1. **Static recipe readiness**: the recipe tree says it is fully ready without executor-specific runtime overrides.
2. **ScenarioExecutor materializability**: the current ScenarioExecutor can materialize the input using its supported recipe semantics plus runtime-owned argument resolution.

The primary accounting answer should be ScenarioExecutor materializability, not raw recipe readiness.

For current Quizzes catalogs that contain the known `GetCourseExecutionsFunctionalitySagas` POC scenario, the new ScenarioExecutor materializability count must be greater than zero even if static recipe-ready count remains zero.

## Non-goals

Do not implement generic fixture/setup replay.

Do not implement new supported recipe node kinds unless needed to remove duplicated readiness logic. This task is about aligning the readiness metric with existing executor materialization semantics.

Do not broaden ScenarioExecutor runtime execution to multi-saga scenarios, fault injection, behavior CSV generation, impact scoring, GA search, or bandit prioritization.

Do not change Quizzes source or tests.

Do not claim that `inputRecipe.executorReady=false` means a scenario cannot run. That is the bug this plan fixes.

## Current Code To Inspect

Executor materialization logic:

```text
verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioMaterializer.java
verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java
```

Current accounting logic:

```text
verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/accounting/ScenarioSpaceAccountingCalculator.java
verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/accounting/ScenarioSpaceAccountingReport.java
```

Recipe mapping/readiness logic:

```text
verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/adapter/InputRecipeMapper.java
verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/InputRecipe.java
verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/InputRecipeArgument.java
verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/InputRecipeNode.java
```

Relevant tests to inspect/update:

```text
verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy
verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/accounting/ScenarioSpaceAccountingCalculatorSpec.groovy
verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/accounting/DummyappAccountingFixtureFoundationSpec.groovy
```

Docs to update:

```text
docs/verifiers-impl/current-state.md
docs/verifiers-impl/evidence.md
docs/verifiers-impl/advisor-brief.md
docs/verifiers-impl/reference/scenario-executor-poc.md
```

## Design Requirements

### 1. Single source of truth for executor materializability

Do not leave accounting and executor with separate definitions of materializability.

Introduce a reusable, context-free evaluator for the materializer's supported semantics. Suggested shape:

```java
public final class ScenarioExecutorReadinessEvaluator {
    public Readiness evaluate(InputVariant input);

    public record Readiness(
        boolean materializable,
        boolean staticRecipeReady,
        List<String> blockers,
        List<String> runtimeOwnedResolutions
    ) {}
}
```

The exact class/record names may differ, but the behavior must be equivalent.

This evaluator must be used by accounting. Prefer also using it from `ScenarioMaterializer` or sharing a helper/policy with `ScenarioMaterializer`, so future executor changes do not drift from the metric again.

### 2. Runtime-owned arguments must override argument readiness

The evaluator must mirror this existing `ScenarioMaterializer` behavior:

```java
if (isRuntimeOwned(argument.expectedTypeFqn())) {
    // materializable by executor even if argument.executorReady() is false
}
```

Runtime-owned types are currently:

```text
pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService
pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWork
pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
```

For these arguments, do not count child recipe blockers such as unresolved `functionalityName` as executor materialization blockers.

### 3. Non-runtime-owned arguments still require supported materialization

For non-runtime-owned arguments, the evaluator must reject the same unsupported cases that `ScenarioMaterializer` would reject.

At minimum, preserve existing support/rejection semantics:

Supported node kinds:

```text
literal
placeholder only when runtime-owned
constructor
collection
local_transform only for toSet
helper_result
property_access when receiver materializes
```

Blocked node kinds / cases:

```text
call_result unless runtime-owned argument bypasses it
unresolved
source-provided placeholder without configured value
unsupported transform
missing constructor target type
unmaterializable property receiver
unsupported recipe kind
```

### 4. Accounting should report both metrics

Update `ScenarioSpaceAccountingReport.ExecutorReadiness` or replace it with a clearer record. Required JSON fields:

```json
"executorReadiness": {
  "acceptedInputVariantCount": 0,
  "executorMaterializableInputVariantCount": 0,
  "staticRecipeReadyInputVariantCount": 0,
  "blockedInputVariantCount": 0,
  "blockerReasonCounts": {},
  "runtimeOwnedResolutionCounts": {}
}
```

Field naming can be slightly different only if it remains unambiguous. Avoid `executorReadyInputVariantCount` unless it means actual ScenarioExecutor materializability.

Compatibility guidance:

- It is acceptable to keep the old field `executorReadyInputVariantCount` for one release if it is redefined to equal actual executor materializability.
- If the old field is kept, add `staticRecipeReadyInputVariantCount` so the previous strict recipe metric is still visible.
- Do not keep a field named `executorReadyInputVariantCount` that still means root `inputRecipe.executorReady`.

### 5. Scenario-plan materializability is useful but optional

The minimum required metric is input-variant materializability.

If easy, also add scenario-level materializability counts for accepted/written plans:

```json
"scenarioExecutorReadiness": {
  "acceptedScenarioPlanCount": 0,
  "executorMaterializableScenarioPlanCount": 0,
  "blockedScenarioPlanCount": 0
}
```

A single-saga plan is materializable when its selected input is materializable and its schedule shape is supported by the current executor. Multi-saga plans are not currently supported by the POC and should be counted as blocked/unsupported.

If this optional scenario-level metric becomes invasive, skip it and document that only input-level materializability was corrected.

## Implementation Steps

### Step 1: Extract runtime-owned type policy

Move the runtime-owned type knowledge out of private-only executor code.

Acceptable options:

1. Create package-private/public helper in executor package, e.g. `ScenarioExecutorMaterializationPolicy`.
2. Make `ScenarioMaterializer` expose a context-free `evaluate(InputVariant)` method.
3. Create `ScenarioExecutorReadinessEvaluator` and make both accounting and materializer use common constants/helpers.

Do not duplicate the runtime-owned type list in accounting.

### Step 2: Implement context-free readiness evaluation

The evaluator should walk `InputRecipe` arguments.

Pseudo-code:

```java
Readiness evaluate(InputVariant input) {
    if input == null or input.inputRecipe == null:
        return blocked(MISSING_INPUT_RECIPE)

    staticRecipeReady = input.inputRecipe.executorReady()

    for each argument sorted by index:
        if runtimeOwned(argument.expectedTypeFqn):
            record runtimeOwnedResolution(argument.expectedTypeFqn)
            continue

        if !argument.executorReady:
            // Do not blindly return UNRESOLVED_ARGUMENT if a more specific node blocker is available.
            // Prefer blockers from argument.blockers or node.blockers.
            collect blockers
            continue

        evaluate node recursively with same support rules as ScenarioMaterializer

    materializable = no blockers
}
```

Blocker counting must be deterministic. Sort blocker keys in emitted JSON as current accounting does.

### Step 3: Update accounting to use the evaluator

Edit `ScenarioSpaceAccountingCalculator.executorReadiness(...)`.

Current wrong behavior:

```java
if (input.inputRecipe() != null && input.inputRecipe().executorReady()) {
    ready++;
}
input.inputRecipe().blockers().forEach(...)
```

Required behavior:

```java
Readiness readiness = evaluator.evaluate(input);
if (readiness.materializable()) materializable++;
if (readiness.staticRecipeReady()) staticRecipeReady++;
readiness.blockers().forEach(...);
readiness.runtimeOwnedResolutions().forEach(...);
```

Ensure the reported `blockedInputVariantCount` means:

```text
acceptedInputVariantCount - executorMaterializableInputVariantCount
```

not:

```text
acceptedInputVariantCount - staticRecipeReadyInputVariantCount
```

### Step 4: Update tests

Add focused unit coverage before touching Quizzes-scale runs.

Required tests:

1. **Runtime-owned blocked recipe becomes materializable**
   - Build an `InputVariant` whose root recipe is `executorReady=false` with blocker `UNRESOLVED_VARIABLE`.
   - Include arguments for only runtime-owned types, including `SagaUnitOfWork` whose recipe contains an unresolved child.
   - Assert evaluator returns:
     - `materializable=true`
     - `staticRecipeReady=false`
     - no materialization blockers
     - runtime-owned resolution counts/list includes all runtime-owned types.

2. **Non-runtime-owned blocked argument remains blocked**
   - Build an input with a non-runtime-owned `Integer` or DTO argument that is not executor-ready.
   - Assert evaluator returns `materializable=false` and a deterministic blocker.

3. **Accounting uses executor materializability**
   - In `ScenarioSpaceAccountingCalculatorSpec`, include at least one runtime-owned-only materializable input and one blocked non-runtime-owned input.
   - Assert:
     - accepted count includes both;
     - executor materializable count is 1;
     - static recipe ready count may be 0 or as fixture dictates;
     - blocked count is accepted minus materializable;
     - runtime-owned resolution counts are present.

4. **ScenarioExecutor behavior remains unchanged**
   - Existing `ScenarioExecutorSpec` should still pass.
   - Add a regression if needed proving explicit execution of a runtime-owned-only input still succeeds.

### Step 5: Validate against an existing Quizzes catalog

Use an existing target catalog if present:

```text
verifiers/target/structured-input-recipes-quizzes-smoke/quizzes-20260520-175058-455/scenario-catalog.jsonl
```

Known scenario id:

```text
2f0c64a371fcd65b5a38f294ccbda93a42df060c3d1e5b7dcedf43568abcf661
```

Expected after fix:

- Static recipe-ready count may remain zero.
- ScenarioExecutor materializable input count must be greater than zero for catalogs containing runtime-owned-only executable inputs.
- The known `GetCourseExecutionsFunctionalitySagas` input should be classified materializable by the new evaluator.

If no suitable target artifact exists in the implementation environment, create a small test fixture instead and document that Quizzes validation was not run.

### Step 6: Update documentation

Update docs to remove the current misleading implication.

Required language in `advisor-brief.md` and `evidence.md`:

```text
The ScenarioExecutor POC executed one generated Quizzes single-saga plan by resolving runtime-owned infrastructure arguments itself. Older accounting that reported zero executor-ready inputs was measuring static recipe readiness only; executor materializability is now reported separately/aligned with ScenarioExecutor semantics.
```

Update `reference/scenario-executor-poc.md` to explicitly explain:

- Runtime-owned arguments are part of executor materialization semantics.
- Static `inputRecipe.executorReady=false` does not automatically mean the ScenarioExecutor cannot run the input.
- The accounting metric now distinguishes static recipe readiness from executor materializability.

Update `current-state.md` if the implementation changes reported metrics or next priorities.

## Validation Commands

Run focused tests from `verifiers/`:

```bash
mvn test -Dtest=ScenarioExecutorSpec,ScenarioSpaceAccountingCalculatorSpec -DfailIfNoTests=false
```

If new evaluator tests live in a separate spec, include that spec explicitly:

```bash
mvn test -Dtest=ScenarioExecutorReadinessEvaluatorSpec,ScenarioExecutorSpec,ScenarioSpaceAccountingCalculatorSpec -DfailIfNoTests=false
```

If accounting integration fixtures are touched:

```bash
mvn test -Dtest=DummyappAccountingFixtureFoundationSpec,ScenarioGeneratorSpec,ScenarioSpaceAccountingCalculatorSpec -DfailIfNoTests=false
```

Only run broader Maven tests if the focused tests expose cross-cutting model serialization changes.

## Acceptance Criteria

- Accounting no longer defines executor readiness as only `inputRecipe.executorReady`.
- A shared evaluator/policy defines ScenarioExecutor materializability, including runtime-owned argument resolution.
- Runtime-owned blocked recipe internals do not count as executor materialization blockers.
- Non-runtime-owned blocked inputs remain blocked.
- `scenario-space-accounting.json` reports both:
  - executor materializable input count;
  - static recipe-ready input count.
- Existing ScenarioExecutor POC behavior remains unchanged.
- Focused tests pass.
- Docs clearly explain the difference between static recipe readiness and ScenarioExecutor materializability.
- The known Quizzes POC scenario is no longer contradicted by the accounting terminology/metric when a catalog containing it is analyzed.

## Suggested Commit Message

```text
Align executor readiness accounting with materializer semantics
```
