# UNIQUE_QUIZ_ANSWER_PER_STUDENT

**Type:** Cross-aggregate uniqueness rule
**Rule:** A student can only create one `QuizAnswer` per quiz. A second `startQuiz()` call for the same (student, quiz) pair must throw `QUIZ_ALREADY_STARTED_BY_STUDENT`.

```
for all q in Quiz:
  for all a in QuizAnswer where a.quiz = q:
    count(a where a.student = s) <= 1
```

---

## Current Implementation

The rule is enforced via **two coupled layers**:

- **Layer 6 (inter-invariant):** The `Quiz` aggregate subscribes to `CreateQuizAnswerEvent` and caches a local `studentsWithAnswers: Set<Integer>`, updated by the async event poll (~1 s interval).
- **Layer 5 (cross-aggregate state guard):** `StartQuizFunctionalitySagas` has a `checkUniqueAnswerStep` that sends `AssertStudentHasNoAnswerCommand` to the Quiz service. `QuizService.assertStudentHasNoAnswer()` loads the Quiz aggregate and checks `quiz.hasStudentWithAnswer(studentId)`.

### Data Flow

```
StartQuizFunctionality (Answer service)     QuizService             Quiz aggregate
        │                                       │                        │
        │  checkUniqueAnswerStep                │                        │
        │──► AssertStudentHasNoAnswerCommand     │                        │
        │                                       │ assertStudentHasNoAnswer()
        │                                       │──► load Quiz           │
        │                                       │    hasStudentWithAnswer() = false
        │                                       │    (ok, proceed)       │
        │  startQuizStep                        │                        │
        │──────────────────────────────────────────────────────────────► │
        │                          startQuiz() registers CreateQuizAnswerEvent
        │                                       │    (event not yet processed)
        │                                       │                        │
        │  [concurrent 2nd call, same student]  │                        │
        │──► AssertStudentHasNoAnswerCommand     │                        │
        │                                       │    hasStudentWithAnswer() = false ← stale!
        │                                       │    (ok, proceed — BUG) │
        │                                       │                        │
        │  [~1 s later: event poll runs]        │                        │
        │                             Quiz.studentsWithAnswers updated   │
        │                                       │                        │
        │  [3rd call, after poll]               │                        │
        │──► AssertStudentHasNoAnswerCommand     │                        │
        │                                       │    hasStudentWithAnswer() = true
        │                                       │──► throws QUIZ_ALREADY_STARTED_BY_STUDENT
```

---

## The Core Problem

**Layer 5 is backed by eventually-consistent state, so its strong-consistency guarantee does not hold.**

Layer 5 guards are supposed to be strongly consistent: a named workflow step reads another aggregate under a semantic lock, blocking concurrent conflicting mutations. But here, the data being read (`studentsWithAnswers`) is a Layer 6 cache that lags behind reality by up to one poll interval.

The race:

```
t=0   startQuiz() #1 → guard passes (cache empty) → QuizAnswer created → event registered
t=0   startQuiz() #2 → guard passes (cache still empty, event not yet processed) → second QuizAnswer created ✗
t=1s  event poll runs → cache updated → invariant already violated
```

The guard becomes reliable only after the event has been processed. This is documented explicitly in the Consistency Note of `unique-quiz-answer-per-student.md` and is worked around in tests by calling `quizEventHandling.handleCreateQuizAnswerEvents()` manually.

Additionally, the Layer 6 infrastructure (event class, subscription, handler, event polling, event processing, update functionality) exists solely to maintain a cache of data that the `QuizAnswer` service already owns natively. The complexity is not justified.

---

## Design Alternatives

### Option A — Layer 3 guard in `QuizAnswerService` (recommended)

Query the repository directly before creating a new `QuizAnswer`, within the same Unit of Work:

```java
// QuizAnswerService.startQuiz()
if (quizAnswerRepository.existsByQuizIdAndStudentId(quizAggregateId, studentAggregateId)) {
    throw new QuizzesException(QUIZ_ALREADY_STARTED_BY_STUDENT, studentAggregateId, quizAggregateId);
}
```

This is the authoritative service — it owns the `QuizAnswer` table. The check reads from the same DB transaction, so there is no staleness window.

The entire Layer 6 wiring (`CreateQuizAnswerEvent` subscription on `Quiz`, `studentsWithAnswers` field, `QuizSubscribesCreateQuizAnswer`, `CreateQuizAnswerEventHandler`, `handleCreateQuizAnswerEvents()`, `QuizEventProcessing`, `AddStudentToQuizAnswersFunctionality*`) can be removed.

The `checkUniqueAnswerStep` in `StartQuizFunctionalitySagas` and `AssertStudentHasNoAnswerCommand` can also be removed.

| Consistency | Complexity | Notes |
|-------------|------------|-------|
| Strong | Low | Eliminates all L5 + L6 wiring for this rule |

---

### Option B — DB unique constraint (safety net)

Add a unique constraint on `(quiz_id, student_id)` in the `QuizAnswer` table:

```sql
ALTER TABLE quiz_answer ADD CONSTRAINT uq_quiz_student UNIQUE (quiz_id, student_id);
```

The database rejects the second insert unconditionally. A constraint violation is caught and translated into a `QuizzesException(QUIZ_ALREADY_STARTED_BY_STUDENT)`.

This is the strongest possible guarantee, but it surfaces as a DB exception rather than a domain exception. Best used **in addition to Option A** as a last-resort safety net.

| Consistency | Complexity | Notes |
|-------------|------------|-------|
| Strongest (DB-level) | Low (additive) | Translate `DataIntegrityViolationException` to domain exception |

---

### Option C — Fix Layer 5 without Layer 6 (intermediate)

Keep the `checkUniqueAnswerStep` saga step but have it query the `QuizAnswer` repository directly, without relying on the Quiz cache:

```java
// QuizAnswerService.assertStudentHasNoAnswer() — reads its own repository
if (quizAnswerRepository.existsByQuizIdAndStudentId(quizAggregateId, studentAggregateId)) {
    throw new QuizzesException(QUIZ_ALREADY_STARTED_BY_STUDENT, ...);
}
```

The command is now routed to the `QuizAnswer` service rather than the `Quiz` service. This eliminates the staleness problem and removes the need for the Layer 6 cache.

Compared to Option A, this keeps the check as an explicit named saga step (more visible in the workflow graph) at the cost of an extra command dispatch hop for a check that the service could perform locally.

| Consistency | Complexity | Notes |
|-------------|------------|-------|
| Strong | Medium | Removes L6, keeps the L5 step visible in workflow |

---

## Enforcement Summary

| Option | Layer | Where | Consistency | Complexity |
|--------|-------|-------|-------------|------------|
| **Current** | L5 (backed by L6 cache) | `QuizService` + `Quiz` aggregate | Eventually consistent (broken) | High |
| **A (recommended)** | L3 | `QuizAnswerService.startQuiz()` | Strong | Low |
| **B (safety net)** | DB constraint | `QuizAnswer` table | Strongest | Low (additive) |
| **C (alternative)** | L5 (own repository) | `QuizAnswerService` command | Strong | Medium |

The root cause of the current design's failure is using a Layer 5 guard to enforce a rule while reading Layer 6 (eventually-consistent) state. Any correct fix must read from an authoritative, synchronously-updated source.
