## Parent PRD

`issues/fast-quizzes-enrichment/prd.md`

## Type

AFK

## What to build

Replace JSON-tree-retaining evidence reads with a compact verifier-side event projection and read-result metrics while preserving public read/join behavior.

This slice should reduce the reader-side memory risk without changing dynamic evidence semantics or runtime payload shape.

## Acceptance criteria

- [x] Existing reader behavior for malformed JSONL lines is preserved: malformed lines produce warnings and do not abort the full read.
- [x] Multi-file evidence reads remain deterministic by source path and line order.
- [x] The public read result reports evidence files read, dynamic events read, events missing test context, reader warnings, and evidence bytes read.
- [x] Current fixture evidence passed through the public reader and joiner path produces the same join statuses, observed summaries, and warning counts as before this slice.
- [x] A generated fixture with 20,000 evidence events reads successfully, reports `dynamicEventsRead=20000`, reports a positive evidence byte count, and completes under 10 seconds in the verifier module test environment.
- [x] Raw Jackson JSON trees are not part of the production event API consumed by the joiner.
- [x] Tests avoid asserting on private parser internals.

## Validation

- Replaced raw-tree-retaining `DynamicEvidenceEvent` with a compact verifier-side projection containing only event identity, test identity, input/functionality/step fields, compact payload map accessors, and source path/line metadata.
- Changed `DynamicEvidenceReader` to stream JSONL lines with a `BufferedReader`, parse each line into compact maps, preserve malformed-line warnings without aborting, and keep deterministic source-path ordering across files.
- Expanded `DynamicEvidenceReadResult` to report `evidenceFilesRead`, `dynamicEventsRead`, `eventsMissingTestContext`, reader warnings, and `evidenceBytesRead`.
- Added reader coverage for deterministic multi-file ordering and metrics, malformed-line warning behavior, public reader+joiner fixture behavior preserving `MATCHED_HIGH_CONFIDENCE` plus observed step/command/aggregate summaries, and a generated 20,000-event fixture with positive byte count and under-10-second assertion.
- Updated existing joiner tests to consume compact events through the public event API instead of `JsonNode` helpers.
- Verified production dynamic model code contains no `JsonNode` or `raw()` event API with `grep` over `verifiers/src/main/java/.../dynamic/model`.
- Ran `mvn -Dtest=DynamicEvidenceReaderSpec,DynamicEvidenceJoinerSpec test` in `verifiers/`: passed, 21 tests.
- Ran `mvn "-Dtest=DummyappDynamicEnrichmentIntegrationSpec#dummyapp dynamic enrichment runs through run scoped batched orchestrator path" test` in `verifiers/`: passed, 1 test.

## Blocked by

- Blocked by `issues/fast-quizzes-enrichment/004-dummyapp-batched-tracer.md`

## User stories addressed

- User story 15
- User story 18
- User story 20
- User story 23
