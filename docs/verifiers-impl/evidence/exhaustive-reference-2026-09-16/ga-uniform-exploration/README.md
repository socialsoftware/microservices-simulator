# GA with uniform unseen initialization and fallback

## Approved single change

The user approved testing uniform sampling among unseen catalogue candidates for the
initial population and the fallback after a duplicate offspring. This is an explicit
`exploration="uniform-unseen"` option on the search function, exercised by the experiment
runner with a fully enumerated RecordedDomain. Default on-demand/live callers retain
the previous per-Saga fault and returned-recovery sampler.

The domain contains candidate structure only. `sample_unseen` chooses from sorted unseen
keys using the seeded RNG, without reading scores. The chosen candidate supplies the
fault genes and exact recovery actions to the existing loop. Parent selection, crossover,
mutation probability (0.3), population (8), tie handling, recovery repair, weights and
measured feedback remain unchanged. Same-parent choices and duplicate offspring remain
possible. Uniform mode requires a finite catalogue adapter and stall limit >= 2, allowing
one duplicate proposal to reach the guaranteed-unseen fallback.

## Results

All comparisons use the same 186-case reference, five unit weights and seeds 1–30.
112 cases have complete positive scores, 72 complete zeros and two unavailable scores.
The unavailable cases remain unavailable and consume budget; no application runs occurred.

| Evaluations | Original GA | Uniform-exploration GA | Uniform random |
| --- | --- | --- | --- |
| 25 | 12.03 | 17.37 | 14.83 |
| 50 | 26.03 | 35.33 | 29.33 |
| 100 | 54.93 | 66.80 | 59.37 |
| 150 | 85.17 | 94.40 | 89.93 |
| 184 | 110.00 | 110.97 | 110.83 |

Values are mean complete-score positive scenarios. At 100 evaluations the variant leads
uniform random in 28 seeds, ties in one and loses in one. Same-numbered seeds do not
couple equivalent random choices. Mean evaluations to 56 known positives are 81.77 for
the variant, 94.00 for uniform random and 101.27 for original GA. All 30 variant runs
cover all 186 candidates without repeated evaluations; original GA covers all in 23/30.
Mean evaluations to all 112 positives are 184.90 for the variant and 185.33 for uniform:
the main advantage is earlier discovery, not substantially faster completion.

At 100 evaluations, the variant averages 32.93 score-2 cases and 99.73 summed available
score, versus 27.53 and 86.90 for uniform random. Unavailable scores are excluded from
the sum, not reclassified as zero. Novel offspring supply 1,000 of the 3,000 first-100
variant evaluations; random initialization/fallback still supplies 2,000. The improvement
therefore does not require eliminating duplicate children or increasing the fraction of
evaluations supplied by crossover. This experiment changes initialization and fallback
together and does not separate their individual effects.

## Validation and limits

- All 55 fixed-workload Python tests pass, including finite-catalogue exhaustion despite
  duplicate offspring, unavailable-feedback exclusion from parents, seeded reproducibility
  independent of input map order, and rejection of unsupported uniform-mode configurations.
- The default policy reproduces all 30 original candidate sequences, scores, parents,
  genes, recovery replacements, proposal/duplicate counts and stop reasons exactly.
- All variant scores match the measured reference, each catalogue is covered exactly
  once per seed, and source/evidence hashes stay unchanged during the experiment.
- The plot averages all 30 seeds through the common measured horizon (184 evaluations).
  Original GA stalls are not filled with invented future observations.

This is a development-map result: this workload motivated the change. It supports a
bounded improvement on that map, not universal GA superiority or held-out validation.
Full enumeration/storage cost is not measured by replay discovery curves. The next step
is to freeze this variant and agree a second workload/control protocol before inspecting
its comparative outcomes. Do not add distinct-parent selection, more child retries or
weight tuning at the same time. No paper text or production/live default was changed.

## Reproduction

Requires the completed reference, the uniform-baseline output and matplotlib:

```sh
python verifiers/experiments/fixed-workload-ga/uniform_exploration.py \
  --reference verifiers/target/exhaustive-reference-2026-09-16 \
  --uniform verifiers/target/uniform-reference-2026-09-16 \
  --output verifiers/target/my-ga-uniform-exploration
```

Raw variant traces, proposals and all comparison curves are retained under
`verifiers/target/ga-uniform-exploration-2026-09-16/`. This directory keeps compact results,
protocol hashes, validation and figures. Earlier experiments remain unchanged.
