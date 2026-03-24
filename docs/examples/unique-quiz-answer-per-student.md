# UNIQUE_QUIZ_ANSWER_PER_STUDENT

**Type:** Cross-aggregate uniqueness rule
**Rule:** A student can only create one `QuizAnswer` per quiz. A second `startQuiz()` call for the same (student, quiz) pair must throw `QUIZ_ALREADY_STARTED_BY_STUDENT`.

```
for all q in Quiz:
  for all a in QuizAnswer where a.quiz = q:
    count(a where a.student = s) <= 1
```

---

## Current Implementation (Option A — Layer 3 Service Guard)

The rule is enforced via a **synchronous repository check** in `QuizAnswerService.startQuiz()`, before any aggregate is created:

```java
// UNIQUE_QUIZ_ANSWER_PER_STUDENT (Layer 3 guard)
if (quizAnswerRepository.existsByQuizIdAndStudentId(quizAggregateId, userDto.getAggregateId())) {
    throw new QuizzesException(QuizzesErrorMessage.QUIZ_ALREADY_STARTED_BY_STUDENT,
            userDto.getAggregateId(), quizAggregateId);
}
```

`QuizAnswerService` is the authoritative owner of the `QuizAnswer` table. The check reads from the same DB transaction, so there is no staleness window and no cross-service coordination is needed.

### Data Flow

```
StartQuizFunctionality (Answer service)     QuizAnswerService
        │                                        │
        │  startQuiz()                           │
        │──────────────────────────────────────► │
        │                     existsByQuizIdAndStudentId() → false
        │                     (ok, proceed — create QuizAnswer)
        │                                        │
        │  [concurrent 2nd call, same student]   │
        │──────────────────────────────────────► │
        │                     existsByQuizIdAndStudentId() → true
        │──────────────────────────────────────► throws QUIZ_ALREADY_STARTED_BY_STUDENT
```

---

## Previous Implementation (Broken — removed)

The rule was previously enforced via **two coupled layers**:

- **Layer 6 (inter-invariant):** The `Quiz` aggregate subscribed to `CreateQuizAnswerEvent` and cached a local `studentsWithAnswers: Set<Integer>`, updated by the async event poll (~1 s interval).
- **Layer 5 (cross-aggregate state guard):** `StartQuizFunctionalitySagas` had a `checkUniqueAnswerStep` that sent `AssertStudentHasNoAnswerCommand` to the Quiz service. `QuizService.assertStudentHasNoAnswer()` loaded the Quiz aggregate and checked `quiz.hasStudentWithAnswer(studentId)`.

### Why it was broken

**Layer 5 was backed by eventually-consistent state, so its strong-consistency guarantee did not hold.**

Layer 5 guards are supposed to be strongly consistent: a named workflow step reads another aggregate under a semantic lock, blocking concurrent conflicting mutations. But the data being read (`studentsWithAnswers`) was a Layer 6 cache that lagged behind reality by up to one poll interval.

The race:

```
t=0   startQuiz() #1 → guard passes (cache empty) → QuizAnswer created → event registered
t=0   startQuiz() #2 → guard passes (cache still empty, event not yet processed) → second QuizAnswer created ✗
t=1s  event poll runs → cache updated → invariant already violated
```

Additionally, the Layer 6 infrastructure (event class, subscription, handler, event polling, event processing, update functionality) existed solely to maintain a cache of data that the `QuizAnswer` service already owned natively. The complexity was not justified.

---

## Why Option A Is the Right Choice for This Rule

This invariant is a **local uniqueness constraint on a table that one service owns exclusively**. The `QuizAnswer` service is the sole writer of the `QuizAnswer` table — no other service can create a `QuizAnswer`. That means:

| Property | Consequence |
|---|---|
| Single owner | No cross-service read is needed; the check is purely local |
| Same DB transaction | The read and write are atomic — no staleness window |
| No semantic lock needed | There is no other aggregate to lock; the guard lives in the authoritative service |

Layer 3 is correct here precisely because the uniqueness constraint lives entirely within the authority of one service. A Layer 4 or Layer 6 approach only makes sense when the state you need to read belongs to a *different* service — which is not the case here.

### Design alternatives considered

| Option | Layer | Where | Consistency | Complexity | Decision |
|--------|-------|-------|-------------|------------|----------|
| **A — repository check (chosen)** | L3 | `QuizAnswerService.startQuiz()` | Strong | Low | Eliminates all L5 + L6 wiring |
| **B — DB unique constraint** | DB | `QuizAnswer` table | Strongest | Low (additive) | Useful as a safety net on top of A |
| **C — saga step on own repository** | L5 | `QuizAnswerService` command | Strong | Medium | Keeps the step visible in workflow graph, but adds a dispatch hop for a check the service can do locally |
| **Previous — saga step on Quiz cache** | L5 (backed by L6) | `QuizService` + `Quiz` aggregate | Eventually consistent | High | **Broken** — wrong layer for the data source |

Option B (DB unique constraint) can be added on top of A as a last-resort safety net; it does not replace A since a raw `DataIntegrityViolationException` is not a domain exception.
