---
name: inter-invariant
description: Implement a new inter-invariant in the microservices-simulator quizzes application. Use this when the user wants to enforce a cross-aggregate consistency rule triggered by domain events. Arguments: "<ConsumerAggregate> <condition>", e.g. "Execution courseQuestionCount > 0 when last execution"
argument-hint: "<ConsumerAggregate> <condition-description>"
---

# Implement Inter-Invariant: $ARGUMENTS

You are implementing a new **inter-invariant** in the `applications/quizzes` module.
An inter-invariant is a **Layer 6** consistency rule that spans multiple aggregates and is maintained via domain events (eventually consistent).

> If uncertain which layer to use for this rule, consult `docs/concepts/invariants.md` before proceeding.

---

## Step 0 — Understand the request and identify the pattern

Parse `$ARGUMENTS` to identify:

**For both patterns:**
- **ConsumerAggregate**: the aggregate that must observe external state
- **PublisherAggregate**: the aggregate that owns the changing data
- **Trigger events**: what domain actions the consumer must react to
- **Tracked state**: what data the consumer needs to cache locally (boolean flag, count, reference, set of IDs, …)
- **Update logic**: how the cached state changes in response to each event

**Determine the pattern:**

- **Reactive-update**: the enforcement IS the state update — no guard or error throw is needed. Use this when a domain event should cause the consumer to update its own state (e.g., topic deleted → remove from set).
- **Guard-enforcement**: the consumer also needs a guard method that throws before a mutating operation is allowed. Use this when an operation must be blocked based on cached external state.

**For guard-enforcement only, also identify:**
- **Guard condition**: the boolean predicate checked when the guarded operation is called
- **Guarded operation**: the service method that throws when the guard fails
- **Error message key**: name for the `QuizzesErrorMessage` enum entry

**Layer decision:**

- **Use Layer 6 (this skill)** — when the guarded condition changes infrequently and ~1 s eventual lag is acceptable; the cached state is already tracked by the event chain.
- **Use Layer 5 instead** — when the condition must be strongly consistent at operation time; state is maintained synchronously in the same saga workflow (e.g., `cannot-delete-last-execution-with-content.md` uses a `getCourseStep` to read the Course aggregate under a semantic lock — that is a **Layer 5 example, not an async inter-invariant**).

Ask the user to clarify any of the above if the arguments are ambiguous before writing any code.

---

## Step 1 — Read existing code

Before writing anything, read:
1. The **ConsumerAggregate** domain class under `microservices/<consumer>/aggregate/`
2. The **ConsumerAggregate** service under `microservices/<consumer>/service/`
3. The **PublisherAggregate** service under `microservices/<publisher>/service/`
4. The `microservices/<consumer>/events/` directory — understand existing event wiring
5. The `events/` top-level directory — see existing shared event classes
6. The `microservices/<consumer>/coordination/` directory — existing functionality examples
7. `QuizzesErrorMessage.java` — existing error constants (guard-enforcement only)
8. The existing test class closest to the affected operation

Use the appropriate worked example as a structural reference:
- **Guard-enforcement pattern**: `docs/examples/cannot-delete-last-execution-with-content.md` (**Layer 5 — not async**; use for structural reference only)
- **Reactive-update or multiple invariants**: `docs/examples/tournament-inter-invariants.md`

Do not copy domain logic — use them for structure (data flow, file list, code shape per layer).

---

## Step 2 — Design the tracked state

Decide what the consumer needs to cache locally.

Examples:
- A counter (`int questionCount`) when the condition depends on a running total
- A boolean flag (`boolean hasActiveEnrollments`) when the condition is binary
- A set of IDs (`Set<Integer> enrolledUserIds`) when the condition depends on membership
- A single reference (`Integer linkedAggregateId`) when the condition checks existence

**Reactive-update**: the update logic IS the enforcement — no guard is needed. Document the update rule (e.g., "remove topicId from set on DeleteTopic event").

**Guard-enforcement**: also design the guard predicate that will be checked at operation time (e.g., "throw if questionCount > 0").

Choose the simplest representation that satisfies the invariant. Document your decision before proceeding.

---

## Step 3 — Implement: Shared event classes

Create or reuse event classes in `applications/quizzes/src/main/java/.../quizzes/events/`.

For each trigger action:
```java
public class XxxEvent extends Event {
    // publisherAggregateId: must be the aggregate ID used as the subscription anchor
    //   for TCC consistency tracking (usually the parent/owning aggregate, not the entity itself)
    // carry only the fields the consumer needs to filter and update its tracked state
}
```

If a matching event already exists, reuse it and skip creation.

---

## Step 4 — Publish events from the publisher service

In the publisher's `XxxService.java`, register the event at the right point:
```java
unitOfWorkService.registerEvent(new XxxEvent(...), unitOfWork);
```

