# Slice Review: 006 - Exact Persisted Action Replay

## Review Attempt

Attempt: `02`

Latest copy updated: `yes`

## Verdict

`FAIL`

Moved to done: `no`

## Sources Reviewed

- Slice: `issues/2026-07-19-compensation-aware-scenario-catalog/006-exact-persisted-action-replay.md`
- Spec: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Implementation plan: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`, `issues/2026-07-19-compensation-aware-scenario-catalog/decision-frame.md`
- ADRs: `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`, `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`, `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`, `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md`
- Completion evidence: `006-exact-persisted-action-replay.md` — `## Completion Evidence` (`Status: implemented-awaiting-review`) and `### Review Attempt 01 Fix Evidence`
- Changed files reviewed: `docker-compose.yml`; `verifiers/scripts/run-scenario-executor.sh`; executor source under `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/FaultScenarioValidator.java`; executor/writer tests under `verifiers/src/test/`; simulator workflow/unit-of-work source under `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/`; and new simulator tests under `simulator/src/test/`
- Prior review reports: `issues/2026-07-19-compensation-aware-scenario-catalog/review/006-exact-persisted-action-replay-review-01.md`
- Commands run by reviewer:
  - `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test`
  - `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test`
  - `cd verifiers && mvn -Dtest=RecoveryScheduleGeneratorSpec,ScenarioCatalogJsonlWriterSpec,OnDemandFaultScenarioServiceSpec test`
  - `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec,RecoveryScheduleGeneratorSpec,ScenarioCatalogJsonlWriterSpec test`
  - `git diff --check`
  - `bash -n verifiers/scripts/run-scenario-executor.sh && test -x verifiers/scripts/run-scenario-executor.sh`
  - `docker compose config --quiet`
  - production-reference `rg --pcre2` inspection for removed v2 executor/catalog and direct vector-overlay surfaces
  - compiled and ran `/tmp/DynamicManifestAliasProbe.java` against the built verifier classes to test `outputPath == workload-dynamic-evidence-manifest.json`

## Summary

The two attempt-01 blockers are substantially fixed. The new simulator executor-only step operation executes only the named dependency-ready step, the branched simulator and verifier regressions pass, and persisted planned/actual action positions now correspond to one runtime body at a time. The executor also rejects normalized, symbolic, and same-file aliases for the manifest and four linked package artifacts before preparation.

