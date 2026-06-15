# 009 - Dummyapp Accounting Integration

Mode: AFK
Parent PRD: ./prd.md
Blocked by: 007-catalog-write-modes-and-generation-strategies.md, 008-static-executor-readiness-summary.md
Feature Criteria Covered: AC-16
Verification Mode: test
Proof Required: passing dummyapp integration tests for full-write and count-only accounting behavior

## Slice Contract

Validate the full scenario-space accounting feature on dummyapp fixtures with exact expectations and small enough spaces to materialize catalogs.

## Acceptance Criteria

- Dummyapp integration tests cover unrelated sagas with inputs.
- Dummyapp integration tests cover strict interaction.
- Dummyapp integration tests cover broad/type-only interaction.
- Dummyapp integration tests cover an interacting missing-input case through `typeLevelCoverage`.
- Dummyapp integration tests cover a three-saga connected chain.
- Dummyapp integration tests cover multi-step order-preserving schedule counts.
- Dummyapp integration tests cover compatible and incompatible input tuples.
- `BRUTE_FORCE + WRITE_PLANS` writes the expected full catalog and accounting counts.
- `INTERACTION_PRUNED + WRITE_PLANS` writes the expected selected catalog and accounting counts.
- `COUNT_ONLY` writes an empty catalog and complete accounting counts.

## Domain Context

- Dummyapp is the exact fixture target for verifier assumptions and edge cases.
- This slice proves the feature end-to-end before Quizzes Docker smoke.
- Quizzes source/tests remain unchanged.

## Implementation Notes

- Use deterministic fixture names and expected counts.
- Keep integration tests focused and bounded.
- Prefer assertions on JSON paths for accounting artifact contents.

## Completion Evidence

- Implementation: Added dummyapp accounting integration assertions in `DummyappAccountingFixtureFoundationSpec` for unrelated sagas with inputs, strict symbolic interaction, broad/type-only interaction, missing-input/type-level coverage, a three-saga chain, order-preserving schedule counts, compatible/incompatible input tuples, `BRUTE_FORCE + WRITE_PLANS`, `INTERACTION_PRUNED + WRITE_PLANS`, and `COUNT_ONLY`.
- Verification: `mvn test -Dtest=DummyappAccountingFixtureFoundationSpec -DfailIfNoTests=false` passed with 10 tests from `verifiers/`.
- Verification: `mvn test -Dtest=DummyappAccountingFixtureFoundationSpec,ScenarioGeneratorSpec,ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec -DfailIfNoTests=false` passed with 72 tests from `verifiers/`.
- Slice compliance review: PASS (`ses_171ab05e7ffeaZQw0Zdb0fQ3Fg`).
- Code quality review: PASS (`ses_171ab05b5ffeh4gzKH3Xgu7WLR`).
- Notes: The strict `INTERACTION_PRUNED + WRITE_PLANS` fixture deterministically selects the strict row shape but emits zero input-bound plans because capped accepted input tuples are incompatible; row-level assertions still prove selection/pruning, schedule count, and tuple compatibility diagnostics.
