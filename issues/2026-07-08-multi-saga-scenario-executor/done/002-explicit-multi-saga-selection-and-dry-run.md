# 002 - Explicit Multi-Saga Selection and Dry-Run Validation

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `001-v3-participant-report-and-single-saga-migration.md`  
ACs covered: `AC-1, AC-2, AC-3, AC-4, AC-5, AC-6, AC-10, AC-11, AC-12, AC-18, AC-19, AC-20, AC-30, AC-40`  
Risk: `high`

## Purpose

Teach the executor to recognize an explicitly selected multi-saga `ScenarioPlan` and validate its structural execution contract without runtime side effects. This proves catalog/static-enrichment loading, participant mapping, step mapping, and fault-vector mapping before adding non-dry-run execution.

## Scope

- Allow explicit `--scenario-id` selection of `ScenarioKind.MULTI_SAGA` plans by `ScenarioPlan.deterministicId`.
- Keep auto-selection limited to supported single-saga default-vector candidates; multi-saga records are skipped/counted when no explicit scenario id is supplied.
- Validate selected multi-saga shape before materialization:
  - at least one selected plan and scenario id match;
  - every scheduled step references a known `SagaInstance`;
  - every `SagaInstance` has exactly one matching `InputVariant` by input variant id;
  - runtime step names are derived with the existing final-`::` plus trailing-`#<digits>` removal rule;
  - fault-space length, unique scheduled step ids, scheduled-step mapping, and vector syntax/length are valid.
- Produce v3 dry-run reports for explicit multi-saga plans with one participant per saga instance.
- For any explicitly selected plan that fails structural/vector validation after the plan is found, still report one participant entry per known `SagaInstance`; leave the participant list empty only when no scenario plan was selected.
- Mark dry-run participant states as not materialized/not started and scheduled step outcomes as `DRY_RUN` under the owning participant.
- Mark assigned dry-run fault slots as `DRY_RUN`; unassigned slots remain `NOT_ASSIGNED`.
- Preserve static/enriched catalog files unchanged.

## Out of Scope

- Non-dry-run multi-saga materialization, startup, scheduled execution, closure, compensation, or provider installation.
- Multi-saga auto-selection or prioritization.
- New materializer semantics.
- Quizzes Docker smoke.

