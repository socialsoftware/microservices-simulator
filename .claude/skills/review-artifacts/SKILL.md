---
name: review-artifacts
description: Periodic deep review of docs/ and .claude/skills/ for inconsistencies, alignment gaps, and improvement opportunities. No arguments. Writes a structured report to docs/reviews/review-{YYYY-MM-DD}.md.
argument-hint: "(no arguments)"
---

# Review Artifacts

Periodic read-only review of all documentation and skill files. Every check reads files directly
from disk. The only write is the dated report file produced at the end.

One invocation reviews all artifacts. No arguments needed.

---

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

### 1.c — Enumerate and read all retro files

Run: `find applications -name "retro-*.md" | sort`

Read every retro file. For each retro extract:
- `{app-name}`, `{session-id}`, `{Aggregate}` from the filename
- All rows from the "Action Items" table (Priority, Target file, Action)
- The "One-Line Summary" or "Key Outcome" line

Hold all High-priority action items that target a file under `docs/` or `.claude/skills/`.
These are mandatory review targets in Step 10.

---

## Step 2: Read All Artifacts

Read every file from both lists in Step 1.b in parallel.

**Skill files (explicit list — cross-check against Step 1.b list for new skills):**
- `.claude/skills/boot-strap/SKILL.md`
- `.claude/skills/classify-and-plan/SKILL.md`
- `.claude/skills/implement-aggregate/SKILL.md`
- `.claude/skills/implement-aggregate/session-a.md`
- `.claude/skills/implement-aggregate/session-b.md`
- `.claude/skills/implement-aggregate/session-c.md`
- `.claude/skills/implement-aggregate/session-d.md`
- `.claude/skills/review-aggregate/SKILL.md`
- `.claude/skills/review-artifacts/SKILL.md` (this file)

**Doc files (explicit list — cross-check against Step 1.b list for new docs):**
- `docs/workflow.md`
- `docs/architecture.md`
- `docs/concepts/aggregate.md`
- `docs/concepts/commands.md`
- `docs/concepts/events.md`
- `docs/concepts/rule-enforcement-patterns.md`
- `docs/concepts/sagas.md`
- `docs/concepts/service.md`
- `docs/concepts/testing.md`
- `docs/templates/domain-model-template.md`
- `docs/templates/aggregate-grouping-template.md`

If any file in the Step 1.b enumeration is NOT in the explicit lists above, read it too and
flag it in the report as an untracked artifact.

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

## Step 5: Check 3 — Session-to-Workflow Alignment

### 5.a — Files-to-produce alignment

`docs/workflow.md` Phase 2 loop describes which files each session produces.
Each `session-*.md` also has an explicit "Produces" or "Files" section.

For each session (a, b, c, d), extract both lists and compare:

| Session | File pattern | In workflow.md | In session-*.md | Delta |
|---------|-------------|---------------|-----------------|-------|
| a | ... | Yes/No | Yes/No | ... |
| b | ... | ... | ... | ... |
| c | ... | ... | ... | ... |
| d | ... | ... | ... | ... |

Note any file listed in one source but missing from the other.
Note files described with different path patterns or different per-operation counts.

### 5.b — BeanConfigurationSagas beans alignment

`docs/workflow.md` has a "BeanConfigurationSagas Incremental Update Reference" table.
Each `session-*.md` has an "Update BeanConfigurationSagas.groovy" section.

Extract both per session:

| Session | Bean | In workflow.md | In session-*.md | Verdict |
|---------|------|---------------|-----------------|---------|
| a | `Sagas{Aggregate}Factory` | ... | ... | ... |
| a | `{Aggregate}CustomRepositorySagas` | ... | ... | ... |
| b | `{Aggregate}Service` | ... | ... | ... |
| b | `{Aggregate}CommandHandler` | ... | ... | ... |
| b | `{Aggregate}Functionalities` | ... | ... | ... |
| b | `{Op}FunctionalitySagas` | ... | ... | ... |
| c | (any) | ... | ... | ... |
| d | `{Aggregate}EventHandling` | ... | ... | ... |
| d | `{Aggregate}EventHandler` | ... | ... | ... |
| d | `{Aggregate}EventProcessing` | ... | ... | ... |

Pay particular attention to whether `{Op}FunctionalitySagas` classes are listed as Spring beans
in workflow.md but correctly excluded from beans in the session skills. Quote the conflicting
text if found.

---

## Step 6: Check 4 — Test Taxonomy Alignment

### 6.a — Session-to-test-type mapping

From `docs/workflow.md` and `docs/concepts/testing.md`, extract the authoritative mapping:
T1 → session a; T2 write → session b; T2 read → session c; T3 → session d.

From each `session-*.md`, find the test section and extract which test types it produces.

