# Verifier and scenario-generation roadmap

This roadmap describes the planned pipeline around the verifier module. It maps current static-analysis work to the later execution and search stages.

## Goal

Automate fault-scenario discovery for saga-based microservice applications by progressing from static scenario synthesis to executable scenarios, impact scoring, and search-based prioritization.

The implementation should remain application-agnostic: agents and algorithms should use workflow structure, step footprints, aggregate interactions, inputs, and execution feedback rather than hardcoded domain rules.

## Pipeline overview

1. **Static scenario synthesis** — analyse source/tests and generate relevant scenario plans.
2. **Scenario execution** — materialize inputs, instantiate sagas, execute steps in generated order, and apply fault bits.
3. **Impact analysis** — convert logs, traces, exceptions, compensation state, and domain state into impact scores.
4. **Local GA fault search** — search fault bit vectors within a fixed scenario.
5. **Scenario prioritization** — use bandit/RL-style selection to allocate budget across scenarios.

## Stage 1 — Static scenario synthesis

### Target

The verifier should statically obtain:

- Saga model: each saga and its ordered steps.
- Aggregate model: relevant aggregate types.
- Step-to-aggregate footprints: aggregate, access mode, and ideally instance key.
- Input variants from happy-path tests.
- Single-saga scenarios for each usable saga/input pair.
- Multi-saga scenario candidates for interacting sagas with compatible input bindings.
- Dependency-aware schedules, with redundant independent interleavings pruned or compressed where possible.
- A deterministic machine-readable catalog for later execution.

### Current status

Mostly implemented as a bounded static-analysis and catalog-generation MVP.

Implemented:

- Static extraction of sagas, steps, dependencies, dispatch footprints, command-handler/service access policies, and Groovy construction recipes.
- JSONL scenario catalog plus manifest.
- Conservative confidence labels for aggregate-key evidence.
- Bounded defaults to avoid large-app combinatorial explosion.

Remaining gaps:

- Exact aggregate-instance key binding is incomplete.
- Segment compression should be verified/matured before being claimed as an evaluation-ready optimization.
- Catalog entries are not yet executable by a runtime runner.

## Stage 2 — Scenario execution / generic runner

### Target

The runtime side should:

1. Load a scenario catalog record or derived behavior specification.
2. Materialize required inputs and runtime dependencies.
3. Instantiate saga/functionality objects.
4. Execute steps in the generated schedule.
5. Apply a fault bit vector to determine failed/successful steps.
6. Produce logs, traces, and domain-state observations.

### Current status

Not implemented.

Current static work prepares for this stage by preserving:

- source expression/provenance text;
- replay-oriented value recipes;
- expected constructor argument types;
- unresolved placeholder categories;
- deterministic scenario and schedule identifiers.

Open design questions:

- Should the runner consume JSONL directly?
- Should the verifier generate simulator behavior CSV files as an adapter format?
- How much runtime dependency materialization should be generic versus application-specific?

## Stage 3 — Impact analysis

### Target

Reduce rich execution data into a scalar impact score and/or structured impact report.

Potential signals:

- invariant violations;
- unhandled exceptions;
- failed or incomplete compensations;
- state divergence after scenario execution;
- aborted sagas;
- latency/timing anomalies;
- trace error tags and logs.

### Current status

Not implemented.

This stage depends on executable generated scenarios and a repeatable way to collect execution results.

## Stage 4 — Local GA fault search

### Target

For a fixed scenario, search over fault bit vectors.

Expected shape:

- Chromosome: binary string over scheduled steps.
- Initial population: single-point failures, write-step heuristics, and random configurations.
- Duplicate avoidance: do not re-run already evaluated fault vectors.
- Selection: tournament or similar.
- Crossover: likely uniform crossover.
- Mutation: bit-flip, possibly adaptive based on diversity.
- Fitness: impact score, potentially combined with diversity/novelty.

### Current status

Not implemented.

Dependencies:

- ScenarioExecutor/generic runner.
- Impact scoring.
- Stable scenario/fault-space identifiers.

## Stage 5 — Scenario prioritization

### Target

Select which generated scenarios deserve execution budget.

Expected shape:

- Scenarios are arms.
- Context features describe saga types, aggregate types, step counts, conflict evidence, or other structural properties.
- Rewards come from GA results or aggregate impact scores.
- Selected scenarios are removed from the available set because deterministic re-execution should not provide new information.
- Shared features allow information from explored scenarios to influence prioritization of unexplored scenarios.

### Current status

Not implemented.

Dependencies:

- Scenario catalog with useful structural metadata.
- Executable scenarios.
- Impact scores from local search.

## Status matrix

| Roadmap item | Status | Evidence / source | Main gap |
|---|---|---|---|
| Application source discovery | Implemented | `ApplicationsFileTreeParser` and specs | Keep robust as project layout evolves |
| Domain service classification | Implemented | dispatch-target pipeline and dummyapp specs | Profile-aware ambiguous service resolution |
| Command-handler mapping | Implemented | command-handler visitor specs | Broader syntax coverage as discovered |
| Saga step extraction | Implemented | workflow visitor specs | Generic command wrappers in compensation paths |
| Groovy input tracing | Implemented / evolving | Groovy constructor trace specs | Runtime materialization not implemented |
| Facade call recipe extraction | Implemented | dummyapp and Quizzes report/spec assertions | Keep expanding real syntax coverage |
| HTML report | Implemented | renderer/application specs | Human view only, not machine contract |
| Scenario catalog JSONL/manifest | Implemented MVP | scenario package, writer, application specs | Exact keys, filters, executor integration |
| Bounded Quizzes smoke | Recommended | current-state next priority | Refresh evidence and inspect diagnostics |
| ScenarioExecutor | Not implemented | none | Design and runtime integration |
| Behavior CSV generation | Not implemented | none | Decide whether adapter or canonical contract |
| Impact scoring | Not implemented | none | Requires executable scenarios |
| GA search | Not implemented | none | Requires impact scoring |
| Bandit prioritization | Not implemented | none | Requires scenario-level rewards |

## Near-term milestone proposal

1. Stabilize and validate the static scenario catalog on dummyapp and bounded Quizzes runs.
2. Improve aggregate-instance key binding enough to support meaningful multi-saga conflict evidence.
3. Define the ScenarioExecutor input contract and decide JSONL-vs-CSV responsibilities.
4. Implement a minimal executor for single-saga scenarios.
5. Extend executor to bounded multi-saga schedules.
6. Add first impact metrics.
7. Add local GA search for a fixed scenario.
8. Add scenario prioritization after scenario execution and impact scoring exist.
