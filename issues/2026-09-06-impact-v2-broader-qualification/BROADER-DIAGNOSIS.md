# Broader ImpactV2 diagnosis

The observer and control-prerequisite follow-ups are completed in
[coverage and control qualification](../2026-09-06-impact-coverage-controls/HANDOFF.md).
The refreshed selection has 30 complete pairs with successful exact controls, versus
17 in this earlier campaign. The counts and blockers below preserve historical evidence.
Production application defects and scoring-policy decisions remain separate follow-ups.

> Subsequent correction: semantic-lock analysis and the executor completion check were
> incomplete during this campaign. The 34-to-17 projection does not establish equivalent
> recovery; some omitted read-payload checkpoints had real semantic-state rollback.
> Preserve the reports as observations, with fresh qualification in
> `../2026-09-06-semantic-lock-recovery/HANDOFF.md` governing the corrected behavior.

State: 60 of 60 planned attempts were run and produced both ScenarioExecutor and
ImpactV2 reports. This document diagnoses the frozen broader sample; it does not change
the score, replace a selected workload, or repair an application or harness defect.

## What was sampled

The selection froze 30 control/late-fault pairs before runtime results: one eligible
single-participant workload for each of 26 of the 68 discovered Saga types, plus a
maximum-event shape for four eligible event-bearing Sagas. The other 42 Saga types had
no eligible single-participant workload under this selection procedure. That is a
catalogue/setup result, not proof that all 42 types are inherently unexecutable.

Each attempt used its persisted package workload and fault scenario through the ordinary
ScenarioExecutor, with a fresh JVM/H2 database. The sample does not exhaust inputs,
fault positions, recovery schedules, event routes, concurrent pairs, or all workloads
for a Saga.

## Attempt and assessment accounting

| Measure | Count |
| --- | ---: |
| Planned and attempted | 60 |
| ScenarioExecutor reports | 60 |
| ImpactV2 reports | 60 |
| Process exit 0 | 51 |
| Process exit 1 | 9 |
| Assessment `COMPLETE` | 47 |
| Assessment `PARTIAL` | 4 |
| Assessment `INVALID` | 9 |
| Complete score 0 | 39 |
| Complete score 1 | 7 |
| Complete score 2 | 1 |
| No complete score (`null`) | 13 |

The 47 complete reports contain eight failed-operation residual positives and one
deleted-dependency positive. No unresolved-delivered-event positive was observed in
this sample. The 13 partial/invalid results remain unknown or invalid; none is counted
as zero.

At pair level, 21 of 30 pairs have two `COMPLETE` assessments. Seventeen pairs also have
a successful, exact control and an exact fault attempt, so they support a clean paired
comparison. Three of those 17 clean pairs have a positive fault result and a complete
zero control; the other 14 are zero/zero. Four more pairs have complete assessments but
an unhealthy control. The remaining nine pairs are limited by partial coverage or an
invalid attempt.

## Fault-associated positives with healthy controls

| Pair | Control | Late fault | Evidence |
| --- | ---: | ---: | --- |
| `CreateQuestion-basic` | 0 | 1 | `FAILED_OPERATION_RESIDUAL`: a new active `SagaQuestion(4)` is absent at baseline and remains after the fault and reported recovery. |
| `RemoveCourseExecution-basic` | 0 | 1 | `FAILED_OPERATION_RESIDUAL`: `SagaCourse(1).courseExecutionCount` changes from 1 to 0 before the removal fault and is not restored. |
| `RemoveTournament-basic` | 0 | 2 | One active `SagaTournament(10)` retains a dependency on deleted `SagaQuiz(9)`, and that deleted Quiz is also a failed-operation residual. The score is two distinct affected identities. |

For `CreateQuestion`, the fault occurs before the final course-question-count update,
after `createQuestionStep` has created the Question. The Saga source registers recovery
for the course and topic reads but no restoring compensation for the creation step, so
the active Question remains. The successful control also creates the Question, but a
successful committed operation is outside the failed-operation residual condition.

For `RemoveCourseExecution`, the count-decrement step commits before the fault on the
removal step. The source registers no restoring compensation for the count update.
Both the basic and event-shaped late-fault attempts therefore report the same
`SagaCourse(1)` finding. The event shape is not a second independent defect: its control
is invalid because no subscriber is available, so only the basic pair supplies a clean
fault/control comparison.

`RemoveTournament` reproduces the two-object union established by the focused M2
qualification. The category counts are one deleted dependency and one failed-operation
residual; deduplication is by affected identity across categories.

These findings identify persistent differences under the current three checks. They do
not by themselves establish severity or universal domain incorrectness.

## Positive controls and non-attributable positives

`SolveQuiz-basic` and `SolveQuizAsync-basic` each have score 1 in both the control and
late-fault attempt. Both controls fail without an assigned fault at `solveQuizStep`
because the selected user is not enrolled in the selected tournament. Recovery deletes
the newly created QuizAnswer. The corresponding baseline contains no QuizAnswer and the
final state contains a `DELETED` `SagaQuizAnswer` tombstone (`13` for the synchronous
case and `14` for the asynchronous case).

