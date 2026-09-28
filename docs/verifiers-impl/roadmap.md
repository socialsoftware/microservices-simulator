# Verifier roadmap

[Current state](current-state.md) owns implemented behaviour and terminology. This file
owns the next work; dated reports own experiment history. The priorities below replace
older launch plans and algorithm-development comparisons, not their evidence.

## Where we are

### Meeting direction — 22 September

The [meeting decisions](reunioes/2026-09-22.md#4-decisões-da-reunião) supersede the
earlier intention to close allocation evaluation and immediately compress its presentation.
For GA, first describe the observed impact types, their prevalence and co-occurrence, then
explain search performance against that reference. For allocation, compare what hybrid,
independent and progress-only methods actually discover, by impact type and application
behaviour as well as aggregate score. Audit implementation correctness and fair comparison
before expanding the sample. Once validated, use existing evidence and additional cluster
collection to evaluate broader Quizzes workload families with explicit input, generation
and execution bounds. Feed diagnosed verifier limitations back into fixes and revalidate
affected comparisons; do not select changes merely to obtain a preferred winner.

The local Quizzes measurement audit reclassified 12 retained keyed-list residuals from
unknown to score 1 with the production assessor, and now records context for incomplete
copied-update traces without changing their scores. The 1,270-scenario cohort has 519
incomplete fifth-criterion traces and three failed executions; four fresh diagnostic
replays reproduce the scope, duplicate-identity and cross-thread gaps. Two remaining
residual cases are recovered creations absent at baseline, while three event scenarios
fail when their receiver has been deleted. Synchronous between-command copy provenance
now has a generic observer/assessor extension and a separate, frozen remeasurement cohort;
thread provenance and duplicate identities still require separate measurement decisions.
The [completed synchronous qualification](evidence/saga-copy-transport-2026-09-28/README.md)
recovers 269 evaluable copied-update verdicts from 270 attempts (all negative); one
startup failure remains unavailable and four asynchronous cases remain excluded.
Joint fitness is 56 positive, 213 negative and one unavailable, with unchanged other
criteria on completed executions.
The historical 519-gap count above describes the retained pre-extension measurements.
The separate 80-run joint-score replay of the same eight policies, five seeds and
two collections is [complete and audited](evidence/workload-master-inventory-2026-09-25/measurement-replay-results-2026-09-28.json).
It followed both completed hybrid alpha-control cohorts and preserved the original
alpha parameters to isolate the measurement revision. The failed remeasurement
remains unavailable. Discuss the close S/SP results without giants and the full
collection's score/positive-count/family trade-off before choosing further controls.
Order, new progress inputs and concurrent observation remain separate decisions.
Complete scoring of every scenario is not an admission requirement for the larger study:
freeze coverage eligibility before looking at rewards, retain unknowns, and report their
rate by criterion and workload family so concentrated gaps cannot be hidden by sample size.

The separate 24 September Quizzes workload search qualified complete maps 201
(deleted dependency: 2/8 positives) and 205 (compensated read: 2/16 positives), with
normal-source provenance and zero controls. These extend the development sample but
repeat mechanisms already present in families 1 and 133. Keep their mechanism groups
together in any train/test split. A normal source-derived `UpdateQuestion` event workload
now scores one event positive on exact no-fault delivery; the no-delivery control and
both pre-step faults score zero. The pilot admission gate now allows its positive
baseline with complete evidence while withholding that score from the bandit and GA;
its three-scenario complete map now supports development replay. It is not part of the
earlier 709-scenario inventory or a reserved confirmation panel. The pilot gate also
admits fully scored `PARTIAL_COMPENSATED/DEVIATED` controls as baseline failures;
their causes and reproducibility still need analysis before final sample selection.

Temporarily allow the manuscript to exceed the page limit so the fuller analysis can also
support the dissertation. Select and shorten the paper's results afterwards. This records
the agreed direction, not a new cluster booking or an experiment launched by the note update.

Source-derived preparation, controlled Saga/local execution, five configurable impact
criteria, and GA/uniform-random search within one workload are implemented. A sequential
contextual allocator supports recorded feedback and now has a sequential live dispatcher
with durable pause/resume. Contract/input validation and the four-attempt Docker smoke
pass, including pause/resume without duplicate execution. See the
[live verification](evidence/live-allocation-2026-09-21/RESULTS.md). The active [21 September follow-up](research/allocator-factorial-2026-09-21.md)
has completed the three-model/two-rule comparison and unavailable-assessment audit.
The [verified results](evidence/allocator-factorial-2026-09-21/RESULTS.md) do not support
enabling the missing-feedback rule by default or claiming that structural context always
improves allocation. Paper updates wait for André's review of these results.

The [20 September continuation](research/cluster-next-campaign-2026-09-19.md) completes
both large candidate maps: 3,360 and 3,918 attempted scenarios. The separately labelled
reference after the authorised timeout repeat has 3,006 positive, 478 zero and 434
unavailable scores in the larger map. The separately retained v3 residual reassessment now
has 3,190 positive, 478 zero and 250 unavailable scores in that map. Five smaller maps also finished; three creation
workloads have 58 zero-score scenarios including their controls. The Topic/event map
has 17 positive, 25 zero and two unavailable scores among 44 candidates.

The [preference comparison](evidence/weight-preferences-2026-09-20/REPORT.md) reruns the
frozen GA and uniform random locally with three agreed profiles and thirty paired seeds.
For deleted dependencies alone, the 3,360-case map contains 150 confirmed positives;
at 1,000 selections GA finds 90.07 versus random's 44.47. Both large maps remain related
workloads from the same Saga family. The separate
[5,184-case study](evidence/full-map-results-2026-09-18/RESULTS.md) is complete.

The local experiment interface discussed at the meeting now has a bounded first
implementation: [Fault Lab](../../verifiers/webui/README.md) browses retained results and
launches prepared allocation experiments. Automatic application preparation, cluster
scheduling and multiuser hosting remain outside that interface's current scope; the
evaluation priorities above are unchanged.

## Transfer inventory — 21 September

The [local inventory](evidence/transfer-inventory-2026-09-21/RESULTS.md) identifies 96
complete recorded workload references across 45 Saga sets. A proposed same-runtime,
small-map split uses 66 one/two-Saga workloads (485 scenarios) for prior learning and
15 three-Saga workloads (1,026 scenarios) for testing new combinations of known Sagas.
The [four-arm transfer comparison](evidence/allocator-transfer-2026-09-21/RESULTS.md)
is complete: 30 seeds, a 132-observation common learning prefix and 256 target selections.
Structural prior learning finds 138.10 positives versus 144.73 without it; progress-only
learning finds 146.77 versus 150.20. The initial structural advantage does not persist to
the primary endpoint. Target positives concentrate in one Saga set, whose ten orders have
identical structural profiles; the dependency-only target profile is entirely zero.
Metadata adaptation and independent verification are complete, with no application/cluster
execution. Review this bounded result with André before changing features or adding runs;
no tuning or new campaign follows automatically.

### Allocation follow-up — authorised bounded exploration

André approved exploring this sequence on 21 September: bounded implementation,
independent review and local recorded-feedback comparisons. Reward changes, cluster
application runs and paper edits remain outside this follow-up.
The main question is allocation during one search starting without prior learning;
transfer from earlier workloads is a complementary question.

1. Freeze a common initially untrained experiment: identical admitted workloads,
   global attempt budget, per-workload GA, feedback availability and preference weights.
2. Compare round-robin, uniform workload selection, independent adaptive UCB and the
   existing contextual policies, including progress-only, under matched settings.
3. Test a small set of automatically extracted ordinary-order features separately:
   relative read/write positions and event-delivery positions on potential interactions.
   Validate step/access provenance first; type overlap is not proof of same-object access
   or a harmful outcome. These features must not inspect unselected fault outcomes.
4. Consider a separate hybrid shared/workload-specific LinUCB variant, so shared
   expectations can coexist with local evidence. Do not bundle it with the feature change.
5. Confirm the selected method on another workload collection fixed before examining
   its new comparison outcomes. Previously examined collections are development evidence.

The [frozen comparison protocol](evidence/allocator-order-hybrid-2026-09-21/protocol.json)
uses the existing 15- and 66-workload collections separately, seven arms and thirty seeds,
with 256 initially untrained choices each. Both collections were examined previously;
confirmation means another collection for the new comparisons, not untouched data.

The [completed comparison](evidence/allocator-order-hybrid-2026-09-21/RESULTS.md)
has 420 verified searches (no new application execution). At 256 choices, adding order
changes mean impact from 171.37 to 176.13 in the 15-workload collection and from 43.57
to 48.37 in the 66-workload collection. The hybrid reaches 186.43 and 47.43 respectively.
Independent UCB already reaches 182.83 in the first collection; progress-only reaches
60.27 in the second. No universal winner or production-default change follows.
Implementation and independent checks are complete; review results with André before
combining variants, additional experiments or manuscript changes.

Keep the user-weighted impact reward, reporting accumulated impact and positive
discoveries. The current shared model already includes each workload's observed progress;
the proposed hybrid adds workload-specific learned parameters, not progress tracking for
the first time. Model choice and paper claims follow evidence; no deep-RL requirement.

## Count-only follow-up agreed during paper review

André authorised extending count-only generation to fault scenarios, including events
and recovery, through sets of three and four Sagas. The implementation and complete
Quizzes counts are recorded in [the count report](evidence/fault-counts-2026-09-20/RESULTS.md).
All 819 accepted inputs and all 74,481 sets of 2–4 distinct Saga types are covered;
no cluster booking or application execution was needed for these mathematical totals.
The newest [paper review handoff](research/paper-revision-plan-2026-09-18.md#latest-agreed-review--20-september-evening-resume-here)
records André's edits and agreed structure. Resume writing block by block with him,
using workload/fault-scenario terminology and these verified counts; do not restore the
old Experimental Setup/search-method dump. Pruning/compression preservation remains a
separate bounded runtime result. The authorised recorded-feedback contextual allocator is
implemented; broader RL design remains outside the current execution package.

## Active sequence agreed on 20 September

1. Complete the generic Full handler-switch and necessary Full-2 input extraction fixes;
   validate and count the three variants under the same current selection/compression policy.
2. If that succeeds, execute a bounded pruning preservation comparison, independently
   of compression and with matched isolated controls.
3. Report before paper editing. This gate was satisfied; André subsequently authorised
   the writing pass now recorded in [the paper handoff](research/paper-revision-plan-2026-09-18.md#applied-on-20-september--ready-for-andrés-review).

This sequence is now complete within its bounded scope: [three-variant counts](evidence/three-variants-2026-09-20/RESULTS.md)
and [matched pruning observations](evidence/pruning-preservation-2026-09-20/RESULTS.md).
The latter measures 118 admitted scenarios and preserves the one distinct finding;
it is separate from compression preservation. Full-2 runtime preparation and the Full
variants' non-Saga event consumers remain explicit extraction/execution limits.
The formulation/approach terminology and section 4 are now updated in Overleaf, ready
for André's review. The [Tuesday note](reunioes/2026-09-22.md) proposes focusing the main
runtime evaluation on Quizzes, with the Full variants discussed as alternative
implementations requiring further support. André authorised the bounded cross-workload
allocator on 21 September without requiring more GA application executions. It is now
implemented for sequential recorded feedback over complete maps. The six matched variants
isolate model features and the missing-feedback rule. Bounded verifier corrections also
support reassessment from retained evidence. The subsequently approved live dispatcher is
implemented and its bounded real application smoke passes.

The older priority table is broader context, not permission to start more cluster
campaigns. The 21 September authorisation covers bounded allocation, its optional missing-feedback
rule, and the subsequent live dispatch/pause/resume integration. General RL and new GA
application campaigns remain outside this execution package. The bounded live smoke is
complete; agree the next allocation evaluation with André before expanding runs. Paper
updates remain deferred.

## Priorities

| Priority | Next outcome | What establishes completion | Resource |
| --- | --- | --- | --- |
| 1. Resume the paper coherently | Review the rewritten introduction with André, then formulation, approach and evaluation in that order. | Terms introduced before use; consistent workload/scenario/score definitions; figures and results agree. Preserve advisor comments and Reviewing mode. | Writing/review; no cluster. |
| 2. Account for search overhead | Measure candidate selection/bookkeeping and distinguish shared enumeration cost. | Report the bounded local overhead measurement alongside discovery by execution budget; no dedicated campaign on selected-case duration. | Local saved-map measurements. |
| 3. Diversify execution evidence | Finish useful existing maps and test new interaction families selected before their GA results are known. | Complete maps for several different stories, including zero and mixed outcomes; explicit reasons for rejected controls. | Cluster application runs. |
| 4. Deliver the three-variant comparison | Validate missing extraction in Quizzes-full; determine which Full-2 input gaps affect the claimed counts. | Same counting policy for all three variants, with relevant source differences explained. | Bounded generic fixes, then local static runs. |
| 5. Broaden preservation evidence | Compare complete small examples with the orders kept after compression, then address pruning separately. | Finding signatures and co-occurrence compared under the same inputs and fault assignments, including events/removal where executable. | Small cluster studies after enumeration. |
| 6. Evaluate allocation between workloads | Review the matched three-model/two-rule comparison and the revised assessment reference, then design a test of transfer across Saga families. | Independent adaptive UCB, shared progress-only linear UCB and shared structural-plus-progress linear UCB, each with the same optional missing-feedback rule. | Local replay of complete maps; no new application executions. |

Priorities 1–4 can progress independently. The bounded allocation work does not depend on
every gap, more anomaly families, or a dramatic GA win. No new cluster booking,
production change or manuscript rewrite is authorised merely by this roadmap.

## Outcome 6 — Local fault-vector search

Keep the current GA and uniform-random policies fixed for the next comparisons.
Both expose fitness only after a candidate is selected; unavailable scores consume
budget, remain null and cannot become parents. Report confirmed positive scenarios and
accumulated assessed score. Neither is a count of distinct bugs. The historical per-Saga
random baseline is development evidence, not the final comparator.

The local [overhead diagnostic](research/cluster-campaign-2026-09-18.md#local-search-cost-diagnostic)
measures the preloaded search loop, not live end-to-end performance. André chose to
keep evaluation focused on discoveries per execution budget and the search algorithm's
own overhead. The proposed 600-attempt live timing campaign is removed from the queue.
Keep common preparation/enumeration cost explicit where relevant; reuse already recorded
application times for operational planning. Do not claim end-to-end speedup from this
local diagnostic. Variable duration of the selected application cases is not a separate
current evaluation task.

### Preference sensitivity

The first agreed profiles are now evaluated: all five unit weights, deleted dependencies
only, and compensated reads only. See the preference comparison above. Further profiles
should express a user preference before rerunning search over complete saved maps. Merely rescoring the old selected order is
not sufficient. No new application runs are needed when all required observations are
already available. Cases with incomplete enabled criteria stay unavailable. If a criterion
never produces findings in the selected maps, changing its weight has no effect.

Do not simultaneously tune weights, operators and workload selection to improve GA's
headline. Rare positives alone do not guarantee a GA advantage: useful combinations must
help it find further positives after the first hit. Keep unsuccessful and all-zero cases.

## Next evaluation collection: breadth across workloads

The frozen breadth catalogue has **3,820 WorkloadPlans**; 129 selected plans were tested,
not the entire catalogue. Those 129 cover 70 Saga-type combinations under its particular
input/order limits. Of them, 67 had successful exact, fully assessed zero-score controls;
64 progressed to fault collection. The other 3,691 plans are not all proven executable.

The [current all-input static counts](evidence/three-variants-2026-09-20/RESULTS.md) concern
819 accepted Quizzes inputs and sets of 2–4 Saga types. For pairs, pruning/compression
leave 620,567 normal orders. These are **not** 620,567 executable fault scenarios;
fault assignments, recovery, events and runtime setup are separate. There is no verified
global total of executable Quizzes fault scenarios.

Proposed next compute packages, to select with André:

| Package | Initial size | Purpose / launch condition |
| --- | ---: | --- |
| Finish collected maps | 214 large-map + 60 small-map cases | Resume missing identities only, with unchanged runtime and verified storage. |
| Broader pilot | About 8 new workloads × 64 uniform cases, plus controls | Choose across source stories, size and event structure before inspecting fault outcomes. Exact successful zero controls first. Keep all pilot results; no positive in a pilot does not prove an all-zero workload. |
| Preservation | 2–3 structurally different small cases; enumerate before budgeting runs | Reuse known full/compressed subsets and compare observations. Do not launch an unknown exponential domain. |

A pilot can guide which maps are feasible to complete, but record that selection and
include unfavourable outcomes. Do not pool hundreds of tiny maps into one fictional GA
workload to manufacture rare positives. Cross-workload pooling is the allocator's study.

**Storage is currently the cluster gate.** The VM disk and archives share a 100 GiB host
account quota even when the host filesystem has free space. Obtain sufficient authorised
storage or verify and relocate existing evidence before another campaign. Budget bytes
per attempt as well as workers/minute. Preserve resume identities and backup/halt guards.

## Static-analysis and compression evaluation

Quizzes all-eligible-input counts are complete. The
[98-to-40 compression study](evidence/compression-preservation-2026-09-18/RESULTS.md)
preserves all compared finding signatures, co-occurrence and final-state projections for
its update/query pair. Broader pruning preservation has not thereby been established.
A dedicated static runtime/memory benchmark is not required for the count-reduction claim.

For pruning, begin with a small audit rather than executing every rejected combination.
Freeze a source/input population and compare the combination sets with pruning on/off;
hold scheduling constant and disable compression for this test. Select a few small
discarded pairs by structural reason, plus a retained interacting pair as a control.
After equivalent preparation and successful normal controls, enumerate their fault and
recovery cases and compare findings with each Saga executed alone under matched faults.
A positive discarded pair is not automatically a missed interaction: it may reproduce
a single-Saga defect. Distinguish a new interaction-dependent finding from isolated
findings occurring together. The intended claim about preservation of those combinations
must be agreed before making a stronger equivalence claim in the paper. Enumerate and
estimate storage before assigning cluster compute; the 98-to-40 evidence does not cover
this pruning audit.

The [first pruning audit](evidence/pruning-audit-2026-09-19/RESULTS.md) reproduced
666 input-bearing Saga pairs: 248 retained and 418 discarded. Among the discarded pairs,
67 have a potential event-mediated connection and six a compensation-access connection.
The generic selection correction now includes bounded one-hop event and recovery surfaces
without attributing an event receiver to the producer input. Fresh post-fix generation
retains all 16 UpdateStudentName/FindTournament plans, still prunes both unrelated-query
plans, and preserves all six direct UpdateTournament/FindTournament plans and IDs. The
three regenerated catalogues still contain 170 scenarios including 24 controls. Prioritize
receiver/setup qualification, the no-same-source recovery case and matched isolated
controls before a larger preservation campaign. This fixes the structural omission; it
does not yet establish runtime outcome preservation or lost harm.

The original event pair has no eligible Tournament participant. An extended ordinary test
now provides membership through source-derived setup; two selected delivery/query orders
and all six faulted variants have complete zero scores. Exact event-writer attribution also
closes the known read gap in the existing AddParticipant/name-update/query controls.
The unrelated-query pair's two normal orders have complete zeros. Next qualify other event
families against the new assessor, and prepare recovery-only/matched isolated controls;
this zero-only example does not establish general preservation or a missed harmful outcome.
The fresh selection-only count admits 316/666 input-bearing Saga pairs; schedule compression
counts still require a recount using tuple-applicable anchors. See
[qualification and counts](evidence/event-read-attribution-2026-09-19/RESULTS.md).

A follow-up anchor review found that connectivity-minimized evidence had also been reused
for compression. The corrected split keeps minimal WorkloadPlan evidence but schedules from
the complete tuple-applicable anchor set. The independent two-by-two recovery probe now
generates and accounts for all six orders (previously two generated versus six accounted),
and mixed direct-plus-indirect and multi-step event cases have focused regression coverage.
A second review probe confirms that `BRUTE_FORCE` retains those anchors inside a disconnected
three-Saga set while interaction pruning still rejects the disconnected participant set.

The advisor requires **Quizzes, Quizzes-full and Quizzes-full-2** differences and comparable
counts. See [subject inspection](research/rq1-subject-inspection.md): Full misses accesses
from direct command-handler switch calls; Full-2 has unsupported setup helper shapes.
Fix or delimit what affects the counts before treating missing extraction as a reduction.
Do not change the applications to fit the analyser. Their shared domain limits independence;
all-three runtime execution is not automatically required by the counts requirement.

The [workload unblock report](evidence/workload-unlock-2026-09-18/README.md) distinguishes
already regenerated event packages from the still-open event-origin read attribution gap.
That generic gap is a useful candidate if it unlocks event-oriented evaluation; it is not
an instruction to broaden the score silently. Prepare its exact verdict contract first.

## Outcome 7 — Prioritize across workloads

The first bounded allocator is implemented. Each decision gives one execution-equivalent
attempt to one nonexhausted workload, whose persistent population-8/mutation-0.3 GA selects
one unseen scenario. Reward is the newly observed configured weighted score. Unknown fitness
consumes the budget without a zero label, GA parent, or adaptive update. The policies are
balanced round-robin, uniform workload choice, and three adaptive models: independent UCB,
shared progress-only linear UCB and shared structural-plus-progress linear UCB. Each adaptive
model has a raw and a guarded variant. The guarded variant excludes a null-returning
workload for one completed decision when another remains active, without changing reward or
model updates. The raw policy stays available as a diagnostic comparator; neither contextual
variant is yet established as best.

The shared policy uses Saga membership, Saga pairs, referenced interaction/access structure,
scheduled events and already observed progress. It receives no workload-ID feature, unseen
score or known positive density. Evaluation uses several complete retained maps with zero,
mixed and unavailable outcomes, reporting allocation, confirmed discoveries,
cumulative score and combined outer-allocation/local-search bookkeeping overhead under one
matched global budget. Keep related input/schedule variants grouped when separating
development and evaluation cases. This is a bounded contextual-bandit study, not a synonym
for full reinforcement learning or permission for a paper claim before results are reviewed.

The [26 September expanded replay](evidence/workload-master-inventory-2026-09-25/replay-multiseed-2026-09-26.json)
is complete for 172 qualified workloads, six objectives, seven variants and five seeds
at 8,000 choices. A common seeded tie rule is now implemented and checked in the prospective
`seeded_replay.py` entry point. The [28 September formal replay](evidence/workload-master-inventory-2026-09-25/formal-replay-2026-09-28.json)
completes those new comparisons: 420 audited runs across the full and three-giant-excluded
collections, all six objectives, seven policies and five seeds, with matched GA prefixes.
The [independent-UCB addendum](evidence/workload-master-inventory-2026-09-25/ucb-addendum-2026-09-28.json)
now adds the missing simple baseline in 60 matched runs. The
[measurement diagnosis](evidence/workload-master-inventory-2026-09-25/measurement-diagnosis-2026-09-28.json)
separates execution failures, copied-update provenance and competing-writer attribution.
André approved beginning the staged controls on the same frozen inventory. The first
entry point, `allocator-expanded-replay/hybrid_exploration_control.py`, varies only the
two hybrid exploration multipliers to match an initial bonus of one, using both cohorts,
six objectives and the existing five seeds (120 additional runs). That control and the
80-run corrected-measurement replay are complete. The next approved comparison is
`allocator-expanded-replay/order_replay.py`: S/SO, SP/SPO and H1/H1+O on the corrected
maps, joint objective, five seeds, 30 new runs. It retains the original alpha settings;
the exploration control does not select parameters. Review its audited results before
designing recent-progress or fraction-visited controls; progress remains unchanged.
Observer extensions use separate references and representative execution,
preserving this frozen comparison; updated measurements will require a new common replay.
The [28 September audited inventory](evidence/workload-master-inventory-2026-09-25/master-inventory-2026-09-28.json)
adds 144 complete Quizzes maps after same-VM resume, giving 316 qualified workloads
and 18,858 scenarios (6,396 without the three giant workloads). This is an inventory
expansion and now the input to the formal recorded-feedback comparison; historical
and new runtime/measurement provenance, including effective CPU quota, still needs review.
Before a manuscript claim, review schedule representation: three dominant maps alias
under unordered structural features, and
two one-seed priority controls show material score sensitivity. Preserve the exploratory
traces; do not silently replace their policies. Common-runtime cluster validation remains
the next measurement gate; measure overhead on a separate smaller live sample. Rare-criterion
discovery time and family coverage accompany score; historical-map scale alone does not
establish diversity or generalization.

The [first comparisons](evidence/contextual-allocation-2026-09-21/RESULTS.md) are complete:
two cohorts, two weight profiles and 30 seeds. The contextual policy improves on uniform
workload choice but does not consistently beat independent adaptive allocation. The
missing-feedback guard prevents consecutive monopolization by unknown results but costs
discoveries in the mixed all-criteria case. Review this tradeoff with André before further
policy changes. Next implementation work is live evaluator dispatch and resumable search
state; next scientific evidence should isolate structural transfer across Saga families,
rather than repeat the same dominant map. Neither step is implied by recorded replay alone.

The subsequent [six-variant study](evidence/allocator-factorial-2026-09-21/RESULTS.md)
applies the same guard to all three models and repeats affected comparisons after pure
reassessment. It resolves 184 full scores without application runs, leaving 218 incomplete
recoveries, 32 intervening-writer read assessments and 16 overlapping event/Saga residual
assessments in the mixed cohort. The guard reduces discoveries for all three models in
both mixed-cohort profiles; progress-only sharing is competitive with structural context.
Review these results before parameter tuning or choosing a default. No policy default was
changed on the strength of this comparison, and no new cluster campaign is needed to inspect it.

The authorised [order/hybrid and preference study](evidence/allocator-order-hybrid-followup-2026-09-21/RESULTS.md)
is complete and independently verified. Its eight methods, two unchanged collections,
two fixed reward profiles and five budget checkpoints separate added order information
from per-workload model parameters. The combined hybrid/order variant adds essentially
no final impact over the hybrid in these collections. The preferred allocation method
changes with the reward: in the 66-workload collection, progress leads with all criteria,
whereas structure finds all six deleted-dependency positives under the second profile;
in the 15-workload collection progress instead leads for compensated reads. The complete
paired comparisons preserve losses as well as gains.

Next, following the 22 September meeting: inspect the semantic and aggregate differences
in discoveries, verify implementation and comparison fairness, then expand workload
coverage if those checks pass. Keep the two-level GA/allocation approach and compare
meaningful simpler alternatives; do not turn the best method observed in each collection
into an assumed deployment rule. Develop the fuller explanation before reducing the paper
to its page budget. A new default and further model tuning remain separate decisions.
Existing live dispatch is implemented and smoke-verified; the additional
order/hybrid variants remain experimental adapters rather than new live modes.

## Specific open follow-ups

These are conditional candidates, not simultaneous assignments. Select one only when it
unlocks a named evaluation case or resolves an interpretation problem.

| Area | Remaining work and boundary |
| --- | --- |
| Input preparation | Unsupported constructor/HashSet representations, scalar property collections, helper shapes and missing source contexts. The 26 September shared-setup fix retains exact feature prefixes despite shared argument coverage and prevents shorter fixture fallback across preparation barriers. Earlier selected operations stay measured, and deliberately composed cross-feature workloads retain their coherent common fixture. The two Quizzes event stories regenerate the same seven qualified workload IDs. The 176-workload audit excludes four diagnostics; all 17 changed/ambiguous recipes were requalified (85 scenarios, unchanged assessments), admitting 172 workloads to recorded replay. Thirteen use explicit standalone fixtures and four updated prefixes. One removal prefix required explicit expansion of an omitted unassigned createQuestion helper; automatic helper extraction and the rejected asynchronous prefix remain separate follow-ups. Historical measurement groups still need common-runtime validation. Diagnose an exact case before extending the recipe model; keep identities, types and ordered mutations explicit. |
| Synchronous UpdateQuestionTopics | The `each` closure extraction gap and iteration/accumulation semantics remain separate work. A supported ordinary application test may supply a smaller route than generic loop expansion. |
| Event breadth | Original event-reaching triples still need coherent receiver preparation and regenerated qualification. Indirect one-hop event relationships are now retained by generation, but actual receiver identity and runtime preservation still require qualification. |
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

## Adopted scoring boundaries

No LLM harm inference, mandatory developer correctness oracle, blanket invariant replay
or automatic future-operation probes. A rejected operation alone does not earn impact
points. Existing observations report supported conditions, not universal business harm.
Keep coverage explicit and preserve unavailable measurements. These are adopted boundaries,
not pending implementation tasks.

## Deferred breadth

TCC, stream/gRPC parity, genuinely parallel application execution, delay/non-binary fault
models, compensation fault injection/retry policies, multi-host package writing and generic
support for all framework patterns are outside the immediate Saga/local thesis path.

## Roadmap decision rule

Before adding a stage, metric or abstraction, identify the thesis question, the decision
its result changes, representative positive/negative evidence and whether an existing
mechanism already supplies it. Keep an extension only when that benefit justifies its cost.

## Allocation decision applied (21 September)

On 21 September André authorised progress toward choosing workloads across the application
without waiting for the Full/Full-2 extractors. That decision scoped the first allocator to
supported workloads and retained maps.

The implemented boundary is a shared execution budget, a persistent search session per
selected workload (population, seen candidates, RNG and history), and an outer policy choosing
which workload receives the next single attempt. Returning to a workload resumes its search.
Comparators are balanced round-robin, uniform workload selection and independent adaptive
UCB; the shared model is a transparent linear-UCB contextual policy. Reward uses only newly
observed configured weighted scores. Unavailable scores remain a separate observation and
cause no model update. The explicit cooldown variant changes only next-decision eligibility:
after null feedback, that workload waits for one other completed attempt when an alternative
is active. The raw policy remains the diagnostic comparator.

Evaluate using several complete retained maps, including zero and mixed workloads,
with results revealed only when selected. This establishes a bounded allocation experiment,
not coverage of every workload of the application. Application-scale use also needs lazy
workload admission/qualification and explicit enumeration bounds; it must not initialise a
GA or run a control for millions of workloads before any useful search can begin.


### Shared structural allocation

The implemented model shares learned parameters across workloads using extracted Saga
membership, Saga pairs and interaction structure, together with observed search progress.
This should let observations in one workload inform a related unmeasured workload;
independent per-workload reward averages are a baseline, not the full intended model.
The recommended guarded variant for missing-feedback evaluation keeps that model intact and
does not impute zero or add an availability reward. It is a robustness choice, not evidence
that the policy is optimal.

The CLI currently supplies complete retained observations sequentially through an evaluator
boundary. Live ScenarioExecutor dispatch, lazy workload admission and application-scale
qualification remain future work.
