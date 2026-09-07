# Compensated Saga creation read qualification

Experimental driver and isolated Docker runner for the approved
[protocol](../../../issues/2026-09-07-compensated-saga-read/M2-PROTOCOL.md).
Production detection uses the generic collector/assessor; this harness supplies real
application actions and exact source occurrence/checkpoint evidence to that same path.
It does not add runtime result binding to generated scenarios.

## Run

Use an independent absolute directory and a private Maven repository. The runner pins
its JDK21 image, limits each container to two CPUs/3 GiB and starts each case in a fresh
JVM/Spring/H2 process. It never mounts shared Maven targets or caches. Coordinate Docker
capacity before starting; the runner starts only one container at a time.

```sh
python3 verifiers/experiments/saga-read-exposure/qualification.py plan
python3 verifiers/experiments/saga-read-exposure/qualification.py prepare --run-root /absolute/private-run
python3 verifiers/experiments/saga-read-exposure/qualification.py run --run-root /absolute/private-run --run-id qualification-01 --package /absolute/private-run/ordinary-control-package/scenario-catalog-manifest.json --scenario b2f2c737ada60d3e7171259f188ffcb0281feaa3364a6b2e2743bec6c6c0760a
python3 verifiers/experiments/saga-read-exposure/summarize.py /absolute/private-run/reports/m2/qualification-01
```

The ordinary package must be copied intact under the private run directory before running.
The scenario ID above belongs to the frozen setup-backed FindQuiz control recorded in
this issue; it is not a universal ID for regenerated packages. Preserve the manifest and
all its referenced artifacts. Preparation requires the pinned image locally and network
access for Maven dependencies and the hash-verified JOL tool. It refuses to overwrite an
existing frozen source snapshot. Preserve failed builds/runs and prepare a fresh snapshot
after changing measured source; never patch a snapshot after its manifest is recorded.

`--job` can select predeclared jobs for debugging, but a partial matrix cannot pass the
final summarizer. Every run keeps its literal command, exit status, logs, artifact hashes
and build provenance. Commands completing successfully alone are not semantic acceptance.
The summarizer verifies all 22 jobs, diagnostic persistence/counts, referenced hashes and
paired execution/ImpactV1/ImpactV2 equality, retaining any differences in `summary.json`.

## Interpretation

Controlled positives pass the Quiz ID actually created by A to B. The ordinary executor
control uses a setup result and establishes sidecar persistence independently. The outer
Tournament contract covers its own revision; its nested Quiz reference is unsupported.
The experiment-only clock fixture fixes DateHandler.now in both modes. Application values,
revisions, lifecycles and dependencies are compared verbatim. Only explicitly recorded
run-attempt identifiers (including exact setup-result prefixes and framework fault messages),
framework creation timestamps and the measured source-setup duration are normalized for
comparisons. Every original normalized value is retained in the audit. Runtime Logback
files are recorded as generated outputs, with all frozen source hashes checked separately.

Action durations exclude startup, setup, assertions/probes, final snapshots, JOL and JSON
serialization. The fixed cost protocol has one excluded warmup pair and three measured
pairs. Report every sample and median paired difference; it cannot establish statistical
significance or scaling. JOL measures the inclusive object graph reachable from the
collector and report, including shared references. It does not measure exclusive retained
heap, peak allocation or whole-application overhead. No JOL dependency is added to production.

The diagnostic retains all metadata without a cap. Identity/revision joins are indexed,
but per-call history and fallback scans can cost `O(R * W_same_identity + R * A)`; no global
linear-time claim is made. Findings describe exposure, independently of harm or scores.

The qualified matrix, all samples, initial audit failures and their evidence-backed
resolution are documented in [RESULTS.md](../../../issues/2026-09-07-compensated-saga-read/RESULTS.md).
