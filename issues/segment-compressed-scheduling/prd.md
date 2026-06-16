## Problem Statement

The verifier exposes `SEGMENT_COMPRESSED` as a schedule strategy, but the current behavior is explicitly a placeholder. For small tuples it behaves like bounded `ORDER_PRESERVING_INTERLEAVING`; for larger tuples it falls back to serial scheduling. This means it does not provide the thesis-style reduction that the name implies, and public claims must currently avoid using it as real segment compression.

This limitation is visible in Quizzes count-only accounting. Size-3 rows with multi-step sagas explode because every step-level order-preserving interleaving is counted, even when many permutations only move internal or non-conflicting steps around and do not change the conflict-relevant order cases. Falling back to serial avoids explosion, but it also collapses conflict-relevant ordering cases too aggressively and is not a credible segment-compressed baseline.

The user needs a deterministic schedule strategy that preserves conflict-relevant ordering cases while collapsing non-conflicting/internal step permutations. The same strategy must drive materialized `ScenarioPlan` schedules and scenario-space-accounting schedule counts, so count-only Quizzes accounting and written catalogs describe the same scenario space under one run configuration.

## Solution

Replace the placeholder `SEGMENT_COMPRESSED` behavior with real deterministic segment-compressed scheduling.

For a selected saga set and input tuple, segment compression identifies conflict-anchor steps: steps that participate in at least one configured cross-saga conflict candidate within the current saga set. The conflict-anchor lens is the run's configured static conflict lens: strict conflict evidence when `allowTypeOnlyFallback=false`, and broad conflict evidence when `allowTypeOnlyFallback=true`. This same lens applies whether the saga set was selected by `INTERACTION_PRUNED` or by `BRUTE_FORCE`.

Each saga's ordered steps are compressed into anchor segments. An anchor segment contains the deterministic in-saga run of non-anchor steps since the previous anchor plus the anchor step itself. Non-anchor tail steps after a saga's final anchor are appended in a canonical deterministic order after all anchor segments have been emitted. If one saga has no anchors while other sagas in the same set do have anchors, that zero-anchor saga contributes its full in-saga step list to the same canonical tail.

The strategy enumerates order-preserving interleavings over conflict-anchor tokens, not over every individual step. Each emitted anchor token expands to its full anchor segment, preserving the saga's internal step order. If a saga set has no conflict anchors, the strategy emits one canonical full schedule. If every step is conflict-relevant, `SEGMENT_COMPRESSED` naturally matches `ORDER_PRESERVING_INTERLEAVING`.

Interaction pruning remains separate. `SEGMENT_COMPRESSED` does not decide which saga sets interact. It only reduces schedules inside whichever saga sets were selected by the configured generation strategy. For `INTERACTION_PRUNED`, the selected saga sets still come from the existing strict or broad interaction graph. For `BRUTE_FORCE`, all input-bound saga sets remain selected under the existing brute-force rules, and segment compression only uses conflict evidence to decide how many schedules those already-selected sets need.

The implementation should expose the compression logic as a deep scheduling module with a small interface that can be tested through public generator/accounting behavior. Materialized generation and compressed accounting must share the same schedule-count semantics rather than duplicating independent formulas that can drift.

## User Stories

1. As a thesis author, I want `SEGMENT_COMPRESSED` to perform real segment compression, so that I can claim it as a schedule-space reduction strategy without caveats about placeholder behavior.
2. As a thesis author, I want Quizzes count-only accounting to shrink size-3 schedule counts under `SEGMENT_COMPRESSED`, so that the evaluation can report a meaningful reduced scenario-space baseline.
3. As a verifier developer, I want segment compression to preserve conflict-relevant step ordering cases, so that the generator does not collapse all multi-saga schedules into serial order.
4. As a verifier developer, I want non-conflicting/internal step permutations to collapse deterministically, so that schedule counts stop being dominated by irrelevant step movement.
5. As a verifier developer, I want materialized schedules and accounting counts to use the same compression semantics, so that small full-write tests and large count-only runs remain consistent.
6. As a verifier developer, I want interaction pruning to remain separate from schedule compression, so that changing the schedule strategy does not change which saga sets are selected.
7. As a verifier developer, I want stable schedule ordering and deterministic IDs, so that repeated verifier runs produce reproducible catalogs and reports.
8. As a verifier developer, I want exact dummyapp-first tests for small examples, so that compression behavior is proven before relying on Quizzes-scale smoke checks.
9. As a verifier developer, I want existing `SERIAL` and `ORDER_PRESERVING_INTERLEAVING` behavior preserved, so that current baselines remain comparable.
10. As an advisor reviewing the thesis work, I want documentation to distinguish real segment compression from interaction pruning, so that the evaluation language does not conflate schedule reduction with saga-set selection.
11. As an advisor reviewing the thesis work, I want the placeholder warning removed only after implementation and verification, so that current-state docs remain honest.

