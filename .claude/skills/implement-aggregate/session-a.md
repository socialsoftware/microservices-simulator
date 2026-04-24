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
  - Owned entity collections (`@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)`)
- Constructor: accepts all required fields; sets `state = ACTIVE`; calls `verifyInvariants()`
- `verifyInvariants()`: enforces all **P1 rules** for this aggregate listed in plan.md. Throws `{AppClass}Exception` with the appropriate error message constant on violation.
- Getters and setters for all mutable fields
- No business logic methods (sagas call service; service calls setters then verifyInvariants)

### Owned entity classes

For each entity owned by this aggregate (listed in plan.md aggregate section):

Path: `{src}microservices/{aggregate}/aggregate/{Entity}.java`

- `@Entity` + `@Table`; `@Id` auto-generated
- Fields matching the domain model
- Constructor, getters, setters

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
- Include `IN_UPDATE_{AGGREGATE}` and `IN_DELETE_{AGGREGATE}` (or similar) for write sagas that **modify an existing instance** — these sagas lock an existing aggregate
- **Do not** add a state for create sagas — `Create{Aggregate}` creates a new aggregate instance; there is no existing instance to lock
- Include `READ_{AGGREGATE}` if other aggregates use this aggregate as a cross-aggregate prerequisite (another aggregate's write saga fetches this one's DTO — check plan.md's write functionalities for other aggregates)

### `Sagas{Aggregate}Factory.java`

Path: `{src}microservices/{aggregate}/aggregate/sagas/factories/Sagas{Aggregate}Factory.java`

- Plain `@Service @Profile("sagas")` class — no interface to implement (quizzes-full has no TCC variant, so no factory interface is required)
- Three methods:
  - `create{Aggregate}(aggregateId, ...)` — calls `new Saga{Aggregate}(...)`, sets `sagaState = GenericSagaState.NOT_IN_SAGA`, returns it
  - `create{Aggregate}Copy(Saga{Aggregate} existing)` — constructs a new `Saga{Aggregate}` with the same field values as `existing`, sets `sagaState = GenericSagaState.NOT_IN_SAGA`, returns it
  - `create{Aggregate}Dto({Aggregate} {aggregate})` — wraps the aggregate in a `{Aggregate}Dto` and returns it

### `{Aggregate}CustomRepositorySagas.java`

Path: `{src}microservices/{aggregate}/aggregate/sagas/repositories/{Aggregate}CustomRepositorySagas.java`

- Concrete `@Service @Profile("sagas")` class (not an interface)
- Has an `@Autowired {Aggregate}Repository {aggregate}Repository` field
- Add custom JPQL query methods only as needed; for aggregates with no cross-table lookups, the class body can be left empty (just the class declaration with the autowired repository)

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
- One `def "create {Aggregate}"()` test per constructor variant that is valid
- One `def "create {Aggregate} — {violation}"()` test per P1 rule violation (one test per rule)
- **Do not** use `{AppClass}Functionalities.create{Aggregate}(...)` — write functionalities are not available until session b. Instead, instantiate `Saga{Aggregate}` directly (e.g., `new Saga{Aggregate}(1, 'name', Type.VALUE)`) and call `verifyInvariants()` explicitly to trigger violation checks
- Asserts: fields set correctly on valid input; `{AppClass}Exception` thrown (with the correct error message constant) on each violation

### Error message constants

Open `{src}microservices/exception/{AppClass}ErrorMessage.java` and add one `public static final String` constant per P1 rule enforced in `verifyInvariants()` for this aggregate. Append to the existing file; do not remove existing constants.

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
