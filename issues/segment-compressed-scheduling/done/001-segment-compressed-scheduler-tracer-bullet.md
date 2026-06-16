# 001 - Segment-Compressed Scheduler Tracer Bullet

Mode: AFK
Parent PRD: ./prd.md
Blocked by: None
Feature Criteria Covered: AC-1, AC-2, AC-6, AC-8, AC-13, AC-16
Verification Mode: test
Proof Required: passing focused Spock tests for one-anchor-per-saga compressed schedules, counts, and a larger-than-old-cutoff case proving no serial fallback

## Slice Contract

Replace the placeholder `SEGMENT_COMPRESSED` path with the first real conflict-anchor segment-compressed behavior through public verifier scheduling/generation behavior.

For two sagas where each saga has one internal step followed by one conflict-anchor step, `SEGMENT_COMPRESSED` must emit/count the two conflict-anchor ordering cases instead of all six step-level order-preserving interleavings. This slice also proves the former total-step cutoff/serial fallback is no longer the effective behavior.

## Acceptance Criteria

- A focused test defines two sagas with steps `aInternal, aConflict` and `bInternal, bConflict`, where only `aConflict` and `bConflict` participate in a write/read or write/write cross-saga conflict.
- `SEGMENT_COMPRESSED` emits/counts exactly two schedules for that fixture, preserving only the two conflict-anchor orders.
- The emitted schedules expand full anchor segments, so representative schedules include `aInternal, aConflict, bInternal, bConflict` and `bInternal, bConflict, aInternal, aConflict` or the deterministic equivalent under existing ordering rules.
- The emitted schedules preserve intra-saga step order.
- The natural segment-compressed count for this case is computed as the multinomial over per-saga conflict-anchor counts.
- A larger-than-former-cutoff fixture proves `SEGMENT_COMPRESSED` does not fall back to serial solely because total step count exceeds the old placeholder threshold.
- Existing `SERIAL` and `ORDER_PRESERVING_INTERLEAVING` behavior remains unchanged.
- Tests that asserted the old small-tuple placeholder behavior are removed or updated to assert real segment compression.

## Domain Context

- `SEGMENT_COMPRESSED` is currently documented as a placeholder in `docs/verifiers-impl/current-state.md`.
- A conflict anchor is a step participating in at least one configured cross-saga conflict candidate within the selected saga set.
- Segment compression reduces schedules inside selected saga sets; it does not select saga sets.
- This is the tracer bullet for the full PRD and should not implement Quizzes smoke, documentation updates, executor behavior, fault injection, GA, bandit, or impact scoring.

## Implementation Notes

- Prefer tests through `ScheduleEnumerator` and/or `ScenarioGenerator` public behavior rather than private helper internals.
- Keep the first implementation minimal but avoid encoding the old total-step cutoff.
- It is acceptable to introduce a small deep scheduling/count model if needed, but do not overbuild beyond this tracer behavior.
- Preserve existing deterministic ordering based on saga FQNs, saga instance IDs, step order indexes, and existing tie-breakers.

## Completion Evidence

- Implementation: Replaced the `SEGMENT_COMPRESSED` placeholder path in `ScheduleEnumerator` with conflict-anchor segment enumeration, using selected `ConflictGraphBuilder.ConflictCandidate` rows as anchors and appending deterministic tail steps. Updated `ScenarioGenerator` to pass saga-set-selected candidates into scheduling. Updated accounting to count order-preserving interleavings over per-saga anchor counts and to filter candidates to the grouped saga set. Updated the accounting run description away from the old cutoff/serial fallback warning.
- Verification: PASS - `mvn -Dtest=ScenarioGeneratorSpec,ScenarioSpaceAccountingCalculatorSpec test` from `verifiers/` (42 tests, 0 failures, 0 errors).
- Slice compliance review: PASS - read-only spec compliance review task `ses_131a3951effefeEbtSyOdqp1vQ`.
- Code quality review: PASS - read-only code quality review task `ses_131a093f6ffeVaorXTrRrB1CTN` after fixing row-local accounting candidate filtering.
- Notes: Added focused scheduler/accounting tests for one-anchor-per-saga compression, larger-than-old-cutoff behavior, and external conflict anchors not affecting unrelated grouped rows. Initial reviewer findings were fixed before completion.
