---
name: review-artifacts
description: Static consistency check over docs/ and .claude/ - path validity, P1-P4 and R1-R8 alignment, neutral-domain compliance, ambiguous guidance. Run at every aggregate boundary during a run, and again before starting one. No arguments. Writes a structured report to docs/reviews/review-{YYYY-MM-DD}.md.
argument-hint: "(no arguments)"
---

# Review Artifacts

Static pre-flight check over the harness itself: `docs/**` and `.claude/skills/**`. It reads only
those two trees, checks them for internal consistency, and writes one dated report. Every check
reads files directly from disk. The only write is the report file produced at the end.

**When to run it:** at every **aggregate boundary** — after the last session of aggregate `{N}` is
committed and before the first session of aggregate `{N+1}` begins — and once more before a run
starts, after a round of harness edits.

Running it during a run is the point, not a violation. The harness is self-healing
(`AGENTS.md` § "Harness evolution"): sessions repair `docs/` and `.claude/skills/` mid-run under the
Type 1 gate, so the artifacts change *while* they are being read. A Type 1 fix made in `2.4.c` can
contradict a doc that `2.5.a` is about to read, and the aggregate boundary is the last moment that
contradiction is cheap. A self-healing harness needs more static consistency checking during a run,
not less.

**What it is not:** it does not evaluate how the harness performed on a real run. That is
`/harness-retrospective`, which reads a completed run's `harness-log.md`, retros and reviews.
This skill's ground truth is the harness files themselves; that skill's ground truth is empirical
evidence from a run. The only thing this skill reads under `applications/` is the aggregate name
list in `plan.md`, needed by Check 4 (Step 6), and it reads nothing else there.

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
find .claude -type f -name "*.md" | sort
```

Hold both lists. These are the complete artifact sets. Any file path referenced in a skill
or doc must appear in one of these lists to be a valid reference.

The second `find` covers **all** of `.claude`, not just `.claude/skills`. `AGENTS.md`
§ "Harness evolution" defines the harness delta as `git log --oneline docs/ .claude/`, and
`.claude/agents/aggregate-slice.md` is a harness file that `implement-aggregate-full` delegates to at
runtime - `docs/workflow.md` § "Two entry points" calls it the contract for a slice. Enumerating only
the skills tree leaves it unread by Step 2 and unscanned by Step 6.

**Generated outputs excluded from input set:** files under `docs/reviews/` (e.g., `review-YYYY-MM-DD.md`, `harness-retro-{app-name}-YYYY-MM-DD.md`) are produced by `/review-artifacts` and `/harness-retrospective` and are **not** part of the input artifact enumeration. Do not flag them as untracked artifacts or broken references when they appear on disk but not in the `find docs` list.

---

## Step 2: Read All Artifacts

Read every file returned by the two `find` commands in Step 1.b (all `docs/**/*.md` and all
`.claude/**/*.md`) - this is the complete review set. Do not maintain a separate
hard-coded list here: because the set is derived directly from Step 1.b, newly added files
(e.g. `.claude/skills/_shared/conventions.md`, each `.claude/skills/implement-aggregate/session-*.md`,
or any future skill/doc) are picked up automatically without editing this skill.

Read all files in parallel where possible.

---

## Step 3: Check 1 — Path Validity

For every file path of the form `docs/...` or `.claude/...` mentioned literally (not as a
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
| `.claude/skills/implement-aggregate/session-c.md` | P3 | ... | ... | ... |
| `.claude/skills/implement-aggregate/session-d.md` | P2 | ... | ... | ... |

### 4.b — R1–R8 definitions

Extract the authoritative one-liner for each of R1–R8 from `docs/architecture.md`.

Check whether each restriction is mentioned in the concept doc or session skill where it
would most naturally be enforced:

| Restriction | Expected location | Mentioned? | Claim consistent? | Notes |
|-------------|------------------|------------|-------------------|-------|
| R1 — service loads only own aggregate | `docs/concepts/service.md`, session-b.md, session-c.md | ... | ... | ... |
| R2 — service injects only own components | `docs/concepts/service.md`, session-b.md, session-c.md | ... | ... | ... |
| R3 — cross-aggregate flow via DTOs only | `docs/concepts/service.md`, session-b.md, session-c.md | ... | ... | ... |
| R4 — saga steps declare lock intent (semantic lock vs forbiddenStates) | `docs/concepts/sagas.md`, session-c.md | ... | ... | ... |
| R5 — getEventSubscriptions in consumer only | `docs/concepts/events.md`, session-d.md | ... | ... | ... |
| R6 — verifyInvariants must not read from DB | `docs/concepts/aggregate.md`, session-a.md | ... | ... | ... |
| R7 — DTOs are immutable value objects | `docs/concepts/service.md`, session-b.md, session-c.md | ... | ... | ... |
| R8 — functionalities only send commands upstream | `docs/concepts/commands.md`, session-b.md, session-c.md | ... | ... | ... |

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

## Step 6: Check 4 — Neutral Domain

`.claude/skills/_shared/conventions.md` § "Neutral domain" forbids a harness fix from naming any
entity, aggregate or operation of the application currently being generated. The rule exists because
fixes are authored while looking at one specific aggregate, and the vivid example that comes to mind
is a leaked answer for the next application the harness is pointed at. Self-healing makes that risk
continuous, so this check is what turns the rule from a disclaimer into a control.

Skip this check only when no run is in progress (no `plan.md` anywhere under `applications/`); say so
in the report rather than omitting the section.

### 6.a — Collect the forbidden nouns

Derive `{app-name}` per `.claude/skills/_shared/conventions.md` § "Resolve app context". The names
come from that run's own `plan.md`: one per `### {N}. {Aggregate}` section header. Write them to a
scratch file, one per line.

