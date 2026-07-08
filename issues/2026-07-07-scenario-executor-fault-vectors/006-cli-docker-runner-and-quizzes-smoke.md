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
