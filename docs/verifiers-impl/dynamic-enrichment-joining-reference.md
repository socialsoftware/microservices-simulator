# Dynamic enrichment joining detailed reference

This is the preserved detailed reference for dynamic-enrichment joining. For the shorter first-read path, start with [`dynamic-enrichment-joining-explained.md`](dynamic-enrichment-joining-explained.md). For shared terminology used here, including `InputVariant`, dynamic input map, and join statuses, see [`glossary.md`](glossary.md).

Date: 2026-05-17

Purpose: explain how the verifier connects static scenario-catalog data to simulator runtime evidence, why this is hard, what the current join statuses mean, and where the remaining ambiguous or bad results probably come from.

This document is intentionally more detailed than a meeting summary. It is meant to help thesis writing and future design review.

## Short version

The verifier currently has two complementary views of the application.

```text
static verifier:
  reads Java and Groovy source code
  builds SagaDefinition, InputVariant, ScenarioPlan
  predicts which saga steps and aggregate types matter

dynamic enrichment:
  runs selected application tests
  asks the simulator to emit structured JSONL events
  records the concrete commands, steps, aggregate ids, and test identity observed at runtime

join:
  takes static ScenarioPlan records and runtime DynamicEvidenceEvent records
  decides which static plans are supported by runtime evidence
  writes sidecar enriched catalog artifacts
```

The key idea is this:

```text
static analysis says:
  this Spock test appears to invoke CreateItemFunctionalitySagas with this DTO

dynamic evidence says:
  when that Spock test ran, getOrderStep sent GetOrderCommand(rootAggregateId=13)
  and createItemStep sent CreateItemCommand and wrote Item(41)

join says:
  these runtime facts belong to this static input/scenario, exactly or with some uncertainty
```

The join is not the scenario executor. It does not run generated fault schedules. It enriches the static catalog with observed runtime evidence so we can understand whether static scenario generation is aligned with real application executions.

## Why this exists

Static analysis is good at structure.

It can find:

```text
saga class
steps
step dependencies
commands sent by each step
command handler dispatches
service read/write policy
test-source calls that invoke a saga
```

But static analysis is weak at concrete runtime identity.

It often knows:

```text
getOrderStep reads Order
createItemStep writes Item
```

It often does not know with enough confidence:

```text
getOrderStep reads Order(13)
createItemStep writes Item(41)
```

Dynamic evidence complements that by observing the simulator while selected tests run. It can record:

```text
test class and method
runtime saga/functionality class
runtime step name
command type and rootAggregateId
aggregate access type and aggregateId
success/error outcome
```

The research problem in this area is the correlation problem:

```text
Which static InputVariant and ScenarioPlan does this runtime event actually belong to?
```

That is why the current work has focused on dynamic enrichment and input attribution before moving to the executor.

## Running example

Use a simplified version of `applications/dummyapp/` because it is intentionally small and built to expose verifier assumptions.

The shape is:

```text
ItemFunctionalitiesFacade.createItem(dto)
  -> creates SagaUnitOfWork
  -> creates CreateItemFunctionalitySagas
  -> executes workflow

CreateItemFunctionalitySagas
  getOrderStep
    -> sends GetOrderCommand(orderId)
  createItemStep, after getOrderStep
    -> sends CreateItemCommand(itemDto)

OrderCommandHandler
  GetOrderCommand -> OrderService.getOrder(...) -> READ Order

ItemCommandHandler
  CreateItemCommand -> ItemService.createItem(...) -> WRITE Item

Spock test
  def dto = new ItemDto(aggregateId: 41, orderId: 13)
  itemFunctionalities.createItem(dto)
```

This gives the verifier enough material to demonstrate the whole pipeline:

```text
Java production code gives the saga graph and service access policies.
Groovy test code gives the source-level invocation input.
Runtime evidence gives the concrete command and aggregate ids seen during execution.
The join connects those views.
```

## Static side, step by step

The static pipeline is orchestrated by `ScenarioGeneratorApplication.run()`.

### 1. Resolve configured application

Docker config for the main verifier service sets:

```text
VERIFIERS_APPLICATIONS_ROOT=/applications
VERIFIERS_APPLICATION_BASE_DIR=quizzes
VERIFIERS_OUTPUT_ROOT=/reports
SPRING_PROFILES_ACTIVE=sagas,local
```

For dummyapp tests, the application base directory is `dummyapp`.

The app path becomes:

```text
/applications/quizzes
```

or, in local fixture tests:

```text
applications/dummyapp
```

### 2. Configure JavaParser symbol solving

`ScenarioGeneratorApplication.configureSymbolSolver()` configures JavaParser with:

```text
Java 21 language level
ReflectionTypeSolver
ClassLoaderTypeSolver
JavaParserTypeSolver for every src/main/java under the configured app
```

This matters because the verifier asks type questions such as:

```text
Is this class a CommandHandler?
Is this object creation a SagaStep?
What is the FQN of this command type?
Does this service field implement this interface?
```

Without type resolution, the verifier would fall back to fragile name matching.

### 3. Discover source files

`ApplicationsFileTreeParser` walks the configured application tree and records:

```text
src/main/java/**/*.java   -> FQN to path map
src/test/groovy/**/*.groovy -> FQN to path map
```

It does not compile the application. It builds source indexes for later visitors.

### 4. Index command-handler dispatch targets

`CommandHandlerIndexVisitor` runs first.

It finds classes that subclass simulator `CommandHandler` and records injected service types.

Example:

```java
@Component
public class ItemCommandHandler extends CommandHandler {
    @Autowired
    private ItemService itemService;
}
```

The visitor records:

```text
dispatchTargetFqns:
  com.example.dummyapp.item.service.ItemService
```

If the handler injects an interface, the visitor records the interface separately. A later phase admits it only if exactly one `@Service` implementation exists.

This phase exists to avoid treating every `@Service` in the app as a domain command target. The verifier wants services that are actually called by command handlers.

### 5. Classify domain services as read or write

`ServiceVisitor` analyzes `@Service` classes that are command-handler dispatch targets and that use `UnitOfWorkService`.

