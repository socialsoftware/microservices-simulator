# Retro — 2.8.d — Tournament

**App:** quizzes-full
**Session:** 2.8.d (Event Wiring)
**Date:** 2026-05-19

---

## Files Produced

### Application files (quizzes-full)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/notification/subscribe/TournamentSubscribesDeleteUser.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/notification/subscribe/TournamentSubscribesUpdateStudentName.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/notification/subscribe/TournamentSubscribesAnonymizeStudent.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/notification/subscribe/TournamentSubscribesUpdateTopic.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/notification/subscribe/TournamentSubscribesDeleteTopic.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/notification/subscribe/TournamentSubscribesDeleteCourseExecution.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/notification/subscribe/TournamentSubscribesInvalidateQuiz.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/notification/subscribe/TournamentSubscribesQuizAnswerQuestionAnswer.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/notification/handling/TournamentEventHandling.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/notification/handling/handlers/TournamentEventHandler.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/coordination/eventProcessing/TournamentEventProcessing.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/tournament/TournamentInterInvariantTest.groovy`

### Application bug fixes (earlier-session files)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/aggregate/Tournament.java` — `getEventSubscriptions()` populated (was returning empty HashSet in session 2.8.a)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/service/TournamentService.java` — 9 ByEvent service methods added (including `setParticipantQuizAnswer` for test setup)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/coordination/functionalities/TournamentFunctionalities.java` — 8 ByEvent functionalities methods added
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/BeanConfigurationSagas.groovy` — 3 new event wiring beans added

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `.claude/skills/implement-aggregate/session-d.md` | All sections | Yes | Clear patterns for all components |
| `docs/concepts/testing.md` | T3 section | Yes | Deletion-event pattern (and: block + thrown) was clear |

---

## Skill Instructions Feedback

### What worked well

- The `and:` / `then: thrown(SimulatorException)` pattern for deletion events was clearly documented in testing.md and worked without ambiguity.
- The description of anchoring for user subscriptions (raw `(Integer, Long)` constructor) covered the multi-entry scenario (creator + participants each needing a separate subscription).
- The distinction between "Remove sub-entity from collection" vs "Invalidate the whole consumer" in session-d.md was exactly right for all 8 event types without guessing.

### What was unclear or missing

- **QuizAnswerQuestionAnswerEvent subscription gap**: The subscription for `QuizAnswerQuestionAnswerEvent` is anchored on `quizAnswerAggregateId`, but no existing saga (AddParticipant, CreateQuizAnswer, AnswerQuestion) links the quiz answer to the tournament. The skill has no guidance for this case. Resolution: added `setParticipantQuizAnswer` service method as test scaffolding and null-checked in `getEventSubscriptions()`. This required independent reasoning — the skill should note that when a subscription depends on an ID not set by any saga, a service-level helper may be needed for test setup, and the subscription should be conditional on the ID being non-null.
- No guidance on how to test the `QuizAnswerQuestionAnswerEvent` "ignores unrelated" case when the anchor ID is conditionally set. Had to infer: create a second quiz answer for a second user (not linked to tournament) to produce the "unrelated" event.

### Suggested wording / structure changes

- In session-d.md, add a note under `TournamentInterInvariantTest.groovy (T3)`: "If the subscription is conditional on a nullable anchor ID (e.g., `quizAnswerAggregateId != null`), the 'ignores unrelated' test can use a second quiz answer whose aggregateId is never registered with the tournament."

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `session-d.md` | No guidance for conditional subscriptions (nullable anchor ID) or the need for test-setup service methods when no saga populates the anchor | Medium | Add a note under T3 test section describing the conditional-subscription pattern and the `setParticipantQuizAnswer`-style test helper |
| `session-d.md` | No mention of the TOURNAMENT_IS_CANCELED / TOURNAMENT_DELETE invariant interaction for ByEvent deletion methods | Low | Document that deletion ByEvent methods on cancelled tournaments with participants will raise TOURNAMENT_IS_CANCELED by design |

---

## Patterns to Capture

- **Conditional subscription based on nullable anchor**: When a cached reference (e.g., `quizAnswerAggregateId`) may be null until set by a saga or test setup, `getEventSubscriptions()` should guard with `!= null` before adding the subscription. The test then calls a service-level helper to set the ID, enabling the subscription to be registered on the next load.
  **Observed in:** `Tournament.java` — `getEventSubscriptions()` QuizAnswer block

- **Multi-entry user subscription**: When a tournament has both a creator and a participant set, each needing independent user-event subscriptions, the raw `(Integer userAggregateId, Long userVersion)` constructor on the subscription class allows one entry per user rather than being forced to use a single entity object.
  **Observed in:** `TournamentSubscribesDeleteUser.java`, `Tournament.getEventSubscriptions()`

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| Medium | `.claude/skills/implement-aggregate/session-d.md` | Add note on conditional subscriptions (nullable anchor ID) and the need for test-setup service helpers |
| Low | `.claude/skills/implement-aggregate/session-d.md` | Note that ByEvent deletion on invariant-constrained aggregates (e.g., TOURNAMENT_IS_CANCELED) behaves consistently with the regular saga deletion |
| ~~Done~~ | `plan.md` + `TournamentFunctionalities` + `TournamentCommandHandler` + test | Root cause fixed: `SolveQuizFunctionalitySagas` + `SolveQuizCommand` added; `TournamentInterInvariantTest` now calls `tournamentFunctionalities.solveQuiz()` instead of the direct service hack |

---

## Follow-up (post-session diagnosis)

**Root cause of the `quizAnswerAggregateId` null problem:**
`SolveQuiz` was absent from the plan. In the reference app, `SolveQuizFunctionalitySagas` is a Tournament-owned saga that creates a QuizAnswer and writes the resulting ID back to `TournamentParticipant.quizAnswer.quizAnswerAggregateId`. In quizzes-full, `CreateQuizAnswerFunctionalitySagas` (QuizAnswer-owned) creates the QuizAnswer, but no saga ever performed the write-back, leaving `quizAnswerAggregateId` permanently null in any real workflow.

**Resolution:**
- Added `SolveQuizCommand` (carries `tournamentId, userId, quizAnswerAggregateId, quizAnswerVersion`)
- Added `SolveQuizFunctionalitySagas` (3 steps: get Tournament → get QuizAnswer by `(quizId, userId)` → call `SolveQuizCommand`)
- Wired into `TournamentCommandHandler` (calls `TournamentService.setParticipantQuizAnswer()`)
- Added `TournamentFunctionalities.solveQuiz(tournamentId, userId)` as the public entry point
- `TournamentInterInvariantTest` now calls `tournamentFunctionalities.solveQuiz()` instead of the `createUnitOfWork` + direct service call scaffold
- `TournamentService.setParticipantQuizAnswer()` is NOT dead — it is now properly invoked via the command handler

**Lesson for future sessions:** When a `getEventSubscriptions()` subscription is anchored on a nullable cached ID, the missing piece is always a saga that performs the write-back. Look for a corresponding `SolveX` / `LinkX` saga in the reference app before resorting to a test-setup service method.

---

## One-Line Summary

The main finding is that `QuizAnswerQuestionAnswerEvent` requires a nullable-anchor conditional subscription because no saga populated `quizAnswerAggregateId` — the root cause was a missing `SolveQuiz` saga, which was subsequently added to the plan and implemented.
