# 004 - Deterministic Segment-Compressed Identities

Mode: AFK
Parent PRD: ./prd.md
Blocked by: 003-segment-boundary-cases.md
Feature Criteria Covered: AC-12, AC-13
Verification Mode: test
Proof Required: passing repeatability tests showing stable schedule order, scheduled-step IDs, scenario IDs, and unchanged non-segment strategies across repeated runs

## Slice Contract

Prove `SEGMENT_COMPRESSED` preserves deterministic schedule ordering and deterministic IDs across repeated runs.

The same saga definitions, input variants, and configuration must produce the same expanded schedule order, scheduled-step IDs, scenario IDs, and result ordering. Existing non-segment strategies must remain stable and unchanged.

## Acceptance Criteria

- Repeated `SEGMENT_COMPRESSED` generation over the same fixture produces identical schedule ordering.
- Repeated `SEGMENT_COMPRESSED` generation over the same fixture produces identical scheduled-step IDs.
- Repeated `SEGMENT_COMPRESSED` generation over the same fixture produces identical scenario IDs.
- Permuting input saga definition order and input variant order does not destabilize deterministic IDs beyond existing normalized ordering semantics.
- Existing `SERIAL` and `ORDER_PRESERVING_INTERLEAVING` determinism tests still pass.
- No hidden randomness or deterministic-seed-dependent shuffling is introduced for segment compression.

## Domain Context

- Stable IDs are part of the scenario catalog contract and are required for reproducible accounting, catalog output, and future dynamic/execution joins.
- `ScenarioPlan` identity includes expanded schedule and conflict evidence, so schedule-order determinism directly affects scenario IDs.

## Implementation Notes

- Prefer public `ScenarioGenerator.generate(...)` repeatability tests over assertions on private sorting helpers.
- Keep deterministic ordering aligned with existing saga FQN, saga instance ID, step order index, deterministic ID, step key, and name tie-breakers.
- Do not change `ScenarioIdGenerator` unless the current ID contract cannot represent the new schedules correctly.

## Completion Evidence

- Implementation: Added public `ScenarioGenerator.generate(...)` repeatability coverage for `SEGMENT_COMPRESSED`, asserting stable expanded schedule ordering, scheduled-step IDs, scenario IDs, and result ordering across repeated runs, permuted saga/input order, and different deterministic seeds. No `ScenarioIdGenerator` changes were needed.
- Verification: PASS - `mvn -Dtest=ScenarioGeneratorSpec,ScenarioSpaceAccountingCalculatorSpec test` from `verifiers/` (51 tests, 0 failures, 0 errors).
- Slice compliance review: PASS - read-only spec compliance review task `ses_13195cc8affeN748mEPMgpXWsD`.
- Code quality review: PASS - read-only code quality review task `ses_13195cc55ffeDGktsENJA2qXJ0`.
- Notes: Existing serial and order-preserving interleaving determinism coverage continued to pass unchanged; segment compression remains deterministic without seed-dependent shuffling.
