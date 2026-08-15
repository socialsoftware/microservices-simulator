# Persist and deterministically replay atomic event consequences

- Workflow: SP coordinator v1
- State: `complete`
- Work shape: `System`
- Route: `Guided`
- Updated: `2026-08-01`
- Workspace: current checkout `/home/andre/thesis/microservices-simulator`, branch `fault-analysis/scenarios`
- Git: uncommitted changes only; no commit, push, PR, merge, or external action authorized
- Authority: implement and verify the approved three-outcome plan; no external actions
- Implementation approval: `approved (user request: "Lets implement ... Its approved")`

## Current Checkpoint

- Completed: All three outcomes, final-source Docker proof, canonical docs, and two-cycle independent review convergence are complete; reviewer verdict is `PASS`.
- Current: complete.
- Problem: none. Seven unrelated Quizzes async assertion failures remain outside this outcome.
- User input: none.
- Next: none unless the user authorizes Git actions or opens follow-up scope.

## Intent Brief

### Outcome

Persist one application-independent event consequence as part of normal workload structure, schedule it as one atomic `EVENT_CONSEQUENCE` action between ordinary `FaultScenario` actions, and deterministically invoke the selected real application handler path during Saga/local replay.

The consequence is causally dependent on one event-emitting forward occurrence but is not another outer workload participant. Its downstream Saga runs synchronously inside the event action, receives no fault-vector slot, contributes no generated compensation checkpoint, and completes before the next outer action begins.

### Representative Example

Use the existing Quizzes stale-read interaction with two outer Saga participants:

```text
A1 = AddParticipantFunctionalitySagas.getUserStep
B1 = UpdateStudentNameFunctionalitySagas.updateStudentNameStep
E  = the exact UpdateStudentNameEvent emitted by B1
     -> TournamentEventHandling.handleUpdateStudentNameEvent
     -> UpdateStudentNameEventHandler
     -> TournamentEventProcessing.processUpdateStudentNameEvent
     -> TournamentFunctionalities.updateStudentName
     -> UpdateUserNameFunctionalitySagas.executeWorkflow (atomic outer action)
A2 = AddParticipantFunctionalitySagas.addParticipantStep
```

Persist at least these all-zero schedules from the same prerequisite baseline and selected Tournament route:

```text
positive: A1 -> B1 -> E -> A2   => existing Tournament invariant rejection; ImpactV1 = 1
control:  A1 -> B1 -> A2 -> E   => no invariant rejection; ImpactV1 = 0
```

Also persist a scenario that faults B1 before its body. It must account for E as `MASKED_BY_TRIGGER_FAULT`, invoke no handler, and create neither an event fault slot nor an event compensation action.

### In Scope

- Producer-side static extraction from a Saga forward step, through its command handler/service path, to a uniquely identified `registerEvent(new EventType(...), unitOfWork)` emission site.
- Reuse of the existing consumer-side `EventHandlingBridgeVisitor` route:
  `EventHandling -> EventHandler -> EventProcessing -> facade -> downstream Saga`.
- An occurrence-level event-consequence model linking one trigger scheduled step, one emission site/event type, and one explicitly selected handler route.
- A unified persisted normal schedule that orders outer `FORWARD` occurrences and atomic `EVENT_CONSEQUENCE` occurrences while retaining the existing forward schedule as the fault-slot/checkpoint source.
- Deterministic consequence placement after its trigger; positive and control placements become distinct WorkloadPlans with distinct identities.
- `EVENT_CONSEQUENCE` in persisted FaultScenario actions and action identity, with no fault-slot or compensation-checkpoint reference.
- Recovery generation that preserves the selected normal order, inserts existing compensation actions only, and treats a causally masked consequence as a forced no-op rather than a new recovery choice.
- Simulator-owned exact event-emission capture and selected-event replay control.
- Invocation of the real Spring `EventHandling` bean method under an exact selected-event scope; no direct Quizzes facade/Saga shortcut.
- Replay-only event polling control that is active before prerequisite setup and prevents unscoped scheduled polling from processing events while explicit replay is active.
- A generic test-classpath prerequisite-baseline provider and typed persisted baseline-binding recipes for the Quizzes aggregate IDs/DTOs that cannot be known before setup.
- Separate setup/baseline reporting; prerequisite actions are outside measured actions, fault allocation, recovery, conformance, and ImpactV1 collection.
- Dummyapp-first extraction/generation proof, dummyapp-labelled executor/runtime proof, and real Quizzes positive/control evidence.
- Clean versioned package/report changes and canonical documentation.

