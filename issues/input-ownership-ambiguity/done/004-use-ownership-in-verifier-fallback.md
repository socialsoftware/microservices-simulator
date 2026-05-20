## Parent PRD

`issues/input-ownership-ambiguity/prd.md`

## Type

AFK

## What to build

Apply the same ownership semantics used by simulator runtime attribution to verifier-side fallback matching. When runtime evidence lacks a direct `inputVariantId`, verifier fallback should use ownership-aware eligibility before assigning existing high-confidence or partial match statuses.

This slice keeps the status model unchanged. It only improves when existing statuses are selected.

## Acceptance criteria

- [x] Verifier fallback matching treats an input as eligible when the active test identity matches any declared owner and workflow/step evidence also matches.
- [x] Verifier fallback matching excludes inputs whose owner sets do not include the active test identity before ambiguity classification.
- [x] A unique ownership-aware fallback match with complete test identity produces the existing `MATCHED_HIGH_CONFIDENCE` status.
- [x] A unique ownership-aware fallback match without complete test identity does not exceed the existing `MATCHED_PARTIAL` behavior.
- [x] Ownership-aware fallback does not override or weaken `MATCHED_EXACT` when direct runtime ids are present for the plan.
- [x] Focused joiner tests prove that runtime attribution and verifier fallback use consistent ownership semantics for owned and non-owned inputs.
- [x] The slice does not add new join statuses.

## Validation

Implemented ownership-aware fallback in `DynamicEvidenceJoiner.inputIdentityMatches(...)`, using declared owners when present and retaining the legacy provenance source method fallback for inputs without owners. Existing join statuses were unchanged.

Validated with:

- `mvn -Dtest=DynamicEvidenceJoinerSpec,DynamicInputMapWriterSpec test` in `verifiers/`: BUILD SUCCESS, 20 tests, 0 failures.
- `mvn -Dtest=DynamicInputMapTest test` in `simulator/`: BUILD SUCCESS, 7 tests, 0 failures.

## Blocked by

- Blocked by `issues/input-ownership-ambiguity/001-resolve-owned-helper-inputs.md`

## User stories addressed

- User story 7
- User story 11
- User story 12
- User story 18
- User story 20