For each public non-static method, it records an access policy:

```text
READ
WRITE
```

A method is `WRITE` if the visitor sees a `registerChanged(...)` call through:

```text
the unitOfWorkService field
a constructor-injected unitOfWorkService parameter
a local alias
a trivial getter
a one-level helper method
```

Example:

```java
public ItemDto createItem(ItemDto itemDto, UnitOfWork unitOfWork) {
    Item item = new Item(null, itemDto.getName(), itemDto.getPrice());
    unitOfWorkService.registerChanged(item, unitOfWork);
    return new ItemDto(item);
}
```

Static result:

```text
ItemService.createItem(ItemDto, UnitOfWork) -> WRITE
```

Example read method:

```java
public OrderDto getOrder(Integer orderAggregateId, UnitOfWork unitOfWork) {
    Order order = (Order) uow.aggregateLoadAndRegisterRead(orderAggregateId, unitOfWork);
    return new OrderDto(order);
}
```

Current static result:

```text
OrderService.getOrder(Integer, UnitOfWork) -> READ
```

The static service visitor is intentionally simple: it uses `registerChanged(...)` as the write signal. It does not fully interpret all read APIs, repository methods, or all possible state mutation patterns.

### 6. Map commands to service methods

`CommandHandlerVisitor` analyzes `CommandHandler` subclasses.

It does three main things.

First, it resolves injected service fields:

```text
itemService -> ItemService building block
orderService -> OrderService building block
```

Second, it extracts the aggregate type name from `getAggregateTypeName()`:

```java
@Override
protected String getAggregateTypeName() {
    return "Item";
}
```

Static result:

```text
aggregateName = Item
```

Third, it maps private command handler methods to the first service call inside them.

Example:

```java
private Object handleCreateItem(CreateItemCommand cmd) {
    return itemService.createItem(cmd.getItemDto(), cmd.getUnitOfWork());
}
```

Static result:

```text
CreateItemCommand
  -> ItemService.createItem(ItemDto, UnitOfWork)
  -> accessPolicy WRITE
  -> aggregateName Item
```

For the running example, the verifier can now know:

```text
GetOrderCommand    -> READ  Order
CreateItemCommand  -> WRITE Item
```

### 7. Extract saga steps and dispatch footprints

`WorkflowFunctionalityVisitor` finds classes that:

```text
extend WorkflowFunctionality
declare SagaUnitOfWorkService
```

Then it finds `new SagaStep(...)` expressions.

Example:

```java
SagaStep getOrderStep = new SagaStep("getOrderStep", () -> {
    GetOrderCommand cmd = new GetOrderCommand(unitOfWork, "Order", itemDto.getOrderId());
    commandGateway.send(cmd);
});

SagaStep createItemStep = new SagaStep("createItemStep", () -> {
    CreateItemCommand cmd = new CreateItemCommand(unitOfWork, "Item", itemDto);
    this.itemDto = (ItemDto) commandGateway.send(cmd);
}, new ArrayList<>(Arrays.asList(getOrderStep)));
```

Static result:

```text
CreateItemFunctionalitySagas
  step getOrderStep
    predecessor: none
    dispatch: GetOrderCommand -> Order [READ, FORWARD, SINGLE]

  step createItemStep
    predecessor: getOrderStep
    dispatch: CreateItemCommand -> Item [WRITE, FORWARD, SINGLE]
```

The visitor also handles compensation dispatches registered through `registerCompensation(...)` and tries to infer multiplicity:

```text
SINGLE
STATIC_REPEAT xN
PARAMETRIC_REPEAT
```

The important product here is the step footprint:

```text
this step can read/write this aggregate type
```

At this stage it is usually type-level, not exact-instance-level.

### 8. Extract facade creation sites

`WorkflowFunctionalityCreationSiteVisitor` finds non-private methods outside saga classes that create a saga and execute a workflow.

Example:

```java
public ItemDto createItem(ItemDto itemDto) {
    SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork("createItem");
    CreateItemFunctionalitySagas functionality = new CreateItemFunctionalitySagas(
            sagaUnitOfWorkService,
            itemDto,
            unitOfWork,
            commandGateway);
    functionality.executeWorkflow(unitOfWork);
    return functionality.getItemDto();
}
```

Static result:

```text
ItemFunctionalitiesFacade.createItem(...)
  -> creates CreateItemFunctionalitySagas
  -> constructor arg[0]: field sagaUnitOfWorkService
  -> constructor arg[1]: method parameter itemDto
  -> constructor arg[2]: local unitOfWork
  -> constructor arg[3]: field commandGateway
```

This is what lets a Spock call to `itemFunctionalities.createItem(dto)` become a saga input, even though the test does not directly call `new CreateItemFunctionalitySagas(...)`.

### 9. Parse Groovy test source

`GroovySourceIndex` parses Groovy source under `src/test/groovy` and records:

```text
class metadata
imports
fields
annotations
methods
constructed type names
source-backed superclass relationships
```

This supports Spock-specific tracing without running tests.

### 10. Trace Spock inputs

`GroovyConstructorInputTraceVisitor` processes source-backed Spock `Specification` classes.

It traces:

```text
field initializers
setup()
setupSpec()
feature methods with human-readable Spock names
direct saga constructors
facade calls that map to saga creation sites
helper methods and simple return chains
assignments and bare calls
workflow calls such as executeWorkflow, executeUntilStep, resumeWorkflow
```

For example:

```groovy
def 'creates item'() {
    given:
    def dto = new ItemDto(aggregateId: 41, orderId: 13)

    when:
    itemFunctionalities.createItem(dto)

    then:
    true
}
```

Static trace result:

```text
sourceClassFqn = com.example.dummyapp.GroovySagaTracingSpec
sourceMethodName = creates item
originKind = FACADE_CALL
sourceExpressionText = itemFunctionalities.createItem(dto)
sagaClassFqn = com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas
constructor arguments:
  arg[0]: field sagaUnitOfWorkService [injectable placeholder]
  arg[1]: dto <- new ItemDto(aggregateId: 41, orderId: 13) [resolved/replayable shape]
  arg[2]: local unitOfWork <- sagaUnitOfWorkService.createUnitOfWork("createItem") [runtime call]
  arg[3]: field commandGateway [injectable placeholder]
```

