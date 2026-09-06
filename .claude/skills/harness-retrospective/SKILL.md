---
name: harness-retrospective
description: End-of-run empirical evaluation of the self-healing harness. Reads a completed run's harness-log.md and all retros, measures whether harness edits converged, re-judges every Type 1 edit the agent made without asking, and produces a prioritised gap list. Runs once, after a run is finished. No arguments. Writes reviews/harness-retro-{app-name}-{YYYY-MM-DD}.md.
argument-hint: "(no arguments)"
---

# Harness Retrospective

End-of-run evaluation of the harness against the evidence a completed generation run produced:
`applications/{app-name}/harness-log.md`, every file under `retros/`, and the run's own `harness:`
commits.

**What this skill produces depends on the run's self-healing mode** (`AGENTS.md`
§ "Harness evolution"), read in Step 1 from the `harness-log.md` header:

| Mode | Status | Produces |
|------|--------|----------|
| **ON** | Obligatory. A self-healing run is not finished until it has been evaluated. | Everything below: convergence, post-hoc justification of the Type 1 edits, and the gap list. |
| **OFF** | Recommended, not obligatory. | The prioritised gap list only. Steps 6 and 7 are skipped - see § "Under self-healing OFF". |

Under ON the harness repaired itself mid-run, so this skill answers three questions rather than one:

1. **Did the edits converge?** Harness repairs concentrated in the early aggregates mean the harness
   absorbed what it was missing. Repairs at a flat rate to the last aggregate mean a structural gap
   the next application will pay for again.
2. **Were the Type 1 edits legitimate?** Type 1 is fixed unilaterally, with no human gate. The
   honest objection to that regime is that an agent will ratify its own guess by editing the doc it
   disagreed with. Every Type 1 edit is therefore re-judged here, after the fact, against the
   evidence in the commit.
3. **What does the harness still lack?** The `declined` rows and the **open** `deferred` rows (a
   `deferred` row closed by a later row per `.claude/skills/_shared/conventions.md` § "Harness log"
   is convergence evidence, not a standing gap), plus any gap the run surfaced and nobody fixed.

It writes a prioritised gap list with a proposed fix per gap. It does not apply any fix.

One invocation per completed run. No arguments.

### Under self-healing OFF

A run under OFF made no unilateral harness edit and produced no `harness:` commit, so questions 1 and
2 have no evidence to answer them and are not asked: there is nothing to converge and nothing to
re-judge. Question 3 is the whole skill. Concretely:

- **State the mode in the first line of the report**, before anything else. A reader who mistakes an
  OFF report for an ON one reads "no harness edits" as convergence when it means the mechanism was
  switched off.
- **Skip Step 6 (Convergence)** and **Step 7 (Post-Hoc Justification of the Type 1 Edits)**, and drop
  their report sections along with the `Verdict:` line, whose three values all describe convergence.
- **Step 8 (Neutral-Domain Compliance)** has no added harness lines to scan. Report that it scanned
  zero lines **because the mode withheld the edits**, not "clean" - a clean verdict over nothing
  scanned is the failure mode `.claude/skills/_shared/conventions.md` § "Commands whose output feeds
  a verdict" warns about.
- Everything else runs unchanged: the evidence read, the per-artifact grouping, the row cross-check,
  the Type 2 halts, and the prioritised gap list. Type 1 rows are `deferred` under OFF, so they enter
  the gap list as open gaps rather than as edits to audit - which is the point: OFF turns every Type 1
  into a finding for a human instead of a fix by an agent.

**What it is not:** it does not check the harness for internal consistency (dangling paths,
P1-P4 drift, R1-R8 coverage). That is `/review-artifacts`, a static pre-flight check whose ground
truth is the harness files themselves. This skill's ground truth is empirical: what actually went
wrong while the harness was being used. The two are deliberately separate and neither subsumes the
other.

It also does not propose fixes for the **generated application**. Implementation defects in the
generated application are out of scope for this skill.

---

## Step 0: Anchor to the repository root

Before Step 1, read `.claude/skills/_shared/conventions.md` and follow "Anchor to the repository
root". Do not run any command until you have.

## Step 1: Resolve Context

Read `.claude/skills/_shared/conventions.md` § "Resolve app context". Derive `{app-name}`.

Additionally derive:

```
{retro-date}   = today in YYYY-MM-DD
{report-file}  = reviews/harness-retro-{app-name}-{retro-date}.md
```

If a report with that name already exists, append `-2`, `-3`, etc. rather than overwriting.

