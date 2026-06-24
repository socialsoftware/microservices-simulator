# Verifier current state

Last updated: 2026-06-23

This is the present-tense status page for verifier/scenario-generation work. Keep it short. Detailed run evidence lives in [`evidence.md`](evidence.md); meeting framing lives in [`advisor-brief.md`](advisor-brief.md).

## One-paragraph summary

The verifier currently extracts saga-oriented scenario structure from simulator applications, generates deterministic scenario catalogs, and can optionally enrich those catalogs with runtime evidence from existing test executions. It also has a narrow ScenarioExecutor POC for supported single-saga candidates. Generic scenario execution, generated fault injection, impact scoring, GA search, and scenario prioritization remain future work.

## Scope

Current implemented scope:

- Saga-catalog generation from Java production code and Groovy/Spock tests.
- Source-mode classification for saga/TCC/mixed/unknown test inputs.
- Deterministic bounded scenario catalog generation and accounting that separates static recipe readiness from ScenarioExecutor materializability.
- Optional dynamic enrichment using simulator runtime evidence.
- Narrow single-saga ScenarioExecutor POC for supported materializable catalog entries.

Current main targets:

- `applications/dummyapp/` for verifier fixtures and regression coverage.
- `applications/quizzes/` for realistic smoke runs and thesis-scale evaluation.

Current non-goals:

- TCC runtime scenario generation/execution.
- Distributed, stream, or gRPC runtime parity.
- Quizzes-specific verifier shortcuts.
- Rewriting static catalog records from dynamic evidence.

## Implemented now

### Static extraction

- Application file-tree parsing for Java production files and Groovy test files.
- Saga/functionality discovery, ordered workflow steps, dependency edges, dispatch footprints, and compensation/forward dispatch phases.
- Command-handler dispatch target indexing and service access-policy extraction.
- Domain-service classification based on dispatch structure rather than package/name suffixes.
- Conservative handling for ambiguous service implementations and unresolved dependency references.

### Groovy test/input extraction

- Groovy source indexing and direct saga constructor tracing.
- Facade/functionality call extraction into saga construction recipes.
- Helper-chain tracing, local transforms/coercions, DTO construction, collection shapes, placeholders, and unresolved values.
- Replay-oriented `inputRecipe` payloads embedded on scenario input variants.
- Ownership metadata separate from provenance, so runtime attribution can decide which feature methods may claim an input.

### Source-mode filtering

- Source-mode classification as `SAGAS`, `TCC`, `MIXED`, or `UNKNOWN` using generic simulator/Spring evidence.
- Saga catalog policy:
  - `SAGAS` accepted;
  - `TCC` rejected diagnostically;
  - `MIXED` rejected diagnostically;
  - `UNKNOWN` accepted with warning.

### Scenario catalog and accounting

- Deterministic `ScenarioPlan` ids and bounded generation.
- JSONL scenario catalog, manifest, rejected-input diagnostics, and scenario-space accounting.
- Single-saga scenarios and bounded multi-saga candidates.
- Conflict evidence based on write/read or write/write interactions; read/read pairs ignored.
- `SERIAL`, bounded order-preserving interleaving, and conflict-anchor `SEGMENT_COMPRESSED` schedule strategies.
- `COUNT_ONLY` mode for large-space accounting without materializing every plan.

### Dynamic enrichment

- Disabled-by-default verifier orchestration that runs selected application test classes with simulator evidence enabled.
- Per-test-class `dynamic-input-map.json` written before each dynamic run.
- Simulator-side runtime attribution emits `inputVariantId` when test owner + functionality class + step name resolve to exactly one static input variant.
- Conservative join statuses: `MATCHED_EXACT`, `MATCHED_HIGH_CONFIDENCE`, `MATCHED_PARTIAL`, `AMBIGUOUS`, `UNMATCHED`, `NOT_COVERED`.
- Enriched artifacts are sidecars; `scenario-catalog.jsonl` remains the static contract.
- Plan-local ambiguity containment: direct ids for neighboring inputs are not reused to promote the wrong plan.

### Human reports and packaging

- HTML analysis report generation.
- Per-run output directories with stable artifact names.
- Docker Compose verifier service for full static + dynamic Quizzes-oriented runs.

## POC / partial

### ScenarioExecutor POC

