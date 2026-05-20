# Intake

## Goal
Improve dynamic enrichment attribution for saga inputs by:
- adding explicit ownership metadata for helper/setup/field-derived inputs
- stopping ambiguity from one input set spilling onto unrelated scenario plans

Ship this as a planning-scoped change for verifier + simulator attribution/join behavior, with strong behavioral tests, dummyapp regression coverage, documentation updates, and a Quizzes directional smoke rerun.

## Context
The current pipeline is:
1. `verifiers/` statically analyzes Groovy/Spock tests into `InputVariant`s
2. `verifiers/` writes `dynamic-input-map.json` per selected test class
3. `simulator/` uses that map at runtime to stamp step events with `inputVariantId` when possible
4. `verifiers/` joins runtime events back onto scenario plans

Two concrete problems are in scope:
- Fix 1: helper/setup/field-created inputs are modeled with provenance like `sourceMethodName=createUser` or `setup`, but runtime test identity is usually the owning feature method, so valid executions are missed or downgraded.
- Fix 2: verifier-side joining can mark a plan `AMBIGUOUS` because neighboring inputs are ambiguous, even when the current plan's own input id is not among the ambiguous identity matches.

Relevant repo areas include:
- `verifiers/.../scenario/model/InputVariant.java`
- `verifiers/.../scenario/adapter/ApplicationAnalysisScenarioModelAdapter.java`
- `verifiers/.../dynamic/DynamicInputMapWriter.java`
- `verifiers/.../dynamic/DynamicEvidenceJoiner.java`
- `simulator/.../monitoring/dynamic/DynamicInputMap.java`

## Decisions
- Fix 1 uses an explicit ownership model, not a matcher-only heuristic.
- Ownership is first-class model data on analyzed inputs and is propagated into dynamic input maps.
- Ownership is additive metadata for this work; current deterministic ids and dedup semantics should stay stable as much as possible.
- Ownership assignment is conservative: only attach owners when static analysis can justify the relationship through the spec hierarchy.
- Ownership can have multiple owners per input.
- In scope for ownership modeling: helper methods, `setup()`, field initializers, and inherited helper/setup/field paths.
- `setupSpec()` ownership metadata is in scope, but improved runtime attribution for `setupSpec()` executions is not required.
- Ownership semantics should be used by both simulator runtime attribution and verifier fallback matching.
- Fix 2 is verifier-side only in this work.
- Genuine same-feature sibling ambiguity remains valid; this work only stops spillover onto unrelated plans.
- Main proof should come from focused tests first, then dummyapp end-to-end regression coverage, then a Quizzes directional smoke rerun.
- Clean schema/documentation changes are allowed; backward compatibility is not a goal on this branch.

## Recommended Defaults Accepted
- Do not solve semantic dedup of value-equivalent inputs in this work.
- Do not add new runtime payload/schema expansion for fix 2 unless needed by implementation detail.
- Do not promise exact Quizzes count targets; require directional improvement and no contradiction with focused tests.
- Update implementation docs to explain ownership semantics, runtime matching behavior, and ambiguity containment.

## Out Of Scope
- Executor/materializer work
- Semantic dedup of equivalent inputs
- Disambiguating genuinely indistinguishable same-feature sibling inputs
- Guaranteed improved runtime attribution for `setupSpec()` executions
- Broader dynamic-enrichment cleanup unrelated to fixes 1 and 2, such as failed-test reporting splits, excluded-source coverage buckets, or uncapped catalog policy redesign

## Risks And Tradeoffs
- Conservative ownership may leave some valid helper/setup relationships unmatched.
- Overly broad ownership would create false positive runtime attribution, so the model must prefer soundness over coverage.
- Keeping current ids/dedup semantics avoids catalog churn, but it means some semantically equivalent inputs remain distinct for now.
- Same-feature sibling ambiguity will remain in some cases and must be documented as expected behavior, not a bug.
- Runtime attribution and verifier fallback matching must stay semantically aligned, or the new ownership model will be inconsistent across the pipeline.
