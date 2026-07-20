# Slice Review: 005 - Atomic On-Demand Multi-Fault Persistence

## Review Attempt

Attempt: `02`

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
- Completion evidence: `005-atomic-on-demand-multi-fault-persistence.md` → `## Completion Evidence`, including `### Review Attempt 01 Fix Evidence`
- Dependency evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/done/004-materializable-eager-baseline-and-accounting.md` and `issues/2026-07-19-compensation-aware-scenario-catalog/review/004-materializable-eager-baseline-and-accounting-review-02.md`
- Changed files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/EagerFaultScenarioGenerator.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/accounting/ScenarioSpaceAccountingReport.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/{FaultScenarioRequestCli,OnDemandFaultScenarioRequest,OnDemandFaultScenarioResult,OnDemandFaultScenarioService,ScenarioCatalogJsonlWriter}.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/{FaultScenarioVectorSource,ScenarioCatalogManifest}.java`; `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioServiceSpec.groovy`
- Relevant existing files reviewed: `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogPackageReader.java`; `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/{FaultScenarioValidator,RecoveryScheduleCap,RecoveryScheduleGenerator}.java`; relevant scenario model records; `ScenarioGeneratorApplication` package-path guards
- Prior review reports: `issues/2026-07-19-compensation-aware-scenario-catalog/review/005-atomic-on-demand-multi-fault-persistence-review-01.md`
- Commands run by reviewer:
  - `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec test`
  - `cd verifiers && mvn -Dtest=OnDemandFaultScenarioServiceSpec,ScenarioSpaceAccountingCalculatorSpec,ScenarioCatalogJsonlWriterSpec,ScenarioGeneratorApplicationSpec,DummyappAccountingFixtureFoundationSpec test`
  - `cd verifiers && mvn -Dtest=RecoveryScheduleGeneratorSpec test`
  - `git diff --check`
  - worktree, dependency, changed-file, source-reference, prior-review, and done-path collision inspection
  - isolated package-path probe using `FaultScenarioRequestCli` against a copied valid manifest whose FaultScenario link pointed outside the package to `/tmp/s5-outside-secret.txt`
  - isolated single-process concurrency probe compiled under `/tmp`, using two accepted manifest files in one package directory that linked the same mutable artifacts

## Summary

The three attempt-01 blockers are fixed. Current validation recomputes workload aggregate counts and retained manifest package counts, dedup reconciles exact fresh counts and the generated scenario-id set, and post-commit temporary cleanup is best-effort. Fresh reviewer runs pass the focused 31-test suite, the 108-test S5/S4 regression suite, and the 14-test recovery suite.

The slice still cannot pass because two package-boundary guarantees are not actually enforced. Linked artifact paths are dereferenced and parsed before containment and symlink checks run; an out-of-package file token was returned in the structured diagnostic, proving unintended external-file access and disclosure. Separately, the mutation guard is keyed by manifest filename rather than the package/run, so two accepted manifest paths in one package directory can enter mutation concurrently against the same FaultScenario and accounting files. The isolated probe made both otherwise valid requests fail during publication because their writes interleaved. These violate the required package boundary and package-local serialization semantics.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Slice scope | fail | Normal request persistence is implemented, but linked artifacts are opened before package containment validation and the lock does not cover all requests targeting the same package/run artifacts. |
| Slice out-of-scope respected | pass | No batch policy, GA/search, prioritization, execution, WorkloadPlan mutation, cap mutation, or runtime materialization/startup claim was introduced. |
| Spec non-goals respected | pass | No eager `2^n` expansion, all-vector exact recovery total, search/scoring, compensation faults, distributed/TCC parity, or new dependency was added. |
| Dependencies done | pass | S4 is under `done/`; its latest review is attempt 02 with verdict `PASS`. |

## Acceptance Criteria Review

| AC | Verdict | Evidence | Notes |
|----|---------|----------|-------|
| AC-15 | pass | Valid `0011` generation persists scenarios in which assigned faults realize in both still-live participants; the S3 recovery suite remains green. | Runtime replay remains later-slice scope. |
| AC-16 | pass | The package cap is positive/frozen; asserted `0`, `-1`, and malformed values reject before staging. | |
| AC-19 | pass | The manifest cap is authoritative, and an asserted mismatch rejects before mutation even after persistence. | |
| AC-20 | pass | The normal valid request persists a bounded multi-fault vector and performs staged/final package validation before returning `PERSISTED`. | Package-path aliases expose a separate concurrency defect in the required mutation guard. |
| AC-21 | fail | Invalid linked paths are eventually reported as `INVALID_PACKAGE` without changing the three mutable package files, but `ScenarioCatalogPackageReader` opens/parses the linked target before containment/symlink rejection. The probe disclosed `SUPER_SECRET_TOKEN` from an out-of-package file in the diagnostic. | Package-boundary validation must precede linked artifact access, not merely package mutation. |
| AC-22 | pass | Repeat requests reconcile fresh exact uncapped/written counts and the complete deterministic id set; byte-equivalent records deduplicate, while collisions and deterministic mismatches leave package bytes unchanged. | Attempt-01 blocker is fixed. |
| AC-42 | pass | The request path remains separate from `ScenarioExecutor` and mutates only the three intended package artifacts on normal success. | Full execution immutability remains S6/S7 scope. |
| AC-44 | fail | The focused suite is broad and passes, but its symlink test only checks eventual rejection after dereference, and its concurrency test uses the identical manifest path, so neither uncovered the failing package-boundary cases reproduced by reviewer probes. | Add boundary-order and package-identity regressions. |

