# CLAUDE.md — Navigation Hub

Navigation hub for humans and AI agents — each section links to the authoritative source.

**Current objective:** Build `quizzes-full`, a complete Quizzes-Tutor implementation driven by AI agents via skills and documentation.

Docs and skills are **living artifacts**. When something is unclear or you find yourself consulting the `quizzes/` reference app, that's a signal to update the docs or add a skill — the goal is a self-contained process that captures every pattern learned.

---

## Build Commands

```bash
# Install core library first
cd simulator && mvn install

# Run tests in a specific application (example: quizzes reference app or quizzes-full)
cd applications/<appName>

mvn clean -Ptest-sagas test                                     # all sagas tests
mvn clean -Ptest-sagas test -Dtest=ClassName                   # single test class
```

---

## Module Map

| Module | Purpose                                                                                                       | Local context |
|--------|---------------------------------------------------------------------------------------------------------------|---------------|
| `simulator/` | Core library: `Aggregate`, `Workflow`, `UnitOfWork`, `CommandGateway`, events                                 | [`simulator/CLAUDE.md`](simulator/CLAUDE.md) |
| `applications/quizzes/` | Reference example — patterns and templates for new applications                                               | [`applications/quizzes/CLAUDE.md`](applications/quizzes/CLAUDE.md) |
| `applications/quizzes-full/` | In progress app being built by harnessing AI agents |  |

---

## Documentation

| Topic | Path |
|-------|------|
| Application architecture & restrictions | [`docs/architecture.md`](docs/architecture.md) |
| Aggregate versioning | [`docs/concepts/aggregate.md`](docs/concepts/aggregate.md) |
| Domain events + canonical wiring snippet | [`docs/concepts/events.md`](docs/concepts/events.md) |
| Sagas semantic locks | [`docs/concepts/sagas.md`](docs/concepts/sagas.md) |
| Invariant & guard taxonomy | [`docs/concepts/consistency-enforcement.md`](docs/concepts/consistency-enforcement.md) |
| Consistency layer decision flowchart (AI agent) | [`docs/concepts/decision-guide.md`](docs/concepts/decision-guide.md) |
| Test taxonomy & templates | [`docs/concepts/testing.md`](docs/concepts/testing.md) |
| Domain model template | [`docs/templates/domain-model-template.md`](docs/templates/domain-model-template.md) |
| Aggregate grouping template | [`docs/templates/aggregate-grouping-template.md`](docs/templates/aggregate-grouping-template.md) |

---
