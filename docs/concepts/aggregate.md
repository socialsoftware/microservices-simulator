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

Each aggregate has two factory classes:
- `{Aggregate}Factory.java` — interface declaring `create{Aggregate}(...)` and `create{Aggregate}Copy(...)` (profile-agnostic; injected into services)
- `sagas/factories/Sagas{Aggregate}Factory.java` — `@Service @Profile("sagas")` concrete class implementing the interface; returns `Saga{Aggregate}` instances

```java
// Interface — microservices/{aggregate}/aggregate/{Aggregate}Factory.java
public interface CourseFactory extends AggregateFactory<Course> {
    Course createCourse(Integer aggregateId, String name, String type);
    Course createCourseCopy(Course other);
    CourseDto createCourseDto(Course course);
}

// Sagas implementation — returns covariant Saga subtype
@Service
@Profile("sagas")
public class SagasCourseFactory implements CourseFactory {
    @Override
    public SagaCourse createCourse(Integer aggregateId, String name, String type) {
        SagaCourse course = new SagaCourse(aggregateId, name, type);
        course.verifyInvariants();
        return course;
    }
    // createCourseCopy, createCourseDto ...
}
```

Factories implement `AggregateFactory<T>` and are injected into services via the interface — never the concrete `Sagas*` class.

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

---

## Reference Implementations (Quizzes)

- `applications/quizzes/src/main/java/.../execution/aggregate/Execution.java` — base with event subscriptions
- `applications/quizzes/src/main/java/.../tournament/aggregate/Tournament.java` — multi-service references
