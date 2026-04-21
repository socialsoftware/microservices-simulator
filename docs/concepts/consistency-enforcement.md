# Consistency Enforcement Taxonomy

## Quick Reference (AI Agent Entry Point)

For a new application, use this table to decide how each domain rule is enforced. For the full decision flowchart, see [`docs/concepts/decision-guide.md`](decision-guide.md).

| Rule type | Layer | Implemented by | Consistency |
|-----------|-------|---------------|-------------|
| Single-aggregate state rule (§3.1) | 1 | intra-invariant | Strong |
| Pre-mutation check (DB read or input validation), own service only | 2 | service guard | Strong |
| Synchronous check reading a different aggregate | 3 | saga step with `setForbiddenStates` | Strong |
| Cache state from another aggregate (no blocking) | 4 | inter-invariant | Eventual |

---

## Introduction

Consistency in this codebase is enforced at four distinct layers. Each layer has a different scope (single aggregate vs. cross-aggregate), timing (before mutation, at commit, or asynchronously), and consistency guarantee (strong or eventual). The layers cover two distinct kinds of rule:

- **Invariants** — properties of aggregate *state* that must always hold (layers 1, 4).
- **Guards** — requirements on *operations* that must be met for a call to be valid (layers 2–3).

Understanding which layer is responsible for what prevents redundant checks and makes it clear where to add a new rule. **When in doubt, prefer Layer 1:** if a rule can be expressed as a property of a single aggregate's state, it belongs in `verifyInvariants()` — not scattered across multiple layers.

---

## Summary Table

| # | Layer | Rule type | Where | Timing | Consistency |
|---|-------|-----------|-------|--------|-------------|
| 1 | **Intra-invariant** | State / transition invariant | `verifyInvariants()` in aggregate | At UoW commit | Strong |
| 2 | **Service-layer guard** | Guard (DB read or input validation) | `*Service.java`, before mutation | Inside workflow step, within `@Transactional` | Strong |
| 3 | **Functionality cross-aggregate state guard** | Guard | `*Functionality*.java`, saga step reading another aggregate under semantic lock | In workflow step | Strong |
| 4 | **Inter-invariant (event-driven)** | State invariant (eventual) | `getEventSubscriptions()` + event handler chain | Async (~1 s) | Eventual |

---

## Layer 1 — Intra-Invariant

**Definition:** A consistency rule that must always hold within a single aggregate instance. Checked every time an aggregate is changed by one of its services, regardless of what operation caused the change.

**Where it lives:** `verifyInvariants()` in the base aggregate class (e.g., `Tournament.java`, `Execution.java`).

**When it runs:** Called by the UoW commit path after all mutations for a functionality have been applied but before the new version is persisted.

**Consistency:** Strong — the commit is aborted if any invariant fails.

**Centralization principle:** Layer 1 is the canonical home for aggregate-state invariants. Whenever a rule can be expressed purely in terms of a single aggregate's state — regardless of which operation triggered the change — it belongs here. Because `verifyInvariants()` runs on every commit, a Layer 1 check fires uniformly across all operations, eliminating the need to repeat the same logic in individual service guards (Layer 2) or workflow steps (Layer 3). **If a rule fits Layer 1, define it only there — do not add the same check at another layer.**

**Exception pattern:** Each invariant has its own `if` block and throws the most descriptive specific exception available (e.g., `COURSE_MISSING_NAME`). Use `INVARIANT_BREAK` only as a last resort if no domain-specific constant fits.

> **Restriction:** Mutation methods on aggregate classes (`add()`, `remove()`, setters) must **not** throw domain exceptions. All state-consistency rules must be placed in `verifyInvariants()`. Throwing from a mutation method bypasses the centralized invariant check and breaks the UoW commit contract — the UoW may partially apply mutations before the exception fires, leaving the aggregate in an inconsistent in-memory state.

**Examples:**
- `ANSWER_BEFORE_START` — `applications/.../tournament/aggregate/Tournament.java:verifyInvariants()` — a tournament cannot accept answers before its start time.
- `REMOVE_NO_STUDENTS` — `applications/.../execution/aggregate/Execution.java:verifyInvariants()` — a course execution cannot be removed if it still has enrolled students.
- `CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT` — `applications/.../course/aggregate/Course.java:verifyInvariants()` — `courseExecutionCount` cannot reach 0 while `courseQuestionCount > 0`; both counters live inside `Course`, so the rule is purely intra. Fires when `updateCourseExecutionCount` commits; the saga places this step **before** the removal step so that if the invariant fires the execution has not yet been deleted and compensation can cleanly release its semantic lock.
- `COURSE_SAME_TOPIC_COURSE` — `applications/.../question/aggregate/Question.java:verifyInvariants()` — every `QuestionTopic.courseAggregateId` (snapshotted from `TopicDto.getCourseId()` at construction) must equal `questionCourse.getCourseAggregateId()`; all data lives inside `Question`, so the rule is purely intra. Fires when the question aggregate is first committed.

### Variant: transition invariant via mutation timestamp

Some invariants express a *transition* rule of the form "field X cannot change once condition Y is met", where Y involves wall-clock time. These cannot call `DateHandler.now()` directly inside `verifyInvariants()` because the check would be non-idempotent (the same aggregate state could pass at T1 and fail at T2).

