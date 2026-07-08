# Spec: Scenario Executor Fault Vectors

## Problem

The verifier can generate deterministic scenario plans with a `faultSpace`, and the ScenarioExecutor can run some materializable single-saga scenarios, but execution is still fault-free. The next thesis/runtime step needs the executor to evaluate an assigned fault vector over a scenario plan while preserving simulator-owned saga fault semantics, abort/compensation behavior, and reproducible reporting.

The executor must stop being treated as a temporary POC for this work. It should become a supported scenario-execution feature for the currently supported shape, while clearly stating what remains unsupported or future work.

## Solution

Add fault-vector execution to the ScenarioExecutor for materializable single-saga saga/local scenarios.

A scenario execution attempt evaluates exactly one `{scenarioPlanId, faultVector}` pair. The fault vector is an assigned binary string aligned by index to the selected plan's `faultSpace.scheduledStepIds`. The executor validates and expands the vector into fault-slot assignments, installs an execution-scoped simulator-owned in-memory fault provider, drives forward scheduled steps through simulator workflow boundaries, closes the saga lifecycle, and writes a standalone v2 execution report.

Fault injection remains simulator-owned. The verifier maps catalog identities into plain simulator provider values, but the simulator decides when to inject the fault inside `ExecutionPlan` and raises a machine-distinguishable injected-fault signal. Legacy CSV impairment behavior remains available for existing simulator/Quizzes tests and manual use, but it is not the ScenarioExecutor fault-vector contract.

## Domain References

- Glossary/context: `CONTEXT-MAP.md` routes verifier terminology to `docs/verifiers-impl/glossary.md`, which is the canonical glossary for Scenario Plan, Scenario Execution, Scenario Execution Attempt, Scheduled Step, Fault Space, Fault Slot, Fault Vector, Assigned Vector, Realized Fault Slot, and Masked Fault Slot.
- ADRs: None required.
- Decision frame: `issues/2026-07-07-scenario-executor-fault-vectors/decision-frame.md`

## Scope

### In Scope

- Execute one assigned binary fault vector for one selected scenario plan.
- Default to the selected plan's `faultSpace.defaultVector` when no explicit vector is provided.
- Require an explicit scenario id for explicit fault-vector runs.
- Preserve auto-selection only for default-vector smoke behavior.
- Validate selected fault vectors and fault-space mappings before saga execution.
- Expand every vector bit into a deterministic fault-slot mapping.
- Add a simulator-owned in-memory fault-provider contract using plain identifier values, not verifier model classes.
- Make vector-injected faults machine-distinguishable from ordinary runtime failures.
- Drive successful executions to commit/closure.
- Drive faulted executions to compensation completion.
- Report expected injected faults as valid experimental outcomes, not executor step failures.
- Report masked/unrealized assigned fault slots explicitly.
- Emit a standalone v2 executor report artifact without mutating scenario catalogs or dynamic-enrichment sidecars.
- Provide CLI/Docker invocation surface for explicit vectors.
- Preserve existing CSV/manual impairment behavior outside executor-owned vector runs.
- Update executor documentation so it is no longer described as a POC after this feature lands.
- Add broad dummyapp coverage for designed states and a small Quizzes Docker smoke for realistic end-to-end behavior.

### Out of Scope

- Multi-saga runtime execution.
- TCC runtime execution.
- Stream, gRPC, or distributed execution parity.
- Fault injection into compensation steps.
- Delay injection or non-binary impairment types.
- Batch/vector-set execution in one executor run.
- Choosing, generating, searching, deduplicating, or prioritizing fault vectors.
- GA/local search and contextual-bandit prioritization.
- Domain-impact scoring or impact-analysis telemetry.
- Feeding fault-execution outcomes back into dynamic enrichment.
- Expanding materialization/setup coverage beyond scenarios the current executor can already materialize.
- Generic database/application reset inside the executor core.
- CSV fault vectors as executor input or executor-generated CSV behavior files.
- Backward compatibility with the old verifier executor report schema.

## Actors and Permissions

