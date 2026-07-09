# 001 - v3 Participant Report and Single-Saga Migration

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `None`  
ACs covered: `AC-35, AC-36, AC-37, AC-38, AC-39, AC-44`  
Risk: `high`

## Purpose

Make the v3 participants report model the only ScenarioExecutor report shape before adding multi-saga execution. This keeps later slices from carrying v2 compatibility fields or the v2 `FAULT_COMPENSATED` terminal alias.

## Scope

- Change `ScenarioExecutionReport` to schema version `microservices-simulator.scenario-execution-report.v3`.
- Move saga-specific facts under a required `participants` list for every selected single-saga attempt.
- Keep scenario-level facts top-level: execution id, plan id, scenario kind, terminal status, assigned vector, vector source, provider mode, runtime metadata, fault slots, skipped-candidate counts, and scenario-level blockers.
- Add participant fields for saga instance id, saga FQN, input variant id, materialization state, startup state, lifecycle outcome, participant step outcomes, skipped steps, and participant-local blockers/failure details.
- Migrate current single-saga success, dry-run, materialization failure, startup failure, assigned-fault, expected-not-injected, provider mismatch, runtime failure, and compensation-failure reports to the v3 structure.
- Replace v2 `FAULT_COMPENSATED` in v3 with `COMPENSATED` for single-saga compensated outcomes.
- Keep current single-saga non-assigned scheduled-step runtime-failure mechanics only as a transitional S1 behavior; S6 must migrate that case to the spec's one-participant participant-outcome aggregation.
- Update `ScenarioExecutorCli.exitCodeFor` for v3 zero statuses: `SUCCESS`, `COMPENSATED`, `PARTIAL_COMPENSATED`, and `DRY_RUN`; all hard-stop statuses and `COMPENSATION_FAILED` are non-zero.
- Update CLI output/report summaries so they read participant step outcomes or only top-level v3 summary fields, not removed top-level flat `stepOutcomes`.

## Out of Scope

- Multi-saga selection, dry-run, materialization, or execution.
- New fault behavior beyond preserving current single-saga mechanics through the v3 report shape.
- Backward-compatible v2 fields, aliases, or dual-schema output.
- Documentation updates; those are reserved for the final docs/smoke slice.

## Repo Anchors

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java` — report record to evolve to v3.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — report construction and current single-saga statuses.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorCli.java` — exit-code helper and CLI output.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — primary report-shape and single-saga behavior coverage.
- `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md` — accepted report-model decision.

## Implementation Shape

- Keep the report as a simple Jackson-serializable Java record or small set of nested records; avoid a second DTO layer.
- Introduce exact v3 vocabulary as string constants or a small internal vocabulary helper only if it reduces repeated typo-prone literals.
- Preserve current `ScenarioExecutor.execute(...)` external API.
- Represent validation/selection failures with an empty participant list when no plan is selected; once a plan is selected, report one participant for the single saga.
- Do not keep top-level v2 fields: `lifecycleOutcome`, `sagaInstanceId`, `sagaFqn`, `inputVariantId`, or flat `stepOutcomes`.
- Do not leave any `ScenarioExecutorCli` code path depending on removed top-level flat `stepOutcomes`; use participant-local step outcomes if step details are still printed.
- For current single-saga scheduled-step runtime exceptions, preserve current mechanics only inside S1; S6 owns the final v3 behavior where one-participant scheduled-step domain failures aggregate like participant outcomes.

## TDD / Test Shape

- First behavior to test: an existing successful single-saga execution writes v3 schema with exactly one participant and no v2 top-level saga identity fields.
- Expected red failure: tests currently see v2 schema/top-level fields and `FAULT_COMPENSATED`.
- Additional coverage:
  - single-saga dry-run has one participant with `NOT_ATTEMPTED` materialization/startup and participant step outcomes marked `DRY_RUN`;
  - assigned single-saga fault reports top-level `COMPENSATED` and participant lifecycle `COMPENSATED`;
  - JSON tree assertions prove `sagaInstanceId`, `sagaFqn`, `inputVariantId`, top-level `lifecycleOutcome`, top-level `stepOutcomes`, and `FAULT_COMPENSATED` are absent from v3 output;
  - CLI output no longer reads top-level flat `stepOutcomes` and either prints participant-local step outcomes or only top-level v3 summary fields;
  - CLI exit-code helper covers all v3 zero statuses and representative non-zero statuses.

## Just-in-Time Preflight Required

Before source edits, the executor must re-check:

- Current fields and helper methods in `ScenarioExecutionReport.java` and all constructor call sites in `ScenarioExecutor.java`.
- Current expectations in `ScenarioExecutorSpec.groovy` that assert v2 schema, top-level saga fields, top-level step outcomes, or `FAULT_COMPENSATED`.
- Current `ScenarioExecutorCli` references to removed flat `stepOutcomes`.
- Any references to `FAULT_COMPENSATED` under `verifiers/src/main`, `verifiers/src/test`, `docs/verifiers-impl`, and `verifiers/scripts`.

## Verification

