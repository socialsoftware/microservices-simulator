---
name: retro
description: Post-session retrospective for quizzes-full aggregate implementation. Run after /implement-aggregate to evaluate docs and skill quality, capturing which files were consulted, what gaps were found, and what can be improved. Writes findings to applications/{app-name}/retros/retro-{session}-{Aggregate}.md. Invoke with /retro or /retro 2.3.b.
argument-hint: "[session] (e.g. 2.3.b — optional, auto-detects last completed session)"
---

# Post-Session Retrospective

This skill produces a structured retrospective immediately after a Phase 2 (or Phase 3) session completes.
Its purpose is to surface gaps in docs and skills so they can be improved incrementally — making each
subsequent session cheaper and more reliable.

The audience is the docs/skills maintainer (the user), not a future implementation agent.

## When to invoke

Run immediately after `/implement-aggregate` completes, while the implementation context is still fresh.
The retro should capture what actually happened during the session, not a reconstruction from file diffs.

## Input

```
/retro                 # auto-detect last completed session
/retro 2.3.b           # target a specific session
```

---

## Step 1: Identify the Target Session

### 1.a — Find plan.md

Run: `find applications -name plan.md`

Use the same plan.md as `/implement-aggregate`. If multiple found, use the one with the most recently
ticked checkbox.

### 1.b — Determine target session

**If a session argument was provided (e.g., `2.3.b`):** parse `{N}` and `{type}`. Verify the
checkbox `- [x] 2.{N}.{type}` exists in plan.md (it must be completed).

**If no argument (auto-detect):** scan plan.md for **all** lines matching `- [x] 2.\d+\.[abcd]`.
Use the **last** one in document order — that is the most recently completed session.

Extract:
- `{session-id}` — e.g., `2.3.b`
- `{N}` — aggregate number
- `{type}` — session type (`a`, `b`, `c`, or `d`)
- `{session-type-name}` — map: a→"Domain Layer", b→"Write Functionalities", c→"Read Functionalities", d→"Event Wiring"
- `{Aggregate}` — from the plan.md section header `### {N}. {Aggregate}`

---

## Step 2: Derive Paths

From the plan.md file path (e.g., `applications/quizzes-full/plan.md`):
- `{app-name}` — directory name (e.g., `quizzes-full`)
- `{retro-dir}` — `applications/{app-name}/retros/`
- `{retro-file}` — `{retro-dir}retro-{session-id}-{Aggregate}.md`

Example: `applications/quizzes-full/retros/retro-2.3.b-Tournament.md`

---

## Step 3: Gather Evidence from Conversation Context

This is a synthesis step — do NOT run filesystem audits, grep, or re-read files to reconstruct history.
Use only what is already in the conversation context.

Answer these questions by reviewing what happened during the session:

1. **Which files were produced?** List every file created or modified.
2. **Which concept docs were read?** (e.g., `docs/concepts/aggregate.md`, `docs/concepts/testing.md`)
   For each: which sections were actually used? Was the doc sufficient for the task?
3. **Was the reference app (`applications/quizzes/`) consulted?** If yes: exactly which files, and
   what gap did each consultation fill? This is the most important signal — every reference-app
   lookup means the docs or skill didn't cover something.
4. **Which instructions in the skill sub-file (`session-{type}.md`) were unclear, missing, or required
   inference beyond what was written?**
5. **Were there any naming, path, or pattern decisions the skill/docs didn't cover?**
6. **Were there any bugs, corrections, or fixes applied mid-session?** What triggered them?
7. **Were any patterns observed that aren't yet documented anywhere?**

---

## Step 4: Write the Retro File

Create `{retro-file}` using this exact template. Write "none" for any section with nothing to report —
do not omit sections.

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

---

## Step 5: Create Directory and Write File

Run: `mkdir -p {retro-dir}` (only if the directory does not already exist).

Write the completed retro to `{retro-file}`.

---

## Step 6: Report Completion

Output to chat:
- The absolute path of the retro file written
- The "One-Line Summary" from the file
- The top 1–2 action items (priority + target + action)

---

## Hard Rules

1. **Synthesis only.** This is a retrospective of what happened in THIS session. No filesystem audits,
   no grep sweeps, no re-reading files to reconstruct history.
2. **Never omit sections.** If a section has nothing to report, write "none" — structure stability is
   the whole point.
3. **Absolute paths in Files Produced.** The reader may have a different working directory.
4. **Every reference-app consultation must appear in "Reference App Consulted".** These entries are
   the primary mechanism for surfacing doc gaps.
5. **No emojis, no hype.** Terse and concrete — paths, file names, section names, decisions.
6. **Does not modify plan.md, source files, BeanConfigurationSagas.groovy, or memory.**
   Output is the retro file only.
