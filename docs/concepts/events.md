# Events

## What They Are

Domain events are the mechanism for **eventual consistency** across aggregates. When an aggregate is committed, it may register one or more events. Downstream aggregates poll for those events and react by updating their local state.

## Event Classes

Located in `src/main/java/.../<appName>/events/` (e.g., `applications/{app-name}/src/main/java/.../{app}/events/`).

Each event extends `Event` from `ms.aggregate` (`pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event`) — it is a JPA `@Entity`, so the subclass must be annotated `@Entity` too:

```java
@Entity
public class CreateShipmentEvent extends Event {
    private Integer shipmentAggregateId;
    private Integer warehouseAggregateId;

    public CreateShipmentEvent(Integer shipmentAggregateId, Integer warehouseAggregateId) {
        super(warehouseAggregateId);  // publisherAggregateId
        this.shipmentAggregateId = shipmentAggregateId;
        this.warehouseAggregateId = warehouseAggregateId;
    }
}
```

**Critical:** `publisherAggregateId` (the argument to `super(...)`) must be the aggregate ID used as the **subscription anchor**. Using the wrong ID breaks event filtering.

## Publishing Events

In the publisher service's method, after modifying the aggregate:

```java
unitOfWorkService.registerEvent(new XxxEvent(entityId, anchorAggregateId), unitOfWork);
```

The event is persisted when the UoW commits.

**Choosing the anchor id.** The second argument is the **publisher aggregate id** — the id
subscribers match on. For an event about a child entity, this is the *parent aggregate's* id, not
the child's. Getting it wrong is silent: the event publishes, no subscription matches, and the
consumer never updates.

```java
// WRONG — anchored on the child entity's own id; no subscriber matches on it
unitOfWorkService.registerEvent(
        new Update{Entity}Event({entity}.getId(), {entity}.getId()), unitOfWork);

// RIGHT — anchored on the owning aggregate; the payload still carries the child id
unitOfWorkService.registerEvent(
        new Update{Entity}Event({entity}.getId(), {aggregate}.getAggregateId()), unitOfWork);
```

The subscriber side of this contract is `subscribedAggregateId` — see § EventSubscription, where the
same id must appear.

## EventSubscription

Each subscriber aggregate declares which events it watches via `getEventSubscriptions()`. Located in `microservices/<aggregate>/notification/subscribe/`.

Subscriptions live in the **downstream (consumer)** aggregate only; a publisher never subscribes to its own events and never references downstream aggregate types (R5, [`../architecture.md`](../architecture.md)). Subscribing in the wrong direction creates a cycle in the event pipeline.

```java
public class ShipmentSubscribesUpdateWarehouse extends EventSubscription {
    public ShipmentSubscribesUpdateWarehouse(ShipmentWarehouse warehouse) {
        super(warehouse.getWarehouseAggregateId(), warehouse.getWarehouseVersion(), UpdateWarehouseEvent.class.getSimpleName());
    }

    public ShipmentSubscribesUpdateWarehouse() {}
}
```

Matching is performed by the infrastructure: `EventService.getSubscribedEvents` runs a DB query on `subscribedAggregateId` and `subscribedVersion`, then re-applies the same predicate through `subscribesEvent()`. **Do not override `subscribesEvent()`** for additional filtering (e.g. checking a discriminating field for shared-anchor events). It sees only the three fields the subscription was constructed with, not the consumer's current state, and it is re-evaluated against a subscription rebuilt on every poll — so any discrimination expressed there is both state-blind and duplicated. Put it in the service-layer ByEvent method instead, where the consumer aggregate is loaded.

`subscribedAggregateId` must match `publisherAggregateId` in the event.

**`subscribedVersion`:** pass the anchor entity's current version, so that the eligible set is narrowed to events the consumer has not already folded in. The anchor entity always has one: `.claude/skills/classify-and-plan/SKILL.md` § Step 3.d gives a version field to every snapshot — an owned-entity class where the snapshot subscribes to events, an id-plus-version field pair on the aggregate itself where it does not. A missing version field is therefore a planning defect to fix at its source, not something to paper over with a `0L` default.

### A snapshot-seeded version does not exclude the events already published

`subscribedVersion` does **not** mean "events emitted after this snapshot was taken". Aggregate versions and event versions are two draws from the *same* global counter: `registerChanged` takes one tick and stamps it on the aggregate, then `registerEvent` takes another and stamps it on the event. A publisher that commits and then publishes therefore emits an event whose `publisherAggregateVersion` is strictly **greater** than the aggregate version that same operation committed — and that aggregate version is what a fetched `{Publisher}Dto` carries into the consumer's snapshot.

