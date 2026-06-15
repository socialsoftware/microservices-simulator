## Parent PRD

`issues/scenario-executor-poc/prd.md`

## Type

AFK

## What to build

Add the first dry-run executor path for catalog ingestion and deterministic candidate inspection. The behavior should load an enriched catalog by default when present, fall back to the static catalog when needed, unwrap enriched records to their embedded `ScenarioPlan`, validate explicit `--scenario-id` as a scenario plan id, classify single-saga eligibility, derive runtime step names, and emit deterministic selection/skip diagnostics without starting the target application or executing any saga.

## Acceptance criteria

- [x] Given a run directory with `scenario-catalog-enriched.jsonl`, the dry-run reader loads enriched records first and exposes their embedded `ScenarioPlan` values.
- [x] Given no enriched catalog but a `scenario-catalog.jsonl`, the dry-run reader loads static scenario plans.
- [x] Given `--scenario-id` for an existing scenario plan id, the dry run reports that scenario as the selected plan and does not select a different plan.
- [x] Given `--scenario-id` for a missing scenario plan id, the dry run reports a scenario-selection failure with the missing id.
- [x] Given mixed single-saga and multi-saga records, the dry run marks only exactly-one-instance single-saga plans as structurally eligible for the POC and records skip reasons for unsupported shapes.
- [x] Given scheduled step ids such as `com.example.Saga::stepName#0`, the dry run derives `stepName` by final-`::` extraction plus one trailing `#<digits>` removal.
- [x] Given invalid step ids that do not yield a nonblank runtime step name, the dry run records a structured unsupported-step-id reason.
- [x] Given the same catalog and options twice, dry-run selected ids, eligible candidate order, derived step names, and skip-reason counts are identical.
- [x] The dry-run report does not instantiate target classes, start Spring, or call `executeUntilStep`.

## Feature criteria covered

- AC-001
- AC-002
- AC-003
- AC-004
- AC-007
- AC-008
- AC-016
- AC-023

## Domain context

Use the glossary terms `ScenarioPlan`, scenario plan id, scenario catalog, enriched catalog sidecar, and join status from `docs/verifiers-impl/glossary.md`. Treat enriched records as wrappers; the embedded static `ScenarioPlan` remains the executable contract.

## Verification plan

- Run focused verifier tests for catalog loading, enriched unwrap, static fallback, explicit scenario plan id selection, unsupported-shape diagnostics, step-name derivation, and deterministic dry-run output.
- Suggested command: `cd verifiers && mvn test -Dtest='*ScenarioExecutor*Spec,*Catalog*Executor*Spec' -DfailIfNoTests=false`

## Completion evidence

- Implemented `ScenarioCatalogReader`, `ScenarioExecutorOptions`, `ScenarioExecutionReport`, and dry-run support in `ScenarioExecutor`.
- Added enriched-default/static-fallback JSONL loading, enriched-record unwrapping to embedded `ScenarioPlan`, explicit scenario plan id selection, missing-id failure, single-saga structural validation, deterministic join-status/line-order auto-selection, runtime step-name derivation, and skip-reason counts.
- Verified by `cd verifiers && mvn test -Dtest='*ScenarioExecutor*Spec' -DfailIfNoTests=false` on 2026-05-26: 10 tests run, 0 failures, 0 errors.

## Verification

- Verified enriched catalog preference, embedded `ScenarioPlan` exposure, explicit id success/failure, static fallback, unsupported shape diagnostics, invalid step-id diagnostics, deterministic repeated dry-run output, and dry-run non-execution through `ScenarioExecutorSpec`.
- Verified focused command: `cd verifiers && mvn test -Dtest='*ScenarioExecutor*Spec' -DfailIfNoTests=false`.

## Blocked by

None - can start immediately

## User stories addressed

- User story 2
- User story 3
- User story 4
- User story 5
- User story 6
- User story 7
- User story 8
- User story 9
- User story 10
- User story 11
- User story 12
- User story 13
- User story 14
- User story 15
- User story 35
- User story 47
- User story 48
