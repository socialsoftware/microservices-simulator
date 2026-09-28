# Paper revision after the 18 September meeting

This is an editorial and evaluation plan, not approval to change production code.
The latest handoff below supersedes the earlier editorial scope and pause instructions.
The first manuscript revision below was authorised by André and applied in Overleaf
in Reviewing mode. Advisor comments were neither answered nor resolved.

## Latest agreed review — 20 September evening (resume here)

21 September: André approved the semantic explanation of unequal contributions and
requested the actual counts in its example. Applied three local tracked edits to 4.2:
added a Combinations column for all three configurations; updated the table reference;
added the explanation that multi-aggregate, multi-step operations provide both potential
interactions and many action orders. The example explicitly compares 3,780,972 workloads
for tournament creation + course-execution removal with 2,975,790 from all 350 discarded
pairs together. Section 3.3 already explains how participants/inputs/orders define a
workload, so that definition was NOT repeated in Evaluation, per André's annotation.
The pending-edit statement below is superseded by this entry.

Pending reader clarification before the next paper edit: distinguish a set of Saga
types from workloads formed using its inputs and normal orders. André wants the set
reduction included because global workload totals obscure it. Independently summed
the current `fault-counts-2026-09-20/*-sets.jsonl.gz`: pruning keeps 316/666 pairs,
3,223/7,770 triples and 27,073/66,045 quadruples. Every kept row has the same workload
count before/after pruning in this dataset. For pairs, the 316 kept sets already
contribute 18,029,104 workloads before pruning; the 350 removed sets contribute only
2,975,790 (total 21,004,894). A single kept pair, RemoveCourseExecution + CreateTournament,
has 234 input combinations and 3,780,972 workloads, more workloads than all 350 removed
pairs combined. GetCourseExecutionById + FindTournament contributes 2,958 input
combinations and 5,916 workloads, all discarded. These are static counts, not executed
or confirmed interacting objects. Use this unequal contribution to explain the result;
do not imply removed combinations are recreated or expanded by pruning. No manuscript
edit for this clarification has yet been applied.

Follow-up accepted by André: restore all three reduction configurations in the count
table (none, pruning, pruning + compression); the before/after-only table hid pruning's
contribution. Applied a compact nine-row, four-column table with the same verified
counts. Rephrased the pruning experiment around its question: does combining independent
Sagas reveal a finding absent when each runs alone? The course query is paired with
a tournament query or update, and each Saga is also executed alone. The text now makes
the denominator explicit: 106 pair scenarios plus 12 individual scenarios; pruning
discards the pairs, while the update's residual finding remains in its individual
executions. Matched final-state comparison and the 38-to-two positive count remain.
This supersedes the three-row table described below. Edits are confined to 4.2;
preserve André's intervening edits elsewhere and leave 4.3 for his next review.
Live source matches the six intended local replacements. Recompiled PDF pages 7–8
were visually checked, with no clipped text or table content. Current snapshot:
`output/pdf/paper-2026-09-20/article-pruning-clarified.pdf`.

Latest user review: section 4.2 was too dense; the count table plus figure occupied
too much space; the preservation table was confusing. Paper limit is **10 pages
excluding references**, with Related Work and earlier-section review still pending.
Applied a further bounded 4.2 revision: removed its figure and preservation table from
the manuscript (assets/evidence kept), replaced the nine-row totals with a three-row
before/after table for workloads and fault scenarios, and shortened the prose from
about 548 to 313 whitespace-delimited words. The pruning-only result is one sentence.
Preservation uses the matched execution stories and essential 118-to-12 / 98-to-40
results, retaining the difference between fewer positives and preserved findings.
No 4.3-or-later source was changed in this pass; LaTeX renumbers the former Table 3
(criterion comparisons) to Table 2. Explained "control" to André as an execution
expected not to exhibit the particular finding; proposed the clearer column heading
"Execution without that finding", not yet applied pending his 4.3 review.
Source and cross-references were verified; compilation has zero errors and 16 warnings
(the additional warning concerns final-column balancing). Pages 7–10 were visually
checked. The PDF is still 10 body pages plus references due to text and float placement;
do not claim the full paper budget is solved. Snapshot:
`output/pdf/paper-2026-09-20/article-evaluation-compact.pdf`.

André is reviewing the paper block by block. The preceding writing pass is NOT an
accepted final style. Read the live Overleaf source before editing; preserve his edits.
The agreed next step is count-only support for fault scenarios, including events and
recovery, through sets of three and four Sagas. See
[fault-count work](fault-count-only-2026-09-20.md). Resume writing when the new counts
or their demonstrated feasibility are available, using the best verified evidence.

