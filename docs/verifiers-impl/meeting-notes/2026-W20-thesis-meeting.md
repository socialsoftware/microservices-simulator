# Thesis meeting notes — 2026-W20

Date: 2026-05-12

Purpose: advisor meeting notes for the static scenario catalog + dynamic enrichment milestone and the next exact-attribution step.

## One-sentence update

The verifier now has a committed static + dynamic enrichment milestone: it can generate a scenario catalog, run selected tests with simulator dynamic evidence enabled, and write sidecar enriched artifacts that show which static scenarios are supported, ambiguous, unmatched, or not covered by runtime evidence.

## What changed since the last committed baseline

- Committed simulator dynamic evidence:
  - opt-in JSONL/manifest artifacts;
  - step start/finish evidence;
  - command-sent evidence;
  - saga aggregate read/write evidence.
- Committed verifier dynamic enrichment:
  - per-test-class Maven orchestration;
  - dynamic evidence reader;
  - static/dynamic joiner;
  - enriched sidecar writer;
  - join report.
- Committed Docker/runtime wiring:
  - verifier runtime image can launch Maven tests;
  - Compose service enables static catalog + dynamic enrichment flow.
- Committed verifier documentation:
  - current-state/roadmap updates;
  - dynamic-analysis investigation;
  - hybrid static/dynamic decision note;
  - dynamic joining explanation.
- Prepared the design for runtime `inputVariantId` propagation.

Related deeper notes:

- `docs/verifiers-impl/verifier-pipeline-plain-explanation.md`
- `docs/verifiers-impl/dynamic-enrichment-joining-explained.md`
- `docs/verifiers-impl/decisions/2026-05-12-runtime-input-variant-attribution.md`

## Current pipeline narrative

1. Parse application source.
2. Extract saga/functionality structure, workflow steps, command dispatches, and aggregate footprints.
3. Parse tests to find realistic saga inputs (`InputVariant`s).
4. Filter source inputs by transactional mode (`SAGAS`, `TCC`, `MIXED`, `UNKNOWN`) for the saga catalog.
5. Generate static `scenario-catalog.jsonl`.
6. Run selected application tests with simulator dynamic evidence enabled.
7. Collect runtime events for steps, commands, and aggregate accesses.
8. Join runtime evidence back to static scenarios as sidecar enriched artifacts.
9. Next: propagate `inputVariantId` into runtime events so some inferred matches become exact.

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

Dynamic evidence observes:

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

Current join:

```text
MATCHED_HIGH_CONFIDENCE
```

Planned join after propagation:

```text
runtime event carries inputVariantId=2c98e7...
join result can become MATCHED_EXACT
```

## Validation baseline

Narrow Quizzes dynamic-enrichment smoke:

```text
runStatus = COMPLETE
testClassesSelected = 2
testClassesPassed = 2
dynamicEventsRead = 362
MATCHED_HIGH_CONFIDENCE = 1
UNMATCHED = 199
```

Full/default sagas-only Quizzes run:

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

Failure interpretation:

- The two failed classes in the full run were reproduced without dynamic enrichment instrumentation.
- Current interpretation: likely existing test/runtime behavior, not a dynamic-enrichment regression.

## What this proves

- The static catalog can be enriched with real runtime evidence.
- Runtime evidence includes concrete command and aggregate access facts.
- The full Quizzes run exposes measurable ambiguity instead of hiding it.
- The next precision improvement has clear before/after metrics.

## What not to overclaim

- This is not the `ScenarioExecutor` yet.
- It does not yet run generated fault scenarios.
- It does not yet materialize arbitrary catalog inputs at runtime.
- It does not yet inject generated fault vectors.
- It does not yet implement impact scoring or search.
- Exact `inputVariantId` propagation is planned, not implemented.
- Current dynamic parity is local/sagas only:
  - no stream/gRPC parity yet;
  - no distributed runtime parity yet;
  - no causal/TCC runtime parity yet.

## Proposed next design

The next precision step is runtime input-variant attribution.

Planned design:

1. The verifier writes one `dynamic-input-map.json` per selected test class.
2. The simulator dynamic evidence listener loads that map through an opt-in property.
3. Runtime events include:
   - `functionalityClassFqn`;
   - `functionalityClassSimpleName`;
   - `inputVariantId` when attribution is unambiguous.
4. Runtime attribution uses:
   - test class/method;
   - actual functionality class FQN;
   - step name;
   - `COMMAND_SENT.rootAggregateId`;
   - `AGGREGATE_ACCESSED.aggregateType + aggregateId`.
5. Runtime attribution does not use arbitrary reflected command/DTO fields for exact matches in v1.
6. Reports count exact matches, ambiguity, contradictions, and mismatch reasons.

## Success metrics for next iteration

Correctness gates:

```text
inputVariantAttributionContradictions = 0
functionalityClassMismatches = 0
testIdentityMismatches = 0
```

Usefulness gates:

```text
MATCHED_EXACT > 0
MATCHED_EXACT + MATCHED_HIGH_CONFIDENCE increases
AMBIGUOUS decreases, or rejectedByReason explains why not
NOT_COVERED does not unexpectedly increase
```

Evaluation split:

```text
dummyapp:
  correctness oracle with controlled cases

Quizzes:
  scale/realism benchmark with before/after counts
```

## Questions for advisor

1. Is the sidecar dynamic-enrichment design acceptable, or should runtime evidence eventually modify the canonical catalog?
2. Should the next iteration prioritize exact `inputVariantId` propagation over starting the `ScenarioExecutor`?
3. Is the proposed exactness rule conservative enough: only emit `inputVariantId` when runtime attribution is unambiguous?
4. For evaluation, are exact-match rate, strong-match rate, ambiguity rate, and contradiction count good metrics?
5. Should dummyapp be used as the correctness oracle and Quizzes as the scale/realism benchmark?

## Suggested verbal update

Since the previous static catalog milestone, I added a dynamic evidence bridge. The verifier now generates the static scenario catalog, runs selected tests with simulator instrumentation, and joins runtime evidence back to the static scenarios. Runtime evidence records step execution, commands sent, and concrete aggregate reads/writes. On Quizzes, the bridge works and produces measurable results, but the full run exposes ambiguity: exact matches are still zero, high-confidence matches are two, and ambiguous joins are forty-four. The next planned step is to propagate static `inputVariantId`s into runtime events through a per-test input map, so unambiguous runtime evidence can become `MATCHED_EXACT` instead of inferred.

