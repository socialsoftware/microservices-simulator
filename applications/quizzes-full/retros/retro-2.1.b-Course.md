# Retro — 2.1.b — Course

**App:** quizzes-full
**Session:** 2.1.b (Write Functionalities)
**Date:** 2026-04-24

---

## Files Produced

- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/ServiceMapping.java` (new — Step 5b)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/course/CreateCourseCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/course/UpdateCourseCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/course/DeleteCourseCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/course/DecrementExecutionCountCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/course/DecrementQuestionCountCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/course/GetCourseByIdCommand.java` (new — Step 5b, moved from 2.1.c)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/course/service/CourseService.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/course/messaging/CourseCommandHandler.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/course/coordination/sagas/CreateCourseFunctionalitySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/course/coordination/sagas/UpdateCourseFunctionalitySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/course/coordination/sagas/DeleteCourseFunctionalitySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/course/coordination/functionalities/CourseFunctionalities.java` (new — Step 5b)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/exception/QuizzesFullErrorMessage.java` (modified — added COURSE_FIELDS_IMMUTABLE)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/BeanConfigurationSagas.groovy` (modified)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/QuizzesFullSpockTest.groovy` (modified)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/course/CreateCourseTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/course/UpdateCourseTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/course/DeleteCourseTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/plan.md` (modified)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | Create, mutate, read method patterns; copy-on-write rule; P3 guard placement; event publication | Partial | Doesn't address when delete skips copy-on-write. `deleteCourse` in the reference app mutates in-place, contradicting the "always copy-on-write" rule stated in the doc. |
| `docs/concepts/commands.md` | Command structure, ServiceMapping enum, CommandHandler switch pattern | Partial | Doesn't explain the bean naming contract that ties `serviceName` (lowercase) to `getBean(serviceName + "CommandHandler")`. Had to read `LocalCommandService.java` to understand routing. `getAggregateTypeName()` vs `serviceName` distinction also undocumented. |
| `docs/concepts/sagas.md` | SagaWorkflow structure, SagaStep, compensation registration | Partial | Doesn't explain the `SagaCommand(innerCmd) + setSemanticLock(state)` pattern used in lock-acquiring get-steps vs. plain `Command` for write steps. The distinction had to be inferred from `DeleteTopicFunctionalitySagas`. |
| `docs/concepts/testing.md` | T2 section — happy path, guard violation, step-interleaving | Yes | Template was clear. No interleaving tests needed for Course (no forbidden-state deps between write ops). |

---

## Reference App Consulted

| Reference file | Why consulted | Gap it reveals |
|---------------|--------------|----------------|
| `applications/quizzes/microservices/course/service/CourseService.java` | Pattern for createCourse (ID generation + factory), deleteCourse (in-place mutation), decrementXxxCount (copy-on-write) | `docs/concepts/service.md` copy-on-write rule doesn't call out the delete exception; `session-b.md` doesn't list service method signatures |
| `applications/quizzes/microservices/course/messaging/CourseCommandHandler.java` | Confirm switch pattern, `getAggregateTypeName()` casing, logger.warning for unknown | `docs/concepts/commands.md` doesn't show a full CommandHandler example with all cases |
| `applications/quizzes/microservices/topic/coordination/sagas/DeleteTopicFunctionalitySagas.java` | Two-step delete saga: SagaCommand+setSemanticLock in step 1, plain Command in step 2, compensation pattern | `docs/concepts/sagas.md` and `session-b.md` don't show the `SagaCommand` wrapper used in read-then-lock steps |
| `applications/quizzes/microservices/topic/coordination/sagas/CreateTopicFunctionalitySagas.java` | Multi-step saga dependency declaration (`new ArrayList<>(Arrays.asList(prevStep))`) | `docs/concepts/sagas.md` mentions dependency lists but doesn't show the exact Java syntax |
| `applications/quizzes/microservices/topic/coordination/functionalities/TopicFunctionalities.java` | `CourseFunctionalities` pattern: `@Service`, `new Throwable().getStackTrace()[0].getMethodName()`, saga instantiation inline | `session-b.md` omits `{Aggregate}Functionalities` from the list of files to produce |
| `applications/quizzes/BeanConfigurationSagas.groovy` | `CourseService(unitOfWorkService, customRepo)` Groovy bean factory signature | `session-b.md` bean snippet shows `{aggregate}Service(...)` but not which deps to inject or how the factory method signature maps to constructor |
| `applications/quizzes/QuizzesSpockTest.groovy` | `createCourseExecution()` helper pattern; `@Autowired` fields naming; test constants style | `session-b.md` doesn't mention updating `{AppClass}SpockTest.groovy` |
| `simulator/src/main/java/.../ms/messaging/local/LocalCommandService.java` | Confirmed routing: `getBean(command.getServiceName() + "CommandHandler")` | `docs/concepts/commands.md` says `getAggregateTypeName()` must match `serviceName` — this is wrong; the actual routing key is the Spring bean name derived from the class, not `getAggregateTypeName()` |

---

## Skill Instructions Feedback

### What worked well

- The "Files to produce" table in plan.md is the canonical file list — reading it first and treating it as authoritative worked well.
- The "Step 5b: Patch plan.md for Missing Files" mechanism correctly surfaced the three missing files (`ServiceMapping`, `CourseFunctionalities`, `GetCourseByIdCommand`).
- Step ordering in session-b.md (data-assembly → lock → execute) was correct conceptually, even if the SagaCommand wrapping detail was missing.

### What was unclear or missing

- **`{Aggregate}Functionalities.java` not in the file list**: session-b.md doesn't include it, but tests need it as a `@Service` bean. Every session-b will need this class. It must be added to the "Produce" section.
- **`ServiceMapping.java` not mentioned**: session-b.md doesn't say to create or verify that `ServiceMapping.java` exists. It's required for all commands. Should be a prerequisite check.
- **GetCourseByIdCommand dependency in write sagas**: session-b.md says read steps send a command to acquire a lock, but doesn't say this requires the read command (`Get{Aggregate}ByIdCommand`) which may not yet exist. The skill should tell the implementer to create it in session-b if it doesn't already exist.
- **SagaCommand wrapping**: The lock-acquisition pattern (`new SagaCommand(readCmd); sagaCommand.setSemanticLock(state)`) is not explained anywhere in docs or the skill. Only visible in `DeleteTopicFunctionalitySagas`.
- **Delete service method exception to copy-on-write**: session-b.md says to mutate via factory copy, but `deleteCourse` does not. The rule should state: "for `remove()` (soft-delete), mutate in-place — no copy-on-write."
- **BeanConfigurationSagas: FunctionalitySagas beans are wrong**: session-b.md says "One bean per write FunctionalitySagas class" — this is incorrect. FunctionalitySagas are instantiated inline inside `{Aggregate}Functionalities`. Only `{Aggregate}Service`, `{Aggregate}CommandHandler`, and `{Aggregate}Functionalities` are beans.
- **`QuizzesFullSpockTest.groovy` update not mentioned**: session-b.md doesn't say to add `@Autowired {aggregate}Functionalities` and a `create{Aggregate}()` helper to the SpockTest base class.
- **`getAggregateTypeName()` claim in commands.md is misleading**: The doc says it must match `serviceName` — it doesn't. `getAggregateTypeName()` is PascalCase and used by `CommandHandlerDecorator`; routing uses the Spring bean name (camelCase class name = `serviceName + "CommandHandler"`).

### Suggested wording / structure changes

- **`session-b.md` § Produce**: Add `{Aggregate}Functionalities.java` (path: `{src}microservices/{aggregate}/coordination/functionalities/`) as a required file. Describe it as a `@Service` that creates the saga, calls `executeWorkflow`, and exposes one method per write functionality.
- **`session-b.md` § Produce**: Add a prerequisite check: "If `Get{Aggregate}ByIdCommand` does not exist (it may be planned for session-c), create it now — it is required by the lock-acquiring step of write sagas."
- **`session-b.md` § Update BeanConfigurationSagas.groovy**: Remove "One bean per write FunctionalitySagas class" — replace with: "One `@Bean {Aggregate}Functionalities {aggregate}Functionalities()` (no-arg constructor)."
- **`session-b.md` § Update BeanConfigurationSagas.groovy**: Add note to update `{AppClass}SpockTest.groovy` with `@Autowired(required = false) protected {Aggregate}Functionalities {aggregate}Functionalities` and a `create{Aggregate}(...)` helper method.
- **`docs/concepts/commands.md`**: Fix the claim that `getAggregateTypeName()` must match `serviceName`. Clarify: `serviceName` (lowercase) must match the Spring bean name prefix of the CommandHandler (`serviceName + "CommandHandler"`). `getAggregateTypeName()` is PascalCase and used by `CommandHandlerDecorator`, not for routing.
- **`docs/concepts/sagas.md`**: Add a subsection on the lock-acquisition step pattern: `SagaCommand sagaCommand = new SagaCommand(readCmd); sagaCommand.setSemanticLock(XxxSagaState.IN_YYY);`. Contrast with write steps that use plain commands.
- **`docs/concepts/service.md`**: Add a note under Copy-on-Write Rule: "Exception: for soft-delete operations (`aggregate.remove()`), mutate the loaded aggregate in-place — no factory copy is needed."

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/commands.md` | `getAggregateTypeName()` vs. `serviceName` routing mechanism | High — caused confusion, needed simulator source read | Add subsection: "Command routing uses `serviceName + 'CommandHandler'` as Spring bean name; `getAggregateTypeName()` is independent." |
| `docs/concepts/sagas.md` | SagaCommand wrapping for semantic lock acquisition | High — every write saga with a get-step needs this | Add code example showing `new SagaCommand(readCmd)` + `setSemanticLock(state)` vs plain command for write step |
| `docs/concepts/service.md` | Delete exception to copy-on-write rule | Medium — easy to get wrong | Add explicit carve-out: "delete/remove operations mutate in-place" |
| `session-b.md` | `{Aggregate}Functionalities.java` missing from file list | High — needed every session-b | Add to Produce section |
| `session-b.md` | `ServiceMapping.java` prerequisite not mentioned | Medium — needed for all commands | Add as a prerequisite check at the top of Produce |
| `session-b.md` | `GetXxxByIdCommand` dependency in write sagas | Medium — plan listed it in session-c but it's needed in session-b | Explicit note: create read command early if not yet produced |
| `session-b.md` | BeanConfigurationSagas: wrong instruction about FunctionalitySagas beans | High — would produce an uncompilable config | Correct to: only Service, CommandHandler, and Functionalities are beans |
| `session-b.md` | SpockTest update not mentioned | Medium — tests cannot compile without @Autowired field | Add step: update `{AppClass}SpockTest.groovy` |