The counting follow-up is now complete: [new totals](../evidence/fault-counts-2026-09-20/RESULTS.md)
cover all 819 accepted inputs and 2–4 Saga types with supported event selections and
recovery. They provide the proposed workloads/fault-scenarios table. All three profiles
completed locally, so there is no cluster prerequisite for returning to writing.
Pruning plus compression reduces fault-scenario counts by 79.50%, 79.50% and 70.82%.
Use this result instead of relabelling the older event-free order totals. Manuscript
The full Evaluation revision below is now applied, following André's request to
finish all its sections in the same pass.

### Full Evaluation revised in Overleaf — 20 September

André then explicitly authorised completing the whole evaluation ("faz logo tudo").
The live source was read again. Changes were applied as local sentence replacements
and additions in Reviewing mode; the superseded count table was replaced as a block.
All source preceding Evaluation, including his introduction and front matter, remains
byte-for-byte unchanged. Advisor comments were not answered or resolved.

- Quizzes gives the input/Saga scale and clarifies that selected event consumers add
  operations to the 2–4 explicitly invoked Saga types.
- RQ1 now uses the verified fault-count evidence: a nine-row workload/fault-scenario
  table (five significant digits) and a new figure showing percentage reductions.
  The old input-combination and event-free order counts are no longer used there.
  Shared inputs, supported event scope, combinatorial counting and the 18 enumeration
  checks are explained briefly. Generated candidates are distinguished from executions.
- Preservation describes matched observations before fewer positive executions, keeping
  the pruning and compression comparisons separate. The 118-to-12 and 98-to-40 results
  remain, with their finding/state comparisons and bounded evidence.
- RQ2 simplifies the controls and lost-update example, preserves the generated event
  findings, the 15-workload cohort and unavailable assessments, and removes repeated
  conclusions that added no information.
- RQ3 now orders its method as workloads, execution/revealed feedback, search settings
  and weights, then discovery/score measurements and results. It keeps the three large
  maps, 30 paired seeds, variability figure, 80% discovery metric, small/flat results
  and the unfavourable tiny comparison. W3 deviations and repetition evidence appear
  once in Threats to Validity rather than interrupting the comparison.
- Limitations cover the application/input scope, bounded preservation comparisons,
  saved execution outcomes, unavailable scores, schedule deviations and unmeasured
  execution time/selection overhead. No RL implementation or speedup is claimed.

New figure source: `evidence/fault-counts-2026-09-20/paper-figure.py`; uploaded asset:
`quizzes-fault-reduction.pdf`. The existing search figure is unchanged. The final
editor source matched all intended replacements; compilation reported zero errors
and the existing 15 warnings. Evaluation pages 7–10 were rendered and visually checked.
PDF snapshot: `output/pdf/paper-2026-09-20/article-evaluation-revised.pdf` (ignored).
Next: André reviews the completed Evaluation; do not treat this writing pass as his
acceptance of every wording choice. No further evaluation rewrite or compute is pending
merely because the older handoffs below describe unfinished work.

### Earlier opening-only pass — superseded by the full revision above

The first resumed writing block is applied in Reviewing mode. Experimental Setup is
now **Quizzes**, with the existing introduction reference and a short statement of
68 Saga types, 819 extracted inputs covering 37 types, and workloads with 2–4 distinct
Saga types. The Evaluation/RQ opening is preserved. The saved-result method, fresh
state, budget and no-fault admission paragraph has moved to Search within a Workload;
its obsolete reference back to the setup subsection was removed. The repeated input
totals in RQ1 were replaced with a reference to the inputs described above.

The live source matched the intended local replacements; compilation had zero errors
and the existing 15 warnings. User edits, front matter and advisor comments were
preserved. Next review block: replace RQ1's old input-combination/normal-order table
and figure with the verified workload/fault-scenario counts above, then refine its
explanation and preservation results. Those old counts are still in the manuscript;
do not claim the new table has already been inserted. Review search prose separately
later; this round relocated its method without rewriting that section.

### User edits observed directly in Overleaf

Compared with the retained earlier manuscript source, André added
`\label{sec:Introduction}` and replaced a literal Section 1 reference with
`\ref{sec:Introduction}`. He removed the repeated description of Quizzes operations,
removed “predefined” before executions, simplified “combinations and action orders”
to “scenarios ... under different configurations”, and removed “under a fixed clock”.
The subject and evaluation outline are now one short paragraph. Fresh application
state is a short sentence preceding the search-method paragraph. These edits are
examples of desired economy; do not restore the removed descriptive clutter.
The latest browser read showed no further change since that review. Read live again
next time; this is a record of observed edits, not permission to overwrite newer ones.

