# Spec: Multi-Saga Scenario Executor Interleavings

## Problem

The verifier can generate bounded multi-saga `ScenarioPlan` records with deterministic schedules, but the ScenarioExecutor can currently execute only one materializable single-saga saga/local scenario per attempt. This blocks the next thesis/runtime milestone: proving that generated multi-saga interleavings can be replayed from the catalog, faulted with a scenario-wide binary vector, and observed through a report shape suitable for future impact scoring.

The user needs a generic verifier-owned executor contract, not a Quizzes-specific harness and not a full runtime scenario engine. The executor should execute catalog-defined deterministic interleavings while keeping fixture generation, true concurrency, distributed runtime parity, impact scoring, and search out of this feature.

## Solution

Extend the ScenarioExecutor to support one explicit materializable multi-saga saga/local scenario execution attempt by treating `ScenarioPlan.expandedSchedule` as the runtime interleaving contract.

For a supported multi-saga plan, the executor will create one scenario execution participant per `SagaInstance`, materialize each participant input under the existing ScenarioExecutor materialization policy, instantiate one runtime functionality and one unit of work per participant, execute scheduled forward steps in ascending `scheduleOrder`, and route binary fault vectors through the simulator in-memory fault-vector provider using scenario/saga/scheduled-step identity.

Scheduled-step runtime failures use **compensate-and-continue**. When a participant realizes an assigned fault or throws a scheduled-step domain/simulator exception, the executor compensates that participant immediately when possible, marks that participant terminal, skips that participant's remaining forward steps, and continues surviving participants according to the catalog schedule. Surviving participants are closed at the end. This gives future impact scoring a meaningful observation boundary without claiming delayed compensation, retry, true parallel execution, or distributed runtime parity.

Execution reports move to a v3 participants model. Scenario-level facts remain top-level; saga-specific facts live under required participant entries. Single-saga execution becomes the one-participant case of the same v3 report model and keeps the existing single-saga execution mechanics, but not v2 report compatibility fields or v2 terminal-status aliases such as `FAULT_COMPENSATED`.

## Domain References

- Glossary/context: `docs/verifiers-impl/glossary.md` — `Scenario`, `ScenarioPlan`, `Scheduled step`, `Scenario execution attempt`, `Scenario execution participant`, `Deterministic interleaving replay`, `Compensate-and-continue`, `Deferred-compensation continuation`, `Fault vector`, `Fault slot`, `ScenarioExecutor supported path`.
- Decision frame: `issues/2026-07-08-multi-saga-scenario-executor/decision-frame.md`.
- Planning audit: `issues/2026-07-08-multi-saga-scenario-executor/quizzes-materializability-audit.md`.
- ADR: `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md` — scenario catalog JSONL remains the static executor contract.
- ADR: `docs/verifiers-impl/decisions/2026-04-28-hybrid-static-dynamic-key-binding.md` — dynamic evidence enriches but does not redefine static scenario structure.
- ADR: `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md` — segment-compressed schedules are static schedule shapes, not runtime impact proof.
- ADR: `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md` — accepted compensate-and-continue policy and alternatives for advisor review.
- ADR: `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md` — accepted v3 participants report model.
- Current executor reference: `docs/verifiers-impl/reference/scenario-executor.md` — current single-saga behavior and command shape.

## Scope

### In Scope

- Explicit execution of one selected multi-saga `ScenarioPlan` per scenario execution attempt.
- Saga/local runtime path only.
- Deterministic interleaving replay using the catalog's `expandedSchedule` in ascending `scheduleOrder`.
- One scenario execution participant per `SagaInstance`.
- Per-participant materialization, startup, step execution, failure, compensation, closure, and reporting.
- Existing runtime-owned argument resolution for `SagaUnitOfWorkService`, `CommandGateway`, and `SagaUnitOfWork`.
- Existing supported recipe materialization semantics; no new generic fixture synthesis.
- Scenario-wide binary fault vectors aligned to `ScenarioPlan.faultSpace`.
- Multiple assigned faults may realize across different still-alive participants in one attempt.
- Compensate-and-continue for scheduled-step runtime failures.
- v3 execution reports with canonical participant entries for both single-saga and multi-saga runs.
- Dry-run validation/mapping for explicit multi-saga scenarios without runtime execution.
- Dummy/synthetic automated coverage and a real Quizzes multi-saga smoke using a currently materializable candidate.
- Documentation updates after implementation to reflect the new supported path and limitations.