The slice still violates its explicit enrichment immutability contract. The alias guard protects only the manifest, WorkloadPlan catalog, FaultScenario catalog, accounting, and rejected-input artifact. It does not protect the v3 dynamic-enrichment sidecar, its manifest, or join report. A reviewer dry-run probe used the canonical `workload-dynamic-evidence-manifest.json` as `outputPath`; the executor returned `DRY_RUN` and replaced the dynamic manifest with report schema v4. This directly fails slice scope and AC-42, and the test suite has no enrichment-output collision coverage.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | fail | Exact action replay is fixed, but `006-exact-persisted-action-replay.md:24` requires package, accounting, **and enrichment** bytes to remain unchanged; the reviewer probe overwrote the canonical dynamic-enrichment manifest. |
| Slice out-of-scope respected | pass | Full zero-bit fallback remains deferred to S7; no on-demand mutation, v2 compatibility, compensation retry policy, or unrelated feature was added. |
| Spec non-goals respected | pass | No TCC/distributed parity, compensation faults/retries, delayed commit, search/scoring, or new dependency was introduced. |
| Dependencies done | pass | Required dependency `004-materializable-eager-baseline-and-accounting.md` exists under `issues/2026-07-19-compensation-aware-scenario-catalog/done/`; S2 and S3 are also done. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-2 | pass | `ScenarioCatalogReader` loads only the linked v3 package; v2 rejection passes; production inspection found no legacy executor reader or old execution selector. | Diagnostic clearly states that v3 packages are required and v2 is unsupported. |
| AC-9 | pass | All-zero verifier coverage reports four exact actions, commits each participant on its own final forward action, and asserts the runtime body trace. | Branched coverage now proves no ready sibling leaks into an earlier action. |
| AC-14 | pass | Final forward actions invoke controlled finalization immediately and emit `AUTOMATIC_COMMIT` only after successful commit; commit invocation counts are one per survivor. | Final-body and commit phases remain one planned action. |
| AC-25 | pass | Java options, CLI, script, and Compose require package plus persisted FaultScenario id and reject `--fault-vector`; package validation covers references, ownership, action identities, enabling, reverse compensation, completeness, and residual global forward order before preparation. | Required and supplemental verifier suites pass. |
| AC-26 | pass | Materialization and startup failures have zero actual actions and absent conformance; every participant is materialized before startup and every startup is attempted before replay. | Fixture body/constructor assertions support the all-or-nothing gate. |
| AC-27 | pass | `ExecutionPlan.executeStepForExecutor` rejects unmet/already-attempted targets and executes only `List.of(targetStep)`; branched tests assert exact body, action-id, planned-position, and actual-position order. | Attempt-01 branch leak is fixed. |
| AC-28 | pass | Assigned targets use `abortBeforeStepForExecutor`; simulator and branched verifier tests prove target body absence, target absence from `SagaUnitOfWork.executedSteps`, and recoverability of prior checkpoints. | Later assigned branch no longer runs early. |
| AC-29 | pass | Each unique persisted compensation action calls `recoverStepForExecutor` once; tests prove one requested checkpoint, explicit-before-implicit truth, and no compensation fault slots. | Supplemental generator/package suites pass. |
| AC-30 | pass | Reports distinguish body and commit outcomes; successful final actions commit, controlled commit failure retains body success and unconsumed checkpoints, and assigned final fault runs neither body nor commit. | Focused simulator and verifier assertions pass. |
| AC-36 | pass | First-step assigned failure emits `ABORTED`, `NO_COMPENSATION_WORK`, and `COMPENSATED`; final states and invocation counts prevent double close/recovery. | No-work case has zero compensation outcomes. |
| AC-37 | pass | Report v4 contains attempt/package/workload/FaultScenario identity, vector slots, planned and actual actions, lifecycle events, participants/final states, blockers, and terminal status. | Serialization assertions pass. |
| AC-38 | pass | Complete all-zero and assigned schedules report `EXACT`; dry-run/preparation failures omit conformance; measured controlled hard stops report `INCOMPLETE`. | S7 still owns completed `DEVIATED` fallback. |
| AC-39 | pass | Action outcomes carry owner/source/positions/status/body/commit/fault origin/evidence/exceptions; recovery outcomes carry ordered explicit and implicit sub-outcomes. | Exact-step fix makes forward attribution truthful for branches. |
| AC-40 | pass | Assigned replay reports `NOT_ASSIGNED`, `REALIZED`, and `MASKED`, preserves participant-local skipped forwards, and retains `NOT_REACHED` for assigned slots not reached before hard stop. | State matrix assertions pass. |
| AC-41 | pass | Reports emit explicit abort, automatic commit, compensated, and no-work lifecycle events with separate final body/commit outcomes. | Branched runtime order now agrees with event/action attribution. |
| AC-42 | fail | `ScenarioExecutor.java:606-615` protects only five package paths. The reviewer probe put a valid v3 dynamic-enrichment manifest at the canonical configured name and selected it as report output; execution returned `DRY_RUN`, removed the dynamic schema, and wrote report v4. | Spec AC-42 explicitly includes dynamic-enrichment artifacts. |
| AC-44 | fail | Simulator/executor/package/on-demand suites are meaningful and green, but no automated case protects dynamic sidecar, sidecar-manifest, or join-report bytes from caller-controlled report output. | Missing coverage permits the AC-42 regression. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| `cd simulator && mvn -Dtest=SagaExecutorControlTest,SagaStepwiseRecoveryTest,WorkflowExecutionPlanTest test` | pass | Reviewer rerun: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test` | pass | Reviewer rerun: `Tests run: 36, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| Supplemental executor/generator/package regression | pass | Reviewer rerun of executor/orchestrator/recovery/writer: `Tests run: 59, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| On-demand compatibility after validator changes | pass | Reviewer rerun of recovery/writer/on-demand: `Tests run: 59, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`. |
| Production-reference inspection | pass | No production legacy executor model/schema, enriched-catalog execution path, exact old v2 catalog path, `--fault-vector`, `FAULT_VECTOR`, or `CATALOG_PATH` invocation surface remains in `verifiers/src/main`, the runner script, or Compose. The in-memory provider name is correctly retained. |
| Script/Compose validation | pass | Shell syntax/executable check and `docker compose config --quiet` passed. |
| `git diff --check` | pass | No whitespace errors. |
| Branched exact-step regression | pass | Simulator and verifier tests show only `first`, then selected sibling `second`; assigned later sibling `third` remains body-not-run and absent from executed steps. |
| Package-artifact alias regressions | pass | Dry-run/measured tests reject all five linked package artifacts plus normalized and symbolic aliases before startup. |
| Dynamic-enrichment manifest alias probe | fail | `/tmp/DynamicManifestAliasProbe.java` wrote canonical `workload-dynamic-evidence-manifest.json` with schema `microservices-simulator.workload-dynamic-evidence-manifest.v3`, then used it as dry-run report output. Result: `terminal=DRY_RUN`, `dynamicSchemaPreserved=false`, `reportSchemaPresent=true`. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Exact-step changes are narrow executor-oriented controls; report/reader/CLI migration remains within S6. |
| Existing patterns | pass | Ordinary `executeUntilStep`, `resumeWorkflow`, and existing Saga behavior remain intact; the executor uses a separately named exact-step control. |
| Test quality | fail | Exact replay, lifecycle, validation, package artifacts, and branch behavior are well tested, but the explicit dynamic-enrichment immutability class is absent. |
| Regression risk | fail | Caller-controlled output can destroy runtime evidence needed for later audit/QA even before measured actions begin. |
| Security/data safety | fail | Existing canonical dynamic sidecar artifacts can be overwritten by a valid API/CLI output choice; package-only alias checks are insufficient for the specified protected data set. |
| Change hygiene | pass | Changes are confined to the slice and its evidence; unrelated untracked meeting notes were not modified. |

## Findings

| Severity | Finding | Evidence | Required Fix |
|----------|---------|----------|--------------|
| blocking | Report-output collision protection omits v3 dynamic-enrichment artifacts, so dry-run or measured execution can overwrite the WorkloadPlan-linked sidecar, sidecar manifest, or join report despite the slice/AC-42 immutability contract. | `ScenarioExecutor.java:606-615` lists only the package manifest, workload catalog, FaultScenario catalog, accounting, and rejected-inputs file. Canonical enrichment paths are configured at `verifiers/src/main/resources/application.yaml:34-36`. `/tmp/DynamicManifestAliasProbe.java` replaced a valid canonical dynamic manifest with report v4 during `DRY_RUN`. | Extend the pre-preparation collision gate to protect associated v3 dynamic-enrichment artifacts, including custom configured locations and normalized/symbolic/hard-link aliases. If the base package cannot currently discover those paths, add a reliable input/discovery contract or reject overwriting an existing recognized v3 enrichment artifact. Add dry-run and measured regressions for the sidecar JSONL, sidecar manifest, and join report that assert byte-identical checksums and zero startup/actions. |

## Done Transition

- Moved to done: `no`
- Done path: `None`
- Reason if not moved: `FAIL — dynamic-enrichment artifacts remain overwritable through the report output path, violating slice scope and AC-42.`

## Reviewer Notes

The first two Maven commands were initially invoked from the repository root and failed because this repository has no root aggregator. They were immediately rerun from `simulator/` and `verifiers/` as required and passed; only those correctly scoped runs are used as verification evidence. The unrelated untracked meeting-note files remained untouched.
