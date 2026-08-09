# Session 2.N.d — Event Wiring

This sub-file is loaded by `implement-aggregate` when the target session type is `d`. All context variables (`{app-name}`, `{pkg}`, `{AppClass}`, `{Aggregate}`, `{N}`, `{src}`, `{test}`, `{bean-config}`) are already available from the router.

> **Slice scope.** You may be assigned a subset of this session's items. If your brief names specific
> functionalities or events, implement only those, and append to the shared files rather than
> rewriting them. If no subset is named, you own the whole session.

> This session is only reached when the plan.md aggregate section has a non-empty "Events subscribed" list and a `- [ ] 2.{N}.d` checkbox. The router guarantees this — if you are reading this sub-file, the aggregate subscribes to at least one event.

---

## Reads

Load these files before writing any code:

1. **`docs/concepts/events.md`** — specifically:
   - § Event Classes, § Publishing Events, § EventSubscription (anchor field, `getSubscribedAggregateId()`, `getEventType()`)
   - § EventHandler, § Polling — dispatch and `@Scheduled` polling
   - § Canonical Wiring Snippet (and all subsections) — the per-file structure for this session
   - § Canonical Wiring Snippet → EventProcessing class, § ByEvent sagaState guard — the contract: `verifyInvariants()` after the cached-field change, plus the saga-state skip
   - § Cascade Invalidation Pattern — only if a deletion event causes `copy.remove()` on this aggregate

2. **`docs/concepts/testing.md`** — § T3 — Subscription (Inter-Invariant) Test, including the deletion-event `and:`-block pattern, plus § Assertion Ownership. Note:
   - What a T3 subscription test asserts (event received → cached field updated → invariant re-evaluated)
   - How to publish a domain event in a test and verify the consumer processes it
   - How to test deletion events (aggregate marked deleted/invalid after processing)
   - Subscription tests may *trigger* publication via a functionality but must **not** re-assert event-store contents — `{Aggregate}ServiceTest` (T2, session b) owns those assertions

3. **Domain files from session 2.{N}.a** — specifically:
   - `{Aggregate}.java`: field names of the cached snapshot fields that will be updated by events
   - `Saga{Aggregate}.java`: the full class (needed by EventProcessing to load and mutate the aggregate)

4. **For each event in the "Events subscribed" list**: read the source files of the aggregate that publishes it. Specifically:
   - The event class itself (`{src}events/{Event}.java`) — to know the payload fields
   - The publishing aggregate's domain class — to understand what the payload fields represent and how they map to the cached snapshot fields in *this* aggregate

---

## Produce

Produce every file listed in the plan.md `2.{N}.d` row. plan.md is a blueprint, not a manifest: the `###` subheadings below are the authority on what this session must emit, and a file they require but plan.md omits is still produced - amend the row per `_shared/session-completion.md` § "Amend plan.md for omitted files".

### One `{Aggregate}Subscribes{Event}.java` per subscribed event (R5)

Path: `{src}microservices/{aggregate}/notification/subscribe/{Aggregate}Subscribes{Event}.java`

R5 (see `docs/concepts/events.md`): subscription classes and the `getEventSubscriptions()` entries
that return them belong to the **consuming** aggregate only. Never add either to the publisher.

- Extends `EventSubscription` (from simulator core)
- Constructor: calls `super(anchorRef.getAnchorAggregateId(), anchorRef.getAnchorVersion(), {EventName}.class.getSimpleName())`. The anchor is the owning/parent aggregate whose ID and version are stored in the cached reference (e.g., for `UpdateWarehouseEvent` subscribed by `Shipment`, the anchor is the `ShipmentWarehouse` reference that holds `warehouseAggregateId` and `warehouseVersion`).
- Empty default constructor: `public {Aggregate}Subscribes{Event}() {}`
- Matching is done by the infrastructure on `subscribedAggregateId` and `subscribedVersion`. Never override `subscribesEvent()` to add filtering — `docs/concepts/events.md` § EventSubscription owns that rule and its reasons. Additional filtering goes in the service-layer ByEvent method (see "Shared-anchor events" below).

#### Shared-anchor events: service-layer filtering

When a deletion event is anchored on a **collection-owner** aggregate (e.g., `RemoveShipmentFromWarehouseEvent` anchored on `warehouseAggregateId`), **every** consumer aggregate for that owner receives the event — even those that belong to a different member. The subscription infrastructure cannot distinguish them because it only filters by anchor ID.