### Non-goals and Meaningful Deferrals

- One database transaction spanning event lookup, handler execution, nested Saga, and adjacent outer actions. Atomicity means synchronous, indivisible execution relative to the outer executor schedule.
- Treating the downstream event Saga as an outer WorkloadPlan participant.
- Event or nested-Saga fault slots, generated event compensation, compensation faults, or handler retry/backoff.
- Recursive event consequences or replay of events emitted by the downstream consequence.
- Fan-out of one consequence to several handler routes or several subscriber aggregates. A package selects one route and requires one matching subscriber at runtime.
- Conditional/indirect/multiple event emissions from one trigger occurrence in the first slice.
- TCC, remote publisher, stream, gRPC, distributed, causal, or true-parallel event replay.
- Same-process batch reset. The supported Quizzes evidence uses one fresh JVM/container and fresh H2 database per attempt.
- A general prerequisite DSL, database snapshot format, or application cleanup API. The first slice uses a generic provider SPI with a Quizzes test-scope provider.
- V3-to-v4 package migration or compatibility readers. Regenerate packages under v4.
- Changes to ImpactV1 scoring, recovery-search policy, genetic algorithms, cross-workload allocation, or dynamic-evidence ownership.

### Constraints and System Invariants

- Preserve the pipeline boundary:
  `visitor/* -> ApplicationAnalysisState -> scenario/adapter/* -> scenario/* -> dynamic/*`.
- `WorkloadPlan` continues to own reusable normal-execution structure; `FaultScenario` continues to own one assigned vector and one complete concrete action order.
- Existing outer forward steps remain the only binary fault slots and the only sources of generated compensation checkpoints.
- Event route selection is explicit and deterministic. Event database IDs, timestamps, publisher versions, generated aggregate IDs, and payload values are runtime evidence, not deterministic package identity.
- A trigger assigned fault occurs before its body, so no expected event is emitted and the consequence is causally masked rather than failed.
- A successful trigger with zero or multiple captured matching events is an invalid experiment, not a guessed delivery.
- The selected handler must receive exactly one captured event and exactly one matching subscriber aggregate.
- Unexpected handler/downstream failure, replay-control failure, recursive emission, or cardinality ambiguity hard-stops measured execution and makes impact `NOT_EVALUATED`, never zero.
- The selected `EventHandling` method and complete downstream Saga call return before the executor starts the next outer action.
- The event action runs outside every `FaultVectorBoundaryContext`; legacy impairment behavior remains suppressed by executor-owned provider mode.
- Execution, baseline setup, impact collection, and reports must not modify any of the five package files.

### Acceptance Proof

- Representative outcome: one checksum-valid package contains persisted positive `A1 -> B1 -> E -> A2` and nearby control `A1 -> B1 -> A2 -> E`; fresh Quizzes executions produce one evaluated ImpactV1 finding/score `1` for the positive and evaluated score `0` for the control.
- Fault/compensation boundary: package inspection proves E has no `ForwardFaultSlot`, vector bit, `CompensationCheckpoint`, or generated compensation action; accounting keeps vector dimension tied only to outer forwards.
- Causal masking: a persisted B1 single-point fault records B1 as realized and E as `MASKED_BY_TRIGGER_FAULT`, never invokes the handler, and remains an evaluated domain attempt when all other current eligibility rules hold.
- Determinism: run the positive and control at least three times each from equivalent fresh `test,sagas,local` H2 state; compare terminal status, conformance, ordered measured actions, consequence status/route, and ImpactV1 after normalizing attempt/runtime-generated IDs.
- Package immutability: save SHA-256 for all five files before baseline/setup and execution; compare after every positive, control, and masking attempt with no changed bytes.
- Focused regression: simulator replay-control/emission tests; verifier extraction, adapter, identity, validator, recovery, package, materializer, executor/report/wrapper tests; existing fault/recovery/Impact controls; then complete affected simulator and verifier suites.
- Real boundary: Docker Compose generation plus one-shot ScenarioExecutor runs under the same `test,sagas,local` profile and mounted report paths seen by the user.
- Documentation: current handbook, roadmap, and one durable decision record agree with implemented v4 behavior, evidence, and limits.
- Independent review: one fresh `sp-reviewer` reviews the full approved boundary and evidence after focused/full/Quizzes proof; the same reviewer verifies any fixes until `PASS`.

