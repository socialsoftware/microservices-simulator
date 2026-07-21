# Remediation Review: 001 - Shared Reader Checksum Verification

## Review Attempt

Attempt: `02`

Latest copy updated: `yes`

## Verdict

`PASS`

Moved to done: `yes` (caller transition after PASS)

## Sources Reviewed

- Remediation slice and appended attempt-01 fix evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/001-shared-reader-checksum-verification.md`
- Parent contract: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Independent follow-up: `issues/2026-07-19-compensation-aware-scenario-catalog/independent-review-follow-up.md`
- Prior review: `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-review/001-shared-reader-checksum-verification-review-01.md`
- Changed production files:
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogPackageReader.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioService.java`
- Changed test files:
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogJsonlWriterSpec.groovy`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioServiceSpec.groovy`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`
- Relevant unchanged anchors rechecked: `ScenarioCatalogJsonlWriter.java`, `ScenarioCatalogReader.java`, and the executor package-loading entry point in `ScenarioExecutor.java`
- Actual diff reviewed: complete base-to-worktree production/test diff, including the strict-decoding fix and checksum-current malformed-byte regression.

## Summary

Attempt 01's major finding is resolved. JSONL artifacts are now strictly decoded from the same retained `byte[]` snapshot used for SHA-256 verification, with malformed and unmappable input configured to `REPORT`. The resulting diagnostic identifies the artifact kind and path. The new regression refreshes the manifest checksum after inserting raw invalid byte `0x80`, proving encoding rejection rather than checksum mismatch.

The complete remediation contract also passes re-review: all four linked artifacts are format- and content-checked at the shared reader, normal schema/count/id/semantic validation remains active, executor loading rejects mismatched content before selection or execution, and on-demand root/path/containment/symlink protections remain unchanged. Fresh focused verification passes 115 tests. No blocking, major, minor, or note finding remains.

## Attempt 01 Resolution

| Prior Finding | Verdict | Evidence |
|---------------|---------|----------|
| Lossy JSONL UTF-8 decoding could accept checksum-current malformed bytes | resolved | `ScenarioCatalogPackageReader.java:264-275` uses a fresh UTF-8 decoder with both malformed and unmappable input set to `CodingErrorAction.REPORT`; it decodes `ArtifactSnapshot.bytes()` and reports kind plus path. |
| Required checksum-current malformed-byte regression was absent | resolved | `ScenarioCatalogJsonlWriterSpec.groovy:177-201` inserts raw `0x80`, refreshes the workload checksum, and asserts malformed-UTF-8 rejection with `WORKLOAD_CATALOG`, path, and no checksum-mismatch text. |
| Independent probe previously accepted the malformed package | resolved | The same probe package has matching expected/actual SHA-256 `514209364c606b6f1219838ddb5c78c3074676e70f90279009698266562a369d` and contains raw `0x80`; current code rejects with `Malformed UTF-8 in package artifact WORKLOAD_CATALOG <path>` caused by `MalformedInputException`, exit 1. |

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Shared enforcement for all four artifacts | pass | `ScenarioCatalogPackageReader.java:51-63,168-188,224-238`; mismatch matrix at `ScenarioCatalogJsonlWriterSpec.groovy:155-175` covers WorkloadPlan, FaultScenario, accounting, and rejected-input artifacts. |
| Lowercase 64-character SHA-256 format | pass | `ScenarioCatalogPackageReader.java:184-187`; missing, uppercase, and wrong-length cases at `ScenarioCatalogJsonlWriterSpec.groovy:127-153`. |
| Checksum mismatch rejection | pass | `ScenarioCatalogPackageReader.java:228-234`; all-four mismatch matrix and same-count replacement at `ScenarioCatalogJsonlWriterSpec.groovy:155-175,203-222`. |
| Same verified byte snapshot for parsing | pass | `readArtifact` performs the sole linked-artifact file read, hashes the resulting bytes, and stores them at `ScenarioCatalogPackageReader.java:224-234,296-299`; JSONL decoding consumes `artifact.bytes()` at `:264-270`, and accounting parsing consumes `accountingArtifact.bytes()` at `:68`. |
| Strict malformed/unmappable UTF-8 rejection | pass | Decoder uses `REPORT` for both error classes at `ScenarioCatalogPackageReader.java:264-275`; fresh regression and independent checksum-current probe reject. |
| Artifact-identifying encoding diagnostic | pass | `ScenarioCatalogPackageReader.java:272-275` includes artifact kind and path; test asserts both at `ScenarioCatalogJsonlWriterSpec.groovy:195-200`. |
| Existing schema/count/id/semantic validation preserved | pass | Workload, FaultScenario, rejected diagnostic, accounting schema, exact-count, duplicate-id, ownership, fingerprint, and semantic checks remain at `ScenarioCatalogPackageReader.java:64-77,82-210`; focused suite passes. Tests that intentionally reach semantic validation refresh the relevant hash. |
| Executor rejects before selection/execution | pass | `ScenarioCatalogReader` still delegates directly to the shared reader; `ScenarioExecutorSpec.groovy:747-765` asserts mismatch, zero constructor calls, and no bodies. |
| On-demand path/root/symlink protections preserved | pass | Boundary validation, path consistency, containment, and segment-symlink checks remain at `OnDemandFaultScenarioService.java:529-611`; valid, outside-path, and symlink tests pass in the 37-test spec. |
| Redundant checksum removal is limited | pass | Only the post-shared-reader checksum reread was removed from `OnDemandFaultScenarioService`; no boundary or consistency validation was removed. |
| Scope/non-goals respected | pass | No signatures, authentication, schema change, dependency, unrelated refactor, or item-2 work appears in the diff. |
| Dependencies | pass | Slice declares `Depends on: none`. |

## Focused Behavior Review

| Required Behavior | Verdict | Evidence |
|-------------------|---------|----------|
| Valid package loads | pass | Existing package-reader happy path and high-precision round trip pass in `ScenarioCatalogJsonlWriterSpec`. |
| Missing/malformed checksums reject | pass | Generic metadata guard plus table at `ScenarioCatalogJsonlWriterSpec.groovy:127-153`. |
| Changed bytes in all four artifacts reject | pass | Table at `ScenarioCatalogJsonlWriterSpec.groovy:155-175`; appended blank lines are otherwise semantically/count-neutral, so rejection is checksum-specific. |
| Semantically valid same-count replacement rejects | pass | `ScenarioCatalogJsonlWriterSpec.groovy:203-222`. |
| Schema/count/id/semantic failures remain reachable with current checksums | pass | Dangling WorkloadPlan link, stale recipe fingerprint, malformed ownership, schema, duplicate, and count coverage remain green; modified semantic fixtures refresh checksums first. |
| Executor cannot select or run mismatched scenario | pass | `ScenarioExecutorSpec.groovy:747-765`. |
| On-demand rejects mismatch without mutation | pass | `OnDemandFaultScenarioServiceSpec.groovy:189-205`. |
| On-demand accepts valid package and rejects outside/symlink boundaries | pass | Existing valid persistence and boundary regressions pass in `OnDemandFaultScenarioServiceSpec`. |
| Empty/blank and valid line splitting behavior preserved | pass | Strict decoding still feeds `String.lines()` and existing blank-line skipping/count checks; all valid package and appended-blank-line tests pass. |

## Verification Evidence Check

| Command / Method | Verdict | Notes |
|------------------|---------|-------|
| `cd verifiers && mvn -Dtest=ScenarioCatalogJsonlWriterSpec,OnDemandFaultScenarioServiceSpec,ScenarioExecutorSpec test` | pass | Fresh `BUILD SUCCESS`; 115 tests, 0 failures, 0 errors, 0 skipped: executor 57, package reader/writer 21, on-demand 37. OpenTelemetry localhost export warnings were non-failing environment noise. |
| Independent checksum-current malformed UTF-8 probe | pass | Manifest and workload SHA-256 values matched exactly; raw `0x80` was present; reader rejected with artifact kind/path and `MalformedInputException`, not checksum mismatch. |
| `git diff --check` | pass | No tracked whitespace errors. |
| Required scoped `git diff --check -- ...` | pass | No output/errors for scoped production, tests, and issue package paths. |
| Conflict-marker and status/diff inspection | pass | No conflict markers; only the expected two production and three test files are changed. Existing untracked issue artifacts remain expected. The two meeting-note files were untouched. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | The fix adds only strict decoder configuration, artifact metadata in the snapshot, and one focused regression. |
| Existing patterns | pass | Reuses the shared writer SHA-256 representation and existing reader/export boundaries. |
| Test quality | pass | The malformed-byte test explicitly refreshes the checksum and excludes mismatch as the failure cause; mismatch tests use otherwise tolerated blank bytes; executor and on-demand tests assert side-effect/byte invariants. |
| Regression risk | pass | Valid-package, semantic, count, executor, and on-demand suites all pass after strict decoding. |
| Security/data safety | pass | Integrity checking and semantic decoding now operate faithfully on one snapshot; no authenticity claim was introduced. |
| Change hygiene | pass | No dependency, schema, unrelated production change, item-2 work, or meeting-note modification. |

## Findings

None.

## Done Transition

- Moved to done: `yes`, by the caller after the reviewer returned `PASS`.
- Done path: `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/001-shared-reader-checksum-verification.md`
- Reviewer constraint respected: the reviewer did not move the slice during attempt 02.

## Recommendation

Accept remediation item 1 as review-complete. Preserve the current uncommitted changes and review artifacts; the caller may perform any later workflow transition separately. Do not begin item 2 as part of this review.
