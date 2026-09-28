# Name propagation, membership and retrieval: qualification

The selected story executes successfully, but does not qualify for the current
five-component catalogue search: compensated-read coverage is partial when the
retrieved version was written by an event consumer. Its complete domain contains
only 12 candidates. No faulted application executions or search comparisons were run.

## Story selected before execution

All three source inputs come from `AddParticipantAndUpdateStudentNameTest`, method
`sequential: add; update`. The ordinary generator was filtered to complete
AddParticipant before UpdateStudentName, and UpdateStudentName before FindTournament.
Among those schedules, selection required exactly the Tournament name-update route,
delivered between the update and the final query. Exactly one workload matched:
`b9cadedadcab970f16fc34504edff17f81e539429e706437e6431e70df71d64a`.

The source-derived setup uses 14 ordinary calls: create a course execution, create
and activate two users and enrol them, create three topics and three questions,
then create a Tournament with its Quiz. It does not enrol the measured participant
in the Tournament. There is no new test, manual input provider or business-code change.

The five measured actions are:

1. Read the enrolled student's data from CourseExecution.
2. Add that student as a Tournament participant, copying their original name.
3. Change the student's name in CourseExecution to `UpdatedName`.
4. Deliver `UpdateStudentNameEvent` to the Tournament handler, updating its copy.
5. Retrieve the Tournament.

Only the Tournament listener is selected. The separate QuizAnswer listener is not
part of this workload's event horizon. The event action is a generated consequence,
not a fourth independently selected top-level Saga.

## What the control proved

The fresh Docker/JVM/database execution completed all five actions with
`SUCCESS / EXACT`, in 38.19 seconds while enumeration shared the machine.
Final snapshots show student 4 named `UpdatedName` in CourseExecution and in
Tournament 12. The recorded query delivered Tournament version 27, which is also
the final version. Its producer is the selected event consumer.

Four criteria have complete zero counts: deleted dependencies, failed-operation
residuals, unresolved delivered events and lost copied updates. The compensated-read
criterion has zero observed findings but **partial coverage**, so combined fitness is
**null**, not zero. The catalogue runner's strict control check would reject it.

The precise gap is `PRODUCER_ACTION_UNPROVEN` on `call:10`. The report records the
producer of version 27 as `EVENT_CONSUMER`, phase `EVENT`, and its action completed.
`SagaReadExposureAssessor` currently seeks a proven forward Saga producer, with a
special case for recovery-produced versions. Its generic action match also compares
functionality and step names: the event writer names the handler, whereas the
recorded event action names its triggering Saga step. This event-produced version
therefore cannot be classified by the current rule.

This is a detector-scope/attribution limitation, not an observed application failure
or evidence of a dirty read. Supporting it requires an explicit event-producer
identity and completion/recovery interpretation; do not equate the handler's writes
with the publisher Saga's completion just because the action carries its participant ID.
No detector change or criterion disabling was performed for this qualification.

## Complete domain and cost

Generation produced five whole-participant-order workloads without hitting input,
schedule, workload or event-expansion caps. Structural generation took 20.23 seconds.
The selected workload has four fault slots across three Sagas. Under the current
canonical one-fault-per-Saga domain, there are `3 × 2 × 2 = 12` vectors: no fault or
one of the two AddParticipant steps; no fault or the name-update step; no fault or
the query step. Each vector has exactly one generated action/recovery sequence.
All 12 were enumerated with recovery cap 500 and no truncation in 31.54 seconds.

Those are **12 generated candidates, not 12 measured outcomes**. Only the all-zero
control was executed. A full map would need 11 additional application executions;
using this control's 38.19-second duration gives roughly seven minutes plus overhead.
Faulted cases may differ, and the control was not an isolated timing benchmark.

With population 8, a 12-case domain leaves only four evaluations after initialization.
Even after repairing coverage, this is a small functional/regression example rather
than a persuasive main GA evaluation workload. It does not replace the 5,184-case
budget-limited comparison or the existing complete maps.

## Reproduction and integrity

Raw artifacts: `verifiers/target/name-join-read-qualification-2026-09-16/`.
The retained generation command uses the existing source-selection helper and
immutable Docker image; config retains the same frozen runtime as catalogue-live.

```sh
python3 verifiers/experiments/fixed-workload-ga/run.py control \
  --config verifiers/target/name-join-read-qualification-2026-09-16/config.json \
  --output verifiers/target/name-join-read-qualification-2026-09-16/control-repeat
python3 verifiers/experiments/fixed-workload-ga/run.py catalogue \
  --config verifiers/target/name-join-read-qualification-2026-09-16/config.json \
  --output verifiers/target/name-join-read-qualification-2026-09-16/catalogue-repeat
```

Runtime hashes, unchanged application source hashes, retained report hashes, the
12-candidate enumeration and the read/write/final-version join were verified.
[summary.json](summary.json) records results; [artifact-hashes.json](artifact-hashes.json)
pins the evidence. No paper edits, GA tuning, commits or additional campaign launch.
