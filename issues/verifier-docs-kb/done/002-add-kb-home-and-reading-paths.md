## Parent PRD

`issues/verifier-docs-kb/prd.md`

## Type

AFK

## What to build

Add the primary knowledge-base entry path described in the parent PRD's `Solution`, `User Stories`, and `Implementation Decisions` sections. This slice should make the verifier docs feel like a curated knowledge base by adding a clear home page, a reading guide, and curated navigation that surfaces the most important verifier sections first.

## Acceptance criteria

- [x] The site has a clear verifier-docs start page that works as the primary entry point.
- [x] The site has a reading-guide page that explains how to read the verifier docs by purpose.
- [x] Primary navigation is curated around the intended verifier sections instead of treating every markdown file as equally prominent.
- [x] Lower-value prompt or handoff material is de-emphasized in primary navigation without becoming inaccessible.
- [x] A reader can navigate from the primary entry path to current state, roadmap, major deep dives, decisions, history, and archive without manual URL editing.

## Blocked by

- Blocked by `issues/verifier-docs-kb/001-launch-verifier-docs-site.md`

## User stories addressed

- User story 1
- User story 2
- User story 9
- User story 13
- User story 15
- User story 17
- User story 20
- User story 25

## Validation

- Updated `docs/verifiers-impl/README.md` into the verifier knowledge-base start page with primary paths to current state, reading guide, roadmap, deep dives, decisions, engineering history, advisor history, and archive.
- Added `docs/verifiers-impl/reading-guide.md`, organized by reader purpose: current truth, narrative overview, durable rationale, engineering history, advisor/thesis history, and provenance.
- Updated `mkdocs.verifier.yml` with curated primary navigation: Start, Reading Guide, Current State, Roadmap, Site Workflow, Deep Dives, Decisions, History, and Archive and Handoff.
- De-emphasized `scenario-catalog-followup-prompt.md` by placing it under `Archive and Handoff` instead of the primary reading path while keeping it reachable in the site.
- Ran `./scripts/verifier-docs build`; MkDocs completed successfully with `Documentation built in ...` and no missing-nav warnings.
- Inspected generated output for `target/verifier-docs-site/reading-guide/index.html` and searched generated HTML for `Reading Guide`, `Current State`, `Dynamic Enrichment Joining`, `Archive and Handoff`, and `Scenario Catalog Follow-up Prompt`, confirming the entry path and de-emphasized handoff page are reachable through site navigation.
