# Decision: keep verifier scope saga-only for now

Date: 2026-04-05

## Status

Accepted.

## Context

The broader simulator supports both sagas and TCC/causal consistency. The verifier/scenario-generator work is still stabilizing extraction of saga workflows, command dispatch, service policies, Groovy test inputs, and scenario catalogs.

Adding TCC fixtures and visitors at the same time would expand the analysis surface before the saga path is reliable.

## Decision

Keep verifier work saga-only unless the scope is explicitly changed.

Practical consequences:

- Do not add TCC dummyapp fixtures by default.
- Do not add verifier tests for `CausalWorkflow` or TCC `Step` extraction unless requested.
- Phrase new backlog items and scenario-generation work in saga terms.
- Preserve TCC application/runtime behavior outside verifier work.

## Rationale

- Saga scenario generation is already complex enough: steps, dependencies, compensations, command dispatches, input traces, and generated schedules.
- The near-term roadmap needs an executable saga scenario path before widening to TCC.
- This keeps dummyapp focused and avoids duplicating fixture patterns prematurely.

## Revisit when

- Saga scenario catalog generation and execution are stable.
- The evaluation requires comparing saga and TCC fault behavior.
- A concrete TCC verifier task is approved.
