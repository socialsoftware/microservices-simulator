# Static Event Semantics for Verifier Inputs — Validation

## Baseline To Beat

A count-only Quizzes baseline was generated during planning from the implementation base commit:

```text
bd208d54 fix(verifiers): trace facade calls inside setup helpers
```

Baseline artifact path:

```text
verifiers/target/event-semantics-baseline-before/quizzes-20260624-183729-414
```

The run used a temporary detached worktree at:

```text
/tmp/microservices-simulator-event-baseline
```

Reason: current working tree has simulator/verifier API drift (`transaction` vs `transactional` package names and simulator dependency version mismatch), causing local current-HEAD `spring-boot:run` attempts to discover 0 sagas. Those invalid attempts must not be used as baselines.

## Baseline Command

Executed from the temporary worktree:

```bash
cd /tmp/microservices-simulator-event-baseline/verifiers && mvn -q spring-boot:run -Dspring-boot.run.arguments="--verifiers.applications-root=/tmp/microservices-simulator-event-baseline/applications --verifiers.application-base-dir=quizzes --verifiers.output-root=/home/andre/microservices-simulator/verifiers/target/event-semantics-baseline-before --verifiers.report-html-path=analysis-report.html --verifiers.scenario-catalog.enabled=true --verifiers.dynamic-enrichment.enabled=false --verifiers.scenario-catalog.catalog-write-mode=COUNT_ONLY --verifiers.scenario-catalog.max-input-variants-per-saga=100000 --verifiers.scenario-catalog.max-schedules-per-input-tuple=100000"
```

Important config:

```text
catalogWriteMode = COUNT_ONLY
maxInputVariantsPerSaga = 100000
maxSchedulesPerInputTuple = 100000
maxSagaSetSize = 1
inputPolicy = RESOLVED_OR_REPLAYABLE
dynamicEnrichment = false
```

`COUNT_ONLY` means no large scenario catalog was materialized. `catalogWritten = 0` is expected.

## Baseline Metrics

From:

```text
verifiers/target/event-semantics-baseline-before/quizzes-20260624-183729-414/scenario-space-accounting.json
```

| Metric | Baseline |
|---|---:|
| Discovered sagas | 65 |
| Sagas with accepted inputs | 26 |
| Sagas without accepted inputs | 39 |
| Accepted input variants | 517 |
| Selected input-bound scenario total | 517 |
| Catalog written | 0 |
| Executor-ready input variants | 0 |

## Runtime-Evidence Group Baseline Status

All five target sagas are missing accepted static inputs in the baseline:

| Saga FQN | Baseline status |
|---|---|
| `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.RemoveQuizAnswerFunctionalitySagas` | MISSING |
| `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.UpdateUserNameInQuizAnswerFunctionalitySagas` | MISSING |
| `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.RemoveUserFromCourseExecutionFunctionalitySagas` | MISSING |
| `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AnonymizeUserTournamentFunctionalitySagas` | MISSING |
| `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateUserNameFunctionalitySagas` | MISSING |

## Focused Validation

Run focused verifier tests first:

```bash
cd verifiers && mvn -q -Dtest=EventHandlingBridgeVisitorDummyappSpec,GroovyConstructorInputTraceVisitorDummyappSpec,ApplicationAnalysisScenarioModelAdapterSpec test
```

If no dedicated bridge spec exists yet, create one before relying on Quizzes smoke output.

Focused tests must prove:

1. Event bridge extraction:

   ```text
   EventHandling method -> EventProcessing method -> Functionalities facade -> Saga FQN
   ```

2. Groovy event-handler call tracing emits an event-origin trace.
3. Adapter produces an `InputVariant` for the downstream saga.

## After-Implementation Quizzes Smoke

Run the same count-only shape after implementation, from the implementation branch/worktree:

```bash
cd verifiers && mvn -q spring-boot:run -Dspring-boot.run.arguments="--verifiers.applications-root=/home/andre/microservices-simulator/applications --verifiers.application-base-dir=quizzes --verifiers.output-root=/home/andre/microservices-simulator/verifiers/target/event-semantics-after --verifiers.report-html-path=analysis-report.html --verifiers.scenario-catalog.enabled=true --verifiers.dynamic-enrichment.enabled=false --verifiers.scenario-catalog.catalog-write-mode=COUNT_ONLY --verifiers.scenario-catalog.max-input-variants-per-saga=100000 --verifiers.scenario-catalog.max-schedules-per-input-tuple=100000"
```

Use the same config values as baseline. If current HEAD still has simulator/verifier package drift, fix that separately or run from a clean implementation worktree with compatible simulator/verifier versions. Do not compare against a run that discovers 0 sagas.

## After implementation

Post-event-semantics artifact path:

```text
verifiers/target/event-semantics-after-current-only/quizzes-20260624-215147-522
```

The run used `COUNT_ONLY`, so `catalogWritten = 0` is expected and is not a failure.

Metrics from `scenario-space-accounting.json`:

| Metric | After implementation |
|---|---:|
| Discovered sagas | 68 |
| Sagas with accepted inputs | 36 |
| Sagas without accepted inputs | 32 |
| Accepted input variants | 584 |
| Selected input-bound scenario total | 584 |
| Catalog written | 0 |
| Static recipe-ready input variants | 0 |
| ScenarioExecutor materializable input variants | 94 |
| ScenarioExecutor ready input variants | 94 |
| Blocked input variants | 490 |

Original target group status after implementation:

| Saga FQN | After status |
|---|---|
| `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.RemoveQuizAnswerFunctionalitySagas` | ACCEPTED |
| `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.UpdateUserNameInQuizAnswerFunctionalitySagas` | ACCEPTED |
| `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.RemoveUserFromCourseExecutionFunctionalitySagas` | ACCEPTED |
| `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AnonymizeUserTournamentFunctionalitySagas` | ACCEPTED |
| `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateUserNameFunctionalitySagas` | ACCEPTED |

Preferred static acceptance criteria were met:

- all five runtime-evidence target sagas moved from `MISSING` to accepted static inputs;
- `sagasWithoutAcceptedInputs` dropped below 39 (`32`);
- `sagasWithAcceptedInputs` rose above 26 (`36`);
- `acceptedInputVariantCount` rose above 517 (`584`).

Materialization was intentionally not solved in this slice. Event payload placeholders remain blockers (`EVENT_PAYLOAD_PLACEHOLDER: 132`). `executorMaterializableInputVariantCount=94` and `executorReadyInputVariantCount=94` come from current ScenarioExecutor materializability/runtime-owned handling, not full event payload replay. Static accepted input coverage is therefore distinct from executor replayability.

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

Focused validation reports observed:

```text
verifiers/target/surefire-reports/pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.EventHandlingBridgeVisitorDummyappSpec.txt
Tests run: 1, Failures: 0, Errors: 0

verifiers/target/surefire-reports/pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.GroovyConstructorInputTraceVisitorDummyappSpec.txt
Tests run: 24, Failures: 0, Errors: 0

verifiers/target/surefire-reports/pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ApplicationAnalysisScenarioModelAdapterSpec.txt
Tests run: 11, Failures: 0, Errors: 0
```

## Acceptance Criteria

Minimum:

- Focused dummyapp tests pass.
- Quizzes smoke discovers real sagas, not 0; the post-event run discovers 68.
- Event bridge facts are visible in reports/diagnostics.
- At least one event-driven target saga moves from missing to accepted input; the post-event run covers all five target FQNs.

Preferred for this slice:

- All five runtime-evidence target sagas move from `MISSING` to accepted static inputs.
- `sagasWithoutAcceptedInputs` drops below 39.
- `sagasWithAcceptedInputs` rises above 26.
- `acceptedInputVariantCount` rises above 517.
- Static recipe-ready count may remain 0; that is acceptable if event payload recipes are placeholders. Executor materializability is reported separately.

## Comparison Metrics

Compare before/after:

```text
discoveredSagaCount
len(sagasWithAcceptedInputs)
len(sagasWithoutAcceptedInputs)
acceptedInputVariantCount
selected input-bound scenario total
staticRecipeReadyInputVariantCount
executorMaterializableInputVariantCount
executorReadyInputVariantCount
target 5 FQN statuses
newly covered saga FQNs
remaining missing saga FQNs
```

Use FQNs only. Do not compare by simple class name.

## Baseline Extraction Snippet

```bash
python3 - <<'PY' /path/to/run
import json, sys
run=sys.argv[1]
a=json.load(open(run+'/scenario-space-accounting.json'))
tl=a['typeLevelCoverage']; er=a['executorReadiness']; ib=a['inputBoundScenarioSpace']
print('discoveredSagaCount', tl['discoveredSagaCount'])
print('sagasWithAcceptedInputs', len(tl['sagasWithAcceptedInputs']))
print('sagasWithoutAcceptedInputs', len(tl['sagasWithoutAcceptedInputs']))
print('acceptedInputVariantCount', er['acceptedInputVariantCount'])
print('staticRecipeReadyInputVariantCount', er.get('staticRecipeReadyInputVariantCount'))
print('executorMaterializableInputVariantCount', er.get('executorMaterializableInputVariantCount'))
print('executorReadyInputVariantCount', er.get('executorReadyInputVariantCount'))
print('selectedInputBoundTotal', ib['selectedByGenerator']['total'])
print('catalogWritten', ib['catalogWritten']['total'])
PY
```
