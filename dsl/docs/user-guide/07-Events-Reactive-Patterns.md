# Events and Reactive Patterns

This chapter covers Nebula's event system: publishing events, subscribing to events from other aggregates, and reacting to cross-aggregate changes.

> **Tied example:** [`05-eventdriven`](../../abstractions/05-eventdriven/): Author and Post aggregates with event publishing and subscription.

## Domain Overview

The `05-eventdriven` example models a blog domain:

```
Author ──publishes──→ AuthorUpdatedEvent ──subscribed by──→ Post (projection sync)
Author ──publishes──→ AuthorDeletedEvent ──subscribed by──→ Post (cascade INACTIVE)
```

- **Author** publishes events when updated or deleted
- **Post** subscribes to those events to keep its local author data in sync and to cascade state changes

## Publishing Events

Events can be published in two ways.

### Explicit Declaration in Events Block

Define events with typed fields in the `Events` block:

```nebula
Events {
    publish AuthorUpdatedEvent {
        String name
        String bio
    }

    publish AuthorDeletedEvent {
        String name
    }
}
```

Each published event generates a Java event class extending the simulator's `Event` base class:

```java
public class AuthorUpdatedEvent extends Event {
    private String name;
    private String bio;
    // Constructor, getters, setters
}
```

### Inferred from `publishes` in Methods

Events can also be published inline from method action bodies using `publishes`:

```nebula
Methods {
    @PostMapping("/users/{userId}/loyalty")
    awardLoyaltyPoints(Integer userId, Integer points) {
        action {
            load User(userId) as user
            user.loyaltyPoints = points
        }
        publishes UserLoyaltyAwardedEvent {
            userAggregateId: user.aggregateId,
            pointsAwarded: points
        }
    }
}
```

When using `publishes`, the event class is auto-generated with field types inferred from the assignment expressions. No separate `publish` declaration in the `Events` block is needed.

### Event Dispatch

Events are registered on the Unit of Work and dispatched only when the transaction commits:

```java
unitOfWorkService.registerEvent(new AuthorDeletedEvent(author.getAggregateId()), unitOfWork);
```

If the transaction fails, no events are sent.

## Subscribing to Events

### Simple Subscriptions (Projection Sync)

Subscribe to update events to keep local projection data in sync:

```nebula
Events {
    subscribe AuthorUpdatedEvent
}
```

This generates an event handler that refreshes local copies of data when the source aggregate changes. For example, when an Author's name changes, all Posts referencing that author get their local `authorName` updated automatically.

### Event Flow

```
1. AuthorService registers event
   unitOfWorkService.registerEvent(new AuthorUpdatedEvent(...), unitOfWork)

2. UnitOfWork commits → event persisted

3. PostEventProcessing polls for new events (@Scheduled)

4. Subscription classes match events to affected aggregates
   PostSubscribesAuthorUpdated

5. Event handlers process updates
   AuthorUpdatedEventHandler refreshes local projection data
```

## Reactive Subscriptions with `when` + `action`

For subscriptions that need to react to an event with specific logic, use `when` (match condition) and `action` (what to do):

```nebula
Events {
    subscribe AuthorDeletedEvent from Author {
        when author.authorAggregateId == event.aggregateId
        action {
            this.state = INACTIVE
        }
    }
}
```

Breaking this down:
- **`subscribe AuthorDeletedEvent from Author`**: listen for this event from the Author aggregate
- **`when author.authorAggregateId == event.aggregateId`**: only react if the deleted author is the one this Post references
- **`action { this.state = INACTIVE }`**: mark this Post as INACTIVE

This is the simulator's native referential integrity model. Deletes always succeed on the source side; affected subscribers react by cascading their state.

### Both `when` and `action` Are Explicit

The DSL requires you to specify both what triggers the reaction and what happens. There is no hidden cascade logic — a reader of the `.nebula` file sees exactly what the subscription does.

### Multiple Subscriptions

An aggregate can subscribe to events from multiple sources:

```nebula
Events {
    subscribe MemberDeletedEvent from Member {
        when member.memberAggregateId == event.aggregateId
        action {
            this.state = INACTIVE
        }
    }

    subscribe BookDeletedEvent from Book {
        when book.bookAggregateId == event.aggregateId
        action {
            this.state = INACTIVE
        }
    }
}
```

Each subscription independently monitors its referenced aggregate.

### Mixing Simple and Reactive Subscriptions

You can combine bare subscriptions (for projection sync) with reactive subscriptions (for cascade logic):

```nebula
Events {
    subscribe AuthorUpdatedEvent

    subscribe AuthorDeletedEvent from Author {
        when author.authorAggregateId == event.aggregateId
        action {
            this.state = INACTIVE
        }
    }
}
```

- `subscribe AuthorUpdatedEvent`: auto-refreshes projection fields when author data changes
- `subscribe AuthorDeletedEvent ... { when ... action ... }`: cascades to INACTIVE when author is deleted

## Complete Example: Author and Post

### Author (event publisher)

```nebula
Aggregate Author {

    Root Entity Author {
        String name
        String bio

        invariants {
            name.length() > 0 : "Author name cannot be blank"
        }
    }

    Events {
        publish AuthorUpdatedEvent {
            String name
            String bio
        }

        publish AuthorDeletedEvent {
            String name
        }
    }
}
```

### Post (event subscriber)

```nebula
Aggregate Post {

    Entity PostAuthor {
        from Author { name as authorName }
    }

    Root Entity Post {
        String title
        String content
        PostAuthor author
        LocalDateTime publishedAt

        invariants {
            title.length() > 0 : "Post title cannot be blank"
            author != null : "Post must have an author"
        }
    }

    Events {
        subscribe AuthorUpdatedEvent

        subscribe AuthorDeletedEvent from Author {
            when author.authorAggregateId == event.aggregateId
            action {
                this.state = INACTIVE
            }
        }
    }
}
```

This demonstrates:
- **Event publishing**: Author emits `AuthorUpdatedEvent` and `AuthorDeletedEvent`
- **Simple subscription**: Post subscribes to `AuthorUpdatedEvent` for projection data sync
- **Reactive subscription**: Post subscribes to `AuthorDeletedEvent` with `when` + `action` for referential integrity cascade

### Generate and verify:

```bash
cd dsl/nebula
./bin/cli.js generate ../abstractions/05-eventdriven/
```

Explore the generated event infrastructure:
- `AuthorUpdatedEvent.java` / `AuthorDeletedEvent.java`: event classes
- `PostEventProcessing.java`: event routing and cascade logic
- `PostSubscribesAuthorDeletedAuthorRef.java`: subscription class matching events to affected Posts

---

**Previous:** [06-Cross-Aggregate-References](06-Cross-Aggregate-References.md) | **Next:** [08-Tutorial-Library-System](08-Tutorial-Library-System.md)
