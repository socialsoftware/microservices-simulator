# 002 - Accounting Artifact Contract

Mode: AFK
Parent PRD: ./prd.md
Blocked by: None
Feature Criteria Covered: AC-1, AC-2, AC-3, AC-6
Verification Mode: test
Proof Required: passing focused tests for schema, manifest integration, config fields, and decimal-string counts

## Slice Contract

Introduce the machine-readable scenario-space accounting artifact contract and run-level metadata wiring without implementing the full counting algorithms yet.

## Acceptance Criteria

- Scenario catalog export writes a `scenario-space-accounting.json` artifact in the same run directory as the catalog artifacts.
- The accounting artifact schema is exactly `microservices-simulator.scenario-space-accounting.v1`.
- The scenario catalog manifest records `catalogWriteMode`.
- The scenario catalog manifest records a run-relative or otherwise resolvable reference to `scenario-space-accounting.json`.
- The accounting config serializes target application, generation strategy, catalog write mode, include-singles setting, max saga set size, max input variants per saga, max schedules per input tuple, max scenarios write cap, schedule strategy, type-only fallback, input policy, source-mode handling, and effective segment behavior when applicable.
- Large combinatoric counts serialize as decimal strings, not JSON numbers.
- The artifact contract and config/property knobs are documented for future Docker/manual runs.

## Domain Context

- The run directory is the artifact package; accounting should not duplicate artifact paths or timestamps already owned by the manifest.
- Accounting is static v1 scenario-space accounting, not executor execution evidence.
- `SEGMENT_COMPRESSED` is placeholder behavior and must not be claimed as thesis-style compression.

## Implementation Notes

- Add or extend model/writer code with an extensible shape that later slices can populate.
- Prefer a deep module for accounting artifact serialization so counting logic can be tested independently from application startup.
- Keep initial counts minimal placeholders if necessary; later slices fill in real count sections.
- Documentation can be in verifier implementation docs or the PRD package, as long as config keys are exact.

## Completion Evidence

- Implementation: Added v1 accounting report/writer (`ScenarioSpaceAccountingReport`, `ScenarioSpaceAccountingWriter`), explicit `generationStrategy` and `catalogWriteMode` config, manifest fields `catalogWriteMode` and `scenarioSpaceAccountingPath`, application wiring for `scenario-space-accounting.json`, YAML defaults, and current-state documentation for exact config keys. Counts are placeholder contract fields for later slices and serialize as decimal strings.
- Verification: PASS, `mvn test -Dtest=ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec -DfailIfNoTests=false` from `verifiers/` (`23` tests, `0` failures/errors).
- Slice compliance review: PASS, task `ses_172243f45ffeWrlTA6gE9c8b3D`.
- Code quality review: PASS, task `ses_172243ea5ffeS7eSR3MKkI33RU`.
- Notes: Full compressed counting, count-only behavior, and generator selection are intentionally left to later dependent slices.
