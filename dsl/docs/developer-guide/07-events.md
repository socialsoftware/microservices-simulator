# Step 7 · Events and Subscriptions

[📚 Guide Index](00-index.md)

> **Goal:** define published and subscribed events for cross-aggregate communication in the causal-saga architecture.

[← Back to Step 6](06-web-api.md) · [Next → Step 8](08-sagas.md)

---

## 7.1 Event-Driven Architecture Overview

In the causal-saga architecture, aggregates communicate through events:

- **Published Events**: Notifications that something happened in this aggregate
- **Subscribed Events**: Reactions to events from other aggregates
- **Event Handlers**: Code that processes incoming events

This enables loose coupling while maintaining eventual consistency across bounded contexts.

---

## 7.2 Publishing Events

Events are automatically published when using `@GenerateCrud`:

```nebula
Aggregate User {
    Root Entity User {
        String name;
        String username;
        Boolean active;
    }

    Service UserService {
        @GenerateCrud;
    }
}
```

The update and delete operations automatically register events:

```java
// In UserService.updateUser()
unitOfWorkService.registerEvent(
    new UserUpdatedEvent(newUser.getAggregateId(), newUser.getName(), ...),
    unitOfWork
);

// In UserService.deleteUser()
unitOfWorkService.registerEvent(
    new UserDeletedEvent(newUser.getAggregateId()),
    unitOfWork
);
```

---

## 7.3 Custom Published Events

Define custom events with the fields they carry:

```nebula
Aggregate Course {
    Root Entity Course {
        String name;
        CourseType type;
    }

    Events {
        publishes CourseCreatedEvent {
            Integer aggregateId;
            String name;
            CourseType type;
        }

        publishes CourseNameChangedEvent {
            Integer aggregateId;
            String oldName;
            String newName;
        }
    }
}
```

```java
// CourseCreatedEvent.java
public class CourseCreatedEvent extends Event {
    private Integer aggregateId;
    private String name;
    private CourseType type;

    public CourseCreatedEvent() { }

    public CourseCreatedEvent(Integer aggregateId, String name, CourseType type) {
        this.aggregateId = aggregateId;
        this.name = name;
        this.type = type;
    }

    // getters and setters...
}
```

---

## 7.4 Subscribing to Events

Subscribe to events from other aggregates:

```nebula
Aggregate Execution {
    Entity ExecutionCourse uses dto CourseDto mapping {
        aggregateId -> courseAggregateId;
        name -> courseName;
    } {
        Integer courseAggregateId;
        String courseName;
    }

    Root Entity Execution {
        String acronym;
        ExecutionCourse course;
    }

    Events {
        subscribes CourseNameChangedEvent from Course
            on course.courseAggregateId == event.aggregateId;
    }
}
```

This generates:
1. An **event subscription** that routes matching events
2. An **event handler** that processes the event

---

## 7.5 Event Routing Conditions

The `on` clause specifies when the subscription matches:

```nebula
// Simple ID match
subscribes CourseDeletedEvent from Course
    on course.courseAggregateId == event.aggregateId;

// Multiple conditions (AND logic)
subscribes UserUpdatedEvent from User
    on executionUser.userAggregateId == event.aggregateId
    and executionUser.userVersion < event.version;
```

The routing expression:
- `course.courseAggregateId` - navigates through the entity's fields
- `event.aggregateId` - accesses the incoming event's fields
- Converted to Java getter chains automatically

---

## 7.6 Generated Event Handling

For each subscription, Nebula generates handler infrastructure:

```java
// ExecutionEventHandling.java - Coordinates all handlers for this aggregate
@Component
public class ExecutionEventHandling {
    @Autowired
    private CourseNameChangedEventHandler courseNameChangedEventHandler;

    public void handleEvent(Event event, Execution execution) {
        if (event instanceof CourseNameChangedEvent) {
            courseNameChangedEventHandler.handle(
                (CourseNameChangedEvent) event, execution
            );
        }
    }
}

// CourseNameChangedEventHandler.java - Specific handler logic
@Component
public class CourseNameChangedEventHandler {
    public void handle(CourseNameChangedEvent event, Execution execution) {
        // Update the denormalized data
        ExecutionCourse course = execution.getCourse();
        if (course != null && course.getCourseAggregateId().equals(event.getAggregateId())) {
            course.setCourseName(event.getNewName());
        }
    }
}
```

---

## 7.7 Subscription with Version Tracking

For causal consistency, track the version of the source aggregate:

```nebula
Events {
    subscribes UserUpdatedEvent from User
        routing {
            id: executionUser.userAggregateId;
            version: executionUser.userVersion;
        }
        on executionUser.userAggregateId == event.aggregateId;
}
```

The routing block:
- `id` - The aggregate ID being tracked
- `version` - The last known version

This enables the saga coordination to detect stale reads and ensure causal ordering.

---

## 7.8 Inter-Aggregate Invariants

Define invariants that span aggregates using event subscriptions:

```nebula
Aggregate Quiz {
    Entity QuizExecution uses dto ExecutionDto mapping {
        aggregateId -> executionAggregateId;
    } {
        Integer executionAggregateId;
    }

    Root Entity Quiz {
        QuizExecution execution;
    }

    Events {
        interInvariant QUIZ_EXECUTION_EXISTS {
            subscribes ExecutionDeletedEvent from Execution
                on execution.executionAggregateId == event.aggregateId;
        }
    }
}
```

When the invariant is violated (e.g., execution deleted while quiz exists), the saga coordination handles compensation.

---

## 7.9 Event Processing Flow

1. **Event Published**: Service registers event with unit of work
2. **Event Stored**: Unit of work commits, event persisted
3. **Event Dispatched**: Event processing polls for new events
4. **Subscription Matched**: Routing conditions evaluated
5. **Handler Invoked**: Specific handler processes the event
6. **State Updated**: Denormalized data synchronized

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Course    │────>│   Event     │────>│  Execution  │
│   Service   │     │   Store     │     │   Handler   │
└─────────────┘     └─────────────┘     └─────────────┘
       │                   │                   │
  CourseUpdated ──> Store & Dispatch ──> Update Local Copy
```

---

## 7.10 Complete Events Example

```nebula
Aggregate Topic {
    Entity TopicCourse uses dto CourseDto mapping {
        aggregateId -> courseAggregateId;
        version -> courseVersion;
        name -> courseName;
    } {
        Integer courseAggregateId;
        Integer courseVersion;
        String courseName;
    }

    Root Entity Topic {
        String name;
        TopicCourse course;
    }

    Events {
        publishes TopicCreatedEvent {
            Integer aggregateId;
            String name;
            Integer courseAggregateId;
        }

        publishes TopicDeletedEvent {
            Integer aggregateId;
        }

        subscribes CourseUpdatedEvent from Course
            routing {
                id: course.courseAggregateId;
                version: course.courseVersion;
            }
            on course.courseAggregateId == event.aggregateId;

        subscribes CourseDeletedEvent from Course
            on course.courseAggregateId == event.aggregateId;

        interInvariant TOPIC_COURSE_EXISTS {
            subscribes CourseDeletedEvent from Course
                on course.courseAggregateId == event.aggregateId;
        }
    }
}
```

---

## Recap

Events enable loose coupling between aggregates while maintaining consistency through eventual synchronization. Published events notify about state changes, subscribed events update denormalized copies, and inter-invariants enforce cross-aggregate business rules. The generated event handling infrastructure routes events based on conditions you define in the DSL.

[← Back to Step 6](06-web-api.md) · [Next → Step 8](08-sagas.md)
