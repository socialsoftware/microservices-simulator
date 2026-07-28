# Verifier evidence appendix

Last updated: 2026-07-28

This page stores concrete validation results, metrics, and run references so [`current-state.md`](current-state.md) can stay readable. Treat this as an appendix: cite it when you need proof, not as the first-read narrative.

## Generic invariant-impact baseline

Verified on 2026-07-28. Production detection is application-independent: `SagaUnitOfWorkService.registerChanged` records one `INVARIANT_VIOLATION` when its existing `Aggregate.verifyInvariants()` call throws, then rethrows the identical exception. The optional executor sidecar evaluates `ImpactV1 = invariantViolationCount`; faults, aborts, and compensation outcomes are not separate score inputs.

Focused simulator verification:

```bash
cd simulator
mvn -Dtest=SagaUnitOfWorkServiceDynamicEvidenceTest,DynamicEvidenceRecorderTest,DynamicEvidenceAutoConfigurationTest test
```

Result: 30 tests, zero failures/errors/skips. Coverage includes zero/one/multiple exact signals, aggregate and Saga/step/fault-boundary context, identical exception propagation when recorder enablement or dispatch fails, no merge after rejection, scoped-recorder restoration, and the manifest's default invariant-event count.

Focused verifier verification:

```bash
cd simulator && mvn -DskipTests -Dprotobuf.skip install
cd ../verifiers
mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec,ScenarioExecutorWrapperSpec test
```

Result: 111 tests, zero failures/errors/skips. A dummyapp-labelled executor fixture drives a rejecting aggregate through the real `SagaUnitOfWorkService.registerChanged → Aggregate.verifyInvariants()` boundary and writes an evaluated sidecar with exactly one finding and score `1`. Separate controls prove ordered multiple counts, rejection of missing/mismatched/delayed-attempt signals, a fresh empty collector for a sequential attempt, evaluated zero for a realized assigned fault followed by successful no-work compensation, `NOT_EVALUATED` with null score for setup, infrastructure, dry-run, and report-write failure, CLI/wrapper/orchestrator propagation, absent-leaf symlink-parent alias protection, and byte-identical package artifacts. `ScenarioExecutionReport.v4` remains unchanged.

Complete affected-module regression also passed: simulator 106 tests and verifier 595 tests, with zero failures/errors/skips.

Real Quizzes positive control:

```bash
cd applications/quizzes
mvn -Ptest-sagas -Dtest=AddParticipantAndUpdateStudentNameTest test
```

Result: 9 tests, zero failures/errors/skips. The existing feature `concurrent - add creator: add-s1; update; event; add-s2` reads stale creator state, applies the update/event, resumes AddParticipant, and reaches the application's declared invariant rejection. Its scoped evidence assertion observes exactly one event with:

```text
eventKind: INVARIANT_VIOLATION
aggregateType: SagaTournament
sourceMethod: SagaUnitOfWorkService.registerChanged
verificationMethod: Aggregate.verifyInvariants
functionality: AddParticipantFunctionalitySagas
step: addParticipantStep
```

This is real Quizzes interaction evidence for the generic detector, not a generated ScenarioExecutor positive: the current persisted action model cannot schedule the prerequisite-heavy Saga/event interaction.

Real Quizzes zero-impact Docker control:

```bash
MEDIUM_MEM_LIMIT=3g MEDIUM_MEM_RESERVATION=1g \
PACKAGE_PATH=/reports/outcome2-helper-tracing/quizzes-20260727-180306-391/scenario-catalog-manifest.json \
FAULT_SCENARIO_ID=142eccc9b6d9464439f9607061ff6c7a32109108d34a1ddc076e6bf8519ab71b \
OUTPUT_PATH=/reports/generic-invariant-impact-baseline/execution-single-fault.json \
IMPACT_OUTPUT_PATH=/reports/generic-invariant-impact-baseline/impact-single-fault.json \
docker compose run --rm -e JAVA_TOOL_OPTIONS=-Xmx2g scenario-executor
```

An initial run at the Compose default 768 MiB limit exhausted the heap while loading the 27 MiB workload catalog and wrote no report or package artifact. The bounded rerun succeeded:

```text
executionAttemptId: 2556421e-7479-4b43-a54c-897b0f544956
execution terminal/conformance: COMPENSATED / EXACT
assigned fault slot: REALIZED
impact schema/model: microservices-simulator.scenario-impact-report.v1 / ImpactV1
impact evaluation: EVALUATED
invariantViolationCount / impactScore: 0 / 0
findings: []
```

