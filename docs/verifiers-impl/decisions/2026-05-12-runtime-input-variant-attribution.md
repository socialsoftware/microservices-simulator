# Decision: runtime input-variant attribution for dynamic enrichment

Date: 2026-05-12

## Status

Accepted; partially implemented.

The dynamic-enrichment bridge can collect runtime evidence and join it back to static scenario plans. It can produce `MATCHED_EXACT`, `MATCHED_HIGH_CONFIDENCE`, `AMBIGUOUS`, `UNMATCHED`, and `NOT_COVERED` results.

The first-pass runtime attribution path is implemented: the verifier writes per-test `dynamic-input-map.json` files, the simulator loads those maps, runtime events can carry static `inputVariantId`, and the joiner upgrades matching scenario plans to `MATCHED_EXACT`.

Deferred parts:

- runtime candidate refinement using `COMMAND_SENT.rootAggregateId`;
- runtime candidate refinement using `AGGREGATE_ACCESSED.aggregateType + aggregateId`;
- contradiction/mismatch counters;
- broader warning ergonomics for why remaining candidates were rejected.

## Problem

Static analysis creates `InputVariant` records.

An `InputVariant` is a source/test-derived way to invoke a saga/functionality. Example:

```text
source test:
  CreateTournamentFaultTest

source method:
  Check Quiz existence

source expression:
  quizFunctionalities.findQuiz(11)

static saga:
  FindQuizFunctionalitySagas

static step:
  findQuizStep

inputVariantId:
  2c98e7...
```

Dynamic analysis observes what happens when tests run. Example:

```text
test:
  CreateTournamentFaultTest / Check Quiz existence

runtime functionality:
  findQuiz

runtime step:
  findQuizStep

command:
  GetQuizByIdCommand(rootAggregateId=11)

aggregate access:
  READ SagaQuiz(11)
```

Without direct runtime ids, the joiner infers that these probably correspond to the same input by matching test identity, saga/functionality name, and step name. That is useful, but it is still an inferred match.

For `MATCHED_EXACT`, the runtime event should explicitly say:

```text
inputVariantId = 2c98e7...
```

## Main constraints

- Do not add Quizzes-specific hooks.
- Do not modify Quizzes production code.
- Keep simulator changes small and generic.
- Keep dynamic evidence opt-in.
- Do not guess exact matches when the evidence is ambiguous.
- Preserve the static `scenario-catalog.jsonl` as the reproducible contract; dynamic outputs stay sidecar artifacts.

## Decision process

### 1. Exactness must come from raw runtime evidence

We considered whether the verifier could infer `inputVariantId` after the test run and mark that as exact.

Decision:

```text
MATCHED_EXACT requires raw dynamic evidence to contain inputVariantId.
```

Reason:

If the verifier infers the id afterward, that is still a heuristic. It can be `MATCHED_HIGH_CONFIDENCE`, but not exact.

### 2. Use one input map per test class

The verifier already runs dynamic enrichment one test class at a time.

Decision:

```text
dynamic-evidence/<safe-test-class>/dynamic-input-map.json
```

Each map contains only accepted `InputVariant`s from the final generated scenario plans for that test class.

Reason:

Per-class maps are small, debuggable, and aligned with the existing per-class evidence directories.

### 3. Emit runtime workflow class identity

Runtime currently records `functionalityName`, which often comes from the facade method name stored in the unit of work, such as:

```text
findQuiz
```

But the actual workflow object exists at runtime:

```text
pt...FindQuizFunctionalitySagas
```

Decision:

Add top-level fields to dynamic events:

```text
functionalityClassFqn
functionalityClassSimpleName
```

Use `functionalityClassFqn` as the primary match against static `InputVariant.sagaFqn`.

Reason:

This avoids relying on loose name aliases when the actual runtime class is available.

### 4. Emit inputVariantId only when attribution is unambiguous

Decision:

