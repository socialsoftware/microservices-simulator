## Problem Statement

Dynamic enrichment currently gives an incomplete and sometimes misleading picture of which static saga inputs were actually exercised at runtime.

The first issue is that the static model conflates provenance with ownership. An analyzed input remembers where the saga or facade call was discovered, such as a helper method, `setup()`, a field initializer, or an inherited fixture path. Runtime evidence, however, is tagged with the active feature test identity. When a helper-created or fixture-created input really belongs to a feature, the runtime matcher can fail to recognize that relationship because it compares the active feature name too literally against the static source method. This produces avoidable `UNMATCHED` or weaker-than-necessary results even when useful runtime evidence exists.

The second issue is that verifier-side joining can spread ambiguity too broadly. When multiple neighboring inputs are genuinely ambiguous with each other, the current join behavior can still mark unrelated scenario plans as `AMBIGUOUS` even when the current plan's own input is not among the ambiguous identity matches. That makes the enriched catalog noisier and less trustworthy.

These problems matter because dynamic enrichment is the bridge between the static scenario catalog and future runtime execution work. If the bridge is noisy or semantically inconsistent, it becomes harder to tell whether a miss is a real coverage gap, a real ambiguity, or just an attribution bug. The result is slower iteration on Quizzes, less confidence in dummyapp regression coverage, and a weaker basis for the later scenario executor.

## Solution

Introduce an explicit ownership model for analyzed saga inputs and use it consistently across static export, simulator runtime attribution, and verifier fallback matching.

Ownership will become a first-class property of an analyzed input, separate from provenance. Provenance will still describe where the input was discovered, but ownership will describe which feature methods the input belongs to. The model will support multiple owners per input and will be assigned conservatively, only when static analysis can justify the relationship through the spec hierarchy.

The ownership model will cover direct feature-created inputs, helper-created inputs, `setup()`-created inputs, field-initialized inputs, and inherited helper/setup/field paths. `setupSpec()` ownership metadata will also be preserved so the model stays complete, but improved runtime attribution for `setupSpec()` executions is not a goal of this work.

The simulator runtime matcher will use ownership-aware matching rather than treating the static source method as the sole owner identity. An input with multiple owners should be eligible when the active feature matches any declared owner, and ineligible otherwise. Ownership should narrow the candidate set, not broaden it across unrelated features. The verifier fallback matcher will use the same ownership semantics so that both sides of the pipeline agree on what it means for a runtime execution to belong to a static input.

For ambiguity handling, the verifier joiner will become plan-local. Genuine same-feature sibling ambiguity will remain a valid result. However, ambiguity will no longer spill onto unrelated plans. A scenario plan should only be marked ambiguous when the ambiguous identity set actually includes that plan's input. Exact runtime ids will continue to win when they belong to the plan being enriched.

The public status ladder remains the same. This work improves which existing status is produced, rather than inventing new statuses. A uniquely resolved runtime input id should still produce `MATCHED_EXACT`. When a runtime event lacks a direct id but ownership-aware verifier matching finds one unique complete-identity match, the expected result remains `MATCHED_HIGH_CONFIDENCE`. When complete test identity is missing, the existing `MATCHED_PARTIAL` behavior remains the ceiling. Ownership-based fixes should primarily turn false `UNMATCHED` or false foreign-plan `AMBIGUOUS` results into the correct existing statuses.

The work will include documentation updates, focused behavior-first tests, dummyapp end-to-end regression coverage, and a Quizzes directional smoke rerun to confirm that the noise profile improves without overclaiming exact count targets.

## User Stories

