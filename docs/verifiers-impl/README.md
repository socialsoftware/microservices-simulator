# Verifier docs

This directory documents the verifier/scenario-generation work. Keep the main path short; use reference and archive material only when needed.

## Start here

| Need | Read |
|---|---|
| Current implementation status | [`current-state.md`](current-state.md) |
| Thesis/advisor meeting prep | [`advisor-brief.md`](advisor-brief.md) |
| Concrete validation evidence and run metrics | [`evidence.md`](evidence.md) |
| Planned stages and remaining work | [`roadmap.md`](roadmap.md) |
| Thesis-safe claims | [`thesis-claims-evidence-map.md`](thesis-claims-evidence-map.md) |
| Plain pipeline explanation | [`verifier-pipeline-plain-explanation.md`](verifier-pipeline-plain-explanation.md) |
| Terminology | [`glossary.md`](glossary.md) |

## Reference material

| Topic | Read |
|---|---|
| Test-to-input extraction | [`reference/source-to-input-flow.md`](reference/source-to-input-flow.md) |
| Structured input recipes | [`reference/input-recipes.md`](reference/input-recipes.md) |
| Dynamic enrichment overview | [`reference/dynamic-enrichment.md`](reference/dynamic-enrichment.md) |
| Dynamic enrichment detailed reference | [`archive/investigations/dynamic-enrichment-joining-reference.md`](archive/investigations/dynamic-enrichment-joining-reference.md) |
| ScenarioExecutor | [`reference/scenario-executor.md`](reference/scenario-executor.md) |
| Durable design decisions | [`decisions/`](decisions/index.md) |

## Historical material

| Material | Location |
|---|---|
| Engineering logs | [`archive/implementation-log/`](archive/implementation-log/index.md) |
| Advisor/meeting history | [`archive/meeting-notes/`](archive/meeting-notes/index.md) |
| Archived notes | [`archive/`](archive/index.md) |

Historical notes are useful for provenance, but they are not current truth. Check [`current-state.md`](current-state.md) before making thesis claims.

## Site workflow

Preview the docs site from the repository root:

```bash
./scripts/verifier-docs serve
```

Build the static site:

```bash
./scripts/verifier-docs build
```

See [`site-workflow.md`](site-workflow.md) for details.

## Update policy

For meaningful verifier changes:

1. Update [`current-state.md`](current-state.md) if implemented status, limitations, evidence, or next priorities changed.
2. Update [`advisor-brief.md`](advisor-brief.md) before thesis meetings.
3. Update [`evidence.md`](evidence.md) when citing new runs or measurements.
4. Add a decision note only for durable architectural choices or rejected alternatives.
