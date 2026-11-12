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
   - **R6** - `verifyInvariants()` must not perform DB reads. This session writes it; the restriction is
     owned by `docs/architecture.md` § R6 and `docs/concepts/aggregate.md`.

2. **`docs/concepts/testing.md`** — § T1 — Aggregate Test. Note:
   - What the full T1 matrix covers: creation happy-path, one violation per non-`final` P1 rule, BVA straddles for ordered predicates
   - That all T1 cases go via direct construction/mutation + `verifyInvariants()` — never through the service
   - The test class location and naming convention

3. ***(Conditional)*** If the aggregate section in plan.md lists snapshot fields copied from an upstream aggregate (e.g., cached `warehouseId` from Warehouse, or `code`/`carrier` from Shipment): read the domain files of those upstream aggregates from `{src}microservices/{upstreamAggregate}/aggregate/` — only the field declarations you need to copy. Do not read the whole upstream codebase.

4. ***(Conditional)*** If this aggregate's `**Domain sentinels:**` line in plan.md names a constant declared by another aggregate, read `{src}microservices/domain/{AppClass}DomainConstants.java`. That constant is the **only** cross-aggregate value this session imports - snapshot fields are still copied, never shared.

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
    - **Collection** (`@OneToMany`): `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)` with **no** `mappedBy` — the aggregate is the owning side and the association is materialised as a join table; the owned entity declares no back-reference field. Do not add `mappedBy` here: there is no back-reference for it to name, and the persistence unit fails to initialise if you do
    - **Single** (`@OneToOne`): `@OneToOne(cascade = CascadeType.ALL, mappedBy = "{aggregate}")` — aggregate holds the inverse side; the entity class holds the FK via a plain `@OneToOne` back-reference. `mappedBy` names the back-reference field **on the entity** (`{aggregate}`), never the aggregate's own field holding the entity — naming the latter fails at EntityManagerFactory init with *"mappedBy reference an unknown target entity property"*. Whichever constructor or setter installs the entity must call `entity.set{Aggregate}(this)` to wire the bidirectional link before the entity is persisted; where the reference is immutable and therefore has no setter, that is the constructor
- Constructor: accepts all required fields; sets `state = ACTIVE`; does **not** call `verifyInvariants()` — the framework calls it automatically via `registerChanged` at commit time
- `verifyInvariants()`: enforces all **P1 rules** for this aggregate listed in plan.md. Throws `{AppClass}Exception` with the appropriate error message constant on violation. It reads only fields already held by the aggregate - never a repository, a service or any other DB access (R6 - see `docs/concepts/aggregate.md`). A predicate that compares against a fixed literal reads it from `{AppClass}DomainConstants` (see § "Domain sentinel constants") - never inline the literal here.
- `getEventSubscriptions()`: in session a, always return `new HashSet<>()` — do **not** reference any subscribe classes yet (they do not exist until session d). Session d will update this method to return the proper set of subscribe class instances.
- Getters and setters for all mutable fields
- A collection field additionally gets a plain `add{Element}` / `remove{Element}` helper alongside its getter and setter. These are setters, not business logic, so they belong to this session - a later session never re-opens the aggregate to add them, and the T1 test mutates the collection through them rather than through the getter's live list
- No business logic methods (sagas call service; service calls setters then verifyInvariants)

### Owned entity classes

For each entity owned by this aggregate (listed in plan.md aggregate section):

Path: `{src}microservices/{aggregate}/aggregate/{Entity}.java`

- `@Entity` + `@Table`; `@Id` auto-generated
- Fields matching the domain model
- Constructor, getters, setters
- **Bidirectional `@OneToOne` (aggregate → entity):** If the aggregate side uses `@OneToOne(mappedBy = "{aggregate}")`, this entity class holds the owning side: declare a plain `@OneToOne {Aggregate} {aggregate}` field (no `mappedBy`) with a getter/setter — this is the field `mappedBy` names. Whatever installs the entity on the aggregate (setter, or constructor when the reference is immutable) must call `entity.set{Aggregate}(this)` to wire the back-reference before persisting.
- **Nested entity-to-entity `@OneToOne` (entity owns a sub-entity):** When an owned entity itself exclusively owns one sub-entity (e.g., `ShipmentItem → ShipmentItemLabel`), use a unidirectional `@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)` on the outer entity — no `mappedBy`, no back-reference field on the sub-entity unless explicitly needed. The outer entity's copy constructor must deep-copy the sub-entity via `new SubEntity(existing.getSubEntity())`.

### Snapshot entity classes

Distinct from § "Owned entity classes" above, which covers only the §1 entities this aggregate owns
outright. A **snapshot** entity caches state belonging to another aggregate. Which §2 snapshots get a
class is decided by `/classify-and-plan` — see `.claude/skills/classify-and-plan/SKILL.md` § Step 3.d
and its restatement in the 2.N.a file-table notes — and every file it decides on is already named in
the plan.md `2.{N}.a` row. Three cases, and the row tells you which applies:

- a `× N` **collection** snapshot → `aggregate/{CollectionSnapshotEntity}.java` **and**
  `aggregate/{CollectionSnapshotEntity}Dto.java`;
