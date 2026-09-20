# {AppName} — Domain Model (the plain domain)

> **Authored by the domain expert.** AI agents read this file as given — they do not modify entity
> definitions or rule semantics.
>
> **One plain domain, N aggregate groupings.** This file describes the domain and nothing else. The
> decomposition into aggregates lives in a separate `{app-name}-aggregate-grouping.md`, and several
> such files may exist over this one domain. A question whose answer would change this file is a
> domain question; a question whose answer would change only the grouping file must not reach this
> one.
>
> Replace every `{placeholder}` with content specific to your application.
> Remove any rows or blocks that do not apply.
> Keep the section numbers and block shapes intact — they are part of the contract this file makes with AI agents.

---

## What this file must never say

The following belong to the grouping file, never here. They are decomposition decisions, framework
mechanics or harness reasoning, and stating any of them here makes a second grouping impossible to
write without editing the "domain" model.

| Forbidden here | Lives in |
|---|---|
| Aggregate names, aggregate membership, "co-located", an `Owns` column | Grouping §1 |
| `{entity}AggregateId`, `{entity}Version`, a field copied from another entity | Grouping §2 |
| `technical` fields (`lastModifiedTime`, …) and their justification | Grouping §2.b |
| Caching, snapshots, cache coherence | Grouping §2 / §5 |
| Events, event subscriptions, the event DAG, saga steps | Grouping §3 / §4 |
| `state == DELETED` / `ACTIVE` / `INACTIVE`, `remove()`, `AggregateState` | Grouping (§2.b note) — say "has been removed" |
| Pattern names P1–P4, "precondition checked at operation time", "standing invariant vs. precondition" | Grouping §3 Consistency policy and §5 Rule realisation |
| Anything phrased in terms of what `/classify-and-plan` or any other skill will do | Grouping §5 |
| "References are by aggregate id, not by natural key" | Grouping |

Say **"a Station that has been removed"**, never `Station.state == DELETED`. Say **`Order.trip`**,
never `Order.tripAggregateId`.

---

## §1 — Entities

A flat list. Every entity with domain identity or domain attributes gets a row, and nothing else
does.

| Entity | Attributes |
|---|---|
| **{EntityName}** | `{field}: {Type}` (immutable?), `{field}: {Type}` |

> **immutable** — add this annotation to any field that must not change after creation.
> **default:** — annotate attributes with non-null initial values (e.g. `active: Boolean (default: false)`).
> **Enum types** — list valid values inline (e.g. `type: {EntityName}Type (VALUE_A \| VALUE_B)`).
> **Removal** — do **not** add a `state`, `deleted` or `active`-for-deletion field. Removal is
> available to every entity; rules refer to it in prose ("a `{Entity}` that has been removed").
> **No copied fields.** An attribute that duplicates another entity's attribute is a snapshot, which
> is a grouping decision. Model the relationship in §2 instead and let the grouping decide whether it
> is realised by a copy.

> **Relationship attributes become associative entities.** An attribute of a *pairing* belongs to
> neither end of it. `enrollTime` is an attribute of the participation, not of the `Tournament` and
> not of the `User`: model it as `{AssociativeEntity} { enrollTime }` with two `MANY_TO_ONE`
> relationships, one of them a composition. That says nothing about aggregates — a grouping may
> inline it, make it an aggregate of its own, or reduce it to a list of ids.

---

## §2 — Relationships

| From | To | Cardinality | Immutable | Composition |
|---|---|---|---|---|
| {Entity} | {Entity} | ONE_TO_ONE / MANY_TO_ONE / ONE_TO_MANY / MANY_TO_MANY | yes / no / {condition} | yes / no |

> **Immutable** can be a condition rather than yes/no (e.g. "frozen after `availableDate`").
>
> **Composition** — `yes` when the referencing entity has no independent existence and is destroyed
> with its target (an `Option` cannot exist without its `Question`). This is a fact about the domain.
> It is **not** aggregate membership: composition *constrains* a grouping, it does not *decide* one.
> "`Option` lives inside the `Question` aggregate" is one grouping's choice and belongs in the
> grouping file.

---

## §3 — Rules

### 3.1 — Single-entity rules

Rules that inspect only fields of a single **entity** — not of a single aggregate. Whether that
entity shares an aggregate with anything else is a grouping question.

| Rule | Entity | Predicate |
|---|---|---|
| {RULE_NAME} | {Entity} | `{predicate in pseudo-code}` |

> Use `final` in the predicate column (e.g. "`Entity.field` is immutable — Java `final` field") for fields enforced by language immutability rather than a runtime check.

---

### 3.2 — Cross-entity rules

Rules that relate two or more entities.

> **Write every rule as a standing invariant over the domain**, in domain vocabulary — a statement
> that is either true or false of a domain state, with no mention of when or how it is checked.
> `∀rs ∈ Route.stations: rs.station has not been removed`, not "the referenced station was ACTIVE
> when the operation ran".
>
> Whether a grouping realises a given invariant transactionally, as an operation-time precondition,
> or by event propagation is recorded in the grouping's §5 Rule realisation table, and the pattern
> (P1–P4) is assigned from there by `/classify-and-plan`. None of that is written here: the same
> rule under a cascading grouping is a standing invariant and under a non-cascading one is a
> precondition, so stating the answer here would bind the domain to one grouping.

One block per rule. Use the exact shape below.

---

#### Rule: {RULE_NAME}

| Field | Value |
|---|---|
| Entities | {Entity1}, {Entity2} |
| Predicate | `{predicate in pseudo-code}` |

---

## §4 — Functionalities

> **§4 is a complete inventory of every operation the application exposes** - writes and reads alike.
> **One row per operation.** Nothing is omitted: an operation absent from §4 is an operation the AI
> agent will never implement, because §4 is the only source it has for functionalities.
>
> **Kind** — `Write` for an operation that changes state, `Read` for a query.
>
> **Primary Entity** — the entity that owns the main state change for a write, or the one being
> queried for a read.
>
> **Other Entities** — every other entity the operation reads or writes. `/classify-and-plan` joins
> these two columns against grouping §1 to derive the operation's primary and other *aggregates*:
> entities the grouping co-locates collapse to one aggregate, and an operation left with no other
> aggregate needs no saga coordination. Do not name aggregates here, and do not prescribe saga steps
> in the description.

| Functionality | Primary Entity | Other Entities | Kind | Description |
|---|---|---|---|---|
| {WriteFunctionalityName} | {PrimaryEntity} | {Entity1}, {Entity2} | Write | {One-sentence description of what the operation does, in domain terms} |
| {SingleEntityWriteName} | {PrimaryEntity} | — | Write | {One-sentence description} |
| Get{PrimaryEntity}ById | {PrimaryEntity} | — | Read | {One-sentence description of what the query returns} |

---
