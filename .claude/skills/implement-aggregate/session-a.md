# Session 2.N.a — Domain Layer

This sub-file is loaded by `implement-aggregate` when the target session type is `a`. All context variables (`{app-name}`, `{pkg}`, `{AppClass}`, `{Aggregate}`, `{N}`, `{src}`, `{test}`, `{bean-config}`) are already available from the router.

---

## Reads

Load these files before writing any code:

1. **`docs/concepts/aggregate.md`** — the full file. Pay attention to:
   - `Aggregate` base class fields and lifecycle methods
   - `verifyInvariants()` contract (called after every state change; throws on violation)
   - `SagaAggregate` interface and what `getSagaState()` / `setSagaState()` must return
   - `CustomRepositorySagas` — `getLatestVersion` and `findSagaAggregateById` queries
   - How `prev` is used for temporal invariants

2. **`docs/concepts/testing.md`** — T1 section only (Creation Tests). Note:
   - What a T1 test asserts (fields set correctly, invariants hold, aggregate state)
   - The test class location and naming convention

3. ***(Conditional)*** If the aggregate section in plan.md lists snapshot fields copied from an upstream aggregate (e.g., cached `courseId` from Course, or `name`/`username` from User): read the domain files of those upstream aggregates from `{src}microservices/{upstreamAggregate}/aggregate/`. Read only the fields you need to copy — do not read the whole upstream codebase.

---

## Produce

Produce every file listed in the plan.md `2.{N}.a` row for this aggregate. The authoritative file list is in plan.md — use it exactly. The descriptions below explain what each file must contain.

### `{Aggregate}.java`

Path: `{src}microservices/{aggregate}/aggregate/{Aggregate}.java`

- Extends `Aggregate` (from the simulator core)
- JPA annotated (`@Entity`, `@Table`, `@Id`, etc.)
- Contains all fields defined in the domain model for this aggregate, including:
  - Snapshot fields copied from other aggregates (cached denormalized data)
  - Owned entity fields — choose based on cardinality:
    - **Collection** (`@OneToMany`): `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)` — aggregate is the inverse side with no FK column
    - **Single** (`@OneToOne`): `@OneToOne(cascade = CascadeType.ALL, mappedBy = "{entityField}")` — aggregate holds the inverse side; the entity class holds the FK via a plain `@OneToOne` back-reference. The aggregate's setter must call `entity.set{Aggregate}(this)` to wire the bidirectional link before the entity is persisted
- Constructor: accepts all required fields; sets `state = ACTIVE`; does **not** call `verifyInvariants()` — the framework calls it automatically via `registerChanged` at commit time
- `verifyInvariants()`: enforces all **P1 rules** for this aggregate listed in plan.md. Throws `{AppClass}Exception` with the appropriate error message constant on violation.
- Getters and setters for all mutable fields
- No business logic methods (sagas call service; service calls setters then verifyInvariants)

### Owned entity classes

For each entity owned by this aggregate (listed in plan.md aggregate section):

Path: `{src}microservices/{aggregate}/aggregate/{Entity}.java`

- `@Entity` + `@Table`; `@Id` auto-generated
- Fields matching the domain model
- Constructor, getters, setters
- **Bidirectional `@OneToOne` (when applicable):** If the aggregate side uses `@OneToOne(mappedBy = "{entityField}")`, this entity class holds the owning side: declare a plain `@OneToOne {Aggregate} {aggregate}` field (no `mappedBy`) with a getter/setter. The aggregate's setter for this entity must call `entity.set{Aggregate}(this)` to wire the back-reference before persisting.

### Domain enum (conditional)

If any aggregate field is typed as a domain enum (e.g., `role: UserRole`), produce a companion enum file:

Path: `{src}microservices/{aggregate}/aggregate/{Aggregate}Role.java` (or appropriate name)

- Plain Java `enum` — no JPA annotations
- Values matching the domain model
- Add it explicitly to the plan.md 2.N.a row if not already listed

### `Saga{Aggregate}.java`

Path: `{src}microservices/{aggregate}/aggregate/sagas/Saga{Aggregate}.java`

- Extends `{Aggregate}`, implements `SagaAggregate`
- Adds a `sagaState` field of type `SagaAggregate.SagaState` (the interface) — **no** JPA annotation; the simulator's `SagaStateConverter` handles persistence
- Constructor delegates to super and initializes `sagaState` to `GenericSagaState.NOT_IN_SAGA`
- Implements `getSagaState()` returning the field; `setSagaState(SagaState state)` sets it directly (field type is the interface, no cast needed)
- No other logic

### `{Aggregate}SagaState.java`

Path: `{src}microservices/{aggregate}/aggregate/sagas/states/{Aggregate}SagaState.java`

- Enum implementing `SagaState`
- Always includes `NOT_IN_SAGA`
- Include `IN_UPDATE_{AGGREGATE}` or `IN_DELETE_{AGGREGATE}` only when the saga has additional steps **after** the primary write step that must observe the aggregate under a distinct locked state. For a simple two-step saga (read → write-as-final-step), `READ_{AGGREGATE}` is sufficient as the only non-`NOT_IN_SAGA` state.
- **Do not** add a state for create sagas — `Create{Aggregate}` creates a new aggregate instance; there is no existing instance to lock
- Include `READ_{AGGREGATE}` if other aggregates use this aggregate as a cross-aggregate prerequisite (another aggregate's write saga fetches this one's DTO — check plan.md's write functionalities for other aggregates)

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
- Add custom JPQL query method implementations only as needed; for aggregates with no cross-table lookups, the class body can be left empty beyond the autowired repository

### `{Aggregate}Repository.java`

Path: `{src}microservices/{aggregate}/aggregate/{Aggregate}Repository.java`

- Interface extending `AggregateRepository<{Aggregate}, Integer>` (standard Spring Data JPA)
- No custom queries needed here (custom queries go in `CustomRepositorySagas`)

### `{Aggregate}Dto.java`

Path: `{src}microservices/{aggregate}/aggregate/{Aggregate}Dto.java`

- Plain Java class (no JPA annotations)
- Fields matching the aggregate's public surface (what other aggregates or tests need to read)
- Constructor from `{Aggregate}`, all-fields constructor, and no-arg constructor
- Getters and setters

### `{Aggregate}Test.groovy` (T1)

Path: `{test}sagas/{aggregate}/{Aggregate}Test.groovy`

- Extends `{AppClass}SpockTest`
- **One happy-path creation test**: `def "create {Aggregate}"()` — instantiate `Saga{Aggregate}` directly and assert fields are set correctly
- **Do not** use `{AppClass}Functionalities.create{Aggregate}(...)` — write functionalities are not available until session b
- **Invariant-violation tests** — for each P1 intra-invariant in `verifyInvariants()`, add one test that:
  1. Instantiates `Saga{Aggregate}` directly and manipulates its state to violate the invariant (e.g., add owned entities via `addXxx()`, call `aggregate.remove()` to set state to DELETED, etc.)
  2. Calls `verifyInvariants()` directly on the aggregate
  3. Asserts `thrown({AppClass}Exception)`
- These tests exercise the aggregate layer in isolation — no service or saga needed
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
