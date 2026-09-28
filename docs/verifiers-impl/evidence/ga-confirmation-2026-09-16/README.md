# Second-workload confirmation of uniform GA exploration

Completed in **23.38 minutes** from qualification through comparison. The complete
72-candidate map contains **29 positives (all score 2), 41 complete zeros and two
unavailable scores**. All six preselected repeats matched feedback and execution
status/conformance. Frozen sources and feedback validation passed.

## Results

Mean distinct positive scenarios discovered over 30 seeds:

| Evaluations | Original GA | GA with uniform unseen exploration | Uniform random |
| --- | --- | --- | --- |
| 25 | 9.93 | 12.93 | 10.30 |
| 50 | 20.13 | 22.07 | 20.73 |

At 25 evaluations the frozen variant beats uniform random in 23 seeds, ties in five
and loses in two; at 50 these counts are 20/3/7. Reaching 15 of the 29 known positives
takes 28.67 evaluations on average for the variant versus 35.50 for uniform random.
Both cover all 72 candidates in all 30 seeds and eventually find the same 29 known
positives. Original GA misses one candidate/positive in one seed. The benefit here is
earlier discovery; it does not imply more distinct bugs or universal superiority.

The positive cases combine an active Tournament referring to a deleted Quiz
(DELETED_DEPENDENCY=1) with the failed removal leaving that Quiz deleted after completed
recovery (FAILED_OPERATION_RESIDUAL=1). Other enabled criteria are zero in all 70
scorable cases. The two unavailable cases fail compensation when loading Tournament 12
after it has been deleted; they consume search budget and remain unknown, not zero.
Of the 70 scorable histories, 57 are EXACT and 13 DEVIATED; a representative deviation
is Leave rejecting a student who is not enrolled. Fitness describes the actual history.

This supports the frozen change on a second, smaller workload from the same Quizzes
family. It provides no new evidence about finding concurrency-anomaly positives or
end-to-end live search speed. The six repeats are a bounded reproducibility check.

![Mean positive discovery over 30 seeds](discovery-curves.png)

`results-summary.json` preserves compact counts, comparisons and validation. This plot
is rendered from the unchanged comparison data: the generic raw plot/report retained
stale development-map wording (including 186 in the title); the evidence plot uses the
correct 72-case workload. Raw measured data and frozen search sources are preserved.

## Selected story and relation to earlier evidence

The selected whole-Saga forward order is AddParticipant → LeaveTournament →
RemoveTournament. A student joins, leaves, then the Tournament and its associated Quiz
are removed. Relative to the 186-case development workload, UpdateTournament is replaced
by RemoveTournament. Fault points and valid recovery placements vary; normal forward
order and source-derived inputs remain fixed.

Inputs and setup come from the existing ordinary Quizzes test `add participant, update
tournament, leave, and remove tournament`. No new test, recipe, application code or
provider was added. This exact triple did not supply tuning evidence for the GA variant,
but its operations and source story were already inspected in the four-Saga campaign.
It is a second workload in the same application family, not an independent application.

## Qualification and frozen comparison

- Use the ordinary generator with exact selected inputs, strict conflict evidence and
  whole-Saga ordering. Enumerate every canonical fault vector, with recovery cap 500;
  require no truncation and a complete domain of at most 1,000 candidates.
- Require a fresh SUCCESS/EXACT no-fault control with complete enabled score zero.
  A failed qualification stops the campaign for diagnosis; it does not trigger a search
  for a workload where GA wins.
- Freeze the existing runtime/assessor and all search-source hashes before qualification.
  The only previously approved variant is uniform unseen initialization and fallback.
- Measure each candidate once in a fresh application runtime, reusing the qualified
  control. At most two containers run concurrently. Repeat six preselected keys and
  require matching feedback and execution status/conformance before comparison.
- Compare original GA, original two-stage random, uniform catalogue random and GA with
  uniform unseen exploration, using seeds 1–30, population 8, mutation 0.3 and the same
  five unit weights. Retain unknown scores and actual early stops.
- Every policy receives recorded feedback only after selecting the candidate. No GA
  parameters, fitness weights or operators are changed after selection. Discovery curves
  do not measure end-to-end live search time or hide catalogue enumeration cost.

The source-derived workload was generated in 19.09 seconds. Exact enumeration found
72 candidates across 36 canonical vectors, with no truncation. The fresh no-fault control
is SUCCESS/EXACT with complete score zero, taking 44.96 seconds while enumeration ran
concurrently. Measurement reuses that control, executes the other 71 cases and six repeat
checks, then runs all recorded-feedback comparisons. Measured wall time from qualification through comparisons was 23.38 minutes with two workers.
This is smaller than the 186-case development map; interpret it as another workload,
not a scalability result. Useful shared discovery checkpoints include 25 and 50 evaluations.

## Operation and evidence

`selection.json` records input IDs, prior exposure, selection criteria and source hashes.
`run_campaign.py` runs qualification, the complete map, repeat checks and all comparisons
in sequence. It stops if frozen search source changes. Raw generation commands/proof,
qualification, reports, trace files and logs live in
`verifiers/target/ga-confirmation-2026-09-16/`. An idle-sleep inhibitor follows the process.

```sh
cat verifiers/target/ga-confirmation-2026-09-16/status.json
cat verifiers/target/ga-confirmation-2026-09-16/reference/status.json
```

The second status file appears when measurement starts. Final comparison outputs are
in `variant/REPORT.md`, `variant/comparison.json` and `variant/discovery-curves.*`;
`uniform/` and `reference/` retain the baselines. Do not modify the fixed-workload Python
sources while this frozen campaign is running.
