# Consistency Enforcement Taxonomy

## Introduction

Consistency in this codebase is enforced at six distinct layers. Each layer has a different scope (single aggregate vs. cross-aggregate), timing (before mutation, at commit, or asynchronously), and consistency guarantee (strong or eventual). The layers cover two distinct kinds of rule:

- **Invariants** — properties of aggregate *state* that must always hold (layers 1, 6).
- **Preconditions / guards** — requirements on *operations* or *inputs* that must be met for a call to be valid (layers 2–5).

Understanding which layer is responsible for what prevents redundant checks and makes it clear where to add a new rule.

---

## Summary Table

| # | Layer | Rule type | Where | Timing | Consistency |
|---|-------|-----------|-------|--------|-------------|
| 1 | **Intra-invariant** | State / transition invariant | `verifyInvariants()` in aggregate | At UoW commit | Strong |
| 2 | **Operation precondition** | Operation precondition | Mutation methods (`remove()`, mutators) | At mutation | Strong |
| 3 | **Service-layer guard** | Guard (DB read) | `*Service.java`, before mutation | Inside workflow step, within `@Transactional` | Strong |
| 4 | **Input validation guard** | Guard (no DB) | `checkInput()` in `*Functionalities.java`, before `createUnitOfWork()` | Before UoW is created | Strong |
| 5 | **Functionality cross-aggregate state guard** | Guard | `*Functionality*.java`, saga step reading another aggregate under semantic lock | In workflow step | Strong |
| 6 | **Inter-invariant (event-driven)** | State invariant (eventual) | `getEventSubscriptions()` + event handler chain | Async (~1 s) | Eventual |

---

## Layer 1 — Intra-Invariant

**Definition:** A consistency rule that must always hold within a single aggregate instance. Checked every time an aggregate is changed by one of its services, regardless of what operation caused the change.

**Where it lives:** `verifyInvariants()` in the base aggregate class (e.g., `Tournament.java`, `Execution.java`).

**When it runs:** Called by the UoW commit path after all mutations for a functionality have been applied but before the new version is persisted.

**Consistency:** Strong — the commit is aborted if any invariant fails.

**Examples:**
- `ANSWER_BEFORE_START` — `applications/.../tournament/aggregate/Tournament.java:verifyInvariants()` — a tournament cannot accept answers before its start time.
- `REMOVE_NO_STUDENTS` — `applications/.../execution/aggregate/Execution.java:verifyInvariants()` — a course execution cannot be removed if it still has enrolled students.
- `CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT` — `applications/.../course/aggregate/Course.java:verifyInvariants()` — `courseExecutionCount` cannot reach 0 while `courseQuestionCount > 0`; both counters live inside `Course`, so the rule is purely intra. Fires when `updateCourseExecutionCount` commits; the saga places this step **before** the removal step so that if the invariant fires the execution has not yet been deleted and compensation can cleanly release its semantic lock.
- `COURSE_SAME_TOPIC_COURSE` — `applications/.../question/aggregate/Question.java:verifyInvariants()` — every `QuestionTopic.courseAggregateId` (snapshotted from `TopicDto.getCourseId()` at construction) must equal `questionCourse.getCourseAggregateId()`; all data lives inside `Question`, so the rule is purely intra. Fires when the question aggregate is first committed.

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

**Definition:** A guard that requires a DB read — uniqueness constraints and state checks. Lives in `*Service.java`, inside the `@Transactional(SERIALIZABLE)` boundary, making the read and any subsequent write serializable and race-free.

**Where it lives:** `*Service.java`, at the start of the relevant method, before any aggregate mutation.

**When it runs:** Inside the first workflow step, within the `@Transactional(SERIALIZABLE)` boundary of the `*Service.java` method, before any mutation.

**Consistency:** Strong — throws an exception that aborts the workflow before any aggregate is dirtied.

### When to use Layer 3 for uniqueness rules

A **local uniqueness constraint** on a table that one service owns exclusively belongs here, not at Layer 5 or 6. The key question is: *who owns the data being checked?*

- Same service → Layer 3 repository read in `*Service.java` (local, transactionally atomic).
- Different service → Layer 5 workflow step (under semantic lock) to read the remote aggregate.

