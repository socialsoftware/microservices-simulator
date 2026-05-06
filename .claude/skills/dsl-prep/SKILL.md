---
name: dsl-prep
description: Analyze an existing application and produce a Nebula-DSL-ready breakdown as Markdown files — aggregates/roots, owned-vs-projected fields, events, cross-aggregate dependencies, intra-invariants, and a nebula.config.json stub. Read-only on the target app; writes to dsl/prep/<app>-prep/. Arguments: "<path/to/application>"
argument-hint: "<path/to/application>"
---

# DSL Prep: $ARGUMENTS

You are preparing the application at `$ARGUMENTS` to be expressed in the Nebula DSL. Your
job is **analysis and authoring of Markdown breakdown files** — you do NOT write `.nebula`
files, and you do NOT modify the target application. Your only writes go to
`dsl/prep/<app>-prep/` (where `<app>` is the final path segment of `$ARGUMENTS`).

Do all steps in order. After each step, confirm the output file exists before continuing.

---

## Reference material (read as needed)

- `dsl/CLAUDE.md` — DSL architecture overview
- `dsl/abstractions/teastore/` — minimal `.nebula` example (6 aggregates)
- `dsl/abstractions/answers/` — saga-heavy `.nebula` example (11 aggregates)
- `dsl/nebula/src/language/nebula.langium` — authoritative grammar
- `dsl/docs/user-guide/11-Reference.md` — DSL reference

Consult these to ground your classifications in actual DSL capabilities. If a domain
construct cannot be expressed in the grammar, flag it as a **⚠ Gap** rather than inventing
syntax.

---

## Step 0 — Locate source & prepare output path

1. Resolve `<app>` as the last path segment of `$ARGUMENTS`.
2. Ensure `dsl/prep/<app>-prep/` exists — create it if it does not.
3. Identify the domain source inside `$ARGUMENTS`. In priority order, look for:
   - Java aggregate classes (often under `.../domain/` with events under `.../events/`)
   - Existing domain-model markdown (`*-domain-model.md`, `*-aggregate-grouping.md`)
   - Any `.nebula` files (if present, this is already partly done — note it)
4. If multiple candidate sources exist and it's unclear which is authoritative, ask the
   user before proceeding.

---

## Step 1 — Aggregates & roots → `01-aggregates.md`

Identify each aggregate and its root entity. For each, record:
- Aggregate name
- Root entity name (and why it was picked — usually the entity that owns the aggregate's
  identity and lifecycle)
- Other entities contained in the aggregate (nested / child entities)
- One-sentence purpose of the aggregate

Write to `dsl/prep/<app>-prep/01-aggregates.md` with one `## <AggregateName>` section per
aggregate. Include a **⚠ Gap** note for any aggregate where the root choice is ambiguous.

---

## Step 2 — Field classification → `02-fields.md`

For each aggregate, list every field on its root entity and classify as:
- **Owned** — the aggregate is the source of truth
- **Projected from `<OtherAggregate>`** — a cached copy of data owned by another aggregate
  (typically set via event subscription; in Nebula this maps to an `Entity X from Y { map
  a as b }` projection)
- **Reference to `<OtherAggregate>`** — a foreign id, not a data copy

Produce a Markdown table per aggregate:

```
| Field | Type | Classification | Notes |
|-------|------|----------------|-------|
| ...   | ...  | Owned          |       |
| ...   | ...  | Projected from User | cached from UserUpdatedEvent |
```

Write to `dsl/prep/<app>-prep/02-fields.md`. Flag any field whose classification is
uncertain as **⚠ Gap**.

---

## Step 3 — Events → `03-events.md`

For each aggregate, list:
- **Publishes** — each event name + its payload (field name & type)
- **Subscribes** — events this aggregate listens to from others

Source of truth: look for Java event classes, `@EventListener`/workflow code, or existing
domain docs. Group by aggregate. Write to `dsl/prep/<app>-prep/03-events.md`.

Flag missing payloads or unclear subscribers as **⚠ Gap**.

---

## Step 4 — Cross-aggregate dependencies → `04-cross-aggregate.md`

For each pair of related aggregates, record:
- **Reference** — `source.field -> TargetAggregate` with an `onDelete` policy
  (`prevent` / `cascade` / `setNull`) and a user-facing message. Choose the policy that
  matches the domain semantics observed in the source; if unclear, note alternatives.
- **Inter-invariant** — rules of the form "when X happens in aggregate A, aggregate B must
  react/forbid". Express as a Nebula-style block:
  ```
  interInvariant NAME {
      subscribe FooDeletedEvent from Foo { <boolean expr> }
  }
  ```
- **Event subscription** — non-invariant cross-aggregate event flows (e.g., projection
  refresh on `UserUpdatedEvent`).

Organise the file by aggregate (what each aggregate consumes / enforces against others).
Write to `dsl/prep/<app>-prep/04-cross-aggregate.md`. Flag missing policies as **⚠ Gap**.

---

## Step 5 — Intra-invariants → `05-invariants.md`

For each aggregate, express every business rule that can be checked using only the root
entity's own fields as a boolean expression, in Nebula form:

```
check <invariantName> { <boolean expression over root fields> }
    error "<user-facing message>"
```

Rules requiring cross-aggregate state belong in Step 4, not here — if a rule needs both,
split it and cross-reference.

Write to `dsl/prep/<app>-prep/05-invariants.md`. Flag rules that cannot be expressed as
simple boolean expressions over root fields as **⚠ Gap** (they may need refactoring or a
projected field).

---

## Step 6 — Config stub → `06-config.md`

Draft a `nebula.config.json` for the application. Propose values based on what's in the
source (package name from Java imports, DB type if visible in `application.yml` / pom,
etc.). Mark guesses as **PLACEHOLDER**:

```json
{
  "projectName": "<app>",
  "basePackage": "<inferred or PLACEHOLDER>",
  "consistencyModels": ["sagas"],
  "database": {
    "type": "postgresql",
    "host": "postgres",
    "port": 5432,
    "name": "<app>db",
    "username": "postgres",
    "password": "password"
  },
  "java": {
    "version": "21",
    "springBootVersion": "3.5.3"
  }
}
```

Explain each non-trivial choice in prose below the JSON block. Ask (in the file) whether
`sagas`, `tcc`, or both are desired if the source doesn't make it clear.

Write to `dsl/prep/<app>-prep/06-config.md`.

---

## Step 7 — Summary → `SUMMARY.md`

Write a top-level `dsl/prep/<app>-prep/SUMMARY.md` with:
- Count of aggregates, events, references, invariants discovered
- A consolidated list of every **⚠ Gap** from steps 1–6, each with file + section reference
- A short "next steps" section suggesting which gaps to resolve first before writing
  `.nebula` files

---

## Output discipline

- Read-only on the target application — never edit files inside `$ARGUMENTS`.
- Only write inside `dsl/prep/<app>-prep/`.
- Prefer terse, structured Markdown (tables and bullets) over prose.
- When in doubt about a classification, write down both options and mark **⚠ Gap** —
  don't silently pick one.
- Do not fabricate events, fields, or invariants not present in the source.

## End-of-run report

Report to the user:
1. Path to the output folder
2. Number of aggregates analysed
3. Number of **⚠ Gaps** flagged (total)
4. Any source-location ambiguities that required assumptions
