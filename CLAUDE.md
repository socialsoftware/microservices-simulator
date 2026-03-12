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

## Core Concept Glossary

- **Aggregate** — unit of consistency; each write creates a new row; `aggregateId` is the logical identity; `version` is global. → [`docs/concepts/aggregate.md`](docs/concepts/aggregate.md)
- **Unit of Work** — coordinates reads and writes for one functionality execution; committed or aborted atomically. Saga: `SagaUnitOfWork`. TCC: `CausalUnitOfWork`.
- **Functionality** — a `WorkflowFunctionality` subclass that orchestrates a cross-service operation as a DAG of `Step`s. → [`docs/concepts/sagas.md`](docs/concepts/sagas.md)
- **Sagas** — concurrency protocol using semantic locks (`SagaState`); conflicting steps declare `forbiddenStates`. → [`docs/concepts/sagas.md`](docs/concepts/sagas.md)
- **TCC (Causal)** — concurrency protocol using field-level merge; concurrent writes to different fields can both succeed. → [`docs/concepts/tcc.md`](docs/concepts/tcc.md)
- **Inter-invariant** — cross-aggregate consistency rule maintained via domain events (eventually consistent); consumer caches publisher state locally. → [`docs/concepts/inter-invariants.md`](docs/concepts/inter-invariants.md)

---

## Key File Locations

| Concern | Path |
|---------|------|
| Base aggregate | `simulator/.../ms/domain/aggregate/Aggregate.java` |
| Saga aggregate interface | `simulator/.../ms/sagas/aggregate/SagaAggregate.java` |
| TCC aggregate interface | `simulator/.../ms/causal/aggregate/CausalAggregate.java` |
| Workflow base | `simulator/.../ms/coordination/workflow/WorkflowFunctionality.java` |
| Execution aggregate (with inter-invariants) | `applications/.../execution/aggregate/Execution.java` |
| Tournament saga functionality (multi-step) | `applications/.../tournament/coordination/sagas/AddParticipantFunctionalitySagas.java` |
| Test bean config (Sagas) | `applications/.../quizzes/BeanConfigurationSagas.groovy` |
| Base Spock test class | `applications/.../quizzes/QuizzesSpockTest.groovy` |
| Error messages | `applications/.../quizzes/microservices/exception/QuizzesErrorMessage.java` |

---

## Documentation

| Topic | Path |
|-------|------|
| Aggregate versioning | [`docs/concepts/aggregate.md`](docs/concepts/aggregate.md) |
| Domain events | [`docs/concepts/events.md`](docs/concepts/events.md) |
| Sagas semantic locks | [`docs/concepts/sagas.md`](docs/concepts/sagas.md) |
| TCC merge | [`docs/concepts/tcc.md`](docs/concepts/tcc.md) |
| Inter-invariants | [`docs/concepts/inter-invariants.md`](docs/concepts/inter-invariants.md) |
| Implement a new application | [`docs/guides/implement-new-application.md`](docs/guides/implement-new-application.md) |
| Implement an aggregate | [`docs/guides/implement-aggregate.md`](docs/guides/implement-aggregate.md) |
| Implement an event | [`docs/guides/implement-event.md`](docs/guides/implement-event.md) |
| Implement a functionality | [`docs/guides/implement-functionality.md`](docs/guides/implement-functionality.md) |
| Worked examples | [`docs/examples/README.md`](docs/examples/README.md) |

---

## Available Skills

Invoke these with `/skill-name <arguments>` when implementing new features in `applications/quizzes/`:

| Skill | When to use | Invoke with |
|-------|------------|-------------|
| `inter-invariant` | Add a cross-aggregate consistency rule via domain events | `/inter-invariant <ConsumerAggregate> <condition>` |
| `new-aggregate` | Scaffold a new domain aggregate (base + Saga + TCC + factory + repo) | `/new-aggregate <AggregateName>` |
| `new-event` | Add a new domain event with subscriptions, handler, and polling | `/new-event <EventName> <publisher> <consumer>` |
| `new-functionality` | Implement a new cross-service operation (Sagas + TCC + command handler + controller) | `/new-functionality <Name> <aggregates...>` |
