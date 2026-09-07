# Space-map setup qualification — 2026-09-07

This follow-up preserves the original 159-attempt campaign and qualifies a newly
generated package from the corrected setup extraction. It does not reinterpret the
original IDs as reruns and does not change ImpactV2, event replay, or Quizzes domain
behavior.

## Setup correction

The original `w01` setup executed `addStudent(...)` and then selected that same call as
the first measured participant. Its no-fault participant therefore failed with
`already enrolled`. The adapter now treats every exact facade occurrence in `setup()`
as a possible measured target. A binding for such a target uses only actions strictly
before the target and can bind later compatible participants from the same source
class. Provenance keeps the prefix applicable only when its frontier target is selected,
so it does not introduce ambiguity for tuples that should retain the complete setup.

In the regenerated package, the `w01` structural equivalent is workload
`538fce71e410c8ec2811b67fe0e048f2346d1f706761e6b72ffb27a5b134e16d` with setup
`setup-0268379c50749004aa44045d`. The setup contains only `createCourseExecution`,
`createUser`, and `activateUser`; it binds both participants and is statically
materializable. Its no-fault execution confirms all three setup actions and all four
forward steps complete. It then stops at the first selected QuizAnswer route because
that fixture contains no eligible QuizAnswer receiver. This is a receiver limitation,
not a repeated-target setup failure.

`w02` and `w07` also receive strict-prefix setups. `w03`–`w06` retain their previous
setup IDs. `w08` remains without one coherent setup: preparing its later participants
requires executing an earlier selected setup target. No action is deleted or reordered
to force that tuple to become executable.

## Receiver control

The source fixture `AnonymizeStudentAndSolveQuizTest` explicitly prepares a Tournament
participant for the `UpdateStudentName` event. Workload
`4beddc06499429db7ab95f6c1c3f16f968489b2ca97141965ac1da5aa667c9da` selects only
that Tournament route.

- vector `0`, scenario `e4adf1b55ee9370cf0afb648aac2a20a9960227c849f9e6cc2af691e684ef9af`:
  `SUCCESS / EXACT`; setup, trigger, and Tournament consequence all complete;
- vector `1`, scenario `f08728e387e09af8e71c48774852d0830f73069967451effb10f499185a8a282`:
  `COMPENSATED / EXACT`; the trigger has the assigned fault and the consequence is
  `MASKED_BY_TRIGGER_FAULT`.

This positive control proves that route replay works when its receiver is present. It
does not make a different fixture's missing QuizAnswer or Tournament receiver eligible.

## Classification of the original forty invalid executions

The forty invalid discovery attempts form six event-bearing groups, not forty setup
bugs:

| Group | Invalid attempts | Setup/participant finding | First receiver hard stop |
|---|---:|---|---|
| `w01` | 6 | repeated AddStudent target; corrected by strict prefix | QuizAnswer absent; Tournament not reached |
| `w02` | 6 | repeated AddStudent target; corrected by strict prefix | Tournament can complete; QuizAnswer absent |
| `w03` | 6 | setup unchanged | QuizAnswer absent; prepared Tournament not reached |
| `w04` | 4 | setup unchanged | Tournament absent; QuizAnswer also absent |
| `w05` | 10 | independent unassigned `Aggregate 12 breaks invariants` on AddParticipant | Tournament can complete; QuizAnswer absent |
| `w06` | 8 | setup unchanged | QuizAnswer absent; Tournament also absent |

`SELECTED_SUBSCRIBER_NOT_FOUND` remains a valid hard stop for a selected route with no
eligible receiver. Correcting setup-prefix ownership therefore removes the unassigned
`already enrolled` failures from the affected controls but deliberately does not turn
all forty historical invalid executions into valid scores.

## Raw evidence and integrity

Raw build, generated package, private Maven cache, and runtime reports remain under
`verifiers/target/space-map-setup-qualification-03/`. The qualified package contains
3,820 workloads and 12,395 initial FaultScenarios. A final regeneration after the
ambiguity guard produced byte-identical accounting, Saga, input, interaction, setup,
workload, FaultScenario, request, and manifest files. A final local regeneration after
the missing-occurrence fail-closed guard was also byte-identical to that Docker package;
the runtime reports therefore
refer to the same package content under the earlier generated directory. The retained
report hashes are in
[`summary.json`](summary.json). The Docker image was
`sha256:0aab59d58bfe4f83e6bee2a4002a913dcbc26d861acee5f0327c053f12918282`.
