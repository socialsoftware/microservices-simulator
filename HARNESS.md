# HARNESS.md - Generating an Application with the Agent Harness

This repository contains two things. `simulator/` is the **framework**: a DDD microservices
simulator that runs the same domain code under different consistency models, documented in
[`README.md`](README.md). Everything described here is the **harness**: an instruction set that
drives an AI coding agent to generate a complete simulator application from a written specification,
aggregate by aggregate.

This file is the user-facing guide to that harness. `AGENTS.md` is the agent-facing entry point; you
do not need to read it to use the harness, and you will need it if you want to change one.

---

## 1. What the harness is

The harness is what teaches an agent to turn a **spec pair** into a working application. It is
neither the application it generates nor the library that application runs on. Three buckets:

| Bucket | Files | What it is |
|--------|-------|------------|
| **Harness** | `AGENTS.md`, `CLAUDE.md`, `HARNESS.md`, `docs/`, `.claude/skills/`, `.claude/agents/` | The instruction set. Prose an agent reads at runtime, plus the skills that sequence it. No code. |
| **Framework** | `simulator/` | The core library every generated application compiles against. The system under study. |
| **Run record** | `applications/{app-name}/` and `reviews/` | What one run leaves behind: its spec pair, plan, generated source, retros, harness log, and any review reports. |

The membership test: delete `applications/`. Whatever must remain to generate a new application from
a spec pair is the harness, plus the framework it compiles against. `AGENTS.md`
§ "What the harness is" is the authority on this split; it is repeated here because everything below
depends on it.

**Your spec pair is not part of the harness.** It is the input to a run.

## 2. Requirements

- **Claude Code.** The workflow is packaged as Claude Code skills and subagents (see § 3).
- **JDK 21.** The application poms set `<java.version>21</java.version>`; an older JDK fails at the
  first compile with `error: release version 21 not supported`, which reads like a scaffold bug and
  is not. `sdk use java 21.0.10-tem`, or set `JAVA_HOME` per command.
- **Maven**, and a `~/.m2/settings.xml` that resolves the project's dependencies. `/boot-strap`
  copies a `.mvn/maven.config` into each new application to point at it.
- Run `cd simulator && mvn install` once before generating anything: every application compiles
  against the installed core library.

## 3. Portability

**The content is agent-agnostic.** Everything under `docs/` is prose: architecture, concept docs,
rule-enforcement patterns, the test taxonomy, the spec templates. Any capable coding agent can be
pointed at it.

**The workflow is Claude Code only.** `.claude/skills/` and `.claude/agents/` use Claude Code's
skill format, its slash-command invocation and its subagent spawning. `/implement-aggregate-full` in
particular depends on subagents with their own context windows; there is no portable equivalent.

**TODO - `.agents/` plus symlinks.** The agreed portability shape is a vendor-neutral `.agents/`
directory with per-vendor symlinks into it, so the same instruction set serves more than one agent
runtime. Not done. Until it is, treat "agent-agnostic" as a claim about the prose only.

## 4. The pipeline

Six stages. Each produces a stable artifact the next one starts from, so an agent is told exactly
what to read and what to produce rather than being handed the whole problem.

| Stage | Command | Produces |
|-------|---------|----------|
| Spec authoring | `/author-spec <pointer>` | The spec pair (§ 5) |
| Phase 0 - Bootstrap | `/boot-strap <app-name> [--self-healing]` | Maven scaffold, exception classes, Spock base classes, `harness-log.md` with the run's mode |
| Phase 1 - Classify & Plan | `/classify-and-plan <domain-model> <aggregate-grouping>` | `plan.md`: rule classification, aggregate implementation order, per-session file lists and slice lists |
| Phase 2 - Implementation | `/implement-aggregate` or `/implement-aggregate-full` (§ 6) | The application, one session per commit, with a retro per session |
| Aggregate boundary | `/review-artifacts` | A static consistency report over the harness, in `reviews/` |
| End of run | `/harness-retrospective` | An empirical evaluation of the harness against the run's evidence |

Phase 2 runs four session types per aggregate, in order: `a` domain layer, `b` read
functionalities, `c` write functionalities, `d` event wiring (only when the aggregate subscribes to
events). Aggregates are implemented in topological order of their event dependencies, so an
aggregate is never built before something it caches from.

**Run `/review-artifacts` at every aggregate boundary.** It is human-invoked in both topologies:
`/implement-aggregate-full` stops at the boundary and tells you to run it rather than running it
itself. Act on its Critical and Major findings before starting the next aggregate. The full
description of each phase is in [`docs/workflow.md`](docs/workflow.md).

## 5. Writing your spec pair

A run starts from two files under `applications/{app-name}/`:

| File | Owns |
|------|------|
| `{app-name}-domain-model.md` | Entities, relationships, rules (§3.1 single-entity, §3.2 cross-entity), functionalities (§4) |
| `{app-name}-aggregate-grouping.md` | Aggregate partitioning (§1), snapshots (§2), the event DAG (§3), events (§4) |

Every later phase treats these as given. `/classify-and-plan` will not question an aggregate
boundary, and no Phase 2 session will add a functionality §4 omitted. It is the highest-leverage
artifact in the whole pipeline.

Three things to write it with:

1. **The templates.** [`docs/templates/domain-model-template.md`](docs/templates/domain-model-template.md)
   and [`docs/templates/aggregate-grouping-template.md`](docs/templates/aggregate-grouping-template.md)
   define the section numbers, table columns and block shapes the harness parses. Keep them intact.
