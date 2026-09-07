# Repeating the forty empty-receiver failures

All **40 previously INVALID discovery attempts now have COMPLETE ImpactV2 assessments
with score 0**. The same forty IDs represent 23 distinct workload/vector/ordered-action
sequences. Equal-sequence IDs agree on the bounded semantic summary in every group.
The run took 494.5 seconds, with two fresh Docker/JVM/H2 attempts at a time.

This closes the full former-invalid cohort, not the entire 159-attempt space map.
The unchanged old packages run on the current qualified production build at `ad747affc`.
The generator was not rerun and known old setup recipes were deliberately preserved.
This comparison also spans prior runtime/scoring corrections; it is not an isolated
one-change causal experiment. Historical reports remain unchanged.

| Original group | Operations | Attempts / distinct sequences | Empty route attempts | Actual deliveries |
| --- | --- | ---: | ---: | ---: |
| w01 | AddStudent + RemoveStudentFromCourseExecution | 6 / 3 | 12 | 0 |
| w02 | AddStudent + UpdateStudentName | 6 / 3 | 6 | 6 |
| w03 | UpdateStudentName + AddParticipant | 6 / 3 | 6 | 6 |
| w04 | RemoveStudentFromCourseExecution + UpdateStudentName | 4 / 2 | 8 | 0 |
| w05 | GetCourseExecutionById + UpdateStudentName + AddParticipant | 10 / 6 | 10 | 10 |
| w06 | AnonymizeStudent + GetCourseExecutionById + RemoveStudentFromCourseExecution | 8 / 6 | 16 | 0 |
| Total | | 40 / 23 | 58 | 22 |

## What completed means

All forty processes exited successfully and produced joined, complete assessments with
unique attempt IDs. No replay hard stop, timeout, infrastructure failure or observation
gap remained. This does **not** mean all application operations succeeded:

- Three executions ended `SUCCESS`; 37 ended `PARTIAL_COMPENSATED`.
- Thirty followed the planned schedule exactly; ten reported supported `DEVIATED`
  conformance and an unassigned application exception.
- All forty have ImpactV2 score 0: none of the three current persistent-state checks
  reported a positive object. This does not establish universal domain correctness.
- Four attempts have ImpactV1 score 1 from invariant exceptions, while ImpactV2 is 0.
  The two measurements retain different meanings; exceptions were not hidden or
  converted into persistent impact findings.

## Concrete executions

**w03-01: a missing QuizAnswer does not prevent a real Tournament delivery.** The
student's name is updated. The selected QuizAnswer route finds no receiver. Execution
continues through the participant operation's user read, delivers the name update to
the existing Tournament, and adds the participant. It finishes `SUCCESS / EXACT`,
ImpactV2 0. The old execution stopped at the empty QuizAnswer route.

**w04-01: continuing exposes a later rejected operation.** The first operation reads
the course execution and removes the student. The next event attempt finds no receiver.
Previously that stopped execution. Now the later name-update step is reached and fails
with `Student with aggregate id 3 not found in course execution 2.` The remaining selected
route is also empty. The result is `PARTIAL_COMPENSATED / DEVIATED`, ImpactV2 0. The
exception stays visible. This is a newly reached rejection, not proof of persistent
damage or a newly introduced application defect. The same observation appears under
w04-02, an equivalent old ID: two attempts, one distinct sequence.

The other eight unassigned-exception attempts already contained the same failures in
the old reports: four duplicate-enrolment attempts (w01/w02) and four Tournament
invariant-rejection attempts (w05). Their old packages have not become healthy controls
merely because event replay can now continue. Across the eleven selected zero-fault IDs,
three finish `SUCCESS / EXACT` and eight expose unassigned failures. They cover six
workloads; only w03 and w06 have successful no-fault controls in this retained package.

## Evidence and next use

[Comparison](comparison.json) retains all forty scenario/workload/vector identities,
sequence keys, old/new report hashes, terminal/conformance/score outcomes, event counts,
and old/new unassigned exceptions. [Proof](proof.json) identifies the unchanged package,
qualified build/image, raw run and integrity checks. Full execution reports, impact
sidecars and Docker logs remain under
`verifiers/target/empty-event-delivery/forty-invalid-01/`.
Reproduce with `verifiers/experiments/empty-event-delivery/rerun_invalid.py`; the command
and scope are documented in the parent experiment README. Source/build/dependencies,
image, package, historical evidence and launch code were checked before and after.
There was one attempt per ID, no retries and no receiver synthesis.

The execution-policy blocker is resolved for this cohort. Before using these workloads
as a new search baseline, regenerate the old setup recipes with the current extractor
and qualify the zero-fault controls. Do not infer new current totals for the other 119
old attempts, or ask search to optimize this all-zero subset as if it were a demonstrated
positive landscape. The separately qualified delivered-but-unresolved Question case
still supplies a positive calibration control; it was not part of these forty repeats.
