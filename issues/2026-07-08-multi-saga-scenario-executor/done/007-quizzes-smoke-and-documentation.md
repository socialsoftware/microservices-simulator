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

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Validated the audited Quizzes multi-saga catalog and explicit plan id still exist.
- Ran the Docker/forked ScenarioExecutor smoke for the explicit multi-saga plan and recorded v3 report evidence, exit code, participant outcomes, schedule, assigned vector, and catalog checksum non-mutation.
- Updated verifier docs/current-state/evidence to describe supported explicit multi-saga deterministic sequential interleaving replay, v3 participant reports, compensate-and-continue outcomes, exit-code behavior, and limitations.
- Addressed review attempt 01 by aligning remaining live verifier docs that still described ScenarioExecutor as single-saga/v2-only or multi-saga execution as unimplemented.
- Addressed review attempt 02 by fixing the malformed claim-map table row in `docs/verifiers-impl/thesis-claims-evidence-map.md`.

### Files Changed

- `docs/verifiers-impl/reference/scenario-executor.md` — revised ScenarioExecutor reference from single-saga/v2-only language to current v3 single-saga plus explicit multi-saga deterministic replay support and limitations.
- `docs/verifiers-impl/current-state.md` — moved explicit multi-saga deterministic replay into current supported scope while preserving non-goals for auto-selection, distributed parity, TCC, scoring, and search.
- `docs/verifiers-impl/evidence.md` — added the 2026-07-09 Quizzes multi-saga Docker smoke evidence and aligned older single-saga smoke wording with the current bounded saga/local executor path.
- `docs/verifiers-impl/glossary.md` — updated execution terminology from planned/proposed/v2 wording to implemented bounded v3 multi-saga replay terminology.
- `docs/verifiers-impl/roadmap.md` — aligned Stage 2 and roadmap matrix language with current explicit multi-saga deterministic replay support and remaining gaps.
- `docs/verifiers-impl/advisor-brief.md` — aligned advisor-facing summary, evidence, limitations, and next-work language with current bounded executor support.
- `docs/verifiers-impl/verifier-pipeline-plain-explanation.md` — updated meeting/thesis explanation for the current bounded saga/local executor path.
- `docs/verifiers-impl/thesis-claims-evidence-map.md` — updated claim-safety language to distinguish supported bounded executor behavior from generic/distributed runtime coverage; fixed the review-identified malformed claim-map table row by splitting the static-extraction and deterministic-catalog claims back into separate rows.
- `issues/2026-07-08-multi-saga-scenario-executor/007-quizzes-smoke-and-documentation.md` — recorded this completion evidence.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `test -f verifiers/target/multi-saga-executor-planning-audit/quizzes-20260709-004627-310/scenario-catalog.jsonl && grep -c 0945caa9ac2fe06a268e6df6aa992fcf69e253116264684d577bdbbb955c2e25 ...` | PASS | Catalog exists and contains the audited plan id exactly once. |
| `CATALOG_PATH=/reports/multi-saga-executor-planning-audit/quizzes-20260709-004627-310/scenario-catalog.jsonl SCENARIO_ID=0945caa9ac2fe06a268e6df6aa992fcf69e253116264684d577bdbbb955c2e25 OUTPUT_PATH=/reports/scenario-executor/multi-saga-default-report.json docker compose run --rm scenario-executor` | PASS | `docker_exit=0`; report written to `verifiers/target/scenario-executor/multi-saga-default-report.json`; CLI reported `status=PARTIAL_COMPENSATED`. |
| `sha256sum verifiers/target/multi-saga-executor-planning-audit/quizzes-20260709-004627-310/scenario-catalog.jsonl` before and after Docker run | PASS | Both checksums were `631538f64789c80a2b5b01291f0dd1a08b48966dfbaea4c5b805d7ecd48cafcd`, proving the input catalog was not mutated. |
| `python3` JSON inspection of `verifiers/target/scenario-executor/multi-saga-default-report.json` | PASS | `schemaVersion=microservices-simulator.scenario-execution-report.v3`, `scenarioKind=MULTI_SAGA`, `assignedVector=00000`, `vectorSource=DEFAULT_VECTOR`, `providerMode=IN_MEMORY_FAULT_VECTOR`, `terminalStatus=PARTIAL_COMPENSATED`. Participants: `CreateCourseExecutionFunctionalitySagas` lifecycle `COMPENSATED`; `GetCourseExecutionsFunctionalitySagas` lifecycle `COMMITTED`. Schedule: order 0 `getCourseExecutionsStep` completed, order 1 `getCourseStep` completed, order 2 `createCourseStep` failed, orders 3-4 skipped by saga failure. |
| `./scripts/verifier-docs build` | PASS | MkDocs completed successfully and built to `target/verifier-docs-site`; it emitted only existing informational nav warnings. Rerun after review-fix docs alignment also passed. |
| `rg -n "current implementation supports a narrow single-saga|In the proposed v3|Planned multi-saga|Proposed multi-saga|standalone v2 execution report|materializable single-saga saga/local path|supported single-saga executor path|Multi-saga runtime execution is not implemented|multi-saga runtime execution is not implemented|does not cover arbitrary catalog replay, multi-saga schedules|multi-saga schedules, TCC execution|supported but narrow single-saga ScenarioExecutor path|supported single-saga ScenarioExecutor path|current supported single-saga" docs/verifiers-impl -g '!archive/**' -g '!decisions/**' -g '!**/archive/**' -g '!**/decisions/**'` | PASS | No stale live-doc matches remained after updating glossary, roadmap, advisor brief, plain explanation, evidence, and thesis claims map. |
| `rg -n "current implementation supports a narrow single-saga|In the proposed v3|Planned multi-saga|Proposed multi-saga|standalone v2 execution report|materializable single-saga saga/local path|supported single-saga executor path|Multi-saga runtime execution is not implemented|multi-saga runtime execution is not implemented|does not cover arbitrary catalog replay, multi-saga schedules|multi-saga schedules, TCC execution|supported but narrow single-saga ScenarioExecutor path|supported single-saga ScenarioExecutor path|current supported single-saga|FAULT_COMPENSATED|generic multi-saga execution" docs/verifiers-impl -g '!archive/**' -g '!decisions/**' -g '!**/archive/**' -g '!**/decisions/**'` | PASS | Review-attempt-02 stale-claim pattern returned no matches (`stale_claim_search_ok`). |
| Python claim-map table row check for `docs/verifiers-impl/thesis-claims-evidence-map.md` | PASS | `claim_map_table_rows_ok`; all claim-map body rows have the expected 5 pipe characters. |
| `git diff --check -- <changed docs and slice file>` | PASS | No whitespace errors in slice-owned tracked docs/evidence changes. Rerun after the review-attempt-02 table fix also passed for `thesis-claims-evidence-map.md` and this slice file. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test` | NOT RUN | No runner or source code changed in this slice; the slice requires this only after runner/doc-adjacent code changes. |

### Acceptance Criteria Evidence

- AC-1: Non-dry-run Docker smoke consumed the static catalog and before/after sha256 remained identical.
- AC-2: Smoke used explicit `SCENARIO_ID=0945caa9ac2fe06a268e6df6aa992fcf69e253116264684d577bdbbb955c2e25` and selected that plan.
- AC-35: Smoke report uses `schemaVersion=microservices-simulator.scenario-execution-report.v3`; docs now describe v3 as canonical.
- AC-39: Documentation keeps single-saga execution as the one-participant v3 case and preserves existing single-saga support language.
- AC-41: Quizzes validation used the bounded planning-audit multi-saga catalog through the Docker/forked `scenario-executor` path.
- AC-42: Evidence records catalog path, plan id, participants, schedule, assigned vector, terminal status, lifecycle outcomes, report path, and checksum non-mutation.
- AC-43: `scenario-executor.md`, `current-state.md`, `evidence.md`, `glossary.md`, `roadmap.md`, `advisor-brief.md`, `verifier-pipeline-plain-explanation.md`, and `thesis-claims-evidence-map.md` now describe supported explicit multi-saga deterministic interleaving replay and limitations without stale single-saga/v2-only current-status claims.
- AC-44: Docker exited `0` for `PARTIAL_COMPENSATED`, matching the v3 zero-exit contract.

### Browser / Manual Evidence

- Not required. Manual artifact inspection was performed via JSON parsing and docs build output.

### TDD Notes

- TDD was not practical for this documentation/smoke slice. The required behavior had already been implemented by prior slices; this slice validates it through Docker and updates documentation/evidence.

### Deviations From Plan

- The audited default-vector run completed with `PARTIAL_COMPENSATED` rather than all-success because the generated `CreateCourseExecutionFunctionalitySagas` input reached a scheduled-step domain/runtime failure (`Name is null`). This is still an accepted zero-exit v3 terminal status, proves compensate-and-continue deterministic replay, and stayed within the slice contract.
- The first attempted smoke used `tee` under `verifiers/target/scenario-executor/`, which is root-owned from Docker and caused permission-denied messages for auxiliary checksum/log files. The smoke itself wrote the Docker report successfully. The verification was rerun with shell output outside the root-owned directory and produced clean `docker_exit=0` checksum evidence.

### Review Attempt 01 Fixes

- Fixed the blocking documentation drift called out by `issues/2026-07-08-multi-saga-scenario-executor/review/007-quizzes-smoke-and-documentation-review.md`.
- Updated remaining live verifier docs that still implied the current executor was single-saga/v2-only or that multi-saga execution was entirely unimplemented.
- Reran docs build, targeted stale-claim search, and diff whitespace check.

### Review Attempt 02 Fixes

- Fixed the blocking malformed Markdown table row in `docs/verifiers-impl/thesis-claims-evidence-map.md` by splitting the collapsed first two claim-map rows.
- Reran docs build, stale-claim search with the review-attempt-02 pattern, claim-map table row check, and diff whitespace check.

### Blockers / Follow-Ups

- None.