## Repo Anchors

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — selection, validation, dry-run report construction.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioCatalogReader.java` — static/enriched catalog loading boundary.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/ScenarioPlan.java` — kind, saga instances, inputs, schedule, fault space.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/ScheduledStep.java` — scheduled step owner and deterministic id.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — add dry-run and unsupported-shape coverage.

## Implementation Shape

- Reuse the existing `runtimeStepName(String stepId)` rule; do not introduce a new naming convention.
- Prefer a plan-level validation result that contains participants, normalized runtime steps, assigned vector, fault slots, and blockers. It should work for single-saga and multi-saga plans, including unsupported selected plans that still have known saga instances.
- Treat explicit multi-saga non-dry-run as unsupported until later slices add runtime support. This slice should not fake execution.
- Keep blockers deterministic by iterating saga instances, inputs, scheduled steps, and fault slots in stable catalog order or sorted order where the existing executor already sorts.
- Do not mutate loaded `ScenarioPlan` records to add execution facts.

## TDD / Test Shape

- First behavior to test: explicit multi-saga dry-run with two participants validates and writes a v3 report without materialization or execution.
- Expected red failure: current validation rejects every multi-saga plan as `UNSUPPORTED_SCENARIO_SHAPE`.
- Additional coverage:
  - no `--scenario-id` does not auto-select a multi-saga plan;
  - explicit missing scenario id returns `SELECTION_FAILED` before materialization with an empty participant list because no plan was selected;
  - explicit fault vector without `--scenario-id` returns `INVALID_FAULT_VECTOR` before materialization/runtime and uses the v3 invalid-vector report shape;
  - missing input, duplicate matching input, unknown scheduled-step owner, invalid step id, duplicate fault-space id, unresolved fault-space id, non-binary vector, and wrong-length vector all fail before runtime;
  - selected-plan validation failures include participant entries for known saga instances, even when terminal status is `UNSUPPORTED_SCENARIO` or `INVALID_FAULT_VECTOR`;
  - static and enriched catalog temp files are byte-identical before/after dry-run;
  - dry-run report has required top-level facts, required participant list, participant-local step outcomes, and scenario-level fault slots.

## Just-in-Time Preflight Required

Before source edits, the executor must re-check:

- Current `validate(...)`, `validateVector(...)`, `autoSelect(...)`, and `explicit(...)` methods in `ScenarioExecutor.java`.
- Current `multiPlan(...)` helper and existing unsupported-shape tests in `ScenarioExecutorSpec.groovy`.
- Current `ScenarioCatalogReader` behavior for run-directory preference of `scenario-catalog-enriched.jsonl` over `scenario-catalog.jsonl`.

## Verification

- `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` — expected to pass with new explicit multi-saga dry-run and validation cases.

## Evidence to Record

- files changed
- commands run and outputs
- dry-run and selected-plan validation-failure report excerpts with participants and fault slots
- before/after catalog immutability evidence
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- Do not accidentally enable multi-saga auto-selection while broadening validation.
- Distinguish invalid vector statuses from unsupported-shape statuses as the current executor does.
- Dry-run must not call materializer, runtime-owned argument resolution, `Class.forName` startup, provider installation, workflow methods, closure, or compensation.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Extended `ScenarioExecutor` validation so explicit dry-run selection can accept `ScenarioKind.MULTI_SAGA` plans while auto-selection still skips multi-saga records.
- Added plan-level participant validation and reporting for selected plans: one participant per known `SagaInstance`, exact input-variant matching, known scheduled-step owners, duplicate scheduled-step id detection, runtime step-name normalization, and existing fault-vector/fault-space validation.
- Added candidate-based v3 report construction so explicit multi-saga dry-run reports participant-local `DRY_RUN` step outcomes and scenario-level fault slots, while selected validation failures still include known participants.
- Kept explicit multi-saga non-dry-run unsupported before materialization/runtime execution.

### Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — multi-saga-aware explicit validation, auto-selection skip behavior, participant-aware report construction for dry-run/validation failures.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — explicit multi-saga dry-run, no-auto-selection, selected-plan failure participant reporting, invalid multi-saga validation, non-dry-run unsupported, and catalog immutability coverage.
- `issues/2026-07-08-multi-saga-scenario-executor/002-explicit-multi-saga-selection-and-dry-run.md` — completion evidence.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | PASS | Maven reported `Tests run: 38, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`. |

### Acceptance Criteria Evidence

- AC-1: Static multi-saga dry-run test compares `scenario-catalog.jsonl` before/after; enriched catalog preference test now compares `scenario-catalog-enriched.jsonl` before/after.
- AC-2: `explicit multi-saga dry run validates participants steps vector slots and preserves catalog` selects `multi-dry-run` by deterministic id and reports `scenarioKind=MULTI_SAGA`.
- AC-3: `auto selection skips multi-saga candidates...` proves auto-selection skips/counts a multi-saga record and selects the valid single-saga record.
- AC-4: Existing explicit missing-id coverage still returns `SELECTION_FAILED` before runtime with no selected participant list.
- AC-5: Explicit multi-saga non-dry-run returns `UNSUPPORTED_SCENARIO` before materialization; selected invalid multi-saga shapes return structured blockers before runtime.
- AC-6: Missing input and duplicate matching input variants are covered by `MISSING_INPUT_VARIANT` and `DUPLICATE_INPUT_VARIANT` assertions.
- AC-10: Explicit multi-saga dry-run asserts `providerMode=NONE`, participant materialization/startup `NOT_ATTEMPTED`, lifecycle `NOT_STARTED`, `DRY_RUN` step outcomes, and no fixture runtime steps.
- AC-11: Multi-saga dry-run asserts final-`::` plus trailing-`#<digits>` normalization to `run` and `other`.
- AC-12: Invalid step id coverage asserts `UNSUPPORTED_STEP_ID` before runtime.
- AC-18: Existing dry-run default-vector coverage remains passing; explicit multi-saga dry-run records explicit vector source while auto/default behavior remains isolated.
- AC-19: Existing malformed-vector table remains passing; new multi-saga invalid test asserts wrong-length explicit vector on a selected multi-saga plan.
- AC-20: Explicit multi-saga dry-run asserts scenario-level fault slots with assigned `DRY_RUN` and unassigned `NOT_ASSIGNED` states across participants.
- AC-30: Multi-saga non-dry-run and validation failures stop before materialization/runtime; fixture step log remains empty.
- AC-40: `ScenarioExecutorSpec` now includes dummy/synthetic coverage for explicit multi-saga dry-run and validation behavior required by this slice.

### Browser / Manual Evidence

- Not required.

### TDD Notes

- Added focused Spock coverage for explicit multi-saga dry-run and validation behavior before implementation. The expected pre-change behavior was rejection as `UNSUPPORTED_SCENARIO_SHAPE`; final verification passes after implementation.

### Deviations From Plan

- None.

### Blockers / Follow-Ups

- None for this slice. Later slices still own non-dry-run multi-saga materialization/startup and runtime execution.
