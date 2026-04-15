# Application Architecture

This document is the system-level companion to the per-concept docs. Read it before implementing a new application to understand how the layers fit together, what constraints govern each one, and which simulator base classes to extend. To start a new application with Claude's help, invoke `/new-application <AppName>`.

---

## Core Concepts

| Concept | One-liner | Deep dive |
|---------|-----------|-----------|
| **Aggregate** | Unit of consistency; each write creates a new row; `aggregateId` is the logical identity; `version` is global | [`concepts/aggregate.md`](concepts/aggregate.md) |
| **Unit of Work** | Coordinates reads and writes for one functionality execution; committed or aborted atomically. Saga: `SagaUnitOfWork`. TCC: `CausalUnitOfWork` | [`concepts/sagas.md`](concepts/sagas.md), [`concepts/tcc.md`](concepts/tcc.md) |
| **Functionality** | A `WorkflowFunctionality` subclass that orchestrates a cross-service operation as a DAG of `Step`s | [`concepts/sagas.md`](concepts/sagas.md) |
| **Sagas** | Concurrency protocol using semantic locks (`SagaState`); conflicting steps declare `forbiddenStates` | [`concepts/sagas.md`](concepts/sagas.md) |
| **TCC (Causal)** | Concurrency protocol using field-level merge; concurrent writes to different fields can both succeed | [`concepts/tcc.md`](concepts/tcc.md) |

---

## Key Simulator Classes

| Class | Path | Role |
|-------|------|------|
| `Aggregate` | `simulator/.../ms/domain/aggregate/Aggregate.java` | Base for all domain aggregates; defines `verifyInvariants()`, `getEventSubscriptions()`, version chain |
| `SagaAggregate` | `simulator/.../ms/sagas/aggregate/SagaAggregate.java` | Interface for semantic-lock protocol; adds `getSagaState()` / `setSagaState()` |
| `CausalAggregate` | `simulator/.../ms/causal/aggregate/CausalAggregate.java` | Interface for field-level merge; adds `getMutableFields()`, `getIntentions()`, `mergeFields()` |
| `WorkflowFunctionality` | `simulator/.../ms/coordination/workflow/WorkflowFunctionality.java` | Base for all cross-service workflows; provides `executeWorkflow()`, `executeUntilStep()`, `resumeWorkflow()` |

---

## Application Anatomy

Every application built on the simulator has five synchronous layers plus an asynchronous event pipeline:

| Layer | Key class(es) | Responsibility |
|-------|--------------|----------------|
| **Controller** | `*Controller` | HTTP entry points; maps requests to commands and returns DTOs |
| **Functionality** | `*FunctionalitySagas`, `*FunctionalityTCC` | Orchestrates cross-aggregate workflows as a DAG of steps |
| **Command Handler** | `*CommandHandler` | Routes commands from `CommandGateway` to service methods |
| **Service** | `*Service` | Holds business logic; reads and writes its own aggregate type via `aggregateLoadAndRegisterRead` |
| **Aggregate** | `Xxx`, `SagaXxx`, `CausalXxx` | Encapsulates domain state; enforces intra-invariants at UoW commit |

**Asynchronous event pipeline** (one per inter-invariant dependency):

```
Aggregate publishes event
    → EventHandling polls for new events
        → EventHandler invokes EventProcessing
            → Update Functionality caches publisher state in consumer aggregate
```

---

## Package Structure Convention

Canonical directory layout for one microservice. Each package maps to an architectural layer:

```
microservices/{serviceName}/
├── aggregate/                              ← LAYER: Aggregate
│   ├── {Xxx}.java                          (base abstract aggregate)
│   ├── {Xxx}Dto.java                       (immutable DTO for inter-service reads)
│   ├── {Xxx}Repository.java
│   ├── {Xxx}Factory.java
│   ├── sagas/
│   │   ├── Saga{Xxx}.java                  (implements SagaAggregate)
│   │   ├── {Xxx}SagaState.java             (semantic-lock state enum)
│   │   └── factories/, repositories/
│   └── causal/
│       ├── Causal{Xxx}.java                (implements CausalAggregate)
│       └── factories/, repositories/
├── service/                                ← LAYER: Service
│   └── {Xxx}Service.java
├── commandHandler/                         ← LAYER: Command Handler
│   └── {Xxx}CommandHandler.java
├── coordination/                           ← LAYER: Functionality + Controller
│   ├── webapi/
│   │   └── {Xxx}Controller.java
│   ├── sagas/
│   │   └── {Operation}FunctionalitySagas.java
│   ├── causal/
│   │   └── {Operation}FunctionalityTCC.java
│   ├── functionalities/
│   │   └── {Xxx}Functionalities.java       (entry point / dispatch)
│   └── eventProcessing/
│       └── {Xxx}EventHandling.java
├── events/
│   ├── subscribe/
│   │   └── {Xxx}Subscribes{Event}.java
│   └── handling/handlers/
└── exception/
    └── {App}ErrorMessage.java
```

---

## Request Lifecycle

Happy-path flow from an HTTP request through UoW commit and into the async event tail. Invariant layer numbers refer to the taxonomy in [`concepts/consistency-enforcement.md`](concepts/consistency-enforcement.md).

