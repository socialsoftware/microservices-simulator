# Slice Review: 002 - Explicit Multi-Saga Selection and Dry-Run Validation

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes`

## Sources Reviewed

- Slice: `issues/2026-07-08-multi-saga-scenario-executor/done/002-explicit-multi-saga-selection-and-dry-run.md`
- Spec: `issues/2026-07-08-multi-saga-scenario-executor/spec.md`
- Implementation plan: `issues/2026-07-08-multi-saga-scenario-executor/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`
- ADRs: `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md`, `docs/verifiers-impl/decisions/2026-04-28-hybrid-static-dynamic-key-binding.md`, `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md`, `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`
- Completion evidence: `issues/2026-07-08-multi-saga-scenario-executor/done/002-explicit-multi-saga-selection-and-dry-run.md#completion-evidence`
- Changed files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`, `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`, `issues/2026-07-08-multi-saga-scenario-executor/002-explicit-multi-saga-selection-and-dry-run.md`
- Additional repo anchors inspected: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioCatalogReader.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/ScenarioPlan.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/FaultSpace.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java`, dependency review `issues/2026-07-08-multi-saga-scenario-executor/review/001-v3-participant-report-and-single-saga-migration-review.md`
- Prior review reports: None for this slice
- Commands run by reviewer:
  - `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`
  - `git diff --check -- verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy issues/2026-07-08-multi-saga-scenario-executor/002-explicit-multi-saga-selection-and-dry-run.md`

## Summary

