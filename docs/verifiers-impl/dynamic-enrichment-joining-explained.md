# Dynamic enrichment joining explained

Date: 2026-05-11

Purpose: explain what the static scenario catalog contributes, what dynamic evidence contributes, how the join works, why ambiguity remains, and how to improve matching without adding Quizzes-specific hooks.

## Short version

Static analysis finds candidate saga inputs from source/test code. Dynamic analysis runs selected tests and records what actually happened at runtime. The join step attaches runtime evidence back to static scenario records.

Think of it as:

```text
static analysis:
  "I found a test call that should create/run FindQuizFunctionalitySagas with argument 11."

dynamic analysis:
  "When that test ran, findQuizStep sent GetQuizByIdCommand(11) and read SagaQuiz(11)."

join:
  "These are probably the same scenario input, so enrich that static scenario with the observed command and aggregate access."
```

The current implementation can produce useful `MATCHED_HIGH_CONFIDENCE` matches, but it usually cannot produce `MATCHED_EXACT` yet because runtime events do not directly carry the static `inputVariantId`.

## What static analysis has

The static scenario catalog is built from source and test analysis. Its main unit is a `ScenarioPlan`.

A simplified shape:

```text
ScenarioPlan
  deterministicId
  kind
  sagaInstances
  inputs
  expandedSchedule
  faultSpace
  conflictEvidence
  warnings
```

The most important field for dynamic enrichment is `inputs`.

An `InputVariant` means:

> A concrete source/test-derived way to invoke a saga/functionality.

It is not necessarily a fully concrete JSON payload. Many values still come from runtime setup calls. It is closer to:

```text
InputVariant = replayable/provenance description of a saga invocation
```

Typical static fields:

```text
deterministicId
sagaFqn
sourceClassFqn
sourceMethodName
stableSourceText
provenanceText
constructorArgumentSummaries
logicalKeyBindings
warnings
```

Real Quizzes example from the narrow run:

```text
inputVariantId:
  2c98e7a52ea30c4fa659da56cf235ea3fb96c9f72b5c8d617a33a6cb44327f65

saga:
  pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.FindQuizFunctionalitySagas

source test:
  pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.behaviour.CreateTournamentFaultTest

source method:
  Check Quiz existence

source expression:
  quizFunctionalities.findQuiz(11)

constructor/provenance:
  arg[1]: 11
  resolved via facade QuizFunctionalities.findQuiz(...)

static schedule:
  FindQuizFunctionalitySagas::findQuizStep#0

static warning:
  type-only footprint for Quiz
```

Static analysis knows where the input came from and which saga/step it should exercise. In this example it also sees the literal `11`, but it still records the step footprint as type-only because the static catalog has not yet systematically converted argument provenance into exact aggregate-key bindings.

## What dynamic analysis obtains

Dynamic analysis runs selected application tests with simulator dynamic evidence enabled.

The current simulator event kinds are:

```text
STEP_STARTED
COMMAND_SENT
AGGREGATE_ACCESSED
STEP_FINISHED
```

Each event can include:

```text
testClassFqn
testMethodName
testDisplayName
testUniqueId
functionalityName
functionalityInvocationId
stepName
unitOfWorkVersion
payload
```

For the `FindQuizFunctionalitySagas` example, runtime evidence observed:

```text
testClassFqn:
  pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.behaviour.CreateTournamentFaultTest

testMethodName:
  Check Quiz existence

functionalityName:
  findQuiz

stepName:
  findQuizStep

COMMAND_SENT:
  commandType = GetQuizByIdCommand
  serviceName = quiz
  rootAggregateId = 11

AGGREGATE_ACCESSED:
  accessMode = READ
  aggregateType = SagaQuiz
  aggregateId = 11

STEP_FINISHED:
  outcome = SUCCESS or ERROR depending on the observed execution path
```

This is the key upgrade over static analysis alone:

```text
static:
  "findQuizStep touches a Quiz aggregate."

dynamic:
  "findQuizStep actually sent GetQuizByIdCommand(rootAggregateId=11) and read SagaQuiz(11)."
```

## What the join does

The joiner receives:

```text
List<ScenarioPlan>        from scenario-catalog.jsonl
List<DynamicEvidenceEvent> from dynamic-evidence.jsonl files
```

For each static scenario, it tries to decide whether any runtime events support it.

Current matching signals:

