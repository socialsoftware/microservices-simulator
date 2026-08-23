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

If there is exactly one result, use it. If there are several, apply this tie-break in order - `find`
returns them in directory order, which has nothing to do with which run is current, so never just take
the first:

1. The plan.md with at least one unchecked `- [ ]` box. If exactly one qualifies, that is the run in
   flight.
2. Otherwise (all complete, or several still open) the one whose application directory has the most
   recent commit: `git log -1 --format=%ct -- applications/{app-name}`.
3. If that is still ambiguous, halt and ask which application to work on. Never guess.

A referencing skill may override this with its own local rule; absent one, the tie-break above is
binding.

From the result path, extract:

- `{app-name}` = directory containing `plan.md` (e.g., `my-app`)
- `{pkg}` = `{app-name}` with hyphens removed, lowercase (e.g., `myapp`)
- `{AppClass}` = PascalCase of `{app-name}` — split on hyphens, capitalize each segment, join without
  separator (e.g., `MyApp`)

If no `plan.md` found, halt: **"No plan.md found. Run /classify-and-plan first."**

---

## Resolve aggregate context

For skills that take an aggregate name as their argument. Requires `{app-name}`, `{pkg}` from
"Resolve app context" above.

From the PascalCase argument, derive:

- `{Aggregate}` = the argument as given (e.g., `Warehouse`, `ShipmentItem`)
- `{aggregate}` = all-lowercase, hyphens and separators removed (e.g., `warehouse`, `shipmentitem`)

Verify that a section `### N. {Aggregate}` exists in `plan.md`. If not found, halt:
**"Aggregate '{Aggregate}' not found in plan.md. Check the name or run /classify-and-plan."**

`{N}` = the ordinal from that section header. It is the aggregate's position in the Implementation
Order table and the number used in session IDs (`2.{N}.a`).

Path prefixes — all relative to the repository root:

```
{tgt-src}     = applications/{app-name}/src/main/java/pt/ulisboa/tecnico/socialsoftware/{pkg}/
{tgt-test}    = applications/{app-name}/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/{pkg}/
```

---

## Harness evolution

Defined in `AGENTS.md` § "Harness evolution". Read it there; it is not restated here.

One clarification specific to skills: the Type 1 / Type 2 gates apply **from `/boot-strap` onward**,
whether or not `plan.md` exists yet.

---

## Harness log

`applications/{app-name}/harness-log.md` is the single append-only record of harness friction and
harness repair for a run. It is created by `/classify-and-plan` and appended to by every skill that
encounters friction. It is never edited retroactively and rows are never deleted.

A later row may CLOSE an earlier one by beginning its `Problem` cell with `Closes row {N}.` The
earlier row is never edited, because its `Outcome` records what happened at the time, not what
happened eventually. A `deferred` row that no later row closes is still open at the end of the run.

Schema - append one row per distinct friction point:

| # | Session | Type | Artifact | Problem | Outcome | Ref |
|---|---------|------|----------|---------|---------|-----|

- `#` - monotonically increasing; read the last row to get the next number.
- `Session` - the session id (`2.3.b`, `2.7.d`) or `0`/`1` for Phase 0/1.
- `Type` - `1` (contradiction, fixed unilaterally), `2` (ambiguity, human decided), `2-fw`
  (`simulator/`, always human-decided).
- `Artifact` - the repo-relative path of the harness file at fault.
- `Problem` - one sentence. What was needed, what was found instead.
- `Outcome` - `fixed` / `declined` / `deferred`. `declined` means the human decided the harness was
  right and the agent proceeded on the stated reading.
- `Ref` - the `harness:` commit sha for `fixed`; empty otherwise.

**Keep rows thin.** Do not restate the change - git holds the diff with perfect fidelity, and a
prose copy drifts. The row answers *why, which session, what type, what outcome*;
`git show {Ref}` answers *what changed*.

**Implementation defects are not harness friction.** A defect in the generated application belongs
in the review report's own Action Items table. Only findings that target `docs/`, `.claude/skills/`
or `simulator/` come here.

