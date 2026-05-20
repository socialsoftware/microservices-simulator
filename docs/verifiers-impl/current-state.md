# Verifier current state

Last updated: 2026-05-20

This page is the present-tense source of truth for verifier implementation status. Use it before relying on roadmap notes, weekly logs, meeting notes, decision records, or archived material.

For terminology used here, especially scenario/input and dynamic-enrichment status labels, see [`glossary.md`](glossary.md). For the embedded structured input recipe export contract, see [`structured-input-recipes.md`](structured-input-recipes.md).

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
- Structured value recipes for constructor arguments, exported as embedded `inputRecipe` payloads on scenario input variants.
- Facade/functionality call extraction into saga construction recipes.
- Assignment and bare-statement facade calls are represented as `FACADE_CALL` trace origins.
- Common helper-return patterns no longer appear as fake cyclic references.
- Local collection transforms such as `toSet()` and cast/coercion shapes are represented as local transforms.
- Replay-oriented metadata classifies unresolved values as source placeholders, injectable placeholders, runtime calls, unknown unresolved values, or resolved values.
- Expected constructor argument types are attached when available.
- Public input recipes cover literals, constructors, DTO named-argument/setter/property assignments, collections, local transforms/coercions, helper results, property access, generic call results, placeholders, and unresolved values. Recipe readiness and deterministic blockers are explicit; existing summaries/provenance remain diagnostics.
- Analyzed input variants now carry explicit ownership metadata separate from source provenance. Provenance fields such as source class, source method, and source binding explain where the input was found; owners identify the feature methods allowed to claim that input during runtime attribution.
- Ownership metadata covers direct feature inputs, helper-created inputs, `setup()` fixtures, field initializers, inherited helper/setup/field variants, and `setupSpec()` analysis metadata. `setupSpec()` runtime-attribution improvement is not implemented or claimed.
- Source-mode classification is attached to Groovy full traces and scenario input variants using generic simulator/Spring evidence:
  - `@Autowired` saga/causal unit-of-work service fields;
  - nested/local `@TestConfiguration` bean evidence;
  - explicit `@ActiveProfiles`, `@TestPropertySource`, and `@SpringBootTest(properties=...)` profile evidence.
- Conflicting evidence classifies as `MIXED`; unsupported aliases/placeholders are not guessed.

### Dynamic enrichment foundation

- Disabled-by-default `verifiers.dynamic-enrichment` config is present in `verifiers/src/main/resources/application.yaml` with include/exclude directory selection, self-test exclusions, timeout, and Maven settings.
- `DynamicEnrichmentConfig` and `DynamicEnrichmentTestClassDiscoveryService` scan Groovy/Java test files under the configured test source root, normalize/reject traversal, derive FQNs from package declarations or path conventions, and return deterministic sorted class names.
- `DynamicEvidenceReader` recursively ingests `dynamic-evidence.jsonl` files, captures source path/line metadata, and reports malformed lines as warnings.
- `DynamicEvidenceJoiner` provides a pure, conservative scenario-plan-to-evidence transformation with the approved join statuses and observed step/command/aggregate summaries.
- `EnrichedScenarioCatalogWriter` writes sidecar-only enriched JSONL, manifest, and join-report artifacts; the original `scenario-catalog.jsonl` writer remains unchanged.
- `DummyappDynamicEnrichmentIntegrationSpec` exercises the dummyapp static catalog plus synthetic dynamic-evidence bridge end-to-end with the same sidecar schema planned for Quizzes, proving `MATCHED_HIGH_CONFIDENCE` and `NOT_COVERED` paths before runtime wiring.
- `ProcessRunner` / `DefaultProcessRunner` and `DynamicEnrichmentOrchestrator` run selected application test classes one by one with Maven argument lists, per-class evidence directories, `test-run.json`, captured `maven-output.log`, interrupt-safe cleanup, and run-relative output-path guards.
- `DynamicInputMapWriter` now writes a per-test-class `dynamic-input-map.json` before each dynamic Maven class run. The map is built only from accepted final `ScenarioPlan.inputs()` for the selected test class and includes input variant ids, static saga FQNs, source method/provenance metadata, explicit owners, static step-name hints, simple literal argument hints, aggregate-type hints from conflict evidence when available, scenario plan ids, and diagnostic source/provenance text.
- The simulator can now load that per-class `dynamic-input-map.json` and attach `inputVariantId` to runtime evidence when the current test identity matches an input owner and the runtime functionality class FQN and runtime step name resolve to exactly one static input variant.
- Runtime attribution is intentionally conservative:
  - one candidate writes top-level `inputVariantId` plus `payload.inputVariantAttributionStatus=MATCHED`;
  - zero candidates records `NO_MATCH` diagnostics on step events;
  - multiple candidates records `AMBIGUOUS` with candidate ids and does not guess an `inputVariantId`.
