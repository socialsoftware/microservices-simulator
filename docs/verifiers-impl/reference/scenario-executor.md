# ScenarioExecutor

The verifier-owned ScenarioExecutor runs exactly one persisted v3 `FaultScenario` per attempt. It supports materializable saga/local single- and multi-participant workloads through deterministic sequential replay. It is not a generic distributed runner or a true-concurrency engine.

The executor consumes a complete v3 package and the selected `FaultScenario.deterministicId`. The assigned vector and ordered `FORWARD`/`COMPENSATION` actions come from that persisted record. Runtime vector overlays are deliberately unsupported.

## Supported contract

Supported now:

- v3 package loading through `scenario-catalog-manifest.json` or its containing directory;
- exact `FaultScenario` selection by persisted id;
- supported input materialization plus runtime-owned Saga arguments;
- deterministic replay of persisted action order;
- automatic participant commit after its final successful forward action;
- assigned faults at forward fault slots;
- scheduled compensation actions with explicit, implicit Saga rollback, or conservative-unknown evidence;
- immediate checkpoint recovery and survivor continuation when an unassigned zero-bit domain/simulator failure causes a reported schedule deviation;
- first-failure hard stops for executor/infrastructure failures and thrown compensation actions;
- dry-run package/selection/mapping checks;
- standalone v4 action-aware execution reports without package mutation.

Not supported:

- v2 `ScenarioPlan` catalogs, `scenario-catalog.jsonl`, enriched-catalog wrappers, or runtime fault-vector overlays;
- automatic FaultScenario selection;
- TCC execution, stream/gRPC/distributed parity, or true parallel execution;
- compensation fault slots, delays, or non-binary impairments;
- automatic compensation retries, retry counts, or backoff;
- generic reset, impact scoring, batch search, or prioritization.

## Required v3 package

The manifest links four semantic artifacts; together the run directory contains these five package files:

- `workload-catalog.jsonl` — `microservices-simulator.workload-plan.v3` records;
- `fault-scenario-catalog.jsonl` — `microservices-simulator.fault-scenario.v3` records;
- `scenario-space-accounting.json` — `microservices-simulator.scenario-space-accounting.v3`;
- `workload-catalog-rejected-inputs.jsonl` — rejected-input diagnostics;
- `scenario-catalog-manifest.json` — `microservices-simulator.scenario-catalog-manifest.v3`, linked paths, exact record counts, SHA-256 metadata, generation policy, materializability diagnostics, and recovery cap.

A `WorkloadPlan` owns participants, accepted inputs, the normal forward interleaving, conflict evidence, ordered forward fault slots, and compensation checkpoints. A `FaultScenario` references one WorkloadPlan and owns one assigned binary vector plus one complete ordered action schedule. Its deterministic identity includes the workload id, assigned vector, and ordered action identities.

The all-zero and single-point vectors are generated eagerly only for workloads that pass the package materializability/admissibility policy. Arbitrary valid multi-fault vectors use the bounded on-demand mutation path described below.

## Command shape

Run the forked executor with an application classpath containing the target application and verifier executor classes:

```bash
java -cp <target-app-classpath>:<verifiers-classes-or-jar> \
  pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorCli \
  --spring-application-class <target.SpringApplication> \
  --spring-profiles test,sagas,local \
  [--application-base <application-base-or-directory>] \
  [--application-id <stable-application-id>] \
  [--maven-profile <profile-used-to-prepare-app>] \
  --package-path <run-dir>/scenario-catalog-manifest.json \
  --fault-scenario-id <persisted-fault-scenario-id> \
  --output-path <report-dir>/scenario-execution-report.json \
  [--dry-run]
```

`--package-path`, `--fault-scenario-id`, and `--output-path` are required. There is no `--fault-vector` option: persist the required vector as a FaultScenario before execution. The direct Java CLI accepts either the manifest path or its containing package directory.

## Docker Compose runner

The root `scenario-executor` service mounts `./verifiers/target` as `/reports` and invokes `verifiers/scripts/run-scenario-executor.sh`. Unlike the direct Java CLI, this wrapper performs a regular-file preflight and requires the explicit manifest path.

Required environment:

- `PACKAGE_PATH` — v3 `scenario-catalog-manifest.json` file path inside the container; package directories are not accepted by this wrapper;
- `FAULT_SCENARIO_ID` — one id already present in `fault-scenario-catalog.jsonl`.

Optional environment:

- `OUTPUT_PATH` — report path; default `/reports/scenario-executor/execution-report.json`;
- `APPLICATION_BASE_DIR`, `SPRING_APPLICATION_CLASS`, `MAVEN_PROFILE`, `SPRING_PROFILES`, `SERVER_PORT` — target/runtime overrides.

Validated Quizzes compensation-interleaving example:

```bash
PACKAGE_PATH=/reports/compensation-aware-v3-evidence/bounded-quizzes-v3/quizzes-20260720-091007-712/scenario-catalog-manifest.json \
FAULT_SCENARIO_ID=25c0d61a2a2b40c2aaff7946aca8d2bb1becfc54b8b168442bcff40262052271 \
OUTPUT_PATH=/reports/compensation-aware-v3-evidence/execution-report-25c0d61a.json \
docker compose run --rm scenario-executor
```

