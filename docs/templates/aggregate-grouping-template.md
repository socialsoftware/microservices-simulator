# {AppName} — Aggregate Grouping

> **Authored by the domain expert.** AI agents read this file as given — they do not change aggregate boundaries, decide which entities co-locate, or modify the event DAG. These are design decisions made by the domain experts.
>
> **One plain domain, N aggregate groupings.** This file captures *one* aggregate partitioning of
> [{AppName} domain model]({domain-model-file}.md). Several grouping files may exist over that one
> domain, and writing a second one must require **zero** edits to it. Everything that is a
> consequence of *this* decomposition — snapshots, technical fields, the event DAG, the consistency
> policy — belongs here and not there. No section names a per-rule pattern: `/classify-and-plan`
> derives each rule's pattern from §1, §2, §3.a and §4 together with the domain's §4 functionalities.
>
> Replace every `{placeholder}` with content specific to your application.

---

## §1 — Aggregate Grouping

Choose which entities are co-located. This decision determines which cross-entity rules in the domain model require event-based eventual consistency vs. can be enforced transactionally.

> **Entities contained** — domain entities, named exactly as domain §1 names them. Every entity of
> the plain domain appears in exactly one row.
>
> **Snapshot value objects** — classes *this grouping* introduces that are not domain entities:
> local copies of another aggregate's data, carrying the cached fields listed in §2. An entity of
> the plain domain that this grouping realises as a cached copy inside another aggregate is named
> here, not in `Entities contained`.

| Aggregate | Description | Entities contained | Snapshot value objects | Service |
|---|---|---|---|---|
| {AggregateName} | {One-sentence description of what this aggregate represents.} | {Entity1}, {Entity2} | {SnapshotClass1}, or — | {AggregateName}Service |

---

## §2 — Snapshots

For each aggregate that references an entity in a **different** aggregate, list the fields it must cache locally. Omit aggregates that reference no external entities.

> This section is the sole home of `{entity}AggregateId` and `{entity}Version` fields and of every
> field copied from another aggregate. None of them appear in the plain domain.
>
> **Updated on event** — name the domain event (defined in §4 below) that triggers the cache refresh. The AI agent uses this column to wire the correct event subscriptions automatically.
> If the source aggregate's fields are immutable (Java `final`), write `n/a — {SourceAggregate} fields are immutable`. No event subscription is needed; the snapshot is seeded once at the consuming aggregate's creation time via a direct service call in the creation saga.

| Aggregate | Snapshots of | Fields cached | Updated on event |
|---|---|---|---|
| {AggregateName} | {ExternalAggregate} | `{field1}`, `{field2}` | `{EventName}` |

---

## §2.b — Technical fields

Fields that exist for implementation reasons rather than domain reasons, and are therefore a
property of this grouping rather than of the domain. One row each, with the reason stated.

| Aggregate | Field | Why |
|---|---|---|
| {AggregateName} | `{field}: {Type}` | {Why this grouping needs it} |

> **Soft-delete state** — do not add a `state` field to any row here either. The simulator's
> `Aggregate` base class provides `state: AggregateState` (`ACTIVE`, `INACTIVE`, `DELETED`) and sets
> it via `remove()`. A plain-domain rule that says "a `{Entity}` that has been removed" is realised
> against this inherited field.

---

## §3 — Upstream / Downstream Event Dependencies

List pairs where the downstream entity must subscribe to the upstream entity's events when they are in different aggregates. If two entities are co-located in the grouping above, omit the arrow.

> **AI agent reads this as the topology map.** Each arrow `A ──► B` means B caches A's fields locally. If A's fields can change, B also subscribes to A's events (defined in §4) to keep the snapshot current. If A's fields are immutable, include the arrow but omit an event row in §4 — the snapshot is seeded once at creation. Do not omit arrows.

```
{UpstreamAggregate} ──────────────────────────► {DownstreamAggregate}
{UpstreamAggregate} ──────────────────────────► {DownstreamAggregate}
```

> An arrow `A ──► B` means: B must subscribe to A's events and cache the relevant A fields locally.

### 3.a — Consistency policy: {cascade | no cascade}

The heading names the policy, and `/classify-and-plan` reads it: the words after the colon start with
`cascade` or `no cascade`, optionally followed by a comma and a qualifier (`cascade, via events`).
This subsection is required; a grouping without it is incomplete.

- **Cascade** — a change or removal upstream is propagated downstream by the events in §4, so the
  domain's standing invariants are maintained continuously and are eventually consistent.
- **No cascade** — nothing is propagated, so each standing invariant is instead checked at operation
  time; a later upstream removal is tolerated and the invariant is not restored.

Then say what the policy does to each **family** of the plain domain's §3.2 rules (existence,
uniqueness, capacity, ...), in this vocabulary:

- **intra** — the family resolves inside one aggregate, because its entities are co-located in §1 or
  because every field it reads is cached in a §2 snapshot that no event in §4 changes.
- **precondition** — it is checked when the operation runs, against data a saga fetches, and a later
  upstream change is not repaired.
- **eventual** — an event in §4 repairs it after an upstream change. Under a cascade this is usually
  *in addition to* the operation-time check, not instead of it.

The vocabulary is for the reader. `/classify-and-plan` does not read these words: it derives each
rule's pattern from §1, §2 and §4, so the prose must agree with those sections, not substitute for
them. Where the policy deliberately leaves a split rule unrepaired under a cascade (a frozen value,
an immutable reference), name the rule and the reason here. The planner flags every such rule
`needs review - no repair event`, and this sentence is what the human reads to confirm it.

---

## §4 — Events

Name every domain event published by each upstream aggregate.

> **Trigger** — the service operation (or state change) that causes the event to be published.
> **Payload** — the fields the event carries (derived from §2 snapshot fields of the consumer(s)).
> **Consumer(s)** — which aggregates subscribe to this event (must match arrows in §3).

| Event | Publisher | Trigger | Payload fields | Consumer(s) |
|---|---|---|---|---|
| `{EventName}` | {PublisherAggregate} | {operation that fires it} | `{field1}`, `{field2}` | {ConsumerAggregate1}, {ConsumerAggregate2} |

> **Anchor field:** One payload field must be the publisher aggregate's own ID (the **anchor**). This field is passed to `super(anchorAggregateId)` in the event constructor and must match the `subscribedAggregateId` used in the corresponding `EventSubscription` subclass. Without this, event filtering is broken. See [`docs/concepts/events.md`](../../docs/concepts/events.md) canonical wiring for the exact pattern.

---

## §5 — Cross-file notes

Anything that is a consequence of this decomposition rather than of the domain: snapshot coherence
obligations, why a guard is phrased pre-mutation, which reads a given saga declares,
reference-by-aggregate-id conventions. Do not list the domain's §3.2 rules here with a pattern or a
per-rule label: the pattern is derived at planning time, and a hand-written copy only drifts from it.

---
