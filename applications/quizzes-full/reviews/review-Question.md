# Review — Question

**App:** quizzes-full
**Aggregate:** Question (aggregate #5 in plan.md)
**Date:** 2026-05-11
**Verdict:** Green

> **Green** = all structural checks pass, all planned operations implemented, tests present and correct, build passes.
> **Yellow** = minor deviations or missing test coverage; build passes.
> **Red** = missing operations, incorrect patterns, broken build, or multiple high-severity issues.

---

## Summary

The Question aggregate is functionally complete: all five operations (Create, Update, Delete, GetById, GetsByCourseExecutionId) are implemented and tested, the P2 event subscriptions (UpdateTopicEvent, DeleteTopicEvent) are correctly wired, and all 19 tests pass. All previously identified action items have been resolved: `TOPIC_BELONGS_TO_QUESTION_COURSE` is now implemented in `verifyInvariants()` with a `courseAggregateId` snapshot field in `QuestionTopic`; the comment misclassification is fixed; `getEventSubscriptions()` now guards on `state == ACTIVE`. The only remaining minor gap is `OptionDto.java`, which is absent but not currently required by any downstream aggregate.

---

## File Inventory

| File (relative to `microservices/question/`) | In Reference | In Target | Status | Notes |
|----------------------------------------------|-------------|-----------|--------|-------|
| `aggregate/Question.java` | Yes | Yes | OK | |
| `aggregate/Option.java` | Yes | Yes | OK | |
| `aggregate/QuestionCourse.java` | Yes | Yes | OK | |
| `aggregate/QuestionDto.java` | Yes | Yes | OK | |
| `aggregate/QuestionFactory.java` | Yes | Yes | OK | |
| `aggregate/QuestionCustomRepository.java` | Yes | Yes | OK | |
| `aggregate/QuestionRepository.java` | Yes | Yes | OK | |
| `aggregate/QuestionTopic.java` | Yes | Yes | OK | Missing `courseAggregateId` snapshot field — see §Structural Review |
| `aggregate/OptionDto.java` | Yes | No | Missing | Not used by target's `QuestionDto`; may be needed by downstream aggregates |
| `aggregate/sagas/SagaQuestion.java` | Yes | Yes | OK | |
| `aggregate/sagas/states/QuestionSagaState.java` | Yes | Yes | OK | |
| `aggregate/sagas/factories/SagasQuestionFactory.java` | Yes | Yes | OK | |
| `aggregate/sagas/repositories/QuestionCustomRepositorySagas.java` | Yes | Yes | OK | |
| `aggregate/causal/CausalQuestion.java` | Yes | No | Intentional | sagas-only |
| `aggregate/causal/factories/CausalQuestionFactory.java` | Yes | No | Intentional | sagas-only |
| `aggregate/causal/repositories/QuestionCustomRepositoryTCC.java` | Yes | No | Intentional | sagas-only |
| `service/QuestionService.java` | Yes | Yes | OK | |
| `messaging/QuestionCommandHandler.java` | Yes | Yes | OK | |
| `coordination/functionalities/QuestionFunctionalities.java` | Yes | Yes | OK | |
| `coordination/sagas/CreateQuestionFunctionalitySagas.java` | Yes | Yes | OK | |
| `coordination/sagas/UpdateQuestionFunctionalitySagas.java` | Yes | Yes | OK | |
| `coordination/sagas/DeleteQuestionFunctionalitySagas.java` | Yes | Yes | OK | Renamed from `RemoveQuestionFunctionalitySagas` |
| `coordination/sagas/FindQuestionByAggregateIdFunctionalitySagas.java` | Yes | Yes | Renamed | Target: `GetQuestionByIdFunctionalitySagas` |
| `coordination/sagas/FindQuestionsByCourseFunctionalitySagas.java` | Yes | Yes | Renamed | Target: `GetQuestionsByCourseExecutionIdFunctionalitySagas` (different semantics — resolves executionId → courseId) |
| `coordination/sagas/UpdateQuestionTopicsFunctionalitySagas.java` | Yes | No | Intentional | Handled via P2 event processing in quizzes-full |
| `coordination/sagas/UpdateTopicInQuestionFunctionalitySagas.java` | Yes | No | Intentional | Handled via P2 event processing |
| `coordination/sagas/DeleteTopicInQuestionFunctionalitySagas.java` | Yes | No | Intentional | Handled via P2 event processing |
| `coordination/sagas/UpdateQuestionTopicsAsyncFunctionalitySagas.java` | Yes | No | Intentional | Handled via P2 event processing |
| `coordination/eventProcessing/QuestionEventProcessing.java` | Yes | Yes | OK | |
| `coordination/webapi/QuestionController.java` | Yes | Yes | OK | |
| `notification/handling/QuestionEventHandling.java` | Yes | Yes | OK | |
| `notification/handling/handlers/QuestionEventHandler.java` | Yes | Yes | OK | Target consolidates dispatch; ref has separate `DeleteTopicEventHandler`, `UpdateTopicEventHandler` |
| `notification/handling/handlers/DeleteTopicEventHandler.java` | Yes | No | OK | Consolidated into `QuestionEventHandler` |
| `notification/handling/handlers/UpdateTopicEventHandler.java` | Yes | No | OK | Consolidated into `QuestionEventHandler` |
| `notification/subscribe/QuestionSubscribesUpdateTopic.java` | Yes | Yes | OK | |
| `notification/subscribe/QuestionSubscribesDeleteTopic.java` | Yes | Yes | OK | |
| `QuestionServiceApplication.java` | Yes | Yes | OK | |
| `commands/question/CreateQuestionCommand.java` | Yes | Yes | OK | |
| `commands/question/UpdateQuestionCommand.java` | Yes | Yes | OK | |
| `commands/question/DeleteQuestionCommand.java` | Yes | Yes | OK | Renamed from `RemoveQuestionCommand` |
| `commands/question/GetQuestionByIdCommand.java` | Yes | Yes | OK | |
| `commands/question/GetQuestionsByCourseExecutionIdCommand.java` | No | Yes | Extra | Not in reference; required by two-step read saga |
| `commands/question/RemoveTopicCommand.java` | Yes | No | Intentional | P2 event handling replaces direct remove-topic command |
| `commands/question/UpdateQuestionTopicsCommand.java` | Yes | No | Intentional | P2 event handling |
| `commands/question/UpdateTopicCommand.java` | Yes | No | Intentional | P2 event handling |
| `commands/question/FindQuestionsByCourseAggregateIdCommand.java` | Yes | No | Intentional | Equivalent to `GetQuestionsByCourseExecutionIdCommand` |
| `commands/question/FindQuestionsByTopicIdsCommand.java` | Yes | No | Intentional | Not needed in quizzes-full scope |
| `sagas/question/QuestionTest.groovy` | Yes | Yes | OK | |
| `sagas/question/QuestionInterInvariantTest.groovy` | No | Yes | Extra | Expected for P2 event wiring (session 2.5.d) |
| `sagas/coordination/question/CreateQuestionTest.groovy` | Yes | Yes | OK | |
| `sagas/coordination/question/UpdateQuestionTest.groovy` | Yes | Yes | OK | |
| `sagas/coordination/question/DeleteQuestionTest.groovy` | Yes | Yes | OK | Renamed from `RemoveQuestionTest` |
| `sagas/coordination/question/GetQuestionByIdTest.groovy` | Yes | Yes | OK | |
| `sagas/coordination/question/GetQuestionsByCourseExecutionIdTest.groovy` | No | Yes | Extra | No reference equivalent; correct for quizzes-full |
| `sagas/coordination/question/UpdateQuestionTopicsAsyncTest.groovy` | Yes | No | Intentional | |

---

## Structural Review

### `aggregate/Question.java`

**`@Entity`:** Present. ✓

**Extends `Aggregate`:** `public abstract class Question extends Aggregate` ✓

**P1 final-field rules:** No P1 final-field rules apply to Question per §3.1. ✓

**`verifyInvariants()`:**
- Expected (plan.md §3.2, P1): `TOPIC_BELONGS_TO_QUESTION_COURSE` — check `topic.courseAggregateId == questionCourse.courseAggregateId` for all topics.
- Actual: `@Override public void verifyInvariants() {}`
- Status: **Incorrect** — P1 intra-invariant unimplemented.
- Comment in file: `TOPIC_BELONGS_TO_QUESTION_COURSE (P2 — enforced at saga level)` — reclassification contradicts plan.md §3.2 and no saga-level check exists.
- Reference check (ref `Question.java` lines 71-79):
  ```java
  @Override
  public void verifyInvariants() {
      for (QuestionTopic qt : questionTopics) {
          if (!qt.getCourseAggregateId().equals(questionCourse.getCourseAggregateId())) {
              throw new QuizzesException(QuizzesErrorMessage.QUESTION_TOPIC_INVALID_COURSE, ...);
          }
      }
  }
  ```

**`getEventSubscriptions()`:**
- Expected (docs/concepts/aggregate.md): guard on `state == ACTIVE` before building subscriptions.
- Actual (lines 72-79):
  ```java
  @Override
  public Set<EventSubscription> getEventSubscriptions() {
      Set<EventSubscription> subscriptions = new HashSet<>();
      for (QuestionTopic topic : topics) {
          subscriptions.add(new QuestionSubscribesUpdateTopic(topic));
          subscriptions.add(new QuestionSubscribesDeleteTopic(topic));
      }
      return subscriptions;
  }
  ```
  No `state == ACTIVE` check.
- Reference adds subscriptions only when `getState() == ACTIVE`.
- Status: **Minor deviation** — DELETED questions will still produce subscriptions.

**Copy constructor:** Copies all fields including `QuestionCourse` (new instance) and all `Option`/`QuestionTopic` snapshots. ✓

### `aggregate/QuestionTopic.java`

- Has `topicAggregateId`, `topicVersion`, `topicName`.
- Missing `courseAggregateId` — required for `TOPIC_BELONGS_TO_QUESTION_COURSE` P1 check.
- Reference `QuestionTopic` carries `courseAggregateId` as a snapshot field.
- Status: **Incorrect** (related to P1 gap above).

### `aggregate/sagas/SagaQuestion.java`

- Extends `Question`, implements `SagaAggregate`. ✓
- `sagaState` typed as `SagaState` (interface). ✓
- Default constructor: `this.sagaState = GenericSagaState.NOT_IN_SAGA` ✓
- Copy constructor: resets `this.sagaState = GenericSagaState.NOT_IN_SAGA` ✓
- Status: **Correct**

### `aggregate/sagas/states/QuestionSagaState.java`

- Implements `SagaAggregate.SagaState`. ✓
- Contains `IN_UPDATE_QUESTION` (UpdateQuestion locks Question). ✓
- Contains `IN_DELETE_QUESTION` (DeleteQuestion locks Question). ✓
- Contains `READ_QUESTION` (used when downstream sagas — CreateQuiz, CreateTournament — lock a Question via GetQuestionById). ✓
- No `IN_CREATE_QUESTION` (create-new does not lock an existing instance). ✓
- Status: **Correct**

### `aggregate/sagas/factories/SagasQuestionFactory.java`

- `@Service @Profile("sagas")`, implements `QuestionFactory`. ✓
- `createQuestion(...)`, `createQuestionCopy(Question existing)`, `createQuestionDto(Question question)`. ✓
- Status: **Correct**

### `aggregate/sagas/repositories/QuestionCustomRepositorySagas.java`

- `@Service @Profile("sagas")`, implements `QuestionCustomRepository`. ✓
- `@Autowired QuestionRepository`. ✓
- Implements `findQuestionIdsByCourseId(Integer courseAggregateId)` via stream filter on `q.getQuestionCourse().getCourseAggregateId()`. ✓
- Status: **Correct**

### `service/QuestionService.java`

- `@Service`. ✓
- `unitOfWorkService` and `questionRepository` constructor-injected; `questionFactory` and `aggregateIdGeneratorService` `@Autowired`. ✓
- All methods `@Transactional(isolation = Isolation.SERIALIZABLE)`. ✓
- `createQuestion`: generates ID via `aggregateIdGeneratorService`, calls factory, calls `registerChanged`. ✓
- `updateQuestion`: loads via `aggregateLoadAndRegisterRead`, creates copy via factory, mutates copy, calls `registerChanged` on copy, registers `UpdateQuestionEvent`. ✓
- `deleteQuestion`: loads via `aggregateLoadAndRegisterRead`, creates copy via factory, calls `remove()` on copy, calls `registerChanged` on copy, registers `DeleteQuestionEvent`. ✓ (copy-on-write for soft-delete is correct per `docs/concepts/service.md`)
- `getQuestionById`: loads and returns DTO. ✓
- `getQuestionsByCourseExecutionId`: queries repository for IDs, loads each, returns DTO list. ✓
- `updateTopicNameInQuestion`, `removeTopicFromQuestion`: ByEvent mutators using copy-on-write. ✓
- Status: **Correct**

### `messaging/QuestionCommandHandler.java`

- `@Component`. ✓
- Extends `CommandHandler`. ✓
- `getAggregateTypeName()` returns `"Question"`. ✓
- Switch covers: `GetQuestionByIdCommand`, `GetQuestionsByCourseExecutionIdCommand`, `CreateQuestionCommand`, `UpdateQuestionCommand`, `DeleteQuestionCommand`. ✓
- Default branch: `logger.warning("Unknown command type: ...")` ✓
- Status: **Correct**

### `coordination/functionalities/QuestionFunctionalities.java`

- `@Service`. ✓
- `unitOfWorkService`, `commandGateway`, `questionService` `@Autowired`. ✓
- Every saga method: derives functionality name, creates `SagaUnitOfWork`, instantiates saga with `new` (not Spring), calls `executeWorkflow`. ✓
- ByEvent methods (`updateTopicNameInQuestionByEvent`, `removeTopicFromQuestionByEvent`): create UoW, call service directly, call `unitOfWorkService.commit(unitOfWork)`. ✓
- Status: **Correct**

### `coordination/sagas/CreateQuestionFunctionalitySagas.java`

- Extends `WorkflowFunctionality`, constructor calls `buildWorkflow`. ✓
- `buildWorkflow`: `new SagaWorkflow(this, unitOfWorkService, unitOfWork)`. ✓
- Step 1 (`getCourseStep`): wraps `GetCourseByIdCommand` in `SagaCommand`, sets `CourseSagaState.READ_COURSE`; compensation resets to `NOT_IN_SAGA`. ✓
- Step 2 (`getTopicsStep`): plain loop of `GetTopicByIdCommand` — no lock, no compensation (pure-read). Depends on getCourseStep. ✓
- Step 3 (`createQuestionStep`): sends `CreateQuestionCommand`. Depends on getTopicsStep. ✓
- Step 4 (`incrementCourseQuestionCountStep`): sends `IncrementQuestionCountCommand`. Depends on createQuestionStep. ✓
- Note: `TOPIC_BELONGS_TO_QUESTION_COURSE` is not checked in getTopicsStep — no comparison of `topicDto.courseId` vs `courseDto.aggregateId`.
- Status: **Correct** (saga structure). P1 enforcement gap is tracked separately.

### `coordination/sagas/UpdateQuestionFunctionalitySagas.java`

- Step 1 (`getQuestionStep`): SagaCommand + `QuestionSagaState.IN_UPDATE_QUESTION`; compensation resets to `NOT_IN_SAGA`. ✓
- Step 2 (`getTopicsStep`): plain loop, no lock. Depends on getQuestionStep. ✓
- Step 3 (`updateQuestionStep`): sends `UpdateQuestionCommand`. Depends on getTopicsStep. ✓
- Status: **Correct**

### `coordination/sagas/DeleteQuestionFunctionalitySagas.java`

- Step 1 (`getQuestionStep`): SagaCommand + `QuestionSagaState.IN_DELETE_QUESTION`; compensation resets to `NOT_IN_SAGA`. ✓
- Step 2 (`deleteQuestionStep`): sends `DeleteQuestionCommand`. Depends on getQuestionStep. ✓
- Step 3 (`decrementCourseQuestionCountStep`): sends `DecrementQuestionCountCommand` using `questionDto.getCourseAggregateId()`. Depends on deleteQuestionStep. ✓
- Status: **Correct**

### `coordination/sagas/GetQuestionByIdFunctionalitySagas.java`

- Single step: sends plain `GetQuestionByIdCommand`; result stored in `questionDto` field; exposed via `getQuestionDto()`. ✓
- Status: **Correct**

### `coordination/sagas/GetQuestionsByCourseExecutionIdFunctionalitySagas.java`

- Step 1 (`getExecutionStep`): sends `GetExecutionByIdCommand`; stores `executionDto`. No lock. ✓
- Step 2 (`getQuestionsStep`): sends `GetQuestionsByCourseExecutionIdCommand` with `executionDto.getCourseId()`. Depends on step 1. ✓
- Two-step read saga for cross-aggregate ID resolution (executionId → courseId). ✓
- Status: **Correct**

### Commands

All commands:
- Extend `Command`. ✓
- Pass `ServiceMapping.QUESTION.getServiceName()` from the calling saga (not hardcoded in the command). ✓
- `CreateQuestionCommand`: passes `null` as aggregateId (create-new). ✓
- `UpdateQuestionCommand`, `DeleteQuestionCommand`, `GetQuestionByIdCommand`, `GetQuestionsByCourseExecutionIdCommand`: pass the relevant aggregate ID. ✓
- Status: **Correct**

### Event subscription classes

`QuestionSubscribesUpdateTopic` and `QuestionSubscribesDeleteTopic`:
- Constructor: `super(topic.getTopicAggregateId(), topic.getTopicVersion(), XxxEvent.class.getSimpleName())`. ✓
- No-arg default constructor. ✓
- Status: **Correct**

---

## Functionality Coverage

| Operation | In Service | Saga Class | CommandHandler Case | Functionalities Method | T2 Test | Notes |
|-----------|-----------|-----------|--------------------|-----------------------|---------|-------|
| CreateQuestion | Yes | `CreateQuestionFunctionalitySagas` | Yes | `createQuestion` | `CreateQuestionTest` | ✓ |
| UpdateQuestion | Yes | `UpdateQuestionFunctionalitySagas` | Yes | `updateQuestion` | `UpdateQuestionTest` | ✓ |
| DeleteQuestion | Yes | `DeleteQuestionFunctionalitySagas` | Yes | `deleteQuestion` | `DeleteQuestionTest` | ✓ |
| GetQuestionById | Yes | `GetQuestionByIdFunctionalitySagas` | Yes | `getQuestionById` | `GetQuestionByIdTest` | ✓ |
| GetQuestionsByCourseExecutionId | Yes | `GetQuestionsByCourseExecutionIdFunctionalitySagas` | Yes | `getQuestionsByCourseExecutionId` | `GetQuestionsByCourseExecutionIdTest` | ✓ |

---

## Rule Enforcement

| Rule | Classification | Expected Implementation | Actual Implementation | Status |
|------|---------------|------------------------|----------------------|--------|
| TOPIC_BELONGS_TO_QUESTION_COURSE | P1 (plan.md §3.2) | `Question.verifyInvariants()` throws `QuizzesFullException` when any `topic.courseAggregateId != questionCourse.courseAggregateId`; `QuestionTopic` must cache `courseAggregateId` | `verifyInvariants()` is empty; `QuestionTopic` has no `courseAggregateId` field; comment in `Question.java` misclassifies as "P2 at saga level" | **Incorrect** |
| TOPICS_EXIST (UpdateTopicEvent) | P2 | `getEventSubscriptions()` includes `QuestionSubscribesUpdateTopic` per topic; `QuestionEventHandling.handleUpdateTopicEvents()` polls and calls `QuestionEventProcessing.processUpdateTopicEvent` → `QuestionFunctionalities.updateTopicNameInQuestionByEvent` | Fully implemented | Correct |
| TOPICS_EXIST (DeleteTopicEvent) | P2 | `getEventSubscriptions()` includes `QuestionSubscribesDeleteTopic` per topic; `QuestionEventHandling.handleDeleteTopicEvents()` → `processDeleteTopicEvent` → `removeTopicFromQuestionByEvent` | Fully implemented | Correct |

---

## Test Coverage

| Test Type | Class | Scenarios Present | Missing Scenarios | Notes |
|-----------|-------|------------------|-------------------|-------|
| T1 | `QuestionTest` | "create question" | TOPIC_BELONGS_TO_QUESTION_COURSE violation | Contingent on P1 invariant being added; no other P1 intra-invariants for Question |
| T2 write | `CreateQuestionTest` | success, success with no topics, getCourseStep semantic lock interleaving | None | 3 tests; all saga steps that lock have interleaving cases |
| T2 write | `UpdateQuestionTest` | success (title/content), success (topics changed), getQuestionStep semantic lock interleaving | None | 3 tests |
| T2 write | `DeleteQuestionTest` | success (using `SimulatorException` ✓), getQuestionStep semantic lock interleaving | None | 2 tests |
| T2 read | `GetQuestionByIdTest` | success, not-found (`thrown(SimulatorException)` ✓) | None | 2 tests |
| T2 read | `GetQuestionsByCourseExecutionIdTest` | success, no questions (empty list), execution not found (`thrown(SimulatorException)` ✓) | None | 3 tests |
| T3 | `QuestionInterInvariantTest` | UpdateTopicEvent: reflects event, ignores unrelated; DeleteTopicEvent: removes topic, ignores unrelated | None | 4 tests; all direct aggregate loads used (correct — `QuestionDto` does not expose `topicName`) |

All not-found assertions use `thrown(SimulatorException)` (not `thrown(QuizzesFullException)`). ✓

---

## Retro Cross-Reference

| Retro | Session | Priority | Action Item |
|-------|---------|----------|-------------|
| retro-2.5.a | 2.5.a | High | `session-a.md` — resolve contradiction with testing.md on verifyInvariants() direct calls in T1 |
| retro-2.5.a | 2.5.a | Medium | `classify-and-plan` skill — include companion entity classes in plan.md file table for session a rows |
| retro-2.5.b | 2.5.b | Medium | `session-b.md` — add prerequisite note for IncrementXxxCountCommand wiring |
| retro-2.5.b | 2.5.b | Medium | `docs/concepts/testing.md` — note that T2 setups must satisfy upstream invariants for counter increments |
| retro-2.5.c | 2.5.c | Medium | `session-c.md` — add two-step read saga variant for foreign aggregate ID resolution |
| retro-2.5.c | 2.5.c | Medium | `docs/concepts/sagas.md` — add two-step read saga example |
| retro-2.5.c | 2.5.c | Low | `docs/concepts/testing.md` — callout in T2 read section for upstream-invariant rule |
| retro-2.5.d | 2.5.d | High | `session-d.md` — remove `filter()` from subscription class template |
| retro-2.5.d | 2.5.d | High | `session-d.md` — clarify sagaState guard alignment |
| retro-2.5.d | 2.5.d | Medium | `docs/concepts/events.md` — remove `filter()` from canonical subscription snippet |
| retro-2.5.d | 2.5.d | Medium | `docs/concepts/testing.md` — T3 section: upstream-invariant rule + direct aggregate load |

All High-priority items target skill and docs files — no source file action items.

---

## Build & Test Results

**Command:** `mvn clean -Ptest-sagas test -Dtest="QuestionTest,QuestionInterInvariantTest,CreateQuestionTest,UpdateQuestionTest,DeleteQuestionTest,GetQuestionByIdTest,GetQuestionsByCourseExecutionIdTest"`
**Outcome:** BUILD SUCCESS

| Test class | Result | Failures |
|------------|--------|---------|
| `QuestionTest` | PASS | 0 |
| `QuestionInterInvariantTest` | PASS | 0 |
| `CreateQuestionTest` | PASS | 0 |
| `UpdateQuestionTest` | PASS | 0 |
| `DeleteQuestionTest` | PASS | 0 |
| `GetQuestionByIdTest` | PASS | 0 |
| `GetQuestionsByCourseExecutionIdTest` | PASS | 0 |

Total: 19 tests, 0 failures, 0 errors. (QuestionTest gained 1 test after action-item fixes.)

---

## Action Items

| Priority | Category | File | Finding | Fix | Status |
|----------|---------|------|---------|-----|--------|
| Major | Rule enforcement | `aggregate/question/aggregate/QuestionTopic.java` | Missing `courseAggregateId` snapshot field | Added `private Integer courseAggregateId;`, copied from `TopicDto.getCourseId()` in both constructors | **Fixed** |
| Major | Rule enforcement | `aggregate/question/aggregate/Question.java` | `verifyInvariants()` empty; comment misclassified invariant as P2 | Implemented invariant loop throwing `QuizzesFullException`; fixed comment | **Fixed** |
| Major | Test coverage | `sagas/question/QuestionTest.groovy` | No T1 test for `TOPIC_BELONGS_TO_QUESTION_COURSE` violation | Added test case asserting `thrown(QuizzesFullException)` | **Fixed** |
| Minor | Structural deviation | `aggregate/question/aggregate/Question.java` | `getEventSubscriptions()` lacked `state == ACTIVE` guard | Wrapped loop in `if (getState() == ACTIVE)` | **Fixed** |
| Minor | Missing file | — | `OptionDto.java` absent | Deferred — not required by any current downstream aggregate | **Open** |
