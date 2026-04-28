# Scenario Generator Pending Improvements

## Status

Working note for the early `verifiers/` phase.

## Scope

- Saga-only. Ignore TCC for now, both in verifier logic and in `dummyapp` coverage.
- Focus on extraction quality first: service classification, command dispatch mapping, step graph extraction.
- Test analysis is intentionally deferred at this stage. This note is only about the production-code extraction gaps discussed on 2026-04-05.

## Intended Use

Each item below is meant to be implemented through the same loop:

1. Pick a real pattern from `quizzes/`
2. Reduce it to a minimal `dummyapp` fixture
3. Add a verifier spec that asserts the semantic result
4. Change the visitor or shared analysis model

---

# 05-04-2026 

## 1. Workflow Step Dependencies And Ordering

### Status

**Resolved (2026-04-05).** The workflow visitor now records predecessor edges, command multiplicity, looped sends, and compensation footprints in `verifiers/`. The corresponding `dummyapp` fixtures and `WorkflowFunctionalityVisitorSpec` coverage are implemented and passing.

### Problem

The current workflow extraction records step footprints, but not the step graph itself.
For scenario generation this is not enough: valid schedules depend on predecessor constraints, not only on the list of touched aggregates.

The missing pieces are:

- explicit predecessor edges between steps
- whether a step issues one command or several
- whether a step contains looped dispatches
- whether a step registers compensations that should be modeled separately

### Why This Matters

The scenario generator cannot safely prune interleavings if it does not know:

- which steps are allowed to run before others
- which command sends are grouped inside the same step
- when a single step actually touches several aggregate instances

### Implemented Result Summary

- Replaced the old step-footprint model with explicit dispatch modeling:
  - `DispatchPhase` (`FORWARD`, `COMPENSATION`)
  - `DispatchMultiplicity` (`SINGLE`, `STATIC_REPEAT`, `PARAMETRIC_REPEAT`)
  - `StepDispatchFootprint` on each `SagaStepBuildingBlock`
- Added predecessor-edge extraction into `SagaStepBuildingBlock.predecessorStepKeys`.
- Implemented loop-aware multiplicity inference in `WorkflowFunctionalityVisitor`:
  - constant-bounded `for` loops -> `STATIC_REPEAT`
  - runtime-bound loops (`forEach`, `while`, unresolved bounds) -> `PARAMETRIC_REPEAT`
- Implemented compensation extraction from `registerCompensation(...)` lambdas as `COMPENSATION` dispatches.
- Added conservative handling for unresolved dependency references:
  - do not infer an edge
  - emit WARN log

Main code paths changed in `verifiers/`:

- `.../buildingblock/SagaStepBuildingBlock.java`
- `.../buildingblock/StepDispatchFootprint.java`
- `.../buildingblock/DispatchPhase.java`
- `.../buildingblock/DispatchMultiplicity.java`
- `.../buildingblock/DispatchMultiplicityKind.java`
- `.../visitor/WorkflowFunctionalityVisitor.java`
- removed `.../buildingblock/StepFootprint.java`

### Real Patterns Already Present In `quizzes`

- `CreateTournamentFunctionalitySagas` has explicit dependencies and a looped topic-read step
- `SolveQuizFunctionalitySagas` has a looped question-read step
- several sagas register compensations after write steps

### Implemented `dummyapp` Fixtures

- `applications/dummyapp/src/main/java/com/example/dummyapp/item/coordination/CreateItemDependencyGraphFunctionalitySagas.java`
  - explicit dependencies (single predecessor + fan-in)
  - unresolved dependency reference case for conservative handling
- `applications/dummyapp/src/main/java/com/example/dummyapp/item/coordination/CreateItemLoopedReadsFunctionalitySagas.java`
  - static-count loop and runtime-bound loop for multiplicity extraction
- `applications/dummyapp/src/main/java/com/example/dummyapp/item/coordination/CreateItemCompensationFunctionalitySagas.java`
  - forward write + compensation command in `registerCompensation(...)`

### Implemented Specs

- `WorkflowFunctionalityVisitorSpec`
  - `captures predecessor edges between saga steps`
  - `captures fan-in dependencies for a step with two predecessors`
  - `captures repeated command sends with multiplicity`
  - `captures compensation commands separately from forward-step commands`
  - `does not infer predecessors from unresolved dependency references`

