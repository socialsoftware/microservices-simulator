# GA with uniform unseen exploration

Single-change recorded-feedback experiment on the previously inspected development map.

| Evaluations | Original GA | GA with uniform unseen exploration | Uniform random |
| --- | --- | --- | --- |
| 25 | 12.03 | 17.37 | 14.83 |
| 50 | 26.03 | 35.33 | 29.33 |
| 100 | 54.93 | 66.80 | 59.37 |
| 150 | 85.17 | 94.40 | 89.93 |
| 184 | 110.00 | 110.97 | 110.83 |

All known positives reached: {'ga-original': 23, 'ga-uniform': 30, 'uniform': 30}

All 30 original traces reproduce exactly under the default policy. All variant traces cover the full catalogue with no repeated evaluations. Unknown feedback remains unavailable. No application runs, weights or genetic operators were changed. Generalization requires other workloads.
