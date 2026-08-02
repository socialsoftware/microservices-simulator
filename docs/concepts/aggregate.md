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

`simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/aggregate/Aggregate.java`

Abstract methods every subclass must implement:
- `verifyInvariants()` — throw if any intra-invariant is violated
- `getEventSubscriptions()` — return the set of events this aggregate instance subscribes to

`verifyInvariants()` **must not perform repository reads** (R6, [`../architecture.md`](../architecture.md)):
it runs inside the UoW commit path, where repository calls risk deadlocks. Check only fields already
present on the aggregate instance. A rule that genuinely needs a DB read is a P3 service-layer guard
instead — see [`rule-enforcement-patterns.md`](rule-enforcement-patterns.md).

`getEventSubscriptions()` is implemented in the **downstream (consumer)** aggregate only (R5) — a
publisher never subscribes to its own events.

## Variants

### Sagas variant
Interface: `ms.transaction.sagas.aggregate.SagaAggregate`
(`simulator/.../ms/transaction/sagas/aggregate/SagaAggregate.java`)

Adds:
- `getSagaState()` / `setSagaState(SagaState state)` — semantic lock used to block conflicting concurrent operations
- Inner interface `SagaState` — implemented as an enum per aggregate (e.g., `CourseExecutionSagaState`)

Example: saga aggregates extend `Aggregate` and implement `SagaAggregate`:
```
Execution (abstract) → SagaExecution → implements SagaAggregate
```

## Factories

Each aggregate has two factory classes:
- `{Aggregate}Factory.java` — interface declaring `create{Aggregate}(...)` and `create{Aggregate}Copy(...)` (profile-agnostic; injected into services)
- `sagas/factories/Sagas{Aggregate}Factory.java` — `@Service @Profile("sagas")` concrete class implementing the interface; returns `Saga{Aggregate}` instances

```java
// Interface — microservices/{aggregate}/aggregate/{Aggregate}Factory.java
public interface {Aggregate}Factory {
    {Aggregate} create{Aggregate}(Integer aggregateId, ...);
    {Aggregate} create{Aggregate}Copy({Aggregate} existing);
    {Aggregate}Dto create{Aggregate}Dto({Aggregate} {aggregate});
}

// Sagas implementation — returns covariant Saga subtype
@Service
@Profile("sagas")
public class Sagas{Aggregate}Factory implements {Aggregate}Factory {
    @Override
    public Saga{Aggregate} create{Aggregate}(Integer aggregateId, ...) {
        return new Saga{Aggregate}(aggregateId, ...);
    }

    @Override
    public Saga{Aggregate} create{Aggregate}Copy({Aggregate} existing) {
        return new Saga{Aggregate}((Saga{Aggregate}) existing);
    }

    @Override
    public {Aggregate}Dto create{Aggregate}Dto({Aggregate} {aggregate}) {
        return new {Aggregate}Dto({aggregate});
    }
}
```

The interface is a **plain Java interface with no supertype and no annotations** - there is no
`AggregateFactory<T>` base type in `simulator/`. Its three methods are typed against the abstract
aggregate and the DTO, so no sagas type appears in a signature. Services inject the interface, never
the concrete `Sagas*` class.

## Repositories

Each aggregate has three repository artifacts:

| File | Shape | Purpose |
|------|-------|---------|
| `aggregate/{Aggregate}Repository.java` | interface extending `AggregateRepository` (no type arguments) | Spring Data JPA repository; holds any JPQL `@Query` methods |
| `aggregate/{Aggregate}CustomRepository.java` | plain Java interface, no annotations | Profile-agnostic contract the service injects; declares only the custom query signatures the service needs, and may be empty |
| `sagas/repositories/{Aggregate}CustomRepositorySagas.java` | `@Service @Profile("sagas")` class **implementing** `{Aggregate}CustomRepository` | Sagas implementation; holds an `@Autowired {Aggregate}Repository` and delegates to it |

`AggregateRepository` (`simulator/.../ms/aggregate/AggregateRepository.java`) is **not generic**: it
is declared `interface AggregateRepository extends JpaRepository<Aggregate, Integer>`, so
`{Aggregate}Repository` extends it bare. Any JPQL a subinterface adds is written against the
concrete aggregate class (`select a from {Aggregate} a where ...`), not against a type parameter.

```java
@Service
@Profile("sagas")
public class {Aggregate}CustomRepositorySagas implements {Aggregate}CustomRepository {
    @Autowired
    private {Aggregate}Repository {aggregate}Repository;

    // One method per signature declared on {Aggregate}CustomRepository, each delegating
    // to a @Query method on {Aggregate}Repository. Empty until a service method needs one.
}
```

`{Aggregate}CustomRepositorySagas` does **not** extend `SagaAggregateRepository`. It is a Spring
`@Service`, not a JPA repository interface.

For the JPQL these methods delegate to — including the latest-active-version filter most custom
queries need — see [`service.md`](service.md) § Custom Repository — Latest-Active-Version Query.

`SagaAggregateRepository`
(`simulator/.../ms/transaction/sagas/aggregate/SagaAggregateRepository.java`) is framework-internal.
It declares exactly three queries, each returning `Optional<Aggregate>` for the highest-version row
of one `aggregateId`: `findNonDeletedSagaAggregate`, `findDeletedSagaAggregate` and
`findAnySagaAggregate`. Application code does not call them - `SagaUnitOfWorkService` does, behind
`aggregateLoadAndRegisterRead`.

## Naming Conventions

| Layer | Pattern | Example |
|-------|---------|---------|
| Base class | `Xxx` (abstract) | `Execution` |
| Saga subclass | `SagaXxx` | `SagaExecution` |
| Saga factory | `SagasXxxFactory` | `SagasExecutionFactory` |
| Saga repository | `XxxCustomRepositorySagas` | `CourseExecutionCustomRepositorySagas` |
| Saga state enum | `XxxSagaState` | `CourseExecutionSagaState` |

## getEventSubscriptions() Implementation

`getEventSubscriptions()` builds the set of `EventSubscription` objects from the aggregate's cached snapshot fields. It is called by the UoW on every commit to determine which events the current aggregate version listens to.

```java
@Override
public Set<EventSubscription> getEventSubscriptions() {
    Set<EventSubscription> eventSubscriptions = new HashSet<>();
    if (getState() == AggregateState.ACTIVE) {
        interInvariantUsersExist(eventSubscriptions);
        // add one helper call per inter-invariant
    }
    return eventSubscriptions;
}

private void interInvariantUsersExist(Set<EventSubscription> eventSubscriptions) {
    for (CourseExecutionStudent student : this.students) {
        eventSubscriptions.add(new CourseExecutionSubscribesRemoveUser(student));
    }
}
```

Rules:
- Guard with `getState() == ACTIVE` so deleted aggregates shed their subscriptions.
- One private helper method per inter-invariant; name it after the invariant (e.g. `interInvariantUsersExist`).
- Each helper adds one `EventSubscription` subclass per referenced upstream entity.
- If the aggregate has no inter-invariants, return an empty set.

See [`concepts/events.md`](events.md) for the `EventSubscription` subclass template.
