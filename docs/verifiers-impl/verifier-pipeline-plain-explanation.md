# Verifier pipeline, plain explanation

Date: 2026-05-12

Purpose: explain the verifier work in practical engineering terms for a thesis/advisor discussion.

## One-sentence summary

The verifier is being built to turn an application built on the simulator into a set of fault-injection experiments: first by reading the source code and tests, then by checking those static predictions against real runtime evidence.

## What the verifier is trying to do

The final thesis goal is not just to run random faults.

The goal is to generate meaningful fault scenarios such as:

```text
Run this saga.
Stop or delay this step.
Interleave it with another saga that touches the same aggregate.
Observe whether invariants break, compensation fails, or state diverges.
```

To do that safely, the verifier needs to understand the application first.

It needs to know things like:

```text
Which saga/functionality exists?
Which steps does it execute?
Which commands does each step send?
Which aggregates can those commands read or write?
Which test inputs show realistic ways to invoke the saga?
Which exact aggregate instance is touched at runtime?
```

This is why the verifier work has several stages. The hard part is not producing a JSON file. The hard part is making sure the JSON file means something real about the application.

## Current pipeline

The current pipeline has two main parts:

```text
static scenario synthesis
dynamic evidence enrichment
```

Static synthesis reads source code and tests.

Dynamic enrichment runs selected tests and records what actually happened.

Together they answer:

```text
What might happen according to the code?
What did happen in real test executions?
How confidently can we connect the two?
```

## Stage 1: parse application code

The verifier reads the application source tree.

It extracts building blocks such as:

```text
saga/functionality classes
workflow steps
command handlers
domain services
aggregate read/write footprints
```

Example:

```text
FindQuizFunctionalitySagas
  step: findQuizStep
  command: GetQuizByIdCommand
  likely touches: Quiz
```

At this stage, the verifier can often identify aggregate types, but not always exact aggregate ids.

For example, it may know:

```text
findQuizStep reads a Quiz
```

but not yet:

```text
findQuizStep reads Quiz(11)
```

That exact-key problem is one of the main reasons the work is non-trivial.

## Stage 2: parse tests for useful inputs

The verifier also reads Groovy/Spock tests.

It looks for calls that invoke application functionalities. These become `InputVariant`s.

An `InputVariant` is:

```text
a source/test-derived way to invoke a saga/functionality
```

Example from Quizzes:

```text
test class:
  CreateTournamentFaultTest

test method:
  Check Quiz existence

source expression:
  quizFunctionalities.findQuiz(11)

static saga:
  FindQuizFunctionalitySagas

inputVariantId:
  2c98e7...
```

This is not only about the main tested line. Setup and helper calls can also be useful inputs because they describe realistic ways to create data or run functionalities.

That is why static input discovery is careful and broad.

## Stage 3: filter by transactional mode

Quizzes can run in saga mode or causal/TCC mode.

A source call like this is not enough by itself:

```groovy
tournamentFunctionalities.createTournament(...)
```

The same facade can create a saga workflow or a TCC workflow depending on Spring configuration.

So the verifier classifies test-derived inputs by source mode:

```text
SAGAS
TCC
MIXED
UNKNOWN
```

For a saga catalog:

```text
SAGAS inputs are accepted.
TCC and MIXED inputs are rejected from the accepted saga catalog.
UNKNOWN can be accepted with warnings, depending on policy.
```

This matters because otherwise the saga catalog could silently include inputs that came from TCC tests.

## Stage 4: generate a static scenario catalog

After parsing source and tests, the verifier writes:

```text
scenario-catalog.jsonl
scenario-catalog-manifest.json
scenario-catalog-rejected-inputs.jsonl
```

Each scenario record contains:

```text
scenario id
saga instance(s)
input variant(s)
expanded step schedule
fault slots
conflict evidence
warnings
```

For now, the main implemented catalog is single-saga scenario generation.

