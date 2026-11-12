# Retro — 2.4.a — Execution

**App:** quizzes-full-2
**Session:** 2.4.a (Domain Layer)
**Date:** 2026-08-04

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/aggregate/Execution.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/aggregate/ExecutionStudent.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/aggregate/ExecutionStudentDto.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/aggregate/ExecutionFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/aggregate/ExecutionCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/aggregate/ExecutionDto.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/aggregate/ExecutionRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/aggregate/sagas/SagaExecution.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/aggregate/sagas/states/ExecutionSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/aggregate/sagas/factories/SagasExecutionFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/aggregate/sagas/repositories/ExecutionCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/ExecutionServiceApplication.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/execution/ExecutionIntraInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy` (appended: `EXECUTION_*` / `EXECUTION_STUDENT_*` domain constants, `import java.time.LocalDateTime`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy` (appended: `SagasExecutionFactory` / `ExecutionCustomRepositorySagas` beans + imports)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/exception/QuizzesFull2ErrorMessage.java` (appended: `REMOVE_NO_STUDENTS`, `STUDENT_ALREADY_ENROLLED`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md` (2.4.a row amended for the three omitted files; 2.4.a checkbox ticked)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/harness-log.md` (rows 29, 30)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `.claude/skills/_shared/conventions.md` | § Anchor to the repository root, § Application isolation, § Neutral domain, § Run the test suite | Yes | The "narrowed run sums stale classes" warning was exactly right — the unfiltered total was 78 against the 5 the narrowed run produced. |
| `.claude/skills/implement-aggregate/session-a.md` | § Reads, § Verify Mandatory Files, § Produce (all subheadings), § Update `{AppClass}SpockTest.groovy`, § Update `BeanConfigurationSagas.groovy` | Partial | Two gaps, both on the owned-collection case — see § Documentation Gaps. Both are now fixed (harness-log rows 29, 30). |
| `docs/concepts/aggregate.md` | § Key Fields, § Base Class, § Variants→Sagas, § Factories, § Repositories, § `getEventSubscriptions()` | Yes | R6 (no repository reads in `verifyInvariants`) held trivially: both P1 rules read only `students`. |
| `docs/concepts/testing.md` | § T1, § Choosing Input Values (EP & BVA), § Assertion Ownership, § Fake/Wrong/Weak | Yes | The decision rule cleanly separated `REMOVE_NO_STUDENTS` (collection-size → straddle pair) from `STUDENT_ALREADY_ENROLLED` (categorical → single representative). |
| `applications/quizzes-full-2/plan.md` | §3.1/§3.2 rule rows 51 + 54, snapshot table rows 125-126 + `CourseType` note, §4 Execution section | Yes | The `CourseType` note pre-resolved what would otherwise have been a Type 2 escalation (importing `course.aggregate.CourseType` from the execution package). |
| `applications/quizzes-full-2/quizzes-full-2-domain-model.md` | §1 entity rows, §2 relationships, §3.2 `REMOVE_NO_STUDENTS` / `STUDENT_ALREADY_ENROLLED` | Yes | §1 gave the `ExecutionStudent` field list verbatim. |
| `applications/quizzes-full-2/quizzes-full-2-aggregate-grouping.md` | §2 snapshot field table | Yes | Confirmed `userVersion` is cached on `ExecutionStudent` because it subscribes. |

---

## Skill Instructions Feedback

### What worked well

- 2.4.a: session-a.md's per-file subheadings are directly executable.
- 2.4.a: the `SagaExecution` copy-constructor rationale (inherit `sagaState`, never reset) is stated with enough reasoning that it is not guessable-wrong.
- 2.4.a: plan.md's `**Saga states:**` line was transcribed verbatim with no derivation needed — exactly as intended.

### What was unclear or missing

- 2.4.a: session-a.md § "`{Aggregate}.java`" said an owned collection uses `@OneToMany(cascade = ALL, orphanRemoval = true)` and that the "aggregate is the inverse side with no FK column". The two statements are in tension: without `mappedBy` the aggregate is the *owning* side and Hibernate materialises a join table.
- 2.4.a: session-a.md forbids "business logic methods" on the aggregate, but T1 must mutate an owned collection to trip a P1 rule. The slice treated `addStudent(ExecutionStudent)` as a collection setter; the skill did not say whether collection add/remove helpers are session `a` or session `c` output.

### Suggested wording / structure changes

- Both of the above are now applied to `.claude/skills/implement-aggregate/session-a.md` — see § Harness Changes.

### Manager observations

- Slice 2.4.a returned `DONE` on its first spawn with `FRICTION: none`; both harness defects arrived only inside its `RETRO FRAGMENT` § Documentation Gaps, not as `FRICTION` blocks. The manager reclassified them at the gate. This is a reporting-channel gap worth watching: a slice that "worked around it and moved on" routes a genuine gate item through the retro rather than the friction channel, where a manager that merged fragments mechanically would never triage it.
- Row 29 was triaged as Type 1 and fixed unilaterally: the claim is mechanically demonstrable against the JPA spec and against the mapping the slice actually emitted (`Execution.java:32`, no `mappedBy`).
- Row 30 was escalated as Type 2 and the human ruled for session `a`. No application code changed — the slice's reading already matched the ruling.
- The session-end full clean suite was green on the first run (`MAVEN_EXIT=0`, tests=78, failures=0, errors=0). No cross-slice regression, as expected for a single-slice session.
- No slice re-spawns.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-a.md` | The owned-collection bullet's "inverse side with no FK column" contradicts the annotation it prescribes (no `mappedBy` ⇒ owning side ⇒ join table). | Medium — reported by 2.4.a. The mapping persists either way, but an agent trusting the prose adds `mappedBy` and then fails at EMF init because the owned entity has no back-reference field. | Applied — the bullet now states the aggregate is the owning side, that the association is a join table, and that `mappedBy` must not be added. |
| `.claude/skills/implement-aggregate/session-a.md` | No statement on whether collection add/remove helpers on the aggregate are session `a` or session `c` output, despite "no business logic methods". | Medium — reported by 2.4.a. Risks either a T1 test reaching through `getX().add(...)`, or session `c` re-opening a file session `a` owns. | Applied — a collection field gets `add{Element}` / `remove{Element}` in session `a`; they are setters, not business logic. |
| `applications/quizzes-full-2/plan.md` (2.4.a row) | Row omitted `{test}QuizzesFull2SpockTest.groovy`, `{test}BeanConfigurationSagas.groovy` and `QuizzesFull2ErrorMessage.java`, all three mandated by session-a.md and all three present in the 2.1.a-2.3.a rows. | Low — the blueprint-not-manifest rule covers it, but the inconsistency with earlier rows reads as deliberate rather than an oversight. | Applied — the 2.4.a row is amended with all three and their reasons. Not a harness-log row: plan.md is an application artifact, not a harness artifact. |