Artifacts:

```text
verifiers/target/generic-invariant-impact-baseline/execution-single-fault.json
verifiers/target/generic-invariant-impact-baseline/impact-single-fault.json
```

All five package hashes were unchanged before and after execution:

```text
b557240f5b3ca91415a0d3d7d95e4357c8aa23024cf59fb4e2ce0165c969b1bf  workload-catalog.jsonl
5481e2e515efc6817024e8afafca05a3c001828ec73bab2e19c1115fed544d3a  fault-scenario-catalog.jsonl
395aebce8bb5b0ec1b790f341045c2b4b6e83191f9a75ad965e70104c5dd231c  scenario-space-accounting.json
37671daa3fe80f4ae9c681441ed53409df6469912c13f40990707a5690f58a19  workload-catalog-rejected-inputs.jsonl
8553999ff3ba9f06043ff6daedf91d57f6c83e76d666b3054ff2e29b0f8498f9  scenario-catalog-manifest.json
```

## Setup materializability and helper-fidelity accuracy

Verified on 2026-07-27 against a refreshed single-saga Quizzes package after adding caller-to-helper argument substitution and ordered helper DTO mutation capture.

Artifacts:

```text
before: verifiers/target/materializability-accuracy-smoke/quizzes-20260727-003718-018/
after:  verifiers/target/outcome2-helper-tracing/quizzes-20260727-180306-391/
preflight report: verifiers/target/outcome2-helper-tracing/setup-preflight-report.json
```

The refreshed manifest records the effective generator configuration:

```text
generationStrategy: BRUTE_FORCE
catalogWriteMode: WRITE_WORKLOADS
includeSingles: true
maxSagaSetSize: 1
maxCatalogScenarios: 10000
maxInputVariantsPerSaga: 1000
maxSchedulesPerInputTuple: 1
inputPolicy: RESOLVED_OR_REPLAYABLE
scheduleStrategy: SEGMENT_COMPRESSED
deterministicSeed: 1234
```

Static comparison:

| Metric | Before helper mutation tracing | After helper mutation tracing |
|---|---:|---:|
| Accepted inputs / written WorkloadPlans | 587 | 732 |
| Manifest-declared setup candidates (`materializable=true`) | 94 | 82 |
| Statically blocked inputs | 493 | 650 |
| Eager FaultScenarios | 326 | 164 |
| `createUser` helper candidates with ordered DTO assignments | 0 / 40 | 75 / 75 |
| `createCourseExecution` helper candidates with ordered DTO assignments | 0 / 46 | 50 / 50 |

The increased accepted count comes from preserving more concrete helper call sites. The lower static candidate count is a correctness result, not a coverage regression hidden by the implementation: the previous dominant course/user recipes were empty DTO constructors. All refreshed `createUser` helper recipes carry `name`, `username`, and `role`. All refreshed `createCourseExecution` helper recipes carry `name`, `type`, `acronym`, `academicTerm`, and `endDate`; the last assignment retains the source `DateHandler.toISOString(endDate)` call and is conservatively blocked as `UNMATERIALIZABLE_ASSIGNMENT` because that local call is not yet supported. One user helper candidate is conservatively blocked as `LOOP_DEPENDENT_MUTATION`.

The initial optional preflight was run through the Docker ScenarioExecutor wrapper with one target Spring context:

```bash
PACKAGE_PATH=/reports/outcome2-helper-tracing/quizzes-20260727-180306-391/scenario-catalog-manifest.json \
OUTPUT_PATH=/reports/outcome2-helper-tracing/setup-preflight-report.json \
docker compose run --rm -e PREFLIGHT=true scenario-executor
```

Result:

```text
report schema: microservices-simulator.scenario-setup-preflight-report.v1
candidate selection: MANIFEST_DECLARED_MATERIALIZABLE
candidates / participants: 82 / 82
SETUP_READY: 80
STARTUP_FAILED: 2
measured setup loop after Spring startup: 49,069,530 ns
workflow actions executed: 0
```