## Verification Evidence Check

| Required Evidence | Verdict | Notes |
|-------------------|---------|-------|
| Focused on-demand mutation/CLI spec | pass | Fresh reviewer run: `Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| On-demand plus S4 regression suite | pass | Fresh reviewer run: `Tests run: 108, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| Recovery generator regression | pass | Fresh reviewer run: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`; Maven `BUILD SUCCESS`. |
| Successful staged/final v3 package validation | pass | The service performs staged and final validation; focused success/fallback/cleanup tests reload the resulting package. |
| Attempt-01 aggregate/dedup/cleanup regressions | pass | Fresh focused run includes the false aggregate/manifest counts, exact count/id-set reconciliation, and cleanup-failure cases; all pass. |
| Package containment before linked artifact access | fail | A copied manifest linked `faultScenarioCatalog.path` to `/tmp/s5-outside-secret.txt`. CLI result was `REJECTED/INVALID_PACKAGE`, but its message included `Unrecognized token 'SUPER_SECRET_TOKEN'`, proving the external file was read and parsed before containment rejection. |
| Package-local mutation serialization | fail | Two initially valid manifests in one package directory linked the same catalogs. Concurrent requests for `1001` and `0110` both reached generation and returned `PERSISTENCE_FAILED` with manifest count/checksum race diagnostics instead of being serialized. |
| `git diff --check` | pass | No whitespace errors. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Changes remain confined to request contracts/CLI/service, checksums/source/count metadata, accounting extension, shared materializability evaluation, and focused tests. |
| Existing patterns | fail | `ScenarioGeneratorApplication` validates containment and symlink segments before touching a configured output target; the request path instead calls the full package reader before applying equivalent linked-path checks. |
| Test quality | fail | Behavioral tests cover normal persistence, invalid vectors, dedup/collision, six transaction boundaries, fallback, cleanup, ordering, and same-manifest concurrency, but the path test cannot prove no dereference and the guard test does not vary accepted manifest aliases for shared package artifacts. |
| Regression risk | fail | A crafted manifest can cause arbitrary local regular files to be read and parser details returned; aliased package requests can interleave publication and turn valid requests into persistence failures. |
| Security/data safety | fail | Out-of-package content crossed the package boundary into a diagnostic before containment rejection. The package lock also permits overlapping writes to one artifact set. |
| Change hygiene | pass | `git diff --check` passes; unrelated untracked meeting-note files were not modified. |

## Findings

| Severity | Finding | Evidence | Required Fix |
|----------|---------|----------|--------------|
| blocking | Linked artifacts are opened and parsed before package containment and symlink checks, so the claimed package boundary can read and disclose out-of-package file content. | `OnDemandFaultScenarioService.java:509-527` calls `ScenarioCatalogPackageReader.read` at line 516 and only afterward checks `startsWith(packageRoot)` and symlink segments. `ScenarioCatalogPackageReader.java:52-60` resolves and reads every linked artifact. The reviewer path probe pointed the FaultScenario link to `/tmp/s5-outside-secret.txt`; the returned `INVALID_PACKAGE` message included the file token `SUPER_SECRET_TOKEN`. | Parse only the manifest first, resolve every linked path, and reject escape/symlink cases before any linked artifact is opened, checksummed, or parsed. Keep invalid-path diagnostics boundary-owned rather than propagating content-derived parser messages. Add direct outside-path and symlink regressions that prove containment rejection occurs before target parsing and all mutable package bytes remain unchanged. |
| blocking | The in-process guard is manifest-file-local, not package-local, so accepted manifest aliases that share package artifacts can interleave writes. | `OnDemandFaultScenarioService.java:85-88` keys `PACKAGE_LOCKS` by normalized manifest path. The reviewer created a second initially valid manifest in the same package directory, linking the same FaultScenario/accounting files. Concurrent requests entered generation together and both returned `PERSISTENCE_FAILED` from count/checksum races. The existing test varies service instances but always supplies the identical manifest path. | Key serialization to a canonical package/run identity that covers the shared mutable artifacts before package reads and writes (for example, the canonical package directory under the enforced containment contract), or reject overlapping manifest aliases deterministically before mutation. Add a regression with two accepted manifest paths targeting one artifact set and prove requests cannot overlap publication; no valid request should fail because another in-process request interleaved. |

## Done Transition

- Moved to done: `no`
- Done path: `None`
- Reason if not moved: `FAIL` — two blocking package-boundary and mutation-serialization findings remain.

## Reviewer Notes

The attempt-01 aggregate-count, dedup-reconciliation, and cleanup findings are resolved. The new findings come from reviewing the whole slice boundary rather than only rerunning those regressions. The isolated probes touched `/tmp` only; unrelated worktree changes were preserved.
