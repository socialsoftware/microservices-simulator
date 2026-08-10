---
name: review-artifacts
description: Static consistency check over docs/, .claude/skills/ and .claude/agents/ - path validity, P1-P4 and R1-R8 alignment, neutral-domain compliance, ambiguous guidance. Run at every aggregate boundary during a run, and again before starting one. No arguments. Writes a structured report to docs/reviews/review-{YYYY-MM-DD}.md.
argument-hint: "(no arguments)"
---

# Review Artifacts

Static pre-flight check over the harness itself: `docs/**`, `.claude/skills/**` and
`.claude/agents/**`. It reads only those three trees, checks them for internal consistency, and
writes one dated report. Every check
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
find .claude/skills -type f \( -name "*.md" -o -name "*.template" \) | sort
find .claude/agents -type f -name "*.md" | sort
```

The `*.template` files under `.claude/skills/boot-strap/templates/` are part of the review set:
`boot-strap/SKILL.md` and `templates/README.md` make specific claims about their contents (beans,
fields, marker comments, package declarations), and nothing else in the harness checks those claims.
Read them as code, not as prose — Checks 2 and 3 apply to them only where a doc or skill describes
what they contain.

The files under `.claude/agents/` are part of the review set too. `.claude/agents/aggregate-slice.md`
defines the scope, friction gate and return contract every slice obeys, and
`implement-aggregate-full/SKILL.md` delegates to it at runtime; a stale rule or a leaked domain noun
there reaches generated code exactly as one in a `session-*.md` would.

Hold all three lists. These are the complete artifact sets. Any file path referenced in a skill
or doc must appear in one of these lists to be a valid reference.

**Generated outputs excluded from input set:** files under `docs/reviews/` (e.g., `review-YYYY-MM-DD.md`, `harness-retro-{app-name}-YYYY-MM-DD.md`) are produced by `/review-artifacts` and `/harness-retrospective` and are **not** part of the input artifact enumeration. Do not flag them as untracked artifacts or broken references when they appear on disk but not in the `find docs` list.

---

## Step 2: Read All Artifacts

Read every file returned by the three `find` commands in Step 1.b (all `docs/**/*.md`, all
`.claude/skills/**/*.md`, the `.claude/skills/boot-strap/templates/*.template` scaffolds and all
`.claude/agents/**/*.md`) — this is the complete review set. Do not maintain a separate
hard-coded list here: because the set is derived directly from Step 1.b, newly added files
(e.g. `.claude/skills/_shared/conventions.md`, each `.claude/skills/implement-aggregate/session-*.md`,
or any future skill/doc) are picked up automatically without editing this skill.

Read all files in parallel where possible.

---

## Step 3: Check 1 — Path Validity

For every file path of the form `docs/...`, `.claude/skills/...` or `.claude/agents/...` mentioned
literally (not as a template pattern) in any skill, agent or doc file, verify the path appears in the
Step 1.b artifact list or as a real file on disk.

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

### 3.b — Symbols the scaffold templates are claimed to contain

`boot-strap/SKILL.md` and `.claude/skills/boot-strap/templates/README.md` describe what each
`*.template` file contains: its target path and package declaration, the beans it declares, the
fields and helper methods it ships, and the marker comments later phases append at. Every such
named symbol must be present in the template it is attributed to, and every helper method a
template ships must be described by some doc or skill — a helper no artifact mentions is a Minor
finding, since agents hand-roll the equivalent instead of calling it.

| Template | Symbol claimed | Claimed by | Present? | Notes |
|----------|---------------|------------|----------|-------|
| ... | bean / field / method / marker / package | `boot-strap/SKILL.md:NN` | Yes / No / Undocumented | ... |

Severity: Critical if a template omits something `boot-strap/SKILL.md` tells a later phase to rely
on (Phase 0 then produces an application that does not compile); Minor for an undocumented helper.

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

### 6.a — Run the scan

Derive `{app-name}` per `.claude/skills/_shared/conventions.md` § "Resolve app context". The
forbidden nouns come from that run's own `plan.md`: one per `### {N}. {Aggregate}` section header.

Run the whole scan as one `python3` script. It must be `python3`, not `rg`: a `PreToolUse` hook in
this environment can rewrite a bare `rg` into `grep`, which either fails on `rg`-only flags or scans
with different semantics — see `.claude/skills/_shared/conventions.md` § "Commands whose output feeds
a verdict". Substitute `{app-name}` before running.

