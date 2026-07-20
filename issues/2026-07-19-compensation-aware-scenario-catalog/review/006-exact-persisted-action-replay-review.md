# Slice Review: 006 - Exact Persisted Action Replay

## Review Attempt

Attempt: `03`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes`

## Sources Reviewed

- Slice: `issues/2026-07-19-compensation-aware-scenario-catalog/006-exact-persisted-action-replay.md`
- Spec: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Implementation plan: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`, `issues/2026-07-19-compensation-aware-scenario-catalog/decision-frame.md`
- ADRs: `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`, `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`, `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md`, `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`
- Completion evidence: `006-exact-persisted-action-replay.md` — `## Completion Evidence` (`Status: implemented-awaiting-review`), including attempt-01 and attempt-02 fix evidence
- Changed files reviewed: `docker-compose.yml`; `verifiers/scripts/run-scenario-executor.sh`; executor source under `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/FaultScenarioValidator.java`; executor/writer tests under `verifiers/src/test/`; simulator workflow/unit-of-work source under `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/`; new simulator tests under `simulator/src/test/`
- Prior review reports: `issues/2026-07-19-compensation-aware-scenario-catalog/review/006-exact-persisted-action-replay-review-01.md`, `issues/2026-07-19-compensation-aware-scenario-catalog/review/006-exact-persisted-action-replay-review-02.md`
- Commands run by reviewer:
  - `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test`
  - `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec,EnrichedScenarioCatalogWriterSpec test`
  - `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec,RecoveryScheduleGeneratorSpec,ScenarioCatalogJsonlWriterSpec,OnDemandFaultScenarioServiceSpec test`
  - `bash -n verifiers/scripts/run-scenario-executor.sh && test -x verifiers/scripts/run-scenario-executor.sh`
  - `docker compose config --quiet`
  - `git diff --check`
  - repository-wide production-reference `rg --pcre2` checks for the removed legacy executor classes, v2 schema/catalog names, enriched-catalog execution path, direct vector option/environment, and old catalog environment
  - compiled and ran `/tmp/ParticipantOrderProbe.java` as an exploratory runtime/catalog incompatibility probe
  - two preliminary Maven invocations from the repository root, which failed because this repository has no root aggregator; the correctly scoped module commands above were then run successfully

## Summary

The slice now satisfies its persisted-action replay contract. The executor loads only a linked v3 package, selects one persisted FaultScenario id, validates package/action semantics before preparation, gates all participants before measured actions, and replays exactly one named runtime body per persisted forward action. Assigned faults abort before the target body or executed-step registration, persisted compensations advance one source checkpoint at a time, and each surviving participant commits at its final successful workload forward action through a controlled body/commit boundary.

