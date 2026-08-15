# Retro — 2.7.c — QuizAnswer

**App:** quizzes-full
**Session:** 2.7.c (Read Functionalities)
**Date:** 2026-05-16

---

## Files Produced

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/exception/QuizzesFullErrorMessage.java` (modified: added `QUIZ_ANSWER_NOT_FOUND`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/service/QuizAnswerService.java` (modified: appended `getQuizAnswerByQuizIdAndStudentId`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/quizanswer/GetQuizAnswerByQuizIdAndStudentIdCommand.java` (created)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/coordination/sagas/GetQuizAnswerByQuizIdAndStudentIdFunctionalitySagas.java` (created)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/coordination/functionalities/QuizAnswerFunctionalities.java` (modified: appended `getQuizAnswerByQuizIdAndStudentId`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/messaging/QuizAnswerCommandHandler.java` (modified: appended `GetQuizAnswerByQuizIdAndStudentIdCommand` case)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/quizanswer/GetQuizAnswerByQuizIdAndStudentIdTest.groovy` (created)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/plan.md` (modified: ticked 2.7.c)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | § Read method | Yes | Pattern clear; needed to adapt for lookup by composite key via custom repository |
| `docs/concepts/commands.md` | § Naming Conventions, § What a Command Is | Yes | — |
| `docs/concepts/sagas.md` | § Read Functionality Sagas | Yes | Single-step read saga template covered the case fully |
| `docs/concepts/testing.md` | § T2 Functionality Test, § Not-Found Assertions | Partial | Not-found note says `SimulatorException`, but for a by-composite-key read the not-found is detected at the service level (Optional empty) and throws `QuizzesFullException` — the doc generalisation does not cover this distinction |

---

## Skill Instructions Feedback

### What worked well

- The pre-emption check for `GetQuizAnswerByIdCommand` (already in session b) was clearly described and easy to apply.
- Session-c.md's "Always required" rules for `QuizAnswerFunctionalities` and `QuizAnswerCommandHandler` (even if not listed in the file table) are explicit and correct.
- The single-step read saga template in `docs/concepts/sagas.md` produced correct output without ambiguity.

### What was unclear or missing

- The not-found test guidance in `testing.md` says "assert `SimulatorException`" for not-found cases, but that is only accurate when using `aggregateLoadAndRegisterRead` with a non-existent primary ID. When the service first queries by composite key via the custom repository (Optional empty), the application exception is thrown instead. The skill sub-file does not clarify this distinction.

### Suggested wording / structure changes

- `docs/concepts/testing.md` § Not-Found Assertions: add a note distinguishing (a) infrastructure not-found via `aggregateLoadAndRegisterRead` → `SimulatorException`, from (b) service-level not-found via custom repository Optional empty → `QuizzesFullException`.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/testing.md` | Not-found assertion note only covers `aggregateLoadAndRegisterRead` path; silent about composite-key lookup path | Medium | Add note distinguishing the two not-found paths and which exception each throws |

---

## Patterns to Capture

- **Pattern:** Composite-key read service method via custom repository
  **Observed in:** `QuizAnswerService.getQuizAnswerByQuizIdAndStudentId`
  **Description:** When a read functionality queries by non-primary-key fields (e.g., quizId + userId), the service calls the custom repository's Optional-returning method; if empty, throws `QuizzesFullException`; if found, extracts the aggregateId and calls `aggregateLoadAndRegisterRead` to load the latest version and register the read in the UoW.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| Medium | `docs/concepts/testing.md` | Clarify not-found assertion: distinguish infrastructure (`SimulatorException`) vs. service-level (`QuizzesFullException`) not-found paths |

---

## One-Line Summary

The composite-key read for `GetQuizAnswerByQuizIdAndStudentId` required a two-step service lookup (repository Optional + `aggregateLoadAndRegisterRead`) and throws `QuizzesFullException` on empty Optional, which the testing doc's not-found guidance does not explicitly address.