A narrow verifier-owned ScenarioExecutor POC exists. It can load a catalog/enriched catalog, select or receive a supported single-saga scenario, materialize only supported inputs, run the selected saga step schedule, and write an execution report. Accounting now reports ScenarioExecutor materializability separately from static `inputRecipe.executorReady` readiness.

This is not yet generic scenario execution. It does not implement generated fault injection, multi-saga execution, behavior CSV generation, impact scoring, GA search, or scenario prioritization.

### Dynamic enrichment

Runtime input attribution works for the first exact case, but it does not yet use command fields, aggregate access evidence, literal runtime values, or aggregate keys to reduce the remaining ambiguous/unmatched cases.

### Aggregate-instance binding

The verifier often knows aggregate type and access mode, but exact aggregate-instance key extraction is still incomplete.

### Source-mode classification

Source-mode classification is evidence-based. It is not a full Spring profile/environment solver.

## Current evidence

Latest Quizzes sagas-local dynamic-enrichment comparison after runtime input attribution:

```text
MATCHED_EXACT: 0 -> 46
AMBIGUOUS: 44 -> 3
UNMATCHED: 20 -> 17
warningCount: 8238 -> 328
```

Segment-compressed scheduling Quizzes count-only comparison:

```text
ORDER_PRESERVING_INTERLEAVING selected total: 218528454
SEGMENT_COMPRESSED selected total: 1019393
```

ScenarioExecutor POC smoke:

```text
Generated Quizzes single-saga plan executed successfully.
Saga: GetCourseExecutionsFunctionalitySagas
Step: getCourseExecutionsStep
Terminal status: SUCCESS
```

That run relied on executor runtime-owned argument resolution. Older accounting that reported zero executor-ready inputs was measuring static recipe readiness only; executor materializability is now reported as the executor-aligned metric.

See [`evidence.md`](evidence.md) for commands, run paths, and interpretation.

## Current limitations

- Exact aggregate-instance key extraction is incomplete.
- Dynamic enrichment is local/sagas-focused; stream/gRPC/distributed/TCC parity is not established.
- Dynamic evidence is additive sidecar evidence and does not redefine static scenario structure.
- Remaining Quizzes dynamic misses (`AMBIGUOUS=3`, `UNMATCHED=17`) still need manual classification.
- Same-feature sibling ambiguity can remain when current evidence cannot distinguish neighboring static inputs.
- Groovy input recipes are replay-oriented, but generic materialization is incomplete.
- Segment compression is a static reduction under extracted conflict evidence, not proof of semantic completeness.
- Type-only fallback is opt-in and must not be described as exact shared-instance evidence.
- `UNKNOWN` source mode is accepted by default with warning to preserve coverage.
- Enriched matched execution entries previously exposed incomplete per-record test-run status; join-report status counts remain the reliable source unless refreshed.

## Not implemented

- Generic ScenarioExecutor for arbitrary catalog replay.
- Multi-saga runtime scenario execution.
- Runtime fault injection from generated catalog schedules.
- Behavior CSV generation from scenario catalog records.
- Domain-impact scoring.
- Genetic/local search over fault configurations.
- Multi-armed/contextual bandit scenario prioritization.
- TCC verifier/runtime scenario-generation support.
- Stream/gRPC/distributed dynamic evidence parity.
- Semantic deduplication of value-equivalent inputs.
- Profile-aware resolution for ambiguous multiple `@Service` implementations.

## Current next priorities

1. Classify the remaining Quizzes enrichment misses (`AMBIGUOUS=3`, `UNMATCHED=17`) before adding new attribution rules.
2. Implement the smallest attribution refinement justified by that classification, likely command/aggregate/literal pruning rather than broad name matching.
3. Improve exact aggregate-instance key extraction where it affects scenario usefulness.
4. Tighten sidecar metadata completeness, especially per-match test-run status if still stale.
5. Convert the ScenarioExecutor POC into a minimal repeatable executor baseline.
6. Add generated fault injection only after the executor baseline is stable.
7. Add first domain-impact metrics after executable scenarios exist.

## Meeting discussion points

- Should the next thesis slice prioritize better static/dynamic matching or faster movement to executable fault scenarios?
- Is conservative type-only interaction evidence acceptable for early scenario synthesis experiments?
- Should the executor consume JSONL directly, or should the verifier generate simulator behavior files as an adapter format?
- Which first impact metric matters most: invariant violations, compensation divergence, inconsistent final state, or unhandled exceptions?
