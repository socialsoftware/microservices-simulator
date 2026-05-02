# Session 2.N.c — Read Functionalities

This sub-file is loaded by `implement-aggregate` when the target session type is `c`. All context variables (`{app-name}`, `{pkg}`, `{AppClass}`, `{Aggregate}`, `{N}`, `{src}`, `{test}`, `{bean-config}`) are already available from the router.

> **If the plan.md aggregate section lists "Read functionalities: none"**, there is nothing to do for this session. Skip to ticking the checkbox (if one exists) or advancing to session `d`. Report to the user that this aggregate has no read functionalities.

---

## Reads

Load these files before writing any code:

1. **Domain files produced in session 2.{N}.a** — specifically: `{Aggregate}.java`, `{Aggregate}Dto.java`. You need the field names and DTO constructor signature.

2. **Service file produced in session 2.{N}.b** — `{Aggregate}Service.java`. You will append read methods to this file; understanding its existing structure is required.

3. **`docs/concepts/service.md`** — the read-operation section specifically. Note:
   - How a read service method fetches the aggregate and maps it to a DTO
   - When a read method joins data from a foreign aggregate (fetch that aggregate's DTO too)

4. **`docs/concepts/commands.md`** — the read-command section. Note:
   - Naming convention for read commands (`Get{Aggregate}By{Field}Command`, etc.)
   - How read commands differ from write commands (no `UnitOfWork` side-effects)

5. **`docs/concepts/sagas.md`** — the read-functionality section. Note:
   - Read functionalities are single-step (no compensation needed)
   - How the saga step fetches data and returns it

6. **`docs/concepts/testing.md`** — T2 section only (specifically the read-functionality test subsection). Note:
   - What a T2 read test asserts (DTO fields match aggregate state)
   - How to test not-found cases

7. ***(Conditional)*** If any read functionality joins data from an upstream aggregate (e.g., a "get with details" that includes Course name alongside Execution): read that upstream aggregate's service file to understand what it returns.

---

## Produce

Produce every file listed in the plan.md `2.{N}.c` row. The authoritative file list is in plan.md — use it exactly.

### Read methods appended to `{Aggregate}Service.java`

Path: `{src}microservices/{aggregate}/service/{Aggregate}Service.java`

> **Pre-emption check:** If plan.md notes that `Get{Aggregate}ByIdCommand` and the service read method were moved to session `b` (because a write saga needed to fetch the aggregate for semantic lock acquisition), **skip this step and the command step below**. Still produce the `FunctionalitySagas` class, the coordinator method in `{Aggregate}Functionalities.java`, and the test.

- **Append** new methods to the existing service class — do not rewrite the file
- One method per read functionality listed in plan.md
- Method signature: receives query parameters (ids, filters) + `UnitOfWork unitOfWork`
- Body: fetch aggregate via `{Aggregate}CustomRepositorySagas.getLatestVersion(...)`, map to `{Aggregate}Dto`, return it
- Throw `{AppClass}Exception` with `AGGREGATE_NOT_FOUND` (or domain-specific constant) if not found
- If the read joins a foreign aggregate: fetch the foreign aggregate's DTO via its service and include in the response
- **List-return reads**: If the read returns a collection (e.g., all topics for a course), the service method iterates all aggregate instances. When no suitable query method exists on `{Aggregate}CustomRepository`, add `findXxxIdsBy{Field}(Integer fieldValue)` to the interface and implement it in `{Aggregate}CustomRepositorySagas` via:
  ```java
  jpaRepo.findAll().stream()
      .filter(a -> fieldValue.equals(a.get{Field}()))
      .map({Aggregate}::getAggregateId)
      .distinct()
      .collect(Collectors.toList())
  ```
  The service method then maps each id to a DTO via `aggregateLoadAndRegisterRead`.

### One `{Query}Command.java` per read functionality

Path: `commands/{aggregate}/{Query}Command.java`

> **Pre-emption check:** Skip if plan.md moved this command to session `b` (see note above).

- Implements `Command`
- Fields: all parameters needed by the service read method (e.g., `aggregateId`, filter fields)
- Constructor, getters
- Name convention: `Get{Aggregate}By{Field}Command` or similar (match plan.md file list exactly)

### One `{Query}FunctionalitySagas.java` per read functionality

Path: `{src}microservices/{aggregate}/coordination/sagas/{Query}FunctionalitySagas.java`

- Extends the read-functionality base class from simulator core
- Single step: send `{Query}Command` to `{Aggregate}CommandHandler`, store the result DTO in an instance field
- Provide a getter for the result DTO
- No compensation needed (reads are non-mutating)
- See `docs/concepts/sagas.md` — "Read Functionality Sagas" section for the full class template

### Read method appended to `{Aggregate}Functionalities.java`

Path: `{src}microservices/{aggregate}/coordination/functionalities/{Aggregate}Functionalities.java`

> **Always required**, even if `{Aggregate}Functionalities.java` is not listed in the plan.md `2.{N}.c` file table (it may have been created in session `b` and therefore appears there instead). Read the file, append the method, update it.

- **Append** one method per read functionality — do not rewrite the file
- The method creates a `SagaUnitOfWork`, instantiates the `{Query}FunctionalitySagas` inline, calls `executeWorkflow`, and returns the DTO via `saga.get{Aggregate}Dto()`
- Follow the same pattern as the write coordinator methods added in session `b`

### Update `{Aggregate}CommandHandler`

Path: `{src}microservices/{aggregate}/messaging/{Aggregate}CommandHandler.java`

> **Always required** — append one case per new read command added in this session, even if the file is not listed in the plan.md `2.{N}.c` file table.

- **Append** one `case` in `handleDomainCommand` for each new read command
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

### One `{Query}Test.groovy` per read functionality (T2)

Path: `{test}sagas/coordination/{aggregate}/{Query}Test.groovy`

- Extends `{AppClass}SpockTest`
- **Happy-path test**: create the aggregate using the `{AppClass}SpockTest` helper (or directly), execute the read, assert the returned DTO matches the aggregate's state
- **Not-found test**: execute the read with a non-existent id, assert `SimulatorException` is thrown (the infrastructure throws `SimulatorException` when an aggregate ID does not exist — not the app-level exception; the app exception is only thrown by domain/service guards)

---

## BeanConfigurationSagas — No Change Needed

`FunctionalitySagas` classes receive a `SagaUnitOfWork` in their constructor and are instantiated *inline* inside coordinator methods — they are per-request objects, not Spring singletons. The `{Aggregate}Functionalities` coordinator bean was already registered in `{bean-config}` during session `b`. **Do not add any `@Bean` method for `FunctionalitySagas` classes.**

---

## Tick the Checkbox

In plan.md, replace:
```
- [ ] 2.{N}.c — Read functionalities
```
with:
```
- [x] 2.{N}.c — Read functionalities
```