The 80 setup-ready workloads are 76 `CreateUserFunctionalitySagas` and four `GetCourseExecutionsFunctionalitySagas` workloads. `GetCourseExecutionByIdFunctionalitySagas` and `FindQuizFunctionalitySagas` failed startup because persisted integer values materialized as `BigInteger` while their public constructors require `Integer`; the report lists both actual argument types and available constructor signatures. This is the pre-fix baseline. The preflight report is outside the five-file semantic package, and package validation/checksums remained intact.

### Exact integral constructor restoration

Verified on 2026-07-28 against the same package. The executor first retains ordinary Java reflection invocation and overload behavior. Only when no constructor is directly invocable does the typed boundary attempt exact numeric conversion to `byte`/`Byte`, `short`/`Short`, `int`/`Integer`, `long`/`Long`, or `BigInteger`. Fractional values, overflow, null-to-primitive, and non-numeric or unsupported targets remain startup failures with argument index, persisted type/value, target type, and rejection reason. Constructor-body failures still propagate as constructor failures rather than overload mismatches.

Focused executor regression:

```bash
cd verifiers
mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorWrapperSpec,ScenarioExecutorReadinessEvaluatorSpec,ScenarioExecutorOrchestratorSpec test
```

Result: 105 tests, zero failures/errors/skips. Discriminating cases cover exact restoration across wrapper and primitive integral targets, valid upper range boundaries, an exact integral decimal, overflow, a fractional decimal, null-to-primitive, unsupported string coercion, fallback to a wider exact overload, ordinary reflection widening and overload preference, and constructor-body failures before and after conversion.

Full verifier regression:

```bash
cd verifiers
mvn test
```

Result: 587 tests, zero failures/errors/skips.

An initial Docker run at the Compose default 768 MiB limit exhausted the Java heap while reading the package and wrote no report or package artifact. The successful one-context Quizzes preflight used the existing package and the previously validated 3 GiB container / 2.5 GiB JVM heap boundary:

```bash
MEDIUM_MEM_LIMIT=3g \
PACKAGE_PATH=/reports/outcome2-helper-tracing/quizzes-20260727-180306-391/scenario-catalog-manifest.json \
OUTPUT_PATH=/reports/integral-numeric-restoration/setup-preflight-report.json \
docker compose run --rm -T \
  -e PREFLIGHT=true \
  -e FAULT_SCENARIO_ID= \
  -e JAVA_TOOL_OPTIONS=-Xmx2500m \
  scenario-executor
```

Artifacts:

```text
report: verifiers/target/integral-numeric-restoration/setup-preflight-report.json
container log: verifiers/target/integral-numeric-restoration/scenario-executor.log
verification summary: verifiers/target/integral-numeric-restoration/verification-summary.json
```

Measured result:

```text
report schema: microservices-simulator.scenario-setup-preflight-report.v1
terminal status: SUCCESS
candidates / participants: 82 / 82
SETUP_READY: 82
STARTUP_FAILED: 0
measured setup loop after Spring startup: 83,231,479 ns
GetCourseExecutionByIdFunctionalitySagas: 1 SETUP_READY
FindQuizFunctionalitySagas: 1 SETUP_READY
FaultScenarios linked to SETUP_READY workloads: 164 / 164
workflow actions executed: 0
Spring application contexts started: 1
```

All five package files matched their preflight-baseline SHA-256 values after the run:

```text
b557240f5b3ca91415a0d3d7d95e4357c8aa23024cf59fb4e2ce0165c969b1bf  workload-catalog.jsonl
5481e2e515efc6817024e8afafca05a3c001828ec73bab2e19c1115fed544d3a  fault-scenario-catalog.jsonl
8553999ff3ba9f06043ff6daedf91d57f6c83e76d666b3054ff2e29b0f8498f9  scenario-catalog-manifest.json
395aebce8bb5b0ec1b790f341045c2b4b6e83191f9a75ad965e70104c5dd231c  scenario-space-accounting.json
37671daa3fe80f4ae9c681441ed53409df6469912c13f40990707a5690f58a19  workload-catalog-rejected-inputs.jsonl
```

The package schema, deterministic ids, manifest-declared static candidates, and all package bytes are unchanged. The only changed result is runtime setup truth: the prior two constructor-type false negatives are now setup-ready.

Historical helper-fidelity focused regression command:

```bash
cd verifiers
mvn -q -Dtest='ScenarioExecutorSpec,ScenarioExecutorWrapperSpec,ScenarioExecutorOrchestratorSpec,ScenarioExecutorReadinessEvaluatorSpec,GroovyConstructorInputTraceVisitorSpec,GroovyConstructorInputTraceVisitorDummyappSpec,InputRecipeMapperSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec' test
```

