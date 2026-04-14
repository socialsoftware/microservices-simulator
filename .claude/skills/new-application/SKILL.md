---
name: new-application
description: Bootstrap a new application on the microservices-simulator from scratch. Guides through all 5 phases: reading human-authored domain templates, aggregate scaffolding, cross-service functionalities, event wiring, and tests. Arguments: "<AppName> [short description of the domain]"
argument-hint: "<AppName> [short description of the domain]"
---

# Bootstrap New Application: $ARGUMENTS

You are building a new application on top of the simulator library from scratch. Use `applications/quizzes/` as a reference throughout.

> **Critical constraints:**
> 1. **Domain model and aggregate boundaries are human-defined.** The domain expert has filled the templates `{AppName}-domain-model.md` and `{AppName}-aggregate-grouping.md` before this skill is invoked. Do not change entity definitions, aggregate groupings, functionalities, event names, or rule semantics — read them as given.
> 2. **Consistency layer placement is AI-decided.** For each rule in the domain model, use `docs/concepts/decision-guide.md` to classify it into the correct layer (1, 2, 3, or 4) and confirm with the user before coding.
> 3. **Sagas only.** All TCC/Causal classes are empty stubs. See `docs/concepts/tcc-placeholder-pattern.md`.

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
3. Add the simulator dependency to `pom.xml` and define the Maven profile following `applications/quizzes/pom.xml`:
   - `test-sagas` profile only (Sagas only — do not add `test-tcc` unless TCC is explicitly required)
4. Define shared exception class and error message enum (e.g., `<App>Exception.java`, `<App>ErrorMessage.java`). Follow `applications/quizzes/src/main/java/.../exception/QuizzesException.java` and `QuizzesErrorMessage.java`.

> **STOP after Phase 0.** Report what was created and ask: "Phase 0 complete. Ready to proceed to Phase 1 (read templates and classify rules)?"

---

## Phase 1 — Read Templates and Classify Rules

This phase has no code output. It produces a classification table and a work plan that must be confirmed by the user before Phase 2 begins.

### 1.1 Read the human-authored domain templates

Read both files as provided by the domain expert:
- `{AppName}-domain-model.md` — entities, fields, relationships, §3.1 rules, §3.2 rules, §4 functionalities
- `{AppName}-aggregate-grouping.md` — aggregate groupings (§1), snapshots (§2), event DAG (§3), named events (§4)

Extract:
- **Aggregates** — from §1 of grouping template
- **Snapshot fields per aggregate** — from §2 of grouping template (which fields are cached and which event triggers each update)
- **Event names and payloads** — from §4 of grouping template (each row is one `/new-event` call in Phase 4)
- **Functionalities** — from §4 of domain model template (each row is one `/new-functionality` call in Phase 3)
- **§3.1 rules** — single-entity, always Layer 1
- **§3.2 rules** — cross-entity, classify below

### 1.2 Classify §3.2 rules

For each §3.2 rule, apply the decision flowchart in `docs/concepts/decision-guide.md` and assign it to Layer 1, 2, 3, or 4.

Key reminder:
- All data already inside the same aggregate (including its snapshot fields) → **Layer 1** (`/intra-invariant`)
- Synchronous check, reads only the aggregate being mutated → **Layer 2** (`/service-guard`)
- Synchronous check, reads a *different* aggregate → **Layer 3** (`/new-functionality` with `setForbiddenStates`)
- Eventual cache sync, no blocking needed → **Layer 4** (`/inter-invariant`)

### 1.3 Output classification table and confirm with user

Produce this table and **stop — do not proceed to Phase 2 until the user approves the classification**:

```
Rule Classification Table — {AppName}

§3.1 Rules (all → Layer 1):
  - {RULE_NAME}: Layer 1 → /intra-invariant on {AggregateName}

§3.2 Rules:
  - {RULE_NAME}: Layer {1|2|3|4} → {skill(s)}
  - ...

Aggregates to scaffold: {list}

Functionalities to implement (from §4 of domain model):
  - {FunctionalityName}: /new-functionality {FunctionalityName} {PrimaryAggregate} [{OtherAggregates...}]

Events to wire (from §4 of grouping):
  - {EventName}: {Publisher} → {Consumer(s)} — /new-event {EventName} {Publisher} {Consumer}
    (if multiple consumers: one /new-event call per consumer)
```

