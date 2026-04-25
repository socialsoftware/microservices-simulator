# Retro — 2.2.a — User

**App:** quizzes-full
**Session:** 2.2.a (Domain Layer)
**Date:** 2026-04-25

---

## Files Produced

- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/user/aggregate/UserRole.java` (new)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/user/aggregate/User.java` (new)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/user/aggregate/UserFactory.java` (new)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/user/aggregate/UserCustomRepository.java` (new)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/user/aggregate/UserDto.java` (new)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/user/aggregate/UserRepository.java` (new)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/user/aggregate/sagas/SagaUser.java` (new)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/user/aggregate/sagas/states/UserSagaState.java` (new)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/user/aggregate/sagas/factories/SagasUserFactory.java` (new)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/user/aggregate/sagas/repositories/UserCustomRepositorySagas.java` (new)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/user/UserTest.groovy` (new)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/exception/QuizzesFullErrorMessage.java` (modified — added `USER_DELETED_STATE`)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/BeanConfigurationSagas.groovy` (modified — added User beans)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/plan.md` (modified — ticked checkbox, updated 2.2.a file table)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/aggregate.md` | verifyInvariants, SagaAggregate, getEventSubscriptions, naming conventions | Partial | Doesn't address constructor calling verifyInvariants (see gaps) |
| `docs/concepts/testing.md` | T1 Creation section | Partial | Template assumes `create{Aggregate}` functionalities exist; no variant for aggregates without a CreateXxx write functionality |

---

## Reference App Consulted

| Reference file | Why consulted | Gap it reveals |
|---------------|--------------|----------------|
| `applications/quizzes/microservices/user/aggregate/User.java` | Field layout, P1 rule implementation (deletedState() helper pattern) | session-a.md describes verifyInvariants but doesn't document the private-helper pattern for compound state checks |
| `applications/quizzes/microservices/user/aggregate/sagas/states/UserSagaState.java` | Verify whether IN_DELETE_USER is expected or only READ_USER | session-a.md says "include IN_DELETE_{AGGREGATE}" for write sagas — but the reference doesn't use it for User; guidance is overly broad |
| `applications/quizzes/microservices/user/coordination/sagas/DeleteUserFunctionalitySagas.java` | Confirm which saga state the delete saga sets on User during the get step | Needed because session-a.md's IN_DELETE guidance conflicts with reference practice for two-step sagas |
| `applications/quizzes/microservices/user/aggregate/UserDto.java` | DTO fields and constructor-from-aggregate pattern | session-a.md describes DTO structure but not which fields to include for each aggregate — always requires reference check |
| `applications/quizzes/microservices/user/aggregate/UserFactory.java` | Interface method names and signatures | session-a.md uses generic `create{Aggregate}Copy` naming but quizzes uses `createUserFromExisting`; confirmed quizzes-full should use `createUserCopy` per the session-a.md template |
| `applications/quizzes/microservices/user/coordination/sagas/CreateUserFunctionalitySagas.java` | Understand User creation saga shape for context | No gap — just orientation; quizzes-full has no CreateUser functionality |

---

## Skill Instructions Feedback

### What worked well

- The file list structure in session-a.md is accurate and well-ordered.
- The distinction between abstract base class and SagaXxx subclass is clearly described.
- The BeanConfigurationSagas update instructions are precise and produce correct output.
- Step 5b (patch plan.md for missing files) correctly prompted adding UserRole, UserFactory, and UserCustomRepository.

### What was unclear or missing

- **verifyInvariants in constructor:** session-a.md's "Constructor" description says "calls `verifyInvariants()`" but neither the quizzes reference User nor the existing quizzes-full Course calls it in the constructor. This contradiction required checking the reference to decide — the correct answer is: do not call it in the constructor.
- **IN_DELETE saga state guidance:** session-a.md says "Include `IN_UPDATE_{AGGREGATE}` and `IN_DELETE_{AGGREGATE}` for write sagas that modify an existing instance." For User's simple two-step DeleteUser saga (get → delete), the reference uses `READ_USER` (not `IN_DELETE_USER`) because the delete step is the final step and no interleaving protection is needed. The rule should be qualified: `IN_DELETE` is needed only when there are saga steps *after* the delete that reference the aggregate under a different lock.
- **Aggregate-specific enum types:** The "Produce" section lists named file types but does not proactively flag that an aggregate with an enum-typed field (e.g., `role: UserRole`) requires a companion enum file. It only appears in Step 5b as a potential omission to patch. It should be an explicit conditional in the Produce section.
- **T1 test for DTO-constructor aggregates:** The session-a.md T1 note says "instantiate `Saga{Aggregate}` directly (e.g., `new Saga{Aggregate}(1, 'name', Type.VALUE)`)" but User's constructor takes `(Integer aggregateId, UserDto userDto)`, not raw args. There's no guidance on how to set up the UserDto before instantiation in the test. This required inference.

### Suggested wording / structure changes

- `session-a.md` → "Constructor" bullet: change "calls `verifyInvariants()`" to "does NOT call `verifyInvariants()` — the UoW calls it at commit time." Add a note that the test must call it manually to check violations.
- `session-a.md` → UserSagaState section: add "For a simple two-step saga (read → write-final-step), `READ_{AGGREGATE}` is sufficient as the only non-NOT_IN_SAGA state. Add `IN_UPDATE_{AGGREGATE}` only when there are saga steps after the primary write step that need to observe a distinct locked state."
- `session-a.md` → Produce section: add a conditional bullet after the owned-entity section: "If any aggregate field is typed as a domain enum (e.g., `role: UserRole`), produce `{Aggregate}Role.java` (or appropriate name) as a separate file. Add it to the plan.md 2.N.a row."
- `session-a.md` → T1 test section: add a note "If the aggregate constructor takes a `{Aggregate}Dto` rather than raw args, build the DTO first in the `given:` block, then instantiate `new Saga{Aggregate}(id, dto)`."

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/aggregate.md` | No mention of whether constructors should/should not call `verifyInvariants()` | Medium — caused contradiction with session-a.md instruction | Add explicit note: constructors do not call verifyInvariants; the UoW invokes it at commit; tests call it manually to verify violations |
| `docs/concepts/testing.md` | T1 template assumes `{aggregate}Functionalities.create{Aggregate}()` exists — no variant for aggregates that have no CreateXxx write functionality | Medium — required inference for UserTest | Add alternative T1 template for direct-instantiation case with DTO setup |

