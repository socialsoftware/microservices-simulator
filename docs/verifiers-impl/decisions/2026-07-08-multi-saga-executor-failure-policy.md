# Multi-saga executor failure policy

Date: 2026-07-08

Status: accepted for the historical immediate-compensation executor. Assigned-fault recovery timing and compensation-failure continuation are superseded by the implemented v3 FaultScenario contract in [`2026-07-19-compensation-aware-fault-scenario-contract.md`](2026-07-19-compensation-aware-fault-scenario-contract.md). Under that current contract, only a zero-bit failure carrying the explicit `DomainFailure` marker retains immediate checkpoint recovery and survivor continuation; plain `SimulatorException` is not sufficient.

## Context

The current ScenarioExecutor supports a narrow materializable single-saga saga/local path with binary fault vectors. Extending it to multi-saga scenarios raises a core execution-policy question: after one saga instance realizes an assigned fault or fails, should the executor stop immediately, continue other saga instances, or delay compensation to expose broader impact?

This decision matters because future impact scoring needs an observation boundary that can reveal downstream domain effects, not just the first injected fault. A stop-first policy is simple and safe, but it can hide stale writes, invariant violations, unreleased saga state, and recovery behavior that only appear when neighboring saga instances continue.

## Decision

For the first multi-saga executor spec, scheduled-step runtime failures use a **compensate-and-continue** policy. This includes assigned binary fault-vector faults and non-assigned domain/simulator exceptions thrown while executing a scheduled saga step, such as aggregate-in-use conflicts.

When a saga instance realizes an assigned fault or throws a scheduled-step runtime failure:

1. the executor records the failed scheduled step and failed saga instance;
2. for assigned faults, the executor also records the realized fault slot;
3. the failed saga is compensated immediately when the saga runtime supports compensation;
4. remaining scheduled forward steps for that failed saga instance are skipped;
5. scheduled steps for other saga instances continue in catalog `expandedSchedule` order;
6. surviving saga instances are closed at the end of the scenario attempt;
7. the report records per-saga lifecycle outcomes, skipped steps, realized/masked fault slots, runtime failure details, and final terminal status.

Scenario-wide fault vectors may contain multiple assigned `1` bits across different saga participants. Under compensate-and-continue, later assigned slots in the failed participant are masked by that participant's failure, but assigned slots in surviving participants remain active and may still realize when their scheduled steps are reached.

Executor/infrastructure failures remain hard-stop failures. Selection errors, invalid fault vectors, unsupported scenario shapes, materialization failures, startup failures, provider mismatches, and configuration/report-writing failures are not treated as meaningful domain scenario outcomes.

A participant compensation failure does not hard-stop the whole scenario attempt. The failed participant remains terminal with lifecycle `COMPENSATION_FAILED`, its remaining forward steps are skipped, and surviving participants continue in catalog schedule order. The top-level terminal status is `COMPENSATION_FAILED`, and the report must make clear that final state may include uncompensated or partially compensated effects.

This policy is deterministic sequential interleaving, not true parallel execution. It does not claim distributed runtime parity or delayed-compensation semantics.

## Considered Options

### Stop at first fault

Stop-first would terminate the whole scenario when the first assigned fault or unexpected failure appears. It is the simplest executor-control policy and minimizes accidental state mutation after a failure.

It was rejected as the main multi-saga fault policy because it is too weak for future impact scoring. It can hide effects that only appear after another saga observes or reacts to the failed saga's compensation or partial effects.

Example risks hidden by stop-first:

- a neighboring saga may later commit a stale write after another saga removes or changes the same aggregate;
- a tournament removal may fail while update is active, but the relevant impact is whether update finishes consistently and whether a retry later deletes both tournament and quiz;
- compensation may clear or fail to clear saga state, but that is only visible if the scenario observes later steps and final state.

### Compensate failed saga immediately and continue surviving sagas

This is the selected policy. It gives the future scorer a meaningful final observation boundary while keeping the runtime semantics conservative: the failed saga is not left open indefinitely, and the executor does not invent retry behavior.

This policy is best for the first multi-saga slice because it preserves deterministic replay and exposes downstream effects caused by a handled saga failure.

### Defer compensation until the end, then continue surviving sagas

Deferred compensation would keep the failed saga's partial state or saga locks visible while other saga instances continue, and then attempt compensation near the end of the scenario attempt.

This is important to discuss with the advisor because it may expose a different class of failures: delayed compensation windows, long-lived aggregate locks, or anomalies caused by partial effects remaining visible. It is not selected for the first slice because it makes a stronger operational claim and may not match the current simulator's saga lifecycle assumptions. It should remain a future experimental policy if impact-scoring research needs to model delayed recovery.

### Configurable failure policy

A configurable policy such as `STOP_FIRST`, `COMPENSATE_AND_CONTINUE`, and `DEFER_COMPENSATION_AND_CONTINUE` would make the executor more flexible for later research.

It is deferred because adding a policy matrix now would enlarge the first multi-saga feature, multiply report/status cases, and make validation harder before the basic multi-saga replay path exists.

## Consequences

- Multi-saga execution reports must be per-saga, not only top-level single-saga summaries.
- Fault-slot masking must distinguish slots skipped because the failed saga terminated from slots skipped because the whole scenario ended.
- Top-level terminal status must distinguish clean success (`SUCCESS`), all-started-participants compensated (`COMPENSATED`), mixed committed/compensated outcomes (`PARTIAL_COMPENSATED`), and compensation failures (`COMPENSATION_FAILED`).
- The selected policy supports future impact scoring better than stop-first because it allows surviving saga instances to produce final observable state.
- The selected policy still does not model delayed compensation, retry, crash recovery, or true concurrency.
- The professor/advisor should review whether delayed compensation should become a later explicit experimental policy for impact-scoring evaluation.

## Implementation Notes

The immediate compensate-and-continue policy was implemented and validated in the bounded multi-saga executor. Scheduled-step runtime exceptions use the same immediate observation boundary as assigned faults, while executor/infrastructure failures remain hard stops.

For the selected compensation-aware v3 feature, assigned faults instead follow persisted recovery schedules and thrown compensation actions hard-stop. Zero-bit domain/simulator failures retain immediate compensate-and-continue as the deterministic fallback.
