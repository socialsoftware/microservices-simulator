## Parent PRD

`issues/fast-quizzes-enrichment/prd.md`

## Type

AFK

## What to build

Change dynamic enrichment orchestration to write one run-scoped input map, launch one serial Maven command for all selected classes, use one evidence directory with one shared evidence file, capture one Maven log, emit batch and per-class test-run artifacts, remove the old per-class loop, and preserve partial/strict behavior.

This slice turns the input-map and status-reporting pieces into the first full orchestration path, still before memory-oriented reader/joiner refactors.

## Acceptance criteria

- [x] A fake process runner observes exactly one Maven command for a selected multi-class dynamic run.
- [x] The Maven command includes all selected test classes through the chosen Surefire selector and does not launch one command per class.
- [x] The Maven command uses run-scoped dynamic-evidence properties for output directory, input-map path, test context, listener autodetection, and application name.
- [x] The Maven command does not add Surefire parallelism or external bounded-parallelism flags.
- [x] The run-scoped evidence directory contains `dynamic-input-map.json`, `maven-output.log`, `test-run.json`, and per-class `test-runs/*.json` audit sidecars.
- [x] Orchestration no longer creates one evidence directory per selected test class.
- [x] `allowPartialTestRun=true` writes enriched artifacts after a nonzero Maven exit caused by failed tests.
- [x] Strict mode writes artifacts before surfacing the dynamic test-run failure.
- [x] The persisted batch/per-class metadata includes selected classes, command arguments, phase timestamps or durations available in this slice, batch status, per-class report-derived status, static catalog path, and evidence root.
- [x] Existing orchestrator tests are updated to assert run-scoped behavior through the public dynamic-enrichment run entry point.

## Validation

- Reworked `DynamicEnrichmentOrchestrator.run(...)` to write one run-scoped `dynamic-input-map.json`, invoke one Maven command with a comma-separated Surefire `-Dtest=` selector for all selected classes, and use the evidence root directly for output and input-map properties.
- Removed the per-class orchestration loop and per-class evidence directories from the orchestrator path; per-class data is now written only as `test-runs/*.json` audit sidecars under the shared evidence root.
- Wired `SurefireTestRunReporter` into orchestration so per-class statuses come from Surefire reports while `maven-output.log`, batch `test-run.json`, command arguments, timestamps/duration, batch status, static catalog path, and evidence root are persisted before enrichment output is written.
- Preserved partial mode by continuing read/join/write after a nonzero Maven exit; preserved strict mode diagnostics by writing sidecars and join artifacts before throwing the dynamic test-run failure.
- Updated `DynamicEnrichmentOrchestratorSpec` to assert public run-entry behavior: exactly one command for multi-class runs, all classes in the Surefire selector, run-scoped dynamic-evidence properties, no parallel flags, expected run-scoped artifacts, no per-class evidence directory, partial-mode artifacts after failed reports, and strict-mode artifacts before failure.
- Ran `mvn -Dtest=DynamicEnrichmentOrchestratorSpec test` in `verifiers/`: passed, 5 tests.
- Ran `mvn -Dtest=SurefireTestRunReporterSpec,DynamicInputMapWriterSpec,DynamicEnrichmentOrchestratorSpec test` in `verifiers/`: passed, 11 tests.
- Ran `mvn -Dtest=DynamicInputMapTest test` in `simulator/`: passed, 10 tests.

## Blocked by

- Blocked by `issues/fast-quizzes-enrichment/001-run-scoped-input-map.md`
- Blocked by `issues/fast-quizzes-enrichment/002-report-derived-test-runs.md`

## User stories addressed

- User story 5
- User story 6
- User story 10
- User story 12
- User story 13
- User story 14
- User story 19
- User story 20
- User story 21
- User story 24
- User story 25
