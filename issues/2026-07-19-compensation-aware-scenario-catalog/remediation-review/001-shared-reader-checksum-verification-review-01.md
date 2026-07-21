# Remediation Review: 001 - Shared Reader Checksum Verification

## Review Attempt

Attempt: `01`

Latest copy updated: `yes`

## Verdict

`FAIL`

Moved to done: `no`

## Sources Reviewed

- Remediation slice and completion evidence: `issues/2026-07-19-compensation-aware-scenario-catalog/remediation-done/001-shared-reader-checksum-verification.md`
- Parent spec: `issues/2026-07-19-compensation-aware-scenario-catalog/spec.md`
- Independent follow-up: `issues/2026-07-19-compensation-aware-scenario-catalog/independent-review-follow-up.md`
- Implementation plan and prior package QA: `issues/2026-07-19-compensation-aware-scenario-catalog/implementation-plan.md`, `issues/2026-07-19-compensation-aware-scenario-catalog/qa-report.md`
- Context docs: `CONTEXT-MAP.md`, `docs/verifiers-impl/glossary.md`, `docs/verifiers-impl/current-state.md`
- ADR: `docs/verifiers-impl/decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`
- Changed production files:
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogPackageReader.java`
  - `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioService.java`
- Changed test files:
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogJsonlWriterSpec.groovy`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/OnDemandFaultScenarioServiceSpec.groovy`
  - `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ScenarioExecutorSpec.groovy`
- Relevant unchanged anchors: `ScenarioCatalogJsonlWriter.java`, `ScenarioCatalogReader.java`, and the executor package-loading entry point in `ScenarioExecutor.java`
- Prior remediation review reports: `None`
- Actual diff reviewed: all five changed production/test files, including base-to-worktree source and test hunks; implementer summary was not treated as evidence.

## Summary

The implementation correctly centralizes lowercase 64-hex SHA-256 format and content checks for all four linked artifacts, snapshots each artifact once, rejects mismatches before the executor can select or execute a scenario, and preserves the on-demand root/path/symlink checks. The focused 114-test command and scoped diff hygiene pass.

The review nevertheless fails because the new JSONL snapshot parser performs lossy UTF-8 decoding. A hash-consistent artifact containing malformed UTF-8 can be accepted after Java silently replaces invalid bytes with `U+FFFD`. The previous `Files.readAllLines` path rejected the same bytes with `MalformedInputException`. This weakens the existing input-validation boundary and means parsed JSON is not a faithful decoding of the byte snapshot whose checksum was verified.

## Contract Compliance

| Requirement | Verdict | Evidence |
|-------------|---------|----------|
| Shared enforcement for all four artifacts | pass | `ScenarioCatalogPackageReader.java:48-60,165-185,221-234`; mismatch matrix at `ScenarioCatalogJsonlWriterSpec.groovy:155-175` covers workload, FaultScenario, accounting, and rejected-input artifacts. |
| Lowercase 64-character SHA-256 format | pass | `ScenarioCatalogPackageReader.java:181-184`; missing/uppercase/wrong-length cases at `ScenarioCatalogJsonlWriterSpec.groovy:127-153`. |
| Mismatch rejection | pass | `ScenarioCatalogPackageReader.java:225-230`; same-count semantic replacement at `ScenarioCatalogJsonlWriterSpec.groovy:177-196`. |
| Same snapshot for hash and parse | fail | `ScenarioCatalogPackageReader.java:225-231` retains one `byte[]`, but JSONL parsing at `:261-262` lossily transforms malformed bytes before parsing. The reviewer probe accepted a checksum-matching workload containing raw byte `0x80`. |
| Existing schema/count/id/semantic validation preserved | fail | Normal focused regressions pass and validators remain at `ScenarioCatalogPackageReader.java:79-163,188-206`; however malformed UTF-8 that the old `Files.readAllLines` rejected is now accepted, so the prior read-validation boundary is not preserved. |
| Executor rejects before selection/execution | pass | `ScenarioCatalogReader.java` delegates to the shared reader; executor regression at `ScenarioExecutorSpec.groovy:747-765` asserts checksum error, zero constructor calls, and zero bodies. |
| On-demand root/path/symlink protections preserved | pass | Pre-read boundary checks remain at `OnDemandFaultScenarioService.java:529-611`, including containment and symlink rejection; existing outside-path and symlink tests at `OnDemandFaultScenarioServiceSpec.groovy:207-250` pass. Only the later redundant checksum reread was removed. |
| Slice out-of-scope respected | pass | No signature/authentication/schema/dependency/refactor work was added. |
| Item 2 not started | pass | Diff is limited to the shared reader, redundant on-demand checksum removal, and focused checksum tests. |
| Dependencies done | pass | Slice declares `Depends on: none`. |

## Acceptance Criteria Review

| Requirement | Verdict | Evidence | Notes |
|-------------|---------|----------|-------|
| Valid package still loads | pass | Existing package-reader happy path at `ScenarioCatalogJsonlWriterSpec.groovy:89-102`; focused suite passes. | Includes unchanged high-precision semantic round trip. |
| Missing or malformed checksums reject | pass | `ScenarioCatalogPackageReader.java:181-184`; `ScenarioCatalogJsonlWriterSpec.groovy:127-153`. | Generic production check applies to every metadata record. |
| Every linked artifact rejects changed bytes | pass | `ScenarioCatalogJsonlWriterSpec.groovy:155-175`. | Appended blank lines are intentionally semantically/count-neutral, so failures are specifically checksum-driven rather than incidental parse/count failures. |
| Semantically valid same-count replacement rejects | pass | `ScenarioCatalogJsonlWriterSpec.groovy:177-196`. | Reorders a JSON field without changing record semantics/count. |
| Executor cannot return/select/execute mismatched content | pass | `ScenarioExecutorSpec.groovy:747-765`; shared delegation in `ScenarioCatalogReader.java`. | Failure occurs before materialization and measured execution. |
| On-demand still validates boundaries and accepts valid packages | pass | Valid request and outside/symlink regressions in `OnDemandFaultScenarioServiceSpec.groovy`; all 37 tests pass. | Mismatch regression also verifies mutable artifacts remain byte-identical. |

## Verification Evidence Check

| Command / Method | Verdict | Notes |
|------------------|---------|-------|
| `cd verifiers && mvn -Dtest=ScenarioCatalogJsonlWriterSpec,OnDemandFaultScenarioServiceSpec,ScenarioExecutorSpec test` | pass | `BUILD SUCCESS`; 114 tests, 0 failures, 0 errors, 0 skipped (57 executor, 20 package writer/reader, 37 on-demand). |
| `git diff --check` | pass | No tracked whitespace errors. |
| Required scoped `git diff --check -- ...` | pass | No output/errors for the production export, export tests, executor tests, and issue package paths. |
| Diff/status/numstat/conflict-marker inspection | pass | Exactly two changed production files and three changed tests; no conflict markers. The untracked follow-up/remediation artifacts are expected. The two pre-existing meeting-note files were ignored and untouched. |
| Hash-consistent malformed UTF-8 reviewer probe | fail | A copied valid package was modified to contain raw `0x80` in an ignored WorkloadPlan JSON field and its manifest SHA-256 was refreshed. Current reader output: `ACCEPTED workloads=1`. `Files.readAllLines` on the same file output: `REJECTED MalformedInputException`. |
| Accidental root-level Maven invocation | not applicable | Failed immediately because the repository has no root POM; the required command was then run from `verifiers/` and passed. No files were changed by the failed invocation. |

## Code Quality / Repo Fit

| Area | Verdict | Notes |
|------|---------|-------|
| Minimality | pass | Small shared-reader change plus removal of the now-redundant on-demand checksum pass. |
| Existing patterns | pass | Reuses `ScenarioCatalogJsonlWriter.sha256(byte[])` and the current reader/export architecture. |
| Test quality | pass | Artifact mismatch tests use semantically ignored byte changes, same-count replacement, executor side-effect sentinels, and on-demand byte snapshots; they do not merely assert incidental parse/count failures. |
| Regression risk | fail | Lossy JSONL decoding introduces acceptance of malformed UTF-8 that the old reader rejected. |
| Security/data safety | fail | The checksum can attest malformed bytes while the semantic parser sees replacement characters instead of a strict UTF-8 interpretation. This is integrity validation, not authenticity, but the byte-to-model boundary must still be faithful. |
| Change hygiene | pass | No unrelated production/test change, dependency, schema change, or item-2 work. |

## Findings

| Severity | Finding | Evidence | Required Correction |
|----------|---------|----------|---------------------|
| major | JSONL snapshots are decoded with replacement semantics, so checksum-matching malformed UTF-8 can be accepted and parsed as different text. This regresses the prior strict read behavior and weakens the requirement to parse the verified snapshot faithfully. | `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/export/ScenarioCatalogPackageReader.java:261-262` uses `new String(bytes, StandardCharsets.UTF_8)`. Reviewer probe: raw `0x80` plus refreshed manifest checksum produced `ACCEPTED workloads=1`; the former `Files.readAllLines` behavior rejects the same file with `MalformedInputException`. | Decode the retained `byte[]` snapshot with a strict UTF-8 decoder (`CodingErrorAction.REPORT`) while preserving the existing line-splitting/blank-line/count semantics, and convert malformed-input failure into an artifact-identifying package-read diagnostic. Add a regression with a manifest checksum matching malformed JSONL bytes and assert rejection. Hash and parse must continue to consume the same snapshot; do not re-read the file. |

No blocking, minor, or note findings.

## Edge-Case Review Notes

- **Line splitting:** for valid UTF-8, `String.lines()` and the former `Files.readAllLines` agree on LF, CRLF, CR, trailing terminators, blank lines, and empty files for the reader's skip/count behavior. The blocker is malformed-sequence replacement, not ordinary delimiter handling.
- **Empty JSONL:** unchanged behavior remains: blank lines are skipped and exact manifest record counts decide whether zero records are valid. The checksum still covers empty/blank bytes.
- **Malformed checksum/error ordering:** all four metadata records are schema/kind/path/hash-format/count-checked before artifact reads; artifact hashes are then verified before any artifact parsing. Diagnostics identify artifact kind and checksum issue.
- **Redundant checks:** removal from `OnDemandFaultScenarioService` is justified because every on-demand package read now passes through the shared reader. Root/containment/path-consistency/symlink checks remain separate and non-redundant.
- **TOCTOU:** the code substantiates only the narrow claim that each artifact's checksum and parsing use one in-memory snapshot. It does not establish a general filesystem race-free package read, and this review does not treat it as doing so.

## Done Transition

- Moved to done: `no`
- Done path: `None`
- Reason if not moved: `FAIL` due to the major UTF-8 fidelity regression. The user also explicitly prohibited moving this remediation slice during this review.

## Recommendation

Keep remediation item 1 active. Fix strict UTF-8 decoding from the existing snapshot, add the hash-consistent malformed-UTF-8 regression, rerun the same 114-test focused command and diff checks, then request review attempt 02. Do not begin item 2 yet.
