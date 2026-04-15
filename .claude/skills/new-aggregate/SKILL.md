---
name: new-aggregate
description: Scaffold a new domain aggregate in the microservices-simulator (base class, SagaXxx, CausalXxx stubs, factories, repositories, service stub). Arguments: "<AggregateName> [description]"
argument-hint: "<AggregateName> [short description of what it represents]"
---

# Scaffold New Aggregate: $ARGUMENTS

You are adding a new domain aggregate to the application currently being built on the simulator.

> **Sagas only.** The Sagas implementation is the authoritative path. TCC/Causal classes are empty stubs that compile but are never invoked. See `docs/concepts/tcc-placeholder-pattern.md`.

---

## Step 0 — Parse arguments

From `$ARGUMENTS` identify:
- **AggregateName**: the PascalCase name (e.g., `Tournament`, `Submission`)
- **description**: what the aggregate represents (ask if missing)
- **Fields**: what data fields it holds (ask for a minimum set if not provided)
- **References**: which other aggregates it embeds references to (e.g., a `CourseRef`)
- **Intra-invariants**: any local consistency rules that must always hold (§3.1 from the domain model)

Clarify before writing any code.

---

## Step 1 — Read existing templates

Before writing anything, read `docs/architecture.md` for architectural restrictions and package structure conventions.

Then read the following reference implementations from `applications/quizzes/` to understand the patterns:
1. `microservices/execution/aggregate/Execution.java` — base class with event subscriptions
2. `microservices/execution/aggregate/sagas/SagaExecution.java` — saga subclass
3. `microservices/execution/aggregate/sagas/factories/SagasExecutionFactory.java`
4. `microservices/execution/aggregate/CourseExecutionCustomRepository.java`
5. `microservices/execution/aggregate/sagas/repositories/CourseExecutionCustomRepositorySagas.java`
6. `microservices/execution/service/ExecutionService.java` — for service method patterns

Apply these patterns to the application you are working on, adapting package names, exception classes, and error message enums accordingly.

---

## Step 2 — Implement: Base abstract class

File: `microservices/<aggregate>/aggregate/<Aggregate>.java`

- Extend `Aggregate`
- Add all fields with getters/setters
- Implement default constructor, creation constructor (calls `super(aggregateId)` and `setAggregateType(getClass().getSimpleName())`), copy constructor (copies ALL fields including snapshot fields)
- Implement `verifyInvariants()` — throw `<App>Exception` for any violated intra-invariant
- Implement `getEventSubscriptions()` — return empty set initially; inter-invariants are added later

---

## Step 3 — Implement: Saga state enum

File: `microservices/<aggregate>/aggregate/sagas/states/<Aggregate>SagaState.java`

Include at minimum:
- `NOT_IN_SAGA` — the default state

Add operation-specific states only when you know which functionalities will need them.

---

## Step 4 — Implement: SagaXxx subclass

File: `microservices/<aggregate>/aggregate/sagas/Saga<Aggregate>.java`

- Extend the base class
- Implement `SagaAggregate`
- Add `sagaState` field with `@Enumerated(EnumType.STRING)`; default to `NOT_IN_SAGA`
- Copy `sagaState` in copy constructor

---

## Step 5 — Implement: CausalXxx stub (TCC placeholder)

File: `microservices/<aggregate>/aggregate/causal/Causal<Aggregate>.java`

This is a **stub only**. Do not implement real merge logic. Follow the pattern from `docs/concepts/tcc-placeholder-pattern.md`:

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

## Step 6 — Implement: Factories

**Sagas factory** — `sagas/factories/Sagas<Aggregate>Factory.java`:
- Implements `AggregateFactory<Saga<Aggregate>>`
- `createAggregate(...)` returns a new `Saga<Aggregate>` instance
- `copy(Saga<Aggregate> existing)` returns `new Saga<Aggregate>(existing)` (copy constructor)

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

## Step 7 — Implement: Repositories

- `<Aggregate>CustomRepository.java` — shared interface, extends `AggregateRepository<T>`
- `sagas/repositories/<Aggregate>CustomRepositorySagas.java` — extends both above and `SagaAggregateRepository<Saga<Aggregate>>`
- `causal/repositories/<Aggregate>CustomRepositoryTCC.java` — stub: extends both above and `CausalAggregateRepository<Causal<Aggregate>>`, no additional methods

---

## Step 8 — Implement: Service stub

File: `microservices/<aggregate>/service/<Aggregate>Service.java`

Add at minimum a `create<Aggregate>` method following the pattern:
```java
Integer aggregateId = unitOfWorkService.generateAggregateId();
// build embedded refs ...
<Aggregate> agg = factory.createAggregate(aggregateId, dto, ref);
agg.verifyInvariants();
unitOfWorkService.registerChanged(agg, unitOfWork);
return new <Aggregate>Dto(agg);
```

---

## Step 9 — Wire into BeanConfiguration

Register the factory, repository, and service in `BeanConfigurationSagas.groovy` if not already picked up by component scan. Do **not** add TCC factory or TCC repository to the Sagas bean configuration.

---

## Step 10 — Run tests

```bash
cd applications/<appName>
mvn clean -Ptest-sagas test
```

Write at minimum a creation test to confirm the aggregate can be created and persisted.

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
- [ ] TCC repository stub (no additional methods)
- [ ] Service stub with `create<Aggregate>`
- [ ] Tests passing: `mvn clean -Ptest-sagas test`
