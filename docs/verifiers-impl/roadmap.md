# Verifier and scenario-generation roadmap

This roadmap describes the planned pipeline around the verifier module. It maps current static-analysis work to the later execution and search stages.

## Goal

Automate fault-scenario discovery for saga-based microservice applications by progressing from static scenario synthesis to executable scenarios, impact scoring, and search-based prioritization.

The implementation should remain application-agnostic: agents and algorithms should use workflow structure, step footprints, aggregate interactions, inputs, and execution feedback rather than hardcoded domain rules.

## Pipeline overview

1. **Static scenario synthesis** — analyse source/tests and generate relevant scenario plans.
2. **Dynamic evidence bridge** — optionally collect simulator JSONL evidence to enrich exact runtime key bindings before execution work.
3. **Scenario execution** — materialize inputs, instantiate sagas, execute steps in generated order, and apply fault bits.
4. **Impact analysis** — convert logs, traces, exceptions, compensation state, and domain state into impact scores.
5. **Local GA fault search** — search fault bit vectors within a fixed scenario.
6. **Scenario prioritization** — use bandit/RL-style selection to allocate budget across scenarios.

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
- Conflict-anchor segment-compressed scheduling and accounting for selected saga sets. This reduces Quizzes count-only selected-space totals while preserving configured conflict-anchor order cases under static evidence.

Remaining gaps:

- Exact aggregate-instance key binding is incomplete.
- Segment-compressed scheduling remains a static reduction under extracted conflict evidence; it does not prove semantic completeness or exact runtime aggregate-instance binding.
- Catalog entries are not yet executable by a runtime runner.

## Stage 1.5 — Dynamic evidence bridge

### Target

Before building a generic executor, the simulator can emit opt-in runtime evidence that confirms which concrete aggregate instances real application executions touched. This bridge should help convert conservative static/type-only footprints into exact key bindings without replacing the static catalog contract.

### Current status

Implemented as a verifier-orchestrated local/sagas bridge with sidecar enrichment:

- simulator dynamic evidence remains disabled by default (`simulator.dynamic-evidence.enabled=false`, `simulator.dynamic-evidence.test-context.enabled=false`);
- verifier dynamic enrichment remains disabled by default (`verifiers.dynamic-enrichment.enabled=false`);
- when enabled, the verifier runs selected test classes one by one with Maven, passing:
  - `-Dsimulator.dynamic-evidence.enabled=true`
  - `-Dsimulator.dynamic-evidence.test-context.enabled=true`
  - `-Djunit.platform.listeners.autodetection.enabled=true`
  - run-local dynamic-evidence output directories;
- each class writes per-class runtime artifacts (`dynamic-evidence.jsonl`, `dynamic-evidence-manifest.json`, `dynamic-input-map.json`, `test-run.json`, `maven-output.log`) under `<run-dir>/dynamic-evidence/<safe-test-class-fqn>/`;
- dynamic evidence is joined back to the static catalog with conservative statuses (`MATCHED_EXACT`, `MATCHED_HIGH_CONFIDENCE`, `MATCHED_PARTIAL`, `AMBIGUOUS`, `UNMATCHED`, `NOT_COVERED`);
- enriched outputs are sidecar-only (`scenario-catalog-enriched.jsonl`, `scenario-catalog-enriched-manifest.json`, `dynamic-evidence-join-report.json`), keeping `scenario-catalog.jsonl` unchanged;
- the simulator loads the verifier-written `dynamic-input-map.json` and emits `inputVariantId` when current test identity + runtime functionality class FQN + runtime step name resolve to exactly one accepted static input variant;
- before/after measurement is based on join-report status counts: first-pass propagation should increase `MATCHED_EXACT` when runtime evidence carries direct `inputVariantId`, and remaining ambiguity/unmatched records require later runtime refinement;
- Docker `fault-analysis-scenario-gen` enables this full static+dynamic flow with run-relative report path behavior.

Remaining gaps:

- Direct runtime `inputVariantId` propagation is still first-pass only; it does not yet use runtime command payloads, aggregate accesses, literal argument values, or aggregate keys to reduce the remaining ambiguous/unmatched candidates.
- Quizzes before/after counts were refreshed on 2026-05-12 against the comparable sagas-only 42-class run: `MATCHED_EXACT` improved `0 -> 46`, `AMBIGUOUS` dropped `44 -> 3`, `UNMATCHED` dropped `20 -> 17`, and enriched-manifest `warningCount` dropped `8238 -> 328`.
- No stream/gRPC/distributed or causal/TCC runtime hooks/parity yet.
- Remaining Quizzes ambiguity is now concentrated in a few same-shape neighboring-input cases rather than broad semantic fallback noise.
- Matched enriched entries still expose `testRunStatus: null` in the refreshed run, even though join-report test-run statuses exist.
- Logs and Jaeger traces remain auxiliary diagnostics rather than the primary evidence source.

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

POC only.

A narrow verifier-owned ScenarioExecutor POC can load a catalog/enriched catalog, select or receive a supported single-saga scenario, materialize only supported inputs, run the selected saga step schedule, and write an execution report. It has a Quizzes smoke success for one generated single-saga plan.

Generic execution is still not implemented. The POC does not cover arbitrary catalog replay, multi-saga schedules, generated fault injection, behavior CSV generation, impact scoring, GA search, or scenario prioritization.

Current static work prepares for the broader stage by preserving:

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
| Segment-compressed scheduling/accounting | Implemented static reduction | scheduler/accounting specs, dummyapp integration, Quizzes count-only comparison | Exact aggregate-instance binding and runtime semantic completeness remain separate |
| Dynamic evidence + sidecar enrichment | Implemented MVP | simulator dynamic-evidence package, verifier orchestrator, input-map sidecar, Task 7/8 Quizzes runs, 2026-05-12 exact-attribution refresh | Runtime attribution refinement; stream/gRPC/distributed/TCC parity; remaining ambiguity/unmatched analysis |
| Quizzes orchestration smoke baseline | Implemented | Task 7 narrow run + Task 8 full/default run in `current-state.md` | Refresh periodically and track exact/ambiguous/unmatched trends |
| ScenarioExecutor | POC | `scenario-executor-poc.md`, Quizzes single-saga smoke | General catalog replay, multi-saga execution, fault injection, impact scoring |
| Behavior CSV generation | Not implemented | none | Decide whether adapter or canonical contract |
| Impact scoring | Not implemented | none | Requires executable scenarios |
| GA search | Not implemented | none | Requires impact scoring |
| Bandit prioritization | Not implemented | none | Requires scenario-level rewards |

## Near-term milestone proposal

1. Keep static scenario catalog and sidecar-enrichment baselines reproducible on dummyapp and Quizzes (narrow + default runs).
2. Classify the remaining Quizzes enrichment misses (`AMBIGUOUS=3`, `UNMATCHED=17`) into uncovered static inputs, joiner limitations, and refinable attribution gaps.
3. Improve aggregate-instance key binding and reduce enrichment ambiguity without over-claiming exactness.
4. Tighten sidecar metadata completeness (including per-match test-run status population) and warning ergonomics.
5. Define the ScenarioExecutor input contract and decide JSONL-vs-CSV responsibilities.
6. Implement a minimal executor for single-saga scenarios.
7. Extend executor to bounded multi-saga schedules.
8. Add first impact metrics.
9. Add local GA search for a fixed scenario.
10. Add scenario prioritization after scenario execution and impact scoring exist.
