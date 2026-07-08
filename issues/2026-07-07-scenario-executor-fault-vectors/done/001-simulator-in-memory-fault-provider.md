# 001 - Simulator In-Memory Fault Provider

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `None`  
ACs covered: `AC-5, AC-6, AC-7, AC-8, AC-9`  
Risk: `high`

## Purpose

Provide the simulator-owned fault-injection primitive required by executor-assigned binary vectors, without making simulator code depend on verifier model classes and without breaking existing CSV/manual impairment tests.

## Scope

- Add a simulator-owned in-memory fault-provider contract using plain values such as scenario execution id, scenario plan id, saga instance id, scheduled step id, slot index, functionality FQN/simple name, runtime step name, and assigned bit.
- Add a machine-distinguishable injected-fault signal for vector faults, carrying enough identity for executor reports and participating in existing workflow abort semantics before compensation.
- Add execution-scoped provider install/clear semantics with explicit cleanup and deterministic rejection/failure for concurrent active providers in one JVM.
- Add an explicit current-boundary context/scope using plain values such as scenario execution id, scenario plan id, saga instance id, scheduled step id, slot index, functionality FQN/simple name, runtime step name, and assigned bit; the executor will bind it before each forward boundary and clear it after the boundary.
- Integrate the provider at the forward-step start boundary in simulator execution so a `1` bit can fault before the step body executes.
- Preserve legacy CSV/manual impairment behavior when no vector provider is active.
- Suppress/override legacy CSV faults and delays while an executor-owned vector provider is active.

## Out of Scope

- Verifier-side vector parsing, catalog validation, report generation, CLI flags, and Docker input.
- Multi-saga scheduling semantics beyond carrying plain identity fields in the provider request/signal.
- Delay injection or non-binary impairment types.
- Fault injection into compensation steps.

## Repo Anchors

- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/ExecutionPlan.java` — existing CSV impairment query and forward-step execution boundary.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/impairment/ImpairmentHandler.java` — legacy CSV/manual compatibility boundary.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/exception/SimulatorException.java` — existing simulator exception style to inspect before adding a typed injected-fault signal.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/monitoring/dynamic/DynamicEvidenceRecorderHolder.java` — holder/scope style reference for simulator-owned runtime state.
- `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/ExecutionPlanDynamicEvidenceTest.java` — execution-plan unit test patterns and fixtures.
- `simulator/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/sagas/workflow/WorkflowExecutionPlanTest.groovy` — workflow lifecycle test style.
- `applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/behaviour/**` — CSV impairment compatibility tests/resources to keep working.

## Implementation Shape

- Introduce a small simulator package or class group near `coordination`/`impairment` for fault-vector provider values and scope management. Keep it independent from `verifiers/` packages.
- Model provider inputs/outputs as simulator-owned values, not verifier records. The provider can be backed by in-memory assignments installed by the executor later.
- Use an explicit scope API that installs one active provider and clears it via close/finally. The first implementation may reject concurrent active providers.
- Use an explicit current-boundary context/scope so `ExecutionPlan` can query the provider for the step currently being driven, rather than relying only on functionality or runtime step simple names. Clear that boundary context after each forward boundary.
- Make `ExecutionPlan` consult the active provider at forward-step start before the step body executes. When `executeUntilStep(...)` executes a prefix plus target step, provider injection must not preempt earlier prefix steps before their bodies run.
- When no provider is active, existing CSV behavior remains the path.
- While a vector provider is active, ensure CSV faults and delays do not affect the same execution. This may be done by returning empty behavior, bypassing CSV decisions, or otherwise making provider decisions authoritative.
- Throw a typed injected-fault signal only for provider-triggered faults. Ordinary step exceptions and legacy `SimulatorException`s must remain distinguishable. The typed signal must either extend `SimulatorException` or be translated/caught by workflow code so `Workflow.executeUntilStep(...)` marks the workflow aborted before `resumeCompensation(...)` is called.

## TDD / Test Shape

- First behavior to test: with an active provider assigning a fault to the current boundary runtime step, `ExecutionPlan` throws the typed injected-fault signal before that step body runs.
- Expected red failure: the current code only knows CSV `SimulatorException` impairment and cannot expose provider identity, current-boundary context, abort-compatible signal behavior, or scoped cleanup.
- Additional coverage:
  - active provider state and current-boundary context clear after scope close;
  - concurrent active provider install is rejected or reported deterministically;
  - no active provider preserves existing CSV behavior;
  - active provider suppresses CSV fault/delay behavior;
  - typed signal carries scenario execution id, plan id, slot index, scheduled step id, and runtime step name;
  - provider-injected fault through `executeUntilStep(...)` makes saga compensation legal through existing abort semantics;
  - second-slot fault timing lets the first prefix step body run and prevents only the target faulted step body.

## Just-in-Time Preflight Required

Before source edits, the executor must re-check:

- Current `ExecutionPlan` fault/delay branches and any recent edits to dynamic evidence step instrumentation.
- Existing `SimulatorException` constructors and package conventions.
- Current simulator test naming and Surefire includes in `simulator/pom.xml`.
- Whether any existing impairment tests need a narrower command than the full Quizzes suite.

## Verification

