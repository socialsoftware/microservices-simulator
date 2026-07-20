# 005 - Atomic On-Demand Multi-Fault Persistence

Parent spec: `./spec.md`  
Implementation plan: `./implementation-plan.md`  
Depends on: `004-materializable-eager-baseline-and-accounting.md`  
ACs covered: `AC-15, AC-16, AC-19, AC-20, AC-21, AC-22, AC-42, AC-44`  
Risk: `high`

## Purpose

Provide the explicit operator/search boundary for requesting one arbitrary vector, validating it without side effects, and persisting all bounded deterministic FaultScenarios as one consistent package revision before any execution can select them.

## Scope

- Add a verifier-owned on-demand request service and a simple operator entry point consistent with existing CLI/orchestrator conventions.
- Accept exactly one package/run, WorkloadPlan id, binary vector, and optional asserted effective cap.
- Read and validate the existing v3 package, WorkloadPlan reference, input readiness, structural admissibility, slot mapping, vector length/bits, and frozen cap before mutation.
- Generate requested multi-fault variants through S3’s generator and package semantics through S4.
- Deduplicate byte-equivalent existing ids; treat same-id/different-semantic-content as an integrity failure.
- Update `fault-scenario-catalog.jsonl`, manifest source/count metadata, and accounting per-vector rows as one staged, validated package revision.
- Serialize all replacement bytes first, validate the staged package, publish the manifest last, and restore/retain every prior artifact byte-for-byte on validation or write/promotion failure.
- Return structured diagnostics without invoking the executor.

## Out of Scope

- Batch requests, GA/search policy, scenario prioritization, or execution of every generated variant.
- Changing WorkloadPlans, compensation evidence, or the package’s frozen cap.
- Runtime materialization/startup claims.

## Repo Anchors

- V3 package reader/writer and structural validator from S2/S4.
- Recovery/FaultScenario generator from S3.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorCli.java` — simple existing option parsing style, not a reason to couple request generation to execution.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/` — package ownership boundary.
- Java NIO same-directory temp files and move/replace support — no new transaction library.

## Implementation Shape

- Keep request generation separate from `ScenarioExecutor`; success means records are persisted and selectable, not executed.
- Acquire a package-local mutation guard appropriate to this single-process tool so two requests cannot interleave writes.
- Canonically merge/sort records rather than append in request arrival order.
- Snapshot original bytes/existence, stage complete replacements in the same filesystem, validate checksums/references/counts, then promote data/accounting before manifest. Roll back all originals if any promotion fails.
- Record vector source as on-demand in manifest/accounting without changing semantic FaultScenario content.
- Reject a cap mismatch even when the requested vector already exists; the package manifest remains authoritative.

## TDD / Test Shape

- First behavior to test: requesting a valid two-bit vector persists its bounded scenarios and updated manifest/accounting before the service returns success.
- Expected red failure: no package mutation/request API exists.
- Additional coverage: missing plan; non-binary/wrong length vector; malformed slot mapping; blocked input; structurally inadmissible workload; zero/negative/malformed asserted cap; cap mismatch; repeated request byte stability; duplicate semantic ids; injected same-id/different-content collision; injected failure at each stage/promotion boundary with all three original byte snapshots unchanged; successful canonical ordering independent of request order; no executor invocation.

## Just-in-Time Preflight Required

Before source edits, the executor must re-check:

- Final S4 manifest/accounting fields and canonical serializers.
- Existing path-containment/symlink checks in `ScenarioGeneratorApplication` for reuse at the package boundary.
- Filesystem behavior in the test environment for `ATOMIC_MOVE`; define tested fallback/rollback without weakening byte-unchanged failure semantics.
- The exact operator entry-point naming; behavior is fixed even if flags are selected at preflight.

## Verification

- Run targeted on-demand mutation service/CLI specs introduced during preflight — valid persistence, invalid matrix, dedup/collision, frozen cap, and failure-injection byte checks pass.
- Inspect a successful request package with the v3 package reader before attempting any executor call.

## Evidence to Record

- files changed
- commands run and outputs
- request/diagnostic examples
- before/after checksums for success, repeat, invalid, collision, and injected failure
- persisted vector and count rows
- ACs satisfied
- deviations from this plan

## Risks / Watchpoints

- Multiple fixed files do not become a transaction merely because each move is atomic. Stage everything, publish the manifest last, and test rollback explicitly.
- Do not mutate accounting/manifest in memory and write them before FaultScenario serialization is known valid.
- Do not expose an unpersisted generated object to the executor.
