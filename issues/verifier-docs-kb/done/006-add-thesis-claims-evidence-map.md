## Parent PRD

`issues/verifier-docs-kb/prd.md`

## Type

AFK

## What to build

Add the thesis claims and evidence map described in the parent PRD's `Solution`, `User Stories`, `Implementation Decisions`, and `Further Notes` sections. This slice should provide a practical bridge from thesis claim drafting to supporting documents, validation evidence, and limitations across the rest of the knowledge base.

## Acceptance criteria

- [x] The site includes a page that maps core thesis claims to supporting verifier docs and validation evidence.
- [x] The map explicitly captures limitations or "what not to overclaim" guidance alongside the claims it supports.
- [x] The page links effectively into the clarified current-truth docs, historical evidence, glossary, and dynamic-enrichment overview/reference where relevant.
- [x] A reader can use the page as an entry point for drafting or checking thesis sections without reconstructing the support path manually.

## Blocked by

- Blocked by `issues/verifier-docs-kb/003-clarify-current-vs-history.md`
- Blocked by `issues/verifier-docs-kb/004-add-verifier-glossary.md`
- Blocked by `issues/verifier-docs-kb/005-split-dynamic-enrichment-docs.md`

## User stories addressed

- User story 11
- User story 17
- User story 19
- User story 22
- User story 23

## Validation

- Added `docs/verifiers-impl/thesis-claims-evidence-map.md` with a claim map that connects thesis claim drafts to supporting docs, validation evidence, and explicit `Do not overclaim` guidance.
- Included limitations beside each supported claim, including no current arbitrary executor, no static catalog redefinition by dynamic evidence, no generalization beyond the comparable Quizzes sagas-local baseline, and no claim that roadmap stages are implemented.
- Linked the map to current truth (`current-state.md`), historical evidence (`implementation-log/index.md`, `implementation-log/2026-W20.md`, `meeting-notes/index.md`, `archive/index.md`), glossary (`glossary.md`), and dynamic-enrichment overview/reference pages.
- Updated `docs/verifiers-impl/README.md`, `docs/verifiers-impl/reading-guide.md`, and `mkdocs.verifier.yml` so readers can use the map as a primary thesis drafting/checking entry point.
- Ran `./scripts/verifier-docs build`; MkDocs completed successfully with `Documentation built in ...`.
- Searched generated HTML for `Claim map`, `Do not overclaim`, `Evidence workflow`, `current-state`, `glossary`, `dynamic-enrichment-joining`, `implementation-log`, `meeting-notes`, and `Thesis Claims Evidence Map`, confirming the page renders and links into the required support paths.
