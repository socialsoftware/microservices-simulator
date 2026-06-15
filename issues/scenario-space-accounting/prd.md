## Problem Statement

The verifier can statically extract saga structure, test-derived input variants, and a deterministic scenario catalog, and it has a narrow scenario executor POC. The next thesis stage needs a credible way to measure the scenario space before expanding runtime execution.

For Quizzes, fully materializing a brute-force scenario catalog is infeasible. A static estimate with current accepted inputs shows that size-3 order-preserving interleavings can reach more than one hundred billion scenario shapes. Writing one `ScenarioPlan` per shape would be destructive, but reporting only a single total would lose the useful explanation of where the space explodes.

The user needs a run-local accounting artifact that measures the input-bound brute-force denominator, the configured generator-selected subset, and the actual catalog materialization outcome under one concrete verifier configuration. The artifact must also explain pre-input losses, especially type-level interactions that cannot become executable scenarios because one or more saga classes have no accepted input variants.

## Solution

Add verifier-side scenario-space accounting with exact compressed counting. The accounting groups candidates by saga set and computes input-tuple counts and schedule counts mathematically instead of materializing every scenario plan.

Each verifier run remains a single concrete configuration. A run chooses one target application, input policy, source-mode handling, include-singles behavior, schedule strategy, generation strategy, write mode, maximum saga-set size, input-variant bounds, schedule bounds, and catalog materialization bounds. The accounting report does not speculate about alternative schedule strategies or generator modes in the same `inputBoundScenarioSpace` totals. Strict and broad type-level coverage can appear as diagnostics because they explain extraction and input-coverage quality, not selected output for a different run.

The main artifact is `scenario-space-accounting.json`, written in the verifier run directory alongside the scenario catalog artifacts. The manifest references the accounting artifact. In count-only mode, the verifier writes an empty `scenario-catalog.jsonl`, still writes rejected inputs and the manifest, and writes full accounting counts without materializing selected plans.

The feature also introduces a brute-force generation strategy. `BRUTE_FORCE` selects all input-bound saga sets and schedules under the run configuration. `INTERACTION_PRUNED` selects the current thesis generator subset using static interaction evidence. With `allowTypeOnlyFallback=true`, interaction-pruned generation can include broad type-level interactions, but it remains different from brute force because unrelated aggregate types are still pruned.

Accounting distinguishes scenario-space shaping bounds from catalog materialization bounds. Input-variant and schedule-per-tuple bounds shape the scenario space when configured and therefore affect `allInputBound` and `selectedByGenerator`. The global catalog write cap affects materialization and therefore `catalogWritten`, not the full selected-space count. Count-only mode also affects only materialization.

Dummyapp remains the exact verification target for visitors, fixture shapes, input compatibility, schedule counting, and full catalog writes. Quizzes remains the thesis-scale target for compressed count-only accounting and Docker smoke validation. TeaStore is explicitly not part of this PRD because it has no visible test-derived input fixtures and is not plug-and-play for the current verifier pipeline.

## User Stories

1. As a thesis author, I want to measure the extracted input-bound brute-force scenario universe, so that I can explain why exhaustive scenario materialization is infeasible for Quizzes.
2. As a thesis author, I want the report to separate brute-force denominator, generator-selected space, and written catalog size, so that bounds and count-only mode do not obscure the comparison.
3. As a thesis author, I want type-level interaction coverage before input filtering, so that I can explain which production saga interactions are lost because tests do not provide usable inputs.
4. As a thesis author, I want strict and broad interaction diagnostics, so that I can distinguish high-confidence static interaction evidence from type-only fallback evidence.
5. As a verifier developer, I want compressed saga-set rows, so that Quizzes-scale runs can report billions of represented scenario shapes without writing billions of JSONL lines.
6. As a verifier developer, I want top contributor summaries, so that I can quickly identify which saga sets, input counts, and step interleavings dominate the search space.
7. As a verifier developer, I want a `BRUTE_FORCE` generation strategy, so that small applications can emit a full brute-force catalog using the same `ScenarioPlan` contract.
8. As a verifier developer, I want count-only mode, so that large applications can count selected scenarios without materializing the catalog.
9. As a verifier developer, I want dummyapp fixtures for unrelated, strict-interacting, broad/type-only, missing-input, connected-chain, multi-step, and incompatible-input cases, so that the accounting formulas are tested exactly.
10. As a verifier developer, I want input compatibility counting to respect exact logical-key contradictions, so that brute-force counts do not overcount impossible input tuples when exact key evidence exists.
11. As a verifier developer, I want executor readiness summarized from static input recipes, so that future executor work can see how much of the accepted input space is currently replay-oriented without changing the scenario denominator.
12. As an advisor reviewing the thesis work, I want a baseline investigation note explaining why the legacy behavior CSV generator is not the scenario-space baseline, so that related simulator work is positioned correctly.
13. As an advisor reviewing the thesis work, I want Docker-verified Quizzes accounting artifacts, so that the results reflect the same environment used for observed verifier runs.

