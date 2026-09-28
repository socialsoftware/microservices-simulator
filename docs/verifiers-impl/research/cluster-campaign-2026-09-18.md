# Cluster collection: three Join/Update/Query orders

This collection extends the [workload qualification](../evidence/workload-unlock-2026-09-18/README.md).
It measures complete fault catalogues for three existing WorkloadPlans with the same
AddParticipant, UpdateTournament and FindTournament inputs. AddParticipant finishes
before UpdateTournament begins. The query occurs before the update, immediately after
its Tournament write, or after the update finishes. This rule was frozen before
collecting their fault outcomes; positive density and GA performance are not selection
criteria. These are related orders of one input tuple, not three independent applications.

Each order must first pass a fresh no-fault control: SUCCESS, EXACT conformance and
complete zero score under all five criteria. For the during-update order, a known
positive fault is also repeated and checked against the Mac qualification. Only then
is its entire fault catalogue enumerated and measured. Catalogue truncation is an
error; collection does not silently treat capped enumeration as complete. Four workers
execute scenarios, with two CPU cores and 3 GiB allowed per container. Search replay
with recorded feedback is a later step, not part of these application executions.

## Provenance and environment

- Host: proteina06, accessed through the user's authenticated SSH session over the VPN.
- Shared reservation: 18 September 22:03 to 19 September 01:00, Europe/Lisbon.
- Booking: `4ei6bliv91kj7okv6lfjara95c`. The inaccessible vitamina01 reservation was cancelled.
- Cluster policy requires virtualisation: Docker runs inside an Ubuntu 22.04 VirtualBox
  VM, not on the host. Initial VM allocation: 16 vCPUs, 24 GiB RAM. The subsequent 16-worker
  capacity pilot uses 32 vCPUs and 40 GiB RAM.
- Application/verifier bytecode, dependencies and copy-instrumentation sources come
  from the frozen Join/Update/Query qualification. All 3,436 transferred files were
  checked against SHA-256 hashes after extraction.
- The Java 21 Maven container is AMD64 on this host. Its image identity differs from
  the Mac image and is recorded in `deployment.json`. Fresh controls check behaviour;
  this is not a runtime-performance comparison between the two machines.
- Selection, runtime hashes, descriptor, scripts and attempt reports are retained.
  No detector or application semantics were changed for this deployment.

## Files and operation

Host directory: `/home/andremmsilva/thesis-campaign-2026-09-18/`.
VM directory: `/home/vagrant/thesis/verifiers/target/cluster-results/`.
Local deployment bundle: `verifiers/target/cluster-proteina06-2026-09-18/` (ignored artifacts).

The collector runs independently of the Mac SSH connection. It stops launching work at
00:53 Lisbon; a separate host process halts our VM at 00:59, before the booking ends.
A separate host monitor saves status once per minute and copies the evidence archive
out of the VM on completion or at 00:56. Completed attempts have receipts and hashes. `measure_map.py` can resume an interrupted
map after the VM is restarted during another valid booking; restarting `collect.py`
from scratch is not the resume procedure.

`campaign-status.json` records controls, enumeration and map stages. Each `map-*/status.json`
contains measured/total counts. An initial deployment attempt encountered Linux file
ownership differences; its evidence is retained under `deployment-attempts/`. The
collector runs with elevated permissions inside its own VM, consistent with the
cluster VM workflow; host permissions are unchanged.

Final counts and findings must come from the completed maps. Do not reuse the 52-case
count from the older, rejected forward order or present this collection as a second
5,184-case campaign.

## Initial cluster qualification

The during-update control passed with SUCCESS, EXACT conformance and complete score
zero. The selected fault repeated the prior qualification: PARTIAL_COMPENSATED with
EXACT conformance, one failed-operation residual and one compensated-read exposure
(weighted score 2). All five component counts and coverage values matched. The
collector proceeded to full enumeration. `launch-status.json` in the local deployment
bundle retains this checkpoint; it is not the final collection result.

## Completed initial collection

All three maps completed: 156 distinct fault scenarios, with 60 positive and 96 zero
weighted scores; none had an unavailable combined score. The no-fault controls passed
for all three orders. Collection took about 35 minutes including catalogue generation
and controls; application-map execution alone took about 23 minutes.

| Query position | Scenarios | Positive | Zero | Map execution |
| --- | ---: | ---: | ---: | ---: |
| During update | 48 | 12 | 36 | 426 s |
| Before update | 36 | 12 | 24 | 321 s |
| After update | 72 | 36 | 36 | 635 s |

The original evidence archive was copied to the host and downloaded locally as
`results-initial.tar.gz`. These results describe the selected orders. The subsequent local GA comparison
is reported below under Recorded-feedback comparison.

## Parallelism pilot

The same 48 during-update candidates are repeated with eight workers, on the same
16-vCPU VM, to compare elapsed map time and verify per-candidate component scores,
coverage and terminal outcomes against the four-worker run. This is an operational
capacity check, not a statistical performance evaluation. Results are written to
`parallelism-8-summary.json`; CPU and available-memory samples are retained separately.

The eight-worker pilot completed all 48 cases in 158.50 s, compared with 425.86 s
for the original four-worker map. All compared outcomes and five-component
assessments matched. The observed throughput ratio is 2.69; this single sequential
comparison includes possible cache and shared-host effects, so it is not a general
speedup estimate. A 16-worker pilot uses a larger VM to avoid fitting 32 requested
container CPUs into only 16 virtual CPUs.

The 16-worker pilot completed in 198.90 s, again with no differences in compared
results. Eight workers were the fastest tested configuration:

| Workers | VM vCPUs / RAM | Same 48 scenarios | Scenarios/minute |
| ---: | --- | ---: | ---: |
| 4 | 16 / 24 GiB | 425.86 s | 6.76 |
| 8 | 16 / 24 GiB | 158.50 s | 18.17 |
| 16 | 32 / 40 GiB | 198.90 s | 14.48 |

Use eight workers on the 16-vCPU, 24-GiB VM for the next comparable collection.
These are single sequential trials, and the larger VM was rebooted before the
16-worker trial. The pilot supports an operational choice; it does not isolate
the cause of the difference or establish an optimal parallelism for every workload.
The two repeat maps are capacity/reproducibility checks, not additional distinct
fault scenarios. Their evidence is archived separately in `parallelism-results.tar.gz`.
All planned collection and pilot work has finished; the VM is halted to release
resources. Its next-start configuration returns to 16 vCPUs and 24 GiB.

## Reservation extension collection

At the user's request to collect as much as possible before 01:00, the VM was
restarted with the selected 16-vCPU / 24-GiB configuration. The extension schedules
eight workload lanes; each lane runs at most one generator or application container
at a time. It does not multiply eight lanes by eight scenario workers.