---

## Patterns to Capture

- **Pattern:** Private helper method for compound state checks in verifyInvariants
  **Observed in:** `User.java` (quizzes reference and quizzes-full)
  **Description:** When a P1 rule involves multiple fields (e.g., state == DELETED implies active == false), extract the check into a private boolean helper (e.g., `deletedState()`) and call `if (!deletedState()) throw` in verifyInvariants. Keeps the invariant method readable when there are multiple P1 rules.

- **Pattern:** UserFactory interface uses `createUserCopy` (not `createUserFromExisting`)
  **Observed in:** quizzes-full `UserFactory.java`
  **Description:** quizzes-full normalises the copy-constructor method name to `create{Aggregate}Copy` matching session-a.md's template. The quizzes reference used `createUserFromExisting` — quizzes-full diverges here intentionally for consistency.

- **Pattern:** plan.md 2.N.a rows systematically omit `{Aggregate}Factory.java` and `{Aggregate}CustomRepository.java`
  **Observed in:** both 2.1.a (Course) and 2.2.a (User) plan.md rows
  **Description:** The classify-and-plan skill does not include these interfaces in the generated file table. They are always required for session-a and should be added to the plan.md template or the classify-and-plan skill's output.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| High | `.claude/skills/implement-aggregate/session-a.md` | Remove "calls `verifyInvariants()`" from constructor description; add note that tests call it manually |
| High | `.claude/skills/implement-aggregate/session-a.md` | Qualify IN_DELETE/IN_UPDATE saga state guidance: only needed when saga has steps after the primary write; READ_{AGGREGATE} suffices for two-step (get → delete) sagas |
| Medium | `.claude/skills/implement-aggregate/session-a.md` | Add explicit conditional in Produce section: if aggregate has enum-typed field, produce companion enum file and add to plan.md |
| Medium | `.claude/skills/implement-aggregate/session-a.md` | Add T1 test variant for DTO-constructor aggregates (set up DTO in `given:` block before `new Saga{Aggregate}(id, dto)`) |
| Medium | `.claude/skills/classify-and-plan` | Always include `{Aggregate}Factory.java` and `{Aggregate}CustomRepository.java` in 2.N.a file table rows |
| Low | `docs/concepts/testing.md` | Add T1 direct-instantiation template for aggregates without a CreateXxx functionality |

---

## One-Line Summary

The main friction point was the contradiction between session-a.md's "constructor calls verifyInvariants" instruction and the reference app's practice of not doing so — plus overly broad IN_DELETE saga state guidance that conflicts with the reference's two-step delete pattern.
