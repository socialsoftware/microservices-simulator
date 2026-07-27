# Trustworthy scenario materializability review

Authority:
- Intent: [`work.md`](./work.md)
- Detailed behavior: none
- Delivery coordination: none

Reviewer: `sp-reviewer/strong`, session `019fa4de-f79c-7a08-bc32-07a90d356374`
Reviewed: uncommitted diff on `fault-analysis/scenarios`
Verdict: `PASS`

## Review Boundary

Reviewed the complete approved setup-preflight and helper-fidelity package, focused/full verifier evidence, refreshed Quizzes package, preflight report, targeted execution reports, canonical docs, and package integrity. Batch all-zero qualification, domain validation, fixture reconstruction, event reconstruction, and distributed/TCC execution remain excluded by approved intent.

## Findings

### MAT-REV-001 — `NEEDS_FIX`

- Contract: Shared preflight must preserve ordinary executor setup semantics.
- Evidence: `ScenarioExecutor.instantiate` prefilters constructors by wrapper-class identity, rejecting Java reflection primitive widening and treating null as initially compatible with primitive parameters.
- Impact: Preflight and normal execution can falsely report `STARTUP_FAILED` for tuples reflection can construct or stop before trying a compatible overload.
- Required correction: Preserve Java invocation conversions, reject null for primitive parameters, continue overload search, retain precise final diagnostics, and cover widening/null overloads while keeping `BigInteger` to `Integer` incompatible.
- Disposition: `fixed`

### MAT-REV-002 — `NEEDS_FIX`

- Contract: Batch preflight must check the complete manifest-declared candidate set.
- Evidence: Candidate selection accepts an incomplete/duplicate materializability table; a manifest with no rows can return `SUCCESS` with zero candidates despite loaded workloads and stale counts.
- Impact: A malformed manifest can silently hide candidates.
- Required correction: Require exactly one unique materializability row per loaded WorkloadPlan, no extras, and candidate-count consistency before selection; add missing/duplicate-row tests.
- Disposition: `fixed`

### MAT-REV-003 — `NEEDS_FIX`

- Contract: Helper recipes must represent DTO state at the facade/Saga call.
- Evidence: Helper scanning accumulates all method mutations before resolving the nested facade call, including mutations occurring after that call.
- Impact: Persisted tuples can contain values absent when the Saga actually started.
- Required correction: Capture a point-in-time mutation snapshot at the facade boundary or conservatively reject unsupported ordering; cover exclusion of post-call mutations and isolation across helper calls.
- Disposition: `fixed`

### MAT-REV-004 — `NEEDS_FIX`

- Contract: Preflight mode must be unambiguous and must never silently execute workflow actions.
- Evidence: CLI and wrapper treat every value except exact lowercase `true` as false, so typos or `TRUE` can route to ordinary execution when a FaultScenario id is present.
- Impact: A malformed setup-only request can execute a scenario.
- Required correction: Strictly validate boolean mode values before startup/build work, reject preflight combined with a FaultScenario id, and cover CLI/wrapper validation.
- Disposition: `fixed`

## Verification Cycles

### Cycle 1

- Fix state reviewed: initial uncommitted implementation
- MAT-REV-001: `unresolved`
- MAT-REV-002: `unresolved`
- MAT-REV-003: `unresolved`
- MAT-REV-004: `unresolved`
- New same-class defect: none
- Verdict: `NEEDS_FIX`

### Cycle 2

- Fix state reviewed: final uncommitted diff after original outcome owners repaired findings
- MAT-REV-001: `resolved` — reflection again permits Java invocation conversions, skips null-to-primitive overloads, continues incompatible overload search, and preserves constructor-body failures; discriminating tests pass.
- MAT-REV-002: `resolved` — preflight rejects missing, duplicate, extra, null-id, and count-inconsistent manifest rows before setup; focused tests pass.
- MAT-REV-003: `resolved` — point-in-time facade snapshots exclude post-call mutations and isolate two helper calls with stable distinct ids; focused tests pass.
- MAT-REV-004: `resolved` — CLI and wrapper reject malformed/ambiguous modes before startup or build work; focused tests pass.
- New same-class defect: none
- Proof: reviewer independently reran 113 discriminating tests; parent focused 177 and full 573 test results reconcile; post-fix Quizzes preflight remains 80 setup-ready / two expected type failures at 49,069,530 ns.
- Verdict: `PASS`

## Residual Notes

The reviewer independently confirmed current Quizzes counts, stable preflight ordering, expected 80/2 setup split, helper-assignment counts, targeted execution outcomes, and manifest-linked artifact hashes. Saved artifacts do not contain a separate before/after hash-list file, but source flow, timestamps, package integrity, and package-equality tests make the no-mutation claim credible.
