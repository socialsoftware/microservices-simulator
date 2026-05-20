# Intake

## Goal

Extend verifier exports so scenario inputs carry executor-facing structured input recipes, not only human-readable summaries. The output must preserve enough structure for a future executor to instantiate input values, instantiate the saga, and run saga steps without parsing summary strings.

## Context

Current verifier outputs include provenance-rich `InputVariant`s with `constructorArgumentSummaries`, `provenanceText`, `owners`, and statuses like `RESOLVED`, `REPLAYABLE`, `PARTIAL`, and `UNRESOLVED`.

The current baseline already includes ownership metadata from the ownership/ambiguity work. This PRD must consume that metadata, not redesign ownership.

The trace layer already has structured `GroovyValueRecipe` trees, but `ApplicationAnalysisScenarioModelAdapter` still collapses them into text for scenario catalog export. This PRD restores that structure as a stable executor-facing recipe schema.

## Decisions

Use embedded `inputRecipe` payloads on `InputVariant`, not a sidecar file.

Export recipes for accepted and rejected inputs when recipes exist.

Change rejected-input JSONL to a wrapper shape containing the full input object plus rejection metadata, so accepted and rejected inputs do not drift structurally.

Use one recipe schema across `RESOLVED`, `REPLAYABLE`, `PARTIAL`, and `UNRESOLVED`; expose `executorReady`, per-node readiness, and blocking reasons.

Use stable executor-facing node kinds, not raw Groovy internals: literals, constructors, assignments, collections, helper results, call results, property access, placeholders, unresolved nodes.

No public facade-specific execution kind. A `userFunctionalities.createUser(...)` style call is just a replayable `call_result` if executor must invoke it.

Support typed literals, constructor target FQNs, named args, setter/property assignments, list/set/map construction, helper-return values, runtime/replayable call edges, injectable placeholders, and property access on replayed/runtime results.

Include a deterministic recipe fingerprint in `inputVariantId` and export it in the recipe.

Version both the scenario catalog and the recipe schema. Backward compatibility with old local artifacts is not required.

## Recommended Defaults Accepted

Keep human-readable summaries, ownership, source-mode metadata, and provenance text.

Keep v1 verifier-side only: no executor, no runtime materializer.

Require deterministic output but not canonical JSON signing-style formatting.

Use dummyapp first for tests and examples, then Quizzes smoke validation.

Update verifier implementation docs with recipe semantics and a concrete dummyapp JSON example.

## Out Of Scope

Scenario executor implementation.

Runtime materializer implementation.

Ownership model changes.

Dynamic joiner logic changes.

Semantic input deduplication.

Arbitrary object mutation tracing beyond simple deterministic same-scope construction and assignments.

Facade/app-architecture-specific execution semantics.

## Risks And Tradeoffs

Embedding recipes duplicates payloads across scenario lines, but keeps executor consumption straightforward.

Including recipe fingerprints in IDs may create catalog churn, but avoids collapsing structurally different runnable inputs.

Changing rejected-input JSONL shape is a contract break, but backward compatibility is not required and the wrapper shape prevents schema drift.

Simple assignment support is necessary for DTO materialization, but broad mutation analysis is intentionally deferred.

Missing target types, unresolved receivers, ambiguous mutation, or unknown values must be explicit blockers, not guessed.
