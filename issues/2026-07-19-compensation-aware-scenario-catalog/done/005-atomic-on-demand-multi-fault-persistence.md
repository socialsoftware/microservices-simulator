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

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Added a verifier-owned on-demand request service and standalone `FaultScenarioRequestCli`. One request names one v3 manifest, WorkloadPlan id, binary vector, and optional asserted recovery cap; the service returns structured persisted/deduplicated/rejected/integrity/persistence results and never invokes `ScenarioExecutor`.
- Added pre-mutation package validation for v3 schemas, artifact checksums/counts/references, package-directory containment and symlink rejection, manifest materializability rows, fresh input readiness plus structural admissibility, exact eager-vector coverage, accounting consistency, requested vector shape, and the frozen positive cap.
- Reused `RecoveryScheduleGenerator` for arbitrary vectors, including multi-participant assigned faults. Canonical id-ordered merging deduplicates equivalent records and rejects same-id/different-content collisions.
- Extended manifest artifact metadata with SHA-256 checksums and extended manifest/accounting source and count metadata to distinguish eager and `ON_DEMAND_REQUEST` vectors without changing `FaultScenario` semantic content.
- Added a package-local in-process mutation guard. Complete FaultScenario, accounting, and manifest bytes are serialized and staged before publication; a staged package is read and validated, data/accounting are promoted before the manifest, `ATOMIC_MOVE` has a tested replace fallback, and any stage/promotion/final-validation failure restores all three original byte snapshots.

### Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioService.java` — request validation, generation/deduplication, package consistency checks, staged publication, fallback, and rollback.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/{OnDemandFaultScenarioRequest,OnDemandFaultScenarioResult}.java` — request and structured diagnostic/result contracts.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/FaultScenarioRequestCli.java` — simple operator entry point with `--manifest-path`, `--workload-plan-id`, `--fault-vector`, and optional `--recovery-schedule-cap`.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/accounting/ScenarioSpaceAccountingReport.java` — eager/on-demand per-workload and aggregate vector counts plus canonical on-demand row insertion.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogJsonlWriter.java` — initial-package artifact checksums and zero on-demand/total computed-vector manifest counts.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/{FaultScenarioVectorSource,ScenarioCatalogManifest}.java` — `ON_DEMAND_REQUEST` and artifact SHA-256 metadata.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/EagerFaultScenarioGenerator.java` — exposes the existing shared materializability evaluation for request preflight.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioServiceSpec.groovy` — 23 dummyapp-style package/service/CLI cases covering success, invalid input, blocked/malformed packages, dedup/collision, all six transaction boundaries, canonical order, fallback, concurrency, and no executor path.
- `issues/2026-07-19-compensation-aware-scenario-catalog/005-atomic-on-demand-multi-fault-persistence.md` — completion evidence only.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec test` before implementation | EXPECTED FAIL | Groovy test compilation reported missing `OnDemandFaultScenarioRequest` and `OnDemandFaultScenarioService`, proving the requested mutation API did not exist. |
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec test` | PASS | Fresh focused run: `Tests run: 23, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec,ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec test` | PASS | Fresh final mutation plus S4 regression run: `Tests run: 100, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=RecoveryScheduleGeneratorSpec test` | PASS | `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`; existing bounded recovery and multi-fault semantics remain green. |
| Successful package inspection with `ScenarioCatalogPackageReader`, `jq`, and `sha256sum` | PASS | Persisted vector `0011` has exact uncapped/written counts `3/2`, two six-action FaultScenarios, cap `2`, one on-demand vector, six total computed vectors, and eight total FaultScenarios. Linked fault/accounting hashes match manifest metadata. |
| `git diff --check` | PASS | No whitespace errors. |

Checksum evidence from the focused fixtures (fault catalog, accounting, manifest respectively):

```text
initial eager package: b709a41793fc..., 2717705bb144..., a2fcab4261a6...
successful 0011 revision: f988774599fe..., 1f15b6c29c1a..., 883563275c5b...
repeat request: f988774599fe..., 1f15b6c29c1a..., 3bb8bcfb0f61... (same-fixture before/after snapshots equal)
invalid request: b709a41793fc..., 2717705bb144..., a2fcab4261a6... (same-fixture before/after snapshots equal)
collision: b709a41793fc..., 2717705bb144..., 6460ea59a6b2... (same-fixture before/after snapshots equal)
injected MANIFEST_PROMOTED failure: b709a41793fc..., 2717705bb144..., d412ead5a60f... (same-fixture before/after snapshots equal)
```

