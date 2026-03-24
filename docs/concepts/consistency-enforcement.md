# Consistency Enforcement Taxonomy

## Introduction

Consistency in this codebase is enforced at five distinct layers. Each layer has a different scope (single aggregate vs. cross-aggregate), timing (before mutation, at commit, or asynchronously), and consistency guarantee (strong or eventual). The layers cover two distinct kinds of rule:

- **Invariants** — properties of aggregate *state* that must always hold (layers 1, 5).
- **Preconditions / guards** — requirements on *operations* or *inputs* that must be met for a call to be valid (layers 2–4).

Understanding which layer is responsible for what prevents redundant checks and makes it clear where to add a new rule.

---

## Summary Table

| # | Layer | Rule type | Where | Timing | Consistency |
|---|-------|-----------|-------|--------|-------------|
| 1 | **Intra-invariant** | State / transition invariant | `verifyInvariants()` in aggregate | At UoW commit | Strong |
| 2 | **Operation precondition** | Operation precondition | Mutation methods (`remove()`, mutators) | At mutation | Strong |
| 3 | **Service-layer guard** | Guard | `*Functionalities.java` / `*Service.java`, before profile switch | Before aggregate mutation | Strong |
| 4 | **Functionality cross-aggregate state guard** | Guard | `*Functionality*.java`, saga step reading another aggregate under semantic lock | In workflow step | Strong |
| 5 | **Inter-invariant (event-driven)** | State invariant (eventual) | `getEventSubscriptions()` + event handler chain | Async (~1 s) | Eventual |

---

## Layer 1 — Intra-Invariant

**Definition:** A consistency rule that must always hold within a single aggregate instance. Checked on every Unit of Work commit, regardless of what operation caused the change.

**Where it lives:** `verifyInvariants()` in the base aggregate class (e.g., `Tournament.java`, `Execution.java`).

**When it runs:** Called by the UoW commit path after all mutations for a functionality have been applied but before the new version is persisted.

**Consistency:** Strong — the commit is aborted if any invariant fails.

**Examples:**
- `ANSWER_BEFORE_START` — `applications/.../tournament/aggregate/Tournament.java:verifyInvariants()` — a tournament cannot accept answers before its start time.
- `REMOVE_NO_STUDENTS` — `applications/.../execution/aggregate/Execution.java:verifyInvariants()` — a course execution cannot be removed if it still has enrolled students.

### Variant: transition invariant via mutation timestamp

Some invariants express a *transition* rule of the form "field X cannot change once condition Y is met", where Y involves wall-clock time. These cannot call `DateHandler.now()` directly inside `verifyInvariants()` because the check would be non-idempotent (the same aggregate state could pass at T1 and fail at T2), and in TCC the UoW calls `verifyInvariants()` a second time after a concurrent-version merge.

**Pattern:** the setter stamps the mutation time as a persistent field (`lastModifiedTime`) before applying the change. `verifyInvariants()` then compares `this.lastModifiedTime` against the threshold — never calling `DateHandler.now()` inside the invariant check itself.

Three rules apply when using this pattern:
1. **Setters stamp time:** each guarded setter calls `setLastModifiedTime(DateHandler.now())` before modifying the field.
2. **Copy constructor bypasses setters:** the copy constructor assigns guarded fields directly (e.g. `this.availableDate = other.getAvailableDate()`) so that copying an existing aggregate does not stamp a new `lastModifiedTime`. The copied `lastModifiedTime` is also assigned directly from `other`.
3. **`verifyInvariants()` reads the stored time:** the invariant method uses `this.lastModifiedTime`, not `DateHandler.now()`.

**Examples:**
- `QUESTIONS_FINAL_AFTER_AVAILABLE_DATE` / `AVAILABLE_DATE_FINAL_AFTER_AVAILABLE_DATE` / `CONCLUSION_DATE_FINAL_AFTER_AVAILABLE_DATE` / `RESULTS_DATE_FINAL_AFTER_AVAILABLE_DATE` — `applications/.../quiz/aggregate/Quiz.java:invariantFieldsFinalAfterAvailableDate()` — once `lastModifiedTime > prev.availableDate`, the question set and date fields must be identical to the previous version.
- `FINAL_AFTER_START` / `IS_CANCELED` — `applications/.../tournament/aggregate/Tournament.java:invariantFinalAfterStart()` and `invariantCancelledFieldsAreFinal()` — the same pattern for Tournament fields.

---

## Layer 2 — Operation Precondition

**Definition:** A check inside a mutation method that rejects a **logically invalid operation** — not an inconsistent aggregate state, but a call that makes no sense given the current data (e.g., removing an element that does not exist). These are preconditions on the *operation*, not invariants on the *state*.

**Where it lives:** Mutation methods directly on the aggregate class.

**When it runs:** At the moment the mutation is called, before any field is changed.

**Consistency:** Strong — an exception is thrown synchronously; the aggregate state is never dirtied.