```
HTTP Request
      │
      ▼
Controller
      │  creates Command
      ▼
Functionality (WorkflowFunctionality)
      │
      ├──► Step N: commandGateway.send(GetXxxCommand)
      │         └─ CommandHandler → Service.getXxx()
      │                 aggregateLoadAndRegisterRead(id, uow)   ← own type only
      │                 returns XxxDto to the functionality
      │
      └──► Step M: commandGateway.send(MutateXxxCommand)       ← depends on N
                └─ CommandHandler → Service.mutateXxx()
                        [Layer 2] service-layer guard
                            input validation + DB checks, inside @Transactional(SERIALIZABLE)
                            throw if precondition violated
                        aggregate.mutate()
                        unitOfWorkService.registerChanged(aggregate, uow)

      UoW commit
            [Layer 1] verifyInvariants() on each changed aggregate
            persist new version row
            publish domain events

      Async (~1 s poll interval)
            EventHandling detects new events
                [Layer 4] EventProcessing → Update Functionality
                    consumer aggregate caches publisher state
```

---

## Architectural Restrictions

These rules are not enforced by the compiler. Violating them produces subtle runtime failures or breaks the concurrency guarantees the simulator is designed to provide.

---

### R1 — A Service may only load its own aggregate type

`aggregateLoadAndRegisterRead(aggregateId, uow)` must be called **only for the aggregate type that the service owns**. Passing an ID that belongs to a different aggregate type breaks UoW read tracking and produces incorrect version-conflict detection.

**Instead:** Read another aggregate's state by issuing a `Get*Command` through a workflow step (`commandGateway.send(...)`), which routes to that aggregate's own service.

---

### R2 — A Service may only inject its own aggregate's components

A service class may `@Autowired` (or constructor-inject) only the repository, custom repository, and factory belonging to its own aggregate type, plus shared infrastructure (`UnitOfWorkService`, `AggregateIdGeneratorService`). Injecting a foreign service class mixes transaction boundaries and bypasses the UoW; injecting a foreign repository gives the service direct read/write access to a foreign data store — both escape the coordinated commit.

**Correct injections in `XxxService`:**
- `XxxRepository` / `XxxCustomRepository`
- `XxxFactory`
- `UnitOfWorkService`
- `AggregateIdGeneratorService`

**Instead:** Coordinate cross-aggregate operations at the Functionality layer. Each service is only called through its CommandHandler via `commandGateway`.

---

### R3 — Cross-aggregate state must flow through DTOs, not aggregate instances

A service may accept and return `{Xxx}Dto` objects belonging to any aggregate. It must never hold a reference to another aggregate's concrete class (e.g., `SagaExecution`, `CausalQuiz`). Aggregate instances carry UoW registration state that must not leak across service boundaries.

**Instead:** Expose all observable state through immutable DTO classes. The Functionality receives the DTO from a `Get*Command` step and passes the needed fields to downstream steps as plain values.

---

### R4 — Saga steps that mutate an aggregate must declare `forbiddenStates`

In the Sagas protocol, any step that mutates an aggregate must list the `SagaState` values of concurrent operations that would conflict. Omitting `forbiddenStates` allows two operations to interleave in ways that violate business rules.

See [`concepts/sagas.md`](concepts/sagas.md) for how semantic locks are acquired and checked.

---

### R5 — `getEventSubscriptions()` belongs in the downstream (consumer) aggregate only

Subscriptions encode a one-way dependency: the consumer caches state from the publisher. The upstream (publisher) aggregate must not subscribe to its own events and must not reference downstream aggregate types. Adding subscriptions in the wrong direction creates circular dependencies in the event pipeline.

See [`concepts/consistency-enforcement.md`](concepts/consistency-enforcement.md) Layer 4 for the upstream/downstream model.

---

### R6 — Every aggregate must implement both `SagaXxx` and `CausalXxx` variants

The simulator test suite runs under two Maven profiles (`test-sagas`, `test-tcc`). An aggregate that implements only one protocol will fail the other profile's tests. Each variant requires its own factory and repository.

---

### R7 — `verifyInvariants()` must not perform DB reads

`verifyInvariants()` is called inside the UoW commit path, after all mutations have been applied. Repository calls at this point risk deadlocks and violate the layering contract. Intra-invariants must check only fields already present on the aggregate instance.

**Instead:** Use a Layer 2 service-layer guard in `*Service.java`, which runs before the UoW commit and can safely read from the DB.

---

### R8 — Aggregate DTOs must be immutable

DTOs are point-in-time snapshots of an aggregate's observable state. A Functionality step must not mutate a DTO it received from a `Get*Command`. Mutations must be expressed as new commands dispatched to the owning service.

---

## Choosing the Right Invariant Layer

For a quick decision, use this table. For full rationale and examples for each layer, see [`concepts/consistency-enforcement.md`](concepts/consistency-enforcement.md).

| Rule type | Right layer |
|-----------|-------------|
| Always true within one aggregate; derivable from its own fields | Layer 1 — `verifyInvariants()` |
| Requires a DB read OR pure input validation before mutation | Layer 2 — service-layer guard |
| Requires reading a **different** aggregate under a semantic lock | Layer 3 — cross-aggregate state guard (saga step) |
| Cross-aggregate; eventual consistency is acceptable | Layer 4 — inter-invariant via domain events |

---

## Related Documentation

| Topic | Path |
|-------|------|
| Aggregate versioning | [`concepts/aggregate.md`](concepts/aggregate.md) |
| Sagas semantic locks | [`concepts/sagas.md`](concepts/sagas.md) |
| TCC field-level merge | [`concepts/tcc.md`](concepts/tcc.md) |
| Domain events | [`concepts/events.md`](concepts/events.md) |
| Invariant taxonomy (full) | [`concepts/consistency-enforcement.md`](concepts/consistency-enforcement.md) |
| Bootstrap a new application | `/new-application` skill |
| Worked example | [`examples/cannot-delete-last-execution-with-content.md`](examples/cannot-delete-last-execution-with-content.md) |