The service installs the simulator into a writable temporary directory, builds verifier classes, prepares the target application with `test-compile`, assembles the runtime classpath, disables the verifier Spring application inside the target context, and uses a random server port by default.

## On-demand multi-fault persistence

Do not pass an ad hoc vector to the executor. Persist it atomically first:

```bash
java -cp <verifiers-classes-and-dependencies> \
  pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.FaultScenarioRequestCli \
  --manifest-path <run-dir>/scenario-catalog-manifest.json \
  --workload-plan-id <workload-plan-id> \
  --fault-vector <binary-vector> \
  [--recovery-schedule-cap <must-match-package-cap>]
```

The request path validates the package, workload, vector length/content, materializability, structural admissibility, and package recovery cap before mutation. On success it generates bounded recovery schedules, merges records in canonical id order, updates exact accounting and manifest metadata, and publishes the FaultScenario catalog, accounting, and manifest as one guarded revision. Repeating the same request returns `DEDUPLICATED`; invalid or inconsistent requests do not mutate package bytes.

`OnDemandFaultScenarioResult.status` is `PERSISTED`, `DEDUPLICATED`, `REJECTED`, `INTEGRITY_FAILURE`, or `PERSISTENCE_FAILED`. Only the first two are successful.

## Runtime semantics

For each persisted action:

- `FORWARD` with bit `0` invokes the named forward step;
- `FORWARD` with bit `1` realizes the assigned fault at that exact slot and aborts that participant;
- `COMPENSATION` invokes the checkpoint named by the persisted action;
- the final successful forward action commits its participant automatically; commit is not independently schedulable.

An assigned fault follows the persisted recovery ordering. If a zero-bit forward body or automatic commit throws a domain/simulator failure, execution deviates deterministically: recover that participant immediately from runtime checkpoint truth, skip its remaining forwards, and continue valid actions owned by other participants. Executor/infrastructure failures and any failed compensation action hard-stop the measured prefix. Compensation can be retried only by a later explicit invocation; the executor has no automatic retry loop.

Only these runtime-owned constructor arguments may be supplied by the executor:

- `SagaUnitOfWorkService` from the target Spring context;
- `CommandGateway` from the target Spring context;
- `SagaUnitOfWork` created for each selected participant.

Other unresolved values, unsupported calls/transforms, and event payload placeholders remain materialization blockers.

## v4 report

`microservices-simulator.scenario-execution-report.v4` includes:

- package manifest path, WorkloadPlan id, FaultScenario id, scenario kind, assigned vector, and provider mode;
- ordered `plannedActions` and measured `actualActions` with action kind, stable source ids, runtime occurrence, body/commit outcome, fault origin, and recovery sub-outcomes;
- `faultSlots`, participant materialization/startup/final state, skipped actions, lifecycle events, and blockers;
- schedule conformance and, when applicable, first deviation or hard-stop metadata.

Conformance values:

- `EXACT` — measured execution completed the persisted order;
- `DEVIATED` — execution completed through the zero-bit immediate-recovery fallback;
- `INCOMPLETE` — a hard stop occurred after at least one measured action;
- absent — measured action execution never began.

Zero-exit terminal statuses are `SUCCESS`, `COMPENSATED`, `PARTIAL_COMPENSATED`, and `DRY_RUN`. Selection, materialization, startup, provider, infrastructure, compensation, configuration, and report-write failures are non-zero outcomes. Package/CLI preflight errors may terminate before a persisted report can be written.

Execution reports must not alias any package artifact. Normal and deviated execution leave all five semantic package files byte-identical.

## Dynamic evidence boundary

Dynamic enrichment is separate from execution and from semantic package identity. Current v3 enrichment writes:

- `workload-dynamic-evidence.jsonl`;
- `workload-dynamic-evidence-manifest.json`;
- `dynamic-evidence-join-report.json`.

Sidecar records link by `workloadPlanId` and input ids. They do not embed or rewrite WorkloadPlans/FaultScenarios, and the executor does not use them to redefine the persisted action schedule.

## Current evidence and limits

The 2026-07-20 bounded Quizzes run wrote 2,000 multi-saga WorkloadPlans, classified 12 as eagerly materializable, and wrote 84 eager FaultScenarios under `SEGMENT_COMPRESSED` scheduling and recovery cap `20`. Selected FaultScenario `25c0d61a...` has vector `00010` and a seven-action schedule in which `CreateCourseExecutionFunctionalitySagas` compensation surrounds a surviving `GetCourseExecutionsFunctionalitySagas` forward action.

The Docker executor completed with `PARTIAL_COMPENSATED / DEVIATED`: a zero-bit `createCourseStep` failed because the extracted DTO carried a null course name, immediate no-work recovery compensated that participant, and the survivor committed. This is an honest input-quality/runtime deviation, not evidence of exact replay. The v4 report, container log, verification summary, and before/after package checksums are under `verifiers/target/compensation-aware-v3-evidence/`.

See [`../evidence.md`](../evidence.md) for full commands, counts, paths, hashes, and interpretation. Historical v2 executor smokes remain evidence of the superseded `ScenarioPlan` path only; they are not current invocation guidance.
