# Uniform catalogue baseline

186 candidates; 112 complete-score positives; 2 unavailable scores.

All methods use the same measured map and weights. GA and two-stage random traces are retained unchanged. Uniform random shuffles all candidate keys before reading feedback. Unknown cases remain in the permutation and consume budget. This compares discovery order, not live timing.

| Evaluations | GA | Two-stage random | Uniform random | Uniform expectation |
| --- | --- | --- | --- | --- |
| 25 | 12.03 | 8.83 | 14.83 | 15.05 |
| 50 | 26.03 | 18.83 | 29.33 | 30.11 |
| 100 | 54.93 | 45.43 | 59.37 | 60.22 |
| 150 | 85.17 | 80.00 | 89.93 | 90.32 |
| 184 | 110.00 | 110.07 | 110.83 | 110.80 |

All-known-positive completion counts: {'ga': 23, 'random': 27, 'uniform': 30}

Curves average all 30 seeds up to the common measured horizon; no extrapolation across GA stalls. The uniform expectation is n × known positives / candidate count. A complete catalogue has an enumeration/storage cost; this comparison does not measure that cost.
