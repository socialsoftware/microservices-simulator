---
name: boot-strap
description: Bootstrap a new microservices-simulator application (Phase 0). Creates pom.xml, the {AppClass}Simulator entry point, exception classes, BeanConfigurationSagas.groovy, and Spock test base classes from the checked-in scaffold templates. Invoke with /boot-strap <App Name> (e.g., /boot-strap my-app).
argument-hint: "<App Name>"
---

# Boot-Strap Phase 0 Application

This skill automates the Phase 0 bootstrap process. It creates the Maven project scaffold, exception infrastructure, and test configuration for a new application, ready for Phase 1 (plan generation) and Phase 2 (aggregate implementation).

The bootstrap establishes a minimal, working foundation: all infrastructure beans and base test classes are in place, but domain-specific code is deferred to Phase 1 planning and Phase 2 implementation.

Every file is produced from a checked-in template under `.claude/skills/boot-strap/templates/`. The templates are already correct and minimal — the only operation is token substitution. **Never read another application during bootstrap** — see `.claude/skills/_shared/conventions.md` § "Application isolation".

## Input

The skill is invoked as: `/boot-strap <App Name>`

Examples:
- `/boot-strap my-app`
- `/boot-strap train-ticket`

> **If no argument is provided**, ask the user: "What should the new application be named? (kebab-case, e.g. `my-app`)"

## Process

### Step 0: Anchor to the repository root

Before Step 1, read `.claude/skills/_shared/conventions.md` and follow "Anchor to the repository
root". Do not run any command until you have.

### Step 1: Parse Arguments and Derive Naming Variables

Given the `<App Name>` argument, derive the four substitution tokens:

| Token | Derivation | Example |
|-------|------------|---------|
| `{{APP_NAME}}` | the argument as-is (kebab-case) | `my-app` |
| `{{PKG}}` | hyphens removed, lowercase | `myapp` |
| `{{APP_CLASS}}` | split on hyphens, capitalize each segment, join without separator | `MyApp` |
| `{{appClass}}` | as `{{APP_CLASS}}` but with the first segment lowercase | `myApp` |

The argument must be kebab-case (lowercase with hyphens).

### Step 2: Read Template Files

Read all nine templates from `.claude/skills/boot-strap/templates/`:

1. `pom.xml.template`
2. `application.yaml.template`
3. `application-test.yaml.template`
4. `AppSimulator.java.template`
5. `AppException.java.template`
6. `AppErrorMessage.java.template`
7. `SpockTest.groovy.template`
8. `BeanConfigurationSagas.groovy.template`
9. `AppSpockTest.groovy.template`

`templates/README.md` documents the token vocabulary and how the templates are maintained.

### Step 3: Create Directory Structure

Create the following directory structure (relative to repo root):

```
applications/{app-name}/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/pt/ulisboa/tecnico/socialsoftware/{pkg}/
    │   │   ├── {AppClass}Simulator.java
    │   │   └── microservices/exception/
    │   │       ├── {AppClass}Exception.java
    │   │       └── {AppClass}ErrorMessage.java
    │   └── resources/
    │       ├── application.yaml
    │       └── application-test.yaml
    └── test/groovy/pt/ulisboa/tecnico/socialsoftware/
        ├── SpockTest.groovy
        └── {pkg}/
            ├── BeanConfigurationSagas.groovy
            └── {AppClass}SpockTest.groovy
```

### Step 4: Produce Files

For each template: substitute the four tokens from Step 1, then write the result to its output path.
Copy the template content **verbatim** apart from token substitution — do not add, remove, reorder,
or reformat anything, and do not delete the `// ... added here as aggregates are implemented in
Phase 2` marker comments (Phase 2 relies on them as insertion points).

