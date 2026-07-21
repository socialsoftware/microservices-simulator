# Spec: Compensation-Aware Scenario Catalog and Replay

## Problem

The verifier currently emits one `ScenarioPlan` shape containing participants, inputs, a forward schedule, and a compact `FaultSpace`. Fault vectors are supplied later as executor input, and participant compensation happens immediately as an executor policy rather than as cataloged scenario semantics. This prevents the catalog from identifying and replaying the recovery timings discussed with the advisor: a failed Saga may compensate before, between, or after the remaining forward steps of still-live Sagas.

The thesis author needs a reproducible experimental contract where one concrete scenario includes its workload, assigned failures, and compensation ordering. The contract must expose intermediate recovery windows for later anomaly detection without eagerly multiplying the full Quizzes workload space by every fault vector and recovery interleaving. It must also preserve arbitrary multi-fault execution for later GA candidates, keep current runtime/materialization boundaries honest, and avoid treating executor failures as domain outcomes.

## Solution

Replace the v2 single-record catalog with a clean v3 package containing two linked semantic levels:

1. A **WorkloadPlan** records reusable normal-execution structure: Saga participants, accepted input variants, one deterministic forward interleaving, conflict evidence, ordered forward fault slots, and ordered compensation checkpoints with their primary evidence classes. All of those semantic fields contribute to WorkloadPlan identity; diagnostic-only warnings do not.
2. A **FaultScenario** references one WorkloadPlan and records one assigned binary fault vector plus one concrete compensation-aware action schedule. Its stable identity includes the WorkloadPlan id, assigned vector, and ordered action identities. An action identity includes action kind, owning participant, source forward-slot/checkpoint identity, and occurrence identity; every field that changes replay semantics participates either directly or through the referenced WorkloadPlan/vector.

The bounded eager baseline writes FaultScenarios only for executor-materializable WorkloadPlans: one all-zero scenario and every single-point fault vector. Recovery schedules are limited by a configurable positive per-vector cap, default `20`. Selection preserves deterministic earliest, latest, alternating, and final-step/automatic-commit boundary representatives before filling remaining capacity in canonical order. Arbitrary multi-fault vectors remain available through explicit on-demand generation: requested variants are persisted and receive stable FaultScenario ids before any execution.

Compensation checkpoints are retained when static evidence is explicit, implies Saga rollback/write effects, or remains conservatively unknown. Only confidently effect-free steps are omitted. Assigned faults follow the persisted recovery schedule. Recovery generation preserves the complete residual WorkloadPlan forward sequence globally and only inserts compensation actions; it never re-interleaves surviving participants' forward actions. A zero-bit failure explicitly classified as a domain failure is a meaningful observed deviation and uses deterministic immediate compensate-and-continue. Plain or unmarked `SimulatorException`, unknown, executor, and infrastructure failures hard-stop. Any thrown compensation action also hard-stops and invalidates the measured attempt; no automatic retry loop or compensation-fault search is introduced.

A participant commits automatically when its final successful forward step completes. Commit is reported as a lifecycle event but is not an independently schedulable action. Moving that final forward action relative to another participant's compensation already models conclusion before, during, or after recovery.

## Domain References

- Glossary/context: `docs/verifiers-impl/glossary.md` — `Workload plan`, `Fault scenario`, `Compensation checkpoint`, `Compensation evidence class`, `Recovery schedule`, `Recovery-schedule cap`, `Automatic participant commit`, and `Unassigned runtime fallback`.
- Decision frame: `issues/2026-07-19-compensation-aware-scenario-catalog/decision-frame.md`.
- ADR: `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md` — selected two-level v3 contract and recovery/failure semantics.
- Superseded catalog decision: `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md` — retained as v1/v2 history.
- Partially superseded failure-policy decision: `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md` — current immediate-compensation implementation and retained zero-bit fallback rationale.
- Existing schedule-reduction decision: `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md` — WorkloadPlan forward schedule generation remains static conflict-anchor compression where configured.
- Current execution report decision: `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md` — participant-oriented reporting remains the base, but action-aware planned/actual reporting requires a new report version.

## Scope

### In Scope

