# Diagnosis of unavailable scores in the 500 × 3 campaign

The diagnosis below led to the user-approved exclusive-field extension, now implemented
and qualified in the final section. Neither diagnosis nor offline reassessment is a new GA run.
The campaign completed 3,000 attempts in 16.49 hours. GA found 829 positive attempts
across its three seeds, versus 528 for random, under the frozen five-unit-weight policy.
There were 565 unavailable scores (18.83%). Attempts repeated across arms are included
in these totals; they are not distinct bugs or globally distinct scenarios.

## Evidence and reproduction

Run `python3 docs/verifiers-impl/evidence/ga-coverage-diagnosis-2026-09-16/diagnose.py`
from the repository root. It reads the retained campaign at
`verifiers/target/ga-500x3-2026-09-15/`, writes the small adjacent `summary.json`, and
writes per-attempt details to that campaign's `analysis/coverage-diagnosis.json`.
It does not execute application operations, alter reports, or assign replacement scores.
The original campaign analysis, integrity audit and plots remain in `analysis/`.

The script compares each exact committed snapshot with the preceding snapshot, starting
at the preparation baseline. Equality uses application data and lifecycle, as the current
assessor does. Framework metadata is excluded from equality but used to check predecessor
identity/version continuity. A top-level collection is compared as one field; this is
not element-level attribution or an inference about fields read by the application.

## 489 partial residual assessments

These are 333 distinct candidate keys across arms. Every affected object's writes have
valid Saga/attempt/workload/phase attribution, a continuous observed predecessor chain,
no persistent-evidence coverage gaps, and a final state matching the last tracked write.
Each has exactly one failed writer. All 489 exhibit disjoint top-level *changed* fields
between that failed Saga and the other writers. This does not establish independence of
their computations or business rules.

| Attempts | Failed Saga / object | Changed by failed Saga, including recovery | Changed by other Sagas |
| --- | --- | --- | --- |
| 322 | UpdateTournament / Tournament | Number of questions, topics | Participants |
| 64 | UpdateTournament / Tournament | Number of questions, topics | Lifecycle/state; sometimes participants |
| 103 | RemoveTournament / Quiz | Lifecycle/state | Questions |

The current residual rule rejects every case because it requires a single writer for
the entire aggregate. This is a deliberate scope limit, not missing writer instrumentation.
The 64 cases with a lifecycle change by another Saga deserve separate consideration:
an independently deleted object should not automatically be treated as an ordinary
surviving object with disjoint attribute updates.

**A second blocker overlaps this group:** 42 attempts also have an incomplete compensated-read
assessment, all with `INTERVENING_WRITER`. Four belong to the lifecycle group of 64;
38 belong to the other 425. A residual-only improvement cannot supply complete combined
fitness for these 42. There are no other unavailable criteria in the partial group.

### Example: participant operations do not explain missing topic data

`ga-seed-11/attempt-018`:

1. AddParticipant succeeds, changing the Tournament participant list.
2. UpdateTournament saves new question settings, then hits an assigned fault at
   `findQuestionsByTopicIds`.
3. LeaveTournament retrieves the updated Tournament.
4. UpdateTournament compensates. The question count returns to two, but the two
   original topics now have `topicCourseAggregateId = null`, instead of `1`.
5. LeaveTournament completes, changing the participant list. RemoveTournament then
   faults at its first step without deleting anything.

Versions 23 → 24 → 25 → 26 → 27 identify the baseline, participant addition, update,
recovery and participant removal. The other Sagas never change the `tournamentTopics`
field in this observed chain. Its final difference is attributable to UpdateTournament's
observed writes, even though the aggregate has multiple writers. The current score remains
null; the compensated-read component already has one finding and complete coverage.

### Example: successful question update does not explain a deleted Quiz

