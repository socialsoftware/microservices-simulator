# 006 - CLI/Docker Runner and Quizzes Smoke

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `001-simulator-in-memory-fault-provider.md`, `002-executor-vector-validation-and-report-v2.md`, `003-no-fault-lifecycle-closure.md`, `004-realized-fault-compensation-and-masking.md`, `005-mismatch-unexpected-and-compensation-failures.md`  
ACs covered: `AC-1, AC-2, AC-11, AC-20, AC-22`  
Risk: `medium`

## Purpose

Expose the fault-vector contract through the supported forked CLI/Docker runner and prove the feature works against Quizzes in the realistic one-shot `test,sagas,local` path.

## Scope

- Pass `--fault-vector` through `ScenarioExecutorOrchestrator` when configured.
- Add `FAULT_VECTOR` support to `verifiers/scripts/run-scenario-executor.sh` and the root `docker-compose.yml` `scenario-executor` service.
- Preserve `SCENARIO_ID` optionality only for default-vector/no-explicit-vector smoke runs.
- Ensure explicit `FAULT_VECTOR` without `SCENARIO_ID` fails through the same exact executor status/exit-code contract as the CLI.
- Run/record a Quizzes Docker smoke for default/all-zero vector success under `test,sagas,local`.
- Run/record a Quizzes Docker smoke for one explicit single-bit vector that realizes a fault and reports `FAULT_COMPENSATED` under `test,sagas,local`.
- Store/report v2 execution artifacts under `verifiers/target/` without mutating the input catalog.

## Out of Scope

- Generating or searching vectors.
- Adding Quizzes-specific executor shortcuts.
- Persistent database reset orchestration beyond the one-shot H2/test-profile process described by the spec.
- Docs rename/update; S7 handles docs after smoke behavior is known.

## Repo Anchors

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestrator.java` — forked command construction.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestratorSpec.groovy` — command assertion tests.
- `verifiers/scripts/run-scenario-executor.sh` — Docker entrypoint and environment variable handling.
- `docker-compose.yml` — `scenario-executor` service env block and mounted reports.
- `applications/quizzes/pom.xml` — `test-sagas` Maven profile and `test,sagas,local` runtime profile assumptions.
- Existing or freshly generated Quizzes scenario catalog under `verifiers/target/` — smoke input artifact.

## Implementation Shape

- Extend orchestrator config with an optional fault vector and append `--fault-vector <value>` only when present.
- In the shell script, read `FAULT_VECTOR` and append the CLI argument when non-empty. Do not generate CSV files.
- In compose, expose `FAULT_VECTOR: ${FAULT_VECTOR:-}`.
- Keep script logging explicit: application, catalog path, output path, scenario id mode, and vector source mode without printing misleading generated-vector language.
- For smoke, use a catalog with a materializable single-saga plan. The default smoke may auto-select or use a pinned scenario id; the explicit smoke must pin both `SCENARIO_ID` and `FAULT_VECTOR`.
- Record the chosen plan id, vector, command, exit code, report path, terminal status, lifecycle outcome, and key slot mapping in implementation evidence.

## TDD / Test Shape

- First behavior to test: orchestrator includes `--fault-vector 010` when config has a vector and includes no vector argument when absent.
- Expected red failure: current orchestrator config and shell script know only `SCENARIO_ID` and cannot pass vector input.
- Additional coverage:
  - shell/compose env variable propagation inspection;
  - explicit vector without scenario id exits non-zero and writes/prints invalid-vector evidence if report path is available;
  - default Quizzes smoke exits zero with `SUCCESS`;
  - explicit single-bit Quizzes smoke exits zero with `FAULT_COMPENSATED`.

## Just-in-Time Preflight Required

Before source edits, the executor must re-check:

- Current `ScenarioExecutorOrchestrator.Config` constructor usages.
- Current Docker service env names and shell script strict-mode behavior.
- Availability of a current Quizzes catalog under `verifiers/target/`; generate one if necessary using the existing verifier Docker workflow.
- Current materializable scenario ids and fault-space lengths before choosing the explicit vector.

## Verification

