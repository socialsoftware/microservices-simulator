# Tournament Inter-Invariants

**Type:** Inter-invariants
**Consumer aggregate:** `Tournament`
**Rule summary:** A `Tournament` caches references to external aggregates (creator/participants as `User`s, `CourseExecution`, `Topic`s, `Quiz`, `QuizAnswer`s). Each cached reference must remain consistent with the current state of the referenced aggregate. Consistency is maintained eventually via domain events.

---

## Invariant Overview

| Invariant | Publisher aggregate | Trigger event | Effect on Tournament |
|-----------|-------------------|---------------|----------------------|
| `CREATOR_EXISTS` / `PARTICIPANT_EXISTS` | `UserService` | `AnonymizeStudentEvent` | Creator/participant name and username set to `"ANONYMOUS"`, state → INACTIVE |
| `CREATOR_EXISTS` / `PARTICIPANT_EXISTS` | `UserService` | `UpdateStudentNameEvent` | Creator/participant name updated to match current User |
| `COURSE_EXECUTION_EXISTS` | `ExecutionService` | `DeleteCourseExecutionEvent` | Tournament anonymised / set INACTIVE |
| `CREATOR_COURSE_EXECUTION` / `PARTICIPANT_COURSE_EXECUTION` | `ExecutionService` | `DisenrollStudentFromCourseExecutionEvent` | Disenrolled creator/participant removed from tournament |
| `TOPIC_EXISTS` | `TopicService` | `UpdateTopicEvent` | Cached topic name updated |
| `TOPIC_EXISTS` | `TopicService` | `DeleteTopicEvent` | Topic removed from tournament topics |
| `QUIZ_EXISTS` | `QuizService` | `InvalidateQuizEvent` | Tournament quiz marked invalid |
| `QUIZ_ANSWER_EXISTS` / `NUMBER_OF_ANSWERED` / `NUMBER_OF_CORRECT` | `QuizAnswerService` | `QuizAnswerQuestionAnswerEvent` | Participant answer stats updated |

---

## Event Subscription Wiring — `Tournament.getEventSubscriptions()`

```java
@Override
public Set<EventSubscription> getEventSubscriptions() {
    Set<EventSubscription> eventSubscriptions = new HashSet<>();
    if (this.getState() == AggregateState.ACTIVE) {
        interInvariantCourseExecutionExists(eventSubscriptions);  // COURSE_EXECUTION_EXISTS
        interInvariantCreatorExists(eventSubscriptions);           // CREATOR_EXISTS
        interInvariantParticipantExists(eventSubscriptions);       // PARTICIPANT_EXISTS
        interInvariantQuizAnswersExist(eventSubscriptions);        // QUIZ_ANSWER_EXISTS
        interInvariantTopicsExist(eventSubscriptions);             // TOPIC_EXISTS
        interInvariantQuizExists(eventSubscriptions);              // QUIZ_EXISTS
    }
    return eventSubscriptions;
}
```

Subscriptions are only registered while the tournament is `ACTIVE`. Inactive tournaments do not receive further updates.

---

## Per-Invariant Details

### CREATOR_EXISTS / PARTICIPANT_EXISTS

**Rule:**
```
this.creator.state != INACTIVE =>
    EXISTS User(this.creator.id)
    && this.creator.username == User(this.creator.id).username
    && this.creator.name   == User(this.creator.id).name

forall p : this.tournamentParticipants | p.state != INACTIVE =>
    EXISTS User(p.id)
    && p.username == User(p.id).username
    && p.name     == User(p.id).name
```

**Subscriptions:**
```java
private void interInvariantCreatorExists(Set<EventSubscription> eventSubscriptions) {
    eventSubscriptions.add(new TournamentSubscribesDisenrollStudentFromCourseExecution(this));
    eventSubscriptions.add(new TournamentSubscribesAnonymizeStudent(this));
    eventSubscriptions.add(new TournamentSubscribesUpdateStudentName(this));
}
```