## Domain References

- `docs/verifiers-impl/glossary.md` defines `Scenario`, `ScenarioPlan`, `Input-bound brute-force universe`, `Type-level shape space`, `Compressed accounting`, `Count-only catalog mode`, `Fault-free execution`, and related terms.
- `docs/verifiers-impl/current-state.md` is the current implementation source of truth and documents existing static extraction, dynamic enrichment, scenario catalog limits, and the placeholder status of `SEGMENT_COMPRESSED`.
- `docs/verifiers-impl/scenario-executor-poc.md` documents the current narrow scenario executor POC and its limitations.
- `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md` establishes JSONL/manifest scenario catalog artifacts as the handoff contract.
- `docs/verifiers-impl/decisions/2026-04-28-hybrid-static-dynamic-key-binding.md` explains why exact aggregate-key evidence is incomplete and why type-level fallback must be treated carefully.
- `docs/verifiers-impl/test-analysis-saga-input-flow.md` explains how test-derived inputs feed the scenario catalog and why missing inputs limit executable scenario enumeration.
- `applications/dummyapp/` is the canonical verifier fixture corpus for parser, scenario, and enrichment edge cases.
- `applications/quizzes/` is the realistic thesis-scale target for compressed accounting and smoke validation.

## Feature Acceptance Criteria

