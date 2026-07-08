# 007 - ScenarioExecutor Docs

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `001-simulator-in-memory-fault-provider.md`, `002-executor-vector-validation-and-report-v2.md`, `003-no-fault-lifecycle-closure.md`, `004-realized-fault-compensation-and-masking.md`, `005-mismatch-unexpected-and-compensation-failures.md`, `006-cli-docker-runner-and-quizzes-smoke.md`  
ACs covered: `AC-23`  
Risk: `low`

## Purpose

Update verifier documentation after the feature lands so ScenarioExecutor is no longer described as a POC for the supported single-saga fault-vector path, while keeping unsupported and future areas explicit.

## Scope

- Rename `docs/verifiers-impl/reference/scenario-executor-poc.md` to `docs/verifiers-impl/reference/scenario-executor.md`.
- Update live references/navigation from `scenario-executor-poc.md` to `scenario-executor.md`.
- Replace "POC" framing with supported-scope language for materializable single-saga saga/local default and explicit binary fault-vector runs.
- Document CLI/Docker usage for default-vector and explicit-vector runs, including the explicit scenario-id requirement.
- Document v2 report status vocabulary at a reference level, without duplicating implementation internals unnecessarily.
- Update `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/README.md`, `docs/verifiers-impl/roadmap.md`, `docs/verifiers-impl/reading-guide.md`, `docs/verifiers-impl/advisor-brief.md`, `docs/verifiers-impl/thesis-claims-evidence-map.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/evidence.md` if new smoke evidence is cited, and `mkdocs.verifier.yml`.
- Keep unsupported/future work explicit: multi-saga, TCC, stream/gRPC/distributed parity, compensation-step faults, delay/non-binary impairments, batch/search/scoring/prioritization, generic reset, and impact telemetry.

## Out of Scope

- Updating archived meeting notes or historical implementation logs except if a live docs link breaks.
- Creating a root `CONTEXT.md`.
- Adding a new ADR unless implementation discovers a durable decision not already captured by the spec/decision frame.

## Repo Anchors

- `docs/verifiers-impl/reference/scenario-executor-poc.md` — source doc to rename and rewrite.
- `docs/verifiers-impl/README.md` — reference table link.
- `docs/verifiers-impl/current-state.md` — current truth status and limitations.
- `docs/verifiers-impl/roadmap.md` — Stage 2 status and status matrix.
- `docs/verifiers-impl/reading-guide.md` — prep path link.
- `docs/verifiers-impl/advisor-brief.md` — live meeting prep wording and links.
- `docs/verifiers-impl/thesis-claims-evidence-map.md` — thesis-safe claim wording and links.
- `docs/verifiers-impl/glossary.md` — Scenario execution/fault-vector terminology.
- `docs/verifiers-impl/evidence.md` — add smoke evidence only if S6 produced durable paths/commands worth citing.
- `mkdocs.verifier.yml` — navigation label/path.
- `CONTEXT-MAP.md` — reminder that glossary updates go to `docs/verifiers-impl/glossary.md`.

## Implementation Shape

- Rename the reference document and update live docs/nav references with targeted edits.
- Keep docs present-tense and evidence-backed: say the executor supports the specific materializable single-saga saga/local fault-vector path after implementation, not generic catalog replay.
- Keep the old Quizzes POC smoke as historical evidence only if useful, and clearly separate it from new v2 fault-vector smoke evidence.
- Use the canonical terms from the glossary: Scenario execution attempt, Fault space, Fault slot, Assigned vector, Realized fault slot, Masked fault slot.
- Make unsupported areas obvious so thesis claims remain safe.

## TDD / Test Shape

- First behavior to test: docs/nav no longer reference `reference/scenario-executor-poc.md` in live docs paths.
- Expected red failure: `rg "scenario-executor-poc|ScenarioExecutor POC" docs/verifiers-impl mkdocs.verifier.yml -g '!archive/**'` currently finds live references.
- Additional coverage:
  - docs build succeeds if MkDocs tooling is available;
  - glossary still routes through `CONTEXT-MAP.md` and no root `CONTEXT.md` is created;
  - docs include default and explicit vector command examples and exact supported/unsupported scope.

