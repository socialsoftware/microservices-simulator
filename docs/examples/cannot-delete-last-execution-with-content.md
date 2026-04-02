# CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT

**Type:** Inter-invariant
**Enforced in:** `RemoveCourseExecutionFunctionalitySagas` / `RemoveCourseExecutionFunctionalityTCC`
**Rule:** A `CourseExecution` cannot be deleted if it is the **sole remaining execution** for its course and the course still has questions (`courseQuestionCount > 0`).

---

## Data Flow

```
CreateQuestionFunctionality          Course aggregate
     │                                    │
     │  createQuestion()                  │
     │──► UpdateCourseQuestionCountCommand ──► Course.courseQuestionCount + 1
     │
     │  removeQuestion()
     │──► UpdateCourseQuestionCountCommand ──► Course.courseQuestionCount - 1

CreateCourseExecutionFunctionality   Course aggregate
     │                                    │
     │  createCourseExecution()           │
     │──► UpdateCourseExecutionCountCommand ──► Course.courseExecutionCount + 1
     │
RemoveCourseExecutionFunctionality   Course aggregate
     │                                    │
     │  removeCourseExecution()           │
     │──► getCourseStep: read Course, check invariant
     │──► removeCourseExecutionStep
     │──► UpdateCourseExecutionCountCommand ──► Course.courseExecutionCount - 1
```

---

## Key Files

### Course Aggregate — `microservices/course/aggregate/Course.java`

**Fields:**
```java
private int courseQuestionCount = 0;   // incremented when a question is created
private int courseExecutionCount = 0;  // incremented when an execution is created
```

Both are copied in the copy-constructor and serialised to `CourseDto`.

---

### Count Update Commands

| Command | Fields |
|---------|--------|
| `command/course/UpdateCourseQuestionCountCommand.java` | `courseAggregateId`, `increment` (boolean) |
| `command/course/UpdateCourseExecutionCountCommand.java` | `courseAggregateId`, `increment` (boolean) |

Routed through `CourseCommandHandler` to `CourseService.increment/decrementCourseQuestionCount()` and `increment/decrementCourseExecutionCount()`.

---

### Question Functionalities

**`CreateQuestionFunctionalitySagas`** — adds `updateCourseQuestionCountStep` after `createQuestionStep`:
```java
SagaStep updateCourseQuestionCountStep = new SagaStep("updateCourseQuestionCountStep", () -> {
    commandGateway.send(new UpdateCourseQuestionCountCommand(unitOfWork, COURSE, courseDto.getAggregateId(), true));
}, List.of(createQuestionStep));
```

**`RemoveQuestionFunctionalitySagas`** — stores `courseAggregateId` from the question DTO in `getQuestionStep`, then adds `updateCourseQuestionCountStep` (decrement) after `removeQuestionStep`.

TCC variants (`CreateQuestionFunctionalityTCC`, `RemoveQuestionFunctionalityTCC`) perform the same update inline in their single step.

---

### Execution Functionalities

**`CreateCourseExecutionFunctionalitySagas`** — adds `updateCourseExecutionCountStep` after `createCourseExecutionStep`:
```java
SagaStep updateCourseExecutionCountStep = new SagaStep("updateCourseExecutionCountStep", () -> {
    commandGateway.send(new UpdateCourseExecutionCountCommand(unitOfWork, COURSE, courseExecutionDto.getCourseAggregateId(), true));
}, List.of(createCourseExecutionStep));
```

---

### Invariant Enforcement — `RemoveCourseExecutionFunctionalitySagas`

The workflow adds a `getCourseStep` that reads Course under `CourseSagaState.READ_COURSE` and checks:

```java
if (courseDto.getCourseQuestionCount() > 0 && courseDto.getCourseExecutionCount() == 1) {
    throw new QuizzesException(CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT, executionAggregateId);
}
```

Then `updateCourseExecutionCountStep` decrements `Course.courseExecutionCount` after the execution is removed.

The TCC variant (`RemoveCourseExecutionFunctionalityTCC`) performs the same read + invariant check + decrement inline in its single step.

---

## Consistency Properties

`courseQuestionCount` and `courseExecutionCount` are **strongly consistent**: they are updated as explicit saga/TCC steps in the same workflow that creates or deletes the question/execution. There is no eventual consistency lag.

