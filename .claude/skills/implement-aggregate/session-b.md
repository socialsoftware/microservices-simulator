# Session 2.N.b — Read Functionalities

This sub-file is loaded by `implement-aggregate` when the target session type is `b`. All context variables (`{app-name}`, `{pkg}`, `{AppClass}`, `{Aggregate}`, `{N}`, `{src}`, `{test}`, `{bean-config}`) are already available from the router.

> **Slice scope.** You may be assigned a subset of this session's items. If your brief names specific
> functionalities or events, implement only those, and append to the shared files rather than
> rewriting them. If no subset is named, you own the whole session.

> **If the plan.md aggregate section lists "Read functionalities: none"**, this session is still not empty: produce the boilerplate `Get{Aggregate}ByIdCommand`, its `{Aggregate}Service` read method, its `{Aggregate}CommandHandler` case and its T2 not-found test, and no *domain read* artifacts beyond these. There are no domain read functionalities to add on top, so no `{Query}FunctionalitySagas`, no coordinator read methods beyond `get{Aggregate}ById`, and no `{Query}Test.groovy`.

---

## Reads

Load these files before writing any code:

1. **Domain files produced in session 2.{N}.a** — specifically: `{Aggregate}.java`, `{Aggregate}Dto.java`. You need the field names and DTO constructor signature.

2. **`docs/architecture.md`** — § package layout for this aggregate. This session creates `{Aggregate}Service.java`, `{Aggregate}CommandHandler.java` and `{Aggregate}Functionalities.java`; session 2.{N}.c appends its write methods to all three.

3. **`docs/concepts/service.md`** — specifically:
   - § Method Patterns → Read method
   - § Custom Repository — Latest-Active-Version Query (only if the read returns a collection)
   - **R3** - cross-aggregate state flows as DTOs, never as aggregate instances (`docs/architecture.md`
     § R3, restated in § Injected Dependencies). A read method returns this aggregate's own DTO.
   - **R7** - DTOs are immutable value objects; never call a setter on one (`docs/architecture.md` § R7,
     `service.md` § DTO Immutability).

4. **`docs/concepts/commands.md`** — specifically:
   - § What a Command Is, § Naming Conventions (read-command form: `Get{Aggregate}By{Field}Command`)
   - § Routing Commands (CommandHandler) — adding a new case for the read command

5. **`docs/concepts/sagas.md`** — specifically:
   - § Read Functionality Sagas, and its § List-return read variant when the read returns a collection.
     For § Two-step read saga variant the criterion is § One-step vs two-step decision, below in this file.
   - **R8** - a functionality may only send commands to aggregates upstream of its own. A read saga is no
     exception (`docs/architecture.md` § R8, `docs/concepts/commands.md`).

6. **`docs/concepts/testing.md`** — § T2 — Service Test (including § Not-Found Paths for the Path A / Path B rule of thumb), § T4 — Functionality Test, and § Assertion Ownership. T1 (aggregate) and T3 subscription (inter-invariant) tests are not produced in this session.

7. ***(Conditional)*** If any read functionality joins data from an upstream aggregate (e.g., a "get with details" that includes Warehouse name alongside Shipment): read that upstream aggregate's service file to understand what it returns.

---

## Produce

Produce every file listed in the plan.md `2.{N}.b` row. plan.md is a blueprint, not a manifest: the `###` subheadings below are the authority on what this session must emit, and a file they require but plan.md omits is still produced - amend the row per `_shared/session-completion.md` § "Amend plan.md for omitted files".

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
- If the read needs data from a foreign aggregate, the service does **not** fetch it. The saga resolves it in a preceding step and passes the DTO in as a parameter — see § "Two-step read saga variant" below, and `docs/concepts/service.md` § Injected Dependencies (R1/R2/R3). Injecting a foreign `*Service` to fetch it here violates R2.
- **List-return reads**: If the read returns a collection (e.g., all open shipments for a warehouse), the service method iterates all matching aggregate instances. Use a JPQL "latest-active-version" query rather than `jpaRepo.findAll()` — `findAll()` returns every historical version, not just the current one. Add `findAllLatestActive()` (or a narrower variant) to the JPA repository interface and call it from `{Aggregate}CustomRepositorySagas`. See `docs/concepts/service.md` — "Custom Repository — Latest-Active-Version Query" for the JPQL pattern.

  The service method then maps each matching aggregate to a DTO via `aggregateLoadAndRegisterRead`.

### One `{Query}Command.java` per read functionality