## Domain References

- `docs/verifiers-impl/current-state.md` documents the current verifier scope, scenario catalog export, scenario-space accounting, and the present limitation that `SEGMENT_COMPRESSED` is only a placeholder.
- `docs/verifiers-impl/glossary.md` defines `Scenario`, `ScenarioPlan`, `Input-bound brute-force universe`, `Compressed accounting`, `Generation strategy`, `Schedule strategy`, and related scenario-space terms.
- `docs/verifiers-impl/roadmap.md` places dependency-aware schedules and compression in static scenario synthesis, before executor, impact scoring, GA search, and scenario prioritization.
- `docs/verifiers-impl/thesis-claims-evidence-map.md` provides claim-safety guidance: do not claim execution/search stages as implemented, and verify current implementation status before citing a result.
- `docs/verifiers-impl/decisions/2026-04-05-saga-only-verifier-scope.md` keeps verifier work saga-only unless scope changes explicitly.
- `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md` establishes JSONL catalog and manifest artifacts as the scenario handoff contract.
- `docs/verifiers-impl/decisions/2026-04-28-hybrid-static-dynamic-key-binding.md` explains why exact aggregate-key evidence is incomplete and why type-only fallback must remain explicit and conservative.
- `issues/scenario-space-accounting/prd.md` introduced scenario-space accounting and explicitly deferred true thesis-style segment compression in AC-14 and out-of-scope notes.
- `applications/dummyapp/` remains the canonical verifier fixture corpus for exact schedule/accounting behavior.
- `applications/quizzes/` remains the realistic thesis-scale target for count-only accounting smoke validation.

## Feature Acceptance Criteria

- AC-1: When `scheduleStrategy=SEGMENT_COMPRESSED`, the verifier no longer uses the current total-step cutoff or serial fallback placeholder behavior.
- AC-2: `SEGMENT_COMPRESSED` identifies conflict-anchor steps from configured cross-saga conflict candidates within the current saga set. It uses strict conflict evidence when `allowTypeOnlyFallback=false` and broad conflict evidence when `allowTypeOnlyFallback=true`, regardless of whether the selected saga set came from `INTERACTION_PRUNED` or `BRUTE_FORCE`. Read/read pairs remain non-conflicts under existing conflict semantics.
- AC-3: `SEGMENT_COMPRESSED` preserves interaction-pruning boundaries. It must not change which saga sets are selected by `INTERACTION_PRUNED` or `BRUTE_FORCE`; it only changes schedule enumeration and schedule counts inside already-selected saga sets.
- AC-4: For a saga set with no conflict anchors, `SEGMENT_COMPRESSED` emits and counts exactly one canonical schedule per compatible input tuple.
- AC-5: For a saga set where every step is a conflict anchor, `SEGMENT_COMPRESSED` emits and counts the same number of schedules as `ORDER_PRESERVING_INTERLEAVING`, subject to the same configured schedule cap.
- AC-6: For mixed anchor/internal-step saga sets, `SEGMENT_COMPRESSED` enumerates order-preserving interleavings over per-saga conflict anchors and expands each anchor token into a deterministic full segment while preserving intra-saga step order.
- AC-7: Non-anchor steps before a conflict anchor are emitted with that anchor's segment. Non-anchor tail steps after the final anchor are emitted once in canonical deterministic order after all anchor segments. A saga with zero anchors in a mixed anchored/no-anchor saga set contributes its full in-saga step list to that canonical tail.
- AC-8: The segment-compressed schedule count per input tuple is the multinomial count over per-saga conflict-anchor counts, with zero anchors producing count `1`, and with `maxSchedulesPerInputTuple` applied consistently.
- AC-9: Materialized `ScenarioPlan.expandedSchedule` rows produced by `SEGMENT_COMPRESSED` match the accounting schedule count for small fully materialized fixtures when no global `maxScenarios` cap truncates the catalog.
- AC-10: `maxSchedulesPerInputTuple=0` still disables schedule emission/counting for `SEGMENT_COMPRESSED` in the same bounded manner as existing schedule strategies.
- AC-11: If a compressed schedule naturally has more schedules than `maxSchedulesPerInputTuple`, materialized generation emits only the capped number, accounting uses the capped count, and existing generation warnings/counters report the schedule cap deterministically. The accounting grouped-row count must reflect the capped value; no new accounting warning field is required unless implementation changes the artifact shape deliberately.
- AC-12: Repeated `SEGMENT_COMPRESSED` runs with the same inputs and configuration produce stable schedule order, stable scheduled-step IDs, stable scenario IDs, and stable accounting row ordering.
- AC-13: Existing `SERIAL` and `ORDER_PRESERVING_INTERLEAVING` behavior and tests continue to pass unchanged except where tests asserted the old placeholder `SEGMENT_COMPRESSED` behavior.
- AC-14: Scenario-space accounting grouped saga-set rows report reduced `scheduleCountPerTuple` values for segment-compressed examples where internal/non-conflicting steps exist.
- AC-15: The accounting run config or equivalent behavior description no longer says `SEGMENT_COMPRESSED` is not thesis-style compression after the feature is implemented. It describes conflict-anchor segment compression instead.
- AC-16: Dummyapp-first or synthetic Spock coverage proves exact small examples: no anchors, one anchor per saga with internal steps, all steps as anchors, mixed anchor/internal steps, and schedule caps.
- AC-17: Dummyapp integration coverage proves that materialized `ScenarioPlan` counts and scenario-space accounting totals agree for a small `SEGMENT_COMPRESSED` full-write configuration.
- AC-18: Quizzes count-only accounting smoke runs with dynamic enrichment disabled, `maxSagaSetSize=3`, and comparable configuration for `ORDER_PRESERVING_INTERLEAVING` and `SEGMENT_COMPRESSED`. The smoke compares matching `sagaSetKey` rows across both accounting artifacts and verifies that `SEGMENT_COMPRESSED` has a lower `scheduleCountPerTuple` for at least one selected size-3 row with internal/non-anchor steps. It also verifies selected totals are reduced overall, or any unchanged matching rows are explicitly explained by all-anchor/no-anchor structure.
- AC-19: Quizzes smoke still verifies empty `scenario-catalog.jsonl` in `COUNT_ONLY` mode, manifest existence, rejected-input artifact existence, accounting artifact existence, non-empty grouped rows, selected-space totals, and `catalogWritten=0`.
- AC-20: Documentation updates remove the placeholder/deferred warning for `SEGMENT_COMPRESSED`, supersede the deferred note from the scenario-space-accounting PRD, and add/update glossary/current-state language for `conflict anchor`, `anchor segment`, and segment-compressed scheduling. The docs describe the implemented limitation honestly: compression preserves conflict-anchor order cases under static conflict evidence; it does not prove semantic completeness, exact aggregate-instance binding, or runtime impact.
- AC-21: A verifier decision record is added or updated to explain why conflict-anchor segment compression was chosen over full step-level interleaving, serial fallback, and pairwise-orientation-only compression.

