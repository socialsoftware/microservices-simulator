## Parent PRD

`issues/input-ownership-ambiguity/prd.md`

## Type

AFK

## What to build

Build the first ownership tracer bullet for helper-created inputs. A helper-created saga input should carry explicit owner metadata separate from provenance, export that ownership through the dynamic input map, and resolve at runtime when the active feature is one of the declared owners. The same path must stay conservative for undeclared features.

This slice establishes the ownership model and the runtime matching contract for one narrow but complete behavior before expanding to fixture contexts.

## Acceptance criteria

- [x] A helper-created input exposes first-class ownership metadata through the public analyzed-input/scenario model surface, while preserving separate provenance fields such as source class, source method, and source binding.
- [x] Ownership metadata can list multiple owning feature methods for one input.
- [x] A dynamic input map exported for a selected test class includes helper-input ownership metadata in the runtime-consumable contract.
- [x] Runtime input-map resolution matches an owned helper-created input when the active test identity is one of the declared owners and the workflow/step identity also matches.
- [x] Runtime input-map resolution does not match that helper-created input when the active test identity is not among the declared owners.
- [x] A multi-owner helper input resolves for either declared owner and remains unmatched for an undeclared feature.
- [x] Ownership metadata does not participate in deterministic input id or dedup behavior in this slice unless an implementation constraint is explicitly documented.
- [x] Tests prove the behavior through public surfaces such as adapted input records, exported input maps, and runtime input-map resolution rather than private traversal internals.

## Validation

Implemented `InputOwner`/`owners` on `InputVariant`, exported owners from `DynamicInputMapWriter`, loaded and resolved owners in simulator `DynamicInputMap`, and made verifier fallback use the same owner semantics. Deterministic input id generation still calls `ScenarioIdGenerator.inputVariantId(...)` with the same pre-existing fields and does not include owners.

Validated with:

- `mvn -Dtest=DynamicInputMapTest test` in `simulator/`: BUILD SUCCESS, 7 tests, 0 failures.
- `mvn -Dtest=DynamicEvidenceJoinerSpec,DynamicInputMapWriterSpec test` in `verifiers/`: BUILD SUCCESS, 20 tests, 0 failures.

## Blocked by

None - can start immediately

## User stories addressed

- User story 1
- User story 5
- User story 6
- User story 7
- User story 8
- User story 9
- User story 10
- User story 20
- User story 21
