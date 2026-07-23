---
name: review-aggregate-vs-quizzes
description: One-off harness-validation skill, quizzes-full only. Diffs one aggregate's implementation in quizzes-full against the hand-written quizzes reference app, file-by-file, to check whether the docs/skills harness reproduces the reference's design. Not part of the standard per-aggregate review loop and not applicable to future apps — use /review-aggregate for that. Writes a report to applications/quizzes-full/reviews/review-vs-quizzes-{Aggregate}.md. Invoke with /review-aggregate-vs-quizzes <AggregateName>.
argument-hint: "<AggregateName> (e.g. Course, CourseExecution, Tournament)"
---

# Review Aggregate vs Quizzes (one-off harness validation)

**Purpose:** `quizzes-full` is a comparative implementation — the same 8 aggregates as the reference
`quizzes` app, built purely from the docs/skills harness, to validate that the harness is
profile-agnostic and produces correct output without needing to consult `quizzes` directly (see
`AGENTS.md` "Current objective"). This skill is the check on that premise: it diffs one aggregate's
`quizzes-full` implementation against the actual `quizzes` reference files and reports where they
diverge and why.

This is **not** a conformance check — `/review-aggregate` already validates the target against its
own `plan.md` and the concept docs, with no reference app involved, and applies to any app built on
this harness. This skill exists only because `quizzes-full` happens to have a hand-written twin to
compare against. Once the harness-validation objective is complete, this skill has no further use
and should not be referenced from `docs/workflow.md`'s per-aggregate loop or from any other app's
process.

A divergence found here is not automatically a defect in `quizzes-full` — the two apps may make
different valid design choices (see Hard Rule 2). Divergences worth escalating are the ones that
reveal a **doc or skill gap**: something the harness under-specified, causing quizzes-full to
diverge from the reference by accident rather than by choice.

---

## Step 0: Anchor to the repository root

Before Step 1, read `.claude/skills/_shared/conventions.md` and follow "Anchor to the repository
root". Do not run any command until you have.

## Step 1: Resolve Context

### 1.a — Locate plan.md and verify scope

Read `.claude/skills/_shared/conventions.md` § "Resolve app context" and derive `{app-name}`,
`{pkg}`, `{AppClass}`.

If `{app-name}` is not `quizzes-full`, halt: "This skill only applies to quizzes-full — it diffs
against the hand-written quizzes reference app, which has no equivalent for other apps. Use
/review-aggregate instead."

### 1.b — Resolve the aggregate

`{Aggregate}` = PascalCase argument (e.g., `Course`).
`{aggregate}` = lowercase (e.g., `course`).

Verify that a section `### N. {Aggregate}` exists in plan.md. If not found, halt:
"Aggregate '{Aggregate}' not found in plan.md. Check the name or run /classify-and-plan."

### 1.c — Derive path prefixes

```
{ref-src}     = applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/
{ref-test}    = applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/
{tgt-src}     = applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/{pkg}/
{tgt-test}    = applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/{pkg}/
{review-dir}  = applications/quizzes-full/reviews/
{review-file} = {review-dir}review-vs-quizzes-{Aggregate}.md
```

---

## Step 2: Read Files

Read each file and hold it in context. Absent files are findings, not errors. Read as many files in
parallel as possible.

### 2.a — Reference app files for this aggregate

Enumerate all files under `{ref-src}microservices/{aggregate}/` recursively.
Also: `find applications/quizzes/src/main/java -path "*/commands/{aggregate}/*" -name "*.java"`
Also: `find {ref-test} -path "*/{aggregate}/*" -name "*.groovy"`

Read each file. Build the reference file list.

### 2.b — Target app files for this aggregate

Enumerate all files under `{tgt-src}microservices/{aggregate}/` recursively.
Also: `find applications/quizzes-full/src/main/java -path "*/commands/{aggregate}/*" -name "*.java"`
Also: `find {tgt-test} -path "*/{aggregate}*" -name "*.groovy"` (covers `sagas/{aggregate}/` and `sagas/coordination/{aggregate}/`)

Read each file. Build the target file list.

---

## Step 3: File Inventory Diff

Compare the reference file list (Step 2.a) against the target file list (Step 2.b).

For each reference file, determine whether an equivalent target file exists. Match by purpose and
naming convention, not exact path (`{pkg}` replaces `quizzes`, `{AppClass}` replaces `Quizzes`).

