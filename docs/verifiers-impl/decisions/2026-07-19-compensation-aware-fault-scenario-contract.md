# Compensation-aware v3 fault-scenario contract

Date: 2026-07-19

Status: active and implemented

## Context

A single record that combines reusable workload structure, one fault vector, and one recovery ordering duplicates the same participants, inputs, forward schedule, and conflict evidence across many experiments. It also makes recovery timing an implicit runtime choice rather than part of reproducible scenario identity.

Multi-Saga replay needs a clear policy for persisted assigned faults, unexpected domain failures, infrastructure failures, and compensation failures. Treating every `SimulatorException` as a domain result allowed service/configuration failures to trigger recovery and survivor continuation incorrectly.

## Decision

Use a two-level v3 package:

- `WorkloadPlan` owns reusable participants, accepted inputs, one normal forward interleaving, conflict evidence, forward fault slots, and compensation checkpoints.
- `FaultScenario` references one WorkloadPlan and owns one assigned vector plus one complete ordered `FORWARD`/`COMPENSATION` action schedule.

FaultScenario identity includes the workload id, vector, and ordered action identities. Eager generation is limited to all-zero and single-point vectors for static setup candidates. Arbitrary valid multi-fault vectors are persisted through the bounded guarded on-demand path before execution.

Assigned faults follow their persisted recovery schedule. Compensation checkpoints use explicit compensation, implicit Saga rollback, or conservative-unknown evidence; reverse checkpoints may interleave with still-live participants' forward actions. A participant commits automatically after its final successful forward action rather than through a separately schedulable commit action.

For the supported Saga/local executor:

- only an application/Saga failure explicitly implementing `DomainFailure` is a meaningful domain failure;
- a zero-bit marked body or commit failure may use immediate checkpoint recovery and survivor continuation, reported as `DEVIATED` when completed;
- extending `SimulatorException` alone is insufficient;
- plain/unknown `SimulatorException`, service unavailability, command retry exhaustion, ordinary runtime failures, leaked assigned-fault exceptions, and missing infrastructure hard-stop without fallback or survivor continuation and report `INCOMPLETE` after measured execution begins;
- a thrown compensation action hard-stops;
- no automatic compensation retry, retry count, or backoff is added.

## Why this contract

- Workload structure is not duplicated for every vector/recovery ordering.
- Every executable experiment has a deterministic persisted identity.
- Recovery timing is explicit rather than invented by the executor.
- The executor cannot silently reinterpret infrastructure failure as domain behavior.
- Single- and multi-participant attempts share one action-aware report model.

## Consequences

- V3 is a clean replacement for the historical v1/v2 `ScenarioPlan` catalog and participant-only execution-report designs; those remain available through Git history, not the active documentation tree.
- Runtime vector overlays and automatic FaultScenario selection are unsupported.
- On-demand local writers serialize package revisions through a package-local OS lock, but three-file promotion is not crash-atomic and does not establish network-filesystem or multi-host coordination.
- Current replay is deterministic sequential Saga/local execution, not TCC, stream, gRPC, causal, distributed, or true-parallel parity.

## Revisit when

- compensation itself needs fault slots or retry scheduling;
- a broader runtime can preserve the same action identity under distributed/parallel execution;
- package publication requires crash-atomic multi-file revisions;
- an external consumer requires a versioned migration beyond v3.

Current package, execution, and limitation evidence is in [`../current-state.md`](../current-state.md).
