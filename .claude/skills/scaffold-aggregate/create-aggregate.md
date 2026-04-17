# Create Aggregate Files

These instructions create all files for one aggregate. The aggregate name and application context
are already established by the caller. Use `<Aggregate>` as a placeholder for the PascalCase name
and `<aggregate>` for the camelCase/lowercase form.

Before writing anything, read `docs/architecture.md` for architectural restrictions and package
structure conventions. Then read the following reference implementations from `applications/quizzes/`:

1. `microservices/execution/aggregate/Execution.java` — base class with event subscriptions
2. `microservices/execution/aggregate/sagas/SagaExecution.java` — saga subclass
3. `microservices/execution/aggregate/sagas/factories/SagasExecutionFactory.java`
4. `microservices/execution/aggregate/CourseExecutionCustomRepository.java`
5. `microservices/execution/aggregate/sagas/repositories/CourseExecutionCustomRepositorySagas.java`
6. `microservices/execution/service/ExecutionService.java`

---

## Step 1 — Base abstract class

File: `microservices/<aggregate>/aggregate/<Aggregate>.java`

- Extend `Aggregate`
- Add all fields with getters/setters
- For boolean fields (e.g., `active`): use `is<FieldName>()` getter naming convention (e.g., `isActive()`, not `getActive()`); always add `@Column(columnDefinition = "boolean default false")` to set the DB default
- Implement default constructor, creation constructor (calls `super(aggregateId)` and
  `setAggregateType(getClass().getSimpleName())`), copy constructor (copies ALL fields including
  snapshot fields declared in the outer scaffold step)
- Implement `verifyInvariants()` — empty body initially; intra-invariant helpers are added next
- Implement `getEventSubscriptions()` — return empty set initially; event wiring happens in Phase 4
- Add a trivial `@Override public void remove() { super.remove(); }` method (required by Phase 3 delete/anonymize operations)

---

## Step 2 — Saga state enum

File: `microservices/<aggregate>/aggregate/sagas/states/<Aggregate>SagaState.java`

Include at minimum:
- `NOT_IN_SAGA` — the default state

Add operation-specific states only when the functionalities that need them are implemented.

---

## Step 3 — SagaXxx subclass

File: `microservices/<aggregate>/aggregate/sagas/Saga<Aggregate>.java`

- Extend the base class
- Implement `SagaAggregate`
- Add `sagaState` field with `@Enumerated(EnumType.STRING)`; default to `NOT_IN_SAGA`
- Copy `sagaState` in the copy constructor

---

## Step 4 — Factories

**Sagas factory** — `sagas/factories/Sagas<Aggregate>Factory.java`:
- Implements `AggregateFactory<Saga<Aggregate>>`
- `createAggregate(...)` returns a new `Saga<Aggregate>` instance
- `copy(Saga<Aggregate> existing)` returns `new Saga<Aggregate>(existing)`

---

## Step 5 — Repositories

- `<Aggregate>CustomRepository.java` — shared interface, extends `AggregateRepository<T>`
- `sagas/repositories/<Aggregate>CustomRepositorySagas.java` — extends both above and
  `SagaAggregateRepository<Saga<Aggregate>>`

---

## Step 6 — Service stub

File: `microservices/<aggregate>/service/<Aggregate>Service.java`

Add at minimum a `create<Aggregate>` method:

```java
Integer aggregateId = unitOfWorkService.generateAggregateId();
<Aggregate> agg = factory.createAggregate(aggregateId, dto /*, refs */);
unitOfWorkService.registerChanged(agg, unitOfWork);
return new <Aggregate>Dto(agg);
```

> **Do not call `agg.verifyInvariants()` directly.** `registerChanged` always calls it internally before writing — it is the single place that owns this responsibility. Calling it from a service method is redundant and misleading (it implies the service layer is responsible, which causes omissions in `getOrCreate`-style paths).

> **R2** — Only inject this aggregate's own repository, custom repository, and factory,
> plus `UnitOfWorkService` and `AggregateIdGeneratorService`. Never inject another aggregate's
> service, repository, or factory.

---

## Step 7 — Command handler stub

File: `microservices/<aggregate>/coordination/<Aggregate>CommandHandler.java`

Minimal stub — add routing for `Create<Aggregate>Command` only. Further commands are added when
functionalities are implemented in Phase 3.

---

## Checklist

- [ ] Base abstract class: correct constructors (`no-arg`, `(Id, Dto)`, copy), `verifyInvariants()`, `getEventSubscriptions()`, `remove()` override
- [ ] Boolean fields use `is<FieldName>()` getter with `@Column(columnDefinition = "boolean default false")`
- [ ] `<Aggregate>SagaState` enum with at least `NOT_IN_SAGA`
- [ ] `Saga<Aggregate>`: implements `SagaAggregate`, copies `sagaState`
- [ ] Sagas factory (creates `Saga<Aggregate>`)
- [ ] Common repository interface
- [ ] Saga repository
- [ ] Service stub with `create<Aggregate>` and `get<Aggregate>ById`
- [ ] Command handler stub
- [ ] DTO includes `AggregateState state` field and populates it in copy constructor
