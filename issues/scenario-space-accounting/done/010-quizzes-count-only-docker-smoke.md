# 010 - Quizzes Count-Only Docker Smoke

Mode: AFK
Parent PRD: ./prd.md
Blocked by: 009-dummyapp-accounting-integration.md
Feature Criteria Covered: AC-17
Verification Mode: docker
Proof Required: Docker Compose strict and type-fallback Quizzes count-only runs with expected artifacts and accounting assertions

## Slice Contract

Validate Quizzes-scale compressed accounting in the Docker Compose verifier environment without materializing a huge scenario catalog.

## Acceptance Criteria

- The issue implementation documents and runs exact Docker Compose commands or equivalent root-compose verifier service invocations for two Quizzes count-only runs.
- Run 1 uses `INTERACTION_PRUNED`, `allowTypeOnlyFallback=false`, `COUNT_ONLY`, `maxSagaSetSize=3`, `ORDER_PRESERVING_INTERLEAVING`, and dynamic enrichment disabled.
- Run 2 uses `INTERACTION_PRUNED`, `allowTypeOnlyFallback=true`, `COUNT_ONLY`, `maxSagaSetSize=3`, `ORDER_PRESERVING_INTERLEAVING`, and dynamic enrichment disabled.
- Each run uses an explicit output root or run directory suitable for verification.
- Each run records timeout guidance and artifact locations in completion evidence.
- Each run produces an empty `scenario-catalog.jsonl`.
- Each run produces `scenario-catalog-manifest.json`.
- Each run produces `scenario-catalog-rejected-inputs.jsonl`.
- Each run produces `scenario-space-accounting.json`.
- Each accounting artifact contains non-empty grouped rows.
- Each accounting artifact contains `allInputBound` totals.
- Each accounting artifact contains `selectedByGenerator` totals.
- Each accounting artifact contains `catalogWritten=0` or equivalent zero written-plan totals.
- Each accounting artifact contains `typeLevelCoverage`.
- At least one large Quizzes combinatoric count is serialized as a JSON string.

## Domain Context

- Quizzes is the thesis-scale realistic target.
- Docker Compose validation is preferred when artifacts must match the user's observed environment.
- Full Quizzes brute-force catalog materialization is explicitly out of scope.

## Implementation Notes

- Use the root compose verifier service pattern from project notes.
- Keep dynamic enrichment disabled for this smoke.
- Do not modify Quizzes source or tests.
- Capture the exact commands and output artifact paths in completion evidence.

## Completion Evidence

