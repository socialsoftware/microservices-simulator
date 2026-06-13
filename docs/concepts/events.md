# Events

## What They Are

Domain events are the mechanism for **eventual consistency** across aggregates. When an aggregate is committed, it may register one or more events. Downstream aggregates poll for those events and react by updating their local state.

## Event Classes

Located in `src/main/java/.../<appName>/events/` (e.g., `applications/quizzes/src/main/java/.../quizzes/events/`).

Each event extends `Event` from `ms.domain.event`:

```java
public class CreateQuestionEvent extends Event {
    private Integer questionAggregateId;
    private Integer courseAggregateId;

    public CreateQuestionEvent(Integer questionAggregateId, Integer courseAggregateId) {
        super(courseAggregateId);  // publisherAggregateId
        this.questionAggregateId = questionAggregateId;
        this.courseAggregateId = courseAggregateId;
    }
}
```

**Critical:** `publisherAggregateId` (the argument to `super(...)`) must be the aggregate ID used as the **subscription anchor**. Using the wrong ID breaks event filtering.

## Publishing Events

In the publisher service's method, after modifying the aggregate:

```java
unitOfWorkService.registerEvent(new XxxEvent(entityId, anchorAggregateId), unitOfWork);
```

The event is persisted when the UoW commits.

## EventSubscription

Each subscriber aggregate declares which events it watches via `getEventSubscriptions()`. Located in `microservices/<aggregate>/notification/subscribe/`.

```java
public class ExecutionSubscribesCreateQuestion extends EventSubscription {
    public ExecutionSubscribesCreateQuestion(CourseExecutionCourse course) {
        super(course.getCourseAggregateId(), course.getCourseVersion(), CreateQuestionEvent.class.getSimpleName());
    }

    public ExecutionSubscribesCreateQuestion() {}
}
```

In the **sagas profile**, matching is performed by the infrastructure via a DB query on `subscribedAggregateId` and `subscribedVersion` — `EventApplicationService.handleSubscribedEvent()` does **not** call `subscribesEvent()`. Any additional filtering (e.g., checking a discriminating field for shared-anchor events) must be implemented in the service-layer ByEvent method. Note: the TCC profile's `CausalUnitOfWork` does call `subscribesEvent()` for causal consistency checks — a `subscribesEvent()` override is meaningful there but irrelevant for sagas.

`subscribedAggregateId` must match `publisherAggregateId` in the event.

**`subscribedVersion`:** pass the anchor entity's current version so that only events published *after* the snapshot was taken are processed. If the subscriber entity does not track the publisher's version (e.g., a `User` cached inside an `Execution` that has no `userVersion` field), use `0L` — this means all events from that user since the beginning are eligible for processing, which is functionally correct but slightly broader than necessary.

## EventHandler

Located in `microservices/<aggregate>/notification/handling/handlers/`.

Use **one concrete `{Consumer}EventHandler` class** per consumer aggregate. It extends `EventHandler` from the simulator core and dispatches all subscribed event types via `instanceof`, delegating to `{Consumer}EventProcessing`. Do **not** create a separate handler subclass per event type.

```java
@Component
public class ExecutionEventHandler extends EventHandler {

    private final ExecutionEventProcessing executionEventProcessing;

    public ExecutionEventHandler(ExecutionRepository repository,
                                 ExecutionEventProcessing executionEventProcessing) {
        super(repository);
        this.executionEventProcessing = executionEventProcessing;
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        if (event instanceof DeleteUserEvent e) {
            executionEventProcessing.processDeleteUserEvent(subscriberAggregateId, e);
        } else if (event instanceof UpdateStudentNameEvent e) {
            executionEventProcessing.processUpdateStudentNameEvent(subscriberAggregateId, e);
        } else if (event instanceof AnonymizeStudentEvent e) {
            executionEventProcessing.processAnonymizeStudentEvent(subscriberAggregateId, e);
        }
    }
}
```

The handler does **not** load or mutate the aggregate — that happens inside `{Consumer}Functionalities` ByEvent methods.

## Polling

Located in `microservices/<aggregate>/notification/handling/<Aggregate>EventHandling.java`.