- Quantification uses the existing enriched manifest/join-report status counts:
  - `MATCHED_EXACT` means dynamic evidence carried a direct `inputVariantId` that belongs to the scenario plan;
  - `MATCHED_HIGH_CONFIDENCE` means dynamic evidence matched through test identity + static semantic shape, but no direct input id was present;
  - `AMBIGUOUS` means dynamic evidence was relevant but still mapped to more than one possible static input;
  - `NOT_COVERED` means no useful runtime evidence was seen for that scenario.
- `DummyappDynamicEnrichmentIntegrationSpec` now checks the before/after shape for the same selected dummyapp plan: semantic-only evidence produces one `MATCHED_HIGH_CONFIDENCE` record, while the same events with direct runtime `inputVariantId` produce one `MATCHED_EXACT` record.
- Verifier fallback matching uses the same ownership semantics as simulator runtime attribution when direct `inputVariantId` evidence is absent. It keeps the existing status model: unique complete-identity owner matches become `MATCHED_HIGH_CONFIDENCE`, incomplete identity remains capped at `MATCHED_PARTIAL`, and direct runtime ids still produce `MATCHED_EXACT` for the owning plan.
- Dynamic joining now contains plan-local ambiguity containment: foreign direct input ids are not reused to promote neighboring plans through semantic fallback, and `AMBIGUOUS` is scoped to plans whose own inputs participate in the relevant ambiguous identity set. Same-feature sibling ambiguity can remain when current evidence cannot distinguish sibling inputs.
- A refreshed full/default Quizzes sagas-only run after runtime input attribution moved the comparable baseline from `MATCHED_EXACT=0`, `MATCHED_HIGH_CONFIDENCE=2`, `AMBIGUOUS=44`, `UNMATCHED=20`, `warningCount=8238` to `MATCHED_EXACT=46`, `MATCHED_HIGH_CONFIDENCE=0`, `AMBIGUOUS=3`, `UNMATCHED=17`, `warningCount=328`.
- The orchestrated Maven command appends simulator/test-context flags per class:
  - `-Dsimulator.dynamic-evidence.enabled=true`
  - `-Dsimulator.dynamic-evidence.test-context.enabled=true`
  - `-Djunit.platform.listeners.autodetection.enabled=true`
  - `-Dsimulator.dynamic-evidence.output-dir=<run-dir>/dynamic-evidence/<safe-test-class-fqn>`
  - `-Dsimulator.dynamic-evidence.input-map-path=<run-dir>/dynamic-evidence/<safe-test-class-fqn>/dynamic-input-map.json`
  - `-Dsimulator.dynamic-evidence.application-name=<application-base-dir>`
- `ScenarioGeneratorApplication` wires dynamic enrichment after static catalog export when `verifiers.dynamic-enrichment.enabled=true`, using the same per-run output directory for HTML, static catalog, dynamic evidence, and enriched sidecars.
- Docker Compose `fault-analysis-scenario-gen` now enables the full static + dynamic flow with `VERIFIERS_OUTPUT_ROOT=/reports`, run-relative `VERIFIERS_REPORT_HTML_PATH=analysis-report.html`, scenario-catalog and dynamic-enrichment flags enabled, partial mode enabled, and sagas-only Quizzes selection.
- Dynamic enrichment fails fast if enabled while scenario catalog export is disabled; partial test-run handling is enabled by default and strict mode preserves artifacts before failing.

### Dynamic-enrichment artifact layout (same run directory)

When enrichment is enabled, each verifier run directory now contains:

- `analysis-report.html`
- `scenario-catalog.jsonl`
- `scenario-catalog-manifest.json`
- `scenario-catalog-rejected-inputs.jsonl`
- `dynamic-evidence/<safe-test-class-fqn>/dynamic-evidence.jsonl`
- `dynamic-evidence/<safe-test-class-fqn>/dynamic-evidence-manifest.json`
- `dynamic-evidence/<safe-test-class-fqn>/dynamic-input-map.json`
- `dynamic-evidence/<safe-test-class-fqn>/test-run.json`
- `dynamic-evidence/<safe-test-class-fqn>/maven-output.log`
- `scenario-catalog-enriched.jsonl`
- `scenario-catalog-enriched-manifest.json`
- `dynamic-evidence-join-report.json`