| Session | Expected test type(s) | Actual in skill | Consistent? |
|---------|-----------------------|-----------------|-------------|
| a | T1 | ... | ... |
| b | T2 (write operations) | ... | ... |
| c | T2 (read operations) | ... | ... |
| d | T3 | ... | ... |

### 6.b — Test assertion rules

Extract these specific rules from `docs/concepts/testing.md`:
1. Not-found: `thrown(SimulatorException)`, NOT `thrown({AppClass}Exception)`
2. T1: no direct `verifyInvariants()` call; no call to service methods
3. T2 step-interleaving: one test case per saga step that calls `setForbiddenStates`
4. T3: polling called directly (no `@Scheduled`); event handler called manually

Verify each rule is stated consistently in the relevant session skill:

| Rule | Source in testing.md | In session-*.md | Consistent? | Notes |
|------|---------------------|-----------------|-------------|-------|
| Not-found exception type | ... | session-c.md | ... | ... |
| T1 no verifyInvariants | ... | session-a.md | ... | ... |
| T2 step-interleaving | ... | session-b.md | ... | ... |
| T3 direct polling | ... | session-d.md | ... | ... |

---

## Step 7: Check 5 — Classify-and-Plan vs. Template Alignment

### 7.a — Domain model template sections

From `classify-and-plan/SKILL.md` Steps 1 and 2, extract:
- The expected section headings it validates (§3.1, §3.2, §4 etc.)
- The column names it expects in each table
- The formatting conventions it parses (pipes, bullet points, etc.)

From `docs/templates/domain-model-template.md`, extract the actual section headings and
table structures.

| Expected by classify-and-plan | Actual in template | Match? | Notes |
|------------------------------|-------------------|--------|-------|

### 7.b — Aggregate grouping template sections

From `classify-and-plan/SKILL.md` Steps 1 and 3, extract expected section headings and
column names (§1, §3, §4, etc.).

From `docs/templates/aggregate-grouping-template.md`, extract the actual structures.

| Expected by classify-and-plan | Actual in template | Match? | Notes |
|------------------------------|-------------------|--------|-------|

### 7.c — Plan.md output structure alignment

From `classify-and-plan/SKILL.md` Step 8 (the "Construct plan.md" section), extract the
plan.md structure it generates: section headers, file-table row format per session, checklist
item format, Phase 3 session list format.

From `docs/workflow.md` "plan.md — The Job Queue" section, extract the expected plan.md
structure.

| Element | In classify-and-plan output | In workflow.md | Consistent? |
|---------|-----------------------------|----------------|-------------|

---

## Step 8: Check 6 — Doc Completeness (Undocumented Patterns from Retros)

For every "Patterns to Capture" item extracted from retros in Step 1.c, verify the pattern
was subsequently added to the relevant doc or skill file.

Additionally, check these specific known-gap patterns regardless of retro coverage:

1. **SagaCommand lock-acquisition pattern** — the two-step write-saga pattern where a
   `SagaCommand` wraps the read command for lock acquisition.
   Check: `docs/concepts/sagas.md` for a dedicated subsection.

2. **Delete in-place exception to copy-on-write** — delete mutates in-place; no copy needed.
   Check: `docs/concepts/service.md` for an explicit carve-out.

3. **FunctionalitySagas are not Spring beans** — `{Op}FunctionalitySagas` classes are
   instantiated inline inside `{Aggregate}Functionalities`; they are not Spring beans.
   Check: `session-b.md` and `session-c.md` BeanConfigurationSagas instructions.

4. **`{Aggregate}Functionalities.java` in session-b file list** — Functionalities coordinator
   always produced in session b, not session c.
   Check: `classify-and-plan/SKILL.md` file-table generation for 2.N.b rows.

5. **`{Aggregate}Factory.java` and `{Aggregate}CustomRepository.java` in session-a rows** —
   interfaces produced in session a alongside their implementations.
   Check: `classify-and-plan/SKILL.md` file-table generation for 2.N.a rows.

6. **Event class files in session-b rows** — when a write operation publishes an event,
   the `{Event}.java` class is produced in session b.
   Check: `classify-and-plan/SKILL.md` session-b file-table logic and `session-b.md`.

7. **EventProcessing design** — `docs/concepts/events.md` canonical wiring shows
   `EventProcessing` delegating to `{Aggregate}Functionalities.<updateMethod>()`. But
   `session-d.md` shows `EventProcessing` directly loading the aggregate, mutating cached
   fields, calling `verifyInvariants()`, and persisting. These are different implementation
   shapes for the same P2 pattern.
   Check: compare `events.md` EventProcessing template vs. `session-d.md` EventProcessing
   template. Quote both. Flag if different.

