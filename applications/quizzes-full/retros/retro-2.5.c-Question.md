# Retro — 2.5.c — Question

**App:** quizzes-full
**Session:** 2.5.c (Read Functionalities)
**Date:** 2026-05-04

---

## Files Produced

- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/service/QuestionService.java` (appended `getQuestionsByCourseExecutionId`)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/question/GetQuestionsByCourseExecutionIdCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/coordination/sagas/GetQuestionByIdFunctionalitySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/coordination/sagas/GetQuestionsByCourseExecutionIdFunctionalitySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/coordination/functionalities/QuestionFunctionalities.java` (appended `getQuestionById`, `getQuestionsByCourseExecutionId`)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/messaging/QuestionCommandHandler.java` (appended `GetQuestionsByCourseExecutionIdCommand` case)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/question/GetQuestionByIdTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/question/GetQuestionsByCourseExecutionIdTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/plan.md` (ticked 2.5.c)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | § Read method, § Injected Dependencies | Yes | List-return pattern from TopicService used as model |
| `docs/concepts/commands.md` | § Naming Conventions, § Routing Commands | Yes | — |
| `docs/concepts/sagas.md` | § Read Functionality Sagas, § List-return read variant | Yes | Two-step saga for GetQuestionsByCourseExecutionId inferred from list-return variant section |
| `docs/concepts/testing.md` | § T2, § Not-Found Assertions, § Upstream-invariant rule | Yes | Upstream-invariant rule was key: createExecution needed in GetQuestionByIdTest setup |

---

## Skill Instructions Feedback

### What worked well

- The pre-emption check (GetQuestionByIdCommand already in 2.5.b) was clearly stated and applied correctly.
- The stub upgrade exception clause (session-c.md) was correctly evaluated: no stub was present in QuestionFunctionalities.
- The "Always required" clauses for QuestionFunctionalities and QuestionCommandHandler gave clear guidance without listing these files in the 2.5.c file table.

### What was unclear or missing

- The plan.md 2.5.c file table lists `commands/question/GetQuestionByIdCommand.java` even though it was moved to 2.5.b. The pre-emption rule in session-c.md handles this correctly, but the plan.md file table is slightly misleading — it lists a file that was already produced. No action needed, but it causes a momentary re-check.
- `GetQuestionsByCourseExecutionIdFunctionalitySagas` requires two steps (get execution to resolve courseId, then get questions). The docs describe the single-step list variant but not the two-step variant. Design was inferred from the two-step write saga pattern applied to a read context.

### Suggested wording / structure changes

- `session-c.md` could note: "If a list-return read requires resolving a foreign aggregate's ID first (e.g., executionId → courseId), use a two-step saga: step 1 fetches the foreign aggregate DTO, step 2 sends the question command with the resolved ID." This prevents having to infer from write saga patterns.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/sagas.md` | Two-step read saga pattern (read needs foreign aggregate's ID first) not documented | Medium | Add a "Two-step read saga" variant below the list-return variant section |
| `docs/concepts/testing.md` | Upstream-invariant rule is present but easy to miss; would benefit from a direct callout in the read-test section | Low | Add a note in T2 read subsection pointing to the upstream-invariant rule |

---

## Patterns to Capture

- **Pattern:** Two-step read saga for cross-aggregate ID resolution
  **Observed in:** `GetQuestionsByCourseExecutionIdFunctionalitySagas.java`
  **Description:** When a read functionality's parameter is a foreign aggregate ID (executionId) that needs to be resolved to the primary aggregate's filter field (courseAggregateId), use a two-step saga: step 1 fetches the foreign aggregate DTO, step 2 sends the read command with the resolved field. No compensation needed on either step since reads are non-mutating.

- **Pattern:** Upstream-invariant prerequisite in read test setup
  **Observed in:** `GetQuestionByIdTest.groovy`
  **Description:** Read tests for Question must call `createExecution` in setup because `createQuestion` increments Course `questionCount`, which triggers the `CANNOT_DELETE_LAST_EXECUTION_WITH_QUESTIONS` invariant when `executionCount == 0`. This same prerequisite applies to all Question T2 tests.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| Medium | `.claude/skills/implement-aggregate/session-c.md` | Add a "two-step read saga" variant for cases where the saga must first fetch a foreign aggregate to resolve a filter ID |
| Medium | `docs/concepts/sagas.md` | Add a two-step read saga example under § Read Functionality Sagas |
| Low | `docs/concepts/testing.md` | Add a callout in T2 read tests: "Remember the upstream-invariant rule — if createX triggers a counter increment on an upstream aggregate, the test setup must satisfy that aggregate's invariants first" |

---

## One-Line Summary

The `GetQuestionsByCourseExecutionId` read functionality required a two-step saga (execution → questions) not covered by the docs, and a test setup fix due to the upstream-invariant rule (Course's `questionCount > 0` requires `executionCount > 0`).
