# Event-consequence ownership and replay contract

Date: 2026-07-30

Status: active and implemented

## Context

A persisted Saga step can emit an application event whose selected local subscriber starts a downstream Saga. Treating that delivery as background infrastructure loses a causally important action: moving the delivery before or after another outer Saga step can change the observed domain result. Treating it as another Saga participant or faultable step is also wrong because the event has no independent producer body, compensation contract, or fault boundary.

Runtime replay has a second risk. The normal scheduler can race the executor for the same persisted event, prerequisite setup can leave unrelated pending events, and application-specific dispatch shortcuts would make the verifier package non-generic.

## Decision

Represent each supported delivery as an atomic `EVENT_CONSEQUENCE` normal action in the current workload/FaultScenario contract. A workload may select a bounded ordered subset of different routes of the same trigger/emission; the default bound of one preserves earlier generation.

Static ownership is conservative:

- trace one Saga forward step through command dispatch and service code to a direct `registerEvent(new EventType(...), unitOfWork)` call whose receiver resolves to a supported `UnitOfWorkService` variable and whose second argument is the service method's single relevant `UnitOfWork` parameter;
- reject the whole trigger step if a matching emission also originates in compensation;
- join that exact event type to a selected existing `EventHandling -> EventProcessing -> functionality -> Saga` route only when the `EventHandling` method contains exactly one unconditional, unrepeated, direct `handleSubscribedEvent` delegation;
- persist the trigger occurrence, emission site, event type, handling bean/method, handler class, downstream Saga, delivery policy, and causal placement;
- include route and placement in deterministic workload/action identity;
- reject ambiguous dispatch, wrong receiver or unit-of-work binding, multiple/repeated/conditional emission, compensation-origin emission, malformed consumer delegation, missing or ambiguous consumer routes, implicit multi-object fan-out within a route, direct recursion, and unsupported nested event chains rather than selecting heuristically;
- retain separate candidates when genuinely distinct consumer routes are globally selected; explicit bounded combinations preserve each route as a separate action with one shared event origin, rather than invoking an unscoped broadcast.

The executable exporter resolves every event occurrence against the authoritative Saga
route catalogue already written by static export. Matching is scoped to the triggering
participant Saga, exact step and emission ordinal, and selected consumer semantics.
A subset containing only the second consumer still references that consumer's catalogue
route; workload-local list positions never assign route identity. Zero or multiple exact
matches reject export. This 2026-09-04 repair changes no record shape; packages produced
with the earlier workload-local projection must be regenerated.

`forwardSchedule` remains the sole source of `ForwardFaultSlot`, vector bits, and `CompensationCheckpoint`. An event consequence owns none of them and never receives generated compensation.

Runtime ownership belongs to the simulator:

- activate replay mode before Spring starts;
- suppress unscoped scheduled event polling while replay mode is active;
- capture the exact event only after persistence assigns its runtime id;
- clear prerequisite-created pending events before measurement;
- for each selected route action, reuse that one persisted event id and require exactly one currently eligible subscriber for the route; missing or multiple subscribers remain hard stops, even after an earlier delivery;
- invoke the persisted real Spring `EventHandling` bean method synchronously, outside `FaultVectorBoundaryContext`, before the next outer action;
- reject recursive event registration before nested persistence;
- clear replay, capture, selection, and fault contexts in `finally`.

The event action is causally masked when its specific trigger occurrence has a pre-body assigned fault, fails before capturing a matching event, or is not reached. If the trigger body or commit captures the selected event and then fails, execution hard-stops as `TRIGGER_FAILED_AFTER_EVENT_EMISSION`, does not dispatch the event, and leaves ImpactV1 not evaluated. Missing/multiple events or subscribers, route mismatch, handler failure, recursion, and replay-control failure also hard-stop measured execution. None of these hard stops produces an ImpactV1 zero.

Prerequisite setup uses an exact provider id/version and typed baseline bindings persisted in the workload. Provider work, binding resolution, pending-event cleanup, and baseline evidence stay outside measured actions, fault allocation, recovery, conformance, and ImpactV1.

Execution reports use their separate v5 report schema so prerequisite evidence and actual event/subscriber evidence remain separate from the package. Execution never mutates package files.

## Why this contract

- Event timing becomes reproducible scenario identity instead of a scheduler race.
- An emitted event cannot be silently treated as a masked consequence after its trigger fails.
- Fault and compensation ownership remain aligned with executable outer Saga steps.
- Masking follows the actual trigger occurrence rather than a participant-wide approximation.
- The runtime invokes the application-selected Spring route without Quizzes or dummyapp production special-cases.
- Prerequisites can construct realistic initial state without being mistaken for measured behavior.
- Fresh process/container plus fresh H2 state provides a simple repeatability boundary without claiming unsafe same-process reset.

## Consequences

- Current package records use exact, versionless kind-specific shapes; historical versioned package records are rejected.
- Execution reports are v5; ImpactV1 remains v1.
- Supported event replay is Saga/local and synchronous, with one unique matching subscriber per selected route. `max-event-consequences-per-workload` bounds combined routes (default 1); catalogue truncation remains explicit.
- Automatic delivery to multiple objects within a route, combining different event emissions, recursive/nested event chains, event faults, event compensation, retries, TCC, stream, gRPC, remote/distributed delivery, true parallel replay, and same-process reset remain unsupported.
- Application-specific prerequisite providers may live on the target application's test classpath, but production verifier generation and execution stay descriptor/provider-driven and application-independent.

## Revisit when

- multiple concrete receivers per route or nested event chains require broader identity/eligibility semantics;
- an event delivery itself needs a fault or retry model;
- distributed replay can preserve exact event/subscriber identity and exclusivity;
- repeatability needs a reset boundary other than fresh process/container and database;
- package consumers require a new current contract.

Current behavior and Quizzes positive/control/masking evidence are in [`../current-state.md`](../current-state.md#source-derived-event-receiver-qualification).

## Bounded route-combination qualification

The September 6 extension is governed by
`issues/2026-09-06-combined-event-deliveries/`. It retains atomic delivery and forward-only
fault ownership, while allowing explicit subsets, orders and placements of several
routes of one event. It does not turn all asynchronous processing into a mandatory
final drain. The meeting note and current handbook distinguish selected horizons from
full application event convergence.