```bash
cd "$(git rev-parse --show-toplevel)"
rg -o '^### [0-9]+\. \S+' applications/{app-name}/plan.md | sed 's/^### [0-9]*\. //' | sort -u > /tmp/harness-nouns.txt
```

### 6.b — Diff the harness against the run's base commit

The base commit is where the run's harness edits begin:

```bash
BASE=$(git merge-base HEAD master)
[ "$BASE" = "$(git rev-parse HEAD)" ] && BASE=$(git log --format=%H --grep='^harness:' HEAD -- docs .claude | tail -1)^
git diff "$BASE" -- docs .claude ':(exclude)docs/reviews' -M | rg '^\+' | rg -v '^\+\+\+' > /tmp/harness-added.txt
```

`merge-base` alone is not enough. When harness work is committed on **master** — which is what
happens between runs and during a harness-preparation phase — `merge-base HEAD master` resolves to
`HEAD` itself, the diff is empty, and the check silently passes having scanned nothing. The fallback
walks back to the parent of the oldest `harness:` commit instead, so a run of harness edits on master
is still measured. **State in the report which base was used and how it was derived** — a reader
cannot interpret "0 violations" without knowing how many lines were scanned.

If neither rule yields a base (no `harness:` commits at all), there is nothing to check; say so.

`':(exclude)docs/reviews'` keeps the pathspec aligned with Step 1.b, which excludes the generated
report tree from the artifact set. Without it every run after the first re-scans the previous
report's own "Forbidden nouns" line and reports one guaranteed false positive per aggregate.

### 6.c — Match

```bash
rg -n -w -f /tmp/harness-nouns.txt /tmp/harness-added.txt
```

`-w` matches whole words only, so a hit is a real occurrence of the noun and not a substring of an
unrelated identifier.

**A hit is not yet a finding.** `git diff` renders a **moved** line as an addition, so any refactor
that relocates content between harness files reports every domain noun it carried, none of which is
new. Before reporting a hit, check whether the identical line already existed at the base:

```bash
git grep -F "<the added line>" "$BASE" -- docs .claude
```

A match means the line was moved, not introduced: it is a `Moved` verdict, not a violation. This is
not a rare case - a session-letter swap or a sub-file split produces nothing else, and on Check 4's
first exercise all three hits were moves.

Then report the added line verbatim and the file it came from (re-run
`git diff "$BASE" -- docs .claude ':(exclude)docs/reviews' -M` and locate the hunk, since the filtered file has lost
its `+++` headers). Read each surviving hit before reporting it: a noun that is also an ordinary
English word can appear legitimately, and a plural or possessive form will not match `-w` at all, so
scan the added lines for those by eye.

| File | Added line | Noun | Verdict |
|------|-----------|------|---------|

Verdicts: `Violation` (a domain noun newly introduced into the harness - Major, with the neutral
rewrite to apply) / `Moved` (present verbatim at the base elsewhere; name the file it came from) /
`False positive` (ordinary English usage, with the reason).

Report the three counts separately. A run whose hits are all `Moved` is a clean run, and saying so
is more useful than a bare "0 violations".

---

## Step 7: Write the Report

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

## Check 4 — Neutral Domain

**Run in progress:** {app-name} | none (check skipped)
**Base commit:** {BASE}
**Added lines scanned:** {count}

| File | Added line | Noun | Verdict |
|------|-----------|------|---------|

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

## Step 8: Print Summary to Conversation

Output to the conversation (not to the report file):

1. Absolute path to `{report-file}`
2. Verdict with one-sentence justification
3. All Critical action items (verbatim from the report Action Items table)
4. Count of Major items and count of Minor items
5. Neutral-domain verdict: number of `Violation` rows from Check 4, or "clean", or "skipped (no run
   in progress)"

---

## Hard Rules

1. **Read files directly.** Every check is based on file content read in Step 2. Do not rely
   on conversation memory or prior knowledge of file contents.
2. **Read-only except for the report.** Do not modify any skill, doc, source, or config file.
3. **Never omit sections.** Write "nothing to report" in any section with no findings.
4. **Quote the evidence.** For every Critical or Major finding, quote the conflicting text
   verbatim from both sources (with file path and approximate line context).
5. **Static scope only.** The review set is `docs/**` and `.claude/skills/**`. The single permitted
   read under `applications/**` is the `### {N}. {Aggregate}` header list in `plan.md`, for Check 4
   (Step 6). No retros, no reviews, no harness log, no generated source. Empirical evaluation of a
   completed run belongs to `/harness-retrospective`.
6. **Running during a generation run is expected.** Unchecked `- [ ]` boxes in a `plan.md` are not a
   precondition failure - this skill is the aggregate-boundary checkpoint of a self-healing harness
   (`AGENTS.md` § "Harness evolution"). Never halt on them.
7. **Report, do not repair.** The findings are for a human or a later session to act on. This skill
   writes exactly one file. A Critical finding does not license fixing the artifact here.
8. **One invocation covers all artifacts.** Do not scope to a single aggregate or session.
9. **No emojis. Terse and specific.** File paths, section names, quoted snippets — no fluff.
