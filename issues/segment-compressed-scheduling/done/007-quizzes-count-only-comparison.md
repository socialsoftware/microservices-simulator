# 007 - Quizzes Count-Only Comparison

Mode: AFK
Parent PRD: ./prd.md
Blocked by: 006-dummyapp-segment-compressed-integration.md
Feature Criteria Covered: AC-18, AC-19
Verification Mode: docker
Proof Required: Docker/root-container Quizzes count-only runs with dynamic enrichment disabled, artifact paths, matching `sagaSetKey` row comparisons, lower segment-compressed schedule counts, and `catalogWritten=0`

## Slice Contract

Validate thesis-scale accounting behavior on Quizzes without materializing plans.

Run comparable count-only accounting configurations for `ORDER_PRESERVING_INTERLEAVING` and `SEGMENT_COMPRESSED`, with dynamic enrichment disabled and `maxSagaSetSize=3`. Compare matching selected `sagaSetKey` rows across accounting artifacts and capture evidence that segment compression reduces schedule counts for at least one selected size-3 row.

## Acceptance Criteria

- The issue implementation documents and runs exact Docker Compose, root-container, or equivalent verifier-container commands for comparable Quizzes count-only runs.
- One run uses `scheduleStrategy=ORDER_PRESERVING_INTERLEAVING`.
- One run uses `scheduleStrategy=SEGMENT_COMPRESSED`.
- Both runs use dynamic enrichment disabled, `catalogWriteMode=COUNT_ONLY`, `generationStrategy=INTERACTION_PRUNED`, and `maxSagaSetSize=3`.
- Both runs use comparable input policy, type-only fallback setting, input bounds, schedule caps, and output-root settings unless differences are explicitly justified in completion evidence.
- Both runs write an empty `scenario-catalog.jsonl`.
- Both runs write `scenario-catalog-manifest.json`.
- Both runs write `scenario-catalog-rejected-inputs.jsonl`.
- Both runs write `scenario-space-accounting.json`.
- Both accounting artifacts contain non-empty grouped rows, selected-space totals, type-level coverage, and `catalogWritten=0`.
- Matching `sagaSetKey` rows across both accounting artifacts show at least one selected size-3 row where `SEGMENT_COMPRESSED.scheduleCountPerTuple` is lower than `ORDER_PRESERVING_INTERLEAVING.scheduleCountPerTuple`.
- Selected totals are reduced overall, or any unchanged matching rows inspected in completion evidence are explained by all-anchor/no-anchor structure.
- Exact commands, timeout guidance, artifact paths, and count excerpts are captured in completion evidence.
- No Quizzes source or test files are modified.

## Domain Context

- Quizzes is the realistic thesis-scale target where step-level order-preserving interleavings make size-3 accounting explode.
- Count-only mode is required because full Quizzes scenario materialization is intentionally out of scope.
- Docker/container validation is preferred when artifacts must match the user's observed environment.

## Implementation Notes

- Use the root compose verifier service pattern from project notes when feasible.
- If the local Docker Compose plugin or disk space prevents a compose run, use the already-built verifier container pattern only if it exercises the same verifier image and mounted artifact layout; document the deviation clearly.
- Do not enable dynamic enrichment for this smoke.
- Do not implement executor, fault injection, GA, bandit, or impact scoring.

## Completion Evidence

- Implementation: Ran comparable Docker Compose Quizzes count-only verifier smokes using the root `fault-analysis-scenario-gen` service. No production code changes and no Quizzes source/test modifications were required.
- Verification: PASS - OPI command from repository root, with unrelated compose interpolation satisfied by `CATALOG_PATH=/tmp/unused-scenario-catalog.jsonl`:
  `CATALOG_PATH=/tmp/unused-scenario-catalog.jsonl docker compose run --rm -e VERIFIERS_OUTPUT_ROOT=/reports/segment-compressed-scheduling-007/opi -e VERIFIERS_DYNAMIC_ENRICHMENT_ENABLED=false -e VERIFIERS_SCENARIO_CATALOG_CATALOG_WRITE_MODE=COUNT_ONLY -e VERIFIERS_SCENARIO_CATALOG_GENERATION_STRATEGY=INTERACTION_PRUNED -e VERIFIERS_SCENARIO_CATALOG_MAX_SAGA_SET_SIZE=3 -e VERIFIERS_SCENARIO_CATALOG_MAX_INPUT_VARIANTS_PER_SAGA=3 -e VERIFIERS_SCENARIO_CATALOG_MAX_SCHEDULES_PER_INPUT_TUPLE=1000000 -e VERIFIERS_SCENARIO_CATALOG_ALLOW_TYPE_ONLY_FALLBACK=true -e VERIFIERS_SCENARIO_CATALOG_INPUT_POLICY=RESOLVED_OR_REPLAYABLE -e VERIFIERS_SCENARIO_CATALOG_SCHEDULE_STRATEGY=ORDER_PRESERVING_INTERLEAVING fault-analysis-scenario-gen`
