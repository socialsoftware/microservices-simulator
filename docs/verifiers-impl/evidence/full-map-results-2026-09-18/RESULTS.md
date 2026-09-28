# Complete-map search comparison

5184 scenarios: 3979 complete positive scores, 657 complete zero scores, 548 unavailable complete scores.

Both methods use the shipped search implementation, the same catalogue, unit weights for all five criteria, and seeds 1–30. Population 8 and mutation probability 0.3 are fixed. Each observation is revealed only when its scenario is selected. Uniform random samples without replacement; GA initializes and falls back to uniform unseen sampling. Unknown scores consume budget and cannot become GA parents.

| Evaluations | GA positives | Random positives | GA score sum | Random score sum | GA unknown | Random unknown |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 100 | 84.93 | 75.87 | 264.00 | 213.17 | 7.30 | 10.67 |
| 250 | 214.00 | 191.60 | 673.23 | 531.93 | 18.03 | 25.70 |
| 500 | 426.43 | 382.30 | 1342.00 | 1058.77 | 37.67 | 53.97 |
| 1000 | 841.17 | 764.73 | 2617.03 | 2107.27 | 82.20 | 106.53 |
| 2000 | 1643.83 | 1531.70 | 4976.03 | 4213.07 | 178.30 | 214.10 |
| 3000 | 2412.97 | 2301.33 | 7088.63 | 6330.33 | 287.13 | 317.73 |
| 5184 | 3979.00 | 3979.00 | 10952.00 | 10952.00 | 548.00 | 548.00 |

| Fraction of known positives | GA evaluations, mean | Random evaluations, mean |
| --- | ---: | ---: |
| 50% | 2445.37 | 2597.97 |
| 80% | 4051.07 | 4148.47 |
| 90% | 4613.20 | 4666.43 |
| 100% | 5183.73 | 5183.70 |

All 60 traces visit each catalogue member exactly once and therefore reach the same positive count and available score sum at exhaustion. Counts refer to scenarios, not distinct defects. The score sum accumulates available complete scores; unavailable scores remain explicitly reported and are not classified as negative.

This evaluates discovery order on one measured workload, not general superiority across applications, live search duration, or execution repeatability for every scenario. The six preselected repeat checks are reported separately. Execution deviations and incomplete assessments remain part of the reference map and must accompany its interpretation.

## Measurement coverage

The 548 unavailable complete scores consist of:

- 222 application executions ending in COMPENSATION_FAILED: recovery tries to access
  Tournament aggregate 12 after it has been deleted. The runner records a nonzero
  process exit; inspection of the execution reports identifies the application
  compensation failure. These are not infrastructure failures or zero-impact cases.
- 246 executions with residual-state attribution unavailable due to a competing
  lifecycle change.
- 68 executions with incomplete compensated-read attribution due to an intervening writer.
- 11 timeouts and one process-launch/report error (Operation not permitted).

Of the 4,636 completely scored observations, 2,971 have EXACT and 1,665 DEVIATED
schedule conformance. Both methods receive the same recorded observed outcomes; this
is not a claim that every assigned schedule completed exactly. The comparison
measures prioritization of available impact scores and reports unknowns separately.

## Repeat qualification

All six candidate keys selected before measurement were rerun with the frozen runtime.
All six reproduce the fitness components, coverage, score availability, recorded
status and schedule conformance. They include positive scores 5 and 1, a zero score,
a competing-lifecycle unknown and a compensation-failure unknown. This is a narrow
repeat check, not a repeatability claim for the whole map. See measurement-audit.json.

## Interpretation

At 500 evaluations, GA finds 426.43 positive scenarios versus 382.30 for uniform
random (+11.54%), with available score sums 1,342.00 versus 1,058.77 (+26.75%). At
1,000, the gains are +9.99% in positive scenarios and +24.19% in available score.
The difference decreases toward exhaustive coverage. Reaching every known positive
takes essentially the entire catalogue for both methods; this workload does not show
a material GA advantage for that completion target.

The workload fixes AddParticipant, UpdateTournament, LeaveTournament and RemoveTournament
with a source-derived setup and a fixed normal order. The catalogue varies canonical
fault assignments and recovery schedules. All five criterion weights equal one.
There are no positive lost-copied-update or unresolved-delivered-event counts in this
map; it cannot establish the search value of those criteria or robustness to other
weight choices.

The manuscript can report a quantitative benefit at finite budgets for this workload.
Broader superiority across workloads/apps and live computational performance require
separate evidence. Figures report mean curves and the 10th–90th seed percentiles,
not confidence intervals across applications.
