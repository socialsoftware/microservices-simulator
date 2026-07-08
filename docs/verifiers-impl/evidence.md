# Verifier evidence appendix

Last updated: 2026-07-08

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

Interpretation: static event topology improved accepted static input coverage for the implemented `EventHandling`/`EventProcessing` shape, including the original target group of five event-driven sagas. The refreshed dynamic baseline below confirms that the post-event static catalog can still be enriched with runtime evidence. It does not make event-origin inputs replayable: event payload placeholders remain materialization blockers, and `executorMaterializableInputVariantCount=94` only means the current ScenarioExecutor can materialize that subset through executor-readiness/runtime-owned handling.

## Dynamic-enrichment Quizzes baselines

Full/default sagas-only Quizzes run against the post-event-semantics static catalog before fixture/setup ownership diagnostics:

```text
verifiers/target/2026-06-29-dynamic-baseline-test-profile/quizzes-20260629-222801-046/
```

The run used `SPRING_PROFILES_ACTIVE=test,sagas,local`. This matters because four async `@SpringBootTest` classes rely on the `test` profile datasource configuration. A first run with only `sagas,local` introduced profile-related `ApplicationContext` failures, so use `test,sagas,local` for this baseline.

Static catalog summary:

```text
discovered/adapted sagas: 68
sagas with accepted inputs: 36
sagas without accepted inputs: 32
accepted input variants: 584
rejected source-mode inputs: 81
scenario catalog records written: 584
multi-saga records: 0
```

Executor-readiness summary:

```text
acceptedInputVariantCount: 584
staticRecipeReadyInputVariantCount: 0
executorMaterializableInputVariantCount: 94
blockedInputVariantCount: 490
EVENT_PAYLOAD_PLACEHOLDER blockers: 132
```

Dynamic run summary:

```text
runStatus: PARTIAL
batchStatus: FAILED
testClassesSelected: 45
testClassesPassed: 43
testClassesFailed: 2
testClassesTimedOut: 0
testClassesNoReport: 0
evidenceFilesRead: 1
dynamicEventsRead: 26820
eventsMissingTestContext: 260
scenarioPlansRead: 584
scenarioPlansEnriched: 584
```

Dynamic event breakdown:

```text
STEP_STARTED: 4862
COMMAND_SENT: 10768
AGGREGATE_ACCESSED: 6328
STEP_FINISHED: 4862
warnings: 0
writeErrors: 0
includeCommandFields: true
```

Join status counts:

```text
MATCHED_EXACT: 291
MATCHED_HIGH_CONFIDENCE: 109
MATCHED_PARTIAL: 0
AMBIGUOUS: 0
UNMATCHED: 184
NOT_COVERED: 0
warningCount: 0
```

The two failing test classes are the same known Quizzes failures seen in earlier good baselines:

```text
pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.AnonymizeStudentAndRemoveStudentTest
  tests=9, failures=3, errors=0

pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament.RemoveTournamentAndUpdateTournamentTest
  tests=10, failures=4, errors=0
```

Before runtime input attribution, the comparable baseline was:

```text
MATCHED_EXACT: 0
MATCHED_HIGH_CONFIDENCE: 2
AMBIGUOUS: 44
UNMATCHED: 20
warningCount: 8238
```

The comparable post-event run with direct attribution before fixture/setup ownership diagnostics was:

```text
MATCHED_EXACT: 291
MATCHED_HIGH_CONFIDENCE: 109
AMBIGUOUS: 0
UNMATCHED: 184
warningCount: 0
```

The current post fixture/setup and feature-helper ownership run is:

```text
verifiers/target/feature-helper-owner-fix-dynamic-smoke/quizzes-20260630-122219-034/
scenarioPlansRead: 584
scenarioPlansEnriched: 584
runStatus: PARTIAL
batchStatus: FAILED
testClassesSelected: 45
testClassesPassed: 43
testClassesFailed: 2
dynamicEventsRead: 26815
eventsMissingTestContext: 256
MATCHED_EXACT: 435
MATCHED_HIGH_CONFIDENCE: 125
MATCHED_PARTIAL: 0
AMBIGUOUS: 0
UNMATCHED: 24
NOT_COVERED: 0
unmatchedReasonCounts:
  FAILED_TEST_CLASS: 8
  NOT_SELECTED_TEST_CLASS: 7
  HELPER_OWNER_MISMATCH: 0
  UNCLASSIFIED: 9
```

