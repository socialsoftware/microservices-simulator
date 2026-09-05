# Retro — 2.1.a — Course

**App:** quizzes-full-2
**Session:** 2.1.a (Domain Layer)
**Date:** 2026-08-02

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/course/aggregate/Course.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/course/aggregate/CourseType.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/course/aggregate/CourseFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/course/aggregate/CourseCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/course/aggregate/CourseRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/course/aggregate/CourseDto.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/course/aggregate/sagas/SagaCourse.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/course/aggregate/sagas/states/CourseSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/course/aggregate/sagas/factories/SagasCourseFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/course/aggregate/sagas/repositories/CourseCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/course/CourseServiceApplication.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/course/CourseIntraInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy` (appended)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy` (appended)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md` (checkbox + 2.1.a file-row amendment)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/harness-log.md` (rows 1-2)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `.claude/skills/_shared/conventions.md` | Anchor to repository root; Application isolation; Neutral domain; Run the test suite | Yes | Narrow form applied as written; no cross-application read occurred |
| `.claude/skills/implement-aggregate/session-a.md` | all | Partial | Per-file headings precise enough to write each file without guessing, but silent on `final`-field realization and on the saga-state set — see Documentation Gaps |
| `docs/concepts/aggregate.md` | Key Fields; Base Class; Variants→Sagas; Factories; Repositories | Yes | The "AggregateRepository is not generic" note prevented a wrong `extends AggregateRepository<Course>` |
| `docs/concepts/testing.md` | Taxonomy; Assertion Ownership; Choosing Input Values; T1 | Yes | "P1 Java-final fields need no coverage in any tier" is what reduced this T1 to one happy-path case |
| `docs/concepts/sagas.md` | SagaState | Yes | Enum shape + "never redeclare NOT_IN_SAGA" |
| `docs/concepts/rule-enforcement-patterns.md` | P1 | Partial | Consulted for the immutability question; no realization guidance |
| `applications/quizzes-full-2/quizzes-full-2-domain-model.md` | §1, §3.1, §4 | Yes | Field list and CourseType values |
| `applications/quizzes-full-2/plan.md` | §1 Course; §3.1 rule classification | Yes | — |
| `simulator/` (`Aggregate`, `SagaAggregate`, `SagaStateConverter`, `GenericSagaState`, `SagaUnitOfWorkService`) | read-only | Yes | Read to confirm `aggregateType` handling |

---

## Skill Instructions Feedback

### What worked well

- 2.1.a: the explicit rationale blocks in `session-a.md` (why `@Convert` is mandatory, why the copy constructor inherits `sagaState`, why `CustomRepositorySagas` stays empty) removed three decisions that would otherwise have been coin flips.

### What was unclear or missing

- 2.1.a: the `{AppClass}SpockTest.groovy` append is mandated by `session-a.md` but absent from plan.md's 2.1.a file row. The slice appended and reported; the manager amended plan.md.
- 2.1.a: `session-a.md`'s rule "include `READ_{AGGREGATE}` if other aggregates use this aggregate as a cross-aggregate prerequisite" forced a scan of *other* aggregates' plan.md sections. plan.md §1 Course does not state the resulting saga-state set, so `CourseSagaState.READ_COURSE` was emitted on inference.

### Suggested wording / structure changes

- Add the `{AppClass}SpockTest.groovy` append to the standard session-`a` file list emitted by `/classify-and-plan`, so plan.md and `session-a.md` agree.

### Manager observations

- No slice re-spawns; no Type 1 fixes; no Type 2 escalations blocked the session. Session `a` is never sliced, so this was a single whole-session run.
- The slice's retro fragment proposed a Type 1 fix to `docs/concepts/aggregate.md`, claiming that hardcoding `aggregateType` as `"{Aggregate}"` rather than the saga subclass name "silently breaks command routing". The manager verified this against `simulator/.../transaction/sagas/unitOfWork/SagaUnitOfWorkService.java:207-214`: `resolveServiceName` does `aggregateType.replace("Saga", "")` and then lowercases the first character, so `Course` and `SagaCourse` both resolve to `course`. **The report does not hold**; no harness fix was made and no harness-log row was opened for it. The doc line `aggregateType | String | Simple class name, set in subclass constructor` is accurate as written.
- The session-end full `clean` suite reproduced the slice's narrow result exactly (`MAVEN_EXIT=0`, tests=1, failures=0, errors=0). No cross-slice regression, as expected for a single-slice session.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-a.md` | No guidance on how a P1 rule classified as "Java `final` field" is realized in a JPA entity: whether the field is `private final`, how the mandatory no-arg constructor definitely-assigns it, and whether the Hibernate load path tolerates it | Medium — reported by 2.1.a. Invisible in session `a` (T1 never loads a row); first proven or disproven when 2.1.b reads the aggregate back | Add a paragraph under § `{Aggregate}.java` stating the sanctioned shape for an immutable field: creating constructor + copy constructor + null-assigning no-arg constructor + getter, no setter; and state explicitly whether field-access Hibernate writing a `final` instance field reflectively is sanctioned |
| `.claude/skills/classify-and-plan/SKILL.md` | plan.md emits no per-aggregate saga-state set, but `session-a.md` requires `READ_{AGGREGATE}` to be present when a *later* aggregate uses this one as a cross-aggregate prerequisite | Medium — reported by 2.1.a. Forces a session-`a` agent into forward-looking inference; a mismatch surfaces only in a later aggregate's session `c` | Emit the saga-state constant set per aggregate in the plan.md aggregate section, derived from the same dependency DAG that produces the Implementation Order table |

---

## Patterns to Capture

- **Pattern:** Immutable-by-`final` aggregate field
  **Observed in:** `applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/course/aggregate/Course.java`
  **Description:** Creating constructor + copy constructor + null-assigning no-arg constructor + getter only. By construction T1 never exercises the Hibernate load path for such a field; the first read-back in session `b` is the real proof. Carry forward as a watch item for the 2.1.b agent.

- **Pattern:** All-`final` P1 rule set collapses the T1 tier
  **Observed in:** `applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/course/CourseIntraInvariantTest.groovy`
  **Description:** When every P1 rule for an aggregate is a final-field rule, `verifyInvariants()` has no predicate, T1 reduces to exactly one happy-path case, and the aggregate contributes zero error-message constants. Worth stating once so review does not read it as under-coverage.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: 1, 2

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 1 | 2 | deferred | - |
| 2 | 2 | deferred | - |

---

## One-Line Summary

Course's domain layer is green on one happy-path T1 case because every P1 rule is a Java `final` field, which also means the harness's silence on how such a field survives the JPA load path is unproven until 2.1.b reads a Course back.
