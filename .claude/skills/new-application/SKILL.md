---
name: new-application
description: Bootstrap a new application on the microservices-simulator from scratch. Guides through all 5 phases: reading human-authored domain templates, aggregate scaffolding, cross-service functionalities, event wiring, and tests. Arguments: "<AppName> [short description of the domain]"
argument-hint: "<AppName> [short description of the domain]"
---

# Bootstrap New Application: $ARGUMENTS

You are building a new application on top of the simulator library from scratch. Use `applications/quizzes/` as a reference throughout.

> **Critical constraints:**
> 1. **Domain model and aggregate boundaries are human-defined.** The domain expert has filled the templates `{AppName}-domain-model.md` and `{AppName}-aggregate-grouping.md` before this skill is invoked. Do not change entity definitions, aggregate groupings, or rule semantics — read them as given.
> 2. **Consistency layer placement is AI-decided.** For each rule in the domain model, use `docs/concepts/decision-guide.md` to classify it into the correct layer (1, 3, 5, or 6) and confirm with the user before coding.
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
   - `test-sagas` profile (Sagas only — do not add `test-tcc` unless TCC is explicitly required)
4. Define shared exception class and error message enum (e.g., `<App>Exception.java`, `<App>ErrorMessage.java`)

---

## Phase 1 — Read Templates and Classify Rules

This phase has no code output. It produces a classification table that must be confirmed by the user before Phase 2 begins.

### 1.1 Read the human-authored domain templates

Read both files as provided by the domain expert:
- `{AppName}-domain-model.md` — entities, fields, relationships, §3.1 rules, §3.2 rules
- `{AppName}-aggregate-grouping.md` — aggregate groupings (§1), snapshots (§2), event DAG (§3)

Extract:
- List of aggregates (from §1 of grouping template)
- List of snapshot fields per aggregate (from §2 of grouping template)
- List of event dependencies (from §3 of grouping template — each arrow is one event)
- List of §3.1 rules (single-entity — always Layer 1)
- List of §3.2 rules (cross-entity — classify below)

### 1.2 Classify §3.2 rules

For each §3.2 rule, apply the decision flowchart in `docs/concepts/decision-guide.md` and assign it to Layer 3, 5, or 6 (reactive or guard).

### 1.3 Output classification table and confirm with user

Produce this table and **stop — do not proceed to Phase 2 until the user approves the classification**:

```
Rule Classification Table — {AppName}

§3.1 Rules (all → Layer 1):
  - {RULE_NAME}: Layer 1 → /intra-invariant

§3.2 Rules:
  - {RULE_NAME}: Layer {3|5|6 reactive|6 guard} → {skill(s)}
  - ...

Aggregates to scaffold: {list}
Events to wire (from §3 DAG): {list}
Functionalities to implement: {list}
```

Ask: "Does this classification look correct? Any adjustments before I start generating code?"

---

## Phase 2 — Aggregate Scaffolding

For each aggregate listed in §1 of the grouping template, invoke:
```
/new-aggregate <AggregateName> [description]
```

Each invocation produces:
- Base abstract class with `verifyInvariants()` (with any §3.1 intra-invariants) and `getEventSubscriptions()` (initially empty)
- `Saga<Aggregate>` with `<Aggregate>SagaState` enum
- `Causal<Aggregate>` — **TCC stub** (empty `getMutableFields()`, `mergeFields() → return this`)
- Sagas and TCC factories (TCC factory throws `UnsupportedOperationException`)
- Repositories (Saga and TCC stub)
- Service stub

Reference implementations: `applications/quizzes/.../execution/`, `applications/quizzes/.../tournament/`

---

## Phase 3 — Cross-Service Functionalities

For each operation that touches more than one aggregate (identified in Phase 1), invoke:
```
/new-functionality <OperationName> <PrimaryAggregate> [other aggregates...]
```

Each invocation produces:
- `<Op>FunctionalitySagas.java` — Sagas workflow with steps and `forbiddenStates` (full implementation)
- `<Op>FunctionalityTCC.java` — **TCC stub** (empty `buildWorkflow()`)
- Entry point in `<Primary>Functionalities.java` (Sagas wired; TCC throws `UnsupportedOperationException`)
- Command handler and service method
- REST controller (if the operation is exposed via HTTP)

---

## Phase 4 — Event Wiring and Invariants

Work through the event DAG from §3 of the grouping template. Each arrow `A ──► B` represents one event subscription.

For each arrow:
```
/new-event <EventName> <PublisherAggregate> <ConsumerAggregate>
```

For each Layer 4 rule:
```
/inter-invariant <ConsumerAggregate> <condition>
```

For each Layer 2 rule:
```
/service-guard <ServiceName> <operation-method> <precondition>
```

Layer 1 intra-invariants from §3.1 should already be in place from Phase 2 (each aggregate's `verifyInvariants()`).

---

## Phase 5 — Tests

For each functionality, write Spock tests in `src/test/groovy/.../sagas/`:

- [ ] Happy-path test
- [ ] Intra-invariant violation tests (§3.1 rules)
- [ ] Layer 2 guard violation tests (§3.2 rules implemented at Layer 2)
- [ ] Inter-invariant (eventual) tests — call `handle<Xxx>Events()` manually, assert cached state
- [ ] Concurrent interleaving tests (`executeUntilStep` + `resumeWorkflow`)

Run:
```bash
cd applications/<appName>
mvn clean -Ptest-sagas test
```

---

## Checklist

- [ ] `pom.xml` with simulator dependency and `test-sagas` profile
- [ ] Shared exception + error message class
- [ ] Domain model and aggregate grouping templates confirmed with classification table
- [ ] All aggregates scaffolded (base + Saga + Causal stub + factories + repos + service)
- [ ] Command classes for all operations
- [ ] Sagas functionalities for all cross-aggregate operations (TCC stubs created)
- [ ] Domain events wired per §3 event DAG
- [ ] Layer 4 inter-invariants implemented
- [ ] Layer 2 service guards added (where classification table requires)
- [ ] Sagas tests green: `mvn clean -Ptest-sagas test`
