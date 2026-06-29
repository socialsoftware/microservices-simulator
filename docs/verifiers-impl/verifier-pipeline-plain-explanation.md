# Verifier pipeline, plain explanation

Last updated: 2026-06-23

Purpose: explain the verifier in meeting/thesis language without implementation archaeology.

## One-sentence summary

The verifier turns simulator application code and tests into candidate saga fault scenarios, then optionally checks those static candidates against runtime evidence from existing test executions.

## Why this exists

The thesis goal is not to run random faults. The goal is to generate meaningful experiments such as:

```text
Run this saga input.
Execute these steps in this order.
Inject a failure/delay at this step.
Observe whether compensation, invariants, or final state break.
```

To do that, the system first needs to understand the application:

- which sagas/functionality classes exist;
- which steps they execute;
- which commands each step sends;
- which aggregate types those commands read or write;
- which test inputs represent realistic saga invocations;
- where runtime evidence confirms the static prediction.

## Current pipeline

```text
source/tests
  -> static extraction
  -> scenario catalog
  -> optional dynamic evidence
  -> enriched sidecar catalog
  -> narrow executor POC / future generic execution
```

## Stage 1: static extraction

The verifier reads Java production code and Groovy/Spock tests.

From production code, it extracts:

- saga/functionality classes;
- ordered workflow steps;
- command handlers;
- domain-service access policies;
- aggregate read/write footprints;
- compensation/forward dispatch phases.

From tests, it extracts input variants:

```text
A static input variant = one observed source/test way to invoke a saga/functionality.
```

Example shape:

```text
test class: CreateTournamentFaultTest
test method: Check Quiz existence
source call: quizFunctionalities.findQuiz(11)
static saga: FindQuizFunctionalitySagas
inputVariantId: input-...
```

Setup/helper calls can also be useful because they show how realistic state is created before the tested functionality.

## Stage 2: source-mode filtering

Quizzes can run similar facades in saga or TCC/causal modes. A call like this is not enough by itself:

```groovy
tournamentFunctionalities.createTournament(...)
```

So the verifier classifies source inputs as:

```text
SAGAS | TCC | MIXED | UNKNOWN
```

Saga catalog policy:

```text
SAGAS   -> accepted
TCC     -> rejected diagnostically
MIXED   -> rejected diagnostically
UNKNOWN -> accepted with warning
```

This prevents known TCC/mixed inputs from silently entering the saga catalog.

## Stage 3: scenario catalog

The static catalog is the deterministic machine-readable plan:

```text
scenario-catalog.jsonl
scenario-catalog-manifest.json
scenario-catalog-rejected-inputs.jsonl
scenario-space-accounting.json
```

Each scenario plan can include:

- participating saga instance(s);
- selected input variant(s);
- step schedule;
- aggregate read/write footprint evidence;
- fault slots for later execution;
- warnings/diagnostics.

The catalog is not yet proof that the scenario has been executed. It is the handoff contract for enrichment and future execution.

## Stage 4: dynamic evidence enrichment

Static analysis is good at structure but weak at exact runtime identity.

It may know:

```text
findQuizStep reads Quiz
```

but not always:

```text
findQuizStep reads Quiz(11)
```

Dynamic enrichment runs selected existing tests with simulator evidence enabled. Runtime emits events such as:

```text
STEP_STARTED
COMMAND_SENT
AGGREGATE_ACCESSED
STEP_FINISHED
```

The verifier joins that runtime evidence back to static scenario plans.

Important rule:

```text
Runtime evidence does not rewrite the static catalog.
It is attached as sidecar evidence.
```

## Join statuses

The enriched catalog uses conservative statuses:

| Status | Plain meaning |
|---|---|
| `MATCHED_EXACT` | Runtime evidence directly names the static input variant. |
| `MATCHED_HIGH_CONFIDENCE` | Runtime evidence lines up by test/functionality/step, but lacks direct input id. |
| `MATCHED_PARTIAL` | Some relevant shape matched, but not enough for high confidence. |
| `AMBIGUOUS` | Runtime evidence could match multiple static candidates; the verifier refuses to guess. |
| `UNMATCHED` | Runtime evidence exists but cannot be usefully joined to this scenario. |
| `NOT_COVERED` | No useful runtime evidence was observed for this scenario. |

## Current dynamic-enrichment result

On the refreshed Quizzes sagas-local baseline against the post-event-semantics catalog:

```text
run: verifiers/target/2026-06-29-dynamic-baseline-test-profile/quizzes-20260629-222801-046/
scenario records: 584
test classes selected/passed/failed: 45 / 43 / 2
dynamicEventsRead: 26820
MATCHED_EXACT=291
MATCHED_HIGH_CONFIDENCE=109
MATCHED_PARTIAL=0
AMBIGUOUS=0
UNMATCHED=184
NOT_COVERED=0
warningCount=0
```

The older baseline before runtime input attribution was:

```text
MATCHED_EXACT=0
MATCHED_HIGH_CONFIDENCE=2
AMBIGUOUS=44
UNMATCHED=20
warningCount=8238
```

Plain interpretation:

```text
Runtime input attribution works at Quizzes scale.
It eliminated ambiguity in the latest baseline.
The remaining gap is unmatched static plans, not ambiguous joins.
```

Do not overstate this. It does not prove stream/gRPC, distributed, TCC, or generic executor behavior.

## Stage 5: scenario execution

A narrow ScenarioExecutor POC exists. It can run supported single-saga catalog candidates and has one successful Quizzes smoke.

Current POC evidence:

```text
Saga: GetCourseExecutionsFunctionalitySagas
Step: getCourseExecutionsStep
Terminal status: SUCCESS
```

But generic scenario execution is not complete. Still missing:

- arbitrary catalog replay;
- multi-saga execution;
- generated fault injection;
- behavior CSV generation;
- impact scoring;
- GA/local search;
- scenario prioritization.

## Why this is hard

The hard problem is binding values across layers.

A value may start in a test:

```groovy
quizFunctionalities.findQuiz(11)
```

or be created by setup:

```groovy
quizDto = createQuiz(...)
quizFunctionalities.findQuiz(quizDto.aggregateId)
```

Then it can flow through:

```text
test helper -> facade -> saga constructor -> saga field -> step -> command -> handler -> service -> aggregate
```

Perfect static tracking across Java and Groovy would be a large interprocedural value-analysis problem. The current design avoids pretending otherwise:

```text
Static analysis finds useful candidates.
Runtime evidence confirms what actually happened.
The join stays conservative when identity is unclear.
```

## Safe thesis framing

Safe claim:

> The verifier now produces deterministic saga scenario catalogs and can enrich them with runtime evidence. On the refreshed Quizzes sagas-local baseline, runtime input attribution produced 291 exact matches, 109 high-confidence matches, and zero ambiguous joins over 584 static scenario records.

Unsafe claim:

> The system can already execute arbitrary generated fault scenarios and optimize them with search.

That is future work.

## Next improvement

Classify the remaining `UNMATCHED=184` records before adding more attribution rules.

The next implementation should be the smallest structured refinement justified by those cases, likely using command payloads, aggregate accesses, literal hints, or aggregate keys. Avoid broad name matching that increases false exactness.
