# Verifier implementation documentation

This directory is the durable knowledge base for the verifier and scenario-generation work.
It is intended for both humans and coding agents.

## Read order

For verifier work, read these files in order:

1. [`current-state.md`](current-state.md) — compact live implementation status, known limits, and next priorities.
2. [`roadmap.md`](roadmap.md) — planned pipeline stages and how current work maps to the broader solution.
3. The latest weekly log in [`implementation-log/`](implementation-log/) — recent progress, validation, deferred work, and meeting notes.
4. [`decisions/`](decisions/) — durable architectural choices and rejected alternatives.
5. [`dynamic-enrichment-joining-explained.md`](dynamic-enrichment-joining-explained.md) — plain-language explanation of static inputs, runtime evidence, join statuses, ambiguity, and `inputVariantId` propagation.
6. [`archive/`](archive/) — historical source material preserved for traceability, not current instructions.

## Documentation roles

| Area | Purpose |
|---|---|
| `current-state.md` | Source of truth for what is implemented, partial, not implemented, and next. |
| `roadmap.md` | High-level staged plan from static scenario synthesis to execution and search. |
| `implementation-log/YYYY-WW.md` | Weekly progress record, including what was not done. |
| `decisions/YYYY-MM-DD-topic.md` | Lasting rationale for important technical choices. |
| `dynamic-enrichment-joining-explained.md` | Static/dynamic joining examples and recommended low-intrusion matching improvements. |
| `archive/` | Raw or superseded notes kept for provenance. Do not treat as live guidance. |

## Update policy

For meaningful changes in `verifiers/` or verifier-oriented `applications/dummyapp/` fixtures:

- Update the current weekly implementation log.
- Update `current-state.md` when the implementation status, known limitations, validation baseline, or next priorities change.
- Update `roadmap.md` when a roadmap stage changes status or scope.
- Add a decision note only for durable architectural choices, rejected alternatives, or scope boundaries.

Each weekly log should explicitly include a **Not done / deferred** section so meeting summaries can distinguish progress from remaining work.
