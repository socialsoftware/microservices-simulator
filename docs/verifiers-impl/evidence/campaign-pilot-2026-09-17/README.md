# Resumable map collection and local capacity pilot

The measurement runner now supports pause/drain/resume and a configurable number of
isolated Docker executors. This changes campaign operation, not application schedules,
detector scope, fitness weights or the search operators. The full 5,184-case campaign
has not been launched by this pilot.

## Design

Use the same frozen runtime, exact successful control, and complete 72-case catalogue
as the catalogue-live qualification. Select twelve evenly spaced sorted structural keys
before executing them (`sorted(keys)[::6]`). Each worker setting (2, 3, 4) executes these
same twelve cases in a separate directory, sequentially by setting. Each case uses a
fresh JVM/database, with the existing 2-CPU and 3-GB container limits. The fixed sample
is for capacity selection, not application-wide coverage or GA effectiveness.

A separate three-case proof requests pause while the first case is running with one
worker, waits for `PAUSED` with no in-flight work, then resumes with two workers. The
finished first attempt must remain unchanged and must not run again.

Compare every measured criterion assessment, report status, terminal outcome and schedule
conformance with the retained 72-case reference. Capture Docker resource samples during
execution. Cases per minute use the campaign's active collection time, excluding control
qualification, enumeration and later recorded-feedback search. A single small sequential
batch per worker count can be affected by workload mix, host load and thermal state; it
is a practical capacity pilot, not a statistically controlled performance result.

## Evidence

Raw artifacts: `verifiers/target/campaign-pilot-2026-09-17/`.

- `pilot-protocol.json`, `keys.json`: predeclared configuration and structural selection.
- `pause-proof.json`: observed pause/resume states and unchanged first receipt.
- `results.json`: measured throughput and reference agreement per worker count.
- `workers-*-resources.json`: sampled Docker CPU/memory statistics.
- `workers-*/`: isolated attempts, report hashes, completion receipts and measured map.
- `pilot.py`, `*-command.json`, logs: reproducible orchestration and exact commands.

A real Docker orphan check also confirmed that an existing container mounted to the campaign
directory blocks resume until it exits (`orphan-proof.json`).

Nine new Python tests cover bounded dispatch, in-flight draining, null-score retention,
recovery of completed-but-not-checkpointed attempts, interrupted attempt numbering,
tamper/missing evidence, concurrent coordinator exclusion, orphan containers and frozen
configuration. Together with the existing suite: 69 tests passed.

## Results

| Concurrent executors | Twelve cases | Cases/minute | Matches retained reference |
| --- | --- | --- | --- |
| 2 | 232.08 s | 3.10 | 12/12 |
| 3 | 208.65 s | 3.45 | 12/12 |
| 4 | 247.38 s | 2.91 | 12/12 |

Use three workers for the next local campaign: throughput was 11.2% higher than with two.
Four were slower in this pilot despite higher sampled CPU use. Peak sampled memory summed
across executors was 1.13, 1.56 and 2.04 GiB respectively; these figures exclude other
containers and VM overhead. This does not establish the cause of the slowdown.

Every configuration found the same five positives, six complete zeros and one unavailable
result. The unavailable case is the existing compensation failure, not a new failure under
higher concurrency. In total, 42 real attempts passed retained report/fitness/terminal checks:
36 in the capacity comparison and three in each of two pause/resume proofs. The final CLI
proof paused after two completed attempts and resumed to three without changing or repeating
either finished attempt. The benchmark's archived runner and `measure_map.py` differ only
in the runner's own filename; the historical `campaign.py` is unchanged.

A direct throughput projection gives approximately 25 hours for 5,184 cases at three
workers, excluding enumeration, repetitions and pauses. This is only a planning estimate:
the larger workload has four Sagas and may have a different cost distribution. No full map
or new GA effectiveness campaign was started.

## Disk cleanup

Preserved all report bundles, prepared builds and local/SNAPSHOT Maven artifacts. Removed
938 regenerable third-party release JAR cache files (403 MiB) from the two old space-map
qualification Maven caches. Their paths and hashes are retained in the raw cleanup inventory;
rebuilding with those caches may redownload the dependencies. The current runtime's hashes
still verify.

Also checked and re-cloned 7,780 byte-identical large package snapshots using APFS copy-on-write,
with hashes checked before replacement. Every path and file content remains available. Their
16.6 GiB logical size was mostly already shared: available space changed by only about 36 MiB,
not by 16.6 GiB. Do not use summed directory sizes as an estimate of reclaimable APFS space.
Approximately 9.5 GiB remained free after the cache cleanup; the full campaign still needs a
storage estimate and sufficient headroom before launch.
