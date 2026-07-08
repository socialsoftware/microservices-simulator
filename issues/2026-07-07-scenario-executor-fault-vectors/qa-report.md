# QA Report: Scenario Executor Fault Vectors

## Verdict

Feature QA: `PARTIAL`

Ready to PR: `no`

Recommended next action: `resolve blockers`

## Sources Reviewed

- Spec: `issues/2026-07-07-scenario-executor-fault-vectors/spec.md`
- Implementation plan: `issues/2026-07-07-scenario-executor-fault-vectors/implementation-plan.md`
- Active slices: None found in package root.
- Done slices:
  - `issues/2026-07-07-scenario-executor-fault-vectors/done/001-simulator-in-memory-fault-provider.md`
  - `issues/2026-07-07-scenario-executor-fault-vectors/done/002-executor-vector-validation-and-report-v2.md`
  - `issues/2026-07-07-scenario-executor-fault-vectors/done/003-no-fault-lifecycle-closure.md`
  - `issues/2026-07-07-scenario-executor-fault-vectors/done/004-realized-fault-compensation-and-masking.md`
  - `issues/2026-07-07-scenario-executor-fault-vectors/done/005-mismatch-unexpected-and-compensation-failures.md`
  - `issues/2026-07-07-scenario-executor-fault-vectors/done/006-cli-docker-runner-and-quizzes-smoke.md`
  - `issues/2026-07-07-scenario-executor-fault-vectors/done/007-scenario-executor-docs.md`
- Review reports: latest PASS reports under `issues/2026-07-07-scenario-executor-fault-vectors/review/*-review.md`; prior failed/fixed attempts for slices 002 and 007 also reviewed.
- Execution log: `issues/2026-07-07-scenario-executor-fault-vectors/execution-log.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`
- ADRs: None.
- Git status/diff: reviewed `git status --short`, `git diff --name-status`, untracked files, feature source/test/docs diffs, and S6 report artifacts. The working tree contains feature-owned changes plus unrelated/unowned changes listed below.
- Commands/browser checks run by QA:
  - `cd simulator && mvn -Dtest=FaultVectorProviderTest,ExecutionPlanDynamicEvidenceTest test`
  - `cd simulator && mvn -DskipTests install`
  - `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test`
  - `bash -n verifiers/scripts/run-scenario-executor.sh`
  - `docker compose config --quiet`
  - `rg -n "scenario-executor-poc|ScenarioExecutor POC|generated fault injection" docs/verifiers-impl mkdocs.verifier.yml -g '!docs/verifiers-impl/archive/**' || true`
  - `rg -n "\\bPOC\\b|scenario-executor-poc|ScenarioExecutor POC|generated fault injection" docs/verifiers-impl/*.md docs/verifiers-impl/reference mkdocs.verifier.yml || true`
  - `./scripts/verifier-docs build`
  - Python assertions over `verifiers/target/scenario-executor/s6-default-report.json`, `s6-explicit-report.json`, and `s6-invalid-report.json`

## Summary

The implemented behavior is mostly complete and well verified: all seven slices are in `done/`, each has final PASS review evidence, targeted simulator/verifier/docs checks pass, and the S6 Quizzes smoke reports show default-vector `SUCCESS` and explicit-vector `FAULT_COMPENSATED` outcomes.

QA is not marking the package complete because AC-18 is only partially satisfied. The v2 report does contain catalog/scenario/vector metadata, but `runtimeMetadata.applicationBase` is populated with the saga FQN and the report does not carry Spring profiles or the runner application id/base needed by the spec's reproducibility wording. The working tree also contains unrelated/unowned changes that block PR readiness even if the feature code is otherwise isolated.

## Acceptance Criteria Matrix