## Context and Route

### Current Behavior and Anchors

- `WorkloadPlan.v3` owns participants, accepted inputs, one outer forward schedule, conflict evidence, one fault slot per forward occurrence, and compensation checkpoints:
  `verifiers/src/main/java/.../scenario/model/WorkloadPlan.java`.
- `FaultScenario.v3` owns the vector and ordered `FORWARD`/`COMPENSATION` actions:
  `FaultScenario.java`, `FaultScenarioAction.java`, and `FaultScenarioActionKind.java`.
- Stable IDs and structural validation are centralized in `ScenarioIdGenerator`, `WorkloadPlanValidator`, and `FaultScenarioValidator`.
- `RecoveryScheduleGenerator` preserves residual forward order while interleaving enabled compensation queues; `EagerFaultScenarioGenerator` currently expects exactly one all-zero scenario per WorkloadPlan.
- `EventHandlingBridgeVisitor` already extracts the consumer route and dummyapp proves the bridge, but event-origin input recipes use placeholders and do not link an upstream emitting step to a selected runtime event.
- `ScenarioExecutor` replays only forward and compensation actions. `ScenarioRuntimeContext` exposes bean lookup and Saga unit-of-work creation, but no event replay or baseline API.
- `SagaUnitOfWorkService.registerEvent` persists and locally publishes events but exposes no attempt-scoped emitted-event identity.
- `EventApplicationService.handleSubscribedEvent` scans all subscriber aggregates and all matching pending events. `EventRepository.findUnprocessedEvents` has no exact event-ID selection and orders only by timestamp, so it cannot provide deterministic explicit replay as written.
- `EnableDisableEventsController.stopSchedule()` has no startup barrier or in-flight guarantee. The executor starts the Spring context before it can call that controller.
- The real Quizzes path and positive oracle exist in `AddParticipantAndUpdateStudentNameTest.groovy`; the current package cannot persist or execute it because event consequence structure and prerequisite-bound inputs are missing.

### Recommended Technical Approach

#### 1. Persist normal event structure without changing fault ownership

Add v4 model records equivalent to:

- `EventEmissionSite`: source class/method/signature, deterministic emission ordinal, event type, and extraction evidence.
- `EventConsequence`: deterministic ID, trigger scheduled-step ID, emission-site ID, event type, selected event-handling class/method, selected handler class, event-processing class/method, facade class/method, downstream Saga FQN, delivery policy `UNIQUE_MATCHING_SUBSCRIBER`, and diagnostics.
- `NormalActionRef`: dense normal order over either one forward scheduled-step ID or one event-consequence ID.
- `PrerequisiteBaseline`: provider ID/version and sorted required typed binding keys.

`WorkloadPlan.v4` retains `forwardSchedule`, `faultSlots`, and `compensationCheckpoints`, and adds `eventConsequences`, `normalSchedule`, and optional prerequisite-baseline requirements. Every forward occurrence appears once in `normalSchedule`; E appears once after its trigger. All semantic fields participate in WorkloadPlan identity.

Add `EVENT_CONSEQUENCE` and `sourceEventConsequenceId` to v4 `FaultScenarioAction`. The action has no source fault slot/checkpoint. Its source participant/occurrence identify the causal trigger only.

Generate one WorkloadPlan per bounded, causally valid consequence placement. For the target outer forward order `A1,B1,A2`, generation emits the positive placement after B1 and control placement after A2 as distinct workloads. Keep route and placement ordering deterministic by FQN, method, emission ordinal, and normal action order; use existing workload/schedule caps rather than an unbounded event permutation stage.

#### 2. Extend extraction conservatively

Add a producer-side event-emission visitor/index that follows an outer Saga step dispatch through the existing command-handler/service facts to direct `registerEvent(new EventType(...), unitOfWork)` calls. Attach resolved emission facts to the relevant `SagaStepBuildingBlock`/`StepDefinition` in the adapter.

Join each resolved emission to the existing `EventDrivenFunctionalityInvocation` consumer routes by event type and full route identity. Multiple global consumers may become separate explicitly selected WorkloadPlan candidates; one event action never means fan-out. Reject unresolved/conditional/multiple emission sites, ambiguous bridge internals, compensation-origin events, downstream non-Saga/local routes, and statically recursive routes with stable diagnostics rather than guessing.

Add a dummyapp producer path to the existing dummyapp consumer fixture and prove extraction through generated v4 package records and identities.