---

## Patterns to Capture

- **Pattern:** Semantic lock acquisition in two-step write sagas
  **Observed in:** `DeleteCourseFunctionalitySagas.java`, `UpdateCourseFunctionalitySagas.java`
  **Description:** Step 1 wraps the read command in `SagaCommand`, calls `setSemanticLock(XxxSagaState.IN_YYY)`, and registers a compensation that sends `new SagaCommand(new Command(...))` with `setSemanticLock(GenericSagaState.NOT_IN_SAGA)` to release the lock. Step 2 sends a plain (unwrapped) command that performs the mutation. The step 2 depends on step 1.

- **Pattern:** Delete service method (in-place mutation)
  **Observed in:** `CourseService.deleteCourse()`, `applications/quizzes/microservices/course/service/CourseService.java`
  **Description:** `aggregateLoadAndRegisterRead` → `aggregate.remove()` → `registerChanged(aggregate, uow)`. No factory copy. This is the deliberate exception to the copy-on-write rule; `remove()` is a terminal state transition that doesn't interact with version merging.

- **Pattern:** CommandHandler bean name must equal `serviceName + "CommandHandler"`
  **Observed in:** `LocalCommandService.java` (simulator), `BeanConfigurationSagas.groovy`
  **Description:** `LocalCommandService.send()` looks up `applicationContext.getBean(command.getServiceName() + "CommandHandler")`. The `@Bean` method name in `BeanConfigurationSagas` must produce a Spring bean with that exact name. `ServiceMapping.COURSE.getServiceName()` = `"course"` → bean name `"courseCommandHandler"` → `@Bean CourseCommandHandler courseCommandHandler() { ... }`.

