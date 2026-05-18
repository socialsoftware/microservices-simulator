## Parent PRD

`issues/verifier-docs-kb/prd.md`

## Type

AFK

## What to build

Clarify the documentation-role boundaries described in the parent PRD's `Problem Statement`, `Solution`, and `Implementation Decisions` sections. This slice should make canonical current truth, engineering history, advisor/thesis history, durable decisions, and archive material visibly distinct while preserving access to all historical content.

## Acceptance criteria

- [x] Canonical current-truth entry points clearly identify themselves as the present-tense source of truth.
- [x] The site includes landing pages or equivalent entry points that explain how to use decisions, implementation logs, meeting notes, and archive material.
- [x] Historical engineering and advisor-facing materials remain fully reachable after the restructuring.
- [x] A reader can quickly tell where to look for "what is true now", "what changed last week", and "how this was framed for advisor/thesis discussion".
- [x] The clarified roles reduce duplication by guidance and linking rather than removing useful historical content.

## Blocked by

- Blocked by `issues/verifier-docs-kb/002-add-kb-home-and-reading-paths.md`

## User stories addressed

- User story 6
- User story 7
- User story 8
- User story 9
- User story 14
- User story 18
- User story 19
- User story 20
- User story 23

## Validation

- Updated `docs/verifiers-impl/current-state.md` to identify itself as the present-tense source of truth before roadmap notes, weekly logs, meeting notes, decision records, or archive material.
- Added role landing pages at `docs/verifiers-impl/decisions/index.md`, `docs/verifiers-impl/implementation-log/index.md`, `docs/verifiers-impl/meeting-notes/index.md`, and `docs/verifiers-impl/archive/index.md` explaining when to use each document family.
- Updated `docs/verifiers-impl/README.md` and `docs/verifiers-impl/reading-guide.md` so readers can distinguish current truth, recent engineering changes, advisor/thesis framing, decisions, and archive material by links and guidance rather than deleting historical content.
- Updated `mkdocs.verifier.yml` so historical engineering logs, advisor meeting notes, decisions, and archive/handoff pages remain reachable through curated navigation.
- Ran `./scripts/verifier-docs build`; MkDocs completed successfully with `Documentation built in ...`.
- Searched generated HTML for `present-tense source of truth`, `what changed`, `advisor-facing`, `Decision records`, and `Archive and Handoff`, confirming the role labels and historical entry paths render in the built site.
