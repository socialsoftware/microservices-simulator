# Tournament update read before compensation

**Result:** the real `FindTournament` operation successfully returned the revision written
by an unfinished `UpdateTournament` Saga. The writer then failed and explicitly restored
selected fields. The reader had already committed and retained its original DTO. This
reproduces a Saga-level dirty-read witness for an update, not only for a creation.

Four controlled histories passed in both local messaging modes: serialization disabled
and enabled. The final qualification comprises **eight fresh JVM/Spring/H2 executions**,
not eight different histories or statistical repetitions. The complete run took 100.95 s,
including harness compilation and application startup. No production source was changed.
The existing impact scores and generic creation-only read assessor were not evaluated or
modified by this experiment. Experimental zeros below must not be read as impact scores.

Implementation follow-up: the [production diagnostic extension](../../../../issues/2026-09-09-compensated-update-read/HANDOFF.md)
is now implemented and qualified. The results below remain the original research-only
experiment; its proposed detector work has been completed, while generated positive
workload qualification remains pending.

## Domain story and exact sequence

Setup creates a course execution, an active enrolled student, three topics with one
Question each, and a Tournament through normal application operations. Tournament 11
starts at 15:00, ends at 15:55, asks two questions and selects topics 4 and 5. Its Quiz is
aggregate 10. Dates are on 1 January 2030; the course ends the following day.

A executes `UpdateTournament` with start 16:00, end 17:00, three questions and topics
4, 5 and 6. B executes `FindTournament` on the same setup-created Tournament ID.
The positive run's actual action order is:

1. A `getOriginalTournamentStep`: retain the original DTO; place `IN_UPDATE_TOURNAMENT`.
2. A `getTopicsStep`: obtain the requested topics.
3. A `updateTournamentStep`: persist Tournament revision **21**, predecessor **20**.
4. B `findTournamentStep`: the real gateway returns Tournament **11/v21**, including
   16:00, 17:00, three questions and the three topic IDs. B finalizes successfully.
5. A `findQuestionsByTopicIds`: obtain questions for the requested topics.
6. Inject the assigned fault **before the body of `updateQuizStep`**. The Quiz is unchanged.
7. Recover `updateTournamentStep`: explicit compensation persists Tournament **11/v22**,
   predecessor **21**, restoring 15:00, 15:55, two questions and topic IDs 4 and 5.
8. Recover `getOriginalTournamentStep`: implicit rollback releases the semantic lock.

At the horizon B's retained DTO still identifies **v21 and 16:00**; the latest persistent
Tournament is **v22 and 15:00**. B made no persistent writes. The observation proves
successful delivery and retention; B has no later business step whose use of those data
we could claim to have demonstrated.

The lock is visibly present when B reads. B uses a plain `GetTournamentByIdCommand`;
it does not declare the forbidden-state check used by A's initial wrapped command.
The explicit compensation restores selected data while the lock is still present;
the following implicit rollback releases it without generating another data revision.

## Controls and results

| History | B actually received | A finishes as | Final Tournament | Experimental compensated-update-read witnesses |
| --- | --- | --- | --- | --- |
| `success-between` | v21, 16:00, before A finishes | Committed | v21, 16:00; Quiz updated | 0 |
| `fault-between` | v21, 16:00, before A fails | Compensated | v22, 15:00; Quiz unchanged | **1** |
| `fault-before` | v20, 15:00, before A starts | Compensated | v22, 15:00; Quiz unchanged | 0 |
| `fault-after` | v22, 15:00, after A recovers | Compensated | v22, 15:00; Quiz unchanged | 0 |

Each row has the same result with JSON serialization enabled. The validator checks the
exact schedules, terminal outcomes, gateway-to-DTO version match, writer/read/recovery
ordering, direct predecessor links, fault identity, recovery checkpoint modes, selected
restoration, unchanged faulted Quiz, released final locks, no reader writes and no observer
failures. The unmapped Topic/list/update-command response paths remain visible; they do
not invalidate the supported outer Tournament read or establish wider read coverage.

## What did not return to the original state