Each event type gets one `@Scheduled` method. All methods pass the **same** `{Consumer}EventHandler` bean instance:

```java
@Component
public class ExecutionEventHandling {

    @Autowired
    private EventApplicationService eventApplicationService;

    @Autowired
    private ExecutionEventHandler executionEventHandler;

    @Scheduled(fixedDelay = 1000)
    public void handleDeleteUserEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteUserEvent.class, executionEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleUpdateStudentNameEvents() {
        eventApplicationService.handleSubscribedEvent(UpdateStudentNameEvent.class, executionEventHandler);
    }
}
```

The `@Scheduled` annotation does **not** run in `@DataJpaTest` — call the method manually in tests.

## Naming Conventions

| Layer | Pattern | Example |
|-------|---------|---------|
| Event class | `XxxEvent` | `CreateQuestionEvent` |
| Subscription | `ConsumerSubscribesXxx` | `ExecutionSubscribesCreateQuestion` |
| Handler | `{Consumer}EventHandler` (single dispatcher) | `ExecutionEventHandler` |
| Polling bean | `<Consumer>EventHandling` | `ExecutionEventHandling` |
| Processing | `<Consumer>EventProcessing` | `ExecutionEventProcessing` |

## Canonical Wiring Snippet

Use this exact pattern in all skills and implementations. All skill files must reference this snippet rather than defining their own variant.

### Event class
```java
public class <EventName> extends Event {
    private Integer entityAggregateId;
    private Integer anchorAggregateId;

    public <EventName>(Integer entityAggregateId, Integer anchorAggregateId) {
        super(anchorAggregateId);  // publisherAggregateId = subscription anchor
        this.entityAggregateId = entityAggregateId;
        this.anchorAggregateId = anchorAggregateId;
    }
    // getters only — no setters
}
```

**`publisherAggregateId`** (passed to `super(...)`) must be the **subscription anchor** — the owning/parent aggregate ID used for version tracking. For a child entity event, this is the parent aggregate's ID, not the child entity's ID.

### Subscription class
```java
public class <Consumer>Subscribes<Xxx> extends EventSubscription {
    public <Consumer>Subscribes<Xxx>(SomeRef ref) {
        super(ref.getAnchorAggregateId(), ref.getAnchorVersion(), <EventName>.class.getSimpleName());
    }

    public <Consumer>Subscribes<Xxx>() {}
}
```

`subscribedAggregateId` (from the `super(...)` call) must match `publisherAggregateId` used in the event constructor. In the sagas profile, `EventApplicationService` does **not** call `subscribesEvent()` — do not override it for sagas event filtering; use the service-layer ByEvent method instead.

### Handler (single dispatcher)

One `{Consumer}EventHandler` bean per consumer aggregate — **not** one subclass per event type:

```java
@Component
public class {Consumer}EventHandler extends EventHandler {

    private final {Consumer}EventProcessing {consumer}EventProcessing;

    public {Consumer}EventHandler({Consumer}Repository repository,
                                  {Consumer}EventProcessing {consumer}EventProcessing) {
        super(repository);
        this.{consumer}EventProcessing = {consumer}EventProcessing;
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        if (event instanceof <EventName> e) {
            {consumer}EventProcessing.process<Xxx>Event(subscriberAggregateId, e);
        }
        // additional instanceof branches for each subscribed event type
    }
}
```

### Polling method

Each `@Scheduled` method passes the shared handler bean:

```java
@Scheduled(fixedDelay = 1000)
public void handle<Xxx>Events() {
    eventApplicationService.handleSubscribedEvent(<EventName>.class, {consumer}EventHandler);
}
```

### EventProcessing class

`EventProcessing` is the bridge between the event handler and the update functionality. It receives the raw event from the handler and delegates to a `*Functionalities` method that performs the actual aggregate update.

```java
@Service
public class <Consumer>EventProcessing {

    @Autowired
    private <Consumer>Functionalities <consumer>Functionalities;

    public void process<Xxx>Event(Integer aggregateId, <EventName> event) {
        <consumer>Functionalities.<updateMethod>(aggregateId, event.get<RelevantField>());
    }
}
```

