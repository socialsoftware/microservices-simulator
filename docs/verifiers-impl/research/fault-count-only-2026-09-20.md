# Count-only fault scenarios

Approved on 20 September: count workloads and canonical fault scenarios, including
supported event selections/placements and recovery, without materialising catalogues.
Validate against complete small generation, then measure all accepted Quizzes inputs
for sets of two, three and four distinct Saga types. Cluster use requires a current
booking; no previous booking remains valid. Do not change generator/scoring semantics.

Implementation and 2–4 Saga Quizzes counts complete. See
[verified results](../evidence/fault-counts-2026-09-20/RESULTS.md). Existing normal-order
totals remain authoritative only for their documented event-free scope. New totals must match the generator's workload
and scenario identities, not deduplicate different workloads by observed outcome.
All-zero assignments count as fault scenarios. Canonical choices are no fault or the
first fault per Saga. Counts describe structural candidates, not successful execution.

The recovery generator already has an exact dynamic-programming counter per workload
and vector. The new aggregate counter must also preserve skipped normal steps in
workload identity, forced no-op event ordering, compressed segment tails, and the
current rule that combined deliveries come from one emission site only.

## Completion

- `RecoveryScheduleGenerator.count` performs no materialisation.
- The structural counter explores progress states rather than workload paths; a next
  visible action is selected before recovery choices. Hidden normal steps still
  distinguish workloads, but do not introduce duplicate compensation positions.
- Failure histories merge only when their possible continuations match: live, failed
  before/at emission, or failed after emission. Remaining compensation length is part
  of the state. Scalar structure answers are reused across accepted input tuples.
- Production opt-in `count-fault-scenarios` in COUNT_ONLY mode writes supplementary
  per-set rows and complete/null totals. Existing generation/scoring are unchanged.
- 18 Quizzes enumeration comparisons materialised 249,621 scenarios with exact
  agreement; focused Spock and the real Spring dummyapp count-only path passed.
- All 74,481 Quizzes sets were counted in all three profiles. Largest case (four Saga
  types) completed locally; no cluster booking was necessary. Final compact tables
  occupy about 3.2 MB. No full global catalogues were created.
- The initial unreduced-state feasibility pilots hit guards and were stopped after
  an equivalent smaller state representation passed tests. Their partial output is
  labelled INTERRUPTED and excluded. The final runs have zero incomplete sets.
- A broad application test run was stopped while an unrelated existing Quizzes
  catalogue test used substantial memory. The affected count-only Spring tests were
  then run explicitly with a 768 MiB heap and passed. Do not claim the whole application
  test suite passed; the source count/recovery suites passed separately.

Next: resume the paper review from the newest editorial handoff. Use the new complete
counts for the workload/fault-scenario table; retain the separate bounded preservation
experiments. Do not silently substitute structural counts for executable-case counts.