#### 3. Integrate ordering and recovery

Change recovery preparation from a forward-only residual sequence to the selected WorkloadPlan `normalSchedule`:

- outer forward actions behave exactly as now and remain the only actions that change participant/fault/recovery state;
- a consequence becomes enabled only after its trigger completes and captures the expected event;
- if the trigger receives an assigned pre-body fault or a supported zero-bit domain failure before emission, retain the planned E action as a deterministic masked no-op;
- if the trigger is never reached after an earlier participant failure, retain E as `MASKED_BY_TRIGGER_NOT_REACHED`;
- a masked E is forced at its normal position and does not create before/after compensation branches;
- an enabled E is one indivisible residual normal action; existing compensation queues may be scheduled around it, but never inside it;
- E never adds compensation work, a fault slot, or a search dimension.

Validators enforce causal order, exact normal-schedule coverage, unique IDs, one event action per consequence, null fault/checkpoint references for E, valid route/baseline references, and residual normal/recovery order. Accounting reports consequence counts and complete normal-schedule counts while retaining fault-vector mathematics over outer slots only.

#### 4. Invoke the exact real event path

Add a simulator-owned replay coordinator installed in deterministic replay mode before `SpringApplication.run` completes:

- an attempt/trigger scope records each event persisted by `SagaUnitOfWorkService.registerEvent`, including runtime event ID, type, publisher aggregate/version, and published state;
- a selected-event scope identifies the captured event, expected event type, selected `EventHandling` bean/method, expected handler class, and `UNIQUE_MATCHING_SUBSCRIBER` policy;
- while replay mode is active, unscoped scheduled `EventApplicationService.handleSubscribedEvent` calls cannot process pending events; explicit selected dispatch owns the gate and scope;
- the executor invokes the persisted Spring `EventHandling` bean method, which still calls the real `EventApplicationService`, constructs the real handler, reaches event processing/facade code, and runs the downstream Saga;
- `EventApplicationService` validates the selected handler/type, loads only the selected event, computes eligible subscribers, requires exactly one, and delivers once;
- nested event registration during E is rejected as recursive in this slice;
- every scope/gate/fault context is cleared in `finally`.

This is synchronous logical atomicity relative to the outer loop, not a new cross-handler database transaction.

#### 5. Prepare equivalent prerequisite state outside measurement

Define a generic target-application `ScenarioPrerequisiteProvider` SPI resolved by persisted provider ID. A Quizzes test-classpath implementation reproduces the oracle setup through real application functionality APIs and returns typed named bindings such as course-execution, creator-user, tournament, and update DTO values.

Add `baseline_binding` to `input-recipe.v2`; the package persists binding keys/types, never silently substitutes runtime values. The materializer resolves them only from the selected provider result. Missing provider/key/type mismatch fails before measured execution.

For each one-shot attempt:

```text
fresh JVM + fresh H2 context
  -> replay-only event gate already active
  -> provider prepares prerequisite domain state
  -> clear prerequisite-generated pending events through the generic replay/event service
  -> verify empty pending-event baseline and required typed bindings
  -> start ImpactV1 collection and measured outer replay
  -> write setup/execution/impact reports outside the package
  -> discard the process/database
```

Report provider identity, setup status, binding-key/type fingerprint, event-gate state, and pre-measurement pending-event count separately from measured actions. Do not expose raw generated IDs as deterministic package identity.

### Failure and Unsupported Classification