Current event breakdown:

```text
STEP_STARTED: 4862
COMMAND_SENT: 10765
AGGREGATE_ACCESSED: 6326
STEP_FINISHED: 4862
warnings: 0
writeErrors: 0
```

Interpretation: direct runtime attribution plus fixture/setup and feature-helper ownership metadata materially improved exact static/dynamic joining, kept ambiguity at zero, and reduced unmatched records from `184` to `24`. This is mainly a precision/attribution improvement, not proof that all useful static inputs are dynamically exercised. Dynamic evidence remains an additive sidecar rather than a source of new static scenarios.

Current implementation note: enriched dynamic records now include `dynamicEvidence.unmatchedReason` for `UNMATCHED` records only, and manifests/join reports include `unmatchedReasonCounts`. Static/exported inputs include `inputRole`, `fixtureOrigin`, and `callContextMethodName` so remaining Quizzes unmatched records can be interpreted by failed/not-selected class, helper-owner mismatch, or unclassified residual categories.

Validation note (2026-06-30): the comparable 2g Quizzes dynamic smoke reached static catalog generation (`584` scenarios) but was killed with exit code `137` before dynamic artifacts. A follow-up 4g smoke preserved partial dynamic artifacts under `verifiers/target/unmatched-fixture-diagnostics-dynamic-smoke-4g/quizzes-20260630-004120-601/`; it timed out 42 of 45 selected classes, so it is not comparable to the baseline. Its join report still validates artifact shape: `dynamicEventsRead=1766`, `MATCHED_EXACT=17`, `MATCHED_HIGH_CONFIDENCE=11`, `AMBIGUOUS=0`, `UNMATCHED=556`, `unmatchedReasonCounts={FAILED_TEST_CLASS=534, NOT_SELECTED_TEST_CLASS=7, HELPER_OWNER_MISMATCH=0, UNCLASSIFIED=15}`.

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

## ScenarioExecutor fault-vector smoke

Verified on 2026-07-08 with a real verifier-generated Quizzes catalog through the forked Docker runtime path:

Commands:

```bash
CATALOG_PATH=/reports/quizzes-20260708-163552-193/scenario-catalog.jsonl \
SCENARIO_ID=910f72907e0d901bc5d35e0ecea03ec920b7ffb63929bbba1bfdba4fe531e195 \
OUTPUT_PATH=/reports/scenario-executor/s6-default-report.json \
docker compose run --rm scenario-executor

CATALOG_PATH=/reports/quizzes-20260708-163552-193/scenario-catalog.jsonl \
SCENARIO_ID=910f72907e0d901bc5d35e0ecea03ec920b7ffb63929bbba1bfdba4fe531e195 \
FAULT_VECTOR=1 \
OUTPUT_PATH=/reports/scenario-executor/s6-explicit-report.json \
docker compose run --rm scenario-executor
```

Reported results:

```text
Catalog: verifiers/target/quizzes-20260708-163552-193/scenario-catalog.jsonl
Scenario plan id: 910f72907e0d901bc5d35e0ecea03ec920b7ffb63929bbba1bfdba4fe531e195
Saga: pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.GetCourseExecutionsFunctionalitySagas
Default vector report: verifiers/target/scenario-executor/s6-default-report.json
Default vector terminal status: SUCCESS
Default vector lifecycle: COMMITTED
Explicit vector report: verifiers/target/scenario-executor/s6-explicit-report.json
Explicit vector terminal status: FAULT_COMPENSATED
Explicit vector lifecycle: COMPENSATED
Explicit vector realized slot: 0 (runtime step `getCourseExecutionsStep`)
```

Interpretation: a narrow executor path now supports the implemented single-saga fault-vector contract. It executed one generated Quizzes plan by resolving runtime-owned infrastructure arguments and using the in-memory fault-vector provider for the explicit-fault run. Older accounting that reported zero executor-ready inputs was measuring static recipe readiness only; executor materializability is still reported separately/aligned with ScenarioExecutor semantics. This is still not generic multi-saga execution, broad runtime parity, impact scoring, or search.

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