- AC-1: When scenario catalog export is enabled, the verifier writes `scenario-space-accounting.json` in the same run directory as the catalog artifacts.
- AC-2: The scenario catalog manifest records `catalogWriteMode` and a path or run-relative reference to `scenario-space-accounting.json`.
- AC-3: The accounting report is per-run/per-config and includes the target application, generation strategy, catalog write mode, include-singles setting, maximum saga-set size, maximum input variants per saga, maximum schedules per input tuple, maximum scenarios write cap, schedule strategy, type-only fallback setting, input policy, and source-mode handling used for saga-catalog input acceptance.
- AC-4: The accounting report contains `typeLevelCoverage` over all discovered production saga classes. It includes discovered saga FQNs, saga counts, sagas with accepted inputs, sagas without accepted inputs, and separate `strict` and `broad` sections. Each strict/broad section includes interaction-pair counts, input-covered interaction-pair counts, missing-input interaction-pair counts, and connected-set counts by size.
- AC-5: The accounting report contains `inputBoundScenarioSpace` with `allInputBound`, `selectedByGenerator`, and `catalogWritten` totals by saga-set size and total counts.
- AC-6: Large combinatoric counts in the accounting report are serialized as decimal strings rather than JSON numbers.
- AC-7: The accounting report includes `groupedSagaSets` for all input-bound saga sets up to the configured maximum saga-set size. The default grouped-row safety threshold is at least `100000`; if a run exceeds the configured threshold, the verifier fails the accounting run rather than silently omitting grouped rows.
- AC-8: Each grouped saga-set row has a deterministic saga-set key derived from the sorted saga FQNs, includes full saga FQNs, input counts by saga, step counts by saga, compatible input-tuple count, schedule count per tuple, scenario shape count, and whether the row is selected by the configured generator. Its interaction summary has separate `strict` and `broad` subsections, and each subsection contains `connected`, `directPairCount`, and evidence-kind counts for that subsection's interaction graph.
- AC-9: The accounting report includes a small `topContributors` list ranked by brute-force scenario shape count. Each top-contributor entry references a grouped saga-set key and includes the represented scenario-shape count.
- AC-10: `BRUTE_FORCE` generation selects all input-bound saga sets and schedules under the run configuration. On small dummyapp fixtures in `WRITE_PLANS` mode, selected generator counts and written catalog counts match the full input-bound denominator.
- AC-11: `INTERACTION_PRUNED` generation selects single-saga rows when singles are enabled and selects multi-saga rows only when the configured static interaction graph includes the saga set. With type-only fallback disabled it uses strict interaction evidence; with type-only fallback enabled it may select broad type-level interactions while still pruning unrelated aggregate types.
- AC-12: `COUNT_ONLY` mode writes an empty `scenario-catalog.jsonl`, still writes `scenario-catalog-manifest.json`, still writes `scenario-catalog-rejected-inputs.jsonl`, and writes complete accounting counts for the selected generation strategy. Manifest exported-scenario counts reflect the empty written catalog; selected-space counts live in `scenario-space-accounting.json`.
- AC-13: Input tuple counts respect the existing conservative compatibility semantics: exact logical-key contradictions are rejected, while missing or unknown logical-key evidence is allowed rather than guessed.
- AC-14: Schedule counts match the configured schedule strategy. `SERIAL` counts one schedule per input tuple. `ORDER_PRESERVING_INTERLEAVING` counts order-preserving interleavings mathematically. If `SEGMENT_COMPRESSED` is configured, accounting uses the current implementation's effective behavior, records that effective behavior in the run config, and does not claim thesis-style segment compression.
- AC-15: The accounting report includes a minimal input-level `executorReadiness` summary based only on static `inputRecipe.executorReady` and recipe blockers. It does not run the scenario executor, does not run the materializer, and does not report scenario-level executor admissibility in v1.
- AC-16: Dummyapp tests cover unrelated sagas with inputs, strict interaction, broad/type-only interaction, missing-input interaction, a three-saga connected chain, multi-step order-preserving schedule counts, and compatible/incompatible input tuples.
- AC-17: Quizzes Docker smoke validation runs two count-only accounting configurations with dynamic enrichment disabled: strict interaction-pruned (`allowTypeOnlyFallback=false`) and type-fallback interaction-pruned (`allowTypeOnlyFallback=true`). Each smoke uses `maxSagaSetSize=3`, `ORDER_PRESERVING_INTERLEAVING`, and `COUNT_ONLY`; verifies an empty `scenario-catalog.jsonl`; verifies manifest, rejected-input, and accounting artifacts exist; and verifies accounting contains non-empty input-bound grouped rows, `allInputBound` totals, `selectedByGenerator` totals, `catalogWritten=0`, and `typeLevelCoverage`.
- AC-18: A baseline investigation note is produced at `issues/scenario-space-accounting/baseline-investigation-note.md`. It explains what the legacy behavior CSV generator does, why it is not scenario-space enumeration, why type-level shape space is report-only, why input-bound brute force is the executable baseline, how count-only compressed accounting replaces unsafe materialization for Quizzes, and which code/docs support those claims.

## Implementation Decisions

