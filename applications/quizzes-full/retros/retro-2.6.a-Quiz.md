# Retro — 2.6.a — Quiz

**App:** quizzes-full
**Session:** 2.6.a (Domain Layer)
**Date:** 2026-05-11

---

## Files Produced

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/aggregate/Quiz.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/aggregate/QuizExecution.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/aggregate/QuizQuestion.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/aggregate/QuizType.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/aggregate/QuizFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/aggregate/QuizCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/aggregate/QuizRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/aggregate/QuizDto.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/aggregate/sagas/SagaQuiz.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/aggregate/sagas/states/QuizSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/aggregate/sagas/factories/SagasQuizFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/aggregate/sagas/repositories/QuizCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quiz/QuizServiceApplication.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/exception/QuizzesFullErrorMessage.java` (modified)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/quiz/QuizTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/BeanConfigurationSagas.groovy` (modified)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/plan.md` (modified)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/aggregate.md` | § Key Fields, § Sagas variant, § Factories, § Repositories, § getEventSubscriptions() | Yes | — |
| `docs/concepts/testing.md` | § T1 Creation Test | Yes | — |
| `quizzes-full-domain-model.md` | §1 Entities (Quiz row), §3 Rules (QUIZ_DATE_ORDERING, QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE) | Yes | — |
| `quizzes-full-aggregate-grouping.md` | §2 Snapshots (Quiz row) | Yes | — |

---

## Skill Instructions Feedback

### What worked well

- The session-a.md file list pattern was clear: produce all domain-layer artefacts before moving on.
- The retro for 2.5.d clarified that `getEventSubscriptions()` is a stub (empty set) in session a and is filled in session d — no ambiguity needed.
- `QuizExecution` and `QuizQuestion` as companion entities followed the `ExecutionCourse` and `QuestionTopic` patterns exactly.

### What was unclear or missing

- session-a.md still does not explicitly instruct producing the `QuizFactory.java` (interface) and `QuizCustomRepository.java` (interface) files; these are required to compile `SagasQuizFactory` and `QuizCustomRepositorySagas` but are not listed in plan.md. Step 5b (patch plan.md) caught them, but the instruction could be more explicit.
- session-a.md says `getEventSubscriptions()` should be implemented, but the subscription classes don't exist until session d. The correct behaviour (stub with empty set) was inferred from the 2.5.d retro, not from the session-a.md instructions.

### Suggested wording / structure changes

- session-a.md should explicitly note: "In session a, `getEventSubscriptions()` must return an empty `HashSet` — it will be updated in session d once the subscribe classes are created."
- session-a.md should list `{Aggregate}Factory.java` (interface) and `{Aggregate}CustomRepository.java` (interface) as required files, since they are needed to compile the sagas implementations.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-a.md` | `getEventSubscriptions()` stub pattern not documented — session d modifies the domain aggregate to add subscribe references | Medium | Add note: "Return empty HashSet in session a; session d will add subscribe class references." |
| `.claude/skills/implement-aggregate/session-a.md` | `{Aggregate}Factory.java` and `{Aggregate}CustomRepository.java` interfaces not mentioned as required artefacts | Medium | Add them to the "Produce" section as required companion files alongside `Sagas{Aggregate}Factory` and `{Aggregate}CustomRepositorySagas` |

---

## Patterns to Capture

- **Pattern:** `lastModifiedTime` stamp in setters for temporal invariants
  **Observed in:** `Quiz.java`
  **Description:** Aggregates with "frozen after date X" invariants (QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE, TOURNAMENT_FINAL_AFTER_START) must stamp `lastModifiedTime = LocalDateTime.now()` in every mutating setter so that `verifyInvariants()` can compare without calling `now()` directly, preserving idempotency across TCC merges.

- **Pattern:** `prev`-based invariant checks require `instanceof` cast
  **Observed in:** `Quiz.java` `quizFieldsFinalAfterAvailableDate()`
  **Description:** When `verifyInvariants()` reads fields from `getPrev()`, it must cast with an `instanceof` guard because `prev` is typed as `Aggregate`. Missing this guard causes a `ClassCastException` when the copy-constructor chain introduces a different subtype.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| High | `.claude/skills/implement-aggregate/session-a.md` | Add explicit note: getEventSubscriptions() must return empty HashSet in session a; session d modifies it |
| High | `.claude/skills/implement-aggregate/session-a.md` | Add {Aggregate}Factory.java and {Aggregate}CustomRepository.java interfaces to the required artefacts list |
| Medium | `.claude/skills/classify-and-plan` | Include companion entity classes (QuizExecution, QuizQuestion) and interface files in plan.md session-a rows |

---

## One-Line Summary

Quiz has two non-trivial P1 invariants (date ordering and frozen-after-available-date) requiring a `lastModifiedTime` technical stamp pattern; `getEventSubscriptions()` is correctly stubbed empty in session a pending session d.
