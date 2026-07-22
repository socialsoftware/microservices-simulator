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

---

## Resolve aggregate context

For skills that take an aggregate name as their argument. Requires `{app-name}`, `{pkg}` from
"Resolve app context" above.

From the PascalCase argument, derive:

- `{Aggregate}` = the argument as given (e.g., `Course`, `CourseExecution`)
- `{aggregate}` = all-lowercase, hyphens and separators removed (e.g., `course`, `courseexecution`)

Verify that a section `### N. {Aggregate}` exists in `plan.md`. If not found, halt:
**"Aggregate '{Aggregate}' not found in plan.md. Check the name or run /classify-and-plan."**

`{N}` = the ordinal from that section header. It is the aggregate's position in the Implementation
Order table and the number used in session IDs (`2.{N}.a`, `3.{N}`, `4.{N}`).

Path prefixes — all relative to the repository root:

```
{tgt-src}     = applications/{app-name}/src/main/java/pt/ulisboa/tecnico/socialsoftware/{pkg}/
{tgt-test}    = applications/{app-name}/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/{pkg}/
{review-dir}  = applications/{app-name}/reviews/
```

Skills that additionally need the `quizzes` reference app define `{ref-src}` / `{ref-test}` locally —
they are not part of this block, because most skills must not consult the reference app.

---

## Run the test suite

**Never pipe maven through `tail`, `head`, or `grep`, and never grep its stdout.** A `PreToolUse` hook
may filter or rewrite verbose CLI output before you see it, so `BUILD SUCCESS` and `Tests run:` lines
are not reliably present in what reaches you, and a pipe replaces maven's exit code with the exit code
of the last command in the pipeline. A skill that decides pass/fail from piped maven stdout is
reporting a result it did not observe.

Get the verdict from two sources the hook does not touch: maven's own **exit status**, and the
**surefire report files** maven writes to disk.

Run the build with no pipe, then read the exit status on the following line:

```bash
cd "$(git rev-parse --show-toplevel)/applications/{app-name}"
mvn clean -Ptest-sagas test {-Dtest=... if narrowing}
echo "MAVEN_EXIT=$?"
```

Then aggregate the surefire reports:

```bash
cd "$(git rev-parse --show-toplevel)/applications/{app-name}"
python3 -c "
import glob, re, sys
tot=fail=err=skip=0
bad=[]
for p in sorted(glob.glob('target/surefire-reports/*.txt')):
    body=open(p).read()
    m=re.search(r'Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)', body)
    if not m: continue
    t,f,e,s=(int(g) for g in m.groups())
    tot+=t; fail+=f; err+=e; skip+=s
    if f or e: bad.append((p, body))
print(f'TOTAL tests={tot} failures={fail} errors={err} skipped={skip}')
for p, body in bad:
    print('=== ' + p); print(body)
"
```

Interpret the two together:

- **Green** = `MAVEN_EXIT=0` **and** `failures=0 errors=0`.
- `MAVEN_EXIT` non-zero with `failures=0 errors=0` means the failure is outside the tests —
  a compilation error, or a `@PendingFeature` test that unexpectedly passed. Neither is visible in the
  surefire totals; treat it as a failure and find the cause.
- The script prints the full report body for every class with a failure or error, so quote the failure
  output from there rather than from maven stdout.

Report the build outcome using these observed numbers. Do not write "BUILD SUCCESS" unless
`MAVEN_EXIT` was `0`.
