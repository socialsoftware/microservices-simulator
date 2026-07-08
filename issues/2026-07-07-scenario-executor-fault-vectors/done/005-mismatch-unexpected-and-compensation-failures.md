# 005 - Mismatch, Unexpected, and Compensation Failures

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `001-simulator-in-memory-fault-provider.md`, `002-executor-vector-validation-and-report-v2.md`, `003-no-fault-lifecycle-closure.md`, `004-realized-fault-compensation-and-masking.md`  
ACs covered: `AC-7, AC-13, AC-14, AC-15, AC-16, AC-17, AC-20, AC-21`  
Risk: `high`

## Purpose

Finish the executor's negative and broken-execution semantics so provider mismatches, missing expected injected faults, ordinary runtime failures, and compensation failures are reported with exact machine statuses instead of generic step failures.

## Scope

- Report `EXPECTED_FAULT_NOT_INJECTED` when a reached assigned `1` slot completes without the expected injected-fault signal.
- Report `UNEXPECTED_INJECTED_FAULT` when an injected-fault signal appears for an assigned `0` slot.
- Report `FAULT_PROVIDER_MISMATCH` when the injected-fault signal identity does not match the expected current slot/provider assignment.
- Report `UNEXPECTED_EXECUTION_FAILURE` for ordinary forward exceptions at `0` bits and attempt best-effort lifecycle closure when simulator APIs expose it.
- Report `COMPENSATION_FAILED` when compensation closure fails after a realized expected fault or unexpected forward failure, including compensation exception details.
- Ensure invalid/broken statuses map to non-zero CLI exits while `SUCCESS`, `FAULT_COMPENSATED`, and `DRY_RUN` remain zero.
- Preserve deterministic slot and step ordering in all failure reports.
- Ensure provider state is cleared after every failure path.

## Out of Scope

- Adding new fault types or delay semantics.
- Retry/recovery policy beyond the simulator lifecycle primitives already exposed.
- Batch execution or retrying vectors after failure.
- Quizzes Docker smoke; S6 handles realistic smoke evidence.

## Repo Anchors

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — exception classification and lifecycle closure logic.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java` — exact terminal/lifecycle/error fields.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorCli.java` — exit-code mapping for exact statuses.
- `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java` — fixture knobs for unexpected body exceptions and compensation failures.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — negative-path report coverage.
- Simulator provider classes from S1 — identity/signal accessors used for mismatch classification.

## Implementation Shape

- Centralize exception classification so the executor can distinguish typed injected-fault signals from ordinary runtime exceptions after reflection/`CompletionException` unwrapping.
- Compare injected-fault identity against the current slot assignment and vector bit before deciding whether it is expected, unexpected, or a mismatch.
- For an assigned `1` slot that returns normally, mark it `UNREALIZED` and terminate with `EXPECTED_FAULT_NOT_INJECTED`.
- For ordinary runtime failures at `0` bits, record the failed forward outcome and attempt compensation/closure only when the current workflow path exposes it. If closure is not exposed, report `lifecycleOutcome = CLOSURE_SKIPPED`.
- For compensation failures, preserve both the original forward fault/failure context and compensation exception details in the v2 report, with terminal status `COMPENSATION_FAILED`.
- Ensure provider cleanup runs after report construction in all branches.

## TDD / Test Shape

- First behavior to test: a reached assigned `1` with no injected signal returns `EXPECTED_FAULT_NOT_INJECTED` and marks that slot `UNREALIZED`.
- Expected red failure: current executor has no expected-fault tracking and treats only thrown exceptions as failures.
- Additional coverage:
  - injected signal at `0` bit -> `UNEXPECTED_INJECTED_FAULT`;
  - injected signal with wrong slot identity -> `FAULT_PROVIDER_MISMATCH`;
  - ordinary body exception at `0` -> `UNEXPECTED_EXECUTION_FAILURE` with best-effort compensation/closure evidence;
  - compensation closure throws -> `COMPENSATION_FAILED` with exception class/message and non-zero exit mapping;
  - provider cleanup after each negative path;
  - step outcomes and fault slots remain deterministically ordered.

## Just-in-Time Preflight Required

Before source edits, the executor must re-check:

