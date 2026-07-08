# Slice Review: 002 - Executor Vector Validation and Report V2

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`FAIL`

Moved to done: `no`

## Sources Reviewed

- Slice: `issues/2026-07-07-scenario-executor-fault-vectors/002-executor-vector-validation-and-report-v2.md`
- Spec: `issues/2026-07-07-scenario-executor-fault-vectors/spec.md`
- Implementation plan: `issues/2026-07-07-scenario-executor-fault-vectors/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`
- ADRs: None
- Completion evidence: `issues/2026-07-07-scenario-executor-fault-vectors/002-executor-vector-validation-and-report-v2.md#completion-evidence`
- Changed files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOptions.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorCli.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java`, `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`, `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java`
- Prior review reports: `issues/2026-07-07-scenario-executor-fault-vectors/review/001-simulator-in-memory-fault-provider-review.md` for dependency status; no prior S2 review reports
- Commands run by reviewer: `git status --short`; `find issues/2026-07-07-scenario-executor-fault-vectors/review -maxdepth 1 -type f -name '002-executor-vector-validation-and-report-v2-review*.md' -print | sort && test -e issues/2026-07-07-scenario-executor-fault-vectors/done/002-executor-vector-validation-and-report-v2.md && echo DONE_COLLISION || true`; `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`; `rg -n "ScenarioExecutionReport\\(|terminalStatus\\(|schemaVersion|execution-report|scenario-execution-report\\.v" verifiers/src/main verifiers/src/test docs/verifiers-impl -g '!target/**'`; `nl -ba .../ScenarioExecutor.java .../ScenarioExecutorCli.java .../ScenarioExecutionReport.java`

## Summary

The implementation covers the main validation, dry-run, deterministic slot mapping, CLI option, and exit-code foundation, and the targeted `ScenarioExecutorSpec` command passes. The slice cannot pass yet because the v2 report contract loses required reproducibility metadata on invalid-vector paths: invalid reports drop the assigned vector value, and runtime metadata reports `dryRun=false` whenever terminal status is not `DRY_RUN`, even if the user requested a dry-run attempt that failed validation/selection.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | fail | Core validation/dry-run scope is implemented, but v2 report metadata is incomplete for invalid-vector reports (`ScenarioExecutor.java:32-33`, `57-58`, `270-287`). |
| Slice out-of-scope respected | pass | No provider installation, lifecycle closure, compensation, Docker `FAULT_VECTOR`, or docs rename work was added in this slice. |
| Spec non-goals respected | pass | No batch/vector search, CSV vector files, multi-saga execution expansion, DB reset, or backward-compatible v1 schema layer was added. |
| Dependencies done | pass | Dependency S1 is in `done/001-simulator-in-memory-fault-provider.md` with PASS review `review/001-simulator-in-memory-fault-provider-review.md`. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-1 | pass | `ScenarioExecutorOptions` adds `faultVector`; `ScenarioExecutorCli` passes `options.get("fault-vector")`; `ScenarioExecutorSpec` covers explicit vector without scenario id as `INVALID_FAULT_VECTOR`. | Docker `FAULT_VECTOR` propagation is explicitly S6, not required for this slice. |
| AC-2 | pass | `validateVector(...)` defaults to `faultSpace.defaultVector()` when no explicit vector is present; test asserts `assignedVector == '01'` and `vectorSource == DEFAULT_VECTOR`. | Full executor-owned provider/CSV behavior is later-slice scope. |
| AC-3 | pass | Tests cover wrong length, non-binary, invalid default vector, fault-space length mismatch, duplicate scheduled step ids, unresolved scheduled step id, non-unique mapping, and invalid explicit empty vector before execution. | Rejection behavior is credible. |
| AC-4 | pass | Dry-run branch returns before materialization/execution, uses provider mode `NONE`, emits slot mapping and dry-run step outcomes; test asserts fixture steps remain empty and catalog content unchanged. | Source inspection confirms no provider installation in this slice. |
| AC-17 | pass | `validateVector(...)` builds slots by fault-space index; dry-run step outcomes use schedule-order-sorted `candidate.steps()`. Tests assert slot indexes and step statuses. | Later lifecycle/fault states remain later slices. |
| AC-18 | fail | `ScenarioExecutor.report(...)` sets `RuntimeMetadata.dryRun` from terminal status only (`"DRY_RUN".equals(status)`), not the requested executor option, and invalid-vector branches pass `assignedVector = null`. | Invalid dry-run/invalid-vector reports are not sufficient to reproduce the attempt. |
| AC-19 | pass | `writeReport(...)` writes only `options.outputPath`; dry-run test compares catalog content before/after. | Source review found no catalog/enriched/sidecar writes. |
| AC-20 | pass | `ScenarioExecutorCli.exitCodeFor(...)` maps `SUCCESS`, `FAULT_COMPENSATED`, and `DRY_RUN` to zero and all other statuses to non-zero; targeted test covers representative values. | More statuses are later-slice scope. |
| AC-21 | pass | `ScenarioExecutorSpec` now covers this slice's dummyapp-style validation/defaulting/malformed mapping/dry-run/report/exit-code states. | Full AC-21 fault compensation/mismatch/CSV coverage is intentionally distributed over S3-S5. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | pass | Reviewer rerun passed: `Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, finished `2026-07-08T16:32:58+01:00`. |
| CLI exit-code foundation | pass | Covered by `ScenarioExecutorSpec` and source review of `ScenarioExecutorCli.exitCodeFor(...)`. |
| v2 invalid-report metadata | fail | Existing tests do not assert assigned vector value or dry-run flag for invalid reports; source shows the metadata is currently wrong/missing. |
| Done-path collision check | pass | No existing `done/002-executor-vector-validation-and-report-v2.md` was present. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Source changes are limited to executor options/CLI/executor/report and focused executor specs. |
| Existing patterns | pass | Keeps current executor selection/materialization structure and record-style report model. |
| Test quality | fail | The new tests cover the main happy/validation paths, but miss invalid-report metadata that the v2 contract requires for reproducibility. |
| Regression risk | fail | Future runner/search logic would be unable to reconstruct invalid attempts from reports that omit the explicit/default vector and misreport requested dry-run mode. |
| Security/data safety | n/a | No migrations, external services, secrets, or destructive operations were introduced. |
| Change hygiene | pass | Review only observed unrelated existing worktree changes and did not modify source. |