| Actor | Can / Cannot | Notes |
|-------|--------------|-------|
| Verifier author | Can run default-vector and explicit-vector scenario execution attempts through the CLI/Docker runner. | Explicit vectors require an explicit scenario id. |
| Future search/orchestration runner | Can call the executor repeatedly, one vector per run. | Search policy and batching stay outside this package. |
| ScenarioExecutor | Can install one execution-scoped in-memory fault provider, drive one scenario/vector, close lifecycle, and write one report. | It must clear provider state after execution. |
| Simulator runtime | Can inject vector faults through a simulator-owned provider API. | It must not depend on verifier classes. |
| Legacy simulator/Quizzes impairment tests | Can continue using CSV/manual impairment behavior. | CSV behavior is suppressed/overridden only during executor-owned vector runs. |

## User Stories

1. As the verifier author, I want to execute a selected scenario plan with an assigned binary fault vector, so that generated scenarios become runnable fault experiments.
2. As the verifier author, I want fault execution to preserve saga commit/abort/compensation semantics, so that observed outcomes are meaningful for later impact analysis.
3. As the verifier author, I want reports to show exactly how every vector bit mapped to a fault slot, so that future search/scoring can reproduce and compare executions.
4. As the verifier author, I want expected injected faults to be valid outcomes, so that a compensated fault run is not confused with executor failure.
5. As a future search runner, I want one `{scenarioPlanId, faultVector}` evaluated per execution, so that search policy can remain outside the executor.
6. As a maintainer, I want existing CSV impairment tests to keep working, so that this feature does not break current simulator/Quizzes behavior.

## Behavior and Edge Cases

- Fault vectors are strictly binary: `0` means no injected fault for that fault slot; `1` means inject the expected simulator fault at that forward scheduled step.
- The external vector contract is a binary string aligned by index to `faultSpace.scheduledStepIds`. Internal representation is implementation-owned.
- `ScenarioPlan.deterministicId` remains structural and vector-independent. Each scenario execution attempt receives a unique opaque `scenarioExecutionId`.
- If no explicit vector is provided, the executor uses `faultSpace.defaultVector` and reports `vectorSource = DEFAULT_VECTOR`.
- If an explicit vector is provided, the executor reports `vectorSource = EXPLICIT_VECTOR` and requires an explicit scenario id.
- Both explicit vectors and `faultSpace.defaultVector` are validated before provider installation/materialization/execution.
- Invalid vectors or fault-space mappings fail before provider installation/materialization/execution with `INVALID_FAULT_VECTOR` when possible.
- Invalid cases include wrong vector length, non-`0|1` characters, explicit vector without explicit scenario id, `faultSpace.length != faultSpace.scheduledStepIds.size()`, duplicate `faultSpace.scheduledStepIds`, `scheduledStepIds` that do not each resolve exactly one forward `expandedSchedule` entry, and non-unique fault-slot-to-step mapping.
- An explicit empty vector or empty default vector is valid only when `faultSpace.length == 0`.
- Dry-run validates scenario selection and vector mapping, expands the fault slots, writes a `DRY_RUN` report, and does not install a provider or execute/compensate sagas.
- A fault-vector run installs the in-memory provider as authoritative for that execution. Legacy CSV state must not combine with vector faults or delays during executor-owned vector runs.
- Provider state is execution-scoped with explicit cleanup. The first implementation may reject concurrent active providers in the same JVM rather than silently sharing global state.
- Faults target forward saga steps only. Compensation steps are not fault slots and are not targeted by vector bits.
- The injected fault occurs at forward-step start, before the target step body executes. The faulted step has no normal step-body side effects.
- Forward scheduled steps are driven one boundary at a time. In this repository, the current primitives are `executeUntilStep(...)` for forward boundaries, `resumeWorkflow(...)` for success closure, and `resumeCompensation(...)` for fault closure.
- On all-zero/no-fault success, the executor explicitly closes the saga lifecycle so the unit of work commits.
- On a realized expected fault, forward execution stops for that saga and the executor drives compensation to completion.
- A compensated expected fault is reported as the valid terminal outcome `FAULT_COMPENSATED`, not `STEP_EXECUTION_FAILED`.
- If multiple `1` bits target the same saga and an earlier one aborts it, later unreached `1` bits are valid masked fault slots, not executor failures.
- If a reached `1` bit does not raise the expected injected-fault signal, the run is invalid and reports `EXPECTED_FAULT_NOT_INJECTED`.
- If an injected-fault signal appears at a `0` bit, the run reports `UNEXPECTED_INJECTED_FAULT`.
- If an injected-fault signal appears at the wrong slot, the run reports `FAULT_PROVIDER_MISMATCH`.
- Unexpected forward exceptions at `0` bits are reported as `UNEXPECTED_EXECUTION_FAILURE`, with best-effort lifecycle closure when the simulator exposes compensation primitives.
- If compensation fails after a realized fault or unexpected forward failure, the report includes compensation failure details. `COMPENSATION_FAILED` exits non-zero because lifecycle closure failed.
- Once a saga instance realizes a fault or terminal forward failure, the executor does not continue later forward scheduled steps for that same saga.
- For this package's single-saga scope, any terminal forward fault/failure stops forward execution.
- Compensation is reported as lifecycle outcome, separate from forward `stepOutcomes`; compensation steps are not added to the forward fault space.
- Report ordering is deterministic: fault slots by slot index and forward step outcomes by `scheduleOrder`.
- The current dynamic-enrichment pipeline remains pre-execution/catalog enrichment only. Fault-vector execution emits executor reports only; future impact-analysis telemetry is separate future work.
- CLI/Docker exit codes distinguish valid experiments from invalid/broken executions. `SUCCESS`, `FAULT_COMPENSATED`, and `DRY_RUN` exit zero; invalid vectors, unsupported scenarios, materialization failures, startup failures, provider mismatches, unexpected failures, and compensation failures exit non-zero.

