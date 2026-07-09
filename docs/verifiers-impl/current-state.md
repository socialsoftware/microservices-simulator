# Verifier current state

Last updated: 2026-07-09

This is the present-tense status page for verifier/scenario-generation work. Keep it short. Detailed run evidence lives in [`evidence.md`](evidence.md); meeting framing lives in [`advisor-brief.md`](advisor-brief.md).

## One-paragraph summary

The verifier currently extracts saga-oriented scenario structure from simulator applications, including the implemented `EventHandling`/`EventProcessing` event-origin shape, generates deterministic scenario catalogs, and can optionally enrich those catalogs with runtime evidence from existing test executions. It also supports a narrow ScenarioExecutor path for default-vector and explicit binary fault-vector runs on supported materializable saga/local scenarios, including explicit multi-saga deterministic sequential interleaving replay. Broader runtime parity, impact scoring, GA search, and scenario prioritization remain future work.

## Scope

Current implemented scope:

- Saga-catalog generation from Java production code and Groovy/Spock tests.
- Static event semantics for the implemented `EventHandling`/`EventProcessing` chain shape, producing event-origin accepted inputs.
- Source-mode classification for saga/TCC/mixed/unknown test inputs.
- Deterministic bounded scenario catalog generation and accounting that separates static accepted input coverage, static recipe readiness, and ScenarioExecutor materializability.
- Optional dynamic enrichment using simulator runtime evidence as sidecar attribution.
- Narrow ScenarioExecutor support for supported materializable saga/local catalog entries, including single-saga runs and explicit multi-saga deterministic sequential interleaving replay with default or explicit binary fault vectors.

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
- Static event topology for the implemented `EventHandling`/`EventProcessing` shape. The original runtime-evidence target group of five event-driven Quizzes sagas now has accepted static inputs.

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
- Run-level `dynamic-input-map.json` written before the selected-test-class Maven batch.
- Simulator-side runtime attribution emits `inputVariantId` when test owner + functionality class + step name resolve to exactly one static input variant.
- Static input variants now keep provenance, owner, call-context, role, and fixture-origin separately. `sourceMethodName` remains the source/helper that created the input; `callContextMethodName` records the surrounding fixture/feature context when known; owners constrain which Spock feature identities may match at runtime.
- Unmatched dynamic joins carry an additive `unmatchedReason` (`FAILED_TEST_CLASS`, `NOT_SELECTED_TEST_CLASS`, `HELPER_OWNER_MISMATCH`, or `UNCLASSIFIED`) and report `unmatchedReasonCounts` in enriched manifests/join reports.
- Conservative join statuses: `MATCHED_EXACT`, `MATCHED_HIGH_CONFIDENCE`, `MATCHED_PARTIAL`, `AMBIGUOUS`, `UNMATCHED`, `NOT_COVERED`.
- Enriched artifacts are sidecars; `scenario-catalog.jsonl` remains the static contract.
- Plan-local ambiguity containment: direct ids for neighboring inputs are not reused to promote the wrong plan.

### Human reports and packaging

- HTML analysis report generation.
- Per-run output directories with stable artifact names.
- Docker Compose verifier service for full static + dynamic Quizzes-oriented runs.

## Partial / bounded runtime support

### ScenarioExecutor

A narrow verifier-owned ScenarioExecutor path now exists for supported materializable saga/local scenarios. It can load a catalog/enriched catalog, auto-select supported single-saga scenarios, explicitly select supported single-saga or multi-saga scenarios, validate one assigned binary fault vector, materialize only supported inputs, replay the selected schedule, close or compensate participant lifecycle state, and write a standalone v3 participant report. Multi-saga support is deterministic sequential interleaving replay of `expandedSchedule`, not true concurrency or distributed parity. Accounting still reports ScenarioExecutor materializability separately from static `inputRecipe.executorReady` readiness.

This is still not generic scenario execution. The supported path is intentionally narrow: no multi-saga auto-selection, no TCC execution, no stream/gRPC/distributed runtime parity, no true parallel execution, no compensation-step faults, no delay/non-binary impairments, and no impact scoring, GA search, or scenario prioritization.

### Dynamic enrichment

Runtime input attribution is now useful at Quizzes scale: the refreshed post-fixture-ownership baseline produced 435 exact matches, 125 high-confidence matches, zero ambiguous joins, and 24 unmatched records over 584 static scenario records. It still does not use aggregate access evidence, literal runtime values, or aggregate keys deeply enough to reduce all residual unmatched cases.

### Aggregate-instance binding

The verifier often knows aggregate type and access mode, but exact aggregate-instance key extraction is still incomplete.

### Source-mode classification

Source-mode classification is evidence-based. It is not a full Spring profile/environment solver.

## Current evidence

Latest Quizzes static event-semantics count-only comparison after simulator/quizzes API migration:

```text
discovered sagas: 65 -> 68
sagas with accepted inputs: 26 -> 36
sagas without accepted inputs: 39 -> 32
accepted input variants: 517 -> 584
selected input-bound scenario total: 517 -> 584
catalog written: 0 -> 0 (COUNT_ONLY, expected)
staticRecipeReadyInputVariantCount: 0
executorMaterializableInputVariantCount: 94
executorReadyInputVariantCount: 94
blockedInputVariantCount: 490
EVENT_PAYLOAD_PLACEHOLDER blockers: 132
```

Dynamic-enrichment Quizzes baseline against the post-event-semantics static catalog, before fixture/setup ownership diagnostics:

```text
run: verifiers/target/2026-06-29-dynamic-baseline-test-profile/quizzes-20260629-222801-046/
scenario catalog records: 584
runStatus: PARTIAL
test classes selected/passed/failed: 45 / 43 / 2
dynamicEventsRead: 26820
MATCHED_EXACT: 291
MATCHED_HIGH_CONFIDENCE: 109
MATCHED_PARTIAL: 0
AMBIGUOUS: 0
UNMATCHED: 184
NOT_COVERED: 0
```

Current post fixture/setup and feature-helper ownership run:

```text
run: verifiers/target/feature-helper-owner-fix-dynamic-smoke/quizzes-20260630-122219-034/
scenario catalog records: 584
runStatus: PARTIAL
test classes selected/passed/failed: 45 / 43 / 2
dynamicEventsRead: 26815
MATCHED_EXACT: 435
MATCHED_HIGH_CONFIDENCE: 125
MATCHED_PARTIAL: 0
AMBIGUOUS: 0
UNMATCHED: 24
NOT_COVERED: 0
unmatchedReasonCounts: FAILED_TEST_CLASS=8, NOT_SELECTED_TEST_CLASS=7, HELPER_OWNER_MISMATCH=0, UNCLASSIFIED=9
```

Segment-compressed scheduling Quizzes count-only comparison:

```text
ORDER_PRESERVING_INTERLEAVING selected total: 218528454
SEGMENT_COMPRESSED selected total: 1019393
```

ScenarioExecutor Quizzes smoke:

```text
Single-saga catalog: verifiers/target/quizzes-20260708-163552-193/scenario-catalog.jsonl
Single-saga scenario plan id: 910f72907e0d901bc5d35e0ecea03ec920b7ffb63929bbba1bfdba4fe531e195
Single-saga explicit vector: 1 -> COMPENSATED / participant COMPENSATED

Multi-saga catalog: verifiers/target/multi-saga-executor-planning-audit/quizzes-20260709-004627-310/scenario-catalog.jsonl
Multi-saga scenario plan id: 0945caa9ac2fe06a268e6df6aa992fcf69e253116264684d577bdbbb955c2e25
Multi-saga default vector: 00000 -> PARTIAL_COMPENSATED / participants COMPENSATED + COMMITTED
```

Those runs relied on executor runtime-owned argument resolution plus the new in-memory fault-vector provider. Accounting still separates static recipe readiness (`staticRecipeReadyInputVariantCount=0`) from ScenarioExecutor materializability (`executorMaterializableInputVariantCount=94`). The remaining 490 blocked variants include event-origin inputs blocked by event payload placeholders; accepted static inputs are not automatically executor-ready.

See [`evidence.md`](evidence.md) for commands, run paths, and interpretation.

## Current limitations

- Static event topology exists for the implemented `EventHandling`/`EventProcessing` shape; other event shapes still need evidence before being claimed.
- Exact aggregate-instance key extraction is incomplete.
- The remaining 32 Quizzes sagas without accepted static inputs need classification. Missing accepted input means no accepted static `InputVariant` was discovered; it does not mean no test exists.
- Dynamic enrichment is local/sagas-focused; stream/gRPC/distributed/TCC parity is not established.
- Dynamic evidence is additive sidecar evidence and does not redefine or create static scenario structure. The current post fixture/setup ownership baseline has zero ambiguous joins and reduced unmatched records from `184` to `24`, but the residual unmatched records remain caveats rather than validated scenarios.
- Same-feature sibling ambiguity can still return when current evidence cannot distinguish neighboring static inputs, even though the latest Quizzes baseline did not expose ambiguity.
- Groovy input recipes are replay-oriented, but generic materialization/replay is incomplete. Event payload placeholders remain materialization blockers.
- Segment compression is a static reduction under extracted conflict evidence, not proof of semantic completeness.
- Type-only fallback is opt-in and must not be described as exact shared-instance evidence.
- `UNKNOWN` source mode is accepted by default with warning to preserve coverage.
- Dynamic enrichment baselines must run Quizzes tests with the `test` profile active (`SPRING_PROFILES_ACTIVE=test,sagas,local`); without it, async `@SpringBootTest` classes can fail before evidence collection because the datasource profile is missing.

## Not implemented

- Generic ScenarioExecutor for arbitrary catalog replay.
- Multi-saga auto-selection and generic distributed/runtime-parity multi-saga execution.
- TCC runtime scenario execution.
- Stream/gRPC/distributed runtime parity for scenario execution.
- Compensation-step faults, delay injection, and other non-binary impairments.
- Behavior CSV generation from scenario catalog records.
- Domain-impact scoring.
- Genetic/local search over fault configurations.
- Multi-armed/contextual bandit scenario prioritization.
- TCC verifier/runtime scenario-generation support.
- Stream/gRPC/distributed dynamic evidence parity.
- Semantic deduplication of value-equivalent inputs.
- Profile-aware resolution for ambiguous multiple `@Service` implementations.

## Current next priorities

1. Finalize and classify the remaining 32 Quizzes sagas without accepted static inputs.
2. Triage the remaining `UNMATCHED=24` dynamic records, especially the `UNCLASSIFIED=9` residuals, before deciding whether aggregate-key/runtime-value matching is worth improving before executor work.
3. Improve event payload reconstruction and materialization/replay for event-origin inputs.
4. Extend the ScenarioExecutor beyond the current supported saga/local deterministic replay path only after materialization improves.
5. Add first domain-impact metrics after broader executable scenarios exist.

## Meeting discussion points

- Should the next thesis slice prioritize better static/dynamic matching or faster movement to executable fault scenarios?
- Is conservative type-only interaction evidence acceptable for early scenario synthesis experiments?
- Should the executor consume JSONL directly, or should the verifier generate simulator behavior files as an adapter format?
- Which first impact metric matters most: invariant violations, compensation divergence, inconsistent final state, or unhandled exceptions?
