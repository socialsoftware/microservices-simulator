# Slice Review: 001 - Simulator In-Memory Fault Provider

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes`

## Sources Reviewed

- Slice: `issues/2026-07-07-scenario-executor-fault-vectors/001-simulator-in-memory-fault-provider.md`
- Spec: `issues/2026-07-07-scenario-executor-fault-vectors/spec.md`
- Implementation plan: `issues/2026-07-07-scenario-executor-fault-vectors/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`
- ADRs: None
- Completion evidence: `issues/2026-07-07-scenario-executor-fault-vectors/001-simulator-in-memory-fault-provider.md#completion-evidence`
- Changed files reviewed: `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/FaultVectorBoundaryContext.java`, `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/FaultVectorFault.java`, `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/FaultVectorFaultProvider.java`, `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/FaultVectorInjectedFaultException.java`, `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/FaultVectorProviderHolder.java`, `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/InMemoryFaultVectorProvider.java`, `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/ExecutionPlan.java`, `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/FaultVectorProviderTest.java`
- Prior review reports: None
- Commands run by reviewer: `git status --short`; `find issues/2026-07-07-scenario-executor-fault-vectors/review -maxdepth 1 -type f -print 2>/dev/null | sort || true && find issues/2026-07-07-scenario-executor-fault-vectors/done -maxdepth 1 -type f -print 2>/dev/null | sort || true`; `cd simulator && mvn -Dtest=FaultVectorProviderTest test`; `cd simulator && mvn -Dtest=FaultVectorProviderTest,ExecutionPlanDynamicEvidenceTest test`; `rg "verifiers" simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/ExecutionPlan.java || true`

## Summary

The slice satisfies the simulator-side provider contract. The implementation adds simulator-owned plain-value fault-vector records, an in-memory provider, scoped active-provider and boundary-context holders, a typed injected-fault `SimulatorException`, and `ExecutionPlan` integration that injects before the forward step body while suppressing legacy CSV behavior only when a provider is active. Targeted simulator verification passed, including dynamic-evidence regression coverage. No blocking findings remain.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | pass | `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults/*` implements provider/context/fault/scope/exception values; `ExecutionPlan` consults the provider at forward-step start through `injectFaultIfAssigned(...)`. |
| Slice out-of-scope respected | pass | No verifier-side vector parsing/report/CLI changes were introduced in this slice; no compensation-step fault injection or delay-vector support was added. |
| Spec non-goals respected | pass | Simulator code has no verifier imports; provider is binary fault-only and suppresses, rather than reuses, CSV impairment during active provider scopes. |
| Dependencies done | pass | Slice declares `Depends on: None`. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-5 | pass | `FaultVectorBoundaryContext`, `FaultVectorFault`, `FaultVectorFaultProvider`, `FaultVectorProviderHolder`, and `InMemoryFaultVectorProvider` live under `simulator/src/main/java/.../ms/faults` and use plain values. `rg "verifiers" ...` returned no output. | Simulator owns the provider API; no verifier model dependency. |
| AC-6 | pass | `FaultVectorInjectedFaultException` extends `SimulatorException` and carries execution id, plan id, saga id, scheduled step id, slot index, functionality names, runtime step, and assigned bit. `FaultVectorProviderTest.activeProviderInjectsTypedFaultBeforeStepBodyAndCarriesIdentity` asserts key identity fields and legacy CSV remains a plain `SimulatorException`. | Machine-distinguishable signal is present. |
| AC-7 | pass | `FaultVectorProviderHolder.install(...)` returns `Scope`, rejects concurrent active providers with `IllegalStateException`, `clear()` removes provider/context, and `enterBoundary(...)` returns a restorable boundary scope. `FaultVectorProviderTest.scopedProviderClearsStateAndRejectsConcurrentProviders` and `boundaryScopeClearsCurrentContextOnClose` pass. | First implementation's one-active-provider JVM rule is deterministic. |
| AC-8 | pass | `ExecutionPlan` loads `ImpairmentHandler` behavior when no provider is active; `FaultVectorProviderTest.legacyCsvFaultStillAppliesWhenNoProviderIsActive` passes and verifies legacy CSV throws the existing non-vector `SimulatorException`. | Broader Quizzes impairment smoke is deferred to later slices; simulator-local compatibility is enough for this slice. |
| AC-9 | pass | `ExecutionPlan` skips CSV load at construction when provider is active and `behaviourValues(...)` / `getTotalDelay()` return zero behavior while active. `FaultVectorProviderTest.activeProviderSuppressesCsvFaultsAndDelaysWhenItDoesNotInject` passes. | Active provider is authoritative for vector runs. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| `cd simulator && mvn -Dtest=FaultVectorProviderTest test` | pass | Reviewer rerun passed: 7 tests, 0 failures, 0 errors. |
| `cd simulator && mvn -Dtest=FaultVectorProviderTest,ExecutionPlanDynamicEvidenceTest test` | pass | Reviewer rerun passed: 15 tests, 0 failures, 0 errors. Confirms provider integration and existing dynamic-evidence behavior. |
| `rg "verifiers" simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/faults simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/coordination/ExecutionPlan.java || true` | pass | No output; simulator provider/integration code does not reference verifier packages. |
| Done-path collision check | pass | `find .../done` returned no existing `001-simulator-in-memory-fault-provider.md` before move. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Changes are limited to a small simulator `faults` package, `ExecutionPlan` integration, and focused tests. |
| Existing patterns | pass | The holder/scope approach matches existing simulator holder style; exception extends existing `SimulatorException`. |
| Test quality | pass | Tests exercise scope cleanup/rejection, boundary cleanup, typed signal identity, before-body timing, prefix-step timing, workflow abort/compensation legality, CSV compatibility, and CSV suppression. |
| Regression risk | pass | Targeted provider tests and `ExecutionPlanDynamicEvidenceTest` both pass. |
| Security/data safety | n/a | No persistence, migration, external IO, or dependency changes were introduced beyond temporary test CSV files. |
| Change hygiene | pass | No source edits outside the slice's simulator scope were needed; unrelated pre-existing worktree changes were observed but not modified by this review. |

## Findings

None

## Done Transition

- Moved to done: `yes`
- Done path: `issues/2026-07-07-scenario-executor-fault-vectors/done/001-simulator-in-memory-fault-provider.md`
- Reason if not moved: `None`

## Reviewer Notes

Targeted CSV compatibility was verified inside `FaultVectorProviderTest` rather than by running the broader Quizzes impairment suite. That is acceptable for S1 because the slice only changes simulator primitives and later slices own CLI/Docker/Quizzes smoke coverage.
