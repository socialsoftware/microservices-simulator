# Quizzes: all eligible source inputs

All combinations of 2–4 distinct Saga types among 37 types, using all 817 inputs accepted by the declared Saga source-mode and RESOLVED_OR_REPLAYABLE policy. The configured cap of 1,000 exceeds the extracted population: the normalizer records **zero inputs removed by the cap**. This is the complete eligible population in this source snapshot, not every possible application input or a guarantee of runtime preparation.

## Input-limit sensitivity

| Input cap | Sagas | Input combinations before pruning | After pruning | Input combinations removed | Forward orders retained after pruning + compression |
| --- | ---: | ---: | ---: | ---: | ---: |
| 1 | 2 | 666 | 248 | 62.76% | 7.37% |
| 1 | 3 | 7,770 | 2,216 | 71.48% | 1.33% |
| 1 | 4 | 66,045 | 17,612 | 73.33% | 0.83% |
| 3 | 2 | 3,841 | 1,481 | 61.44% | 8.71% |
| 3 | 3 | 107,085 | 31,291 | 70.78% | 1.87% |
| 3 | 4 | 2,167,713 | 595,123 | 72.55% | 1.21% |
| 10 | 2 | 23,255 | 9,729 | 58.16% | 8.74% |
| 10 | 3 | 1,572,728 | 519,280 | 66.98% | 2.13% |
| 10 | 4 | 76,452,186 | 24,776,202 | 67.59% | 2.31% |
| All eligible | 2 | 309,568 | 123,318 | 60.16% | 7.88% |
| All eligible | 3 | 72,334,850 | 23,013,302 | 68.19% | 3.50% |
| All eligible | 4 | 11,693,312,153 | 3,772,461,422 | 67.74% | 16.60% |

## Complete-population counts

| Sagas | All forward orders | After pruning | After pruning and compression |
| ---: | ---: | ---: | ---: |
| 2 | 7177832 | 5678962 | 565344 |
| 3 | 884664549310 | 860391239192 | 30967282662 |
| 4 | 409603507703198258 | 407732752429852360 | 68013420453657718 |

## Evidence and scope

- Limits 1, 3 and 10 reproduce the previous input identities/accounting as equal JSON values and every Saga-set count row byte-for-byte; input populations are nested. JSON object key order differs between JVM launches but values do not.
- 31 complete-enumeration checks at the full eligible population passed count, uniqueness, per-Saga order and whole conflict-anchor-order preservation; 3996 comparisons agree with the production accounting calculator.
- All 74,481 Saga sets are counted at each of four input limits. No sampling of Saga sets or cap on the computed number of forward orders.
- The local run finished in 2218.6 seconds including compilation, extraction, counting, checks and reporting. This is elapsed operational turnaround, including any laptop suspension; not an isolated performance benchmark or CPU time.
- These are static forward orders. Faults, recovery, events and prerequisite expansion are not included; accepted recipes can still be unsupported by the executor.
- Structural preservation checks concern the extracted conflict model. Runtime preservation of findings and executability require application runs.
- No application, simulator or production verifier behavior was modified. Source hashes remained unchanged during the run.
- Raw rows and logs: `verifiers/target/generation-comparison-all-inputs-2026-09-18/`. This folder retains manifests, exact input identities, checks, summary and row hashes.
- Reproduce with the generation-comparison runner using `--input-caps 1 3 10 1000`, a current isolated build, matplotlib-enabled Python, and a fresh output directory. Always verify `inputVariantsCapped == 0` before calling the last population complete.
