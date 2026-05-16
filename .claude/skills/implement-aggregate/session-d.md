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

4. **For each event in the "Events subscribed" list**: read the source files of the aggregate that publishes it. Specifically:
   - The event class itself (`{src}events/{Event}.java`) — to know the payload fields
   - The publishing aggregate's domain class — to understand what the payload fields represent and how they map to the cached snapshot fields in *this* aggregate

---

## Produce

Produce every file listed in the plan.md `2.{N}.d` row. The authoritative file list is in plan.md — use it exactly.

### One `{Aggregate}Subscribes{Event}.java` per subscribed event

Path: `{src}microservices/{aggregate}/notification/subscribe/{Aggregate}Subscribes{Event}.java`

- Extends `EventSubscription` (from simulator core)
- Constructor: calls `super(anchorRef.getAnchorAggregateId(), anchorRef.getAnchorVersion(), {EventName}.class.getSimpleName())`. The anchor is the owning/parent aggregate whose ID and version are stored in the cached reference (e.g., for `UpdateTopicEvent` subscribed by `Question`, the anchor is the `QuestionTopic` reference that holds `topicAggregateId` and `topicVersion`).
- Empty default constructor: `public {Aggregate}Subscribes{Event}() {}`
- In the **sagas profile**, matching is done by the infrastructure via a DB query on `subscribedAggregateId` and `subscribedVersion` — `EventApplicationService.handleSubscribedEvent()` does **not** call `subscribesEvent()`. Overriding it for sagas event filtering has no effect; any additional filtering must go in the service-layer ByEvent method (see "Shared-anchor events" below). (The TCC profile's `CausalUnitOfWork` does call `subscribesEvent()` for causal consistency checks, but that is out of scope here.)

#### Shared-anchor events: service-layer filtering

When a deletion event is anchored on a **collection-owner** aggregate (e.g., `DisenrollStudentFromCourseExecutionEvent` anchored on `executionAggregateId`), **every** consumer aggregate for that owner receives the event — even those that belong to a different member. The subscription infrastructure cannot distinguish them because it only filters by anchor ID.

In these cases the discriminating check (e.g., `userId`) must happen inside the service ByEvent method before taking action:

```java
// In {Aggregate}Service:
public void removeIfUserMatches(Integer aggregateId, Integer userId, UnitOfWork unitOfWork) {
    {Aggregate} aggregate = get{Aggregate}ById(aggregateId, unitOfWork);
    if (!aggregate.getUserId().equals(userId)) {
        return; // not the affected consumer — ignore silently
    }
    aggregate.remove();
    unitOfWork.registerChanged(aggregate);
}
```

Do **not** attempt to move this check into a `subscribesEvent()` override — it is not called by the sagas event processing infrastructure.

### `{Aggregate}EventHandling.java`

Path: `{src}microservices/{aggregate}/notification/handling/{Aggregate}EventHandling.java`

- Spring `@Component`
- One `@Scheduled(fixedDelay = ...)` method per subscribed event type
- Each method: calls the event service to poll for unprocessed events of that type; for each event found, calls `{Aggregate}EventHandler.handle{Event}(event)`

### `{Aggregate}EventHandler.java`

Path: `{src}microservices/{aggregate}/notification/handling/handlers/{Aggregate}EventHandler.java`

- Spring `@Component`
- A **single concrete class** that extends `EventHandler` and dispatches all subscribed event types
- Constructor: accepts `{Aggregate}Repository` (required by `EventHandler`) and `{Aggregate}EventProcessing`
- `handleEvent(Integer subscriberAggregateId, Event event)`: dispatches via `instanceof` to the appropriate `process{Xxx}Event` call on `{Aggregate}EventProcessing`
- Does NOT load the aggregate — that responsibility belongs to Functionalities

> **Why single-class?** One `@Component` with an `instanceof` dispatch is simpler than a separate class per event type and requires only one bean in BeanConfig. Use the quizzes `CourseExecutionEventHandler` as a reference for the multi-event dispatch pattern.

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

For every event that mirrors an operation that also has a saga Functionalities method (e.g., `updateStudentName`, `anonymizeStudent`, `removeStudentFromExecution`), you **must add a separate `{operation}ByEvent` method** to `{Aggregate}Functionalities` that:

1. Opens its own `UnitOfWork`
2. Loads the aggregate from the service directly
3. Applies the cached-field change
4. Calls `verifyInvariants()`
5. Commits — **without starting a new saga**

**Do NOT call the existing saga Functionalities method from EventProcessing.** Saga methods set a semantic lock (`sagaState`) and trigger compensations; calling them from an event handler creates a circular saga loop (the saga emits another event → handler fires again → infinite loop). The `ByEvent` method sidesteps this by talking directly to the service layer.

```java
// In {Aggregate}Functionalities:
public void {operation}ByEvent(Integer aggregateId, ...) {
    UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork();
    {Aggregate} aggregate = {aggregate}Service.get{Aggregate}ById(aggregateId, unitOfWork);
    {aggregate}Service.{operation}(aggregate, ..., unitOfWork);
    unitOfWorkService.commit(unitOfWork);
}
```

Add the corresponding service helper if needed (pure mutation + `verifyInvariants()`, no saga).

#### Deletion events: `remove()` on the whole consumer vs. remove a sub-entity

When the inbound event signals that a publisher aggregate has been deleted, choose between two actions based on whether the consumer remains valid without that entity:

| Case | When to use | Action in the ByEvent method |
|------|-------------|------------------------------|
| **Remove sub-entity from collection** | The deleted entity is one member of a collection and the consumer remains valid with it absent (e.g., `DeleteTopicEvent` removing one topic from a `Question`'s topic list) | Remove the sub-entity from the collection; do **not** call `remove()` on the aggregate |
| **Invalidate the whole consumer** | The deleted entity is structurally required for the consumer to function (e.g., `DeleteQuestionEvent` for a `Quiz`, `DeleteCourseExecutionEvent` for a `Quiz`) | Call `copy.remove()` to mark the consumer `DELETED`; publish an outbound invalidation event so downstream aggregates can react (see `docs/concepts/events.md` — Cascade Invalidation Pattern) |

The distinguishing question is: *can this consumer aggregate still fulfil its purpose if the referenced entity is gone?* If the answer is no, invalidate the whole consumer.

#### UpdateQuestionEvent for consumers that cache only `questionVersion`

If the consumer aggregate caches no question payload (no title, no content — only a `questionVersion` field on a sub-entity like `QuestionAnswer`), the `UpdateQuestionEvent` subscription exists solely to update that version field:

```java
// In {Aggregate}Service:
public void updateQuestionVersionIn{SubEntity}(Integer aggregateId, Integer questionAggregateId,
                                               Integer publisherVersion, UnitOfWork unitOfWork) {
    {Aggregate} aggregate = get{Aggregate}ById(aggregateId, unitOfWork);
    aggregate.get{SubEntities}().stream()
        .filter(e -> e.getQuestionAggregateId().equals(questionAggregateId))
        .findFirst()
        .ifPresent(e -> e.setQuestionVersion(publisherVersion));
    aggregate.verifyInvariants();
    unitOfWork.registerChanged(aggregate);
}
```

The `publisherVersion` to use is `event.getPublisherAggregateVersion()` (the version of the question aggregate at the time the event was emitted).

### `{Aggregate}InterInvariantTest.groovy` (T3)

Path: `{test}sagas/{aggregate}/{Aggregate}InterInvariantTest.groovy`

- Extends `{AppClass}SpockTest`
- **Two tests per subscribed event type:**
  1. **Reflects event** — create the aggregate, publish the event for the enrolled/owned entity, call the polling method directly, assert the effect:
     - **Field-update events** (e.g., `UpdateStudentNameEvent`): assert the cached field is updated on the aggregate
     - **Sub-entity removal events** (e.g., `DeleteTopicEvent`): assert the sub-entity is removed from the aggregate's collection
     - **Whole-consumer deletion events** (e.g., `DeleteQuestionEvent` / `DeleteCourseExecutionEvent` received by `Quiz`): the consumer aggregate is marked `DELETED`. Do **not** load it in `then:` — instead move `aggregateLoadAndRegisterRead` into an `and:` block and assert `thrown(SimulatorException)` in `then:`. See the T3 Deletion-Event Tests section in `docs/concepts/testing.md`.
  2. **Ignores unrelated** — enroll entity A, publish the same event for an unrelated entity B, call the polling method directly, assert entity A's cached data is unchanged
- **Invariant-violation tests**: if processing the event causes `verifyInvariants()` to throw, assert the exception is raised with the correct error message
- Both the "reflects" and "ignores unrelated" tests are required for every subscribed event type

> **Version numbers:** Aggregate versions start much higher than `1L` because the multi-step test setup issues several commits. Always capture `versionBefore` *after* the setup call completes, then assert `versionAfter > versionBefore` (reflects) or `versionAfter == versionBefore` (ignores). Never hardcode `== 1L` or any specific version number.

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
{Aggregate}EventHandler {aggregate}EventHandler({Aggregate}Repository {aggregate}Repository) {
    return new {Aggregate}EventHandler({aggregate}Repository)
}

@Bean
{Aggregate}EventProcessing {aggregate}EventProcessing() {
    return new {Aggregate}EventProcessing()
}
```

> **Note:** `{Aggregate}EventHandler` requires the repository as a constructor arg because `EventHandler` (simulator core) accepts it. The `{Aggregate}Repository` bean is provided automatically by `@DataJpaTest` auto-configuration in tests.

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
