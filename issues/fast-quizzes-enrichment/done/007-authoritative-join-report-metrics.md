## Parent PRD

`issues/fast-quizzes-enrichment/prd.md`

## Type

AFK

## What to build

Make `dynamic-evidence-join-report.json` the authoritative operational summary for dynamic-enrichment run status, per-class status counts, evidence counts and bytes, and simple phase durations. Keep the enriched manifest comparable for enriched-catalog status counts.

This slice can be implemented after run-scoped orchestration without waiting for the indexed join refactor. Later reader/joiner work should feed the same report fields.

## Acceptance criteria

- [x] `dynamic-evidence-join-report.json` includes `dynamicRunStartedAt` and `dynamicRunFinishedAt`.
- [x] `dynamic-evidence-join-report.json` includes `mavenDurationMillis` and `readJoinWriteDurationMillis`.
- [x] `dynamic-evidence-join-report.json` includes `evidenceBytesRead`, `evidenceFilesRead`, `dynamicEventsRead`, and `eventsMissingTestContext`.
- [x] `dynamic-evidence-join-report.json` includes selected, passed, failed, timedOut, skipped, and noReport class counts.
- [x] `dynamic-evidence-join-report.json` includes run-scoped static catalog path, dynamic evidence root, enriched catalog path, batch status, selected classes, command arguments, and per-class report-derived statuses.
- [x] The enriched manifest still reports comparable record, join-status, warning, and test-run status counts.
- [x] Report paths point at run-scoped artifacts rather than per-class evidence directories.
- [x] No profiler, JMX, or heap metric integration is required.

## Validation

- Extended `DynamicEvidenceJoinResult` and the joiner to carry `evidenceBytesRead` from the compact reader into report writing.
- Extended `EnrichedScenarioCatalogWriter` with optional report metadata while preserving the existing writer API; the join report now includes authoritative operational fields and the counts include `evidenceBytesRead` alongside files/events/missing-context counts.
- Updated `DynamicEnrichmentOrchestrator` to pass run-scoped report metadata: `dynamicRunStartedAt`, `dynamicRunFinishedAt`, `mavenDurationMillis`, `readJoinWriteDurationMillis`, `batchStatus`, selected classes, command arguments, static catalog path, dynamic evidence root, enriched catalog path, and Maven output log path.
- Verified report class counts include selected, passed, failed, timed out, skipped, and no-report counts; per-class report-derived statuses remain in `testRuns`.
- Verified report paths point at the run-scoped evidence root and enriched catalog path rather than per-class evidence directories.
- The implementation intentionally adds no profiler, JMX, or heap metrics.
- Ran `mvn -Dtest=DynamicEnrichmentOrchestratorSpec,EnrichedScenarioCatalogWriterSpec test` in `verifiers/`: passed, 7 tests.

## Blocked by

- Blocked by `issues/fast-quizzes-enrichment/002-report-derived-test-runs.md`
- Blocked by `issues/fast-quizzes-enrichment/003-single-maven-orchestration.md`

## User stories addressed

- User story 12
- User story 17
- User story 18
- User story 19
- User story 20
- User story 24
- User story 26
