# Potential-impact assessment for Saga/local execution

Status: implemented and qualified for the approved first slice; see HANDOFF.md for proof and limits.

## 1. What ImpactV2 is

ImpactV2 counts distinct aggregate identities with an observed potential final-effect
condition after a persisted Saga/local FaultScenario. Each point has a reason and
supporting facts. It measures the extent of three declared patterns, not business harm,
severity or universal consistency. ImpactV1 retains its invariant-rejection meaning.

The first version reuses application dependencies already exposed through
`Aggregate.getEventSubscriptions()`. It does not infer references from field names.
The Tournament-to-Quiz and QuizAnswer-to-Quiz experiment relationships both use this API.

## 2. Goals

1. Attach the same assessment process to every ordinary supported execution attempt.
2. Detect declared dependencies on deleted targets, failed-operation residual data,
   and unresolved delivered events without Quizzes-specific checking code.
3. Distinguish a measured zero from incomplete evidence and invalid execution.
4. Reproduce positive and healthy controls through the production execution path.

## 3. Non-goals

Full serializability, field-level causal attribution, arbitrary DTO/reference inference,
LLM judgments, custom harm rules, severity weights, other transactional profiles,
automatic extra event delivery and subsequent user-operation probes are excluded.
Production application bug fixes and broad scenario-generation repairs are separate.

## 4. Functional requirements

### Evidence and horizon

- **FR-1:** Measurement starts after successful setup and baseline cleanup. Record the
  initial state and the final state after the scheduled forward/event/recovery actions.
  Recovery-boundary observations may explain restoration but are not separately scored.
  Do not execute extra application work to settle the score.
- **FR-2:** Observe exact aggregate identities, committed writes,
  persistent application data, lifecycle state, writer identity and phase, recovery
  outcomes, and selected event/receiver identity and eligibility. Distinguish attempted
  writes rejected or rolled back from durable writes. Event consumers are writers too;
  do not identify all writes by the top-level participant alone. Retain exact read versions
  when existing hooks supply them; reconstructing cached reads is not required by these
  three checks and their absence alone does not make an assessment incomplete.
- **FR-3:** Compare application data deterministically, including application dates,
  owned collections and embedded reference versions. Exclude known framework row IDs,
  aggregate revision/creationTs/predecessor and semantic locks from data equality, while
  retaining them as evidence. Unsupported mappings or missing identity produce explicit
  coverage gaps. Lifecycle ACTIVE/INACTIVE/DELETED is not ignored.
- **FR-4:** Collect current dependency declarations from each observed aggregate's
  subscriptions, identifying the receiver from the owning aggregate. Resolve targets
  through actual persisted aggregate identity. Missing target type/identity is unknown;
  integer equality or property-name conventions are not reference proof. This category
  covers subscription-declared dependencies, not every application reference.

### Three checks

- **FR-5 — Deleted dependency:** Count an ACTIVE source aggregate whose final subscription
  still names a target observed becoming DELETED during measurement and remaining so at
  the horizon. A target already deleted before measurement does not satisfy the rule.
  Historical-reference legitimacy is not inferred. Deduplicate multiple subscriptions
  to the same affected source.
- **FR-6 — Failed-operation residual:** Count a final persistent data/lifecycle difference
  on an aggregate durably changed by a failed Saga after its scheduled recovery has
  finished, only when that Saga and its own recovery are the only observed writers of
  that aggregate during measurement. Preserve absence/presence for creation and deletion
  rather than assuming every object existed initially. Another participant or event
  consumer writing the same aggregate makes this object/category UNKNOWN; unrelated
  objects and the other checks remain evaluable. A semantic-lock change or rejected
  write alone is not a data residual. Restoration observed before a later independent
  write remains useful evidence but cannot justify blaming the final difference on the
  failed Saga. Missing recovery/writer evidence is UNKNOWN, not positive impact. The
  user-approved 7 September amendment excludes a baseline-absent object whose first
  observed write creates it ACTIVE in FORWARD and whose first DELETED and final writes
  are in RECOVERY, after the existing evidence/sole-writer/recovery checks pass. Keep its
  raw evidence and independently assess surviving active dependencies. See the
  [policy decision](../../docs/verifiers-impl/decisions/2026-09-07-recovered-creation-remnants.md).

- **FR-7 — Unresolved delivered event:** Count a surviving receiver when a scheduled
  delivery of the exact event succeeded, left its normalized receiver application data
  unchanged across that delivery, and that same event remains eligible for that receiver
  at the final horizon. Invoke the final owner's polymorphic
  `EventSubscription.subscribesEvent(exactEvent)` predicate; do not reconstruct eligibility
  from base fields. An unsupported projection or predicate evaluation is UNKNOWN.
  A repaired later delivery that makes the event ineligible, or receiver removal, clears
  the finding. Undelivered retained events, guards, mere version increments and handler
  success alone do not earn points. Eligibility observations must not deliver an event.

### Report and interpretation

- **FR-8:** The object count is the union of positive aggregate identities across checks.
  Preserve each reason, object, relevant actions/versions and category coverage. An
  object satisfying two checks earns one point. Setup/infrastructure failure or an
  invalid measured execution has a null score. Partial coverage retains findings and
  an explicitly named observed count, with the complete score null. A complete zero
  means absence within these checks and declared coverage, not global correctness.
- **FR-9:** Record the observation horizon and candidate scope. Relevant unfinished work
  that prevents a final-effect conclusion is UNKNOWN. Mere historical event storage
  does not invalidate an assessment. A valid domain failure can be assessed; an
  incomplete executor run must not masquerade as a completed domain scenario.
- **FR-10:** Preserve raw findings from ordinary and faulted runs. Compare matching
  unassigned-fault controls during evaluation; do not label a baseline application bug
  as injected-fault damage or silently subtract its count.
- **FR-11:** Add a separately versioned assessment without changing ImpactV1 or persisted
  scenario/package identity. Collection failures must not replace application outcomes;
  retain the evidence gap and invalidate the affected assessment. Output is deterministic
  given the same normalized evidence and does not leak between attempts.

## 5. Architecture

One shared runtime observer feeds three small deterministic checks and one report
assembler. Reuse simulator persistence, subscription and execution context APIs.
Keep framework observation in `simulator/` and checking/reporting in `verifiers/`;
preserve existing analysis pipeline boundaries. No detector registry or rule language.
Use dummyapp for generic positive/negative contracts and Quizzes for realistic execution.

## 6. Data model

The assessment records attempt/package/workload/scenario identity, horizon, completeness,
nullable complete score, observed affected-object count, category results and evidence.
Facts include aggregate type/logical ID, normalized data, committed writer/version,
dependency target, and event/receiver identity. Exact Java types and internal layout are
implementation choices; persisted assessment semantics follow FR-1 through FR-11.

## 7. Security model

Stay within the existing local execution and report-output authority. The observer is
read-only with respect to application state; never call business mutations from a check.
Do not expand local state reports to external logging services.

## 8. Operating

Every supported normal attempt emits assessment status, including explicit unavailable
status when setup or infrastructure fails. Existing ImpactV1 consumers remain compatible.
Qualification reports observation coverage separately from positive counts. Observer
on/off controls must preserve application outcomes, schedule and final state.

## 9. Future roadmap

Relax competing-writer UNKNOWN only with validated attribution evidence. Relationships
outside subscription declarations, propagated failed values and lost-update detection
are subsequent detector extensions, not hidden promises of this score.

## 10. Open decisions

No blocking discovery questions remain. The user approved the assembled package with
"yes, that sounds good. execute the package". The conservative writer rule and
subscription-based first scope are approved; implementation proof is recorded in handoffs.
