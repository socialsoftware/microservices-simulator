---
name: harness-retrospective
description: End-of-run empirical evaluation of the self-healing harness. Reads a completed run's harness-log.md, all retros and all review reports, measures whether harness edits converged, re-judges every Type 1 edit the agent made without asking, and produces a prioritised gap list. Runs once, after a run is finished. No arguments. Writes docs/reviews/harness-retro-{app-name}-{YYYY-MM-DD}.md.
argument-hint: "(no arguments)"
---

# Harness Retrospective

End-of-run evaluation of the harness against the evidence a completed generation run produced:
`applications/{app-name}/harness-log.md`, every file under `retros/`, every file under `reviews/`,
and the run's own `harness:` commits.

The harness is self-healing (`AGENTS.md` § "Harness evolution"), so this skill answers three
questions rather than one:

1. **Did the edits converge?** Harness repairs concentrated in the early aggregates mean the harness
   absorbed what it was missing. Repairs at a flat rate to the last aggregate mean a structural gap
   the next application will pay for again.
2. **Were the Type 1 edits legitimate?** Type 1 is fixed unilaterally, with no human gate. The
   honest objection to that regime is that an agent will ratify its own guess by editing the doc it
   disagreed with. Every Type 1 edit is therefore re-judged here, after the fact, against the
   evidence in the commit.
3. **What does the harness still lack?** The `declined` and `deferred` rows, plus any gap the run
   surfaced and nobody fixed.

It writes a prioritised gap list with a proposed fix per gap. It does not apply any fix.

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

### 3.a — Harness log

`applications/{app-name}/harness-log.md`. Read every row. Hold the full table: `#`, `Session`,
`Type`, `Artifact`, `Problem`, `Outcome`, `Ref`. The schema is defined in
`.claude/skills/_shared/conventions.md` § "Harness log".

Check the log's own integrity while reading and record any of these as a finding in the run
summary: gaps or duplicates in the `#` sequence; rows with a `Type` outside `1` / `2` / `2-fw`; rows
with an `Outcome` outside `fixed` / `declined` / `deferred`; `fixed` rows with an empty or
unresolvable `Ref`; any row whose `Type` is `1` but whose `Artifact` is under `simulator/`, which
the gates forbid; and rows whose `Artifact` is not a path under `docs/`, `.claude/skills/` or
`simulator/` (implementation defects misfiled as friction — dismiss them in Step 9, do not count
them as harness gaps).

### 3.a.i — The harness commits

The log's `Ref` column points at commits rather than restating diffs, so the diffs must be read
here:

```bash
cd "$(git rev-parse --show-toplevel)"
git log --oneline "$(git merge-base HEAD master)"..HEAD -- docs .claude/skills
```

That listing is the run's complete harness delta. Read `git show {Ref}` for every `fixed` row. Two
cross-checks: every `fixed` row's `Ref` must appear in the listing, and every commit in the listing
must be referenced by some row. A `harness:` commit with no log row is an unrecorded edit and a
finding in its own right.

### 3.b — Retros

`find applications/{app-name}/retros -name "*.md" | sort`

Read each. Take the narrative sections and the `## Harness Changes` sub-table
(`Row # / Type / Outcome / harness: commit`), which is the session's own account of what it
repaired. Reconcile it against the log: a retro naming a row the log does not carry, or a log row
whose session's retro is silent about it, is a finding. Retros no longer carry an Action Items
table; if one appears, the session used a stale skill version — record that as a finding.

### 3.c — Review reports

`find applications/{app-name}/reviews -name "*.md" | sort`

Read each. Take the verdict, the Action Items table, and any statement that a doc or skill was
unclear. Implementation Action Items are **not** harness evidence; they enter this report only as
a count in the run summary, and only where a review explicitly attributes a defect to a harness
gap.

### 3.d — The artifacts under evaluation

For every distinct path named in the harness log's `Artifact` column, read that file's **current
content**. This is the cross-check in Step 5 and it must be a fresh read, not the version implied
by the row's `Problem` wording, and not the post-image of the commit in `Ref`.

### 3.e — Plan and build state

Read `applications/{app-name}/plan.md` for the session list and aggregate order. Run the test
suite following `.claude/skills/_shared/conventions.md` § "Run the test suite" and record the
observed `MAVEN_EXIT` and surefire totals. The build outcome is part of the run summary; a green
suite with heavy friction and a red suite with none are different results.

---

## Step 4: Group Rows by Artifact

Build one row per distinct `Artifact` path, aggregating every harness-log row that names it:

| Artifact | Rows | T1 | T2 | 2-fw | fixed | declined | deferred | What the friction was about |
|----------|------|----|----|------|-------|----------|----------|-----------------------------|

Sort by row count descending, then by `deferred` + `declined` count descending. The artifact at the
top of this table is the harness's largest single weakness for this run — say so explicitly in the
report. An artifact with many rows but all of them `fixed` is a different result from one with few
rows all `deferred`: the first absorbed its lesson, the second is still owed one.

Then build the same aggregation by `Type`. The shape of the answer differs by type: a run dominated
by Type 1 means the harness contained demonstrable errors and the agent cleared them, a run
dominated by Type 2 means the harness was silent where it needed to speak and the human had to
supply the design, and any `2-fw` row is individually significant regardless of count because it
means an agent believed the framework itself was at fault.

---

## Step 5: Cross-Check Each Row Against Current Content

For every harness-log row, compare its `Problem` text against the current content of its `Artifact`
read in Step 3.d, and classify. `fixed` rows and non-`fixed` rows are judged against different
questions.

For rows with `Outcome` = `fixed`, the question is whether the fix actually holds:

| # | Artifact | Ref | Verdict | Evidence |
|---|----------|-----|---------|----------|

- **Fix holds** — the current file covers the problem, and the commit in `Ref` is what made it do
  so. Quote the covering text.
- **Fix incomplete** — the problem is partly addressed; a later session or another file still
  carries the same gap. Name what remains.
- **Fix reverted or superseded** — the current file no longer contains the change. Name the commit
  that removed it.
- **Fix wrong** — the change made the artifact contradict the framework, another doc, or a stated
  principle. This is the most serious verdict this skill can return: it means the unilateral gate
  let a wrong edit through, and it is the evidence Step 7 exists to weigh.

For rows with `Outcome` = `declined` or `deferred`, the question is whether the gap is still open:

| # | Artifact | Outcome | Verdict | Evidence |
|---|----------|---------|---------|----------|

- **Confirmed** — the gap the row describes is still present in the file as it stands today.
- **Already closed** — the file has since changed and now covers it (a later session's fix, or an
  unrelated edit). Quote the text that now covers it.
- **Misattributed** — the named artifact does cover it; the friction was caused by something else
  (another file, a spec ambiguity, or an agent error). Name what.
- **Not a harness gap** — the row records an implementation defect, a spec defect, or an agent
  mistake the harness could not reasonably have prevented.

**Confirmed**, **Fix incomplete**, **Fix reverted or superseded** and **Fix wrong** rows become gaps
in Step 9. Everything else is listed with its reason in the dismissed section — never dropped
silently.

---

## Step 6: Convergence

Order the harness-log rows by their `Session` field in plan.md session order (`0`, `1`, `2.1.a` …
`3.N`, `4.N`) and count rows per aggregate, split by `Type`:

| Aggregate / phase | Sessions | Type 1 | Type 2 | 2-fw | Total |
|-------------------|----------|--------|--------|------|-------|

The question this answers: did harness edits **decline** as aggregates progressed, or stay **flat**?

- **Declining** is the result the run is trying to produce. It means the harness absorbed each
  lesson and later aggregates traversed it cleanly. Report the aggregate at which the count reaches
  and stays at zero, if it does.
- **Flat or rising** means a structural gap: every aggregate hits it independently, so the harness
  will keep paying it on the next application. Name the artifacts that recur across aggregates —
  those are the structural ones, as distinct from a one-off that happened late.

Read the types separately. Type 1 declining while Type 2 stays flat is a specific and important
result: the harness's factual errors were cleared but its silences were not, because a silence is
only ever filled by a human decision. Say so if that is what the data shows.

State which pattern the data shows, with the per-aggregate counts that support it. If the run is
too short for the distinction to mean anything, say that instead of inventing a trend. A run with
zero rows in the last two aggregates and fewer than three aggregates before them is too short.

---

## Step 7: Post-Hoc Justification of the Type 1 Edits

Type 1 edits were made unilaterally, mid-session, with no human gate. This step is the audit that
regime is owed, and its output belongs in the run's written evaluation verbatim.

For every `Type` = `1` row, read the diff in its `Ref` (Step 3.a.i) and judge it against the
definition in `AGENTS.md` § "Harness evolution": a Type 1 fix requires a contradiction the agent
could **demonstrate mechanically** — a failing build, a missing symbol, two skills prescribing
different things.