The frozen queue contains 286 additional WorkloadPlan IDs: 21 Join/Update, 165
Join/Update/Query and 100 Join/Update/Leave/Remove. These come from existing source
packages, with duplicate IDs removed and completed workload IDs excluded. Within
each family, fewer switches between participants in the forward schedule come first,
then workload ID; families are interleaved round-robin. This ranking is structural,
not based on observed positives or GA performance. The source packages have their
own generation bounds, so this is not every possible application workload.

Each admitted map requires a fresh SUCCESS/EXACT, complete-zero control. Rejections
and errors are retained explicitly. Valid controls proceed to full catalogue
enumeration with recovery cap 10,000; truncation is rejected. Each map then executes
with one worker, up to eight maps concurrently. Queued plans are not measured cases.
Deadline-stopped maps must be distinguished from complete maps in all reporting.

New work stops launching at 00:50 Lisbon. The separate host backup starts when the
coordinator terminates or at 00:54, before the existing 00:59 VM halt guard. Evidence
is saved on the host as `extended-results.tar.gz`. Live host status is
`extended-status.json`; VM evidence lives in
`/home/vagrant/thesis/verifiers/target/cluster-extended/`.
The frozen local selection and orchestration scripts are in the deployment artifact
directory as `extended-selection.json`, `extended.py` and `extended-backup.py`.

Map-level `measure_map.py --resume` retains completed attempts, with a new valid
reservation. The extension coordinator itself starts a fresh output directory and
must not be rerun as if it were a resume command. Interrupted catalogue generation
requires separate continuation/reconstruction before fault-map execution.

## Verified extension results

The extension stopped accepting new work at 00:50 Lisbon and drained its current
attempts by 00:50:26. The host backup completed at 00:52:50. The deadline guard ran
at 00:59; the host subsequently reported `VMState="poweroff"`. No VM restart or
application computation was performed after the reservation. The archive was
downloaded and inspected locally.

Of 286 queued WorkloadPlans, 27 reached their no-fault control:

- 14 passed the SUCCESS/EXACT/complete-zero gate. Six fault maps completed, seven
  paused at the deadline, and one stopped during catalogue generation.
- 13 failed the control gate and did not proceed to fault collection.
- 259 queued plans were not started. They are neither rejected nor qualified.

The extension measured **825 distinct (workload, canonical scenario) pairs**:

| Collection | Measured | Positive score | Zero score | Score unavailable |
| --- | ---: | ---: | ---: | ---: |
| Six complete maps | 276 | 134 | 142 | 0 |
| Seven partial maps | 549 | 365 | 179 | 5 |
| Extension total | 825 | 499 | 321 | 5 |

The five criterion weights were all 1. Score frequencies were 321 at 0, 398 at 1,
60 at 2, 18 at 3 and 23 at 5, plus five unavailable. These count positive scenarios,
not distinct bugs. A single aggregate can contribute to multiple criteria.

All 825 attempt receipts matched their attempt-file hashes and canonical keys;
all referenced report hashes matched, and the counts agreed with the saved map
statuses. No duplicate canonical candidate was found within a workload.
This integrity check does not replace a scientific replication of each execution.

### Complete maps

| Workload ID prefix | Sagas | Scenarios | Positive | Zero |
| --- | --- | ---: | ---: | ---: |
| `67a72159f5` | AddParticipant + UpdateTournament | 52 | 30 | 22 |
| `2f62ebc8f9` | AddParticipant + FindTournament + UpdateTournament | 36 | 12 | 24 |
| `7c26d665df` | AddParticipant + UpdateTournament | 18 | 6 | 12 |
| `dfc87108c9` | AddParticipant + FindTournament + UpdateTournament | 104 | 60 | 44 |
| `806b968b0f` | AddParticipant + UpdateTournament | 30 | 14 | 16 |
| `0c3d423234` | AddParticipant + FindTournament + UpdateTournament | 36 | 12 | 24 |

All positives in these six complete maps have failed-operation residual findings;
none has a positive compensated-read, deleted-dependency, unresolved-event or
lost-copied-update count. The positive fractions range from 33.3% to 57.7%.
This collection has not yet found a completed workload with rare positives.

Together with the three initial maps, there are now **nine complete maps containing
432 scenarios: 194 positive and 238 zero**, with no unavailable combined scores.
The subsequent GA/uniform-random replay comparison covers all nine maps, as reported below. These remain related Quizzes workloads, not
nine independent applications.

### Partial maps

| Workload ID prefix | Sagas | Measured / catalogue | Positive | Zero | Unavailable |
| --- | --- | ---: | ---: | ---: | ---: |
| `a603fd11d9` | Join/Update/Query | 116 / 160 | 71 | 45 | 0 |
| `c099f2b180` | Join/Update/Query | 116 / 160 | 75 | 41 | 0 |
| `23f9768282` | Join/Update/Leave/Remove | 84 / 3360 | 65 | 18 | 1 |
| `242d3ef997` | Join/Update/Leave/Remove | 80 / 3918 | 66 | 10 | 4 |
| `02cf5305c1` | Join/Update/Query | 94 / 104 | 58 | 36 | 0 |
| `13810131c7` | Join/Update/Query | 37 / 160 | 21 | 16 | 0 |
| `0763c8495d` | Join/Update | 22 / 36 | 9 | 13 | 0 |

These are prefixes of the runner's sorted canonical-key order, not random samples;
their observed positive fractions do not estimate their complete catalogues' density.
In particular, the 3,360- and 3,918-case workloads remain far from complete. Completing
only the 94/104 and 22/36 maps would require 24 additional scenario executions under
a new valid reservation (or an appropriately requalified local continuation).

### Concrete observations and remaining assessment gaps

In workload `242d3ef997`, attempt 039 (`faultVector=010001000011`), UpdateTournament
writes the Tournament and then encounters an injected fault. LeaveTournament and
RemoveTournament both read that written version before its compensation. RemoveTournament
also deletes the Quiz, then encounters an injected fault before deleting the Tournament.
After recovery, the Tournament remains active and refers to the deleted Quiz.
The recorded score is **5 = 1 deleted dependency + 2 objects with failed-operation
residuals + 2 compensated-read exposures**. The two residual objects are the Quiz and
Tournament. The two read exposures are distinct reader Sagas observing the same
Tournament version; they are not two different affected Tournament objects.
This example combines final-state findings and execution-history findings using the
existing detectors, without adding an application-specific harmful-state rule.

No positive lost-copied-update or unresolved-delivered-event finding was recorded
in this extension. The inspected workloads do not establish performance of those
criteria on positive examples.

All five unavailable scores occur in the partial four-Saga maps:

- Two attempts exited with `PROCESS_FAILURE` (009/attempt-080 and 012/attempt-055).
  Their root causes have not been diagnosed by this results inspection.
- Two attempts have `COMPETING_LIFECYCLE_CHANGE` for the Tournament residual
  assessment (012/attempt-019 and 012/attempt-053).
- One has `INTERVENING_WRITER` gaps for the read-exposure assessment
  (012/attempt-016). Its persistent-state assessment is available, but its combined
  five-criterion score is not.