```bash
cd "$(git rev-parse --show-toplevel)"
python3 - <<'EOF'
import re, subprocess, sys

APP = "{app-name}"
PATHSPEC = ["docs", ".claude/skills", ".claude/agents", ":(exclude)docs/reviews"]

def git(*args):
    return subprocess.run(["git", *args], capture_output=True, text=True, check=True).stdout

plan = open(f"applications/{APP}/plan.md").read()
nouns = sorted(set(re.findall(r'^### \d+\. (\S+)', plan, re.M)))
if not nouns:
    sys.exit("no aggregate headers found in plan.md - check {app-name}")

head = git("rev-parse", "HEAD").strip()
base = git("merge-base", "HEAD", "master").strip()
derivation = "git merge-base HEAD master"
if base == head:
    oldest = git("log", "--format=%H", "--grep=^harness:", "HEAD", "--", *PATHSPEC[:3]).split()
    if not oldest:
        sys.exit("no harness: commits and merge-base == HEAD - nothing to check")
    base = oldest[-1] + "^"
    derivation = "parent of the oldest harness: commit (merge-base == HEAD)"

diff = git("diff", base, "-M", "--", *PATHSPEC).splitlines()
pattern = re.compile(r"\b(" + "|".join(map(re.escape, nouns)) + r")(s|es|'s)?\b")

current, added, hits = None, 0, []
for line in diff:
    if line.startswith("+++ "):
        current = line[6:]
    elif line.startswith("+"):
        added += 1
        m = pattern.search(line)
        if m:
            hits.append((current, line[1:], m.group(1)))

print(f"BASE={base}  ({derivation})")
print(f"nouns={nouns}")
print(f"added lines scanned={added}  hits={len(hits)}")
for path, line, noun in hits:
    moved = subprocess.run(["git", "grep", "-F", line.strip(), base, "--", *PATHSPEC[:3]],
                           capture_output=True, text=True).returncode == 0
    print(f"\n{'MOVED' if moved else 'HIT  '} {path} [{noun}]\n  {line.strip()}")
EOF
```

**Why the base is derived this way.** `merge-base` alone is not enough: when harness work is
committed on **master** — what happens between runs and during a harness-preparation phase —
`merge-base HEAD master` resolves to `HEAD` itself, the diff is empty, and the check silently passes
having scanned nothing. The fallback walks back to the parent of the oldest `harness:` commit
instead. **State in the report which base was used and how it was derived** — a reader cannot
interpret "0 violations" without knowing how many lines were scanned. If neither rule yields a base
(no `harness:` commits at all), there is nothing to check; say so.

**Why `docs/reviews` is excluded from the pathspec.** For the same reason Step 1.b excludes it from
the input artifact set: those files are this skill's own generated output, and every previous report
prints the forbidden-noun list about itself. Including them turns each past report into a spurious
hit that the move-test cannot clear, because the line is genuinely new at that commit.

### 6.b — Classify each hit

The script prints one line per match, prefixed `MOVED` or `HIT`, plus the totals the report needs.

**A hit is not yet a finding.** `git diff` renders a **moved** line as an addition, so any refactor
that relocates content between harness files reports every domain noun it carried, none of which is
new. The `git grep` in the script is that move-test: a `MOVED` line already existed verbatim at the
base and is not a violation. This is not a rare case — a session-letter swap or a sub-file split
produces nothing else, and on Check 4's first exercise all three hits were moves.

Read each surviving `HIT` before reporting it: a noun that is also an ordinary English word can
appear legitimately, and is a `False positive` with the reason stated. The pattern already matches
the regular `s` / `es` / `'s` inflections; scan the added lines by eye only for an **irregular**
plural of a domain noun (e.g. a noun whose plural changes its stem), which no suffix rule catches.

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
**Scaffold templates reviewed:** {count}
**Agent contract files reviewed:** {count}
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

### Scaffold template contents

| Template | Symbol claimed | Claimed by | Present? | Notes |
|----------|---------------|------------|----------|-------|

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
5. **Static scope only.** The review set is `docs/**`, `.claude/skills/**` and `.claude/agents/**`. The single permitted
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
