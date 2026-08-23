---
name: implement-aggregate-full
description: Phase 2 whole-aggregate implementation for microservices-simulator. Drives sessions 2.N.a through 2.N.d for one aggregate in a single run, delegating each slice to a fresh aggregate-slice subagent and owning the harness gate, the retros and the commits. Invoke with /implement-aggregate-full <N> or /implement-aggregate-full <Aggregate>.
argument-hint: "<N> or <Aggregate> (e.g. 3 or Warehouse)"
---

# Phase 2: Implement a Whole Aggregate

You are the **manager**. You drive sessions `2.{N}.a` through `2.{N}.d` for one aggregate, spawn one
`aggregate-slice` subagent per slice, and stop at the aggregate boundary.

You buy **context quality**, not wall-clock speed. Slices run **strictly sequentially**, one at a
time. That is what makes this design safe: the shared files a session appends to serialize
naturally, so there is no merge protocol and no write-conflict handling anywhere here. Never spawn
two slices at once.

## What you read, and what you must not read

You read **only**:

- this file;
- `AGENTS.md` § "Harness evolution";
- `.claude/skills/_shared/conventions.md`;
- `.claude/skills/_shared/session-completion.md`;
- the plan.md aggregate section for `{N}`;
- one specific harness file, lazily, when triaging a Type 1 report.

You do **not** read `.claude/skills/implement-aggregate/session-*.md`, and you do **not** read
`docs/concepts/*`. A manager that loads the session sub-files and the concept docs reconstitutes
exactly the context saturation this skill exists to prevent. The slices read them; that is their job.

You verify slices by `git status` and by the session-end full test suite - **not** by reading their
diffs.

## The single-session escape hatch

`/implement-aggregate` still works and is unchanged. Use it to resume by hand after a halt, or to run
one session in the single-agent topology.

---

## Step 0: Anchor to the repository root

Read `.claude/skills/_shared/conventions.md` and follow "Anchor to the repository root". Do not run
any command until you have.

## Step 1: Resolve context

Follow `.claude/skills/_shared/conventions.md` § "Resolve app context", then § "Resolve aggregate
context".

The argument is either the aggregate ordinal `{N}` or the aggregate name. If it is an ordinal, find
the `### {N}. {Aggregate}` section in plan.md and take `{Aggregate}` from it. If it is a name, the
conventions section resolves `{N}` from the header. If no argument was given, halt and ask which
aggregate to run - do not auto-detect. This skill commits four sessions in a row; starting on the
wrong aggregate is expensive.

## Step 2: Load the gate

Read `AGENTS.md` § "Harness evolution" in full. Do not proceed on a remembered version of it.

**The gate is yours exclusively.** Slices report friction; they never repair it. You:

- make every Type 1 fix yourself, and commit it alone with a `harness:` prefix;
- escalate every Type 2 and every `2-fw` to the human, verbatim, and wait;
- own every row appended to `applications/{app-name}/harness-log.md`;
- own every commit of the run.

The reason is single-writer: several slices independently repairing the same ambiguity would produce
competing `harness:` commits and racing appends to an append-only log whose next `#` is read from its
last row. It also keeps exactly one conversation with the human.

**Harness fixes are written in neutral vocabulary** - `_shared/conventions.md` § "Neutral domain".

## Step 3: Read the plan.md aggregate section

Read the entire `### {N}. {Aggregate}` section of `applications/{app-name}/plan.md` and hold it. It
carries the write and read functionalities, events published and subscribed, P1/P2/P3 rule notes,
cross-aggregate prerequisites, the per-session file tables, and the checklist with its slice
sub-checkboxes.

Read nothing else from plan.md beyond this section and the aggregate's own Implementation Order row.

## Step 4: Require a slice list

Inspect the checklist. If **none** of the aggregate's sessions carries slice sub-checkboxes and the
aggregate has more than three write functionalities or more than three subscribed events, the plan
predates slicing. Halt:

> plan.md has no slice list. Regenerate it with /classify-and-plan.

