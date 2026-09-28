# Search under declared user preference profiles

Two fixed workloads use the same four Sagas: AddParticipant, LeaveTournament, RemoveTournament and UpdateTournament, with different forward orders. Map 009 has 3,360 candidates; map 012 has 3,918.

Two complete candidate maps, 30 paired seeds per non-flat profile. The second reference incorporates the one predeclared repeat of all 64 timeouts; original outcomes remain retained. These are exploratory recorded-feedback comparisons, not new application executions or live wall-time measurements. Profiles were agreed before these comparisons; all results are reported, including flat or unfavourable ones.

Each method visits the same finite catalogue without repeating a candidate. GA population 8, mutation 0.3 and uniform unseen exploration are unchanged. Each observation is revealed only on selection; configured weights determine its fitness and subsequent GA choices. Unavailable scores consume budget and cannot become parents.

## Available outcomes

| Map | User preference | Candidates | Positive | Zero | Unavailable |
| --- | --- | ---: | ---: | ---: | ---: |
| 009 | All five criteria | 3360 | 2622 | 708 | 30 |
| 009 | Deleted dependencies only | 3360 | 150 | 3198 | 12 |
| 009 | Compensated reads only | 3360 | 0 | 3348 | 12 |
| 012 | All five criteria | 3918 | 3006 | 478 | 434 |
| 012 | Deleted dependencies only | 3918 | 1782 | 1918 | 218 |
| 012 | Compensated reads only | 3918 | 1858 | 1810 | 250 |

Disabling a criterion removes its completeness requirement; the assessed subset can change. Positive means positive under the chosen preference, not a different definition of the underlying observations. Counts are scenarios, not distinct bugs.

## Discovery at 1,000 evaluations

| Map | Preference | GA positives, mean | Uniform random, mean | Relative difference | GA evaluations to 80%, mean | Random evaluations to 80%, mean |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| 009 | All five criteria | 824.77 | 780.17 | +5.7% | 2621.17 | 2689.10 |
| 009 | Deleted dependencies only | 90.07 | 44.47 | +102.5% | 1822.10 | 2671.40 |
| 012 | All five criteria | 837.57 | 764.87 | +9.5% | 3058.07 | 3132.10 |
| 012 | Deleted dependencies only | 628.10 | 454.70 | +38.1% | 2735.57 | 3128.47 |
| 012 | Compensated reads only | 560.93 | 470.73 | +19.2% | 2940.20 | 3137.63 |

## Accumulated configured score at 1,000 evaluations

| Map | Preference | GA, mean | Uniform random, mean |
| --- | --- | ---: | ---: |
| 009 | All five criteria | 916.37 | 839.63 |
| 009 | Deleted dependencies only | 90.07 | 44.47 |
| 012 | All five criteria | 2878.30 | 2326.70 |
| 012 | Deleted dependencies only | 628.10 | 454.70 |
| 012 | Compensated reads only | 965.87 | 800.10 |

The 009 read-only profile has no assessed positive outcome, so no discovery comparison is run. Full-catalogue endpoints are identical for both methods by construction. Per-seed curves and compact candidate-index sequences remain in each profile directory. The known-positive curves omit unavailable outcomes without classifying them as zero.

Integrity checks: retained observations re-scored; full distinct coverage and endpoint checks for every trace; random candidate order equal across preference profiles; 009 all-criteria sequences exactly reproduce the earlier frozen 60 traces. Local processing ran concurrently, so its timing is operational only.

## Artifacts

Compact per-seed curves and candidate-index sequences, source references, scripts and archive verification receipts are retained under `verifiers/target/cluster-proteina01-2026-09-20/`. No application or production search source was changed. The five non-flat map/profile combinations comprise 300 recorded-feedback searches. The earlier discarded residual-disabled proposal is not part of this comparison.

## Parallel application collection

Three creation workloads completed with all five criteria available and zero: 16, 26
and 16 scenarios, each including a healthy no-fault control (55 new faulted attempts).
The separately labelled Topic/event/UpdateTournament measurement has 44 candidates:
17 positive, 25 zero and two unavailable. All scored positives are failed-operation
residuals. The two unavailable results have competing/unknown writer attribution for
that same criterion; compensated-read coverage has no gaps. No copied-update positives
were observed in this particular fixed order.

