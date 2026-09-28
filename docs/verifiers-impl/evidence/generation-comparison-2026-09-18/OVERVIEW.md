# Global Quizzes selection and compression

All combinations of 2–4 distinct Saga types among 37 types with accepted inputs; at most three accepted input variants per Saga. Broad includes strict evidence plus type-only/unknown-key fallback.

| Sagas | All input tuples | Broad input tuples | Strict input tuples | Input reduction, broad | All forward orders | Broad forward orders | Broad + compressed orders |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 2 | 3841 | 1481 | 14 | 61.44% | 102904 | 86117 | 8961 |
| 3 | 107085 | 31291 | 0 | 70.78% | 4672333452 | 4626288619 | 87153627 |
| 4 | 2167713 | 595123 | 0 | 72.55% | 1086137855316984 | 1083376568484336 | 13162997272310 |

The two panels have different denominators. Broad pruning removes 61–73% of input tuples, but the retained tuples dominate the number of forward orders. At sizes 3 and 4, pruning alone removes about 0.99% and 0.25% of forward orders respectively; compression then reduces the retained order space by 98.12% and 98.79%.

These counts measure reduction, not runtime preservation. The separate 98-to-40 update/query experiment holds selection fixed and evaluates compression preservation under declared observations. It does not validate pruning recall.