The catalog is the machine-readable static contract.

It says:

```text
Here are the candidate scenarios that the verifier thinks are relevant and replayable enough to consider later.
```

It does not yet execute those scenarios.

## Stage 5: collect dynamic runtime evidence

Static analysis alone cannot always know exact runtime values.

Example:

```groovy
courseExecutionDto = createCourseExecution(...)
quizFunctionalities.findQuiz(courseExecutionDto.aggregateId)
```

The actual aggregate id may only exist after runtime setup executes.

So the simulator now has an opt-in dynamic evidence bridge.

When enabled, runtime writes events like:

```text
STEP_STARTED
COMMAND_SENT
AGGREGATE_ACCESSED
STEP_FINISHED
```

Example:

```text
test:
  CreateTournamentFaultTest / Check Quiz existence

runtime step:
  findQuizStep

command:
  GetQuizByIdCommand(rootAggregateId=11)

aggregate access:
  READ SagaQuiz(11)
```

This gives concrete evidence that static analysis usually cannot guarantee by itself.

## Stage 6: join static scenarios with runtime evidence

Dynamic enrichment joins:

```text
static ScenarioPlan/InputVariant
runtime dynamic-evidence events
```

The join compares:

```text
test class/method
saga/functionality identity
step name
runtime command evidence
runtime aggregate accesses
```

The output is sidecar-only:

```text
scenario-catalog-enriched.jsonl
scenario-catalog-enriched-manifest.json
dynamic-evidence-join-report.json
```

The original static catalog remains unchanged.

This is intentional. Static output stays reproducible, and dynamic output is additional evidence.

## Join statuses

Each enriched scenario gets a dynamic status:

```text
MATCHED_EXACT
MATCHED_HIGH_CONFIDENCE
MATCHED_PARTIAL
AMBIGUOUS
UNMATCHED
NOT_COVERED
```

Meaning:

```text
MATCHED_EXACT
  Runtime evidence directly names the static inputVariantId.

MATCHED_HIGH_CONFIDENCE
  Runtime evidence strongly lines up with exactly one static input by test, saga/functionality, and step.

MATCHED_PARTIAL
  Runtime evidence lines up semantically, but test identity is incomplete.

AMBIGUOUS
  Runtime evidence is real, but it could match multiple static inputs.

UNMATCHED
  Runtime evidence exists, but it does not match this scenario.

NOT_COVERED
  No relevant dynamic evidence was observed for this scenario.
```

The system can now produce exact matches when runtime evidence directly carries the static `inputVariantId`. Older evidence without direct ids still flows through the high-confidence, partial, ambiguous, unmatched, or not-covered statuses.

## Concrete example

Static analysis finds:

```text
source:
  quizFunctionalities.findQuiz(11)

test:
  CreateTournamentFaultTest / Check Quiz existence

static saga:
  FindQuizFunctionalitySagas

static step:
  findQuizStep

inputVariantId:
  2c98e7...
```

Dynamic analysis observes:

```text
test:
  CreateTournamentFaultTest / Check Quiz existence

runtime step:
  findQuizStep

command:
  GetQuizByIdCommand(rootAggregateId=11)

aggregate:
  READ SagaQuiz(11)
```

The join can say:

```text
This runtime execution supports that static input.
```

Without direct runtime ids, this is `MATCHED_HIGH_CONFIDENCE`.

With input-variant propagation, the runtime event directly includes:

```text
inputVariantId = 2c98e7...
```

Then the status becomes `MATCHED_EXACT` if the id belongs to the scenario plan being enriched.

## Why this is difficult

The difficulty is value binding across layers.

A value may start in a test:

```groovy
quizFunctionalities.findQuiz(11)
```

or it may come from runtime setup:

```groovy
quizDto = createQuiz(...)
quizFunctionalities.findQuiz(quizDto.aggregateId)
```

Then it may flow through:

