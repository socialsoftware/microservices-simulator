---
name: wire-event
description: Phase 4 driver — wire one event-consumer inter-invariant: event class, subscription, handler, polling, EventProcessing chain, tracked field, update functionality, service method, test, tick plan.md. Arguments: "<ConsumerAggregate> <EventName>"
argument-hint: "<ConsumerAggregate> <EventName>  (e.g. CourseExecution CourseUpdatedEvent)"
---

# Wire Event: $ARGUMENTS

You are wiring one Phase 4 inter-invariant for the (consumer, event) pair in `$ARGUMENTS`.
Complete all steps in order. Tick the checkbox in `plan.md` after the test passes.

> **Layer 4 only.** The consumer caches state from the publisher via domain events (eventually
> consistent, ~1 s lag). No operation is blocked. If blocking is needed, use Layer 3 instead.

---

## Step 0 — Gather context

Parse `$ARGUMENTS`:
- **ConsumerAggregate** (e.g., `CourseExecution`) — caches external state
- **EventName** (e.g., `CourseUpdatedEvent`) — event that triggers the update

Read before writing any code:

1. `plan.md` — find the entry under Phase 4. Confirm the test class name.
2. The aggregate-grouping template (`*-aggregate-grouping.md`) — §2 for the consumer. Shows
   exactly which fields to cache from this event and which aggregate is the publisher.
3. The consumer aggregate class and service — understand existing fields and methods.
4. The `events/` top-level directory — check if the event class already exists.
5. `docs/concepts/events.md` — canonical wiring snippet (subscription anchor rules).
6. `applications/quizzes/microservices/<consumer>/events/` — reference for event wiring structure.

---

## Step 1 — Event class

If the event class does not already exist, create `events/<EventName>.java`:

```java
public class <EventName> extends Event {
    private Integer entityAggregateId;
    private Integer anchorAggregateId;

    public <EventName>(Integer entityAggregateId, Integer anchorAggregateId) {
        super(anchorAggregateId);  // anchorAggregateId = publisher aggregate ID
        this.entityAggregateId = entityAggregateId;
        this.anchorAggregateId = anchorAggregateId;
    }
    public <EventName>() {}
    // getters only
}
```

If the event class already exists (wired for another consumer earlier), reuse it — do not create
a duplicate.

---

## Step 2 — Publish event from publisher service

In `<Publisher>Service.java`, at the operation that triggers this event:

```java
unitOfWorkService.registerEvent(new <EventName>(entityId, anchorId), unitOfWork);
```

If already registered (another consumer was wired first), skip.

---

## Step 3 — Add tracked state to ConsumerAggregate

In `microservices/<consumer>/aggregate/<Consumer>.java`:

1. Add the tracked state field(s) with getter/setter, initialised to a safe default
   (`null` / `0` / `false` / `new HashSet<>()`)
2. Copy them in the copy constructor
3. Add a private helper and call it from `getEventSubscriptions()`:

```java
@Override
public Set<EventSubscription> getEventSubscriptions() {
    Set<EventSubscription> subs = new HashSet<>();
    if (getState() == AggregateState.ACTIVE) {
        interInvariant<Xxx>(subs);
    }
    return subs;
}

private void interInvariant<Xxx>(Set<EventSubscription> subs) {
    subs.add(new <Consumer>Subscribes<Xxx>(this.relevantRef));
}
```

For per-entity subscriptions (one per member of a collection):
```java
for (MemberType member : this.memberCollection) {
    subs.add(new <Consumer>Subscribes<Xxx>(member));
}
```

---

## Step 4 — Subscription class

Create `microservices/<consumer>/events/subscribe/<Consumer>Subscribes<Xxx>.java`:

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

`subscribedAggregateId` must match the `anchorAggregateId` used in the event constructor.

---

## Step 5 — Command class

Create `command/<consumer>/Update<Xxx>Command.java`:

```java
public class Update<Xxx>Command extends Command {
    private final Integer consumerAggregateId;
    // payload fields the service needs to update tracked state

    public Update<Xxx>Command(UnitOfWork unitOfWork, String serviceId,
            Integer consumerAggregateId, ...) {
        super(unitOfWork, serviceId);
        this.consumerAggregateId = consumerAggregateId;
    }
    // getters
}
```

---

## Step 6 — Event handler

Create `microservices/<consumer>/events/handling/handlers/<Xxx>EventHandler.java`.
Extend the existing base handler for this consumer if one exists:

```java
public class <Xxx>EventHandler extends <Consumer>EventHandler {
    public <Xxx>EventHandler(<Consumer>Repository repo,
            <Consumer>EventProcessing processing) {
        super(repo, processing);
    }

    @Override
    public void handleEvent(Integer aggregateId, Event event) {
        this.consumerEventProcessing.process<Xxx>Event(aggregateId, (<EventName>) event);
    }
}
```

---

## Step 7 — Polling method in EventHandling

In `microservices/<consumer>/events/handling/<Consumer>EventHandling.java`, add:

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

## Step 8 — EventProcessing → Functionalities wiring

In `<Consumer>EventProcessing.java`:
```java
public void process<Xxx>Event(Integer aggregateId, <EventName> event) {
    consumerFunctionalities.update<Xxx>(aggregateId, event);
}
```

In `<Consumer>Functionalities.java`, add `update<Xxx>` method that creates a UoW and dispatches
`Update<Xxx>Command`.

---

## Step 9 — Update functionality (Sagas + TCC stub)

**Sagas** (`coordination/sagas/Update<Xxx>FunctionalitySagas.java`): single-step workflow that
sends `Update<Xxx>Command` to the consumer service.

**TCC stub** (`coordination/causal/Update<Xxx>FunctionalityTCC.java`): empty `buildWorkflow()`.

---

## Step 10 — State update in ConsumerService

Add the method called by the command handler. Load, copy, update tracked field(s), register:

```java
public void update<Xxx>(Integer aggregateId, <EventName> event, UnitOfWork unitOfWork) {
    <Consumer> old = aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
    <Consumer> next = factory.copy(old);
    // update tracked state — semantics depend on field type:
    // counter: next.setCount(event.getCount());
    // boolean: next.setActive(event.isActive());
    // set: next.getIds().add/remove(event.getEntityId());
    unitOfWorkService.registerChanged(next, unitOfWork);
}
```

---

## Step 11 — Run the test

Add T3 test cases to `src/test/groovy/.../sagas/<consumer>/<Consumer>InterInvariantTest.groovy`
(or the existing file if prior inter-invariants for this consumer were already wired).
Follow the T3 template in `docs/concepts/testing.md`.

Cover:
- Event received → consumer aggregate reflects the change
- Event for an unrelated entity → state unchanged

Trigger event processing manually: call `<Consumer>EventHandling.handle<Xxx>Events()` directly
(`@Scheduled` does not run in `@DataJpaTest`).

Run the full test class — all cases must pass:
```bash
cd applications/<appName>
mvn clean -Ptest-sagas test -Dtest=<ConsumerAggregate>InterInvariantTest
```

---

## Step 12 — Tick plan.md

- [x] `/wire-event <ConsumerAggregate> <EventName>` → test `<ConsumerAggregate>InterInvariantTest`

---

## Done

Report:
- Event class: created or reused `<EventName>.java`
- Consumer aggregate: tracked field(s) added and wired
- Test: green — `<ConsumerAggregate>InterInvariantTest`
