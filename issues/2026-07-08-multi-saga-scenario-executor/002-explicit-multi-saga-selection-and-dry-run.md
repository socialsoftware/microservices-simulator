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