Manifest hashes differ between fixture directories because manifests contain absolute linked-artifact paths. Automated assertions compare all three mutable artifacts within each fixture. The six injected boundaries (`FAULT_SCENARIO_STAGED`, `ACCOUNTING_STAGED`, `MANIFEST_STAGED`, and all three corresponding promoted boundaries) each retained/restored exact original bytes.

Persisted row and request examples:

```text
workloadPlanId=3b76f66de56865c6d8cb3c358c5417fa89cf2208d37af288f9daf747059f149f
assignedVector=0011, vectorSource=ON_DEMAND_REQUEST, uncapped=3, written=2
FaultScenario ids:
  4d1231d67481c328ab60499bb16fdc109f2b46e9a858b4fe29f2bf7a388aa7b4
  e749bcbeb94b03fbdad28777a9e5fe0f6a315a8d9c53df23f47140fce13c36ef
success status=PERSISTED
repeat status=DEDUPLICATED
cap mismatch diagnostic=RECOVERY_CAP_MISMATCH
same-id/different-content diagnostic=FAULT_SCENARIO_ID_COLLISION
```

### Acceptance Criteria Evidence

- AC-15: Vector `0011` realizes faults in both still-live participants; every persisted bounded schedule contains compensation actions for participants `a` and `b`, while S3 masking semantics remain green.
- AC-16: Default package cap behavior remains covered by S4; explicit `0`, `-1`, and malformed asserted caps are rejected with `INVALID_ASSERTED_RECOVERY_CAP` before mutation.
- AC-19: Requests use manifest cap `2`; asserted cap `3` is rejected before generation/mutation even after the vector already exists.
- AC-20: A ready, structurally admissible four-slot plan persists two bounded multi-fault FaultScenarios, revised accounting, and manifest metadata; the v3 reader validates the final package before the service returns `PERSISTED`.
- AC-21: Missing plan, non-binary/wrong-length vector, malformed slot mapping, blocked input, symlink traversal, malformed cap, checksum/package inconsistency, and duplicate existing ids return structured rejection diagnostics with all three mutable bytes unchanged.
- AC-22: A repeated request returns `DEDUPLICATED` with exact byte stability; injected same-id/different-content generation returns `FAULT_SCENARIO_ID_COLLISION` without mutation. Opposite request orders produce byte-identical canonical FaultScenario and accounting artifacts.
- AC-42: The request path has no executor dependency or invocation. Success changes only FaultScenario JSONL, accounting, and manifest; workload and rejected-input artifacts remain byte-identical.
- AC-44: Dummyapp-first synthetic package coverage exercises valid multi-fault persistence, the invalid matrix, checksum/path validation, all transaction boundaries, atomic-move fallback, package-local serialization, CLI output, and v3 reader inspection.

### Browser / Manual Evidence

- Browser evidence was not required. Manual JSON/hash inspection and v3 package-reader evidence are recorded above.

### TDD Notes

- Added the valid two-participant `0011` persistence test first and observed the expected missing-API compile failure. Implemented the request/result/service path, then added invalid, dedup/collision, rollback-boundary, fallback, canonical-order, concurrency, and CLI cases before running the S4 regression suite.

### Deviations From Plan

- None.

### Blockers / Follow-Ups

- None. Ready for `sp-review-slice`; the slice remains active and was not moved to `done/`.

### Review Attempt 01 Fix Evidence

Status: `implemented-awaiting-review`

The three blocking attempt-01 findings are fixed without expanding slice scope:

- Existing/staged/final package validation now recomputes materializable and non-materializable WorkloadPlan totals from freshly evaluated package workloads. It requires both accounting aggregates and their manifest counts to match, and also validates the retained `rejectedInputsExported` manifest count against the linked diagnostic records. Hash-consistent false aggregate metadata is rejected before staging.
- Deduplication now compares the persisted requested-vector row's exact uncapped/written counts and complete sorted FaultScenario-id set with fresh deterministic `RecoveryScheduleGenerator` output. A mismatch returns `INTEGRITY_FAILURE` / `DETERMINISTIC_VECTOR_MISMATCH`; a valid but non-deterministically selected persisted id set also fails without mutation.
- Temporary-file deletion is now best-effort after a successful committed/finally validated revision, so cleanup failure cannot change `PERSISTED` into a false `PERSISTENCE_FAILED`. On a real publication failure, cleanup errors are attached as suppressed errors without masking the primary failure or rollback result.

