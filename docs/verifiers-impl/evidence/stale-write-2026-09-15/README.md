# Stale writes in normal execution and compensation

## Research question and terminology

Can an ordinary Quizzes workflow overwrite a change already applied by another workflow
because it later writes previously obtained data? Does this occur only during recovery?

The classical literature calls the read–intervening write–stale write pattern **P4,
lost update**: [Berenson et al., 1995, section 4.1](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/tr-95-51.pdf).
Our application experiment spans locally committed Saga steps and event consumers.
Compensation is itself another committed action, so its history is not mechanically
identical to the database-transaction P4 history ending in the original transaction's
commit. We describe the observed cases as stale writes/lost application updates,
distinguishing FORWARD and RECOVERY writers. “Compensation overwrite” describes a cause;
it is not presented as a separate standardized anomaly name.
[Korth, Levy and Silberschatz, 1990](https://www.vldb.org/conf/1990/P095.PDF)
provides the relevant recovery context: compensating an externalized transaction must
account for activity occurring after its original execution.

## Application story

A Tournament has an associated Quiz and keeps local copies of its Topics, including
each Topic's name. Updating the Tournament reads Topics, stores their DTOs, then writes
a new set of TournamentTopic objects. Its compensation instead uses the original
Tournament DTO saved at the first step.

A second user renames an existing Topic from `TOPIC 1` to `RENAMED TOPIC`.
The Topic service emits UpdateTopicEvent. The Tournament consumer handles that event by
updating the Tournament's local copy of the name. There are two user-level operations
but three Saga executions: Tournament update (A), Topic rename (B), and the event-triggered
Tournament-topic update (C). The harness selects the exact real event and Tournament
handler. Other event consumers are outside this experiment's selected horizon.

The Tournament updater's first command forbids another IN_UPDATE_TOURNAMENT or
IN_DELETE_TOURNAMENT state and places its update marker. The Topic event consumer uses
an ordinary UpdateTopicCommand without that forbidden-state check. The experiment confirms
that event delivery succeeds while the Tournament is IN_UPDATE_TOURNAMENT. Two ordinary
Tournament updates were therefore not assumed to form an executable positive case.

## Five histories, repeated with serialization off and on

| Case | Relevant order | Final Tournament copy | Outcome |
| --- | --- | --- | --- |
| forward-stale | A reads Topics → B renames → C applies event → A writes cached Topics and succeeds | TOPIC 1 | Normal write loses the applied rename |
| forward-fresh | A saves its original Tournament → B renames → C applies event → A reads current Topics and succeeds | RENAMED TOPIC | Fresh-input control preserves the name |
| recovery-stale | A updates Tournament → B renames → C applies event → fault before A updates Quiz → A recovers from its original DTO | TOPIC 1 | Recovery loses the applied rename |
| recovery-delayed-event | A updates Tournament → B renames → fault before Quiz update → A recovers → C applies event | RENAMED TOPIC | Delivery after recovery preserves the name |
| recovery-no-event | A updates Tournament → fault before Quiz update → A recovers; no rename | TOPIC 1 | Recovery alone does not demonstrate an overwritten foreign update |

All ten runs completed their declared history. Four executions exhibit the stale write
(two patterns × two serialization modes); six are controls. In all rename cases the
original Topic finishes as RENAMED TOPIC. All Tournaments remain ACTIVE and release their
Saga marker. The forward-positive case has **no injected fault or compensation**.

For example, in the nonserialized forward-positive run, the event writes Tournament
revision 23 with RENAMED TOPIC; A writes revision 24 with TOPIC 1. In the recovery-positive
run the corresponding revisions are 24 and 25. The retained committed-write observations
identify C as EVENT_CONSUMER and A as FORWARD or RECOVERY. No aggregate is deleted.
These are examples from one run, not identifiers hardcoded into a detector.

The controls isolate the copied-name effect. They are not claims that every property is
correct: the already known compensation defect still loses embedded course identifiers.
Also, forward-fresh preserves the name while replacing event progress stamp 22 with Topic
DTO version 21. Source revision and event publication stamp are not interchangeable;
a falling version number alone is insufficient to establish that a name update was lost.

## What is proved and what remains engineering work

The experiment uses real application setup, Saga steps, the exact pre-body fault boundary,
explicit recovery checkpoints and the real selected event handler. It records committed
versions, writer/action/phase, event identity, cached DTO values and final state.
Independent validation checks the original applied value, later overwriting writer,
retained old input, source Topic and controls. There were no observer callback failures
or reported persistent-collection gaps.

This is a controlled application experiment, **not a newly generated workload qualification,
a production anomaly detector or an added scoring component**. I/A were not evaluated
for these histories. The application-specific checks establish this witness; they must
not become a rule hardcoded as “Tournament topic name must equal Topic name”.

The next detector needs to distinguish a stale-input overwrite from a deliberate later
update. Current framework evidence provides writer/version/recovery attribution and exact
outer Tournament reads. The harness additionally records cached DTO payloads; ordinary
read observations do not record generic read-to-write input provenance. GetTopicById
responses are currently DELIVERED_UNMAPPED/NO_READ_ADAPTER. The forward case also crosses
from a Topic DTO to an embedded TournamentTopic. The recovery case involves nested Topics
in an otherwise supported Tournament DTO.

Consequently, neither “two writes to the same aggregate” nor “final value differs” proves
this anomaly generically. The next brief should define the smallest explicit evidence
contract for the values used by writes, stable matching inside supported collections,
and intentional/unrelated-field controls. Keep normal and recovery writes in scope; do
not silently implement only a deletion or compensation special case. If the initial
contract only proves a reverted foreign write, report that narrower observation rather
than claiming all classical lost updates are detected.

A source scan of 26 Saga classes with explicit compensation registrations found the
Tournament update as the direct saved-DTO replacement candidate; most other compensations
release state or remove newly created objects. UpdateQuestionTopics also retains Topic
copies between steps and is a plausible follow-up forward-write family, not a reproduced
case. UpdateQuiz builds question copies from caller inputs, whose staleness may predate
the measured execution; merely seeing a read step there would not prove its input origin.

## Reproduction and provenance

The [experiment sources](../../../../verifiers/experiments/stale-write/README.md) own the
harness and commands. Raw logs, Java source snapshot, prepared runtime hashes, exact
Docker commands, before/after states, observation records and report hashes are retained
under `verifiers/target/stale-write/run-01/`. Each case uses a fresh JVM/database; both
local transport serialization modes use the same fixed future fixture dates.
[validation.json](validation.json) indexes all ten reports with their hashes, observed
writer sequences and the field values that establish each result.

No Quizzes, simulator, generator, executor or scoring production code was modified.
