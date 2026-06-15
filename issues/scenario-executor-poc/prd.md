## Problem Statement

The verifier can now export deterministic scenario plans with structured, executor-facing input recipes. The catalog can describe saga instances, input variants, input recipes, and the expanded schedule of steps that should be driven at runtime. Dynamic enrichment can also produce sidecar records that preserve the embedded static `ScenarioPlan` while adding join-status evidence.

What is missing is the first execution bridge. There is no generic scenario executor that can take a verifier-generated JSONL record, reconstruct enough runtime inputs, instantiate the target saga/functionality, and call `executeUntilStep(...)` in catalog order. Without that bridge, the pipeline remains a scenario generator and enrichment tool, not an executable scenario workflow.

The immediate user need is a narrow professor-demo milestone: prove that at least one real Quizzes catalog scenario can cross the full path from verifier-generated JSONL to runtime saga stepping, without modifying Quizzes source or tests and without claiming arbitrary catalog replay.

## Solution

Build a first scenario executor POC owned by the `verifiers` module. The executor will read `scenario-catalog-enriched.jsonl` by default when available, fall back to `scenario-catalog.jsonl` when the enriched catalog is unavailable, unwrap enriched records to their embedded `ScenarioPlan`, select or accept a specific scenario plan id through `--scenario-id`, materialize supported constructor arguments, instantiate one target saga reflectively on the target application classpath, and execute its scheduled steps in deterministic order.

The executor will run target application code in a verifier-orchestrated subprocess/JVM whose classpath includes the target application and the verifier executor classes. This preserves the design constraint that Quizzes does not need committed source or test changes for the POC, while still letting the executor instantiate Quizzes saga classes and use the target Spring context.

The first POC supports only single-saga serial schedules. It will call `executeUntilStep(stepName, unitOfWork)` once per scheduled step sorted by `scheduleOrder`, and it will not call `resumeWorkflow` in the MVP. The output will be a small execution report plus console trace showing which scenario was selected, which steps were attempted, and whether each step completed or failed.

The executor will not require strict `inputRecipe.executorReady=true` for the whole recipe when the only blockers are executor-owned runtime arguments. It may supply `SagaUnitOfWorkService`, `CommandGateway`, and `SagaUnitOfWork` from the runtime environment. All other unresolved or source-owned values remain blockers.

The first realistic smoke target is one materializable Quizzes single-saga scenario, preferably a `GetCourseExecutionsFunctionalitySagas` candidate if available, because the current recipe-aware Quizzes smoke catalog has plausible candidates under the runtime-owned argument rule and that saga has a simple single-step schedule.

## Domain References

- `docs/verifiers-impl/glossary.md`: scenario catalog, `ScenarioPlan`, input variant, dynamic enrichment, sidecar artifact, join status, and scenario execution terminology.
- `docs/verifiers-impl/current-state.md`: current verifier implementation status and explicit absence of `ScenarioExecutor` / runtime materialization.
- `docs/verifiers-impl/structured-input-recipes.md`: embedded input recipe schema, readiness semantics, blockers, and executor/materializer boundaries.
- `docs/verifiers-impl/roadmap.md`: scenario execution as the next stage after static scenario synthesis and dynamic evidence bridge.
- `docs/verifiers-impl/meeting-notes/2026-W21-executor-readiness-meeting.md`: executor-readiness framing and proposed constrained executor MVP.
- `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md`: JSONL/manifest scenario catalog as the structured executor contract.
- `docs/verifiers-impl/decisions/2026-04-28-hybrid-static-dynamic-key-binding.md`: dynamic evidence as optional sidecar enrichment that does not replace static catalog facts.
- `docs/verifiers-impl/decisions/2026-05-12-runtime-input-variant-attribution.md`: conservative dynamic-enrichment join-status terminology and exactness rules.

## Feature Acceptance Criteria

