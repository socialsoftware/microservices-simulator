# Slice Review: 006 - CLI/Docker Runner and Quizzes Smoke

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes`

## Sources Reviewed

- Slice: `issues/2026-07-07-scenario-executor-fault-vectors/006-cli-docker-runner-and-quizzes-smoke.md`
- Spec: `issues/2026-07-07-scenario-executor-fault-vectors/spec.md`
- Implementation plan: `issues/2026-07-07-scenario-executor-fault-vectors/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`
- ADRs: None
- Completion evidence: `issues/2026-07-07-scenario-executor-fault-vectors/006-cli-docker-runner-and-quizzes-smoke.md#completion-evidence`
- Changed files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestrator.java`, `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestratorSpec.groovy`, `verifiers/scripts/run-scenario-executor.sh`, `docker-compose.yml`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorCli.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOptions.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`, `applications/quizzes/pom.xml`, `verifiers/target/quizzes-20260708-163552-193/scenario-catalog.jsonl`, `verifiers/target/scenario-executor/s6-default-report.json`, `verifiers/target/scenario-executor/s6-explicit-report.json`, `verifiers/target/scenario-executor/s6-invalid-report.json`
- Prior review reports: dependency latest reports `review/001-...` through `review/005-...` checked for `PASS` / moved-to-done status; no prior `006` review existed.
- Commands run by reviewer: `bash -n verifiers/scripts/run-scenario-executor.sh`; `docker compose config --quiet`; `cd verifiers && mvn -Dtest=ScenarioExecutorOrchestratorSpec test`; `python3` report-field assertions for the three S6 reports; `python3` selected-plan catalog lookup; `git diff --check -- docker-compose.yml verifiers/scripts/run-scenario-executor.sh verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestrator.java verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOrchestratorSpec.groovy`

## Summary