### Agreed structure (applied; awaiting André's review)

- Open Evaluation with a short paragraph linking the three RQs.
- Replace Experimental Setup with a short **Quizzes** subsection. Give relevant scale
  (68 Saga types; 37 with accepted inputs; 819 inputs, pending any newer extraction),
  rather than explaining the domain again. Avoid the title “The Quizzes space”.
- RQ1: reduction and preservation. Keep workload/fault-scenario counts by Saga-set
  size beside the before/after comparison; do not duplicate them in the preamble.
- RQ2: detection of persistent effects and anomalies, with concrete cases and controls.
- RQ3: search within a workload. Move saved execution results, how feedback is revealed,
  seeds, budget, curves and search admission here. They explain this experiment, not
  the whole evaluation. Describe each comparison immediately before its results.
- End with material threats to validity, without repeating defensive disclaimers.

### Terminology and presentation decisions

A workload fixes invocations, inputs and one normal order (including selected event
deliveries). A fault scenario adds fault choices and recovery ordering. A combination
of invocations/inputs before choosing an order is only an intermediate construction
stage, not another synonym for workload. Prefer workload and fault scenario in tables,
figures and prose; do not rename them “action orders”, “execution structures”, or
“scenario space” for variation. Explain the ordering mechanism where needed without
turning it into another name for the counted object. Do not call all counts scenarios.

The older global counts describe workloads WITHOUT event-placement expansion; they
are not global fault-scenario totals. The new count-only implementation should supply
an understandable table: Sagas per workload | reduction applied | workloads | fault
scenarios, comparing none, pruning, and pruning plus compression with identical inputs
and event policy. Count-only is combinatorial, not millions of application executions.
Preservation evidence answers a different question and remains beside these counts.

Write final results, not bug-fix history. Introduce terms before use, reuse them exactly,
avoid adjective stacking, unnecessary setup parameters and synonym substitutions.
The story is: what we analyse → how much we reduce → what we detect → how search
finds it earlier. Review one coherent block with André at a time, with small realistic
tracked edits, no advisor-comment replies/resolutions and no front-matter removal.

## Applied on 20 September — ready for André's review

André authorised resuming manuscript edits and requested small, realistic tracked
changes rather than replacing whole paragraphs for minor wording changes. The local
style guide now records that instruction. The [22 September meeting note](../reunioes/2026-09-22.md)
contains the proposal to concentrate the main runtime evaluation on Quizzes, with the
Full variants' structural differences and support gaps available for discussion with
the advisor. This is not a claim that the Full variants lack interesting behaviours.

Applied in Overleaf, preserving front matter and all advisor comments:

- Formulation and approach consistently use workload for invocations, inputs and one
  normal order. Removed the extra plan notation q and updated dependent references.
- The five criterion counts and their weighted sum are now the primary score model;
  units, deduplication and per-criterion coverage remain explicit. No detector changed.
- Section 4.1 centres the evaluation on Quizzes and explains saved-result search and
  the no-fault control. Sections 4.2–4.5 were updated through local sentence, value,
  table-row and caption edits, preserving the existing explanations where applicable.
- Generation uses the final 819-input counts: 8.62%, 3.85% and 16.88% of normal orders
  remain after pruning/compression. The pruning experiment (118 to 12) is explained
  separately from compression (98 to 40), with finding preservation before the
  reduction in positive executions.
- Assessment adds generated Topic/event evidence and the 15-control diversity cohort.
  The lost-update findings remain distinguished from unavailable overall scores.
- Search reports all three large workloads and three preferences, a two-panel W1
  figure, 30-seed variability, positive discoveries, accumulated scores and selections
  to 80%. Small, flat and unfavourable results remain in the discussion. RL is described
  as a proposed extension, not a completed experiment.

Verification: the final editor contents matched the intended individual replacements;
front matter matched the initial source exactly. Overleaf compiled with zero errors.
The two generated figures and compiled pages were visually checked. Existing bibliography
and template warnings remain, plus a 2.34 pt paragraph overfull warning with no clipped
content. Figure reproduction: `evidence/weight-preferences-2026-09-20/paper-figures.py`.
Local PDF snapshot and figures: `output/pdf/paper-2026-09-20/` (ignored local artifacts).
No source-code, experiment or scoring changes were made during this writing pass.

