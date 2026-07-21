# Verifier and scenario-generation roadmap

This roadmap describes the planned pipeline around the verifier module. It maps current static-analysis work to the later execution and search stages.

## Goal

Automate fault-scenario discovery for saga-based microservice applications by progressing from static scenario synthesis to executable scenarios, impact scoring, and search-based prioritization.

The implementation should remain application-agnostic: agents and algorithms should use workflow structure, step footprints, aggregate interactions, inputs, and execution feedback rather than hardcoded domain rules.

## Pipeline overview

1. **Static scenario synthesis** — analyse source/tests and generate WorkloadPlans plus bounded FaultScenarios.
2. **Dynamic evidence bridge** — optionally collect simulator JSONL evidence and attach workload-linked sidecars without changing package identity.
3. **Scenario execution** — materialize inputs and replay one persisted FaultScenario action schedule.
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
- A deterministic five-artifact v3 package for later execution.

### Current status

Implemented as a bounded static-analysis and compensation-aware package pipeline.

Implemented:

- Static extraction of sagas, steps, dependencies, dispatch footprints, compensation evidence, command-handler/service access policies, Groovy construction recipes, facade calls, and event-origin inputs for the implemented `EventHandling`/`EventProcessing` shape.
- Deterministic v3 package: WorkloadPlan JSONL, FaultScenario JSONL, rejected-input diagnostics, exact accounting, and manifest.
- Conservative confidence labels for aggregate-key and compensation evidence.
- Bounded defaults to avoid large-app combinatorial explosion.
- Conflict-anchor segment-compressed forward scheduling and separate bounded recovery-schedule generation.
- Materializability/admissibility classification, eager all-zero/single-point FaultScenarios, and atomic on-demand multi-fault persistence.
- Exact BigInteger recovery accounting for computed vectors without materializing the uncapped schedule space.

Remaining gaps:

- Exact aggregate-instance key binding is incomplete.
- The remaining 32 Quizzes sagas without accepted static inputs are not yet classified.
- Event payload reconstruction is incomplete; event-origin inputs may be statically accepted while blocked for materialization by payload placeholders.
- Segment-compressed scheduling remains static evidence; it does not prove semantic completeness or exact runtime aggregate-instance binding.
- The current executor supports only persisted materializable saga/local FaultScenarios and deterministic sequential action replay; package presence does not imply runtime success for every workload.

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
- v3 enriched outputs are sidecar-only (`workload-dynamic-evidence.jsonl`, `workload-dynamic-evidence-manifest.json`, `dynamic-evidence-join-report.json`), keeping all five semantic package artifacts unchanged;
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

Dynamic enrichment remains sidecar-only: it may attribute runtime evidence to static inputs and WorkloadPlans, but it does not create or redefine InputVariants, WorkloadPlans, FaultScenarios, assigned vectors, or action schedules. The broad Quizzes counts above are a historical v2-era attribution baseline; current v3 compatibility is regression-verified against dummyapp, but a fresh broad Quizzes v3 enrichment baseline has not yet been recorded.

Remaining gaps:

- Direct runtime `inputVariantId` propagation is still conservative; it does not yet use runtime command payloads, aggregate accesses, literal argument values, or aggregate keys to reduce all residual unmatched candidates.
- The latest Quizzes baseline eliminated ambiguity and reduced unmatched records to `24`; the remaining `UNCLASSIFIED=9` records need triage before a runtime-value matching plan is justified.
- Dynamic Quizzes baselines must run with `SPRING_PROFILES_ACTIVE=test,sagas,local`; without the `test` profile, async `@SpringBootTest` classes can fail before evidence collection due to missing datasource configuration.
- No stream/gRPC/distributed or causal/TCC runtime hooks/parity yet.
- Logs and Jaeger traces remain auxiliary diagnostics rather than the primary evidence source.

## Stage 2 — Scenario execution / generic runner

### Target

The runtime side should:

1. Load a complete v3 package and one persisted FaultScenario id.
2. Materialize required inputs and runtime dependencies.
3. Instantiate Saga/functionality objects.
4. Replay persisted `FORWARD` and `COMPENSATION` actions in order.
5. Apply the FaultScenario's persisted assigned vector at forward fault slots.
6. Produce action-, lifecycle-, and participant-aware execution evidence.

