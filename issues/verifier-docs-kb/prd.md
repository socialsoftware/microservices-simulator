## Problem Statement

The verifier documentation already contains substantial implementation knowledge, thesis framing, and historical evidence, but it is difficult to consume as a coherent knowledge base. The current material is distributed across live status documents, roadmap notes, decisions, weekly logs, meeting notes, and deep technical explainers. That structure is valuable, but it currently behaves more like a well-maintained folder of Markdown files than a browsable documentation product.

For the primary reader, the main friction is re-entry cost: returning to the verifier work after a gap requires remembering which document is canonical, which document is historical, which document contains the readable narrative, and which long-form deep dive contains the technical detail needed for thesis writing. The current shape also makes it harder to extract safe thesis claims and supporting evidence without re-reading overlapping material.

There is also no site-generation tooling yet for the verifier docs area. The desired reading experience is a clean local web site, reachable privately over Tailscale from a remote box, started on demand from the repository root, with both live preview and static export. The docs experience should stay scoped to the verifier work rather than expanding into project-wide documentation ownership.

## Solution

Create a verifier-only documentation site and knowledge-base structure that turns the existing Markdown corpus into a curated, thesis-friendly browsing experience. The site will be generated with MkDocs Material and run on demand from the repository root through a wrapper command that supports live preview over Tailscale and static build output.

The documentation itself will be reorganized as a knowledge base with clear roles:

- a short start page and reading guide for fast re-entry;
- a glossary for core verifier terminology;
- a thesis claims and evidence map for later writing;
- clearer separation between canonical current truth, durable design decisions, weekly engineering history, advisor/thesis history, and archive material;
- targeted restructuring of the hardest long-form entry points, especially dynamic-enrichment documentation, by splitting overview material from detailed technical reference.

The result should let the primary user answer four questions quickly:

1. Where am I now?
2. What is implemented, deferred, or out of scope?
3. Which documents support a thesis claim I want to write?
4. Where is the deep technical explanation if I need to go deeper?

## User Stories

1. As the verifier author, I want a single starting page for verifier docs, so that I can re-enter the work without remembering the current reading order from memory.
2. As the verifier author, I want the documentation to be browsable as a site instead of a raw folder tree, so that long reading sessions feel smoother and less mentally fragmented.
3. As the verifier author, I want an on-demand command from the repository root to launch the docs site, so that using the knowledge base becomes part of my normal workflow.
4. As the verifier author, I want the live docs server to be reachable privately over Tailscale, so that I can read and navigate the knowledge base from another machine while the repo stays on the remote box.
5. As the verifier author, I want a static build command in addition to live preview, so that the docs can later be archived or shared without redesigning the documentation workflow.
6. As the verifier author, I want canonical current-state material to be visually and structurally distinct from historical notes, so that I can tell what is true now without reconstructing it from a timeline.
7. As the verifier author, I want weekly engineering logs to remain intact and easy to browse, so that I can answer what changed in a specific week.
8. As the verifier author, I want meeting notes to remain accessible as a first-class history layer, so that advisor-facing framing is preserved for thesis preparation.
9. As the verifier author, I want a reading guide that explains when to use current-state, roadmap, decisions, logs, meeting notes, and archive, so that I do not need to infer each document's role every time.
10. As the verifier author, I want a glossary of recurring verifier terms, so that terminology such as `ScenarioPlan`, `InputVariant`, source modes, and join statuses is quick to refresh.
11. As the verifier author, I want a thesis claims and evidence map, so that I can move from a claim draft to the supporting documents, validation notes, and limitations efficiently.
12. As the verifier author, I want the dynamic-enrichment material to have a short overview and a preserved deep reference, so that I can choose the right level of detail for the moment.
13. As the verifier author, I want the site navigation to be curated rather than flat, so that the most useful entry points are obvious and lower-value prompt-style documents do not dominate the experience.
14. As the verifier author, I want archive material to stay available but clearly marked as non-current, so that historical provenance is preserved without confusing present-tense reading.
15. As the verifier author, I want long documents to provide better onward navigation, so that I can move from summary to detail without repeated manual searching.
16. As the verifier author, I want the docs site to reflect the scoped ownership boundary of verifier docs only, so that this effort does not turn into a repo-wide documentation rewrite.
17. As an advisor, I want a clean narrative path through the verifier work, so that I can understand the current state, the roadmap, and the significance of recent progress without digging through raw notes.
18. As an advisor, I want historical meeting notes to remain available, so that I can compare what was claimed at different stages of the work.
19. As an advisor, I want implementation claims to remain connected to validation and limitations, so that I can distinguish demonstrated progress from planned work.
20. As a collaborator or coding agent, I want the verifier docs site to show which pages are canonical and which are historical, so that I can use the docs correctly when making future changes.
21. As a collaborator or coding agent, I want deep technical references to remain preserved even after restructuring, so that no important implementation detail is lost during cleanup.
22. As a future thesis reader, I want the documentation tone to remain serious, clean, and technical, so that it supports academic writing rather than feeling like informal notes.
23. As the verifier author, I want doc restructuring to reduce duplication by role rather than deleting useful material, so that I keep traceability while still improving readability.
24. As the verifier author, I want root-level site machinery to stay minimal and separate from the content scope, so that content ownership remains centered on the verifier docs area.
25. As the verifier author, I want the site to be useful before any CI or permanent hosting exists, so that documentation value does not depend on infrastructure work.

