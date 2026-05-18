## Parent PRD

`issues/verifier-docs-kb/prd.md`

## Type

AFK

## What to build

Add the verifier glossary described in the parent PRD's `Solution`, `User Stories`, and `Implementation Decisions` sections. This slice should create a reusable terminology reference that reduces re-entry friction and helps align the main verifier entry pages around shared vocabulary.

## Acceptance criteria

- [x] The site includes a glossary page for core verifier terminology and recurring status labels.
- [x] The glossary covers the main concepts needed to read the verifier docs productively, including scenario/input terminology and dynamic-enrichment matching language.
- [x] Key verifier entry pages link to or otherwise guide readers toward the glossary.
- [x] The glossary improves terminology consistency across the main entry path rather than existing as an isolated reference page.

## Blocked by

- Blocked by `issues/verifier-docs-kb/002-add-kb-home-and-reading-paths.md`

## User stories addressed

- User story 10
- User story 15
- User story 20
- User story 22

## Validation

- Added `docs/verifiers-impl/glossary.md` with status labels, static scenario/input terms, dynamic-enrichment terms, join statuses, and future execution/search terms.
- Confirmed the glossary covers `ScenarioPlan`, scenario plan id, input variant, `InputVariant` id, source mode, dynamic input map, sidecar artifacts, and join statuses including `MATCHED_EXACT`, `MATCHED_HIGH_CONFIDENCE`, `AMBIGUOUS`, `UNMATCHED`, and `NOT_COVERED`.
- Linked the glossary from `docs/verifiers-impl/README.md`, `docs/verifiers-impl/reading-guide.md`, `docs/verifiers-impl/current-state.md`, and `docs/verifiers-impl/dynamic-enrichment-joining-explained.md`; also added it to primary navigation in `mkdocs.verifier.yml`.
- Ran `./scripts/verifier-docs build`; MkDocs completed successfully with `Documentation built in ...`.
- Searched generated HTML under `target/verifier-docs-site/` for `Glossary`, `ScenarioPlan`, `InputVariant`, `Source mode`, `Dynamic input map`, `MATCHED_EXACT`, and `NOT_COVERED`, confirming the glossary renders and is linked across the main entry path.
