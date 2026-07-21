# Shared Skill Conventions

Common blocks referenced by multiple skills via a blocking Read pointer (skills cannot `@`-transclude
each other). Read the referenced section in full before continuing; do not paraphrase from memory of
a previous read.

---

## Anchor to the repository root

All paths in the referencing skill are relative to the **repository root**. The skill may be invoked
from any working directory (including inside `applications/{app-name}/`). Before running any `find`,
`cd`, read, or write, pin the working directory to the repo root:

```bash
cd "$(git rev-parse --show-toplevel)"
```

The change persists for the rest of the session. When constructing paths for the Read/Write tools,
root them at this directory. Never write to `applications/{app-name}/...` without first confirming cwd
is the repo root — otherwise a nested `applications/{app-name}/applications/{app-name}/...` path is
silently created.

---

## Resolve app context

Run: `find applications -name plan.md`

Use the first result (if a skill needs different handling for multiple results — e.g. disambiguating
by unchecked checkboxes or prompting the user — that logic is defined locally in the referencing
skill, not here). From the result path, extract:

- `{app-name}` = directory containing `plan.md` (e.g., `quizzes-full`)
- `{pkg}` = `{app-name}` with hyphens removed, lowercase (e.g., `quizzesfull`)
- `{AppClass}` = PascalCase of `{app-name}` — split on hyphens, capitalize each segment, join without
  separator (e.g., `QuizzesFull`)

If no `plan.md` found, halt: **"No plan.md found. Run /classify-and-plan first."**