- `cd verifiers && mvn -Dtest=ScenarioExecutorOrchestratorSpec test` — command construction passes.
- Default smoke: `CATALOG_PATH=/reports/<run>/scenario-catalog.jsonl docker compose run --rm scenario-executor` — exits zero and report has `terminalStatus = SUCCESS`.
- Explicit smoke: `CATALOG_PATH=/reports/<run>/scenario-catalog.jsonl SCENARIO_ID=<plan-id> FAULT_VECTOR=<single-bit-vector> docker compose run --rm scenario-executor` — exits zero and report has `terminalStatus = FAULT_COMPENSATED`.

## Evidence to Record

- files changed
- commands run and outputs
- selected Quizzes catalog path, scenario id, and vector
- report paths and excerpts for both smokes
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- Do not make explicit vectors auto-select a scenario; the spec requires explicit scenario id for explicit vector runs.
- Do not make the shell script synthesize vectors or CSV files.
- Quizzes smoke may need a freshly generated current catalog if old scenario ids no longer materialize.
- Docker runs are heavier than unit tests; record if environment/time prevents running them, but do not claim AC-22 without successful smoke evidence.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Added optional fault-vector propagation to the forked `ScenarioExecutorOrchestrator` command.
- Added `FAULT_VECTOR` support to the Docker runner script and compose service.
- Kept explicit vectors tied to explicit `SCENARIO_ID`: the runner passes `--fault-vector` only when `FAULT_VECTOR` is non-empty, and the executor returns `INVALID_FAULT_VECTOR`/exit code 1 when no scenario id is supplied.
- Fixed runner issues found during the required Docker smoke: copy the mounted simulator source to writable `/tmp` before Maven install, allow compose parsing without a global `CATALOG_PATH`, and unset exported `SPRING_PROFILES` before launching Spring Boot while still passing profiles through the CLI option.
- Generated a current Quizzes catalog and ran pinned default/explicit one-shot smokes against materializable plan `910f72907e0d901bc5d35e0ecea03ec920b7ffb63929bbba1bfdba4fe531e195`.

### Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestrator.java` — added optional `faultVector` config field and `--fault-vector` command argument.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestratorSpec.groovy` — asserted forked command construction includes `--fault-vector 010`.
- `verifiers/scripts/run-scenario-executor.sh` — reads `FAULT_VECTOR`, logs vector source mode, passes the CLI argument when non-empty, avoids Spring Boot seeing invalid exported `SPRING_PROFILES`, and builds simulator dependency from a writable temp copy.
- `docker-compose.yml` — exposes `FAULT_VECTOR` to `scenario-executor` and makes `CATALOG_PATH` validation happen in the runner script instead of global compose interpolation.
- `issues/2026-07-07-scenario-executor-fault-vectors/006-cli-docker-runner-and-quizzes-smoke.md` — completion evidence only.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=ScenarioExecutorOrchestratorSpec test` before implementation after the test edit | FAIL | Expected TDD red: `Could not find matching constructor ... ScenarioExecutorOrchestrator$Config(..., String, String, String)` because the config had no fault-vector field yet. |
| `bash -n verifiers/scripts/run-scenario-executor.sh` | PASS | No syntax output. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorOrchestratorSpec test` | PASS | `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`; build success. |
| `docker compose run --rm fault-analysis-scenario-gen` | FAIL then superseded | First attempt was blocked by compose requiring `CATALOG_PATH` globally; after fixing that, default memory hit `java.lang.OutOfMemoryError: Java heap space` while generating Quizzes. |
| `MEDIUM_MEM_LIMIT=3g MEDIUM_MEM_RESERVATION=2g JAVA_TOOL_OPTIONS='-Xmx2500m' docker compose run --rm fault-analysis-scenario-gen` | PASS | Generated current Quizzes catalog at `verifiers/target/quizzes-20260708-163552-193/scenario-catalog.jsonl` with 86 scenarios. |
| `CATALOG_PATH=/reports/quizzes-20260708-163552-193/scenario-catalog.jsonl SCENARIO_ID=910f72907e0d901bc5d35e0ecea03ec920b7ffb63929bbba1bfdba4fe531e195 OUTPUT_PATH=/reports/scenario-executor/s6-default-report.json docker compose run --rm scenario-executor` | PASS | Exit code 0. Report `verifiers/target/scenario-executor/s6-default-report.json`: `terminalStatus=SUCCESS`, `lifecycleOutcome=COMMITTED`, `assignedVector=0`, `vectorSource=DEFAULT_VECTOR`, provider `IN_MEMORY_FAULT_VECTOR`. |
| `CATALOG_PATH=/reports/quizzes-20260708-163552-193/scenario-catalog.jsonl SCENARIO_ID=910f72907e0d901bc5d35e0ecea03ec920b7ffb63929bbba1bfdba4fe531e195 FAULT_VECTOR=1 OUTPUT_PATH=/reports/scenario-executor/s6-explicit-report.json docker compose run --rm scenario-executor` | PASS | Exit code 0. Report `verifiers/target/scenario-executor/s6-explicit-report.json`: `terminalStatus=FAULT_COMPENSATED`, `lifecycleOutcome=COMPENSATED`, `assignedVector=1`, `vectorSource=EXPLICIT_VECTOR`, slot 0 `REALIZED`, step 0 `INJECTED_FAULT`. |
| `CATALOG_PATH=/reports/quizzes-20260708-163552-193/scenario-catalog.jsonl FAULT_VECTOR=1 OUTPUT_PATH=/reports/scenario-executor/s6-invalid-report.json docker compose run --rm scenario-executor` | PASS | Expected non-zero contract observed via wrapper: `EXIT_CODE=1`. Report `verifiers/target/scenario-executor/s6-invalid-report.json`: `terminalStatus=INVALID_FAULT_VECTOR`, `lifecycleOutcome=NOT_STARTED`, blocker `MISSING_EXPLICIT_SCENARIO_ID`. |

