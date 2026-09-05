# Retro — 2.2.b — User

**App:** quizzes-full-2
**Session:** 2.2.b (Read Functionalities)
**Date:** 2026-08-03

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/user/GetUserByIdCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/user/GetStudentsCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/commands/user/GetTeachersCommand.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/user/service/UserService.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/user/messaging/UserCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/user/coordination/sagas/GetUserByIdFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/user/coordination/sagas/GetStudentsFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/user/coordination/sagas/GetTeachersFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/user/coordination/functionalities/UserFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/user/UserServiceTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/user/GetUserByIdTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/user/GetStudentsTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/coordination/user/GetTeachersTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/ServiceMapping.java` (appended — `USER("user")`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/user/aggregate/UserRepository.java` (appended — `findAllLatestActiveByRole` JPQL)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/user/aggregate/UserCustomRepository.java` (appended — `findUserIdsByRole`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/user/aggregate/sagas/repositories/UserCustomRepositorySagas.java` (appended — `findUserIdsByRole` implementation)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy` (appended — `userService`, `userCommandHandler`, `userFunctionalities` beans)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy` (appended — `userService` / `userFunctionalities` fields, `createUser` fixture helper)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md` (manager — 2.2.b row amended for five omitted files; 2.2.b checkbox ticked)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `.claude/skills/_shared/conventions.md` | § Anchor to repo root, § Application isolation, § Run the test suite | Yes | verdict read from `MAVEN_EXIT` + surefire, never from piped stdout |
| `.claude/skills/implement-aggregate/session-b.md` | whole file | Yes | the "Produce" list is the authority; the plan.md row was the weaker artifact |
| `docs/concepts/service.md` | § Injected Dependencies, § Method Patterns → Read method, § Custom Repository — Latest-Active-Version Query | Yes | the "omit any of the five the service does not use" clause let `UserService` drop both `UserRepository` and `AggregateIdGeneratorService` |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § File Location, § ServiceMapping, § Routing Commands | Yes | the no-arg-ctor + setters rule mattered: `GetUserByIdCommand` round-trips through Jackson |
| `docs/concepts/sagas.md` | § Read Functionality Sagas, § List-return read variant | Yes | one-step form correct for all three reads; no filter parameter is a foreign id, so no two-step variant |
| `docs/concepts/testing.md` | § T2 (incl. Not-Found Paths), § T4, § Assertion Ownership, § Fake/Wrong/Weak, § Spec-First Ordering | Yes | Path A applies to `getUserById`; T4 kept to orchestration + `NOT_IN_SAGA` only |
| `applications/quizzes-full-2/plan.md` | § 2. User (read functionality list, Role values, saga states), 2.2.b file row | Partial | the 2.2.b file row omitted the two shared test files session-b.md mandates |

---

## Skill Instructions Feedback

### What worked well

- 2.2.b1: session-b.md's per-file `###` sections map one-to-one onto what had to be written, and its explicit "plan.md is a blueprint, not a manifest" line pre-authorised the three repository files the role-filtered bulk reads needed — no halt was necessary.
- 2.2.b1: the note carried forward from 2.1.b (`findAll()` is forbidden for bulk reads) meant the repository-query decision took zero re-derivation.

### What was unclear or missing

- 2.2.b1: session-b.md § "Update `BeanConfigurationSagas.groovy`" shows a service `@Bean` template whose first parameter is `{Aggregate}Repository`, while service.md's own Injected Dependencies example and its "omit any of the five the service genuinely does not use" clause allow dropping it. The two reconcile on a careful read, but the template is the thing an agent copies, so it biases toward injecting a JPA repository the service never touches. Not filed as friction — no contradiction is demonstrable, and 2.1.b resolved it the same way.
- 2.2.b1: session-b.md's `create{Aggregate}` fixture template ("built directly on the aggregate here") is stated unconditionally, but the sibling `createCourse` helper in the same base class already goes through the real create functionality because 2.1.c re-pointed it. A reader comparing the two could think the template was violated. One sentence noting that earlier aggregates' helpers will already look different would remove the doubt.

### Suggested wording / structure changes

- none

### Manager observations

- plan.md lists no slice sub-checkboxes for 2.2.b (three read functionalities, at the threshold), so the session ran as one implicit slice, `2.2.b1`, with no re-spawns.
- Session-end full clean suite: `MAVEN_EXIT=0`, TOTAL tests=22 failures=0 errors=0 skipped=0. No cross-slice regression (single slice); the Course tests that run through the shared `QuizzesFull2SpockTest.groovy` stayed green after this session appended to it.
- No Type 1 fix was made and no Type 2 was escalated. The five plan.md file-row omissions are application-artifact defects handled by § "Amend plan.md for omitted files", not harness friction.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a — session `b` produces read sagas only; reads acquire no lock, per sagas.md § Read Functionality Sagas, so no `setSemanticLock` call site exists.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/service.md` § Custom Repository — Latest-Active-Version Query | the JPQL example is unparameterised (`findAllLatestActive()`); a filtered variant (`and x.{field} = :{field}` with `@Param`) exists in prose only ("or a narrower variant") | Low — reported by 2.2.b1; the extrapolation is mechanical, but an enum-valued filter column is the one place a reader might reach for a string literal instead of a bound parameter | add a two-line filtered variant beside the existing block, binding the filter with `@Param` |
| `.claude/skills/implement-aggregate/session-b.md` § Update `BeanConfigurationSagas.groovy` | the service `@Bean` template's parameter list is stricter than service.md's "omit what the service does not use" clause | Low — reported by 2.2.b1 | note on the template that its parameter list mirrors the service's actual constructor |

---

## Patterns to Capture

- **Pattern:** Two filter-constant reads behind one private helper
  **Observed in:** `microservices/user/service/UserService.java`
  **Description:** Two same-shaped list reads differing only by a filter constant collapse to one private helper (`getUsersByRole(role, uow)`) behind two public methods. The public methods stay one-per-functionality — that is what the CommandHandler cases and the coordinator methods bind to — while the duplicated stream/map/collect body exists once.

- **Pattern:** Filter inside the latest-active-version JPQL, not after it
  **Observed in:** `microservices/user/aggregate/UserRepository.java`
  **Description:** A filtered bulk read filters on the column *inside* the outer query, with the `max(version)` subquery still correlated on `aggregateId` alone. Filtering after the fact (load all latest-active, then filter in the service) would be correct here only because the filter field is `final` on the aggregate; doing it in JPQL keeps it correct for mutable filter fields too.

- **Pattern:** Negative-membership fixture for list reads
  **Observed in:** `sagas/user/UserServiceTest.groovy`
  **Description:** T2 list-read coverage that is not Weak needs a fixture population that also contains the excluded categories, so a query that forgot its filter predicate fails rather than passing on a homogeneous fixture.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: none

For each `fixed` row, the `harness:` commit sha:

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| — | — | — | — |

---

## One-Line Summary

All three read functionalities landed green with no harness friction; the session's only substantive decision — a role-filtered latest-active-version JPQL query rather than `findAll()` plus in-service filtering — was pre-authorised by service.md and by the carried-forward 2.1.b precedent.