The exact categories depend on the traced expression, but the broad idea is:

```text
resolved values: literals, object construction, local transforms
replayable placeholders: Spring-injected fields, source placeholders, runtime calls with known provenance
partial/unresolved values: expressions whose data flow cannot be safely reconstructed
```

### 11. Classify source mode

`SourceModeClassifier` classifies each Groovy trace as:

```text
SAGAS
TCC
MIXED
UNKNOWN
```

It uses generic Spring/simulator evidence:

```text
@ActiveProfiles("sagas") or @ActiveProfiles("tcc")
spring.profiles.active in @SpringBootTest or @TestPropertySource
@Autowired SagaUnitOfWorkService fields
@Autowired CausalUnitOfWorkService fields
local @TestConfiguration bean evidence
```

For a saga catalog:

```text
SAGAS  -> accepted
UNKNOWN -> accepted with warning by current default policy
TCC    -> rejected and exported diagnostically
MIXED  -> rejected and exported diagnostically
```

This protects the saga catalog from silently absorbing TCC-origin test inputs.

### 12. Adapt to scenario model

`ApplicationAnalysisScenarioModelAdapter` turns `ApplicationAnalysisState` into normalized scenario-generation records.

Main outputs:

```text
SagaDefinition
  sagaFqn
  steps
  warnings

StepDefinition
  deterministicId
  stepKey
  name
  orderIndex
  predecessorStepKeys
  footprints
  warnings

StepFootprint
  aggregateKey
  accessMode
  warnings

InputVariant
  deterministicId
  sagaFqn
  sourceClassFqn
  sourceMethodName
  sourceBindingName
  resolutionStatus
  sourceMode
  stableSourceText
  provenanceText
  constructorArgumentSummaries
  logicalKeyBindings
  warnings
```

For the running example, a simplified static record is:

```text
InputVariant
  id = input-create-item-1
  sagaFqn = com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas
  sourceClassFqn = com.example.dummyapp.GroovySagaTracingSpec
  sourceMethodName = creates item
  stableSourceText = itemFunctionalities.createItem(dto)
  constructorArgumentSummaries = [
    arg[1]: dto <- new ItemDto(aggregateId: 41, orderId: 13),
    arg[2]: unitOfWork <- sagaUnitOfWorkService.createUnitOfWork("createItem") [replayable]
  ]
```

The current adapter creates type-only footprints for dispatch aggregates:

```text
getOrderStep -> AggregateKey(type/name roughly Order, key null, confidence TYPE_ONLY)
createItemStep -> AggregateKey(type/name roughly Item, key null, confidence TYPE_ONLY)
```

That is a major reason dynamic evidence is useful.

### 13. Generate scenario plans

`ScenarioGenerator` takes saga definitions and input variants.

It does this:

```text
normalize and filter InputVariants
reject TCC/MIXED source modes from saga catalog
apply input resolution policy
cap input variants per saga
build conflict graph from step footprints
emit single-saga scenarios first
optionally emit bounded multi-saga scenarios
enumerate schedules
write ScenarioPlan records
```

With current safe defaults:

```text
includeSingles = true
maxSagaSetSize = 1
scheduleStrategy = SERIAL
allowTypeOnlyFallback = false
inputPolicy = RESOLVED_OR_REPLAYABLE
```

So the default catalog is mostly single-saga plans.

For the running example, one static scenario plan looks like:

```text
ScenarioPlan
  kind = SINGLE_SAGA
  inputs = [input-create-item-1]
  sagaInstances = [CreateItemFunctionalitySagas instance bound to input-create-item-1]
  expandedSchedule = [
    getOrderStep,
    createItemStep
  ]
  faultSpace = one fault slot per scheduled step
  conflictEvidence = [] for single-saga
```

The static catalog is written as:

```text
scenario-catalog.jsonl
scenario-catalog-manifest.json
scenario-catalog-rejected-inputs.jsonl
```

## Dynamic enrichment side, step by step

Dynamic enrichment begins only after the static catalog exists.

It is sidecar-only. It does not rewrite `scenario-catalog.jsonl`.

### 1. Select test classes

`DynamicEnrichmentTestClassDiscoveryService` scans the configured test source root, normally:

```text
src/test/groovy
```

The Docker verifier service currently includes Quizzes sagas tests and excludes causal/TCC tests:

```text
include-test-dirs = pt/ulisboa/tecnico/socialsoftware/quizzes/sagas
exclude-test-dirs = pt/ulisboa/tecnico/socialsoftware/quizzes/causal, pt/ulisboa/tecnico/socialsoftware/quizzes/tcc
```

The point is to run only tests relevant to saga dynamic enrichment.

### 2. Write a per-test dynamic input map

Before each selected test class run, `DynamicEnrichmentOrchestrator.runOne(...)` writes:

```text
dynamic-evidence/<safe-test-class-fqn>/dynamic-input-map.json
```

`DynamicInputMapWriter` builds this map from accepted final `ScenarioPlan.inputs()`.

It includes only inputs whose `sourceClassFqn` equals the selected test class.

For our running example:

```json
{
  "schemaVersion": "microservices-simulator.dynamic-input-map.v1",
  "testClassFqn": "com.example.dummyapp.GroovySagaTracingSpec",
  "inputCount": 1,
  "inputs": [
    {
      "inputVariantId": "input-create-item-1",
      "sagaFqn": "com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas",
      "sourceClassFqn": "com.example.dummyapp.GroovySagaTracingSpec",
      "sourceMethodName": "creates item",
      "resolutionStatus": "RESOLVED",
      "sourceMode": "SAGAS",
      "stepNameHints": ["createItemStep", "getOrderStep"],
      "literalArgumentValueHints": [],
      "constructorArgumentSummaries": [
        "arg[1]: dto <- new ItemDto(aggregateId: 41, orderId: 13)"
      ],
      "logicalKeyBindings": {},
      "scenarioPlanIds": ["scenario-create-item-1"],
      "stableSourceText": "itemFunctionalities.createItem(dto)",
      "provenanceText": "..."
    }
  ]
}
```