Add one registration per trigger action.

---

## Step 5 — Add tracked state to the ConsumerAggregate

In `microservices/<consumer>/aggregate/<Consumer>.java`:
1. Add the field(s) that represent the tracked state, with getter/setter.
2. Initialise them to a safe default (e.g. `0`, `false`, `new HashSet<>()`).
3. Copy them in the copy-constructor.
4. Add a private helper for the new inter-invariant subscriptions and call it from `getEventSubscriptions()`.

Subscriptions are only registered while the aggregate is ACTIVE. Inactive or deleted aggregates do not receive further updates:

```java
@Override
public Set<EventSubscription> getEventSubscriptions() {
    Set<EventSubscription> eventSubscriptions = new HashSet<>();
    if (getState() == AggregateState.ACTIVE) {
        interInvariantXxx(eventSubscriptions);
    }
    return eventSubscriptions;
}

private void interInvariantXxx(Set<EventSubscription> eventSubscriptions) {
    // add one subscription per event type this invariant reacts to
    eventSubscriptions.add(new ConsumerSubscribesXxx(this.relevantRef));
}
```

---

## Step 6 — Implement: Event subscription classes

Create under `microservices/<consumer>/events/subscribe/`:
```java
public class ConsumerSubscribesXxx extends EventSubscription {
    public ConsumerSubscribesXxx(<Ref> ref) {
        super(ref.getAggregateId(), ref.getVersion(), XxxEvent.class.getSimpleName());
    }

    public ConsumerSubscribesXxx() {}

    @Override
    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
```

`subscribedAggregateId` must match `publisherAggregateId` used in the event.

**Per-entity subscriptions**: if the consumer tracks a set of entities and each entity independently publishes events, create one subscription per entity in the set. The `subscribedAggregateId` is each entity's own aggregate ID:
```java
// In interInvariantXxx():
for (ConsumerEntity entity : this.entitySet) {
    eventSubscriptions.add(new ConsumerSubscribesXxx(entity));
}
```

---

## Step 7 — Implement: Command

Create `command/<consumer>/XxxCommand.java` with the fields needed by the service method that will update the tracked state. The shape depends entirely on the domain:

```java
public class XxxCommand extends Command {
    private final Integer consumerAggregateId;
    // add whatever payload the service needs (a delta, a flag, an ID, ...)
}
```

---

## Step 8 — Implement: Event handlers

Create under `microservices/<consumer>/events/handling/handlers/` one handler per event type.

If a base `<Consumer>EventHandler` already exists in that package, extend it. Create the abstract base only if absent.

Each concrete handler receives the event, filters via the subscription, extracts needed data, and delegates to `<Consumer>EventProcessing`:

```java
// Concrete handler per event type
public class XxxEventHandler extends ConsumerEventHandler {
    public XxxEventHandler(ConsumerRepository repo, ConsumerEventProcessing processing) {
        super(repo, processing);
    }

    @Override
    public void handleEvent(Integer aggregateId, Event event) {
        this.consumerEventProcessing.processXxxEvent(aggregateId, (XxxEvent) event);
    }
}
```

---

## Step 9 — Wire polling in EventHandling bean

In `microservices/<consumer>/events/handling/<Consumer>EventHandling.java`, add one `@Scheduled` method per event type, with an inline invariant-name comment documenting which invariant the handler enforces:

```java
/*
    <INVARIANT_NAME>
*/
@Scheduled(fixedDelay = 1000)
public void handleXxxEvents() {
    eventApplicationService.handleSubscribedEvent(XxxEvent.class,
            new XxxEventHandler(consumerRepository, eventProcessing));
}
```

---

## Step 10 — Wire EventProcessing → Functionalities

In `<Consumer>EventProcessing.java`, add a method per event type that:
1. Identifies the affected consumer aggregate(s)
2. Calls the appropriate method on `<Consumer>Functionalities`

In `<Consumer>Functionalities.java`, add methods that create a UoW and dispatch the command via the command gateway.

---

## Step 11 — Implement: Sagas and TCC functionalities

Create:
- `coordination/sagas/XxxFunctionalitySagas.java`
- `coordination/causal/XxxFunctionalityTCC.java`

Each builds a `Workflow` (typically a single step) that sends the command to the consumer service. Follow the structure of existing functionality classes in those packages.

---

## Step 12 — Implement: State update in ConsumerService

Add the method(s) called by the command handler to `<Consumer>Service.java`. The method must:
1. Load the current aggregate version
2. Copy it
3. Update the tracked state field(s) according to the event semantics
4. Register the changed aggregate

The exact logic depends on the tracked state type — a counter increments/decrements, a boolean flips, a set adds/removes an ID, etc.

