# Inter-Invariants

## Upstream / Downstream Dependencies

If aggregate **A** caches state from aggregate **B**, then **B is upstream of A**. The upstream aggregate is the publisher; the downstream aggregate is the consumer.

**Rule:** event subscriptions always flow downstream. A downstream aggregate subscribes to its upstream aggregate's events and caches the relevant state locally. Downstream aggregates never call upstream aggregates at read time — all cross-aggregate reads go through the local cache.

This determines where `getEventSubscriptions()` lives: always in the **downstream (consumer) aggregate**.

### Dependency Graph (Quizzes)

```
Course ──────────────────────────► Execution
Course ──────────────────────────► Question
Topic ───────────────────────────► Question
Question ────────────────────────► Quiz
User ────────────────────────────► Execution
User ────────────────────────────► Tournament
Execution ───────────────────────► Tournament
Quiz ────────────────────────────► QuizAnswer
```

Each arrow means: the downstream aggregate (right) holds a subscription to the upstream aggregate (left) and caches relevant state from it.

---

## What They Are

An inter-invariant is a **consistency rule that spans multiple aggregates**. Because aggregates are owned by separate services, they cannot read each other's state synchronously without coupling. Instead, the consumer aggregate **caches a local copy** of the relevant publisher state, kept eventually consistent via domain events.

---

## Pattern Variants

Inter-invariants come in two distinct patterns. Choose based on whether enforcement requires blocking an operation or simply updating state.

### Reactive-Update Pattern

The consumer updates its cached state in response to events. **No guard method or error throw is needed** — the enforcement IS the state change.

Use when: a domain event should cause the consumer to change its own state (e.g., a topic is deleted → remove it from the tournament's cached topic set).

```
Publisher aggregate                Consumer aggregate
      │                                  │
      │  event: XxxHappened              │
      │─────────────────────────────────►│
      │                                  │  poll (1s interval)
      │                                  │  update cached state  ← enforcement IS here
      │                                  │
```

### Guard-Enforcement Pattern

The consumer caches state AND a guard method checks it and throws an exception **before** a mutating operation is allowed.

Use when: an operation must be blocked based on external state (e.g., cannot delete an execution that still has questions, cannot start a quiz that was already answered).

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

---

## Components

### Common to Both Patterns

| Component | Location | Purpose |
|-----------|---------|---------|
| Event class | `quizzes/events/XxxEvent.java` | Carries the data the consumer needs |
| Event subscription | `<consumer>/events/subscribe/ConsumerSubscribesXxx.java` | Declares what to listen for and how to filter |
| Event handler | `<consumer>/events/handling/handlers/XxxEventHandler.java` | Receives and dispatches the event |
| Polling method | `<consumer>/events/handling/<Consumer>EventHandling.java` | `@Scheduled` poll loop |
| Cached state field | `<consumer>/aggregate/<Consumer>.java` | Local copy of publisher data |
| Update functionality | `<consumer>/coordination/sagas/XxxFunctionalitySagas.java` + TCC | Workflow that applies the update to the aggregate |

### Guard-Enforcement Pattern Only

| Component | Location | Purpose |
|-----------|---------|---------|
| Guard method | `<consumer>/service/<Consumer>Service.java` | Loads its own aggregate, checks cached state, throws if guard fails |
| Guard command | `command/<consumer>/Assert<Condition>Command.java` | Routed to the consumer service for the guard check |
| Guard invocation step | `<operation>/coordination/sagas/<Op>FunctionalitySagas.java` + TCC | Dedicated step before the mutating command |
| Error message | `QuizzesErrorMessage.java` | Enum entry for the thrown exception |

---

## Tracked State Patterns

Choose the simplest representation for the invariant:

| State type | Tracked field | Update logic |
|-----------|-------------|-------------|
| Running total | `int xxxCount` | `+1` on create event, `-1` on delete event |
| Presence check | `boolean hasXxx` | `true` on create, `false` on delete |
| Membership | `Set<Integer> xxxIds` | `add(id)` on create, `remove(id)` on delete |
| Single reference | `Integer xxxId` | set on link event, null on unlink event |

> **Reactive-update**: the update logic IS the enforcement — no guard check is needed.
> **Guard-enforcement**: the cached field is also checked in the guard method at operation time.

---

## Subscription Granularity

Subscriptions can be scoped at two levels:

**Per-aggregate** (most common): one subscription per consumer aggregate instance, anchored to a publisher aggregate ID.
```java
// One subscription for the whole consumer aggregate
eventSubscriptions.add(new ConsumerSubscribesXxx(this.publisherRef));
```

**Per-entity**: one subscription per item in a set — used when the consumer tracks a collection and each element can independently publish events. The `subscribedAggregateId` is the entity's own aggregate ID.
```java
// One subscription per entity in the set
for (ConsumerEntity entity : this.entitySet) {
    eventSubscriptions.add(new ConsumerSubscribesXxx(entity));
}
```

Both patterns (reactive-update and guard-enforcement) can use either subscription granularity.

---

## publisherAggregateId Convention

The `publisherAggregateId` passed to `super(...)` in the event constructor **must be the aggregate ID used as the subscription anchor**. This is the same value used in the subscription's `subscribedAggregateId`.

For TCC, this is critical: `EventSubscriberService` uses `publisherAggregateId` to determine which aggregate versions a `CausalExecution` must have processed before it can be causally read alongside other aggregates.

**Wrong:** using the entity ID (e.g., `questionAggregateId`) when the consumer subscribes by `courseAggregateId`.
**Right:** using the same anchor ID in both the event and the subscription.

---

## Guard Placement Rule *(Guard-Enforcement Pattern Only)*

The guard method belongs in the **consumer service** — the service that owns the aggregate holding the cached state. `aggregateLoadAndRegisterRead` must only be called with the service's own aggregate type; the consumer service satisfies this because it owns the consumer aggregate.

The guard is **not** placed inline inside the operation service (e.g., `QuizAnswerService`). Instead, the operation's functionality (Sagas or TCC) calls the guard via a dedicated command/step before the mutating call. This keeps each service's reads restricted to its own aggregate type.

---

## Eventual Consistency Trade-off

Cached state is updated asynchronously (default: 1 second poll interval). A guard check may use a stale value. This is a known trade-off — document it in the summary markdown when implementing a new invariant.

---

## Worked Examples

| Pattern | Example |
|---------|---------|
| Guard-enforcement | [`docs/examples/cannot-delete-last-execution-with-content.md`](../examples/cannot-delete-last-execution-with-content.md) — complete end-to-end walkthrough |
| Reactive-update + multiple invariants | [`docs/examples/tournament-inter-invariants.md`](../examples/tournament-inter-invariants.md) — tournament invariants including per-entity subscriptions |

---

## Implementing a New Inter-Invariant

Use the `/inter-invariant` skill:
```
/inter-invariant Execution courseQuestionCount > 0 when last execution
/inter-invariant Tournament remove topic from set when topic deleted
```

The skill walks through all steps, adapting to reactive-update or guard-enforcement as appropriate.