Ask: "Does this classification look correct? Any adjustments before I start Phase 2 (aggregate scaffolding)?"

> **STOP after Phase 1.** Do not write any code until the user explicitly approves the classification table above.

---

## Phase 2 — Aggregate Scaffolding

For each aggregate listed in §1 of the grouping template:

**2.1** Scaffold the aggregate:
```
/new-aggregate <AggregateName> [description]
```

Each invocation produces:
- Base abstract class with `verifyInvariants()` (initially empty) and `getEventSubscriptions()` (initially empty)
- `Saga<Aggregate>` with `<Aggregate>SagaState` enum
- `Causal<Aggregate>` — **TCC stub** (empty `getMutableFields()`, `mergeFields() → return this`)
- Sagas and TCC factories (TCC factory throws `UnsupportedOperationException`)
- Repositories (Saga and TCC stub)
- Service stub

**2.2** After scaffolding, immediately look up §2 of the grouping template for this aggregate. Add all snapshot fields it must cache from external aggregates as fields in the aggregate class. Do not wire the event subscriptions yet — that happens in Phase 4.

**2.3** Add all Layer 1 rules that belong to this aggregate (from the classification table):
```
/intra-invariant <AggregateName> <rule-description>
```

Repeat for every §3.1 rule (and any §3.2 rules re-classified to Layer 1) assigned to this aggregate. By the end of Phase 2 every aggregate is fully defined — structure **and** invariants. Tests written in Phase 5 will run against complete aggregates.

Reference implementations: `applications/quizzes/.../execution/`, `applications/quizzes/.../tournament/`

> **STOP after Phase 2.** List every aggregate scaffolded and every intra-invariant added. Ask: "Phase 2 complete. Ready to proceed to Phase 3 (cross-service functionalities)?"

---

## Phase 3 — Cross-Service Functionalities

For each row in §4 of the domain model template, invoke:
```
/new-functionality <FunctionalityName> <PrimaryAggregate> [other aggregates...]
```

Each invocation produces:
- `<Op>FunctionalitySagas.java` — Sagas workflow with steps and `forbiddenStates` (full implementation)
- `<Op>FunctionalityTCC.java` — **TCC stub** (empty `buildWorkflow()`)
- Entry point in `<Primary>Functionalities.java` (Sagas wired; TCC throws `UnsupportedOperationException`)
- `<Op>Command.java` in the shared `commands/` package
- Command handler method in `<Primary>CommandHandler.java`
- REST controller method (if the operation is exposed via HTTP)

After each invocation, wire any **Layer 3 rules** (from the classification table) that apply to this functionality by adding `setForbiddenStates(...)` on the relevant saga steps. Refer to `AddParticipantFunctionalitySagas.java` for the pattern.

After all functionalities are implemented, also add any **Layer 2 rules** (from the classification table):
```
/service-guard <ServiceName> <operation-method> <precondition>
```

> **STOP after Phase 3.** List every functionality implemented and every service guard added. Ask: "Phase 3 complete. Ready to proceed to Phase 4 (event wiring and inter-invariants)?"

---

## Phase 4 — Event Wiring and Cross-Aggregate Consistency

Work through the named events in §4 of the grouping template. Each row is one event; if multiple consumers subscribe to the same event, invoke `/new-event` once per consumer.

For each (event, consumer) pair:
```
/new-event <EventName> <PublisherAggregate> <ConsumerAggregate>
```