## Just-in-Time Preflight Required

Before source edits, the executor must re-check:

- Final CLI/Docker option names and report schema names after S1-S6.
- Actual Quizzes smoke commands, report paths, and terminal statuses from S6.
- Current live docs references from `rg "scenario-executor-poc|ScenarioExecutor POC|generated fault injection" docs/verifiers-impl mkdocs.verifier.yml -g '!archive/**'`, with optional manual inspection for intentionally historical archive mentions.
- Whether docs build script and dependencies are available locally.

## Verification

- `rg "scenario-executor-poc|ScenarioExecutor POC" docs/verifiers-impl mkdocs.verifier.yml -g '!archive/**'` — no live stale POC references for the implemented executor, except intentionally historical/archive references if left with context.
- `./scripts/verifier-docs build` — docs site builds, if local docs tooling is available.
- Manual inspection of `docs/verifiers-impl/reference/scenario-executor.md` — supported scope, commands, report status vocabulary, and future limitations are explicit.

## Evidence to Record

- files changed/renamed
- commands run and outputs
- docs build or inspection evidence
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- Do not overclaim generic execution or multi-saga support.
- Do not delete historical archive references unless they break live docs; archives can still mention the old POC as history.
- Keep docs aligned with actual final status names and CLI/Docker env names.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Renamed the live ScenarioExecutor reference page to `docs/verifiers-impl/reference/scenario-executor.md` and rewrote it around the supported single-saga fault-vector path.
- Updated live docs/navigation to stop presenting the executor as a POC and to point at the renamed reference page.
- Refreshed current-state, roadmap, advisor/thesis pages, glossary terms, evidence, and the plain pipeline explanation so they describe the implemented default-vector and explicit-vector path without claiming generic execution.
- Fixed the review finding by removing the remaining live `POC` wording from `docs/verifiers-impl/verifier-pipeline-plain-explanation.md` and updating its freshness date.
- Updated `docs/verifiers-impl/evidence.md`'s last-updated date to match the newly cited 2026-07-08 fault-vector smoke evidence.
- Kept unsupported scope explicit and added the 2026-07-08 Quizzes fault-vector smoke commands/results as durable reference evidence.
- Updated two archive meeting-note links only because the rename otherwise produced MkDocs broken-link warnings.

### Files Changed