Next: André reviews the edited formulation and section 4; then agree the cross-workload
allocator's decision unit, reward and baselines. Further GA collection is not a prerequisite.
The older sections below are historical handoffs, not current pause instructions.

## Approved handoff on 20 September — pause before manuscript edits

André approved using **workload** consistently for invocations, inputs and one normal
order, and presenting the five criterion counts and weighted score as the main model
instead of introducing I/A first. Apply these together in formulation, approach and
references after the implementation/evaluation work below; preserve advisor comments.

The agreed evaluation revision is: (4.1) explain the roles of generation, preservation,
detector controls and search, distinguishing the three large orders of the same four
Sagas from other stories; (4.2) replace old pruning/compression counts with current
three-variant measurements, retaining the scoped 98-to-40 preservation result;
(4.3) keep the five-criterion/control table and add concrete Topic/event lost-update
and diverse-workload evidence, separating findings from complete fitness and schedule
conformance; (4.4) compare the three large workloads and declared all-five,
deleted-dependency and compensated-read preferences, with paired seed variability,
positive discoveries, accumulated score and selections to 80% of known positives.
A clear main figure contrasts all-five versus deleted-dependency in the 3,360 map;
use a compact comparison table for other maps/profiles and keep full results supporting.
The rare-positive result is 90.07 vs 44.47 positives at 1,000 selections and
1,822.10 vs 2,671.40 selections to 80%, over 30 seeds. Keep small/flat/unfavourable
results and explain that complete enumeration makes final endpoints equal.
Update limitations to actual evidence; do not describe RL evaluation as completed.
The authoritative results are in evidence/weight-preferences-2026-09-20/REPORT.md.

**Current execution order, explicitly authorised by André:** fix the Full command
switch extraction and the necessary Full-2 test-input patterns, then perform matched
counts for all three Quizzes variants using current pruning/compression. If validated,
follow with a bounded pruning preservation experiment, keeping compression disabled
and comparing discarded combinations against matched isolated scenarios. Stop and
report before any Overleaf edits. No new cluster reservation or scoring/GA/RL change.
Keep compact static artifacts and respect limited local storage. This replaces the
older immediate editorial sequence below while preserving the writing proposal.

Implementation/evaluation handoff: the [three-variant recount](../evidence/three-variants-2026-09-20/RESULTS.md)
is complete, with current inputs and anchors. The [new pruning experiment](../evidence/pruning-preservation-2026-09-20/RESULTS.md)
measures 118 admitted scenarios versus 12 kept, preserving one distinct finding and the
compared domain states. In its positive group, 106→8 scenarios and 38→2 positive executions
must be explained together with 1→1 finding signature. Do not generalize beyond the measured
pairs or merge this claim with the earlier compression result. Both Full variants have
unresolved event routes, and Full-2 preparation remains unsupported; state the extraction
scope when using their counts. No manuscript edits have been made in this execution package.

## Current handoff

The rewritten introduction still needs André's review. Resume with its transition into
Problem Formulation, then Approach, then Evaluation; do not restart the manuscript or
copy the writing task's older draft over the current Overleaf source. The workload/order notation and five-criterion presentation follow the approved
20 September decision above; their manuscript edits remain paused until this
implementation/evaluation sequence is reported. Update figures and captions with each reviewed block.

André's local editorial guide is `.local/paper-style-guide.md`, excluded through Git's
local `info/exclude`. The task **Paper — avaliação e RQs** has been instructed to follow
it and wait for André before editing. It condenses the agreed style into one short guide;
the canonical handbook continues to own technical definitions. No Overleaf edits were
made during the 19 September roadmap review.

Use the current [roadmap](../roadmap.md) for pending experiments and the
[cluster report](cluster-campaign-2026-09-18.md) for verified new results. Writing can
advance before further compute: use current supported claims, and add numerical results
only after their comparison is complete. Do not narrate implementation fixes or turn a
temporary missing result into a permanent limitation of the proposed method.

## Applied revision

- Rewrote the evaluation opening around the experiments and their purposes. Introduced
  all three Quizzes variants with concrete differences in participant-addition and
  tournament-update steps. The reported measurements still explicitly concern Quizzes;
  comparable Full counts remain pending, with a source-only editorial reminder.
- Updated the reduction table, explanation and figure together to all 817 accepted
  inputs. The main fractions of normal orders remaining are 7.88%, 3.50% and 16.60%
  for two, three and four Sagas. Smaller input limits now illustrate sensitivity.
  The figure is reproduced by
  [paper-figure.py](../evidence/generation-all-inputs-2026-09-18/paper-figure.py)
  from the saved summary, and uploaded as `global-selection-compression-all-inputs.pdf`.