| File (relative to microservices/{aggregate}/) | In Reference | In Target | Status | Notes |
|------------------------------------------------|-------------|-----------|--------|-------|

Status values: `OK` / `Missing` / `Extra` / `Intentional` / `Renamed`

**Always Intentional:** `causal/` subtree files — quizzes-full is sagas-only; quizzes has both a
sagas and a causal/TCC implementation side by side. This is not a harness gap.

For target files not in the reference, mark `Extra` with a justification note.

---

## Step 4: Design-Decision Deltas

For each file present in both trees, note any structural or behavioral divergence between the
reference and target implementation. This is not a re-derivation of the full structural checklist
(`/review-aggregate` already covers conformance to the concept docs) — focus specifically on places
where the two implementations differ from each other.

Known differences to check and record (not exhaustive — note anything else found while reading):

- **`quizzes` has a different domain shape than `quizzes-full` in places.** E.g. the reference
  `CourseService` has `createCourseRemote`, `getAndOrCreateCourseRemote`, etc., reflecting a design
  where Course is created indirectly via CourseExecution; in `quizzes-full`, Course is first-class.
  Compare service methods by purpose (create / update / delete / read), not by name, and record the
  shape difference rather than flagging it as a defect.
- **`Saga{Aggregate}` copy-constructor `sagaState` handling** — record whether the reference copies
  `other.getSagaState()` and whether the target resets to `GenericSagaState.NOT_IN_SAGA` (or vice
  versa). Both are valid; this is a delta to record, not a defect.
- Any other divergence in file structure, method signatures, or event handling discovered while
  reading Step 2's files.

| Delta | Reference | Target | Design choice or harness gap? | Notes |
|-------|-----------|--------|-------------------------------|-------|

Use "Design choice" when both implementations are valid and simply differ. Use "Harness gap" only
when the divergence stems from the docs/skills under-specifying something, causing an accidental
rather than deliberate difference — these are the findings worth acting on.

---

## Step 5: Write the Diff Report

Create `{review-dir}` if it does not exist (`mkdir -p {review-dir}`).
Write `{review-file}` using the template below. Do not omit any section — write "nothing to report"
if a section has no findings.

```markdown
# Review vs Quizzes — {Aggregate}

**App:** quizzes-full (compared against quizzes reference)
**Aggregate:** {Aggregate} (aggregate #{N} in plan.md)
**Date:** {today}
**Verdict:** Equivalent | Diverges by design | Harness gap found

> **Equivalent** = file inventory and design decisions match the reference (or differ only in
>   causal/-subtree files).
> **Diverges by design** = one or more deltas found, all attributable to valid alternative design
>   choices.
> **Harness gap found** = at least one delta traces back to under-specified docs/skills, not a
>   deliberate design choice.

---

## Summary

(One paragraph: overall equivalence verdict with justification.)

---

## File Inventory

| File | In Reference | In Target | Status | Notes |
|------|-------------|-----------|--------|-------|

---

## Design-Decision Deltas

| Delta | Reference | Target | Design choice or harness gap? | Notes |
|-------|-----------|--------|-------------------------------|-------|

---

## Harness Follow-Ups

(For each "Harness gap" delta: which doc or skill file should be updated, and what should change.
"Nothing to report" if no harness gaps were found.)
```

---

## Step 6: Print Summary to Conversation

Output to the conversation:
1. Absolute path to the review file
2. Verdict and one-sentence justification
3. Any Harness gap deltas verbatim
4. Count of Design-choice deltas

---

## Hard Rules

1. **Read files directly.** Every comparison is based on actual file content read in Step 2. Never infer the content of an unread file.
2. **A delta is not automatically a defect.** Only escalate deltas traceable to a doc/skill gap; record design-choice deltas without treating them as action items.
3. **Read-only except for the review file.** Do not modify any source or skill files.
4. **Never omit sections.** Write "nothing to report" if a section is empty.
5. **`causal/` files are always Intentional.** quizzes-full is sagas-only. Never raise a finding for absent TCC/causal files.
6. **One aggregate per invocation.**
7. **This is a one-off harness-validation activity**, not a routine review step. Do not wire it into `docs/workflow.md`'s per-aggregate loop or generalize it to other apps.
8. **No emojis. Terse and specific.** File paths, method names, rule names, line numbers where relevant.
