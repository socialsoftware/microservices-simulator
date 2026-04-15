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
4. Define shared exception class and error message enum (e.g., `<App>Exception.java`, `<App>ErrorMessage.java`). Follow `applications/quizzes/src/main/java/.../exception/QuizzesException.java` and `QuizzesErrorMessage.java` as references.

5. Create the test infrastructure (front-loaded — needed to run creation tests after each aggregate in Phase 2):
   - **`BeanConfigurationSagas.groovy`** — `@TestConfiguration` class that registers all aggregate services, repositories, and the `CommandGateway`. Follow `applications/quizzes/src/test/groovy/.../BeanConfigurationSagas.groovy` as a reference — replace every Quizzes-specific service with the new application's services. At this point the class will be mostly empty; add each service as it is scaffolded in Phase 2.
   - **Base Spock test class** (e.g., `<AppName>SpockTest.groovy`) — extends `SpockTest` and provides `@Autowired` fields for all services plus shared `setup()` helpers (e.g., `create<Entity>()` factory methods). Follow `applications/quizzes/src/test/groovy/.../QuizzesSpockTest.groovy` as a reference.

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
- **Event names and payloads** — from §4 of grouping template (each row is one `/inter-invariant` call in Phase 4)
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
    [if Layer 3: wired into step "{StepName}" of /new-functionality {FunctionalityName}]

Aggregates to scaffold: {list}

Functionalities to implement (from §4 of domain model):
  - {FunctionalityName}: /new-functionality {FunctionalityName} {PrimaryAggregate} [{OtherAggregates...}]

Events and inter-invariants to wire (from §4 of grouping):
  - {EventName}: {Publisher} → {Consumer(s)}
    → /inter-invariant {ConsumerAggregate} <condition>
    (if multiple consumers: one /inter-invariant call per consumer)
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

Repeat for every §3.1 rule (and any §3.2 rules re-classified to Layer 1) assigned to this aggregate.

**2.4** Register the aggregate's service, factory, and repository in `BeanConfigurationSagas.groovy` (created in Phase 0). Then write and run a creation test:

```bash
cd applications/<appName>
mvn clean -Ptest-sagas test -Dtest=<AggregateName>Test
```

Fix failures before scaffolding the next aggregate.

Reference implementations: `applications/quizzes/.../execution/`, `applications/quizzes/.../tournament/`

> **STOP after Phase 2.** List every aggregate scaffolded, every intra-invariant added, and confirm all creation tests pass. Ask: "Phase 2 complete. Ready to proceed to Phase 3 (cross-service functionalities)?"

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

After each `/new-functionality` and `/service-guard` invocation, run the tests for that functionality:

```bash
cd applications/<appName>
mvn clean -Ptest-sagas test -Dtest=<FunctionalityName>Test
```

Fix failures before moving to the next functionality.

> **STOP after Phase 3.** List every functionality implemented and every service guard added. Confirm all tests pass. Ask: "Phase 3 complete. Ready to proceed to Phase 4 (event wiring and inter-invariants)?"

---

## Phase 4 — Event Wiring and Cross-Aggregate Consistency

Work through the named events in §4 of the grouping template. Each row is one event; if multiple consumers subscribe to the same event, invoke `/inter-invariant` once per consumer.

For each (event, consumer) pair in the classification table (from §4 of the grouping template):

```
/inter-invariant <ConsumerAggregate> <condition>
```

This single invocation produces everything: the event class, subscription, handler, polling method, EventProcessing chain, tracked field on the consumer aggregate, update functionality, and service state-update method.

After each invocation, run its test to confirm the state update works:

```bash
cd applications/<appName>
mvn clean -Ptest-sagas test -Dtest=<ConsumerAggregate>InterInvariantTest
```

> **One event name per business transition.** If §4 of the grouping template lists two separate events for the same publisher (e.g., `UpdateStudentNameEvent` and `AnonymizeStudentEvent`), treat each as a separate invocation even if they flow to the same consumer.

> **STOP after Phase 4.** List every event wired and every inter-invariant added. Confirm all tests pass. Ask: "Phase 4 complete. Ready to proceed to Phase 5 (full suite)?"

---

## Phase 5 — Full Suite

By this point, all individual tests have been written and run incrementally during Phases 2–4. Phase 5 is about the tests that couldn't be written earlier because they require multiple aggregates and functionalities to be in place simultaneously.

### 5.1 Cross-functionality concurrency tests

Write any remaining Spock tests that span multiple functionalities — specifically, concurrent interleaving scenarios where two different operations race against each other:

```groovy
def "concurrent interleaving — <Op1> vs <Op2>"() {
    given:
    def f1 = new <Op1>FunctionalitySagas(...)
    def f2 = new <Op2>FunctionalitySagas(...)

    when:
    f1.executeUntilStep("<stepName>", uow1)
    f2.executeWorkflow(uow2)   // f2 completes while f1 holds semantic lock
    f1.resumeWorkflow(uow1)    // f1 tries to continue

    then:
    thrown(<App>Exception)     // or assert committed state
}
```

Reference: `applications/quizzes/src/test/groovy/.../sagas/coordination/AddParticipantAndUpdateStudentNameTest.groovy`

### 5.2 Run the full suite

```bash
cd applications/<appName>
mvn clean -Ptest-sagas test
```

Fix all failures. Do not add the `-Ptest-tcc` profile — TCC stubs are not expected to pass.

---

## Checklist

**Phase 0**
- [ ] `pom.xml` with simulator dependency and `test-sagas` profile
- [ ] Shared exception class + error message enum
- [ ] `BeanConfigurationSagas.groovy` skeleton created
- [ ] Base Spock test class created

**Phase 1**
- [ ] Domain model and aggregate grouping templates read
- [ ] Rule classification table confirmed with user (Layer 3 rules include step name)

**Phase 2**
- [ ] All aggregates scaffolded (base + Saga + Causal stub + factories + repos + service)
- [ ] Snapshot fields added to each aggregate (from §2 of grouping template)
- [ ] Layer 1 intra-invariants added to all aggregates
- [ ] Creation test passing per aggregate

**Phase 3**
- [ ] Command classes for all functionalities
- [ ] Sagas functionalities for all cross-aggregate operations (TCC stubs created)
- [ ] Layer 3 `setForbiddenStates` wired in relevant saga steps
- [ ] Layer 2 service guards added
- [ ] Tests passing per functionality (happy path + invariant violations + guard violations)

**Phase 4**
- [ ] `/inter-invariant` invoked per (event, consumer) pair (event class + subscription + handler + polling + tracked field + update functionality)
- [ ] Inter-invariant tests passing per consumer aggregate

**Phase 5**
- [ ] Cross-functionality concurrency tests written and passing
- [ ] Full suite green: `mvn clean -Ptest-sagas test`
