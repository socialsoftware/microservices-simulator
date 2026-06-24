# Aggregate

## What It Is

An `Aggregate` is the unit of consistency in the simulator. Every write creates a **new row** (copy-on-write); reads fetch the latest version by `aggregateId`. The `prev` pointer chains versions into a history log.

## Key Fields

| Field | Type | Purpose |
|-------|------|---------|
| `id` | `Integer` | JPA physical row PK (auto-generated) |
| `aggregateId` | `Integer` | Logical identity — stable across versions |
| `version` | `Integer` | Global monotonic version from `VersionService` |
| `state` | `AggregateState` | `ACTIVE`, `INACTIVE`, or `DELETED` |
| `prev` | `Aggregate` | Pointer to the previous version row |
| `aggregateType` | `String` | Simple class name, set in subclass constructor |

## Base Class

`simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/domain/aggregate/Aggregate.java`

Abstract methods every subclass must implement:
- `verifyInvariants()` — throw if any intra-invariant is violated
- `getEventSubscriptions()` — return the set of events this aggregate instance subscribes to

## Variants

### Sagas variant
Interface: `ms.sagas.aggregate.SagaAggregate`
(`simulator/.../ms/sagas/aggregate/SagaAggregate.java`)

Adds:
- `getSagaState()` / `setSagaState(SagaState state)` — semantic lock used to block conflicting concurrent operations
- Inner interface `SagaState` — implemented as an enum per aggregate (e.g., `CourseExecutionSagaState`)

Example (Quizzes): saga aggregates extend `Aggregate` and implement `SagaAggregate`:
```
Execution (abstract) → SagaExecution → implements SagaAggregate
```

## Factories

Each aggregate has a factory class for the Sagas protocol:
- `sagas/factories/SagasXxxFactory.java` — creates `SagaXxx` instances

Factories implement `AggregateFactory<T>` and are injected into services.

## Repositories

Each aggregate has a JPA repository interface for the Sagas protocol:
- `sagas/repositories/XxxCustomRepositorySagas.java` — extends `SagaAggregateRepository`

`XxxCustomRepository.java` (the base interface, no protocol suffix) is a common interface.

## Naming Conventions

| Layer | Pattern | Example |
|-------|---------|---------|
| Base class | `Xxx` (abstract) | `Execution` |
| Saga subclass | `SagaXxx` | `SagaExecution` |
| Saga factory | `SagasXxxFactory` | `SagasExecutionFactory` |
| Saga repository | `XxxCustomRepositorySagas` | `CourseExecutionCustomRepositorySagas` |
| Saga state enum | `XxxSagaState` | `CourseExecutionSagaState` |

## Reference Implementations (Quizzes)

- `applications/quizzes/src/main/java/.../execution/aggregate/Execution.java` — base with event subscriptions
- `applications/quizzes/src/main/java/.../tournament/aggregate/Tournament.java` — multi-service references
