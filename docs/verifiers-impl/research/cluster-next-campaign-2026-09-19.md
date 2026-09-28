# Next cluster campaign: preparation

No new cluster execution or reservation was started during this preparation.

## Ready for measurement

- Resume the original large map with 214 unmeasured cases and the five breadth maps
  with 60 unmeasured cases in total. Keep their frozen runtime and completed attempts.
- UpdateTopic + selected Tournament event + UpdateTournament now passes its fresh
  no-fault control with SUCCESS / EXACT and complete zero counts in all five criteria.
  The event-produced version read by UpdateTournament is attributed to its completed
  delivery. The retained catalogue has 44 scenarios; a fresh measurement uses the
  corrected assessor and remains separate from the old map's 17 positive, 22 zero and
  five unavailable results. No faulted cases were rerun during this preparation.

The topic control uses the native Java 21 qualification path, frozen dependencies and
an explicit overlay of the corrected read assessor/report. It is functional qualification,
not Docker performance evidence. Report/package hashes, report identities and the final
build hashes were checked. Evidence:
`verifiers/target/event-read-attribution-2026-09-19/topic-control-final/verified-summary.json`.

## Previously deferred CreateTournament workloads

The three exact workloads enumerate completely under the campaign's canonical domain
(no fault or one fault per Saga), with every recovery schedule written below the 10,000
per-vector cap:

| Workload | Saga combination | Vectors | Distinct scenarios |
| --- | --- | ---: | ---: |
| `a00c956cdae0…` | GetCourseExecutionById + CreateTournament | 16 | 16 |
| `3d8d79c5d058…` | UpdateStudentName + CreateTournament, two selected event actions | 16 | 26 |
| `e61e7630681d…` | UpdateStudentName + CreateTournament, no selected event actions | 16 | 16 |

This is 58 scenarios across three workloads, not 58 workloads. Each includes its
previously measured no-fault control; the 55 faulted scenarios have not been measured.
They add creation-related application stories, but are small individual GA benchmarks.
No positive density or discovery advantage is known from this enumeration.

The overnight coordinator deferred these because `2**domain.width > 128`: eight
fault slots implied 256 unrestricted binary vectors. The canonical domain actually
uses (1 + 1) × (7 + 1) = 16 vectors. That conservative preliminary guard did not
measure the canonical enumeration size. The next campaign should use the canonical
vector count, then the exact generated scenario count, when budgeting these cases.

The local packages were reproduced from the frozen 556-file breadth source snapshot
and frozen generator bytecode. Exact selected workload IDs and canonical record hashes
match the earlier retained records; the original host-only package archive was not
retrieved. These are not claimed to be byte-identical whole-package copies.

Independent checking reproduced each vector set and confirmed the uncapped totals,
written counts and unique candidate counts agree.

Per-vector enumeration receipts and copied packages:
`verifiers/target/cluster-next-preparation-2026-09-19/`.

## Storage decision

The read-only host inventory reports 100,772 MiB charged against a 102,400 MiB hard
account quota: about 1.59 GiB remains. The VM directory occupies about 86.2 GiB, campaign
archives about 10.4 GiB, and the Vagrant base box about 1.87 GiB. The VM is off. Global
filesystem capacity does not increase this account quota.

The midday archive also has a retained local copy (about 2.38 GiB); deleting its host
copy would provide temporary headroom only. Both copies were freshly SHA256-verified
against the original receipt during this preparation. The two
overnight archives are host-only and must not be removed. The base box is small relative
to the VM and is useful if rebuilding becomes necessary. No files were deleted.

First audit the VM contents during an authorised booking, separating archived completed
attempts, reports needed to resume incomplete maps, runtime dependencies and disposable
working files. Preserve verified compressed evidence, then reclaim redundant working
copies and caches. Check host quota after cleanup; if the virtual disk does not release
space, assess compaction or a lean rebuild after verifying full recovery of evidence and
the frozen runtime. More quota or external storage is a fallback, not a prerequisite
that has already been established.
Deleting files inside the guest alone does not establish a reduction in host quota usage.
Do not zero-fill the nearly quota-full virtual disk.

