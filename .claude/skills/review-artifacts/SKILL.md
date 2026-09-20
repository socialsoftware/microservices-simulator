---
name: review-artifacts
description: Garbage collector for the harness itself - a static consistency check over docs/, .claude/skills/, .claude/agents/, AGENTS.md and HARNESS.md covering path validity, P1-P4 and R1-R8 alignment, neutral-domain compliance, plain-domain contamination in every application's domain model, and ambiguous guidance. Recommended after any substantial harness change, and after each finished aggregate when self-healing is ON; optional otherwise. Expensive: it reads every harness file in full, so run it in a fresh session. No arguments. Writes a structured report to reviews/review-{YYYY-MM-DD}.md.
argument-hint: "(no arguments)"
---

# Review Artifacts

Static pre-flight check over the harness itself: `docs/**`, `.claude/skills/**`,
`.claude/agents/**`, `AGENTS.md` and `HARNESS.md`. It reads those three trees and those two root
files, checks them for internal consistency, and writes one dated report. Two checks reach outside
that set by name: Check 4 reads `plan.md`'s aggregate list and Check 5 reads every
`applications/*/*-domain-model.md`. Every check
reads files directly from disk. The only write is the report file produced at the end.

**What it is for:** collecting the debris that hand-editing and mid-run repair leave in the harness —
paths that no longer resolve, two files prescribing different things, a piece of knowledge that lost
its single owner, a domain noun leaked in from the application being generated, a decomposition
decision leaked into a plain domain.

**It is expensive.** It reads every harness file in full, so it fills a context window fast. Run it
in a fresh session, never inline in a Phase 2 session or alongside work whose context you still
need. That cost is why the guidance below is a recommendation rather than a mandatory step: run it
when the harness has actually changed, not on a schedule.

**When it is worth running,** in descending order:

1. **After any substantial change to the harness** — a refactor, a batch of doc rewrites, a new or
   retired skill. This is the primary use, and the run that follows reads whatever those edits left
   behind.
2. **After each finished aggregate, under self-healing ON** (`AGENTS.md` § "Harness evolution").
   There, sessions repair `docs/` and `.claude/skills/` under the Type 1 gate, so the artifacts
   change *while* they are being read: a fix made in `2.4.c` can contradict a doc that `2.5.a` is
   about to read, and the aggregate boundary is the last moment that contradiction is cheap.
3. **Occasionally under OFF, if you want the neutral-domain sweep early.** Under OFF the harness
   cannot drift mid-run, so between aggregates there is little new for it to find. Running it during
   a run is never a violation — just rarely worth its cost before the run ends.

Running it is always the human's call, under both Phase 2 entry points.

**What it is not:** it does not evaluate how the harness performed on a real run. That is
`/harness-retrospective`, which reads a completed run's `harness-log.md`, retros and reviews.
This skill's ground truth is the harness files themselves; that skill's ground truth is empirical
evidence from a run. Two things under `applications/` are read, each by one named check and for
nothing else: the aggregate name list in `plan.md`, for Check 4 (Step 6), and every
`*-domain-model.md`, for Check 5 (Step 6.5).

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
{report-file}  = reviews/review-{review-date}.md
```

If a report for today already exists, append `-2`, `-3`, etc. to avoid overwriting.

### 1.b — Enumerate all artifact files

Run:
```
find docs -type f -name "*.md" | sort
find .claude/skills -type f \( -name "*.md" -o -name "*.template" \) | sort
find .claude/agents -type f -name "*.md" | sort
ls AGENTS.md HARNESS.md
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

`AGENTS.md` and `HARNESS.md` are in the review set for the same reason, one level up: they are
repo-root markdown that a skill or agent contract **reads at runtime**.
`implement-aggregate/SKILL.md`, `implement-aggregate-full/SKILL.md` and
`.claude/agents/aggregate-slice.md` each instruct the agent to read `AGENTS.md`
§ "Harness evolution" *in full* before acting on friction, and that section defines the
Type 1 / Type 2 / `2-fw` gate the whole self-healing loop turns on. `HARNESS.md` is read the same
way: `docs/workflow.md` § "Spec Authoring" delegates to `HARNESS.md` § 5 for the spec-pair detail. A
contradiction in either reaches generated code exactly as one in a `session-*.md` would.

