# Verifier current state

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
  -> automatic potential-impact evidence sidecar
```

The verifier does **not** prove that an application is correct. It currently answers narrower questions:

1. Which Sagas, steps, aggregate footprints, and test-derived inputs can be found?
2. Which reproducible workload and fault schedules can be generated from those facts?
3. Which generated inputs are static setup candidates, and which can actually start in the current Saga/local runtime?
4. What happened when one persisted FaultScenario was replayed?
5. Did that attempt trigger an observed aggregate-invariant rejection?
6. Which of the three implemented potential-impact conditions hold, for which aggregate identities, and with what observation coverage?

The earlier explicit Quizzes broken-reference benchmark rule is retained as separate
application-specific evidence; it is not the generic ImpactV2 definition.

The retained ordinary single-input count analysis has **665 static setup candidates out of
796 accepted inputs**: 664 with source setup, one without setup, and 131 blocked.
Exact earlier setup results now survive inside participant DTOs and collections.
Four representative inputs from the latest extension completed fault-free Docker
execution with exact persisted identities. This does not qualify all 665 at runtime.

ImpactV2 is integrated into ordinary Saga/local execution. It counts distinct objects
with deleted dependencies, residual effects of failed operations, or an unresolved
selected event delivery, with explicit completeness and invalid-attempt reporting.
The latest selected qualification has **29 COMPLETE benchmark assessments** (14 score 0,
15 score 2) and **30 broader control/fault pairs** (60 COMPLETE: 52 score 0, seven score 1,
one score 2). All 30 controls succeed with exact conformance; one has a positive score.
These are potential-effect observations, not severity or universal domain-harm judgments.

Start with the [Portuguese advisor note](reunioes/2026-09-08.md) for the domain and a
step-by-step example. [ImpactV2](#impactv2-assessment) owns the checking contract and
[the latest qualification](#owned-cycle-coverage-and-control-requalification) owns current
campaign results. The [four-run investigation](#understanding-impact-through-quizzes)
explains the earlier methodological choices; its counts are not the current benchmark.
The 665-input count snapshot predates the final qualification fixture additions and is
not a new full-catalogue count for this checkout.

## Reading order

Use this page by question:

- [What the main terms mean](#the-essential-terms)
- [What inputs are analyzed](#inputs-and-static-extraction)
- [What the current package contains](#the-current-package)
- [What the accounting metrics mean](#how-to-read-accounting)
- [What dynamic enrichment contributes](#optional-dynamic-evidence)
- [How setup preflight and execution differ](#scenarioexecutor)
- [The Quizzes domain, four runs, and impact interpretation](#understanding-impact-through-quizzes)
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

A **SetupPlan** is a reusable preparation record referenced by WorkloadPlans. A source-derived setup stores validated application-facade calls in source order and binds participant arguments to earlier setup results or approved properties. One coherent observed setup context may cover any number of inputs, but it is attached only when it completely and unambiguously supplies the selected workload's setup-dependent arguments; inputs from separate test contexts are not combined into one observed setup. A provider-backed setup stores the provider identity and typed binding requirements. Setup actions run outside target fault injection and measured impact; attempt-local runtime ids and values never enter package identity.

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

### Potential-impact evidence

**Potential-impact evidence** is the attempt-scoped, read-only persistent-state record
collected after successful setup for ImpactV2 checks. It contains normalized
baseline/final aggregate projections, transaction-confirmed writes, writer/action phase,
subscription-declared dependencies, and exact selected-event receiver observations.
Coverage gaps and invalid or unavailable measurement remain explicit. The ordinary
executor preserves these facts in the assessment sidecar alongside derived findings.

### Impact

**ImpactV1** is the existing invariant-rejection metric:

```text
ImpactV1 = invariantViolationCount
```

It counts structured `INVARIANT_VIOLATION` events emitted when the existing Saga aggregate-write boundary rejects a change through `Aggregate.verifyInvariants()`. It does not infer harm from an assigned fault, abort, or compensation alone.

**ImpactV2** assesses explicit potential-impact conditions from
persisted state and execution evidence, counting distinct affected aggregate identities
within a declared check scope. Its first categories are subscription-declared
dependencies on deleted objects, residual data after failure/recovery without competing
writers, and unresolved event progress.
It is an extent measure, not a business-harm or severity oracle. Coverage gaps and invalid
attempts remain distinct from an evaluated zero. Partial reports retain an observed
affected-object lower bound while their complete score remains null.

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

When reading a persisted package, readiness is restored per argument and recursively
through its recipe. An unresolved setup-bound identity does not make a separate, fully
specified DTO constructor unready. The input's overall `materializable` flag remains
unchanged; exact setup bindings still have to resolve before execution.

The executor materializes a supported subset. Runtime-owned arguments currently include `SagaUnitOfWorkService`, `CommandGateway`, and a fresh `SagaUnitOfWork`. A baseline binding is materializable only when the persisted provider id/version is present and the provider returns the required key with the persisted exact type. Applications may declare bounded prerequisite workloads in `src/test/resources/verifier-prerequisite-scenarios.json`; descriptor schema `microservices-simulator.prerequisite-scenario-descriptor.v2` requires `selectionKind=EVENT|NO_EVENT`. `EVENT` preserves the exact selected route, while `NO_EVENT` rejects event-route fields and selects only a source-derived workload with no event consequence. The generic adapter resolves named Saga steps without application FQNs in verifier production code. The matching provider may live on the application's test classpath. Unsupported calls and unresolved source values remain blockers.

Source setup result properties admit `aggregateId`, `courseAggregateId`, and the exact
nested path `quiz.aggregateId`. The nested path retains the existing `resultProperty`
record and `property` string. Before any setup dispatch, the runner verifies public
zero-argument getters from a declared DTO root through a DTO `getQuiz()` result to an
Integer-compatible `getAggregateId()` result. Other paths and incompatible signatures
are rejected; null intermediate values fail setup without starting target participants.

Source setup preserves existing complete `setup()`-only candidates and can additionally
use preparation from the same exact Spock feature. Typed AST occurrences identify the
target and the source order: a new candidate contains ordinary `setup()` actions followed
by supported facade calls strictly before that target, including void activation and
enrollment effects. Calls from different features or classes are never combined by this
extension. Assertion/cleanup/where labels, control flow, direct workflow execution, and
direct event-handler execution close further feature-prefix extraction.

Direct facade arguments retain caller DTO setters and property assignments as they stood
before the call, including self-rebinding calls. Later assignments cannot leak backward
into the retained setup argument recipe.

For a selected tuple, the candidate must end before its earliest selected target, supply
every setup-dependent argument, and contain no selected target action. An unselected
facade effect between selected targets blocks the tuple rather than being omitted or
moved into setup. Collapsed repeated target occurrences and differing complete candidates
remain ambiguous and blocked. This metadata is internal; persisted record shapes and the
closed application setup dispatcher are unchanged. Existing fixture-only coverage retains
priority, so this is an extension of supported preparation, not whole-test replay.

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

A dispatched `SagaCommand` can add a persistent semantic-state write to its payload
access. A supported non-null `setSemanticLock(...)` before dispatch adds a WRITE on
the same aggregate target, including values named `READ_TOURNAMENT` or `NOT_IN_SAGA`.
The payload READ remains visible. Enum names do not determine whether a write occurred.
Unsupported wrapper configuration, custom subclasses and anonymous wrappers remain uncertain; a proven plain read needs no
implicit rollback checkpoint.

Before the executor reports a failed participant `COMPENSATED`, it queries the framework's
remaining recovery checkpoints. Pending work stops execution with terminal status `UNEXPECTED_EXECUTION_FAILURE`,
schedule conformance `INCOMPLETE`, and `PENDING_RUNTIME_RECOVERY`; a failed query reports `RECOVERY_CHECKPOINT_DISCOVERY_FAILED`.
Both produce an invalid ImpactV2 assessment with null score. The executor does not run
unplanned cleanup to make an incomplete recovery schedule appear complete.

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

A source setup may prepare application state without supplying any participant argument.
The parent accepts its explicitly empty `participantBindings` array when action,
cleanup/baseline, materialization, startup, and worker-isolation checks pass. Missing,
null, or malformed binding evidence is rejected before deserialization; any reported
binding must still be resolved.

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

Replay mode is activated before Spring startup. The simulator captures the exact event only after persistence, suppresses unscoped scheduled polling, and allows one selected event id through one persisted `EventHandling` bean method and one eligible subscriber. The executor invokes that real Spring bean synchronously outside the fault-vector boundary and before the next outer action when the persisted route resolves to one `EventHandling` class.

There are two separate multiplicities: different handler routes for one event, and
different eligible aggregate instances within one route. `ScenarioGenerator` retains
the base workload and creates separate route/placement variants, each with
`List.of(consequence)`, subject to extraction support and catalogue caps. It does not
combine deliveries to several routes in one generated workload. Within the selected
route, `EventApplicationService` requires exactly one eligible aggregate; zero or
multiple receivers hard-stop. This is narrower than normal application event processing,
which can traverse several subscribers and invoke different routes. Delivery order
across all interested listeners is therefore not qualified by these scenarios.

Each selected delivery is one atomic normal action, placed after its trigger among
outer forward steps. It adds no fault bit or recovery checkpoint; the consumer's internal
steps are not independently interleaved with outer actions. A completed scenario is the
end of that selected action schedule, not proof that every possible listener has run.
The [Portuguese event example](reunioes/2026-09-08.md#34-um-evento-com-varios-listeners-o-que-executamos-atualmente)
illustrates the distinction and its effect on the successful positive control.

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
- automatic `*.impact-v2.json` assessment sidecar derived from the execution-report path.

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

## ImpactV2 assessment

The collection path is concrete instrumentation, not free-text log inference:

| Code boundary | Responsibility |
| --- | --- |
| `ScenarioExecutor` / `ImpactV2EvidenceCollector.start()` | Start after successful setup and obtain the baseline with `PersistentStateObserver.snapshotAll()` |
| `ImpactWriterContext` | Attribute each forward/recovery/event action; consumers have distinct writer identity |
| `SagaUnitOfWorkService.registerChanged()` / `registerCommittedWriteObservation()` | Register a Spring `afterCommit` callback and reload the exact revision through `snapshotVersion()` in an independent read transaction |
| `EventApplicationService.handleSelectedEvent()` | Observe the selected receiver's persisted state and eligibility before and after the real handler |
| `ImpactV2EvidenceCollector.finish()` / `ImpactV2Assessor.assess()` | Collect final state and event eligibility, then assess evidence against execution outcomes |

The collector is not a general read-from or value-lineage recorder. Existing aggregate
access traces identify objects and access modes but do not generically identify every
returned read version or copied value. ImpactV2's three checks do not classify dirty
reads, lost updates, write skew, or serializability; completeness is relative to those
checks. A multi-writer residual candidate remains unknown rather than being assigned to
one writer. The [Portuguese methodological discussion](reunioes/2026-09-08.md#46-e-as-anomalias-de-concorrencia-mudamos-a-direcao-da-tese)
explains why persistent-effect measurement was prioritized and what causal analysis
would additionally require. This is an explicit current scope boundary, not evidence
that concurrency-anomaly detection is unsuitable for the thesis.

Ordinary ScenarioExecutor attempts write
`microservices-simulator.scenario-impact-v2-assessment.v1` beside the execution report:
`execution-report.json` becomes `execution-report.impact-v2.json`. Collection begins only
after setup and captures one baseline, exact committed revisions reloaded after transaction commit during
measured actions, exact selected-event receiver state/eligibility after delivery, and one
final horizon snapshot plus final polymorphic eligibility. Receiver eligibility is also
measured against the exact selected event before the handler runs. Selection/setup and
incomplete execution produce assessment status `INVALID` while retaining unavailable
collection evidence. A valid completed execution without the observer produces assessment
status `UNAVAILABLE`. `microservices.simulator.impact.enabled=false` provides the bounded
observer-off control while still writing an explicit `COLLECTION_DISABLED` sidecar.

Persistent application data is projected from JPA managed attributes. Aggregate lifecycle
is included in comparison data; application dates, ordered lists, sets, owned entities,
and embedded reference versions are retained. Framework-generated row identities,
aggregate revision/creation timestamp/predecessor metadata, and Saga semantic locks are
kept out of application-data equality. Backreferences to an aggregate normalize to its
logical identity. A nested owned entity's backlink to an ancestor becomes a stable
relative ancestor reference only when an inverse JPA `mappedBy` association with
`PERSIST`/`ALL` cascade proves ownership and actually contains that child. Child fields
remain in the projection; generated row IDs and unordered traversal indexes do not enter
the reference. Unsupported cycles remain `PERSISTENT_MAPPING_CYCLE` gaps. Other unsupported mappings and missing identities
produce coverage gaps. Subscription targets resolve to exact persisted aggregate identities;
missing or type-ambiguous targets remain explicit gaps. Event consumers carry their own
writer identity, and unowned, mismatched, or asynchronous writes become attribution gaps.
Observation failures are contained and retained independently from a failing callback so
they cannot replace application outcomes or disappear from the sidecar.
The observer remains installed through synchronous final-horizon collection, then closes
before its retained failures are drained. Saga/local execution and Spring transaction
`afterCommit` callbacks complete before their action returns; general asynchronous command
propagation beyond that scheduled horizon is outside this narrow executor boundary.

The sidecar preserves package-manifest, attempt, workload and FaultScenario identity,
the raw baseline/final snapshots, committed writes, event deliveries and coverage gaps,
and three deterministic category results:

- `DELETED_DEPENDENCY`: an ACTIVE final source retains a subscription-declared dependency
  on a target observed becoming DELETED during the attempt and remaining DELETED;
- `FAILED_OPERATION_RESIDUAL`: a failed Saga with completed recovery is the sole observed
  writer of an aggregate whose application data or lifecycle differs at the horizon;
- `UNRESOLVED_DELIVERED_EVENT`: the exact scheduled event delivery succeeded, the same
  typed receiver's persistent state did not change across delivery, and the surviving
  receiver remains polymorphically eligible for that event at the horizon.

Each category records deterministic candidates, findings, evidence references and unknown
reasons. Another Saga or event consumer writing a residual candidate makes that object
unknown. Missing projections, writer attribution, exact delivery evidence, or final
eligibility cannot produce a positive finding. Current persistence represents deletion
through the aggregate lifecycle projection; a missing final snapshot is unknown rather
than proof of physical deletion.

`COMPLETE` assessments have a numeric `completeScore`, including explicit zero. The score
is the union of positive aggregate identities, so one object with multiple reasons counts
once. `PARTIAL` assessments retain findings and `observedAffectedObjectCount` as a lower
bound but serialize `completeScore` as null. Invalid executions and unavailable collection
serialize both counts as null. Assessment failure is contained, retains raw evidence and
adds `ASSESSMENT_FAILED`; it does not replace the application outcome. ImpactV1 semantics
and the v5 execution report are unchanged.

### Persisted qualification of the three checks

The first implementation slice passed eight persisted Quizzes cases through the ordinary
ScenarioExecutor, plus three observer-disabled controls. These are deliberately small,
provider-backed qualification workloads, not a claim that the entire generated catalogue
has been executed. The same collector and checks run for each case.

A Tournament points to a generated Quiz containing copied Question data. Removing a
Tournament deletes its Quiz first. Updating a Question can publish an event that should
refresh the Quiz's copy. Updating a Tournament changes its settings before updating the
Quiz; a failure between those operations invokes compensation.

| Case | Complete score | What the final evidence shows |
| --- | ---: | --- |
| Failure after Quiz removal, before Tournament removal | 2 | The active Tournament still declares the deleted Quiz dependency; the failed Saga also leaves the Quiz deleted |
| Successful removal control | 0 | Both removals finish; no surviving active source matches the rule |
| Ordinary Tournament compensation | 1 | Count, dates and topic IDs are restored, but embedded topics lose their course IDs and the application mutation time changes |
| Temporary no-op compensation variant | 1 | The same Tournament retains the requested count, dates and topic set |
| Successful update, ordinary build | 0 | The operation succeeds; no failed-operation residual is claimed |
| Successful update, no-op-compensation build | 0 | The unused compensation mutation does not create a finding |
| Current Question event consumer | 1 | Delivery succeeds but the Quiz stays unchanged and eligible for that exact event |
| Temporary repaired event consumer | 0 | Delivery persists the copied data and makes that event ineligible |

All eight assessments are COMPLETE with no collection gaps. Three additional fresh runs
with observation disabled report UNAVAILABLE and null counts. Their execution outcomes,
actions, prerequisite inputs and selected independent final-state witnesses match the
enabled counterparts. These witnesses cover the fixture's Tournament, Quiz and Question;
they are not an exhaustive equality proof for every database field.

The ordinary compensation result corrects the earlier expectation of zero. Its recovery
DTO is built by `TournamentTopic.buildDto()`, which omits the topic's course ID. Rebuilding
the embedded topic from that DTO therefore changes the saved course ID from 1 to null.
The generic persistent-state comparison exposed this without a Quizzes-specific rule.
The earlier experiment checked topic IDs and other selected fields, so its claim of
restoration was limited to those observations. Production Quizzes code remains unchanged.

Both compensation cases count one Tournament: this is an object count, not a severity
ranking. Application-owned timestamps are retained too; a timestamp-only difference can
satisfy the residual pattern under the current contract. Neither finding automatically
establishes business harm. Changing that policy requires a separate methodological choice.

Reproduction is owned by `verifiers/experiments/impact-v2/README.md`; the case matrix,
source/patch provenance, costs and limitations are in
`issues/2026-09-06-potential-impact-v2/M2-HANDOFF.md`. Validated reports are under
`verifiers/target/impact-v2-qualification/run-03/`.

### Broader ImpactV2 qualification

**Correction after qualification:** source analysis had missed the semantic-state write
in RemoveTournament's `getTournamentStep`. Some generated recovery schedules therefore
omitted real framework rollback, and the executor could report them compensated anyway.
The counts below describe the preserved earlier campaign, not proof of complete recovery.
The correction and fresh benchmark qualification are tracked in
`issues/2026-09-06-semantic-lock-recovery/HANDOFF.md`.

The next campaign ran 77 ordinary ScenarioExecutor attempts from frozen compiled sources
in fresh Docker/JVM/H2 environments: 17 current RemoveTournament/AddParticipant recovery
schedules and a separate 60-attempt catalogue sample. No scoring or application source
was changed during qualification.

| Cohort | Attempts with reports | COMPLETE | PARTIAL | INVALID | UNAVAILABLE |
| --- | ---: | ---: | ---: | ---: | ---: |
| Current historical-benchmark family | 17 | 12 | 5 | 0 | 0 |
| Broader sample: 30 control/fault pairs, 26 Saga types | 60 | 47 | 4 | 9 | 0 |

The historical 34 schedules map onto that campaign’s 17 when the omitted
`CONSERVATIVE_UNKNOWN` recovery actions are removed. All 34 rows are accounted for;
this is a structural projection, not 34 fresh executions or proof that the removed
runtime actions were semantically irrelevant. Eight current schedules correspond to
the old 19 `HARMFUL_FOR_RULE` rows: five have complete score 2 and three have a positive
lower bound of 1 with partial coverage. Of the nine schedules corresponding to the old
15 `NO_BROKEN_REFERENCE` rows, seven have complete score 0 and two are partial with
lower bound 0. The old labels never enter scoring. A newer benchmark wrapper rejected
this original provider-backed setup before execution; those 17 wrapper rejections are
retained separately, and the ordinary executor supplied the 17 authoritative reports.
No new independent application-rule observation is claimed.

The broader sample was selected before observing outcomes: one setup-materializable
single-participant workload per eligible Saga, plus four event-bearing shapes, each
paired with no assigned fault and a final-slot fault. This is setup eligibility, not
proof of argument runnability or a healthy control. Of 68 discovered Saga types, 42 had
no eligible workload under this procedure (32 lack accepted inputs; 10 have inputs but
no eligible setup/workload). No unsuccessful selection was silently replaced.

Twenty-one pairs have two complete assessments; 17 also have a successful exact control.
Three of those 17 show a positive fault result against zero: CreateQuestion leaves a new
active Question (score 1), RemoveCourseExecution leaves its Course count reduced despite
failed removal (score 1), and RemoveTournament leaves a deleted Quiz plus its surviving
Tournament dependency (score 2). The other 14 qualified pairs are zero/zero under the
implemented checks. Complete scores across all 60 attempts are 39 zeros, seven ones and
one two; four partial and nine invalid assessments have no complete score.

The main measurement gap is a nested owned back-reference:
`TournamentParticipant -> ParticipantAnswer -> TournamentParticipant`. Its mapping cycle
makes residual assessment partial in the four broader cases and five benchmark cases.
Six broader attempts cannot materialize UserDto/TopicDto arguments after successful
setup; three event controls have no eligible subscriber for their selected route.
No broader pair therefore extends successful event-delivery qualification beyond the
focused fixture above. Four otherwise complete pairs also have unhealthy controls.
In particular, both SolveQuiz controls already fail and leave a deleted QuizAnswer record
that was absent at baseline; the corresponding fault attempts have the same score 1.
The current lifecycle comparison counts that persistent remnant. It may be expected
compensation storage rather than damaged usable data; it is not an injected-fault harm
claim. Treating such deletion markers differently needs an explicit contract decision.

Selection, reproducible scripts and validators live in
`verifiers/experiments/impact-v2-broader/`. The campaign handoff and plain domain examples
are in `issues/2026-09-06-impact-v2-broader-qualification/`; raw and aggregate reports are
under `verifiers/target/impact-v2-broader/run-01/`. This sample does not qualify every
input, fault slot, schedule, event route, Saga type or concurrent combination.

An invariant exception can show that a local safeguard prevented a write, while earlier
Saga effects remain persisted. Rejection count alone therefore establishes neither final
harm nor successful recovery. The saved all-zero RemoveTournament/AddParticipant run
makes this concrete: joining commits first, then Tournament deletion is rejected because
participants remain. Its preceding Quiz-removal step has already completed. The saved
report is PARTIAL_COMPENSATED / DEVIATED with one rejection; it does not include a full
persisted final-state snapshot. The guard diagnosis is explained in the research
RESULTS.md. All-zero means no assigned fault, not guaranteed application success.
The experiment below assesses a different observation; it does not change ImpactV1.

### Corrected semantic-state recovery qualification

The corrected RemoveTournament/AddParticipant package contains **29 schedules across
12 canonical fault vectors**. It restores implicit rollback of the Tournament status
written by `getTournamentStep`, while the ordinary User read remains without a recovery
checkpoint. All 29 ran in fresh Docker/JVM/H2 environments. The 23 schedules whose fault
occurs after the Tournament status write report successful `IMPLICIT_SAGA_ROLLBACK` for
that step; every corrected execution reaches a valid terminal outcome.

A separate execution of the earlier `01001` plan stops with `PENDING_RUNTIME_RECOVERY`
for `getTournamentStep`, incomplete conformance, and an invalid/null ImpactV2 assessment.
This demonstrates the new completion guard independently of the generator fix.

ImpactV2 gives 19 complete assessments (10 score 0, nine score 2) and 10 partial
assessments (six with observed lower bound 1, four with lower bound 0). The partial cases
retain the existing participant/answer persistent-mapping-cycle coverage gap. Recovery
completeness and observation coverage remain separate. No impact category or scoring
policy changed.

A final static-only safeguard rejects custom and anonymous SagaCommand constructions
as uncertain. Regeneration after that safeguard produced identical hashes for every
package artifact, including the manifest; the runtime executor, simulator and Quizzes
sources match the frozen build used for the 29 executions. The evidence and reproduction
commands are in `issues/2026-09-06-semantic-lock-recovery/HANDOFF.md` and
`verifiers/target/semantic-lock-recovery/`.

### Owned-cycle coverage and control requalification

The nested owned-backlink observer correction was rerun against all 29 corrected
benchmark schedules. All assessments are now COMPLETE: **14 score 0 and 15 score 2**.
The ten former partials resolve to four zeros and six twos. Every previously complete
score is unchanged; execution outcomes, conformance and actual implicit rollback lists
match the previous campaign, including all 23 Tournament-status rollbacks. This is an
observation-coverage improvement, with no scoring-policy change.

Source input qualification also repairs per-argument readiness and provides explicit
test prerequisites for the retained broader control/fault pairs. The final selection
has 60 COMPLETE assessments across 30 pairs, all with successful exact controls:
52 scores of 0, seven of 1 and one of 2. One successful control scores 1: the selected
QuizAnswer deletion route runs, while a separate eligible Quiz route is outside the
schedule and the active Quiz still depends on the deleted offering. Complete assessment
does not imply zero impact or that every unselected handler has run.

Qualification retained 93 actual attempts: the 89-run main campaign plus two successive
two-run repairs of the UpdateStudentName event prerequisite. The first repair supplied
the wrong subscriber type for the persisted Tournament route. Both invalid controls are
preserved; the final pair supplies an eligible Tournament and scores 0/0. The other 29
broader pairs retain their first campaign evidence. The full evidence,
declared input replacements and source-linked domain examples are owned by
`issues/2026-09-06-impact-coverage-controls/HANDOFF.md` and `DOMAIN-CASES.md` in that
directory. Campaign artifacts are under `verifiers/target/impact-coverage-controls/`.

## Understanding impact through Quizzes

### The application and ordinary user stories

Quizzes is a teaching application with a question bank, quizzes, student attempts, and
tournaments. These names describe different objects:

| Object | Meaning in the application |
| --- | --- |
| Course and CourseExecution | A subject and one offering of it, with enrolled students |
| Topic and Question | A question-bank category and a reusable question |
| Quiz | A selection of questions, associated with a course offering and availability dates |
| Tournament | A competition with a creator, topics, participants, dates, and a reference to its Quiz |
| QuizAnswer | One student's attempt at a Quiz; starting creates it before any answer need be submitted |
| QuestionAnswer | An individual answer within that attempt |

A student enrolled in a course offering can create a Tournament by choosing topics,
a number of questions, and dates. The application obtains questions, generates a Quiz,
and then creates the Tournament referring to it. Joining adds a Tournament participant;
it does not itself create a QuizAnswer. Solving a Tournament loads its Tournament and
Quiz, starts a student attempt, and attaches that attempt to the participant.

These actions span separate microservice objects. `CreateTournament` is a Saga: it
performs several steps whose effects can persist before the whole request finishes.
The Tournament needs the generated Quiz's returned identity, so the order is:

```text
Validate/load the course offering, creator and topics; obtain questions
  -> generate and persist Quiz Q
  -> create and persist Tournament T referring to Q
  -> finish the Saga
