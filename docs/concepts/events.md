# Events

## What They Are

Domain events are the mechanism for **eventual consistency** across aggregates. When an aggregate is committed, it may register one or more events. Downstream aggregates poll for those events and react by updating their local state.

## Event Classes

Located in `applications/quizzes/src/main/java/.../quizzes/events/`.

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

**Critical:** `publisherAggregateId` (the argument to `super(...)`) must be the aggregate ID used as the **subscription anchor**. For TCC, this is used for version-tracking. Using the wrong ID breaks TCC causal consistency.

## Publishing Events

In the publisher service's method, after modifying the aggregate:

```java
unitOfWorkService.registerEvent(new XxxEvent(entityId, anchorAggregateId), unitOfWork);
```

The event is persisted when the UoW commits.

## EventSubscription

Each subscriber aggregate declares which events it watches via `getEventSubscriptions()`. Located in `microservices/<aggregate>/events/subscribe/`.

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

Located in `microservices/<aggregate>/events/handling/handlers/`.

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

Located in `microservices/<aggregate>/events/handling/<Aggregate>EventHandling.java`.

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

## Reference Implementations

- `applications/quizzes/src/main/java/.../events/CreateQuestionEvent.java`
- `applications/quizzes/src/main/java/.../execution/events/subscribe/ExecutionSubscribesCreateQuestion.java`
- `applications/quizzes/src/main/java/.../execution/events/handling/CourseExecutionEventHandling.java`
- See also: [`docs/examples/cannot-delete-last-execution-with-content.md`](../examples/cannot-delete-last-execution-with-content.md)