`literalArgumentValueHints` is intentionally narrow today. It captures simple argument summaries such as `arg[0]: 13` or `arg[2]: label <- "rush"`. It does not yet mine nested DTO fields from a summary such as `new ItemDto(aggregateId: 41, orderId: 13)`.

Current important limitations of this map:

```text
It is built from accepted ScenarioPlans only.
It does not include rejected TCC/MIXED candidates.
It does not include candidates capped out by scenario generation.
It has step-name hints and literal hints, but runtime attribution currently uses only test identity, saga FQN, and step name.
It does not yet include invocation ordinals.
```

### 3. Run the selected test class with simulator evidence enabled

For each selected test class, the orchestrator runs Maven in the application directory.

Shape:

```text
mvn -Ptest-sagas test
  -Dtest=<testClassFqn>
  -Dsimulator.dynamic-evidence.enabled=true
  -Dsimulator.dynamic-evidence.test-context.enabled=true
  -Djunit.platform.listeners.autodetection.enabled=true
  -Dsimulator.dynamic-evidence.output-dir=<evidence-dir>
  -Dsimulator.dynamic-evidence.input-map-path=<evidence-dir>/dynamic-input-map.json
  -Dsimulator.dynamic-evidence.application-name=<application-base-dir>
```

The output directory per class gets:

```text
dynamic-input-map.json
dynamic-evidence.jsonl
dynamic-evidence-manifest.json
test-run.json
maven-output.log
```

The verifier can allow partial test runs. With `allowPartialTestRun=true`, failed test classes do not erase artifacts from classes that already ran.

### 4. Simulator test listener captures test identity

`DynamicEvidenceTestExecutionListener` is discovered by JUnit Platform when listener autodetection is enabled.

At test-plan start it:

```text
creates DynamicEvidenceProperties from system properties
creates DynamicEvidenceJsonlRecorder
sets it in DynamicEvidenceRecorderHolder
```

When an individual test starts, it stores test identity in `DynamicEvidenceTestContext`:

```text
testClassFqn
testMethodName
testDisplayName
testUniqueId
```

For Spock, the display name is important because feature methods often have human-readable names.

Example:

```text
testClassFqn = com.example.dummyapp.GroovySagaTracingSpec
testMethodName = creates item
testDisplayName = creates item
testUniqueId = [engine:spock]/[spec:...]/[feature:$spock_feature_0_0]
```

### 5. Simulator loads the dynamic input map

`DynamicEvidenceJsonlRecorder.initializeInputMap()` loads the file pointed to by:

```text
simulator.dynamic-evidence.input-map-path
```

It stores the map in `DynamicInputAttributionHolder`.

If the file is absent or malformed, attribution is disabled and a warning is recorded. Evidence recording can still continue.

### 6. ExecutionPlan enters a step and tries runtime attribution

When the simulator executes a saga workflow step, `ExecutionPlan.executeInstrumentedStep(...)` calls:

```java
DynamicEvidenceContext.enterStep(
    funcName,
    functionalityClassFqn,
    functionalityClassSimpleName,
    stepName,
    unitOfWorkVersion)
```

Inside `enterStep`, the simulator asks:

```text
DynamicInputAttributionHolder.resolve(functionalityClassFqn, stepName)
```

The current simulator-side matching rule is strict and intentionally conservative.

For a map entry to match, all of this must hold:

```text
current JUnit/Spock test identity exists
map.testClassFqn == current testClassFqn
entry.sourceClassFqn == current testClassFqn
entry.sourceMethodName is blank or equals testMethodName or testDisplayName
entry.sagaFqn == runtime functionalityClassFqn
entry.stepNameHints contains runtime stepName
```

Then:

```text
0 matching entries:
  attribution status = NO_MATCH
  no inputVariantId is attached

1 distinct matching inputVariantId:
  attribution status = MATCHED
  top-level inputVariantId is attached to runtime events

more than 1 distinct matching inputVariantId:
  attribution status = AMBIGUOUS
  candidate ids are recorded in the payload
  no inputVariantId is attached
```

This is the first runtime `inputVariantId` propagation slice.

It is generic simulator infrastructure. It does not require Quizzes-specific hooks.

### 7. Runtime emits step events

`DynamicEvidenceRecorderHolder.recordStepStarted(...)` writes a `STEP_STARTED` event.

The runtime `functionalityName` usually comes from the unit of work, for example `createItem`. If no unit of work name is available, the simulator can fall back to the functionality class simple name. The stronger identity is `functionalityClassFqn`.

Shape:

```json
{
  "schema": "microservices-simulator.dynamic-evidence.v1",
  "eventKind": "STEP_STARTED",
  "testClassFqn": "com.example.dummyapp.GroovySagaTracingSpec",
  "testMethodName": "creates item",
  "functionalityName": "createItem",
  "functionalityClassFqn": "com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas",
  "functionalityClassSimpleName": "CreateItemFunctionalitySagas",
  "inputVariantId": "input-create-item-1",
  "functionalityInvocationId": "createItem-90",
  "stepName": "getOrderStep",
  "unitOfWorkVersion": 90,
  "payload": {
    "stepPhase": "FORWARD",
    "inputVariantAttributionStatus": "MATCHED",
    "inputVariantAttributionBasis": "TEST_FUNCTIONALITY_CLASS_STEP",
    "candidateInputVariantIds": ["input-create-item-1"]
  }
}
```

`STEP_FINISHED` records:

```text
outcome = SUCCESS or ERROR
durationMillis
error type/message if any
```

### 8. Runtime emits command events

`LocalCommandGateway.send(...)` records `COMMAND_SENT` before dispatching the command locally.

For the running example:

```json
{
  "eventKind": "COMMAND_SENT",
  "inputVariantId": "input-create-item-1",
  "functionalityClassFqn": "com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas",
  "stepName": "getOrderStep",
  "payload": {
    "commandType": "GetOrderCommand",
    "commandFqn": "com.example.dummyapp.order.commands.GetOrderCommand",
    "serviceName": "Order",
    "rootAggregateId": "13",
    "fields": {}
  }
}
```

Top-level command evidence always includes:

```text
commandType
commandFqn
serviceName
rootAggregateId
```

Nested command fields are only populated when command-field extraction is enabled. The current orchestrator does not set `simulator.dynamic-evidence.include-command-fields=true` by default.

### 9. Runtime emits aggregate access events

`SagaUnitOfWorkService` records `AGGREGATE_ACCESSED` when saga unit-of-work APIs observe reads/writes.

For a read:

```json
{
  "eventKind": "AGGREGATE_ACCESSED",
  "inputVariantId": "input-create-item-1",
  "stepName": "getOrderStep",
  "payload": {
    "accessMode": "READ",
    "aggregateType": "Order",
    "aggregateId": "13",
    "sourceMethod": "SagaUnitOfWorkService.aggregateLoadAndRegisterRead"
  }
}
```

For a write:

```json
{
  "eventKind": "AGGREGATE_ACCESSED",
  "inputVariantId": "input-create-item-1",
  "stepName": "createItemStep",
  "payload": {
    "accessMode": "WRITE",
    "aggregateType": "Item",
    "aggregateId": "41",
    "sourceMethod": "SagaUnitOfWorkService.registerChanged"
  }
}
```

This is the strongest runtime signal for exact aggregate-instance behavior.

### 10. Verifier reads evidence files

After all selected test classes run, `DynamicEvidenceReader` recursively reads every:

```text
dynamic-evidence.jsonl
```

under the dynamic evidence root.

It produces `DynamicEvidenceEvent` records with:

```text
eventId
eventKind
testClassFqn
testMethodName
testDisplayName
testUniqueId
inputVariantId
functionalityName
functionalityInvocationId
stepName
payload
sourcePath
lineNumber
raw JSON
```

Important current mismatch:

```text
The simulator emits functionalityClassFqn and functionalityClassSimpleName.
The verifier-side DynamicEvidenceEvent currently does not parse those top-level fields.
The raw JSON still contains them, but the joiner currently uses functionalityName instead.
```

This matters for remaining ambiguity because `functionalityClassFqn` is a stronger identity than a normalized simple/functionality name.

## Join algorithm, step by step

`DynamicEvidenceJoiner` receives:

```text
List<ScenarioPlan> from static generation
List<DynamicEvidenceEvent> from dynamic evidence reader
```

It returns:

```text
DynamicEvidenceJoinResult
  records: List<EnrichedScenarioRecord>
  warnings
  dynamicEventsRead
  eventsMissingTestContext
  evidenceFilesRead
```

Each `EnrichedScenarioRecord` contains:

```text
scenarioPlanId
original ScenarioPlan
DynamicEvidenceSummary
```

The summary contains:

```text
joinStatus
matchedInputVariantIds
matchedTestExecutions
observedSteps
observedAggregateAccesses
observedCommands
warnings
```

### Phase 1. Build saga-name lookup

The joiner builds a catalog index from all static saga FQNs.

For a static saga FQN like:

```text
com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas
```

It indexes several observed names:

```text
com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas
CreateItemFunctionalitySagas
CreateItem
createItem
```

This lets runtime evidence with `functionalityName=createItem` match the static class `CreateItemFunctionalitySagas`.

This normalization is useful, but also a source of ambiguity when different packages contain same simple names or when several inputs share the same functionality name.

### Phase 2. Analyze each runtime event

For every event, `analyzeEvent(...)` computes candidate static inputs.

It first asks:

```text
Which static saga FQNs can match event.functionalityName?
```

If there are no candidate saga FQNs or the event has no step name, the event cannot support a plan through semantic matching.

Then it finds candidate inputs from plans where:

```text
the runtime step name matches one static scheduled step name
the plan has a saga matching the runtime functionality name
the plan input saga matches the candidate saga FQN
```

Step matching normalizes static schedule suffixes:

```text
static scheduled step id: CreateItemFunctionalitySagas::getOrderStep#0
normalized static name: getOrderStep
runtime step name: getOrderStep
match: yes
```

It intentionally does not strip suffixes from runtime step names. If runtime says `reserve#0`, that is treated as a different runtime step name.

For each candidate input, the joiner also computes whether test identity matches:

```text
input.sourceClassFqn == event.testClassFqn
input.sourceMethodName == event.testMethodName or event.testDisplayName
```

So an event analysis has:

```text
candidateSagaFqns
candidateInputs
identityMatches
completeTestIdentity = testClassFqn present and method/display present
```

### Phase 3. Enrich each static plan

For each `ScenarioPlan`, the joiner applies this priority order.

#### 1. No dynamic events at all

If there are no event analyses:

```text
status = NOT_COVERED
```

This means dynamic enrichment did not observe any useful runtime evidence for the entire run or evidence root.

#### 2. Direct inputVariantId wins

The joiner collects all events whose top-level `inputVariantId` belongs to one of the plan's input ids.

If any exist:

```text
status = MATCHED_EXACT
matchedInputVariantIds = those direct ids
matchedEvents = those exact events
```

This is the strongest current match.

Example:

```text
plan.inputs = [input-create-item-1]
event.inputVariantId = input-create-item-1

result:
  MATCHED_EXACT
```

Note what exact means here:

```text
The runtime event was directly attributed to the static InputVariant.
```

It does not mean the generated scenario schedule was executed under faults. It means normal test execution produced runtime evidence tied to that input.

#### 3. No relevant events for this plan

If no direct id matched, the joiner checks whether any event analysis is relevant to this plan.

An analysis is relevant when one of its candidate inputs came from the current plan id.

If none are relevant:

```text
status = UNMATCHED
```

This means dynamic evidence existed, but it did not map to this static plan.

#### 4. Relevant events with complete test identity

