# Session 2.N.b — Write Functionalities

This sub-file is loaded by `implement-aggregate` when the target session type is `b`. All context variables (`{app-name}`, `{pkg}`, `{AppClass}`, `{Aggregate}`, `{N}`, `{src}`, `{test}`, `{bean-config}`) are already available from the router.

---

## Reads

Load these files before writing any code:

1. **Domain files produced in session 2.{N}.a** — read every file listed in the plan.md `2.{N}.a` row for this aggregate. You need the aggregate class structure, field names, constructor signature, and `SagaState` enum values.

2. **`docs/concepts/service.md`** — specifically:
   - § Method Patterns (Read / Create / Mutate / Mutate with event publication / Mutate with optional sub-collection parameter)
   - § Copy-on-Write Rule, § DTO Immutability (R7), § Exception-Throw Convention
   - § P3 Guard Placement — where own-table uniqueness checks and DTO field validation live
   - § Partial-Data Owned Entities (only if the aggregate has owned sub-entities)
   - § Custom Repository — Latest-Active-Version Query (only if returning lists)

3. **`docs/concepts/commands.md`** — specifically:
   - § What a Command Is, § Naming Conventions, § File Location
   - § ServiceMapping Enum (mandatory entry per aggregate)
   - § Sending Commands (Functionality Layer), § Routing Commands (CommandHandler)
   - § Known DTO Gaps and Compensating Command Steps (only if the saga touches a known-gap DTO; see subsection list)

4. **`docs/concepts/sagas.md`** — specifically:
   - § Step Ordering (authoritative; this session no longer restates it)
   - § Lock-Acquisition Step Pattern (Two-Step Write Sagas), § Semantic Locks in Practice
   - § R4 Decision Table — `SagaCommand` vs `setForbiddenStates`
   - § Write Workflow Structure

5. **`docs/concepts/testing.md`** — § Assertion Ownership, § T2 — Service Test, § T3 — Event Publication Test, § T4 — Functionality Test. Note:
   - **Assertion Ownership** — each fact is asserted in exactly one tier. T2 owns persistence, uniqueness, not-found, and P3 numeric boundaries; T3 owns event-publication assertions; T4 asserts orchestration outcomes only.
   - What a T2 service test asserts (change persisted and read back via a **fresh** UnitOfWork, uniqueness/composite-key guards, P3 numeric-guard boundaries) — direct `*Service` bean calls, no saga workflow, no P1 predicate tests
   - What a T3 publication test asserts (event row in the store with correct type + every payload field, plus one negative no-publish case) — triggered via direct service call
   - What a T4 functionality test asserts (orchestration outcomes, saga-path guard violations, P4a prerequisite failures — e.g., sending a command that causes the saga fetch to fail)
   - **State-transition rule (semantic-lock acquisition):** each saga step that calls `setSemanticLock` is an *acquire* transition into `IN_{OP}` (see `docs/concepts/testing.md` § T4 — Functionality Test). One case per such step: use `executeUntilStep`, assert the expected `IN_{OP}` saga state, then `resumeWorkflow` and `noExceptionThrown()` (completing the traversal back to `NOT_IN_SAGA`). Cross-aggregate `setForbiddenStates` conflict validation is **deferred — see Appendix — Cross-Functionality Test** in `docs/concepts/testing.md`.
   - § Fake / Wrong / Weak Detection Checklist — apply before committing each test file

6. ***(Conditional)*** If the plan.md aggregate section lists cross-aggregate prerequisites (P4a or P3 DTO-check rules): read the service file and relevant command files of each upstream aggregate involved. You need their command class names, service method signatures, and what they throw on failure.

---

## Produce

Produce every file listed in the plan.md `2.{N}.b` row. The authoritative file list is in plan.md — use it exactly. The descriptions below explain what each file must contain.