- [x] AC-001: The executor can load an enriched scenario catalog when it exists and unwrap each record to the embedded `ScenarioPlan` without redefining the static scenario structure.
- [x] AC-002: The executor can fall back to the static scenario catalog when the enriched catalog is unavailable.
- [x] AC-003: The executor accepts an explicit scenario plan id through `--scenario-id` and executes only that scenario when it exists and is materializable.
- [x] AC-004: The executor fails with a clear scenario-selection error when an explicit scenario plan id is absent from the loaded JSONL.
- [x] AC-005: The executor fails with a clear materialization or support error when an explicit scenario plan id exists but is unsupported by the POC.
- [x] AC-006: Without an explicit scenario plan id, the executor deterministically selects the first materializable single-saga candidate by enriched join-status priority and original JSONL line order.
- [x] AC-007: Enriched join metadata is used only for deterministic candidate prioritization; execution still uses the embedded `ScenarioPlan` as the contract.
- [x] AC-008: The executor supports only single-saga scenario plans with exactly one saga instance and one matching input variant.
- [x] AC-009: Multi-saga plans and cross-saga schedules are skipped in auto-select mode and rejected in explicit scenario mode with structured reasons.
- [x] AC-010: The executor can identify materializable constructor arguments when they are recipe-ready, injectable Spring bean placeholders, or whitelisted executor-owned runtime arguments.
- [x] AC-011: The executor supplies `SagaUnitOfWorkService` from the target Spring context when required by the selected saga constructor.
- [x] AC-012: The executor supplies `CommandGateway` from the target Spring context when required by the selected saga constructor.
- [x] AC-013: The executor creates and supplies a `SagaUnitOfWork` for the selected saga instance when required by the selected saga constructor.
- [x] AC-014: The executor rejects unresolved values that are not explicitly whitelisted executor-owned runtime arguments.
- [x] AC-015: The executor supports `literal`, `constructor`, simple assignment, `collection`, supported `local_transform` for `toSet`, `helper_result`, materialized-receiver `property_access`, injectable `placeholder`, and whitelisted runtime-owned `SagaUnitOfWork` materialization.
- [x] AC-016: The executor extracts runtime step names by taking the text after the final `::` in a catalog step id and removing one trailing `#<digits>` suffix, then calls `executeUntilStep` in ascending `scheduleOrder`.
- [x] AC-017: The executor does not call `resumeWorkflow` in the MVP.
- [x] AC-018: The executor stops on the first step failure for the selected scenario and records the failed step plus exception class/message.
- [x] AC-019: The executor writes a deterministic execution report with schema version, scenario plan id, selection mode/reason, saga/input identifiers, step outcomes, skipped-candidate counts, and terminal status.
- [x] AC-020: The executor prints a concise console trace showing the selected scenario and each attempted scheduled step.
- [x] AC-021: The first Quizzes smoke demonstrates at least one real catalog scenario selected, materialized, instantiated, and stepped through without committed Quizzes source or test changes.
- [x] AC-022: For each rejected explicit scenario or skipped auto-select candidate category, the executor reports a structured support/materialization blocker and never synthesizes values for non-whitelisted unresolved arguments.
- [x] AC-023: Candidate selection and skipped-reason counts are stable across repeated runs over the same catalog and configuration.
- [x] AC-024: The executor consumes existing static/enriched catalog schemas and writes only executor-specific output artifacts; it does not write or modify scenario catalog, enriched catalog, dynamic evidence, or dynamic join-report artifacts.
- [x] AC-025: Runtime inputs are explicit and validated: application base directory, Spring application class, Maven profile, Spring profiles, catalog path or run directory, optional scenario plan id, and output path.
- [x] AC-026: The execution report distinguishes preparation/startup, selection, materialization, unsupported-scenario, and step-execution terminal statuses.

## User Stories