2. **`/author-spec <pointer to the application you are modelling>`.** An interview that walks the
   design tree in dependency order, one question at a time, with a recommended answer for each. It
   challenges premises rather than transcribing answers, and it writes the pair only once you say the
   interview is finished. It is a domain-specific instance of the `grill-me` / `grilling` skills and
   borrows its stance from them.
3. **A worked example.** `applications/trainticket/` holds a finished spec pair plus a
   `trainticket-design-rationale.md` recording why its boundaries fell where they did. Read it for
   shape and depth, not for content.

**Known gap:** there is no full spec-authoring guide. The templates say what shape to produce and
`/author-spec` asks the questions, but the judgement behind a good aggregate boundary is not written
down anywhere. Contributions welcome.

## 6. The two entry points

Phase 2 can be driven two ways. They read the same session sub-files, close a session through the
same completion procedure, and produce the same retros and the same one-commit-per-session history.

| Axis | `/implement-aggregate` | `/implement-aggregate-full` |
|------|------------------------|------------------------------|
| **Scheduled checkpoints** | One per session, four per aggregate | One per aggregate |
| **Unscheduled halts** | Type 2 and `2-fw` halt and wait for you | **Identical** |
| **Token cost** | Lower: one agent, one context per session | Higher: every slice reads its docs from cold, and the manager holds its own context |
| **Context quality** | Degrades across a long session | Higher per unit of work: each slice starts fresh and narrow |
| **Failure blast radius** | One session | Up to a whole aggregate |

**`/implement-aggregate` is the recommended default.** It gives you the most chances to notice that
something has gone wrong, at the lowest cost.

**`/implement-aggregate-full` is not unattended.** It reduces how often you are asked to *start*
something, not how often you are asked to *decide* something: the halts are the same halts, on the
same friction, and they wait just as long. Reach for it when you would rather be interrupted once per
aggregate than four times.

## 7. Self-healing mode

The harness can repair itself mid-run. When a doc or a skill misleads an agent, the agent classifies
the friction:

- **Type 1 - contradiction.** Demonstrably wrong: a failing build, a symbol that does not exist, two
  skills prescribing opposite things.
- **Type 2 - silence or ambiguity.** No pattern for the case at hand, and no way to prove what the
  right answer is, because it is a design decision. **Always halts and asks you**, in every mode.
- **Type `2-fw`.** Anything under `simulator/`. Always Type 2, never repaired unilaterally: patching
  the framework to accommodate a wrong implementation is the cheapest way to silently corrupt a
  result.

**Self-healing mode is off by default.** ON lets an agent fix a Type 1 itself, mid-session, in its
own `harness:` commit. OFF withholds exactly that: the agent proceeds on the most reasonable reading,
records the contradiction as a `deferred` row in `harness-log.md`, and surfaces it in the session
report for you to act on between runs. Everything else is identical in both modes - the
classification, the Type 2 halts, the harness-log rows, the retros, the boundary review. OFF is
**enforced**, not merely instructed: the session commit halts if any harness file is dirty.

**Why off is the default.** Under ON, the artifacts change while they are being read, so a run is
measured against a moving target and the agent can ratify its own guess by editing the doc it
disagreed with. Under OFF, the harness a run was given is provably the harness it was judged on. A
run under OFF still produces the complete record of where the harness failed; it just does not act
on it.

**Turn it on** with `/boot-strap <app-name> --self-healing`, which is the only place the mode is
declared. It is written into the `harness-log.md` header and holds for the whole run; a missing or
unreadable header reads as off. A single Phase 2 invocation can override it with `--self-healing` /
`--no-self-healing`, which binds that invocation only and is reported rather than written to the
header.

## 8. What a run leaves behind

```
applications/{app-name}/
├── {app-name}-domain-model.md          the spec pair you wrote
├── {app-name}-aggregate-grouping.md
├── plan.md                             the job queue, with every session ticked
├── harness-log.md                      every friction point: type, artifact, outcome
├── retros/retro-{session}-{Agg}.md     one per Phase 2 session
└── src/                                the generated application
reviews/
├── review-{date}.md                    one per aggregate boundary
└── harness-retro-{app-name}-{date}.md  one per completed run
```

The generated application is the deliverable. The rest is the evidence: `harness-log.md` says where
the harness failed and what was done about it, the retros say what each session found, and
`/harness-retrospective` reads all of it at the end to say whether the harness converged and what it
still lacks. Under self-healing OFF the retrospective emits the gap list only, since a run that made
no edits has no convergence to measure.

None of it is rewritten after the fact. `harness-log.md` is append-only, and a past retro is never
edited to read better.

## 9. Changing the harness yourself

Between runs, edit `docs/` and `.claude/` directly; that is what they are for. Run
`/review-artifacts` afterwards to catch dangling paths and contradictions before an agent hits them.

Two rules worth knowing before you write anything into a harness file:

1. **Neutral vocabulary.** A harness file may not name an entity, aggregate or operation of the
   application currently being generated. Use the placeholder vocabulary (`{Aggregate}`, `{Entity}`,
   `{Operation}`, `{Event}`), or invent an unrelated neutral domain for a worked example. A vivid
   example drawn from the application in front of you is a leaked answer for the next one. Naming a
   **completed** application as a human-readable example is fine.
2. **`simulator/` is not the harness.** It is the system under study, and "the framework is broken"
   is in practice almost always "my implementation is wrong". Framework changes are always a human
   decision.

`AGENTS.md` § "Harness evolution" is the full statement of the gates, and
`.claude/skills/_shared/conventions.md` is where the shared skill conventions live.
