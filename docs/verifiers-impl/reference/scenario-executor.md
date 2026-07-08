# ScenarioExecutor

The verifier-owned ScenarioExecutor supports one materializable single-saga saga/local scenario execution attempt per run.

A scenario execution attempt evaluates exactly one `{scenarioPlanId, faultVector}` pair and writes a standalone v2 execution report. It does not modify `scenario-catalog.jsonl`, `scenario-catalog-enriched.jsonl`, dynamic evidence artifacts, or join-report sidecars.

Supported now:

- catalog or enriched-catalog loading;
- supported single-saga scenario selection/materialization;
- default-vector execution using the plan's `faultSpace.defaultVector`;
- explicit binary fault-vector execution when `--scenario-id` is also provided;
- dry-run validation/mapping without runtime execution;
- lifecycle closure to `SUCCESS` or `FAULT_COMPENSATED` for the supported path.

Not supported now:

- generic arbitrary catalog replay;
- multi-saga execution;
- TCC execution;
- stream/gRPC/distributed runtime parity;
- compensation-step faults;
- delay/non-binary impairments;
- batch/search/scoring/prioritization;
- generic reset or impact telemetry.

## Command shape

Run the forked executor with an application classpath that contains the target application plus verifier executor classes:

```bash
java -cp <target-app-classpath>:<verifiers-classes-or-jar> \
  pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorCli \
  --spring-application-class <target.SpringApplication> \
  --spring-profiles test,sagas,local \
  [--application-base <application-base-or-directory>] \
  [--application-id <stable-application-id>] \
  [--maven-profile <profile-used-to-prepare-app>] \
  --catalog-path <run-dir>/scenario-catalog.jsonl \
  --output-path <run-dir>/scenario-execution-report.json \
  [--scenario-id <scenario-plan-id>] \
  [--fault-vector <binary-string>] \
  [--dry-run]
```

Rules:

- `--fault-vector` requires `--scenario-id`.
- When `--fault-vector` is omitted, the executor uses the selected plan's `faultSpace.defaultVector` and reports `vectorSource = DEFAULT_VECTOR`.
- Auto-selection is only for default-vector runs. Explicit vectors must pin the scenario id.
- The external fault-vector contract is a binary string aligned by index to `faultSpace.scheduledStepIds`.

## Docker Compose runner

The root compose file includes a one-shot `scenario-executor` service that consumes an existing catalog mounted through `./verifiers/target:/reports`.

Required environment:

- `CATALOG_PATH` — catalog path inside the container.

Optional environment:

- `SCENARIO_ID` — selected scenario plan id. Optional only for default-vector auto-select runs.
- `FAULT_VECTOR` — explicit binary vector. Requires `SCENARIO_ID`.
- `OUTPUT_PATH` — report path inside the container. Defaults to `/reports/scenario-executor/execution-report.json`.
- `APPLICATION_BASE_DIR`, `SPRING_APPLICATION_CLASS`, `MAVEN_PROFILE`, `SPRING_PROFILES` — target/runtime overrides.

Default-vector Quizzes smoke:

```bash
CATALOG_PATH=/reports/quizzes-20260708-163552-193/scenario-catalog.jsonl \
SCENARIO_ID=910f72907e0d901bc5d35e0ecea03ec920b7ffb63929bbba1bfdba4fe531e195 \
OUTPUT_PATH=/reports/scenario-executor/default-report.json \
docker compose run --rm scenario-executor
```

Explicit-vector Quizzes smoke:

```bash
CATALOG_PATH=/reports/quizzes-20260708-163552-193/scenario-catalog.jsonl \
SCENARIO_ID=910f72907e0d901bc5d35e0ecea03ec920b7ffb63929bbba1bfdba4fe531e195 \
FAULT_VECTOR=1 \
OUTPUT_PATH=/reports/scenario-executor/explicit-report.json \
docker compose run --rm scenario-executor
```

The service reinstalls the simulator into a writable temp directory inside the container, builds verifier classes, prepares the target application with `test-compile`, builds the runtime classpath, disables the verifier Spring application inside the target context with `--verifiers.application.enabled=false`, and uses `--server.port=0` by default.

## Catalog loading and selection

When a run directory is used by the executor API, `scenario-catalog-enriched.jsonl` is preferred when present. If it is absent, `scenario-catalog.jsonl` is used.

Enriched records are wrappers: execution uses the embedded static `ScenarioPlan` as the contract, while join status is only a deterministic selection hint.