| AC | Status | Evidence | Notes |
|----|--------|----------|-------|
| AC-1 | PASS | `ScenarioExecutorCli` parses `--fault-vector`; `ScenarioExecutorOrchestrator` and `verifiers/scripts/run-scenario-executor.sh` pass vectors; `docker-compose.yml` exposes `FAULT_VECTOR`; S6 invalid report has `INVALID_FAULT_VECTOR` / `MISSING_EXPLICIT_SCENARIO_ID`. | Explicit vectors require explicit scenario id. |
| AC-2 | PASS | `ScenarioExecutor.validateVector(...)` uses `faultSpace.defaultVector()` when no vector is supplied; S3/S6 reports show `vectorSource=DEFAULT_VECTOR`; provider mode is `IN_MEMORY_FAULT_VECTOR` for real runs. | CSV suppression is covered by S1 provider tests plus executor provider activation. |
| AC-3 | PASS | `ScenarioExecutorSpec` covers wrong length, non-binary strings, missing explicit id, invalid defaults, fault-space length mismatch, duplicate ids, unresolved ids, and non-unique mappings before execution. | QA reran the full executor spec. |
| AC-4 | PASS | Dry-run tests assert expanded fault slots, `providerMode=NONE`, `terminalStatus=DRY_RUN`, no fixture execution, and unchanged input catalog content. | No provider/materialization/execution in dry-run path. |
| AC-5 | PASS | Simulator provider API under `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/*`; reviewer `rg` evidence found no verifier imports. | Plain simulator-owned values. |
| AC-6 | PASS | `FaultVectorInjectedFaultException` extends `SimulatorException` and carries execution/plan/saga/scheduled-step/slot/runtime-step identity; simulator and executor tests assert identity. | Machine-distinguishable from legacy CSV `SimulatorException`. |
| AC-7 | PASS | `FaultVectorProviderHolder.install(...)` scoped API, concurrent install rejection, boundary cleanup; executor tests assert inactive holder after success, fault, mismatch, unexpected failure, and compensation failure. | QA reran simulator and verifier tests. |
| AC-8 | PASS | `FaultVectorProviderTest.legacyCsvFaultStillAppliesWhenNoProviderIsActive` passed in QA run. | Existing CSV/manual impairment remains outside provider scopes. |
| AC-9 | PASS | `FaultVectorProviderTest.activeProviderSuppressesCsvFaultsAndDelaysWhenItDoesNotInject` passed; executor real runs install provider and report `IN_MEMORY_FAULT_VECTOR`. | Provider is authoritative during executor vector runs. |
| AC-10 | PASS | Fixture execution reports `SUCCESS` / `COMMITTED`, calls `resumeWorkflow(...)`, and emits ordered completed steps. | QA reran `ScenarioExecutorSpec`. |
| AC-11 | PASS | Realized fault tests show prefix body executes, target body does not, compensation is called, and report is `FAULT_COMPENSATED` / `COMPENSATED`; S6 explicit Quizzes report confirms `INJECTED_FAULT`. | Supported single-saga path. |
| AC-12 | PASS | Masking test vector `011` reports later assigned slot `MASKED` with reason `masked by earlier realized slot ...`. | Deterministic slot order preserved. |
| AC-13 | PASS | Suppressed-signal fixture path reports `EXPECTED_FAULT_NOT_INJECTED` with slot `UNREALIZED`. | QA reran negative-path tests. |
| AC-14 | PASS | Unexpected signal at `0` reports `UNEXPECTED_INJECTED_FAULT`; wrong identity reports `FAULT_PROVIDER_MISMATCH`. | Not collapsed into ordinary runtime failure. |
| AC-15 | PASS | Ordinary `0`-bit fixture failure reports `UNEXPECTED_EXECUTION_FAILURE`, records exception details, and calls best-effort compensation when exposed. | Lifecycle outcome is `COMPENSATED` in covered fixture path. |
| AC-16 | PASS | Compensation-failure fixture reports `COMPENSATION_FAILED`, includes forward and compensation blocker details, and exit-code helper maps it to non-zero. | QA reran executor tests. |
| AC-17 | PASS | Fault slots are by slot index; step outcomes by `scheduleOrder`; tests assert ordering across dry-run, success, fault, masking, and negative paths. | S6 reports also show deterministic slot/step mapping. |
| AC-18 | PARTIAL | `ScenarioExecutionReport.RuntimeMetadata` has catalog path/kind, scenario id, vector source, executor mode, and dry-run; reports also include top-level saga/scenario/vector fields. | Gap: `runtimeMetadata.applicationBase` is populated from `saga.sagaFqn()` in `ScenarioExecutor.report(...)`, and reports do not include Spring profiles or runner application id/base. This weakens the spec's reproducibility requirement. |
| AC-19 | PASS | Source review found report writes only via `writeReport(...)`; dry-run test asserts catalog unchanged; feature writes standalone executor reports, not catalog/enrichment sidecars. | Runtime domain state changes are expected for real execution and are not catalog mutation. |
| AC-20 | PASS | `ScenarioExecutorCli.exitCodeFor(...)` maps `SUCCESS`, `FAULT_COMPENSATED`, and `DRY_RUN` to zero; broken statuses to non-zero; S6 default/explicit/invalid reports align. | QA reran exit-helper coverage. |
| AC-21 | PASS | `ScenarioExecutorSpec` covers vector validation, invalid defaults, malformed mappings, dry-run, cleanup, compensation, masking, missing expected fault, mismatch, unexpected failure, compensation failure, and unsupported shape; `FaultVectorProviderTest` covers CSV override. | Broad dummyapp-style coverage exists. |
| AC-22 | PASS | S6 evidence and QA Python assertions over `verifiers/target/scenario-executor/s6-default-report.json` and `s6-explicit-report.json` show Quizzes default `SUCCESS` and explicit single-bit `FAULT_COMPENSATED`. | QA did not rerun heavy Docker smokes; it field-checked existing smoke artifacts and reran runner/config tests. |
| AC-23 | PASS | `docs/verifiers-impl/reference/scenario-executor.md` exists; live stale POC searches returned no output; `./scripts/verifier-docs build` passed. | Unsupported/future areas remain explicit. |

