# Quizzes multi-saga materializability audit

Date: 2026-07-09

## Purpose

Check during spec planning whether current Quizzes artifacts can support a real multi-saga ScenarioExecutor smoke, or whether materializability/fixture-readiness work must happen first.

## Generation command

A bounded Quizzes catalog was generated with multi-saga-only `WRITE_PLANS` output:

```bash
cd verifiers
java -jar target/verifiers-0.0.1-SNAPSHOT-exec.jar \
  --verifiers.applications-root=/home/andre/microservices-simulator/applications \
  --verifiers.application-base-dir=quizzes \
  --verifiers.output-root=/home/andre/microservices-simulator/verifiers/target/multi-saga-executor-planning-audit \
  --verifiers.scenario-catalog.enabled=true \
  --verifiers.scenario-catalog.generation-strategy=INTERACTION_PRUNED \
  --verifiers.scenario-catalog.catalog-write-mode=WRITE_PLANS \
  --verifiers.scenario-catalog.include-singles=false \
  --verifiers.scenario-catalog.max-saga-set-size=2 \
  --verifiers.scenario-catalog.max-catalog-scenarios=500 \
  --verifiers.scenario-catalog.max-input-variants-per-saga=2 \
  --verifiers.scenario-catalog.max-schedules-per-input-tuple=4 \
  --verifiers.scenario-catalog.schedule-strategy=SEGMENT_COMPRESSED \
  --verifiers.dynamic-enrichment.enabled=false
```

Generated run directory:

```text
verifiers/target/multi-saga-executor-planning-audit/quizzes-20260709-004627-310/
```

## Catalog result

The verifier generated 500 multi-saga catalog records:

```text
Scenario catalog export wrote 500 scenarios to .../scenario-catalog.jsonl
```

Existing saved Quizzes executor catalogs before this audit were single-saga-only or count-only-empty, so they were insufficient for this planning question.

## Materializability audit

A temporary Java audit used the repository's actual `ScenarioExecutorReadinessEvaluator` over the generated catalog. The audit used Jackson unknown-property tolerance, matching `ScenarioCatalogReader` behavior.

Result:

```text
catalog=verifiers/target/multi-saga-executor-planning-audit/quizzes-20260709-004627-310/scenario-catalog.jsonl
totalPlans=500
multiSagaPlans=500
uniqueInputs=52
materializableUniqueInputs=4
materializableMultiSagaPlans=8
blockers={CALL_RECEIVER_NOT_READY=220, EVENT_PAYLOAD_PLACEHOLDER=28, MISSING_TARGET_TYPE=748, PROPERTY_RECEIVER_NOT_READY=798, TRANSFORM_RECEIVER_NOT_READY=90, UNKNOWN_VALUE=424, UNMATERIALIZABLE_ASSIGNMENT=132, UNRESOLVED_PLACEHOLDER=8, UNRESOLVED_VARIABLE=432}
```

## Example materializable plan

Plan id:

```text
0945caa9ac2fe06a268e6df6aa992fcf69e253116264684d577bdbbb955c2e25
```

Participants:

```text
CreateCourseExecutionFunctionalitySagas
GetCourseExecutionsFunctionalitySagas
```

Schedule:

```text
0 GetCourseExecutionsFunctionalitySagas.getCourseExecutionsStep#0
1 CreateCourseExecutionFunctionalitySagas.getCourseStep#0
2 CreateCourseExecutionFunctionalitySagas.createCourseStep#1
3 CreateCourseExecutionFunctionalitySagas.createCourseExecutionStep#2
4 CreateCourseExecutionFunctionalitySagas.updateCourseExecutionCountStep#3
```

Fault space:

```text
length=5
defaultVector=00000
```

Conflict evidence:

```text
WRITE/READ on Execution, with symbolic/type-only evidence.
```

## Planning conclusion

Current Quizzes has at least one bounded, currently materializable multi-saga candidate under the existing executor readiness policy. The multi-saga executor spec does not need to be postponed for a separate materializability-first feature.

Caveat: these first materializable candidates are not necessarily the richest anomaly/impact examples. They are adequate for a real Quizzes smoke of multi-saga deterministic interleaving replay; broader impact-oriented examples may still require later materialization and fixture-readiness improvements.
