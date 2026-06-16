# 002 - Configured Conflict Lens And Selection Isolation

Mode: AFK
Parent PRD: ./prd.md
Blocked by: 001-segment-compressed-scheduler-tracer-bullet.md
Feature Criteria Covered: AC-2, AC-3, AC-13
Verification Mode: test
Proof Required: passing tests proving strict vs broad/type-only anchor behavior and unchanged `INTERACTION_PRUNED` / `BRUTE_FORCE` saga-set selection

## Slice Contract

Prove that `SEGMENT_COMPRESSED` uses the run's configured static conflict lens to identify conflict anchors without taking over interaction pruning.

Strict runs must use strict conflict evidence for anchors. Broad runs must include broad/type-only fallback candidates only when `allowTypeOnlyFallback=true`. Changing the schedule strategy must not change which saga sets are selected by `INTERACTION_PRUNED` or `BRUTE_FORCE`.

## Acceptance Criteria

- A strict fixture proves anchors are derived from strict conflict evidence when `allowTypeOnlyFallback=false`.
- A broad/type-only fixture proves additional conflict anchors can be used only when `allowTypeOnlyFallback=true`.
- Read/read footprint pairs remain non-conflicts and do not create conflict anchors.
- `INTERACTION_PRUNED` selects and prunes the same saga sets when switching between `ORDER_PRESERVING_INTERLEAVING` and `SEGMENT_COMPRESSED`; only schedule enumeration/counts may change.
- `BRUTE_FORCE` still selects all input-bound saga sets under existing brute-force rules; segment compression only changes schedule enumeration/counts within those sets.
- Existing `SERIAL` and `ORDER_PRESERVING_INTERLEAVING` behavior remains unchanged.

## Domain Context

- The PRD explicitly keeps interaction pruning separate from segment compression.
- Strict interaction evidence excludes type-only fallback; broad interaction evidence includes configured type-only/unknown fallback behavior.
- `BRUTE_FORCE` is an input-bound denominator strategy and must not become interaction-pruned because `SEGMENT_COMPRESSED` consumes conflict evidence.

## Implementation Notes

- Reuse the same strict/broad conflict semantics already used by `ConflictGraphBuilder` and scenario-space accounting.
- Tests should compare observable selected saga sets and schedule counts rather than private graph internals unless no public assertion is practical.
- Avoid adding new generation strategies or public configuration fields unless required by the existing code shape.

## Completion Evidence

- Implementation: Added focused public accounting coverage proving `SEGMENT_COMPRESSED` anchor counts use the configured strict/broad conflict lens, read/read pairs do not create anchors, `INTERACTION_PRUNED` selected saga sets are unchanged when switching from `ORDER_PRESERVING_INTERLEAVING` to `SEGMENT_COMPRESSED`, and `BRUTE_FORCE` remains input-bound rather than becoming interaction-pruned. The implementation from issue 001 already routes materialized scheduling through selected configured conflict candidates and accounting through strict/broad `ConflictGraphBuilder` candidates.
- Verification: PASS - `mvn -Dtest=ScenarioGeneratorSpec,ScenarioSpaceAccountingCalculatorSpec test` from `verifiers/` (46 tests, 0 failures, 0 errors).
- Slice compliance review: PASS - read-only spec compliance review task `ses_1319d2fd7ffeCn9X4JErWvzCJh`.
- Code quality review: PASS - read-only code quality review task `ses_1319d2fadfferdiAt64gbeqZeW`.
- Notes: No production changes were needed for this slice beyond the issue 001 implementation; this slice locked in the configured conflict-lens and selection-isolation behavior with regression tests.
