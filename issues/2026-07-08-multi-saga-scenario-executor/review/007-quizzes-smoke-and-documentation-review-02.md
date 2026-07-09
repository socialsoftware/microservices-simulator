# Slice Review: 007 - Quizzes Smoke and Documentation

## Review Attempt

Attempt: `02`

Latest copy updated: `yes`

## Verdict

`FAIL`

Moved to done: `no`

## Sources Reviewed

- Slice: `issues/2026-07-08-multi-saga-scenario-executor/007-quizzes-smoke-and-documentation.md`
- Spec: `issues/2026-07-08-multi-saga-scenario-executor/spec.md`
- Implementation plan: `issues/2026-07-08-multi-saga-scenario-executor/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`, `docs/verifiers-impl/evidence.md`, `docs/verifiers-impl/roadmap.md`, `docs/verifiers-impl/advisor-brief.md`, `docs/verifiers-impl/verifier-pipeline-plain-explanation.md`, `docs/verifiers-impl/thesis-claims-evidence-map.md`
- ADRs: `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md`, `docs/verifiers-impl/decisions/2026-04-28-hybrid-static-dynamic-key-binding.md`, `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`, `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`
- Completion evidence: `issues/2026-07-08-multi-saga-scenario-executor/007-quizzes-smoke-and-documentation.md#completion-evidence`
- Changed files reviewed: `docs/verifiers-impl/reference/scenario-executor.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/evidence.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/roadmap.md`, `docs/verifiers-impl/advisor-brief.md`, `docs/verifiers-impl/verifier-pipeline-plain-explanation.md`, `docs/verifiers-impl/thesis-claims-evidence-map.md`, `issues/2026-07-08-multi-saga-scenario-executor/007-quizzes-smoke-and-documentation.md`; artifacts `verifiers/target/multi-saga-executor-planning-audit/quizzes-20260709-004627-310/scenario-catalog.jsonl`, `verifiers/target/scenario-executor/multi-saga-default-report.json`, `verifiers/target/scenario-executor/multi-saga-default-report-review02.json`
- Prior review reports: `issues/2026-07-08-multi-saga-scenario-executor/review/007-quizzes-smoke-and-documentation-review-01.md`, `issues/2026-07-08-multi-saga-scenario-executor/review/007-quizzes-smoke-and-documentation-review.md`
- Commands run by reviewer:
  - `git status --short && ...` dependency/done-collision check
  - `git diff -- <changed docs and slice file>`
  - `rg -n "current implementation supports a narrow single-saga|In the proposed v3|Planned multi-saga|Proposed multi-saga|standalone v2 execution report|materializable single-saga saga/local path|supported single-saga executor path|Multi-saga runtime execution is not implemented|multi-saga runtime execution is not implemented|does not cover arbitrary catalog replay, multi-saga schedules|multi-saga schedules, TCC execution|supported but narrow single-saga ScenarioExecutor path|supported single-saga ScenarioExecutor path|current supported single-saga|FAULT_COMPENSATED|generic multi-saga execution" docs/verifiers-impl -g '!archive/**' -g '!decisions/**' -g '!**/archive/**' -g '!**/decisions/**'`
  - `rg -n "v2|single-saga|multi-saga execution|multi-saga runtime|not implemented|Planned|Proposed" docs/verifiers-impl -g '!archive/**' -g '!decisions/**' -g '!**/archive/**' -g '!**/decisions/**'`
  - catalog/report JSON inspection with `python3`
  - `CATALOG_PATH=/reports/multi-saga-executor-planning-audit/quizzes-20260709-004627-310/scenario-catalog.jsonl SCENARIO_ID=0945caa9ac2fe06a268e6df6aa992fcf69e253116264684d577bdbbb955c2e25 OUTPUT_PATH=/reports/scenario-executor/multi-saga-default-report-review02.json docker compose run --rm scenario-executor`
  - JSON inspection of `verifiers/target/scenario-executor/multi-saga-default-report-review02.json`
  - `./scripts/verifier-docs build`
  - `git diff --check -- docs/verifiers-impl/reference/scenario-executor.md docs/verifiers-impl/current-state.md docs/verifiers-impl/evidence.md docs/verifiers-impl/glossary.md docs/verifiers-impl/roadmap.md docs/verifiers-impl/advisor-brief.md docs/verifiers-impl/verifier-pipeline-plain-explanation.md docs/verifiers-impl/thesis-claims-evidence-map.md issues/2026-07-08-multi-saga-scenario-executor/007-quizzes-smoke-and-documentation.md`
  - Markdown table row check for `docs/verifiers-impl/thesis-claims-evidence-map.md`

