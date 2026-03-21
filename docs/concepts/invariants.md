# Invariant & Guard Taxonomy

## Introduction

Consistency in this codebase is enforced at six distinct layers. Each layer has a different scope (single aggregate vs. cross-aggregate), timing (before mutation, at commit, or asynchronously), and consistency guarantee (strong or eventual). Multiple layers often protect the same business rule from different angles. Understanding which layer is responsible for what prevents redundant checks and makes it clear where to add a new rule.

---

## Layer 1 — Intra-Invariant

**Definition:** A consistency rule that must always hold within a single aggregate instance. Checked on every Unit of Work commit, regardless of what operation caused the change.

**Where it lives:** `verifyInvariants()` in the base aggregate class (e.g., `Tournament.java`, `Execution.java`).

**When it runs:** Called by the UoW commit path after all mutations for a functionality have been applied but before the new version is persisted.

**Consistency:** Strong — the commit is aborted if any invariant fails.

**Examples:**
- `ANSWER_BEFORE_START` — `applications/.../tournament/aggregate/Tournament.java:verifyInvariants()` — a tournament cannot accept answers before its start time.
- `REMOVE_NO_STUDENTS` — `applications/.../execution/aggregate/Execution.java:verifyInvariants()` — a course execution cannot be removed if it still has enrolled students.

---

## Layer 2 — Aggregate Mutation Guard

**Definition:** A check inside a mutation method (`remove()`, a setter, or a state-transition method) that rejects an illegal state change before it is applied to the aggregate.

**Where it lives:** Mutation methods directly on the aggregate class.

**When it runs:** At the moment the mutation is called, before any field is changed.

**Consistency:** Strong — an exception is thrown synchronously; the aggregate state is never dirtied.

**Examples:**
- `CANNOT_DELETE_COURSE_EXECUTION` — `applications/.../execution/aggregate/Execution.java:remove()` — prevents deletion if the execution is in a state that disallows it.
- `CANNOT_UPDATE_QUIZ` — `applications/.../quiz/aggregate/Quiz.java` setters — prevents quiz fields from being changed after the quiz has been answered.

---

## Layer 3 — Service-Layer Guard

**Definition:** A guard check in the service class that validates cross-entity or state preconditions before creating or mutating an aggregate. Reads from the database to verify preconditions.

**Where it lives:** `*Service.java` (e.g., `ExecutionService.java`).

**When it runs:** Before the aggregate is created or mutated; inside the same UoW transaction as the operation.

**Consistency:** Strong — throws an exception that aborts the UoW before any aggregate is touched.

**Examples:**
- `DUPLICATE_COURSE_EXECUTION` — `applications/.../execution/service/ExecutionService.java` — rejects creation of a course execution that already exists.
- `INACTIVE_USER` — `applications/.../execution/service/ExecutionService.java` — blocks enrollment of an inactive user.

---

## Layer 4 — Functionality Input Validation

**Definition:** Validation of the operation's input parameters at the start of the functionality, before any step reads from the database. Checks things that can be determined from the command object alone.

**Where it lives:** `*Functionality*.java` (Sagas or TCC), typically in the constructor or a dedicated validation block at the top of `executeWorkflow()`.

**When it runs:** Before any workflow step; no aggregate is loaded.

**Consistency:** Strong (input only) — the operation is rejected immediately on invalid input.

**Examples:**
- `QUESTION_TOPIC_INVALID_COURSE` — `applications/.../question/coordination/sagas/CreateQuestionFunctionalitySagas.java` — the question's topic must belong to the same course; checked from command fields before any DB read.
- `USER_MISSING_NAME` — `applications/.../user/coordination/sagas/UpdateStudentNameFunctionalitySagas.java` — the new name must not be blank; validated from the command before any step executes.

---

## Layer 5 — Functionality Cross-Aggregate State Guard