- Clean v3 catalog package with separate WorkloadPlan and FaultScenario JSONL artifacts plus one linking manifest and compensation-aware accounting.
- Deterministic WorkloadPlan identity over participants, inputs, global forward schedule, conflict evidence, ordered fault slots, and ordered compensation checkpoints with primary evidence classes.
- Deterministic FaultScenario identity over WorkloadPlan id, assigned vector, and ordered action identities.
- Forward and compensation action kinds in concrete FaultScenario schedules.
- Preservation of compensation phase/evidence from static analysis into the workload model.
- Compensation evidence classes `EXPLICIT_COMPENSATION`, `IMPLICIT_SAGA_ROLLBACK`, and `CONSERVATIVE_UNKNOWN`.
- Omission only of steps confidently shown to be effect-free.
- Eager all-zero plus single-point FaultScenario generation for executor-materializable WorkloadPlans.
- Configurable positive recovery-schedule cap, default `20`.
- Deterministic coverage-first schedule selection under the cap.
- Explicit on-demand generation and persistence for arbitrary valid multi-fault vectors on one materializable WorkloadPlan.
- Layered exact scenario-space accounting without claiming an exact all-vector recovery total.
- Saga/local deterministic sequential replay of one persisted FaultScenario per execution attempt.
- Stepwise assigned-fault compensation interleaved with still-valid forward actions.
- Automatic participant commit after its final successful forward action.
- Immediate compensate-and-continue fallback for zero-bit simulator/application failures carrying the explicit domain-failure marker; plain or unmarked exceptions hard-stop conservatively.
- Hard-stop behavior for executor/infrastructure failures and thrown compensation actions.
- Action-aware execution reporting with planned and actual order, schedule-conformance state, fault origin, lifecycle events, compensation evidence, skipped/masked slots, and failure details.
- Dynamic enrichment linkage to WorkloadPlans without rewriting WorkloadPlan or FaultScenario structure.
- Dummyapp-first automated coverage and bounded real Quizzes generation/execution evidence.
- Current/live verifier documentation updates after implementation.

### Out of Scope

- Reading, writing, upgrading, or replaying v2 scenario catalogs after v3 becomes canonical.
- Eager enumeration of every `2^n` vector.
- Exact aggregate recovery-schedule counting across every vector in the full `2^n` space.
- Eager FaultScenario generation for currently non-materializable WorkloadPlans.
- Generic materialization of currently unsupported inputs or event payload placeholders.
- Compensation-step faults or any non-forward fault slots.
- Automatic compensation retries, retry counts, retry backoff, or permanently stuck compensation modeling.
- Delay injection or other non-binary impairments.
- Independent delayed-commit scheduling after a participant's final forward step.
- True parallel/threaded execution or distributed, stream, gRPC, or TCC parity.
- Generic fixture/database seeding, reset, or teardown.
- Impact scoring or anomaly-detector implementation.
- GA/local search implementation or scenario prioritization/bandit allocation.
- Quizzes-specific generation or execution shortcuts.

## Actors and Permissions

| Actor | Can / Cannot | Notes |
|-------|--------------|-------|
| Verifier author / operator | Can generate the bounded v3 WorkloadPlan and eager FaultScenario package. | Materialization caps and configured workload bounds remain explicit. |
| Verifier author / operator | Can request one arbitrary binary vector for one WorkloadPlan. | The WorkloadPlan must be materializable; generated variants are persisted before execution. |
| Scenario generator | Can write deterministic workload/fault catalogs, manifest, diagnostics, and accounting. | It must not fabricate input values or silently include non-materializable workloads in the eager FaultScenario baseline. |
| ScenarioExecutor | Can execute one selected persisted FaultScenario and write an action-aware report. | It cannot accept an unpersisted vector overlay or modify catalog records during execution. |
| Dynamic enrichment pipeline | Can attach runtime evidence to WorkloadPlans as sidecar data. | It cannot create or rewrite WorkloadPlans or FaultScenarios. |
| Future search/impact consumer | Can request/persist FaultScenarios and consume reports later. | Search, scoring, and prioritization algorithms are outside this feature. |

## User Stories

1. As the thesis author, I want normal workloads separated from concrete fault/recovery experiments, so that scenario identity is precise without duplicating workload data.
2. As the verifier author, I want assigned vectors and compensation ordering persisted before execution, so that every experiment is reproducible.
3. As the verifier author, I want compensation to interleave with still-live Sagas, so that recovery windows can expose domain anomalies.
4. As the verifier author, I want compensation candidates labeled by evidence quality, so that uncertain static analysis remains visible rather than fabricated as exact.
5. As the verifier author, I want a bounded all-zero/single-point baseline, so that Quizzes produces an executable experimental set before GA exists.
6. As the verifier author, I want arbitrary multi-fault vectors available on demand, so that current capability and future GA candidates are preserved without eager `2^n` expansion.
7. As the thesis author, I want exact layered accounting, so that reported scenario-space numbers remain defensible.
8. As the executor operator, I want the executor to follow one persisted action schedule, so that planned recovery timing matches runtime behavior when no deviation occurs.
9. As a future impact consumer, I want planned and actual actions plus lifecycle events, so that exact replay can be distinguished from meaningful zero-bit runtime deviations.
10. As the verifier author, I want infrastructure and compensation failures separated from domain outcomes, so that broken experiments are not scored as application impact.

## Behavior and Edge Cases

### Workload and fault-scenario generation

