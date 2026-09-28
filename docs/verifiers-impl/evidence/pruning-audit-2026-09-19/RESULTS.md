# Quizzes pruning audit

This audit originally showed that the then-current pruning rule omitted some pairs connected
through events or compensation. The historical measurements are preserved below; the
production corrections and independent-review closure are recorded in the later sections.
The audit has not established a lost impact finding at runtime. The earlier 98-to-40
experiment concerns segment compression only.

## Inventory

A fresh source extraction reproduces the eligible population of 817 inputs across 37 Saga
types. No input was removed by the cap. With the broad interaction rule:

| Pair inventory | Count |
| --- | ---: |
| All pairs of Saga types | 666 |
| Retained pairs | 248 |
| Discarded pairs | 418 |
| Discarded pairs with a potential one-hop event connection | 67 |
| Discarded pairs with a potential compensation-access connection | 6 |

The last two groups are disjoint in this snapshot. They compare extracted aggregate types,
not runtime object identities, so they are not counts of harmful or executable interactions.
35 discarded pairs have at least one pair of inputs from the same source test method.
The discarded pairs account for 186,250 input combinations, matching the earlier static
population: 309,568 before pruning and 123,318 afterwards.

## Concrete generator probes

The probes use original source inputs selected by a fixed rule, full interleavings, identical
event settings, and no compression. No application execution was performed.

| Pair | Plans without pruning | Plans with pruning | Enumerated scenarios, including controls |
| --- | ---: | ---: | ---: |
| UpdateStudentName + FindTournament | 16 | 0 | 64, including 16 no-fault cases |
| GetCourseExecutionById + FindTournament | 2 | 0 | 8, including 2 no-fault cases |
| UpdateTournament + FindTournament | 6 | 6 | 98, including 6 no-fault cases |

The retained pair has identical WorkloadPlan IDs in both packages. All 24 unpruned plans
pass static materializability checks. Runtime preparation and healthy controls remain
unqualified. The 98 scenarios here use the input-ID selection rule of this audit, not the
same inputs as the completed September 18 compression experiment; its runtime results
must not be imported into this probe.

The event probe has two normal Saga orders. Selecting delivery routes and their placements
expands them to 16 plans: two with no selected delivery, six with one, and eight with two.
Both the Tournament and QuizAnswer routes are present; eligibility is still a runtime question.
All canonical forward faults and recovery schedules yield 64 scenarios without truncation.

A fourth probe, Tournament UpdateUserName + DeactivateUser, has no same-source input pair.
It remains documented instead of synthesizing a manually linked fixture. The other five
compensation candidates likewise have no same-source pair in this input population.

## Why the event pair disappears

1. UpdateStudentName writes the student's copied name in CourseExecution (`Execution` in
   the extracted accesses).
2. It publishes UpdateStudentNameEvent.
3. A Tournament consumer can update the name held in that Tournament.
4. FindTournament reads the Tournament.

The producer's direct accesses and the query's direct accesses do not overlap. Generation
builds the conflict graph and chooses pairs first; event placements are added only to the
remaining plans. The known event connection therefore cannot rescue this discarded pair.
The source test used by the probe is `AddParticipantAndUpdateStudentNameTest`,
`sequential: update; add`. Its generated setup contains 14 operations and no AddParticipant
call. This is a specific qualification concern: the test's later participant addition was
not moved into setup. Static materializability alone does not establish the intended receiver.

The compensation example is also concrete: Tournament UpdateUserName reads/writes Tournament
in its forward steps, but compensation for `getParticipantStep` sends a User command to
release a semantic lock. The conflict graph compares only forward footprints. Whether that
recovery can interfere with another User operation requires matched runtime controls.

## Next experiment

Start with event-route eligibility and the two-query controls, before spending compute on
all faults. If preparation needs a different original input or an added application test,
record the selection change explicitly. Reuse the existing event-attribution diagnosis;
an incomplete read score is not a zero. Generate equivalent single-Saga controls before
claiming a new pair-only finding. At the time of this original audit the production pruning
rule had not been changed; that statement is superseded by the corrections below.

