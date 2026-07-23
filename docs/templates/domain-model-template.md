# {AppName} — Domain Model

> **Authored by the domain expert.** AI agents read this file as given — they do not modify entity definitions, aggregate boundaries, or rule semantics.
>
> Replace every `{placeholder}` with content specific to your application.
> Remove any rows or blocks that do not apply.
> Keep the section numbers and block shapes intact — they are part of the contract this file makes with AI agents.
>
> Example: [`applications/quizzes/quizzes-domain-model.md`](../../applications/quizzes/quizzes-domain-model.md)

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

> **One row per operation.** The **Primary Aggregate** is the one that owns the main state change (the one whose service method is the coordination entry point). List every aggregate that the saga reads or writes in **Other Aggregates**.
> If an operation touches only a single aggregate, omit it here — it becomes a plain service method with no saga coordination.

| Functionality | Primary Aggregate | Other Aggregates | Description |
|---|---|---|---|
| {FunctionalityName} | {PrimaryAggregate} | {Aggregate1}, {Aggregate2} | {One-sentence description of what the operation does} |

---