Its initial catalogue permission failure was corrected at the guest coordinator boundary;
application code, assessor overlay, inputs and scoring were unchanged. The continuation
repeated its healthy control and executed the 43 faulted candidates. All 44 report hash
sets, 43 attempt receipts, complete catalogue membership and recomputed fitness were
independently checked. Its new archive is separately retained on proteina01 and locally:
`topic-continuation-results.tar.gz`, SHA256
`fb8eef6a578afe3716ad98204465e51c0a59ccbc8be9b31a498b5cbc07959d58`.
The VM powered off after verified backup, within its reservation. No bandit implementation
or paper changes form part of this work.

## Completed 5,184-scenario comparison

The earlier 5,184 map uses the same four Saga types as 009 and 012, with a different
forward order. It is a third ordering comparison, not a third application or domain
family. The control and frozen original observations are reused; raw logs were not
restored and no application scenarios were re-executed for these profiles.

All 30 paired seeds are complete. The all-criteria traces were independently revalidated
and reused; the two new profiles were replayed against the same frozen search. Each
trace visits all 5,184 candidates exactly once, with identical final totals; random
candidate orders match across profiles. Unknown outcomes consume budget unchanged.

| Preference | Positive / zero / unavailable | GA positives at 1,000 | Random | GA evaluations to 80% | Random |
| --- | ---: | ---: | ---: | ---: | ---: |
| all | 3979 / 657 / 548 | 841.17 | 764.73 | 4051.07 | 4148.47 |
| deleted-dependency | 2090 / 2860 / 234 | 549.33 | 401.17 | 3695.33 | 4150.73 |
| compensated-read | 2581 / 2289 / 314 | 583.40 | 497.50 | 3930.37 | 4145.50 |

The dependency-only gain is 36.9% more positives at 1,000 evaluations, but only 11.0%
fewer evaluations to reach 80% of known positives. The read-only figures are 17.3%
and 5.2%, respectively. This distinction matters: an early discovery advantage does
not remain constant throughout catalogue exhaustion. The three-map figure includes
all declared profiles, including the flat read-only profile, and 10th–90th seed
percentiles. It is an evidence overview; the final paper should select a readable
figure and keep the full results in supporting material.

Evidence: `5184-final-summaries.json`, `preferences-three-maps.pdf` and the compact
per-seed curves under the ignored local artifact directory. Timing from concurrent
local replays is not an isolated GA overhead benchmark.

## How the cases support the search evaluation

The evidence has three complementary roles:

| Cases | What distinguishes them | What they test |
| --- | --- | --- |
| Three large Tournament workloads (3,360 / 3,918 / 5,184) | Same four Saga types, different forward orders; complete fault catalogues | Whether search gains persist across execution orders and configured objectives |
| Topic update and Tournament update, with different event positions | A propagated change can arrive before a read, between a read and write, or around recovery; one order has no selected event | Whether event timing changes the observed anomaly and whether it is already present without injection |
| Creation, membership, privacy and course-removal workloads | Different domain operations, Saga combinations and event routes; usually smaller catalogues | Whether the evaluation covers more than the large Tournament family, including controls that fail and maps with no positives |

The earlier breadth cohort selected 129 plans across 70 Saga-type combinations, with
67 successful exact complete-zero controls across 39 combinations. These are figures
for that selected cohort, not global Quizzes executability. The new eight-plan extension
selects additional exact plans by domain story and event structure before observing
their new scores. Its complete/partial status and exclusions will be reported separately.

For each fully measured map, compare GA and uniform random with the same catalogue,
criteria, seed set and number of evaluations. Present both confirmed positives and
accumulated configured score. Report the number of evaluations needed to reach 80%
of known positives when there are any, with variation across seeds. For unequal map
sizes, additionally compare at the same fraction of each catalogue; 1,000 evaluations
is useful only for the large maps. Complete enumeration supplies the denominator;
incomplete maps are collection evidence, not exhaustive discovery comparisons.

For the paper, a compact figure can contrast a frequent-positive objective with a
rare-positive objective. A companion table should retain results from the other
workloads, including weak gains, losses and zero-positive maps. Figure examples may
be chosen for explanatory value after analysis, but that is different from claiming
they were a representative sample chosen before measurement. Thirty seeds describe
search variability on each fixed map; they do not provide thirty independent domain
workloads. Selection rules and full results remain available in supporting material.

## Verified Topic-order comparison

The three additional orders completed with 98 retained attempts and 95 fault-attempt
receipts; archive SHA256 `4f14fd3f542df57b6703d3e9016735a22b895278e6091b3632250d58bbde96d0`.

| Order | Complete catalogue | Positive / zero / unavailable, all criteria | GA evaluations to 80%, mean | Uniform random |
| --- | ---: | ---: | ---: | ---: |
| Event delivered late, potentially overlapping recovery | 62 | 20 / 26 / 16 | 43.90 | 47.13 |
| No selected event | 35 | 15 / 20 / 0 | 24.03 | 26.73 |

