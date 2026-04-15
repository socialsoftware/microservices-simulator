# New Application Development Workflow

This document captures the end-to-end workflow for implementing a new application on the
microservices-simulator, using the skill chain under `.claude/skills/` and the documentation
under `docs/`.

---

## Human vs. AI Division of Labour

- **Human authors:** domain model template + aggregate grouping template
  (entities, rules, aggregate groupings, events — fixed inputs, never modified by the AI)
- **AI decides:** consistency layer per §3.2 rule, then executes each phase using the skill chain

---

## Phase Structure

```
/new-application
  │
  ├─ Phase 0 — Bootstrap
  │     pom.xml, module skeleton, exception class, error message enum
  │     BeanConfigurationSagas.groovy skeleton + base Spock test class  ← front-loaded
  │     STOP → confirm
  │
  ├─ Phase 1 — Read templates & classify rules
  │     Reads: {App}-domain-model.md + {App}-aggregate-grouping.md
  │     Applies: docs/concepts/decision-guide.md  ←  the §3.2 flowchart
  │     Output: Rule classification table + work plan
  │       (Layer 3 rules include the step name they attach to)
  │     STOP → user confirms before any code
  │
  ├─ Phase 2 — Aggregate scaffolding
  │     For each aggregate in §1 of grouping:
  │       /new-aggregate <AggregateName>
  │         → base class, SagaXxx, CausalXxx stub
  │         → factories (Sagas + TCC stub), repos, service stub
  │       add snapshot fields (from §2 of grouping)
  │       /intra-invariant for all §3.1 rules + any §3.2 reclassified to Layer 1
  │       register in BeanConfigurationSagas.groovy
  │       run creation test → fix before next aggregate
  │     STOP → confirm all creation tests pass
  │
  ├─ Phase 3 — Cross-service functionalities
  │     For each row in §4 of domain model:
  │       /new-functionality <Name> <PrimaryAggregate> [others...]
  │         → Sagas workflow (full) + TCC stub
  │         → command(s), command handler, Functionalities entry, controller
  │       Wire Layer 3 rules via setForbiddenStates on relevant saga steps
  │       run functionality tests → fix before next functionality
  │     Add Layer 2 rules:
  │       /service-guard <ServiceName> <method> <precondition>
  │     STOP → confirm all functionality tests pass
  │
  ├─ Phase 4 — Event wiring & inter-invariants
  │     For each row in §4 of grouping (one per event-consumer pair):
  │       /inter-invariant <ConsumerAggregate> <condition>
  │         → event class, subscription, handler, polling, EventProcessing chain,
  │           tracked field, update functionality, service state-update method
  │       run inter-invariant test → fix before next event
  │     STOP → confirm all inter-invariant tests pass
  │
  └─ Phase 5 — Full Suite
        Cross-functionality concurrency tests (executeUntilStep + resumeWorkflow)
        mvn clean -Ptest-sagas test
```

---

## Skill-to-Phase Mapping

| Skill | Called from | Produces |
|---|---|---|
| `/new-aggregate` | Phase 2 | base class, Saga variant, Causal stub, factories, repos, service stub |
| `/intra-invariant` | Phase 2 (per §3.1 + reclassified §3.2) | `invariantXxx()` helper + `verifyInvariants()` call |
| `/new-functionality` | Phase 3 | Sagas workflow, TCC stub, command, handler, Functionalities entry, controller |
| `/service-guard` | Phase 3 (Layer 2) | guard check + optional repo query + error constant |
| `/inter-invariant` | Phase 4 (Layer 4) | event class, subscription, handler, polling, EventProcessing chain, tracked field, update functionality, service method |

---

## Invariant Decision Flowchart

Applied in Phase 1 to every §3.2 rule. Source: `docs/concepts/decision-guide.md`.

```
For each §3.2 rule:

  Does it involve only data inside ONE aggregate (incl. snapshot fields)?
  └─ YES → Layer 1   /intra-invariant

  Must it be checked synchronously?
  └─ YES: reads only the mutated aggregate's own table?
        └─ YES → Layer 2   /service-guard
        └─ NO  → Layer 3   /new-functionality + setForbiddenStates
  └─ NO (eventual ~1 s lag acceptable, no blocking needed)?
        └─ YES → Layer 4   /inter-invariant  (caches state only — never blocks)
```

---

## Architectural Restrictions (enforced by skill patterns)

| Constraint | Rule | Skill that enforces it |
|---|---|---|
| Service loads only its own aggregate type | R1 | `new-aggregate` service stub |
| A Service may only inject its own aggregate's components (no foreign services or repositories) | R2 | `new-aggregate` Step 8 + `new-functionality` Step 7 |
| Cross-aggregate state through DTOs only | R3 | `new-functionality` command classes |
| Mutation steps must declare `forbiddenStates` | R4 | `new-functionality` Step 3 |
| Subscriptions belong in consumer only | R5 | `inter-invariant` Step 6 |
| Both Sagas + Causal variants required | R6 | `new-aggregate` Steps 5–7 |
| `verifyInvariants()` must not DB-read | R7 | `intra-invariant` doc block + `service-guard` placement |
| DTOs are immutable | R8 | `new-aggregate` DTO pattern |

---

## Phase 1 — Rule Classification Table Format

Produced at the end of Phase 1, confirmed by the user before any code is written.

```
Rule Classification Table — {AppName}

§3.1 Rules (all → Layer 1):
  - {RULE_NAME}: Layer 1 → /intra-invariant on {AggregateName}

§3.2 Rules:
  - {RULE_NAME}: Layer {1|2|3|4} → {skill(s)}
    [Layer 3: attached to step "{StepName}" of /new-functionality {FunctionalityName}]

Aggregates to scaffold:
  - {AggregateName}

Functionalities (from §4 of domain model):
  - {FunctionalityName}: /new-functionality {FunctionalityName} {PrimaryAggregate} [{OtherAggregates...}]

Events to wire (from §4 of grouping):
  - {EventName}: {Publisher} → {Consumer(s)}
    → /inter-invariant {ConsumerAggregate} <condition>
```
