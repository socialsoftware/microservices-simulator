# Decision Frame: Multi-Saga Scenario Executor Interleavings

## Original Request

> I want to expand my scenario executor coverage to support multi-saga scenarios with interleavings... How can we approach this?

## Repo Context

- `docs/verifiers-impl/current-state.md` identifies the ScenarioExecutor as a narrow single-saga saga/local path; multi-saga runtime execution is explicitly not implemented.
- `ScenarioPlan` already supports multiple `SagaInstance` records, input variants, an `expandedSchedule`, and a `FaultSpace` whose scheduled-step ids can span the scenario.
- Static scenario generation already supports bounded multi-saga candidates and deterministic schedule strategies: `SERIAL`, `ORDER_PRESERVING_INTERLEAVING`, and `SEGMENT_COMPRESSED`.
- `ScenarioExecutor` currently validates only `ScenarioKind.SINGLE_SAGA` with exactly one saga instance, materializes one input, instantiates one functionality, and writes a single-saga-shaped report.
- The simulator-side in-memory fault-vector provider already uses scenario execution id, scenario plan id, saga instance id, scheduled step id, and runtime step name, which is compatible with future multi-saga identity.
- The latest documented Quizzes executor smoke used a single-saga catalog (`max-saga-set-size=1`), so multi-saga executor validation will need a catalog generated with `max-saga-set-size >= 2`.

## Problem Framing

The verifier author needs the ScenarioExecutor to move beyond single-saga replay and execute generated multi-saga scenario plans with catalog-defined interleavings. The next useful outcome is proving that a materializable multi-saga `ScenarioPlan` can be replayed deterministically from JSONL without conflating this work with fixture generation, true parallelism, impact scoring, or search.

## Selected Direction

Extend the ScenarioExecutor to support one explicit materializable multi-saga saga/local scenario by treating `ScenarioPlan.expandedSchedule` as a deterministic sequential interleaving. The executor will create one runtime saga session per `SagaInstance`, materialize each saga's input, instantiate each functionality, create one unit of work per saga instance, execute scheduled steps in ascending `scheduleOrder`, route binary fault vectors through the existing simulator in-memory provider, and report per-saga outcomes.

This is interleaved sequential execution, not true parallel execution.

## Alternatives Considered

| Option | Status | Summary | Reason |
|--------|--------|---------|--------|
| Deterministic sequential interleaving runner | selected | Execute one explicit multi-saga catalog plan by stepping each owning saga session according to `expandedSchedule`. | Best fit for the current catalog contract, deterministic schedule strategies, and existing fault-vector identity model. |
| Existing-test or generated-harness runner | rejected | Map catalog plans onto existing Quizzes-style tests or generated test harnesses that prepare fixtures and drive interleavings. | Faster for demos, but too coupled to application tests and weaker as a generic verifier-owned executor contract. |
| Full runtime scenario engine | deferred | Add environment reset, fixture providers, aggregate binding, multi-saga lifecycle policy, telemetry, batch execution, scoring, and search together. | Long-term direction, but too broad for the next slice and likely to stall on fixture/setup and impact concerns before proving interleaving replay. |

## Key Tradeoffs

| Tradeoff | Chosen Side | Consequence |
|----------|-------------|-------------|
| Deterministic replay vs true concurrency | Deterministic sequential interleaving | Aligns with `expandedSchedule` and is reproducible; does not claim thread-level or distributed concurrency. |
| Generic executor contract vs fast Quizzes-specific demo | Generic executor contract | Keeps the executor application-agnostic; Quizzes smoke may be blocked until a materializable multi-saga candidate exists. |
| Small executable slice vs full scenario engine | Small executable slice | Produces a useful runtime milestone sooner; fixture setup, telemetry, and search remain future work. |
| Stop-on-first-fault vs continuing after faults | Compensate-and-continue for assigned faults | Gives future impact scoring a meaningful observation boundary while keeping failed-saga handling deterministic. |
| Single-saga report compatibility vs accurate multi-saga evidence | v3 participants report model | Breaks v2 report expectations, but gives single- and multi-saga executions one clean canonical artifact shape. |

## Assumptions