```text
test helper
application facade
saga constructor
saga field
workflow step
command constructor
command handler
service method
repository/load call
aggregate id
```

Static analysis can follow some of this, but doing it perfectly for all Java/Groovy patterns would be a large interprocedural value-analysis problem.

Dynamic evidence gives a practical shortcut:

```text
Let static analysis identify the interesting candidate.
Let runtime execution show which concrete ids were actually touched.
Join the two conservatively.
```

This is why the work is staged. Each stage removes one kind of uncertainty:

```text
source parsing:
  What workflows and steps exist?

test tracing:
  What realistic inputs exist?

source-mode filtering:
  Are these saga inputs or TCC inputs?

scenario catalog:
  What scenarios can we generate?

dynamic evidence:
  What happened at runtime?

joining:
  Which static scenario does runtime evidence support?

inputVariantId propagation:
  Runtime evidence can directly name the static input in unambiguous cases.
```

## Current validation baseline

Narrow Quizzes dynamic-enrichment smoke:

```text
runStatus = COMPLETE
testClassesSelected = 2
testClassesPassed = 2
dynamicEventsRead = 362
MATCHED_HIGH_CONFIDENCE = 1
UNMATCHED = 199
```

Original full/default sagas-only Quizzes run before runtime `inputVariantId` propagation:

```text
runStatus = PARTIAL
testClassesSelected = 42
testClassesPassed = 40
testClassesFailed = 2
dynamicEventsRead = 18868
MATCHED_EXACT = 0
MATCHED_HIGH_CONFIDENCE = 2
AMBIGUOUS = 44
UNMATCHED = 20
NOT_COVERED = 0
```

Refreshed comparable full/default sagas-only Quizzes run after runtime `inputVariantId` propagation:

```text
runStatus = PARTIAL
testClassesSelected = 42
testClassesPassed = 40
testClassesFailed = 2
dynamicEventsRead = 18868
MATCHED_EXACT = 46
MATCHED_HIGH_CONFIDENCE = 0
MATCHED_PARTIAL = 0
AMBIGUOUS = 3
UNMATCHED = 17
NOT_COVERED = 0
warningCount = 328
```

The main current signal is:

```text
The dynamic bridge works, runtime ids materially improve exactness, and the remaining misses are now small enough to classify manually.
```

## Next improvement

The next improvement is to classify the remaining `AMBIGUOUS=3` and `UNMATCHED=17` records before adding more attribution rules.

Plan:

1. Decide which misses are real static inputs not exercised by the selected Quizzes tests.
2. Decide which misses are joiner limitations where useful runtime evidence was assigned to a neighboring static input.
3. Decide which misses can be resolved by command payloads, aggregate accesses, literal hints, or aggregate keys.
4. Implement the smallest structured refinement that improves those cases without broad name matching.
5. Keep exactness conservative: direct `inputVariantId` wins only when it belongs to the scenario plan being enriched.

Success means:

```text
remaining ambiguous/unmatched records are classified
new attribution rules have a concrete target
MATCHED_EXACT does not gain false positives
AMBIGUOUS or UNMATCHED decreases, or the remaining reasons are documented
```

## What is not implemented yet

This is not yet the runtime fault executor.

Still future work:

```text
ScenarioExecutor
runtime materialization of generated inputs
generated step/fault execution
impact scoring
genetic/local search
scenario prioritization
distributed runtime parity
TCC runtime parity
stream/gRPC evidence parity
```

The current work is the foundation needed before those stages:

```text
first understand scenarios,
then verify them against real executions,
then execute generated fault experiments.
```

## Meeting framing

A concise way to explain the current state:

> Runtime input attribution is now implemented for the first exact case. On the comparable Quizzes sagas-only run, exact matches increased from zero to forty-six, ambiguity dropped from forty-four to three, and warning volume dropped from over eight thousand to three hundred twenty-eight. The next task is to classify the remaining ambiguous and unmatched records before adding more attribution heuristics.
