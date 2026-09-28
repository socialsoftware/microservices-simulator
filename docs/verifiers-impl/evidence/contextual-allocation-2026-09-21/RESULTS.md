# Contextual allocation — 21 September 2026

Implemented and independently reviewed. These are local searches over previously measured scenarios; no application was executed again. The final contextual policy below is `contextual-linucb-cooldown`. Raw `contextual-linucb` results remain in `summary.json` as a diagnostic comparator.

## What is implemented

One configurable global budget counts selected, previously unseen scenarios across workloads. The outer policy selects a workload for one attempt; its existing GA selects the scenario. Switching preserves that GA’s population and history. Reward is the newly revealed weighted impact score, with fixed user-selected weights. The shared model uses Saga membership, Saga pairs, interactions, events and observed progress; it has no workload-ID or unseen-outcome feature.

Unknown scores consume budget but supply no training label or GA parent. After an unknown, the guarded contextual policy selects another workload once if one remains available. This prevents consecutive monopolization by a workload that cannot be scored; it does not eliminate unavailable results. Zero is a valid reward and does not trigger this guard.

The CLI currently reads complete recorded maps. Live application dispatch, qualification of new workloads, persistence/resume of a live allocator and concurrent allocation remain future work.

## Comparison design

- Small cohort: all nine admitted complete maps in the retained final-diversity, late-diversity and topic-orders batches, 189 distinct scenarios. Rejected controls were excluded using the existing gate, not new scores.
- Mixed cohort: those nine plus the complete 3,918-scenario AddParticipant/UpdateTournament/LeaveTournament/RemoveTournament map, using its separately verified retry-adjusted reference: 4,107 distinct scenarios.
- Each policy uses the same GA (population 8, mutation 0.3, uniform unseen initialization/fallback), seeds 1–30, and one shared budget. Policies differ only in workload allocation.
- Balanced means round-robin; random means uniform among currently nonexhausted workloads, not uniform among all scenarios. Independent adaptive UCB learns each workload separately. The contextual model shares its parameters.
- Two fixed preferences: all five criteria with unit weights; or deleted dependencies alone. Exploration and ridge were both 1, without tuning to these results.
- Every recorded attempt is unique within a search; unknowns are retained. Full small-cohort exhaustion reaches identical observations for every policy.

## Results

Each cell is **mean confirmed positive scenarios / mean accumulated score**, over 30 seeds. These are scenarios, not distinct bugs. Full distributions and paired comparisons are in [summary.json](summary.json).

### Small cohort — all criteria

Catalogue: 189 scenarios; 38 positive, 135 zero, 16 unknown under this preference.

| Budget | Balanced | Random workload | Independent adaptive | Contextual |
|---:|---:|---:|---:|---:|
| 30 | 4.73 / 5.60 | 4.33 / 5.27 | 5.77 / 6.63 | 6.53 / 7.60 |
| 60 | 7.70 / 9.40 | 8.07 / 9.73 | 12.93 / 14.53 | 16.30 / 17.63 |
| 120 | 20.63 / 22.63 | 20.57 / 22.57 | 30.10 / 32.03 | 33.20 / 34.87 |
| 189 | 38.00 / 40.00 | 38.00 / 40.00 | 38.00 / 40.00 | 38.00 / 40.00 |

### Small cohort — deleted dependencies only

Catalogue: 189 scenarios; 2 positive, 187 zero, 0 unknown under this preference.

| Budget | Balanced | Random workload | Independent adaptive | Contextual |
|---:|---:|---:|---:|---:|
| 30 | 0.87 / 0.87 | 0.93 / 0.93 | 1.17 / 1.17 | 1.27 / 1.27 |
| 60 | 1.70 / 1.70 | 1.67 / 1.67 | 2.00 / 2.00 | 2.00 / 2.00 |
| 120 | 2.00 / 2.00 | 2.00 / 2.00 | 2.00 / 2.00 | 2.00 / 2.00 |
| 189 | 2.00 / 2.00 | 2.00 / 2.00 | 2.00 / 2.00 | 2.00 / 2.00 |

### Mixed cohort — all criteria

Catalogue: 4,107 scenarios; 3,044 positive, 613 zero, 450 unknown under this preference.

