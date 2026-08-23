# Session 2.N.a — Domain Layer

This sub-file is loaded by `implement-aggregate` when the target session type is `a`. All context variables (`{app-name}`, `{pkg}`, `{AppClass}`, `{Aggregate}`, `{N}`, `{src}`, `{test}`, `{bean-config}`) are already available from the router.

> **Slice scope.** You may be assigned a subset of this session's items. If your brief names specific
> functionalities or events, implement only those, and append to the shared files rather than
> rewriting them. If no subset is named, you own the whole session.

---

## Reads

Load these files before writing any code:

1. **`docs/concepts/aggregate.md`** — all sections. Specifically use:
   - § Key Fields, § Base Class — base-class fields and lifecycle methods
   - § Variants → Sagas variant — `SagaAggregate` interface; what `getSagaState()` / `setSagaState()` must return
   - § Factories, § Repositories — the three artifacts this session produces, and why
     `{Aggregate}CustomRepositorySagas` is a `@Service` implementing `{Aggregate}CustomRepository`
     rather than an extension of `SagaAggregateRepository`
   - § getEventSubscriptions() Implementation — relevant only if this aggregate has subscribed events (otherwise skip)
   - References to `prev` (used for temporal invariants) appear under § Key Fields / § Base Class
   - **R6** — `verifyInvariants()` must not perform DB reads. This session writes it; the restriction is
     owned by `docs/architecture.md` § R6 and `docs/concepts/aggregate.md`.

2. **`docs/concepts/testing.md`** — § T1 — Aggregate Test. Note:
   - What the full T1 matrix covers: creation happy-path, one violation per non-`final` P1 rule, BVA straddles for ordered predicates
   - That all T1 cases go via direct construction/mutation + `verifyInvariants()` — never through the service
   - The test class location and naming convention

3. ***(Conditional)*** If the aggregate section in plan.md lists snapshot fields copied from an upstream aggregate (e.g., cached `warehouseId` from Warehouse, or `code`/`carrier` from Shipment): read the domain files of those upstream aggregates from `{src}microservices/{upstreamAggregate}/aggregate/` — only the field declarations you need to copy. Do not read the whole upstream codebase.

---

## Verify Mandatory Files in plan.md

Before producing any files, check the plan.md `2.{N}.a` row for this aggregate and verify that **both** of the following interface files are listed:

- `aggregate/{Aggregate}Factory.java`
- `aggregate/{Aggregate}CustomRepository.java`

If either is missing, add it to the plan.md `2.{N}.a` file cell now. These two files are mandatory for every aggregate — `{Aggregate}Factory.java` because the service layer and `BeanConfigurationSagas.groovy` always inject the factory interface, and `{Aggregate}CustomRepository.java` because `{Aggregate}CustomRepositorySagas.java` always implements it. Neither may be omitted regardless of whether the aggregate has cross-table lookups.

---

## Produce

Produce every file listed in the plan.md `2.{N}.a` row for this aggregate. plan.md is a blueprint, not a manifest: the `###` subheadings below are the authority on what this session must emit, and a file they require but plan.md omits is still produced - amend the row per `_shared/session-completion.md` § "Amend plan.md for omitted files". The descriptions below explain what each file must contain.

### `{Aggregate}.java`

Path: `{src}microservices/{aggregate}/aggregate/{Aggregate}.java`

- Extends `Aggregate` (from the simulator core)
- JPA annotated (`@Entity`, `@Table`, `@Id`, etc.)
- Contains all fields defined in the domain model for this aggregate, including:
  - Snapshot fields copied from other aggregates (cached denormalized data)
  - Owned entity fields — choose based on cardinality:
    - **Collection** (`@OneToMany`): `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)` — unidirectional and **owning**, mapped through a join table. Do **not** add `mappedBy`: there is no back-reference on the entity to name, and the entity class declares no field pointing at the aggregate
    - **Single** (`@OneToOne`): `@OneToOne(cascade = CascadeType.ALL, mappedBy = "{entityField}")` — aggregate holds the inverse side; the entity class holds the FK via a plain `@OneToOne` back-reference. The aggregate's setter must call `entity.set{Aggregate}(this)` to wire the bidirectional link before the entity is persisted