## Slice Reconciliation

| Slice | State | Completion Evidence | Review Status | ACs Claimed | Notes |
|-------|-------|---------------------|---------------|-------------|-------|
| `001-simulator-in-memory-fault-provider.md` | done | complete | PASS | AC-5, AC-6, AC-7, AC-8, AC-9 | QA reran simulator provider and dynamic-evidence regression tests. |
| `002-executor-vector-validation-and-report-v2.md` | done | complete | PASS after attempt 02 | AC-1, AC-2, AC-3, AC-4, AC-17, AC-18, AC-19, AC-20, AC-21 | Prior review findings were fixed. AC-18 remains partial at package QA due report runtime metadata gaps not caught by slice review. |
| `003-no-fault-lifecycle-closure.md` | done | complete | PASS | AC-2, AC-7, AC-9, AC-10, AC-17, AC-18, AC-19, AC-20, AC-21 | Committed no-fault lifecycle verified. |
| `004-realized-fault-compensation-and-masking.md` | done | complete | PASS | AC-6, AC-7, AC-9, AC-11, AC-12, AC-17, AC-20, AC-21 | Fault compensation/masking verified. |
| `005-mismatch-unexpected-and-compensation-failures.md` | done | complete | PASS | AC-7, AC-13, AC-14, AC-15, AC-16, AC-17, AC-20, AC-21 | Negative statuses verified. |
| `006-cli-docker-runner-and-quizzes-smoke.md` | done | complete | PASS | AC-1, AC-2, AC-11, AC-20, AC-22 | Heavy Docker smokes were not rerun by QA; existing reports were checked. |
| `007-scenario-executor-docs.md` | done | complete | PASS after attempt 02 | AC-23 | Docs build and stale-reference searches pass. |

## Verification Summary

