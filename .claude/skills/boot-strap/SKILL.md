---
name: boot-strap
description: Bootstrap a new microservices-simulator application (Phase 0). Creates pom.xml, exception classes, BeanConfigurationSagas.groovy, and Spock test base classes from the checked-in scaffold templates. Invoke with /boot-strap <App Name> (e.g., /boot-strap quizzes-full).
argument-hint: "<App Name>"
---

# Boot-Strap Phase 0 Application

This skill automates the Phase 0 bootstrap process. It creates the Maven project scaffold, exception infrastructure, and test configuration for a new application, ready for Phase 1 (plan generation) and Phase 2 (aggregate implementation).

The bootstrap establishes a minimal, working foundation: all infrastructure beans and base test classes are in place, but domain-specific code is deferred to Phase 1 planning and Phase 2 implementation.

Every file is produced from a checked-in template under `.claude/skills/boot-strap/templates/`. The templates are already correct and minimal — the only operation is token substitution. **Never read `applications/quizzes/` or `applications/quizzes-full/` during bootstrap.**

## Input

The skill is invoked as: `/boot-strap <App Name>`

Examples:
- `/boot-strap quizzes-full`
- `/boot-strap my-app`

> **If no argument is provided**, ask the user: "What should the new application be named? (kebab-case, e.g. `my-app`)"

## Process

### Step 0: Anchor to the repository root

Before Step 1, read `.claude/skills/_shared/conventions.md` and follow "Anchor to the repository
root". Do not run any command until you have.

### Step 1: Parse Arguments and Derive Naming Variables

Given the `<App Name>` argument, derive the four substitution tokens:

| Token | Derivation | Example |
|-------|------------|---------|
| `{{APP_NAME}}` | the argument as-is (kebab-case) | `quizzes-full` |
| `{{PKG}}` | hyphens removed, lowercase | `quizzesfull` |
| `{{APP_CLASS}}` | split on hyphens, capitalize each segment, join without separator | `QuizzesFull` |
| `{{appClass}}` | as `{{APP_CLASS}}` but with the first segment lowercase | `quizzesFull` |

The argument must be kebab-case (lowercase with hyphens).

### Step 2: Read Template Files

Read all eight templates from `.claude/skills/boot-strap/templates/`:

1. `pom.xml.template`
2. `application.yaml.template`
3. `application-test.yaml.template`
4. `AppException.java.template`
5. `AppErrorMessage.java.template`
6. `SpockTest.groovy.template`
7. `BeanConfigurationSagas.groovy.template`
8. `AppSpockTest.groovy.template`

`templates/README.md` documents the token vocabulary and how the templates are maintained.

### Step 3: Create Directory Structure

Create the following directory structure (relative to repo root):

```
applications/{app-name}/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/pt/ulisboa/tecnico/socialsoftware/{pkg}/
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
| `AppException.java.template` | `src/main/java/pt/ulisboa/tecnico/socialsoftware/{pkg}/microservices/exception/{AppClass}Exception.java` |
| `AppErrorMessage.java.template` | `src/main/java/pt/ulisboa/tecnico/socialsoftware/{pkg}/microservices/exception/{AppClass}ErrorMessage.java` |
| `SpockTest.groovy.template` | `src/test/groovy/pt/ulisboa/tecnico/socialsoftware/SpockTest.groovy` |
| `BeanConfigurationSagas.groovy.template` | `src/test/groovy/pt/ulisboa/tecnico/socialsoftware/{pkg}/BeanConfigurationSagas.groovy` |
| `AppSpockTest.groovy.template` | `src/test/groovy/pt/ulisboa/tecnico/socialsoftware/{pkg}/{AppClass}SpockTest.groovy` |

Note that `SpockTest.groovy` lands in the **parent** package folder (`.../socialsoftware/`), not
under `{pkg}/` — its package declaration is `pt.ulisboa.tecnico.socialsoftware`.

### Step 5: Verify and Confirm

1. Assert no token survived substitution:

   ```bash
   rg -n '\{\{' applications/{app-name}
   ```

   This must return no matches. If it does, fix the file before continuing.

2. Report that bootstrap completed successfully and list the full paths of all 8 created files.
3. Confirm the structure and mention that Phase 1 (plan generation) is the next step.

---

## Notes

- The skill does not create a `plan.md` — that is Phase 1's responsibility.
- The skill does not create any `{AppClass}Simulator.java` entry point (that is outside the bootstrap scope), even though `pom.xml` names it as `<start-class>`. Nothing in the test profile resolves that class.
- All 8 files are ready for Phase 1 planning immediately after bootstrap completes.
- `@PropertySource("classpath:application-test.properties")` in `BeanConfigurationSagas` resolves against `simulator/src/main/resources/application-test.properties` in the library jar, not against the new app. Leave it unchanged.
- If the first `mvn` run in the new app fails resolving plugins from an unreachable mirror, the developer's `~/.m2/settings.xml` needs overriding per-app. The existing apps do this with an untracked `applications/{app-name}/.mvn/maven.config` (a `-s <path-to-settings>` line). It is deliberately not templated — it is per-developer machine config, excluded via `.git/info/exclude`.
- If a template turns out to be wrong or stale, fix the **template**, not the generated app — see `templates/README.md`.
