# Decision: use JSONL/manifest as the scenario catalog contract

Date: 2026-04-27

## Status

Accepted.

## Context

The verifier HTML report is useful for human inspection, but it is not a stable or efficient contract for a future ScenarioExecutor. The next pipeline stage needs structured data: scenario IDs, saga instances, input variants, schedules, fault slots, conflict evidence, warnings, and configuration metadata.

Naively parsing the HTML report would couple execution tooling to a human presentation layer and would not solve bounded scenario generation.

## Decision

Introduce a bounded, versioned machine-readable scenario catalog:

- `scenario-catalog.jsonl` — one JSON object per `ScenarioPlan`.
- `scenario-catalog-manifest.json` — generated-at timestamp, effective configuration, counts, warnings, and output paths.
- Schema version: `microservices-simulator.scenario-catalog.v1`.

The HTML report remains a separate human-facing analysis artifact.

## Rationale

- JSONL is stream-friendly and avoids one-file-per-scenario output.
- The manifest captures diagnostics and conservative assumptions.
- A normalized scenario IR decouples static analysis, generation logic, export format, and human reporting.
- Bounded defaults prevent Quizzes-scale runs from accidentally materializing unbounded combinations.

## Conservative modeling choices

- Exact aggregate-instance key matching is not claimed unless supported by extracted facts.
- Type-only matching is explicit and opt-in for multi-saga fallback.
- Missing aggregate names remain unknown/non-matchable, not mislabeled as type-only.
- Unresolved inputs are represented with replay status and warnings rather than fabricated concrete values.

## Safe defaults

- Export disabled by default.
- `includeSingles=true`.
- `maxSagaSetSize=1`.
- `maxScenarios=100`.
- `maxInputVariantsPerSaga=3`.
- `maxSchedulesPerInputTuple=20`.
- `allowTypeOnlyFallback=false`.
- `inputPolicy=RESOLVED_OR_REPLAYABLE`.
- `scheduleStrategy=SERIAL`.
- `deterministicSeed=1234`.

## Rejected alternative

Export or parse the current HTML report as structured JSON.

Rejected because it would make the future executor depend on presentation details and would obscure scenario-specific boundaries.

## Revisit when

- ScenarioExecutor requirements force schema changes.
- Behavior CSV generation is designed; decide whether CSV is an adapter artifact or a primary contract.
- Exact aggregate-key extraction becomes reliable enough to change default multi-saga generation behavior.
