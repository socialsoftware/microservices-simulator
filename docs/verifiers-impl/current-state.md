# Verifier current state

Last updated: 2026-07-21

This is the present-tense status page for verifier/scenario-generation work. Detailed validation lives in [`evidence.md`](evidence.md); terminology lives in [`glossary.md`](glossary.md); executor operation lives in [`reference/scenario-executor.md`](reference/scenario-executor.md).

## One-paragraph summary

The verifier extracts saga-oriented structure and test-derived inputs, generates a deterministic compensation-aware v3 package, and can optionally attach runtime evidence as workload-linked sidecars. The package separates reusable `WorkloadPlan` records from executable `FaultScenario` records: eager generation writes materializable all-zero and single-point vectors, while a guarded on-demand path persists arbitrary valid multi-fault vectors. A narrow saga/local ScenarioExecutor replays one persisted FaultScenario and writes an action-aware v4 report. Generic/distributed execution, impact scoring, GA search, and prioritization remain future work.

## Current scope

Implemented:

- Java/Groovy static extraction of Saga participants, ordered steps, dispatch footprints, compensation evidence, and test-derived input recipes.
- Static event semantics for the implemented `EventHandling`/`EventProcessing` chain shape.
- Source-mode filtering (`SAGAS`, `TCC`, `MIXED`, `UNKNOWN`) with rejected-input diagnostics.
- Deterministic bounded WorkloadPlan generation, including single-saga and bounded multi-saga shapes.
- `SERIAL`, bounded order-preserving, and conflict-anchor `SEGMENT_COMPRESSED` forward scheduling.
- Compensation checkpoints classified as `EXPLICIT_COMPENSATION`, `IMPLICIT_SAGA_ROLLBACK`, or `CONSERVATIVE_UNKNOWN`.
- Bounded recovery-schedule generation with reverse per-participant compensation order and survivor-forward interleavings.
- Eager FaultScenarios for materializable all-zero and single-point vectors. Saga/local materializability requires each participant's forward `runtimeStepName` values to be unique; the same name may be used by different participants.
- Idempotent on-demand persistence with package-local writer serialization across local JVM processes.
- Exact accounting for computed vectors, including uncapped and written recovery-schedule counts.
- Optional workload-linked dynamic-evidence sidecars that leave semantic package bytes unchanged.
- Narrow saga/local ScenarioExecutor replay of one persisted FaultScenario, including zero-bit fallback and hard-stop policy.
- HTML analysis report and Docker Compose generation/test/executor services.

Main targets:

- `applications/dummyapp/` for parser, generation, enrichment, and executor regression fixtures.
- `applications/quizzes/` for realistic bounded generation and runtime smokes.

Non-goals/current exclusions:

- TCC scenario execution.
- Stream/gRPC/distributed runtime parity or true concurrency.
- Quizzes-specific verifier shortcuts.
- Runtime vector overlays or behavior CSV as the executor contract.
- Dynamic evidence rewriting WorkloadPlans or FaultScenarios.
- Compensation faults, automatic recovery retry/backoff, impact scoring, GA search, or prioritization.

## v3 scenario package

A generated run publishes these semantic artifacts:

- `workload-catalog.jsonl` — deterministic `microservices-simulator.workload-plan.v3` records;
- `fault-scenario-catalog.jsonl` — deterministic `microservices-simulator.fault-scenario.v3` records;
- `workload-catalog-rejected-inputs.jsonl` — source-mode/input rejection diagnostics;
- `scenario-space-accounting.json` — `microservices-simulator.scenario-space-accounting.v3`;
- `scenario-catalog-manifest.json` — `microservices-simulator.scenario-catalog-manifest.v3` with linked paths, counts, hashes, policy, materializability rows, and recovery cap.

The v2 `scenario-catalog.jsonl` / `ScenarioPlan` contract is unsupported by the current package reader and executor. Older docs and evidence may mention it only as explicitly historical v1/v2 material; the migration decision is [`decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`](decisions/2026-07-19-compensation-aware-fault-scenario-contract.md).

### WorkloadPlan

A WorkloadPlan is reusable normal-execution structure. It owns participant instances, accepted inputs, one deterministic global forward interleaving, conflict evidence, ordered forward fault slots, compensation checkpoints, and stable identities. Scheduled-step occurrence ids remain semantic identities used by slots, checkpoints, actions, and reports. Because the current Saga/local runtime stores execution and recovery state by runtime step name, structural admissibility rejects repeated `(sagaInstanceId, runtimeStepName)` forward occurrences. The same runtime name in separate participants remains valid. A WorkloadPlan owns neither an assigned vector nor a recovery ordering.

### FaultScenario

A FaultScenario is one reproducible experiment. It references one WorkloadPlan and persists one binary vector plus one complete ordered action sequence. Actions are `FORWARD` or `COMPENSATION`; action identity is part of FaultScenario identity. A participant commits automatically on its final successful forward step, so commit is reported but not independently schedulable.

