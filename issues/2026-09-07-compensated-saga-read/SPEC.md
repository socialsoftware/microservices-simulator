# Observation of a read of a subsequently compensated creation

Status: **implementation approved on 2026-09-07**. Rationale:
[DIAGNOSIS.md](DIAGNOSIS.md). This first slice restricts the general pattern of reading
a subsequently compensated effect to creation followed by logical deletion.

Approval record: the user accepted this SPEC/PLAN, explicit Quiz/Tournament adapters,
a separate diagnostic sidecar, and controlled-harness positive proof with ordinary
executor integration/control proof. Restored updates and runtime result binding between
participants remain excluded. The user requested English SPEC/PLAN; this translation
preserves the approved requirements. No further routine milestone approval is required.

## 1. What it is

A complementary diagnostic for the Saga/local ScenarioExecutor that identifies when a
response delivers to Saga B the revision created by another Saga A, and compensation
of that creation subsequently persists its deletion. It reports exposure without
requiring harm or a later write by B. It is not a read of an uncommitted local transaction.

Implemented glossary term: **exposure to a subsequently compensated
creation** — proven delivery of a revision created by another Saga, preceding
confirmed deletion by compensation of the creating step.

## 2. Goals

1. Produce an auditable finding from exact creation → delivery → compensation evidence.
2. Distinguish an evaluated negative from missing, ambiguous, or unsupported coverage.
3. Preserve application behavior and ImpactV1/ImpactV2 metrics.

## 3. Non-goals

Restored updates, final harm, field dependencies, classic dirty reads, lost updates,
write skew, serializability, event propagation, new inputs/schedules, result binding
between participants during measurement, gRPC/stream/TCC, and GA/score changes.
Do not reconstruct an arbitrary DTO graph or introduce a new tracing framework.

## 4. Functional requirements

- **FR-1 — Actual delivery.** Record only responses actually returned by a successful
  local call, after deserialization when enabled. Each adapter declares exact command
  and result classes, persistent type, and typed extraction of the outer aggregate's
  identity and revision. This is an audited/tested provenance contract, not inference
  from `get*` names, `id/version` fields, matching IDs, or the UoW version. Do not assign
  nested-reference versions to the outer aggregate.
- **FR-2 — Identity and attribution.** Preserve attempt, workload/scenario, participant,
  action/occurrence, phase, command, adapter contract/version, typed logical identity,
  runtime type, and returned revision. Require a unique join with A's confirmed write.
  Type collisions, ambiguous revisions, and unknown attribution are coverage gaps.
- **FR-3 — Positive predicate.** Require: (a) covered prior absence and A's FORWARD write
  creating X/v with no predecessor and ACTIVE lifecycle; (b) FORWARD delivery of X/v to
  B≠A before A successfully completes; (c) a later RECOVERY write by A persisting X/c as
  DELETED, with exact predecessor X/v; (d) execution records proving that this action
  belongs to explicit compensation of the same producing step/occurrence. The order is
  confirmed creation < delivery < confirmed deletion. A's failure, checkpoint execution,
  or lock release alone is insufficient.
- **FR-4 — No harm prerequisite.** B may make no writes, finish before compensation, or
  subsequently fail/compensate. Do not erase an already proven exposure because of B's
  final outcome, later recreation, or legitimate historical use. The observation
  describes an occurrence, not a forbidden state at the final horizon.
- **FR-5 — Negatives and gaps.** Apply the matrix below. Preserve facts and a reason for
  each unknown. Do not infer physical deletion from a missing row or a compensation
  relationship merely from the same Saga. Chains containing an intermediate revision
  are outside this first proof; never select an arbitrary writer.
- **FR-6 — Coverage.** Declare covered contracts, observed calls, failures without
  delivery, commands without adapters, and excluded paths. Initial coverage includes
  singular outer Quiz/Tournament lookups and dummyapp fixtures. Internal reads, lists,
  predicates, nested references, in-memory reuse without a call, and event consumers
  have no global coverage. No events does not imply no reads.
- **FR-7 — Isolation.** Exclude setup, observers, and probes. Missing/mismatched attribution
  cannot become B. Contain collection failures without changing application outcomes,
  retries, locks, transactions, or DTOs. Gaps specific to this diagnostic must not degrade
  the coverage or scores of the three ImpactV2 checks.