Inventory: `verifiers/target/cluster-next-preparation-2026-09-19/storage-inventory.json`.
The Mac had about 5 GiB free, so it is not currently a suitable destination for all the
host-only archives plus safe working space. Host quota headroom must be restored and checked before another large collection;
the inventory does not establish that all current VM contents need to be retained.


## Cleanup on 20 September

The 11:42 Lisbon booking check found another user's exclusive reservation on proteina06
from 20 September 10:00 until 23 September 10:00 (booking
`fiug6rc1g922bp8anes03q9j34`). Our VM remains off. Guest inventory, deletion of archived
raw attempts and any virtual-disk compaction were not performed outside our booking.

The redundant host `midday-results.tar.gz` was removed after fresh full SHA256 and size
checks of both copies against the original receipt. The retained copy is LOCAL at
`verifiers/target/cluster-proteina06-2026-09-18/midday-results.tar.gz` (2,557,307,697 bytes,
SHA256 `84e8109494e3c3e8237cd9fc18efcd2485b53e0e54cdcd43b195ccd8017f9e2f`).
The two prerequisite overnight archives remain on the HOST. Restoration therefore
combines those two host archives with this local midday delta. No evidence was discarded.

Host account usage fell from 100,772 MiB to 98,332 MiB, leaving about 3.97 GiB before
the 100 GiB hard quota. This is temporary headroom, not completion of VM cleanup.
The exact deletion receipt is `midday-host-duplicate-removal-2026-09-20.json` under the
preparation artifacts, with a copy on the host. Its initial quota query returned status 1
for the soft-quota condition after successful deletion; receipt finalisation was recovered
without repeating the deletion.

Next guest inventory should distinguish the complete 009 map's archived raw attempts,
other fully archived completed maps, build/container caches, and reports/runtime files
needed to resume the incomplete maps. Establish archive coverage before selecting deletion
paths, then measure actual host quota recovery. No recoverable VM-byte estimate is yet
established. Other machines had free slots at the check, but moving or recreating this
runtime elsewhere is a separate option, not an already completed migration.


## Alternative host and next deliverable, 20 September

Read-only SSH probes confirm separate account usage on proteina01 (10.15.0.11) and
proteina07 (10.15.0.17): each reports only 20 KiB used against a 100 GiB hard quota.
Their home filesystems are local ext4. The proteina06 usage is not shared with these
accounts. At 12:10 Lisbon proteina01 had a free four-hour window, 188 GiB physical
memory and the required VM tooling. SSH authentication works. It is the preferred
candidate; no booking or application computation was started by this check.

The immediate deliverable should be the completed 3,918-scenario map and its frozen
GA/uniform-random comparison, by measuring only its 214 missing scenarios. Then finish
the 60 missing scenarios across the five small maps. These complete existing evidence;
they do not promise a larger GA advantage. The 58 creation cases and refreshed 44-case
topic measurement are subsequent diversity work, not a new large sparse benchmark.

Bring the frozen runtime and the packages/state needed for unfinished work, rather than
copying the 86 GiB VM. Already archived completed raw reports stay archived. The existing
map runner accepts an explicit candidate-key subset, which may avoid restoring all old
attempt directories; verify the immutable original candidate IDs and result merge before
using this route. A new-host smoke check validates deployment, not another open-ended
workload triage. Storage/account quota must be monitored and completed report batches
archived/verified before pruning their redundant working copies.

Evidence: `verifiers/target/cluster-next-preparation-2026-09-19/new-machine-check-2026-09-20.json`.


## Campaign launched on proteina01, 20 September

Shared booking `8be3qshjedcajon2f0gri5j7jk` runs 12:12–17:00 Lisbon. VM
`andre-thesis-2026-09-20` has 32 vCPUs and 64 GiB RAM. Its host guard was installed before
boot and powers it off at 16:59; collection stops launching by 16:40. Another shared
booking is active, so observed throughput is operational evidence, not isolated timing.