Post-review result: 177 tests, zero failures/errors/skips (`81 + 5 + 3 + 2 + 17 + 27 + 5 + 22 + 15`). Coverage includes reflection widening/overload behavior, complete manifest materializability tables, strict CLI/wrapper modes, and point-in-time helper mutation snapshots. The dummyapp fixture proves literal caller arguments flow into ordered setter/property mutations, excludes post-facade mutations, isolates separate helper calls, and preserves deterministic input ids and recipe fingerprints. The complete verifier suite then passed with 573 tests, zero failures/errors/skips after reconciling deterministic dummyapp fixture counts.

Two targeted executions—not a batch qualification stage—confirmed that ordinary execution still uses the shared setup path and reports its own outcome:

```text
all-zero FaultScenario: 564dc9b56a716cd2797a3f3200485791e6ecdb1ed383b422de987a72792d63bd
report: verifiers/target/outcome2-helper-tracing/execution-all-zero.json
result: SUCCESS / EXACT; forward action COMPLETED

single-fault FaultScenario: 142eccc9b6d9464439f9607061ff6c7a32109108d34a1ddc076e6bf8519ab71b
report: verifiers/target/outcome2-helper-tracing/execution-single-fault.json
result: COMPENSATED / EXACT; assigned fault realized
```

The five package hashes remained the same after preflight and both executions. This evidence establishes setup accuracy and helper recipe fidelity; it deliberately does not batch-qualify all-zero or faulty scenarios.

## Compensation-aware v3 end-to-end evidence

Verified on 2026-07-20 with bounded real Quizzes generation and the Docker ScenarioExecutor path.

### Package generation

Final generation command:

```bash
MEDIUM_MEM_LIMIT=3g MEDIUM_MEM_RESERVATION=2g MEDIUM_CPUS=2 \
docker compose run --rm \
  -e JAVA_TOOL_OPTIONS=-Xmx2500m \
  -e VERIFIERS_OUTPUT_ROOT=/reports/compensation-aware-v3-evidence/bounded-quizzes-v3 \
  -e VERIFIERS_SCENARIO_CATALOG_ENABLED=true \
  -e VERIFIERS_SCENARIO_CATALOG_GENERATION_STRATEGY=BRUTE_FORCE \
  -e VERIFIERS_SCENARIO_CATALOG_CATALOG_WRITE_MODE=WRITE_WORKLOADS \
  -e VERIFIERS_SCENARIO_CATALOG_INCLUDE_SINGLES=false \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_SAGA_SET_SIZE=2 \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_CATALOG_SCENARIOS=2000 \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_INPUT_VARIANTS_PER_SAGA=2 \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_SCHEDULES_PER_INPUT_TUPLE=4 \
  -e VERIFIERS_SCENARIO_CATALOG_SCHEDULE_STRATEGY=SEGMENT_COMPRESSED \
  -e VERIFIERS_SCENARIO_CATALOG_RECOVERY_SCHEDULE_CAP=20 \
  -e VERIFIERS_DYNAMIC_ENRICHMENT_ENABLED=false \
  fault-analysis-scenario-gen
```

Run and log:

```text
package: verifiers/target/compensation-aware-v3-evidence/bounded-quizzes-v3/quizzes-20260720-091007-712/
container log: verifiers/target/compensation-aware-v3-evidence/bounded-quizzes-v3-container.log
manifest schema: microservices-simulator.scenario-catalog-manifest.v3
WorkloadPlans written: 2000
manifest-declared setup-candidate WorkloadPlans: 12
statically blocked WorkloadPlans: 1988
FaultScenarios written: 84
computed eager vectors: 60
exact uncapped/written sum over computed vectors: 84 / 84
legacy scenario-catalog.jsonl present: false
```

The generated semantic package contains:

```text
workload-catalog.jsonl
workload-catalog-rejected-inputs.jsonl
fault-scenario-catalog.jsonl
scenario-space-accounting.json
scenario-catalog-manifest.json
```

