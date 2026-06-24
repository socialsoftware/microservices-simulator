# Verifier evidence appendix

Last updated: 2026-06-23

This page stores concrete validation results, metrics, and run references so [`current-state.md`](current-state.md) can stay readable. Treat this as an appendix: cite it when you need proof, not as the first-read narrative.

## Latest dynamic-enrichment Quizzes baseline

Comparable full/default sagas-only Quizzes run after runtime `inputVariantId` attribution:

```text
runStatus: PARTIAL
testClassesSelected: 42
testClassesPassed: 40
testClassesFailed: 2
evidenceFilesRead: 42
dynamicEventsRead: 18868
eventsMissingTestContext: 0
MATCHED_EXACT: 46
MATCHED_HIGH_CONFIDENCE: 0
MATCHED_PARTIAL: 0
AMBIGUOUS: 3
UNMATCHED: 17
NOT_COVERED: 0
warningCount: 328
```

Before runtime input attribution, the comparable baseline was:

```text
MATCHED_EXACT: 0
MATCHED_HIGH_CONFIDENCE: 2
AMBIGUOUS: 44
UNMATCHED: 20
warningCount: 8238
```

Interpretation: direct runtime attribution materially improved exact static/dynamic joining and reduced ambiguity. It only modestly increased non-unmatched coverage, so this is mainly a precision improvement, not proof that all useful static inputs are dynamically exercised.

## Segment-compressed scheduling evidence

Validation commands reported in the local/container environment:

```bash
cd verifiers && mvn -Dtest=ScenarioGeneratorSpec,ScenarioSpaceAccountingCalculatorSpec test
cd verifiers && mvn -Dtest=DummyappAccountingFixtureFoundationSpec,ScenarioGeneratorSpec,ScenarioSpaceAccountingCalculatorSpec test
```

Reported results:

```text
55 tests, 0 failures/errors
66 tests, 0 failures/errors
```

Quizzes count-only comparison with dynamic enrichment disabled and `maxSagaSetSize=3`:

```text
ORDER_PRESERVING_INTERLEAVING selected total: 218528454
SEGMENT_COMPRESSED selected total: 1019393
```

Interpretation: segment compression substantially reduces selected schedule-space counts under the verifier's static conflict-anchor evidence. It does not prove semantic completeness or exact aggregate-instance binding.

## ScenarioExecutor POC smoke

Verified on 2026-05-26 with a real verifier-generated Quizzes catalog through the forked runtime path:

```text
Catalog: verifiers/target/structured-input-recipes-quizzes-smoke/quizzes-20260520-175058-455/scenario-catalog.jsonl
Scenario plan id: 2f0c64a371fcd65b5a38f294ccbda93a42df060c3d1e5b7dcedf43568abcf661
Saga: pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.GetCourseExecutionsFunctionalitySagas
Step: getCourseExecutionsStep
Terminal status: SUCCESS
Report: /tmp/opencode/quizzes-execution-report-get-course-executions.json
```

Interpretation: a narrow executor POC exists for supported single-saga candidates. It is not generic multi-saga execution, fault injection, impact scoring, or search.

## Static source-mode/catalog Quizzes smoke

A bounded Quizzes smoke with catalog export after source-mode workflow completion produced:

```text
inputVariantsAdapted: 549
inputVariantsAccepted: 468
inputVariantsRejectedBySourceMode: 69
inputVariantsExcludedByPolicy: 12
scenariosExported: 468
HTML raw trace source modes before scenario-catalog dedup/filtering: SAGAS=1323, TCC=258, MIXED=0, UNKNOWN=0
accepted source modes: SAGAS=468, TCC=0, MIXED=0, UNKNOWN=0
```

Interpretation: source-mode filtering removed known causal/TCC-derived inputs from the accepted saga catalog and preserved them diagnostically in rejected-input artifacts.

## Useful focused verifier test commands

For scenario catalog and source-mode work:

```bash
cd verifiers
mvn test -Dtest=SourceModeClassifierSpec,GroovySourceIndexSpec
mvn test -Dtest=GroovyConstructorInputTraceVisitorSpec,ApplicationAnalysisScenarioModelAdapterSpec,ScenarioGeneratorSpec
mvn test -Dtest=ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,ApplicationsFileTreeParserSpec
mvn test -Dtest=GroovyConstructorInputTraceVisitorDummyappSpec,ScenarioGeneratorApplicationSpec,ApplicationsFileTreeParserSpec,ScenarioGeneratorSpec
```

For dynamic enrichment components:

```bash
cd verifiers
mvn test -Dtest=DynamicEvidenceReaderSpec,DynamicEvidenceJoinerSpec,EnrichedScenarioCatalogWriterSpec -DfailIfNoTests=false
mvn test -Dtest='*Dynamic*Spec,*Enriched*Spec' -DfailIfNoTests=false
```

Refresh this appendix before using the numbers in final thesis text.
