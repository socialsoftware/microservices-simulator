## Parent PRD

`issues/structured-input-recipes/prd.md`

## Type

AFK

## What to build

Extend structured recipes for collection construction and local transforms/coercions. Dummyapp list, set, map, `toSet()`, `as Set`, and runtime `.toSet()` cases should export deterministic machine-readable nodes with clear readiness semantics.

## Acceptance criteria

- [x] Collection nodes include `collectionKind` with values for list, set, map, or unknown.
- [x] List and set entries preserve source order where source order is known.
- [x] Map nodes include ordered key/value recipe entries.
- [x] Basic map literal support is covered by a dummyapp fixture or equivalent verifier fixture.
- [x] Local transform nodes represent `toSet()` with transform name, receiver recipe, source text, readiness, and blockers.
- [x] Local coercion nodes represent `as Set` with transform name or target type, receiver recipe, source text, readiness, and blockers.
- [x] Supported transforms over resolved collections can be executor-ready.
- [x] Runtime or unresolved transform receivers compose with replayable child recipes when safe, or produce deterministic blockers when not executor-ready.
- [x] Partial/unresolved collection children make the containing argument readiness and blockers explicit.

## Verification

- Added public `collection` and `local_transform` nodes for list/set/map literals, ordered map entries, `toSet()`, and `as Set` coercions with readiness/blocker propagation.
- Verified ordered list/map children, map entry structure, executor-ready resolved transforms, and unresolved receiver blockers in `InputRecipeMapperSpec`.
- Verified dummyapp representative recipe shapes include collection and `toSet` transform nodes in `ApplicationAnalysisScenarioModelAdapterSpec`.
- Ran `mvn -q -Dtest=InputRecipeMapperSpec,ApplicationAnalysisScenarioModelAdapterSpec test` in `verifiers` successfully.

## Blocked by

- Blocked by `issues/structured-input-recipes/001-accepted-direct-recipes.md`

## User stories addressed

- User story 7
- User story 16
- User story 17
- User story 18
- User story 19
- User story 42
