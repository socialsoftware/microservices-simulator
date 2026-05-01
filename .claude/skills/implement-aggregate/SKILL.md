---
name: implement-aggregate
description: Phase 2 aggregate implementation for microservices-simulator. Auto-detects the next unchecked session in plan.md and delegates to the appropriate sub-file (session-a.md through session-d.md). Invoke with /implement-aggregate [session] (e.g., /implement-aggregate or /implement-aggregate 2.3.b).
argument-hint: "[session] (e.g. 2.3.b — optional, auto-detects if omitted)"
---

# Phase 2: Implement Aggregate

This skill drives one Phase 2 session at a time, advancing the plan.md job queue by one checkbox per invocation. It reads plan.md, identifies the next unchecked session, loads only the instructions relevant to that session type, and produces the files listed in plan.md for that session.

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

## Step 1: Locate plan.md and Identify the Target Session

### 1.a — Find plan.md

Run: `find applications -name plan.md`

Collect all results. If multiple found, check which have unchecked checkboxes (`- [ ]`).

- **Exactly one** with unchecked boxes → use it.
- **Multiple** → ask user (see prompt above).
- **None** → halt: "No plan.md found. Run /classify-and-plan first to generate one."

### 1.b — Determine target session

**If a session argument was provided (e.g., `2.3.b`):**

Parse it: `{N}` = `3`, `{type}` = `b`. Verify the checkbox `- [ ] 2.3.b` exists in plan.md. If already ticked, warn: "Session 2.3.b is already marked complete. Did you mean a different session?" and ask for confirmation before proceeding.

**If no argument (auto-detect):**

Scan plan.md for the first line matching `- [ ] 2.\d+\.[abcd]`. Extract:
- `{N}` = the aggregate number (the `\d+`)
- `{type}` = the session type (`a`, `b`, `c`, or `d`)

If no unchecked Phase 2 session found, check for unchecked Phase 3 sessions (`- [ ] 3.\d+`). If those exist, inform: "All Phase 2 sessions are complete. Use /implement-behaviour for Phase 3." If everything is ticked, inform: "All sessions are complete — nothing left to do."

---

## Step 2: Derive Context Variables

From the plan.md file path (e.g., `applications/quizzes-full/plan.md`):
- `{app-name}` = the directory name (e.g., `quizzes-full`)
- `{pkg}` = app-name with hyphens removed, lowercase (e.g., `quizzesfull`)
- `{AppClass}` = PascalCase — split on hyphens, capitalize each segment, join (e.g., `QuizzesFull`)
- `{appClass}` = same but first segment lowercase (e.g., `quizzesFull`)

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

This is the authoritative source for what to produce. Do not re-derive the file list — use the one in plan.md exactly.

---

## Step 4: Delegate to the Appropriate Sub-File

Based on `{type}`, load and follow the corresponding sub-file. All context variables derived above are available to the sub-file.

| type | Sub-file | Session name |
|------|----------|--------------|
| `a`  | `session-a.md` | Domain Layer |
| `b`  | `session-b.md` | Write Functionalities |
| `c`  | `session-c.md` | Read Functionalities |
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

## Step 5b: Patch plan.md for Missing Files

While producing files, note any file that should exist but is absent from plan.md's file table for this session — for example, an enum type class (e.g., `CourseType.java`) for an aggregate field typed as that enum, or an owned entity class referenced in the domain model.

- For **unambiguous** omissions (a file you had to create to make the code compile): add it to the appropriate session row in plan.md and record it in the final report.
- For **ambiguous** cases (you are unsure whether a file belongs or should be generated elsewhere): ask the user before adding.

---

## Step 6: Report Completion

After ticking the checkbox, output a concise structured report:

1. **Files produced** — list all files created or modified (full paths)
2. **Checkbox ticked** — e.g., `[x] 2.1.a`
3. **plan.md additions** — any files added to the plan.md file table during this session (Step 5b), and why
4. **Contradictions / problems** — any contradiction between plan.md and the domain model or rule classification (e.g., a write functionality that mutates a field marked P1 final), or any pattern that didn't match the docs and required consulting the reference app
5. **Reference app consulted** — list any files read from `applications/quizzes/` and what question each resolved (signals for future doc improvement)
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
3. **Was the reference app (`applications/quizzes/`) consulted?** If yes: exactly which files, and what gap did each fill? Every reference-app lookup means the docs or skill didn't cover something.
4. **Which instructions in the skill sub-file (`session-{type}.md`) were unclear, missing, or required inference beyond what was written?**
5. **Were there any naming, path, or pattern decisions the skill/docs didn't cover?**
6. **Were there any bugs, corrections, or fixes applied mid-session?** What triggered them?
7. **Were any patterns observed that aren't yet documented anywhere?**

### 7.b — Write the Retro File

Create `{retro-file}` using this exact template. Write "none" for any section with nothing to report — do not omit sections.

```markdown
# Retro — {session-id} — {Aggregate}

**App:** {app-name}
**Session:** {session-id} ({session-type-name})
**Date:** {today}

---

## Files Produced

List every file created or modified this session (absolute paths).

- `/path/to/file1`
- `/path/to/file2`

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/aggregate.md` | § verifyInvariants, § SagaAggregate | Yes | — |

**Sufficient?** = `Yes` / `Partial` / `No`
- `Partial` = doc existed but was missing something important
- `No` = doc didn't address the need; fell back to reference app or inference

---

## Reference App Consulted

Each entry here is a gap signal — it means the docs or skill didn't cover something.

| Reference file | Why consulted | Gap it reveals |
|---------------|--------------|----------------|
| `applications/quizzes/...` | needed to see X pattern | session-a.md doesn't explain X |

If nothing was consulted from the reference app, write: **none**

---

## Skill Instructions Feedback

### What worked well

- (specific instructions or patterns in the skill that produced correct output without ambiguity)

### What was unclear or missing

- (gaps or ambiguities that required guessing or looking elsewhere)

### Suggested wording / structure changes

- (optional: concrete rewrite proposals; reference the exact file and section)

---

## Documentation Gaps

Gaps in `docs/concepts/` files that caused friction or required reference-app consultation.

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

## Action Items

Prioritised improvements to docs and skills based on this retro.

| Priority | Target file | Action |
|----------|------------|--------|
| High | `.claude/skills/implement-aggregate/session-a.md` | ... |
| Medium | `docs/concepts/aggregate.md` | ... |

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
4. **Every reference-app consultation must appear in "Reference App Consulted".**
5. **No emojis, no hype.** Terse and concrete — paths, file names, section names, decisions.
6. **Does not modify plan.md, source files, or BeanConfigurationSagas.groovy.**

Do not print a separate retro completion report — the retro file path is included in the Step 8 commit output.

---

## Step 8: Commit

Stage all files produced during this session using `git add <specific files>` (never `git add -A`). Include:
- Every file created or modified (from the Step 6 report)
- The retro file written in Step 7

Issue a single commit using HEREDOC format:

```
feat({app-name}): 2.{N}{type} ({Aggregate} {session-type-name})
```

Where `{session-type-name}` maps: `a`→"Domain Layer", `b`→"Write Functionalities", `c`→"Read Functionalities", `d`→"Event Wiring".

Example: `feat(quizzes-full): 2.2c (User Read Functionalities)`

After the commit, output the commit hash and message as the final line of the session report.

---

## Reference App

If a pattern is unclear from the concept docs alone, consult the equivalent file in `applications/quizzes/src/` — it is the authoritative working example. Note what you consulted so the docs can be improved later.
