# Retro — 2.5.d — Question

**App:** quizzes-full
**Session:** 2.5.d (Event Wiring)
**Date:** 2026-05-05

---

## Files Produced

- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/notification/subscribe/QuestionSubscribesUpdateTopic.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/notification/subscribe/QuestionSubscribesDeleteTopic.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/notification/handling/QuestionEventHandling.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/notification/handling/handlers/QuestionEventHandler.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/coordination/eventProcessing/QuestionEventProcessing.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/question/QuestionInterInvariantTest.groovy`
- Modified: `applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/aggregate/Question.java`
- Modified: `applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/service/QuestionService.java`
- Modified: `applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/coordination/functionalities/QuestionFunctionalities.java`
- Modified: `applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/BeanConfigurationSagas.groovy`
- Modified: `applications/quizzes-full/plan.md`

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/events.md` | EventSubscription, EventHandler, EventHandling, EventProcessing, Canonical Wiring Snippet | Partial | `filter()` method shown in canonical snippet doesn't exist on `EventSubscription` — the matching is done by `subscribesEvent()` in the simulator |
| `docs/concepts/testing.md` | T3 section | Partial | Template uses `<consumer>Service.get<Consumer>()` but `QuestionDto` doesn't expose topic names — needed direct aggregate load via `unitOfWorkService.aggregateLoadAndRegisterRead()` |
| `.claude/skills/implement-aggregate/session-d.md` | All sections | Partial | sagaState guard mentioned for ByEvent methods but not present in existing `ExecutionService` pattern; docs and code are inconsistent |

---

## Skill Instructions Feedback

### What worked well

- The single-class `EventHandler` with `instanceof` dispatch pattern was clear and produced correct code without ambiguity.
- The `ByEvent` method pattern (Functionalities → Service, own UoW, no saga) was clear.
- The BeanConfigurationSagas update instructions (3 beans: EventHandling, EventHandler with repo, EventProcessing) matched the Execution reference exactly.

### What was unclear or missing

- The `session-d.md` canonical snippet shows `filter()` on the subscription class, but the actual `EventSubscription` base class only has `subscribesEvent()`. The filter method doesn't exist. Checking the simulator confirmed this — the subscription class only needs a constructor and empty default constructor.
- The `session-d.md` says ByEvent methods should include a `sagaState != NOT_IN_SAGA` guard, but the existing Execution pattern doesn't include this guard at any layer. The inconsistency required checking the codebase.
- `session-d.md` says the T3 template uses `<consumer>Service.get<Consumer>()` to assert state, but the `QuestionDto` doesn't expose cached `topicName` fields. Fell back to `unitOfWorkService.aggregateLoadAndRegisterRead()` with a cast to `Question`.

### Suggested wording / structure changes

- `session-d.md` should remove the `filter()` method from the subscription class template and replace it with just the constructor + empty constructor, matching how `EventSubscription.subscribesEvent()` actually works.
- `session-d.md` should reconcile the sagaState guard: either remove the mention or add it consistently to the Execution reference and document why the existing code omits it.
- `session-d.md` T3 section should note that when `QuestionDto` (or similar) doesn't expose cached sub-entity fields, the test should use `unitOfWorkService.aggregateLoadAndRegisterRead()` to load the aggregate directly.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/events.md` | Canonical snippet shows `filter()` method on subscription class; method doesn't exist on `EventSubscription` base class | Medium | Remove `filter()` from canonical snippet; explain that `subscribesEvent()` handles matching via `subscribedAggregateId` and `subscribedVersion` |
| `.claude/skills/implement-aggregate/session-d.md` | sagaState guard mentioned but not implemented in codebase | Medium | Align: either add guard to existing ExecutionService pattern and document it, or remove the mention from session-d.md |
| `docs/concepts/testing.md` | T3 template assumes DTO exposes cached fields directly; doesn't address case where sub-entity fields aren't in the DTO | Low | Add note: when DTO doesn't expose the cached field, load the aggregate directly via `unitOfWorkService.aggregateLoadAndRegisterRead()` |

---

## Patterns to Capture

- **Pattern:** Upstream invariant prerequisite in T3 tests
  **Observed in:** `QuestionInterInvariantTest.groovy`
  **Description:** `Course` enforces `executionCount > 0` when `questionCount > 0`. Any test that creates a `Question` must first call `createExecution()`. This applies to T3 tests as well as T2 tests — the T2 docs mention the upstream-invariant rule but the T3 template doesn't reference it. Adding `createExecution()` before `createQuestion()` in every T3 test was required.

- **Pattern:** Direct aggregate load for T3 assertions when DTO omits sub-entity cached fields
  **Observed in:** `QuestionInterInvariantTest.groovy`
  **Description:** When the aggregate's DTO (e.g., `QuestionDto`) only exposes IDs of sub-entities (topicIds) and not their cached name fields (topicName), use `unitOfWorkService.aggregateLoadAndRegisterRead(id, uow)` cast to the aggregate base class to assert cached field values directly.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| High | `.claude/skills/implement-aggregate/session-d.md` | Remove `filter()` method from subscription class template; explain `subscribesEvent()` does matching |
| High | `.claude/skills/implement-aggregate/session-d.md` | Clarify sagaState guard: align with codebase or document why it's omitted |
| Medium | `docs/concepts/events.md` | Remove `filter()` from canonical subscription snippet |
| Medium | `docs/concepts/testing.md` | Add note to T3 section: upstream-invariant rule applies; direct aggregate load when DTO omits cached sub-entity fields |

---

## One-Line Summary

The `filter()` method on subscription classes is a docs fiction — `EventSubscription.subscribesEvent()` handles all matching — and the T3 test needed direct aggregate loading (not DTO) to assert topic name changes.
