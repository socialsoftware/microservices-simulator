# Implement potential-impact assessment

Status: M0/M1/M2 implemented and qualified in `/Users/andre/meic/thesis/microservices-simulator-impact-v2`
on `codex/potential-impact-v2`. No commits or merge are authorized.

## 1. Environment and execution mode

One spec and plan, reviewed milestones, isolated `codex/` worktree after approval.
Preserve the primary checkout's inherited dirty work. Do not commit, push or merge by
default. Incorporate the required inherited changes deliberately; do not start from a
clean HEAD that silently omits the current verifier behavior.

Adopt the completed B reporting repair's reviewed incremental changes into the execution
worktree before overlapping executor edits, comparing against the current primary file.
Its handoff is in the sibling `microservices-simulator-b-preflight` worktree under
`issues/2026-09-05-preflight-failure-reporting/`. Do not merge that entire dirty tree.

Root owns integration and the collector's attribution/commit semantics. Sol Medium can
own a bounded implementation slice; Luna Max can independently review the rules and
controls. Parallelize rule/test work only after its evidence inputs stabilize. Avoid
multiple agents editing ScenarioExecutor or building against changing module sources.
No intermediate user verdict is needed unless a material contract change emerges.

Planning review checked this boundary against Quizzes source and tightened polymorphic
eligibility, the definition of event progress, optional read-version evidence and bounded
initial qualification. This was document/source review, not runtime validation.

## 2. Implementation strategy

### Why competing writers matter now

The read-only audit found existing tests rather than only a hypothetical collision:

- `AnonymizeStudentAndUpdateTournamentTest.groovy:358-397`: request four questions,
  fail because too few exist, compensate back to two, then anonymize the creator and
  mark the same Tournament INACTIVE. The final difference is legitimate event work.
  Lines 400-445 also show the event rejected during IN_UPDATE and processed after recovery.
- `RemoveTournamentAddParticipantRecoveryWindowExploratoryTest.groovy:103-139`:
  AddParticipant writes the Tournament during RemoveTournament's deletion window;
  deletion later fails. AddParticipant forbids IN_UPDATE, not IN_DELETE.
- `AddParticipantAndUpdateTournamentTest.groovy:94-112` covers an overlap that is
  blocked. Do not assume every apparent pair of writers reaches two committed writes.

These are source-inspected executable test definitions, not new test runs or incidence
measurements. The latest inspected package is
`verifiers/target/astra-nested-bindings/final-generated/quizzes-20260905-125205-380/`.
It contains 941 one-participant workload rows and three provider-backed pairs. Two pairs
combine UpdateStudentName with AddParticipant, with the Tournament name event before or
after participant insertion. The third combines removal and participant insertion.
Those shapes are not runtime success claims. UpdateTournament has 28 workload rows but
none referenced by persisted FaultScenarios here, so the earlier hand-driven update
experiment does not establish generated executor coverage. Counts are catalogue shape,
not the 665 static-candidate denominator or qualification counts.

### Small implementation, explicit facts

Keep state normalization restricted to managed persistent data supported by this
simulator. Reuse `Aggregate.getEventSubscriptions()` for dependency declarations and
exact event eligibility; both existing deletion examples expose their Quiz dependency
there. For eligibility call the loaded final owner's actual subscription object's
`subscribesEvent(exactEvent)` method, retaining subclass payload/state conditions. Never
invoke a delivery service to observe eligibility. Obtain receiver identity from the owner, not the subscription's
`subscriberAggregateId` field, whose base constructor currently initializes it from
the target ID. Do not add a broad static DTO/data-flow analyzer.

Capture at baseline, committed writes/recovery boundaries and final observation. Batch
baseline/final enumeration; do not rescan every aggregate after every action or copy
the whole database per detector. Reuse loaded versions and compute projections once.
Resolve committed writes with transaction-aware evidence and persisted version identity;
the current registerChanged WRITE event follows merge but can precede transaction commit.
Ordinary logs remain useful, but attempted merge is not proof of durability.
Keep read-version recording opportunistic at existing hooks; these three checks do not
need a new cached-read or value-lineage reconstruction subsystem.

Treat event consumers as distinct writers even when triggered by a participant. Keep
the simple whole-object competing-writer guard through the final observation horizon;
do not build a per-field history engine. Recovery snapshots explain the successful
restore-then-anonymize example, while final scoring remains tied to the end of the run.

## 3. Milestones

### M0 — Shared evidence in ordinary execution (FR-1–4, FR-9, FR-11)

**Outcome:** normal attempts supply reliable normalized observations and coverage to
checking code. No positive scalar is claimed from a partial collector.

