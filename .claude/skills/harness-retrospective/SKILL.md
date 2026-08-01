---
name: harness-retrospective
description: End-of-run empirical evaluation of the harness. Reads a completed run's friction-log.md, all retros and all review reports, groups friction by the docs/ or .claude/skills/ artifact that caused it, and produces a prioritised harness gap list. Runs once, after a run is finished. No arguments. Writes docs/reviews/harness-retro-{app-name}-{YYYY-MM-DD}.md.
argument-hint: "(no arguments)"
---

# Harness Retrospective

End-of-run evaluation of the harness against the evidence a completed generation run produced:
`applications/{app-name}/friction-log.md`, every file under `retros/`, and every file under
`reviews/`. It answers one question — **which parts of `docs/` and `.claude/skills/` failed to
guide the agent, and how** — and writes a prioritised gap list with a proposed fix per gap.

One invocation per completed run. No arguments.

**What it is not:** it does not check the harness for internal consistency (dangling paths,
P1-P4 drift, R1-R8 coverage). That is `/review-artifacts`, a static pre-flight check whose ground
truth is the harness files themselves. This skill's ground truth is empirical: what actually went
wrong while the harness was being used. The two are deliberately separate and neither subsumes the
other.

It also does not propose fixes for the **generated application**. Implementation defects belong to
the Phase 3 and Phase 4 reports (`/review-aggregate`, `/adversarial-review-aggregate`,
`/review-tests`) and their own Action Items tables.

---

## Step 0: Anchor to the repository root

Before Step 1, read `.claude/skills/_shared/conventions.md` and follow "Anchor to the repository
root". Do not run any command until you have.

## Step 1: Resolve Context

Read `.claude/skills/_shared/conventions.md` § "Resolve app context". Derive `{app-name}`.

Additionally derive:

```
{retro-date}   = today in YYYY-MM-DD
{report-file}  = docs/reviews/harness-retro-{app-name}-{retro-date}.md
```

If a report with that name already exists, append `-2`, `-3`, etc. rather than overwriting.

## Step 2: Precondition — the run must be complete

Read `applications/{app-name}/plan.md` and count unchecked boxes:

```bash
grep -c -- "- \[ \]" applications/{app-name}/plan.md
```

If the count is non-zero, **halt** and report:

> "Run not complete: {count} unchecked boxes remain in applications/{app-name}/plan.md.
> /harness-retrospective evaluates finished runs only."

Do not produce a partial report. A retrospective written mid-run measures a run that is still
being influenced by it.

---

## Step 3: Read the Evidence

Read every file below in full. Read in parallel where possible. An absent file is itself a
finding — record it, do not skip past it silently.

### 3.a — Friction log

`applications/{app-name}/friction-log.md`. Read every row. Hold the full table: `#`, `Session`,
`Severity`, `Category`, `Artifact`, `Friction`. The schema is defined in
`.claude/skills/_shared/conventions.md` § "Friction log".

Check the log's own integrity while reading and record any of these as a finding in the run
summary: gaps or duplicates in the `#` sequence, rows with a `Category` outside the defined set,
rows whose `Artifact` is not a path under `docs/` or `.claude/skills/` (implementation defects
that were misfiled as friction — dismiss them in Step 7, do not count them as harness gaps).

### 3.b — Retros

`find applications/{app-name}/retros -name "*.md" | sort`

Read each. Take the narrative sections and the `## Friction Recorded` row-number list. Retros no
longer carry an Action Items table; if one appears, the session used a stale skill version —
record that as a finding.

### 3.c — Review reports

`find applications/{app-name}/reviews -name "*.md" | sort`

Read each. Take the verdict, the Action Items table, and any statement that a doc or skill was
unclear. Implementation Action Items are **not** harness evidence; they enter this report only as
a count in the run summary, and only where a review explicitly attributes a defect to a harness
gap.

### 3.d — The artifacts under evaluation

For every distinct path named in the friction log's `Artifact` column, read that file's **current
content**. This is the cross-check in Step 5 and it must be a fresh read, not the version implied
by the friction row's wording.

### 3.e — Plan and build state

Read `applications/{app-name}/plan.md` for the session list and aggregate order. Run the test
suite following `.claude/skills/_shared/conventions.md` § "Run the test suite" and record the
observed `MAVEN_EXIT` and surefire totals. The build outcome is part of the run summary; a green
suite with heavy friction and a red suite with none are different results.

---

## Step 4: Group Friction by Artifact

Build one row per distinct `Artifact` path, aggregating every friction row that names it:

| Artifact | Rows | High | Med | Low | Categories | What the friction was about |
|----------|------|------|-----|-----|------------|-----------------------------|

Sort by row count descending, then by High count descending. The artifact at the top of this table
is the harness's largest single weakness for this run — say so explicitly in the report.

Then build the same aggregation by `Category` (`doc-gap` / `skill-gap` / `framework` / `halt` /
`deviation`), because the shape of the answer differs: a run dominated by `doc-gap` means the
concept docs under-specify, one dominated by `skill-gap` means the procedures do, and any
`framework` or `deviation` row is individually significant regardless of count.

---

## Step 5: Cross-Check Each Row Against Current Content

For every friction row, compare its `Friction` text against the current content of its `Artifact`
read in Step 3.d, and classify:

| # | Artifact | Verdict | Evidence |
|---|----------|---------|----------|

Verdicts:

- **Confirmed** — the gap the row describes is still present in the file as it stands today.
- **Already fixed** — the file has since changed and now covers it (an unrelated edit, or a
  between-runs fix). Quote the text that now covers it.
- **Misattributed** — the named artifact does cover it; the friction was caused by something else
  (another file, a spec ambiguity, or an agent error). Name what.
