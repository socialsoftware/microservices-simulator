# Retro — 2.4.c — Execution

**App:** quizzes-full
**Session:** 2.4.c (Read Functionalities)
**Date:** 2026-05-02

---

## Files Produced

List every file created or modified this session (absolute paths).

- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/coordination/sagas/GetExecutionByIdFunctionalitySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/execution/GetStudentByExecutionIdAndUserIdCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/coordination/sagas/GetStudentByExecutionIdAndUserIdFunctionalitySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/coordination/functionalities/ExecutionFunctionalities.java` (updated)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/messaging/ExecutionCommandHandler.java` (updated)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/execution/GetExecutionByIdTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/execution/GetStudentByExecutionIdAndUserIdTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/plan.md` (checkbox ticked)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/sagas.md` | § Read Functionality Sagas | Yes | Template matched exactly what was needed |
| `docs/concepts/testing.md` | § T2, § Not-Found Assertions | Yes | Clear guidance on SimulatorException for aggregate-not-found |
| `.claude/skills/implement-aggregate/session-c.md` | § Pre-emption check, § Produce | Yes | Pre-emption check correctly identified what was already done in 2.4.b |

---

## Skill Instructions Feedback

### What worked well

- The pre-emption check in session-c.md correctly identified that `getExecutionById` service method and `GetExecutionByIdCommand` were already produced in 2.4.b, preventing duplication.
- The instruction to still produce `FunctionalitySagas` and update the coordinator method was clear.
- The not-found assertion guidance (`SimulatorException` vs app exception) was clear and correct.

### What was unclear or missing

- The session-c.md says "append one method per read functionality — do not rewrite the file", but the existing `getExecutionById` method in `ExecutionFunctionalities.java` (placed in 2.4.b) called the service directly rather than using the saga pattern. This required replacing the existing method body, not just appending. The instruction doesn't cover this case (when session-b placed a direct-service stub for a read method that session-c should upgrade to saga).
- The plan.md 2.4.c row still lists `commands/execution/GetExecutionByIdCommand.java` even though 2.4.b already produced it. This is informational only (pre-emption check handles it), but slightly confusing.

### Suggested wording / structure changes

- In `session-c.md`, add a note under "Pre-emption check": "If the coordinator already has a direct-service stub for `getExecutionById` (placed in 2.4.b for test wiring), replace its body with the saga-based implementation rather than appending a duplicate method."

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `session-c.md` | No guidance on upgrading a direct-service stub to saga when 2.4.b placed one | Medium | Add note to the pre-emption check section about updating existing stub bodies |

---

## Patterns to Capture

- **Pattern:** Direct-service stub in functionalities upgraded to saga in session-c
  **Observed in:** `ExecutionFunctionalities.java` — `getExecutionById`
  **Description:** When 2.4.b places a direct `executionService.getExecutionById(...)` call in the functionalities coordinator (for test wiring), session-c should replace the body with the full saga pattern. The method signature remains the same; only the body changes to instantiate and execute `GetExecutionByIdFunctionalitySagas`.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| Medium | `.claude/skills/implement-aggregate/session-c.md` | Add note to pre-emption check: if functionalities already has a direct-service stub for the read method, replace its body with the saga pattern rather than appending a duplicate |

---

## One-Line Summary

The main decision point was replacing the direct-service `getExecutionById` stub (placed in 2.4.b) with the proper saga-based implementation — session-c.md's pre-emption check covers skipping re-creation but doesn't explicitly handle the stub-upgrade case.