If relevant events have complete test identity, the joiner uses identity matches.

Cases:

```text
exactly one identity-matched input id, and it belongs to this plan:
  MATCHED_HIGH_CONFIDENCE

exactly one identity-matched input id, but it does not belong to this plan:
  UNMATCHED

more than one identity-matched input id:
  AMBIGUOUS

zero identity-matched input ids:
  UNMATCHED
```

High confidence means:

```text
test class/method matched
saga/functionality name matched
step name matched
only one static input candidate survived
but runtime did not carry a direct inputVariantId for this plan
```

#### 5. Relevant events without complete test identity

If relevant events do not have complete test identity, the joiner falls back to weaker semantic matching.

Cases:

```text
exactly one candidate id, and it belongs to this plan:
  MATCHED_PARTIAL

exactly one candidate id, but it does not belong to this plan:
  UNMATCHED

more than one candidate id:
  AMBIGUOUS

zero candidate ids:
  UNMATCHED
```

Partial means:

```text
saga/functionality and step matched
but test identity was missing or incomplete
```

Partial is useful as a diagnostic but weaker than high confidence.

## Join statuses

| Status | Meaning | Typical cause | Good or bad? |
|---|---|---|---|
| `MATCHED_EXACT` | Runtime event directly carried an `inputVariantId` belonging to the plan. | Dynamic input map resolved exactly one candidate at step entry. | Best current result. |
| `MATCHED_HIGH_CONFIDENCE` | No direct id, but test identity plus saga/functionality plus step resolved to one static input. | Runtime attribution was absent, but semantic matching was unique. | Good, but not exact. |
| `MATCHED_PARTIAL` | Saga/functionality plus step resolved to one candidate, but test identity was missing. | Event occurred outside captured test context, or listener/context did not attach identity. | Useful but weak. |
| `AMBIGUOUS` | Runtime evidence is relevant but maps to multiple candidate input ids. | Repeated same saga/step in same test, duplicate simple names, neighboring inputs with same shape. | Needs investigation. |
| `UNMATCHED` | Dynamic evidence exists, but not for this plan. | Test did not cover the plan, plan was capped/different, or evidence matched another input. | Normal for uncovered static plans, bad if expected coverage. |
| `NOT_COVERED` | No dynamic evidence was available to cover plans. | No evidence files, no selected tests, dynamic run produced nothing. | Bad for enrichment run; expected for empty evidence tests. |

## Concrete example: high confidence vs exact

Assume the static plan has:

```text
ScenarioPlan scenario-create-item-1
  input id = input-create-item-1
  input.sourceClassFqn = com.example.dummyapp.GroovySagaTracingSpec
  input.sourceMethodName = creates item
  input.sagaFqn = com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas
  schedule = [getOrderStep, createItemStep]
```

Runtime evidence without direct attribution:

```text
eventKind = COMMAND_SENT
testClassFqn = com.example.dummyapp.GroovySagaTracingSpec
testMethodName = creates item
functionalityName = createItem
stepName = getOrderStep
payload.commandType = GetOrderCommand
payload.rootAggregateId = 13
```

Join result:

```text
MATCHED_HIGH_CONFIDENCE
matchedInputVariantIds = [input-create-item-1]
observedCommands = [GetOrderCommand rootAggregateId=13]
```

Runtime evidence with direct attribution:

```text
eventKind = COMMAND_SENT
inputVariantId = input-create-item-1
testClassFqn = com.example.dummyapp.GroovySagaTracingSpec
testMethodName = creates item
functionalityClassFqn = com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas
stepName = getOrderStep
payload.commandType = GetOrderCommand
payload.rootAggregateId = 13
```

Join result:

```text
MATCHED_EXACT
matchedInputVariantIds = [input-create-item-1]
observedCommands = [GetOrderCommand rootAggregateId=13]
```

The enriched record still includes the original static scenario plan. Dynamic evidence is attached as a sidecar summary.

## Why ambiguity happens

Ambiguity is not just a bug. Some ambiguity is the correct answer when the available evidence is insufficient.

### Pattern 1. Multiple static inputs in the same test method

A Spock feature can call the same facade or saga several times:

```groovy
def 'same method tracks two order saga instances'() {
    given:
    def firstOrderSaga = new CreateOrderFunctionalitySagas(null, null)
    def secondOrderSaga = new CreateOrderFunctionalitySagas(null, null)

    when:
    firstOrderSaga.executeWorkflow(null)
    secondOrderSaga.executeUntilStep('createOrderStep', null)
}
```

Static analysis may produce two `InputVariant`s with the same:

```text
sourceClassFqn
sourceMethodName
sagaFqn
stepNameHints
```

Runtime evidence may say:

```text
test = same method tracks two order saga instances
functionality = CreateOrderFunctionalitySagas
step = createOrderStep
```

Without invocation order or stronger value matching, this maps to two static inputs.

Correct result:

```text
AMBIGUOUS
```

### Pattern 2. Same saga and step shape appears in neighboring scenario plans

One static input can appear in several scenario plans, especially when multi-saga plans are enabled or when equivalent inputs produce neighboring records.

The current joiner checks exact direct ids first, but if a direct id belongs to a neighboring input rather than the plan being enriched, the event can still be semantically relevant by saga and step.

This can leave a plan ambiguous even though runtime emitted direct ids for related inputs.

This is mentioned in current-state diagnostics for the remaining Quizzes ambiguous records:

```text
runtime emitted direct inputVariantIds,
but the ids belonged to neighboring static input variants with the same test/functionality/step shape,
so the joiner avoided guessing.
```

Potential fix:

```text
When an event has a direct inputVariantId that is not in the current plan,
treat it as negative evidence for that plan unless there is a clear multi-input reason not to.
```

This should be reviewed carefully, but it is a promising low-intrusion refinement.

### Pattern 3. Runtime functionality name is weaker than runtime functionality class FQN

The simulator emits:

```text
functionalityClassFqn
functionalityClassSimpleName
functionalityName
```

The verifier-side event model currently parses:

```text
functionalityName
```

