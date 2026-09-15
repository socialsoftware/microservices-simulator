# Fixed-workload GA evidence

The [experiment results](../../../../verifiers/experiments/fixed-workload-ga/RESULTS.md)
explain the domain, fixed workload, no-fault rejection control, GA/random comparison and
three independent read-exposure witnesses.

[summary.json](summary.json) is the compact audit of 53 fresh application executions:
48 search attempts, two controls and three witnesses. It retains per-attempt joins, hashes,
I/A availability and coverage, population lineage checks and per-arm discovery accounting.
The 6–2 result for seed 29 came from initialization/random fallback; it does not establish
an evolutionary advantage. Seed 11 executed three new crossover children.

Raw package snapshots, reports, commands and source/runtime provenance are under
`verifiers/target/fixed-workload-ga/qualification-01/`. The separate
`verifiers/target/fixed-workload-ga/generation-smoke-01/` validates real on-demand persistence
and deduplication without additional application executions. Search scope and generator
caps were selected before outcomes; no weights or parameter tuning were introduced.
