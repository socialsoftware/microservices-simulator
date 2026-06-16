# 006 - Dummyapp Segment-Compressed Integration

Mode: AFK
Parent PRD: ./prd.md
Blocked by: 005-accounting-and-cap-parity.md
Feature Criteria Covered: AC-9, AC-16, AC-17
Verification Mode: test
Proof Required: passing dummyapp integration tests proving real verifier-pipeline compressed schedules and accounting/materialized count agreement on bounded full-write configurations

## Slice Contract

Validate real verifier-pipeline behavior on dummyapp before Quizzes.

Use bounded dummyapp configurations that are small enough to materialize full catalogs. The slice should prove the adapter-generated saga/input model can flow through `SEGMENT_COMPRESSED` generation and scenario-space accounting with matching materialized and selected counts.

## Acceptance Criteria

- Dummyapp analysis uses the real verifier pipeline through parsed dummyapp source/tests and the scenario model adapter.
- At least one real dummyapp multi-step/interacting fixture is generated with `scheduleStrategy=SEGMENT_COMPRESSED`.
- A bounded full-write dummyapp configuration produces materialized `ScenarioPlan` counts matching accounting `selectedByGenerator.total` when no global `maxScenarios` cap truncates output.
- Dummyapp assertions prove expanded schedules preserve intra-saga order and include full segment-expanded steps.
- Dummyapp coverage exercises at least one real compressed case where segment compression reduces schedule count versus step-level order-preserving interleaving, if such a fixture is available in current dummyapp.
- Exact edge semantics remain primarily covered by focused tests from earlier slices; this slice focuses on real adapter/pipeline integration.
- No Quizzes source or test files are modified.

## Domain Context

- `applications/dummyapp/` is the canonical verifier fixture corpus for parser, scenario, accounting, and enrichment edge cases.
- Dummyapp integration should prove the feature on real extracted verifier data before thesis-scale Quizzes accounting.
- Quizzes remains the smoke/evaluation target, not the first place to debug exact schedule semantics.

## Implementation Notes

- Build on existing `DummyappAccountingFixtureFoundationSpec` or nearby dummyapp scenario-generation coverage if that keeps the slice minimal.
- Keep config bounds high enough to avoid accidental `maxScenarios` truncation but small enough for fast tests.
- Avoid duplicating every synthetic edge test from earlier slices in dummyapp; assert the end-to-end pipeline behavior that synthetic tests cannot prove.

## Completion Evidence

- Implementation: Added a real dummyapp parser/adapter integration test in `DummyappAccountingFixtureFoundationSpec` using parsed dummyapp source/tests, `ApplicationAnalysisScenarioModelAdapter`, interaction-pruned `SEGMENT_COMPRESSED` generation, and scenario-space accounting. The test targets the real `CreateItemFunctionalitySagas` / `CancelOrderFromItemFunctionalitySagas` interaction, asserts non-empty conflict evidence, full expanded schedules with per-saga order preservation, accounting/materialized selected-count parity, and reduced schedule count versus order-preserving interleaving for the same row.
- Verification: PASS - `mvn -Dtest=DummyappAccountingFixtureFoundationSpec,ScenarioGeneratorSpec,ScenarioSpaceAccountingCalculatorSpec test` from `verifiers/` (66 tests, 0 failures, 0 errors).
- Slice compliance review: PASS - read-only spec compliance review task `ses_131888daaffeHBTqa4wAQ9rRIK` after tightening the test to prove a generated interacting compressed plan.
- Code quality review: PASS - read-only code quality review task `ses_131888d7effeGMp3caG3ihg4wN` after tightening the test.
- Notes: Initial reviews failed because the first dummyapp test could pass on brute-force non-interacting plans; the final version uses interaction-pruned generation and asserts the concrete interacting dummyapp pair and reduced accounting row.
