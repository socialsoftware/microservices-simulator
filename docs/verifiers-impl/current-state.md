# Verifier current state

Last updated: 2026-04-30

## Scope

The verifier module statically analyses simulator-based applications to extract the semantic structure needed for scenario generation.

Current boundaries:

- Saga-catalog scope with source-mode classification for SAGAS/TCC/MIXED/UNKNOWN test inputs.
- Static extraction and bounded scenario catalog generation.
- Focus on structural facts from source and tests, not runtime fault injection.
- TCC runtime analysis/execution remains out of scope; TCC-classified saga-catalog candidates are rejected and exported diagnostically.
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
- Source-mode classification is attached to Groovy full traces and scenario input variants using generic simulator/Spring evidence:
  - `@Autowired` saga/causal unit-of-work service fields;
  - nested/local `@TestConfiguration` bean evidence;
  - explicit `@ActiveProfiles`, `@TestPropertySource`, and `@SpringBootTest(properties=...)` profile evidence.
- Conflicting evidence classifies as `MIXED`; unsupported aliases/placeholders are not guessed.

### Human reports

- HTML analysis report generation.
- Each verifier run writes to a dedicated run directory under `verifiers/output` by default, named `<application>-<yyyyMMdd-HHmmss-SSS>/`.
- Stable artifact filenames live inside that run directory, e.g. `analysis-report.html` unless configured otherwise.
- Groovy traces are surfaced with origin, readable provenance, replay categories, and unresolved summaries.
- Source-mode classification is surfaced in the Groovy Trace Explorer through a summary table, per-trace source-mode/confidence chips, and per-trace evidence tables.

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
- Source-mode policy for saga catalogs is applied before scenario planning:
  - `SAGAS` accepted;
  - `TCC` rejected;
  - `MIXED` rejected;
  - `UNKNOWN` accepted with a warning.
- JSONL catalog writer emits one accepted `ScenarioPlan` per line, including input source-mode metadata.
- Rejected TCC/MIXED candidates are written to `scenario-catalog-rejected-inputs.jsonl` with rejection reasons; the file is written even when empty.
- Manifest writer records schema, generated timestamp, effective config, counts, warnings, artifact paths, and source-mode/rejection counters.
- CLI wiring runs export after HTML generation when `verifiers.scenario-catalog.enabled=true`.
- Catalog, manifest, rejected-input, and HTML artifacts use stable filenames inside the per-run output directory.
- The run directory is the archive; per-file timestamp sibling archives are not written and archive-path fields are absent from the manifest.
- Relative configured output paths are resolved under the verifier run output directory, cannot traverse outside it, and cannot cross existing symlink path segments; absolute artifact-path overrides are rejected.
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
- `outputRoot=output`
- run directory name `<application>-<yyyyMMdd-HHmmss-SSS>` under `outputRoot`
- `rejectedInputsPath=scenario-catalog-rejected-inputs.jsonl`

## Partially implemented / current limitations

- Exact aggregate-instance key extraction is still incomplete.
- Dispatch footprints usually remain type-only unless exact key data is available.
- Missing aggregate names remain non-matchable/unknown rather than being mislabeled as type-only.
- Type-only fallback is opt-in and should not be described as exact shared-instance evidence.
- Groovy recipes are replay-oriented, but no runtime materializer exists yet.
- Scenario catalog records are executor-facing, but not directly executed by the simulator yet.
- Segment-compressed scheduling exists as a strategy concept, but the current safe/default path is serial or bounded order-preserving scheduling; verify before relying on compression for evaluation claims.
- Include/exclude filters from the broader scenario-catalog design are not exposed yet.
- Source-mode classification is evidence-based, not a full Spring profile/environment solver.
- Package/name hints are not primary source-mode evidence.
- `UNKNOWN` source mode is accepted by default with a warning to preserve coverage.
- Effective Spring binding of all scenario-catalog properties should receive stronger coverage; current assertions include text/config-level checks.
- Quizzes-scale smoke checks should validate invariants such as parseability, unique scenario IDs, known schedule references, source-mode filtering, rejected-input artifacts, and report generation rather than exact scenario counts.