## Implementation Decisions

- Keep `SEGMENT_COMPRESSED` as a schedule strategy enum value and replace its effective behavior rather than introducing a new public strategy name.
- Implement conflict-anchor segment compression as the canonical semantics for `SEGMENT_COMPRESSED`.
- Treat conflict anchors as schedule-relevant only when conflict evidence applies within the current saga set under the run's configured strict/broad conflict lens: strict when `allowTypeOnlyFallback=false`, broad when `allowTypeOnlyFallback=true`.
- Keep interaction selection and schedule compression as separate pipeline concepts. Schedule compression may consume conflict evidence, but it must not choose saga sets.
- Use a shared scheduling/counting module or equivalent shared model so materialized generation and accounting cannot drift.
- Preserve existing deterministic ordering: saga FQNs, saga instance IDs, step order indexes, deterministic IDs, and stable tie-breakers remain the basis of emitted order.
- Keep single-saga schedules canonical. Segment compression is meaningful for multi-saga schedule reduction; single-saga schedules remain one in-saga ordering.
- Apply schedule caps after computing the natural segment-compressed order cases, just as order-preserving interleaving is capped today.
- Use arbitrary-precision counts for accounting, then serialize counts as decimal strings as existing accounting already does.
- Keep scenario-space accounting schema shape stable unless implementation discovers a required new field. The semantic difference should be visible through `scheduleStrategy=SEGMENT_COMPRESSED` and the updated effective behavior description.
- Update current-state and glossary/roadmap language only after tests prove the placeholder behavior has been replaced.
- Add or update a decision record because the selected compression model affects thesis claims, accounting baselines, and future schedule semantics.

## Testing Decisions

