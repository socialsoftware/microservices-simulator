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
skill, not here). **If `find` returns more than one result and the referencing skill defines no local
rule, halt and ask which application** — `find` order is filesystem-dependent, so "the first result"
would otherwise pick an arbitrary app. From the result path, extract:

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

Schema - append one row per distinct friction point. The three rows below are **illustrative**, not
real entries: they show the `Problem` phrasing to aim for, a `Closes row {N}.` row, and how thin a
thin row is.

| # | Session | Type | Artifact | Problem | Outcome | Ref |
|---|---------|------|----------|---------|---------|-----|
| 12 | 2.4.b | 1 | `.claude/skills/implement-aggregate/session-b.md` | § "{Aggregate}Service.java (read methods)" told the service to fetch a foreign aggregate's DTO through that aggregate's service, which requires injecting a foreign `*Service` - forbidden by R2 and contradicted by the same file's § "Two-step read saga variant". Two sections of one skill prescribed opposite mechanisms for the same read. | fixed | a1b2c3d4e |
| 19 | 2.6.d | 2 | `docs/concepts/events.md` | § ByEvent sagaState guard says the guard may be skipped when the event "must apply even while the aggregate is mid-saga" but gives no test for which events those are, and `{Operation}` here writes a cached field two saga steps also write. Silent, not contradictory - the answer is a design decision. Human chose the field-overlap test. | fixed | f6g7h8i9j |
| 24 | 2.7.d | 1 | `docs/concepts/events.md`, `.claude/skills/implement-aggregate/session-d.md` | Closes row 19. The test agreed there was stated in `events.md` only; `session-d.md` still carried its own softer paraphrase, so the two could drift. Paraphrase replaced with a pointer to the owning section. | fixed | k1l2m3n4o |

- `#` - monotonically increasing; read the last row to get the next number.
- `Session` - the session id (`2.3.b`, `3.5`, `4.1`), `0`/`1` for Phase 0/1, or `review` for a repair
  driven by a `docs/reviews/` report rather than by friction hit inside a session.
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

**Do not redirect maven's output to a file.** A build log written into the working tree is an
artifact of a verification step, not of the application, and nothing downstream reads it - failures
are quoted from the surefire report `.txt` files, which maven writes to disk anyway.

A full run's stdout can exceed the tool's output budget and be truncated, taking the `MAVEN_EXIT`
line with it. **A run whose `MAVEN_EXIT` line you did not see is unverified**: re-run it, do not
infer the verdict from whatever stdout survived, and never report a pass you did not observe.

Run the build with no pipe and no redirect, then read the exit status from the `MAVEN_EXIT` line:

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

**Without `clean`, `target/surefire-reports/` still holds every earlier run's reports**, so the
aggregation script below sums classes the narrowed run never executed. Its `tests=` total therefore
does not describe the narrowed run. Report the per-class numbers for the classes named in `-Dtest=`,
and say which total you are quoting - a slice that reports the unfiltered total as its own is
reporting a number it did not produce, and one that reads a stale class's failure as its own will
chase a defect that is not there.

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

---

## Commands whose output feeds a verdict

The same `PreToolUse` hook that rewrites maven output also rewrites other CLI invocations - a bare
`rg` can reach the shell as `grep`, which rejects `rg`-only flags and fails the command outright, or
accepts a pattern file with different semantics and scans differently. Two rules follow:

- **A check whose command errored is a failed check, never a pass with zero hits.** Zero output from
  a command that did not run to completion is the absence of evidence, not evidence of absence.
  Inspect the exit status before reporting any "clean" verdict derived from a search.
- **Where a search result decides a verdict, use `python3 -` rather than `rg` or `grep`.** A short
  inline script reads the files itself, so the pattern semantics are in the script and not at the
  mercy of which binary the hook substituted. `rg` remains fine for exploration, where a mangled
  result costs a retry rather than a wrong conclusion.