**Events consumed:**

| Event | Source file | Effect |
|-------|------------|--------|
| `AnonymizeStudentEvent` | `events/AnonymizeStudentEvent.java` | Sets creator/participant name+username to `"ANONYMOUS"`, marks INACTIVE |
| `UpdateStudentNameEvent` | `events/UpdateStudentNameEvent.java` | Propagates new name into cached creator/participant fields |

**Polling:**
```java
// TournamentEventHandling.java
@Scheduled(fixedDelay = 1000)
public void handleAnonymizeStudentEvents() {
    eventApplicationService.handleSubscribedEvent(AnonymizeStudentEvent.class,
            new AnonymizeStudentEventHandler(tournamentRepository, tournamentEventProcessing));
}

@Scheduled(fixedDelay = 1000)
public void handleUpdateStudentNameEvent() {
    eventApplicationService.handleSubscribedEvent(UpdateStudentNameEvent.class,
            new UpdateStudentNameEventHandler(tournamentRepository, tournamentEventProcessing));
}
```

**Processing chain:**
`AnonymizeStudentEventHandler` → `TournamentEventProcessing.processAnonymizeStudentEvent()` → `TournamentFunctionalities.anonymizeStudent()`
`UpdateStudentNameEventHandler` → `TournamentEventProcessing.processUpdateStudentNameEvent()` → `TournamentFunctionalities.updateStudentName()`

---

### COURSE_EXECUTION_EXISTS

**Rule:**
```
this.tournamentCourseExecution.state != INACTIVE =>
    EXISTS CourseExecution(this.tournamentCourseExecution.id)
    && this.courseExecution.courseId  == CourseExecution(...).Course.id
    && this.courseExecution.status    == CourseExecution(...).status
    && this.courseExecution.acronym   == CourseExecution(...).acronym
```

**Subscription:**
```java
private void interInvariantCourseExecutionExists(Set<EventSubscription> eventSubscriptions) {
    eventSubscriptions.add(new TournamentSubscribesDeleteCourseExecution(
            this.getTournamentCourseExecution()));
}
```

**Event consumed:** `DeleteCourseExecutionEvent` — emitted by `ExecutionService` when a `CourseExecution` is removed.

**Polling:**
```java
@Scheduled(fixedDelay = 1000)
public void handleDeleteCourseExecutionEvents() {
    eventApplicationService.handleSubscribedEvent(DeleteCourseExecutionEvent.class,
            new DeleteCourseExecutionEventHandler(tournamentRepository, tournamentEventProcessing));
}
```

**Processing chain:**
`DeleteCourseExecutionEventHandler` → `TournamentEventProcessing.processRemoveCourseExecutionEvent()` → `TournamentFunctionalities.removeCourseExecution()`

---

### CREATOR_COURSE_EXECUTION / PARTICIPANT_COURSE_EXECUTION

**Rule:** Every creator and participant must be enrolled in the tournament's course execution. When a student is disenrolled from the execution they are removed from the tournament.

**Subscription:** `TournamentSubscribesDisenrollStudentFromCourseExecution` (shared with `interInvariantCreatorExists` / `interInvariantParticipantExists`).

**Event consumed:** `DisenrollStudentFromCourseExecutionEvent`

**Polling:**
```java
@Scheduled(fixedDelay = 1000)
public void handleUnenrollStudentFromCourseExecutionEvents() {
    eventApplicationService.handleSubscribedEvent(DisenrollStudentFromCourseExecutionEvent.class,
            new DisenrollStudentFromCourseExecutionEventHandler(tournamentRepository, tournamentEventProcessing));
}
```

**Processing chain:**
`DisenrollStudentFromCourseExecutionEventHandler` → `TournamentEventProcessing.processDisenrollStudentFromCourseExecutionEvent()` → `TournamentFunctionalities.disenrollStudent()`

