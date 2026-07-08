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
