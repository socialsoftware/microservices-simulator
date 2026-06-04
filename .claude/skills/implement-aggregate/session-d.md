# Session 2.N.d — Event Wiring

This sub-file is loaded by `implement-aggregate` when the target session type is `d`. All context variables (`{app-name}`, `{pkg}`, `{AppClass}`, `{Aggregate}`, `{N}`, `{src}`, `{test}`, `{bean-config}`) are already available from the router.

> This session is only reached when the plan.md aggregate section has a non-empty "Events subscribed" list and a `- [ ] 2.{N}.d` checkbox. The router guarantees this — if you are reading this sub-file, the aggregate subscribes to at least one event.

---

## Reads

Load these files before writing any code:

1. **`docs/concepts/events.md`** — specifically:
   - § Event Classes, § Publishing Events, § EventSubscription (anchor field, `getAggregateId()`, `getEventType()`)
   - § EventHandler, § Polling — dispatch and `@Scheduled` polling
   - § Canonical Wiring Snippet (and all subsections) — the per-file structure for this session
   - § Canonical Wiring Snippet → EventProcessing class, § ByEvent sagaState guard — the contract: `verifyInvariants()` after the cached-field change, plus the saga-state skip
   - § Cascade Invalidation Pattern — only if a deletion event causes `copy.remove()` on this aggregate

2. **`docs/concepts/testing.md`** — § T3 — Inter-Invariant Test and § T3 Deletion-Event Tests. Note:
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

For every event that mirrors an operation also exposed as a saga `Functionalities` method (e.g., `updateStudentName`, `anonymizeStudent`, `removeStudentFromExecution`), add a separate `{operation}ByEvent` method to `{Aggregate}Functionalities`. The full pattern — method body, `sagaState != NOT_IN_SAGA` guard, where the guard goes (after load, not in the shared service method), and when it may be skipped — is documented in `docs/concepts/events.md` § ByEvent sagaState guard. Follow that section.

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
  1. **Reflects event** — create the aggregate, publish the event for the enrolled/owned entity, call the polling method directly, assert the effect. Assert the new cached-field value against what the event payload specifies (from `plan.md`'s subscribed events table) — not by reading what `EventProcessing` does:
     - **Field-update events** (e.g., `UpdateStudentNameEvent`): assert the cached field is updated on the aggregate
     - **Sub-entity removal events** (e.g., `DeleteTopicEvent`): assert the sub-entity is removed from the aggregate's collection
     - **Whole-consumer deletion events** (e.g., `DeleteQuestionEvent` / `DeleteCourseExecutionEvent` received by `Quiz`): the consumer aggregate is marked `DELETED`. Do **not** load it in `then:` — instead move `aggregateLoadAndRegisterRead` into an `and:` block and assert `thrown(SimulatorException)` in `then:`. See the T3 Deletion-Event Tests section in `docs/concepts/testing.md`.
  2. **Ignores unrelated** — enroll entity A, publish the same event for an unrelated entity B, call the polling method directly, assert entity A's cached data is unchanged. **Capture `<originalValue>` in the `given:` block before the event is published** — never read it back after event processing, which would produce a tautological assertion
- **Invariant-violation tests**: if processing the event causes `verifyInvariants()` to throw, assert the exception is raised with the correct error message and that the event is not marked as processed (event-processing outcome). This is an event-processing assertion — not a re-test of the P1 predicate itself (the predicate's violation cases belong in `{Aggregate}IntraInvariantTest.groovy`, T1).
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

When a cached reference may be `null` until populated by a saga step or test setup (e.g., `quizAnswerAggregateId` is `null` until a participant solves the quiz), guard the subscription construction with a null check:

```java
for (TournamentParticipant p : participants) {
    if (p.getQuizAnswer() != null && p.getQuizAnswer().getQuizAnswerAggregateId() != null) {
        subscriptions.add(new TournamentSubscribesQuizAnswerQuestionAnswer(p.getQuizAnswer()));
    }
}
```

In the corresponding T3 test, the "ignores unrelated" scenario can use a second entity whose anchor ID is never registered with the consumer aggregate — no extra setup is needed.

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
