# Slice Review: 004 - Realized Fault Compensation and Masking

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes`

## Sources Reviewed

- Slice: `issues/2026-07-07-scenario-executor-fault-vectors/004-realized-fault-compensation-and-masking.md`
- Spec: `issues/2026-07-07-scenario-executor-fault-vectors/spec.md`
- Implementation plan: `issues/2026-07-07-scenario-executor-fault-vectors/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`
- ADRs: None
- Completion evidence: `issues/2026-07-07-scenario-executor-fault-vectors/004-realized-fault-compensation-and-masking.md#completion-evidence`
- Changed files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`, `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java`, `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorCli.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java`, `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/ExecutionPlan.java`, `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/*.java`, `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/FaultVectorProviderTest.java`
- Prior review reports: None for target slice. Dependency slices `001`, `002`, and `003` are present under `done/` with PASS reports recorded in `execution-log.md`.
- Commands run by reviewer:
  - `git status --short`
  - `find issues/2026-07-07-scenario-executor-fault-vectors -maxdepth 2 -type f | sort`
  - `find issues/2026-07-07-scenario-executor-fault-vectors/review -maxdepth 1 -type f -name '004-realized-fault-compensation-and-masking-review*' -print | sort && test -e issues/2026-07-07-scenario-executor-fault-vectors/done/004-realized-fault-compensation-and-masking.md && echo DONE_EXISTS || true`
  - `git diff -- ...ScenarioExecutor.java ...FixtureWorkflow.java ...ScenarioExecutorSpec.groovy ...ExecutionPlan.java ...simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults ...`
  - `rg -n "FaultVectorProviderHolder.install|FaultVectorInjectedFaultException|realizedAndMaskedSlots|resumeCompensation|INJECTED_FAULT|FAULT_COMPENSATED|assigned fault realizes|multiple assigned faults|compensationCalls|exitCodeFor" ...`
  - `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`
  - `cd simulator && mvn -Dtest=FaultVectorProviderTest test`

## Summary

The slice satisfies the S4 contract. The executor installs the in-memory vector provider for real runs, binds per-step boundary context, treats the matching typed injected-fault signal as an expected `INJECTED_FAULT`, stops the forward loop, calls `resumeCompensation(...)`, reports `FAULT_COMPENSATED` / `COMPENSATED`, and marks later assigned `1` slots as `MASKED`. Focused verifier and simulator provider tests pass.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | pass | `ScenarioExecutor.java` installs `FaultVectorProviderHolder` provider scope, enters boundary contexts, handles matching `FaultVectorInjectedFaultException`, calls `resumeCompensation(...)`, returns `FAULT_COMPENSATED`, and updates realized/masked slots. |
| Slice out-of-scope respected | pass | No Docker/Quizzes smoke, docs rename, vector mismatch coverage, missing expected-fault behavior, or broad materialization expansion was required or claimed for S4. Defensive failure statuses remain for later S5 coverage. |
| Spec non-goals respected | pass | Implementation remains single-saga/materialized executor scope; no multi-saga/TCC/distributed execution, compensation-step fault slots, batch vector execution, catalog mutation, or search/scoring was introduced. |
| Dependencies done | pass | `done/001-simulator-in-memory-fault-provider.md`, `done/002-executor-vector-validation-and-report-v2.md`, and `done/003-no-fault-lifecycle-closure.md` exist; `execution-log.md` records PASS review reports for all three dependencies. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-6 | pass | `ScenarioExecutor.java` recognizes `FaultVectorInjectedFaultException` only on the expected current step and records `INJECTED_FAULT` with exception class/message. `ScenarioExecutorSpec` asserts the report class is `pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorInjectedFaultException` and the message contains `slot 1`; simulator provider tests assert typed signal identity fields. | The report also contains deterministic fault-slot identity, so the realized slot can be reconstructed from structured fields. |
| AC-7 | pass | Real execution uses try-with-resources around `FaultVectorProviderHolder.install(...)`; S4 tests assert `!FaultVectorProviderHolder.active` after realized and masked fault runs. `FaultVectorProviderTest` also passes provider cleanup/concurrency coverage. | Covers fault/compensation paths for this slice. |
| AC-9 | pass | S4 real runs report `providerMode = IN_MEMORY_FAULT_VECTOR`; provider install makes simulator provider authoritative. `FaultVectorProviderTest` verifies active providers suppress CSV faults/delays. | CSV-specific executor smoke remains later/out-of-scope. |
| AC-11 | pass | Test vector `010` reports `FAULT_COMPENSATED` and `COMPENSATED`, step outcomes `['COMPLETED', 'INJECTED_FAULT']`, `FixtureWorkflow.STEPS == ['first']`, `resumeCalls == 0`, and `compensationCalls == 1`. Simulator provider test also verifies provider injection occurs before the target body and compensation is legal after abort. | This proves prefix execution, pre-body fault timing, no later forward step, and compensation closure. |
| AC-12 | pass | Test vector `011` reports slot states `['NOT_ASSIGNED', 'REALIZED', 'MASKED']` and a mask reason containing `earlier realized slot 1`. | Deterministic slot ordering is preserved by index. |
| AC-17 | pass | Fault slots remain ordered by slot index from validation; S4 tests assert ordered fault-slot states and ordered step outcomes `['first', 'second']`. | No compensation steps are added to forward `stepOutcomes`. |
| AC-20 | pass | `ScenarioExecutorCli.exitCodeFor` maps `FAULT_COMPENSATED` to `0`; `ScenarioExecutorSpec` covers the mapping and passed. | CLI/Docker propagation is S6. |
| AC-21 | pass | Dummyapp-style fixture coverage now includes realized expected fault, target body not executed, compensation call, provider cleanup, and masked later assigned slots. | S5 will add mismatch/unexpected/compensation-failure coverage. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | pass | Reviewer rerun passed: `Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, finished `2026-07-08T16:58:54+01:00`. |
| `cd simulator && mvn -Dtest=FaultVectorProviderTest test` | pass | Reviewer rerun passed: `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, finished `2026-07-08T17:00:11+01:00`. This validates pre-body injection, prefix timing, provider cleanup, CSV suppression, and abort-compatible compensation legality relied on by S4. |
| Report excerpt with `FAULT_COMPENSATED`, realized slot, and masked slot | pass | Covered by S4 Spock assertions: `FAULT_COMPENSATED` / `COMPENSATED`, vector `010` slot 1 `REALIZED`, vector `011` later slot `MASKED` with reason. |
| Prefix-step/body-not-executed and compensation-legal evidence | pass | `FixtureWorkflow.STEPS == ['first']` and `compensationCalls == 1` for realized and masked tests; simulator provider test proves typed injected fault prevents target body and makes `resumeCompensation(...)` legal after workflow abort. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | S4 changes are focused on executor fault realization, fixture counters, and targeted tests. No broad pipeline refactor or new dependency was introduced. |
| Existing patterns | pass | Uses existing reflection-based executor workflow calls and simulator provider holder/scope API from prior slices. |
| Test quality | pass | Tests assert observable report status, lifecycle outcome, slot states, forward step ordering, body-not-executed behavior, compensation calls, persisted report status, provider cleanup, and exit mapping. |
| Regression risk | pass | Targeted verifier and simulator provider tests pass. Remaining S5 statuses are not required for this slice. |
| Security/data safety | n/a | No persistence migration, secrets handling, destructive operations, or external IO beyond existing temp report writes. |
| Change hygiene | pass | Target slice has completion evidence with status `implemented-awaiting-review`; no target review collision existed before writing attempt `01`; provider state is explicitly cleared in covered paths. |

## Findings

None

## Done Transition

- Moved to done: `yes`
- Done path: `issues/2026-07-07-scenario-executor-fault-vectors/done/004-realized-fault-compensation-and-masking.md`
- Reason if not moved: `None`

## Reviewer Notes

S5 still needs to complete and verify missing expected faults, wrong-slot/provider mismatches, unexpected ordinary failures, and compensation failure reporting. That is outside this slice and does not block S4 PASS.