- **Not a harness gap** — the row records an implementation defect, a spec defect, or an agent
  mistake the harness could not reasonably have prevented.

Only **Confirmed** rows become gaps in Step 7. Everything else is listed with its reason in the
dismissed section — never dropped silently.

---

## Step 6: Friction Over Time

Order the friction rows by their `Session` field in plan.md session order (`0`, `1`, `2.1.a` …
`3.N`, `4.N`) and count rows per aggregate.

The question this answers: did friction **decline** as aggregates progressed, or stay **flat**?

- Declining suggests a learning curve — the early aggregates paid a one-off cost that later ones
  did not. Less urgent to fix.
- Flat or rising suggests a structural gap — every session hits it independently, so the harness
  will keep paying it on the next application. More urgent.

State which pattern the data shows, with the per-aggregate counts that support it. If the run is
too short for the distinction to mean anything, say that instead of inventing a trend.

---

## Step 7: Produce the Prioritised Gap List

One entry per **Confirmed** row, or one per group of rows that share a single root cause. For each:
the artifact, the gap in one sentence, the friction row numbers that evidence it, the severity
carried from those rows, and a **proposed fix** concrete enough to act on — which section of which
file, and what it should say.

Priority ordering:

1. Any row whose severity is `High` (produced or nearly produced wrong output).
2. Any gap evidenced by rows from more than one session (structural, not incidental).
3. Everything else, by severity.

Do not apply any fix. This skill writes a report; the fixes are applied between runs, on master,
by a later session.

---

## Step 8: Write the Report

Run `mkdir -p docs/reviews` (no-op if it exists). Write `{report-file}` using the template below.
Never omit a section — write "nothing to report" where a section produced no findings.

```markdown
# Harness Retrospective — {app-name}

**App:** {app-name}
**Date:** {retro-date}
**Friction rows:** {count}
**Sessions with friction:** {count} of {total sessions in plan.md}
**Verdict:** Harness held | Harness held with gaps | Harness did not hold

> **Harness held** = no High-severity confirmed gaps, no halts, no deviations, build green.
> **Harness held with gaps** = confirmed gaps exist but no session was blocked and the build is green.
> **Harness did not hold** = any halt caused by a harness gap, any deviation, or a red build
>   attributable to the harness.

---

## Run Summary

| | |
|---|---|
| Sessions executed | {count} |
| Friction rows | {count} (High {n} / Med {n} / Low {n}) |
| Halts | {count} |
| Deviations (freeze broken) | {count} |
| Build outcome | MAVEN_EXIT={n}, tests={n} failures={n} errors={n} |
| Implementation Action Items across reviews | {count} |

(One paragraph: what this run says about the harness overall.)

Friction-log integrity: (numbering, categories, misfiled rows — or "clean".)

---

## Friction by Artifact

| Artifact | Rows | High | Med | Low | Categories | What the friction was about |
|----------|------|------|-----|-----|------------|-----------------------------|

## Friction by Category

| Category | Rows | Notes |
|----------|------|-------|

---

## Friction Over Time

| Aggregate / phase | Sessions | Friction rows |
|-------------------|----------|---------------|

**Pattern:** declining | flat | rising | too short to tell — with the reasoning.

---

## Confirmed Gaps

### G1 — {artifact} — {one-line gap}

**Evidence:** friction rows {#, #}
**Severity:** High | Med | Low
**Proposed fix:** {which section of which file, and what it should say}

(Repeat per gap, numbered G1, G2, …, in priority order.)

---

## Dismissed Rows

| # | Artifact | Verdict | Reason |
|---|----------|---------|--------|

---

## Halts and Deviations

| # | Session | What happened | What unblocked it | Follow-up |
|---|---------|---------------|-------------------|-----------|

---

## Recorded Limitations

(Carried from the run's plan — the confounds that constrain what this retrospective can claim.
State them here even when they are inconvenient.)
```

---

## Step 9: Print Summary to Conversation

Output to the conversation (not to the report file):

1. Absolute path to `{report-file}`
2. Verdict with one-sentence justification
3. The artifact that generated the most friction, with its row count
4. Every High-severity confirmed gap, verbatim from the Confirmed Gaps section
5. Counts: confirmed gaps, dismissed rows, halts, deviations
6. Build outcome: observed `MAVEN_EXIT` and surefire totals

---

## Hard Rules

1. **Read-only except for the report.** This skill writes exactly one file:
   `docs/reviews/harness-retro-{app-name}-{retro-date}.md`. It never edits a doc, a skill, the
   friction log, a retro, a review, or any source file — including the gaps it identifies. Fixes
   are applied between runs by a later session.
2. **Completed runs only.** Halt on any unchecked `- [ ]` in `plan.md` (Step 2). No partial reports.
3. **Every claim cites evidence.** A friction row number (`#7`), a repo-relative file path, or a
   quoted line. A statement about the harness with no citation does not go in the report.
4. **Cross-check before confirming.** No gap is Confirmed without a fresh read of the artifact's
   current content (Step 3.d). A friction row is a record of what an agent believed at the time,
   not a standing fact.
5. **No silent dismissals.** Every friction row appears in the report exactly once, either as
   evidence for a confirmed gap or as a dismissed row with a stated reason.
6. **Do not propose fixes for the generated application.** Implementation defects belong to the
   Phase 3 and Phase 4 reports. If a review's Action Item is unaddressed, note it in the run
   summary and stop there.
7. **Report what the evidence shows.** A run with little friction is a valid result and so is a run
   that went badly. Do not soften a bad outcome and do not manufacture gaps to fill the section.
8. **No emojis. Terse and specific.** File paths, section names, row numbers, quoted snippets.
