# Intake

## Goal

Create a thesis-friendly knowledge base and on-demand web docs experience for `docs/verifiers-impl/`, using MkDocs Material and Tailscale-accessible live preview from the repository root.

## Context

The current verifier docs already have strong content and roles:

- `README.md` defines a reading order.
- `current-state.md` is intended to be the live source of truth.
- `roadmap.md` captures future stages.
- `decisions/` holds durable architectural choices.
- `implementation-log/` and `meeting-notes/` preserve chronology and thesis/advisor framing.
- `archive/` preserves historical notes.

Current pain points:

- the docs feel more like a folder of markdown files than a cohesive knowledge base;
- major deep dives, especially dynamic-enrichment material, are too heavy as first-read entry points;
- live truth, historical progress, and thesis framing are all present, but not yet clearly layered in a browsable site;
- there is no docs-site tooling in the repo yet;
- Tailscale is already active on the remote box, `uv` is available, and `mkdocs` is not yet installed.

The primary audience is:

1. Andre, for fast re-entry and thesis writing
2. advisor, for browsing the narrative and evidence
3. collaborators/agents, for implementation context

## Decisions

- Scope content ownership to `docs/verifiers-impl/`.
- Allow minimal site machinery outside that folder when needed, such as root-level `mkdocs` config and a root script.
- Build a curated knowledge-base site rather than exposing every markdown file equally.
- Use MkDocs Material as the site engine.
- Use `uv` as the preferred local execution/install path.
- Provide an on-demand root-level command to start the docs server.
- Make the live server reachable privately over Tailscale; no public exposure.
- Support both live preview and static export, with live preview as the primary flow.
- Include new knowledge-base pages in v1, such as a start page, reading guide, glossary, thesis claims/evidence map, and folder landing pages.
- Allow targeted rewrites and splitting of high-value docs, especially the dynamic-enrichment deep dive.
- Keep historical materials accessible, but clearly framed as timeline/history rather than canonical present truth.
- Prefer de-duplication by role and linking over deleting useful historical material.
- Use a serious, clean, pleasant technical-docs tone.
- Keep CI publishing, always-on deployment, and public hosting out of scope for v1.

## Recommended Defaults Accepted

- v1 includes only verifier docs content, not global docs or DSL docs.
- The site optimizes first for fast re-entry and thesis extraction.
- The docs server is on-demand, started manually from repo root.
- A repo-managed wrapper command is preferred over a raw long command.
- Navigation is curated, not a flat dump of every file.
- Some lower-value prompt/handoff docs may be de-emphasized in primary nav.
- File renames/moves are allowed if they improve the site structure.

## Out Of Scope

- Editing DSL docs or other colleagues' documentation areas.
- Broad cleanup of root `README.md`, `docs/architecture.md`, or `docs/concepts/*`.
- Tailscale installation or account/network administration.
- Public hosting or internet exposure.
- CI/CD publishing pipelines.
- Permanent background service management for the docs server.
- GitHub issue creation or any external issue tracker sync.

## Risks And Tradeoffs

- Rewriting/splitting major docs improves readability, but risks temporary disorientation and link churn.
- Strong curation improves entry-point clarity, but may hide useful niche material unless secondary navigation is designed carefully.
- Keeping both current-truth docs and weekly/advisor timelines preserves thesis evidence, but requires disciplined boundary-setting to avoid duplication drift.
- Using `uv` is a pragmatic fit for this box, but the plan should avoid assuming Python packaging state that is not yet present.
- On-demand serving is simpler than always-on hosting, but requires a smooth startup command and clear instructions to avoid friction.
