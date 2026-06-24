# Scenario Executor POC

The verifier-owned scenario executor consumes a scenario catalog or enriched catalog and writes an executor-specific execution report. It does not modify `scenario-catalog.jsonl`, `scenario-catalog-enriched.jsonl`, dynamic evidence, or dynamic join-report artifacts.

## Command Shape

Run the forked executor with an application classpath that contains the target application plus verifier executor classes:

```bash
java -cp <target-app-classpath>:<verifiers-classes-or-jar> \
  pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorCli \
  --spring-application-class <target.SpringApplication> \
  --spring-profiles local,sagas \
  --catalog-path <run-dir>/scenario-catalog-enriched.jsonl \
  --output-path <run-dir>/scenario-execution-report.json \
  --scenario-id <scenario-plan-id>
```

## Docker Compose Runner

The root compose file includes a one-shot `scenario-executor` service that consumes an existing catalog mounted through `./verifiers/target:/reports`. `CATALOG_PATH` is required and must point to a catalog path inside the container. `SCENARIO_ID` is optional; if unset, the executor auto-selects the first supported materializable candidate.

Pinned Quizzes smoke example:

```bash
CATALOG_PATH=/reports/structured-input-recipes-quizzes-smoke/quizzes-20260520-175058-455/scenario-catalog.jsonl \
SCENARIO_ID=2f0c64a371fcd65b5a38f294ccbda93a42df060c3d1e5b7dcedf43568abcf661 \
docker compose run --rm scenario-executor
```

Configurable target application example:

```bash
APPLICATION_BASE_DIR=quizzes \
SPRING_APPLICATION_CLASS=pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator \
MAVEN_PROFILE=test-sagas \
SPRING_PROFILES=test,sagas,local \
CATALOG_PATH=/reports/some-run/scenario-catalog.jsonl \
OUTPUT_PATH=/reports/scenario-executor/custom-report.json \
docker compose run --rm scenario-executor
```

The service reinstalls the simulator into the container Maven cache, builds verifier classes, prepares the target application with `test-compile`, builds the runtime classpath, disables the verifier Spring application inside the target context with `--verifiers.application.enabled=false`, and uses `--server.port=0` by default to avoid port collisions.

`--scenario-id` is a scenario plan id. If omitted, the executor auto-selects the first supported single-saga candidate using enriched join status priority and original JSONL line order.

## Catalog Loading

When a run directory is used by the executor API, `scenario-catalog-enriched.jsonl` is preferred when present. If it is absent, `scenario-catalog.jsonl` is used. Enriched records are wrappers: execution uses the embedded static `ScenarioPlan` as the contract, while join status is only a deterministic auto-selection hint.

## Runtime-Owned Overrides

Only these runtime-owned arguments may be supplied by the executor:

- `SagaUnitOfWorkService`, resolved from the target Spring context.
- `CommandGateway`, resolved from the target Spring context.
- `SagaUnitOfWork`, created by the executor for the selected saga instance.

These runtime-owned arguments are part of the executor materialization semantics. Their blocked recipe internals do not make the input unmaterializable because the executor supplies them directly.

Other unresolved values, source-provided placeholders without values, unsupported call results, and unsupported transforms remain blockers. Static `inputRecipe.executorReady=false` does not automatically mean the ScenarioExecutor cannot run the input; it may only mean the raw recipe tree is not fully replayable without executor runtime overrides.

## Accounting Alignment

`scenario-space-accounting.json` distinguishes static recipe readiness from ScenarioExecutor materializability. Static recipe readiness reports whether `inputRecipe.executorReady` is true without executor-specific overrides. Executor materializability reports whether the current ScenarioExecutor can materialize the accepted input using supported recipe semantics plus runtime-owned argument resolution.

## Report And Trace

The execution report includes schema version, terminal status, catalog path/kind, selection mode and reason, scenario plan id, saga instance id, saga FQN, input variant id, step outcomes, skipped-candidate reason counts, and blockers.

Terminal statuses distinguish dry-run, success, selection failure, unsupported scenario, materialization failure, startup failure, and step-execution failure. Console trace prints the selected scenario and each attempted scheduled step.

Skipped-candidate reason counts summarize unsupported auto-select candidates, such as unsupported scenario shapes or invalid step ids. Explicit scenario mode reports blockers for the requested scenario instead of skipping it.

Executor output is not dynamic-enrichment evidence and is not joined back into enriched catalogs in this POC.

## Quizzes Smoke Evidence

Verified on 2026-05-26 with a real verifier-generated Quizzes catalog through the forked runtime path:

- Catalog artifact: `verifiers/target/structured-input-recipes-quizzes-smoke/quizzes-20260520-175058-455/scenario-catalog.jsonl`.
- Scenario plan id: `2f0c64a371fcd65b5a38f294ccbda93a42df060c3d1e5b7dcedf43568abcf661`.
- Saga FQN: `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.GetCourseExecutionsFunctionalitySagas`.
- Step trace: `getCourseExecutionsStep` completed.
- Terminal status: `SUCCESS`.
- Execution report path: `/tmp/opencode/quizzes-execution-report-get-course-executions.json`.

## Limitations

Quizzes source/test changes, fixture generation, multi-saga execution, fault injection, behavior CSV generation, impact scoring, GA search, and bandit prioritization are out of scope for this POC.