The failed-operation residual contract deliberately compares the full lifecycle-bearing
persistent state. Absence at baseline and a deleted final record are different, so each
tombstone is counted as a new persistent remnant even though the active object was
removed. This may be expected compensation storage rather than broken usable data. The
matched control already has the same score and affected identity, so these pairs show no
fault-specific increase and support no domain-harm claim.

`RemoveCourseExecution-events` has a late-fault score of 1, but its control is invalid.
It duplicates the same count-decrement residual proven by the healthy basic pair and is
retained as evidence with that limitation.

## Partial measurement coverage

Four reports are `PARTIAL`, all because the generic persistent mapper detects the exact
nested owned back-reference
`SagaTournament.tournamentParticipants[0].participantAnswer.tournamentParticipant`.

- `AddParticipant-basic-control` and `AddParticipantAsync-basic-control` record the
  cycle during both the committed write and final snapshot. Their late-fault attempts
  finish `COMPLETE` with score 0 because the participant structure is not created before
  the selected fault. A complete fault report cannot turn its partial control into a
  zero/zero pair.
- Both `LeaveTournament-basic` attempts are partial. The control has one snapshot cycle;
  the late-fault attempt observes the same path in two snapshots. Category-level unknown
  reasons are deduplicated to `PERSISTENT_MAPPING_CYCLE`.

In these reports, deleted-dependency and event coverage remains complete, while
failed-operation-residual coverage is partial and the overall score is null. Supporting
this owned cyclic object shape is the clearest metric-coverage limitation exposed by the
sample.

## Invalid setup and event routes

Six invalid reports reach Spring startup and complete source setup, but fail
materialization before any measured Saga action:

- `CreateTopic-basic` control and fault cannot materialize constructor argument 2, a
  `TopicDto` with literal `name=TOPIC_NAME_1`.
- `UpdateStudentName-basic` and `UpdateStudentName-events`, both control and fault,
  cannot materialize constructor argument 3, a `UserDto` with literal
  `name=Updated Name`. Their source setup successfully binds the course-execution and
  user IDs first.

This separates setup-plan eligibility from complete argument runnability. The frozen
selection was not changed after these failures appeared.

Three event-shaped controls are invalid after their trigger steps commit:
`AnonymizeStudent-events`, `RemoveCourseExecution-events`, and
`RemoveStudentFromCourseExecution-events`. Each ends with
`SELECTED_SUBSCRIBER_NOT_FOUND`. Its source setup creates the direct Saga inputs but no
eligible QuizAnswer receiver for the persisted event route. The paired late fault occurs
at the trigger boundary and masks the event, so that attempt can complete; it does not
make the invalid control usable for fault attribution. This is a runtime event-fixture
coverage gap, separate from ImpactV2 category coverage.

## Complete reports with unhealthy controls

Four pairs have two complete ImpactV2 reports but cannot be treated as clean fault
comparisons:

- `CreateCourseExecution-basic`: both control and fault are `COMPENSATED/DEVIATED` with
  score 0. The source setup has already created the same `TESTBLCM` / `2022/2023` course
  execution, so the unassigned creation step fails as a duplicate before the selected
  late fault is reached.
- `FindQuiz-basic`: the control is `COMPENSATED/DEVIATED` with score 0 because its
  persisted literal Quiz ID 11 does not exist in the setup. The fault attempt stops at
  the selected boundary before that lookup and is exact with score 0.
- `SolveQuiz-basic` and `SolveQuizAsync-basic`: the controls have the enrollment failure
  and positive tombstone residual described above; the late-fault attempts have the same
  score.

Complete category evidence means the metric evaluated the observed attempt. It does not
make an unhealthy zero-fault execution a valid behavioral baseline.

## Reproducibility references

- Selection:
  `verifiers/target/impact-v2-broader/broader-selection.json`
  (`dca5f4c4dfa2a2777feddc511e1ac85aad0645f215bbff1fd35881affb5fbd5a`)
- Package manifest SHA-256 recorded before execution:
  `cd99c268cbba94f13e51b2f8845020957b9527e6ebef0c650eb451680342b436`
- Source-content manifest:
  `verifiers/target/impact-v2-broader/run-01/source-content-manifest.tsv`
  (`02309b525d01ad4e98b1e5eb174cd43979945b9391a755359f494b70f6ac1b2c`)
- Attempt index:
  `verifiers/target/impact-v2-broader/run-01/all-attempts.tsv`
  (`fb893878b31a591cac0478207123e9a2787a525cfc593a2b8aba90957a84e3c6`)
- Per-attempt reports and logs:
  `verifiers/target/impact-v2-broader/run-01/broader/attempts/<caseId>/`

The historical 34-row comparison and the rejected/then-rerun 17-case application wrapper
are separate cohorts. Their results should not be merged into these 60 attempts or used
to change the score semantics.
