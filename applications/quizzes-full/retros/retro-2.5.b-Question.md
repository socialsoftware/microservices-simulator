# Retro — 2.5.b — Question

**App:** quizzes-full
**Session:** 2.5.b (Write Functionalities)
**Date:** 2026-05-04

---

## Files Produced

List every file created or modified this session (absolute paths).

- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/events/UpdateQuestionEvent.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/events/DeleteQuestionEvent.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/question/CreateQuestionCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/question/UpdateQuestionCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/question/DeleteQuestionCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/question/GetQuestionByIdCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/course/IncrementQuestionCountCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/aggregate/sagas/states/QuestionSagaState.java` (modified — added IN_UPDATE_QUESTION, IN_DELETE_QUESTION)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/service/QuestionService.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/messaging/QuestionCommandHandler.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/coordination/sagas/CreateQuestionFunctionalitySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/coordination/sagas/UpdateQuestionFunctionalitySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/coordination/sagas/DeleteQuestionFunctionalitySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/coordination/functionalities/QuestionFunctionalities.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/coordination/webapi/QuestionController.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/course/messaging/CourseCommandHandler.java` (modified — added IncrementQuestionCountCommand case)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/BeanConfigurationSagas.groovy` (modified — added Question beans)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/QuizzesFullSpockTest.groovy` (modified — added questionFunctionalities, createQuestion helper)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/question/CreateQuestionTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/question/UpdateQuestionTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/question/DeleteQuestionTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/plan.md` (modified — ticked 2.5.b, updated file table)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | § Create method, § Mutate method with event publication, § Copy-on-Write Rule | Yes | — |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § File Location, § ServiceMapping Enum | Yes | — |
| `docs/concepts/sagas.md` | § Lock-Acquisition Step Pattern, § Write Workflow Structure | Yes | — |
| `docs/concepts/testing.md` | § T2 Functionality Test template | Yes | — |
| `applications/quizzes-full/quizzes-full-aggregate-grouping.md` | § §4 Events (payload fields for UpdateQuestionEvent, DeleteQuestionEvent) | Yes | Used to determine event payloads |

**Sufficient?** = `Yes` / `Partial` / `No`

---

## Skill Instructions Feedback

### What worked well

- The session-b.md prerequisite checklist for `GetQuestionByIdCommand` was clear and correctly flagged that it must be created in session-b when write sagas need semantic lock steps.
- The prerequisite note on `QuestionFunctionalities` (step 5b pattern) was correctly inferred from prior session patterns.
- The existing saga patterns (DeleteExecutionFunctionalitySagas, CreateTopicFunctionalitySagas) provided clear templates.

### What was unclear or missing

- The session-b.md does not explicitly state that `IncrementQuestionCountCommand` (analogous to `IncrementExecutionCountCommand`) must be created and wired in `CourseCommandHandler`. This was inferred from the `CreateQuestion` saga structure, but the skill instruction text doesn't surface this case.
- The skill doesn't mention that the `CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT` invariant fires on `incrementQuestionCount` when `executionCount == 0`, meaning test setups for CreateQuestion/UpdateQuestion/DeleteQuestion must include a prior `createExecution`. This was discovered via test failures after wiring `IncrementQuestionCountCommand`.

### Suggested wording / structure changes

- In session-b.md, under the description of CreateQuestion saga, add a note: "If CreateQuestion increments a course counter, check whether `CourseCommandHandler` already handles the corresponding command. If not, add it before running tests."
- Add a note: "Test setups for operations that increment counters must satisfy any invariants those counters participate in (e.g., `questionCount > 0` requires `executionCount > 0` on Course)."

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/testing.md` | No mention that test setups must satisfy all course counter invariants when incrementing question/execution counts | Medium | Add a note in T2 template section: "when a saga increments a counter cached on an upstream aggregate, verify that the aggregate's invariants permit the new counter value — add prerequisite state to `setup:` accordingly" |
| `.claude/skills/implement-aggregate/session-b.md` | `IncrementXxxCountCommand` for course not surfaced as a required artefact when CreateQuestion saga increments questionCount | Medium | Add note in the "Prerequisite" section: "Also check upstream count-manipulation commands (e.g., IncrementQuestionCountCommand) and ensure they are wired in the upstream CommandHandler." |

---

## Patterns to Capture

- **Pattern:** CreateQuestion saga requires an execution to exist on the target course before creating questions (due to `executionCount == 0 ⟹ questionCount == 0` invariant in `Course.verifyInvariants()`).
  **Observed in:** `CreateQuestionTest.groovy`, `UpdateQuestionTest.groovy`, `DeleteQuestionTest.groovy`
  **Description:** Any T2 test for CreateQuestion/UpdateQuestion/DeleteQuestion must call `createExecution(courseId, ...)` in `setup:` before calling `createQuestion(...)`. Without it, `incrementQuestionCount` triggers the Course invariant and the test fails.

- **Pattern:** Multi-topic fetch in a single saga step via loop.
  **Observed in:** `CreateQuestionFunctionalitySagas.java`, `UpdateQuestionFunctionalitySagas.java`
  **Description:** When a saga needs DTOs for a variable-length list of upstream entities (e.g., topicIds), it fetches them all in a single `getTopicsStep` using a for-loop over the IDs, building the value-object set inline. No compensation is needed for pure-read steps.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| Medium | `.claude/skills/implement-aggregate/session-b.md` | Add prerequisite note: when CreateXxx saga increments an upstream counter, verify the corresponding IncrementXxxCountCommand is handled by the upstream CommandHandler |
| Medium | `docs/concepts/testing.md` | Add note that T2 test setups must satisfy upstream invariants activated by counter increments |

---

## One-Line Summary

`IncrementQuestionCountCommand` was missing from `CourseCommandHandler` and tests broke because the `CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT` invariant fires on question creation when no execution exists; test setups now create an execution first.
