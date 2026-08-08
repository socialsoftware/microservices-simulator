# Application Architecture

This document is the system-level companion to the per-concept docs. Read it before implementing a new application to understand how the layers fit together, what constraints govern each one, and which simulator base classes to extend.

---

## Core Concepts

| Concept | One-liner | Deep dive |
|---------|-----------|-----------|
| **Aggregate** | Unit of consistency; each write creates a new row; `aggregateId` is the logical identity; `version` is global | [`concepts/aggregate.md`](concepts/aggregate.md) |
| **Unit of Work** | Coordinates reads and writes for one functionality execution; committed or aborted atomically. Saga: `SagaUnitOfWork` | [`concepts/sagas.md`](concepts/sagas.md) |
| **Functionality** | A `WorkflowFunctionality` subclass that orchestrates a cross-service operation as a DAG of `Step`s | [`concepts/sagas.md`](concepts/sagas.md) |
| **Sagas** | Concurrency protocol using semantic locks (`SagaState`); conflicting steps declare `forbiddenStates` | [`concepts/sagas.md`](concepts/sagas.md) |

---

## Key Simulator Classes

| Class | Path | Role |
|-------|------|------|
| `Aggregate` | `simulator/.../ms/aggregate/Aggregate.java` | Base for all domain aggregates; defines `verifyInvariants()`, `getEventSubscriptions()`, version chain |
| `SagaAggregate` | `simulator/.../ms/transaction/sagas/aggregate/SagaAggregate.java` | Interface for semantic-lock protocol; adds `getSagaState()` / `setSagaState()` |
| `WorkflowFunctionality` | `simulator/.../ms/coordination/WorkflowFunctionality.java` | Base for all cross-service workflows; provides `executeWorkflow()`, `executeUntilStep()`, `resumeWorkflow()` |

---

## Application Anatomy

Every application built on the simulator has five synchronous layers plus an asynchronous event pipeline:

| Layer | Key class(es) | Responsibility |
|-------|--------------|----------------|
| **Controller** | `*Controller` | HTTP entry points; maps requests to commands and returns DTOs |
| **Functionality** | `*FunctionalitySagas` | Orchestrates cross-aggregate workflows as a DAG of steps |
| **Command Handler** | `*CommandHandler` | Routes commands from `CommandGateway` to service methods |
| **Service** | `*Service` | Holds business logic; reads and writes its own aggregate type via `aggregateLoadAndRegisterRead` |
| **Aggregate** | `Xxx`, `SagaXxx` | Encapsulates domain state; enforces intra-invariants at UoW commit |

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

Two packages under `microservices/` are shared rather than per-service: `exception/`
(`{App}Exception`, `{App}ErrorMessage`) and `domain/` (`{App}DomainConstants`, present only when some
aggregate declares a sentinel). Both hold plain constants and exception types rather than beans, so
they cross service boundaries without coupling one microservice's package to another's.

```
microservices/{serviceName}/
├── {Xxx}ServiceApplication.java
├── aggregate/                                      ← LAYER: Aggregate
│   ├── {Xxx}.java                                  (base abstract aggregate)
│   ├── {Xxx}Dto.java                               (immutable DTO)
│   ├── {Xxx}Repository.java
│   ├── {Xxx}CustomRepository.java
│   ├── {Xxx}Factory.java
│   └── sagas/
│       ├── Saga{Xxx}.java                          (implements SagaAggregate)
│       ├── states/{Xxx}SagaState.java
│       ├── factories/
│       └── repositories/
├── service/                                        ← LAYER: Service
│   └── {Xxx}Service.java
├── messaging/                                      ← LAYER: Command Handler
│   └── {Xxx}CommandHandler.java
├── coordination/                                   ← LAYER: Functionality + Controller
│   ├── webapi/
│   │   └── {Xxx}Controller.java
│   ├── functionalities/
│   │   └── {Xxx}Functionalities.java               (entry point / dispatch)
│   ├── sagas/
│   │   └── {Operation}FunctionalitySagas.java
│   └── eventProcessing/                            ← optional: only if aggregate consumes events
│       └── {Xxx}EventProcessing.java
└── notification/                                   ← optional: only if aggregate consumes events
    ├── handling/
    │   ├── {Xxx}EventHandling.java                 (polling loop)
    │   └── handlers/
    │       └── {Event}EventHandler.java
    └── subscribe/
        └── {Xxx}Subscribes{Event}.java
```

