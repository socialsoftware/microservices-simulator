# Retro — 2.7.a — QuizAnswer

**App:** quizzes-full
**Session:** 2.7.a (Domain Layer)
**Date:** 2026-05-16

---

## Files Produced

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/aggregate/QuizAnswer.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/aggregate/QuestionAnswer.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/aggregate/sagas/SagaQuizAnswer.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/aggregate/sagas/states/QuizAnswerSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/aggregate/sagas/factories/SagasQuizAnswerFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/aggregate/sagas/repositories/QuizAnswerCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/aggregate/QuizAnswerCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/aggregate/QuizAnswerFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/aggregate/QuizAnswerDto.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/aggregate/QuizAnswerRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/quizanswer/QuizAnswerServiceApplication.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/quizanswer/QuizAnswerTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/exception/QuizzesFullErrorMessage.java` (modified — added QUESTION_ALREADY_ANSWERED constant)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/BeanConfigurationSagas.groovy` (modified — added QuizAnswer beans)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/plan.md` (modified — ticked 2.7.a, patched file table)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/aggregate.md` | § Key Fields, § Variants/Sagas, § Factories, § Repositories, § getEventSubscriptions() | Yes | — |
| `docs/concepts/testing.md` | § T1 — Creation Test | Yes | — |

---

## Skill Instructions Feedback

### What worked well

- The session-a.md sub-file clearly specifies when `getEventSubscriptions()` returns `new HashSet<>()` at session a, deferring real subscriptions to session d.
- Owned entity pattern with `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)` was straightforward.
- Plan.md file list, immutability note, and P1/P3 rule classifications were unambiguous.

### What was unclear or missing

- `QuizAnswerCustomRepository.java` (interface) and `QuizAnswerFactory.java` (interface) were not listed in the plan.md 2.7.a file table, but are required by their respective sagas implementations. This is a recurring pattern gap — the two interface files should always accompany their sagas implementations in the plan.

### Suggested wording / structure changes

- session-a.md `{Aggregate}CustomRepository.java` and `{Aggregate}Factory.java` sections already describe producing these files, but they are often omitted from the generated plan.md table by /classify-and-plan. The /classify-and-plan skill or session-a.md should note these as mandatory and ensure they appear in plan.md.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-a.md` | Does not mention that `{Aggregate}CustomRepository.java` and `{Aggregate}Factory.java` interfaces must be explicitly added to plan.md table if absent | Medium | Add a note in the "Patch plan.md for Missing Files" equivalent within session-a.md to always verify both interface files are listed |

---

## Patterns to Capture

- **Pattern:** QuizAnswer stores snapshot fields for User (userName, userUsername, userVersion) even though creationDate and answerDate are both immutable. The snapshot user fields are mutable to support UpdateStudentName/Anonymize events.
  **Observed in:** `QuizAnswer.java`
  **Description:** Aggregates that subscribe to update events on a referenced entity must cache the mutable fields that can change (name, username), even if the reference itself (userAggregateId) is immutable.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| Medium | `.claude/skills/classify-and-plan/` or plan template | Ensure `QuizAnswerCustomRepository.java` and `QuizAnswerFactory.java` (interface files) are always listed in the 2.N.a file table |

---

## One-Line Summary

Both interface files (`QuizAnswerCustomRepository`, `QuizAnswerFactory`) were missing from plan.md and had to be added as unambiguous omissions; T1 test passes.