These five are preserved as unavailable, not converted to zero or retried until positive.

The 13 rejected controls contain application exceptions before any injected fault.
Typical examples are an AddParticipant action encountering the Tournament's update
lock, or LeaveTournament attempting to remove a user whose enrollment did not succeed.
One control also reports a deletion lock and an invariant rejection. These histories
are not classified as framework crashes merely because they fail the benchmark gate;
their reports remain available for separate analysis of naturally rejected operations.

### Combined collection and retained evidence

Initial collection plus extension: **981 distinct measured fault scenarios,
559 positive, 417 zero and 5 unavailable**. This total includes partial maps.
It excludes no-fault controls, the known witness repeat, and the 96 scenario repeats
used for the eight- and sixteen-worker capacity trials. It does not include the older
5,184-case Mac campaign.

Local artifacts:

- [Extension archive](../../../verifiers/target/cluster-proteina06-2026-09-18/extended-results.tar.gz)
  (619,753,376 bytes), SHA-256
  `914c6c61456f4141ad3667b303a3448193d3ebba4bb533dec681f34f086e60bc`.
- [Verified counts and representative findings](../../../verifiers/target/cluster-proteina06-2026-09-18/extended-analysis.json).
- [Rejected controls](../../../verifiers/target/cluster-proteina06-2026-09-18/extended-rejected-controls.json),
  [unavailable assessments](../../../verifiers/target/cluster-proteina06-2026-09-18/extended-unavailable.json),
  and [concrete execution/read example](../../../verifiers/target/cluster-proteina06-2026-09-18/extended-concrete-example.json).
- [Initial collection summary](../../../verifiers/target/cluster-proteina06-2026-09-18/collection-summary.json).

The same extension archive remains on the cluster host, outside the halted VM.
The local JSON summaries are derived from the retained archive; scripts
`analyse_extended.py` and `inspect_extended_gaps.py` in the same artifact directory
reproduce this inspection without running the application.

## Recorded-feedback comparison

All nine complete maps were compared locally using the shipped `search.run` and
`RecordedDomain`: GA with uniform unseen exploration against uniform random without
replacement. Both used seeds 1–30, population 8, mutation 0.3, stall limit 1,000 and
five unit criterion weights, with a budget equal to each complete catalogue. No map
was selected or discarded based on GA performance. Each score was revealed only when
its candidate was selected. The seven partial maps were excluded from this comparison.

All **540 searches** visited every candidate in their respective catalogue exactly
once and reached matching endpoint scores and positive counts. Catalogue completeness
and candidate identities were checked, recorded scores were recomputed from component
assessments, and archive/search-code hashes remained unchanged.

| Map | Candidate count | Positive count | Positives at half budget, GA / random | Evaluations to 80% positives, GA / random |
| --- | ---: | ---: | ---: | ---: |
| During-update query | 48 | 12 | 6.67 / 6.07 | 34.80 / 37.70 |
| Before-update query | 36 | 12 | 7.07 / 5.83 | 25.37 / 28.47 |
| After-update query | 72 | 36 | 20.00 / 18.13 | 54.37 / 57.30 |
| `67a72159f5` | 52 | 30 | 16.37 / 15.03 | 38.53 / 41.10 |
| `2f62ebc8f9` | 36 | 12 | 6.70 / 6.23 | 25.90 / 28.60 |
| `7c26d665df` | 18 | 6 | 2.93 / 2.97 | 13.33 / 13.57 |
| `dfc87108c9` | 104 | 60 | 33.23 / 29.97 | 77.83 / 83.40 |
| `806b968b0f` | 30 | 14 | 7.23 / 6.70 | 24.00 / 25.07 |
| `0c3d423234` | 36 | 12 | 7.17 / 5.73 | 25.60 / 28.90 |

All values are means over the 30 seeds. Summing the positive-discovery curve over all
equal-budget checkpoints favours GA on average in eight maps; the 18-case map slightly
favours random. Its slightly lower GA mean time to 80% does not contradict that result:
a single threshold and discovery across the whole run measure different aspects of
ordering. On the 104-case map, the whole-curve comparison favours GA in 28 of 30 seed
pairs, and reaching 80% requires about 6.7% fewer choices on average.

Cumulative weighted-score curves are also retained. In seven maps all positive scores
are 1, so score and positive-count curves coincide. The initial during-update and
after-update maps additionally contain score-2 cases. This is limited evidence for
how search trades off positive counts against higher scores; no weights were tuned.

These data support a modest earlier-discovery advantage across several related
Quizzes workloads. They do not yet supply the sought rare-positive workload: their
positive fractions range from 25% to 57.7%. The comparison measures discovery order
using recorded observations, not live execution speed. Seed percentile bands in the
figures describe variation across runs, not confidence intervals across applications.

Local artifacts:

- [Replay report and workload mapping](../../../verifiers/target/cluster-proteina06-2026-09-18/replay-nine/REPORT.md).
- [Positive-discovery curves](../../../verifiers/target/cluster-proteina06-2026-09-18/replay-nine/positive-discovery.png)
  and [weighted-score curves](../../../verifiers/target/cluster-proteina06-2026-09-18/replay-nine/score-discovery.png),
  also available as PDF and SVG in the same directory.
- [Frozen protocol](../../../verifiers/target/cluster-proteina06-2026-09-18/replay-nine/protocol.json),
  [comparison data](../../../verifiers/target/cluster-proteina06-2026-09-18/replay-nine/comparison.json),
  and [validation](../../../verifiers/target/cluster-proteina06-2026-09-18/replay-nine/validation.json).

`replay_nine.py` and `plot_nine.py` in the deployment artifact directory reproduce the
analysis. The replay uses existing search code unchanged; only artifact orchestration
and reporting were added. The paper has not been edited as part of this comparison.

## Overnight breadth and resumed collection — 19 September

Authorised after the initial results discussion. Shared booking
`1n3ts5cdnkogrh2443q4eo5vr4` reserves proteina06 from 02:00 to 09:00 Lisbon.
The same VM and frozen runtime are reused, with 16 vCPUs, 24 GiB RAM and at most
eight application workers. No application, detector or search semantics change.

The overnight sequence is:

1. Resume the 104- and 36-case partial maps, which need 10 and 14 additional cases.
2. Regenerate the bounded broad package using the frozen runtime and its prepared
   Quizzes source tree. Keep the historical 129-plan/70-family queue as provenance;
   select anew from the regenerated package using the same structural rule: one
   WorkloadPlan per Saga-type combination before a second, preferring available
   source setup, fixed SHA-256 ranking and a different event count in round two.
   The regenerated selection records its actual family and workload counts.
3. Run fresh no-fault controls and retain rejections and preparation failures.
   SUCCESS/EXACT controls with all five criteria complete and zero proceed to
   catalogue enumeration. For this breadth pilot, workloads with more than seven
   binary fault slots are deferred to keep enumeration bounded; recovery cap is
   10,000, and capped catalogues are rejected. Select up to 12 distinct catalogue
   cases by a fixed hash ordering, independently of scores. A completed pilot is
   a complete map only when the full catalogue has at most 12 cases.
