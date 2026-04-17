---
name: new-application
description: Bootstrap a new application on the microservices-simulator from scratch. Guides through all 5 phases: reading human-authored domain templates, aggregate scaffolding, cross-service functionalities, event wiring, and tests. Arguments: "<path/to/{AppName}-domain-model.md> <path/to/{AppName}-aggregate-grouping.md>"
argument-hint: "<path/to/{AppName}-domain-model.md> <path/to/{AppName}-aggregate-grouping.md>"
---

# Bootstrap New Application: $ARGUMENTS

You are building a new application on top of the simulator library from scratch. Use `applications/quizzes/` as a reference throughout.

> **Critical constraints:**
> 1. **Domain model and aggregate boundaries are human-defined.** The domain expert has filled the templates `{AppName}-domain-model.md` and `{AppName}-aggregate-grouping.md` before this skill is invoked. Do not change entity definitions, aggregate groupings, functionalities, event names, or rule semantics — read them as given.
> 2. **Consistency layer placement is AI-decided.** For each rule in the domain model, use `docs/concepts/decision-guide.md` to classify it into the correct layer (1, 2, 3, or 4) and confirm with the user before coding.
> 3. **Sagas only.** Development focuses on the Sagas pattern exclusively.

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
   Use the app name lowercased with hyphens removed as the package leaf (e.g., `quizzes-full` → `pt.ulisboa.tecnico.socialsoftware.quizzesfull`).
3. Add the simulator dependency to `pom.xml` and define the Maven profile following `applications/quizzes/pom.xml`:
   - `test-sagas` profile only.
   - **Only carry over the `test-sagas` profile.** Omit the microservice deployment profiles (`answer-service`, `execution-service`, …), communication layer profiles (`local`, `stream`, `grpc`), and kubernetes profile — those are for the quizzes production deployment and are not needed in a new application.
4. Define shared exception class and error message constants class (e.g., `<App>Exception.java`, `<App>ErrorMessage.java`). Follow `applications/quizzes/src/main/java/.../exception/QuizzesException.java` and `QuizzesErrorMessage.java` as references. Note: `QuizzesErrorMessage` is a `final class` with `static final String` constants — not a Java `enum`.

   Also create `ServiceMapping.java` — an enum that maps each aggregate's service name to a lowercase string identifier (e.g., `TOURNAMENT("tournament")`). This is **required for tests**: the `LocalCommandGateway` routes commands by looking up the Spring bean named `{serviceName}CommandHandler`, where `serviceName` comes from `ServiceMapping`. Populate it with all aggregates listed in §1 of the grouping template. Follow `applications/quizzes/src/main/java/.../quizzes/ServiceMapping.java` as a reference.

5. Create the test infrastructure (front-loaded — needed to run creation tests after each aggregate in Phase 2):
   - **`src/test/resources/application-test.properties`** — copy from `simulator/src/main/resources/application-test.properties`. This file is required because `BeanConfigurationSagas.groovy` loads it via `@PropertySource("classpath:application-test.properties")`.
   - **`BeanConfigurationSagas.groovy`** — `@TestConfiguration` class with the infrastructure skeleton only: `CommandGateway`, `EventService`, `EventApplicationService`, `SagaUnitOfWorkService`, `ImpairmentService`, and related beans. Do **not** add aggregate-specific service/factory/repository beans yet — add each one in step 2.4 of Phase 2 as the aggregate is scaffolded. Follow `applications/quizzes/src/test/groovy/.../BeanConfigurationSagas.groovy` as a reference for which infrastructure beans are needed.
   - **Base Spock test class** (e.g., `<AppName>SpockTest.groovy`) — extends `SpockTest` and provides `@Autowired` fields for all services plus shared `setup()` helpers (e.g., `create<Entity>()` factory methods). Follow `applications/quizzes/src/test/groovy/.../QuizzesSpockTest.groovy` as a reference.

