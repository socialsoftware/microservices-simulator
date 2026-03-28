# {AppName} — Aggregate Grouping

> This file captures the aggregate partitioning decision for [{AppName} domain model]({domain-model-file}.md).
> Replace every `{placeholder}` with content specific to your application.
> One domain model can have multiple aggregate grouping files (e.g. monolith vs. microservices).
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

| Aggregate | Snapshots of | Fields cached |
|---|---|---|
| {AggregateName} | {ExternalAggregate} × N | `{field1}`, `{field2}` |

---

## §3 — Upstream / Downstream Event Dependencies

List pairs where the downstream entity must subscribe to the upstream entity's events when they are in different aggregates. If two entities are co-located in the grouping above, omit the arrow.

```
{UpstreamEntity} ──────────────────────────► {DownstreamEntity}
{UpstreamEntity} ──────────────────────────► {DownstreamEntity}
```

> An arrow `A ──► B` means: B must subscribe to A's events and cache the relevant A fields locally.

---