`ga-seed-11/attempt-028`: UpdateTournament succeeds, changing Quiz questions.
RemoveTournament later deletes that Quiz, but its Tournament deletion fails.
Its scheduled recovery finishes without restoring the Quiz. The final Tournament is
ACTIVE and the Quiz DELETED. On the Quiz, the successful update changed `quizQuestions`;
the failed removal changed lifecycle/state. The existing deleted-dependency check can
already report the Tournament; the residual check remains unknown because of the earlier
successful Quiz writer. This is observed residual deletion, not proof of a new bug family.

## 76 compensation failures

These are 53 distinct keys. In all 76, RemoveTournament is COMMITTED and the final
Tournament is DELETED. A later recovery fails with `Aggregate with aggregate id 12
does not exist.`:

| Attempts | Failing recovery step | Recovery path |
| --- | --- | --- |
| 43 | LeaveTournament.getOldTournamentStep | Explicit semantic-lock release |
| 30 | UpdateTournament.getOriginalTournamentStep | Implicit Saga rollback |
| 3 | UpdateTournament.updateTournamentStep | Explicit update compensation |

For example, `ga-seed-11/attempt-078` reads the Tournament for LeaveTournament,
faults the leave action, lets RemoveTournament delete the Quiz and Tournament, then
attempts the leave read's lock-release compensation. The Saga command needs to load
the Tournament; the normal loader uses `findNonDeletedSagaAggregate`, so it rejects it.

This is a failed application/framework recovery interaction, not missing test setup or
the previous pending-undo-history executor defect. The executor correctly retains the
failure. Whether releasing a lock on a deleted object should be a no-op is a separate
runtime contract decision; changing it would change the experiment. Updating a deleted
Tournament during compensation is also different from merely releasing its lock.
Keep these attempts unavailable under the current completed-recovery scoring contract.

## Approved bounded extension

Extend only failed-operation residual assessment to **exclusive observed field changes**:

1. Keep the current single-writer path and recovered-creation policy.
2. For an existing aggregate with one failed Saga whose recovery completed, require
   exact writer attribution, continuous observed versions, complete snapshots and
   the final state explained by the last write.
3. Compare top-level persistent fields and lifecycle between adjacent snapshots.
   Treat collections as whole values. Do not supply Quizzes-specific field mappings.
4. Require disjoint changed fields between the failed Saga (forward plus recovery)
   and every other writer. Keep overlaps, missing evidence and multiple failed writers
   unknown. For this first extension, also keep lifecycle changes by another Saga unknown.
5. A remaining final difference in a field changed exclusively by the failed Saga is
   a residual finding for that aggregate. Restoration of its exclusive fields is a
   negative result for this bounded pattern. Differences exclusively changed by successful
   Sagas do not earn residual points. This identifies observed final effects, not arbitrary
   data dependencies or the intended business outcome.
6. Retain field names and relevant versions in diagnostic evidence. Keep one residual
   point per aggregate, rather than counting fields. Version/document the assessment rule
   so old and new experiments can be distinguished.

The user approved this extension of FR-6 on 16 September after reviewing the diagnosis.
The original potential-impact specification and plan now record the amendment. This
observed-change rule is narrower than general causal reconstruction.

**Projection made before implementation:** 425 partial attempts fit the initial shape;
38 still have the independent read blocker. If implementation and regression controls
validate the proposal, 387 attempts could gain a complete score. Unavailable attempts
would then fall from 565 to 178 (18.83% → 5.93%). Do not report this as a measured
improvement until the actual Java assessor has reassessed the retained evidence.
Even accepting all 489 residual cases would leave 118 unavailable scores, because of
the 42 read gaps and 76 failed recoveries.

Validation should cover generic positive and restored controls, legitimate successful
writer differences, same-field interference, collection changes, lifecycle interference,
unknown writers, broken version chains and unfinished recovery. Use dummyapp/Spock
for the generic contract, then the real retained reports. Preserve complete existing
results and inspect any deviation; write new assessments separately from frozen originals.

