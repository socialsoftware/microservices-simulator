# Slice Review: 005 - Mismatch, Unexpected, and Compensation Failures

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes`

## Sources Reviewed

- Slice: `issues/2026-07-07-scenario-executor-fault-vectors/005-mismatch-unexpected-and-compensation-failures.md`
- Spec: `issues/2026-07-07-scenario-executor-fault-vectors/spec.md`
- Implementation plan: `issues/2026-07-07-scenario-executor-fault-vectors/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`
- ADRs: None
- Completion evidence: `issues/2026-07-07-scenario-executor-fault-vectors/005-mismatch-unexpected-and-compensation-failures.md#completion-evidence`
- Changed files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorCli.java`, `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java`, `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`, simulator provider classes from dependency slices under `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/`
- Prior review reports: None for target slice. Dependency slices `001`, `002`, `003`, and `004` are present under `done/` with PASS reports.
- Commands run by reviewer:
  - `ls -la issues/2026-07-07-scenario-executor-fault-vectors && find issues/2026-07-07-scenario-executor-fault-vectors -maxdepth 2 -type f | sort`
  - `find issues/2026-07-07-scenario-executor-fault-vectors/review -maxdepth 1 -type f -name '005-mismatch-unexpected-and-compensation-failures-review*.md' -print | sort && [ -e issues/2026-07-07-scenario-executor-fault-vectors/done/005-mismatch-unexpected-and-compensation-failures.md ] && echo DONE_EXISTS || true`
  - `git status --short && git diff --stat`
  - `rg -n "expected fault not injected|unexpected injected fault|provider mismatch|runtime step failure|compensation failure|CLI exit code|EXPECTED_FAULT_NOT_INJECTED|UNEXPECTED_INJECTED_FAULT|FAULT_PROVIDER_MISMATCH|UNEXPECTED_EXECUTION_FAILURE|COMPENSATION_FAILED|FaultVectorProviderHolder.active|compensationFails|suppressFaultSignal|injectUnexpectedSignal|injectWrongSlotSignal|exitCodeFor" ...`
  - `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`

## Summary

The slice satisfies the S5 contract. The executor now classifies missing expected injected signals, unexpected injected signals at `0` bits, wrong-slot injected signals, ordinary forward failures, and compensation failures into the exact machine statuses required by the spec. It preserves ordered fault slots and step outcomes, records forward and compensation exception details when compensation fails, and clears provider state on each covered negative path. The focused verifier suite passes.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | pass | `ScenarioExecutor.java` returns `EXPECTED_FAULT_NOT_INJECTED`, `UNEXPECTED_INJECTED_FAULT`, `FAULT_PROVIDER_MISMATCH`, `UNEXPECTED_EXECUTION_FAILURE`, and `COMPENSATION_FAILED` on the relevant negative paths; `ScenarioExecutorSpec.groovy` covers each status plus provider cleanup. |
| Slice out-of-scope respected | pass | No new fault types, delay semantics, retry policy, batch execution, Docker/Quizzes smoke, docs rename, or scenario search behavior was introduced in this slice. |
| Spec non-goals respected | pass | Implementation remains within supported single-saga/local executor behavior; it does not add multi-saga/TCC/distributed execution, compensation-step fault slots, catalog mutation, or impact/search telemetry. |
| Dependencies done | pass | `done/001-simulator-in-memory-fault-provider.md`, `done/002-executor-vector-validation-and-report-v2.md`, `done/003-no-fault-lifecycle-closure.md`, and `done/004-realized-fault-compensation-and-masking.md` exist, with PASS reports in `review/`. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-7 | pass | Executor real runs still use try-with-resources around `FaultVectorProviderHolder.install(...)`; S5 tests assert `!FaultVectorProviderHolder.active` after `EXPECTED_FAULT_NOT_INJECTED`, `UNEXPECTED_INJECTED_FAULT`, `FAULT_PROVIDER_MISMATCH`, `UNEXPECTED_EXECUTION_FAILURE`, and `COMPENSATION_FAILED`. | Covers cleanup for this slice's negative paths. |
| AC-13 | pass | The suppressed-signal fixture path sets an assigned `1` slot, lets the body run, then reports `EXPECTED_FAULT_NOT_INJECTED`; tests assert slot states `['UNREALIZED', 'NOT_ASSIGNED']`, step status `UNREALIZED`, and blocker reason `EXPECTED_FAULT_NOT_INJECTED`. | The executor marks the reached assigned slot `UNREALIZED` and terminates. |
| AC-14 | pass | The unexpected-signal fixture at a `0` bit reports `UNEXPECTED_INJECTED_FAULT`; the wrong-identity fixture at an assigned `1` reports `FAULT_PROVIDER_MISMATCH`. Tests assert neither path becomes an ordinary execution failure and that the step body is not recorded. | Matching requires the current slot id, runtime step name, slot index, and assigned bit to align with the current assigned `1` slot. |
| AC-15 | pass | The fixture step named `fail` at a `0` bit throws `IllegalStateException`; executor reports `UNEXPECTED_EXECUTION_FAILURE`, records exception details, calls `resumeCompensation(...)`, and reports `lifecycleOutcome = COMPENSATED`. | The best-effort closure path also supports `CLOSURE_SKIPPED` when `resumeCompensation(...)` is unavailable. |
| AC-16 | pass | Compensation-failure test reports `COMPENSATION_FAILED` / `COMPENSATION_FAILED`, blocker reasons `FORWARD_FAILURE` and `COMPENSATION_FAILED`, forward injected-fault details, compensation exception details containing `fixture compensation failure`, and provider cleanup. `ScenarioExecutorCli.exitCodeFor('COMPENSATION_FAILED') == 1`. | The same `compensate(...)` helper is used after expected injected faults and ordinary forward failures. |
| AC-17 | pass | S5 tests assert deterministic fault-slot state order and step-outcome status order for the negative paths; validation still builds fault slots by slot index and runtime execution processes steps by schedule order from earlier slices. | Compensation steps are not appended to forward `stepOutcomes`. |
| AC-20 | pass | `ScenarioExecutorCli.exitCodeFor(...)` keeps `SUCCESS`, `FAULT_COMPENSATED`, and `DRY_RUN` at zero and returns non-zero for all other statuses by default; the test table includes `COMPENSATION_FAILED -> 1` and remains passing. | The newly introduced broken statuses are non-zero through the default branch. |
| AC-21 | pass | Dummyapp-style `ScenarioExecutorSpec` coverage now includes missing expected fault, unexpected injected fault, provider mismatch, unexpected forward failure, compensation failure, ordered statuses, and provider cleanup. | Complements previous S2-S4 coverage for validation, dry-run, default success, compensation, masking, CSV override, and unsupported shape. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | pass | Reviewer rerun passed: `Tests run: 32, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, finished `2026-07-08T17:09:37+01:00`. |
| CLI/status helper coverage | pass | Included in `ScenarioExecutorSpec`: `SUCCESS`, `FAULT_COMPENSATED`, and `DRY_RUN` map to `0`; invalid/broken examples including `COMPENSATION_FAILED` map to `1`. No separate CLI command is needed for this slice. |
| Report/status excerpts for negative terminal states | pass | Covered by Spock assertions for `EXPECTED_FAULT_NOT_INJECTED`, `UNEXPECTED_INJECTED_FAULT`, `FAULT_PROVIDER_MISMATCH`, `UNEXPECTED_EXECUTION_FAILURE`, and `COMPENSATION_FAILED`, including blocker reasons and step/fault-slot states. |
| Provider cleanup evidence | pass | S5 Spock assertions check `!FaultVectorProviderHolder.active` after every added negative path. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Changes are focused on executor classification and fixture/test knobs for the S5 negative paths. No broad verifier pipeline refactor or new dependency was introduced. |
| Existing patterns | pass | Uses the existing reflection-based workflow invocation style, prior-slice provider scope API, v2 report records, and Spock fixture style. |
| Test quality | pass | Tests assert observable terminal statuses, lifecycle outcomes, blocker reasons/messages, ordered step/slot states, compensation calls, body-not-run behavior for injected signals, exit mapping, and provider cleanup. |
| Regression risk | pass | Focused verifier executor suite passes. S5 does not alter simulator provider contracts, so the plan's simulator-provider rerun condition was not triggered. |
| Security/data safety | n/a | No destructive operation, migration, secret handling, or external network behavior was introduced. |
| Change hygiene | pass | Completion evidence status is `implemented-awaiting-review`; no existing 005 review artifact or done-path collision existed before this review; unrelated worktree changes were not edited. |

## Findings

None

## Done Transition

- Moved to done: `yes`
- Done path: `issues/2026-07-07-scenario-executor-fault-vectors/done/005-mismatch-unexpected-and-compensation-failures.md`
- Reason if not moved: `None`

## Reviewer Notes

Provider-mismatch coverage intentionally exercises a wrong scheduled-step identity, which is the slice's required wrong-slot case. The simulator provider contract from S1 prevents ordinary provider output from drifting across scenario execution ids, and provider cleanup is covered on all S5 negative paths.