The transfer is a 144 MiB frozen runtime bundle plus compact immutable metadata, rather
than the old VM. All 3,436 bundle file hashes match, and the Docker image ID matches
proteina06 exactly (`sha256:3b5f7c15b1a16d3fdf09e6883cde602e4a5406cf5bdf6b251b8ac5c510219311`).
The old archives were checked in full against their recorded SHA256 while selecting
metadata. Each of the 3,704 existing large-map and 60 breadth-pilot attempt summaries
matches its receipt and exact catalogue candidate; no previous candidate is queued again.

The large-map fresh control passed SUCCESS / EXACT with all five criteria complete and
zero. Its 214 pending keys are sorted deterministically: the first 32 run with eight
workers, the next 64 with sixteen, and the remaining 118 use the higher observed
throughput unless the sixteen-worker group has infrastructure-like failures. The groups
contain different cases: this is a capacity choice using useful new executions, not a
controlled speedup result. The five small maps then run their 26, 3, 2, 24 and 5 pending
cases, each after its fresh zero control. Unknown outcomes are preserved and not retried
until positive. Application code, scoring and frozen runtime are unchanged.

New attempts are collected in separate subsets and merged by exact candidate key with
the immutable prior observations. No old result is overwritten. A full merged reference
requires every original catalogue key exactly once. New-host controls are repetitions,
not additional distinct scenarios. The 58 creation and 44 topic cases remain a later
batch; they are not part of this 274-case continuation.

Host base: `/home/andremmsilva/thesis-campaign-2026-09-20` on `10.15.0.11`.
Guest output: `/home/vagrant/thesis/verifiers/target/cluster-finish-2026-09-20`.
Local scripts, selected metadata, checks and booking:
`verifiers/target/cluster-proteina01-2026-09-20/`.
The host monitor checks quota and status every 30 seconds, requests PAUSE below 10 GiB
quota headroom, backs up results on completion/failure, verifies the archive and shuts
the VM down. The guest also pauses below 10 GiB filesystem headroom. At deployment,
the guest used 6.6 GiB with 52 GiB free, and the host account had over 90 GiB headroom.
A Codex follow-up is scheduled for 13:00 to inspect progress/completion and, when ready,
verify the archive and perform the local 30-seed GA/uniform-random comparison.


Initial live check: eight attempts were in flight and the first eight finished with
seven assessed outcomes and one `PROCESS_FAILURE`. That process failure produced full
execution evidence: `COMPENSATION_FAILED / INCOMPLETE`, with recovery referring to
missing aggregate 12. It is not evidence of host memory exhaustion or a container timeout.
The frozen protocol preserves its unavailable score. The capacity selector conservatively
counts process-failure statuses when deciding whether to keep sixteen workers; this
heuristic must not be reported as proof that concurrency caused the application failure.

### 13:00 monitoring: sixteen-worker timeouts

The eight-worker phase finished with 29 COMPLETE, one PARTIAL and two PROCESS_FAILURE
outcomes. All 64 attempts in the sixteen-worker phase timed out at the unchanged
approximately 180-second limit, without execution reports. The coordinator consequently
selected eight workers for the remaining cases. At this check, 56 of those 118 attempts
had finished: 51 COMPLETE, four PARTIAL and one PROCESS_FAILURE; eight were active.
Thus 152 of the 214 pending large-map candidates had been attempted, but 64 of these
did not obtain application observations. They must not be described as fully assessed.

The sixteen-worker phase's apparent 4.86 attempts/minute counts timeout completions;
it is not useful-result throughput or evidence of a speedup over eight workers.
The inspected timeout directories contain replay/package/attempt metadata, but no
retained application logs or execution reports. No kernel OOM entries were found;
the available evidence does not establish the precise cause of the timeouts. The
contrast with successful eight-worker batches suggests a capacity/startup issue,
but the batches contain different scenarios, so it does not prove that diagnosis.
No timeout was retried or replaced, and scoring/runtime remained unchanged.

Guest free disk was about 50.6 GiB, host quota headroom about 90.4 GiB, and both
backup monitor and independent 16:59 halt guard were alive. The next check is 13:30.
Snapshot: `verifiers/target/cluster-proteina01-2026-09-20/check-1300.json`.