| Case | Classification | Evaluation/conformance |
|---|---|---|
| Trigger assigned fault before body | `MASKED_BY_TRIGGER_FAULT` | Expected no-op event action; no handler; exact if the persisted schedule otherwise completes |
| Supported zero-bit trigger domain failure before emission | `MASKED_BY_TRIGGER_FAILURE` | Existing `DEVIATED` fallback semantics; no handler |
| Trigger emits the selected event and then its body or commit fails | `TRIGGER_FAILED_AFTER_EVENT_EMISSION` | Hard stop; `INCOMPLETE`; handler not invoked; impact not evaluated |
| Prior hard stop means trigger never ran | `MASKED_BY_TRIGGER_NOT_REACHED` or report `NOT_REACHED` | Attempt remains incomplete for the original hard-stop reason |
| Successful trigger emits no expected event | `EXPECTED_EVENT_NOT_EMITTED` | Hard stop; `INCOMPLETE`; impact not evaluated |
| Trigger emits several matching events | `MULTIPLE_MATCHING_EVENTS_UNSUPPORTED` | Hard stop; `INCOMPLETE`; impact not evaluated |
| No matching subscriber for selected route | `SELECTED_SUBSCRIBER_NOT_FOUND` | Hard stop; `INCOMPLETE`; impact not evaluated |
| Several matching subscribers | `MULTIPLE_MATCHING_SUBSCRIBERS_UNSUPPORTED` | Hard stop; `INCOMPLETE`; impact not evaluated |
| Several global handler routes | Separate explicitly selected WorkloadPlan candidates | Fan-out in one E action is unsupported; ambiguous route selection rejects generation |
| Selected handling method resolves several handlers/downstream routes | `MULTIPLE_HANDLER_ROUTE_UNSUPPORTED` | Static/package rejection before execution |
| Handler or downstream Saga throws unexpectedly | `EVENT_CONSEQUENCE_FAILED` | Immediate hard stop; `INCOMPLETE`; no outer generated recovery; impact not evaluated |
| Consequence emits another event | `RECURSIVE_EVENT_CONSEQUENCE_UNSUPPORTED` | Static rejection when known; runtime hard stop when observed |
| Scheduler/replay gate cannot prove exclusive delivery | `EVENT_REPLAY_CONTROL_FAILED` | No measured replay or immediate hard stop; impact not evaluated |
| Baseline provider/binding/reset fails | `PREREQUISITE_BASELINE_FAILED` | Measured actions never begin; no conformance; impact not evaluated |

### Versioned Surfaces

Use a clean v4 package boundary; do not silently add event semantics to v3:

- `microservices-simulator.workload-plan.v4`
- `microservices-simulator.fault-scenario.v4`
- `microservices-simulator.scenario-catalog-manifest.v4`
- `microservices-simulator.scenario-space-accounting.v4` because consequence placement and recovery accounting gain explicit fields
- `microservices-simulator.input-recipe.v2` for typed prerequisite bindings
- `microservices-simulator.scenario-execution-report.v5` for baseline and event-consequence planned/actual evidence

Keep unchanged unless implementation proves a shape change is necessary:

- the five package filenames and checksum boundary;
- `microservices-simulator.workload-catalog-rejected-input.v3`;
- `microservices-simulator.scenario-impact-report.v1` and `ImpactV1 = invariantViolationCount`;
- setup-preflight v1 for its current non-baseline batch contract;
- dynamic-evidence schemas and sidecar ownership.

V4 readers reject v3 workload/fault/manifest packages. On-demand vector persistence, package validation, hashes, accounting, and manifest updates move coherently to v4; execution still selects one persisted FaultScenario ID and never accepts a runtime vector overlay.

### Route Rationale

Guided is the minimum safe route because this is one coherent Saga/local feature with three independently demonstrable outcomes, clean schema changes, simulator/verifier/application-test integration, real-environment proof, and one fresh independent review. It does not alter security, privacy, live data, remote services, or deployment policy, so Governed ceremony is not justified.

### Material Risks

- The current event infrastructure has no exact event-ID dispatch. Reusing its broad pending-event query would make the result nondeterministic.
- Scheduler cancellation after startup is too late. Replay gating must be effective before prerequisite setup and must cover every event-delivery call.
- A selected event type can have several routes and one handler can match several subscriber aggregates. The unique selected-route/unique-subscriber contract is essential, not an implementation detail.
- Typed runtime baseline bindings are a deliberate change from fully concrete source values. Without a persisted provider/key/type contract, prerequisite setup becomes an untracked input overlay.
- Adding atomic events to normal schedules can multiply WorkloadPlans and recovery positions. Existing caps, exact computed counts, and deterministic ordering must remain honest.
- The positive invariant failure occurs after E at A2 and should use the current marked-domain fallback. Final expected terminal/conformance must be verified from the generated package rather than inferred from the old Spock test.
- The handler's downstream Saga is synchronous but not one cross-handler database transaction. Claiming transactional atomicity would be false.
- A background scheduled method with side effects outside `EventApplicationService` would violate the supported replay shape; static validation should limit the first slice to the currently extracted direct-delegation form.

### Canonical Docs

