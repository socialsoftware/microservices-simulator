# Saga anomalies: theory, evidence and next experiments

Research for the 11 September advisor discussion. This exploratory synthesis links both
qualified experiments and implemented slices; it is not the governing specification or
a change to ImpactV2 persistent-object scoring.
[Current state](../current-state.md) owns shipped behavior and
[roadmap](../roadmap.md) owns implementation priorities.

## The central distinction

There are two transaction boundaries: a local database transaction and the entire
Saga. A local write can commit and become visible while its Saga is still running.
Compensation is another action that amends an earlier effect; it is not automatically
the same as aborting an uncommitted database transaction. This is the setting introduced
by Garcia-Molina and Salem's [Sagas](https://www.cs.princeton.edu/research/techreps/598).

The paper must distinguish three questions:

1. What interaction was observed between operations?
2. What persistent conditions remain at the declared observation horizon?
3. What isolation or application requirement would make that observation unacceptable?

Our tool can report the first two without supplying a universal business-harm oracle.
It must still define its checks: application independence does not mean absence of
an observation model. An accepted Saga-level exposure is not necessarily a database
isolation violation, and an exception can be a successful safeguard.

An explicit reader outcome matters. For example, Adya's aborted-read definition
requires a committed reader, while our current exposure diagnostic can retain a
finding even when the reader later recovers. Therefore the existing finding must not
be relabelled mechanically as classical G1a. The same care applies to a Saga's multiple
locally committed updates versus an atomic transaction's intermediate versions.
[Adya, Liskov and O'Neil, 2000](https://15721.courses.cs.cmu.edu/spring2019/papers/02-transactions/adya-icde2000.pdf).

## Pattern map

The stories below are explanatory examples, not claims that each has been reproduced
in the current Quizzes checkout. Classical terminology follows
[Berenson et al., 1995](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/tr-95-51.pdf)
and Adya et al.; the Saga adaptations and engineering choices are ours.

| Pattern | Explanatory story | Evidence needed here | Present status |
| --- | --- | --- | --- |
| Read of a subsequently compensated creation | A creates a Quiz; B receives its revision; A compensates by deleting it | Exact returned revision, creating Saga/step, explicit compensation and ordered writes | Implemented for declared outer responses; controlled Quizzes positives exist |
| Read of a subsequently compensated update | A changes an existing Tournament's date; B receives the changed date; A later restores it | Exact read-to-update attribution, pre-update and recovery values, recovery linkage; distinguish restored from merely changed-again | Implemented bounded top-level attribute comparison; four controlled histories qualified through the production diagnostic in both local transport modes |
| Intermediate read | A changes the same object twice; B receives the first version | Two same-producer writes with a read between them, reader outcome and defined Saga boundary | Raw supported read/write facts exist; no general classifier |
| Non-repeatable read | One operation reads the same Quiz twice and observes an intervening update by another operation | Same reader Saga and object, two returned versions, external writer, actual observable difference | No classifier; requires a suitable repeated-read workload |
| Lost update / compensation overwrite | A saves old data; B persists a change; A's stale replacement or recovery erases B's contribution | The read/base used by A and field-level or operation-level overwrite evidence; two writes alone are insufficient | Normal/recovery Quizzes witnesses reproduced; generic detection and input provenance remain open |
| Read skew / fractured observation | One operation receives new Tournament settings but old associated Quiz settings | Cross-object version dependencies and an explicit consistency boundary | Not equivalent to merely comparing two version numbers; no generic checker |
| Write skew | Two operations both rely on the old state of two objects, then write different objects whose combination violates the intended condition | Actual reads and writes; a defensible dependency cycle or declared joint constraint | No Quizzes defect reproduced; broader instrumentation required |
| Phantom | An operation repeats the same search for Questions by topic and gets a changed membership due to another operation | Query predicate/parameters and returned membership, plus causal changes | Lists and predicates are outside current read coverage |

Except for the already implemented pattern, all table classifications are candidate
Saga-level observations. Their resemblance to a classical anomaly does not establish
equivalence to its database-transaction definition.

Standard dirty writes concern overlapping uncommitted database writes. The controlled
Saga/local executor runs whole scheduled actions sequentially, so the useful immediate
question is cross-step stale replacement or compensation overwrite. Calling those a
database dirty write without defining a Saga-level adaptation would be misleading.

## Stale-write investigation after configurable fitness

The [15 September experiment](../evidence/stale-write-2026-09-15/README.md) now reproduces
lost Topic-name updates in both a successful normal UpdateTournament step and its
compensation. The intervening writer is the real Tournament consumer of UpdateTopicEvent.
Ten controlled runs cover both local serialization modes and three control histories.
This advances the lost-update row from a candidate to an application witness; no generic
detector or additional score component has been implemented. The source scan and report
explain why read-to-write input provenance and nested copied values remain explicit gaps.

## What the simulator already gives us

Static footprints identify potential interactions and guide generation. They do not
prove a runtime anomaly. `READ_TOURNAMENT` and similar names also do not prove that
a read was isolated: the selected command's wrapper and checks determine behavior.

The measured runtime has:

- Baseline and final aggregate projections, plus committed version observations.
- Writer identity, Saga/action/phase attribution, and explicit recovery checkpoints.
- Exact successful return observations for supported synchronous command responses.
- Selected event identity, actual deliveries, receiver state and eligibility.

Relevant code paths (under `src/main/java/pt/ulisboa/tecnico/socialsoftware/` in
the corresponding module):

- `LocalCommandGateway.send()` and `ReadResponseAdapter`: observe the actual final
  response, including the JSON round-trip path. Repository lookup alone does not prove
  which response a different Saga received.
- `SagaReadExposureCollector`, `SagaReadExposureAssessor`, `SagaReadExposureReport`:
  join metadata and assess the current compensated-creation pattern. The report excludes
  payloads and explicitly lists unsupported paths.
- `ImpactV2EvidenceCollector`, `PersistentStateObserver`, `ImpactV2Assessor`: persistent
  projections and the existing three potential-effect conditions.
- `SagaCommandHandler.handle()`: checks declared forbidden states before dispatch and
  registers a declared semantic lock afterwards. Plain commands do not acquire those
  checks automatically.
- `QuizzesSagaReadResponseAdapters`: two declared mappings, for outer Quiz and Tournament
  responses. Nested objects, lists, predicates, internal service reads and event consumers
  are not covered as application readers.

The [22-run qualification](../../../issues/2026-09-07-compensated-saga-read/RESULTS.md)
already has positive and negative controls, a read-only reader, and observation-on/off
comparisons. Its positive harness passes the newly created ID to the reader itself.
Generated binding of a participant to another participant's runtime result is a separate
gap; the ordinary-executor qualification is currently a baseline-object negative control.

The audit did not identify an existing Saga with two outer `GetQuizById` or
`GetTournamentById` sends. That makes non-repeatable-read qualification less immediate
than it sounds. An internal reload during an update is not a second observed gateway
return; adding a repeated-read fixture would demonstrate a mechanism, not coverage of
an existing ordinary workflow.

## Qualified Quizzes experiment and next detector extension

The [9 September experiment](../evidence/saga-update-read-2026-09-09/README.md)
qualified `UpdateTournament` with `FindTournament` on the same existing Tournament.
The executed positive history is:

1. Prepare the Tournament and its Quiz normally; choose valid future dates.
2. A acquires its update state and changes the Tournament settings.
3. B runs the existing find operation and receives the updated Tournament DTO.
4. Inject a fault at A's later Quiz-update step.
5. A runs its explicit Tournament compensation using the original DTO.
6. Inspect what B received and which fields A restored, left changed, or failed to restore.

Why this is promising: both inputs can refer to an object prepared before measurement,
and the returned Tournament DTO already has an adapter. It avoids first solving
creation-result binding. The first read in `UpdateTournamentFunctionalitySagas` is
wrapped with `IN_UPDATE_TOURNAMENT`; `FindTournamentFunctionalitySagas` sends a plain
`GetTournamentByIdCommand`. A lock marker is therefore not enough to conclude that
the plain reader will be rejected. The controlled run confirmed that the plain reader succeeds while the marker is present.

Controls qualified A completing successfully, B reading before the update and B
reading after recovery, each with serialization disabled and enabled. Only the
read-between-update-and-compensation history has a compensated-update-read witness.
A lock-aware conflicting-operation control remains a possible further experiment. A failed read is not an exposure. A later revision without a relevant
data change is not evidence that a value was undone.

The measured Tournament recovery changes embedded topic course IDs from 1 to null and
changes the last-modified timestamp, while restoring dates, question count and topic membership.
Do not assume exact whole-object restoration. A checker must distinguish a proven
restored field from incomplete recovery, metadata-only differences and ambiguous writers.
Avoid teaching it that a particular Tournament date is correct: Quizzes values explain
the witness, while the checking contract concerns observed versions and recovery effects.

Lost-update detection is a separate extension. In particular, `updateTournament()`
loads the current Tournament and replaces selected settings, retaining other data;
we cannot claim its compensation erases a participant merely because it uses an older DTO.

## How this relates to impact and search

Three dimensions should remain inspectable even if the user later selects a combined
search objective: affected persistent objects, observed Saga interactions, and operation
outcomes. Anomaly evidence can be useful when the final object count is zero. Conversely,
the failed-removal counter example has a persistent effect without a second Saga.

The diagnostic now reports creation/update category, identities, producer/reader actions,
exact revision observations, restored/not-restored changed attributes, reader outcome and
coverage. [Implementation and qualification](../../../issues/2026-09-09-compensated-update-read/HANDOFF.md). Keep unknown evidence distinct from a negative result.
Whether an exposure contributes one point, counts a reader/object, or only guides search
is a product/research decision; this exploration has not changed that policy.

The PIC2 puts the GA inside a fixed workload and a contextual bandit above it to select
which workloads to explore. A previous conversational suggestion to use RL for selecting
GA mutations was a different design and should not silently replace the PIC2 architecture.
Search work can reuse the current evaluator while the new observation contract is defined.

## Where the contribution fits in prior simulator work

[Esgalhado's 2024 thesis](https://www.dpss.inesc-id.pt/~ler/projects/dacomico/aesgalhadomsc.pdf),
section 5.1.4, already explains name-update/AddParticipant/event interleavings, invariant
rejections and explicit retry, and competing Tournament updates protected by semantic
locks. Our recent matched pair has a different declared horizon without automatic retry.
The contribution should concern automated extraction, controlled replay, evidence-backed
classification and search; the domain story itself is established prior work.

The [2026 simulator preprint](https://arxiv.org/html/2605.01159v1), by Daniel da Palma
Pereira and António Rito Silva, also discusses semantic locks, monitoring, and controlled
interleavings. It is directly related background, not evidence that our verifier supports
all of that paper's topologies or that every described anomaly exists in this checkout.

## Literature for the paper

| Source | What to cite it for | Relationship to the local PIC2 bibliography |
| --- | --- | --- |
| Garcia-Molina and Salem, **Sagas**, SIGMOD 1987, pp. 249–259; [author-institution report](https://www.cs.princeton.edu/research/techreps/598) | Local transaction sequences and compensation; why Saga boundaries matter | Not present |
| Berenson, Bernstein, Gray, Melton, E. O'Neil and P. O'Neil, **A Critique of ANSI SQL Isolation Levels**, SIGMOD 1995, pp. 1–10; [paper](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/tr-95-51.pdf) | Precise terminology and limitations of a small anomaly list | Not present |
| Adya, Liskov and O'Neil, **Generalized Isolation Level Definitions**, ICDE 2000, pp. 67–78; [paper](https://15721.courses.cs.cmu.edu/spring2019/papers/02-transactions/adya-icde2000.pdf) | Versions, transaction dependencies and explicit outcome requirements | Not present |
| Kingsbury and Alvaro, **Elle: Inferring Isolation Anomalies from Experimental Observations**, PVLDB 14(3), pp. 268–280, 2021, DOI 10.14778/3430915.3430918; [paper](https://vldb.org/pvldb/vol14/p268-alvaro.pdf) | Inferring explainable anomalies from observations under explicit assumptions; year follows the paper's reference format | Not present |
| Korth, Levy and Silberschatz, **A Formal Approach to Recovery by Compensating Transactions**, VLDB 1990, pp. 95–106; [paper](https://www.vldb.org/conf/1990/P095.PDF) | Externalization of effects and semantic compensation after other transactions have observed them | Not present |
| Richardson, **Microservices Patterns**, 2018; [author's Saga pattern page](https://microservices.io/patterns/data/saga.html) | Practical Saga isolation and countermeasures; the author points to chapter 4, section 4.3 | Already reference [14] |
| Daraghmi, Zhang and Yuan, **Enhancing Saga Pattern for Distributed Transactions within a Microservices Architecture**, Applied Sciences 12(12), 6242, 2022; [publisher](https://www.mdpi.com/2076-3417/12/12/6242) | Existing related Saga work to revisit; full text was unavailable in this research pass | Already reference [5] |
| André Martins Esgalhado, **Simulator for Saga Implemented Microservice Systems**, MSc thesis, IST, October 2024; [thesis](https://www.dpss.inesc-id.pt/~ler/projects/dacomico/aesgalhadomsc.pdf) | Direct predecessor and concrete Quizzes interaction examples | Not present as this title |
| Daniel da Palma Pereira and António Rito Silva, **A Domain-Driven Design Simulator for Business Logic-Rich Microservice Systems**, arXiv:2605.01159v1, May 2026; [preprint](https://arxiv.org/html/2605.01159v1) | More recent simulator architecture and evaluation background | Not present |

Elle is a useful methodological precedent, not a drop-in Saga oracle. It infers
dependencies using observable operations and suitable datatypes; the paper excludes
predicate checking. Our complete local version observations offer different evidence,
but Saga outcomes, copied DTOs and compensation still require an explicit mapping.
Start with a few exact patterns; a graph of mere aggregate-name co-occurrences is not
a serialization proof. [Elle](https://vldb.org/pvldb/vol14/p268-alvaro.pdf).

The bibliography check concerns the local PIC2 PDF, not an unseen current paper manuscript.

Korth et al. are especially useful for the compensation discussion: once another
transaction has observed an effect, returning to an old physical snapshot is not in
general equivalent to undoing the operation semantically. A counter decrement followed
by another operation's decrement illustrates why blindly restoring the old count can
erase legitimate work. This motivates a compensation-overwrite investigation; it does
not supply a universal numeric harm score.
[Korth et al.](https://www.vldb.org/conf/1990/P095.PDF).

Practical countermeasures to explain alongside detection are semantic locks (check a
declared intermediate state), reread/version validation (check that the basis for a
decision still holds), and commutative or operation-aware compensation (undo a delta
instead of replacing an entire snapshot when the operation semantics permit it).
Richardson provides the Saga practitioner framing. These are prevention strategies,
not evidence that every protected path is safe; current command declarations and
runtime outcomes must be checked separately.

## Suggested Codex organization

Keep one coordinating task, Thesis HQ, for choices and plain-language explanations.
Use three outcome-focused working tasks when those fronts begin: Anomalies and Impact,
Search GA/RL, and Paper. Subagents answer bounded literature/code/review questions
inside a task; they do not each become another user-facing project to track.
Keep decisions and source links in repository documents so work does not depend on
reconstructing task history. Allocate different files to simultaneous implementers;
preserve the user's local branch workflow and avoid overlapping builds or writes.

This investigation used two Sol medium subagents for literature and code audit, with
the coordinating agent checking the sources and synthesizing the recommendation.
No production code, score, generated catalogue, or application runtime was changed.

## Generated execution follow-up

The [ordinary package qualification](../../../issues/2026-09-09-generated-update-read/HANDOFF.md)
now reproduces the update/read condition without hand-scheduling runtime actions. Six
control and eight fault executions cover every read placement around the five update
steps and the two recovery checkpoints. Three fault executions expose the compensated
revision; all eight fault executions retain the same persistent recovery residue.
This is provider-backed input preparation with source-extracted Saga structure and
existing schedule generation, not autonomous discovery of a usable fixture.
