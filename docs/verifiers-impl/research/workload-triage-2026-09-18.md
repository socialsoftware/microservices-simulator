# Workload triage for further GA evaluation, 2026-09-18

This note narrows the next workload choices after the complete 5,184-case map. It is a
selection and qualification report, not a GA result. The existing four-Saga map contains
3,979 positive, 657 zero and 548 unknown cases, so 76.8% of the entire catalogue is
positive. Its final 30-seed recorded-feedback comparison shows GA gains, but it does not
answer how the policies behave on a sparse or event-oriented landscape.

The triage reused existing exact catalogues and runtime evidence. It made four fresh
application attempts, with one worker, against the exact frozen runtime descriptor in
`verifiers/target/full-map-5184-2026-09-17/config.json`. The limit was 12 attempts. No
faulted attempt was run before the no-fault control, and no broad random scan was run.

## Follow-up qualification

The later [workload-unlock report](../evidence/workload-unlock-2026-09-18/README.md) records two verified unblock paths, ten additional executions, and the remaining event-origin and helper-resolution follow-ups. Use that report for current readiness; the controls below retain the original triage evidence. These implementation follow-ups are tracked here rather than in the meeting agenda.

## Candidate inventory

Current follow-up: the [event-read attribution qualification](../evidence/event-read-attribution-2026-09-19/RESULTS.md)
now repeats both delivery/query orders of the name-update triple with complete zero scores.
Candidate 4's recorded frozen-runtime failure below is historical; its normal control is
unblocked by the new assessor. Candidate 2 was also repeated with that assessor: its
normal execution is SUCCESS / EXACT with complete zero counts in all five criteria.
The event-produced Tournament read is attributed to the completed delivery. The 44-case
fault domain is now eligible for fresh measurement; its old 17/22/5 map remains unchanged.
The new control receipt and verified report hashes are in
`verifiers/target/event-read-attribution-2026-09-19/topic-control-final/verified-summary.json`.
The table below records the original triage, not these later qualification results.

| Priority | Fixed workload | Exact structural size | Setup/runtime status | What is known | Decision |
| --- | --- | ---: | --- | --- | --- |
| 1 | AddParticipant + UpdateTournament + FindTournament (`090c5e…`) | 36 vectors, 52 candidates | Source setup exists. Fresh current-runtime control is `PARTIAL_COMPENSATED / DEVIATED`, not `SUCCESS / EXACT`. | The update reads and locks Tournament before AddParticipant. The join is rejected with `Aggregate is being used in IN_UPDATE_TOURNAMENT saga`; observed I, A and lost-copy count are all zero with complete component coverage. | Reject under the current catalogue-search control gate. It is useful only as an explicitly separate semantic-lock rejection cohort. |
| 2 | UpdateTopic + event + UpdateTournament (`09012c…`) | 18 vectors, 44 candidates | Fresh current-runtime control is `SUCCESS / EXACT`, but compensated-read coverage remains partial. | The full retained map has 17 positive, 22 zero and 5 unknown scores. Every scored positive is a failed-operation residual; there are no lost-copied-update positives in this fixed early-event workload. | Keep as calibration only. It fails the complete five-component evidence gate. |
| 3 | RemoveCourseExecution + Answer/Quiz event receivers (`46ba08…`, reverse order `ebb440…`) | Exactly 4 candidates per fixed route order | Both fresh current-runtime controls are `SUCCESS / EXACT`, I=0 and A=0. Lost-copy coverage is unavailable because the retained package has no inferred copy contracts. | Earlier COMPLETE ImpactV2 evidence has a clean event gradient: both deliveries score 0, one selected delivery scores 1, no deliveries score 2; a trigger fault masks both deliveries and scores 1. | Structurally executable event workloads, but blocked from the five-component comparison by package/evidence coverage. |
| 4 | AddParticipant + UpdateStudentName + Tournament event + FindTournament (`b9cade…`) | 12 vectors, 12 candidates | Current frozen runtime control is `SUCCESS / EXACT`, but combined fitness is unavailable. | The final query sees the propagated Tournament version. Compensated-read assessment reports `PRODUCER_ACTION_UNPROVEN` because that version was written by an event consumer. | Blocked by detector coverage; do not spend faulted executions yet. |
| 5 | FindTournament + RemoveTournament + UpdateTournament (`06e3d9…`) | 48 vectors planned; no valid catalogue | No setup. Eighteen generator requests were all rejected. | The inputs come from `RemoveTournamentAndUpdateTournamentTest`, but p1/p2 arguments are unresolved and p3 also has a not-ready property receiver. | Blocked before runtime. Do not repair it inside an evaluation campaign. |

## Inputs and provenance

The 52-case Join/Update/Query workload uses three inputs from
`AddParticipantAndUpdateTournamentTest.'update tournament then add participant'`:

- AddParticipant `ff7ce508…`;
- FindTournament `0f5261c6…`;
- UpdateTournament `09a5a31c…`.