### Acceptance Criteria Evidence

- AC-1: CLI already parsed `--fault-vector`; this slice wires `ScenarioExecutorOrchestrator`, `FAULT_VECTOR`, and compose runner propagation. Explicit vector without scenario id produced `INVALID_FAULT_VECTOR` and exit code 1.
- AC-2: Default smoke used catalog default vector `0`, reported `vectorSource=DEFAULT_VECTOR`, provider `IN_MEMORY_FAULT_VECTOR`, and completed with `SUCCESS`/`COMMITTED`.
- AC-11: Explicit single-bit vector `1` on the selected Quizzes plan realized slot 0, step status `INJECTED_FAULT`, lifecycle `COMPENSATED`, terminal `FAULT_COMPENSATED`.
- AC-20: Docker/CLI exit codes matched the contract for this slice: `SUCCESS` exit 0, `FAULT_COMPENSATED` exit 0, and missing explicit scenario id / `INVALID_FAULT_VECTOR` exit 1.
- AC-22: Quizzes Docker smoke covered default/all-zero success and explicit single-bit compensated fault under `test,sagas,local` using catalog `verifiers/target/quizzes-20260708-163552-193/scenario-catalog.jsonl` and plan `910f72907e0d901bc5d35e0ecea03ec920b7ffb63929bbba1bfdba4fe531e195`.

Selected Quizzes slot mapping:

- plan id: `910f72907e0d901bc5d35e0ecea03ec920b7ffb63929bbba1bfdba4fe531e195`
- saga: `GetCourseExecutionsFunctionalitySagas`
- fault-space length: `1`
- default vector: `0`
- explicit vector: `1`
- slot 0 scheduled step id: `ebeca9bce8f36249b8631ec2ab1024ade807c4845f3379d5690aafa89d842127`
- runtime step: `getCourseExecutionsStep`

### Browser / Manual Evidence

- Not required.

### TDD Notes

- Added the orchestrator command-construction expectation first and observed the expected constructor failure before implementing the `faultVector` config field and command argument.
- Shell and compose behavior were validated through Docker smokes rather than a separate shell unit-test harness, matching this slice's runner focus.

### Deviations From Plan

- The default smoke used an explicitly pinned materializable scenario id instead of auto-select because auto-select chose an earlier `CreateUserFunctionalitySagas` candidate that failed at runtime with `UNEXPECTED_EXECUTION_FAILURE` from a null DTO name. The slice explicitly allowed pinned ids for default smoke.
- The Quizzes catalog had to be generated with increased container memory (`MEDIUM_MEM_LIMIT=3g`, `JAVA_TOOL_OPTIONS=-Xmx2500m`) after the default compose limit hit OOM.
- Runner fixes for writable simulator build input and `SPRING_PROFILES` env leakage were required to make the supported Docker path executable; both are inside the runner/Docker scope.

### Blockers / Follow-Ups

- None for this slice.
