## Problem Statement

The verifier can currently find useful saga inputs, classify them as `RESOLVED`, `REPLAYABLE`, `PARTIAL`, or `UNRESOLVED`, and export provenance-rich `InputVariant` records. Those records preserve human-readable constructor argument summaries, provenance text, source-mode evidence, ownership metadata, warnings, and stable scenario IDs.

That is enough for reporting and dynamic-enrichment attribution, but it is not enough for a future scenario executor. An executor should not have to reverse-engineer constructor values, setup calls, DTO creation, runtime-produced values, or property reads from prose strings. The verifier already has structured Groovy trace recipes internally, but the scenario catalog export still collapses the important value structure into text.

This gap blocks the next executor-oriented stage. A future executor must be able to instantiate input values, instantiate the saga, and run saga steps from machine-readable catalog records. The current catalog describes that work to a human, but not precisely enough to be a runnable contract.

## Solution

Add an embedded, schema-versioned `inputRecipe` payload to verifier input exports. The recipe will be an executor-facing DTO mapped from the verifier trace structures, not a direct serialization of internal Groovy classes.

Accepted scenario catalog inputs will carry `inputRecipe` directly inside each `InputVariant`. Rejected input records will switch to a wrapper shape containing the full input object plus rejection metadata, so accepted and rejected inputs expose the same input fields, including owners and recipes. Enriched catalog records will preserve recipes because they preserve the scenario plan; dynamic joiner semantics do not change.

The recipe schema will use action-oriented concepts that describe what a future executor can do:

- typed literal values;
- constructor or DTO creation;
- named-argument, setter, and simple property assignments;
- list, set, and map construction;
- local transforms and coercions such as `toSet()` and `as Set`;
- helper-return results reduced to nested result recipes;
- method-call results that may need replay at execution time;
- injectable placeholders;
- property access on another recipe result;
- explicit unresolved nodes and blockers.

The recipe will expose readiness separately from catalog acceptance. `RESOLVED` and `REPLAYABLE` recipes can be executor-ready when all required structure is present. `PARTIAL` and `UNRESOLVED` inputs will still use the same recipe shape, but will mark the input, affected arguments, and affected nodes as not executor-ready with deterministic blocking reasons.

Human-readable summaries and provenance text remain part of the catalog. Structured recipes add a machine-readable contract; they do not replace diagnostics.

## User Stories

