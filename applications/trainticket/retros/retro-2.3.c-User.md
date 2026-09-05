# Retro — 2.3.c — User

**App:** trainticket
**Session:** 2.3.c (Write Functionalities)
**Date:** 2026-08-22

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/service/UserService.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/messaging/UserCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/coordination/functionalities/UserFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/coordination/sagas/CreateUserFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/coordination/sagas/UpdateUserFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/coordination/sagas/DeleteUserFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/user/coordination/webapi/UserController.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/user/CreateUserCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/user/UpdateUserCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/user/DeleteUserCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/exception/TrainticketErrorMessage.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/user/UserServiceTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/user/CreateUserTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/user/UpdateUserTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/user/DeleteUserTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/user/UpdateUserCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/user/DeleteUserCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/resources/groovy/UpdateUserCompensationTest/UpdateUserFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/resources/groovy/DeleteUserCompensationTest/DeleteUserFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md`

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `AGENTS.md` | § Harness evolution, § Build Commands | Yes | — |
| `.claude/skills/_shared/conventions.md` | § Anchor to the repository root, § Resolve app context, § Application isolation, § Neutral domain, § Harness log, § Run the test suite (incl. § Inspecting maven output) | Yes | The subprocess capture recipe added by commit `59122e1c2` was used as written and produced the `START EXECUTION STEP` lines the compensation sanity check needs. |
| `.claude/skills/implement-aggregate/session-c.md` | Reads, Produce (all subsections), BeanConfigurationSagas — No Change Needed, Update `{AppClass}SpockTest.groovy` | Yes | The "keep the helper signature exactly as 2.{N}.b wrote it" instruction was directly actionable - `createUser`'s parameter list maps one-to-one onto `UserDto`. |
| `docs/concepts/service.md` | § Method Patterns, § Copy-on-Write Rule, § P3 Guard Placement, § Exception-Throw Convention | Yes | — |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § File Location, § ServiceMapping Enum, § Sending Commands, § Routing Commands | Yes | `ServiceMapping.USER` was already present from 2.3.b. |
| `docs/concepts/sagas.md` | § Step Ordering, § Lock-Acquisition Step Pattern, § R4 Decision Table, § Create Functionality Sagas (Shape 1), § Write Workflow Structure | Yes | All three User writes are single-aggregate, so Shape 1 applied verbatim and no compensation was registered on the create step. |
| `docs/concepts/testing.md` | § Assertion Ownership, § T2 — Service Test, § Not-Found Paths, § T4 — Functionality Test, § Soft-delete functionalities, § Compensation Test, § CRITICAL gotcha, § Fake/Wrong/Weak, § Choosing Input Values, § Spec-First Ordering | Yes | The gap logged as harness-log row 14 in 2.2.c is closed: the sanity check ran this session by the documented means. |

---

## Skill Instructions Feedback

### What worked well

- § "Update `{AppClass}SpockTest.groovy`" is unambiguous about replacing only the *body* of the
  2.{N}.b fixture helper. Rerouting `createUser` onto `CreateUserFunctionalitySagas` left the
  2.3.b read tests untouched and they still pass, which is the stated intent of the instruction.
- The Compensation Test cross-reference chain (session-c.md → testing.md § Compensation Test →
  conventions.md § Inspecting maven output) worked end to end with no inference. The subprocess
  capture confirmed `START EXECUTION STEP: getUserStep` for both `updateUser` and `deleteUser`
  before `Fault on updateUserStep` / `Fault on deleteUserStep` fired.
- § "Choosing Input Values" correctly ruled out boundary cases here: `UNIQUE_USER_NAME` is a
  categorical rule, so one representative per class is complete and no straddling pair was invented.

### What was unclear or missing

- (none)

### Suggested wording / structure changes

- (none)

---

## Semantic-Lock Coverage Audit

| Saga class | Step name | Foreign aggregate locked | Lock-acquisition test name | Present? |
|------------|-----------|--------------------------|----------------------------|---------:|
| `CreateUserFunctionalitySagas` | — (no `setSemanticLock`; create saga, per sagas.md § Create Functionality Sagas) | — | — | n/a |
| `UpdateUserFunctionalitySagas` | `getUserStep` | none (primary aggregate) | `updateUser: getUserStep acquires IN_UPDATE_USER semantic lock` (`UpdateUserTest`) | Yes |
| `DeleteUserFunctionalitySagas` | `getUserStep` | none (primary aggregate) | `deleteUser: getUserStep acquires IN_DELETE_USER semantic lock` (`DeleteUserTest`) | Yes |

Compensate transitions for both lock-holding sagas are covered by `UpdateUserCompensationTest` and
`DeleteUserCompensationTest`.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| (none) | — | — | — |

---

## Patterns to Capture

- **Pattern:** Create-only own-table uniqueness guard on an immutable key
  **Observed in:** `microservices/user/service/UserService.java` (`checkUserNameIsUnique`)
  **Description:** When the unique field is Java `final`, the guard takes no
  "aggregate under change" parameter and is called only from the create method - the update path
  cannot break the rule. This is a narrower shape than the two-call-site guard used where the
  unique field is mutable, and plan.md's rule table already distinguishes the two cases. Both
  shapes now exist in this application; the docs describe the general placement rule
  (`service.md` § P3 Guard Placement) but not the immutable-key specialisation.

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: none

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| (none) | — | — | — |

---

## One-Line Summary

Three single-aggregate write sagas for User landed with no harness friction, and the
compensation-test sanity check that 2.2.c could not perform now runs by the documented subprocess
capture recipe, closing that gap empirically.
