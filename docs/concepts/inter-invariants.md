# Inter-Invariants

## What They Are

An inter-invariant is a **consistency rule that spans multiple aggregates**. Because aggregates are owned by separate services, they cannot read each other's state synchronously without coupling. Instead, the consumer aggregate **caches a local copy** of the relevant publisher state, kept eventually consistent via domain events. The guard is enforced locally at operation time using this cached state.

## Pattern

```
Publisher aggregate                Consumer aggregate
      │                                  │
      │  event: XxxHappened              │
      │─────────────────────────────────►│
      │                                  │  poll (1s interval)
      │                                  │  update cached state
      │                                  │
      │                  guarded operation call
      │                                  │
      │                                  │  check cached state
      │                                  │  throw if guard fails
```

## Components

| Component | Location | Purpose |
|-----------|---------|---------|
| Event class | `quizzes/events/XxxEvent.java` | Carries the data the consumer needs |
| Event subscription | `<consumer>/events/subscribe/ConsumerSubscribesXxx.java` | Declares what to listen for and how to filter |
| Event handler | `<consumer>/events/handling/handlers/XxxEventHandler.java` | Receives and dispatches the event |
| Polling method | `<consumer>/events/handling/<Consumer>EventHandling.java` | `@Scheduled` poll loop |
| Cached state field | `<consumer>/aggregate/<Consumer>.java` | Local copy of publisher data |
| Update functionality | `<consumer>/coordination/sagas/XxxFunctionalitySagas.java` + TCC | Workflow that applies the update to the aggregate |
| Guard | `<consumer>/service/<Consumer>Service.java` | Check before the mutating call |
| Error message | `QuizzesErrorMessage.java` | Enum entry for the thrown exception |

## Tracked State Patterns

Choose the simplest representation for the guard:

| Guard type | Tracked field | Update logic |
|-----------|-------------|-------------|
| Running total | `int xxxCount` | `+1` on create event, `-1` on delete event |
| Presence check | `boolean hasXxx` | `true` on create, `false` on delete |
| Membership | `Set<Integer> xxxIds` | `add(id)` on create, `remove(id)` on delete |
| Single reference | `Integer xxxId` | set on link event, null on unlink event |

## publisherAggregateId Convention

The `publisherAggregateId` passed to `super(...)` in the event constructor **must be the aggregate ID used as the subscription anchor**. This is the same value used in the subscription's `subscribedAggregateId`.

For TCC, this is critical: `EventSubscriberService` uses `publisherAggregateId` to determine which aggregate versions a `CausalExecution` must have processed before it can be causally read alongside other aggregates.

**Wrong:** using the entity ID (e.g., `questionAggregateId`) when the consumer subscribes by `courseAggregateId`.
**Right:** using the same anchor ID in both the event and the subscription.

## Eventual Consistency Trade-off

Cached state is updated asynchronously (default: 1 second poll interval). A guard check may use a stale count. This is a known trade-off — document it in the summary markdown when implementing a new invariant.

## Worked Example

See [`docs/examples/cannot-delete-last-execution-with-content.md`](../examples/cannot-delete-last-execution-with-content.md) for a complete end-to-end walkthrough of every layer.

## Implementing a New Inter-Invariant

Use the `/inter-invariant` skill:
```
/inter-invariant Execution courseQuestionCount > 0 when last execution
```

The skill walks through all 16 implementation steps, from event design through tests and summary markdown.
