# Slice Review: 005 - Assigned-Fault Compensate-and-Continue

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes`

## Sources Reviewed

- Slice: `issues/2026-07-08-multi-saga-scenario-executor/done/005-assigned-fault-compensate-and-continue.md`
- Spec: `issues/2026-07-08-multi-saga-scenario-executor/spec.md`
- Implementation plan: `issues/2026-07-08-multi-saga-scenario-executor/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`
- ADRs: `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`
- Completion evidence: `issues/2026-07-08-multi-saga-scenario-executor/005-assigned-fault-compensate-and-continue.md#completion-evidence`
- Changed files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`, `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`, `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java`, `issues/2026-07-08-multi-saga-scenario-executor/005-assigned-fault-compensate-and-continue.md`
- Prior review reports: `issues/2026-07-08-multi-saga-scenario-executor/review/001-v3-participant-report-and-single-saga-migration-review.md`, `issues/2026-07-08-multi-saga-scenario-executor/review/002-explicit-multi-saga-selection-and-dry-run-review.md`, `issues/2026-07-08-multi-saga-scenario-executor/review/003-multi-participant-materialization-and-startup-gates-review.md`, `issues/2026-07-08-multi-saga-scenario-executor/review/004-default-vector-interleaving-and-survivor-closure-review.md`
- Commands run by reviewer:
  - `git status --short && git diff --stat && git diff --name-only`
  - `test -e issues/2026-07-08-multi-saga-scenario-executor/done/005-assigned-fault-compensate-and-continue.md; echo done_collision=$?; ... dependency done checks ...; find issues/2026-07-08-multi-saga-scenario-executor/review -maxdepth 1 -type f -name '005-assigned-fault-compensate-and-continue-review*.md' -print | sort`
  - `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`
  - `git diff --check -- verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy issues/2026-07-08-multi-saga-scenario-executor/005-assigned-fault-compensate-and-continue.md`
  - `nl -ba ...ScenarioExecutor.java | sed -n '235,455p'; nl -ba ...ScenarioExecutorSpec.groovy | sed -n '650,735p'`

## Summary

The slice satisfies the multi-saga assigned-fault compensate-and-continue contract. `ScenarioExecutor` now realizes assigned injected faults only for the current participant/slot identity, compensates the failed participant, marks it terminal, records participant skipped-step evidence, masks only later assigned slots for that same participant, continues surviving participants, and aggregates mixed/all-compensated outcomes to `PARTIAL_COMPENSATED`/`COMPENSATED`. The focused executor suite passed under reviewer rerun.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | pass | Multi-saga schedule execution handles `FaultVectorInjectedFaultException` as a participant-local outcome (`ScenarioExecutor.java:258-271`), compensates the owning participant (`:263-265`), skips later same-participant steps (`:248-249`, `:332-337`), preserves survivor execution (`:271`), and aggregates final status (`:310`, `:322-329`). |
| Slice out-of-scope respected | pass | Non-assigned scheduled-step runtime failures and compensation-failure continuation remain hard-stop behavior in this slice (`ScenarioExecutor.java:280-285`, `:266-270`), matching S6 ownership. No Quizzes smoke or docs update was added. |
| Spec non-goals respected | pass | No catalog/enrichment mutation, true concurrency, distributed/TCC parity, retry/deferred compensation, search/scoring, fixture synthesis, or Quizzes-specific shortcut was introduced. |
| Dependencies done | pass | S1-S4 slice files are present in `issues/2026-07-08-multi-saga-scenario-executor/done/`, and their latest review reports are PASS. Done-path collision check returned `done_collision=1` before this review, so no existing done file would be overwritten. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-20 | pass | Scenario-level fault slots retain the v3 realization vocabulary. Multi-saga assigned-fault tests assert `REALIZED`, `NOT_ASSIGNED`, and `MASKED_BY_SAGA_FAILURE` states (`ScenarioExecutorSpec.groovy:681-685`, `:717-720`). `markNotReached` now preserves realized/masked states instead of rewriting them (`ScenarioExecutor.java:372-377`). | `EXPECTED_FAULT_NOT_INJECTED`/hard-stop cases remain S6 where mapped. |
| AC-21 | pass | Multi-saga injected-fault realization checks current scheduled step id, runtime step name, slot index, assigned bit, scenario execution id, scenario plan id, and saga instance id (`ScenarioExecutor.java:380-392`). Provider assignments are built per assigned slot using the owning participant context (`ScenarioExecutor.java:466-474`, `:485-495`). | The existing in-memory provider also gates normal injections by scenario/plan/saga/step/runtime identity. |
| AC-22 | pass | `multi-saga assigned faults in surviving participants remain active and later failed participant slots are masked` realizes left slot 0 and surviving right slot 3 in one attempt and reports both participants `COMPENSATED` (`ScenarioExecutorSpec.groovy:695-725`). | |
| AC-23 | pass | Same test asserts left participant's later assigned slot is `MASKED_BY_SAGA_FAILURE` (`ScenarioExecutorSpec.groovy:717-720`). Masking is restricted to the failed saga instance (`ScenarioExecutor.java:442-449`). | |
| AC-24 | pass | Same test proves the right participant's assigned slot remains active after left fails and becomes `REALIZED` (`ScenarioExecutorSpec.groovy:717-724`). `realizedAndMaskedSlots` filters by failed saga instance id, so survivor slots are not masked (`ScenarioExecutor.java:447-448`). | |
| AC-27 | pass | `multi-saga assigned fault compensates failed participant skips its later steps and lets survivor commit` asserts left `COMPENSATED`, left skipped step `left-second`, right step outcomes `rightFirst/rightSecond`, right-only closure, one compensation call, and `PARTIAL_COMPENSATED` (`ScenarioExecutorSpec.groovy:658-692`). | Failed participants are excluded from final closure by terminal lifecycle filtering (`ScenarioExecutor.java:248-249`, `:290-295`). |
| AC-32 | pass | The multiple-fault test asserts top-level `COMPENSATED` when both participants compensate and none commits (`ScenarioExecutorSpec.groovy:717-724`). Aggregation returns `COMPENSATED` for compensated-without-committed participants (`ScenarioExecutor.java:322-329`). | |
| AC-33 | pass | The survivor-continuation test asserts top-level `PARTIAL_COMPENSATED` when one participant compensates and a survivor commits (`ScenarioExecutorSpec.groovy:681-689`). Aggregation returns `PARTIAL_COMPENSATED` for mixed compensated/committed outcomes (`ScenarioExecutor.java:322-329`). | |
| AC-36 | pass | Participant reports include lifecycle outcomes, participant-local step outcomes, and skipped steps for failed participants (`ScenarioExecutor.java:355-365`; tests at `ScenarioExecutorSpec.groovy:683-688`, `:719-721`). | |
| AC-37 | pass | Top-level report facts remain scenario-level: terminal status, provider mode, assigned vector/fault slots, blockers, and participants are emitted through `participantStateReport` (`ScenarioExecutor.java:340-369`). Tests assert terminal status, fault slots, and provider cleanup for assigned paths (`ScenarioExecutorSpec.groovy:681-692`, `:717-725`). | |
| AC-40 | pass | `ScenarioExecutorSpec` now includes focused dummy coverage for assigned-fault survivor continuation, no double closure of failed participants, multiple realized faults across participants, same-participant masking, aggregate status vocabulary, and provider cleanup (`ScenarioExecutorSpec.groovy:658-725`). | Other AC-40 runtime-failure/exit-code cases remain assigned to S6. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| Completion evidence status | pass | Slice contains `Status: implemented-awaiting-review` and records files changed, implementation summary, test evidence, AC evidence, deviations, and follow-ups. |
| Dependencies done | pass | S1-S4 are under `done/` and latest review reports are PASS. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | pass | Reviewer rerun passed: `Tests run: 43, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| `git diff --check -- <slice files>` | pass | No whitespace/error output. |
| Done-path collision check | pass | `done_collision=1` before move, meaning `done/005-assigned-fault-compensate-and-continue.md` did not already exist. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Source changes are limited to the multi-saga schedule loop's assigned-fault branch, same-participant masking, skipped-step reporting, identity matching, and aggregate status calculation. |
| Existing patterns | pass | Keeps existing reflection lifecycle calls, `FaultVectorProviderHolder` scoped provider/boundary pattern, report record construction style, and Spock fixture-based executor tests. No dependency was added. |
| Test quality | pass | Tests assert observable report behavior, participant lifecycle/step/skipped-step fields, survivor execution/closure side effects, compensation counts, multiple-fault realization, same-participant masking, and provider cleanup. |
| Regression risk | pass | The focused executor suite passed and includes prior S1-S4 coverage for v3 reports, dry-run/selection, materialization/startup gates, default interleaving, closure ordering, and single-saga behavior. |
| Security/data safety | n/a | No credentials, migrations, persistent data operations, network calls, or destructive commands were introduced. |
| Change hygiene | pass | Targeted `git diff --check` passed. The broader worktree contains unrelated unstaged/deleted/untracked files that were not modified by this review. |

## Findings

None

## Done Transition

- Moved to done: `yes`
- Done path: `issues/2026-07-08-multi-saga-scenario-executor/done/005-assigned-fault-compensate-and-continue.md`
- Reason if not moved: `None`

## Reviewer Notes

This PASS covers S5 assigned-fault semantics only. Non-assigned runtime exceptions, compensation-failure continuation, expected-fault-not-injected/provider hard stops, exit-code behavior, Quizzes smoke, and documentation refresh remain S6/S7 responsibilities. The stale current-state/reference docs still describe pre-feature executor support and should be updated in S7, not in this slice.