A first bounded `INTERACTION_PRUNED` multi-saga diagnostic at `verifiers/target/compensation-aware-v3-evidence/quizzes-20260720-090609-170/` wrote 360 WorkloadPlans but no FaultScenarios because no selected workload had every participant pass the static setup-candidate gate. A bounded single-saga diagnostic then identified candidate Quizzes inputs. The final run deliberately used `BRUTE_FORCE` with a 2,000-workload cap to include real candidate multi-saga pairs; it did not loosen input readiness or use application-specific generation shortcuts.

### Selected compensation-interleaving FaultScenario

```text
WorkloadPlan id: 01c49ae4314e106161ccc75f7531c62d01299417e702908100d7fb809ca2face
FaultScenario id: 25c0d61a2a2b40c2aaff7946aca8d2bb1becfc54b8b168442bcff40262052271
assigned vector: 00010
vector source: EAGER_SINGLE_POINT
participants:
  CreateCourseExecutionFunctionalitySagas
  GetCourseExecutionsFunctionalitySagas
fault slots: 5
compensation checkpoints: 3
uncapped unique recovery schedules: 3
written recovery schedules: 3
selected action count: 7
```

Persisted action order:

```text
0 FORWARD      CreateCourseExecution.getCourseStep
1 FORWARD      CreateCourseExecution.createCourseStep
2 FORWARD      CreateCourseExecution.createCourseExecutionStep
3 FORWARD      CreateCourseExecution.updateCourseExecutionCountStep (assigned bit 1)
4 COMPENSATION CreateCourseExecution.createCourseExecutionStep
5 FORWARD      GetCourseExecutions.getCourseExecutionsStep
6 COMPENSATION CreateCourseExecution.createCourseStep
```

This is a concrete compensation/forward interleaving: one failed participant's reverse checkpoints surround a still-live participant's forward action.

### Docker execution and report

**Pre-remediation historical evidence:** this saved execution predates explicit domain-failure classification. Its recorded result is preserved below, but it is not evidence of the current zero-bit fallback policy.

No vector overlay was supplied:

```bash
PACKAGE_PATH=/reports/compensation-aware-v3-evidence/bounded-quizzes-v3/quizzes-20260720-091007-712/scenario-catalog-manifest.json \
FAULT_SCENARIO_ID=25c0d61a2a2b40c2aaff7946aca8d2bb1becfc54b8b168442bcff40262052271 \
OUTPUT_PATH=/reports/compensation-aware-v3-evidence/execution-report-25c0d61a.json \
MEDIUM_MEM_LIMIT=3g MEDIUM_MEM_RESERVATION=2g MEDIUM_CPUS=2 \
docker compose run --rm scenario-executor
```

Recorded result:

```text
Docker exit code: 0
report: verifiers/target/compensation-aware-v3-evidence/execution-report-25c0d61a.json
executor log: verifiers/target/compensation-aware-v3-evidence/executor-25c0d61a-container.log
schema: microservices-simulator.scenario-execution-report.v4
terminalStatus: PARTIAL_COMPENSATED
scheduleConformance: DEVIATED
deviationPolicy: IMMEDIATE_CHECKPOINT_RECOVERY_AND_CONTINUE
providerMode: IN_MEMORY_FAULT_VECTOR
participant final states: COMPENSATED, COMMITTED
actual actions: 3
blocker: UNASSIGNED_RUNTIME_FORWARD_FAILURE
```

Measured action and lifecycle order:

```text
actual 0 FORWARD CreateCourseExecution.getCourseStep             COMPLETED
actual 1 FORWARD CreateCourseExecution.createCourseStep          FAILED / UNASSIGNED_RUNTIME
actual 2 FORWARD GetCourseExecutions.getCourseExecutionsStep     COMPLETED
lifecycle 0 CreateCourseExecution ABORTED             FORWARD_FAILED
lifecycle 1 CreateCourseExecution NO_COMPENSATION_WORK SUCCEEDED
lifecycle 2 CreateCourseExecution COMPENSATED          SUCCEEDED
lifecycle 3 GetCourseExecutions   AUTOMATIC_COMMIT     SUCCEEDED
assigned slot 3 updateCourseExecutionCountStep: MASKED
```

The first `getCourseStep` completed. The zero-bit `createCourseStep` then failed with a plain, unmarked `SimulatorException` reporting service unavailability after command retries, before the assigned slot at `updateCourseExecutionCountStep`. The pre-remediation executor incorrectly treated that infrastructure failure as a domain deviation, ran no-work fallback, and completed/committed `GetCourseExecutionsFunctionalitySagas`. This historical run proves package selection, materialization, action-aware measurement, and package immutability under the old classifier; it does **not** prove current zero-bit domain fallback, survivor continuation, exact planned compensation, or valid extracted input behavior. Under the current explicit-marker contract, the same failure runs no fallback or survivor action and reports `UNEXPECTED_EXECUTION_FAILURE / INCOMPLETE`.