- WorkloadPlan generation preserves the configured bounded Saga-set, input-tuple, conflict-evidence, and forward-schedule behavior. Segment compression still reduces normal forward schedules; it does not itself generate recovery schedules.
- Each WorkloadPlan contains ordered forward fault slots aligned one-to-one with its forward scheduled-step occurrences.
- The WorkloadPlan retains compensation candidates separately from forward footprints and records one primary evidence class per retained checkpoint.
- Evidence precedence is deterministic: `EXPLICIT_COMPENSATION` when compensation registration is statically established; otherwise `IMPLICIT_SAGA_ROLLBACK` when resolved forward evidence establishes write/Saga-state rollback potential; otherwise `CONSERVATIVE_UNKNOWN` unless the effect-free proof rule succeeds.
- Explicit registration is preserved even when the verifier cannot resolve a command/dispatch inside the compensation body. Recognized body footprints remain supporting evidence but are not required to retain the checkpoint.
- A step is confidently effect-free only when explicit compensation registration is absent, all relevant static paths/dispatches are resolved, every extracted forward footprint is read-only, no Saga-state/write rollback potential is present, and no unresolved-analysis diagnostic weakens that conclusion. Every other non-explicit/non-implicit case remains `CONSERVATIVE_UNKNOWN`.
- An all-zero FaultScenario contains the WorkloadPlan forward schedule and no compensation actions. A participant commits automatically as part of completing its final successful forward action.
- Exactly one eager all-zero FaultScenario is written for each executor-materializable WorkloadPlan.
- Every single-point vector for an executor-materializable WorkloadPlan is considered eagerly. The faulting action occurs before the target step body; that target step does not complete and does not add its own compensation checkpoint.
- Remaining forward actions belonging to a participant after its assigned fault are omitted from the executable action schedule and remain diagnosable as skipped/masked workload slots.
- A failed participant's eligible completed checkpoints are ordered in reverse completed-forward order.
- Recovery generation inserts reverse compensation queues into the complete residual WorkloadPlan forward sequence. The relative order of every remaining forward action is preserved globally across participants, and each failed participant's compensation order remains reversed; recovery generation never creates a new survivor-to-survivor forward interleaving.
- A participant's final successful forward action immediately closes/commits that participant. Its placement relative to compensation therefore determines whether that participant commits before, between, or after another participant's recovery actions.
- Multi-fault generation preserves assigned bits across participants. Later assigned slots in an already-failed participant are masked; assigned slots in still-live participants may realize and add additional reverse compensation queues.
- Equivalent generated action schedules deduplicate by deterministic FaultScenario identity.

### Caps and representative selection

- The recovery-schedule cap is a positive configurable integer with default `20`. Missing configuration uses the default; non-positive or malformed values fail configuration validation rather than meaning unlimited or no output.
- The cap applies separately to each vector and consistently to eager and on-demand generation.
- If the uncapped valid set is at or below the cap, every unique schedule is materialized.
- If it exceeds the cap, representative construction uses one total canonical action ordering: compensation queues are ordered by participant id and then source checkpoint occurrence id; residual forward actions retain their WorkloadPlan order.
- Representative candidates are constructed in this priority order:
  1. **Earliest recovery:** whenever compensation and forward progress are both enabled, choose compensation; ties between compensation queues use canonical order.
  2. **Latest recovery:** choose residual forward progress whenever available and drain compensation queues only afterward.
  3. **Alternating, recovery first:** alternate one canonical compensation action and one residual forward action while both classes remain available, starting with compensation and draining the remaining class canonically.
  4. **Alternating, forward first:** the same rule starting with residual forward progress.
  5. **Final-step boundary representatives:** consider only a still-live participant's reachable final forward action whose assigned bit is zero and which therefore reaches a successful automatic-commit boundary. For each such boundary in residual WorkloadPlan order, the before variant preserves residual forward actions up to the boundary, drains all then-enabled compensation queues canonically, executes the final action/commit, and applies earliest-recovery choice to the suffix. The after variant preserves residual forward actions through the final action/commit, drains all then-enabled queues canonically, and applies earliest-recovery choice to the suffix. Compensation queues enabled by later suffix faults are handled by that suffix rule.
- Invalid or duplicate representatives are discarded before consuming capacity. If the cap is smaller than the distinct representative set, the priority/final-step order above determines the retained prefix.
- Remaining capacity is filled from complete valid schedules sorted lexicographically by ordered action identity. No hidden or seeded random sampling is used for recovery schedules.
- Generation reports both uncapped unique schedule count and written count for every materialized vector.

### On-demand arbitrary vectors

