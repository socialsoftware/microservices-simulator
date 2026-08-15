# Retro — 2.7.a — QuizAnswer

**App:** quizzes-full-2
**Session:** 2.7.a (Domain Layer)
**Date:** 2026-08-07

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/aggregate/QuizAnswer.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/aggregate/QuizAnswerQuiz.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/aggregate/QuizAnswerStudent.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/aggregate/QuizAnswerExecution.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/aggregate/QuestionAnswer.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/aggregate/QuestionAnswerDto.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/aggregate/QuizAnswerDto.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/aggregate/QuizAnswerFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/aggregate/QuizAnswerCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/aggregate/QuizAnswerRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/aggregate/sagas/SagaQuizAnswer.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/aggregate/sagas/states/QuizAnswerSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/aggregate/sagas/factories/SagasQuizAnswerFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/aggregate/sagas/repositories/QuizAnswerCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/QuizAnswerServiceApplication.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/exception/QuizzesFull2ErrorMessage.java` (appended two constants)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/quizanswer/QuizAnswerIntraInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy` (appended domain constants)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy` (appended two beans)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md` (ticked 2.7.a)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/aggregate.md` | § Key Fields, § Base Class, § Variants (Sagas), § Factories, § Repositories, § getEventSubscriptions() | Yes | R6 (no repository reads in `verifyInvariants()`) settled why `COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION` stays P3 rather than being re-litigated here |
| `docs/concepts/testing.md` | § T1 — Aggregate Test, § Choosing Input Values — EP & BVA, § Fake/Wrong/Weak, § Spec-First Ordering | Yes | The categorical carve-out under § Choosing Input Values directly settled that neither P1 rule here gets a boundary straddle |
| `.claude/skills/implement-aggregate/session-a.md` | all sections | Yes | — |
| `.claude/skills/_shared/conventions.md` | § Anchor to repo root, § Resolve app context, § Application isolation, § Run the test suite | Yes | — |
| `.claude/skills/_shared/session-completion.md` | all sections | Yes | — |
| `AGENTS.md` | § Harness evolution | Yes | No friction reached the gate |

---

## Skill Instructions Feedback

### What worked well

- `session-a.md` § "Owned entity classes" states the bidirectional `@OneToOne` contract precisely enough to apply three times over on one aggregate without ambiguity: `mappedBy` names the back-reference field **on the entity**, and the installer wires `entity.set{Aggregate}(this)`. `QuizAnswer` holds three single subscribing snapshots (`QuizAnswerQuiz`, `QuizAnswerStudent`, `QuizAnswerExecution`), each with its own `quizAnswer` back-reference, and the persistence unit initialised first try.
- The `**Saga states:**` transcription rule removed all guesswork: `IN_ANSWER_QUESTION` / `IN_CONCLUDE_QUIZ` were taken verbatim, including the stated exclusion of a state for `CreateQuizAnswer`.
- The plan.md snapshot-class blockquote ("§1's 'stored directly on the aggregate' does not exempt a **subscribing** single snapshot from having an owned-entity class") pre-empted exactly the wrong call this session would otherwise have made for the three single references.

### What was unclear or missing

- `session-a.md` § "Verify Mandatory Files in plan.md" implies the factory and custom-repository interfaces are the only mandatory-file check. `{Aggregate}Repository.java` is equally mandatory (`{Aggregate}CustomRepositorySagas` autowires it and will not compile without it), but is not in that check. plan.md listed it here, so nothing broke.

### Suggested wording / structure changes

- (none)

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-a.md` | § "Verify Mandatory Files in plan.md" names two mandatory interfaces but omits `{Aggregate}Repository.java`, which `{Aggregate}CustomRepositorySagas` autowires unconditionally | Low | Add the third bullet to that section's list |

---

## Patterns to Capture

- **Pattern:** Multiple single subscribing snapshots on one aggregate
  **Observed in:** `.../quizanswer/aggregate/QuizAnswer.java`
  **Description:** An aggregate may hold more than one single-reference snapshot that subscribes to events. Each becomes its own `@OneToOne(cascade = ALL, orphanRemoval = true, mappedBy = "{aggregate}")` field with a matching entity class, and every entity class declares a back-reference field with the **same** name (`quizAnswer`) — distinct classes, so no collision. Where the reference is immutable, one private installer per snapshot wires the back-reference from both constructors, so the "no public setter" immutability guarantee and the "back-reference wired before persist" requirement hold together.

- **Pattern:** Null-guarded predicate over a partially-populated owned collection
  **Observed in:** `.../quizanswer/aggregate/QuizAnswer.java` (`answerMatchesCorrectOption`)
  **Description:** A create functionality may seed owned entities in an incomplete state that a later write completes (`QuestionAnswer` rows are seeded with `correctOptionKey` only, and gain `optionKey`/`correct` at answer time). The P1 predicate filters on the field that marks completion (`optionKey != null`) before comparing, so the invariant is vacuously true for the seeded state and binding once answered. The T1 file needs a third case beyond the violation/satisfying pair: the guarded-out state itself.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: none

For each `fixed` row, the `harness:` commit sha:

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| (none) | - | - | - |

---

## One-Line Summary

The domain layer landed with no harness friction; the only judgement call was making `answerDate` a Java `final` field on the strength of the domain model's "(immutable)" annotation, since §3.1's immutability blockquote names `QUIZANSWER_FINAL_CREATION_DATE` but not an `answerDate` counterpart.
