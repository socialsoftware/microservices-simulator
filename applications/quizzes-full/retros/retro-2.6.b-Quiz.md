# Retro — 2.6.b — Quiz

**App:** quizzes-full
**Session:** 2.6.b (Write Functionalities)
**Date:** 2026-05-11

---

## Files Produced

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/quiz/GetQuizByIdCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/quiz/CreateQuizCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/quiz/UpdateQuizCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/service/QuizService.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/messaging/QuizCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/coordination/sagas/CreateQuizFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/coordination/sagas/UpdateQuizFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/coordination/functionalities/QuizFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/coordination/webapi/QuizController.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/aggregate/sagas/states/QuizSagaState.java` (modified: added IN_UPDATE_QUIZ)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/quiz/CreateQuizTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/quiz/UpdateQuizTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/BeanConfigurationSagas.groovy` (modified: added Quiz service/handler/functionalities beans)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/QuizzesFullSpockTest.groovy` (modified: added quizFunctionalities and createQuiz helper)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/plan.md` (modified: ticked 2.6.b, added missing files)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | § Create method, § Mutate method, § Copy-on-Write Rule | Yes | — |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § ServiceMapping Enum, § Routing Commands | Yes | — |
| `docs/concepts/sagas.md` | § Lock-Acquisition Step Pattern, § Write Workflow Structure | Yes | — |
| `docs/concepts/testing.md` | § T2 — Functionality Test, § Upstream-invariant rule | Yes | — |

---

## Skill Instructions Feedback

### What worked well

- The lock-acquisition step pattern (SagaCommand + setSemanticLock) was clearly documented and translated directly to CreateQuiz and UpdateQuiz sagas.
- The prerequisite note about GetQuizByIdCommand being required in session-b when an update saga uses get-then-lock was helpful.
- The BeanConfigurationSagas and SpockTest update instructions were unambiguous.

### What was unclear or missing

- `QuizFunctionalities.java` was absent from the plan.md 2.6.b file table even though session-b.md explicitly lists it as a required produce. This caused a plan.md patch (Step 5b).
- `GetQuizByIdCommand.java` is planned for session-c but required in session-b for the UpdateQuiz lock step. The prerequisite note in session-b.md correctly flagged this, but plan.md's 2.6.b file table didn't include it.

### Suggested wording / structure changes

- none

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| n/a | All patterns were documented sufficiently | — | — |

---

## Patterns to Capture

- **Pattern:** QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE invariant is not testable via normal service path without time manipulation.
  **Observed in:** `UpdateQuizTest.groovy`
  **Description:** The invariant fires when `lastModifiedTime > prev.availableDate`. Since `lastModifiedTime = now()` at update time and quiz creation requires `creationDate < availableDate`, it is impossible to have `prev.availableDate` in the past at test creation time without mocking the clock. The QUIZ_DATE_ORDERING invariant (availableDate >= conclusionDate) was tested instead as a representative P1 violation.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| Medium | `applications/quizzes-full/plan.md` template (or classify-and-plan skill) | Ensure `{Aggregate}Functionalities.java` is listed in session-b file table for all aggregates |
| Low | `.claude/skills/implement-aggregate/session-b.md` | Add note that `GetQuizByIdCommand` (or similar get-by-id command) is also needed for aggregates using update sagas with lock steps, even if it is officially in session-c |

---

## One-Line Summary

`QuizFunctionalities.java` and `GetQuizByIdCommand.java` were both missing from plan.md's 2.6.b file table but required by the session-b pattern — added as plan.md patches.
