# Retro — 2.6.d — Quiz

**App:** quizzes-full
**Session:** 2.6.d (Event Wiring)
**Date:** 2026-05-14

---

## Files Produced

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/events/InvalidateQuizEvent.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/notification/subscribe/QuizSubscribesUpdateQuestion.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/notification/subscribe/QuizSubscribesDeleteQuestion.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/notification/subscribe/QuizSubscribesDeleteCourseExecution.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/notification/handling/QuizEventHandling.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/notification/handling/handlers/QuizEventHandler.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/coordination/eventProcessing/QuizEventProcessing.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/quiz/QuizInterInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/aggregate/Quiz.java` (modified: `getEventSubscriptions()` implemented)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/service/QuizService.java` (modified: added `updateQuestionInQuiz`, `removeQuestionFromQuiz`, `invalidateQuiz`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/coordination/functionalities/QuizFunctionalities.java` (modified: added `@Autowired QuizService` and three ByEvent methods)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/BeanConfigurationSagas.groovy` (modified: added Quiz event handling beans)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/plan.md` (modified: ticked 2.6.d)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/events.md` | Full file — EventSubscription structure, EventHandler dispatch, EventProcessing pattern, ByEvent pattern | Yes | — |
| `docs/concepts/testing.md` | T3 section — InterInvariant test template, deletion event assertion pattern | Partial | T3 template does not cover how to assert that a deleted aggregate is unreachable (see Doc Gaps) |

---

## Skill Instructions Feedback

### What worked well

- The session-d.md sub-file gave a clear structure: subscribe classes → EventHandling → EventHandler → EventProcessing → ByEvent methods in Functionalities → test.
- The `InvalidateQuizEvent` creation and publication pattern (in `removeQuestionFromQuiz` and `invalidateQuiz` service methods) followed naturally from the docs once the plan.md note was read.
- The single `QuizEventHandler` with `instanceof` dispatch worked cleanly for three event types.

### What was unclear or missing

- The session-d.md sub-file does not address how to test deletion events where `aggregateLoadAndRegisterRead` throws `SimulatorException` because DELETED aggregates are filtered out by the infrastructure. The T3 template only shows field-update and removal-from-collection cases.
- The plan.md note states "the Quiz event handler publishes `InvalidateQuizEvent` after updating the quiz state" but does not clarify whether the quiz itself should be marked DELETED or just have a question removed. The decision to call `remove()` for both `DeleteQuestionEvent` and `DeleteCourseExecutionEvent` required inference.

### Suggested wording / structure changes

- `session-d.md` should add a sub-section on deletion-event tests: "For events that cause the consumer aggregate to be deleted (`remove()` called), assert deletion by attempting `aggregateLoadAndRegisterRead` in the `and:` block and matching `thrown(SimulatorException)` in `then:`."

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/testing.md` — T3 section | No guidance on asserting deletion events where DELETED aggregates cannot be loaded via `aggregateLoadAndRegisterRead` | High | Add a note: "For deletion events that mark the consumer as DELETED: move the load attempt into the `when:` phase (`and:` block) and use `thrown(SimulatorException)` in `then:`." |
| `.claude/skills/implement-aggregate/session-d.md` | No mention of when to call `remove()` on the consumer vs. just removing a sub-entity. Two cases exist: (a) remove sub-entity from collection (e.g., `DeleteTopicEvent` in Question), (b) invalidate the whole consumer (`DeleteQuestionEvent`/`DeleteCourseExecutionEvent` in Quiz). The decision rule is not documented. | Medium | Add: "If the event signals the loss of an entity that makes the consumer aggregate non-functional (e.g., no questions left, no execution), call `remove()` on the aggregate copy and publish an outbound event if appropriate. If the event only removes one sub-entity from a collection (collection can still be empty-valid), just remove from the collection." |

---

## Patterns to Capture

- **Pattern:** DELETED aggregate is not loadable via `aggregateLoadAndRegisterRead`
  **Observed in:** `QuizInterInvariantTest.groovy` (test failure + fix)
  **Description:** After `remove()` is called on an aggregate, it is stored with `AggregateState.DELETED`. `SagaUnitOfWorkService.aggregateLoadAndRegisterRead` filters out DELETED aggregates and throws `SimulatorException`. Tests that assert deletion must not call the load in the `then:` block; instead call it in `and:` (part of the `when:` phase) and assert `thrown(SimulatorException)`.

- **Pattern:** ByEvent + `remove()` + outbound event for invalidation
  **Observed in:** `QuizService.removeQuestionFromQuiz`, `QuizService.invalidateQuiz`
  **Description:** When the consumer aggregate becomes non-functional due to an event (publisher deleted), the service method calls `copy.remove()` to mark it DELETED and registers an outbound `InvalidateQuizEvent` so downstream subscribers can react. This is distinct from the simpler pattern where a sub-entity is removed from a collection without invalidating the whole aggregate.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| High | `docs/concepts/testing.md` | Add T3 deletion-event assertion pattern: move load into `and:` block, use `thrown(SimulatorException)` in `then:` |
| High | `.claude/skills/implement-aggregate/session-d.md` | Add decision rule for when to call `remove()` on the consumer vs. just removing a sub-entity from a collection |
| Medium | `docs/concepts/events.md` | Clarify the "InvalidateQuizEvent" pattern: when a consumer aggregate publishes an outbound event after processing an inbound deletion event |

---

## One-Line Summary

DELETED aggregates are not loadable via `aggregateLoadAndRegisterRead`, requiring a test structure change for deletion-event assertions; this gap is absent from the T3 testing docs.
