# CLAUDE.md — Navigation Hub

This file is the entry point for both humans and AI agents. Each section links to the authoritative source for deeper information.

The current objective is to implement a new more complete version of the Quizzes-Tutor application (quizzes-full) with the use of AI agents through skills and documentation.
Both the documentation and skills are not static - they should evolve as we learn from the process of building quizzes-full, and as we identify gaps and areas for improvement. So any time something is unclear, or you find yourself needing to refer back to the reference quizzes app for guidance, that's a signal that we need to update the documentation or build a new skill to fill that gap.
The goal is to make the process as smooth and self-contained as possible for future applications, so we want to capture all the learnings and patterns from this implementation in our docs and skills.

---

## Build Commands

```bash
# Install core library first
cd simulator && mvn install

# Run tests in a specific application (example: quizzes reference app)
cd applications/quizzes

mvn clean -Ptest-sagas test          # all sagas tests
mvn clean -Ptest-tcc test            # all TCC tests
mvn clean -Ptest-sagas test -Dtest=AddParticipantAndUpdateStudentNameTest  # single class

# For new applications, run the same commands from applications/<appName>/
```

---

## Module Map

| Module | Purpose                                                                                                       | Local context |
|--------|---------------------------------------------------------------------------------------------------------------|---------------|
| `simulator/` | Core library: `Aggregate`, `Workflow`, `UnitOfWork`, `CommandGateway`, events                                 | [`simulator/CLAUDE.md`](simulator/CLAUDE.md) |
| `applications/quizzes/` | Reference example — patterns and templates for new applications                                               | [`applications/quizzes/CLAUDE.md`](applications/quizzes/CLAUDE.md) |
| `applications/quizzes-full/` | In progress app being built by harnessing AI agents | [`applications/quizzes-full/plan.md`](applications/quizzes-full/plan.md) |

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
| New-application loop-based workflow | [`docs/workflow.md`](docs/workflow.md) |
| TCC placeholder stub pattern | [`docs/concepts/tcc-placeholder-pattern.md`](docs/concepts/tcc-placeholder-pattern.md) |
| Domain model template | [`docs/templates/domain-model-template.md`](docs/templates/domain-model-template.md) |
| Aggregate grouping template | [`docs/templates/aggregate-grouping-template.md`](docs/templates/aggregate-grouping-template.md) |
| Worked examples | [`docs/examples/README.md`](docs/examples/README.md) |

---

## Available Skills

### Bootstrap

| Skill | When to use | Invoke with |
|-------|------------|-------------|
| `new-application` | Bootstrap a new application from scratch: reads domain templates, classifies rules, writes `plan.md` + `PROMPT.md` + `run.sh` | `/new-application <path/domain-model.md> <path/aggregate-grouping.md>` |

### Phase driver skills (loop entry points — one work unit per session)

| Skill | Phase | Invoke with |
|-------|-------|-------------|
| `scaffold-aggregate` | Phase 2 — one call per aggregate | `/scaffold-aggregate <AggregateName>` |
| `implement-functionality` | Phase 3 — one call per functionality | `/implement-functionality <FunctionalityName>` |
| `wire-event` | Phase 4 — one call per event-consumer pair | `/wire-event <ConsumerAggregate> <EventName>` |

