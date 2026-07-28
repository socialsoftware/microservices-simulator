# Establish the first generic invariant-impact baseline

- Workflow: SP coordinator v1
- State: `complete`
- Work shape: `System`
- Route: `Guided`
- Updated: `2026-07-28`
- Workspace: current checkout on `fault-analysis/scenarios`
- Git: uncommitted; no commit, push, PR, or merge authorized for this package
- Authority: implement and verify the approved intent; no external actions
- Implementation approval: `approved (user: "go ahead and proceed with @issues/2026-07-28-generic-invariant-impact-baseline/work.md")`

## Current Checkpoint

- Completed: Delivered and independently reviewed the generic invariant signal, ImpactV1 sidecar, dummyapp-labelled controls, and bounded Quizzes positive/zero evidence.
- Current: Complete.
- Problem: none.
- User input: none.
- Next: none.

## Intent Brief

### Outcome

The simulator exposes a structured, application-agnostic signal whenever an application's existing `Aggregate.verifyInvariants()` contract rejects a Saga write. The first impact model counts those signals for one ScenarioExecutor attempt: `ImpactV1 = invariantViolationCount`. A realized injected fault or successful compensation does not increase impact by itself.

### Representative Example

Two Quizzes cases distinguish the model:

1. The existing `AddParticipantAndUpdateStudentNameTest` feature `concurrent - add creator: add-s1; update; event; add-s2` resumes an AddParticipant Saga with stale creator data after an update/event interleaving. `Tournament.verifyInvariants()` rejects the write, the generic simulator hook emits one structured invariant-violation signal, and ImpactV1 is `1`.
2. The existing persisted `GetCourseExecutionsFunctionalitySagas` assigned-fault scenario realizes its fault and reaches `COMPENSATED / EXACT` with no invariant rejection. Its ImpactV1 report is evaluated with score `0`.

This establishes the intended distinction: a fault is not automatically harmful, while a real application-declared invariant rejection is impact.

### In Scope

- Instrument the Saga aggregate-write boundary around the existing `aggregate.verifyInvariants()` call so a thrown invariant failure emits exactly one structured signal before the original exception is rethrown unchanged.
- Correlate each signal, when available, with stable execution context: execution attempt, Saga/functionality, runtime step/action, aggregate type/id, and exception class/message.
- Add a small deterministic ImpactV1 model whose only scored feature is `invariantViolationCount` and whose score equals that count.
- Add an optional standalone impact report sidecar for normal ScenarioExecutor execution, linked to `executionAttemptId`, `workloadPlanId`, and `faultScenarioId`; omitting impact output preserves ordinary execution behavior.
- Treat setup, executor, or infrastructure failure as not evaluated rather than as score zero or positive impact.
- Add serious dummyapp-first regression fixtures for zero, one, and multiple invariant signals, exact counting, context isolation, exception preservation, deterministic finding order, and not-evaluated behavior.
- Record bounded real Quizzes evidence for the known stale-read invariant break and the known compensated generated fault.

### Non-goals

- Scoring compensation exceptions, Saga aborts, successful compensation, assigned faults, or schedule deviation.
- Detecting silent compensation/postcondition errors, final-state divergence, dirty reads, lost updates, or non-repeatable reads.
- Parsing arbitrary text logs or Jaeger traces, adding latency scoring, or defining weighted/severity models.
- Adding application-specific Quizzes repository queries or hardcoded invariant/error-message rules to production impact code.
- Making the known stale-read Quizzes test executable as a persisted FaultScenario in this package.
- TCC, stream, gRPC, distributed, or true-concurrent impact parity.
- Input/materialization improvements, local date-transform support, batch impact execution, GA search, or scenario prioritization.

### Constraints

