# CLAUDE.md — Navigation Hub

This file is the entry point for both humans and AI agents. Each section links to the authoritative source for deeper information.

---

## Build Commands

```bash
# Install core library first
cd simulator && mvn install

# Run Quizzes application tests
cd applications/quizzes

mvn clean -Ptest-sagas test          # all sagas tests
mvn clean -Ptest-tcc test            # all TCC tests
mvn clean -Ptest-sagas test -Dtest=AddParticipantAndUpdateStudentNameTest  # single class
```

---

## Module Map

| Module | Purpose | Local context |
|--------|---------|---------------|
| `simulator/` | Core library: `Aggregate`, `Workflow`, `UnitOfWork`, `CommandGateway`, events | [`simulator/CLAUDE.md`](simulator/CLAUDE.md) |
| `applications/quizzes/` | Quizzes case study built on the simulator | [`applications/quizzes/CLAUDE.md`](applications/quizzes/CLAUDE.md) |

---

Core concepts and architectural overview → [`docs/architecture.md`](docs/architecture.md)

---

## Key File Locations

| Concern | Path |
|---------|------|
| Execution aggregate (with inter-invariants) | `applications/.../execution/aggregate/Execution.java` |
| Tournament saga functionality (multi-step) | `applications/.../tournament/coordination/sagas/AddParticipantFunctionalitySagas.java` |
| Test bean config (Sagas) | `applications/.../quizzes/BeanConfigurationSagas.groovy` |
| Base Spock test class | `applications/.../quizzes/QuizzesSpockTest.groovy` |
| Error messages | `applications/.../quizzes/microservices/exception/QuizzesErrorMessage.java` |

---

## Documentation

| Topic | Path |
|-------|------|
| Application architecture & restrictions | [`docs/architecture.md`](docs/architecture.md) |
| Aggregate versioning | [`docs/concepts/aggregate.md`](docs/concepts/aggregate.md) |
| Domain events + canonical wiring snippet | [`docs/concepts/events.md`](docs/concepts/events.md) |
| Sagas semantic locks | [`docs/concepts/sagas.md`](docs/concepts/sagas.md) |
| TCC merge | [`docs/concepts/tcc.md`](docs/concepts/tcc.md) |
| Invariant & guard taxonomy | [`docs/concepts/consistency-enforcement.md`](docs/concepts/consistency-enforcement.md) |
| Consistency layer decision flowchart (AI agent) | [`docs/concepts/decision-guide.md`](docs/concepts/decision-guide.md) |
| TCC placeholder stub pattern | [`docs/concepts/tcc-placeholder-pattern.md`](docs/concepts/tcc-placeholder-pattern.md) |
| Domain model template | [`docs/templates/domain-model-template.md`](docs/templates/domain-model-template.md) |
| Aggregate grouping template | [`docs/templates/aggregate-grouping-template.md`](docs/templates/aggregate-grouping-template.md) |
| Worked examples | [`docs/examples/README.md`](docs/examples/README.md) |

---

## Available Skills

Invoke these with `/skill-name <arguments>` when implementing new features in `applications/quizzes/`:

| Skill | When to use | Invoke with |
|-------|------------|-------------|
| `new-application` | Bootstrap a new application from human-authored templates (reads domain model, classifies rules, implements Sagas) | `/new-application <AppName> [description]` |
| `new-aggregate` | Scaffold a new domain aggregate (base + Saga + TCC stub + factory + repo) | `/new-aggregate <AggregateName>` |
| `new-functionality` | Implement a cross-service operation (Sagas only + TCC stub + command handler + controller) | `/new-functionality <Name> <aggregates...>` |
| `new-event` | Add a domain event with subscription, handler, and polling | `/new-event <EventName> <publisher> <consumer>` |
| `intra-invariant` | Add a Layer 1 intra-invariant (`verifyInvariants` check) inside a single aggregate | `/intra-invariant <AggregateName> <rule-description>` |
| `service-guard` | Add a Layer 2 service-layer guard (DB read or input validation) that throws before any mutation | `/service-guard <ServiceName> <operation-method> <precondition>` |
| `inter-invariant` | Add a Layer 4 inter-invariant (event-driven cache sync, no blocking) | `/inter-invariant <ConsumerAggregate> <condition>` |
