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
machine config, so it is copied from a sibling app rather than templated.

> **The one sanctioned exception to § "Application isolation".** `.mvn/maven.config` is untracked
> machine configuration — a `-s <path>` line naming the developer's settings file. It carries no
> domain content and is not a design source, so copying it is not a peer-as-specification read. The
> exception is limited to this single file: never open any other file of a sibling application, and
> never read the sibling's `pom.xml`, source, or tests while doing it.

```bash
src=$(ls -d applications/*/.mvn 2>/dev/null | grep -v "applications/{app-name}/" | head -1)
if [ -n "$src" ]; then
  mkdir -p applications/{app-name}/.mvn
  cp "$src/maven.config" applications/{app-name}/.mvn/
  echo "/applications/{app-name}/.mvn/maven.config" >> .git/info/exclude
fi
```

`.git/info/exclude` lists these paths one app at a time, so the new app's entry must be appended —
otherwise the file shows up as untracked and can be committed by accident.

If no sibling app has one, skip this step; the developer's default `~/.m2/settings.xml` may already
resolve. Never commit `.mvn/maven.config`.

### Step 6: Verify and Confirm

1. Assert no token survived substitution:

   ```bash
   rg -n '\{\{' applications/{app-name}
   ```

   This must return no matches. If it does, fix the file before continuing.

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

---

## Notes

- The skill does not create a `plan.md` — that is Phase 1's responsibility.
- `{AppClass}Simulator.java` is **required**, not optional, even though nothing in Phase 0 runs it. Phase 2 test classes are annotated `@DataJpaTest`, which locates its context by searching for a `@SpringBootConfiguration` in the test's package and then upwards; `{AppClass}Simulator` in `pt.ulisboa.tecnico.socialsoftware.{pkg}` is the only class that search can find. It also supplies the `@EnableJpaRepositories` / `@EntityScan` over both `...{pkg}` and `...ms` that the simulator's entities and repositories need. Omit it and the first Phase 2 test fails with `Unable to find a @SpringBootConfiguration by searching packages upwards from the test`. The per-aggregate `{Aggregate}ServiceApplication` created in session 2.N.a does not substitute for it: it sits under `...{pkg}.microservices.{aggregate}`, which is not an ancestor of any test package, and is `@Profile`-gated.
- All 9 files are ready for Phase 1 planning immediately after bootstrap completes.
- `@PropertySource("classpath:application-test.properties")` in `BeanConfigurationSagas` resolves against `simulator/src/main/resources/application-test.properties` in the library jar, not against the new app. Leave it unchanged.
- `applications/{app-name}/.mvn/maven.config` (Step 5) is a `-s <path-to-settings>` line and is deliberately not templated — it names an absolute path on the developer's machine.
- If a template turns out to be wrong or stale, fix the **template**, not the generated app — see `templates/README.md`.