The slice satisfies the explicit multi-saga dry-run and validation contract. `ScenarioExecutor` now distinguishes explicit from auto selection for multi-saga plans, validates selected plan participants, input variants, scheduled-step ownership, runtime step-name mapping, fault-space mapping, and vector syntax before runtime work, and builds v3 participant reports for successful dry-runs and selected-plan validation failures. Auto-selection still skips multi-saga plans. The implementation does not enable non-dry-run multi-saga execution and does not write catalog/enrichment inputs. Targeted reviewer verification passed.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | pass | Explicit selection calls `validate(record, options, true)` and reports selected validation failures through `candidateReport(...)` with candidate participants (`ScenarioExecutor.java:50-67`). Dry-run reports are built from normalized candidate steps and participant data before materialization (`ScenarioExecutor.java:102-110`). |
| Slice out-of-scope respected | pass | Non-dry-run multi-saga plans are rejected with `UNSUPPORTED_SCENARIO_SHAPE` before `runCandidate` (`ScenarioExecutor.java:289-292`); no multi-saga materialization/startup/scheduled execution/closure/compensation path was added. |
| Spec non-goals respected | pass | No catalog generation, dynamic enrichment, materializer semantics, Quizzes smoke, distributed/TCC support, search/scoring, or sidecar rewriting was introduced. `ScenarioCatalogReader` remains read-only and `ScenarioExecutor.writeReport` writes only the configured execution report. |
| Dependencies done | pass | Dependency S1 is done: `issues/2026-07-08-multi-saga-scenario-executor/done/001-v3-participant-report-and-single-saga-migration.md`; dependency review verdict was PASS. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-1 | pass | Static multi-saga dry-run test captures and compares `scenario-catalog.jsonl` before/after (`ScenarioExecutorSpec.groovy:477-503`); enriched-catalog preference test compares `scenario-catalog-enriched.jsonl` before/after (`ScenarioExecutorSpec.groovy:30-41`). `ScenarioCatalogReader` unwraps enriched records without writing inputs. | Enriched immutability is covered on the existing enriched path; multi-saga uses the same unwrapped `ScenarioPlan` validation path. |
| AC-2 | pass | Explicit selection matches `options.scenarioId()` to `record.plan().deterministicId()` (`ScenarioExecutor.java:50-59`); test selects `multi-dry-run` and reports `scenarioKind=MULTI_SAGA` (`ScenarioExecutorSpec.groovy:481-487`). | |
| AC-3 | pass | Auto-selection validates with `allowExplicitMultiSaga=false` (`ScenarioExecutor.java:70-76`), which blocks multi-saga records (`ScenarioExecutor.java:289-290`); test proves a multi-saga record is skipped and the single-saga candidate is selected (`ScenarioExecutorSpec.groovy:517-524`). | |
| AC-4 | pass | Missing explicit id returns `SELECTION_FAILED` before validation/materialization (`ScenarioExecutor.java:55-58`); existing missing-id coverage asserts `MISSING_SCENARIO_PLAN_ID` and no selected participant list. | This is no-plan-selected behavior, so participants stay empty. |
| AC-5 | pass | Non-dry-run multi-saga returns `UNSUPPORTED_SCENARIO` with participant entries and no fixture execution (`ScenarioExecutorSpec.groovy:551-564`); invalid selected shapes report structured blockers for missing input, unknown owner, duplicate scheduled ids, invalid step ids, and fault-space failures (`ScenarioExecutorSpec.groovy:506-548`). | |
| AC-6 | pass | Validation finds matching inputs by `SagaInstance.inputVariantId()` and adds `MISSING_INPUT_VARIANT` or `DUPLICATE_INPUT_VARIANT` blockers (`ScenarioExecutor.java:297-308`); tests cover both missing and duplicate cases (`ScenarioExecutorSpec.groovy:506-548`). | |
| AC-10 | pass | Dry-run returns before materialization/runtime and uses provider mode `NONE` (`ScenarioExecutor.java:105-110`); test asserts `NOT_ATTEMPTED` materialization/startup, `NOT_STARTED`, participant-local `DRY_RUN` outcomes, and empty fixture runtime step log (`ScenarioExecutorSpec.groovy:484-503`). | |
| AC-11 | pass | The existing `runtimeStepName` rule still takes text after final `::` and strips one trailing `#<digits>` (`ScenarioExecutor.java:438-443`); multi-saga test normalizes `LeftSaga::run#0` and `RightSaga::other#1` to `run` and `other` (`ScenarioExecutorSpec.groovy:494-501`). | |
| AC-12 | pass | Invalid runtime step ids add `UNSUPPORTED_STEP_ID` before execution (`ScenarioExecutor.java:324-328`); invalid-shape test asserts that blocker and no fixture execution (`ScenarioExecutorSpec.groovy:531-548`). | |
| AC-18 | pass | Default-vector source/value are selected by the shared vector path when no explicit vector is provided (`ScenarioExecutor.java:336-340`, `ScenarioExecutor.java:424-430`); existing dry-run default-vector assertions remain passing (`ScenarioExecutorSpec.groovy:466-469`). | Non-dry-run multi-saga default execution remains out of this slice and is intentionally rejected. |
| AC-19 | pass | Vector validation checks binary syntax, length alignment, zero-length semantics, fault-space length, duplicate fault-space ids, and unresolved/non-unique mapping (`ScenarioExecutor.java:342-384`); malformed-vector and multi-saga wrong-length tests passed. | |
| AC-20 | pass | Fault slots are scenario-level records carrying scheduled step id, saga instance id, runtime step name, bit, and realization state (`ScenarioExecutor.java:386-391`); multi-saga dry-run test asserts `DRY_RUN` for assigned bit and `NOT_ASSIGNED` for unassigned bit across participants (`ScenarioExecutorSpec.groovy:497-501`). | |
| AC-30 | pass | Validation failures and non-dry-run multi-saga stop before materialization/runtime (`ScenarioExecutor.java:59-66`, `ScenarioExecutor.java:289-292`); tests assert the fixture runtime step log remains empty for invalid/non-dry cases (`ScenarioExecutorSpec.groovy:527-528`, `:547-548`, `:563-564`). | |
| AC-40 | pass | `ScenarioExecutorSpec` now includes dummy/synthetic coverage for explicit multi-saga dry-run, no auto-selection, selected-plan validation failure participant reporting, invalid shape/vector blockers, non-dry-run unsupported behavior, and catalog immutability. | Later AC-40 runtime interleaving/fault/closure cases are assigned to later slices. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | pass | Reviewer rerun passed: `Tests run: 38, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| `git diff --check -- <slice files>` | pass | No whitespace/error output. |
| Completion evidence status | pass | Slice contains `Status: implemented-awaiting-review` and records files changed, verification command, AC evidence, deviations, and follow-ups. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Changes are confined to `ScenarioExecutor`, focused `ScenarioExecutorSpec` coverage, and slice evidence. The implementation reuses the existing executor/report model instead of introducing a new materializer or runner path. |
| Existing patterns | pass | Keeps Java records and existing validation/report-construction style. New candidate/participant validation is local to `ScenarioExecutor` and keeps catalog reader/model boundaries unchanged. |
| Test quality | pass | Tests assert externally observable report status, participant state, step/fault-slot mapping, skipped auto-selection behavior, no runtime side effects, and catalog immutability rather than only private implementation details. |
| Regression risk | pass | Focused executor suite passed. Single-saga default/explicit vector and v3 report tests remain in the same suite, reducing risk that S2 regressed S1 behavior. |
| Security/data safety | n/a | No credentials, network calls, migrations, destructive operations, or persistent data changes were introduced. |
| Change hygiene | pass | `git diff --check` passed. Existing unrelated unstaged/deleted/untracked files in the broader worktree were not modified by this review. |

## Findings

None

## Done Transition

- Moved to done: `yes`
- Done path: `issues/2026-07-08-multi-saga-scenario-executor/done/002-explicit-multi-saga-selection-and-dry-run.md`
- Reason if not moved: `None`

## Reviewer Notes

Documentation still describes the old narrow single-saga/v2 executor in places, but the implementation plan explicitly reserves documentation/current-state updates for the final Quizzes smoke/documentation slice, so this is not a S2 blocker.
