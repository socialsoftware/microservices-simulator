# Remediation 001 - Shared Reader Checksum Verification

Parent spec: `../spec.md`
Independent review follow-up: `../independent-review-follow-up.md`
Depends on: none
Risk: `medium`
Status: `review-complete`

## Finding

The v3 manifest records a SHA-256 checksum for each linked package artifact, but `ScenarioCatalogPackageReader` does not compare those values with the artifact bytes it accepts. `ScenarioCatalogReader`, and therefore normal executor package loading, delegates to this shared reader. The on-demand mutation service performs a separate later checksum check, so package integrity differs by entry point.

## Required Behavior

- `ScenarioCatalogPackageReader` is the shared checksum-enforcement boundary for all four linked artifacts:
  - WorkloadPlan catalog;
  - FaultScenario catalog;
  - scenario-space accounting;
  - rejected-input diagnostic.
- Reject a missing or malformed SHA-256 value.
- Reject an artifact whose actual SHA-256 value differs from its manifest value.
- Parse and validate the same bytes whose checksum was verified; do not hash one file read and parse a second file read.
- Preserve existing schema, path, exact-record-count, deterministic-id, and semantic validation.
- Preserve the on-demand service's package-root, linked-path, and symlink protections. Remove only checksum logic that becomes genuinely redundant through the shared reader.
- Do not add signatures, authentication, schema changes, or unrelated package refactoring.

## Concrete Example

If the manifest records checksum `AAA` for a ten-record FaultScenario catalog, replacing that artifact with a different internally valid ten-record catalog whose checksum is `BBB` must fail with a checksum-mismatch diagnostic before any scenario can be returned or selected.

## Implementation Guidance

- Keep the change inside the existing package reader/writer/export design.
- Reuse the existing SHA-256 representation: lowercase, 64-character hexadecimal text.
- Prefer one in-memory byte snapshot per artifact. The current reader already loads complete artifacts, so do not introduce a new streaming abstraction solely for this remediation.
- Error messages must identify the artifact kind and checksum problem without claiming cryptographic authenticity.

## Focused Tests

At minimum prove:

1. a valid package still loads;
2. a missing or malformed artifact checksum rejects;
3. changing each of the four linked artifacts without updating the manifest checksum rejects;
4. a semantically valid replacement with the same record count rejects;
5. the executor reader cannot return/select a scenario from a checksum-mismatched package;
6. the on-demand path still rejects invalid package boundaries and accepts valid packages through the shared reader.

## Verification

Run the narrowest relevant verifier tests, including at least:

```bash
cd verifiers
mvn -Dtest=ScenarioCatalogJsonlWriterSpec,OnDemandFaultScenarioServiceSpec,ScenarioExecutorSpec test
```

Also run:

```bash
git diff --check -- \
  verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export \
  verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export \
  verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor \
  issues/2026-07-19-compensation-aware-scenario-catalog
```

## Evidence to Record

Append completion evidence to this file with:

- implementation summary;
- exact files changed;
- tests added and what they prove;
- commands run and exact results;
- any deviation or remaining concern.

Do not commit, move this slice, edit existing review reports, or start another remediation item.

## Completion Evidence

Status: `implemented-awaiting-review`

### Implementation Summary

- Moved validation of every linked artifact's lowercase 64-character SHA-256 value into `ScenarioCatalogPackageReader`.
- The shared reader now reads each artifact once into a byte snapshot, verifies that snapshot, and parses/validates that same snapshot.
- Removed the on-demand service's redundant second checksum read while retaining its manifest/package-root, linked-path consistency/containment, and symlink checks.
- Added package-reader, executor, and on-demand regressions for malformed metadata, all four artifact mismatch cases, a semantically valid same-record-count replacement, pre-selection executor rejection, and mutation-free on-demand rejection.