- Follow a TDD vertical-slice workflow. Do not write all expected tests first. Add one failing behavior test, implement the smallest production change that passes, then continue with the next behavior.
- Prefer behavior tests through public verifier interfaces rather than tests coupled to private helpers. Good public surfaces include schedule enumeration, scenario generation, scenario-space accounting, and application-level dummyapp/Quizzes runs.
- Keep tests named in domain language: segment-compressed schedules, conflict anchors, non-anchor/internal steps, scenario-space accounting, and selected generator space.
- Avoid tests that assert an internal object model such as a particular private `Segment` class. A refactor should not break tests if observable schedules and counts remain correct.
- Start with a tracer bullet: two sagas where each has one internal step and one conflict anchor. `ORDER_PRESERVING_INTERLEAVING` has 6 step-level schedules for 2+2 steps; `SEGMENT_COMPRESSED` should produce 2 schedules preserving only the two possible conflict-anchor orders.
- Add a no-anchor example: two sagas with multiple steps and no cross-saga conflict evidence. `ORDER_PRESERVING_INTERLEAVING` may have many schedules, while `SEGMENT_COMPRESSED` must produce/count exactly 1 canonical schedule.
- Add an all-anchor example: if all steps are conflict anchors, `SEGMENT_COMPRESSED` must match `ORDER_PRESERVING_INTERLEAVING` for both materialized schedules and accounting counts.
- Add a mixed-anchor example: a saga with non-anchor steps between anchors should emit those non-anchor steps with the following anchor segment, preserving intra-saga order while avoiding independent interleaving of non-anchor steps.
- Add a mixed anchored/no-anchor saga-set example: one saga has anchors, another saga has no anchors. The zero-anchor saga should contribute its full in-saga step list to the canonical tail, and the compressed count should be based only on the anchored saga counts.
- Add accounting/materialization agreement examples on small fixtures where all selected plans can be written without hitting the global catalog cap.
- Add schedule-cap examples where a natural compressed count exceeds `maxSchedulesPerInputTuple`; both materialized schedules and accounting counts should reflect the cap and deterministic warning behavior.
- Add dummyapp integration before Quizzes. Dummyapp should prove the verifier pipeline can adapt real fixture sagas into compressed schedules and accounting totals without relying only on synthetic objects.
- Add Quizzes count-only smoke last. It should compare comparable `ORDER_PRESERVING_INTERLEAVING` and `SEGMENT_COMPRESSED` accounting runs and verify reduced schedule counts without materializing the full catalog.
- Keep dynamic enrichment disabled in Quizzes scheduling smokes. Runtime evidence is unrelated to this schedule-strategy PRD.
- Suggested TDD order: first exact one-anchor-per-saga behavior; no-anchor behavior; all-anchor parity; mixed internal-anchor behavior; accounting/materialization agreement; schedule cap behavior; dummyapp integration; Quizzes count-only comparison.

Example behavior sketches for test planning:

```text
Two sagas, one internal step and one conflict anchor each:

Saga A: aInternal, aConflict
Saga B: bInternal, bConflict

ORDER_PRESERVING_INTERLEAVING count = 6
SEGMENT_COMPRESSED count = 2

Representative compressed schedules:
1. aInternal, aConflict, bInternal, bConflict
2. bInternal, bConflict, aInternal, aConflict
```

```text
No conflict anchors:

Saga A: 2 steps
Saga B: 3 steps

ORDER_PRESERVING_INTERLEAVING count = 10
SEGMENT_COMPRESSED count = 1
```

```text
All steps are conflict anchors:

Saga A: 2 conflict steps
Saga B: 1 conflict step

ORDER_PRESERVING_INTERLEAVING count = 3
SEGMENT_COMPRESSED count = 3
```

## Out of Scope

- ScenarioExecutor changes.
- Runtime input materialization changes.
- Fixture/database setup generation.
- Runtime fault injection.
- Domain-impact scoring.
- Genetic Algorithm local search.
- Bandit or reinforcement-learning scenario prioritization.
- Dynamic enrichment or runtime evidence changes.
- Changing source-mode acceptance rules.
- Improving aggregate-instance key extraction.
- Changing which saga sets are selected by interaction pruning.
- Quizzes source or test modifications.
- Same-saga multi-instance scenario generation.
- HTML rendering of accounting summaries beyond any minimal text/doc updates needed for correctness.

## Further Notes

This PRD replaces the deferred/placeholder portion of scenario-space accounting. It does not change the thesis pipeline order: static scenario synthesis and accounting still precede arbitrary execution, fault injection, impact scoring, GA search, and bandit prioritization.

The key claim after this work should be narrow and honest: `SEGMENT_COMPRESSED` preserves conflict-anchor ordering cases under the verifier's static conflict model while collapsing non-anchor/internal step permutations. It is not proof that all semantically relevant runtime interactions are known, because exact aggregate-instance key extraction and runtime impact analysis remain separate concerns.

No product, domain, or acceptance questions are intentionally left open in this PRD. If implementation reveals that the conflict-anchor model loses a required ordering case, return to planning before widening scope.
