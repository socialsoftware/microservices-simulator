# Retro — 2.5.c — Question

**App:** quizzes-full-2
**Session:** 2.5.c (Write Functionalities)
**Date:** 2026-08-06

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/events/UpdateQuestionEvent.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/events/DeleteQuestionEvent.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/question/CreateQuestionCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/question/UpdateQuestionCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/question/DeleteQuestionCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/coordination/sagas/CreateQuestionFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/coordination/sagas/UpdateQuestionFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/coordination/sagas/DeleteQuestionFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/coordination/webapi/QuestionController.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/service/QuestionService.java` (write methods appended; constructor widened)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/messaging/QuestionCommandHandler.java` (write cases appended)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/coordination/functionalities/QuestionFunctionalities.java` (write coordinators appended)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/question/QuestionServiceTest.groovy` (write-method + event-publication cases appended)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/question/CreateQuestionTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/question/UpdateQuestionTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/question/DeleteQuestionTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/question/UpdateQuestionCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/question/DeleteQuestionCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/resources/groovy/UpdateQuestionCompensationTest/UpdateQuestionFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/resources/groovy/DeleteQuestionCompensationTest/DeleteQuestionFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy` (`createQuestion` helper body swapped to the create saga)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy` (`questionService` bean widened)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md` (2.5.c row amended; checkbox ticked)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/harness-log.md` (row 42)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | § Injected Dependencies, § Method Patterns (Create / Mutate / Mutate with event publication), § Copy-on-Write Rule, § DTO Immutability (R7), § Exception-Throw Convention, § P3 Guard Placement | Yes | Copy-on-write for soft-delete and the `AggregateIdGeneratorService` widening rule both applied verbatim. |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § File Location, § ServiceMapping Enum, § Sending Commands, § Routing Commands | Yes | The `null` `rootAggregateId` rule for create commands settled `CreateQuestionCommand`. |
| `docs/concepts/sagas.md` | § Step Ordering, § Lock-Acquisition Step Pattern, § R4 Decision Table, § Create Functionality Sagas, § Semantic-lock release on abort is automatic, § Write Workflow Structure | Partial | Silent on a `× N` data-assembly fetch - the Type 2 halt below. Fixed this session by the new § Collection-valued data-assembly step. |
| `docs/concepts/testing.md` | § Assertion Ownership, § T2 — Service Test, § Event Publication, § T4 — Functionality Test, § Compensation Test, § Fake / Wrong / Weak Detection Checklist, § Choosing Input Values | Yes | The delete-shaped "no happy-path case" exception and the class-scoped negative event case both applied as written. |
| `docs/concepts/rule-enforcement-patterns.md` | (via plan.md rule table only) | Yes | `TOPIC_BELONGS_TO_QUESTION_COURSE` is P1, so no service or saga guard was written. |

---

## Skill Instructions Feedback

### What worked well

- § "Update `{AppClass}SpockTest.groovy`" was precise about keeping the 2.5.b signature and defaults.
  The swap to the real create saga was a body-only change and all six session-`b` read tests passed
  unchanged, because 2.5.b had already minted `courseAggregateId` from `createCourse()`.
- § "One `{Op}CompensationTest.groovy`" states the applicability test per functionality rather than
  per session, which correctly excluded `CreateQuestion` (its create step is last and holds no lock)
  and included both mutate functionalities.
- The mandated fault-flag-`0` sanity check earned its keep: it confirmed `getQuestionStep` logs
  `START EXECUTION STEP` before the fault fires in both compensation tests, so neither is a false
  positive.

### What was unclear or missing

- Nothing beyond the `× N` data-assembly gap recorded as harness-log row 42.

### Suggested wording / structure changes

- (none)

---

## Semantic-Lock Coverage Audit

| Saga class | Step name | Foreign aggregate locked | Lock-acquisition test name | Present? |
|------------|-----------|--------------------------|----------------------------|---------:|
| `UpdateQuestionFunctionalitySagas` | `getQuestionStep` | none (primary: `Question`) | `UpdateQuestionTest."updateQuestion: getQuestionStep acquires IN_UPDATE_QUESTION semantic lock"` | Yes |
| `DeleteQuestionFunctionalitySagas` | `getQuestionStep` | none (primary: `Question`) | `DeleteQuestionTest."deleteQuestion: getQuestionStep acquires IN_DELETE_QUESTION semantic lock"` | Yes |

`CreateQuestionFunctionalitySagas` has no `setSemanticLock` call site: the create step brings the
aggregate into existence, and `getCourseStep` / `getTopicsStep` are plain upstream reads.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/sagas.md` | No pattern for a data-assembly fetch over a collection (one upstream fetch per element of a caller-supplied id list) | High - recurs for every aggregate with a `× N` snapshot, i.e. Quiz, QuizAnswer and Tournament | Fixed this session: new § Collection-valued data-assembly step, commit `62c3cf6bb` |

---

## Patterns to Capture

- **Pattern:** Collection-valued data-assembly step
  **Observed in:** `microservices/question/coordination/sagas/CreateQuestionFunctionalitySagas.java`,
  `.../UpdateQuestionFunctionalitySagas.java`
  **Description:** One `get{Members}Step` whose action loops over the caller-supplied id list,
  sending one `Get{Member}ByIdCommand` per element and accumulating the snapshot DTOs on the
  functionality. Keeps the step name independent of the input, which the three mechanisms that key
  on step names as literals (`executeUntilStep`, impairment CSVs, the lock audit) all require.
  Now documented in `docs/concepts/sagas.md`.

- **Pattern:** Service-stamped creation timestamp
  **Observed in:** `microservices/question/service/QuestionService.java` (`createQuestion`)
  **Description:** `creationDate` is not a functionality parameter; the create service method stamps
  it with `DateHandler.now()` (the framework's fixed-UTC clock), never `LocalDateTime.now()`.
  `testing.md` § Choosing Input Values names `DateHandler` as the clock a guard reads, but no doc
  states it is also the clock a write method stamps with.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: 42

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 42 | 2 | fixed | `62c3cf6bb` |

---

## One-Line Summary

The harness had no pattern for an N-element data-assembly fetch; the human chose a single looping
step over one step per element, on the grounds that step names must stay literal for
`executeUntilStep`, the impairment CSVs and the lock audit to keep working.
