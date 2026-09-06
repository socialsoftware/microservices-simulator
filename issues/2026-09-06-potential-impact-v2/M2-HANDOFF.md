# M2 qualification handoff

State: complete (`run-03` validation PASS).

## Scope and route

The qualification uses the bounded provider-backed Quizzes fixture under
`verifiers/experiments/impact-v2/`. It is explicitly labeled
`qualification-only-provider-backed-not-source-extracted`, to distinguish this deliberately supplied setup/input/event-placement fixture from
source-extraction coverage. The fixture uses production scenario model, ID, generator,
and writer APIs. It does not establish that those workload shapes are source-generated.
Every measurement selects a persisted `FaultScenario` from the generated package and
runs it through the ordinary `ScenarioExecutorCli` path in a fresh JVM and in-memory H2
database.

No simulator, verifier, or Quizzes production code was changed by M2. The two controlled
Quizzes variants were built from immutable temporary copies using byte-identical copies
of the trusted `impact-updates` patches. The runner built serially with a 1536 MiB heap
and 512 MiB metaspace cap. Python was installed only in each disposable `--rm` runtime
container; no image was committed and no existing service was stopped or restarted.

## Final result

`verifiers/target/impact-v2-qualification/run-03/validation.json` reports `PASS` for all
eight scored cases and three mandatory observer-disabled comparisons.

| Case | Assessment | Deleted dependency | Failed-operation residual | Unresolved delivered event | Complete score |
| --- | --- | ---: | ---: | ---: | ---: |
| deleted assigned | COMPLETE | 1 | 1 | 0 | 2 |
| deleted unassigned | COMPLETE | 0 | 0 | 0 | 0 |
| update assigned, normal compensation | COMPLETE | 0 | 1 | 0 | 1 |
| update assigned, no-op compensation | COMPLETE | 0 | 1 | 0 | 1 |
| update unassigned, normal build | COMPLETE | 0 | 0 | 0 | 0 |
| update unassigned, no-op build | COMPLETE | 0 | 0 | 0 | 0 |
| question event, current build | COMPLETE | 0 | 0 | 1 | 1 |
| question event, repaired build | COMPLETE | 0 | 0 | 0 | 0 |

Every scored report has schema
`microservices-simulator.scenario-impact-v2-assessment.v1`, collection status
`OBSERVED`, horizon `FINAL_SCHEDULED_ACTION`, all three category coverage statuses
`COMPLETE`, empty coverage gaps and unknown reasons, and exact integer
`completeScore`/`observedAffectedObjectCount` values. The deletion score is the union of
the surviving Tournament with its deleted Quiz dependency and the deleted Quiz residual;
the same object is not counted twice across categories.

The three observer-disabled attempts have `assessmentStatus=UNAVAILABLE`,
`assessmentReason=COLLECTION_DISABLED`, `collectionStatus=UNAVAILABLE`, explicit null
score/count fields, empty raw evidence, and all three categories present as unavailable
zeroes. Their execution traces and selected read-only final application-state witnesses exactly
match the corresponding observer-enabled attempts.

## Quizzes finding and bounded follow-up

Normal `UpdateTournament` compensation leaves a real full-projection difference on the
Tournament. It restores the question count, start/end dates, topic IDs, topic names,
topic versions, and topic lifecycle states. It changes `lastModifiedTime` and loses each
restored topic's embedded `topicCourseAggregateId` (`1` becomes `null`). The source-level
explanation is `TournamentTopic.buildDto()`: it copies aggregate ID, version, name, and
state but does not copy course ID; compensation rebuilds the tournament topics from
those DTOs. The controlled no-op variant differs in five top-level fields:
`endTime`, `lastModifiedTime`, `numberOfQuestions`, `startTime`, and `tournamentTopics`.

Both variants therefore score one affected Tournament. This demonstrates a stated model
limit: the scalar identifies affected object identities and is not a harm oracle. Whether
domain-owned metadata should be distinguished requires a separate product decision.
The embedded course-reference loss should be tracked as a bounded Quizzes application
follow-up; M2 does not repair it.

## Reproducibility and provenance

- source revision: `36784346d1f5e988ad612132b6ed9f5db344ee78`
- shared fixture instant: `2026-09-06T01:54:07.000` UTC
- package manifest SHA-256: `9196a0a733181003dbf2e477eec6730b89f85466ca505569447e7af5b2f56df5`
- fixture selection SHA-256: `36754be85716baf97cef0904a3a38a9ef94d544ac2149986a69be5a07807f389`
- source-content manifest SHA-256: `00692a412dba7edb293817740b34e864af3f12e746f95cc1d74bf6233c7acc8c`
- artifact hash manifest SHA-256: `1a6ac0a8dd94406acfab4326ee26694ea5d9bb3a7f9678575d966862a182172a`
- validation report SHA-256: `41efdeb44d62e3d0fa8afe150d127a5ccfe42f09cc3f1414cff9c64873555c22`
- full run-03 orchestrator log SHA-256: `12547763a62d1bac4bda41bf803dcdbac69c070b006f28f69ef0a92a0d122aea`

The validator checks package-manifest hashes, source and temporary-copy content hashes,
variant diff boundaries, trusted-patch byte equality, fixture inputs across paired runs,
execution/assessment/event identities, report sizes and evidence counts, and every
artifact checksum. It passed inside the container and passed again from the host without
altering a hashed source tree.

## Descriptive cost observations

These are one serial observation per case and are not benchmark estimates. Enabled case
wall times ranged from 13.39 s to 21.63 s. The three enabled/disabled duration ratios
were 0.943 for deletion, 1.487 for the normal update residual, and 1.286 for the event
case. Enabled ImpactV2 reports were 54,408, 70,741, and 71,384 bytes for those pairs;
disabled reports were 1,591, 1,591, and 1,587 bytes. Enabled cases captured 11 baseline
and 11 final snapshots; the event family captured one selected delivery. All scored cases
recorded zero coverage gaps.

## Preserved diagnostic trials

- `run-01` stopped after builds/package generation and before any scenario because the
  stock image lacked `python3`. Its orchestrator log SHA-256 is
  `6e706969f55b37a5d77cd2cbe923ccc83092444467c430ec71240d178b192e1f`.
- `run-02` completed all 11 scenarios and exposed two generic evidence bugs: database
  timestamp precision made the latest committed write disagree with final H2 state, and
  event consumer evidence used the source scheduled-step ID instead of the persisted
  event action ID. Its final validator also identified generated `verifiers/logs/` as an
  invalid source-hash input. The production evidence bugs were fixed and focused-tested
  before `run-03`; the qualification manifest now excludes generated runtime logs. The
  run-02 orchestrator log SHA-256 is
  `b2e4bea710484c596a1155022744150b58f1b6ad38ce7b5c41f848ba88394090`.

No report data was edited in any trial.