- Multi-saga execution will initially require an explicit `--scenario-id`; auto-select can remain single-saga or be extended later after supported-candidate ranking is clear.
- The first executable scope is saga/local only.
- Every supported saga instance must have exactly one matching materializable input variant under current materializer semantics.
- The catalog's `expandedSchedule` is the execution contract; the executor should not synthesize new interleavings at runtime.
- Fault vectors remain strictly binary and aligned to `ScenarioPlan.faultSpace`.
- Static scenario identity remains independent from the assigned fault vector.
- Current fixture/database state limitations remain visible blockers, not values guessed by the executor.
- Surviving saga participants close in ascending order of their last executed scheduled step; zero-step participants close afterward in deterministic saga-instance order.
- Failed-and-compensated participants are terminal: skip their remaining scheduled forward steps and exclude them from final `resumeWorkflow` closure.
- Multi-saga top-level terminal status distinguishes clean success, all-compensated failure, mixed outcomes, and compensation failure: `SUCCESS` only when all participants complete without scheduled-step failures; `COMPENSATED` when every executed/started participant that reaches a terminal outcome is compensated and no participant commits; `PARTIAL_COMPENSATED` when at least one participant compensates and at least one surviving participant commits; `COMPENSATION_FAILED` when any participant compensation fails.
- Scenario-wide explicit fault vectors may realize multiple assigned faults across different still-alive saga participants. Later assigned slots in a failed participant are `MASKED_BY_SAGA_FAILURE`; assigned slots in surviving participants remain active until reached or until an executor/infrastructure hard stop.
- Participant compensation failure does not hard-stop surviving participants. The failed participant remains terminal with `COMPENSATION_FAILED`, surviving participants continue, and the top-level terminal status is `COMPENSATION_FAILED`.

## Non-Goals

- True parallel/threaded execution.
- Generic fixture/database seeding or reset.
- Event-origin payload reconstruction.
- TCC execution.
- stream/gRPC/distributed runtime parity.
- Compensation-step faults.
- Delay or non-binary impairments.
- Batch execution, impact scoring, genetic/local search, or scenario prioritization.
- Quizzes-specific executor shortcuts.
- Rewriting static catalog records or dynamic-enrichment sidecars from executor output.

## Open Questions for Spec

| ID | Question | Why It Matters | Risk | Recommended Default |
|----|----------|----------------|------|---------------------|
| Q1 | What is the lifecycle policy when one saga instance faults or fails during a multi-saga scenario? | This determines whether other started sagas are committed, compensated, aborted, skipped, or left open, and affects report meaning and future impact scoring. | high | Resolved: scheduled-step runtime failures use compensate-and-continue. Compensate the failed saga immediately when possible, skip its remaining scheduled forward steps, continue other saga instances in catalog schedule order, and close surviving sagas at the end. Executor/infrastructure failures remain hard stops. |
| Q2 | How should multi-saga execution reports evolve while preserving existing single-saga consumers? | The current report has top-level single `sagaInstanceId`, `sagaFqn`, and `inputVariantId`; multi-saga runs need per-saga materialization, step, lifecycle, and blocker evidence. | high | Resolved: introduce a v3 participants report model for both single- and multi-saga runs, with no v2 compatibility requirement. Scenario-level facts stay top-level; saga-specific facts move under required participant entries. |
| Q3 | Should explicit multi-saga execution be the only initial selection mode? | Auto-selection across multi-saga candidates depends on materializability, join status, schedule strategy, and fixture readiness, and could hide why a run chose a scenario. | medium | Resolved: require explicit `--scenario-id` for multi-saga execution in this feature; leave multi-saga auto-select for a later prioritization/selection slice. |
| Q4 | How should assigned fault bits after a failed saga be classified across other saga instances? | Existing masking is single-saga-oriented; multi-saga scenarios may contain later assigned bits in different saga instances. | high | Resolved: assigned slots in the failed saga after its failure point are `MASKED_BY_SAGA_FAILURE`; assigned slots in surviving saga instances remain reachable under compensate-and-continue; `NOT_REACHED` is reserved for assigned slots skipped because the whole execution ends early due to executor/infrastructure failure. |
| Q5 | What validation smoke should prove real application usefulness after dummy/synthetic coverage? | The current documented Quizzes executor catalog is single-saga only, and many accepted inputs remain non-materializable. | medium | Resolved by planning audit: generate/use a bounded Quizzes `WRITE_PLANS`, multi-saga-only catalog and run a smoke against one currently materializable candidate. Audit evidence found 8 materializable multi-saga plans in `verifiers/target/multi-saga-executor-planning-audit/quizzes-20260709-004627-310/scenario-catalog.jsonl`; see `quizzes-materializability-audit.md`. |
| Q6 | Should successful multi-saga runs close all saga instances at the end or only those that appeared in the schedule? | The answer affects commit behavior and final state when schedules are segment-compressed or contain tails. | medium | Resolved: close every instantiated saga session after all scheduled forward steps complete; report sessions with no executed steps distinctly if such plans are possible. |

## Notes

The selected direction intentionally builds on the current static schedule semantics. It should be described as deterministic interleaving replay, not as concurrency simulation or distributed runtime parity.

Spec-phase update: scheduled-step runtime failures should use the compensate-and-continue policy documented in `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`. This includes assigned binary fault-vector faults and non-assigned domain/simulator exceptions thrown by scheduled saga steps. Stop-first and deferred-compensation continuation remain documented alternatives for advisor review.

Spec-phase update: execution reports should move to the v3 participants model documented in `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`. No v2 compatibility bridge is required.

## Handoff

**Highest-risk open question:** Q1 — multi-saga fault/failure lifecycle policy  
**Recommended next skill:** `sp-spec-with-docs`  
**Next action:** Resolve the lifecycle/report semantics before implementation planning.