- Constructor: accepts all required fields; sets `state = ACTIVE`; does **not** call `verifyInvariants()` — the framework calls it automatically via `registerChanged` at commit time
- `verifyInvariants()`: enforces all **P1 rules** for this aggregate listed in plan.md. Throws `{AppClass}Exception` with the appropriate error message constant on violation.
- `getEventSubscriptions()`: in session a, always return `new HashSet<>()` — do **not** reference any subscribe classes yet (they do not exist until session d). Session d will update this method to return the proper set of subscribe class instances.
- Getters and setters for all mutable fields
- No business logic methods (sagas call service; service calls setters then verifyInvariants)

### Owned entity classes

For each entity owned by this aggregate (listed in plan.md aggregate section):

Path: `{src}microservices/{aggregate}/aggregate/{Entity}.java`

- `@Entity` + `@Table`; `@Id` auto-generated
- Fields matching the domain model
- Constructor, getters, setters
- **Bidirectional `@OneToOne` (aggregate → entity):** If the aggregate side uses `@OneToOne(mappedBy = "{entityField}")`, this entity class holds the owning side: declare a plain `@OneToOne {Aggregate} {aggregate}` field (no `mappedBy`) with a getter/setter. The aggregate's setter for this entity must call `entity.set{Aggregate}(this)` to wire the back-reference before persisting.
- **Nested entity-to-entity `@OneToOne` (entity owns a sub-entity):** When an owned entity itself exclusively owns one sub-entity (e.g., `ShipmentItem → ShipmentItemLabel`), use a unidirectional `@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)` on the outer entity — no `mappedBy`, no back-reference field on the sub-entity unless explicitly needed. The outer entity's copy constructor must deep-copy the sub-entity via `new SubEntity(existing.getSubEntity())`.

### Domain enums

Every aggregate field typed as a domain enum gets its own enum file, listed in the plan.md `2.{N}.a`
row by `/classify-and-plan`:

Path: `{src}microservices/{aggregate}/aggregate/{DomainEnum}.java`, unless the plan.md row marks the
enum `(shared)`, in which case the row gives `{src}enums/{DomainEnum}.java` instead - the app-root
package for an enum type that more than one aggregate's §1 attributes name. Follow the path in the
row; do not re-derive it.

- Plain Java `enum` - no JPA annotations
- Values matching the domain model
- Name taken verbatim from the domain-model attribute's type

A `(shared)` enum is written once, by the earliest session whose row lists it, and imported unchanged
by every later one: check whether the file already exists before writing it, and if it does, leave it
alone. Never copy it into your aggregate's own package - the copies become distinct Java types and a
value can no longer cross an aggregate boundary without conversion.

An aggregate with no enum-typed field has no such row and produces none.

### `Saga{Aggregate}.java`

Path: `{src}microservices/{aggregate}/aggregate/sagas/Saga{Aggregate}.java`

- Annotated `@Entity`. This is the **concrete** class of the aggregate hierarchy, and the abstract
  `{Aggregate}` above it holds the `@Table`, so under `TABLE_PER_CLASS` this is the only class
  Hibernate can map a row to. Omit it and every intra-invariant test still passes - they never
  persist - while the first write in session `b`/`c` fails with
  *"Unable to locate persister: ...Saga{Aggregate}"*
- Extends `{Aggregate}`, implements `SagaAggregate`
- Adds a `sagaState` field of type `SagaAggregate.SagaState` (the interface), annotated
  `@Convert(converter = SagaStateConverter.class)`. The converter
  (`pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter`) is declared
  `@Converter` **without** `autoApply = true`, so it is not picked up implicitly: omit the
  `@Convert` and Hibernate fails at EntityManagerFactory init with *"Could not determine recommended
  JdbcType"*, taking down every test in the application
