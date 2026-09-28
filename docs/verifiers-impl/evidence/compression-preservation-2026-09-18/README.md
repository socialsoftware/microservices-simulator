# Compression preservation: declared runtime comparison

The [experiment protocol](../../../../verifiers/experiments/compression-preservation/README.md)
fixes the original Tournament update/query inputs before measurement.

- Full: six forward orders, 72 canonical workload/vector pairs, 98 scenarios.
- Compressed: three forward orders, 36 canonical workload/vector pairs, 40 scenarios.
- Every compressed scenario is an exact member of the full set.
- Both sets have byte-identical input, setup and copy-contract artifacts.
- Includes no fault and all one-fault-per-Saga assignments, including faults in both
  Sagas, with uncapped recovery enumeration for each vector. No selected event deliveries.

The [completed results](RESULTS.md) and detailed comparison are retained here. Raw
runtime reports and execution progress are under
`verifiers/target/compression-preservation-2026-09-18/run/`.
The run's `RESULTS.md` and `comparison.json` distinguish complete comparisons from
provisional or incomplete observations. One observation per scenario is planned.