That criterion — read at runtime by a skill or agent contract — is the whole test, and it is
deliberately not a wildcard over repo-root `*.md`. `README.md` documents the framework for humans
and no skill delegates to it; `CLAUDE.md` is a one-line `@AGENTS.md` include with nothing of its own
to check. Neither is in the set. If a future root file starts being read at runtime, add it here.

`CLAUDE.md` is nonetheless in Check 4's commit pathspec (Step 6). The two scopes answer different
questions: the review set is the files whose *content* is checked for consistency, while Check 4
scans the *commits* that make up the harness delta as `AGENTS.md` § "Harness evolution" defines it. A
harness fix can land in `CLAUDE.md`, and a domain noun leaked there leaks whether or not the file has
prose of its own worth reviewing.

Hold all four lists. These are the complete artifact sets. Any file path referenced in a skill
or doc must appear in one of these lists to be a valid reference.

The second `find` covers **all** of `.claude`, not just `.claude/skills`. `AGENTS.md`
§ "Harness evolution" defines the harness delta as `git log --oneline docs/ .claude/`, and
`.claude/agents/aggregate-slice.md` is a harness file that `implement-aggregate-full` delegates to at
runtime - `docs/workflow.md` § "Two entry points" calls it the contract for a slice. Enumerating only
the skills tree leaves it unread by Step 2 and unscanned by Step 6.

**Generated outputs excluded from input set:** files under `reviews/` (e.g., `review-YYYY-MM-DD.md`, `harness-retro-{app-name}-YYYY-MM-DD.md`) are produced by `/review-artifacts` and `/harness-retrospective` and are **not** part of the input artifact enumeration. Do not flag them as untracked artifacts or broken references when they appear on disk but not in the `find docs` list.

---

## Step 2: Read All Artifacts

Read every file listed by Step 1.b (all `docs/**/*.md`, all
`.claude/skills/**/*.md`, the `.claude/skills/boot-strap/templates/*.template` scaffolds, all
`.claude/agents/**/*.md`, `AGENTS.md` and `HARNESS.md`) — this is the complete review set. Do not
maintain a separate hard-coded list here: because the set is derived directly from Step 1.b, newly
added files (e.g. `.claude/skills/_shared/conventions.md`, each `.claude/skills/implement-aggregate/session-*.md`,
or any future skill/doc) are picked up automatically without editing this skill.

Read all files in parallel where possible.

---

## Step 3: Check 1 — Path Validity

