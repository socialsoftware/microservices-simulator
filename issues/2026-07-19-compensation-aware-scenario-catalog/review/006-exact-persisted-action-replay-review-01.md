# Slice Review: 006 - Exact Persisted Action Replay

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`FAIL`

Moved to done: `no`

## Sources Reviewed

- Slice: `issues/2026-07-19-compensation-aware-scenario-catalog/006-exact-persisted-action-replay.md`
- Spec: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Implementation plan: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`
- ADRs: `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`, `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`, `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`, `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md`
- Completion evidence: `006-exact-persisted-action-replay.md` — `## Completion Evidence` (`Status: implemented-awaiting-review`)
- Changed files reviewed: `docker-compose.yml`; `verifiers/scripts/run-scenario-executor.sh`; executor source under `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/FaultScenarioValidator.java`; executor/writer tests under `verifiers/src/test/`; simulator workflow/unit-of-work source under `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/`; and the new simulator tests under `simulator/src/test/`
- Prior review reports: `None` for slice 006
- Commands run by reviewer:
  - `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test`
  - `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test`
  - `git diff --check`
  - `bash -n verifiers/scripts/run-scenario-executor.sh && test -x verifiers/scripts/run-scenario-executor.sh`
  - `docker compose config --quiet`
  - production-reference `rg --pcre2` inspection for the removed v2 reader/catalog and execution-time vector surfaces
  - compiled and ran `/tmp/ExactStepProbe.java` against simulator classes to test a valid branched Saga one action at a time
  - compiled and ran `/tmp/OutputAliasProbe.java` against verifier classes to test `outputPath == package manifest path`

## Summary

The persisted-id migration, pure action validation, preparation gate, pre-body abort control, stepwise recovery, report v4 shape, CLI/script/Compose migration, and required focused suites are present and green. The slice still fails its central exactness and immutability contracts.

First, normal forward replay calls the ordinary `executeUntilStep`, which executes every unexecuted runtime-plan step preceding the target. Runtime Saga ordering among simultaneously ready branches is derived from `HashMap` iteration and need not equal the persisted WorkloadPlan order. A reviewer probe observed a request for `second` execute `third` first (`calls=[first, third, second]`). The executor would nevertheless emit one successful outcome for `second` and can later claim `EXACT`; an assigned fault for the already-run `third` body would also be reported as pre-body. Second, the caller-controlled report output is not checked against package input paths. A dry run with the manifest as `outputPath` replaced the v3 manifest with a v4 execution report.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | fail | Exact persisted forward action replay and package immutability are both violated; see blocking findings. |
| Slice out-of-scope respected | pass | Full zero-bit fallback remains deferred to S7; no on-demand mutation, v2 compatibility, or unrelated feature was added. |
| Spec non-goals respected | pass | No TCC/distributed parity, compensation faults/retries, delayed commit, search/scoring, or new dependency was introduced. |
| Dependencies done | pass | Required dependency `004-materializable-eager-baseline-and-accounting.md` exists under `issues/2026-07-19-compensation-aware-scenario-catalog/done/`; S2 and S3 are also done. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-2 | pass | `ScenarioCatalogReader` uses the v3 package reader; v2 rejection test passes; production inspection found no legacy executor reader. | Clear v2 diagnostic is present. |
| AC-9 | fail | The linear all-zero fixture passes, but `executeUntilStep` can complete a later persisted forward body during an earlier action in a valid branched Saga. | The executor can mark the complete list while action/body completion is not one-to-one. |
| AC-14 | fail | Commit is invoked at the reported final action position, but that final body may already have run as an unreported side effect of an earlier `executeUntilStep`. | This is not immediate commit after the actual final-body completion. |
| AC-25 | pass | Java/CLI/script/Compose select a persisted FaultScenario id only; package reader plus `WorkloadPlanValidator`/`FaultScenarioValidator` reject malformed ownership, identities, enablement, reverse recovery, and residual forward order before materialization. | Production vector-overlay surface is removed. |
| AC-26 | pass | Materialization and startup failure specs retain zero actual actions and absent conformance; startup begins only after all participants materialize. | Required suite passed. |
| AC-27 | fail | `ScenarioExecutor.java:239` delegates to `ExecutionPlan.executeUntilStep`; `ExecutionPlan.java:391-403` executes all preceding unexecuted runtime-plan steps. Reviewer probe observed `third` execute before requested `second`. | Planned/actual report-list equality does not prove runtime body order. |
| AC-28 | fail | The new direct pre-body abort control works in the linear test, but a later assigned target can already have executed during an earlier branched forward action. | Body-not-called and target-absent guarantees do not hold for every valid Saga shape. |
| AC-29 | pass | Each persisted compensation action is unique after validation and invokes `recoverStepForExecutor` once; simulator tests prove one requested checkpoint and retry markers. | No compensation fault slots were added. |
| AC-30 | fail | Body/commit fields and controlled commit result exist, but the branch leak can make `bodyOutcome=SUCCEEDED` without that body running in the current action and can separate actual final-body completion from commit. | Assigned-final behavior is only proven for a linear fixture. |
| AC-36 | pass | First-step assigned failure reports `ABORTED`, `NO_COMPENSATION_WORK`, then `COMPENSATED`; terminal guards prevent planned actions from targeting terminal participants. | Focused no-work test passes. |
| AC-37 | pass | Report v4 contains attempt/workload/FaultScenario/package-path identity, vector slots, planned/actual actions, lifecycle events, participants/final states, blockers, and terminal status. | Serialization assertions pass. |
| AC-38 | fail | The executor unconditionally emits `EXACT` after iterating the persisted list, even when the runtime executed extra bodies during an earlier action. | Conformance is not truthful for branched Sagas. |
| AC-39 | fail | The schema contains the required action and recovery fields, but forward body outcomes can be attributed to the wrong action position under the exact-step defect. | Structural presence is insufficient when runtime truth is wrong. |
| AC-40 | pass | Assigned replay covers `NOT_ASSIGNED`, `REALIZED`, `MASKED`, and skipped owner forwards; untouched assigned slots remain `NOT_REACHED` on a hard stop. | Linear state matrix passes. |
| AC-41 | fail | Abort/commit/compensated/no-work events exist, but final forward body/commit distinction can be false when the final body ran during an earlier action. | Same exact-step blocker. |
| AC-42 | fail | `ScenarioExecutor.java:605-610` writes directly to arbitrary `outputPath`. Reviewer dry-run probe changed the manifest SHA-256 from `0a88c294...` to `4c7aeee6...` and replaced its schema with `microservices-simulator.scenario-execution-report.v4`. | Package bytes are not protected from output aliasing. |
| AC-44 | fail | Focused suites pass, but coverage is linear-only for exact replay and does not test a branched Saga or report-output aliasing against package artifacts. | The missing cases expose both blockers. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` | pass | Reviewer rerun: `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test` | pass | Reviewer rerun: `Tests run: 23, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| Production-reference inspection | pass | No production `CatalogScenarioRecord`, `LegacyScenarioPlan`, v2 schema, exact old `scenario-catalog.jsonl`/enriched executor path, `--fault-vector`, `CATALOG_PATH`, or old executor selector remains; on-demand generation’s vector option is correctly outside the executor surface. |
| Script/Compose validation | pass | Script syntax/executable check and `docker compose config --quiet` passed. |
| `git diff --check` | pass | No whitespace errors. |
| Branched exact-step probe | fail | Calling persisted-style targets `first` then `second` on a Saga with ready sibling `third` produced `calls=[first, third, second]` and `executedSteps=[first, third, second]`. |
| Output/package alias probe | fail | Dry-run report output set to the manifest path replaced the package manifest; before/after checksums differed and the file became report schema v4. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Changes stay within the executor, narrow simulator controls, focused fixtures, and invocation surfaces required by S6. |
| Existing patterns | fail | Reusing ordinary `executeUntilStep` is incompatible with one-persisted-action-at-a-time replay because that API deliberately executes a prefix. |
| Test quality | fail | Existing assertions are meaningful for linear workflows, validation, lifecycle, and serialization, but omit branched exact-step isolation and output/input path aliasing. |
| Regression risk | fail | Extra runtime body execution can mutate application state, register unplanned checkpoints, invalidate assigned pre-body semantics, and produce false `EXACT` reports. |
| Security/data safety | fail | A valid CLI/API output path can overwrite canonical package artifacts, including through path aliases unless canonical/same-file checks are added. |
| Change hygiene | pass | Required production migration is surgical, unrelated untracked meeting-note files were not touched, and diff/shell/Compose checks pass. |

