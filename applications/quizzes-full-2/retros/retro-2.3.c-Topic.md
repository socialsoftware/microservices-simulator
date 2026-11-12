# Retro — 2.3.c — Topic

**App:** quizzes-full-2
**Session:** 2.3.c (Write Functionalities)
**Date:** 2026-08-04

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/topic/CreateTopicCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/topic/UpdateTopicCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/topic/DeleteTopicCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/events/UpdateTopicEvent.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/events/DeleteTopicEvent.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/coordination/sagas/CreateTopicFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/coordination/sagas/UpdateTopicFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/coordination/sagas/DeleteTopicFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/coordination/webapi/TopicController.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/topic/CreateTopicTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/topic/UpdateTopicTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/topic/DeleteTopicTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/topic/UpdateTopicCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/topic/DeleteTopicCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/resources/groovy/UpdateTopicCompensationTest/UpdateTopicFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/resources/groovy/DeleteTopicCompensationTest/DeleteTopicFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/service/TopicService.java` (appended: `createTopic` / `updateTopic` / `deleteTopic`; constructor widened with `AggregateIdGeneratorService`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/messaging/TopicCommandHandler.java` (appended: write cases)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/coordination/functionalities/TopicFunctionalities.java` (appended: write coordinators)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/topic/TopicServiceTest.groovy` (appended: write-method cases + event-publication assertions)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy` (modified: `topicService` bean widened with `AggregateIdGeneratorService`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy` (modified: `createTopic()` helper body re-pointed at the real create saga)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md` (manager: 2.3.c row amended for six omitted files; 2.3.c checkbox ticked)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/harness-log.md` (manager: row 25)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `.claude/skills/implement-aggregate/session-c.md` | whole file | Yes | The § Produce warning that the SpockTest-helper edit lives outside § Produce is well placed — it is the one required edit a manifest-reading agent drops. |
| `.claude/skills/_shared/conventions.md` | § Anchor to the repository root, § Application isolation, § Run the test suite | Yes | The narrowed-form caveat about stale surefire reports is real; the slice cleared the report dir for its final run rather than quote a total it did not produce. |
| `docs/concepts/sagas.md` | § Step Ordering, § Lock-Acquisition Step Pattern, § R4 Decision Table, § Create Functionality Sagas, § Semantic-lock release on abort is automatic | Yes | § Create Functionality Sagas Shape 2 answered "does `createTopicStep` need a compensation?" directly: it is the last step, so no. |
| `docs/concepts/service.md` | § Method Patterns, § Copy-on-Write Rule, § Injected Dependencies, § P3 Guard Placement | Yes | The Injected-Dependencies "closed list" note is what stopped the slice adding `TopicRepository` speculatively. |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § File Location, § ServiceMapping, § Routing Commands | Yes | The "create command passes `null` as `rootAggregateId`" note is load-bearing and was stated. |
| `docs/concepts/testing.md` | § Assertion Ownership, § T2, § T4, § Compensation Test, § Fake/Wrong/Weak | Partial | § Compensation Test's read-back template presumes a by-id read coordinator exists. See Documentation Gaps. |

---

## Skill Instructions Feedback

### What worked well

- (2.3.c1) session-c.md's two-edit rule for a service gaining a collaborator (constructor + `@Bean`, never a second bean) is stated in exactly the place an appending session reads, and its "usual case is `AggregateIdGeneratorService`" line predicted this slice precisely.
- (2.3.c1) The harness-log row 19 ruling relayed in the brief (omit the T4 happy path for a delete-shaped operation) matches testing.md § T4's own exception clause verbatim, so there was nothing to reconcile.

### What was unclear or missing

- (2.3.c1) Nothing that blocked the slice.

### Suggested wording / structure changes

- (2.3.c1) Add one sentence to testing.md § Compensation Test: when the aggregate exposes no by-id read coordinator, read back through whichever read coordinator does resolve it (filtering the result), rather than adding a coordinator for the test's benefit.

### Manager observations

