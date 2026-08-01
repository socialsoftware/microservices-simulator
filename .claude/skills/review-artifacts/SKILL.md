---
name: review-artifacts
description: Static pre-flight consistency check over docs/ and .claude/skills/ - path validity, P1-P4 and R1-R8 alignment, ambiguous guidance. Run between generation runs, never during one. No arguments. Writes a structured report to docs/reviews/review-{YYYY-MM-DD}.md.
argument-hint: "(no arguments)"
---

# Review Artifacts

Static pre-flight check over the harness itself: `docs/**` and `.claude/skills/**`. It reads only
those two trees, checks them for internal consistency, and writes one dated report. Every check
reads files directly from disk. The only write is the report file produced at the end.

**When to run it:** **between** generation runs — after a round of harness edits, before the next
run is started. Never during a run: while a run is in progress the harness is frozen
(`AGENTS.md` § "Harness freeze") and there is nothing this skill could legitimately act on.

**What it is not:** it does not evaluate how the harness performed on a real run. That is
`/harness-retrospective`, which reads a completed run's `friction-log.md`, retros and reviews.
This skill's ground truth is the harness files themselves; that skill's ground truth is empirical
evidence from a run. Do not read `applications/**` here.

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

**Generated outputs excluded from input set:** files under `docs/reviews/` (e.g., `review-YYYY-MM-DD.md`, `harness-retro-{app-name}-YYYY-MM-DD.md`) are produced by `/review-artifacts` and `/harness-retrospective` and are **not** part of the input artifact enumeration. Do not flag them as untracked artifacts or broken references when they appear on disk but not in the `find docs` list.

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

For every file path of the form `docs/...` or `.claude/skills/...` mentioned literally (not as a
template pattern) in any skill or doc file, verify the path appears in the Step 1.b artifact list
or as a real file on disk.

Paths under `applications/` are **never** existence-checked — a generated application is transient
and its absence is not a finding. Instead check that every such path is written as a template
pattern (`applications/{app-name}/...`) and that the directory structure it describes is
consistent with the layout in `docs/workflow.md`. A **literal** application name hard-coded in a
skill or doc is a Major finding: it should be `{app-name}`.

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
| `.claude/skills/adversarial-review-aggregate/SKILL.md` — Steps 3, 8 | P1, and the Family F re-derivation | ... | ... | ... |

### 4.b — R1–R8 definitions

Extract the authoritative one-liner for each of R1–R8 from `docs/architecture.md`.

Check whether each restriction is mentioned in the concept doc or session skill where it
would most naturally be enforced:

> **Code-conformance owner.** `.claude/skills/adversarial-review-aggregate/SKILL.md` Step 6 is the
> single place R1-R8 are checked against *code* (every other reference teaches or documents them).
> Verify that its Family D table still covers all eight restrictions and that each check matches the
> authoritative one-liner extracted above. A restriction present in `docs/architecture.md` but absent
> from that table is a **Critical** finding — it means the restriction is documented but enforced
> nowhere.

| Restriction | Expected location | In Family D table? | Mentioned? | Claim consistent? | Notes |
|-------------|------------------|--------------------|------------|-------------------|-------|
| R1 — service loads only own aggregate | `docs/concepts/service.md`, session-b.md | ... | ... | ... | ... |
| R2 — service injects only own components | `docs/concepts/service.md`, session-b.md | ... | ... | ... | ... |
| R3 — cross-aggregate flow via DTOs only | `docs/concepts/service.md`, session-b.md | ... | ... | ... | ... |
| R4 — setForbiddenStates on mutating saga steps | `docs/concepts/sagas.md`, session-b.md | ... | ... | ... | ... |
| R5 — getEventSubscriptions in consumer only | `docs/concepts/events.md`, session-d.md | ... | ... | ... | ... |
| R6 — verifyInvariants must not read from DB | `docs/concepts/aggregate.md`, session-a.md | ... | ... | ... | ... |
| R7 — DTOs are immutable value objects | `docs/concepts/service.md`, session-b.md | ... | ... | ... | ... |
| R8 — functionalities only send commands upstream | `docs/concepts/commands.md`, session-b.md | ... | ... | ... | ... |

---

## Step 5: Check 3 — Improvement Opportunities

### 5.a — Missing examples in concept docs

For each concept doc, identify algorithm or pattern descriptions that have no code example
and where a concrete example would reduce the need to infer.

| Doc | Section | Why an example would help |
|-----|---------|---------------------------|

### 5.b — Ambiguous guidance in skill files

Scan skill files for instructions containing:
- "depends on", "may be", "if applicable", "as appropriate", "as needed"
  without a concrete decision criterion following them
- Pattern names referenced without showing the code shape

| File | Step | Ambiguous phrase | What decision criterion is missing |
|------|------|------------------|------------------------------------|

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
**Verdict:** Clean | Minor issues | Issues requiring action

> **Clean** = all alignment checks pass; only improvement opportunities found.
> **Minor issues** = inconsistencies present but none cause incorrect agent output.
> **Issues requiring action** = any inconsistency that would cause an agent to produce
>   incorrect output.

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

---

## Action Items

| Priority | Category | File | Finding | Suggested Fix |
|----------|---------|------|---------|---------------|
| Critical | ... | ... | ... | ... |
| Major    | ... | ... | ... | ... |
| Minor    | ... | ... | ... | ... |

**Critical** = causes an agent to produce incorrect output (wrong code, missing file, wrong beans,
  wrong exception type, broken skill delegation).
**Major** = leaves an agent to infer or guess something the harness should have stated.
**Minor** = cosmetic inconsistency, missing example, ambiguous wording with low impact.
```

---

## Step 7: Print Summary to Conversation

Output to the conversation (not to the report file):

1. Absolute path to `{report-file}`
2. Verdict with one-sentence justification
3. All Critical action items (verbatim from the report Action Items table)
4. Count of Major items and count of Minor items

---

## Hard Rules

1. **Read files directly.** Every check is based on file content read in Step 2. Do not rely
   on conversation memory or prior knowledge of file contents.
2. **Read-only except for the report.** Do not modify any skill, doc, source, or config file.
3. **Never omit sections.** Write "nothing to report" in any section with no findings.
4. **Quote the evidence.** For every Critical or Major finding, quote the conflicting text
   verbatim from both sources (with file path and approximate line context).
5. **Static scope only.** The review set is `docs/**` and `.claude/skills/**`. Do not read
   `applications/**` — no retros, no reviews, no friction logs, no generated source. Empirical
   evaluation of a completed run belongs to `/harness-retrospective`.
6. **Do not run during a generation run.** If any `applications/*/plan.md` has an unchecked
   `- [ ]` box, the harness is frozen (`AGENTS.md` § "Harness freeze") — say so and stop rather
   than producing a report nobody may act on.
7. **One invocation covers all artifacts.** Do not scope to a single aggregate or session.
8. **No emojis. Terse and specific.** File paths, section names, quoted snippets — no fluff.
