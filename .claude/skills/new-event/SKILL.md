---
name: new-event
description: Add a new domain event to the microservices-simulator quizzes application, including event class, subscription, handler, and polling wiring. Arguments: "<EventName> <PublisherAggregate> <ConsumerAggregate>"
argument-hint: "<EventName> <PublisherAggregate> <ConsumerAggregate>"
---

# Add New Domain Event: $ARGUMENTS

You are adding a new domain event to the `applications/quizzes` module to propagate state changes from one aggregate to another.

---

## Step 0 — Parse arguments

From `$ARGUMENTS` identify:
- **EventName**: PascalCase name (e.g., `EnrollStudentEvent`)
- **PublisherAggregate**: the aggregate whose service registers this event
- **ConsumerAggregate**: the aggregate that reacts to this event
- **Trigger action**: what operation causes the publisher to emit this event
- **Subscription anchor**: which aggregate ID is used as `publisherAggregateId` (and `subscribedAggregateId`) — may differ from the entity being created/deleted
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

Also read `docs/concepts/events.md` for background on event wiring mechanics.

---

## Step 2 — Check for reuse

Check if a suitable event class already exists in `quizzes/events/`. If so, reuse it and skip Step 3.

---

## Step 3 — Create the event class

File: `src/main/java/.../quizzes/events/<EventName>.java`

```java
public class <EventName> extends Event {
    private Integer entityAggregateId;
    private Integer anchorAggregateId;

    public <EventName>(Integer entityAggregateId, Integer anchorAggregateId) {
        super(anchorAggregateId);  // publisherAggregateId = subscription anchor
        this.entityAggregateId = entityAggregateId;
        this.anchorAggregateId = anchorAggregateId;
    }
    // getters only
}
```

**Critical:** `super(...)` must receive the same ID that consumers will use as `subscribedAggregateId`. Check `docs/concepts/consistency-enforcement.md` for the TCC implication.

---

## Step 4 — Publish the event from the publisher service

In `microservices/<publisher>/service/<Publisher>Service.java`, at the point where the trigger action occurs:

```java
unitOfWorkService.registerEvent(new <EventName>(entityId, anchorId), unitOfWork);
```

---

## Step 5 — Create the subscription class

File: `microservices/<consumer>/events/subscribe/<Consumer>Subscribes<Xxx>.java`

```java
public class <Consumer>Subscribes<Xxx> extends EventSubscription {
    public <Consumer>Subscribes<Xxx>(SomeRef ref) {
        super(ref.getAnchorAggregateId(), ref.getAnchorVersion(), <EventName>.class);
    }

    @Override
    public boolean filter(Event event) {
        <EventName> e = (<EventName>) event;
        return e.getAnchorAggregateId().equals(this.subscribedAggregateId);
    }
}
```

---

## Step 6 — Wire subscription into consumer aggregate

In the consumer's base class `getEventSubscriptions()`, add the new subscription inside the appropriate helper method (or create a new private helper for a new inter-invariant):

```java
private void interInvariantXxx(Set<EventSubscription> subs) {
    subs.add(new <Consumer>Subscribes<Xxx>(this.relevantRef));
}
```

---

## Step 7 — Create the event handler

File: `microservices/<consumer>/events/handling/handlers/<Xxx>EventHandler.java`

Receives the event and delegates to `<Consumer>EventProcessing`:
```java
public class <Xxx>EventHandler extends EventHandler {
    @Override
    public void handleEvent(Event event) {
        <EventName> e = (<EventName>) event;
        eventProcessing.process<Xxx>Event(e);
    }
}
```

---

## Step 8 — Wire polling

In `microservices/<consumer>/events/handling/<Consumer>EventHandling.java`:

```java
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
public void process<Xxx>Event(<EventName> event) {
    // locate affected consumer aggregate(s) by anchor ID
    // call consumerFunctionalities.update<Xxx>(...)
}
```

In `<Consumer>Functionalities.java`, create a UoW and dispatch a new functionality.

---

## Step 10 — Write tests

- Create a test scenario that triggers the publisher action
- Call `<Consumer>EventHandling.handle<Xxx>Events()` manually
- Assert the consumer aggregate's cached state updated correctly

---

## Checklist

- [ ] Event class with correct `publisherAggregateId` (anchor, not entity)
- [ ] Event registered in publisher service
- [ ] Subscription class with matching `subscribedAggregateId`
- [ ] Subscription wired into consumer's `getEventSubscriptions()`
- [ ] Event handler created
- [ ] Polling method added to `<Consumer>EventHandling`
- [ ] EventProcessing → Functionalities chain wired
- [ ] Tests call handler method manually and assert state update
