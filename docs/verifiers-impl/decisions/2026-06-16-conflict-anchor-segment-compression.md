# Conflict-anchor segment compression

Date: 2026-06-16

Status: accepted

## Context

The verifier exposed `SEGMENT_COMPRESSED` before real segment compression existed. The old behavior was only a placeholder: small tuples behaved like bounded order-preserving interleaving and larger tuples fell back to serial scheduling. That made the strategy unsafe to cite as a thesis-style schedule-space reduction baseline.

Quizzes count-only accounting showed why a real reduction is needed. Size-3 selected saga sets with multi-step sagas can be dominated by step-level interleavings that move non-conflicting/internal steps around without changing the conflict-relevant order cases. Serial fallback avoids the explosion but collapses conflict-relevant order cases too aggressively.

## Decision

`SEGMENT_COMPRESSED` now means conflict-anchor segment compression.

For a selected saga set and compatible input tuple, the verifier identifies conflict anchors from configured cross-saga conflict candidates within that selected saga set. The same configured static conflict lens is used as interaction evidence: strict evidence when `allowTypeOnlyFallback=false`, and broad/type-only fallback evidence when `allowTypeOnlyFallback=true`. Read/read pairs are still non-conflicts.

Each saga's ordered steps are compressed into anchor segments. An anchor segment contains the deterministic in-saga run of non-anchor steps since the previous anchor plus the anchor step itself. Schedule enumeration interleaves anchor segments while preserving each saga's anchor order. Non-anchor tail steps after a saga's final anchor, including all steps of zero-anchor sagas in a mixed anchored/no-anchor set, are appended once in canonical deterministic saga order after all anchor segments.

Scenario-space accounting uses the same count semantics: multinomial count over per-saga conflict-anchor counts, one canonical schedule for zero-anchor rows, and the same `maxSchedulesPerInputTuple` cap and zero-cap behavior as materialized generation.

Interaction pruning remains separate. `INTERACTION_PRUNED` and `BRUTE_FORCE` still decide which saga sets are selected; `SEGMENT_COMPRESSED` only reduces schedule enumeration and schedule counts inside those already-selected sets.

## Rejected Alternatives

- Full step-level order-preserving interleaving: preserves every possible step order but makes Quizzes count-only totals dominated by permutations of non-conflicting/internal steps. It remains available as `ORDER_PRESERVING_INTERLEAVING` for comparison.
- Serial fallback: avoids combinatorial growth but collapses conflict-relevant order cases and was not a credible segment-compressed baseline.
- Pairwise-orientation-only compression: counting only pair orientations between conflicting steps would be smaller, but it would not naturally preserve per-saga anchor order or expand deterministic full schedules for materialized `ScenarioPlan` rows.
- Runtime/dynamic redefinition of schedules: dynamic evidence may enrich static catalogs, but it should not redefine the static scenario structure or schedule strategy.

## Consequences

- `SEGMENT_COMPRESSED` is now claimable as an implemented static schedule-space reduction strategy, with the narrower claim that it preserves conflict-anchor order cases under the verifier's static conflict model while collapsing non-anchor/internal permutations.
- Quizzes count-only comparison evidence shows selected totals reduced from `218528454` under `ORDER_PRESERVING_INTERLEAVING` to `1019393` under `SEGMENT_COMPRESSED` for comparable `INTERACTION_PRUNED`, dynamic-enrichment-disabled, size-3 runs.
- Dummyapp integration proves the behavior through the real parser/adapter pipeline before relying on Quizzes-scale accounting.
- The strategy does not prove semantic completeness, exact aggregate-instance binding, runtime impact, executor feasibility, fault injection behavior, impact scoring, GA search, or bandit prioritization.
- Exact aggregate-instance key extraction remains incomplete. Type-only fallback remains opt-in and must not be described as exact shared-instance evidence.

## Evidence

- `ScenarioGeneratorSpec` covers anchor interleaving, no-anchor canonical schedules, all-anchor parity, mixed anchor/internal expansion, deterministic tails, zero-anchor saga tails, and deterministic IDs.
- `ScenarioSpaceAccountingCalculatorSpec` covers strict/broad lenses, read/read non-conflicts, accounting/materialized parity, caps, zero caps, stable grouped rows, and non-placeholder behavior text.
- `DummyappAccountingFixtureFoundationSpec` covers real dummyapp parser/adapter integration, materialized/accounting parity, conflict evidence, full expanded schedules, and reduced compressed counts against order-preserving interleaving.
- Quizzes Docker count-only artifacts under `verifiers/target/segment-compressed-scheduling-007/` compare `ORDER_PRESERVING_INTERLEAVING` and `SEGMENT_COMPRESSED` with dynamic enrichment disabled, `COUNT_ONLY`, `INTERACTION_PRUNED`, and `maxSagaSetSize=3`.
