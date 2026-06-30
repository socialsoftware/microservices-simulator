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
A static input variant = one observed source/test way to invoke a saga/functionality. It keeps these identities separate: provenance (`sourceClassFqn`, `sourceMethodName`, `sourceBindingName`, `provenanceText`), owner feature(s), call context (`callContextMethodName`), and fixture metadata (`inputRole`, `fixtureOrigin`). Setup/helper-created inputs are fixture prerequisites, not feature-under-test inputs, but can still be owned by the feature methods that run that setup.
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

On the refreshed Quizzes sagas-local baseline after fixture/setup and feature-helper ownership fixes:

```text
run: verifiers/target/feature-helper-owner-fix-dynamic-smoke/quizzes-20260630-122219-034/
scenario records: 584
test classes selected/passed/failed: 45 / 43 / 2
dynamicEventsRead: 26815
MATCHED_EXACT=435
MATCHED_HIGH_CONFIDENCE=125
MATCHED_PARTIAL=0
AMBIGUOUS=0
UNMATCHED=24
NOT_COVERED=0
unmatchedReasonCounts={FAILED_TEST_CLASS=8, NOT_SELECTED_TEST_CLASS=7, HELPER_OWNER_MISMATCH=0, UNCLASSIFIED=9}
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
Fixture/setup and feature-helper ownership fixes reduced unmatched records from 184 to 24.
The latest baseline still has zero ambiguous joins. Unmatched records now carry a diagnostic reason so the gap can be separated into failed test classes, non-selected test classes, helper-owner mismatches, and unclassified residual cases.
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

> The verifier now produces deterministic saga scenario catalogs and can enrich them with runtime evidence. On the refreshed Quizzes sagas-local baseline, runtime input attribution and ownership metadata produced 435 exact matches, 125 high-confidence matches, zero ambiguous joins, and 24 unmatched records over 584 static scenario records.

Unsafe claim:

> The system can already execute arbitrary generated fault scenarios and optimize them with search.

That is future work.

## Next improvement

Triage the remaining `UNMATCHED=24` records before adding more attribution rules.

The next implementation should be the smallest structured refinement justified by those cases. Current residuals point more to failed/not-selected tests, fault paths, async context boundaries, and negative-input paths than to broad runtime-value matching. Avoid broad name matching that increases false exactness.
