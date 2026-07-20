# Thesis meeting — 2026-W29

## Status

- Most of this week was spent writing the thesis.
- The current text is ready for review and feedback at its usual location.
- No compensation-interleaving or impact-scoring changes have been implemented yet; the following is a design proposal for discussion.

## Compensation interleavings — proposed plan

- Current executor behaviour: when Saga A fails, A is compensated immediately as one operation; only then does Saga B continue.
- Proposed behaviour:
  - mark A as aborted and skip its remaining forward steps;
  - expose A's completed steps as compensation actions in reverse order;
  - merge those actions with the remaining forward steps of live sagas;
  - replay deterministic alternatives where B progresses before, between, or after A's compensation actions.
- Reuse the simulator's stepwise `compensateUntilStep` support instead of treating `resumeCompensation` as one atomic action.
- Record forward and compensation actions, their ordering, participant, aggregate accesses, and final lifecycle state in the execution report.
- Discussion point: should the recovery/interleaving schedule be part of the scenario catalog, or an execution-time choice alongside the fault vector?

## Assumption: compensations eventually succeed

- Compensation actions are not fault slots and compensation-failure combinations are not searched.
- No retry count, backoff policy, or permanently partially-compensated terminal state needs to be modelled.
- The executor can still interleave successful compensation actions with live sagas; the assumption removes failure outcomes, not the intermediate states visible during compensation.
- An unexpected compensation exception should invalidate/hard-fail the execution rather than contribute to the impact score.
- Limitation to state explicitly: this cannot evaluate unavailable compensation services, retry cost, or permanently stuck Sagas.

## Detecting anomalies from runtime evidence

- Evidence already available: ordered/timestamped events, functionality invocation and step identity, unit-of-work version, command fields, and aggregate `READ`/`WRITE` events with aggregate type and id.
- Candidate detectors:
  - **Dirty read:** B reads an aggregate version/value written by A before A reaches a successful terminal state, and A later compensates.
  - **Lost update:** A and B read the same base aggregate version, both write it, and the final state preserves only one update.
  - **Non-repeatable read:** one Saga reads the same aggregate twice, performs no intervening write itself, and observes different versions/values after another Saga writes.
- Current evidence can identify suspicious read/write overlaps, but cannot confirm these anomalies: it does not record the aggregate version/value observed by each read, before/after write state, explicit commit/compensation boundaries, or a final-state snapshot.
- Proposed approach:
  - first emit low-cost **anomaly candidates** from ordered read/write traces;
  - add observed aggregate version plus compact before/after or field-level state evidence;
  - add lifecycle events and a final-state snapshot;
  - only classify a confirmed anomaly when the required causal/value evidence exists.
- Discussion point: begin with generic version/read-write detectors, or first implement a few Quizzes-specific invariants as stronger impact oracles?

## Meeting outcomes

- Begin writing the article, initially focusing on:
  - introduction;
  - proposed solution;
  - related work.
- Reformulate the scenario catalog so that fault vectors and compensation interleavings are represented as part of scenario semantics, rather than only as execution-time behaviour.
- Define the new catalog model before changing the generator or executor.