## Data and Lifecycle Rules

- Source of truth for fault slots is the selected scenario plan's `faultSpace.scheduledStepIds` and `expandedSchedule`.
- Source of truth for an assigned vector is the CLI/Docker input or the plan's default vector when absent.
- A fault-space mapping is valid only when `faultSpace.length == faultSpace.scheduledStepIds.size()`, scheduled step ids are unique, and each scheduled step id resolves exactly one forward scheduled step in `expandedSchedule`.
- The executor owns generation of `scenarioExecutionId`; callers do not supply it.
- A scenario execution attempt produces a standalone v2 report artifact. It does not mutate `scenario-catalog.jsonl`, `scenario-catalog-enriched.jsonl`, dynamic evidence, or join-report sidecars.
- The v2 report must include enough data to reproduce and inspect the execution: schema version, `scenarioExecutionId`, scenario plan id, vector, vector source, provider mode, catalog path/kind, application/runtime metadata, slot mapping, realized/masked slots, forward step outcomes, lifecycle outcome, terminal status, blockers/errors, and skipped counts where relevant.
- The v2 report uses exact machine-readable status values:

| Field | Values | Notes |
|-------|--------|-------|
| `vectorSource` | `DEFAULT_VECTOR`, `EXPLICIT_VECTOR` | Default means the selected plan's `faultSpace.defaultVector`; explicit means CLI/Docker input. |
| `providerMode` | `IN_MEMORY_FAULT_VECTOR`, `NONE` | `NONE` is used for dry-run and pre-execution validation failures where no provider is installed. |
| `terminalStatus` | `SUCCESS`, `FAULT_COMPENSATED`, `DRY_RUN`, `INVALID_FAULT_VECTOR`, `SELECTION_FAILED`, `UNSUPPORTED_SCENARIO`, `MATERIALIZATION_FAILED`, `STARTUP_FAILED`, `EXPECTED_FAULT_NOT_INJECTED`, `UNEXPECTED_INJECTED_FAULT`, `FAULT_PROVIDER_MISMATCH`, `UNEXPECTED_EXECUTION_FAILURE`, `COMPENSATION_FAILED` | Valid experiments are `SUCCESS`, `FAULT_COMPENSATED`, and `DRY_RUN`; all other statuses are invalid or broken executions. |
| `lifecycleOutcome` | `NOT_STARTED`, `COMMITTED`, `COMPENSATED`, `COMPENSATION_FAILED`, `CLOSURE_SKIPPED` | `CLOSURE_SKIPPED` is used only when best-effort lifecycle closure is not exposed by the simulator path. |
| fault-slot realization state | `NOT_ASSIGNED`, `REALIZED`, `MASKED`, `UNREALIZED` | `UNREALIZED` means an assigned `1` slot was reached but the expected injected-fault signal was not observed. |