### Explicitly authorised timeout repeat, 20 September

The 274-case continuation completed and its original archive was retained on proteina01:
`results.tar.gz`, 445,995,204 bytes, SHA256
`bab1e9221c02e02a0a9684cb6d98c54d7a3b4908cbd8065726e8e23b34c49cd4`
(host backup receipt; independent retrieval verification remains pending).
Its combined large-map counts are 2,951 positive, 473 zero and 494 unavailable;
the five small maps contain 120 zero-score cases in total.

At 13:49 Lisbon André authorised retrying the 64 timeouts. The VM was restarted within
the existing booking, with its independent 16:59 guard still active. `retry64.py` performs
exactly one repeat of every case from the sixteen-worker batch, now with eight workers,
a fresh healthy control, and the same frozen runtime, scoring and 180-second timeout.
This is 64 repeated attempts and zero new distinct scenarios. No original outcome is
replaced. Any revised reference must be separately labelled and retain the paired results;
selection is all 64 keys, regardless of retry outcome.

Guest output: `verifiers/target/cluster-retry64-2026-09-20`.
Host monitoring/backup: `retry64-status.json`, `retry64-quota.json`,
`retry64-results.tar.gz`, `retry64-backup-complete.json`.
The separate monitor preserves the original archive, applies the same quota/cutoff rules
and shuts down the guest after backup. Local scripts are in the proteina01 artifact folder.
The 58 creation and 44 topic cases remain proposed subsequent work, not silently launched.

### Authorised diversity queue

André subsequently approved the three creation workloads followed by the topic workload.
`diversity-queue.py` is running on proteina01, waiting for the retry's verified backup
and guest shutdown. It then boots the same VM within the current booking, deploys the
SHA256-verified input bundle and launches `diversity.py` with eight workers. It refuses
late startup after 16:25; the existing 16:40 drain and 16:59 halt remain in force.
Each workload is admitted only after a fresh SUCCESS / EXACT complete-zero control and
complete canonical enumeration matching 16, 26, 16 and 44 respectively. A rejected control
is recorded without patching inputs or treating rejection as impact.

Creation uses the frozen runtime. Topic alone prepends the previously qualified, hashed
`runtime-overlay-final` assessor classes; its 44-case measurement is a new labelled
assessment, not an extension of the old scoring reference. The controls are included once
in each complete reference and excluded from the faulted-case queue (55 creation fault
cases and 43 topic fault cases). They must not all be counted as new distinct discoveries.

Guest output: `verifiers/target/cluster-diversity-2026-09-20`.
Host status: `diversity-queue-status.json`, `diversity-status.json`.
Separate backup: `diversity-results.tar.gz` / `diversity-backup-complete.json`.
Input bundle SHA256: `2504f57b99fac039eb8327711ad6ecefcb308024f4276b8cf1f00d27ee7d8ff1`.
No production source, weights or search algorithm changed. Recounting compression after
pruning and fixing the other Quizzes extraction shapes are separate local work.


### 14:30 check: retry verified and diversity running

The original and retry archives were downloaded with sufficient local space and their
sizes and SHA256 values independently matched the host receipts. Streaming inspection
verified 274 original attempt receipts and 64 retry receipts, plus report hashes for
280 original attempts (including six controls) and 65 retry attempts (including its
control). Each new reference observation matched its retained attempt. Verification:
`verifiers/target/cluster-proteina01-2026-09-20/verify_archives.py` and
`verified/verification-summary.json` in that folder.

All 64 repeat keys and candidates match the original sixteen-worker timeouts. At eight
workers, 60 have complete scores: 55 positive and five zero. Three remain PARTIAL and
one PROCESS_FAILURE. Their fitness was independently recomputed from retained attempt
observations. There are no additional distinct scenarios and no second retry is scheduled.
Replacing precisely the predeclared 64 outcomes in a separately labelled reference gives
3,006 positive, 478 zero and 434 unavailable among 3,918 candidates. The original reference
remains unchanged. This improves observed coverage but does not establish the exact cause
of the sixteen-worker timeouts. Verification of the historical reference provenance and
the paired 30-seed replay remain part of the final analysis.

