# Retro — 2.3.b — User

**App:** trainticket
**Session:** 2.3.b (Read Functionalities)
**Date:** 2026-08-22

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/ServiceMapping.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/user/GetUserByIdCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/user/GetUsersCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/aggregate/UserRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/aggregate/UserCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/aggregate/sagas/repositories/UserCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/service/UserService.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/messaging/UserCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/coordination/sagas/GetUserByIdFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/coordination/sagas/GetUsersFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/coordination/functionalities/UserFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/BeanConfigurationSagas.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/user/UserServiceTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/user/GetUserByIdTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/user/GetUsersTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md`

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `.claude/skills/_shared/conventions.md` | § Anchor to the repository root, § Resolve app context, § Application isolation, § Harness log, § Run the test suite | Yes | The no-pipe rule plus surefire aggregation gave the verdict cleanly. |
| `.claude/skills/implement-aggregate/session-b.md` | whole file | Yes | The bean-method split (constructor vs `@Autowired` field) fixed in row 13 now matches the service the docs prescribe. |
| `docs/concepts/sagas.md` | § Read Functionality Sagas, § List-return read variant, § Unfiltered variant | Yes | `GetUsers` is the unfiltered variant; the `rootAggregateId = null` allowance from row 7 covered it with no inference. |
| `docs/concepts/testing.md` | § T2 - Service Test, § Not-Found Paths, § T4 - Functionality Test, § Assertion Ownership | Yes | The collection-read empty-result rule (row 9) covered `getUsers` directly. |
| `AGENTS.md` | § Harness evolution, § Build Commands | Yes | The JDK-21 note (row 2) explained the first `release version 21 not supported` failure immediately. |

**Sufficient?** = `Yes` / `Partial` / `No`

---

## Skill Instructions Feedback

### What worked well

- The § "Update `{AppClass}SpockTest.groovy`" fixture rule - build `createUser` directly on `SagaUser` because the create functionality does not exist until 2.3.c, with a signature chosen to survive the 2.3.c reroute - needed no interpretation; the 2.2.b/2.2.c pair in this app shows the swap works as specified.
- The unconditional `Get{Aggregate}ByIdCommand` rule and the ServiceMapping camelCase prerequisite are both stated where they are needed, before any command is written.

### What was unclear or missing

- none

### Suggested wording / structure changes

- none

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

- none

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: none

For each `fixed` row, the `harness:` commit sha:

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| (none) | — | — | — |

---

## One-Line Summary

The first session of this run to hit zero harness friction: every gap that bit the equivalent TrainType session (bean-method arity, unfiltered collection reads, the collection-read not-found exemption) was already closed, and `User`'s two reads were produced straight from the docs.