4. Resume the remaining older partial maps, finishing smaller maps before allocating
   four workers to each of the two large maps. Their earlier attempts are retained
   and verified by the existing resume contract; new counts exclude those attempts.

The regenerated package uses the prepared source snapshot matching the retained
runtime, not a claim of analysis of every current checkout test. Bounds are sizes
1–3, ten inputs per Saga, one serial forward order per input tuple, up to three event
consequences and 50,000 generated catalogue scenarios. Static selection is not proof
of executable inputs. The separate full-input counts remain a different experiment.

The coordinator stops launching work at **08:40 Lisbon**, drains in-flight attempts,
then archives evidence to the host. A separate host process powers off our VM at
**08:59**, independently of the Mac connection. A disk guard requests graceful stop
below 8 GiB free, preserving evidence rather than filling the VM disk. Incomplete
maps remain partial; no missing assessment becomes a zero score.

Host status and evidence: `overnight-status.json`, `overnight-backup-complete.json`,
`overnight-backup.log`, `overnight-results.tar.gz`. VM output is
`/home/vagrant/thesis/verifiers/target/cluster-overnight-v2-2026-09-19/`;
resumed maps remain under `cluster-extended/`. The new archive includes both, while
`extended-results.tar.gz` remains the immutable earlier snapshot. Local orchestration,
source hashes and booking context are retained in
`verifiers/target/cluster-proteina06-2026-09-18/overnight*.py` and
`breadth-source-hashes.json`. Final verified results are to be appended after retrieval;
this section records the launch protocol, not projected findings.

The initial overnight coordinator completed the two near-finished maps, then the
broad generator failed on an unresolved Spring profile placeholder. Its diagnostic
artifacts remain under `cluster-overnight-2026-09-19/`. The corrected orchestration
sets an explicit static generation profile, drains the earlier coordinator and resumes
without repeating completed scenarios. `overnight-v2.py` and
`overnight-backup-v2.py` own the active launch and archive process. Fast gzip compression
keeps archival time within the reservation; both orchestration attempts are archived.

Launch verification: fresh generation exited zero and produced 3,820 WorkloadPlans.
The regenerated queue has 129 plans across 70 Saga-type combinations, with 98 static
setup candidates. All 129 chosen IDs match the historical queue. The first eight
controls were observed running in isolated containers. The two near-complete maps
finished with all 104 and 36 cases respectively, adding exactly 24 attempts; their
second coordinator resumed them with zero additional attempts. Host backup and halt
guards were confirmed running. `overnight-selection.json` and
`overnight-booking.json` retain the launch evidence locally.

A detached local retrieval helper keeps the existing SSH connection in use, mirrors
compact host status every five minutes and downloads the final archive only when the
host's completion receipt exists. It verifies SHA-256 and leaves at least 2 GiB free
locally; if local storage or authentication blocks retrieval, the host archive remains
available. This helper is not required for server-side execution or the deadline guard.
The Codex follow-up is scheduled for 09:02 Lisbon to inspect and explain the results.

## 07:00 capacity check and supplementary continuation

The scheduled check found the coordinator stopped at 05:51 Lisbon, after the disk
safeguard requested a drain below 8 GiB free. All 129 breadth plans had reached a
recorded outcome: 59 complete maps, five completed 12-case pilots, three enumeration
deferrals, 31 rejected controls and 31 preparation/generation errors. These are
operational status counts, not yet a verified score summary. The breadth maps/pilots
reported 341 measured scenarios. The five smaller resumed maps were complete; the
two large maps were paused at 1,312/3,360 and 1,299/3,918.

The VM's existing 128-GiB virtual disk already had 63 GiB free in its LVM volume
group. Extending the root logical volume by 32 GiB online raised available filesystem
space from about 8 to 39 GiB, without a reboot, new virtual disk or evidence deletion.
The original archive stream had completed, but its 120-second tar-list validation
had timed out. `verify-overnight-backup.py` revalidates that unchanged `.part` archive
with a longer timeout before finalizing its checksum receipt.

With this capacity restored, `overnight-supplement.py` resumes only the two paused
maps, four workers each. The starting 1,312 and 1,299 measurements are frozen in
`cluster-supplement-2026-09-19/baseline.json`; no completed attempt is rerun. New work
still stops at 08:40, the 8-GiB disk guard remains, and the independent 08:59 VM
power-off is unchanged. Supplement status and archival are separate from the original
backup so neither the initial archive nor an in-progress download is overwritten.

Host files: `overnight-supplement-status.json`, `overnight-supplement-complete.json`,
`overnight-supplement-backup.log`, `overnight-supplement.tar.gz`.
The supplementary archive contains **new attempt directories and updated map metadata**;
it is an overlay on `overnight-results.tar.gz`, not a standalone replacement. Its
checksum receipt names the base archive. A separate local retrieval helper downloads
it if at least 2 GiB can remain free. The final analysis must combine base and supplement,
then compare against the immutable earlier `extended-results.tar.gz` for nightly gains.
The 09:02 follow-up was restored before this inspection.

## Verified overnight results

Both archives were downloaded to the Mac and their complete SHA-256 digests matched
host receipts. Streaming inspection verified every collected map/pilot attempt receipt
and all report hashes declared by those attempts, checked unique candidate keys and
matched attempt counts to map status. All 825 attempts from the immutable pre-night
extension archive are byte-identical after resume. No integrity mismatches or duplicate
measured candidates were found. Verification did not require extracting the large
archives onto the Mac filesystem.

| Collection | Newly measured scenarios | Positive score | Zero score | Unavailable combined score |
| --- | ---: | ---: | ---: | ---: |
| Continuation of existing maps | 4,314 | 3,296 | 778 | 240 |
| New breadth maps and pilots | 341 | 14 | 327 | 0 |
| **Overnight additions** | **4,655** | **3,310** | **1,105** | **240** |

These counts exclude no-fault qualification executions and repeated capacity trials.
Including the earlier 825 extension scenarios gives 5,480 distinct measured scenarios:
3,809 positive, 1,426 zero and 245 unavailable. The separate original 156-case
collection is not included in that denominator; adding it gives 5,636 distinct cases,
3,869 positive, 1,522 zero and 245 unavailable. Neither figure is the total possible
Quizzes scenario count, and positive scenarios are not counts of distinct bugs.

### Breadth and qualification

The 129 selected WorkloadPlans cover 70 Saga-type combinations. Their outcomes are:

| Qualification outcome | Plans |
| --- | ---: |
| Successful exact control, all five criteria complete and zero | 67 |
| Successful exact execution, but combined score unavailable | 16 |
| Application execution did not complete the intended normal schedule | 15 |
| Static preparation unavailable; no executable no-fault candidate | 31 |