- Add a static v1 scenario-space accounting model and writer with schema `microservices-simulator.scenario-space-accounting.v1`.
- Add a generation strategy configuration with at least `INTERACTION_PRUNED` and `BRUTE_FORCE`.
- Add a catalog write mode configuration with at least `WRITE_PLANS` and `COUNT_ONLY`.
- Keep accounting per-run/per-config. Do not include alternate schedule-strategy or alternate generator-strategy projections in `inputBoundScenarioSpace` for the same run.
- Always compute `allInputBound` as the brute-force denominator for accepted input-bearing saga combinations under the configured include-singles setting, maximum saga-set size, input-variant bounds, schedule strategy, and maximum schedules per input tuple.
- Compute `selectedByGenerator` as the full scenario shape count selected by the configured generation strategy, independently from how many plans are written.
- Compute `catalogWritten` from the actual materialized catalog output. In count-only mode this count is zero, and existing manifest export counts also reflect the empty written catalog rather than selected-space counts.
- Implement compressed accounting by grouping by saga set and using exact mathematical counts for input tuples and schedules rather than constructing every `ScenarioPlan`.
- Reuse the existing static scenario pipeline boundaries where possible: visitor extraction feeds `ApplicationAnalysisState`, adapter output feeds scenario models, and accounting consumes normalized scenario-model data.
- Reuse or mirror existing `ConflictGraphBuilder` and `ConnectedSagaSetEnumerator` semantics for strict/broad interaction and connected saga-set discovery.
- Reuse existing input normalization and source-mode acceptance rules so accounting and catalog output operate on the same accepted input universe.
- Implement compatibility-aware input tuple counting. Use multiplication when no exact logical-key bindings are present. Preserve full binding signatures when grouped counting is needed so multi-key correlations are not lost.
- Use `BigInteger` or equivalent arbitrary precision for combinatoric counts.
- Keep `scenario-space-accounting.json` focused on counts and grouped diagnostics. Do not duplicate artifact paths or timestamps already owned by the manifest/run directory.
- Keep grouped rows input-bound only. Type-level interactions that lack inputs are counted in `typeLevelCoverage`, not emitted as grouped scenario rows.
- Keep missing-input examples out of v1 accounting. Counts are enough for this PRD.
- Keep same-saga multi-instance scenarios out of the v1 comparison universe and document the limitation.
- Do not include TeaStore in this PRD. It may be revisited later if input fixtures and verifier compatibility make it useful.
- Produce a baseline investigation note as a planning/documentation deliverable, separate from the machine-readable accounting artifact.

## Testing Decisions

- Add focused Spock tests for the deep accounting modules rather than only end-to-end application tests.
- Test schedule-count formulas against materialized `ScheduleEnumerator` outputs for small controlled inputs.
- Test connected-set counting against existing connected-chain and disconnected graph fixture shapes.
- Test strict versus broad interaction counts with exact/symbolic and type-only footprint fixtures.
- Test input tuple counting with exact logical-key matches, exact contradictions, missing bindings, and multi-key signatures.
- Test `BRUTE_FORCE + WRITE_PLANS` on dummyapp fixtures where the full catalog is small enough to materialize and compare line counts to accounting counts.
- Test `INTERACTION_PRUNED + WRITE_PLANS` on dummyapp fixtures where selected and pruned sets are known.
- Test `COUNT_ONLY` on dummyapp to prove the catalog is empty while accounting totals remain complete.
- Test manifest integration for `catalogWriteMode` and the accounting artifact reference.
- Test executor readiness summaries from static input recipes and blockers without launching the executor.
- Run Quizzes Docker smoke tests for strict and type-fallback count-only accounting to verify artifact production and practical Quizzes-scale compressed counting.
- Do not require full Quizzes brute-force catalog materialization as verification.

## Out of Scope

- Multi-saga scenario executor support.
- Runtime input materialization improvements.
- Fixture/database setup generation.
- Runtime fault injection or non-zero fault-vector execution.
- Domain-impact scoring.
- Genetic Algorithm local search.
- Bandit or reinforcement-learning scenario prioritization.
- Dynamic enrichment changes beyond existing input and key evidence behavior.
- HTML rendering of accounting summaries.
- TeaStore evaluation or fixture work.
- Quizzes source or test modifications.
- Same-saga multi-instance scenario generation.
- True thesis-style segment-compressed schedule generation.

## Further Notes

The central thesis claim supported by this PRD is not that the generator finds all real semantic interactions. The claim is that the verifier can exactly measure the extracted input-bound brute-force scenario space under a declared configuration, show why exhaustive materialization is infeasible at Quizzes scale, and quantify how much the configured static generator prunes before runtime execution.

Strict and broad interaction are static approximations. If the generator output matches one of these lenses, that proves consistency with the implemented static interaction model, not semantic completeness of the application. The accounting should make missing-input losses and type-only fallback dependence visible so the thesis can discuss current limits honestly.

Fault slots remain part of `ScenarioPlan.faultSpace`, but this PRD treats all executions as fault-free/default for accounting purposes and does not multiply scenario counts by fault-vector combinations.
