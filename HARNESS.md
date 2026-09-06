# HARNESS.md - Generating an Application with the Agent Harness

This repository contains two things. `simulator/` is the **framework**: a DDD microservices
simulator that runs the same domain code under different consistency models, documented in
[`README.md`](README.md). Everything described here is the **harness**: an instruction set that
drives an AI coding agent to generate a complete simulator application from a written specification,
aggregate by aggregate.

This file is the user-facing guide to that harness. `AGENTS.md` is the agent-facing entry point; you
do not need to read it to use the harness, and you will need it if you want to change the harness
itself.

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

- **An agent runtime with skill dispatch.** The workflow is packaged as skills invoked by name; it is
  developed against Claude Code. Only `/implement-aggregate-full` needs more than that, namely
  subagent spawning (see § 3).
- **git.** Every Phase 2 session ends in one commit, and self-healing OFF is enforced by reading
  `git status --porcelain` (§ 8). The harness assumes it is running inside a git worktree.
- **Python 3 on `PATH`.** Any check whose output decides a verdict runs through `python3` rather
  than a shell pipeline, so that a mangled or truncated stream cannot be mistaken for a pass.
- **JDK 21.** The application poms set `<java.version>21</java.version>`; an older JDK fails at the
  first compile with `error: release version 21 not supported`, which reads like a scaffold bug and
  is not. `sdk use java 21.0.10-tem`, or set `JAVA_HOME` per command.
- **Maven**, and a `~/.m2/settings.xml` that resolves the project's dependencies. `/boot-strap`
  copies a `.mvn/maven.config` into each new application to point at it.
- Run `cd simulator && mvn install` once before generating anything: every application compiles
  against the installed core library.

## 3. Portability

**The instruction set is vendor-neutral; two things around it are not.** Everything under `docs/` is
prose, and `.claude/skills/` uses the open Agent Skills format: a `SKILL.md` carrying `name` and
`description` frontmatter, with no vendor-specific syntax in the body. The exceptions are
`.claude/agents/`, whose two files use Claude Code's subagent frontmatter and would need
re-expressing per runtime, and `/implement-aggregate-full`, which needs a runtime that can spawn a
subagent per slice and read a structured return block from it. A runtime without that primitive still
runs the whole pipeline through `/implement-aggregate`, which is the recommended default anyway.

Note that `docs/` and `.claude/` are not separable: the concept docs link into the skill tree by
path, and the skills read `docs/` back. They port as one unit.

**Serving a second runtime is a symlink, in one direction only.** `.claude/` stays the real
directory, and a per-vendor name symlinks to it:

```bash
ln -s .claude .agents
```

The reverse - a real `.agents/` with `.claude` symlinked into it - looks tidier and breaks the
harness, because git tracks only the real path. Staging a harness file by its `.claude/` path, which
is what every `harness:` commit does, fails outright with `pathspec is beyond a symbolic link`. The
OFF-mode
dirty check in `session-completion.md` tests for a `.claude/` prefix that `git status` would no
longer emit, so it reports a clean harness however dirty the harness is. And the harness delta
`git log --oneline docs/ .claude/ ...` matches only the symlink blob, so it comes back empty. Two of
those three fail silently.

**Untested, because there is no second runtime yet.** A clone with `core.symlinks=false` (Windows
without developer mode) materializes the link as a plain text file and the second runtime finds
nothing, and a skill scanner that does not follow directory symlinks will not see the tree either.

## 4. The pipeline

Five stages. Each produces a stable artifact the next one starts from, so an agent is told exactly
what to read and what to produce rather than being handed the whole problem.

| Stage | Command | Produces |
|-------|---------|----------|
| Spec authoring | `/author-spec <pointer>` | The spec pair (§ 5) |
| Phase 0 - Bootstrap | `/boot-strap <app-name> [--self-healing]` | Maven scaffold, exception classes, Spock base classes, `harness-log.md` with the run's mode |
| Phase 1 - Classify & Plan | `/classify-and-plan <domain-model> <aggregate-grouping>` | `plan.md`: rule classification, aggregate implementation order, per-session file lists and slice lists |
| Phase 2 - Implementation | `/implement-aggregate` or `/implement-aggregate-full` (§ 6) | The application, one session per commit, with a retro per session |
| End of run | `/harness-retrospective` | An empirical evaluation of the harness against the run's evidence |

Phase 2 runs four session types per aggregate, in order: `a` domain layer, `b` read
functionalities, `c` write functionalities, `d` event wiring (only when the aggregate subscribes to
events). Aggregates are implemented in topological order of their event dependencies, so an
aggregate is never built before something it caches from.

