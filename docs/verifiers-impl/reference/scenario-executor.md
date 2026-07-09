# ScenarioExecutor

The verifier-owned ScenarioExecutor supports one materializable saga/local scenario execution attempt per run. Supported attempts include single-saga plans and explicit, materializable multi-saga plans replayed as deterministic sequential interleavings.

A scenario execution attempt evaluates exactly one `{scenarioPlanId, faultVector}` pair and writes a standalone v3 execution report. It does not modify `scenario-catalog.jsonl`, `scenario-catalog-enriched.jsonl`, dynamic evidence artifacts, or join-report sidecars.

Supported now:

- catalog or enriched-catalog loading;
- supported single-saga scenario selection/materialization;
- explicit multi-saga scenario selection by `ScenarioPlan.deterministicId`;
- default-vector execution using the plan's `faultSpace.defaultVector`;
- explicit binary fault-vector execution when `--scenario-id` is also provided;
- dry-run validation/mapping without runtime execution;
- deterministic sequential replay of `expandedSchedule` in ascending `scheduleOrder` for materializable saga/local multi-saga plans;
- one v3 participant entry per saga instance;
- compensate-and-continue for scheduled-step runtime failures and assigned forward-step faults.

Not supported now:

- generic arbitrary runtime parity for every catalog shape;
- auto-selection of multi-saga scenarios;
- TCC execution;
- stream/gRPC/distributed runtime parity;
- true parallel/threaded execution;
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

- `SCENARIO_ID` — selected scenario plan id. Required for multi-saga runs; optional only for default-vector single-saga auto-select runs.
- `FAULT_VECTOR` — explicit binary vector. Requires `SCENARIO_ID`.
- `OUTPUT_PATH` — report path inside the container. Defaults to `/reports/scenario-executor/execution-report.json`.
- `APPLICATION_BASE_DIR`, `SPRING_APPLICATION_CLASS`, `MAVEN_PROFILE`, `SPRING_PROFILES` — target/runtime overrides.

Default-vector multi-saga Quizzes smoke:

```bash
CATALOG_PATH=/reports/multi-saga-executor-planning-audit/quizzes-20260709-004627-310/scenario-catalog.jsonl \
SCENARIO_ID=0945caa9ac2fe06a268e6df6aa992fcf69e253116264684d577bdbbb955c2e25 \
OUTPUT_PATH=/reports/scenario-executor/multi-saga-default-report.json \
docker compose run --rm scenario-executor
```

Explicit-vector single-saga Quizzes smoke:

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

`--scenario-id` refers to a scenario plan id. If it is omitted, the executor auto-selects the first supported single-saga candidate using enriched join-status priority and original JSONL line order. Multi-saga execution always requires an explicit scenario id.

## Runtime-owned overrides

Only these runtime-owned arguments may be supplied by the executor:

- `SagaUnitOfWorkService`, resolved from the target Spring context.
- `CommandGateway`, resolved from the target Spring context.
- `SagaUnitOfWork`, created by the executor for the selected saga instance or participant.

These runtime-owned arguments are part of the executor materialization semantics. Their blocked recipe internals do not make the input unmaterializable because the executor supplies them directly.

Other unresolved values, source-provided placeholders without values, unsupported call results, and unsupported transforms remain blockers. Static `inputRecipe.executorReady=false` does not automatically mean the ScenarioExecutor cannot run the input; it may only mean the raw recipe tree is not fully replayable without executor runtime overrides.

## Report and terminal status vocabulary

The v3 execution report includes schema version, `scenarioExecutionId`, scenario plan id, scenario kind, assigned vector, vector source, provider mode, catalog path/kind, runtime metadata, deterministic fault-slot mapping, participant entries, terminal status, blockers, and errors. Runtime metadata records stable runner inputs, including application base/id, Spring application class, Spring profiles, Maven profile, catalog path/kind, scenario id, vector source, executor mode, and dry-run mode.

The top-level `participants` list is required. It contains exactly one participant per selected saga instance; single-saga runs are represented as the one-participant case. Saga-specific identity and lifecycle facts live under participant entries, not as v2 top-level `sagaFqn`, `sagaInstanceId`, or `inputVariantId` fields.

