# CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT

**Type:** Inter-invariant
**Enforced in:** `ExecutionService.removeCourseExecution()`
**Rule:** A `CourseExecution` cannot be deleted if it is the **sole remaining execution** for its course and the course still has questions (`courseQuestionCount > 0`).

---

## Data Flow

```
QuestionService                 Execution aggregate
     │                               │
     │  createQuestion()             │
     │──► registers CreateQuestionEvent ──────────────────────────────────────►│
     │                               │  CourseExecutionEventHandling (1s poll) │
     │  removeQuestion()             │  handleCreateQuestionEvents()           │
     │──► registers DeleteQuestionEvent ─────────────────────────────────────►│
                                     │  → CreateQuestionEventHandler           │
                                     │  → ExecutionEventProcessing             │
                                     │  → ExecutionFunctionalities             │
                                     │  → UpdateCourseQuestionCountCommand     │
                                     │  → ExecutionService.increment/decrement │
                                     │  → Execution.courseQuestionCount ±1     │
```

---

## Key Files

### Events (published by `QuestionService`)

| File | Role |
|------|------|
| `events/CreateQuestionEvent.java` | Emitted on question creation; carries `courseAggregateId`. `publisherAggregateId = courseAggregateId` (intentional — required for causal TCC consistency tracking). |
| `events/DeleteQuestionEvent.java` | Emitted on question deletion; same structure. |

**Emitted in** `microservices/question/service/QuestionService.java`:
```java
// createQuestion()
unitOfWorkService.registerEvent(new CreateQuestionEvent(question.getAggregateId(), courseAggregateId), unitOfWork);

// removeQuestion()
unitOfWorkService.registerEvent(new DeleteQuestionEvent(question.getAggregateId(), courseAggregateId), unitOfWork);
```

---

### Execution Aggregate — `microservices/execution/aggregate/Execution.java`

**Field:**
```java
private int courseQuestionCount = 0;  // eventually consistent; updated via events
```

**Copied** in the copy-constructor: `this.courseQuestionCount = other.getCourseQuestionCount();`

**Event subscriptions** (active only when `ACTIVE`):
```java
private void interInvariantCourseHasNoContent(Set<EventSubscription> eventSubscriptions) {
    eventSubscriptions.add(new ExecutionSubscribesCreateQuestion(this.courseExecutionCourse));
    eventSubscriptions.add(new ExecutionSubscribesDeleteQuestion(this.courseExecutionCourse));
}
```

**`remove()`** — enforces student guard only; content guard delegated to service:
```java
@Override
public void remove() {
    if (!getStudents().isEmpty()) {
        throw new QuizzesException(CANNOT_DELETE_COURSE_EXECUTION, getAggregateId());
    }
    // CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT checked in ExecutionService
    super.remove();
}
```

---

### Event Subscriptions

| File | Filters on |
|------|-----------|
| `execution/events/subscribe/ExecutionSubscribesCreateQuestion.java` | `event.getCourseAggregateId() == this.courseAggregateId` |
| `execution/events/subscribe/ExecutionSubscribesDeleteQuestion.java` | `event.getCourseAggregateId() == this.courseAggregateId` |

Both constructed with `super(courseAggregateId, courseVersion, eventType)`.

---

### Event Polling — `execution/events/handling/CourseExecutionEventHandling.java`

```java
@Scheduled(fixedDelay = 1000)
public void handleCreateQuestionEvents() {
    eventApplicationService.handleSubscribedEvent(CreateQuestionEvent.class,
            new CreateQuestionEventHandler(courseExecutionRepository, executionEventProcessing));
}

@Scheduled(fixedDelay = 1000)
public void handleDeleteQuestionEvents() {
    eventApplicationService.handleSubscribedEvent(DeleteQuestionEvent.class,
            new DeleteQuestionEventHandler(courseExecutionRepository, executionEventProcessing));
}
```

Handlers call `ExecutionEventProcessing.processCreateQuestionEvent()` / `processDeleteQuestionEvent()`, which delegate to `ExecutionFunctionalities.incrementCourseQuestionCount()` / `decrementCourseQuestionCount()`.

---

### Count Update Workflow

**Command:** `command/execution/UpdateCourseQuestionCountCommand.java`
Fields: `courseExecutionAggregateId`, `increment` (boolean).

**Functionalities (one per protocol):**
- `execution/coordination/sagas/UpdateCourseQuestionCountFunctionalitySagas.java`
- `execution/coordination/causal/UpdateCourseQuestionCountFunctionalityTCC.java`

Both build a single step that sends `UpdateCourseQuestionCountCommand` to `ExecutionService`.

**Service** — `execution/service/ExecutionService.java`:
```java
public void incrementCourseQuestionCount(Integer executionAggregateId, UnitOfWork unitOfWork) {
    Execution old = load(executionAggregateId);
    Execution next = copy(old);
    next.setCourseQuestionCount(next.getCourseQuestionCount() + 1);
    registerChanged(next);
}

public void decrementCourseQuestionCount(Integer executionAggregateId, UnitOfWork unitOfWork) {
    Execution old = load(executionAggregateId);
    Execution next = copy(old);
    next.setCourseQuestionCount(Math.max(0, next.getCourseQuestionCount() - 1));
    registerChanged(next);
}
```

---

### Invariant Enforcement — `ExecutionService.removeCourseExecution()`

```java
int numberOfExecutionsOfCourse = Math.toIntExact(
    getAllCourseExecutions(unitOfWork).stream()
        .filter(ce -> ce.getCourseAggregateId()
                .equals(newExecution.getExecutionCourse().getCourseAggregateId()))
        .count());

if (numberOfExecutionsOfCourse == 1 && newExecution.getCourseQuestionCount() > 0) {
    throw new QuizzesException(CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT, executionAggregateId);
}

newExecution.remove();
```

`getAllCourseExecutions()` includes the execution being deleted (not yet removed), so `count == 1` means it is the sole remaining execution.

---

## Consistency Note

`courseQuestionCount` is **eventually consistent**: the count is updated asynchronously via event polling (1 s interval). A deletion may succeed transiently if the count has not yet been updated. This is a known trade-off of the event-driven design.

---

## Tests — `sagas/coordination/execution/RemoveCourseExecutionTest.groovy`

| Test | Outcome |
|------|---------|
| Sole execution, no content (`courseQuestionCount = 0`) | ✓ Removed |
| Sole execution, has content (`courseQuestionCount > 0`) | ✗ `QuizzesException` thrown |
| Non-last execution, has content (two executions, same course) | ✓ Removed (guard does not apply) |

---

## Implementation Notes

- `CreateQuestionEvent.publisherAggregateId = courseAggregateId` is **intentional**: the causal (TCC) consistency framework uses `publisherAggregateId` for version-tracking and snapshot filtering. Using `questionAggregateId` instead breaks TCC tests because each `CausalExecution` would then need to have processed every individual question's event before it could be causally read alongside a Question aggregate.
- The `subscribedAggregateId` in both subscriptions is also `courseAggregateId`, so `subscribedVersion = courseVersion` anchors the "last processed" version correctly.
