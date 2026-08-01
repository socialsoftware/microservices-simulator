# Session 2.N.b — Read Functionalities

This sub-file is loaded by `implement-aggregate` when the target session type is `b`. All context variables (`{app-name}`, `{pkg}`, `{AppClass}`, `{Aggregate}`, `{N}`, `{src}`, `{test}`, `{bean-config}`) are already available from the router.

> **If the plan.md aggregate section lists "Read functionalities: none"**, this session is still not empty: produce the boilerplate `Get{Aggregate}ByIdCommand`, its `{Aggregate}Service` read method, its `{Aggregate}CommandHandler` case and its T2 not-found test, and nothing else. There are no domain read functionalities to add on top, so no `{Query}FunctionalitySagas`, no coordinator read methods beyond `get{Aggregate}ById`, and no `{Query}Test.groovy`.

---

## Reads

Load these files before writing any code:

1. **Domain files produced in session 2.{N}.a** — specifically: `{Aggregate}.java`, `{Aggregate}Dto.java`. You need the field names and DTO constructor signature.

2. **`docs/architecture.md`** — § package layout for this aggregate. This session creates `{Aggregate}Service.java`, `{Aggregate}CommandHandler.java` and `{Aggregate}Functionalities.java`; session 2.{N}.c appends its write methods to all three.

3. **`docs/concepts/service.md`** — specifically:
   - § Method Patterns → Read method
   - § Custom Repository — Latest-Active-Version Query (only if the read returns a collection)

4. **`docs/concepts/commands.md`** — specifically:
   - § What a Command Is, § Naming Conventions (read-command form: `Get{Aggregate}By{Field}Command`)
   - § Routing Commands (CommandHandler) — adding a new case for the read command

5. **`docs/concepts/sagas.md`** — specifically:
   - § Read Functionality Sagas (and its subsections § List-return read variant, § Two-step read saga variant, as applicable)

6. **`docs/concepts/testing.md`** — § T2 — Service Test (including § Not-Found Paths for the Path A / Path B rule of thumb), § T4 — Functionality Test, and § Assertion Ownership. T1 (aggregate) and T3 subscription (inter-invariant) tests are not produced in this session.

7. ***(Conditional)*** If any read functionality joins data from an upstream aggregate (e.g., a "get with details" that includes Course name alongside Execution): read that upstream aggregate's service file to understand what it returns.

---

## Produce

Produce every file listed in the plan.md `2.{N}.b` row. plan.md is a blueprint, not a manifest: the `###` subheadings below are the authority on what this session must emit, and a file they require but plan.md omits is still produced - amend the row per `SKILL.md` § "Step 5b: Amend plan.md for Omitted Files".

`Get{Aggregate}ByIdCommand` is produced unconditionally for **every** aggregate, whether or not §4
lists any read functionality for it. Write sagas need it for their get-then-lock step, so it is
infrastructure rather than a domain read. Session `b` is therefore never empty.

> **Prerequisite — ServiceMapping**: Verify that `{src}ServiceMapping.java` exists and contains an entry for `{AGGREGATE}`. If not, create it (or add the missing entry) before writing any commands — every command constructor references `ServiceMapping.{AGGREGATE}.getServiceName()`. This session is the first to create commands for this aggregate, so the entry lands here.
>
> **Multi-word aggregate naming:** The value must equal `resolveServiceName(Saga{Aggregate})`, which strips "Saga" from the aggregate's simple class name and lowercases the first character. For multi-word aggregates this is camelCase — a `SagaShipmentItem` resolves to `"shipmentItem"`. Never use a shortened alias such as `"item"`; mismatches cause silent bean-lookup failures that only appear at commit/abort time.

### `{Aggregate}Service.java` (read methods)

Path: `{src}microservices/{aggregate}/service/{Aggregate}Service.java`

