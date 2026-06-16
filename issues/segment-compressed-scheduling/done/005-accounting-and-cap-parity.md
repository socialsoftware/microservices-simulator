# 005 - Accounting And Cap Parity

Mode: AFK
Parent PRD: ./prd.md
Blocked by: 004-deterministic-segment-compressed-identities.md
Feature Criteria Covered: AC-8, AC-9, AC-10, AC-11, AC-12, AC-14, AC-15
Verification Mode: test
Proof Required: passing accounting/generator parity tests for shared compressed count semantics, grouped rows, caps, zero caps, deterministic accounting ordering, and updated effective behavior text

## Slice Contract

Make scenario-space accounting consume the real segment-compressed schedule-count semantics and prove it agrees with materialized generation for small fully written cases.

This slice also proves schedule caps and zero caps behave consistently for `SEGMENT_COMPRESSED`, grouped rows report reduced schedule counts where appropriate, and the accounting run config no longer describes segment compression as placeholder behavior or serial fallback.

## Acceptance Criteria

- Accounting `scheduleCountPerTuple` for `SEGMENT_COMPRESSED` uses the same conflict-anchor multinomial semantics as materialized schedule generation.
- Accounting treats zero conflict anchors as one canonical schedule.
- Accounting applies `maxSchedulesPerInputTuple` to segment-compressed counts consistently with materialized generation.
- `maxSchedulesPerInputTuple=0` disables schedule emission/counting for `SEGMENT_COMPRESSED` consistently.
- A small uncapped fixture proves materialized `ScenarioPlan` count equals accounting `selectedByGenerator.total` when no global `maxCatalogScenarios` cap truncates output.
- A capped fixture proves materialized generation emits only capped schedules and accounting grouped-row counts use the capped value.
- Existing generation warnings/counters report schedule caps deterministically; no new accounting warning field is required unless implementation deliberately changes the artifact shape.
- Grouped saga-set rows report reduced `scheduleCountPerTuple` for segment-compressed examples with internal/non-anchor steps.
- Repeated accounting over the same fixture produces stable grouped-row ordering and totals.
- The accounting run config or effective behavior text describes conflict-anchor segment compression and no longer says `SEGMENT_COMPRESSED` is not thesis-style compression or falls back to serial for large tuples.

## Domain Context

- Scenario-space accounting is the count-only source of truth for large applications such as Quizzes.
- The PRD requires materialized schedules and accounting counts to share schedule semantics so written catalogs and count-only runs describe the same scenario space.
- Counts should continue to use arbitrary precision and serialize as decimal strings under existing accounting conventions.

## Implementation Notes

- Prefer a shared schedule/count component or equivalent shared model so accounting and materialization do not duplicate formulas that can drift.
- Keep the accounting schema stable unless a deliberate schema change is required.
- Do not materialize large scenario spaces for accounting tests; use small exact fixtures for parity and grouped-row assertions.

## Completion Evidence

- Implementation: Added accounting/materialized generator parity coverage for small full-write `SEGMENT_COMPRESSED` fixtures, capped schedule counts, zero schedule caps, reduced grouped-row schedule counts, deterministic accounting row/totals ordering, and updated non-placeholder effective behavior text. Existing production accounting already uses conflict-anchor multinomial counts, row-local configured conflict candidates, cap application, and zero-anchor canonical count semantics.
- Verification: PASS - `mvn -Dtest=ScenarioGeneratorSpec,ScenarioSpaceAccountingCalculatorSpec test` from `verifiers/` (55 tests, 0 failures, 0 errors).
- Slice compliance review: PASS - read-only spec compliance review task `ses_131911ff7ffeaOtz7VEoATka7d`.
- Code quality review: PASS - read-only code quality review task `ses_131911fc5ffea1itCO9fHt6xzS`.
- Notes: Accounting schema remained stable. Review noted a non-blocking maintainability risk that accounting and materialized enumeration still have separate count/enumeration logic, mitigated by parity tests in this slice.
