# Advisor brief

Last updated: 2026-06-29

Use this page before thesis meetings. It is intentionally short. For implementation detail, follow links from [`current-state.md`](current-state.md).

## 30-second update

The verifier can generate deterministic saga scenario catalogs from application source and tests, including accepted static inputs for the implemented `EventHandling`/`EventProcessing` event-origin shape, enrich those catalogs with runtime evidence from existing simulator test runs, and run a narrow single-saga executor POC for supported catalog entries.

The current thesis gap is no longer the original event-driven static input hole for the target group, and the dynamic attribution baseline has now been refreshed. The hard boundary is now materialization/replayability plus a repeatable fault-injection and impact-scoring baseline.

## What exists now

- Static saga scenario extraction from Java/Groovy source.
- Static event semantics for the implemented `EventHandling`/`EventProcessing` chain shape.
- Source-mode filtering so saga catalogs do not silently include known TCC/mixed inputs.
- Deterministic JSONL scenario catalog, manifest, rejected-input diagnostics, and scenario-space accounting.
- Optional dynamic enrichment that runs selected tests and joins runtime evidence back to static scenario plans.
- Runtime input attribution for the first exact case: test owner + functionality class + step name resolves to one static input variant.
- Conflict-anchor segment-compressed schedule counting/generation for selected saga sets.
- A narrow ScenarioExecutor POC that can execute supported single-saga catalog candidates without fault injection.

## Evidence I can cite

Post-event-semantics Quizzes count-only static accounting:

```text
discovered sagas: 65 -> 68
sagas with accepted inputs: 26 -> 36
sagas without accepted inputs: 39 -> 32
accepted input variants: 517 -> 584
selected input-bound scenario total: 517 -> 584
catalog written: 0 -> 0 (COUNT_ONLY, expected)
staticRecipeReadyInputVariantCount: 0
executorMaterializableInputVariantCount: 94
blockedInputVariantCount: 490
```

Fresh post-event-semantics Quizzes dynamic baseline:

```text
run: verifiers/target/2026-06-29-dynamic-baseline-test-profile/quizzes-20260629-222801-046/
scenario records: 584
test classes selected/passed/failed: 45 / 43 / 2
dynamicEventsRead: 26820
MATCHED_EXACT: 291
MATCHED_HIGH_CONFIDENCE: 109
AMBIGUOUS: 0
UNMATCHED: 184
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

Older accounting that reported zero executor-ready inputs was measuring static recipe readiness only; executor materializability is now reported separately/aligned with ScenarioExecutor semantics. Static accepted input coverage, dynamic sidecar attribution, and executor materializability are separate claims.

Detailed commands and run paths live in [`evidence.md`](evidence.md).

## What I should not claim

- Generic scenario execution is not complete.
- Fault injection from generated schedules is not implemented.
- Multi-saga runtime execution is not implemented.
- Domain-impact scoring is not implemented.
- GA/local search and bandit prioritization are not implemented.
- TCC, stream/gRPC, and distributed-runtime parity are not established.
- Static accepted input coverage does not mean executor-ready or replayable.
- Dynamic evidence does not rewrite or create static catalog structure; it is attached next to it.
- The refreshed dynamic baseline does not validate every static scenario; `UNMATCHED=184` remains.

## Next meeting: in-person zoom-out

Next week’s advisor meeting is in person and should be treated as a full recap/design review. Be ready to explain what has been built, how the pieces fit together, what currently runs, what is incomplete, what is hard or poorly designed, what is working well, and which questions need advisor input.

Do not reread every archived note. Use this page, [`current-state.md`](current-state.md), [`roadmap.md`](roadmap.md), [`evidence.md`](evidence.md), and [`reference/scenario-executor-poc.md`](reference/scenario-executor-poc.md) as the prep path.

## Current limitation to explain plainly

Static analysis can now accept the original event-driven target group through the implemented event topology, but 32 Quizzes sagas still lack accepted static inputs and need classification. Missing accepted input means no accepted static `InputVariant` was discovered; it does not mean no test exists.

Dynamic evidence helps by showing what happened during real executions, but it remains a sidecar. The current executor-specific hard part is materialization: turning static catalog inputs, event payload placeholders, recipes, Spring/runtime dependencies, and runtime-produced values into live objects that can be executed reliably.

## Next work

1. Finalize and classify the remaining 32 Quizzes sagas without accepted static inputs.
2. Classify the refreshed dynamic baseline's `UNMATCHED=184` records and decide whether improving runtime matching is worth it before executor work.
3. Improve event payload reconstruction and materialization/replay for event-origin inputs.
4. Tighten exact aggregate-instance key extraction where it affects scenario usefulness.
5. Continue executor baseline/fault injection only after materialization improves.

## Question for advisor

Is the next thesis milestone better framed as:

1. improving remaining static coverage and refreshed static-dynamic attribution, or
2. prioritizing materialization/replay so a minimal executable fault-injection baseline becomes credible?
