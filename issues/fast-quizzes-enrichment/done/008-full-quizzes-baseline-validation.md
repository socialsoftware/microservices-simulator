## Parent PRD

`issues/fast-quizzes-enrichment/prd.md`

## Type

AFK

## What to build

Run and document the full Quizzes dynamic-enrichment smoke under default Docker compose memory settings, comparing timing, memory viability, and enriched output against the approved baseline from `verifiers/target/quizzes-20260519-100927-216/`.

This is the final validation slice for the PRD. It should not change scenario semantics, Quizzes tests, matching rules, or Docker memory defaults to make the numbers pass.

## Acceptance criteria

- [x] The full 42-class Quizzes dynamic-enrichment run completes under 15 minutes under default compose memory settings.
- [x] The full run does not require `JAVA_TOOL_OPTIONS=-Xmx3g` or raised `MEDIUM_MEM_LIMIT`/`MEDIUM_MEM_RESERVATION` overrides.
- [x] The enriched catalog has 66 records.
- [x] The join counts are exactly `MATCHED_EXACT=52`, `MATCHED_HIGH_CONFIDENCE=1`, `MATCHED_PARTIAL=0`, `AMBIGUOUS=0`, `UNMATCHED=13`, and `NOT_COVERED=0`.
- [x] `warningCount=0`.
- [x] `eventsMissingTestContext=0`.
- [x] `testClassesSelected=42`.
- [x] If the Quizzes tests are otherwise unchanged, class status counts are `testClassesPassed=40`, `testClassesFailed=2`, `testClassesTimedOut=0`, `testClassesSkipped=0`, and `testClassesNoReport=0`.
- [x] `dynamicEventsRead` is reported. The target is exactly 18,868 events; 17,925-19,812 is acceptable only when exact join counts, warning count, test-context count, and observed-summary semantics remain stable.
- [x] The join report includes the operational metrics required by `007-authoritative-join-report-metrics.md`.
- [x] Validation notes record the command/environment used, run directory, duration, memory settings, status counts, event counts, and any accepted event-count delta explanation.

## Validation Notes

- Final command: `env -u JAVA_TOOL_OPTIONS -u MEDIUM_MEM_LIMIT -u MEDIUM_MEM_RESERVATION docker compose run --rm -e VERIFIERS_DYNAMIC_ENRICHMENT_PER_TEST_TIMEOUT_SECONDS=900 fault-analysis-scenario-gen` after `docker compose build --no-cache fault-analysis-scenario-gen`.
- Final run directory: `verifiers/target/quizzes-20260520-012233-812`.
- Memory settings: default compose settings only, `mem_limit=${MEDIUM_MEM_LIMIT:-768m}`, `mem_reservation=${MEDIUM_MEM_RESERVATION:-512m}`, and service default `JAVA_TOOL_OPTIONS=${JAVA_TOOL_OPTIONS:--XX:InitialRAMPercentage=50.0 -XX:MaxRAMPercentage=75.0}`. No `JAVA_TOOL_OPTIONS=-Xmx3g` and no raised memory environment overrides were used.
- Duration: `mavenDurationMillis=246831` and `readJoinWriteDurationMillis=26331`, total dynamic run window from `2026-05-20T01:22:37.074079717Z` to `2026-05-20T01:27:10.610584846Z`, comfortably under 15 minutes.
- Run status: `batchStatus=FAILED` and `runStatus=PARTIAL`, matching the known two Quizzes application test failures while preserving artifacts.
- Enriched catalog/manifest: `recordCount=66`, `warningCount=0`.
- Join counts: `MATCHED_EXACT=52`, `MATCHED_HIGH_CONFIDENCE=1`, `MATCHED_PARTIAL=0`, `AMBIGUOUS=0`, `UNMATCHED=13`, `NOT_COVERED=0`.
- Test status counts: `testClassesSelected=42`, `testClassesPassed=40`, `testClassesFailed=2`, `testClassesTimedOut=0`, `testClassesSkipped=0`, `testClassesNoReport=0`.
- Evidence counts: `evidenceFilesRead=1`, `evidenceBytesRead=24134611`, `dynamicEventsRead=18868`, `eventsMissingTestContext=0`.
- Event-count delta explanation: no delta accepted; final `dynamicEventsRead` exactly matches the 18,868-event target.
- Operational metrics present in `dynamic-evidence-join-report.json`: `dynamicRunStartedAt`, `dynamicRunFinishedAt`, `mavenDurationMillis`, `readJoinWriteDurationMillis`, `batchStatus`, `selectedTestClassFqns`, `commandArguments`, `staticCatalogPath`, `dynamicEvidenceRoot`, `enrichedCatalogPath`, and `mavenOutputLogPath`.
- Supporting targeted validations run during this slice: `mvn -Dtest=DefaultProcessRunnerSpec test`, `mvn -Dtest=DynamicEvidenceRecorderTest test`, `mvn -Dtest=DynamicEnrichmentOrchestratorSpec test`, and `mvn -Dtest=SurefireTestRunReporterSpec test`.

## Blocked by

- Blocked by `issues/fast-quizzes-enrichment/006-indexed-join-equivalence.md`
- Blocked by `issues/fast-quizzes-enrichment/007-authoritative-join-report-metrics.md`

## User stories addressed

- User story 1
- User story 2
- User story 17
- User story 18
- User story 22
- User story 26
