---
name: review-artifacts
description: Periodic deep review of docs/ and .claude/skills/ for inconsistencies, alignment gaps, and improvement opportunities. No arguments. Writes a structured report to docs/reviews/review-{YYYY-MM-DD}.md.
argument-hint: "(no arguments)"
---

# Review Artifacts

Periodic read-only review of all documentation and skill files. Every check reads files directly
from disk. The only write is the dated report file produced at the end.

One invocation reviews all artifacts. No arguments needed.

This skill verifies that each piece of harness knowledge has exactly one owning file and that
readers of that knowledge (skills, docs, reports) resolve to it correctly. It does not diff
duplicate copies against each other — the harness has no duplicate copies by design. Each piece
of knowledge has a single owning file (e.g. `.claude/skills/_shared/conventions.md` for shared
skill conventions, `session-*.md` for per-session file/bean lists, `classify-and-plan/SKILL.md`
for the `plan.md` output structure, `docs/concepts/testing.md` for the T1–T4 taxonomy); everywhere
else references it via a pointer rather than restating it.

---

## Step 0: Anchor to the repository root

Before Step 1, read `.claude/skills/_shared/conventions.md` and follow "Anchor to the repository
root". Do not run any command until you have.

## Step 1: Resolve Context

### 1.a — Date and output path

Determine today's date in `YYYY-MM-DD` format.

Set:
```
{review-date}  = today in YYYY-MM-DD
{report-file}  = docs/reviews/review-{review-date}.md
```

If a report for today already exists, append `-2`, `-3`, etc. to avoid overwriting.

### 1.b — Enumerate all artifact files

Run:
```
find docs -type f -name "*.md" | sort
find .claude/skills -type f -name "*.md" | sort
```

Hold both lists. These are the complete artifact sets. Any file path referenced in a skill
or doc must appear in one of these lists to be a valid reference.

**Generated outputs excluded from input set:** files under `docs/reviews/` (e.g., `review-YYYY-MM-DD.md`) are produced by `/review-artifacts` and are **not** part of the input artifact enumeration. Do not flag them as untracked artifacts or broken references when they appear on disk but not in the `find docs` list.

### 1.c — Enumerate and read all retro files

Run: `find applications -name "retro-*.md" | sort`

Read every retro file. For each retro extract:
- `{app-name}`, `{session-id}`, `{Aggregate}` from the filename
- All rows from the "Action Items" table (Priority, Target file, Action)
- The "One-Line Summary" or "Key Outcome" line

Hold all High-priority action items that target a file under `docs/` or `.claude/skills/`.
These are mandatory review targets in Step 5.

---

## Step 2: Read All Artifacts

Read every file returned by the two `find` commands in Step 1.b (all `docs/**/*.md` and all
`.claude/skills/**/*.md`) — this is the complete review set. Do not maintain a separate
hard-coded list here: because the set is derived directly from Step 1.b, newly added files
(e.g. `.claude/skills/_shared/conventions.md`, each `.claude/skills/implement-aggregate/session-*.md`,
or any future skill/doc) are picked up automatically without editing this skill.

Read all files in parallel where possible.

---

## Step 3: Check 1 — Path Validity

For every file path of the form `docs/...`, `.claude/skills/...`, or
`applications/...` mentioned literally (not as a template pattern) in any skill or doc file,
verify the path appears in the Step 1.b artifact list or as a real file on disk.

For template patterns such as `applications/{app-name}/...`, skip existence validation but
check that the directory structure described is consistent with the layout in `docs/workflow.md`.

| Source file | Referenced path | Exists? | Notes |
|-------------|----------------|---------|-------|
| ... | ... | Yes / No / Template | ... |

Flag every broken reference as a finding (severity: Critical if the missing file is a skill
that another skill delegates to at runtime; Major otherwise).

---

## Step 4: Check 2 — Pattern Alignment (P1–P4 and R1–R8)

### 4.a — P1–P4 definitions

Extract the authoritative one-liner for each of P1, P2, P3, P4a, P4b from
`docs/concepts/rule-enforcement-patterns.md`.

Then verify consistency of those definitions in each location below:

| Location | Pattern | Claim in that file | Authoritative definition | Consistent? |
|----------|---------|--------------------|--------------------------|-------------|
| `docs/architecture.md` — pattern selection table | P1/P2/P3/P4 | ... | ... | ... |
| `docs/architecture.md` — Request Lifecycle | P1/P2/P3 | ... | ... | ... |
| `.claude/skills/classify-and-plan/SKILL.md` — Step 4 | P1–P4a/b | ... | ... | ... |
| `.claude/skills/implement-aggregate/session-a.md` | P1 | ... | ... | ... |
| `.claude/skills/implement-aggregate/session-b.md` | P3 | ... | ... | ... |
| `.claude/skills/implement-aggregate/session-d.md` | P2 | ... | ... | ... |
| `.claude/skills/review-aggregate/SKILL.md` — Step 6 | P1–P4 | ... | ... | ... |

### 4.b — R1–R8 definitions

Extract the authoritative one-liner for each of R1–R8 from `docs/architecture.md`.

Check whether each restriction is mentioned in the concept doc or session skill where it
would most naturally be enforced:

| Restriction | Expected location | Mentioned? | Claim consistent? | Notes |
|-------------|------------------|------------|-------------------|-------|
| R1 — service loads only own aggregate | `docs/concepts/service.md`, session-b.md | ... | ... | ... |
| R2 — service injects only own components | `docs/concepts/service.md`, session-b.md | ... | ... | ... |
| R3 — cross-aggregate flow via DTOs only | `docs/concepts/service.md`, session-b.md | ... | ... | ... |
| R4 — setForbiddenStates on mutating saga steps | `docs/concepts/sagas.md`, session-b.md | ... | ... | ... |
| R5 — getEventSubscriptions in consumer only | `docs/concepts/events.md`, session-d.md | ... | ... | ... |
| R6 — verifyInvariants must not read from DB | `docs/concepts/aggregate.md`, session-a.md | ... | ... | ... |
| R7 — DTOs are immutable value objects | `docs/concepts/service.md`, session-b.md | ... | ... | ... |
| R8 — functionalities only send commands upstream | `docs/concepts/commands.md`, session-b.md | ... | ... | ... |

---

## Step 5: Check 3 — Improvement Opportunities

### 5.a — Missing examples in concept docs

For each concept doc, identify algorithm or pattern descriptions that have no code example
and where a concrete example would reduce the need to consult the quizzes reference app.

| Doc | Section | Why an example would help |
|-----|---------|---------------------------|

### 5.b — Ambiguous guidance in skill files

Scan skill files for instructions containing:
- "depends on", "may be", "if applicable", "as appropriate", "as needed"
  without a concrete decision criterion following them
- Pattern names referenced without showing the code shape

| File | Step | Ambiguous phrase | What decision criterion is missing |
|------|------|------------------|------------------------------------|

### 5.c — Open retro action items

For each High-priority action item collected in Step 1.c that targets a `docs/` or
`.claude/skills/` file:

1. Read the current contents of the targeted file.
2. Run: `git log -1 --format="%ai" -- {targeted-file-path}`
3. Compare the modification date against the retro file's creation date
   (`git log -1 --format="%ai" -- {retro-file-path}`).
4. Read the current state of the targeted file and confirm whether the specific action
   described in the retro has been carried out.

| Retro | Session | Priority | Target file | Action | File last modified | Resolved? |
|-------|---------|----------|-------------|--------|--------------------|-----------|

**Resolved** = file was modified after the retro AND the current content addresses the action.
**Open** = file not modified since retro, or modification does not address the specific action.

---

## Step 6: Write the Report

Run `mkdir -p docs/reviews` (no-op if exists).

Write `{report-file}` using the template below. Never omit a section — write
"nothing to report" if a check produced no findings.

```markdown
# Artifacts Review — {review-date}

**Date:** {review-date}
**Skill files reviewed:** {count}
**Doc files reviewed:** {count}
**Retros scanned:** {count}
**Verdict:** Clean | Minor issues | Issues requiring action

> **Clean** = all alignment checks pass; only improvement opportunities found.
> **Minor issues** = inconsistencies present but none cause incorrect agent output.
> **Issues requiring action** = any inconsistency that would cause an agent to produce
>   incorrect output, or any unresolved High-priority retro action item.

---

## Executive Summary

(2–3 sentences: most critical finding, overall quality assessment, recommended next action.)

---

## Check 1 — Path Validity

| Source file | Referenced path | Status | Notes |
|-------------|----------------|--------|-------|

---

## Check 2 — Pattern Alignment (P1–P4 and R1–R8)

### P1–P4 Consistency

| Location | Pattern | Claim | Authoritative definition | Verdict |
|----------|---------|-------|--------------------------|---------|

### R1–R8 Coverage

| Restriction | Expected location | Mentioned? | Consistent? | Notes |
|-------------|------------------|------------|-------------|-------|

---

## Check 3 — Improvement Opportunities

### Missing Examples

| Doc | Section | Why an example would help |
|-----|---------|---------------------------|

### Ambiguous Guidance

| File | Step | Ambiguous phrase | Missing decision criterion |
|------|------|------------------|---------------------------|

### Open Retro Action Items

| Retro | Session | Priority | Target file | Action | File last modified | Resolved? |
|-------|---------|----------|-------------|--------|--------------------|-----------|

---

## Action Items

| Priority | Category | File | Finding | Suggested Fix |
|----------|---------|------|---------|---------------|
| Critical | ... | ... | ... | ... |
| Major    | ... | ... | ... | ... |
| Minor    | ... | ... | ... | ... |

**Critical** = causes an agent to produce incorrect output (wrong code, missing file, wrong beans,
  wrong exception type, broken skill delegation).
**Major** = causes unnecessary reference-app lookups, or is an unresolved High-priority retro item.
**Minor** = cosmetic inconsistency, missing example, ambiguous wording with low impact.
```

---

## Step 7: Print Summary to Conversation

Output to the conversation (not to the report file):

1. Absolute path to `{report-file}`
2. Verdict with one-sentence justification
3. All Critical action items (verbatim from the report Action Items table)
4. Count of Major items and count of Minor items
5. Count of open High-priority retro action items

---

## Hard Rules

1. **Read files directly.** Every check is based on file content read in Step 2. Do not rely
   on conversation memory or prior knowledge of file contents.
2. **Read-only except for the report.** Do not modify any skill, doc, source, or config file.
3. **Never omit sections.** Write "nothing to report" in any section with no findings.
4. **Quote the evidence.** For every Critical or Major finding, quote the conflicting text
   verbatim from both sources (with file path and approximate line context).
5. **Retro action items are mandatory checks.** Every High-priority action item targeting
   `docs/` or `.claude/skills/` must appear in Check 3 with a Resolved / Open determination.
6. **Use `git log` for retro resolution.** Run `git log -1 --format="%ai" -- {file-path}` for
   each targeted file; do not estimate modification dates.
7. **One invocation covers all artifacts.** Do not scope to a single aggregate or session.
8. **No emojis. Terse and specific.** File paths, section names, quoted snippets — no fluff.
