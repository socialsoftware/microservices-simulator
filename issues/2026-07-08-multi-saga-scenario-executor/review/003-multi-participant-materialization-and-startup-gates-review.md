# Slice Review: 003 - Multi-Participant Materialization and Startup Gates

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes`

## Sources Reviewed

- Slice: `issues/2026-07-08-multi-saga-scenario-executor/done/003-multi-participant-materialization-and-startup-gates.md`
- Spec: `issues/2026-07-08-multi-saga-scenario-executor/spec.md`
- Implementation plan: `issues/2026-07-08-multi-saga-scenario-executor/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`
- ADRs: `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`
- Completion evidence: `issues/2026-07-08-multi-saga-scenario-executor/done/003-multi-participant-materialization-and-startup-gates.md#completion-evidence`
- Changed files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioMaterializer.java`, `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`, `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java`
- Prior review reports: `issues/2026-07-08-multi-saga-scenario-executor/review/001-v3-participant-report-and-single-saga-migration-review.md`, `issues/2026-07-08-multi-saga-scenario-executor/review/002-explicit-multi-saga-selection-and-dry-run-review.md`
- Commands run by reviewer:
  - `test -f issues/2026-07-08-multi-saga-scenario-executor/done/001-v3-participant-report-and-single-saga-migration.md && test -f issues/2026-07-08-multi-saga-scenario-executor/done/002-explicit-multi-saga-selection-and-dry-run.md && echo dependencies_done`
  - `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`
  - `cd verifiers && mvn -Dtest=ScenarioExecutorReadinessEvaluatorSpec test`
  - `git diff --check -- verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioMaterializer.java verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java issues/2026-07-08-multi-saga-scenario-executor/003-multi-participant-materialization-and-startup-gates.md`

## Summary

The slice satisfies the materialization/startup preparation gate contract. Non-dry-run explicit multi-saga plans now enter a preparation path after structural/vector validation, materialize every selected participant before startup, instantiate participants only after successful all-participant materialization, and hard-stop materialization/startup failures before provider installation, scheduled steps, closure, or compensation. The materializer accepts a pre-created participant-owned `SagaUnitOfWork`, and the single-saga execution path now reuses that same object for constructor/runtime-owned arguments and lifecycle calls. Targeted reviewer verification passed.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | pass | `ScenarioExecutor.java:112-116` routes non-dry-run multi-saga plans to the new preparation path and creates a pre-owned unit of work for single-saga materialization. `ScenarioExecutor.java:183-230` builds participant runtime states, materializes all participants, starts participants only after materialization success, and reports gate failures. |
| Slice out-of-scope respected | pass | Successful post-startup multi-saga scheduled execution still returns `UNSUPPORTED_SCENARIO` at `ScenarioExecutor.java:228-230`; no interleaving replay, closure ordering, assigned-fault continuation, compensation-failure policy, Quizzes smoke, or docs update was added in this slice. |
| Spec non-goals respected | pass | No catalog/enrichment mutation, distributed/TCC support, fixture synthesis, persistent database setup/reset, search/scoring, or Quizzes-specific shortcut was introduced. |
| Dependencies done | pass | S1 and S2 slice files are present under `issues/2026-07-08-multi-saga-scenario-executor/done/`; their latest review reports are PASS. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-7 | pass | `ScenarioExecutor.java:196-206` loops over all participant runtime states and calls `ScenarioMaterializer.materialize(...)` for each participant before any startup loop. The materialization-failure test at `ScenarioExecutorSpec.groovy:556-585` reports `MATERIALIZED` for one participant and `MATERIALIZATION_FAILED` for the other. | Uses existing materializer semantics with runtime-owned argument handling. |
| AC-8 | pass | Materialization failure returns `MATERIALIZATION_FAILED` with `providerMode=NONE` at `ScenarioExecutor.java:208-210`; participant entries are built with participant-local blockers at `ScenarioExecutor.java:248-258`; test assertions at `ScenarioExecutorSpec.groovy:571-585` cover participants, blockers, no constructor/startup/steps/closure/compensation, and no active provider. | |
| AC-9 | pass | Startup runs only after materialization has no blockers (`ScenarioExecutor.java:208-212`) and startup failure returns `STARTUP_FAILED` with provider `NONE` (`ScenarioExecutor.java:224-226`). Test assertions at `ScenarioExecutorSpec.groovy:603-615` cover startup states, startup blocker, no scheduled steps, no closure, no compensation, and no provider. | |
| AC-15 | pass | `ParticipantRuntimeState` stores `unitOfWork`, `materializedArguments`, and `functionality` (`ScenarioExecutor.java:647-656`). Multi-saga preparation creates one unit of work per participant and passes it to the materializer (`ScenarioExecutor.java:196-198`). Single-saga execution creates one unit of work before materialization and reuses it for lifecycle calls (`ScenarioExecutor.java:115-135`, `:172-173`). The regression test at `ScenarioExecutorSpec.groovy:618-635` proves constructor and lifecycle calls receive the same object. | Full multi-saga lifecycle reuse is necessarily completed by later execution slices; this slice leaves the participant state holder ready for that path. |
| AC-25 | pass | Gate reports call `markNotReached(slots)` when building the report (`ScenarioExecutor.java:260-270`). Materialization/startup tests assert assigned slots become `NOT_REACHED` (`ScenarioExecutorSpec.groovy:580`, `:610`). | Unassigned slots remain `NOT_ASSIGNED`. |
| AC-30 | pass | Materialization/startup failures return hard-stop terminal statuses with `providerMode=NONE` before provider installation (`ScenarioExecutor.java:208-226`) and before scheduled execution. Tests assert no provider, no steps, no closure, and no compensation for both gate failures. | |
| AC-36 | pass | Participant entries include saga/input ids, materialization/startup/lifecycle states, empty step outcomes for hard stops, skipped-step list, and participant-local blockers (`ScenarioExecutor.java:248-258`). Tests assert required participant state vocabulary and blockers for materialization/startup failures. | |
| AC-37 | pass | `participantStateReport(...)` keeps top-level scenario facts including scenario execution id, selected plan id/kind, assigned vector/source, provider mode, runtime metadata, fault slots, skipped counts, blockers, and participants (`ScenarioExecutor.java:233-262`). Tests assert terminal status, provider mode, participants, vector-derived fault-slot states, and no runtime side effects. | |
| AC-40 | pass | `ScenarioExecutorSpec` now includes focused dummy/synthetic coverage for multi-saga materialization failure, startup failure, hard-stop slot state, provider absence, no lifecycle side effects, and `SagaUnitOfWork` reuse. Reviewer reran the focused suite successfully. | Later AC-40 interleaving/fault/closure/exit-code cases remain assigned to later slices. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| Completion evidence status | pass | Slice contains `Status: implemented-awaiting-review` with files changed, commands, AC evidence, deviations, and follow-ups. |
| Dependencies done | pass | Reviewer command returned `dependencies_done`. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | pass | Reviewer rerun passed: `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorReadinessEvaluatorSpec test` | pass | Reviewer rerun passed: `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`; run because materializer runtime-owned handling changed. |
| `git diff --check -- <slice files>` | pass | No whitespace/error output. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Source changes are limited to executor gate sequencing, a narrow materializer overload/runtime context propagation, and focused fixture/test instrumentation. |
| Existing patterns | pass | Keeps existing Java record/report style, current executor API, current materialization policy, and Spock fixture pattern. No new dependency or broad refactor was introduced. |
| Test quality | pass | Tests assert externally observable report status/state, participant blockers, fault-slot state, provider absence, and lifecycle/constructor counters rather than only private implementation details. |
| Regression risk | pass | Focused executor and readiness suites passed; S1/S2 behavior remains covered in the same `ScenarioExecutorSpec` run. |
| Security/data safety | n/a | No credentials, migrations, destructive operations, persistent data setup/reset, or network calls introduced. |
| Change hygiene | pass | Slice diff is confined to the listed implementation/test/slice files. The broader worktree has unrelated unstaged/deleted/untracked files that were not modified by this review. |

## Findings

None

## Done Transition

- Moved to done: `yes`
- Done path: `issues/2026-07-08-multi-saga-scenario-executor/done/003-multi-participant-materialization-and-startup-gates.md`
- Reason if not moved: `None`

## Reviewer Notes

The successful-preparation path still stops with `UNSUPPORTED_SCENARIO` after startup, which matches the slice deviation and keeps scheduled-step execution/final closure in S4. Documentation still describes the older narrow executor path in places; S7 owns those updates.