- **Pattern:** `{Aggregate}Functionalities` as saga orchestrator bean
  **Observed in:** `CourseFunctionalities.java`, `applications/quizzes/microservices/topic/coordination/functionalities/TopicFunctionalities.java`
  **Description:** `@Service` class with one method per functionality. Each method: derives `functionalityName` from stack trace, creates a `SagaUnitOfWork`, instantiates the `{Op}FunctionalitySagas` directly (not as a bean), calls `executeWorkflow(uow)`, returns the result DTO. Tests `@Autowired` this class and call its methods.

- **Pattern:** Read command created in session-b when needed by write sagas
  **Observed in:** `GetCourseByIdCommand.java` created in 2.1.b despite being listed in 2.1.c
  **Description:** Write sagas that use a two-step lock pattern (get-then-mutate) need `Get{Aggregate}ByIdCommand` in the lock-acquisition step. If the read command is planned for session-c but the write saga needs it in session-b, create it early and move it in plan.md.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| High | `.claude/skills/implement-aggregate/session-b.md` | Add `{Aggregate}Functionalities.java` to the Produce section; fix BeanConfigurationSagas bean list (remove FunctionalitySagas beans, add Functionalities bean); add step to update SpockTest base class |
| High | `.claude/skills/implement-aggregate/session-b.md` | Add prerequisite check: create `Get{Aggregate}ByIdCommand` in session-b if not yet produced (write sagas need it for lock steps) |
| High | `docs/concepts/sagas.md` | Add subsection with code example showing `SagaCommand` wrapping for lock acquisition vs. plain command for write step |
| High | `docs/concepts/commands.md` | Fix `getAggregateTypeName()` claim; document `serviceName + "CommandHandler"` bean name routing rule |
| Medium | `docs/concepts/service.md` | Add explicit carve-out for delete: in-place mutation, no factory copy |
| Medium | `.claude/skills/implement-aggregate/session-b.md` | Add `ServiceMapping.java` prerequisite check at top of Produce section |

---

## One-Line Summary

`session-b.md` omits three files that are required every session-b (`{Aggregate}Functionalities`, `ServiceMapping`, `Get{Aggregate}ByIdCommand`) and incorrectly instructs adding FunctionalitySagas as Spring beans; additionally, `docs/concepts/sagas.md` and `docs/concepts/commands.md` both have gaps that forced reference-app reads for the lock-acquisition and command-routing patterns.
