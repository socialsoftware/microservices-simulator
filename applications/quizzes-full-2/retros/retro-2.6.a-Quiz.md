# Retro — 2.6.a — Quiz

**App:** quizzes-full-2
**Session:** 2.6.a (Domain Layer)
**Date:** 2026-08-07

---

## Files Produced

List every file created or modified this session (absolute paths).

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/aggregate/Quiz.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/aggregate/QuizType.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/aggregate/QuizExecution.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/aggregate/QuizQuestion.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/aggregate/QuizQuestionDto.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/aggregate/QuizDto.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/aggregate/QuizFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/aggregate/QuizCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/aggregate/QuizRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/aggregate/sagas/SagaQuiz.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/aggregate/sagas/states/QuizSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/aggregate/sagas/factories/SagasQuizFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/aggregate/sagas/repositories/QuizCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/QuizServiceApplication.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/exception/QuizzesFull2ErrorMessage.java` (appended)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/quiz/QuizIntraInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy` (appended)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy` (appended)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md` (2.6.a row amended, checkbox ticked)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/harness-log.md` (row 51)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/aggregate.md` | § Key Fields, § Base Class, § Variants → Sagas, § Factories, § Repositories, § getEventSubscriptions() | Yes | `prev` is documented under § Key Fields, which is what the temporal invariant needed |
| `docs/concepts/testing.md` | § T1 — Aggregate Test, § Choosing Input Values — EP & BVA, § Fake/Wrong/Weak | Yes | The temporal-mechanics note (`plusNanos(1)`, pin both instants) drove all six date-ordering boundary cases |
| `.claude/skills/implement-aggregate/session-a.md` | all sections | Partial | See row 51: the `mappedBy` placeholder named the wrong side of the association |
| `.claude/skills/_shared/conventions.md` | § Anchor to root, § Resolve app context, § Application isolation, § Harness log, § Run the test suite | Yes | — |
| `.claude/skills/_shared/session-completion.md` | all sections, single-agent mode | Yes | — |

---

## Skill Instructions Feedback

### What worked well

- § "`{Aggregate}SagaState.java`" telling the session to transcribe the `**Saga states:**` line verbatim: `IN_UPDATE_QUIZ` alone is not derivable in session `a`, since the guard that references it lives in `UpdateTournamentFunctionalitySagas` (aggregate 8).
- The plan.md § "Snapshot class decisions" note for session-`a` agents pre-empted the domain-model §1 sentence that says single-reference snapshots are "stored directly on the aggregate" — without it `QuizExecution` would have been flattened into two plain fields and the 2.6.d subscription would have had no anchor entity.
- § "Error message constants" plus testing.md's `ex.message == <RULE_NAME>` form left no ambiguity about how the two P1 rules surface in T1.

### What was unclear or missing

- § "`{Aggregate}.java`" mandates getters and setters for all mutable fields, and is silent on fields whose immutability the domain model enforces by *absence of a setter* rather than by `final` (`QUIZ_COURSE_EXECUTION_FINAL`). A bidirectional `@OneToOne` cannot be `final` in practice — the back-reference wiring wants a single install point — so `Quiz.execution` got a private installer called from both constructors. This is a reading of the rule, not something the skill states.
- Neither session-a.md nor testing.md says which mutators stamp the technical `lastModifiedTime` field that a temporal P1 rule reads. plan.md §3.1 says "setters stamp `lastModifiedTime`, copy constructor bypasses them", which settled it here, but that is application spec, not harness guidance — the next application with a temporal freeze gets no rule.

### Suggested wording / structure changes

- `session-a.md` § "`{Aggregate}.java`": add one line on temporal-freeze fields — the stamp is written by the mutating setters and read (never re-derived) by `verifyInvariants()`, and the field keeps a plain unstamping setter so T1 can pin the boundary instant.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-a.md` | No guidance on where the stamp of a technical `lastModifiedTime`-style field is written, given that the predicate reading it may not call the clock | Medium | Add the one-liner proposed above under § "`{Aggregate}.java`" |
| `.claude/skills/implement-aggregate/session-a.md` | "Getters and setters for all mutable fields" does not cover a field whose immutability is enforced by omitting the setter | Low | Note that a domain-model immutability rule enforced by "absence of setters" is satisfied by a constructor-only install, and that this is the expected shape for an immutable single snapshot |

---

## Patterns to Capture

- **Pattern:** Membership-only comparison in a freeze predicate
  **Observed in:** `applications/quizzes-full-2/.../quiz/aggregate/Quiz.java` (`questionAggregateIds()`)
  **Description:** When a P1 freeze covers an owned collection whose elements are snapshots refreshed by a P2 event handler, the predicate must compare element *identity* (the cached aggregate ids), not element equality. Comparing whole elements would make the freeze fire on every legal event-driven refresh after the freeze point, and the failure would only appear in a session-`d` T3 test.

- **Pattern:** Immutable single snapshot installed by constructor
  **Observed in:** `applications/quizzes-full-2/.../quiz/aggregate/Quiz.java` (`setExecution` private)
  **Description:** A bidirectional `@OneToOne` snapshot whose reference is immutable is installed through one private method that both constructors call, so `entity.set{Aggregate}(this)` cannot be forgotten on the copy-on-write path, and no public setter exists to violate the immutability rule.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: 51

For each `fixed` row, the `harness:` commit sha:

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 51 | 1 | fixed | `817fb30fc` |

---

## One-Line Summary

The first single subscribing snapshot in this application exposed a `mappedBy` placeholder in session-a.md that named the aggregate's own field instead of the entity's back-reference, which would have failed persistence-unit init for every application that reached this shape.
