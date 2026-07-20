# Decision Frame: Compensation-Aware Scenario Catalog

## Original Request

> fun! So lets use sp-brainstorm, How can we Specify the catalog model (what approaches and what do you mean by that) how do we implement compensation-aware generation and replay?

## Workspace

- Mode: `linked worktree`
- Path: `/home/andre/microservices-simulator-worktree-fault-analysis-compensation-aware-catalog`
- Branch: `fault-analysis/compensation-aware-catalog`

## Repo Context

- The current `ScenarioPlan` models Saga participants, accepted inputs, one forward `expandedSchedule`, conflict evidence, and a `FaultSpace`; the assigned vector is execution-time input and is excluded from scenario identity.
- The current multi-Saga executor replays forward steps sequentially and immediately calls `resumeCompensation` when one participant fails.
- The simulator already supports stepwise recovery through `compensateUntilStep(stepName, unitOfWork)` and tracks executed/aborted steps in `SagaUnitOfWork`.
- Static extraction distinguishes forward and compensation dispatch phases, but the scenario adapter currently discards that phase when building `StepFootprint` records.
- The advisor direction recorded in the W29 notes is to make fault vectors and compensation interleavings part of scenario semantics and to define that model before changing generation or execution.

## Problem Framing

The thesis author needs a reproducible catalog contract in which a generated experiment includes not only a normal Saga workload but also its selected failures and recovery ordering. The model must support compensation interleavings that expose intermediate states without eagerly duplicating the complete workload for every fault-vector and recovery-schedule combination, because later brute-force baselines and GA search will generate many concrete experiments.

## Selected Direction

Use a two-level logical catalog:

1. A **WorkloadPlan** describes Saga participants, inputs, the normal forward interleaving, conflict evidence, and available forward fault slots.
2. A **FaultScenario** references one WorkloadPlan and adds one assigned fault vector plus one concrete compensation-aware action schedule.

`FaultScenario` is the reproducible experiment identity. Its deterministic ID includes the workload-plan ID, assigned vector, and ordered forward/compensation action identities. Concrete fault scenarios may be materialized eagerly for small baselines, bounded deterministically for larger spaces, or produced on demand by later search while still being persisted for replay.

Assigned faults follow the concrete catalog recovery schedule. If a zero-bit forward action throws a domain or simulator failure, the executor applies a deterministic `IMMEDIATE_COMPENSATE_AND_CONTINUE` fallback: abort and immediately compensate that participant, skip its remaining forward actions, then resume still-valid planned actions for other participants. The report preserves planned and actual action sequences and identifies the deviation point. Executor/infrastructure failures still hard-stop.

Compensation scheduling uses conservative evidence tiers. A completed forward step contributes a compensation checkpoint when static analysis finds explicit compensation, a potential implicit Saga rollback/write effect, or unresolved evidence that cannot safely prove the step effect-free. Only confidently read-only steps with no explicit compensation or Saga-state rollback potential are omitted.

The selected catalog contract is a clean v3 package with separate workload and fault-scenario JSONL artifacts linked by one manifest. New runs and pipeline components use v3 only; v2 output and read compatibility are intentionally removed rather than dual-written or carried forward.

The first eager FaultScenario baseline is deliberately executable and bounded: expand only executor-materializable WorkloadPlans into one all-zero case plus every single-point fault vector, with at most 20 deterministic recovery schedules per fault vector. Preserve full uncapped counts in accounting and leave multi-fault vectors plus broader WorkloadPlans available for later on-demand materialization rather than removing them from the semantic space.

Participant commit is not independently schedulable. A participant closes and commits automatically when its final successful forward step completes; abort remains an immediate transition caused by a failed forward action, and compensated remains a derived transition after the participant's final compensation action. A surviving Saga therefore concludes before, between, or after another Saga's compensations according to where its final forward step appears.

Arbitrary multi-fault vectors remain supported through explicit on-demand FaultScenario generation for one materializable WorkloadPlan. The requested vector is never executed as an unpersisted overlay: bounded deterministic compensation-aware variants are written first and execution selects one persisted FaultScenario by identity.