This is deliberate. Existing plan.md files are not migrated, and guessing a slice list at runtime
would silently produce a different split on every run, destroying the reproducibility that emitting
it at Phase 1 buys. See `.claude/skills/classify-and-plan/SKILL.md` § "Step 8.5".

A session legitimately carries no sub-checkboxes when its item count is at or below the threshold.
That session has exactly **one** implicit slice covering the whole session, named
`2.{N}.{type}1 {SessionName}`, and you run it exactly like any other slice.

---

## Step 5: Run the sessions

For each session type in order `a`, `b`, `c`, `d` - skipping `d` when the aggregate subscribes to no
events, and skipping any session whose checkbox is already ticked:

### 5.1 Run each slice, in order

For each unticked slice of the session, in the order plan.md lists them, one at a time:

**a. Spawn** one `aggregate-slice` subagent with the brief from § "Slice brief" below. Wait for it.

**b. On `STATUS: DONE`:**

1. Run `git status --porcelain`. Confirm every changed path is under `applications/{app-name}/`.
   If anything else was touched, that is a contract breach: revert those paths, record it as
   friction, and re-spawn the slice with the breach named in its brief.
2. Tick the slice's sub-checkbox in plan.md.
3. Store the slice's `RETRO FRAGMENT`, its `FILES ADDED BEYOND plan.md` lines and any `FRICTION`
   blocks. You will need all three at session end and you will not be able to recover them later.

**c. On `STATUS: HALTED`:**

- **Type 1** - triage it. Read the named harness file (this is the one lazy read you are allowed).
  If the report holds, repair the file in neutral vocabulary, commit it **alone** with a `harness:`
  prefix, and re-spawn the slice with the fix named in its brief. If the report does not hold, say
  so in the re-spawn brief and re-spawn without a fix.
- **Type 2 / `2-fw`** - surface the block to the human **verbatim**: the artifact, the problem, and
  the options as the slice stated them. Then **wait**. Do not answer it yourself, do not pick the
  option that looks obvious, and do not proceed with the rest of the session. When the human answers,
  re-spawn the slice with the answer appended to its brief.

**d. On `STATUS: FAILED`:**

- Re-spawn the slice **exactly once**, with the surefire output appended to its brief.
- A second failure **halts the whole aggregate**. Do not continue to the next slice, do not commit,
  and leave the checkbox unticked. Report how far you got (Step 6).

**A `FAILED` slice blocks the rest of its session.** Later slices append to the same shared files and
build on earlier slices' state, so a green result from a slice that ran on top of a broken one means
nothing. Stop the line.

A `DONE` slice that reported Type 1 friction without halting still gets its fix: repair the harness
file and commit it with a `harness:` prefix before spawning the next slice, so the next slice reads
the corrected version.

### 5.2 Close the session

After the last slice of the session returns `DONE`:

1. **Run the full suite.** Follow `_shared/conventions.md` § "Run the test suite" in its mandated
   `clean` form, for the whole application. Slices verify narrowly and without `clean`; cross-slice
   regressions are invisible to every one of them. The concrete case: session `c` rewires the
   `create{Aggregate}()` helper in `{AppClass}SpockTest.groovy`, and session `b`'s read tests run
   through it from then on.

2. **If it is red** - the failure belongs to no single slice. Spawn **one** `aggregate-slice`
   subagent as a repair slice, scoped to the failing test classes, with the surefire bodies in its
   brief. Same one-retry rule: if the repair slice fails twice, halt the aggregate.

3. **If it is green** - read `.claude/skills/_shared/session-completion.md` in full and follow it in
   **manager mode**: amend plan.md for omitted files, append the harness-log rows, assemble the
   merged retro from the stored fragments, tick the session checkbox, and issue the single session
   commit.

Then move to the next session type.

---

## Step 6: Stop at the aggregate boundary

After committing `2.{N}.d` (or `2.{N}.c` when the aggregate has no subscribed events), **stop**. Do
not continue to aggregate `{N+1}`.

Report to the human:

