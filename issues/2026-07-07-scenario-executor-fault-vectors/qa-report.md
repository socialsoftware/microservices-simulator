# QA Report: Scenario Executor Fault Vectors

## Verdict

Feature QA: `COMPLETE`

Ready to PR: `no`

Recommended next action: `other: no implementation work remains; do not start PR flow unless/until unrelated dirty worktree state is intentionally isolated`

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
- Review reports: latest `PASS` reports under `issues/2026-07-07-scenario-executor-fault-vectors/review/*-review.md`; failed/fixed attempts for slices 002 and 007 also reviewed through the final latest reports and execution log.
- Execution log: `issues/2026-07-07-scenario-executor-fault-vectors/execution-log.md`, including `QA Repair: 2026-07-08 19:39 WEST`.
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`, `docs/verifiers-impl/evidence.md`.
- ADRs: None required by the spec/plan.
- Git status/diff: reviewed `git status --short`, `git diff --name-status`, `git diff --stat`, and recent feature commits. Current feature code/docs are committed in `HEAD`; current working-tree dirt is unrelated to this feature package and is accepted per user instruction because this is not a PR flow.
- Commands/browser checks run by QA:
  - `cd simulator && mvn -Dtest=FaultVectorProviderTest,ExecutionPlanDynamicEvidenceTest test`
  - `cd simulator && mvn -DskipTests install`
  - `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test`
  - `bash -n verifiers/scripts/run-scenario-executor.sh`
  - `docker compose config --quiet`
  - `rg -n "scenario-executor-poc|ScenarioExecutor POC|generated fault injection" docs/verifiers-impl mkdocs.verifier.yml -g '!docs/verifiers-impl/archive/**' || true`
  - `rg -n "\\bPOC\\b|scenario-executor-poc|ScenarioExecutor POC|generated fault injection" docs/verifiers-impl/*.md docs/verifiers-impl/reference mkdocs.verifier.yml || true`
  - `[ -e CONTEXT.md ] && echo ROOT_CONTEXT_EXISTS || echo NO_ROOT_CONTEXT`
  - `./scripts/verifier-docs build`
  - Python assertions over `verifiers/target/scenario-executor/s6-default-report.json`, `s6-explicit-report.json`, `s6-invalid-report.json`, and selected catalog plan `verifiers/target/quizzes-20260708-163552-193/scenario-catalog.jsonl`.

## Summary

The feature package is complete for the specified supported scope. All seven slices are in `done/`, every latest slice review is `PASS`, the prior package-QA AC-18 metadata gap has been repaired, and targeted simulator/verifier/docs checks pass. The validated Quizzes smoke reports now include reproducible runtime metadata (`applicationBase`, `applicationId`, Spring application class/profiles, Maven profile, catalog metadata) and show the required default-vector `SUCCESS` and explicit-vector `FAULT_COMPENSATED` outcomes.

The worktree remains dirty with unrelated files, but the user explicitly stated this is not a PR and dirty state is acceptable. QA therefore does not treat unrelated dirty files as a feature-completion blocker; it only keeps `Ready to PR` as `no` because no clean PR boundary is being asserted.

## Acceptance Criteria Matrix

| AC | Status | Evidence | Notes |
|----|--------|----------|-------|
| AC-1 | PASS | `ScenarioExecutorCli` parses `--fault-vector`; `ScenarioExecutorOptions` carries `faultVector`; `ScenarioExecutorOrchestrator` and `verifiers/scripts/run-scenario-executor.sh` propagate vectors; `docker-compose.yml` exposes `FAULT_VECTOR`; `ScenarioExecutorSpec` and S6 invalid report cover explicit vector without explicit scenario id. | Explicit vectors require explicit `--scenario-id` / `SCENARIO_ID`. |
| AC-2 | PASS | `ScenarioExecutor.validateVector(...)` defaults to `faultSpace.defaultVector()`; S3/S6 reports show `vectorSource=DEFAULT_VECTOR`; real runs use `providerMode=IN_MEMORY_FAULT_VECTOR`; S1 provider tests cover CSV suppression when active. | Auto-selection remains only for default-vector runs. |
| AC-3 | PASS | `ScenarioExecutorSpec` covers wrong length, non-binary vector, explicit vector without id, invalid default vector, `faultSpace.length` mismatch, duplicate scheduled ids, unresolved ids, non-unique mapping, and invalid empty vector before execution. | QA reran the full executor spec. |
| AC-4 | PASS | Dry-run tests assert expanded slots, `providerMode=NONE`, `terminalStatus=DRY_RUN`, no execution/compensation, and unchanged input catalog. | Dry-run does not install provider or materialize/execute sagas. |
| AC-5 | PASS | Simulator provider API lives under `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/*`; S1 review and source inspection confirm plain simulator values and no verifier model dependency. | Simulator owns injection contract. |
| AC-6 | PASS | `FaultVectorInjectedFaultException` extends `SimulatorException` and carries scenario execution id, plan id, saga id, scheduled step id, slot index, functionality identity, runtime step, and assigned bit; provider/executor tests assert identity and report typed exception class. | Distinguishable from ordinary runtime/CSV failures. |
| AC-7 | PASS | `FaultVectorProviderHolder.install(...)` is scoped, rejects concurrent providers, clears current boundary/provider state; executor tests assert inactive holder after success, expected fault, mismatch, unexpected failure, and compensation failure. | QA reran simulator and verifier tests. |
| AC-8 | PASS | `FaultVectorProviderTest.legacyCsvFaultStillAppliesWhenNoProviderIsActive` passed. | Existing CSV/manual impairment remains usable outside executor-owned vector runs. |
| AC-9 | PASS | `FaultVectorProviderTest.activeProviderSuppressesCsvFaultsAndDelaysWhenItDoesNotInject` passed; `ExecutionPlan` bypasses CSV behavior when provider is active; executor reports `IN_MEMORY_FAULT_VECTOR` for real vector runs. | Vector provider is authoritative during executor-owned runs. |
| AC-10 | PASS | Fixture execution calls `resumeWorkflow(...)`, reports `terminalStatus=SUCCESS` and `lifecycleOutcome=COMMITTED`; S6 default report shows `SUCCESS` / `COMMITTED`. | No-fault/default vector closes lifecycle. |
| AC-11 | PASS | Realized-fault fixture tests show prefix body executes, target body does not, forward execution stops, `resumeCompensation(...)` is called, and report is `FAULT_COMPENSATED` / `COMPENSATED`; S6 explicit report shows slot `REALIZED` and step `INJECTED_FAULT`. | Fault injects before step body and compensation succeeds. |
| AC-12 | PASS | Masking test vector `011` reports later assigned slot `MASKED` with reason `masked by earlier realized slot ... after saga abort`. | Deterministic slot order preserved. |
| AC-13 | PASS | Suppressed provider signal fixture reports `EXPECTED_FAULT_NOT_INJECTED`; reached assigned slot is `UNREALIZED`. | Covered in `ScenarioExecutorSpec`. |
| AC-14 | PASS | Fixture-injected signal at `0` bit reports `UNEXPECTED_INJECTED_FAULT`; wrong scheduled-step identity reports `FAULT_PROVIDER_MISMATCH`. | Not collapsed into ordinary runtime failure. |
| AC-15 | PASS | Ordinary forward exception at a `0` bit reports `UNEXPECTED_EXECUTION_FAILURE`, records exception details, and attempts best-effort compensation where exposed. | Fixture reports lifecycle `COMPENSATED`. |
| AC-16 | PASS | Compensation-failure fixture reports `COMPENSATION_FAILED`, preserves forward and compensation blocker details, and exit helper maps it non-zero. | Covered in `ScenarioExecutorSpec`. |
| AC-17 | PASS | Tests assert fault slots ordered by `slotIndex` and step outcomes ordered by `scheduleOrder`; S6 reports have deterministic single-slot/single-step mapping. | Applies across dry-run, success, fault, masking, and negative paths. |
| AC-18 | PASS | `ScenarioExecutionReport.RuntimeMetadata` now includes `applicationBase`, `applicationId`, `springApplicationClass`, `springProfiles`, `mavenProfile`, catalog path/kind, scenario plan id, vector source, executor mode, and dry-run flag; `ScenarioExecutor.report(...)` populates from options; CLI/orchestrator/script pass stable runner values; QA Python assertions over S6 reports passed. | Prior QA metadata gap is fixed. No DB/application state snapshot is attempted. |
| AC-19 | PASS | `ScenarioExecutor.writeReport(...)` writes only `options.outputPath`; dry-run test compares catalog content before/after; S6 reports are standalone under `verifiers/target/scenario-executor/`; source review found no catalog/enriched/dynamic sidecar mutation. | Runtime domain changes during real execution are expected and outside artifact mutation. |
| AC-20 | PASS | `ScenarioExecutorCli.exitCodeFor(...)` returns zero for `SUCCESS`, `FAULT_COMPENSATED`, `DRY_RUN`; invalid/broken statuses return non-zero; tests and S6 invalid evidence align. | S6 invalid report has `INVALID_FAULT_VECTOR` / `MISSING_EXPLICIT_SCENARIO_ID`. |
| AC-21 | PASS | `ScenarioExecutorSpec` covers validation, invalid defaults, malformed mappings, dry-run, no-fault cleanup, compensation, masking, missing expected fault, provider mismatch, unexpected forward failure, compensation failure, unsupported shape/materialization paths; `FaultVectorProviderTest` covers provider/CSV behavior. | Dummyapp-style coverage is broad for designed states. |
| AC-22 | PASS | S6 completion/review evidence and QA Python assertions over regenerated Docker smoke reports show Quizzes default vector `0` -> `SUCCESS` / `COMMITTED`, explicit vector `1` -> `FAULT_COMPENSATED` / `COMPENSATED`, under `test,sagas,local` metadata. | QA did not rerun heavy Docker containers; it validated existing regenerated smoke artifacts and runner/config tests. |
| AC-23 | PASS | `docs/verifiers-impl/reference/scenario-executor.md` exists; live stale POC searches returned no output; glossary/current-state/reference/evidence docs are aligned; `./scripts/verifier-docs build` passed. | Unsupported/future scope remains explicit. |

## Slice Reconciliation

| Slice | State | Completion Evidence | Review Status | ACs Claimed | Notes |
|-------|-------|---------------------|---------------|-------------|-------|
| `001-simulator-in-memory-fault-provider.md` | done | complete | PASS | AC-5, AC-6, AC-7, AC-8, AC-9 | QA reran simulator provider and dynamic-evidence regression tests. |
| `002-executor-vector-validation-and-report-v2.md` | done | complete | PASS after attempt 02 | AC-1, AC-2, AC-3, AC-4, AC-17, AC-18, AC-19, AC-20, AC-21 | Prior review findings fixed; later QA metadata gap was repaired in execution log and verified here. |
| `003-no-fault-lifecycle-closure.md` | done | complete | PASS | AC-2, AC-7, AC-9, AC-10, AC-17, AC-18, AC-19, AC-20, AC-21 | Committed no-fault lifecycle verified. |
| `004-realized-fault-compensation-and-masking.md` | done | complete | PASS | AC-6, AC-7, AC-9, AC-11, AC-12, AC-17, AC-20, AC-21 | Fault compensation/masking verified. |
| `005-mismatch-unexpected-and-compensation-failures.md` | done | complete | PASS | AC-7, AC-13, AC-14, AC-15, AC-16, AC-17, AC-20, AC-21 | Negative statuses and compensation failures verified. |
| `006-cli-docker-runner-and-quizzes-smoke.md` | done | complete | PASS | AC-1, AC-2, AC-11, AC-20, AC-22 | QA validated runner/config and regenerated smoke reports; Docker commands were not rerun. |
| `007-scenario-executor-docs.md` | done | complete | PASS after attempt 02 | AC-23 | Docs search and build pass. |

## Verification Summary

| Command / Method | Result | Evidence | Notes |
|------------------|--------|----------|-------|
| `cd simulator && mvn -Dtest=FaultVectorProviderTest,ExecutionPlanDynamicEvidenceTest test` | PASS | `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`; finished `2026-07-08T19:48:06+01:00`. | Validates provider scope/signal/CSV behavior and dynamic-evidence regression. Expected recorder-warning stack traces are from tests that assert recorder failures are swallowed. |
| `cd simulator && mvn -DskipTests install` | PASS | `BUILD SUCCESS`; installed `MicroservicesSimulator-3.2.0-SNAPSHOT`; finished `2026-07-08T19:48:37+01:00`. | Refreshes local simulator dependency for non-aggregated `verifiers/` tests. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test` | PASS | `Tests run: 35, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`; finished `2026-07-08T19:49:22+01:00`. | Validates executor behavior, report metadata fix, negative statuses, and orchestrator vector propagation. |
| `bash -n verifiers/scripts/run-scenario-executor.sh` | PASS | No output. | Shell syntax validation. |
| `docker compose config --quiet` | PASS | No output. | Compose parses without requiring global `CATALOG_PATH`. |
| Live stale docs searches | PASS | No output for live `scenario-executor-poc`, `ScenarioExecutor POC`, `generated fault injection`, or broader live `POC` searches. | Archive historical references excluded where appropriate. |
| `[ -e CONTEXT.md ] && echo ROOT_CONTEXT_EXISTS || echo NO_ROOT_CONTEXT` | PASS | `NO_ROOT_CONTEXT`. | Confirms verifier glossary routing did not create a root `CONTEXT.md`. |
| `./scripts/verifier-docs build` | PASS | Built docs to `target/verifier-docs-site` in 1.58s. | MkDocs Material 2.0 warning is informational. |
| Python S6 report/catalog assertions | PASS | `Scenario executor S6 report assertions passed for default, explicit, invalid, and selected catalog plan`. | Checked terminal/lifecycle/vector/provider/slot/step fields and repaired runtime metadata fields. |

## End-to-End / Integration Check

- Status: `PASS`
- Evidence: QA validated the regenerated S6 Docker smoke reports and selected catalog plan:
  - `verifiers/target/scenario-executor/s6-default-report.json`: `SUCCESS`, `COMMITTED`, `DEFAULT_VECTOR`, vector `0`, `IN_MEMORY_FAULT_VECTOR`, slot `NOT_ASSIGNED`, step `COMPLETED`, runtime metadata `applicationBase=quizzes`, `applicationId=quizzes`, `springProfiles=test,sagas,local`, `mavenProfile=test-sagas`.
  - `verifiers/target/scenario-executor/s6-explicit-report.json`: `FAULT_COMPENSATED`, `COMPENSATED`, `EXPLICIT_VECTOR`, vector `1`, `IN_MEMORY_FAULT_VECTOR`, slot `REALIZED`, step `INJECTED_FAULT`, same stable runner metadata.
  - `verifiers/target/scenario-executor/s6-invalid-report.json`: `INVALID_FAULT_VECTOR`, `NOT_STARTED`, `EXPLICIT_VECTOR`, vector `1`, `providerMode=NONE`, blocker `MISSING_EXPLICIT_SCENARIO_ID`, same stable runner metadata.
  - `verifiers/target/quizzes-20260708-163552-193/scenario-catalog.jsonl`: selected plan `910f72907e0d901bc5d35e0ecea03ec920b7ffb63929bbba1bfdba4fe531e195` has `faultSpace.length=1`, `defaultVector=0`, and the scheduled step id used by the reports.
- Gaps: QA did not rerun the heavy Docker smoke containers. Existing S6/repair artifacts were regenerated before this QA run and were field-checked with strict assertions.

## Unowned Changes Audit

Current `git diff`/`git status` has no uncommitted feature source/test/docs changes except this refreshed `qa-report.md`. Feature implementation is already in recent commits (`e7455944`, `eb93258a`, `0e980a8c`, `8b607895`, `b40dc6d7`) and reconciles with slice evidence.

| Path | Status | Owner / Evidence | Notes |
|------|--------|------------------|-------|
| `issues/2026-07-07-scenario-executor-fault-vectors/qa-report.md` | owned | This QA run | The only file QA writes. |
| `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/*`, `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/ExecutionPlan.java`, `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/FaultVectorProviderTest.java` | owned | Slice 001, commit `e7455944`, PASS review | Simulator provider/API/CSV boundary. |
| `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/*`, `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/*`, `verifiers/src/test/java/.../FixtureWorkflow.java` | owned | Slices 002-005, commit `eb93258a`, PASS reviews | Executor v2 report, vector validation, lifecycle, fault handling, negative statuses. |
| `verifiers/scripts/run-scenario-executor.sh`, `docker-compose.yml`, `ScenarioExecutorOrchestrator*` | owned | Slice 006, commit `0e980a8c`, PASS review | Runner/Docker vector propagation. |
| `docs/verifiers-impl/reference/scenario-executor.md`, removed `reference/scenario-executor-poc.md`, `mkdocs.verifier.yml`, live verifier docs/glossary/evidence/current-state` | owned | Slice 007, commit `8b607895`, PASS review | Docs rename and supported-scope language. |
| `issues/2026-07-07-scenario-executor-fault-vectors/**` | owned | Spec/plan/execution/review evidence, commits `b4d3417c` and `b40dc6d7` | Package artifacts and previous QA evidence. |
| `.claude/skills/**`, `CLAUDE.md` | unrelated-existing | None in this feature package | Deleted in current working tree; accepted per user because this is not a PR flow. |
| `docs/verifiers-impl/archive/meeting-notes/index.md`, `docs/verifiers-impl/archive/meeting-notes/2026-W27-thesis-meeting-outcomes.md` | unrelated-existing / unclear | None in this feature package | Dirty/untracked archive meeting-note work; not treated as a feature blocker for this non-PR QA. |
| `André_Silva___IST_UL___MEIC_PIC2.pdf` | unrelated-existing | None in this feature package | Untracked thesis PDF. |
| `issues/dynamic-unmatched-fixture-diagnostics/`, `issues/missing-saga-input-coverage-post-helper-fix-audit/`, `issues/scenario-executor-readiness-alignment/`, `issues/static-event-semantics-for-verifier-inputs/*` | unrelated-existing | None in this feature package | Untracked unrelated issue-package material. |
| `thesis-drafts/*` | unrelated-existing | None in this feature package | Untracked thesis drafts. |

## Scope and Non-Goal Audit

- No evidence found of adding multi-saga execution, TCC runtime execution, stream/gRPC/distributed parity, compensation-step fault injection, delay or non-binary vector impairments, batch/vector-set execution, vector search/prioritization, domain-impact scoring, generic reset, CSV fault-vector input/output, catalog mutation, or dynamic-enrichment feedback.
- The v2 executor report intentionally breaks the old report schema, matching the spec.
- Documentation keeps unsupported/future areas explicit.

## Docs / Context / ADR Drift

- `CONTEXT-MAP.md` correctly routes verifier terminology to `docs/verifiers-impl/glossary.md`; QA confirmed no root `CONTEXT.md` exists.
- `docs/verifiers-impl/glossary.md`, `current-state.md`, `reference/scenario-executor.md`, and `evidence.md` now align with the supported single-saga default/explicit fault-vector path.
- No ADR is required by the spec or plan.
- No docs/context drift remains for the prior AC-18 metadata issue; docs describe runner metadata and reports now contain those fields.

## Findings

None

## Blockers

- None.

## Remaining Work

- None for the feature package.
- If a PR is later desired, isolate or clean unrelated dirty worktree changes first; this is intentionally outside the current non-PR QA request.

## Final Recommendation

Treat the feature package as complete for the supported non-PR workflow. Do not do more implementation work for this package. If the goal later changes to a PR, first create a clean branch/diff boundary around the committed feature and handle the unrelated dirty files separately.