- `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` — expected to pass after report-shape migration and to compile CLI code against the v3 report API.
- `cd verifiers && mvn -Dtest=ScenarioExecutorOrchestratorSpec test` — run if CLI/orchestrator-visible command behavior is touched beyond `exitCodeFor`.

## Evidence to Record

- files changed
- commands run and outputs
- report JSON excerpts showing schema version and participant list
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- This is intentionally breaking for v2 report assertions. Do not add compatibility fields to make old tests pass.
- Keep single-saga execution behavior stable while moving facts into the participant entry.
- Avoid broad report abstractions; the v3 report is still a local verifier artifact.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Migrated `ScenarioExecutionReport` from the v2 flat single-saga shape to canonical v3 with top-level scenario facts and required `participants` entries.
- Moved single-saga saga identity, materialization/startup state, lifecycle outcome, step outcomes, skipped steps, and participant blockers under the one participant entry.
- Replaced the v2 `FAULT_COMPENSATED` terminal alias with `COMPENSATED` for assigned single-saga faults, and migrated fault-slot states to v3 vocabulary (`DRY_RUN`, `NOT_REACHED`, `REALIZED`, `MASKED_BY_SAGA_FAILURE`, `EXPECTED_FAULT_NOT_INJECTED`, `NOT_ASSIGNED`).
- Updated CLI step printing to read participant-local step outcomes and updated exit-code mapping for v3 zero statuses.
- Updated focused executor coverage to assert v3 schema, one-participant single-saga reports, removed top-level v2 fields, participant state vocabulary, participant-local step outcomes, and v3 exit-code behavior.

### Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java` — changed report schema version to v3, removed v2 top-level single-saga fields, and added participant/skipped-step records.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — builds v3 one-participant reports for selected single-saga attempts, emits `COMPENSATED`, and uses v3 fault-slot realization vocabulary.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorCli.java` — prints participant-local step outcomes and treats `SUCCESS`, `COMPENSATED`, `PARTIAL_COMPENSATED`, and `DRY_RUN` as zero-exit statuses.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — migrated assertions from v2 flat report fields to v3 participants and added JSON-shape checks for absent top-level compatibility fields.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | PASS | `Tests run: 34, Failures: 0, Errors: 0, Skipped: 0`; compiles executor and CLI against the v3 report API. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorOrchestratorSpec test` | PASS | `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`; run because CLI-visible output/exit behavior was touched. |
| Source/test grep | PASS | `rg` over executor main/test/scripts has no production `FAULT_COMPENSATED`, top-level `report.stepOutcomes()`, or top-level report saga identity access; only tests assert the old alias is non-zero/absent. |

### Acceptance Criteria Evidence

- AC-35: `ScenarioExecutionReport.SCHEMA_VERSION` is now `microservices-simulator.scenario-execution-report.v3`; focused tests assert this schema in generated JSON.
- AC-36: Selected single-saga reports now contain exactly one participant with `sagaInstanceId`, `sagaFqn`, `inputVariantId`, `materializationState`, `startupState`, `lifecycleOutcome`, participant-local `stepOutcomes`, `skippedSteps`, and participant blockers.
- AC-37: Scenario-level facts remain top-level: execution id, plan id, scenario kind, terminal status, assigned vector/source, provider mode, runtime metadata, fault slots, skipped-candidate counts, and scenario-level blockers.
- AC-38: JSON assertions prove top-level `sagaInstanceId`, `sagaFqn`, `inputVariantId`, `lifecycleOutcome`, and flat `stepOutcomes` are absent; assigned-fault terminal status is `COMPENSATED`, and tests assert `FAULT_COMPENSATED` is not emitted.
- AC-39: Existing single-saga dry-run, success, materialization failure, startup failure, assigned-fault, expected-not-injected, provider mismatch, runtime failure, and compensation-failure paths remain covered through the v3 one-participant model.
- AC-44: `ScenarioExecutorCli.exitCodeFor` returns zero for `SUCCESS`, `COMPENSATED`, `PARTIAL_COMPENSATED`, and `DRY_RUN`; representative hard-stop statuses and `COMPENSATION_FAILED` return non-zero.

Report JSON excerpt asserted by tests:

```json
{
  "schemaVersion": "microservices-simulator.scenario-execution-report.v3",
  "terminalStatus": "SUCCESS",
  "participants": [
    {
      "sagaInstanceId": "fixture-plan-saga",
      "sagaFqn": "pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.FixtureWorkflow",
      "inputVariantId": "fixture-plan-input",
      "materializationState": "MATERIALIZED",
      "startupState": "STARTUP_READY",
      "lifecycleOutcome": "COMMITTED",
      "stepOutcomes": [
        { "runtimeStepName": "first", "status": "COMPLETED" },
        { "runtimeStepName": "second", "status": "COMPLETED" }
      ]
    }
  ]
}
```

### Browser / Manual Evidence

- Not required.

### TDD Notes

- TDD was partially practical: the slice started from existing v2 assertions, which failed/needed migration by construction once the v3 record removed flat accessors. The focused executor spec was updated to assert v3 behavior and then driven to pass with the implementation.

### Deviations From Plan

- None.

### Blockers / Follow-Ups

- None.
