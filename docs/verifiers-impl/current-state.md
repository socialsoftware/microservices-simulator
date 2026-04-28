# Verifier current state

Last updated: 2026-04-27

## Scope

The verifier module statically analyses simulator-based applications to extract the semantic structure needed for scenario generation.

Current boundaries:

- Saga-only scope.
- Static extraction and bounded scenario catalog generation.
- Focus on structural facts from source and tests, not runtime fault injection.
- TCC analysis is out of scope unless explicitly requested.
- `applications/dummyapp/` is the synthetic fixture corpus for verifier assumptions.
- `applications/quizzes/` is the main real-world case study and smoke target.

## Implemented

### Application/source discovery

- Application file-tree parsing for Java production files and Groovy test files.
- FQN-to-path indexes for parsed application source.
- Dummyapp parser coverage.

### Static production-code extraction

- Command-handler dispatch target indexing.
- Domain-service classification based on command-handler dispatch structure rather than package names or class suffixes.
- Coordination facades are excluded from domain services when they create unit-of-work objects or execute workflows without being command-handler dispatch targets.
- Command-handler mapping supports:
  - field and constructor service injection;
  - interface injection when exactly one implementation exists;
  - conservative skip for ambiguous multiple implementations;
  - one-level local helper delegation;
  - aggregate type constants;
  - overloaded service methods through signature-keyed access policies.
- Service access-policy extraction supports:
  - direct `registerChanged(...)` calls;
  - unit-of-work service aliases;
  - trivial getters;
  - one-level local helpers wrapping writes.
- Saga/workflow extraction supports:
  - saga functionality discovery;
  - ordered steps;
  - predecessor/dependency edges;
  - dispatch footprints;
  - forward vs compensation dispatch phases;
  - single, static-repeat, and parametric-repeat multiplicity;
  - conservative handling of unresolved dependency references.
- Saga constructor parameter signatures are collected for replay-oriented trace metadata.

### Groovy test/input extraction

- Groovy test source indexing.
- Direct saga constructor tracing from tests.
- Helper-chain tracing for constructor inputs.
- Structured value recipes for constructor arguments.
- Facade/functionality call extraction into saga construction recipes.
- Assignment and bare-statement facade calls are represented as `FACADE_CALL` trace origins.
- Common helper-return patterns no longer appear as fake cyclic references.
- Local collection transforms such as `toSet()` and cast/coercion shapes are represented as local transforms.
- Replay-oriented metadata classifies unresolved values as source placeholders, injectable placeholders, runtime calls, unknown unresolved values, or resolved values.
- Expected constructor argument types are attached when available.

### Human reports

- HTML analysis report generation.
- Stable latest report path remains `analysis-report.html` unless configured otherwise.
- Timestamped archived HTML sibling is also written.
- Groovy traces are surfaced with origin, readable provenance, replay categories, and unresolved summaries.

### Scenario catalog export

- Normalized scenario IR under `verifiers/src/main/java/.../faults/scenario/`.
- Deterministic scenario IDs.
- Defensive copies in scenario model records.
- Pure bounded scenario generator.
- Single-saga scenarios emitted before multi-saga candidates.
- Conflict evidence ignores read/read pairs and models write/read or write/write interactions.
- Connected saga-set enumeration is bounded by configuration.
- Input tuple joining rejects incompatible exact logical-key bindings.
- Schedule enumeration preserves intra-saga order and reports caps only when useful branches are truncated.
- JSONL catalog writer emits one `ScenarioPlan` per line.
- Manifest writer records schema, generated timestamp, effective config, counts, warnings, and output paths.
- CLI wiring runs export after HTML generation when `verifiers.scenario-catalog.enabled=true`.
- Catalog export is disabled by default.

## Current safe defaults for scenario catalog export

- `enabled=false`
- `includeSingles=true`
- `maxSagaSetSize=1`
- `maxScenarios=100`
- `maxInputVariantsPerSaga=3`
- `maxSchedulesPerInputTuple=20`
- `allowTypeOnlyFallback=false`
- `inputPolicy=RESOLVED_OR_REPLAYABLE`
- `scheduleStrategy=SERIAL`
- `deterministicSeed=1234`

## Partially implemented / current limitations

- Exact aggregate-instance key extraction is still incomplete.
- Dispatch footprints usually remain type-only unless exact key data is available.
- Missing aggregate names remain non-matchable/unknown rather than being mislabeled as type-only.
- Type-only fallback is opt-in and should not be described as exact shared-instance evidence.
- Groovy recipes are replay-oriented, but no runtime materializer exists yet.
- Scenario catalog records are executor-facing, but not directly executed by the simulator yet.
- Segment-compressed scheduling exists as a strategy concept, but the current safe/default path is serial or bounded order-preserving scheduling; verify before relying on compression for evaluation claims.
- Include/exclude filters from the broader scenario-catalog design are not exposed yet.
- Effective Spring binding of all scenario-catalog properties should receive stronger coverage; current assertions include text/config-level checks.
- Quizzes-scale smoke checks should validate invariants such as parseability, unique scenario IDs, known schedule references, and report generation rather than exact scenario counts.

## Not implemented

- ScenarioExecutor / generic scenario runner.
- Runtime saga/functionality materialization from catalog recipes.
- Behavior CSV generation from scenario catalog records.
- Runtime fault injection from generated catalog schedules.
- Execution trace/domain-state impact scoring.
- Genetic Algorithm search over fault bit vectors.
- Multi-armed bandit or contextual bandit scenario picker.
- TCC verifier/scenario-generation support.
- Profile-aware resolution for ambiguous multiple `@Service` implementations.

## Validation baseline

Recent documentation sources report focused verifier specs and full verifier runs during earlier implementation phases. Before using this file for a report or milestone claim, refresh the validation baseline with:

```bash
cd verifiers && mvn test
```

For scenario catalog work, useful focused commands include:

```bash
cd verifiers
mvn test -Dtest=ScenarioModelSpec,ScenarioGeneratorSpec
mvn test -Dtest=ScenarioCatalogJsonlWriterSpec
mvn test -Dtest=ApplicationAnalysisScenarioModelAdapterSpec,ScenarioGeneratorApplicationSpec
```

## Current next priorities

1. Run or refresh a bounded Quizzes smoke with catalog export enabled and small caps.
2. Inspect catalog manifest diagnostics for skipped traces, unresolved inputs, type-only footprints, and sagas without usable inputs.
3. Improve exact aggregate-instance key extraction from input variants into step footprints.
4. Design the minimal ScenarioExecutor/generic runner contract over the JSONL catalog.
5. Decide whether behavior CSV is a generated adapter format or whether JSONL remains the primary runtime contract.
6. After executable scenarios exist, implement impact scoring and then search components.

## Meeting discussion points

- Is the current conservative type-only fallback acceptable for early scenario synthesis experiments, or should multi-saga generation wait for stronger exact-key extraction?
- Should the executor consume JSONL directly, or should the verifier generate simulator behavior files as an intermediate artifact?
- Which impact metrics should be prioritized first: invariant violations, unhandled exceptions, compensation failures, state divergence, latency, or a combination?
- What minimum Quizzes smoke evidence is sufficient before moving from static generation to runtime execution?