1. As a future scenario-executor author, I want each exported input variant to include structured constructor argument recipes, so that I can instantiate saga inputs without parsing summary text.
2. As a future scenario-executor author, I want recipes embedded next to the input variant that uses them, so that a scenario line is directly consumable without joining to a separate sidecar file.
3. As a verifier author, I want textual summaries and provenance to remain available, so that existing analysis and debugging workflows stay useful.
4. As a verifier author, I want accepted and rejected inputs to expose the same input shape, so that rejection metadata does not hide recipe or ownership information.
5. As a verifier author, I want rejected inputs to keep explicit rejection reasons, so that catalog policy decisions remain distinguishable from recipe materializability.
6. As a future scenario-executor author, I want `executorReady` to be explicit, so that I do not accidentally try to run partial or unresolved inputs.
7. As a future scenario-executor author, I want blocking reasons to be attached to the input, constructor argument, and node that caused them, so that executor planning can report precise gaps.
8. As a verifier author, I want `RESOLVED`, `REPLAYABLE`, `PARTIAL`, and `UNRESOLVED` inputs to share one recipe schema, so that downstream tools do not need separate parsers for incomplete cases.
9. As a future scenario-executor author, I want literals to include typed values, so that `17`, `'17'`, `true`, `null`, and decimal values are not ambiguous.
10. As a future scenario-executor author, I want constructor recipes to include target type FQNs when executor-ready, so that DTO construction does not require resolving short names from source text later.
11. As a verifier author, I want missing constructor target types to become explicit blockers, so that the export does not pretend a DTO is mechanically instantiable when its type is unknown.
12. As a future scenario-executor author, I want named constructor arguments to be represented structurally, so that Groovy-style DTO creation can be materialized later.
13. As a future scenario-executor author, I want setter calls to be represented structurally, so that DTOs mutated after construction can still become runnable recipes.
14. As a future scenario-executor author, I want simple property assignments to be represented structurally, so that direct field/property DTO setup is not lost in prose.
15. As a verifier author, I want assignment capture limited to deterministic same-scope construction and mutation before use, so that v1 remains sound instead of guessing through arbitrary mutation flows.
16. As a future scenario-executor author, I want list, set, and map construction represented explicitly, so that collection-valued constructor arguments can be materialized mechanically.
17. As a future scenario-executor author, I want map entries to preserve key and value recipes, so that named and map-like data do not collapse into unstructured text.
18. As a future scenario-executor author, I want local transforms and coercions represented explicitly, so that `toSet()`, `as Set`, and similar source shapes are not lost.
19. As a verifier author, I want unresolved transform receivers to become blockers, so that runtime-dependent transforms are not treated as statically materializable.
20. As a future scenario-executor author, I want helper-return values represented as nested recipes, so that I can use the reduced value recipe without replaying Spock helper methods.
21. As a future scenario-executor author, I want opaque or runtime-producing method calls represented as call-result recipes, so that setup calls can be replayed when needed.
22. As a verifier author, I want no public facade-specific execution kind, so that the schema does not encode Quizzes-specific `Functionalities` naming as a required application architecture.
23. As a future scenario-executor author, I want method-call recipes to include receiver structure, method name, ordered argument recipes, and return type when known, so that invocation does not require source-text parsing.
24. As a future scenario-executor author, I want property access represented as a node over another recipe result, so that values such as runtime-created DTO aggregate IDs can be replayed then read.
25. As a future scenario-executor author, I want injectable placeholders represented explicitly, so that infrastructure dependencies can be supplied by the executor environment.
26. As a verifier author, I want ownership metadata to remain first-class and separate from provenance, so that recipe export builds on the current attribution model without redesigning it.
27. As a verifier author, I want recipe fingerprints to be deterministic, so that structurally different runnable inputs do not collapse under the same input ID.
28. As a verifier author, I want recipe fingerprints included in input variant identity, so that input IDs reflect executor-facing structure rather than only human-readable summaries.
29. As a verifier author, I want ownership not to become a semantic dedup key by itself, so that multi-owner inputs can still represent one input with multiple eligible feature owners.
30. As a maintainer, I want recipe schema versioning separate from catalog schema versioning, so that recipe evolution is explicit.
31. As a maintainer, I want the scenario catalog schema version bumped, so that consumers can detect the changed output shape.
32. As a verifier author, I want deterministic recipe JSON content and ordering, so that repeated runs are reproducible.
33. As a verifier author, I want enriched catalog records to preserve input recipes, so that dynamic evidence does not strip executor-facing input data.
34. As a verifier author, I want dynamic joiner behavior unchanged, so that this PRD does not mix recipe export with attribution semantics.
35. As a verifier author, I want dummyapp coverage first, so that representative recipe shapes are validated in a controlled fixture before Quizzes smoke validation.
36. As a verifier author, I want Quizzes smoke validation after dummyapp coverage exists, so that the design is checked against the realistic target without making brittle count claims.
37. As a future maintainer, I want docs to explain the structured recipe contract, so that executor work can start from a stable description rather than reading verifier internals.
38. As a future maintainer, I want a concrete dummyapp JSON example in docs or tests, so that recipe semantics are easy to understand.
39. As a verifier author, I want no executor implementation in this work, so that export structure can be stabilized before runtime execution is attempted.
40. As a verifier author, I want no runtime materializer in this work, so that recipe correctness can be validated as exported data first.
41. As a verifier author, I want no semantic input deduplication in this work, so that identity and materialization structure do not get bundled with equivalence reasoning.
42. As a verifier author, I want unresolved receiver, missing type, ambiguous mutation, and unknown value cases to remain visible, so that the catalog is honest about what is not runnable yet.

## Implementation Decisions

