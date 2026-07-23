# Retro — 2.3.b — Topic

**App:** quizzes-full
**Session:** 2.3.b (Write Functionalities)
**Date:** 2026-05-02

---

## Files Produced

List every file created or modified this session (absolute paths).

- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/events/DeleteTopicEvent.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/events/UpdateTopicEvent.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/topic/CreateTopicCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/topic/DeleteTopicCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/commands/topic/GetTopicByIdCommand.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/service/TopicService.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/messaging/TopicCommandHandler.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/coordination/sagas/CreateTopicFunctionalitySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/coordination/sagas/DeleteTopicFunctionalitySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/coordination/functionalities/TopicFunctionalities.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/coordination/webapi/TopicController.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/topic/CreateTopicTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/topic/DeleteTopicTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/BeanConfigurationSagas.groovy` (modified)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/QuizzesFullSpockTest.groovy` (modified)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/plan.md` (checkbox ticked + file table patched)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `.claude/skills/implement-aggregate/session-b.md` | All sections — service, command handler, sagas, functionalities, tests, BeanConfiguration, SpockTest | Yes | — |

---

## Reference App Consulted

| Reference file | Why consulted | Gap it reveals |
|---------------|--------------|----------------|
| `applications/quizzes/src/main/java/.../topic/service/TopicService.java` | Verified createTopic signature (TopicDto + TopicCourse parameters) and deleteTopic copy pattern | session-b.md doesn't spell out that createTopic receives a pre-assembled TopicCourse from the saga rather than just a courseId |
| `applications/quizzes/src/main/java/.../topic/messaging/TopicCommandHandler.java` | Verified handler pattern and switch-case structure | — |
| `applications/quizzes/src/main/java/.../topic/coordination/sagas/CreateTopicFunctionalitySagas.java` | Verified saga step structure: getCourseStep (READ_COURSE lock + compensation) → createTopicStep | — |
| `applications/quizzes/src/main/java/.../topic/coordination/sagas/DeleteTopicFunctionalitySagas.java` | Verified saga step structure: getTopicStep (READ_TOPIC lock + compensation) → deleteTopicStep | — |
| `applications/quizzes/src/main/java/.../topic/coordination/functionalities/TopicFunctionalities.java` | Confirmed TopicFunctionalities is a @Service wrapping sagas (not a @Profile-switched class) | session-b.md correctly specifies the pattern, but reference confirmed profile-agnostic approach in quizzes-full |

---

## Skill Instructions Feedback

### What worked well

- The prerequisite check for `GetTopicByIdCommand` in session-b.md was correct and caught that the delete saga needs the command before session c.
- The step-5b instruction to patch plan.md for missing files (TopicFunctionalities, GetTopicByIdCommand, event classes) worked well.

### What was unclear or missing

- session-b.md says "create GetTopicByIdCommand now if not yet existing" but doesn't explicitly say to also add `getTopicById` service method and handler case — these are implied. A future reader might create the command but miss the handler and service method, causing a compile error.
- The plan.md 2.3.b file table omitted: `TopicFunctionalities.java`, `GetTopicByIdCommand.java`, `events/DeleteTopicEvent.java`, `events/UpdateTopicEvent.java`. All four were unambiguous omissions added in step 5b.

### Suggested wording / structure changes

- session-b.md §Prerequisite — Get{Aggregate}ByIdCommand: add "Also add `getTopicById` to `{Aggregate}Service` and add a handler case for `Get{Aggregate}ByIdCommand` in `{Aggregate}CommandHandler`."

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/service.md` | No mention of the createTopic signature taking a pre-assembled owned-entity (`TopicCourse`) rather than a raw courseId | Medium | Add a note: when a saga assembles a value-object/owned-entity from a DTO (e.g. `TopicCourse` from `CourseDto`), the service method receives the assembled object, not the raw ID. |

---

## Patterns to Capture

- **Pattern:** TopicFunctionalities is always omitted from plan.md `2.N.b` file table (same as CourseFunctionalities, UserFunctionalities in previous sessions)
  **Observed in:** `applications/quizzes-full/plan.md` rows 2.1.b, 2.2.b, 2.3.b
  **Description:** The `{Aggregate}Functionalities.java` bean is always needed for session-b but keep getting omitted from the plan table. The classify-and-plan skill should auto-include it.

- **Pattern:** UpdateTopicEvent listed as published but no UpdateTopic write functionality exists in plan
  **Observed in:** `applications/quizzes-full/plan.md` §3 (Topic)
  **Description:** `UpdateTopicEvent` is listed in "Events published" for Topic, but there is no `UpdateTopic` write functionality in session 2.3.b (or any session). The event class was created to avoid downstream compilation failures (Question and Tournament subscribe to it). This is a gap in the plan that should be addressed: either add an UpdateTopic functionality or remove UpdateTopicEvent from the plan.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| High | `.claude/skills/implement-aggregate/session-b.md` | Add explicit note: when creating GetTopicByIdCommand, also add service method and handler case |
| Medium | `.claude/skills/classify-and-plan` or plan template | Auto-include `{Aggregate}Functionalities.java` in every 2.N.b file table |
| Low | `applications/quizzes-full/plan.md` | Resolve UpdateTopicEvent contradiction: either add UpdateTopic write functionality or remove event from published list |

---

## One-Line Summary

TopicFunctionalities (missing from plan) and GetTopicByIdCommand (moved from 2.3.c) were both unambiguous omissions that had to be created to make the delete saga compile and tests pass.

---

## Resolution

Resolved after retro review:

| Action item | Status | Notes |
|-------------|--------|-------|
| **High** — session-b.md GetByIdCommand prerequisite note | ✅ Applied | Extended the callout to explicitly say: also add service method + handler case, or a compile error results even when the command class exists. |
| **Medium** — classify-and-plan auto-include `{Aggregate}Functionalities.java` | ✅ Already present | `coordination/functionalities/{Aggregate}Functionalities.java` is in the 2.N.b template row AND has an explicit "Always include" callout note in `classify-and-plan/SKILL.md`. No change needed. |
| **Low** — Resolve UpdateTopicEvent contradiction in plan.md | ✅ Resolved | Added `UpdateTopic(topicId, name)` as a write functionality: domain model §4 updated, plan.md 2.3.b file list extended, Phase 3 T4 entry 3.25 (UpdateTopic + DeleteTopic) added. Implementation: `UpdateTopicCommand.java`, `UpdateTopicFunctionalitySagas.java`, `UpdateTopicTest.groovy` created; `TopicService`, `TopicCommandHandler`, `TopicFunctionalities` patched. |
