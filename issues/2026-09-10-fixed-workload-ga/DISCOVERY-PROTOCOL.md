# Positive-discovery evaluation

The user approved evaluating discovery speed using the current I before changing the
feedback. The frozen machine-readable protocol is
`verifiers/target/fixed-workload-ga/discovery-01/protocol.json`.

The selected RemoveTournament/AddParticipant workload and runtime match the first
qualification. Complete the eight unseen candidate keys, retaining the 21 already
measured keys and their report hashes. The complete reference contains 29 scenarios;
its outcomes are an evaluation denominator and are never passed to the search policy.
The 21 reused reference keys do not count as new application executions.

Compare GA and random with seeds 11, 29 and 47 and budget 29. Population stays 8,
mutation 0.3, proposal-stall limit 100. Each strategy owns its package and cache; every
attempt starts a fresh Docker/JVM/H2 and obtains its own I/A reports. Up to two independent
containers run concurrently. Reuse the identical-runtime qualified no-fault control after
checking scope and evidence hashes. New cost is at most 182 application executions.

Primary observations:

- Cumulative distinct I-positive candidates after each real application execution.
- Execution count to first positive and at least 50%, 80% and 100% of reference positives.
- Unmet targets and the actual stop point, including stalls before exhausting the budget.

Also retain best I, null fitness, duplicates, new crossover children versus random
fallback, and wall time. Compare paired seeds descriptively. Three seeds on one known
workload are not a population-level effectiveness or significance claim. No tuning based
on this campaign and no automatic reruns to replace an unfavorable or stalled arm.

The algorithms optimize available I, whereas positive discovery counts every I>0 candidate
once. These objectives coincide more closely in this benchmark's binary 0/2 landscape
than in a richer future score. Positive scenarios can expose the same application defect.

Analysis runs after the campaign. It independently validates reference/current reports,
replay snapshots, input-package integrity, attempt/container uniqueness and chronological
parent feedback. Missing measured feedback cannot reveal a reference-positive label to
the algorithm or inflate discovered-positive counts. Tests cover target rounding,
censored results, duplicates, mismatching scores and a zero-positive reference.

A score-policy change requires a newly assessed complete reference before reusing these
coverage fractions. Anomalies remain separate observations during this experiment.
