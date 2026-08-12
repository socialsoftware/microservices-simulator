# Review — User

**App:** quizzes-full
**Aggregate:** User (aggregate #2 in plan.md)
**Date:** 2026-05-01
**Verdict:** Yellow

> **Yellow** = minor deviations or missing test coverage; build passes.

---

## Summary

The User aggregate implementation is structurally sound: all domain, factory, service, command-handler, saga, and coordination files are present and follow the documented patterns. Both P1 rules are correctly enforced. All five test classes compile and pass. The verdict is Yellow due to two missing test scenarios: (1) `CreateUserTest.groovy` is listed in plan.md 2.2.b but was never produced, leaving the CreateUser path without a T2 test; (2) `DeleteUserFunctionalitySagas` acquires a semantic lock in `getUserStep` but `DeleteUserTest` contains no step-interleaving case. One minor structural deviation: `SagaUser`'s copy constructor copies `sagaState` instead of resetting to `NOT_IN_SAGA`, which is the reverse of the documented quizzes-full convention (both are noted as valid in the review skill).

---

## File Inventory

| File (relative to microservices/user/) | In Reference | In Target | Status | Notes |
|----------------------------------------|-------------|-----------|--------|-------|
| `aggregate/User.java` | Yes | Yes | OK | |
| `aggregate/Role.java` | Yes | Renamed: `aggregate/UserRole.java` | Renamed | quizzes-full uses `UserRole` for clarity |
| `aggregate/UserDto.java` | Yes | Yes | OK | |
| `aggregate/UserFactory.java` | Yes | Yes | OK | |
| `aggregate/UserRepository.java` | Yes | Yes | OK | |
| `aggregate/UserCustomRepository.java` | Yes | Yes | OK | |
| `aggregate/sagas/SagaUser.java` | Yes | Yes | OK | |
| `aggregate/sagas/states/UserSagaState.java` | Yes | Yes | OK | |
| `aggregate/sagas/factories/SagasUserFactory.java` | Yes | Yes | OK | |
| `aggregate/sagas/repositories/UserCustomRepositorySagas.java` | Yes | Yes | OK | |
| `service/UserService.java` | Yes | Yes | OK | |
| `messaging/UserCommandHandler.java` | Yes | Yes | OK | |
| `coordination/functionalities/UserFunctionalities.java` | Yes | Yes | OK | |
| `coordination/sagas/CreateUserFunctionalitySagas.java` | Yes | Yes | OK | Infrastructure (no planned CreateUser in original domain) |
| `coordination/sagas/DeleteUserFunctionalitySagas.java` | Yes | Yes | OK | |
| `coordination/sagas/FindUserByIdFunctionalitySagas.java` | Yes | Renamed: `GetUserByIdFunctionalitySagas.java` | Renamed | quizzes-full naming convention |
| `coordination/webapi/UserController.java` | Yes | Yes | OK | |
| `UserServiceApplication.java` | Yes | Yes | OK | |
| `coordination/sagas/ActivateUserFunctionalitySagas.java` | Yes | No | Intentional | Activate/Deactivate not in quizzes-full domain |
| `coordination/sagas/DeactivateUserFunctionalitySagas.java` | Yes | No | Intentional | |
| `coordination/sagas/GetStudentsFunctionalitySagas.java` | Yes | No | Intentional | Different read scope in quizzes-full |
| `coordination/sagas/GetTeachersFunctionalitySagas.java` | Yes | No | Intentional | |
| `commands/user/ActivateUserCommand.java` | Yes | No | Intentional | |
| `commands/user/DeactivateUserCommand.java` | Yes | No | Intentional | |
| `commands/user/GetStudentsCommand.java` | Yes | No | Intentional | |
| `commands/user/GetTeachersCommand.java` | Yes | No | Intentional | |
| `commands/user/CreateUserCommand.java` | No (ref uses different entry) | Yes | Extra (Intentional) | Infrastructure for test setup; no native CreateUser in domain |
| `commands/user/DeleteUserCommand.java` | Yes | Yes | OK | |
| `commands/user/GetUserByIdCommand.java` | Yes | Yes | OK | |
| `commands/user/UpdateUserNameCommand.java` | No | Yes | Extra (Intentional) | Execution-owned saga invokes User service via this command |
| `commands/user/AnonymizeUserCommand.java` | No | Yes | Extra (Intentional) | Same — Execution saga calls User service |
| `causal/` subtree | Yes | No | Intentional | quizzes-full is sagas-only |

---

## Structural Review

### `aggregate/User.java`

- **Extends `Aggregate`:** `public abstract class User extends Aggregate` — Correct
- **`@Entity`:** present — Correct
- **P1 final field `USER_ROLE_FINAL`:** `private final UserRole role` — Correct
- **`verifyInvariants()`:** calls private `deletedState()` helper; throws `QuizzesFullException(QuizzesFullErrorMessage.USER_DELETED_STATE)` — Correct
- **`getEventSubscriptions()`:** returns `new HashSet<>()` (User has no subscribed events) — Correct
- **Copy constructor:** copies `name`, `username`, `role`, `active` — Correct

### `aggregate/sagas/SagaUser.java`

- **Extends `User`, implements `SagaAggregate`:** Correct
- **`sagaState` typed as `SagaState`:** Correct
- **Default constructor:** `sagaState = GenericSagaState.NOT_IN_SAGA` — Correct
- **Copy constructor:** copies `sagaState = other.getSagaState()` — **Minor deviation.** The review skill specifies quizzes-full should reset to `GenericSagaState.NOT_IN_SAGA` in copy constructors. The current implementation copies the state (the quizzes reference pattern). Both are noted as valid in the review skill; flagged for consistency.

### `aggregate/sagas/states/UserSagaState.java`

- Implements `SagaAggregate.SagaState` — Correct
- Contains only `READ_USER` — Correct. Per retro-2.2.a, `IN_DELETE_USER` is unnecessary for a two-step (get → delete-final) saga; `READ_USER` suffices.

### `aggregate/sagas/factories/SagasUserFactory.java`

- `@Service @Profile("sagas")` — Correct
- Implements `UserFactory` interface — correct per quizzes-full architecture principle (profile-agnosticism via abstract interface injection); review skill note "NOT implementing an interface" reflects the quizzes reference, not quizzes-full's design.
- Three methods: `createUser(Integer, UserDto)`, `createUserCopy(User)`, `createUserDto(User)` — Correct
- `createUserCopy` casts `User` to `SagaUser` internally — acceptable; factory knows its concrete type.

### `aggregate/sagas/repositories/UserCustomRepositorySagas.java`

- `@Service @Profile("sagas")` — Correct
- Implements `UserCustomRepository` — Correct
- `@Autowired UserRepository userRepository` — Correct
- Empty body (no custom cross-table lookups needed for User) — Correct

### `service/UserService.java`

- `@Service` — Correct
- `unitOfWorkService` constructor-injected; `aggregateIdGeneratorService` and `userFactory` `@Autowired` — Correct
- All five methods have `@Transactional(isolation = Isolation.SERIALIZABLE)` — Correct
- `createUser`: generates ID via `aggregateIdGeneratorService`, calls `userFactory.createUser`, calls `registerChanged` — Correct
- `deleteUser`: loads via `aggregateLoadAndRegisterRead`, calls `user.remove()`, registers the SAME instance (no copy) — Correct per service.md soft-delete exception and the quizzes-full no-copy canonical pattern
- `updateUserName`: loads, creates copy via factory, mutates copy, registers copy, registers event — Correct
- `anonymizeUser`: same copy-on-write pattern, registers `AnonymizeStudentEvent` — Correct
- No P3 guards (no P3 rules apply to User) — Correct

### `messaging/UserCommandHandler.java`

- `@Component` — Correct
- Extends `CommandHandler` — Correct
- `getAggregateTypeName()` returns `"User"` — Correct
- Switch expression covers: `GetUserByIdCommand`, `CreateUserCommand`, `DeleteUserCommand`, `UpdateUserNameCommand`, `AnonymizeUserCommand` — Correct
- Default branch logs warning and yields null — Correct

### `coordination/functionalities/UserFunctionalities.java`

- `@Service` — Correct
- Three methods: `createUser`, `deleteUser`, `getUserById`
- Each method: derives name via `getStackTrace()[0].getMethodName()`, creates `SagaUnitOfWork`, instantiates saga with `new` (not Spring injection), calls `executeWorkflow`, returns result from saga getter — Correct

### `coordination/sagas/CreateUserFunctionalitySagas.java`

- Extends `WorkflowFunctionality` — Correct
- Constructor calls `buildWorkflow(...)` — Correct
- Single step (`createUserStep`), no `SagaCommand` wrapper (create needs no lock) — Correct

### `coordination/sagas/DeleteUserFunctionalitySagas.java`

- Extends `WorkflowFunctionality` — Correct
- Two-step pattern:
  - Step 1 (`getUserStep`): wraps `GetUserByIdCommand` in `SagaCommand`, calls `setSemanticLock(UserSagaState.READ_USER)` — Correct
  - Step 1 compensation: releases lock via `GenericSagaState.NOT_IN_SAGA` — Correct
  - Step 2 (`deleteUserStep`): sends `DeleteUserCommand`, declares Step 1 as dependency — Correct

### `coordination/sagas/GetUserByIdFunctionalitySagas.java`

- Extends `WorkflowFunctionality` — Correct
- Single step, no `SagaCommand` wrapper — Correct
- Result stored in `userDto` field via direct assignment, exposed via `getUserDto()` getter — Correct

### Commands

| Command | Extends Command | `super(uow, serviceName, aggId)` | ServiceMapping.USER | Notes |
|---------|----------------|----------------------------------|---------------------|-------|
| `CreateUserCommand` | Yes | Yes (`null` for aggId — correct for new) | Yes | |
| `DeleteUserCommand` | Yes | Yes | Yes | |
| `GetUserByIdCommand` | Yes | Yes | Yes | |
| `UpdateUserNameCommand` | Yes | Yes | Yes | |
| `AnonymizeUserCommand` | Yes | Yes | Yes | |

All commands: Correct.

---

## Functionality Coverage

| Operation | In Service | Saga Class Exists | CommandHandler Case | Functionalities Method | T2 Test Exists | Notes |
|-----------|-----------|------------------|--------------------|-----------------------|---------------|-------|
| CreateUser | Yes (`createUser`) | `CreateUserFunctionalitySagas` | Yes | `UserFunctionalities.createUser` | **No** | `CreateUserTest.groovy` listed in plan.md 2.2.b but absent |
| DeleteUser | Yes (`deleteUser`) | `DeleteUserFunctionalitySagas` | Yes | `UserFunctionalities.deleteUser` | Yes (`DeleteUserTest`) | Missing interleaving case |
| UpdateUserName | Yes (`updateUserName`) | — (Execution-owned saga invokes) | Yes | — (service-command) | Yes (`UpdateUserNameTest`) | |
| AnonymizeUser | Yes (`anonymizeUser`) | — (Execution-owned saga invokes) | Yes | — (service-command) | Yes (`AnonymizeUserTest`) | |
| GetUserById | Yes (`getUserById`) | `GetUserByIdFunctionalitySagas` | Yes | `UserFunctionalities.getUserById` | Yes (`GetUserByIdTest`) | |

---

## Rule Enforcement

| Rule | Classification | Expected Implementation | Actual Implementation | Status |
|------|---------------|------------------------|----------------------|--------|
| `USER_ROLE_FINAL` | P1 (final field) | `private final UserRole role` in `User.java` | `private final UserRole role` — field is `final` | Correct |
| `USER_DELETED_STATE` | P1 (intra-invariant) | `verifyInvariants()` throws `QuizzesFullException(USER_DELETED_STATE)` when state is DELETED and `active != false` | `deletedState()` helper; `verifyInvariants()` throws `QuizzesFullException(QuizzesFullErrorMessage.USER_DELETED_STATE)` | Correct |

User publishes `DeleteUserEvent`, `UpdateStudentNameEvent`, `AnonymizeStudentEvent`. These events are consumed by downstream aggregates (Execution, Tournament, QuizAnswer). Publication is correctly wired in `UserService.deleteUser`, `updateUserName`, and `anonymizeUser`. No inter-invariants are owned by User (Events subscribed: none).

---

## Test Coverage

| Test Type | Class | Scenarios Present | Missing Scenarios | Notes |
|-----------|-------|------------------|-------------------|-------|
| T1 | `UserTest` | "create user" (valid data) | — | Invariant violation tests belong in T2 per testing.md; T1 is correct with one scenario |
| T2 (write) | `CreateUserTest` | — | **Entire file missing** | Listed in plan.md 2.2.b; never produced |
| T2 (write) | `DeleteUserTest` | "deleteUser: success" | Step-interleaving case for `getUserStep` (calls `setSemanticLock`) | One interleaving case required per review skill |
| T2 (service-cmd) | `UpdateUserNameTest` | "updateUserName: success" | — | Correct — single-step, no saga lock |
| T2 (service-cmd) | `AnonymizeUserTest` | "anonymizeUser: success" | — | Correct — single-step, no saga lock |
| T2 (read) | `GetUserByIdTest` | "getUserById: success", "getUserById: user not found" | — | Not-found uses `thrown(SimulatorException)` — Correct |
| T3 | — | — | None expected | User has no subscribed events |

**Not-found assertion check:** `GetUserByIdTest` uses `thrown(SimulatorException)` with `SimulatorErrorMessage.AGGREGATE_NOT_FOUND` — Correct.

---

## Retro Cross-Reference

| Retro | Session | Priority | Action Item | Source-file? | Status |
|-------|---------|----------|-------------|--------------|--------|
| retro-2.2.a | 2.2.a | High | Remove "calls `verifyInvariants()`" from session-a.md constructor description | Skill file | Not investigated (skill file) |
| retro-2.2.a | 2.2.a | High | Qualify IN_DELETE/IN_UPDATE saga state guidance in session-a.md | Skill file | Not investigated (skill file) |
| retro-2.2.a | 2.2.a | Medium | Add enum-typed field conditional to session-a.md Produce section | Skill file | Not investigated (skill file) |
| retro-2.2.a | 2.2.a | Medium | Add T1 DTO-constructor test variant to session-a.md | Skill file | Not investigated (skill file) |
| retro-2.2.a | 2.2.a | Medium | Always include Factory/CustomRepository in classify-and-plan 2.N.a rows | Skill file | Not investigated (skill file) |
| retro-2.2.a | 2.2.a | Low | Add T1 direct-instantiation template to docs/concepts/testing.md | Doc file | Not investigated (doc file) |
| retro-2.2.b | 2.2.b | High | Add createUser-as-infrastructure guidance to session-b.md | Skill file | Not investigated (skill file) |
| retro-2.2.b | 2.2.b | High | Ensure plan.md file tables include event files and Functionalities | Skill file | Not investigated (skill file) |
| retro-2.2.b | 2.2.b | Medium | Resolve soft-delete copy-on-write contradiction in docs/concepts/service.md | **Doc file** | **Resolved** — service.md Note section added: "Both are valid. For new aggregates in quizzes-full, follow the no-copy pattern shown above." |
| retro-2.2.b | 2.2.b | Medium | Add "skip P1 tests for final fields" to docs/concepts/testing.md | Doc file | Resolved — testing.md line 130: "Skip P1 tests for Java `final` fields — no write path can violate them." |
| retro-2.2.b | 2.2.b | Low | Add note: skip P1 violation tests for final fields in session-b.md | Skill file | Not investigated (skill file) |
| retro-2.2.c | 2.2.c | High | Add pre-empted GetXxxByIdCommand note to session-c.md | Skill file | Not investigated (skill file) |
| retro-2.2.c | 2.2.c | Medium | Clarify Functionalities update even when not in plan.md 2.N.c table | Skill file | Not investigated (skill file) |
| retro-2.2.c | 2.2.c | Low | Standardise read saga template in docs/concepts/sagas.md on direct field assignment | Doc file | Not investigated (doc file) |

No High-priority retro items target source files — no mandatory source-file investigation required.

---

## Build & Test Results

**Command:** `mvn clean -Ptest-sagas test -Dtest="UserTest,DeleteUserTest,UpdateUserNameTest,AnonymizeUserTest,GetUserByIdTest"`
**Outcome:** BUILD SUCCESS

| Test class | Result | Failures |
|------------|--------|---------|
| `UserTest` | PASS | 0 |
| `DeleteUserTest` | PASS | 0 |
| `UpdateUserNameTest` | PASS | 0 |
| `AnonymizeUserTest` | PASS | 0 |
| `GetUserByIdTest` | PASS | 0 |

**Total:** 6 tests run, 0 failures, 0 errors.

---

## Action Items

| Priority | Category | File | Finding | Fix |
|----------|---------|------|---------|-----|
| Major | Missing test | `sagas/coordination/user/CreateUserTest.groovy` | File listed in plan.md 2.2.b but never produced. No T2 coverage for `CreateUser` functionality. | Create `CreateUserTest.groovy` with at least a "createUser: success" case verifying the returned `UserDto` fields. |
| Major | Missing test scenario | `sagas/coordination/user/DeleteUserTest.groovy` | `DeleteUserFunctionalitySagas.getUserStep` calls `setSemanticLock(UserSagaState.READ_USER)` but no step-interleaving case exists. | Add a test case using `executeUntilStep("getUserStep", uow)` / `resumeWorkflow(uow)` to verify the semantic lock protection. |
| Minor | Structural deviation | `aggregate/sagas/SagaUser.java` | Copy constructor copies `sagaState = other.getSagaState()` (line 26). Documented quizzes-full convention is to reset to `NOT_IN_SAGA`. Both are noted as valid but quizzes-full should be consistent. | Change copy constructor to `this.sagaState = GenericSagaState.NOT_IN_SAGA` to match the quizzes-full convention. |