Since `EventRepository.findUnprocessedEvents` keeps events with `publisherAggregateVersion > subscribedVersion`, a consumer seeded from a DTO stays eligible for every event the publisher emitted up to and including the commit that produced that DTO. **This backlog is expected behaviour, not a defect to work around.** Consequences to design for:

- **The consumer's first poll drains the backlog**, not just the event under consideration. Handlers must therefore be idempotent under re-application of a payload already folded in.
- **A stale payload can overwrite a fresher cached value, inside a single poll.** `EventRepository.findUnprocessedEvents` orders the batch `timestamp DESC` and `EventApplicationService` hands the whole batch to the handler in that order, so the newest event is applied first and an older one reaches the consumer immediately afterwards. Advancing the cached publisher version (below) does not prevent this on its own — the ByEvent mutation must also **reject** an event that does not advance that version (§ "Reject an event that does not advance the cached version").
- **T3 subscription tests must drain the backlog in `given:`**, before capturing the `versionBefore` the "reflects event" assertion compares against. Call the polling method once at the end of setup — where a fixture helper creates the publisher in several commits, give that drain its own named private helper so the reason survives. Without the drain, the fixture's own event moves the cached version before the test fires anything, and the assertion cannot tell the two apart.

## EventHandler

Located in `microservices/<aggregate>/notification/handling/handlers/`.

Use **one concrete `{Consumer}EventHandler` class** per consumer aggregate. It extends `EventHandler` from the simulator core and dispatches all subscribed event types via `instanceof`, delegating to `{Consumer}EventProcessing`. Do **not** create a separate handler subclass per event type.

```java
@Component
public class ShipmentEventHandler extends EventHandler {

    private final ShipmentEventProcessing shipmentEventProcessing;

    public ShipmentEventHandler(ShipmentRepository repository,
                                ShipmentEventProcessing shipmentEventProcessing) {
        super(repository);
        this.shipmentEventProcessing = shipmentEventProcessing;
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        if (event instanceof DeleteWarehouseEvent e) {
            shipmentEventProcessing.processDeleteWarehouseEvent(subscriberAggregateId, e);
        } else if (event instanceof UpdateWarehouseNameEvent e) {
            shipmentEventProcessing.processUpdateWarehouseNameEvent(subscriberAggregateId, e);
        } else if (event instanceof ArchiveWarehouseEvent e) {
            shipmentEventProcessing.processArchiveWarehouseEvent(subscriberAggregateId, e);
        }
    }
}
```

The handler does **not** load or mutate the aggregate — that happens inside `{Consumer}Functionalities` ByEvent methods.

## Polling

Located in `microservices/<aggregate>/notification/handling/<Aggregate>EventHandling.java`.

Each event type gets one `@Scheduled` method. All methods pass the **same** `{Consumer}EventHandler` bean instance:

```java
@Component
public class ShipmentEventHandling implements EventHandling {

    @Autowired
    private EventApplicationService eventApplicationService;

    @Autowired
    private ShipmentEventHandler shipmentEventHandler;

    @Scheduled(fixedDelay = 1000)
    public void handleDeleteWarehouseEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteWarehouseEvent.class, shipmentEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleUpdateWarehouseNameEvents() {
        eventApplicationService.handleSubscribedEvent(UpdateWarehouseNameEvent.class, shipmentEventHandler);
    }
}
```

**`implements EventHandling` is mandatory**, from `ms.notification.EventHandling` — a marker interface with no methods. `EnableDisableEventsController` autowires `List<EventHandling>` and runs every element through `ScheduledAnnotationBeanPostProcessor` to start and stop the scheduled polling. A polling bean that omits it compiles, runs and passes every test, and is simply invisible to `/scheduler/start` and `/scheduler/stop`: its `@Scheduled` loop cannot be paused.

The `@Scheduled` annotation does **not** run in `@DataJpaTest` — call the method manually in tests.

## Naming Conventions

| Layer | Pattern | Example |
|-------|---------|---------|
| Event class | `XxxEvent` | `CreateShipmentEvent` |
| Subscription | `ConsumerSubscribesXxx` | `ShipmentSubscribesUpdateWarehouse` |
| Handler | `{Consumer}EventHandler` (single dispatcher) | `ShipmentEventHandler` |
| Polling bean | `<Consumer>EventHandling` | `ShipmentEventHandling` |
| Processing | `<Consumer>EventProcessing` | `ShipmentEventProcessing` |

