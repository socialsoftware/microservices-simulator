# Slice Review: 007 - Runtime Fallback and Hard Stops

## Review Attempt

Attempt: `02`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes`

## Sources Reviewed

- Slice: `issues/2026-07-19-compensation-aware-scenario-catalog/007-runtime-fallback-and-hard-stops.md`
- Spec: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Implementation plan: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`, `issues/2026-07-19-compensation-aware-scenario-catalog/decision-frame.md`
- ADRs: `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`, `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`
- Completion evidence: `007-runtime-fallback-and-hard-stops.md` — `## Completion Evidence` (`Status: implemented-awaiting-review`)
- Changed files reviewed: `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/Workflow.java`; `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowFunctionality.java`; `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowRecoveryCheckpoint.java`; `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowStepExecutionResult.java`; `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowStepRecoveryException.java`; `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/unitOfWork/SagaUnitOfWork.java`; `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/unitOfWork/SagaUnitOfWorkService.java`; `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaWorkflow.java`; `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/unitOfWork/SagaStepwiseRecoveryTest.java`; `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaExecutorControlTest.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioReportWriteException.java`; `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java`; `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`
- Prior review reports: `issues/2026-07-19-compensation-aware-scenario-catalog/review/007-runtime-fallback-and-hard-stops-review-01.md`
- Commands run by reviewer:
  - `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test`
  - `cd simulator && mvn -DskipTests install`
  - `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`
  - direct inspection of exact, deviated, and compensation-failed serialized reports under `/tmp/v3-executor-package8097094936401475169`, `/tmp/v3-executor-package15321752176929031200`, and `/tmp/v3-executor-package5726674303669120343`
  - SHA-256 inspection of the five package artifacts in those sample package directories
  - `rg -n "deviationActionId == null|multiple zero-bit fallbacks preserve" ...`
  - `git diff --check`
  - `git status --short` and review/done collision inspection

## Summary

The slice now satisfies its fallback and hard-stop contract. Zero-bit `SimulatorException` body and controlled-commit failures recover immediately from Saga unit-of-work checkpoint truth, emit runtime-only references where needed, retain ordered explicit/implicit sub-outcomes, skip only the failed participant's invalid suffix, and continue valid survivors. Scheduled and fallback recovery failures stop on the first failed sub-outcome with `COMPENSATION_FAILED / INCOMPLETE`, preserve retry-safe simulator markers, and do not automatically retry or continue survivors.

