# Guide: Implement an Event

Follow these steps to add a new domain event for cross-aggregate eventual consistency.

Reference: [`docs/concepts/events.md`](../concepts/events.md), [`docs/concepts/invariants.md`](../concepts/invariants.md)

---

## Step 1 — Create the event class

File: `<app>/events/<Name>Event.java`

```java
public class XxxEvent extends Event {
    private Integer entityAggregateId;
    private Integer anchorAggregateId;  // the subscription anchor

    public XxxEvent(Integer entityAggregateId, Integer anchorAggregateId) {
        super(anchorAggregateId);  // IMPORTANT: publisherAggregateId = anchorAggregateId
        this.entityAggregateId = entityAggregateId;
        this.anchorAggregateId = anchorAggregateId;
    }

    // getters only (events are immutable)
}
```

**Rule:** `super(...)` receives the **same ID** that subscribers will use as `subscribedAggregateId`. Mismatch breaks TCC.

If a suitable event already exists, reuse it.

---

## Step 2 — Publish the event from the publisher service

In the publisher's `XxxService.java`, after modifying the aggregate:

```java
unitOfWorkService.registerEvent(new XxxEvent(entityId, anchorId), unitOfWork);
```

Add one registration per trigger action (create, update, delete, etc.).

---

## Step 3 — Create the subscription class

File: `microservices/<consumer>/events/subscribe/<Consumer>Subscribes<Xxx>.java`

```java
public class ConsumerSubscribesXxx extends EventSubscription {
    public ConsumerSubscribesXxx(SomeRef ref) {
        super(ref.getAnchorAggregateId(), ref.getAnchorVersion(), XxxEvent.class);
    }

    @Override
    public boolean filter(Event event) {
        XxxEvent e = (XxxEvent) event;
        return e.getAnchorAggregateId().equals(this.subscribedAggregateId);
    }
}
```

`subscribedAggregateId` is set by `super(...)` and must match `publisherAggregateId` in the event.

---

## Step 4 — Wire subscription into the consumer aggregate

In `<Consumer>.java`, add a private helper and call it from `getEventSubscriptions()`:

```java
@Override
public Set<EventSubscription> getEventSubscriptions() {
    Set<EventSubscription> subs = new HashSet<>();
    if (getState() == ACTIVE) {
        interInvariantXxx(subs);
    }
    return subs;
}

private void interInvariantXxx(Set<EventSubscription> subs) {
    subs.add(new ConsumerSubscribesXxx(this.someRef));
}
```

---

## Step 5 — Create the event handler

File: `microservices/<consumer>/events/handling/handlers/<Xxx>EventHandler.java`

```java
public class XxxEventHandler extends EventHandler {
    private final XxxCustomRepository repository;
    private final ConsumerEventProcessing eventProcessing;

    public XxxEventHandler(XxxCustomRepository repository, ConsumerEventProcessing eventProcessing) {
        this.repository = repository;
        this.eventProcessing = eventProcessing;
    }

    @Override
    public void handleEvent(Event event) {
        XxxEvent e = (XxxEvent) event;
        eventProcessing.processXxxEvent(e);
    }
}
```

---

## Step 6 — Wire polling in the EventHandling bean

File: `microservices/<consumer>/events/handling/<Consumer>EventHandling.java`

```java
@Scheduled(fixedDelay = 1000)
public void handleXxxEvents() {
    eventApplicationService.handleSubscribedEvent(XxxEvent.class,
            new XxxEventHandler(consumerRepository, eventProcessing));
}
```

---

## Step 7 — Wire EventProcessing → Functionalities

In `<Consumer>EventProcessing.java`:
```java
public void processXxxEvent(XxxEvent event) {
    // find affected consumer aggregate(s) by anchorAggregateId
    // call consumerFunctionalities.updateXxx(...)
}
```

In `<Consumer>Functionalities.java`:
```java
public void updateXxx(Integer consumerAggregateId, ...) {
    UnitOfWork uow = unitOfWorkService.createUnitOfWork();
    XxxUpdateFunctionalitySagas f = new XxxUpdateFunctionalitySagas(..., uow, commandGateway);
    f.executeWorkflow(uow);
    unitOfWorkService.commit(uow);
}
```

---

## Step 8 — Write tests

In the test class:
- Manually call `consumerEventHandling.handleXxxEvents()` (the `@Scheduled` annotation does not run in `@DataJpaTest`)
- Assert the consumer aggregate's cached state updated correctly

---

## Checklist

- [ ] Event class with correct `publisherAggregateId` (anchor, not entity)
- [ ] Event registered in publisher service
- [ ] Subscription class matches `subscribedAggregateId` to `publisherAggregateId`
- [ ] Subscription wired into `getEventSubscriptions()` under correct `ACTIVE` guard
- [ ] Handler created and delegates to EventProcessing
- [ ] Polling method added to EventHandling bean
- [ ] EventProcessing → Functionalities chain wired
- [ ] Tests call handler method manually
