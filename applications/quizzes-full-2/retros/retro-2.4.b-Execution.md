# Retro — 2.4.b — Execution

**App:** quizzes-full-2
**Session:** 2.4.b (Read Functionalities)
**Date:** 2026-08-04

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/service/ExecutionService.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/messaging/ExecutionCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/execution/GetExecutionByIdCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/execution/GetExecutionsCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/execution/GetUserExecutionsCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/coordination/sagas/GetExecutionByIdFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/coordination/sagas/GetExecutionsFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/coordination/sagas/GetUserExecutionsFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/coordination/functionalities/ExecutionFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/execution/ExecutionServiceTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/execution/GetExecutionByIdTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/execution/GetExecutionsTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/execution/GetUserExecutionsTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/ServiceMapping.java` (appended: `EXECUTION("execution")`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/aggregate/ExecutionRepository.java` (appended: `findAllLatestActive`, `findAllLatestActiveByStudent`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/aggregate/ExecutionCustomRepository.java` (appended: `findAllExecutionIds`, `findExecutionIdsByStudent`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/execution/aggregate/sagas/repositories/ExecutionCustomRepositorySagas.java` (appended: the two implementations)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy` (appended: `ExecutionService` / `ExecutionFunctionalities` fields, `createExecution`, `enrollStudentInExecution`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy` (appended: `executionService`, `executionCommandHandler`, `executionFunctionalities` beans)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md` (2.4.b row amended for the five omitted files; 2.4.b checkbox ticked)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/harness-log.md` (rows 31, 32)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `.claude/skills/_shared/conventions.md` | § Anchor to the repository root, § Application isolation, § Harness log, § Run the test suite | Yes | The narrow-form caveat about stale surefire reports was exactly right: the aggregation script summed 88 tests for a 10-test run. |
| `.claude/skills/implement-aggregate/session-b.md` | whole file, including the new § "Fixture state a create cannot reach" | No | The section as it stood at the start of the session had no concept of a fixture whose required state is a collection — the slice halted on it (row 31). Its replacement code block then failed to compile against the session's own service surface (row 32). Both now fixed. |
| `.claude/skills/implement-aggregate/session-c.md` | § create-helper replacement | Partial | Read only to check the forward contract; its single-call template is what made the "widen the create helper" option self-contradictory. Now amended to cover sibling helpers. |
| `docs/concepts/service.md` | § Injected Dependencies, § Method Patterns → Read, § Custom Repository — Latest-Active-Version Query | Partial | The closed five-item dependency list with its "omit any the service does not use" clause made dropping `AggregateIdGeneratorService` and `ExecutionRepository` from the constructor unambiguous. No JPQL shape given for the owned-collection join case. |
| `docs/concepts/commands.md` | § What a Command Is (incl. the bulk-read-`null` clause), § Naming Conventions, § File Location, § ServiceMapping, § Routing Commands | Yes | The row-26 callout ("do not pass the filter value even when it is a foreign aggregate's id") decided `GetUserExecutionsCommand`'s `super(...)` call outright. |
| `docs/concepts/sagas.md` | § Read Functionality Sagas, § List-return read variant, § Two-step read saga variant | Yes | Two-step correctly ruled out: `userAggregateId` is already stored on the aggregate's owned collection, so no foreign-id resolution step is needed. The doc's list variant caches `unitOfWorkService`/`commandGateway` as fields where the app's existing `GetCourses`/`GetTopicsByCourse` do not; followed the in-app precedent. |
| `docs/concepts/testing.md` | § Taxonomy, § Assertion Ownership, § Fake/Wrong/Weak, § Spec-First, § T2 (incl. Not-Found Paths), § T4 | Yes | Assertion Ownership kept the T4 files to three one-case classes and pushed filtering/emptiness assertions into T2. |
| `applications/quizzes-full-2/harness-log.md` | rows 23-30 | Yes | Rows 26/27 pre-answered two questions that would otherwise have been halts; row 30 confirmed `Execution.addStudent` is the sanctioned collection mutator. |

---

## Skill Instructions Feedback

### What worked well

- 2.4.b: the three-way split of what session `b` creates (service / commands+sagas / functionalities+handler), plus the explicit "always required even if plan.md omits it" notes, meant nothing had to be inferred.
- 2.4.b: § "Fixture state a create cannot reach" (written mid-session in response to the halt) is a good fix — it names the exact situation, a read filtering on an owned-entity field, rather than a general principle, so it was directly applicable.
- 2.4.b: the signature-contract rule made `createExecution(courseAggregateId, acronym, academicTerm, endDate)` fall out of plan.md's own `CreateExecution(...)` signature exactly. `courseName`/`courseType` are seeded internally from the domain constants, so 2.4.c can swap the body without touching a call site.

### What was unclear or missing

- 2.4.b (first attempt, halted): session-b.md's fixture section reasoned entirely about *scalar* params — own field versus foreign id — and had no concept of a fixture whose required state is a collection. Since the read being tested is precisely the one that filters on that collection, the gap was structural, not incidental: it recurs at `Question`, `Quiz`, `QuizAnswer` and `Tournament` in this same application.
- 2.4.b (second attempt): the new subsection's code block loaded the aggregate through a service read that returns a DTO. A literal transcription would not compile.

### Suggested wording / structure changes

- Both applied — see § Harness Changes.

### Manager observations

- This session took two spawns. The first halted Type 2 before writing anything and returned no implementation, only the friction block and an anticipated `FILES ADDED BEYOND plan.md` list. That anticipation was accurate and let the plan.md amendment be pre-planned; worth noting as a useful halt-report habit.
- Row 31 was escalated to the human verbatim with the slice's four options. The human chose the sibling-helper option, which was also the slice's own stated preference. Options (a) widen-the-create-helper, (c) per-test-class fixture and (d) defer-the-tests were rejected.
- Row 32 is a defect the manager introduced while fixing row 31, caught by the very next slice. The Type 1 fast path worked as designed: the slice implemented against the surrounding prose rather than the broken block, reported it, and did not halt.
- The session-end full clean suite was green on the first run (`MAVEN_EXIT=0`, tests=88, failures=0, errors=0). No cross-slice regression — expected for a single-slice session, but it does confirm the three earlier aggregates still pass with `ServiceMapping` and the shared Spock base class extended.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-b.md` § "Update `{AppClass}SpockTest.groovy`" | No account of how session `b` produces a fixture whose owned collection is non-empty, when only a session-`c` write functionality can populate it. | High — reported by 2.4.b. Blocked the session outright; four more aggregates in this application have the same shape. | Applied — § "Fixture state a create cannot reach" states the sibling-helper rule, the signature contract it inherits, and the two rejected alternatives. |
| `.claude/skills/implement-aggregate/session-b.md` § "Fixture state a create cannot reach" | The load step in the sibling-helper block used a service read that returns a DTO. | Low — reported by 2.4.b. The prose above and below the block is unambiguous, but a literal transcription would not compile. | Applied — replaced with `aggregateLoadAndRegisterRead` plus the `Saga{Aggregate}` copy constructor. |
| `.claude/skills/implement-aggregate/session-c.md` § create-helper replacement | The replacement body was fixed at one functionality call, so it could not honour a session-`b` signature encoding collection state. | Medium — reported by 2.4.b. Made the widen-the-create-helper option self-contradictory across the two sub-files. | Applied — session `c` now also replaces each sibling helper's body, under the same signature rule. |
| `docs/concepts/service.md` § "Custom Repository — Latest-Active-Version Query" | No JPQL shape for a bulk read filtered by a field of an owned collection (the join case); only root-field filters are shown. | Low — reported by 2.4.b. The join was mechanical, but it is the one shape that interacts non-obviously with the latest-active-version subquery. | Add a one-line variant: `select a from {Aggregate} a join a.{elements} e where a.state = 'ACTIVE' and e.{foreign}AggregateId = :id and a.version = (select max(...))`. Not yet applied — no slice was blocked by it. |
| `applications/quizzes-full-2/plan.md` (2.4.b row) | Row omitted the repository / custom-repository / custom-repository-sagas appends that a non-PK bulk read requires, plus the two shared test files. Same omission as the 2.3.b row. | Low — every bulk-read session must amend its own row. | Applied to the 2.4.b row. The generator-side fix (have `/classify-and-plan` emit the repository trio in every `b` row whose aggregate has a bulk read) is a Phase 1 change and is left for the end-of-run retrospective. |

---

## Patterns to Capture

- **Pattern:** Three appends, not one, for a read filtered on an owned-collection field
  **Observed in:** `.../execution/aggregate/ExecutionRepository.java`, `.../ExecutionCustomRepository.java`, `.../sagas/repositories/ExecutionCustomRepositorySagas.java`
  **Description:** The JPQL join query on the JPA repository, the id-returning method on the custom-repository interface, and its sagas implementation. plan.md's file table for a `b` session predictably under-lists these; the Topic 2.3.b row had the same gap.

- **Pattern:** A bulk-read command filtered by a foreign id
  **Observed in:** `.../commands/execution/GetUserExecutionsCommand.java`
  **Description:** Ends up with `super(uow, serviceName, null)` while still carrying the foreign id as a payload field. Worth stating as the canonical shape, since the `null` looks like an omission at a glance.

- **Pattern:** A read filtered on an owned-collection field is still a one-step read saga
  **Observed in:** `.../coordination/sagas/GetUserExecutionsFunctionalitySagas.java`
  **Description:** The criterion lives inside the aggregate, so the two-step foreign-id-resolution variant does not apply. Worth one sentence in `docs/concepts/sagas.md` § "One-step vs two-step read saga decision", which currently contrasts only "field on the aggregate" against "foreign id needing resolution" and leaves the owned-collection case unclassified.

- **Pattern:** The minimal create helper is what makes the sibling helper safe
  **Observed in:** `.../QuizzesFull2SpockTest.groovy`
  **Description:** `create{Aggregate}`'s parameter list is literally plan.md's `Create{Aggregate}(...)` signature. That identity is the check that the session-`c` swap will not move a call site; a collection parameter on the create helper would have no counterpart in the create functionality.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: 31, 32

For each `fixed` row, the `harness:` commit sha:

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 31 | 2 | fixed | `a85cd8f7` |
| 32 | 1 | fixed | `8c019542` |

---

## One-Line Summary

A session-`b` read that filters on an owned collection had no sanctioned fixture, because only a session-`c` write can populate one; the human ruled for a sibling fixture helper per depended-on write functionality, keeping the create helper minimal and the 2.4.c signature swap call-site-stable.
