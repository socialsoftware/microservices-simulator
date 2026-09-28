# Event-produced reads and pruning qualification

The compensated-read assessor now recognises a version produced by a completed selected
event delivery. It uses the existing runtime instrumentation; no new application mappings,
business rules, score weights or simulator behaviour were introduced.

## Attribution rule

Join the write and read by persistent identity and revision. For an EVENT_CONSUMER writer,
also require the same execution attempt, workload, action, event ID and subscriber ID.
Match the action to the package's event route and triggering step, including the handling
class/method and handler class. Require successful completion before the read in both
action order and observation order. The resulting negative assessment is
`EVENT_DELIVERY_COMPLETED_BEFORE_READ`.

The synchronous executor does not interleave another participant inside a selected delivery.
This proves that this read occurred after the consumer action. The publisher's Saga ID is
correlation metadata, not proof that publisher compensation compensates the consumer's
write. Failed, missing, ambiguous, misordered or mismatched evidence remains unknown.
Consumer-internal reads and asynchronous/nested consumer recovery are not added to scope.
The sidecar retains source event routes and runtime event metadata as additive v2 fields;
old reports without this proof remain historical observations.

## Application qualification

The existing `AnonymizeStudentAndSolveQuizTest` already prepares a Tournament participant
in `setup()`. Its event eligibility feature now also delivers the event and checks the
name through the public Tournament query. Both features in that test class pass.

The normal source analyser extracts UpdateStudentName and FindTournament from this feature.
Their generated setup contains 15 ordinary calls, including AddParticipant. The experiment
does not patch the setup or assign runtime IDs. Pruning on/off both generate the same
16 WorkloadPlan IDs, including routes outside the focused runtime selection below.

Select exactly the Tournament route and put the name update first. This gives two plans:

1. Update the student's name, deliver the event, query Tournament.
2. Update the student's name, query Tournament, deliver the event.

For each, execute no fault, a fault before the update, a fault before the query, and faults
before both steps. Each assignment has one generated recovery sequence: eight distinct
scenarios altogether, two normal controls and six faulted variants. Both actual deliveries
in the normal controls reach Tournament 12 and propagate the participant's name. Faults
before the update mask its event; faults before the query prevent that read. All eight
have exact conformance and complete zero counts in all five criteria.

Also repeat the two previously qualified AddParticipant/update/query controls, with the
query before versus after delivery. Both now have complete zero scores. The after-delivery
control previously had incomplete read attribution. This is a measurement correction;
the original campaign reports are not rewritten.

These zero outcomes do not demonstrate a newly found harmful execution or general pruning
preservation. Recovery-only pairs and matched isolated controls remain separate work.

## Checks and provenance

- 293 verifier tests passed: SagaReadExposureSpec and ScenarioExecutorSpec. Tests include
  wrong event/receiver/handler, missing and duplicate route/action, failed and empty delivery,
  ordering errors and publisher compensation independent of the consumer.
- Two Quizzes application tests passed, including the extended ordinary event test.
- Runtime qualification uses fresh native Java 21 processes, the existing fixed clock,
  hash-verified campaign dependencies and an isolated overlay of the changed read assessor
  and report classes. Docker remained unavailable. This is functional evidence, not a
  Docker timing/parity comparison.
- Raw scripts, generation, selected packages, commands, logs and receipts:
  `verifiers/target/event-read-attribution-2026-09-19/`.
- Final evidence directories: `joined-final`, `prepared-pair-final` and
  `prepared-pair-faults-final`. Earlier diagnostic attempts are retained and excluded from
  the final distinct scenario count. `final-build-receipt.json` pins the changed classes
  and sources. The runtime summary checks package/report hashes and cross-report IDs.

## Refreshed input selection counts

This extraction has 819 eligible inputs and 37 Saga types, with a cap of 1,000 that removes
none. The ordinary test extension adds two inputs: its public query and event-processing
invocation. Both selection rules below use exactly these same 819 inputs.

| Saga types per combination | All input combinations | Forward-only selection | Current selection |
| --- | ---: | ---: | ---: |
| 2 | 311,137 | 123,701 | 136,156 |
| 3 | 72,904,145 | 23,120,980 | 27,551,023 |
| 4 | 11,820,344,542 | 3,800,560,014 | 4,739,545,978 |

Of 666 pairs of Saga types with inputs, 248 have admitted tuples using direct forward
accesses and 316 using the current event/compensation-aware selection. For every set,
the former count is no greater than the latter, which is no greater than all combinations.
Counts use production InputTupleSelection and both production conflict graph views;
per-set rows are retained under `counts/` for independent summation.

These are input combinations before ordering, event placement, faults and runtime setup.
They are not executable FaultScenario counts. This recount does not measure compressed
orders; the old 565,344 figure belongs to the earlier generator snapshot. Its replacement
needs accounting with the current tuple-applicable anchors.

## Topic-update follow-up control

A further fresh control of WorkloadPlan `09012cdb7635d5e9fa5f0c805077456689fb1174382f68f45041eb91171a8bf7`
also passes SUCCESS / EXACT with complete zero counts in all five criteria. UpdateTopic
publishes its update, the selected consumer updates Tournament, and UpdateTournament
then reads that persisted version. Its assessment is
`EVENT_DELIVERY_COMPLETED_BEFORE_READ`, with no attribution gaps. This qualifies the
normal story for a fresh 44-scenario measurement; no faulted cases were rerun here.

This eleventh final qualification attempt uses the same frozen dependencies and final
assessor overlay as the ten attempts above. Evidence is in `topic-control-final/`, with
an independent `verified-summary.json` checking package/report hashes and cross-report
identities. The earlier ten-case summary remains scoped to its original selection.
