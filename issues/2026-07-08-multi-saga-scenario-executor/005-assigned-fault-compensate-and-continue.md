# 005 - Assigned-Fault Compensate-and-Continue

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `001-v3-participant-report-and-single-saga-migration.md`, `002-explicit-multi-saga-selection-and-dry-run.md`, `003-multi-participant-materialization-and-startup-gates.md`, `004-default-vector-interleaving-and-survivor-closure.md`  
ACs covered: `AC-20, AC-21, AC-22, AC-23, AC-24, AC-27, AC-32, AC-33, AC-36, AC-37, AC-40`  
Risk: `high`

## Purpose

Implement the selected multi-saga assigned-fault policy: a reached assigned binary fault terminates only the owning participant, compensates that participant immediately when possible, masks that participant's later assigned slots, and continues surviving participants in catalog schedule order.

## Scope

- Build scenario-wide `InMemoryFaultVectorProvider` assignments from all assigned `1` bits in `ScenarioPlan.faultSpace`.
- Realize an assigned fault only when the provider signal matches the current slot index, scenario execution id, scenario plan id, saga instance id, scheduled step id, and runtime step name.
- On realized assigned fault:
  - record participant step outcome `INJECTED_FAULT`;
  - set the matching fault slot to `REALIZED`;
  - immediately call `resumeCompensation` for that participant when possible;
  - set participant lifecycle to `COMPENSATED` or `CLOSURE_SKIPPED` if compensation is unsupported but non-failing;
  - mark that participant terminal;
  - skip that participant's remaining forward scheduled steps;
  - mark later assigned slots in that participant as `MASKED_BY_SAGA_FAILURE`;
  - keep assigned slots in other surviving participants active.
- Allow multiple assigned faults in different surviving participants to realize in one attempt.
- Aggregate top-level status to `COMPENSATED` when every started/executed terminal participant compensates and no participant commits, or `PARTIAL_COMPENSATED` when at least one participant compensates and at least one survivor commits.

## Out of Scope

- Non-assigned scheduled-step runtime exceptions; S6 owns them.
- Compensation failure continuation; S6 owns it.
- Expected-fault-not-injected and provider mismatch hard stops; S6 owns them.
- Quizzes Docker smoke.

## Repo Anchors

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java` — current single-saga assigned-fault handling, masking, and compensation.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/InMemoryFaultVectorProvider.java` — provider identity matching by slot and scenario/saga/step/runtime fields.
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/FaultVectorInjectedFaultException.java` — injected-fault identity carried back to executor.
- `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java` — extend to make participant-specific fault/compensation behavior observable.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — add assigned-fault continuation, masking, and multiple-fault tests.

## Implementation Shape

- Reuse current provider boundary/context pattern; expand provider assignment creation to all participant slots instead of one saga.
- Track participant terminal state so the main schedule loop can skip later steps for failed participants while continuing others.
- Keep participant skipped-step reporting separate from fault-slot realization state: skipped forward steps belong to participant evidence; masked assigned slots belong to scenario-level fault-slot evidence.
- Compute aggregate terminal status only after the schedule loop and survivor closure complete.
- Treat `NoSuchMethodException` for compensation like current single-saga behavior (`CLOSURE_SKIPPED`) only if existing behavior already permits it; do not invent generic recovery.

## TDD / Test Shape

- First behavior to test: vector fault in participant A compensates A, skips A's later steps, then participant B continues and commits, producing `PARTIAL_COMPENSATED`.
- Expected red failure: current executor stops at first single-saga fault and returns v2 `FAULT_COMPENSATED`.
- Additional coverage:
  - two assigned faults in different participants both realize if the first failed participant's failure does not kill the second participant;
  - later assigned slots in the failed participant are `MASKED_BY_SAGA_FAILURE`;
  - later assigned slots in surviving participants remain active and can become `REALIZED`;
  - all-started participants compensated and none committed aggregates to `COMPENSATED`;
  - failed-and-compensated participant is not closed during final survivor closure;
  - provider is cleared after assigned-fault paths.

## Just-in-Time Preflight Required

Before source edits, the executor must re-check:

- Current `realizedAndMaskedSlots(...)`, `matchesCurrentSlot(...)`, `provider(...)`, and `boundaryContext(...)` methods in `ScenarioExecutor.java`.
- Current single-saga masking state names and tests; v3 must use `MASKED_BY_SAGA_FAILURE`, not v2 `MASKED`.
- The accepted failure-policy ADR for compensate-and-continue semantics.

## Verification

- `cd verifiers && mvn -Dtest=ScenarioExecutorSpec test` — expected to pass with multi-saga assigned-fault, continuation, multiple-fault, and masking tests.

## Evidence to Record

- files changed
- commands run and outputs
- report excerpts for `REALIZED`, `MASKED_BY_SAGA_FAILURE`, `COMPENSATED`, and `PARTIAL_COMPENSATED`
- fixture evidence showing survivor continuation and no double closure
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- Do not mask assigned slots in surviving participants after another participant fails.
- Do not stop the whole attempt on an assigned fault unless a validation/provider/compensation hard-stop from S6 applies.
- Be precise about matching provider identity; a wrong participant or scheduled-step id must not count as realization.
