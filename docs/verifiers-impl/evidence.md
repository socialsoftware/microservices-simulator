# Verifier evidence appendix

Last updated: 2026-06-25

This page stores concrete validation results, metrics, and run references so [`current-state.md`](current-state.md) can stay readable. Treat this as an appendix: cite it when you need proof, not as the first-read narrative.

## Static event semantics Quizzes count-only comparison

Baseline artifact:

```text
verifiers/target/event-semantics-baseline-before/quizzes-20260624-183729-414/scenario-space-accounting.json
```

Post-event-semantics artifact:

```text
verifiers/target/event-semantics-after-current-only/quizzes-20260624-215147-522/scenario-space-accounting.json
```

Both runs used `COUNT_ONLY`; `catalogWritten=0` is expected and is not a failure.

| Metric | Baseline before event semantics | After event semantics |
|---|---:|---:|
| Discovered sagas | 65 | 68 |
| Sagas with accepted static inputs | 26 | 36 |
| Sagas without accepted static inputs | 39 | 32 |
| Accepted input variants | 517 | 584 |
| Selected input-bound scenario total | 517 | 584 |
| Catalog written | 0 | 0 |
| Static recipe-ready input variants | unavailable / old metric 0 | 0 |
| ScenarioExecutor materializable input variants | unavailable | 94 |
| ScenarioExecutor ready input variants | 0 / unavailable | 94 |
| Blocked input variants | 517 | 490 |

Original runtime-evidence target group now covered by accepted static inputs:

- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.RemoveQuizAnswerFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.UpdateUserNameInQuizAnswerFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.RemoveUserFromCourseExecutionFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AnonymizeUserTournamentFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateUserNameFunctionalitySagas`

Newly accepted compared to baseline:

- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.RemoveQuizAnswerFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.UpdateUserNameInQuizAnswerFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.RemoveUserFromCourseExecutionFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.FindQuestionByAggregateIdFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.UpdateQuestionTopicsAsyncFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantAsyncFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AnonymizeUserTournamentFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.CreateTournamentAsyncFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.SolveQuizAsyncFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateUserNameFunctionalitySagas`

Newly discovered compared to baseline:

- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.UpdateQuestionTopicsAsyncFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.CreateTournamentAsyncFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.SolveQuizAsyncFunctionalitySagas`

Remaining post-event sagas without accepted static inputs (classification pending; this does not mean no test exists):

- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.AnswerQuestionFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.ConcludeQuizFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.RemoveQuestionFromQuizAnswerFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.RemoveUserFromQuizAnswerFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.DeleteTopicInQuestionFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.FindQuestionsByCourseFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.RemoveQuestionFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.UpdateQuestionFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.UpdateQuestionTopicsFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.UpdateTopicInQuestionFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.GetAvailableQuizzesFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.RemoveCourseExecutionFromQuizFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.RemoveQuizQuestionFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.UpdateQuestionInQuizFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.UpdateQuizFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas.DeleteTopicFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas.FindTopicsByCourseFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas.GetTopicByIdFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas.UpdateTopicFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.DeleteTopicFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.FindParticipantFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.GetClosedTournamentsForCourseExecutionFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.GetOpenedTournamentsForCourseExecutionFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.GetTournamentsForCourseExecutionFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.InvalidateQuizFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveCourseExecutionFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveUserFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateParticipantAnswerFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateTopicFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.sagas.FindUserByIdFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.sagas.GetStudentsFunctionalitySagas`
- `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.sagas.GetTeachersFunctionalitySagas`

Post-event blocker counts:

```text
EVENT_PAYLOAD_PLACEHOLDER: 132
MISSING_TARGET_TYPE: 753
PROPERTY_RECEIVER_NOT_READY: 778
TRANSFORM_RECEIVER_NOT_READY: 22
UNRESOLVED_VARIABLE: 226
UNKNOWN_VALUE: 214
CALL_RECEIVER_NOT_READY: 120
UNMATERIALIZABLE_ASSIGNMENT: 12
UNRESOLVED_PLACEHOLDER: 15
UNSUPPORTED_TRANSFORM: 1
UNRESOLVED_RUNTIME_EDGE: 1
```

Focused surefire reports already observed:

```text
verifiers/target/surefire-reports/pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.EventHandlingBridgeVisitorDummyappSpec.txt
Tests run: 1, Failures: 0, Errors: 0

verifiers/target/surefire-reports/pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.GroovyConstructorInputTraceVisitorDummyappSpec.txt
Tests run: 24, Failures: 0, Errors: 0

verifiers/target/surefire-reports/pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ApplicationAnalysisScenarioModelAdapterSpec.txt
Tests run: 11, Failures: 0, Errors: 0
```

Interpretation: static event topology improved accepted static input coverage for the implemented `EventHandling`/`EventProcessing` shape, including the original target group of five event-driven sagas. This does not prove dynamic enrichment matches are fixed; the dynamic sidecar flow needs a fresh run against the new static catalog. It also does not make event-origin inputs replayable: event payload placeholders remain materialization blockers, and `executorMaterializableInputVariantCount=94` only means the current ScenarioExecutor can materialize that subset through executor-readiness/runtime-owned handling.

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

Interpretation: a narrow executor POC exists for supported single-saga candidates. It executed this generated plan by resolving runtime-owned infrastructure arguments itself. Older accounting that reported zero executor-ready inputs was measuring static recipe readiness only; executor materializability is now reported separately/aligned with ScenarioExecutor semantics. It is not generic multi-saga execution, fault injection, impact scoring, or search.

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
