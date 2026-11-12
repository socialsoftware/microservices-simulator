# Retro — 2.2.a — User

**App:** quizzes-full-2
**Session:** 2.2.a (Domain Layer)
**Date:** 2026-08-03

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/user/aggregate/User.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/user/aggregate/Role.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/user/aggregate/UserFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/user/aggregate/UserCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/user/aggregate/UserRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/user/aggregate/UserDto.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/user/aggregate/sagas/SagaUser.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/user/aggregate/sagas/states/UserSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/user/aggregate/sagas/factories/SagasUserFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/user/aggregate/sagas/repositories/UserCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/user/UserServiceApplication.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/user/UserIntraInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/exception/QuizzesFull2ErrorMessage.java` (appended — `USER_DELETED_STATE`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy` (appended — `Role` import, `USER_AGGREGATE_ID` / `USER_NAME` / `USER_USERNAME` / `USER_ROLE`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy` (appended — `sagasUserFactory()`, `userCustomRepositorySagas()`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md` (manager — 2.2.a row amended for three omitted files; 2.2.a checkbox ticked)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `.claude/skills/_shared/conventions.md` | § Anchor to repo root, § Application isolation, § Harness log, § Run the test suite | Yes | narrow-form test command unambiguous |
| `.claude/skills/implement-aggregate/session-a.md` | § Reads, § Verify Mandatory Files, § Produce (all subheadings), § Update `{AppClass}SpockTest.groovy`, § Update `BeanConfigurationSagas.groovy` | Yes | "plan.md is a blueprint, not a manifest" resolved all three file omissions without a halt |
| `docs/concepts/aggregate.md` | § Key Fields, § Base Class, § Variants → Sagas, § Factories, § Repositories | Yes | `getEventSubscriptions()` correctly skippable — User subscribes to nothing |
| `docs/concepts/testing.md` | § T1 — Aggregate Test, § Choosing Input Values (EP & BVA), § Spec-First Ordering, § Fake/Wrong/Weak | Yes | the "categorical → no boundary pair" rule decided `USER_DELETED_STATE`'s test shape |
| `applications/quizzes-full-2/quizzes-full-2-domain-model.md` | §1 (User row), §3.1 (`USER_ROLE_FINAL`, `USER_DELETED_STATE`) | Yes | Role set = STUDENT / TEACHER / ADMIN taken verbatim |
| `applications/quizzes-full-2/plan.md` | § 2. User (write/read functionalities, saga states, P1 list), 2.2.a file row | Partial | file row omitted the three shared files session-a.md mandates |

---

## Skill Instructions Feedback

### What worked well

- session-a.md's per-file subheadings are precise enough that no sibling-aggregate read was required.
- The `Saga{Aggregate}` copy-constructor rationale (inherit `sagaState`, never reset) is stated together with its consequence, removing the only genuine judgement call in the session.
- The explicit "plan.md is a blueprint, not a manifest" clause — without it each of the three omitted shared files would have been a Type 2 halt.

### What was unclear or missing

- 2.2.a1: session-a.md mandates appending to `BeanConfigurationSagas.groovy`, while this application's plan.md attributes that file to session `b` for aggregate 1 (the 2.1.b row reads "session-b.md mandates the three beans"). Both are satisfiable — `a` adds the factory/repository beans, `b` adds the service/functionalities beans — but a reader cross-checking the two artifacts sees an apparent disagreement about which session owns the file.

### Suggested wording / structure changes

- none

### Manager observations

- Session `a` is never sliced, so this session ran as one implicit slice, `2.2.a1`, with no re-spawns.
- The slice returned no return block on completion and had to be asked for it explicitly; the block was then complete and well-formed.
- Session-end full clean suite: `MAVEN_EXIT=0`, TOTAL tests=13 failures=0 errors=0 skipped=0. No cross-slice regression (single slice).
- No Type 1 fix was made and no Type 2 was escalated. The three plan.md file-row omissions are application-artifact defects handled by § "Amend plan.md for omitted files", not harness friction.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-a.md` § Produce → `{Aggregate}.java` | no statement about the `@Table` name colliding with a SQL reserved word | Low — reported by 2.2.a1; `users` is legal in H2 2.x, but a singular reserved name would fail at schema creation with a cryptic error | one line: quote or pluralize table names that are SQL reserved words |
| `.claude/skills/implement-aggregate/session-a.md` § `{Aggregate}Dto.java` / `{Aggregate}.java` | no boolean-accessor convention (`isX()` vs `getX()`) | Low — reported by 2.2.a1; `isActive()` chosen on both aggregate and DTO, a later session assuming the other form would not compile | state the convention once |

---

## Patterns to Capture

- **Pattern:** EP-only test pair for a boolean state freeze
  **Observed in:** `sagas/user/UserIntraInvariantTest.groovy`
  **Description:** A P1 state freeze of the form `state == DELETED ⟹ flag == false` is naturally covered by a violation/satisfaction equivalence pair with no BVA straddle. Adding the explicit satisfying case is cheap and makes the freeze's direction legible in the test file.

- **Pattern:** Null-safe boolean predicate in `verifyInvariants()`
  **Observed in:** `microservices/user/aggregate/User.java`
  **Description:** `Boolean.TRUE.equals(field)` avoids an NPE on a Hibernate-loaded row whose column is null — a hazard the direct-construction T1 path never surfaces.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: none

For each `fixed` row, the `harness:` commit sha:

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| — | — | — | — |

---

## One-Line Summary

The User domain layer landed green with zero harness friction; the only friction was plan.md's 2.2.a file row omitting the three shared files session-a.md mandates, which the "blueprint, not a manifest" clause resolved without a halt.