1. Direct `inputVariantId` if present in the event.
2. Test identity:
   - static `InputVariant.sourceClassFqn`
   - static `InputVariant.sourceMethodName`
   - runtime `testClassFqn`
   - runtime `testMethodName` or `testDisplayName`
3. Saga/functionality name:
   - static `FindQuizFunctionalitySagas`
   - runtime `findQuiz`
   - normalized through full name, simple class name, stripped `FunctionalitySagas`, and decapitalized forms.
4. Step name:
   - static `FindQuizFunctionalitySagas::findQuizStep#0`
   - runtime `findQuizStep`
   - the joiner strips the static `#0` schedule suffix.

For the real `FindQuizFunctionalitySagas` example:

```text
static input:
  sourceClassFqn = CreateTournamentFaultTest
  sourceMethodName = Check Quiz existence
  sagaFqn = ...FindQuizFunctionalitySagas
  schedule step = ...::findQuizStep#0

runtime event:
  testClassFqn = CreateTournamentFaultTest
  testMethodName = Check Quiz existence
  functionalityName = findQuiz
  stepName = findQuizStep
  command rootAggregateId = 11
  aggregate access = READ SagaQuiz(11)

join result:
  MATCHED_HIGH_CONFIDENCE
```

The enriched record then contains:

```text
matchedInputVariantIds:
  [2c98e7a52ea30c4fa659da56cf235ea3fb96c9f72b5c8d617a33a6cb44327f65]

observedCommands:
  GetQuizByIdCommand, service=quiz, rootAggregateId=11

observedAggregateAccesses:
  READ SagaQuiz(11)

observedSteps:
  findQuizStep with STEP_STARTED, COMMAND_SENT, AGGREGATE_ACCESSED, STEP_FINISHED
```

## Why this is not `MATCHED_EXACT`

`MATCHED_EXACT` is reserved for the strongest case:

```text
runtime event directly contains:
  inputVariantId = 2c98e7a52ea30c4fa659da56cf235ea3fb96c9f72b5c8d617a33a6cb44327f65
```

That is not implemented yet.

Today, runtime events carry:

```text
test identity
functionality name
step name
command fields
aggregate accesses
```

The joiner infers the match from those facts. That is why the status is `MATCHED_HIGH_CONFIDENCE`, not `MATCHED_EXACT`.

## What `inputVariantId` propagation means

Static analysis already assigns a stable deterministic id to each input:

```text
quizFunctionalities.findQuiz(11)
  -> inputVariantId = 2c98e7a52...
```

But when the test runs normally, the simulator does not know that static id. It only sees ordinary runtime objects, commands, unit-of-work versions, and aggregate accesses.

`inputVariantId` propagation means carrying the static id into runtime evidence, so emitted events look like:

```json
{
  "eventKind": "AGGREGATE_ACCESSED",
  "inputVariantId": "2c98e7a52ea30c4fa659da56cf235ea3fb96c9f72b5c8d617a33a6cb44327f65",
  "functionalityName": "findQuiz",
  "stepName": "findQuizStep",
  "payload": {
    "accessMode": "READ",
    "aggregateType": "SagaQuiz",
    "aggregateId": "11"
  }
}
```

Then the join no longer has to infer the relationship through test/method/name matching:

```text
event.inputVariantId belongs to scenario input id
  -> MATCHED_EXACT
```

## Why ambiguity happens

Ambiguity appears when the runtime evidence is real but not specific enough to choose one static input.

Real pattern from the full Quizzes run:

```text
runtime:
  functionalityName = updateStudentName
  stepName = updateStudentNameStep
  READ SagaExecution(2)
  WRITE SagaExecution(2)

static candidates:
  AddParticipantAndUpdateStudentNameTest / sequential: update; add
  AddParticipantAndUpdateStudentNameTest / sequential: add; update
  AddParticipantAndUpdateStudentNameTest / concurrent: add(1); update; add(2); event
  ...
```

Many tests and feature methods can execute the same saga and same step. If the runtime evidence only says "this was updateStudentName/updateStudentNameStep" and several static inputs fit, the joiner returns:

```text
AMBIGUOUS
candidateInputVariantIds = [id1, id2, id3]
```

This is intentional. The tool should expose uncertainty instead of pretending the evidence is exact.

## Recommended low-intrusion path for better matching

Constraints:

- Do not add Quizzes-specific hooks.
- Avoid touching Quizzes source/tests.
- Avoid broad simulator changes.

Recommended approach:

> Add a verifier-produced runtime mapping file and a small generic simulator/test-listener correlation layer.