Retry archive: `retry64-results.tar.gz`, 60,676,642 bytes, SHA256
`fa428cb9b207c2141618271e36e72cdd749f83233dbbb929851d75768db91781`.
Both new archives now also exist locally in the proteina01 artifact folder.

The diversity queue launched automatically after retry backup and VM shutdown. The first
creation workload completed its 15 faulted attempts after a healthy control; the second
passed its control and had 16/25 faulted attempts completed with eight in flight. Its map
reported no runner errors. The host halt guard remained alive, guest disk had 46.9 GiB
free, and account quota headroom was about 86.3 GiB. No additional workload was launched
outside the authorised queue. Next monitor: 15:00 Lisbon.


### Exploratory component census for the weighting discussion

Local `component_census.py` / `component-census.json` in the proteina01 artifact folder
reassess the retained 009 reference and separately labelled retry-adjusted 012 reference.
All original scores/components match recomputation (unavailability reason ordering is
ignored). This is a post-hoc diagnostic, not a new runtime campaign or a GA comparison.
It covers these two workloads, not the application as a whole.

Among cases assessed with all five unit weights, 009 has 2,622 positives: all contain
FAILED_OPERATION_RESIDUAL, 150 also DELETED_DEPENDENCY; 2,472 contain only the former.
There are no positive read-exposure, copied-update or unresolved-event components.
For 012, 3,006 are positive: all contain FAILED_OPERATION_RESIDUAL, 1,762 contain
DELETED_DEPENDENCY and 1,740 read exposure. Exactly 600 contain residual only; the other
combinations are 644 residual+read, 666 residual+dependency and 1,096 all three.
These are scenario counts, not distinct application bugs.

Setting only the residual weight to zero and recomputing coverage gives 009:
150 positive / 3,198 zero / 12 unavailable (3,360 total). For 012 the corresponding
counts are 2,524 / 1,144 / 250 (3,918 total). Disabling read exposure alone does not
make these maps sparse: 009 is unchanged; 012 becomes 3,036 / 478 / 404, because 30
previously incomplete cases become scoreable on the remaining criteria. Missing evidence
for a disabled criterion no longer prevents an otherwise valid assessment. Never present
these changes as identical-population positive subtraction.

The sparse 009 profile is an exploratory candidate for a declared user preference about
what outcomes matter. Retain unit-weight results and predeclare subsequent profiles;
do not select or report weights solely because they improve GA relative to random.
A changed GA fitness requires a new local recorded-feedback search, not merely rescoring
the old selected sequence. No application execution is needed where retained component
evidence is sufficient. A smaller positive fraction does not guarantee a genetic advantage.


### Authorised weight comparisons and Topic continuation

André approved tasks 1–3: finish Topic, verify the creation results, and compare user
weight preferences locally. The proposed residual-disabled profile was rejected because
it lacked a clear user intention. The declared profiles are now all five unit weights,
DELETED_DEPENDENCY only, and COMPENSATED_READ_EXPOSURE only. Other weights are zero.
They apply to both large maps, with thirty paired seeds, unchanged population 8,
mutation 0.3, uniform unseen exploration, and full catalogue budget. Null scores consume
budget and are not parents. Zero-only profiles are explicitly flat, not evidence of a
GA advantage. Each selected candidate reveals its retained observation only then.
`compare_weights.py` retains compact curves and candidate-index sequences under
`weight-comparison/`; it does not repeat application executions. Cross-workload learning
implementation remains deferred for discussion.

All three creation references independently verified as complete zero: 16, 26 and 16
cases, including one healthy control each. The new faulted executions total 55, not 58.
The diversity archive SHA256/size matched its receipt, all 55 faulted-attempt receipts
matched, and report hashes were verified for 59 attempts (including four controls).
Fitness was independently recomputed for every creation reference. Topic's fourth
control succeeded, but enumeration failed because the container wrote its updated
manifest with root ownership and mode 0600; the unprivileged coordinator could not read it.
This is a collection permission failure, not an application outcome.