---

### TOPIC_EXISTS

**Rule:**
```
t : this.tournamentTopics | t.state != INACTIVE =>
    EXISTS Topic(t.id) && t.name == Topic(t.id).name
```

**Subscriptions:**
```java
private void interInvariantTopicsExist(Set<EventSubscription> eventSubscriptions) {
    for (TournamentTopic tournamentTopic : this.tournamentTopics) {
        eventSubscriptions.add(new TournamentSubscribesDeleteTopic(tournamentTopic));
        eventSubscriptions.add(new TournamentSubscribesUpdateTopic(tournamentTopic));
    }
}
```

One subscription pair is registered **per topic** — each `TournamentTopic` subscribes independently.

**Events consumed:**

| Event | Effect |
|-------|--------|
| `UpdateTopicEvent` | Updates the cached topic name in `TournamentTopic` |
| `DeleteTopicEvent` | Removes the `TournamentTopic` from the tournament's topic set |

**Polling:**
```java
@Scheduled(fixedDelay = 1000)
public void handleUpdateTopicEvents() {
    eventApplicationService.handleSubscribedEvent(UpdateTopicEvent.class,
            new UpdateTopicEventHandler(tournamentRepository, tournamentEventProcessing));
}

@Scheduled(fixedDelay = 1000)
public void handleDeleteTopicEvents() {
    eventApplicationService.handleSubscribedEvent(DeleteTopicEvent.class,
            new DeleteTopicEventHandler(tournamentRepository, tournamentEventProcessing));
}
```

**Processing chain:**
`UpdateTopicEventHandler` → `TournamentEventProcessing.processUpdateTopicEvent()` → `TournamentFunctionalities.updateTopic()`
`DeleteTopicEventHandler` → `TournamentEventProcessing.processDeleteTopicEvent()` → `TournamentFunctionalities.deleteTopic()`

---

### QUIZ_EXISTS

**Rule:**
```
this.tournamentQuiz.state != INACTIVE =>
    EXISTS Quiz(this.tournamentQuiz.id)
```

**Subscription:**
```java
private void interInvariantQuizExists(Set<EventSubscription> eventSubscriptions) {
    eventSubscriptions.add(new TournamentSubscribesInvalidateQuiz(this.getTournamentQuiz()));
}
```

**Event consumed:** `InvalidateQuizEvent` — emitted by `QuizService` when the quiz is invalidated.

**Polling:**
```java
@Scheduled(fixedDelay = 1000)
public void handleInvalidateQuizEvent() {
    eventApplicationService.handleSubscribedEvent(InvalidateQuizEvent.class,
            new InvalidateQuizEventHandler(tournamentRepository, tournamentEventProcessing));
}
```

**Processing chain:**
`InvalidateQuizEventHandler` → `TournamentEventProcessing.processInvalidateQuizEvent()` → `TournamentFunctionalities.invalidateQuiz()`

---

### QUIZ_ANSWER_EXISTS / NUMBER_OF_ANSWERED / NUMBER_OF_CORRECT

**Rule:**
```
p : this.participants | (!p.answer.isEmpty && p.answer.state != INACTIVE) =>
    EXISTS QuizAnswer(p.answer.id)
```

**Subscription:** Registered per participant that already has an answer:
```java
private void interInvariantQuizAnswersExist(Set<EventSubscription> eventSubscriptions) {
    for (TournamentParticipant tournamentParticipant : this.tournamentParticipants) {
        if (tournamentParticipant.getParticipantAnswer().getQuizAnswerAggregateId() != null) {
            eventSubscriptions.add(new TournamentSubscribesAnswerQuestion(tournamentParticipant));
        }
    }
}
```

Only participants whose `quizAnswerAggregateId` is already set subscribe — i.e., the tournament tracks answer update events only after a student has started the quiz.