`aggregateId` is the consumer aggregate's ID (passed down from the handler). The `<Consumer>Functionalities` update method opens its own UoW, loads the consumer aggregate, checks `sagaState != NOT_IN_SAGA` (skipping the update if the aggregate is mid-saga to avoid conflicting with its in-progress state), applies the cached-field update, calls `verifyInvariants()`, and commits.

---

### ByEvent sagaState guard

For every event that mirrors an operation also exposed as a saga `Functionalities` method (e.g., `updateStudentName`, `removeStudentFromExecution`), add a separate `{operation}ByEvent` method to `<Consumer>Functionalities`. It opens its own `UnitOfWork`, loads the aggregate directly from the service, applies the cached-field change, calls `verifyInvariants()`, and commits — **without starting a new saga**.

```java
public void {operation}ByEvent(Integer aggregateId, ...) {
    SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork();
    {Consumer} aggregate = ({Consumer}) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
    if (!GenericSagaState.NOT_IN_SAGA.equals(((SagaAggregate) aggregate).getSagaState())) {
        return;  // skip — aggregate is mid-saga; avoid conflicting with in-progress state
    }
    {consumer}Service.{operation}(aggregate, ..., unitOfWork);
    unitOfWorkService.commit(unitOfWork);
}
```

**Why not call the saga `Functionalities` method from `EventProcessing`?** Saga methods set a semantic lock (`sagaState`) and trigger compensations; calling them from an event handler creates a circular saga loop (the saga emits another event → handler fires again → infinite loop). The `ByEvent` method sidesteps this by talking directly to the service layer.

**Where the guard goes.** Put the `sagaState != NOT_IN_SAGA` check inside the `{operation}ByEvent` method **after the load** — not in the shared service method. If the guard lived in a service method that is also called from saga steps on the same aggregate, those saga steps would silently be skipped.

**When to skip the guard.** Only when the event must apply even while the aggregate is mid-saga (rare). For standard cached-field updates and sub-entity removals, always skip when `sagaState != NOT_IN_SAGA`. For whole-consumer invalidation via `copy.remove()`, apply the same guard unless a T4 test explicitly requires processing during an in-flight saga on the same aggregate.

---

## Cascade Invalidation Pattern

A consumer aggregate that processes a deletion event and becomes non-functional should both mark itself deleted **and** publish its own outbound event so its downstream consumers can react.

**When to use:** when the ByEvent method for a deletion event calls `copy.remove()` on the consumer (see `implement-aggregate/session-d.md` — "Deletion events: remove() on the whole consumer"). Simply marking the consumer `DELETED` and committing is not enough if other aggregates cache references to it.

**Steps:**

1. In the ByEvent service method, after calling `copy.remove()`, register an outbound invalidation event:

```java
// In {Consumer}Service.{operation}ByEvent(...)
copy.remove();
unitOfWorkService.registerEvent(new Invalidate{Consumer}Event(copy.getAggregateId()), unitOfWork);
```

2. Define `Invalidate{Consumer}Event` with `publisherAggregateId` set to the consumer's own aggregate ID:

```java
public class Invalidate{Consumer}Event extends Event {
    public Invalidate{Consumer}Event(Integer consumerAggregateId) {
        super(consumerAggregateId);  // publisherAggregateId = this aggregate's ID
    }
}
```

3. Downstream aggregates that cache a reference to `{Consumer}` subscribe to `Invalidate{Consumer}Event` and process it the same way — either removing the sub-entity from a collection or cascading their own invalidation.

**Key invariant:** the outbound invalidation event must use the consumer's own aggregate ID as `publisherAggregateId` so that downstream `EventSubscription` instances anchored to that ID receive it. This is the same rule that applies to all events: `super(anchorAggregateId)` must match the `subscribedAggregateId` of the downstream subscriber.

---

## Reference Implementations (Quizzes)

- `applications/quizzes/src/main/java/.../events/CreateQuestionEvent.java`
- `applications/quizzes/src/main/java/.../execution/notification/subscribe/CourseExecutionSubscribesRemoveUser.java`
- `applications/quizzes/src/main/java/.../execution/notification/handling/CourseExecutionEventHandling.java`