## Not implemented

- ScenarioExecutor / generic scenario runner.
- Runtime saga/functionality materialization from catalog recipes.
- Behavior CSV generation from scenario catalog records.
- Runtime fault injection from generated catalog schedules.
- Execution trace/domain-state impact scoring.
- Genetic Algorithm search over fault bit vectors.
- Multi-armed bandit or contextual bandit scenario picker.
- TCC verifier/scenario-generation runtime support.
- Profile-aware resolution for ambiguous multiple `@Service` implementations.

## Validation baseline

Refreshed during the source-mode classification workflow on 2026-04-30:

```bash
cd verifiers && mvn test -Dtest=SourceModeClassifierSpec,GroovySourceIndexSpec,GroovyConstructorInputTraceVisitorSpec,GroovyConstructorInputTraceVisitorDummyappSpec,ApplicationAnalysisScenarioModelAdapterSpec,ScenarioGeneratorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,ApplicationsFileTreeParserSpec -q
cd verifiers && mvn test -q
```

Both commands passed in the local agent environment. Before using this file for a later report or milestone claim, refresh with the current branch state.

A bounded Quizzes smoke with catalog export was also run after source-mode workflow completion and refreshed after moving verifier artifacts to per-run directories. The latest run wrote artifacts under `verifiers/output/quizzes-20260430-120345-251/` and produced:

- `inputVariantsAdapted=549`
- `inputVariantsAccepted=468`
- `inputVariantsRejectedBySourceMode=69`
- `inputVariantsExcludedByPolicy=12`
- `scenariosExported=468`
- HTML raw trace source modes before scenario-catalog dedup/filtering: `SAGAS=1323`, `TCC=258`, `MIXED=0`, `UNKNOWN=0`
- accepted source modes: `SAGAS=468`, `TCC=0`, `MIXED=0`, `UNKNOWN=0`
- rejected source-mode reasons: `SOURCE_MODE_TCC_REJECTED_FOR_SAGA_CATALOG=69`

Compared with the previous no-source-mode-filter interpretation of the same strict replayable/non-partial input set (`537` accepted candidates), the classifier removes `69` causal/TCC-derived inputs from the accepted saga catalog and preserves them in `scenario-catalog-rejected-inputs.jsonl` with static evidence.

For scenario catalog and source-mode work, useful focused commands include:

```bash
cd verifiers
mvn test -Dtest=SourceModeClassifierSpec,GroovySourceIndexSpec
mvn test -Dtest=GroovyConstructorInputTraceVisitorSpec,ApplicationAnalysisScenarioModelAdapterSpec,ScenarioGeneratorSpec
mvn test -Dtest=ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,ApplicationsFileTreeParserSpec
mvn test -Dtest=GroovyConstructorInputTraceVisitorDummyappSpec,ScenarioGeneratorApplicationSpec,ApplicationsFileTreeParserSpec,ScenarioGeneratorSpec
```

## Current next priorities

1. Inspect catalog manifest diagnostics for skipped traces, unresolved inputs, type-only footprints, rejected TCC/MIXED inputs, and sagas without usable inputs.
2. Improve exact aggregate-instance key extraction from input variants into step footprints.
3. Design the minimal ScenarioExecutor/generic runner contract over the JSONL catalog.
4. Decide whether behavior CSV is a generated adapter format or whether JSONL remains the primary runtime contract.
5. After executable scenarios exist, implement impact scoring and then search components.
6. Decide whether future TCC-specific catalogs/executors should consume rejected TCC input variants directly.

## Meeting discussion points

- Is the current conservative type-only fallback acceptable for early scenario synthesis experiments, or should multi-saga generation wait for stronger exact-key extraction?
- Should the executor consume JSONL directly, or should the verifier generate simulator behavior files as an intermediate artifact?
- Which impact metrics should be prioritized first: invariant violations, unhandled exceptions, compensation failures, state divergence, latency, or a combination?
- What minimum Quizzes smoke evidence is sufficient before moving from static generation to runtime execution?
