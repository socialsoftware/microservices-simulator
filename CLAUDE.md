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
| Domain events | [`docs/concepts/events.md`](docs/concepts/events.md) |
| Sagas semantic locks | [`docs/concepts/sagas.md`](docs/concepts/sagas.md) |
| TCC merge | [`docs/concepts/tcc.md`](docs/concepts/tcc.md) |
| Invariant & guard taxonomy | [`docs/concepts/invariants.md`](docs/concepts/invariants.md) |
| Worked examples | [`docs/examples/README.md`](docs/examples/README.md) |

---

## Available Skills

Invoke these with `/skill-name <arguments>` when implementing new features in `applications/quizzes/`:

| Skill | When to use | Invoke with |
|-------|------------|-------------|
| `new-application` | Bootstrap a new application on the simulator from scratch (all 5 phases) | `/new-application <AppName> [description]` |
| `intra-invariant` | Add a Layer 1 intra-invariant (`verifyInvariants` check) inside a single aggregate | `/intra-invariant <AggregateName> <rule-description>` |
| `service-guard` | Add a Layer 3 service-layer guard that reads the DB and throws before any mutation | `/service-guard <ServiceName> <operation-method> <precondition>` |
| `inter-invariant` | Add a Layer 6 cross-aggregate consistency rule maintained via domain events (eventual) | `/inter-invariant <ConsumerAggregate> <condition>` |
| `new-aggregate` | Scaffold a new domain aggregate (base + Saga + TCC + factory + repo) | `/new-aggregate <AggregateName>` |
| `new-event` | Add a new domain event with subscriptions, handler, and polling | `/new-event <EventName> <publisher> <consumer>` |
| `new-functionality` | Implement a new cross-service operation (Sagas + TCC + command handler + controller) | `/new-functionality <Name> <aggregates...>` |