A separately named `topic-continuation.py` was launched in the existing booking with a
root coordinator inside the guest, so it can read the container-created files. Runtime,
application, inputs, assessor overlay and scoring are unchanged. It repeats the healthy
control and performs fresh enumeration and the 43 faulted cases; no Topic faulted attempt
had completed in the previous batch. Original outputs and backup are unchanged.
Guest output `cluster-topic-continuation-2026-09-20`; host monitor and archive prefix
`topic-continuation-`, with the same 16:40 stop and independent 16:59 halt. Its monitor
backs up separately then powers down the VM. No later workloads were silently added.


Local storage: the three newly downloaded archives were removed locally only after fresh
host SHA256 checks matched their independently verified local copies. They remain on
proteina01 at the campaign base: results.tar.gz, retry64-results.tar.gz and
diversity-results.tar.gz. Compact references, receipt summaries and scripts remain local.
This recovered 715,251,594 bytes; local free space was about 5.6 GiB. See
`local-storage-receipt.json`. The earlier note saying both copies exist is superseded.
The 3,704 historical large-map attempt metadata files were also checked against their
receipts and joined exactly to the 214 new observations: all 3,918 unique candidate keys
match the final original reference. The separate retry reference substitutes only the
64 predeclared keys. See `verified/combined-reference-provenance.json`.


### Verified completion of the authorised three tasks

Topic's continuation completed all 43 faulted attempts plus the repeated healthy control:
17 positive, 25 zero and two unavailable. Both unavailable scores are
FAILED_OPERATION_RESIDUAL_INCOMPLETE / COMPETING_OR_UNKNOWN_WRITER; read attribution has
no gaps. All 17 scored positives concern failed-operation residuals. The exact 44-case
uncapped catalogue, report hashes and 43 receipts were independently verified.
Archive 39,016,140 bytes, SHA256
`fb8eef6a578afe3716ad98204465e51c0a59ccbc8be9b31a498b5cbc07959d58`,
retained as `topic-continuation-results.tar.gz` on host and locally. Host VM state was
verified `poweroff` after backup (14:09:46 UTC), with the independent guard still alive.

All five non-flat map/profile comparisons completed thirty seeds for each method (300
recorded-feedback searches); the 009 read-only profile has no assessed positives and is
reported as flat. Exact full-catalogue endpoints and unique coverage were checked. Random
orders match across profiles and the 009 all-criteria traces reproduce the older frozen
comparison exactly. Main result: at 1,000 selections, deleted-dependency GA/random means
are 90.07/44.47 for 009 and 628.10/454.70 for 012. Read-only 012 is 560.93/470.73.
No extra application executions were used for the weight comparisons.
Durable report and figures: [weight preferences](../evidence/weight-preferences-2026-09-20/REPORT.md).
Raw compact curves: `weight-comparison/` under the proteina01 artifact folder.


### Authorised alternative Topic orders, 20 September 15:22

André approved exploring additional Topic/Tournament orders in the remaining booking.
Three existing generated workloads were fixed before measuring their full maps:
`1d79a9…` (event late enough to be overwritten during recovery), `3daab108…` (event
between the earlier Topic read and Tournament write), and `4d36f10…` (no selected
Tournament event). These are known detector-qualification stories, not a random sample
selected without prior observations. Their exact IDs and orders are in `topic-orders.py`.
They use the already transferred topic source package and the same verified runtime and
read-assessor overlay. No source inputs, scoring or application code are modified.

Each control is repeated before fault collection. A SUCCESS/EXACT control with a positive
complete score is retained as POSITIVE_NO_FAULT_CONTROL and is not called a setup failure.
The existing catalogue-search admission still requires complete zero, so that workload's
fault map is not launched under this campaign. The other workloads enumerate the complete
canonical domain, with a predeclared operational maximum of 300 candidates each. Larger
maps are documented for later rather than truncated or partially called complete.
Eight workers maximum; launches stop 16:40 and the existing independent 16:59 halt remains.