- Put preservation of the compared observations before the 98-to-40 reduction.
  Explained why 36-to-12 positive executions does not mean a distinct observed outcome
  was lost. The 40 are a subset of the 98 executed scenarios, not another runtime campaign.
- Simplified the search comparison and captions: all 5,184 scenarios of one workload,
  results saved once and revealed on selection, positive discoveries and accumulated
  score. The measured GA results, weights and algorithm are unchanged.
- Shortened the approach overview and the explanation of generated records and reports.
  Clarified that a workload plan fixes inputs and a normal schedule; linked the existing
  mathematical pair to references to a fixed workload. Did not rename notation or
  change the I/A and five-criterion definitions.
- Removed repeated uses of “retain” by naming the action (record, save, keep, read,
  select). Reduced repeated scope qualifications and defensive conclusions. Preserved
  title/front matter, references, detector semantics and advisor comments.

Validation: source read back through the editor and compared against the intended edits;
front matter unchanged; Overleaf recompiled with zero errors. Existing template and
bibliography warnings remain. The new plot was inspected before upload.

Remaining work: qualify and add comparable counts for the two Full variants; review
the more substantial notation simplification and the five-criterion presentation with
André; add runtime breadth only when measured. The sections below preserve the broader
plan and proposals, rather than claiming that every proposed revision is complete.

## Agreed direction

### Introduction pass agreed with André

The introduction was rewritten in Overleaf after André requested a complete first
pass to review together. It now introduces Saga execution and compensation, briefly
explains event publication and later processing, then uses the tournament example.
Anomalies and persistent effects motivate testing; the choices being tested introduce
the term **fault scenario** before the proposal, configurable score, GA and RQs.
Workloads, cross-workload allocation and class names are deferred. The DDD/aggregate
definition moved to the opening of Problem Formulation, where it is first needed.
The figure, front matter and advisor comments were preserved. Source read-back matched
the intended edits exactly; compilation reported zero errors and 16 existing warnings.

The editorial rule for subsequent passes is to build the reader's vocabulary in order:
explain a concept, name it, then reuse that name. A scenario specifies a test; an
execution runs it; a finding is a detected occurrence with supporting observations.
Avoid swapping these names for synonyms, and explain reduction mechanisms before
using their names in research questions or results. This pass awaits André's review;
it is not approval to rewrite the remaining sections wholesale.

### Whole-paper direction

- Include Quizzes, Quizzes-full and Quizzes-full-2. Explain their relevant differences
  and compare their counts under the same declared analysis policy.
- Assume the named application is the scope unless a smaller scope is specified.
  State generation bounds once. Avoid repeatedly saying “application-wide”.
- Write about the final method and its results. Development history belongs in
  implementation evidence, not the paper's evaluation narrative.
- Use British English and the PIC's problem-first exposition: explain the action and
  its purpose, introduce terms before notation, and use a concrete example to support
  each difficult distinction.
- Revise in coherent blocks with André. Avoid a global synonym replacement or another
  unreviewed rewrite of the entire paper.

## Main diagnosis

The manuscript mixes domain explanation, mathematical definitions, implementation
contracts and experimental restrictions within individual paragraphs. Shorter words
alone will not solve that burden. Each section needs a clear job, and each concept
needs one name.

The inspected source contains 29 instances of “retain” and its derivatives. They mean
several different things: selecting workloads, reducing orders, preserving outcomes,
storing evidence, remembering an earlier read, and keeping GA population members.
Those actions should be described directly. Technical nouns should remain consistent;
we should not introduce synonyms for “workload” merely to avoid repetition.

A concrete inconsistency needs resolution before rewriting: the formulation defines
workload w separately from normal schedule sigma, then creates q=(w,sigma). In the
implementation, WorkloadPlan already contains one normal action order. “Complete
scenario space” in the evaluation obscures the fact that the GA experiment evaluates
all scenarios of one such workload.

## Terminology proposal to review first