| Command / Method | Result | Evidence | Notes |
|------------------|--------|----------|-------|
| `cd simulator && mvn -Dtest=FaultVectorProviderTest,ExecutionPlanDynamicEvidenceTest test` | PASS | `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`; finished 2026-07-08T19:11:11+01:00. | Validates provider scope/signal/CSV behavior and dynamic-evidence regression. |
| `cd simulator && mvn -DskipTests install` | PASS | `BUILD SUCCESS`; installed `MicroservicesSimulator-3.2.0-SNAPSHOT` to local Maven repo; finished 2026-07-08T19:11:44+01:00. | Needed so `verifiers/` compiles/tests against current simulator source in this non-aggregated repo. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test` | PASS | `Tests run: 35, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`; finished 2026-07-08T19:12:13+01:00. | Validates executor behavior and orchestrator vector propagation. |
| `bash -n verifiers/scripts/run-scenario-executor.sh` | PASS | No syntax output. | Shell syntax only. |
| `docker compose config --quiet` | PASS | No output. | Compose parses without global `CATALOG_PATH`. |
| Stale docs search | PASS | No output for live `scenario-executor-poc`, `ScenarioExecutor POC`, `generated fault injection`, or live top-level/reference `POC` searches. | Archive historical references excluded where appropriate. |
| `./scripts/verifier-docs build` | PASS | Documentation built to `target/verifier-docs-site` in 1.53s. | MkDocs Material warning is informational. |
| Python S6 report assertions | PASS | `S6 report assertions passed for default, explicit, and invalid reports`. | Checked terminal/lifecycle/vector/slot/step fields in existing smoke reports. |

## End-to-End / Integration Check

- Status: `PARTIAL`
- Evidence: QA field-checked existing Docker smoke reports from S6:
  - `verifiers/target/scenario-executor/s6-default-report.json`: `SUCCESS`, `COMMITTED`, `DEFAULT_VECTOR`, vector `0`, slot `NOT_ASSIGNED`, step `COMPLETED`.
  - `verifiers/target/scenario-executor/s6-explicit-report.json`: `FAULT_COMPENSATED`, `COMPENSATED`, `EXPLICIT_VECTOR`, vector `1`, slot `REALIZED`, step `INJECTED_FAULT`.
  - `verifiers/target/scenario-executor/s6-invalid-report.json`: `INVALID_FAULT_VECTOR`, `NOT_STARTED`, `EXPLICIT_VECTOR`, blocker `MISSING_EXPLICIT_SCENARIO_ID`.
- Gaps: QA did not rerun the heavy Docker smoke commands. The current report metadata gap under AC-18 also prevents calling the package fully complete.

## Unowned Changes Audit

| Path | Status | Owner / Evidence | Notes |
|------|--------|------------------|-------|
| `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/*` | owned | Slice 001 completion/review | New simulator provider API. |
| `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/ExecutionPlan.java` | owned | Slice 001 completion/review | Provider integration and CSV suppression. |
| `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/FaultVectorProviderTest.java` | owned | Slice 001 completion/review | Provider regression coverage. |
| `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/*` | owned | Slices 002-006 completion/reviews | Executor v2 report, vector validation, lifecycle, fault handling, CLI/orchestrator. |
| `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/*` and `verifiers/src/test/java/.../FixtureWorkflow.java` | owned | Slices 002-006 completion/reviews | Executor and orchestrator tests. |
| `verifiers/scripts/run-scenario-executor.sh`, `docker-compose.yml` | owned | Slice 006 completion/review | Runner and compose vector propagation. |
| `docs/verifiers-impl/reference/scenario-executor.md`, removed `docs/verifiers-impl/reference/scenario-executor-poc.md`, `mkdocs.verifier.yml`, main live verifier docs | owned | Slice 007 completion/review | Docs rename and POC wording cleanup. |
| `issues/2026-07-07-scenario-executor-fault-vectors/done/*`, `review/*`, deleted root `001-*.md` through `007-*.md`, `execution-log.md` | owned | sp-execute/sp-review workflow evidence | Expected slice moves/review artifacts are unstaged/untracked because no commit was authorized. |
| `.claude/skills/**`, `CLAUDE.md` | unowned | None in this feature package | Deleted files are unrelated to the scenario-executor feature and block clean PR readiness unless handled separately. |
| `André_Silva___IST_UL___MEIC_PIC2.pdf` | unowned | None in this feature package | Untracked PDF; relevant as thesis context generally, but not owned by this feature package. |
| `docs/verifiers-impl/archive/meeting-notes/2026-W27-thesis-meeting-outcomes.md`, `docs/verifiers-impl/archive/meeting-notes/index.md` | unowned / unclear | Not listed in slice completion evidence | Meeting-outcomes doc/index additions look unrelated to the ScenarioExecutor docs slice; isolate or document ownership before PR. |
| `issues/dynamic-unmatched-fixture-diagnostics/`, `issues/missing-saga-input-coverage-post-helper-fix-audit/`, `issues/scenario-executor-readiness-alignment/`, `issues/static-event-semantics-for-verifier-inputs/*` | unowned | None in this feature package | Untracked unrelated issue-package material. |
| `thesis-drafts/*` | unowned | None in this feature package | Untracked thesis draft files; not part of this feature package. |

## Scope and Non-Goal Audit

- No evidence of multi-saga execution, TCC execution, stream/gRPC/distributed parity, compensation-step faults, non-binary/delay vectors, batch/search/scoring, generic reset, catalog mutation, or dynamic-enrichment feedback being added by this package.
- Documentation keeps unsupported/future work explicit.
- The executor v2 schema intentionally breaks old report compatibility, matching the spec.

## Docs / Context / ADR Drift

- Glossary routing is correct: `CONTEXT-MAP.md` points verifier terminology to `docs/verifiers-impl/glossary.md`; no root `CONTEXT.md` exists.
- Live docs no longer use current-path ScenarioExecutor POC framing.
- Drift found: report runtime metadata does not fully match spec/docs reproducibility intent because application base/id and Spring profiles are not represented as report fields.

## Findings

| Severity | Finding | Evidence | Required Follow-Up |
|----------|---------|----------|--------------------|
| major | AC-18 is only partially satisfied: v2 report runtime metadata is not sufficient on its own for reproducibility. | `ScenarioExecutionReport.RuntimeMetadata` lacks Spring profiles and runner application id/base; `ScenarioExecutor.report(...)` populates `applicationBase` with `saga.sagaFqn()`; `verifiers/target/scenario-executor/s6-default-report.json` shows `runtimeMetadata.applicationBase` equals the saga FQN and has no Spring profile/application runner fields. | Add explicit runtime metadata fields for application base/id and Spring profiles, or change the schema/docs/tests to make the actual reproducibility contract explicit; rerun executor tests and smoke/report assertions. |
| major | Working tree contains unrelated/unowned changes that block PR readiness. | `git status --short` shows unrelated deleted `.claude`/`CLAUDE.md`, untracked PDF, unrelated issue packages, thesis drafts, and unclear meeting-outcomes docs in addition to feature changes. | Isolate/stash/commit separately or document ownership before opening a PR for this feature. |
| note | QA did not rerun heavy Docker smokes. | Existing S6 reports were field-checked and runner/config tests passed, but Docker one-shot commands were not rerun in QA. | Rerun Docker smokes if PR gate requires fresh end-to-end evidence. |

## Blockers

- AC-18 runtime metadata gap.
- Unrelated/unowned worktree changes prevent a clean PR boundary.

## Remaining Work

- Add or correct v2 report runtime metadata for application base/id and Spring profiles, with tests and report assertions.
- Clean or isolate unrelated worktree changes before PR.
- Optionally rerun Quizzes Docker smokes after the metadata fix.

## Final Recommendation

Do not start PR flow yet. Make a small repair for AC-18, then isolate unrelated worktree changes and rerun the targeted simulator/verifier/docs checks plus the Quizzes smoke/report assertions. After that, rerun package QA; the feature should be close to complete once the metadata and worktree-boundary issues are resolved.