The enriched artifacts are sidecars. `scenario-catalog.jsonl` stays unchanged as the static contract.

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
- JSONL catalog writer emits one accepted `ScenarioPlan` per line with schema `microservices-simulator.scenario-catalog.v2`, including input source-mode metadata and embedded `inputRecipe` payloads when available.
- Rejected TCC/MIXED candidates are written to `scenario-catalog-rejected-inputs.jsonl` with schema `microservices-simulator.scenario-catalog-rejected-input.v2`, wrapping the full input object plus `rejectionReason` and `rejectionWarnings`; the file is written even when empty.
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

## Current safe defaults for dynamic enrichment

- `enabled=false`
- `allowPartialTestRun=true`
- `dynamicEvidenceSubdir=dynamic-evidence`
- `enrichedCatalogPath=scenario-catalog-enriched.jsonl`
- `enrichedManifestPath=scenario-catalog-enriched-manifest.json`
- `joinReportPath=dynamic-evidence-join-report.json`
- `testSourceRoot=src/test/groovy`
- `includeTestDirs=[]`
- `excludeTestDirs=[]`
- `excludeTestClasses=[CreateTournamentDynamicEvidenceSmokeTest, DynamicEvidenceDisabledSmokeTest]`
- `perTestTimeoutSeconds=300`
- `maven.executable=mvn`
- `maven.profile=test-sagas`

### Related simulator/test-context defaults and orchestration flags

- Simulator dynamic evidence is also disabled by default:
  - `simulator.dynamic-evidence.enabled=false`
  - `simulator.dynamic-evidence.test-context.enabled=false`
- Verifier orchestration enables both properties per Maven class-run and also sets:
  - `junit.platform.listeners.autodetection.enabled=true`
  - `simulator.dynamic-evidence.input-map-path=<per-class dynamic-input-map.json>`
- This keeps generic test-context capture infrastructure opt-in for non-orchestrated runs while requiring no Quizzes-specific hooks.

## Partially implemented / current limitations

- Exact aggregate-instance key extraction is still incomplete.
- Dynamic enrichment can now propagate runtime `inputVariantId` for the ownership-aware exact case: current test identity owner + runtime functionality class FQN + runtime step name must leave exactly one static input candidate. It does not yet use command fields, aggregate access evidence, literal runtime values, or aggregate keys to reduce ambiguous candidates further.
- First-pass propagation mainly upgrades match certainty by turning runtime events with direct `inputVariantId` into `MATCHED_EXACT`. In Quizzes it also reduced broad semantic ambiguity, but it only modestly increased non-unmatched coverage (`46 -> 49` plans).
- No Quizzes source/test hooks are required or used by orchestration; this is intentional and should be preserved.
- Dynamic evidence/runtime parity is still local/sagas only:
  - no stream/gRPC instrumentation parity;
  - no distributed runtime orchestration parity;
  - no causal/TCC runtime parity.
- Enrichment remains sidecar-only; `scenario-catalog.jsonl` is intentionally unchanged.
- Partial test-run behavior is the default (`allowPartialTestRun=true`); strict mode is opt-in.
- Dispatch footprints usually remain type-only unless exact key data is available.
- Missing aggregate names remain non-matchable/unknown rather than being mislabeled as type-only.
- Type-only fallback is opt-in and should not be described as exact shared-instance evidence.
- Groovy recipes are replay-oriented, but no runtime materializer exists yet.
- Scenario catalog records are executor-facing, but not directly executed by the simulator yet.
- Semantic deduplication of value-equivalent inputs and stronger same-feature sibling disambiguation are not implemented.
- Segment-compressed scheduling exists as a strategy concept, but the current safe/default path is serial or bounded order-preserving scheduling; verify before relying on compression for evaluation claims.
- Include/exclude filters from the broader scenario-catalog design are not exposed yet.
- Source-mode classification is evidence-based, not a full Spring profile/environment solver.
- Package/name hints are not primary source-mode evidence.
- `UNKNOWN` source mode is accepted by default with a warning to preserve coverage.
- Full/default Quizzes enrichment is no longer dominated by ambiguity after first-pass runtime input attribution, but the remaining `AMBIGUOUS=3` and `UNMATCHED=17` records still need interpretation as diagnostic output rather than proof of exact coverage.
- Remaining ambiguous Quizzes records are cases where runtime did emit direct `inputVariantId`s, but the direct ids belonged to neighboring static input variants with the same test/functionality/step shape rather than the scenario plan being enriched. The joiner correctly avoids guessing in those cases.
- Enriched matched execution entries still show `testRunStatus: null` even though join-report per-class status counts are present; treat per-record run status as provisional until this is tightened.

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