Guest `topic-orders.py` / `verifiers/target/cluster-topic-orders-2026-09-20`.
Host `topic-orders-status.json`, `topic-orders-quota.json`, separate
`topic-orders-results.tar.gz` / `topic-orders-backup-complete.json`.
The monitor archives and shuts down after completion. The recovery-late-event control
passed SUCCESS/EXACT complete-zero and enumeration started. No prior archive is overwritten.

In parallel, `compare_weights_5184.py` compares the same declared single-criterion profiles
on the existing 5,184-candidate reference locally. Its all-criteria baseline reuses and
revalidates the original 60 search traces, not new application runs. See
`weight-comparison-5184/5184-all/reuse-proof.json`. Raw archived application reports are
not expanded. All criteria remain recorded; weights only change the search preference.

### Final diversity extension authorised at 15:38, 20 September

André authorised using the remaining booking for additional, structurally different
workloads while completing local GA comparisons. This authorisation extends the earlier
three Topic orders; it does not change runtime, detectors, search or admission rules.

The next queue contains eight exact plans from the existing frozen breadth package:

1. RemoveCourseExecution + CreateQuiz: deletion and creation across related data.
2. AnonymizeStudent + CreateTournament: privacy changes and creation.
3. AnonymizeStudent + GetCourseExecutionById + RemoveStudentFromCourseExecution:
   privacy, query and membership removal.
4. UpdateStudentName + AddParticipant + FindTournament: name propagation,
   participation and querying.
5. AnonymizeStudent + AddParticipant: privacy changes and participation.
6. RemoveStudentFromCourseExecution + UpdateStudentName: membership removal and name change.
7. AddStudent + UpdateStudentName: membership creation and name change.
8. GetCourseExecutionById + UpdateStudentName + AddParticipant: query, propagation
   and participation.

These families were selected for different application stories before measuring their
new outcomes. Within each family, selection excludes the 129 earlier breadth-plan IDs,
requires a static setup candidate and at most 64 canonical fault vectors, prefers more
selected event actions, then uses fixed SHA256 rank. This is purposive exploration,
not a random or exhaustive sample of Quizzes. `final-diversity-selection.json` records
all exact schedules and package hashes. A fresh normal control must succeed with the
requested order and complete zero score. Positive no-fault controls are retained as
findings but excluded from the unchanged fault-search admission rule. Other rejected
controls remain visible.

Four workload lanes use two workers each: at most eight concurrent application attempts.
Admitted catalogues are enumerated without truncation; a complete fault map is measured
only if it has at most 128 candidates. Larger maps are recorded for a later booking.
The cap limits this booking's work, not the scientific definition of a workload. Partial
maps at the deadline remain partial and cannot support exhaustive replay comparisons.
The already qualified event-origin assessor overlay is reused and hashed; no new source
or scoring changes were made.

Host `final-diversity-queue.py` (PID 969154) waits for the Topic-orders verified backup
and VM shutdown, then starts this batch before 16:20 Lisbon. It does not overlap the
previous coordinator. Inputs are a 1.9 MB package bundle. Guest output:
`verifiers/target/cluster-final-diversity-2026-09-20`. Host monitor, status, quota and
archive use prefix `final-diversity-`. Stop launching at 16:40 and the independent
16:59 halt remain unchanged. Backup precedes shutdown and preserves earlier archives.

Local `final-local-analysis.py` (PID 52193) waits for Topic-orders and final-diversity
archives. It checks available space, archive SHA256, attempt receipts, report hashes,
uncapped catalogue membership and recalculated fitness, retaining compact references.
It then compares the same frozen GA and uniform random policy on every completed map
under the three declared profiles (all criteria, deleted dependencies, compensated
reads), with 30 paired seeds. Flat profiles are retained without a discovery comparison.
No new application executions are needed for these local comparisons. Failures stop
analysis visibly; no missing score is replaced by zero. Heartbeat checks at 16:15 and
subsequently before/following the booking end supervise the queue and local analysis.