The 31 preparation failures correspond exactly to the 31 selected plans without a
static setup candidate. They are not application executions with zero impact. The
16 successful but incompletely assessed controls also remain distinct from application
rejections. The 67 admitted plans cover 39 Saga-type combinations. Three plans with
eight fault slots were deferred by the declared enumeration bound; the other 64
plans cover 37 combinations and produced 341 measured cases.

Of those 64 plans, **59 have complete maps** (281 cases) and **five have 12-case
samples** (60 cases). The five samples contain only zero scores, but their entire
catalogues have 38, 15, 14, 36 and 17 cases respectively. Sixty additional executions
would complete those five maps. Their unmeasured cases are not classified as zero.

Fourteen breadth cases are positive, across eight WorkloadPlans and five Saga-type
combinations. All fourteen include a failed-operation residual; six also include an
active object depending on an object deleted during execution/recovery. No breadth
case produced a positive compensated-read, lost-copied-update or unresolved-delivery
component. This broad pilot therefore adds execution variety and negative controls;
it does not establish a large individual workload with rare positive outcomes. The
pooled 14/341 rate is not a within-workload GA benchmark.

### Complete and partial maps

The continuation completed all five previously small partial maps. Their complete
sizes are 104, 36, 160, 160 and 160 cases. Together with the six maps already complete
before the night, the extended Tournament collection now has eleven complete maps,
896 cases, 520 positive and 376 zero, all with complete combined scores.

| Large workload map | Measured / catalogue | Positive | Zero | Unavailable | Remaining |
| --- | ---: | ---: | ---: | ---: | ---: |
| Join/Update/Leave/Remove `23f9768282` | 2,137 / 3,360 | 1,661 | 458 | 18 | 1,223 |
| Join/Update/Leave/Remove `242d3ef997` | 2,106 / 3,918 | 1,614 | 265 | 227 | 1,812 |

Both large maps remain partial. They were collected in deterministic catalogue order,
not as random samples, so their observed positive rates must not be extrapolated to
unmeasured cases. The 245 unavailable assessments are entirely in these two maps and
must not be scored zero. Incomplete residual/read evidence and execution/assessment
failures overlap; individual reason counts cannot be added as disjoint failures.

Across this overnight archive plus its earlier extension base, there are **70 complete
maps with 1,177 scenarios**, 534 positive and 643 zero, plus the two large partial maps
and five breadth samples. These 70 maps are related input/order variations, not 70
independent applications. The original three maps add another 156 cases separately.

### What the 07:00 intervention added

The supplementary continuation collected **1,632 additional scenarios**: 825 in the
first large map and 807 in the second. It stopped launching at 08:40 and finished
draining at 08:40:32 Lisbon. The supplementary archive was verified on the host at
08:44:27. The independent guard powered off the VM at 08:59; host inspection after
09:00 confirmed `VMState="poweroff"`. No post-booking VM computation was performed.

### Artifacts

Local directory: `verifiers/target/cluster-proteina06-2026-09-18/`.

- `overnight-results.tar.gz`: 6,609,710,426 bytes;
  SHA-256 `5c005597f0a59d08f6597992b1404e9ab7548e3c3d1a7b9bc31cc5896ca848bc`.
- `overnight-supplement.tar.gz`: 1,980,113,613 bytes;
  SHA-256 `55774581b1ba2513550a8c68501a89647f10c2d713305e9fc815238ef63cb59a`.
- [Verified analysis](../../../verifiers/target/cluster-proteina06-2026-09-18/overnight-analysis.json)
  and [readable per-map counts](../../../verifiers/target/cluster-proteina06-2026-09-18/overnight-analysis.log).
- `analyse_overnight.py` reproduces checksum, receipt, report, uniqueness, continuation
  and score checks directly from the base, supplement and earlier extension archives.

The measurement archives are ready for further recorded-feedback comparisons, but
**no new GA-versus-random comparison was run during this collection**. New curves
must be generated from the complete maps using the agreed search protocol; the older
nine-map comparison does not automatically include these additional measurements.

### Concrete observations and unavailable-score breakdown

In breadth plan `0705b39ad7` (RemoveCourseExecution), attempt 004 injects the fault
before `removeCourseExecutionStep`. The earlier `updateCourseExecutionCountStep`
has already changed Course 1's `courseExecutionCount` from **2 to 1**. Recovery is
reported complete, but the final Course retains 1. The execution being removed remains
active: this is a concrete failed-operation residual with weighted score 1. It
reproduces the previously identified recovery problem on a source-selected input;
it is not counted as a newly discovered independent bug.

In breadth plan `43daecb06b` (FindTournament + RemoveTournament), attempt 006 leaves
active Tournament 12 pointing to deleted Quiz 11 after recovery. The deleted Quiz is
also a residual effect of the failed removal, giving one point in each of those two
criteria (weighted score 2). The query itself faults in this particular scenario,
so this example does not establish a dirty read.

Among the final 245 unavailable combined scores, **124 attempts record PROCESS_FAILURE**
and do not provide a terminal execution report; the other **121 have execution reports**
with insufficient evidence for at least one enabled criterion. Of those 121, 103 also
have a partial persistent-state assessment and 18 have a complete persistent-state
assessment but unavailable combined fitness. Execution-log diagnosis is needed before
attributing process failures to application defects, infrastructure or the harness.
The retained read gaps include `INTERVENING_WRITER`; these are deliberately not guessed
into positive or zero scores. The detailed observations and example reports are in
[overnight-details.json](../../../verifiers/target/cluster-proteina06-2026-09-18/overnight-details.json),
produced by `inspect_overnight.py` from the already hash-verified archives.

## Recorded-feedback comparison after the overnight collection

All 73 complete cluster maps were compared using the unchanged protocol: seeds 1–30,
GA versus uniform random with uniform unseen initialization/fallback, population 8,
mutation 0.3, stall limit 1,000 and five unit weights. Each run receives a score only
when it selects that candidate and exhausts the full catalogue. The 4,380 searches
cover 1,333 measured candidates, including 594 positive and 739 zero outcomes.
All endpoint, score-recomputation, domain-completeness and distinct-visit checks pass;
the earlier nine-map curves reproduce exactly. No application was re-executed.

Fifty-one maps contain no positives. Seven other maps have at most eight candidates,
so they fit entirely inside the initial population and exercise no offspring-based
search. Different paired-seed orders on these small maps arise from random-number
consumption during parent bookkeeping; they are not evidence of adaptive discovery.
These maps remain in the inventory and full results.

Among the fifteen positive maps larger than the initial population, the GA has a
higher mean whole-curve positive-discovery measure in thirteen, while uniform random
has the higher value in two. This is a descriptive comparison across related workloads.
The most useful new results are the three complete 160-case maps, each with 104 positives:

| Forward order without injected faults | Mean positives after 80 selections: GA / random | Mean selections to 80% of positives: GA / random |
| --- | ---: | ---: |
| Update, join, query | 57.10 / 51.40 | 123.00 / 128.37 |
| Update, query, join | 56.93 / 51.87 | 123.60 / 128.33 |
| Update, read joining student, query, add participant | 56.33 / 52.33 | 122.77 / 129.03 |

Thus GA finds about 8–11% more positives at half the catalogue budget in these three
maps; both methods finish with all 104. Their compensated-read findings also give
weighted-score curves that differ from positive-count curves. Recovery schedules can
interleave after faults even though these listed no-fault forward orders are mostly serial.

The two unfinished large maps and five partial breadth samples are excluded from
this complete-map experiment. This replay evaluates discovery order, not live execution
time. It adds no support for a large rare-positive workload or universal GA superiority.

- [Full comparison report](../../../verifiers/target/cluster-proteina06-2026-09-18/replay-complete/REPORT.md)
- [Discovery curves](../../../verifiers/target/cluster-proteina06-2026-09-18/replay-complete/new-160-case-comparisons.png)
- [Validation](../../../verifiers/target/cluster-proteina06-2026-09-18/replay-complete/validation.json)
- Scripts: `replay_complete.py` and `plot_complete.py` in the retained cluster artifact directory.

## Next reservation: finish the two large maps

André authorised booking the available 12:00–16:00 Lisbon window on 19 September.
Shared booking `p9n8htm1u645ofoa6toloqj2no` on proteina06 is confirmed. The target is
the remaining 1,223 + 1,812 = 3,035 cases, retaining the frozen runtime and map resume
contracts. A 12:00 thread wakeup is scheduled to install a fresh 15:59 halt guard,
start the VM only within the booking, check/extend available guest LVM capacity if
needed, and resume with at most eight application workers. New work must stop by
15:40 for draining and archival. New attempts require a distinct supplementary archive;
prior archives remain immutable. The Mac currently has limited space, so full local
retrieval must be conditional on capacity, with host evidence retained regardless.
The continuation was launched at 12:03:19 Lisbon after confirming the reservation and
installing the independent 15:59 host halt guard. The same thread wakeup is scheduled
for 16:02 to verify results. At launch both maps entered receipt/runtime validation
(`RESUMING`); new execution counts must be taken from the subsequent map statuses.


### Midday continuation launch

The starting measured counts were verified as 2,137/3,360 and 2,106/3,918, with
zero in-flight attempts and no map errors. No application containers were running.
The VM started only after the active booking and guard were confirmed. Its root
filesystem had 26 GiB free and the existing volume group had 31 GiB unused; extending
the existing root LV by 24 GiB increased free space to 48 GiB. Collection retains
the 8 GiB disk safeguard and stops launching at 15:40 Lisbon, with four workers per
map and at most eight total. Completed unavailable assessments remain measured;
resume does not retry them to obtain a score.

Retained orchestration scripts in `verifiers/target/cluster-proteina06-2026-09-18/`:
`midday.py`, `midday-launch.py`, `midday-backup.py`, `midday-guard.py`, and
`midday-retrieve.py`. Application source, scoring, catalogue and runtime were not
changed. The ordinary resume contract checks those frozen identities and all prior
attempt/report hashes before collecting missing candidates.

- Guest coordinator evidence: `/home/vagrant/thesis/verifiers/target/cluster-midday-2026-09-19/`.
  `baseline.json` freezes starting statuses and attempt directory identities;
  `archive-paths.txt` selects only new attempt directories plus updated map metadata.
- Host base: `/home/andremmsilva/thesis-campaign-2026-09-18/`.
  `midday-status.json` mirrors the coordinator; `midday-backup.log` records backup progress.
  Expected completed backup: `midday-results.tar.gz` with SHA-256/size receipt
  `midday-complete.json`; failures are recorded in `midday-failed.json`.
- The new archive is a delta over **both** `overnight-results.tar.gz` and
  `overnight-supplement.tar.gz`. Neither earlier archive is overwritten. Map counts
  must subtract the midday baseline to identify newly measured scenarios.
- `midday-guard.pid` identifies the independent host guard. Its completion evidence
  is `midday-halt-complete.json`; the cutoff is 15:59 Lisbon regardless of the guest.
- The local retrieval helper polls every five minutes until 16:20, keeps compact
  status/receipts, and downloads only if archive size plus 2 GiB fits on the Mac.
  Otherwise evidence remains on the host for verified streaming analysis/retrieval.

These are launch facts, not completed collection or GA comparison results. The 16:02
follow-up must inspect final statuses, validate receipts and report hashes, and run
recorded-feedback comparisons only on maps whose catalogues are complete.

### Hourly check at 13:01 Lisbon

Both maps are progressing: 009 has 2,529/3,360 measured (392 new this session),
and 012 has 2,506/3,918 (400 new), for 792 new cases and 2,243 remaining.
Each map has four in-flight attempts, current status timestamps and no coordinator
errors. Eight Docker workers are active, 42 GiB remain free, and the independent
halt guard and backup monitor are alive. Individual PARTIAL/PROCESS_FAILURE
assessments remain retained outcomes, not a collection stall. No intervention was
needed. The next check is scheduled for 14:00, followed by 15:00 and final analysis
at 16:02. Raw inspection: `midday-check-1300.json` in the local cluster artifact directory.

### Hourly check at 14:00 Lisbon

Map 009 has 3,073/3,360 measured and map 012 has 3,043/3,918: 1,873 new
cases since the midday baseline, 1,081 since the previous check, and 1,162 still
unmeasured. Both maps have four in-flight attempts, fresh status timestamps and
no coordinator errors. Eight Docker workers are active; 33 GiB remain free.
The host halt guard and backup monitor are alive, with no backup error logged.
Individual unavailable assessments remain retained, without retries or scoring
changes. No intervention was needed. The next check is scheduled for 15:00;
that check will restore the 16:02 analysis wakeup. Raw inspection is retained as
`midday-check-1400.json` in the local cluster artifact directory.

### Local storage relocation authorised on 19 September

The Mac had only 1.6 GiB free. Fresh SHA-256 checks on the host confirmed both
`overnight-results.tar.gz` and `overnight-supplement.tar.gz` still match their
recorded receipts. Their local duplicate archives were removed with user approval,
freeing 8,589,824,039 bytes; the immutable host copies remain at the campaign base.
Local summaries, comparison data and scripts remain. Subsequent analysis must stream
these archives from the host or deliberately retrieve them with a capacity check,
rather than assuming the two local archive files still exist. Removal receipt:
`verifiers/target/cluster-proteina06-2026-09-18/local-archives-removed-2026-09-19.json`.

The completed older `ga-500x3-2026-09-15` and `full-map-5184-2026-09-17` experiments
are also being archived directly to `/home/andremmsilva/thesis-evidence-archive-2026-09-19/`
on proteina06. This is storage/backup work only; it does not alter the active VM
collection. Local pruning is conditional on verified transfer SHA-256, a complete
archive read, exact file inventory and unchanged source metadata. Status and receipts
are under `verifiers/target/evidence-storage-2026-09-19/`; do not assume completion
until its status is COMPLETE and the per-experiment receipts exist.

