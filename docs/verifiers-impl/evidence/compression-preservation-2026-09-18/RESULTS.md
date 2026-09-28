# Segment compression: completed runtime comparison

All 98 scenarios completed with exact schedule conformance and complete observations.
The six no-fault controls succeeded with all five criterion counts zero. The observed
application baseline was identical in every attempt.

**All observed finding signatures, combinations of findings, and final-state
projections remained represented within each of the 12 named fault assignments,
while the required scenario set fell from 98 to 40 (59.18% fewer).**

| Primary comparison | Result |
| --- | ---: |
| Full / compressed forward orders | 6 / 3 |
| Full / compressed fault scenarios, including recovery alternatives | 98 / 40 |
| Finding signatures absent from the compressed subset, within the same fault assignment | 0 |
| Combinations of findings absent from the compressed subset | 0 |
| Observed final-state projections absent from the compressed subset | 0 |
| Incomplete observations | 0 |

This distinguishes executions from their observations. The full set contains 36
positive scenarios, while the retained subset contains 12: multiple executions
reproduce observations already represented in those 12. Likewise, compensated-read
exposures occur in six full-set scenarios and two retained scenarios, without losing
any compared exposure signature. These are scenario counts, not distinct defect
counts. The result does not claim that the discarded execution histories themselves
are identical or that arbitrary business observations would be preserved.

For generation accounting, the full and compressed sets contain 72 and 36
workload/fault-vector pairs respectively. The preservation experiment holds the
selected Saga/input pair fixed: it evaluates compression, not the recall of pruning.

The concrete case updates a Tournament and queries it. Faults before question
selection or before the Quiz update can trigger recovery after the Tournament update.
Recovery restores the timetable and question count but rebuilds its topics without
their course identifiers. Some query placements additionally return the version
subsequently compensated. Both observations remain represented in the compressed set.

The result concerns one fixed original test-input pair and the declared canonical
forward-fault/recovery domain. It does not establish universal semantic completeness,
other input coverage, selected event-delivery preservation or pruning recall. Each
scenario has one runtime observation. See the experiment README for signature and
projection definitions; raw reports remain under the target run directory.
