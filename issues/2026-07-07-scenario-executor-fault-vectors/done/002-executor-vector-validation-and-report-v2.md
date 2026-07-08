# 002 - Executor Vector Validation and Report V2

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `001-simulator-in-memory-fault-provider.md`  
ACs covered: `AC-1, AC-2, AC-3, AC-4, AC-17, AC-18, AC-19, AC-20, AC-21`  
Risk: `high`

## Purpose

Add the executor-side vector contract before real fault execution: accept/default one assigned vector, validate it against the selected plan's fault space and expanded schedule, expand deterministic fault slots, write a standalone v2 report, and enforce dry-run/no-execution behavior.

## Scope

- Extend executor options and CLI parsing with `--fault-vector`.
- Require explicit `--scenario-id` when `--fault-vector` is supplied.
- Preserve auto-selection only for runs with no explicit vector, using each selected plan's `faultSpace.defaultVector`.
- Validate explicit and default vectors before provider installation, materialization, execution, or compensation.
- Validate fault-space structure: length/ids size match, binary vector, correct vector length, duplicate scheduled step ids, unresolved scheduled step ids, non-forward/duplicate step mapping, and explicit empty vector only when fault-space length is zero.
- Expand every bit into deterministic fault-slot entries ordered by slot index.
- Introduce the v2 report contract with schema version, `scenarioExecutionId`, vector metadata, provider mode, runtime metadata, slot mapping, terminal/lifecycle status fields, blockers/errors, skipped counts, and ordered forward step outcomes.
- Make dry-run return `DRY_RUN`, provider mode `NONE`, expanded slot mapping, and no materialization/execution/compensation.
- Preserve report-only side effects: do not mutate catalog/enriched/dynamic sidecar files.
- Add exit-code mapping foundations for exact terminal statuses; later slices add more non-zero statuses.

## Out of Scope

- Installing the provider for real execution.
- Changing no-fault commit closure, realized fault compensation, or mismatch/failure handling.
- Docker `FAULT_VECTOR` env propagation; that is S6.
- Documentation rename; that is S7.

## Repo Anchors

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOptions.java` — add vector input to executor options.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorCli.java` — parse `--fault-vector`, enforce explicit id rule, and map terminal statuses to process exit codes.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — central selection, validation, dry-run, and report writing path.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java` — replace/evolve v1 record into v2 report schema.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/FaultSpace.java` and `ScheduledStep.java` — source of truth for fault-slot mapping.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — vector validation, dry-run, and report tests.
- `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java` — extend fixture counters to prove no dry-run execution/compensation.

## Implementation Shape

- Keep validation in verifier/executor code because it maps catalog identities to runtime provider values. Keep provider values plain when passed into simulator APIs.
- Create a deterministic slot-expansion step that resolves each `faultSpace.scheduledStepIds[index]` to exactly one forward `expandedSchedule` entry and derives the current runtime step name with the existing `runtimeStepName(...)` rules or a tightened equivalent.
- Treat validation failures as reportable `INVALID_FAULT_VECTOR` outcomes when a report can be written.
- Generate `scenarioExecutionId` inside the executor for every attempt, including dry-runs and validation failures.
- Use exact enum/string values from the spec for `vectorSource`, `providerMode`, `terminalStatus`, `lifecycleOutcome`, and fault-slot realization state.
- Preserve deterministic ordering: slot mapping by slot index, step outcomes by `scheduleOrder`.
- Keep old v1 compatibility only if trivial; the spec allows breaking the old report schema.

## TDD / Test Shape

- First behavior to test: an explicit vector without an explicit scenario id produces `INVALID_FAULT_VECTOR` before materialization/provider/execution.
- Expected red failure: current options/CLI have no fault-vector field and reports are v1 without vector data.
- Additional coverage:
  - wrong vector length;
  - non-binary characters;
  - invalid default vector;
  - `faultSpace.length != scheduledStepIds.size()`;
  - duplicate `scheduledStepIds`;
  - unresolved scheduled step id;
  - duplicate/non-unique mapping to expanded schedule;
  - empty vector valid only for zero-length fault space;
  - dry-run expands all slots and does not execute/compensate;
  - default vector reports `vectorSource = DEFAULT_VECTOR`;
  - explicit vector reports `vectorSource = EXPLICIT_VECTOR`;
  - output report does not mutate input catalog/enriched/sidecar artifacts.

## Just-in-Time Preflight Required

Before source edits, the executor must re-check:

- Current constructor/signature usages of `ScenarioExecutorOptions` in tests and production.
- Current `ScenarioExecutionReport` JSON consumers in docs/tests/scripts.
- Current `ScenarioExecutor.runtimeStepName(...)` behavior and step-id patterns generated by scenario catalog tests.
- Whether `ScenarioExecutorCli.main` can safely call `System.exit(...)` or should delegate status calculation to a testable helper.

## Verification

- `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` — vector validation, dry-run, report mapping, and no-mutation tests pass.
- CLI-focused unit test or `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec test` if CLI status helpers are tested there — exit-code mapping foundations pass.

## Evidence to Record

