# TCC Placeholder Pattern

## Purpose

New applications built on the simulator are implemented using the **Sagas model only**. All TCC/Causal classes are generated as minimal stubs that satisfy the compiler and allow the application to start — they are never the authoritative implementation.

This document describes the exact stub pattern for each TCC class so that AI agents apply it consistently.

---

## Why Stubs Instead of Full TCC

The Sagas model is sufficient for correctness and testability. Implementing full TCC merge logic in parallel with Sagas doubles the implementation effort without adding functionality during the development phase.

The stubs exist because:
1. The simulator framework requires both profiles (`test-sagas`, `test-tcc`) to compile.
2. Spring component scan may pick up both factory variants; stubs must not throw at construction time.
3. Stubs make it straightforward to add TCC implementations later without restructuring.

---

## Stub Pattern per Class

### `Causal<Aggregate>` (TCC aggregate subclass)

```java
@Entity
public class Causal<Aggregate> extends <Aggregate> implements CausalAggregate {

    public Causal<Aggregate>() {
        super();
    }

    public Causal<Aggregate>(<Aggregate> other) {
        super(other);
    }

    @Override
    public Set<String> getMutableFields() {
        return Set.of();  // TCC not implemented
    }

    @Override
    public Set<Pair<String, String>> getIntentions() {
        return Set.of();  // TCC not implemented
    }

    @Override
    public Aggregate mergeFields(Set<String> toCommitChangedFields,
                                  Aggregate committedVersion,
                                  Set<String> committedChangedFields) {
        return this;  // TCC not implemented — no merge
    }
}
```

### `Causal<Aggregate>Factory` (TCC factory)

```java
@Component
@Profile("tcc")
public class Causal<Aggregate>Factory extends <Aggregate>Factory {

    @Override
    public Causal<Aggregate> createAggregate(Integer aggregateId, <Aggregate>Dto dto, ...) {
        throw new UnsupportedOperationException("TCC not implemented");
    }

    @Override
    public Causal<Aggregate> copy(<Aggregate> existing) {
        throw new UnsupportedOperationException("TCC not implemented");
    }
}
```

### `<Aggregate>CustomRepositoryTCC` (TCC repository)

```java
public interface <Aggregate>CustomRepositoryTCC
        extends <Aggregate>CustomRepository, CausalAggregateRepository<Causal<Aggregate>> {
    // No additional methods — TCC not implemented
}
```

### `<FunctionalityName>FunctionalityTCC` (TCC workflow)

```java
public class <FunctionalityName>FunctionalityTCC extends WorkflowFunctionality {

    public <FunctionalityName>FunctionalityTCC(CausalUnitOfWorkService uows, ..., CausalUnitOfWork uow, CommandGateway gw) {
        // TCC not implemented — do not call buildWorkflow
    }

    @Override
    public void buildWorkflow(...) {
        // TCC not implemented
    }
}
```

### `<Primary>Functionalities` entry point — TCC case

In the `Functionalities` router class, the TCC case throws immediately:

```java
case TCC:
    throw new UnsupportedOperationException(
        "<FunctionalityName>: TCC not implemented. Run with -Ptest-sagas.");
```

---

## What Stubs Must NOT Do

- **Do not implement real merge logic** in `mergeFields()`. Return `this` only.
- **Do not add real `getMutableFields()` entries.** Return `Set.of()`.
- **Do not call `buildWorkflow()` from the TCC constructor.** Leave it empty.
- **Do not add guard or invariant logic** to TCC classes. All business logic lives in the Sagas path.

---

## Testing

Only the Sagas profile is tested:

```bash
mvn clean -Ptest-sagas test
```

The TCC profile (`-Ptest-tcc`) is not expected to pass for new applications using the stub pattern. Do not write or run TCC tests.