- The generic detector must use the simulator's existing `Aggregate.verifyInvariants()` invocation boundary, not parse exception text or know application aggregate classes.
- The original exception type, template/message, transactional behavior, and executor classification must remain unchanged.
- Exactly one signal is recorded for one rejected `verifyInvariants()` invocation; report/lifecycle/log representations of the same exception must not be double-counted.
- Impact collection must be scoped to one evaluation attempt and must not leak signals across sequential tests or executor runs.
- A successfully handled assigned fault with no invariant rejection scores zero.
- Impact output is a sidecar and must not mutate the five v3 package artifacts or change `ScenarioExecutionReport.v4` semantics.
- Real Quizzes evidence may validate the generic detector through an existing Spock interaction even though that positive interaction is not yet a persisted executable FaultScenario; this limitation must remain explicit.

### Acceptance Proof

- Representative outcome: the named Quizzes stale-read feature emits exactly one generic invariant signal and evaluates to ImpactV1 `1`; the persisted assigned-fault Quizzes execution remains `COMPENSATED / EXACT`, realizes the assigned fault, emits no invariant signal, and writes ImpactV1 `0`.
- Focused regression: dummyapp proves zero/one/multiple exact counts, deterministic ordered findings, no cross-attempt leakage, preserved thrown exception semantics, and optional impact mode behavior.
- Material-risk evidence: ordinary ScenarioExecutor execution without impact output is unchanged; setup/infrastructure failures are not evaluated; no assigned-fault or successful-compensation event is misclassified as invariant impact; all five package hashes remain unchanged.
- Boundary/broad/real-environment checks: targeted simulator/verifier/Quizzes tests, complete affected Maven suites as justified by the cross-module hook, and Docker ScenarioExecutor evidence for the real persisted Quizzes fault.

## Context and Route

### Current Behavior

- `SagaUnitOfWorkService.registerChanged(...)` invokes `aggregate.verifyInvariants()` before assigning the commit version and merging the aggregate. A thrown exception currently propagates without a dedicated structured invariant-violation signal.
- ScenarioExecutor v4 already distinguishes assigned faults, domain failures, infrastructure failures, compensation outcomes, participant states, and schedule conformance, but it has no impact report.
- The latest Quizzes package has 82/82 setup-ready candidates and 164/164 generated FaultScenarios linked to setup-ready workloads.
- The saved assigned-fault `GetCourseExecutionsFunctionalitySagas` report is `COMPENSATED / EXACT` and contains no compensation work or known invariant rejection.
- Quizzes already contains a deterministic stale-read/event interleaving test that reaches `INVARIANT_BREAK` through `Tournament.verifyInvariants()`, but current generated inputs/action schedules cannot replay that interaction through ScenarioExecutor.

### Key Invariants

- ImpactV1 measures application-declared invariant rejections, not the presence of injected faults or recovery activity.
- A safe rollback is zero impact under this model.
- Infrastructure failure is not useful GA fitness and cannot become a positive or zero evaluated score.
- Production impact logic remains application-agnostic; Quizzes supplies evidence through its normal aggregate contract, not a Quizzes-specific analyzer.
- Raw findings remain available so later impact models can change without redefining what ImpactV1 measured.

### Route Rationale

Guided is the minimum safe route because this introduces the first durable impact/fitness meaning across simulator and verifier boundaries, requires two independently meaningful outcomes, and benefits from one fresh independent review. The model itself remains intentionally minimal and does not justify Governed ceremony.

### Material Risks

- Recording at both the simulator boundary and executor exception/report boundary could double-count one invariant rejection.
- Static/global recorder state could leak findings between tests or execution attempts.
- Broadly treating every `DomainFailure` as an invariant violation would misclassify ordinary business failures; only the exact `verifyInvariants()` boundary is authoritative.
- The positive Quizzes evidence is a real interaction test but not yet generated ScenarioExecutor proof; documentation must not collapse that distinction.

### Canonical Docs

- `docs/verifiers-impl/glossary.md` — define invariant-impact signal, ImpactV1, and evaluated versus not evaluated.
- `docs/verifiers-impl/current-state.md` — record implemented first-model scope and limitations.
- `docs/verifiers-impl/evidence.md` — record dummyapp and bounded Quizzes commands/results.
- `docs/verifiers-impl/roadmap.md` — mark the first impact-analysis baseline without claiming broader scoring or GA.
- `docs/verifiers-impl/reference/scenario-executor.md` — document optional impact sidecar operation if exposed through the executor CLI/wrapper.

