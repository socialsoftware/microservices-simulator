# First fixed-workload GA qualification

The GA and matched random policy completed the declared experiment: **53/53 COMPLETE
ImpactV2 assessments**, all with **COMPLETE_WITHIN_SCOPE** read-exposure coverage.
There were 48 search executions, two separate no-fault controls and three update/read
witnesses. Every execution used a distinct container and attempt ID. Conformance was
EXACT in 50 cases and supported DEVIATED in three; no unavailable fitness was hidden as zero.

## What was searched

The chosen workload contains `RemoveTournamentFunctionalitySagas` and
`AddParticipantFunctionalitySagas`. Its ordinary order is:

1. RemoveTournament obtains the Tournament and marks its Saga state.
2. RemoveTournament removes its Quiz.
3. RemoveTournament removes the Tournament.
4. AddParticipant obtains the user.
5. AddParticipant tries to register the user in the Tournament.

The existing `quizzes-stale-read-baseline@1` setup supplies the same course execution,
creator and Tournament for every attempt. The workload has no selected event deliveries.
The runtime is Saga/local, serialized messaging, fixed clock `2030-01-01T12:00`, the
qualified September 10 observation overlay, and immutable image
`sha256:0aab59d58bfe4f83e6bee2a4002a913dcbc26d861acee5f0327c053f12918282`.

Search varies the first fault per Saga and valid recovery placement. The 12 canonical
vectors resolve to 29 distinct action sequences. All 12 vectors were requested across
the four arms; none was truncated by the frozen recovery cap of 20. The 48 search
executions covered **21/29 distinct scenarios**. Each arm used its own package and cache.

The no-fault control removes the Tournament before the registration attempt. The
application rejects registration, giving PARTIAL_COMPENSATED/DEVIATED and COMPLETE I=0,
A=0. This is a supported domain rejection, not an invalid measurement or a positive score.

## Measured comparison

Population 8, mutation probability 0.3, budget 12 per arm, no tuning/retries:

| Strategy | Seed | I-positive / 12 | First positive attempt | Best I | Duplicate proposals | Wall seconds |
| --- | --- | --- | --- | --- | --- | --- |
| GA | 11 | 4 | 1 | 2 | 3 | 161.6 |
| Random | 11 | 4 | 1 | 2 | 16 | 161.7 |
| GA | 29 | 6 | 3 | 2 | 6 | 157.1 |
| Random | 29 | 2 | 5 | 2 | 0 | 161.5 |

All four arms reached their 12-execution budget; duplicates consumed no application
executions. Generation took 16.1–19.1 seconds per arm, included in the wall times above;
preparation and the no-fault controls are separate. A=0 throughout the benchmark search.

**The 6–2 row does not establish an evolutionary advantage.** In GA seed 29, all four
crossover proposals repeated cached candidates, so all four later executions came from
random fallback. Four of its six positives were already in its initial eight evaluations;
the matched random arm had one positive in its first eight. Both use the declared same
sampling distribution, but GA's tie-breaking/operator draws consume its seeded random
stream differently; equal seeds do not imply identical initial populations.

In GA seed 11, three new crossover children were executed and two were positive; one
later execution used random fallback. This qualifies actual adaptive selection and
execution, while the fake-evaluator tests independently prove that changing fitness
changes GA choices and leaves the random policy's proposal sequence unchanged.

## A concrete I=2 result

`ga-11/attempt-001` assigns vector `00110`: the Tournament-removal step fails after the
Quiz was deleted, and the user-read step of AddParticipant also receives a fault. During
recovery, the Quiz remains deleted and the Tournament remains active. I counts:

- the Tournament, because it still depends on that deleted Quiz;
- the Quiz, because its deletion survives the completed recovery of a failed operation.

This is two affected objects in one scenario. Several different fault/recovery schedules
expose the same partial-removal problem; positive-scenario counts are not defect counts.

## Independent A integration

The second workload is `UpdateTournamentFunctionalitySagas` with
`FindTournamentFunctionalitySagas`, using the latest source-derived setup. Its no-fault
control succeeds with EXACT conformance and I=0/A=0. The three final-fault recovery
placements reproduce:

| Witness | I | A | What differs |
| --- | --- | --- | --- |
| Read before recovery | 1 | 1 | The reader receives the changed Tournament values later restored by compensation. |
| First alternative recovery placement | 1 | 0 | The read does not expose that compensated update. |
| Second alternative recovery placement | 1 | 0 | The read does not expose that compensated update. |

I stays 1 because the existing Quizzes recovery loses the embedded topics' course ID.
The search adapter reproduces both the stable I and differing A; it changes neither detector.

## Proof and reproduction

The [compact audit](../../../docs/verifiers-impl/evidence/fixed-workload-ga-2026-09-10/summary.json)
checks report joins/hashes, exact replay package snapshots, original input packages,
runtime files, request revision chains, distinct attempt/container IDs, parent fitness
availability and stable I/A on repeated candidate keys. Raw evidence:
`verifiers/target/fixed-workload-ga/qualification-01/`.

The separate `generation-smoke-01` requested update/read vector `010001`, persisted two
new candidates, then obtained the same candidates with zero additions on a second Java
request. It launched no application executions. Original package hashes stayed unchanged.

Tests: 17 search/evaluator tests, 13 existing batch tests and 13 existing baseline tests
passed. The documentation build and `git diff --check` passed. No production Java or
Quizzes changes were needed. See [README.md](README.md) for preparation/run/replay commands.

```bash
python3 verifiers/experiments/fixed-workload-ga/summarize_qualification.py \
  --run verifiers/target/fixed-workload-ga/qualification-01 \
  --output docs/verifiers-impl/evidence/fixed-workload-ga-2026-09-10/summary.json
```

The next evaluation should provide enough post-initialization budget to examine evolution,
a separately selected broader workload cohort, and the I/A priority rule agreed with the
advisor. This small known benchmark closes implementation qualification; it is not a
claim that GA generally outperforms random search.
