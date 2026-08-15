# Retro — 2.5.a — Question

**App:** quizzes-full
**Session:** 2.5.a (Domain Layer)
**Date:** 2026-05-03

---

## Files Produced

- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/aggregate/Question.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/aggregate/Option.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/aggregate/QuestionCourse.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/aggregate/QuestionTopic.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/aggregate/QuestionFactory.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/aggregate/QuestionCustomRepository.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/aggregate/QuestionRepository.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/aggregate/QuestionDto.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/aggregate/sagas/SagaQuestion.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/aggregate/sagas/states/QuestionSagaState.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/aggregate/sagas/factories/SagasQuestionFactory.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/aggregate/sagas/repositories/QuestionCustomRepositorySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/question/QuestionServiceApplication.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/question/QuestionTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/BeanConfigurationSagas.groovy` (modified)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/plan.md` (modified)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/aggregate.md` | § Key Fields, § Sagas variant, § Factories, § Repositories, § getEventSubscriptions() | Yes | — |
| `docs/concepts/testing.md` | § T1 Creation Test | Partial | Contradiction with session-a.md on invariant-violation tests (see below) |
| `.claude/skills/implement-aggregate/session-a.md` | All sections | Yes | — |

---

## Skill Instructions Feedback

### What worked well

- The file list in plan.md (even though incomplete) correctly identifies the core aggregate files.
- The pattern for QuestionCourse (mirroring TopicCourse) and QuestionTopic (mirroring ExecutionStudent) was clear from existing implementations.
- No P1 intra-invariants for Question made verifyInvariants() trivially empty.

### What was unclear or missing

- **Contradiction between session-a.md and testing.md on invariant-violation tests**: session-a.md says to add invariant-violation tests that call `verifyInvariants()` directly; testing.md says "Never call `verifyInvariants()` directly." Since Question has no P1 intra-invariants, the contradiction was moot here, but it remains unresolved.
- session-a.md doesn't explicitly say to add companion entity classes (QuestionCourse, QuestionTopic) to the plan.md. It mentions step 5b patching, but the decision criteria ("unambiguous omissions") required inference.

### Suggested wording / structure changes

- Clarify which instruction takes precedence when session-a.md and testing.md contradict each other on `verifyInvariants()` testing.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/testing.md` | Contradiction with session-a.md: T1 says never call verifyInvariants() directly, but session-a.md says to add invariant-violation tests using it | Medium | Align the two documents; clarify whether T1 tests should call verifyInvariants() directly |

---

## Patterns to Capture

- **Pattern:** Companion entity classes for cross-aggregate references always omitted from plan.md file table
  **Observed in:** `plan.md` row for 2.5.a
  **Description:** QuestionCourse and QuestionTopic are always required to compile Question.java, but plan.md never lists them. The plan template should be updated to always include these companion classes in the 2.N.a row, or session-a.md should explicitly instruct adding them.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| High | `.claude/skills/implement-aggregate/session-a.md` | Resolve contradiction with testing.md on verifyInvariants() direct calls in T1 |
| Medium | `.claude/skills/classify-and-plan` | Include companion entity classes (XxxCourse, XxxTopic) in plan.md file table for session a rows |

---

## One-Line Summary

Question has no P1 intra-invariants so verifyInvariants() is empty; the main implementation decision was recognizing QuestionCourse and QuestionTopic as required companion entities omitted from plan.md.
