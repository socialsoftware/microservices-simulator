# Diagnosis of the retained GA on the 186-case reference

## Method and validation

Replay all 30 GA seeds using the frozen search.py, fitness.py and RecordedDomain class,
with feedback from the measured reference. Read the current parent population through
the existing emit callback without changing the search frame or drawing random numbers.
Retain all proposal traces, including rejected duplicates. Compare candidate order,
feedback, operator, parent lineage, genes, recovery replacements, stop reasons and
proposal/duplicate counts with the original retained traces. All 30 replays match.
All input hashes remain unchanged; no application execution or GA behavior was modified.
The script loads only the structural class from its frozen AST to avoid importing Docker
runtime tooling with paths relative to its old source location.

Raw proposals and population snapshots are in `verifiers/target/ga-diagnosis-2026-09-16/`.
`summary.json` here keeps pooled counts and concrete examples. Reproduce with:

```sh
python verifiers/experiments/fixed-workload-ga/diagnose_reference_ga.py \
  --reference verifiers/target/exhaustive-reference-2026-09-16 \
  --output verifiers/target/my-ga-diagnosis
```

## What happens in the first 100 evaluations

Across 30 seeds there are 3,000 distinct-within-search evaluations. Of these:

| Source of evaluated candidate | Evaluations | Complete positive scores | Positive fraction |
| --- | --- | --- | --- |
| Crossover, including optional mutation/repair | 1,072 | 948 | 88.43% |
| Random initialization or duplicate fallback | 1,928 | 700 | 36.31% |

Thus an average first-100 search evaluates 35.73 offspring and 64.27 random candidates.
Good novel offspring exist; most evaluated candidates nevertheless come from random
sampling. These conditional rates are descriptive, not a causal comparison of operators
on identical remaining candidate sets.

The search proposes 2,760 offspring before completing those 3,000 evaluations. Of these,
1,688 (61.16%) are already seen. In the 51–100 evaluation interval specifically, 992 of
1,500 offspring proposals (66.13%) repeat a seen candidate. A single duplicate increments
`stalled`; the `stalled == 0` guard then disables crossover until random sampling finds a
new candidate. It does not try another informed child first. Random fallback uses the
same per-Saga-fault then recovery sampler whose distribution was diagnosed in the
uniform-baseline comparison. Rejected duplicates cost proposals, not app evaluations;
their indirect effect is changing the source of the next evaluated candidate.

## Parent selection and recovery inheritance

The pool keeps the eight highest-scoring evaluated candidates, with shuffled ties and
no explicit diversity criterion. All 30 searches reach eight score-2 parents by
21–65 evaluations (median 36). At evaluation 100, the pool still spans 4–7 distinct
fault vectors, so it has not literally collapsed to one genotype. All parent Update
faults are slots 5 or 6, consistent with the productive late-update region.

The two parent tournaments run independently: they can select the same parent twice.
This occurs in 698/5,339 (13.07%) offspring proposals over full searches. Example from
seed 1: proposal 24 selects the same score-2 case twice, applies no mutation and recreates
that case. Proposal 25 switches to random and selects an Update getTopics fault with
score zero. The example establishes a mechanism; it is not the sole cause of duplicates.

Recovery inheritance copies the entire ordered action list from a parent. Changing the
fault choices often invalidates that exact action list for the child. In 2,692/5,339
(50.42%) offspring proposals, it is replaced by a valid randomly selected list. This
preserves validity but means half the proposals do not retain the proposed recovery list.
These data do not independently establish that repair hurts discovery; it can introduce
useful novelty as well.

## It is not just a mismatch between positive count and score magnitude

At 100 evaluations, averages from the unchanged traces are:

| Metric | GA | Uniform catalogue random |
| --- | --- | --- |
| Complete positive cases | 54.93 | 59.37 |
| Score-2 cases | 23.53 | 27.53 |
| Sum of available scores | 78.47 | 86.90 |

Unavailable fitness is excluded from the score sum, not reclassified as score zero.
Uniform random also leads on these weighted-output measures, so maximizing score 2
rather than merely finding positives does not explain away the result.

## Proposed experiment and subsequent implementation

For a completely enumerated catalogue, change only initialization and duplicate fallback
to uniform sampling among unseen candidates. Keep crossover, parent selection, mutation,
weights, seeds and measured feedback fixed. Compare this variant with uniform random
and the retained original GA. It tests the diagnosed reliance on a weak exploration
sampler; it also guarantees progress to exhaustion for this finite-catalogue mode.
Do not claim the same guarantee or enumeration cost for on-demand search.

If useful, a separate subsequent experiment can test distinct parents or bounded child
retries. Do not combine changes immediately or tune weights on this reference to claim
superiority. This map is now a development dataset; any selected improvement needs
confirmation on other qualified workloads or held-out evidence. The present data justify
a hypothesis about exploration and duplicate handling, not a proven improved GA.

The user subsequently approved this experiment. Its implementation and measured outcome
are recorded in [uniform exploration](../ga-uniform-exploration/README.md); the diagnosis
and original search evidence above remain unchanged.
