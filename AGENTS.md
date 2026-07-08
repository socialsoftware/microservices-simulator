# Project Notes

Microservices Simulator is a Java/Spring project for testing DDD-style microservice applications under transactional models such as sagas and TCC. The verifier work analyzes those applications and their tests to generate fault-analysis scenarios for later execution and search.

There is no root Maven aggregator; run Maven from the relevant module directory.

## Maven Modules

- `simulator/`: the core simulator framework and runtime support, including coordination abstractions, saga/TCC support, messaging layers, versioning, and dynamic evidence hooks.
- `verifiers/`: the verifier/scenario-generation module; it extracts saga-oriented scenario structure from source/tests, generates deterministic scenario catalogs, can optionally enrich them with simulator runtime evidence, and includes a narrow single-saga ScenarioExecutor POC.
- `applications/quizzes/`: the main realistic application running on top of the simulator, with saga/TCC and local/stream/gRPC profiles.
- `applications/answers/` and `applications/teastore/`: generated/example simulator applications that provide additional application shapes.
- `applications/dummyapp/`: source-only fixture code for verifier tests; it is not a concrete application, but a "simulated simulated app" used to exercise verifier patterns and edge cases.
- `dsl/`: DSL/prototype area for generating application examples; generated examples may contain standalone Maven projects, but they are not the main simulator/verifier runtime.

## Domain Glossaries

Do not create or update a root `CONTEXT.md` in this repository.

Use `CONTEXT-MAP.md` to find the canonical glossary for each bounded context. For verifier, scenario-generation, dynamic-enrichment, saga-analysis, or fault-analysis terminology, use:

- `docs/verifiers-impl/glossary.md`

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
  - `scenario-executor`: narrow ScenarioExecutor POC runner; requires `CATALOG_PATH` and writes `OUTPUT_PATH` (defaults to `/reports/scenario-executor/execution-report.json`).
- Inspect runtime behavior with container logs rather than assuming local Maven output parity.
- Verifier run artifacts are mounted to host `verifiers/target/` (`/reports` in container), typically under per-run directories like `<application>-<timestamp>/` containing report HTML, scenario catalogs, dynamic evidence, and per-test `maven-output.log` files.