6. Create deployment infrastructure (reference: `applications/quizzes/`):
   - **`<AppName>Simulator.java`** — main `@SpringBootApplication` entry point. Annotate with `@EnableJpaRepositories`, `@EntityScan`, `@EnableTransactionManagement`, `@EnableJpaAuditing`, `@EnableScheduling`, `@EnableRetry`, scanning both `ms.*` and `<appPkg>.*`. Implements `InitializingBean` to call `eventService.clearEventsAtApplicationStartUp()`.
   - **`<Service>ServiceApplication.java`** for every aggregate in §1 of the grouping template — placed in `microservices/<service>/`. Annotate with `@Profile("<service>-service")`, `@SpringBootApplication`, `@EnableJpaRepositories`, `@EntityScan` (scans only that service's package + `ms.*`; `@EntityScan` also adds `<appPkg>.events` for domain events created in Phase 4), and `@EnableScheduling`. Same `InitializingBean` startup hook as the main entry point.
   - **`src/main/resources/application.yaml`** — base datasource + JPA config; `remote` profile block (RabbitMQ, Eureka, event-subscriber bindings for every service); `stream` profile block (command producer/consumer channels for every service); `grpc` and `kubernetes` profile blocks. Use `@activatedProperties@` as the Spring profiles active placeholder (filtered by Maven resource filtering configured in `pom.xml`).
   - **`src/main/resources/application-kubernetes.yaml`** — DB URL/user/password from env vars.
   - **`src/main/resources/application-test.yaml`** — H2 in-memory DB, discovery off, `local.messaging.serialize: true` (mimics remote serialization in tests).
   - **`src/main/resources/application-<service>-service.yaml`** for every service — `spring.application.name`, datasource URL, cloud stream command bindings, `server.port`, `grpc.server.port`, and API gateway route paths. Assign non-conflicting ports (check existing applications for taken ranges).
   - **`Dockerfile`** — multi-stage build: install simulator to local Maven, package app JAR, copy into minimal JRE image. `START_CLASS` and JAR name reference the new app's `<AppName>Simulator`.
   - **`Dockerfile.test`** — two stages: cache simulator in `.m2`, then copy app source for `mvn test`.
   - **`.gitignore`** and **`.dockerignore`** — identical to `applications/quizzes/`.
   - **Update `pom.xml`**: add `<start-class>` property; add `<resources><resource><filtering>true</filtering>` build block for `@activatedProperties@` substitution; add `sagas`, `local`, `stream`, `grpc`, `distributed-version`, `kubernetes` profiles; add one microservice profile per service (sets `<start-class>` to its `ServiceApplication` and `<activatedProperties>` to `<service>-service,${transaction.pattern},${communication.layer}${version.suffix}`).

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
- **Event names and payloads** — from §4 of grouping template (each row is one `/wire-event` call in Phase 4)
- **Functionalities** — from §4 of domain model template (each row is one `/implement-functionality` call in Phase 3)
- **§3.1 rules** — single-entity, always Layer 1
- **§3.2 rules** — cross-entity, classify below

### 1.2 Classify §3.2 rules

For each §3.2 rule, apply the decision flowchart in `docs/concepts/decision-guide.md` and assign it to Layer 1, 2, 3, or 4.

Key reminder:
- All data already inside the same aggregate (including its snapshot fields) → **Layer 1** (intra-invariant)
- Synchronous check, reads only the aggregate being mutated → **Layer 2** (service guard, applied inline by `/implement-functionality`)
- Synchronous check, reads a *different* aggregate → **Layer 3** (`setForbiddenStates` on a saga step, wired by `/implement-functionality`)
- Eventual cache sync, no blocking needed → **Layer 4** (inter-invariant, wired by `/wire-event`)

### 1.3 Output classification table and confirm with user

Produce this table and **stop — do not proceed to Phase 2 until the user approves the classification**:

```
Rule Classification Table — {AppName}

§3.1 Rules (all → Layer 1):
  - {RULE_NAME}: Layer 1 (intra-invariant on {AggregateName})

§3.2 Rules:
  - {RULE_NAME}: Layer {1|2|3|4}
    [if Layer 2: service guard on {ServiceName}.{method} — applied by /implement-functionality {FunctionalityName}]
    [if Layer 3: wired into step "{StepName}" of /implement-functionality {FunctionalityName}]

Aggregates to scaffold: {list}

Functionalities to implement (from §4 of domain model):
  - {FunctionalityName}: /implement-functionality {FunctionalityName} {PrimaryAggregate} [{OtherAggregates...}]
    [Layer 2 guard: {GUARD_NAME} on {ServiceName}.{method}]  ← only if applicable

Events and inter-invariants to wire (from §4 of grouping):
  - {EventName}: {Publisher} → {Consumer(s)}
    → /wire-event {ConsumerAggregate} {EventName}
    (if multiple consumers: one /wire-event call per consumer)
```

Ask: "Does this classification look correct? Any adjustments before I start Phase 2 (aggregate scaffolding)?"

### 1.4 Write `plan.md` after user confirms

Once the user approves the classification, write `applications/<appName>/plan.md` — the authoritative progress tracker for all remaining phases. This file is the single source of truth that any agent (or the user) can open to see what has been done and what remains.

Structure the file with one section per phase. Use GitHub-flavored **checkboxes** (`- [ ]` / `- [x]`) for every discrete work item so that the executing agent can tick items off as it completes them.

**Ordering Phase 2 aggregates:** Before writing the Phase 2 section, perform a topological sort of the aggregates based on their **creation-test dependencies** — which other aggregates' `create<X>()` helpers must already be registered in `BeanConfigurationSagas` when this aggregate's creation test runs. An aggregate A must precede B if B's creation test needs to call `createA()` (or any helper that routes a command through A's service). Use the snapshot fields in §2 of the grouping template and the event DAG in §3 as signals, but the key criterion is which aggregates are needed *at creation time* (not just for event-driven updates wired in Phase 4). Roots (no upstream creation-time deps) come first.