Dynamic-enrichment foundation validation performed in the local agent environment:

- `cd verifiers && mvn test -Dtest=DynamicEnrichmentTestDiscoverySpec -DfailIfNoTests=false` — PASS.
- `cd verifiers && mvn test -Dtest=DynamicEnrichmentTestDiscoverySpec,VerifiersConfigurationSpec,ScenarioGeneratorApplicationSpec -DfailIfNoTests=false` — PASS.
- `DynamicEnrichmentTestDiscoverySpec` now also covers symlink-escape skipping and package declarations with whitespace around dots.
- `cd verifiers && mvn test -Dtest=DynamicEvidenceReaderSpec,DynamicEvidenceJoinerSpec,EnrichedScenarioCatalogWriterSpec -DfailIfNoTests=false` — PASS (`13` tests, `0` failures/errors).
- `cd verifiers && mvn test -Dtest=DynamicEvidenceReaderSpec,DynamicEvidenceJoinerSpec,EnrichedScenarioCatalogWriterSpec,DynamicEnrichmentTestDiscoverySpec,VerifiersConfigurationSpec -DfailIfNoTests=false` — PASS (`26` tests, `0` failures/errors).
- `DynamicEvidenceJoinerSpec` now includes a regression for duplicate simple saga names across scenario plans, confirming the joiner emits `AMBIGUOUS` with candidate warnings when runtime evidence only provides an unqualified saga name.
- `DynamicEvidenceJoinerSpec` also covers expanded static schedule labels like `...::reserve#0` against raw runtime `reserve` evidence, and now separately guards against runtime suffixes like `reserve#0` being normalized away.

Dynamic-enrichment orchestration validation performed in the local agent environment:

- `cd verifiers && mvn test -Dtest=DefaultProcessRunnerSpec,DynamicEnrichmentOrchestratorSpec,ScenarioGeneratorApplicationSpec -DfailIfNoTests=false` — PASS (`22` tests, `0` failures/errors).
- `cd verifiers && mvn test -Dtest='*Dynamic*Spec,*Enriched*Spec' -DfailIfNoTests=false` — PASS (`29` tests, `0` failures/errors).
- `cd verifiers && mvn test` — PASS (`206` tests, `0` failures/errors).
- The orchestrator specs use a fake process runner to prove Maven command construction, interrupt-safe cleanup, run-relative output guards, partial vs strict behavior, per-class artifacts, same-run-directory sidecars, and the scenario-catalog-required startup guard without launching Quizzes tests.

Docker runtime/service validation performed in the local agent environment:

- `docker compose build fault-analysis-scenario-gen` — PASS.
- `docker compose run --rm --no-deps --entrypoint mvn fault-analysis-scenario-gen -version` — PASS (Maven 3.9.7 with Java 21 in the final image).

Dummyapp dynamic-enrichment integration validation performed in the local agent environment:

- `cd verifiers && mvn test -Dtest=DummyappDynamicEnrichmentIntegrationSpec -DfailIfNoTests=false` — PASS.
- `cd verifiers && mvn test -Dtest=DummyappDynamicEnrichmentIntegrationSpec,DynamicEvidenceJoinerSpec,EnrichedScenarioCatalogWriterSpec -DfailIfNoTests=false` — PASS (`12` tests, `0` failures/errors).
- `cd verifiers && mvn test -Dtest=ScenarioGeneratorApplicationSpec,ApplicationAnalysisScenarioModelAdapterSpec,DummyappDynamicEnrichmentIntegrationSpec -DfailIfNoTests=false` — PASS (`23` tests, `0` failures/errors).
- The first failing-first run exposed two integration details that are now encoded in the spec: the runtime evidence should use raw step names like `getOrderStep`, while the joiner only normalizes expanded static schedule labels like `getOrderStep#0` and leaves runtime suffixes untouched; dynamic enrichment counts for `dynamicEventsRead` / `eventsMissingTestContext` live in the join report rather than the enriched-catalog manifest.