8. **Constructor does not call `verifyInvariants()`** — only the factory calls it after
   construction. The constructor itself must not.
   Check: `session-a.md` aggregate constructor guidance.

9. **`Get{Aggregate}ByIdCommand` may be produced in session b** — when required as the lock-
   acquisition read step in write sagas, the read command is produced in session b rather than c.
   Check: `session-b.md` and `session-c.md` for conditional handling of this command.

| Pattern | Retro origin | Expected doc | Expected skill | Current status |
|---------|--------------|-------------|----------------|----------------|

---

## Step 9: Check 7 — Skill Completeness (Doc Rules Not Captured in Skills)

For each concept doc, extract explicit rules, restrictions, or invariants. For each rule,
identify the session skill responsible for implementing it and verify the rule is mentioned.

Do not re-check items already covered in Check 2 (R1–R8) or Check 4 (test assertion rules).
Focus on rules that are doc-only and may not have propagated to skills.

| Doc | Rule / invariant | Responsible skill | Mentioned? | Notes |
|-----|------------------|-------------------|------------|-------|
| `aggregate.md` | copy constructor via `createFromExisting` | session-a.md | ... | ... |
| `aggregate.md` | `getEventSubscriptions()` required when P2 rules exist | session-d.md | ... | ... |
| `events.md` | subscription anchor = publisher aggregate's own id | session-d.md | ... | ... |
| `events.md` | do not subscribe to own published events | session-d.md | ... | ... |
| `sagas.md` | check `sagaState != NOT_IN_SAGA` before event processing | session-d.md | ... | ... |
| `commands.md` | `ServiceMapping` enum must list all services | session-b.md | ... | ... |
| `service.md` | event publishing via `unitOfWork.registerEvent(...)` | session-b.md | ... | ... |
| `testing.md` | Service-Command Tests (T2 variant) separate from saga tests | session-b.md | ... | ... |

---

## Step 10: Check 8 — Improvement Opportunities

### 10.a — Missing examples in concept docs

For each concept doc, identify algorithm or pattern descriptions that have no code example
and where a concrete example would reduce the need to consult the quizzes reference app.

| Doc | Section | Why an example would help |
|-----|---------|---------------------------|

### 10.b — Ambiguous guidance in skill files

Scan skill files for instructions containing:
- "depends on", "may be", "if applicable", "as appropriate", "as needed"
  without a concrete decision criterion following them
- Pattern names referenced without showing the code shape

| File | Step | Ambiguous phrase | What decision criterion is missing |
|------|------|------------------|------------------------------------|

### 10.c — Open retro action items

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

## Step 11: Write the Report

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

## Check 3 — Session-to-Workflow Alignment

### Files-to-Produce Delta

| Session | File pattern | In workflow.md | In session-*.md | Delta |
|---------|-------------|---------------|-----------------|-------|

### BeanConfigurationSagas Beans Delta

| Session | Bean | In workflow.md | In session-*.md | Verdict |
|---------|------|---------------|-----------------|---------|

---

## Check 4 — Test Taxonomy Alignment

### Session-to-Test-Type Mapping

| Session | Expected test type | Actual in skill | Consistent? |
|---------|-------------------|-----------------|-------------|

### Test Assertion Rules

| Rule | Source in testing.md | In session-*.md | Consistent? | Notes |
|------|---------------------|-----------------|-------------|-------|

---

## Check 5 — Classify-and-Plan vs. Template Alignment

### Domain Model Template Sections

| Expected by classify-and-plan | Actual in template | Match? | Notes |
|------------------------------|-------------------|--------|-------|

### Aggregate Grouping Template Sections

| Expected by classify-and-plan | Actual in template | Match? | Notes |
|------------------------------|-------------------|--------|-------|

### Plan.md Structure

| Element | In classify-and-plan | In workflow.md | Consistent? |
|---------|---------------------|----------------|-------------|

---

## Check 6 — Doc Completeness (Patterns from Retros)

| Pattern | Retro origin | Expected doc | Expected skill | Status |
|---------|--------------|-------------|----------------|--------|

---

## Check 7 — Skill Completeness (Doc Rules Not Captured)

| Doc | Rule | Responsible skill | Mentioned? | Notes |
|-----|------|-------------------|------------|-------|

---

## Check 8 — Improvement Opportunities

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

## Step 12: Print Summary to Conversation

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
   `docs/` or `.claude/skills/` must appear in Check 8 with a Resolved / Open determination.
6. **Use `git log` for retro resolution.** Run `git log -1 --format="%ai" -- {file-path}` for
   each targeted file; do not estimate modification dates.
7. **One invocation covers all artifacts.** Do not scope to a single aggregate or session.
8. **No emojis. Terse and specific.** File paths, section names, quoted snippets — no fluff.