Accounting uses layered exact claims. It reports WorkloadPlan counts, mathematical `2^n` vector-space size, exact eager all-zero/single-point recovery counts before caps, written counts after caps, and exact counts for each explicitly requested multi-fault vector. It does not claim an exact aggregate recovery-schedule total across every vector in the `2^n` space.

Recovery-schedule materialization uses a configurable positive cap with default `20`, applied consistently to eager and on-demand vectors. Selection is coverage-first and deterministic: earliest recovery, latest recovery, alternating recovery, and compensation immediately before/after survivor final-step/automatic-commit boundaries are prioritized and deduplicated, then remaining capacity is filled in canonical order without random sampling.

## Alternatives Considered

| Option | Status | Summary | Reason |
|--------|--------|---------|--------|
| Flat concrete catalog | rejected | Repeat participants, inputs, vector, and full action schedule in every catalog line. | Direct replay is simple, but workload data is duplicated across a scenario space that multiplies schedules, vectors, and recovery interleavings. |
| WorkloadPlan + FaultScenario | selected | Keep normal workload structure separate from concrete failure/recovery experiments linked by deterministic ID. | Preserves exact replay while supporting compact accounting, bounded baseline generation, and later on-demand GA candidates. |
| Guarded action graph | deferred | Represent all forward and recovery possibilities as guarded actions and precedence edges. | Expressive and compact, especially for contingent runtime failures, but substantially increases generator, validator, executor, and thesis complexity before impact evaluation exists. |

## Key Tradeoffs

| Tradeoff | Chosen Side | Consequence |
|----------|-------------|-------------|
| Direct flat simplicity vs scalable reuse | Two linked semantic levels | Readers and executors resolve one reference, but workload data is not repeated for every experiment. |
| Eager completeness vs bounded/on-demand materialization | Bounded and on demand | The full space can be counted without requiring every FaultScenario to be written. |
| Flexible runtime recovery vs exact replay | Concrete schedule plus one deterministic fallback | Assigned faults replay exactly; zero-bit domain failures deviate through immediate compensate-and-continue and must be clearly marked in reports. |
| General formal model vs first evaluable pipeline | Explicit ordered actions | A guarded action/state graph remains deferred unless concrete schedules prove insufficient. |
| Clean semantics vs legacy catalog compatibility | Clean v3-only contract | Existing v2 artifacts remain historical evidence but are not accepted by the new executor pipeline. |

## Assumptions

- Compensation actions eventually succeed through retry; compensation failures are not searched as fault dimensions.
- Compensation actions are not fault slots.
- Replay remains deterministic and sequential; it models interleaving order, not true thread-level or distributed concurrency.
- A fault injected before a forward step body means that step did not complete and does not itself contribute a compensation action.
- Successfully completed steps of an aborted participant are compensated in reverse Saga order, while compensation actions may interleave with forward actions of still-live participants.
- Search-generated FaultScenarios must be persisted with stable identity before or alongside execution so results remain reproducible.
- A zero-bit domain or simulator failure is a meaningful observed outcome, not an executor failure. It triggers immediate compensation of that participant and continuation of still-valid actions for other participants.
- Executor/infrastructure failures, such as invalid catalog shape, materialization failure, reflection failure, or fault-provider mismatch, hard-stop rather than triggering domain recovery.
- Compensation candidates carry one of three evidence classes: `EXPLICIT_COMPENSATION`, `IMPLICIT_SAGA_ROLLBACK`, or `CONSERVATIVE_UNKNOWN`; only confidently effect-free steps are omitted.
- All current generator, enrichment, accounting, and executor components that consume the canonical catalog migrate together to v3.
- The eager materialization limit is an initial evaluation boundary, not a claim that multi-fault or currently non-materializable WorkloadPlans are uninteresting or outside the conceptual scenario space.
- Commit remains an explicit reported lifecycle event but is bound atomically to successful completion of the participant's final forward action; delayed commit is not a separate experiment dimension.
- On-demand multi-fault generation uses the same compensation evidence, deterministic ordering, and configurable per-vector recovery-schedule cap as eager single-point generation.
- The recovery-schedule cap must be a positive integer; invalid non-positive values are rejected rather than interpreted as unlimited or as silently disabling output.
- Simulator compensation actions are retryable because a thrown action is not marked executed, but neither the simulator nor this executor feature automatically retries them.