Path: `{src}commands/{aggregate}/{Query}Command.java` — rooted at the **app source root**, not at
`microservices/{aggregate}/`, so the same command class can be sent by other aggregates' sagas. See
`docs/concepts/commands.md` § "File Location".

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
> - **One step** — when every filter criterion is stored directly on the aggregate (e.g., `warehouseAggregateId` is a field on `Shipment`), **or when there is no filter at all** (a read returning every instance of its type). The saga sends one command and returns the result; no foreign-ID resolution is needed. An unfiltered read takes no id parameter anywhere and its command passes `null` as `rootAggregateId` — see `docs/concepts/sagas.md` § "Unfiltered variant — no filter parameter at all".
> - **Two steps** — when the filter parameter is a foreign aggregate's ID that must be resolved to a different field before the primary query can run (e.g., `shipmentId → warehouseAggregateId`).
>
> **Two-step read saga:** If the read's filter parameter is a foreign aggregate's ID that must be resolved before the primary read command can be sent (e.g., `shipmentId → warehouseAggregateId`), use a two-step saga instead:
> - Step 1: fetch the foreign aggregate DTO (plain read step, no compensation needed)
> - Step 2: send the primary read command using the resolved field from step 1 (declare step 1 as a dependency)
>
> No compensation is needed on either step since reads are non-mutating. See `docs/concepts/sagas.md` — "Two-step read saga variant" section for the full class template.
>
> Step 1 fetches the foreign aggregate through a command sent **upstream** - to an aggregate this one
> already depends on (R8 - see `docs/concepts/commands.md`); never downstream to an aggregate that
> depends on this one. The DTO it returns is read as-is and never mutated
> (see `docs/concepts/service.md` § DTO Immutability (R7)).

> **Deferred reads:** If a read functionality carries a ⚠️ DEFERRED marker in plan.md, it reads an
> aggregate ordered _after_ this one, whose service does not exist yet. The marker is added by
> `classify-and-plan` § "Step 5.5b". Do the following:
> 1. **Skip it entirely** — produce no `{Query}Command.java`, no `{Query}FunctionalitySagas.java`, no
>    `{Query}Test.groovy`, and no service method. Do not add stubs; a stub that compiles is worse than
>    an absent file, because nothing later forces it to be revisited.
> 2. Implement **every other** read for this aggregate as normal and tick the session checkbox. A
>    deferred read does not hold up session `b`.
> 3. Flag the deferral explicitly in the session retro.
> 4. When the blocking aggregate's session `c` completes, revisit this session and add the skipped
>    files. plan.md carries a matching "revisit" note in that aggregate's section.
>
> This is the read-side counterpart of § "Deferred P3 guards" in
> [`session-c.md`](session-c.md), and arises for the same reason: the topological sort follows
> event-subscription edges, and a read-time dependency running the other way is its deferred
> consequence.

### `{Aggregate}Functionalities.java` (read methods)

Path: `{src}microservices/{aggregate}/coordination/functionalities/{Aggregate}Functionalities.java`

> **Always required**, even if `{Aggregate}Functionalities.java` is not listed in the plan.md `2.{N}.b` file table. This session **creates** the class; session 2.{N}.c appends the write coordinator methods to it.

- Spring `@Service`
- One method per read functionality
- The method creates a `SagaUnitOfWork`, instantiates the `{Query}FunctionalitySagas` inline, calls `executeWorkflow`, and returns the DTO via `saga.get{Aggregate}Dto()`
- **Name the unit of work with a string literal** matching the method name —
  `unitOfWorkService.createUnitOfWork("get{Aggregate}ById")`. Session `c` appends its write
  coordinators to this same class under the same rule, so the file carries one idiom throughout.
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
- **Happy-path test only**: create the aggregate using the `create{Aggregate}(...)` helper this session adds to `{AppClass}SpockTest` (see § Update `{AppClass}SpockTest.groovy`), execute the read via `{Aggregate}Functionalities`, assert the returned DTO matches the aggregate's state
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
  - **Collection reads have neither path.** A read returning a list has no id that can fail to resolve: an empty result is a valid answer, not an error. Write an **empty-result** case instead — run the read with no matching aggregate present and assert the returned list is empty. Do not invent a not-found exception for it.

---

## Update BeanConfigurationSagas.groovy

Open `{bean-config}` and add new `@Bean` methods for the three classes this session creates.

The service's dependencies are a **closed list**, fixed by `docs/concepts/service.md` § Injected
Dependencies: own repository, own custom repository, own factory, `UnitOfWorkService` (raw, no type
argument), `AggregateIdGeneratorService`. Nothing else — a foreign service or foreign repository
violates R1/R2; cross-aggregate data reaches this service as a DTO passed in by a saga, never by
injecting the other aggregate's components (R3). Inject factories and repositories through their
abstract interfaces, never the concrete `Sagas*` classes. Every one of them goes through the
constructor into a `final` field; a service declares no `@Autowired` field.

Omit any of the five the service genuinely does not use — typically `AggregateIdGeneratorService`,
which no read method needs. Session 2.{N}.c's create method is then the first to need it, and widens
both this `@Bean` method and the service constructor to match.

That list is split two ways, and the bean method must match the split: `UnitOfWorkService`, the
repository and the custom repository are **constructor** parameters, while the factory and
`AggregateIdGeneratorService` are `@Autowired` **fields** on the service and therefore do not appear
in the bean method at all. Passing all five to the constructor does not compile against the service
`docs/concepts/service.md` prescribes.

