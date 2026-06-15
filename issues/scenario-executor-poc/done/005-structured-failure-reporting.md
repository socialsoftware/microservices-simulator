## Parent PRD

`issues/scenario-executor-poc/prd.md`

## Type

AFK

## What to build

Add structured failure and blocker reporting across selection, support validation, materialization, startup/preparation, step-id validation, and runtime step execution. Auto-select mode should skip unsupported candidates and continue. Explicit scenario plan id mode should fail the selected unsupported candidate. Step execution should stop on the first failed step and record the exception details.

## Acceptance criteria

- [x] Missing explicit scenario plan ids produce a selection terminal status with the requested id.
- [x] Unsupported scenario shapes produce structured support blockers in explicit mode and skipped-candidate counts in auto-select mode.
- [x] Materialization blockers include the scenario plan id, input variant id, argument index when available, and stable blocker reason.
- [x] Invalid step ids produce a structured support blocker before execution starts.
- [x] Target preparation/startup failures are distinguishable from selection, materialization, and step-execution failures.
- [x] Runtime step failure records the failed scheduled step, derived step name, exception class, exception message, and terminal status.
- [x] After a step failure, later scheduled steps are not attempted.
- [x] Unsupported candidates never synthesize values for non-whitelisted unresolved arguments.
- [x] Report tests cover each terminal status through public behavior and report output.

## Feature criteria covered

- AC-005
- AC-009
- AC-014
- AC-018
- AC-019
- AC-022
- AC-026

## Domain context

Use PRD terminal-status terminology consistently: preparation/startup, selection, materialization, unsupported-scenario, and step-execution. Use `ScenarioPlan`, input variant, and scheduled step identifiers from the scenario catalog model.

## Verification plan

- Run focused verifier tests for structured failure reports and skipped-candidate counts.
- Suggested command: `cd verifiers && mvn test -Dtest='*ScenarioExecutor*Spec,*ExecutionReport*Spec' -DfailIfNoTests=false`

## Completion evidence

- Added structured terminal statuses and blockers across selection, unsupported scenario validation, materialization, startup, step-id validation, and runtime step execution.
- Auto-select skips unsupported candidates and accumulates skipped-candidate counts; explicit mode reports blockers for the requested unsupported candidate.
- Step execution stops at the first failure and records scheduled step id, catalog step id, derived runtime step name, exception class, exception message, and terminal status.

## Verification

- Verified by `ScenarioExecutorSpec` missing-id, unsupported-shape, invalid-step-id, materialization blocker, startup failure, and step failure tests through public report output.
- Verified command: `cd verifiers && mvn test -Dtest='*ScenarioExecutor*Spec' -DfailIfNoTests=false` on 2026-05-26: 10 tests run, 0 failures, 0 errors.

## Blocked by

- Blocked by `issues/scenario-executor-poc/002-first-fixture-execution.md`
- Blocked by `issues/scenario-executor-poc/003-dto-constructor-materialization.md`
- Blocked by `issues/scenario-executor-poc/004-collections-helper-property-materialization.md`

## User stories addressed

- User story 6
- User story 7
- User story 14
- User story 28
- User story 34
- User story 38
- User story 39
- User story 44
- User story 49
