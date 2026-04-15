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

| Aggregate | Entities contained | Service |
|---|---|---|
| {AggregateName} | {Entity1}, {Entity2} | {AggregateName}Service |

---

## §2 — Snapshots

For each aggregate that references an entity in a **different** aggregate, list the fields it must cache locally. Omit aggregates that reference no external entities.

> **Updated on event** — name the domain event (defined in §4 below) that triggers the cache refresh. The AI agent uses this column to wire the correct event subscriptions automatically.

| Aggregate | Snapshots of | Fields cached | Updated on event |
|---|---|---|---|
| {AggregateName} | {ExternalAggregate} | `{field1}`, `{field2}` | `{EventName}` |

---

## §3 — Upstream / Downstream Event Dependencies

List pairs where the downstream entity must subscribe to the upstream entity's events when they are in different aggregates. If two entities are co-located in the grouping above, omit the arrow.

> **AI agent reads this as the topology map.** Each arrow `A ──► B` means B subscribes to one or more events from A. The actual event names and payloads are defined in §4. Do not omit arrows.

```
{UpstreamAggregate} ──────────────────────────► {DownstreamAggregate}
{UpstreamAggregate} ──────────────────────────► {DownstreamAggregate}
```

> An arrow `A ──► B` means: B must subscribe to A's events and cache the relevant A fields locally.

---

## §4 — Events

Name every domain event published by each upstream aggregate. The AI agent uses this table as the canonical input for each `/inter-invariant` call in Phase 4 of `/new-application`. There must be one row per event — if two consumers subscribe to the same event, they share a single row here.

> **Trigger** — the service operation (or state change) that causes the event to be published.
> **Payload** — the fields the event carries (derived from §2 snapshot fields of the consumer(s)).
> **Consumer(s)** — which aggregates subscribe to this event (must match arrows in §3).

| Event | Publisher | Trigger | Payload fields | Consumer(s) |
|---|---|---|---|---|
| `{EventName}` | {PublisherAggregate} | {operation that fires it} | `{field1}`, `{field2}` | {ConsumerAggregate1}, {ConsumerAggregate2} |

---