The verifier already knows static input variants before launching dynamic tests. For each selected test class, it can write a mapping file into that test class evidence directory:

```json
{
  "schema": "microservices-simulator.dynamic-input-map.v1",
  "testClassFqn": "pt.ulisboa...CreateTournamentFaultTest",
  "inputs": [
    {
      "inputVariantId": "2c98e7a52...",
      "sourceMethodName": "Check Quiz existence",
      "sagaFqn": "pt.ulisboa...FindQuizFunctionalitySagas",
      "observedFunctionalityNames": ["FindQuizFunctionalitySagas", "findQuiz"],
      "stepNames": ["findQuizStep"],
      "stableSourceText": "quizFunctionalities.findQuiz(11)",
      "argumentHints": [
        { "index": 1, "value": "11" }
      ]
    }
  ]
}
```

Then runtime evidence can use this map to attach the best current input candidate.

There are two implementation levels.

### Level 1: verifier-side post-run exactness upgrade

Do not change the simulator.

After reading dynamic evidence, the verifier loads the mapping file and improves the joiner:

- restrict candidates to the current `testClassFqn` and current test method/display name;
- compare normalized saga/functionality names;
- compare step names;
- use argument hints against dynamic command root ids and aggregate ids where available.

This can convert some `AMBIGUOUS` records into `MATCHED_HIGH_CONFIDENCE` without runtime `inputVariantId` in the JSONL.

Pros:

- no simulator change;
- no Quizzes change;
- easiest next step;
- preserves current sidecar-only contract.

Cons:

- still not true `MATCHED_EXACT`;
- repeated identical calls in the same test method may remain ambiguous.

### Level 2: generic test-listener/runtime correlation

Add a small generic extension to the simulator dynamic evidence infrastructure:

- the verifier passes a JVM property such as:

```text
-Dsimulator.dynamic-evidence.input-map-path=<evidence-dir>/dynamic-input-map.json
```

- `DynamicEvidenceTestExecutionListener` loads that map at test-plan start;
- when a test method starts, it stores candidate input variants for the current test identity in a `ThreadLocal`;
- when `ExecutionPlan` enters a functionality/step, the dynamic evidence context tries to select the candidate whose saga/functionality and step names match;
- emitted events include `inputVariantId` when exactly one candidate matches.

This still does not touch Quizzes. It is generic simulator/test infrastructure.

Pros:

- can produce real `MATCHED_EXACT` for unambiguous runtime invocations;
- no Quizzes-specific code;
- small simulator surface area, concentrated in dynamic-evidence infrastructure;
- keeps runtime events self-contained.

Cons:

- needs careful handling when multiple matching saga invocations occur in the same test method;
- may need an invocation counter to distinguish repeated calls.

### Level 3: invocation-order correlation

If Level 2 still leaves ambiguity, add invocation order.

Static map:

```text
test method: "concurrent: add; update"
inputVariantId A -> 1st updateStudentName invocation
inputVariantId B -> 2nd updateStudentName invocation
```

Runtime evidence:

```text
current test method
functionalityName = updateStudentName
functionalityInvocationOrdinal = 1
```

Then matching can use:

```text
test identity + functionality name + invocation ordinal
```

This is still generic and does not require touching Quizzes, but it requires the static analyzer to compute a stable per-method call order and the runtime evidence layer to count functionality entries per test.

## Practical recommendation

Do Level 1 first, then Level 2 only if needed.

Near-term implementation order:

1. Generate a `dynamic-input-map.json` sidecar from existing `ScenarioPlan.inputs()` before running each test class.
2. Teach the joiner to use this map after dynamic evidence is read.
3. Use dynamic command `rootAggregateId` and `AGGREGATE_ACCESSED.aggregateId` to prune candidates when static argument hints are available.
4. Measure whether `AMBIGUOUS=44` decreases on the full Quizzes run.
5. If ambiguity remains due to repeated same-saga calls inside the same method, add generic runtime `inputVariantId` support through the simulator dynamic-evidence test listener and `ExecutionPlan` context.
6. Add invocation-order matching only after confirming repeated-call ambiguity is the dominant remaining cause.

Advisor-facing position:

> I would not add Quizzes hooks. The clean next step is for the verifier to emit a per-test input map from its static catalog and use it to strengthen the dynamic join. If that is not enough, the same map can be consumed by the generic simulator dynamic-evidence listener so runtime events can carry `inputVariantId` directly. This keeps the approach application-agnostic and limits simulator changes to the existing opt-in evidence infrastructure.