- **FR-8 — Persistence and reproducibility.** Persist a separate sidecar with auditable
  evidence/joins, stable ordering, and IDs derived from attempt/occurrences. Repeated
  exposures of the same A/B pair and compensated revision produce one finding referring
  to every delivery; different readers remain distinguishable.
- **FR-9 — Proof.** Qualify the predicate and negatives in Spock, dummyapp first; prove
  direct/serialized delivery in the framework and the Quizzes chain in a controlled
  harness, including a reader that persists no effects. Separately demonstrate ordinary
  executor persistence, observer on/off equivalence, and cost.

| Situation | Verdict |
| --- | --- |
| A creates X/v → B receives X/v → compensation of the creating step deletes X/v | `OBSERVED` |
| Same sequence; B only reads and finishes without writing | `OBSERVED` |
| A finishes successfully; X changes normally afterward | `NOT_OBSERVED`, with sufficient evidence |
| B receives another revision, demonstrably not produced by A | No finding attributed to A |
| Deletion precedes the call; B receives an error | No delivery, no finding |
| A fails; compensation only releases a lock or deletes another object | `NOT_OBSERVED`, if evidence establishes this |
| Only A's failure/compensation is known; writes/revisions are missing | `UNKNOWN` |
| Restored update, intermediate revision, missing/ambiguous provenance | `UNKNOWN` / coverage reason, never a global zero |
| Observer or setup query | Excluded from the reader population |

## 5. Architecture

The framework collects typed deliveries and reuses confirmed-write evidence; the
verifier deterministically joins actions/recovery and writes the diagnostic. Adapters
belong to application diagnostic integration and use the same generic contract. Do not
introduce Quizzes classes into the verifier or public DTO telemetry fields. Preserve
visitor → analysis state → adapter → scenario → dynamic boundaries.
**(assumption)** Proof covers the synchronous local runtime and data revisions respecting
framework versioning; direct writes outside those boundaries do not become covered merely
because a return hook is installed.

## 6. Data model

Sidecar: `<execution>.saga-read-exposure.json`, schema
`microservices-simulator.saga-read-exposure.v1`. Preserve manifest and execution-report
references/hashes, attempt/workload/scenario IDs, scope/contracts, deliveries, necessary
writes and baseline/checkpoint sources, findings, and gaps. Runtime type disambiguates
the current persistent identity; row IDs and timestamps are not causal keys.

Separate execution validity, collection coverage (`COMPLETE_WITHIN_SCOPE`, `PARTIAL`,
`UNAVAILABLE`), and individual verdicts. Interrupted prefixes may retain an already
proven `OBSERVED` but cannot support complete absence. `observedExposureCount` counts
diagnostic findings, a lower bound when partial; it is null when no usable measurement
exists. Never serialize `impactScore` or add to ImpactV2. Each sidecar is self-contained
for its proofs and explicitly reports unavailable evidence dependencies.

## 7. Security model

Application code and typed adapters are the provenance trust boundary. Do not evaluate
text, invoke mutations, or scan arbitrary getters as an oracle. Store only identity,
revision, and necessary facts; do not duplicate personal data from DTO payloads.

## 8. Operating

Opt-in: `microservices.simulator.saga-read-exposure.enabled=true`, disabled by default,
with no new sidecar when disabled. When enabled, reuse existing write collection; if
that collection is unavailable/disabled, report `UNAVAILABLE` without implicitly
activating it. Disabling restores current cost; no data migration. Setup/preflight
must not fabricate measured reads.

## 9. Future roadmap

An update extension needs a contract for the restored projection/effect and its
relationship to the returned result, plus concurrent-write and partial-compensation
controls. Normal generation of the pair consuming an ID produced during measurement
is separate work.

## 10. Open decisions

None blocking the approved slice. The user selected creation followed by deletion with
explicit adapters; including updates/general provenance would reopen the effect and
instrumentation contract. See the two examples in DIAGNOSIS and the approved [PLAN](PLAN.md).

## Implementation evidence

M0 and M1 are committed and independently reviewed; M2 runtime qualification passed the
fixed 22-run matrix. [RESULTS.md](RESULTS.md) records the actual coverage, ordinary/control
separation, on/off equivalence, cost limitations and retained proof. No approved product
behavior or non-goal changed during implementation.