| Template | Output path (under `applications/{app-name}/`) |
|----------|------------------------------------------------|
| `pom.xml.template` | `pom.xml` |
| `application.yaml.template` | `src/main/resources/application.yaml` |
| `application-test.yaml.template` | `src/main/resources/application-test.yaml` |
| `AppSimulator.java.template` | `src/main/java/pt/ulisboa/tecnico/socialsoftware/{pkg}/{AppClass}Simulator.java` |
| `AppException.java.template` | `src/main/java/pt/ulisboa/tecnico/socialsoftware/{pkg}/microservices/exception/{AppClass}Exception.java` |
| `AppErrorMessage.java.template` | `src/main/java/pt/ulisboa/tecnico/socialsoftware/{pkg}/microservices/exception/{AppClass}ErrorMessage.java` |
| `SpockTest.groovy.template` | `src/test/groovy/pt/ulisboa/tecnico/socialsoftware/SpockTest.groovy` |
| `BeanConfigurationSagas.groovy.template` | `src/test/groovy/pt/ulisboa/tecnico/socialsoftware/{pkg}/BeanConfigurationSagas.groovy` |
| `AppSpockTest.groovy.template` | `src/test/groovy/pt/ulisboa/tecnico/socialsoftware/{pkg}/{AppClass}SpockTest.groovy` |

Note that `SpockTest.groovy` lands in the **parent** package folder (`.../socialsoftware/`), not
under `{pkg}/` — its package declaration is `pt.ulisboa.tecnico.socialsoftware`.

### Step 5: Give the New App the Machine-Local Maven Settings

Maven is invoked per-app, so each app carries its own untracked `.mvn/maven.config` pointing at the
developer's settings file. Without it the very first `mvn` run fails resolving plugins from whatever
mirror `~/.m2/settings.xml` names — an error that looks like a scaffold bug but is not. This file is
machine config, so it is copied from elsewhere in the repo rather than templated: from a sibling app
if one exists, otherwise from `simulator/.mvn/maven.config`, which is what makes this step work for
the **first** application in a repo, when the sibling glob matches nothing.

> **The one sanctioned exception to § "Application isolation".** `.mvn/maven.config` is untracked
> machine configuration — a `-s <path>` line naming the developer's settings file. It carries no
> domain content and is not a design source, so copying it is not a peer-as-specification read. The
> exception is limited to this single file: never open any other file of a sibling application, and
> never read the sibling's `pom.xml`, source, or tests while doing it.

```bash
src=$(ls -d applications/*/.mvn 2>/dev/null | grep -v "applications/{app-name}/" | head -1)
[ -n "$src" ] || src=simulator/.mvn
[ -f "$src/maven.config" ] || { echo "no maven.config to inherit from $src"; exit 1; }
mkdir -p applications/{app-name}/.mvn
cp "$src/maven.config" applications/{app-name}/.mvn/
echo "/applications/{app-name}/.mvn/maven.config" >> .git/info/exclude
```

`.git/info/exclude` lists these paths one app at a time, so the new app's entry must be appended —
otherwise the file shows up as untracked and can be committed by accident.

`simulator/.mvn/maven.config` is itself untracked machine config, for the same reason the copy is -
it is git-excluded, not committed. It is nonetheless the right fallback: `simulator/` is where the
core library is built, so any checkout that has ever run `mvn install` has one, which covers the
first application in a repo. Do not fall back to "the default `~/.m2/settings.xml` may already
resolve" - it typically does not, and the failure surfaces much later as an unreachable-mirror build
error that reads like a scaffold bug.

If `simulator/.mvn/maven.config` is absent too, the developer has no mirror configuration to inherit:
halt and say so, rather than scaffolding an application whose build will fail for a reason unrelated
to the scaffold. Never commit `applications/{app-name}/.mvn/maven.config`.

### Step 6: Verify and Confirm