```text
0 candidates:
  no inputVariantId

1 candidate:
  emit inputVariantId

multiple candidates:
  no inputVariantId
  record diagnostic candidate ids/reason
```

Reason:

Exact attribution must be conservative. Ambiguous evidence is still useful, but it should not be promoted to exact.

### 5. Use runtime ids only from explicit simulator concepts

Runtime command events include structured command metadata and optionally reflected command fields.

Decision:

Use these for exact candidate pruning:

```text
COMMAND_SENT.rootAggregateId
AGGREGATE_ACCESSED.aggregateType + aggregateId
```

Do not use arbitrary reflected command/DTO fields from `payload.fields.*` for exact attribution in v1.

Reason:

Field-name matching can be misleading. A command may contain several ids, such as tournament id, student id, user id, and course execution id. Without a stronger static field-to-aggregate binding, generic field-name matching risks false exact matches.

Command fields remain useful diagnostics, but not exact attribution evidence yet.

Implementation status:

Deferred. The first-pass implementation currently attributes from test identity, runtime functionality class FQN, and runtime step name. Command and aggregate runtime ids are the next refinement candidates, after classifying the remaining Quizzes `AMBIGUOUS=3` and `UNMATCHED=17` records.

### 6. Allow later events to resolve the step context

At step start, test identity + functionality class + step may be enough to find one candidate. If not, later command or aggregate events may disambiguate.

Decision:

```text
STEP_STARTED:
  try initial attribution

COMMAND_SENT:
  refine using rootAggregateId

AGGREGATE_ACCESSED:
  refine/confirm using aggregateType + aggregateId

STEP_FINISHED:
  use the resolved id if the context has one
```

No buffering or rewriting old JSONL events in v1.

Reason:

Append-only JSONL is simpler and more robust. It is fine if `STEP_STARTED` has no id but `COMMAND_SENT` and `AGGREGATE_ACCESSED` later do.

Implementation status:

Deferred with Slice 4. The current implementation resolves the active step context at step entry and propagates that `inputVariantId` to step, command, and aggregate events. It does not yet let later command/aggregate events refine an unresolved step context.

### 7. Contradictions clear exact attribution

Decision:

If a context resolves to one `inputVariantId` but later evidence contradicts it:

```text
stop emitting inputVariantId for future events in that context
record a contradiction diagnostic
```

Reason:

Contradictions indicate that the attribution model is missing something. Keeping or switching ids silently would make `MATCHED_EXACT` untrustworthy.

Implementation status:

Deferred. The current conservative behavior avoids guessing when the initial candidate set is ambiguous, but it does not yet emit explicit contradiction counters.

### 8. Accepted replayable inputs are eligible

Decision:

Any accepted `InputVariant` present in final `ScenarioPlan`s can appear in the dynamic input map, including `REPLAYABLE` inputs.

Rejected inputs do not appear in the v1 map.

Reason:

Replayable inputs are exactly where runtime evidence is useful. Rejected inputs are outside the accepted scenario catalog and should not produce exact catalog enrichment.

## Implementation slices

### Slice 1: runtime class identity

Add actual workflow class identity to dynamic evidence.

Outputs:

```text
functionalityClassFqn
functionalityClassSimpleName
```

Verification:

- Simulator event JSON includes the class FQN/simple name.
- Verifier event reader parses these fields.
- Joiner prefers FQN equality when available.

Status: implemented.

### Slice 2: per-test dynamic input map

Write:

```text
dynamic-evidence/<safe-test-class>/dynamic-input-map.json
```

from final `ScenarioGenerationResult.scenarioPlans()`.

Map entries include:

```text
inputVariantId
sourceClassFqn
sourceMethodName
sagaFqn
stepNameHints
literal argumentValueHints
expectedCommands
expectedAggregateTypes
scenarioPlanIds
stableSourceText/provenanceText for diagnostics
```

Verification:

- Includes only accepted catalog inputs from the selected test class.
- Excludes rejected inputs.
- Extracts simple literal hints such as `arg[1]: 11`.