---

## Patterns to Capture

- **Pattern:** Deep-copy of an owned collection in the aggregate copy constructor
  **Observed in:** `.../execution/aggregate/Execution.java`
  **Description:** First owned collection in this application. The copy constructor must deep-copy per element (`other.getStudents().stream().map(ExecutionStudent::new)`); a shared list reference across copy-on-write versions combined with `orphanRemoval = true` would delete rows out from under the previous version.

- **Pattern:** Straddle vs single-representative test inputs for two P1 rules over the same collection
  **Observed in:** `.../sagas/execution/ExecutionIntraInvariantTest.groovy`
  **Description:** A `state == DELETED ⟹ collection.isEmpty()` rule is an ordered-domain collection-size predicate, so it takes the BVA straddle (deleted + 0 elements → `notThrown`; deleted + 1 element → `thrown`), while a sibling uniqueness rule on the same collection takes a single representative pair. The same shape recurs verbatim for `TOURNAMENT_DELETE` / `TOURNAMENT_UNIQUE_AS_PARTICIPANT` in aggregate 8.

- **Pattern:** Immutable snapshot fields as Java `final`
  **Observed in:** `.../execution/aggregate/Execution.java`, `.../execution/aggregate/ExecutionStudent.java`
  **Description:** Snapshot fields for a reference marked immutable in domain-model §2 go in Java `final` fields (Topic precedent), which also discharges T1 coverage for them per `docs/concepts/testing.md` § T1.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: 29, 30

For each `fixed` row, the `harness:` commit sha:

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 29 | 1 | fixed | `fda4dd45` |
| 30 | 2 | fixed | `9dde2a4c` |

---

## One-Line Summary

The first owned collection in the application exposed two latent defects in session-a.md — a JPA ownership statement that contradicts the annotation it prescribes, and silence on which session owns collection add/remove helpers — both of which the slice worked around silently and reported only in its retro, not through the friction channel.
