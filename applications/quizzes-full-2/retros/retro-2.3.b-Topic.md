# Retro — 2.3.b — Topic

**App:** quizzes-full-2
**Session:** 2.3.b (Read Functionalities)
**Date:** 2026-08-04

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/topic/GetTopicByIdCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/topic/GetTopicsByCourseCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/service/TopicService.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/messaging/TopicCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/coordination/sagas/GetTopicsByCourseFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/coordination/functionalities/TopicFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/topic/TopicServiceTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/topic/GetTopicsByCourseTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/ServiceMapping.java` (appended: `TOPIC("topic")` row)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy` (appended: imports + `topicService`, `topicCommandHandler`, `topicFunctionalities` beans)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy` (appended: imports, `topicService` + `topicFunctionalities` fields, `createTopic` fixture helper)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md` (manager: 2.3.b row amended for five omitted files; 2.3.b checkbox ticked)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/harness-log.md` (manager: rows 23, 24)

### Application bug fixes (earlier-session files)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/aggregate/TopicRepository.java` (2.3.a file widened: `findAllLatestActiveByCourse` JPQL)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/aggregate/TopicCustomRepository.java` (2.3.a file widened: `findTopicIdsByCourse(Integer)`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/aggregate/sagas/repositories/TopicCustomRepositorySagas.java` (2.3.a file widened: `findTopicIdsByCourse` implementation)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `.claude/skills/_shared/conventions.md` | § Anchor to the repository root, § Application isolation, § Run the test suite | Yes | The "narrow form sums stale reports" warning was exactly the trap the slice would otherwise have fallen into; the 57 total is meaningless for a narrowed run. |
| `.claude/skills/implement-aggregate/session-b.md` | whole file | Yes | The "`Get{Aggregate}ByIdCommand` unconditionally" rule and the fixture-helper contract were both decisive. |
| `docs/concepts/service.md` | § Injected Dependencies, § Method Patterns → Read, § Custom Repository - Latest-Active-Version Query | Yes | The closed dependency list plus "omit any the service does not use" resolved the `AggregateIdGeneratorService` question without a halt. |
| `docs/concepts/sagas.md` | § Read Functionality Sagas, § List-return read variant, § one-step vs two-step decision | Yes | The decision box answered the one-step/two-step question in one read. |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § File Location, § Routing Commands | Yes | The mandatory no-arg ctor + setters note is load-bearing under `local.messaging.serialize`. |
| `docs/concepts/testing.md` | § T2 (incl. Not-Found Paths), § T4, § Assertion Ownership, § Fake/Wrong/Weak | Yes | § Assertion Ownership kept the T4 file to one orchestration case instead of duplicating T2. |
| `docs/architecture.md` | § Package Structure Convention, R1-R3 | Yes | — |
| `applications/quizzes-full-2/plan.md` | § 3 Topic, 2.3.b file row | Yes | The explicit `GetTopicById` note removed the one ambiguity this slice had. |

---

## Skill Instructions Feedback

### What worked well

- (2.3.b1) session-b.md's § Produce being declared authoritative over the plan.md row made the three repository-widening files a non-event: the slice knew to produce them and report, rather than halt.
- (2.3.b1) The brief's "Note on bulk reads" pointing at the 2.1.b precedent was redundant with service.md but cheap, and confirmed the reading rather than competing with it.

### What was unclear or missing

- (2.3.b1) session-b.md's § "Update `BeanConfigurationSagas.groovy`" gives a `@Bean` template that always includes the JPA repository parameter, while service.md's closed list and its "omit any the service genuinely does not use" clause make it optional. Not a contradiction — the template is illustrative — but the reader has to hold both sentences at once to see that.

### Suggested wording / structure changes

- (2.3.b1) Add one sentence to `docs/concepts/commands.md` § "What a Command Is": a bulk read filtered by a non-PK field also passes `null` as `rootAggregateId`, for the same reason a create does.
- (2.3.b1) Note in session-b.md that foreign-aggregate id params of a fixture helper stay required and undefaulted; only own-field params get constant defaults.

### Manager observations

