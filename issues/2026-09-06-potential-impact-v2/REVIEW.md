# M0 evidence-boundary review

## Pass 1

The initial M0 evidence-boundary snapshot was blocked pending the following concrete
corrections. These findings were sent to the root agent and M0 implementer. The first
five are recorded here as the pass-1 gate record; M0 has since applied corresponding
working-tree fixes and they require pass-2 adjudication against the final diff.

- [P1] Resolve receiver snapshots by full persisted identity — `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/monitoring/impact/PersistentStateObserver.java:155-161`

  Receiver lookup initially selected by logical integer ID alone. When two aggregate
  types share that ID, the horizon snapshot can describe the wrong receiver and make
  eligibility/effect evidence false. Carry the persisted aggregate type through the
  delivery fact, or reject an ambiguous ID with an explicit gap.

- [P1] Preserve dependency target identity and resolution — `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/monitoring/impact/PersistentStateObserver.java:178-190`

  A dependency fact initially retained only the subscription's integer target ID, so
  missing or colliding persisted targets could be treated as a valid relationship.
  Emit the resolved persisted identity only for one unique target and retain an
  explicit unknown/gap for missing or ambiguous targets.

- [P1] Project the managed durable instance — `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/transaction/sagas/unitOfWork/SagaUnitOfWorkService.java:311-313`

  The write hook initially merged an aggregate but projected the detached input rather
  than the `EntityManager.merge` result. Lazy state, listeners, and generated values
  can therefore differ from the committed row. Project the managed merge result (or
  reload the exact committed version) before registering the after-commit fact.

- [P2] Make collector finalization atomic with delivery capture — `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ImpactV2EvidenceCollector.java:64-77`

  Finalization initially copied/cleared delivery state while callbacks could append to
  it, allowing a delivery to be lost or a concurrent mutation to escape the horizon.
  Serialize final snapshot/horizon replacement with callback writes under one collector
  lock, and define the close boundary before report serialization.

- [P1] Record the actual pre-delivery eligibility — `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/aggregate/EventApplicationService.java:123-129`

  The initial delivery fact hard-coded `eligibleBefore=true`, which could award an
  unresolved-event fact for a stale, inactive, or otherwise ineligible receiver. Use
  the observer's exact pre-delivery predicate result and retain a gap when that result
  is unavailable.

## Pass 2

**PASS for the M0 FR1–4/9/11 evidence boundary.** The five pass-1 blockers are resolved:

- receiver lookup carries the expected persisted `AggregateIdentity` and ID-only lookup
  records ambiguity gaps;
- dependency facts carry the unique persisted target identity and gap missing/ambiguous
  targets;
- write projection uses the managed `EntityManager.merge` return;
- collector finalization is synchronized with delivery callbacks; and
- `eligibleBefore` comes from the actual latest-owner predicate.

The callback-failure finding is also resolved. `ScenarioExecutor` keeps the observer
installed through `finish()`, closes it, and drains retained enablement/callback/gap
failures afterward. The M0 handoff documents the supported synchronous Saga/local
transaction boundary and excludes unrelated asynchronous work beyond the scheduled
horizon. No remaining actionable M0 finding was established from the final diff.

M1 should define the final assessment-status composition for partial/unavailable
collection; M0 intentionally emits `NOT_ASSESSED_M0` with a null score while retaining
`PARTIAL`/`UNAVAILABLE` coverage status and gaps.

No build or runtime command was run by this reviewer. Root verified the reported focused
test artifacts independently; real JPA/proxy behavior remains an M2 proof boundary.

## Integrated M1/M2 review — final

**PASS.** No remaining actionable contract blocker was established in the assessor,
collector, observer boundary, or qualification controls. The reviewed implementation
retains exact event/action and writer attribution, final-horizon eligibility, typed
persistent comparisons, observer failure gaps, and separate unavailable/partial
assessment semantics without altering ImpactV1 or execution-v5 output.

Root verified the frozen full suites (simulator 134 tests; verifier 763 tests) with zero
failures, errors, or skips. The persisted qualification artifact
`verifiers/target/impact-v2-qualification/run-03/validation.json` reports `PASS`: all
eight enabled cases are `COMPLETE` with zero coverage gaps and scores
`[2, 0, 1, 1, 0, 0, 1, 0]`, and all three observer-disabled controls match their
enabled execution/state witnesses while reporting unavailable collection with null
counts. The M2 handoff records the bounded Quizzes topic-course-reference residual as
an application follow-up; it is not an evidence-contract failure.
