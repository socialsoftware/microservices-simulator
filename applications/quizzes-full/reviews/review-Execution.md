# Review — Execution

**App:** quizzes-full
**Aggregate:** Execution (aggregate #4 in plan.md)
**Date:** 2026-05-03
**Verdict:** Yellow

> **Green** = all structural checks pass, all planned operations implemented, tests present and correct, build passes.
> **Yellow** = minor deviations or missing test coverage; build passes.
> **Red** = missing operations, incorrect patterns, broken build, or multiple high-severity issues.

---

## Summary

All four sessions are complete. The implementation is structurally sound: every planned file is present, all nine operations are wired end-to-end, all P1/P2/P3 rules are enforced in the correct layer, and the build passes 30 tests across 11 classes. The verdict is Yellow due to two Major gaps in T1 test coverage (both `verifyInvariants()` intra-invariants lack dedicated T1 cases) and two Minor test-quality issues (a too-broad `thrown(Exception)` assertion in `EnrollStudentInExecutionTest` and a missing "ignores unrelated" T3 case for `AnonymizeStudentEvent`).

---

## File Inventory

| File (relative to microservices/execution/) | In Reference | In Target | Status | Notes |
|---------------------------------------------|-------------|-----------|--------|-------|
| `aggregate/Execution.java` | Yes (same name) | Yes | OK | |
| `aggregate/ExecutionCourse.java` | Yes (CourseExecutionCourse) | Yes | OK | Renamed to match quizzes-full domain |
| `aggregate/ExecutionStudent.java` | Yes (CourseExecutionStudent) | Yes | OK | Renamed |
| `aggregate/ExecutionFactory.java` | Yes (CourseExecutionFactory interface) | Yes | OK | |
| `aggregate/ExecutionCustomRepository.java` | Yes | Yes | OK | |
| `aggregate/ExecutionDto.java` | Yes (CourseExecutionDto) | Yes | OK | Renamed |
| `aggregate/ExecutionStudentDto.java` | No (not in reference) | Yes | Extra | Justified — needed for getStudentByExecutionIdAndUserId P4a read |
| `aggregate/ExecutionRepository.java` | Yes (CourseExecutionRepository) | Yes | OK | |
| `aggregate/sagas/SagaExecution.java` | Yes | Yes | OK | |
| `aggregate/sagas/states/ExecutionSagaState.java` | Yes (CourseExecutionSagaState) | Yes | OK | Renamed |
| `aggregate/sagas/factories/SagasExecutionFactory.java` | Yes (SagasCourseExecutionFactory) | Yes | OK | Renamed |
| `aggregate/sagas/repositories/ExecutionCustomRepositorySagas.java` | Yes (CourseExecutionCustomRepositorySagas) | Yes | OK | Renamed |
| `aggregate/causal/` subtree | Yes (reference only) | No | Intentional | quizzes-full is sagas-only |
| `service/ExecutionService.java` | Yes | Yes | OK | |
| `messaging/ExecutionCommandHandler.java` | Yes | Yes | OK | |
| `coordination/functionalities/ExecutionFunctionalities.java` | Yes | Yes | OK | |
| `coordination/webapi/ExecutionController.java` | Yes | Yes | OK | |
| `coordination/sagas/CreateExecutionFunctionalitySagas.java` | Yes (CreateCourseExecutionFunctionalitySagas) | Yes | OK | |
| `coordination/sagas/UpdateExecutionFunctionalitySagas.java` | No direct ref | Yes | OK | New operation in quizzes-full domain |
| `coordination/sagas/DeleteExecutionFunctionalitySagas.java` | Yes (RemoveCourseExecutionFunctionalitySagas) | Yes | OK | |
| `coordination/sagas/EnrollStudentInExecutionFunctionalitySagas.java` | Yes (AddStudentFunctionalitySagas) | Yes | OK | |
| `coordination/sagas/DisenrollStudentFunctionalitySagas.java` | Yes (RemoveStudentFromCourseExecutionFunctionalitySagas) | Yes | OK | |
| `coordination/sagas/UpdateStudentNameFunctionalitySagas.java` | Yes | Yes | OK | |
| `coordination/sagas/AnonymizeStudentFunctionalitySagas.java` | Yes | Yes | OK | |
| `coordination/sagas/GetExecutionByIdFunctionalitySagas.java` | Yes (GetCourseExecutionByIdFunctionalitySagas) | Yes | OK | |
| `coordination/sagas/GetStudentByExecutionIdAndUserIdFunctionalitySagas.java` | Yes | Yes | OK | |
| `coordination/eventProcessing/ExecutionEventProcessing.java` | Yes | Yes | OK | |
| `coordination/causal/` subtree | Yes (reference only) | No | Intentional | |
| `notification/subscribe/ExecutionSubscribesDeleteUser.java` | Yes (CourseExecutionSubscribesRemoveUser) | Yes | OK | |
| `notification/subscribe/ExecutionSubscribesUpdateStudentName.java` | No (not in reference) | Yes | Extra | Justified — quizzes-full subscribes to UpdateStudentNameEvent for cached name sync |
| `notification/subscribe/ExecutionSubscribesAnonymizeStudent.java` | No (not in reference) | Yes | Extra | Justified — quizzes-full subscribes to AnonymizeStudentEvent |
| `notification/handling/ExecutionEventHandling.java` | Yes (CourseExecutionEventHandling) | Yes | OK | |
| `notification/handling/handlers/ExecutionEventHandler.java` | Yes (CourseExecutionEventHandler) | Yes | OK | Single-dispatcher pattern instead of per-event-type subclasses |
| `ExecutionServiceApplication.java` | Yes | Yes | OK | |
| `commands/execution/` (9 command files) | Yes (with different names) | Yes | OK | |

---

## Structural Review

### `aggregate/Execution.java`

**Expected:** extends `Aggregate`; `@Entity`; intra-invariants in `verifyInvariants()` throwing `QuizzesFullException`; `getEventSubscriptions()` returns subscriptions per enrolled student; copy constructor copies all fields including `ExecutionCourse` and `ExecutionStudent` set.

**Actual:** Correct. `@Entity` ✓; extends `Aggregate` ✓; `verifyInvariants()` checks REMOVE_NO_STUDENTS and STUDENT_ALREADY_ENROLLED, both throw `QuizzesFullException` ✓; `getEventSubscriptions()` iterates `students` and adds three subscriptions per student (DeleteUser, UpdateStudentName, AnonymizeStudent) ✓; copy constructor deep-copies `ExecutionCourse` and each `ExecutionStudent` ✓.

**Status:** Correct

---

### `aggregate/sagas/SagaExecution.java`

**Expected:** extends `Execution` implements `SagaAggregate`; `sagaState` typed as `SagaState` interface; default and copy constructors set `NOT_IN_SAGA`.

**Actual:**
```java
private SagaState sagaState;
public SagaExecution() { super(); this.sagaState = GenericSagaState.NOT_IN_SAGA; }
public SagaExecution(SagaExecution other) { super(other); this.sagaState = GenericSagaState.NOT_IN_SAGA; }
```
All correct. Copy constructor resets to `NOT_IN_SAGA` (not copying `other.getSagaState()`). ✓

**Status:** Correct

---

### `aggregate/sagas/states/ExecutionSagaState.java`

**Expected:** `implements SagaAggregate.SagaState`; `IN_` states for all write ops on existing instances (UpdateExecution, DeleteExecution, EnrollStudent, DisenrollStudent, UpdateStudentName, AnonymizeStudent); `READ_EXECUTION` for downstream sagas that lock Execution as a data-assembly step; no IN_ state for CreateExecution (creates new, no existing lock needed).

**Actual:** Has `READ_EXECUTION`, `IN_UPDATE_EXECUTION`, `IN_DELETE_EXECUTION`, `IN_ENROLL_STUDENT`, `IN_DISENROLL_STUDENT`, `IN_UPDATE_STUDENT_NAME`, `IN_ANONYMIZE_STUDENT`. CreateExecution has no IN_ state in this enum ✓.

**Status:** Correct

---

### `aggregate/sagas/factories/SagasExecutionFactory.java`

**Expected:** `@Service @Profile("sagas")` implements `ExecutionFactory`; three methods: `createExecution`, `createExecutionCopy`, `createExecutionDto`.

**Actual:** Matches exactly. ✓

**Status:** Correct

---

### `aggregate/sagas/repositories/ExecutionCustomRepositorySagas.java`

**Expected:** `@Service @Profile("sagas")` concrete class; `@Autowired ExecutionRepository`; implements custom lookup.

**Actual:** Correct. Implements `findExecutionIdsOfAllNonDeleted()` filtering non-deleted aggregates. ✓

**Status:** Correct

---

### `service/ExecutionService.java`

**Expected:** `@Service`; constructor injection for `UnitOfWorkService` and `ExecutionCustomRepository`; `@Autowired` factory; `@Transactional(SERIALIZABLE)` on all methods; create via `aggregateIdGeneratorService` + factory + `registerChanged`; mutate via load + copy + mutate copy + `registerChanged` on copy; delete using copy-on-write (`createExecutionCopy` then `remove()`); P3 guards in service body.

**Actual:** All patterns correct. Notable:
- `deleteExecution` uses copy-on-write (not in-place mutation) to avoid JPA auto-flush during saga abort — this is the correct documented fix. ✓
- P3 INACTIVE_USER guard at line 89: `if (!userDto.isActive()) throw new QuizzesFullException(INACTIVE_USER, ...)` ✓
- P3 NO_DUPLICATE_COURSE_EXECUTION guard at lines 52–56: iterates all non-deleted executions ✓
- `getStudentByExecutionIdAndUserId` returns `null` via `orElse(null)` when student not enrolled — plan.md describes the command as throwing `COURSE_EXECUTION_STUDENT_NOT_FOUND`, but actual behavior returns null. This is a Minor inconsistency; the null-return is documented by a passing test and is harmless if the Tournament saga handles null explicitly (to be verified when Tournament is implemented).

**Status:** Correct (Minor noted)

---

### `messaging/ExecutionCommandHandler.java`

**Expected:** `@Component` extends `CommandHandler`; `getAggregateTypeName()` returns `"Execution"`; switch covers all 9 commands; default branch logs warning.

**Actual:** All 9 command types covered (GetExecutionById, GetStudentByExecutionIdAndUserId, CreateExecution, UpdateExecution, DeleteExecution, EnrollStudentInExecution, DisenrollStudentFromExecution, UpdateStudentNameInExecution, AnonymizeStudentInExecution). Default branch logs warning. ✓

**Status:** Correct

---

### `coordination/functionalities/ExecutionFunctionalities.java`

**Expected:** `@Service`; one method per functionality; each creates `SagaUnitOfWork`; instantiates saga inline with `new`; calls `executeWorkflow`; returns result from saga getter.

**Actual:** Correct for all 9 standard saga functionalities. Three additional "ByEvent" methods (`removeStudentFromExecutionByEvent`, `updateStudentNameByEvent`, `anonymizeStudentByEvent`) call the service directly and call `unitOfWorkService.commit()` — correct non-saga pattern for event-driven updates (avoids circular saga loops). ✓

**Status:** Correct

---

### `coordination/sagas/*.java` — Saga step patterns

**CreateExecutionFunctionalitySagas:** 3 steps: (1) `GetCourseByIdCommand` wrapped in `SagaCommand` + `setSemanticLock(CourseSagaState.READ_COURSE)` with compensation; (2) `CreateExecutionCommand` (no lock on Execution — correct for create); (3) `IncrementExecutionCountCommand`. ✓

**UpdateExecutionFunctionalitySagas:** 2 steps: (1) `GetExecutionByIdCommand` + `setSemanticLock(IN_UPDATE_EXECUTION)` with compensation; (2) `UpdateExecutionCommand`. ✓

**DeleteExecutionFunctionalitySagas:** 3 steps: (1) `GetExecutionByIdCommand` + `setSemanticLock(IN_DELETE_EXECUTION)` with compensation; (2) `DeleteExecutionCommand`; (3) `DecrementExecutionCountCommand`. ✓

**EnrollStudentInExecutionFunctionalitySagas:** 3 steps: (1) `GetUserByIdCommand` (no lock — data assembly); (2) `GetExecutionByIdCommand` + `setSemanticLock(IN_ENROLL_STUDENT)` with compensation, depends on step 1; (3) `EnrollStudentInExecutionCommand`, depends on step 2. ✓

**DisenrollStudentFunctionalitySagas:** 2 steps: (1) lock + compensation; (2) disenroll command. ✓

**UpdateStudentNameFunctionalitySagas:** 3 steps: (1) lock Execution; (2) `UpdateStudentNameInExecutionCommand`; (3) `UpdateUserNameCommand` (publishes `UpdateStudentNameEvent`). ✓

**AnonymizeStudentFunctionalitySagas:** 3 steps: (1) lock Execution; (2) `AnonymizeStudentInExecutionCommand`; (3) `AnonymizeUserCommand`. ✓

**GetExecutionByIdFunctionalitySagas / GetStudentByExecutionIdAndUserIdFunctionalitySagas:** single-step read sagas, no lock. ✓

**Status:** All Correct

---

### Event wiring (session 2.4.d)

**`ExecutionSubscribesDeleteUser/UpdateStudentName/AnonymizeStudent.java`:** All use `EventSubscription` constructor with `(student.getUserAggregateId(), 0L, EventClass.class.getSimpleName())`. Use of `0L` as `subscribedVersion` is intentional (ExecutionStudent has no version field tracking the publisher's version). ✓

**`ExecutionEventHandling.java`:** `@Component`; three `@Scheduled` methods dispatching to `ExecutionEventHandler`. ✓

**`ExecutionEventHandler.java`:** `@Component extends EventHandler`; single concrete class with `instanceof` dispatch for all three event types; constructor takes `ExecutionRepository`. ✓

**`ExecutionEventProcessing.java`:** `@Service`; delegates to `executionFunctionalities.removeStudentFromExecutionByEvent / updateStudentNameByEvent / anonymizeStudentByEvent` — uses the non-saga "ByEvent" methods, not the standard saga functionalities, avoiding circular loops. ✓

**Status:** Correct

---

## Functionality Coverage

| Operation | In Service | Saga Class | CommandHandler Case | Functionalities Method | T2 Test | Notes |
|-----------|-----------|-----------|--------------------|-----------------------|---------|-------|
| CreateExecution | Yes | CreateExecutionFunctionalitySagas | Yes | Yes | Yes (4 cases) | |
| UpdateExecution | Yes | UpdateExecutionFunctionalitySagas | Yes | Yes | Yes (2 cases) | |
| DeleteExecution | Yes | DeleteExecutionFunctionalitySagas | Yes | Yes | Yes (3 cases) | |
| EnrollStudentInExecution | Yes | EnrollStudentInExecutionFunctionalitySagas | Yes | Yes | Yes (4 cases) | INACTIVE_USER test uses `thrown(Exception)` — Minor |
| DisenrollStudent | Yes | DisenrollStudentFunctionalitySagas | Yes | Yes | Yes (2 cases) | |
| UpdateStudentName | Yes | UpdateStudentNameFunctionalitySagas | Yes | Yes | Yes (2 cases) | |
| AnonymizeStudent | Yes | AnonymizeStudentFunctionalitySagas | Yes | Yes | Yes (2 cases) | |
| GetExecutionById | Yes | GetExecutionByIdFunctionalitySagas | Yes | Yes | Yes (2 cases) | |
| GetStudentByExecutionIdAndUserId | Yes | GetStudentByExecutionIdAndUserIdFunctionalitySagas | Yes | Yes | Yes (3 cases) | Returns null for not-enrolled (not throw) |

---

## Rule Enforcement

| Rule | Classification | Expected Implementation | Actual Implementation | Status |
|------|---------------|------------------------|----------------------|--------|
| REMOVE_NO_STUDENTS | P1 intra-invariant | `verifyInvariants()` throws `QuizzesFullException` when `state==DELETED && !students.isEmpty()` | `removeNoStudents()` called in `verifyInvariants()`, throws `REMOVE_NO_STUDENTS` | Correct |
| STUDENT_ALREADY_ENROLLED | P1 intra-invariant | `verifyInvariants()` throws `QuizzesFullException` when duplicate userId in students set | `studentAlreadyEnrolled()` called in `verifyInvariants()`, throws `STUDENT_ALREADY_ENROLLED` | Correct |
| NO_DUPLICATE_COURSE_EXECUTION | P3 | Guard in `ExecutionService.createExecution` checking (acronym, academicTerm) uniqueness | `for` loop over all non-deleted IDs, loads and compares; throws `NO_DUPLICATE_COURSE_EXECUTION` | Correct |
| INACTIVE_USER | P3 | Guard in `ExecutionService.enrollStudentInExecution` after saga fetches `UserDto`; check `userDto.active == true` | `if (!userDto.isActive()) throw new QuizzesFullException(INACTIVE_USER, ...)` at line 89 | Correct |
| USER_EXISTS (DeleteUser) | P2 inter-invariant | `getEventSubscriptions()` returns `ExecutionSubscribesDeleteUser` per student; event handler removes student | `ExecutionSubscribesDeleteUser` per student in `getEventSubscriptions()` ✓; `ExecutionEventProcessing.processDeleteUserEvent` → `removeStudentFromExecutionByEvent` ✓ | Correct |
| USER_EXISTS (UpdateStudentName) | P2 inter-invariant | `getEventSubscriptions()` returns `ExecutionSubscribesUpdateStudentName`; event updates cached name | Both wired correctly via non-saga "ByEvent" method | Correct |
| USER_EXISTS (AnonymizeStudent) | P2 inter-invariant | `getEventSubscriptions()` returns `ExecutionSubscribesAnonymizeStudent`; event anonymizes cached data | Both wired correctly via non-saga "ByEvent" method | Correct |

---

## Test Coverage

| Test Type | Class | Scenarios Present | Missing Scenarios | Notes |
|-----------|-------|------------------|-------------------|-------|
| T1 | `ExecutionTest` | "create execution" (1 case) | REMOVE_NO_STUDENTS invariant violation case; STUDENT_ALREADY_ENROLLED invariant violation case | Both intra-invariants in `verifyInvariants()` lack T1 test cases — **Major** |
| T2-write | `CreateExecutionTest` | success; NO_DUPLICATE_COURSE_EXECUTION violation; different acronym same term is allowed (extra positive); getCourseStep lock interleaving | — | Complete |
| T2-write | `UpdateExecutionTest` | success; getExecutionStep lock interleaving | — | No P3 guards to test |
| T2-write | `DeleteExecutionTest` | success; REMOVE_NO_STUDENTS violation; getExecutionStep lock interleaving | — | Complete |
| T2-write | `EnrollStudentInExecutionTest` | success; STUDENT_ALREADY_ENROLLED violation; INACTIVE_USER violation; getExecutionStep lock interleaving | — | INACTIVE_USER case uses `thrown(Exception)` (line 70) instead of `thrown(QuizzesFullException)` — **Minor** |
| T2-write | `DisenrollStudentTest` | success; getExecutionStep lock interleaving | — | No guards; disenroll is no-op for non-enrolled student |
| T2-write | `UpdateStudentNameTest` | success; getExecutionStep lock interleaving | — | No guards |
| T2-write | `AnonymizeStudentTest` | success; getExecutionStep lock interleaving | — | No guards |
| T2-read | `GetExecutionByIdTest` | success; not-found throws `SimulatorException` ✓ | — | Correct exception type |
| T2-read | `GetStudentByExecutionIdAndUserIdTest` | success; execution not found throws `SimulatorException` ✓; student not enrolled returns null | — | Correct |
| T3 | `ExecutionInterInvariantTest` | DeleteUser: reflects ✓, ignores unrelated ✓; UpdateStudentName: reflects ✓, ignores unrelated ✓; AnonymizeStudent: reflects ✓ | AnonymizeStudent "ignores unrelated entity" case | — **Minor** |

---

## Retro Cross-Reference

| Retro | Session | Priority | Action Item |
|-------|---------|----------|-------------|
| retro-2.4.a | 2.4.a | Medium | `.claude/skills/classify-and-plan`: include owned entity classes and factory/repository interfaces in 2.N.a rows when aggregate has snapshots |
| retro-2.4.b | 2.4.b | High | `session-b.md` §Event classes: document that events must extend `Event` (JPA entity), not implement `DomainEvent` |
| retro-2.4.b | 2.4.b | High | `docs/concepts/service.md`: add copy-on-write note for aggregates whose `verifyInvariants()` checks `state==DELETED` |
| retro-2.4.b | 2.4.b | Medium | `session-b.md`: add `ExecutionFunctionalities.java` explicitly to plan.md file table for write-functionality sessions |
| retro-2.4.c | 2.4.c | Medium | `session-c.md`: add note about replacing direct-service stubs in functionalities with saga-based implementations |
| retro-2.4.d | 2.4.d | High | `docs/concepts/events.md`: fix EventSubscription canonical snippet — use `EventClass.class.getSimpleName()` for eventType arg |
| retro-2.4.d | 2.4.d | High | `session-d.md`: add guidance on "ByEvent" non-saga Functionalities methods for events that mirror saga operations |
| retro-2.4.d | 2.4.d | Medium | `docs/concepts/events.md`: add note on `subscribedVersion = 0L` when subscriber entity has no version tracking |
| retro-2.4.d | 2.4.d | Medium | `session-d.md`: fix BeanConfig template for EventHandler constructor to pass repository arg |
| retro-2.4.d | 2.4.d | Low | `session-d.md`: clarify that `{Aggregate}EventHandler.java` is a single concrete dispatcher, not abstract base + subclasses |

All action items target skill or documentation files — no source file fixes required. The one source-level fix mentioned in retro-2.4.b (copy-on-write for `deleteExecution`) is verified as present in `ExecutionService.java` (lines 78–83).

---

## Build & Test Results

**Command:** `mvn -Ptest-sagas test -Dtest="ExecutionTest,ExecutionInterInvariantTest,CreateExecutionTest,UpdateExecutionTest,DeleteExecutionTest,EnrollStudentInExecutionTest,DisenrollStudentTest,UpdateStudentNameTest,AnonymizeStudentTest,GetExecutionByIdTest,GetStudentByExecutionIdAndUserIdTest"`
**Outcome:** BUILD SUCCESS

| Test class | Result | Failures |
|------------|--------|---------|
| ExecutionTest | PASS (1) | 0 |
| ExecutionInterInvariantTest | PASS (5) | 0 |
| CreateExecutionTest | PASS (4) | 0 |
| UpdateExecutionTest | PASS (2) | 0 |
| DeleteExecutionTest | PASS (3) | 0 |
| EnrollStudentInExecutionTest | PASS (4) | 0 |
| DisenrollStudentTest | PASS (2) | 0 |
| UpdateStudentNameTest | PASS (2) | 0 |
| AnonymizeStudentTest | PASS (2) | 0 |
| GetExecutionByIdTest | PASS (2) | 0 |
| GetStudentByExecutionIdAndUserIdTest | PASS (3) | 0 |

**Total: 30 tests, 0 failures, 0 errors.**

---

## Action Items

| Priority | Category | File | Finding | Fix |
|----------|---------|------|---------|-----|
| Major | Test | `sagas/execution/ExecutionTest.groovy` | No T1 test for REMOVE_NO_STUDENTS intra-invariant | Add test: create `SagaExecution`, add a student, call `execution.remove()` to set state to DELETED, call `execution.verifyInvariants()`, expect `thrown(QuizzesFullException)` |
| Major | Test | `sagas/execution/ExecutionTest.groovy` | No T1 test for STUDENT_ALREADY_ENROLLED intra-invariant | Add test: create `SagaExecution`, add two `ExecutionStudent` instances with the same `userAggregateId`, call `execution.verifyInvariants()`, expect `thrown(QuizzesFullException)` |
| Minor | Test | `sagas/coordination/execution/EnrollStudentInExecutionTest.groovy:70` | INACTIVE_USER guard test uses `thrown(Exception)` instead of a specific type | Replace `thrown(Exception)` with `thrown(QuizzesFullException)` — the P3 guard in `ExecutionService.enrollStudentInExecution` throws `QuizzesFullException(INACTIVE_USER)` |
| Minor | Test | `sagas/execution/ExecutionInterInvariantTest.groovy` | Missing T3 "ignores unrelated entity" case for `AnonymizeStudentEvent` | Add test mirroring the existing `"ignores UpdateStudentNameEvent for unrelated user"` case: enroll user1, anonymize user2 directly, poll `handleAnonymizeStudentEvents()`, verify user1's cached data is unchanged |
