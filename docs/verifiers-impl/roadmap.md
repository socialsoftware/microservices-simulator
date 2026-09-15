# Verifier roadmap

[`current-state.md`](current-state.md) owns implemented behavior, terminology and evidence.
This roadmap owns remaining work and its order. Completed issue packages and dated
experiments are supporting records, not additional active queues.

## End goal

Derive concrete Saga workloads from application source and tests, generate and execute
fault scenarios, report supported potential-impact and anomaly observations, and prioritize
experiments under a finite budget. Claims remain bounded by the selected applications,
inputs, schedules, runtime profiles and observation coverage.

The supported Saga/local path already includes source-derived setup, controlled execution,
three persistent-state criteria, compensated-read exposures, lost copied updates and
fixed-workload GA/random search. Individual criterion weights are configurable; the
historical distinct-object policy remains available. Further work should evaluate and
explain this implementation before adding unrelated detector or input families.

## Current next work

| Priority | Work | Completion boundary |
| --- | --- | --- |
| 1. Finish and interpret the larger GA/random comparison | The approved 500-attempt-per-method campaign uses three matched seeds on one four-operation workload. Inspect completion, invalid/unavailable outcomes, discovery curves and cost. | A reproducible comparison with uncertainty and scope stated; no requirement that GA win. |
| 2. Consolidate the repository and documentation | Identify completed changes for review/commits, remove repeated status narratives and distinguish reusable tools from research prototypes. | Current behavior and actual pending work are easy to find; evidence remains reproducible. Preserve the running campaign's frozen dependencies. |
| 3. Prepare the paper evaluation | Use the completed search results and a bounded static-analysis/compression protocol. Explain configuration, weights, costs and limitations. | Agreed evaluation questions and measured results support the manuscript; writing does not anticipate unmeasured outcomes. |
| 4. Broaden the application comparison | Inspect Quizzes, quizzes-full and quizzes-full-2 before treating them as distinct evaluation targets. | Document source/test differences, compatible configurations, generated workload counts and runtime qualification where needed. |
| 5. Define cross-workload learning | Use several qualified workloads and compare allocation against simple baselines. | A small agreed reward/context/budget protocol before implementing RL or a contextual bandit. |

The user wants learning work soon. It need not wait for every anomaly family or proof of
universal GA superiority. It does need an interpretable local-search baseline and enough
workload variety to make allocation meaningful. The active single-workload campaign alone
does not establish that variety.

## Outcome 6 — Local fault-vector search

GA and random execution are implemented. The remaining outcome is evaluation, not another
implementation of the basic search loop. The [search runner](../../verifiers/experiments/fixed-workload-ga/README.md)
owns configuration and replay instructions.

The current [recovery-qualified campaign](evidence/recovery-history-2026-09-15/README.md)
uses 500 attempts per method, seeds 11/29/47 and five unit weights. Its declared domain has
5,184 fault/action candidates for one fixed forward workload. Live progress and completion
are recorded under `verifiers/target/ga-500x3-2026-09-15/`; do not infer completion from this
roadmap. The correction's controls and pilot qualify this runtime, not GA effectiveness.

Evaluate:

- cumulative distinct positive scenarios against actual execution count;
- best score found and the underlying criterion counts;
- unavailable attempts, duplicate proposals and early stops;
- generation, application execution and search overhead, distinguishing measured stages;
- variation across seeds, with the same workload, runtime, weights and budget per method.

The earlier [29-case discovery comparison](../../verifiers/experiments/fixed-workload-ga/DISCOVERY-RESULTS.md)
has a complete reference map. The larger domain does not: do not claim time to all positives
or recall without that denominator. Positive scenarios are not counts of distinct bugs.
Unavailable attempts consume execution budget and retain null fitness, not zero.

Repeated offspring and random fallback are already observed in the small benchmark.
Inspect their frequency in the larger run before changing selection, diversity or mutation.
Any operator experiment is separate from a weight experiment; freeze the comparison before
seeing which configuration makes GA look best. Keep population/mutation defaults visible
as engineering choices, not established optima.

### Preference sensitivity

After the first larger comparison, choose a small set of explicit criterion-weight
configurations with the user. Revaluing saved reports can explain score differences but
cannot reproduce an adaptive GA trajectory under different feedback; online comparison
requires new matched runs. An impact-only or anomaly-only configuration can be an ablation,
without defining either as the sole intended tool objective.

