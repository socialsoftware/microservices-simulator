# AGENTS.md — Navigation Hub

This file is the entry point for the automated agent harness that implements this codebase. This enables **progressive disclosure**: agents start with a small, stable entry point and are taught where to look next, rather than being overwhelmed up front.

**What this harness does:** it generates a simulator application from a domain-model + aggregate-grouping spec pair, aggregate by aggregate.

**Architecture principle:** The current implementation targets the sagas consistency pattern only, but must remain **profile-agnostic at the service layer**. Concretely: `*Service` classes inject factories and repositories via abstract interfaces (e.g. `CourseFactory`, `CourseCustomRepository`), never via the concrete sagas-profile classes (e.g. `SagasCourseFactory`). This keeps the door open to adding a TCC or other pattern later without touching service code.

Docs and skills are **living artifacts**, but they are only edited **between** runs. While a run
is in progress (see § Harness freeze) an unclear doc is logged as friction and left alone — the
friction log is the input to the next round of harness improvements.

When in doubt, ask clarifying questions.

---

## Harness freeze

A generation run is IN PROGRESS if and only if any `applications/*/plan.md` contains an
unchecked `- [ ]` box.

While a run is in progress, the following are **read-only for every agent** — inside a skill
invocation and outside one:

- `docs/**`
- `.claude/**`
- `simulator/**`
- `AGENTS.md`, `CLAUDE.md`

Friction with the harness is **recorded, never fixed**. Append a row to
`applications/{app-name}/friction-log.md` and continue on the best defensible reading of the
docs. If you are genuinely blocked and no defensible reading exists: **halt and report to the
human.** Do not edit. Do not guess.

Writes to `applications/{app-name}/**` (source, tests, `plan.md`, `retros/`, `reviews/`,
`friction-log.md`) are unaffected by the freeze.

---

## Build Commands

```bash
# Install core library first
cd simulator && mvn install

# Run tests in a specific application
cd applications/<appName>

mvn clean -Ptest-sagas test                                     # all sagas tests
mvn clean -Ptest-sagas test -Dtest=ClassName                   # single test class
```

---

## Module Map

| Module | Purpose                                                                                                       | Local context |
|--------|---------------------------------------------------------------------------------------------------------------|---------------|
| `simulator/` | Core library: `Aggregate`, `Workflow`, `UnitOfWork`, `CommandGateway`, events                                 | [`simulator/AGENTS.md`](simulator/AGENTS.md) |
| `applications/{app-name}/` | A generated application; its spec pair, `plan.md`, `retros/`, `reviews/` and `friction-log.md` live here | — |

---

## Optional: rtk (token-optimized CLI proxy)

[rtk](https://github.com/rtk-ai/rtk) filters/summarizes verbose CLI output (git, build tools, etc.) before it reaches the agent's context. It's optional and per-developer — not required to work on this repo.

To enable it for your own Claude Code sessions in this project:

```bash
rtk --version   # confirm it's installed; see github.com/rtk-ai/rtk for install instructions
rtk init        # patches your local .claude/settings.local.json with a Bash PreToolUse hook
```

This only touches your personal, gitignored `settings.local.json` — it does not affect other contributors or get committed.

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
