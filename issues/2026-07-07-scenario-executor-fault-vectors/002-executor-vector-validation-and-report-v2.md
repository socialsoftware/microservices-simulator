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
