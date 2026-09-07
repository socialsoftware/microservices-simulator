# Empty event delivery — implementation and qualification handoff

## State

`complete` — M0 implementation, focused checks, M1 runtime qualification, and final
Astra review passed. Sol medium implemented; Astra reviewed and qualified the result.

## Outcome

- A selected Saga/local event route now records exactly one replay result: `DELIVERED`
  or `NO_ELIGIBLE_SUBSCRIBER`.
- Successful empty eligibility enumeration returns the latter result without invoking a
  consumer, inventing a subscriber, or emitting an `EventDelivery`. ScenarioExecutor
  reports the captured event and selected route, a null subscriber, body `NOT_RUN`, and
  continues the schedule with exact conformance when the remaining actions conform.
- Missing, repeated, and conflicting replay results are sticky control failures. Multiple
  eligible receivers, event/route mismatches, recursion, selection exceptions, and
  handler failures keep their existing hard stops.
- ImpactV2 production logic is unchanged: only `EVENT_CONSEQUENCE` actions with status
  `COMPLETED` enter `UNRESOLVED_DELIVERED_EVENT`. Focused coverage proves that an empty
  attempt creates no candidate or missing-delivery unknown and does not hide a separate
  exact delivered-event positive.
- Execution reports move from v5 to v6 because the serialized action-status vocabulary
  and empty-route meaning changed. The record shape and package schemas are unchanged;
  the ImpactV2 sidecar remains v1. Canonical docs identify v5 reports as historical
  evidence under the old hard-stop policy.

## Actual files changed

- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/aggregate/EventApplicationService.java`
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/notification/EventReplayCoordinator.java`
- `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/notification/EventReplayCoordinatorTest.java`
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutionReport.java`
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutor.java`
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ImpactV2AssessorSpec.groovy`
- `verifiers/experiments/batch-execution/run.py`
- `verifiers/experiments/batch-execution/test_run.py`
- `docs/verifiers-impl/current-state.md`
- `docs/verifiers-impl/roadmap.md`
- `verifiers/experiments/empty-event-delivery/` (fixed qualification runner and reproduction)
- `docs/verifiers-impl/evidence/empty-event-delivery-2026-09-07/` (compact runtime/test proof)
- governing SPEC/PLAN and this handoff

## Discovery and decisions

- Status consumers were traced. `ImpactV2Assessor` already selects only exact
  `COMPLETED` event actions; `SagaReadExposureReport` copies action status without
  interpreting it, and the CLI prints it generically. The reusable batch runner now
  explicitly accepts historical v5 and current v6 reports while rejecting other schema
  versions. No assessment production change or additional status adapter was needed.
- The explicit outcome belongs to the replay scope rather than evidence collection.
  Empty selection is recorded only after exact event verification and successful real
  eligibility enumeration.
- The first private-cache command used a Maven output override that this build ignored
  and regenerated ignored artifacts under `simulator/target`; it did not clean or alter
  retained verifier evidence. Final proof was rerun from the isolated source snapshot
  `/tmp/empty-event-delivery-m0.8BqXAq/src` with its private Maven cache.
- No material scope delta was found. Receiver synthesis, retry, final backlog scans,
  fan-out, score expansion, Quizzes production changes, and historical-result rewrites
  remain excluded.
- [M1 Docker qualification evidence](../../docs/verifiers-impl/evidence/empty-event-delivery-2026-09-07/README.md)
  records six passing fresh-container attempts and confirms that all frozen production
  sources match the checkout.

## Validation

JDK 21.0.9, isolated source snapshot and Maven cache:

- `simulator`: `EventReplayCoordinatorTest` — 8 tests, 0 failures/errors/skips.
- `verifiers`: `ScenarioExecutorSpec` — 197 tests, 0 failures/errors/skips.
- `verifiers`: `ImpactV2AssessorSpec` — 23 tests, 0 failures/errors/skips.
- `verifiers/experiments/batch-execution`: `test_run.py` — 13 tests, 0 failures/errors.
- `git diff --check` — clean.

The focused scenarios cover observer on/off parity, empty and delivered outcomes, sticky
missing/repeated/conflicting-result rejection, later forward and selected-route
continuation, removal/acknowledgement causing later empty selection, independent real
deliveries and independent deleted-dependency/residual findings beside an empty route,
selection-query failure, and unchanged hard stops for the existing negative matrix. The
known `ApplicationsFileTreeParserSpec` `ReadResponseFixture` inventory
omissions were outside these focused checks and remain untouched.

## Final review

Astra reviewed the production diff, outcome/error paths, report consumers, tests and
canonical docs against FR-1–FR-8. The sticky repeated-result guard and reusable batch
reader compatibility findings were fixed and rechecked; no blocking findings remain.
All 791 production files still match the runtime snapshot and all 1,777 recorded build
files retain their hashes. The three focused test sources match the tested private
snapshot; [test outputs](../../docs/verifiers-impl/evidence/empty-event-delivery-2026-09-07/tests/)
are retained with the six-run evidence. The personal note and meeting note are preserved.

## What to try

Run an exact selected route whose eligibility becomes empty before its scheduled action.
The v6 execution report should show `NO_ELIGIBLE_SUBSCRIBER`, null
`subscriberAggregateId`, body `NOT_RUN`, no matching delivery record, and the next
scheduled action. Compare a one-receiver route to confirm it still reports `COMPLETED`
and receives ordinary ImpactV2 delivery assessment.

The schema-version choice is the main implementation decision a reviewer may reasonably
veto; retaining v5 would require an explicit compatibility argument for the expanded
status vocabulary.
