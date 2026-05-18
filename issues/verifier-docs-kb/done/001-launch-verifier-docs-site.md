## Parent PRD

`issues/verifier-docs-kb/prd.md`

## Type

AFK

## What to build

Create the first usable end-to-end verifier docs site workflow described in the parent PRD's `Solution`, `Implementation Decisions`, and `Testing Decisions` sections. A reader should be able to start a live preview from the repository root and build a static site without manually composing raw generator commands. This slice establishes the minimal MkDocs Material site workflow and the intended private Tailscale-friendly access pattern.

## Acceptance criteria

- [x] From the repository root, one documented public command starts the verifier docs live-preview workflow.
- [x] The live preview is configured for the intended private remote-reading flow over Tailscale rather than being limited to localhost-only access.
- [x] A documented public build command produces a static verifier docs site successfully.
- [x] The generated site renders a minimal working verifier docs site rather than a raw directory listing.
- [x] The live-preview and build workflows can be verified through observable command output and generated site behavior.

## Blocked by

None - can start immediately

## User stories addressed

- User story 2
- User story 3
- User story 4
- User story 5
- User story 16
- User story 24
- User story 25

## Validation

- Added `mkdocs.verifier.yml`, `scripts/verifier-docs`, and `docs/verifiers-impl/site-workflow.md` so the documented root commands are `./scripts/verifier-docs serve` and `./scripts/verifier-docs build`.
- Confirmed the live-preview workflow is configured for private remote reading by default through `VERIFIER_DOCS_ADDR:-0.0.0.0:8000`, documented in `README.md`, `docs/verifiers-impl/README.md`, and `docs/verifiers-impl/site-workflow.md`.
- Ran `./scripts/verifier-docs build`; MkDocs reported `Documentation built in ...` and generated `target/verifier-docs-site/`.
- Inspected `target/verifier-docs-site/index.html`; it is a Material-generated HTML page titled `Verifier Knowledge Base`, not a raw directory listing.
- Ran `timeout 8s ./scripts/verifier-docs serve`; observable output included `Serving on http://0.0.0.0:8000/`.