1. As the verifier author, I want helper-created saga inputs to carry explicit owning feature metadata, so that runtime enrichment can recognize that they belong to a feature even when the helper name differs from the feature name.
2. As the verifier author, I want `setup()`-created inputs to be attributed to the feature executions that depend on them, so that fixture preparation does not look like missing runtime coverage.
3. As the verifier author, I want field-initialized inputs to preserve both provenance and ownership, so that static traces remain explainable while runtime matching stays feature-aware.
4. As the verifier author, I want inherited helper, setup, and field fixture paths to participate in ownership modeling, so that base-spec fixture behavior is represented consistently.
5. As the verifier author, I want an input to support more than one owning feature method, so that shared helper or setup-created inputs can be modeled honestly.
6. As the verifier author, I want ownership assignment to stay conservative, so that enrichment does not create false matches just because a helper exists somewhere in the class hierarchy.
7. As the verifier author, I want provenance and ownership to be separate concepts, so that source tracing remains useful without forcing runtime attribution to depend on raw source method names.
8. As the verifier author, I want the scenario model to carry ownership as first-class data, so that ownership is visible beyond one reporting step and can support later executor-facing work.
9. As the verifier author, I want dynamic input-map artifacts to expose ownership metadata cleanly, so that runtime attribution has an explicit contract rather than hidden heuristics.
10. As the verifier author, I want simulator runtime attribution to use ownership-aware matching, so that feature executions can resolve owned helper/setup inputs when the static relationship is justified.
11. As the verifier author, I want verifier fallback matching to use the same ownership semantics as runtime attribution, so that the pipeline does not disagree with itself about what belongs to a feature.
12. As the verifier author, I want direct runtime ids to remain the strongest signal, so that exact attribution stays conservative and trustworthy.
13. As the verifier author, I want genuine same-feature sibling ambiguity to remain visible, so that the system does not pretend to distinguish runtime executions it cannot actually distinguish.
14. As the verifier author, I want unrelated scenario plans to stop inheriting somebody else's ambiguity, so that `AMBIGUOUS` means "this plan is ambiguous" rather than "something nearby was ambiguous."
15. As the verifier author, I want a plan with a foreign direct runtime id to avoid being promoted by semantic fallback, so that exact evidence for another input does not pollute the current plan.
16. As the verifier author, I want dummyapp to provide controlled regression coverage for helper, setup, field, inherited, and sibling-ambiguity cases, so that future attribution refactors can be validated cheaply.
17. As the verifier author, I want Quizzes smoke results to improve directionally after the change, so that the design proves useful on the real target application and not only on a fixture.
18. As the verifier author, I want documentation to explain ownership semantics and ambiguity containment clearly, so that I can understand future enrichment output without reverse-engineering the code.
19. As a future scenario-executor author, I want ownership to be represented explicitly in analyzed inputs, so that the static model becomes easier to reason about when deciding which recipes belong to which feature flows.
20. As a future maintainer, I want tests to verify public attribution behavior instead of implementation details, so that internal refactors of the matcher or trace model do not cause noisy, low-value test failures.
21. As a future maintainer, I want schema and documentation updates to be clean rather than compatibility-driven, so that the ownership model stays understandable on this branch.
22. As the verifier author, I want `setupSpec()` ownership metadata preserved even if runtime attribution is not improved for it yet, so that the analysis model stays complete and future work has a stable place to extend from.
23. As the verifier author, I want this work to stop short of semantic input deduplication, so that attribution quality can improve without bundling a larger execution-equivalence redesign.
24. As the verifier author, I want this work to stop short of same-feature sibling disambiguation, so that the plan remains focused on truthful ownership and ambiguity containment rather than inventing new runtime identity mechanisms.

## Implementation Decisions

- Ownership will be represented as first-class analyzed-input metadata, separate from provenance.
- Ownership will be allowed to have multiple owners per input rather than forcing one canonical owner.
- Multiple owners are an eligibility list, not an ambiguity source by themselves. During runtime attribution and verifier fallback, an input is eligible if the active feature matches any declared owner. Inputs whose owner sets do not include the active feature should be excluded before ambiguity classification.
- Ownership assignment will be conservative and based only on statically justified relationships through the specification hierarchy.
- The ownership model will cover direct feature-created inputs, helper-created inputs, `setup()`-created inputs, field-initialized inputs, and inherited helper/setup/field paths.
- `setupSpec()` ownership metadata will be modeled and exported, but improved runtime attribution for `setupSpec()` executions is not part of the required behavior for this work.
- Existing deterministic ids and current dedup semantics should remain stable as much as possible; this work improves attribution semantics without redefining execution equivalence.
- The analyzed-input contract and the dynamic input-map contract may be cleaned up structurally to represent ownership clearly. Backward compatibility with older local artifacts is not a goal on this branch.
- A dedicated ownership-bearing module boundary should exist conceptually between Groovy trace extraction and downstream attribution. The trace layer remains responsible for source provenance, while the adapted input model becomes responsible for explicit ownership facts.
- Runtime attribution should use ownership-aware matching over the current test identity, workflow identity, and step identity, rather than relying on raw provenance method names as the effective owner.
- Verifier fallback matching should consume the same ownership semantics as simulator runtime attribution so the static-to-runtime bridge stays coherent.
- Exact runtime attribution remains conservative: an explicit runtime input id is still the only basis for `MATCHED_EXACT`.
- Ownership-aware matching is expected to improve outcomes within the existing join-status model: unique direct runtime attribution should produce `MATCHED_EXACT`, unique complete-identity fallback should produce `MATCHED_HIGH_CONFIDENCE`, and incomplete-identity fallback should remain `MATCHED_PARTIAL` at best.
- Fix 2 is intentionally verifier-side. The planned change is to contain ambiguity to plans whose input ids actually participate in the ambiguous identity set, and to prevent semantic fallback from promoting a plan when direct runtime evidence points to a different input.
- Genuine same-feature sibling ambiguity remains expected behavior and should not be reclassified away in this work.
- No semantic deduplication of value-equivalent inputs is included in this PRD.
- No broader dynamic-enrichment cleanup is bundled here beyond ownership modeling, ownership-aware matching, ambiguity containment, and the necessary documentation/schema updates.