## Canonical Wiring Snippet

Use this exact pattern in all skills and implementations. All skill files must reference this snippet rather than defining their own variant.

### Event class
```java
@Entity
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

**`publisherAggregateId`** (passed to `super(...)`) must be the **subscription anchor** — the owning/parent aggregate ID used for version tracking. For a child entity event, this is the parent aggregate's ID, not the child entity's ID.

### Subscription class
```java
public class <Consumer>Subscribes<Xxx> extends EventSubscription {
    public <Consumer>Subscribes<Xxx>(SomeRef ref) {
        super(ref.getAnchorAggregateId(), ref.getAnchorVersion(), <EventName>.class.getSimpleName());
    }

    public <Consumer>Subscribes<Xxx>() {}
}
```

`subscribedAggregateId` (from the `super(...)` call) must match `publisherAggregateId` used in the event constructor. Do not override `subscribesEvent()` to filter — use the service-layer ByEvent method, per § EventSubscription.

### Handler (single dispatcher)

One `{Consumer}EventHandler` bean per consumer aggregate — **not** one subclass per event type:

```java
@Component
public class {Consumer}EventHandler extends EventHandler {

    private final {Consumer}EventProcessing {consumer}EventProcessing;

    public {Consumer}EventHandler({Consumer}Repository repository,
                                  {Consumer}EventProcessing {consumer}EventProcessing) {
        super(repository);
        this.{consumer}EventProcessing = {consumer}EventProcessing;
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        if (event instanceof <EventName> e) {
            {consumer}EventProcessing.process<Xxx>Event(subscriberAggregateId, e);
        }
        // additional instanceof branches for each subscribed event type
    }
}
```

### Polling method

Each `@Scheduled` method passes the shared handler bean:

```java
@Scheduled(fixedDelay = 1000)
public void handle<Xxx>Events() {
    eventApplicationService.handleSubscribedEvent(<EventName>.class, {consumer}EventHandler);
}
```

### EventProcessing class

`EventProcessing` is the bridge between the event handler and the update functionality. It receives the raw event from the handler and delegates to a `*Functionalities` method that performs the actual aggregate update.

```java
@Service
public class <Consumer>EventProcessing {

    @Autowired
    private <Consumer>Functionalities <consumer>Functionalities;

    public void process<Xxx>Event(Integer aggregateId, <EventName> event) {
        <consumer>Functionalities.<operation>ByEvent(aggregateId, event.get<RelevantField>(),
                event.getPublisherAggregateVersion());
    }