### Verification

Ran and passing in `verifiers/`:

- `mvn test -Dtest=WorkflowFunctionalityVisitorSpec,CommandHandlerVisitorSpec,ServiceVisitorSpec`
- `mvn test`

### Known Remaining Gap

- Compensation flows that instantiate only generic `Command`/`SagaCommand` wrappers (without a resolvable concrete `*Command` dispatch mapping) are not fully represented yet by the current extraction model.

---

## 2. Distinguishing Domain Services From Orchestration Facades

### Status

**Resolved (2026-04-06).** The three-phase pipeline (CommandHandlerIndexVisitor → ServiceVisitor → CommandHandlerVisitor) is implemented and passing. `OrderFunctionalitiesFacade` is excluded from `state.services`; all six domain services pass the dispatch-target guard. Tests run: 49, all pass.

Main code paths changed in `verifiers/`:

- `.../state/ApplicationAnalysisState.java` — added `dispatchTargetFqns: Set<String>` (Phase 1 output, Phase 2 input)
- `.../visitor/CommandHandlerIndexVisitor.java` — new Phase 1 visitor; collects concrete service FQNs from CommandHandler injection points (skips interface types)
- `.../visitor/ServiceVisitor.java` — added dispatch-target guard: skips `@Service` classes whose FQN is not in `dispatchTargetFqns` (open when the set is empty, for backward compatibility)

Fixtures added in `applications/dummyapp/`:

- `order/coordination/OrderFunctionalitiesFacade.java` — negative case: `@Service`, injects `SagaUnitOfWorkService`, calls `createUnitOfWork` + `executeWorkflow`; no command handler dispatches to it
- `order/coordination/CreateOrderFunctionalitySagas.java` — minimal saga used by the facade
- `item/commands/AliasWriteItemCommand.java`, `GetterWriteItemCommand.java`, `HelperWriteItemCommand.java` — commands for the three write-detection services
- `item/commandHandler/AliasUnitOfWorkItemCommandHandler.java`, `GetterBasedUnitOfWorkItemCommandHandler.java`, `HelperMarkChangedItemCommandHandler.java` — handlers ensuring the three write-detection services are registered as dispatch targets

Specs updated:

- `ServiceVisitorSpec` — three-phase setup; removed 4 stale `// FAILS:` comments; added `does not classify coordination facade as a domain service` and `classifies command-handler dispatch target as a domain service`
- `CommandHandlerVisitorSpec` — three-phase setup; added `service registry contains only dispatch-target services for dummyapp`

### Problem

A plain `@Service` plus `*UnitOfWorkService` field is not enough to identify a domain service.
This shape also matches coordination facade classes that create unit-of-work objects and launch workflows.

### Non-Arbitrary Distinction Rule

Do not distinguish these classes by package, class name, or suffix.
Instead distinguish them structurally.

Recommended rule:

1. A domain service is a class that is a dispatch target of a `CommandHandler`
2. A domain service method is one that operates inside an already-existing unit of work
3. A coordination facade is a class that creates units of work or instantiates or executes workflow objects

Strong structural signals of a coordination facade:

- calls `createUnitOfWork(...)`
- instantiates `WorkflowFunctionality` subclasses
- calls `executeWorkflow(...)`, `executeUntilStep(...)`, or `resumeWorkflow(...)`
- orchestrates through `CommandGateway` without being a `CommandHandler` dispatch target

Strong structural signals of a domain service:

- is referenced by a `CommandHandler` injected field or constructor parameter
- its public methods are invoked from command-handler branches
- it interacts with aggregates through `UnitOfWorkService`

### Recommended Pipeline Adjustment

The current order is service-first, command-handler-second.
The more reliable model is:

1. collect command-handler dispatch targets
2. classify only those target classes as domain services
3. optionally keep a secondary analysis for coordination facades, but do not mix them with dispatch targets

This is not a naming convention. It uses the actual dispatch structure of the application.

### Proposed `dummyapp` Fixtures

- `applications/dummyapp/src/main/java/com/example/dummyapp/order/service/OrderService.java`
  - keep as the positive domain-service case
- `applications/dummyapp/src/main/java/com/example/dummyapp/order/coordination/OrderFunctionalitiesFacade.java`
  - annotate with `@Service`
  - inject `SagaUnitOfWorkService`
  - call `createUnitOfWork(...)`
  - instantiate `new CreateOrderFunctionalitySagas(...)`
  - call `executeWorkflow(...)`