**Optional directories:**
- `coordination/eventProcessing/` and `notification/` are present only in aggregates that **consume events** from other services (e.g., Shipment). Aggregates that only **publish events** (e.g., Warehouse) omit both directories.

### `{Xxx}ServiceApplication.java`

Each microservice has a `@SpringBootApplication` entry point gated by `@Profile("{xxx}-service")`. This lets the service run in isolation (activated by its named profile) while remaining inert inside the simulator's monolithic `{AppClass}Simulator` context (which does not activate any per-service profile). The class:
- Scans only its own `microservices/{xxx}` package plus `pt.ulisboa.tecnico.socialsoftware.ms`
- Implements `InitializingBean` to call `eventService.clearEventsAtApplicationStartUp()` on startup
- Is profile-gated and does not conflict with the monolithic simulator entry point

### `{Xxx}Controller.java`

A minimal `@RestController` stub under `coordination/webapi/`. In the simulator, HTTP endpoints are not exercised by the test harness (tests drive operations directly via `{Xxx}Functionalities`). The controller is created as an empty stub to mark the architectural slot. Endpoints can be filled in when a web-API layer is needed.

---

## Request Lifecycle

Happy-path flow from an HTTP request through UoW commit and into the async event tail. Pattern codes refer to the taxonomy in [`concepts/rule-enforcement-patterns.md`](concepts/rule-enforcement-patterns.md).

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
                        [P3] service-layer guard
                            own-table reads, uniqueness checks, or DTO field validation;
                            runs inside @Transactional(SERIALIZABLE), throw if precondition violated
                        aggregate.mutate()
                        unitOfWorkService.registerChanged(aggregate, uow)

      UoW commit
            [P1] verifyInvariants() on each changed aggregate
            persist new version row
            publish domain events

      Async (~1 s poll interval)
            EventHandling detects new events
                [P2] EventProcessing → Update Functionality
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

A service may accept and return `{Xxx}Dto` objects belonging to any aggregate. It must never hold a reference to another aggregate's concrete class (e.g., `SagaWarehouse`, `CausalShipment`). Aggregate instances carry UoW registration state that must not leak across service boundaries.

**Instead:** Expose all observable state through immutable DTO classes. The Functionality receives the DTO from a `Get*Command` step and passes the needed fields to downstream steps as plain values.

---

### R4 — Saga steps that touch a pre-existing aggregate must declare their lock intent

In the Sagas protocol, a step that touches an aggregate **that already exists when the saga starts** must declare how it guards against concurrent operations. Which mechanism applies depends on the aggregate's relationship to the saga:

- **Primary aggregate** (the one owning this saga) — wrap the *read* command in `SagaCommand` and call `setSemanticLock(state)` on it. The mutate step that follows sends a plain, unwrapped command and declares the lock step as a dependency. Do not use `forbiddenStates` to acquire a primary-aggregate lock.
- **Foreign aggregate** (an upstream aggregate a cross-aggregate step touches) — send a plain command with `setForbiddenStates([...])`, listing the `SagaState` values of concurrent operations that would conflict. This checks that the foreign aggregate is not already mid-saga; it does not acquire a lock.

Declaring neither lets two operations interleave in ways that violate business rules.

The scope is deliberate. A step that **creates** an aggregate has no prior state to guard: nothing else can hold a lock on an aggregate whose id the service has not minted yet. Such a step declares neither a semantic lock nor `forbiddenStates`, and instead registers a compensation that removes what it created if and only if a later step follows it. See [`concepts/sagas.md`](concepts/sagas.md) § "Create Functionality Sagas".