Reference-level status vocabulary:

- `terminalStatus`:
  - zero-exit outcomes: `SUCCESS`, `COMPENSATED`, `PARTIAL_COMPENSATED`, `DRY_RUN`;
  - invalid/broken outcomes: `INVALID_FAULT_VECTOR`, `SELECTION_FAILED`, `UNSUPPORTED_SCENARIO`, `MATERIALIZATION_FAILED`, `STARTUP_FAILED`, `EXPECTED_FAULT_NOT_INJECTED`, `UNEXPECTED_INJECTED_FAULT`, `FAULT_PROVIDER_MISMATCH`, `UNEXPECTED_EXECUTION_FAILURE`, `COMPENSATION_FAILED`, `CONFIGURATION_FAILED`, `REPORT_WRITE_FAILED`.
- `materializationState`: `NOT_ATTEMPTED`, `MATERIALIZED`, `MATERIALIZATION_FAILED`.
- `startupState`: `NOT_ATTEMPTED`, `STARTUP_READY`, `STARTUP_FAILED`.
- `lifecycleOutcome`: `NOT_STARTED`, `COMMITTED`, `COMPENSATED`, `COMPENSATION_FAILED`, `CLOSURE_SKIPPED`.
- step status: `DRY_RUN`, `COMPLETED`, `INJECTED_FAULT`, `FAILED`, `EXPECTED_FAULT_NOT_INJECTED`, `SKIPPED_BY_SAGA_FAILURE`, `NOT_EXECUTED_HARD_STOP`.
- `vectorSource`: `DEFAULT_VECTOR`, `EXPLICIT_VECTOR`.
- `providerMode`: `IN_MEMORY_FAULT_VECTOR`, `NONE`.
- fault-slot realization state: `NOT_ASSIGNED`, `DRY_RUN`, `REALIZED`, `MASKED_BY_SAGA_FAILURE`, `NOT_REACHED`, `EXPECTED_FAULT_NOT_INJECTED`.

Exit-code contract:

- zero: `SUCCESS`, `COMPENSATED`, `PARTIAL_COMPENSATED`, `DRY_RUN`;
- non-zero: every other terminal status.

## Accounting alignment

`scenario-space-accounting.json` distinguishes static recipe readiness from ScenarioExecutor materializability.

- Static recipe readiness reports whether `inputRecipe.executorReady` is true without executor-specific overrides.
- Executor materializability reports whether the current ScenarioExecutor can materialize the accepted input using supported recipe semantics plus runtime-owned argument resolution.

## Quizzes smoke evidence

Verified on 2026-07-09 with a real verifier-generated Quizzes multi-saga catalog through the forked Docker runtime path:

- Catalog artifact: `verifiers/target/multi-saga-executor-planning-audit/quizzes-20260709-004627-310/scenario-catalog.jsonl`.
- Scenario plan id: `0945caa9ac2fe06a268e6df6aa992fcf69e253116264684d577bdbbb955c2e25`.
- Saga FQNs: `CreateCourseExecutionFunctionalitySagas` and `GetCourseExecutionsFunctionalitySagas`.
- Fault-space length: `5`.
- Default vector smoke: `assignedVector=00000`, `vectorSource=DEFAULT_VECTOR`, `terminalStatus=PARTIAL_COMPENSATED`, participant lifecycles `COMPENSATED` and `COMMITTED`.
- The input catalog checksum was unchanged before and after non-dry-run execution.

See [`../evidence.md`](../evidence.md) for commands, report paths, participant details, and interpretation.

## Limitations

The current executor is intentionally narrow even after fault-vector support:

- supported runtime shape is materializable saga/local only, with multi-saga support limited to explicit deterministic sequential interleaving replay;
- multi-saga plans are not auto-selected;
- fault slots target forward scheduled steps only;
- compensation steps are not faultable;
- persistent-environment reset remains caller/orchestrator responsibility;
- behavior CSV/manual impairment remains compatibility behavior outside executor-owned vector runs and is not the executor contract.