The slice satisfies the runner/compose/orchestrator propagation contract and has credible Quizzes smoke artifacts. I reran the focused orchestrator test, shell syntax check, compose config parse, and report assertions. I did not rerun the heavy Docker smokes; the recorded reports under `verifiers/target/scenario-executor/` match the required terminal statuses and slot outcomes.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | pass | `ScenarioExecutorOrchestrator` appends `--fault-vector` only when configured; `run-scenario-executor.sh` reads `FAULT_VECTOR`, logs vector source, and passes `--fault-vector`; `docker-compose.yml` exposes `FAULT_VECTOR`; S6 reports show default, explicit, and invalid-vector paths. |
| Slice out-of-scope respected | pass | No vector generation/search or CSV synthesis was added. The runner forwards one vector only and writes reports under `/reports/scenario-executor`, mounted to `verifiers/target/`. |
| Spec non-goals respected | pass | No multi-saga/TCC/distributed runner behavior, batch execution, database reset orchestration, or docs rename was introduced in this slice. |
| Dependencies done | pass | `done/001` through `done/005` exist and latest dependency review reports show `PASS` and moved-to-done. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-1 | pass | CLI option exists in `ScenarioExecutorCli`/`ScenarioExecutorOptions`; orchestrator command includes `--fault-vector 010` in the rerun Spock test; script/compose expose `FAULT_VECTOR`; `s6-invalid-report.json` has `terminalStatus=INVALID_FAULT_VECTOR`, `providerMode=NONE`, blocker `MISSING_EXPLICIT_SCENARIO_ID`. | Implementer evidence records the invalid Docker wrapper exit as `EXIT_CODE=1`. |
| AC-2 | pass | `s6-default-report.json` has `assignedVector=0`, `vectorSource=DEFAULT_VECTOR`, `providerMode=IN_MEMORY_FAULT_VECTOR`, `terminalStatus=SUCCESS`, `lifecycleOutcome=COMMITTED`; catalog plan `910f...e195` has `faultSpace.defaultVector=0`. | Default smoke used pinned scenario id, which the slice explicitly allowed. |
| AC-11 | pass | `s6-explicit-report.json` has `assignedVector=1`, `vectorSource=EXPLICIT_VECTOR`, `terminalStatus=FAULT_COMPENSATED`, `lifecycleOutcome=COMPENSATED`, slot 0 `REALIZED`, and step 0 `INJECTED_FAULT`. | Quizzes smoke proves the explicit single-bit vector realizes a fault in the supported path. |
| AC-20 | pass | `ScenarioExecutorCli.exitCodeFor` maps `SUCCESS`, `FAULT_COMPENSATED`, and `DRY_RUN` to zero and all other statuses to one; completion evidence records zero exits for default/explicit smokes and non-zero for missing scenario id. | Reviewer inspected reports but did not rerun Docker exit-code commands. |
| AC-22 | pass | Recorded Quizzes catalog `verifiers/target/quizzes-20260708-163552-193/scenario-catalog.jsonl` contains plan `910f72907e0d901bc5d35e0ecea03ec920b7ffb63929bbba1bfdba4fe531e195` with one fault slot; S6 default and explicit reports prove `SUCCESS` and `FAULT_COMPENSATED` under the runner path. | `applications/quizzes/pom.xml` has `test-sagas` activating `test,sagas,local`; compose/script default to that profile. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| `bash -n verifiers/scripts/run-scenario-executor.sh` | pass | No syntax output. |
| `docker compose config --quiet` | pass | Compose parses without requiring global `CATALOG_PATH`, so validation is now delegated to the runner script. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorOrchestratorSpec test` | pass | `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`; build success. |
| `git diff --check -- <S6 files>` | pass | No whitespace/error output. |
| Default Docker smoke report | pass | Docker command not rerun by reviewer. Existing `s6-default-report.json` asserts `SUCCESS`, `COMMITTED`, `DEFAULT_VECTOR`, provider `IN_MEMORY_FAULT_VECTOR`. |
| Explicit Docker smoke report | pass | Docker command not rerun by reviewer. Existing `s6-explicit-report.json` asserts `FAULT_COMPENSATED`, `COMPENSATED`, `EXPLICIT_VECTOR`, slot 0 `REALIZED`, step 0 `INJECTED_FAULT`. |
| Explicit vector without scenario id report | pass | Existing `s6-invalid-report.json` asserts `INVALID_FAULT_VECTOR`, `NOT_STARTED`, `providerMode=NONE`, blocker `MISSING_EXPLICIT_SCENARIO_ID`. |
| Selected catalog/plan evidence | pass | Reviewer catalog lookup found the selected plan with fault-space length 1, default vector `0`, and scheduled step `ebeca9bce8f36249b8631ec2ab1024ade807c4845f3379d5690aafa89d842127`. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Source changes are limited to orchestrator command construction, orchestrator test, runner script, and compose env wiring for this slice. |
| Existing patterns | pass | Uses existing Java record config, shell array argument construction, compose env interpolation, and `/reports` mount convention. |
| Test quality | pass | Focused Spock coverage proves orchestrator propagation; actual S6 report artifacts cover default/explicit/invalid runner behavior. |
| Regression risk | pass | Missing `CATALOG_PATH` now fails explicitly in the script with exit 2; compose still parses; script strict mode remains enabled. |
| Security/data safety | pass | No new secrets, network exposure, destructive git operations, or catalog mutation path found. Outputs are confined to the mounted reports directory. |
| Change hygiene | pass | No new dependencies or broad refactors; targeted diff check is clean. |

## Findings

None

## Done Transition

- Moved to done: `yes`
- Done path: `issues/2026-07-07-scenario-executor-fault-vectors/done/006-cli-docker-runner-and-quizzes-smoke.md`
- Reason if not moved: `None`

## Reviewer Notes

Docker smokes were not rerun during review because they are heavier and current artifacts are present, timestamped after the selected catalog, and field-checked. The next slice can proceed to docs cleanup.