[`concepts/sagas.md`](concepts/sagas.md) § "R4 Decision Table" is authoritative for which of the three cases applies, and § "Lock-Acquisition Step Pattern" shows how semantic locks are acquired and checked.

---

### R5 — `getEventSubscriptions()` belongs in the downstream (consumer) aggregate only

Subscriptions encode a one-way dependency: the consumer caches state from the publisher. The upstream (publisher) aggregate must not subscribe to its own events and must not reference downstream aggregate types. Adding subscriptions in the wrong direction creates circular dependencies in the event pipeline.

See [`concepts/rule-enforcement-patterns.md`](concepts/rule-enforcement-patterns.md) P2 for the upstream/downstream model.

---

### R6 — `verifyInvariants()` must not perform DB reads

`verifyInvariants()` is called inside the UoW commit path, after all mutations have been applied. Repository calls at this point risk deadlocks and violate the layering contract. Intra-invariants must check only fields already present on the aggregate instance.

**Instead:** Use a P3 service-layer guard in `*Service.java`, which runs before the UoW commit and can safely read from the DB.

---

### R7 — Aggregate DTOs must be immutable

DTOs are point-in-time snapshots of an aggregate's observable state. A Functionality step must not mutate a DTO it received from a `Get*Command`. Mutations must be expressed as new commands dispatched to the owning service.

```java
// WRONG — mutates a snapshot owned by another aggregate. The write is invisible to
// that aggregate's UoW, so it is never persisted and never compensated.
{Aggregate}Dto dto = ({Aggregate}Dto) commandGateway.sendAndCollect(new Get{Aggregate}ByIdCommand(id));
dto.set{Field}(newValue);

// RIGHT — read the snapshot, send the change as a command to the owning service.
{Aggregate}Dto dto = ({Aggregate}Dto) commandGateway.sendAndCollect(new Get{Aggregate}ByIdCommand(id));
commandGateway.sendAndCollect(new Update{Aggregate}{Field}Command(dto.getAggregateId(), newValue));
```

---

### R8 — Functionalities may only send commands to upstream aggregates

A functionality that belongs to aggregate A may only issue commands (read or mutate) to aggregates that are **upstream of A** in the event dependency graph. It must never send commands to aggregates that are downstream of A.

**Why:** Downstream aggregates depend on A's events to cache A's state — the data-flow direction is A → downstream. Sending a command from A's functionality to a downstream aggregate reverses that direction, couples A to downstream internals, and risks circular command chains.

---

## Choosing the Right Enforcement Pattern

For a quick decision, use this table. For the full decision flowchart and pattern recipes, see [`concepts/rule-enforcement-patterns.md`](concepts/rule-enforcement-patterns.md).

| Rule type | Pattern |
|-----------|---------|
| Always true within one aggregate; derivable from its own fields | P1 — `verifyInvariants()` |
| Synchronous service-level check (own-table uniqueness OR saga-assembled DTO field validation) | P3 — service guard |
| Cross-aggregate; eventual consistency is acceptable | P2 — inter-invariant via domain events |
| Precondition implicit in saga fetch / same value to two aggregates | P4a/P4b — by construction |

---

## Related Documentation

| Topic | Path |
|-------|------|
| Aggregate versioning | [`concepts/aggregate.md`](concepts/aggregate.md) |
| Service layer patterns | [`concepts/service.md`](concepts/service.md) |
| Commands & CommandHandler | [`concepts/commands.md`](concepts/commands.md) |
| Sagas semantic locks | [`concepts/sagas.md`](concepts/sagas.md) |
| Domain events | [`concepts/events.md`](concepts/events.md) |
| Rule-enforcement patterns (full) | [`concepts/rule-enforcement-patterns.md`](concepts/rule-enforcement-patterns.md) |