## Non-Goals

- Impact scoring or anomaly-detector implementation.
- GA/local search or bandit prioritization.
- Compensation-step faults, retry-count exploration, retry backoff, or permanently stuck compensation outcomes.
- True concurrent/threaded replay, distributed runtime parity, TCC execution, stream execution, or gRPC execution.
- Generic materialization of currently unsupported catalog inputs.
- Writing, reading, upgrading, or replaying legacy v2 catalog records after the v3 contract becomes canonical.

## Resolved Questions

| ID | Decision | Rationale |
|----|----------|-----------|
| Q1 | A zero-bit domain or simulator failure triggers deterministic immediate compensate-and-continue. The report records `failureOrigin=UNASSIGNED_RUNTIME`, the deviation action, the fallback policy, and both planned and actual action order. | Natural invariant failures, aggregate conflicts, and application exceptions may be the most valuable experiment outcomes. Hard-stopping them as invalid would discard downstream and final-state evidence, while dynamically generating a new interleaving would make the first model substantially more complex. |
| Q2 | Schedule compensation checkpoints for steps with `EXPLICIT_COMPENSATION`, `IMPLICIT_SAGA_ROLLBACK`, or `CONSERVATIVE_UNKNOWN` evidence; omit only steps confidently proven effect-free. | Explicit-only extraction can miss unit-of-work Saga-state restoration and unfamiliar static shapes, while including every completed read-only step would inflate the recovery schedule space with no-op permutations. |
| Q3 | Replace the v2 catalog with a clean v3-only package containing separate workload and fault-scenario JSONL artifacts plus one linking manifest; do not retain v2 read or write support. | Backward compatibility adds implementation and terminology complexity without a current requirement. Historical v2 artifacts can remain evidence of the previous implementation rather than inputs to the new semantics. |
| Q4 | Eagerly materialize FaultScenarios only for executor-materializable WorkloadPlans: one all-zero case plus every single-point fault vector, capped at 20 deterministic recovery schedules per vector; account for the full uncapped space and defer multi-fault materialization to explicit/on-demand generation. | The current generator does not enumerate vectors at all. Expanding the full Quizzes segment-compressed workload space would create millions of cases before recovery variants, while the audited bounded set had only 8 materializable plans out of 500 and supports a practical first baseline in the tens to hundreds of cases. |
| Q5 | Do not schedule commit independently. Automatically close/commit a participant when its final successful forward step completes; keep abort as the immediate outcome of failure and compensated as the transition after final compensation. | Moving the surviving participant's final forward step relative to compensation already models conclusion before, during, or after recovery. A separate movable commit action would create artificial delayed-commit schedules based on executor mechanics rather than selected Saga semantics. |
| Q6 | Preserve arbitrary multi-fault execution through explicit on-demand generation: request one vector for one materializable WorkloadPlan, persist its bounded deterministic FaultScenarios, then execute only by persisted FaultScenario identity. | This retains the current multi-fault capability and prepares for later GA candidates without eagerly enumerating `2^n` vectors or weakening the rule that vector and recovery ordering are part of scenario semantics. |
| Q7 | Use layered accounting: exact WorkloadPlan counts, exact `2^n` vector-space size, exact all-zero/single-point recovery counts before and after caps, and exact counts for explicitly requested multi-fault vectors; do not claim one exact all-vector recovery total. | This preserves defensible, reproducible thesis numbers without requiring exponential enumeration or a new compressed multi-fault recovery-counting method in the first v3 feature. |
| Q8 | Use a configurable positive recovery-schedule cap, defaulting to `20`. Under the cap, prioritize deterministic earliest, latest, alternating, and compensation immediately before/after survivor final-step/automatic-commit boundaries, deduplicate them, then fill remaining slots in canonical order without randomness. | This avoids lexicographic bias while keeping Quizzes-scale output bounded; configurability permits evaluation sensitivity checks without changing scenario semantics. |
| Q9 | Bind commit atomically to a participant's final successful forward step, including the one all-zero FaultScenario per WorkloadPlan; do not enumerate independent commit placements. | This matches the expected Saga lifecycle and preserves conclusion-before/during/after-compensation cases by moving the final forward action itself, without introducing an artificial delayed-commit dimension. |
| Q10 | Invoke each scheduled compensation action once. If it throws, mark the attempt `COMPENSATION_FAILED`, hard-stop remaining measured actions, preserve the partial trace, and do not score the attempt; do not add automatic retry. | The simulator leaves a thrown compensation action eligible for a later explicit call but has no automatic retry loop. Retrying until success would require limits, backoff, and retry accounting that are intentionally outside this feature. |
| Q11 | Recovery generation preserves the complete residual WorkloadPlan forward sequence globally and only inserts compensation actions; it never re-interleaves surviving participants' forward actions. | Re-interleaving survivors would change both the normal workload and recovery timing in one FaultScenario, producing different counts and weakening the WorkloadPlan/FaultScenario separation. |
| Q12 | Ordered compensation checkpoints and primary evidence classes participate in WorkloadPlan identity; replay actions are identified by kind, owner, source slot/checkpoint, and occurrence. | Recovery evidence changes generated semantics, so retaining an old identity would break immutability, deduplication, and auditability. |
| Q13 | Compensation evidence precedence is explicit registration, then implicit Saga rollback/write evidence, then conservative unknown; omission requires resolved read-only proof with no registration, rollback potential, or unresolved diagnostic. | A deterministic proof rule makes checkpoint classification testable and preserves explicit compensation even when its body dispatch cannot be resolved. |
| Q14 | An aborted participant with no eligible compensation checkpoint becomes `COMPENSATED` immediately and emits `NO_COMPENSATION_WORK`. | Fault-at-first-step is common and otherwise has no defined terminal lifecycle. |
| Q15 | Final-step body and close/commit are one action with separate reported phases: domain/simulator commit failure is `UNASSIGNED_RUNTIME` and triggers fallback, while invocation/infrastructure closure failure hard-stops. | Commit can fail after the body succeeds; separating phases preserves truthful action and lifecycle reporting. |
| Q16 | Recovery-cap representatives use canonical earliest/latest, recovery-first/forward-first alternating, and before/after successful final-step/commit-boundary constructors with participant/checkpoint identity tie-breaking, earliest-recovery suffix handling for later-enabled queues, then lexicographic fill. | Named schedule families alone are not reproducible when several queues or survivors are enabled. |
| Q17 | Schedule conformance is `EXACT` for complete planned replay, `DEVIATED` for completed unassigned-runtime fallback, `INCOMPLETE` for measured hard-stop prefixes, and absent before measured execution. | Binary exact/deviated vocabulary cannot truthfully classify partial traces caused by compensation or infrastructure failure. |
| Q18 | Recovery cap is frozen per catalog package; an on-demand request with a different cap is rejected and sensitivity analysis uses a separate package/run. | One manifest-level cap must accurately describe all persisted scenarios and accounting in the package. |

## Open Questions for Spec

None blocking.

## Notes

The selected terminology intentionally stops calling the normal happy-path structure a complete fault scenario. A WorkloadPlan can be reused across many concrete FaultScenarios, while one execution attempt replays exactly one FaultScenario.

## Handoff

**Highest-risk open question:** none blocking  
**Recommended next skill:** `sp-spec-with-docs`  
**Next action:** Write the specification, canonical glossary terms, and superseding recovery-policy decision.