> **Prerequisite — ServiceMapping**: Verify that `{src}{AppClass}/ServiceMapping.java` exists and contains an entry for `{AGGREGATE}`. If not, create it (or add the missing entry) before writing any commands — every command constructor references `ServiceMapping.{AGGREGATE}.getServiceName()`.
>
> **Multi-word aggregate naming:** The value must equal `resolveServiceName(SagaXxx)`, which strips "Saga" from the aggregate's simple class name and lowercases the first character. For multi-word aggregates this is camelCase — e.g. `SagaQuizAnswer` → `"quizAnswer"`. Never use a shortened alias; mismatches cause silent bean-lookup failures that only appear at commit/abort time.

> **Prerequisite — `Get{Aggregate}ByIdCommand`**: If `Get{Aggregate}ByIdCommand` does not yet exist (it may be planned for session-c), create it now. Write sagas that use a get-then-lock step require this read command in session-b. Also add `get{Aggregate}ById` to `{Aggregate}Service` (if not yet present) and a handler case for `Get{Aggregate}ByIdCommand` in `{Aggregate}CommandHandler` — missing either will cause a compile error even when the command class exists.
>
> **Recording this in plan.md:** After creating `Get{Aggregate}ByIdCommand` here, add a note in plan.md under the session-b row (or as a separate inline comment) stating that the command was already created in session-b. This prevents session-c from creating a duplicate when it encounters the command in the session-c file list.

> **Prerequisite — Upstream count-manipulation commands**: If any saga for this aggregate sends an `Increment{Xxx}CountCommand` or `Decrement{Xxx}CountCommand` to an upstream aggregate's `CommandHandler`, verify that handler already routes the command. If the case is missing, add it before running tests — an unrouted command silently does nothing and will cause invariant violations or state corruption that are difficult to diagnose after the fact.

### `{Aggregate}Service.java` (write methods)

Path: `{src}microservices/{aggregate}/service/{Aggregate}Service.java`

- Spring `@Service`
- One method per write functionality listed in plan.md
- Method signature: receives the command's fields + `UnitOfWork unitOfWork`
- **P3 own-table uniqueness guards** (if listed in plan.md P3 rules): query the repository for duplicates before creating; throw `{AppClass}Exception` with the appropriate error message constant if found
- **P3 DTO field checks** (if listed in plan.md cross-aggregate prerequisites): receive the saga-assembled DTO as a parameter; validate the field; throw `{AppClass}Exception` on violation
- After validation: fetch the target aggregate via `{Aggregate}CustomRepositorySagas`, mutate its fields via setters, call `verifyInvariants()`, then `unitOfWork.registerChanged(aggregate)`
- **Soft-delete** (`remove()`): use copy-on-write — load the aggregate, create a factory copy via `factory.create{Aggregate}Copy(old)`, call `copy.remove()`, then `registerChanged(copy)`. Never call `remove()` on the managed entity returned by `aggregateLoadAndRegisterRead`; doing so lets JPA auto-flush the deleted state before the saga abort query runs, making the aggregate invisible to the abort path.
- **Event publishing**: for each event this aggregate publishes (see plan.md Events published), call `unitOfWork.registerEvent(new {Event}(...))` at the end of the relevant service method