- An on-demand request identifies exactly one WorkloadPlan and one binary vector.
- The vector must match the WorkloadPlan fault-space length and mapping. WorkloadPlan materializability requires every participant input to pass current input-readiness semantics and the plan to pass generation-time structural admissibility checks for participant/input references, action ownership, step identities, runtime step-name mapping, fault-space shape, and supported Saga/local execution shape. Because current Saga/local execution and recovery state is keyed by runtime step name rather than occurrence id, each participant's forward `runtimeStepName` values must be unique; the same runtime name may appear in different participants. Scheduled-step occurrence ids remain semantic catalog/action identities. Runtime materialization and startup remain separate execution gates rather than facts proven by generation.
- Missing WorkloadPlan ids, non-binary vectors, wrong-length vectors, malformed slot mappings, and non-materializable plans are rejected with diagnostics before catalog mutation or execution.
- A catalog package freezes its effective recovery cap in the manifest at creation. On-demand generation uses that same cap; a request with a different effective cap is rejected before mutation and requires a separate package/run.
- A valid request generates, caps, and persists its deterministic FaultScenarios before any one of them can execute.
- Repeating the same request with the same WorkloadPlan, vector, compensation evidence, and frozen cap does not duplicate existing FaultScenario records.
- Execution still selects one FaultScenario id per attempt; this feature does not introduce batch execution of every generated variant.

### Replay and lifecycle

- The executor accepts a persisted FaultScenario id as the execution contract. Direct execution-time vector overlays are unsupported in v3.
- Before materialization or measured execution, catalog validation rejects dangling or misowned WorkloadPlan slots/checkpoints, repeated `(sagaInstanceId, runtimeStepName)` forward occurrences, duplicate action identities, actions referencing absent forward steps, compensation actions before their enabling failure/completion, participant-order violations, and residual forward-order violations. Supporting repeated same-participant runtime step names requires a future occurrence-aware runtime-state change; this feature does not claim that support.
- All participants materialize and start successfully before measured forward actions begin. Existing all-or-nothing preparation gates remain.
- The executor follows the selected FaultScenario action order exactly unless a zero-bit simulator/application failure carrying the explicit domain-failure marker causes the defined fallback.
- A forward action executes through the existing simulator fault boundary using its WorkloadPlan slot and assigned bit.
- On an assigned fault, the failed participant aborts, its remaining forward actions are skipped, and subsequent actions follow the persisted compensation-aware schedule.
- A compensation action advances recovery through the simulator's stepwise compensation capability for its source step/checkpoint.
- Each planned compensation action is invoked once. Compensation actions are not fault slots.
- After abort, a participant with no eligible compensation checkpoints transitions immediately to `COMPENSATED` and emits an explicit `NO_COMPENSATION_WORK` lifecycle event with zero compensation-action outcomes.
- After the final successful compensation action for a participant, that participant becomes `COMPENSATED` and is not committed or compensated again.
- After the final forward body succeeds, participant close/commit runs as the atomic completion phase of that same forward action. The participant becomes `COMMITTED` only if close/commit succeeds; reporting distinguishes body completion from commit outcome.
- An explicitly domain-classified simulator/application failure during close/commit is `UNASSIGNED_RUNTIME` at the final forward action: the participant does not commit, immediate fallback compensation runs, and still-valid actions for other participants continue. Plain or unmarked `SimulatorException`, unknown, reflection, provider, configuration, or invocation-shape failures during closure are executor/infrastructure hard stops.
- An assigned fault in a participant's final forward action prevents its body and commit from running.
- A zero-bit simulator/application failure is recorded with `failureOrigin = UNASSIGNED_RUNTIME` only when it carries the explicit domain-failure marker. The participant enters one immediate fallback recovery episode through the simulator's normal compensation path, its remaining forward actions are skipped, and still-valid planned actions for other participants continue. "Once" means one recovery episode, not one opaque compensation action. Unknown or unmarked failures hard-stop instead.
- Fallback action reporting follows runtime unit-of-work truth at checkpoint granularity. Persisted compensation checkpoints remain limited to steps completed before an assigned pre-body fault. A zero-bit failure can occur after the runtime marks the failing step executed, so fallback may emit a runtime-only recovery-action reference for that partially failed step with participant, runtime step/occurrence, and nullable planned-checkpoint id.
- One runtime recovery action represents recovery of one source step/checkpoint. Its ordered sub-outcomes distinguish explicit registered compensation from implicit Saga-state rollback when both occur; explicit compensation appears only if runtime registration completed before failure.
- A zero-bit fallback marks schedule conformance as deviated and records the deviation action plus each actual fallback checkpoint/action. It does not rewrite the persisted FaultScenario.
- Selection, catalog-shape, vector, materialization, startup, reflection, provider, configuration, and report-writing failures are executor/infrastructure hard stops, not domain outcomes.
- If any scheduled or fallback recovery action/sub-outcome throws, terminal status is `COMPENSATION_FAILED`; the measured attempt hard-stops, remaining planned/fallback actions do not execute, the partial trace reports `scheduleConformance = INCOMPLETE`, and no automatic retry occurs.
- A thrown compensation remains retryable by a later explicit simulator invocation because it is not marked executed, but such retry is outside this execution attempt and feature.
- Attempts with executor/infrastructure or compensation failure are ineligible for future impact scoring. Zero-bit domain deviations remain eligible domain outcomes, subject to future scorer policy.

