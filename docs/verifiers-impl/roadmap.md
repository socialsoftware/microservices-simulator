# Verifier roadmap

[`current-state.md`](current-state.md) describes what exists now and the evidence behind it. This roadmap describes the remaining outcomes, why they matter to the thesis, their dependencies, and what would count as done. It repeats only the evidence needed to explain the current order.

## End goal

The thesis direction is a reproducible pipeline that can:

1. derive meaningful Saga workloads from application source and tests;
2. persist concrete fault experiments;
3. execute those experiments repeatedly under a controlled runtime;
4. measure application-independent evidence of potential impact, with explicit observation coverage;
5. search and prioritize the experiment space under a finite budget.

The implementation supports bounded Saga/local generation and execution. ImpactV2
already measures three potential-impact conditions in ordinary execution, alongside
the separate invariant-rejection result. Representative control/fault qualification is
complete for the selected cohort. The immediate question is how to interpret and present
those observations in the thesis, not whether to start implementing the metric.

## Priority order

```text
A. Explain measured cases with the user -------------> advisor discussion
B. Current execution/control qualification ----------> completed evidence to reuse
C. Additional coverage, only for a selected question -> targeted further evidence
                                                       |
D. Measure cost and define a repeatable run budget --> bounded search evaluation
                                                       |
E. Compare search baselines, then prioritization
```

Search requires a selected set of repeatably executable workloads and an informative,
justified objective. It does not require every accepted input to become executable.
The breadth and representativeness of that selected set bound the thesis claim.

## Current next work