- One slice, `2.3.b1`, covering the whole session. Zero re-spawns; `STATUS: DONE`, `FRICTION: none`.
- `git status --porcelain` after the slice confirmed every changed path was under `applications/quizzes-full-2/`. No contract breach.
- The slice flagged three brief deviations, all reviewed and accepted: the ServiceMapping file is `ServiceMapping.java` (the brief mis-named it `QuizzesFull2ServiceMapping.java`; the slice correctly followed plan.md's `{src}ServiceMapping.java`); `TopicService` omits `AggregateIdGeneratorService` and `TopicRepository` because no read method uses them, which service.md explicitly permits and which 2.3.c will widen; and `GetTopicsByCourseCommand` passes `null` as `rootAggregateId`, matching `GetCoursesCommand`.
- The slice reported `FRICTION: none` but its Documentation Gaps table names two harness artifacts. The manager logged both as Type 2 `deferred` (harness-log rows 23, 24) rather than escalating them as blocking halts: the slice did not halt, both questions were settled by in-app precedent rather than by manager fiat, and writing the missing guidance would be the manager answering a Type 2 itself. Both are surfaced to the human at the aggregate boundary.
- The session-end full clean suite found no cross-slice regression: `MAVEN_EXIT=0`, `tests=57 failures=0 errors=0 skipped=0`.
- No Type 2 or `2-fw` was escalated mid-session, so no question blocked the run.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a — session `b` has no `setSemanticLock` call sites; reads acquire no locks.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/commands.md` | § "What a Command Is" states the `rootAggregateId` rule for create commands (pass `null`) but says nothing about a bulk read filtered by a non-PK field, which likewise has no single root aggregate | Low - reported by 2.3.b1; the create paragraph generalises, but the generalisation is inferred, not stated. A slice could instead pass the filter's foreign id, which would be wrong: it is not the aggregate whose lock lifecycle the command joins | Add one sentence to the same callout: a bulk read filtered by a non-PK field also passes `null`, for the same reason - it declares no lock and no forbidden states, so the handler never dereferences it. Harness-log row 23 |
| `.claude/skills/implement-aggregate/session-b.md` | § "Update `{AppClass}SpockTest.groovy`" says a create helper's params are "defaulted to the domain constants", but a foreign-key param has no usable constant and must come from a real upstream fixture at call time | Low - reported by 2.3.b1; resolved by leaving it required and first in the signature, which is also what 2.3.c's `CreateTopic(courseAggregateId, name)` needs | Note that foreign-aggregate id params stay required and undefaulted; only own-field params get constant defaults. Harness-log row 24 |
| `.claude/skills/implement-aggregate/session-b.md` | § "Update `BeanConfigurationSagas.groovy`"'s `@Bean` template always shows the JPA repository parameter, which service.md makes optional | Low - reported by 2.3.b1; illustrative template, resolved by reading both | Mark the template's dependency list as illustrative, or point it at service.md's closed list |

---

## Patterns to Capture

- **Pattern:** Filtered bulk read as narrowed latest-active JPQL
  **Observed in:** `microservices/topic/aggregate/TopicRepository.java`, `.../sagas/repositories/TopicCustomRepositorySagas.java`
  **Description:** A bulk read filtered by a column is `findAllLatestActiveByCourse`-style JPQL — the unfiltered query with one extra predicate — rather than `findAllLatestActive()` plus an in-memory filter. The unfiltered variant loads every aggregate of the type only to discard most of them.

- **Pattern:** Three cases for a filtered-bulk-read T2 class
  **Observed in:** `sagas/topic/TopicServiceTest.groovy`
  **Description:** A filtered bulk read wants matching set, empty set, and an exclusion case with a second filter value present. Without the third, a service that ignores its filter argument entirely still passes.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: 23, 24

For each `fixed` row, the `harness:` commit sha:

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 23 | 2 | deferred | - |
| 24 | 2 | deferred | - |

---

## One-Line Summary

Topic's read side went in on one slice with no halts, leaving two open Type 2 gaps where the harness is silent on non-PK-filtered bulk reads — for the `rootAggregateId` value and for fixture-helper parameter defaulting.
