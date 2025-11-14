# DTO Generation Refactor Plan

## Goals
- Remove reliance on manually maintained DTO definitions inside `SharedDtos` blocks.
- Automatically derive DTOs from root entity definitions while preserving customisation where needed.
- Provide an explicit, ergonomic opt-out mechanism for root properties that should not surface in the generated DTO.

## Proposed Direction
1. **Grammar Enhancements**
   - Introduce a DTO opt-out modifier for entity properties, e.g. `String internalNote dto-exclude;`.
   - Consider dedicated metadata block or annotation syntax if modifiers become noisy.
   - Extend `Entity` grammar to allow root-level DTO configuration (e.g. default DTO name override, include aggregate metadata toggle).
2. **AST & Validator Updates**
   - Regenerate AST types after grammar change; ensure new modifier is represented on `Property`.
   - Update validation logic to:
     - Warn/error if a DTO opt-out is applied to required infrastructure fields (e.g. `id` if required).
     - For non-root entities using `uses dto SomeDto ... mapping`:
       - Verify that `SomeDto` corresponds to an existing root entity (i.e., `SomeDto` = `Some` + `Dto` where `Some` is a root entity)
       - Verify that all DTO fields referenced in the `mapping` block exist in that root entity's DTO (derived from the root entity's properties, excluding `dto-exclude` fields)
       - Verify that all entity fields referenced in the `mapping` block exist in the non-root entity
3. **DTO Generation Pipeline**
   - Replace lookups into `SharedDtos` with DTO definitions derived by traversing root entities.
   - Respect opt-out flags, and propagate mapped names from optional `dtoMapping`.
   - Auto-generate DTO constructors/builders directly from the root entity using consistent getter usage.
   - Provide hooks for future custom fields (e.g. computed projections) if needed.
4. **SharedDtos Removal**
   - Completely remove support for `SharedDtos` blocks from the grammar and all generators.
   - All DTOs will be derived from root entities:
     - Root entity DTOs: Auto-generated from root entity definition
     - Non-root entity DTO references: Must point to root entity DTOs (validated)
   - No backward compatibility - this is a breaking change.
5. **CLI & Generator Refactor**
   - Simplify `SharedDtoGenerator` responsibilities:
     - Accept derived DTO schema (from root entities) instead of raw `SharedDtos` definitions.
     - Remove brittle heuristics (`isRootEntityField`, etc.).
   - Update entity orchestrators to consume the central DTO schema service.
6. **Migration Strategy**
   - Phase 1: Introduce grammar changes and regenerate AST.
   - Phase 2: Replace DTO derivation pipeline to consume root entities directly.
   - Phase 3: Remove legacy `SharedDtos` handling and clean up generators.
   - Phase 4: Provide CLI utilities for automatic removal of obsolete DTO definitions in repositories.

## Outstanding Questions - Answered

1. **Field-level override (e.g. rename/type cast) beyond opt-out?**
   - **Answer:** Not needed for now. Keep it simple with just the `dto-exclude` modifier.

2. **Should aggregate metadata fields (`aggregateId`, `version`, `state`) remain implicit or become configurable?**
   - **Answer:** Remain implicit. These are always included in root entity DTOs automatically.

3. **How to handle DTOs shared across aggregates (e.g. legacy shared types) once auto-generation is the default?**
   - **Answer:** Non-root entities can only reference DTOs of root entities from other aggregates. The `SharedDtos` file will be completely removed. This means:
     - Root entities: DTOs auto-generated from entity definition (with `dto-exclude` opt-out)
     - Non-root entities: Use `uses dto ... mapping` to reference DTOs of other root entities (e.g., `QuizExecution` references `ExecutionDto` which is auto-generated from the `Execution` root entity)
     - **Validation requirement:** When a non-root entity uses `uses dto SomeDto`, verify that:
       - `SomeDto` corresponds to a root entity (i.e., `SomeDto` = `Some` + `Dto` where `Some` is a root entity that exists)
       - All fields referenced in the `mapping` block exist in that root entity's DTO (derived from the root entity's properties)

## Next Steps
1. Finalise syntax choice for opt-out modifier.
2. Prototype grammar change + AST regeneration.
3. Update validation tests to cover opt-out behaviour.
4. Refactor DTO generation pipeline around derived schema.

## Task Breakdown

1. Grammar & AST
   - [x] Update grammar to support `dto-exclude` post-fix modifier.
   - [x] Regenerate parser/AST artifacts.
   - [ ] Adjust scope provider and services if new tokens require updates (not required so far).

2. Validation Enhancements
   - [x] Implement opt-out validation (prevent excluding required fields).
   - [x] Add cross-entity DTO reference validation for non-root mappings.

3. DTO Derivation Service
   - [x] Build in-memory DTO schema builder that walks root entities.
   - [x] Integrate opt-out handling and mapping-driven renames.
   - [x] Expose DTO schema to generators via shared service.

4. Generator Refactor
   - [x] Replace `SharedDtos` inputs with DTO schema service in shared DTO generator.
   - [x] Simplify entity constructors/builders to rely on schema (remove heuristics).
   - [x] Update other generators (factories, controllers, etc.) to consume schema.

5. SharedDtos Decommission
   - [x] Remove grammar production and regenerate artifacts.
   - [x] Delete related CLI features/templates and update codegen pipeline.
   - [ ] Surface a clearer CLI error message if legacy `SharedDtos` blocks are encountered (grammar already errors by default).