## Testing Decisions

- Good tests for this work should verify externally observable attribution behavior through public interfaces, not internal helper functions, traversal details, or intermediate private data structures.
- Testing should follow tracer-bullet logic through vertical slices. The first slice should prove that owned inputs can be modeled and resolved behaviorally in a small deterministic case. The next slice should prove ambiguity containment. Later slices should extend that proof to dummyapp end-to-end and then to a Quizzes smoke rerun.
- The highest-value public behaviors to test are:
  - analyzed inputs expose explicit ownership for justified helper/setup/field/inherited cases;
  - multiple owners can be represented without broadening ownership beyond what static analysis justifies;
  - runtime input resolution matches owned feature executions while staying conservative for non-owned cases;
  - multi-owner inputs match when any declared owner is active and are excluded when none are active;
  - verifier fallback matching follows the same ownership semantics as runtime resolution;
  - direct runtime ids still win when they belong to the plan being enriched;
  - ownership-aware unique runtime attribution upgrades false misses to `MATCHED_EXACT` when a direct id is emitted;
  - ownership-aware unique verifier fallback upgrades false misses to `MATCHED_HIGH_CONFIDENCE` when complete test identity exists, and never exceeds `MATCHED_PARTIAL` when it does not;
  - ambiguity no longer spills onto unrelated plans;
  - genuine same-feature sibling ambiguity still remains ambiguous;
  - `setupSpec()` ownership metadata is preserved without requiring improved runtime attribution;
  - dummyapp dynamic enrichment reflects the intended ownership and ambiguity behavior end to end;
  - Quizzes smoke results improve directionally without depending on brittle exact-number assertions.
- Prior art for test style should come from the existing attribution-oriented specs and integration checks already in the repository, especially the dummyapp trace specs, input-map resolution tests, dynamic joiner specs, and dynamic enrichment integration specs.
- Tests should prefer public interfaces such as adapted input records, exported input maps, runtime input-map resolution behavior, and enriched join statuses. They should avoid asserting on private traversal order, incidental serialization formatting, or fragile warning text unless that warning text is itself part of the intended user-facing behavior.
- Quizzes validation should be a smoke-level behavior check, not the primary unit of truth. Exact counts should not be hard-coded as acceptance criteria because they are too sensitive to unrelated catalog shape changes.
- Out-of-scope edge cases for testing in this work include semantic equivalence dedup, stronger same-feature sibling disambiguation, executor materialization behavior, and any runtime-attribution guarantees specifically for `setupSpec()` execution contexts.

## Out of Scope

- Scenario executor or recipe materialization work.
- Semantic deduplication of value-equivalent inputs.
- Disambiguating genuinely indistinguishable same-feature sibling inputs.
- Guaranteed improved runtime attribution for `setupSpec()` executions.
- New runtime payload expansion for deeper disambiguation, unless a minimal implementation detail makes a small payload adjustment unavoidable.
- Broader dynamic-enrichment cleanup outside ownership modeling and ambiguity containment, including failed-test reporting redesign, excluded-source coverage bucketing, or catalog policy redesign.

## Further Notes

- This PRD is intentionally about making dynamic enrichment more truthful and easier to trust, not about making the full scenario executor possible by itself.
- The most important conceptual cleanup is separating provenance from ownership. That distinction should stay explicit in the final design and documentation.
- If this work succeeds, the next attribution-related decisions can be made from a cleaner baseline: semantic dedup, richer runtime disambiguation signals, and executor-facing input materialization can all build on top of an ownership-aware model instead of a provenance-only model.