### Reporting

- The action-aware execution report uses a new schema version rather than silently changing the existing participant report contract.
- Schedule conformance uses `EXACT` only when the complete persisted action schedule finishes in order, `DEVIATED` when the defined unassigned-runtime fallback changes planned actions, and `INCOMPLETE` when a selected scenario executes only an exact/deviated prefix before compensation or infrastructure hard-stop. Conformance is absent/not applicable when no FaultScenario was selected or no measured action began.
- The report retains one participant entry per WorkloadPlan Saga instance and scenario-level identity for the selected WorkloadPlan and FaultScenario.
- The report records:
  - assigned vector and slot mapping;
  - planned action order;
  - actual action order;
  - action kind, participant, source workload step/checkpoint, and planned/actual position;
  - forward, compensation, skipped, faulted, committed, compensated, and hard-stop outcomes;
  - compensation evidence class;
  - fault origin (`ASSIGNED` or `UNASSIGNED_RUNTIME` where applicable);
  - schedule conformance (`EXACT`, `DEVIATED`, or `INCOMPLETE`) when measured execution began, absence/not-applicable when it did not, plus deviation/hard-stop action and fallback policy where applicable;
  - explicit abort, automatic commit, and compensated lifecycle events;
  - realized, masked, not-reached, and not-assigned fault-slot states;
  - exception type/message and structured blockers;
  - final participant lifecycle and top-level terminal status.
- Exact assigned-fault replay with no runtime deviation and complete schedule reports `scheduleConformance = EXACT`.
- Zero-bit fallback that completes its resulting measured actions reports `scheduleConformance = DEVIATED` even when all fallback compensation succeeds and surviving participants commit.
- Compensation or infrastructure hard-stop after measured execution begins reports `scheduleConformance = INCOMPLETE` and retains the exact/deviated prefix plus the hard-stop action/reason.
- Top-level success vocabulary continues to distinguish clean committed success, all-compensated outcomes, and mixed committed/compensated outcomes. Compensation exceptions and infrastructure failures remain non-success hard stops.

## Data and Lifecycle Rules

- Source of truth for normal structure: the v3 WorkloadPlan artifact produced from verifier static analysis and accepted inputs.
- Source of truth for one experiment: the persisted v3 FaultScenario record; CLI/vector request parameters are generation input, not execution identity.
- Canonical v3 package artifacts:
  - `workload-catalog.jsonl`;
  - `fault-scenario-catalog.jsonl`;
  - `scenario-catalog-manifest.json` linking both record sets and their schema/configuration/count metadata;
  - `scenario-space-accounting.json` with layered workload/vector/recovery counts.
- New v3 runs do not write `scenario-catalog.jsonl` v2 records and v3 readers do not accept them.
- WorkloadPlan records are immutable after generation. Dynamic enrichment remains sidecar evidence and does not modify them.
- Eager and on-demand FaultScenario generation may add missing deterministic records to the fault-scenario artifact for that run. On-demand writers for one real package directory are serialized across cooperating local JVM processes by a stable package-local OS lock acquired before package validation and held through publication and final validation. A successful return means the resulting FaultScenario JSONL, manifest, and accounting revision was validated; invalid requests and caught generation/publication failures preserve or restore the prior semantic bytes. The three-file promotion is not hard-crash atomic: abrupt process/JVM, kernel, host, or power failure can leave a checksum-invalid package, there is no automatic recovery, and the local lock does not establish network-filesystem or multi-host distributed coordination.
- An existing deterministic id with byte-equivalent semantic content deduplicates. The same id with different semantic content is an integrity error and leaves the package unchanged.
- The manifest records effective workload bounds, materializability policy, recovery cap, eager/on-demand vector source, uncapped counts, written counts, warnings, and artifact paths.
- Accounting records exact `n` and `2^n` per WorkloadPlan, exact uncapped/written counts per eager or requested vector, and clearly labeled aggregate sums only where exact per-record counts are summed. Exact counts use non-overflow representation rather than bounded integer truncation.
- WorkloadPlan ids change when ordered participants, inputs, global forward schedule, conflict evidence, fault slots, compensation checkpoint set/order, or primary compensation evidence class changes. Diagnostic-only warnings do not affect identity.
- Forward/compensation action ids change when action kind, owning participant, source slot/checkpoint, or occurrence identity changes. FaultScenario ids remain stable only across reruns with the same WorkloadPlan, vector, and ordered action identities. Configuration that changes the retained action schedule changes the FaultScenario set, not existing record meaning.
- Execution never mutates workload, fault-scenario, enrichment, or accounting artifacts. It writes a separate action-aware execution report.
- Execution-attempt identity remains distinct from FaultScenario identity so repeated attempts of one scenario are auditable.
- Artifact retention remains the caller/run-directory responsibility.
- Catalogs and reports contain source/runtime identifiers and command/value evidence already within verifier artifacts; this feature introduces no new user identity, authorization, or external data-sharing surface.