**Read the run's self-healing mode** from the `**Self-healing:**` line in the header of
`applications/{app-name}/harness-log.md`: `on` or `off`, with a missing, unreadable or unrecognised
value meaning `off` (`.claude/skills/_shared/conventions.md` § "Harness log"). It decides which steps
run - see § "Under self-healing OFF" above - and it is the first line of the report. There is no
invocation flag here: this skill evaluates a finished run, and the mode that run executed under is a
fact about it, not a choice available now.

## Step 2: Precondition — the run must be complete

Read `applications/{app-name}/plan.md` and count unchecked boxes. The count decides whether the
retrospective runs at all, so it comes from `python3` rather than `grep` — see
`.claude/skills/_shared/conventions.md` § "Commands whose output feeds a verdict". Substitute
`{app-name}` before running:

```bash
cd "$(git rev-parse --show-toplevel)"
python3 - <<'EOF'
import pathlib, sys

plan = pathlib.Path("applications/{app-name}/plan.md")
if not plan.is_file():
    sys.exit(f"{plan} does not exist - check the app name")

lines = plan.read_text().splitlines()
unchecked = [(n, l.strip()) for n, l in enumerate(lines, 1) if "- [ ]" in l]
print(f"scanned {len(lines)} lines of {plan}")
print(f"unchecked={len(unchecked)}")
for n, l in unchecked:
    print(f"  {plan}:{n}: {l}")
EOF
```

If the script errors, or prints `scanned 0 lines`, that is a failed check, not a complete run — fix
the app name and re-run. If `unchecked` is non-zero, **halt** and report:

> "Run not complete: {count} unchecked boxes remain in applications/{app-name}/plan.md.
> /harness-retrospective evaluates finished runs only."

Do not produce a partial report. A retrospective written mid-run measures a run that is still
being influenced by it.

---

## Step 3: Read the Evidence

Every file below is read in full during the retrospective, but not all of them by you: 3.a, 3.a.i and
3.d are yours, while the bulk reading in 3.b and 3.c is delegated in Step 4.b. Each subsection says
which. An absent file is itself a finding - record it, do not skip past it silently.

### 3.a — Harness log

`applications/{app-name}/harness-log.md`. Read every row. Hold the full table: `#`, `Session`,
`Type`, `Artifact`, `Problem`, `Outcome`, `Ref`. The schema is defined in
`.claude/skills/_shared/conventions.md` § "Harness log".

Check the log's own integrity while reading and record any of these as a finding in the run
summary: gaps or duplicates in the `#` sequence; rows with a `Type` outside `1` / `2` / `2-fw`; rows
with an `Outcome` outside `fixed` / `declined` / `deferred`; `fixed` rows with an empty or
unresolvable `Ref`; any row whose `Type` is `1` but whose `Artifact` is under `simulator/`, which
the gates forbid; and rows whose `Artifact` is not a path under `docs/`, `.claude/`,
`simulator/`, or one of the root files `AGENTS.md`, `CLAUDE.md` and `HARNESS.md` (implementation
defects misfiled as friction — dismiss them in Step 9, do not count them as harness gaps).

**Compound `Type` values.** `.claude/skills/_shared/conventions.md` § "Harness log" admits `1`, `2`
and `2-fw` only, so a row logging a bundle of findings under a compound value (`1+2`) is an integrity
finding - record it. The log is append-only, so the row stands and every later step must still handle
it: **a compound row counts once in the row total and once in each `Type` bucket it names**, and its
Type 1 half is audited in Step 7 like any other Type 1 edit. Say so wherever the counts are reported;
per-type counts summing to more than the row total is correct in that case and confusing without the
sentence.

### 3.a.i — The harness commits

The log's `Ref` column points at commits rather than restating diffs, so the diffs must be read
here. The listing feeds two cross-checks and every Step 7 verdict, so it is derived with `python3 -`
and `subprocess` rather than bare `git` - see `.claude/skills/_shared/conventions.md`
§ "Commands whose output feeds a verdict". The `git` helper below is the same one
`.claude/skills/review-artifacts/SKILL.md` § "Step 6: Check 4 — Neutral Domain" uses.

