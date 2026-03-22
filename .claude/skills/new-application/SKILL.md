---
name: new-application
description: Bootstrap a new application on the microservices-simulator from scratch. Guides through all 5 phases: domain model design, aggregate scaffolding, cross-service functionalities, event wiring, and tests. Arguments: "<AppName> [short description of the domain]"
argument-hint: "<AppName> [short description of the domain]"
---

# Bootstrap New Application: $ARGUMENTS

You are building a new application on top of the simulator library from scratch. Use `applications/quizzes/` as a reference throughout.

---

## Step 0 — Parse arguments and clarify domain model

From `$ARGUMENTS` identify:
- **AppName**: the name of the application (e.g., `Library`, `Marketplace`)
- **Description**: what domain problem it addresses

Before writing any code, ask the user to enumerate:
1. **Aggregates** — the domain entities, each with a name, key fields, and intra-invariants
2. **Functionalities** — cross-aggregate operations (e.g., "enroll student", "submit order")
3. **Inter-aggregate dependencies** — which aggregates cache state from which others (upstream → downstream), and which invariants they enforce

Read `docs/architecture.md` for architectural restrictions before proceeding. Clarify the full domain model before writing any code.

---

## Phase 0 — Prerequisites

1. Install the simulator library to your local Maven repo:
   ```bash
   cd simulator && mvn install
   ```
2. Create the application module directory structure:
   ```
   applications/<appName>/
   ├── pom.xml
   └── src/main/java/<package>/microservices/
   ```
3. Add the simulator dependency to `pom.xml` and define both Maven profiles following `applications/quizzes/pom.xml`:
   - `test-sagas` profile
   - `test-tcc` profile
4. Define shared exception class and error message enum (e.g., `<App>ErrorMessage.java`)

---

## Phase 1 — Domain Model

### 1.1 Define aggregates

For each aggregate, confirm:
- Key fields and their types
- Embedded references to other aggregates (stored as value objects, not foreign keys)
- Intra-invariants (`verifyInvariants()` rules)

### 1.2 Define commands

For each operation exposed by a service, create a command class:
```java
public class CreateXxxCommand extends Command {
    // fields the service needs
}
```
Place them in `<app>/command/<service>/`.

### 1.3 Define shared events

For each cross-aggregate state change that consumers need to react to, note the publisher and consumer aggregate pair. Events are wired in Phase 4.

Reference concepts: `docs/concepts/aggregate.md`, `docs/concepts/events.md`, `docs/architecture.md`

---

## Phase 2 — Aggregate Scaffolding

For each aggregate, invoke:
```
/new-aggregate <AggregateName> [description]
```

Each `/new-aggregate` invocation produces:
- Base abstract class with `verifyInvariants()` and `getEventSubscriptions()`
- `SagaXxx` implementing `SagaAggregate` with `XxxSagaState` enum
- `CausalXxx` implementing `CausalAggregate` with `mergeFields()`
- Sagas and TCC factories
- Sagas and TCC repositories
- Service stub

Reference implementations: `applications/quizzes/.../execution/`, `applications/quizzes/.../tournament/`

---

## Phase 3 — Cross-Service Functionalities

For each operation that touches more than one aggregate, invoke:
```
/new-functionality <OperationName> <PrimaryAggregate> [other aggregates...]
```

Each `/new-functionality` invocation produces:
- `XxxFunctionalitySagas` — saga workflow with steps and `forbiddenStates`
- `XxxFunctionalityTCC` — TCC workflow
- Command handler wiring
- REST controller

---

## Phase 4 — Event Wiring and Inter-Invariants

For each cross-aggregate state dependency (upstream → downstream):
```
/new-event <EventName> <PublisherAggregate> <ConsumerAggregate>
```

For each consistency rule to enforce eventually:
```
/inter-invariant <ConsumerAggregate> <condition>
```

Each event produces: event class, subscription, handler, polling wiring, and update functionality (Sagas + TCC).

Reference: `docs/concepts/invariants.md` Layer 6 for the upstream/downstream model.

---

## Phase 5 — Tests

For each functionality, write:
- [ ] Happy-path test
- [ ] Invariant-violation tests
- [ ] Concurrent interleaving tests (`executeUntilStep` + `resumeWorkflow`)
- [ ] Event processing tests (call `handleXxxEvents()` manually — not auto-polled in `@DataJpaTest`)

Run both profiles:
```bash
cd applications/<appName>
mvn clean -Ptest-sagas test
mvn clean -Ptest-tcc test
```

---

## Checklist

- [ ] `pom.xml` with simulator dependency and both profiles (`test-sagas`, `test-tcc`)
- [ ] Shared exception + error message class
- [ ] All aggregates scaffolded (base + Saga + Causal + factories + repos + service)
- [ ] Command classes for all operations
- [ ] Functionalities (Sagas + TCC) for all cross-aggregate operations
- [ ] Domain events and inter-invariants wired
- [ ] Both test profiles green