Docker did not respond to a bounded local availability check during this audit. No cluster
computation was attempted outside the reservation. The prepared catalogues contain 170
scenarios total (24 controls plus 146 faulted cases), before any extra isolated controls.
This is an enumeration budget, not a promise of 170 executable cases.

## Evidence

- Harness: `verifiers/experiments/pruning-audit/`.
- Raw extraction, input identities, probes, ordinary packages and complete enumeration:
  `verifiers/target/pruning-audit-2026-09-19-run1/`.
- `generation-manifest.json`: source/build/dependency hashes; sources unchanged during extraction.
- `inventory.json`, `probes.json`, `fault-enumeration.json`, `summary.json`: counts and receipts.
- Package hashes, exact retained subset and untruncated recovery receipts verified.
- Compact summary copied alongside this report. The earlier directory without `-run1`
  contains a harness compile failure (local variable name collision), no experiment results.

## Post-audit generator correction

The 817-input, 666-pair inventory above remains historical evidence for the old
forward-only selection rule. Production selection now augments direct forward conflicts
with compensation accesses and resolved one-hop event routes. Event receiver identity is
kept unbound to the producer input: strict mode needs exact static equality, while broad
mode admits unresolved identity but not a proven unequal key. Event edges are excluded
when their downstream Saga is already a selected participant, matching route placement.
The recovery checkpoint source and event trigger provide compression anchors. Minimal
WorkloadPlan evidence remains separate from the complete tuple-applicable anchor set.

A fresh source extraction and probe regeneration from the post-fix classes is under
`verifiers/target/pruning-audit-2026-09-19-final/`. The harness now supplies all extracted
Saga definitions while restricting accepted inputs to the probed pair, so event selection
can inspect its non-participant downstream Saga. Results are:

| Pair | Brute-force plans | Interaction-pruned plans | Enumerated scenarios |
| --- | ---: | ---: | ---: |
| UpdateStudentName + FindTournament | 16 | 16 | 64, including 16 controls |
| GetCourseExecutionById + FindTournament | 2 | 0 | 8 brute-force controls/faults, including 2 controls |
| UpdateTournament + FindTournament | 6 | 6 | 98, including 6 controls |

The event and direct probes have exactly the same WorkloadPlan IDs with pruning on/off.
The direct and unrelated-query probe IDs also match the pre-fix run. Package hashes,
distinct scenario IDs and untruncated recovery requests pass `analyze.py`. No application
scenario was executed. The event probe's 14-action setup still lacks AddParticipant, the
recovery probe still has no same-source tuple, and matched isolated controls remain pending.

## Follow-up anchor correction

Independent review showed that the deterministic minimal conflict-evidence subset was also
being passed to `SEGMENT_COMPRESSED`. With two recovery-capable steps against two reader
steps, the selection graph contained four recovery candidates but one Saga-pair evidence
edge was enough for connectivity; generation therefore wrote two orders while accounting
reported six. The corrected implementation keeps that minimal evidence for WorkloadPlan
identity and passes all four tuple-applicable candidates to scheduling. The review probe at
`verifiers/target/pruning-review-2026-09-19/probe-fixed.json` now reports one evidence edge,
six generated orders and six accounted orders. Direct evidence no longer suppresses a
distinct recovery/event anchor.

The source-hashed static audit was regenerated at
`verifiers/target/pruning-audit-2026-09-19-review/`. Its order-preserving audit probes remain
16/16 for the event pair, 0/2 for the unrelated-query control and 6/6 for the direct pair;
the 170 enumerated scenarios and 24 controls are unchanged. This audit still does not run
the application or qualify the event receiver, recovery setup, or matched isolated controls.

## Independent review closure

Two independent probes now pass against the final source-hashed review build. The first
has four recovery conflicts but one minimal evidence edge; generation and accounting both
retain all six compressed orders. The second uses BRUTE_FORCE with an unrelated third Saga;
all four A/B forward conflicts remain scheduling anchors even though the complete three-Saga
set is disconnected, so generation and accounting again retain six triple orders.
Interaction-pruned generation still rejects that disconnected triple.

