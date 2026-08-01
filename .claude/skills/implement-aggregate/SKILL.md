---
name: implement-aggregate
description: Phase 2 aggregate implementation for microservices-simulator. Auto-detects the next unchecked session in plan.md and delegates to the appropriate sub-file (session-a.md through session-d.md). Invoke with /implement-aggregate [session] (e.g., /implement-aggregate or /implement-aggregate 2.3.b).
argument-hint: "[session] (e.g. 2.3.b — optional, auto-detects if omitted)"
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

**If the docs or skill don't cover something:** flag it explicitly in the Step 6 report and append a
harness-log row (Step 7.d). Do not silently fill the gap from another application. Surfacing the gap is
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

Scan plan.md for the first line matching `- [ ] 2.\d+\.[abcd]`. Extract:
- `{N}` = the aggregate number (the `\d+`)
- `{type}` = the session type (`a`, `b`, `c`, or `d`)

If no unchecked Phase 2 session found, inform: "All Phase 2 sessions are complete for {app-name}. Nothing left for /implement-aggregate."

---

## Step 2: Derive Context Variables

Read `.claude/skills/_shared/conventions.md` § "Resolve app context" and derive `{app-name}`,
`{pkg}`, `{AppClass}` from the plan.md path located in Step 1 (already found — do not re-run the
`find`). Additionally, locally:
- `{appClass}` = same as `{AppClass}` but first segment lowercase (e.g., `myApp`)

From plan.md, find the aggregate details section for aggregate number `{N}`:
- Section header pattern: `### {N}. {Aggregate}` or `### {N}. {AggregateName}`
- `{Aggregate}` = the aggregate name in PascalCase (e.g., `Tournament`)
- `{aggregate}` = lowercase version (e.g., `tournament`)

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
per Step 5b rather than skipping the file.

---

## Step 3b: The Self-Healing Gate

Implementation starts at Step 4. Before it does, load the gate, because this is the skill where it
actually fires: read `AGENTS.md` § "Harness evolution" in full. It defines Type 1, Type 2 and
`2-fw`; do not proceed on a remembered version of it.

Operationally, for every friction point hit between here and the end of the session:

1. **Classify it first.** Can you demonstrate the harness wrong mechanically - a failing build, a
   symbol that does not exist, two harness files prescribing different things? That is Type 1.
   Otherwise it is Type 2.
2. **Type 1 - fix it on the spot.** Repair the `docs/` or `.claude/skills/` file, commit it alone
   with a `harness:` prefix, and carry on. Do not ask, do not batch it to the end of the session,
   and do not work around it in the application code. The fix is written in the neutral vocabulary
   of `.claude/skills/_shared/conventions.md` § "Neutral domain".
3. **Type 2 - halt before writing any code.** The answer is a design decision and it determines the
   code, so writing first and asking later produces code that has to be thrown away. State what the
   harness says, what it does not settle, and the options; then wait.
4. **Anything under `simulator/` is Type `2-fw` and always halts**, even when it looks mechanically
   provable. There is no Type 1 fast path for the framework.

Every one of these gets a row in `harness-log.md` at Step 7.d, whatever its outcome.

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

## Step 5: Tick the Checkbox

After all files are produced and BeanConfigurationSagas.groovy is updated, update plan.md:

Replace `- [ ] 2.{N}.{type}` with `- [x] 2.{N}.{type}` in plan.md.

---

## Step 5b: Amend plan.md for Omitted Files

plan.md is a fallible blueprint. A file absent from its file table may still be required by the
session sub-file or the concept docs; producing it is correct, and the omission is a defect in
plan.md rather than in the session.

For every file produced this session that the `2.{N}.{type}` row did not list, append it to that row
with the session id and a one-line reason, so the amendment carries its own provenance:

```
`aggregate/{DomainEnum}.java` (added 2.{N}.a - field `{field}` is typed as this enum)
```

Report every amendment in the Step 6 report.

If the **owning session** of a required file is genuinely unclear - it could belong to this session
or to a later one - that is Type 2 friction: halt and ask before writing it (Step 3b).

---

## Step 6: Report Completion

After ticking the checkbox, output a concise structured report:

1. **Files produced** — list all files created or modified (full paths)
2. **Checkbox ticked** — e.g., `[x] 2.1.a`
3. **plan.md additions** — any files added to the plan.md file table during this session (Step 5b), and why
4. **Contradictions / problems** — any contradiction between plan.md and the domain model or rule classification (e.g., a write functionality that mutates a field marked P1 final), or any pattern that required inference or guessing beyond what the docs cover
5. **Doc gaps** — any pattern that wasn't covered by the docs or skill and required inference or guessing; each gap is a candidate for a documentation improvement
6. **Next session** — "Next: 2.{N}.{next-type}" or "Aggregate {Aggregate} complete. Next: aggregate {N+1}."

---

## Step 7: Write Retro

All session context variables are already resolved. Derive retro paths:
- `{session-id}` = `2.{N}.{type}`
- `{session-type-name}` = map: a→"Domain Layer", b→"Write Functionalities", c→"Read Functionalities", d→"Event Wiring"
- `{retro-dir}` = `applications/{app-name}/retros/`
- `{retro-file}` = `{retro-dir}retro-{session-id}-{Aggregate}.md`

### 7.a — Gather Evidence from Conversation Context

This is a synthesis step — do NOT run filesystem audits, grep, or re-read files to reconstruct history. Use only what is already in the conversation context.

Answer these questions by reviewing what happened during the session:

1. **Which files were produced?** List every file created or modified.
2. **Which concept docs were read?** For each: which sections were actually used? Was the doc sufficient?
3. **Was any file outside `applications/{app-name}/` read?** Cross-app reads are a violation (see `conventions.md` § "Application isolation"); record the file read and the gap that drove it as a harness-log row - Type 1 if a doc or skill demonstrably failed to supply what was needed, Type 2 otherwise.
4. **Which instructions in the skill sub-file (`session-{type}.md`) were unclear, missing, or required inference beyond what was written?**
5. **Were there any naming, path, or pattern decisions the skill/docs didn't cover?**
6. **Were there any bugs, corrections, or fixes applied mid-session?** What triggered them?
7. **Were any patterns observed that aren't yet documented anywhere?**
8. **Were any files under `simulator/` modified?** If yes, list each one with: the exact diff (what was removed vs added), the root cause that required the change, and why the fix belongs in the framework rather than in application code.

### 7.b — Write the Retro File

Run Step 7.d first - the `## Harness Changes` section below cites the row numbers it appends.

Create `{retro-file}` using this exact template. Write "none" for any section with nothing to report — do not omit sections.

```markdown
# Retro — {session-id} — {Aggregate}

**App:** {app-name}
**Session:** {session-id} ({session-type-name})
**Date:** {today}

---

## Files Produced

List every file created or modified this session (absolute paths).

### Application files ({app-name})

- `/path/to/file1`
- `/path/to/file2`

### Application bug fixes (earlier-session files)

- (none) OR list files fixed this session that were produced in a previous session

---

> ## ⚠️ SIMULATOR FRAMEWORK CHANGES
>
> [Include this block ONLY if one or more files under `simulator/` were modified. Delete this entire block if no simulator files changed.]
>
> The following files belong to `simulator/` — the **shared core library** used by all applications. Changes here affect every consumer of the library and must be treated as framework patches, not application fixes. Each change below includes a root-cause explanation and a justification for why the fix belongs in the framework rather than in application code.

### `simulator/path/to/ChangedFile.java`

**What changed (diff summary):**
```diff
- removed line(s)
+ added line(s)
```

**Root cause:** (What application-level symptom triggered the investigation? What was wrong in the framework?)

**Fix rationale:** (Why is this the correct fix? Why does the fix belong here and not in the application?)

**Impact scope:** (Which other applications or test profiles are affected? Any production impact?)

---

[Repeat the `### simulator/...` block for each additional simulator file changed.]

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/aggregate.md` | § verifyInvariants, § SagaAggregate | Yes | — |

**Sufficient?** = `Yes` / `Partial` / `No`
- `Partial` = doc existed but was missing something important
- `No` = doc didn't address the need; fell back to inference or guessing

---

## Skill Instructions Feedback

### What worked well

- (specific instructions or patterns in the skill that produced correct output without ambiguity)

### What was unclear or missing

- (gaps or ambiguities that required guessing or looking elsewhere)

### Suggested wording / structure changes

- (optional: concrete rewrite proposals; reference the exact file and section)

---

## Semantic-Lock Coverage Audit (sessions `b` only — write "n/a" for `a`/`c`/`d`)

For every saga produced this session, list every saga step that calls `setSemanticLock`, and the test
name covering its lock-acquisition case.

| Saga class | Step name | Foreign aggregate locked | Lock-acquisition test name | Present? |
|------------|-----------|--------------------------|----------------------------|---------:|

- One row per `setSemanticLock` call site.
- "Present? = No" rows are **Major** findings: add the test in this session, or open an explicit follow-up. **Never tick the session checkbox with an unresolved `No` row** unless the row is followed by an explicit deferral rationale written directly beneath this table.

---

## Documentation Gaps

Gaps in `docs/concepts/` files that caused friction or required inference or guessing.

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/aggregate.md` | X not explained | High | Add section on X |