- **Two constructors**, and they differ in how they seed `sagaState`:
  - **Creating constructor** `Saga{Aggregate}(Integer aggregateId, ...)` — delegates to super and
    initializes `sagaState` to `GenericSagaState.NOT_IN_SAGA`. A brand-new aggregate is quiescent.
  - **Copy constructor** `Saga{Aggregate}(Saga{Aggregate} other)` — delegates to super and
    **inherits** `other.getSagaState()`. This is the constructor
    `Sagas{Aggregate}Factory.create{Aggregate}Copy` calls, so it runs on every copy-on-write write
    while a saga may be holding a semantic lock. `verifySagaState` and `abortAggregate` both resolve
    through `findNonDeletedSagaAggregate`, i.e. the **newest version row**, so resetting to
    `NOT_IN_SAGA` here would silently release a held lock at the locking saga's first write and turn
    every downstream `forbiddenStates` guard into a no-op. Inheriting cannot strand a lock:
    `commitAggregate` sets `NOT_IN_SAGA` unconditionally.
- Implements `getSagaState()` returning the field; `setSagaState(SagaState state)` sets it directly (field type is the interface, no cast needed)
- No other logic

> `SagaAggregate` is a bare interface declaring only `getSagaState()` / `setSagaState()`; the
> framework prescribes nothing about copy semantics, so the rule above is a harness decision rather
> than something derivable from `simulator/`.

> **Bean naming constraint:** The simple class name of this saga aggregate determines the service routing string used by `resolveServiceName()` in session b. For multi-word aggregates this is camelCase, not a shortened alias — a `SagaShipmentItem` produces `"shipmentItem"`, never `"item"`. The `ServiceMapping` entry created in session b **must** use this exact value. Note it now to avoid a silent routing failure later.

### `{Aggregate}SagaState.java`

Path: `{src}microservices/{aggregate}/aggregate/sagas/states/{Aggregate}SagaState.java`

- Enum implementing `SagaState`
- **Do not** include `NOT_IN_SAGA` — the initial state is set to `GenericSagaState.NOT_IN_SAGA` (from the framework) in the `Saga{Aggregate}` constructor. This enum only holds operation-specific locked states.
- **One locked state per mutating saga this aggregate owns**, named `IN_{OPERATION}_{AGGREGATE}`
  after the operation rather than after a fixed CRUD verb - `IN_UPDATE_{AGGREGATE}` and
  `IN_DELETE_{AGGREGATE}` are just the two commonest instances, and an aggregate with several
  domain-specific mutating operations gets one constant for each. This is the state the saga's
  lock-acquisition step passes to `setSemanticLock(...)`, and every two-step write saga has one:
  `docs/concepts/sagas.md` § "Lock-Acquisition Step Pattern (Two-Step Write Sagas)" and § "Step
  Ordering" item 3 require the lock step regardless of how many steps follow it. Read the
  aggregate's write functionalities in plan.md and emit one constant per non-create operation.
- **Do not** add a state for create sagas — `Create{Aggregate}` creates a new aggregate instance; there is no existing instance to lock
- **Do not** add a read state. A cross-aggregate step that only fetches this aggregate's DTO acquires no lock — `docs/concepts/sagas.md` § R4 Decision Table gives it `setForbiddenStates` — so no documented step shape ever assigns such a constant, and a `forbiddenStates` list naming it would be a permanent no-op.

### `{Aggregate}Factory.java` (interface)

Path: `{src}microservices/{aggregate}/aggregate/{Aggregate}Factory.java`

- Plain Java interface — no annotations
- Three methods typed against the abstract aggregate and DTO (no sagas-specific types in the signature):
  - `{Aggregate} create{Aggregate}(Integer aggregateId, ...)` — returns `{Aggregate}` (abstract base)
  - `{Aggregate} create{Aggregate}Copy({Aggregate} existing)` — returns `{Aggregate}`
  - `{Aggregate}Dto create{Aggregate}Dto({Aggregate} {aggregate})` — returns the DTO