The 5,184-profile comparisons have now finished all 30 paired seeds. Final results and
seed-variation figures are in `../evidence/weight-preferences-2026-09-20/REPORT.md` and
`5184-final-summaries.json`. The three large maps use the same four Saga types; collecting
more schedules from that family alone would not establish application-story diversity.

### 16:15 supervision and conditional final top-up

The first extension is healthy: three complete maps, three rejected normal controls,
and two maps still collecting, four active workers. Guard is alive; guest free disk
43.8 GiB and host hard-quota headroom about 82.8 GiB. Topic-orders backup and local
comparisons finished and were verified (98 attempt reports, 95 receipts). The two
admitted maps contain 62 and 35 scenarios. Their global score counts are respectively
20 positive / 26 zero / 16 unavailable and 15 positive / 20 zero. The 62-case map
also has two positive LOST_COPIED_UPDATE observations whose overall score remains
unavailable because residual attribution is incomplete. The positive no-fault control
has one lost copied update. These component findings are retained separately from the
all-criteria search fitness; see the evidence report.

Within André's instruction to refill capacity with different untried plans, a final
conditional queue was prepared with four additional exact plans: AddParticipantAsync
+ FindTournament, FindTournament + RemoveTournament, FindTournament + UpdateTournament,
and RemoveCourseExecution with three selected event actions. Selection again excludes
the prior breadth-plan IDs, uses static setup availability and structural ranking,
and is frozen before measuring new outcomes. Canonical vector counts are 6, 8, 12,
and 4. These are candidates; controls may reject them and enumeration may defer them.

Host `late-diversity-queue.py` PID 971581 waits for the final-diversity verified backup
and complete VM shutdown. It starts only before 16:32; otherwise it records a deferred
launch and performs no compute. It never overlaps the earlier batch. Coordinator and
monitor use prefix `late-diversity`, four lanes/two workers, unchanged 16:40 launch
cutoff, 10 GiB quota safeguard and 16:59 host guard. Output is
`verifiers/target/cluster-late-diversity-2026-09-20`. Source package and qualified assessor
are unchanged; new archive is `late-diversity-results.tar.gz`.

Local `late-local-analysis.py` PID 53419 waits for this archive and uses the same hash,
receipt, catalogue and score checks and three-profile/30-seed comparisons. Its status
is separate from `final-local-analysis-status.json`. If the late queue misses its
launch window, record that status rather than interpreting a missing archive as lost
evidence. Next heartbeat is 16:45, then 17:02; never restart after the reservation.

### Verified completion before 17:00, 20 September

The final eight-plan batch completed five full maps (36, 12, 14, 6 and 12 candidates,
all 80 zero) and rejected three controls. Its archive is 300,712,077 bytes, SHA256
`ae9973becc6a1e7dcce475de65840e05fff2ba95f8df62a2dd536f2b225a808f`.
The conditional late batch launched after that backup and shutdown, completed maps of
eight candidates (two positive/six zero) and four (one positive/three zero), rejected
one control and retained one SUCCESS/EXACT control with unavailable copied-update
coverage. Its archive is 67,382,732 bytes, SHA256
`e5782037bff83a6a58e45d7cfb3480eb7c8c3b02c37e8fc00a8f41b4fc58cc8f`.
Both archives were downloaded within the space safeguard, hashes/receipts/reports and
full catalogue identities verified, and local comparisons completed automatically.

Across Topic-orders, final-diversity and late-diversity: 15 controls, nine complete maps,
189 distinct map scenarios (38 positive/135 zero/16 unavailable), 180 fault-attempt
receipts and 195 report sets checked. No partial maps. All 300 non-flat-profile replay
traces passed the independent final check. Control rejections, eligible/absent event
receivers and the two lost updates inside globally unavailable scores are described in
`../evidence/weight-preferences-2026-09-20/REPORT.md`; do not flatten these distinctions.

Direct VBoxManage inspection at the 16:45 follow-up confirms VMState=poweroff after
backup; the independent 16:59 guard remains intact. `extension-shutdown-verified.json`
records the check. All authorised collection and local comparison work is concluded.
No further cluster launches were made. The final report can be issued and heartbeat
paused without waiting to restart or use the remainder of the booking.