The invariant check in `RemoveCourseExecution` reads the `Course` aggregate directly (under a semantic lock in sagas, within the same UoW in TCC), so the check reflects the current committed state.

---

## Tests — `sagas/coordination/execution/RemoveCourseExecutionTest.groovy`

| Test | Outcome |
|------|---------|
| Sole execution, course has no questions | ✓ Removed |
| Sole execution, course has a question (via real `createQuestion`) | ✗ `QuizzesException` thrown |
| Non-last execution (two executions), course has a question | ✓ Removed (guard does not apply) |

---

## Design Alternatives for the Execution Count Check

Five alternatives were considered:

---

### Invariant vs. Pre-condition

Before comparing options it is worth clarifying what `CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT` actually is:

- The **invariant** is the system-level property that must always hold:
  ```
  for all c in Course:
    courseQuestionCount(c) > 0 =>
      # {e in Execution: e.course = c && e.state != DELETED} >= 1
  ```
- `CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT` is the **pre-condition** on the delete operation that enforces it — it checks whether proceeding would violate the invariant. The name describes what cannot be done, not what must always be true.

---

### Option A1 — Targeted repository query (in the service)

Replace `getAllCourseExecutions()` with a `COUNT` query filtered by `courseAggregateId`:

```java
int count = courseExecutionCustomRepository.countNonDeletedByCourseAggregateId(
    newExecution.getExecutionCourse().getCourseAggregateId());
```

The predicate lock is scoped to the rows of that specific course only. No architectural changes needed.

| Consistency | Lock scope | Complexity |
|-------------|------------|------------|
| Strong | Per-course rows | Low |

---

### Option A2 — Targeted query as an explicit saga step (in the workflow)

Functionally identical to Option A (same targeted `COUNT` query, same lock scope), but the check is moved out of `ExecutionService` and into `RemoveCourseExecutionFunctionalitySagas` as a named step. The service method is cleaner and the invariant check is visible in the workflow graph.

| Consistency | Lock scope | Complexity |
|-------------|------------|------------|
| Strong | Per-course rows | Low |

---

### Option B — Cache `courseExecutionCount` on `Execution` (inter-invariant)

Each `Execution` caches a `courseExecutionCount` field: the number of non-deleted sibling executions for the same course. Maintained reactively via events when executions are created or deleted (analogous to `courseQuestionCount`). The pre-condition becomes a purely local check with no DB query at delete time.

| Consistency | Lock scope | Complexity |
|-------------|------------|------------|
| Eventual (~1 s lag) | None | Medium |

**Trade-off:** the cached count may lag, meaning the guard could momentarily allow or block incorrectly in a short window.

---

### Option C — Cache both counters on `Course` (revised design — **implemented**)

Both `courseQuestionCount` and `courseExecutionCount` live on `Course`, which is the natural semantic owner. The saga workflow for `RemoveCourseExecution` adds a step to read `Course` (under a semantic lock) and check both counters atomically.

The upstream/downstream rule applies only to **event subscriptions** (consumers cache state from publishers). In sagas, the workflow coordinator can send commands to any aggregate in any direction — that is the saga pattern. `Course` holds state derived from sagas that coordinate across it, not from events emitted by downstream aggregates.

| Consistency | Lock scope | Complexity |
|-------------|------------|------------|
| Strong | Course aggregate (semantic lock) | Medium |

**Benefits:**
- Eliminates eventual consistency for the invariant guard
- Removes event polling infrastructure for `courseQuestionCount`
- Invariant check is a named, visible saga step
- No predicate lock on the `CourseExecution` table

---

### Summary

| Option     | Where | Consistency | Lock scope | Notes                              |
|------------|-------|-------------|------------|------------------------------------|
| **Old** | `ExecutionService` | Strong | Whole CE table | Big predicate lock; EC for questionCount |
| **A1**     | `ExecutionService` | Strong | Per-course rows | Minimal change                     |
| **A2**     | Saga workflow step | Strong | Per-course rows | Same as A1, better placement       |
| **B**      | `Execution` inter-invariant | Eventual | None | Fully local, eventually consistent |
| **C (current)** | `Course` aggregate + saga step | Strong | Course semantic lock | Implemented |