Thirty paired seeds per non-flat profile; all candidate memberships, report hashes,
receipts and reassessed scores verified. Deleted-dependency and compensated-read
profiles are flat zero in both maps, with complete enabled-criterion coverage.

The late-event map also contains **two detected lost copied updates**. Both have an
unavailable overall score because the residual-effect criterion cannot attribute the
writer completely. These component findings remain recorded; they are not converted
into positive all-criteria fitness. The 20 fully scored positives are residual effects.
The distinction is recorded explicitly in `topic-orders-verification.json`, including
exact witness keys and component coverage. This prevents a summary of only available
scores from concealing a detected anomaly.

The third order delivers an event between an earlier read and a later write. Its
no-fault control succeeds in the requested order and detects one lost copied update.
The unchanged zero-control admission rule therefore excludes its fault map. This is
an application finding without an injected fault, not a failure to prepare the input.
No fault scenarios from that order were counted as measured.

## Extension complete: diverse application stories

All three extension batches have finished, been backed up and verified. The VM is
off before the booking ends. This extension contains **15 workload controls, nine
complete maps and 189 distinct scenarios in those maps**: 38 positive, 135 zero and
16 unavailable under all five criteria. There are no partial maps. The 195 recorded
attempts comprise 180 faulted attempts and 15 controls; six controls were not admitted
to fault collection. These counts describe this extension, not all prior campaigns.

| Workload story | Scenarios | Positive | Zero | Unavailable |
| --- | ---: | ---: | ---: | ---: |
| Topic update + Tournament update, late event | 62 | 20 | 26 | 16 |
| Topic update + Tournament update, no selected event | 35 | 15 | 20 | 0 |
| Anonymise student + query course execution + remove membership | 36 | 0 | 36 | 0 |
| Update name + add participant + find Tournament | 12 | 0 | 12 | 0 |
| Anonymise student + add participant | 14 | 0 | 14 | 0 |
| Add student + update name | 6 | 0 | 6 | 0 |
| Query course execution + update name + add participant | 12 | 0 | 12 | 0 |
| Find Tournament + remove Tournament | 8 | 2 | 6 | 0 |
| RemoveCourseExecution, three selected event actions | 4 | 1 | 3 | 0 |

Control exclusions are concrete and retained. RemoveCourseExecution + CreateQuiz is
rejected because the application refuses to delete a last course execution while its
course has questions. Removing a student's membership before changing their name
causes the name change to be rejected because that student is no longer enrolled.
AnonymiseStudent + CreateTournament and FindTournament + UpdateTournament trigger
application invariant exceptions. Their precise violated invariants were not diagnosed
in this campaign. AddParticipantAsync + FindTournament succeeds in the requested order,
but copied-update coverage is incomplete (unsupported concurrent thread and missing
UserDto input origin). The remaining exclusion is the positive no-fault lost-update
control already described above. None is silently classified as a zero fault map.

Selected event routes are not proof that an eligible receiver exists. The five zero
maps exercise their Saga operations; normal controls of three also complete a Tournament
event delivery (name/participant/query; anonymisation/participant; query/name/participant).
The other selected receivers in these controls are absent. The privacy/query/removal,
add-student/name and single RemoveCourseExecution controls have no eligible receivers
for their selected events. This qualifies the event-coverage claim without treating
absence as impact. Exact statuses are in `extension-control-details.json`.

Every non-flat map/profile received 30 paired seeds; flat profiles are explicitly retained.
Independent final checking verified 300 additional replay traces, unique visits,
matching endpoints and unchanged frozen source hashes. The tiny maps do not establish
a useful GA performance advantage: the four-case RemoveCourseExecution mean is 2.70
versus 2.63 evaluations to its one positive (slightly worse for GA), and the eight-case
map needs 5.83 versus 6.23 evaluations to both positives. The Topic maps show modest
mean savings to 80% (43.90 versus 47.13; 24.03 versus 26.73).

This extension therefore strengthens application-story coverage and demonstrates
specific observed conditions, but supplies **no new large benchmark family**. The
large-map preference comparisons remain the stronger search-efficiency evidence.
To expand that claim, the next collection should prioritise longer workloads with
actual eligible event receivers or supported asynchronous observation, rather than
adding more nominal event routes or more permutations of the same four Sagas.

Verification: `extension-completion-verified.json`, per-batch verification and search
summaries. All three archives remain on proteina01 and locally; compact references and
traces are retained without expanding raw reports. No paper edits or bandit implementation
were performed.