| Term in the paper | Meaning | Editorial action |
| --- | --- | --- |
| Application variant | One of the three Quizzes implementations | Explain once that these share a domain but differ in workflows and code/test structure. |
| Input | Concrete arguments for one Saga invocation, derived from a test | Avoid “input-bound” when every workload already has inputs by definition. |
| Workload | Saga invocations with their inputs and a normal action order, including selected event processing | Align prose with WorkloadPlan. Review whether the extra w/sigma/q layer earns its place in the mathematics. |
| Fault scenario | A workload with assigned faults and the resulting order of execution/recovery actions | Use “scenario” afterwards. Reserve “experiment” for an evaluation study and “execution” for running one scenario. |
| Finding | A condition established by a detector, with supporting execution evidence | Different scenarios may expose the same finding or underlying defect. |
| Impact score | The configured weighted combination of criterion counts used to guide search | Define once; explain that the GA uses it as fitness. Avoid alternating score/impact/fitness as if they were three algorithms. |
| All scenarios of a workload | Every candidate in the declared fault/recovery domain of that workload | Prefer this phrase to “complete scenario space”. Specify its bounds once. |

Do not replace “complete scenario space” with an undefined “complete workload”. The
clear sentence is: “We execute all 5,184 scenarios of one workload.” This does not
claim to test every workload in Quizzes.

Also review whether the main narrative needs the historical I/A summaries before the
five criterion counts and their weighted score. The final search uses the latter.
Proposed simplification: make the five counts and their score the main explanation;
keep an affected-object union only where a result or research claim actually uses it.
This would change presentation, not detector or scoring semantics. It needs André's
review before changing notation throughout the manuscript.

## What each section should do

| Section | Reader's question | Content to keep there |
| --- | --- | --- |
| Introduction | What problem matters, and what does this work contribute? | One motivating interaction, the need to test faults and recovery, contributions and RQs. No catalogue accounting or execution-report taxonomy. |
| Problem formulation | What can the tool choose, and what is it trying to discover? | Workload, scenario, budget, findings and configurable score. Introduce only notation reused later. |
| Approach | How does the tool construct, execute and prioritise scenarios? | Follow the pipeline: source/tests, pruning/compression, faults/recovery, execution/assessment, GA. Define each mechanism here once. |
| Evaluation | What happened when we applied the method? | Subjects, essential comparison conditions, measurements, results and their interpretation. Refer back to mechanisms rather than redefining them. |
| Limitations | Where do the evidence and method stop? | Application family, supported observations and extraction, execution deviations/unknowns, and scope of measured workloads. Place a local qualification earlier only when needed to interpret a specific result. |

The upper-level learning method remains an open design task. Do not choose its reward,
algorithm or claims incidentally while simplifying the GA section.

## Proposed evaluation story

1. **Applications and experiments.** Introduce the three variants in one short
   paragraph and a compact comparison table. Name the experiments and the question
   each answers. State common bounds and essential runtime conditions once.
2. **Generation and reduction.** Show counts for all three variants, followed by the
   effect of pruning and compression. Use all eligible source inputs for the principal
   result when each variant has been analysed reliably. Smaller input limits become
   a sensitivity comparison, not the default headline.
3. **Preservation.** Explain the test before giving the reduction: compare the outcomes
   of all scenarios with those represented after compression. Distinguish fewer
   positive executions from a missing distinct outcome. Keep the existing runtime
   result tied explicitly to its update/query interaction; broader pruning preservation
   still needs its own evidence.
4. **Execution and assessment.** Show whether generated cases can be prepared and run,
   then explain detector results through controls. A compact table should distinguish
   workloads prepared, scenarios executed as requested, and scenarios with complete
   assessment. These use different denominators; the current detector controls do not
   establish global executability coverage.
5. **Search.** State the selected workload before describing its 5,184 scenarios.
   Explain the recorded-result comparison in ordinary verbs: execute once, save the
   result, reveal it when selected. Show positives and accumulated score over budget.
   Multiple-workload and weight comparisons enter only when measured.

This order moves from application scope to named case studies. It does not imply that
all detector, preservation and GA experiments have already been repeated on all three
variants. The advisor's explicit three-variant requirement currently concerns their
differences and counts; any broader runtime requirement should be discussed separately.

## Three-variant evidence work

The existing [subject inspection](rq1-subject-inspection.md) already describes relevant
workflow differences. For example, AddParticipant has two steps in Quizzes and four in
each Full variant; UpdateTournament has five, four and six respectively. These differences
change possible interleavings and accessed aggregates. The variants are not three
independent domains or a simple small/medium/large series.

Before using comparable reduction figures:

1. Pin the three source versions and make a small source-to-extraction check of the
   operations used in the explanation.
2. Diagnose and support Full's direct service calls in command-handler switch cases.
   The current extractor misses those accesses; its low interaction count is not an
   application result. Propose a bounded generic fix with fixture coverage rather than
   changing the application to fit the analyser.
3. Separate Full-2 input/setup support from static counts. Helpers can block replay
   without automatically blocking every structural count. Establish which inputs and
   accesses are actually represented before deciding whether a helper fix is necessary
   for the counts or only for executability experiments.