`--scenario-id` refers to a scenario plan id. If it is omitted, the executor auto-selects the first supported single-saga candidate using enriched join-status priority and original JSONL line order.

## Runtime-owned overrides

Only these runtime-owned arguments may be supplied by the executor:

- `SagaUnitOfWorkService`, resolved from the target Spring context.
- `CommandGateway`, resolved from the target Spring context.
- `SagaUnitOfWork`, created by the executor for the selected saga instance.

These runtime-owned arguments are part of the executor materialization semantics. Their blocked recipe internals do not make the input unmaterializable because the executor supplies them directly.

Other unresolved values, source-provided placeholders without values, unsupported call results, and unsupported transforms remain blockers. Static `inputRecipe.executorReady=false` does not automatically mean the ScenarioExecutor cannot run the input; it may only mean the raw recipe tree is not fully replayable without executor runtime overrides.

## Report and terminal status vocabulary

The v2 execution report includes schema version, `scenarioExecutionId`, scenario plan id, assigned vector, vector source, provider mode, catalog path/kind, runtime metadata, deterministic fault-slot mapping, realized/masked slot state, forward step outcomes, lifecycle outcome, terminal status, blockers, and errors. Runtime metadata records stable runner inputs, including application base/id, Spring application class, Spring profiles, Maven profile, catalog path/kind, scenario id, vector source, executor mode, and dry-run mode; saga class identity remains in the top-level `sagaFqn` field.

Reference-level status vocabulary:

- `terminalStatus`:
  - valid outcomes: `SUCCESS`, `FAULT_COMPENSATED`, `DRY_RUN`;
  - invalid/broken outcomes: `INVALID_FAULT_VECTOR`, `SELECTION_FAILED`, `UNSUPPORTED_SCENARIO`, `MATERIALIZATION_FAILED`, `STARTUP_FAILED`, `EXPECTED_FAULT_NOT_INJECTED`, `UNEXPECTED_INJECTED_FAULT`, `FAULT_PROVIDER_MISMATCH`, `UNEXPECTED_EXECUTION_FAILURE`, `COMPENSATION_FAILED`.
- `lifecycleOutcome`: `NOT_STARTED`, `COMMITTED`, `COMPENSATED`, `COMPENSATION_FAILED`, `CLOSURE_SKIPPED`.
- `vectorSource`: `DEFAULT_VECTOR`, `EXPLICIT_VECTOR`.
- `providerMode`: `IN_MEMORY_FAULT_VECTOR`, `NONE`.
- fault-slot realization state: `NOT_ASSIGNED`, `REALIZED`, `MASKED`, `UNREALIZED`.

Exit-code contract:

- zero: `SUCCESS`, `FAULT_COMPENSATED`, `DRY_RUN`;
- non-zero: every other terminal status.

## Accounting alignment

`scenario-space-accounting.json` distinguishes static recipe readiness from ScenarioExecutor materializability.

- Static recipe readiness reports whether `inputRecipe.executorReady` is true without executor-specific overrides.
- Executor materializability reports whether the current ScenarioExecutor can materialize the accepted input using supported recipe semantics plus runtime-owned argument resolution.

## Quizzes smoke evidence

Verified on 2026-07-08 with a real verifier-generated Quizzes catalog through the forked Docker runtime path:

- Catalog artifact: `verifiers/target/quizzes-20260708-163552-193/scenario-catalog.jsonl`.
- Scenario plan id: `910f72907e0d901bc5d35e0ecea03ec920b7ffb63929bbba1bfdba4fe531e195`.
- Saga FQN: `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.GetCourseExecutionsFunctionalitySagas`.
- Fault-space length: `1`.
- Default vector smoke: `assignedVector=0`, `vectorSource=DEFAULT_VECTOR`, `terminalStatus=SUCCESS`, `lifecycleOutcome=COMMITTED`.
- Explicit vector smoke: `assignedVector=1`, `vectorSource=EXPLICIT_VECTOR`, slot `0` realized at runtime step `getCourseExecutionsStep`, `terminalStatus=FAULT_COMPENSATED`, `lifecycleOutcome=COMPENSATED`.

See [`../evidence.md`](../evidence.md) for commands, report paths, and interpretation.

## Limitations

The current executor is intentionally narrow even after fault-vector support:

- supported runtime shape is materializable single-saga saga/local only;
- fault slots target forward scheduled steps only;
- compensation steps are not faultable;
- persistent-environment reset remains caller/orchestrator responsibility;
- behavior CSV/manual impairment remains compatibility behavior outside executor-owned vector runs and is not the executor contract.
