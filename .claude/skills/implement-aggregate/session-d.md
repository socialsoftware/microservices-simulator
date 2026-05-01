# Session 2.N.d — Event Wiring

This sub-file is loaded by `implement-aggregate` when the target session type is `d`. All context variables (`{app-name}`, `{pkg}`, `{AppClass}`, `{Aggregate}`, `{N}`, `{src}`, `{test}`, `{bean-config}`) are already available from the router.

> This session is only reached when the plan.md aggregate section has a non-empty "Events subscribed" list and a `- [ ] 2.{N}.d` checkbox. The router guarantees this — if you are reading this sub-file, the aggregate subscribes to at least one event.

---

## Reads

Load these files before writing any code:

1. **`docs/concepts/events.md`** — the full file. Pay attention to:
   - `EventSubscription` subclass structure (anchor field, `getAggregateId()`, `getEventType()`)
   - `@Scheduled` polling pattern in `{Aggregate}EventHandling`
   - How `{Aggregate}EventHandler` dispatches to `{Aggregate}EventProcessing`
   - How `process{Event}(...)` updates cached snapshot fields on the aggregate
   - The contract: call `verifyInvariants()` after updating snapshot fields
   - What `anchor aggregate id` means and how to filter events by it

2. **`docs/concepts/testing.md`** — T3 section only (Inter-Invariant Tests). Note:
   - What a T3 test asserts (event received → cached field updated → invariant re-evaluated)
   - How to publish a domain event in a test and verify the consumer processes it
   - How to test deletion events (aggregate marked deleted/invalid after processing)

3. **Domain files from session 2.{N}.a** — specifically:
   - `{Aggregate}.java`: field names of the cached snapshot fields that will be updated by events
   - `Saga{Aggregate}.java`: the full class (needed by EventProcessing to load and mutate the aggregate)
   - `{Aggregate}SagaState.java`: state enum (EventProcessing must skip aggregates in a saga)

4. **For each event in the "Events subscribed" list**: read the source files of the aggregate that publishes it. Specifically:
   - The event class itself (`{src}events/{Event}.java`) — to know the payload fields
   - The publishing aggregate's domain class — to understand what the payload fields represent and how they map to the cached snapshot fields in *this* aggregate

---

## Produce

Produce every file listed in the plan.md `2.{N}.d` row. The authoritative file list is in plan.md — use it exactly.

### One `{Aggregate}Subscribes{Event}.java` per subscribed event

Path: `{src}microservices/{aggregate}/notification/subscribe/{Aggregate}Subscribes{Event}.java`

- Extends `EventSubscription` (from simulator core)
- Constructor: calls `super({Event}.class)` (or equivalent registration call — check events.md)
- `getAggregateId(DomainEvent event)`: casts `event` to `{Event}`, returns the field that identifies this aggregate (the "anchor" — e.g., for `DeleteUserEvent` subscribed by Execution, the anchor is `userId`, filtered to find all Execution aggregates that have that `userId` in their students list)
- `getEventType()`: returns `{Event}.class`

### `{Aggregate}EventHandling.java`

Path: `{src}microservices/{aggregate}/notification/handling/{Aggregate}EventHandling.java`

- Spring `@Component`
- One `@Scheduled(fixedDelay = ...)` method per subscribed event type
- Each method: calls the event service to poll for unprocessed events of that type; for each event found, calls `{Aggregate}EventHandler.handle{Event}(event)`

### `{Aggregate}EventHandler.java`

Path: `{src}microservices/{aggregate}/notification/handling/handlers/{Aggregate}EventHandler.java`

- Spring `@Component`
- Extends the `{Aggregate}EventHandler` base class (which holds the repository and EventProcessing references)
- One concrete subclass per subscribed event type (matching the quizzes pattern — e.g., `UpdateStudentNameEventHandler`, `DeleteTopicEventHandler`)
- `handleEvent(Integer subscriberAggregateId, Event event)`: casts the event to the concrete type and calls `this.{aggregate}EventProcessing.process{Xxx}Event(subscriberAggregateId, ({EventName}) event)`
- Does NOT load the aggregate — that responsibility belongs to Functionalities

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
        {aggregate}Functionalities.{updateMethod}(aggregateId, event.get{RelevantField}());
    }
}
```

- Does NOT load, mutate, or persist the aggregate directly
- The sagaState guard, cached-field update, `verifyInvariants()`, and UoW commit all happen inside the Functionalities update method

**P2 rule enforcement:** The invariant check happens inside the Functionalities update method, which loads the aggregate, applies the cached-field change, and calls `verifyInvariants()` before committing. If the invariant fails, the exception propagates and the event is not marked as processed (allowing retry or manual intervention).

### `{Aggregate}InterInvariantTest.groovy` (T3)

Path: `{test}sagas/{aggregate}/{Aggregate}InterInvariantTest.groovy`

- Extends `{AppClass}SpockTest`
- One test per subscribed event type:
  - **Field-update events** (e.g., `UpdateStudentNameEvent`): create the aggregate, publish the event, assert the cached field is updated on the aggregate
  - **Deletion events** (e.g., `DeleteTopicEvent`): create the aggregate referencing the entity, publish the deletion event, assert the aggregate reflects the deletion (entity removed, aggregate marked invalid, etc.)
  - **Invariant-violation tests**: if processing the event causes `verifyInvariants()` to throw, assert the exception is raised with the correct error message

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
{Aggregate}EventHandler {aggregate}EventHandler() {
    return new {Aggregate}EventHandler()
}

@Bean
{Aggregate}EventProcessing {aggregate}EventProcessing() {
    return new {Aggregate}EventProcessing()
}
```

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