Anchor extraction is therefore component-local and does not perform participant admission.
Connectivity remains an interaction-pruning concern. Reproducers and final receipts are in
`verifiers/target/pruning-review-2026-09-19/` as `ReviewProbe-final2.json` and
`DisconnectedProbe-final2.json`; both use the source-hashed
`pruning-fix-build-2026-09-19-final-review2` build. Runtime event eligibility, recovery setup
and matched isolated controls remain the only pending qualification items from this audit.

The chief-of-staff task independently inspected the final code, verified all 202 build
source hashes, recompiled and executed both probes, checked 54 audit artifact hashes and
the focused reports (128 tests, zero failures/errors/skips). Both probes independently
produced six generated and six accounted orders. Review receipt:
`verifiers/target/pruning-review-2026-09-19/chief-review-complete.json`.
Generator correction is signed off within this scope; application execution remains pending.

## Runtime qualification after review

Eight new no-fault controls ran in fresh native Java 21 processes using the hash-verified
frozen campaign runtime and its fixed clock. Docker did not respond within 12 seconds;
these are local functional diagnostics, not Docker parity or performance measurements.
Application code, scoring and source tests were unchanged. Each report join, report hash
and unchanged package hash was checked. Compact evidence is in
[runtime-qualification.json](runtime-qualification.json); scripts, packages, commands,
logs and receipts are under `verifiers/target/pruning-runtime-2026-09-19/`.

| Source story | Controls | Result |
| --- | ---: | --- |
| Original UpdateStudentName + FindTournament pair | 2 | SUCCESS / EXACT, complete zeros; selected Tournament delivery has NO_ELIGIBLE_SUBSCRIBER |
| Same pair from `sequential: add; update` | 2 | Same result: generated setup still has no AddParticipant |
| AddParticipant + UpdateStudentName + FindTournament | 2 | SUCCESS / EXACT; both deliveries actually reach Tournament 12 |
| GetCourseExecutionById + FindTournament | 2 | Both orders SUCCESS / EXACT, complete zeros |

The alternative pair was regenerated with the final reviewed generator. Choosing a source
feature that adds the participant earlier does not itself move that invocation into this
pair's generated setup. Both pair packages have the same 14 preparation calls, without
AddParticipant. The raw directory label `receiver-ready` denotes the attempted candidate;
the run established that it is **not** receiver-ready. These four successful empty-delivery
controls do not qualify the event-mediated interaction.

The triple reuses the existing source-derived September 16 package and includes the join
as measured work. It establishes the concrete sequence: add the student, update their name
in CourseExecution, deliver the event and retrieve Tournament. Delivery changes the copied
name to `UpdatedName`. Swapping only the query and delivery yields two successful controls:
query before delivery has complete zero scores; query after delivery has zero persistent
findings and zero lost copied updates, but compensated-read coverage is PARTIAL with
`PRODUCER_ACTION_UNPROVEN`. Combined five-criterion fitness is therefore unavailable for
the latter, not zero. This reproduces the existing event-writer attribution gap.

No faulted scenarios or new preservation comparisons ran. The generator correction remains
verified; the original pair needs a source-derived fixture with an eligible participant,
and the event-consumer read needs proper producer attribution before a complete-score
campaign. Recovery-only setup and matched isolated controls are still pending.

These setup/attribution follow-ups are now implemented and qualified in
[the subsequent experiment](../event-read-attribution-2026-09-19/RESULTS.md): the extended
ordinary test supplies membership, and eight focused pair scenarios have complete zero
scores. The two existing triple controls also have complete read attribution. The fresh
selection-only count admits 316/666 Saga pairs. Broader preservation remains pending.

## Subsequent bounded preservation comparison

Matched isolated controls and full fault enumeration for two discarded pairs are now
complete in [the 20 September experiment](../pruning-preservation-2026-09-20/RESULTS.md).
It covers 118 admitted scenarios, preserving one distinct finding after pruning keeps
12. Recovery-only pairs and a general application-wide preservation claim remain outside
that result. The historical pending statements above refer to their original audit stage.