### `Sagas{Aggregate}Factory.java`

Path: `{src}microservices/{aggregate}/aggregate/sagas/factories/Sagas{Aggregate}Factory.java`

- `@Service @Profile("sagas") public class Sagas{Aggregate}Factory implements {Aggregate}Factory`
- Three methods implementing the interface — may use covariant return types (`Saga{Aggregate}`) but must cast internally when needed:
  - `create{Aggregate}(aggregateId, ...)` — `return new Saga{Aggregate}(...)` (override)
  - `create{Aggregate}Copy({Aggregate} existing)` — cast to `Saga{Aggregate}` inside: `return new Saga{Aggregate}((Saga{Aggregate}) existing)` (override)
  - `create{Aggregate}Dto({Aggregate} {aggregate})` — `return new {Aggregate}Dto({aggregate})` (override)
- The service layer injects `{Aggregate}Factory` (the interface), never `Sagas{Aggregate}Factory` directly.

### `{Aggregate}CustomRepository.java` (interface)

Path: `{src}microservices/{aggregate}/aggregate/{Aggregate}CustomRepository.java`

- Plain Java interface — no annotations
- Declare only the custom query method signatures needed by the service (no Spring Data JPA auto-magic here — implementations provide JPQL). For aggregates with no cross-table lookups, the interface body can be empty.
- The service layer injects this interface, never the concrete sagas class.

### `{Aggregate}CustomRepositorySagas.java`

Path: `{src}microservices/{aggregate}/aggregate/sagas/repositories/{Aggregate}CustomRepositorySagas.java`

- `@Service @Profile("sagas") public class {Aggregate}CustomRepositorySagas implements {Aggregate}CustomRepository`
- Has an `@Autowired {Aggregate}Repository {aggregate}Repository` field
- **Leave the class body empty** beyond the autowired repository. Session `a` has no service methods yet, so which queries are needed is not yet knowable. Sessions `b` and `c` add a method here when a service method they write requires one — each addition paired with the matching signature on `{Aggregate}CustomRepository`.

### `{Aggregate}Repository.java`

Path: `{src}microservices/{aggregate}/aggregate/{Aggregate}Repository.java`

- Interface extending `AggregateRepository` — **no type arguments**; the framework interface is not
  generic (`interface AggregateRepository extends JpaRepository<Aggregate, Integer>`)
- No custom queries needed here (custom queries go in `CustomRepositorySagas`)

### `{Aggregate}Dto.java`

Path: `{src}microservices/{aggregate}/aggregate/{Aggregate}Dto.java`

- Plain Java class (no JPA annotations)
- Fields matching the aggregate's public surface (what other aggregates or tests need to read)
- Constructor from `{Aggregate}`, all-fields constructor, and no-arg constructor
- Getters and setters

### `{Aggregate}IntraInvariantTest.groovy` (T1)

Path: `{test}sagas/{aggregate}/{Aggregate}IntraInvariantTest.groovy`

See `docs/concepts/testing.md` § T1 — Aggregate Test for the full remit and templates.