- `cd simulator && mvn -Dtest=<new-provider-test> test` — provider scope/signal behavior, current-boundary context cleanup, prefix timing, and abort-compatible compensation behavior pass.
- `cd simulator && mvn -Dtest=<new-provider-test>,ExecutionPlanDynamicEvidenceTest test` — provider integration does not regress existing execution-plan evidence behavior.
- Targeted CSV compatibility command or existing Quizzes impairment test command if CSV behavior is touched — legacy impairment still works without an active provider.

## Evidence to Record

- files changed
- commands run and outputs
- provider cleanup/concurrency evidence
- current-boundary context and abort/compensation compatibility evidence
- CSV compatibility/override evidence
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- Do not import verifier classes into `simulator/`.
- Do not let active provider state leak between tests or executor runs.
- Do not swallow ordinary runtime failures as injected vector faults.
- Ensure provider-injected faults mark the workflow aborted before the executor drives `resumeCompensation(...)`.
- Be careful around `ExecutionPlan.executeSteps(...)`: a provider fault must happen at the intended forward boundary, not before earlier already-targeted boundaries execute.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Added simulator-owned fault-vector provider primitives under `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults` using plain identifier/value records and no verifier model imports.
- Added scoped active-provider install/clear and current-boundary context APIs with deterministic concurrent-provider rejection.
- Added `FaultVectorInjectedFaultException`, extending `SimulatorException`, carrying scenario execution, scenario plan, saga instance, scheduled step, slot, functionality, runtime step, and assigned-bit identity.
- Integrated provider lookup into `ExecutionPlan` at forward-step start. Provider-triggered faults happen before the step body, while prefix steps in `executeUntilStep(...)` continue until the bound target runtime step.
- Preserved legacy CSV/manual impairment when no provider is active and suppressed CSV faults/delays/loaded behavior while a vector provider is active.

### Files Changed

- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/FaultVectorBoundaryContext.java` — current forward-boundary identity values.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/FaultVectorFault.java` — provider fault identity payload.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/FaultVectorFaultProvider.java` — simulator-owned provider contract.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/FaultVectorInjectedFaultException.java` — typed injected-fault signal extending `SimulatorException`.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/FaultVectorProviderHolder.java` — active provider and current-boundary scopes.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/InMemoryFaultVectorProvider.java` — plain in-memory assignment-backed provider.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/ExecutionPlan.java` — provider lookup at forward boundary and CSV suppression while provider is active.
- `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/FaultVectorProviderTest.java` — provider/signal/scope, timing, workflow-abort, and CSV compatibility/override coverage.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd simulator && mvn -Dtest=FaultVectorProviderTest test` | PASS | 7 tests run, 0 failures, 0 errors. Covers provider scope cleanup, concurrent install rejection, current-boundary cleanup, typed signal identity, before-body injection, prefix timing, workflow compensation legality, CSV compatibility, and CSV suppression. |
| `cd simulator && mvn -Dtest=FaultVectorProviderTest,ExecutionPlanDynamicEvidenceTest test` | PASS | 15 tests run, 0 failures, 0 errors. Confirms provider integration plus existing `ExecutionPlan` dynamic evidence behavior. |
| `rg "verifiers" simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/ExecutionPlan.java || true` | PASS | No output; simulator provider/integration code does not import/reference verifier packages. |

### Acceptance Criteria Evidence

- AC-5: Added simulator-owned provider API (`FaultVectorFaultProvider`, `FaultVectorBoundaryContext`, `FaultVectorFault`, holder/scope, in-memory provider) using plain values and no verifier imports.
- AC-6: Added `FaultVectorInjectedFaultException` extending `SimulatorException`; tests assert scenario execution id, plan id, saga instance id, scheduled step id, slot index, and runtime step name are carried distinctly from legacy CSV `SimulatorException`.
- AC-7: `FaultVectorProviderHolder.install(...)` returns an explicit closeable scope, `clear()` removes state, boundary scopes clear/restore thread-local context, and concurrent provider install is rejected deterministically; tests cover cleanup and rejection.
- AC-8: Legacy CSV/manual impairment remains active without a provider; `legacyCsvFaultStillAppliesWhenNoProviderIsActive` verifies a CSV fault still throws the existing `SimulatorException` path.
- AC-9: Active provider is authoritative for vector runs; `ExecutionPlan` loads empty behavior and returns zero total delay while a provider is active, and `activeProviderSuppressesCsvFaultsAndDelaysWhenItDoesNotInject` verifies CSV fault/delay behavior is suppressed.

### Browser / Manual Evidence

- Not required.

### TDD Notes

- Full TDD red/green was not practical in this pass because the simulator provider API and integration point had to be introduced together. Focused provider/integration coverage was added with the implementation; early verification exposed compile/assertion issues, which were fixed before the final passing runs recorded above.

### Deviations From Plan

- Used a new simulator package `pt.ulisboa.tecnico.socialsoftware.ms.faults` rather than placing the provider under `coordination` or `impairment`; this keeps the new vector contract independent while still integrating at `ExecutionPlan`.
- Targeted CSV compatibility is covered by `FaultVectorProviderTest` with temporary CSV behavior instead of running the broader Quizzes impairment suite.

### Blockers / Follow-Ups

- None.
