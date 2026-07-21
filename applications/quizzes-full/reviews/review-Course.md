# Review — Course

**App:** quizzes-full
**Aggregate:** Course (aggregate #1 in plan.md)
**Date:** 2026-04-25
**Verdict:** Green

> **Green** = all structural checks pass, all planned operations implemented, tests present and correct, build passes.

---

## Summary

The Course aggregate is fully implemented across all three sessions (2.1.a–2.1.c). All four planned operations (CreateCourse, UpdateCourse, DeleteCourse, GetCourseById) are present in the service, command handler, functionalities coordinator, and saga classes. All rule classifications are correctly enforced: `COURSE_TYPE_FINAL` and `COURSE_NAME_FINAL` are Java `final` fields; `CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT` fires in `verifyInvariants()`; `UpdateCourse` correctly throws `COURSE_FIELDS_IMMUTABLE` because both fields are P1 final. The build passes with 8/8 tests. Minor deviations: T2 test classes are missing the `@Transactional` annotation that the testing.md template requires; the T1 test instantiates `SagaCourse` directly instead of calling `courseFunctionalities.createCourse(...)` (a residue from session-a, before functionalities existed); and `CourseService` injects the concrete `SagasCourseFactory` rather than an abstract factory interface (acceptable for sagas-only apps). All retro action items from all three sessions target doc/skill files, none target source files.

---

## File Inventory

| File (relative to microservices/course/) | In Reference | In Target | Status | Notes |
|------------------------------------------|-------------|-----------|--------|-------|
| `aggregate/Course.java` | Yes | Yes | OK | |
| `aggregate/CourseType.java` | Yes | Yes | OK | |
| `aggregate/CourseDto.java` | Yes | Yes | OK | |
| `aggregate/CourseRepository.java` | Yes | Yes | OK | |
| `aggregate/sagas/SagaCourse.java` | Yes | Yes | OK | |
| `aggregate/sagas/states/CourseSagaState.java` | Yes | Yes | OK | |
| `aggregate/sagas/factories/SagasCourseFactory.java` | Yes | Yes | OK | |
| `aggregate/sagas/repositories/CourseCustomRepositorySagas.java` | Yes | Yes | OK | |
| `service/CourseService.java` | Yes | Yes | OK | |
| `messaging/CourseCommandHandler.java` | Yes | Yes | OK | |
| `coordination/functionalities/CourseFunctionalities.java` | Yes | Yes | OK | |
| `coordination/sagas/CreateCourseFunctionalitySagas.java` | No | Yes | Extra | Justified: quizzes-full has first-class CreateCourse; reference creates Course indirectly via CourseExecution |
| `coordination/sagas/UpdateCourseFunctionalitySagas.java` | No | Yes | Extra | Justified: quizzes-full has UpdateCourse (even though it always throws due to P1 constraints) |
| `coordination/sagas/DeleteCourseFunctionalitySagas.java` | No | Yes | OK | Reference equivalent is a conceptual match |
| `coordination/sagas/GetCourseByIdFunctionalitySagas.java` | No | Yes | OK | Reference equivalent is a conceptual match |
| `aggregate/causal/CausalCourse.java` | Yes | No | Intentional | quizzes-full is sagas-only |
| `aggregate/causal/factories/CausalCourseFactory.java` | Yes | No | Intentional | quizzes-full is sagas-only |
| `aggregate/causal/repositories/CourseCustomRepositoryTCC.java` | Yes | No | Intentional | quizzes-full is sagas-only |
| `aggregate/CourseCustomRepository.java` (interface) | Yes | No | Intentional | No TCC variant; `CourseCustomRepositorySagas` is standalone |
| `aggregate/CourseFactory.java` (interface) | Yes | No | Intentional | No TCC variant; `SagasCourseFactory` is standalone |
| `CourseServiceApplication.java` | Yes | No | N/A | Not part of test harness; app has its own main class |
| `coordination/webapi/CourseController.java` | Yes | No | N/A | REST layer not tested via simulator test harness |

---

## Structural Review

### `aggregate/Course.java`

**`@Entity` annotation:** Present. Status: Correct.

**Extends `Aggregate`:** `public abstract class Course extends Aggregate`. Status: Correct.

**P1 final fields — COURSE_TYPE_FINAL and COURSE_NAME_FINAL:**
```java
private final CourseType type;
private final String name;
```
Status: Correct.

**`verifyInvariants()` — CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT:**
```java
if (executionCount == 0 && questionCount > 0) {
    throw new QuizzesFullException(QuizzesFullErrorMessage.CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT);
}
```
Status: Correct.

**`getEventSubscriptions()` returns empty set (no subscribed events):**
```java
return new HashSet<>();
```
Status: Correct.

**Copy constructor copies all fields:**
```java
public Course(Course other) {
    super(other);
    this.name = other.getName();
    this.type = other.getType();
    this.executionCount = other.getExecutionCount();
    this.questionCount = other.getQuestionCount();
}
```
Status: Correct.

**Default constructor null initialisation:** Sets `name = null; type = null`. Reference sets sentinel strings. Minor difference; acceptable for JPA, no semantic impact. Status: Minor deviation.

---

### `aggregate/sagas/SagaCourse.java`

**Extends `Course` and implements `SagaAggregate`:** Present. Status: Correct.

**`sagaState` field typed as `SagaState` (interface):**
```java
private SagaState sagaState;
```
Status: Correct.

**Default constructor initialises to `GenericSagaState.NOT_IN_SAGA`:** Present. Status: Correct.

**Copy constructor resets `sagaState = GenericSagaState.NOT_IN_SAGA`:**
```java
public SagaCourse(SagaCourse other) {
    super(other);
    this.sagaState = GenericSagaState.NOT_IN_SAGA;
}
```
quizzes-full resets to NOT_IN_SAGA (does not copy `other.getSagaState()`). Reference copies the state. Both are valid; quizzes-full uses reset-on-copy. Status: Correct (noted for documentation).

**Constructor signature difference:** Reference uses `CourseExecutionDto` param; target uses `String name, String type`. Intentional — quizzes-full has first-class Course construction. Status: Correct.

---

### `aggregate/sagas/states/CourseSagaState.java`

**Implements `SagaAggregate.SagaState`:** Present. Status: Correct.

**States:**
- `IN_UPDATE_COURSE` — for UpdateCourse saga locking an existing instance: Correct.
- `IN_DELETE_COURSE` — for DeleteCourse saga locking an existing instance: Correct.
- `READ_COURSE` — for downstream sagas (Topic, Execution, Question) that will lock Course as a prerequisite: Correct (anticipatory; reference includes it for the same reason).

Reference has only `READ_COURSE`. Target adds two additional IN_ states for the write operations that are first-class in quizzes-full. Status: Correct.

---

### `aggregate/sagas/factories/SagasCourseFactory.java`

**`@Service @Profile("sagas")`, no interface implemented:** Present. Status: Correct.

**Three methods — `createCourse(...)`, `createCourseCopy(SagaCourse)`, `createCourseDto(Course)`:** All present. Status: Correct.

---

### `aggregate/sagas/repositories/CourseCustomRepositorySagas.java`

**`@Service @Profile("sagas")`, concrete class:** Present. Status: Correct.

**`@Autowired CourseRepository courseRepository`:** Present. Status: Correct.

**Empty body beyond autowired field** (no custom cross-table lookups for Course): Present. Status: Correct.

---

### `service/CourseService.java`

**`@Service`, constructor-injected deps, factory `@Autowired`:** Present. Status: Correct.

**Every method has `@Transactional(isolation = Isolation.SERIALIZABLE)`:** All six methods annotated. Status: Correct.

**`createCourse` — ID generation, factory, `registerChanged`:**
```java
Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
SagaCourse course = sagasCourseFactory.createCourse(aggregateId, name, type);
unitOfWorkService.registerChanged(course, unitOfWork);
return sagasCourseFactory.createCourseDto(course);
```
Status: Correct.

**`updateCourse` — throws immediately (P1 contradiction):**
```java
throw new QuizzesFullException(QuizzesFullErrorMessage.COURSE_FIELDS_IMMUTABLE);
```
Expected per plan.md contradiction note. Status: Correct.

**`deleteCourse` — in-place mutation, no copy-on-write:**
```java
Course course = (Course) unitOfWorkService.aggregateLoadAndRegisterRead(courseAggregateId, unitOfWork);
course.remove();
unitOfWorkService.registerChanged(course, unitOfWork);
```
Status: Correct (delete exception to copy-on-write per docs/concepts/service.md).

**`decrementExecutionCount`, `decrementQuestionCount` — copy-on-write:** Both follow load → factory copy → mutate copy → `registerChanged`. Status: Correct.

**Factory injection type:** `@Autowired SagasCourseFactory sagasCourseFactory` (concrete class). Reference injects via `CourseFactory` interface. Since quizzes-full has no TCC variant, there is no interface to inject through. Status: Minor deviation (acceptable for sagas-only; not a bug).

**Missing increment methods:** `incrementExecutionCount` and `incrementQuestionCount` are absent. These will be added when Execution and Question sagas are implemented. No action needed now. Status: N/A for current sessions.

---

### `messaging/CourseCommandHandler.java`

**`@Component`, extends `CommandHandler`:** Present. Status: Correct.

**`getAggregateTypeName()` returns `"Course"` (PascalCase):** Present. Status: Correct.

**Switch covers all commands with default warning branch:**
```java
case GetCourseByIdCommand cmd -> ...
case CreateCourseCommand cmd -> ...
case UpdateCourseCommand cmd -> ...
case DeleteCourseCommand cmd -> ...
case DecrementExecutionCountCommand cmd -> ...
case DecrementQuestionCountCommand cmd -> ...
default -> { logger.warning(...); yield null; }
```
Status: Correct.

**Compensation warning (expected):** During UpdateCourse compensation, a base `Command` (not a typed subclass) is sent to release the semantic lock. The handler's default branch logs a warning — this is expected behavior matching the reference pattern. Status: Correct.

---

### `coordination/functionalities/CourseFunctionalities.java`

**`@Service`, one method per functionality:** Four methods present (`getCourseById`, `createCourse`, `updateCourse`, `deleteCourse`). Status: Correct.

**Each method derives functionality name, creates `SagaUnitOfWork`, instantiates saga `new`, calls `executeWorkflow`, returns DTO:**
```java
String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
CreateCourseFunctionalitySagas saga = new CreateCourseFunctionalitySagas(...);
saga.executeWorkflow(unitOfWork);
return saga.getCreatedCourseDto();
```
Status: Correct.

---

### `coordination/sagas/CreateCourseFunctionalitySagas.java`

**Extends `WorkflowFunctionality`, calls `buildWorkflow` in constructor:** Present. Status: Correct.

**`buildWorkflow` creates `new SagaWorkflow(this, unitOfWorkService, unitOfWork)`:** Present. Status: Correct.

**Single step (no lock — create-new pattern):** `createCourseStep` sends `CreateCourseCommand`, stores result in `createdCourseDto`. No `setSemanticLock`. Status: Correct.

---

### `coordination/sagas/UpdateCourseFunctionalitySagas.java`

**Two-step lock pattern:**
- Step 1 (`getCourseStep`): Wraps `GetCourseByIdCommand` in `SagaCommand`, calls `setSemanticLock(CourseSagaState.IN_UPDATE_COURSE)`. Compensation releases lock with `NOT_IN_SAGA`.
- Step 2 (`updateCourseStep`): Sends `UpdateCourseCommand` (which throws), declares step 1 as dependency.

Status: Correct.

---

### `coordination/sagas/DeleteCourseFunctionalitySagas.java`

**Two-step lock pattern:**
- Step 1 (`getCourseStep`): Wraps `GetCourseByIdCommand` in `SagaCommand`, calls `setSemanticLock(CourseSagaState.IN_DELETE_COURSE)`. Compensation releases lock.
- Step 2 (`deleteCourseStep`): Sends `DeleteCourseCommand`, declares step 1 as dependency.

Status: Correct.

---

### `coordination/sagas/GetCourseByIdFunctionalitySagas.java`

**Single step, no compensation, no lock:**
```java
SagaStep getCourseStep = new SagaStep("getCourseStep", () -> {
    GetCourseByIdCommand cmd = new GetCourseByIdCommand(...);
    this.courseDto = (CourseDto) commandGateway.send(cmd);
});
```
Result exposed via `getCourseDto()`. Status: Correct.

---

### Command files

All five commands (`CreateCourseCommand`, `UpdateCourseCommand`, `DeleteCourseCommand`, `GetCourseByIdCommand`, `DecrementExecutionCountCommand`, `DecrementQuestionCountCommand`) extend `Command`, call `super(unitOfWork, serviceName, aggregateId)`, pass `ServiceMapping.COURSE.getServiceName()` for `serviceName`, contain no business logic. Status: Correct.

---

### Test file structure

**`CourseTest.groovy`:**
- `@DataJpaTest`, `@Transactional`, `@Import(LocalBeanConfiguration)` present. Status: Correct.
- Inner `LocalBeanConfiguration extends BeanConfigurationSagas` present. Status: Correct.

**`CreateCourseTest.groovy`, `UpdateCourseTest.groovy`, `DeleteCourseTest.groovy`, `GetCourseByIdTest.groovy`:**
- `@DataJpaTest`, `@Import(LocalBeanConfiguration)` present.
- `@Transactional` missing. `@DataJpaTest` is itself `@Transactional` so tests function correctly, but the explicit annotation is absent per the testing.md template.
- Status: Minor deviation.

---

## Functionality Coverage

| Operation | In Service | Saga Class | CommandHandler Case | Functionalities Method | T2 Test | Notes |
|-----------|-----------|-----------|--------------------|-----------------------|---------|-------|
| CreateCourse | Yes | `CreateCourseFunctionalitySagas` | Yes (`CreateCourseCommand`) | `createCourse` | `CreateCourseTest` | |
| UpdateCourse | Yes (always throws) | `UpdateCourseFunctionalitySagas` | Yes (`UpdateCourseCommand`) | `updateCourse` | `UpdateCourseTest` | P1 contradiction; UpdateCourse intentionally always throws COURSE_FIELDS_IMMUTABLE |
| DeleteCourse | Yes | `DeleteCourseFunctionalitySagas` | Yes (`DeleteCourseCommand`) | `deleteCourse` | `DeleteCourseTest` | |
| GetCourseById | Yes | `GetCourseByIdFunctionalitySagas` | Yes (`GetCourseByIdCommand`) | `getCourseById` | `GetCourseByIdTest` | |

---

## Rule Enforcement

| Rule | Classification | Expected Impl | Actual Impl | Status |
|------|---------------|--------------|-------------|--------|
| COURSE_TYPE_FINAL | P1 final-field | `final CourseType type` in `Course.java` | `private final CourseType type` | Correct |
| COURSE_NAME_FINAL | P1 final-field | `final String name` in `Course.java` | `private final String name` | Correct |
| CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT | P1 intra-invariant | Condition in `Course.verifyInvariants()` throwing `QuizzesFullException` | `if (executionCount == 0 && questionCount > 0) throw new QuizzesFullException(...)` | Correct |

---

## Test Coverage

| Test Type | Class | Scenarios Present | Missing Scenarios | Notes |
|-----------|-------|------------------|-------------------|-------|
| T1 | `CourseTest` | "create course" (valid data); "create course — CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT" | None | Instantiates `SagaCourse` directly rather than via `courseFunctionalities.createCourse(...)`. Accepted: retro 2.1.a documents this as the correct T1 approach for session-a. No `verifyInvariants` call on valid-data test (invariant doesn't fire so the test is still correct). Minor style deviation. |
| T2 | `CreateCourseTest` | Happy path (TECNICO type); extra variant (EXTERNAL type) | None required | No P3 guards; no foreign aggregates so no step-interleaving needed (confirmed by retro 2.1.b) |
| T2 | `UpdateCourseTest` | COURSE_FIELDS_IMMUTABLE violation | No happy path (none possible) | UpdateCourse always throws; single test covers the only outcome |
| T2 | `DeleteCourseTest` | Happy path (success + SimulatorException on re-read) | None required | No P3 guards; no foreign aggregates so no step-interleaving needed |
| T2 | `GetCourseByIdTest` | Happy path; not-found (`thrown(SimulatorException)`) | None | `SimulatorException` (not `QuizzesFullException`) used for not-found: Correct |
| T3 | N/A — Course has no subscribed events | N/A | N/A | No `CourseInterInvariantTest` expected |

---

## Retro Cross-Reference

| Retro | Session | Priority | Action Item |
|-------|---------|----------|-------------|
| retro-2.1.a | 2.1.a | (no High-priority items targeting source files) | — |
| retro-2.1.b | 2.1.b | High | Add `{Aggregate}Functionalities.java` to session-b Produce section |
| retro-2.1.b | 2.1.b | High | Add prerequisite check for `Get{Aggregate}ByIdCommand` in session-b |
| retro-2.1.b | 2.1.b | High | Add SagaCommand subsection to `docs/concepts/sagas.md` |
| retro-2.1.b | 2.1.b | High | Fix `getAggregateTypeName()` claim in `docs/concepts/commands.md` |
| retro-2.1.b | 2.1.b | Medium | Add delete exception note to `docs/concepts/service.md` |
| retro-2.1.b | 2.1.b | Medium | Add `ServiceMapping.java` prerequisite check to session-b |
| retro-2.1.c | 2.1.c | High | Replace `@Bean FunctionalitySagas` template in session-c; add instruction to append read method to Functionalities coordinator |
| retro-2.1.c | 2.1.c | High | Add not-found subsection to `docs/concepts/testing.md` clarifying `SimulatorException` |
| retro-2.1.c | 2.1.c | Medium | Add "Read Functionality Sagas" subsection to `docs/concepts/sagas.md` |

---

## Build & Test Results

**Command:** `cd applications/quizzes-full && mvn clean -Ptest-sagas test -Dtest="CourseTest,CreateCourseTest,UpdateCourseTest,DeleteCourseTest,GetCourseByIdTest"`
**Outcome:** BUILD SUCCESS

| Test class | Result | Failures |
|------------|--------|---------|
| `CourseTest` | PASS (2 tests) | 0 |
| `CreateCourseTest` | PASS (2 tests) | 0 |
| `UpdateCourseTest` | PASS (1 test) | 0 |
| `DeleteCourseTest` | PASS (1 test) | 0 |
| `GetCourseByIdTest` | PASS (2 tests) | 0 |
| **Total** | **8 tests** | **0** |

---

## Action Items

| Priority | Category | File | Finding | Fix |
|----------|---------|------|---------|-----|
| Minor | Test structure | `CreateCourseTest.groovy`, `UpdateCourseTest.groovy`, `DeleteCourseTest.groovy`, `GetCourseByIdTest.groovy` | Missing `@Transactional` annotation at class level (template in `docs/concepts/testing.md` requires it) | Add `@org.springframework.transaction.annotation.Transactional` annotation to each T2 test class |
| Minor | Test approach | `sagas/course/CourseTest.groovy` | T1 valid-data test instantiates `SagaCourse` directly (`new SagaCourse(1, "Software Engineering", "TECNICO")`) instead of calling `courseFunctionalities.createCourse(...)` as the testing.md template requires; functionalities are now available | Rewrite the "create course" test to call `createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)` via the SpockTest helper |
| Minor | Service injection | `service/CourseService.java` | `sagasCourseFactory` is injected as the concrete `SagasCourseFactory` rather than an abstract factory interface; this couples the service to the sagas profile | Acceptable for sagas-only apps; no change required unless a profile-agnostic design is added |
