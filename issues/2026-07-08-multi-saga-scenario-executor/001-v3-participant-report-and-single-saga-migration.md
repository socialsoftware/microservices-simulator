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