## Existing System Fit

- Preserve the verifier pipeline boundary: `visitor/*` -> `ApplicationAnalysisState` -> `scenario/adapter/*` -> scenario generation/export -> `dynamic/*` -> executor.
- Static extraction already records forward versus compensation dispatch phase; v3 adaptation must preserve rather than discard that evidence.
- WorkloadPlan generation reuses deterministic participant/input/forward-schedule/conflict semantics and current segment-compressed scheduling where configured.
- FaultScenario recovery generation is a separate stage from WorkloadPlan forward-schedule generation; it must not redefine static conflict evidence or dynamic-enrichment ownership.
- Existing input-level `ScenarioExecutorReadinessEvaluator` semantics contribute participant readiness; WorkloadPlan eligibility additionally requires generation-time structural admissibility checks equivalent to the executor's participant/input, schedule ownership/id, runtime-step-name, fault-space, and supported-shape validation.
- Existing simulator `compensateUntilStep`/Saga unit-of-work state supports stepwise compensation. Existing execution state leaves failed compensation actions eligible for later explicit retry but performs no automatic retry.
- Existing simulator in-memory fault-vector boundaries remain the injection mechanism, but assignments come from the persisted FaultScenario rather than an execution-time vector overlay.
- Dynamic enrichment joins to WorkloadPlan/input identity. FaultScenarios reference that stable workload identity rather than receiving rewritten static structure.
- Current state/reference docs continue to describe implemented v2 behavior until the feature is validated; implementation completion must then align current-state, glossary status, roadmap, evidence, advisor brief, executor reference, and thesis claims.

## Research Notes

No external library research was required. The repository already contains deterministic forward-schedule enumeration, fault-vector injection, stepwise Saga compensation, participant-oriented reports, and materializability evaluation.

Existing Quizzes evidence bounds the first baseline:

```text
segment-compressed selected WorkloadPlan-like total: 1,019,393 (count-only)
bounded multi-Saga audit records: 500
materializable multi-Saga audit records: 8
example materializable fault-space length: 5
```

The current generator does not enumerate vectors. For the five-slot example, the eager vector baseline grows from one default vector to six all-zero/single-point vectors before recovery variants. At cap `20`, if all eight materializable plans had the demonstrated example's five slots, the illustrative result would be `8 × (1 + 5 × 20) = 808` written FaultScenarios before validity checks and deduplication. The eight plans' complete slot-length distribution has not been measured here, so `808` is not an upper bound. These are planning illustrations, not acceptance thresholds.

## Acceptance Criteria

### V3 catalog contract

- [ ] AC-1: A v3 generation run writes separate WorkloadPlan and FaultScenario JSONL artifacts, one linking manifest, and layered accounting without writing a v2 `scenario-catalog.jsonl` artifact.
- [ ] AC-2: V3 readers reject v2 catalog records with a clear unsupported-schema diagnostic; no v2 read, write, upgrade, or replay compatibility is required.
- [ ] AC-3: Every WorkloadPlan deterministically records participants, accepted inputs, one global forward schedule, conflict evidence, ordered fault slots, and ordered compensation checkpoints/evidence; all semantic fields participate in stable identity while diagnostic-only warnings do not.
- [ ] AC-4: Every FaultScenario references exactly one WorkloadPlan, contains one valid assigned vector and one ordered forward/compensation action schedule, and has a stable id derived from WorkloadPlan id, vector, and action identities that include kind, owner, source slot/checkpoint, and occurrence identity.
- [ ] AC-5: Repeated generation with identical source facts and effective configuration produces byte-stable record ordering and identical WorkloadPlan/FaultScenario ids.
- [ ] AC-6: The package manifest identifies both catalogs, effective generation/materializability/cap settings, source mode/count diagnostics, and written/uncapped counts.

### Compensation evidence and generation

- [ ] AC-7: Static forward and compensation dispatch phases remain distinguishable in the v3 workload model instead of being merged into one undifferentiated footprint list.
- [ ] AC-8: Checkpoint classification applies precedence `EXPLICIT_COMPENSATION` > `IMPLICIT_SAGA_ROLLBACK` > `CONSERVATIVE_UNKNOWN`; explicit registration remains retained without recognized body dispatches, and omission requires resolved read-only evidence, no explicit registration, no rollback/write potential, and no unresolved diagnostic.
- [ ] AC-9: The all-zero vector produces exactly one eager FaultScenario per executor-materializable WorkloadPlan, contains no compensation actions, and commits each participant automatically with its final successful forward action.
- [ ] AC-10: Every valid single-point vector is considered for each executor-materializable WorkloadPlan; a faulting step does not complete or contribute its own compensation checkpoint.
- [ ] AC-11: Assigned failure removes that participant's remaining forward actions from the executable action schedule while preserving them as skipped/masked workload-slot diagnostics.
- [ ] AC-12: A failed participant's compensation actions preserve reverse eligible completed-step order.
- [ ] AC-13: Recovery schedules preserve the complete residual WorkloadPlan forward sequence globally across participants and only insert valid reverse compensation actions; they never re-interleave survivor forward actions.
- [ ] AC-14: A live participant commits automatically when its final successful forward action completes, allowing its final action to appear before, between, or after another participant's compensations without an independent commit action.
- [ ] AC-15: Multi-fault generation masks later assigned slots in already-failed participants while allowing assigned slots in still-live participants to realize and create additional compensation queues.

