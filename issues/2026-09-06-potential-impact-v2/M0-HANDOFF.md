# M0 handoff — shared evidence in ordinary execution

State: complete.

## Outcome

Implemented the shared, generic observation boundary for FR-1–4, FR-9 and FR-11.
Successful setup starts one attempt-scoped collector; selection/setup failures retain an
explicit unavailable result. Ordinary executions automatically write
`microservices-simulator.scenario-impact-v2-evidence.v1` beside the execution report
(`execution.json` -> `execution.impact-v2.json`). The M0 assessment status is
`NOT_ASSESSED_M0` with a null score; incomplete executions are `INVALID`.

The simulator exposes immutable baseline/final projections, subscription-declared
dependencies, transaction-confirmed writes, writer/action phase, exact selected-event
receiver before/after state and eligibility, final polymorphic eligibility, plus coverage gaps.
Collection begins after setup. No detector, score, extra handler, or business operation
was added. ImpactV1 and the v5 execution-report constructor/schema are unchanged.

## Actual files changed

- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/monitoring/impact/*`
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/aggregate/EventApplicationService.java`
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/unitOfWork/SagaUnitOfWorkService.java`
- `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/monitoring/impact/*`
- `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/notification/EventReplayCoordinatorTest.java`
- `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/unitOfWork/SagaUnitOfWorkServiceDynamicEvidenceTest.java`
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ImpactV2EvidenceCollector.java`
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ImpactV2EvidenceReport.java`
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`
- `docs/verifiers-impl/current-state.md`
- `docs/verifiers-impl/roadmap.md`

The inherited B preflight-reporting patch and other pre-existing dirty files were
preserved. No commit, push, merge, production change, or application business fix was
performed.

## Discovery and decisions

- `registerChanged` runs after merge but before transaction completion. It now captures only
  the typed identity, version and writer, then reloads and projects that exact persisted
  revision from a read-only `REQUIRES_NEW` transaction in `afterCommit`. Rejected and
  rolled-back writes produce no committed-write fact.
- Normalization follows JPA managed attributes, preserves nulls, application dates,
  ordered lists, deterministically ordered sets, owned data and application child
  versions. Aggregate lifecycle stays in comparison data. Framework row/revision/
  creation/predecessor and Saga-lock facts are retained separately.
- Owned child backreferences to their aggregate normalize as logical identity refs;
  other unsupported cycles/mappings become gaps.
- Selected-event observation loads the latest receiver and invokes its actual
  `subscribesEvent(exactEvent)` predicate. Event-consumer writes have a distinct writer;
  missing, unknown, or attempt/workload-mismatched writers become attribution gaps.
- ID-only lookup rejects type-ambiguous logical identities. Dependency declarations resolve
  their integer target through persisted state to an exact aggregate identity; missing and
  ambiguous targets stay as explicit gaps. Baseline/final enumeration shares one identity
  index; per-object projections query only the declared target IDs.
- The managed value returned by `EntityManager.merge` supplies the committed revision
  identity. Reloaded values are unproxied before metamodel access. Collector finalization is
  synchronized with fact callbacks.
- Persisted M2 run 02 exposed PostgreSQL timestamp precision as a real boundary: the former
  pre-commit snapshot retained nanoseconds while the final database row retained microseconds.
  Exact post-commit reload now makes committed-write and final snapshots compare the same
  persisted representation without truncating or excluding application timestamps.
- Event-consequence delivery writers now use the executor action's deterministic ID, matching
  `ScenarioExecutionReport.actualActions[].actionId`; the source scheduled-step ID remains in
  the existing event replay context.
- `microservices.simulator.impact.enabled=false` disables reads/hooks for the bounded
  observer-off control and reports `UNAVAILABLE/COLLECTION_DISABLED`.
- Observer install, projection, callback, predicate, and sidecar-write failures are
  contained and cannot replace the application outcome. Callback/enablement failures are
  retained outside the failing callback. The scope remains installed through synchronous
  horizon collection, then closes before those failures are drained into coverage gaps;
  sidecar-write failure is logged. Supported Saga/local transaction callbacks finish before
  their action returns; general asynchronous work after the scheduled horizon is outside
  this executor's lifecycle boundary.

The automatic M0 evidence sidecar was chosen with the root agent instead of changing
the existing v5 report. Runtime cost and real Quizzes on/off equivalence remain part of
the prepared M2 persisted qualification, after M1 stabilizes category output.

## Proof

- Java 21 focused simulator tests: 28 passed (normalization, dates/nulls/collections/
  owner cycles, exact/ambiguous identities, typed dependency targets, polymorphic
  eligibility, event before/after evidence, post-commit exact-revision reload with an H2
  microsecond timestamp round trip, commit vs rollback/rejection, reload-failure gaps with
  no false write, callback containment, writer and observer scope isolation).
- Java 21 `ScenarioExecutorSpec`: 143 passed, including automatic unavailable/observed
  M0 sidecars, event-delivery/action ID equality, retained callback failures, writer
  mismatch coverage, opt-out behavior, existing execution behavior, and ImpactV1
  compatibility.
- Java 21 full simulator install: 134 passed across 21 suites with zero failures,
  errors, or skips (`verifiers/target/impact-v2-base/simulator-full.log`).
- `./scripts/verifier-docs build`: passed.
- `git diff --check`: passed.
- Independent M0 implementation review: PASS after the second review pass.

## What to try

Run any supported normal Saga/local ScenarioExecutor attempt and inspect the sibling
`*.impact-v2.json`. A runtime without `PersistentStateObserver` should still complete and
write explicit unavailable evidence. Repeat with
`-Dmicroservices.simulator.impact.enabled=false` for the observer-off control.

The separately versioned M0 evidence schema is intentionally an internal input to M1;
its category/report composition is the remaining reasonable review point.