Real Quizzes dynamic-enrichment validation (Tasks 7-8) was performed in the local agent environment:

- Narrow orchestration smoke:
  - command: `cd verifiers && mvn spring-boot:run -Dspring-boot.run.arguments="--verifiers.applications-root=/Users/andre/meic/thesis/microservices-simulator/applications --verifiers.application-base-dir=quizzes --verifiers.output-root=/Users/andre/meic/thesis/microservices-simulator/verifiers/target/task7-quizzes-smoke-final2 --verifiers.report-html-path=analysis-report.html --verifiers.scenario-catalog.enabled=true --verifiers.dynamic-enrichment.enabled=true --verifiers.dynamic-enrichment.allow-partial-test-run=true --verifiers.dynamic-enrichment.include-test-dirs=pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/behaviour --verifiers.dynamic-enrichment.exclude-test-dirs=pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/behaviour/execution --verifiers.dynamic-enrichment.exclude-test-classes=CreateTournamentDynamicEvidenceSmokeTest,DynamicEvidenceDisabledSmokeTest,AbortUpdateAndRetryTest,AddParticipantAndRecoverTest,AddParticipantWithDelaysTest,GenerateBehaviourTest,HandleEventBehaviour --verifiers.scenario-catalog.max-scenarios=200 --verifiers.scenario-catalog.max-input-variants-per-saga=20"`
  - run dir: `verifiers/target/task7-quizzes-smoke-final2/quizzes-20260501-124230-037/`
  - result: `runStatus=COMPLETE`, `testClassesSelected=2`, `testClassesPassed=2`, `dynamicEventsRead=362`, `eventsMissingTestContext=0`, join counts `MATCHED_HIGH_CONFIDENCE=1`, `UNMATCHED=199`.
- Full/default sagas-only orchestration (partial default behavior):
  - command: `cd verifiers && mvn spring-boot:run -Dspring-boot.run.arguments="--verifiers.applications-root=/Users/andre/meic/thesis/microservices-simulator/applications --verifiers.application-base-dir=quizzes --verifiers.output-root=/Users/andre/meic/thesis/microservices-simulator/verifiers/target/task8-quizzes-full --verifiers.report-html-path=analysis-report.html --verifiers.scenario-catalog.enabled=true --verifiers.dynamic-enrichment.enabled=true --verifiers.dynamic-enrichment.allow-partial-test-run=true --verifiers.dynamic-enrichment.include-test-dirs=pt/ulisboa/tecnico/socialsoftware/quizzes/sagas --verifiers.dynamic-enrichment.exclude-test-dirs=pt/ulisboa/tecnico/socialsoftware/quizzes/causal,pt/ulisboa/tecnico/socialsoftware/quizzes/tcc"`
  - run dir: `verifiers/target/task8-quizzes-full/quizzes-20260501-132052-052/`
  - result: `runStatus=PARTIAL`, `testClassesSelected=42`, `testClassesPassed=40`, `testClassesFailed=2`, `evidenceFilesRead=42`, `dynamicEventsRead=18868`, `eventsMissingTestContext=0`.
  - join counts: `MATCHED_HIGH_CONFIDENCE=2`, `AMBIGUOUS=44`, `UNMATCHED=20`, `NOT_COVERED=0`.
  - enriched manifest caveat: `counts.warningCount=8238` on the same run; warnings are dominated by repeated ambiguity candidate diagnostics.
  - sidecar caveat: enriched `matchedTestExecutions[].testRunStatus` is still `null` in this run, while `dynamic-evidence-join-report.json` correctly records per-class statuses.
