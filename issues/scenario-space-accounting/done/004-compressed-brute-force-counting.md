# 004 - Compressed Brute Force Counting

Mode: AFK
Parent PRD: ./prd.md
Blocked by: 002-accounting-artifact-contract.md, 003-dummyapp-accounting-fixture-foundation.md
Feature Criteria Covered: AC-5, AC-7, AC-8, AC-9, AC-10, AC-14
Verification Mode: test
Proof Required: passing unit tests for compressed all-input-bound counts, grouped rows, top contributors, bounds, and schedule formulas

## Slice Contract

Implement exact compressed counting for the input-bound brute-force denominator. This slice should count scenario shapes by saga set without materializing every `ScenarioPlan`.

## Acceptance Criteria

- Accounting computes `inputBoundScenarioSpace.allInputBound` totals by saga-set size and total count.
- Accounting computes `selectedByGenerator` for `BRUTE_FORCE` as equal to `allInputBound` under the same run configuration.
- Accounting carries `catalogWritten` placeholders or actual zeroes as appropriate before write-mode integration is completed.
- Accounting emits grouped rows for all input-bound saga sets up to configured `maxSagaSetSize`.
- The default grouped-row safety threshold is at least `100000`.
- A configured lower grouped-row threshold fails fast rather than silently omitting grouped rows.
- Each grouped row includes deterministic `sagaSetKey`, full saga FQNs, input counts by saga, step counts by saga, compatible input-tuple count, schedule count per tuple, scenario shape count, strict/broad interaction summary placeholders, and `selectedByConfiguredGenerator`.
- `topContributors` ranks by brute-force scenario shape count and includes rank, saga-set key, and represented scenario-shape count.
- Large counts use arbitrary precision and serialize as decimal strings.
- Grouped-row ordering is stable and deterministic.
- Top-contributor tie-breaking is stable and deterministic.
- `maxInputVariantsPerSaga` shapes `allInputBound` and `selectedByGenerator`.
- `maxSchedulesPerInputTuple` shapes `allInputBound` and `selectedByGenerator`.
- `includeSingles=true` includes set-size-1 rows and counts; `includeSingles=false` excludes them consistently.
- `SERIAL` schedule counts one schedule per input tuple.
- `ORDER_PRESERVING_INTERLEAVING` schedule counts match materialized small schedule enumerations.
- Same-saga multi-instance combinations are excluded.
- Fault-vector multiplication is excluded.

## Domain Context

- `allInputBound` is the brute-force denominator for accepted input-bearing sagas under one run configuration.
- Compressed accounting groups by saga set and computes counts mathematically.
- This slice does not implement interaction-pruned selection beyond `BRUTE_FORCE` count equality.

## Implementation Notes

- Use `BigInteger` or equivalent for counts.
- Use existing schedule semantics rather than inventing new schedule behavior.
- Avoid building `ScenarioPlan` records for every represented scenario shape.
- The grouped row interaction summary may be structurally present with placeholder values until the interaction slice fills it.

## Completion Evidence

- Implementation: added `ScenarioSpaceAccountingCalculator`, grouped saga-set rows, top contributors, `BigInteger` compressed brute-force counting, deterministic row/top ordering, grouped-row threshold config, and focused accounting tests.
- Verification: PASS, `mvn test -Dtest=ScenarioSpaceAccountingCalculatorSpec -DfailIfNoTests=false` from `verifiers/` passed with 8 tests; PASS, `mvn test -Dtest=ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,ScenarioGeneratorSpec,DummyappAccountingFixtureFoundationSpec -DfailIfNoTests=false` from `verifiers/` passed with 55 tests.
- Slice compliance review: PASS, task `ses_171d9fa11ffexNeR5ZhAGtdbKF`; reviewer confirmed AC-5, AC-7, AC-8, AC-9, AC-10, AC-14 and issue-specific criteria are covered.
- Code quality review: PASS, task `ses_171d9f9bbffe9VGA1dHOa0bo36`; reviewer confirmed minimality, maintainability, test adequacy, compressed integration without `ScenarioPlan` materialization, and low regression risk.
- Notes: reviewer-identified gaps were fixed by enforcing `maxGroupedSagaSetRows` during combination generation and by replacing integer joiner metrics with compressed `BigInteger` logical-key binding group counting; regression coverage includes a `2500000000` tuple-count case.
