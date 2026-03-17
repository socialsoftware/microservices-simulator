# UNIQUE_QUIZ_ANSWER_PER_STUDENT

**Type:** Inter-invariant
**Enforced in:** `QuizAnswerService.startQuiz()`
**Rule:** A student can only create one `QuizAnswer` per quiz. If the student has already started a quiz, any subsequent `startQuiz()` call for the same quiz throws `QUIZ_ALREADY_STARTED_BY_STUDENT`.

---

## Data Flow

```
QuizAnswerService               Quiz aggregate
     │                               │
     │  startQuiz()                  │
     │──► registers CreateQuizAnswerEvent ──────────────────────────────────────►│
     │                               │  QuizEventHandling (1s poll)              │
     │                               │  handleCreateQuizAnswerEvents()           │
     │                               │  → CreateQuizAnswerEventHandler           │
     │                               │  → QuizEventProcessing                    │
     │                               │  → QuizFunctionalities                    │
     │                               │  → AddStudentToQuizAnswersCommand         │
     │                               │  → QuizService.addStudentToQuizAnswers()  │
     │                               │  → Quiz.studentsWithAnswers.add(studentId)│
     │                               │
     │  startQuiz() [2nd call]        │
     │──► loads Quiz aggregate        │
     │    quiz.hasStudentWithAnswer() = true
     │──► throws QUIZ_ALREADY_STARTED_BY_STUDENT
```

---

## Key Files

| Layer | File |
|-------|------|
| Shared event | `events/CreateQuizAnswerEvent.java` |
| Publisher (registers event) | `microservices/answer/service/QuizAnswerService.java` |
| Consumer tracked state | `microservices/quiz/aggregate/Quiz.java` — `studentsWithAnswers: Set<Integer>` |
| TCC merge | `microservices/quiz/aggregate/causal/CausalQuiz.java` — union merge |
| Event subscription | `microservices/quiz/events/subscribe/QuizSubscribesCreateQuizAnswer.java` |
| Event handler | `microservices/quiz/events/handling/handlers/CreateQuizAnswerEventHandler.java` |
| Event polling | `microservices/quiz/events/handling/QuizEventHandling.java` — `handleCreateQuizAnswerEvents()` |
| Event processing | `microservices/quiz/coordination/eventProcessing/QuizEventProcessing.java` |
| Command | `command/quiz/AddStudentToQuizAnswersCommand.java` |
| Sagas functionality | `microservices/quiz/coordination/sagas/AddStudentToQuizAnswersFunctionalitySagas.java` |
| TCC functionality | `microservices/quiz/coordination/causal/AddStudentToQuizAnswersFunctionalityTCC.java` |
| State update | `microservices/quiz/service/QuizService.java` — `addStudentToQuizAnswers()` |
| Guard | `microservices/answer/service/QuizAnswerService.java` — `startQuiz()` |
| Error message | `microservices/exception/QuizzesErrorMessage.java` — `QUIZ_ALREADY_STARTED_BY_STUDENT` |
| Tests | `src/test/groovy/.../sagas/coordination/answer/StartQuizTest.groovy` |

---

## Code Snippets

### Event registration (publisher)
```java
// QuizAnswerService.startQuiz()
unitOfWorkService.registerEvent(
        new CreateQuizAnswerEvent(quizAggregateId, userDto.getAggregateId()), unitOfWork);
```

### Tracked state field (consumer)
```java
// Quiz.java
@ElementCollection
private Set<Integer> studentsWithAnswers = new HashSet<>();

public boolean hasStudentWithAnswer(Integer studentAggregateId) {
    return studentsWithAnswers.contains(studentAggregateId);
}
```

### Event subscription wiring
```java
// Quiz.java — getEventSubscriptions()
private void interInvariantUniqueQuizAnswerPerStudent(Set<EventSubscription> subs) {
    subs.add(new QuizSubscribesCreateQuizAnswer(this));
}
```

### Guard check (guarded operation)
```java
// QuizAnswerService.startQuiz()
// Inter-invariant: UNIQUE_QUIZ_ANSWER_PER_STUDENT
Quiz quiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizAggregateId, unitOfWork);
if (quiz.hasStudentWithAnswer(userDto.getAggregateId())) {
    throw new QuizzesException(QuizzesErrorMessage.QUIZ_ALREADY_STARTED_BY_STUDENT,
            userDto.getAggregateId(), quizAggregateId);
}
```

---

## Consistency Note

This invariant is **eventually consistent**. After the first `startQuiz()` call, the `CreateQuizAnswerEvent` is persisted but the Quiz's `studentsWithAnswers` set is only updated when `QuizEventHandling.handleCreateQuizAnswerEvents()` next polls (1-second interval). During that window, a second concurrent `startQuiz()` could also succeed.

The guard becomes reliable once the event has been processed. In tests, call `quizEventHandling.handleCreateQuizAnswerEvents()` manually to synchronise before asserting the blocked case.

---

## Test Table

| Scenario | Expected outcome |
|----------|-----------------|
| Student starts a quiz for the first time | `startQuiz()` succeeds, no exception |
| Student tries to start the same quiz after event is processed | `QuizzesException` (`QUIZ_ALREADY_STARTED_BY_STUDENT`) |
| A different student starts the same quiz | `startQuiz()` succeeds — guard is per-student |
