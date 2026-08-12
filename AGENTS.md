# AGENTS.md — Navigation Hub

This file is the entry point for the automated agent harness that implements this codebase. This enables **progressive disclosure**: agents start with a small, stable entry point and are taught where to look next, rather than being overwhelmed up front.

**What this harness does:** it generates a simulator application from a domain-model + aggregate-grouping spec pair, aggregate by aggregate.

**Architecture principle:** The current implementation targets the sagas consistency pattern only, but must remain **profile-agnostic at the service layer**. Concretely: `*Service` classes inject factories and repositories via abstract interfaces (e.g. `WarehouseFactory`, `WarehouseCustomRepository`), never via the concrete sagas-profile classes (e.g. `SagasWarehouseFactory`). This keeps the door open to adding a TCC or other pattern later without touching service code.

Docs and skills are **living artifacts**, edited both between runs and during them - see
§ Harness evolution for the two gates that govern in-run edits.

When in doubt, ask clarifying questions.

---

## Harness evolution

The harness is **self-healing**. When the docs or skills mislead an agent, the agent's job includes
repairing them, so the same mistake is never made twice. Every repair is recorded in
`applications/{app-name}/harness-log.md` and committed separately with a `harness:` prefix, so the
harness delta of a run is exactly `git log --oneline docs/ .claude/`.

That claim only holds if the log covers every commit, so: **every `harness:` commit carries at least
one `harness-log.md` row**, and a commit closing N review findings carries either N rows or one row
naming all N. A `harness:` commit with no row is a defect in the run's record, not a shortcut.

Two kinds of friction, with different gates:

**Type 1 - contradiction.** The harness contradicts the framework, contradicts itself, or names
something that does not exist, and you can demonstrate it mechanically: a failing build, a missing
symbol, two skills prescribing different things. **Fix it immediately, mid-session, in its own
commit.** Do not ask. A human adds nothing to "this type does not exist".

**Type 2 - silence or ambiguity.** The harness gives no pattern for the case in front of you, or
gives one that cannot be followed without violating another stated principle. You **cannot** prove
the right answer, because there is not one yet - it is a design decision. **Halt and ask the human
before writing the code**, since the answer determines the code. Then write both the harness fix and
the implementation.

A finding whose only defect is a missing example, a cross-reference the reader must follow, or
imprecise wording is silence, not contradiction, however obvious the improvement looks - it is
Type 2 and it halts. This is what `/review-artifacts` Check 3 ("Improvement Opportunities" and
"Ambiguous Guidance") produces by construction; Checks 1, 2 and 4 produce Type 1 candidates.

**`simulator/` is always Type 2**, with no Type 1 fast path. Docs are guidance *about* the system;
`simulator/` *is* the system under study. A framework patch made to accommodate a wrong
implementation is the cheapest way to silently corrupt a result, and "the framework is broken" is in
practice almost always "my implementation is wrong". Log these as Type `2-fw`.

**Under delegated execution, the manager owns the gate.** When a session is driven by a manager
spawning subagents (`/implement-aggregate-full`), the definitions above are unchanged, but only one
agent acts on them. Subagents **report** friction and **halt** on Type 2 and `2-fw` before writing
any code; they never edit `docs/`, `.claude/` or `simulator/`, never append to `harness-log.md`, and
never commit. The manager makes every Type 1 fix and its `harness:` commit, escalates every Type 2 to
the human verbatim, and re-spawns the halted subagent with the answer. Single writer: several
subagents repairing the same ambiguity would produce competing `harness:` commits and racing appends
to an append-only log.

**Harness fixes are written in neutral vocabulary** - see
`.claude/skills/_shared/conventions.md` § "Neutral domain".

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
| `applications/{app-name}/` | A generated application; its spec pair, `plan.md`, `retros/`, `logs/` and `harness-log.md` live here | — |

Review reports are **not** per-application: `docs/reviews/` holds both kinds, `review-{date}.md`
written by `/review-artifacts` and `harness-retro-{app-name}-{date}.md` written by
`/harness-retrospective`.

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