Placing a uniqueness check at Layer 6 (event cache) when the authoritative data is local is a mistake: the cache lags behind reality and the "strong" Layer 6 guard becomes eventually consistent in practice, allowing duplicate inserts in a narrow race window. See [`docs/examples/unique-quiz-answer-per-student.md`](../examples/unique-quiz-answer-per-student.md) for a full case study of this failure mode and the Layer 3 fix.

**Examples:**
- `INACTIVE_USER` — `applications/.../execution/service/ExecutionService.java` — blocks enrollment of an inactive user.
- `DUPLICATE_COURSE_EXECUTION` — `applications/.../execution/service/ExecutionService.java` — rejects creation of a course execution that already exists.
- `QUIZ_ALREADY_STARTED_BY_STUDENT` — `applications/.../answer/service/QuizAnswerService.java` — rejects a second `startQuiz()` call for the same (student, quiz) pair via `quizAnswerRepository.existsByQuizIdAndStudentId(...)`.

---

## Layer 4 — Input Validation Guard

**Definition:** A guard that validates pure input before any workflow infrastructure is created. Covers null/blank field checks and structural DTO validation that requires no DB read.

**Where it lives:** `checkInput()` in `*Functionalities.java`, called **before `createUnitOfWork()`**.

**When it runs:** Before `createUnitOfWork()` is called — the operation is rejected before any version slot is consumed or workflow step executes.

**Consistency:** Strong — throws an exception immediately; no UoW, no transaction, no aggregate is touched.

**Distinction from Layer 3:** Layer 3 runs inside a workflow step within a `@Transactional` boundary and requires a DB read. Layer 4 fires before the workflow system is involved at all. Input validation must **never** live inside `*FunctionalitySagas.java` or `*FunctionalityTCC.java` (forces profile duplication), and must not be placed after `createUnitOfWork()` (wastes a version-counter slot for every rejected call).

**Examples:**
- `TOPIC_MISSING_NAME` — `applications/.../topic/coordination/functionalities/TopicFunctionalities.java` — rejects the call if the supplied name is null.
- `USER_MISSING_NAME` — `applications/.../user/coordination/functionalities/UserFunctionalities.java` — rejects the call if the supplied name is null.
- Tournament null-field guards — `applications/.../tournament/coordination/functionalities/TournamentFunctionalities.java` — rejects `createTournament()` if userId, topicsId, or core date fields are null.

---

## Layer 5 — Functionality Cross-Aggregate State Guard

**Definition:** A workflow step that loads a *different* aggregate (not the operation's primary target) under a semantic lock (Sagas) or causal snapshot (TCC) to verify a cross-aggregate precondition before the mutating step is allowed to run.

**Where it lives:** `*FunctionalitySagas.java` / `*FunctionalityTCC.java` — a named step that issues a read command to another service.

**When it runs:** During workflow execution, after the semantic lock on the primary aggregate is acquired, before the mutating step.

**Consistency:** Strong — under Sagas, the read step acquires or respects semantic locks that prevent the inspected aggregate from being concurrently modified in a conflicting way. See [`sagas.md`](sagas.md) for how `forbiddenStates` work.

**Examples:**

There are currently no Layer 5 business-rule checks in this codebase. Both rules that previously lived here (`CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT` and `COURSE_SAME_TOPIC_COURSE`) were migrated to Layer 1 once it was recognised that all data they inspect belongs to a single aggregate. Layer 5 read steps that remain (e.g. `getTopicsStep` in `CreateQuestionFunctionalitySagas`) exist to fetch data needed to build the aggregate, not to enforce a cross-aggregate guard.

---

## Layer 6 — Inter-Invariant (Event-Driven)

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
[4] Input validation guard
    (checkInput() in *Functionalities.java — BEFORE createUnitOfWork())
      │
      ▼
 createUnitOfWork() / profile switch
      │
      ▼
[3] Service-layer guard
    (in *Service.java — before mutation, inside @Transactional(SERIALIZABLE))
      │
      ▼
[5] Functionality cross-aggregate state guard
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
[6] Inter-invariant event handler
    (consumer caches publisher state update)
```

Layers 1–5 are synchronous and strong. Layer 6 is asynchronous and eventually consistent.
