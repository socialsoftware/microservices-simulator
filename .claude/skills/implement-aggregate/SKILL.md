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

## Step 5b: Patch plan.md for Missing Files

While producing files, note any file that should exist but is absent from plan.md's file table for this session — for example, an enum type class (e.g., `CourseType.java`) for an aggregate field typed as that enum, or an owned entity class referenced in the domain model.

- For **unambiguous** omissions (a file you had to create to make the code compile): add it to the appropriate session row in plan.md and record it in the final report.
- For **ambiguous** cases (you are unsure whether a file belongs or should be generated elsewhere): ask the user before adding.

---

## Step 6: Report Completion

After ticking the checkbox, output a concise structured report:

1. **Files produced** — list all files created or modified (full paths)
2. **Checkbox ticked** — e.g., `[x] 2.1.a`
3. **plan.md additions** — any files added to the plan.md file table during this session (Step 5b), and why
4. **Contradictions / problems** — any contradiction between plan.md and the domain model or rule classification (e.g., a write functionality that mutates a field marked P1 final), or any pattern that didn't match the docs and required consulting the reference app
5. **Reference app consulted** — list any files read from `applications/quizzes/` and what question each resolved (signals for future doc improvement)
6. **Next session** — "Next: 2.{N}.{next-type}" or "Aggregate {Aggregate} complete. Next: aggregate {N+1}."

---

## Step 7: Write Retro

All session context variables are already resolved (`{session-id}`, `{N}`, `{type}`, `{Aggregate}`, `{app-name}`, `{session-type-name}`). Load `.claude/skills/retro/SKILL.md` and execute **Steps 3–5 only** (Steps 1 and 2 are already done):

- **Step 3** — Gather evidence from conversation context. Do NOT re-read files or run filesystem commands. Answer from what happened during this session: which files were produced, which concept docs were read (and whether they were sufficient), which reference-app files were consulted (and why), which skill instructions were unclear, any naming/path decisions not covered, any mid-session bugs or corrections, any undocumented patterns observed.
- **Step 4** — Fill in the retro template exactly as specified in the retro skill.
- **Step 5** — Create `applications/{app-name}/retros/` if it does not exist, then write the retro file to `applications/{app-name}/retros/retro-{session-id}-{Aggregate}.md`.

Do not print a separate retro completion report — the retro file path is included in the Step 8 commit output.

---

## Step 8: Commit

Stage all files produced during this session using `git add <specific files>` (never `git add -A`). Include:
- Every file created or modified (from the Step 6 report)
- The retro file written in Step 7

Issue a single commit using HEREDOC format:

```
feat({app-name}): 2.{N}{type} ({Aggregate} {session-type-name})
```

Where `{session-type-name}` maps: `a`→"Domain Layer", `b`→"Write Functionalities", `c`→"Read Functionalities", `d`→"Event Wiring".

Example: `feat(quizzes-full): 2.2c (User Read Functionalities)`

After the commit, output the commit hash and message as the final line of the session report.

---

## Reference App

If a pattern is unclear from the concept docs alone, consult the equivalent file in `applications/quizzes/src/` — it is the authoritative working example. Note what you consulted so the docs can be improved later.