### Files Changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogPackageReader.java` — enforce and snapshot all four linked artifact checksums at the shared read boundary.
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioService.java` — remove only the now-redundant second checksum pass.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogJsonlWriterSpec.groovy` — package-reader checksum matrix, valid replacement regression, and hash refreshes for tests intentionally reaching semantic validation.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioServiceSpec.groovy` — shared-reader mismatch rejection with byte-unchanged mutable artifacts.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy` — mismatch rejection before scenario selection/materialization/execution.
- `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/001-shared-reader-checksum-verification.md` — this completion evidence only.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=ScenarioCatalogJsonlWriterSpec,OnDemandFaultScenarioServiceSpec,ScenarioExecutorSpec test` (red phase) | FAIL (expected) | 113 tests ran; 9 checksum regressions failed because the shared reader accepted mismatched/malformed checksum cases before the fix. The on-demand regression already passed through its then-local check. |
| `cd verifiers && mvn -Dtest=ScenarioCatalogJsonlWriterSpec,OnDemandFaultScenarioServiceSpec,ScenarioExecutorSpec test` (final) | PASS | 114 tests run, 0 failures, 0 errors, 0 skipped; Maven `BUILD SUCCESS`. Counts: package-reader/writer spec 20, on-demand spec 37, executor spec 57. |
| Required targeted `git diff --check -- ...` | PASS | No whitespace errors in the tracked scoped diff; the separately checked appended completion-evidence section also has no trailing whitespace. |

### Acceptance Criteria Evidence

- Shared enforcement: all four manifest artifact kinds are format-checked and content-checked by `ScenarioCatalogPackageReader` before package contents are returned.
- Snapshot integrity: each artifact is loaded once into `ArtifactSnapshot`; checksum computation and JSON/JSONL parsing consume its `byte[]` rather than performing separate file reads.
- Existing validation: focused tests continue to reach schema, dangling-reference, input-fingerprint, ownership, record-count, and semantic validation when their test manifests carry current hashes.
- Executor/on-demand: the executor cannot select or execute from a mismatched FaultScenario artifact; on-demand returns `INVALID_PACKAGE` without mutating package artifacts.
- Boundary preservation: existing outside-path and symlink regressions remain in the passing 37-test on-demand suite; containment, path-consistency, and symlink code remains present.

### Browser / Manual Evidence

- Not required.

### TDD Notes

- Added the package-reader and executor checksum regressions first and observed 9 expected failures: malformed/missing checksums, all four linked artifact byte mismatches, and executor acceptance before the production fix.
- Added the explicit semantically valid same-count FaultScenario replacement regression after the initial green run, then reran the complete focused command successfully.

### Deviations From Plan

- No implementation deviation. One preliminary Maven invocation was accidentally run from the repository root and failed immediately because this repository has no root POM; all actual red/green verification was rerun from `verifiers/` as required.

### Blockers / Follow-Ups

- No implementation follow-up. OpenTelemetry emitted a non-test-failing localhost collector connection warning during focused execution; Maven still reported the exact passing counts above. The original untracked remediation card contains four pre-existing Markdown hard-break lines with trailing spaces; the appended evidence does not.

## Review Attempt 01 Fix Evidence

Status: `implemented-awaiting-review`

### Fix Summary

- Replaced lossy JSONL `new String(..., UTF_8)` decoding with a UTF-8 `CharsetDecoder` configured with `CodingErrorAction.REPORT` for malformed and unmappable input.
- Kept the prior `String.lines()` splitting and blank-line/count handling after successful decoding, preserving valid UTF-8 behavior for line terminators, blank lines, trailing terminators, and empty files.
- Continued decoding and parsing the retained `ArtifactSnapshot.byte[]` used for SHA-256 verification; no artifact reread was introduced.
- Added an artifact-kind/path diagnostic for malformed UTF-8 and a regression whose manifest checksum is refreshed after inserting raw invalid byte `0x80` into otherwise acceptable WorkloadPlan JSONL.

### Files Changed for Review Fix

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogPackageReader.java` — strict snapshot decoding and artifact-identifying malformed-encoding diagnostic.
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogJsonlWriterSpec.groovy` — hash-consistent malformed UTF-8 regression.
- `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/001-shared-reader-checksum-verification.md` — review-attempt-01 fix evidence.

### Verification

| Command / Method | Result | Evidence |
|------------------|--------|----------|
| `cd verifiers && mvn -Dtest=ScenarioCatalogJsonlWriterSpec test` (red phase) | FAIL (expected) | 21 tests run, 1 failure, 0 errors, 0 skipped; the checksum-matching malformed UTF-8 package was accepted before strict decoding. |
| `cd verifiers && mvn -Dtest=ScenarioCatalogJsonlWriterSpec,OnDemandFaultScenarioServiceSpec,ScenarioExecutorSpec test` | PASS | 115 tests run, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS`. Counts: package reader/writer 21, on-demand 37, executor 57. |
| Required scoped `git diff --check -- ...` | PASS | No scoped tracked-diff whitespace errors. |

### Review Finding Evidence

- A checksum-consistent raw `0x80` byte now reaches strict decoding, fails with `Malformed UTF-8 in package artifact WORKLOAD_CATALOG <path>`, and is not reported as a checksum mismatch.
- The strict decoder consumes `ArtifactSnapshot.bytes()` directly. The only artifact file read remains the snapshot creation in `readArtifact`.
- Existing valid-package, blank-line mismatch, semantic/count, executor, and on-demand regressions remain green in the 115-test focused run.

### Deviations / Remaining Concerns

- No deviation from the requested correction and no implementation concern. The same non-failing OpenTelemetry localhost collector warning appeared during the focused suite. The original four Markdown hard-break lines remain untouched.

## Review Outcome

- Attempt 01: `FAIL` — strict UTF-8 decoding was missing. See `../remediation-review/001-shared-reader-checksum-verification-review-01.md`.
- Attempt 02: `PASS` — the finding is resolved and no blocking, major, minor, or note finding remains. See `../remediation-review/001-shared-reader-checksum-verification-review-02.md`.
- Fresh reviewer verification: 115 tests passed with 0 failures, 0 errors, and 0 skipped.
- Independent checksum-current malformed UTF-8 probe: strict artifact-identifying rejection confirmed.
- Final state: remediation item 1 is review-complete; no commit has been made.
