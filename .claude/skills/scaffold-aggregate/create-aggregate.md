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
- Implement default constructor, creation constructor (calls `super(aggregateId)` and
  `setAggregateType(getClass().getSimpleName())`), copy constructor (copies ALL fields including
  snapshot fields declared in the outer scaffold step)
- Implement `verifyInvariants()` — empty body initially; intra-invariant helpers are added next
- Implement `getEventSubscriptions()` — return empty set initially; event wiring happens in Phase 4

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

## Step 4 — CausalXxx stub (TCC placeholder)

File: `microservices/<aggregate>/aggregate/causal/Causal<Aggregate>.java`

Stub only — do not implement real merge logic:

```java
@Entity
public class Causal<Aggregate> extends <Aggregate> implements CausalAggregate {

    public Causal<Aggregate>() { super(); }

    public Causal<Aggregate>(<Aggregate> other) { super(other); }

    @Override
    public Set<String> getMutableFields() { return Set.of(); }

    @Override
    public Set<Pair<String, String>> getIntentions() { return Set.of(); }

    @Override
    public Aggregate mergeFields(Set<String> toCommitChangedFields,
                                  Aggregate committedVersion,
                                  Set<String> committedChangedFields) {
        return this;
    }
}
```

---

## Step 5 — Factories

**Sagas factory** — `sagas/factories/Sagas<Aggregate>Factory.java`:
- Implements `AggregateFactory<Saga<Aggregate>>`
- `createAggregate(...)` returns a new `Saga<Aggregate>` instance
- `copy(Saga<Aggregate> existing)` returns `new Saga<Aggregate>(existing)`

**TCC factory stub** — `causal/factories/Causal<Aggregate>Factory.java`:
```java
@Component
@Profile("tcc")
public class Causal<Aggregate>Factory extends <Aggregate>Factory {
    @Override
    public Causal<Aggregate> createAggregate(...) {
        throw new UnsupportedOperationException("TCC not implemented");
    }
    @Override
    public Causal<Aggregate> copy(<Aggregate> existing) {
        throw new UnsupportedOperationException("TCC not implemented");
    }
}
```

---

## Step 6 — Repositories

- `<Aggregate>CustomRepository.java` — shared interface, extends `AggregateRepository<T>`
- `sagas/repositories/<Aggregate>CustomRepositorySagas.java` — extends both above and
  `SagaAggregateRepository<Saga<Aggregate>>`
- `causal/repositories/<Aggregate>CustomRepositoryTCC.java` — stub: extends both above and
  `CausalAggregateRepository<Causal<Aggregate>>`, no additional methods

---

## Step 7 — Service stub

File: `microservices/<aggregate>/service/<Aggregate>Service.java`

Add at minimum a `create<Aggregate>` method:

```java
Integer aggregateId = unitOfWorkService.generateAggregateId();
<Aggregate> agg = factory.createAggregate(aggregateId, dto /*, refs */);
agg.verifyInvariants();
unitOfWorkService.registerChanged(agg, unitOfWork);
return new <Aggregate>Dto(agg);
```

> **R2** — Only inject this aggregate's own repository, custom repository, and factory,
> plus `UnitOfWorkService` and `AggregateIdGeneratorService`. Never inject another aggregate's
> service, repository, or factory.

---

## Step 8 — Command handler stub

File: `microservices/<aggregate>/coordination/<Aggregate>CommandHandler.java`

Minimal stub — add routing for `Create<Aggregate>Command` only. Further commands are added when
functionalities are implemented in Phase 3.

---

## Checklist

- [ ] Base abstract class: correct constructors, `verifyInvariants()`, `getEventSubscriptions()`
- [ ] `<Aggregate>SagaState` enum with `NOT_IN_SAGA`
- [ ] `Saga<Aggregate>`: implements `SagaAggregate`, copies `sagaState`
- [ ] `Causal<Aggregate>`: stub — `getMutableFields() → Set.of()`, `getIntentions() → Set.of()`, `mergeFields() → return this`
- [ ] Sagas factory (creates `Saga<Aggregate>`)
- [ ] TCC factory stub (throws `UnsupportedOperationException`)
- [ ] Common repository interface
- [ ] Saga repository
- [ ] TCC repository stub
- [ ] Service stub with `create<Aggregate>`
- [ ] Command handler stub