- Runtime environment metadata should include stable runner inputs such as application base/id, Spring profiles, catalog path/kind, scenario id, vector source, and executor mode. It should not attempt to snapshot full database/application state.
- For supported Quizzes Docker mode, each execution is expected to run one vector in a fresh one-shot scenario-executor JVM/container using `test,sagas,local`. The current Quizzes test profile uses in-memory H2 with `create-drop`, so the app DB is fresh per process.
- For persistent DB profiles or non-default execution environments, state reset is the caller/orchestrator's responsibility.
- Provider state must be cleared in a `finally`-style lifecycle so one execution's vector cannot leak into the next.
- Existing CSV/manual impairment files remain simulator-owned compatibility data and are not part of the ScenarioExecutor fault-vector report contract.

## Existing System Fit

- `ScenarioPlan` already carries `FaultSpace`, `expandedSchedule`, saga instances, and deterministic ids; this feature uses that structure rather than creating separate catalog records per vector.
- The current executor supports materializable single-saga candidates and runtime-owned arguments. This feature layers fault execution on top of that support instead of broadening materialization.
- The simulator already owns workflow execution, impairment/fault behavior, and saga compensation primitives. The new provider should live in `simulator`, use plain values, and avoid verifier dependencies.
- The current CSV impairment path remains valid for existing tests/manual use but is not the canonical executor path.
- The current scenario-executor Docker runner is the supported Quizzes smoke path for this feature, with `test,sagas,local` isolation.
- Documentation should rename `docs/verifiers-impl/reference/scenario-executor-poc.md` to `scenario-executor.md`, update references so the executor is no longer framed as a POC once this feature lands, and keep verifier/scenario-executor terminology centralized in `docs/verifiers-impl/glossary.md` as routed by `CONTEXT-MAP.md`.

## Research Notes

No external library research was required.

Repository alternatives considered:

- Selected: simulator-owned in-memory fault provider installed by the executor for one scenario execution attempt.
- Rejected: executor-generated CSV behavior files as the canonical contract, because CSV relies on weak simple-name/global-state/file semantics and is a poor fit for search and multi-saga evolution.
- Deferred: minimal in-memory registry hidden inside the legacy `ImpairmentHandler`, because it can be transitional but should not define the long-term contract.
- Rejected: generated Quizzes/test harnesses, because they are application/test-lifecycle coupled and not a generic scenario-executor contract.

## Acceptance Criteria

