# Retro — 2.5.b — Question

**App:** quizzes-full-2
**Session:** 2.5.b (Read Functionalities)
**Date:** 2026-08-06

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/ServiceMapping.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/service/QuestionService.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/messaging/QuestionCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/coordination/functionalities/QuestionFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/coordination/sagas/GetQuestionByIdFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/coordination/sagas/GetQuestionsByCourseFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/question/GetQuestionByIdCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/question/GetQuestionsByCourseCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/aggregate/QuestionRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/aggregate/QuestionCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/aggregate/sagas/repositories/QuestionCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/question/QuestionServiceTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/question/GetQuestionByIdTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/question/GetQuestionsByCourseTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md`

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/architecture.md` | (not re-read; package layout already established by aggregates 1-4 in this app) | Yes | — |
| `docs/concepts/service.md` | § Injected Dependencies, § Method Patterns → Read method, § Custom Repository — Latest-Active-Version Query | Yes | The by-course bulk read mapped directly onto the JPQL pattern. |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § File Location, § ServiceMapping Enum, § Routing Commands | Yes | The row-26 callout settled `rootAggregateId = null` for `GetQuestionsByCourseCommand` with no inference. |
| `docs/concepts/sagas.md` | § Read Functionality Sagas, § List-return read variant | Yes | One-step form applies: `courseAggregateId` is a field on `Question`, so no foreign-id resolution step. |
| `docs/concepts/testing.md` | § Test Taxonomy, § Assertion Ownership, § Fake/Wrong/Weak, § Spec-First Ordering, § T2 — Service Test, § T2 Not-Found Paths, § T4 — Functionality Test | Yes | — |
| `.claude/skills/_shared/conventions.md` | § Anchor to repository root, § Resolve app context, § Application isolation, § Harness log, § Run the test suite | Yes | — |

---

## Skill Instructions Feedback

### What worked well

- § "Update `{AppClass}SpockTest.groovy`"'s signature contract (rows 24/27/31/33 accumulated) was
  decisive for `createQuestion`: `courseAggregateId` required and leading, `title`/`content`
  defaulted to constants. It also made the `creationDate` decision obvious - the field is stamped by
  the create functionality rather than passed by the caller, so it stays out of the signature and
  the tests assert `creationDate != null` instead of the fixture constant, which would break on the
  2.5.c body swap.
- § "Fixture state a create cannot reach" was correctly *not* triggered: `GetQuestionsByCourse`
  filters on `Question.courseAggregateId`, an own scalar field, not on a field of an owned
  collection, so no sibling helper was needed. The section's trigger condition is stated precisely
  enough to rule itself out.
- The § "Update BeanConfigurationSagas.groovy" allowance to omit `AggregateIdGeneratorService` from
  a read-only service applied cleanly; `QuestionService` takes three dependencies and 2.5.c widens
  both the constructor and the `@Bean` method when `createQuestion` needs an id.

### What was unclear or missing

- (none - no instruction required inference this session)

### Suggested wording / structure changes

- (none)

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| (none) | — | — | — |

---

## Patterns to Capture

- (none new - every pattern used this session is already stated in `service.md`, `sagas.md`,
  `commands.md` or `session-b.md`)

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: none

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| (none) | — | — | — |

---

## One-Line Summary

The read-side harness has converged: aggregate 5's session `b` ran end to end with no Type 1 fix, no
Type 2 halt and no inference, the accumulated fixture-signature rules (rows 24/27/31/33) answering
every decision the session posed.