**Definition:** A workflow step that loads a *different* aggregate (not the operation's primary target) under a semantic lock (Sagas) or causal snapshot (TCC) to verify a cross-aggregate precondition before the mutating step is allowed to run.

**Where it lives:** `*FunctionalitySagas.java` / `*FunctionalityTCC.java` — a named step that issues a read command to another service.

**When it runs:** During workflow execution, after the semantic lock on the primary aggregate is acquired, before the mutating step.

**Consistency:** Strong — under Sagas, the read step acquires or respects semantic locks that prevent the inspected aggregate from being concurrently modified in a conflicting way. See [`sagas.md`](sagas.md) for how `forbiddenStates` work.

**Examples:**
- `CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT` — `applications/.../execution/coordination/sagas/RemoveCourseExecutionFunctionalitySagas.java` — a `getCourseStep` reads the `Course` aggregate to check `courseQuestionCount > 0`; if true, the removal step is rejected before any deletion occurs.

---

## Layer 6 — Inter-Invariant (Event-Driven)

**Definition:** A consistency rule that spans multiple aggregates and is maintained via domain events. The consumer aggregate caches a local copy of the relevant publisher state, kept eventually consistent by asynchronous event polling.

**Where it lives:** `getEventSubscriptions()` in the consumer aggregate, plus the event handler chain (`EventHandling` → `EventHandler` → `EventProcessing` → update functionality).

**When it runs:** Asynchronously, on a 1-second poll interval, after the publisher commits an event.

**Consistency:** Eventual — the consumer's cached state may lag behind the publisher by up to one poll interval.

For details on the full event wiring, see below and [`docs/guides/implement-event.md`](../guides/implement-event.md).

### Upstream / Downstream Dependencies

If aggregate **A** caches state from aggregate **B**, then **B is upstream of A**. Event subscriptions always flow downstream: the downstream aggregate subscribes to its upstream aggregate's events and caches the relevant state locally.

`getEventSubscriptions()` always lives in the **downstream (consumer) aggregate**.

#### Dependency Graph (Quizzes)

```
Course ──────────────────────────► Execution
Course ──────────────────────────► Question
Topic ───────────────────────────► Question
Question ────────────────────────► Quiz
User ────────────────────────────► Execution
User ────────────────────────────► Tournament
Execution ───────────────────────► Tournament
Quiz ────────────────────────────► QuizAnswer
```

### Reactive-Update Pattern

The consumer updates its cached state in response to events. No guard method or error throw is needed — the enforcement IS the state change.

Use when: a domain event should cause the consumer to change its own state (e.g., a topic is deleted → remove it from the tournament's cached topic set).

```
Publisher aggregate                Consumer aggregate
      │                                  │
      │  event: XxxHappened              │
      │─────────────────────────────────►│
      │                                  │  poll (1s interval)
      │                                  │  update cached state  ← enforcement IS here
      │                                  │
```

### Components

| Component | Location | Purpose |
|-----------|---------|---------|
| Event class | `quizzes/events/XxxEvent.java` | Carries the data the consumer needs |
| Event subscription | `<consumer>/events/subscribe/ConsumerSubscribesXxx.java` | Declares what to listen for and how to filter |
| Event handler | `<consumer>/events/handling/handlers/XxxEventHandler.java` | Receives and dispatches the event |
| Polling method | `<consumer>/events/handling/<Consumer>EventHandling.java` | `@Scheduled` poll loop |
| Cached state field | `<consumer>/aggregate/<Consumer>.java` | Local copy of publisher data |
| Update functionality | `<consumer>/coordination/sagas/XxxFunctionalitySagas.java` + TCC | Workflow that applies the update to the aggregate |

### Tracked State Patterns

Choose the simplest representation for the invariant:

| State type | Tracked field | Update logic |
|-----------|-------------|-------------|
| Running total | `int xxxCount` | `+1` on create event, `-1` on delete event |
| Presence check | `boolean hasXxx` | `true` on create, `false` on delete |
| Membership | `Set<Integer> xxxIds` | `add(id)` on create, `remove(id)` on delete |
| Single reference | `Integer xxxId` | set on link event, null on unlink event |

The update logic IS the enforcement — no guard check is needed.

### Subscription Granularity

**Per-aggregate** (most common): one subscription per consumer aggregate instance, anchored to a publisher aggregate ID.
```java
// One subscription for the whole consumer aggregate
eventSubscriptions.add(new ConsumerSubscribesXxx(this.publisherRef));
```

**Per-entity**: one subscription per item in a set — used when the consumer tracks a collection and each element can independently publish events. The `subscribedAggregateId` is the entity's own aggregate ID.
```java
// One subscription per entity in the set
for (ConsumerEntity entity : this.entitySet) {
    eventSubscriptions.add(new ConsumerSubscribesXxx(entity));
}
```

### publisherAggregateId Convention

The `publisherAggregateId` passed to `super(...)` in the event constructor **must be the aggregate ID used as the subscription anchor**. This is the same value used in the subscription's `subscribedAggregateId`.

For TCC, this is critical: `EventSubscriberService` uses `publisherAggregateId` to determine which aggregate versions a `CausalExecution` must have processed before it can be causally read alongside other aggregates.

**Wrong:** using the entity ID (e.g., `questionAggregateId`) when the consumer subscribes by `courseAggregateId`.
**Right:** using the same anchor ID in both the event and the subscription.

### Eventual Consistency Trade-off

Cached state is updated asynchronously (default: 1 second poll interval). This is a known trade-off — document it in the summary markdown when implementing a new invariant.

### Worked Examples

| Example |
|---------|
| [`docs/examples/tournament-inter-invariants.md`](../examples/tournament-inter-invariants.md) — reactive-update pattern + per-entity subscriptions, multiple invariants |

### Implementing a New Inter-Invariant

Use the `/inter-invariant` skill:
```
/inter-invariant Tournament remove topic from set when topic deleted
```

The skill walks through all steps for the reactive-update pattern.

---

## Summary Table

| # | Layer | Where | Timing | Consistency |
|---|-------|-------|--------|-------------|
| 1 | **Intra-invariant** | `verifyInvariants()` in aggregate | At UoW commit | Strong |
| 2 | **Aggregate mutation guard** | Mutation methods (`remove()`, setters) | At mutation | Strong |
| 3 | **Service-layer guard** | `*Service.java` | Before aggregate mutation | Strong |
| 4 | **Functionality input validation** | `*Functionality*.java`, no DB read | Before any step | Strong (input) |
| 5 | **Functionality cross-aggregate state guard** | `*Functionality*.java`, saga step reading another aggregate under semantic lock | In workflow step | Strong |
| 6 | **Inter-invariant (event-driven)** | `getEventSubscriptions()` + event handler chain | Async (~1 s) | Eventual |

---

## Enforcement Lifecycle

Order of enforcement layers within a single operation:

```
Request arrives
      │
      ▼
[4] Functionality input validation
    (command fields only, no DB read)
      │
      ▼
[3] Service-layer guard
    (DB read of own aggregate type)
      │
      ▼
[5] Functionality cross-aggregate state guard
    (saga step reads another aggregate under semantic lock)
      │
      ▼
[2] Aggregate mutation guard
    (mutation method checks internal state before applying change)
      │
      ▼
[1] Intra-invariant check
    (verifyInvariants() at UoW commit)
      │
      ▼
    commit
      │
      ▼  (async, ~1 s poll interval)
[6] Inter-invariant event handler
    (consumer caches publisher state update)
```

Layers 1–5 are synchronous and strong. Layer 6 is asynchronous and eventually consistent.