```bash
cd "$(git rev-parse --show-toplevel)"
python3 - <<'EOF'
import subprocess

TREES = ["docs", ".claude/skills", ".claude/agents", "AGENTS.md", "CLAUDE.md", "HARNESS.md"]
PATHSPEC = TREES + [":(exclude)reviews"]

def git(*args):
    return subprocess.run(["git", *args], capture_output=True, text=True, check=True).stdout

base = git("merge-base", "HEAD", "master").strip()
rng = f"{base}..HEAD"

harness = git("log", "--oneline", "--grep=^harness:", rng, "--", *PATHSPEC).splitlines()
everything = git("log", "--oneline", rng, "--", *PATHSPEC).splitlines()
other = [l for l in everything if l not in harness]

print(f"BASE={base}")
print(f"harness: commits={len(harness)}")
for l in harness:
    print(f"  {l}")
print(f"\nharness-path commits without a harness: prefix={len(other)}")
for l in other:
    print(f"  {l}")
EOF
```

The `harness:` listing is the run's complete harness delta. Read the diff of every `fixed` row's
`Ref` the same way - `git("show", "-M", ref)` inside a `python3 -` script, never a bare `git show`,
for the same reason.

Two cross-checks against the `harness:` listing: every `fixed` row's `Ref` must appear in it, and
every commit in it must be referenced by some row. A `harness:` commit with no log row is an
unrecorded edit and a finding in its own right.

**A `Ref` missing from the listing is not automatically unresolvable.** A rebase or amend anywhere in
the run rewrites shas, and the log is append-only, so a row keeps the sha the commit had when it was
written. Three cases, and they are different findings:

- the sha resolves and is an ancestor of `HEAD` - normal;
- the sha resolves but `git merge-base --is-ancestor {ref} HEAD` fails - the commit was rewritten.
  Find its replacement on the branch by identical subject, or by `git patch-id`, judge **that** diff,
  and record a stale-`Ref` integrity finding naming both shas. Do not report the row as unresolvable
  and do not skip its Step 7 audit;
- no replacement is found - genuinely unresolvable, and the row's Step 5 and Step 7 verdicts must say
  the evidence was unavailable rather than infer one.

Report the stale-sha count in the run summary. It measures how faithfully the log's `Ref` column
survived the run, which is a property of the harness, not of the application.

The second listing is a **different** finding class and must not be reported as unrecorded edits.
`reviews` is excluded by the pathspec, so what remains is a commit that changed a harness file
without the `harness:` prefix `AGENTS.md` § "Harness evolution" requires - a commit-convention
deviation. Report each one with its sha and subject, and say whether the edit it carries is recorded
by a log row.

### 3.b — Retros

`find applications/{app-name}/retros -name "*.md" | sort`

A run's retros are collectively large, so the reconciliation below is delegated to a batch of
`harness-retro-evidence` workers per Step 4.b. Read directly only the retros of the sessions whose
rows you end up judging yourself in Step 5.

Read each. Take the narrative sections and the `## Harness Changes` sub-table
(`Row # / Type / Outcome / harness: commit`), which is the session's own account of what it
repaired. Reconcile it against the log: a retro naming a row the log does not carry, or a log row
whose session's retro is silent about it, is a finding. Retros no longer carry an Action Items
table; if one appears, the session used a stale skill version — record that as a finding.

### 3.c — The artifacts under evaluation

For every distinct path named in the harness log's `Artifact` column, the file's **current content**
must be read: this is the cross-check in Step 5, and it must be a fresh read, not the version implied
by the row's `Problem` wording, and not the post-image of the commit in `Ref`.

These reads are what Step 4.b delegates. Do not perform them here - the artifact set of a full run
does not fit one context alongside the diffs and the retros. Read an artifact yourself only when
Step 5 requires you to re-judge one of its rows.

### 3.d — Plan and build state

Read `applications/{app-name}/plan.md` for the session list and aggregate order. Run the test
suite following `.claude/skills/_shared/conventions.md` § "Run the test suite" and record the
observed `MAVEN_EXIT` and surefire totals. The build outcome is part of the run summary; a green
suite with heavy friction and a red suite with none are different results.

This is the full clean suite over every aggregate of the run and takes a while. Start it before
spawning the Step 4.b workers so it runs alongside them. Its result is **recorded, not acted on** -
a red suite is reported as the run's outcome and never repaired here (Hard Rule 1).

---

## Step 4: Group Rows by Artifact

Build one row per distinct `Artifact` path, aggregating every harness-log row that names it. Resolve
`deferred` rows first by scanning the log for `Closes row {N}` references (per
`.claude/skills/_shared/conventions.md` § "Harness log"): a `deferred` row named by such a reference
is `deferred (closed)`; one named by none is `deferred (open)`.