## Findings

| Severity | Finding | Evidence | Required Fix |
|----------|---------|----------|--------------|
| blocking | Invalid-vector reports drop the assigned vector value, so the v2 report does not carry the vector metadata needed to inspect/reproduce invalid attempts. | `ScenarioExecutor.java:32-33` builds the explicit-vector/no-id report with `assignedVector = null`; `ScenarioExecutor.java:57-58` does the same for selected plans with invalid explicit/default vectors; `ScenarioExecutor.java:270-287` serializes that null directly. | Preserve and report the attempted vector value whenever known: explicit `options.faultVector()` for explicit-vector invalids, and the selected plan's `faultSpace.defaultVector()` for default-vector invalids/malformed mappings. Add assertions for invalid explicit and invalid default reports. Null is acceptable only when no vector value exists. |
| blocking | Runtime metadata `dryRun` is derived from terminal status instead of the requested executor mode, so dry-run attempts that fail validation/selection are reported as non-dry-run. | `ScenarioExecutor.java:277-284` creates `RuntimeMetadata` with `"DRY_RUN".equals(status)`. Any `options.dryRun() == true` attempt that returns `INVALID_FAULT_VECTOR`, `SELECTION_FAILED`, or another pre-execution status will serialize `dryRun=false`. | Pass the requested `options.dryRun()` into report construction and serialize that value in `RuntimeMetadata.dryRun`. Add a test for a dry-run invalid-vector or dry-run selection-failure report. |

## Done Transition

- Moved to done: `no`
- Done path: `None`
- Reason if not moved: `FAIL` due blocking v2 report metadata findings above.

## Reviewer Notes

The main executor validation and dry-run implementation is close. Keep the fix narrow: do not start S3/S4 lifecycle/provider work here; only preserve the attempted vector value and requested dry-run flag in v2 reports, then extend the existing focused Spock coverage.
