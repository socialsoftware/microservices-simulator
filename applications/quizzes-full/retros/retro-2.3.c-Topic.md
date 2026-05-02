# Retro — 2.3.c — Topic

**App:** quizzes-full
**Session:** 2.3.c (Read Functionalities)
**Date:** 2026-05-02

---

## Files Produced

- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/aggregate/TopicCustomRepository.java` (modified — added `findTopicIdsByCourseId`)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/aggregate/sagas/repositories/TopicCustomRepositorySagas.java` (modified — implemented `findTopicIdsByCourseId`)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/service/TopicService.java` (modified — appended `getTopicsByCourseId`)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/topic/GetTopicsByCourseIdCommand.java` (created)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/coordination/sagas/GetTopicByIdFunctionalitySagas.java` (created)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/coordination/sagas/GetTopicsByCourseIdFunctionalitySagas.java` (created)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/coordination/functionalities/TopicFunctionalities.java` (modified — appended `getTopicById` and `getTopicsByCourseId`)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/messaging/TopicCommandHandler.java` (modified — added `GetTopicsByCourseIdCommand` case)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/topic/GetTopicByIdTest.groovy` (created)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/topic/GetTopicsByCourseIdTest.groovy` (created)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/plan.md` (ticked `[x] 2.3.c`)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | § Read method | Yes | Pattern for `aggregateLoadAndRegisterRead` + factory DTO mapping |
| `docs/concepts/sagas.md` | § Read Functionality Sagas | Yes | Single-step no-compensation template was sufficient |
| `docs/concepts/testing.md` | § T2 — Functionality Test | Partial | Only happy-path and not-found cases described; nothing specific about list-return reads |

---

## Reference App Consulted

| Reference file | Why consulted | Gap it reveals |
|---------------|--------------|----------------|
| `applications/quizzes/src/main/java/.../topic/coordination/sagas/GetTopicByIdFunctionalitySagas.java` | Confirmed field naming (`commandGateway` vs. `CommandGateway` typo in ref) | sagas.md template has `CommandGateway` (capital C) — reference app has a naming inconsistency; quizzes-full uses lowercase consistently |
| `applications/quizzes/src/main/java/.../topic/coordination/sagas/FindTopicsByCourseFunctionalitySagas.java` | Pattern for `List<TopicDto>` return in a read saga | session-c.md only shows single-DTO getter pattern; list variant is not covered |
| `applications/quizzes/src/main/java/.../topic/service/TopicService.java` | How `findTopicsByCourseId` is implemented (JPA `findAll()` + stream filter) | quizzes service uses JPA repo directly; quizzes-full uses `TopicCustomRepository` — needed to decide whether to add method to interface or inject JPA repo separately |
| `applications/quizzes/src/main/java/.../topic/messaging/TopicCommandHandler.java` | Confirmed pattern for `FindTopicsByCourseIdCommand` dispatch | session-c.md does not mention updating the CommandHandler; required reference app to confirm this step |

---

## Skill Instructions Feedback

### What worked well

- Pre-emption check correctly identified that `GetTopicByIdCommand` and `getTopicById` service method were already produced in 2.3.b; instructions were clear on what to skip vs. still produce.
- The single-step no-compensation template from `sagas.md` was directly usable for `GetTopicByIdFunctionalitySagas`.
- BeanConfigurationSagas — no change needed section was correct; no confusion.

### What was unclear or missing

- **List-return read saga**: session-c.md only shows the `{Aggregate}Dto get{Aggregate}Dto()` getter pattern. The `GetTopicsByCourseId` functionality returns `List<TopicDto>`. The session had to consult `FindTopicsByCourseFunctionalitySagas` in the reference app to confirm the pattern.
- **CommandHandler update not mentioned**: session-c.md does not mention updating `{Aggregate}CommandHandler` with the new read commands. This is always required but absent from the instructions.
- **`TopicCustomRepository` extension**: the quizzes reference's `findTopicsByCourseId` uses a JPA repo injected directly into the service. In quizzes-full, the service already injects `TopicCustomRepository`. Session-c.md has no guidance on how to handle list queries that require scanning all instances — whether to add a method to the custom repository interface, inject the JPA repo alongside, or other. Consulted reference app to choose the custom-repo approach.

### Suggested wording / structure changes

- `session-c.md` §"One `{Query}FunctionalitySagas.java` per read functionality": add a note that when the read returns a collection (`List<{Aggregate}Dto>`), the result field and getter follow the list pattern (e.g., `List<TopicDto> topics` / `getTopics()`); provide an example beside the single-DTO one.
- `session-c.md`: add a step to update `{Aggregate}CommandHandler` for each new read command — same as session-b.md does for write commands.
- `session-c.md`: add a note that list-returning service methods may require adding a query method to `{Aggregate}CustomRepository` (and its sagas implementation) when no suitable method exists.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/sagas.md` | Read Functionality Sagas section only shows single-aggregate-by-ID template; no list-result variant | Medium | Add a `GetXxxByFieldFunctionalitySagas` example with `List<XxxDto>` return |
| `.claude/skills/implement-aggregate/session-c.md` | CommandHandler update step not included | High | Add explicit step: "Update `{Aggregate}CommandHandler.handleDomainCommand` to dispatch each new read command to the corresponding service method" |
| `.claude/skills/implement-aggregate/session-c.md` | No guidance on list-query repository design | Medium | Add note: "If the read returns a collection, add a `findXxxIdsBy{Field}(...)` method to `{Aggregate}CustomRepository` and implement it in `{Aggregate}CustomRepositorySagas`" |

---

## Patterns to Capture

- **Pattern:** List-return read saga
  **Observed in:** `GetTopicsByCourseIdFunctionalitySagas.java`
  **Description:** When a read functionality returns multiple aggregates, the saga field is `List<{Aggregate}Dto>` and the getter is `getXxxs()` / `getTopics()`. The command sends the `courseAggregateId` and the service method returns the full list; the saga stores and exposes it without mapping.

- **Pattern:** Custom repository list query
  **Observed in:** `TopicCustomRepository.java`, `TopicCustomRepositorySagas.java`
  **Description:** When a service method needs to iterate all instances of an aggregate to filter by a field (e.g., by courseId), add `findXxxIdsBy{Field}(Integer fieldValue)` to the custom repository interface and implement via `jpaRepo.findAll().stream().filter(...).map(Xxx::getAggregateId).distinct().collect(...)` in the sagas implementation.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| High | `.claude/skills/implement-aggregate/session-c.md` | Add step to update `{Aggregate}CommandHandler` for each new read command |
| High | `.claude/skills/implement-aggregate/session-c.md` | Add note on list-query custom repository pattern |
| Medium | `docs/concepts/sagas.md` | Add list-return variant to Read Functionality Sagas section |

---

## One-Line Summary

Session-c.md lacks two critical steps always needed for read functionalities: updating the CommandHandler and extending `TopicCustomRepository` for list queries — both had to be inferred from the reference app.