```groovy
@Bean
{Aggregate}Service {aggregate}Service(SagaUnitOfWorkService unitOfWorkService,
                                      {Aggregate}Repository {aggregate}Repository,
                                      {Aggregate}CustomRepository {aggregate}CustomRepository) {
    return new {Aggregate}Service(unitOfWorkService, {aggregate}Repository, {aggregate}CustomRepository)
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

The fixture helper below calls `aggregateIdGeneratorService`, which the scaffolded base class does
**not** declare. Add it too, once per application - the first aggregate's session `b` adds it and
later aggregates reuse it:

```groovy
@Autowired(required = false)
protected AggregateIdGeneratorService aggregateIdGeneratorService
```

Then add the `create{Aggregate}(...)` fixture helper. This session's T2 and T4 tests need a persisted
aggregate to read back, but the create functionality does not exist until session 2.{N}.c, so the
helper is built **directly on the aggregate** here:

```groovy
Integer create{Aggregate}(/* minimal valid args, defaulted to the domain constants */) {
    def {aggregate} = new Saga{Aggregate}(aggregateIdGeneratorService.getNewAggregateId(), /* args */)
    unitOfWorkService.registerChanged({aggregate}, unitOfWorkService.createUnitOfWork("fixture"))
    return {aggregate}.getAggregateId()
}
```

Not every parameter can be defaulted. A parameter carrying a **foreign aggregate's id** has no
domain constant to default to - the id is minted at fixture time by whichever upstream helper
created that aggregate, and differs per test. Such parameters stay **required and undefaulted, and
come first** in the signature, ahead of the defaulted own-field parameters; the caller passes the
id returned by the upstream fixture helper:

```groovy
Integer create{Aggregate}(Integer {foreign}AggregateId, {Field} {field} = {FIELD_CONSTANT}) { ... }
```

Only the aggregate's **own** fields get constant defaults.

`registerChanged` merges the aggregate immediately, so no `commit` is needed for the read-back to
resolve through a fresh `UnitOfWork`.

**The signature is a contract with session 2.{N}.c**, which replaces this body with the real create
functionality. Choose the parameter list and defaults so that the call sites written this session
survive that swap unchanged: minimal valid arguments, own-field parameters defaulted to the domain
constants and foreign-aggregate-id parameters required and leading, aggregate id returned. Do **not** name it `persist{Aggregate}` or make it `private` to a test class — a
per-test-class fixture is thrown away in 2.{N}.c and every call site has to be rewritten.

### Fixture state a create cannot reach

`create{Aggregate}` builds a **minimal valid** aggregate, so its owned collections come out empty. A
read functionality that filters on a field of an owned entity - `Get{Aggregate}sBy{Element}({foreign}AggregateId)`
selecting over `{Aggregate}.{elements}` - therefore cannot be tested through `create{Aggregate}`
alone, because only a session-2.{N}.c write functionality can populate that collection.

Do **not** widen `create{Aggregate}` to carry the collection, and do **not** populate the collection
inline in a test class. Instead add **one sibling helper per write functionality the session-`b`
reads depend on**, named after that functionality and built directly on the aggregate exactly as
`create{Aggregate}` is:

```groovy
void {operation}{Aggregate}(Integer {aggregate}AggregateId, Integer {foreign}AggregateId) {
    def unitOfWork = unitOfWorkService.createUnitOfWork("fixture")
    def {aggregate} = new Saga{Aggregate}((Saga{Aggregate}) unitOfWorkService.aggregateLoadAndRegisterRead(
            {aggregate}AggregateId, unitOfWork))
    {aggregate}.add{Element}(new {Element}({foreign}AggregateId, /* snapshot fields */))
    unitOfWorkService.registerChanged({aggregate}, unitOfWork)
}
```

The same signature contract binds these helpers: parameters minimal, foreign-aggregate-id parameters
required and leading, and the signature unchanged when 2.{N}.c replaces the body with the real
functionality.

**The foreign id must be minted by the upstream aggregate's own fixture helper, never a domain
constant** - the same rule as for `create{Aggregate}`, and for a sharper reason here. This helper's
2.{N}.c replacement calls a write functionality that *fetches* that foreign aggregate and runs its P3
guards against it, so a synthetic id names an aggregate that does not exist and every call site
throws on the swap. The upstream aggregate must also already be in whatever state those guards
require: if the functionality rejects an inactive counterpart, the fixture activates it first. Where
no upstream helper produces that state yet, add one, in the same base class and under this same
contract. Keeping `create{Aggregate}` minimal is what makes that swap safe - a collection
parameter on the create helper would have no counterpart in the create functionality and would force
a signature change in 2.{N}.c, rewriting every call site.

---

## Tick the Checkbox

The session checkbox for this session is `- [ ] 2.{N}.b — Read functionalities`. Read
`_shared/session-completion.md` § "Tick the checkbox" in full and follow it. Do not continue until
you have. It owns the whole rule, including how to anchor on the session line rather than doing a
bare string replace, and what manager mode and single-agent mode each do about the slice
sub-checkboxes underneath it.
