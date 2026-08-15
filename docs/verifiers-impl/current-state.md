# Verifier current state

Last updated: 2026-08-01

This is the canonical handbook for verifier and fault-analysis scenario work. It owns the current conceptual model, terminology, supported operations, latest representative evidence, reproduction commands, and limitations. [`roadmap.md`](roadmap.md) owns future direction. [`decisions/`](decisions/index.md) explains the few design choices whose rationale is not obvious from current behavior.

## The short version

The verifier turns Saga application source and tests into deterministic fault experiments:

```text
Java application code + Groovy/Spock tests
  -> static Saga, step, interaction, and input extraction
  -> WorkloadPlans
  -> FaultScenarios
  -> optional runtime-evidence attribution
  -> setup preflight or one-scenario execution
  -> optional invariant-impact result
```

The verifier does **not** prove that an application is correct. It currently answers narrower questions:

1. Which Sagas, steps, aggregate footprints, and test-derived inputs can be found?
2. Which reproducible workload and fault schedules can be generated from those facts?
3. Which generated inputs are static setup candidates, and which can actually start in the current Saga/local runtime?
4. What happened when one persisted FaultScenario was replayed?
5. Did that attempt trigger an observed aggregate-invariant rejection?

The current high-level Quizzes result is:

```text
discovered Sagas:                 68
Sagas with accepted inputs:       36
Sagas without accepted inputs:    32
accepted inputs / WorkloadPlans:  732
static setup candidates:          82
runtime setup-ready:              82 / 82
FaultScenarios for those plans:   164
```

