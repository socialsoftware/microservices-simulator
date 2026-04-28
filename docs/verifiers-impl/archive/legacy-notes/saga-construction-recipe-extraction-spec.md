# Handoff Spec

## Goal
Implement a minimal, conservative verifier enhancement that extracts **saga construction recipes** from Groovy tests.

The output must support this future executor flow:
1. pick a discovered saga recipe
2. materialize any unresolved runtime pieces itself
3. instantiate the saga
4. call `executeUntilStep(...)` programmatically

The verifier does **not** need to replay test timelines.

## Design Decisions
- Focus on **construction recipes**, not test replay.
- Keep `executeWorkflow(...)`, `resumeWorkflow(...)`, and `executeUntilStep(...)` as legacy trace/report data only.
- Do not expand event-handler extraction.
- Do not guess "infra" specially. Keep values like `unitOfWorkService`, `commandGateway`, and `createUnitOfWork(...)` as unresolved/runtime placeholders.
- Ignore TCC. Only extract saga-side recipes.
- Stay conservative:
  - follow local Groovy helpers already supported today
  - follow Java functionality/facade creation sites already discoverable from source
  - do not build a general interprocedural call graph

## Required Outcome
After the change, the verifier should produce construction recipes for:
- direct Groovy saga constructors like `new UpdateTournamentFunctionalitySagas(...)`
- facade/functionality calls like `tournamentFunctionalities.createTournament(...)`
- void facade/functionality calls like `courseExecutionFunctionalities.updateStudentName(...)`

It should also stop reporting fake cyclic references for the common helper pattern in `QuizzesSpockTest.groovy`.

## Non-Goals
- replaying full test interleavings
- modeling event-handler calls between saga pauses
- deriving concrete runtime ids statically
- changing `applications/quizzes` production code
- changing `simulator/`

