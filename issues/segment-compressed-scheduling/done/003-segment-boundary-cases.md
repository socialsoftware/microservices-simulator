# 003 - Segment Boundary Cases

Mode: AFK
Parent PRD: ./prd.md
Blocked by: 002-configured-conflict-lens-and-selection-isolation.md
Feature Criteria Covered: AC-4, AC-5, AC-7, AC-13, AC-16
Verification Mode: test
Proof Required: passing focused tests for no-anchor canonical schedules, all-anchor OPI parity, mixed anchor/internal segments, and mixed anchored/no-anchor canonical tails

## Slice Contract

Complete the observable segment-boundary semantics after the tracer bullet and conflict-lens slice.

This slice proves canonical no-anchor behavior, all-anchor parity with `ORDER_PRESERVING_INTERLEAVING`, non-anchor-before-anchor segment expansion, canonical tail placement, and zero-anchor saga behavior in mixed anchored/no-anchor saga sets.

## Acceptance Criteria

- A no-anchor multi-saga fixture emits/counts exactly one canonical `SEGMENT_COMPRESSED` schedule per compatible input tuple.
- An all-anchor fixture emits/counts the same number of schedules as `ORDER_PRESERVING_INTERLEAVING`, subject to the same schedule cap.
- A mixed anchor/internal fixture emits non-anchor steps before a conflict anchor with that following anchor segment.
- Non-anchor tail steps after a saga's final anchor are emitted once in canonical deterministic order after all anchor segments.
- In a mixed anchored/no-anchor saga set, a zero-anchor saga contributes its full in-saga step list to the canonical tail.
- All emitted schedules preserve intra-saga order.
- Existing `SERIAL` and `ORDER_PRESERVING_INTERLEAVING` behavior remains unchanged.

## Domain Context

- An anchor segment contains the deterministic in-saga run of non-anchor steps since the previous anchor plus the anchor step itself.
- Non-anchor tails are not independently interleaved because they do not change conflict-anchor ordering cases.
- If every step is conflict-relevant, segment compression should naturally converge to order-preserving interleaving.

## Implementation Notes

- Keep tests small and exact; synthetic saga definitions are appropriate for boundary behavior.
- Assert full expanded schedule step order, not only schedule counts, for at least the mixed anchor/internal and mixed anchored/no-anchor cases.
- Do not add accounting integration in this slice except where necessary to support public scheduling behavior; accounting parity is handled by a later slice.

## Completion Evidence

- Implementation: Added focused `ScheduleEnumerator` coverage for no-anchor canonical schedules, all-anchor parity with `ORDER_PRESERVING_INTERLEAVING`, mixed internal-before-anchor segment expansion, deterministic post-anchor tail placement, and zero-anchor saga tail contribution in mixed saga sets. Existing scheduler implementation already satisfied these semantics after the issue 001 tail handling fix.
- Verification: PASS - `mvn -Dtest=ScenarioGeneratorSpec,ScenarioSpaceAccountingCalculatorSpec test` from `verifiers/` (50 tests, 0 failures, 0 errors).
- Slice compliance review: PASS - read-only spec compliance review task `ses_131997453ffe2F1IFVZCj78TrV`.
- Code quality review: PASS - read-only code quality review task `ses_13199739dffeJMio7029i1nJi1`.
- Notes: Kept this slice to exact scheduler boundary behavior; broader accounting/materialization parity remains deferred to the later accounting slice as requested.
