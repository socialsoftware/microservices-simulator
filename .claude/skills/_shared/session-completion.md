# Phase 2 Session Completion

The steps that close a Phase 2 session, once its files are produced: tick, amend, report, retro,
harness-log rows, commit. Consumed via a blocking Read pointer by both Phase 2 entry points:

| Skill | Mode | Who runs these steps |
|-------|------|----------------------|
| `.claude/skills/implement-aggregate/SKILL.md` | **single-agent mode** | the one agent that implemented the whole session |
| `.claude/skills/implement-aggregate-full/SKILL.md` | **manager mode** | the manager, after the last slice of the session returned `DONE` |

The two modes differ in exactly one place: § "Retro assembly". Everything else is identical, and the
retro **file shape** is identical in both.

Required context variables (already resolved by the calling skill): `{app-name}`, `{Aggregate}`,
`{N}`, `{type}`.

Derived here:
- `{session-id}` = `2.{N}.{type}`
- `{session-type-name}` = map: a→"Domain Layer", b→"Read Functionalities", c→"Write Functionalities", d→"Event Wiring"
- `{retro-dir}` = `applications/{app-name}/retros/`
- `{retro-file}` = `{retro-dir}retro-{session-id}-{Aggregate}.md`

---

## Tick the checkbox

After all files are produced and BeanConfigurationSagas.groovy is updated, update plan.md:

Replace `- [ ] 2.{N}.{type}` with `- [x] 2.{N}.{type}` in plan.md.

**Match the session checkbox only.** A session may be followed by indented slice sub-checkboxes of
the form `- [ ] 2.{N}.{type}{k} {ItemName}`; `- [ ] 2.{N}.{type}` is a prefix of those, so anchor on
the session line (the one with no trailing digit after `{type}`) rather than doing a bare string
replace.

- **Manager mode:** the slice sub-checkboxes were already ticked one by one as their slices returned
  `DONE`; this step ticks the parent session checkbox. Every sub-checkbox of the session must already
  be ticked - if one is not, the session is not complete and this step must not run.
- **Single-agent mode:** the whole session was implemented in one go, so tick the session checkbox
  **and** every slice sub-checkbox under it. Leaving them unticked would make the session look
  half-done to the next reader.

---

## Amend plan.md for omitted files

plan.md is a fallible blueprint. A file absent from its file table may still be required by the
session sub-file or the concept docs; producing it is correct, and the omission is a defect in
plan.md rather than in the session.

For every file produced this session that the `2.{N}.{type}` row did not list, append it to that row
with the session id and a one-line reason, so the amendment carries its own provenance:

```
`aggregate/{DomainEnum}.java` (added 2.{N}.a - field `{field}` is typed as this enum)
```

In manager mode the source for this list is the `FILES ADDED BEYOND plan.md` block of every slice
return, unioned across the session's slices.

Report every amendment in the completion report.

If the **owning session** of a required file is genuinely unclear - it could belong to this session
or to a later one - that is Type 2 friction: halt and ask before writing it (`AGENTS.md`
§ "Harness evolution"). In manager mode the slice halts and the manager escalates; the manager never
answers a Type 2 itself.

---

## Report completion

After ticking the checkbox, output a concise structured report:

1. **Files produced** — list all files created or modified (full paths)
2. **Checkbox ticked** — e.g., `[x] 2.1.a`
3. **plan.md additions** — any files added to the plan.md file table during this session, and why
4. **Contradictions / problems** — any contradiction between plan.md and the domain model or rule classification (e.g., a write functionality that mutates a field marked P1 final), or any pattern that required inference or guessing beyond what the docs cover
5. **Doc gaps** — any pattern that wasn't covered by the docs or skill and required inference or guessing; each gap is a candidate for a documentation improvement
6. **Next session** — "Next: 2.{N}.{next-type}" or "Aggregate {Aggregate} complete. Next: aggregate {N+1}."

In manager mode, add a seventh line: **Slices run** — the slice ids of this session, each with
`DONE` and the number of re-spawns it took.

---

## Retro assembly

Run § "Harness-log rows" first - the retro's `## Harness Changes` section cites the row numbers it
appends.

Both modes write the same file, `{retro-file}`, using the template in § "Retro template" below. They
differ only in where the material comes from.

### Single-agent mode

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

### Manager mode

The manager has no slice's conversation context, and must not reconstruct one. The synthesis
happened inside each slice, which emitted a `RETRO FRAGMENT` block as part of its return. The
manager **merges fragments**; it does not re-derive their content, and it does not grep or audit the
filesystem to enrich them.

Merge rules, section by section:

- **Files Produced** - union of every slice's `FILES CREATED` and `FILES APPENDED TO`, plus any file
  the manager itself touched under `applications/{app-name}/`. A shared file appended to by several
  slices is listed once.
- **Docs Consulted** - union of the fragment rows, keyed by `(doc, sections used)`. When two slices
  report the same doc with different `Sufficient?` verdicts, keep the **worst** verdict
  (`No` > `Partial` > `Yes`) and record both notes.
