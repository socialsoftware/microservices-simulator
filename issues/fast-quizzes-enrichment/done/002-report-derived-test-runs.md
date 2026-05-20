## Parent PRD

`issues/fast-quizzes-enrichment/prd.md`

## Type

AFK

## What to build

Add a Surefire report status extraction path that turns a selected test-class list plus Maven report output into per-class test-run audit sidecars and aggregate class status counts.

This slice provides the per-class observability needed once there is only one Maven exit code for the whole dynamic run.

## Acceptance criteria

- [x] A report with zero failures and zero errors maps to `PASSED`.
- [x] A report with failures or errors maps to `FAILED`.
- [x] A report with zero executed tests and zero failures/errors maps to `PASSED`.
- [x] A report where all tests are skipped and the report exposes that state distinctly maps to `SKIPPED`.
- [x] When Maven times out, selected classes without completed reports map to `TIMED_OUT`.
- [x] When Maven does not time out, a selected class without a report maps to `NO_REPORT` and produces a warning/count.
- [x] Per-class audit sidecars are written under `test-runs/<safe-test-class-fqn>.json` or the equivalent approved run-scoped sidecar path.
- [x] Aggregate class status counts are deterministic and include passed, failed, timedOut, skipped, and noReport counts.
- [x] Tests use representative Surefire report fixtures and selected-class lists rather than relying on a real Maven run.

## Validation

- Added `SurefireTestRunReporter`, which parses deterministic `TEST-*.xml` Surefire reports for selected test classes and writes a run-scoped `test-run.json` plus per-class sidecars under `test-runs/<safe-test-class-fqn>.json`.
- Implemented explicit status mapping: zero failures/errors -> `PASSED`, failures/errors -> `FAILED`, zero tests with zero failures/errors -> `PASSED`, all skipped with test count exposed -> `SKIPPED`, missing reports after timeout -> `TIMED_OUT`, and missing reports without timeout -> `NO_REPORT` with a warning.
- Added deterministic aggregate counts with `passed`, `failed`, `timedOut`, `skipped`, and `noReport` keys.
- Added `SurefireTestRunReporterSpec` using representative XML fixtures and selected-class lists, with no real Maven execution dependency.
- Ran `mvn -Dtest=SurefireTestRunReporterSpec test` in `verifiers/`: passed, 3 tests.

## Blocked by

None - can start immediately

## User stories addressed

- User story 12
- User story 18
- User story 23
- User story 24
- User story 26