### Out of Scope

- True parallel/threaded execution.
- Generic distributed, stream, or gRPC runtime parity.
- TCC execution.
- Generic fixture/database seeding, reset, or teardown.
- Replaying Spock setup methods or test helper methods.
- Event-origin payload reconstruction.
- New materializer support beyond what is necessary to use currently materializable multi-saga candidates.
- Compensation-step faults.
- Delay injection or non-binary impairments.
- Retry policy after participant failure.
- Deferred-compensation continuation.
- Configurable failure-policy matrix.
- Batch execution.
- Domain-impact scoring implementation.
- Genetic/local search over fault vectors.
- Scenario prioritization or bandit allocation.
- Quizzes-specific executor shortcuts.
- Rewriting static catalog, enriched catalog, dynamic evidence, or dynamic join-report sidecars from executor output.

## Actors and Permissions

| Actor | Can / Cannot | Notes |
|-------|--------------|-------|
| Verifier author / executor caller | Can run one explicit scenario execution attempt from a static or enriched catalog. | Multi-saga execution requires an explicit scenario id. |
| Verifier author / executor caller | Can provide an explicit binary fault vector for the selected scenario. | Explicit vectors still require explicit scenario id and must align to the plan fault space. |
| ScenarioExecutor | Can materialize supported participant inputs and provide runtime-owned arguments. | It must not guess source-owned unresolved values or synthesize fixtures. |
| ScenarioExecutor | Can write executor-specific v3 execution reports. | It must not modify catalog, enriched catalog, dynamic evidence, or join-report artifacts. |
| Future impact scorer | Can consume v3 reports later. | Scoring is out of scope for this feature, but report semantics must be suitable for it. |

## User Stories

1. As the verifier author, I want to execute a generated multi-saga scenario plan, so that I can prove catalog interleavings can cross into runtime execution.
2. As the verifier author, I want the executor to follow `expandedSchedule`, so that the runtime attempt corresponds to the static scenario contract.
3. As the verifier author, I want each saga instance reported as a participant, so that multi-saga outcomes are auditable without misleading top-level single-saga fields.
4. As the verifier author, I want assigned faults to remain scenario-wide, so that multiple participants can realize assigned faults in one attempt when the schedule reaches them.
5. As the verifier author, I want compensate-and-continue behavior, so that future impact scoring can observe downstream effects after a participant failure.
6. As the verifier author, I want executor/infrastructure failures to remain hard stops, so that broken setup is not confused with meaningful domain impact.
7. As the verifier author, I want a real Quizzes smoke, so that the feature proves more than synthetic executor behavior.
8. As a future impact-scoring consumer, I want clean participant and terminal-status semantics, so that final-state scoring can distinguish clean success, compensated failures, mixed outcomes, and compensation failures.

## Behavior and Edge Cases

