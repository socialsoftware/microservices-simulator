# Retro — 2.6.b — Quiz

**App:** quizzes-full-2
**Session:** 2.6.b (Read Functionalities)
**Date:** 2026-08-07

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/ServiceMapping.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/aggregate/QuizRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/aggregate/QuizCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/aggregate/sagas/repositories/QuizCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/service/QuizService.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/quiz/GetQuizByIdCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/quiz/GetQuizzesForExecutionCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/coordination/sagas/GetQuizByIdFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/coordination/sagas/GetQuizzesForExecutionFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/coordination/functionalities/QuizFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/messaging/QuizCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/quiz/QuizServiceTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/quiz/GetQuizByIdTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/quiz/GetQuizzesForExecutionTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md`

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `.claude/skills/_shared/conventions.md` | § Anchor to the repository root, § Resolve app context, § Application isolation, § Harness log, § Run the test suite | Yes | — |
| `.claude/skills/implement-aggregate/session-b.md` | whole file | Yes | Fixture-helper contract covered the `createQuiz` signature decision. |
| `docs/concepts/service.md` | § Injected Dependencies, § Method Patterns → Read method, § Custom Repository — Latest-Active-Version Query | Yes | The closed dependency list settled dropping `AggregateIdGeneratorService` from the read-only service. |
| `docs/concepts/sagas.md` | § Read Functionality Sagas, § List-return read variant, § Two-step read saga variant | Yes | Two-step variant read and correctly rejected: `executionAggregateId` is stored on the quiz's own `QuizExecution` entity, so one step suffices. |
| `docs/concepts/testing.md` | § Assertion Ownership, § T2 — Service Test (incl. § Not-Found Paths), § T4 — Functionality Test, § Spec-First Ordering | Yes | — |
| `.claude/skills/_shared/session-completion.md` | whole file | Yes | — |
| `AGENTS.md` | § Harness evolution, § Build Commands | Yes | — |

---

## Skill Instructions Feedback

### What worked well

- session-b.md § "Update `{AppClass}SpockTest.groovy`" states the signature-is-a-contract rule with
  2.{N}.c explicitly. That decided two open points on its own: `creationDate` stays out of the
  `createQuiz` signature because `CreateQuiz` does not take it, and `executionAggregateId` leads the
  parameter list, undefaulted, minted by `createExecution`.
- The one-step vs two-step read-saga decision box named the exact discriminator needed here (is the
  filter criterion stored on the aggregate?), so no inference was required for
  `GetQuizzesForExecution`.
- service.md § "Custom Repository — Latest-Active-Version Query" made the JPQL obligation
  unambiguous for the list read; the only local judgement was navigating into the owned entity
  (`q.execution.executionAggregateId`), which JPQL handles directly.

### What was unclear or missing

- session-b.md § "Fixture state a create cannot reach" forbids widening `create{Aggregate}` with a
  collection, while `CreateQuiz` genuinely takes `questionAggregateIds` as a create parameter. The
  section reads as a blanket ban but is really about collections the create functionality cannot
  populate. Resolved by leaving the parameter off entirely (no session-`b` read needs quiz
  questions), which matches what 2.5.b did for `createQuestion`/`topicAggregateIds` — 2.5.d added the
  parameter later as a trailing default, and every call site survived. Not logged as harness friction
  because the section's rationale paragraph does settle it on a careful read.

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

- **Pattern:** Latest-active-version JPQL filtering on a field of an owned entity
  **Observed in:** `applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quiz/aggregate/QuizRepository.java`
  **Description:** service.md's JPQL template filters on a scalar field of the aggregate. When the
  filter value is a foreign-aggregate id cached inside an owned entity (a one-to-one snapshot), the
  predicate navigates into it (`q.execution.executionAggregateId = :id`) while the
  `max(version)`-per-`aggregateId` subquery stays unchanged. Still a one-step read saga, since the
  criterion is on the aggregate itself.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: none

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| — | — | — | — |

---

## One-Line Summary

Quiz's two reads were fully determined by the existing docs — the only judgement call was keeping
`createQuiz` minimal so 2.6.c can swap its body for the real create functionality without touching a
call site.