In these cases the discriminating check (e.g., `shipmentId`) must happen inside the service ByEvent method before taking action:

```java
// In {Aggregate}Service:
public void removeIfShipmentMatches(Integer aggregateId, Integer shipmentId, UnitOfWork unitOfWork) {
    {Aggregate} aggregate = get{Aggregate}ById(aggregateId, unitOfWork);
    if (!aggregate.getShipmentId().equals(shipmentId)) {
        return; // not the affected consumer — ignore silently
    }
    aggregate.remove();
    unitOfWorkService.registerChanged(aggregate, unitOfWork);
}
```

Do **not** attempt to move this check into a `subscribesEvent()` override — see `docs/concepts/events.md` § EventSubscription.

### `{Aggregate}EventHandling.java`

Path: `{src}microservices/{aggregate}/notification/handling/{Aggregate}EventHandling.java`

- Spring `@Component`
- One `@Scheduled(fixedDelay = 1000)` method per subscribed event type
- Each method body is a single call:
  `eventApplicationService.handleSubscribedEvent({Event}.class, {aggregate}EventHandler)` — every
  method passes the **same** autowired handler bean. See `docs/concepts/events.md` § Polling.

### `{Aggregate}EventHandler.java`

Path: `{src}microservices/{aggregate}/notification/handling/handlers/{Aggregate}EventHandler.java`

- Spring `@Component`
- A **single concrete class** that extends `EventHandler` and dispatches all subscribed event types
- Constructor: accepts `{Aggregate}Repository` (required by `EventHandler`) and `{Aggregate}EventProcessing`
- `handleEvent(Integer subscriberAggregateId, Event event)`: dispatches via `instanceof` to the appropriate `process{Xxx}Event` call on `{Aggregate}EventProcessing`
- Does NOT load the aggregate — that responsibility belongs to Functionalities

> **Why single-class?** One `@Component` with an `instanceof` dispatch is simpler than a separate class per event type and requires only one bean in BeanConfig.

The dispatch body has this shape — one branch per subscribed event type:

```java
if (event instanceof {EventNameA} e) {
    {aggregate}EventProcessing.process{Xxx}Event(subscriberAggregateId, e);
} else if (event instanceof {EventNameB} e) {
    {aggregate}EventProcessing.process{Yyy}Event(subscriberAggregateId, e);
}
```

### `{Aggregate}EventProcessing.java`

Path: `{src}microservices/{aggregate}/coordination/eventProcessing/{Aggregate}EventProcessing.java`

- Spring `@Service`
- Injects `{Aggregate}Functionalities` via `@Autowired`
- One `process{Xxx}Event(Integer aggregateId, {EventName} event)` method per subscribed event
- Each method delegates entirely to Functionalities:

```java
@Service
public class {Aggregate}EventProcessing {

    @Autowired
    private {Aggregate}Functionalities {aggregate}Functionalities;

    public void process{Xxx}Event(Integer aggregateId, {EventName} event) {
        {aggregate}Functionalities.{updateMethod}ByEvent(aggregateId, event.get{RelevantField}());
    }
}
```

- Does NOT load, mutate, or persist the aggregate directly
- The cached-field update, `verifyInvariants()`, and UoW commit all happen inside the Functionalities update method

**P2 rule enforcement:** The invariant check happens inside the Functionalities update method, which loads the aggregate, applies the cached-field change, and calls `verifyInvariants()` before committing. If the invariant fails, the exception propagates and the event is not marked as processed (allowing retry or manual intervention).

#### "ByEvent" methods in Functionalities — mandatory pattern

For every event that mirrors an operation also exposed as a saga `Functionalities` method (e.g., `updateWarehouseName`, `archiveWarehouse`, `removeShipmentFromWarehouse`), add a separate `{operation}ByEvent` method to `{Aggregate}Functionalities`. The full pattern — method body, `sagaState != NOT_IN_SAGA` guard, where the guard goes (after load, not in the shared service method), and when it may be skipped — is documented in `docs/concepts/events.md` § ByEvent sagaState guard. Follow that section.

The `{operation}ByEvent` **Functionalities** method is always new — one per event, per `events.md` § ByEvent sagaState guard. The **service** method it delegates to is shared with the saga path: reuse the existing `{Aggregate}Service` mutate method whenever one already performs exactly this mutation. Write a new service helper (pure mutation + `verifyInvariants()`, no saga) only when no existing service method does — typically when the event updates a cached field that no saga operation touches. Never move the `sagaState` guard into the shared service method; it belongs in the ByEvent method after the load, or saga steps calling the same service method are silently skipped.

