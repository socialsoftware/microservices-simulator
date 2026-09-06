---
name: implement-aggregate
description: Phase 2 aggregate implementation for microservices-simulator. Auto-detects the next unchecked session in plan.md and delegates to the appropriate sub-file (session-a.md through session-d.md). Invoke with /implement-aggregate [session] (e.g., /implement-aggregate or /implement-aggregate 2.3.b).
argument-hint: "[session] [--self-healing|--no-self-healing] (e.g. 2.3.b - session optional, auto-detects if omitted)"
---

# Phase 2: Implement Aggregate

This skill drives one Phase 2 session at a time, advancing the plan.md job queue by one checkbox per invocation. It reads plan.md, identifies the next unchecked session, loads only the instructions relevant to that session type, and produces the files listed in plan.md for that session.

Required scenarios, assertion ownership, and templates for every tier are defined in
`docs/concepts/testing.md` — do not restate them here. This table maps each session to the test
file(s) it produces (session → file, for file-generation purposes only):

| Session | Test type | File |
|---------|-----------|------|
| `a` | T1 Aggregate | `{Aggregate}IntraInvariantTest.groovy` |
| `b` | T2 Service (read methods) | `{Aggregate}ServiceTest.groovy` |
| `b` | T4 Functionality (reads) | `{Query}Test.groovy` |
| `c` | T2 Service (write methods + event publication) | appended to `{Aggregate}ServiceTest.groovy` |
| `c` | T4 Functionality (writes) | `{Op}Test.groovy` |
| `c` | T4 Compensation | `{Op}CompensationTest.groovy` |
| `d` | T3 Subscription (Inter-Invariant) | `{Aggregate}InterInvariantTest.groovy` |

> **Recorded decision — why T2 lives in sessions `b`/`c`, not `a`:** the migration plan's default
> assigns T1+T2 to session `a`, but session `a` produces only the domain layer; the `*Service`
> class that T2 tests invoke does not exist until session `b`. Per the plan's allowance, T2
> authoring is therefore assigned to session `b` for the read methods, with the write-method and
> event-publication cases appended in session `c`.

---

## Application isolation

Read `.claude/skills/_shared/conventions.md` § "Application isolation" in full before implementing.
Do not continue until you have. It governs which files this session may read.

**If the docs or skill don't cover something:** flag it explicitly in the completion report and append a
harness-log row (`_shared/session-completion.md` § "Harness-log rows"). Do not silently fill the gap from another application. Surfacing the gap is
the correct behavior — it becomes a harness improvement, not a hidden copy of a peer's pattern.

---

## Input

Invoked as:
```
/implement-aggregate
/implement-aggregate 2.3.b
```

- **No argument** — auto-detect the next unchecked session across all `applications/*/plan.md` files.
- **With session arg** (e.g., `2.3.b`) — target that specific session; skip the auto-detect scan.
- **`--self-healing` / `--no-self-healing`** - override the run's declared self-healing mode for this
  invocation only (Step 3b). The `harness-log.md` header is never rewritten.

> If multiple applications have unchecked sessions and no argument was given, ask: "Multiple apps have unchecked sessions: {list}. Which should I work on?"

---

## Step 0: Anchor to the repository root

Before Step 1, read `.claude/skills/_shared/conventions.md` and follow "Anchor to the repository
root". Do not run any command until you have.

## Step 1: Locate plan.md and Identify the Target Session

### 1.a — Find plan.md

Run: `find applications -name plan.md`

Collect all results. If multiple found, check which have unchecked checkboxes (`- [ ]`).

- **Exactly one** with unchecked boxes → use it.
- **Multiple** → ask user (see prompt above).
- **None** → halt: "No plan.md found. Run /classify-and-plan first."

### 1.b — Determine target session

**If a session argument was provided (e.g., `2.3.b`):**

Parse it: `{N}` = `3`, `{type}` = `b`. Verify the checkbox `- [ ] 2.3.b` exists in plan.md. If already ticked, warn: "Session 2.3.b is already marked complete. Did you mean a different session?" and ask for confirmation before proceeding.

**If no argument (auto-detect):**

Scan plan.md for the first line matching `- \[ \] 2\.\d+\.[abcd](?![0-9])`. Extract:

The negative lookahead matters: a session checkbox may be followed by indented **slice**
sub-checkboxes of the form `- [ ] 2.{N}.{type}{k} {ItemName}`
(`.claude/skills/classify-and-plan/SKILL.md` § "Step 8.5"). Those are not sessions and this skill
does not target them - it implements the whole session regardless of how plan.md sliced it, and ticks
the session checkbox plus every one of its sub-checkboxes at the end.

- `{N}` = the aggregate number (the `\d+`)
- `{type}` = the session type (`a`, `b`, `c`, or `d`)

If no unchecked Phase 2 session found, inform: "All Phase 2 sessions are complete for {app-name}. Nothing left for /implement-aggregate."

---

## Step 2: Derive Context Variables

Read `.claude/skills/_shared/conventions.md` § "Resolve app context" and derive `{app-name}`,
`{pkg}`, `{AppClass}` from the plan.md path located in Step 1 (already found — do not re-run the
`find`).

