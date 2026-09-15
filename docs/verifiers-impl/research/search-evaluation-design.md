# Search evaluation: selecting a useful horizon

Status: workload exploration authorized; final cohort and publication protocol remain for
user discussion. This note records evaluation criteria, not completed experimental results.
The current exploration may execute bounded pilot comparisons with predeclared settings.

## Horizon and repetitions answer different questions

The horizontal axis is the number of distinct application executions made by one strategy
on one fixed workload. Repeating a 30-execution run with ten seeds does not produce a
300-execution discovery curve. Seeds measure variation between runs; the per-run budget
allows later generations to use earlier feedback.

Choose the per-run horizon after measuring the available domain. Prefer candidate workloads
with at least 200 unique fault/recovery alternatives, where the current application permits
it. This is a selection target, not a claim that such workloads are already executable.
Retain small spaces as complete reference controls rather than padding them with repeated
executions. Do not enlarge an experiment by counting proposal duplicates, masked fault bits,
or different identifiers for identical ordered actions as new application cases.

## Four explicit experimental dimensions

1. Saga/input selection: `BRUTE_FORCE` versus `INTERACTION_PRUNED`, with strict or explicit
   type-only fallback evidence for interaction pruning.
2. Forward ordering: full `ORDER_PRESERVING_INTERLEAVING` versus `SEGMENT_COMPRESSED`.
3. Search within a fixed workload: GA versus the existing matched random sampler.
4. Feedback: a fixed explicit vector of criterion weights, held identical between strategies.

The code's `BRUTE_FORCE` configuration selects Saga sets; it is not a synonym for full
step interleaving or for executing every fault scenario. State these dimensions separately.
Compression also uses the selected conflict-evidence lens: varying that lens can change
both selected combinations and segment boundaries. Isolate one factor at a time.

The current GA does not choose participants, source inputs, forward order or event horizon.
Those belong to the selected workload. Its choices concern fault locations and permitted
recovery orderings. Multiple forward histories are therefore multiple workloads, not
additional genes of the same current search.

## Cohort selection before effectiveness claims

Inventory source-derived executable setup, Saga stories, fault coordinates, exact computed
recovery totals, returned caps, and no-fault controls. Record why candidates were retained
or rejected. Include variation in domain size and interactions, not just many ID variants
of the same known positive. A large domain whose reachable feedback is constant is useful
as a limitation/control, but cannot establish that feedback guides search.

Select candidates primarily by structural criteria and execution viability. Pilot results
may establish whether a score is available or varies; report that selection explicitly and
keep the pilot distinct from the eventual comparison. Do not silently remove unfavorable
runs or workloads with coverage gaps. Fix the final cohort before final comparison outcomes.

For each proposed longer run, declare its budget, seeds, recovery cap, weights, runtime,
population, mutation, stall rule and concurrency before launch. Keep raw component counts
and gaps even when some weights are zero. Unsupported evidence is not zero impact.

## Fair comparison and interpretation

Both arms receive the same domain, runtime, weights, execution budget and initialization
sampling rule. Every fresh run gets its own cache and measured feedback. The random policy
samples per-Saga fault coordinates then a returned recovery ordering; it is not uniform
sampling over all persisted scenarios. Any additional uniform-catalogue baseline must be
labelled separately. No outcomes from a reference map enter parent selection.

Plot cumulative distinct positive scenarios and best-so-far score against actual executions.
Retain per-component discoveries, score-unavailable attempts, offspring versus random
fallback, duplicate proposals, generation time and total wall time. Incomplete application
attempts consume budget. Do not draw unexecuted curve segments as measured progress after
exhaustion or a proposal stall. Compare paired seeds and show spread when repetitions exist.

A scalar fitness maximizes the chosen score; it does not directly maximize the number of
positive scenarios. A method may find a few high-scoring scenarios while another finds
more lower-scoring positives. Report both rather than declaring one metric the implicit
objective of the other. Scenario counts also do not count independent application bugs.

"Fraction of all positives" requires a fully enumerated and assessed reference under the
same domain/cap and score policy. Without it, report the number discovered, not recall over
an unknown total. Even with a complete reference, an eventual tie at full exhaustion is
expected: the question is which method finds useful cases earlier.

Offline rescoring assesses the same recorded sequence with different weights. It does not
simulate how changed weights would alter GA choices. Comparing those choices requires new
GA runs. Keep raw findings available to explain the tradeoff between preferred criteria.

## What is ready for the paper

Configuration and implemented algorithm mechanics can be described now. Effectiveness,
compression savings and scalability statements require the selected experiments. Avoid
claiming configuration usability has been evaluated merely because JSON parameters exist.
Write results and limitations after reviewing the exploration and agreed main campaign.
