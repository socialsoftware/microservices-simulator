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

- Static extraction of sagas, steps, dependencies, dispatch footprints, command-handler/service access policies, Groovy construction recipes, facade calls, and event-origin inputs for the implemented `EventHandling`/`EventProcessing` shape.
- JSONL scenario catalog plus manifest.
- Conservative confidence labels for aggregate-key evidence.
- Bounded defaults to avoid large-app combinatorial explosion.
- Conflict-anchor segment-compressed scheduling and accounting for selected saga sets. This reduces Quizzes count-only selected-space totals while preserving configured conflict-anchor order cases under static evidence.

Remaining gaps:

- Exact aggregate-instance key binding is incomplete.
- The remaining 32 Quizzes sagas without accepted static inputs are not yet classified.
- Event payload reconstruction is incomplete; event-origin inputs may be statically accepted while blocked for materialization by payload placeholders.
- Segment-compressed scheduling remains static evidence; it does not prove semantic completeness or exact runtime aggregate-instance binding.
- Catalog entries are not yet generally executable by a runtime runner; the current executor supports only materializable saga/local single-saga plans and explicit materializable multi-saga deterministic interleavings.

## Stage 1.5 — Dynamic evidence bridge

### Target

Before building a generic executor, the simulator can emit opt-in runtime evidence that confirms which concrete aggregate instances real application executions touched. This bridge should help convert conservative static/type-only footprints into exact key bindings without replacing the static catalog contract.

### Current status

Implemented as a verifier-orchestrated local/sagas bridge with sidecar enrichment:

- simulator dynamic evidence remains disabled by default (`simulator.dynamic-evidence.enabled=false`, `simulator.dynamic-evidence.test-context.enabled=false`);
- verifier dynamic enrichment remains disabled by default (`verifiers.dynamic-enrichment.enabled=false`);
- when enabled, the verifier runs a Maven batch over the selected test classes, passing:
  - `-Dsimulator.dynamic-evidence.enabled=true`
  - `-Dsimulator.dynamic-evidence.test-context.enabled=true`
  - `-Djunit.platform.listeners.autodetection.enabled=true`
  - a run-local dynamic-evidence output directory;
- the batch writes run-level runtime artifacts (`dynamic-evidence.jsonl`, `dynamic-evidence-manifest.json`, `dynamic-input-map.json`, `test-run.json`, `maven-output.log`) under `<run-dir>/dynamic-evidence/`;
- dynamic evidence is joined back to the static catalog with conservative statuses (`MATCHED_EXACT`, `MATCHED_HIGH_CONFIDENCE`, `MATCHED_PARTIAL`, `AMBIGUOUS`, `UNMATCHED`, `NOT_COVERED`);
- enriched outputs are sidecar-only (`scenario-catalog-enriched.jsonl`, `scenario-catalog-enriched-manifest.json`, `dynamic-evidence-join-report.json`), keeping `scenario-catalog.jsonl` unchanged;
- the simulator loads the verifier-written `dynamic-input-map.json` and emits `inputVariantId` when current test identity + runtime functionality class FQN + runtime step name resolve to exactly one accepted static input variant;
- before/after measurement is based on join-report status counts: first-pass propagation should increase `MATCHED_EXACT` when runtime evidence carries direct `inputVariantId`, and remaining ambiguity/unmatched records require later runtime refinement;
- Docker `fault-analysis-scenario-gen` enables this full static+dynamic flow with run-relative report path behavior.

The dynamic-enrichment baseline was refreshed after fixture/setup and feature-helper ownership fixes on 2026-06-30:

```text
run: verifiers/target/feature-helper-owner-fix-dynamic-smoke/quizzes-20260630-122219-034/
scenario records: 584
test classes selected/passed/failed: 45 / 43 / 2
dynamicEventsRead: 26815
MATCHED_EXACT: 435
MATCHED_HIGH_CONFIDENCE: 125
AMBIGUOUS: 0
UNMATCHED: 24
unmatchedReasonCounts: FAILED_TEST_CLASS=8, NOT_SELECTED_TEST_CLASS=7, HELPER_OWNER_MISMATCH=0, UNCLASSIFIED=9
```

For comparison, the 2026-06-29 post-event baseline before those ownership fixes had `MATCHED_EXACT=291`, `MATCHED_HIGH_CONFIDENCE=109`, `AMBIGUOUS=0`, and `UNMATCHED=184`.

Dynamic enrichment remains sidecar-only: it may attribute runtime evidence to static variants, but it does not create or redefine static `InputVariant` or `ScenarioPlan` structure.

Remaining gaps:

- Direct runtime `inputVariantId` propagation is still conservative; it does not yet use runtime command payloads, aggregate accesses, literal argument values, or aggregate keys to reduce all residual unmatched candidates.
- The latest Quizzes baseline eliminated ambiguity and reduced unmatched records to `24`; the remaining `UNCLASSIFIED=9` records need triage before a runtime-value matching plan is justified.
- Dynamic Quizzes baselines must run with `SPRING_PROFILES_ACTIVE=test,sagas,local`; without the `test` profile, async `@SpringBootTest` classes can fail before evidence collection due to missing datasource configuration.
- No stream/gRPC/distributed or causal/TCC runtime hooks/parity yet.
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

Implemented for a narrow bounded path only.

A verifier-owned ScenarioExecutor now supports a bounded materializable saga/local path: it can load a catalog/enriched catalog, auto-select supported single-saga scenarios, explicitly select supported single-saga or multi-saga scenarios, validate a default or explicit binary fault vector, materialize only supported inputs, replay the selected schedule, close or compensate participant lifecycle state, and write a standalone v3 participant report. Quizzes smoke covers single-saga fault-vector execution and an explicit multi-saga deterministic interleaving replay with `PARTIAL_COMPENSATED`.

Current accounting distinguishes three different notions: static accepted input coverage, static recipe readiness, and ScenarioExecutor materializability. In the post-event Quizzes count-only run, `acceptedInputVariantCount=584`, `staticRecipeReadyInputVariantCount=0`, and `executorMaterializableInputVariantCount=94` / `executorReadyInputVariantCount=94`; this does not mean all accepted inputs can replay, and event payload placeholders remain blockers.

Generic execution is still not implemented. The current supported path does not cover arbitrary catalog replay, multi-saga auto-selection, non-materializable multi-saga shapes, TCC execution, stream/gRPC/distributed parity, true parallel execution, compensation-step faults, delay/non-binary impairments, impact scoring, GA search, or scenario prioritization.

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
| Groovy input tracing | Implemented / evolving | Groovy constructor trace specs including event-origin traces | Runtime materialization and event payload reconstruction not complete |
| Facade/event-origin recipe extraction | Implemented / evolving | dummyapp bridge specs and post-event Quizzes count-only accounting | Remaining 32 sagas without accepted inputs unclassified; other event shapes need evidence |
| HTML report | Implemented | renderer/application specs | Human view only, not machine contract |
| Scenario catalog JSONL/manifest | Implemented MVP | scenario package, writer, application specs | Exact keys, filters, executor integration |
| Segment-compressed scheduling/accounting | Implemented static reduction | scheduler/accounting specs, dummyapp integration, Quizzes count-only comparison | Exact aggregate-instance binding and runtime semantic completeness remain separate |
| Dynamic evidence + sidecar enrichment | Implemented MVP with refreshed ownership-aware baseline | simulator dynamic-evidence package, verifier orchestrator, input-map sidecar, 2026-06-30 Quizzes baseline (`MATCHED_EXACT=435`, `MATCHED_HIGH_CONFIDENCE=125`, `AMBIGUOUS=0`, `UNMATCHED=24`) | Triage residual unmatched records; runtime attribution refinement only where justified; stream/gRPC/distributed/TCC parity |
| Quizzes orchestration smoke baseline | Implemented | 2026-06-29 full/default dynamic baseline in `current-state.md` / `evidence.md` | Refresh periodically and track exact/ambiguous/unmatched trends |
| ScenarioExecutor | Implemented for bounded saga/local fault-vector path / materializability accounting evolving | `scenario-executor.md`, Quizzes single-saga default/explicit-vector smokes, Quizzes explicit multi-saga deterministic replay smoke, post-event count-only accounting (`94` materializable, `0` static recipe-ready) | Event payload materialization, general catalog replay, multi-saga auto-selection/non-materializable shapes, broader runtime parity, impact scoring |
| Behavior CSV generation | Not implemented | none | Decide whether adapter or canonical contract |
| Impact scoring | Not implemented | none | Requires executable scenarios |
| GA search | Not implemented | none | Requires impact scoring |
| Bandit prioritization | Not implemented | none | Requires scenario-level rewards |

## Near-term milestone proposal

1. Finalize and classify the remaining 32 Quizzes sagas without accepted static inputs.
2. Triage the refreshed dynamic baseline's `UNMATCHED=24` records, especially `UNCLASSIFIED=9`, before deciding whether runtime-value/aggregate-key matching is worth improving before executor work.
3. Improve event payload reconstruction and materialization/replay for event-origin inputs.
4. Improve aggregate-instance key binding where it affects scenario usefulness.
5. Extend the current ScenarioExecutor beyond the supported saga/local deterministic replay path only when materialization semantics are clearer.
6. Add first impact metrics.
7. Add local GA search for a fixed scenario.
8. Add scenario prioritization after scenario execution and impact scoring exist.
