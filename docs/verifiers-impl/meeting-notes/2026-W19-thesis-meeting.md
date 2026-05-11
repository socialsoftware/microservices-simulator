# Thesis meeting notes — 2026-W19

Date: 2026-05-06

Purpose: weekly thesis/advisor meeting notes, focused on the progress after the previous meeting's scenario-catalog milestone.

## One-sentence update

Since the previous meeting, the verifier gained an opt-in hybrid static/dynamic evidence bridge: it can export the static scenario catalog, run selected application tests with simulator runtime evidence enabled, and write sidecar enriched catalog artifacts showing which generated scenarios are supported, ambiguous, or unmatched by runtime evidence.

## Simple thesis narrative

Last week the main message was:

> "the verifier can propose bounded, structured fault-injection experiments to run later."

This week the message is:

> "before executing generated fault scenarios, the tool can now observe real application executions and use that evidence to reduce uncertainty in the generated catalog."

Current pipeline narrative:

1. Parse application source and Groovy tests.
2. Extract saga structure, steps, command dispatches, and aggregate read/write effects.
3. Export a bounded static `scenario-catalog.jsonl` plus manifest.
4. Select relevant application tests for dynamic enrichment.
5. Run those tests one class at a time with simulator dynamic evidence enabled.
6. Collect per-test runtime artifacts: JSONL evidence, manifest, `test-run.json`, and Maven logs.
7. Join runtime evidence back to the static catalog as sidecar artifacts.
8. Next: reduce ambiguity, improve exact key binding, and define the minimal `ScenarioExecutor` contract.

Important framing:

> The dynamic bridge is not the `ScenarioExecutor`. It is an evidence-collection and enrichment stage that makes the exact aggregate-key problem measurable before runtime fault injection is implemented.

## Context from the previous meeting

The previous meeting established the static-analysis baseline:

- better Groovy/test input tracing;
- improved HTML report diagnostics;
- interface-based command-handler dispatch improvement;
- first machine-readable scenario catalog;
- documentation/knowledge migration;
- the open problem of exact aggregate-instance key binding.

This week's progress builds on that baseline. The main new contribution is the dynamic evidence bridge and the first validation of static/dynamic enrichment on Quizzes.

## What changed since the previous meeting

### 1. Chose and documented the hybrid static + dynamic direction

The advisor's suggestion was to use runtime information such as logs, traces, or test executions to help identify values that static analysis cannot reliably infer.

What was clarified:

- Existing logs and Jaeger traces are useful diagnostics, but not reliable enough as the primary exact-key mechanism.
- The preferred direction is structured simulator/runtime events first, with logs/traces as auxiliary evidence.
- Static analysis still decides what is relevant; dynamic analysis should enrich, not replace, the static catalog.

Why logs/traces alone are not enough:

- Current JSON logs mostly store domain information in free-text `message` fields rather than structured fields.
- Logs are inconsistent across handlers: some include IDs clearly, while others print DTO object references or only partial context.
- Logs do not consistently carry source anchors, constructor/input provenance, or scenario/input identifiers.
- OpenTelemetry traces expose useful hierarchy such as root → functionality → step, but they do not consistently include command fields, DTO values, aggregate IDs, or static source locations.
- Free-text log parsing would be brittle and application-specific, which conflicts with the goal of keeping the verifier generic.

Useful examples still exist:

```text
START EXECUTION STEP: getCourseExecutionStep with from functionality CreateTournamentFunctionalitySagas
Getting course execution by id: 2
Loaded and registered read for aggregate ID: 2 - SagaExecution
```

This supports the intuition that runtime evidence is valuable, but the stronger design is to emit a structured event directly at simulator hook points instead of relying on the log message format.

Why this matters:

> This avoids building a brittle full interprocedural static value-propagation engine before proving the thesis pipeline, while still keeping the process explainable and conservative.

Advisor-facing wording:

> Existing logs/traces expose useful step and aggregate information, but too much data is free-text or inconsistently structured. The implemented direction is therefore a small structured evidence path at simulator hook points.

### 2. Added simulator-level dynamic evidence artifacts

The simulator can now emit dynamic evidence when explicitly enabled.

Current behavior:

- Disabled by default: `simulator.dynamic-evidence.enabled=false`.
- Enabled runs write:
  - `dynamic-evidence.jsonl`
  - `dynamic-evidence-manifest.json`
- Initial event kinds:
  - `STEP_STARTED`
  - `COMMAND_SENT`
  - `AGGREGATE_ACCESSED`
  - `STEP_FINISHED`

Why these event kinds:

| Event kind | Why it exists | Example evidence it provides |
|---|---|---|
| `STEP_STARTED` | Establishes the current saga/functionality step context before commands and aggregate accesses happen. | `CreateTournamentFunctionalitySagas/getCourseExecutionStep` started. |
| `COMMAND_SENT` | Records the command leaving the saga step through the local command gateway. This helps connect step-level execution to command-handler behavior. | `GetCourseExecutionByIdCommand` sent to service `execution`, with root aggregate id when available. |
| `AGGREGATE_ACCESSED` | Records the concrete aggregate type/id read or written by the unit-of-work service. This is the most direct evidence for exact aggregate-key binding. | `READ SagaExecution(2)` from `SagaUnitOfWorkService.aggregateLoadAndRegisterRead`. |
| `STEP_FINISHED` | Closes the step context and records whether the step completed successfully or with an error. | `getCourseExecutionStep` finished with `SUCCESS`. |

Together, the four events give a minimal structured chain:

```text
saga step started
  -> command sent
  -> aggregate read/write observed
  -> saga step finished
```

This is enough for the first enrichment slice because it connects the static entities already known by the verifier — saga, step, command, aggregate footprint — to runtime evidence.

Instrumentation points:

- `ExecutionPlan` step boundaries;
- `LocalCommandGateway.send(Command)`;
- `SagaUnitOfWorkService` aggregate read/write hooks.

Concrete Quizzes smoke evidence:

- `CreateTournamentFunctionalitySagas/getCourseExecutionStep` emits step, command, and aggregate-read evidence.
- The smoke observes `AGGREGATE_ACCESSED` for `SagaExecution(2)`.
- Disabled-mode smoke confirms no dynamic evidence artifacts are written when the observer is off.

Why this matters:

> The verifier now has structured runtime observations for exact aggregate access evidence, instead of depending on parsing logs or manually inspecting traces.

### 3. Added verifier-side dynamic enrichment and sidecar outputs

The verifier can now run a static catalog export and then perform a dynamic-enrichment pass.

What changed:

- Added disabled-by-default `verifiers.dynamic-enrichment` configuration.
- Added test-class discovery with include/exclude directories and self-test exclusions.
- Added Maven orchestration that runs selected application test classes one by one.
- Each test class gets its own evidence directory under the verifier run directory.
- Added reader/joiner/writer components for sidecar enrichment.

The enrichment pass is deliberately staged:

1. The verifier first builds the ordinary static scenario catalog.
2. It discovers relevant application test classes using configured include/exclude directories.
3. It runs each test class with dynamic evidence and test-context capture enabled.
4. It reads all emitted `dynamic-evidence.jsonl` files.
5. It joins runtime events back to static `ScenarioPlan` records using test identity, saga/functionality name, and step name evidence.
6. It writes enriched sidecars without mutating the static catalog.

Dynamic enrichment writes, in the same verifier run directory:

- `scenario-catalog.jsonl` — unchanged static contract;
- `scenario-catalog-manifest.json`;
- `scenario-catalog-rejected-inputs.jsonl`;
- `dynamic-evidence/<safe-test-class-fqn>/dynamic-evidence.jsonl`;
- `dynamic-evidence/<safe-test-class-fqn>/dynamic-evidence-manifest.json`;
- `dynamic-evidence/<safe-test-class-fqn>/test-run.json`;
- `dynamic-evidence/<safe-test-class-fqn>/maven-output.log`;
- `scenario-catalog-enriched.jsonl`;
- `scenario-catalog-enriched-manifest.json`;
- `dynamic-evidence-join-report.json`.

Join statuses:

- `MATCHED_EXACT`
- `MATCHED_HIGH_CONFIDENCE`
- `MATCHED_PARTIAL`
- `AMBIGUOUS`
- `UNMATCHED`
- `NOT_COVERED`

Meaning of the join statuses:

| Status | Meaning | How to interpret it |
|---|---|---|
| `MATCHED_EXACT` | Runtime evidence explicitly carries the generated input/scenario identifier needed for a direct match. | Strongest future target; not common yet because direct runtime `inputVariantId` propagation is not implemented. |
| `MATCHED_HIGH_CONFIDENCE` | Runtime evidence has complete test identity and matches the static scenario through saga/functionality and step evidence, with a single matching input candidate. | Good enrichment evidence, but still not the same as direct ID propagation. |
| `MATCHED_PARTIAL` | Runtime evidence matches semantically but lacks complete test identity. | Useful diagnostic evidence, but weaker provenance. |
| `AMBIGUOUS` | Runtime evidence could match multiple scenario/input candidates. | The evidence is real, but the join is not precise enough to pick one scenario confidently. |
| `UNMATCHED` | Runtime evidence exists, but it does not match the scenario's expected input/step identity. | Indicates either unrelated coverage or a matching gap. |
| `NOT_COVERED` | No relevant dynamic evidence was observed for the scenario. | The selected tests did not exercise that scenario path, or evidence collection did not capture it. |

Why the statuses matter:

> They prevent the tool from overclaiming exactness. Runtime evidence can strengthen a scenario, expose ambiguity, or show that the current test selection does not cover the generated candidate.

Key design boundary:

> The original `scenario-catalog.jsonl` remains unchanged. Runtime enrichment is sidecar-only so the static contract stays reproducible and auditable.

Why this matters:

> The verifier can now compare what static analysis predicted with what selected tests actually exercised, without pretending that runtime evidence is always complete or exact.

### 4. Validated the dynamic bridge on dummyapp and Quizzes

Dummyapp validation:

- Added an integration spec that builds dummyapp static analysis state, synthesizes dynamic evidence, and writes enriched sidecars end-to-end.
- This proved `MATCHED_HIGH_CONFIDENCE` and `NOT_COVERED` paths before running real Quizzes tests.

Narrow Quizzes orchestration smoke:

- `runStatus=COMPLETE`
- `testClassesSelected=2`
- `testClassesPassed=2`
- `testClassesFailed=0`
- `dynamicEventsRead=362`
- `eventsMissingTestContext=0`
- join counts:
  - `MATCHED_HIGH_CONFIDENCE=1`
  - `UNMATCHED=199`

Full/default sagas-only Quizzes attempt:

- `runStatus=PARTIAL`
- `testClassesSelected=42`
- `testClassesPassed=40`
- `testClassesFailed=2`
- `evidenceFilesRead=42`
- `dynamicEventsRead=18868`
- `eventsMissingTestContext=0`
- join counts:
  - `MATCHED_HIGH_CONFIDENCE=2`
  - `AMBIGUOUS=44`
  - `UNMATCHED=20`
  - `NOT_COVERED=0`

Failure interpretation:

- The two failed Quizzes classes were re-run without dynamic enrichment instrumentation.
- They reproduced the same `Expected SimulatorException, but got CompletionException` pattern.
- Current interpretation: these are existing test/runtime failures rather than dynamic-enrichment regressions.

Why this matters:

> The pipeline now works on a real case-study selection, and the full run exposes concrete ambiguity/warning numbers that can guide the next precision improvements.

### 5. Cleaned saga catalog inputs with source-mode filtering

This complements dynamic enrichment by keeping causal/TCC-origin inputs out of saga catalogs.

Problem:

Quizzes reuses many façade/functionality entry points across transactional modes. A Groovy test call such as:

```groovy
tournamentFunctionalities.createTournament(...)
```

is not enough by itself to know whether the call will create a saga-mode or causal/TCC-mode workflow. A saga catalog should not silently accept inputs that came from causal/TCC tests.

How filtering works:

- Groovy test source is indexed for generic simulator/Spring evidence, not Quizzes-specific package hacks.
- The classifier looks for evidence such as:
  - saga versus causal unit-of-work services;
  - saga versus causal bean/test configuration;
  - active profiles and test properties;
  - creation-site evidence where available.
- Each traced input receives source-mode metadata:
  - `SAGAS`
  - `TCC`
  - `MIXED`
  - `UNKNOWN`
