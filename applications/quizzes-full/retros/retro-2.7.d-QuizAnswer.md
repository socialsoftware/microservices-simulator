# Retro — 2.7.d — QuizAnswer

**App:** quizzes-full
**Session:** 2.7.d (Event Wiring)
**Date:** 2026-05-16

---

## Files Produced

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/notification/subscribe/QuizAnswerSubscribesDeleteUser.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/notification/subscribe/QuizAnswerSubscribesUpdateStudentName.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/notification/subscribe/QuizAnswerSubscribesAnonymizeStudent.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/notification/subscribe/QuizAnswerSubscribesUpdateQuestion.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/notification/subscribe/QuizAnswerSubscribesDeleteCourseExecution.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/notification/subscribe/QuizAnswerSubscribesDisenrollStudentFromCourseExecution.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/notification/subscribe/QuizAnswerSubscribesInvalidateQuiz.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/notification/handling/QuizAnswerEventHandling.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/notification/handling/handlers/QuizAnswerEventHandler.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/coordination/eventProcessing/QuizAnswerEventProcessing.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/quizanswer/QuizAnswerInterInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/aggregate/QuizAnswer.java` (modified: populated getEventSubscriptions())
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/service/QuizAnswerService.java` (modified: added removeQuizAnswer, removeQuizAnswerIfUserMatches, updateStudentName, anonymizeStudent, updateQuestionVersionInQuizAnswer)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/coordination/functionalities/QuizAnswerFunctionalities.java` (modified: added ByEvent methods and QuizAnswerService injection)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/BeanConfigurationSagas.groovy` (modified: added 3 new beans for event wiring)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/plan.md` (modified: ticked 2.7.d)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/events.md` | § EventSubscription, § EventHandler, § Polling, § Canonical Wiring Snippet, § Cascade Invalidation Pattern | Partial | subscribesEvent() override pattern documented but never called by infrastructure — see Doc Gaps |
| `docs/concepts/testing.md` | § T3 Inter-Invariant Test, § Deletion-Event Tests (T3) | Yes | — |
| `.claude/skills/implement-aggregate/session-d.md` | All sections | Partial | subscribesEvent() override section describes an unimplemented infrastructure feature — misleads implementors |

---

## Skill Instructions Feedback

### What worked well

- The single-class EventHandler with instanceof dispatch pattern is clear and easy to follow.
- The ByEvent method pattern (separate from saga Functionalities method) is well-explained and avoids circular saga loops.
- The deletion event table (remove sub-entity vs. invalidate whole consumer) was useful for classifying the 4 invalidating events.

### What was unclear or missing

- The session-d.md says to override `subscribesEvent()` for additional filtering (e.g., checking a sub-entity's active status). However, `EventApplicationService.handleSubscribedEvent()` never calls `subscribesEvent()` — it calls `getSubscribedEvents()` which queries by `subscribedAggregateId` and `subscribedVersion` only. The override is a dead code path in the current infrastructure.
- For `DisenrollStudentFromCourseExecutionEvent`: the anchor is `executionAggregateId`, so ALL QuizAnswer aggregates for that execution receive the event, regardless of which user was disenrolled. The skill doesn't address this case — the fix must be in the service layer (check userId before removing), not in a subscription filter.
- `UpdateQuestionEvent` for QuizAnswer: QuestionAnswer doesn't cache title/content, only `questionVersion`. The intent of the subscription (update `questionVersion`) had to be inferred.

### Suggested wording / structure changes

- session-d.md: Add a note that `subscribesEvent()` overrides are NOT called by the current `EventApplicationService` implementation. Remove the suggestion to override it for filtering, or document that filtering must be done in the service layer instead.
- session-d.md: Add guidance for the "shared anchor" case — when a deletion event is anchored on a collection-owner aggregate (e.g., execution) but only affects one member of the consumer, the userId/memberId check must happen in the service, not in the subscription.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/events.md` | `subscribesEvent()` override is described but `EventApplicationService` never calls it — filtering is purely by `subscribedAggregateId` + `subscribedVersion` in the DB query | High | Clarify that subscription-level filtering beyond aggregateId/version is not supported; additional filtering must go in the service ByEvent method |
| `.claude/skills/implement-aggregate/session-d.md` | No guidance for events where anchor = collection-owner and only one member of that owner is affected (e.g., DisenrollStudentFromCourseExecutionEvent → only affects the disenrolled student's QuizAnswer) | High | Add a section: "Shared-anchor events: when multiple consumer aggregates share the same anchor but only one should be affected, check the discriminating field (e.g., userId) in the service ByEvent method, not in the subscription" |
| `.claude/skills/implement-aggregate/session-d.md` | No guidance on what UpdateQuestionEvent means for aggregates (like QuizAnswer) that don't cache question title/content — intent of subscription is unclear | Medium | Add a note: "If the consumer caches no payload fields from the publisher, the UpdateQuestionEvent subscription exists to update the cached version field on the matching sub-entity" |

---

## Patterns to Capture

- **Pattern:** Shared-anchor event filtering in service layer
  **Observed in:** `QuizAnswerService.removeQuizAnswerIfUserMatches()`
  **Description:** When a deletion event is anchored on an aggregate that may own multiple consumers (e.g., `DisenrollStudentFromCourseExecutionEvent` anchored on executionId), all consumers for that anchor receive the event. The consumer must check the discriminating field (userId) inside the service ByEvent method before taking action, because the subscription infrastructure only filters by anchor ID.

- **Pattern:** QuestionVersion-only update from UpdateQuestionEvent
  **Observed in:** `QuizAnswerService.updateQuestionVersionInQuizAnswer()`
  **Description:** When a consumer caches no payload fields from the question (no title/content), the UpdateQuestionEvent handler updates only the `questionVersion` on the matching sub-entity to the event's `publisherAggregateVersion`.

- **Pattern:** T3 version comparison without hardcoded version numbers
  **Observed in:** `QuizAnswerInterInvariantTest`
  **Description:** Aggregate versions start much higher than 1 due to the multi-step setup. T3 tests must capture `versionBefore` after the answerQuestion call and compare `versionAfter > versionBefore` (reflects) or `versionAfter == versionBefore` (ignores), never hardcode `== 1L`.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| High | `.claude/skills/implement-aggregate/session-d.md` | Add section: "Shared-anchor events require service-layer userId filtering — override of subscribesEvent() is NOT called by EventApplicationService" |
| High | `docs/concepts/events.md` | Clarify that subscribesEvent() is not called in the event processing loop; additional filtering must go in the ByEvent service method |
| Medium | `.claude/skills/implement-aggregate/session-d.md` | Add guidance on UpdateQuestionEvent for consumers that don't cache question payload (update questionVersion only) |
| Medium | `.claude/skills/implement-aggregate/session-d.md` | Add note to T3 test section: never hardcode version numbers, always capture versionBefore after setup completes |

---

## One-Line Summary

The `subscribesEvent()` override pattern documented in session-d.md is a dead code path in the current infrastructure; shared-anchor event filtering (e.g., DisenrollStudentFromCourseExecutionEvent) must instead be implemented in the service-layer ByEvent method.