1. Assert no token survived substitution. The result is a verdict on the scaffold, so it comes from
   `python3` rather than `rg` — a `PreToolUse` hook in this environment can rewrite a bare `rg` into
   `grep`, and a command that failed outright prints no matches either, which would read as a pass.
   See `.claude/skills/_shared/conventions.md` § "Commands whose output feeds a verdict". Substitute
   `{app-name}` before running:

   ```bash
   cd "$(git rev-parse --show-toplevel)"
   python3 - <<'EOF'
   import pathlib, sys

   root = pathlib.Path("applications/{app-name}")
   if not root.is_dir():
       sys.exit(f"{root} does not exist - check the app name")

   scanned, hits = 0, []
   for path in sorted(p for p in root.rglob("*") if p.is_file()):
       try:
           lines = path.read_text().splitlines()
       except (UnicodeDecodeError, OSError):
           continue
       scanned += 1
       hits += [(path, n, l.strip()) for n, l in enumerate(lines, 1) if "{{" in l]

   print(f"scanned {scanned} files under {root}")
   print(f"surviving tokens={len(hits)}")
   for path, n, l in hits:
       print(f"  {path}:{n}: {l}")
   EOF
   ```

   Expect `surviving tokens=0` over a non-zero file count. A zero over `scanned 0 files` is a failed
   check, not a pass. If any token survived, fix the file before continuing.

2. Compile the scaffold. Read `.claude/skills/_shared/conventions.md` § "Run the test suite" and
   follow it — no pipes, verdict from maven's exit status:

   ```bash
   cd "$(git rev-parse --show-toplevel)/applications/{app-name}"
   mvn clean -Ptest-sagas test
   echo "MAVEN_EXIT=$?"
   ```

   Expect `MAVEN_EXIT=0` and no tests run — Phase 0 ships no tests, and the surefire include is
   `**/{pkg}/sagas/**`, which nothing matches yet. A non-zero exit here means a template has drifted
   from the `simulator` library: fix the **template**, then regenerate. Do not patch the generated
   app.

3. Report that bootstrap completed successfully and list the full paths of all 9 created files.
4. Confirm the structure and mention that Phase 1 (plan generation) is the next step.

### Step 7: Commit the Scaffold

Phase 0 output must be committed before Phase 1 starts. Left uncommitted, the first application
commits of the run are not buildable from a clean checkout, and the run's history no longer shows
where the application began.

Stage the 9 generated files with explicit paths - never `git add -A` or `git add .`, which would
sweep in unrelated working-tree files. `applications/{app-name}/.mvn/maven.config` is machine
config and is **not** staged; Step 5 already excluded it.

```
chore({app-name}): bootstrap scaffold
```

Verify with `git status --short applications/{app-name}` that nothing generated is left untracked
apart from `.mvn/`, and report the commit sha.

---

## Notes

- The skill does not create a `plan.md` — that is Phase 1's responsibility.
- `{AppClass}Simulator.java` is **required**, not optional, even though nothing in Phase 0 runs it. Phase 2 test classes are annotated `@DataJpaTest`, which locates its context by searching for a `@SpringBootConfiguration` in the test's package and then upwards; `{AppClass}Simulator` in `pt.ulisboa.tecnico.socialsoftware.{pkg}` is the only class that search can find. It also supplies the `@EnableJpaRepositories` / `@EntityScan` over both `...{pkg}` and `...ms` that the simulator's entities and repositories need. Omit it and the first Phase 2 test fails with `Unable to find a @SpringBootConfiguration by searching packages upwards from the test`. The per-aggregate `{Aggregate}ServiceApplication` created in session 2.N.a does not substitute for it: it sits under `...{pkg}.microservices.{aggregate}`, which is not an ancestor of any test package, and is `@Profile`-gated.
- All 9 files are ready for Phase 1 planning immediately after bootstrap completes.
- `@PropertySource("classpath:application-test.properties")` in `BeanConfigurationSagas` resolves against `simulator/src/main/resources/application-test.properties` in the library jar, not against the new app. Leave it unchanged.
- `applications/{app-name}/.mvn/maven.config` (Step 5) is a `-s <path-to-settings>` line and is deliberately not templated — it names an absolute path on the developer's machine.
- If a template turns out to be wrong or stale, fix the **template**, not the generated app — see `templates/README.md`.