### Package immutability

Before and after executor checksums were identical:

```text
7ea4baa145c2310e30bd39517a01a5a97e54bcede8382f1b2d6088271e28c5af  workload-catalog.jsonl
c5ed7c161b01b67c18b5536618f29867f82e3095a922913ec361f9eb9f00b42b  workload-catalog-rejected-inputs.jsonl
fabd702b10ad0ad11cd11488720d562c9b1c60a71e30035cfbcbc0d6562192cc  fault-scenario-catalog.jsonl
ce17e8b25b1b5249c035d25d0c95df9183778bcc4a27c178dc130ec768ed7f8d  scenario-space-accounting.json
7423fa52844d745b8309b1fbf504be197ef9f7ccbc8f489edc074765fe4a0012  scenario-catalog-manifest.json
```

Evidence files:

```text
verifiers/target/compensation-aware-v3-evidence/package.sha256.before
verifiers/target/compensation-aware-v3-evidence/package.sha256.after
verifiers/target/compensation-aware-v3-evidence/verification-summary.json
```

### Dynamic-enrichment compatibility

`DummyappDynamicEnrichmentIntegrationSpec` now writes a complete eager v3 package before enrichment, snapshots all five semantic artifacts, writes `workload-dynamic-evidence.jsonl` plus manifest/join report, and asserts the semantic bytes are unchanged. It also asserts that the sidecar links by `workloadPlanId` and embeds no WorkloadPlan, FaultScenario, or forward schedule. The focused spec, full verifier Maven suite, and Docker verifier-test suite passed.

### High-cardinality recovery accounting

The synthetic 61-forward-step case in `RecoveryScheduleGeneratorSpec` validates the uncapped/capped boundary without enumerating the uncapped space:

```text
exact uncapped unique recovery count: 118264581564861424
written schedules at configured cap: 20
counting states visited: 992
materialized leaves: fewer than 100
```

This is exact BigInteger accounting for the computed vector, not a claim that all vectors across all workloads were counted.

### Verification commands

```bash
cd simulator && mvn test
cd verifiers && mvn test
cd verifiers && mvn -Dtest=ApplicationsFileTreeParserSpec,DummyappDynamicEnrichmentIntegrationSpec,DummyappAccountingFixtureFoundationSpec test
cd verifiers && mvn -Dtest=RecoveryScheduleGeneratorSpec test
docker compose run --rm fault-analysis-scenario-gen-test
```

All passed:

```text
targeted simulator controls/recovery/ordinary workflow: 17 tests, 0 failures/errors
complete simulator suite: 96 tests, 0 failures/errors
focused parser + dummyapp accounting/enrichment: 21 tests, 0 failures/errors
focused recovery scheduler: 14 tests, 0 failures/errors
complete verifier suite: 509 tests, 0 failures/errors
Docker fault-analysis-scenario-gen-test: exit 0
```

The full verifier suite initially exposed two stale integration expectations introduced by the v3 surface: parser discovery omitted `GroovySetupHelperOwnershipSpec`, and the dummyapp enrichment test still used the old workload-only writer call. Those tests were updated to the current file tree and complete v3 package/immutability contract, then the focused, full, and Docker runs passed.

## Historical v1/v2 evidence migration note

Sections below retain older `ScenarioPlan`, `scenario-catalog.jsonl`, enriched-wrapper, default-vector, and vector-overlay evidence for chronology. They are **not** current package or executor guidance. Current v3 uses the five-file WorkloadPlan/FaultScenario package, workload-linked dynamic sidecars, persisted FaultScenario selection, no vector overlay, and v4 execution reports. See [`reference/scenario-executor.md`](reference/scenario-executor.md) and [`decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`](decisions/2026-07-19-compensation-aware-fault-scenario-contract.md).

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
| Manifest-declared ScenarioExecutor setup candidates | unavailable | 94 |
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