### Caps, on-demand vectors, and accounting

- [ ] AC-16: Recovery-schedule cap configuration defaults to `20`, accepts positive integers, and rejects zero, negative, or malformed values before artifact mutation.
- [ ] AC-17: When uncapped schedules do not exceed the configured cap, every unique valid schedule is written.
- [ ] AC-18: When schedules exceed the cap, deterministic constructors and total tie-breaking produce earliest, latest, recovery-first alternating, forward-first alternating, and before/after successful automatic-commit-boundary representatives in the specified priority order; boundary suffixes use earliest-recovery handling for later-enabled queues, duplicates are removed, and remaining capacity uses lexicographic action-identity order without randomness.
- [ ] AC-19: The configured positive cap applies independently per vector, is frozen in the package manifest, and is used consistently by eager and on-demand generation; mismatched-cap requests fail before package mutation.
- [ ] AC-20: An explicit on-demand request for one WorkloadPlan whose participant inputs are ready and whose plan shape is structurally admissible, including unique forward `runtimeStepName` values within each participant, plus one valid arbitrary binary vector, persists bounded deterministic FaultScenarios before execution and supports multiple assigned bits; the same runtime name across different participants remains admissible, occurrence ids remain semantic identities, and runtime materialization/startup remain execution gates.
- [ ] AC-21: Invalid/missing WorkloadPlan ids, non-binary or wrong-length vectors, malformed slot mappings, repeated `(sagaInstanceId, runtimeStepName)` forward occurrences, other structurally inadmissible plans, and non-ready participant inputs fail with structured diagnostics before catalog mutation or execution, leaving FaultScenario JSONL, manifest, and accounting byte-unchanged.
- [ ] AC-22: Repeating an on-demand request with the same semantic inputs deduplicates byte-equivalent existing FaultScenarios rather than appending duplicate ids; concurrent writers for one real package directory are serialized across cooperating local JVM processes from pre-read validation through validated publication, so successful requests for different vectors accumulate without lost updates; an existing id with different semantic content is an integrity error and leaves the package unchanged. Hard-crash atomicity, automatic crash recovery, network-filesystem locking, and multi-host distributed coordination are excluded.
- [ ] AC-23: Accounting reports exact `n` and mathematical `2^n` per WorkloadPlan, exact eager vector count, exact uncapped/written counts per all-zero, single-point, or requested multi-fault vector, and clearly labeled aggregate sums where exact; counts use non-overflow exact-number serialization.
- [ ] AC-24: Accounting does not label any aggregate recovery-schedule total across every `2^n` vector as exact unless that total has actually been computed by a future supported method.

### Replay and failure behavior

- [ ] AC-25: V3 execution selects one persisted FaultScenario id, rejects direct unpersisted execution-time vector overlays, and validates per-participant runtime-step-name uniqueness, action references, ownership, uniqueness, enabling conditions, reverse compensation order, and residual global forward order before materialization or measured execution; repeated same-participant runtime names remain unsupported until runtime state is occurrence-aware.
- [ ] AC-26: All participants pass materialization/startup gates before any measured FaultScenario action executes; preparation failure hard-stops with no measured forward or compensation action.
- [ ] AC-27: In the absence of runtime deviation, actual forward and compensation action order exactly matches the selected FaultScenario.
- [ ] AC-28: Assigned faults inject at the matching persisted forward slot before the target body, abort that participant, and follow the persisted compensation-aware continuation schedule.
- [ ] AC-29: Each planned compensation action advances the owning participant's stepwise compensation once and compensation actions never receive fault slots.
- [ ] AC-30: Final successful forward bodies immediately run close/commit as the same action's completion phase; `COMMITTED` is emitted only after close succeeds, while final assigned-fault actions run neither body nor commit.
- [ ] AC-31: A zero-bit simulator/application failure carrying the explicit domain-failure marker from a forward body or close/commit is recorded as `UNASSIGNED_RUNTIME`, starts one immediate fallback recovery episode, skips that participant's remaining forward actions, and continues still-valid actions for other participants; unknown, plain, or unmarked failures hard-stop, while checkpoint-level recovery follows runtime unit-of-work truth and may emit a runtime-only source-step reference for implicit rollback of the partially failed step.
- [ ] AC-32: Zero-bit fallback does not mutate the persisted FaultScenario and reports the deviation point, fallback policy, and one runtime recovery action per source step/checkpoint with ordered explicit-compensation and implicit-rollback sub-outcomes; explicit compensation for the partially failed step appears only when runtime registration completed.
- [ ] AC-33: Executor/infrastructure failures, including reflection/provider/configuration/invocation-shape closure failures, hard-stop and are not reported as meaningful domain outcomes.
- [ ] AC-34: Any thrown scheduled or fallback recovery action/sub-outcome produces `COMPENSATION_FAILED`, hard-stops remaining measured actions, retains the partial trace with `scheduleConformance = INCOMPLETE`, and triggers no automatic retry.
- [ ] AC-35: A thrown compensation action remains simulator-retryable by later explicit invocation because it is not marked executed, but retry execution is outside this attempt and feature.
- [ ] AC-36: Participants that reach `COMMITTED` or `COMPENSATED` are not closed or compensated again; an aborted participant with no eligible checkpoints becomes `COMPENSATED` immediately with `NO_COMPENSATION_WORK` and zero compensation-action outcomes.