- Multi-saga execution requires explicit `--scenario-id`. Auto-selection remains limited to currently supported single-saga candidates until a later selection/prioritization feature.
- A supported multi-saga scenario must have exactly one matching input variant for each `SagaInstance`.
- For non-dry-run execution, all participant inputs must be materializable under current ScenarioExecutor materialization semantics before runtime execution begins.
- If any participant cannot be materialized, the attempt fails before runtime execution with a structured materialization failure report.
- If any participant cannot be instantiated or started, the attempt hard-stops before scheduled-step execution with a structured startup failure report. Startup failure is a pre-forward phase: no scheduled steps execute, no final `resumeWorkflow` closure runs, and no scenario compensation is attempted. Participants already materialized or startup-ready are reported with their participant states, but are not closed through saga lifecycle methods.
- Dry-run mode validates participant mapping, input mapping, step-name mapping, and fault-vector mapping, writes a v3 report, and performs no materialization, no runtime-owned argument resolution, and no runtime execution.
- Runtime step names continue to be derived from catalog step ids by taking the text after the final `::` and removing one trailing `#<digits>` suffix.
- Scheduled steps execute in ascending `scheduleOrder`. Each scheduled step is dispatched to the participant whose `sagaInstanceId` matches the scheduled step.
- Step ids that cannot be mapped to a nonblank runtime step name are unsupported and fail before runtime execution.
- Fault slots are scenario-level and aligned by index to `ScenarioPlan.faultSpace.scheduledStepIds`.
- Explicit fault vectors must be binary and have length equal to the scenario fault-space length.
- Assigned slots in surviving participants remain active even after another participant fails.
- If a participant realizes an assigned fault, the executor records the realized fault slot, compensates that participant immediately when possible, skips that participant's remaining scheduled forward steps, and continues other participants.
- If a participant throws a non-assigned scheduled-step runtime exception, the executor treats it as a scenario outcome, compensates that participant immediately when possible, skips that participant's remaining scheduled forward steps, and continues other participants.
- If participant compensation fails, the failed participant remains terminal with lifecycle `COMPENSATION_FAILED`, surviving participants continue, and the top-level terminal status is `COMPENSATION_FAILED`.
- Failed-and-compensated participants are terminal and must not be closed again through final `resumeWorkflow`.
- Surviving participants close after scheduled forward execution completes. Closure order is ascending order of each participant's last executed scheduled step. Participants with no executed scheduled steps close afterward in deterministic saga-instance id order.
- If all participants commit without scheduled-step failures, terminal status is `SUCCESS`.
- If all started/executed participants that reach terminal outcome are compensated and no participant commits, terminal status is `COMPENSATED`.
- If at least one participant compensates and at least one surviving participant commits, terminal status is `PARTIAL_COMPENSATED`.
- If any participant compensation fails, terminal status is `COMPENSATION_FAILED`.
- The CLI/Docker process exits zero only for `SUCCESS`, `COMPENSATED`, `PARTIAL_COMPENSATED`, and `DRY_RUN`. It exits non-zero for `COMPENSATION_FAILED` and for every hard-stop/setup/validation/provider/configuration/report-writing failure.
- Executor/infrastructure failures remain hard stops and are not treated as domain scenario outcomes. This includes selection failures, invalid vectors, unsupported scenario shapes, materialization failures, startup failures, provider mismatches, configuration failures, and report-writing failures.
- Later assigned slots in a failed participant are `MASKED_BY_SAGA_FAILURE`.
- Assigned slots skipped because the whole execution attempt hard-stopped before reaching them are `NOT_REACHED`.
- Assigned slots that are reached but fail to inject as expected remain validation failures, not domain outcomes.
- The executor does not retry failed participants or synthesize compensating/recovery schedules beyond the current immediate compensation call.

## Data and Lifecycle Rules

- Source of truth: the static `ScenarioPlan` embedded in `scenario-catalog.jsonl` or in an enriched catalog wrapper. Enrichment metadata may influence existing single-saga auto-selection, but execution uses the embedded static plan as the contract.
- Scenario identity: `ScenarioPlan.deterministicId` identifies the static scenario shape. Fault vectors are execution-time choices and do not create new scenario plan ids.
- Execution identity: each scenario execution attempt receives its own `scenarioExecutionId`.
- Participant identity: each v3 participant corresponds to one catalog `SagaInstance` and carries that saga instance id, saga FQN, and input variant id.
- Report schema: v3 reports use schema version `microservices-simulator.scenario-execution-report.v3`.
- Participant list: the top-level `participants` list is required. It is empty only when no scenario plan was selected. For a selected plan, it contains exactly one participant per catalog `SagaInstance`; single-saga execution therefore has exactly one participant.
- v3 compatibility boundary: v3 reports do not preserve v2 top-level `sagaInstanceId`, `sagaFqn`, or `inputVariantId` fields, and do not use v2 terminal-status aliases such as `FAULT_COMPENSATED`.
- Fault data: assigned vector, vector source, provider mode, fault slots, scheduled-step mapping, and exact fault-slot realization states are part of the execution report.
- Top-level `terminalStatus` values:
  - `SUCCESS` — all participants commit without scheduled-step failures.
  - `COMPENSATED` — every started/executed participant that reaches terminal outcome compensates and no participant commits.
  - `PARTIAL_COMPENSATED` — at least one participant compensates and at least one surviving participant commits.
  - `COMPENSATION_FAILED` — at least one participant compensation fails; the report is written when possible, but the process exits non-zero.
  - `DRY_RUN` — mapping validation completed without materialization or runtime execution.
  - Hard-stop/setup/validation statuses: `SELECTION_FAILED`, `UNSUPPORTED_SCENARIO`, `INVALID_FAULT_VECTOR`, `MATERIALIZATION_FAILED`, `STARTUP_FAILED`, `EXPECTED_FAULT_NOT_INJECTED`, `UNEXPECTED_INJECTED_FAULT`, `FAULT_PROVIDER_MISMATCH`, `UNEXPECTED_EXECUTION_FAILURE`, `CONFIGURATION_FAILED`, and `REPORT_WRITE_FAILED`.