    // Removal / invalidation events take no version — the cached row does not survive
    // the mutation, so there is nothing to stamp. See § "Advance the cached publisher version".
    public void process<DeleteXxx>Event(Integer aggregateId, <DeleteEventName> event) {
        <consumer>Functionalities.removeFor<Publisher>ByEvent(aggregateId,
                event.get<Publisher>AggregateId());
    }
}
```

The `ByEvent` suffix is **mandatory** — see § ByEvent sagaState guard below. Calling the saga
`Functionalities` method here instead creates a circular saga loop.

`aggregateId` is the consumer aggregate's ID (passed down from the handler). The `<Consumer>Functionalities` update method opens its own UoW, loads the consumer aggregate, checks `sagaState != NOT_IN_SAGA` (skipping the update if the aggregate is mid-saga to avoid conflicting with its in-progress state), calls the service method that applies the cached-field update and registers the new version changed — which invariant-checks it — and commits.

---

### ByEvent sagaState guard

For every event that mirrors an operation also exposed as a saga `Functionalities` method (e.g., `updateWarehouseName`, `removeShipmentFromWarehouse`), add a separate `{operation}ByEvent` method to `<Consumer>Functionalities`. It opens its own `UnitOfWork`, loads the aggregate to evaluate the guard, delegates the cached-field change to the service, and commits — **without starting a new saga**.

```java
public void {operation}ByEvent(Integer aggregateId, {FieldType} {field}, Long {publisher}Version) {
    SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork();
    {Consumer} aggregate = ({Consumer}) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
    if (!GenericSagaState.NOT_IN_SAGA.equals(((SagaAggregate) aggregate).getSagaState())) {
        return;  // skip — aggregate is mid-saga; avoid conflicting with in-progress state
    }
    {consumer}Service.{operation}(aggregateId, {field}, {publisher}Version, unitOfWork);
    unitOfWorkService.commit(unitOfWork);
}
```

The `Long {publisher}Version` parameter is mandatory for every mutation that leaves the cached row in
place, and absent for one that removes it — see § "Advance the cached publisher version" below for
which is which. Where the event carries several payload fields, they all sit between `aggregateId` and
the version, which stays last before the `UnitOfWork`.

**The service method takes the aggregate id, not the loaded aggregate.** This section owns that
signature. Every service method in the harness is `(Integer aggregateId, ..., UnitOfWork unitOfWork)`
and loads its own aggregate, which is what lets it mutate a factory copy rather than the instance the
caller holds ([`service.md`](service.md) § Copy-on-Write Rule). The load in the ByEvent method exists
to evaluate the guard.

**Why not call the saga `Functionalities` method from `EventProcessing`?** Saga methods set a semantic lock (`sagaState`) and trigger compensations; calling them from an event handler creates a circular saga loop (the saga emits another event → handler fires again → infinite loop). The `ByEvent` method sidesteps this by talking directly to the service layer.

**Where the guard goes.** Put the `sagaState != NOT_IN_SAGA` check inside the `{operation}ByEvent` method **after the load** — not in the shared service method. If the guard lived in a service method that is also called from saga steps on the same aggregate, those saga steps would silently be skipped.

**Every cached publisher version is a `Long`.** The field on the cached sub-entity, the service-method parameter carrying it and any local holding it are all `Long`, because `Event.getPublisherAggregateVersion()` returns `Long` and `EventSubscription`'s constructor is `(Integer subscribedAggregateId, Long subscribedVersion, String eventType)`. `Aggregate.version` is likewise `Long` ([`aggregate.md`](aggregate.md) § Key Fields). Only aggregate *ids* are `Integer`.

**Advance the cached publisher version.** A ByEvent mutation that leaves the cached row in place must stamp the cached publisher version from `event.getPublisherAggregateVersion()` alongside whatever payload fields it applies — not only in the version-carries-no-payload case shown in `.claude/skills/implement-aggregate/session-d.md`. The service method takes the version as a parameter and sets it on the cached entity in the same mutation:

```java
public void set{Entity}{Field}(Integer aggregateId, Integer {entity}AggregateId,
                               {FieldType} {field}, Long {entity}Version, UnitOfWork unitOfWork) {
    {Consumer} old{Consumer} = ({Consumer}) unitOfWorkService
            .aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
    {Consumer} new{Consumer} = {consumer}Factory.create{Consumer}Copy(old{Consumer});
    // ... locate the cached entity on new{Consumer} ...
    cached.set{Field}({field});
    cached.set{Entity}Version({entity}Version);
    unitOfWorkService.registerChanged(new{Consumer}, unitOfWork);
}
```

Without it the subscription's `subscribedVersion` never moves, the same event stays eligible on every subsequent poll, and the backlog described in § "A snapshot-seeded version does not exclude the events already published" never converges. It is also what makes the T3 "reflects event" assertion meaningful for a re-affirming payload.

**The exception: mutations that do not leave a cached row.** A ByEvent method that removes the cached
sub-entity from its collection, or calls `copy.remove()` on the whole consumer (§ Cascade Invalidation
Pattern), takes **no** version parameter and stamps nothing — there is no surviving row to carry it,
and no backlog can build up: `getEventSubscriptions()` constructs one subscription per cached row, so
removing the row removes the subscription with it, and `copy.remove()` takes the consumer out of
`ACTIVE`, which drops every subscription it declares. The redelivery loop that the version guards
against cannot occur where there is no longer a subscription to redeliver against.

Apply this test rather than the shape of the event name: **does a cached row survive this mutation?**
If yes, the version parameter is mandatory; if no, it must be absent.

**Reject an event that does not advance the cached version.** Stamping the version is only half the
rule. Before applying any payload field, compare the incoming version against the one already cached
on the row and return **without** `registerChanged` when it does not advance:

```java
{CachedEntity} cached = find{CachedEntity}(new{Consumer}, {publisher}AggregateId);
if (cached == null) {
    return;
}
if (cached.get{Publisher}Version() != null && cached.get{Publisher}Version() >= {publisher}Version) {
    return;   // stale or replayed event
}
cached.set{Field}({field});
cached.set{Publisher}Version({publisher}Version);

