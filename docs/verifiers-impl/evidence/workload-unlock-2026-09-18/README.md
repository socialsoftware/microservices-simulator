# Workload unblock qualification

This follows the [workload triage](../../research/workload-triage-2026-09-18.md).
It changes no application, generator, detector, or search implementation. It reuses
source-derived inputs, regenerates one stale package, and qualifies an existing
forward order with the frozen runtime from the 5,184-case map. The remaining two
blockers are diagnosed below; no broader detector semantics are silently introduced.

Raw evidence and reproduction scripts:
`verifiers/target/workload-unlock-2026-09-18/`.
The durable `qualification.json` records outcomes, components, source/package provenance,
and report hashes. All runtime qualification uses Docker, fresh application state,
five unit weights, and at most two simultaneous executors.

## Join, update, and query: ready control and positive witness

The original catalogue already contains the order:

1. Complete AddParticipant's two steps.
2. UpdateTournament reads the original Tournament and the topics, then writes the Tournament.
3. FindTournament queries that version.
4. UpdateTournament selects questions and updates the Quiz.

Selection rule: preserve the exact original inputs; finish the joining write before
UpdateTournament acquires its semantic lock; place the query immediately after the
Tournament write. This rule was recorded before measuring the fault outcome. The
selected workload is
`a308f424b252c1355a35baab75da17d728e4eddbcd14f549e0c64ba5e6278940`.
It comes from the existing 168-order catalogue, not a hand-edited workload.

The normal control is SUCCESS/EXACT with all five criteria complete and zero.
Injecting a fault at the final updateQuizStep produces PARTIAL_COMPENSATED/EXACT,
with one failed-operation residual and one compensated-read exposure: weighted score 2.
All five criteria remain complete. The first generated recovery order was used as a
single diagnostic witness; the full fault domain has not been measured here.

This qualifies a useful additional three-Saga interaction. It does not yet establish
its positive density, total measured landscape, or GA performance. Its residual/read
mechanisms overlap those already observed, so it is not sufficient diversification alone.

## RemoveCourseExecution: regenerate the package, retain both receiver orders

`RegenerateEventPackage.java` reuses production extraction and export against current
Quizzes source, restricting inputs to the original
`RemoveCourseExecutionQuizAnswerReceiverTest` feature. The current isolated verifier
build's recorded source hashes matched before generation. No input IDs or copy mappings
were supplied manually.

Generation retains the same input
`fdcbce1a30eac8c4e70981013544081994e4c055ebf4e48c5226ef6b343c9c8e`
and emits 16 workload plans, including selected event combinations, with no workload
or event expansion cap reached. The package now contains inferred `copy-contracts.json`.
Both original two-receiver workload IDs survive:

- Answer then Quiz: `46ba0843baee94204e06eaa800135fd6ea58d3066028bb1860ae598fc2037476`.
- Quiz then Answer: `ebb440…` (full ID in qualification.json).

The two fresh no-fault controls are SUCCESS/EXACT, score 0, and complete in all five
criteria. Each fixed order has four canonical vectors: no fault, or a fault at one
of its three normal Saga steps. The qualification enumerates and executes these
vectors after its control passes; final per-vector outcomes are in qualification.json.
All eight executions (four per order) are EXACT with complete measurements for all
five criteria. Each order yields scores 0, 0, 0, and 1 for vectors 000, 100, 010,
and 001 respectively. The positive is a failed-operation residual; neither order
produces an unresolved-delivered-event finding in these four cases.
These four vectors do not independently omit selected event routes: the receiver
order is fixed by the workload. Different route subsets are different workloads.

The obsolete package was the blocker. The change is package regeneration, not a new
application fixture or a relaxation of detector coverage.

## Event-produced reads: precise cause and proposed extension

The UpdateTopic/event/UpdateTournament control records a committed Tournament write
with `kind=EVENT_CONSUMER`, `phase=EVENT`, the exact event ID, and the event action ID.
The matching EVENT_CONSEQUENCE action and receiver before/after observations exist.
A subsequent ordinary Saga query reads that exact Tournament revision.

The read assessor cannot currently join these facts:

- `SagaReadExposureAssessor.Index.owned` accepts only `kind=SAGA`.
- `forwardAction` additionally requires a FORWARD action and a matching source step.
- `SagaReadExposureCollector.eventDelivery` deliberately ignores delivery facts.
- The diagnostic Action projection omits event runtime evidence and reconstructs the
  functionality name from the publisher participant rather than the consumer handler.

Therefore `PRODUCER_ACTION_UNPROVEN` is an explicit support gap, not missing data in
this particular execution. Simply admitting EVENT_CONSUMER into `owned` is unsafe:
the writer's participant ID is inherited from the publishing Saga. Its successful
completion or compensation does not establish the lifecycle of the consumer's write.

Proposed bounded change: retain the delivery's event/action/receiver identity and
successful outcome in the read evidence; prove that the read revision is the receiver
revision from that delivery; distinguish this consumer origin from an ordinary Saga
producer. Evaluate any subsequent supersession/recovery using that origin, rather
than attributing publisher compensation to the copied receiver value by default.
Keep missing, ambiguous, or failed delivery evidence explicit.

Before implementation, pin down the event-origin verdict contract and add generic
positive/negative/unknown tests: successful delivery and later read; wrong event or
receiver/version; missing or failed delivery; publisher compensation without receiver
rollback; and a genuinely supported subsequent reversal. Then run the retained
UpdateTopic and name-propagation controls and ordinary Saga read regression cases.
This is a small documented detector extension, not an evaluation-only package repair.

## Query, removal, and update: helpers and mixed source contexts

Fresh extraction reproduces the unavailable recipes. Representative leaves include
unresolved inherited constants (`USER_NAME_1`, `STUDENT_ROLE`, course constants),
unresolved helper arguments (`endDate`, `courseExecutionDto`), and an explicit
`unresolved depth-limit` for a nested TopicDto constructor. The visitor's current
trace depth limit is 12. These occur along the nested test helpers that create users,
course execution, topics, and Tournament; merely substituting an aggregate ID would
lose the required setup provenance.

The previously selected tuple also crosses three different feature methods:

- FindTournament `0d52de15…`: `sequential: update; remove`;
- RemoveTournament `b1337535…`: `concurrent: update - getTopicsStep; remove; update - resume`;
- UpdateTournament `55d483cd…`: `sequential: remove; update`.

Even a better helper resolver must not silently combine independent observed setup
contexts. The next bounded investigation should select all three operations from one
coherent test story, trace helper parameter substitution and inherited constants with
dummyapp fixtures, and preserve exact earlier setup results. Increasing the depth
limit alone is not a demonstrated fix. A small normal application test with explicit
facade calls is an alternative if that is the intended source input, but was not added.

## Scope of this pass

Two practical unblock paths are verified: an existing non-rejecting forward order,
and regeneration with current copy contracts. The event-origin extension and nested
helper extraction remain separate implementation work. No large campaign or GA
comparison was launched, and no historical result or frozen runtime was overwritten.
