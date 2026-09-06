# Prepared batch execution

Run an explicit frozen selection repeatedly, using the existing Docker prepared-build
launcher. Each attempt has a new container/JVM and the `test,sagas,local` profile's new
in-memory H2 database. This is a small measurement harness outside the Java pipeline,
not a search algorithm or a catalogue qualification service.

## Approved brief and execution route

Direct implementation in the existing checkout was explicitly approved, including local
commit and technical documentation. Reuse the prepared compiler output and existing
launch scripts; freeze six known executable cases before new outcomes; compare three
repetitions at concurrency 1 with three at concurrency 2. Preserve every attempt,
distinguish invalid measurement from evaluated zero, and estimate a cautious search
budget on this computer. A subsequent explicit correction removed the global time cap:
only a normal per-attempt timeout remains. The 36 attempts are the comparison design,
not a prohibition on justified checks. No retries, metric/policy/GA/domain changes,
meeting-note edit, IDE changes, worktree, push, merge or PR.

Implementation sequence: inspect source/build/package compatibility; freeze selection
and expectations; test runner failure contracts; execute both groups; recheck artifact
identities and semantic results; document measured costs and limitations. This direct
brief needs no separate SPEC/PLAN package or additional approval gate.

## Reproduce

From the repository root, with the existing executor image and retained run-02 build:

```sh
python3 verifiers/experiments/batch-execution/run.py freeze \
  --reference-run verifiers/target/combined-event-deliveries/run-02 \
  --output verifiers/target/batch-execution/selection.json

python3 verifiers/experiments/batch-execution/run.py run \
  --selection verifiers/target/batch-execution/selection.json \
  --output verifiers/target/batch-execution/NEW_RUN \
  --concurrency 1 2 --repetitions 3 --timeout 180

python3 verifiers/experiments/batch-execution/summarize.py \
  --run verifiers/target/batch-execution/NEW_RUN
```

Freeze only once; subsequent runs use the existing selection. Output directories and
selection files must be new. `--concurrency 1` or `2` also runs a single comparison
group. Positive repetitions and timeout are configurable; concurrency is restricted
to 1 or 2. There is no global temporal-budget mechanism. Each container is limited to
2 CPUs and 3 GiB; Java uses `-Xmx1536m -XX:MaxMetaspaceSize=512m`. At most two containers
run at once. The timeout terminates only the exact named container for that attempt
and its Docker CLI process group; unrelated containers are never stopped.

The retained build/package is a local prerequisite, not shipped in Git. If unavailable
or incompatible, use the existing
[combined-event campaign](../combined-event-deliveries/README.md) to prepare a new frozen
source build, generate a current package and establish reference reports, then freeze
from that new run. Do not adapt old package contracts or scenario IDs to bypass the
reader. Compilation/generation are outside the repeated-attempt cost and are **not
new capabilities introduced by this runner**. Their fresh cost was not measured here.

`freeze --cases CASE ...` chooses explicit rows from the reference run. The reference
format is the existing combined-event plan (`manifest`, `manifestSha256`, `rows`, with
`cohort`, `caseId`, `workloadId`, `faultScenarioId`, `faultVector`) plus its prepared
build, source hashes and complete joined reports. This is deliberately a narrow reuse
contract, not automatic selection from an arbitrary catalogue. Defaults are:

| Case | Terminal | Complete score | Positive condition |
| --- | --- | ---: | --- |
| none | SUCCESS | 2 | DELETED_DEPENDENCY × 2 |
| answer | SUCCESS | 1 | DELETED_DEPENDENCY × 1 |
| quiz | SUCCESS | 1 | DELETED_DEPENDENCY × 1 |
| answer-quiz | SUCCESS | 0 | None |
| quiz-answer | SUCCESS | 0 | None |
| answer-quiz-trigger-fault | COMPENSATED | 1 | FAILED_OPERATION_RESIDUAL × 1 |

All expect EXACT conformance and COMPLETE assessment. Selection and expected outcomes
come from prior known evidence, before the new campaign. One removal fixture supplies
all six cases. The known missing-third-receiver negative case is deliberately outside
this executable performance sample; this is not a six-Saga or catalogue-wide sample.

## Identity, measurements and failure accounting

The runner compares current production/test sources against the reference source
hashes and copied build sources. It freezes every prepared-build file hash, validates
the manifest and its declared file hashes, and fingerprints external Maven jars and
the Docker image. Sources, package, build, jars, image and launcher are checked again
after execution. Maven cache contents must remain stable during a campaign; this is
checked reuse, not a hermetic image. An image/cache/build change invalidates the run's
integrity claim. The Java reader also validates the selected package at execution.

Each `cN/rNN-CASE/` retains `attempt.json`, the Docker command/log and all reports that
were produced. Reports must join the selected workload/scenario/vector/package and
the same attempt ID. UUID uniqueness is checked across the campaign. Semantic checks
compare terminal state, conformance, assessment, all three condition counts, V1/V2
scores, action outcomes and final lifecycle counts. UUIDs, timestamps and numeric
database identities are not compared literally across fresh processes. This is a
bounded semantic comparison, not equality of every final database field; exact event
and object evidence remains in the raw reports and prior event qualification.

`PASS`, `SEMANTIC_DIVERGENCE`, `PROCESS_FAILURE`, `TIMEOUT`, `INVALID_REPORT` and
`INFRASTRUCTURE_FAILURE` remain separate. Failed/missing/invalid reports have a null
runner score. A well-formed but divergent report retains its actual score alongside
the divergence, so it cannot masquerade as an expected result. All attempts remain in
the throughput denominator; valid expected completions/hour is reported separately.
The runner returns nonzero for any failed attempt or integrity check. It does not retry
or resume an existing output directory. If externally interrupted, completed attempts
and the in-progress records remain on disk; the interruption is not a completed run.

`plan.json` freezes resource limits, identities, selection, counts and timeout before
the first measured attempt. `results.json` records both group wall times and all attempt
records. `summarize.py` rechecks report hashes, joins and semantics and writes
`measurements.json`, with count/min/median/mean/nearest-rank p95/max:

- **Preparation:** current run's integrity checks and dependency fingerprint. It does
  not include historical compilation/generation or the separate selection-freeze command.
- **Attempt wall time:** monotonic timer around Docker launch through process exit,
  including cleanup when timed out.
- **Spring startup:** existing `Started ScenarioExecutorCli in ... seconds` log timer;
  absent/ambiguous log entries remain unavailable.
- **Setup:** existing `sourceSetup.durationNanos` and prerequisite timer in the report.

Remaining wall time mixes JVM/Docker startup, package loading, target actions, impact
collection, report writes and shutdown. It must not be labelled pure action execution.
No new production instrumentation or speculative timing precision is added. Groups
run in the specified order with no warm-up exclusion; page/JVM-image caches and host
load can affect the concurrency comparison. Do not infer a causal speedup or a broad
search budget from this small sample alone.

## Validation

```sh
python3 -m unittest discover -s verifiers/experiments/batch-execution -v
```

Tests use real short-lived child processes for timeout, nonzero exit, missing and
malformed reports, launch failure and preservation, plus report-identity, semantic-zero,
divergence and package-drift checks. These are orchestration tests outside the Java
verifier contract; no generic parser/executor behavior changed or requires new Spock
fixtures. The measured campaign provides the fresh Docker/H2 application proof.

See [RESULTS.md](RESULTS.md) for this computer's measurements and evidence links.