From plan.md, find the aggregate details section for aggregate number `{N}`:
- Section header pattern: `### {N}. {Aggregate}` or `### {N}. {AggregateName}`
- `{Aggregate}` = the aggregate name in PascalCase (e.g., `Warehouse`)
- `{aggregate}` = lowercase version (e.g., `warehouse`)

Derive standard path prefixes:
- `{src}` = `applications/{app-name}/src/main/java/pt/ulisboa/tecnico/socialsoftware/{pkg}/`
- `{test}` = `applications/{app-name}/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/{pkg}/`
- `{bean-config}` = `{test}BeanConfigurationSagas.groovy`

---

## Step 3: Read the Aggregate Section from plan.md

Read and hold in context the entire aggregate section for `{N}` from plan.md. This section contains:
- Write functionalities list
- Read functionalities list (may be "none")
- Events published list
- Events subscribed list
- P1/P2/P3 rules notes
- Cross-aggregate prerequisites
- Files to produce table (sessions 2.N.a through 2.N.d)
- Checklist

plan.md is the **starting point** for what to produce, not an authoritative manifest. What is
actually required for this session is determined by the session sub-file loaded in Step 4 and the
concept docs it names. Producing a file that plan.md omitted is correct and expected - amend the row
per `_shared/session-completion.md` § "Amend plan.md for omitted files" rather than skipping the
file.

---

## Step 3b: The Self-Healing Gate

Implementation starts at Step 4. Before it does, load the gate, because this is the skill where it
actually fires: read `AGENTS.md` § "Harness evolution" in full. It defines Type 1, Type 2 and
`2-fw`; do not proceed on a remembered version of it.

**Read the mode before anything else.** Open the `**Self-healing:**` line in the header of
`applications/{app-name}/harness-log.md`. `on` or `off`; missing, unreadable or anything else means
**off** (`.claude/skills/_shared/conventions.md` § "Harness log"). A `--self-healing` /
`--no-self-healing` flag on this invocation overrides it for this session only, and is stated in the
completion report and the retro rather than written to the header. State the mode in force, and
whether it came from the header or a flag, in the completion report.

Operationally, for every friction point hit between here and the end of the session:

1. **Classify it first.** Can you demonstrate the harness wrong mechanically - a failing build, a
   symbol that does not exist, two harness files prescribing different things? That is Type 1.
   Otherwise it is Type 2. **Classification does not depend on the mode**; only what you do next
   does.
2. **Type 1, mode ON - fix it on the spot.** Repair the `docs/` or `.claude/skills/` file, commit it
   alone with a `harness:` prefix, and carry on. Do not ask, do not batch it to the end of the
   session, and do not work around it in the application code. The fix is written in the neutral
   vocabulary of `.claude/skills/_shared/conventions.md` § "Neutral domain".
3. **Type 1, mode OFF - do not edit the harness, and do not halt.** Proceed on the most reasonable
   reading, log the row with `Outcome` = `deferred` and `Ref` = `-`, and name it in the completion
   report so the human can act on it between runs. Say in the report which reading you proceeded on;
   that sentence is what makes the deferred row actionable later.
4. **Type 2 - halt before writing any code, in both modes.** The answer is a design decision and it
   determines the code, so writing first and asking later produces code that has to be thrown away.
   State what the harness says, what it does not settle, and the options; then wait.
5. **Anything under `simulator/` is Type `2-fw` and always halts**, in both modes, even when it looks
   mechanically provable. There is no Type 1 fast path for the framework.

Every one of these gets a row in `harness-log.md` at `_shared/session-completion.md`
§ "Harness-log rows", whatever the mode and whatever its outcome.

---

## Step 4: Delegate to the Appropriate Sub-File

Based on `{type}`, load and follow the corresponding sub-file. All context variables derived above are available to the sub-file.

| type | Sub-file | Session name |
|------|----------|--------------|
| `a`  | `session-a.md` | Domain Layer |
| `b`  | `session-b.md` | Read Functionalities |
| `c`  | `session-c.md` | Write Functionalities |
| `d`  | `session-d.md` | Event Wiring |

Read the sub-file now and follow its instructions exactly. The sub-file specifies:
1. Which additional files to read before coding
2. What files to produce and how
3. How to update `BeanConfigurationSagas.groovy`
4. Which checkbox to tick

---

## Steps 5-8: Complete the Session

Read `.claude/skills/_shared/session-completion.md` in full and follow it, in **single-agent mode**.
Do not continue from memory of a previous read. It owns, in this order:

| Step | Section in `session-completion.md` |
|------|------------------------------------|
| 5  | § "Tick the checkbox" |
| 5b | § "Amend plan.md for omitted files" |
| 6  | § "Report completion" |
| 6b | § "Harness-log rows" |
| 7  | § "Retro assembly" - use § "Single-agent mode", then § "Retro template" |
| 8  | § "Commit" |

The retro template, the Semantic-Lock Coverage Audit commit-blocking rule and the commit message
format each live there and only there. `.claude/skills/implement-aggregate-full/SKILL.md` reads the
same file in manager mode, which is what keeps the two entry points producing identical artifacts.