- `applications/dummyapp/src/main/java/com/example/dummyapp/order/coordination/CreateOrderFunctionalitySagas.java`
  - minimal saga used by the facade

### Proposed Specs

- `ServiceVisitorSpec`
  - `does not classify coordination facade as a domain service`
  - `classifies command-handler dispatch target as a domain service`
  - `service registry contains only dispatch-target services for dummyapp`

---

## 3. CommandHandler Assumptions That Are Too Narrow

### Status

**Resolved (2026-04-05).** All four core sub-problems below have been fixed in `verifiers/`. The proposed fixtures and specs are implemented and passing.

One edge case remains open: when an interface has two or more `@Service` implementations (e.g., one per Spring profile), `CommandHandlerVisitor` emits a WARN log and skips the injection point. Future work could read `application.properties` or `application.yaml` to determine the active profile and use that to break the tie among profile-scoped implementations.

### Problem

The `CommandHandlerVisitor` assumed a narrow implementation style:

1. ~~the service call is a direct field call inside the private handler method~~ — **fixed:** one level of local helper delegation is now supported.
2. ~~`getAggregateTypeName()` returns a string literal directly~~ — **fixed:** `private static final String` constants are now resolved.
3. ~~service injection always uses a concrete class type~~ — **fixed:** interface-typed injection (field and constructor) is now resolved; the single-implementation case resolves to the concrete service; the multiple-implementation case is warned and skipped.
4. ~~service access policy is keyed by method name alone~~ — **fixed:** policies are now keyed by FQN signature (`methodName(FQN1,FQN2,...)`), distinguishing overloaded methods correctly.

### Proposed `dummyapp` Fixtures

All fixtures listed below have been created:

- `applications/dummyapp/src/main/java/com/example/dummyapp/order/service/OrderServiceApi.java`
  - interface implemented by `OrderService`
- `applications/dummyapp/src/main/java/com/example/dummyapp/order/commandHandler/InterfaceInjectedOrderCommandHandler.java`
  - inject `OrderServiceApi`
  - dispatch through the interface type
- `applications/dummyapp/src/main/java/com/example/dummyapp/order/commandHandler/CtorInterfaceInjectedOrderCommandHandler.java`
  - constructor-inject `OrderServiceApi`
  - dispatch through the interface type
- `applications/dummyapp/src/main/java/com/example/dummyapp/shared/service/AmbiguousServiceApi.java`
  - interface with two `@Service` implementations (`AmbiguousServiceImplA`, `AmbiguousServiceImplB`)
- `applications/dummyapp/src/main/java/com/example/dummyapp/shared/commandHandler/AmbiguousCommandHandler.java`
  - injects `AmbiguousServiceApi` — field must be skipped (WARN logged, dispatch empty)
- `applications/dummyapp/src/main/java/com/example/dummyapp/item/commandHandler/DelegatingItemCommandHandler.java`
  - private handler method calls a local helper
  - local helper performs the service call
- `applications/dummyapp/src/main/java/com/example/dummyapp/item/commandHandler/ConstantAggregateTypeItemCommandHandler.java`
  - `getAggregateTypeName()` returns a `private static final String`
- `applications/dummyapp/src/main/java/com/example/dummyapp/item/service/OverloadedItemService.java`
  - two overloads with the same name but different access policies
- `applications/dummyapp/src/main/java/com/example/dummyapp/item/commandHandler/OverloadedItemCommandHandler.java`
  - calls each overload from different commands

### Proposed Specs

All specs listed below have been implemented in `CommandHandlerVisitorSpec`:

- `maps command dispatched through interface field-injected service`
- `maps command dispatched through interface constructor-injected service`
- `skips dispatch when interface has multiple implementations`
- `maps command dispatched through one local helper method`
- `extracts aggregate type name from constant field`
- `distinguishes overloaded service methods by signature`

---

## 4. Service Write-Detection Assumptions That Are Too Narrow

### Status

Fixed in `verifiers/`. The `ServiceVisitor` now recognizes:

- direct `registerChanged(...)` calls on the tracked unit-of-work service
- local aliases of that service
- trivial getters that return it
- one local helper method that wraps the write call

### Problem

