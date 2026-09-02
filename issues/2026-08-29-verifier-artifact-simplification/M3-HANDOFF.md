# M3 handoff — normalized dynamic evidence

State: `complete`

## Outcome

- Normalized the five supported runtime event kinds (`stepStarted`, `stepFinished`,
  `commandSent`, `aggregateAccessed`, and `invariantViolation`) into one
  `dynamic-observations.jsonl` record per runtime observation.
- Added `dynamic-attribution-links.jsonl`, grouped by test execution, Saga, and Saga
  invocation. Attribution distinguishes `exactInput`, `testAndShape`, `shapeOnly`,
  `ambiguous`, and `unmatched`; a unique runtime input id is the strongest evidence.
  Non-exact candidates must match the resolved Saga and every observed Saga-local step;
  test identity also preserves source/owner display-name parity.
- Added dynamic accounting for observation/test/attribution totals, unique input evidence,
  and all four workload participant categories.
- Extended the current manifest reader/writer with optional `dynamicObservations` and
  `dynamicAttributionLinks` roles, hashes, exact reference validation, and M0-fixture exact
  serialization coverage for every observation kind and attribution status. Absent optional
  evidence produces no placeholder role or file.
  The reader rejects linked empty roles, wrong kind-specific field combinations, and any
  observation/attribution/unique-input/participant accounting mismatch.
- Kept run/test/normalization diagnostics under the external dynamic-evidence directory.
  Raw event streams are deleted only after package data, accounting, manifest, and current
  reader validation succeed; failure retains the raw stream. Publication snapshots and
  restores all four mutable artifacts if staging, promotion, or final validation fails.
- Removed the workload dynamic sidecar, sidecar manifest, join report, old compatibility
  projection models, and the residual legacy scenario catalog writer. Scenario execution
  output protection now relies on current manifest-linked package inputs rather than old
  v3 dynamic artifact recognition.
- Preserved the known runtime input-map mismatch without changing simulator behavior:
  verifier input-map entries still expose `workloadPlanIds`, while the simulator expects
  `scenarioPlanIds`; normalization emits a diagnostic when this prevents exact evidence.

## Main implementation files

- `dynamic/DynamicEvidenceReader.java`, `DynamicEvidenceJoiner.java`,
  `DynamicEnrichmentOrchestrator.java`, `DynamicEnrichmentConfig.java`
- `dynamic/model/DynamicEvidenceEvent.java`, `DynamicEvidenceReadResult.java`,
  `DynamicEvidenceJoinResult.java`, `DynamicObservation.java`,
  `DynamicAttributionLink.java`
- `dynamic/export/DynamicArtifactWriter.java`
- `scenario/export/ScenarioCatalogPackageReader.java`
- `ScenarioGeneratorApplication.java` and `application.yaml`
- `executor/ScenarioExecutor.java` (removal of legacy dynamic-artifact compatibility path)

Deleted legacy production surfaces include `EnrichedScenarioCatalogWriter`,
`ScenarioCatalogJsonlWriter`, `UnmatchedReasonClassifier`, and the old workload-sidecar
summary/status/observation model records.

## Tests and proof

- Added `CurrentDynamicNormalizationSpec`: all five kinds, exact/test-and-shape/shape-only/
  ambiguous/unmatched attribution, exact-evidence precedence, repeated grouped observations,
  one input observed under all three successful statuses but counted once at its strongest
  status, display-name owner parity, positive/negative step-aware shapes, unique-input
  accounting, input-map mismatch diagnostic, and all four participant categories.
- Added `DynamicArtifactWriterSpec`: production writer + current reader/manifest/hash path,
  exact serialized equality to all five M0 observation records and all five M0 attribution
  records, accounting, omission of absent optional files, and rollback at all six injected
  staging/promotion/final-validation boundaries.
- Added `DynamicCurrentReaderCorruptionSpec`: 20 current-reader corruption cases covering
  empty linked roles, required and incompatible fields for every kind, optional error detail,
  and exact reconciliation of totals, kinds, context, statuses, strongest input, and all
  participant categories.
- Added `DynamicRawEvidenceLifecycleSpec`: successful finalize deletes raw evidence; failed
  injected manifest-promotion finalization retains it, preserves the original manifest and a
  reader-valid package, and keeps diagnostics outside the package. The parameterized writer
  test is the byte-for-byte proof for every manifest-linked package artifact.
- Migrated reader/discovery and dummyapp accounting/current-package tests; removed historical
  sidecar/joiner/writer assertions rather than weakening them.
- Focused review suite: **39 tests, 0 failures, 0 errors, 0 skipped**.
- `cd verifiers && mvn clean test`: **654 tests, 0 failures, 0 errors, 0 skipped** across
  42 suites.
- `cd verifiers && mvn -DskipTests compile test-compile`: success.
- `git diff --check`: success.

## Discovery and boundaries

- Autonomous implementation detail: the current reader needed dynamic cross-reference and
  accounting validation so writer finalization could be the raw-stream deletion boundary.
- Autonomous cleanup: two M2 tests still used the deleted legacy catalog writer for fixture
  publication/hash helpers; they now use `ExecutableArtifactWriter` and a local SHA-256 helper.
- No material scope delta. Application behavior, prerequisite/materializability algorithms,
  and the known simulator input-map mismatch were not changed.
- Existing unrelated/shared changes in `docs/verifiers-impl/current-state.md`, `lib/`, and
  `tmp/` were preserved.

## What to try

Generate a current executable package with dynamic enrichment enabled. Confirm the manifest
contains the two optional dynamic roles only when evidence exists, diagnostics remain under
the run's dynamic-evidence directory, and the raw `dynamic-evidence.jsonl` is absent only
after the package can be read successfully through the current reader.