For every file path of the form `docs/...`, `.claude/skills/...` or `.claude/agents/...`, and every
reference to `AGENTS.md`, mentioned literally (not as a template pattern) in any skill, agent or doc
file, verify the path appears in the Step 1.b artifact list or as a real file on disk.

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
is a leaked answer for the next application the harness is pointed at. This check is what turns the
rule from a disclaimer into a control, and under self-healing ON, where harness fixes land mid-run,
the risk it controls is continuous rather than confined to edits made between runs.

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
TREES = ["docs", ".claude/skills", ".claude/agents", "AGENTS.md", "CLAUDE.md", "HARNESS.md"]
PATHSPEC = TREES + [":(exclude)reviews"]

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
    oldest = git("log", "--format=%H", "--grep=^harness:", "HEAD", "--", *TREES).split()
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
    moved = subprocess.run(["git", "grep", "-F", line.strip(), base, "--", *TREES],
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

**Why `reviews` is excluded from the pathspec.** For the same reason Step 1.b excludes it from
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

## Step 6.5: Check 5 — Plain-Domain Contamination

`docs/templates/domain-model-template.md` § "What this file must never say" gives the plain domain a
closed vocabulary: it describes the domain and names no decomposition. The property that buys is
**one plain domain, N aggregate groupings** — a second grouping file must be writable over an
existing domain model with zero edits to it. One leaked `aggregateId`, one `state == DELETED`, one
"entity A caches entity B's identifier" and that property is gone, silently, because nothing fails
until someone tries to write the second grouping.

This check is the control. It scans **every** `applications/*/*-domain-model.md`, not only the run
in progress: a contaminated domain model is a defect in that pair whenever it is found.

### 6.5.a — Run the scan

`python3`, not `rg`, for the reason Step 6.a gives.

```bash
cd "$(git rev-parse --show-toplevel)"
python3 - <<'EOF'
import re, sys, glob

CASE_SENSITIVE = [
    r'\w*AggregateId\b', r'\w+Version\b', r'\bOwns\b', r'\bACTIVE\b', r'\bINACTIVE\b',
    r'\bDELETED\b', r'\bEventSubscription\b', r'Primary Aggregate', r'Other Aggregates',
    r'\bAggregateState\b', r'\bP[1-4][ab]?\b',
]
CASE_INSENSITIVE = [
    r'\baggregates?\b', r'\bsnapshots?\b', r'\bcach(e|ed|es|ing)\b', r'\bevents?\b',
    r'\bsubscri\w*', r'\bsagas?\b', r'\bco-located\b',
]

CODE  = re.compile(r'`[^`]*`')
LINK  = re.compile(r'\]\([^)]*\)')
OPEN  = re.compile(r'<!--\s*plain-domain:\s*allow\b(.*?)-->')
CLOSE = re.compile(r'<!--\s*plain-domain:\s*end\s*-->')

def masked(line):
    out = list(line)
    for rx in (CODE, LINK):
        for m in rx.finditer(line):
            for i in range(m.start(), m.end()):
                out[i] = ' '
    return ''.join(out)

failed = False
for path in sorted(glob.glob('applications/*/*-domain-model.md')):
    hits, allowed, reasons, inside = [], 0, [], False
    for n, line in enumerate(open(path), 1):
        m = OPEN.search(line)
        if m:
            inside = True
            reasons.append((n, m.group(1).strip(' -')))
        if inside:
            if CLOSE.search(line):
                inside = False
            allowed += 1
            continue
        text = masked(line)
        for rx in CASE_SENSITIVE:
            mm = re.search(rx, text)
            if mm: hits.append((n, mm.group(0), line.rstrip()))
        for rx in CASE_INSENSITIVE:
            mm = re.search(rx, text, re.I)
            if mm: hits.append((n, mm.group(0), line.rstrip()))
    print(f"== {path}  ({allowed} allow-listed lines)")
    for n, why in reasons:
        print(f"   allow @{n}: {why or '(NO REASON GIVEN)'}")
    if not hits:
        print("   clean")
        continue
    failed = True
    for n, tok, line in hits:
        print(f"   {n}: [{tok}] {line[:160]}")
sys.exit(1 if failed else 0)
EOF
```

**What the two suppression rules are and why.**

- **Inline code and link targets are masked.** A domain model legitimately links to its grouping
  file, whose filename contains "aggregate", and legitimately writes a field name in backticks.
  Matching inside either produces noise that no rewrite can remove.
- **An explicit allow region.** A line inside
  `<!-- plain-domain: allow — <reason> -->` … `<!-- plain-domain: end -->` is skipped, and the
  script prints the reason for every region it honoured. This is the documented allow-list: it lives
  in the file it exempts, it names its justification, and the review reads those reasons back. The
  two legitimate uses so far are a **provenance note** (which must name what moved out of the file)
  and a **preamble that names benchmark services or a source system** whose own names carry the
  vocabulary. A region with `(NO REASON GIVEN)` is itself a finding.

**A region left unclosed exempts the rest of the file**, which is how a whole domain model is
excused: a marker on line 1 with no `end` covers everything. That form is for a **historic run
record authored before the plain-domain contract** and deliberately not re-partitioned. It is not a
way to silence a live pair — for one of those, exempt the individual lines and say why.

A domain whose subject matter genuinely contains one of these words — an event-ticketing
application, say — uses an allow region for the affected lines and states that in its provenance
preamble. Do not widen the pattern list to accommodate it; the pattern list is the contract, and a
per-file exemption keeps the contract legible.

### 6.5.b — Classify each hit

Each hit is one of:

| Verdict | Meaning | What to write |
|---|---|---|
| `Contamination` | a decomposition fact, or its vocabulary, stated in the plain domain | the domain-vocabulary rewrite, and which grouping section the displaced fact belongs in |
| `False positive` | ordinary English that the masking did not catch | the reason |
| `Undocumented exemption` | an allow region with no reason | the reason to add, or the region to remove |

**These findings are not harness friction, and this skill's agent never fixes them.** A spec pair is
run record, not the Harness bucket (`AGENTS.md` § "What the harness is"), so a contaminated domain
model is neither a Type 1 edit to make on sight nor a Type 2 question about the harness. Report it;
the human decides whether to re-partition that pair. The one thing that *is* harness friction is a
gap in the template or in `/author-spec` that let the contamination through, and that is filed
normally under Check 1, 2 or 3.

---

## Step 7: Write the Report

Run `mkdir -p reviews` (no-op if exists).

Write `{report-file}` using the template below. Never omit a section — write
"nothing to report" if a check produced no findings.

**State the harness-evolution type each check's findings carry** (`AGENTS.md` § "Harness
evolution"), because a later session acting on this report decides from it whether it may fix
unilaterally:

| Check | Type its findings carry | Why |
|-------|------------------------|-----|
| Check 1 — Path Validity | Type 1 candidates | A path or symbol that does not exist is a mechanical contradiction. |
| Check 2 — Pattern Alignment | Type 1 candidates | Two artifacts prescribing different things for the same pattern is a demonstrable contradiction. |
| Check 3 — Improvement Opportunities | **Type 2 - must halt** | Missing examples and ambiguous guidance are silences, not contradictions: nothing in the harness is provably wrong, so the fix is a design decision the human owns. |
| Check 4 — Neutral Domain | Type 1 candidates | A domain noun in a harness file contradicts `_shared/conventions.md` § "Neutral domain". |
| Check 5 — Plain-Domain Contamination | **Neither - human's call** | The file is a spec pair, which is run record and not the Harness bucket. Report and stop. A template or `/author-spec` gap that allowed it is a separate finding under Checks 1-3. |

A Check 3 finding never becomes Type 1 by being obviously right, small, or already agreed in
conversation. If a Check 3 finding also exposes a genuine contradiction, the contradiction is a
Check 1 or Check 2 finding and belongs in that section, filed on its own evidence.

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

> Findings in this check are **Type 2** (`AGENTS.md` § "Harness evolution"): a session acting on
> them halts and asks the human, and never applies them under the Type 1 fast path.

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

## Check 5 — Plain-Domain Contamination

**Type:** neither Type 1 nor Type 2 - report only; the spec pair is run record.

**Domain models scanned:** {count} | **Clean:** {count} | **With findings:** {count}

| Domain model | Line | Token | Verdict | Rewrite / destination |
|---|---|---|---|---|

### Allow regions honoured

| Domain model | Line | Stated reason |
|---|---|---|

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
6. Plain-domain verdict: number of `Contamination` rows from Check 5 and the domain models they are
   in, or "clean across {n} domain models"

---

## Hard Rules

1. **Read files directly.** Every check is based on file content read in Step 2. Do not rely
   on conversation memory or prior knowledge of file contents.
2. **Read-only except for the report.** Do not modify any skill, doc, source, or config file.
3. **Never omit sections.** Write "nothing to report" in any section with no findings.
4. **Quote the evidence.** For every Critical or Major finding, quote the conflicting text
   verbatim from both sources (with file path and approximate line context).
5. **Static scope only.** The review set is `docs/**`, `.claude/skills/**`, `.claude/agents/**`,
   `AGENTS.md` and `HARNESS.md`; Check 4 additionally scans `CLAUDE.md` commits, per Step 1.b. Two
   reads under `applications/**` are permitted, both by a named check and for nothing else: the
   `### {N}. {Aggregate}` header list in `plan.md`, for Check 4 (Step 6), and
   `applications/*/*-domain-model.md`, for Check 5 (Step 6.5). No retros, no reviews, no harness
   log, no generated source. Empirical evaluation of a completed run belongs to
   `/harness-retrospective`.
6. **Running during a generation run is expected.** Unchecked `- [ ]` boxes in a `plan.md` are not a
   precondition failure - this skill is the aggregate-boundary checkpoint, in both self-healing modes
   (`AGENTS.md` § "Harness evolution"). Never halt on them.
7. **Report, do not repair.** The findings are for a human or a later session to act on. This skill
   writes exactly one file. A Critical finding does not license fixing the artifact here.
8. **One invocation covers all artifacts.** Do not scope to a single aggregate or session.
9. **No emojis. Terse and specific.** File paths, section names, quoted snippets — no fluff.