- `docs/verifiers-impl/current-state.md` — define event consequence at first use; document v4 package/executor behavior, failure boundary, commands, positive/control evidence, and limitations.
- `docs/verifiers-impl/roadmap.md` — mark the harmful generated interaction and its chosen repeatable reset boundary complete only after the evidence passes; keep search entry conditions unchanged.
- `docs/verifiers-impl/decisions/2026-07-30-deterministic-event-consequence-replay.md` — retain the non-obvious v4 normal-schedule/action model, exact selected-event path, logical atomicity, baseline-binding contract, and explicit deferrals.
- `docs/verifiers-impl/decisions/index.md` — link the new active decision.

## Delivery

| Outcome | Status | Proof |
|---|---|---|
| 1. Persisted event-consequence extraction, model, identity, validation, and dummyapp generation | complete | Dummyapp produces stable v4 positive/control normal schedules and checksum-valid package records; focused extraction/model/package tests pass (7 tests), the verifier suite passes (589 tests), and inspection confirms E has no fault/checkpoint source. |
| 2. Atomic executor replay, polling control, causal masking, reporting, baseline bindings, and dummyapp runtime | complete | Focused simulator replay tests pass (5), verifier executor/orchestrator/wrapper tests pass (128), and full simulator/verifier suites pass (111/606); tests cover exact selection, synchronous dispatch, scope cleanup, masking/hard stops, report v5, baseline failures, ImpactV1 non-evaluation, and package hash stability. |
| 3. Reproducible Quizzes positive/control evidence and canonical documentation | complete | Final package `verifiers/target/quizzes-20260801-014405-816/` obeys the 100-workload cap with honest omission accounting; renewed 3+3+1 attempts reproduce ImpactV1 `1`/`0`/`0`, route/masking/setup evidence, and unchanged five-file hashes; docs and reviewer verdict are `PASS`. |

Outcome order is intentional. Outcome 1 freezes the package contract before runtime code consumes it. Outcome 2 proves the generic runtime/reset boundary before Quizzes evidence. Outcome 3 proves final composition rather than driving Quizzes-specific production shortcuts.

## Decisions and Scope Deltas

| ID | Decision | Status | Reason |
|---|---|---|---|
| D1 | Use Guided System coordination with three delivery outcomes and stop after planning. | approved | The user explicitly selected this route and authorized investigation/planning only. |
| D2 | Preserve every supplied semantic boundary and acceptance requirement. | approved | Direct user instruction defines the approved intent. |
| D3 | Use a clean v4 package plus execution-report v5 rather than changing v3 meaning in place. | approved | Approved in the user's implementation request; workload identity, action kinds, validation, accounting, and reader behavior change materially. |
| D4 | Interpret atomicity as synchronous completion of the selected real handler/downstream Saga before the next outer action, not one database transaction. | approved | Approved in the user's implementation request; this matches the existing real application path. |
| D5 | Persist one selected handler route per consequence and require exactly one eligible runtime subscriber; do not replay fan-out. | approved | Approved in the user's implementation request; explicit route plus unique subscriber provides the deterministic contract. |
| D6 | Use a persisted generic prerequisite-provider/binding contract and a Quizzes test-scope provider, with one fresh H2 process per attempt. | approved | Approved in the user's implementation request; runtime-created IDs stay outside package identity and prerequisite work stays outside measurement. |
| D7 | Model E in WorkloadPlan normal structure and as a FaultScenario action, while keeping outer forwards as the only fault/checkpoint sources. | approved | Approved in the user's implementation request; ordering becomes explicit without changing fault, compensation, search, allocation, or ImpactV1 ownership. |

## Acceptance Evidence

