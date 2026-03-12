---
name: inter-invariant
description: Implement a new inter-invariant in the microservices-simulator quizzes application. Use this when the user wants to enforce a cross-aggregate consistency rule triggered by domain events. Arguments: "<ConsumerAggregate> <condition>", e.g. "Execution courseQuestionCount > 0 when last execution"
argument-hint: "<ConsumerAggregate> <condition-description>"
---

# Implement Inter-Invariant: $ARGUMENTS

You are implementing a new **inter-invariant** in the `applications/quizzes` module.
An inter-invariant is a consistency rule that spans multiple aggregates and is maintained via domain events (eventually consistent).

---

## Step 0 — Understand the request

Parse `$ARGUMENTS` to identify:
- **ConsumerAggregate**: the aggregate that must observe external state
- **PublisherAggregate**: the aggregate that owns the changing data
- **Trigger events**: what domain actions the consumer must react to
- **Tracked state**: what data the consumer needs to cache locally (could be a boolean flag, a count, a reference, a set of IDs — anything)
- **Guard condition**: the boolean predicate checked when the guarded operation is called
- **Guarded operation**: the service method that throws when the guard fails
- **Error message key**: name for the `QuizzesErrorMessage` enum entry

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
7. `QuizzesErrorMessage.java` — existing error constants
8. The existing test class closest to the guarded operation

Use `docs/examples/cannot-delete-last-execution-with-content.md` as a structural reference (data flow, file list, code snippets per layer, consistency note, test table), but do not copy its domain logic.

---

## Step 2 — Design the tracked state

Decide what the consumer needs to cache locally to evaluate the guard at operation time.

Examples:
- A counter (`int questionCount`) when the condition depends on a running total
- A boolean flag (`boolean hasActiveEnrollments`) when the condition is binary
- A set of IDs (`Set<Integer> enrolledUserIds`) when the condition depends on membership
- A single reference (`Integer linkedAggregateId`) when the condition checks existence

Choose the simplest representation that satisfies the guard. Document your decision before proceeding.

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
4. Add a private helper for the new inter-invariant subscriptions and call it from `getEventSubscriptions()`:

```java
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
        super(ref.getAggregateId(), ref.getVersion(), XxxEvent.class);
    }
    @Override
    public boolean filter(Event event) {
        XxxEvent e = (XxxEvent) event;
        return e.getRelevantId().equals(this.subscribedAggregateId);
    }
}
```

`subscribedAggregateId` must match `publisherAggregateId` used in the event.

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
Each handler: receives the event, filters via the subscription, extracts needed data, and delegates to `<Consumer>EventProcessing`.

---

## Step 9 — Wire polling in EventHandling bean

In `microservices/<consumer>/events/handling/<Consumer>EventHandling.java`, add one `@Scheduled` method per event type:
```java
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

---

## Step 13 — Add error message

In `QuizzesErrorMessage.java`:
```java
CANNOT_<OPERATION>_WHEN_<CONDITION>("Cannot <operation>: <human-readable reason>"),
```

---

## Step 14 — Enforce the guard in the service

In the guarded service method, add the check **before** the mutating call (e.g. `aggregate.remove()`):
```java
// Inter-invariant: <one-line description>
if (<guard condition on tracked state>) {
    throw new QuizzesException(CANNOT_<OPERATION>_WHEN_<CONDITION>, aggregateId);
}
```

---

## Step 15 — Write tests

Add test cases to the relevant `*Test.groovy` under `src/test/groovy/.../sagas/coordination/<consumer>/`.

Cover at minimum:
| Scenario | Expected outcome |
|----------|-----------------|
| Guard condition is false (operation safe) | Operation succeeds |
| Guard condition is true (operation blocked) | `QuizzesException` with correct error code |
| Any relevant boundary case | Correct behaviour |

Trigger event processing manually in tests by calling the `<Consumer>EventHandling.handleXxxEvents()` method (the `@Scheduled` annotation does not run in `@DataJpaTest`).

---

## Step 16 — Write summary markdown

Create `docs/examples/cannot-<operation>-when-<condition>.md` with:
- One-line rule description (type, enforced in, rule)
- Data flow diagram (ASCII)
- Key files table
- Code snippet per layer
- Consistency note (eventual vs. strong)
- Test table

---

## Checklist before finishing

- [ ] Event class(es) created with correct `publisherAggregateId`
- [ ] Events registered in publisher service
- [ ] Tracked state field(s) added, initialised, and copied in copy-constructor
- [ ] Event subscriptions created and wired into `getEventSubscriptions()`
- [ ] Command class created with appropriate payload
- [ ] Event handlers created
- [ ] Polling methods added to EventHandling bean
- [ ] EventProcessing → Functionalities wiring added
- [ ] Sagas + TCC functionality classes created
- [ ] State update method(s) added to ConsumerService
- [ ] Error message added to `QuizzesErrorMessage`
- [ ] Guard check added to the guarded service method
- [ ] Tests written and passing (`mvn clean -Ptest-sagas test`)
- [ ] Summary markdown written in `docs/examples/`