#### Attempt 01 Fix Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioService.java`
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioServiceSpec.groovy`
- `issues/2026-07-19-compensation-aware-scenario-catalog/005-atomic-on-demand-multi-fault-persistence.md`

#### Attempt 01 TDD and Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec test` after adding the three regressions but before fixes | EXPECTED FAIL | Groovy compilation failed because the injectable `TemporaryFileCleaner` boundary did not exist. The reviewer had independently demonstrated the two count-integrity failures against the pre-fix service. |
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec test` | PASS | Fresh focused run: `Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. New cases cover accounting workload aggregates, each retained manifest aggregate count, fresh dedup exact counts, persisted id-set mismatch, and post-commit cleanup failure. |
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec,ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec test` | PASS | Fresh final run: `Tests run: 108, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=RecoveryScheduleGeneratorSpec test` | PASS | `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`; deterministic recovery generation remains green. |
| `git diff --check` | PASS | No whitespace errors. |

Attempt-01 AC evidence updates:

- AC-21: Hash-consistent false accounting values `materializableWorkloadPlans=999` / `nonMaterializableWorkloadPlans=888`, false corresponding manifest counts, and false `rejectedInputsExported` now return `REJECTED` / `INVALID_PACKAGE` with exact mutable-byte stability.
- AC-22: A persisted requested vector claiming uncapped count `999` while fresh generation returns `3` now returns `INTEGRITY_FAILURE` / `DETERMINISTIC_VECTOR_MISMATCH`; a valid alternate scenario-id set also returns integrity failure, and both leave all mutable artifacts byte-identical.
- AC-44: The focused suite now includes 31 cases and explicitly covers all three review findings, including successful final package readability after injected cleanup failure.

No spec, plan, review report, acceptance checkbox, dependent slice, executor behavior, or out-of-scope feature was edited. No blockers remain; ready for a new `sp-review-slice` attempt.

### Review Attempt 02 Fix Evidence

Status: `implemented-awaiting-review`

The two blocking attempt-02 findings are fixed without expanding slice scope:

- `readValidatedPackage` now parses only the requested manifest first, resolves all four linked paths, and rejects package-directory escapes or any existing symlink segment before invoking `ScenarioCatalogPackageReader`, checksumming, or otherwise opening a linked artifact. Boundary-owned diagnostics name only the artifact kind; they cannot include parser output derived from an out-of-package or symlink target. The resolved path list is also compared with the reader result to detect path changes during the read.
- The in-process guard is now keyed by the canonical real package directory rather than the manifest filename. Distinct accepted manifest filenames in one package/run therefore share one mutation lock before package reads and writes. A serialized stale alias is rejected as `INVALID_PACKAGE` after the first revision rather than interleaving publication or returning `PERSISTENCE_FAILED`.

#### Attempt 02 Fix Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioService.java`
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioServiceSpec.groovy`
- `issues/2026-07-19-compensation-aware-scenario-catalog/005-atomic-on-demand-multi-fault-persistence.md`

#### Attempt 02 TDD and Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec test` before the fixes | EXPECTED FAIL | `Tests run: 33, Failures: 3`: both outside/symlink diagnostics disclosed their injected secret tokens, and concurrent manifest aliases were not serialized under one package identity. |
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec test` | PASS | Fresh focused run: `Tests run: 33, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec,ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec test` | PASS | Fresh final run: `Tests run: 110, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=RecoveryScheduleGeneratorSpec test` | PASS | `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`; deterministic recovery generation remains green. |
| `git diff --check` | PASS | No whitespace errors. |

Attempt-02 AC evidence updates:

- AC-21: Direct outside-package and in-package-symlink targets contain invalid secret-token text; both requests return `REJECTED` / `INVALID_PACKAGE`, no diagnostic contains the token, and every mutable package byte remains unchanged. This proves containment/symlink rejection precedes linked target parsing.
- AC-20/AC-44: Two initially accepted manifest filenames linked to the same mutable package artifacts. The first request persists successfully while the second waits on the same canonical package-directory lock, then rejects its stale alias cleanly; measured generation concurrency remains `1` and neither request fails from interleaved publication.

No spec, plan, review report, acceptance checkbox, dependent slice, executor behavior, or unrelated file was edited. No blockers remain; ready for another `sp-review-slice` attempt.

### Review Attempt 03 Fix Evidence

Status: `implemented-awaiting-review`

The single blocking attempt-03 finding is fixed without expanding slice scope:

- Pre-mutation, staged, and final package validation now regenerates every accounting `perComputedVectorRecoverySpace` row with `RecoveryScheduleGenerator` using its referenced WorkloadPlan, assigned vector, and the manifest's frozen recovery cap. Each carried eager or on-demand row must match the fresh exact uncapped count, written count, and complete sorted FaultScenario id set before the package is accepted. This package-wide check is independent of the vector currently requested and of the request service's injectable generation source.
- Hash/checksum-consistent false metadata or an alternate persisted id set for vector A is therefore rejected as `REJECTED` / `INVALID_PACKAGE` before a distinct vector B can be generated or any mutable artifact can change.

#### Attempt 03 Fix Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioService.java`
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioServiceSpec.groovy`
- `issues/2026-07-19-compensation-aware-scenario-catalog/005-atomic-on-demand-multi-fault-persistence.md`

#### Attempt 03 TDD and Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec test` before the fix | EXPECTED FAIL | `Tests run: 35, Failures: 3`: false exact metadata for a carried eager vector, false exact metadata for a carried on-demand vector, and a carried alternate scenario-id set were all accepted while requesting a distinct vector. |
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec test` | PASS | Fresh focused run: `Tests run: 35, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec,ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec test` | PASS | Fresh final run: `Tests run: 112, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=RecoveryScheduleGeneratorSpec test` | PASS | `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`; deterministic recovery generation remains green. |
| `git diff --check` | PASS | No whitespace errors. |

Attempt-03 AC evidence updates:

- AC-20/AC-21: A package with aggregate- and checksum-consistent false exact metadata for carried vector A now rejects before servicing valid distinct vector B; FaultScenario JSONL, accounting, and manifest remain byte-identical. Both eager and retained on-demand source rows are covered.
- AC-44: The focused suite now proves package-wide deterministic exact-count and complete-id-set reconciliation rather than only direct requested-vector dedup reconciliation.

No spec, plan, review report, acceptance checkbox, dependent slice, executor behavior, or unrelated file was edited. No blockers remain; ready for another `sp-review-slice` attempt.

### Review Attempt 04 Fix Evidence

Status: `implemented-awaiting-review`

The single blocking attempt-04 finding is fixed without expanding slice scope:

- Existing, staged, and final package validation now retains each freshly evaluated `WorkloadMaterializability` result and requires every computed-vector accounting row—and therefore its matching FaultScenario set—to belong to a materializable WorkloadPlan. This applies equally to eager and `ON_DEMAND_REQUEST` rows before deterministic regeneration or request-specific processing.
- A count-, checksum-, and deterministic-generation-consistent carried on-demand vector for a non-ready workload is now rejected as `REJECTED` / `INVALID_PACKAGE` before a valid request for another ready workload can generate, stage, or mutate artifacts.

#### Attempt 04 Fix Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioService.java`
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioServiceSpec.groovy`
- `issues/2026-07-19-compensation-aware-scenario-catalog/005-atomic-on-demand-multi-fault-persistence.md`

#### Attempt 04 TDD and Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec test` before the fix | EXPECTED FAIL | `Tests run: 36, Failures: 1`: a package containing one ready and one freshly non-materializable workload accepted an exact/hash-consistent carried on-demand vector for the blocked workload and returned `PERSISTED` while requesting the ready workload. |
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec test` | PASS | Fresh focused run: `Tests run: 36, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec,ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec test` | PASS | Fresh final run: `Tests run: 113, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| `cd verifiers && mvn -Dtest=RecoveryScheduleGeneratorSpec test` | PASS | `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`; deterministic recovery generation remains green. |
| `git diff --check` | PASS | No whitespace errors. |

Attempt-04 AC evidence updates:

- AC-20/AC-21: A package with a carried on-demand vector owned by a non-ready WorkloadPlan now rejects before servicing a separate valid ready-workload request; FaultScenario JSONL, accounting, and manifest remain byte-identical.
- AC-44: The focused two-workload regression constructs deterministic scenarios and exact accounting for the blocked owner, updates all relevant package counts and hashes consistently, and proves that package-wide materializability—not incidental metadata inconsistency—causes rejection.

No spec, plan, review report, acceptance checkbox, dependent slice, executor behavior, or unrelated file was edited. No blockers remain; ready for another `sp-review-slice` attempt.