- The chosen export shape is embedded recipes inside input variant records, not a sidecar keyed by input variant ID.
- Embedding is chosen because the future executor should be able to consume each scenario plan without a mandatory second-file join.
- A sidecar remains a possible future optimization if catalog size becomes a proven problem, but it is not the v1 design.
- The public recipe model will be an executor-facing DTO mapped from internal trace recipes.
- Public recipe node kinds will be stable action-oriented concepts rather than raw internal Groovy enum names.
- The embedded input field name will be `inputRecipe`.
- The recipe payload field name for its schema version will be `schemaVersion`.
- The recipe schema version will be `microservices-simulator.input-recipe.v1`.
- The scenario catalog schema version will be bumped to `microservices-simulator.scenario-catalog.v2` because the input record shape changes.
- The rejected input line schema version will be `microservices-simulator.scenario-catalog-rejected-input.v2`.
- The enriched catalog schema version will be bumped to `microservices-simulator.scenario-catalog-enriched.v2` if enriched records include scenario plans with the v2 input shape.
- The enriched manifest schema version will be bumped to `microservices-simulator.scenario-catalog-enriched-manifest.v2` if its output contract references the v2 enriched catalog.
- Backward compatibility with older local catalog artifacts is not required.
- Existing textual fields remain available: constructor summaries, stable source text, provenance text, warnings, source-mode evidence, and owners are preserved.
- Ownership is current baseline data and is consumed as input metadata. This PRD does not alter owner derivation or matching semantics.
- Accepted inputs will expose `inputRecipe` inside the input variant embedded in scenario plans.
- Rejected input export will use a wrapper record containing `schemaVersion`, `input`, `rejectionReason`, and `rejectionWarnings`.
- Rejected input recipes are exported when available. Rejection remains separate from executor readiness.
- `inputRecipe.executorReady` means materializability of the input recipe, not acceptance by the saga-catalog source-mode policy.
- `PARTIAL` and `UNRESOLVED` inputs will retain best-effort recipe trees with explicit blockers instead of switching to a different schema.
- Blocking reasons should be deterministic symbolic values, not only free-form prose.
- Free-form diagnostics may be retained alongside symbolic blockers when useful for humans.
- Constructor arguments in the recipe will be ordered by argument index.
- Each constructor argument entry will carry index, expected type when known, argument-level status, readiness, blockers, provenance text, and a nested recipe node.
- Literal nodes will carry a typed literal kind, a machine value when safely known, original source text, readiness, and blockers if typing is unsafe.
- Constructor nodes will carry target type FQN when executor-ready, source or display type text for diagnostics, ordered positional argument recipes, and assignment entries.
- If a constructor or DTO creation node is otherwise executor-ready but target type FQN cannot be determined, that node and its containing input are not executor-ready.
- Named arguments are represented as constructor assignment entries rather than only as map-like source text.
- Setter assignments are represented with target property name, source setter name, value recipe, and deterministic order.
- Simple property assignments are represented with target property name, value recipe, and deterministic order.
- Assignment capture is limited to simple deterministic same-scope construction and mutation before saga/facade use.
- Alias-heavy mutation, conditional or loop-dependent mutation order, dynamic property mutation, reflection, and ambiguous multi-writer cases become blockers rather than guessed recipes.
- Collection nodes will distinguish list, set, map, and unknown collection kinds.
- Map nodes will carry ordered key/value recipe entries.
- Transform nodes will represent local transforms and coercions such as `toSet()` and `as Set` with a transform name, optional target type, receiver recipe, source text, readiness, and blockers.
- Local transforms over resolved collections may be executor-ready when the transform is supported by the recipe schema.
- Local transforms over runtime or unresolved receivers must either compose with a replayable child recipe or mark the affected node as not executor-ready with a deterministic blocker.
- Source order is preserved for literal collections and assignments; stable sorted ordering is used only where source order is unavailable.
- Helper-return nodes represent verifier-reduced helper results. They preserve helper provenance and nested result recipe, but do not require the future executor to call the Spock helper.
- Call-result nodes represent values that may need runtime invocation. They include receiver recipe or reference, method name, ordered argument recipes, expected return type when known, source text, provenance, readiness, and blockers.
- A missing actionable receiver for an executor-ready method call is a blocker.
- There is no public `facade` or `creation_site` execution kind in the recipe schema.
- Existing creation-site or facade-analysis facts may remain diagnostic provenance, but the executor-facing action remains a generic call result when invocation is required.
- Property-access nodes read a named property or accessor result from a child recipe result.
- Placeholder nodes carry placeholder identity, expected type when known, and placeholder purpose such as injectable or source-provided value.
- Runtime-produced and replayable values are represented through call-result and property-access composition, not through prose-only runtime edge summaries.
- Unknown or unresolved nodes carry best-known source text, provenance, internal category when helpful, readiness false, and blockers.
- A recipe fingerprint will be derived from normalized recipe structure and exported in `inputRecipe.recipeFingerprint`.
- The input variant deterministic ID hash inputs will be the existing normalized identity inputs plus `inputRecipe.recipeFingerprint`.
- Existing logical key bindings remain part of the input variant deterministic ID hash.
- Owners remain outside the input variant deterministic ID hash so one input can carry multiple eligible owners without being split solely by ownership metadata.
- The recipe fingerprint should not be raw JSON text; it should be derived from a stable normalized representation.
- Owners should remain metadata for attribution and should not by themselves split otherwise equivalent input recipes.
- Deterministic output requires stable argument ordering, stable assignment ordering, stable map ordering, stable blocker ordering, no hidden timestamps in recipes, and no hidden randomness.
- Full canonical JSON formatting is not required.
- Dynamic input maps remain attribution-focused runtime artifacts in v1. They should not become the executor recipe contract unless a later PRD explicitly changes that scope.
- Dynamic enriched catalog writing must preserve recipes embedded in scenario plans and must not recompute, strip, or reinterpret them.
- Documentation must make clear that structured recipes are the machine-readable contract and textual summaries remain diagnostics.

## Testing Decisions