- Implementation: No Quizzes source or test files were modified. The slice was implemented as Docker/container smoke evidence using the root Compose-built `fault-analysis-scenario-gen:latest` verifier image and mounted `verifiers/target` reports directory.
- Verification: Initial root Compose invocation required `CATALOG_PATH=/unused` because the unrelated `scenario-executor` service has a required compose interpolation variable. Compose then built `fault-analysis-scenario-gen:latest` but failed while writing build metadata with `no space left on device`; the image existed afterward and was reused directly to avoid another build on the nearly-full filesystem.
- Verification command, strict run, 20 minute timeout guidance: `timeout 20m docker run --rm -v /home/andre/microservices-simulator/verifiers/target:/reports -e SPRING_PROFILES_ACTIVE=sagas,local -e VERIFIERS_APPLICATIONS_ROOT=/applications -e VERIFIERS_APPLICATION_BASE_DIR=quizzes -e VERIFIERS_OUTPUT_ROOT=/reports/scenario-space-accounting-smoke/strict-large -e VERIFIERS_REPORT_HTML_PATH=analysis-report.html -e VERIFIERS_SCENARIO_CATALOG_ENABLED=true -e VERIFIERS_SCENARIO_CATALOG_GENERATION_STRATEGY=INTERACTION_PRUNED -e VERIFIERS_SCENARIO_CATALOG_CATALOG_WRITE_MODE=COUNT_ONLY -e VERIFIERS_SCENARIO_CATALOG_MAX_SAGA_SET_SIZE=3 -e VERIFIERS_SCENARIO_CATALOG_MAX_INPUT_VARIANTS_PER_SAGA=1000 -e VERIFIERS_SCENARIO_CATALOG_MAX_SCHEDULES_PER_INPUT_TUPLE=1000000000 -e VERIFIERS_SCENARIO_CATALOG_SCHEDULE_STRATEGY=ORDER_PRESERVING_INTERLEAVING -e VERIFIERS_SCENARIO_CATALOG_ALLOW_TYPE_ONLY_FALLBACK=false -e VERIFIERS_DYNAMIC_ENRICHMENT_ENABLED=false fault-analysis-scenario-gen:latest`.
- Verification command, type-fallback run, 20 minute timeout guidance: `timeout 20m docker run --rm -v /home/andre/microservices-simulator/verifiers/target:/reports -e SPRING_PROFILES_ACTIVE=sagas,local -e VERIFIERS_APPLICATIONS_ROOT=/applications -e VERIFIERS_APPLICATION_BASE_DIR=quizzes -e VERIFIERS_OUTPUT_ROOT=/reports/scenario-space-accounting-smoke/type-fallback-large -e VERIFIERS_REPORT_HTML_PATH=analysis-report.html -e VERIFIERS_SCENARIO_CATALOG_ENABLED=true -e VERIFIERS_SCENARIO_CATALOG_GENERATION_STRATEGY=INTERACTION_PRUNED -e VERIFIERS_SCENARIO_CATALOG_CATALOG_WRITE_MODE=COUNT_ONLY -e VERIFIERS_SCENARIO_CATALOG_MAX_SAGA_SET_SIZE=3 -e VERIFIERS_SCENARIO_CATALOG_MAX_INPUT_VARIANTS_PER_SAGA=1000 -e VERIFIERS_SCENARIO_CATALOG_MAX_SCHEDULES_PER_INPUT_TUPLE=1000000000 -e VERIFIERS_SCENARIO_CATALOG_SCHEDULE_STRATEGY=ORDER_PRESERVING_INTERLEAVING -e VERIFIERS_SCENARIO_CATALOG_ALLOW_TYPE_ONLY_FALLBACK=true -e VERIFIERS_DYNAMIC_ENRICHMENT_ENABLED=false fault-analysis-scenario-gen:latest`.
- Verification artifacts, strict run: `/home/andre/microservices-simulator/verifiers/target/scenario-space-accounting-smoke/strict-large/quizzes-20260603-164323-792/` containing `scenario-catalog.jsonl`, `scenario-catalog-manifest.json`, `scenario-catalog-rejected-inputs.jsonl`, and `scenario-space-accounting.json`.
- Verification artifacts, type-fallback run: `/home/andre/microservices-simulator/verifiers/target/scenario-space-accounting-smoke/type-fallback-large/quizzes-20260603-164412-020/` containing `scenario-catalog.jsonl`, `scenario-catalog-manifest.json`, `scenario-catalog-rejected-inputs.jsonl`, and `scenario-space-accounting.json`.
- Verification assertions: both catalogs were empty; both manifests had `catalogWriteMode=COUNT_ONLY` and `scenariosExported=0`; both accounting artifacts had `targetApplication=quizzes`, `generationStrategy=INTERACTION_PRUNED`, `catalogWriteMode=COUNT_ONLY`, `maxSagaSetSize=3`, `maxInputVariantsPerSaga=1000`, `maxSchedulesPerInputTuple=1000000000`, `scheduleStrategy=ORDER_PRESERVING_INTERLEAVING`, expected `allowTypeOnlyFallback`, non-empty `groupedSagaSets`, `typeLevelCoverage`, string `allInputBound` and `selectedByGenerator` totals, and `catalogWritten.total='0'`.
- Verification output: strict run `groupedRows=2625`, `allInputBound=119329379005`, `selected=102858915516`, `catalogWritten=0`, `largeStringExample=119329379005`; type-fallback run `groupedRows=2625`, `allInputBound=119329379005`, `selected=114785788163`, `catalogWritten=0`, `largeStringExample=119329379005`.
- Slice compliance review: PASS (`ses_1719f740cffeD1iDAKuYO0dI4E`).
- Code quality review: PASS (`ses_1719f73e8ffeO2cQ3kCEyKgE2l`).
- Notes: The final successful invocations used the root Compose-built verifier image directly because the installed Compose plugin lacks `run --no-build` and the filesystem had only about 1.1 GB free after image build. The final runs still exercised the same verifier container image, mounted `/reports` to `verifiers/target`, targeted Quizzes, and disabled dynamic enrichment.