```

The Quiz can therefore exist before the Tournament. If creation fails after generating
the Quiz but before creating the Tournament, the registered compensation deletes the
Quiz. Here deletion sets its latest lifecycle state to DELETED; older versions remain.
Compensation is application recovery logic, not an automatic rollback of every earlier
transaction or every other Saga's use of the data.

There are two distinct start paths. The ordinary Tournament solve operation uses
`SolveQuizFunctionalitySagas`, which loads the Tournament and associates the attempt with
its participant. The experiment uses generic `StartQuizFunctionalitySagas`, passing a
known Quiz ID directly from the creating Saga's result. It can cache a Quiz DTO and later
create an attempt from it without loading a Tournament. **The experiment establishes
this controlled workflow interaction, not ordinary UI discovery of an unfinished
Tournament or its Quiz.**

### The four controlled runs

Each run starts in a fresh JVM/Spring/H2 context with the prerequisite course offering,
students, topics, and questions. Runs 1, 2, and 4 create a new Quiz during measurement;
run 3 starts with an existing Tournament and Quiz and no participants. Letters identify
operations within a run: A creates or removes a Tournament, and B starts a Quiz attempt.

**Run 1 — Compensation happens before the other operation reads.**

1. A generates Quiz Q.
2. An assigned fault fires before A's Tournament-creation step body.
3. A compensates by deleting Q.
4. B tries to load Q and fails because it is absent from the live lookup.

Final observation: Q is deleted; no Tournament or student attempt was created.
This control shows that the read detects the deletion when recovery precedes it.

**Run 2 — The reader retains data across compensation.**

1. A generates Quiz Q; the same fault prevents Tournament creation.
2. Before A compensates, B reads Q and retains its returned DTO.
3. A compensates by deleting Q.
4. B continues from the cached data and successfully creates QuizAnswer R.

Final observation: R is active and refers to an earlier version of deleted Q.
The probe establishes the exact identity/version chain: Q version 18 was created,
returned to B, and stored in R; Q's latest version 19 is deleted. The exact read version
comes from a supplemental cached-DTO observation: existing raw access events identify
an object access but do not supply its read version. Creating an attempt is
not automatically harm. The unresolved question is whether this surviving attempt can
legitimately function using that snapshot.

**Run 3 — Partial removal leaves a reference without another operation.**

1. Setup supplies existing Tournament T and its Quiz Q.
2. A starts removing T and deletes Q first.
3. An assigned fault fires before the Tournament-deletion step body.
4. The available recovery releases the Tournament's semantic lock; it does not restore Q.

Final observation: T remains active and refers to deleted Q. There is no B and no
cross-operation read. A dirty-read detector cannot explain this execution because the
remaining effect comes from one Saga's incomplete removal/recovery.

**Run 4 — Intermediate visibility followed by successful completion.**

1. A generates Quiz Q.
2. B reads Q before A finishes.
3. A creates Tournament T and finishes successfully.
4. B creates QuizAnswer R and finishes successfully.

Final observation: Q, T, and R are active. Intermediate visibility occurred, but the
surviving-reference pattern is absent. Exposure alone contributes no harm under the
agreed impact direction; this control does not prove every possible domain invariant.

### Results and what they mean

| Run | Read another unfinished Saga's newly created object? | Active objects retaining a reference to a target deleted during the run | Invariant rejections |
| --- | ---: | --- | ---: |
| 1. Compensation before read | No | None | 0 |
| 2. Cached read survives compensation | Yes | One student attempt | 0 |
| 3. Partial removal alone | No | One Tournament | 0 |
| 4. Successful overlap | Yes | None | 0 |

All measured event queues were empty at the end, and all observed Quiz, Tournament,
and QuizAnswer semantic locks were released. A released lock says recovery no longer
holds that lock; it does not certify consistency. Six executions covered these four
runs plus repeats of runs 2 and 4, with identical normalized results for both repeats.

The observer extracts two application relationships through typed getters:
QuizAnswer→Quiz and Tournament→Quiz. The offline detector applies the same rule to both:
**an active object retains a reference to a target observed becoming deleted during the
attempt and still deleted at the observation horizon.** It counts distinct source
objects, not repeated log events. Its classification has no Quizzes class names or
case-specific verdicts; relationship extraction itself is not automatic or generic yet.

| Evidence | What we can conclude | What remains unknown |
| --- | --- | --- |
| B read A's object before A finished | Intermediate exposure occurred | Whether it caused a harmful final effect |
| An active object still refers to the deleted target | A reproducible final-state candidate exists | Whether a live target is required or a historical snapshot is legitimate |
| Existing invariant checks rejected no writes | No rejection was observed at that boundary | Whether the settled state satisfies all domain requirements |
| No candidates were detected | This pattern was absent in the observed slice | Whether other forms of damage exist |

An intentionally historical reference also triggers the structural detector in a unit
control. This is why `candidateAffectedObjectCount` is a measurement, while the research
`impactScore` remains null. The current observation slice covers three object types;
it is neither a whole-application scan nor a complete serializability comparison.
Some existing Quizzes tests compare serial orders and interleavings, but the generic
executor does not derive an impact verdict by comparing their final observations.

### Behavioral follow-up and current impact matrix

Six fresh application runs now compare three probes against healthy controls. Each
captures the original final state first, then records the probe as a separate continuation.
Independent probes use separate reproduced states. The original score and timeline do
not incorporate probe activity.

| Observation/probe | Affected state | Healthy control | Current interpretation |
| --- | --- | --- | --- |
| Active object retaining a reference to a deleted target | Present in the attempt and partial-removal examples | Absent | Structural candidate; live-required versus historical meaning unresolved |
| Load the surviving Tournament | Succeeds | Succeeds | The Tournament remains retrievable |
| Load Tournament, then fetch its returned Quiz ID | Quiz lookup fails: aggregate not found | Both reads succeed | Demonstrated read failure against a working control; domain harm undecided |
| Submit an answer on the surviving attempt | Fails at getQuizAnswerStep | Same failure | Existing baseline defect; no discrimination of compensation's consequence |

The user explicitly chose to record the read failure without declaring domain harm or
adding an active-Tournament/retrievable-Quiz rule. This matrix contains observations and
controls, not severity weights. Research impact scores remain null; production ImpactV1
is unchanged.

The Tournament Quiz probe is a sequence of two existing public facades, not the full
SolveQuiz workflow. The two Tournament states have matching fixture structure and relative
dates, with no participants; valid solve preconditions were not established. The answer
pair uses the exact retained Quiz version and matching question/option/user inputs. Both
fail because the workflow dereferences its attempt DTO before assigning it, so answer
submission cannot currently answer the impact question. Application repairs are separate
from this measurement.

All six before snapshots match their recorded original final states. The observed
Quiz/Tournament/QuizAnswer state slice, persisted answer contents, and latest observed
Saga aggregate locks/versions remain unchanged by these probes; pending queues stay
empty. This is bounded observation, not a whole-application correctness claim. Returned
Tournament DTOs omit the Quiz version, so exact target-version evidence comes from the
persisted relationship rather than the DTO.

Detailed results and the earlier all-zero rejection diagnosis are in the experiment's
RESULTS.md. [The roadmap](roadmap.md#impact-probes-are-a-methodological-option-not-an-adopted-score)
keeps final-state checks, bounded continuations, and serial comparisons as alternatives.
The user subsequently accepted potential-impact observations without bespoke prohibited-state
rules. The update experiments below broaden the evidence beyond deleted-target references;
read failures are still not automatically treated as domain harm.

### Update experiments: propagation and compensation

The user chose potential-impact observations without requiring a business-harm oracle,
and asked to test update effects before integrating a broader score. A separate experiment
under `verifiers/experiments/impact-updates/` compares four fresh JVM/Spring/H2 conditions:

| Condition | Observed result | Meaning |
| --- | --- | --- |
| Current Question-to-Quiz update handler | Question 7 changes title/content; the same exact update event is delivered successfully twice to Quiz 10, whose embedded question remains unchanged | Reproduced application persistence defect and lack of receiver progress; no injected fault is needed |
| Temporary build with the missing receiver save | First delivery persists the new text and event version; the same event is subsequently ineligible | Diagnostic repaired control, not a production application fix |
| Current UpdateTournament compensation | Tournament 11 changes from two questions to three; an assigned fault stops updateQuizStep before its body; recovery restores the original count, dates and topics | Successful recovery of the observed update, despite a new aggregate version |
| Temporary build with a no-op compensation body | Same forward update and assigned fault; recovery executes its checkpoint and releases the lock, but count three and the changed dates/topics remain | Controlled residual-update mutant; the Quiz still has its original two questions |

The Question aggregate's new version and the later event publisher version differ: they
are separate increments. The repaired embedded QuizQuestion stores the event version.
Raw event-table rows are retained history, not a count of unprocessed work. The propagation
horizon explicitly includes two selected-event attempts; the repaired second attempt is
an expected ineligible replay, not an application failure. Unscoped scheduled polling is
suppressed. No additional event-draining behavior is introduced into ScenarioExecutor.

The observations cover declared persistent projections and identities, not every field
in the application. The mutant changes only the compensation body in a copied build;
its checkpoint can report execution without a restoring data write. The original and
repaired Question consumer similarly can both return success, while only one persists
progress. Therefore completion/exception counters alone do not distinguish these cases.

These experiments motivated the three implemented ImpactV2 categories: deleted-target
dependencies, failed-operation residuals, and unresolved delivered events. The ordinary
executor now counts distinct affected aggregate identities with explicit attribution and
coverage. These earlier hand-driven experiments remain separate from persisted-scenario
qualification and do not establish coverage of all generated scenarios. The real baseline update defect must
be distinguished from injected-fault effects before evaluating search.

The experiment README and RESULTS.md own exact reproduction, selected projections,
source/build hashes, validation and limitations. Reports are under
`verifiers/target/impact-updates/`. Implementation sequencing and subsequent qualification are recorded in
[the roadmap](roadmap.md#proposed-route-to-potential-impact-scoring-in-ordinary-execution).

### Reproduce and inspect the experiment

The research harness lives in `verifiers/experiments/impact-three-cases/`, outside
production sources and normal generated FaultScenario execution. From the repository root:

```sh
docker compose run --rm --no-deps --pull never -T --entrypoint bash scenario-executor /verifiers/experiments/impact-three-cases/run.sh
python3 verifiers/experiments/impact-three-cases/analyze.py verifiers/target/impact-three-cases/run-1/*.json --output verifiers/target/impact-three-cases/analysis-run-1.json
python3 verifiers/experiments/impact-three-cases/analyze.py verifiers/target/impact-three-cases/run-2/*.json --output verifiers/target/impact-three-cases/analysis-run-2.json
python3 -m unittest discover -s verifiers/experiments/impact-three-cases -p 'test_*.py' -v
```

Its README owns harness operation and observation details; RESULTS.md records the exact
version chain, review and technical limitations. Reports and logs are under
`verifiers/target/impact-three-cases/`: `run-1/`, `run-2/`, `analysis-run-1.json`,
`analysis-run-2.json`, and `repeatability.json`. The original four-case validation is six successful
harness runs and twelve detector tests. The separate behavior follow-up adds six paired
probe runs; run `run-behavior-probes.sh` through the same Docker entrypoint to reproduce
them under `verifiers/target/impact-behavior-probes/`. Raw IDs, dates and timings are not claimed
byte-stable; repeatability concerns normalized findings. No LLM inference is part of
this method, and no production impact behavior changed.

## Current evidence

Each result below belongs to its named package, configuration, and execution scope.
The latest single-input qualification supersedes earlier single-input candidate totals;
older multi-Saga and event runs remain evidence only for their stated capabilities.
Paths are workspace-relative. Local `target/` artifacts are reproducibility outputs,
not a durable publication archive; preserve selected raw evidence before cleaning them.

| Question | Representative evidence | Interpretation |
| --- | --- | --- |
| How many ordinary single inputs are static candidates? | 665/796; 88 gained, zero lost | Latest nested-binding package; not 665 successful executions |
| Do the new nested bindings survive real execution? | Four fault-free Docker attempts: SUCCESS / EXACT | Exact course/question/topic identities and one target creation |
| Can an event reach its intended receiver? | Delivery, masking, and absent-receiver controls | One source-derived QuizAnswer fixture, not generic receiver synthesis |
| Has every latest candidate been preflighted? | No; older 402-workload scan plus seven targeted repairs | Not a full rerun on the latest package |
| Do the impact controls differ? | Candidate objects 0/1/1/0; exposures 0/1/0/1 | Final-state candidates, not established domain harm |
| What is the current verifier test result? | 763 tests, 48 suites, no failures/errors/skips | `verifiers/target/impact-v2-base/verifiers-full.log`; obsolete XML reports excluded |

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

Fresh setup-aware size-1–3 package: `verifiers/target/setup-translation-final-count-only/quizzes-20260904-120355-844/`, generated locally in 48 seconds after Docker stalled while fetching its Maven base image. It declares only `accounting`, `sagas`, `inputs`, and `interactions`; count-only wrote zero workload rows and no workload file. Counts are 68 Sagas; 36/32 with/without accepted inputs; 886 inputs; and 796/90 accepted/rejected. Materializable input recipes rise from 150 to 153. Direct interactions remain 785: 0 exact, 535 symbolic, and 250 type-only. Strict connected sets remain 382/3,594 for sizes 2/3, of which 35/42 have accepted positive input tuples. Fallback connected sets remain 547/7,190, of which 227/1,904 have accepted inputs.

The package contains 147 `relativeDateTime` recipes: 41 `PT5M`, one `PT25M`, 40 `PT1H5M`, and 65 `PT1H25M`. These represent the four Quizzes `DateHandler.toISOString(DateHandler.now()...)` forms currently recognized. Their compact `anchor: "now"` plus ISO-8601 `offset` shape survives package write/read and materializes relative to the executor's current time. Other date expressions remain blocked.

The retained baseline `verifiers/target/quizzes-20260902-011240-501/` reported 764 direct interactions (0/152/612 exact/symbolic/type-only), strict connected sets 140/1,299 with 63/400 accepted-input sets, and fallback sets 540/7,005 with 223/1,840 accepted-input sets. The change is semantic, not a count-direction target. For example, `CreateQuestionCommand` and `CreateQuizCommand` explicitly delegate a null aggregate root; their third call arguments name a course or course execution and are no longer mis-associated as Question or Quiz keys. Conversely, `AnswerQuestionCommand`, `RemoveQuestionCommand`, and getter-based `UpdateQuestionCommand` calls now preserve their declared semantic roots. Stronger type-level evidence therefore grows, while strict input-bound selection shrinks because missing contradiction is no longer treated as proof that two inputs name the same aggregate.

The accounting equations reconcile independently by size. All Saga sets remain `36 + 630 + 7,140 = 7,806` and selected sets remain `36 + 35 + 42 = 113`. Feature-local source tracking retains two previously collapsed RemoveTournament invocation points, so accepted inputs increase from 794 to 796. The corresponding all input-bound total is 1,258,442,130 and the selected total is 76,913, an increase of 2,640 schedules that reconciles as two singles, 438 pairs, and 2,200 triples. Of those selected workloads, 57,293 have source setup, one needs no setup, and 19,619 are blocked: 57,294 static setup candidates instead of 11,935. Count-only streams selected tuples and rejects incompatible partial tuples early; it does not enumerate the 1.258-billion baseline.

The strict size-1–4 run under `verifiers/target/setup-accounting-size4/quizzes-20260904-021908-623/` completed in about 15 minutes before the setup improvement. Size 4 then added 37 connected Saga sets and 321,120 selected input-bound workloads: 4,800 had source setup and 316,320 were blocked. It should be rerun before quoting current size-4 setup coverage.

Static package sizes are: accounting 21,429 bytes; Sagas 68 records / 59,931 bytes;
inputs 886 / 3,893,116 bytes; interactions 785 / 487,506 bytes; manifest 482 bytes.

### Bounded workload-driven source setup qualification

The earlier full-cap size-1–3 count-only package under
`verifiers/target/m2-count-only/quizzes-20260903-205314-778/` is the pre-translation
baseline: 794/90 accepted/rejected inputs, 150/644 materializable/blocked inputs, 785
direct interactions (0 exact, 535 symbolic, 250 type-only), 382/3,594 strict connected
size-2/3 sets, 35/42 such sets with accepted positive tuples, and
74,273/1,247,308,000 selected/all input-bound totals.

The refreshed bounded stable-order writer package under
`verifiers/target/setup-translation-final-write/quizzes-20260904-121227-002/` contains
1,620 WorkloadPlans and 6,066 eager FaultScenarios. Of the WorkloadPlans, 1,338 reference
one of 311 source-derived setups: 210 singles, 444 pairs, and 684 triples. The earlier
bounded writer had 408 setup-bearing WorkloadPlans and 94 setups. The attachment
mechanism has no participant-count branch: the configured generator cap decides which
sizes are enumerated. One setup action occurrence is retained once even when several
participant bindings consume its result, and no candidate is formed by joining different
test contexts. The package shapes are unchanged.

This writer run used `maxInputVariantsPerSaga=10`, sizes 1–3, one schedule and one
recovery schedule, and `maxCatalogScenarios=50000`; stable enumeration exhausted at
1,620 workloads before the workload cap. An attempted writer run with the full 1,000
input variants was stopped before artifact writing after 8m42s while scanning the
then-current all-input Cartesian space. Accordingly, 1,338 is bounded catalog evidence,
not a claim about every qualifying Quizzes combination. The full-cap count-only package,
not the reduced-cap writer, remains the accounting authority.

Docker preflight used an isolated current-shape projection of the existing
RemoveTournament/AddParticipant pair and one natural triple combining AnonymizeStudent,
GetCourseExecutionById, and RemoveStudentFromCourseExecution. The report under
`verifiers/target/m2-preflight/pair-triple-preflight-report.json` returned `SUCCESS` for
two candidates and five participants: the pair completed 12/12 actions and 4/4 bindings,
and the triple completed 4/4 actions and 5/5 bindings; every participant reached
`MATERIALIZED` and `STARTUP_READY`. This proves runtime setup readiness only for those
two representatives.

### Bounded current executable package

`verifiers/target/m4-final/quizzes-source-package/` was generated by the focused `SourceDerivedSharedSagaWorkloadAnalysisSpec` opt-in writer. Before dynamic enrichment it contained 68 Sagas, 847 inputs, 764 interactions, one 12-action source-derived setup, one RemoveTournament/AddParticipant WorkloadPlan with five exact Saga-local step occurrences, 13 eager FaultScenarios, and an empty request stream.

Docker preflight used `PACKAGE_PATH=/reports/m4-final/quizzes-source-package/scenario-catalog-manifest.json`, `PREFLIGHT=true`, and `OUTPUT_PATH=/reports/m4-final/source-preflight-report.json`. It returned `SUCCESS`, one candidate, two participants, one successful source setup, all 12 actions successful, four exact participant bindings resolved, and `SETUP_READY`.

One on-demand request persisted workload `12f7f358f8c3c04a6541a8a86450639089e1922c355206871fc2df6047c68417`, vector `10100`, and effective cap 20. It reported one uncapped/written schedule and added scenario `2dcaa575630b3c8e7bba41f27ef0717b5bc05a2a0f4df8c7d65f6965b783260a`. A fresh Docker execution selected that persisted id and returned `PARTIAL_COMPENSATED / EXACT`; ImpactV1 evaluated zero findings. Reports and full logs are under `verifiers/target/m4-final/`.

A later twelve-scenario pair/triple check under `verifiers/target/execution-baseline/`
confirmed setup success for every attempt. All six pair scenarios and two triple scenarios
reached impact evaluation. Four triple scenarios stopped at event replay because
`QuizAnswerEventHandling` and `TournamentEventHandling` both expose
`handleAnonymizeStudentEvents`, while the compact package no longer preserves the exact
selected `EventHandling` class. The executor reported `EVENT_REPLAY_CONTROL_FAILED`
instead of guessing between them.

The corrected package under
`verifiers/target/event-route-proof/quizzes-20260904-021015-239/` preserves exact fully
qualified `eventHandlingClass` and handler identities. The reader restores those values
and execution loads the exact classes rather than searching by method or simple name;
route ordering and ids were unchanged in that repair. The four event-reaching triple
controls reported `SELECTED_SUBSCRIBER_NOT_FOUND`; their setup creates only CourseExecution
and User state. A later export correction (below) found that workload-local route indices
could still point at the wrong catalogue consumer. These historical reports demonstrate
missing subscriber state for the restored route, but do not establish the intended
consumer identity. Regenerate before requalifying those exact triples.

Exact nested-property reconstruction and feature-prefix setup have bounded runtime
proof in addition to the latest nested whole-result qualification below. Three FindQuiz
inputs resolved `quiz.aggregateId` through their retained Tournament result and passed
Docker preflight. Three feature-prefix inputs passed after reproducing earlier user
creation, enrollment, or other source actions. StartQuiz and LeaveTournament prefixes
also passed after explicit dispatcher registrations were added. These checks establish
input reconstruction/startup, not replay of their complete source test methods.

The source fixes preserve pre-call DTO mutations and stop setup before the exact measured
target occurrence. Earlier count-only stages are superseded by the latest ordinary
single-input measurement below. Commands, selected IDs, and reports for these distinct
runtime proofs remain in `issues/2026-09-04-complete-source-derived-setup/FINAL-HANDOFF.md`
and `verifiers/target/astra-qualification/`.

The complete preflight of the earlier package
`event-route-proof/quizzes-20260904-021015-239` ran 402 candidate workloads in isolated
workers. Its saved combined report retains 395 as ready and changes seven successful
state-only setups to `FRESH_STATE_ISOLATION_FAILED` because the old parent rejected an
empty binding list. The corrected parent was then exercised in Docker against exactly
those seven persisted workloads: all seven returned `SETUP_READY`, with seven started
participants, explicitly empty binding arrays, and unchanged package hashes. The bounded
rerun took 109.23 seconds; evidence is under `verifiers/target/astra-qualification/`
(`state-only-preflight.json`, log, selection file, and qualification-only driver).
This is a seven-workload regression proof, not a fresh 402-workload or latest-package
preflight. The historical full report remains 395/402.

### Source-derived event receiver qualification

The ordinary size-one package at
`verifiers/target/astra-event-receiver/generated/quizzes-20260904-222711-381/`
predates the latest nested participant extension. Its 944 written workloads include
event/provider variants and are not the ordinary-input denominator. Its candidate totals
are superseded by the nested-binding measurement below. All nine files match a
second generation byte-for-byte and remained unchanged after Docker execution.

The executable exporter now resolves selected event references against the authoritative
Saga route catalogue, scoped by participant Saga, exact step/emission and consumer
semantics. A workload selecting only the second consumer references `event#0-route#1`
rather than incorrectly reusing `event#0`. Missing/ambiguous matches fail export; no
record shape changes. Earlier exports affected by this projection must be regenerated.

The Quizzes closed setup dispatcher now registers ten exact methods, adding `createQuiz`,
`startQuiz`, and `addParticipant`. These covered every source-setup signature in that event qualification's package;
method coverage alone does not establish setup or measured-action success.
The earlier blocker partition, now superseded for the nested DTO cohort, is in
`issues/2026-09-04-event-receiver-setup/DIAGNOSIS.md`. The existing ordinary
`QuizAnswerEventHandlingTest#AnonymizeStudentEvent updates the student name in the quiz answer`
already supplies an eight-action fixture creating a course execution, active enrolled
student, question, quiz, and started quiz answer with shared identities. No descriptor,
provider, manual package editing or cross-test fixture synthesis supplies this setup.

Three fresh Docker executions of persisted scenarios proved:

| Case | Result | Event evidence |
| --- | --- | --- |
| QuizAnswer route, vector `00` | `SUCCESS / EXACT` | One event delivered to QuizAnswer subscriber 7; publisher CourseExecution 2, selected student 3 |
| Same route, vector `01` | `COMPENSATED / EXACT` | `MASKED_BY_TRIGGER_FAULT`; no delivery receipt |
| Tournament route, vector `00`, same fixture | `UNEXPECTED_EXECUTION_FAILURE / INCOMPLETE` | `SELECTED_SUBSCRIBER_NOT_FOUND`; exact Tournament handler retained, no fallback to existing QuizAnswer |

All three executed the source setup successfully, cleared one pending setup event and
proved an empty event baseline before measurement. The two previously blocked StartQuiz
and LeaveTournament prefix examples also passed isolated preflight in 29.76 seconds,
clearing one and three pending events respectively. This is bounded setup/replay evidence,
not full qualification of the latest candidates or a new harmful/zero-impact comparison.
No ImpactV1 output was requested for this qualification.

Evidence, selected persisted IDs, commands and independent review are in
`issues/2026-09-04-event-receiver-setup/`; logs and reports are under
`verifiers/target/astra-event-receiver/`. The latest full verifier result is recorded
below. Five focused Quizzes tests passed: two dispatcher tests
and three existing receiver-behavior features.

### Nested participant setup-result qualification (2026-09-05)

Participant binding now traverses existing constructor, assignment, collection and
supported-transform recipes to retain whole results from exact earlier setup actions.
For example, a QuestionDto can retain a previously created TopicDto in its set, and a
QuizDto can retain a previously created QuestionDto in its list. A whole-result reference
is authoritative; its historical construction recipe is not evaluated again. Missing,
conflicting, mistyped or omitted producers fail closed instead of falling back to an
unbound value. Identical traces of the same source occurrence remain deduplicated.

When the measured target itself occurs in `setup()` or a supported setup helper, its
setup plan stops before that exact target occurrence. Missing or ambiguous target
occurrence metadata cannot fall back to replaying the complete fixture. Existing root
property bindings remain supported; new nested scalar property-result collections,
including the deferred Tournament patterns, remain outside this extension.

The ordinary package under
`verifiers/target/astra-nested-bindings/final-generated/quizzes-20260905-125205-380/`
contains **665 of 796 static candidates: 664 source setup, one without setup, 131 blocked**.
Independent input-ID comparison against the previous 577-candidate package finds
**88 gained and zero lost: 85 CreateQuestion and three CreateQuiz inputs**. Intermediate
measurements that included nested scalar property collections are superseded by this
final scoped result. This is static candidacy, not 665 successful executions.

Four fresh Docker executions selected unchanged persisted fault-free scenarios: the
CreateQuiz inputs from StartQuizTest, StartQuizCompensationTest and
QuizAnswerEventHandlingTest, plus the CreateQuestion input from StartQuizTest. All four
returned `SUCCESS / EXACT`, successful source setup and an empty pending-event baseline.
A qualification-only observer checked persisted course identities and exact nested
question/topic identities against setup action results, and verified the target was
created exactly once. No application-specific impact rule or runtime authorization was
added for this proof.

All nine package files were byte-identical on repeat generation and remained unchanged
after Docker qualification. The full verifier test result and stale-report correction
are recorded in [regression proof](#regression-proof).

The exact issue and qualification evidence are in
`issues/2026-09-04-nested-participant-setup-bindings/` and
`verifiers/target/astra-nested-bindings/`.

### Missing-input audit (2026-09-05)

The latest nested-binding package still has 68 Sagas: 36 with accepted inputs and
32 without. A read-only audit covered all 62 Groovy and six Java test files under
`applications/quizzes/src/test`. None of these 32 Sagas has even a rejected row in that
package's `inputs.jsonl`; the recorded input-policy/source-mode rejections therefore
do not explain this cohort.

| Finding | Sagas | Meaning and next useful action |
| --- | ---: | --- |
| No relevant direct invocation found in the inspected tests, after separating event routes | 19 | Includes AnswerQuestion and ConcludeQuiz. Select useful user behavior and establish a working application test before expecting the verifier to extract an input. This is not proof of no coverage outside the inspected scope. |
| Indirect event-consumer path identified | 12 | Includes removing a user from an attempt and updating a Tournament participant's answer. Static event routing shows how these paths can be reached, not that a test executed them. Qualification needs the producing event and the exact receiver's state. |
| Direct invocation exists but no extracted input | 1 | The synchronous UpdateQuestionTopics call appears inside the benchmark loop in UpdateQuestionTopicsAsyncTest. Its asynchronous counterpart has an accepted input. Diagnose the missing extraction before adding a heuristic; its exact root cause is not yet established. |

These categories use event-route evidence first, then direct test calls, then absence
of such calls. An event consumer can also lack a direct test call; the categories are
an audit partition, not mutually exclusive concepts. The strongest extraction example
is `UpdateQuestionTopicsAsyncTest.groovy` in the test package
`pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/coordination/question`: it prepares
`benchmarkTopicIds`, creates a fresh synchronous Question in the loop, and invokes
`questionFunctionalities.updateQuestionTopics(syncQuestion.getAggregateId(), benchmarkTopicIds)`.

The AnswerQuestion/ConcludeQuiz finding matters for impact research: the existing
StartQuiz tests do not establish that submitting or concluding the created attempt
works. The new paired answer probe independently exposed an application defect before
it could distinguish compensated and healthy states. Fixing that path is separate
application work, not an automatic verifier coverage gain.

Audit inputs are the latest package under `verifiers/target/astra-nested-bindings/`
and its `final-generation.log` event-bridge evidence. Accounting reports 886 analyzed
inputs (796 accepted, 90 rejected); the raw input file also includes four additional
provider/prerequisite entries for already covered Sagas. Those entries do not alter
the 36/32 partition or the 665 static-candidate result. There is no dynamic observation
artifact in this static package, so the audit makes no runtime-coverage claim.

### Bounded current dynamic smoke

The focused RemoveTournament/AddParticipant package described under bounded executable evidence was enriched by one host invocation of `DynamicEnrichmentOrchestrator` selecting only `RemoveTournamentAddParticipantRecoveryWindowExploratoryTest`. Maven ran five features with zero failures. The current input-map rerun is under `verifiers/target/input-map-fix/quizzes-source-package/`. It has 1,038 observations (188 step-started, 188 step-finished, 422 command-sent, 239 aggregate-accessed, one invariant violation) and 10 attribution groups (2 `exactInput`, 8 `shapeOnly`). Unique input evidence is 2 exact, 0 test-and-shape, 0 shape-only; its only workload remains `allInputsObservedInOneCommonTest`.

Before the repair, the equivalent run produced 0 exact, 2 test-and-shape, and 8 shape-only groups because the verifier wrote `workloadPlanIds` while the simulator expected `scenarioPlanIds`. The simulator now reads `workloadPlanIds`; the raw event JSONL was removed after successful publication, while the input map, Maven log, test reports, and normalization diagnostics remain outside the package.

That focused enriched package's role sizes are: accounting 22,015 bytes; Sagas 68 / 90,446; inputs 847 / 3,959,368; interactions 764 / 402,946; setups 1 / 9,643; workloads 1 / 1,050; FaultScenarios 14 / 3,792; requests 1 / 262; observations 1,038 / 725,795; attributions 10 / 8,991; manifest 1,226 bytes. Every declared SHA-256 matched, and the current reader revalidated all cross-file references after dynamic publication.

### Regression proof

Local consolidation was validated in the primary checkout with JDK 21: **775 verifier
tests**, **136 simulator tests**, and **17 focused Quizzes tests**, all passing without
failures, errors or skips. Logs are in
`verifiers/target/local-consolidation-validation/{verifiers,simulator,quizzes-focused}.log`.
The complete verifier run includes the final dedicated event-receiver fixture; its
nested-input cohort assertions now account for that fixture's additional Question and
Quiz while checking their exact source provenance. The Quizzes run is a selected suite,
not a claim that every application test was executed.

Focused qualification covers exact producer identity and target exclusion, typed nested
bindings, package determinism, route identity, state-only preflight, and real application
setup/execution as described above. The Docker impact experiments are separate runtime
evidence and are not added to either unit/regression-suite total.

## Current limitations

- Thirty-two discovered Quizzes Sagas still lack accepted static inputs. This does not mean no tests exist; their invocation/value shapes remain unclassified or unsupported.
- Event-expanded setup can prepare a receiver when the existing source fixture already contains it, as the qualified QuizAnswer example proves. It does not infer receiver-only state or compose independent test contexts. Earlier triple route exports need regeneration before exact-consumer requalification.
- The seven state-only setups previously rejected by the preflight parent now pass a targeted Docker rerun. The old complete report and newer package generations still need to be distinguished; the targeted repair does not qualify every newer workload.
- Two Quizzes steps retain focused static-analysis limitations: one unresolved `SagaCommand` payload and one unresolved dispatch through a helper `send` call. Unsupported aggregate-root expressions remain keyless and can enter only the configured fallback lens.
- Event-consequence extraction supports one conservative direct producer shape and one unique local consumer. Wrong receiver or unit-of-work binding, mixed compensation-origin emission, conditional/repeated consumer delegation, multiple/repeated/conditional producer emissions, fan-out, recursion, nested event chains, and unresolved routes are rejected diagnostically.
- Four observed Quizzes forms of `DateHandler.toISOString(DateHandler.now()...)` are materializable as a relative `now` plus offset. Setup translation now also handles the observed `Arrays.asList(...)`, bounded string concatenation, and `QuizDto` shapes. Other expressions remain blocked rather than being guessed.
- Static setup candidacy is conservative prediction. The newly attached setups have full static validation, but broad runtime preflight has not yet been repeated for them.
- The setup dispatcher admits only explicitly registered signatures. The known StartQuiz and LeaveTournament gaps are fixed and their bounded preflight passes; unregistered signatures still fail closed. The parent preserves validated expected failure reports from nonzero workers; crashes, absent or malformed reports and mismatched identities remain invalid attempts.
- Repeated same-participant runtime step names are structurally rejected because current Saga/local runtime state is keyed by step name rather than occurrence id.
- Segment compression preserves conflict-anchor order cases under extracted evidence; it does not prove every semantically distinct runtime interleaving is retained.
- Dynamic enrichment remains local/Saga-focused. The fresh one-class smoke resolves 2 of 10 Saga-invocation groups exactly; the other 8 remain shape-only, and 933 observations still lack a uniquely resolved Saga and Saga-local step.
- The historical generated Quizzes event-consequence pair provided one ImpactV1 1/0 discrimination; it needs current-package regeneration before new execution claims. The automatic source-derived RemoveTournament–AddParticipant path proves one harmful/control pair without its descriptor/provider; the retained historical benchmark still provides the complete 34-row 19/15 landscape. ImpactV1 was zero for every historical Remove/Add row. The implemented ImpactV2 contract now measures three generic evidence categories; the historical labels remain separate application-rule evidence.
- Three refreshed benchmark controls demonstrate the explicitly marked zero-bit domain-fallback path; other fallback shapes remain unqualified.
- Persistent-environment reset is the caller/orchestrator's responsibility.
- On-demand mutation is process-local and filesystem-local. Covered publication failures roll back byte-for-byte, but network filesystems, multi-host coordination, and abrupt host/process death are not qualified.

## Not implemented

- Generic execution for every generated WorkloadPlan/FaultScenario shape.
- TCC, stream, gRPC, distributed, causal, or true-concurrent execution parity.
- Compensation faults, delay injection, non-binary impairments, or automatic recovery retry/backoff.
- Semantic deduplication of value-equivalent inputs.
- Profile-aware resolution for ambiguous multiple `@Service` implementations.
- A universal domain-correctness oracle, generic serial-comparison oracle, or automatic continuation-probe impact model. ImpactV2 already assesses its three bounded final-effect conditions.
- Generic batch execution qualification, generic reset orchestration beyond fresh process workers, GA/local fault search, or scenario prioritization. The current benchmark command remains application-specific.

## Safe thesis framing

Safe current claim:

> The verifier derives deterministic fault experiments from Saga application source and
> tests, reconstructs supported prerequisite state, and replays selected persisted
> scenarios in an isolated Saga/local runtime. Its ImpactV2 assessment reports three
> application-independent potential-impact conditions using supported persistent state,
> write attribution and event evidence, with explicit observation coverage. The latest
> Quizzes qualification covers 29 benchmark schedules and 30 broader control/fault pairs.
> These results demonstrate the implemented conditions within that scope; they do not
> establish severity, universal domain harm or coverage of every executable scenario.
> Methodological interpretation with the advisor, broader claims where justified, and
> automated search evaluation remain future work.