- Refreshed full/default sagas-only orchestration after runtime `inputVariantId` attribution:
  - command: `cd verifiers && mvn spring-boot:run -Dspring-boot.run.arguments="--verifiers.applications-root=/Users/andre/meic/thesis/microservices-simulator/applications --verifiers.application-base-dir=quizzes --verifiers.output-root=/private/tmp/ms-simulator-quizzes-measure-20260512-sagas-only --verifiers.scenario-catalog.enabled=true --verifiers.dynamic-enrichment.enabled=true --verifiers.dynamic-enrichment.allow-partial-test-run=true --verifiers.dynamic-enrichment.include-test-dirs=pt/ulisboa/tecnico/socialsoftware/quizzes/sagas --verifiers.dynamic-enrichment.exclude-test-dirs=pt/ulisboa/tecnico/socialsoftware/quizzes/causal,pt/ulisboa/tecnico/socialsoftware/quizzes/tcc --verifiers.scenario-catalog.max-scenarios=100 --verifiers.scenario-catalog.max-input-variants-per-saga=3 --verifiers.scenario-catalog.max-schedules-per-input-tuple=20"`
  - run dir: `/private/tmp/ms-simulator-quizzes-measure-20260512-sagas-only/quizzes-20260512-184921-906/`
  - result: `runStatus=PARTIAL`, `testClassesSelected=42`, `testClassesPassed=40`, `testClassesFailed=2`, `evidenceFilesRead=42`, `dynamicEventsRead=18868`, `eventsMissingTestContext=0`.
  - join counts: `MATCHED_EXACT=46`, `MATCHED_HIGH_CONFIDENCE=0`, `MATCHED_PARTIAL=0`, `AMBIGUOUS=3`, `UNMATCHED=17`, `NOT_COVERED=0`.
  - enriched manifest: `recordCount=66`, `warningCount=328`, `testRunStatusCounts={PASSED=40, FAILED=2}`.
  - runtime attribution diagnostics: `226` step events carried `payload.inputVariantAttributionStatus=MATCHED`; `270` command/aggregate events carried the propagated top-level `inputVariantId` without a step attribution status; `8096` step events reported `NO_MATCH`.
  - remaining ambiguity examples are `FindQuizFunctionalitySagas.findQuizStep`, `StartQuizFunctionalitySagas.getQuizStep/startQuizStep`, and `RemoveTournamentFunctionalitySagas.getTournamentStep/removeTournamentStep`, where direct runtime ids pointed to other static input variants with the same semantic shape.
  - interpretation: exact scenario attribution improved materially (`0 -> 46` exact plans), ambiguity dropped (`44 -> 3`), and warning volume dropped (`8238 -> 328`). Covered-but-not-exact evidence increased only slightly (`46 -> 49` non-unmatched plans), so this is mainly a precision/certainty improvement rather than proof that every useful static input is now dynamically exercised.
- Failure interpretation for the two Task 8 failed classes was reproduced without enrichment instrumentation:
  - command: `cd applications/quizzes && mvn -Ptest-sagas test -Dtest=pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.AnonymizeStudentAndRemoveStudentTest,pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament.RemoveTournamentAndUpdateTournamentTest`
  - result: FAIL with the same `Expected SimulatorException, but got CompletionException` mismatch, supporting the interpretation that these are existing test/runtime issues rather than orchestration regressions.

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

1. Classify the remaining full-run enrichment misses (`AMBIGUOUS=3`, `UNMATCHED=17`) before adding new attribution rules:
   - decide which records are real static inputs not exercised by the selected Quizzes tests;
   - decide which records are joiner limitations where useful runtime evidence exists but is assigned to a neighboring input variant;
   - decide which records can be improved with command payloads, aggregate access evidence, literal argument hints, or aggregate keys;
   - keep false exactness out of the output: direct `inputVariantId` wins only when it belongs to the scenario plan being enriched.
2. Implement the smallest next attribution refinement supported by the classification results, likely command/aggregate/literal pruning rather than broad name matching.
3. Improve exact aggregate-instance key extraction from input variants into step footprints.
4. Populate enriched `matchedTestExecutions[].testRunStatus` directly from orchestrator test-run metadata, or document why the join report remains the source of truth.
5. Design the minimal ScenarioExecutor/generic runner contract over the JSONL catalog.
6. Decide whether behavior CSV is a generated adapter format or whether JSONL remains the primary runtime contract.
7. After executable scenarios exist, implement impact scoring and then search components.
8. Decide whether future TCC-specific catalogs/executors should consume rejected TCC input variants directly.

## Meeting discussion points

- Is the current conservative type-only fallback acceptable for early scenario synthesis experiments, or should multi-saga generation wait for stronger exact-key extraction?
- Should the executor consume JSONL directly, or should the verifier generate simulator behavior files as an intermediate artifact?
- Which impact metrics should be prioritized first: invariant violations, unhandled exceptions, compensation failures, state divergence, latency, or a combination?
- What minimum Quizzes smoke evidence is sufficient before moving from static generation to runtime execution?
