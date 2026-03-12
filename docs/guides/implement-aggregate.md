# Guide: Implement an Aggregate

Follow these steps for each new domain aggregate. Use `execution/` and `tournament/` as templates.

Reference: [`docs/concepts/aggregate.md`](../concepts/aggregate.md)

---

## Step 1 — Base abstract class

File: `microservices/<aggregate>/aggregate/<Aggregate>.java`

```java
@Entity
public abstract class Xxx extends Aggregate {
    // Fields
    private String name;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "xxx")
    private XxxSomeRef someRef;   // embedded reference to another aggregate

    // Default constructor (required by JPA)
    public Xxx() {}

    // Creation constructor
    public Xxx(Integer aggregateId, XxxDto dto, XxxSomeRef someRef) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(dto.getName());
        setSomeRef(someRef);
    }

    // Copy constructor — must copy ALL fields
    public Xxx(Xxx other) {
        super(other);
        setName(other.getName());
        setSomeRef(new XxxSomeRef(other.getSomeRef()));
        // copy inter-invariant cached fields too
    }

    @Override
    public void verifyInvariants() {
        // throw QuizzesException for any violated intra-invariant
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> subs = new HashSet<>();
        if (getState() == ACTIVE) {
            interInvariantSomeRule(subs);
        }
        return subs;
    }

    private void interInvariantSomeRule(Set<EventSubscription> subs) {
        subs.add(new XxxSubscribesSomeEvent(this.someRef));
    }

    // Getters and setters
}
```

Checklist:
- [ ] Extends `Aggregate`
- [ ] `setAggregateType(getClass().getSimpleName())` in creation constructor
- [ ] Copy constructor copies **all** fields including embedded objects
- [ ] `verifyInvariants()` implemented (may be empty initially)
- [ ] `getEventSubscriptions()` returns subscriptions only when `ACTIVE`

---

## Step 2 — Saga subclass

File: `microservices/<aggregate>/aggregate/sagas/SagaXxx.java`

```java
@Entity
public class SagaXxx extends Xxx implements SagaAggregate {
    @Enumerated(EnumType.STRING)
    private XxxSagaState sagaState = XxxSagaState.NOT_IN_SAGA;

    public SagaXxx() {}

    public SagaXxx(Integer aggregateId, XxxDto dto, XxxSomeRef someRef) {
        super(aggregateId, dto, someRef);
    }

    public SagaXxx(SagaXxx other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    @Override public SagaState getSagaState() { return sagaState; }
    @Override public void setSagaState(SagaState state) { this.sagaState = (XxxSagaState) state; }
}
```

---

## Step 3 — Saga state enum

File: `microservices/<aggregate>/aggregate/sagas/states/XxxSagaState.java`

```java
public enum XxxSagaState implements SagaAggregate.SagaState {
    NOT_IN_SAGA("NOT_IN_SAGA"),
    IN_SOME_OPERATION("IN_SOME_OPERATION");

    private final String stateName;
    XxxSagaState(String name) { this.stateName = name; }

    @Override
    public String getStateName() { return stateName; }
}
```

---

## Step 4 — TCC subclass

File: `microservices/<aggregate>/aggregate/causal/CausalXxx.java`

```java
@Entity
public class CausalXxx extends Xxx implements CausalAggregate {
    public CausalXxx() {}
    public CausalXxx(Integer aggregateId, XxxDto dto, XxxSomeRef someRef) { super(aggregateId, dto, someRef); }
    public CausalXxx(CausalXxx other) { super(other); }

    @Override
    public Set<String> getMutableFields() {
        return new HashSet<>(Arrays.asList("name" /*, other mutable field names */));
    }

    @Override
    public Set<String[]> getIntentions() {
        return new HashSet<>();  // add pairs that must change together
    }

    @Override
    public Aggregate mergeFields(Set<String> toCommitChangedFields, Aggregate committed, Set<String> committedChangedFields) {
        CausalXxx merged = new CausalXxx((CausalXxx) committed);
        if (toCommitChangedFields.contains("name")) merged.setName(this.getName());
        // repeat for each mutable field
        return merged;
    }
}
```

**Note:** Only fields in `getMutableFields()` are compared during merge. Fields not listed are never considered conflicting.

---

## Step 5 — Factories

`sagas/factories/SagasXxxFactory.java`:
```java
@Component
public class SagasXxxFactory implements AggregateFactory<SagaXxx> {
    @Override
    public SagaXxx createAggregate(Integer aggregateId, ...) {
        return new SagaXxx(aggregateId, dto, someRef);
    }
    @Override
    public SagaXxx copy(SagaXxx existing) {
        return new SagaXxx(existing);
    }
}
```

Same pattern for `CausalXxxFactory`.

---

## Step 6 — Repositories

`XxxCustomRepository.java` (shared interface):
```java
public interface XxxCustomRepository extends AggregateRepository<Xxx> {}
```

`sagas/repositories/XxxCustomRepositorySagas.java`:
```java
public interface XxxCustomRepositorySagas extends XxxCustomRepository, SagaAggregateRepository<SagaXxx> {}
```

`causal/repositories/XxxCustomRepositoryTCC.java`:
```java
public interface XxxCustomRepositoryTCC extends XxxCustomRepository, CausalAggregateRepository<CausalXxx> {}
```

---

## Step 7 — Service stub

File: `microservices/<aggregate>/service/XxxService.java`

```java
@Service
public class XxxService {
    @Autowired private XxxCustomRepository repository;
    @Autowired private UnitOfWorkService unitOfWorkService;

    public XxxDto createXxx(XxxDto dto, UnitOfWork unitOfWork) {
        Integer aggregateId = unitOfWorkService.generateAggregateId();
        // build embedded refs...
        Xxx xxx = factory.createAggregate(aggregateId, dto, ref);
        xxx.verifyInvariants();
        unitOfWorkService.registerChanged(xxx, unitOfWork);
        return new XxxDto(xxx);
    }
}
```

---

## Naming Conventions Summary

| File | Naming pattern |
|------|---------------|
| Base class | `Xxx.java` |
| Saga subclass | `SagaXxx.java` |
| Saga state | `XxxSagaState.java` |
| TCC subclass | `CausalXxx.java` |
| Saga factory | `SagasXxxFactory.java` |
| TCC factory | `CausalXxxFactory.java` |
| Common repo | `XxxCustomRepository.java` |
| Saga repo | `XxxCustomRepositorySagas.java` |
| TCC repo | `XxxCustomRepositoryTCC.java` |
| Service | `XxxService.java` |
