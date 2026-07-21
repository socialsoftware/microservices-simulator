# Slice Review: 005 - Atomic On-Demand Multi-Fault Persistence

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`FAIL`

Moved to done: `no`

## Sources Reviewed

- Slice: `issues/2026-07-19-compensation-aware-scenario-catalog/005-atomic-on-demand-multi-fault-persistence.md`
- Spec: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Implementation plan: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/reference/scenario-executor.md`
- ADRs: `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`, `docs/verifiers-impl/decisions/2026-04-27-scenario-catalog-export-contract.md`, `docs/verifiers-impl/decisions/2026-07-08-multi-saga-executor-failure-policy.md`, `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md`, `docs/verifiers-impl/decisions/2026-07-08-scenario-execution-report-v3-participants.md`
- Completion evidence: `005-atomic-on-demand-multi-fault-persistence.md` → `## Completion Evidence`
- Dependency evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/done/004-materializable-eager-baseline-and-accounting.md` and `issues/2026-07-19-compensation-aware-scenario-catalog/review/004-materializable-eager-baseline-and-accounting-review-02.md`
- Changed files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/EagerFaultScenarioGenerator.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/accounting/ScenarioSpaceAccountingReport.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/{FaultScenarioRequestCli,OnDemandFaultScenarioRequest,OnDemandFaultScenarioResult,OnDemandFaultScenarioService,ScenarioCatalogJsonlWriter}.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/{FaultScenarioVectorSource,ScenarioCatalogManifest}.java`; `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioServiceSpec.groovy`
- Relevant existing files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogPackageReader.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/{FaultScenarioValidator,RecoveryScheduleCap,RecoveryScheduleGenerator}.java`; relevant scenario model records
- Prior review reports: `None`
- Commands run by reviewer:
  - `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec test`
  - `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec,ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec test`
  - `cd verifiers && mvn -Dtest=RecoveryScheduleGeneratorSpec test`
  - `git diff --check`
  - worktree, dependency, review-attempt, changed-file, source-reference, and done-path collision inspection
  - two isolated CLI integrity probes built from a copied generated package under `/tmp`: one with internally hash-consistent but false workload aggregate counts, and one with an internally sum/hash-consistent but false exact uncapped count for persisted vector `0011`

## Summary

The normal on-demand path is largely present: the service validates package paths and hashes, recomputes workload materializability, enforces the frozen cap, generates through the S3 generator, canonically merges scenarios and accounting rows, stages all three mutable artifacts, validates a staged package, publishes the manifest last, rolls back injected stage/promotion failures, serializes in-process requests, and exposes a separate structured CLI. The focused 23-test service suite, the 100-test mutation/S4 regression suite, and the 14-test recovery suite all pass.

The slice cannot pass because package-integrity validation is incomplete in two observable ways. False workload-level aggregate counts are accepted and carried into a successful new package revision. A repeated request also returns `DEDUPLICATED` even when the persisted vector's claimed exact uncapped count disagrees with the freshly generated exact count. In addition, an `IOException` while deleting temporary files after successful final validation escapes from `finally`, causing the service to report `PERSISTENCE_FAILED` after all revised artifacts have already been published, without rollback. These violate the slice's validated-consistent-revision and failure-semantics contract.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | fail | Valid generation/persistence works, but `readValidatedPackage` does not validate all package count metadata, dedup does not reconcile its accounting row with the fresh deterministic result, and post-commit cleanup can convert a committed revision into a reported failure without restoration. |
| Slice out-of-scope respected | pass | No batch request/search policy, scenario prioritization, execution, WorkloadPlan mutation, cap mutation, or runtime materialization/startup claim was added. |
| Spec non-goals respected | pass | No eager `2^n` expansion, exact all-vector recovery total, search/scoring, compensation faults, distributed/TCC parity, or new dependency was introduced. |
| Dependencies done | pass | S4 is under `done/`; its latest review is attempt 02 with verdict `PASS`. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-15 | pass | The valid `0011` service test persists generated scenarios in which assigned faults realize for both still-live participants; the S3 multi-fault regression suite remains green. | Runtime replay remains later-slice scope. |
| AC-16 | pass | Package cap use is mandatory and positive; asserted `0`, `-1`, and malformed text are rejected before staging, and the default/frozen package cap remains supplied by S4. | |
| AC-19 | pass | Requests use the manifest cap; an asserted mismatch is rejected before generation/mutation, including after the vector exists. | |
| AC-20 | pass | A valid four-slot, ready, structurally admissible workload persists bounded `0011` scenarios, accounting, and manifest metadata; final package validation occurs before normal `PERSISTED` return. | The cleanup finding affects failure/result atomicity rather than the demonstrated normal result. |
| AC-21 | fail | The listed request-shape/readiness/slot/path cases reject without mutable-byte changes, but an invalid package whose workload aggregate counts are `999/888` is accepted and mutated successfully. | Existing-package and staged-package count validation is part of this slice's pre-mutation contract. |
| AC-22 | fail | Byte-equivalent scenario records deduplicate and ID collisions reject, but dedup only checks that an accounting row exists. The manual probe returned `DEDUPLICATED` with generated `uncappedScheduleCount=3` while retaining persisted `uncappedUniqueScheduleCount=999`. | The package is left semantically inconsistent with the deterministic request result. |
| AC-42 | pass | The request path is separate from `ScenarioExecutor`; success mutates only FaultScenario JSONL, accounting, and manifest while workload/rejected artifacts remain unchanged. | Full execution immutability remains S6/S7-owned. |
| AC-44 | fail | Existing synthetic and dummyapp regression coverage is broad, but it misses the two accepted-inconsistent-package cases and post-commit cleanup failure semantics found in this review. | Add focused regressions before claiming the atomic/integrity matrix complete. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| Focused on-demand mutation/CLI spec | pass | Fresh reviewer run: `Tests run: 23, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| On-demand plus S4 regression suite | pass | Fresh reviewer run: `Tests run: 100, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| Recovery generator regression | pass | Fresh reviewer run: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| Successful package inspection through v3 reader | pass | The service spec reloads the successful package and checks the requested vector, scenarios, source/count metadata, and unchanged immutable artifacts. The service itself also performs final checksum/count/reference validation before normal success. |
| Invalid/failure mutable-byte snapshots | pass | Existing tests cover request validation, collision, all six declared stage/promotion injection boundaries, fallback replacement, and repeat stability. |
| Existing-package aggregate-count integrity probe | fail | After changing accounting and manifest materializable/non-materializable counts to `999/888` and refreshing the accounting SHA-256, request `1001` returned exit `0`, status `PERSISTED`, and retained both false aggregates. |
| Dedup exact-count integrity probe | fail | After changing vector `0011` uncapped count from `3` to `999`, adjusting aggregate/manifest sums, and refreshing SHA-256, the CLI returned exit `0`, status `DEDUPLICATED`, result count `3`, while the persisted row remained `999`. |
| Post-commit cleanup failure semantics | fail | Source inspection shows `Files.deleteIfExists` in `publishRevision`'s `finally` can throw after final validation has succeeded; that exception bypasses the rollback `catch` and is returned as `PACKAGE_REVISION_FAILED` after publication. |
| `git diff --check` | pass | No whitespace errors. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Changes are confined to request contracts/CLI/service, package hashes/source/count metadata, accounting extension, shared materializability access, and focused tests. |
| Existing patterns | pass | Uses Jackson, immutable records, S3 generation, S4 package reader/validators, NIO same-directory staging/moves, stable sorting, exact decimal counts, and Spock fixtures without a new dependency. |
| Test quality | fail | Happy path, invalid matrix, collision, rollback boundaries, fallback, ordering, concurrency, and CLI are meaningful, but exact count reconciliation, workload aggregate validation, and post-commit cleanup failure are untested. |
| Regression risk | fail | False metadata can survive a new validated revision, dedup can contradict persisted accounting, and a cleanup error can report failure after commit. These are central high-risk package mutation semantics. |
| Security/data safety | fail | Path/symlink/hash checks are good, but the service accepts an internally checksum-consistent package with false aggregate metadata and can violate failure byte-stability at the cleanup boundary. |
| Change hygiene | pass | `git diff --check` passes; unrelated untracked meeting-note files were not modified. |

## Findings

| Severity | Finding | Evidence | Required Fix |
|----------|---------|----------|--------------|
| blocking | Existing and staged package validation omits workload aggregate count consistency, so a false package can be accepted and republished as a supposedly valid revision. | `OnDemandFaultScenarioService.java:608-645` validates per-workload rows and `workloadPlansWritten`, but not `workloadCatalogSpace.materializableWorkloadPlans` / `nonMaterializableWorkloadPlans` or their corresponding manifest counts. `ScenarioSpaceAccountingReport.java:186-190` carries those values forward unchanged. In `/tmp/s5-review-aggregate-count`, hash-consistent values `999/888` were accepted; request `1001` returned `PERSISTED`, and the false values remained in both accounting and manifest. | Recompute and validate materializable/non-materializable aggregate counts from the validated workload rows/materializability facts, require the corresponding manifest counts to match, and reject before staging. Ensure staged/final validation applies the same invariant. Add a regression that refreshes hashes after tampering and proves structured failure plus byte-unchanged mutable artifacts. Audit the other retained manifest/package count fields under the same consistency rule rather than assuming their presence makes them valid. |
| blocking | Dedup reports success without proving the existing computed-vector row matches the freshly generated deterministic result. | `OnDemandFaultScenarioService.java:185-201` requires only that a row exists when `additions.isEmpty()`. `validateConsistency` at lines `555-560` checks only non-negativity/cap/record cardinality, not the exact generator result. In `/tmp/s5-review-dedup-count`, vector `0011` persisted `uncappedUniqueScheduleCount=999` with adjusted sums/hashes; the service returned `DEDUPLICATED` and generated count `3`, while leaving `999` in the package. | Before `DEDUPLICATED`, require the existing row's uncapped/written counts and the existing scenario-id set for that workload/vector to equal the fresh generation result; otherwise return an integrity failure with all bytes unchanged. Add a focused exact-count/ID-set tamper regression. If package-wide validation is intended to certify every exact computed row, apply equivalent deterministic reconciliation to all rows. |
| blocking | Temporary-file cleanup can make a completed publication return failure without rollback. | `OnDemandFaultScenarioService.java:299-306` promotes all artifacts and successfully validates the final package. Lines `319-322` then call throwing `Files.deleteIfExists` from `finally`, outside the rollback `catch` at lines `307-318`. An `IOException` there reaches `requestLocked`, which returns `PERSISTENCE_FAILED`, although the revised package remains published. | Make cleanup unable to flip a committed validated revision into a failure without restoration: perform best-effort cleanup that preserves the successful result, or keep strict cleanup inside transaction handling and rollback on its failure. Preserve/suppress cleanup errors without masking the primary failure. Add an injectable cleanup-failure regression proving the chosen result/byte contract. |

## Done Transition

- Moved to done: `no`
- Done path: `None`
- Reason if not moved: `FAIL` — three blocking package-integrity/atomic-result findings remain.

## Reviewer Notes

The current six failure-injection boundaries do prove restoration around the declared staging and promotion checkpoints. The cleanup finding is a separate post-final-validation boundary and is not covered by those tests. Unrelated untracked meeting-note files were left untouched.