- Spring `@Service`. This session **creates** the class; session 2.{N}.c appends the write methods to it.
- One method per read functionality listed in plan.md, plus `get{Aggregate}ById` unconditionally
- Method signature: receives query parameters (ids, filters) + `UnitOfWork unitOfWork`
- Body — follow `docs/concepts/service.md` § Method Patterns → Read method:
  - **By primary key (the normal case):** `{aggregate}Factory.create{Aggregate}Dto(({Aggregate}) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork))`. Do not add a not-found guard — the infrastructure throws `SimulatorException` when the ID does not resolve. This is the Path A that the T2 not-found case below asserts.
  - **By composite / non-PK key:** query `{Aggregate}CustomRepository`, and throw `{AppClass}Exception` with the domain-specific not-found constant when the `Optional` is empty. This is Path B.
- If the read joins a foreign aggregate: fetch the foreign aggregate's DTO via its service and include in the response
- **List-return reads**: If the read returns a collection (e.g., all open tournaments for an execution), the service method iterates all matching aggregate instances. Use a JPQL "latest-active-version" query rather than `jpaRepo.findAll()` — `findAll()` returns every historical version, not just the current one. Add `findAllLatestActive()` (or a narrower variant) to the JPA repository interface and call it from `{Aggregate}CustomRepositorySagas`. See `docs/concepts/service.md` — "Custom Repository — Latest-Active-Version Query" for the JPQL pattern.

  The service method then maps each matching aggregate to a DTO via `aggregateLoadAndRegisterRead`.

### One `{Query}Command.java` per read functionality

Path: `commands/{aggregate}/{Query}Command.java`

`Get{Aggregate}ByIdCommand` is always one of them, even when plan.md lists no read functionality.

- Implements `Command`
- Fields: all parameters needed by the service read method (e.g., `aggregateId`, filter fields)
- Constructor, getters
- Name convention: `Get{Aggregate}By{Field}Command` or similar (match plan.md file list exactly)

### One `{Query}FunctionalitySagas.java` per read functionality

Path: `{src}microservices/{aggregate}/coordination/sagas/{Query}FunctionalitySagas.java`

- Extends `WorkflowFunctionality` (`pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality`) — the same base class as write sagas; there is no separate read base class
- Constructor calls `buildWorkflow(...)`, which assigns `this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork)` and registers the step
- Single step: send `{Query}Command` to `{Aggregate}CommandHandler`, store the result DTO in an instance field
- Provide a getter for the result DTO
- No compensation needed (reads are non-mutating)
- See `docs/concepts/sagas.md` — "Read Functionality Sagas" section for the full class template

> **One-step vs two-step read saga decision:**
> - **One step** — when every filter criterion is stored directly on the aggregate (e.g., `executionAggregateId` is a field on `Tournament`). The saga sends one command and returns the result; no foreign-ID resolution is needed.
> - **Two steps** — when the filter parameter is a foreign aggregate's ID that must be resolved to a different field before the primary query can run (e.g., `executionId → courseAggregateId`).
>
> **Two-step read saga:** If the read's filter parameter is a foreign aggregate's ID that must be resolved before the primary read command can be sent (e.g., `executionId → courseAggregateId`), use a two-step saga instead:
> - Step 1: fetch the foreign aggregate DTO (plain read step, no compensation needed)
> - Step 2: send the primary read command using the resolved field from step 1 (declare step 1 as a dependency)
>
> No compensation is needed on either step since reads are non-mutating. See `docs/concepts/sagas.md` — "Two-step read saga variant" section for the full class template.

### `{Aggregate}Functionalities.java` (read methods)

Path: `{src}microservices/{aggregate}/coordination/functionalities/{Aggregate}Functionalities.java`

> **Always required**, even if `{Aggregate}Functionalities.java` is not listed in the plan.md `2.{N}.b` file table. This session **creates** the class; session 2.{N}.c appends the write coordinator methods to it.

- Spring `@Service`
- One method per read functionality
- The method creates a `SagaUnitOfWork`, instantiates the `{Query}FunctionalitySagas` inline, calls `executeWorkflow`, and returns the DTO via `saga.get{Aggregate}Dto()`
- Tests `@Autowired` this class and call its methods directly

### `{Aggregate}CommandHandler.java`

