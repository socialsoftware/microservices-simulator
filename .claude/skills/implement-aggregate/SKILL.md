---
name: implement-aggregate
description: Phase 2 aggregate implementation for microservices-simulator. Auto-detects the next unchecked session in plan.md and delegates to the appropriate sub-file (session-a.md through session-d.md). Invoke with /implement-aggregate [session] (e.g., /implement-aggregate or /implement-aggregate 2.3.b).
argument-hint: "[session] (e.g. 2.3.b — optional, auto-detects if omitted)"
---

# Phase 2: Implement Aggregate

This skill drives one Phase 2 session at a time, advancing the plan.md job queue by one checkbox per invocation. It reads plan.md, identifies the next unchecked session, loads only the instructions relevant to that session type, and produces the files listed in plan.md for that session.

## Input

Invoked as:
```
/implement-aggregate
/implement-aggregate 2.3.b
```

- **No argument** — auto-detect the next unchecked session across all `applications/*/plan.md` files.
- **With session arg** (e.g., `2.3.b`) — target that specific session; skip the auto-detect scan.

> If multiple applications have unchecked sessions and no argument was given, ask: "Multiple apps have unchecked sessions: {list}. Which should I work on?"

---

## Step 1: Locate plan.md and Identify the Target Session

### 1.a — Find plan.md

Run: `find applications -name plan.md`

Collect all results. If multiple found, check which have unchecked checkboxes (`- [ ]`).

- **Exactly one** with unchecked boxes → use it.
- **Multiple** → ask user (see prompt above).
- **None** → halt: "No plan.md found. Run /classify-and-plan first to generate one."

### 1.b — Determine target session

**If a session argument was provided (e.g., `2.3.b`):**

Parse it: `{N}` = `3`, `{type}` = `b`. Verify the checkbox `- [ ] 2.3.b` exists in plan.md. If already ticked, warn: "Session 2.3.b is already marked complete. Did you mean a different session?" and ask for confirmation before proceeding.

**If no argument (auto-detect):**

Scan plan.md for the first line matching `- [ ] 2.\d+\.[abcd]`. Extract:
- `{N}` = the aggregate number (the `\d+`)
- `{type}` = the session type (`a`, `b`, `c`, or `d`)

If no unchecked Phase 2 session found, check for unchecked Phase 3 sessions (`- [ ] 3.\d+`). If those exist, inform: "All Phase 2 sessions are complete. Use /implement-behaviour for Phase 3." If everything is ticked, inform: "All sessions are complete — nothing left to do."

---

## Step 2: Derive Context Variables

From the plan.md file path (e.g., `applications/quizzes-full/plan.md`):
- `{app-name}` = the directory name (e.g., `quizzes-full`)
- `{pkg}` = app-name with hyphens removed, lowercase (e.g., `quizzesfull`)
- `{AppClass}` = PascalCase — split on hyphens, capitalize each segment, join (e.g., `QuizzesFull`)
- `{appClass}` = same but first segment lowercase (e.g., `quizzesFull`)

From plan.md, find the aggregate details section for aggregate number `{N}`:
- Section header pattern: `### {N}. {Aggregate}` or `### {N}. {AggregateName}`
- `{Aggregate}` = the aggregate name in PascalCase (e.g., `Tournament`)
- `{aggregate}` = lowercase version (e.g., `tournament`)

Derive standard path prefixes:
- `{src}` = `applications/{app-name}/src/main/java/pt/ulisboa/tecnico/socialsoftware/{pkg}/`
- `{test}` = `applications/{app-name}/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/{pkg}/`
- `{bean-config}` = `{test}BeanConfigurationSagas.groovy`

---

## Step 3: Read the Aggregate Section from plan.md

Read and hold in context the entire aggregate section for `{N}` from plan.md. This section contains:
- Write functionalities list
- Read functionalities list (may be "none")
- Events published list
- Events subscribed list
- P1/P2/P3 rules notes
- Cross-aggregate prerequisites
- Files to produce table (sessions 2.N.a through 2.N.d)
- Checklist

This is the authoritative source for what to produce. Do not re-derive the file list — use the one in plan.md exactly.

---

## Step 4: Delegate to the Appropriate Sub-File

Based on `{type}`, load and follow the corresponding sub-file. All context variables derived above are available to the sub-file.

| type | Sub-file | Session name |
|------|----------|--------------|
| `a`  | `session-a.md` | Domain Layer |
| `b`  | `session-b.md` | Write Functionalities |
| `c`  | `session-c.md` | Read Functionalities |
| `d`  | `session-d.md` | Event Wiring |

Read the sub-file now and follow its instructions exactly. The sub-file specifies:
1. Which additional files to read before coding
2. What files to produce and how
3. How to update `BeanConfigurationSagas.groovy`
4. Which checkbox to tick

---

## Step 5: Tick the Checkbox

After all files are produced and BeanConfigurationSagas.groovy is updated, update plan.md:

Replace `- [ ] 2.{N}.{type}` with `- [x] 2.{N}.{type}` in plan.md.

---

## Step 6: Report Completion

After ticking the checkbox:
1. List all files created or modified (with full paths)
2. State which checkbox was ticked
3. State what comes next: "Next session: 2.{N}.{next-type}" or "Aggregate {Aggregate} is complete. Next: aggregate {N+1}."

---

## Reference App

If a pattern is unclear from the concept docs alone, consult the equivalent file in `applications/quizzes/src/` — it is the authoritative working example. Note what you consulted so the docs can be improved later.
