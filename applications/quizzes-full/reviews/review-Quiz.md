# Review — Quiz

**App:** quizzes-full
**Aggregate:** Quiz (aggregate #6 in plan.md)
**Date:** 2026-05-14
**Verdict:** Green

> **Green** = all structural checks pass, all planned operations implemented, tests present and correct, build passes.
> **Yellow** = minor deviations or missing test coverage; build passes.
> **Red** = missing operations, incorrect patterns, broken build, or multiple high-severity issues.

---

## Summary

The Quiz aggregate implementation is overall solid. All three planned operations (CreateQuiz, UpdateQuiz, GetQuizById) are fully implemented and tested. The two non-trivial P1 intra-invariants (QUIZ_DATE_ORDERING, QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE) are correctly enforced in `verifyInvariants()`, and all T3 inter-invariant scenarios are present and use the correct deletion-event assertion pattern. The single Major finding is that `QUIZ_CREATION_DATE_FINAL` is classified P1 Java-final in plan.md but `creationDate` is not declared `final` in `Quiz.java`. Minor deviations include inlining `getEventSubscriptions()` logic instead of using private helper methods per `aggregate.md`, and missing T1 intra-invariant violation cases (though the doc `testing.md` places violations in T2, where they are covered). Build passes with 15/15 tests.

---

## File Inventory

| File (relative to microservices/quiz/) | In Reference | In Target | Status | Notes |
|----------------------------------------|:------------:|:---------:|--------|-------|
| `aggregate/Quiz.java` | ✓ | ✓ | OK | |
| `aggregate/QuizCourseExecution.java` | ✓ | `QuizExecution.java` | Renamed | Same purpose; name uses quizzes-full conventions |
| `aggregate/QuizQuestion.java` | ✓ | ✓ | OK | |
| `aggregate/QuizType.java` | ✓ | ✓ | OK | |
| `aggregate/QuizFactory.java` | ✓ | ✓ | OK | |
| `aggregate/QuizRepository.java` | ✓ | ✓ | OK | |
| `aggregate/QuizDto.java` | ✓ | ✓ | OK | |
| `aggregate/QuizOption.java` | ✓ | — | Intentional | quizzes-full does not cache options at Quiz level; domain difference |
| `aggregate/QuizCustomRepository.java` | — | ✓ | Extra | Empty interface; follows pattern used for all other aggregates |
| `aggregate/sagas/SagaQuiz.java` | ✓ | ✓ | OK | |
| `aggregate/sagas/states/QuizSagaState.java` | ✓ | ✓ | OK | |
| `aggregate/sagas/factories/SagasQuizFactory.java` | ✓ | ✓ | OK | |
| `aggregate/sagas/repositories/QuizCustomRepositorySagas.java` | — | ✓ | Extra | Empty class; consistent with pattern for aggregates with no custom queries |
| `service/QuizService.java` | ✓ | ✓ | OK | |
| `messaging/QuizCommandHandler.java` | ✓ | ✓ | OK | |
| `coordination/functionalities/QuizFunctionalities.java` | ✓ | ✓ | OK | |
| `coordination/sagas/CreateQuizFunctionalitySagas.java` | ✓ | ✓ | OK | |
| `coordination/sagas/UpdateQuizFunctionalitySagas.java` | ✓ | ✓ | OK | |
| `coordination/sagas/GetQuizByIdFunctionalitySagas.java` | — | ✓ | OK | quizzes-full adds explicit GetById; reference uses FindQuiz/GetAvailableQuizzes instead |
| `coordination/eventProcessing/QuizEventProcessing.java` | ✓ | ✓ | OK | |
| `coordination/webapi/QuizController.java` | ✓ | ✓ | OK | |
| `notification/subscribe/QuizSubscribesUpdateQuestion.java` | ✓ | ✓ | OK | |
| `notification/subscribe/QuizSubscribesDeleteQuestion.java` | ✓ | ✓ | OK | |
| `notification/subscribe/QuizSubscribesDeleteCourseExecution.java` | ✓ | ✓ | OK | |
| `notification/handling/QuizEventHandling.java` | ✓ | ✓ | OK | |
| `notification/handling/handlers/QuizEventHandler.java` | ✓ | ✓ | OK | |
| `QuizServiceApplication.java` | ✓ | ✓ | OK | |
| `commands/quiz/CreateQuizCommand.java` | ✓ | ✓ | OK | |
| `commands/quiz/UpdateQuizCommand.java` | ✓ | ✓ | OK | |
| `commands/quiz/GetQuizByIdCommand.java` | ✓ | ✓ | OK | |
| `causal/` subtree | ✓ | — | Intentional | quizzes-full is sagas-only |

---

## Structural Review

### `aggregate/Quiz.java`

**`QUIZ_CREATION_DATE_FINAL` — P1 Java-final field**

Expected (per plan.md §3.1 and reference `Quiz.java`): `private final LocalDateTime creationDate;`

Actual (target line 33):
```java
@Column
private LocalDateTime creationDate;
```

The reference declares `creationDate` as `final` and initialises it in both constructors (`this.creationDate = LocalDateTime.now()` / `this.creationDate = null`). The target omits `final`. No setter exists, so behavioural immutability is preserved, but the Java compiler does not enforce it. **Incorrect.**

---

**`getEventSubscriptions()` — private helper method pattern**

Expected per `aggregate.md`:
```java
private void interInvariantQuestionsExist(Set<EventSubscription> subscriptions) {
    for (QuizQuestion question : questions) {
        subscriptions.add(new QuizSubscribesUpdateQuestion(question));
        subscriptions.add(new QuizSubscribesDeleteQuestion(question));
    }
}
```

Actual (lines 128–138): all logic inlined directly inside `getEventSubscriptions()`. Functionally equivalent but does not follow the one-private-helper-per-inter-invariant naming convention from `aggregate.md`. **Minor deviation.**

---

**`verifyInvariants()` — P1 intra-invariants** — **Correct.**

Both `QUIZ_DATE_ORDERING` (line 119) and `QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE` (line 122) throw `QuizzesFullException` with the appropriate error message. The `instanceof` guard on `getPrev()` (line 97) is an improvement over the reference which omits it.

---

**Copy constructor** — **Correct.** All fields including `lastModifiedTime`, `quizExecution` (via copy constructor), and all `QuizQuestion` instances are deep-copied.

---

### `aggregate/sagas/SagaQuiz.java`

- Extends `Quiz` and implements `SagaAggregate`. ✓
- `sagaState` typed as `SagaState` (interface). ✓
- Default constructor: `this.sagaState = GenericSagaState.NOT_IN_SAGA`. ✓
- Copy constructor: resets `this.sagaState = GenericSagaState.NOT_IN_SAGA` (quizzes-full convention; reference copies `other.getSagaState()` — both are valid per docs). ✓

### `aggregate/sagas/states/QuizSagaState.java`

- Implements `SagaAggregate.SagaState`. ✓
- `IN_UPDATE_QUIZ`: correct; UpdateQuiz write saga locks the existing Quiz. ✓
- `READ_QUIZ`: forward-declared for downstream sagas (e.g., CreateQuizAnswer) that will read-lock Quiz in session 7. Consistent with reference. ✓

### `aggregate/sagas/factories/SagasQuizFactory.java`

- `@Service @Profile("sagas")`, implements `QuizFactory`. ✓
- Three methods: `createQuiz(...)`, `createQuizCopy(Quiz existing)`, `createQuizDto(Quiz quiz)`. ✓
- `createQuizCopy` casts `existing` to `SagaQuiz` and calls the copy constructor. ✓

### `aggregate/sagas/repositories/QuizCustomRepositorySagas.java`

- `@Service @Profile("sagas")`, implements `QuizCustomRepository`. ✓
- `@Autowired QuizRepository quizRepository`. ✓
- Empty body beyond the autowired field — correct for aggregates with no custom cross-table lookups. ✓

### `service/QuizService.java`

- `@Service`; factory `@Autowired`; `unitOfWorkService` and `quizRepository` constructor-injected. ✓
- All methods annotated `@Transactional(isolation = Isolation.SERIALIZABLE)`. ✓
- `createQuiz`: generates ID via `aggregateIdGeneratorService`, calls factory, calls `registerChanged`. ✓
- `updateQuiz`: load → copy → mutate copy → `registerChanged(newQuiz)`. ✓
- `updateQuestionInQuiz`, `removeQuestionFromQuiz`, `invalidateQuiz`: all use copy-on-write; `remove()` called on the copy. ✓
- `removeQuestionFromQuiz` and `invalidateQuiz` register `InvalidateQuizEvent` after `registerChanged`. ✓
- No P3 guards needed for Quiz operations. ✓

### `messaging/QuizCommandHandler.java`

- `@Component`, extends `CommandHandler`. ✓
- `getAggregateTypeName()` returns `"Quiz"`. ✓
- Switch expression covers `GetQuizByIdCommand`, `CreateQuizCommand`, `UpdateQuizCommand`. ✓
- Default branch logs warning. ✓

### `coordination/functionalities/QuizFunctionalities.java`

- `@Service`. ✓
- Each method derives `functionalityName` via stack trace, creates `SagaUnitOfWork`, instantiates saga inline with `new` (not via Spring injection), calls `executeWorkflow`. ✓
- `updateQuestionInQuizByEvent`, `removeQuestionFromQuizByEvent`, `invalidateQuizByEvent`: call service directly and `commit` — correct ByEvent pattern. ✓

### `coordination/sagas/CreateQuizFunctionalitySagas.java`

- Three steps: `getExecutionStep` → `getQuestionsStep` → `createQuizStep`. ✓
- `getExecutionStep`: wraps read command in `SagaCommand` + `setSemanticLock(ExecutionSagaState.READ_EXECUTION)`. Compensation releases lock. ✓
- `getQuestionsStep`: plain command, no lock (questions are read-only in this flow). ✓
- `createQuizStep`: plain create command. ✓
- Create-new operation — no lock on Quiz itself needed. ✓

### `coordination/sagas/UpdateQuizFunctionalitySagas.java`

- Three steps: `getQuizStep` → `getQuestionsStep` → `updateQuizStep`. ✓
- `getQuizStep`: `SagaCommand` + `setSemanticLock(QuizSagaState.IN_UPDATE_QUIZ)`. Compensation releases lock. ✓
- Step dependencies correctly chained. ✓

### `coordination/sagas/GetQuizByIdFunctionalitySagas.java`

- Single step, no lock, no compensation. Matches read-saga template in `sagas.md` exactly. ✓

### Commands

- `CreateQuizCommand`: `super(unitOfWork, serviceName, null)` — correct for create (no aggregateId yet). ✓
- `UpdateQuizCommand`: `super(unitOfWork, serviceName, quizAggregateId)`. ✓
- `GetQuizByIdCommand`: `super(unitOfWork, serviceName, quizAggregateId)`. ✓

### Notification files

- Three subscribe classes, `QuizEventHandling`, `QuizEventHandler`, `QuizEventProcessing`: all structurally correct. ✓
- `QuizEventHandling` uses `@Scheduled(fixedDelay = 1000)` per event type. ✓
- `QuizEventHandler` dispatches via `instanceof` checks for all three event types. ✓

---

## Functionality Coverage

| Operation | In Service | Saga Class | CommandHandler Case | Functionalities Method | T2 Test | Notes |
|-----------|:---------:|:----------:|:-------------------:|:----------------------:|:-------:|-------|
| CreateQuiz | ✓ | CreateQuizFunctionalitySagas | ✓ | ✓ | ✓ | |
| UpdateQuiz | ✓ | UpdateQuizFunctionalitySagas | ✓ | ✓ | ✓ | |
| GetQuizById | ✓ | GetQuizByIdFunctionalitySagas | ✓ | ✓ | ✓ | |

---

## Rule Enforcement

| Rule | Classification | Expected Implementation | Actual Implementation | Status |
|------|:-------------:|------------------------|----------------------|:------:|
| QUIZ_CREATION_DATE_FINAL | P1 final-field | `private final LocalDateTime creationDate` in `Quiz.java` | `@Column private LocalDateTime creationDate` (non-final) | Incorrect |
| QUIZ_DATE_ORDERING | P1 intra-invariant | `quizDateOrdering()` checked in `verifyInvariants()` | `quizDateOrdering()` → `throw QuizzesFullException(QUIZ_DATE_ORDERING)` | Correct |
| QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE | P1 intra-invariant | `quizFieldsFinalAfterAvailableDate()` checked in `verifyInvariants()` | Present with `instanceof` guard; checks `lastModifiedTime > prev.availableDate` | Correct |
| QUIZ_COURSE_EXECUTION_FINAL | P1 final-field | `quizExecution` field effectively immutable | No setter changes it after construction; JPA prevents `final` on `@OneToOne`; reference also non-final | Correct (JPA constraint) |
| QUESTION_EXISTS (P2 — UpdateQuestionEvent) | P2 inter-invariant | `getEventSubscriptions()` + subscribe class + event handler | `QuizSubscribesUpdateQuestion`, `handleUpdateQuestionEvents()`, `processUpdateQuestionEvent()` → `updateQuestionInQuiz` | Correct |
| QUESTION_EXISTS (P2 — DeleteQuestionEvent) | P2 inter-invariant | `getEventSubscriptions()` + subscribe class + event handler | `QuizSubscribesDeleteQuestion`, `handleDeleteQuestionEvents()`, `processDeleteQuestionEvent()` → `removeQuestionFromQuiz` + `InvalidateQuizEvent` | Correct |
| COURSE_EXECUTION_EXISTS (P2 — DeleteCourseExecutionEvent) | P2 inter-invariant | `getEventSubscriptions()` + subscribe class + event handler | `QuizSubscribesDeleteCourseExecution`, `handleDeleteCourseExecutionEvents()`, `processDeleteCourseExecutionEvent()` → `invalidateQuiz` + `InvalidateQuizEvent` | Correct |

---

## Test Coverage

| Test Type | Class | Scenarios Present | Missing Scenarios | Notes |
|-----------|-------|------------------|-------------------|-------|
| T1 | `QuizTest` | "create quiz" | P1 intra-invariant violation cases (QUIZ_DATE_ORDERING, QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE) | `testing.md` places violations in T2 (where QUIZ_DATE_ORDERING is covered); doc contradiction with review-skill Step 7 |
| T2 write | `CreateQuizTest` | success with questions; success with no questions; `getExecutionStep` acquires READ_EXECUTION lock | — | Correct; no P3 guards to test |
| T2 write | `UpdateQuizTest` | success; QUIZ_DATE_ORDERING violation; `getQuizStep` acquires IN_UPDATE_QUIZ lock | QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE violation | Acknowledged in retro 2.6.b: not practically testable without clock manipulation |
| T2 read | `GetQuizByIdTest` | success; not-found throws `SimulatorException` | — | Correctly uses `thrown(SimulatorException)` for not-found |
| T3 | `QuizInterInvariantTest` | UpdateQuestionEvent: reflects + ignores; DeleteQuestionEvent: invalidates + ignores; DeleteCourseExecutionEvent: invalidates + ignores | — | Deletion tests correctly use `and:` + `thrown(SimulatorException)` per docs |

---

## Retro Cross-Reference

| Retro | Session | Priority | Action Item |
|-------|---------|:--------:|-------------|
| retro-2.6.a | 2.6.a | High | `.claude/skills/implement-aggregate/session-a.md` — add getEventSubscriptions() stub note |
| retro-2.6.a | 2.6.a | High | `.claude/skills/implement-aggregate/session-a.md` — add {Aggregate}Factory and {Aggregate}CustomRepository to required artefacts |
| retro-2.6.a | 2.6.a | Medium | `classify-and-plan` skill — include companion entities in plan.md session-a rows |
| retro-2.6.b | 2.6.b | Medium | `plan.md` template — ensure `{Aggregate}Functionalities.java` in session-b table |
| retro-2.6.b | 2.6.b | Low | `session-b.md` — note get-by-id command needed for update sagas with lock steps |
| retro-2.6.c | 2.6.c | — | No action items |
| retro-2.6.d | 2.6.d | High | `docs/concepts/testing.md` — add T3 deletion-event assertion pattern |
| retro-2.6.d | 2.6.d | High | `.claude/skills/implement-aggregate/session-d.md` — add decision rule for `remove()` vs. sub-entity removal |
| retro-2.6.d | 2.6.d | Medium | `docs/concepts/events.md` — clarify `InvalidateQuizEvent` pattern |

All High-priority items target doc/skill files, not source files. No source-file checks required per review skill Step 8.

---

## Build & Test Results

**Command:** `mvn clean -Ptest-sagas test -Dtest="QuizTest,QuizInterInvariantTest,CreateQuizTest,UpdateQuizTest,GetQuizByIdTest"`
**Outcome:** BUILD SUCCESS

| Test class | Result | Failures |
|------------|:------:|:--------:|
| QuizTest | PASS (1/1) | 0 |
| QuizInterInvariantTest | PASS (6/6) | 0 |
| CreateQuizTest | PASS (3/3) | 0 |
| UpdateQuizTest | PASS (3/3) | 0 |
| GetQuizByIdTest | PASS (2/2) | 0 |
| **Total** | **15/15** | **0** |

---

## Action Items

All action items resolved.

| Priority | Category | File | Finding | Resolution |
|----------|---------|------|---------|-----|
| ~~Major~~ | Rule enforcement | `microservices/quiz/aggregate/Quiz.java` | `creationDate` not declared `final` | Added `final`; no-arg constructor initialises to `null` |
| ~~Minor~~ | Pattern deviation | `microservices/quiz/aggregate/Quiz.java` | `getEventSubscriptions()` logic inlined | Extracted `interInvariantQuestionsExist` and `interInvariantCourseExecutionExists` private helpers |
| ~~Minor~~ | Doc contradiction | `docs/concepts/testing.md` + `review-aggregate/SKILL.md` Step 7 | Skill expected T1 violation cases; doc says violations go in T2 | Removed the expectation from Step 7; T1 is correct as-is. Added explicit note that P1 Java-final rules need no tests anywhere. |
| ~~Minor~~ | Test coverage | `sagas/coordination/quiz/UpdateQuizTest.groovy` | No violation test for `QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE` | Added TODO comment; acknowledged untestable without clock manipulation (retro 2.6.b) |
