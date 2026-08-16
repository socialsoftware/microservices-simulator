# Project Notes

Microservices Simulator is a Java/Spring project for testing DDD-style microservice applications under transactional models such as sagas and TCC. The verifier work analyzes those applications and their tests to generate fault-analysis scenarios for later execution and search.

There is no root Maven aggregator; run Maven from the relevant module directory.

## Maven Modules

- `simulator/`: the core simulator framework and runtime support, including coordination abstractions, saga/TCC support, messaging layers, versioning, and dynamic evidence hooks.
- `verifiers/`: the verifier/scenario-generation module; it extracts saga-oriented scenario structure from source/tests, generates deterministic compensation-aware v4 packages, can optionally attach workload-linked runtime evidence, and includes a narrow deterministic Saga/local ScenarioExecutor.
- `applications/quizzes/`: the main realistic application running on top of the simulator, with saga/TCC and local/stream/gRPC profiles.
- `applications/answers/` and `applications/teastore/`: generated/example simulator applications that provide additional application shapes.
- `applications/dummyapp/`: source-only fixture code for verifier tests; it is not a concrete application, but a "simulated simulated app" used to exercise verifier patterns and edge cases.
- `dsl/`: DSL/prototype area for generating application examples; generated examples may contain standalone Maven projects, but they are not the main simulator/verifier runtime.

## Domain Context

Do not create or update a root `CONTEXT.md` in this repository.

Use `CONTEXT-MAP.md` to find the canonical context for each bounded context. For verifier, scenario-generation, dynamic-enrichment, Saga-analysis, or fault-analysis terminology and current behavior, use:

- `docs/verifiers-impl/current-state.md`

## Verifiers Thesis Context (PIC2)

- `verifiers/` development follows the thesis direction documented in `André_Silva___IST_UL___MEIC_PIC2.pdf`.
- The PDF is large but relevant context for simulator background, intended architecture, and evaluation goals.
- Use the PDF as intent context; use repository code and `docs/verifiers-impl/current-state.md` as the source of truth for what is already implemented.

## Verifier Patterns and Fixtures

- Preserve the existing verifier pipeline boundaries: `visitor/*` -> `ApplicationAnalysisState` -> `scenario/adapter/*` -> `scenario/*` -> `dynamic/*`.
- Keep outputs reproducible (stable ordering, deterministic IDs, explicit seeds, no hidden randomness).
- Keep `applications/dummyapp/` as the canonical verifier fixture for parser/scenario/enrichment edge cases.
- Use `applications/quizzes/` as the realistic high-complexity target for smoke checks and thesis-scale evaluation.
- For generic verifier mechanisms, add/update Spock coverage in `verifiers/src/test/groovy` with dummyapp-first positive and negative cases. Test application-provided behavior in the target application; add dummyapp coverage only when it exercises the generic contract.

## Docker Workflow for Verifier Runtime Evidence

- Prefer Docker Compose runs when runtime evidence, logs, and artifacts must match the user's observed environment ("see as the user sees").
- Verifier containers are defined in root compose and can be run from the repository root:
  - `fault-analysis-scenario-gen`: static + dynamic verifier pipeline (target app defaults to `quizzes`).
  - `fault-analysis-scenario-gen-test`: verifier test container (target app = `dummyapp`).
  - `scenario-executor`: narrow Saga/local ScenarioExecutor; normal execution requires `PACKAGE_PATH` and `FAULT_SCENARIO_ID`, while setup preflight uses `PREFLIGHT=true`; both write `OUTPUT_PATH` (default `/reports/scenario-executor/execution-report.json`).
- Inspect runtime behavior with container logs rather than assuming local Maven output parity.
- Verifier run artifacts are mounted to host `verifiers/target/` (`/reports` in container). A normal generation run writes the five-file v4 package under `<application>-<timestamp>/`; optional dynamic evidence and its logs are additive, and preflight/execution reports are separate outputs.

## Glossary

`docs/verifiers-impl/current-state.md#the-essential-terms` owns domain terms used across docs, code, and plans. Use its definitions consistently. Propose entries for new modeled concepts; do not add generic words or UI labels. Mark planned but unimplemented terms `(future)`.

## Canonical Documentation

- `docs/verifiers-impl/current-state.md` owns current verifier behavior and terminology.
- `docs/verifiers-impl/roadmap.md` owns intended verifier outcomes, sequencing, and scope.
- `docs/verifiers-impl/decisions/` owns durable verifier design decisions.

Before planning, implementing, or reviewing, read only the relevant canonical docs, glossary terms, and exact issue package. Update affected canonical docs in the same change as behavior.

## Feature Workflow

Use the global skills `disambiguate → spec → plan → execute`. There are two routes:

- direct: agreed brief → implementation approval → execute;
- documented: `SPEC.md` → `PLAN.md` → implementation approval → execute.

Documented issues live at `issues/YYYY-MM-DD-<slug>/` and contain `SPEC.md` and `PLAN.md`. Spec and plan are one planning phase; implementation always stops for explicit approval before worktree creation or source changes unless the user explicitly waives that gate.

## Artifact Authority

- The spec owns approved product behavior, boundaries, assumptions, and terminology.
- The plan owns execution strategy, milestone boundaries, discovery, risks, and proof.
- Canonical docs own current shipped behavior.
- Handoffs own actual changed files, implementation discoveries, deviations, and proof.
- Update governing artifacts together when approved intent or strategy changes.

## Execution Scope

Plans approve outcomes and change boundaries, not exhaustive file allowlists. Necessary implementation details inside the boundary may proceed and must be reported. Material changes to intent, security/data/API or production boundaries, architecture, rollout, reversibility, or meaningful cost require user approval. Do not absorb unrelated findings.

## Workflow Safety

- Preserve unrelated dirty work.
- Never perform destructive, production, deployment, release, push, or merge actions without explicit authority.
- Run the narrowest meaningful validation and report checks honestly.

## Application-Generation Harness

The aggregate-by-aggregate application-generation harness is a supported, explicitly scoped workflow; it is not the repository's default objective. Use it only when the user requests generated-application work, then follow `docs/workflow.md`, the relevant material under `docs/concepts/` and `docs/templates/`, and the target application's local guidance.

Generated application service code must remain profile-agnostic: service classes depend on abstract factories and repository interfaces, not concrete Saga-profile implementations. This preserves compatibility with other transactional profiles.

Relevant module guidance:

- `simulator/AGENTS.md` for framework-specific work;
- `applications/quizzes/AGENTS.md` for Quizzes application-generation work.

For that workflow, install the simulator first with `cd simulator && mvn install`, then run application tests from the application module with the required profile, for example `cd applications/quizzes && mvn -Ptest-sagas test`.