---

## Application isolation

While implementing or reviewing `{app-name}`, read only within `applications/{app-name}/`.
Files anywhere under `applications/` belonging to a **different** application must never be read —
not as templates, not for naming, not to resolve an ambiguity. The docs and skills are the
authoritative source; another application is a peer, not a specification.

Reads **within** `applications/{app-name}/` are unrestricted, including sibling aggregates already
implemented in earlier sessions.

If a cross-application read happens anyway, log it in `harness-log.md` naming the file read and the
gap that drove it: Type 1 if a doc or skill demonstrably failed to supply what was needed, Type 2
otherwise.

---

## Neutral domain

A harness fix may **not** name any entity, aggregate or operation of the application currently being
generated. Write fixes in the placeholder vocabulary already used across the docs - `{Aggregate}`,
`{Entity}`, `{Operation}`, `{Event}`. Where a worked example genuinely needs concrete nouns, invent a
neutral domain unrelated to the application at hand.

The reason is that a fix is authored while looking at one specific aggregate, and the vivid example
that comes to mind is a leaked answer for the next application the harness is pointed at.

This rule binds the harness artifacts only. `harness-log.md` rows are **exempt** and should name the
functionality that triggered the fix - that is the provenance the end-of-run retrospective needs.

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

**The `clean` run above is the mandated form for session-level and final verification** - the
verdict a session, a review or a phase reports comes from it, and nothing weaker.

**One narrow form is permitted, for intermediate verification only.** An agent implementing a slice
of a session (`.claude/agents/aggregate-slice.md`) drops `clean` and narrows with `-Dtest=`:

```bash
cd "$(git rev-parse --show-toplevel)/applications/{app-name}"
mvn -Ptest-sagas test -Dtest={NarrowedClasses}
echo "MAVEN_EXIT=$?"
```

`clean` on every slice recompiles the whole application once per slice, and the run it guards
against - a stale class from a previous build - is caught by the full clean suite the manager runs at
the end of the session anyway. The narrow form is never a substitute for that run: a slice's green is
a local signal, not the session's verdict, and cross-slice regressions are invisible to it.

Everything below applies to both forms.

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
- `MAVEN_EXIT` non-zero with `failures=0 errors=0` means the failure is outside the tests — e.g. a
  compilation error. It is not visible in the surefire totals; treat it as a failure and find the
  cause.
- The script prints the full report body for every class with a failure or error, so quote the failure
  output from there rather than from maven stdout.

Report the build outcome using these observed numbers. Do not write "BUILD SUCCESS" unless
`MAVEN_EXIT` was `0`.

### Inspecting maven output

The verdict never comes from maven's stdout - that is what the exit status and the surefire reports
above are for. A few checks nonetheless need the build's *real* stdout, because what they look for
is written only to the console: a framework log line a test's correctness depends on, for instance.

A shell redirect (`mvn ... > file.log`) does not reliably capture it. A `PreToolUse` hook may
filter or summarize the command's output before it is written, so the file can hold a condensed
transcript in which the line you are looking for simply is not present - and a `grep` returning
zero matches then reads as "the line never appeared" when it means "the line was filtered out".

Capture it from a child process instead, which the hook does not sit in front of:

```bash
cd "$(git rev-parse --show-toplevel)/applications/{app-name}"
python3 -c "
import subprocess, os, re
env = dict(os.environ); env['JAVA_HOME'] = '/path/to/jdk-21'
r = subprocess.run(['mvn','clean','-Ptest-sagas','test'{, '-Dtest=...'}],
                   capture_output=True, text=True, env=env)
print('MAVEN_EXIT', r.returncode)
out = r.stdout + r.stderr
for line in sorted(set(re.findall(r'{pattern to confirm}', out))):
    print(line)
"
```

The exit status is still `r.returncode`, and the surefire aggregation above still runs afterwards
unchanged - this recipe adds a console-log assertion, it does not replace either signal.
