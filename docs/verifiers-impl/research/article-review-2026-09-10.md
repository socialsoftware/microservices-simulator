# Article review: comments, impact evidence and search

Working review of [André Silva - Article](https://www.overleaf.com/project/6a54f72420fff871a2fd5707),
read directly in Overleaf on 10 September. This is an editorial proposal, not an approved
change to the scoring policy or implementation. The initial review left the manuscript
and advisor comments untouched. The subsequently authorised writing pass is recorded below.

## Writing agreement recovered from Article writer

- Use the PIC for writing style: British English, problem-first exposition, explicit
  causal explanations, definitions before notation, and concrete examples followed by
  generalisation. Do not copy its old implementation assumptions or its entire structure.
- Keep the intended final-system voice in the manuscript so the advisor can review the
  design. Do not turn it into an implementation progress report.
- Track implemented behaviour, intended design and open decisions in working notes.
  Never invent results or treat a preliminary comparison as evidence of superiority.
- Propose changes in small coherent blocks. Apply approved edits only in Reviewing mode;
  leave advisor comments open and unanswered.
- Preserve the hierarchical research direction. The upper-level allocator is still a
  design task internally; its detailed algorithm and reward must not be fixed by incidental
  editorial wording.

Style reference: André_Silva___IST_UL___MEIC_PIC2.pdf, especially PDF pages 4, 17, 20–21
and 25. The PIC's mixed initial population, forced duplicate mutation, diversity fitness
and Jaeger/log-based impact calculation are historical design choices, not descriptions
of the current implementation.

## Advisor comments read in the current article

The overview contains 13 open substantive comment threads in `acmtog.tex`, plus tracked
edits, including the explicit request inserted after Figure 1. These are newer than the
four-comment cluster discussed in the earlier writing task.

| Attached passage | Advisor comment | Proposed response |
| --- | --- | --- |
| After the motivating example / Figure 1 | Add a description of observable situations, concurrency anomalies, etc., generalising the example | Add one paragraph on stale copies, surviving dependencies, incomplete restoration and reads of later-compensated versions. Define these as observations; explain their relation to final state. |
| Related-work gap paragraph | “Esta frase fica mais forte…” after introducing anomalies, invariants and compensation | Introduce those concepts before comparing the approach to other tools. Verify the broad related-work exclusion claim against primary sources in the later citation pass. |
| “Infrastructure symptoms …” impact paragraph | “Isto é um pouco o que deveria vir logo a seguir ao exemplo” | Move and rewrite its substance immediately after the example, avoiding a second explanation later. |
| Research questions | “Poder-se-ia ter uma RQ mais genérica…” | Add one overarching question; retain three measurable subquestions for construction, observation and efficient discovery. |
| Action occurrence tied to “aggregate state” | “é mais do que um agregate”; explain actions acting on aggregates earlier | Define action targets/access sets and distinguish an individual action's state access from a Saga's interaction with multiple aggregates. Avoid using an unspecified singular state as the whole execution context. |
| “inter-Saga aggregate conflicts” | Possible conflicts or actual conflicts? Invariants? Anomalies? Interest is consistency relations between aggregates | State that source analysis derives potential conflicting accesses. Execution evidence establishes a particular interaction; persistent checks and read diagnostics assess different observations. Explain local invariants versus cross-aggregate relations. |
| “selected event consequences” | Are these actions triggered by event processing? | Define emission, selected consumer route, eligibility, delivery and resulting application processing before using the term. Explain multiple selected routes and atomic consequence boundaries. |
| Setup excluded from schedule, fault assignment and impact | “Não é claro para mim…” | Use a concrete example: create course/student/Quiz/Tournament before testing removal and registration; their resulting state is the measured baseline. Setup operations are excluded, but setup-created objects remain observable during the experiment. |
| “concrete inputs, participant sets” in the space summary | “Estes nunca surgiram explicitamente” | Define participants as distinct invocations and their input tuples explicitly before the summary. They are already mentioned, but the relationship and notation need strengthening. |
| Pipeline figure | Distinguish data from actions | Give data artifacts and processing stages different shapes/roles. Show reports flowing back to search and candidate requests flowing into generation/execution. |
| “dispatched commands” in Evidence Extraction | Commands or services? Commands may belong to implementation | Use service operations in the conceptual account; explain command dispatch in Implementation. |
| “handlers” in the same paragraph | “O mesmo para handlers.” | Use event processing / consumer operations conceptually, with handler classes explained later. |
| “A workload may also reference a deterministic setup plan” | “Não surgiu nas definições anteriores?” | Introduce setup and its binding to participant inputs once in the formulation; explain its construction in Approach. |
| Invalid setup/orchestration/infrastructure attempts | Their number could be part of evaluation | Report these counts, categories and execution cost. Invalid attempts consume the attempt budget; they are not assigned a zero score. |

## Material discrepancies to address

### The motivating example and its execution boundary

The paragraph says the participant-addition Saga reads **and stores** the old name before
the source update. Figure 1 instead shows **read old → update/publish → store old → delivery**.
Align the prose to the figure, which exposes the cached-input interaction more clearly.

The figure places an explicit fault inside the event-triggered propagation Saga. The
ordinary generated event consequence is currently atomic and owns no independent fault
slot; local GA cannot choose that internal propagation fault. This also conflicts with
the later explanation that event consequences have no vector positions if the reader
assumes every motivating action is directly searched.

Keep the example as motivation with its boundary explained, or replace/augment it with
a qualified measured example. Do not present its exact injected propagation fault as a
result of the current GA. Expanding consumer-step fault control would be a separate
implementation decision, not an editorial fix. Event-propagation control defects and any
experimental repair must also be checked before reusing older positive outcomes.

### Observation, impact and preference are different parts of the model

The current `J(e)=Phi(L(e),tau(e),D(e))` paragraph remains too vague. It lists invariant
rejections and architect-defined consistency expectations as though they were the
implemented scoring mechanism. Our current mechanism is a set of deterministic checks
over structured runtime evidence; it does not require an architect-supplied harmful-state
oracle. Invariant exceptions and compensation occurrence are not automatic score points.

Introduce the evidence collected, the conditions assessed, measurement coverage, and the
separate numerical outputs before describing a search preference. Proposed conceptual
notation, to discuss rather than adopt silently:

`O(e) = (persistent findings, read-exposure findings, lifecycle outcomes, coverage)`.

`I(e)` counts the union of distinct aggregate identities with established persistent
findings. `A(e)` counts deduplicated read exposures within the declared read scope; its
unit differs from I. A policy can use both, but no combined weights have been selected.
The first search uses complete I only and records A separately. Incomplete evidence is
not equivalent to measured absence, and an object with two reasons counts once in I.

Explain the three persistent conditions accurately:

1. An active aggregate retains a subscription-declared dependency on an aggregate observed
   becoming deleted during the attempt and remaining deleted.
2. A failed Saga finishes recovery but leaves changed application state/lifecycle on an
   aggregate for which it is the sole observed writer. A new creation logically deleted
   during its recovery is excluded; ambiguous multiple writers remain unknown.
3. A selected event was actually delivered, the same receiver's persistent state did not
   change across delivery, and that surviving receiver remains eligible at the horizon.

The third condition is not a count of every undelivered event. An empty eligible receiver
set is explicitly recorded and contributes no impact solely through that absence.

Read exposure joins an exact returned revision to another Saga's write and subsequent
explicit compensation: creation removed or changed attributes restored. Define this at
the Saga boundary and retain reader outcome; do not equate it mechanically to every
classical dirty-read definition. General lost updates, write skew and predicate reads
are not covered by this diagnostic.

### What evidence is actually available

The formulation should include the baseline as well as the terminal state. Runtime
instrumentation records committed revisions, writer/action identity, selected deliveries,
receiver eligibility and supported read responses. Exact response adapters are needed
for the current read scope. This is not arbitrary textual log interpretation or generic
Jaeger trace analysis, and static footprints alone do not establish runtime anomalies.

An application rejection can be a valid, completely observed result with a deviating
schedule. Avoid equating every deviation/exception with invalid infrastructure execution.
Say what conformance records and what assessment prerequisites require.

### Search representation and budget

Retain the useful formulation `e=(q,x,rho)`. Local search must explicitly choose both
the fault assignment and the recovery order; equal vectors with different orders can
have different outcomes and must remain distinct candidates.

The GA operates on one no-fault/first-fault choice per Saga plus a recovery sequence.
It translates those choices into a binary vector for execution. Later fault bits within
an already-aborted Saga are masked, so `2^N` is a raw assignment count, not the number of
distinct executable alternatives. In the measured pair, five slots give 32 raw vectors,
12 canonical per-Saga combinations, and 29 scenarios after recovery alternatives.

Describe the actual steady-state cycle: evaluated initial population; parent tournaments;
per-Saga crossover; inherited recovery sequence or valid replacement; mutation; duplicate
check; fresh execution; retain the highest-score population. Record the chosen defaults
in experimental configuration rather than presenting eight and 0.3 as theoretically
preferred. Diversity-based survival is a proposal, not implemented behaviour.

Correct “at most B distinct valid experiments” to a budget of **attempted executions**.
Invalid attempts consume budget with unavailable fitness. Duplicate proposals do not
consume another application execution but have generation/time costs. A prior no-fault
control has separate, explicit accounting. Termination also includes proven exhaustion
of the bounded space and proposal stall.

The hierarchy can remain part of the intended design. Internally, do not claim an
implemented contextual allocator, a selected combined I/A reward, or novelty-guided
selection. The figure currently says “impact and novelty guide…” before we have chosen
that novelty mechanism.

### Scope statements worth tightening

- Supported combinations of distinct selected event routes exist; “unsupported fan-out”
  alone does not explain that capability or distinguish multiple eligible receivers.
- A non-selected route is outside the declared horizon, not automatically an omitted
  delivery fault. A selected route with no eligible receiver is an explicit empty result.
- Aggregate access must include semantic state effects added by Saga command wrappers;
  a read-named operation may require recovery. Avoid implying conflict compression proves
  independence beyond the available evidence.
- Do not describe partial restoration as “undo” guaranteeing full rollback. Compensation
  attempts a business recovery; measuring its residual effects is central to this work.

## Suggested writing blocks

### First: introduction generalisation and research question

Proposed paragraph, to refine with the user and add appropriate primary-source citations:

> The example illustrates one way in which a fault can leave related aggregates with
> different representations of the same information. Other executions may leave an
> active aggregate depending on an object removed during recovery, or retain changes
> made by a Saga whose compensation has finished. An operation may also read a version
> written by another Saga that subsequently compensates that version. These observations
> require different evidence: some concern the state left after recovery, while others
> concern the order of reads and writes during execution. We record them separately so
> that the reported outcome identifies both the affected state and the observed interaction.

Add a short explanation that local invariant checks may reject an operation successfully
while cross-aggregate conditions still require observation. Architectural tolerance can
remain motivation for configurable priorities, without requiring a hand-authored oracle.

Proposed overarching question:

> How can application evidence and observed execution outcomes guide the systematic
> discovery of potential consistency problems in concurrent Saga executions under a
> limited execution budget?

Subquestions should measure executable construction, correctness/coverage of the declared
observations, and discovery efficiency under equal attempt budgets. Retain the upper-level
research objective without inventing its results.

### Second: formulation and approach aligned together

Use one small running example to connect invocation inputs, setup, actions, selected
routes, fault choices, recovery and reports. Introduce I/A as separate outputs, then
explain evidence collection and the GA loop. Keep implementation class names out of the
conceptual paragraphs and remove duplicated definitions between sections.

Redraw the pipeline as data flowing through operations, with the search control loop
explicit. Avoid a row where source files, algorithms and results all look like the same
kind of object.

### Third: Implementation and Evaluation Methodology

Implementation can now describe source/test extraction, setup result binding, deterministic
execution, persistent observations, read adapters and search operators concretely.
Evaluation Methodology can define controls, coverage, repeated seeds, fixed budgets,
discovery curves, reference maps and costs before a broader final campaign exists.

The current 29-case reference has 15 I-positive and 14 I-zero scenarios, all positives
exposing the same partial-removal defect. The six GA/random arms used 174 new executions;
eight extra reference completions make 182 new executions in that campaign. Results are
mixed, all arms find all 15 by execution 29, and only 14 of 63 post-initialisation GA
executions are new crossover children. This supports a measured mechanism account, not
an established efficiency advantage or a count of 15 independent bugs.

The separate update/read campaign supplies I=1 histories with A=1 versus A=0, showing
additional discrimination. Do not splice these into one cohort or infer relative weights
from a set with constant I.

## Proceed without turning this into another implementation project

Start with the paragraph requested after the example, the example ordering correction,
and the overarching RQ. Then revise the impact/formulation/approach block coherently.
Draft Implementation and Evaluation Methodology next. Leave reward choice and any event
fault expansion for explicit discussion. The title, authors, abstract and remaining ACM
sample material need cleanup, but should not displace the scientific revision or be
silently replaced with guessed publication metadata.

## Evidence used

- [Current behaviour](../current-state.md#impactv2-assessment), including the read diagnostic,
  event consequence model and fixed-workload genetic search.
- [Roadmap](../roadmap.md#outcome-6-local-fault-vector-search).
- [Current GA comparison](../../../verifiers/experiments/fixed-workload-ga/DISCOVERY-RESULTS.md).
- [Impact/anomaly matrix](../evidence/impact-anomaly-matrix-2026-09-10/README.md).
- [Anomaly literature and scope map](saga-anomalies.md).

This pass reviews the manuscript and repository evidence. It is not a fresh audit of
every related-work citation or a full dissertation review.

## Authorised writing applied in Overleaf

The user approved writing the stable formulation and mechanism descriptions before
discussing advisor comments individually or in related groups. Twelve local tracked
replacements were applied to `acmtog.tex` in Reviewing mode:

- distinguish raw binary assignments from canonical first-fault choices per Saga;
- define baseline, execution outcomes, conformance, structured observations and coverage;
- define affected-aggregate set G, read-exposure set X, and their separate counts I and A;
- retain Phi as an abstract search preference, without selecting combined weights;
- account for executed attempts, including unavailable assessments, and separate controls
  and duplicate-generation cost;
- describe persistent checks and exact-version read proofs, including concrete Quizzes
  examples, attribution limits and empty-receiver handling;
- describe the steady-state GA against `search.py`: parent tournaments, per-Saga crossover,
  mutation before recovery-order resolution, duplicate fallback and population survival;
- describe equal-budget random comparisons, repeated seeds and discovery curves;
- preserve the intended hierarchical architecture without selecting its upper-level
  algorithm or reward; remove the figure's unsupported novelty-feedback wording;
- clarify that setup establishes the measured baseline and compensation attempts recovery.

The full editor source was copied back and matched exactly the expected twelve edits.
The introduction and ACM template material were unchanged. All thirteen advisor comment
threads remained open; none was answered or resolved. Overleaf recompiled successfully
with zero errors and 23 warnings (the warning count was also 23 before this pass).
The new equations fit in the rendered column. The edited document has eleven pages,
including the pre-existing ACM sample material.

The manuscript retains the agreed intended-final-system voice. Internally, the shipped
GA still uses complete I only; combined I/A fitness and the contextual allocator remain
design decisions. This editorial pass changes no implementation or experiment results.

Next, discuss the introduction's generalisation-after-example comment cluster with the
user. Its old architect-oracle wording, example-order discrepancy and event-internal
fault boundary still need alignment. Continue through the remaining comment groups,
including potential conflicts, action/input/event definitions and the pipeline figure.
Some comments overlap the new setup/assessment text, but remain to be reviewed together.

## Proposed reimagining of the introductory example and Figure 1

Discussion proposal, not applied to Overleaf. The user reports additional feedback that
the existing example and figure are difficult for an unfamiliar reader.

Prefer the qualified UpdateTournament / FindTournament interaction to the three-Saga
name-propagation story. Introduce the domain in ordinary terms: a scheduled quiz activity
has settings and an associated question collection. A teacher changes its settings;
another request retrieves them. Keep source class names for the later case-study section.

Use matrix histories 11 and 13: the same forward schedule and final-step fault, with
the retrieval placed before or after recovery. The shared prefix persists the Tournament
update (start time 12:05 to 12:25, among other changes), then fails before updating the
associated Quiz. In one continuation the retrieval returns 12:25 before recovery restores
12:05; in the other recovery runs first and the retrieval returns 12:05. This contrast
belongs to recovery alternatives within one fixed workload, so it also connects directly
to the local GA's representation. Avoid suggesting that the current GA changes normal
read placements across different fixed workload plans.

Figure proposal: one initial-state card; one shared update/fault prefix; two short
continuation lanes; explicit returned values and final start time. Label lanes by read
placement, not harmful/benign. Show application actions as verbs, state values separately,
and use position/arrows for order. Display only the start-time projection and say so in
the caption. Do not assign I=0 or claim whole-object restoration: both real fault runs
have I=1 because recovery loses embedded topic course IDs. Explain that separate residue
in the detailed assessment example; the introduction's figure illustrates read exposure,
not every output of the metric. A differs (1 versus 0).

Follow the example immediately with the generalisation requested by the advisor:
execution history reveals reads of later-compensated versions; terminal state reveals
residual changes and surviving dependencies; selected event processing can leave related
state unsynchronised. Then explain why faults, normal order and recovery order must be
explored together. Move the interleaving-count formula to Problem Formulation and introduce
DDD terminology after the reader understands the practical problem. Keep related work
after this motivation and generalisation.

Alternatives considered: partial Tournament removal is stronger for explaining persistent
effects and the existing I-only GA benchmark, but the damage can occur without a reader;
a familiar commerce example is easier to recognise, but would be illustrative rather
than a reproduced application experiment. Retain removal for the detailed I explanation.
This recommendation concerns motivating Figure 1; redesign of the pipeline Figure 2
remains a separate comment group.

### Applied after user approval

The user approved this reimagining and explicitly required that comments remain untouched.
Six local tracked replacements were applied in Reviewing mode: the opening scope sentence,
the motivating example, Figure 1 with the following generalisation and DDD explanation,
the qualitative exploration-space paragraph, the old architect-oracle paragraph's
assessment wording, and relocation of the interleaving formula to Problem Formulation.

The new paragraph begins “The example illustrates a concurrency anomaly at the Saga
level” and connects compensated-version reads to residual changes, surviving dependencies
and unsynchronised event-related state. The next paragraph defines local invariants and
explains why relating actions and aggregate versions is necessary. Both precede the
related-work discussion. The original uppercase advisor instruction remains verbatim.

All twelve comment threads present at the start of this editing pass retain exactly the
same visible comment text; none was answered or resolved. The current source was captured
again before editing, preserving intervening author changes since the previous pass.
The final source matches exactly the six expected replacements. Overleaf compiles with
zero errors and 23 warnings. The PDF now has twelve pages including existing template
material. Figure 1 was inspected at full-panel width: labels, branching arrows and
returned/final values are legible and do not overlap. The paragraph appears immediately
after the example in reading order; the two-column figure floats to the next page.
The browser was left showing the new figure for review.

### Broader motivation and literature after browser annotations

On 11 September the user approved broadening the introduction beyond the current
measurement categories, naming and citing relevant anomaly families, marking the fault
as injected, and making the example's persistent effect visible. Five local tracked
replacements were applied in `acmtog.tex`, and two entries were appended to
`sample-base.bib`:

- The opening now concerns how faults and interleavings affect application behaviour
  and consistency. The generalisation records interactions and resulting state without
  introducing the I/A separation in the introduction.
- Dirty reads and lost updates are named with short explanations. Berenson et al.,
  *A Critique of ANSI SQL Isolation Levels* (SIGMOD 1995), supplies the classical
  terminology. The next sentences explicitly move to Saga-level interactions across
  locally committed transactions, citing the existing Richardson reference. This is
  motivation, not a claim that the current detector covers every classical anomaly.
- Korth, Levy and Silberschatz, *A Formal Approach to Recovery by Compensating
  Transactions* (VLDB 1990), supports the need to account for effects already observed
  by other operations. The text does not adopt that paper's formal correctness model.
- Figure 1 now shows `Injected fault` and the initially present / finally missing topic
  course identifiers in both histories. The example introduces what those identifiers
  represent and states that recovery loses them. The reader's placement distinguishes
  the read exposure; it does not cause the persistent topic defect.
- The DDD paragraph was shortened to explain local invariants and cross-aggregate
  relationships. The original uppercase advisor instruction remains verbatim.

Primary text checked: [Berenson et al.](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/tr-95-51.pdf),
especially P1/P4; [Korth et al.](https://www.vldb.org/conf/1990/P095.PDF), sections 1–2;
and [Richardson's Saga pattern page](https://microservices.io/patterns/data/saga.html).
The publisher's online book chapter was inaccessible in this pass; its contents were
not treated as newly verified. Bibliographic metadata was cross-checked against the
papers and bibliographic records.

Validation: the full TeX source matches the five expected edits; the complete original
bibliography is preserved with exactly the two new entries appended. All twelve open
comment threads retain the same visible text and status; none was answered or resolved.
The article compiles with zero errors and 24 warnings. The additional warning is the
missing publisher address in the Korth bibliography entry; no unresolved citations occur.
The new paragraph and figure were inspected at full-panel width, including rendered
author–year citations, fault label, arrows and both final-state boxes.

### Simplified example and advisor-requested paragraph order

The user subsequently requested that the introductory example focus on the timetable
and the concurrent retrieval. The topic/course-identifier defect remains an experimental
finding, but is no longer part of the introductory story, Figure 1, its caption or its
accessible description. The example now explains that restoring 12:05 does not change
the response of 12:25 already returned to the first reader. Final figure boxes explicitly
describe the start time, rather than implying that the complete Tournament is restored.

The generalisation paragraph now follows the DDD/local-invariant paragraph, immediately
before the advisor's unchanged uppercase insertion. This follows the requested order:
example, aggregates and invariants, anomaly/recovery families, then the exploration
space. The anomaly references and broader motivation are retained. No comment was
answered or resolved, and all twelve open comment threads remain available.

Validation: the complete TeX source matches the intended local changes. The refreshed
PDF compiles with zero errors and the same 24 warnings; the introduction and simplified
figure were visually inspected. The bibliography and implementation are unchanged.

### Whole-introduction flow and readability pass

The user approved a connected revision after reviewing the standalone DDD paragraph
and repetitions in the second half. Twelve local tracked replacements were applied
to the introduction in Overleaf. The fresh source already omitted the advisor's
uppercase instruction; that intervening user edit was preserved rather than restored.

- The opening now explains intermediate visibility, faults and concurrent recovery
  without repeating the same motivation in several sentences. The timetable example
  and simplified figure are unchanged.
- Generalisation starts with the example's Saga-level dirty read. The following
  paragraph introduces persistent effects and integrates DDD aggregates and local
  invariants as the consistency boundaries needed to understand them.
- The related-work paragraph retains the existing descriptions and citations, but
  replaces the broad exclusion claim about all three systems with an explicit statement
  of this paper's application-level problem. This is not a new literature audit or
  proof of novelty.
- A single proposal paragraph explains construction, controlled execution, assessment
  and feedback. The separate three-challenge recap was folded into a shorter search
  description. Genetic search includes fault locations and recovery/continuation
  orders. Allocation across workloads remains part of the proposed search design;
  no completed allocator experiment or performance improvement is asserted.
- One overarching research question introduces three subquestions on executable
  scenario construction and reduction, reproducible execution and explained findings,
  and discovery under equal budgets. These are manuscript proposals for joint review;
  the advisor's corresponding comment remains open.
- The simulator paragraph describes what its environment provides. Evaluation prose
  names executability, reproducibility, evidence and discoveries over execution budget,
  removing an undefined severity measure and unexplained classified-coverage terminology.
  The contribution list covers formulation, assessment, search design and implementation/
  evaluation. No I/A combination, detector scope or implementation priority changed.

Validation: complete source readback equals the twelve expected replacements, with all
source from Problem Formulation onwards unchanged. The twelve open comment threads
retain identical visible text and status; none was answered or resolved. Compilation
has zero errors, 24 warnings and seven typesetting notices. The refreshed PDF has eleven
pages including the remaining ACM template material; the first three pages were visually
inspected. Prose tokenisation by whitespace, excluding Figure 1 source, changes from
1,042 to 948 words (approximate, including TeX markup). The bibliography is unchanged.

Suggested next comment group: action scope, potential conflicts and consistency
relationships in Problem Formulation. Explain which objects an action can access,
why a possible shared access is only a scenario-selection signal, and how execution
evidence establishes the eventual finding. Review these together before addressing
event consequences, setup definitions and the pipeline figure.

### Actions, potential conflicts and consistency relationships

Following the user's approval of the three proposed clarifications, four local tracked
edits were applied to Problem Formulation:

- Actions may read or modify multiple aggregate instances; their effects depend on
  inputs and encountered state. The text defines action type and occurrence, then
  explains the derived footprint through types, access modes and identifier evidence.
- The extraction list now names potential conflicts between aggregate accesses of
  different Sagas, rather than claiming that static analysis establishes actual conflicts.
- The workload-selection paragraph explains possible overlapping accesses with at least
  one modifying access, the weaker evidence of type-only matches, and the role of
  execution observations in identifying actual instances and versions.
- Section 2.3 opens by relating local invariant checks to the wider assessment of
  operation interactions and aggregate relationships. No new detector, invariant oracle,
  metric or formula was introduced.

The preceding review checked the production-extraction documentation and
`ConflictGraphBuilder`: read/read pairs are ignored, and exact, symbolic and weaker
matches retain distinct evidence. This supports the wording without treating static
matching as proof of runtime interference.

Validation: full source readback matches exactly the four expected edits. The eight
comment threads open at the beginning of this pass retain identical visible text and
status; none was answered or resolved. Earlier user changes to comments and source
were preserved. The refreshed PDF compiles with zero errors, 23 warnings and eight
typesetting notices. Page 3 was visually inspected, including the new action and
conflict paragraphs and the opening of Section 2.3. Existing box/layout notices remain;
the current complete document has twelve pages, including ACM template material.

### Event consequences as scheduled actions

Following approval to return to the advisor's event/action comment, four local
tracked edits clarify the existing model:

- Section 2.1 defines an event consequence as processing an emitted event through
  a selected consumer, including the application work it triggers, after its producer.
- Section 3.2 illustrates this with a student-name update and the Tournament
  consumer updating its stored copy. Other scheduled actions may occur between them.
- Section 3.3 explains that several selected routes for the same event each occupy
  their own schedule position.
- Section 3.5 explains synchronous consumer processing, the absence of independent
  interleaving of its internal Saga steps, delivery-time eligibility, empty eligible
  sets continuing without impact solely for absence, and the current limit of one
  eligible aggregate per selected route.

The event-replay contract in `current-state.md` was checked before editing. This is
manuscript clarification, not a change to implementation or metric scope. The current
singular aggregate wording in the action definition, edited since the previous pass,
was preserved. The conflict discussion and pipeline figure remain separate review work.

Validation: complete source readback equals exactly the four intended replacements.
The five open advisor comment threads present at the beginning of this pass retain
identical visible text and status; none was answered or resolved. Reviewing mode was
active. Compilation has zero errors, 24 warnings and seven typesetting notices; the
document remains twelve pages. The updated route and replay paragraphs on page 5 were
visually inspected. Existing bibliography/template and layout warnings remain.

### Introduce persistent conditions before the formula

The user approved the short explanation requested by the advisor's September 11
comment about the forward reference. Section 2.3 now names deleted dependencies,
residual changes after recovery, and unchanged surviving event receivers that remain
eligible, before defining `G(e)`. Section 3.6 retains the detailed evidence requirements.
The definitions of `X(e)`, `I(e)` and `A(e)` and the fitness policy are unchanged.

Validation: full source readback equals the single intended tracked replacement;
the open advisor comments retain identical visible text and status. No comment was
answered or resolved. Compilation has zero errors, 24 warnings and seven typesetting
notices. Page 4 was visually inspected with the explanation directly above the formula.

### Approach figure: operations, data and search feedback

Following the user's approval on September 14, Figure 2 was replaced with an editable
TikZ diagram. Rounded boxes name five operations: analyse application and tests,
construct workloads, select a fault experiment, execute and observe, and assess
execution. Square-cornered boxes contain input source/tests and output findings and
measurements; intermediate data are named on the arrows. The selection box identifies
allocation across workloads and GA search within a workload as parts of the proposed
approach. A dashed path returns results to selection. The former `Validated domain
impact` label was removed. No search policy, implementation claim or formula changed.

The figure follows a two-row path to keep labels legible at full text width. The caption
explains the visual notation and feedback; the accessibility description follows the
same data flow. The surrounding manuscript was preserved.

Validation: complete source readback matches the single intended figure replacement.
The three open advisor comments retained identical visible text and status, with none
answered or resolved. Reviewing mode was active. The refreshed twelve-page PDF compiles
with zero errors, 23 warnings and eight typesetting notices. Figure 2 on page 5 was
visually inspected; no figure overlap or clipping was observed. Existing manuscript
and template layout/bibliography notices remain.

### Align the Approach prose with the revised figure

Read the complete Approach and searched the manuscript for Figure 2 references and
the old pipeline terminology. Three tracked edits align the narrative:

- The overview now follows analysis, workload construction, experiment selection,
  execution, assessment and feedback, with validity and coverage accompanying findings.
  It explains that subsequent selections reuse the derived workload structures.
- Fault and Recovery Scenarios introduces construction as defining admissible
  experiments from which exploration selects.
- Hierarchical Guided Exploration explicitly links the figure's selection operation
  to allocation across execution structures and search within the selected structure.

The extraction, event handling, replay, assessment and detailed GA descriptions remain
compatible with the diagram and were preserved. No implementation, formula or search
policy changed. The remaining advisor comments on commands/services and evaluation
were not resolved or absorbed into this figure-coherence pass.

Validation: full source readback equals the three intended replacements, and all open
advisor comments retain identical visible text/status. Compilation has zero errors,
23 warnings and seven typesetting notices. The document remains twelve pages. The
revised overview and its rendered Figure 2 reference on page 4 were visually inspected.

### Remove ACM sample content (15 September)

Removed the sample title, authors, abstract, classification, publication metadata and
all template sections following the Approach, including sample acknowledgements and
appendices. Retained the ACM document class and typography with `nonacm`, the original
license comments, TikZ support and the manuscript's bibliography commands. Plain page
numbering avoids sample running headers while the real front matter is still unwritten.
No replacement title, abstract or evaluation section was invented.

The complete source readback matched the intended prefix/suffix replacements. The
source from Introduction through the end of Approach, including both figures, was
unchanged. The bibliography file was not edited. Reviewing mode remained active; no
comment was answered or resolved and no existing research edit was accepted or rejected.

The compiled PDF decreased from twelve to seven pages, with zero errors, thirteen
warnings and one typesetting notice. Bibliography warnings remain. The first page was
visually checked at fit-to-width: the first figure and Introduction render without
the fictional front matter. Implementation and evaluation work were left for the
user's requested discussion after this cleanup.

### Restore front matter after scope clarification (15 September)

The user clarified that cleanup meant the template content starting at section 4,
not the opening material. Restored the entire original source prefix before
Introduction, including title, authors, abstract, publication settings and `maketitle`.
This also removes the temporary `nonacm` and plain-page settings. Full source readback
confirmed an exact prefix restoration with the rest of the cleaned manuscript unchanged.
Only the removal of template sections after Approach remains. Compilation reports zero
errors and sixteen warnings; the restored first page was visually verified. Reviewing
mode remains active and no comments were answered or resolved.
