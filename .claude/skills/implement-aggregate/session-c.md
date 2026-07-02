# Session 2.N.c — Read Functionalities

This sub-file is loaded by `implement-aggregate` when the target session type is `c`. All context variables (`{app-name}`, `{pkg}`, `{AppClass}`, `{Aggregate}`, `{N}`, `{src}`, `{test}`, `{bean-config}`) are already available from the router.

> **If the plan.md aggregate section lists "Read functionalities: none"**, there is nothing to do for this session. Skip to ticking the checkbox (if one exists) or advancing to session `d`. Report to the user that this aggregate has no read functionalities.

---

## Reads

Load these files before writing any code:

1. **Domain files produced in session 2.{N}.a** — specifically: `{Aggregate}.java`, `{Aggregate}Dto.java`. You need the field names and DTO constructor signature.

2. **Service file produced in session 2.{N}.b** — `{Aggregate}Service.java`. You will append read methods to this file; understanding its existing structure is required.

3. **`docs/concepts/service.md`** — specifically:
   - § Method Patterns → Read method
   - § Custom Repository — Latest-Active-Version Query (only if the read returns a collection)

4. **`docs/concepts/commands.md`** — specifically:
   - § What a Command Is, § Naming Conventions (read-command form: `Get{Aggregate}By{Field}Command`)
   - § Routing Commands (CommandHandler) — adding a new case for the read command

5. **`docs/concepts/sagas.md`** — specifically:
   - § Read Functionality Sagas (and its subsections § List-return read variant, § Two-step read saga variant, as applicable)

6. **`docs/concepts/testing.md`** — § T2 — Service Test (including § Not-Found Paths for the Path A / Path B rule of thumb), § T4 — Functionality Test, and § Assertion Ownership. T1 (aggregate) and T4 subscription (inter-invariant) tests are not produced in this session.

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
- **List-return reads**: If the read returns a collection (e.g., all open tournaments for an execution), the service method iterates all matching aggregate instances. Use a JPQL "latest-active-version" query rather than `jpaRepo.findAll()` — `findAll()` returns every historical version, not just the current one. Add `findAllLatestActive()` (or a narrower variant) to the JPA repository interface and call it from `{Aggregate}CustomRepositorySagas`. See `docs/concepts/service.md` — "Custom Repository — Latest-Active-Version Query" for the JPQL pattern.

  The service method then maps each matching aggregate to a DTO via `aggregateLoadAndRegisterRead`.

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

> **One-step vs two-step read saga decision:**
> - **One step** — when every filter criterion is stored directly on the aggregate (e.g., `executionAggregateId` is a field on `Tournament`). The saga sends one command and returns the result; no foreign-ID resolution is needed.
> - **Two steps** — when the filter parameter is a foreign aggregate's ID that must be resolved to a different field before the primary query can run (e.g., `executionId → courseAggregateId`).
>
> **Two-step read saga:** If the read's filter parameter is a foreign aggregate's ID that must be resolved before the primary read command can be sent (e.g., `executionId → courseAggregateId`), use a two-step saga instead:
> - Step 1: fetch the foreign aggregate DTO (plain read step, no compensation needed)
> - Step 2: send the primary read command using the resolved field from step 1 (declare step 1 as a dependency)
>
> No compensation is needed on either step since reads are non-mutating. See `docs/concepts/sagas.md` — "Two-step read saga variant" section for the full class template.

### Read method appended to `{Aggregate}Functionalities.java`

Path: `{src}microservices/{aggregate}/coordination/functionalities/{Aggregate}Functionalities.java`

> **Always required**, even if `{Aggregate}Functionalities.java` is not listed in the plan.md `2.{N}.c` file table (it may have been created in session `b` and therefore appears there instead). Read the file, append the method, update it.

- **Append** one method per read functionality — do not rewrite the file
- **Exception — stub upgrade:** If session `b` placed a direct-service call (e.g., `return {aggregate}Service.get{Aggregate}ById(...)`) in this coordinator as a temporary stub for test wiring, **replace the body** of that existing method with the full saga-based implementation. Do not append a duplicate method; the signature stays the same, only the body changes.
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

### One `{Query}Test.groovy` per read functionality (T4)

Path: `{test}sagas/coordination/{aggregate}/{Query}Test.groovy`

- Extends `{AppClass}SpockTest`
- **Happy-path test only**: create the aggregate using the `{AppClass}SpockTest` helper (or directly), execute the read via `{Aggregate}Functionalities`, assert the returned DTO matches the aggregate's state
- **No not-found cases here** — per `docs/concepts/testing.md` § Assertion Ownership, not-found belongs to T2 (next section)

### Read-method T2 cases appended to `{Aggregate}ServiceTest.groovy`

Path: `{test}sagas/{aggregate}/{Aggregate}ServiceTest.groovy` (created in session `b` — **append**, do not rewrite)

For each read service method added this session:

- **Happy read-back**: call the read service method directly with a fresh `UnitOfWork` on an existing aggregate, assert the returned DTO fields (skip if the method is already exercised as the read-back of a session-`b` write happy path)
- **Not-found tests** — two paths (see `docs/concepts/testing.md` § T2 — Service Test → Not-Found Paths):
  - **Path A (PK load):** service calls `aggregateLoadAndRegisterRead` with a non-existent ID → assert `thrown(SimulatorException)`
  - **Path B (composite/custom-repo lookup):** service queries a custom repository returning `Optional` and throws on empty → assert `thrown({AppClass}Exception)` with `ex.message == <NOT_FOUND_CONSTANT>`
  - **Rule of thumb:** read the service method first — if it calls `aggregateLoadAndRegisterRead` directly with an ID, use Path A; if it first calls a custom repository returning `Optional`, use Path B.

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