- Current exception wrapper shapes from reflection (`InvocationTargetException`), `CompletionException`, and simulator workflow methods.
- Exact status/lifecycle enum or string names from S2.
- Current fixture workflow capabilities from S3/S4.
- Existing CLI tests or helper methods for exit status mapping.

## Verification

- `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` — all negative-path executor tests and provider-cleanup assertions pass.
- CLI/status helper test command if separated from `ScenarioExecutorSpec` — exact zero/non-zero mapping passes.

## Evidence to Record

- files changed
- commands run and outputs
- report excerpts for each negative terminal status
- provider cleanup evidence
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- Do not collapse provider mismatches into ordinary `UNEXPECTED_EXECUTION_FAILURE`.
- Do not mark expected injected faults as step execution failures.
- Best-effort compensation after ordinary failures should not hide the original failure; if compensation fails, terminal status becomes `COMPENSATION_FAILED` with both contexts available.
- Keep exact status names from the spec; do not introduce aliases like the old `STEP_EXECUTION_FAILED` for vector runs.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Added executor classification for missing expected injected faults, unexpected injected signals at `0` bits, wrong-identity injected signals, ordinary forward failures, and compensation failures.
- Added best-effort compensation closure for ordinary forward failures and preserved both forward and compensation exception details when compensation fails.
- Extended the fixture workflow with targeted knobs for suppressed provider signals, unexpected injected signals, wrong-slot signals, and compensation failures.
- Added dummyapp-style Spock coverage for all S5 negative statuses plus provider cleanup and CLI non-zero status mapping through the existing helper test.

### Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — classified injected-fault signals vs ordinary failures, added missing expected-fault handling, best-effort compensation, compensation-failure reporting, and reflection/`CompletionException` unwrapping.
- `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java` — added fixture controls for no-signal, unexpected-signal, wrong-slot-signal, and compensation-failure paths.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — added/updated negative-path tests and provider cleanup assertions.
- `issues/2026-07-07-scenario-executor-fault-vectors/005-mismatch-unexpected-and-compensation-failures.md` — recorded completion evidence.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | PASS | Maven build success; `Tests run: 32, Failures: 0, Errors: 0, Skipped: 0`. |

### Acceptance Criteria Evidence

- AC-7: Provider cleanup is asserted after each added negative path (`EXPECTED_FAULT_NOT_INJECTED`, `UNEXPECTED_INJECTED_FAULT`, `FAULT_PROVIDER_MISMATCH`, `UNEXPECTED_EXECUTION_FAILURE`, `COMPENSATION_FAILED`) using `!FaultVectorProviderHolder.active`.
- AC-13: Reached assigned `1` with suppressed injected signal reports `EXPECTED_FAULT_NOT_INJECTED`; the slot remains `UNREALIZED`; report step statuses are deterministic (`COMPLETED`, `UNREALIZED`).
- AC-14: Fixture-injected signal at an assigned `0` reports `UNEXPECTED_INJECTED_FAULT`; wrong scheduled-step identity on an assigned `1` reports `FAULT_PROVIDER_MISMATCH`; neither path reports ordinary runtime failure.
- AC-15: Ordinary fixture body exception at a `0` bit reports `UNEXPECTED_EXECUTION_FAILURE`, records exception details, and calls `resumeCompensation(...)` with `lifecycleOutcome = COMPENSATED` for the exposed fixture path.
- AC-16: Compensation failure after a realized expected fault reports `COMPENSATION_FAILED`, records forward fault and compensation exception details, and remains covered by CLI helper non-zero status mapping.
- AC-17: Tests assert ordered step outcomes and fault-slot realization states for the negative paths.
- AC-20: Existing CLI helper coverage keeps `SUCCESS`, `FAULT_COMPENSATED`, and `DRY_RUN` zero and broken statuses including `COMPENSATION_FAILED` non-zero.
- AC-21: Dummyapp-style executor coverage now includes missing expected fault, unexpected injected fault, wrong-slot provider mismatch, unexpected forward failure, compensation failure, and provider cleanup.

### Browser / Manual Evidence

- Not required.

### TDD Notes

- Added the focused missing-expected-fault Spock case and related negative-path cases before implementing the executor classification changes. The complete targeted command now passes.

### Deviations From Plan

- No separate CLI/status command was needed because `ScenarioExecutorSpec` already contains the focused `ScenarioExecutorCli.exitCodeFor(...)` helper coverage.

### Blockers / Follow-Ups

- None.