Status: implemented.

### Slice 3: simulator map loading and initial attribution

Add property:

```text
simulator.dynamic-evidence.input-map-path
```

The dynamic test listener loads the map. During each test method, runtime contexts can find candidates by:

```text
test class/method
functionalityClassFqn
step name
```

Verification:

- Unique candidate emits `inputVariantId`.
- Zero candidates emits no id.
- Multiple candidates emits no id and records diagnostics.

Status: implemented.

### Slice 4: runtime id refinement

Use:

```text
COMMAND_SENT.rootAggregateId
AGGREGATE_ACCESSED.aggregateType + aggregateId
```

to refine candidate sets.

Verification:

- `rootAggregateId` resolves literal candidate ambiguity.
- Aggregate type/id confirms or refines candidates.
- Contradictions clear attribution.
- Reflected command fields are ignored for exact attribution.

Status: deferred.

### Slice 5: joiner and reporting counters

Keep the existing join ladder:

```text
MATCHED_EXACT
MATCHED_HIGH_CONFIDENCE
MATCHED_PARTIAL
AMBIGUOUS
UNMATCHED
NOT_COVERED
```

Add attribution-quality counters:

```text
eventsWithInputVariantId
eventsWithoutInputVariantId
inputVariantAttributionContradictions
functionalityClassMismatches
testIdentityMismatches
resolvedByBasis
rejectedByReason
```

Verification:

- Exact events produce `MATCHED_EXACT`.
- Old evidence without ids still works through high-confidence/ambiguous fallbacks.
- Contradictions/mismatches are visible in reports.

Status: partially implemented. Direct `inputVariantId` evidence now produces `MATCHED_EXACT`, and old evidence still flows through the previous join ladder. The attribution-quality counters remain deferred.

### Slice 6: dummyapp correctness oracle

Use dummyapp to prove correctness with controlled cases:

```text
unique candidate -> exact
zero candidate -> no id
multiple candidates -> ambiguous/no id
rootAggregateId resolves ambiguity
contradiction prevents exact
rejected inputs stay out of the map
```

Status: partially implemented. Dummyapp now proves the key before/after status shift from semantic-only evidence to direct `inputVariantId` evidence. The later command/aggregate refinement and contradiction cases remain deferred.

### Slice 7: Quizzes before/after measurement

Compare the same Quizzes dynamic-enrichment baseline before and after propagation.

Original full-run baseline:

```text
MATCHED_EXACT=0
MATCHED_HIGH_CONFIDENCE=2
AMBIGUOUS=44
UNMATCHED=20
NOT_COVERED=0
```

Success criteria:

```text
MATCHED_EXACT > 0
MATCHED_EXACT + MATCHED_HIGH_CONFIDENCE increases
AMBIGUOUS decreases, or rejectedByReason explains why not
NOT_COVERED does not unexpectedly increase
contradictions = 0
functionalityClassMismatches = 0
testIdentityMismatches = 0
```

Observed refreshed Quizzes result after first-pass runtime attribution:

```text
MATCHED_EXACT=46
MATCHED_HIGH_CONFIDENCE=0
MATCHED_PARTIAL=0
AMBIGUOUS=3
UNMATCHED=17
NOT_COVERED=0
warningCount=328
```

Status: implemented for the comparable local/sagas Quizzes run. The result mainly improved exactness and reduced ambiguity/warnings; it did not prove that every useful static input is dynamically covered.

## Summary

The accepted design keeps exact attribution application-agnostic:

- the verifier writes a per-test map from its static scenario catalog;
- the simulator loads that map through opt-in dynamic-evidence config;
- runtime emits `inputVariantId` only when attribution is unambiguous;
- reports quantify exactness, ambiguity, and contradictions.

The first-pass implementation proved the core path without adding Quizzes-specific hooks. The next work is to classify the remaining ambiguous/unmatched Quizzes records before adding command/aggregate/literal refinement.
