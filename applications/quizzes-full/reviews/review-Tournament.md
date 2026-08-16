# Review — Tournament

**App:** quizzes-full
**Aggregate:** Tournament (aggregate #8 in plan.md)
**Date:** 2026-05-20
**Verdict:** Yellow

> **Green** = all structural checks pass, all planned operations implemented, tests present and correct, build passes.
> **Yellow** = minor deviations or missing test coverage; build passes.
> **Red** = missing operations, incorrect patterns, broken build, or multiple high-severity issues.

---

## Summary

The Tournament aggregate is structurally sound and builds cleanly with all 43 tests passing. All eight planned operations are implemented end-to-end (service, command, saga, functionalities, tests). However, four Major findings require follow-up: the CREATOR_IS_NOT_ANONYMOUS intra-invariant listed in plan.md §3.1 is absent from `Tournament.verifyInvariants()`; `getEventSubscriptions()` lacks the `state == ACTIVE` guard present in the reference implementation; and two P1 intra-invariants (TOURNAMENT_ENROLL_UNTIL_START_TIME and TOURNAMENT_FINAL_AFTER_START) have no corresponding T2 test cases. Three Minor deviations are noted but are justified or inconsequential.

---

## File Inventory

| File (relative to microservices/tournament/) | In Reference | In Target | Status | Notes |
|---------------------------------------------|-------------|-----------|--------|-------|
| `aggregate/Tournament.java` | Yes | Yes | OK | — |
| `aggregate/TournamentTopic.java` | Yes | Yes | OK | — |
| `aggregate/TournamentParticipant.java` | Yes | Yes | OK | — |
| `aggregate/TournamentParticipantQuizAnswer.java` | Yes | Yes | OK | — |
| `aggregate/TournamentFactory.java` | Yes | Yes | OK | — |
| `aggregate/TournamentCustomRepository.java` | Yes | Yes | OK | — |
| `aggregate/TournamentDto.java` | Yes | Yes | OK | — |
| `aggregate/TournamentRepository.java` | Yes | Yes | OK | — |
| `aggregate/TournamentCreator.java` | Yes | No | Intentional | Target inlines creator fields directly in Tournament.java (creatorAggregateId/Name/Username/Version as direct columns); design simplification |
| `aggregate/TournamentCourseExecution.java` | Yes | No | Intentional | Target inlines execution fields directly in Tournament.java (executionAggregateId/Version); design simplification |
| `aggregate/TournamentQuiz.java` | Yes | No | Intentional | Target inlines quiz fields directly in Tournament.java (quizAggregateId/Version); design simplification |
| `aggregate/sagas/SagaTournament.java` | Yes | Yes | OK | — |
| `aggregate/sagas/states/TournamentSagaState.java` | Yes | Yes | OK | — |
| `aggregate/sagas/factories/SagasTournamentFactory.java` | Yes | Yes | OK | — |
| `aggregate/sagas/repositories/TournamentCustomRepositorySagas.java` | Yes | Yes | OK | — |
| `service/TournamentService.java` | Yes | Yes | OK | — |
| `messaging/TournamentCommandHandler.java` | Yes | Yes | OK | — |
| `coordination/functionalities/TournamentFunctionalities.java` | Yes | Yes | OK | — |
| `coordination/sagas/CreateTournamentFunctionalitySagas.java` | Yes | Yes | OK | — |
| `coordination/sagas/AddParticipantFunctionalitySagas.java` | Yes | Yes | OK | — |
| `coordination/sagas/UpdateTournamentFunctionalitySagas.java` | Yes | Yes | OK | — |
| `coordination/sagas/CancelTournamentFunctionalitySagas.java` | Yes | Yes | OK | — |
| `coordination/sagas/DeleteTournamentFunctionalitySagas.java` | Yes | Yes | OK | — |
| `coordination/sagas/SolveQuizFunctionalitySagas.java` | Yes (via reference SolveQuizFunctionalitySagas) | Yes | OK | Added to plan in 2.8.d follow-up |
| `coordination/sagas/GetOpenTournamentsFunctionalitySagas.java` | Yes | Yes | OK | — |
| `coordination/sagas/GetTournamentByIdFunctionalitySagas.java` | Yes | Yes | OK | — |
| `coordination/eventProcessing/TournamentEventProcessing.java` | Yes | Yes | OK | — |
| `coordination/webapi/TournamentController.java` | Yes | Yes | OK | — |
| `notification/handling/TournamentEventHandling.java` | Yes | Yes | OK | — |
| `notification/handling/handlers/TournamentEventHandler.java` | Yes | Yes | OK | Consolidated from 9 individual handler classes in reference into one |
| `notification/subscribe/TournamentSubscribesDeleteUser.java` | Yes | Yes | OK | — |
| `notification/subscribe/TournamentSubscribesUpdateStudentName.java` | Yes | Yes | OK | — |
| `notification/subscribe/TournamentSubscribesAnonymizeStudent.java` | Yes | Yes | OK | — |
| `notification/subscribe/TournamentSubscribesUpdateTopic.java` | Yes | Yes | OK | — |
| `notification/subscribe/TournamentSubscribesDeleteTopic.java` | Yes | Yes | OK | — |
| `notification/subscribe/TournamentSubscribesDeleteCourseExecution.java` | Yes | Yes | OK | — |
| `notification/subscribe/TournamentSubscribesInvalidateQuiz.java` | Yes | Yes | OK | — |
| `notification/subscribe/TournamentSubscribesQuizAnswerQuestionAnswer.java` | Yes | Yes | OK | — |
| `aggregate/causal/` subtree | Yes | No | Intentional | quizzes-full is sagas-only |
| `coordination/causal/` subtree | Yes | No | Intentional | quizzes-full is sagas-only |
| `commands/tournament/*.java` (8 files) | Yes | Yes | OK | — |
| `TournamentServiceApplication.java` | Yes | Yes | OK | — |

---

## Structural Review

### `aggregate/Tournament.java`

**`@Entity` and base class:** `@Entity public abstract class Tournament extends Aggregate` — Correct.

**P1 final fields:**
- `private final Integer executionAggregateId` — TOURNAMENT_COURSE_EXECUTION_IS_FINAL. Correct.
- `private final Integer creatorAggregateId` — TOURNAMENT_CREATOR_IS_FINAL. Correct.
- `private final Integer quizAggregateId` — TOURNAMENT_QUIZ_IS_FINAL. Correct.

**`verifyInvariants()`:** Throws `QuizzesFullException` for seven rules — all present and correct:
- `TOURNAMENT_START_BEFORE_END_TIME`: `startBeforeEndTime()` — Correct.
- `TOURNAMENT_UNIQUE_AS_PARTICIPANT`: `uniqueParticipants()` — Correct.
- `TOURNAMENT_ENROLL_UNTIL_START_TIME`: `enrollUntilStartTime()` — Correct.
- `TOURNAMENT_FINAL_AFTER_START`: `tournamentFinalAfterStart()` — Correct.
- `TOURNAMENT_IS_CANCELED`: `tournamentIsCanceled()` — Correct.
- `TOURNAMENT_DELETE`: `tournamentDeleteHasNoParticipants()` — Correct.
- `TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY`: `creatorParticipantConsistency()` — Correct.

**CREATOR_IS_NOT_ANONYMOUS — missing:** plan.md §3.1 classifies this as P1 intra-invariant: "intra-invariant in `Tournament.verifyInvariants()` — checks cached `creatorName` and `creatorUsername`; fires after `AnonymizeStudentEvent` updates cached values via P2". No such check exists in `verifyInvariants()`. The T3 test for `AnonymizeStudentEvent` expects anonymization to succeed (creatorName set to "ANONYMOUS"), which is inconsistent with an invariant that throws when the creator is anonymous. The plan classification may require reconciliation (either add the check and update the T3 test, or reclassify the rule in plan.md).

**`getEventSubscriptions()`:** Builds subscriptions for all six P2 invariants (DeleteUserEvent/UpdateStudentNameEvent/AnonymizeStudentEvent per creator + per participant, UpdateTopicEvent/DeleteTopicEvent per topic, DeleteCourseExecutionEvent, InvalidateQuizEvent, QuizAnswerQuestionAnswerEvent conditionally when `quizAnswerAggregateId != null`). Correct.

**Missing `state == ACTIVE` guard:**
Expected (per reference `Tournament.java:154-165`):
```java
public Set<EventSubscription> getEventSubscriptions() {
    Set<EventSubscription> eventSubscriptions = new HashSet<>();
    if (this.getState() == AggregateState.ACTIVE) {
        ...build subscriptions...
    }
    return eventSubscriptions;
}
```
Actual (quizzes-full `Tournament.java:257-283`):
```java
public Set<EventSubscription> getEventSubscriptions() {
    Set<EventSubscription> subscriptions = new HashSet<>();
    // always builds subscriptions regardless of state
    subscriptions.add(new TournamentSubscribesDeleteUser(creatorAggregateId, creatorVersion));
    ...
    return subscriptions;
}
```
Status: **Incorrect** — deleted/inactive tournaments will return subscriptions, potentially causing repeated event processing on already-deleted aggregates.

**Copy constructor:** Copies all fields including `topics` (deep copy) and `participants` (deep copy via `TournamentParticipant(TournamentParticipant)` copy constructor). Correct.

---

### `aggregate/sagas/SagaTournament.java`

- Extends `Tournament`, implements `SagaAggregate`. Correct.
- `sagaState` typed as `SagaState` (interface). Correct.
- Default constructor: `this.sagaState = GenericSagaState.NOT_IN_SAGA`. Correct.
- Copy constructor: `this.sagaState = GenericSagaState.NOT_IN_SAGA` (resets, does not copy). Correct.
- Status: **Correct**.

---

### `aggregate/sagas/states/TournamentSagaState.java`

- Implements `SagaAggregate.SagaState`. Correct.
- Contains: `IN_ADD_PARTICIPANT`, `IN_UPDATE_TOURNAMENT`, `IN_CANCEL_TOURNAMENT`, `IN_DELETE_TOURNAMENT`. Correct.
- CreateTournament creates a new aggregate (no lock needed) — no `IN_CREATE_TOURNAMENT`. Correct.
- SolveQuiz reads Tournament without locking (single read-then-write, not a two-step lock) — no `IN_SOLVE_QUIZ`. Correct.
- Status: **Correct**.

---

### `aggregate/sagas/factories/SagasTournamentFactory.java`

- `@Service @Profile("sagas")` implements `TournamentFactory`. Correct.
- `createTournament(...)`: delegates to `SagaTournament(...)` constructor. Correct.
- `createTournamentCopy(Tournament existing)`: casts to `SagaTournament` and calls copy constructor. Correct (matches pattern used by all other sagas-profile factories).
- `createTournamentDto(Tournament)`: delegates to `TournamentDto(tournament)`. Correct.
- Status: **Correct**.

---

### `aggregate/sagas/repositories/TournamentCustomRepositorySagas.java`

- `@Service @Profile("sagas")` implements `TournamentCustomRepository`. Correct.
- `@Autowired TournamentRepository`. Correct.
- `getOpenTournamentsByExecutionId`: uses `findAllLatestActive()` to restrict to latest version per aggregateId, then filters by `!cancelled`, `executionAggregateId`, and `endTime.isAfter(now)`. Correct (the `findAllLatestActive` pattern was added in retro-2.8.c to fix all-version return).
- Status: **Correct**.

---

### `service/TournamentService.java`

- `@Service`, constructor-injected `unitOfWorkService` and `tournamentRepository`, `@Autowired tournamentFactory`. Correct.
- All methods have `@Transactional(isolation = Isolation.SERIALIZABLE)`. Correct.
- `createTournament`: generates ID via `aggregateIdGeneratorService`, calls factory, calls `registerChanged`. Correct.
- `addParticipant`, `updateTournament`, `cancelTournament`: load → copy via factory → mutate copy → `registerChanged` on copy. Correct copy-on-write pattern.
- `deleteTournament` — **Pattern deviation:**
  Expected (docs/concepts/service.md): `oldTournament.remove(); unitOfWorkService.registerChanged(oldTournament, unitOfWork);`
  Actual:
  ```java
  Tournament newTournament = tournamentFactory.createTournamentCopy(oldTournament);
  newTournament.setParticipants(new HashSet<>());
  newTournament.remove();
  unitOfWorkService.registerChanged(newTournament, unitOfWork);
  ```
  Status: **Minor deviation** — intentional (documented in retro-2.8.b: clearing participants before removal is required for the TOURNAMENT_DELETE / TOURNAMENT_IS_CANCELED invariant interaction; pure in-place removal without clearing participants would fail verifyInvariants() for non-cancelled tournaments with participants).
- P3 guard (TOPIC_COURSE_EXECUTION) in `createTournament`: present and correct.
- ByEvent service methods (9 total, including `setParticipantQuizAnswer`): all present, follow copy-on-write pattern. Correct.
- Status: **Minor deviation** (deleteTournament pattern only).

---

### `messaging/TournamentCommandHandler.java`

- `@Component`, extends `CommandHandler`. Correct.
- `getAggregateTypeName()` returns `"Tournament"`. Correct.
- `handleDomainCommand` switch covers: `GetTournamentByIdCommand`, `GetOpenTournamentsCommand`, `CreateTournamentCommand`, `AddParticipantCommand`, `UpdateTournamentCommand`, `CancelTournamentCommand`, `DeleteTournamentCommand`, `SolveQuizCommand`. All 8 commands covered. Correct.
- Default branch logs a warning. Correct.
- Status: **Correct**.

---

### `coordination/functionalities/TournamentFunctionalities.java`

- `@Service`. Correct.
- Saga-based methods (8): each derives functionality name, creates `SagaUnitOfWork`, instantiates saga inline with `new`, calls `executeWorkflow`, returns result from getter. Correct.
- ByEvent methods (8): call service directly then `unitOfWorkService.commit(unitOfWork)`. Correct pattern for event handlers (no saga wrapping needed).
- Status: **Correct**.

---

### Coordination sagas

**CreateTournamentFunctionalitySagas:** Multi-step saga (8 steps): validateDates → getExecution → getStudent (P4a) → getCreatorUser → getTopics → getQuestions → createQuiz → createTournament. No lock acquisition (creates new aggregate). P4b satisfied: same executionId/startTime/endTime/questions passed to Quiz and Tournament. Correct.

**AddParticipantFunctionalitySagas:** getUserStep → getStudentStep (P4a) → getTournamentStep (SagaCommand + `setSemanticLock(IN_ADD_PARTICIPANT)` + compensation) → addParticipantStep. Two-step lock pattern correct.

**UpdateTournamentFunctionalitySagas:** getTournamentStep (lock + compensation) → getTopicsStep → updateTournamentStep → updateQuizStep. Correct. Note: `topicDtos.isEmpty() ? null : topicDtos` handles optional topic update via null-safe guard in service.

**CancelTournamentFunctionalitySagas / DeleteTournamentFunctionalitySagas:** Standard two-step lock pattern. Correct.

**SolveQuizFunctionalitySagas:** getTournamentStep (read, no lock) → getQuizAnswerStep → solveQuizStep. No semantic lock on Tournament — correct since this saga reads then writes without needing exclusive hold.

**GetOpenTournamentsFunctionalitySagas / GetTournamentByIdFunctionalitySagas:** Single-step read sagas. Correct.

---

### Notification files

**TournamentEventHandling.java:** `@Component`, 8 `@Scheduled` handlers covering all subscribed event types. Correct.

**TournamentEventHandler.java:** `@Component extends EventHandler`, dispatches all 8 event types to `TournamentEventProcessing`. Correct (reference uses separate handler classes per event type; target consolidates into one — functionally equivalent).

**TournamentEventProcessing.java:** `@Service`, delegates all 8 event types to corresponding `TournamentFunctionalities` ByEvent methods. Correct.

**Subscribe classes:** All 8 present and match expected events from plan.md. Correct.

---

## Functionality Coverage

| Operation | In Service | Saga Class | CommandHandler Case | Functionalities Method | T2 Test | Notes |
|-----------|-----------|-----------|--------------------|-----------------------|---------|-------|
| CreateTournament | Yes | Yes | Yes | Yes | Yes | — |
| AddParticipant | Yes | Yes | Yes | Yes | Yes | — |
| SolveQuiz | Yes | Yes | Yes | Yes | No dedicated T2 | Tested via T3 setup; plan didn't list SolveQuizTest.groovy |
| UpdateTournament | Yes | Yes | Yes | Yes | Yes | — |
| CancelTournament | Yes | Yes | Yes | Yes | Yes | — |
| DeleteTournament | Yes | Yes | Yes | Yes | Yes | — |
| GetOpenTournaments | Yes | Yes | Yes | Yes | Yes | — |
| GetTournamentById | Yes | Yes | Yes | Yes | Yes | — |

---

## Rule Enforcement

| Rule | Classification | Expected Impl | Actual Impl | Status |
|------|---------------|--------------|-------------|--------|
| TOURNAMENT_START_BEFORE_END_TIME | P1 intra-invariant | `verifyInvariants()` throws | Present: `startBeforeEndTime()` check | Correct |
| TOURNAMENT_UNIQUE_AS_PARTICIPANT | P1 intra-invariant | `verifyInvariants()` throws | Present: `uniqueParticipants()` check | Correct |
| TOURNAMENT_ENROLL_UNTIL_START_TIME | P1 intra-invariant | `verifyInvariants()` throws | Present: `enrollUntilStartTime()` check | Correct |
| TOURNAMENT_FINAL_AFTER_START | P1 intra-invariant | `verifyInvariants()` throws | Present: `tournamentFinalAfterStart()` check | Correct |
| TOURNAMENT_IS_CANCELED | P1 intra-invariant | `verifyInvariants()` throws | Present: `tournamentIsCanceled()` check | Correct |
| TOURNAMENT_DELETE | P1 intra-invariant | `verifyInvariants()` throws | Present: `tournamentDeleteHasNoParticipants()` check | Correct |
| TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY | P1 intra-invariant | `verifyInvariants()` throws | Present: `creatorParticipantConsistency()` check | Correct |
| CREATOR_IS_NOT_ANONYMOUS | P1 intra-invariant | `verifyInvariants()` throws when creatorName is anonymous | Absent from `verifyInvariants()` | **Incorrect** |
| TOURNAMENT_CREATOR_IS_FINAL | P1 final-field | Java `final` field | `private final Integer creatorAggregateId` | Correct |
| TOURNAMENT_COURSE_EXECUTION_IS_FINAL | P1 final-field | Java `final` field | `private final Integer executionAggregateId` | Correct |
| TOURNAMENT_QUIZ_IS_FINAL | P1 final-field | Java `final` field | `private final Integer quizAggregateId` | Correct |
| CREATOR_EXISTS / PARTICIPANT_EXISTS (DeleteUserEvent) | P2 | Subscription in `getEventSubscriptions()` + handler | TournamentSubscribesDeleteUser per creator + per participant | Correct |
| CREATOR_EXISTS / PARTICIPANT_EXISTS (UpdateStudentNameEvent) | P2 | Subscription + handler | TournamentSubscribesUpdateStudentName per creator + per participant | Correct |
| CREATOR_EXISTS / PARTICIPANT_EXISTS (AnonymizeStudentEvent) | P2 | Subscription + handler | TournamentSubscribesAnonymizeStudent per creator + per participant | Correct |
| TOPIC_EXISTS (UpdateTopicEvent) | P2 | Subscription + handler | TournamentSubscribesUpdateTopic per topic | Correct |
| TOPIC_EXISTS (DeleteTopicEvent) | P2 | Subscription + handler | TournamentSubscribesDeleteTopic per topic | Correct |
| COURSE_EXECUTION_EXISTS (DeleteCourseExecutionEvent) | P2 | Subscription + handler | TournamentSubscribesDeleteCourseExecution | Correct |
| QUIZ_EXISTS (InvalidateQuizEvent) | P2 | Subscription + handler | TournamentSubscribesInvalidateQuiz | Correct |
| QUIZ_ANSWER_EXISTS (QuizAnswerQuestionAnswerEvent) | P2 | Subscription + handler | TournamentSubscribesQuizAnswerQuestionAnswer (conditional on `quizAnswerAggregateId != null`) | Correct |
| TOPIC_COURSE_EXECUTION | P3 | Service guard in `createTournament` | Present: `for (TopicDto topic : topicDtos) { if (!topic.getCourseId().equals(executionCourseId)) { throw ... } }` | Correct |
| CREATOR_COURSE_EXECUTION | P4a | `getStudentStep` in CreateTournamentFunctionalitySagas | `GetStudentByExecutionIdAndUserIdCommand` — throws if not enrolled | Correct |
| PARTICIPANT_COURSE_EXECUTION | P4a | `getStudentStep` in AddParticipantFunctionalitySagas | `GetStudentByExecutionIdAndUserIdCommand` — throws if not enrolled | Correct |
| QUIZ_COURSE_EXECUTION_CONSISTENCY | P4b | Same `executionId` to Tournament and Quiz | `createTournamentStep` passes `executionDto.getAggregateId()` to both | Correct |
| START_TIME_AVAILABLE_DATE / END_TIME_CONCLUSION_DATE | P4b | Same times to Tournament and Quiz | `startTime`/`endTime` passed identically to both `CreateTournamentCommand` and `CreateQuizCommand` | Correct |
| NUMBER_OF_QUESTIONS / QUIZ_TOPICS | P4b | Same questions/count to both | `quizQuestions` derived from topic+question fetch; `numberOfQuestions` from caller | Correct |

---

## Test Coverage

| Test Type | Class | Scenarios Present | Missing Scenarios | Notes |
|-----------|-------|------------------|-------------------|-------|
| T1 | `TournamentTest` | "create tournament" (valid data) | — | Correct; no violation cases expected in T1 |
| T2 | `CreateTournamentTest` | success; CREATOR_COURSE_EXECUTION violation; TOPIC_COURSE_EXECUTION (P3) violation; TOURNAMENT_START_BEFORE_END_TIME violation; getStudentStep interleaving | — | No lock step in CreateTournament so no lock-acquisition interleaving needed |
| T2 | `AddParticipantTest` | success; PARTICIPANT_COURSE_EXECUTION violation; TOURNAMENT_UNIQUE_AS_PARTICIPANT violation; getTournamentStep lock interleaving; TOURNAMENT_IS_CANCELED violation | **TOURNAMENT_ENROLL_UNTIL_START_TIME** (enroll after startTime) | Missing scenario per testing.md: one case per P1 intra-invariant |
| T2 | `UpdateTournamentTest` | success; TOURNAMENT_START_BEFORE_END_TIME violation; TOURNAMENT_IS_CANCELED violation; getTournamentStep lock interleaving | **TOURNAMENT_FINAL_AFTER_START** (update after tournament started) | Missing scenario per testing.md: one case per P1 intra-invariant |
| T2 | `CancelTournamentTest` | success; TOURNAMENT_IS_CANCELED violation (already cancelled); getTournamentStep lock interleaving | — | Correct |
| T2 | `DeleteTournamentTest` | success no-participants; success cancelled no-participants; TOURNAMENT_IS_CANCELED violation (cancelled with participants); getTournamentStep lock interleaving | — | TOURNAMENT_DELETE invariant is tested via the cancelled+participants case |
| T2 | `GetTournamentByIdTest` | success; not-found (`thrown(SimulatorException)`) | — | Not-found correctly uses SimulatorException, not QuizzesFullException |
| T2 | `GetOpenTournamentsTest` | success (open tournament returned); cancelled not returned; different execution returns empty | — | Correct |
| T3 | `TournamentInterInvariantTest` | DeleteUserEvent (2); UpdateStudentNameEvent (2); AnonymizeStudentEvent (2); UpdateTopicEvent (2); DeleteTopicEvent (2); DeleteCourseExecutionEvent (2); InvalidateQuizEvent (2); QuizAnswerQuestionAnswerEvent (2) = 16 scenarios | — | All deletion-event not-found assertions correctly use `thrown(SimulatorException)` |

---

## Retro Cross-Reference

| Retro | Session | Priority | Action Item |
|-------|---------|----------|-------------|
| retro-2.8.a | 2.8.a | High | Fix "Always includes NOT_IN_SAGA" in session-a.md SagaState section |
| retro-2.8.a | 2.8.a | Medium | Add nested OneToOne entity note in session-a.md |
| retro-2.8.a | 2.8.a | Low | Pre-list topic-snapshot entity in classify-and-plan |
| retro-2.8.b | 2.8.b | High | Add GetXByIdCommand must be created in session-b note |
| retro-2.8.b | 2.8.b | Medium | Add null-safe optional sub-collection update pattern in service.md |
| retro-2.8.b | 2.8.b | Medium | Document ExecutionStudentDto lacks version field in commands.md |
| retro-2.8.c | 2.8.c | High | Document local.messaging.serialize Jackson behavior in testing.md |
| retro-2.8.c | 2.8.c | High | Document exception-throw convention (no format args) in service.md |
| retro-2.8.c | 2.8.c | Medium | Add list-return saga guidance in session-c.md |
| retro-2.8.c | 2.8.c | Medium | Document latest-active-version JPQL pattern in service.md |
| retro-2.8.d | 2.8.d | Medium | Add conditional subscription note in session-d.md |
| retro-2.8.d | 2.8.d | Low | Note ByEvent deletion on invariant-constrained aggregates in session-d.md |
| retro-2.8.d | 2.8.d | ~~Done~~ | SolveQuizFunctionalitySagas + SolveQuizCommand added; TournamentInterInvariantTest uses tournamentFunctionalities.solveQuiz() |

All High and Medium priority retro items target skill or doc files, not source files. No source-file-targeted High-priority items remain unresolved. The "Done" item in retro-2.8.d is verified: `SolveQuizFunctionalitySagas.java` is present and `TournamentInterInvariantTest` calls `tournamentFunctionalities.solveQuiz(tournamentId, creatorId)` at line 309.

---

## Build & Test Results

**Command:** `mvn clean -Ptest-sagas test -Dtest="TournamentTest,CreateTournamentTest,AddParticipantTest,UpdateTournamentTest,CancelTournamentTest,DeleteTournamentTest,GetOpenTournamentsTest,GetTournamentByIdTest,TournamentInterInvariantTest"`
**Outcome:** BUILD SUCCESS

| Test class | Result | Failures |
|------------|--------|---------|
| TournamentTest | PASS | 0 |
| CreateTournamentTest | PASS | 0 |
| AddParticipantTest | PASS | 0 |
| UpdateTournamentTest | PASS | 0 |
| CancelTournamentTest | PASS | 0 |
| DeleteTournamentTest | PASS | 0 |
| GetOpenTournamentsTest | PASS | 0 |
| GetTournamentByIdTest | PASS | 0 |
| TournamentInterInvariantTest | PASS | 0 |

Total: 43 tests run, 0 failures, 0 errors.

---

## Action Items

| Priority | Category | File | Finding | Fix |
|----------|---------|------|---------|-----|
| Major | Rule enforcement | `aggregate/Tournament.java` | CREATOR_IS_NOT_ANONYMOUS listed in plan.md §3.1 as P1 intra-invariant (`verifyInvariants()`) but absent from code | Either add `if (creatorName != null && creatorName.equals("ANONYMOUS")) throw ...` to `verifyInvariants()` and update the T3 AnonymizeStudentEvent test to expect a deletion, OR reclassify the rule in plan.md if the intended behaviour is that anonymization succeeds without invariant violation |
| Major | Structural pattern | `aggregate/Tournament.java` | `getEventSubscriptions()` lacks `if (getState() == AggregateState.ACTIVE)` guard present in reference; deleted/inactive tournaments will return subscriptions on every load | Wrap subscription-building in `if (getState() == AggregateState.ACTIVE) { ... }` |
| Major | Test coverage | `sagas/coordination/tournament/AddParticipantTest.groovy` | No test for TOURNAMENT_ENROLL_UNTIL_START_TIME — missing case: attempt to enroll a participant after `startTime` has passed | Add scenario: create tournament with `startTime = now().minusMinutes(1)`; call `addParticipant`; expect `thrown(QuizzesFullException)` with `ex.message == TOURNAMENT_ENROLL_UNTIL_START_TIME` |
| Major | Test coverage | `sagas/coordination/tournament/UpdateTournamentTest.groovy` | No test for TOURNAMENT_FINAL_AFTER_START — missing case: attempt to update a tournament after `startTime` has passed | Add scenario: create tournament with `startTime = now().minusMinutes(1)`; call `updateTournament` with new times; expect `thrown(QuizzesFullException)` with `ex.message == TOURNAMENT_FINAL_AFTER_START` |
| Minor | Service pattern | `service/TournamentService.java:142-153` | `deleteTournament` uses copy-on-write instead of in-place mutation prescribed by docs/concepts/service.md for delete | Intentional: clearing participants before `remove()` is required to satisfy TOURNAMENT_DELETE invariant without triggering TOURNAMENT_IS_CANCELED for cancelled+participant tournaments. Document this exception in docs/concepts/service.md under the delete pattern section |
| Minor | Design difference | `aggregate/Tournament.java` | Reference uses separate entities TournamentCreator, TournamentCourseExecution, TournamentQuiz with `@OneToOne`; target inlines their fields directly | No correctness issue; design simplification. No action needed unless inter-aggregate comparison requires structural parity |
| Minor | Test coverage | N/A | No dedicated `SolveQuizTest.groovy` for the SolveQuiz write operation | Plan.md did not list SolveQuizTest.groovy in produced files. SolveQuiz is tested via TournamentInterInvariantTest setup. Consider adding a dedicated T2 test class if the operation needs standalone coverage |