**Distinction from Layer 1:** A Layer 1 invariant expresses a property of the aggregate's state that must always hold. A Layer 2 precondition expresses a requirement of the *caller*. The same rule cannot be expressed as a Layer 1 invariant because after the operation succeeds there is nothing wrong with the resulting state — the element simply is not there.

**Examples:**
- `Execution.removeStudent()` — throws `COURSE_EXECUTION_STUDENT_NOT_FOUND` if the student is not enrolled. Operation precondition: you can only remove a student that is actually enrolled.
- `QuizAnswer.addQuestionAnswer()` — throws `QUESTION_ALREADY_ANSWERED` if the question was already answered. Operation precondition: you can only answer a question once.

---

## Layer 3 — Service-Layer Guard

**Definition:** A guard that validates preconditions before the operation reaches any workflow or aggregate. Covers both pure input validation (no DB read required) and state preconditions that require a DB read. Lives in the `*Functionalities.java` coordinator (before the profile `switch`) or in `*Service.java`.

Input validation must **never** live inside `*FunctionalitySagas.java` or `*FunctionalityTCC.java`: placing it there forces duplication across profiles and mixes orchestration concerns with validation.

**Where it lives:** `*Functionalities.java` (before the `switch` on workflow type) for input validation; `*Service.java` for DB-backed preconditions.

**When it runs:** Before any UoW or aggregate is created; the operation is rejected before any workflow step executes.

**Consistency:** Strong — throws an exception that aborts the call before any aggregate is touched.

**Examples:**
- `USER_MISSING_NAME` — `applications/.../execution/coordination/functionalities/ExecutionFunctionalities.java` — rejects the call if the supplied name is null, before the Sagas/TCC switch.
- `DUPLICATE_COURSE_EXECUTION` — `applications/.../execution/service/ExecutionService.java` — rejects creation of a course execution that already exists.
- `INACTIVE_USER` — `applications/.../execution/service/ExecutionService.java` — blocks enrollment of an inactive user.

---

## Layer 4 — Functionality Cross-Aggregate State Guard

**Definition:** A workflow step that loads a *different* aggregate (not the operation's primary target) under a semantic lock (Sagas) or causal snapshot (TCC) to verify a cross-aggregate precondition before the mutating step is allowed to run.

**Where it lives:** `*FunctionalitySagas.java` / `*FunctionalityTCC.java` — a named step that issues a read command to another service.

**When it runs:** During workflow execution, after the semantic lock on the primary aggregate is acquired, before the mutating step.

**Consistency:** Strong — under Sagas, the read step acquires or respects semantic locks that prevent the inspected aggregate from being concurrently modified in a conflicting way. See [`sagas.md`](sagas.md) for how `forbiddenStates` work.

**Examples:**
- `CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT` — `applications/.../execution/coordination/sagas/RemoveCourseExecutionFunctionalitySagas.java` — a `getCourseStep` reads the `Course` aggregate to check `courseQuestionCount > 0`; if true, the removal step is rejected before any deletion occurs.
- `TOPIC_BELONGS_TO_QUESTION_COURSE` (`QUESTION_TOPIC_INVALID_COURSE`) — `applications/.../question/coordination/sagas/CreateQuestionFunctionalitySagas.java` — `getTopicsStep` reads each `Topic` aggregate under a semantic lock and verifies its `courseId` matches the target course before the question is created.

---

## Layer 5 — Inter-Invariant (Event-Driven)

**Definition:** A consistency rule that spans multiple aggregates and is maintained via domain events. The consumer aggregate caches a local copy of the relevant publisher state, kept eventually consistent by asynchronous event polling.

**Where it lives:** `getEventSubscriptions()` in the consumer aggregate, plus the event handler chain (`EventHandling` → `EventHandler` → `EventProcessing` → update functionality).

**When it runs:** Asynchronously, on a 1-second poll interval, after the publisher commits an event.

**Consistency:** Eventual — the consumer's cached state may lag behind the publisher by up to one poll interval.

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

### Going Deeper

For a worked example across multiple invariants:
→ [`docs/examples/tournament-inter-invariants.md`](../examples/tournament-inter-invariants.md)

For the underlying event wiring mechanics (event class, subscription, handler, polling):
→ `/new-event` skill

To scaffold a new inter-invariant:
→ `/inter-invariant <ConsumerAggregate> <condition>`

---

## Enforcement Lifecycle

Order of enforcement layers within a single operation:

```
Request arrives
      │
      ▼
[3] Service-layer guard
    (input validation or DB read — in *Functionalities.java before switch, or *Service.java)
      │
      ▼
[4] Functionality cross-aggregate state guard
    (saga step reads another aggregate under semantic lock)
      │
      ▼
[2] Operation precondition
    (mutation method checks caller precondition before applying change)
      │
      ▼
[1] Intra-invariant check
    (verifyInvariants() at UoW commit)
      │
      ▼
    commit
      │
      ▼  (async, ~1 s poll interval)
[5] Inter-invariant event handler
    (consumer caches publisher state update)
```

Layers 1–4 are synchronous and strong. Layer 5 is asynchronous and eventually consistent.
