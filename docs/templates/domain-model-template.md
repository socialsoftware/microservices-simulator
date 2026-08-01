# {AppName} — Domain Model

> **Authored by the domain expert.** AI agents read this file as given — they do not modify entity definitions, aggregate boundaries, or rule semantics.
>
> Replace every `{placeholder}` with content specific to your application.
> Remove any rows or blocks that do not apply.
> Keep the section numbers and block shapes intact — they are part of the contract this file makes with AI agents.

---

## §1 — Entities

| Entity | Attributes | Owns |
|---|---|---|
| **{EntityName}** | `{field}: {Type}` (immutable?), `{field}: {Type}` | {OwnedChild} × N, or — |

> **Owns** — value objects with no independent identity; created and deleted with the entity. When an owned value object has more than one field, give it its own row in the table (e.g., `TournamentParticipant` owned by `Tournament`).
> **immutable** — add this annotation to any field that must not change after creation.
> **technical** — add this annotation to fields that exist for implementation reasons (e.g. `lastModifiedTime`), not domain reasons.
> **default:** — annotate attributes with non-null initial values (e.g. `active: Boolean (default: false)`).
> **Enum types** — list valid values inline (e.g. `type: CourseType (VALUE_A \| VALUE_B)`).
> **Soft-delete state** — do **not** add a `state` field to any entity row. The simulator's `Aggregate` base class provides `state: AggregateState` (`ACTIVE`, `INACTIVE`, `DELETED`) and sets it via `remove()`. Rules predicate on `Entity.state == DELETED` using this inherited field.

---

## §2 — Relationships

| From | To | Cardinality | Immutable |
|---|---|---|---|
| {Entity} | {Entity} | ONE_TO_ONE / MANY_TO_ONE / ONE_TO_MANY / MANY_TO_MANY | yes / no / {condition} |

> **Immutable** can be a condition rather than yes/no (e.g. "frozen after `availableDate`").

---

## §3 — Rules

### 3.1 — Single-entity rules

Rules that inspect only fields of a single entity.

> **AI agent implementation:** All §3.1 rules become **P1 intra-invariants** inside `verifyInvariants()`.

| Rule | Entity | Predicate |
|---|---|---|
| {RULE_NAME} | {Entity} | `{predicate in pseudo-code}` |

> Use `final` in the predicate column (e.g. "`Entity.field` is immutable — Java `final` field") for fields enforced by language immutability rather than a runtime check.

---

### 3.2 — Cross-entity rules

> **AI agent implementation:** The AI agent classifies each §3.2 rule into a pattern (P1–P4) using `docs/concepts/rule-enforcement-patterns.md` and confirms the classification with the user before writing any code. Domain experts write the rules; the AI decides the pattern.

One block per rule. Use the exact three-field shape below.

---

#### Rule: {RULE_NAME}

| Field | Value |
|---|---|
| Entities | {Entity1}, {Entity2} |
| Predicate | `{predicate in pseudo-code}` |

---

## §4 — Functionalities

> **§4 is a complete inventory of every operation the application exposes** - writes and reads alike.
> **One row per operation**, regardless of how many aggregates it touches. Nothing is omitted: an
> operation absent from §4 is an operation the AI agent will never implement, because §4 is the only
> source it has for functionalities.
>
> **Kind** — `Write` for an operation that changes state, `Read` for a query. Read functionalities
> (get, list, search, retrieve by id) are listed here exactly like writes; they are part of the
> application's surface and are planned and implemented like any other operation.
>
> **Primary Aggregate** — the aggregate that owns the main state change for a write, or the one being
> queried for a read (the one whose service method is the entry point).
>
> **Other Aggregates** — every aggregate the saga additionally reads or writes. This column is the
> saga-coordination signal: an empty cell means the operation needs no coordination and becomes a
> plain service method. Aggregates that merely react asynchronously to a published event are not
> listed here.

| Functionality | Primary Aggregate | Other Aggregates | Kind | Description |
|---|---|---|---|---|
| {WriteFunctionalityName} | {PrimaryAggregate} | {Aggregate1}, {Aggregate2} | Write | {One-sentence description of what the operation does} |
| {SingleAggregateWriteName} | {PrimaryAggregate} | — | Write | {One-sentence description; empty Other Aggregates means no saga coordination} |
| Get{PrimaryAggregate}ById | {PrimaryAggregate} | — | Read | {One-sentence description of what the query returns} |

---