but not:

```text
functionalityClassFqn
functionalityClassSimpleName
```

So fallback matching still relies on name normalization such as:

```text
CreateItemFunctionalitySagas
CreateItem
createItem
```

This is weaker than matching the full class FQN.

Potential fix:

```text
Parse functionalityClassFqn in verifier DynamicEvidenceEvent.
Prefer exact FQN matching when present.
Fall back to current functionalityName normalization only when FQN is absent.
```

This is likely one of the cleanest next fixes.

### Pattern 4. Missing test context

Some events may not have:

```text
testClassFqn
testMethodName
testDisplayName
```

Causes include:

```text
event emitted outside a test method
JUnit listener not active
async execution loses ThreadLocal context
evidence emitted during setup/teardown shape that does not map cleanly
```

Without test identity, the joiner can only produce `MATCHED_PARTIAL` or ambiguity based on saga and step.

### Pattern 5. Async boundaries lose step context

`ExecutionPlan` uses `ThreadLocal` dynamic evidence context.

The code explicitly notes that async continuations completing on a different thread do not currently get full ThreadLocal propagation.

For local/synchronous smoke scenarios this is acceptable. For asynchronous or remote execution, command and aggregate events may lose the current step context.

Symptoms:

```text
COMMAND_SENT without stepName
AGGREGATE_ACCESSED without inputVariantId
eventsMissingTestContext increases
MATCHED_PARTIAL or UNMATCHED increases
```

### Pattern 6. Static inputs are capped or rejected

The dynamic input map is built only from accepted final scenario-plan inputs.

That excludes:

```text
TCC/MIXED rejected inputs
inputs excluded by resolution policy
inputs capped by maxInputVariantsPerSaga
scenario plans capped by maxCatalogScenarios
```

Runtime may observe a real test execution, but if the corresponding input is not in the accepted static catalog, exact attribution cannot map it to a plan.

### Pattern 7. Static aggregate keys are still weak

The current joiner summarizes dynamic aggregate accesses but does not use them deeply to prune candidate inputs.

For example, if static input summaries include:

```text
arg[0]: 13
```

and runtime command evidence says:

```text
rootAggregateId = 13
```

that could help distinguish candidates.

`DynamicInputMapWriter` already extracts simple literal hints, but simulator-side exact attribution does not use them yet, and the joiner does not currently use command/aggregate values as a strong pruning rule.

Potential fix:

```text
Use literalArgumentValueHints and logicalKeyBindings against:
  COMMAND_SENT.payload.rootAggregateId
  COMMAND_SENT.payload.fields.*AggregateId when available
  AGGREGATE_ACCESSED.payload.aggregateId
```

This should be conservative. A value match can increase confidence; a mismatch may or may not be safe as negative evidence depending on which constructor argument it came from.

## Why unmatched happens

`UNMATCHED` means evidence exists, but not for that specific plan.

This can be completely normal.

Example:

```text
static catalog has 100 scenario plans
dynamic run selected tests covering 49 of them
51 plans may be UNMATCHED
```

It becomes suspicious when a plan expected to be covered by the selected tests is unmatched.

Common reasons:

```text
the selected test did not actually execute that saga path
the static test trace came from setup/helper code but runtime evidence happened elsewhere
the runtime functionality name did not match the static saga name
the runtime step name did not match the static scheduled step name
the event had direct inputVariantId for a different plan/input
the static input was accepted but the dynamic input map had no unique candidate
the test failed before the relevant step executed
the event happened in stream/gRPC/distributed mode where instrumentation parity is incomplete
```

## What the enriched sidecars contain

Dynamic enrichment writes:

```text
scenario-catalog-enriched.jsonl
scenario-catalog-enriched-manifest.json
dynamic-evidence-join-report.json
```

Each enriched JSONL line contains:

```text
schemaVersion
scenarioPlanId
scenarioPlan
dynamicEvidence
```

The manifest records status counts:

```text
MATCHED_EXACT
MATCHED_HIGH_CONFIDENCE
MATCHED_PARTIAL
AMBIGUOUS
UNMATCHED
NOT_COVERED
recordCount
warningCount
testRunStatusCounts
```

The join report adds run-level diagnostics:

```text
testClassesSelected
testClassesPassed
testClassesFailed
testClassesTimedOut
evidenceFilesRead
dynamicEventsRead
eventsMissingTestContext
scenarioPlansRead
scenarioPlansEnriched
warnings
```

These counts are currently the main measurement loop for dynamic enrichment quality.

The current documented Quizzes baseline after first-pass runtime input attribution is:

```text
before runtime inputVariantId attribution:
  MATCHED_EXACT = 0
  MATCHED_HIGH_CONFIDENCE = 2
  AMBIGUOUS = 44
  UNMATCHED = 20
  warningCount = 8238

after runtime inputVariantId attribution:
  MATCHED_EXACT = 46
  MATCHED_HIGH_CONFIDENCE = 0
  AMBIGUOUS = 3
  UNMATCHED = 17
  warningCount = 328
```

This is a strong improvement. It also tells us the remaining problem is no longer broad name ambiguity. It is now likely a smaller set of specific attribution edge cases.

## What to inspect next before changing code

Before implementing fixes, classify the remaining bad records.

For each `AMBIGUOUS` record, inspect:

```text
scenarioPlanId
plan.inputs[*].deterministicId
plan.inputs[*].sourceClassFqn
plan.inputs[*].sourceMethodName
plan.inputs[*].stableSourceText
dynamicEvidence.warnings
dynamicEvidence.observedSteps
dynamicEvidence.observedCommands
dynamicEvidence.observedAggregateAccesses
raw dynamic events around sourceEventIds if available
whether raw events had inputVariantId
whether raw events had functionalityClassFqn
```

For each `UNMATCHED` record that should have been covered, inspect:

```text
whether the selected test class ran and passed
whether the static input's source class has a dynamic-input-map entry
whether the map entry had the expected stepNameHints
whether runtime emitted events for the same test method
whether runtime emitted functionalityClassFqn matching input.sagaFqn
whether direct inputVariantId exists but belongs to another input
whether the test failed before the expected step
```