| # | Session | Artifact | Claimed contradiction | Demonstrable? | Verdict |
|---|---------|----------|----------------------|---------------|---------|

Verdicts:

- **Sound** — a real contradiction, and the diff fixes exactly it. The evidence is checkable now:
  the named symbol is genuinely absent from `simulator/`, or the two named files genuinely
  disagreed at that commit.
- **Sound but over-broad** — the contradiction was real, but the diff changed more than the
  contradiction required. Name what else it changed. This is the common failure mode and it is how
  a design preference rides in on the back of a typo fix.
- **Ratified a guess** — no demonstrable contradiction. The harness was silent or merely
  inconvenient, the agent formed a view, and edited the doc to agree with the code it had already
  written or was about to write. This should have been Type 2 and should have halted. State what
  the human would plausibly have decided instead.
- **Misclassified** — the row is really Type 2 or `2-fw` on its face (for example, an edit to
  `simulator/`, which has no Type 1 path at all).

Count the verdicts and state the ratio in the report. That ratio, not the raw edit count, is what
says whether the unilateral gate was safe on this run. Do not soften a **Ratified a guess** verdict:
finding none is a claim, and finding some is what makes the section worth reading.

---

## Step 8: Neutral-Domain Compliance

`.claude/skills/_shared/conventions.md` § "Neutral domain" forbids harness fixes from naming an
entity of the application being generated. The run's fixes were authored while looking at that
application, so the rule needs an end-of-run measurement and not only the per-boundary one.

Run the check exactly as `.claude/skills/review-artifacts/SKILL.md` § "Step 6: Check 4 — Neutral
Domain" defines it — that skill owns the procedure; do not restate it here. Scope it to the whole
run: the base commit is `git merge-base HEAD master`, so the diff covers every harness commit of
the run rather than one aggregate's worth.

| File | Added line | Noun | harness-log row (if any) |
|------|-----------|------|--------------------------|

Report the count of violations and, separately, how many of them the aggregate-boundary
`/review-artifacts` runs had already flagged. A violation that a boundary check caught and nobody
acted on is a process finding, not a rule finding — say which kind each one is.

---

## Step 9: Produce the Prioritised Gap List

One entry per gap-producing row from Step 5, or one per group of rows that share a single root
cause. For each: the artifact, the gap in one sentence, the harness-log row numbers that evidence
it, the Step 5 verdict, and a **proposed fix** concrete enough to act on — which section of which
file, and what it should say.

Priority ordering:

1. Any **Fix wrong** row, and any Step 7 **Ratified a guess** verdict. The harness is now actively
   misleading, and it was made so without review.
2. Any gap evidenced by rows from more than one aggregate (structural, not incidental).
3. Any `2-fw` row still open — a framework question nobody answered.
4. Everything else.

Do not apply any fix. This skill writes a report; the fixes are applied between runs, on master,
by a later session.

---

## Step 10: Write the Report

Run `mkdir -p docs/reviews` (no-op if it exists). Write `{report-file}` using the template below.
Never omit a section — write "nothing to report" where a section produced no findings.

