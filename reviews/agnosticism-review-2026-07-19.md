# Harness Agnosticism & Duplication Review — 2026-07-19

**Trigger:** Post-`quizzes-full` review of docs, skills, and CLAUDE/AGENTS files to (a) make the
harness application-agnostic for future apps and (b) eliminate content duplication so the artifacts
stay maintainable.

**Verdict:** Issues requiring action — the harness is *functionally* agnostic (every skill auto-detects
`app-name` from `plan.md` at runtime), but the same knowledge is written down in 2-4 places at once, it
has already begun to drift, and one skill plus one AGENTS.md are structurally tied to the `quizzes`
reference in ways that will not carry to a new app.

---

## Scope

**In scope (harness artifacts):**
- `docs/architecture.md`, `docs/workflow.md`
- `docs/concepts/*.md`, `docs/templates/*.md`
- `.claude/skills/**/SKILL.md` (6 skills) + `implement-aggregate/session-{a,b,c,d}.md`
- `AGENTS.md`, `simulator/AGENTS.md`, `applications/quizzes/AGENTS.md` (the `CLAUDE.md` files are
  one-line `@AGENTS.md` includes — no independent content)

**Out of scope:** per-app generated output (`applications/*/plan.md`, `retros/`, `reviews/`) and
`docs/reviews/*` — regenerated per app, not reusable harness.

---

## Decisions this review was built on

| # | Decision | Resolution |
|---|----------|------------|
| 1 | Scope | Harness artifacts only (above). |
| 2 | Reference coupling | `boot-strap` keeps copying from `quizzes` (canonical scaffold); `review-aggregate` should split its reusable conformance half from its one-off `quizzes`-diff half. |
| 3 | Dedup mechanism | Skills cannot `@`-transclude (confirmed — see note). Extract shared blocks into one file; reference via a **blocking Read pointer**, mirroring `implement-aggregate` → `session-*.md`. |
| 4 | Deliverable | Findings + prioritized recommendations, no diffs (this file). |
| 5 | Domain examples | Illustrative `Course`/`Tournament`/`Quiz` names in concept docs are fine; flag only usages that read as **normative**. |

> **Confirmed fact — `@`-includes do not expand inside `SKILL.md`.** `@path` is a memory-file feature
> (`CLAUDE.md` → `@AGENTS.md`). No skill uses it. Decisive in-repo proof: `implement-aggregate`
> composes itself from `session-a..d.md` via an explicit *"Read the sub-file now"* pointer (Step 4),
> not `@session-b.md`. All de-duplication below therefore uses that same blocking-pointer pattern, not
> transclusion.

---

## Executive summary

The costliest problem is **not** application-coupling — it is **duplication**. Four bodies of
knowledge (the repo-root anchor, the app-context resolution preamble, the `plan.md` job-queue
structure, and the per-session file-list + bean-config) are each authored in 2-4 files simultaneously.
`review-artifacts` is an entire skill whose job is to *detect drift among these copies* — its existence
is the symptom, not the cure. Two of these copies have **already drifted**. Fixing duplication
structurally shrinks `review-artifacts` and removes a whole class of future bugs.