Start with the [Portuguese meeting note](reunioes/2026-09-08.md) for the domain and a
fully explained control/fault pair. The [latest qualification](current-state.md#owned-cycle-coverage-and-control-requalification)
owns the current results. The earlier four-run investigation remains useful methodological
evidence, but is not the latest campaign. Scoring-policy changes are held for discussion.

| Track | Priority and concrete work | Deliverable and dependency | Suggested ownership |
| --- | --- | --- | --- |
| A. Interpret impact | Immediate: review the first detailed case with the user, then expand the case sheets and advisor questions. | Explain why each object is counted, including deleted remnants and the successful positive control. Keep severity and domain harm separate. | Current task with the user. |
| B. Prepare and report experiments correctly | Completed: validated worker failure reports, corrected selected prerequisites, and successful controls for all 30 selected pairs. | Reuse the qualification and reporting handoffs. A full latest-catalogue preflight remains a different, optional task. | No new repair assignment is pending. |
| C. Broaden useful behavior coverage | The missing-input audit is complete; the current selected cohort is qualified. Choose another family only when it answers an evaluation question. | Exact reconstruction and runtime proof for that family, not a promise to make every static candidate executable. | Bounded implementation after selection. |
| D. Make experiments reproducible and affordable | Before search: measure build/start/setup/action costs and review launcher overlap. | A measured budget and reusable execution route that preserves fresh state. Existing campaign scripts are scoped evidence runners. | Independent of the advisor-note writing. |
| E. Evaluate search | Downstream: establish deterministic/random baselines using the implemented metric, then evaluate search. | Budget-matched discovery results with valid attempts and explicit coverage. Depends on A's methodological decisions and D's cost measurements. | Experiment design first, then bounded implementation. |

Thesis/paper writing runs alongside all tracks: explain the domain and experimental
question, record supported claims and limitations, and use measured results in the
method/evaluation argument. The implementation handbook is the evidence source; editing
it is not itself a manuscript update. No manuscript changes are claimed by this roadmap.

### What track B actually entails

B's two bounded deliverables are complete for the selected qualification scope. They
explain why runner reliability is distinct from deciding which effects deserve a score.

**B1 — Preserve the reason the setup worker already reported.** A worker may explain
that a required setup method is not authorized, but exit nonzero. The repaired parent
now preserves a validated, expected failure report with its actual setup reason, rather
than discarding it because of the exit code. A crashed or timed-out
worker, missing/malformed report, mismatched experiment identity, or inconsistent
success/exit status must still remain an invalid attempt. This is a reporting repair,
not inference of the root cause of arbitrary application exceptions. Implementation and
proof are in `issues/2026-09-05-preflight-failure-reporting/HANDOFF.md` and `REVIEW.md`.

**B2 — Check that selected generated experiments are prepared as described.** Choose
persisted workloads to exercise different preparation mechanisms rather than arbitrary
IDs or only the easiest cases. Examples of mechanisms are an earlier setup result nested
inside a DTO, two participants bound to the same Tournament, a source prefix that joins
a student before LeaveTournament, and a fixture supplying the exact event receiver.
Some already have bounded proof; reuse that evidence and select outstanding combinations
from the latest package rather than repeat every successful example.

For each selected workload, prepare fresh state through its persisted setup and check
that arguments point at the intended actual objects, relevant setup actions ran, pending
setup events are excluded, and the measured target was not accidentally run in setup.
Setup preflight stops before target workflow actions. Selected ordinary fault-free
executions can additionally check the execution path, but are separate proof: some
inputs originate in negative tests and legitimately reject their business action.
Report the expected/actual stage and reason rather than require every input to succeed.
The output is a short selected-case table with exact reports, not a claim that all static
candidates execute or that their final states are harmless.

A asks whether a properly run experiment's observed outcome constitutes impact. B asks
whether the tool prepared and recorded that experiment faithfully. An intentionally
injected failure inside the scenario is part of the experiment; failure to launch its
worker is failure to conduct it. Merely being a technical/service exception does not
settle that distinction: it depends on the supported execution contract and evidence.
No broader runtime exception reclassification is included in B1.

B can therefore proceed while A is discussed: neither exact object identity nor
preserving a worker's existing diagnosis depends on a choice of impact weights. They
meet when a selected generated scenario is used to collect the agreed impact evidence.

### Potential-impact measurement beyond deletion

The user accepts a potential-impact indicator without developer-supplied harm rules,
but deletion-only coverage is insufficient. The approved update experiments compare
residual update effects after compensation and stale copied values after relevant event
processing. Their execution evidence is owned by the impact-updates experiment RESULTS.md;
the implemented collector and checks are described below. The PIC explicitly proposes removed-compensation and disabled-handler
mutants for evaluation. Concrete source-supported examples, healthy controls and
instrumentation limits are recorded in
`verifiers/experiments/impact-three-cases/UPDATE-IMPACT-NOTES.md`.
The approved contract and execution plan live in
`issues/2026-09-06-potential-impact-v2/`. ImpactV2 now assesses these three patterns in
ordinary Saga/local execution. The initial eight scored cases and three observer-disabled
controls are followed by the completed 29-schedule benchmark and 30 broader control/fault
pairs. These selected campaigns do not establish full catalogue coverage.

### Impact probes are a methodological option, not an adopted score

A proposed diagnostic probe would first record the scenario's settled state and outcome,
then exercise one existing application operation against that state and a comparable
control. Any mutation by the probe belongs to a separate continuation, not to the original
scenario's final state or score. Independent probes need separate equivalent reproduced
states, so an earlier probe cannot repair or damage the state seen by a later one.
The bounded protocol has now been exercised in six fresh runs. The Tournament read
sequence distinguishes the affected state; answer submission fails equally in both
states due to an existing application defect. See the handbook's behavioral matrix.
The user chose to record the read failure while leaving domain harm undecided.

For example, a surviving student attempt may still accept an answer from its stored
snapshot, while a surviving Tournament may fail to supply its Quiz because a lookup
requires the latest live object. Either result must be measured, not assumed. A failed
probe also needs a justified expected-success precondition: dates, enrollment, access,
and legitimate removal behavior can otherwise explain failure. Successful probes do
not establish complete domain correctness.

The research purpose is to test whether the current structural candidate is a useful
proxy and expose the application semantics it lacks. It does not automatically produce
a generic detector or establish that the same reference rule is harmful in other apps.
The methodological alternatives considered were:

| Design | What the verifier would evaluate | Information/boundary still needed |
| --- | --- | --- |
| Final-state observations | State/relationship predicates at the original observation horizon | A defensible way to identify required live dependencies versus historical snapshots; API failure in one example does not derive that rule generically |
| Bounded continuation probes | A predefined set of application operations from equivalent reproduced post-scenario states | Selection, valid inputs/preconditions, controls, isolation and cost; harm claims are relative to the specified operation set |
| Serial/control comparisons | Differences in selected final observations and outcomes under comparable runs | Comparable faults, timing and initial state; differences do not by themselves establish business harm, and single-Saga compensation damage can survive every serial order |

The adopted ImpactV2 method checks explicit final-effect observations without adding
future application operations. The probes remain supporting research evidence. No
severity weights or LLM inference form part of the method.

### Parallel execution and integration order

The completed ImpactV2 implementation, semantic-state recovery correction, observer
coverage repair, source prerequisites and worker-reporting repair are consolidated in
the primary checkout. Their issue handoffs preserve the individual qualification proof.
There is no outstanding adoption task for the B repair.

1. Review the first Portuguese case sheet with the user, then develop the remaining
   cases without changing the scoring policy.
2. In parallel with writing, a selected cost/launcher audit can prepare the execution
   budget. No additional application mutation or campaign is required for the meeting.
3. After the advisor discussion, implement only the agreed metric refinements and rerun
   the affected controls. Keep known application defects separate from verifier changes.
4. Use the measured cost and reset boundary to compare simple search baselines before
   GA or cross-workload prioritization.

### Specific open follow-ups within those tracks

| Follow-up | Track | Current boundary |
| --- | --- | --- |
| Interpret the measured behavior contrast | A | Read sequence differs for the Tournament; answer control fails independently. Keep harm undecided and identify the application-derived evidence needed for a generalizable final-state interpretation. No continuation-based impact contract agreed. |
| Answer-submission baseline defect | A/C | Both probes fail at getQuizAnswerStep because quizAnswer is read before assignment. The recorded upstream comparison at c240e7cf882b59262ecb731b1507fd6ffae82115 found identical AnswerQuestion and ConcludeQuiz Saga files; this is evidence at that revision, not a fresh upstream check. A bounded repair must also check AnswerQuestion command routing and exact answer identity, establish a valid healthy submission test, and then repeat the paired probe. Application repair remains a proposal; do not attribute this baseline defect to compensation. |
| Full latest-catalogue preflight | B, only if the evaluation needs it | The selected 30 pairs are qualified; they do not replace a full scan of every newer input |
| Original event-reaching triples | C, when needed by A/E | Route fix is complete; those triples still need regenerated packages and coherent receiver preparation |
| Nested scalar property collections / Tournament helpers | C | Deliberately excluded from the completed whole-DTO fix; 23 older helper examples are a cohort, not guaranteed gains |
| Synchronous UpdateQuestionTopics extraction gap | C | Direct benchmark test invocation exists but emits no input; exact extraction cause remains to be diagnosed. The broader 32-Saga classification is complete. |
| Launcher/configuration cleanup | D | Explicitly postponed after artifact work; overlapping options still need a coherent audit |
| Startup/build cost and batch orchestration | D | Generic affordable repeated execution is not implemented; fresh-state guarantees remain mandatory |
| Broader anomaly observations and generic serial comparisons | A, then E if selected | Application tests have some serial baselines; the generic executor has no comparison oracle |

The earlier all-zero rejection is now explained by the existing participant/deletion
guard and the saved join-before-remove schedule; see the experiment RESULTS.md. It is
not an outstanding diagnosis or a justified harm baseline.

The dated artifact audit and issue handoffs are evidence records, not additional active
queues. Completed fixes live in [current evidence](current-state.md#current-evidence).
This section owns outstanding work; completed items are not carried as "leftovers."

### Deliberately conditional work

Do not queue broad dynamic-enrichment reruns, a catalogue of every anomaly type, generic
receiver-state synthesis, cross-test state composition, or scalar impact weights merely
because they were discussed. Each needs a concrete evaluation question. Local fault
search waits for repeatable execution with a useful reward; cross-workload prioritization
waits for several such workloads. Distributed profiles and other fault models remain
[deferred breadth](#deferred-breadth).

## Outcome 0 — Repair exact runtime input identity

**Status: complete (2026-09-02).** The simulator now reads the verifier's
`workloadPlanIds` field. The equivalent bounded smoke changed from 0 exact / 2
test-and-shape / 8 shape-only groups to 2 exact / 0 test-and-shape / 8 shape-only.

### Goal

Make the verifier-produced dynamic input map and simulator reader use one current plan-id field so exact eligible runtime Saga invocations can carry their static `inputVariantId`.

### Why it matters

The bounded final smoke passed five Quizzes features and normalized 1,038 observations,
but exact-input attribution initially remained 0 because the two modules used different
field names. Aligning both on `workloadPlanIds` lets eligible runtime observations carry
the exact persisted input id while leaving the matching rules unchanged.

### Boundary

- settle and implement one field name on both writer and reader;
- keep workload identity, static inference, matching order, and package shapes unchanged;
- prove positive exact propagation plus rejection of wrong-plan, wrong-test, wrong-Saga, and ambiguous candidates;
- rerun the same bounded Quizzes class and compare exact/test-and-shape/shape-only counts without inventing matches.

### Done when

- the simulator accepts the generated current input map;
- a controlled realistic invocation carries the expected exact input id;
- exact attribution remains step- and test-aware and reader accounting reconciles;
- the bounded before/after evidence explains which input/workload claims became stronger.

## Completed qualification — Correct static interaction selection

**Status: complete (2026-09-02).** Command aggregate roots now come from constructor semantics rather than an argument position. Strict multi-Saga selection requires two-sided semantic access evidence and positive exact or same-source input evidence. Typed `SagaCommand` wrappers and justified generic compensation accesses are covered without duplicate forward accesses. The bounded Quizzes comparison reconciles every size-1/2/3 accounting equation while leaving input acceptance and materializability unchanged. The next priority remains improving materializability.

## Outcome 1 — Improve useful input coverage

**Status: in progress.** Package closure, bounded relative-date recipes, setup value
translation for the main observed Quizzes shapes, and workload-driven source setup are
complete. Source setup attachment now uses complete
selected-input coverage for any generator-emitted workload size, preserves each observed
test context, and has bounded runtime proof for one natural triple and the existing
Remove/Add pair. Broader input families remain outcome work.

### Goal

Increase the number and variety of generated WorkloadPlans that can pass runtime setup without weakening the truthfulness of the static setup gate.

### Why it matters

Static Saga discovery alone does not create executable experiments. Every useful experiment needs an exact persisted input tuple. Missing or fabricated values make schedule and fault-space counts irrelevant to runtime evaluation.

### Current entry point

The current handbook records the supported recipe model, the current setup-ready
baseline, and the remaining Sagas without accepted inputs. The four observed Quizzes
relative-date forms are represented and materialized without persisting run-specific
timestamps. Coherent source setup can now attach to singles, pairs, or larger selected
workloads only when it completely supplies the setup-dependent arguments; it does not
synthesize shared state across tests. The latest candidate accounting is in the handbook. Remaining work includes nested
scalar property-result collections, missing source contexts, event payload placeholders,
and the selected event consumer's own prerequisites. Prioritize a representative new
behavior family over another headline count.

### Work direction

- Use the completed missing-input audit to choose between adding useful application tests, preparing event receivers, and diagnosing the one evidenced direct-call extraction gap.
- Add generic recipe support only for patterns observed in dummyapp and justified by a realistic Quizzes case.
- Prioritize patterns that unlock a representative harmful interaction or materially broaden Saga shapes, not patterns that only increase the accepted-input headline.
- Keep source-mode filtering conservative and application-independent.
- Preserve exact argument types and ordered mutations through helper/facade boundaries.

### Done when

- each targeted input family has dummyapp-first positive and negative coverage;
- the static setup gate does not gain false positives in the representative runtime;
- a refreshed Quizzes package and preflight show which additional exact workloads became setup-ready;
- the result explains why those workloads matter to later execution/impact evaluation.

## Outcome 2 — Make a harmful generated interaction executable

**Status: historically achieved (2026-08-01).** A dated v4 Quizzes event-consequence positive/control pair provided repeatable ImpactV1 discrimination under the then-current reader. That package is retained evidence only: the current reader will not consume it, so any new execution or regression claim requires regeneration as a current package. Further event breadth is not part of this outcome.

### Goal

Persist and replay at least one realistic multi-Saga or Saga/event interaction that produces a non-zero generic impact signal under a controlled fault schedule.

### Why it matters

The generic invariant detector needed a persisted scenario with enough prerequisite and event structure to reproduce the real Quizzes stale-read interaction. That gap is now closed for one conservative unique local event route; broader event patterns remain deferred.

### Work direction

- Choose one existing realistic harmful Quizzes interaction as the target oracle.
- Identify the smallest missing representation or materialization capability between its test setup and a persisted WorkloadPlan/FaultScenario.
- Extend the generic model only for that proven gap; do not hardcode the Quizzes feature.
- Preserve deterministic action identity and explicit evidence for every prerequisite.
- Demonstrate a negative/control scenario alongside the positive interaction.

### Historical completion evidence

- the interaction was represented by the dated valid v4 package and an exact persisted FaultScenario id;
- setup succeeds in the supported runtime;
- repeated execution produces the expected conformance boundary and non-zero generic impact finding;
- a nearby control produces zero impact;
- package bytes remain unchanged by execution;
- the claim does not depend on parsing application-specific log text.

For current work, regenerate the interaction through the current writer and re-establish these checks before citing it as current executable evidence.

## Outcome 3 — Define repeatable execution and reset

**Status: complete for the supported boundary (2026-08-01).** Isolation is one fresh ScenarioExecutor container/process with a fresh H2 database per attempt; same-process reset is unsupported. The supported boundary and its limits are described in [ScenarioExecutor](current-state.md#scenarioexecutor); current research repeatability is described in [the four-run experiment](current-state.md#understanding-impact-through-quizzes).

### Goal

Make repeated attempts comparable by defining who resets persistent application state and what constitutes an isolated execution environment.

### Why it matters

Fault-vector search assumes that reward differences come from the vector/scenario rather than residue from earlier attempts. The supported reset owner is the caller/orchestrator, which must create a fresh process/container and H2 database for each attempt. The executor does not claim same-process cleanup.

### Work direction

- Define the supported reset boundary for the chosen runtime profile before implementing batch execution.
- Prefer a simple process/container/database reset contract over application-specific cleanup APIs.
- Record reset identity and failure in execution orchestration evidence.
- Separate package immutability from environment reset: both are required, but they solve different problems.
- Prove deterministic replay under repeated all-zero and selected-fault controls.

### Done when

- the same persisted FaultScenario can be executed repeatedly from equivalent initial state;
- reset failure prevents impact evaluation rather than producing a misleading score;
- repeated controls have stable terminal/conformance and impact results within the defined boundary;
- the reset mechanism remains application-agnostic for the supported Saga/local profile.

## Outcome 4 — Characterize one harmful workload before broadening impact

**Status: historically achieved (2026-08-16).** The dated v4 package, target/control repetitions, and all 34 retained schedules passed exact converter-decoded `GenericSagaState.NOT_IN_SAGA` proof. Landscape schema v2 recorded 19 bounded harmful rows and 15 no-broken-reference rows while ImpactV1 remained zero throughout. The package is historical evidence and requires current-package regeneration before new execution claims.

### Goal

Turn one already demonstrated harmful Quizzes interaction into a persisted, generated, repeatably executable benchmark. Run its bounded fault and recovery space and record execution validity, existing ImpactV1 evidence, and the known final-state condition separately.

Use that result landscape to decide the smallest broader impact contract and later GA reward. Do not choose weights, category ordering, or a general application-check framework before this evidence exists.

### Why it matters

ImpactV1 observes thrown aggregate-invariant rejections, but the exploratory Quizzes tests reproduce final states classified as harmful by the benchmark's explicit rule that it misses. The historical benchmark established variation under its explicit rule. The current uncertainty is what application-derived evidence justifies interpreting such final observations as harm, and how to reproduce that evidence through the current generator/executor.

### First experiment

- reuse the existing `RemoveTournament` and `AddParticipant` recovery-window evidence;
- identify only the missing generation, shared-input, recovery, execution, and final-state observation capabilities;
- persist the exact workload and its bounded FaultScenarios through the normal package path;
- replay a known harmful case and a safe control from fresh state;
- produce a simple per-scenario table rather than a premature combined score.

The final-state rule is applied once across executions: an active Tournament referring to its deleted Quiz is harmful for this experiment. Setup or infrastructure failure remains not evaluated. Existing invariant rejections stay separately visible.

The completed landscape shows useful final-state variation that ImpactV1 does not detect: all schedules for `00100`, `00101`, and `00110` satisfy the bounded harmful rule, while all other canonical rows do not, yet every ImpactV1 score is zero. ImpactV2 now supplies the broader versioned contract described below, keeping invariant and final-state evidence separately inspectable. Qualification of its coverage is the next step. GA reward design remains downstream; no weights or category ordering are implied by the historical labels.

Impact-design constraints retained by the first ImpactV2 slice:

- Do not use LLM-based harmful-state inference, including an LLM hypothesis-generation
  stage, as part of the thesis method.
- Intermediate visibility or concurrency exposure alone contributes no impact when
  the operations succeed and the settled final state is domain-correct. Keep such
  observations available as separate diagnostics of potential concurrency issues.
- A lack of detected violations does not establish domain correctness; insufficient
  evidence must remain distinguishable from an established zero-impact outcome.
- Existing invariant checks and serial/interleaved comparisons remain under assessment.
  Some Quizzes tests already contain hand-written serial baselines and interleavings;
  the generic executor does not yet compare their outcomes to derive an impact verdict.

These constraints do not change the current invariant-count ImpactV1 implementation.

The read-only Quizzes audit found that blanket final-state `verifyInvariants()` replay
is not a sufficient oracle: QuizAnswer and Topic checks are empty; several checks are
pure predicates over local fields or embedded snapshots; Quiz/Tournament transition
checks depend on retained `prev` and mutation timestamps; and Tournament's
`invariantAnswerBeforeStart()` reads `DateHandler.now()`, so its verdict can change as
time advances without a state change. Selected local checks remain possible diagnostic
evidence. A controlled evaluation time, persistence/history loading and distinction
between check failure and domain violation need definition before generic reruns.
No invariant rerun or new final-state rule was implemented by this audit.

The [four-run experiment](current-state.md#understanding-impact-through-quizzes)
provides a reproducible surviving-reference candidate comparison. Its six behavioral
follow-ups are complete. The user accepts potential-impact observations without a
business-harm oracle and has requested update experiments before a broader implementation
proposal. These experiments remain separate from generated execution and production scoring.

### Done when

- the selected interaction is a valid persisted WorkloadPlan with executable FaultScenarios rather than only a hand-driven test;
- repeated execution reproduces the known harmful final state and a safe control;
- the bounded fault/recovery landscape records valid, harmful-for-this-rule, safe-for-this-rule, and not-evaluated outcomes without manual per-scenario labels;
- package, scenario, execution, and result identities are reproducible;
- the evidence supports a concrete decision about the minimum later impact contract and whether a GA reward has useful variation.

### Proposed route to potential-impact scoring in ordinary execution

The assembled implementation checkpoint is in
`issues/2026-09-06-potential-impact-v2/SPEC.md` and `PLAN.md`. It follows the completed
experiments and the requested source/catalogue audit of competing writers. The user
accepted unresolved delivered events as a potential-impact category and delegated the
documentation/orchestration choice. The shared evidence collector and three deterministic
checks with their scored assessment are implemented and passed the bounded persisted
Quizzes qualification. This completes M0/M1/M2 of the first implementation slice.
The intended scope is all scenarios already supported by the Saga/local executor, not
all transactional profiles, all static candidates, or all possible consistency defects.

**Outcome:** every valid executable attempt produces evidence and a category-level
potential-impact assessment without a per-scenario oracle. The scalar, where evaluable,
counts distinct affected aggregate identities. The report explains each counted object
and preserves unknown coverage; it does not equate more objects with greater business
severity. Existing invariant rejections remain diagnostic evidence and contribute no
points merely because a safeguard fired.

1. **Collect the same facts automatically.** Install an attempt-scoped observer after
   setup. Record exact aggregate identities, committed writes,
   before/after persistent data, actor/step/phase, recovery outcomes, and exact event and
   subscriber identities. Capture the baseline before the first measured action and the
   final state after the scheduled execution/recovery finishes. Reuse framework hooks,
   the persistence metamodel and stored version chains. Prove transaction commit rather
   than treating an attempted merge or command dispatch as a durable write. Preserve
   read versions when available at existing hooks; these three checks do not require
   cached-read reconstruction. Observer
   failures must leave application execution unchanged where possible, record the evidence
   gap and invalidate the assessment rather than fabricate an application fault or zero.
2. **Separate observation from checking.** Normalize managed persistent data using a
   deterministic generic serializer: stable aggregate identity, owned data, collection
   semantics and explicit graph limits. Keep framework version, row identity, creation
   timestamp, predecessor and semantic lock in evidence but outside business-data
   equality. Do not drop arbitrary application timestamps or every field called version.
   Identity/cycle/unsupported mapping failures become recorded coverage gaps. The first
   implementation uses existing `Aggregate.getEventSubscriptions()` declarations for
   dependencies and event eligibility; it does not add general DTO/reference inference.
   Both research Quiz references are already represented by that API. Receiver identity
   comes from the owning aggregate, and target identity must resolve in persisted state.
3. **Start with three supported findings.** Retain declared dependencies on objects deleted
   during measurement; add remaining data changes attributable to a failed Saga; add
   an eligible event's lack of receiver progress after completed delivery. For residual
   writes, require a committed failed-Saga contribution and no other writer of that
   aggregate during measurement, including event consumers and later writes after recovery.
   Otherwise only that object's residual attribution is unknown; other checks continue.
   Recovery snapshots may explain a successful restoration but are not separately scored.
   This restriction is practical: existing Quizzes tests restore an update and then
   legitimately anonymize the same Tournament, or add a participant during deletion.
   Source inspection is not a new execution result or an incidence estimate. For event progress,
   use actual subscription eligibility and durable receiver state; one returned handler
   call or a raw retained-event row count alone is insufficient. A demonstrated no-progress
   condition remains potential impact, not proof that every event must change business
   data. Keep copied-value divergence only where source/receiver mapping is proven.
4. **Integrate and qualify.** Add a separately versioned ImpactV2 report to ScenarioExecutor,
   keeping attempt/package/scenario identity and V1 meaning intact. Generic dummyapp tests
   cover positive and repaired/guarded/idempotent controls, metadata-only changes, duplicate
   signals, missing observations and concurrent interference. First run representative
   persisted Quizzes scenarios for deletion, propagation and compensation through the
   same collector. This finishes the initial implementation slice. Then use measured
   cost/completeness to size the broader executable-set qualification and historical
   34-case refresh. These qualifications are now completed and recorded below. The
   historical 19/15 split belongs to its original experiment and metric; it is not an
   expected split for the corrected 29-schedule ImpactV2 benchmark. Publish observation
   coverage separately from positive findings.

The observation horizon must be part of the report: scheduled actions and recovery plus
only the event actions that the scenario actually includes. Do not silently run extra
handlers or future application operations to obtain a score. Unsatisfied relevant pending
work can make a final-effect interpretation unknown; unrelated retained events do not by
themselves invalidate it. A future explicit event-draining policy would be a separate
execution contract, not an observer side effect.

Implemented report shape in plain language: detected-object count, evaluation completeness,
per-category count/status, object-level evidence, and unsupported/unfinished observations.
A full score is numeric only for complete evaluation of the declared detector scope;
partial evaluation may retain a clearly labeled observed lower bound but must not be
ranked as equivalent to a fully evaluated zero. Invalid executions retain null score.
Every scenario gets a report; not every scenario is promised a justified complete score
before qualification establishes coverage.

**Recovery correction completed:** semantic-state writes now feed conflict/recovery
analysis, and pending runtime recovery prevents a false compensated result. The corrected
benchmark has 29 schedules across the same 12 vectors; all 29 ran, with an additional old
incomplete-plan negative control rejected as expected. The 34-to-17 projection from the
earlier campaign must not be treated as equivalent recovery.
Proof: `issues/2026-09-06-semantic-lock-recovery/HANDOFF.md`.

**Observer coverage completed:** verified nested owned backreferences now project without
losing child data. All 29 benchmark assessments are COMPLETE (14 zero, 15 two), with the
same execution/recovery outcomes and unchanged previously complete scores. Unsupported
cycles still produce explicit gaps. This resolves the ten partial benchmark assessments.

**Control qualification:** per-argument readiness repairs the UserDto/TopicDto blocker;
source test fixtures supply actual Quiz IDs, required Tournament participation and event
receivers. The retained 30 broader pairs are requalified with explicit input replacements,
not silently dropped: all 60 final assessments are complete, and all 30 controls succeed
with exact conformance. One successful control has a positive score, so success is not
equated with zero observations. Both invalid prerequisite attempts remain in the record.
This work supersedes the earlier 17 usable pairs and is tracked in
`issues/2026-09-06-impact-coverage-controls/HANDOFF.md`. The earlier 17-schedule campaign
and its 60 broader attempts remain historical observations, not current recovery proof.

**Next priorities, with scoring policy held for discussion:**

- Discuss concrete domain cases before deciding whether newly created objects that compensation
  leaves only as deleted storage records should count. The current residual rule counts these, as it
  counts application-owned mutation timestamps. Keep the raw evidence; do not silently
  change the comparison or treat the count as severity. The examples in
  `issues/2026-09-06-impact-coverage-controls/DOMAIN-CASES.md` also distinguish a positive
  successful control from a fault-specific finding and show why equal counts can have
  different causes. No scoring-policy change is authorized by this qualification.
- Address the separately observed Quizzes defects in bounded application changes with
  their own tests. Newly exposed: `TournamentTopic.buildDto()` drops the course ID used
  to restore embedded topics during compensation. The current event consumer's missing
  save also remains a production follow-up; its qualification repair was temporary.
  The broader campaign also exposed a Course execution counter not restored after failed
  removal and an active Question left by a failed creation. Keep application fixes and
  their tests separate from changes to measurement.

Lost-update graphs, proof of propagated values from compensated updates, arbitrary field
lineage, severity weights, automated serial baselines and GA/RL are later extensions.
They do not block this first implementation. Application bugs found by the experiments
need separate bounded fixes and healthy tests before being used as fault-induced
comparisons; a baseline defect can still be reported as observed potential impact with
its cause distinguished from an injected fault. Qualify an unassigned-fault control for
each evaluation workload before search: otherwise the search may merely rediscover a
bug already present on the ordinary path. Preserve the raw observation count and control
result rather than silently subtracting unrelated findings or calling every difference
fault-caused.

The implementation and broader qualification are recorded in
`issues/2026-09-06-potential-impact-v2/` and
`issues/2026-09-06-impact-v2-broader-qualification/`. Keep measurement repairs and
application bug fixes separate, each with its own controls.

## Outcome 5 — Refresh dynamic attribution only for a concrete need

**Status:** the bounded current-package smoke is complete; broader enrichment remains conditional on a named evaluation question. Exact input-map repair is Outcome 0 and precedes another broad run.

### Goal

Use dynamic evidence when it can resolve a specific static-identity or coverage problem that blocks useful generation or interpretation.

### Why it matters

Dynamic enrichment is expensive and produces many raw artifacts. Historical broad exact/high-confidence counts showed that attribution can work, but those counts are not themselves execution or impact progress.

### Work direction

- First identify a current-package workload whose usefulness depends on unresolved runtime identity.
- Run a bounded current-package enrichment baseline with explicit memory, profile, and test selection.
- Prefer direct input ids and structured simulator events over fuzzy value/name matching.
- Treat ambiguous, unmatched, and not-covered results as evidence boundaries, not metrics to optimize blindly.
- Do not let dynamic evidence rewrite package identity or persisted actions.

### Done when

- the run answers a named generation/execution question;
- package bytes remain unchanged;
- attribution improvement is measured against a controlled before/after case;
- raw artifacts and runtime cost are justified by information that changes a decision.

A broad Quizzes refresh is not automatically a higher priority than the reduced generic impact contract.

## Outcome 6 — Local fault-vector search

### Goal

For one fixed useful WorkloadPlan, search persisted fault vectors under a finite execution budget and return high-impact distinct experiments.

### Entry conditions

Do not start until:

- the chosen workload is setup-ready and repeatably executable;
- environment reset is defined;
- at least one vector produces a discriminating evaluated result under an approved versioned impact contract; the automatic source-derived benchmark's final-state rule is evidence for designing that contract, not yet the generic search reward;
- arbitrary valid vectors can be persisted idempotently through the current on-demand path;
- invalid/infrastructure attempts cannot masquerade as zero fitness.

### Minimal design boundary

- Search unit: one fixed WorkloadPlan.
- Candidate identity: persisted FaultScenario/vector identity.
- Reward: one explicitly versioned impact result.
- Duplicate policy: never spend budget re-evaluating the same deterministic candidate unless repeatability is itself under test.
- Seed and budget: explicit and recorded.
- Baseline: compare against eager all-zero/single-point vectors and a deterministic random or exhaustive bounded baseline before claiming benefit from a genetic algorithm.

Do not commit prematurely to tournament selection, crossover, mutation, or population parameters. Choose an algorithm only after the executable vector space and reward distribution are measured.

### Done when

- a bounded search run is reproducible from package id, seed, budget, and runtime configuration;
- every evaluated reward links to a valid execution and impact report;
- duplicate and invalid-attempt accounting is explicit;
- the selected method outperforms or usefully differs from the declared baseline on a representative workload.

## Outcome 7 — Prioritize across workloads

### Goal

Allocate execution/search budget across multiple useful WorkloadPlans using their structural context and observed rewards.

### Entry conditions

Do not start until several workloads:

- are repeatably executable;
- expose meaningful structural differences;
- have non-flat, comparable impact rewards;
- support a stable local-search/evaluation cost.

### Work direction

- Define workload-level context from existing package facts such as participant count, step/fault-slot count, conflict evidence, aggregate footprint, and setup history.
- Establish simple baselines first: random, deterministic structural ordering, and equal budget.
- Use a bandit/contextual method only if shared context measurably improves discovery under budget.
- Keep workload selection separate from vector search so each result remains explainable.

### Done when

- the prioritizer's decision and observed reward are reproducible;
- comparison against simple baselines is fair and budget-matched;
- selected context features are current package facts, not application-specific labels;
- the result improves harmful-scenario discovery or reduces required executions on a representative workload set.

## Deferred breadth

The following may be valid later, but they are not prerequisites for the Saga/local thesis path unless evaluation scope changes:

- TCC-specific generation and execution;
- stream/gRPC/distributed runtime parity;
- true parallel execution;
- delay and non-binary impairments;
- compensation fault injection and retry/backoff policies;
- network-filesystem/multi-host package writers;
- generic support for every application framework pattern.

Broaden only when a thesis claim or representative scenario requires it.

## Roadmap decision rule

Before adding a new stage, metric, artifact, or abstraction, answer:

1. Which current thesis or user question does it answer?
2. Which decision changes because the result exists?
3. What is the representative positive and negative evidence?
4. Is the information already available from a current artifact?
5. Can an existing concept be deleted or reused instead?

If those answers are unclear, defer the addition.