Each invocation produces:
- `<EventName>.java` event class (if it does not exist yet; shared across consumers)
- `<Consumer>Subscribes<EventName>.java` subscription class
- `<EventName>EventHandler.java` handler class
- Handler registration in `<Consumer>EventProcessing.java`
- `<Consumer>EventHandling.java` polling class (created once per consumer aggregate)
- `<Consumer>EventSubscriberService.java` Spring Cloud Stream subscriber (created once per consumer)

After wiring each event, implement any **Layer 4 rules** (inter-invariants) that are triggered by that event:
```
/inter-invariant <ConsumerAggregate> <condition>
```

> **One event name per business transition.** If §4 of the grouping template lists two separate events for the same publisher (e.g., `UpdateStudentNameEvent` and `AnonymizeStudentEvent`), treat each as a separate `/new-event` call even if they flow to the same consumer.

> **STOP after Phase 4.** List every event wired and every inter-invariant added. Ask: "Phase 4 complete. Ready to proceed to Phase 5 (tests)?"

---

## Phase 5 — Tests

### 5.1 Test infrastructure setup

Before writing individual test cases, create the test infrastructure for the new application:

1. **`BeanConfigurationSagas.groovy`** — `@TestConfiguration` class that registers all aggregate services, repositories, and the `CommandGateway`. Follow `applications/quizzes/src/test/groovy/.../BeanConfigurationSagas.groovy` exactly — replace every Quizzes-specific service with the new application's services.

2. **Base Spock test class** (e.g., `<AppName>SpockTest.groovy`) — extends `SpockTest` and provides `@Autowired` fields for all services plus shared `setup()` helpers (e.g., `create<Entity>()` factory methods). Follow `applications/quizzes/src/test/groovy/.../QuizzesSpockTest.groovy`.

### 5.2 Write test cases

For each functionality, write Spock tests in `src/test/groovy/.../sagas/`:

- [ ] **Happy-path test** — creates prerequisites, runs the functionality end-to-end, asserts final state
- [ ] **Layer 1 (intra-invariant) violation tests** — trigger each §3.1 rule violation and assert the correct exception
- [ ] **Layer 2 (service-guard) violation tests** — set up the forbidden precondition, call the service method, assert the guard throws
- [ ] **Layer 3 (forbidden-state) violation tests** — use `executeUntilStep(...)` to pause a concurrent functionality in the step that sets the semantic lock, then attempt the conflicting operation and assert rejection
- [ ] **Layer 4 (inter-invariant) tests** — mutate the publisher aggregate, call `handle<Xxx>Events()` manually on the consumer, assert the cached snapshot fields are updated correctly
- [ ] **Concurrent interleaving tests** — `executeUntilStep(workflow, step)` + `resumeWorkflow(workflow)` to deterministically reproduce race conditions; assert both the committed state and any exceptions

Reference: `applications/quizzes/src/test/groovy/.../sagas/coordination/AddParticipantAndUpdateStudentNameTest.groovy`

### 5.3 Run tests

```bash
cd applications/<appName>
mvn clean -Ptest-sagas test
```

Fix all failures before moving on. Do not add the `-Ptest-tcc` profile — TCC stubs are not expected to pass.

---

## Checklist

- [ ] `pom.xml` with simulator dependency and `test-sagas` profile
- [ ] Shared exception class + error message enum
- [ ] Domain model and aggregate grouping templates read; classification table confirmed with user
- [ ] All aggregates scaffolded (base + Saga + Causal stub + factories + repos + service)
- [ ] Snapshot fields added to each aggregate (from §2 of grouping template)
- [ ] Layer 1 intra-invariants added to all aggregates
- [ ] Command classes for all functionalities
- [ ] Sagas functionalities for all cross-aggregate operations (TCC stubs created)
- [ ] Layer 3 `setForbiddenStates` wired in relevant saga steps
- [ ] Layer 2 service guards added
- [ ] Domain events wired per §4 of grouping template (one `/new-event` per event-consumer pair)
- [ ] Layer 4 inter-invariants implemented
- [ ] `BeanConfigurationSagas.groovy` and base Spock test class created
- [ ] Sagas tests green: `mvn clean -Ptest-sagas test`