Path: `{src}microservices/{aggregate}/messaging/{Aggregate}CommandHandler.java`

> **Always required**, even if the file is not listed in the plan.md `2.{N}.b` file table. This session **creates** the class with its read cases; session 2.{N}.c appends the write cases.

- Spring `@Component`, extends `CommandHandler` (`pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler`)
- Exactly two overrides, per `docs/concepts/commands.md` § Routing Commands (CommandHandler):
  `getAggregateTypeName()` returning the PascalCase aggregate name, and a single
  `handleDomainCommand(Command command)` holding one `switch` case per command class plus a
  `default` branch that logs a warning
- The Spring **bean name** must be `ServiceMapping.{AGGREGATE}.getServiceName() + "CommandHandler"` (lowercase camelCase) — that is the actual routing key
- One `case` in `handleDomainCommand` for each read command
- Delegate to a matching private handler method that calls the corresponding service read method
- Pattern:
  ```java
  case Get{Aggregate}By{Field}Command cmd -> handleGet{Aggregate}By{Field}(cmd);
  ```
  ```java
  private Object handleGet{Aggregate}By{Field}(Get{Aggregate}By{Field}Command command) {
      return {aggregate}Service.get{Aggregate}By{Field}(command.get{Field}(), command.getUnitOfWork());
  }
  ```

---

### One `{Query}Test.groovy` per read functionality (T4)

Path: `{test}sagas/coordination/{aggregate}/{Query}Test.groovy`

- Extends `{AppClass}SpockTest`
- **Happy-path test only**: create the aggregate using the `{AppClass}SpockTest` helper (or directly), execute the read via `{Aggregate}Functionalities`, assert the returned DTO matches the aggregate's state
- **No not-found cases here** — per `docs/concepts/testing.md` § Assertion Ownership, not-found belongs to T2 (next section)

### `{Aggregate}ServiceTest.groovy` (T2 — read-method cases)

Path: `{test}sagas/{aggregate}/{Aggregate}ServiceTest.groovy`

This session **creates** the class with its read-method cases; session 2.{N}.c appends the
write-method and event-publication cases. For each read service method added this session:

- **Happy read-back**: call the read service method directly with a fresh `UnitOfWork` on an existing aggregate, assert the returned DTO fields
- **Not-found tests** — two paths (see `docs/concepts/testing.md` § T2 — Service Test → Not-Found Paths):
  - **Path A (PK load):** service calls `aggregateLoadAndRegisterRead` with a non-existent ID → assert `thrown(SimulatorException)`
  - **Path B (composite/custom-repo lookup):** service queries a custom repository returning `Optional` and throws on empty → assert `thrown({AppClass}Exception)` with `ex.message == <NOT_FOUND_CONSTANT>`
  - **Rule of thumb:** read the service method first — if it calls `aggregateLoadAndRegisterRead` directly with an ID, use Path A; if it first calls a custom repository returning `Optional`, use Path B.

---

## Update BeanConfigurationSagas.groovy

Open `{bean-config}` and add new `@Bean` methods for the three classes this session creates:

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

**Note:** `{Query}FunctionalitySagas` classes are **not** Spring beans — they receive a `SagaUnitOfWork` in their constructor and are instantiated *inline* inside coordinator methods, so they are per-request objects, not Spring singletons. Only the three beans above are needed per aggregate; session 2.{N}.c adds none.

Add the corresponding `import` statements. Place new beans after the beans added in session `a` for this aggregate.

---

## Update `{AppClass}SpockTest.groovy`

Open `{test}{AppClass}SpockTest.groovy` and add an `@Autowired(required = false)` field for the functionalities class:

```groovy
@Autowired(required = false)
protected {Aggregate}Functionalities {aggregate}Functionalities
```

The `create{Aggregate}(...)` helper is added in session 2.{N}.c, which is where the create functionality is implemented.

---

## Tick the Checkbox

In plan.md, replace:
```
- [ ] 2.{N}.b — Read functionalities
```
with:
```
- [x] 2.{N}.b — Read functionalities
```
