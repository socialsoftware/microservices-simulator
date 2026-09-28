# Allocation models and missing feedback — 21 September 2026

## What was compared

Three adaptive models, each with and without the same rule after missing feedback.
Independent UCB estimates each workload separately. Progress-only linear UCB shares one
model over observed progress. Full linear UCB also sees Saga membership, Saga pairs,
interactions and scheduled events. The rule excludes the workload returning an unavailable
score from the next decision when an alternative exists. It does not turn missing feedback
into zero, update the reward model, or alter the local GA.

Each choice spends one global attempt on one unseen scenario. Each workload keeps its own
GA population and random state across choices. Reward is the newly obtained weighted score.
We used population 8, mutation 0.3, exploration/ridge 1, seeds 1–30 and unchanged candidate
catalogues. No parameter was tuned on these results.

The small cohort has nine workloads and 189 scenarios. Five workloads have only zero
scores; the other four cover removal and Topic/update event orders. The mixed cohort adds
one 3,918-scenario workload combining AddParticipant, UpdateTournament, LeaveTournament and
RemoveTournament. This large workload supplies 95.4% of its scenarios: the cohort is useful
for comparing allocation, but is not a representative sample of all Quizzes workloads or a
test of transfer to unseen Saga families.

The two user-selected profiles weight all five criteria equally, or only deleted dependencies.
Results below use the revised residual assessment reference. Original results remain in
`summaries.json`, alongside variability, allocation counts, available score, unknown attempts,
paired differences and selection/update overhead. These are sequential searches over recorded
feedback; they do not measure application execution time or live dispatch.

## Results

Means over 30 seeds. Positive scenarios are not distinct bugs. Accumulated score is the
actual sum of rewards supplied to the allocator, separately from positive scenario count.

### Small cohort, all profile, budget 60

Reference: 189 scenarios; 38 positive, 135 zero, 16 unavailable.

| Model | Rule | Positive mean | Positive p10–p90 | Score mean | Unavailable mean |
| --- | --- | ---: | ---: | ---: | ---: |
| Independent UCB | Off | 12.93 | 8.9–18.1 | 14.53 | 3.17 |
| Independent UCB | On | 12.90 | 8.9–18.2 | 14.50 | 3.17 |
| Shared linear UCB: progress | Off | 17.47 | 14.0–21.0 | 18.07 | 7.03 |
| Shared linear UCB: progress | On | 17.20 | 13.9–21.0 | 17.83 | 5.67 |
| Shared linear UCB: structure + progress | Off | 16.00 | 11.9–21.0 | 17.30 | 4.20 |
| Shared linear UCB: structure + progress | On | 16.30 | 12.0–21.1 | 17.63 | 3.93 |

### Small cohort, dependency profile, budget 60

Reference: 189 scenarios; 2 positive, 187 zero, 0 unavailable.

| Model | Rule | Positive mean | Positive p10–p90 | Score mean | Unavailable mean |
| --- | --- | ---: | ---: | ---: | ---: |
| Independent UCB | Off | 2.00 | 2.0–2.0 | 2.00 | 0.00 |
| Independent UCB | On | 2.00 | 2.0–2.0 | 2.00 | 0.00 |
| Shared linear UCB: progress | Off | 1.47 | 0.0–2.0 | 1.47 | 0.00 |
| Shared linear UCB: progress | On | 1.47 | 0.0–2.0 | 1.47 | 0.00 |
| Shared linear UCB: structure + progress | Off | 2.00 | 2.0–2.0 | 2.00 | 0.00 |
| Shared linear UCB: structure + progress | On | 2.00 | 2.0–2.0 | 2.00 | 0.00 |

### Mixed cohort, all profile, budget 1000

Reference: 4,107 scenarios; 3,228 positive, 613 zero, 266 unavailable.

| Model | Rule | Positive mean | Positive p10–p90 | Score mean | Unavailable mean |
| --- | --- | ---: | ---: | ---: | ---: |
| Independent UCB | Off | 867.67 | 852.8–884.1 | 2940.53 | 56.13 |
| Independent UCB | On | 833.17 | 814.0–849.8 | 2806.90 | 55.53 |
| Shared linear UCB: progress | Off | 859.73 | 845.4–877.3 | 2905.50 | 57.37 |
| Shared linear UCB: progress | On | 833.50 | 815.6–853.1 | 2788.23 | 61.03 |
| Shared linear UCB: structure + progress | Off | 866.57 | 851.0–881.0 | 2936.03 | 55.50 |
| Shared linear UCB: structure + progress | On | 836.67 | 817.4–854.7 | 2807.80 | 56.43 |

### Mixed cohort, dependency profile, budget 1000

Reference: 4,107 scenarios; 1,784 positive, 2,105 zero, 218 unavailable.

| Model | Rule | Positive mean | Positive p10–p90 | Score mean | Unavailable mean |
| --- | --- | ---: | ---: | ---: | ---: |
| Independent UCB | Off | 576.27 | 558.0–591.1 | 576.27 | 40.03 |
| Independent UCB | On | 569.43 | 549.0–585.1 | 569.43 | 39.53 |
| Shared linear UCB: progress | Off | 613.97 | 591.3–638.0 | 613.97 | 43.30 |
| Shared linear UCB: progress | On | 590.60 | 560.8–612.0 | 590.60 | 41.17 |
| Shared linear UCB: structure + progress | Off | 610.53 | 589.7–628.1 | 610.53 | 43.30 |
| Shared linear UCB: structure + progress | On | 589.13 | 564.9–607.0 | 589.13 | 41.23 |

