# Phase 0 Scaffold Templates

These files are the **source of truth** for `/boot-strap`. Do not derive Phase 0 output from an
existing application — a populated domain app carved back down leaks domain residue and drifts from
the library. See `.claude/skills/_shared/conventions.md` § "Application isolation".

Every file here is complete and correct as-is. Bootstrap is: read the template, substitute the
tokens below, write it to the output path. There are no deletion rules, no keep-lists, and no
"strip the domain parts" judgement calls.

## Tokens

| Token | Meaning | Example (`my-app`) |
|-------|---------|--------------------|
| `{{APP_NAME}}` | kebab-case app name, as given to `/boot-strap` | `my-app` |
| `{{PKG}}` | `{{APP_NAME}}` with hyphens removed, lowercase | `myapp` |
| `{{APP_CLASS}}` | PascalCase — split on hyphens, capitalize each segment, join | `MyApp` |
| `{{appClass}}` | camelCase — as `{{APP_CLASS}}` but first segment lowercase | `myApp` |

`{{APP_NAME}}`, `{{PKG}}`, and `{{APP_CLASS}}` match the derivations in
`.claude/skills/_shared/conventions.md` ("Resolve app context"). `{{appClass}}` is only used by
`AppException.java.template`.

After substitution, no `{{` may remain anywhere under `applications/{app-name}/`.

## `PLACEHOLDER` in `AppErrorMessage.java.template`

`AppErrorMessage` ships with `public static final String PLACEHOLDER = "placeholder"` for one
reason: a freshly bootstrapped app has no domain error constants yet, and the class must not be
empty. It is the **one exception** to the "append constants, never remove existing ones" rule that
sessions `a` and `c` state — once the first real domain constant lands, `PLACEHOLDER` may be
removed. Nothing references it, so removing it is safe; leaving it is also harmless.

## Maintenance

These templates pin against the `simulator` library. When the library changes, mirror it here:

- a package rename under `pt.ulisboa.tecnico.socialsoftware.ms.*` (this has happened before:
  `ms.transactional.sagas` → `ms.transaction.sagas`)
- a new or removed infrastructure bean in `BeanConfigurationSagas.groovy.template`
- a dependency or plugin version bump in `pom.xml.template`
- a new infrastructure constant in `SimulatorErrorMessage` that apps are expected to mirror

The templates hold only infrastructure. Everything domain-specific — error constants, beans,
`@Autowired` fields, `create*` helpers — is added by Phase 2 (`/implement-aggregate`), which is why
each template marks the insertion points with a comment.
