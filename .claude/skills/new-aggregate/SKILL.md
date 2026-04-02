---
name: new-aggregate
description: Scaffold a new domain aggregate in the microservices-simulator quizzes application (base class, SagaXxx, CausalXxx, factories, repositories, service stub). Arguments: "<AggregateName> [description]"
argument-hint: "<AggregateName> [short description of what it represents]"
---

# Scaffold New Aggregate: $ARGUMENTS

You are adding a new domain aggregate to the `applications/quizzes` module.

---

## Step 0 — Parse arguments

From `$ARGUMENTS` identify:
- **AggregateName**: the PascalCase name (e.g., `Tournament`, `Submission`)
- **description**: what the aggregate represents (ask if missing)
- **Fields**: what data fields it holds (ask for a minimum set if not provided)
- **References**: which other aggregates it embeds references to (e.g., a `CourseRef`)
- **Intra-invariants**: any local consistency rules that must always hold

Clarify before writing any code.

---

## Step 1 — Read existing templates

Read the following to understand the patterns to follow:
1. `microservices/execution/aggregate/Execution.java` — base class with event subscriptions
2. `microservices/execution/aggregate/sagas/SagaExecution.java` — saga subclass
3. `microservices/execution/aggregate/causal/CausalExecution.java` — TCC subclass with merge
4. `microservices/execution/aggregate/sagas/factories/SagasExecutionFactory.java`
5. `microservices/execution/aggregate/causal/factories/CausalExecutionFactory.java`
6. `microservices/execution/aggregate/CourseExecutionCustomRepository.java`
7. `microservices/execution/aggregate/sagas/repositories/CourseExecutionCustomRepositorySagas.java`
8. `microservices/execution/service/ExecutionService.java` — for service method patterns

Also read `docs/architecture.md` for architectural restrictions and package structure conventions.

---

## Step 2 — Implement: Base abstract class

File: `microservices/<aggregate>/aggregate/<Aggregate>.java`

- Extend `Aggregate`
- Add all fields with getters/setters
- Implement default constructor, creation constructor (calls `super(aggregateId)` and `setAggregateType(getClass().getSimpleName())`), copy constructor (copies ALL fields)
- Implement `verifyInvariants()` — throw `QuizzesException` for any violated intra-invariant
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

## Step 5 — Implement: CausalXxx subclass

File: `microservices/<aggregate>/aggregate/causal/Causal<Aggregate>.java`

- Extend the base class
- Implement `CausalAggregate`
- Implement `getMutableFields()` — list field names that may change concurrently
- Implement `getIntentions()` — pairs that must change together (empty set if none)
- Implement `mergeFields()` — take each changed field from `toCommitVersion`; fall back to `committedVersion` for unchanged fields

---

## Step 6 — Implement: Factories

`sagas/factories/Sagas<Aggregate>Factory.java` — creates `Saga<Aggregate>` instances.
`causal/factories/Causal<Aggregate>Factory.java` — creates `Causal<Aggregate>` instances.

Each implements `AggregateFactory<T>` with `createAggregate(...)` and `copy(T existing)`.

---

## Step 7 — Implement: Repositories

- `<Aggregate>CustomRepository.java` — shared interface, extends `AggregateRepository<T>`
- `sagas/repositories/<Aggregate>CustomRepositorySagas.java` — extends both above and `SagaAggregateRepository<Saga<Aggregate>>`
- `causal/repositories/<Aggregate>CustomRepositoryTCC.java` — extends both above and `CausalAggregateRepository<Causal<Aggregate>>`

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

Register the factory, repository, and service in `BeanConfigurationSagas.groovy` and `BeanConfigurationCausal.groovy` if not already picked up by component scan.

---

## Step 10 — Run tests

```bash
cd applications/quizzes
mvn clean -Ptest-sagas test
mvn clean -Ptest-tcc test
```

Write at minimum a creation test to confirm the aggregate can be created and persisted.

---

## Checklist

- [ ] Base abstract class: correct constructors, `verifyInvariants()`, `getEventSubscriptions()`
- [ ] `<Aggregate>SagaState` enum with `NOT_IN_SAGA`
- [ ] `Saga<Aggregate>`: implements `SagaAggregate`, copies `sagaState`
- [ ] `Causal<Aggregate>`: implements `CausalAggregate`, all three methods
- [ ] Sagas factory
- [ ] TCC factory
- [ ] Common repository interface
- [ ] Saga repository
- [ ] TCC repository
- [ ] Service stub with `create<Aggregate>`
- [ ] Tests passing
