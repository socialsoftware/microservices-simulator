# Structured input recipes

Structured input recipes are the machine-readable contract for replay-oriented scenario inputs. They are embedded on `InputVariant.inputRecipe` in the scenario catalog instead of being written to a sidecar file.

Existing summaries, constructor-argument summaries, provenance text, source-mode metadata, warnings, and ownership fields remain diagnostic context. They help humans explain where an input came from and why it was accepted or rejected, but future executor work should read the structured recipe tree for materialization decisions.

## Where Recipes Appear

Accepted scenario records use `schemaVersion=microservices-simulator.scenario-catalog.v2`. Each accepted `ScenarioPlan.inputs[]` entry can include:

```json
{
  "inputVariantId": "input-...",
  "sourceClassFqn": "com.example.dummyapp.GroovySagaTracingSpec",
  "sourceMethodName": "named args, setters, and toSet provenance feed item saga constructor",
  "owners": ["com.example.dummyapp.GroovySagaTracingSpec#named args, setters, and toSet provenance feed item saga constructor"],
  "constructorArgumentSummaries": ["arg[1]: setterDto <- new ItemDto()"],
  "inputRecipe": {
    "schemaVersion": "microservices-simulator.input-recipe.v1",
    "executorReady": true,
    "blockers": [],
    "recipeFingerprint": "sha256:...",
    "arguments": []
  }
}
```

Rejected input records use `schemaVersion=microservices-simulator.scenario-catalog-rejected-input.v2` and are wrapped:

```json
{
  "schemaVersion": "microservices-simulator.scenario-catalog-rejected-input.v2",
  "input": { "inputVariantId": "input-...", "inputRecipe": { "schemaVersion": "microservices-simulator.input-recipe.v1" } },
  "rejectionReason": "SOURCE_MODE_TCC",
  "rejectionWarnings": ["input source mode is TCC and cannot be emitted to the saga catalog"]
}
```

The rejected wrapper keeps rejection metadata separate from recipe readiness. A rejected input can still have a useful recipe; catalog acceptance and recipe materializability are different concepts.

## Readiness And Status

Recipes use one schema across `RESOLVED`, `REPLAYABLE`, `PARTIAL`, and `UNRESOLVED` inputs.

- `executorReady` on the recipe means every argument and node needed for materialization is ready and the input status allows execution.
- `executorReady` on an argument means that argument's root recipe node is ready.
- `executorReady` on a node means that node and all required child nodes are ready for the supported action represented by the node kind.
- `blockers` is a deterministic list explaining why the recipe, argument, assignment, or node is not ready.

Common blockers include `MISSING_TARGET_TYPE`, `MISSING_RECIPE_NODE`, `MISSING_CALL_RECEIVER`, `TRANSFORM_RECEIVER_NOT_READY`, `UNSUPPORTED_TRANSFORM`, `AMBIGUOUS_MULTI_WRITER:<property>`, `CONDITIONAL_MUTATION`, and `LOOP_DEPENDENT_MUTATION`.

Input status is still preserved independently:

- `RESOLVED` means static analysis resolved the value shape directly.
- `REPLAYABLE` means the value shape is action-oriented enough for a future executor to replay, even if it originated from a helper, property access, or supported call shape.
- `PARTIAL` means some parts are structured, but one or more children are unresolved or blocked.
- `UNRESOLVED` means no safe replay recipe exists for the relevant value.

## Ownership And Provenance

Ownership and provenance intentionally serve different purposes.

- Provenance fields, `sourceClassFqn`, `sourceMethodName`, `sourceBindingName`, and constructor summaries explain where the input was found and how static analysis reached it.
- `owners` identify which feature methods are allowed to claim the input during runtime attribution.
- Recipe identity is based on the structured recipe fingerprint plus the existing input identity material. Owners are preserved on the input but are not recipe semantics by themselves.

This separation lets dynamic attribution use ownership without making owner labels part of the materialization recipe.

## Representative Recipe Shapes

The dummyapp fixture `GroovySagaTracingSpec` covers representative shapes. A simplified recipe for `new ItemDto(aggregateId: 17, orderId: 23)`, setter/property mutation before use, and a local `toSet()` value looks like this:

```json
{
  "schemaVersion": "microservices-simulator.input-recipe.v1",
  "executorReady": true,
  "blockers": [],
  "recipeFingerprint": "sha256:...",
  "arguments": [
    {
      "index": 1,
      "status": "RESOLVED",
      "executorReady": true,
      "blockers": [],
      "provenanceText": "setterDto <- new ItemDto()",
      "recipe": {
        "kind": "constructor",
        "sourceText": "ItemDto",
        "targetTypeFqn": "com.example.dummyapp.item.aggregate.ItemDto",
        "executorReady": true,
        "blockers": [],
        "arguments": [],
        "assignments": [
          {
            "assignmentKind": "setter",
            "propertyName": "aggregateId",
            "sourceName": "setAggregateId",
            "orderIndex": 0,
            "sourceText": "setterDto.setAggregateId(namedDto.getAggregateId())",
            "executorReady": true,
            "blockers": [],
            "valueRecipe": {
              "kind": "property_access",
              "propertyName": "aggregateId",
              "receiver": { "kind": "constructor", "targetTypeFqn": "com.example.dummyapp.item.aggregate.ItemDto" }
            }
          },
          {
            "assignmentKind": "setter",
            "propertyName": "orderId",
            "sourceName": "setOrderId",
            "orderIndex": 1,
            "valueRecipe": {
              "kind": "call_result",
              "methodName": "size",
              "receiver": {
                "kind": "local_transform",
                "transformName": "toSet",
                "receiver": {
                  "kind": "collection",
                  "collectionKind": "list",
                  "elements": [
                    { "kind": "literal", "literalKind": "integer", "value": 1 },
                    { "kind": "literal", "literalKind": "integer", "value": 2 },
                    { "kind": "literal", "literalKind": "integer", "value": 3 }
                  ]
                }
              }
            }
          },
          {
            "assignmentKind": "property",
            "propertyName": "name",
            "orderIndex": 2,
            "valueRecipe": { "kind": "literal", "literalKind": "string", "value": "sample-item" }
          }
        ]
      }
    }
  ]
}
```

Other public node kinds include:

- `literal` for typed scalar values.
- `collection` for list, set, map, or unknown collection literals; map entries preserve ordered key/value recipes.
- `helper_result` for values reduced through local helper analysis, with a nested `resultRecipe` so a future executor does not need to call the Spock helper.
- `property_access` over a receiver recipe.
- `call_result` for generic replayable or runtime-producing calls. The public schema does not expose a `facade_call` node kind.
- `placeholder` for injectable or source-provided values that must remain explicit and not masquerade as resolved literals.
- `unresolved` for values with no safe replay recipe.

## Dynamic Enrichment

Enriched scenario catalog records preserve `scenarioPlan.inputs[].inputRecipe` from the static plan. Recipe fingerprints and readiness fields are serialization data; dynamic joining does not reinterpret them, and this work does not add new matching, attribution, or ambiguity logic.

The enriched output contract uses `microservices-simulator.scenario-catalog-enriched.v2` and the enriched manifest uses `microservices-simulator.scenario-catalog-enriched-manifest.v2` because enriched records now contain v2 scenario plans.

## Quizzes Smoke Validation

On 2026-05-20, a bounded static Quizzes smoke was run after dummyapp recipe coverage and this contract documentation were in place:

```bash
mvn -q spring-boot:run -Dspring-boot.run.arguments="--verifiers.applications-root=/home/andre/microservices-simulator/applications --verifiers.application-base-dir=quizzes --verifiers.output-root=/home/andre/microservices-simulator/verifiers/target/structured-input-recipes-quizzes-smoke --verifiers.report-html-path=analysis-report.html --verifiers.scenario-catalog.enabled=true --verifiers.dynamic-enrichment.enabled=false --verifiers.scenario-catalog.max-catalog-scenarios=80 --verifiers.scenario-catalog.max-input-variants-per-saga=10 --verifiers.scenario-catalog.max-schedules-per-input-tuple=20"
```

Artifacts were written under `verifiers/target/structured-input-recipes-quizzes-smoke/quizzes-20260520-175058-455/`.

The smoke validated the contract without treating exact scenario counts as acceptance criteria:

- Accepted records used `microservices-simulator.scenario-catalog.v2`.
- Embedded recipes used `microservices-simulator.input-recipe.v1`.
- Rejected input records used `microservices-simulator.scenario-catalog-rejected-input.v2`.
- Representative accepted inputs preserved constructor summaries, provenance text, source-mode metadata/evidence, owners, and record warnings.
- Representative recipe shapes included `constructor`, `literal`, `call_result`, `property_access`, and `placeholder` nodes.
- Remaining Quizzes recipe blockers such as `MISSING_TARGET_TYPE`, `PROPERTY_RECEIVER_NOT_READY`, `UNRESOLVED_VARIABLE`, and `UNKNOWN_VALUE` were interpreted as diagnostic output, not executor-ready claims.

## Out Of Scope

This work does not implement:

- A scenario executor.
- A runtime materializer for constructing objects from recipes.
- Semantic input deduplication for value-equivalent recipes.
- New dynamic joiner matching or attribution behavior.

Treat `inputRecipe` as the stable static contract that makes those future steps possible, not as proof that execution already exists.