```markdown
# Harness Retrospective — {app-name}

**App:** {app-name}
**Date:** {retro-date}
**Harness-log rows:** {count} (Type 1 {n} / Type 2 {n} / 2-fw {n})
**Sessions with friction:** {count} of {total sessions in plan.md}
**Harness commits:** {count}
**Verdict:** Harness converged | Harness converged with gaps | Harness did not converge

> **Harness converged** = repairs decline toward zero across the run, no Step 7 **Ratified a guess**
>   verdict, no open `2-fw` row, build green.
> **Harness converged with gaps** = repairs decline but confirmed gaps remain open, and no session
>   was blocked by one.
> **Harness did not converge** = a flat or rising repair rate, or any **Ratified a guess** or
>   **Fix wrong** verdict, or a red build attributable to the harness.

---

## Run Summary

| | |
|---|---|
| Sessions executed | {count} |
| Harness-log rows | {count} (T1 {n} / T2 {n} / 2-fw {n}) |
| Outcomes | fixed {n} / declined {n} / deferred {n} |
| `harness:` commits on the run | {count} |
| Type 2 halts | {count} |
| Build outcome | MAVEN_EXIT={n}, tests={n} failures={n} errors={n} |
| Implementation Action Items across reviews | {count} |

(One paragraph: what this run says about the harness overall.)

Harness-log integrity: (numbering, `Type`/`Outcome` values, `Ref` resolvability, commits with no
row, misfiled rows — or "clean".)

---

## Rows by Artifact

| Artifact | Rows | T1 | T2 | 2-fw | fixed | declined | deferred | What the friction was about |
|----------|------|----|----|------|-------|----------|----------|-----------------------------|

## Rows by Type

| Type | Rows | Notes |
|------|------|-------|

---

## Convergence

| Aggregate / phase | Sessions | Type 1 | Type 2 | 2-fw | Total |
|-------------------|----------|--------|--------|------|-------|

**Pattern:** declining | flat | rising | too short to tell — with the reasoning.

(If declining: the aggregate at which the count reaches and stays at zero. If flat or rising: the
artifacts that recur across aggregates, which are the structural gaps.)

---

## Post-Hoc Justification of the Type 1 Edits

(This section goes into the run's written evaluation verbatim. It is the audit owed by a gate that
lets an agent edit the harness without asking.)

| # | Session | Artifact | Claimed contradiction | Demonstrable? | Verdict |
|---|---------|----------|----------------------|---------------|---------|

**Verdict ratio:** Sound {n} / Sound but over-broad {n} / Ratified a guess {n} / Misclassified {n}

(One paragraph: what that ratio says about whether the unilateral gate was safe on this run. Do not
soften a **Ratified a guess** verdict.)

---

## Neutral-Domain Compliance

| File | Added line | Noun | harness-log row (if any) |
|------|-----------|------|--------------------------|

**Violations:** {count}, of which {n} were already flagged by an aggregate-boundary
`/review-artifacts` run and not acted on (process finding) and {n} were not caught at all (rule
finding).

---

## Confirmed Gaps

### G1 — {artifact} — {one-line gap}

**Evidence:** harness-log rows {#, #}
**Step 5 verdict:** Confirmed | Fix incomplete | Fix reverted or superseded | Fix wrong
**Proposed fix:** {which section of which file, and what it should say}

(Repeat per gap, numbered G1, G2, …, in the Step 9 priority order.)

---

## Dismissed Rows

| # | Artifact | Verdict | Reason |
|---|----------|---------|--------|

---

## Type 2 Halts

| # | Session | What the harness did not settle | What the human decided | Was the harness then fixed? |
|---|---------|--------------------------------|------------------------|-----------------------------|

---

## Recorded Limitations

(Carried from the run's plan — the confounds that constrain what this retrospective can claim.
State them here even when they are inconvenient.)
```

---

## Step 11: Print Summary to Conversation

Output to the conversation (not to the report file):

1. Absolute path to `{report-file}`
2. Verdict with one-sentence justification
3. The convergence pattern: declining, flat or rising, and the evidence for it
4. The Step 7 verdict ratio, and every **Ratified a guess** row verbatim
5. The artifact that generated the most rows, with its count
6. Every confirmed gap whose Step 5 verdict is **Fix wrong**, verbatim
7. Counts: confirmed gaps, dismissed rows, Type 2 halts, neutral-domain violations
8. Build outcome: observed `MAVEN_EXIT` and surefire totals

---

## Hard Rules

1. **Read-only except for the report.** This skill writes exactly one file:
   `docs/reviews/harness-retro-{app-name}-{retro-date}.md`. It never edits a doc, a skill, the
   harness log, a retro, a review, or any source file — including the gaps it identifies. This is
   the one skill with no Type 1 fast path of its own: it is judging the fixes, so it may not also be
   making them. Fixes are applied between runs by a later session.
2. **Completed runs only.** Halt on any unchecked `- [ ]` in `plan.md` (Step 2). No partial reports.
3. **Every claim cites evidence.** A harness-log row number (`#7`), a commit sha, a repo-relative
   file path, or a quoted line. A statement about the harness with no citation does not go in the
   report.
4. **Cross-check before confirming.** No gap is Confirmed without a fresh read of the artifact's
   current content (Step 3.d). A harness-log row is a record of what an agent believed at the time,
   not a standing fact — and a `fixed` row is a record of what it believed it had repaired.
5. **No silent dismissals.** Every harness-log row appears in the report exactly once, either as
   evidence for a confirmed gap or as a dismissed row with a stated reason.
6. **Do not propose fixes for the generated application.** Implementation defects belong to the
   Phase 3 and Phase 4 reports. If a review's Action Item is unaddressed, note it in the run
   summary and stop there.
7. **Report what the evidence shows.** A run with little friction is a valid result and so is a run
   that went badly. Do not soften a bad outcome and do not manufacture gaps to fill the section.
8. **No emojis. Terse and specific.** File paths, section names, row numbers, quoted snippets.