- `docs/verifiers-impl/reference/scenario-executor.md` — renamed/replaced the live executor reference doc with supported-scope commands, report vocabulary, smoke evidence, and limitations.
- `docs/verifiers-impl/README.md` — updated the reference table link/label.
- `docs/verifiers-impl/current-state.md` — replaced POC framing with bounded supported-path wording and refreshed current evidence/next priorities.
- `docs/verifiers-impl/roadmap.md` — updated Stage 2 current status, status matrix, and near-term milestone wording.
- `docs/verifiers-impl/reading-guide.md` — updated the reference link.
- `docs/verifiers-impl/advisor-brief.md` — updated meeting-prep wording, smoke evidence, and prep links.
- `docs/verifiers-impl/thesis-claims-evidence-map.md` — updated thesis-safe claim wording and source links.
- `docs/verifiers-impl/glossary.md` — aligned ScenarioExecutor execution terminology with the supported path.
- `docs/verifiers-impl/evidence.md` — replaced the old POC smoke section with the new fault-vector smoke evidence and commands, and refreshed the page date.
- `docs/verifiers-impl/verifier-pipeline-plain-explanation.md` — updated the live pipeline explainer to remove the remaining executor POC framing and refresh its date.
- `mkdocs.verifier.yml` — updated nav label/path for the renamed reference page.
- `docs/verifiers-impl/archive/meeting-notes/2026-W26-thesis-meeting.md` — retargeted the renamed reference-doc link to avoid a docs build warning.
- `docs/verifiers-impl/archive/meeting-notes/2026-W27-thesis-meeting.md` — retargeted the renamed reference-doc links to avoid docs build warnings.
- `issues/2026-07-07-scenario-executor-fault-vectors/007-scenario-executor-docs.md` — completion evidence only.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `rg -n "scenario-executor-poc|ScenarioExecutor POC|generated fault injection" docs/verifiers-impl mkdocs.verifier.yml -g '!archive/**'` before edits | PASS | Confirmed the expected red state: live docs still contained stale POC framing and old-path references in `README.md`, `advisor-brief.md`, `current-state.md`, `roadmap.md`, `reading-guide.md`, `thesis-claims-evidence-map.md`, and `mkdocs.verifier.yml`. |
| `rg -n "scenario-executor-poc|ScenarioExecutor POC" docs/verifiers-impl mkdocs.verifier.yml -g '!archive/**'` after edits | PASS | Output only contained intentionally historical archive mentions; no live current-truth/reference/nav pages still pointed to the old POC path or label. |
| `rg -n "POC|scenario-executor-poc|ScenarioExecutor POC|generated fault injection" docs/verifiers-impl/*.md docs/verifiers-impl/reference mkdocs.verifier.yml` after review-fix | PASS | Exit code 1 / no output. The remaining live non-archive `POC` wording flagged by review in `docs/verifiers-impl/verifier-pipeline-plain-explanation.md` is gone. |
| `./scripts/verifier-docs build` | PASS | MkDocs built successfully to `target/verifier-docs-site`. Initial build warned about broken archive links caused by the rename; after retargeting those archive links, the build completed without broken-link warnings. Re-ran successfully after the review-fix edits. |
| Manual inspection of `docs/verifiers-impl/reference/scenario-executor.md` | PASS | Verified the doc explicitly states the supported single-saga saga/local scope, shows default/explicit vector CLI and Docker commands, requires explicit scenario id for explicit vectors, lists v2 status vocabulary, and keeps unsupported future work explicit. |

### Acceptance Criteria Evidence

- AC-23: Live verifier docs no longer describe the implemented executor path as a POC. The reference page is now `reference/scenario-executor.md`, MkDocs nav and reading paths point to it, current-state/roadmap/advisor/thesis wording now describes the supported single-saga default/explicit fault-vector path, and the live pipeline explainer no longer uses stale `executor POC` framing. Unsupported multi-saga, TCC, stream/gRPC/distributed parity, compensation-step faults, non-binary impairments, batch/search/scoring/prioritization, generic reset, and impact telemetry remain explicit future/out-of-scope areas.
- AC-23: The reference doc now documents CLI and Docker usage for both default-vector and explicit-vector runs, including the explicit `SCENARIO_ID`/`--scenario-id` requirement when `FAULT_VECTOR`/`--fault-vector` is provided.
- AC-23: The reference doc and glossary use the post-implementation execution terminology and list the v2 terminal-status/lifecycle/fault-slot vocabulary at a reference level.
- AC-23: `docs/verifiers-impl/evidence.md` now cites the durable 2026-07-08 Quizzes default/explicit-vector smoke commands and report paths from S6.

### Browser / Manual Evidence

- Not required.

### TDD Notes

- TDD was not practical for this docs-only slice. The closest red/green loop was search-first: confirm stale live references with `rg`, update docs, then re-run `rg` and the MkDocs build.

### Deviations From Plan

- Updated two archive meeting-note links even though archive content was otherwise out of scope, because the live docs rename produced broken-link warnings during `./scripts/verifier-docs build`.
- Also updated `docs/verifiers-impl/verifier-pipeline-plain-explanation.md` because it is a live current-path explainer that still used stale executor wording and would have contradicted the updated current-state/reference docs.
- After review, refreshed `docs/verifiers-impl/evidence.md`'s page date for consistency with the newly cited 2026-07-08 smoke evidence.

### Blockers / Follow-Ups

- None.
