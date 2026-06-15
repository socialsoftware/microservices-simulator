## Parent PRD

`issues/scenario-executor-poc/prd.md`

## Type

AFK

## What to build

Add the verifier-owned CLI and orchestration path that prepares a target application and launches the generic executor on the target application classpath in a forked JVM/subprocess. The CLI must accept explicit runtime inputs, keep Quizzes out of the verifier compile-time dependency graph, and write only executor-specific output artifacts.

## Acceptance criteria

- [x] The CLI validates required runtime inputs: application base directory, Spring application class, Maven profile, Spring profiles, catalog path or run directory, and output path.
- [x] The CLI accepts an optional scenario plan id through `--scenario-id`.
- [x] The orchestrator prepares the target application before launching execution.
- [x] The forked JVM/subprocess command includes the target application classpath and verifier executor classes without requiring a compile-time Quizzes dependency in `verifiers`.
- [x] Spring profile arguments are passed to the target execution process.
- [x] Catalog/run-directory and output-path arguments are passed to the target execution process.
- [x] Fake process-runner tests verify command construction, working directory, terminal status mapping, and artifact-path handling without launching Quizzes.
- [x] Executor output artifacts are written separately from `scenario-catalog.jsonl`, `scenario-catalog-enriched.jsonl`, `dynamic-evidence.jsonl`, and dynamic join-report artifacts.

## Feature criteria covered

- AC-024
- AC-025
- AC-026

## Domain context

Preserve the boundary between verifier-owned orchestration and target application code. Dynamic enrichment remains a sidecar workflow; this executor CLI does not write dynamic evidence or join reports.

## Verification plan

- Run focused verifier tests for CLI/config validation and fake-process orchestration.
- Suggested command: `cd verifiers && mvn test -Dtest='*ScenarioExecutor*Cli*Spec,*ScenarioExecutor*Orchestrator*Spec' -DfailIfNoTests=false`

## Completion evidence

- Added `ScenarioExecutorCli` for target Spring runtime execution and `ScenarioExecutorOrchestrator` for verifier-owned target preparation plus forked JVM command construction.
- Runtime inputs are explicit: application base directory, Spring application class, Maven profile, Spring profiles, catalog path, optional scenario plan id, output path, and classpath.
- Forked command passes target classpath plus verifier executor main class, Spring profiles, catalog path, output path, and optional `--scenario-id`; no Quizzes compile-time dependency was added to `verifiers`.
- Executor report output path is explicit and separate from catalog and dynamic-enrichment artifacts.

## Verification

- Verified by `ScenarioExecutorOrchestratorSpec` fake process-runner tests for prepare command, forked command construction, working directory, preparation-failure terminal mapping, and required output-path validation.
- Verified command: `cd verifiers && mvn test -Dtest='*ScenarioExecutor*Spec' -DfailIfNoTests=false` on 2026-05-26: 10 tests run, 0 failures, 0 errors.

## Blocked by

- Blocked by `issues/scenario-executor-poc/005-structured-failure-reporting.md`

## User stories addressed

- User story 29
- User story 30
- User story 31
- User story 32
- User story 33
- User story 34
