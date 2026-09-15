# Generated update/read qualification — handoff

State: **complete** for the [direct brief](BRIEF.md).

## What was missing

The retained space-map generation script explicitly chose `SERIAL` and
`max-schedules-per-input-tuple=1`. The enumerator sorts Saga names, so FindTournament
ran first. Its nine exact FindTournament/UpdateTournament workload records represented
input combinations, not nine interleavings. All lack setup; their 13 referenced input
facts are non-materializable. We did not establish a scheduling-generator defect.

Existing `ORDER_PRESERVING_INTERLEAVING` already generates the desired order. This
qualification uses the existing prerequisite descriptor mechanism and the existing
`quizzes-impact-v2-qualification@1` test provider to supply concrete inputs. Source
visitors/adapter extract the actual Saga steps; the normal generator and package writer
produce six workloads and 55 eager scenarios. The experiment explicitly selects the pair
and its preparation. It does not claim autonomous fixture discovery or a GA run.

## Bounded runtime fix

A declared prerequisite `java.util.Set<java.lang.Integer>` passed exact descriptor type
validation but failed runtime `Class.forName`. The same mistake occurred again when
materializing the provider-bound input. `ScenarioExecutor` now shares
`ScenarioMaterializer.loadClass`, which erases generic arguments for runtime lookup.
Declared types remain intact in package/report evidence; wrong raw container types and
missing bindings still fail. No recursive generic-element checker was introduced.

Only these two production files changed in this follow-up. Dummyapp-labelled executor
coverage supplies and actually consumes a generic Set binding and rejects a List.
**ScenarioExecutorSpec: 199 passed, zero failures/errors.** Existing tests cover the
materializer through executor composition; no separate ScenarioMaterializerSpec exists.

## Results

Fourteen fresh JVM/Spring/H2 executions use the ordinary ScenarioExecutor, fixed clock,
JSON serialization enabled, and the current production diagnostic. Controls run first.

| Read timing | No-fault controls | Last-step fault schedules | Update/read exposure |
| --- | ---: | ---: | --- |
| Before the Tournament update (three possible positions) | 3 | 3 | 0 |
| After the Tournament update, before the failed Quiz update (two positions) | 2 | 2 | **1 each with fault** |
| After the failed Quiz update, before Tournament recovery | — | 1 | **1** |
| After Tournament recovery, before or after lock recovery | — | 2 | 0 |
| After the successful Saga finishes | 1 | — | 0 |

All six controls are SUCCESS, with zero target anomalies and ImpactV2=0. All eight fault
runs are PARTIAL_COMPENSATED; the reader commits in every run. Exactly three fault runs
prove the exposure. Each has Tournament 11/v21 delivered to p1, followed by p2's explicit
recovery to v22. Recovery restores dates and question count. The independent validator
predicts the outcome from the generated action order, then checks actual order, the
pre-body assigned fault, completed recovery, reader completion and exact delivered version.

**The two metrics reveal different facts here.** All eight fault runs have ImpactV2=1:
Tournament recovery leaves `tournamentTopics` different because each restored embedded
topic loses `topicCourseAggregateId` (1 → null). The fixed clock keeps lastModifiedTime
unchanged; no other projected application attribute differs from baseline. Only three
of those eight runs also expose the subsequently compensated revision to another Saga.
The existing application defect was preserved; anomalies were not added to ImpactV2.

All eight fault diagnostics have COMPLETE_WITHIN_SCOPE collection. All six successful
controls have PARTIAL global diagnostic coverage because an internal CommitSagaCommand
is observed without reader attribution. Their target FindTournament call is present and
receives a definite NOT_OBSERVED verdict. This extra framework-command gap is retained,
not silently converted into complete global coverage or a positive anomaly. Cleaning up
non-read finalization classification is a bounded follow-up.

## Evidence and reproduction

- [Validation matrix](validation.json), [frozen selection](selection.json),
  [generation proof](generation-proof.json), [positive sidecar](positive-sidecar.json).
- [Build/source provenance and test proof](proof.json).
- [Experiment scripts and reproduction](../../verifiers/experiments/saga-update-read/generated/README.md).
- Raw source-derived package: `verifiers/target/saga-update-read/generated-04/package/`.
- Final runtime campaign: `verifiers/target/saga-update-read/generated-06/`.

Retained development attempts are not additional successful experiments: generated-01
failed harness compilation, -02 used the wrong descriptor generic spelling, and -03
omitted the ordinary CLI's source-mode template filter. Generation succeeded in -04;
its runtime attempt exposed provider type lookup failure and was stopped. The -05
attempt exposed the same issue in materialization and was stopped. Neither failed
preparation nor interrupted execution was included in the final 14-case matrix.
The final generator differs from the measured generator only by unused-import/whitespace
cleanup and was compiled separately. Package hashes are preserved on reuse.

Validation ran with Java 21 and the existing verified private Maven repository; the
prepared Docker runtime verifies all unchanged production sources/build artifacts and
compiles an explicit current overlay. No application/simulator production change,
branch, commit, push, PR or merge was made. Inherited documentation and detector changes,
including the personal note, remain intact.

## Next useful steps

1. Use this persisted pair in search evaluation. Its current persistent-object score
   cannot distinguish the three anomaly-positive schedules from the other five faulty
   schedules: all eight have score 1. Choose explicitly whether the separate anomaly
   count is another search objective; no automatic score sum was introduced.
2. Fix the separately demonstrated topic course-ID compensation loss when selected.
3. Improve source-derived input preparation separately if autonomous discovery of this
   pair is needed. The provider-backed qualification does not fix the retained 13 input
   facts. Clean up CommitSagaCommand diagnostic coverage classification independently.

## Subsequent input investigation

The [read-only input audit](INPUT-INVESTIGATION.md) refines the preparation diagnosis:
all seven retained read inputs already have a source-setup candidate; it is the six
updates that prevent complete pair preparation. Standalone materializable=false on the
thirteen input facts must not be read as thirteen missing source setups. The audit also
identifies dropped returned-DTO mutations and a clean success input excluded by the
campaign cap. No extraction fix is included in that investigation.