---

## Patterns to Capture

Patterns or conventions observed during this session that aren't yet documented. Candidates for
adding to docs or skills.

- **Pattern:** (name or brief title)
  **Observed in:** (file path)
  **Description:** (what it does, when to use it)

---

## Harness Changes

Rows appended to `applications/{app-name}/harness-log.md` this session: {row numbers, or "none"}

For each `fixed` row, the `harness:` commit sha:

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|

Rows whose outcome is `declined` or `deferred` have no sha - write `-`.

---

## One-Line Summary

(One sentence: the single most important finding from this retro.)
```

### 7.c — Create Directory and Write File

Create `{retro-dir}` if it does not already exist, then write the completed retro to `{retro-file}`.

**Hard rules for the retro:**
1. **Synthesis only.** No filesystem audits, no grep sweeps, no re-reading files to reconstruct history.
2. **Never omit sections.** If a section has nothing to report, write "none".
3. **Absolute paths in Files Produced.**
4. **Any cross-application read is a violation** — record it as a harness-log row in Step 7.d, naming the file read and the gap that drove it.
5. **No emojis, no hype.** Terse and concrete — paths, file names, section names, decisions.
6. **Does not modify plan.md, source files, or BeanConfigurationSagas.groovy.**
7. **Simulator changes are mandatory to document.** If any file under `simulator/` was modified during the session, the `⚠️ SIMULATOR FRAMEWORK CHANGES` block is **required** in the Files Produced section — not optional. For each changed file include: exact diff, root cause, fix rationale, and impact scope. If no simulator files changed, remove the block entirely rather than leaving it blank.
8. **Semantic-Lock Coverage Audit is mandatory for session-`b` retros.** A session-`b` retro missing the audit table, or containing it with unresolved `Present? = No` rows (without an explicit deferral rationale beneath the table), blocks the Step 8 commit. For sessions `a`/`c`/`d` the section is still present with the literal value "n/a".

Do not print a separate retro completion report — the retro file path is included in the Step 8 commit output.

### 7.d — Append Harness-Log Rows

Do this **before** writing the retro file, so the `## Harness Changes` section can cite the row
numbers it produced.

Read `.claude/skills/_shared/conventions.md` § "Harness log" in full - it owns the column schema and
the rules for `Type`, `Outcome` and `Ref`. Then, for each distinct harness friction point of this
session - every Step 3b classification, plus anything surfaced by the 7.a questions: a `docs/` file
that was missing, wrong or ambiguous; a `.claude/skills/` instruction that failed to guide; a
`simulator/` change; a halt; a cross-application read - append one row to
`applications/{app-name}/harness-log.md`.

- Read the last row of the file to get the next `#`. Append only; never rewrite or delete rows.
- `Session` is this session's `{session-id}`.
- `Ref` is the `harness:` commit sha for the Type 1 fixes already committed during the session.
- If the file does not exist, halt: **"harness-log.md missing. It is created by
  /classify-and-plan."**
- Defects in the generated application are **not** harness friction. They are fixed in this session
  or carried into the Phase 3/4 review reports.

If there was none, append nothing and write "none" in `## Harness Changes`.

---

## Step 8: Commit

Stage all files produced during this session using `git add <specific files>` (never `git add -A`). Include:
- Every file created or modified (from the Step 6 report)
- The retro file written in Step 7
- `applications/{app-name}/harness-log.md`, if Step 7.d appended any row

Issue a single commit using HEREDOC format:

```
feat({app-name}): 2.{N}{type} ({Aggregate} {session-type-name})
```

Where `{session-type-name}` maps: `a`→"Domain Layer", `b`→"Write Functionalities", `c`→"Read Functionalities", `d`→"Event Wiring".

Example: `feat({app-name}): 2.2c (User Read Functionalities)`

After the commit, output the commit hash and message as the final line of the session report.
