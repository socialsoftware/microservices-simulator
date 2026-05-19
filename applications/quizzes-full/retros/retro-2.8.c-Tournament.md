# Retro — 2.8.c — Tournament

**App:** quizzes-full
**Session:** 2.8.c (Read Functionalities)
**Date:** 2026-05-19

---

## Files Produced

List every file created or modified this session (absolute paths).

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/tournament/GetOpenTournamentsCommand.java` (created)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/coordination/sagas/GetOpenTournamentsFunctionalitySagas.java` (created)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/coordination/sagas/GetTournamentByIdFunctionalitySagas.java` (created)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/coordination/functionalities/TournamentFunctionalities.java` (modified: added `getOpenTournaments`, upgraded stub `getTournamentById`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/messaging/TournamentCommandHandler.java` (modified: added `GetOpenTournamentsCommand` case)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/tournament/GetOpenTournamentsTest.groovy` (created)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/tournament/GetTournamentByIdTest.groovy` (created)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/plan.md` (checkbox ticked)

Bugs fixed during session (in earlier-session files):

- `/Users/frleitao/thesis/microservices-simulator/simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/messaging/MessagingObjectMapperProvider.java` (two fixes: removed Dto exclusion; added `Object.class → true` for per-element type info in untyped collections)
- `/Users/frleitao/thesis/microservices-simulator/simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/WorkflowFunctionality.java` (added CompletionException unwrapping in `executeUntilStep`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/aggregate/TournamentRepository.java` (added `findAllLatestActive()` JPQL query)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/aggregate/sagas/repositories/TournamentCustomRepositorySagas.java` (use `findAllLatestActive()` to get only latest version per aggregate)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/service/ExecutionService.java` (throw `QuizzesFullException(COURSE_EXECUTION_STUDENT_NOT_FOUND)` instead of returning null)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/service/TournamentService.java` (throw exception without format args for topic mismatch; add explicit check for already-cancelled tournament)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/coordination/sagas/CreateTournamentFunctionalitySagas.java` (added `validateDatesStep` as first step to check startTime/endTime before quiz creation)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/tournament/DeleteTournamentTest.groovy` (updated success tests to verify deleted aggregate is inaccessible)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/execution/GetStudentByExecutionIdAndUserIdTest.groovy` (updated "returns null" test to "throws")
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/execution/ExecutionInterInvariantTest.groovy` (updated student-removed check to expect exception)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | § Read functionalities | Partial | No guidance on list-return saga (single vs two-step) |
| `.claude/skills/implement-aggregate/session-c.md` | All | Partial | Missing list-return variant pattern |

---

## Skill Instructions Feedback

### What worked well

- The pre-emption check for `GetTournamentByIdCommand` (already done in 2.8.b) was identified correctly
- The stub-upgrade pattern for `getTournamentById` in `TournamentFunctionalities` was clear

### What was unclear or missing

- No guidance on whether a list-return read saga needs two steps (resolve foreign IDs) or one (data already on aggregate). Had to infer from domain model.
- No guidance on `local.messaging.serialize: true` interaction with `CommandResponse.result` typed as `Object` (list element type erasure causes `LinkedHashMap` deserialization).
- No guidance on `MessagingObjectMapperProvider.useForType()` needing `Object.class → true` to support untyped container fields.
- No guidance that `executeUntilStep` in `WorkflowFunctionality` does not unwrap `CompletionException` (unlike `executeWorkflow`).
- No guidance that `TournamentCustomRepositorySagas.findAll()` returns all versions — queries must use a "latest active version per aggregateId" approach.
- No guidance that exceptions should be thrown WITHOUT format args (raw constant string), because test assertions use `ex.message == CONSTANT`.

### Suggested wording / structure changes

- Add a note in `session-c.md` about list-return sagas: when all filter data is on the aggregate itself, one step is sufficient; a second step is only needed if a foreign ID must be resolved.
- Add a note about the `local.messaging.serialize` flag and its Jackson implications.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/service.md` | No guidance on list-return read saga pattern | Medium | Add one-step vs two-step guidance based on whether foreign IDs need resolving |
| `docs/concepts/testing.md` | `local.messaging.serialize: true` Jackson behavior not documented | High | Document that `Object`-typed fields in `CommandResponse` cause type erasure; requires `MessagingObjectMapperProvider.useForType(Object.class) → true` |
| `docs/concepts/service.md` or `commands.md` | Exception throw convention: always use raw constant (no format args) so `ex.message == CONSTANT` tests pass | High | Add a note: throw `new XxxException(CONSTANT)` not `new XxxException(CONSTANT, id)` unless the message has no `%` placeholders |
| `docs/concepts/service.md` | Repository queries must operate on latest-active version per aggregateId; `findAll()` returns all versions | Medium | Document the JPQL pattern: `where state = 'ACTIVE' and version = (select max(...))` |
| `simulator/WorkflowFunctionality.java` | `executeUntilStep` did not unwrap CompletionException unlike `executeWorkflow` | Medium | Fixed in this session; document that both methods should propagate domain exceptions cleanly |

---

## Patterns to Capture

- **Pattern:** List-return read saga (single step)
  **Observed in:** `GetOpenTournamentsFunctionalitySagas.java`
  **Description:** When all filter criteria are stored directly on the aggregate (e.g., `executionAggregateId` on Tournament), a single-step read saga suffices. No foreign ID resolution step needed.

- **Pattern:** Latest-active-version JPQL query for custom repository
  **Observed in:** `TournamentRepository.findAllLatestActive()`
  **Description:** `select t from T t where t.state = 'ACTIVE' and t.version = (select max(t2.version) from T t2 where t2.aggregateId = t.aggregateId)` — required for any bulk query that must reflect current state, since `findAll()` returns all versions.

- **Pattern:** Early validation step before write steps in saga
  **Observed in:** `CreateTournamentFunctionalitySagas.validateDatesStep`
  **Description:** Domain constraints that would otherwise be checked only during a write aggregate (e.g., `Tournament.verifyInvariants`) must be checked BEFORE any earlier write steps that can fail with a different exception first (e.g., quiz date validation fires before tournament validation).

- **Pattern:** Exception constant convention — no format args
  **Observed in:** `ExecutionService`, `TournamentService`
  **Description:** Test assertions use `ex.message == CONSTANT` (raw format string). Exceptions must be thrown as `new XxxException(CONSTANT)` without formatting args, so `getMessage()` returns the literal constant string including any `%d` placeholders.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| High | `docs/concepts/testing.md` | Document `local.messaging.serialize: true` behavior: `CommandResponse.result` typed as `Object` loses element type info; requires `MessagingObjectMapperProvider.useForType(Object.class) → true` |
| High | `docs/concepts/service.md` | Document exception-throw convention: use raw constant (no format args) |
| Medium | `.claude/skills/implement-aggregate/session-c.md` | Add list-return saga guidance: one step when filter data is on aggregate; two steps when foreign ID must be resolved |
| Medium | `docs/concepts/service.md` | Document JPQL latest-active-version query pattern for custom repository bulk reads |
| Low | `.claude/skills/implement-aggregate/session-b.md` | Note that write sagas using `validateDatesStep` may be needed before quiz-creation steps to prevent quiz date invariant masking tournament date invariant |

---

## One-Line Summary

The session exposed several systemic gaps: Jackson type erasure in `CommandResponse.result`, missing exception-throw convention (no format args), `executeUntilStep` not unwrapping `CompletionException`, and custom repository queries returning all versions instead of latest-active — all required framework and service fixes before the read functionalities could be tested.