The action-aware v4 report records stable scenario identity separately from attempt identity, planned and actual positions, slot states, body/commit outcomes, lifecycle events, compensation evidence and runtime sub-outcomes, participant final state, blockers, terminal status, and truthful `EXACT`, `INCOMPLETE`, or absent conformance for S6 behavior. The attempt-02 immutability blocker is fixed: report output is rejected before selection/preparation when it aliases a linked package artifact or an existing artifact recognized by any authoritative v3 dynamic-enrichment schema, including custom-location normalized, symbolic, and hard-link aliases. Required and supplemental suites pass, and no blocking finding remains.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | pass | V3 persisted-id selection, pure schedule validation, preparation gate, exact forward/compensation replay, pre-body abort, controlled commit, action-aware reporting, and package/enrichment immutability are implemented and verified. |
| Slice out-of-scope respected | pass | Completed zero-bit fallback and detailed compensation/infrastructure failure classification remain in S7; no on-demand mutation, v2 compatibility, retry loop, or report-v3 compatibility layer was added. |
| Spec non-goals respected | pass | No TCC/distributed/stream/gRPC parity, true concurrency, compensation faults/retries, delayed commit, search/scoring, reset/seeding, or new dependency was introduced. |
| Dependencies done | pass | Required dependency `004-materializable-eager-baseline-and-accounting.md` is under `done/`; prerequisite S2 and S3 slices are also done. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-2 | pass | `ScenarioCatalogReader` delegates only to `ScenarioCatalogPackageReader`; v2 input receives the explicit unsupported-v2 diagnostic; production inspection found no legacy executor model/schema path. | No v2 read/upgrade/replay fallback remains. |
| AC-9 | pass | All-zero coverage replays the persisted forward list, emits no compensation actions, commits each participant at its own final forward action, and reports `SUCCESS / EXACT`. | Runtime body order and action positions are asserted. |
| AC-14 | pass | Final successful workload forwards call controlled finalization immediately; `AUTOMATIC_COMMIT` is emitted only after commit succeeds, with one commit per survivor. | Commit is not a schedulable action. |
| AC-25 | pass | Java options, CLI, orchestrator, script, and Compose require package plus persisted FaultScenario id and expose no vector overlay. Workload/package validation plus `FaultScenarioValidator` reject malformed references/ownership, duplicate action ids, premature or reversed compensation, incomplete recovery, and residual forward reorder before preparation. | Required and supplemental package/generator suites pass. |
| AC-26 | pass | Materialization and startup failure tests retain zero actual actions and absent conformance; all materialization attempts precede startup, and all startup attempts precede replay. | Fixture constructor/body assertions confirm no measured action leaks. |
| AC-27 | pass | `ExecutionPlan.executeStepForExecutor` executes only the named dependency-ready step and rejects unmet or already-attempted targets. Branched simulator and verifier tests prove planned ids/positions equal actual ids/positions without ready-sibling leakage. | Attempt-01 exactness blocker is fixed. |
| AC-28 | pass | Assigned targets use `abortBeforeStepForExecutor`; tests prove target body absence, target absence from `SagaUnitOfWork.executedSteps`, prior-checkpoint recoverability, and persisted continuation order. | A later assigned branch is not run early. |
| AC-29 | pass | Every unique persisted compensation action calls `recoverStepForExecutor` once; simulator coverage proves one requested checkpoint and explicit-before-implicit sub-outcome order. | Compensation actions have no fault slots. |
| AC-30 | pass | Reports distinguish body and commit phases; successful final forwards commit, controlled commit failure preserves body success and unconsumed checkpoints, and assigned final faults run neither body nor commit. | Ordinary `resumeWorkflow` behavior remains unchanged. |
| AC-36 | pass | First-step failure emits `ABORTED`, `NO_COMPENSATION_WORK`, then `COMPENSATED` with zero compensation outcomes; terminal participant guards and invocation counts prevent double close/recovery. | No-work lifecycle is explicit. |
| AC-37 | pass | Report v4 contains package/workload/FaultScenario/attempt identity, vector and slots, planned/actual actions, lifecycle events, participants/final states, blockers, and terminal status. | Execution attempt id is independently generated. |
| AC-38 | pass | Complete all-zero and assigned replay report `EXACT`; dry-run/materialization/startup failures omit conformance; measured controlled hard stops report `INCOMPLETE`. | Completed `DEVIATED` fallback remains S7 scope. |
| AC-39 | pass | Forward and compensation outcomes carry owner/source ids, planned/actual positions, status, body/commit/fault fields, compensation evidence, ordered runtime recovery sub-outcomes, and exception details where applicable. | Exact-step isolation makes action attribution truthful. |
| AC-40 | pass | Slot state construction and assigned replay distinguish `NOT_ASSIGNED`, `NOT_REACHED`, `REALIZED`, and `MASKED`, while participant-local omitted forwards remain explicit skipped diagnostics. | The assigned interleaving asserts realized/masked/unassigned states and skipped owner suffixes. |
| AC-41 | pass | Reports emit abort, automatic commit, compensated, and no-work events, with separate successful-body and commit-success/failure outcomes. | Event sequence and owning action ids are asserted. |
| AC-42 | pass | Package checksums remain unchanged in dry-run and measured replay. Output collision tests protect every linked package artifact and all three authoritative v3 dynamic-enrichment artifacts at custom locations and through normalized, symbolic, and hard-link aliases before startup/actions. | Attempt-02 dynamic-artifact blocker is fixed. |
| AC-44 | pass | Focused simulator, executor, package, dynamic-writer, generator, and on-demand regressions prove this slice's exact replay, lifecycle, validation, report, and immutability behavior; prior dummyapp-first generation slices remain green through the shared package contracts. | Quizzes evidence remains correctly deferred to S8. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` | pass | Reviewer rerun: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec,EnrichedScenarioCatalogWriterSpec test` | pass | Reviewer rerun: `Tests run: 49, Failures: 0, Errors: 0, Skipped: 0` (`44` executor, `3` orchestrator, `2` dynamic writer); Maven `BUILD SUCCESS`. |
| Supplemental executor/generator/package/on-demand regression | pass | Reviewer rerun: `Tests run: 106, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| Production-reference inspection | pass | No removed legacy executor class, v2 schema, exact old v2 catalog filename, enriched-catalog executor path, `--fault-vector`, standalone `FAULT_VECTOR`, or `CATALOG_PATH` production surface remains. |
| Script/Compose validation | pass | Shell syntax/executable check and `docker compose config --quiet` passed. |
| `git diff --check` | pass | No whitespace errors. |
| Branched exact-step isolation | pass | Simulator and verifier tests run only the named branch; the later assigned branch remains body-not-run and absent from runtime executed steps. |
| Package and dynamic-artifact collision matrix | pass | Dry-run/measured direct package aliases plus custom-location dynamic sidecar/manifest/join-report and normalized/symbolic/hard-link aliases are rejected with byte-identical inputs and zero startup/body activity. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Changes are confined to the executor migration, pure action validation, narrow simulator controls/results, focused runtime fixtures/tests, and required CLI/script/Compose surfaces. |
| Existing patterns | pass | The executor reuses the v3 package reader, materializer, in-memory fault provider, Saga unit-of-work state, Jackson report writing, and existing orchestration style. Ordinary workflow APIs remain intact. |
| Test quality | pass | Tests assert observable bodies, executed checkpoints, compensation/commit invocation counts, planned/actual ids and positions, lifecycle ordering, serialized report fields, pre-action gates, and byte checksums rather than only implementation details. |
| Regression risk | pass | Focused simulator regression covers ordinary Saga execution alongside the new controls; package/generator/on-demand suites cover validator compatibility. Remaining zero-bit and detailed hard-stop behavior is explicitly isolated to S7. |
| Security/data safety | pass | Caller-controlled output cannot overwrite linked package inputs or existing authoritative v3 enrichment artifacts, including filesystem aliases. Execution does not mutate source artifacts. |
| Change hygiene | pass | No unrelated source/docs refactor or new dependency was introduced; unrelated untracked meeting notes were left untouched. |

## Findings

None.

## Done Transition

- Moved to done: `yes`
- Done path: `issues/2026-07-19-compensation-aware-scenario-catalog/done/006-exact-persisted-action-replay.md`
- Reason if not moved: `None`

## Reviewer Notes

The exploratory `/tmp/ParticipantOrderProbe.java` deliberately supplied a self-consistent catalog whose declared participant order conflicted with the test runtime's hidden dependency graph. It hard-stopped with `INCOMPLETE` rather than falsely reporting `EXACT`. This does not block S6: valid generated WorkloadPlans define the persisted order, while runtime incompatibility classification and pre-/post-measured infrastructure handling are explicit S7 scope.

The two initial Maven commands were accidentally invoked from the repository root and failed because the repository has no root aggregator. They were rerun from the required `simulator/` and `verifiers/` module directories and passed; only the module-scoped successes are treated as verification evidence. Browser evidence was not required.