| Artifact | Rows | T1 | T2 | 2-fw | fixed | declined | deferred (open) | deferred (closed) | What the friction was about |
|----------|------|----|----|------|-------|----------|------------------|--------------------|-----------------------------|

Sort by row count descending, then by `deferred (open)` + `declined` count descending. The artifact
at the top of this table is the harness's largest single weakness for this run — say so explicitly
in the report. An artifact with many rows but all of them `fixed` or `deferred (closed)` is a
different result from one with few rows all `deferred (open)`: the first absorbed its lesson, the
second is still owed one.

Then build the same aggregation by `Type`. A compound row counts in each bucket it names (Step 3.a).
The shape of the answer differs by type: a run dominated
by Type 1 means the harness contained demonstrable errors and the agent cleared them, a run
dominated by Type 2 means the harness was silent where it needed to speak and the human had to
supply the design, and any `2-fw` row is individually significant regardless of count because it
means an agent believed the framework itself was at fault.

---

## Step 4.b: Gather the Evidence in Batches

A full run's evidence does not fit one context: the harness diffs, the retros and the current content
of every named artifact together run to several hundred thousand tokens, before a single verdict is
written. Gathering is therefore delegated; judging is not (Step 5, Step 7).

**Batching.** One batch per `Artifact` path, keyed off the Step 4 table. Merge the small artifacts so
that no batch exceeds roughly ten rows and no batch is a single row; leave each of the heaviest
artifacts as a batch of its own. Add one batch per aggregate for retro reconciliation (Step 3.b).
Every row of the log belongs to exactly one artifact batch - a row naming several artifacts goes to
the batch of the first one it names, judged against all of them.

**Spawning.** One `harness-retro-evidence` subagent per batch, with the brief in § "Evidence brief".
Spawn them in parallel; they are read-only by tool list, so they cannot collide. Their contract is
`.claude/agents/harness-retro-evidence.md`.

**What you never delegate.** Read yourself: `harness-log.md`, `plan.md`, the Step 3.a.i commit
listing, the Step 8 neutral-domain script output, and any artifact or diff you must re-judge under
Step 5 or Step 7. The batch returns are evidence and proposals, not findings.

**Checkpointing.** Batch returns may be written to the session scratchpad and re-read while
assembling the report. Nothing under the repository is written before Step 10.

**An `INCOMPLETE` or missing return is not a silent hole.** Re-spawn that batch once. If it returns
incomplete again, judge its rows yourself - a row with no evidence cannot be dismissed for lack of
evidence (Hard Rule 5).

### Evidence brief

Fill this template for every spawn. Substitute every placeholder; a worker that receives an unfilled
placeholder cannot do its job.

```
You are gathering evidence for ONE batch of harness-log rows in the
end-of-run retrospective for application {app-name}.

Read and follow: .claude/agents/harness-retro-evidence.md
Verdict definitions: .claude/skills/harness-retrospective/SKILL.md
                     § "Step 5: Cross-Check Each Row Against Current Content"
                     § "Step 7: Post-Hoc Justification of the Type 1 Edits"

BATCH: {batch-id}
ARTIFACT(S): {repo-relative path(s)}

ROWS (verbatim from applications/{app-name}/harness-log.md):
  <the full row: # | Session | Type | Artifact | Problem | Outcome | Ref>
  <one per row in this batch>

CLOSURE CONTEXT:
  <for each deferred row in this batch, the later row that closes it, or
   "no closer" - from the Step 4 resolution>

RESOLVED REFS:
  <row # -> the on-branch sha to read, for every fixed row whose logged Ref
   was made stale by a history rewrite (Step 3.a.i); "none" if there are none>

Return the block defined in .claude/agents/harness-retro-evidence.md.
```

For a retro reconciliation batch, replace `ARTIFACT(S)` and `ROWS` with the retro file paths and the
harness-log rows whose `Session` those retros cover, and drop `CLOSURE CONTEXT`.

---

## Step 5: Cross-Check Each Row Against Current Content

For every harness-log row, compare its `Problem` text against the current content of its `Artifact`
as the Step 4.b batch returned it, and classify. `fixed` rows and non-`fixed` rows are judged against
different questions.

**Where judgement lives.** A batch return carries a *proposed* verdict. You may accept a proposed
**Fix holds** on the strength of the quote it carries. Every other proposed verdict - and every
`EVIDENCE: insufficient` return - you verify yourself: read the artifact, and for a `fixed` row the
diff in `Ref`, before the verdict enters the report. Those are the rows that become gaps, so they are
the rows the run's conclusions rest on. Their number is a minority of the log.

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

