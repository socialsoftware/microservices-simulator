# Selected event delivery with no eligible receiver

## What this is

The controlled Saga/local executor currently stops when a selected event route has no
eligible aggregate. Ordinary application polling simply performs no delivery in that
case. A receiver may never have existed, may have been removed, or may already have
processed the update. None of those facts alone establishes impact.

An event consequence will attempt delivery through its selected route at its scheduled
position. A confirmed empty eligible set is an explicit completed attempt, with zero
deliveries, rather than an execution failure. Actual deliveries retain their existing
ImpactV2 assessment.

## Goals

1. Continue the scheduled execution after a confirmed empty receiver selection.
2. Distinguish no delivery, actual delivery, and failed selection/processing in reports.
3. Preserve existing positive impact findings and uncertainty handling.

## Non-goals

No receiver synthesis, input rewriting, generator filtering, automatic retry, event
draining, multi-object fan-out, new impact category, or global undelivered-event oracle.
No Quizzes production changes or revision of historical experiment outcomes.

## Functional requirements

- **FR-1.** After resolving the exact captured event and selected route, a successful
  eligibility check returning zero receivers shall finish the action with the explicit
  outcome `NO_ELIGIBLE_SUBSCRIBER`. Later scheduled actions shall continue.
- **FR-2.** That outcome shall retain the selected event/route identity, have no receiver
  identity or delivery record, and be visible in execution diagnostics. It shall not
  claim that an application handler processed the event or that input quality was the
  cause. The eligibility check occurs at the scheduled action, not only during setup.
- **FR-3.** A successful action with zero deliveries shall preserve schedule conformance
  when the schedule otherwise conforms. Completed execution shall remain eligible for
  ordinary impact assessment, subject to existing observation coverage requirements.
- **FR-4.** Empty selection shall contribute no positive finding by itself. It shall not
  be treated as missing evidence of an actual delivery. Other affected objects and
  coverage gaps shall still determine the scenario's score and assessment status.
- **FR-5.** `UNRESOLVED_DELIVERED_EVENT` shall retain its existing contract: exact
  successful delivery, unchanged persistent receiver state across delivery, and the
  same surviving receiver still eligible at the final horizon. An action claiming an
  actual delivery without its exact observation shall remain incomplete evidence.
- **FR-6.** Selection exceptions, missing/mismatched captured events or routes, more than
  one eligible receiver, recursion, handler failures, and failure after trigger emission
  shall retain their existing failure policies. Trigger-fault masking remains distinct.
- **FR-7.** Replay shall require one explicit outcome: one actual delivery or one confirmed
  empty selection. Returning without either, repeated/conflicting outcomes, or leaked
  replay state shall remain control failures. Observation mode shall not change behavior.
- **FR-8.** Reports and qualification results shall distinguish attempted routes, actual
  deliveries, and empty selections. A zero score is not a claim that all possible event
  effects were assessed. A receiver appearing after this action is not automatically
  revisited. Retained old runs shall keep their original policy and results.

## Architecture

Keep route extraction, deterministic scenario identity, exact event capture, application
subscription predicates, and synchronous Saga/local scheduling. The replay coordinator
owns explicit action completion; the executor owns reporting and continuation. Impact
assessment remains evidence-based and separate from replay outcome selection.

## Data model

Add an explicit execution outcome, not a synthetic EventDelivery or a new domain entity.
Preserve existing event/workload/action identity and package contents. Execution-report
compatibility must account for the new status; do not silently reuse `COMPLETED` to mean
zero delivery. Explain the runtime-policy change in canonical documentation and evidence.
The glossary's existing event consequence definition gains the attempted-delivery
semantics; no additional domain term is needed.

## Security model

Not affected. Existing exact-route dispatch and typed runtime authority remain intact.
Do not catch arbitrary failures and reinterpret them as empty selection.

## Operating

Apply to controlled Saga/local replay; ordinary polling retains its behavior. Use
version-labelled fresh executions to qualify the policy. Reverting the change restores
the strict empty-selection failure; preserve evidence from both policies.

## Future roadmap

No-fault controls can qualify which routes a workload actually exercises. General
never-delivered event detection and preparation synthesis remain separate decisions.

## Open decisions

None. The assembled SPEC/PLAN was approved for implementation on 7 September 2026.
