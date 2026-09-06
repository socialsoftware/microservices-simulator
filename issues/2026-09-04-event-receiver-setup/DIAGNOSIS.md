# The earlier 577-candidate diagnosis

This is cohort evidence for the named package, not the current queue. Ranked items 1
and 2 were subsequently completed together: 88 inputs gained candidacy and none were
lost. See `../2026-09-04-nested-participant-setup-bindings/FINAL-HANDOFF.md`. The canonical
`docs/verifiers-impl/roadmap.md` owns remaining follow-ups; the historical counts below
are preserved so that the original partition remains reproducible.

## Reconciled measurement

The complete ordinary single-input selection is **796 = 576 with source setup + 1 needing
none + 219 blocked**. The577 is static setup candidacy, not577 successful executions.
The576 source-setup workloads reference475 distinct reusable setups. The944 written rows
also include event/provider variants and are not the denominator for this comparison.

The old seven-method dispatcher authorized every setup method in551 of576 candidates.
The three new exact registrations cover the remaining25: createQuiz is used by24;
startQuiz by15, all within those24; addParticipant by one additional workload. Thus all
**576/576** source-setup candidates now pass the method-availability check. This says
nothing by itself about domain values, test intent, target success, or event receiver state.
The current issue qualifies two newly enabled setup examples and three event executions.

Luna Max supplied the diagnosis; Astra independently recomputed796/577/219, the20-Saga
blocker partition,475 unique setups and25-row method union directly from the new package.
`COVERAGE-COUNTS.json` retains every blocked input/source and the measured aggregate counts.

## Ranked next work

1. **Nested participant DTO result bindings: three concrete quiz inputs first.**
   The source creates a QuestionDto and places it inside QuizDto.questionDtos. Setup action
   recipes preserve that reference, but participant binding only recognizes an argument-root
   producer. Reuse the existing recursive typed recipe mapping, preserving mutations and
   exact earlier occurrence identity. Likely+3, not a measured gain. Do not replay the
   measured createQuiz during setup. Proposed package:
   `../2026-09-04-nested-participant-setup-bindings/{SPEC,PLAN}.md`.
2. **The85 CreateQuestion inputs are the strongest larger cohort to inspect next.**
   For example CancelTournamentTest#createQuestion (setupHelper) contains
   QuestionDto.topicDto → HashSet → list → unresolved returned TopicDto, and a separate
   courseExecution.courseAggregateId dependency. QuizzesSpockTest's helper constructs this
   input from earlier fixture producers. All85 share the blocked Saga, but a successful
   general fix still needs exact helper provenance, set handling and target exclusion.
   Do not promise+85 merely by matching a diagnostic string.
3. **CreateTournament:39 blocked, with23 setupHelper rows as a focused next cohort.**
   These need creator/course IDs and a list of earlier topic identities together. A complete
   input tuple and exact producer occurrences matter; the other16 feature rows include
   broader preparation/control-flow context. Treat this as a separate follow-up after
   nested binding proof, not another guaranteed39.
4. **Improve failed-worker diagnostics.** A bounded parent repair could retain validated
   failure reports rather than replacing every nonzero worker exit with isolation failure.
   This improves diagnosis, not577. Its positive/negative validation boundary is recorded
   in the prior runtime follow-up; no additional repair was bundled here.

## Cases that should not be made green by relaxing checks

Event-payload placeholders occur in26 blocked rows, across AnonymizeUserTournament,
UpdateUserName, RemoveQuizAnswer, RemoveUserFromCourseExecution and UpdateUserNameInQuizAnswer.
Two DeleteUser rows have unresolved event/service placeholders. They need an actual causal
producer/event input strategy; inventing values is not input extraction.

UpdateTournament28, FindQuiz15 and RemoveTournament6 contain feature/control-flow or prior
facade-result dependencies. Repeated target calls, effects after assertions/workflow/event
barriers, and omitted producer effects must stay blocked until faithfully represented.
Luna also observed loopDependentMutation in7 rows, unsupportedTransform in1, and
unmaterializableAssignment in2. Those are overlapping diagnostic occurrences, not disjoint
categories or guaranteed gains.

Some source inputs come from negative tests or setup/helper calls. Raising candidacy should
mean faithfully reconstructing their input and prerequisite state, not claiming their
measured business action is expected to succeed. Broader runtime qualification is now more
useful because method authorization no longer obstructs these576 source setups.

## Complete disjoint blocker partition

These counts group the219 by target Saga, independent of overlapping reason labels.

| Saga | Blocked base inputs |
| --- | ---: |
| CreateQuestion | 85 |
| CreateTournament | 39 |
| UpdateTournament | 28 |
| FindQuiz | 15 |
| AnonymizeUserTournament | 13 |
| UpdateUserName | 9 |
| RemoveTournament | 6 |
| AddParticipant | 3 |
| AddStudent | 3 |
| CreateQuiz | 3 |
| CreateTopic | 3 |
| DeleteUser | 2 |
| AddParticipantAsync | 2 |
| RemoveQuizAnswer | 2 |
| CreateUser | 1 |
| CreateTournamentAsync | 1 |
| CreateCourseExecution | 1 |
| UpdateQuestionTopicsAsync | 1 |
| RemoveUserFromCourseExecution | 1 |
| UpdateUserNameInQuizAnswer | 1 |

The32 discovered Sagas with no accepted input are a separate extraction frontier and are
outside this796-input denominator. They were not exhaustively diagnosed in this task.

Luna's detailed report, compact JSON and read-only counter are preserved under
`verifiers/target/astra-next-diagnosis/`. The counter reproduces package joins and primary
reason/Saga buckets; its current unsupported-method value is a recorded zero, not a fresh
reflection check of the dispatcher. The exact ten registered signatures were separately
reviewed and tested by QuizzesSourceSetupActionDispatcherTest. The method union and counts
above were independently recomputed by Astra.

Luna's ordered triage groups sum to219:88 nested DTO/producer candidates (85 question +3
quiz),23 tournament setup helpers,28 event/bridge placeholders,10 explicit mutation/
transform/assignment boundaries, and70 remaining feature/prior-result cases. These are
source-pattern triage groups, not88+23 promised gains or a proof every case has one cause.
