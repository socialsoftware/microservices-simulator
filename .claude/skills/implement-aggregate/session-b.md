# Session 2.N.b — Write Functionalities

This sub-file is loaded by `implement-aggregate` when the target session type is `b`. All context variables (`{app-name}`, `{pkg}`, `{AppClass}`, `{Aggregate}`, `{N}`, `{src}`, `{test}`, `{bean-config}`) are already available from the router.

---

## Reads

Load these files before writing any code:

1. **Domain files produced in session 2.{N}.a** — read every file listed in the plan.md `2.{N}.a` row for this aggregate. You need the aggregate class structure, field names, constructor signature, and `SagaState` enum values.

2. **`docs/concepts/service.md`** — the full file. Pay attention to:
   - How the service layer is structured (create / mutate / read patterns)
   - Where P3 service guards are placed (own-table uniqueness checks; DTO field validation)
   - How the `UnitOfWork` is used to register events and persist changes
   - How a service method receives saga-assembled DTOs vs. fetching from the repository itself

3. **`docs/concepts/commands.md`** — the full file. Pay attention to:
   - `Command` structure and field conventions
   - How `CommandHandler` routes commands to service methods
   - The `ServiceMapping` wiring

4. **`docs/concepts/sagas.md`** — the full file. Pay attention to:
   - Saga step anatomy: `executeStep` / `compensateStep` pairs
   - `setForbiddenStates` — where to call it and why (R4 semantic lock: blocks concurrent operations that would conflict with this one)
   - How data-assembly steps fetch DTOs from upstream aggregates
   - How the final step calls the service and commits the `UnitOfWork`
   - Compensation: which steps need compensation and what they do

5. **`docs/concepts/testing.md`** — T2 section only (Write Functionality Tests). Note:
   - What a T2 test asserts (state after the operation, events emitted, error cases)
   - How to test P3 guard violations
   - How to test P4a prerequisite failures (e.g., sending a command that causes the saga fetch to fail)

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
- **Step ordering** (typical pattern):
  1. *(Conditional)* **Validate-dates step** — if the saga creates or updates an aggregate with `startTime`/`endTime` fields **and** a later step also creates/updates a downstream aggregate that independently validates dates (e.g., a Quiz), add a dedicated `validateDatesStep` as the very first step to check date constraints on the primary aggregate's DTO. If you omit this step, the downstream aggregate's date invariant fires first and masks the primary aggregate's date error, making the wrong exception surface to tests.
  2. Data-assembly steps: fetch DTOs from upstream aggregates (required for P4a and P3 DTO-check rules listed in plan.md cross-aggregate prerequisites)
  3. **Primary lock step** — wrap the read command in `SagaCommand`, call `setSemanticLock(state)` on the primary aggregate, and register a compensation that releases the lock with `GenericSagaState.NOT_IN_SAGA`. See `docs/concepts/sagas.md` § Lock-Acquisition Step Pattern. Do **not** use `setForbiddenStates` for primary-aggregate lock acquisition.
  4. Execute step: send a plain (unwrapped) command to `{Aggregate}CommandHandler`; declare the lock step as a dependency
  5. *(For multi-aggregate sagas)* Steps for other aggregates involved — use `setForbiddenStates` only on steps that touch a **foreign** aggregate to abort if that aggregate is mid-saga (see `docs/concepts/sagas.md` § R4 decision table)
- Each data-assembly step that enforces a **P4a rule**: if the upstream command/query fails (throws), the prerequisite is considered violated — no extra guard needed in the service
- **R8 — upstream-only commands**: sagas for this aggregate may only send commands to aggregates that are upstream (i.e., aggregates this one depends on, not aggregates that depend on it). Never dispatch a write command to a downstream aggregate from within this aggregate's sagas.
- Each `executeStep` that can fail has a matching `compensateStep` that rolls back the change

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

### One `{Op}Test.groovy` per write functionality (T2)

Path: `{test}sagas/coordination/{aggregate}/{Op}Test.groovy`

- Extends `{AppClass}SpockTest`
- **Happy-path test**: set up prerequisites using `{AppClass}SpockTest` helpers, execute the operation, assert the resulting aggregate state
- **P3 guard tests**: test each P3 rule violation (own-table duplicate, DTO field check)
- **P4a prerequisite tests**: test what happens when the upstream fetch fails (e.g., creator not enrolled in execution)
- **Assertion for all violation tests:** `thrown({AppClass}Exception)`. Never use `thrown(Exception)` — the bare `Exception` is only acceptable in T5 fault-injection tests where any infrastructure error is valid.
- **P1 invariant violation tests**: for each P1 rule that a write operation can put at risk, add a test that exercises the service method causing the violation. The service calls `registerChanged`, which automatically invokes `verifyInvariants` — **never call `verifyInvariants()` directly**.
  - **Skip P1 tests for `final` fields:** If a P1 rule is enforced by a Java `final` field (plan.md note: `Java \`final\` field`), no write path can violate it. Omit the invariant test for that rule and note the omission in the session report.

### Service-command method tests (T2 variant)

After writing the coordinator-level T2 tests above, scan `{Aggregate}Service.java` for any
additional methods that call `registerChanged` but are **not** exposed through
`{Aggregate}Functionalities`. These are invoked via commands from other aggregates' sagas
(e.g., `decrementExecutionCount` called when a related aggregate is deleted).

For each such method:
1. Write T2-style tests (happy path + invariant violations) — no saga step interleaving needed.
2. If a mirror method is missing (e.g., `incrementExecutionCount` exists in the reference app but
   not here), add it now — it will be needed when the other aggregate's saga is implemented and
   is required for invariant-violation test setup.
3. Place the test file in `{test}sagas/coordination/{aggregate}/`, naming it
   `{OperationName}Test.groovy` or combining related operations into one file
   (e.g., `{Aggregate}CountsTest.groovy`) when they share setup.

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