- sessions committed, with their commit shas;
- slices run per session, and how many needed a re-spawn;
- harness fixes made, with their `harness:` commit shas;
- Type 2 questions raised and how they were answered, plus any left open;
- the instruction: **run `/review-artifacts` in a fresh session before starting aggregate {N+1}**.

That checkpoint stays human-invoked and unchanged (`docs/workflow.md` § "Aggregate-boundary
checkpoint"). The harness is self-healing, so the artifacts changed while this run was reading them,
and the boundary is the last moment a contradiction introduced mid-aggregate is cheap to find.

If the run halted early, report the same list plus the exact point it stopped and what unblocks it.

---

## Slice brief

Fill this template for every spawn. Substitute every placeholder; a slice that receives an unfilled
placeholder cannot do its job.

```
You are implementing ONE slice of session 2.{N}.{type} for {Aggregate}
in application {app-name}.

Read and follow: .claude/skills/implement-aggregate/session-{type}.md

YOUR SLICE: 2.{N}.{type}{k} - {ItemName}
Implement only this item. The other items in this session are owned by
other agents; do not create, modify or test them.

FILES YOU CREATE:
  <the per-item files for this item, from the plan.md 2.{N}.{type} row>

SHARED FILES YOU APPEND TO (never rewrite, never reformat):
  <the session's shared files, from the table below>

PLAN CONTEXT FOR THIS ITEM:
  <the functionality or event line from plan.md, plus any P3/P4a/P4b
   prerequisite that names it>

VERIFY:
  cd applications/{app-name}
  mvn -Ptest-sagas test -Dtest=<this slice's T4 test>,{Aggregate}ServiceTest
  echo "MAVEN_EXIT=$?"
  then the surefire aggregation script from _shared/conventions.md

Return the block defined in .claude/agents/aggregate-slice.md.
```

For a whole-session (unsliced) run, `YOUR SLICE` names the session itself and the "only this item"
sentence is dropped; everything else is unchanged.

On a re-spawn, append one clearly-labelled section to the brief and change nothing else:
`PREVIOUS ATTEMPT FAILED:` with the surefire output, or `HUMAN DECISION:` with the answer to the
Type 2 question, or `HARNESS FIXED:` naming the file and what changed.

### Shared files per session type

These accumulate one member per item, which is why slices append and never rewrite:

| Session | Shared files |
|---------|--------------|
| `b` | `{Aggregate}Service.java`, `{Aggregate}CommandHandler.java`, `{Aggregate}Functionalities.java`, `{Aggregate}ServiceTest.groovy`, `ServiceMapping.java`, `{AppClass}SpockTest.groovy` (the `@Autowired` functionalities field and the `create{Aggregate}(...)` fixture helper) |
| `c` | `{Aggregate}Service.java`, `{Aggregate}CommandHandler.java`, `{Aggregate}Functionalities.java`, `{Aggregate}ServiceTest.groovy`, `{AppClass}ErrorMessage.java` (P3 guard constants) |
| `d` | `{Aggregate}EventHandling.java` (one `@Scheduled` method per event), `{Aggregate}EventHandler.java` (one `instanceof` branch per event), `{Aggregate}EventProcessing.java` (one `process{Xxx}Event` method per event), `{Aggregate}Functionalities.java` (one `{operation}ByEvent` method per event), `{Aggregate}Service.java` (the ByEvent helper, only when no existing mutate method performs exactly this mutation), `{Aggregate}.java` (one `getEventSubscriptions()` subscription per subscribed event, inside the ACTIVE guard), `{AppClass}ErrorMessage.java` (constants for invariants event processing can violate), `{Aggregate}InterInvariantTest.groovy` |

Session `a` produces one aggregate and is never sliced, so it has no shared-file hazard.

Session `c` additionally has one file that is **replaced, not appended to**: the
`create{Aggregate}()` helper body in `{AppClass}SpockTest.groovy`. It belongs to slice `c1`, the
create functionality, which plan.md always orders first. No later slice may touch it.