1. As the verifier author, I want to run one generated scenario from JSONL, so that I can demonstrate the scenario generator is moving toward executable runtime behavior.
2. As the verifier author, I want the executor to prefer the enriched catalog when it exists, so that dynamic-enrichment results can help choose useful scenarios.
3. As the verifier author, I want the executor to fall back to the static catalog, so that execution is still possible when no dynamic-enrichment sidecar has been generated.
4. As the verifier author, I want enriched records unwrapped to their embedded scenario plans, so that the static `ScenarioPlan` remains the executable contract.
5. As the verifier author, I want to pass a scenario plan id explicitly through `--scenario-id`, so that I can rerun a specific catalog scenario for debugging or a professor demo.
6. As the verifier author, I want an explicit missing scenario plan id to fail clearly, so that I do not accidentally demo or debug the wrong scenario.
7. As the verifier author, I want unsupported explicit scenarios to fail with structured reasons, so that I can see whether the blocker is scenario kind, recipe materialization, classpath, Spring context, or runtime execution.
8. As the verifier author, I want auto-selection to find the first runnable candidate, so that a demo can work even when many generated scenarios remain unsupported.
9. As the verifier author, I want auto-selection to be deterministic, so that repeated runs over the same artifact pick the same scenario.
10. As the verifier author, I want enriched join status to be a prioritization hint, so that exact or high-confidence dynamically covered candidates are preferred when possible.
11. As the verifier author, I want static scenario ordering preserved as a deterministic tie-breaker, so that prioritization does not introduce hidden randomness.
12. As the verifier author, I want skipped candidates counted by reason, so that the report explains how far the POC is from broader replay coverage.
13. As the verifier author, I want single-saga scenarios supported first, so that the first executor avoids cross-saga scheduling complexity.
14. As the verifier author, I want multi-saga scenarios rejected honestly, so that the POC does not overclaim support for interacting sagas.
15. As the verifier author, I want one saga instance matched to one input variant, so that constructor materialization has a clear target.
16. As the verifier author, I want constructor arguments materialized from input recipes, so that the executor consumes the structured recipe contract instead of parsing prose summaries.
17. As the verifier author, I want literal recipe values supported, so that simple scalar arguments can be reconstructed mechanically.
18. As the verifier author, I want constructor recipe nodes supported, so that DTO arguments can be created reflectively when the target type is known.
19. As the verifier author, I want simple assignment recipes supported, so that DTOs mutated through exported setter/property assignments can be reconstructed.
20. As the verifier author, I want collection recipes supported, so that list, set, and map constructor arguments can be reconstructed.
21. As the verifier author, I want `toSet`-style local transforms supported where the receiver is materializable, so that common Groovy collection setup can run.
22. As the verifier author, I want helper-result nodes reduced through their nested result recipes, so that the executor does not need to invoke Spock helper methods.
23. As the verifier author, I want property access supported only when the receiver is materialized, so that property reads are safe and explainable.
24. As the verifier author, I want injectable placeholders resolved through Spring, so that application services can be supplied by the target runtime.
25. As the verifier author, I want `SagaUnitOfWorkService` supplied by Spring, so that saga constructors receive the same runtime dependency they expect in tests/application code.
26. As the verifier author, I want `CommandGateway` supplied by Spring, so that saga steps can dispatch commands through the target app runtime.
27. As the verifier author, I want `SagaUnitOfWork` created by the executor, so that the current recipe blocker for `createUnitOfWork(...)` does not prevent every Quizzes candidate from running.
28. As the verifier author, I want other unresolved variables rejected, so that the executor does not guess source-owned values or fixtures.
29. As the verifier author, I want target app runtime settings passed explicitly, so that the POC does not rely on fragile auto-detection.
30. As the verifier author, I want the executor to compile or prepare the target app before launching the execution JVM, so that target classes and dependencies are available.
31. As the verifier author, I want Quizzes source and tests left unchanged, so that the POC is verifier-owned and application-agnostic in ownership.
32. As the verifier author, I want the executor subprocess to run with the target Spring profiles, so that beans match the local/sagas test runtime used by the catalog and enrichment workflows.
33. As the verifier author, I want reflection-based saga instantiation, so that the executor does not compile Quizzes-specific imports into `verifiers`.
34. As the verifier author, I want classpath/Spring startup failures reported separately from scenario materialization failures, so that infrastructure failures are debuggable.
35. As the verifier author, I want step ids converted into runtime step names, so that catalog schedule entries can drive `executeUntilStep`.
36. As the verifier author, I want each scheduled step executed in catalog order, so that the runtime trace corresponds to the verifier-generated schedule.
37. As the verifier author, I want no `resumeWorkflow` call in the MVP, so that the first trace is exactly the scheduled step calls.
38. As the verifier author, I want step failure to stop execution, so that the report reflects the first runtime blocker without corrupting later state.
39. As the verifier author, I want expected failure scenarios reported as failed executions rather than executor crashes, so that later work can intentionally handle negative scenarios.
40. As the verifier author, I want a machine-readable execution report, so that future tooling can consume executor outcomes.
41. As the verifier author, I want a console trace, so that the professor demo visibly shows step progression.
42. As the verifier author, I want the report to include selected scenario plan id and input ids, so that execution can be traced back to the catalog.
43. As the verifier author, I want the report to include selection mode, so that I can distinguish explicit scenario runs from auto-selected demo runs.
44. As the verifier author, I want skipped-candidate diagnostics in the report, so that unsupported coverage is visible without reading logs.
45. As the verifier author, I want dummyapp-first automated tests, so that selection and materialization behavior is proved on controlled fixtures before a Quizzes smoke.
46. As the verifier author, I want Quizzes smoke validation, so that the POC proves a real application scenario can run.
47. As the verifier author, I want tests to exercise public executor interfaces and reports, so that implementation internals can be refactored.
48. As the verifier author, I want vertical tracer-bullet development, so that each slice proves one executable behavior before adding more materializer coverage.
49. As a future executor maintainer, I want unsupported recipe shapes to remain explicit blockers, so that future work can prioritize coverage expansion from real diagnostics.
50. As a future executor maintainer, I want fixture/database setup excluded from this POC, so that the first executor remains focused on the catalog-to-step path.
51. As a future executor maintainer, I want fault injection excluded, so that execution baseline behavior is established before failures are introduced.
52. As a future executor maintainer, I want behavior CSV generation excluded, so that JSONL remains the current primary execution input contract.
53. As a future executor maintainer, I want impact scoring excluded, so that runtime stepping and outcome reporting are not conflated with scoring design.
54. As a future executor maintainer, I want GA search and bandit prioritization excluded, so that search work waits until scenario execution is repeatable.
55. As a future thesis evaluator, I want the first executor to state its limitations, so that a one-scenario demo is not misrepresented as arbitrary Quizzes replay.