For each `MATCHED_PARTIAL`, inspect:

```text
why test identity was missing
whether it happened outside test context
whether async/thread boundaries are involved
whether JUnit listener properties were set
```

## Likely next improvements, still within dynamic joining

This section intentionally avoids moving to `ScenarioExecutor`.

### Improvement 1. Parse runtime functionalityClassFqn in verifier events

Current mismatch:

```text
simulator emits functionalityClassFqn
verifier reader ignores it except in raw JSON
joiner uses functionalityName
```

Small fix:

```text
Add functionalityClassFqn and functionalityClassSimpleName to verifier DynamicEvidenceEvent.
Teach DynamicEvidenceJoiner to prefer exact FQN matching.
Keep old functionalityName normalization as fallback.
```

Expected effect:

```text
less ambiguity from simple names or decapitalized functionality aliases
better diagnostics when runtime and static class identities disagree
```

### Improvement 2. Treat direct ids as stronger negative evidence

Current behavior:

```text
If event.inputVariantId is in the current plan, MATCHED_EXACT.
If event.inputVariantId is not in the current plan, semantic relevance can still contribute to ambiguity.
```

Possible refinement:

```text
When an event has a direct inputVariantId and that id is not part of a plan,
do not let that event make the plan ambiguous through weaker semantic matching.
```

This matches the meaning of direct attribution:

```text
runtime already told us which static input this event belongs to
```

This should be validated against multi-input plans before implementing.

### Improvement 3. Use command and aggregate ids as pruning evidence

Static side already has:

```text
literalArgumentValueHints
logicalKeyBindings, when available
constructorArgumentSummaries
```

Dynamic side has:

```text
COMMAND_SENT.rootAggregateId
AGGREGATE_ACCESSED.aggregateId
```

Possible refinement:

```text
If multiple candidates match by test/saga/step,
try to rank or prune candidates whose literal/key hints agree with runtime aggregate ids.
```

This would be especially useful for repeated calls in one test method with different aggregate ids.

### Improvement 4. Add invocation ordinals only if value pruning is not enough

If remaining ambiguity is repeated same-saga same-step calls in one feature method, values may not distinguish them.

Then add a generic ordinal:

```text
static:
  source method invokes CreateItemFunctionalitySagas for the first time -> ordinal 1
  source method invokes CreateItemFunctionalitySagas for the second time -> ordinal 2

runtime:
  current test method enters CreateItemFunctionalitySagas -> increment ordinal
```

Matching key becomes:

```text
test identity + functionalityClassFqn + invocationOrdinal + stepName
```

Do not add this until we prove remaining ambiguity is invocation-order ambiguity.

### Improvement 5. Improve per-record run status

Current-state notes say enriched matched execution entries still show `testRunStatus: null` even though join-report per-class status counts exist.

Fixing this would not improve matching, but would make thesis diagnostics clearer.

## What not to overclaim

Dynamic enrichment proves alignment between static scenario inputs and observed normal test executions. It does not prove the generated scenarios have been executed under faults.

`MATCHED_EXACT` means:

```text
runtime evidence carried a direct static inputVariantId that belongs to this plan
```

It does not mean:

```text
the ScenarioPlan expandedSchedule was executed by the future ScenarioExecutor
the faultSpace was exercised
the interleaving was enforced
the impact was measured
```

`UNMATCHED` does not automatically mean static analysis is wrong. It may simply mean selected tests did not cover that plan.

`AMBIGUOUS` is not automatically failure either. It is often the correct conservative result when available evidence cannot distinguish repeated or neighboring inputs.

## Thesis framing

A useful way to describe this milestone:

```text
The verifier now separates static scenario synthesis from dynamic evidence enrichment.
The static catalog is deterministic and reproducible.
The dynamic sidecar validates and strengthens that catalog with observed runtime behavior.
The first runtime input attribution mechanism substantially reduced ambiguity on Quizzes.
The remaining work in this area is not broad instrumentation, but targeted disambiguation of the few cases where static inputs share the same test/functionality/step shape.
```

This gives a clean research narrative:

```text
Stage 1: infer candidate saga scenarios from source and tests.
Stage 1.5: check and enrich those candidates against runtime evidence.
Next dynamic-joining work: reduce residual ambiguity using stronger generic evidence.
Later: execute selected scenarios under faults.
```

## Code references

Main verifier orchestration:

```text
verifiers/src/main/java/.../faults/ScenarioGeneratorApplication.java
```

Static extraction:

```text
ApplicationsFileTreeParser.java
CommandHandlerIndexVisitor.java
ServiceVisitor.java
CommandHandlerVisitor.java
WorkflowFunctionalityVisitor.java
WorkflowFunctionalityCreationSiteVisitor.java
GroovySourceIndex.java
GroovyConstructorInputTraceVisitor.java
SourceModeClassifier.java
ApplicationAnalysisScenarioModelAdapter.java
ScenarioGenerator.java
ScenarioCatalogJsonlWriter.java
```

Verifier dynamic enrichment:

```text
DynamicEnrichmentTestClassDiscoveryService.java
DynamicEnrichmentOrchestrator.java
DynamicInputMapWriter.java
DynamicEvidenceReader.java
DynamicEvidenceJoiner.java
EnrichedScenarioCatalogWriter.java
```

Simulator runtime evidence:

```text
DynamicEvidenceTestExecutionListener.java
DynamicEvidenceJsonlRecorder.java
DynamicInputMap.java
DynamicInputAttributionHolder.java
DynamicEvidenceContext.java
DynamicEvidenceRecorderHolder.java
CommandEvidenceExtractor.java
ExecutionPlan.java
LocalCommandGateway.java
SagaUnitOfWorkService.java
```

Useful tests:

```text
DynamicEvidenceJoinerSpec.groovy
DynamicInputMapWriterSpec.groovy
DynamicEnrichmentOrchestratorSpec.groovy
DummyappDynamicEnrichmentIntegrationSpec.groovy
```
