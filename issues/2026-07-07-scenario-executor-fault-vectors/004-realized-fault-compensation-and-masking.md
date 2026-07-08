# 004 - Realized Fault Compensation and Masking

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `001-simulator-in-memory-fault-provider.md`, `002-executor-vector-validation-and-report-v2.md`, `003-no-fault-lifecycle-closure.md`  
ACs covered: `AC-6, AC-7, AC-9, AC-11, AC-12, AC-17, AC-20, AC-21`  
Risk: `high`

## Purpose

Execute a valid vector with assigned `1` bits so the first reached assigned fault is injected by the simulator, forward execution stops, saga compensation is driven to completion, and later assigned slots are reported as masked instead of executor failures.

## Scope

- Install the validated provider assignment for a real executor-owned vector run.
- Bind/report explicit current-boundary slot identity around each forward boundary so a simulator injected-fault signal can be matched to the expected slot.
- On a reached assigned `1` bit, recognize the typed injected-fault signal as an expected experimental outcome, not a step execution failure.
- Stop forward execution for the single-saga run after the realized fault.
- Call the simulator compensation closure primitive (`resumeCompensation(...)`) after the realized fault, relying on the S1 injected-fault signal to have marked the workflow aborted through existing lifecycle semantics.
- Report `terminalStatus = FAULT_COMPENSATED` and `lifecycleOutcome = COMPENSATED` when compensation succeeds.
- Report realized and masked assigned slots deterministically, including clear mask reasons for later `1` bits not reached because the saga already aborted.
- Verify the fault is injected before the target step body executes.
- Clear provider state after fault/compensation paths.

## Out of Scope

- Missing expected faults where a reached `1` does not inject; S5 handles that.
- Unexpected injected faults at `0` bits or wrong slots; S5 handles those.
- Unexpected ordinary runtime failures and compensation failures; S5 handles those.
- Quizzes Docker explicit-vector smoke; S6 records it.

## Repo Anchors

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — forward boundary loop and injected-fault handling.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowFunctionality.java` — `resumeCompensation(...)` lifecycle API.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaWorkflow.java` — compensation behavior after workflow abort.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/workflow/SagaStep.java` — body/compensation registration behavior; useful for body-not-executed reasoning.
- `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java` — extend fixture to simulate compensation and body counters.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — realized fault and masking coverage.

## Implementation Shape

- Use the S2 slot mapping as the single source of assigned bits and expected runtime step identities.
- Before each `executeUntilStep(...)` call, bind the simulator provider current-boundary context in plain values: scenario execution id, scenario plan id, saga instance id, slot index, scheduled step id, runtime step name, and assigned bit. Clear that context after the boundary.
- When the simulator throws the typed injected-fault signal for the current expected `1` slot, mark that slot `REALIZED`, record a forward outcome that is not an executor step failure, and stop the forward loop.
- Drive `resumeCompensation(...)` once for the aborted saga and record the lifecycle outcome separately from forward step outcomes; if compensation is rejected because the workflow was not marked aborted, treat that as a slice failure to fix in S1/S4 rather than working around it.
- Mark later assigned `1` slots in the same single-saga schedule as `MASKED` with a reason such as earlier realized slot/saga abort. Leave `0` bits and already-completed slots with their appropriate non-assigned/completed state.
- Keep vector report ordering stable and status names exact.

## TDD / Test Shape

- First behavior to test: vector `010...` executes the first step body, realizes the second slot, does not run the second step body, stops later forward steps, calls `resumeCompensation(...)`, and reports `FAULT_COMPENSATED`.
- Expected red failure: current executor treats runtime exceptions as `STEP_EXECUTION_FAILED` and has no provider, no compensation path, no abort-compatible injected-fault handling, and no slot realization states.
- Additional coverage:
  - multiple `1` bits where the first reached fault masks later assigned bits;
  - provider cleanup after realized fault;
  - injected fault identity is copied into report fields;
  - compensation is legal after a provider-injected fault because the workflow is marked aborted;
  - `FAULT_COMPENSATED` maps to exit code zero;
  - CSV impairment remains suppressed while vector provider is active.

## Just-in-Time Preflight Required

Before source edits, the executor must re-check:

- The injected-fault exception type, accessor methods, and abort-compatible workflow behavior produced by S1.
- Current reflection helpers for invoking workflow methods.
- Whether fixture workflow methods should implement `resumeCompensation(...)` directly or through a shared test helper.
- Current report field names from S2.

## Verification

- `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` — realized fault, prefix timing, compensation legality, and masking tests pass.
- `cd simulator && mvn -Dtest=<new-provider-test> test` — if S4 needs simulator-side identity/fault-timing adjustments.

## Evidence to Record

- files changed
- commands run and outputs
- report excerpt with `FAULT_COMPENSATED`, realized slot, and masked slot
- prefix-step/body-not-executed and compensation-legal evidence
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- The injected fault must be treated as expected only when it matches the current assigned slot.
- Do not continue later forward scheduled steps for the single saga after a realized fault.
- Do not paper over `resumeCompensation(...)` rejection; it indicates the injected-fault signal did not integrate with workflow abort semantics.
- Do not report compensation steps as forward `stepOutcomes` or add them to the fault space.
- Compensation should be lifecycle outcome, not a forward scheduled step outcome.