## Implementation Decisions

- The executor is verifier-owned and orchestrated from `verifiers`.
- The target application should not receive committed source or test changes for this POC.
- The target application is prepared and executed through an external process/JVM rather than becoming a compile-time dependency of `verifiers`.
- Runtime settings are explicit inputs: application base directory, Maven profile, Spring application class, Spring profiles, catalog path or run directory, optional scenario plan id, and output path.
- The executor prefers the enriched catalog when available and falls back to the static catalog when the enriched catalog is unavailable.
- Enriched records are treated as wrappers around a static `ScenarioPlan`. The executor may inspect enrichment metadata for selection, but it executes from the embedded plan.
- Auto-selection considers only materializable single-saga candidates.
- Candidate ordering is deterministic. Enriched join-status priority is explicit, and the original JSONL line number is the tie-breaker.
- A recommended enriched priority order is exact match, high-confidence match, partial match, ambiguous, unmatched, not covered, and missing join metadata. This priority is only a selection heuristic, not a claim that execution will succeed.
- Explicit scenario plan id mode bypasses auto-selection but not validation or materialization checks.
- Supported scenario shape is restricted to one single-saga plan, one saga instance, and one matching input variant.
- Step execution order comes from `expandedSchedule` sorted by `scheduleOrder`.
- Runtime step names are derived by taking the text after the final `::` in the scheduled step id and removing one trailing `#<digits>` suffix. Step ids without a valid nonblank runtime step name are unsupported.
- The executor calls `executeUntilStep` for each scheduled step and does not call `resumeWorkflow` in the MVP.
- The materializer treats recipe readiness as necessary for source-owned values but allows a narrow runtime-owned override for known runtime dependencies.
- Runtime-owned constructor arguments are limited to `SagaUnitOfWorkService`, `CommandGateway`, and `SagaUnitOfWork` in the POC.
- `SagaUnitOfWorkService` and `CommandGateway` are resolved from the target Spring context.
- `SagaUnitOfWork` is created by the executor for the selected saga instance.
- Other unresolved variables, unknown values, missing target types, unsupported transforms, and source-provided placeholders without values remain blockers.
- Reflection is used to instantiate target saga/functionality classes from the target application classpath.
- Constructor matching should be based on expected argument types and runtime values, not Quizzes-specific imports.
- Execution reports are dedicated executor artifacts. They are not dynamic-enrichment evidence, and this PRD does not feed executor outcomes back into dynamic-enrichment sidecars.
- Console output is human-readable demo feedback; the execution report is the machine-readable contract for this POC.
- Infrastructure/configuration failures, selection failures, materialization failures, and runtime step failures should be distinguishable statuses.
- Auto-select mode skips unsupported candidates and continues searching. Explicit scenario mode fails for the selected unsupported candidate.
- The Quizzes smoke goal is at least one completed real scenario, not broad coverage or all useful combinations.