4. Run the same generation/reduction comparison over all three with the same policy,
   Saga-set sizes and input-selection rule. Check zero cap exclusions before describing
   a population as all eligible inputs. Show extraction coverage where it affects the
   interpretation. Do not turn missing accesses or event support into a reduction win.
5. Keep runtime studies scoped to variants/workloads actually executed. Counts alone
   do not establish runtime compatibility, successful setup or equivalent event support.

The new Quizzes results are already available in
[the complete-input comparison](../evidence/generation-all-inputs-2026-09-18/RESULTS.md).
The 18 September revision applied the all-eligible-input table, percentages and figure
together. The exact post-compression order totals remain in the evidence report; the
current manuscript figure shows percentages instead.

## Revision sequence and review checkpoints

1. **Agree a one-page outline and vocabulary.** Resolve workload/order terminology and
   the role of I/A versus the five weighted criteria. This is the next discussion with
   André, not a request to approve every wording change.
2. **Rewrite the evaluation opening and reduction narrative as one sample block.**
   This establishes the desired density and voice. Mark the two variants' missing
   results in the working plan, not as invented numbers or permanent paper conclusions.
3. **Align formulation and approach.** Consolidate definitions, remove repeated
   contracts, and align the overview figure with the same terms and sequence.
4. **Revise the rest of evaluation, then introduction.** Start paragraphs with the
   finding or question the reader needs, followed by supporting method and evidence.
   Update captions and cross-references together.
5. **Read the paper continuously.** Check terms, notation, scopes, numbers, figure/text
   agreement and repeated qualifications. Recompile and inspect layout. Preserve title,
   front matter, Reviewing mode and all advisor comments.

The three-variant technical work and the writing can progress independently after their
boundaries are agreed. No fresh large runtime campaign is necessary to begin editing.

## Example edits, not yet applied

Current:
> The experiments serve different purposes: application-wide counts measure reduction,
> controlled cases validate findings, and a complete scenario space supports the search comparison.

Proposed structure:
> We compare the number of workloads and execution orders generated for the three
> Quizzes variants. We then use selected executions to test the detectors and compare
> genetic search with random search on all scenarios of one workload.

The second sentence must match the final experiment selection and should not imply
that all three variants supplied the runtime studies. A short subsequent sentence can
name Quizzes as the runtime subject when that is the measured scope.

Other changes:

- “Compression retains 40 of 98 scenarios” → “Compression reduces the 98 scenarios to 40.”
- “The retained combinations account for most possible forward orders” → “The combinations that pass pruning contain most of the execution orders.”
- “Retain its outcome” → “Save the result.”
- “A query retains a Topic name” → “A query reads a Topic name, which the update later reuses.”
- “Retain the highest-fitness members” → “Keep the candidates with the highest scores.”

Use a qualifier when it changes the meaning of the result. Do not attach “bounded”,
“complete”, “observed”, “input-bound” and “application-wide” to every noun. Explain
necessary restrictions at their first relevant use; avoid repeating them defensively
throughout the paper.

## Evaluation evidence checklist after the cluster collection

Verified against the live Overleaf manuscript on 19 September. Section 4.2/Table 1 uses
817 inputs and absolute combination counts; Figure 3 reports action-order percentages.
The exact 565,344 orders for two Sagas after pruning/compression are not printed in the
current manuscript. Add a compact absolute-count presentation in the next agreed
reduction-section revision, clearly distinguishing combinations, normal orders and
fault scenarios. No manuscript change was made during this inspection.