Full persistent projections show that compensation restores the selected dates, count
and topic membership, but does **not** restore the entire Tournament application projection:

- Its `lastModifiedTime` changes.
- Each retained embedded topic's `topicCourseAggregateId` changes from **1 to null**.
  `TournamentTopic.buildDto()` omits the course ID, and recovery reconstructs topics using
  those DTOs. This pre-existing recovery difference occurs in all three fault histories,
  regardless of B's position. It is not caused by the dirty read.

This is why the next detector should identify **which fields were restored**, instead
of requiring whole-object equality. It also explains why “no dirty-read witness” does
not imply “no other final-state difference.” No domain-harm judgment or numerical impact
claim is made for these residuals here. The topic-copy issue is a separate bounded repair
candidate, not a prerequisite for proving the observed read.

The embedded Quiz DTO still has a null version; only the outer Tournament revision is
covered. The experiment makes no claims about nested Quiz reads.

## Detector extension recommended here (now implemented)

Extend the existing diagnostic with an update-restoration category:

1. Join the exact delivered revision to the producer's forward update and its predecessor.
2. Join a later explicitly successful compensation of that same step occurrence.
3. Reuse persistent application projections to report proven restored paths and remaining
   differences. Metadata-only revision changes are insufficient. Missing/ambiguous joins
   must remain unknown; other-writer interference needs explicit handling.
4. Count one producer/reader/revision witness, with the reader's outcome and exact evidence;
   do not award separate points for every restored field.
5. Validate dummyapp positive/negative cases, retain existing creation regressions, then
   replay this four-history matrix through the integrated diagnostic. Ordinary generated
   workload binding/qualification is still a separate step to demonstrate.

The generic claim should be “received a revision containing changes subsequently
restored by compensation” unless a declared response projection proves particular fields
were returned. The Quizzes DTO in this experiment explicitly supplies that stronger
field-level evidence. No business-use inference or guessed DTO field mapping is required.

This extends coverage to existing-object updates using current hooks. It does not yet
cover lost updates, non-repeatable reads, write skew, predicate reads or arbitrary services.
Keep the anomaly count inspectable alongside persistent-object impact; search weighting
is a separate decision.

## Reproduction, provenance and review

- [Harness and protocol](../../../../verifiers/experiments/saga-update-read/README.md)
- [Validated matrix](summary.json)
- [Selected raw witnesses](witnesses.json): exact fields copied from the raw reports,
  with original report hashes; includes persisted before/after data, gateway reads,
  producer/recovery writes, actual actions and checks.
- [Provenance and artifact hashes](proof.json)
- [Validation checks](validation-checks.json) and [retained evidence hashes](evidence-hashes.json)

Raw JSON reports and logs are retained locally under
`verifiers/target/saga-update-read/run-03/`; the repository retains the selected evidence
above. The runner checked 791 production-file hashes and 1,777 prepared build/dependency
hashes against the retained qualified build before and after execution. Docker used the
recorded image, read-only prepared inputs and no network. Only the experiment was compiled.

`run-01` failed harness compilation because a helper was missing; no application case ran.
`run-02` completed all eight cases, then the harness was extended to retain full baseline
and final projections for the recovery-difference audit. `run-03` is the reported final
matrix. The validator initially assumed `UnitOfWork.id` would distinguish actors; that
field is unset in Saga/local. It instead checks the actual controlled A/B writer/reader
attribution and distinct functionality classes; the harness creates their separate UoWs
through the application framework. The unset raw IDs remain recorded, not fabricated.

Application time itself was not frozen. Both transport modes use identical future input
dates; last-modified timestamps differ naturally. This is a deterministic selected
schedule experiment, not a throughput or statistical anomaly-frequency measurement.

A Sol/medium read-only review confirmed the execution proof and identified one reporting
ambiguity: a summary field named `scoreChanged` could imply that scores were compared.
It was renamed `productionScoringModified`; `scoreEvaluated: false` remains explicit.
The final validator passed again without rerunning unchanged application executions.
Five deliberately corrupted report controls were rejected (wrong read version, unrestored
date, failed reader, missing observer evidence and wrong compensation mode).