- Extends `{AppClass}SpockTest`
- **Happy-path creation test**: `def "create {Aggregate}"()` — instantiate `Saga{Aggregate}` directly, call `verifyInvariants()`, and assert all fields from the `plan.md` aggregate field list. Assertion provenance: fields must trace to the spec, not to the constructor body you just wrote. If the constructor sets a field the spec doesn't list, flag the planning gap in the session report.
- **One violation test per non-`final` P1 rule** (from this aggregate's `plan.md` P1 list): construct or mutate a `Saga{Aggregate}` so that exactly one P1 predicate fails, then call `verifyInvariants()` directly and assert `thrown({AppClass}Exception)` with `ex.message == {RULE_NAME}` (the harness-wide assertion form — `docs/concepts/testing.md` § T1 — Aggregate Test, and `docs/concepts/service.md` § Exception-Throw Convention for why exceptions are thrown without format arguments). Skip rules marked as Java `final` fields (compiler-enforced; no write path can violate them — note the omission in the session report).
- **Boundary straddle for every ordered-domain P1 predicate** (count, timestamp, or collection-size comparison — `<`/`<=`/`>`/`>=`/`==`): write the on-point and off-point pair against `verifyInvariants()`, per `docs/concepts/testing.md` § Choosing Input Values — EP & BVA. Categorical rules (uniqueness, boolean/state freezes, set membership) keep their single representative case.
- **Do not** use `{AppClass}Functionalities.create{Aggregate}(...)` — write functionalities are not available until session b. All T1 cases use direct construction/mutation + `verifyInvariants()`.
- If the aggregate constructor takes `{Aggregate}Dto` rather than raw args, build the DTO in the `given:` block before calling `new Saga{Aggregate}(id, dto)`

### Error message constants

Open `{src}microservices/exception/{AppClass}ErrorMessage.java` and add one `public static final String` constant per P1 rule enforced in `verifyInvariants()` for this aggregate. Append to the existing file; do not remove existing constants.

### `{Aggregate}ServiceApplication.java`

Path: `{src}microservices/{aggregate}/{Aggregate}ServiceApplication.java`

- `@Profile("{aggregate}-service")` — gates activation to the named profile only; does not activate during normal `mvn test` runs
- `@SpringBootApplication(scanBasePackages = {"pt.ulisboa.tecnico.socialsoftware.{pkg}.microservices.{aggregate}", "pt.ulisboa.tecnico.socialsoftware.ms"})`
- Same two packages for `@EnableJpaRepositories` and `@EntityScan`
- `@EnableScheduling`
- Implements `InitializingBean`; `@Autowired EventService eventService`; calls `eventService.clearEventsAtApplicationStartUp()` in `afterPropertiesSet()`
- `main` method calls `SpringApplication.run({Aggregate}ServiceApplication.class, args)`

---

## Update {AppClass}SpockTest.groovy

Path: `{test}{AppClass}SpockTest.groovy`

The bootstrap scaffold ships this class with only marker comments where domain content goes. Session
`a` is the first session allowed to add to it, and must, because the T1 happy-path test asserts on
every aggregate field and needs literals to assert against. Insert at the markers, leaving them in
place for later sessions:

- `// Domain constants are added here` - one `public static final` constant per field value this
  aggregate's T1 cases use, named `{AGGREGATE}_{FIELD}` (and `{AGGREGATE}_{FIELD}_2` and similar for
  the second distinct value a uniqueness or straddle case needs). Literals belong here, not inlined
  in the test.
- `// Domain @Autowired fields are added here` - only what session `a` itself needs, which for a
  pure domain-layer session is normally nothing. The factory, repository, service and functionalities
  fields are added by the sessions that create those classes.
- `// Domain imports` - the imports the two additions above require.

Do not add `create{Aggregate}` helpers here; the read session (2.{N}.b) introduces one, and the write
session (2.{N}.c) swaps its body onto the real create functionality. Session `a` has no test that
needs a persisted aggregate — T1 constructs the aggregate directly.

---

## Update BeanConfigurationSagas.groovy

Open `{bean-config}` and add two new `@Bean` methods:

```groovy
@Bean
Sagas{Aggregate}Factory sagas{Aggregate}Factory() {
    return new Sagas{Aggregate}Factory()
}

@Bean
{Aggregate}CustomRepositorySagas {aggregate}CustomRepositorySagas() {
    return new {Aggregate}CustomRepositorySagas()
}
```

Add the corresponding `import` statements for both classes. Place new bean methods after the existing bean methods for previous aggregates, before the closing `}` of the class.

---

## Tick the Checkbox

In plan.md, replace:
```
- [ ] 2.{N}.a — Domain layer
```
with:
```
- [x] 2.{N}.a — Domain layer
```