| Evidence question | Available now | Next measurement or presentation |
| --- | --- | --- |
| What does each application expose? | Quizzes all-eligible-input count and source inspection of three variants. | Comparable Saga/input/step/access/event counts for all three, followed by the same reduction analysis. Diagnose Full extraction gaps before treating low counts as application properties. |
| How much does each reduction save? | Quizzes combinations and normal-order counts for 2–4 Sagas, all accepted inputs, plus input-limit sensitivity. | Show useful absolute order totals alongside percentages; add the two variants. No new Quizzes run needed for existing totals. |
| Can reduced execution still find the same outcomes? | All 98 update/query scenarios compared with the compressed subset of 40; matched observed findings/states. | Add a structurally different interaction, preferably involving events. This does not yet test outcomes of workloads removed by pruning; a separate matched small pruned/unpruned runtime experiment is needed for that claim. |
| How many proposed cases can actually run and be assessed? | Selected controls, the 5,184 map and cluster measurements; no global denominator. | Use the breadth queue to report preparation, normal application outcome, requested-order conformance and complete-score availability with separate sample denominators. Report application rejection separately from framework failure. |
| Do detectors recognise their intended conditions? | Positive/negative controls for all five criteria in Section 4.3. | Preserve those contrasts; broaden measured occurrence counts by criterion using new workloads. Current maps do not exercise every criterion positively. |
| Does GA discover useful scenarios earlier? | Complete 5,184-case comparison plus nine complete cluster maps, 30 seeds per method. | Integrate the new result after review; collect diverse and sparse-positive workloads before claiming broader generalisation. Keep score and positive-count outcomes. |
| What do configurable weights change? | Implementation and unit-weight search results. | Rerun adaptive search locally under a small declared set of weight choices on suitable complete maps; do not merely rescore the old GA order. No new application execution required if all needed component evidence is available. |
| What does the method cost? | Operational execution/enumeration times and a cluster capacity pilot. | Measure extraction/generation, application execution and search overhead separately; equal-budget discovery curves do not establish live time savings. A small live comparison can validate overhead without repeating whole maps. |
| Are saved outcomes repeatable? | Six selected repeats for the large map and identical results in two repetitions of the 48-case cluster map. | Include selected repeats from new families; seed variation alone does not measure application repeatability. |
| Does allocation across workloads help? | No implemented/evaluated RL allocator. | Separate next implementation/evaluation milestone after the workload inventory supplies candidate structures and feedback. |

Prioritise three-variant counts, breadth/executability and wider outcome-preservation
before adding many more same-family GA cases. Weight experiments and search replay
can run locally; cluster time is principally useful for application executions.
Avoid inventing a total count of executable FaultScenarios for unbounded Quizzes.


## Search results to retain for the paper (20 September)

André explicitly wants the evaluation to show both variability and the number of
executions needed to discover a useful fraction of positives, in simple language.
Preserve the measured sparse case: for deleted dependencies alone, 150 of 3,360
scenarios are positive. Across 30 seeds, finding 80% (120 positives) takes 1,822.10
selections with GA and 2,671.40 with uniform random: about 32% fewer. At budget 1,000,
the means are 90.07 and 44.47. The durable [weight report](../evidence/weight-preferences-2026-09-20/REPORT.md)
and `sparse-case-variability.json` retain the exact figures and seed spread.

Keep the 5,184-case workload as well. Its original all-criteria comparison remains
valid and locally available: 3,979 positives, 657 zero and 548 unavailable; at 1,000
selections GA/random discover 841.17/764.73. Reaching 80% takes 4,051.07/4,148.47,
showing a smaller gain for that target. Do not replace it with the most favourable
sparse example. The same two single-criterion preferences are now being compared
locally; those results are pending and must not be invented.

Suggested compact presentation: explain once that both searches reveal saved results
only after selecting a scenario; use a figure contrasting a dense and a sparse case,
with seed variation, and a small table of workloads/preferences with positives at a
fixed budget and evaluations to 80%. The complete tables/curves belong in supplementary
artifacts. These workload variants share a Saga family, so do not present them as
independent application domains. Keep unavailable scores explicit in each population.

The main question is how early useful scenarios are found for the user's chosen
criteria. Useful additional evidence is another interaction family, preference sensitivity,
and a separate measurement of search overhead. Increasing seed count or rerunning
unchanged application maps is lower priority. Cross-workload learning remains a later
experiment after its joint design; no RL results are claimed here. This is an editorial
queue and evidence pointer, not a manuscript edit.

### Workload diversity and final profile results, 20 September

The three large references (3,360, 3,918 and 5,184 cases) all combine AddParticipant,
LeaveTournament, RemoveTournament and UpdateTournament. Distinguish their forward
orders; do not introduce them as three independent application stories. The final
5,184 profile results and the complete three-map figure are now retained in
`../evidence/weight-preferences-2026-09-20/REPORT.md`.

The extension is now verified: 15 controls, nine complete maps and 189 scenarios
(38 positive, 135 zero, 16 unavailable). It adds propagation, privacy and membership/
removal stories; failed controls and missing event receivers remain explicit. Its
smaller maps broaden functional coverage but do not supply another large GA benchmark
family. Two lost copied updates are detected in scenarios whose global score is
unavailable due to a different criterion, plus one in a no-fault control. See the
evidence report for the exact distinctions.

Use one readable contrast figure, a short table of other complete workloads/preferences,
and full results in supporting material. Select illustrations for clarity without
omitting unfavourable comparisons. No paper text was changed by this collection work.
