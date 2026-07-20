# Advisor brief

Last updated: 2026-07-20

Use this page before thesis meetings. For implementation detail, follow links from [`current-state.md`](current-state.md).

## 30-second update

The verifier now generates a deterministic compensation-aware v3 package from application source and tests. Reusable `WorkloadPlan` records carry participants, inputs, normal forward schedules, fault slots, and compensation checkpoints; persisted `FaultScenario` records add one assigned vector and one concrete recovery-aware action schedule. Materializable all-zero and single-point vectors are eager, arbitrary valid multi-fault vectors can be persisted atomically on demand, and exact per-computed-vector recovery counts do not require enumerating the uncapped leaf space.

A narrow saga/local ScenarioExecutor now selects one persisted FaultScenario—with no vector overlay—and writes an action-aware v4 report. The 2026-07-20 bounded Quizzes smoke produced a real compensation interleaving and honestly reported a zero-bit input-quality deviation while preserving all package bytes. The main thesis gaps are now input/materialization quality, broader runtime-shape coverage, repeatable state reset, and impact scoring/search.

## What exists now

- Static Saga extraction from Java/Groovy, including compensation evidence and the implemented event-origin chain shape.
- Source-mode filtering with rejected-input diagnostics.
- Deterministic five-file v3 WorkloadPlan/FaultScenario package.
- Conflict-anchor `SEGMENT_COMPRESSED` forward schedules.
- Bounded recovery schedule generation, eager materializable baselines, and exact computed-vector accounting.
- Guarded, idempotent, atomic on-demand multi-fault persistence.
- Workload-linked dynamic-evidence sidecars that do not mutate semantic package artifacts.
- Persisted-action ScenarioExecutor replay with automatic participant commit, assigned-fault recovery ordering, zero-bit immediate-recovery fallback, and compensation/infrastructure hard stops.
- Docker Compose paths for generation, tests, and one-shot execution.

## Evidence I can cite

Bounded Quizzes v3 generation:

```text
run: verifiers/target/compensation-aware-v3-evidence/bounded-quizzes-v3/quizzes-20260720-091007-712/
configuration: BRUTE_FORCE, multi-saga only, maxSagaSetSize=2,
               maxCatalogScenarios=2000, maxInputVariantsPerSaga=2,
               maxSchedulesPerInputTuple=4, SEGMENT_COMPRESSED,
               recoveryScheduleCap=20, dynamic enrichment disabled
WorkloadPlans written/materializable: 2000 / 12
FaultScenarios written: 84
computed eager vectors: 60
exact uncapped/written schedules over computed vectors: 84 / 84
legacy scenario-catalog.jsonl: absent
```

Selected persisted compensation interleaving:

```text
WorkloadPlan: 01c49ae4314e106161ccc75f7531c62d01299417e702908100d7fb809ca2face
FaultScenario: 25c0d61a2a2b40c2aaff7946aca8d2bb1becfc54b8b168442bcff40262052271
participants: CreateCourseExecutionFunctionalitySagas + GetCourseExecutionsFunctionalitySagas
assigned vector: 00010
recovery schedules uncapped/written: 3 / 3
planned actions: 5 FORWARD + 2 COMPENSATION
interleaving: compensation, survivor forward, compensation
```

Docker execution:

```text
schema: microservices-simulator.scenario-execution-report.v4
terminal status / conformance: PARTIAL_COMPENSATED / DEVIATED
participant final states: COMPENSATED + COMMITTED
zero-bit blocker: UNASSIGNED_RUNTIME_FORWARD_FAILURE
package hashes before/after: all five unchanged
```

The extracted course name was null, so zero-bit `createCourseStep` failed before the assigned fourth slot. The report records immediate no-work recovery, the assigned slot as masked, and survivor continuation. Claim this as evidence of honest fallback/action reporting, not exact replay of the planned compensation sequence.

High-cardinality synthetic recovery-schedule accounting:

```text
exact uncapped schedules: 118264581564861424
written at cap: 20
counting states: 992
materialized leaves: fewer than 100
```

Regression gates passed:

```text
complete simulator Maven suite
complete verifier Maven suite
focused parser + v3 dynamic-enrichment + high-cardinality specs
Docker fault-analysis-scenario-gen-test
```

Historical context only: the 2026-06-30 Quizzes attribution baseline recorded `MATCHED_EXACT=435`, `MATCHED_HIGH_CONFIDENCE=125`, `AMBIGUOUS=0`, and `UNMATCHED=24`. Those are v2-era broad-baseline counts, not a fresh v3 Quizzes enrichment result. V3 sidecar/package immutability is currently covered by dummyapp integration.

Detailed commands, paths, report fields, hashes, and limitations live in [`evidence.md`](evidence.md).

## What I should not claim

- Every WorkloadPlan is materializable or runtime-successful.
- The selected Quizzes smoke replayed its planned action sequence exactly; it deviated on a meaningful zero-bit failure.
- Recovery accounting covers all vectors across all workloads; exact counts are for computed vectors only.
- Generic scenario execution, true concurrency, TCC, or stream/gRPC/distributed parity.
- Automatic FaultScenario selection, runtime vector overlays, compensation faults, or automatic recovery retry/backoff.
- Generic persistent-environment reset.
- Impact scoring, GA/local search, or bandit prioritization.
- A fresh broad Quizzes v3 dynamic-enrichment baseline.
- Dynamic evidence creates or rewrites WorkloadPlans/FaultScenarios; it is sidecar-only.

## Current limitation to explain plainly

Static acceptance and package materializability are necessary gates, not proof that extracted values are semantically valid at runtime. The Quizzes smoke made that boundary concrete: constructor materialization succeeded, but a null course name caused a zero-bit domain/simulator failure. Improving input/value reconstruction and event payload materialization is now more important than increasing headline catalog counts.

Exact aggregate-instance binding also remains incomplete. Segment compression is deterministic under extracted conflict evidence, not proof that every semantically distinct runtime interleaving is preserved.

## Next work

1. Improve extracted input/value quality where runtime reports expose concrete blockers.
2. Classify the remaining 32 Quizzes sagas without accepted static inputs.
3. Refresh a representative Quizzes dynamic-enrichment run against the workload-linked v3 sidecar.
4. Define repeatable state reset and the first domain-impact metrics.
5. Only then add search over persisted on-demand vectors and scenario prioritization.

## Question for advisor

Should the next thesis milestone prioritize:

1. input/materialization quality plus repeatable execution and first impact metrics, or
2. broader static/dynamic coverage before impact/search work?
