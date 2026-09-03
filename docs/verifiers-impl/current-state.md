# Verifier current state

Last updated: 2026-09-02

This is the canonical handbook for verifier and fault-analysis scenario work. It owns the current conceptual model, terminology, supported operations, latest representative evidence, reproduction commands, and limitations. [`roadmap.md`](roadmap.md) owns future direction. [`decisions/`](decisions/index.md) explains the few design choices whose rationale is not obvious from current behavior.

## The short version

The verifier turns Saga application source and tests into deterministic fault experiments:

```text
Java application code + Groovy/Spock tests
  -> static Saga, step, interaction, and input extraction
  -> WorkloadPlans
  -> FaultScenarios
  -> optional runtime-evidence attribution
  -> prerequisite-provider or source-derived ordered setup
  -> setup preflight or one-scenario execution
  -> optional invariant-impact result
```

The verifier does **not** prove that an application is correct. It currently answers narrower questions:

1. Which Sagas, steps, aggregate footprints, and test-derived inputs can be found?
2. Which reproducible workload and fault schedules can be generated from those facts?
3. Which generated inputs are static setup candidates, and which can actually start in the current Saga/local runtime?
4. What happened when one persisted FaultScenario was replayed?
5. Did that attempt trigger an observed aggregate-invariant rejection?
6. For the bounded Quizzes RemoveTournament–AddParticipant benchmark, did final state satisfy its explicit broken-reference rule?

The fresh final-code Quizzes qualification reports:

```text
discovered Sagas:                    68
Sagas with / without accepted input: 36 / 32
accepted / rejected inputs:          794 / 90
materializable / blocked inputs:     91 / 703
strict connected Saga sets (2 / 3):  382 / 3594
strict sets with positive input tuples: 35 / 42
```

A separate bounded source-derived package contains one reusable 12-action setup, one RemoveTournament/AddParticipant WorkloadPlan, and 14 FaultScenarios after one persisted `10100` request. Its Docker preflight is `SETUP_READY`; replay of that requested scenario completed `PARTIAL_COMPENSATED / EXACT`. These are bounded qualification facts, not generic execution coverage.

## Reading order

Use this page by question:

- [What the main terms mean](#the-essential-terms)
- [What inputs are analyzed](#inputs-and-static-extraction)
- [What the current package contains](#the-current-package)
- [What the accounting metrics mean](#how-to-read-accounting)
- [What dynamic enrichment contributes](#optional-dynamic-evidence)
- [How setup preflight and execution differ](#scenarioexecutor)
- [What ImpactV1 measures](#impactv1)
- [Latest commands and evidence](#current-evidence)
- [Current limitations](#current-limitations)
- [What is not implemented](#not-implemented)

The generated JSONL catalogs are machine contracts, not documents intended to be read front-to-back. Start with `scenario-catalog-manifest.json`; inspect a catalog or diagnostic file only for a specific workload, fault scenario, or blocker.

## The essential terms

### Accepted input

An **InputVariant** is one source/test-derived way to invoke a Saga or functionality. It preserves where the input came from, who may own it during runtime attribution, its source mode, and a structured recipe for its constructor arguments.

An accepted input passed source-mode and configured input-policy filtering. Acceptance does **not** prove that the input can be materialized or executed.

### Static setup candidate

A **static setup candidate** is a WorkloadPlan whose accepted inputs pass the current deterministic input-readiness checks and whose structure is admissible to the Saga/local executor.

Current input facts, setup records, WorkloadPlans, and accounting preserve the materializability decision and blockers. Read `materializable` as **setup candidate**, not runtime proof. Eager all-zero and single-point FaultScenario generation uses this gate.

### Runtime setup-ready

A workload is **setup-ready** only when ScenarioExecutor, in a real application context, materializes the exact persisted argument tuple and starts every exact Saga participant. Setup preflight reports this as `SETUP_READY`.

Preflight deliberately runs no target forward, fault, compensation, or commit action. Setup-ready therefore predicts neither domain success nor fault behavior.

### SetupPlan

A **SetupPlan** is a reusable preparation record referenced by WorkloadPlans. A source-derived setup stores validated application-facade calls in source order and binds participant arguments to earlier setup results or approved properties. A provider-backed setup stores the provider identity and typed binding requirements. Setup actions run outside target fault injection and measured impact; attempt-local runtime ids and values never enter package identity.

### WorkloadPlan

A **WorkloadPlan** references participant input facts, an optional reusable setup, direct interactions, and one ordered schedule. Participants and schedule occurrences use short plan-local ids. Step occurrences reference exact Saga-local step ids such as `getUserStep#0`; event occurrences reference a Saga-local route and triggering step occurrence. Fault slots live on faultable scheduled steps.

It does not own an assigned vector or a recovery order. Those belong to FaultScenario records.

### FaultScenario

A **FaultScenario** is one reproducible experiment. It references one WorkloadPlan and adds:

- one assigned binary vector aligned with the WorkloadPlan fault slots;
- one complete ordered action sequence whose one-key `step`, `event`, and `compensate` references point into the WorkloadPlan schedule;
- a deterministic identity derived from the workload, vector, and action order.

The executor selects a persisted FaultScenario by id. It does not accept an ad hoc runtime vector overlay.

### Execution outcome and schedule conformance

These are intentionally separate:

- **terminal status** says what happened to the attempt, such as `SUCCESS`, `COMPENSATED`, `PARTIAL_COMPENSATED`, or an infrastructure/setup failure;
- **schedule conformance** says how measured execution related to the persisted action order:
  - `EXACT`: the persisted order completed;
  - `DEVIATED`: a supported zero-bit domain fallback changed the order but completed;
  - `INCOMPLETE`: measured execution hard-stopped on a prefix;
  - absent: measured action execution never began.

A compensated execution can be exact. A failed execution can be incomplete. These fields are not aliases.

### Impact

**ImpactV1** is the first narrow domain-impact model:

```text
ImpactV1 = invariantViolationCount
```

It counts structured `INVARIANT_VIOLATION` events emitted when the existing Saga aggregate-write boundary rejects a change through `Aggregate.verifyInvariants()`. It does not infer harm from an assigned fault, abort, or compensation alone.

### Benchmark observation

A **benchmark observation** is an application-side evaluation record joined to one persisted package, FaultScenario, execution attempt, and ImpactV1 report. The current Quizzes RemoveTournament–AddParticipant benchmark applies one bounded final-state predicate: an active Tournament still referring to the observed deleted Quiz is `HARMFUL_FOR_RULE`. A valid observation additionally requires the latest raw persisted Tournament Saga-state column to decode through `SagaStateConverter` as exact `GenericSagaState.NOT_IN_SAGA`; SQL null, missing, malformed, wrong-class, or different-state evidence is `NOT_EVALUATED`. `NO_BROKEN_REFERENCE` means only that the predicate is false after this proof succeeds; it is not a global safety claim.

This observation is application-side test/evaluation evidence. It does not change ImpactV1, production Quizzes behavior, or measured final state. The current automatic benchmark uses source-derived setup; older versioned packages remain historical evidence only and are not accepted by the current reader.

## Inputs and static extraction

### Inputs consumed

Static generation consumes:

- Java production source from a simulator application;
- Groovy/Spock tests and fixtures;
- verifier generation configuration such as source policy, schedule strategy, caps, and seed.

Optional dynamic enrichment additionally runs selected application tests and consumes simulator runtime evidence. ScenarioExecutor consumes one complete current package, the target application classpath/Spring context, and either one persisted FaultScenario id or preflight mode.

The main targets are:

- `applications/dummyapp/`: controlled parser, generation, enrichment, and executor fixtures;
- `applications/quizzes/`: realistic high-complexity smoke and thesis-scale evidence.

### Production-code extraction

The Java visitors discover:

- Saga/functionality classes and constructor signatures;
- ordered workflow steps;
- command dispatches and dispatch phases;
- command-handler targets;
- domain-service aggregate access policies;
- aggregate read/write footprints;
- compensation and rollback evidence;
- functionality/facade creation sites;
- event producers through `Saga step -> command dispatch -> service -> direct registerEvent(new EventType(...))`;
- event consumers through `EventHandling -> EventProcessing -> functionality -> Saga`.

An **event consequence** is the deterministic atomic normal action joining one supported producer emission to one selected consumer route. The producer call must use a directly resolved `UnitOfWorkService` variable, pass the service method's single relevant `UnitOfWork` parameter, and construct the event directly. A step is rejected if any matching emission comes from compensation, even when it also has a supported forward emission. The selected `EventHandling` method must contain exactly one unconditional, unrepeated, direct `handleSubscribedEvent` delegation. The extractor also rejects ambiguous producer dispatch, multiple/repeated/conditional emissions, unresolved routes, non-Saga origins, recursion, and unsupported fan-out instead of guessing. Genuinely distinct globally selected routes remain distinct candidates. Event placement and the selected route participate in workload identity. Event consequences never own fault slots, vector bits, generated compensation, or compensation checkpoints.

Domain services are identified structurally through command-handler dispatch targets rather than package or class-name conventions. This prevents coordination facades from being treated as domain state services. The rationale is retained in [`decisions/2026-04-06-domain-service-vs-coordination-facade.md`](decisions/2026-04-06-domain-service-vs-coordination-facade.md).

Aggregate-key inference follows each command constructor's delegation to the framework `Command` constructor. It identifies the semantic aggregate-id parameter and maps literals, getter chains, and Saga-constructor parameters at the command call site into normalized key evidence. Explicit `null` roots and unsupported expressions remain keyless rather than borrowing another argument. Typed `SagaCommand` wrappers are transparent to forward dispatch analysis. For a generic bare-`Command` compensation, the verifier independently derives the aggregate from the payload's service token through the matching command handler and derives the root from the payload's third argument under the base `Command` root-key contract.

The Quizzes qualification found the same 132 forward command accesses as the baseline and 26 usable generic compensation accesses instead of none. Only two of 134 steps retain focused analysis limitations, down from 573 limitations across all 134 steps: one unresolved `SagaCommand` payload and one unresolved dispatch through a helper `send` call. The static package serializes the 132 forward accesses; compensation evidence remains part of the internal step model rather than being presented as a forward dispatch.

### Test-to-input flow

Quizzes tests often call a functionality facade rather than constructing a Saga directly. The verifier combines Java creation-site analysis with Groovy tracing:

```text
Groovy facade call
  -> Java functionality creation-site mapping
  -> target Saga constructor positions
  -> traced literals, DTOs, helpers, properties, calls, and placeholders
  -> InputVariant
```

For each input, provenance and ownership have different jobs:

- **provenance** records where the analyzer found the value: source class, method, binding, expression, fixture origin, and call context;
- **owners** record which test features may claim the input during runtime attribution.

A helper-created fixture can retain helper provenance while being owned by the feature that used it.

### Structured input recipes

Each input fact owns its compact constructor-argument recipes. WorkloadPlans reference input ids instead of embedding input records. Recipe values include:

- typed literals and collections;
- constructors and ordered setter/property assignments;
- helper results reduced to nested recipes;
- property accesses and supported calls/transforms;
- explicit placeholders;
- typed `baseline_binding` nodes supplied by an exact prerequisite provider;
- unresolved nodes with blockers.

The executor materializes a supported subset. Runtime-owned arguments currently include `SagaUnitOfWorkService`, `CommandGateway`, and a fresh `SagaUnitOfWork`. A baseline binding is materializable only when the persisted provider id/version is present and the provider returns the required key with the persisted exact type. Applications may declare bounded prerequisite workloads in `src/test/resources/verifier-prerequisite-scenarios.json`; descriptor schema `microservices-simulator.prerequisite-scenario-descriptor.v2` requires `selectionKind=EVENT|NO_EVENT`. `EVENT` preserves the exact selected route, while `NO_EVENT` rejects event-route fields and selects only a source-derived workload with no event consequence. The generic adapter resolves named Saga steps without application FQNs in verifier production code. The matching provider may live on the application's test classpath. Unsupported calls and unresolved source values remain blockers.

Recipe readiness, catalog acceptance, static setup candidacy, runtime setup readiness, and successful execution are different stages. Do not collapse them into one “executable” count.

### Source-mode filtering

Quizzes exposes similar facades under Saga and TCC/causal configurations. The verifier classifies source evidence as:

```text
SAGAS | TCC | MIXED | UNKNOWN
```

Current Saga-catalog policy is:

```text
SAGAS   -> accepted
TCC     -> rejected diagnostically
MIXED   -> rejected diagnostically
UNKNOWN -> accepted with warning
```

Rejected inputs remain in `inputs.jsonl` with `accepted=false`, their source and recipe when available, and the applicable rejection reason. TCC execution remains out of scope.

## The current package

`scenario-catalog-manifest.json` is the only entry point. It contains integer `formatVersion: 1` and a `files` object keyed by role; each role supplies only a relative path and SHA-256 hash. Ordinary readers accept this current contract only. Package records do not repeat schema or format versions.

Count-only packages contain exactly these semantic roles:

| Role | File | Ownership |
|---|---|---|
| `accounting` | `accounting.json` | Configuration and aggregate numerical results |
| `sagas` | `sagas.jsonl` | One record per discovered Saga, including Saga-local steps and routes |
| `inputs` | `inputs.jsonl` | One record per extracted input, accepted or rejected; catalog-writing may add provider-bound inputs required by emitted prerequisite workloads |
| `interactions` | `interactions.jsonl` | One direct two-access interaction per record |

Catalog-writing adds:

| Role | File | Ownership |
|---|---|---|
| `setups` | `setups.jsonl` | Reusable source-derived or provider-backed preparation |
| `workloads` | `workloads.jsonl` | Participant input references and one plan-local schedule |
| `faultScenarios` | `fault-scenarios.jsonl` | Vector plus complete action references for one WorkloadPlan |
| `requests` | `requests.jsonl` | Successful on-demand workload/vector/effective-cap requests |

Optional dynamic enrichment adds `dynamicObservations` in `dynamic-observations.jsonl` and `dynamicAttributionLinks` in `dynamic-attribution-links.jsonl`. Absent optional evidence is omitted; there are no empty dynamic placeholders.

The reader validates the manifest directory boundary, every declared hash, unique ids, exact kind-specific record shapes, and all Saga, step, route, input, interaction, setup, workload, occurrence, scenario, observation, and attribution references before returning content. Setup preflight, execution, ImpactV1, Maven/test logs, runtime input maps, and normalization diagnostics remain outside the package.

Historical v3/v4/v5 manifests and their embedded record shapes are documentation evidence only. Regenerate a current package for current preflight or execution; there is no compatibility or migration layer.

### Determinism and bounds

The generator preserves stable ordering, deterministic ids, explicit configuration, and an explicit seed. `maxCatalogScenarios` is one total exported-workload cap: descriptor-selected prerequisite workloads are ordered deterministically and reserved first, then remaining capacity is filled from stable base-workload order. Manifest generated/selected/capped/exported counts distinguish those stages, and the final workload catalog never exceeds the configured cap. If event-consequence expansion reaches its reserved base-workload capacity between base records, the manifest records the cap encounter and number of remaining base workloads omitted at that stage instead of silently exiting. Bounded defaults and caps prevent accidental materialization of the full combinatorial space.

Implemented generation choices include:

- `BRUTE_FORCE` or `INTERACTION_PRUNED` workload selection;
- single-Saga and bounded multi-Saga workloads;
- `SERIAL`, bounded order-preserving, and `SEGMENT_COMPRESSED` forward scheduling;
- eager all-zero and single-point vectors for static setup candidates;
- guarded on-demand persistence for arbitrary valid multi-fault vectors;

### Compensation and recovery

A compensation checkpoint records why a completed forward occurrence may require recovery:

- `EXPLICIT_COMPENSATION`;
- `IMPLICIT_SAGA_ROLLBACK`;
- `CONSERVATIVE_UNKNOWN`.

For one aborted participant, checkpoints preserve reverse completed-step order. Recovery schedules may interleave those checkpoints with still-valid forward actions from surviving participants. The configured recovery cap limits written schedules without changing the exact uncapped count for a computed vector.

A participant commits automatically after its final successful forward action. Commit is reported but is not independently schedulable.

### On-demand multi-fault persistence

Multi-fault vectors are persisted before execution rather than overlaid at runtime. The request path:

1. locks the real package directory through `.on-demand-fault-scenario.lock`;
2. re-reads and validates the current package;
3. validates the workload, vector, static setup candidacy, structure, and recovery cap;
4. generates bounded recovery schedules;
5. updates the FaultScenario catalog, accounting, and manifest;
6. validates the resulting package before success.

Persist a vector with the verifier request CLI before selecting it for execution:

```bash
java -cp <verifiers-classes-and-dependencies> \
  pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.FaultScenarioRequestCli \
  --manifest-path <run-dir>/scenario-catalog-manifest.json \
  --workload-plan-id <workload-plan-id> \
  --fault-vector <binary-vector> \
  [--recovery-schedule-cap <positive-request-cap>]
```

The manifest path, WorkloadPlan id, and vector are required. The request uses the package recovery cap by default; a supplied positive cap becomes that request's effective cap. An exact workload/vector/cap repeat deduplicates, while a different cap may add only schedules not already present. The result status is `PERSISTED`, `DEDUPLICATED`, `REJECTED`, `INTEGRITY_FAILURE`, or `PERSISTENCE_FAILED`; only the first two are successful.

Process-local writers serialize and the package directory is also guarded by `.on-demand-fault-scenario.lock`. Mutation stages the fault, request, accounting, and manifest files; validates the staged package; promotes them with rollback on any covered acquisition, write, move, cleanup, or validation failure; and releases lock resources. Tests cover byte preservation across all eight publication boundaries and atomic-move fallback. The local `FileChannel` contract still does not claim network-filesystem or multi-host coordination, and an abrupt host/process death outside the tested rollback boundaries is not qualified.

## How to read accounting

`accounting.json` owns configuration and aggregate counts. It intentionally contains no row per Saga set, WorkloadPlan, vector, recovery schedule, or runtime event.

### Headline questions

Use these questions first:

1. **Input coverage:** how many Sagas have accepted inputs?
2. **Setup candidacy:** how many accepted inputs/workloads pass the static setup gate?
3. **Generation:** how many WorkloadPlans and FaultScenarios were written under the configured bounds?
4. **Reduction:** how does the selected/generated workload space compare with the bounded input-bound baseline?
5. **Recovery:** for vectors actually computed, how many recovery schedules existed uncapped and how many were written?

The manifest's many extraction and generator counters are diagnostic telemetry. They help localize parser/generator changes but do not each represent a thesis result.

### Type-level versus input-bound counts

- **Type-level coverage** asks which Saga classes and aggregate-type interactions exist before requiring concrete test-derived inputs. It explains missing input coverage but cannot emit executable workloads.
- **Input-bound space** combines accepted concrete InputVariants with Saga sets and schedule counts. It can emit WorkloadPlans.

These counts answer different questions and are not expected to match.

### All, selected, and written workload totals

- `workloads.all.inputBoundTotal`: bounded Cartesian baseline over accepted inputs and schedules;
- `workloads.selected.inputBoundTotal`: subset whose Saga sets and concrete input tuples satisfy the configured selection rule;
- `workloads.written.total`: records actually materialized after write mode and caps.

They can be identical for a simple single-Saga uncapped run. They differ when interaction pruning, count-only mode, or catalog caps matter. Do not present three equal values as three independent achievements.

### Strict and broad interaction lenses

These are report/evaluation lenses for uncertainty in static aggregate binding:

- **strict interaction estimate** uses two-sided semantic aggregate-root evidence; a selected multi-Saga input tuple must also have positive exact or same-source evidence for the shared aggregate;
- **broad interaction estimate** additionally permits weaker type-level/unknown-key fallback evidence and is therefore a conservative upper estimate with more possible false positives.

Type-level connected-set counts and connected sets with accepted positive input tuples answer different questions. A lack of contradictory input evidence is not positive evidence. The strict and broad counts do not represent runtime modes; generation configuration such as `generationStrategy` and `allowTypeOnlyFallback` selects records.

They are useful only when evaluating how uncertain aggregate-key extraction affects multi-Saga pruning. For a `maxSagaSetSize=1` setup package, strict/broad pair counts do not explain the emitted single-Saga catalog and should not be treated as headline metrics.

### Segment-compressed scheduling

`SEGMENT_COMPRESSED` identifies cross-Saga conflict-anchor steps, groups each Saga's preceding non-anchor run with its next anchor, and interleaves those segments while preserving in-Saga anchor order. Non-anchor tails are appended once in deterministic order.

It reduces permutations of internal/non-conflicting steps while retaining conflict-anchor order cases under the verifier's static conflict evidence. It is not proof of semantic completeness or exact runtime aggregate-instance binding. See [`decisions/2026-06-16-conflict-anchor-segment-compression.md`](decisions/2026-06-16-conflict-anchor-segment-compression.md).

### Recovery accounting scope

Recovery totals are exact only for vectors that were actually computed eagerly or on demand. `exactComputedSumsScope=EXACT_SUM_OVER_COMPUTED_VECTORS_ONLY` and `allVectorRecoveryTotalStatus=NOT_COMPUTED` prevent an all-vector claim.

A synthetic high-cardinality fixture currently proves exact count `118264581564861424` while retaining only `20` schedules. This is evidence for bounded exact counting, not evidence that every vector in every workload was enumerated.

## Optional dynamic evidence

Dynamic enrichment normalizes five runtime kinds—`stepStarted`, `stepFinished`, `commandSent`, `aggregateAccessed`, and `invariantViolation`—into `dynamic-observations.jsonl`. Each runtime event body appears once. `dynamic-attribution-links.jsonl` groups supporting observation ids by test execution and Saga invocation and records one of `exactInput`, `testAndShape`, `shapeOnly`, `ambiguous`, or `unmatched`.

Unique input accounting assigns each input only its strongest evidence (`exactInput` > `testAndShape` > `shapeOnly`). Workload participant accounting distinguishes all inputs observed in one common test, all observed only across separate tests, some observed, and none observed. Only exact or test-and-shape evidence establishes co-observation; none of these categories claims that the persisted WorkloadPlan schedule executed.

Runtime input maps, Maven output, test-run reports, and normalization diagnostics remain under the diagnostic `dynamic-evidence/` directory outside the package. Raw simulator event JSONL is deleted only after the normalized files and manifest finalize successfully and is retained on failure.

The current input map uses `workloadPlanIds` on both sides. In the bounded Quizzes smoke,
the same 1,038 observations and 10 Saga-invocation groups changed from 0 exact / 2
test-and-shape / 8 shape-only before the repair to 2 exact / 0 test-and-shape / 8
shape-only afterward. The exact groups carry the persisted RemoveTournament and
AddParticipant input ids; the remaining groups retain only the evidence actually
available to them.

The durable static/dynamic boundary is explained in [`decisions/2026-04-28-hybrid-static-dynamic-key-binding.md`](decisions/2026-04-28-hybrid-static-dynamic-key-binding.md).

## ScenarioExecutor

ScenarioExecutor is a narrow deterministic Saga/local replay path, not a generic distributed runner.

### Setup preflight

Preflight selects current WorkloadPlans whose input/setup structure is declared materializable. Provider-backed candidates may share one Spring application context. Each source-derived setup candidate is isolated in its own bounded fresh JVM/Spring/H2 worker before it:

1. restores fresh process-local state;
2. executes validated setup actions once in source order through an application-owned closed dispatch map;
3. retains non-void results and resolves later action/participant references with exact type checks;
4. clears setup-created pending events and proves an empty baseline;
5. materializes each exact persisted input tuple, creates fresh Saga units of work, and starts every exact participant;
6. stops before every target workflow action.

Result meanings:

- `SETUP_READY`: exact materialization and Saga startup succeeded;
- `MATERIALIZATION_FAILED`: a persisted input could not be reconstructed;
- `STARTUP_FAILED`: arguments materialized, but exact Saga construction/startup failed.

Normal execution uses the same setup implementation, so a separate preflight is optional. The occurrence, validation, dispatch, and isolation rationale is retained in [`decisions/2026-08-28-source-derived-ordered-setup.md`](decisions/2026-08-28-source-derived-ordered-setup.md).

### Normal execution

Normal execution requires:

- a complete current package path;
- one exact persisted FaultScenario id;
- an output path;
- an application classpath/Spring application with supported Saga/local runtime dependencies.

It sequentially replays current persisted forward, event, and compensation action references, injects assigned faults at exact forward slots, and commits each participant after its final successful forward action. Before measurement, the referenced provider-backed or source-derived setup runs once, resolves typed bindings, clears setup-created pending events, proves an empty pending-event baseline, and remains outside fault allocation, conformance, and ImpactV1.

Only a zero-bit body/commit failure explicitly marked with the simulator `DomainFailure` contract may use immediate checkpoint recovery, skip the failed participant's remaining forwards, continue valid survivor actions, and report `DEVIATED`.

Unmarked failures—including plain `SimulatorException`, service unavailability, ordinary runtime failures, missing infrastructure, and leaked assigned-fault exceptions—are infrastructure failures. They run no fallback, stop survivor execution, and report `INCOMPLETE` after measured execution starts. Thrown compensation actions also hard-stop.

### Event replay

Replay mode is activated before Spring startup. The simulator captures the exact event only after persistence, suppresses unscoped scheduled polling, and allows one selected event id through one persisted `EventHandling` bean method and one eligible subscriber. The executor invokes that real Spring bean synchronously outside the fault-vector boundary and before the next outer action.

An event consequence is masked when its specific trigger occurrence has a pre-body assigned fault, fails before capturing a matching event, or is not reached. If the trigger body or commit captures the selected event and then fails, execution hard-stops as `TRIGGER_FAILED_AFTER_EVENT_EMISSION`; the event is not dispatched and ImpactV1 is not evaluated. This prevents an emitted-but-undelivered event from becoming a false zero. Missing or multiple matching events/subscribers, selected-route mismatch, recursive registration, replay-control failure, and handler failure also hard-stop measured execution and leave ImpactV1 not evaluated. Replay currently supports one exact local subscriber only—no fan-out, recursion, nested event chain, retry, TCC, remote, stream, or gRPC delivery.

### Report

`microservices-simulator.scenario-execution-report.v5` records:

- attempt, package, WorkloadPlan, FaultScenario, vector, and fault-provider identity;
- separate prerequisite-provider or source-derived setup action/result/binding, cleanup, and baseline evidence;
- planned and actual action order;
- planned event route and actual persisted event/subscriber evidence;
- fault-slot realization or causal event masking;
- action body, commit, and recovery outcomes;
- participant setup/final state and skipped forwards;
- lifecycle events and blockers;
- terminal status and schedule conformance.

Execution reports live outside the package and must not alias package artifacts or impact reports.

### Current execution boundary

Supported:

- persisted setup-candidate Saga/local single- and multi-participant workloads;
- deterministic sequential replay, including one exact local event consequence;
- current provider-backed or validated source-derived setup outside measurement;
- binary forward faults;
- persisted compensation schedules;
- explicit domain-failure fallback and conservative infrastructure hard stops;
- dry-run selection/mapping validation;
- optional ImpactV1 sidecar.

Unsupported:

- TCC, stream, gRPC, distributed, or true-parallel execution;
- arbitrary non-candidate workload replay;
- repeated same-participant runtime step names;
- compensation faults, delay/non-binary impairments, or automatic recovery retries;
- generic persistent-environment reset beyond fresh worker/process isolation;
- automatic FaultScenario selection or runtime vector overlays.

The compensation/failure boundary is retained in [`decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`](decisions/2026-07-19-compensation-aware-fault-scenario-contract.md). Event-consequence ownership and replay isolation are retained in [`decisions/2026-07-30-deterministic-event-consequence-replay.md`](decisions/2026-07-30-deterministic-event-consequence-replay.md).

## ImpactV1

The simulator emits a structured invariant event only when `SagaUnitOfWorkService.registerChanged` reaches the existing `Aggregate.verifyInvariants()` boundary and that verification throws. Instrumentation records context and rethrows the identical application exception; evidence recording must not replace domain behavior.

When `--impact-output-path` is supplied, ScenarioExecutor installs an attempt-scoped collector and writes `microservices-simulator.scenario-impact-report.v1` with:

- execution-attempt, WorkloadPlan, and FaultScenario ids;
- evaluation status and reason when not evaluated;
- invariant finding count and current ImpactV1 score;
- ordered structured findings.

`SUCCESS`, `COMPENSATED`, and `PARTIAL_COMPENSATED` attempts are evaluated. Setup, infrastructure, compensation, dry-run, and report-write failures are `NOT_EVALUATED` with null count/score rather than a false zero.

ImpactV1 does not detect silent compensation errors, postcondition failures, final-state divergence, or general business harm. The count and score are currently numerically identical because this first model has no weighting.

## Current evidence

Evidence here was generated from the final current-only implementation on 2026-09-02. Paths are workspace-relative and intentionally bounded.

### Equivalent Quizzes count-only analysis

Command shape (repository root):

```bash
env MEDIUM_MEM_LIMIT=5g MEDIUM_MEM_RESERVATION=2g /usr/bin/time -p docker compose run --rm \
  -e JAVA_TOOL_OPTIONS=-Xmx4g \
  -e VERIFIERS_DYNAMIC_ENRICHMENT_ENABLED=false \
  -e VERIFIERS_SCENARIO_CATALOG_CATALOG_WRITE_MODE=COUNT_ONLY \
  -e VERIFIERS_SCENARIO_CATALOG_INCLUDE_SINGLES=true \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_SAGA_SET_SIZE=3 \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_CATALOG_SCENARIOS=1 \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_INPUT_VARIANTS_PER_SAGA=1000 \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_SCHEDULES_PER_INPUT_TUPLE=20 \
  -e VERIFIERS_SCENARIO_CATALOG_ALLOW_TYPE_ONLY_FALLBACK=false \
  -e VERIFIERS_SCENARIO_CATALOG_INPUT_POLICY=RESOLVED_OR_REPLAYABLE \
  -e VERIFIERS_SCENARIO_CATALOG_SCHEDULE_STRATEGY=ORDER_PRESERVING_INTERLEAVING \
  fault-analysis-scenario-gen
```

Fresh package: `verifiers/target/quizzes-20260902-182645-973/`, generated in 36.28 seconds. It declares only `accounting`, `sagas`, `inputs`, and `interactions`; count-only wrote zero workload rows and no workload file. Counts remain 68 Sagas; 36/32 with/without accepted inputs; 884 inputs; 794/90 accepted/rejected; and 91/703 materializable/blocked. Direct interactions are now 785: 0 exact, 535 symbolic, and 250 type-only. Strict connected sets are 382/3,594 for sizes 2/3, of which 35/42 have accepted positive input tuples. Fallback connected sets are 547/7,190, of which 227/1,904 have accepted inputs.

The retained baseline `verifiers/target/quizzes-20260902-011240-501/` reported 764 direct interactions (0/152/612 exact/symbolic/type-only), strict connected sets 140/1,299 with 63/400 accepted-input sets, and fallback sets 540/7,005 with 223/1,840 accepted-input sets. The change is semantic, not a count-direction target. For example, `CreateQuestionCommand` and `CreateQuizCommand` explicitly delegate a null aggregate root; their third call arguments name a course or course execution and are no longer mis-associated as Question or Quiz keys. Conversely, `AnswerQuestionCommand`, `RemoveQuestionCommand`, and getter-based `UpdateQuestionCommand` calls now preserve their declared semantic roots. Stronger type-level evidence therefore grows, while strict input-bound selection shrinks because missing contradiction is no longer treated as proof that two inputs name the same aggregate.

The accounting equations reconcile independently by size. All Saga sets are `36 + 630 + 7,140 = 7,806`; selected sets changed from `36 + 63 + 400 = 499` to `36 + 35 + 42 = 113`. The all input-bound total is unchanged at `794 + 2,460,298 + 1,244,846,908 = 1,247,308,000`. The selected total changed from `794 + 543,911 + 45,423,520 = 45,968,225` to `794 + 7,067 + 66,412 = 74,273`. All 884 input ids and non-evidence fields, along with discovery, acceptance, and materializability totals, are unchanged. Aggregate-key evidence intentionally changed on 84 inputs: 83 evidence values were semantically replaced and one was removed, changing evidence-bearing inputs from 652 to 651.

Static package sizes are: accounting 21,202 bytes; Sagas 68 records / 55,815 bytes; inputs 884 / 4,066,606 bytes; interactions 785 / 487,506 bytes; manifest 482 bytes.

### Bounded current executable package

`verifiers/target/m4-final/quizzes-source-package/` was generated by the focused `SourceDerivedSharedSagaWorkloadAnalysisSpec` opt-in writer. Before dynamic enrichment it contained 68 Sagas, 847 inputs, 764 interactions, one 12-action source-derived setup, one RemoveTournament/AddParticipant WorkloadPlan with five exact Saga-local step occurrences, 13 eager FaultScenarios, and an empty request stream.

Docker preflight used `PACKAGE_PATH=/reports/m4-final/quizzes-source-package/scenario-catalog-manifest.json`, `PREFLIGHT=true`, and `OUTPUT_PATH=/reports/m4-final/source-preflight-report.json`. It returned `SUCCESS`, one candidate, two participants, one successful source setup, all 12 actions successful, four exact participant bindings resolved, and `SETUP_READY`.

One on-demand request persisted workload `12f7f358f8c3c04a6541a8a86450639089e1922c355206871fc2df6047c68417`, vector `10100`, and effective cap 20. It reported one uncapped/written schedule and added scenario `2dcaa575630b3c8e7bba41f27ef0717b5bc05a2a0f4df8c7d65f6965b783260a`. A fresh Docker execution selected that persisted id and returned `PARTIAL_COMPENSATED / EXACT`; ImpactV1 evaluated zero findings. Reports and full logs are under `verifiers/target/m4-final/`.

### Bounded current dynamic smoke

The same package was enriched by one host invocation of `DynamicEnrichmentOrchestrator` selecting only `RemoveTournamentAddParticipantRecoveryWindowExploratoryTest`. Maven ran five features with zero failures. The current input-map rerun is under `verifiers/target/input-map-fix/quizzes-source-package/`. It has 1,038 observations (188 step-started, 188 step-finished, 422 command-sent, 239 aggregate-accessed, one invariant violation) and 10 attribution groups (2 `exactInput`, 8 `shapeOnly`). Unique input evidence is 2 exact, 0 test-and-shape, 0 shape-only; its only workload remains `allInputsObservedInOneCommonTest`.

Before the repair, the equivalent run produced 0 exact, 2 test-and-shape, and 8 shape-only groups because the verifier wrote `workloadPlanIds` while the simulator expected `scenarioPlanIds`. The simulator now reads `workloadPlanIds`; the raw event JSONL was removed after successful publication, while the input map, Maven log, test reports, and normalization diagnostics remain outside the package.

Current executable-package role sizes are: accounting 22,015 bytes; Sagas 68 / 90,446; inputs 847 / 3,959,368; interactions 764 / 402,946; setups 1 / 9,643; workloads 1 / 1,050; FaultScenarios 14 / 3,792; requests 1 / 262; observations 1,038 / 725,795; attributions 10 / 8,991; manifest 1,226 bytes. Every declared SHA-256 matched, and the current reader revalidated all cross-file references after dynamic publication.

### Regression proof

The static-interaction focused proof passed 196 tests with zero failures, errors, or skips. The first complete verifier run exposed two stale exact fixture inventories in `ApplicationsFileTreeParserSpec`: both omitted the newly added `SemanticRootItemCommand` fixture. After correcting those expectations, the complete suite passed 675 tests with zero failures, errors, or skips. The fresh package passed current-reader shape, SHA-256, identity, ordering, uniqueness, and reference validation. Generated reports are evidence artifacts, not package roles.

## Current limitations

- Thirty-two discovered Quizzes Sagas still lack accepted static inputs. This does not mean no tests exist; their invocation/value shapes remain unclassified or unsupported.
- Two Quizzes steps retain focused static-analysis limitations: one unresolved `SagaCommand` payload and one unresolved dispatch through a helper `send` call. Unsupported aggregate-root expressions remain keyless and can enter only the configured fallback lens.
- Event-consequence extraction supports one conservative direct producer shape and one unique local consumer. Wrong receiver or unit-of-work binding, mixed compensation-origin emission, conditional/repeated consumer delegation, multiple/repeated/conditional producer emissions, fan-out, recursion, nested event chains, and unresolved routes are rejected diagnostically.
- Helper-built course DTOs now preserve `DateHandler.toISOString(endDate)`, but that local transform remains unsupported and blocks those candidates rather than fabricating empty DTOs.
- Static setup candidacy is conservative prediction. The fresh bounded source-derived workload is setup-ready, but other packages and environments still require actual setup evidence.
- Repeated same-participant runtime step names are structurally rejected because current Saga/local runtime state is keyed by step name rather than occurrence id.
- Segment compression preserves conflict-anchor order cases under extracted evidence; it does not prove every semantically distinct runtime interleaving is retained.
- Dynamic enrichment remains local/Saga-focused. The fresh one-class smoke resolves 2 of 10 Saga-invocation groups exactly; the other 8 remain shape-only, and 933 observations still lack a uniquely resolved Saga and Saga-local step.
- The generated Quizzes event-consequence pair provides one ImpactV1 1/0 discrimination. The automatic source-derived RemoveTournament–AddParticipant path proves one harmful/control pair without its descriptor/provider; the retained historical benchmark still provides the complete 34-row 19/15 landscape. ImpactV1 is zero for every Remove/Add row, so a broader impact contract remains undefined.
- Three refreshed benchmark controls demonstrate the explicitly marked zero-bit domain-fallback path; other fallback shapes remain unqualified.
- Persistent-environment reset is the caller/orchestrator's responsibility.
- On-demand mutation is process-local and filesystem-local. Covered publication failures roll back byte-for-byte, but network filesystems, multi-host coordination, and abrupt host/process death are not qualified.

## Not implemented

- Generic execution for every generated WorkloadPlan/FaultScenario shape.
- TCC, stream, gRPC, distributed, causal, or true-concurrent execution parity.
- Compensation faults, delay injection, non-binary impairments, or automatic recovery retry/backoff.
- Semantic deduplication of value-equivalent inputs.
- Profile-aware resolution for ambiguous multiple `@Service` implementations.
- State-divergence, postcondition, or silent-compensation impact models beyond invariant-count ImpactV1.
- Generic batch execution qualification, generic reset orchestration beyond fresh process workers, GA/local fault search, or scenario prioritization. The current benchmark command remains application-specific.

## Safe thesis framing

Safe current claim:

> The verifier deterministically extracts Saga, input, interaction, setup, workload, fault, and runtime-observation facts into one current-only role-keyed package; derives static conflicts from semantic command roots, requires positive input evidence for strict multi-Saga selection, preserves deterministic bounded generation and current on-demand mutation, can preflight and replay a selected persisted Saga/local FaultScenario, and reports ImpactV1 separately. Fresh Quizzes evidence reconciles the corrected size-1/2/3 space without changing input acceptance or materializability, preflights one source-derived Remove/Add workload, persists and replays one multi-fault request, and normalizes five runtime observation kinds without inventing exact input attribution.