- Tests should verify behavior through public verifier interfaces and exported artifacts, not private traversal details.
- Development should follow vertical tracer-bullet slices rather than writing every test up front.
- The first tracer bullet should prove that one dummyapp direct constructor input exports an embedded `inputRecipe` with typed literal or constructor structure, a schema version, a recipe fingerprint, and unchanged textual summaries.
- The next slices should add helper-return property access, replayable call-result edges, injectable placeholders, DTO assignment capture, list/set/map construction, transform/coercion export, rejected-input wrapper export, and enriched-catalog recipe preservation.
- A focused public recipe-mapping module can have direct tests if it exposes a stable public interface and behaves as a deep module. Those tests should assert recipe behavior and normalized output, not private helper calls.
- Scenario adapter tests should assert that adapted input variants carry recipes, owners, statuses, summaries, warnings, and deterministic IDs through the public model surface.
- Catalog writer tests should assert accepted JSONL shape, rejected wrapper shape, schema versions, recipe fingerprints, deterministic ordering, and preservation of textual diagnostics.
- Dynamic enrichment tests should assert that enriched scenario records preserve embedded input recipes while join statuses and attribution behavior remain unchanged.
- Dummyapp fixture coverage is the primary behavior proof for representative input shapes.
- Dummyapp tests should cover at least direct constructor, helper-return, call-result, runtime-produced value plus property access, injectable placeholder, named-arg DTO creation, setter/property assignment, list/set/map construction, `toSet()`, `as Set`, runtime `.toSet()` handling, and partial/unresolved blockers.
- Quizzes validation should be a smoke check after dummyapp coverage exists. It should confirm that catalog generation still succeeds and representative recipes are present, not assert fragile exact scenario counts.
- Tests should avoid requiring a scenario executor, runtime materializer, or semantic deduplication behavior.
- Tests should avoid overfitting to incidental warning prose unless the warning text is part of the intended public diagnostic contract.
- Determinism tests should compare stable semantic fields and fingerprints. They do not need signing-style canonical JSON byte-for-byte assertions.
- Good failure tests should show that missing target types, unresolved call receivers, and ambiguous mutation paths become explicit blockers instead of silently executable recipes.
- Existing ownership behavior should be treated as baseline. Recipe tests should verify owners are preserved, not re-test ownership derivation exhaustively.

## Acceptance Criteria

- Accepted input variants in the scenario catalog include schema-versioned `inputRecipe` payloads when trace recipes exist.
- Rejected input JSONL uses a wrapper shape containing the full input object and rejection metadata.
- Rejected inputs include `inputRecipe` when trace recipes exist.
- `inputRecipe` includes a deterministic recipe fingerprint.
- Input variant deterministic IDs include the recipe fingerprint.
- Scenario catalog schema version is bumped.
- Recipe schema version is explicit.
- Rejected input wrapper schema version and envelope fields are explicit.
- Human-readable constructor summaries, provenance text, source-mode evidence, owners, and warnings remain available.
- `RESOLVED` and `REPLAYABLE` recipes can be marked executor-ready when required structure is present.
- `PARTIAL` and `UNRESOLVED` recipes use the same shape but are clearly not executor-ready with blocking reasons.
- Dummyapp fixtures demonstrate representative `RESOLVED` and `REPLAYABLE` structured exports.
- Tests cover direct constructor, helper-based, and call-result-based inputs.
- Tests cover named args, setter/property assignments, collection construction, map construction, local transforms/coercions, placeholders, runtime-produced values, and property access on replayed/runtime results.
- Enriched catalog export preserves embedded recipes.
- Dynamic joiner behavior is unchanged by this work.
- Documentation explains recipe schema, readiness semantics, accepted versus rejected input handling, and includes a concrete dummyapp JSON example or equivalent fixture-backed example.
- Quizzes smoke validation is run after dummyapp coverage and does not contradict the focused tests.

## Out of Scope

- Scenario executor implementation.
- Runtime materializer implementation.
- Runtime saga/functionality instantiation.
- Runtime fault injection.
- Helper/setup ownership model changes.
- Dynamic joiner logic changes.
- Semantic input deduplication.
- Stronger same-feature sibling disambiguation.
- Broad arbitrary object mutation analysis.
- Reflection, metaClass, dynamic property mutation, or alias-heavy mutation support.
- Treating Quizzes-style `Functionalities` facades as a required public recipe concept.
- TCC runtime execution support.
- Dynamic input-map redesign as an executor materialization contract.

## Further Notes

- The current verifier already has internal structured value recipes. This work is about preserving and stabilizing an executor-facing subset of that structure in exported artifacts.
- The most important design boundary is honesty about materializability. A recipe may be useful and structured even when it is not executor-ready.
- Rejected input export is intentionally cleaned up because backward compatibility is not required and the current flattened shape would otherwise lose input-owned fields as the input model grows.
- Embedding recipes makes catalog lines larger. That is an accepted v1 tradeoff because it minimizes future executor complexity.
- The recipe schema should be documented as a verifier output contract, not as a promise that every trace shape is executable today.