The `82/82` result proves exact persisted argument materialization and Saga startup for those candidates in one Quizzes `test,sagas,local` runtime. It does not prove that every all-zero or faulty execution succeeds. [The exact command and report are below](#quizzes-setup-preflight-8282).

## Reading order

Use this page by question:

- [What the main terms mean](#the-essential-terms)
- [What inputs are analyzed](#inputs-and-static-extraction)
- [What the five v4 files contain](#the-v4-package)
- [What the accounting metrics mean](#how-to-read-scenario-space-accounting)
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

The v4 manifest stores this as `workloadMaterializability[].materializable=true`. Read `materializable` as **setup candidate**, not runtime proof. Eager all-zero and single-point FaultScenario generation uses this gate.

### Runtime setup-ready

A workload is **setup-ready** only when ScenarioExecutor, in a real application context, materializes the exact persisted argument tuple and starts every exact Saga participant. Setup preflight reports this as `SETUP_READY`.

Preflight deliberately runs no forward, fault, compensation, or commit action. Setup-ready therefore predicts neither domain success nor fault behavior.

### WorkloadPlan

A **WorkloadPlan** is reusable normal-execution structure. It contains:

- one or more Saga participants and their accepted inputs;
- one deterministic global forward schedule;
- one dense normal-action schedule containing forward actions and any event consequences;
- persisted event-consequence definitions and their selected producer/consumer routes;
- optional prerequisite-provider identity and typed baseline-binding requirements;
- conflict evidence;
- ordered forward fault slots;
- compensation checkpoints and evidence;
- stable occurrence and plan identities.

It does not contain an assigned fault vector or one chosen recovery ordering. `forwardSchedule` remains the only source of fault slots, vector bits, and compensation checkpoints.

### FaultScenario

A **FaultScenario** is one reproducible experiment. It references one WorkloadPlan and adds:

- one assigned binary vector aligned with the WorkloadPlan fault slots;
- one complete ordered action sequence containing `FORWARD`, `EVENT_CONSEQUENCE`, and, when applicable, `COMPENSATION` actions;
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

## Inputs and static extraction

### Inputs consumed

Static generation consumes:

- Java production source from a simulator application;
- Groovy/Spock tests and fixtures;
- verifier generation configuration such as source policy, schedule strategy, caps, and seed.

Optional dynamic enrichment additionally runs selected application tests and consumes simulator runtime evidence. ScenarioExecutor additionally consumes a complete v4 package, target application classpath/Spring context, and one persisted FaultScenario id or the preflight mode.

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

Accepted inputs embed `microservices-simulator.input-recipe.v2` under `WorkloadPlan.acceptedInputs[].inputRecipe`. Recipes describe argument construction as nodes such as:

- typed literals and collections;
- constructors and ordered setter/property assignments;
- helper results reduced to nested recipes;
- property accesses and supported calls/transforms;
- explicit placeholders;
- typed `baseline_binding` nodes supplied by an exact prerequisite provider;
- unresolved nodes with blockers.

The executor materializes a supported subset. Runtime-owned arguments currently include `SagaUnitOfWorkService`, `CommandGateway`, and a fresh `SagaUnitOfWork`. A baseline binding is materializable only when the persisted provider id/version is present and the provider returns the required key with the persisted exact type. Applications may declare bounded prerequisite workloads in `src/test/resources/verifier-prerequisite-scenarios.json`; the generic adapter resolves the named Saga steps and exact event route without application FQNs in verifier production code. The matching provider may live on the application's test classpath. Unsupported calls and unresolved source values remain blockers.

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

Rejected inputs remain in `workload-catalog-rejected-inputs.jsonl` with provenance, source-mode evidence, recipe, warnings, and rejection reason. TCC execution remains out of scope.

## The v4 package

V4 is the current package and executor contract. V3 records are rejected rather than upgraded in place; older `ScenarioPlan` and `scenario-catalog.jsonl` artifacts are also unsupported.

A normal generation run writes exactly five contract files:

| File | Purpose | Main consumer |
|---|---|---|
| `scenario-catalog-manifest.json` | Package entry point: paths, schemas, hashes, configuration, counts, setup candidates, and recovery cap | Package readers, preflight, executor, on-demand writer |
| `workload-catalog.jsonl` | Deterministic `microservices-simulator.workload-plan.v4` records | Dynamic sidecars, FaultScenario generation, executor |
| `fault-scenario-catalog.jsonl` | Deterministic `microservices-simulator.fault-scenario.v4` records | Executor and on-demand vector workflow |
| `scenario-space-accounting.json` | `microservices-simulator.scenario-space-accounting.v4` workload-space, setup-candidate, vector, recovery-schedule, and event-consequence accounting | Thesis evaluation and on-demand accounting updates |
| `workload-catalog-rejected-inputs.jsonl` | Inputs excluded by source mode or policy, with diagnostics | Input-coverage debugging |

The manifest uses `microservices-simulator.scenario-catalog-manifest.v4`; input recipes use v2. `analysis-report.html` is no longer produced. It was a pre-v3 static trace browser rendered before WorkloadPlan/FaultScenario generation and had no current package, setup, execution, or impact content.

The manifest is the package entry point and checksum boundary. The executor validates the complete package before selection. Preflight, execution, impact, dynamic sidecars, and logs are outside the five-file semantic package and must not change its bytes.

### Determinism and bounds

The generator preserves stable ordering, deterministic ids, explicit configuration, and an explicit seed. `maxCatalogScenarios` is one total exported-workload cap: descriptor-selected prerequisite workloads are ordered deterministically and reserved first, then remaining capacity is filled from stable base-workload order. Manifest generated/selected/capped/exported counts distinguish those stages, and the final workload catalog never exceeds the configured cap. If event-consequence expansion reaches its reserved base-workload capacity between base records, the manifest records the cap encounter and number of remaining base workloads omitted at that stage instead of silently exiting. Bounded defaults and caps prevent accidental materialization of the full combinatorial space.

Implemented generation choices include:

- `BRUTE_FORCE` or `INTERACTION_PRUNED` workload selection;
- single-Saga and bounded multi-Saga workloads;
- `SERIAL`, bounded order-preserving, and `SEGMENT_COMPRESSED` forward scheduling;
- eager all-zero and single-point vectors for static setup candidates;
- guarded on-demand persistence for arbitrary valid multi-fault vectors;
- bounded recovery schedule materialization with exact uncapped counts for computed vectors.

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
  [--recovery-schedule-cap <must-match-package-cap>]
```

The manifest path, WorkloadPlan id, and vector are required. If the optional recovery cap is supplied, it must match the package cap. The result status is `PERSISTED`, `DEDUPLICATED`, `REJECTED`, `INTEGRITY_FAILURE`, or `PERSISTENCE_FAILED`; only the first two are successful.

Concurrent local JVM writers serialize. This is not crash-atomic storage: the three semantic files are promoted separately, so abrupt process/host failure can leave a checksum-invalid package. Regenerate after such an integrity failure. The local `FileChannel` contract does not claim network-filesystem or multi-host coordination.

## How to read scenario-space accounting

`scenario-space-accounting.json` mixes operational counts, thesis evaluation, and diagnostics. Only a small subset belongs in headline status.

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

- `allInputBound`: bounded baseline over compatible accepted inputs and schedules;
- `selectedByGenerator`: subset selected by the configured generation strategy;
- `catalogWritten`: records actually materialized after write mode and caps.

They can be identical for a simple single-Saga uncapped run. They differ when interaction pruning, count-only mode, or catalog caps matter. Do not present three equal values as three independent achievements.

### Strict and broad interaction lenses

These are report/evaluation lenses for uncertainty in static aggregate binding:

- **strict interaction estimate** uses stronger static conflict evidence;
- **broad interaction estimate** additionally permits weaker type-level/unknown-key fallback evidence and is therefore a conservative upper estimate with more possible false positives.

The two counts do not represent runtime modes. They do not themselves select records; generation configuration such as `generationStrategy` and `allowTypeOnlyFallback` does.

They are useful only when evaluating how uncertain aggregate-key extraction affects multi-Saga pruning. For the latest `maxSagaSetSize=1` setup package, strict/broad pair counts do not explain the emitted single-Saga catalog and should not be treated as headline metrics.

### Segment-compressed scheduling

`SEGMENT_COMPRESSED` identifies cross-Saga conflict-anchor steps, groups each Saga's preceding non-anchor run with its next anchor, and interleaves those segments while preserving in-Saga anchor order. Non-anchor tails are appended once in deterministic order.

It reduces permutations of internal/non-conflicting steps while retaining conflict-anchor order cases under the verifier's static conflict evidence. It is not proof of semantic completeness or exact runtime aggregate-instance binding. See [`decisions/2026-06-16-conflict-anchor-segment-compression.md`](decisions/2026-06-16-conflict-anchor-segment-compression.md).

### Recovery accounting scope

Recovery totals are exact only for vectors that were actually computed eagerly or on demand. `exactComputedSumsScope=EXACT_SUM_OVER_COMPUTED_VECTORS_ONLY` and `allVectorRecoveryTotalStatus=NOT_COMPUTED` prevent an all-vector claim.

A synthetic high-cardinality fixture currently proves exact count `118264581564861424` while retaining only `20` schedules. This is evidence for bounded exact counting, not evidence that every vector in every workload was enumerated.

## Optional dynamic evidence

Static analysis is strong at structure but weaker at exact runtime identity. Dynamic enrichment asks:

> Which static InputVariant and WorkloadPlan does this observed runtime test activity belong to?

When enabled, the verifier runs selected tests with simulator evidence hooks and writes additive artifacts:

```text
workload-dynamic-evidence.jsonl
workload-dynamic-evidence-manifest.json
dynamic-evidence-join-report.json
dynamic-evidence/                 # raw events, input map, test reports, Maven log
```

These artifacts are not part of v4 package identity and do not rewrite WorkloadPlans, FaultScenarios, vectors, or action schedules.

A run-level `dynamic-input-map.json` lets runtime events carry an exact static `inputVariantId` when test identity, functionality class, step, and ownership resolve uniquely. Current join statuses are:

| Status | Meaning |
|---|---|
| `MATCHED_EXACT` | Runtime evidence directly carried an input id owned by the WorkloadPlan |
| `MATCHED_HIGH_CONFIDENCE` | Test and semantic shape matched without a direct id |
| `MATCHED_PARTIAL` | Some relevant shape matched, but confidence is insufficient |
| `AMBIGUOUS` | Multiple candidates remain; the verifier refuses to guess |
| `UNMATCHED` | Relevant evidence exists but cannot be joined usefully |
| `NOT_COVERED` | No useful runtime evidence was observed for the workload |

These statuses currently support attribution quality and debugging. They do not change execution behavior. The latest broad Quizzes counts are historical v2 evidence and are intentionally not retained as a current v4 headline. Current v4 package/sidecar immutability is covered by integration tests; a fresh broad Quizzes v4 enrichment run has not yet been recorded.

The durable static/dynamic boundary is explained in [`decisions/2026-04-28-hybrid-static-dynamic-key-binding.md`](decisions/2026-04-28-hybrid-static-dynamic-key-binding.md).

## ScenarioExecutor

ScenarioExecutor is a narrow deterministic Saga/local replay path, not a generic distributed runner.

### Setup preflight

Preflight selects every manifest row whose legacy `materializable` value is `true`. In one Spring application context, it:

1. resolves runtime-owned dependencies;
2. materializes each exact persisted input tuple;
3. creates a fresh Saga unit of work;
4. starts every exact Saga participant;
5. stops before every workflow action.

Result meanings:

- `SETUP_READY`: exact materialization and Saga startup succeeded;
- `MATERIALIZATION_FAILED`: a persisted input could not be reconstructed;
- `STARTUP_FAILED`: arguments materialized, but exact Saga construction/startup failed.

Normal execution uses the same setup implementation, so a separate preflight is optional.

### Normal execution

Normal execution requires:

- a complete v4 package path;
- one exact persisted FaultScenario id;
- an output path;
- an application classpath/Spring application with supported Saga/local runtime dependencies.

It sequentially replays persisted `FORWARD`, `EVENT_CONSEQUENCE`, and `COMPENSATION` actions, injects assigned faults at their exact forward slots, and commits each participant after its final successful forward action. Before measured execution, an optional exact `ScenarioPrerequisiteProvider` creates the baseline, resolves typed bindings, clears prerequisite-created pending events, and proves an empty pending-event baseline. This setup has separate report evidence and is excluded from measured actions, fault allocation, recovery, conformance, and ImpactV1.

Only a zero-bit body/commit failure explicitly marked with the simulator `DomainFailure` contract may use immediate checkpoint recovery, skip the failed participant's remaining forwards, continue valid survivor actions, and report `DEVIATED`.

Unmarked failures—including plain `SimulatorException`, service unavailability, ordinary runtime failures, missing infrastructure, and leaked assigned-fault exceptions—are infrastructure failures. They run no fallback, stop survivor execution, and report `INCOMPLETE` after measured execution starts. Thrown compensation actions also hard-stop.

### Event replay

Replay mode is activated before Spring startup. The simulator captures the exact event only after persistence, suppresses unscoped scheduled polling, and allows one selected event id through one persisted `EventHandling` bean method and one eligible subscriber. The executor invokes that real Spring bean synchronously outside the fault-vector boundary and before the next outer action.

An event consequence is masked when its specific trigger occurrence has a pre-body assigned fault, fails before capturing a matching event, or is not reached. If the trigger body or commit captures the selected event and then fails, execution hard-stops as `TRIGGER_FAILED_AFTER_EVENT_EMISSION`; the event is not dispatched and ImpactV1 is not evaluated. This prevents an emitted-but-undelivered event from becoming a false zero. Missing or multiple matching events/subscribers, selected-route mismatch, recursive registration, replay-control failure, and handler failure also hard-stop measured execution and leave ImpactV1 not evaluated. Replay currently supports one exact local subscriber only—no fan-out, recursion, nested event chain, retry, TCC, remote, stream, or gRPC delivery.

### Report

`microservices-simulator.scenario-execution-report.v5` records:

- attempt, package, WorkloadPlan, FaultScenario, vector, and fault-provider identity;
- separate prerequisite-provider, binding, cleanup, and baseline evidence;
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
- exact prerequisite providers and typed baseline bindings outside measurement;
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
- generic persistent-environment reset;
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

Evidence here is intentionally current and representative. New baselines replace these sections rather than being appended as historical chronology.

### Quizzes setup preflight: 82/82

The current single-Saga package is:

```text
verifiers/target/outcome2-helper-tracing/quizzes-20260727-180306-391/
```

Its manifest records:

```text
accepted inputs / WorkloadPlans: 732
static setup candidates:         82
statically blocked workloads:    650
eager FaultScenarios:            164
```

The successful one-context preflight was run from the repository root. The `tee` suffix below makes the saved container log reproducible; ScenarioExecutor itself writes only `OUTPUT_PATH`:

```bash
set -o pipefail
MEDIUM_MEM_LIMIT=3g \
PACKAGE_PATH=/reports/outcome2-helper-tracing/quizzes-20260727-180306-391/scenario-catalog-manifest.json \
OUTPUT_PATH=/reports/integral-numeric-restoration/setup-preflight-report.json \
docker compose run --rm -T \
  -e PREFLIGHT=true \
  -e FAULT_SCENARIO_ID= \
  -e JAVA_TOOL_OPTIONS=-Xmx2500m \
  scenario-executor \
  2>&1 | tee verifiers/target/integral-numeric-restoration/scenario-executor.log
```

Evidence files have different ownership:

```text
direct executor output: verifiers/target/integral-numeric-restoration/setup-preflight-report.json
captured stdout/stderr:  verifiers/target/integral-numeric-restoration/scenario-executor.log
derived checks:          verifiers/target/integral-numeric-restoration/verification-summary.json
```

`verification-summary.json` was derived after the run; the wrapper does not create it. The `82/82` value comes directly from the report. The `164/164` linkage and package-immutability statements were checked separately. A reproducible inspection shape is:

```bash
report=verifiers/target/integral-numeric-restoration/setup-preflight-report.json
package=verifiers/target/outcome2-helper-tracing/quizzes-20260727-180306-391

jq '{terminalStatus,candidateCount,participantCount,
     statuses: ([.workloads[].status] | group_by(.) | map({key: .[0], value: length}) | from_entries)}' \
  "$report"

jq -r '.workloads[] | select(.status == "SETUP_READY") | .workloadPlanId' "$report" \
  | sort -u > /tmp/setup-ready-workloads
jq -r '.workloadPlanId' "$package/fault-scenario-catalog.jsonl" \
  | sort -u > /tmp/fault-scenario-workloads
wc -l "$package/fault-scenario-catalog.jsonl"
comm -23 /tmp/fault-scenario-workloads /tmp/setup-ready-workloads  # expected: no output

sha256sum "$package"/{workload-catalog.jsonl,fault-scenario-catalog.jsonl,scenario-catalog-manifest.json,scenario-space-accounting.json,workload-catalog-rejected-inputs.jsonl}
```

For a fresh immutability check, save the five hashes before preflight, repeat afterward, and compare the files with `diff`. The saved derived summary records that comparison against the preflight baseline.

Measured result:

```text
terminal status: SUCCESS
candidates / participants: 82 / 82
SETUP_READY: 82
STARTUP_FAILED: 0
workflow actions: 0
Spring application contexts: 1
FaultScenarios linked to setup-ready workloads: 164 / 164
```

The 82 workloads comprise 76 `CreateUserFunctionalitySagas`, four `GetCourseExecutionsFunctionalitySagas`, one `GetCourseExecutionByIdFunctionalitySagas`, and one `FindQuizFunctionalitySagas` workload.

Why this matters: an earlier preflight found 80 ready and two startup failures because persisted integral values were restored as `BigInteger` while public constructors required `Integer`. The current executor preserves ordinary reflection first and then performs exact, range-checked integral restoration at the typed constructor boundary. The refreshed report establishes that the two concrete false negatives were fixed without changing package bytes or static candidate ids.

What it does **not** prove: no forward/fault/compensation/commit action ran, so the result does not qualify all-zero or faulty behavior.

Focused regression for this boundary:

```bash
cd verifiers
mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorWrapperSpec,ScenarioExecutorReadinessEvaluatorSpec,ScenarioExecutorOrchestratorSpec test
```

The recorded run passed 105 tests with zero failures/errors/skips. The full verifier regression recorded with this change passed 587 tests.

### Targeted all-zero and assigned-fault execution

Two persisted single-Saga scenarios from the same package were executed through the shared setup path:

```text
all-zero id: 564dc9b56a716cd2797a3f3200485791e6ecdb1ed383b422de987a72792d63bd
report:      verifiers/target/outcome2-helper-tracing/execution-all-zero.json
result:      SUCCESS / EXACT

target fault id: 142eccc9b6d9464439f9607061ff6c7a32109108d34a1ddc076e6bf8519ab71b
report:          verifiers/target/outcome2-helper-tracing/execution-single-fault.json
result:          COMPENSATED / EXACT; assigned fault realized
```

These are targeted controls, not a batch execution-coverage claim. All five package hashes remained unchanged.

### Quizzes event-consequence replay: positive, control, and masking

The v4 Quizzes package was generated from the repository root with default catalog bounds, static analysis, and the application's prerequisite descriptor:

```bash
MEDIUM_MEM_LIMIT=4g MEDIUM_MEM_RESERVATION=1g \
docker compose run --rm -T \
  -e JAVA_TOOL_OPTIONS=-Xmx3g \
  -e VERIFIERS_DYNAMIC_ENRICHMENT_ENABLED=false \
  fault-analysis-scenario-gen
```

The default 768 MiB container limit was insufficient and the first attempt was OOM-killed; the explicit limit and heap above are part of the reproducible generation configuration. The persisted package is:

```text
verifiers/target/quizzes-20260801-014405-816/
```

It includes one exact `UpdateStudentNameEvent` consequence from `UpdateStudentNameFunctionalitySagas.updateStudentNameStep` through `TournamentEventHandling.handleUpdateStudentNameEvent`. Two workloads place that consequence around `AddParticipantFunctionalitySagas.addParticipantStep`:

```text
positive: getUserStep -> updateStudentNameStep -> EVENT_CONSEQUENCE -> addParticipantStep
control:  getUserStep -> updateStudentNameStep -> addParticipantStep -> EVENT_CONSEQUENCE
```

The workloads require `quizzes-stale-read-baseline@1`. Its Quizzes test-classpath provider uses application functionality APIs to create a course execution, creator/enrollment, topics/questions, tournament, and updated user, then returns four typed bindings. Provider execution, binding resolution, and pending-event cleanup occur before measurement.

Each replay was a fresh `docker compose run --rm` process with H2 state. The command shape was:

```bash
MEDIUM_MEM_LIMIT=4g MEDIUM_MEM_RESERVATION=1g \
docker compose run --rm -T \
  -e JAVA_TOOL_OPTIONS=-Xmx3g \
  -e PACKAGE_PATH=/reports/quizzes-20260801-014405-816/scenario-catalog-manifest.json \
  -e FAULT_SCENARIO_ID=<persisted-scenario-id> \
  -e OUTPUT_PATH=/reports/outcome3-ec-rev002-20260801-014405-816-evidence/<run>-execution.json \
  -e IMPACT_OUTPUT_PATH=/reports/outcome3-ec-rev002-20260801-014405-816-evidence/<run>-impact.json \
  scenario-executor
```

Persisted ids:

```text
positive workload: ee36b9a067b501c82e9be7ee7bf85531d39b530de2ada01fa5f2e2edfbc60c2c
positive scenario: 24fbeb3b05830543f1deb1aa6eaa0be29e63b2eac807cae3264ce8f1f171e601
control workload:  481ea3b5c05ad122dd46fa9db904fe0c1b6d01b99446f55a3d199b62fcb1be13
control scenario:  ec869302d0912ce60c57a69e9a220dba0afd2aa6246a93fb141f727ffc34ca24
B1 masking:       5b1991d323f2ad8d1066d7c82f33a3559be2444a127adbac07bc6f4c9cd3dede
```

The manifest records the configured total cap and final selection honestly:

```text
maxCatalogScenarios:                         100
prerequisite workloads:                      2 generated / 2 exported / 0 capped
base workloads:                              98 generated / 98 exported / 0 merge-capped
final workloads exported:                    100
workloadsCapped:                             1
eventConsequenceExpansionCapEncounters:      1
eventConsequenceBaseWorkloadsOmittedAtCap:  14
warning: reached maxCatalogScenarios=98 between base workloads during event-consequence
         expansion; 14 remaining base workloads and their placements were not emitted
```

Measured result and fresh execution-attempt ids:

```text
positive-1  151d07b0-8894-4579-96ba-81471f2b456f  PARTIAL_COMPENSATED / DEVIATED  E=COMPLETED  ImpactV1=1
positive-2  eb8a434c-96ea-42ca-bfa5-6918854e9a8d  PARTIAL_COMPENSATED / DEVIATED  E=COMPLETED  ImpactV1=1
positive-3  b4729ce1-99d3-465f-a94a-dd2fb11024ce  PARTIAL_COMPENSATED / DEVIATED  E=COMPLETED  ImpactV1=1
control-1   24f40dcf-4cdb-42ec-a686-6bc51a9b462f  SUCCESS / EXACT                  E=COMPLETED  ImpactV1=0
control-2   4f24d37e-70de-41ba-aa40-6439f142fdcf  SUCCESS / EXACT                  E=COMPLETED  ImpactV1=0
control-3   a7c6cf8b-8098-4ad0-9391-6219fd846e08  SUCCESS / EXACT                  E=COMPLETED  ImpactV1=0
masking-1   0aa1f90e-dc5b-4daf-bf64-50f1869dc5e4  PARTIAL_COMPENSATED / EXACT      E=MASKED_BY_TRIGGER_FAULT  ImpactV1=0
```

Every completed E used persisted event id `3`, publisher id `2`, subscriber id `9`, and `TournamentEventHandling.handleUpdateStudentNameEvent` with `UpdateStudentNameEventHandler`. Positive actions completed `getUserStep`, `updateStudentNameStep`, and E before `addParticipantStep` failed its invariant. Control actions completed all three forwards before E. The masking attempt completed A1, realized B1's assigned pre-body fault, causally masked E, and completed A2.

Reports, logs, per-attempt hashes/diffs, and the derived complete matrix are under:

```text
verifiers/target/outcome3-ec-rev002-20260801-014405-816-evidence/
verifiers/target/outcome3-ec-rev002-20260801-014405-816-evidence/verification-summary.json
```

All seven fresh-process runs used `quizzes-stale-read-baseline@1`, cleared two prerequisite-created pending events, resolved all four typed bindings, and established an empty pending-event baseline. Every per-attempt hash diff is empty. The five package hashes before and after all attempts are:

```text
workload catalog:  fa46dd469c2c60d18096d666f1066d68903ff4893236afb5701b2447848588a8
fault catalog:     165790570406f2ba4d3e9a6358e2a5b00afa43e5a2996043910df567e79a0281
manifest:          63f9fc77221c1c27bd89190b94a864f20f01df8852157249ef07cfe404a13f98
accounting:        7bf7cdf08eba72667fef1e4375f1e3a05ab8f4b64d7f56835056558abc8c9643
rejected inputs:   dd4b4a7051600d5856df6c02a2230a1971b9c7092e85f43130c89e4f959ea0bf
```

This proves one realistic Saga/local event interaction is statically represented, prerequisite-bound, causally replayed, discriminating under ImpactV1, and repeatable at the fresh-process/H2 boundary. It does not prove generic fan-out, nested event chains, distributed event replay, same-process reset, or broad event-pattern coverage.

### Historical bounded multi-Saga package generation

The prior v3 package contract was exercised at Quizzes scale with the following historical command. V4 readers now reject this package; retain the result only as bounded-generation evidence:

```bash
MEDIUM_MEM_LIMIT=3g MEDIUM_MEM_RESERVATION=2g MEDIUM_CPUS=2 \
docker compose run --rm \
  -e JAVA_TOOL_OPTIONS=-Xmx2500m \
  -e VERIFIERS_OUTPUT_ROOT=/reports/compensation-aware-v3-evidence/bounded-quizzes-v3 \
  -e VERIFIERS_SCENARIO_CATALOG_ENABLED=true \
  -e VERIFIERS_SCENARIO_CATALOG_GENERATION_STRATEGY=BRUTE_FORCE \
  -e VERIFIERS_SCENARIO_CATALOG_CATALOG_WRITE_MODE=WRITE_WORKLOADS \
  -e VERIFIERS_SCENARIO_CATALOG_INCLUDE_SINGLES=false \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_SAGA_SET_SIZE=2 \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_CATALOG_SCENARIOS=2000 \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_INPUT_VARIANTS_PER_SAGA=2 \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_SCHEDULES_PER_INPUT_TUPLE=4 \
  -e VERIFIERS_SCENARIO_CATALOG_SCHEDULE_STRATEGY=SEGMENT_COMPRESSED \
  -e VERIFIERS_SCENARIO_CATALOG_RECOVERY_SCHEDULE_CAP=20 \
  -e VERIFIERS_DYNAMIC_ENRICHMENT_ENABLED=false \
  fault-analysis-scenario-gen
```

Output package:

```text
verifiers/target/compensation-aware-v3-evidence/bounded-quizzes-v3/quizzes-20260720-091007-712/
```

Result:

```text
WorkloadPlans written:       2000
static setup candidates:     12
statically blocked:          1988
FaultScenarios written:      84
computed eager vectors:      60
uncapped/written recovery schedules over computed vectors: 84 / 84
```

This proves bounded deterministic multi-Saga package generation and compensation-aware schedules. The saved execution report beside this package predates the current explicit domain-failure classifier and misclassified unmarked service unavailability as a domain deviation. It is not current execution-policy evidence and should not be cited as such. A fresh current multi-Saga execution baseline remains missing.

### Exact bounded recovery counting

```bash
cd verifiers
mvn -Dtest=RecoveryScheduleGeneratorSpec test
```

The high-cardinality fixture records:

```text
exact uncapped schedules: 118264581564861424
written at cap:           20
counting states visited:  992
materialized leaves:      fewer than 100
```

This proves exact BigInteger counting with bounded retained schedules for one computed vector.

### Regression baseline

The event-consequence implementation recorded:

```text
simulator complete suite: 111 tests passed
verifier focused repair suite: 214 tests passed
verifier complete suite:  628 tests passed
focused Quizzes oracle/provider: 10 tests passed
```

`mvn -Ptest-sagas test` in Quizzes ran 152 tests but retained seven unrelated async/concurrency assertion failures: those tests expect `SimulatorException` directly while the async path returns `CompletionException`. No changed path belongs to those tests or their async implementation. The event-consequence provider and stale-read oracle pass, and the verifier dependency no longer activates `ScenarioGeneratorApplication` in ordinary Quizzes tests. This is a full-suite baseline failure, not a passing claim.

These totals are point-in-time evidence, not permanent acceptance criteria. Current changes should run focused affected tests and broaden only when the changed boundary justifies it.

## Current limitations

- Thirty-two discovered Quizzes Sagas still lack accepted static inputs. This does not mean no tests exist; their invocation/value shapes remain unclassified or unsupported.
- Exact aggregate-instance key extraction is incomplete. Type-level and symbolic conflicts can over-approximate interaction.
- Event-consequence extraction supports one conservative direct producer shape and one unique local consumer. Wrong receiver or unit-of-work binding, mixed compensation-origin emission, conditional/repeated consumer delegation, multiple/repeated/conditional producer emissions, fan-out, recursion, nested event chains, and unresolved routes are rejected diagnostically.
- Helper-built course DTOs now preserve `DateHandler.toISOString(endDate)`, but that local transform remains unsupported and blocks those candidates rather than fabricating empty DTOs.
- Static setup candidacy is conservative prediction. The latest package achieved 82/82 runtime setup readiness, but other packages and environments still require actual setup evidence.
- Repeated same-participant runtime step names are structurally rejected because current Saga/local runtime state is keyed by step name rather than occurrence id.
- Segment compression preserves conflict-anchor order cases under extracted evidence; it does not prove every semantically distinct runtime interleaving is retained.
- Dynamic enrichment remains local/Saga-focused. There is no fresh broad Quizzes v4 attribution baseline.
- The generated Quizzes event-consequence pair provides one ImpactV1 1/0 discrimination; this is one representative interaction, not evidence that other workloads or vectors have non-flat fitness.
- No current post-remediation Quizzes smoke demonstrates the explicitly marked zero-bit domain-fallback path.
- Persistent-environment reset is the caller/orchestrator's responsibility.
- On-demand package writes are serialized but not crash-atomic.

## Not implemented

- Generic execution for every generated WorkloadPlan/FaultScenario shape.
- TCC, stream, gRPC, distributed, causal, or true-concurrent execution parity.
- Compensation faults, delay injection, non-binary impairments, or automatic recovery retry/backoff.
- Semantic deduplication of value-equivalent inputs.
- Profile-aware resolution for ambiguous multiple `@Service` implementations.
- State-divergence, postcondition, or silent-compensation impact models beyond invariant-count ImpactV1.
- Batch execution qualification, generic reset orchestration, GA/local fault search, or scenario prioritization.

## Safe thesis framing

Safe current claim:

> The verifier deterministically extracts Saga-oriented workload structure, conservative exact event consequences, and test-derived or prerequisite-bound inputs; publishes compensation-aware WorkloadPlan/FaultScenario v4 packages; can verify exact Saga/local setup readiness; can replay one persisted setup-ready FaultScenario sequentially, including one unique local event consequence; and can report generic Saga aggregate-invariant rejections through a first narrow impact model.

Required qualifications:

- generated does not mean setup-ready;
- setup-ready does not mean execution-successful;
- `EXACT` conformance does not mean harmless;
- assigned fault or compensation does not by itself mean impact;
- static conflict evidence and segment compression do not prove semantic completeness;
- current execution is Saga/local and sequential, not generic distributed concurrency;
- historical v1/v2/v3 catalogs and broad dynamic counts are not current v4 evidence.

Unsafe current claim:

> The verifier can execute arbitrary generated distributed fault scenarios, detect all harmful outcomes, and optimize them through search or prioritization.

That remains roadmap work.
