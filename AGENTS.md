# Project Notes

Microservices Simulator is a Java/Spring project for testing DDD-style microservice applications under transactional models such as sagas and TCC. The verifier work analyzes those applications and their tests to generate fault-analysis scenarios for later execution and search.

There is no root Maven aggregator; run Maven from the relevant module directory.

## Maven Modules

- `simulator/`: the core simulator framework and runtime support, including coordination abstractions, saga/TCC support, messaging layers, versioning, and dynamic evidence hooks.
- `verifiers/`: the verifier/scenario-generation module; it extracts saga-oriented scenario structure from source/tests, generates deterministic compensation-aware v3 packages, can optionally attach workload-linked runtime evidence, and includes a narrow deterministic Saga/local ScenarioExecutor.
- `applications/quizzes/`: the main realistic application running on top of the simulator, with saga/TCC and local/stream/gRPC profiles.
- `applications/answers/` and `applications/teastore/`: generated/example simulator applications that provide additional application shapes.
- `applications/dummyapp/`: source-only fixture code for verifier tests; it is not a concrete application, but a "simulated simulated app" used to exercise verifier patterns and edge cases.
- `dsl/`: DSL/prototype area for generating application examples; generated examples may contain standalone Maven projects, but they are not the main simulator/verifier runtime.

## Domain Context

Do not create or update a root `CONTEXT.md` in this repository.

Use `CONTEXT-MAP.md` to find the canonical context for each bounded context. For verifier, scenario-generation, dynamic-enrichment, Saga-analysis, or fault-analysis terminology and current behavior, use:

- `docs/verifiers-impl/current-state.md`

Define verifier terms at first use in that handbook instead of creating a parallel glossary.

## Verifiers Thesis Context (PIC2)

- `verifiers/` development follows the thesis direction documented in `André_Silva___IST_UL___MEIC_PIC2.pdf`.
- The PDF is large but relevant context for simulator background, intended architecture, and evaluation goals.
- Use the PDF as intent context; use repository code and `docs/verifiers-impl/current-state.md` as the source of truth for what is already implemented.

## Verifier Patterns and Fixtures

- Preserve the existing verifier pipeline boundaries: `visitor/*` -> `ApplicationAnalysisState` -> `scenario/adapter/*` -> `scenario/*` -> `dynamic/*`.
- Keep outputs reproducible (stable ordering, deterministic IDs, explicit seeds, no hidden randomness).
- Keep `applications/dummyapp/` as the canonical verifier fixture for parser/scenario/enrichment edge cases.
- Use `applications/quizzes/` as the realistic high-complexity target for smoke checks and thesis-scale evaluation.
- For verifier behavior changes, add/update Spock coverage in `verifiers/src/test/groovy`, with dummyapp-first coverage before quizzes-specific expectations.

## Docker Workflow for Verifier Runtime Evidence

- Prefer Docker Compose runs when runtime evidence, logs, and artifacts must match the user's observed environment ("see as the user sees").
- Verifier containers are defined in root compose and can be run from the repository root:
  - `fault-analysis-scenario-gen`: static + dynamic verifier pipeline (target app defaults to `quizzes`).
  - `fault-analysis-scenario-gen-test`: verifier test container (target app = `dummyapp`).
  - `scenario-executor`: narrow Saga/local ScenarioExecutor; normal execution requires `PACKAGE_PATH` and `FAULT_SCENARIO_ID`, while setup preflight uses `PREFLIGHT=true`; both write `OUTPUT_PATH` (default `/reports/scenario-executor/execution-report.json`).
- Inspect runtime behavior with container logs rather than assuming local Maven output parity.
- Verifier run artifacts are mounted to host `verifiers/target/` (`/reports` in container). A normal generation run writes the five-file v3 package under `<application>-<timestamp>/`; optional dynamic evidence and its logs are additive, and preflight/execution reports are separate outputs.
