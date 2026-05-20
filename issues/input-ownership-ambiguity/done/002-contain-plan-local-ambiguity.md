## Parent PRD

`issues/input-ownership-ambiguity/prd.md`

## Type

AFK

## What to build

Tighten verifier-side dynamic evidence joining so ambiguity remains local to the plans whose inputs actually participate in the ambiguous identity set. Direct runtime ids must continue to win for the matching plan, but direct evidence for another input must not promote or ambiguously enrich unrelated plans through semantic fallback.

This slice intentionally preserves genuine same-feature sibling ambiguity while preventing false ambiguity spillover.

## Acceptance criteria

- [x] A plan whose input id appears directly in runtime evidence is still enriched as `MATCHED_EXACT`.
- [x] A plan whose input id is not the direct runtime id from otherwise similar evidence is not promoted by semantic fallback because of that foreign direct id.
- [x] A plan is marked `AMBIGUOUS` only when its own input id participates in the ambiguous identity set relevant to the evidence.
- [x] A neighboring plan whose input id is not in the ambiguous identity set is not marked `AMBIGUOUS` solely because nearby inputs are ambiguous.
- [x] Genuine same-feature sibling ambiguity remains `AMBIGUOUS` when the current evidence cannot distinguish between sibling inputs that both participate.
- [x] Focused joiner tests cover direct-id wins, foreign-direct-id non-promotion, genuine sibling ambiguity, and unrelated-plan spillover containment.
- [x] No simulator runtime payload expansion is required by this slice unless a minimal implementation detail is documented.

## Validation

Implemented plan-local dynamic join containment in `DynamicEvidenceJoiner`: direct `inputVariantId` events still win exactly, foreign direct ids are excluded from semantic fallback for other plans, and fallback candidate/identity sets are scoped to the plan being enriched while preserving duplicate saga-name and same-feature sibling ambiguity.

Validated with `mvn -Dtest=DynamicEvidenceJoinerSpec,DynamicInputMapWriterSpec test` in `verifiers/`: BUILD SUCCESS, 20 tests, 0 failures. No simulator runtime event payload fields were added for this slice.

## Blocked by

None - can start immediately

## User stories addressed

- User story 12
- User story 13
- User story 14
- User story 15
- User story 20
- User story 24
