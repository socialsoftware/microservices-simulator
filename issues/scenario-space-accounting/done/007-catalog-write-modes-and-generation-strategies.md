# 007 - Catalog Write Modes And Generation Strategies

Mode: AFK
Parent PRD: ./prd.md
Blocked by: 005-type-level-coverage-and-interaction-selection.md, 006-compatible-input-tuple-counting.md
Feature Criteria Covered: AC-10, AC-12, AC-14
Verification Mode: test
Proof Required: passing tests for `BRUTE_FORCE`, `INTERACTION_PRUNED`, `WRITE_PLANS`, `COUNT_ONLY`, write caps, and segment placeholder reporting

## Slice Contract

Wire compressed accounting into catalog generation behavior through explicit generation strategies and catalog write modes.

## Acceptance Criteria

- `BRUTE_FORCE + WRITE_PLANS` on small fixtures writes every expected `ScenarioPlan`.
- `INTERACTION_PRUNED + WRITE_PLANS` on small fixtures writes only selected `ScenarioPlan`s.
- `COUNT_ONLY` writes an empty `scenario-catalog.jsonl`.
- `COUNT_ONLY` still writes `scenario-catalog-manifest.json`.
- `COUNT_ONLY` still writes `scenario-catalog-rejected-inputs.jsonl`.
- `COUNT_ONLY` still writes complete `scenario-space-accounting.json` selected counts.
- Manifest written/export counts reflect the empty catalog in count-only mode.
- Accounting `selectedByGenerator` remains complete in count-only mode.
- Global `maxCatalogScenarios` write cap affects `catalogWritten`, not `allInputBound` or `selectedByGenerator`.
- If `SEGMENT_COMPRESSED` is configured, accounting records the current effective placeholder behavior and does not claim thesis-style compression.
- Documentation describes the generation strategy and catalog write-mode config keys and behavior.

## Domain Context

- `BRUTE_FORCE` is all input-bound saga sets and schedules under the run configuration.
- `INTERACTION_PRUNED` is the thesis generator strategy and uses strict or broad interaction depending on fallback configuration.
- `COUNT_ONLY` is required for Quizzes-scale brute-force or broad selected spaces.

## Implementation Notes

- Keep selected-space counts separate from written catalog counts.
- Preserve existing scenario catalog artifact contract where possible.
- Avoid full materialization in count-only mode.

## Completion Evidence

- Implementation: `ScenarioGenerator` now honors `COUNT_ONLY` without materializing scenario plans, emits `BRUTE_FORCE` plans for all input-bound saga combinations, and keeps `INTERACTION_PRUNED` graph-selected. `ScenarioGeneratorApplication` now writes real compressed accounting via `ScenarioSpaceAccountingCalculator` and separates `catalogWritten` from selected-space counts. Documentation now describes generation strategies, write modes, max-scenario cap semantics, and count-only behavior.
- Verification: From `verifiers/`, `mvn test -Dtest=ScenarioGeneratorSpec,ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec -DfailIfNoTests=false` passed with 60 tests. From `verifiers/`, `mvn test -Dtest=ScenarioGeneratorSpec,ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec -DfailIfNoTests=false` passed with 67 tests.
- Slice compliance review: PASS (`ses_171c09856ffe5EErp6asr2vdeF`); all acceptance criteria and PRD AC-10/AC-12/AC-14 coverage mapped as covered, no required fixes.
- Code quality review: PASS (`ses_171c0981effewZ1NgMRjskNhJP`); minimality, maintainability, test adequacy, integration, count-only materialization risk, and regression risk passed, no required fixes.
- Notes: Initial focused verification found a test-fixture contradiction in the brute-force unrelated-pair case; the fixture was corrected to keep structural unrelatedness while using compatible input bindings, then focused and broader verification passed.
- AC-14 QA addendum: Final package QA found that `SEGMENT_COMPRESSED` accounting needed to mirror current `ScheduleEnumerator` behavior. Fixed `ScenarioSpaceAccountingCalculator` so `SEGMENT_COMPRESSED` counts order-preserving interleavings for tuples with `totalSteps<=12` and serial fallback for larger tuples, and updated `effectiveSegmentBehavior` to record that precise behavior without claiming thesis-style compression.
- AC-14 verification addendum: From `verifiers/`, `mvn test -Dtest=ScenarioSpaceAccountingCalculatorSpec -DfailIfNoTests=false` passed with 19 tests. From `verifiers/`, `mvn test -Dtest=ScenarioSpaceAccountingCalculatorSpec,ScenarioGeneratorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec -DfailIfNoTests=false` passed with 73 tests.