- **Skill Instructions Feedback** - concatenate, attributing each bullet to its slice id.
- **Documentation Gaps** - dedupe rows on `(doc, missing/unclear)`. A deduped row keeps one entry in
  the table and names the reporting slices in its `Impact` cell, e.g.
  `High - reported by 2.{N}.{type}1, 2.{N}.{type}3, 2.{N}.{type}4`. The repeat count is signal: a gap
  three independent slices hit is stronger evidence than one slice's opinion, and collapsing the
  rows without recording that would throw the signal away.
- **Patterns to Capture** - concatenate; drop exact duplicates.
- **Semantic-Lock Coverage Audit** (session `c`) - concatenate every slice's rows into the single
  table, in slice order. No dedupe: rows are per call site, and two slices cannot own the same one.
- **Integration-level observations** - the manager adds its own, from what only it saw: friction it
  triaged, slices it re-spawned and why, cross-slice regressions the session-end full suite caught
  that no individual slice saw, and Type 2 escalations with the human's answer. These go under
  § "Skill Instructions Feedback" as a final `### Manager observations` subsection.

If a slice returned no fragment (it halted before implementing), record that under
`### Manager observations` with its slice id and the halt reason. Do not invent a fragment for it.

### Retro template

Create `{retro-file}` using this exact template. Write "none" for any section with nothing to report — do not omit sections.

````markdown
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

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

For every saga produced this session, list every saga step that calls `setSemanticLock`, and the test
name covering its lock-acquisition case.

| Saga class | Step name | Aggregate locked (primary) | Lock-acquisition test name | Present? |
|------------|-----------|----------------------------|----------------------------|---------:|

- One row per `setSemanticLock` call site. Per R4 (`docs/concepts/sagas.md` § R4 Decision Table) that is
  always the saga's **primary** aggregate; a foreign aggregate gets `setForbiddenStates` and acquires no
  lock, so a `setForbiddenStates` step is never a row here.
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
````

### Create directory and write file

Create `{retro-dir}` if it does not already exist, then write the completed retro to `{retro-file}`.

**Hard rules for the retro:**
1. **Synthesis only.** No filesystem audits, no grep sweeps, no re-reading files to reconstruct history. In manager mode this binds the merge too: the material is the slice fragments, not the working tree.
2. **Never omit sections.** If a section has nothing to report, write "none".
3. **Absolute paths in Files Produced.**
4. **Any cross-application read is a violation** — record it as a harness-log row, naming the file read and the gap that drove it.
5. **No emojis, no hype.** Terse and concrete — paths, file names, section names, decisions.
6. **Does not modify plan.md, source files, or BeanConfigurationSagas.groovy.**
7. **Simulator changes are mandatory to document.** If any file under `simulator/` was modified during the session, the `⚠️ SIMULATOR FRAMEWORK CHANGES` block is **required** in the Files Produced section — not optional. For each changed file include: exact diff, root cause, fix rationale, and impact scope. If no simulator files changed, remove the block entirely rather than leaving it blank.
8. **Semantic-Lock Coverage Audit is mandatory for session-`c` retros.** A session-`c` retro missing the audit table, or containing it with unresolved `Present? = No` rows (without an explicit deferral rationale beneath the table), blocks the commit below. For sessions `a`/`b`/`d` the section is still present with the literal value "n/a". In manager mode the table must cover **all** slices of the session, not just the last one.

Do not print a separate retro completion report — the retro file path is included in the commit output.

---

## Harness-log rows

Do this **before** writing the retro file, so the `## Harness Changes` section can cite the row
numbers it produced.

Read `.claude/skills/_shared/conventions.md` § "Harness log" in full - it owns the column schema and
the rules for `Type`, `Outcome` and `Ref`. Then, for each distinct harness friction point of this
session - every gate classification, plus anything surfaced by the retro questions: a `docs/` file
that was missing, wrong or ambiguous; a `.claude/skills/` instruction that failed to guide; a
`simulator/` change; a halt; a cross-application read - append one row to
`applications/{app-name}/harness-log.md`.

- Read the last row of the file to get the next `#`. Append only; never rewrite or delete rows.
- `Session` is this session's `{session-id}`.
- `Ref` is the `harness:` commit sha for the Type 1 fixes already committed during the session.
- If the file does not exist, halt: **"harness-log.md missing. It is created by
  /classify-and-plan."**
- Defects in the generated application are **not** harness friction. Fix them in this session.

If there was none, append nothing and write "none" in `## Harness Changes`.

**In manager mode the manager is the only writer of this file.** Its input is the `FRICTION` blocks
of every slice return, plus its own. One row per distinct friction point across the whole session:
the same ambiguity reported by four slices is **one** row, whose `Problem` cell says so. Slices never
append here, so there is no concurrent-append hazard even though the next `#` is read from the last
row.

---

## Commit

Stage all files produced during this session using `git add <specific files>` (never `git add -A`). Include:
- Every file created or modified (from the completion report)
- The retro file written above
- `applications/{app-name}/harness-log.md`, if any row was appended

Issue a single commit using HEREDOC format:

```
feat({app-name}): 2.{N}{type} ({Aggregate} {session-type-name})
```

Example: `feat({app-name}): 2.2c ({Aggregate} Write Functionalities)`

This is the **only** commit a session produces, in either mode. Slices never commit. `harness:`
commits for Type 1 fixes are separate and are issued when the fix is made, not here.

After the commit, output the commit hash and message as the final line of the session report.