The current four-operation workload's qualification supplied persistent-state variation,
but no read-exposure or lost-copy positive. Keep those detector controls separate and
select anomaly-informative workloads before claiming to evaluate their search benefit.

## Static-analysis and compression evaluation

Compare segment-compressed scheduling with bounded exhaustive schedules where enumeration
finishes. Report what counts are reduced, which semantic action orders are retained, caps,
generation time and memory. Count-only results and materialized packages answer different
cost questions and must be labelled accordingly.

Strict input-aware and type-only interaction selection are different selection policies,
not interchangeable compression algorithms. Compare their workload sets and preparation
coverage on the same source snapshot and declared configuration. Do not present additional
candidates as additional successful or harmful executions.

Inspect Quizzes, quizzes-full and quizzes-full-2 before scheduling a broad package per
application. Record Saga/test/input differences, profile compatibility and all Saga-count,
input-count, schedule and event-route limits. Use bounded measurements to choose affordable
runs; a capped result is not the application's total possible workload space.

The existing [static refresh](evidence/static-refresh-2026-09-09/README.md) is a matched
before/after input-preparation comparison. Reuse it for its actual claim; it does not
replace the broader compression/application evaluation. Exact catalogue accounting in
`InputTupleSelection.countGroups` was expensive in a high-input triple configuration;
measure it against the earlier implementation before attributing that cost to newer input
support or changing defaults.

## Outcome 7 — Prioritize across workloads

Allocate execution/search budget using workload facts and observed feedback. Define the
unit of a decision, the reward, treatment of unavailable attempts, and cost budget before
selecting an algorithm. A weighted scenario score is available, but it does not by itself
define the reward for an allocation decision.

Candidate context already present in packages includes participant and fault-slot counts,
conflict evidence, aggregate footprints and setup structure. Compare with random selection,
equal allocation and a simple structural order. Keep workload allocation separate from
fault/recovery selection within a workload so decisions remain explainable.

Start from several repeatably executable workloads with meaningful structural and outcome
variation. Record seeds, selected actions, feedback and runtime cost. A useful completed
study can identify limits or show no improvement; do not make a positive result a condition
for honest completion.

## Specific open follow-ups

These are conditional candidates, not simultaneous assignments. Select one only when it
unlocks a named evaluation case or resolves an interpretation problem.

| Area | Remaining work and boundary |
| --- | --- |
| Input preparation | Unsupported constructor/HashSet representations, scalar property collections, helper shapes and missing source contexts. Diagnose an exact case before extending the recipe model; keep identities, types and ordered mutations explicit. |
| Synchronous UpdateQuestionTopics | The `each` closure extraction gap and iteration/accumulation semantics remain separate work. A supported ordinary application test may supply a smaller route than generic loop expansion. |
| Event breadth | Original event-reaching triples still need coherent receiver preparation and regenerated qualification. Indirect event-only relationships pruned before route expansion are a separate generation question. |
| Full catalogue preflight | Selected controls do not prove that every static setup candidate executes. Run a broader scan only if the evaluation needs that coverage claim. |
| Focused Saga-set generation | A developer-selected Saga set could constrain subset/input/schedule enumeration before local search. Expose inherited limits; local search cannot recover workloads excluded upstream. |
| Launcher/configuration | Audit overlapping entry points and frozen-build dependencies. Preserve fresh process/H2 isolation; no same-process reset is currently supported. |
| Matched event-order rejection | The research comparator is qualified. Ordinary integration, automatic control pairing and inclusion in fitness remain decisions; legitimate rejections alone do not imply harm. |
| Additional anomalies | Arbitrary computed-value lineage, propagated-value consequences, broader lost-update graphs and generic serial comparisons exceed the implemented bounded detectors. |
| Application timestamps | Business-owned timestamps remain in persistent-state comparison. Any timestamp policy change needs explicit semantics and requalification. |
| Dynamic enrichment | Run it for a specific unresolved runtime identity question, keeping package identity immutable and ambiguity explicit; do not refresh broad counts for their own sake. |

### Application defects remain separate

