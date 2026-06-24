# Advisor brief

Last updated: 2026-06-23

Use this page before thesis meetings. It is intentionally short. For implementation detail, follow links from [`current-state.md`](current-state.md).

## 30-second update

The verifier can generate deterministic saga scenario catalogs from application source and tests, enrich those catalogs with runtime evidence from existing simulator test runs, and run a narrow single-saga executor POC for supported catalog entries.

The current thesis gap is no longer “can we discover scenario structure?” It is moving from static catalog + runtime evidence + executor POC to a repeatable scenario execution baseline with fault injection and impact scoring.

## What exists now

- Static saga scenario extraction from Java/Groovy source.
- Source-mode filtering so saga catalogs do not silently include known TCC/mixed inputs.
- Deterministic JSONL scenario catalog, manifest, rejected-input diagnostics, and scenario-space accounting.
- Optional dynamic enrichment that runs selected tests and joins runtime evidence back to static scenario plans.
- Runtime input attribution for the first exact case: test owner + functionality class + step name resolves to one static input variant.
- Conflict-anchor segment-compressed schedule counting/generation for selected saga sets.
- A narrow ScenarioExecutor POC that can execute supported single-saga catalog candidates without fault injection.

## Evidence I can cite

Latest comparable Quizzes sagas-local dynamic-enrichment baseline after runtime input attribution:

```text
MATCHED_EXACT: 0 -> 46
AMBIGUOUS: 44 -> 3
UNMATCHED: 20 -> 17
warningCount: 8238 -> 328
```

Segment-compressed scheduling reduced a Quizzes count-only selected-space comparison from:

```text
ORDER_PRESERVING_INTERLEAVING: 218528454
SEGMENT_COMPRESSED: 1019393
```

ScenarioExecutor POC smoke executed one generated Quizzes single-saga plan successfully by resolving runtime-owned infrastructure arguments itself:

```text
Saga: GetCourseExecutionsFunctionalitySagas
Step: getCourseExecutionsStep
Terminal status: SUCCESS
```

Older accounting that reported zero executor-ready inputs was measuring static recipe readiness only; executor materializability is now reported separately/aligned with ScenarioExecutor semantics.

Detailed commands and run paths live in [`evidence.md`](evidence.md).

## What I should not claim

- Generic scenario execution is not complete.
- Fault injection from generated schedules is not implemented.
- Multi-saga runtime execution is not implemented.
- Domain-impact scoring is not implemented.
- GA/local search and bandit prioritization are not implemented.
- TCC, stream/gRPC, and distributed-runtime parity are not established.
- Dynamic evidence does not rewrite the static catalog; it is attached next to it.

## Next meeting: in-person zoom-out

Next week’s advisor meeting is in person and should be treated as a full recap/design review. Be ready to explain what has been built, how the pieces fit together, what currently runs, what is incomplete, what is hard or poorly designed, what is working well, and which questions need advisor input.

Do not reread every archived note. Use this page, [`current-state.md`](current-state.md), [`roadmap.md`](roadmap.md), [`evidence.md`](evidence.md), and [`reference/scenario-executor-poc.md`](reference/scenario-executor-poc.md) as the prep path.

## Current limitation to explain plainly

Static analysis can usually identify which aggregate type a step touches, but exact aggregate-instance binding is still incomplete. Dynamic evidence helps by showing what happened during real executions, but unresolved/ambiguous cases remain and should not be guessed.

The current executor-specific hard part is materialization: turning static catalog inputs, recipes, placeholders, Spring/runtime dependencies, and runtime-produced values into live objects that can be executed reliably.

## Next work

1. Classify the remaining Quizzes enrichment misses (`AMBIGUOUS=3`, `UNMATCHED=17`).
2. Add the smallest attribution refinement justified by that classification, likely command/aggregate/literal pruning rather than broad name matching.
3. Tighten exact aggregate-instance key extraction where it affects scenario usefulness.
4. Turn the ScenarioExecutor POC into a minimal repeatable executor baseline.
5. Add first fault-injection and impact-scoring hooks after the executor baseline is stable.

## Question for advisor

Is the next thesis milestone better framed as:

1. improving scenario quality/static-dynamic matching further, or
2. moving quickly to a minimal executable fault-injection baseline even if some generated scenarios remain conservative/type-level?