For rows with `Outcome` = `declined` or `deferred`, the question is whether the gap is still open. A
`deferred` row named by a later row's `Closes row {N}` (Step 3.a) is resolved by that reference alone
— treat it as **Already closed** without re-reading the artifact for it — since the later row's own
Step 5 evaluation covers the fix it made.

That shortcut holds only for an **unqualified** closure. A closer whose text qualifies what it closed
(`Closes row {N} in part`, or any wording naming a remainder) leaves the rest of the gap open, so the
row it names gets the full cross-check below and its verdict speaks to the remainder only.

Only `declined` rows, `deferred (open)` rows and partially-closed rows need the full cross-check
below:

| # | Artifact | Outcome | Verdict | Evidence |
|---|----------|---------|---------|----------|

- **Confirmed** — the gap the row describes is still present in the file as it stands today.
- **Already closed** — the file has since changed and now covers it (a later session's fix, an
  unrelated edit, or a `Closes row {N}` reference). Quote the text that now covers it, or cite the
  closing row.
- **Misattributed** — the named artifact does cover it; the friction was caused by something else
  (another file, a spec ambiguity, or an agent error). Name what.
- **Not a harness gap** — the row records an implementation defect, a spec defect, or an agent
  mistake the harness could not reasonably have prevented.

**Confirmed**, **Fix incomplete**, **Fix reverted or superseded** and **Fix wrong** rows become gaps
in Step 9. A **Fix holds** row is listed in the report's Fixes Re-Checked section; every remaining
row is listed with its reason in the dismissed section. Nothing is dropped silently.

---

## Step 6: Convergence

Order the harness-log rows by their `Session` field in plan.md session order (`0`, `1`, `2.1.a` …)
and count rows per aggregate, split by `Type`:

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

For every row whose `Type` names `1` - including a compound row (Step 3.a), whose Type 1 half is
audited here - read the diff in its `Ref` (Step 3.a.i) and judge it against the definition in
`AGENTS.md` § "Harness evolution": a Type 1 fix requires a contradiction the agent could
**demonstrate mechanically** - a failing build, a missing symbol, two skills prescribing different
things.

**This step's judgement is yours, not a worker's.** A proposed **Sound** verdict may be accepted on
the diff hunk the batch return quotes. Every proposed **Sound but over-broad**, **Ratified a guess**
or **Misclassified** verdict you confirm yourself against the full diff before it enters the report,
and so is every `EVIDENCE: insufficient` row. This section exists to catch an agent ratifying its own
guess; a section written by unreviewed agents would not do that.

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

## Step 7.b: Type 2 Halts

A **halt** is the Type 2 gate working: a session stopped, put the question to the human, and resumed
on the answer. Not every Type 2 row is one - a session may record an ambiguity, resolve it by analogy
and carry on, which is a different and weaker event.

Classify every `2` and `2-fw` row:

- **Halt** - its `Problem` says the session stopped or the manager escalated, **or** the row that
  closes it records a human decision.
- **Proceeded** - its `Problem` states the session resolved it and continued, or the row is still
  open with no closer and no escalation.

Count both. Build the table the report carries, one line per halt, pairing the halting row with its
closer:

| # | Session | What the harness did not settle | What the human decided | Was the harness then fixed? |
|---|---------|--------------------------------|------------------------|-----------------------------|

The last column is `yes` with the closing row's `Ref` when the closer is `fixed`, and `no` otherwise.
A halt that produced a decision but no harness edit means the next run hits the same silence.

Name the **Proceeded** rows separately, with their numbers. Each is a place where the gate was
available and not used - worth a sentence, not a gap on its own.

---

## Step 8: Neutral-Domain Compliance

`.claude/skills/_shared/conventions.md` § "Neutral domain" forbids harness fixes from naming an
entity of the application being generated. The run's fixes were authored while looking at that
application, so the rule needs an end-of-run measurement and not only the per-boundary one.

Run the check exactly as `.claude/skills/review-artifacts/SKILL.md` § "Step 6: Check 4 — Neutral
Domain" defines it — that skill owns the procedure, including how the base commit is derived and
what to do when it degenerates; do not restate it here. Scope it to the whole run rather than one
aggregate's worth, so the diff covers every harness commit of the run.

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

