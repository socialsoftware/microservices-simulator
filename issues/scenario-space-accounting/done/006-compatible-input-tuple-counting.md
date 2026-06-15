# 006 - Compatible Input Tuple Counting

Mode: AFK
Parent PRD: ./prd.md
Blocked by: 004-compressed-brute-force-counting.md
Feature Criteria Covered: AC-13
Verification Mode: test
Proof Required: passing tests for compatibility-aware grouped input tuple counts

## Slice Contract

Make compressed input tuple counts respect the existing conservative input compatibility semantics without requiring full tuple materialization for large spaces.

## Acceptance Criteria

- Exact logical-key contradictions are rejected from compatible input tuple counts.
- Unknown or missing logical-key evidence is allowed rather than guessed.
- Full multi-key binding signatures are preserved so correlated keys within one input variant are not overcounted.
- If no exact logical-key bindings are present, simple multiplication is used and is exact under current compatibility rules.
- Grouped counting results match explicit tuple enumeration for small fixtures.
- The compatible input-tuple count feeds grouped saga-set row counts and `allInputBound` totals.

## Domain Context

- `InputVariant.logicalKeyBindings` support compatibility filtering, but executor materialization uses `inputRecipe` and is a separate concern.
- Current compatibility semantics reject only known exact contradictions.
- Dynamic evidence may improve key bindings later, but this slice uses current static input data.

## Implementation Notes

- Prefer a count-first API that can be tested independently.
- It is acceptable to enumerate small fixture tuples in tests to prove grouped counting correctness.
- Avoid approximate counts in v1.

## Completion Evidence

- Implementation: Exposed package-level `ScenarioSpaceAccountingCalculator.countCompatibleInputTuples(...)` and wired grouped rows through that count-first API. The compressed binding-group counter rejects exact logical-key contradictions, allows missing/unknown evidence, preserves full multi-key binding signatures, and uses grouped multiplication without materializing large tuple spaces.
- Verification: From `verifiers/`, `mvn test -Dtest=ScenarioSpaceAccountingCalculatorSpec -DfailIfNoTests=false` passed with 14 tests. From `verifiers/`, `mvn test -Dtest=ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,ScenarioGeneratorSpec,DummyappAccountingFixtureFoundationSpec -DfailIfNoTests=false` passed with 61 tests.
- Slice compliance review: PASS (`ses_171c9e10dffeQDGPxd8iUcOc2X`); all acceptance criteria mapped as covered, no required fixes.
- Code quality review: PASS (`ses_171c9e0e3ffedPwQvDsAPkl6YL`); minimality, maintainability, test adequacy, integration, and regression risk passed, no required fixes.
- Notes: Added regression coverage comparing compressed grouped counts to explicit `InputTupleJoiner.join(...)` enumeration for small multi-key/missing-evidence fixtures, plus no-binding multiplication and row/totals feed tests.