The current `ServiceVisitor` used to recognize writes only when a public method directly contained a call shaped like:

- `unitOfWorkService.registerChanged(...)`
- `this.unitOfWorkService.registerChanged(...)`
- `uow.registerChanged(...)`

That misses valid variants where the write is still structurally obvious.

### Proposed Direction

Support at least these local patterns:

- local alias:
  - `var tracker = unitOfWorkService; tracker.registerChanged(...)`
- trivial getter indirection:
  - `getUnitOfWorkService().registerChanged(...)`
- one local helper method that wraps `registerChanged(...)`

The support can remain intentionally shallow:

- only intra-class
- only one level of indirection
- no inter-procedural analysis outside the current class

### Proposed `dummyapp` Fixtures

- `applications/dummyapp/src/main/java/com/example/dummyapp/item/service/AliasUnitOfWorkItemService.java`
  - write method aliases `unitOfWorkService` to a local variable before `registerChanged(...)`
- `applications/dummyapp/src/main/java/com/example/dummyapp/item/service/GetterBasedUnitOfWorkItemService.java`
  - write method uses `getUnitOfWorkService().registerChanged(...)`
- `applications/dummyapp/src/main/java/com/example/dummyapp/item/service/HelperMarkChangedItemService.java`
  - write method calls a private helper that performs `registerChanged(...)`

### Proposed Specs

- `ServiceVisitorSpec`
  - `classifies write through local alias of unit of work service`
  - `classifies write through getter-based unit of work service access`
  - `classifies write through one local helper that wraps registerChanged`

---

## 5. Sagas-Only Boundary For Now

### Decision

Do not add TCC fixtures or TCC visitor work to this backlog unless thesis scope changes explicitly.

### Consequence

- no `dummyapp` TCC mirror classes
- no verifier tests for `CausalWorkflow` or `Step`
- all new backlog items should be phrased in saga terms only

This keeps the early verifier surface smaller while the saga scenario generator is still being stabilized.

---

## 6. Scenario Catalog Export Contract (2026-04-27)

### Status

Implemented: when `verifiers.scenario-catalog.enabled=true`, the verifier now writes a versioned machine export beside the existing HTML report.

### Contract

- `scenario-catalog.jsonl`: one JSON object per `ScenarioPlan`, schema `microservices-simulator.scenario-catalog.v1`.
- `scenario-catalog-manifest.json`: generated-at timestamp, effective config, counts, warnings, and resolved output paths.
- The HTML report remains a separate human view; it is not the machine contract.

### Safe Defaults

- export disabled by default
- `includeSingles=true`
- `maxSagaSetSize=1`
- `maxScenarios=100`
- `maxInputVariantsPerSaga=3`
- `maxSchedulesPerInputTuple=20`
- `allowTypeOnlyFallback=false`
- `inputPolicy=RESOLVED_OR_REPLAYABLE`
- `scheduleStrategy=SERIAL`
- `deterministicSeed=1234`

### Confidence / Current Limits

- Confidence labels are explicit: `EXACT`, `SYMBOLIC`, `TYPE_ONLY`, `UNKNOWN`.
- Exact aggregate-instance keys are still a follow-up; the current adapter stays conservative and does not overstate exact shared-instance matching.
- Missing aggregate names stay skipped/unknown rather than being mislabeled.
- Current configuration assertions are text/YAML-based; follow-up coverage should validate effective bound properties through Spring binding or application wiring.
- ScenarioExecutor execution, behavior CSV generation, and runtime fault injection remain follow-up work; this catalog is only the machine-readable scenario contract.
- Include/exclude filters from the broader brief are not exposed yet; treat them as follow-up configuration work.

### Bounded Smoke Shape

Use the existing verifier entrypoint against Quizzes with catalog export enabled, `maxSagaSetSize=1`, and a small `maxScenarios` (for example 5–10). Verify only these invariants:

- JSONL parses
- manifest exists
- scenario IDs are unique
- each schedule step references known saga/step IDs
- `analysis-report.html` still writes, along with the archived HTML sibling

---

## Suggested Order

1. Workflow step graph extraction
2. Domain-service vs coordination-facade distinction
3. Command-handler interface and helper indirection
4. Service write-detection indirection
5. Overload handling

The first two improve correctness of the scenario model itself.
The others widen syntactic robustness without changing the high-level generator architecture.
