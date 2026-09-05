# Retro — 2.6.c — Quiz

**App:** quizzes-full-2
**Session:** 2.6.c (Write Functionalities)
**Date:** 2026-08-07

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/events/InvalidateQuizEvent.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/quiz/CreateQuizCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/quiz/UpdateQuizCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/service/QuizService.java` (appended: `createQuiz`, `updateQuiz`, `toQuizQuestion`; constructor widened with `AggregateIdGeneratorService`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/messaging/QuizCommandHandler.java` (appended: two write cases)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/coordination/sagas/CreateQuizFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/coordination/sagas/UpdateQuizFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/coordination/functionalities/QuizFunctionalities.java` (appended: `createQuiz`, `updateQuiz`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/coordination/webapi/QuizController.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy` (widened the `quizService(...)` `@Bean`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy` (`createQuiz` helper body swapped onto the real functionality; quiz date constants re-pinned)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/quiz/QuizServiceTest.groovy` (appended: 5 write-method / event cases)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/quiz/CreateQuizTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/quiz/UpdateQuizTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/quiz/UpdateQuizCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/resources/groovy/UpdateQuizCompensationTest/UpdateQuizFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md` (tick + 2.6.c row amendment)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/harness-log.md` (row 52)

### Application bug fixes (earlier-session files)

- `QuizzesFull2SpockTest.groovy` — `QUIZ_CREATION_DATE`, `QUIZ_AVAILABLE_DATE`, `QUIZ_CONCLUSION_DATE`
  and `QUIZ_RESULTS_DATE` were pinned to absolute 2025 instants by 2.6.b. Once the fixture helper
  routes through the real create functionality, which stamps `creationDate = DateHandler.now()`,
  every quiz fixture violated `QUIZ_DATE_ORDERING`. Re-pinned relative to `DateHandler.now()`.
- `QuizServiceTest.groovy` — the 2.6.b read-back asserted `result.creationDate == QUIZ_CREATION_DATE`
  and `result.executionVersion == QUIZ_EXECUTION_VERSION`. Both values are now produced by the create
  path (clock stamp; version off the fetched `ExecutionDto`), not by the fixture, so both became
  non-null assertions. The `executionVersion` equality is re-asserted properly in the new
  `createQuiz` T2 case, which holds the `ExecutionDto` it passed in and compares against it.

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | § Injected Dependencies, § Method Patterns (Create / Mutate), § Copy-on-Write Rule, § DTO Immutability (R7), § Exception-Throw Convention, § P3 Guard Placement | Yes | R3 explicitly permits a foreign `{Xxx}Dto` as a service parameter, which settled passing `ExecutionDto` into `createQuiz` rather than mutating the caller's `QuizDto` to carry the fetched version. |
| `docs/concepts/sagas.md` | § Step Ordering, § Collection-valued data-assembly step, § Lock-Acquisition Step Pattern, § R4 Decision Table, § Create Functionality Sagas (Shape 1), § Semantic-lock release on abort is automatic, § Write Workflow Structure | Yes | The `× N` looping-step rule added by harness row 42 applied verbatim to `getQuestionsStep` in both sagas. |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § File Location, § ServiceMapping Enum, § Sending Commands, § Routing Commands | Yes | The `null` `rootAggregateId` rule for create commands covered `CreateQuizCommand`. |
| `docs/concepts/testing.md` | § Assertion Ownership, § T2 — Service Test (incl. § Event Publication, § Not-Found Paths), § T4 — Functionality Test, § Compensation Test, § Fake/Wrong/Weak, § Choosing Input Values | Partial | See Documentation Gaps: § Event Publication assumes every published event has a publication site in the session that writes the class. |
| `.claude/skills/implement-aggregate/session-c.md` | whole file | Partial | § "Update `{AppClass}SpockTest.groovy`" covered only the synthetic-foreign-id failure mode of the swap; fixed this session (row 52). |
| `.claude/skills/_shared/conventions.md` | § Anchor to the repository root, § Resolve app context, § Application isolation, § Harness log, § Run the test suite | Yes | The "never pipe maven" rule mattered: the first full-suite invocation ran from the repo root and failed with "no POM in this directory", which the exit-status-plus-surefire discipline caught immediately. |

---

## Skill Instructions Feedback

### What worked well

- The § Produce subheadings being authoritative over plan.md removed all hesitation about emitting
  `UpdateQuizCompensationTest.groovy` and its CSV, which the plan.md row omitted (this plan predates
  the classify-and-plan fix recorded in harness row 48).
- The compensation-test applicability test is stated per functionality, not per session, and decided
  both cases here without further reasoning: `UpdateQuiz` locks in `getQuizStep` and mutates in a
  dependent step (test required); `CreateQuiz` takes no lock (skipped).
- The mandated fault-flag sanity check earned its keep: flipping `updateQuizStep` to `0` produced
  "no exception was thrown" and the log showed `getQuizStep` executing before `updateQuizStep`, so
  the lock is genuinely held when the fault fires.

### What was unclear or missing

- § "Event classes" and the T2 § Event-publication block both key off "plan.md Events published",
  and implicitly assume the session writing the event class also owns a service method that
  publishes it. For `InvalidateQuizEvent` plan.md explicitly splits the two across 2.6.c (class) and
  2.6.d (publication), leaving the payload-asserting T2 case with nothing to trigger. The negative
  case is unaffected and was written here. Resolved from plan.md's own note rather than the skill.

### Suggested wording / structure changes

- `docs/concepts/testing.md` § T2 — Service Test / Event Publication: add a sentence covering the
  split case — when an aggregate's only published event is raised from an event handler rather than
  from a write functionality, the payload case belongs to session `d`, and session `c` still owns
  the class-scoped negative case.

---

## Semantic-Lock Coverage Audit

| Saga class | Step name | Foreign aggregate locked | Lock-acquisition test name | Present? |
|------------|-----------|--------------------------|----------------------------|---------:|
| `UpdateQuizFunctionalitySagas` | `getQuizStep` | none (primary aggregate `Quiz`) | `updateQuiz: getQuizStep acquires IN_UPDATE_QUIZ semantic lock` (`UpdateQuizTest.groovy`) | Yes |

`CreateQuizFunctionalitySagas` contributes no row: it calls `setSemanticLock` nowhere. Its
`getExecutionStep` and `getQuestionsStep` are plain upstream reads, and the create step brings the
aggregate into existence, so there is no prior state to lock (`sagas.md` § Create Functionality
Sagas). `QuizSagaState` declares only `IN_UPDATE_QUIZ`, matching plan.md's "`CreateQuiz` contributes
none."

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-c.md` | § "Update `{AppClass}SpockTest.groovy`" required the swap and required session `b`'s defaults kept, without covering the case where a clock-stamped field makes the two unsatisfiable | High — 14 test errors, every quiz fixture | Fixed this session (harness row 52) |
| `docs/concepts/testing.md` | § T2 Event Publication assumes the session writing an event class also has a publication site for it | Low — plan.md happened to state the split explicitly for this aggregate | Add the split-case sentence proposed above |

---

## Patterns to Capture

- **Pattern:** Fixture constants pinned to the create path's clock
  **Observed in:** `QuizzesFull2SpockTest.groovy`
  **Description:** Where an aggregate's create path stamps a temporal field from `DateHandler.now()`
  and a P1 rule orders it against caller-supplied instants, the fixture's date constants must be
  expressed as offsets from that same clock. Absolute constants are correct only while the
  direct-on-aggregate fixture supplies the stamped field too, so they expire silently at the session-`c`
  swap rather than at some later date. Now documented in session-c.md.

- **Pattern:** A published event whose only publication site is an event handler
  **Observed in:** `InvalidateQuizEvent.java`, `plan.md` §6 Quiz
  **Description:** An aggregate can publish an event that no write functionality raises — here the
  quiz is invalidated only by the incoming `DeleteQuestionEvent`. The event class is a session-`c`
  artifact (it is a published-event class) but its T2 payload assertion cannot exist until session `d`
  wires the publication. The session-`c` T2 class still owns the class-scoped negative case.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: 52

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 52 | 1 | fixed | `2457b5bbe` |

---

## One-Line Summary

The session-`c` fixture swap has a second, undocumented failure mode — a clock-stamped field ordered
against a caller-supplied constant — which session `b` cannot avoid and which fails every fixture of
the aggregate the moment the helper is routed through the real create path.