- a **single** snapshot with a non-empty "Updated on event" →
  `aggregate/{SubscribingSnapshotEntity}.java`, no Dto;
- a **single** snapshot whose "Updated on event" is `n/a` → **no file at all**; it is cached as an id
  field and a version field directly on the aggregate.

Every version field named below is a `Long` — that typing is owned by
`docs/concepts/events.md` § ByEvent sagaState guard, "Every cached publisher version is a `Long`".
Only aggregate *ids* are `Integer`.

#### `{CollectionSnapshotEntity}.java`

Path: `{src}microservices/{aggregate}/aggregate/{CollectionSnapshotEntity}.java`

- `@Entity` + `@Table`; `@Id @GeneratedValue Integer id`
- `Integer {publisher}AggregateId` — which publisher aggregate this row caches
- One field per cached attribute named by the §2 row
- `Long {publisher}Version` — the publisher version the cached fields were taken at. Session `d`'s
  ByEvent path stamps it on every mutation that leaves the row in place
- No-arg constructor (JPA), a field constructor, and a **copy constructor**
  `{CollectionSnapshotEntity}({CollectionSnapshotEntity} other)` — the copy-on-write path deep-copies
  the collection through it
- **No back-reference field.** The aggregate side is `@OneToMany` with no `mappedBy`
  (§ `{Aggregate}.java` above), so the association is a join table and there is nothing for a
  back-reference to name
- Getters and setters

#### `{SubscribingSnapshotEntity}.java`

Path: `{src}microservices/{aggregate}/aggregate/{SubscribingSnapshotEntity}.java`

Same shape as above, with one difference: the aggregate holds it as
`@OneToOne(cascade = CascadeType.ALL, mappedBy = "{aggregate}")`, so this class holds the owning side
— declare a plain `@OneToOne {Aggregate} {aggregate}` field with getter and setter, exactly as
§ "Owned entity classes" describes for a bidirectional `@OneToOne`. **No Dto is produced for it.**

#### `{CollectionSnapshotEntity}Dto.java`

Path: `{src}microservices/{aggregate}/aggregate/{CollectionSnapshotEntity}Dto.java`

- Plain Java class — no JPA annotations. It is a DTO and therefore an immutable value object (R7 —
  see `docs/architecture.md` § R7)
- Field set mirrors the entity: `Integer {publisher}AggregateId`, the cached fields, and
  `Long {publisher}Version`
- **No-arg constructor**, an all-fields constructor, and a constructor from the entity. The no-arg
  constructor is mandatory: the test profile sets `local.messaging.serialize: true`, so anything
  reachable from a command round-trips through Jackson
- Getters and setters

This is what a saga's collection-valued data-assembly step constructs when it fetches one upstream DTO
per caller-supplied id — see `docs/concepts/sagas.md` § "Collection-valued data-assembly step". Do not
confuse it with § `{Aggregate}Dto.java` below, which is this aggregate's own DTO.

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
- **Transcribe the constants from the `**Saga states:**` line of this aggregate's plan.md section.**
  That line is computed by `/classify-and-plan` (§ Step 6.d), which has the whole write-functionality
  set and the whole dependency graph in front of it. Session `a` writes the domain layer before any
  saga exists, so deriving the set here would mean inferring the shape of sagas that later sessions —
  often for later aggregates — have not written yet. Take the list as given; do not add, drop or
  rename a constant. Each carries a one-line origin naming the saga that acquires it.
- If plan.md's line reads `none`, emit the enum with an **empty body**. That is the correct output for
  an aggregate whose only write functionality is a create, and the file is still produced — a later
  aggregate's session may add write functionalities that need it.
- If the `**Saga states:**` line is **absent** from the aggregate section, halt and report it rather
  than deriving a set. plan.md predating § Step 6.d is the likely cause, and a guessed enum surfaces
  as a missing or unreferenced constant only in a much later session's `c`.
- **Do not** include `NOT_IN_SAGA` — the initial state is set to `GenericSagaState.NOT_IN_SAGA` (from the framework) in the `Saga{Aggregate}` constructor. This enum only holds operation-specific locked states, and plan.md never lists it.
- **Do not** add a state for create sagas — `Create{Aggregate}` creates a new aggregate instance; there is no existing instance to lock. plan.md already applies this exclusion, so a create operation never appears in the transcribed list.
- **Do not** add a read state. A cross-aggregate step that only fetches this aggregate's DTO acquires no lock - `docs/concepts/sagas.md` § R4 Decision Table gives it `setForbiddenStates` - so no documented step shape ever assigns such a constant, and a `forbiddenStates` list naming it would be a permanent no-op.

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

- `@Repository @Transactional public interface {Aggregate}Repository extends JpaRepository<{Aggregate}, Integer>`
- **Type the repository against the concrete aggregate, never against the shared
  `AggregateRepository`.** `AggregateRepository` is declared over the abstract `Aggregate`, and
  `Aggregate` uses `InheritanceType.TABLE_PER_CLASS`, so any query it inherits is polymorphic and
  unions every aggregate's table: `findAll()` would return foreign aggregates, and the consumer that
  casts them to its own type fails with a `ClassCastException`. Extending `JpaRepository<{Aggregate},
  Integer>` scopes every query to one physical table by construction.
