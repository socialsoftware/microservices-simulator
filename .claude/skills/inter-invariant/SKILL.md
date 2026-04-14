---
name: inter-invariant
description: Implement a Layer 4 inter-invariant in the microservices-simulator quizzes application. Use this when a consumer aggregate needs to cache state from a publisher aggregate via domain events. Arguments: "<ConsumerAggregate> <condition>", e.g. "Execution userActive=true for enrolled users"
argument-hint: "<ConsumerAggregate> <condition-description>"
---

# Implement Inter-Invariant: $ARGUMENTS

You are implementing a **Layer 4 inter-invariant**. The consumer aggregate caches state from a publisher aggregate via domain events (eventually consistent).

> **Pattern:** publisher publishes event → consumer updates cached state. No operation is blocked.

---

## Step 0 — Verify this is Layer 4

Before implementing, confirm this rule belongs in Layer 4 and not a lower layer. Use `docs/concepts/decision-guide.md`:

- If the check can be done synchronously reading only the consumer's own aggregate → use Layer 2 (`/service-guard`) instead.
- If the check requires reading another aggregate synchronously in the same UoW → use Layer 3 (`setForbiddenStates` in `/new-functionality`) instead.
- If eventual consistency (~1 s lag) is acceptable and the rule only syncs state → **Layer 4 inter-invariant (this skill)**.
- If eventual consistency is acceptable but an operation must also be blocked → re-classify as **Layer 3** (`setForbiddenStates` in `/new-functionality`); Layer 4 never blocks.

Ask the user to confirm the layer before writing any code.

---

## Step 1 — Parse arguments and design tracked state

From `$ARGUMENTS` identify:
- **ConsumerAggregate**: the aggregate that must observe external state
- **PublisherAggregate**: the aggregate that owns the changing data
- **Trigger events**: what domain actions the consumer must react to
- **Tracked state**: what data the consumer needs to cache locally

Decide the simplest representation:
- Counter (`int questionCount`) for totals
- Boolean flag (`boolean hasActiveEnrollments`) for binary conditions
- Set of IDs (`Set<Integer> enrolledUserIds`) for membership
- Single reference (`Integer linkedAggregateId`) for existence

Document your decision before proceeding.

---

## Step 2 — Read existing code

Before writing anything, read:
1. The **ConsumerAggregate** class under `microservices/<consumer>/aggregate/`
2. The **ConsumerAggregate** service under `microservices/<consumer>/service/`
3. The **PublisherAggregate** service under `microservices/<publisher>/service/`
4. The `microservices/<consumer>/events/` directory — understand existing event wiring
5. The `events/` top-level directory — see existing shared event classes
6. The `microservices/<consumer>/coordination/` directory — existing functionality examples

Reference implementation: `docs/examples/tournament-inter-invariants.md` — use for structural reference only, not domain logic.

---

## Step 3 — Implement: Event class

Create or reuse an event class in `events/<EventName>.java`. Use the canonical pattern from `docs/concepts/events.md` §Canonical Wiring Snippet:

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

If a matching event already exists, reuse it.

---

## Step 4 — Publish event from publisher service

In `<Publisher>Service.java`, at the trigger point:
```java
unitOfWorkService.registerEvent(new <EventName>(entityId, anchorId), unitOfWork);
```

---

## Step 5 — Add tracked state to ConsumerAggregate

In `microservices/<consumer>/aggregate/<Consumer>.java`:
1. Add tracked state field(s) with getter/setter, initialised to safe default (`0`, `false`, `new HashSet<>()`)
2. Copy them in the copy-constructor
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

For sets (per-entity subscriptions), add one subscription per entity:
```java
for (ConsumerEntity entity : this.entitySet) {
    subs.add(new <Consumer>Subscribes<Xxx>(entity));
}
```

---

## Step 6 — Implement: Subscription class

Create `microservices/<consumer>/events/subscribe/<Consumer>Subscribes<Xxx>.java`. Use the canonical pattern from `docs/concepts/events.md` §Canonical Wiring Snippet:

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

`subscribedAggregateId` must match `publisherAggregateId` used in the event.

---

## Step 7 — Implement: Command

Create `command/<consumer>/<Xxx>Command.java`:
```java
public class <Xxx>Command extends Command {
    private final Integer consumerAggregateId;
    // payload fields the service needs to update tracked state
}
```

---

## Step 8 — Implement: Event handler

Create `microservices/<consumer>/events/handling/handlers/<Xxx>EventHandler.java`. Extend the existing base handler if one exists:

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

## Step 9 — Wire polling in EventHandling bean

In `microservices/<consumer>/events/handling/<Consumer>EventHandling.java`, add one `@Scheduled` method with an invariant-name comment:

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

## Step 10 — Wire EventProcessing → Functionalities

In `<Consumer>EventProcessing.java`, add a method that identifies the affected consumer aggregate(s) and calls `<Consumer>Functionalities`:

```java
public void process<Xxx>Event(Integer aggregateId, <EventName> event) {
    consumerFunctionalities.update<Xxx>(aggregateId, event);
}
```

In `<Consumer>Functionalities.java`, add a method that creates a UoW and dispatches the command.

---

## Step 11 — Implement: Update functionality (Sagas) and TCC stub

**Sagas** (`coordination/sagas/<Xxx>FunctionalitySagas.java`): single-step workflow that sends the command to the consumer service and calls the state-update method.

**TCC stub** (`coordination/causal/<Xxx>FunctionalityTCC.java`): empty `buildWorkflow()` body — TCC not implemented.

---

## Step 12 — Implement: State update in ConsumerService

Add the method called by the command handler:
1. Load current aggregate version
2. Copy it
3. Update tracked state field(s) according to the event semantics
4. Register the changed aggregate

The update logic depends on the tracked state type:
- Counter: increment/decrement
- Boolean: flip
- Set: add/remove ID

---

## Step 13 — Write tests

Add test cases to the relevant `*Test.groovy` under `src/test/groovy/.../sagas/`:

| Scenario | Expected outcome |
|----------|-----------------|
| Event received, state updated | Consumer aggregate reflects the change |
| Event received for non-member entity | State unchanged |

Trigger event processing manually: call `<Consumer>EventHandling.handle<Xxx>Events()` directly. The `@Scheduled` annotation does not run in `@DataJpaTest`.

---

## Checklist

- [ ] Event class created with correct `publisherAggregateId` (anchor, not child entity)
- [ ] Event registered in publisher service
- [ ] Tracked state field(s) added, initialised, and copied in copy-constructor
- [ ] Event subscriptions created and wired into `getEventSubscriptions()`
- [ ] Event subscriptions wrapped in `if (getState() == ACTIVE)`
- [ ] Subscription uses canonical `filter(Event event)` method
- [ ] Command class created with appropriate payload
- [ ] Event handler created (extends base handler if one exists)
- [ ] Polling method added to EventHandling bean (with invariant-name comment)
- [ ] EventProcessing → Functionalities wiring added
- [ ] Sagas update functionality created (single step)
- [ ] TCC functionality stub created (empty `buildWorkflow()`)
- [ ] State update method added to ConsumerService
- [ ] Tests written and passing: `mvn clean -Ptest-sagas test`