### Host quota interruption during storage relocation

The host filesystem has ample global free space but the account has a **100 GiB
hard quota** (80 GiB soft quota). This was missed by the initial `df` check.
Uploading the older full-map archive reached that quota at about 14:29 Lisbon.
The upload failed with `Disk quota exceeded`; VirtualBox paused the active VM
with `VERR_DISK_FULL`. The incomplete 3.2 GiB upload was removed (the original
full-map evidence was untouched), and the VM was resumed at 14:31:25.
The collection continued without changing or retrying already recorded outcomes.

The verified old GA archive is being retrieved to the Mac so its host duplicate
can be removed after SHA-256 verification; the full-map backup switched to a local
compressed archive. No old full-map reports were deleted on the failed upload.
This storage limit must be checked with **quota**, not only host/guest `df`, during
the next checks and before backup. The halt deadline remains 15:59.

For evaluation integrity, attempts overlapping the VM pause must be identified in
the final audit: elapsed time and timeout/process-failure outcomes may be affected
by this known infrastructure interruption. Do not present such failures as
application defects, silently score them zero, or silently rerun them. Retain the
original attempt evidence and report the limitation explicitly. The raw transfer
failure and verified-first-archive receipt are under
`verifiers/target/evidence-storage-2026-09-19/`.

### Storage cleanup completion and quota protection

Both older experiments are now backed up as **local compressed archives** under
`verifiers/target/evidence-storage-2026-09-19/`, with SHA-256 and full member/inventory
verification before raw per-attempt data was removed. The old GA archive was
retrieved from the host, verified locally, then its host duplicate was removed.
The old full-map archive was generated locally after the quota-limited upload failed.
Compact reference maps, summaries, candidate summaries, replay descriptors and
analysis remain in the original directories; each has `ARCHIVED-EVIDENCE.json`.
Full report audits require restoring the archived reports first.

The GA archive is 3,383,391,706 bytes, SHA-256
`8e8d0e4e0cb2e3476b9d616a147bf8ac9fd7139a35d71d60791ece3e8ce2b553`.
The full-map archive is 6,881,385,631 bytes, SHA-256
`eb0b09e94ea4df3fff565742516fbdee7f100040d684bcc85df3356fb7c755ac`.
The Mac has about 7.8 GiB free after cleanup, versus 1.6 GiB initially. Directory
totals exaggerated reclaimable physical space because immutable input snapshots
use APFS clones; the archived bytes and physical free-space change differ.

To restore host quota headroom, four further **verified duplicate** deployment/older
result archives were removed from the host campaign base: `extended-results.tar.gz`,
`runtime.tar.gz`, `results.tar.gz`, and `parallelism-results.tar.gz`. Their unchanged
local copies remain (`results.tar.gz` is local `results-initial.tar.gz`). The frozen
guest runtime and application evidence are untouched. The two overnight archives
remain on the host, as previously documented.

The pending midday backup now uses existing `pigz -p 4 -6` instead of `gzip -1` to
reduce host storage needs; this changes compression only. Its monitor was restarted
while still waiting for completion, before any backup file existed. A separate
`midday-quota-guard.py` now checks the host account quota every 30 seconds and requests
a drain via existing map PAUSE files if fewer than 4 GiB remain before the hard limit.
This reserves space for backup; it must not be overridden blindly by a resume.
Evidence: host `midday-host-quota.json`, optional `midday-host-quota-stop.json`, and
`midday-quota-guard.log`. The 15:40 stop and 15:59 hard halt remain unchanged.

At 14:40 Lisbon, map 009 was complete (3,360/3,360); map 012 had 3,367/3,918 with
four active attempts. Host quota headroom was about 6.6 GiB and guest free space
about 28 GiB. Retain the documented VM-pause window in the final evaluation audit.

### Hourly check at 15:02 Lisbon

Map 009 remains complete (3,360/3,360); map 012 has 3,509/3,918 with four
active attempts. This is 2,626 newly measured cases since the midday baseline,
with 409 still unmeasured. Both map statuses have no coordinator errors.
Guest free space is 27 GiB; host hard-quota headroom is about 5.5 GiB. The quota
guard has not requested a stop. The backup monitor, quota guard and independent
15:59 halt guard are alive; backup has not begun. Raw inspection is retained in
`verifiers/target/cluster-proteina06-2026-09-18/midday-check-1500.json`.

The guest clock lags the host by about 102 seconds after the earlier VM pause.
To enforce the agreed launch deadline using the host clock, a small independent
host guard, `midday-launch-cutoff.py`, requests the existing map PAUSE mechanism
at 15:40 Lisbon. It does not stop active attempts or change scoring, sources,
worker counts or the 15:59 hard halt. Its receipt is
`midday-launch-cutoff-complete.json` in the host campaign base. The same heartbeat
has been rescheduled to 16:02 for final verification; no routine notification
was sent. Completed-map scoring and the pause-window audit remain for that check.

### Midday collection: verified final results

The VM is powered off; the independent guard halted it at **15:59 Lisbon**.
No application computation was restarted after the booking. The host quota guard
requested a drain at **15:31:37**, preserving space for the backup. This left the
second map partial. The coordinator's outer `COMPLETE` means that its orchestration
finished: the individual map statuses below are authoritative for catalogue coverage.

| WorkloadPlan | Measured / catalogue | Positive | Zero | Unavailable | New this session |
| --- | ---: | ---: | ---: | ---: | ---: |
| 009, `23f9768282…` | **3,360 / 3,360** | 2,622 | 708 | 30 | 1,223 |
| 012, `242d3ef997…` | **3,704 / 3,918** | 2,833 | 452 | 419 | 1,598 |

Both combine AddParticipant, LeaveTournament, RemoveTournament and UpdateTournament,
with different step orders. In 009, LeaveTournament captures the old Tournament
before UpdateTournament reads and changes it. In 012, that read occurs after the
update steps; RemoveTournament also deletes the Quiz before LeaveTournament's
write, whereas 009 places the leave write before that deletion. These are two
WorkloadPlans, each with many fault/recovery variants, not thousands of distinct
application stories or defects.

The continuation collected **2,821 new distinct scenarios**: **2,180 positive,
437 zero and 204 unavailable**. Earlier measurements were not counted again.
There are **214 unmeasured scenarios** left in 012. Unavailable measured outcomes
are separate from those unmeasured cases and were not retried or assigned zero.
Across the extended and overnight breadth collection, the accumulated total is now
8,301 measured scenarios (5,989 positive, 1,863 zero, 449 unavailable). The separate
original 156-case collection remains distinct; including it gives 8,457 measured
scenarios. Capacity pilots and search replay selections are not additional cases.

