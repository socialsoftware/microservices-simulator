# GA evaluation: workload shortlist for discussion

The historical shortlist below records the earlier proposal. The 19 September breadth
queue is now running under the 19 September 02:00–09:00 cluster reservation, after
fresh package generation. The queue itself is not a runtime result; verified campaign
results belong in the [cluster report](cluster-campaign-2026-09-18.md).
The agreed outcomes are discovery of positive scenarios and discovery of higher
weighted scores over an equal number of application evaluations. Report execution
cost separately. The paper describes the final method; implementation chronology
belongs in repository evidence.

Follow-up: the name-update/join/query candidate was selected and qualified in
[this bounded experiment](../evidence/name-join-read-qualification-2026-09-16/README.md).
It has 12 exact candidates and a successful normal execution, but incomplete
compensated-read coverage for an event-produced version. It is not admitted to the
five-component search comparison. The inventory below retains the pre-execution proposal.

## Selection principles

- Select by application story, operation structure, event horizon and domain size
  before comparing GA and uniform random. Do not require observed positives.
- Keep workload-level results visible. Different schedules of the same source story
  are related cases, not independent applications.
- Freeze the exact inputs, setup, forward schedule, event routes and recovery cap.
  A family of operations is not yet a selected WorkloadPlan.
- Qualify preparation, actual control outcome, schedule conformance and all enabled
  evidence. A process that finishes with score zero may still reject the intended
  operation. Report exclusions and their technical reasons.
- The current complete-catalogue live runner requires SUCCESS/EXACT and complete
  zero-fitness control. A positive no-fault history can be scientifically relevant,
  but is outside that runner's present control gate; do not silently disable a
  criterion to admit it. Discuss a separate cohort or a deliberate gate change.
- Counts below are exact for retained historical workloads, not counts for arbitrary
  variants or a newly regenerated package. Refresh package/runtime compatibility
  before using them in the final campaign.

## Proposed shortlist

| Candidate | Concrete story and reason | Available size evidence | Remaining work |
|---|---|---|---|
| Join + update + retrieve Tournament | The update reads the original Tournament; a student joins; the update persists Tournament settings; a query reads them before the update reaches its Quiz step. Adds an explicit reader to the search domain. | 52 candidates, 36 vectors, no truncation, workload `090c5ee7a2cd61085a06b962331cf0c74f4126119335218c31c754313ef2e3b9`. | Fresh no-fault qualification with all five criteria. Semantic-lock effects may reject this interleaving. Retain that as a qualification result, not evidence of search effectiveness. |
| Update student name + join + retrieve Tournament | A student's name is updated in CourseExecution; joining copies student data into Tournament; the selected event handler propagates the name; a query observes Tournament. Adds copied data and asynchronous propagation. | Historical broad package has 348 WorkloadPlans in this family with statically materializable source setups, with 0–2 selected event occurrences. No complete fault-domain count established here. | Choose a coherent input tuple and normal ordering, then count and qualify. Input binding and source setup must be checked together. Do not infer readiness from setup alone. |
| Add student + remove student from CourseExecution | Enrol a student, then remove them; selected removal events notify related objects. Adds a membership lifecycle outside Tournament update/removal. | Historical broad package has 10 plans with statically materializable source setups, 0–2 event occurrences. No exhaustive fault count established here. | Check the strict-prefix setup does not pre-execute the measured enrolment. Select and disclose receiver presence/event horizon, count and qualify using current empty-delivery semantics. |
| Update Topic + update Tournament | Rename a topic while a Tournament update reads topics and rebuilds its configuration; event delivery can update copied topic data. Adds a different event-propagation story. | Previously counted fixed variants: 35, 36, 36, 62 and 44 candidates. They are separate workloads, not one combined catalogue. | Some earlier controls had incomplete read evidence. Requalify with current runtime. Already inspected detector/exploration family, so report prior exposure and avoid claiming a fresh held-out case. |
| Join + update + leave + remove Tournament | Student joins, settings change, student leaves, Tournament and Quiz are removed. Recovery may overlap later operations despite the normal forward order being serial. | 5,184 candidates / 216 vectors, complete retained domain. | Useful large-budget reference, already extensively explored. Final GA and uniform-random policies require fresh search trajectories; rescoring old trajectories does not provide them. |

The first three are the recommended next qualification targets. Candidate selection
within each family should use declared structural criteria, such as a coherent
source story and a normal successful ordering, with deterministic ID tie-breaking.
Do not cycle through orderings until the comparative GA curve looks favorable.
Event-free and event-bearing variants may form a matched comparison, but should
not be pooled as independent workload families.

## Existing results and cases not promoted

- The 186-case AddParticipant/UpdateTournament/LeaveTournament map informed a GA
  choice. Keep it as internal development/regression evidence or clearly disclosed
  development data, rather than the main independent confirmation.
- The 72-case AddParticipant/LeaveTournament/RemoveTournament map is existing
  confirmation on a related source family. Preserve it, but add structural variety.
