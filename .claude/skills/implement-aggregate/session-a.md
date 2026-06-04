# Session 2.N.a — Domain Layer

This sub-file is loaded by `implement-aggregate` when the target session type is `a`. All context variables (`{app-name}`, `{pkg}`, `{AppClass}`, `{Aggregate}`, `{N}`, `{src}`, `{test}`, `{bean-config}`) are already available from the router.

---

## Reads

Load these files before writing any code:

1. **`docs/concepts/aggregate.md`** — all sections. Specifically use:
   - § Key Fields, § Base Class — base-class fields and lifecycle methods
   - § Variants → Sagas variant — `SagaAggregate` interface; what `getSagaState()` / `setSagaState()` must return
   - § Factories, § Repositories — `CustomRepositorySagas` `getLatestVersion` and `findSagaAggregateById` queries
   - § getEventSubscriptions() Implementation — relevant only if this aggregate has subscribed events (otherwise skip)
   - References to `prev` (used for temporal invariants) appear under § Key Fields / § Base Class

2. **`docs/concepts/testing.md`** — § T1 — Intra-Invariant Test. Note:
   - What the full T1 matrix covers: creation happy-path, one violation per non-`final` P1 rule, BVA straddles for ordered predicates
   - That all T1 cases go via direct construction/mutation + `verifyInvariants()` — never through the service
   - The test class location and naming convention

3. ***(Conditional)*** If the aggregate section in plan.md lists snapshot fields copied from an upstream aggregate (e.g., cached `courseId` from Course, or `name`/`username` from User): read the domain files of those upstream aggregates from `{src}microservices/{upstreamAggregate}/aggregate/` — only the field declarations you need to copy. Do not read the whole upstream codebase.

---

## Verify Mandatory Files in plan.md

Before producing any files, check the plan.md `2.{N}.a` row for this aggregate and verify that **both** of the following interface files are listed:

- `aggregate/{Aggregate}Factory.java`
- `aggregate/{Aggregate}CustomRepository.java`

If either is missing, add it to the plan.md `2.{N}.a` file cell now. These two files are mandatory for every aggregate — `{Aggregate}Factory.java` because the service layer and `BeanConfigurationSagas.groovy` always inject the factory interface, and `{Aggregate}CustomRepository.java` because `{Aggregate}CustomRepositorySagas.java` always implements it. Neither may be omitted regardless of whether the aggregate has cross-table lookups.

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
- **Nested entity-to-entity `@OneToOne` (entity owns a sub-entity):** When an owned entity itself exclusively owns one sub-entity (e.g., `TournamentParticipant → TournamentParticipantQuizAnswer`), use a unidirectional `@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)` on the outer entity — no `mappedBy`, no back-reference field on the sub-entity unless explicitly needed. The outer entity's copy constructor must deep-copy the sub-entity via `new SubEntity(existing.getSubEntity())`.

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

> **Bean naming constraint:** The simple class name of this saga aggregate determines the service routing string used by `resolveServiceName()` in session b. For multi-word aggregates (e.g. `SagaQuizAnswer`), `resolveServiceName` produces `"quizAnswer"` — camelCase, not a shortened alias like `"answer"`. The `ServiceMapping` entry created in session b **must** use this exact value. Note it now to avoid a silent routing failure later.

### `{Aggregate}SagaState.java`

Path: `{src}microservices/{aggregate}/aggregate/sagas/states/{Aggregate}SagaState.java`

- Enum implementing `SagaState`
- **Do not** include `NOT_IN_SAGA` — the initial state is set to `GenericSagaState.NOT_IN_SAGA` (from the framework) in the `Saga{Aggregate}` constructor. This enum only holds operation-specific locked states.
- Include `IN_UPDATE_{AGGREGATE}` or `IN_DELETE_{AGGREGATE}` only when the saga has additional steps **after** the primary write step that must observe the aggregate under a distinct locked state. For a simple two-step saga (read → write-as-final-step), `READ_{AGGREGATE}` is sufficient as the only state in this enum.
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

### `{Aggregate}IntraInvariantTest.groovy` (T1)

Path: `{test}sagas/{aggregate}/{Aggregate}IntraInvariantTest.groovy`

See `docs/concepts/testing.md` § T1 — Intra-Invariant Test for the full remit and templates.

- Extends `{AppClass}SpockTest`
- **Happy-path creation test**: `def "create {Aggregate}"()` — instantiate `Saga{Aggregate}` directly, call `verifyInvariants()`, and assert all fields from the `plan.md` aggregate field list. Assertion provenance: fields must trace to the spec, not to the constructor body you just wrote. If the constructor sets a field the spec doesn't list, flag the planning gap in the session report.
- **One violation test per non-`final` P1 rule** (from this aggregate's `plan.md` P1 list): construct or mutate a `Saga{Aggregate}` so that exactly one P1 predicate fails, then call `verifyInvariants()` directly and assert `thrown({AppClass}Exception)` with `ex.getErrorMessage() == {RULE_NAME}`. Skip rules marked as Java `final` fields (compiler-enforced; no write path can violate them — note the omission in the session report).
- **Boundary straddle for every ordered-domain P1 predicate** (count, timestamp, or collection-size comparison — `<`/`<=`/`>`/`>=`/`==`): write the on-point (`verifyInvariants()` must not throw, pinned to the exact boundary value) and the off-point (`verifyInvariants()` must throw, one step beyond the boundary). Categorical rules (uniqueness, boolean/state freezes, set membership) keep their single representative case; do not invent boundary cases for them.
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