## Implementation Decisions

- The knowledge base will remain content-scoped to the verifier documentation area, while allowing minimal supporting site machinery outside that area for configuration and launch ergonomics.
- The documentation product will be a curated site rather than an automatic exposure of every Markdown file at equal prominence.
- The site engine will be MkDocs Material.
- The preferred execution path will use `uv`, because it is already available on the target box and supports lightweight on-demand local tooling.
- The live preview workflow will be the primary interaction mode, with static export supported as a parallel capability.
- The root user experience will be a repository-managed wrapper command rather than a long raw generator command.
- The live docs server will be designed for private access over the already-running Tailscale network and will not target public internet exposure.
- The documentation information architecture will distinguish these roles explicitly: start/read-me-first guidance, current truth, roadmap, technical overviews, detailed references, decisions, engineering timeline, advisor/thesis timeline, and archive.
- The site navigation will be intentionally curated, including de-emphasis of lower-value prompt or handoff documents where that improves reader focus.
- New knowledge-base pages will be added to improve re-entry and thesis use, including a start page, a reading guide, a glossary, a thesis claims and evidence map, and landing pages for key historical/document families.
- Historical materials will remain accessible and first-class, but framed as timeline/history rather than current truth.
- Major deep dives may be split or renamed where doing so produces a clearer reader experience. Path stability is desirable but not the primary constraint.
- The dynamic-enrichment deep dive will be restructured into at least two reader levels: a shorter overview for comprehension and thesis usage, and a preserved detailed technical reference for implementation detail and traceability.
- De-duplication should happen by clarifying document roles and cross-linking, not by erasing useful historical detail.
- The work will optimize for fast re-entry and thesis extraction before broader team-onboarding or organization-wide documentation concerns.
- Visual polish will serve readability and seriousness rather than novelty; the site should feel pleasant and clean without introducing high-maintenance customization.

## Testing Decisions

- Good tests for this work should verify externally observable behavior through public interfaces, not internal formatting choices or generator implementation details.
- The primary tested public interfaces are the root-level docs workflows: launching the live preview, producing a static build, and surfacing the intended curated navigation and key entry pages.
- Testing should follow tracer-bullet thinking for later implementation work: first prove that the docs site can be started and browsed through the chosen public command, then extend coverage incrementally to navigation, build output, and critical knowledge-base entry points.
- High-value behavior to verify includes:
  - the root command starts the docs preview workflow successfully;
  - the live server is configured for the intended private remote-reading flow;
  - the build command produces a static site successfully;
  - curated primary navigation exposes the intended sections;
  - core knowledge-base pages are reachable and build cleanly;
  - historical sections remain reachable after restructuring;
  - the dynamic-enrichment overview/reference split remains navigable and preserves the deep technical path.
- Tests should avoid brittle assertions about exact prose wording, exact theme styling, or incidental Markdown structure that does not affect user-visible browsing behavior.
- Because this is documentation-product work rather than business logic, lightweight behavior checks, build verification, and navigation smoke tests are more valuable than heavy internal unit tests.
- Visual fine-tuning, exact search ranking behavior, and pixel-perfect appearance are not priority test targets for v1.
- When implementation work reaches the docs-site layer, prior art should favor the project's existing pragmatic verification style: simple reproducible commands and observable outputs over deep mocking or implementation-coupled checks.

## Out of Scope

- Editing or restructuring DSL documentation.
- Editing or restructuring project-wide documentation outside the verifier docs area, except for minimal site machinery needed to serve the verifier knowledge base.
- Public hosting, public URLs, or internet exposure.
- Tailscale installation, account setup, or broader network administration.
- CI/CD publishing pipelines.
- Permanent background service management or always-on hosting for the docs server.
- Turning the verifier docs site into a general portal for unrelated project documentation.
- Large-scale rewriting of every historical document solely for stylistic consistency.

## Further Notes

- The most important outcome is not merely generating a site; it is making the verifier knowledge base easier to re-enter, safer to cite in a thesis, and less overwhelming without losing technical depth.
- The documentation reorganization should preserve the distinction between present-tense truth and historical evidence, because both are valuable and serve different reading modes.
- The site should make it easier to move between summary, evidence, and deep implementation detail rather than replacing one of those layers with another.