**Event consumed:** `QuizAnswerQuestionAnswerEvent` — emitted by `QuizAnswerService` when a student answers a question.

**Polling:**
```java
@Scheduled(fixedDelay = 1000)
public void handleAnswerQuestionEvent() {
    eventApplicationService.handleSubscribedEvent(QuizAnswerQuestionAnswerEvent.class,
            new QuizAnswerQuestionAnswerEventHandler(tournamentRepository, tournamentEventProcessing));
}
```

**Processing chain:**
`QuizAnswerQuestionAnswerEventHandler` → `TournamentEventProcessing.processAnswerQuestionEvent()` → `TournamentFunctionalities.updateParticipantAnswer()`

---

## Data Flow Summary

```
UserService                       Tournament aggregate
    │  anonymizeUser()                    │
    │──► AnonymizeStudentEvent ──────────►│ TournamentEventHandling (1s poll)
    │  updateStudentName()                │ → AnonymizeStudentEventHandler
    │──► UpdateStudentNameEvent ─────────►│ → UpdateStudentNameEventHandler
    │                                     │   → TournamentEventProcessing
    │                                     │   → TournamentFunctionalities
    │                                     │   → TournamentService (creator/participant updated)
    │
ExecutionService                  Tournament aggregate
    │  removeCourseExecution()           │
    │──► DeleteCourseExecutionEvent ─────►│ → DeleteCourseExecutionEventHandler
    │  disenrollStudent()                 │   → removeCourseExecution() / disenrollStudent()
    │──► DisenrollStudentEvent ──────────►│ → DisenrollStudentEventHandler
    │
TopicService                      Tournament aggregate
    │  updateTopic()                      │
    │──► UpdateTopicEvent ───────────────►│ → UpdateTopicEventHandler → updateTopic()
    │  deleteTopic()                      │
    │──► DeleteTopicEvent ───────────────►│ → DeleteTopicEventHandler → deleteTopic()
    │
QuizService                       Tournament aggregate
    │  invalidateQuiz()                   │
    │──► InvalidateQuizEvent ────────────►│ → InvalidateQuizEventHandler → invalidateQuiz()
    │
QuizAnswerService                 Tournament aggregate
    │  answerQuestion()                   │
    │──► QuizAnswerQuestionAnswerEvent ──►│ → QuizAnswerQuestionAnswerEventHandler
    │                                     │   → updateParticipantAnswer()
```

---

## Key Files

| Layer | File |
|-------|------|
| Consumer aggregate | `microservices/tournament/aggregate/Tournament.java` |
| Event polling | `microservices/tournament/events/handling/TournamentEventHandling.java` |
| Event processing | `microservices/tournament/coordination/eventProcessing/TournamentEventProcessing.java` |
| Subscriptions | `microservices/tournament/events/subscribe/TournamentSubscribes*.java` |
| Handlers | `microservices/tournament/events/handling/handlers/*EventHandler.java` |
| Functionalities | `microservices/tournament/coordination/functionalities/TournamentFunctionalities.java` |
| Shared events | `events/AnonymizeStudentEvent.java`, `events/UpdateStudentNameEvent.java`, `events/DeleteCourseExecutionEvent.java`, `events/DisenrollStudentFromCourseExecutionEvent.java`, `events/UpdateTopicEvent.java`, `events/DeleteTopicEvent.java`, `events/InvalidateQuizEvent.java`, `events/QuizAnswerQuestionAnswerEvent.java` |

---

## Consistency Note

All Tournament inter-invariants are **eventually consistent**: the Tournament's cached state is updated asynchronously via event polling (1-second interval). During the window between an external change and event processing, the Tournament may hold stale data. This is a known trade-off of the event-driven design.

Intra-invariants (e.g. `FINAL_AFTER_START`, `IS_CANCELED`, `UNIQUE_AS_PARTICIPANT`) are checked synchronously in `Tournament.verifyInvariants()` on every write and are therefore strongly consistent.
