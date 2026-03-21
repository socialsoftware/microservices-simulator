# Guide: Implement a New Application

This guide walks through building a new application on top of the simulator from scratch. Use `applications/quizzes/` as a reference throughout.

---

## Prerequisites

1. Install the simulator library to your local Maven repo:
   ```bash
   cd simulator
   mvn install
   ```
2. Add the simulator dependency to your `pom.xml`.
3. Define your Maven profiles (`test-sagas`, `test-tcc`) following `applications/quizzes/pom.xml`.

---

## Phase 1 — Domain Model

### 1.1 Define aggregates

For each domain entity:
1. Create the base abstract class (e.g., `Execution`) — see [`docs/guides/implement-aggregate.md`](implement-aggregate.md)
2. Identify mutable fields and intra-invariants
3. List which external aggregates it references (embedded value objects, not foreign keys)

Reference: [`docs/concepts/aggregate.md`](../concepts/aggregate.md)

### 1.2 Define shared events

For each cross-aggregate state change that consumers need to react to:
1. Create event classes in `<app>/events/` — see [`docs/guides/implement-event.md`](implement-event.md)

Reference: [`docs/concepts/events.md`](../concepts/events.md)

### 1.3 Define commands

For each operation exposed by a service, create a command class in `<app>/command/<service>/`:
```java
public class CreateXxxCommand extends Command {
    // fields the service needs
}
```

---

## Phase 2 — Infrastructure per Aggregate

For each aggregate, follow [`docs/guides/implement-aggregate.md`](implement-aggregate.md):
- [ ] Base class with `verifyInvariants()` and `getEventSubscriptions()`
- [ ] `SagaXxx` implementing `SagaAggregate`
- [ ] `CausalXxx` implementing `CausalAggregate`
- [ ] Saga and TCC factories
- [ ] Saga and TCC repositories
- [ ] Service class

---

## Phase 3 — Cross-Service Functionalities

For each operation that touches multiple aggregates, follow [`docs/guides/implement-functionality.md`](implement-functionality.md):
- [ ] `XxxFunctionalitySagas` — saga workflow with steps and forbidden states
- [ ] `XxxFunctionalityTCC` — TCC workflow
- [ ] Command handlers (local / stream / gRPC)
- [ ] REST controllers

---

## Phase 4 — Event Wiring

For each inter-invariant, follow [`docs/guides/implement-event.md`](implement-event.md) and the `/inter-invariant` skill:
- [ ] Event class with correct `publisherAggregateId`
- [ ] Subscriptions registered in `getEventSubscriptions()`
- [ ] Handlers, polling, EventProcessing → Functionalities chain
- [ ] Sagas + TCC update functionality
- [ ] Guard enforced in the service

Reference: [`docs/concepts/invariants.md`](../concepts/invariants.md)

---

## Phase 5 — Tests

For each functionality:
- [ ] Happy-path test
- [ ] Invariant-violation tests
- [ ] Concurrent interleaving tests (using `executeUntilStep` + `resumeWorkflow`)
- [ ] Event processing tests (call `handleXxxEvents()` manually)

Run:
```bash
mvn clean -Ptest-sagas test
mvn clean -Ptest-tcc test
```

---

## Reference Implementations

| Aggregate | Path |
|-----------|------|
| Execution | `applications/quizzes/.../execution/` |
| Tournament | `applications/quizzes/.../tournament/` |

Both are complete end-to-end examples covering all phases.