## Delivery

| Outcome | Status | Proof |
|---|---|---|
| Generic invariant signal and ImpactV1 sidecar | complete | Simulator 106/106 and verifier 595/595; focused 30/30 and 111/111 controls cover a real rejecting-write score `1`, exact count/order, correlation admission, isolation, exception preservation, invalid-run null, CLI/wrapper, aliases, and unchanged package bytes. Independent convergence review: PASS. |
| Real Quizzes zero-versus-one case-study evidence | complete | Known stale-read test yields one signal (9/9 test class); persisted compensated fault yields evaluated score zero in Docker with unchanged package hashes. |

## Decisions and Scope Deltas

| ID | Decision | Status | Reason |
|---|---|---|---|
| D1 | Start with `ImpactV1 = invariantViolationCount`. | approved | The user selected the smallest meaningful baseline tied to the simulator's existing aggregate invariant contract. |
| D2 | Do not score a compensation merely because it throws. | approved | Compensation failures may be transient/retryable and are not automatically domain impact; this package does not define retry exhaustion or final-state divergence. |
| D3 | Use dummyapp for generic regression and real Quizzes examples for case-study evidence. | approved | This preserves application-independent implementation while demonstrating both a real invariant break and a safely handled real fault. |
| D4 | Keep the known positive Quizzes interaction test as evidence rather than broadening this package to make it a persisted executable scenario. | approved | Materializing its prerequisite-heavy multi-Saga/event schedule is a separate execution-coverage outcome. |
| D5 | Admit executor findings only when event execution-attempt and WorkloadPlan ids match the selected run. | approved implementation hardening | Independent review found that elapsed-time-only global capture could misattribute delayed/background signals. |

## Acceptance Evidence

- Focused simulator: 30 tests passed; exact zero/one/multiple signaling, identical exception propagation under recorder enablement/dispatch failures, context payload, scope restoration, and manifest count.
- Focused executor/CLI/wrapper: 111 tests passed; real rejecting aggregate boundary writes evaluated score `1`, synthetic ordered-count controls, correlation admission, sequential isolation, safe assigned-fault score `0`, invalid-run null scores, CLI propagation/validation, and normalized/symlinked output alias protection.
- Full modules: simulator 106/106 and verifier 595/595 passed.
- Real Quizzes positive: `AddParticipantAndUpdateStudentNameTest` 9/9 passed; the named stale-read feature records exactly one `SagaTournament` signal at `AddParticipantFunctionalitySagas.addParticipantStep`.
- Real Quizzes zero: Docker persisted FaultScenario `142eccc9...` completed `COMPENSATED / EXACT`, realized its assigned fault, and wrote evaluated ImpactV1 `0`; artifacts are under `verifiers/target/generic-invariant-impact-baseline/`.
- Package immutability: all five source-package SHA-256 values remained unchanged after Docker execution.
- First independent review returned NEEDS_FIX with six findings; INV-001, IMP-001, PROOF-001, OUT-001, CLI-001, and DOC-001 were addressed with focused regression before convergence review.
- Independent convergence review returned PASS with no remaining blocking, needs-fix, or scope-proposal findings and no missing required proof.

## Completion

Delivered the first application-independent impact baseline. Saga invariant rejections now emit structured evidence without changing domain exceptions; ScenarioExecutor can write an optional correlation-filtered ImpactV1 sidecar while preserving the v4 report and five-file package. Dummyapp-labelled execution proves boundary-driven score `1`; real Quizzes evidence proves a stale-read invariant signal and a realized safe fault with score `0`.

Known limits remain intentional: the positive Quizzes interaction is not yet a persisted executable FaultScenario; ImpactV1 does not detect silent postcondition/final-state divergence and does not score compensation exceptions; runtime support remains deterministic Saga/local only.

Canonical docs updated: `docs/verifiers-impl/glossary.md`, `current-state.md`, `evidence.md`, `roadmap.md`, and `reference/scenario-executor.md`.

Git state: changes remain uncommitted; no commit, push, PR, or merge was authorized.