Recorded findings include the answer-submission baseline reading `quizAnswer` before
assignment, missing topic course IDs during Tournament recovery, an event consumer's
missing save, a Course counter not restored after failed removal and an active Question
left by failed creation. These need individual application review, healthy tests and a
current upstream check before a repair is selected. A historical observation is not proof
that every current branch still has the defect.

The answer-submission proposal must also check command routing and exact answer identity.
Both original probes failed, so it is not yet a fault-induced behavior contrast. Other
application fixes can change measured benchmark outcomes; keep those changes separate
from measurement or search changes and retain the old evidence.

## Completed foundations to reuse

| Capability | Current authority or qualification |
| --- | --- |
| Exact runtime input identity, static interaction selection and source setup | [Inputs and extraction](current-state.md#inputs-and-static-extraction), [dynamic evidence](current-state.md#optional-dynamic-evidence) and [current evidence](current-state.md#current-evidence) |
| Nested setup results, returned-DTO setters and integral participant binding | [Source update/read setup](../../issues/2026-09-09-source-update-read-inputs/HANDOFF.md), [integral-input fix](evidence/integral-input-materialization-2026-09-15/README.md) |
| Worker failure reporting and selected prerequisite qualification | [Preflight reporting](../../issues/2026-09-05-preflight-failure-reporting/HANDOFF.md), [coverage controls](../../issues/2026-09-06-impact-coverage-controls/HANDOFF.md) |
| Event combinations and empty eligible receivers | [Combined routes](current-state.md#combined-event-qualification), [empty-delivery qualification](evidence/empty-event-delivery-2026-09-07/README.md) |
| Semantic recovery, owned-cycle observation and recovered-creation policy | [ImpactV2](current-state.md#impactv2-assessment) |
| Forward undo history during compensation | [Recovery-history correction](evidence/recovery-history-2026-09-15/README.md) |
| Compensated reads and automatic copied-value overwrite observations | [Read diagnostic](current-state.md#saga-read-exposure-diagnostic), [integrated lost-copy qualification](evidence/lost-copied-update-2026-09-15/README.md) |
| Configurable fitness and repeatable isolated search | [Search configuration](../../verifiers/experiments/fixed-workload-ga/README.md), [weighted qualification](evidence/weighted-fitness-2026-09-15/README.md) |

The historical 34-scenario benchmark, its later projections and the corrected 29-case
reference have different execution/measurement scopes. Their reports preserve those
scopes; none is a pending implementation task or an expected count for the active campaign.

## Impact probes are a methodological option, not an adopted score

The completed behavioral probes exercise an application operation after a scenario and
compare it with an equivalent control. Tournament reads distinguished the affected state;
answer submission failed independently in both states. The user chose to retain the read
failure without requiring a domain-harm verdict.

Future probes need valid preconditions, equivalent isolated starting states and a separate
observation horizon: a probe must not repair or damage the original scenario before its
score is recorded. They remain supporting research, not part of ordinary ImpactV2 or a
generic correctness oracle. Blanket final-state invariant replay is also not adopted:
some checks are empty, transition-dependent or time-dependent.

## Proposed route to potential-impact scoring in ordinary execution

This earlier proposal is implemented. [ImpactV2](current-state.md#impactv2-assessment)
owns its contract; the approved [specification](../../issues/2026-09-06-potential-impact-v2/SPEC.md)
preserves the design. No new implementation task is implied by this retained link target.

Keep the adopted boundaries in future work: no LLM-based harm inference, no developer
oracle required for the supported observations, explicit coverage and null unavailable
scores, and no silent extra event deliveries or future operations. Invariant rejection
alone earns no impact points. Object counts, anomaly counts and user-weighted search
priority have different meanings; none establishes business severity or complete domain
correctness. Intermediate exposure can inform search without establishing lasting harm.

## Deferred breadth

TCC, stream/gRPC parity, genuinely parallel application execution, delay/non-binary fault
models, compensation fault injection/retry policies, multi-host package writing and generic
support for all framework patterns are outside the immediate Saga/local thesis path.

## Roadmap decision rule

Before adding a stage, metric or abstraction, identify the thesis question, the decision
its result changes, representative positive/negative evidence and whether an existing
mechanism already supplies it. Keep an extension only when that benefit justifies its cost.