Include:
- **Phase 2 section** — one sub-section per aggregate; each sub-section has checkboxes for: scaffold, snapshot fields added, Layer 1 intra-invariants added (list each rule), registered in `BeanConfigurationSagas`, creation test passes.
- **Phase 3 section** — one checkbox per functionality with its `/implement-functionality` invocation; list Layer 3 rules (and which saga step they wire into) as sub-bullets; if a Layer 2 guard applies, add a nested `- [ ] Layer 2 guard applied: <GUARD_NAME>` checkbox inside the functionality block.
- **Phase 4 section** — one checkbox per (event, consumer) inter-invariant pair grouped by event, with the expected test class noted.
- **Phase 5 section** — an explicit, named list of T4, T5, and T6 tests (not just "examples"), plus the full suite run checkbox. Derive the list using the rules in `docs/concepts/testing.md § Phase 5 — How to Derive the Test List`:
  - **T4 Cross-Functionality tests**: build a map of aggregate → functionalities; for each aggregate shared by ≥2 functionalities, enumerate the high-risk (A, B) pairs and name a `<PrimaryOpA>And<PrimaryOpB>Test`
  - **T5 Fault tests**: one `<Functionality>FaultTest` per functionality whose saga has ≥2 steps
  - **T6 Async tests**: one `<Functionality>AsyncTest` for the 2–3 most concurrency-prone operations

### 1.5 Write `PROMPT.md` and `run.sh` after user confirms

Alongside `plan.md`, write two files that enable loop-based execution of Phases 2–5 in isolated Claude sessions.

**`applications/<appName>/PROMPT.md`** — application-specific loop driver:

```markdown
# Loop Driver — <AppName>

Application directory: applications/<appName>/
Domain model: applications/<appName>/<appName>-domain-model.md
Aggregate grouping: applications/<appName>/<appName>-aggregate-grouping.md

## Your task (one work unit)

1. Read `plan.md`.
2. Find the **first work unit in the active phase** with any unchecked sub-step:
   - Phase 2: first `### AggregateName` section with any unchecked checkbox
   - Phase 3: first functionality or service guard entry with an unchecked checkbox
   - Phase 4: first `/wire-event` line that is unchecked
   - Phase 5: first unchecked item
3. Invoke the appropriate skill for that work unit (see routing below).
4. Stop after completing one work unit. Do not advance to the next.

## Skill routing

| Active phase | Work unit found | Invoke |
|---|---|---|
| Phase 2 | `### <AggregateName>` with unchecked items | `/scaffold-aggregate <AggregateName>` |
| Phase 3 | Functionality entry with unchecked checkbox | `/implement-functionality <FunctionalityName>` |
| Phase 4 | `/wire-event <Consumer> <Event>` unchecked | `/wire-event <Consumer> <Event>` |
| Phase 5 | Any unchecked item | Write concurrency tests + `mvn clean -Ptest-sagas test` |
```

**`applications/<appName>/run.sh`** — phase-aware bash driver:

```bash
#!/usr/bin/env bash
# Runs one work unit from plan.md, then stops.
# Exits with a message when a phase boundary is crossed so you can review before continuing.
# Usage: ./run.sh            (auto-detects active phase)
#        while ./run.sh; do :; done   (loop within current phase; stops at phase boundary)
set -euo pipefail

PLAN="plan.md"

active_phase() {
  for p in 2 3 4 5; do
    if awk "/^## Phase $p/,/^## Phase $((p+1))/" "$PLAN" 2>/dev/null | grep -q '\- \[ \]'; then
      echo $p; return
    fi
  done
  echo "done"
}

phase=$(active_phase)

if [ "$phase" = "done" ]; then
  echo "All phases complete."
  exit 0
fi

echo "Active phase: $phase — running one work unit..."
cat PROMPT.md | claude

new_phase=$(active_phase)
if [ "$new_phase" != "$phase" ] || [ "$new_phase" = "done" ]; then
  echo ""
  echo "Phase $phase complete. Review plan.md, then re-run ./run.sh to begin Phase $new_phase."
  exit 1   # non-zero stops a while loop
fi
```

> **STOP after Phase 1.** Do not write any code until the user explicitly approves the classification table above. Once approved, the three generated files (`plan.md`, `PROMPT.md`, `run.sh`) are complete. Phases 2–5 are driven by `./run.sh` from the application directory — one work unit per invocation, pausing automatically at phase boundaries for review.
