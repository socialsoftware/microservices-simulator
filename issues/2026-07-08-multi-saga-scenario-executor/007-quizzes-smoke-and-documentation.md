# 007 - Quizzes Smoke and Documentation

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `001-v3-participant-report-and-single-saga-migration.md`, `002-explicit-multi-saga-selection-and-dry-run.md`, `003-multi-participant-materialization-and-startup-gates.md`, `004-default-vector-interleaving-and-survivor-closure.md`, `005-assigned-fault-compensate-and-continue.md`, `006-runtime-failures-compensation-failures-and-exit-codes.md`  
ACs covered: `AC-1, AC-2, AC-35, AC-39, AC-41, AC-42, AC-43, AC-44`  
Risk: `medium`

## Purpose

Validate the completed feature against a real Quizzes multi-saga catalog through the forked/Docker ScenarioExecutor path, record the smoke evidence, and update verifier documentation so current-state language matches the implemented support and limitations.

## Scope

- Use the existing planning-audit catalog if still valid:
  - `verifiers/target/multi-saga-executor-planning-audit/quizzes-20260709-004627-310/scenario-catalog.jsonl`
  - candidate plan id `0945caa9ac2fe06a268e6df6aa992fcf69e253116264684d577bdbbb955c2e25`
  - participants `CreateCourseExecutionFunctionalitySagas` and `GetCourseExecutionsFunctionalitySagas`
  - default vector `00000`
- If the audited catalog/candidate no longer runs, regenerate or consume a bounded multi-saga-only `WRITE_PLANS` Quizzes catalog using the command in `quizzes-materializability-audit.md`, then choose a currently materializable candidate and record why it replaced the audited plan.
- Run at least one Docker/forked ScenarioExecutor smoke for an explicit multi-saga scenario id.
- Record smoke evidence: catalog path, scenario plan id, participants, schedule, assigned vector, terminal status, participant lifecycle outcomes, report path, exit code, and before/after byte or hash evidence proving the input catalog was not mutated by non-dry-run execution.
- Update documentation after implementation:
  - `docs/verifiers-impl/reference/scenario-executor.md` describes supported multi-saga deterministic interleaving replay, v3 reports, compensate-and-continue, explicit selection, and limitations;
  - `docs/verifiers-impl/current-state.md` moves multi-saga deterministic replay from not-implemented to implemented/partial as appropriate;
  - `docs/verifiers-impl/evidence.md` or an issue-local smoke artifact records the Quizzes run evidence;
  - `docs/verifiers-impl/glossary.md` is updated only if implementation changed terminology beyond the spec.
- Confirm Docker/CLI exit-code contract for the smoke status.

## Out of Scope

- Adding Quizzes-specific executor shortcuts.
- Changing catalog generation strategy or static scenario semantics.
- Adding fixture seeding/reset beyond what the existing Docker runner/application profile already does.
- Impact scoring, search, prioritization, or distributed parity claims.

## Repo Anchors

- `verifiers/scripts/run-scenario-executor.sh` — Docker runner script for catalog path, scenario id, optional fault vector, output path, app base, Spring class, Maven profile, and Spring profiles.
- `docker-compose.yml` service `scenario-executor` — one-shot container used for user-visible smoke behavior.
- `issues/2026-07-08-multi-saga-scenario-executor/quizzes-materializability-audit.md` — audited candidate and regeneration command.
- `docs/verifiers-impl/reference/scenario-executor.md` — executor reference page to revise.
- `docs/verifiers-impl/current-state.md` — canonical current-truth status page.
- `docs/verifiers-impl/evidence.md` — existing evidence record for verifier runs.
- `CONTEXT-MAP.md` and `docs/verifiers-impl/glossary.md` — glossary routing; do not create root `CONTEXT.md`.

## Implementation Shape

- Prefer using the audited catalog first to avoid scope creep. Regenerate only if the artifact is missing or the selected plan is no longer materializable/executable.
- Use explicit `SCENARIO_ID`; do not rely on auto-selection for the smoke.
- Store output reports under `verifiers/target/scenario-executor/` or another clear per-run target path mounted as `/reports/...` inside Docker.
- Capture the audited input catalog's checksum or full byte contents before and after the Docker/forked execution and record the comparison result.
- If an explicit non-zero vector is used for an additional smoke, select a binary vector aligned to the plan fault space and record the rationale. The required smoke can be default-vector if it proves materializable interleaving replay.
- Documentation should state deterministic sequential interleaving replay, not true concurrency or distributed parity.

## TDD / Test Shape

- First behavior to validate manually: Docker smoke exits zero and writes a v3 report for the explicit Quizzes multi-saga plan.
- Expected red failure: before implementation, the executor rejects the plan as unsupported multi-saga shape.
- Additional coverage:
  - report schema is v3;
  - participant count and identities match the selected plan;
  - schedule/step outcomes are present under participants;
  - terminal status is one of the v3 zero-exit statuses for the successful smoke;
  - input catalog checksum or byte comparison is unchanged after non-dry-run execution;
  - docs no longer claim generic multi-saga execution is entirely unimplemented, but still list limitations and non-goals.

## Just-in-Time Preflight Required

Before source/docs edits, the executor must re-check:

- Whether the audited catalog path still exists and contains the target plan id.
- Whether Docker Compose service defaults still match Quizzes saga/local runtime.
- Current `docs/verifiers-impl/reference/scenario-executor.md` and `docs/verifiers-impl/current-state.md` language to avoid stale or overbroad claims.
- Whether `./scripts/verifier-docs build` exists and is usable in this checkout.

## Verification

- `CATALOG_PATH=/reports/multi-saga-executor-planning-audit/quizzes-20260709-004627-310/scenario-catalog.jsonl SCENARIO_ID=0945caa9ac2fe06a268e6df6aa992fcf69e253116264684d577bdbbb955c2e25 OUTPUT_PATH=/reports/scenario-executor/multi-saga-default-report.json docker compose run --rm scenario-executor` — expected zero exit and v3 report for the audited candidate, if still valid.
- `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test` — re-run focused regression after any runner/doc-adjacent code changes.
- `./scripts/verifier-docs build` — run if available; otherwise record targeted docs inspection with `rg`.

## Evidence to Record

- files changed
- commands run and outputs
- Docker exit code
- catalog path and scenario plan id
- participant identities and schedule
- assigned vector and vector source
- terminal status and participant lifecycle outcomes
- report path and relevant JSON excerpt
- before/after catalog checksum or byte comparison evidence
- docs build or inspection evidence
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- The first materializable Quizzes candidate may not be the richest impact example. Do not expand scope into materializer or fixture readiness work if the candidate proves deterministic replay.
- Persistent database state/profile failures should be reported as smoke environment blockers, not hidden with Quizzes-specific executor shortcuts.
- Documentation must stay precise: implemented support is explicit, materializable, saga/local, deterministic sequential interleaving replay with v3 reports, not generic runtime parity.