- [ ] AC-1: The executor accepts `--fault-vector` in the CLI and `FAULT_VECTOR` in the Docker runner, and explicit vectors require explicit `--scenario-id` / `SCENARIO_ID`.
- [ ] AC-2: When no vector is provided, the executor validates and uses `faultSpace.defaultVector`, reports `vectorSource = DEFAULT_VECTOR`, and still suppresses legacy CSV behavior for the executor-owned run.
- [ ] AC-3: Vector validation rejects wrong-length strings, non-binary characters, explicit vectors without explicit scenario id, invalid default vectors, `faultSpace.length` / `scheduledStepIds` size mismatches, duplicate `scheduledStepIds`, unresolved scheduled step ids, and non-unique slot mappings before saga execution.
- [ ] AC-4: Dry-run validates and expands the selected vector into all fault slots without installing the fault provider or executing/compensating sagas.
- [ ] AC-5: The simulator exposes an execution-scoped in-memory fault-provider API owned by `simulator`, using plain string/value identifiers and no verifier model classes.
- [ ] AC-6: Vector-injected faults are machine-distinguishable from ordinary runtime failures and carry enough identity to resolve the scenario execution attempt, plan, slot, scheduled step, and runtime step.
- [ ] AC-7: Provider state is installed for one scenario execution attempt, cleared after execution, and cannot silently leak across runs; concurrent active providers in one JVM are rejected or explicitly unsupported.
- [ ] AC-8: Existing CSV/manual impairment behavior remains usable for existing simulator/Quizzes tests outside executor-owned vector runs.
- [ ] AC-9: During executor-owned vector runs, in-memory provider decisions override/suppress CSV impairment so no legacy CSV fault/delay can alter the assigned vector semantics.
- [ ] AC-10: For successful no-fault/default-vector execution, the executor closes the lifecycle and reports `SUCCESS` with a committed lifecycle outcome.
- [ ] AC-11: For a reached `1` bit, the simulator injects the fault before the forward step body executes, the executor stops forward execution, drives compensation to completion, and reports `FAULT_COMPENSATED` when compensation succeeds.
- [ ] AC-12: Assigned `1` bits not reached because of an earlier realized fault are reported as masked fault slots with a clear mask reason.
- [ ] AC-13: A reached `1` bit that does not inject the expected fault reports `EXPECTED_FAULT_NOT_INJECTED`.
- [ ] AC-14: An injected-fault signal at a `0` bit reports `UNEXPECTED_INJECTED_FAULT`; an injected-fault signal at the wrong slot reports `FAULT_PROVIDER_MISMATCH`; neither is reported as an ordinary domain/runtime failure.
- [ ] AC-15: Unexpected forward exceptions at `0` bits report `UNEXPECTED_EXECUTION_FAILURE` and attempt best-effort lifecycle closure when possible.
- [ ] AC-16: Compensation failure reports `COMPENSATION_FAILED`, includes compensation exception details, and exits non-zero.
- [ ] AC-17: The v2 execution report includes full deterministic fault-slot mapping for every vector bit, ordered by slot index, and forward step outcomes ordered by schedule order.
- [ ] AC-18: The v2 execution report includes runtime metadata sufficient to reproduce the run without snapshotting full application/database state.
- [ ] AC-19: Fault-vector execution writes only the executor report and does not mutate scenario catalogs, enriched catalogs, dynamic evidence, or dynamic join reports.
- [ ] AC-20: CLI exit codes are zero for `SUCCESS`, `FAULT_COMPENSATED`, and `DRY_RUN`; invalid or broken outcomes exit non-zero.
- [ ] AC-21: Dummyapp-focused tests cover vector validation, invalid default vectors, malformed fault-space mappings, dry-run mapping, provider cleanup, expected fault compensation, masked slots, missing expected faults, wrong-slot provider mismatch, unexpected failure, compensation failure, CSV override, and unsupported scenario shape.
- [ ] AC-22: Quizzes Docker smoke covers default/all-zero vector success and one explicit single-bit vector that realizes a fault and reaches `FAULT_COMPENSATED` under `test,sagas,local`.
- [ ] AC-23: Verifier docs stop calling the ScenarioExecutor a POC after this feature lands, rename the reference doc to `scenario-executor.md`, update references/navigation/current-state wording, keep unsupported/future areas explicit, and keep new executor/fault-vector terminology in `docs/verifiers-impl/glossary.md` as routed by `CONTEXT-MAP.md`.

## Assumptions

- The step-level compensation support from `origin/master` is part of the baseline for this feature.
- One active fault-vector execution per JVM is acceptable for this package; parallel/search execution can evolve the provider scope later.
- Current materialization support is sufficient for the first supported scenarios; expanding materialization coverage is separate continuous improvement.
- The default Quizzes Docker scenario-executor path continues using `test,sagas,local` and in-memory H2 state per one-shot JVM/container.
- Multi-saga execution will require future simulator/executor changes for exact catalog identity and scheduling, but this package must not architecturally block that evolution.

## Open Questions

None blocking.

Deferred/future decisions:

- Exact multi-saga execution model and exact simulator runtime identity plumbing for multi-instance schedules.
- Impact-analysis telemetry shape for scenario execution attempts.
- Search/orchestration policy for choosing vectors and resetting persistent environments.

## Handoff

Ready for implementation planning: yes

Continue with `sp-implementation-plan`.