- The other 72-case family, AddStudent/AddParticipant/SolveQuizAsync, is **not** that
  confirmation workload. Its retained control has PARTIAL_COMPENSATED terminal status
  and its old package lacks copy contracts. Forty zero scores do not establish a
  successful normal story or prove the remaining scenarios are zero.
- The 156-case AnonymizeStudent/GetCourseExecution/CreateTournament input defect
  was fixed. Its normal order anonymizes the creator before Tournament creation,
  which the application rejects. Do not revive the old materialization diagnosis or
  promote this as a successful control merely because its scores are complete zero.

## Cost and scope

For discussion only, use 20–35 seconds per fresh application execution on the Mac,
consistent with prior local runs, with no assumed parallel speed-up. A 52-case map
would take about 17–30 minutes; a 72-case map about 24–42 minutes; all 5,184 cases
about 29–50 hours of serial execution. These exclude enumeration, repeats and
diagnosis. Measure again in a pilot; cluster capacity is not yet known.

Map replay can then compare many search seeds without executing the application
again. It measures ordering under recorded feedback, not end-to-end search time.
Use real executions for performance measurement and repeatability checks.

No new runtime campaign, GA tuning, application change or paper edit was made for
this shortlist. The broad inventory examined was the retained September 9 package
under `verifiers/target/static-refresh-2026-09-09/current-checkout-broad/` (3,820
WorkloadPlans). Group counts here require a source setup marked materializable;
they are neither runtime qualification nor proof all participant arguments bind.
This is a bounded inventory of existing artifacts, not a fresh exhaustive scan of
the current application or of quizzes-full/quizzes-full-2.

## Evidence

- [Earlier cohort inventory](../evidence/workload-cohort-exploration-2026-09-15/README.md)
- [Integral-input fix and actual 156-case control](../evidence/integral-input-materialization-2026-09-15/README.md)
- [52-case and four-Saga source generation](../evidence/more-workloads-2026-09-15/README.md)
- [72-case confirmation selection](../evidence/ga-confirmation-2026-09-16/selection.json)
- [Complete-catalogue live integration](../evidence/catalogue-live-2026-09-16/README.md)

The source test `AddParticipantAndUpdateStudentNameTest` explicitly demonstrates
the normal update/add order and the add/update/event order, including why the
Tournament's copied name can lag before event delivery. This supports the second
candidate's story; it does not validate every generated interleaving.

## Breadth queue added on 19 September

André requested broader selection after the nine complete cluster maps remained
concentrated on Tournament stories. The next priority is breadth before further
variations of the same three Saga families. The completed cluster maps and local
540-search comparison are recorded in [the cluster report](cluster-campaign-2026-09-18.md).

The retained September 9 broad package contains 3,820 WorkloadPlans across 70 distinct
Saga-type combinations, including single-Saga cases. Of these plans, 3,535 have a source
setup marked materializable, across 54 combinations. These are historical, bounded
static counts, not total current executable Quizzes scenarios.

A concrete next queue selects **129 candidate plans across all 70 combinations**:
98 have a static setup candidate and 37 contain at least one selected event. These two
properties can overlap. Plans without setup remain explicit preparation follow-ups.

Selection: first visit every combination once, then make a second pass where another
plan exists. Within a combination prefer existing static setup evidence, then a frozen
hash of the workload ID. The second candidate prefers a different event count where
possible. Do not rank by fewer switches or observed positive/GA results. The package
and all source-file hashes are frozen in
[queue.json](../../../verifiers/target/workload-breadth-2026-09-19/queue.json);
`build_queue.py` in that directory reproduces the queue. This adds a new campaign queue
without modifying the completed cluster campaign's frozen selection.

Before execution, regenerate compatible source-derived packages and verify input/setup
identity under the current runtime. Missing copy-contract or event-read evidence is
not silently waived. The old package provides selection leads, not ready-to-run
five-criterion measurements. New generation may produce different IDs; preserve the
mapping through Saga/input/source provenance and disclose structural changes.

For each selected plan record separately: preparation, no-fault outcome, conformance,
and enabled-criterion coverage. The existing GA runner only admits successful exact
complete-zero controls; application rejections remain a separate cohort, not automatic
framework failures. For admitted catalogues, use a small fixed uniform-without-replacement
fault sample (proposed 12 cases, or the whole catalogue if smaller) to identify score
variation and collection cost. Zero findings in this sample do not establish an all-zero
catalogue. Freeze the final comparison cohort before GA results, retaining varied and
all-zero cases alongside candidates suspected to have sparse positives.

Overnight continuation option: finish the two near-complete maps (10 + 14 remaining
cases), then share effort between breadth qualification and the existing partial maps
rather than blocking every worker on unresolved preparation. The seven old partial maps
have 7,349 remaining candidates in total; this count is exact for those maps only.
No new reservation or application run was launched by this queue-preparation change.