Eager generation is intentionally bounded to materializable all-zero and single-point vectors. Non-materializable workloads remain in the workload catalog with diagnostics but receive no eager FaultScenarios. The on-demand request path serializes writers for the same real package directory across local JVM processes with the persistent `.on-demand-fault-scenario.lock` OS lock. It acquires that lock before package read/validation and holds it through generation, publication, final validation, and every result path, so a waiting writer re-reads the preceding validated revision. Invalid requests and caught generation/publication failures preserve or restore the prior three mutable semantic artifact bytes; a successful return means the resulting revision passed package validation.

The three semantic files are still promoted separately. A process/JVM crash, kernel or host failure, or power loss during promotion can therefore leave a checksum-inconsistent package, and there is no automatic crash recovery. Readers remain checksum-protected and reject such torn packages; regenerate the package before retrying after an integrity failure. The `FileChannel` guarantee is for cooperating processes on a local filesystem and does not establish network-filesystem or multi-host distributed locking semantics.

### Recovery accounting

`scenario-space-accounting.json` separates:

- WorkloadPlan counts and per-workload materializability;
- possible binary vectors per workload;
- eager and on-demand computed-vector counts;
- exact uncapped and cap-written recovery-schedule counts for each computed vector;
- exact sums over computed vectors only.

The generator uses `BigInteger` counting and bounded materialization. A synthetic `RecoveryScheduleGeneratorSpec` high-cardinality fixture validates exact uncapped count `118264581564861424` while retaining only `20` schedules, visiting `992` counting states, and materializing fewer than `100` leaves.

## Dynamic enrichment

Dynamic enrichment remains optional and disabled by default. When enabled, the verifier runs selected tests with simulator evidence hooks, reads runtime JSONL, and writes:

- `workload-dynamic-evidence.jsonl` (`microservices-simulator.workload-dynamic-evidence.v3`);
- `workload-dynamic-evidence-manifest.json`;
- `dynamic-evidence-join-report.json`.

Records link through `workloadPlanId` and input ids and use conservative statuses: `MATCHED_EXACT`, `MATCHED_HIGH_CONFIDENCE`, `MATCHED_PARTIAL`, `AMBIGUOUS`, `UNMATCHED`, and `NOT_COVERED`. They do not embed/rewrite WorkloadPlans or FaultScenarios. Dummyapp integration coverage snapshots all five semantic package artifacts and verifies byte equality after sidecar publication.

The latest broad Quizzes attribution counts (`MATCHED_EXACT=435`, `MATCHED_HIGH_CONFIDENCE=125`, `AMBIGUOUS=0`, `UNMATCHED=24`) came from the 2026-06-30 v2-era baseline. They remain useful historical attribution evidence, but they are not a fresh v3 Quizzes enrichment baseline and their old artifact names must not be used as current package guidance.

## ScenarioExecutor

The current executor requires a v3 package path and an exact persisted FaultScenario id. It derives the assigned vector and action order from that record; it does not accept `--fault-vector`, `FAULT_VECTOR`, auto-selection, or v2 catalogs.

Supported execution semantics:

- materialize all supported saga/local participants and runtime-owned infrastructure arguments;
- replay persisted actions sequentially;
- inject assigned faults at their exact forward slots;
- execute scheduled checkpoint compensation in persisted order;
- commit each participant automatically after its final successful forward action;
- on a zero-bit body or commit failure explicitly marked with the simulator `DomainFailure` contract, deviate once into immediate runtime-checkpoint recovery, skip that participant's remaining forwards, and continue valid survivor actions;
- treat plain `SimulatorException`, service unavailability, ordinary runtime failures, leaked assigned-fault exceptions, and all unknown/unmarked failures as infrastructure: run no fallback, stop survivor execution, and report `INCOMPLETE` after measured execution starts;
- hard-stop at the first executor/infrastructure failure or thrown compensation action;
- never mutate the five package artifacts.

The action-aware report schema is `microservices-simulator.scenario-execution-report.v4`. It includes planned and actual action order, runtime occurrence/source ids, body and commit outcomes, recovery sub-outcomes, fault origin, fault-slot realization/masking, lifecycle events, participant final state, blockers, and `EXACT` / `DEVIATED` / `INCOMPLETE` conformance when measured execution begins.

`QuizzesException` business/invariant failures and the Saga/local transactional `SimulatorDomainException` carry the explicit marker. `QuizzesConfigurationException`, including every undefined-transactional-model failure, is unmarked. Base-class membership is deliberately insufficient: extending `SimulatorException` does not by itself make a failure a domain outcome. Supported local command-response restoration preserves the exact supported exception class, its template, and its formatted message.

This remains a narrow deterministic sequential Saga/local replay path. The classification migration does not claim causal, TCC, stream, gRPC, generic distributed, or true-parallel parity.