| Budget | Balanced | Random workload | Independent adaptive | Contextual |
|---:|---:|---:|---:|---:|
| 100 | 21.37 / 43.23 | 22.23 / 45.33 | 77.47 / 267.97 | 72.73 / 247.27 |
| 500 | 303.80 / 990.60 | 303.80 / 990.60 | 416.83 / 1464.40 | 395.30 / 1368.87 |
| 1000 | 718.77 / 2409.80 | 718.77 / 2409.80 | 826.80 / 2853.57 | 782.30 / 2653.07 |

### Mixed cohort — deleted dependencies only

Catalogue: 4,107 scenarios; 1,784 positive, 2,105 zero, 218 unknown under this preference.

| Budget | Balanced | Random workload | Independent adaptive | Contextual |
|---:|---:|---:|---:|---:|
| 100 | 8.03 / 8.03 | 8.17 / 8.17 | 34.60 / 34.60 | 47.37 / 47.37 |
| 500 | 208.03 / 208.03 | 208.03 / 208.03 | 281.80 / 281.80 | 300.70 / 300.70 |
| 1000 | 519.47 / 519.47 | 519.47 / 519.47 | 576.27 / 576.27 | 589.13 / 589.13 |

## Interpretation and limits

With nine workloads and budget 60, the contextual policy finds 16.30 positives on average, against 8.07 for random workload selection and 12.93 for independent adaptive selection. Its 10th–90th percentile range is 12–21.1 positives; this is variation across seeds, not a confidence interval. With budget 189 all policies find the same 38 positives.

Adding the large map changes the result. At budget 1,000 with all criteria, contextual allocation finds 782.30 positives versus 718.77 for random workload selection, but independent adaptive selection finds 826.80. With deleted dependencies alone, contextual allocation finds 589.13 versus 519.47 and 576.27 respectively. The contextual policy is therefore not uniformly better than the simpler adaptive alternative.

The missing-feedback guard has a real cost: on the mixed all-criteria case the raw contextual policy finds 826.27 positives at budget 1,000, while the guarded policy finds 782.30. On the synthetic missing-only stress case, however, the raw policy spends all 20 attempts on unknowns; the guarded policy obtains 10 known positives and 10 unknowns. Both results are retained. The guard was chosen for this operational failure before the guarded real comparisons, not to maximize their score.

The large cohort is dominated by one map, and the small cohort’s positives largely occur in two related Topic workloads. These comparisons demonstrate allocation and bounded search behavior, not broad generalization to unseen Saga families. A useful next evaluation holds out related families and isolates the effect of structural features against independent adaptive allocation. Simply adding more seeds to the same dominant map will not establish that benefit.

## Overhead and verification

At 1,000 mixed-cohort choices, contextual selection and updating together took about 3.28 seconds with all criteria and 3.03 seconds with deleted dependencies; independent adaptive allocation took about 1.63 and 1.26 seconds. These local bookkeeping measurements include the local GA and exclude loading, initialization, evaluator/application latency and final serialization. They are not an isolated performance benchmark or end-to-end runtime comparison.

Implementation validation: 90 unit tests passed. Independent review checked 96 exact seeded comparisons with the frozen pre-refactor GA, workload-switch continuity, hidden-future noninterference, exhaustion without duplicates and a direct matrix-solution check. The 16 independent checks and input/source hashes are preserved in [verification.json](verification.json). The two cohorts, two profiles, five policies and 30 seeds produced 600 search replays, not 600 new application measurements. Reference and configuration hashes were independently checked again after completion.

## Artifacts and continuation

- [CLI and configuration](../../../../verifiers/experiments/fixed-workload-ga/README.md#cross-workload-recorded-allocation).
- [Local comparisons and per-seed decision indices](../../../../verifiers/target/allocator-review-2026-09-21/).
- [Guarded CLI smoke result](../../../../verifiers/target/allocator-review-2026-09-21/cli-guarded-60/results.json).
- [Cohort selection](cohort.json), [mixed selection](mixed-protocol.json), [guard rule frozen before comparisons](guarded-protocol.json).

Next: discuss the missing-feedback tradeoff, then connect the existing evaluator boundary to live ScenarioExecutor execution and persist allocator/GA state for resumption. Before claiming a benefit from shared structural learning, compare matched held-out Saga families and a version without structural features. Paper changes remain paused pending André’s review.