- One slice, `2.3.c1`, covering all three write functionalities. Zero re-spawns; `STATUS: DONE`, `FRICTION: none`.
- `git status --porcelain` after the slice confirmed every changed path was under `applications/quizzes-full-2/`. No contract breach.
- The slice ran the compensation sanity check testing.md mandates: with both impairment CSVs' fault flags flipped to 0, `MAVEN_EXIT=1` and both compensation tests failed on the missing exception, and the step log showed `getTopicStep` executing before the fault point in both sagas. That is direct evidence the lock-acquiring step is genuinely traversed rather than the test passing vacuously. Flags restored and re-run green.
- The session-end full clean suite was the load-bearing check for this session: 2.3.c replaces the `createTopic()` fixture helper body, so 2.3.b's read tests run through the real create saga from now on. No regression — `MAVEN_EXIT=0`, `tests=73 failures=0 errors=0 skipped=0`.
- One new Type 2 logged as `deferred` (row 25), on the same handling as rows 23 and 24: the slice did not halt, resolved it against the aggregate's actual read surface, and writing the missing guidance would be the manager answering a Type 2 itself.
- Across all three sessions of aggregate 3: three slices, zero re-spawns, zero Type 1 fixes, zero `harness:` commits, three deferred Type 2 rows.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

| Saga class | Step name | Foreign aggregate locked | Lock-acquisition test name | Present? |
|------------|-----------|--------------------------|----------------------------|---------:|
| `UpdateTopicFunctionalitySagas` | `getTopicStep` → `setSemanticLock(TopicSagaState.IN_UPDATE_TOPIC)` | none (primary aggregate) | `UpdateTopicTest` — "updateTopic: getTopicStep acquires IN_UPDATE_TOPIC semantic lock" | Yes |
| `DeleteTopicFunctionalitySagas` | `getTopicStep` → `setSemanticLock(TopicSagaState.IN_DELETE_TOPIC)` | none (primary aggregate) | `DeleteTopicTest` — "deleteTopic: getTopicStep acquires IN_DELETE_TOPIC semantic lock" | Yes |
| `CreateTopicFunctionalitySagas` | no `setSemanticLock` call site | — | — | n/a |

No `Present? = No` rows.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/testing.md` § Compensation Test | The template's third assertion is a read-back "through the functionality's own getter", which presumes a by-id read coordinator exists for the aggregate. An aggregate may deliberately expose only a by-foreign-key list read | Low - reported by 2.3.c1; resolved by asserting through the list coordinator filtered by aggregate id. The risk is an agent instead inventing an unplanned by-id coordinator and test to satisfy the template | Add one sentence: when the aggregate exposes no by-id read coordinator, read back through whichever read coordinator does resolve it, filtering the result. Harness-log row 25 |

---

## Patterns to Capture

- **Pattern:** P4a data-assembly step whose upstream DTO seeds nothing
  **Observed in:** `microservices/topic/coordination/sagas/CreateTopicFunctionalitySagas.java`
  **Description:** When the primary aggregate caches only the upstream's id, the data-assembly step should send the command without assigning the result — storing it forces a dead field plus a dead getter on the functionality. Leave a comment saying why the DTO is not carried forward, so a later reader does not "fix" the missing assignment.

- **Pattern:** Impairment directories are keyed by test-class simple name
  **Observed in:** `src/test/resources/groovy/UpdateTopicCompensationTest/`, `.../DeleteTopicCompensationTest/`
  **Description:** An absent CSV means no impairment. A compensation test whose setup now runs a real create saga (because session `c` replaced the fixture helper) therefore needs no CSV for that create saga. Worth stating, since replacing the helper makes every compensation test's setup traverse an extra saga class.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: 25

For each `fixed` row, the `harness:` commit sha:

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 25 | 2 | deferred | - |

---

## One-Line Summary

All three Topic write functionalities landed on one slice with both semantic locks covered and the compensation tests proven non-vacuous, leaving one open Type 2 where the compensation-test template assumes a by-id read coordinator the aggregate does not have.