For agnosticism, the harness is in good shape except for: one skill (`review-aggregate`) whose whole
premise is "diff against `quizzes`" (no equivalent exists for a new app), one concept-doc snippet that
hard-codes the target app's exception class, one **broken doc link**, and one **direct contradiction**
between `quizzes/AGENTS.md` ("use these as templates") and `implement-aggregate` ("never read the
reference app").

---

## Part A — Duplication (maintainability)

### A1 — Repo-root "Step 0: Anchor" block duplicated verbatim ×6  · **Major**

The ~15-line *"Anchor to the repository root"* paragraph is byte-identical across all six skills
(`boot-strap`, `classify-and-plan`, `implement-aggregate`, `review-aggregate`, `review-tests`,
`review-artifacts`). It even embeds a `quizzes-full`-era anecdote — *"this happened once with
`test-review-QuizAnswer.md`"* — in every copy.

- **Cost:** rewording the rule = 6 edits; a missed one = silent drift that `review-artifacts` must
  then detect.
- **Fix:** move to `.claude/skills/_shared/conventions.md` under a `## Anchor to the repository root`
  heading. Each skill's Step 0 becomes a blocking pointer:
  > *Before Step 1, read `.claude/skills/_shared/conventions.md` and follow "Anchor to the repository
  > root". Do not run any command until you have.*
- Strip the app-specific anecdote; state the rule generically ("a nested
  `applications/{app-name}/applications/{app-name}/…` path is created if cwd is not the repo root").

### A2 — App-context resolution duplicated ×4 — **already drifted**  · **Major**

The *"find `plan.md` → derive `{app-name}` / `{pkg}` / `{AppClass}`"* preamble appears in
`implement-aggregate`, `review-aggregate`, and `review-tests` (and `classify-and-plan` derives the same
names differently, from the domain-model filename).

- **Evidence of live drift:** `review-tests` halts with *"No plan.md found. Run /classify-and-plan
  first."* while `review-aggregate` halts with *"…Check the name **or run /classify-and-plan**."* Same
  logic, two wordings. `review-aggregate` also derives `{ref-src}`/`{ref-test}` prefixes the others
  omit.
- **Fix:** add `## Resolve app context` to `_shared/conventions.md` (the `find` + the three
  derivations + the standard halt string). Skills reference it, then add only their *own* extra
  derivations (e.g. `review-aggregate`'s `{ref-*}` prefixes) locally.

### A3 — `plan.md` structure authored in two places  · **Major**

The complete `plan.md` template — Rule Classification table, Implementation Order table, Aggregate
Details, per-session file rows, Phase 3 — is written **both** in `docs/workflow.md` ("plan.md — The Job
Queue", ~85 lines) **and** re-specified as generation logic in `classify-and-plan/SKILL.md` Steps 7-8.

- This is the single biggest maintenance liability: `classify-and-plan` is the *generator* of
  `plan.md`, yet `workflow.md` independently documents the *same* output shape. `review-artifacts`
  Step 7 exists specifically to reconcile the two.
- **Fix:** pick one source of truth. Recommended: `classify-and-plan/SKILL.md` owns the *authoritative
  output structure* (it is the code that produces it); `workflow.md`'s "plan.md — The Job Queue" section
  is reduced to a short prose overview + one small illustrative excerpt + a pointer to the skill.
  Delete the full duplicate template from `workflow.md`.

### A4 — Per-session "Produces" + BeanConfig updates triplicated  · **Major**

For each session `a/b/c/d`, the file list and the `BeanConfigurationSagas.groovy` beans-to-add are
written in **three** places: `docs/workflow.md` (Phase 2 per-session blocks), the corresponding
`session-*.md`, **and** `workflow.md`'s "BeanConfigurationSagas Incremental Update Reference" table.
`classify-and-plan` Step 7 emits a *fourth* copy of the file list into `plan.md`.

- `review-artifacts` Checks 3 and 5 exist solely to diff these against each other.
- **Fix:** `session-*.md` are the executable source of truth for "what session X produces + which beans
  it adds." `workflow.md` Phase 2 becomes a one-line-per-session summary that points to each
  `session-*.md`. Drop the standalone BeanConfig reference table (or keep *only* it and have
  `session-*.md` point *to it* — but not both directions).

### A5 — T1-T4 taxonomy restated in 4 places  · **Minor** (raise to Major if it drifts)

`docs/concepts/testing.md` is declared authoritative, yet the T1-T4 tier table/summary is re-typed in
`implement-aggregate/SKILL.md` (lines 13-21), `review-aggregate` Step 7, `review-tests` Step 3, and
`workflow.md` "Test naming".

- **Fix:** replace each restatement with a pointer — *"tiers T1-T4 defined in
  `docs/concepts/testing.md` § T1-T4; do not restate"*. Keep at most the one-column
  `session → test-file-name` mapping locally where it drives file generation.

### A6 — Phase 0 bootstrap authored twice — drift present  · **Minor**

`docs/workflow.md` Phase 0 (reads/produces/bean-list) re-describes what `boot-strap/SKILL.md` executes.
Already drifting: `workflow.md` says `{App}Exception` *"extends `TutorException` or base exception"*,
while `boot-strap` derives it by transforming `QuizzesException`. `TutorException` appears nowhere else
in the harness.
- **Fix:** `boot-strap/SKILL.md` owns Phase 0; `workflow.md` Phase 0 → short summary + pointer. Remove
  the stale `TutorException` mention.

### A7 — `review-artifacts` is the drift-detector for A1-A6  · **Structural**

Not a defect to fix directly, but the framing matters: much of `review-artifacts` (Checks 3, 5, 6, 7)
is machinery for catching inconsistency between the duplicated copies above. **As A1-A6 collapse to
single sources, those checks lose their reason to exist** and the skill should shrink to: path
validity (Check 1), pattern/restriction coverage (Check 2), and open-retro-item tracking (Check 8).
Re-scope it *after* the extractions, not before.

---

## Part B — Application coupling (agnosticism)

### B1 — `review-aggregate` cannot generalize as written  · **Major**

The skill's entire premise is a file-by-file diff of the target app against the `quizzes` reference
(`{ref-src}`/`{ref-test}` prefixes, Step 3 "File Inventory" vs reference, Hard Rules 8-9 about
`quizzes` `CourseService` specifics). A genuinely new app has **no** reference to diff against, so the
skill does not carry forward.

- **Fix (per Decision 2):** split it.
  - **Reusable half → keep/rename** (e.g. `review-aggregate` = "conformance review"): Steps 4-7
    (structural review vs concept docs, functionality coverage vs `plan.md`, rule enforcement, test
    coverage) and Step 9 (build) need no reference app — they check the target against `plan.md` + the
    concept docs.
  - **`quizzes`-diff half → carve out** as an explicitly app-specific, one-off validation activity (it
    was really *harness self-validation*: "does the agnostic process reproduce the reference?").
    Mark it out-of-band for future apps.

### B2 — Concept doc hard-codes the target app's exception class  · **Major** (reads normative)

`docs/concepts/service.md:216,219` shows `throw new QuizzesFullException(TOURNAMENT_ALREADY_CANCELLED …)`.
In an app-agnostic concept doc this should be `{AppClass}Exception`. Because it is in a normative
"how to write a service" snippet (not a parenthetical example), a reader could copy `QuizzesFullException`
into a new app.
- **Fix:** replace `QuizzesFullException` → `{AppClass}Exception` (and `TOURNAMENT_ALREADY_CANCELLED`
  → `{ERROR_CONSTANT}` or keep as a clearly-labelled example). Note `service.md:70,94` already use
  `QuizzesException` similarly — same treatment.

### B3 — `quizzes/AGENTS.md` contradicts `implement-aggregate`  · **Major**

`applications/quizzes/AGENTS.md` §"Reference Implementations" says *"Use these as templates when
implementing new aggregates or functionalities"* and lists `microservices/execution/`,
`AddParticipantFunctionalitySagas.java`, etc. This **directly contradicts** `implement-aggregate/SKILL.md`
§"Anti-Pattern: Do Not Consult the Reference App" (*"Never read files under `applications/quizzes/`
during implementation… consulting it will reproduce those bugs"*).
- **Fix:** reconcile. Either scope `quizzes/AGENTS.md`'s "use as templates" to humans/manual work only
  and add an explicit *"the automated harness must NOT read these — see `implement-aggregate`"* note,
  or delete the "templates" framing. An agent that reads `quizzes/AGENTS.md` today gets the opposite
  instruction from the one the harness relies on.

### B4 — Stale `quizzes-full` literals in skill examples  · **Minor**

`quizzes-full` is hard-coded in example strings/doc-strings across `boot-strap`, `classify-and-plan`,
`implement-aggregate`, `review-tests`, `review-aggregate`. All are illustrative — every skill
auto-detects `app-name` at runtime — so impact is low, but they read as if the harness targets one app.
- **Fix (low priority):** genericize to `{app-name}` / a neutral `my-app` in examples. Do this
  opportunistically while editing each skill for A1/A2, not as a standalone pass.

### B5 — Concept-doc reference pointers into `quizzes-full`  · **Minor**

Each concept doc ends with a "Reference Implementations (Quizzes)" section (acceptable — illustrative,
and `service.md:183` even documents a known reference *bug*, which is the intended
"docs-correct-the-reference" behavior). But `sagas.md:189,252` point into `applications/quizzes-full/`
— the doc references the very app it was meant to validate.
- **Fix (low priority):** prefer pointing reference sections at the stable `quizzes` app, not
  `quizzes-full`. Keep the `quizzes`-bug callout in `service.md` as-is.

### B6 — `boot-strap` coupling is acceptable  · **No action**

Per Decision 2, `boot-strap` copying from `applications/quizzes/` is legitimate — `quizzes` is the
canonical scaffold source. The 33 `quizzes` references are inherent to that role. Optional future
improvement: factor the "infrastructure vs domain" bean/file split into a checked-in neutral template
so `boot-strap` describes *what to copy* rather than *what to delete from quizzes* — but this is a
nice-to-have, not required for agnosticism.

---

## Part C — Hygiene

### C1 — Broken doc link  · **Major**

`simulator/AGENTS.md:43` links `[TCC merge](../docs/concepts/tcc.md)` — **`docs/concepts/tcc.md` does
not exist**. (TCC is also declared out-of-scope in `workflow.md`.) `review-artifacts` Check 1 should
have caught this.
- **Fix:** create the file, or remove the link and the "TCC support" row in
  `simulator/AGENTS.md`'s "What to Extend" table until TCC is in scope.

### C2 — `review-artifacts` explicit file lists will rot  · **Minor**

`review-artifacts` Step 2 hard-codes the skill/doc file lists it reviews; adding `_shared/conventions.md`
(from A1/A2) requires editing that list. Step 1.b already enumerates files dynamically via `find` — the
explicit lists in Step 2 partly duplicate that.
- **Fix:** when re-scoping `review-artifacts` (A7), have it derive its review set from the Step 1.b
  `find` output and drop the hard-coded lists, so new shared files are picked up automatically.

---

## Prioritized remediation roadmap

Ordered by value-to-effort. Each is independent unless noted.

| Order | Item | Type | Effort | Notes |
|-------|------|------|--------|-------|
| 1 | **C1** broken `tcc.md` link | Hygiene | XS | One-line fix; unblocks a valid path-check. |
| 2 | **B3** AGENTS-vs-skill contradiction | Coupling | S | Highest correctness risk: two docs give opposite instructions. |
| 3 | **A1 + A2** extract `_shared/conventions.md` | Dedup | M | Create the file; rewrite 6 Step 0s + 3-4 context preambles as blocking pointers. Fixes live drift. |
| 4 | **B2** `{AppClass}Exception` in `service.md` | Coupling | XS | Search/replace `QuizzesFullException`/`QuizzesException` in normative snippets. |
| 5 | **A5** taxonomy → `testing.md` pointers | Dedup | S | 4 restatements → pointers. |
| 6 | **A3** `plan.md` structure single-source | Dedup | M | `classify-and-plan` owns it; trim `workflow.md`. |
| 7 | **A4 + A6** session Produces/BeanConfig/Phase0 single-source | Dedup | M | `session-*.md` + `boot-strap` own it; trim `workflow.md`. |
| 8 | **B1** split `review-aggregate` | Coupling | L | Separate conformance review from `quizzes`-diff. |
| 9 | **A7 + C2** re-scope `review-artifacts` | Structural | M | **Do last** — only after 3-7 remove the copies its checks reconcile. |
| 10 | **B4 + B5** genericize `quizzes-full` literals | Coupling | S | Opportunistic, fold into edits above. |

---

## Target end-state (single sources of truth)

| Knowledge | Sole owner | Everyone else |
|-----------|-----------|---------------|
| Repo-root anchor | `.claude/skills/_shared/conventions.md` | Blocking Read pointer |
| App-context resolution | `.claude/skills/_shared/conventions.md` | Blocking Read pointer (+ local extras) |
| Test taxonomy T1-T4 | `docs/concepts/testing.md` | Pointer |
| `plan.md` output structure | `classify-and-plan/SKILL.md` | `workflow.md` = prose overview + pointer |
| Per-session files + beans | `session-*.md` | `workflow.md` = summary + pointer |
| Phase 0 bootstrap procedure | `boot-strap/SKILL.md` | `workflow.md` = summary + pointer |
| Architectural restrictions R1-R8 | `docs/architecture.md` | Pointer (already correct) |
| Enforcement patterns P1-P4 | `docs/concepts/rule-enforcement-patterns.md` | Pointer (already correct) |

`review-artifacts` then verifies only that the pointers resolve and that the single owners stay
internally consistent — a much smaller, more durable job than diffing N copies.

---

## What is already good (no change needed)

- Concept docs are placeholder-driven (`{Aggregate}`, `{Xxx}`) with illustrative domain names — the
  intended style (Decision 5).
- R1-R8 (`architecture.md`) and P1-P4 (`rule-enforcement-patterns.md`) each have exactly one
  authoritative home and are referenced, not copied, elsewhere.
- Runtime agnosticism works: every skill resolves `app-name` from `plan.md`/domain-model filename, so
  hard-coded `quizzes-full` strings are cosmetic, not behavioral.
- `implement-aggregate`'s "never consult the reference app" rule + the retro "Reference App Consulted =
  gap signal" loop is a sound agnosticism mechanism — the goal is to keep pushing knowledge *out* of
  `quizzes` and *into* the docs.
