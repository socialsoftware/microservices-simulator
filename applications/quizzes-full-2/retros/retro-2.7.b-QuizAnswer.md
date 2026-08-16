# Retro — 2.7.b — QuizAnswer

**App:** quizzes-full-2
**Session:** 2.7.b (Read Functionalities)
**Date:** 2026-08-08

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/ServiceMapping.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/exception/QuizzesFull2ErrorMessage.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/aggregate/QuizAnswerRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/aggregate/QuizAnswerCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/aggregate/sagas/repositories/QuizAnswerCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/service/QuizAnswerService.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/messaging/QuizAnswerCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/coordination/functionalities/QuizAnswerFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/coordination/sagas/GetQuizAnswerByIdFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/quizanswer/coordination/sagas/GetQuizAnswerForStudentAndQuizFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/quizanswer/GetQuizAnswerByIdCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/quizanswer/GetQuizAnswerForStudentAndQuizCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/quizanswer/QuizAnswerServiceTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/quizanswer/GetQuizAnswerByIdTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/quizanswer/GetQuizAnswerForStudentAndQuizTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md`

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `.claude/skills/_shared/conventions.md` | § Anchor to the repository root, § Resolve app context, § Application isolation, § Harness log, § Run the test suite | Yes | — |
| `AGENTS.md` | § Harness evolution | Yes | — |
| `.claude/skills/implement-aggregate/session-b.md` | whole file | Yes | The fixture-helper signature contract with 2.{N}.c drove the `createQuizAnswer(quizAggregateId, userAggregateId, executionAggregateId)` parameter list directly. |
| `docs/architecture.md` | § Package Structure Convention, § R1-R3, R7, R8 | Yes | — |
| `docs/concepts/service.md` | § Injected Dependencies, § Method Patterns → Read method, § Custom Repository — Latest-Active-Version Query, § Exception-Throw Convention | Yes | The "omit `AggregateIdGeneratorService` when no read needs it" instruction settled the constructor shape. |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § File Location, § ServiceMapping Enum, § Routing Commands | Partial | The `rootAggregateId = null` guidance is written for a *bulk* read filtered by a non-PK field; the same reasoning applies to a single-result non-PK read, but the doc only names the bulk case. |
| `docs/concepts/sagas.md` | § Read Functionality Sagas, § Two-step read saga variant | Yes | The one-step / two-step decision rule resolved the `GetQuizAnswerForStudentAndQuiz` shape without inference. |
| `docs/concepts/testing.md` | § Test Taxonomy, § Assertion Ownership, § T2 — Service Test (incl. § Not-Found Paths), § T4 — Functionality Test, § Fake/Wrong/Weak, § Spec-First Ordering | Yes | — |

---

## Skill Instructions Feedback

### What worked well

- `session-b.md` § "Fixture state a create cannot reach" and the surrounding signature-contract text made the `createQuizAnswer` helper unambiguous: foreign-aggregate ids required and leading, own stamped fields (creationDate, answerDate) kept out of the signature because `CreateQuizAnswer` does not take them either.
- The Path A / Path B rule of thumb in `testing.md` § Not-Found Paths mapped one-to-one onto the two reads: `GetQuizAnswerById` is Path A, `GetQuizAnswerForStudentAndQuiz` is Path B, and the T2 exception types follow mechanically.
- The one-step vs two-step read-saga decision box in `session-b.md` covered the case where the domain model lists a foreign aggregate ("reads Quiz") but the filter value is already cached on the primary aggregate: one step, no upstream fetch.

### What was unclear or missing

- `session-b.md` § Not-found Path B says to throw "the domain-specific not-found constant" but does not say who declares it when the domain model lists no such constant. The constant (`QUIZ_ANSWER_NOT_FOUND`) was added to the shared error-message class in this session; the skill is silent on whether that belongs to session `a` or `b`. Session `b` was the first session to need it, so it landed here.

### Suggested wording / structure changes

- `docs/concepts/commands.md` § What a Command Is: widen the `rootAggregateId = null` note from "a bulk read filtered by a non-PK field" to any read whose filter is not the primary key, single-result reads included. The justification given (no semantic lock, no forbidden states, so the handler never dereferences it) already covers both.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/commands.md` | `rootAggregateId = null` is stated only for *bulk* non-PK reads; a single-result non-PK read has no stated rule | Low | Generalise the note to any non-PK-filtered read |
| `.claude/skills/implement-aggregate/session-b.md` | Path B names a "domain-specific not-found constant" without saying which session declares it when the domain model does not | Low | State that the first session needing it adds it to the shared `{App}ErrorMessage` |

---

## Patterns to Capture

- **Pattern:** Composite-key read implemented as custom-repo id lookup plus PK load
  **Observed in:** `microservices/quizanswer/service/QuizAnswerService.java`
  **Description:** A Path B read resolves the `(foreignId, foreignId)` pair to a single aggregate id via the custom repository's `Optional<Integer>`, throws the not-found constant when empty, and then delegates to the aggregate's own `get{Aggregate}ById` so the UoW read registration goes through exactly one code path. Keeps the `aggregateLoadAndRegisterRead` call site unique per aggregate.

- **Pattern:** Fixture helper seeds snapshots from the real upstream aggregates
  **Observed in:** `QuizzesFull2SpockTest.createQuizAnswer`
  **Description:** Where the aggregate caches names/versions of upstream aggregates, the session-`b` fixture reads those upstream aggregates through their services and copies the real values into the constructor, instead of using constants. Cached-field assertions written this session then survive the 2.{N}.c swap to the real create functionality unchanged.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: none

For each `fixed` row, the `harness:` commit sha:

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| — | — | — | — |

---

## One-Line Summary

The two QuizAnswer reads split cleanly across the documented Path A / Path B and one-step / two-step decision rules, so the session needed no harness repair; the only friction was two low-impact silences about who declares a Path B not-found constant and whether the `rootAggregateId = null` rule covers single-result non-PK reads.
