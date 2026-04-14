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

> **Owns** — value objects with no independent identity; created and deleted with the entity.
> **immutable** — add this annotation to any field that must not change after creation.
> **technical** — add this annotation to fields that exist for implementation reasons (e.g. `lastModifiedTime`), not domain reasons.
> **[snapshot from AggregateX]** — use this annotation for fields that are locally cached copies of data owned by a different aggregate. The AI agent will wire the corresponding event subscription. Example: `courseName: String [snapshot from Course]`.

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

> **AI agent implementation:** All §3.1 rules become **Layer 1 intra-invariants** inside `verifyInvariants()`. Use `/intra-invariant`.

| Rule | Entity | Predicate |
|---|---|---|
| {RULE_NAME} | {Entity} | `{predicate in pseudo-code}` |

> Use `final` in the predicate column (e.g. "`Entity.field` is immutable — Java `final` field") for fields enforced by language immutability rather than a runtime check.

---

### 3.2 — Cross-entity rules

> **AI agent implementation:** The AI agent classifies each §3.2 rule into Layer 3, 5, or 6 using `docs/concepts/decision-guide.md` and confirms the classification with the user before writing any code. Domain experts write the rules; the AI decides the layer.

One block per rule. Use the exact three-field shape below.

---

#### Rule: {RULE_NAME}

| Field | Value |
|---|---|
| Entities | {Entity1}, {Entity2} |
| Predicate | `{predicate in pseudo-code}` |

---