#### Deletion events: `remove()` on the whole consumer vs. remove a sub-entity

When the inbound event signals that a publisher aggregate has been deleted, choose between two actions based on whether the consumer remains valid without that entity:

| Case | When to use | Action in the ByEvent method |
|------|-------------|------------------------------|
| **Remove sub-entity from collection** | The deleted entity is one member of a collection and the consumer remains valid with it absent (e.g., `DeleteLabelEvent` removing one label from a `Shipment`'s label list) | Remove the sub-entity from the collection; do **not** call `remove()` on the aggregate |
| **Invalidate the whole consumer** | The deleted entity is structurally required for the consumer to function (e.g., `DeleteShipmentEvent` for a `ShipmentItem`, `DeleteWarehouseEvent` for a `Shipment`) | Call `copy.remove()` to mark the consumer `DELETED`; publish an outbound invalidation event so downstream aggregates can react (see `docs/concepts/events.md` — Cascade Invalidation Pattern) |

The distinguishing question is: *can this consumer aggregate still fulfil its purpose if the referenced entity is gone?* If the answer is no, invalidate the whole consumer.

#### `Update{Publisher}Event` for consumers that cache only `{publisher}Version`

If the consumer aggregate caches no publisher payload (no name, no description — only a `warehouseVersion` field on a sub-entity like `ShipmentWarehouse`), the `UpdateWarehouseEvent` subscription exists solely to update that version field:

```java
// In {Aggregate}Service:
public void updateWarehouseVersionIn{SubEntity}(Integer aggregateId, Integer warehouseAggregateId,
                                                Long publisherVersion, UnitOfWork unitOfWork) {
    {Aggregate} aggregate = get{Aggregate}ById(aggregateId, unitOfWork);
    aggregate.get{SubEntities}().stream()
        .filter(e -> e.getWarehouseAggregateId().equals(warehouseAggregateId))
        .findFirst()
        .ifPresent(e -> e.setWarehouseVersion(publisherVersion));
    aggregate.verifyInvariants();
    unitOfWorkService.registerChanged(aggregate, unitOfWork);
}
```

The `publisherVersion` to use is `event.getPublisherAggregateVersion()` (the version of the publisher aggregate at the time the event was emitted). It is a `Long`, as is the cached `warehouseVersion` field it is assigned to - see `docs/concepts/events.md` § "Always advance the cached publisher version", which owns the rule.

This section covers the case where the version is the *only* thing cached. Stamping that version is **not** confined to it: every ByEvent mutation advances the cached publisher version, whatever payload fields it also applies — see `docs/concepts/events.md` § ByEvent sagaState guard, "Always advance the cached publisher version", and the redelivery backlog it bounds.

### `{Aggregate}InterInvariantTest.groovy` (T3 subscription)

Path: `{test}sagas/{aggregate}/{Aggregate}InterInvariantTest.groovy`

- Extends `{AppClass}SpockTest`
- **Two tests per subscribed event type:**
  1. **Reflects event** — create the aggregate, publish the event for the cached/owned entity, call the polling method directly, assert the effect. Assert the new cached-field value against what the event payload specifies (from `plan.md`'s subscribed events table) — not by reading what `EventProcessing` does:
     - **Field-update events** (e.g., `UpdateWarehouseNameEvent`): assert the cached field is updated on the aggregate
     - **Sub-entity removal events** (e.g., `DeleteLabelEvent`): assert the sub-entity is removed from the aggregate's collection
     - **Whole-consumer deletion events** (e.g., `DeleteShipmentEvent` / `DeleteWarehouseEvent` received by `ShipmentItem`): the consumer aggregate is marked `DELETED` — follow the deletion-event pattern in `testing.md` § T3 — Subscription (Inter-Invariant) Test.
  2. **Ignores unrelated** — cache entity A on the aggregate, publish the same event for an unrelated entity B, call the polling method directly, assert entity A's cached data is unchanged. Follow `testing.md` § T3 for where to capture the original value.
- **Re-affirming payloads.** Some events carry a value the consumer is already guaranteed to hold,
  because a guard on the operation that cached the entity admits only that value (e.g. an
  `Activate{Entity}Event` carrying `active=true` reaching a consumer whose own P3 guard on the
  caching operation already rejects an inactive `{Entity}`). Asserting the payload value there is
  trivially satisfied, which
  `testing.md` § Fake forbids. Resolve in this order:
  1. **Prefer a reachable contrary state.** If the consumer can legally reach a state where the
     cached field differs from the payload, set that state up in `given:` and assert the transition.
     The payload assertion is then non-trivial and discharges the test on its own.
  2. **Otherwise assert the payload value plus the cached publisher-version advance.** When no such
     state is reachable — no subscribed event and no operation can produce the contrary value — keep
     the payload assertion as the statement of the spec, and add `versionAfter > versionBefore` on
     the cached publisher version, which is the assertion that fails if the handler never ran.
     Capture `versionBefore` after setup, per the Version numbers note below.
  Never drop the "reflects event" test: it is the only evidence the polling method reached the
  consumer at all.
- **Invariant-violation tests**: if processing the event causes `verifyInvariants()` to throw, assert the exception is raised with the correct error message and that the event is not marked as processed (event-processing outcome). This is an event-processing assertion — not a re-test of the P1 predicate itself (the predicate's violation cases belong in `{Aggregate}IntraInvariantTest.groovy`, T1 Aggregate tier).
- Both the "reflects" and "ignores unrelated" tests are required for every subscribed event type

> **Version numbers:** Aggregate versions start much higher than `1L` because the multi-step test setup issues several commits. Always capture `versionBefore` *after* the setup call completes, then assert `versionAfter > versionBefore` (reflects) or `versionAfter == versionBefore` (ignores). Never hardcode `== 1L` or any specific version number.

### `getEventSubscriptions()` — required ACTIVE guard

Every aggregate that subscribes to events **must** wrap all subscription construction inside an `if (getState() == AggregateState.ACTIVE)` guard. Without it, deleted/inactive aggregates return subscriptions on every commit and trigger spurious event handlers:

```java
@Override
public Set<EventSubscription> getEventSubscriptions() {
    Set<EventSubscription> eventSubscriptions = new HashSet<>();
    if (getState() == AggregateState.ACTIVE) {
        // ... build all subscriptions here ...
    }
    return eventSubscriptions;
}
```

See `docs/concepts/aggregate.md` for the full rationale.

### Conditional subscriptions (nullable anchor IDs)

When a cached reference may be `null` until populated by a saga step or test setup (e.g., a `{Entity}AggregateId` that stays `null` until the functionality which links the two aggregates has run), guard the subscription construction with a null check:

```java
for ({OwnedEntity} e : ownedEntities) {
    if (e.get{Entity}() != null && e.get{Entity}().get{Entity}AggregateId() != null) {
        subscriptions.add(new {Aggregate}Subscribes{Event}(e.get{Entity}()));
    }
}
```

In the corresponding T3 subscription test, the "ignores unrelated" scenario can use a second entity whose anchor ID is never registered with the consumer aggregate — no extra setup is needed.

### Error message constants

If event processing can violate any invariant not already covered, open `{src}microservices/exception/{AppClass}ErrorMessage.java` and add the new constant. Append only — do not remove existing constants.

---

## Update BeanConfigurationSagas.groovy

Open `{bean-config}` and add three new `@Bean` methods:

```groovy
@Bean
{Aggregate}EventHandling {aggregate}EventHandling() {
    return new {Aggregate}EventHandling()
}

@Bean
{Aggregate}EventHandler {aggregate}EventHandler({Aggregate}Repository {aggregate}Repository,
                                                {Aggregate}EventProcessing {aggregate}EventProcessing) {
    return new {Aggregate}EventHandler({aggregate}Repository, {aggregate}EventProcessing)
}

@Bean
{Aggregate}EventProcessing {aggregate}EventProcessing() {
    return new {Aggregate}EventProcessing()
}
```

> **Note:** `{Aggregate}EventHandler` takes the repository (required by `EventHandler` in the simulator core) **and** `{Aggregate}EventProcessing`, matching the constructor described above. The `{Aggregate}Repository` bean is provided automatically by `@DataJpaTest` auto-configuration in tests.

Add the corresponding `import` statements. Place new beans after the write/read functionality beans for this aggregate.

---

## Tick the Checkbox

In plan.md, replace:
```
- [ ] 2.{N}.d — Event wiring
```
with:
```
- [x] 2.{N}.d — Event wiring
```