- Because the repository no longer inherits from `AggregateRepository`, redeclare both of its
  methods here, typed to `{Aggregate}`:

```java
@Query(value = "select a1 from {Aggregate} a1 where a1.aggregateId = :aggregateId AND a1.state = 'ACTIVE' AND a1.version = (select max(a2.version) from Aggregate a2 where a2.aggregateId = :aggregateId)")
Optional<{Aggregate}> findLastAggregateVersion(Integer aggregateId);

Optional<{Aggregate}> findTopByOrderByVersionDesc();
```

  The subquery stays `from Aggregate a2` — the version counter is global across aggregate types.
- No other custom queries needed here (those go in `CustomRepositorySagas`)

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
- **One violation test per non-`final` P1 rule** (from this aggregate's `plan.md` P1 list): construct or mutate a `Saga{Aggregate}` so that exactly one P1 predicate fails, then call `verifyInvariants()` directly and assert `thrown({AppClass}Exception)` with `ex.message == {RULE_NAME}` (the harness-wide assertion form - `docs/concepts/testing.md` § T1 - Aggregate Test, and `docs/concepts/service.md` § Exception-Throw Convention for why exceptions are thrown without format arguments). Skip rules marked as Java `final` fields (compiler-enforced; no write path can violate them - note the omission in the session report). Where the rule compares against a domain sentinel, build the violating value from the constant rather than retyping the literal, so the case fails if the constant is ever changed without the rule.
- **Boundary straddle for every ordered-domain P1 predicate** (count, timestamp, or collection-size comparison — `<`/`<=`/`>`/`>=`/`==`): write the on-point and off-point pair against `verifyInvariants()`, per `docs/concepts/testing.md` § Choosing Input Values — EP & BVA. Categorical rules (uniqueness, boolean/state freezes, set membership) keep their single representative case.
- **Do not** use `{AppClass}Functionalities.create{Aggregate}(...)` — write functionalities are not available until session b. All T1 cases use direct construction/mutation + `verifyInvariants()`.
- If the aggregate constructor takes `{Aggregate}Dto` rather than raw args, build the DTO in the `given:` block before calling `new Saga{Aggregate}(id, dto)`

### Error message constants

Open `{src}microservices/exception/{AppClass}ErrorMessage.java` and add one `public static final String` constant per P1 rule enforced in `verifyInvariants()` for this aggregate. Append to the existing file; do not remove existing constants.

### Domain sentinel constants

Path: `{src}microservices/domain/{AppClass}DomainConstants.java`

Produce this file only if plan.md's `**Domain sentinels:**` line for this aggregate lists at least one
constant to declare. Transcribe each listed entry verbatim as a `public static final` field. Create the
file if it does not exist yet (private constructor, as in `{AppClass}ErrorMessage`); append otherwise,
and never remove an existing constant. If the line reads `none declared` this aggregate is a
*consumer*: produce nothing, and import the class where the predicate needs it. If it reads `none.`,
produce nothing at all.

If the `**Domain sentinels:**` line is **absent** from the aggregate section, halt and report it
rather than deciding a placement here. plan.md predating § Step 6.e is the likely cause, and the
consequence of guessing is invisible: an inlined literal type-checks and every test passes, right up
until a later aggregate's predicate compares against its own second copy.

A sentinel is a fixed literal that a write functionality assigns to a field and that some P1
predicate compares against - usually a P1 rule of a **later** aggregate, reading its own cached
snapshot of this one. It is declared in a shared, aggregate-neutral package so that neither side
depends on the other's microservice package, and so that one literal has one home.

Three placements that look defensible are wrong, and each fails differently:

- **Not on `{Aggregate}.java`.** It compiles, but the consuming aggregate must then import
  `...microservices.{thisAggregate}.aggregate.{Aggregate}` from its own microservice package - the
  one source dependency between microservices in an otherwise separately-deployable app.
- **Not on `{Aggregate}Dto.java`.** The Dto is a transport shape, rebuilt per read; a domain value
  does not belong to it.
- **Not in `{AppClass}SpockTest.groovy`.** That constants block is test-fixture values for T1
  (§ "Update {AppClass}SpockTest.groovy" below); a sentinel is production code, and a copy in the
  test tree would not be visible to `verifyInvariants()` at all.

Re-declaring the same literal on both aggregates is also wrong, and is the one failure with no
symptom: snapshot *fields* are deliberately copied because each copy carries its own version stamp,
but a sentinel carries no version. Two copies that drift do not throw - the predicate silently stops
matching and every test stays green.

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

The session checkbox for this session is `- [ ] 2.{N}.a — Domain layer`. Read
`_shared/session-completion.md` § "Tick the checkbox" in full and follow it. Do not continue until
you have. It owns the whole rule, including how to anchor on the session line rather than doing a
bare string replace, and what the manager and single-agent topologies each do about the slice
sub-checkboxes underneath it.
