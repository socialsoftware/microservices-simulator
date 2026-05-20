# CLAUDE.md

Guidance for Claude Code when authoring `.nebula` abstractions in this repo.

The work here is to **use** the Nebula DSL to model new applications — not to change the DSL itself. The user guide under `docs/user-guide/` is the authoritative source for syntax and semantics; this file only orients you and points you there.

---

## Where work happens

| Path | Purpose |
|------|---------|
| `abstractions/<app>/` | Where you write `.nebula` files (one per aggregate) plus `shared-enums.nebula`, `nebula.config.json`, optional `exceptions.nebula`. |
| `abstractions/answers/` | Reference application — 11 aggregates, saga-style flows. The richer of the two examples. |
| `abstractions/teastore/` | Reference application — 6 aggregates, simpler CRUD-heavy domain. |
| `docs/user-guide/` | Authoritative syntax and concept docs. Read the relevant chapter before inventing patterns. |
| `docs/examples/abstractions/` | Worked examples tied to each user-guide chapter. |
| `nebula/` | DSL compiler (Langium). Engine code — do not edit unless explicitly asked. |

---

## User guide — read these before authoring

| Chapter | When to consult |
|---------|----------------|
| [`01-Introduction.md`](docs/user-guide/01-Introduction.md) | First time touching the DSL. |
| [`02-Getting-Started.md`](docs/user-guide/02-Getting-Started.md) | Project setup, install, generate. |
| [`03-Your-First-Aggregate.md`](docs/user-guide/03-Your-First-Aggregate.md) | Top-level shape of an `Aggregate`, `Root Entity`, `@GenerateCrud`. |
| [`04-Types-Enums-Properties.md`](docs/user-guide/04-Types-Enums-Properties.md) | Primitives, `List/Set/Optional`, `SharedEnums`, defaults, `final`, `dto-exclude`. |
| [`05-Business-Rules-Repositories.md`](docs/user-guide/05-Business-Rules-Repositories.md) | `invariants {}` and the expression grammar; custom `Repository {}` queries. |
| [`06-Cross-Aggregate-References.md`](docs/user-guide/06-Cross-Aggregate-References.md) | Projected entities (`Entity X from Y { map ... as ... }`) and `References { onDelete }`. |
| [`07-Events-Reactive-Patterns.md`](docs/user-guide/07-Events-Reactive-Patterns.md) | `Events { publish / subscribe / interInvariant }`. |
| [`08-Tutorial-Library-System.md`](docs/user-guide/08-Tutorial-Library-System.md) | End-to-end synthesis (Member/Book/Loan). |
| [`09-Advanced-Patterns.md`](docs/user-guide/09-Advanced-Patterns.md) | Extract patterns, `Dto Entity`, custom `Service` / `WebAPIEndpoints`, `exceptions {}`. |
| [`10-Generated-Code.md`](docs/user-guide/10-Generated-Code.md) | What the engine emits — useful for debugging output, not for authoring. |
| [`11-Reference.md`](docs/user-guide/11-Reference.md) | Complete grammar + CLI reference. Use as lookup. |

---

## Reference applications — copy patterns from these

When the user guide leaves a stylistic question open (naming, ordering, when to use a projected entity vs an owned snapshot, how to structure a saga functionality), read the closest aggregate in `answers/` or `teastore/` and follow that pattern.

| Pattern you need | Where to look |
|------------------|---------------|
| Single-aggregate CRUD with `@GenerateCrud` | `teastore/category.nebula`, `teastore/product.nebula` |
| Projected entities + `References` | `teastore/order.nebula`, `teastore/cart.nebula` |
| `interInvariant` delete-guards | `teastore/order.nebula`, `answers/quiz.nebula` |
| Multiple projected entities + `Set<>` of projections | `answers/quiz.nebula`, `answers/tournament.nebula` |
| Custom `exceptions {}` block | `answers/exceptions.nebula` |
| Rich event publish/subscribe graph | `answers/answer.nebula`, `answers/question.nebula` |
| `shared-enums.nebula` with multiple enums | `answers/shared-enums.nebula` |

If neither reference app does the thing you're trying to do, double-check `11-Reference.md` and the tied example for the relevant chapter under `docs/examples/abstractions/`.

---

## Authoring workflow

1. **Read the prep docs** for the target app (e.g. `dsl/prep/<app>-prep/*.md`) to know aggregates, fields, events, references, invariants.
2. **Add enums first** to `<app>/shared-enums.nebula` so entities can reference them.
3. **One `.nebula` file per aggregate**, in this internal order (matches the reference apps):
   1. `@GenerateCrud` (if applicable)
   2. Projected `Entity X from Y { map ... }` blocks
   3. `Root Entity` with owned fields and `invariants {}`
   4. `Events {}` — `publish` first, then `subscribe`, then `interInvariant`
   5. `References {}` with `onDelete` policies
   6. Optional `Repository {}` / `Service {}` / `WebAPIEndpoints {}` / `Functionalities {}`
4. **Cross-check** against the closest reference aggregate before declaring the file done.
5. **Generate** only after all aggregates parse (see chapter 02 for CLI usage).

---

## Common pitfalls (not always covered in the guide)

- **Cross-aggregate checks do not belong in `invariants {}`.** Use `interInvariant` (event-driven) or `References` (delete policy) — see chapters 06 and 07.
- **`subscribe` conditions can only see fields declared in the corresponding `publish` block** of the source aggregate. If the field isn't published, you can't filter on it.
- **Projected fields must be `map`-ed before they can be used** in invariants or other expressions on that entity.
- **An aggregate must declare exactly one `Root Entity`** — missing or duplicate roots will fail generation.
- **Enums referenced in entity properties must exist in `shared-enums.nebula`** of the same project.
- **If a method/operator isn't in chapter 11's expression grammar, it won't parse.** Restructure the rule rather than reaching for Java syntax.

---

## Engine internals (reference only — do not modify unless explicitly asked)

The compiler lives under `nebula/` and is a Langium project. Pipeline:

```
.nebula → Parser → AST → Validators → Generator Registry → Feature Facades → Handlebars Templates → Java
```

For build / generate commands and pipeline detail, see [`02-Getting-Started.md`](docs/user-guide/02-Getting-Started.md) and [`10-Generated-Code.md`](docs/user-guide/10-Generated-Code.md). A pre-built VSCode extension at `extensions/nebula-extension-0.0.2.vsix` provides syntax highlighting and live validation while editing `.nebula` files — install it.