- files changed
- commands run and outputs
- sample v2 dry-run report excerpt
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- Do not install the simulator provider during dry-run or invalid-vector paths.
- Do not materialize inputs before vector validation succeeds.
- Do not let auto-selection with default vectors hide explicit-vector validation errors.
- The v2 report should be stable enough for later search/scoring; avoid ad hoc free-text status values where the spec enumerates machine values.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Review fix after `review/002-executor-vector-validation-and-report-v2-review-01.md`: invalid-vector reports now preserve the attempted assigned vector when known, and runtime metadata now records the requested dry-run mode instead of deriving it from terminal status.
- Added executor fault-vector input and CLI parsing for `--fault-vector`, with explicit vectors rejected as `INVALID_FAULT_VECTOR` when no explicit scenario id is supplied.
- Replaced the executor report schema with v2 fields for `scenarioExecutionId`, vector source/value, provider mode, lifecycle status, runtime metadata, deterministic fault-slot mapping, ordered step outcomes, skipped counts, and blockers.
- Added pre-execution vector/fault-space validation for explicit/default vectors and slot-to-expanded-schedule mapping.
- Added dry-run behavior that expands slots and writes a report with provider mode `NONE` without materialization, execution, provider install, or compensation.
- Added terminal-status exit-code mapping foundation in `ScenarioExecutorCli.exitCodeFor(...)`.

### Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorOptions.java` — added optional `faultVector` option while preserving the existing constructor shape for current tests/callers.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorCli.java` — parses `--fault-vector` and maps terminal statuses to process exit codes via a testable helper.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — added attempt id generation, explicit/default vector validation, deterministic slot expansion, dry-run/no-execution path, v2 report writing, invalid-vector report outcomes, and review fixes for attempted-vector/dry-run metadata on invalid reports.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java` — evolved v1 report record to the v2 schema required by this slice.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — added coverage for explicit vector/no id, malformed vectors and fault spaces, zero-length vector, dry-run slot expansion/no mutation/no execution, default vector source, CLI exit mapping, and review assertions for invalid-report assigned vector plus requested dry-run metadata.
- `issues/2026-07-07-scenario-executor-fault-vectors/002-executor-vector-validation-and-report-v2.md` — recorded completion evidence only.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | PASS | Review-fix rerun reported `Tests run: 26, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS` at 2026-07-08T16:38:14+01:00. |
| `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` | PASS | Initial implementation run reported `Tests run: 26, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS` at 2026-07-08T16:28:15+01:00. |

### Acceptance Criteria Evidence

- AC-1: Executor options and CLI now accept `faultVector` / `--fault-vector`; explicit vectors without `scenarioId` produce `INVALID_FAULT_VECTOR` before execution. Docker `FAULT_VECTOR` remains explicitly out of scope for S6.
- AC-2: No-vector runs use `faultSpace.defaultVector`; tests assert `assignedVector = '01'` and `vectorSource = DEFAULT_VECTOR`.
- AC-3: Tests cover wrong vector length, non-binary vector, invalid default vector, fault-space length mismatch, duplicate scheduled step ids, unresolved scheduled step ids, non-unique expanded-schedule mapping, and invalid non-zero-space empty vector before execution.
- AC-4: Dry-run validates and expands fault slots with `providerMode = NONE`; fixture step calls remain empty, proving no saga execution in the covered path.
- AC-17: V2 report includes ordered `faultSlots` by slot index and ordered dry-run step outcomes by schedule order; tests assert slot indexes `[0, 1]`, scheduled step ids, runtime step names, and assigned bits.
- AC-18: V2 report includes runtime metadata fields for catalog path/kind, scenario id, vector source, executor mode, and requested dry-run flag without application/database state snapshots. Review-fix tests assert invalid dry-run reports retain `runtimeMetadata.dryRun = true` and preserve explicit/default attempted vector values.
- AC-19: Dry-run test reads the catalog before and after execution and asserts unchanged content; only the executor report is written.
- AC-20: `ScenarioExecutorCli.exitCodeFor(...)` returns zero for `SUCCESS`, `FAULT_COMPENSATED`, and `DRY_RUN`, and non-zero for invalid/broken statuses covered by the test.
- AC-21: Dummyapp-style fixture tests in `ScenarioExecutorSpec` cover the S2 validation, defaulting, malformed mapping, dry-run, no-execution, report, and exit-mapping states.

Sample v2 dry-run report excerpt from the covered test path:

```json
{
  "schemaVersion": "microservices-simulator.scenario-execution-report.v2",
  "terminalStatus": "DRY_RUN",
  "lifecycleOutcome": "NOT_STARTED",
  "scenarioPlanId": "dry-vector",
  "assignedVector": "10",
  "vectorSource": "EXPLICIT_VECTOR",
  "providerMode": "NONE",
  "faultSlots": [
    { "slotIndex": 0, "scheduledStepId": "dry-vector-step-1", "runtimeStepName": "first", "assignedBit": 1, "realizationState": "UNREALIZED" },
    { "slotIndex": 1, "scheduledStepId": "dry-vector-step-2", "runtimeStepName": "second", "assignedBit": 0, "realizationState": "NOT_ASSIGNED" }
  ]
}
```

### Browser / Manual Evidence

- Not required.

### TDD Notes

- Added focused Spock coverage for the first requested behavior (explicit vector without explicit scenario id) and the additional validation/dry-run/report cases before/with implementation. The final verification command above passes.

### Deviations From Plan

- Did not install the in-memory fault provider for real execution; this matches this slice's explicit out-of-scope item.
- Existing real execution still reports `SUCCESS` / `STEP_EXECUTION_FAILED` with `lifecycleOutcome = CLOSURE_SKIPPED`; lifecycle closure and fault execution terminal statuses are intentionally left to later slices.

### Blockers / Follow-Ups

- Review attempt 01 findings fixed: invalid-vector reports preserve attempted assigned vector values, and runtime metadata preserves requested dry-run mode for invalid reports.
- Later slices must install the simulator provider for real runs, close no-fault lifecycles, compensate realized faults, and propagate Docker `FAULT_VECTOR`.
