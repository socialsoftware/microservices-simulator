# Event-consequence ownership and replay contract

Date: 2026-07-30

Status: active and implemented

## Context

A persisted Saga step can emit an application event whose selected local subscriber starts a downstream Saga. Treating that delivery as background infrastructure loses a causally important action: moving the delivery before or after another outer Saga step can change the observed domain result. Treating it as another Saga participant or faultable step is also wrong because the event has no independent producer body, compensation contract, or fault boundary.

Runtime replay has a second risk. The normal scheduler can race the executor for the same persisted event, prerequisite setup can leave unrelated pending events, and application-specific dispatch shortcuts would make the verifier package non-generic.

## Decision

Represent one supported delivery as an atomic `EVENT_CONSEQUENCE` normal action in the v4 WorkloadPlan/FaultScenario contract.

Static ownership is conservative:

- trace one Saga forward step through command dispatch and service code to a direct `registerEvent(new EventType(...), unitOfWork)` call whose receiver resolves to a supported `UnitOfWorkService` variable and whose second argument is the service method's single relevant `UnitOfWork` parameter;
- reject the whole trigger step if a matching emission also originates in compensation;
- join that exact event type to a selected existing `EventHandling -> EventProcessing -> functionality -> Saga` route only when the `EventHandling` method contains exactly one unconditional, unrepeated, direct `handleSubscribedEvent` delegation;
- persist the trigger occurrence, emission site, event type, handling bean/method, handler class, downstream Saga, delivery policy, and causal placement;
- include route and placement in deterministic workload/action identity;
- reject ambiguous dispatch, wrong receiver or unit-of-work binding, multiple/repeated/conditional emission, compensation-origin emission, malformed consumer delegation, missing or ambiguous consumer routes, fan-out, direct recursion, and unsupported nested event chains rather than selecting heuristically;
- retain separate candidates when genuinely distinct consumer routes are globally selected.

`forwardSchedule` remains the sole source of `ForwardFaultSlot`, vector bits, and `CompensationCheckpoint`. An event consequence owns none of them and never receives generated compensation.

Runtime ownership belongs to the simulator:

- activate replay mode before Spring starts;
- suppress unscoped scheduled event polling while replay mode is active;
- capture the exact event only after persistence assigns its runtime id;
- clear prerequisite-created pending events before measurement;
- select that one persisted event id and require exactly one eligible subscriber;
- invoke the persisted real Spring `EventHandling` bean method synchronously, outside `FaultVectorBoundaryContext`, before the next outer action;
- reject recursive event registration before nested persistence;
- clear replay, capture, selection, and fault contexts in `finally`.

The event action is causally masked when its specific trigger occurrence has a pre-body assigned fault, fails before capturing a matching event, or is not reached. If the trigger body or commit captures the selected event and then fails, execution hard-stops as `TRIGGER_FAILED_AFTER_EVENT_EMISSION`, does not dispatch the event, and leaves ImpactV1 not evaluated. Missing/multiple events or subscribers, route mismatch, handler failure, recursion, and replay-control failure also hard-stop measured execution. None of these hard stops produces an ImpactV1 zero.

Prerequisite setup uses an exact provider id/version and typed baseline bindings persisted in the workload. Provider work, binding resolution, pending-event cleanup, and baseline evidence stay outside measured actions, fault allocation, recovery, conformance, and ImpactV1.

Execution reports use v5 so prerequisite evidence and actual event/subscriber evidence remain separate from the v4 five-file package. Execution never mutates those five package files.

## Why this contract

- Event timing becomes reproducible scenario identity instead of a scheduler race.
- An emitted event cannot be silently treated as a masked consequence after its trigger fails.
- Fault and compensation ownership remain aligned with executable outer Saga steps.
- Masking follows the actual trigger occurrence rather than a participant-wide approximation.
- The runtime invokes the application-selected Spring route without Quizzes or dummyapp production special-cases.
- Prerequisites can construct realistic initial state without being mistaken for measured behavior.
- Fresh process/container plus fresh H2 state provides a simple repeatability boundary without claiming unsafe same-process reset.

## Consequences

- Package workload, fault, manifest, and accounting schemas are v4; input recipes are v2; v3 package records are rejected.
- Execution reports are v5; ImpactV1 remains v1.
- Supported event replay is Saga/local, synchronous, and limited to one unique matching subscriber.
- Fan-out, recursive/nested event chains, event faults, event compensation, retries, TCC, stream, gRPC, remote/distributed delivery, true parallel replay, and same-process reset remain unsupported.
- Application-specific prerequisite providers may live on the target application's test classpath, but production verifier generation and execution stay descriptor/provider-driven and application-independent.

## Revisit when

- event fan-out or nested event chains require explicit multi-action identity;
- an event delivery itself needs a fault or retry model;
- distributed replay can preserve exact event/subscriber identity and exclusivity;
- repeatability needs a reset boundary other than fresh process/container and database;
- package consumers require a contract beyond v4.

Current behavior and Quizzes positive/control/masking evidence are in [`../current-state.md`](../current-state.md#quizzes-event-consequence-replay-positive-control-and-masking).