## Interpretation and next decision

The missing-feedback rule reduces mean positive discoveries and accumulated score for all
three models in both mixed-cohort profiles. It does not consistently reduce unavailable
attempts either. These results support keeping it optional, not enabling it by default.
No policy default was changed in this pass.

Structural context is not a consistent improvement over progress-only sharing. With all
criteria in the mixed cohort, full context finds 6.83 more positives than progress-only,
while independent UCB finds 1.10 more than full context. With deleted dependencies alone,
progress-only finds 3.44 more than full context; both outperform independent UCB. On the
small all-criteria cohort, progress-only finds 17.47 versus full context's 16.00 at budget
60. The small dependency profile contains only two positives and provides little evidence
about generalization. Paired seed differences and ranges are retained rather than treating
these means as guaranteed rankings.

The next implementation boundary is a live ScenarioExecutor evaluator with durable search
state and resume semantics, preserving one attempt per decision and the same GA
state. A separate evaluation should test whether structure helps prioritize previously
unexplored workloads from held-out families. More repetitions of the same dominant map
would not establish that claim. Both steps remain proposals for André, not work started
under this comparison or permission to edit the paper.

## Assessment audit and correction

All 450 originally unavailable scenarios were joined to their retained execution reports
and checked against recorded hashes. Two bounded corrections were made in verifiers only:

- A lifecycle change by another successful Saga no longer blocks attribution of unrelated
  fields. Existing version-chain and disjoint-field requirements still apply. In all 186
  affected cases, the failed update leaves `/applicationData/tournamentTopics` changed;
  another Saga's lifecycle change concerns a different field.
- A committed event-consumer write can participate only when a unique successful event action
  and delivery prove the exact attempt, workload, action, event, handler, receiver and version.
  Overlapping fields still block attribution. All 16 real event cases now pass provenance
  but remain unknown because the Saga and event consumer changed overlapping fields.

Replaying all 202 retained report pairs with the previous assessor reproduces every category,
status and score. With v3, 186 residual assessments become complete; 184 full weighted scores
become available. The other two still have incomplete read attribution. Unrelated categories
are unchanged in all 202 reassessments. No application or simulator source was changed and
no application was rerun. Prior observations, raw report hashes and reference versions remain
preserved; revised observations explicitly link to the pure reassessment output.

The revised mixed reference has **3,228 positive, 613 zero and 266 unavailable** scenarios.
Its remaining unavailable scores are:

| Cause | Distinct scenarios | Why left unavailable |
| --- | ---: | --- |
| Recovery fails on missing aggregate 12 | 218 | Recovery is incomplete: 130 explicit compensations and 88 implicit rollbacks. These retained reports are not timeout evidence. |
| Read with an intervening writer | 32 | Another write occurs between the version read and recovery; the current detector cannot establish the required attribution. |
| Overlapping Saga/event fields | 16 | Proven writer identity does not establish which operation caused the surviving field value. |

The old count of 30 read-unknown cases excluded the two also blocked on residual attribution.
Read-gap occurrences must not be confused with scenario counts (62 occurrences across 32 cases).

## Verification and reuse

- 92 allocator/search tests reported passing in the implementation handoff.
- 61 ImpactV2 Spock cases pass, including disjoint/overlapping lifecycle changes and exact,
  missing, mismatched, duplicate and failed event provenance.
- Parent review independently checked 24 properties: unchanged local-GA prefixes across
  switches, hidden-score isolation, exhaustive endpoints, all three guard variants,
  absence of structure in progress-only features, and linear algebra against a separate
  direct solver (maximum coefficient error below 2.4e-14).
- All 360 previously recorded seeded adaptive/contextual trajectories match in order,
  checkpoints and allocations after adding the new variants.
- Every comparison verifies selected candidates are unique, feedback matches its reference,
  and source/reference hashes remain unchanged. All fully exhausted small runs reach the
  same endpoint for their profile.
- Pure reassessment changes no candidate's deleted-dependency reward and no small-cohort
  reward under either profile. Those 540 searches are reused after exact candidate-level
  equality checks. Only the 180 mixed/all-criteria searches are repeated. There are 900
  searches in total (720 original plus 180 revised), with no new application measurements.

## Artifacts

- [Final verification](verification.json)
- [Aggregated results and variability](summaries.json)
- [Frozen model parameters and source hashes](protocol.json)
- [Independent behavioral checks](independent-checks.json)
- [Existing trace parity](existing-trace-parity.json)
- [Reassessment verification](reassessment-verification.json)
- [Original/revised reference provenance](reassessment-provenance.json)
- [Exact retained artifact paths and hashes](artifact-hashes.json)

Compact per-seed traces, assessor outputs, selected raw report pairs and reproduction scripts
remain in `verifiers/target/allocator-factorial-2026-09-21/`. Large archives were not copied or
extracted. Paper edits and live dispatch remain deferred for André's review.
