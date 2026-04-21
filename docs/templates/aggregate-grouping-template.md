# {AppName} — Aggregate Grouping

> **Authored by the domain expert.** AI agents read this file as given — they do not change aggregate boundaries, decide which entities co-locate, or modify the event DAG. These are design decisions made by the domain experts.
>
> This file captures the aggregate partitioning decision for [{AppName} domain model]({domain-model-file}.md).
> Replace every `{placeholder}` with content specific to your application.
> One domain model can have multiple aggregate grouping files.
>
> Example: [`applications/quizzes/quizzes-aggregate-grouping.md`](../../applications/quizzes/quizzes-aggregate-grouping.md)

---

## §1 — Aggregate Grouping

Choose which entities are co-located. This decision determines which cross-entity rules in the domain model require event-based eventual consistency vs. can be enforced transactionally.

| Aggregate | Description | Entities contained | Service |
|---|---|---|---|
| {AggregateName} | {One-sentence description of what this aggregate represents.} | {Entity1}, {Entity2} | {AggregateName}Service |

---

## §2 — Snapshots

For each aggregate that references an entity in a **different** aggregate, list the fields it must cache locally. Omit aggregates that reference no external entities.

> **Updated on event** — name the domain event (defined in §4 below) that triggers the cache refresh. The AI agent uses this column to wire the correct event subscriptions automatically.
> If the source aggregate's fields are immutable (Java `final`), write `n/a — {SourceAggregate} fields are immutable`. No event subscription is needed; the snapshot is seeded once at the consuming aggregate's creation time via a direct service call in the creation saga.

| Aggregate | Snapshots of | Fields cached | Updated on event |
|---|---|---|---|
| {AggregateName} | {ExternalAggregate} | `{field1}`, `{field2}` | `{EventName}` |

---

## §3 — Upstream / Downstream Event Dependencies

List pairs where the downstream entity must subscribe to the upstream entity's events when they are in different aggregates. If two entities are co-located in the grouping above, omit the arrow.

> **AI agent reads this as the topology map.** Each arrow `A ──► B` means B caches A's fields locally. If A's fields can change, B also subscribes to A's events (defined in §4) to keep the snapshot current. If A's fields are immutable, include the arrow but omit an event row in §4 — the snapshot is seeded once at creation. Do not omit arrows.

```
{UpstreamAggregate} ──────────────────────────► {DownstreamAggregate}
{UpstreamAggregate} ──────────────────────────► {DownstreamAggregate}
```

> An arrow `A ──► B` means: B must subscribe to A's events and cache the relevant A fields locally.

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