Run `mkdir -p reviews` (no-op if it exists). Write `{report-file}` using the template below.
Never omit a section — write "nothing to report" where a section produced no findings. Under
self-healing OFF, the § Convergence and § Post-Hoc Justification sections and the `Verdict:` line are
**removed** rather than filled with "nothing to report": they ask questions the run cannot answer, and
an empty answer to them reads as a negative result.

```markdown
# Harness Retrospective — {app-name}

**App:** {app-name}
**Date:** {retro-date}
**Self-healing:** on | off (off: convergence and Type 1 re-judgement are not evaluated)
**Harness-log rows:** {count} (Type 1 {n} / Type 2 {n} / 2-fw {n})
**Sessions with friction:** {distinct Session values in the log} of {distinct session ids in plan.md}
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
| Sessions executed | {count} ({n} plan.md checkboxes) |
| Harness-log rows | {count} (T1 {n} / T2 {n} / 2-fw {n}) |
| Outcomes | fixed {n} / declined {n} / deferred (open) {n} / deferred (closed) {n} |
| `harness:` commits on the run | {count} |
| Harness edits without a `harness:` prefix | {count} |
| `Ref`s made stale by a history rewrite | {count} |
| Type 2 halts | {count} halted / {n} proceeded |
| Build outcome | MAVEN_EXIT={n}, tests={n} failures={n} errors={n} |

(One paragraph: what this run says about the harness overall.)

(When a compound `Type` row exists, state here that the per-type counts sum to more than the row
total, and why.)

Harness-log integrity: (numbering, `Type`/`Outcome` values including compound ones, `Ref`
resolvability, `harness:` commits with no row, harness-path commits without a `harness:` prefix,
misfiled rows - or "clean".)

---

## Rows by Artifact

| Artifact | Rows | T1 | T2 | 2-fw | fixed | declined | deferred (open) | deferred (closed) | What the friction was about |
|----------|------|----|----|------|-------|----------|------------------|--------------------|-----------------------------|

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

## Fixes Re-Checked

(Every `fixed` row, with the Step 5 verdict on whether the fix holds today. `Fix incomplete`,
`Fix reverted or superseded` and `Fix wrong` rows also appear under Confirmed Gaps; the rest appear
only here.)

| # | Artifact | Ref | Verdict | Evidence |
|---|----------|-----|---------|----------|

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

**Proceeded without halting:** rows {#, #} - one sentence on what each resolved by itself.

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
8. How many rows were accepted on a batch quote and how many you re-judged directly
9. Build outcome: observed `MAVEN_EXIT` and surefire totals

---

## Hard Rules

1. **Read-only except for the report.** This skill writes exactly one file in the repository:
   `reviews/harness-retro-{app-name}-{retro-date}.md`. It never edits a doc, a skill, the
   harness log, a retro, a review, or any source file — including the gaps it identifies. This is
   the one skill with no Type 1 fast path of its own: it is judging the fixes, so it may not also be
   making them. Fixes are applied between runs by a later session. Batch returns may be checkpointed
   to the session scratchpad, which is outside the repository; the `harness-retro-evidence` workers
   hold no write tool at all, so the rule holds through the delegation.
2. **Completed runs only.** Halt on any unchecked `- [ ]` in `plan.md` (Step 2). No partial reports.
3. **Every claim cites evidence.** A harness-log row number (`#7`), a commit sha, a repo-relative
   file path, or a quoted line. A statement about the harness with no citation does not go in the
   report.
4. **Cross-check before confirming.** No gap is Confirmed without a fresh read of the artifact's
   current content. A harness-log row is a record of what an agent believed at the time, not a
   standing fact - and a `fixed` row is a record of what it believed it had repaired.
5. **No verdict on a worker's word alone.** Only a proposed **Fix holds** or **Sound** carrying a
   verbatim quote may be accepted from a batch return. Every other verdict, and every
   `EVIDENCE: insufficient` row, is confirmed against the artifact or the diff by the agent writing
   the report.
6. **No silent dismissals.** Every harness-log row appears in the report exactly once, in exactly one
   of three places: **Fixes Re-Checked**, **Confirmed Gaps**, or **Dismissed Rows** with a stated
   reason. A row whose fix holds belongs in the first, not in Dismissed Rows.
6. **Do not propose fixes for the generated application.** Implementation defects in the generated
   application are out of scope for this skill.
7. **Report what the evidence shows.** A run with little friction is a valid result and so is a run
   that went badly. Do not soften a bad outcome and do not manufacture gaps to fill the section.
8. **No emojis. Terse and specific.** File paths, section names, row numbers, quoted snippets.
