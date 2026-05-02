# Review — Topic

**App:** quizzes-full
**Aggregate:** Topic (aggregate #3 in plan.md)
**Date:** 2026-05-02
**Verdict:** Yellow

> **Green** = all structural checks pass, all planned operations implemented, tests present and correct, build passes.
> **Yellow** = minor deviations or missing test coverage; build passes.
> **Red** = missing operations, incorrect patterns, broken build, or multiple high-severity issues.

---

## Summary

All five operations (CreateTopic, UpdateTopic, DeleteTopic, GetTopicById, GetTopicsByCourseId) are fully implemented and the build passes all 11 tests. Structural patterns are correct throughout except for two recurring minor deviations (`@Service` on CommandHandler, copy-on-write in deleteTopic). The primary shortfall is missing T2 interleaving scenarios: UpdateTopicTest has no step-interleaving case despite `getTopicStep` acquiring `READ_TOPIC`, and CreateTopicTest has no interleaving case despite `getCourseStep` acquiring `READ_COURSE`. Both missing cases are Major per the testing spec. DeleteTopicTest correctly includes the interleaving case, demonstrating the pattern is understood.

---

## File Inventory

| File (relative to microservices/topic/) | In Reference | In Target | Status | Notes |
|----------------------------------------|-------------|-----------|--------|-------|
| `aggregate/Topic.java` | Yes | Yes | OK | |
| `aggregate/TopicCourse.java` | Yes | Yes | OK | |
| `aggregate/TopicDto.java` | Yes | Yes | OK | |
| `aggregate/TopicFactory.java` | Yes | Yes | OK | |
| `aggregate/TopicRepository.java` | Yes | Yes | OK | |
| `aggregate/TopicCustomRepository.java` | Yes | Yes | OK | Extended in 2.3.c with `findTopicIdsByCourseId` |
| `aggregate/sagas/SagaTopic.java` | Yes | Yes | OK | |
| `aggregate/sagas/states/TopicSagaState.java` | Yes | Yes | OK | |
| `aggregate/sagas/factories/SagasTopicFactory.java` | Yes | Yes | OK | |
| `aggregate/sagas/repositories/TopicCustomRepositorySagas.java` | Yes | Yes | OK | Implements `findTopicIdsByCourseId` |
| `aggregate/causal/` subtree | Yes | No | Intentional | sagas-only implementation |
| `service/TopicService.java` | Yes | Yes | OK | |
| `messaging/TopicCommandHandler.java` | Yes | Yes | OK | |
| `coordination/functionalities/TopicFunctionalities.java` | Yes | Yes | OK | |
| `coordination/sagas/CreateTopicFunctionalitySagas.java` | Yes | Yes | OK | |
| `coordination/sagas/UpdateTopicFunctionalitySagas.java` | Yes | Yes | OK | Added in retro-2.3.b resolution |
| `coordination/sagas/DeleteTopicFunctionalitySagas.java` | Yes | Yes | OK | |
| `coordination/sagas/GetTopicByIdFunctionalitySagas.java` | Yes | Yes | OK | |
| `coordination/sagas/GetTopicsByCourseIdFunctionalitySagas.java` | Yes | Yes | OK | Ref: `FindTopicsByCourseFunctionalitySagas` |
| `coordination/causal/` subtree | Yes | No | Intentional | sagas-only implementation |
| `coordination/webapi/TopicController.java` | Yes | Yes | OK | |
| `TopicServiceApplication.java` | Yes | Yes | OK | |
| `commands/topic/CreateTopicCommand.java` | Yes | Yes | OK | |
| `commands/topic/UpdateTopicCommand.java` | Yes | Yes | OK | |
| `commands/topic/DeleteTopicCommand.java` | Yes | Yes | OK | |
| `commands/topic/GetTopicByIdCommand.java` | Yes | Yes | OK | |
| `commands/topic/GetTopicsByCourseIdCommand.java` | Yes | Yes | OK | Ref: `FindTopicsByCourseIdCommand` |
| `sagas/topic/TopicTest.groovy` | No | Yes | OK | |
| `sagas/coordination/topic/CreateTopicTest.groovy` | No | Yes | OK | |
| `sagas/coordination/topic/UpdateTopicTest.groovy` | No | Yes | OK | Added in retro-2.3.b resolution |
| `sagas/coordination/topic/DeleteTopicTest.groovy` | No | Yes | OK | |
| `sagas/coordination/topic/GetTopicByIdTest.groovy` | No | Yes | OK | |
| `sagas/coordination/topic/GetTopicsByCourseIdTest.groovy` | No | Yes | OK | |

---

## Structural Review

### `aggregate/Topic.java`

Expected: `abstract`, `@Entity`, extends `Aggregate`, no P1 final fields, empty `verifyInvariants()`, `getEventSubscriptions()` returns empty set, copy constructor copies all fields.

Actual: All correct. Copy constructor:
```java
public Topic(Topic other) {
    super(other);
    setName(other.getName());
    setTopicCourse(new TopicCourse(other.getTopicCourse()));
}
```
`setTopicCourse` wires the back-reference (`topicCourse.setTopic(this)`). `verifyInvariants()` is empty — correct, no intra-invariants defined. `getEventSubscriptions()` returns empty `HashSet` — correct, no subscribed events.

**Status:** Correct

### `aggregate/TopicCourse.java`

Expected: `@Entity`, owns FK via plain `@OneToOne`, `@JsonIgnore` on `getTopic()`.

Actual: Correct. `@OneToOne` on the `topic` field (owns the FK), `@JsonIgnore` on getter, copy constructor omits the `topic` back-reference (correct — the aggregate's `setTopicCourse` re-wires it).

**Status:** Correct

### `aggregate/sagas/SagaTopic.java`

Expected: extends `Topic`, implements `SagaAggregate`, `sagaState` typed as `SagaState`, default constructor sets `NOT_IN_SAGA`, copy constructor resets to `NOT_IN_SAGA`.

Actual:
```java
public SagaTopic(SagaTopic other) {
    super(other);
    this.sagaState = GenericSagaState.NOT_IN_SAGA;
}
```
Copy constructor resets to `NOT_IN_SAGA` (does not copy `other.getSagaState()`). Correct per quizzes-full convention.

**Status:** Correct

### `aggregate/sagas/states/TopicSagaState.java`

Expected: implements `SagaAggregate.SagaState`, `READ_TOPIC` state present (Topic is locked by UpdateTopic and DeleteTopic sagas), no `IN_UPDATE`/`IN_DELETE` (those sagas do not separate read and write steps with a named intermediate state).

Actual: Only `READ_TOPIC`. Correct — both UpdateTopic and DeleteTopic acquire `READ_TOPIC` in their lock step.

**Status:** Correct

### `aggregate/sagas/factories/SagasTopicFactory.java`

Expected: `@Service @Profile("sagas")`, implements `TopicFactory`, three methods: `createTopic`, `createTopicCopy`, `createTopicDto`.

Actual: All correct. Method names match convention. `createTopicCopy` casts to `SagaTopic` before calling copy constructor — correct.

**Status:** Correct

### `aggregate/sagas/repositories/TopicCustomRepositorySagas.java`

Expected: `@Service @Profile("sagas")`, concrete class implementing `TopicCustomRepository`, `@Autowired TopicRepository`.

Actual: Correct. `findTopicIdsByCourseId` uses `topicRepository.findAll().stream().filter(...).map(Topic::getAggregateId).distinct().collect(...)`. Filter uses `courseAggregateId.equals(...)` (null-safe).

**Status:** Correct

### `service/TopicService.java`

Expected: `@Service`, constructor injection for repository and unitOfWorkService, `@Autowired` factory. All methods `@Transactional(isolation = Isolation.SERIALIZABLE)`. Create uses `aggregateIdGeneratorService`. Mutate follows copy-on-write. Delete: skill spec says in-place mutation (SAME instance), but reference app creates a copy.

Actual: `deleteTopic` creates a copy before calling `remove()`:
```java
Topic oldTopic = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(topicAggregateId, unitOfWork);
Topic newTopic = topicFactory.createTopicCopy(oldTopic);
newTopic.remove();
unitOfWorkService.registerChanged(newTopic, unitOfWork);
```
This is the copy-on-write approach, consistent with the reference `TopicService.deleteTopic`. The skill review spec says "SAME instance" but both reference and target contradict this. Noting as minor deviation from spec, consistent with reference.

`getTopicById` is correctly positioned before `createTopic` (added in 2.3.b per retro). All methods have `@Transactional`.

**Status:** Minor deviation (deleteTopic copy vs. in-place spec, consistent with reference)

### `messaging/TopicCommandHandler.java`

Expected: `@Component` (not `@Service`), extends `CommandHandler`, `getAggregateTypeName()` returns `"Topic"`, switch covers all 5 commands, default branch logs warning.

Actual: annotated `@Service` (not `@Component`). Reference app also uses `@Service`. Switch is exhaustive — all 5 commands covered. Default logs:
```java
logger.warning("Unknown command type: " + command.getClass().getName());
yield null;
```
`getAggregateTypeName()` returns `"Topic"`. Correct.

**Status:** Minor deviation (`@Service` instead of `@Component`; consistent with reference, inconsistent with skill spec)

### `coordination/functionalities/TopicFunctionalities.java`

Expected: `@Service`, one method per functionality, saga instantiated with `new`, `executeWorkflow` called, result returned via getter.

Actual: All five functionalities present. P3 guard in `updateTopic`:
```java
if (topicDto.getName() == null) {
    throw new QuizzesFullException(TOPIC_MISSING_NAME);
}
```
Guard placed correctly in the functionalities method (not in service). `createTopic` returns `saga.getCreatedTopicDto()`. `getTopicById` returns `saga.getTopicDto()`. `getTopicsByCourseId` returns `saga.getTopics()`. All correct.

**Status:** Correct

### `coordination/sagas/CreateTopicFunctionalitySagas.java`

Expected: extends `WorkflowFunctionality`, constructor calls `buildWorkflow`, two-step pattern: getCourseStep (READ_COURSE lock + compensation) → createTopicStep.

Actual: Correct. `getCourseStep` wraps `GetCourseByIdCommand` in `SagaCommand`, sets `CourseSagaState.READ_COURSE`, compensation releases to `NOT_IN_SAGA`. `createTopicStep` declares `getCourseStep` as dependency.

**Status:** Correct

### `coordination/sagas/UpdateTopicFunctionalitySagas.java`

Expected: two-step pattern: getTopicStep (READ_TOPIC lock + compensation) → updateTopicStep.

Actual: Correct. `getTopicStep` wraps `GetTopicByIdCommand` in `SagaCommand`, sets `TopicSagaState.READ_TOPIC`, compensation releases to `NOT_IN_SAGA`. `updateTopicStep` depends on `getTopicStep`.

**Status:** Correct

### `coordination/sagas/DeleteTopicFunctionalitySagas.java`

Expected: two-step pattern: getTopicStep (READ_TOPIC lock + compensation) → deleteTopicStep.

Actual: Correct. Same pattern as UpdateTopic. `deleteTopicStep` sends `DeleteTopicCommand`.

**Status:** Correct

### `coordination/sagas/GetTopicByIdFunctionalitySagas.java`

Expected: single step, no compensation, result stored in field, exposed via `getTopicDto()`.

Actual: Correct. Sends `GetTopicByIdCommand` directly (no `SagaCommand` wrapper — correct for read with no lock).

**Status:** Correct

### `coordination/sagas/GetTopicsByCourseIdFunctionalitySagas.java`

Expected: single step, `List<TopicDto>` field and `getTopics()` getter.

Actual:
```java
private List<TopicDto> topics;
...
this.topics = (List<TopicDto>) commandGateway.send(cmd);
...
public List<TopicDto> getTopics() { return topics; }
```
Correct list-return pattern.

**Status:** Correct

### Commands

All five commands extend `Command` and call `super(unitOfWork, serviceName, aggregateId)`. `CreateTopicCommand` passes `null` as aggregateId (correct — new aggregate). `GetTopicsByCourseIdCommand` passes `courseAggregateId` as aggregateId — unusual but harmless since no semantic lock is used.

**Status:** Correct

---

## Functionality Coverage

| Operation | In Service | Saga Class | CommandHandler Case | Functionalities Method | T2 Test | Notes |
|-----------|-----------|-----------|--------------------|-----------------------|---------|-------|
| CreateTopic | Yes | CreateTopicFunctionalitySagas | Yes | Yes | Yes | Missing interleaving case |
| UpdateTopic | Yes | UpdateTopicFunctionalitySagas | Yes | Yes | Yes | Missing interleaving case |
| DeleteTopic | Yes | DeleteTopicFunctionalitySagas | Yes | Yes | Yes | Has interleaving case ✓ |
| GetTopicById | Yes | GetTopicByIdFunctionalitySagas | Yes | Yes | Yes | |
| GetTopicsByCourseId | Yes | GetTopicsByCourseIdFunctionalitySagas | Yes | Yes | Yes | |

---

## Rule Enforcement

No Topic-specific rules appear in §3.1 or §3.2 of plan.md. Topic has no P1 final fields, no P1 intra-invariants, no P2 subscribed events, no P3 service guards, and no P4 cross-aggregate data-assembly rules.

| Rule | Classification | Expected Impl | Actual Impl | Status |
|------|---------------|--------------|-------------|--------|
| TOPIC_MISSING_NAME (implicit) | P3 | Guard in functionalities method | `if (topicDto.getName() == null) throw QuizzesFullException(TOPIC_MISSING_NAME)` in `TopicFunctionalities.updateTopic` | Correct |

Nothing to report for P1, P2, P4.

---

## Test Coverage

| Test Type | Class | Scenarios Present | Missing Scenarios | Notes |
|-----------|-------|------------------|-------------------|-------|
| T1 | TopicTest | create topic (valid data) | — | No P1 intra-invariants → no negative T1 cases expected |
| T2 write | CreateTopicTest | success; success second topic on same course | Step-interleaving: getCourseStep acquires READ_COURSE on Course | getCourseStep calls setSemanticLock — interleaving case required per spec |
| T2 write | UpdateTopicTest | success; null name throws QuizzesFullException | Step-interleaving: getTopicStep acquires READ_TOPIC | getTopicStep calls setSemanticLock — interleaving case required per spec |
| T2 write | DeleteTopicTest | success; getTopicStep acquires READ_TOPIC (interleaving) | — | Complete |
| T2 read | GetTopicByIdTest | success; topic not found (thrown SimulatorException) | — | Not-found correctly uses SimulatorException ✓ |
| T2 read | GetTopicsByCourseIdTest | returns all topics for course; returns empty list for unknown courseId | — | List-return: empty-list case is correct (not thrown exception) ✓ |
| T3 | — | N/A | N/A | No subscribed events → T3 not expected |

---

## Retro Cross-Reference

| Retro | Session | Priority | Action Item |
|-------|---------|----------|-------------|
| retro-2.3.a | 2.3.a | High | classify-and-plan: include factory, custom repo, owned entities in 2.N.a row (skill file — no source check needed) |
| retro-2.3.a | 2.3.a | Medium | session-a.md: add @OneToOne(mappedBy) pattern (skill file — no source check needed) |
| retro-2.3.b | 2.3.b | High | session-b.md: add note about service method + handler case when creating GetByIdCommand (skill file — no source check needed) |
| retro-2.3.b | 2.3.b | Medium | classify-and-plan: auto-include {Aggregate}Functionalities.java in 2.N.b (skill file — no source check needed) |
| retro-2.3.b | 2.3.b | Low | Resolve UpdateTopicEvent contradiction: add UpdateTopic functionality or remove event | ✅ Resolved per retro-2.3.b Resolution section: UpdateTopic, UpdateTopicFunctionalitySagas, UpdateTopicCommand, UpdateTopicTest all present |
| retro-2.3.c | 2.3.c | High | session-c.md: add CommandHandler update step (skill file — no source check needed) |
| retro-2.3.c | 2.3.c | High | session-c.md: add list-query custom repository pattern (skill file — no source check needed) |
| retro-2.3.c | 2.3.c | Medium | sagas.md: add list-return variant to Read Functionality Sagas section (doc file — no source check needed) |

No High-priority source-file-targeted items found across all retros. All source-targeted item (retro-2.3.b Low) is verified resolved.

---

## Build & Test Results

**Command:** `cd applications/quizzes-full && mvn clean -Ptest-sagas test -Dtest="TopicTest,CreateTopicTest,UpdateTopicTest,DeleteTopicTest,GetTopicByIdTest,GetTopicsByCourseIdTest"`
**Outcome:** BUILD SUCCESS

| Test class | Result | Failures |
|------------|--------|---------|
| TopicTest | PASS (1/1) | 0 |
| CreateTopicTest | PASS (2/2) | 0 |
| UpdateTopicTest | PASS (2/2) | 0 |
| DeleteTopicTest | PASS (2/2) | 0 |
| GetTopicByIdTest | PASS (2/2) | 0 |
| GetTopicsByCourseIdTest | PASS (2/2) | 0 |
| **Total** | **11/11** | **0** |

---

## Action Items

| Priority | Category | File | Finding | Fix |
|----------|---------|------|---------|-----|
| Major | Test coverage | `sagas/coordination/topic/UpdateTopicTest.groovy` | Missing step-interleaving scenario: `getTopicStep` acquires `READ_TOPIC` before `updateTopicStep` executes; no test verifies the lock state | Add test: pause saga after `getTopicStep` via `executeUntilStep("getTopicStep", uow)`, assert `sagaStateOf(topicDto.aggregateId) == TopicSagaState.READ_TOPIC`, then `resumeWorkflow` and verify update applied |
| Major | Test coverage | `sagas/coordination/topic/CreateTopicTest.groovy` | Missing step-interleaving scenario: `getCourseStep` acquires `CourseSagaState.READ_COURSE` on the Course before `createTopicStep` executes; no test verifies the Course lock state | Add test: pause saga after `getCourseStep` via `executeUntilStep("getCourseStep", uow)`, assert `sagaStateOf(courseDto.aggregateId) == CourseSagaState.READ_COURSE`, then `resumeWorkflow` and verify topic created |
| Minor | Annotation | `microservices/topic/messaging/TopicCommandHandler.java:16` | `@Service` used instead of `@Component`. The reference `TopicCommandHandler` is the only handler in quizzes that uses `@Service` (all 7 others use `@Component`) — it is a bug in the reference that was propagated here. All other quizzes-full handlers already use `@Component`. | Change to `@Component` |
| Major | Service pattern | `microservices/topic/service/TopicService.java:62-68` | `deleteTopic` uses copy-on-write (load → copy → `remove()` on copy → `registerChanged(copy)`). The correct pattern is in-place mutation: load → `remove()` on same instance → `registerChanged(same)`. `CourseService.deleteCourse` and `UserService.deleteUser` in quizzes-full both follow the in-place pattern correctly; Topic is the only outlier. Copied from a bug in the reference `TopicService`. | Remove `topicFactory.createTopicCopy(oldTopic)` call; call `oldTopic.remove()` and `registerChanged(oldTopic, unitOfWork)` directly |

**Note on `@Service` vs `@Component` in reference:** The quizzes reference `TopicCommandHandler` (line 12) uses `@Service` while all other seven command handlers in quizzes use `@Component`. This is a one-off inconsistency in the reference — not an intentional design choice. quizzes-full's Topic handler inherited the bug.

**Note on delete pattern:** The spec and the pattern established by Course and User in quizzes-full is in-place mutation for delete. The reference `TopicService` deviates from this (uses copy-on-write), but `CourseService` in the reference uses in-place. quizzes-full corrected User (in-place) but not Topic (still copy-on-write).