## Findings

| Severity | Finding | Evidence | Required Fix |
|----------|---------|----------|--------------|
| blocking | Forward replay is not one persisted action at a time. `executeUntilStep` may execute ready sibling steps before the requested target, so bodies/checkpoints can run ahead of the catalog while the report still claims ordered outcomes and `EXACT`. | `verifiers/.../ScenarioExecutor.java:237-240`; `simulator/.../ExecutionPlan.java:391-403`; `SagaWorkflow.java:21-56` derives ready order through `HashMap`. `/tmp/ExactStepProbe.java` observed `calls=[first, third, second]` after requesting `first`, then `second`. | Add a narrow executor-only forward-step operation that executes exactly the named next runtime step (and rejects unmet dependencies/already-invalid sequencing) without executing unrelated ready siblings. Wire S6 replay to it while preserving ordinary `executeUntilStep` behavior. Add simulator and executor coverage with a branched Saga proving one body/checkpoint per persisted action, truthful action positions, and an assigned later target that has not already run. |
| blocking | Report output can alias and overwrite a package input, violating the byte-immutability contract even on dry-run. | `ScenarioExecutor.java:57` reads package paths, then `ScenarioExecutor.java:605-610` writes without collision validation. `/tmp/OutputAliasProbe.java` used the manifest as output: SHA-256 changed `0a88c294... -> 4c7aeee6...`, and the manifest became report schema v4. | Before materialization or measured execution, reject an output path that aliases the manifest or any linked package/accounting/diagnostic/enrichment input, including normalized/symlink aliases (`Files.isSameFile` where applicable). Add dry-run and measured-execution regressions proving every protected artifact remains byte-identical and no action starts on collision. |

## Done Transition

- Moved to done: `no`
- Done path: `None`
- Reason if not moved: `FAIL — exact single-action runtime replay and package-output immutability still have blocking defects.`

## Reviewer Notes

A preliminary Maven invocation from the repository root failed because this repository has no root aggregator; the required commands were rerun from `simulator/` and `verifiers/` and passed. The two unrelated untracked meeting-note files visible in the worktree were excluded from this review and left unchanged.