The 44-case two-write workload uses UpdateTopic `5bb770d3…` and UpdateTournament
`b9654290…`, both from `UpdateTournamentTest.'update topic and tournament successfully'`.
Its selected event is `updateTopicStep#0/event#0-route#1`.

The event-propagation workload uses RemoveCourseExecution input `fdcbce1a…` from
`RemoveCourseExecutionQuizAnswerReceiverTest.'RemoveCourseExecution event route has an
eligible quiz answer receiver'`. The two selected routes target Answer and Quiz state;
the two workload IDs differ only in route order.

The name-propagation workload uses AddParticipant `7f8311d0…`, UpdateStudentName
`c5d07dad…` and FindTournament `91b056e0…`, all from
`AddParticipantAndUpdateStudentNameTest.'sequential: add; update'`.

The blocked query/removal/update workload uses FindTournament `0d52de15…`,
RemoveTournament `b1337535…` and UpdateTournament `55d483cd…` from
`RemoveTournamentAndUpdateTournamentTest`. These source calls are real, but this exact
tuple is not a runtime-ready WorkloadPlan.

Full IDs and paths are in
`verifiers/target/workload-triage-2026-09-18/inventory.json`.

## What the new control establishes

The fresh Join/Update/Query control used the same frozen runtime object as the 5,184-case
map. It completed in 21.66 seconds with execution-attempt id
`c914d1f7-e7a7-4131-a993-2937cedd467e`. UpdateTournament first read the Tournament;
AddParticipant then encountered the existing `IN_UPDATE_TOURNAMENT` semantic lock. The
executor followed the supported fallback and reported `PARTIAL_COMPENSATED / DEVIATED`.

This is useful negative qualification evidence. The exact 52-case domain still exists,
but this fixed order is not eligible for the standard live catalogue runner's
`SUCCESS / EXACT`, complete zero-fitness control gate. A zero in this control is not evidence
that the 51 faulted cases are zero.

Preselecting another forward order because it has a `SUCCESS / EXACT` no-fault history is a
legitimate executability criterion when the rule is declared before fault outcomes and GA
results are observed. It is different from cycling through orders to retain the one where GA
looks strongest. Any newly selected order is a different fixed workload and needs its own
identity, exact domain, control and provenance.

## Completed limited qualification

Both RemoveCourseExecution route orders enumerate to exactly four candidates over four
canonical vectors, without truncation at recovery cap 500. Their current-runtime controls
are `SUCCESS / EXACT`, with complete I=0 and A=0. Both fail the five-component evidence
gate for the same package-level reason: `copy-contracts.json` is absent, so lost-copy
coverage is `UNAVAILABLE` with `COPY_CONTRACTS_UNAVAILABLE` and
`OBSERVER_START_FAILED:NoSuchFileException`.

The current-runtime UpdateTopic/UpdateTournament control is also `SUCCESS / EXACT`, with
I=0, A=0 and complete lost-copy coverage. Its compensated-read coverage remains `PARTIAL`:
the event-consumer-produced Tournament version yields `PRODUCER_ACTION_UNPROVEN`. This
reproduces the historical blocker on the current frozen runtime.

No workload passed all control gates. Consequently no faulted probe was authorized by the
predeclared protocol. The qualification used three new controls plus the earlier
Join/Update/Query control: four fresh application attempts total, one worker, within the
twelve-attempt cap.

## Result of the bounded execution plan

The two event orders were enumerated and all three planned controls were executed. The two
event controls failed at lost-copy coverage; the two-write control failed at compensated-read
coverage. Therefore the conditional probe stage did not start. Pilot zeros remain pilot
zeros and are not reported as absence of positives.

Adapting the old event package to the five-component runtime would require regenerating a
source package with inferred copy contracts and qualifying its identity and evidence. That
is broader than this triage and was not attempted. The next step is a separately approved
package-regeneration or detector-coverage task, followed by these same controls before any
faulted probes.

For a later cluster campaign, freeze workloads before measuring outcomes. Report each
workload separately, preserve unknown fitness in the denominator, use the existing five
unit weights, and compare GA with uniform catalogue random at equal application-evaluation
budgets. The event workload may be too small for a persuasive GA result after exact
enumeration; if so, it remains detector/propagation coverage rather than being padded or
pooled with another family.

## Evidence used

- `docs/verifiers-impl/evidence/workload-cohort-exploration-2026-09-15/README.md`
- `docs/verifiers-impl/research/ga-evaluation-workload-shortlist.md`
- `docs/verifiers-impl/evidence/name-join-read-qualification-2026-09-16/README.md`
- `docs/verifiers-impl/evidence/combined-events-2026-09-06/comparison.json`
- `docs/verifiers-impl/evidence/lost-copied-update-2026-09-15/README.md`
- `verifiers/target/more-workloads-2026-09-15/domain-counts/add-update-find-090c/`
- `verifiers/target/workload-triage-2026-09-18/add-update-find-control/`

The report does not change application, verifier, detector, search or fitness code. It does
not reinterpret old unknown scores as zeros, and it does not select a workload because a
pilot made GA look favourable.
