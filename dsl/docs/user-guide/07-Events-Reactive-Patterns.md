# Events and Reactive Patterns

This chapter covers Nebula's event system: publishing events, subscribing to events from other aggregates, and using inter-invariants for referential integrity across aggregate boundaries.

> **Tied example:** [`05-eventdriven`](../examples/abstractions/05-eventdriven/): Author and Post aggregates with event publishing and subscription.

## Domain Overview

The `05-eventdriven` example models a blog domain:

```
Author ──publishes──→ AuthorUpdatedEvent ──subscribed by──→ Post
Author ──publishes──→ AuthorDeletedEvent ──subscribed by──→ Post (inter-invariant)
```

- **Author** publishes events when updated or deleted
- **Post** subscribes to those events to keep its local author data in sync and enforce referential integrity

## Publishing Events

Define events that an aggregate emits using the `publish` keyword inside an `Events` block:

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

Each published event has:
- A **name**:becomes the Java event class name
- **Fields**:data carried by the event

### What Gets Generated

For each published event, Nebula generates:

1. **Event class**:extends the simulator's `Event` base class:

```java
public class AuthorUpdatedEvent extends Event {
    private String name;
    private String bio;

    // Constructor, getters, setters
}
```

2. **Publishing logic** in the service: events are registered via the Unit of Work Service:

```java
AuthorDeletedEvent event = new AuthorDeletedEvent(author.getAggregateId());
event.setPublisherAggregateVersion(author.getVersion());
unitOfWorkService.registerEvent(event, unitOfWork);
```

Events are dispatched only when the Unit of Work commits, ensuring they are not sent if the transaction fails.

## Subscribing to Events

### Simple Subscriptions

Subscribe to events from other aggregates for data synchronization:

```nebula
Events {
    subscribe AuthorUpdatedEvent
}
```

This generates an event handler that updates local copies of data when the source aggregate changes. For example, when an Author's name changes, all Posts referencing that author get their local `authorName` updated.

### Event Flow

Events flow through the system via scheduled polling:

```
1. AuthorService registers event
   unitOfWorkService.registerEvent(new AuthorUpdatedEvent(...), unitOfWork)

2. UnitOfWork commits → event persisted

3. PostEventProcessing polls for new events (@Scheduled)
   eventApplicationService.handleSubscribedEvent(...)

4. Subscription classes match events to affected aggregates
   PostSubscribesAuthorUpdated

5. Event handlers process updates
   AuthorUpdatedEventHandler updates local data
```

## Inter-Invariants

Inter-invariants are the mechanism for enforcing referential integrity across aggregate boundaries. They combine event subscriptions with matching conditions.

### Syntax

```nebula
Events {
    interInvariant AUTHOR_EXISTS {
        subscribe AuthorDeletedEvent from Author {
            author.authorAggregateId == event.aggregateId
        }
    }
}
```

Breaking this down:
- `interInvariant AUTHOR_EXISTS`:named constraint group
- `subscribe AuthorDeletedEvent from Author`:listen for this event from the Author aggregate
- `{ author.authorAggregateId == event.aggregateId }`:matching condition: applies to Posts whose local author reference matches the deleted Author's ID

### How They Work

Inter-invariants work together with the `References` block from [Chapter 06](06-Cross-Aggregate-References.md):

1. The **inter-invariant** detects the event and identifies affected aggregates
2. The **reference action** (`prevent`, `cascade`, `setNull`) determines the response

For example:

```nebula
References {
    author -> Author {
        onDelete: cascade
        message: "Author deleted, removing their posts"
    }
}

Events {
    interInvariant AUTHOR_EXISTS {
        subscribe AuthorDeletedEvent from Author {
            author.authorAggregateId == event.aggregateId
        }
    }
}
```

When an Author is deleted:
1. `AuthorDeletedEvent` is published
2. Post's inter-invariant matches Posts with `author.authorAggregateId == event.aggregateId`
3. The generated `processAuthorDeletedEvent` method in `PostEventProcessing` provides the event processing hook. The reference constraint logic (cascade, prevent, setNull) should be implemented here

> **Note:** The generated event processing method for delete inter-invariants is a stub that needs to be completed with the specific constraint logic. The subscription and event matching infrastructure is fully generated.

### Multiple Subscriptions Per Inter-Invariant

An inter-invariant can subscribe to multiple events when the same constraint applies to different event types:

```nebula
Events {
    interInvariant USERS_EXIST {
        subscribe UserDeletedEvent from User {
            users.userAggregateId == event.aggregateId
        }
        subscribe UserUpdatedEvent from User {
            users.userAggregateId == event.aggregateId
        }
    }
}
```

This is useful when both deletion and updates of the referenced aggregate need to be tracked.

### Multiple Inter-Invariants

An aggregate can define multiple inter-invariants when it references multiple aggregates:

```nebula
Events {
    interInvariant MEMBER_EXISTS {
        subscribe MemberDeletedEvent from Member {
            member.memberAggregateId == event.aggregateId
        }
    }

    interInvariant BOOK_EXISTS {
        subscribe BookDeletedEvent from Book {
            book.bookAggregateId == event.aggregateId
        }
    }
}
```

Each inter-invariant independently monitors its referenced aggregate.

## Complete Example: Author and Post

### Author (event publisher)

```nebula
Aggregate Author {
    @GenerateCrud

    Root Entity Author {
        String name
        String bio

        invariants {
            check nameNotBlank { name.length() > 0 } error "Author name cannot be blank"
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
    @GenerateCrud

    Entity PostAuthor from Author {
        map name as authorName
    }

    Root Entity Post {
        String title
        String content
        PostAuthor author
        LocalDateTime publishedAt

        invariants {
            check titleNotBlank { title.length() > 0 } error "Post title cannot be blank"
            check authorNotNull { author != null } error "Post must have an author"
        }
    }

    References {
        author -> Author {
            onDelete: cascade
            message: "Author deleted, removing their posts"
        }
    }

    Events {
        subscribe AuthorUpdatedEvent

        interInvariant AUTHOR_EXISTS {
            subscribe AuthorDeletedEvent from Author {
                author.authorAggregateId == event.aggregateId
            }
        }
    }
}
```

This demonstrates:
- **Event publishing**:Author emits `AuthorUpdatedEvent` and `AuthorDeletedEvent`
- **Simple subscription**:Post subscribes to `AuthorUpdatedEvent` for data sync
- **Inter-invariant**:Post uses `AUTHOR_EXISTS` to enforce referential integrity on Author deletion
- **Cascade delete**:When an Author is deleted, their Posts are also deleted

### Generate and verify:

```bash
cd dsl/nebula
./bin/cli.js generate ../docs/examples/abstractions/05-eventdriven/ -o ../docs/examples/generated
```

Explore the generated event infrastructure:
- `AuthorUpdatedEvent.java` / `AuthorDeletedEvent.java`:event classes
- `PostEventProcessing.java`:event routing
- `PostEventHandling.java`:subscription handlers

---

**Previous:** [06-Cross-Aggregate-References](06-Cross-Aggregate-References.md) | **Next:** [08-Tutorial-Library-System](08-Tutorial-Library-System.md)
