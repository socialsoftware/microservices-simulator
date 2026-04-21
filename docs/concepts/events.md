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
        super(course.getCourseAggregateId(), course.getCourseVersion(), CreateQuestionEvent.class);
    }

    @Override
    public boolean filter(Event event) {
        CreateQuestionEvent e = (CreateQuestionEvent) event;
        return e.getCourseAggregateId().equals(this.subscribedAggregateId);
    }
}
```

`subscribedAggregateId` must match `publisherAggregateId` in the event.

## EventHandler

Located in `microservices/<aggregate>/notification/handling/handlers/`.

Each handler receives a matching event, verifies it passes the subscription filter, extracts needed data, and calls `<Aggregate>EventProcessing`.

```java
public class CreateQuestionEventHandler extends EventHandler {
    @Override
    public void handleEvent(Event event) {
        // filter already applied by EventApplicationService
        CreateQuestionEvent e = (CreateQuestionEvent) event;
        eventProcessing.processCreateQuestionEvent(e);
    }
}
```

## Polling

Located in `microservices/<aggregate>/notification/handling/<Aggregate>EventHandling.java`.

Each event type gets one `@Scheduled` method:

```java
@Scheduled(fixedDelay = 1000)
public void handleCreateQuestionEvents() {
    eventApplicationService.handleSubscribedEvent(CreateQuestionEvent.class,
            new CreateQuestionEventHandler(repository, eventProcessing));
}
```

The `@Scheduled` annotation does **not** run in `@DataJpaTest` — call the method manually in tests.

## Naming Conventions

| Layer | Pattern | Example |
|-------|---------|---------|
| Event class | `XxxEvent` | `CreateQuestionEvent` |
| Subscription | `ConsumerSubscribesXxx` | `ExecutionSubscribesCreateQuestion` |
| Handler | `XxxEventHandler` | `CreateQuestionEventHandler` |
| Polling bean | `<Consumer>EventHandling` | `CourseExecutionEventHandling` |
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
        super(ref.getAnchorAggregateId(), ref.getAnchorVersion(), <EventName>.class);
    }

    public <Consumer>Subscribes<Xxx>() {}

    @Override
    public boolean filter(Event event) {
        <EventName> e = (<EventName>) event;
        return e.getAnchorAggregateId().equals(this.subscribedAggregateId);
    }
}
```

`subscribedAggregateId` (from the `super(...)` call) must match `publisherAggregateId` used in the event constructor.

### Handler
```java
public class <Xxx>EventHandler extends <Consumer>EventHandler {
    public <Xxx>EventHandler(<Consumer>Repository repo, <Consumer>EventProcessing processing) {
        super(repo, processing);
    }

    @Override
    public void handleEvent(Integer aggregateId, Event event) {
        this.consumerEventProcessing.process<Xxx>Event(aggregateId, (<EventName>) event);
    }
}
```

### Polling method
```java
/*
    <INVARIANT_NAME>
*/
@Scheduled(fixedDelay = 1000)
public void handle<Xxx>Events() {
    eventApplicationService.handleSubscribedEvent(<EventName>.class,
            new <Xxx>EventHandler(consumerRepository, eventProcessing));
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

`aggregateId` is the consumer aggregate's ID (passed down from the handler). The `<Consumer>Functionalities` update method opens its own UoW, loads the consumer aggregate, applies the cached-state update, and commits.

---

## Reference Implementations (Quizzes)

- `applications/quizzes/src/main/java/.../events/CreateQuestionEvent.java`
- `applications/quizzes/src/main/java/.../execution/notification/subscribe/CourseExecutionSubscribesRemoveUser.java`
- `applications/quizzes/src/main/java/.../execution/notification/handling/CourseExecutionEventHandling.java`