**Boundary:** simulator Saga persistence/dynamic evidence, event observation, verifier
attempt lifecycle/report plumbing, dummyapp fixtures and affected canonical docs.
Anchors: `SagaUnitOfWorkService`, `EventReplayCoordinator`, `Aggregate`,
`EventSubscription`, `ImpactV1Collector`, `ScenarioExecutor`.

**Discovery:** confirm transaction scope and rolled-back write visibility; prove safe
read-only subscription enumeration and final-state loading for current mapped aggregates.
Classify unsupported mappings explicitly. Confirm event actor identity survives nesting.

**Proof:** commit versus rollback/rejection, dependency owner/target and overridden
subscription predicates, preservation of available exact read versions,
application dates versus framework timestamps, owned collections and cycles, setup
exclusion, attempt isolation and observer on/off equivalence. Record collector cost on
the same small representative executions without a separate benchmark campaign.

### M1 — Three checks and the scored report (FR-5–11)

**Outcome:** distinct affected objects, reasons and completeness appear beside ordinary
execution results, with ImpactV1 unchanged.

**Boundary:** small verifier check functions/classes, report serialization, targeted
simulator eligibility observation only if needed, and dummyapp-first Spock contracts.
Use the M0 evidence representation for all checks. No new observer per detector.

**Proof:** deleted versus restored/pre-existing target; residual update/create/delete
versus successful recovery; legitimate concurrent or post-recovery writer; framework-only
changes; delivered/no-progress event versus converged, guarded, idempotent and undelivered
controls; duplicate findings; missing facts; invalid attempt/null and complete zero.
The existing restore-then-anonymize example must not become a false positive residual.

### M2 — Qualify through persisted execution (all FRs)

**Outcome:** reproducible persisted positive/healthy cases and explicit observation
coverage for the three checks. Integration applies the same assessment to every supported
attempt, but this milestone does not claim exhaustive runtime qualification.

**Boundary:** bounded Quizzes test/prerequisite fixtures using existing setup/package
contracts, Docker execution, assessment artifacts, and canonical documentation.
Convert deletion, update propagation and compensation controls into persisted executor
cases. If necessary add a minimal Quizzes fixture to provide UpdateTournament input/setup;
do not launch a general materializability expansion. Retain temporary-copy repair/mutant
provenance; production Quizzes fixes are separate.

**Proof:** same collector/checks across the three families; fresh matching unassigned-fault
controls; no hidden event drains; package/attempt identity; generic rule code without
Quizzes names. Record cost and completeness on these representative runs before proposing
the broader qualification batch. This closes the first implementation slice, not the
all-case evaluation campaign.

**Subsequent qualification:** freeze the current executable catalogue and list IDs and
exclusions; reuse one zero-vector control per workload and batch its initial fault
variants against immutable builds. Estimate runtime from M2 before launching. Refresh
the historical 34-case landscape in that campaign, reporting blockers rather than
relabeling old 19/15 results. Keep this visible in the roadmap; do not make exhaustive
execution, recovery-permutation enumeration or new pair/triple generation a prerequisite
for delivering the collector and checks.

## 4. Validation strategy

Run focused simulator tests for hooks and verifier Spock tests for the contract. Run
complete affected module suites once integrated, using their module directories; no root
Maven aggregator exists. Use Compose for real application evidence, persisting full
logs and reports under `verifiers/target/`. Build canonical docs with
`./scripts/verifier-docs build` and run `git diff --check`.

One independent review at M0's stable evidence boundary, then one integrated review after
M1/M2; further passes only for concrete unresolved findings. Reuse existing fixture
generation and Docker images where valid; batch compilation and run independent attempts
against immutable builds. Record actual checks and limitations in the implementation
handoff. Do not turn source inspection in this plan into a test-pass claim.

## 5. Risks and fallbacks

- **Observation becomes the project:** use existing persistence/subscriptions and narrow
  unsupported mappings before adding abstraction. No rule engine or universal serializer.
- **False residual attribution:** require a committed failed-writer contribution and
  exclude competing writers for that object/category; keep other findings evaluable.
- **Transient state mistaken for a final result:** retain the explicit horizon and
  unfinished relevant work status; do not alter execution to get a number.
- **Baseline defects dominate fault evaluation:** record controls and raw counts, without
  automatic subtraction or fault-causality claims.
- **Coverage versus generality:** subscription-declared dependencies are the stated
  initial scope. Missing observations are gaps, not silently inferred relationships.
- **Shared dirty work/B overlap:** review the incremental base before executor edits;
  preserve unrelated changes and record adopted sources. Broader production fixes or
  executor-policy changes require a contract amendment and user decision.