The attempt-01 blocker is fixed: both body- and commit-fallback branches set singular deviation metadata only when no prior deviation exists, and the added two-fallback regression proves that later valid fallback outcomes do not replace the first schedule-divergence point. Required simulator and verifier suites pass from the current worktree, package checksum assertions pass, and inspected exact/deviated/incomplete samples match the report contract.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | pass | The current simulator boundary, executor state machine, report model, and fixture coverage implement zero-bit body/commit fallback, runtime checkpoint reporting, compensation hard stops, retry-safe markers, conformance, lifecycle, fault-slot, skip, and immutability behavior. |
| Slice out-of-scope respected | pass | No retry loop/count/backoff, compensation fault slot, recovery-schedule mutation, scorer behavior, or survivor continuation after compensation failure was introduced. |
| Spec non-goals respected | pass | No TCC/distributed/stream/gRPC parity, true concurrency, delayed commit, impact scoring/search, reset/seeding, v2 compatibility, or new dependency was added. |
| Dependencies done | pass | Required dependency `006-exact-persisted-action-replay.md` exists at `issues/2026-07-19-compensation-aware-scenario-catalog/done/006-exact-persisted-action-replay.md`. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-31 | pass | `Workflow.executeStepForExecutorControlled`, controlled finalization, Saga recovery checkpoint discovery, and executor fallback branches distinguish zero-bit `SimulatorException` body/commit failures, abort once, recover immediately, skip the owner suffix, and continue valid survivor actions. | Required body, partial-step, commit, and survivor cases pass. |
| AC-32 | pass | Runtime recovery is derived from `SagaUnitOfWork` pending explicit/implicit markers; runtime actions can have nullable planned checkpoint/position and carry checkpoint-level ordered sub-outcomes. Package checksum tests prove no persisted scenario mutation. | `ScenarioExecutor.java:295` and `:343` preserve the first deviation point; the regression at `ScenarioExecutorSpec.groovy:290` covers two valid fallbacks. |
| AC-33 | pass | Non-`SimulatorException` body/commit failures, provider installation, selection, materialization/startup, report writing, and outer replay failures remain infrastructure outcomes rather than domain fallback. | Pre-measurement provider failure has absent conformance; measured body/commit failures are `INCOMPLETE`. |
| AC-34 | pass | Scheduled explicit, fallback explicit, and fallback implicit failures produce one failed sub-outcome, `COMPENSATION_FAILED`, hard-stop metadata, survivor hard-stop skips, and no later execution. | Serialized scheduled-failure sample and focused assertions confirm the partial prefix. |
| AC-35 | pass | Explicit compensation is marked executed only after success; implicit rollback is marked aborted only after success. Simulator tests prove a later explicit retry and explicit-success/implicit-failure retry behavior; executor counters prove no automatic retry. | Required simulator suite passes. |
| AC-36 | pass | Assigned and runtime aborts with no recovery work emit `NO_COMPENSATION_WORK` then `COMPENSATED`, with zero compensation action outcomes; terminal participant guards remain intact. | Focused executor cases pass. |
| AC-37 | pass | Report v4 includes attempt/workload/scenario identity, vector/slots, planned and actual actions, lifecycle, participants, blockers, final states, terminal status, and deviation/hard-stop metadata. | Exact, deviated, and failed JSON samples were inspected. |
| AC-38 | pass | Tests cover complete `EXACT`, completed `DEVIATED`, measured `INCOMPLETE`, and absent pre-measurement conformance. | Report-write failure converts a measured completed trace to an in-memory `INCOMPLETE` infrastructure trace. |
| AC-39 | pass | Action outcomes include nullable planned/runtime references, source identities, positions, failure origin/details, evidence, and ordered successful/failed recovery sub-outcomes. | The deviated sample records runtime-only implicit rollback followed by planned-checkpoint explicit compensation. |
| AC-40 | pass | The fixture matrix covers realized, masked, not-reached, unassigned, participant-local deviation skips, and scenario hard-stop skips. | Assigned and zero-bit failure reports retain owner-local diagnostics. |
| AC-41 | pass | Abort, automatic commit, compensated, compensation-failed, and no-work lifecycle events are emitted; final forward outcomes distinguish body success/failure from commit success/failure. | Body and controlled-commit cases pass. |
| AC-42 | pass | Fallback, compensation-failure, report-write, dry-run, and measured tests protect package artifacts with before/after checksums and output-alias guards. | The inspected deviated package retains the completion-evidence hashes `b14c9a...`, `7a91de...`, `636dc9...`, `55b6b0...`, and `e3b0c4...`. |
| AC-44 | pass | Dummyapp-style `FixtureWorkflow`, simulator controls, and 56 executor cases provide deterministic automated slice coverage, including the new multiple-fallback first-deviation regression. | No application-specific production shortcut was introduced. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` | pass | Reviewer rerun: `Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `cd simulator && mvn -DskipTests install` | pass | Refreshed the standalone simulator artifact consumed by `verifiers`; Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | pass | Reviewer rerun: `Tests run: 56, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| Attempt-01 first-deviation regression | pass | Two zero-bit fallbacks complete as `DEVIATED`; report metadata remains on planned action/position `0`, while both forward outcomes retain `UNASSIGNED_RUNTIME`. |
| Exact/deviated/incomplete report inspection | pass | Exact sample is `SUCCESS / EXACT`; fallback sample is `PARTIAL_COMPENSATED / DEVIATED`; scheduled recovery failure is `COMPENSATION_FAILED / INCOMPLETE` with the failed explicit sub-outcome and hard-stop metadata. |
| Package immutability evidence | pass | Focused tests assert before/after package checksums; reviewer SHA-256 inspection reproduced the five recorded hashes for the deviated sample package. |
| `git diff --check` | pass | No whitespace errors. |
| Browser/manual evidence | n/a | No browser behavior is in scope; serialized reports and package artifacts were inspected directly. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Changes are limited to narrow executor-oriented Saga controls, unit-of-work recovery truth, action-aware reporting, focused fixture probes, and tests. |
| Existing patterns | pass | The implementation reuses the S6 persisted replay state machine, Saga unit-of-work bookkeeping, controlled finalization, report records, and deterministic fixture/package patterns. |
| Test quality | pass | Tests assert observable execution order, bodies, compensation attempts, marker state, lifecycle, conformance, fault slots/skips, serialization, checksums, and the prior review regression rather than private implementation details alone. |
| Regression risk | pass | Ordinary Saga workflow tests pass; the exact replay suite remains green after simulator installation; first-deviation and retry-marker regressions cover the identified high-risk edges. |
| Security/data safety | pass | No external data or authorization surface was added; package/dynamic-artifact output alias guards and checksum tests protect persisted inputs. |
| Change hygiene | pass | No unrelated source/docs refactor or dependency change was introduced. Unrelated untracked meeting notes were left untouched. |

## Findings

None.

## Done Transition

- Moved to done: `yes`
- Done path: `issues/2026-07-19-compensation-aware-scenario-catalog/done/007-runtime-fallback-and-hard-stops.md`
- Reason if not moved: `n/a`

## Reviewer Notes

The live `current-state.md` and ScenarioExecutor reference still describe the pre-v3 executor. That is intentional under the implementation plan: slice 008 owns full integration evidence and post-validation live-documentation migration, so it is not a slice-007 finding.
