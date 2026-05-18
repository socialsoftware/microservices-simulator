# Verifier knowledge base

This is the primary entry point for verifier and scenario-generation documentation. It is the curated knowledge base for current implementation status, roadmap context, technical deep dives, durable decisions, engineering history, advisor-facing history, and archived provenance.

If you are returning after a gap, read [`current-state.md`](current-state.md) first, then use [`reading-guide.md`](reading-guide.md) to choose the next path.

Use [`glossary.md`](glossary.md) when refreshing terminology such as `ScenarioPlan`, input variant, source mode, dynamic input map, or join status.

Use [`thesis-claims-evidence-map.md`](thesis-claims-evidence-map.md) when drafting or checking thesis claims against supporting evidence and limitations.

## Primary paths

| Need | Start here |
|---|---|
| What is true now | [`current-state.md`](current-state.md) |
| How to read the docs by purpose | [`reading-guide.md`](reading-guide.md) |
| Core terminology | [`glossary.md`](glossary.md) |
| Thesis claims and supporting evidence | [`thesis-claims-evidence-map.md`](thesis-claims-evidence-map.md) |
| What comes next | [`roadmap.md`](roadmap.md) |
| High-level verifier narrative | [`verifier-pipeline-plain-explanation.md`](verifier-pipeline-plain-explanation.md) |
| Test-to-input extraction detail | [`test-analysis-saga-input-flow.md`](test-analysis-saga-input-flow.md) |
| Static/dynamic evidence joining overview | [`dynamic-enrichment-joining-explained.md`](dynamic-enrichment-joining-explained.md) |
| Static/dynamic evidence joining reference | [`dynamic-enrichment-joining-reference.md`](dynamic-enrichment-joining-reference.md) |
| Durable design rationale | [`decisions/`](decisions/index.md) |
| Recent engineering history | [`implementation-log/`](implementation-log/index.md) |
| Advisor/thesis history | [`meeting-notes/`](meeting-notes/index.md) |
| Archived provenance | [`archive/`](archive/index.md) |

## Site workflow

From the repository root, start the live verifier docs site with:

```bash
./scripts/verifier-docs serve
```

The preview listens on `0.0.0.0:8000` by default for private reading over Tailscale. Build the static site with:

```bash
./scripts/verifier-docs build
```

See [`site-workflow.md`](site-workflow.md) for the command contract and access assumptions.

## Read order

For verifier work, read these files in order:

1. [`current-state.md`](current-state.md) — compact live implementation status, known limits, and next priorities.
2. [`reading-guide.md`](reading-guide.md) — purpose-based reading paths and document roles.
3. [`glossary.md`](glossary.md) — recurring verifier terminology and status labels.
4. [`thesis-claims-evidence-map.md`](thesis-claims-evidence-map.md) — claim-to-evidence map with limitations and support paths.
5. [`roadmap.md`](roadmap.md) — planned pipeline stages and how current work maps to the broader solution.
6. The latest weekly log under [`implementation-log/`](implementation-log/index.md) — recent progress, validation, deferred work, and meeting notes.
7. Decision records under [`decisions/`](decisions/index.md) — durable architectural choices and rejected alternatives.
8. [`dynamic-enrichment-joining-explained.md`](dynamic-enrichment-joining-explained.md) — shorter overview of static/dynamic joining, result categories, current evidence, and limits.
9. [`dynamic-enrichment-joining-reference.md`](dynamic-enrichment-joining-reference.md) — preserved detailed walkthrough of static inputs, runtime evidence, the dynamic input map, join statuses, ambiguity, and current `inputVariantId` propagation.
10. [`site-workflow.md`](site-workflow.md) — how to preview and build the verifier docs site.
11. [`archive/`](archive/index.md) — historical source material preserved for traceability, not current instructions.

## Documentation roles

| Area | Purpose |
|---|---|
| `current-state.md` | Source of truth for what is implemented, partial, not implemented, and next. |
| `reading-guide.md` | Purpose-based guide for choosing current-state, roadmap, decisions, logs, meeting notes, and archive material. |
| `glossary.md` | Shared terminology for scenario generation, source modes, dynamic enrichment, join statuses, and status labels. |
| `thesis-claims-evidence-map.md` | Thesis claim drafts mapped to support docs, validation evidence, and limitations. |
| `roadmap.md` | High-level staged plan from static scenario synthesis to execution and search. |
| `implementation-log/YYYY-WW.md` | Weekly progress record, including what was not done. |
| `decisions/YYYY-MM-DD-topic.md` | Lasting rationale for important technical choices. |
| `meeting-notes/YYYY-WW-thesis-meeting.md` | Advisor-facing history and thesis framing at a point in time. |
| `dynamic-enrichment-joining-explained.md` | First-read overview of static/dynamic joining, result categories, current evidence, and limits. |
| `dynamic-enrichment-joining-reference.md` | Preserved detailed technical reference for static/dynamic joining, ambiguity causes, and recommended matching improvements. |
| `site-workflow.md` | Public live-preview and static-build workflow for this verifier documentation site. |
| `archive/legacy-notes/` | Raw or superseded notes kept for provenance. Do not treat as live guidance. |
| `scenario-catalog-followup-prompt.md` | Prompt-style handoff material. Kept reachable, but intentionally de-emphasized in the main reading path. |

## Update policy

For meaningful changes in `verifiers/` or verifier-oriented `applications/dummyapp/` fixtures:

- Update the current weekly implementation log.
- Update `current-state.md` when the implementation status, known limitations, validation baseline, or next priorities change.
- Update `roadmap.md` when a roadmap stage changes status or scope.
- Add a decision note only for durable architectural choices, rejected alternatives, or scope boundaries.

Each weekly log should explicitly include a **Not done / deferred** section so meeting summaries can distinguish progress from remaining work.
