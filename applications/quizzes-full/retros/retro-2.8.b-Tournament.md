# Retro — 2.8.b — Tournament

**App:** quizzes-full
**Session:** 2.8.b (Write Functionalities)
**Date:** 2026-05-17

---

## Files Produced

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/tournament/GetTournamentByIdCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/tournament/CreateTournamentCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/tournament/AddParticipantCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/tournament/UpdateTournamentCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/tournament/CancelTournamentCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/tournament/DeleteTournamentCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/service/TournamentService.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/messaging/TournamentCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/coordination/sagas/CreateTournamentFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/coordination/sagas/AddParticipantFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/coordination/sagas/UpdateTournamentFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/coordination/sagas/CancelTournamentFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/coordination/sagas/DeleteTournamentFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/coordination/functionalities/TournamentFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/coordination/webapi/TournamentController.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/tournament/CreateTournamentTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/tournament/AddParticipantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/tournament/UpdateTournamentTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/tournament/CancelTournamentTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/tournament/DeleteTournamentTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/exception/QuizzesFullErrorMessage.java` (modified — added TOURNAMENT_TOPIC_COURSE_MISMATCH)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/service/QuizService.java` (modified — null-safe questions in updateQuiz)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/BeanConfigurationSagas.groovy` (modified — Tournament beans)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/QuizzesFullSpockTest.groovy` (modified — tournamentFunctionalities, createTournament helper)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/plan.md` (modified — checkbox ticked, file table patched)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `.claude/skills/implement-aggregate/session-b.md` | Full session instructions, SagaCommand pattern, lock steps, compensation lambdas | Partial | Did not explicitly state that `GetXByIdCommand` must be created in session-b; this was inferred from the lock-step pattern requirement |
| `docs/concepts/sagas.md` | Semantic lock acquisition, SagaCommand wrapping, NOT_IN_SAGA compensation state | Yes | — |
| `docs/concepts/service.md` | Copy-on-write mutation pattern, factory usage, P3 service guard | Yes | — |
| `docs/concepts/commands.md` | Command base class, `super(unitOfWork, serviceName, aggregateId)` pattern, ServiceMapping | Yes | — |
| `docs/concepts/testing.md` | T2 test structure, step interleaving test pattern, sagaStateOf helper | Yes | — |
| `docs/concepts/rule-enforcement-patterns.md` | P3 guard pattern for TOURNAMENT_TOPIC_COURSE_EXECUTION check | Yes | — |

---

## Skill Instructions Feedback

### What worked well

- The lock-acquisition step pattern (SagaCommand wrapping GetXByIdCommand with setSemanticLock + compensation lambda) was consistently applicable across AddParticipant, UpdateTournament, CancelTournament, and DeleteTournament.
- The compensation lambda pattern `(a, u) -> tournamentService.setSemanticLockState(tournamentAggregateId, TournamentSagaState.NOT_IN_SAGA, u)` was clear from analogous aggregates.
- Copy-on-write mutation pattern for addParticipant, updateTournament, cancelTournament, deleteTournament was straightforward.

### What was unclear or missing

- session-b.md does not explicitly state that `GetXByIdCommand` must be created in session-b. The lock step depends on it, but the plan places it in session-c. Agents must infer this from reading the SagaCommand pattern description carefully.
- No guidance on what to do when a saga's update step needs to pass through sub-objects that are unavailable from the DTO returned by the prerequisite command. Specifically: `UpdateTournamentFunctionalitySagas` needs to update quiz but `QuizDto` only returns `questionIds` (not full `QuizQuestion` objects). The resolution — null-safe param, skip null in service — required design inference.
- No guidance on `ExecutionStudentDto` lacking a version field, requiring an additional `GetUserByIdCommand` step in CreateTournamentFunctionalitySagas and AddParticipantFunctionalitySagas.

### Suggested wording / structure changes

- In session-b.md, add an explicit note: "For aggregates with lock-acquisition steps, `GetXByIdCommand` must be created in this session even though it is listed under session-c. Note this as a plan.md addition."
- Add a note in session-b.md or docs/concepts/commands.md: "If an upstream DTO does not carry a version field needed by a mutation command, add an explicit `GetUserByIdCommand` step to fetch the version separately."

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-b.md` | No explicit rule that GetXByIdCommand must be created in session-b for lock steps | High | Add a note in the "Commands to create" section stating the prerequisite dependency |
| `docs/concepts/service.md` | No guidance on null-safe optional parameters in update services when sub-object DTO is incomplete | Medium | Add a pattern note: "When an update command carries null for a sub-collection because the caller cannot reconstruct it, guard the setter with `if (x != null)`" |
| `docs/concepts/commands.md` | No mention that `ExecutionStudentDto` lacks version field; CreateTournament and AddParticipant need extra user fetch step | Medium | Document known DTO gaps that require compensating command steps |

---

## Patterns to Capture

- **Pattern:** Null-safe optional sub-collection update
  **Observed in:** `QuizService.updateQuiz`, `UpdateTournamentFunctionalitySagas`
  **Description:** When updating an aggregate that contains a sub-collection, and the calling saga cannot reconstruct the full sub-objects from available DTOs, pass `null` for that parameter and guard the setter in the service with `if (param != null)`. Avoids fetching each sub-object individually when the update intent is limited to scalar fields.

- **Pattern:** Extra user-version fetch step in creation sagas
  **Observed in:** `CreateTournamentFunctionalitySagas`, `AddParticipantFunctionalitySagas`
  **Description:** When a saga needs the version field of a user but only has an `ExecutionStudentDto` (which lacks `version`), insert a dedicated `GetUserByIdCommand` step immediately after the enrollment-check step to retrieve the full `UserDto` with version. Name this step `getCreatorUserStep` or `getUserStep` as appropriate.

- **Pattern:** DeleteTournament invariant interaction with TOURNAMENT_IS_CANCELED
  **Observed in:** `TournamentService.deleteTournament`, `DeleteTournamentTest`
  **Description:** `deleteTournament` always calls `setParticipants(new HashSet<>())` before `remove()`. For cancelled tournaments with participants, `tournamentIsCanceled` P1 invariant fires because state changed while `prev.cancelled == true`. For cancelled tournaments with no participants, the setParticipants is a no-op so the invariant passes. This implements a guard: cancelled tournaments with participants cannot be deleted.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| High | `.claude/skills/implement-aggregate/session-b.md` | Add explicit note: GetXByIdCommand for lock steps must be created in session-b, even though plan lists it under session-c; record as plan.md addition |
| Medium | `docs/concepts/service.md` | Add null-safe optional sub-collection update pattern |
| Medium | `docs/concepts/commands.md` | Document that ExecutionStudentDto lacks version field; document compensating pattern (extra GetUserByIdCommand step) |

---

## One-Line Summary

The key structural challenge was that `ExecutionStudentDto` lacks a version field and `QuizDto` lacks full sub-object data, requiring compensating design decisions (extra user-fetch step; null-safe quiz update) that the docs do not cover.