A sixth command, `/review-artifacts`, sits outside the pipeline: it is a maintenance pass over the
harness rather than a step that produces the application (§ 10). It is human-invoked and never runs
itself - `/implement-aggregate-full` stops at the aggregate boundary and tells you it is available
rather than running it.

**Under self-healing ON, running it after each finished aggregate is recommended**, because sessions
are editing `docs/` and `.claude/` while later sessions read them. Act on its Critical and Major
findings in their own `harness:` commits before starting the next aggregate. **Under OFF it is
optional**: the harness cannot drift during the run, the harness bucket must stay clean for the whole
run and the next session's commit halts if it is not (§ 8), so anything it finds is carried to the
end of the run and acted on between runs. The full description of each phase is in
[`docs/workflow.md`](docs/workflow.md).

## 5. Writing your spec pair

A run starts from two files under `applications/{app-name}/`:

| File | Owns |
|------|------|
| `{app-name}-domain-model.md` | Entities, relationships, rules (§3.1 single-entity, §3.2 cross-entity), functionalities (§4) |
| `{app-name}-aggregate-grouping.md` | Aggregate partitioning (§1), snapshots (§2), the event DAG (§3), events (§4) |

Every later phase treats the pair as given. `/classify-and-plan` will not question an aggregate
boundary, and no Phase 2 session will add a functionality §4 omitted. The spec pair is the
highest-leverage artifact in the whole pipeline.

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

## 7. When a session fails

A Phase 2 session is atomic in the record. Its `plan.md` checkbox is ticked and its commit made
together, in the completion procedure, after the tests pass. A session that halts or fails partway
therefore leaves no commit, an unticked checkbox, and possibly half-written files in the worktree.

**Resume by re-invoking `/implement-aggregate`.** With no argument it auto-detects the next unticked
session, which is the one that failed; name it explicitly (`/implement-aggregate 2.3.b`) to be sure.
It refuses to silently redo a session whose box is already ticked. This works after a halt in either
topology, so it is also how you resume by hand from a `/implement-aggregate-full` run.

**Clear the partial work first.** `git status` shows what the failed attempt left behind. A re-run
produces the files `plan.md` lists for that session, so leftovers it reaches are overwritten, but
anything it did not reach is not: revert tracked files and delete untracked ones before resuming.

**`/implement-aggregate-full` resumes at slice granularity.** Slice sub-checkboxes are ticked as each
slice returns, so re-invoking it for the same aggregate skips every ticked session and every ticked
slice, and restarts at the first unticked one.

**Verify an aggregate yourself** at any point, from the application directory:

```bash
cd applications/{app-name}
mvn clean -Ptest-sagas test
```

## 8. Self-healing mode

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

## 9. What a run leaves behind

```
applications/{app-name}/
├── {app-name}-domain-model.md          the spec pair you wrote
├── {app-name}-aggregate-grouping.md
├── plan.md                             the job queue, with every session ticked
├── harness-log.md                      every friction point: type, artifact, outcome
├── retros/retro-{session}-{Agg}.md     one per Phase 2 session
├── pom.xml                             written by /boot-strap
├── .mvn/maven.config                   untracked; points Maven at your settings.xml
└── src/                                the generated application
reviews/
├── review-{date}.md                    one per /review-artifacts run, if any
└── harness-retro-{app-name}-{date}.md  one per completed run
```

The generated application is the deliverable. The rest is the evidence: `harness-log.md` says where
the harness failed and what was done about it, the retros say what each session found, and
`/harness-retrospective` reads all of it at the end to say whether the harness converged and what it
still lacks. Under self-healing OFF the retrospective emits the gap list only, since a run that made
no edits has no convergence to measure.

None of it is rewritten after the fact. `harness-log.md` is append-only, and a past retro is never
edited to read better.

## 10. Changing the harness yourself

Between runs, edit `docs/` and `.claude/` directly; that is what they are for.

**`/review-artifacts` is the garbage collector for those edits, and this is where it earns its
keep.** It is a static pass over `docs/`, `.claude/`, `AGENTS.md` and `HARNESS.md` that catches what
hand-editing leaves behind: paths that no longer resolve, two files prescribing different things, a
piece of knowledge that lost its single owner, a domain noun leaked in from the last application.
Run it after any substantial change to the harness - a refactor, a batch of doc rewrites, a new or
retired skill - before the next run reads the result.

**It is expensive.** It reads every harness file in full, so it fills a context window fast. Run it
in a fresh session, never inline in a Phase 2 session or alongside other work you still want context
for. That cost is why it is a recommendation rather than a step of the pipeline: run it when the
harness has actually changed, not on a schedule.

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
