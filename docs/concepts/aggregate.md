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

### TCC variant
Interface: `ms.causal.aggregate.CausalAggregate`
(`simulator/.../ms/causal/aggregate/CausalAggregate.java`)

Adds:
- `getMutableFields()` — names of fields that may change concurrently
- `getIntentions()` — pairs of fields that must change together (conflict detection)
- `mergeFields(...)` — field-level merge logic called when two concurrent versions are detected

## Factories

Each aggregate has two factory classes (one per protocol):
- `sagas/factories/SagasXxxFactory.java` — creates `SagaXxx` instances
- `causal/factories/CausalXxxFactory.java` — creates `CausalXxx` instances

Factories implement `AggregateFactory<T>` and are injected into services.

## Repositories

Each aggregate has two JPA repository interfaces:
- `sagas/repositories/XxxCustomRepositorySagas.java` — extends `SagaAggregateRepository`
- `causal/repositories/XxxCustomRepositoryTCC.java` — extends `CausalAggregateRepository`

`XxxCustomRepository.java` (the base interface, no protocol suffix) is a common interface shared by both.

## Naming Conventions

| Layer | Pattern | Example |
|-------|---------|---------|
| Base class | `Xxx` (abstract) | `Execution` |
| Saga subclass | `SagaXxx` | `SagaExecution` |
| TCC subclass | `CausalXxx` | `CausalExecution` |
| Saga factory | `SagasXxxFactory` | `SagasExecutionFactory` |
| TCC factory | `CausalXxxFactory` | `CausalExecutionFactory` |
| Saga repository | `XxxCustomRepositorySagas` | `CourseExecutionCustomRepositorySagas` |
| TCC repository | `XxxCustomRepositoryTCC` | `CourseExecutionCustomRepositoryTCC` |
| Saga state enum | `XxxSagaState` | `CourseExecutionSagaState` |

## Reference Implementations (Quizzes)

- `applications/quizzes/src/main/java/.../execution/aggregate/Execution.java` — base with event subscriptions
- `applications/quizzes/src/main/java/.../tournament/aggregate/Tournament.java` — multi-service references