- Verification: PASS - segment-compressed command from repository root, with same settings except output root and schedule strategy:
  `CATALOG_PATH=/tmp/unused-scenario-catalog.jsonl docker compose run --rm -e VERIFIERS_OUTPUT_ROOT=/reports/segment-compressed-scheduling-007/segment -e VERIFIERS_DYNAMIC_ENRICHMENT_ENABLED=false -e VERIFIERS_SCENARIO_CATALOG_CATALOG_WRITE_MODE=COUNT_ONLY -e VERIFIERS_SCENARIO_CATALOG_GENERATION_STRATEGY=INTERACTION_PRUNED -e VERIFIERS_SCENARIO_CATALOG_MAX_SAGA_SET_SIZE=3 -e VERIFIERS_SCENARIO_CATALOG_MAX_INPUT_VARIANTS_PER_SAGA=3 -e VERIFIERS_SCENARIO_CATALOG_MAX_SCHEDULES_PER_INPUT_TUPLE=1000000 -e VERIFIERS_SCENARIO_CATALOG_ALLOW_TYPE_ONLY_FALLBACK=true -e VERIFIERS_SCENARIO_CATALOG_INPUT_POLICY=RESOLVED_OR_REPLAYABLE -e VERIFIERS_SCENARIO_CATALOG_SCHEDULE_STRATEGY=SEGMENT_COMPRESSED fault-analysis-scenario-gen`
- Verification: PASS - timeout guidance: each Docker command was run with a 1,200,000 ms tool timeout; observed runtime was about one minute per run after image availability.
- Verification: PASS - artifact paths: OPI artifacts under `verifiers/target/segment-compressed-scheduling-007/opi/quizzes-20260616-033409-334/`; segment-compressed artifacts under `verifiers/target/segment-compressed-scheduling-007/segment/quizzes-20260616-033527-670/`.
- Verification: PASS - both runs wrote `scenario-catalog.jsonl` (0 bytes / empty), `scenario-catalog-manifest.json`, `scenario-catalog-rejected-inputs.jsonl`, and `scenario-space-accounting.json`. Both manifests report `catalogWriteMode=COUNT_ONLY` and `scenariosExported=0`; both accounting files report `catalogWritten.total=0`.
- Verification: PASS - comparable run configs from accounting: both use `generationStrategy=INTERACTION_PRUNED`, `catalogWriteMode=COUNT_ONLY`, `maxSagaSetSize=3`, `maxInputVariantsPerSaga=3`, `maxSchedulesPerInputTuple=1000000`, `allowTypeOnlyFallback=true`, and `inputPolicy=RESOLVED_OR_REPLAYABLE`; OPI uses `ORDER_PRESERVING_INTERLEAVING`, segment run uses `SEGMENT_COMPRESSED`.
- Verification: PASS - accounting excerpts: OPI grouped rows `2625`, discovered saga count `65`, selected total `218528454`, catalog written `0`; segment grouped rows `2625`, discovered saga count `65`, selected total `1019393`, catalog written `0`.
- Verification: PASS - matching selected size-3 row reductions: 573 rows reduced. Example selected size-3 row `pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.StartQuizFunctionalitySagas|pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.AddStudentFunctionalitySagas|pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.CreateQuizFunctionalitySagas`: OPI `scheduleCountPerTuple=560`, segment `scheduleCountPerTuple=12`; OPI `scenarioShapeCount=10080`, segment `scenarioShapeCount=216`; compatible tuples `18`.
- Verification: PASS - selected totals reduced overall from `218528454` to `1019393`, so unchanged inspected matching rows do not need to explain the overall result.
- Verification: PASS - `git status --short -- applications/quizzes/src applications/quizzes/src/test` returned no paths.
- Slice compliance review: PASS - read-only spec compliance review task `ses_1317b9fbbffeV47913zHOm21qI` after completion evidence was persisted.
- Code quality review: PASS - read-only code quality review task `ses_1317b9f8cffetzIgAPrczdmpB7` after completion evidence was persisted.
- Notes: The compose command needs the harmless `CATALOG_PATH=/tmp/unused-scenario-catalog.jsonl` prefix because the root compose file interpolates `CATALOG_PATH` for the unrelated `scenario-executor` service even when only running `fault-analysis-scenario-gen`.