**Pattern:** the setter stamps the mutation time as a persistent field (`lastModifiedTime`) before applying the change. `verifyInvariants()` then compares `this.lastModifiedTime` against the threshold — never calling `DateHandler.now()` inside the invariant check itself.

Three rules apply when using this pattern:
1. **Setters stamp time:** each guarded setter calls `setLastModifiedTime(DateHandler.now())` before modifying the field.
2. **Copy constructor bypasses setters:** the copy constructor assigns guarded fields directly (e.g. `this.availableDate = other.getAvailableDate()`) so that copying an existing aggregate does not stamp a new `lastModifiedTime`. The copied `lastModifiedTime` is also assigned directly from `other`.
3. **`verifyInvariants()` reads the stored time:** the invariant method uses `this.lastModifiedTime`, not `DateHandler.now()`.

**Examples:**
- `QUESTIONS_FINAL_AFTER_AVAILABLE_DATE` / `AVAILABLE_DATE_FINAL_AFTER_AVAILABLE_DATE` / `CONCLUSION_DATE_FINAL_AFTER_AVAILABLE_DATE` / `RESULTS_DATE_FINAL_AFTER_AVAILABLE_DATE` — `applications/.../quiz/aggregate/Quiz.java:invariantFieldsFinalAfterAvailableDate()` — once `lastModifiedTime > prev.availableDate`, the question set and date fields must be identical to the previous version.
- `FINAL_AFTER_START` / `IS_CANCELED` — `applications/.../tournament/aggregate/Tournament.java:invariantFinalAfterStart()` and `invariantCancelledFieldsAreFinal()` — the same pattern for Tournament fields.

---

## Layer 2 — Service-Layer Guard

**Definition:** A guard that runs inside `*Service.java` before any aggregate is mutated. Covers both (a) checks that require a DB read — uniqueness constraints, state checks — and (b) pure input validation (null/blank field checks, structural DTO validation). Both kinds live in the service rather than at the Functionality layer, because the service's `@Transactional(SERIALIZABLE)` boundary makes the check race-free and keeps all pre-mutation logic in one place.

**Where it lives:** `*Service.java`, at the start of the relevant method, before any aggregate mutation.

**When it runs:** Inside the first workflow step, within the `@Transactional(SERIALIZABLE)` boundary of the `*Service.java` method, before any mutation.

**Consistency:** Strong — throws an exception that aborts the workflow before any aggregate is dirtied.

### When to use Layer 2 for uniqueness rules

A **local uniqueness constraint** on a table that one service owns exclusively belongs here, not at Layer 3 or 4. The key question is: *who owns the data being checked?*

- Same service → Layer 2 repository read in `*Service.java` (local, transactionally atomic).
- Different service → Layer 3 workflow step (under semantic lock) to read the remote aggregate.

Placing a uniqueness check at Layer 4 (event cache) when the authoritative data is local is a mistake: the cache lags behind reality and the "strong" Layer 4 guard becomes eventually consistent in practice, allowing duplicate inserts in a narrow race window.

**Examples:**
- `INACTIVE_USER` — `applications/.../execution/service/ExecutionService.java` — blocks enrollment of an inactive user.
- `DUPLICATE_COURSE_EXECUTION` — `applications/.../execution/service/ExecutionService.java` — rejects creation of a course execution that already exists.
- `QUIZ_ALREADY_STARTED_BY_STUDENT` — `applications/.../answer/service/QuizAnswerService.java` — rejects a second `startQuiz()` call for the same (student, quiz) pair via `quizAnswerRepository.existsByQuizIdAndStudentId(...)`.

---

## Layer 3 — Functionality Cross-Aggregate State Guard

**Definition:** A workflow step that loads a *different* aggregate (not the operation's primary target) under a semantic lock (Sagas) to verify a cross-aggregate precondition before the mutating step is allowed to run.

**Where it lives:** `*FunctionalitySagas.java` / `*FunctionalityTCC.java` — a named step that issues a read command to another service.

**When it runs:** During workflow execution, after the semantic lock on the primary aggregate is acquired, before the mutating step.

**Consistency:** Strong — under Sagas, the read step acquires or respects semantic locks that prevent the inspected aggregate from being concurrently modified in a conflicting way. See [`sagas.md`](sagas.md) for how `forbiddenStates` work.

**Examples:**

There are currently no Layer 3 business-rule checks in this codebase. Both rules that previously lived here (`CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT` and `COURSE_SAME_TOPIC_COURSE`) were migrated to Layer 1 once it was recognised that all data they inspect belongs to a single aggregate. Layer 3 read steps that remain (e.g. `getTopicsStep` in `CreateQuestionFunctionalitySagas`) exist to fetch data needed to build the aggregate, not to enforce a cross-aggregate guard.

---

## Layer 4 — Inter-Invariant (Event-Driven)

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

---

## Enforcement Lifecycle

Order of enforcement layers within a single operation:

```
Request arrives
      │
      ▼
[2] Service-layer guard
    (in *Service.java — input validation + DB checks, inside @Transactional(SERIALIZABLE))
      │
      ▼
[3] Functionality cross-aggregate state guard
    (saga step reads another aggregate under semantic lock)
      │
      ▼
[1] Intra-invariant check
    (verifyInvariants() at UoW commit)
      │
      ▼
    commit
      │
      ▼  (async, ~1 s poll interval)
[4] Inter-invariant event handler
    (consumer caches publisher state update)
```

Layers 1–3 are synchronous and strong. Layer 4 is asynchronous and eventually consistent.
