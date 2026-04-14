# {AppName} — Aggregate Grouping

> **Authored by the domain expert.** AI agents read this file as given — they do not change aggregate boundaries, decide which entities co-locate, or modify the event DAG. These are design decisions made by the domain experts.
>
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

> **Format for each snapshot field:** specify the source aggregate, source field name, and which event triggers an update. The AI agent uses this to wire the correct event subscriptions automatically.
> Example row: `Execution | Course | courseName, courseType | updated on: CourseUpdatedEvent`

| Aggregate | Snapshots of | Fields cached | Updated on event |
|---|---|---|---|
| {AggregateName} | {ExternalAggregate} | `{field1}`, `{field2}` | `{TriggerEvent}` |

---

## §3 — Upstream / Downstream Event Dependencies

List pairs where the downstream entity must subscribe to the upstream entity's events when they are in different aggregates. If two entities are co-located in the grouping above, omit the arrow.

> **AI agent reads this as the canonical list of events to implement.** Each arrow becomes one invocation of `/new-event` (and possibly `/inter-invariant`) during Phase 4 of `/new-application`. Do not omit arrows — the AI will not infer them from the snapshot table alone.

```
{UpstreamAggregate} ──────────────────────────► {DownstreamAggregate}
{UpstreamAggregate} ──────────────────────────► {DownstreamAggregate}
```

> An arrow `A ──► B` means: B must subscribe to A's events and cache the relevant A fields locally.

---
