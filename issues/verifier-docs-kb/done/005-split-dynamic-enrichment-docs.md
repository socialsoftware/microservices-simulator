## Parent PRD

`issues/verifier-docs-kb/prd.md`

## Type

AFK

## What to build

Restructure the dynamic-enrichment deep-dive material into the two reader levels described in the parent PRD's `Solution`, `Implementation Decisions`, and `Further Notes` sections. This slice should create a shorter overview for first-read comprehension and thesis use while preserving the existing technical depth in a detailed reference path.

## Acceptance criteria

- [x] The current dynamic-enrichment long-form material is split into a shorter overview and a preserved detailed reference.
- [x] The overview is suitable as the first reader path for understanding the problem, the implemented approach, the meaning of the result categories, and the current limits.
- [x] The detailed reference preserves the deeper technical material needed for implementation understanding and traceability.
- [x] The site navigation and page links make it easy to move between the overview and the detailed reference.
- [x] The restructuring improves readability without discarding important technical nuance.

## Blocked by

- Blocked by `issues/verifier-docs-kb/002-add-kb-home-and-reading-paths.md`

## User stories addressed

- User story 12
- User story 15
- User story 17
- User story 21
- User story 22
- User story 23

## Validation

- Moved the original long-form `docs/verifiers-impl/dynamic-enrichment-joining-explained.md` content to `docs/verifiers-impl/dynamic-enrichment-joining-reference.md`, preserving the detailed static pipeline, dynamic enrichment flow, join algorithm, ambiguity analysis, sidecar artifact notes, improvement ideas, overclaim warnings, thesis framing, and code references.
- Recreated `docs/verifiers-impl/dynamic-enrichment-joining-explained.md` as a shorter overview covering the problem, implemented approach, result categories, current evidence, current limits, and when to use the detailed reference.
- Updated `docs/verifiers-impl/README.md`, `docs/verifiers-impl/reading-guide.md`, and `mkdocs.verifier.yml` so the overview is the first dynamic-enrichment path and the detailed reference is directly reachable from both navigation and page links.
- Ran `./scripts/verifier-docs build`; MkDocs completed successfully with `Documentation built in ...`.
- Searched generated HTML for overview sections (`first-read overview`, `Problem`, `Implemented approach`, `Result categories`, `Current limits`) and reference sections (`preserved detailed reference`, `Static side, step by step`, `Join algorithm, step by step`, `What not to overclaim`, `Code references`), confirming readability improved while the technical material remains available.
- Searched generated HTML for `Dynamic Enrichment Overview` and `Dynamic Enrichment Detailed Reference`, confirming navigation exposes both levels and lets readers move between them.
