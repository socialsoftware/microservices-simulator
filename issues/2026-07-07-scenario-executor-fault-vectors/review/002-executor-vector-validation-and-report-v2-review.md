# Slice Review: 002 - Executor Vector Validation and Report V2

## Review Attempt

Attempt: `02`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes`

## Sources Reviewed

- Slice: `issues/2026-07-07-scenario-executor-fault-vectors/002-executor-vector-validation-and-report-v2.md` (reviewed active path before move)
- Spec: `issues/2026-07-07-scenario-executor-fault-vectors/spec.md`
- Implementation plan: `issues/2026-07-07-scenario-executor-fault-vectors/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`
- ADRs: None
- Completion evidence: `issues/2026-07-07-scenario-executor-fault-vectors/002-executor-vector-validation-and-report-v2.md#completion-evidence`
- Changed files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOptions.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorCli.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioCatalogReader.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioMaterializer.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/FaultSpace.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/ScheduledStep.java`, `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/ScenarioPlan.java`, `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`, `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java`
- Prior review reports: `issues/2026-07-07-scenario-executor-fault-vectors/review/002-executor-vector-validation-and-report-v2-review-01.md`, `issues/2026-07-07-scenario-executor-fault-vectors/review/001-simulator-in-memory-fault-provider-review.md`
- Commands run by reviewer: `git status --short`; `find issues/2026-07-07-scenario-executor-fault-vectors/review -maxdepth 1 -type f -name '002-executor-vector-validation-and-report-v2-review*.md' -printf '%f\n' | sort && test -e issues/2026-07-07-scenario-executor-fault-vectors/done/002-executor-vector-validation-and-report-v2.md && echo DONE_COLLISION || echo NO_DONE_COLLISION`; `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test`; `rg -n "faultVector|exitCodeFor|validateVector|runtimeMetadata|STEP_EXECUTION_FAILED|MISSING_EXPLICIT_SCENARIO_ID|attemptedVector|DRY_RUN|RuntimeMetadata|FaultSlot|schemaVersion|providerMode|assignedVector" ...`

## Summary

The slice now satisfies the executor-side vector validation and v2 dry-run/report contract for S2 scope. The previous blocking findings are fixed: invalid-vector reports preserve the attempted explicit/default vector where known, and runtime metadata records the requested dry-run mode instead of deriving it from terminal status. Targeted verifier tests pass, no done-path collision exists, and the implementation stays within the slice boundary by not installing the provider or changing lifecycle/compensation behavior reserved for later slices.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | pass | `ScenarioExecutorOptions` carries `faultVector`; `ScenarioExecutorCli` parses `--fault-vector`; `ScenarioExecutor.validateVector(...)` validates vectors/fault-space mappings and expands slots; `ScenarioExecutionReport` exposes the v2 schema fields. |
| Slice out-of-scope respected | pass | Source review found no executor provider installation, no no-fault lifecycle closure change, no realized-fault compensation/masking implementation, no Docker `FAULT_VECTOR` propagation, and no docs rename. Existing real execution failure/status behavior remains for later slices. |
| Spec non-goals respected | pass | No multi-saga/TCC/runtime parity expansion, vector batching/search, CSV vector files, database reset, dynamic sidecar mutation, or v1 compatibility layer was added. |
| Dependencies done | pass | S1 is present at `issues/2026-07-07-scenario-executor-fault-vectors/done/001-simulator-in-memory-fault-provider.md` and has PASS review `review/001-simulator-in-memory-fault-provider-review.md`. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-1 | pass | `ScenarioExecutorOptions.faultVector`, `ScenarioExecutorCli` option plumbing, and `ScenarioExecutorSpec` explicit-vector/no-id test cover the executor CLI surface and explicit id rule. | Docker `FAULT_VECTOR` / `SCENARIO_ID` env propagation is explicitly S6, not S2. |
| AC-2 | pass | `ScenarioExecutor.validateVector(...)` uses `faultSpace.defaultVector()` when no explicit vector is supplied; tests assert `assignedVector == '01'` and `vectorSource == DEFAULT_VECTOR`. | CSV suppression for real executor-owned provider runs is distributed across S1/later execution slices. |
| AC-3 | pass | Tests cover wrong vector length, non-binary explicit vector, invalid default vector, fault-space length mismatch, duplicate scheduled step ids, unresolved scheduled step id, duplicate/non-unique mapping, and invalid non-zero-space empty explicit vector before execution. | Fixture step calls remain empty in invalid-vector cases. |
| AC-4 | pass | Dry-run path returns `DRY_RUN`, `providerMode = NONE`, expanded slots, dry-run step outcomes, and no fixture execution; source review shows materialization/execution only occurs after the dry-run branch. | Provider installation is out of scope and not present. |
| AC-17 | pass | Fault slots are constructed by fault-space index and step outcomes by `scheduleOrder`; tests assert slot indexes `[0, 1]`, scheduled step ids, runtime step names, assigned bits, and dry-run step outcome ordering. | Later realization/masking states are S4/S5 scope. |
| AC-18 | pass | V2 report includes runtime metadata for catalog path/kind, scenario plan id, vector source, executor mode, and requested dry-run flag; the review-fix tests assert invalid reports keep `assignedVector` and `runtimeMetadata.dryRun`. | Full Docker/app profile metadata can be expanded in S6 if needed; this slice does not snapshot DB/application state. |
| AC-19 | pass | `writeReport(...)` writes only `options.outputPath`; the dry-run test compares catalog content before/after and confirms it is unchanged. | Source review found no catalog/enriched/dynamic sidecar writes. |
| AC-20 | pass | `ScenarioExecutorCli.exitCodeFor(...)` returns zero for `SUCCESS`, `FAULT_COMPENSATED`, and `DRY_RUN`, and non-zero for invalid/broken statuses; `ScenarioExecutorSpec` covers representative statuses. | The helper is testable without invoking `System.exit`. |
| AC-21 | pass | `ScenarioExecutorSpec` provides this slice's dummyapp-style coverage for validation, defaulting, malformed mappings, dry-run, no-execution, report fields, no-mutation, and exit mapping. | Provider cleanup, compensation, masking, mismatch, CSV override, and unsupported-shape fault states remain assigned to later slices. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | pass | Reviewer rerun passed: `Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, finished `2026-07-08T16:41:16+01:00`. |
| CLI exit-code foundation | pass | Covered by `ScenarioExecutorSpec`; source review confirms `SUCCESS`, `FAULT_COMPENSATED`, and `DRY_RUN` map to exit code 0 and all other statuses default non-zero. |
| Previous review finding: attempted vector missing in invalid reports | pass | `ScenarioExecutor` now passes `options.faultVector()` for explicit-vector/no-id reports and `attemptedVector(record, options)` for selected invalid vectors; tests assert invalid explicit/default `assignedVector` values. |
| Previous review finding: dry-run metadata derived from terminal status | pass | `report(...)` receives and serializes `options.dryRun()` in `RuntimeMetadata`; invalid-vector tests assert `runtimeMetadata().dryRun()`. |
| Done-path collision check | pass | Reviewer command reported `NO_DONE_COLLISION` for `issues/2026-07-07-scenario-executor-fault-vectors/done/002-executor-vector-validation-and-report-v2.md`. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Changes are limited to executor options/CLI/executor/report and focused executor tests, plus fixture counters used by existing executor tests. |
| Existing patterns | pass | Keeps the existing catalog reader, materializer, runtime-step derivation, record-style report model, and Spock executor spec style. |
| Test quality | pass | Tests assert behavior, report fields, and no-execution/no-mutation effects rather than only implementation details; review-specific metadata regressions are now covered. |
| Regression risk | pass | Targeted executor regression suite passes; old constructor shape for `ScenarioExecutorOptions` is preserved for existing callers. |
| Security/data safety | n/a | No migrations, destructive operations, external calls, secret handling, or new dependencies were introduced. |
| Change hygiene | pass | Review observed unrelated pre-existing worktree changes and did not modify source/planning artifacts; only review artifacts and the PASS move are part of this review. |

## Findings

None

## Done Transition

- Moved to done: `yes`
- Done path: `issues/2026-07-07-scenario-executor-fault-vectors/done/002-executor-vector-validation-and-report-v2.md`
- Reason if not moved: `None`

## Reviewer Notes

S2 intentionally leaves real provider installation, no-fault lifecycle closure, realized fault compensation/masking, mismatch/failure terminal statuses, Docker env propagation, and docs rename to later slices. This review only passes the vector validation, dry-run/no-execution, v2 report foundation, and exit-code foundation required by S2.