Interpretation: static event topology improved accepted static input coverage for the implemented `EventHandling`/`EventProcessing` shape, including the original target group of five event-driven sagas. The refreshed dynamic baseline below confirms that the post-event static catalog can still be enriched with runtime evidence. It does not make event-origin inputs replayable: event payload placeholders remain materialization blockers. The historical `executorMaterializableInputVariantCount=94` is the manifest's static setup-candidate count, not proof that all 94 can start; the later 2026-07-27 preflight above found two constructor-type false positives in the corresponding accuracy-smoke candidate family.

## Historical v2 dynamic-enrichment Quizzes baselines

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

## Historical v2 ScenarioExecutor fault-vector smoke

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
Explicit vector terminal status: compensated terminal outcome
Explicit vector lifecycle: COMPENSATED
Explicit vector realized slot: 0 (runtime step `getCourseExecutionsStep`)
```

Interpretation: a narrow executor path supports the implemented saga/local fault-vector contract for the selected setup-ready input. This smoke executed one generated Quizzes single-saga plan by resolving runtime-owned infrastructure arguments and using the in-memory fault-vector provider for the explicit-fault run. Older accounting that reported zero executor-ready inputs measured static recipe readiness only; current docs distinguish manifest-declared setup candidates from actual executor setup. The later multi-saga smoke below extends the supported path to explicit deterministic interleaving replay, but the executor is still not generic catalog replay, broad runtime parity, impact scoring, or search.

## Historical v2 ScenarioExecutor multi-saga Quizzes smoke

Verified on 2026-07-09 with the planning-audit Quizzes multi-saga catalog through the forked Docker runtime path.

Command:

```bash
CATALOG_PATH=/reports/multi-saga-executor-planning-audit/quizzes-20260709-004627-310/scenario-catalog.jsonl \
SCENARIO_ID=0945caa9ac2fe06a268e6df6aa992fcf69e253116264684d577bdbbb955c2e25 \
OUTPUT_PATH=/reports/scenario-executor/multi-saga-default-report.json \
docker compose run --rm scenario-executor
```

Recorded result:

```text
Docker exit code: 0
Catalog: verifiers/target/multi-saga-executor-planning-audit/quizzes-20260709-004627-310/scenario-catalog.jsonl
Catalog sha256 before: 631538f64789c80a2b5b01291f0dd1a08b48966dfbaea4c5b805d7ecd48cafcd
Catalog sha256 after:  631538f64789c80a2b5b01291f0dd1a08b48966dfbaea4c5b805d7ecd48cafcd
Scenario plan id: 0945caa9ac2fe06a268e6df6aa992fcf69e253116264684d577bdbbb955c2e25
Report: verifiers/target/scenario-executor/multi-saga-default-report.json
Schema version: microservices-simulator.scenario-execution-report.v3
Scenario kind: MULTI_SAGA
Assigned vector: 00000
Vector source: DEFAULT_VECTOR
Provider mode: IN_MEMORY_FAULT_VECTOR
Terminal status: PARTIAL_COMPENSATED
```

Participants and schedule:

```text
scheduleOrder=0 participant=GetCourseExecutionsFunctionalitySagas runtimeStep=getCourseExecutionsStep status=COMPLETED lifecycle=COMMITTED
scheduleOrder=1 participant=CreateCourseExecutionFunctionalitySagas runtimeStep=getCourseStep status=COMPLETED lifecycle=COMPENSATED
scheduleOrder=2 participant=CreateCourseExecutionFunctionalitySagas runtimeStep=createCourseStep status=FAILED lifecycle=COMPENSATED
scheduleOrder=3 participant=CreateCourseExecutionFunctionalitySagas runtimeStep=createCourseExecutionStep skipped=SKIPPED_BY_SAGA_FAILURE
scheduleOrder=4 participant=CreateCourseExecutionFunctionalitySagas runtimeStep=updateCourseExecutionCountStep skipped=SKIPPED_BY_SAGA_FAILURE
```

Interpretation: the executor selected an explicit multi-saga plan, wrote the v3 participant report, replayed the catalog schedule sequentially, compensated the failing `CreateCourseExecutionFunctionalitySagas` participant, continued and committed `GetCourseExecutionsFunctionalitySagas`, exited zero for `PARTIAL_COMPENSATED`, and did not mutate the input catalog. The failure was a domain/runtime scheduled-step outcome from a null course name in the generated input, not a setup hard stop. This proves the supported deterministic interleaving replay path; it does not claim true concurrency, distributed parity, generic fixture reset, scoring, or search.

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
