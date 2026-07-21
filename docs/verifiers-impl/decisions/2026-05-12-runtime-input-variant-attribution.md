# Decision: runtime input-variant attribution for dynamic enrichment

Date: 2026-05-12

## Status

Accepted; first-pass attribution remains active. Artifact ownership migrated to the implemented v3 workload-linked sidecar contract on 2026-07-20.

The dynamic-enrichment bridge can collect runtime evidence and join it back to static WorkloadPlans. References below to ScenarioPlans and enriched catalog wrappers describe the historical v2 implementation; current output is `workload-dynamic-evidence.jsonl` plus its manifest/join report and does not mutate the five semantic package artifacts. It can produce `MATCHED_EXACT`, `MATCHED_HIGH_CONFIDENCE`, `AMBIGUOUS`, `UNMATCHED`, and `NOT_COVERED` results.

The first-pass runtime attribution path is implemented: the verifier writes a `dynamic-input-map.json`, the simulator loads that map, runtime events can carry static `inputVariantId`, and the joiner upgrades matching scenario plans to `MATCHED_EXACT`.

2026-06-29 update: the current run shape writes a run-level map for the selected-test-class Maven batch, not one map per Maven process/class. The refreshed post-event Quizzes baseline before fixture/setup ownership diagnostics was `MATCHED_EXACT=291`, `MATCHED_HIGH_CONFIDENCE=109`, `AMBIGUOUS=0`, `UNMATCHED=184`, `warningCount=0`. Older counts in this decision are historical evidence for why the runtime-attribution path was added.

2026-06-30 update: ownership-aware attribution now separates source provenance, call context, fixture role, and runtime owner. The current Quizzes baseline after fixture/setup and feature-helper ownership fixes is `MATCHED_EXACT=435`, `MATCHED_HIGH_CONFIDENCE=125`, `AMBIGUOUS=0`, `UNMATCHED=24`, with `unmatchedReasonCounts={FAILED_TEST_CLASS=8, NOT_SELECTED_TEST_CLASS=7, HELPER_OWNER_MISMATCH=0, UNCLASSIFIED=9}`.

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
- Preserve the complete v3 WorkloadPlan/FaultScenario package as the reproducible contract; dynamic outputs stay workload-linked sidecar artifacts.

## Decision process

### 1. Exactness must come from raw runtime evidence

We considered whether the verifier could infer `inputVariantId` after the test run and mark that as exact.

Decision:

```text
MATCHED_EXACT requires raw dynamic evidence to contain inputVariantId.
```

Reason:

If the verifier infers the id afterward, that is still a heuristic. It can be `MATCHED_HIGH_CONFIDENCE`, but not exact.

### 2. Write an input map for the dynamic test selection

Original 2026-05-12 decision:

```text
dynamic-evidence/<safe-test-class>/dynamic-input-map.json
```

The original plan used one map per test class because the dynamic runner was shaped as one Maven execution per class.

Current 2026-06-29 implementation shape:

```text
<run-dir>/dynamic-evidence/dynamic-input-map.json
```

The current verifier runs a selected-test-class Maven batch and writes a run-level map containing accepted `InputVariant`s from the final generated scenario plans. Entries still carry test ownership metadata, so runtime attribution can filter by test identity even though the map is not physically split per class.

Reason:

The important invariant is not the file granularity. The important invariant is that the simulator receives a verifier-generated map built from accepted final catalog inputs and uses test ownership + functionality class + step name before emitting exact `inputVariantId` evidence.

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

Deferred. The implemented attribution path currently uses test identity, runtime functionality class FQN, runtime step name, direct `inputVariantId`, and ownership metadata. Command and aggregate runtime ids remain refinement candidates, but the refreshed Quizzes baseline now has `UNMATCHED=24`; the `UNCLASSIFIED=9` residuals should be triaged before adding value-based matching.

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

Historical 2026-05-12 original full-run baseline:

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

Historical 2026-05-12 refreshed Quizzes result after first-pass runtime attribution:

```text
MATCHED_EXACT=46
MATCHED_HIGH_CONFIDENCE=0
MATCHED_PARTIAL=0
AMBIGUOUS=3
UNMATCHED=17
NOT_COVERED=0
warningCount=328
```

Historical 2026-06-29 post-event-semantics refreshed Quizzes result before fixture/setup ownership diagnostics:

```text
MATCHED_EXACT=291
MATCHED_HIGH_CONFIDENCE=109
MATCHED_PARTIAL=0
AMBIGUOUS=0
UNMATCHED=184
NOT_COVERED=0
warningCount=0
```

Current 2026-06-30 ownership-aware Quizzes result:

```text
MATCHED_EXACT=435
MATCHED_HIGH_CONFIDENCE=125
MATCHED_PARTIAL=0
AMBIGUOUS=0
UNMATCHED=24
NOT_COVERED=0
unmatchedReasonCounts={FAILED_TEST_CLASS=8, NOT_SELECTED_TEST_CLASS=7, HELPER_OWNER_MISMATCH=0, UNCLASSIFIED=9}
```

Status: implemented for the comparable local/sagas Quizzes run. The result mainly improved exactness and reduced ambiguity/warnings; it did not prove that every useful static input is dynamically covered.

## Summary

The accepted design keeps exact attribution application-agnostic:

- the verifier writes a run-level dynamic input map from its static scenario catalog;
- the simulator loads that map through opt-in dynamic-evidence config;
- runtime emits `inputVariantId` only when attribution is unambiguous;
- reports quantify exactness, ambiguity, and contradictions.

The implementation proved the core path without adding Quizzes-specific hooks and then removed most helper/setup ownership misses. The next work is to triage the remaining `UNCLASSIFIED=9` residuals before adding command/aggregate/literal refinement.