### Reporting, integration, and evidence

- [ ] AC-37: V3 FaultScenario execution writes a new action-aware report version with WorkloadPlan id, FaultScenario id, execution-attempt id, participants, vector/slot mapping, planned actions, actual actions, lifecycle events, final states, blockers, and terminal status.
- [ ] AC-38: Complete exact replay reports `scheduleConformance = EXACT`; completed zero-bit fallback reports `DEVIATED` with `failureOrigin = UNASSIGNED_RUNTIME`; compensation/infrastructure hard-stop after measured actions begin reports `INCOMPLETE`; conformance is absent when no measured action began.
- [ ] AC-39: Action outcomes distinguish forward and compensation actions and include participant, source step/checkpoint or runtime-only partial-step reference, planned/actual position, status, compensation evidence where applicable, and exception details; one fallback recovery action carries ordered explicit-compensation and implicit-rollback sub-outcomes at checkpoint granularity.
- [ ] AC-40: Reports distinguish realized, masked, not-reached, and unassigned fault slots and preserve participant-local skipped forward actions.
- [ ] AC-41: Reports emit explicit abort, automatic commit, compensated, and `NO_COMPENSATION_WORK` lifecycle events; final forward outcomes distinguish successful body from commit success/failure even though commit is not independently schedulable.
- [ ] AC-42: Execution does not modify WorkloadPlan, FaultScenario, manifest, accounting, or dynamic-enrichment artifacts.
- [ ] AC-43: Dynamic enrichment remains sidecar-only and joins to WorkloadPlan/input identity without creating or rewriting FaultScenarios.
- [ ] AC-44: Dummyapp-first automated coverage proves deterministic ids/order, compensation evidence tiers, all-zero/single-point generation, cap representatives, arbitrary multi-fault on-demand persistence, reverse compensation, automatic final-step commit, zero-bit fallback, compensation hard-stop, accounting, and action-aware report semantics.
- [ ] AC-45: A bounded Quizzes run records WorkloadPlan/materializable counts, eager vector counts, uncapped/written recovery counts, effective cap, and at least one executable compensation-interleaving FaultScenario with its ids, participants, vector, planned/actual actions, and report path.
- [ ] AC-46: Current/live verifier documentation is updated after validation to use WorkloadPlan/FaultScenario terminology, describe the clean v3 compatibility boundary, and distinguish implemented evidence from remaining non-goals.

## Assumptions

- The feature remains Saga/local deterministic sequential replay, not true concurrency.
- Eager eligibility combines current input-readiness semantics with WorkloadPlan structural admissibility; runtime materialization and startup remain execution-time gates.
- Existing runtime-owned argument support is sufficient for at least one bounded Quizzes smoke candidate.
- Fault injection occurs before the target forward-step body.
- Compensations are assumed to succeed in the modeled experiment; an actual thrown compensation invalidates the attempt rather than becoming an impact dimension.
- The simulator's current stepwise compensation and unit-of-work bookkeeping remain the runtime source of truth for whether a compensation invocation has observable work; that state is keyed by runtime step name, so repeated same-participant names are structurally rejected until a future occurrence-aware runtime change.
- Persistent application state reset remains caller/orchestrator responsibility.
- Default cap `20` is an initial evaluation setting and can be changed explicitly for sensitivity runs.

## Open Questions

None blocking.

Non-blocking future work:

- Exact compressed recovery counting across every vector in the full `2^n` space.
- Automatic materialization expansion as currently blocked inputs become executor-ready.
- GA-generated on-demand vectors and impact-driven scenario selection.
- Explicit compensation retry/failure experiments if the advisor later removes the eventual-success assumption.

## Handoff

Ready for implementation planning: yes, subject to a passing follow-up spec review.

Rerun `sp-review-spec`; proceed to `sp-implementation-plan` only if the revised spec passes.
