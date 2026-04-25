# CLAUDE.md — Navigation Hub

Navigation hub for humans and AI agents — each section links to the authoritative source. This enables **progressive disclosure**: agents start with a small, stable entry point and are taught where to look next, rather than being overwhelmed up front.

**Current objective:** Build `quizzes-full` as a comparative implementation using the **same 8 aggregates** as the reference `quizzes` app. Use this comparison to validate and refine docs and skills. Only after completing all aggregates will the domain be extended further.

**Architecture principle:** The current implementation targets the sagas consistency pattern only, but must remain **profile-agnostic at the service layer**. Concretely: `*Service` classes inject factories and repositories via abstract interfaces (e.g. `CourseFactory`, `CourseCustomRepository`), never via the concrete sagas-profile classes (e.g. `SagasCourseFactory`). This keeps the door open to adding a TCC or other pattern later without touching service code.

Docs and skills are **living artifacts**. When something is unclear or you find yourself consulting the `quizzes/` reference app, that's a signal to update the docs or add a skill — the goal is a self-contained process that captures every pattern learned.

When in doubt, ask clarifying questions.

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
| `applications/quizzes-full/` | Comparative implementation of quizzes using same domain; validate docs/skills | [`quizzes-full-domain-model.md`](applications/quizzes-full/quizzes-full-domain-model.md), [`quizzes-full-aggregate-grouping.md`](applications/quizzes-full/quizzes-full-aggregate-grouping.md) |

---

## Documentation

| Topic | Path |
|-------|------|
| Application architecture & restrictions | [`docs/architecture.md`](docs/architecture.md) |
| Aggregate versioning + `getEventSubscriptions()` | [`docs/concepts/aggregate.md`](docs/concepts/aggregate.md) |
| Service layer patterns (read / create / mutate) | [`docs/concepts/service.md`](docs/concepts/service.md) |
| Commands, CommandHandler, ServiceMapping | [`docs/concepts/commands.md`](docs/concepts/commands.md) |
| Domain events + canonical wiring snippet | [`docs/concepts/events.md`](docs/concepts/events.md) |
| Sagas semantic locks | [`docs/concepts/sagas.md`](docs/concepts/sagas.md) |
| Rule-enforcement patterns & decision guide | [`docs/concepts/rule-enforcement-patterns.md`](docs/concepts/rule-enforcement-patterns.md) |
| Test taxonomy & templates | [`docs/concepts/testing.md`](docs/concepts/testing.md) |
| Domain model template | [`docs/templates/domain-model-template.md`](docs/templates/domain-model-template.md) |
| Aggregate grouping template | [`docs/templates/aggregate-grouping-template.md`](docs/templates/aggregate-grouping-template.md) |
| Aggregate-by-aggregate implementation workflow (AI agent harness) | [`docs/workflow.md`](docs/workflow.md) |

---