- Outcome 1 focused proof: `cd verifiers && mvn -q -Dtest=EventConsequenceVisitorDummyappSpec,EventConsequenceStaticBoundarySpec,DummyappEventConsequencePackageSpec test` passed 7 tests in both implementer and coordinator runs. The package spec compares all five files byte-for-byte across repeated generation and rejects v3 manifest/workload/fault records.
- Outcome 1 broad proof: `cd verifiers && mvn test` passed 589 tests with no failures/errors/skips in the implementer run; final full-suite proof remains due after all outcomes.
- Static inspection confirms `WorkloadPlan.v4` separates `forwardSchedule` from dense `normalSchedule`, `RecoveryScheduleGenerator` materializes E from `sourceEventConsequenceId`, and E has null fault/checkpoint references.
- Outcome 2 focused proof: `cd simulator && mvn -q -Dtest=EventReplayCoordinatorTest test` passed 5 tests; coordinator reran it successfully. `cd verifiers && mvn -Dtest=ScenarioExecutorSpec,ScenarioExecutorOrchestratorSpec,ScenarioExecutorWrapperSpec test` passed 128 tests; coordinator reran the orchestrator/wrapper boundary successfully.
- Outcome 2 broad proof: `cd simulator && mvn test` passed 111 tests and `cd verifiers && mvn test` passed 606 tests in the implementer run; final suite reruns remain due after Outcome 3.
- Runtime inspection confirms replay activation precedes `SpringApplication.run`, unscoped polling is gated in `EventApplicationService`, captured events are selected by persisted ID, and E is rejected inside a fault-vector boundary.
- Final package: `verifiers/target/quizzes-20260801-014405-816/`; positive `24fbeb3b05830543f1deb1aa6eaa0be29e63b2eac807cae3264ce8f1f171e601`, control `ec869302d0912ce60c57a69e9a220dba0afd2aa6246a93fb141f727ffc34ca24`, masking `5b1991d323f2ad8d1066d7c82f33a3559be2444a127adbac07bc6f4c9cd3dede`.
- Final manifest obeys `maxCatalogScenarios=100`: 2 prerequisite + 98 base workloads; it honestly records one cap encounter and 14 omitted base workloads after event expansion. The package contains 21 persisted fault scenarios.
- Final evidence under `verifiers/target/outcome3-ec-rev002-20260801-014405-816-evidence/`: positive 3/3 `PARTIAL_COMPENSATED/DEVIATED`, E completed through Tournament route, ImpactV1 `1`; control 3/3 `SUCCESS/EXACT`, same route, ImpactV1 `0`; masking `PARTIAL_COMPENSATED/EXACT`, B1 fault realized, E `MASKED_BY_TRIGGER_FAULT`, ImpactV1 `0`. All setup records succeeded, cleared prerequisite events, resolved four typed bindings, and proved an empty pending-event baseline.
- Every post-attempt and final hash snapshot matches `package-before.sha256` for all five files. Coordinator inspected the manifest/seven report pairs/hash snapshots and reran focused generation/extraction/Quizzes checks.
- Final affected checks: simulator 111 passing, verifier 628 passing, focused Quizzes provider/oracle 10 passing, verifier docs build passing, `git diff --check` passing. Full Quizzes Saga suite previously ran 152 tests: 145 passed and seven unrelated async assertions failed because they expect `SimulatorException` directly while the unchanged async path wraps it in `CompletionException`.
- Independent reviewer session `019fba96-b770-7a13-9b8f-55adfb27557b` resolved EC-REV-001–006 over two cycles and returned `PASS`; details are in `review.md`.
- Canonical docs describe v4/current evidence and the active deterministic event-consequence decision; root `ROADMAP.md` records the verified cross-workspace milestone boundary.
- Existing static event work proves handler-side topology but records event payload placeholders rather than producer-to-runtime consequence identity.
- The real Quizzes oracle reaches exactly one `SagaTournament` invariant finding at `AddParticipantFunctionalitySagas.addParticipantStep`.
- Code inspection confirms the selected Tournament handler reaches `UpdateUserNameFunctionalitySagas.executeWorkflow` synchronously, while current event selection scans all pending events/subscribers and current scheduler stop lacks a deterministic startup barrier.
- Git was clean before creating this planning artifact. No source code, tests, canonical docs, commits, pushes, PRs, or live/private systems were changed.

## Completion

Delivered deterministic v4 event-consequence extraction, package identity/validation/accounting, Saga/local replay isolation, exact real-handler dispatch, causal masking and hard-stop semantics, typed prerequisite baselines, report v5, and repeatable Quizzes positive/control evidence.

Known limits remain the approved non-goals: one unique local subscriber and one non-recursive consequence; no event faults/compensation/retries, fan-out, distributed transports, cross-handler transaction, v3 migration, or same-process reset. Seven unchanged Quizzes async exception-wrapper assertions remain unrelated follow-up scope.

Canonical docs updated: `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/roadmap.md`, `docs/verifiers-impl/README.md`, `docs/verifiers-impl/decisions/2026-07-30-deterministic-event-consequence-replay.md`, `docs/verifiers-impl/decisions/index.md`, and `mkdocs.verifier.yml`; cross-workspace `../ROADMAP.md` was reconciled.

All changes are uncommitted on `fault-analysis/scenarios`. No commit, push, PR, merge, deployment, or live/private-system action was performed or authorized.