unitOfWorkService.registerChanged(new{Consumer}, unitOfWork);
```

Without it a consumer that drains a two-event backlog ends the poll holding the *older* payload, per
§ "A snapshot-seeded version does not exclude the events already published" above, and the newer
event becomes eligible again on the next poll — so the projection oscillates and gains a version each
time round instead of quieting down.

**The guard lives in the service method, not beside the `sagaState` guard.** Put it after the
copy-on-write load and after the check that the cached row exists. The two guards look alike and sit
in different layers for opposite reasons: the `sagaState` guard must stay out of the service because
saga steps call the same service method and the guard would silently skip them, whereas the version
guard needs the cached row the service has already located, and a service method that takes a
publisher version is reached from the event path only.

**`>=`, not `>`.** An event carrying the version already cached has been folded in; re-applying it
writes a new aggregate version for no change, which is the redelivery loop this rule exists to close.

**Returning without `registerChanged` is the point** — a rejected event must leave no new aggregate
version behind. Where one event writes several cached rows for the same publisher, guard each row on
its own version and skip `registerChanged` only when the event advanced none of them.

**When to skip the `sagaState` guard.** The version guard above is never skipped; this paragraph is
about the saga-state one only. Apply the test: **skip it only when the cached field the event
writes is one that no saga step of this aggregate ever writes.** The guard exists to stop an event
from overwriting a value an in-flight saga is mid-way through setting; where no saga touches that
field, there is nothing to conflict with and the event must apply. Where any saga step does write it,
the guard is mandatory — that includes every standard cached-field update and sub-entity removal.

For whole-consumer invalidation via `copy.remove()`, apply the guard unless a T3 subscription test
(`<Consumer>InterInvariantTest`) explicitly requires processing during an in-flight saga on the same
aggregate.

---

## Cascade Invalidation Pattern

A consumer aggregate that processes a deletion event and becomes non-functional should both mark itself deleted **and** publish its own outbound event so its downstream consumers can react.

**When to use:** when the ByEvent method for a deletion event calls `copy.remove()` on the consumer (see `implement-aggregate/session-d.md` — "Deletion events: remove() on the whole consumer"). Simply marking the consumer `DELETED` and committing is not enough if other aggregates cache references to it.

**Steps:**

1. In the ByEvent service method, after calling `copy.remove()`, register an outbound invalidation event:

```java
// In {Consumer}Service.{operation}ByEvent(...)
copy.remove();
unitOfWorkService.registerEvent(new Invalidate{Consumer}Event(copy.getAggregateId()), unitOfWork);
```

2. Define `Invalidate{Consumer}Event` with `publisherAggregateId` set to the consumer's own aggregate ID:

```java
public class Invalidate{Consumer}Event extends Event {
    public Invalidate{Consumer}Event(Integer consumerAggregateId) {
        super(consumerAggregateId);  // publisherAggregateId = this aggregate's ID
    }
}
```

3. Downstream aggregates that cache a reference to `{Consumer}` declare the subscription and route the event into a ByEvent method, exactly as for any other subscribed event:

```java
// In {Downstream}.getEventSubscriptions() — anchored to the cached {Consumer}'s aggregate id
eventSubscriptions.add(new {Downstream}SubscribesInvalidate{Consumer}(this.get{Consumer}()));

// In {Downstream}Functionalities — the § ByEvent sagaState guard shape
public void invalidate{Consumer}ByEvent(Integer aggregateId) {
    SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork();
    {Downstream} aggregate = ({Downstream}) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
    if (!GenericSagaState.NOT_IN_SAGA.equals(((SagaAggregate) aggregate).getSagaState())) {
        return;
    }
    {downstream}Service.invalidate{Consumer}(aggregateId, unitOfWork);
    unitOfWorkService.commit(unitOfWork);
}
```

**Which branch the service method takes** is decided by whether the downstream aggregate can still function without the invalidated reference:

- **It only caches the reference** → remove the cached sub-entity from its collection and `registerChanged` the copy. The cascade stops here; no outbound event.
- **It is itself non-functional without the reference** → `copy.remove()` plus its own `registerEvent(new Invalidate{Downstream}Event(...))`, i.e. step 1 again one level down. The cascade continues.

**Key invariant:** the outbound invalidation event must use the consumer's own aggregate ID as `publisherAggregateId` so that downstream `EventSubscription` instances anchored to that ID receive it. This is the same rule that applies to all events: `super(anchorAggregateId)` must match the `subscribedAggregateId` of the downstream subscriber.