- Process exit contract: zero for `SUCCESS`, `COMPENSATED`, `PARTIAL_COMPENSATED`, and `DRY_RUN`; non-zero for `COMPENSATION_FAILED` and all hard-stop/setup/validation/provider/configuration/report-writing failures.
- Participant state vocabulary:
  - `materializationState`: `NOT_ATTEMPTED`, `MATERIALIZED`, `MATERIALIZATION_FAILED`.
  - `startupState`: `NOT_ATTEMPTED`, `STARTUP_READY`, `STARTUP_FAILED`.
  - `lifecycleOutcome`: `NOT_STARTED`, `COMMITTED`, `COMPENSATED`, `COMPENSATION_FAILED`, `CLOSURE_SKIPPED`.
- Step outcome vocabulary: `DRY_RUN`, `COMPLETED`, `INJECTED_FAULT`, `FAILED`, `EXPECTED_FAULT_NOT_INJECTED`, `SKIPPED_BY_SAGA_FAILURE`, and `NOT_EXECUTED_HARD_STOP`.
- Fault-slot realization states:
  - `NOT_ASSIGNED` — slot bit is `0`.
  - `DRY_RUN` — assigned slot was mapped but intentionally not executed in dry-run mode.
  - `REALIZED` — assigned slot was reached and injected by the simulator provider.
  - `MASKED_BY_SAGA_FAILURE` — assigned slot belongs to a participant after that participant's terminal failure point.
  - `NOT_REACHED` — assigned slot was skipped because the whole execution attempt hard-stopped before reaching it.
  - `EXPECTED_FAULT_NOT_INJECTED` — assigned slot was reached but the simulator provider did not inject the expected fault; terminal status is `EXPECTED_FAULT_NOT_INJECTED`.
- `vectorSource` values: `DEFAULT_VECTOR`, `EXPLICIT_VECTOR`.
- `providerMode` values: `IN_MEMORY_FAULT_VECTOR`, `NONE`.
- Blocker placement: scenario-level blockers describe selection, vector, unsupported-shape, provider, configuration, and report-writing failures; participant-local blockers describe materialization, startup, scheduled-step, compensation, and closure details for the owning participant.
- Report lifecycle: the executor writes a standalone v3 execution report to the configured output path when possible. It does not modify static catalogs, enriched catalogs, dynamic evidence artifacts, or join-report sidecars.
- Data retention: report retention is caller/artifact-directory responsibility, matching existing verifier output behavior.
- Trust and caveats: v3 reports describe runtime attempts under the selected local/saga executor semantics. They are not dynamic-enrichment sidecars, not proof of distributed parity, and not domain-impact scores.

## Existing System Fit

- Preserve the verifier pipeline boundary: static scenario generation produces `ScenarioPlan`; ScenarioExecutor consumes the catalog and writes executor-specific output.
- Keep static dynamic-enrichment semantics unchanged: enriched records are wrappers and do not redefine static scenario structure.
- Reuse existing simulator in-memory fault-vector provider identity fields: scenario execution id, scenario plan id, saga instance id, scheduled step id, functionality identity, runtime step name, and assigned bit.
- Reuse existing runtime-owned argument policy for executor materialization.
- Reuse existing command/runner shape where possible: target app classpath, Spring application class, Spring profiles, application base/id, Maven profile, catalog path, scenario id, output path, dry-run, and optional fault vector.
- Keep Docker Compose executor runner semantics aligned with the CLI, adding only what is necessary to run explicit multi-saga plans and write v3 reports.
- Keep current catalog generation and accounting semantics unchanged. Multi-saga executor support consumes already-generated multi-saga plans; it does not change which plans are generated.

## Research Notes

No external library research was required.

A planning audit was required to decide whether Quizzes materializability blocks this feature. The audit generated a bounded Quizzes multi-saga-only `WRITE_PLANS` catalog and evaluated it with the actual `ScenarioExecutorReadinessEvaluator`.

Audit result:

```text
totalPlans=500
multiSagaPlans=500
uniqueInputs=52
materializableUniqueInputs=4
materializableMultiSagaPlans=8
```

Example materializable Quizzes plan:

```text
plan id: 0945caa9ac2fe06a268e6df6aa992fcf69e253116264684d577bdbbb955c2e25
participants: CreateCourseExecutionFunctionalitySagas, GetCourseExecutionsFunctionalitySagas
fault-space length: 5
default vector: 00000
```

Conclusion: this feature should proceed. A separate materializability-first feature is not required before implementing the multi-saga executor, although richer future impact examples may still require broader materialization and fixture-readiness work.

## Acceptance Criteria

- [ ] AC-1: The executor can load a static or enriched catalog containing multi-saga `ScenarioPlan` records without modifying any catalog or enrichment artifact.
- [ ] AC-2: An explicit `--scenario-id` can select a multi-saga scenario plan by `ScenarioPlan.deterministicId`.
- [ ] AC-3: Multi-saga execution without explicit `--scenario-id` is not auto-selected in this feature; existing auto-selection remains limited to supported single-saga default-vector behavior.
- [ ] AC-4: Explicit missing scenario id still fails with a clear selection failure before materialization or runtime execution.
- [ ] AC-5: Explicit unsupported multi-saga shapes fail before runtime execution with structured blockers.
- [ ] AC-6: A supported multi-saga scenario requires every `SagaInstance` to have exactly one matching input variant in the selected plan.
- [ ] AC-7: A supported non-dry-run multi-saga scenario requires every participant input to be materializable under current ScenarioExecutor materialization semantics plus existing runtime-owned argument resolution.
- [ ] AC-8: If any participant input is not materializable, the attempt writes a v3 report with materialization blockers and performs no runtime step execution.
- [ ] AC-9: If any participant cannot be instantiated or started, the attempt writes a v3 startup failure report with participant startup states, performs no scheduled-step execution, and does not close or compensate participants through saga lifecycle methods.
- [ ] AC-10: Dry-run mode for an explicit multi-saga scenario validates participant mapping, step mapping, and fault-vector mapping, writes a v3 report, and performs no materialization, no runtime-owned argument resolution, and no runtime execution.
- [ ] AC-11: Runtime step names for multi-saga scheduled steps use the existing final-`::` plus trailing-`#<digits>` removal rule.
- [ ] AC-12: Invalid or non-mappable scheduled step ids produce structured unsupported-step blockers before runtime execution.
- [ ] AC-13: Runtime execution follows `ScenarioPlan.expandedSchedule` in ascending `scheduleOrder`.
- [ ] AC-14: Each scheduled step executes against the participant identified by that scheduled step's `sagaInstanceId`.
- [ ] AC-15: The executor creates and tracks one runtime functionality/session and one unit of work per participant.
- [ ] AC-16: Successful surviving participants are closed at the end in ascending order of their last executed scheduled step, with zero-step participants closed afterward in deterministic saga-instance id order.
- [ ] AC-17: Failed-and-compensated participants are terminal and are not closed again during final participant closure.
- [ ] AC-18: Default-vector multi-saga execution uses `ScenarioPlan.faultSpace.defaultVector` and records `vectorSource=DEFAULT_VECTOR` in the v3 report.
- [ ] AC-19: Explicit multi-saga fault vectors must be binary and must match `faultSpace.length`; invalid vectors fail before runtime execution.
- [ ] AC-20: Fault-slot mapping remains scenario-level and deterministic across all scheduled steps in `faultSpace.scheduledStepIds`, with v3 realization states limited to `NOT_ASSIGNED`, `DRY_RUN`, `REALIZED`, `MASKED_BY_SAGA_FAILURE`, `NOT_REACHED`, and `EXPECTED_FAULT_NOT_INJECTED`.
- [ ] AC-21: A participant assigned fault realizes only when the matching scheduled step, saga instance, runtime step name, and slot index are reached.
- [ ] AC-22: Multiple assigned faults across different surviving participants can realize in one scenario execution attempt.
- [ ] AC-23: Later assigned slots in a failed participant are reported as `MASKED_BY_SAGA_FAILURE`.
- [ ] AC-24: Assigned slots in surviving participants remain active after another participant fails and may still realize later in the same attempt.
- [ ] AC-25: Assigned slots skipped because the whole execution attempt hard-stopped before reaching them are reported as `NOT_REACHED`.
- [ ] AC-26: Assigned slots reached without expected simulator fault injection fail the attempt with terminal status `EXPECTED_FAULT_NOT_INJECTED`, fault-slot state `EXPECTED_FAULT_NOT_INJECTED`, and a validation blocker, not as a domain scenario outcome.
- [ ] AC-27: Assigned fault realization in a participant triggers immediate compensation for that participant when possible, skips that participant's remaining forward scheduled steps, and continues surviving participants.
- [ ] AC-28: Non-assigned scheduled-step runtime exceptions trigger the same compensate-and-continue behavior as assigned faults.
- [ ] AC-29: Participant compensation failure marks that participant `COMPENSATION_FAILED`, skips its remaining forward steps, continues surviving participants, and produces top-level `COMPENSATION_FAILED`.
- [ ] AC-30: Executor/infrastructure failures remain hard-stop statuses and are not treated as domain scenario outcomes.
- [ ] AC-31: Top-level terminal status is `SUCCESS` only when all participants commit without scheduled-step failures.
- [ ] AC-32: Top-level terminal status is `COMPENSATED` when all started/executed participants that reach terminal outcome compensate and no participant commits.
- [ ] AC-33: Top-level terminal status is `PARTIAL_COMPENSATED` when at least one participant compensates and at least one surviving participant commits.
- [ ] AC-34: Top-level terminal status is `COMPENSATION_FAILED` when any participant compensation fails.
- [ ] AC-35: The v3 execution report is the canonical report shape for both single-saga and multi-saga attempts and uses schema version `microservices-simulator.scenario-execution-report.v3`.
- [ ] AC-36: The v3 execution report has a required participant list; each participant includes saga instance id, saga FQN, input variant id, materialization/startup state, lifecycle outcome, step outcomes, skipped steps, and participant-local blockers/failure details using the vocabulary defined in this spec.
- [ ] AC-37: The v3 execution report keeps scenario-level facts top-level, including scenario execution id, scenario plan id, scenario kind, terminal status, assigned vector, vector source, provider mode, runtime metadata, fault slots, skipped-candidate counts, and scenario-level blockers using the vocabulary defined in this spec.
- [ ] AC-38: The v3 execution report does not preserve v2 top-level single-saga identity fields as compatibility fields and does not use v2 terminal-status aliases such as `FAULT_COMPENSATED`.
- [ ] AC-39: Single-saga execution writes v3 reports with exactly one participant and continues to support the existing single-saga execution mechanics through the new report model.
- [ ] AC-40: Dummy/synthetic automated coverage proves explicit multi-saga dry-run, successful interleaving execution, compensate-and-continue after assigned fault, compensate-and-continue after non-assigned runtime exception, multiple faults across participants, compensation failure continuation, final closure ordering, exact v3 status/state vocabulary, CLI exit-code mapping, and v3 report shape.
- [ ] AC-41: Quizzes validation generates or consumes a bounded multi-saga `WRITE_PLANS` catalog and executes at least one currently materializable multi-saga plan through the forked/Docker scenario-executor path.
- [ ] AC-42: The Quizzes smoke records catalog path, scenario plan id, participants, schedule, assigned vector, terminal status, participant lifecycle outcomes, and report path.
- [ ] AC-43: Documentation updates after implementation revise the ScenarioExecutor reference/current-state language to describe supported multi-saga deterministic interleaving replay and its limitations.
- [ ] AC-44: CLI and Docker runner exit zero for `SUCCESS`, `COMPENSATED`, `PARTIAL_COMPENSATED`, and `DRY_RUN`; they exit non-zero for `COMPENSATION_FAILED` and for all hard-stop/setup/validation/provider/configuration/report-writing failures.

## Assumptions

- The first multi-saga executor support is deterministic sequential interleaving replay, not true parallel execution.
- The first Quizzes smoke can use the materializable `CreateCourseExecutionFunctionalitySagas` / `GetCourseExecutionsFunctionalitySagas` class of candidates found during planning, unless implementation discovers a better materializable candidate.
- Existing runtime-owned argument resolution remains sufficient for at least one Quizzes smoke candidate.
- Persistent environment setup/reset remains caller/orchestrator responsibility.
- Advisor review may later request a deferred-compensation experimental policy, but that does not block this first compensate-and-continue executor feature.

## Open Questions

None blocking.

Non-blocking advisor-review item: whether deferred-compensation continuation should become a later explicit experimental policy for impact-scoring evaluation.

## Handoff

Ready for implementation planning: yes

Continue with `sp-implementation-plan`.
