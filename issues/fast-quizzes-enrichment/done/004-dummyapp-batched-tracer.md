## Parent PRD

`issues/fast-quizzes-enrichment/prd.md`

## Type

AFK

## What to build

Add or update dummyapp-first public verifier-path coverage proving that the run-scoped batched dynamic pipeline works end to end before memory refactors. This tracer should use the existing reader/joiner behavior and focus on proving the new orchestration path.

The process runner may be fake where needed to keep the test deterministic, but the slice must exercise the public dynamic-enrichment orchestration path rather than a hand-built artifact fixture that bypasses the new pipeline.

## Acceptance criteria

- [x] Dummyapp dynamic enrichment runs through the public verifier dynamic-enrichment path using the run-scoped orchestration introduced by `003-single-maven-orchestration.md`.
- [x] The dummyapp tracer writes one run-scoped dynamic input map.
- [x] The dummyapp tracer uses one Maven command through the process-runner boundary for the selected test classes.
- [x] Shared dynamic evidence and report-derived per-class statuses are consumed by the verifier path.
- [x] Enriched sidecars are written and expected dummyapp join statuses remain stable for the covered fixture scenario.
- [x] Required run-scoped sidecars and report fields are present for the dummyapp run.
- [x] No per-class evidence directories are required for the dummyapp tracer.

## Validation

- Added a dummyapp-first tracer in `DummyappDynamicEnrichmentIntegrationSpec` that builds the dummyapp scenario plans, calls the public `DynamicEnrichmentOrchestrator.run(...)` entry point, and uses a fake `ProcessRunner` only at the Maven boundary.
- The fake runner writes one shared `dynamic-evidence.jsonl` under the run-scoped evidence root and representative Surefire reports under `target/surefire-reports`, so the verifier consumes shared evidence and report-derived per-class statuses through the normal reader/joiner/writer path.
- Asserted one Maven command for the selected dummyapp classes, one run-scoped `dynamic-input-map.json`, run-scoped output/input-map properties, `maven-output.log`, batch `test-run.json`, per-class `test-runs/*.json`, and no per-class evidence directory.
- Asserted batch/per-class report fields including selected classes, command arguments, `PASSED` per-class statuses, status counts, static catalog path, and evidence root.
- Asserted enriched output is written and the covered dummyapp fixture scenario remains `MATCHED_HIGH_CONFIDENCE` with the expected observed event sequence, `dynamicEventsRead=5`, and `eventsMissingTestContext=1`.
- Ran `mvn "-Dtest=DummyappDynamicEnrichmentIntegrationSpec#dummyapp dynamic enrichment runs through run scoped batched orchestrator path" test` in `verifiers/`: passed, 1 test.
- Also ran the full `DummyappDynamicEnrichmentIntegrationSpec`; the new tracer passed, while the pre-existing manual exact-evidence branch failed on an older hard-coded count assertion unrelated to the run-scoped orchestration tracer.

## Blocked by

- Blocked by `issues/fast-quizzes-enrichment/001-run-scoped-input-map.md`
- Blocked by `issues/fast-quizzes-enrichment/002-report-derived-test-runs.md`
- Blocked by `issues/fast-quizzes-enrichment/003-single-maven-orchestration.md`

## User stories addressed

- User story 17
- User story 22
- User story 23
