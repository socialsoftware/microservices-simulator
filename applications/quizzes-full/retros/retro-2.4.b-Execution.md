# Retro — 2.4.b — Execution

**App:** quizzes-full
**Session:** 2.4.b (Write Functionalities)
**Date:** 2026-05-02

---

## Files Produced

List every file created or modified this session (absolute paths).

- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/events/DeleteCourseExecutionEvent.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/events/DisenrollStudentFromCourseExecutionEvent.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/course/IncrementExecutionCountCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/execution/GetExecutionByIdCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/execution/CreateExecutionCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/execution/UpdateExecutionCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/execution/DeleteExecutionCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/execution/EnrollStudentInExecutionCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/execution/DisenrollStudentFromExecutionCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/execution/UpdateStudentNameInExecutionCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/execution/AnonymizeStudentInExecutionCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/aggregate/sagas/states/ExecutionSagaState.java` (modified)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/aggregate/ExecutionCustomRepository.java` (modified)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/aggregate/sagas/repositories/ExecutionCustomRepositorySagas.java` (modified)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/service/ExecutionService.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/messaging/ExecutionCommandHandler.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/coordination/sagas/CreateExecutionFunctionalitySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/coordination/sagas/UpdateExecutionFunctionalitySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/coordination/sagas/DeleteExecutionFunctionalitySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/coordination/sagas/EnrollStudentInExecutionFunctionalitySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/coordination/sagas/DisenrollStudentFunctionalitySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/coordination/sagas/UpdateStudentNameFunctionalitySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/coordination/sagas/AnonymizeStudentFunctionalitySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/coordination/functionalities/ExecutionFunctionalities.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/execution/coordination/webapi/ExecutionController.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/course/messaging/CourseCommandHandler.java` (modified — added IncrementExecutionCountCommand case)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/exception/QuizzesFullErrorMessage.java` (modified — added INACTIVE_USER, NO_DUPLICATE_COURSE_EXECUTION, COURSE_EXECUTION_STUDENT_NOT_FOUND)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/user/aggregate/User.java` (modified — User constructor now sets active=true; bug fix)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/user/service/UserService.java` (modified — deleteUser sets active=false before remove())
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/BeanConfigurationSagas.groovy` (modified)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/QuizzesFullSpockTest.groovy` (modified)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/user/UserTest.groovy` (modified — updated active assertion to true)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/user/CreateUserTest.groovy` (modified — updated active assertion to true)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/execution/CreateExecutionTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/execution/UpdateExecutionTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/execution/DeleteExecutionTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/execution/EnrollStudentInExecutionTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/execution/DisenrollStudentTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/execution/UpdateStudentNameTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/execution/AnonymizeStudentTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/plan.md` (modified)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | Create/mutate/read patterns, P3 guard placement, copy-on-write rule, soft-delete exception | Partial | The doc says in-place mutation for soft-delete is preferred, but this causes JPA auto-flush issues when invariants check state==DELETED; copy-on-write is safer |
| `docs/concepts/commands.md` | Command structure, ServiceMapping, CommandHandler routing | Yes | — |
| `docs/concepts/sagas.md` | SagaStep, setSemanticLock, SagaCommand compensation, Write Workflow Structure | Yes | — |
| `docs/concepts/testing.md` | T2 template, step-interleaving, not-found assertions | Yes | — |

---

## Skill Instructions Feedback

### What worked well

- The session-b.md prerequisite note about `GetExecutionByIdCommand` being needed for write sagas was correct and clear.
- The step-ordering pattern (data-assembly → lock → execute) was straightforward to apply across all 7 sagas.
- The `{Aggregate}Functionalities.java` prerequisite note saved a compilation failure.

### What was unclear or missing

- `ExecutionFunctionalities.java` was not listed in plan.md for session 2.4.b (unlike other aggregates where it was explicitly added). Had to discover this via the prerequisite note in session-b.md.
- Events must extend `Event` (a JPA `@Entity`), not implement `DomainEvent`. The session-b.md does not mention this requirement for events. The first implementation used `DomainEvent` and had to be corrected.
- The JPA auto-flush issue with in-place mutation + `verifyInvariants()` during saga abort is not documented anywhere. This required runtime debugging to diagnose.

### Suggested wording / structure changes

- `session-b.md` §Event classes: add "extend `Event` from simulator core — it is a JPA `@Entity`; do NOT implement `DomainEvent`".
- `docs/concepts/service.md` §Copy-on-Write Rule: add a warning that in-place mutation for aggregates whose invariants check `state==DELETED` can cause JPA auto-flush issues during saga abort; copy-on-write is safer in these cases.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `session-b.md` §Event classes | No mention that events must extend `Event` (JPA entity) not implement `DomainEvent` | Medium — caused a compile error that required examining existing events | Add explicit note: "Events must extend `pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event` and be annotated `@Entity`" |
| `docs/concepts/service.md` §Copy-on-Write Rule | The soft-delete in-place mutation pattern causes JPA auto-flush issues when the aggregate has invariants that check state==DELETED and the saga aborts | High — caused a test failure (SimulatorException instead of QuizzesFullException) that required deep debugging | Add a note: "For aggregates whose invariants check `state==DELETED`, use copy-on-write even for soft-delete to avoid JPA auto-flush interactions during saga abort" |
| `docs/concepts/aggregate.md` or `service.md` | `User.active` semantics: new users must be active=true; deleteUser must set active=false before remove(). The session 2.2.a implementation had `User` constructor set active=false which blocks all enrollment. | High — blocked EnrollStudentInExecution tests entirely; required fixing User.java and UserService.java | Document the `active` field lifecycle: "created as true, set to false during deleteUser before remove()" |

---

## Patterns to Capture

- **Pattern:** Copy-on-write for soft-delete when invariants check state==DELETED
  **Observed in:** `ExecutionService.deleteExecution`
  **Description:** Using in-place mutation (`execution.remove()` on a managed JPA entity) before `registerChanged()` can cause JPA to auto-flush the dirty entity state before the abort path's JPQL query (`findNonDeletedSagaAggregate`), making the aggregate invisible to the abort. Copy-on-write avoids this by keeping the old entity unmodified in the JPA context.

- **Pattern:** `active=true` invariant for new Users
  **Observed in:** `User.java` constructor, `UserService.deleteUser`
  **Description:** The `active` field on User tracks whether the user is live in the system. New users must be created with `active=true`. The deleteUser service must set `active=false` before calling `remove()` so the `USER_DELETED_STATE` invariant (state==DELETED → !active) is satisfied.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| High | `.claude/skills/implement-aggregate/session-b.md` | Add to §Event classes: "Events must extend `pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event` and carry `@Entity` — they are JPA-persisted domain events. Do NOT implement `DomainEvent` interface." |
| High | `docs/concepts/service.md` | Add note in §Copy-on-Write Rule: for aggregates whose `verifyInvariants()` checks `state==DELETED` (e.g., REMOVE_NO_STUDENTS), use copy-on-write even for soft-delete to prevent JPA auto-flush from hiding the aggregate during saga abort. |
| Medium | `.claude/skills/implement-aggregate/session-b.md` | Add `ExecutionFunctionalities.java` explicitly to plan.md file table for aggregates with write functionalities, as it is always needed as a Spring bean. |

---

## One-Line Summary

The session exposed two undocumented patterns: (1) events must extend `Event` not implement `DomainEvent`, and (2) in-place soft-delete with invariants that check `state==DELETED` causes JPA auto-flush to hide the aggregate during saga abort — copy-on-write is the safe fix.
