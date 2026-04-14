---
name: new-event
description: Add a new domain event to the microservices-simulator quizzes application, including event class, subscription, handler, and polling wiring. Arguments: "<EventName> <PublisherAggregate> <ConsumerAggregate>"
argument-hint: "<EventName> <PublisherAggregate> <ConsumerAggregate>"
---

# Add New Domain Event: $ARGUMENTS

You are adding a new domain event to propagate state changes from one aggregate to another.

> This skill wires the event infrastructure only. To also implement the consumer-side state update (tracked field + update functionality), use `/inter-invariant` instead. Use this skill when you need the event wiring as a standalone step, or when `/inter-invariant` calls it explicitly.

---

## Step 0 — Parse arguments

From `$ARGUMENTS` identify:
- **EventName**: PascalCase name (e.g., `EnrollStudentEvent`)
- **PublisherAggregate**: the aggregate whose service registers this event
- **ConsumerAggregate**: the aggregate that reacts to this event
- **Trigger action**: what operation causes the publisher to emit this event
- **Subscription anchor**: which aggregate ID is used as `publisherAggregateId` (the owning/parent aggregate ID — may differ from the entity being created/deleted)
- **Effect on consumer**: what field or state the consumer updates when it receives the event

Clarify before writing any code.

---

## Step 1 — Read existing event wiring

Before writing anything, read:
1. `quizzes/events/` — all existing event classes
2. `microservices/<consumer>/events/subscribe/` — existing subscription classes
3. `microservices/<consumer>/events/handling/<Consumer>EventHandling.java` — existing polling methods
4. `microservices/<consumer>/events/handling/handlers/` — existing handlers
5. `microservices/<publisher>/service/<Publisher>Service.java` — where events are registered

Also read `docs/concepts/events.md` for the canonical wiring pattern and background mechanics.

---

## Step 2 — Check for reuse

Check if a suitable event class already exists in `quizzes/events/`. If so, reuse it and skip Step 3.

---

## Step 3 — Create the event class

File: `src/main/java/.../quizzes/events/<EventName>.java`

Use the canonical pattern from `docs/concepts/events.md` §Canonical Wiring Snippet:

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

**Critical:** `super(...)` must receive the **subscription anchor** — the owning/parent aggregate's ID. Using the child entity ID breaks subscription matching.

---

## Step 4 — Publish the event from the publisher service

In `microservices/<publisher>/service/<Publisher>Service.java`, at the trigger point:

```java
unitOfWorkService.registerEvent(new <EventName>(entityId, anchorId), unitOfWork);
```

---

## Step 5 — Create the subscription class

File: `microservices/<consumer>/events/subscribe/<Consumer>Subscribes<Xxx>.java`

Use the canonical pattern from `docs/concepts/events.md` §Canonical Wiring Snippet:

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

`subscribedAggregateId` (from `super(...)`) must match `publisherAggregateId` used in the event constructor.

---

## Step 6 — Wire subscription into consumer aggregate

In the consumer's base class `getEventSubscriptions()`, add the new subscription inside the appropriate helper method (or create a new private helper):

```java
private void interInvariant<Xxx>(Set<EventSubscription> subs) {
    subs.add(new <Consumer>Subscribes<Xxx>(this.relevantRef));
}
```

Call this helper from `getEventSubscriptions()` inside the `if (getState() == ACTIVE)` block.

---

## Step 7 — Create the event handler

File: `microservices/<consumer>/events/handling/handlers/<Xxx>EventHandler.java`

Extend the existing base handler if one exists. Use the canonical pattern from `docs/concepts/events.md` §Canonical Wiring Snippet:

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

---

## Step 8 — Wire polling

In `microservices/<consumer>/events/handling/<Consumer>EventHandling.java`, use the canonical pattern from `docs/concepts/events.md` §Canonical Wiring Snippet:

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

---

## Step 9 — Wire EventProcessing → Functionalities

In `<Consumer>EventProcessing.java`:
```java
public void process<Xxx>Event(Integer aggregateId, <EventName> event) {
    // locate affected consumer aggregate(s) by anchor ID
    // call consumerFunctionalities.update<Xxx>(aggregateId, event)
}
```

In `<Consumer>Functionalities.java`, create a UoW and dispatch the update functionality (Sagas only — TCC stub if needed).

---

## Step 10 — Write tests

- Trigger the publisher action
- Call `<Consumer>EventHandling.handle<Xxx>Events()` manually (`@Scheduled` does not run in `@DataJpaTest`)
- Assert the consumer aggregate's cached state updated correctly

---

## Checklist

- [ ] Event class with correct `publisherAggregateId` (anchor, not child entity)
- [ ] Event registered in publisher service
- [ ] Subscription class: uses `filter(Event event)`, `subscribedAggregateId` matches event's `publisherAggregateId`
- [ ] Subscription wired into consumer's `getEventSubscriptions()` inside `if (getState() == ACTIVE)`
- [ ] Event handler created (extends base handler if one exists)
- [ ] Polling method added to `<Consumer>EventHandling` (with invariant-name comment)
- [ ] EventProcessing → Functionalities chain wired
- [ ] Tests call handler method manually and assert state update