## Summary

The Quizzes ScenarioExecutor smoke and the stale-claim documentation alignment are now mostly correct. I reran the Docker/forked smoke against the audited multi-saga plan; it exited `0`, wrote a v3 report with `terminalStatus=PARTIAL_COMPENSATED`, recorded the expected participants/schedule, and left the input catalog checksum unchanged.

The slice still fails because one changed live documentation page is malformed. `docs/verifiers-impl/thesis-claims-evidence-map.md` accidentally merged the first two claim-map rows into one Markdown table row, producing a row with 10 pipe characters instead of the expected 5. This is a documentation regression in a file explicitly claimed as updated by the slice.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | fail | Smoke/evidence and stale-claim alignment are satisfied, but the changed `thesis-claims-evidence-map.md` table is malformed and needs a docs-only fix before the documentation slice is complete. |
| Slice out-of-scope respected | pass | No source/runner changes were introduced for this slice; Docker used the generic `scenario-executor` service with explicit `SCENARIO_ID`; docs preserve limits for auto-selection, distributed parity, TCC, true concurrency, scoring, and search. |
| Spec non-goals respected | pass | No catalog generation, static scenario semantics, fixture seeding/reset, impact scoring, search, prioritization, or Quizzes-specific executor shortcut was introduced. |
| Dependencies done | pass | `issues/2026-07-08-multi-saga-scenario-executor/done/` contains slices `001` through `006`; no `done/007...` collision exists. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-1 | pass | Reviewer rerun checksum before/after Docker smoke stayed `631538f64789c80a2b5b01291f0dd1a08b48966dfbaea4c5b805d7ecd48cafcd`; report was written separately under `verifiers/target/scenario-executor/multi-saga-default-report-review02.json`. | Static catalog was not mutated by non-dry-run execution. |
| AC-2 | pass | Docker smoke used explicit `SCENARIO_ID=0945caa9ac2fe06a268e6df6aa992fcf69e253116264684d577bdbbb955c2e25`; rerun report `scenarioPlanId` matches. | Explicit multi-saga selection works for the audited plan. |
| AC-35 | pass | Rerun report schema is `microservices-simulator.scenario-execution-report.v3`; reference docs describe v3 participant reports as canonical. | No v2/`FAULT_COMPENSATED` live-doc stale matches were found outside excluded archive/decision docs. |
| AC-39 | pass | `docs/verifiers-impl/reference/scenario-executor.md`, `current-state.md`, and related docs describe single-saga runs as the one-participant v3 case / supported single-saga auto-select path. | Earlier S1-S6 reviews covered runtime behavior. |
| AC-41 | pass | Reviewer reran Docker/forked `scenario-executor` against `verifiers/target/multi-saga-executor-planning-audit/quizzes-20260709-004627-310/scenario-catalog.jsonl`; Docker exit `0`. | Uses the bounded planning-audit Quizzes multi-saga catalog. |
| AC-42 | pass | Evidence records catalog path, plan id, participants, schedule, assigned vector, terminal status, lifecycle outcomes, report path, and checksum non-mutation; reviewer JSON inspection confirmed the same fields in `multi-saga-default-report-review02.json`. | Evidence is credible and freshly rerun by reviewer. |
| AC-43 | fail | Stale single-saga/v2-only claims were fixed, but `docs/verifiers-impl/thesis-claims-evidence-map.md` has a malformed claim map table: line 11 contains the first claim and the `The scenario catalog is deterministic...` claim in one row (`pipe_count=10`). | Required fix is to split the two claim rows back into separate Markdown table rows, then rerun docs build and a small table/stale-claim check. |
| AC-44 | pass | Reviewer rerun Docker smoke exited `0` for report `terminalStatus=PARTIAL_COMPENSATED`; reference docs list zero exit for `SUCCESS`, `COMPENSATED`, `PARTIAL_COMPENSATED`, and `DRY_RUN`. | Full non-zero matrix was covered by S6; S7 confirms the Quizzes smoke status. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| Catalog existence and selected plan id | pass | Catalog exists; audited plan id appears exactly once; catalog checksum is `631538f64789c80a2b5b01291f0dd1a08b48966dfbaea4c5b805d7ecd48cafcd`; catalog plan has `kind=MULTI_SAGA`, fault-space length `5`, default vector `00000`, expected participants, and schedule orders `0..4`. |
| Docker/forked Quizzes smoke | pass | Reviewer reran the smoke with `OUTPUT_PATH=/reports/scenario-executor/multi-saga-default-report-review02.json`; Docker exit was `0`; before/after catalog checksum was unchanged. |
| v3 report JSON inspection | pass | Rerun report has `schemaVersion=microservices-simulator.scenario-execution-report.v3`, `scenarioKind=MULTI_SAGA`, `assignedVector=00000`, `vectorSource=DEFAULT_VECTOR`, `providerMode=IN_MEMORY_FAULT_VECTOR`, `terminalStatus=PARTIAL_COMPENSATED`; participants are `CreateCourseExecutionFunctionalitySagas` lifecycle `COMPENSATED` and `GetCourseExecutionsFunctionalitySagas` lifecycle `COMMITTED`. |
| Schedule/lifecycle evidence | pass | Rerun report has schedule order `0` `getCourseExecutionsStep` `COMPLETED`, order `1` `getCourseStep` `COMPLETED`, order `2` `createCourseStep` `FAILED`, and skipped orders `3`/`4` with reason `SKIPPED_BY_SAGA_FAILURE`. |
| Catalog non-mutation evidence | pass | Reviewer before/after checksum stayed `631538f64789c80a2b5b01291f0dd1a08b48966dfbaea4c5b805d7ecd48cafcd`. |
| `./scripts/verifier-docs build` | pass | MkDocs built to `target/verifier-docs-site`; it emitted only existing informational nav warnings. This build does not catch the malformed Markdown table row. |
| Targeted docs stale-claim search | pass | No live-doc matches remained for the stale single-saga/v2-only or generic multi-saga-unimplemented claim patterns used in attempt 01, excluding archive/decision docs. |
| Markdown table row check | fail | `docs/verifiers-impl/thesis-claims-evidence-map.md` line 11 has `pipe_count=10` where claim-map rows should have 5 pipe characters. The first two rows were collapsed into one row. |
| `git diff --check -- <changed docs and slice file>` | pass | No whitespace errors in the slice-owned tracked docs/evidence changes. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test` | not rerun | Acceptable for this review because S7 made no runner/source changes; Docker smoke and docs checks are the primary S7 validation. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | fail | The docs alignment is otherwise scoped, but the accidental row merge in a changed doc is an avoidable documentation regression. |
| Existing patterns | pass | Evidence is recorded in existing verifier docs/evidence pages and the issue-local completion section; no root `CONTEXT.md` was created. |
| Test quality | n/a | No automated tests were added in S7; this is a smoke/docs slice. Reviewer reran Docker smoke, docs build, stale-claim search, and a table integrity check. |
| Regression risk | fail | The malformed claim-map table can hide or corrupt two thesis claim rows in rendered docs, which directly affects documentation credibility for this docs slice. |
| Security/data safety | pass | No credential, migration, destructive git, or persistent data operation was introduced; Docker used the existing one-shot service and wrote under ignored `verifiers/target/`. |
| Change hygiene | fail | `docs/verifiers-impl/thesis-claims-evidence-map.md` has a malformed Markdown table row introduced by this slice's docs edit. |

## Findings

| Severity | Finding | Evidence | Required Fix |
|----------|---------|----------|--------------|
| blocking | `docs/verifiers-impl/thesis-claims-evidence-map.md` has a malformed Markdown claim map table because the first two rows were collapsed into one line. This is a docs regression in a file the slice explicitly updated for AC-43. | `git diff -- docs/verifiers-impl/thesis-claims-evidence-map.md` shows the two original rows replaced by one long row containing `... current baseline. || The scenario catalog is deterministic ...`; reviewer table check reported `malformed_table_row line=11 pipe_count=10`. | Split the collapsed content back into two separate claim rows: one for static saga structure with the updated generic/distributed multi-saga limitation, and one for deterministic scenario catalog unchanged except as needed. Rerun `./scripts/verifier-docs build`, the stale-claim `rg`, and a simple table-row check. |

## Done Transition

- Moved to done: `no`
- Done path: `None`
- Reason if not moved: `FAIL` due blocking malformed documentation table in a slice-owned changed doc.

## Reviewer Notes

The executor behavior and Quizzes smoke are satisfactory. The remaining work is a small docs-only fix; do not change executor/source code for this review finding.