Do not change the read detector, recovery semantics, GA operators or weights in this
change. Offline reassessment can measure coverage improvement but cannot reproduce the
GA trajectory under different feedback. Freeze the revised rule before the smaller
exhaustive benchmark; retain this campaign as the result of its original policy.

## Implementation and measured qualification

The production Java assessor now implements `exclusive-observed-fields-v2`. The existing
single-writer path and recovered-creation exclusion are preserved. Other writers in the
extension must be known committed Sagas writing in FORWARD; event writers and incomplete
participant outcomes remain unknown. Collections are compared as whole values, and
missing keys differ from present null values. No application or collector changes were needed.

The v1 JSON schema gains an additive `residualAssessmentPolicy` field and optional
`Finding.affectedFields`, using JSON pointers such as `/applicationData/tournamentTopics`
or `/lifecycleState`. Old reports without the policy field deserialize as
`whole-object-single-writer-v1`. New assessments retain action IDs and versions alongside
field paths. The existing offline reassessment utility now labels outputs with the actual
compiled policy rather than its historical hardcoded creation-remnant label.

A negative attribution control exposed an existing null-Saga-ID lookup in a `TreeSet`:
an unknown writer could throw instead of yielding an unknown assessment. The candidate
writer filtering now handles that case, with a regression control. This is the only
adjacent correction; neither recovery behavior nor other detectors changed.

The actual Java assessor reprocessed all 3,000 report pairs. The existing Python fitness
function then combined the new persistent assessment with the original read/lost-copy
assessments and the original five unit weights. Results in `reassessment-summary.json`:

| Measurement | Result |
| --- | --- |
| Newly complete residual assessments | 425 |
| Newly available combined scores | 387 (all positive) |
| Unavailable combined scores | 565 → 178, or 18.83% → 5.93% |
| Previously available scores changed | 0 of 2,435 |
| Other persistent categories changed | 0 |
| Original report hashes verified before and after | 18,000 |

Of the 178 remaining unavailable attempts, 64 have another writer changing lifecycle,
38 additional cases have incomplete read attribution, and 76 have failed recovery.
Four read gaps overlap the lifecycle group. New positive counts on the *retained selected
candidates* are GA 1,007 and random 737, summed across seeds. These are not new search
results: the GA's choices were made with the original feedback, so do not use this as
a comparison of the revised GA policy against random.

Validation: 47 assessor Spock cases, 217 executor Spock cases and 49 search/fitness
Python tests pass. Controls cover residual and restored exclusive fields, legitimate
successful changes, collections, lifecycle interference, unknown/event/wrong-attempt writers,
missing/incorrect predecessor evidence, duplicate sequences, missing baseline, final-version
mismatch, snapshot gaps, unfinished recovery, multiple failed writers, report serialization,
stable order, null versus absence and one point for multiple residual fields.
The checks are pure-assessment/executor tests plus offline evidence replay; no fresh Quizzes
application or Docker campaign was run for this change.

Reproduce from the current checkout (JDK 21; no `clean`, because retained evidence lives
under `target`):

```sh
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
cd verifiers
./mvnw -q -Dtest=ImpactV2AssessorSpec,ScenarioExecutorSpec test
mkdir -p target/exclusive-field-residuals-2026-09-16/classes
./mvnw -q dependency:build-classpath -Dmdep.outputFile=target/exclusive-field-residuals-2026-09-16/classpath.txt
cd ..
python3 docs/verifiers-impl/evidence/ga-coverage-diagnosis-2026-09-16/reassess.py
python3 -m unittest discover -s verifiers/experiments/fixed-workload-ga -p 'test_*.py'
```

The Java utility refuses to overwrite an existing reassessment directory. The retained
run is at `verifiers/target/exclusive-field-residuals-2026-09-16/`: manifest, dependency
classpath, derived sidecars, before/after categories and hashes, fitness comparison,
and test logs. For a repeat, prepare `classes/` and `classpath.txt` in a new directory
and pass that directory as the optional argument to `reassess.py`. No source change
was committed automatically, and the original campaign outputs were not rewritten.
