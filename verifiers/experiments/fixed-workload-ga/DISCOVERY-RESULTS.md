# Discovery speed with current I

The complete reference contains **15 I-positive scenarios out of 29**. GA and random
both found all 15 at execution **29 in every seed**. Intermediate discovery differed:
GA reached half the positives earlier in two seeds and later in one; it reached 80%
earlier in one, later in one and tied in one. These results support inspecting the
whole discovery curve rather than only time to the last positive.

## Experiment and cost

The workload, source setup, normal order, recovery cap 20, runtime and ImpactV2 I are
unchanged from the first qualification. The workload removes a Tournament and tries to
register a participant. All positives fail Tournament removal after Quiz deletion,
leaving an active Tournament depending on that Quiz and a deletion surviving recovery.
I=2 counts those two objects; different positive schedules expose the same defect.

The reference reused 21 already measured current-runtime candidate keys and executed the
eight remaining keys once. GA/random then independently executed budget 29 for each of
seeds 11, 29 and 47. Population stayed 8, mutation 0.3 and proposal-stall limit 100. The
protocol was frozen before the eight missing cases were measured. No tuning was performed.

**182 new application executions completed**: eight map completions and 174 search
attempts. All had COMPLETE ImpactV2 assessments and COMPLETE_WITHIN_SCOPE read coverage;
A remained 0. There were no invalid scores, retries or early stalls. Every arm executed
29 distinct candidates with its own fresh Docker/JVM/H2 attempts. Reference observations
were used only for evaluation; search obtained its own feedback. The identical-runtime,
hash-verified no-fault control was reused and adds no new execution to this count.

Two containers ran concurrently. The paired arms took approximately 640 seconds for
seed 11, 774 seconds for seed 29 and 707 seconds for seed 47. Host-time variation between
pairs is retained; execution counts are the main comparison here. Costs include generator
requests and duplicate handling, while no duplicate proposal launches another application.

## When were the positives found?

The targets are at least 50% (8/15), 80% (12/15) and 100% (15/15). Entries below are
actual application execution counts, not proposals or population generations.

| Seed | Strategy | First positive | 8 positives | 12 positives | All 15 |
| --- | --- | --- | --- | --- | --- |
| 11 | GA | 1 | 20 | 26 | 29 |
| 11 | Random | 1 | 18 | 25 | 29 |
| 29 | GA | 3 | 15 | 24 | 29 |
| 29 | Random | 5 | 20 | 25 | 29 |
| 47 | GA | 2 | 20 | 26 | 29 |
| 47 | Random | 2 | 22 | 26 | 29 |

![Cumulative distinct positive discoveries](../../../docs/verifiers-impl/evidence/ga-discovery-2026-09-10/discovery-curves.png)

Blue is GA; orange is random. The dotted line marks the end of the initial eight
application evaluations. Higher at the same execution count means more positives already
found. The last-positive metric ties in all seeds despite differences in earlier discovery.
SVG and PDF versions are retained beside the PNG.

## What did evolution contribute?

| GA seed | New crossover children executed | Positive children | Later random executions | Duplicate proposals |
| --- | --- | --- | --- | --- |
| 11 | 4 | 3 | 17 | 246 |
| 29 | 5 | 4 | 16 | 118 |
| 47 | 5 | 3 | 16 | 143 |

Across the three GA arms, **14 of the 63 post-initialization executions** were new
crossover children; ten were positive. The remaining 49 were random exploration after
duplicate proposals. Duplicates were cached, never re-executed within an arm. The guided
and random subsets occur at different stages and are selected differently, so their
positive proportions are not an independent effectiveness comparison.

One seed-11 example illustrates the mechanism. The tenth execution combined choices
from an I=0 parent (`10000`) and an I=2 parent (`00101`), changed the recovery choice and
executed a new `00100` scenario with I=2. Another child mutated the removal fault earlier
in the Saga (`01000`), before Quiz deletion, and measured I=0. Lineage records identify
the exact parents, choices, actions and reports.

Seed 29 also began with four positives in GA's initial eight evaluations versus one
in random's initial eight. Equal seeds and the same sampling distribution do not imply
identical initial populations: GA tie-breaking consumes its random stream differently.
The longer experiment provides real evolutionary steps, but it does not isolate their
causal benefit from initialization/exploration on this small benchmark.

## What follows

We now have the requested measurement of discovery speed and a complete, current
reference for evaluating a changed feedback definition. The outcome is mixed, not a
consistent GA advantage. A concrete implementation limitation is the small number of
new children accepted: most later evaluations still come from random exploration.
Discuss improving that novelty handling separately from changing I/A feedback, so their
contributions remain distinguishable. No operator or scoring change was made here.

The current 0/2 landscape is coarse and all positives expose the same partial-removal
problem. A richer feedback comparison also needs workloads where its added observations
actually vary. Reassess the reference whenever the positive definition changes; do not
reuse this denominator as though it described a different metric.

## Evidence and verification

- [Compact audited summary](../../../docs/verifiers-impl/evidence/ga-discovery-2026-09-10/summary.json)
  retains all new search attempts, report hashes, curves, targets and stop/accounting data.
- [Complete reference](../../../docs/verifiers-impl/evidence/ga-discovery-2026-09-10/reference.json)
  links all 29 candidate keys to measured attempts and exact action sequences.
- Raw artifacts: `verifiers/target/fixed-workload-ga/discovery-01/`, including protocol,
  per-arm package revisions, per-attempt replay snapshots, frozen search sources and
  Python environment metadata. Analysis code is retained separately with its hash.
- The audit passed report/score joins, snapshot/package integrity, fresh attempt/container
  identities, chronological parent feedback and score agreement with the complete map.
- All 22 search/evaluator/analysis tests passed; the five new analysis cases cover target
  rounding, unavailable feedback, early stops, duplicate/mismatching observations and an
  empty positive reference. Documentation build and whitespace checks passed.

Run/reproduction commands are in [README.md](README.md). The scoring policy, search
operators, production Java, application code and existing packages were preserved.