Map 009 has persistent-state findings in 2,622 scenarios, including 150 with an
active object depending on a deleted object; its anomaly components have no positive
findings. For example, attempt 009 records an active Tournament referring to a Quiz
deleted during the attempt, together with that Quiz's persistent difference after
failed-operation recovery (combined score 2). This is a concrete condition repeated
under multiple schedules, not two independent bugs. In 012, 1,747 measured attempts
have a complete positive compensated-read component; some still lack a complete
combined score, so this component count must not be added to the positive total.

#### Evidence and integrity

The midday delta archive is downloaded locally as
`verifiers/target/cluster-proteina06-2026-09-18/midday-results.tar.gz`:
2,557,307,697 bytes, SHA-256
`84e8109494e3c3e8237cd9fc18efcd2485b53e0e54cdcd43b195ccd8017f9e2f`.
Host backup verification completed at 15:37 Lisbon. Local size and SHA-256 match;
all 74,846 archive file entries were read. The two prior host archive hashes were
freshly rechecked and still match their earlier verified receipts.

`midday-analysis/summary.json` reports **zero integrity issues**. Each new attempt's
receipt and report hashes were checked, all 4,243 earlier attempt JSONs were read
from the unchanged prior archives and checked against current receipts, and the
complete reference map was compared with those observations. Scores were recomputed
using the frozen configuration, including its canonical criterion order. Baseline
attempt identities distinguish new work from earlier measurements.

The pause audit conservatively identifies ten attempts whose estimated execution
intervals overlap the VM pause, allowing five seconds around the pre-pause guest
clock instant. All ten have complete positive assessments, not process failures or
unavailable scores. Their report hashes validate. This does not prove that pausing
had no effect on behaviour; preserve these identifiers for sensitivity checks and
do not use their elapsed time as uninterrupted execution timing. Raw guest times
after the pause lag host times by about 102 seconds.

Local evidence under `verifiers/target/cluster-proteina06-2026-09-18/`:
`midday-download-verification.json`, `midday-complete.json`,
`midday-halt-complete.json`, `midday-host-quota-stop.json`,
`midday-analysis/summary.json`, `midday-analysis/pause-audit.json`, and
`analyse_midday.py`. The original and supplementary overnight archives remain on
the host; the midday archive overlays both. Only the complete 009 map is eligible
for the new exhaustive recorded-feedback comparison; 012 remains incomplete.

### Recorded-feedback comparison on complete map 009

The local comparison completed **30 paired seeds × two methods** on all 3,360
candidates. It uses the unchanged search implementation, uniform-unseen exploration,
population 8, mutation probability 0.3, stall limit 1,000 and five unit weights.
Each selection reveals only that candidate's recorded outcome. Both methods visit
the identical catalogue once per seed and finish with 2,622 confirmed positives,
708 zeros, 30 unavailable scores and assessed-score sum 2,822.

Unavailable fitness remains `None`, consumes budget and cannot become a parent.
The cumulative curves count confirmed positives and sum available scores; they do
not establish the unknown candidates' true impact. No application execution was
repeated, and these curves do not measure live search wall-clock time.

| Distinct selections | GA positives, mean | Uniform random positives, mean | GA assessed-score sum, mean | Random assessed-score sum, mean |
| ---: | ---: | ---: | ---: | ---: |
| 500 | 404.93 | 389.13 | 462.60 | 416.70 |
| 1,000 | 824.77 | 780.17 | 916.37 | 839.63 |
| 1,680 | 1,375.90 | 1,309.23 | 1,505.10 | 1,409.53 |
| 3,360 | 2,622 | 2,622 | 2,822 | 2,822 |

At 1,000 selections, GA finds about **45 additional confirmed positive scenarios
(5.7%)**, with **9.1% more assessed score**. It reaches 80% of the known positives
after 2,621 selections on average, versus 2,689 for uniform random (about 68 fewer).
The area under both discovery curves favours GA in all 30 paired seeds. This is
consistent evidence of a modest prioritisation benefit on this workload, not a
large acceleration or evidence across 30 independent application workloads.
Approximately 78% of this catalogue is positive, so it still does not provide the
desired evaluation of a workload with rare positives.

Artifacts under `verifiers/target/cluster-proteina06-2026-09-18/replay-midday/`:
`protocol.json`, `comparison.json`, `validation.json`, per-seed chosen candidate
keys, and `discovery.png` / `.pdf` / `.svg`. Validation checks complete uncapped
enumeration, all recomputed scores, identical final candidate sets, feedback only
after selection, preservation of unavailable scores and unchanged source hashes.
The plotted bands are the interquartile range across seeds, not confidence intervals.
The pause-window caveat above applies to the recorded outcomes; no attempt was
silently replaced or excluded from this comparison.

### Local search-cost diagnostic

A short diagnostic on the Mac reused the complete 009 map and timed the unchanged
search loop with the catalogue and observations already in memory. Three seeds were
run per budget and method, alternating method order. This includes proposal selection,
duplicate handling, fitness assessment and in-memory bookkeeping; it excludes application
execution, catalogue enumeration, package preparation and disk reporting.

| Selections | GA mean wall seconds | Uniform random mean wall seconds |
| ---: | ---: | ---: |
| 1,000 | 1.384 | 0.970 |
| 3,360 | 5.429 | 3.070 |

The additional selection cost is about 0.41 seconds per 1,000 choices in this diagnostic.
That makes selection overhead alone an unlikely explanation for losing the observed
execution-budget advantage in this setup; it does not establish a live end-to-end speedup.
The methods select different cases, which may have different application durations.
Shared enumeration/loading cost and live wall time still need a matched measurement.
The ordinary runner already records `wallSeconds`, `applicationSeconds`,
`commandWallSeconds`, and separate catalogue enumeration time; inspect their boundaries
before interpreting differences as algorithm overhead, especially with concurrent workers.

Reproduction and raw measurements:
`verifiers/target/cluster-proteina06-2026-09-18/measure_search_overhead.py` and
`search-overhead-diagnostic.json`. These are local diagnostic timings, not an isolated
paper performance benchmark or measurements of cluster application throughput.


## Preparation after the pruning and event-read qualification

The [next-campaign preparation](cluster-next-campaign-2026-09-19.md) records complete
16/26/16-scenario enumerations for the three previously deferred CreateTournament
workloads, a fresh complete-zero Topic/event/Tournament control, and the account quota
blocker. No new cluster reservation or application execution was started. Existing
campaign outcomes remain frozen; the newly qualified topic measurement uses an explicit
corrected-assessor overlay and will be reported separately.


### Current archive locations after 20 September cleanup

The verified duplicate HOST `midday-results.tar.gz` was removed. Its verified LOCAL copy
remains under `verifiers/target/cluster-proteina06-2026-09-18/`. The two overnight archives
remain HOST-only. See [cleanup and access status](cluster-next-campaign-2026-09-19.md#cleanup-on-20-september)
for the receipt, quota reduction and the exclusive booking blocking VM maintenance.
Earlier sections describe locations at the time of their measurements.