## Testing Decisions

- Tests should verify externally observable behavior through public executor interfaces, CLI/orchestration boundaries, catalog artifacts, and execution reports rather than private helper methods.
- Development should follow vertical tracer bullets: one behavior test, minimal implementation, then the next behavior.
- The first tracer bullet should load a small catalog fixture, select one materializable single-saga plan, and produce a report showing the selected scenario and scheduled step names without requiring Quizzes.
- The next tracer bullet should read an enriched catalog wrapper, unwrap the embedded scenario plan, and prove enriched metadata affects candidate priority without changing the executable plan.
- Selection tests should cover explicit scenario plan id success, missing explicit id failure, unsupported explicit id failure, auto-select skipping unsupported candidates, enriched join-status priority, original JSONL line-order tie-breaking, and deterministic first-runnable selection.
- Materialization tests should cover recipe-ready literals, constructors, assignments, collections, `toSet`, helper results, property access over materialized receivers, injectable placeholders, and runtime-owned `SagaUnitOfWork` override behavior.
- Materialization failure tests should prove non-whitelisted unresolved values, missing target types, unsupported call results, source-provided placeholders without values, and unsupported transforms produce structured blockers.
- Step execution tests should verify that scheduled steps are ordered by `scheduleOrder`, runtime step names are derived from catalog step ids using the final-`::` plus trailing-`#<digits>` removal rule, invalid step ids are rejected, `executeUntilStep` is called for each step, and `resumeWorkflow` is not part of MVP behavior.
- Report tests should verify terminal status, selection mode, scenario identifiers, step outcomes, failure details, and skipped-candidate reason counts.
- Orchestration tests should use a fake process runner where possible to verify explicit runtime-input validation, target app preparation, command construction, output-path handling, distinct terminal status reporting, and failure reporting without launching Quizzes for every test.
- Dummyapp or synthetic fixtures should be the primary automated coverage for verifier assumptions and materializer behavior.
- Quizzes validation should be a focused smoke after dummyapp/synthetic coverage passes. It should prove at least one real catalog scenario completes in the target runtime with no Quizzes source/test changes.
- Quizzes smoke acceptance should not depend on fragile exact catalog counts, but the smoke should record the selected scenario plan id, saga FQN, step trace, terminal status, and catalog artifact used.
- Tests should not assert dynamic-enrichment join count changes because this PRD does not modify dynamic-enrichment semantics.
- Tests should not require fixture/database generation or replay of original Spock setup methods.
- Good tests should be robust to internal refactors of reader, selector, materializer, or orchestration helpers as long as the public behavior and report contract remain stable.

## Out of Scope

- Modifying Quizzes source or tests for the POC.
- Adding Quizzes as a compile-time dependency of `verifiers`.
- Fixture/database generation.
- Replaying original Spock setup methods.
- Calling Quizzes test helper methods or Quizzes-specific fixture builders.
- Supporting every current Quizzes catalog scenario.
- Multi-saga schedules, cross-saga interleavings, or concurrent execution semantics.
- Calling `resumeWorkflow` after scheduled step execution.
- Runtime fault injection.
- Behavior CSV generation.
- Domain-impact scoring.
- Genetic algorithm search over fault configurations.
- Scenario prioritization with bandits or other budget-allocation algorithms.
- Producing dynamic-enrichment evidence from executor runs.
- Joining executor run output back into `scenario-catalog-enriched.jsonl`.
- Changing scenario catalog generation, input recipe export, or dynamic-enrichment matching semantics.

## Further Notes

- The current recipe-aware Quizzes smoke catalog has no plan where every input recipe is strictly executor-ready, but it has plausible candidates when `SagaUnitOfWork` is treated as an executor-owned runtime argument.
- The runtime-owned argument override should remain narrow and explicit. It is a bridge from current recipe semantics to execution, not permission to ignore arbitrary blockers.
- A one-scenario successful Quizzes smoke is the intended proof for this POC. Expanding to all useful combinations requires later fixture strategy, broader recipe materialization, and multi-saga execution design.
- The target app remains unchanged for this POC, not necessarily forever. Future PRDs may add optional app-side fixture providers, generated harnesses, or seed scripts if generic fixture generation proves necessary.