**Reactive-update**: this update IS the enforcement. No further guard step is needed — stop here (skip Steps 13–14).

---

## Step 13 — Add error message *(Guard-Enforcement Pattern Only)*

In `QuizzesErrorMessage.java`:
```java
CANNOT_<OPERATION>_WHEN_<CONDITION>("Cannot <operation>: <human-readable reason>"),
```

---

## Step 14 — Enforce the guard *(Guard-Enforcement Pattern Only)*

The guard has two parts:

**Part A — guard method in the consumer service** (`<consumer>/service/<Consumer>Service.java`):
```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public void assertCanPerform<Operation>(Integer consumerAggregateId, ..., UnitOfWork unitOfWork) {
    <Consumer> aggregate = (<Consumer>) unitOfWorkService.aggregateLoadAndRegisterRead(consumerAggregateId, unitOfWork);
    // Inter-invariant: <one-line description>
    if (<guard condition on tracked state>) {
        throw new QuizzesException(CANNOT_<OPERATION>_WHEN_<CONDITION>, consumerAggregateId);
    }
}
```

The guard method loads only the **consumer's own aggregate type** — `aggregateLoadAndRegisterRead` must not be used to load a different service's aggregate. The guard reads the consumer's own cached eventual state from the event chain; it never reads a foreign aggregate directly.

**Part B — guard invocation in the guarded operation's functionality**:

Create a command (`command/<consumer>/Assert<Condition>Command.java`) and add a dedicated step in both `<op>/coordination/sagas/<Op>FunctionalitySagas.java` and the TCC variant that sends this command **before** the mutating step:
```java
// Sagas
Step checkGuardStep = new SagaStep("checkGuardStep", () -> {
    commandGateway.send(new Assert<Condition>Command(unitOfWork, ServiceMapping.<CONSUMER>.getServiceName(), ...));
});
// mutatingStep depends on checkGuardStep
```

For TCC, add the command send inline before the mutating command in the single composite step.

Do **not** add the guard check inside the operation service itself (e.g., `QuizAnswerService.startQuiz()`), as that would require loading a foreign aggregate type.

---

## Step 15 — Write tests

Add test cases to the relevant `*Test.groovy` under `src/test/groovy/.../sagas/coordination/<consumer>/`.

**Reactive-update**: cover at minimum:
| Scenario | Expected outcome |
|----------|-----------------|
| Event received, state updated | Consumer aggregate reflects the change |
| Event received for non-member entity | State unchanged |

**Guard-enforcement**: cover at minimum:
| Scenario | Expected outcome |
|----------|-----------------|
| Guard condition is false (operation safe) | Operation succeeds |
| Guard condition is true (operation blocked) | `QuizzesException` with correct error code |
| Any relevant boundary case | Correct behaviour |

Trigger event processing manually in tests by calling the `<Consumer>EventHandling.handleXxxEvents()` method (the `@Scheduled` annotation does not run in `@DataJpaTest`).

---

## Step 16 — Write summary markdown

Create `docs/examples/<invariant-name>.md` with the following structure (matching `tournament-inter-invariants.md`):

1. **Header**: type (Layer 6 — inter-invariant), consumer aggregate, one-line rule
2. **Invariant overview table**: name | publisher | trigger event | effect
3. **Subscription wiring snippet** (`getEventSubscriptions()`)
4. **Per-invariant details**: formal rule, subscription snippet, polling snippet, processing chain
5. **Consistency properties**: eventual, ~1 s lag
6. **Tests table**

Then add a row to `docs/examples/README.md` linking the new file.

---

## Checklist before finishing

**Both patterns:**
- [ ] Event class(es) created with correct `publisherAggregateId`
- [ ] Events registered in publisher service
- [ ] Tracked state field(s) added, initialised, and copied in copy-constructor
- [ ] Event subscriptions created and wired into `getEventSubscriptions()`
- [ ] Event subscriptions wrapped in `if (getState() == ACTIVE)`
- [ ] Command class created with appropriate payload
- [ ] Event handlers created
- [ ] Polling methods added to EventHandling bean (with invariant-name comment)
- [ ] EventProcessing → Functionalities wiring added
- [ ] Sagas + TCC functionality classes created
- [ ] State update method(s) added to ConsumerService
- [ ] Tests written and passing (`mvn clean -Ptest-sagas test`)
- [ ] Summary markdown written in `docs/examples/` and linked from `docs/examples/README.md`

**Guard-enforcement pattern only:**
- [ ] Error message added to `QuizzesErrorMessage`
- [ ] Guard method added to the consumer service (loads only its own aggregate type; reads cached eventual state)
- [ ] Guard command created
- [ ] Guard invocation step added to both Sagas and TCC functionalities (before the mutating step)