- Saga-catalog policy is applied before scenario planning:
  - `SAGAS` inputs are accepted;
  - `TCC` inputs are rejected from the accepted saga catalog;
  - `MIXED` inputs are rejected;
  - `UNKNOWN` inputs are accepted with a warning, to avoid losing coverage when evidence is absent.
- Rejected inputs are still exported to `scenario-catalog-rejected-inputs.jsonl` with reasons and evidence.

Why this is generic:

> The classifier uses simulator/Spring evidence such as unit-of-work services, bean configurations, and profiles. It does not rely on hardcoded Quizzes test names as the primary mechanism.

Latest bounded Quizzes catalog smoke:

- `inputVariantsAdapted=549`
- `inputVariantsAccepted=468`
- `inputVariantsRejectedBySourceMode=69`
- `inputVariantsExcludedByPolicy=12`
- `scenariosExported=468`
- HTML raw trace source modes before catalog dedup/filtering:
  - `SAGAS=1323`
  - `TCC=258`
- accepted source modes:
  - `SAGAS=468`
  - `TCC=0`
  - `MIXED=0`
  - `UNKNOWN=0`

Interpretation:

> Compared with the previous no-source-mode-filter interpretation, the classifier removes 69 causal/TCC-derived inputs from the accepted saga catalog while preserving them in rejected-input diagnostics.

### 6. Updated Docker/runtime packaging for the verifier flow

The `fault-analysis-scenario-gen` Docker service can now run the static + dynamic flow.

What changed:

- Final verifier image uses Maven with Java 21 so it can launch application tests from inside the container.
- Outputs are written under `/reports`.
- Scenario catalog export and dynamic enrichment are enabled in the compose service.
- Sagas-only Quizzes selection is configured while causal/TCC directories are excluded.

Validation recorded:

- `docker compose build fault-analysis-scenario-gen` — pass.
- `docker compose run --rm --no-deps --entrypoint mvn fault-analysis-scenario-gen -version` — pass.

Why this matters:

> The verifier runtime now has the pieces needed for reproducible static + dynamic report generation outside the IDE/local Maven session.

## What not to overclaim

Be explicit that this is still not runtime fault injection.

Current limitations:

- `ScenarioExecutor` is not implemented yet.
- Runtime saga/functionality materialization from catalog recipes is not implemented yet.
- Runtime fault injection from generated schedules is not implemented yet.
- Behavior CSV generation is not implemented yet.
- Execution impact scoring is not implemented yet.
- GA search and bandit/scenario prioritization are not implemented yet.
- Exact aggregate-instance key extraction is still incomplete.
- Dynamic enrichment currently correlates by test identity plus saga/functionality/step evidence; direct runtime `inputVariantId` propagation is not implemented.
- Dynamic evidence currently targets local/sagas paths only.
- No stream/gRPC/distributed runtime parity yet.
- No causal/TCC runtime parity yet.
- The full/default Quizzes run has high ambiguity/warning volume: `AMBIGUOUS=44`, `warningCount=8238`.
- In the current enriched sidecars, `matchedTestExecutions[].testRunStatus` is still `null` even though the join report records per-class statuses.

Advisor-facing wording:

> This week did not produce the executor yet. It produced the evidence bridge needed before the executor, so the verifier can say which static scenarios are supported by actual runtime observations and where ambiguity remains.

## Suggested 1-minute verbal update

Since the last meeting, the work focused on the exact-key problem discussed previously. Rather than relying only on static propagation or parsing free-text logs, the simulator now has an opt-in dynamic evidence bridge. When enabled, it writes structured JSONL events for step starts/finishes, commands sent, and aggregate reads/writes. The verifier can then run selected application tests, collect those evidence files, and join them back to the static scenario catalog as sidecar enriched artifacts. The static catalog remains unchanged as the reproducible contract. On Quizzes, a narrow run completed with two selected tests and 362 dynamic events, producing one high-confidence match. A fuller sagas-only run selected 42 classes, passed 40, collected 18,868 dynamic events, and exposed 44 ambiguous joins. So the bridge works, but the next step is to reduce ambiguity and decide how much precision is required before implementing the first `ScenarioExecutor`.

## Future plan for this week

### Priority 1 — Reduce dynamic-enrichment ambiguity

Goal:

> Make the full Quizzes enrichment output more useful by reducing `AMBIGUOUS=44` and the repeated warning volume without inventing false exactness.

Possible work:

- tighten static/dynamic correlation heuristics;
- improve candidate pruning;
- reduce repeated ambiguity warnings;
- keep conservative statuses when evidence is insufficient.

Advisor-facing wording:

> The dynamic bridge already surfaces ambiguity. The next step is to distinguish real ambiguity from avoidable matching noise.

### Priority 2 — Tighten sidecar metadata completeness

Goal:

> Make enriched records self-contained enough for later analysis tools.

Specific issue:

- `matchedTestExecutions[].testRunStatus` is currently `null` in enriched records.
- The join report already has per-class run statuses.

Advisor-facing wording:

> The metadata exists, but it should be propagated into the per-match records so consumers do not have to cross-reference the join report manually.

### Priority 3 — Decide minimal `ScenarioExecutor` contract

Goal:

> Decide how generated scenarios become runtime executions.

Main open question:

- Should the executor consume `scenario-catalog.jsonl` directly?
- Or should the verifier generate existing simulator behavior CSV files as an adapter format?

Suggested position:

> JSONL should remain the primary contract, with CSV as an adapter if needed, but this should be confirmed before implementation.

### Priority 4 — Improve exact aggregate-key binding

Goal:

> Connect input variants and runtime observations more directly to concrete aggregate instance keys.

Potential direction:

- direct runtime `inputVariantId` propagation;
- stronger mapping from static input variants to observed steps/commands/aggregate accesses;
- richer exact/symbolic logical key bindings in enriched sidecars.

Advisor-facing wording:

> The current bridge can observe `SagaExecution(2)` at runtime, but the stronger goal is to systematically connect those observations back to the specific generated input variant and scenario record.

### Priority 5 — Implement single-saga execution only after the contract is clear

Goal:

> Start execution with the simplest useful case: one generated saga scenario, controlled step/fault behavior, and observable result artifacts.

Why single-saga first:

> It validates input materialization, saga instantiation, step mapping, and fault-slot semantics before adding multi-saga interleavings.

Later steps:

- bounded multi-saga schedules;
- runtime fault injection;
- impact scoring;
- GA search;
- bandit/scenario prioritization.

## Questions for the advisor

1. Is the sidecar dynamic-enrichment approach acceptable, or should runtime evidence modify the canonical catalog directly?
2. Should the next sprint focus on ambiguity reduction, or on defining/starting the minimal `ScenarioExecutor`?
3. What level of exact aggregate-key confidence is enough before running multi-saga experiments?
4. Should JSONL remain the primary executor contract, with behavior CSV only as an adapter?
5. Is partial-mode Quizzes enrichment acceptable if failing classes are documented and reproduce independently without instrumentation?
6. Which impact signal should be implemented first once execution exists: invariant violations, unhandled exceptions, compensation failures, state divergence, latency, or a combination?

## Simple explanation of the dynamic evidence bridge

Short version:

> Static analysis says what might happen; dynamic evidence records what did happen during selected tests.

Example:

Static analysis may know:

```text
CreateTournamentFunctionalitySagas/getCourseExecutionStep reads SagaExecution
```

But it may not know the exact runtime ID. Dynamic evidence can record:

```text
During CreateTournamentFunctionalitySagas/getCourseExecutionStep,
the simulator loaded SagaExecution(2)
```

The enrichment stage then attaches that observation to generated scenario records with a confidence/status label.

Why the label matters:

- `MATCHED_HIGH_CONFIDENCE` means the runtime evidence lines up with the scenario through strong identity/step evidence.
- `AMBIGUOUS` means multiple scenario/input candidates still match the same evidence.
- `UNMATCHED` means evidence exists, but not enough to match that scenario.
- `NOT_COVERED` means no relevant dynamic evidence was observed for that scenario.

Advisor-facing explanation:

> This gives us a concrete way to measure where static scenario generation is already supported by runtime behavior and where the mapping is still too ambiguous for strong claims.

## Suggested meeting close

Good closing position:

> The static scenario catalog is now complemented by a runtime evidence bridge. The bridge works on Quizzes, and the next research decision is whether to spend the next iteration improving precision/ambiguity first or to start the minimal executor contract.
