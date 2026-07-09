# Slice Review: 004 - Default-Vector Interleaving and Survivor Closure

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes`

## Sources Reviewed

- Slice: `issues/2026-07-08-multi-saga-scenario-executor/done/004-default-vector-interleaving-and-survivor-closure.md`
- Spec: `issues/2026-07-08-multi-saga-scenario-executor/spec.md`
- Implementation plan: `issues/2026-07-08-multi-saga-scenario-executor/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`
- ADRs: `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`
- Completion evidence: `issues/2026-07-08-multi-saga-scenario-executor/done/004-default-vector-interleaving-and-survivor-closure.md#completion-evidence`
- Changed files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`, `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`, `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java`
- Prior review reports: `issues/2026-07-08-multi-saga-scenario-executor/review/001-v3-participant-report-and-single-saga-migration-review.md`, `issues/2026-07-08-multi-saga-scenario-executor/review/002-explicit-multi-saga-selection-and-dry-run-review.md`, `issues/2026-07-08-multi-saga-scenario-executor/review/003-multi-participant-materialization-and-startup-gates-review.md`
- Commands run by reviewer:
  - `git status --short && find issues/2026-07-08-multi-saga-scenario-executor -maxdepth 3 -type f | sort`
  - `git diff -- verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java issues/2026-07-08-multi-saga-scenario-executor/004-default-vector-interleaving-and-survivor-closure.md`
  - `find issues/2026-07-08-multi-saga-scenario-executor/review -maxdepth 1 -type f | sort && test -e issues/2026-07-08-multi-saga-scenario-executor/done/004-default-vector-interleaving-and-survivor-closure.md; echo done_collision=$?`
  - `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`
  - `git diff --check -- verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java issues/2026-07-08-multi-saga-scenario-executor/004-default-vector-interleaving-and-survivor-closure.md`

## Summary

The slice satisfies the default-vector multi-saga runtime contract. After successful S3 preparation gates, `ScenarioExecutor` now installs the in-memory fault-vector provider, replays normalized runtime steps in ascending schedule order, dispatches each step by the scheduled owner saga instance id, records participant-local completed step outcomes, closes survivors deterministically, and reports `SUCCESS` only after all participants commit. The focused reviewer rerun passed.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | pass | `ScenarioExecutor.java:228-286` replaces the prior post-startup unsupported result with scheduled replay, provider scope, participant-local outcomes, survivor closure, and `SUCCESS` reporting. |
| Slice out-of-scope respected | pass | The implementation does not add compensate-and-continue, compensation failure continuation, Quizzes smoke, fixture/database reset, or true concurrency. Scheduled-step/closure failures in this slice remain hard stops (`ScenarioExecutor.java:253-260`, `:276-282`), which later slices own for richer behavior. |
| Spec non-goals respected | pass | No catalog/enrichment mutation, distributed/TCC support, retry/search/scoring, fixture synthesis/reset, or Quizzes-specific shortcut was introduced. |
| Dependencies done | pass | S1, S2, and S3 slice files are present under `issues/2026-07-08-multi-saga-scenario-executor/done/`; their latest review reports are PASS. No `done/004-default-vector-interleaving-and-survivor-closure.md` collision existed before this review. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-13 | pass | Validation normalizes runtime steps by ascending `scheduleOrder`; `runMultiSagaSchedule` iterates `candidate.steps()` directly (`ScenarioExecutor.java:245-264`). The S4 test constructs catalog order `[right order 2, left order 1]` and observes `FixtureWorkflow.PARTICIPANT_STEPS == ['left:leftRun', 'right:rightRun']` (`ScenarioExecutorSpec.groovy:625-652`). | Sequential deterministic replay only; no concurrency claim. |
| AC-14 | pass | Each runtime step resolves its owner via `bySagaId.get(step.scheduled().sagaInstanceId())` before invoking `executeUntilStep` (`ScenarioExecutor.java:240-251`). The test proves left/right steps run against their owning participant arguments (`ScenarioExecutorSpec.groovy:645-652`). | |
| AC-15 | pass | S3 participant runtime state is reused: each participant carries its own `functionality` and `unitOfWork`; S4 invokes lifecycle methods through that participant state (`ScenarioExecutor.java:247-251`, `:271-275`). | S3 already verified participant-owned unit-of-work creation; S4 uses it for scheduled execution and closure. |
| AC-16 | pass | Survivor closure filters terminal participants and sorts by executed-before-zero-step, last executed schedule order, then saga id (`ScenarioExecutor.java:265-270`). The S4 test verifies closure order `['left', 'right', 'idle']` where `idle` has no scheduled steps (`ScenarioExecutorSpec.groovy:649-653`). | |
| AC-17 | pass | Final closure filters `!terminal(participant.lifecycleOutcome)` and terminal outcomes include `COMMITTED`, `COMPENSATED`, `COMPENSATION_FAILED`, and `CLOSURE_SKIPPED` (`ScenarioExecutor.java:265-293`). | The later failure slices still own producing compensated/failed terminal participants in multi-saga runs. |
| AC-18 | pass | The S4 success test runs without an explicit vector and asserts `assignedVector == '00'` and `vectorSource == 'DEFAULT_VECTOR'` (`ScenarioExecutorSpec.groovy:636-640`). | |
| AC-31 | pass | Top-level `SUCCESS` is returned only after the scheduled-step loop and every non-terminal participant closure succeed (`ScenarioExecutor.java:245-286`). The test asserts all participants have lifecycle `COMMITTED` and top-level status is `SUCCESS` (`ScenarioExecutorSpec.groovy:636-644`). | |
| AC-36 | pass | `participantStateReport` now emits each participant's materialization/startup/lifecycle states, participant-local `stepOutcomes`, skipped steps, and participant blockers (`ScenarioExecutor.java:297-326`). The test asserts participant-local left/right/idle step outcomes and committed lifecycle states (`ScenarioExecutorSpec.groovy:641-649`). | |
| AC-37 | pass | The success report keeps top-level scenario facts: scenario kind, assigned vector/source, provider mode, fault slots, runtime metadata, blockers, and participants (`ScenarioExecutor.java:297-326`). The test asserts `MULTI_SAGA`, `IN_MEMORY_FAULT_VECTOR`, default vector/source, and scenario-level `NOT_ASSIGNED` fault slots (`ScenarioExecutorSpec.groovy:636-650`). | |
| AC-40 | pass | `ScenarioExecutorSpec` includes dummy/synthetic coverage for successful multi-saga interleaving execution, owner dispatch, participant-local outcomes, survivor closure order including zero-step participant, and provider cleanup (`ScenarioExecutorSpec.groovy:620-655`). | Later AC-40 fault/failure/exit-code cases remain assigned to S5/S6. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| Completion evidence status | pass | Slice contains `Status: implemented-awaiting-review` and records files changed, implementation summary, verification, AC evidence, deviations, and follow-ups. |
| Dependencies done | pass | S1/S2/S3 are in `done/` and their latest review reports are PASS. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | pass | Reviewer rerun passed: `Tests run: 41, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| `git diff --check -- <slice files>` | pass | No whitespace/error output. |
| Done-path collision check | pass | `done_collision=1` before move, meaning the target done file did not already exist. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Source changes are limited to the multi-saga post-startup execution path, participant-local step recording, provider assignment helper, closure ordering, and focused fixture/test instrumentation. |
| Existing patterns | pass | Keeps the existing executor/report style, reflection-based workflow lifecycle calls, `FaultVectorProviderHolder` scope pattern, and Spock fixture approach. No new dependency or broad refactor was introduced. |
| Test quality | pass | The new test asserts observable report fields, participant-local outcomes, global fixture execution order, closure order, provider mode, and provider cleanup. |
| Regression risk | pass | The focused executor suite passed and includes the earlier S1-S3 single-saga, dry-run, validation, materialization, startup, and provider behavior coverage. |
| Security/data safety | n/a | No credentials, migrations, persistent data setup/reset, network calls, or destructive operations introduced. |
| Change hygiene | pass | `git diff --check` passed for slice files. The broader worktree has unrelated unstaged/deleted/untracked files that were not modified by this review. |

## Findings

None

## Done Transition

- Moved to done: `yes`
- Done path: `issues/2026-07-08-multi-saga-scenario-executor/done/004-default-vector-interleaving-and-survivor-closure.md`
- Reason if not moved: `None`

## Reviewer Notes

This PASS certifies the default/all-zero multi-saga execution path only. Non-default assigned-fault semantics, compensate-and-continue, runtime-failure continuation, and compensation-failure aggregation remain S5/S6 responsibilities.
