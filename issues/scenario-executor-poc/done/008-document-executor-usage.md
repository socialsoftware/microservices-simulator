## Parent PRD

`issues/scenario-executor-poc/prd.md`

## Type

AFK

## What to build

Document the first scenario executor POC for verifier maintainers. The documentation should explain how to run the executor, how enriched-default/static-fallback catalog loading works, what `--scenario-id` means, which runtime-owned argument overrides are allowed, how to read the execution report and console trace, what skip reasons mean, and which limitations remain out of scope.

## Acceptance criteria

- [x] Documentation includes a runnable command shape for the verifier-owned CLI.
- [x] Documentation explains that `--scenario-id` means scenario plan id.
- [x] Documentation explains enriched catalog defaulting and static catalog fallback.
- [x] Documentation lists the allowed runtime-owned overrides: `SagaUnitOfWorkService`, `CommandGateway`, and `SagaUnitOfWork`.
- [x] Documentation explains report fields, terminal statuses, console trace output, and skipped-candidate reason counts.
- [x] Documentation states that executor output is not dynamic-enrichment evidence and is not joined back into enriched catalogs in this POC.
- [x] Documentation states that Quizzes source/test changes, fixture generation, multi-saga execution, fault injection, behavior CSV, impact scoring, GA search, and bandit prioritization are out of scope.
- [x] Documentation references the Quizzes smoke evidence from `007-quizzes-first-runnable-smoke.md` or the resulting implementation log/report.

## Feature criteria covered

- AC-019
- AC-020
- AC-022
- AC-025

## Domain context

Use the terms from `docs/verifiers-impl/glossary.md` and `docs/verifiers-impl/structured-input-recipes.md`: scenario catalog, enriched catalog, `ScenarioPlan`, input variant, input recipe, executor readiness, blocker, sidecar artifact, and scenario execution.

## Verification plan

- Inspect the documentation for the required command, catalog-loading, report, override, skip-reason, and out-of-scope content.
- If docs include example commands, ensure they match the implemented CLI/config names.

## Completion evidence

- Added `docs/verifiers-impl/scenario-executor-poc.md` documenting the verifier-owned CLI command shape, `--scenario-id` scenario plan id semantics, enriched-default/static-fallback loading, runtime-owned overrides, execution report fields, terminal statuses, console trace, skipped-candidate reason counts, executor-output boundaries, and out-of-scope limitations.
- Documentation records the successful Quizzes smoke evidence from issue 007, including scenario plan id, saga FQN, catalog artifact path, step trace, terminal status, and report path.

## Verification

- Verified by inspecting `docs/verifiers-impl/scenario-executor-poc.md` after implementation and confirming it contains every requested section and matches implemented CLI/config names.

## Blocked by

- Blocked by `issues/scenario-executor-poc/007-quizzes-first-runnable-smoke.md`

## User stories addressed

- User story 40
- User story 41
- User story 42
- User story 43
- User story 44
- User story 50
- User story 51
- User story 52
- User story 53
- User story 54
- User story 55