## Current Anchors
- Direct Groovy constructor tracing:
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/GroovyConstructorInputTraceVisitor.java:359-444, 546-780`
- Current Java creation-site mapping:
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/WorkflowFunctionalityCreationSiteVisitor.java:15-46`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/buildingblock/WorkflowFunctionalityCreationSite.java:1-4`
- Real helper cycle pattern:
  - `applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/QuizzesSpockTest.groovy:99-154`
- Real facade creation mapping:
  - `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/coordination/functionalities/TournamentFunctionalities.java:223-237, 295-306`
- Real facade-only Groovy calls:
  - `applications/quizzes/src/test/groovy/.../CreateTournamentTest.groovy:74-100`
  - `applications/quizzes/src/test/groovy/.../UpdateTournamentTest.groovy:100-128`
  - `applications/quizzes/src/test/groovy/.../UpdateStudentNameTest.groovy:32-50`

## Minimal Output Contract
Keep the existing human-readable report behavior, but make the constructor arguments structured.

Recommended minimal model change:
- Extend `GroovyFullTraceResult` with:
  - `GroovyTraceOriginKind originKind`
  - `String sourceExpressionText`
- Extend `GroovyTraceArgument` from:
  - `record GroovyTraceArgument(int index, String provenance)`
- To:
  - `record GroovyTraceArgument(int index, String provenance, GroovyValueRecipe recipe)`

Add:
- `GroovyTraceOriginKind`
  - `DIRECT_CONSTRUCTOR`
  - `FACADE_CALL`
- `GroovyValueRecipe`
  - recursive value recipe used by the executor later
- `GroovyValueKind`
  - `UNRESOLVED_VARIABLE`
  - `UNRESOLVED_RUNTIME_EDGE`
  - `LITERAL`
  - `CONSTRUCTOR`
  - `HELPER_CALL_RESULT`
  - `PROPERTY_ACCESS`
  - `COLLECTION_LITERAL`
  - `LOCAL_TRANSFORM`

Recommended `GroovyValueRecipe` shape:
```java
public record GroovyValueRecipe(
        GroovyValueKind kind,
        String text,
        List<GroovyValueRecipe> children) {
}
```

Keep it intentionally small:
- `text` holds names like `createTournament`, `aggregateId`, `toSet`, `TournamentDto`, `unitOfWorkService`
- `children` holds nested inputs

Examples:
- `unitOfWorkService`
  - `UNRESOLVED_VARIABLE("unitOfWorkService")`
- `unitOfWorkService.createUnitOfWork(functionalityName1)`
  - `UNRESOLVED_RUNTIME_EDGE("unitOfWorkService.createUnitOfWork(functionalityName1)")`
- `new TournamentDto(...)`
  - `CONSTRUCTOR("TournamentDto", [...])`
- `[topic1.getAggregateId(), topic2.getAggregateId()].toSet()`
  - `LOCAL_TRANSFORM("toSet", [COLLECTION_LITERAL("list", [...])])`
- `createTournament(...)`
  - `HELPER_CALL_RESULT("createTournament", [...])`
- `createTournament(...).aggregateId`
  - `PROPERTY_ACCESS("aggregateId", [HELPER_CALL_RESULT(...)])`

## Real Example 1: Fix The Fake Cyclic Reference
Current report line:
- `verifiers/target/analysis-report.html:212`
- `arg[1]: tournamentDto <- createTournament(...) <- tournamentDto [unresolved cyclic reference]`

Real source:
- `QuizzesSpockTest.groovy:147-153`

```groovy
def createTournament(startTime, endTime, numberOfQuestions, userCreatorId, courseExecutionId, topicIds) {
    def tournamentDto = new TournamentDto()
    tournamentDto.setStartTime(DateHandler.toISOString(startTime))
    tournamentDto.setEndTime(DateHandler.toISOString(endTime))
    tournamentDto.setNumberOfQuestions(numberOfQuestions)
    tournamentDto = tournamentFunctionalities.createTournament(userCreatorId, courseExecutionId, topicIds, tournamentDto)
    tournamentDto
}
```

Desired recipe for later use inside:
- `new UpdateTournamentFunctionalitySagas(unitOfWorkService, tournamentDto, topicsAggregateIds, unitOfWork1, commandGateway)`

Should become:
- `arg[1].recipe = HELPER_CALL_RESULT("createTournament", [...])`

If the constructor uses `.aggregateId`, it should become:
- `arg[1].recipe = PROPERTY_ACCESS("aggregateId", [HELPER_CALL_RESULT("createTournament", [...])])`

Do the same for:
- `createUser(...)`
- `createCourseExecution(...)`

## Real Example 2: Facade Call With Assignment
Source:
- `CreateTournamentTest.groovy:76`

```groovy
def result = tournamentFunctionalities.createTournament(
    userCreatorDto.getAggregateId(),
    courseExecutionDto.getAggregateId(),
    [topicDto1.getAggregateId(), topicDto2.getAggregateId()],
    new TournamentDto(...)
)
```

Today:
- no Groovy construction recipe exists for this test

Desired emitted recipe:
- `originKind = FACADE_CALL`
- `sourceExpressionText = tournamentFunctionalities.createTournament(...)`
- `sagaClassFqn = CreateTournamentFunctionalitySagas`
- constructor args:
  - `unitOfWorkService` -> unresolved variable
  - `userCreatorDto.aggregateId` -> property access
  - `courseExecutionDto.aggregateId` -> property access
  - list of topic ids -> collection literal
  - `new TournamentDto(...)` -> constructor
  - `sagaUnitOfWorkService.createUnitOfWork(functionalityName)` -> unresolved runtime edge
  - `commandGateway` -> unresolved variable

This mapping comes from:
- `TournamentFunctionalities.java:223-237`

## Real Example 3: Facade Call Without Assignment
Source:
- `UpdateTournamentTest.groovy:105`

```groovy
tournamentFunctionalities.updateTournament(tournamentDto, topicsAggregateIds)
```

Desired emitted recipe:
- `originKind = FACADE_CALL`
- `sourceBindingName = null`
- `sourceExpressionText = tournamentFunctionalities.updateTournament(...)`
- `sagaClassFqn = UpdateTournamentFunctionalitySagas`
- args mapped from:
  - `TournamentFunctionalities.java:295-306`

This pattern is important because many `quizzes` tests use it.

---

## Implementation Tasks

### Task 1: Upgrade Java Creation-Site Metadata
**Why**
Facade/functionality Groovy calls can only become construction recipes if the Java side tells us how a facade method maps into a saga constructor.

**Modify**
- `verifiers/src/main/java/.../buildingblock/WorkflowFunctionalityCreationSite.java`
- `verifiers/src/main/java/.../visitor/WorkflowFunctionalityCreationSiteVisitor.java`
- `verifiers/src/main/java/.../state/ApplicationAnalysisState.java`
- `verifiers/src/test/groovy/.../visitor/WorkflowFunctionalityCreationSiteVisitorSpec.groovy`

**Add**
- `verifiers/src/main/java/.../buildingblock/WorkflowCreationArgumentSource.java`
- `verifiers/src/main/java/.../buildingblock/WorkflowCreationArgumentSourceKind.java`

**Pattern Anchors**
- `applications/quizzes/.../TournamentFunctionalities.java:223-237`
- `applications/quizzes/.../TournamentFunctionalities.java:295-306`
- `applications/dummyapp/.../OrderFunctionalitiesFacade.java:14-19`

**Implementation Notes**
For each saga `ObjectCreationExpr` already found in a non-saga method, capture constructor argument sources.

Minimal source kinds:
- `METHOD_PARAMETER`
- `LOCAL_VARIABLE`
- `FIELD_REFERENCE`
- `INLINE_EXPRESSION`

Rules:
- if constructor arg resolves directly to a method parameter, store parameter index and name
- if it is a local variable, capture local name and its initializer text if the initializer is a direct declaration in the same method
- if it is a field reference, store field name
- otherwise store the raw Java expression text

Do not try deep Java dataflow. One hop is enough.

**Acceptance Criteria**
- Creation site records still identify the same saga class/method pairs as today.
- They additionally expose per-constructor-arg origins.
- `WorkflowFunctionalityCreationSiteVisitorSpec` asserts at least one mapped parameterized case, not just class/method/saga.

### Task 2: Replace Fake Helper Cycles With Structured Helper Results
**Why**
The current cyclic-reference output is misleading and blocks recipe extraction for common `quizzes` helpers.

**Modify**
- `verifiers/src/main/java/.../state/GroovyTraceArgument.java`
- `verifiers/src/main/java/.../state/GroovyFullTraceResult.java`
- `verifiers/src/main/java/.../visitor/GroovyConstructorInputTraceVisitor.java`
- `verifiers/src/test/groovy/.../visitor/GroovyConstructorInputTraceVisitorSpec.groovy`
- `verifiers/src/test/groovy/.../visitor/GroovyConstructorInputTraceVisitorDummyappSpec.groovy`

**Add**
- `verifiers/src/main/java/.../state/GroovyValueRecipe.java`
- `verifiers/src/main/java/.../state/GroovyValueKind.java`
- `verifiers/src/main/java/.../state/GroovyTraceOriginKind.java`

**Pattern Anchors**
- `QuizzesSpockTest.groovy:99-154`
- `GroovyConstructorInputTraceVisitor.java:662-825`
- `GroovySagaTracingSpec.groovy:183-197`

**Implementation Notes**
Do not throw away the current `provenance` string. Keep it for report readability.

Add a parallel structured recipe builder inside `describeExpressionProvenance(...)` or a sibling method.

Specific fix:
- when a local helper returns a local variable that was rebound to a non-local method call result, emit `HELPER_CALL_RESULT`
- do not recurse back into the same helper-local variable name and label it as cyclic

Example target:
- `createTournament(...)`
- `createUser(...)`
- `createCourseExecution(...)`

Also add a conservative local-transform case:
- recognize `.toSet()` on list/set literals or on locally resolvable collections
- emit `LOCAL_TRANSFORM("toSet", ...)`
- stop classifying this as `unresolved external/runtime edge`

Keep these as unresolved:
- `unitOfWorkService`
- `commandGateway`
- `createUnitOfWork(...)`

**Acceptance Criteria**
- Common `quizzes` helper-derived args no longer render as `unresolved cyclic reference`.
- Direct traces still render readable text.
- Existing dummyapp helper-chain tests still pass.
- Add a regression asserting helper-return output is `HELPER_CALL_RESULT`-based, not cyclic.

### Task 3: Add Facade/Functionality Call Recipe Extraction In Groovy
**Why**
This is the missing bridge from `CreateTournamentTest`, `UpdateTournamentTest`, `UpdateStudentNameTest`, and other facade-only tests into replayable saga construction recipes.

**Modify**
- `verifiers/src/main/java/.../visitor/GroovyConstructorInputTraceVisitor.java`
- `verifiers/src/main/java/.../state/ApplicationAnalysisState.java`
- `verifiers/src/test/groovy/.../visitor/GroovyConstructorInputTraceVisitorDummyappSpec.groovy`
- `verifiers/src/test/groovy/.../faults/ScenarioGeneratorApplicationSpec.groovy`

**Pattern Anchors**
- `GroovyConstructorInputTraceVisitor.java:359-444`
- `CreateTournamentTest.groovy:76-100`
- `UpdateTournamentTest.groovy:105-128`
- `UpdateStudentNameTest.groovy:37-50`

**Implementation Notes**
Do not add a second Groovy visitor. Extend the existing one.

Add two new hook points:
- `handleAssignment(...)`: if RHS is a `MethodCallExpression` and not a direct saga constructor, try resolving a facade recipe
- `traceStatement(...)`: when the statement expression is a `MethodCallExpression`, try resolving a facade recipe before/alongside legacy workflow-call tracing

Facade resolution algorithm:
1. resolve receiver name from the Groovy method call
2. resolve receiver type conservatively from visible source-backed fields and explicit declarations
3. match `(receiverType, methodName)` to the enhanced `WorkflowFunctionalityCreationSite`
4. substitute Java `METHOD_PARAMETER` constructor args with the actual Groovy call arg expressions
5. convert those Groovy expressions into `GroovyValueRecipe`
6. convert Java local/field/inline constructor args into unresolved/runtime recipe nodes with readable text
7. emit a `GroovyFullTraceResult` with `originKind = FACADE_CALL`

Important:
- for `FACADE_CALL`, `sourceBindingName` should be `null`
- use `sourceExpressionText` to render the original Groovy call
- keep `workflowCalls` empty for facade-derived traces

**Acceptance Criteria**
- `CreateTournamentTest` appears in the Groovy trace output after rerunning analysis.
- `UpdateTournamentTest` facade calls produce recipes even without direct saga variables.
- `UpdateStudentNameTest` also produces a recipe.
- Groovy recipe count against `quizzes` increases beyond the current 29.

### Task 4: Extend Dummyapp To Prove The New Surfaces
**Why**
Current dummyapp only proves direct constructor tracing well. It does not prove facade argument mapping.

**Modify**
- `applications/dummyapp/src/test/groovy/com/example/dummyapp/GroovySagaTracingSpec.groovy`
- `verifiers/src/test/groovy/.../visitor/GroovyConstructorInputTraceVisitorDummyappSpec.groovy`
- `verifiers/src/test/groovy/.../visitor/WorkflowFunctionalityCreationSiteVisitorSpec.groovy`
- `verifiers/src/test/groovy/.../report/AnalysisHtmlReportRendererSpec.groovy`

**Add**
- `applications/dummyapp/src/main/java/com/example/dummyapp/item/coordination/ItemFunctionalitiesFacade.java`

**Pattern Anchors**
- `CreateItemFunctionalitySagas.java:22-27`
- `OrderFunctionalitiesFacade.java:14-19`
- `GroovySagaTracingSpec.groovy:82-89, 171-197`

**Implementation Notes**
Add a minimal Java facade around `CreateItemFunctionalitySagas` with one parameterized method:
```java
public ItemDto createItem(ItemDto itemDto) {
    SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork("createItem");
    CreateItemFunctionalitySagas functionality =
            new CreateItemFunctionalitySagas(sagaUnitOfWorkService, itemDto, unitOfWork, commandGateway);
    functionality.executeWorkflow(unitOfWork);
    return functionality.getItemDto();
}
```

Then add Groovy fixture cases for:
- assignment from facade call:
  - `def result = itemFunctionalities.createItem(dto)`
- bare facade call:
  - `itemFunctionalities.createItem(dto)`
- helper returning facade result:
  - a local helper that rebinds a local dto variable to the facade result and returns it

The dummyapp goal is not realism; it is to pin down these exact semantics with the smallest fixture.

**Acceptance Criteria**
- Dummyapp now proves:
  - mapped facade parameter -> saga constructor arg
  - bare facade call -> recipe
  - helper-return facade result -> no fake cyclic reference
  - `.toSet()` still resolves as local transform

### Task 5: Update The HTML Report To Surface The New Recipe Shape
**Why**
The report is the easiest way to validate that the new extraction is actually useful.

**Modify**
- `verifiers/src/main/java/.../report/AnalysisHtmlReportRenderer.java`
- `verifiers/src/test/groovy/.../report/AnalysisHtmlReportRendererSpec.groovy`

**Implementation Notes**
Keep the current report shape, but make these minimal changes:
- add an `Origin` column or equivalent chip:
  - `direct`
  - `facade`
- use `sourceExpressionText` when present
- continue showing the readable `provenance` strings
- unresolved counts should continue to come from argument provenance and notes
- do not add new report sections for workflow-call replay

**Acceptance Criteria**
- Renderer spec still proves unresolved-category drilldown.
- A facade-derived trace renders clearly without pretending there is a saga binding variable.

---

## Verification
Run in `verifiers/`:

1. Focused tests during development:
- `mvn test -Dtest=WorkflowFunctionalityCreationSiteVisitorSpec`
- `mvn test -Dtest=GroovyConstructorInputTraceVisitorDummyappSpec`
- `mvn test -Dtest=GroovyConstructorInputTraceVisitorSpec`
- `mvn test -Dtest=AnalysisHtmlReportRendererSpec`
- `mvn test -Dtest=ScenarioGeneratorApplicationSpec`

2. Full verifier suite before completion:
- `mvn test`

3. Manual report validation:
- rerun the scenario generator path used by `ScenarioGeneratorApplicationSpec`
- inspect `verifiers/target/analysis-report.html`

Manual checks in the generated `quizzes` report:
- `CreateTournamentTest` appears in Groovy traces
- `UpdateTournamentTest` facade call appears in Groovy traces
- `UpdateStudentNameTest` facade call appears in Groovy traces
- helper-derived args no longer show fake `unresolved cyclic reference` for `createUser/createCourseExecution/createTournament`
- unresolved variable/runtime placeholders still remain for runtime-created dependencies

## Do Not Touch
- `simulator/`
- `applications/quizzes/src/main/java/**`
- TCC analysis
- event-handler extraction
- executor implementation

## Short Success Definition
The work is done when the verifier can emit conservative, replay-oriented saga construction recipes from both:
- explicit Groovy `new Saga(...)`
- Groovy calls into `*Functionalities.*(...)`

without relying on test workflow calls, and without mislabeling helper-return patterns as cyclic references.
