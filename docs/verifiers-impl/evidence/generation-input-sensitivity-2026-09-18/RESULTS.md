# Quizzes input-cap sensitivity

All combinations of 2–4 distinct Saga types among the same 37 types. Input caps 1, 3 and 10 use nested, deterministic source-derived populations: 37, 89 and 220 variants. Of 817 policy/source-mode eligible variants, 597 remain excluded by cap 10. Acceptance does not prove runtime setup readiness.

## Results

| Input cap | Sagas | Input combinations before pruning | After pruning | Input combinations removed | Forward orders retained after pruning + compression |
| ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 2 | 666 | 248 | 62.76% | 7.37% |
| 1 | 3 | 7,770 | 2,216 | 71.48% | 1.33% |
| 1 | 4 | 66,045 | 17,612 | 73.33% | 0.83% |
| 3 | 2 | 3,841 | 1,481 | 61.44% | 8.71% |
| 3 | 3 | 107,085 | 31,291 | 70.78% | 1.87% |
| 3 | 4 | 2,167,713 | 595,123 | 72.55% | 1.21% |
| 10 | 2 | 23,255 | 9,729 | 58.16% | 8.74% |
| 10 | 3 | 1,572,728 | 519,280 | 66.98% | 2.13% |
| 10 | 4 | 76,452,186 | 24,776,202 | 67.59% | 2.31% |

At cap 10, pruning plus compression retains 8.74%, 2.13% and 2.31% of the respective brute-force forward-order spaces for two, three and four Sagas. For four Sagas, the retained fraction rises from 1.21% at cap 3 to 2.31% at cap 10. Reduction remains large, but its magnitude depends on the admitted inputs. The absolute compressed four-Saga space grows from 13,162,997,272,310 to 252,788,942,287,136 forward orders. These are mathematical counts, not materialized or executed scenarios.

## Validation and provenance

- Caps 1 and 3 reproduce the earlier input files and every Saga-set count row byte-for-byte.
- 26 complete-enumeration cases at cap 10 passed count, uniqueness, per-Saga order and whole conflict-anchor-order preservation checks; all 3,996 pair accounting comparisons agree with the production calculator.
- 223,443 Saga-set rows cover all 74,481 sets at each of three caps. No sampling of Saga sets at these sizes.
- Current application, simulator and verifier source hashes stayed unchanged during the run. Only experiment cap configuration/reporting was generalized; no production behavior changed.
- Raw data and logs: `verifiers/target/generation-comparison-cap10-2026-09-18/`. Source/build fingerprints, input IDs, summary, checks and raw row hashes are retained here.
- Overall local turnaround was about 82 seconds, including recovering plotting with an existing Python environment after the bundled Python lacked matplotlib. This is an operational duration, not an isolated performance benchmark. Counts were not rerun.
- Static forward orders only: faults, recovery and event expansion are not counted. Structural checks do not replace runtime outcome-preservation experiments.
- Full eligible-input counting and broad runtime executability coverage remain unmeasured.

Reproduce with the generation-comparison runner using `--input-caps 1 3 10` and a Python environment containing matplotlib; use a fresh output directory. `summarize.py` reconstructs this table and figure from the named frozen run.