> **Deferred P3 guards:** If a P3 DTO-check rule listed in plan.md cross-aggregate prerequisites requires data from an aggregate ordered _after_ this one in plan.md (because that later aggregate subscribes to this one's events), the guard cannot be implemented yet. Do the following:
> 1. **Skip** the data-assembly saga step and the service guard — do not add stubs.
> 2. Add a `// TODO: {RULE_NAME} — deferred; requires {LaterAggregate}Dto, available after session 2.{M}.b` comment in the `{Op}FunctionalitySagas` class at the exact location where the data-assembly step will be inserted.
> 3. Flag the deferral explicitly in the session retro.
> 4. When session 2.{M}.b completes, revisit this saga and add the data-assembly step and service guard.
>
> This situation arises when there is a bidirectional dependency between two aggregates: the topological sort correctly prioritizes the event-subscription direction, and the P3 read-time reverse dependency is the deferred consequence. The plan.md for such rules should carry a ⚠️ DEFERRED marker (added by classify-and-plan) — if you see it, this guidance applies.

### `{Aggregate}CommandHandler.java`

Path: `{src}microservices/{aggregate}/messaging/{Aggregate}CommandHandler.java`

- Spring `@Component`
- One `@CommandHandler` method per command class produced in this session
- Each handler: creates a `UnitOfWork`, calls the matching service method, commits the `UnitOfWork`, returns result
- Routes each command to the corresponding service method

### One `{Op}{Aggregate}Command.java` per write functionality

Path: `commands/{aggregate}/{Op}{Aggregate}Command.java`

- Implements `Command`
- Fields: all parameters needed by the service method
- Constructor, getters
- Name convention: operation in PascalCase + aggregate name + `Command` (e.g., `CreateTournamentCommand`)

### One `{Op}FunctionalitySagas.java` per write functionality

Path: `{src}microservices/{aggregate}/coordination/sagas/{Op}FunctionalitySagas.java`

- Extends `FunctionalitySagas` (or the appropriate saga base class from the simulator core)
- Steps are defined as `SagaStep` instances in the constructor or `defineSteps()` method
- **Step ordering, lock-step pattern, R4 foreign-vs-primary distinction, R8 upstream-only rule, and compensation pairing** — follow `docs/concepts/sagas.md` § Step Ordering (and the linked § Lock-Acquisition Step Pattern, § R4 Decision Table). That section is authoritative; do not re-derive the order from the implementation.
- The **conditional validate-dates step** is per-aggregate guidance, not a generic pattern: if `plan.md` for this aggregate lists time-based invariants on both this aggregate and a downstream aggregate created in the same saga, insert the validate-dates step first.

### `{Aggregate}Functionalities.java`

Path: `{src}microservices/{aggregate}/coordination/functionalities/{Aggregate}Functionalities.java`

- Spring `@Service`
- One public method per write functionality (matching the saga class name)
- Each method:
  1. Derives `functionalityName` via `new Throwable().getStackTrace()[0].getMethodName()`
  2. Creates a `SagaUnitOfWork` with `unitOfWorkService.createUnitOfWork(functionalityName)`
  3. Instantiates the corresponding `{Op}FunctionalitySagas` directly (not as a Spring bean)
  4. Calls `executeWorkflow(uow)` on it
  5. Returns the result DTO (or `void` for mutations)
- Tests `@Autowired` this class and call its methods directly

### `{Aggregate}ServiceTest.groovy` (T2 — write-method cases)

Path: `{test}sagas/{aggregate}/{Aggregate}ServiceTest.groovy`

One class per aggregate, covering all its service methods (session `c` appends the read-method
cases). Follow the template in `docs/concepts/testing.md` § T2 — Service Test. Invoke the
`*Service` bean directly with a `UnitOfWork` — no saga workflow, no `{Aggregate}Functionalities`.

- Extends `{AppClass}SpockTest`
- **Per write service method, a happy path**: call the service with a fresh UnitOfWork, then read
  back **through a second, fresh UnitOfWork** (via the read service method or
  `aggregateLoadAndRegisterRead`) and assert the persisted fields. Reading back through the same
  UnitOfWork instance used for the write is **Fake** — it never exercises the load path.
- **Kill-mutation check (after each happy path):** Ask "if `unitOfWorkService.registerChanged(aggregate)`
  were removed from the service, would my test still pass?" If yes, the test does not verify
  persistence — the `then:` must read back through the fresh UnitOfWork, not from a local variable.
- **Uniqueness / composite-key guard cases**: one per P3 own-table or DTO-check guard in the
  service method.
- **P3 numeric-guard boundaries**: on-point (`notThrown`) / off-point (`thrown` + `ex.message ==
  {RULE_NAME}`) pairs per ordered-domain P3 guard (see testing.md § Choosing Input Values).
- **P1 intra-invariants are not tested here** — they belong in `{Aggregate}IntraInvariantTest.groovy`
  (session a, T1).
- `// Spec:` comment on every test naming the plan.md section and rule (see Spec-First note below).

### `{Aggregate}EventPublicationTest.groovy` (T3 — only if plan.md lists events published)

Path: `{test}sagas/{aggregate}/{Aggregate}EventPublicationTest.groovy`

Follow the template in `docs/concepts/testing.md` § T3 — Event Publication Test. Trigger the
publishing operation **via a direct service call** with a `UnitOfWork` (not via
`{Aggregate}Functionalities`), then assert against the event store via the `EventService` bean.

- **Per published event type** (from plan.md's Events published list): one case asserting the
  event exists with the correct type, `publisherAggregateId`, and **every payload field** —
  asserting only type/count is **Weak**.
- **One negative case**: capture the event-store count before, run a service operation that must
  *not* publish, assert the count is unchanged.
- Consumers are out of scope here — they are covered by T4 subscription tests in session `d`.

### One `{Op}Test.groovy` per write functionality (T4)

Path: `{test}sagas/coordination/{aggregate}/{Op}Test.groovy`

> **Anti-pattern:** Do not read `{Aggregate}Service.java` or `{Op}FunctionalitySagas.java` to decide what to assert. Tests derived from the implementation you just wrote are tautological — they verify what the code does, not what the domain says it should do. The remedy is the spec table below.

**Cite plan.md as the spec — do not author a parallel artifact.** The `plan.md` aggregate section for the target aggregate already contains the happy-path postconditions, the events-published list, and the P1/P3 rule list. That section *is* the spec; the test asserts it. See `docs/concepts/testing.md` § Spec-First Ordering.

At the top of every happy-path and violation test, write a single-line `// Spec:` comment that names the plan.md section and the rule (or "happy path") the test asserts. Example:

```groovy
def "updateQuestionContent: QUESTION_CONTENT_REQUIRED violation"() {
    // Spec: plan.md §3.5 Question / functionalities — UpdateQuestionContent; rule QUESTION_CONTENT_REQUIRED
    given:
    ...
}
```

If the implementation disagrees with the cited section, flag it as an impl deviation — do not adjust the cited rule to match.

**Strict assertion ownership (testing.md § Assertion Ownership):** T4 functionality tests do **not**
assert field-level persistence, uniqueness, or not-found — those belong in `{Aggregate}ServiceTest`
(T2, above). They also do not re-assert event-store contents — `{Aggregate}EventPublicationTest`
(T3) owns that.

- Extends `{AppClass}SpockTest`
- **Happy-path test**: set up prerequisites using `{AppClass}SpockTest` helpers, execute the operation via `{Aggregate}Functionalities`, and assert **orchestration outcomes only**: the operation completes, the returned DTO is coherent, and `sagaStateOf(<aggregateId>) == GenericSagaState.NOT_IN_SAGA`
- **Saga-path guard tests**: P3 guard violations that involve cross-aggregate saga coordination, driven through `{Aggregate}Functionalities` (single-aggregate guard violations are already covered in T2 via direct service calls — do not duplicate them here)
- **P4a prerequisite tests**: test what happens when the upstream fetch fails (e.g., creator not enrolled in execution)
- **Assertion for all violation tests:** `thrown({AppClass}Exception)` plus `ex.message == {RULE_NAME}`. Never use `thrown(Exception)` — the bare `Exception` is only acceptable in Fault / Behavior Test (Appendix) fault-injection tests. Never accept a bare `thrown({AppClass}Exception)` without the message assertion — it passes on any thrown exception of that type, including unrelated bugs. The `{RULE_NAME}` constant must match the name in `plan.md`'s rule list, not be inferred from the implementation.
- **P1 intra-invariants are not tested here** — they belong in `{Aggregate}IntraInvariantTest.groovy` (session a). Do not add P1 violation tests or BVA boundary straddles to T4 functionality tests.
- **State-transition / semantic-lock acquisition (required):** Follow `docs/concepts/testing.md` § T4 — Functionality Test. Each `setSemanticLock` step is an *acquire* transition into `IN_{OP}`. **One case per saga step that calls `setSemanticLock` — no exceptions:**
  - **`setSemanticLock` step:** run the workflow through the lock step via `executeUntilStep("<lockStep>", uow)`, assert `sagaStateOf(<id>) == <Aggregate>SagaState.IN_<OP>` in `expect:` (the post-*acquire* state), call `resumeWorkflow(uow)` in `when:`, assert `noExceptionThrown()` in `then:` (the traversal completes back to `NOT_IN_SAGA`).
  - Cross-aggregate `setForbiddenStates` conflict validation is **deferred — see Appendix — Cross-Functionality Test** in `docs/concepts/testing.md`.
  - **Coverage is audited mechanically.** List every `setSemanticLock` step (one row per call site) in the session retro's **Semantic-Lock Coverage Audit** table — see `.claude/skills/implement-aggregate/SKILL.md` Step 7.b. Unresolved `Present? = No` rows block the Step 8 commit.

### Event classes (if this aggregate publishes events)

For each event listed in plan.md Events published that does not yet exist:

Path: `{src}events/{Event}.java`

- Extends `pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event` from simulator core — it is a JPA `@Entity` and must be annotated `@Entity`. Do **NOT** implement `DomainEvent` directly.
- Fields: all payload fields needed by consumers (check aggregate-grouping §4 event table in domain spec for payload)
- Constructor, getters

### Error message constants

Open `{src}microservices/exception/{AppClass}ErrorMessage.java` and add constants for:
- P3 guard violations introduced in this session
- Any new invariant messages not already added in session `a`

Append to the existing file; do not remove existing constants.

### `{Aggregate}Controller.java`

Path: `{src}microservices/{aggregate}/coordination/webapi/{Aggregate}Controller.java`

- Single `@RestController` annotation; empty body
- Package: `pt.ulisboa.tecnico.socialsoftware.{pkg}.microservices.{aggregate}.coordination.webapi`
- No methods needed — REST endpoints are not exercised by the test harness; this stub marks the architectural slot
- Import: `org.springframework.web.bind.annotation.RestController`

---

## Update BeanConfigurationSagas.groovy

Open `{bean-config}` and add new `@Bean` methods for:

```groovy
@Bean
{Aggregate}Service {aggregate}Service(...) {  // inject repos and other services as needed
    return new {Aggregate}Service(...)
}

@Bean
{Aggregate}CommandHandler {aggregate}CommandHandler() {
    return new {Aggregate}CommandHandler()
}

@Bean
{Aggregate}Functionalities {aggregate}Functionalities() {
    return new {Aggregate}Functionalities()
}
```

**Note:** `{Op}FunctionalitySagas` classes are **not** Spring beans — they are instantiated inline inside `{Aggregate}Functionalities`. Only the three beans above are needed per aggregate.

Add the corresponding `import` statements. Place new beans after the beans added in session `a` for this aggregate.

---

## Update `{AppClass}SpockTest.groovy`

Open `{test}{AppClass}SpockTest.groovy` and add:

1. An `@Autowired(required = false)` field for the functionalities class:
   ```groovy
   @Autowired(required = false)
   protected {Aggregate}Functionalities {aggregate}Functionalities
   ```

2. A `create{Aggregate}(...)` helper method that calls `{aggregate}Functionalities.create{Aggregate}(...)` with a minimal valid DTO and returns the resulting aggregate ID. Tests use this helper in their `setup:` block to satisfy prerequisites.

---

## Tick the Checkbox

In plan.md, replace:
```
- [ ] 2.{N}.b — Write functionalities
```
with:
```
- [x] 2.{N}.b — Write functionalities
```