## Current evidence

### Bounded Quizzes v3 package and execution

Verified 2026-07-20:

```text
run: verifiers/target/compensation-aware-v3-evidence/bounded-quizzes-v3/quizzes-20260720-091007-712/
generationStrategy: BRUTE_FORCE
catalogWriteMode: WRITE_WORKLOADS
includeSingles: false
maxSagaSetSize: 2
maxCatalogScenarios: 2000
maxInputVariantsPerSaga: 2
maxSchedulesPerInputTuple: 4
scheduleStrategy: SEGMENT_COMPRESSED
recoveryScheduleCap: 20
WorkloadPlans written/materializable: 2000 / 12
FaultScenarios written: 84
legacy scenario-catalog.jsonl present: false
```

Selected compensation-interleaving scenario:

```text
WorkloadPlan: 01c49ae4314e106161ccc75f7531c62d01299417e702908100d7fb809ca2face
FaultScenario: 25c0d61a2a2b40c2aaff7946aca8d2bb1becfc54b8b168442bcff40262052271
participants: CreateCourseExecutionFunctionalitySagas + GetCourseExecutionsFunctionalitySagas
assignedVector: 00010
recovery schedules uncapped/written: 3 / 3
planned actions: 5 forwards + 2 compensations
interleaving: compensation, surviving participant forward, compensation
```

The saved Docker execution report is **pre-remediation historical evidence**, not evidence of the current classifier. It exited zero with `terminalStatus=PARTIAL_COMPENSATED` and `scheduleConformance=DEVIATED`, but its zero-bit `createCourseStep` failure is a plain `SimulatorException` reporting service unavailability after command retries. The old executor incorrectly ran fallback and continued the survivor. Under the current explicit-marker contract, the same unmarked failure is infrastructure: it runs no fallback, stops survivor execution, and reports `UNEXPECTED_EXECUTION_FAILURE / INCOMPLETE` after the measured action. The saved artifact is intentionally unchanged. All five package SHA-256 values were unchanged by that historical execution.

Artifacts:

- summary: `verifiers/target/compensation-aware-v3-evidence/verification-summary.json`;
- generation log: `verifiers/target/compensation-aware-v3-evidence/bounded-quizzes-v3-container.log`;
- report: `verifiers/target/compensation-aware-v3-evidence/execution-report-25c0d61a.json`;
- executor log: `verifiers/target/compensation-aware-v3-evidence/executor-25c0d61a-container.log`;
- checksums: `verifiers/target/compensation-aware-v3-evidence/package.sha256.before` and `.after`.

### Regression validation

- Complete simulator Maven suite passed.
- Complete verifier Maven suite passed after updating stale parser discovery and v3 dynamic-enrichment integration expectations.
- Docker `fault-analysis-scenario-gen-test` passed.
- Focused high-cardinality recovery accounting passed with exact count and bounded traversal assertions.

See [`evidence.md`](evidence.md) for commands and totals.

## Current limitations

- Exact aggregate-instance key extraction remains incomplete.
- Thirty-two Quizzes sagas still lack accepted static inputs; this does not imply that no tests exist.
- Event payload placeholders may permit static acceptance while blocking materialization.
- The materializability policy is static readiness plus structural admissibility; runtime success remains unproven until execution. Replaying repeated same-participant runtime step names requires future occurrence-aware runtime state and is not currently materializable.
- Segment compression is a deterministic reduction under extracted conflict evidence, not semantic-completeness proof.
- Dynamic enrichment is local/sagas-focused; no fresh full Quizzes v3 dynamic baseline has been recorded.
- No current post-remediation Quizzes smoke demonstrates an explicitly marked zero-bit domain fallback. The saved smoke predates explicit classification and encountered unmarked service unavailability, which current execution would treat as an infrastructure hard stop.
- Persistent-environment reset is caller/orchestrator responsibility.

## Not implemented

- Generic runtime replay for every WorkloadPlan/FaultScenario shape.
- TCC, stream, gRPC, distributed, or true-concurrent execution parity.
- Compensation faults, delay injection, or non-binary impairments.
- Automatic compensation retries.
- Behavior CSV generation from v3 package records.
- Domain-impact scoring, genetic/local search, or scenario prioritization.
- Semantic deduplication of value-equivalent inputs.
- Profile-aware resolution for ambiguous multiple `@Service` implementations.

## Next priorities

1. Improve accepted-input quality and event payload materialization where runtime failures expose concrete blockers.
2. Classify the remaining Quizzes sagas without accepted inputs.
3. Refresh a representative Quizzes dynamic-enrichment baseline against the workload-linked v3 sidecar contract.
4. Improve aggregate-instance key binding where it affects WorkloadPlan usefulness.
5. Add first domain-impact metrics before introducing search/prioritization.