### Current status

Implemented for a narrow bounded path only.

A verifier-owned ScenarioExecutor now loads a complete v3 package and exactly one persisted FaultScenario id, materializes supported saga/local participants, and sequentially replays the persisted action schedule. Assigned faults follow persisted recovery ordering. Zero-bit domain/simulator failures use immediate checkpoint recovery and survivor continuation as a reported `DEVIATED` fallback; executor/infrastructure and compensation failures hard-stop. The standalone action-aware report schema is v4. Runtime vector overlays and auto-selection are unsupported.

Current accounting distinguishes static recipe readiness from workload-level ScenarioExecutor materializability/admissibility. That policy gates eager FaultScenario generation but does not prove domain success: the 2026-07-20 Quizzes smoke selected a materializable compensation-interleaving FaultScenario and then honestly reported a zero-bit null-input failure and fallback.

Generic execution is still not implemented. The supported path does not cover arbitrary/non-materializable package shapes, TCC execution, stream/gRPC/distributed parity, true parallel execution, compensation faults, delay/non-binary impairments, automatic recovery retries, impact scoring, GA search, or prioritization.

Current static work prepares for the broader stage by preserving:

- source expression/provenance text;
- replay-oriented value recipes;
- expected constructor argument types;
- unresolved placeholder categories;
- deterministic WorkloadPlan, fault-slot, checkpoint, FaultScenario, and action identifiers.

Open design questions:

- How should persistent-environment reset be orchestrated across repeated FaultScenario executions?
- Is behavior CSV still useful as a compatibility adapter even though it is not the v3 executor contract?
- How much additional runtime dependency materialization can remain application-agnostic?

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
| Compensation-aware v3 package | Implemented | WorkloadPlan/FaultScenario generators, writer/reader, manifest, accounting, dummyapp specs, bounded Quizzes smoke | Exact aggregate keys and broader materializable input coverage |
| Segment-compressed scheduling/accounting | Implemented static reduction | scheduler/accounting specs, dummyapp integration, Quizzes count-only comparison | Exact aggregate-instance binding and runtime semantic completeness remain separate |
| Dynamic evidence + sidecar enrichment | Implemented v3 workload-linked sidecar; broad Quizzes counts historical | simulator hooks, verifier orchestrator, dummyapp package-immutability integration, historical 2026-06-30 Quizzes baseline | Fresh broad Quizzes v3 baseline; residual unmatched triage; stream/gRPC/distributed/TCC parity |
| Quizzes orchestration smoke baseline | Implemented | 2026-06-29 full/default dynamic baseline in `current-state.md` / `evidence.md` | Refresh periodically and track exact/ambiguous/unmatched trends |
| ScenarioExecutor | Implemented for one persisted materializable saga/local FaultScenario | `scenario-executor.md`, v4 executor specs, 2026-07-20 Quizzes compensation-interleaving smoke and package hashes | Better input materialization, broader workload shapes/runtime parity, reset orchestration, impact scoring |
| Behavior CSV generation | Not implemented | none | Decide whether adapter or canonical contract |
| Impact scoring | Not implemented | none | Requires executable scenarios |
| GA search | Not implemented | none | Requires impact scoring |
| Bandit prioritization | Not implemented | none | Requires scenario-level rewards |

## Near-term milestone proposal

1. Finalize and classify the remaining 32 Quizzes sagas without accepted static inputs.
2. Triage the refreshed dynamic baseline's `UNMATCHED=24` records, especially `UNCLASSIFIED=9`, before deciding whether runtime-value/aggregate-key matching is worth improving before executor work.
3. Improve event payload reconstruction and materialization/replay for event-origin inputs.
4. Refresh a representative Quizzes dynamic-enrichment baseline against the v3 workload sidecar.
5. Improve aggregate-instance key binding where it affects WorkloadPlan usefulness.
6. Add first impact metrics before broadening runtime/search scope.
7. Add local GA search over on-demand persisted vectors for a fixed WorkloadPlan.
8. Add scenario prioritization after execution and impact scoring exist.
